# connector-api 业务与技术架构现状分析

> **文档性质**：现状分析（代码级，非调研文档）
> **分析对象**：`connector-api` 服务（`/home/usb/wks/open-app/connector-api/`）
> **数据来源**：静态代码扫描 + 关键源码逐行阅读，所有结论均以实际代码佐证；少量设计意图推断处已标注"（推断）"。
> **路径约定**：本文路径相对 `connector-api/src/main/java/com/xxx/it/works/wecode/v2/`，下文简写为 `…/v2/`。

---

## 零、摘要

connector-api 是 open-app 生态中面向「连接流同步执行」的**运行时引擎**。它采用 Spring WebFlux 全响应式单栈（WebFlux + R2DBC + Reactive Redis + WebClient），以「编排 JSON 快照 + DAG 调度 + 节点执行器插件体系 + 双写留痕」为骨架。与 open-server 构成读写分离：open-server 管"流如何被设计、审批、发布"，connector-api 管"流被触发后如何运行、过程如何留痕"。

> **现状核心结论**：整条热路径（HTTP 入口、配置读取、connector 节点出站调用、执行留痕）是响应式（非阻塞）的；**唯一例外是脚本节点（script）**——它因 GraalJS 沙箱的同步执行模型，成为一个「同步孤岛」，脚本内部的 `ctx.http` 动态 HTTP 调用无法利用响应式能力（详见 §5 专项）。

---

## 一、服务速览

| 维度 | 事实（代码佐证） |
|---|---|
| 定位 | 连接器平台运行时服务（连接流同步调度执行引擎） |
| 入口 | `ConnectorApiApplication`（`@SpringBootApplication`），Spring Boot 3.5.14 / Java 21 |
| 端口/路径 | `server.port: 18180`；`spring.webflux.base-path: /connector-api`（application.yml） |
| 对外端点 | `POST /api/v1/flows/{flowId}/invoke`（对外触发）；`POST /api/v1/flows/{flowId}/versions/{versionId}/debug`（内部调试） |
| 依赖 | MySQL `openapp`（R2DBC，192.168.3.155）、Redis Cluster（Reactive Lettuce）、GraalJS 24.2.1、SpringDoc、Actuator |

---

## 二、业务架构（Business）

### 2.1 定位与职责边界：与 open-server 读写分离

| 服务 | 技术栈 | 分工 |
|---|---|---|
| **open-server**（设计侧） | 阻塞式 Spring MVC + MyBatis，端口 18080 | 连接器/连接流/版本的 **CRUD、审批、发布、启停**（写 flow_t / flow_version_t / connector_version_t）；提供 debug-proxy 转发至 connector-api；通过 `FlowCacheEvictor` 在 Flow 启停/部署/删除、FlowVersion 审批/失效时主动删除运行态 Redis key，保证"设计态写入 → 运行态读缓存"一致 |
| **connector-api**（运行侧，即本服务） | Reactive WebFlux + R2DBC，端口 18180 | **只读侧**：只读 flow/flow_version 配置（写入由 open-server 完成）；连接器配置已改为直接消费编排快照 `node.data.connectorVersionConfig`（v6.0"编排自包含"），不再查连接器表。**写入侧**：仅写执行记录/步骤日志（与 open-server 共享表结构，open-server 做查询。**运行时语义**：只读平台全局配置（Lookup `CEC.Open/Connector.Platform.Config`），不读应用级覆盖；生效值 = 编排快照值 ? 编排快照值 : 平台全局配置 |

一句话：**open-server 管"流怎么被设计和发布"，connector-api 管"流被触发后怎么跑、跑的过程怎么留痕"**。

### 2.2 核心业务对象及其业务关系

| 对象 | 对应表/Entity | 关系/关键语义 |
|---|---|---|
| Flow（连接流） | `openplatform_v2_cp_flow_t` / `FlowEntity` | 顶级业务实体；`lifecycle_status`：1=stopped 2=running 3=invalidated 4=deleted；仅 **running(2)** 可被 invoke；`deployed_version_id` 指向当前部署版本 |
| FlowVersion（流版本/编排配置） | `openplatform_v2_cp_flow_version_t` / `FlowVersionEntity` | Flow 的 N:1 版本；`orchestration_config` 为核心，React Flow 格式 JSON（`{nodes[], edges[], flowConfig{}}`）；v6.0 起**编排自包含**——连接器配置以快照 `connectorVersionConfig` 内嵌在节点 `data` 中 |
| Node（节点） | 编排 JSON 中 `nodes[]` | 无独立表，纯编排概念；画布用 `node.type` 渲染，**引擎执行路由用 `node.data.type`**（NodeTypeResolver，"框业分离"） |
| Edge（边） | 编排 JSON 中 `edges[]` | 表达 DAG 依赖；引擎兼容 `source/target` 与 `sourceNodeId/targetNodeId` 双命名 |
| Connector（连接器） | `openplatform_v2_cp_connector_version_t` / `ConnectorVersionEntity` | 运行时已不再直接读表（Entity 保留但执行走快照），连接器本质变为"节点配置 + 认证类型 Schema" |
| ExecutionRecord / ExecutionStep（执行留痕） | `…_execution_record_t` / `…_execution_step_t` | 运行时写入侧产物，open-server 查询展示；`startRecord` 预建 → `updateRecord` 完成；`logStepsBatch` 批量写步骤 |

**业务闭环**：Flow（运行中）+ DeployedVersion（编排快照）→ 每次调用生成一条 ExecutionRecord（雪花 ID）→ DAG 内每节点一条 ExecutionStep。

### 2.3 业务流程（HTTP 触发视角）

```
外部调用方
  │  POST /connector-api/api/v1/flows/{flowId}/invoke
  ▼
[1] InboundRateLimiter (WebFilter @Order(-100))  — Redis Lua 原子限流 (QPS/并发)，超限 429
  ▼
[2] FlowInvokeService.invokeFlow
  ├─ 生成 executionId(UUID) + recordId(雪花) + 计时
  ├─ 读日志采集开关（平台配置①），关闭则不写 record/step
  ├─ loadFlowVersion: flow(lifecycle==2 校验) → deployed_version_id → 版本编排配置
  │    缓存层：EntityCacheManager(flow/flowversion, TTL 7d±2h) + cp:flow:config:{id}(TTL 120s read-through)
  ├─ initExecutionRecord(triggerType=1)  ★ fire-and-forget 预写记录
  ├─ parseAndValidateTrigger: 找 trigger 节点(data.type=trigger)
  │    ├─ triggerType ∈ {http, manual} 校验
  │    ├─ validateAuthConfig → SYSTOKEN 白名单（42001/42002/43001）
  │    ├─ validateRateLimitConfig（基本合法性）
  │    └─ validateInputContract header/query/body 三段（含 String→Number 自动转换）
  ├─ 构建 ExecutionContext + trigger 结构化 NodeContext {header, query, body}
  ├─ 解析 flowConfig.cache → cacheEnabled? checkCache(Redis GET) : …
  ├─ dagScheduler.schedule(orchestrationConfig, ctx)  ★ 核心执行
  │    ├─ 解析 nodes/edges → nodeMap + 邻接表 + 入度计数器
  │    ├─ findEntryNode(trigger) → executeDag 递归
  │    ├─ 每节点: resolveNodeTimeout(③ node.data.timeoutMs ?: ① 平台) → NodeExecutor.execute().timeout()
  │    ├─ 单下游=串行递归；多下游=Flux.merge 并行分支；join 点按入度计数归零触发一次
  │    └─ 节点失败 → 记录 failure NodeContext，不抛中断（失败不阻断后续执行）
  ├─ buildResultFromExecutionContext: status/totalDuration/steps/exit 节点输出 → resultData
  ├─ persistStepLogs (脱敏后批量写 step 表)
  ├─ finalizeExecutionRecord: 读① Max.Execution.Records.Per.Flow → updateRecord + FIFO 清理
  └─ buildTransparentResponse: exit 节点 output.body→响应体, output.header→响应头,
                              元数据→X-Flow-Id/X-Execution-Id/X-Code/X-Duration-Ms/X-Cache-Status 等
  ▼
[3] 结果缓存回写 (FlowCacheManager.writeCache, Lua SET+SADD)
```

调试链路（open-server debug-proxy → `FlowVersionDebugController.executeTestRun` → `FlowVersionDebugService`）：
- 校验**调用方** SysToken + Spring 配置白名单 `internal.auth.sys-account-whitelist`（fail-closed），而非流的 `sysAccountWhitelist`
- **不校验版本状态**（草稿/已发布/已撤回均可调试）、**不写执行记录表**（即时反馈）；`context.setDebug(true)`、`triggerType=2`，结果返回 `ExecutionResult`（含各 step + labelCn/labelEn，非透明穿透）

### 2.4 节点类型体系（业务语义）

引擎从 `node.data.type` 路由，各执行器通过 `NodeExecutor.getNodeType()` 自动注册进 `DagScheduler.executorMap`：

| 业务类型 | 执行器 | 业务语义 |
|---|---|---|
| `trigger` | `TriggerNodeExecutor` | 入口。结构化输入 `{header, query, body}`（query 自动数值化）。**不真正发起外部动作**，仅把请求数据包装为节点上下文供下游引用 |
| `connector` | `ConnectorNodeExecutor` | **HTTP 出站调用**。v6.0 优先读 `data.connectorVersionConfig` 快照，无快照走 legacy `data.protocolConfig`；经 `UnifiedCredentialProcessor` 注入凭证；用 `WebClient.exchangeToMono` 全响应式调用；错误按 DNS/连接/读超时/SSL/普通失败细分（62001~62005） |
| `script` | `ScriptNodeExecutor` | **GraalJS 沙箱执行用户 JS**（`function main(ctx){…}`），支持上游数据组装 ctx + 可选 `ctx.http` 出站客户端 |
| `data_processor` | `DataProcessorExecutor` | 纯内存字段映射/常量赋值（`fieldMappings`，sourceType=constant/reference），"胶水"节点 |
| `parallel` | `ParallelBranchExecutor` | 并行处理节点：上限 8 分支（`PARALLEL_TOO_MANY_BRANCHES`），`Flux.merge` 并发，统计 success/fail → success / partial_success / failed；分支内子节点链目前是简化透传（真正并行由 DagScheduler 的"多下游→并行"驱动） |
| `exit` | `ExitNodeExecutor` | 出口。按 `data.outputMapping{header, body}` 解析表达式组装最终响应；无映射时降级 `collectFallbackOutputs` |

### 2.5 业务接入方（谁消费 connector-api）

| 接入方 | 方式 | 佐证 |
|---|---|---|
| open-server debug-proxy | 内部转发 `/debug`，X-Sys-Token 认证 | `OpDebugProxyService`、`FlowVersionDebugService.validateInvokerToken` |
| 外部系统 | 直接 `POST …/flows/{id}/invoke`，带 SysToken/签名；受流 `authConfigs[].sysAccountWhitelist` 约束 | `FlowInvokeService.validateSystoken` |
| open-server FlowCacheEvictor | 反向的"缓存维护者"（非请求调用方） | `FlowCacheEvictor` 删除运行态 key |
| Python 集成测试 / 运维脚本 | 直连 18180 触发全场景（17 个场景文件） | `src/test/python/` |
| 脚本/connector 节点出站调用的下游 | 被调用方 | `ScriptHttpClient`、`ConnectorNodeExecutor` |

---

## 三、技术架构（Technical）

### 3.1 技术栈清单（pom.xml 确认）

| 技术 | 版本/实现 | 用途 |
|---|---|---|
| Spring Boot | 3.5.14 (parent) | 框架底座 |
| Java | 21 | 语言（虚拟线程可用） |
| WebFlux | starter-webflux | 全响应式 HTTP 层（Netty） |
| R2DBC | `io.asyncer:r2dbc-mysql:1.3.2` | 响应式 MySQL 访问 |
| Reactive Redis | starter-data-redis-reactive（Lettuce + Cluster） | 实体缓存/执行结果缓存/限流/配置缓存 |
| GraalVM Polyglot | `org.graalvm.polyglot:polyglot:24.2.1` + `js-language` | JS 脚本沙箱 |
| SpringDoc | springdoc-openapi-starter-webflux-ui:2.5.0 | OpenAPI 文档 |
| Actuator | starter | health（db/redis enabled，show-details=always） |
| Lombok / Validation | starter | 样板/校验 |
| 测试 | starter-test, reactor-test | JUnit 5 + StepVerifier |

### 3.2 模块/包结构

```
com.xxx.it.works.wecode.v2
├── common/                      横向公共层
│   ├── config/                  R2dbcConfig / ReactiveRedisConfig / JacksonConfig /
│   │                            ConnectorApiPropertyService(读平台全局配置) /
│   │                            OpenplatformLookupRepository / OpenplatformPropertyRepository
│   ├── error/ErrorCode          统一错误码枚举（code+zh+en+HTTP 四维内聚）
│   ├── exception/DefaultErrorHandler  全局异常/错误工厂
│   ├── constant/AuthType        认证类型整数码
│   ├── IdGenerator              雪花 ID（worker/datacenter 可配）
│   └── annotation/StandardTodo  stub 占位标注
├── modules/
│   ├── flow/                    ★ 对外触发入口（FlowInvokeController / FlowInvokeService / FlowEntity / OpFlowReadRepository）
│   ├── flowversion/             ★ 内部调试入口（FlowVersionDebugController / FlowVersionDebugService）
│   ├── runtime/                 ★ DAG 执行核心（DagScheduler / FlowRuntimeEngine / NodeTypeResolver /
│   │                            VersionConfigResolver / FlowConfigParser / executor/NodeExecutor +
│   │                            ReactiveSequentialExecutor / node/{Trigger,Connector,DataProcessor,Exit} /
│   │                            context/{ExecutionContext,NodeContext} / model/{NodeOutput,ExecutionResult,
│   │                            FlowConfig,ResolvedFlowConfig,TransparentFlowResponse} / expression/ExpressionResolver / config/RuntimeConfig）
│   ├── script/                  ★ GraalJS 沙箱（GraalJsContextFactory / ScriptNodeExecutor / CtxAssembler / ScriptHttpClient / ScriptExecutionConfig）
│   ├── auth/                    ★ 认证（SysTokenResolver / credential/{UnifiedCredentialProcessor,
│   │                            CredentialSupplierRegistry, 多种 CredentialSupplier}）
│   ├── execution/               ★ 写入侧（ExecutionRecordService / ExecutionStepService / ExecutionStepLog / LogSanitizer / repositories）
│   ├── connector/entity/        ConnectorVersionEntity（保留实体，执行走快照）
│   ├── ratelimit/               ★ 入站限流（InboundRateLimiter WebFilter + Lua）
│   └── cache/                   ★ 缓存（EntityCacheManager / FlowCacheManager / CacheKeyResolver）
```

### 3.3 关键组件逐一说明

1. **DagScheduler**（`runtime/DagScheduler.java`）— **当前真正生效的调度器**。解析编排 JSON → 建 nodeMap/邻接表/入度计数器 → 找 trigger 入口 → DFS 递归；单下游串行 / 多下游 `Flux.merge` 并行；自带节点超时解析（③`node.data.timeoutMs` → ① 平台 `Node.Max.Timeout.Seconds`，默认 5s）；节点级 `timeout().onErrorResume` 把异常转成 failed/timeout NodeOutput。**错误语义：吞异常、标记失败、不中断 DAG。**
2. **FlowRuntimeEngine**（`runtime/FlowRuntimeEngine.java`）— 注释描述"5 阶段执行管道"，但**当前 Controller 路径并未调用它**，疑似早期/备用引擎，与 ReactiveSequentialExecutor 一同属"冗余双轨"。
3. **ReactiveSequentialExecutor**（`runtime/executor/`）— 早期顺序执行器（`topologicalSort` + `Flux.reduce` 顺序串联）。**未被 Controller 引用**，仅被旧 E2E 测试 autowire，保留作兼容。
4. **ParallelBranchExecutor**（`runtime/ParallelBranchExecutor.java`）— `parallel` 类型节点执行器：提取 `data.branches`（上限 8），`Flux.merge` 并发，输出 `{parallelResult:{totalBranches,successCount,failCount}, branchResults:[…]}`；无 inline branches 时透传成功。
5. **ExecutionStepLog / ExecutionStepService**（`execution/`）— 步骤写入 DTO + 批量服务；先查日志采集开关，再对 input/output 做 `LogSanitizer.sanitize`（敏感键递归脱敏），JSON 化后 `repository.saveAll`。全部 fire-and-forget、吞异常。
6. **GraalJsContextFactory**（`script/`）— 五层纵深沙箱：`allowIO(false)` / `allowCreateThread(false)` / `allowNativeAccess(false)` / `allowHostAccess(EXPLICIT + Map/List/Buffer)` / `allowAllAccess(false)`；ResourceLimits 语句上限 10000；ES2022 严格模式；**Engine 单例复用、Context 每次新建即关（close(true) 强杀）**；`@PostConstruct` 预热防冷启动误报超时。
7. **FlowCacheManager**（`cache/`）— 执行结果缓存：`checkCache`=GET+反序列化 `ExecutionResult`；`writeCache`=Lua `flow_cache_write.lua` 原子 SET + SADD 索引。key 用 `{flowId}` hash tag 规避 Cluster CROSSSLOT。TTL ③ → ① `Flow.Max.Cache.Ttl.Seconds`（默认 1296000s=15d）。
8. **EntityCacheManager**（`cache/`）— Cache-Aside：flow/flowversion/ratelimit/lookup 四类实体缓存；TTL 7 天 ±2h 随机 jitter 防雪崩；Redis miss 回源 R2DBC 再回写；Redis 不可用整体降级 DB。
9. **InboundRateLimiter**（`ratelimit/`，`@Component @Order(-100)` WebFilter）— 仅对 `…/flows/{flowId}/invoke` 类路径生效；QPS 模式 Lua 令牌桶 / 并发模式 Lua INCR+EXPIRE（300s TTL）；超限写 429 + X- 头 + Retry-After；Redis 不可用降级放行。
10. **UnifiedCredentialProcessor + CredentialSupplier**（`auth/credential/`）— 出站凭证注入，见 §3.5。
11. **LogSanitizer**（`execution/`）— 见 §3.3.5。
12. **IdGenerator**（`common/`）— 标准雪花：41bit 时间戳（epoch 2024-01-01）+5 datacenter+5 worker+12 sequence；检测时钟回拨直接抛异常拒绝生成。

### 3.4 运行时模型（核心数据结构）

| 类 | 角色 | 关键字段/结构 |
|---|---|---|
| `ExecutionContext` | 单次执行"黑板上下文"，线程安全 | `executionId, flowId`（final）；`triggerData/triggerHeaders/triggerQueryParams`；`Map<String,NodeContext> nodeContexts`（ConcurrentHashMap）；`debug`；`triggerType`(1=HTTP,2=调试) |
| `NodeContext` | 单节点输入/输出/状态 | `nodeId/nodeType/input(Map)/output(Map)/status(success/failed/timeout)/errorInfo({code,messageZh,messageEn})/durationMs` |
| `NodeOutput` | NodeExecutor 返回值 DTO | 与 NodeContext 同构双分区 input/output + status + durationMs + errorInfo |
| `FlowConfig` | 运行时行为配置 | `timeoutMs / maxQps / maxConcurrency / cacheTtl / cacheKeys`；解析失败回 `defaults()` |
| `ResolvedFlowConfig` | 版本解析产物 | `flow + flowVersion + flowConfig` 三元组 |
| `ExecutionResult` | 执行结果信封（debug 接口返回） | `executionId/flowId/status/resultData/steps[StepDetail]/errorInfo/debug/cacheHit` |
| `TransparentFlowResponse` | HTTP 触发响应载体（v5.8 核心） | `body` + `userHeaders`(exit 自定义头) + `platformHeaders`(X- 前缀元数据) + `httpStatus` |

**数据流转**：`NodeExecutor.execute(ctx, nodeConfig) → Mono<NodeOutput>` → DagScheduler 转为 `NodeContext` 存入 `ExecutionContext.nodeContexts` → 下游/exit/脚本通过 `ExpressionResolver` 以 `${$.node.{id}.output.{path}}` 引用。

### 3.5 认证体系（双方向设计）

- **入站方向**（验证调用方）：`SysTokenResolver` 目前为 stub（`isTokenValid`=非空；`resolveSysAccount`=原样返回 token），均标 `@StandardTodo`。白名单校验分两种：invoke 流校验流配置 `authConfigs[].sysAccountWhitelist`（42001/42002/43001）；debug 接口校验 Spring 配置 `internal.auth.sys-account-whitelist`（未配置 fail-closed）。
- **出站方向**（connector 节点调用下游注入凭证）：`UnifiedCredentialProcessor.apply(authConfigs, headers, queryParams, ctx)` 遍历 authConfigs → 按 type 分派 `CredentialSupplierRegistry` → supplier 返回 `{value表达式 → 解析值}` → 注入 header/query。已实现类型：SYSTOKEN(stub)/AKSK(stub)/SOA(stub)/APIG(读配置+Todo)/**SIGNATURE(真实，HMAC-SHA256)**/**COOKIE(真实，复用 ExpressionResolver)**/NONE(显式空)/DEFAULT(兜底)。

> **诚实提示**：除 SIGNATURE/COOKIE 外，多数 supplier 与 SysTokenResolver 均为 stub/占位（`@StandardTodo`），说明"与凭据管理服务对接"属标准环境待办；当前开发环境以常量/配置值工作，认证强度有限。

### 3.6 基础设施层

- **application.yml**：R2DBC 池（max 20/initial 5/max-idle 30m）；Redis Cluster 6 节点 + lettuce pool（max-active 8）；`management.health.db/redis.enabled=true`；`script.http.client.enabled=true`；`internal.auth.sys-account-whitelist=dev-sys-token`。
- **R2dbcConfig**：手写 URL 解析构建 `ConnectionFactory`（driver=mysql，SSL=false，connectTimeout 10s）；`@EnableR2dbcRepositories`。不维护 DDL/迁移（由 open-flyway 管）。
- **JacksonConfig**：`FAIL_ON_UNKNOWN_PROPERTIES=false` + JavaTimeModule + LOWER_CAMEL_CASE。
- **ReactiveRedisConfig**：`ReactiveRedisTemplate<String,String>`。
- **Lua 脚本**（resources/lua/）：`rate_limit_qps.lua`（秒级令牌桶）、`rate_limit_concurrency.lua`（并发取号+回退）、`flow_cache_write.lua`（SET+SADD 单次往返，hash tag 防 CROSSSLOT）。
- **错误处理**：`ErrorCode` 5 位码分族（2xxxx 成功 / 41xxx-43xxx 前置校验 / 50000 系统 / 60000-66xxx 执行）；`DefaultErrorHandler` @RestControllerAdvice 兜底；HTTP 触发错误通过 X-Code/X-Message-* 头传播（v5.8 失败统一 400，除前置 401/403）。
- **配置读取模型**：`ConnectorApiPropertyService` 只读平台全局配置①，全部带硬编码兜底默认值，DB 失败返回默认不抛错。

### 3.7 测试策略（三层）

**Java 单元测试**（23 文件，`mvn test` 无外部依赖）：按模块覆盖 runtime/script/cache/ratelimit/execution/flow/flowversion/common。一个 `@SpringBootTest @ActiveProfiles("test")` 的 ConnectorFlowE2ETest（挂在旧顺序执行器）。
**Python 集成测试**（17 个场景文件，`src/test/python/inspect/`，需服务在线）：**一场景一文件**；L0~L4 五级金字塔（L0 冒烟/commit → L1 核心/PR → L2 场景/每日 → L3 运行记录/每周 → L4 预留）；**红线：禁止假 OK**——触发执行至少校验 HTTP 状态码 + X-Status 头 + 响应体字段；基础设施集中在 `client.py` / `conftest.py` / `mock_server.py`。
覆盖的行为契约示例：IT-049~065（触发全场景）、FR-012~014（多认证）、FR-034（节点超时）、FR-037（缓存）、FR-038（并行/串行）、FR-040a（脚本沙箱及 HTTP）、FR-041（草稿调试）、FR-042~044（执行记录/日志脱敏/版本解析）。

---

## 四、模块依赖与请求链路

**模块依赖方向（自底向上）**：
```
Controller(flow/flowversion)
   → Service(FlowInvokeService / FlowVersionDebugService)
      → runtime(DagScheduler → executorMap[NodeExecutor] → node/{trigger,connector,data_processor,exit}
         / script/ScriptNodeExecutor / runtime/ParallelBranchExecutor)
         → runtime/expression(ExpressionResolver)
         → auth/credential(UnifiedCredentialProcessor → CredentialSupplierRegistry → *Supplier)
      → flow/repository + cache(EntityCacheManager / FlowCacheManager)  ←→ Redis/R2DBC
      → execution(ExecutionRecordService/ExecutionStepService → repositories → R2DBC)
      → common/config(ConnectorApiPropertyService → EntityCacheManager.getLookupConfig)
横切: ratelimit/InboundRateLimiter(WebFilter) · common(IdGenerator/ErrorCode/DefaultErrorHandler)
```

**一次完整请求的线程/资源轨迹（invoke）**：
1. Netty/Reactor 线程接 HTTP → `InboundRateLimiter`(order -100) 在 Redis 执行 Lua（原子 1 RTT）→ 进 Controller
2. 全链路返回 `Mono`，无阻塞点；DB 用 R2DBC、Redis 用 Reactive（代码反复注释"避免在 reactor/lettuce 线程上 block 自死锁"）
3. connector 节点出站走 `WebClient`（Netty，5s connect timeout，16MB in-memory 上限）
4. script 节点切到虚拟线程执行器（`subscribeOn(Executors.newVirtualThreadPerTaskExecutor())`）跑 GraalJS；`ctx.http` 走独立 4 线程静态池的阻塞 HttpClient
5. 执行记录/步骤写库全部 `subscribe()` fire-and-forget（失败只记日志，不影响响应）
6. 结果 → `TransparentFlowResponse` → Controller 拼装 body/user headers/platform headers

> **关键观察**：第 4 步是整条链路唯一的"同步孤岛"——脚本节点在响应式外壳内跑的是同步 JS + 阻塞 HTTP。这正是 §5 专项要讨论的核心。

---

## 五、专项：脚本节点动态 HTTP 调用与响应式能力现状分析

### 5.1 业务场景定义

> 配置一条连接流：**脚本节点**内先查询/计算出目标地址，然后**在脚本节点内发起 HTTP 请求**（调用 Java 代码提供的 `ctx.http`，即 `ScriptHttpClient`），把下游返回结果交给后续节点/exit。

**核心疑问**：这种场景下，动态调用能否利用到响应式能力？是否因为脚本节点的影响而无法利用？

### 5.2 结论：当前无法真正利用响应式

**脚本节点在整条响应式链路中是一个「同步孤岛」**。它只做到了"外壳响应式、内核同步阻塞"——`ctx.http` 的动态 HTTP 调用无法利用响应式能力。

### 5.3 归因分析（代码佐证）

**（1）GraalJS 沙箱是同步执行模型，没有到 Java Mono 的异步桥接**

`ScriptNodeExecutor.execute()` 第 131-133 行：
```java
return Mono.fromCallable(() -> executeScript(scriptSource, ctxMap))
        .subscribeOn(Schedulers.fromExecutor(Executors.newVirtualThreadPerTaskExecutor()))
        .timeout(Duration.ofSeconds(finalTimeoutMs));
```
脚本入口 `main(ctx)` 为**同步返回一个 Map**（`mainFunc.execute(ctxMap)`）。JS 没有 `await` 一个 Java `Mono` 的能力——无法让 JS `await` 一个响应式 `Mono`。

**（2）`ctx.http` 注入的是阻塞版 `ScriptHttpClient`，不是响应式**

`ScriptNodeExecutor` 第 121 行注入 `ctxMap.put("http", new ScriptHttpClient());`。

`ScriptHttpClient.java` 关键事实：
```java
// 第 84 行：返回 Map（同步），不是 Mono
public Map<String, Object> request(String method, String url, Map<String, Object> opts)

// 第 123-124 行：JDK 阻塞 HttpClient
HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

// 第 140-142 行：阻塞等待（3s 上限）
Future<Map<String, Object>> future = executor.submit(task);
return future.get(FUTURE_TIMEOUT_SECONDS=3, TimeUnit.SECONDS);
```
所以脚本里 `const res = ctx.http.request('POST', url)` 拿到的是**已经完成后的同步结果 Map**，全程没有非阻塞、没有背压，当前虚拟线程会在这个 HTTP 上"卡住"到完成或 3s 超时。

**（3）脚本节点整体是"外壳响应式、内核阻塞"**

`Mono.fromCallable(executeScript)` 提供了响应式外壳（`.timeout()`、`.onErrorResume()`、`.subscribeOn(虚拟线程)`），但一旦进入 `main(ctx)`，内部全部同步——包括 `ctx.http.request()`。脚本节点不会阻塞 Reactor 事件循环（因为卸载到了虚拟线程），但其 HTTP I/O 本身是阻塞的。

### 5.4 对比：connector 节点才是"真响应式"

`ConnectorNodeExecutor` 用 `WebClient.exchangeToMono(...)`（返回 `Mono`，non-blocking），是整条链路真正利用响应式能力的部分。**只有脚本节点**，为了走 GraalJS 沙箱，被迫改用阻塞客户端。

### 5.5 响应式改造可行方向

| 方案 | 做法 | 响应式程度 | 代价 |
|---|---|---|---|
| **A. 拆分节点（推荐）** | 脚本节点只负责"算地址"；把 HTTP 调用移到独立 connector 节点/自定义响应式节点（`WebClient.exchangeToMono` 返回 `Mono<NodeOutput>`） | ✅ 完整响应式 | 编排结构改动；需确认地址能被下游表达式 `${$.node.xxx.output.url}` 引用 |
| **B. 改造 ScriptHttpClient 走 WebClient** | 底层换 `WebClient`，但方法内部 `mono.block()` 返回同步 Map | ⚠️ 半个响应式 | 依旧同步等待，仅利用异步 IO/连接池；JS 仍同步拿结果 |
| **C. 做 JS Promise ↔ Java Mono 桥接** | 注入返回 JS `Promise` 的 http 方法，脚本用 `async/await`，靠 GraalVM 异步转译 | ✅ 最强 | 复杂度最高；`main(ctx)` 入口要改 async，工作量大，MVP 阶段不划算 |

### 5.6 真实性提醒：脚本 HTTP 的硬瓶颈

- **固定 4 线程池**：`ScriptHttpClient` 第 52 行 `Executors.newFixedThreadPool(4)`。所有流的脚本 HTTP 调用共享这 4 个线程，并发超过 4 个即排队。
- **每个调用被 3s `future.get()` 限制**（`FUTURE_TIMEOUT_SECONDS=3`），非 flow/节点超时。
- **GraalJS Context 每次 new、不池化**（源码注释为 MVP 取舍），高并发下冷启动开销放大。

---

## 六、设计亮点 / 值得注意的权衡与风险

### 6.1 设计亮点

1. **全响应式单栈**：WebFlux + R2DBC + Reactive Redis + WebClient + GraalJS 隔离，热路径无阻塞 API；"异步读平台配置"避免 reactor/lettuce 线程自死锁。
2. **只读/写入分离 + 缓存协同闭环**：open-server（阻塞栈）与 connector-api（响应栈）读写分家；跨服务一致性靠 open-server `FlowCacheEvictor` 主动逐出 + connector-api read-through 回填。
3. **编排自包含快照（v6.0）**：connector 配置快照内嵌节点 data，运行期单表可跑、不 join 连接器表，配合 `deployed_version_id` = "发布即冻结"。
4. **Lua 原子化**：QPS 令牌桶 / 并发取号（超限回退）/ 缓存 SET+SADD 全部 Lua 原子 + `{flowId}` hash tag 规避 Cluster CROSSSLOT。
5. **深度安全防御**：GraalJS 五层纵深；HTTP 头注入防护（去 CR/LF）；日志脱敏（LogSanitizer 递归+不区分大小写）；错误消息 HTML escape。
6. **错误码四维内聚**：`ErrorCode` 一个枚举管 X-Code / messageZh / messageEn / HTTP status，全错误路径共用同一结构化 errorInfo，诊断信息经 X-Error-Node 头指到失败节点。
7. **测试分层与"禁止假 OK"**：一场景一文件 + L0~L4 金字塔 + 双重断言（HTTP 层 + DB 层），并把 429/401/超时等反向场景编入契约。

### 6.2 值得注意的权衡 / 风险点

1. **"双轨"引擎冗余**：`FlowRuntimeEngine`（5 阶段管道）与 `ReactiveSequentialExecutor`（顺序链）均已实现并注册为 Bean，但当前 HTTP 与 debug 两条链路都直连 `DagScheduler`；三者并存且语义不同，长期是维护与认知负担（E2E Java 测试还挂在旧执行器上）。
2. **节点失败不中断 DAG**：DagScheduler 对节点异常 `onErrorResume` 吞掉并继续向下游传播执行；"单分支失败不影响并行"被推广到串行链，造成"失败节点之后的下游仍发起对外调用"的副作用，调用方需靠 X-Code 而非 body 判断成败。
3. **响应式外壳内的阻塞内核**：脚本 HTTP 用阻塞 JDK HttpClient + 固定 4 线程池 + 3s 上限，高并发下成为隐性吞吐瓶颈（详见 §5.6）；GraalJS Context 不池化放大冷启动开销。
4. **写路径 fire-and-forget**：execution_record/step 写库全 `subscribe()` 后不管，异常/抖动时执行记录可能丢失；`finalizeExecutionRecord` 补录状态与记录初始化存在时间窗。
5. **凭据体系半成品**：SysTokenResolver 与 SYSTOKEN/AKSK/SOA/APIG supplier 均为 stub + `@StandardTodo`，SIGNATURE/COOKIE 才完整；当前开发环境认证强度有限。
6. **Global flow timeout 未真正生效**：`flowConfig.timeoutMs` 被解析携带，但当前生效链路只做节点级超时，未见整流超时强制；超长 DAG 可能远超单节点超时。
7. **入站限流只覆盖 invoke 路径**：`extractFlowId` 只匹配 `…/api/v1/{trigger|flows}/{flowId}/invoke`；debug 接口与其它路径不参与限流，且限流配置取自 60s 缓存（最大 1s 失效延迟）。
8. **小瑕疵**：`triggerType` 语义在 FlowInvokeService 与 DebugService 两处注释不一致；`ParallelBranchExecutor` 内 `executeBranch` 的 `reduceWith` 实为占位；部分 Repository/CacheKeyResolver/ConnectorVersionEntity 为保留旁路/兼容代码。

---

## 七、结论

connector-api 是一个面向「连接流同步执行」场景、设计干净的响应式运行时引擎：以"编排 JSON 快照 + DAG 调度 + 节点执行器插件体系 + 双写留痕"为骨架，以 Redis（缓存/限流/集群友好 Lua）与 MySQL（只读配置/写执行审计）为两翼，通过 open-server 的 debug-proxy 与 FlowCacheEvictor 完成生态咬合。

**最值得借鉴**：设计/运行读写分离 + 编排自包含 + Lua 原子化 + 日志脱敏/错误码内聚/脚本纵深沙箱的组合。

**最需要治理**：脚本节点同步孤岛（§5）与脚本 HTTP 阻塞瓶颈、引擎双轨并存、失败不中断语义、以及认证凭据体系的 stub 化现状。

---

*本文档由代码级现状分析生成，供架构认知与后续治理参考。*
