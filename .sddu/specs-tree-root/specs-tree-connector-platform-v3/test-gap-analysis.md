# 测试缺口分析：连接器平台 V3

**Feature ID**: CONN-PLAT-002  
**分析日期**: 2026-06-22  
**分析范围**: V3 全部 14 个构建任务涉及的 open-server + connector-api 模块  
**已有测试总数**: 35 个（open-server 26 个 + connector-api 9 个）  
**V3 新增源码类**: 188 个（open-server 131 + connector-api 57）  

---

## 一、已有测试覆盖（34 个类覆盖）

### 1.1 open-server — V3 已有测试覆盖（13 个类）

| V3 源码类 | 测试文件 | 覆盖领域 | 覆盖度 |
|-----------|---------|---------|:---:|
| ConnectorController | ConnectorControllerTest + ConnectorControllerWebMvcTest | 连接器实体 CRUD API | 🟢 高 |
| ConnectorService | ConnectorServiceTest | 连接器实体业务逻辑 | 🟢 高 |
| FlowController | FlowControllerTest + FlowControllerWebMvcTest | 连接流实体 CRUD API | 🟢 高 |
| FlowService | FlowServiceTest | 连接流实体业务逻辑 | 🟢 高 |
| ApprovalController | ApprovalControllerTest | 审批流模板 API | 🟢 中 |
| ApprovalService | ApprovalServiceTest | 审批业务逻辑 | 🟢 中 |
| ApiController | ApiControllerTest | API 管理端点 | 🟢 中 |
| ApiService | ApiServiceTest | API 管理逻辑 | 🟢 中 |
| CategoryController | CategoryControllerTest | 分类管理 API | 🟢 中 |
| CategoryService | CategoryServiceTest | 分类业务逻辑 | 🟢 中 |
| CallbackController | CallbackControllerTest | 回调管理 API | 🟢 中 |
| CallbackService | CallbackServiceTest | 回调业务逻辑 | 🟢 中 |
| EventService | EventServiceTest | 事件服务逻辑 | 🟢 中 |
| PermissionController | PermissionControllerTest | 权限分配 API | 🟢 中 |
| PermissionService | PermissionServiceTest | 权限服务逻辑 | 🟢 中 |
| SyncController | SyncControllerTest | 同步管理 API | 🟢 中 |
| SyncService | SyncServiceTest | 同步业务逻辑 | 🟢 中 |
| EmergencyService | EmergencyServiceTest | 紧急同步逻辑 | 🟢 中 |
| OpDebugProxyController | DebugProxyControllerWebMvcTest | 调试代理 WebMvc | 🟢 高 |
| OpDebugProxyService | DebugProxyTest | 调试代理逻辑 | 🟢 高 |
| (common) Jackson ObjectMapper | JacksonDeserializationTest | JSON 序列化配置 | 🟢 低 |
| (common) GlobalExceptionHandlerV2 | GlobalExceptionHandlerV2Test | 全局异常处理 | 🟢 低 |
| (common) BusinessException | BusinessExceptionTest | 业务异常类 | 🟢 低 |
| (common) HealthController | HealthControllerTest | 健康检查端点 | 🟢 低 |

> 注：非 V3 特异的已测类（Api/Category/Callback/Event/Permission/Sync）也列入，因 V3 编排可能引用其 API/Callback 连接器节点。

### 1.2 connector-api — V3 已有测试覆盖（9 个类）

| V3 源码类 | 测试文件 | 覆盖领域 | 覆盖度 |
|-----------|---------|---------|:---:|
| OpTriggerController | TriggerControllerWebFluxTest | HTTP 触发入口 | 🟢 高 |
| OpTriggerService | TriggerServiceTest | 触发业务逻辑 | 🟢 高 |
| ExecutionContext | ExecutionContextTest | 执行上下文封装 | 🟢 中 |
| ReactiveSequentialExecutor | ReactiveSequentialExecutorTest | Reactor 顺序执行器 | 🟢 中 |
| ConnectorNodeExecutor | NodeExecutorsTest | 连接器节点执行器 | 🟢 中 |
| DataProcessorExecutor | NodeExecutorsTest | 数据处理执行器 | 🟢 中 |
| TriggerNodeExecutor | NodeExecutorsTest | 触发器节点执行器 | 🟢 中 |
| ExitNodeExecutor | NodeExecutorsTest | 出口节点执行器 | 🟢 中 |
| ContractSchema | ContractSchemaTest | 契约 Schema 校验 | 🟢 低 |
| RateLimitFilter | RateLimitFilterTest | 限流过滤器 | 🟢 中 |

---

## 二、测试缺口（按 TASK 分组）

### TASK-004: 连接器管理 API — 缺口 2 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **ConnectorVersionService** | open-server | **P1** | 版本 CRUD 核心逻辑：创建草稿、发布校验（JSON/URL 正则/业务必填）、复制到草稿、失效/恢复/删除、版本上限 1000 校验、状态联动 |
| **ConnectorVersionController** | open-server | **P1** | 版本管理 REST 端点（#8~#16），含 HTTP 请求绑定、`@Valid` 校验、响应序列化、错误码 |

> ✅ 已有：ConnectorController（实体 CRUD）+ ConnectorService 已测

### TASK-005: 连接流管理 API — 缺口 5 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **FlowDeployService** | open-server | **🔴 P0** | 部署核心逻辑：版本绑定、部署前校验、deployed_version_id 更新。用户确认"连接流部署的测试没有" |
| **FlowVersionService** | open-server | **🔴 P0** | 版本管理核心 + 发布校验：创建草稿、更新草稿、发布（9 项校验）、复制到草稿、撤回、催办、失效/恢复/删除 |
| **FlowVersionController** | open-server | **🔴 P0** | 版本管理 REST 端点（#28~#38），含审批提交/撤回/催办入口，所有 V3 核心流程入口 |
| **FlowPublishValidator** | open-server | **🔴 P0** | 发布时 9 项校验逻辑：业务必填、编排非空、入站限流上限、节点超时上限、缓存 TTL 上限、并行分支上限、连接器版本可用性、JSON 语法、脚本语法 |
| **FlowCopyService** | open-server | **P1** | 连接流一键复制：完整版本历史复制、名称追加 `_copy_xxxxx`、状态初始化 |

> ✅ 已有：FlowController（实体 CRUD）+ FlowService 已测

### TASK-006: 安全准入拦截器 — 缺口 3 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **AppWhitelistService** | open-server | **P1** | 应用白名单准入：market-server Lookup 调用、白名单校验、降级放行策略 |
| **AppWhitelistInterceptor** | open-server | **P1** | WebMvc 拦截器：路径匹配 `/connectors/**` 和 `/flows/**`、403 响应、X-App-Id 提取 |
| **AppDataIsolationAspect** | open-server | **P1** | AOP 数据隔离：创建时 app_id 注入、查询时 app_id 过滤、跨应用访问拒绝 |

### TASK-007: 版本发布审批集成 — 缺口 3 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **FlowVersionApprovalService** | open-server | **P1** | 审批实例创建、三级审批状态流转（待审批→审批中→已发布/已驳回/已撤回）、催办通知 |
| **ApprovalCallbackHandler** | open-server | **P1** | 审批回调处理：通过/驳回回调触发 FlowVersion 状态变更（businessType=connector_flow_version_publish） |
| **ApprovalEngine（V3 扩展）** | open-server | **P2** | connector_flow_version_publish 场景注册与回调路由（现有 ApprovalServiceTest 可能部分覆盖） |

### TASK-008: 运行时引擎核心 — 缺口 6 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **FlowRuntimeEngine** | connector-api | **🔴 P0** | 运行时引擎总入口：5 阶段管道编排、异常处理、HTTP 状态码映射。这是整个 V3 运行时最核心的类 |
| **VersionConfigResolver** | connector-api | **🔴 P0** | Phase 2 核心：版本配置解析、Redis/MySQL 双数据源 Cache-Aside、连接器版本快照加载、异常映射（503/500/节点失败） |
| **DagScheduler** | connector-api | **🔴 P0** | DAG 调度器：串行（flatMap）与并行（Flux.merge）调度、节点超时控制、Reactor 流编排 |
| **ParallelBranchExecutor** | connector-api | **P1** | 并行分支执行：各分支独立超时、错误不扩散、全部完成后汇聚 |
| **FlowConfigParser** | connector-api | **P1** | flowConfig 解析：超时/限流/缓存配置提取、解析失败默认值回退、告警记录 |
| **EntityCacheManager** | connector-api | **P1** | 平台配置缓存：Redis 优先读取、miss 回源 MySQL 回写、TTL 7d±2h、版本变更主动失效 |

> ✅ 已有：ExecutionContext + ReactiveSequentialExecutor + 4 个 NodeExecutors 已测

### TASK-009: 运行时认证注入器 + 安全校验 — 缺口 5 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **CookieCredentialInjector** | connector-api | **P1** | Cookie 凭证注入：Cookie 名称匹配、编排映射值读取、HTTP Header 注入 |
| **DigitalSignCredentialInjector** | connector-api | **P1** | 数字签名注入：Secret Key 读取、签名算法计算、Header/Query 位置注入 |
| **MultiAuthCredentialInjector** | connector-api | **P1** | 多认证组合：authConfigs[] 遍历、各认证器排序叠加 |
| **UrlWhitelistValidator** | connector-api | **P1** | URL 白名单运行时校验：组合正则编译（Caffeine 缓存 5min）、一次 Matcher.matches()、空白名单放行 |
| **SystokenWhitelistValidator** | connector-api | **P1** | SYSTOKEN 白名单校验：Phase 3 核心、空白名单全部禁止（401）、令牌比对 |

### TASK-010: 运行时限流拦截器与缓存管理器 — 缺口 4 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **InboundRateLimiter** | connector-api | **P1** | 入站限流 WebFilter：QPS 令牌桶（Redis Lua 原子）、并发计数器、429 响应 + Retry-After、Redis 降级放行 |
| **FlowCacheManager** | connector-api | **P1** | 业务缓存管理：Phase 5 首步、缓存键构造、命中跳过 DAG、未命中执行后回写、TTL 设置、版本变更主动清空 |
| **CacheKeyResolver** | connector-api | **P2** | 缓存键解析：keyTemplate 表达式解析、上下文动态值拼接 |
| **RateLimitConfigReader** | connector-api | **P2** | 限流配置读取：min(流配置值, 应用最大限流值)、market-server Property 查询 |

### TASK-011: 运行时脚本节点执行器 — 缺口 4 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **ScriptNodeExecutor** | connector-api | **P1** | GraalJS 脚本执行：`main(ctx)` 调用、boundedElastic 线程隔离、超时强制终止、类型映射 |
| **GraalJsContextFactory** | connector-api | **P1** | GraalJS Context 工厂：沙箱五层纵深防御配置、ES2022 严格模式、资源限制（statementLimit=10000） |
| **CtxAssembler** | connector-api | **P2** | 上下文组装：上游节点 input/output 嵌套 Map 组装、指针引用 |
| **ScriptExecutionConfig** | connector-api | **P2** | 脚本执行配置：线程池参数、超时值、源码长度限制 |

### TASK-012: 执行记录与步骤日志 — 缺口 7 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **ExecutionRecordService** | connector-api | **P1** | 执行记录写入：异步写入不阻塞、trigger_type 标记、脱敏后写入、FIFO 清理 |
| **ExecutionStepService** | connector-api | **P1** | 步骤日志写入：各节点 I/O 耗时记录、日志开关控制 |
| **LogSanitizer** | connector-api | **P2** | 日志脱敏：password/token/secretKey/signSecretKey 字段脱敏为 `"***"` |
| **ExecutionCleanupJob** | connector-api | **P2** | 定时清理任务：30 天清理（先 step 后 record，分批 1000）、单流 FIFO 上限清理 |
| **ExecutionRecordService** | open-server | **P1** | 执行记录查询：按 flowId/status/triggerType/时间范围分页、详情含 steps 数组 |
| **ExecutionRecordController** | open-server | **P1** | 执行记录 REST 端点（#49~#50）：列表查询、详情查询、HTTP 参数绑定 |

### TASK-013: 调试执行通道 — 缺口 2 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **OpTestRunService** | connector-api | **P2** | 调试执行服务：草稿/已发布版本调试、独立线程池（max 5）、30s 超时、trigger_type=2 |
| **OpTestRunController** | connector-api | **P2** | 调试执行端点（#53）：同步返回各节点执行详情、已失效版本拒绝（EC-014） |

> ✅ 已有：open-server 侧 OpDebugProxyController + OpDebugProxyService 已测（兼容 V2 调试代理）

### TASK-014: 操作日志扩展 — 缺口 2 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **EntitySnapshotLoader** | open-server | **P2** | 实体快照加载：连接器/连接流变更前/后关键字段快照 |
| 操作日志 AOP（ConnectorOperateLogAspect / FlowOperateLogAspect） | open-server | **P2** | 切面拦截：创建/编辑/删除/发布等操作埋点，日志字段完整性 |

> 注：AOP 切面类可能尚未独立创建（任务描述为 NEW），操作日志可能在现有 OperateLog 框架内以手动埋点方式实现。

### 开放平台操作端（Op* 类）— 缺口 4 个

| 缺口类 | 所属模块 | 优先级 | 原因 |
|--------|---------|:---:|------|
| **OpConnectorController** | open-server | **P2** | 运营端连接器管理 API（与用户端 ConnectorController 功能重叠但入口不同） |
| **OpConnectorService** | open-server | **P2** | 运营端连接器业务逻辑 |
| **OpFlowController** | open-server | **P2** | 运营端连接流管理 API |
| **OpFlowService** | open-server | **P2** | 运营端连接流业务逻辑 |

---

## 三、优先级汇总

| 优先级 | 缺口数 | 说明 | 涉及 TASK |
|:---:|:---:|------|---------|
| **🔴 P0** | **6** | 核心业务逻辑无任何测试，包括运行时引擎入口、DAG 调度、版本解析、版本管理、发布校验、部署服务 | TASK-005 (3), TASK-008 (3) |
| **🟡 P1** | **24** | 重要功能无测试，包括版本 Controller、认证注入器、安全校验器、限流缓存、脚本执行、审批集成、执行记录等 | TASK-004 (2), TASK-005 (1), TASK-006 (3), TASK-007 (2), TASK-008 (3), TASK-009 (5), TASK-010 (2), TASK-011 (2), TASK-012 (4) |
| **🟢 P2** | **13** | 辅助/基础设施类无测试，包括缓存键解析、限流配置读取、脱敏器、清理任务、脚本辅助类、调试通道、操作日志、Op* 端 | TASK-007 (1), TASK-010 (2), TASK-011 (2), TASK-012 (3), TASK-013 (2), TASK-014 (3) |
| **总计** | **43** | — | — |

### P0 详细清单（6 个必须立即补充测试）

| # | 类名 | 模块 | 对应 TASK | 核心风险 |
|---|------|------|:---:|------|
| 1 | **FlowDeployService** | open-server | TASK-005 | 部署逻辑错误会导致生产连接流无法调用（503） |
| 2 | **FlowVersionService** | open-server | TASK-005 | 版本发布 9 项校验缺失任一项导致非法版本发布到生产 |
| 3 | **FlowPublishValidator** | open-server | TASK-005 | 独立的 9 项校验逻辑无覆盖，回归风险极高 |
| 4 | **FlowRuntimeEngine** | connector-api | TASK-008 | 5 阶段管道入口，任何阶段异常处理错误直接影响线上调用 |
| 5 | **VersionConfigResolver** | connector-api | TASK-008 | 版本配置解析 + 双数据源 Cache-Aside，Redis/MySQL 任一降级场景未覆盖 |
| 6 | **DagScheduler** | connector-api | TASK-008 | Reactor 流编排核心，串行/并行/超时逻辑错误导致请求卡死或丢失 |

---

## 四、测试覆盖统计

### 按模块

| 模块 | 源码类 | 已测类 | 缺口 | 覆盖率 |
|------|:---:|:---:|:---:|:---:|
| open-server | 131 | ~15 | 24 | ~18% |
| connector-api | 57 | ~9 | 19 | ~33% |
| **合计** | **188** | **~24** | **~43** | **~23%** |

### 按 TASK

| TASK | 名称 | 关键类数 | 已测 | 缺口 |
|:---:|------|:---:|:---:|:---:|
| TASK-004 | 连接器管理 API | 4 | 2 | 2 |
| TASK-005 | 连接流管理 API | 7 | 2 | 5 |
| TASK-006 | 安全准入拦截器 | 3 | 0 | 3 |
| TASK-007 | 版本发布审批集成 | 3 | 0 | 3 |
| TASK-008 | 运行时引擎核心 | 7 | 1 | 6 |
| TASK-009 | 认证注入器 + 安全 | 5 | 0 | 5 |
| TASK-010 | 限流拦截器与缓存 | 4 | 0 | 4 |
| TASK-011 | 脚本节点执行器 | 4 | 0 | 4 |
| TASK-012 | 执行记录与步骤日志 | 7 | 0 | 7 |
| TASK-013 | 调试执行通道 | 2 | 0 | 2 |
| TASK-014 | 操作日志扩展 | 3 | 0 | 3 |

> 注：TASK-001（DB Schema）、TASK-002（实体持久层）、TASK-003（枚举常量）为基础设施层，其 Mapper/Entity/Enum 测试在 tasks.md 中已规划但实际测试文件中未找到对应测试类。这些暂不列入缺口（可通过上层测试间接覆盖）。

---

## 五、建议测试补充优先级

### 🔴 第一优先（P0）：本周必须完成
1. **TASK-008P0**: FlowRuntimeEngine + VersionConfigResolver + DagScheduler 联合测试（运行时引擎核心 3 件套）
2. **TASK-005P0**: FlowDeployService + FlowVersionService + FlowPublishValidator（部署/版本/校验 3 件套）

### 🟡 第二优先（P1）：两周内完成
3. **TASK-004/005** Controller 层测试：ConnectorVersionController、FlowVersionController（WebMvc 切片测试）
4. **TASK-009** 安全相关：UrlWhitelistValidator + SystokenWhitelistValidator + 3 个 CredentialInjector
5. **TASK-006** 准入相关：AppWhitelistService + AppDataIsolationAspect

### 🟢 第三优先（P2）：后续迭代
6. 限流缓存（TASK-010）、脚本沙箱（TASK-011）、执行记录（TASK-012）、调试通道（TASK-013）、操作日志（TASK-014）

---

## 六、Python 集成测试缺口分析

### 6.1 现有 Python 测试覆盖

**open-server 现有 23 个**（位于 `open-server/src/test/python/inspect/`）：
- `client.py` — HTTP 客户端封装（BASE_URL = `http://localhost:18080/open-server`）
- `connector_create.py`, `connector_detail.py`, `connector_update.py`, `connector_delete.py`, `connector_list.py` — 连接器 CRUD
- `connector_config_get.py`, `connector_config_set.py` — 连接器配置读写
- `flow_create.py`, `flow_detail.py`, `flow_update.py`, `flow_delete.py`, `flow_list.py` — 连接流 CRUD
- `flow_config_get.py`, `flow_config_set.py` — 连接流配置读写
- `flow_start.py`, `flow_stop.py` — 启停操作
- `debug_test_run.py` — V1 调试代理
- `api_delete.py`, `callback_delete.py`, `event_delete.py` — 删除接口
- `all.py` — 全量回归执行器

**connector-api 现有 5 个**（位于 `connector-api/src/test/python/inspect/`）：
- `client.py` — HTTP 客户端封装（BASE_URL = `http://localhost:18180/api/v1`）
- `trigger_invoke.py` — HTTP 触发调用（IT-049~065，含 Mock Server）
- `test_run.py` — 内部测试运行（IT-070~073）
- `contract_response.py` — 契约响应校验
- `all.py` — 全量回归执行器

> **结论**：以上 28 个脚本全部为 V1 遗留测试，V3 新增版本管理/部署/审批/调试/多认证/脚本节点 等核心功能无任何 Python 集成测试覆盖。

### 6.2 缺口场景

| 缺口场景 | 优先级 | 对应 FR | 涉及模块 | 归属任务 |
|---------|:---:|------|------|:---:|
| 连接器版本全生命周期 | 🔴 P0 | FR-005a, FR-006, FR-007, FR-009, FR-010 | open-server | E2E-001 |
| 连接流版本全生命周期（含审批） | 🔴 P0 | FR-024a, FR-026, FR-031 | open-server | E2E-001 |
| 连接流部署→启动→触发调用 端到端 | 🔴 P0 | FR-018, FR-019, FR-042 | open-server + connector-api | E2E-002 |
| 连接流一键复制 | 🔴 P0 | FR-017 | open-server | E2E-002 |
| 连接流停止再启动 | 🔴 P0 | FR-019, FR-020 | open-server | E2E-002 |
| 多认证组合调用 | 🔴 P0 | FR-012, FR-013, FR-014 | connector-api | E2E-003 |
| URL 白名单校验 | 🔴 P0 | FR-015 | connector-api | E2E-003 |
| 脚本节点执行（GraalJS） | 🔴 P0 | FR-040a | connector-api | E2E-003 |
| 草稿版本直接调试 | 🔴 P0 | FR-041 | connector-api | E2E-003 |
| 审批全流程（含驳回重提） | 🟡 P1 | FR-031, FR-032 | open-server | E2E-001 (合并于 flow_version_lifecycle.py) |

### 6.3 计划新增

**计划新增**：10 个 Python 集成测试脚本，分为 3 个 E2E 测试任务（均为 P0 优先级）：

| 测试任务 | 脚本数 | 模块 | 脚本列表 | 覆盖 FR |
|---------|:---:|------|------|------|
| TEST-TASK-E2E-001 | 3 | open-server | `connector_version_lifecycle.py`<br>`flow_version_lifecycle.py`<br>`flow_approval_full_flow.py` | FR-005a/006/007/009/010<br>FR-024a/026/031<br>FR-031/032（审批流转+驳回重提） |
| TEST-TASK-E2E-002 | 3 | open-server | `flow_deploy_start_invoke.py`<br>`flow_copy.py`<br>`flow_stop_restart.py` | FR-018/019/042<br>FR-017<br>FR-019/020 |
| TEST-TASK-E2E-003 | 4 | connector-api | `connector_auth_multiple.py`<br>`connector_url_whitelist.py`<br>`script_node_execution.py`<br>`debug_draft_invoke.py` | FR-012/013/014<br>FR-015<br>FR-040a<br>FR-041 |

**总计**：10 个新增 Python 集成测试脚本，3 个 E2E 测试任务

### 6.4 E2E 任务详细覆盖矩阵

| # | Python 脚本 | 归属模块 | 归属任务 | 优先级 | 覆盖 FR | 测试要点 |
|---|------------|---------|:---:|:---:|------|------|
| 1 | `connector_version_lifecycle.py` | open-server | E2E-001 | 🔴 P0 | FR-005a, FR-006, FR-007, FR-009, FR-010 | 创建草稿→发布→复制→失效→删除 全生命周期 |
| 2 | `flow_version_lifecycle.py` | open-server | E2E-001 | 🔴 P0 | FR-024a, FR-026, FR-031 | 创建草稿→编辑编排→发布→审批→版本列表 |
| 3 | `flow_approval_full_flow.py` | open-server | E2E-001 | 🟡 P1 | FR-031, FR-032 | 三级审批流转（通过/驳回/撤回）+ 驳回后重提 |
| 4 | `flow_deploy_start_invoke.py` | open-server | E2E-002 | 🔴 P0 | FR-018, FR-019, FR-042 | 创建→发布→部署→启动→HTTP触发→运行记录 |
| 5 | `flow_copy.py` | open-server | E2E-002 | 🔴 P0 | FR-017 | 一键复制→名称 _copy_xxxxx→状态已停止→版本历史 |
| 6 | `flow_stop_restart.py` | open-server | E2E-002 | 🔴 P0 | FR-019, FR-020 | 运行中停止→已停止再启动→停止期间触发拒绝 |
| 7 | `connector_auth_multiple.py` | connector-api | E2E-003 | 🔴 P0 | FR-012, FR-013, FR-014 | SOA+Cookie / DigitalSign+Cookie / 三种以上组合认证 |
| 8 | `connector_url_whitelist.py` | connector-api | E2E-003 | 🔴 P0 | FR-015 | URL白名单命中/未命中/空白名单放行 |
| 9 | `script_node_execution.py` | connector-api | E2E-003 | 🔴 P0 | FR-040a | GraalJS 正常执行/超时终止/语法错误拒绝 |
| 10 | `debug_draft_invoke.py` | connector-api | E2E-003 | 🔴 P0 | FR-041 | 草稿调试成功/已发布调试成功/已失效拒绝 EC-014 |

### 6.5 Python E2E 测试执行顺序

```
E2E-001 (版本生命周期)  ──┐
                           ├── 可并行执行（操作不同资源）
E2E-002 (部署启动触发)   ──┤
                           │
E2E-003 (认证安全脚本调试) ─┘
```

- E2E-001 和 E2E-002 均操作 open-server，测试不同功能域（版本管理 vs 运行时），可并行
- E2E-003 操作 connector-api，独立模块，可并行
- 每个 E2E 任务内的脚本有顺序依赖（需先创建资源再操作），建议顺序执行

**总计**：10 个新增 Python 集成测试脚本，3 个 E2E 测试任务

---

*分析完成时间：2026-06-22*  
*数据来源：实际代码扫描 `connector-api/src/main/java` + `open-server/src/main/java` vs 实际测试文件扫描 `src/test/java` + Python 测试目录扫描*
