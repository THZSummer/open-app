# 运行时引擎设计：连接器平台 V2

**Feature ID**: CONN-PLAT-002  
**关联文档**: plan.md（§4.2 connector-api 模块），plan-db.md（§3 表结构），plan-json-schema.md（JSON 结构定义）  
**版本**: v1.0  
**创建日期**: 2026-06-09  
**对齐基线**: spec.md v2.24-draft，ADR-005（限流），ADR-006（运行记录）

---

## 0. 概述

V2 运行时在 V1 的串行调度引擎基础上新增 6 个核心模块：

| 模块 | 触发条件 | 关键能力 |
|------|---------|---------|
| 版本配置解析器 | 每次 HTTP/调试 触发 | 按 `deployed_version_id` 读取 FlowVersion 编排快照，连接器配置直接从节点 `connectorVersionConfig` 快照获取（无需查询 ConnectorVersion） |
| 并行分支执行器 | 编排中包含并行边 | Reactor `Flux.merge()` 并发执行，独立超时 + 错误汇聚 |
| flowConfig 解析器 | 版本配置加载后 | 解析超时/限流/缓存配置，初始化运行环境 |
| 脚本节点执行器 | 编排中包含 script 节点 | GraalJS 沙箱执行 `function main(ctx)`，详见 [plan-script.md](./plan-script.md) |
| 入站限流拦截器 | HTTP 触发请求到达 | Redis 令牌桶（QPS）或并发计数器（Concurrency） |
| 认证注入器扩展 | 连接器 HTTP 调用 | Cookie/DigitalSign/MultiAuth 注入器注册到现有 Strategy 模式 |

### 0.1 运行时整体架构

```mermaid
graph TB
    subgraph INBOUND["入站请求"]
        HTTP["HTTP Request"]
        CRED["CredentialValidator<br/>🔑 凭证格式/有效期校验"]
    end

    subgraph RESOLVE["Phase 2 · 连接流存在性判断"]
        VCR["VersionConfigResolver<br/>🔍 存在性 & 版本快照"]
        FCP["FlowConfigParser<br/>⚙️ 解析白名单/超时/限流/缓存"]
    end

    subgraph AUTHZ["Phase 3 · 触发器鉴权"]
        AUTH_TRIG["SystokenValidator<br/>🔐 凭证 vs 白名单"]
    end

    subgraph GATE["Phase 4 · 连接流限流"]
        RATE["InboundRateLimiter<br/>⚡ 按流 QPS / 并发"]
    end

    subgraph EXEC["Phase 5 · 缓存 + 节点调度"]
        CACHE{"FlowCacheManager<br/>💾 结果缓存?"}
        DAG["DAG 调度器"]
        PBE["ParallelBranchExecutor<br/>⇉ 并行分支"]
        DPE["ScriptNodeExecutor<br/>🖊️ GraalJS 脚本"]
    end

    subgraph CONNECTOR["连接器调用"]
        AUTH["Auth Injectors<br/>🔐 连接器认证注入"]
        URL_VAL["UrlWhitelistValidator<br/>🛡️ URL 白名单"]
        HTTP_CALL["Connector HTTP Call"]
    end

    subgraph CROSSCUT["横切关注点"]
        LOG["ExecutionRecord<br/>📝 运行日志（异步）"]
        DEBUG["DebugExecutionService<br/>🐛 调试执行"]
    end

    HTTP --> CRED
    CRED -->|"凭证有效"| VCR
    CRED -->|"无效"| UNAUTH["❌ 401"]
    VCR --> FCP
    FCP --> AUTH_TRIG
    AUTH_TRIG -->|"通过"| RATE
    AUTH_TRIG -->|"拒绝"| FORBIDDEN["❌ 403"]
    RATE -->|"通过"| CACHE
    RATE -->|"超限"| REJECT["❌ 429"]
    CACHE -->|"命中"| RESP["✅ 缓存响应"]
    CACHE -->|"未命中"| DAG
    DAG --> PBE
    DAG --> DPE
    DAG --> AUTH
    AUTH --> URL_VAL
    URL_VAL --> HTTP_CALL
    HTTP_CALL -.->|"异步"| LOG
    DEBUG -.->|"同步（独立线程池）"| DAG

    style INBOUND fill:#e8eaf6,stroke:#283593
    style RESOLVE fill:#fff3e0,stroke:#e65100
    style AUTHZ fill:#ede7f6,stroke:#4527a0
    style GATE fill:#fce4ec,stroke:#c62828
    style EXEC fill:#e8f5e9,stroke:#2e7d32
    style CONNECTOR fill:#fce4ec,stroke:#880e4f
    style CROSSCUT fill:#f3e5f5,stroke:#4a148c
    style REJECT fill:#ffccbc,stroke:#b71c1c
    style UNAUTH fill:#ffccbc,stroke:#b71c1c
    style FORBIDDEN fill:#ffccbc,stroke:#b71c1c
```

---

## 运行时执行全流程

> 本节以**一次完整的连接流 HTTP 调用**为视角，按真实请求处理链路串联全部运行时模块。
> 日志（§7）贯穿全流程异步写入，属横切关注点，不占独立阶段。

### 流程总览

```mermaid
flowchart TD
    START(["📥 HTTP Request"]) --> P1{"Phase 1<br/>🔑 凭证认证<br/>（纯校验，无 DB/Redis）"}
    P1 -->|"无效凭证"| R1(["❌ 401 Unauthorized"])
    P1 -->|"凭证有效"| P2{"Phase 2<br/>🔍 连接流存在性判断"}
    P2 -->|"不存在/未部署"| R2(["❌ 503 Flow not deployed"])
    P2 -->|"存在且已部署"| P2b["解析 flowConfig<br/>（白名单 / 超时 / 限流 / 缓存）"]
    P2b --> P3{"Phase 3<br/>🔐 触发器鉴权<br/>（凭证 vs 白名单）"}
    P3 -->|"不在白名单"| R3(["❌ 403 Forbidden"])
    P3 -->|"鉴权通过"| P4{"Phase 4<br/>⚡ 连接流限流<br/>（按流 QPS / 并发）"}
    P4 -->|"超限"| R4(["❌ 429 Too Many Requests"])
    P4 -->|"通过"| P5{"Phase 5<br/>💾 缓存处理 + ⚙️ 执行"}
    P5 -->|"缓存命中"| R5a(["✅ 返回缓存"])
    P5 -->|"未命中 → 执行"| R5(["📤 返回结果"])

    style P1 fill:#e8eaf6,stroke:#283593
    style P2 fill:#fff3e0,stroke:#e65100
    style P3 fill:#ede7f6,stroke:#4527a0
    style P4 fill:#fce4ec,stroke:#c62828
    style P5 fill:#e8f5e9,stroke:#2e7d32
    style R1 fill:#ffccbc,stroke:#b71c1c
    style R2 fill:#ffccbc,stroke:#b71c1c
    style R3 fill:#ffccbc,stroke:#b71c1c
    style R4 fill:#ffccbc,stroke:#b71c1c
    style R5a fill:#c8e6c9,stroke:#1b5e20
    style R5 fill:#c8e6c9,stroke:#1b5e20
```

### 时序视角

下图为同一次调用的**组件交互时序**，按 Phase 用 `rect` 分区着色，省略异常分支（异常出口见上方 flowchart 和五阶段详解表）。

> 💡 **Phase 2 的数据读取路径**：运行时通过 `EntityCacheManager`（平台配置缓存，详见 [plan-cache §12](./plan-cache.md#12-part-2--平台配置缓存)）以 **Cache-Aside** 模式读取实体——优先 Redis `cp:entity:*`，miss 时回源 MySQL 并回写 Redis。理想全命中场景下，一次 `MGET` 即可取得全部版本快照，**完全跳过 MySQL**。

```mermaid
sequenceDiagram
    participant Client
    participant Runtime as "FlowRuntimeEngine"
    participant VCR as "VersionConfigResolver"
    participant EC as "EntityCacheManager<br/>（平台配置缓存）"
    participant Limiter as "InboundRateLimiter"
    participant Auth as "SystokenValidator"
    participant Cache as "FlowCacheManager<br/>（业务结果缓存）"
    participant DAG as "DAG Scheduler"
    participant Executor as "Node Executors"
    participant Redis
    participant DB

    Client->>Runtime: HTTP Request(flowId)

    rect rgb(232, 234, 246)
        Note over Runtime,Auth: Phase 1 · 凭证认证（纯校验，无 DB/Redis）
        Runtime->>Auth: validateCredential(token)
        Auth->>Auth: 校验凭证格式 / 有效期
        Auth-->>Runtime: 凭证有效
    end

    rect rgb(255, 243, 224)
        Note over Runtime,DB: Phase 2 · 连接流存在性判断
        Runtime->>VCR: resolve(flowId)
        VCR->>EC: 读取 Flow 实体
        EC->>Redis: GET cp:entity:flow:{flowId}
        alt 命中
            Redis-->>EC: Flow JSON (含 deployed_version_id, status)
        else miss → 回源 DB → 回写 Redis
            EC->>DB: SELECT flow_t
            DB-->>EC: Flow row
            EC->>Redis: SET (TTL 7d ± 2h)
        end
        EC-->>VCR: Flow 实体

        VCR->>EC: 读取 FlowVersion 实体
        EC->>Redis: GET cp:entity:flowversion:{versionId}
        alt 命中
            Redis-->>EC: FlowVersion JSON (含 orchestration_config MEDIUMTEXT)
        else miss
            EC->>DB: SELECT flow_version_t
            DB-->>EC: FlowVersion row
            EC->>Redis: SET (TTL 7d ± 2h)
        end
        EC-->>VCR: FlowVersion 实体

        VCR->>VCR: 解析 flowConfig（白名单/超时/限流/缓存）
        VCR-->>Runtime: ResolvedConfig
    end

    rect rgb(237, 231, 246)
        Note over Runtime,Auth: Phase 3 · 触发器鉴权（凭证 vs 白名单）
        Runtime->>Auth: authorize(token, flowConfig.systokenWhitelist)
        Auth->>Auth: 校验 token 是否在白名单中
        Auth-->>Runtime: 鉴权通过
    end

    rect rgb(252, 228, 236)
        Note over Runtime,Redis: Phase 4 · 连接流限流
        Runtime->>Limiter: check(rateLimit)
        Limiter->>Redis: EVAL Lua 令牌桶 / INCR 并发计数
        Redis-->>Limiter: ok
        Limiter-->>Runtime: 放行
    end

    rect rgb(232, 245, 233)
        Note over Runtime,Executor: Phase 5 · 缓存处理 + 节点调度执行
        Runtime->>Cache: check(cacheConfig)
        Cache->>Cache: 解析 key[] → 构造缓存键
        Cache->>Redis: GET cp:cache:{flowId}:{versionNumber}:{dynamicKey}
        alt 缓存命中
            Redis-->>Cache: 缓存值
            Cache-->>Runtime: 命中，返回缓存
        else 未命中
            Cache-->>Runtime: 继续执行
            Runtime->>DAG: execute(config, triggerData)
            loop 遍历编排节点
                DAG->>Executor: 按类型分发
                Executor-->>DAG: 节点输出
            end
            DAG-->>Runtime: 执行结果
        end
    end

    Runtime-->>Client: 📤 返回响应
```

### 五阶段详解

| Phase | 负责模块 | 核心逻辑 | 异常出口 | 详见 |
|-------|---------|---------|---------|------|
| **1. 凭证认证** | CredentialValidator | 校验凭证格式与有效期，**纯内存操作**，不查 DB/Redis。无 flow 相关逻辑 | `401`（无效凭证） | — |
| **2. 连接流存在性判断** | VersionConfigResolver | flowId → EntityCache → FlowVersion 编排快照（含 connectorVersionConfig）；解析 `flowConfig`（白名单 / 超时 / 限流 / 缓存） | `503`（未部署）/ `500`（版本失效） | §1, §3 |
| **3. 触发器鉴权** | SystokenWhitelistValidator | Phase 2 拿到白名单后，校验凭证是否在 `trigger.authConfig.systokenWhitelist` 中 | `403`（不在白名单 / 白名单为空） | §10 |
| **4. 连接流限流** | InboundRateLimiter | 按流读取 `flowConfig.rateLimit`，Redis 令牌桶（QPS）或并发计数器 | `429`（超限）/ 降级放行 | §5 |
| **5. 缓存处理 + 节点调度执行** | FlowCacheManager + DAG + PBE + DPE + Auth Injector + URL Validator | 缓存命中 → 直接返回；未命中 → DAG 遍历执行 | 节点级错误 → 降级/标记失败 | §6, §2, §4, §9, §11 |

### Phase 5 节点级调度细节

Phase 5 的 DAG 调度器按编排边关系遍历节点，根据节点类型分发到对应执行器：

```mermaid
flowchart TD
    DAG_START["DAG 调度器<br/>取下一个节点"] --> NODE_TYPE{"节点类型?"}

    NODE_TYPE -->|"connector"| AUTH["🔐 Auth Injector<br/>认证凭证注入"]
    NODE_TYPE -->|"script"| SCR["🖊️ ScriptNodeExecutor<br/>GraalJS 沙箱"]
    NODE_TYPE -->|"并行分支"| PAR["⇉ ParallelBranchExecutor<br/>并发调度各分支"]

    AUTH --> URL_CHK["🛡️ UrlWhitelistValidator<br/>正则匹配目标 URL"]
    URL_CHK --> HTTP_CALL["🌐 HTTP 调用外部系统"]
    HTTP_CALL --> NEXT{"还有下游节点?"}

    DP --> CONVERT["toString / toNumber<br/>toBoolean / formatDate"]
    CONVERT --> NEXT

    PAR --> BRANCHES["各分支独立执行<br/>⏱ 独立超时 + 🛡️ 错误隔离"]
    BRANCHES --> MERGE["汇聚结果按 nodeId 组织"]
    MERGE --> NEXT

    NEXT -->|"是"| DAG_START
    NEXT -->|"否"| DAG_DONE["✅ 执行完成"]

    style AUTH fill:#fce4ec,stroke:#880e4f
    style DP fill:#e8f5e9,stroke:#2e7d32
    style PAR fill:#e1f5fe,stroke:#01579b
    style DAG_DONE fill:#c8e6c9,stroke:#1b5e20
```

### 关键决策点汇总

| 决策点 | 阶段 | 决策逻辑 |
|--------|------|---------|
| 凭证是否有效？ | Phase 1 | 格式 + 有效期校验（纯内存）→ **通过**；无效 → `401`，不触发后续任何逻辑 |
| 流是否存在？ | Phase 2 | `deployed_version_id` 非空 且 版本快照存在 → **继续** |
| 凭证是否在白名单？ | Phase 3 | 白名单非空且包含当前 token → **通过**；否则 → `403` |
| 是否触发限流？ | Phase 4 | 令牌桶/并发超限 → **429**；Redis 不可用 → **降级放行** |
| 跳过 DAG 执行？ | Phase 5 | `cache.enabled=true` 且缓存键命中 → **返回缓存**；否则 → DAG 执行 |
| 单节点超时多久？ | Phase 5 | `min(节点配置超时, 应用级最大超时)`——上限由平台管理员控制 |

---

## 1. 版本配置解析器（VersionConfigResolver）

### 1.1 设计

```mermaid
sequenceDiagram
    participant Client
    participant TriggerHandler
    participant VersionConfigResolver
    participant EC as "EntityCacheManager"
    participant Redis
    participant DB
    participant FlowRuntimeEngine

    Client->>TriggerHandler: HTTP Request(flowId)
    TriggerHandler->>VersionConfigResolver: resolve(flowId)

    VersionConfigResolver->>EC: 读取 Flow 实体
    EC->>Redis: GET cp:entity:flow:{flowId}
    alt 缓存命中
        Redis-->>EC: Flow JSON (含 deployed_version_id)
    else 缓存未命中
        EC->>DB: SELECT flow_t
        DB-->>EC: Flow row
        EC->>Redis: SET (TTL 7d ± 2h)
    end
    EC-->>VersionConfigResolver: Flow 实体

    VersionConfigResolver->>EC: 读取 FlowVersion 实体
    EC->>Redis: GET cp:entity:flowversion:{versionId}
    alt 缓存命中
        Redis-->>EC: FlowVersion JSON (含 orchestration_config，connector 节点已含 connectorVersionConfig 快照)
    else 缓存未命中
        EC->>DB: SELECT flow_version_t
        DB-->>EC: FlowVersion row
        EC->>Redis: SET (TTL 7d ± 2h)
    end
    EC-->>VersionConfigResolver: FlowVersion 实体

    Note over VersionConfigResolver: 遍历编排 nodes[]，直接从 node.data.connectorVersionConfig 取连接器配置<br/>（编排自包含，无需查询 ConnectorVersion）

    VersionConfigResolver-->>TriggerHandler: ResolvedConfig {flowVersion}
    TriggerHandler->>FlowRuntimeEngine: execute(resolvedConfig, triggerData)
```

### 1.2 错误处理

```mermaid
flowchart TD
    START["resolve(flowId)"] --> DEPLOY{"deployed_version_id<br/>是否为 NULL?"}
    DEPLOY -->|"是 NULL"| E503["❌ 503<br/>Flow not deployed"]
    DEPLOY -->|"有值"| LOAD_FV["加载 FlowVersion"]
    LOAD_FV --> FV_EXIST{"FlowVersion<br/>是否存在?"}
    FV_EXIST -->|"不存在/已删除"| E500["❌ 500<br/>Deployed version not found"]
    FV_EXIST -->|"存在"| PARSE["解析 orchestration_config<br/>connector 节点直接从 node.data.connectorVersionConfig 取配置"]
    PARSE --> RETURN["✅ 返回 ResolvedConfig"]

    style E503 fill:#ffccbc
    style E500 fill:#ffccbc
    style RETURN fill:#c8e6c9
```

| 场景 | 处理 |
|------|------|
| `deployed_version_id` 为 NULL | 返回 503 "Flow not deployed" |
| FlowVersion 不存在或已删除 | 返回 500 "Deployed version not found" |
| connectorVersionConfig 快照缺失 | 标记对应节点为失败，记录错误日志（编排快照不完整） |


### 1.3 缓存策略

- FlowVersion 编排配置读后写入 Redis 缓存（含完整的 connectorVersionConfig 快照，无需单独缓存 ConnectorVersion）
- Key: `cp:entity:flowversion:{versionId}`, TTL: 7 天
- 版本切换时主动失效对应缓存

---

## 2. 并行分支执行器（ParallelBranchExecutor）

### 2.1 分支识别

编排保存时，从 FlowEdge 中识别并行边：

```mermaid
flowchart LR
    EDGES["FlowEdge 列表"] --> CHECK{"edge.data<br/>.connectionMode?"}
    CHECK -->|"= parallel"| PARALLEL["⇉ 标记为并行边<br/>同源并行边可并发执行"]
    CHECK -->|"= serial（默认）"| SERIAL["→ 标记为串行边"]
```

### 2.2 执行模型

```mermaid
flowchart TD
    START["ParallelBranchExecutor<br/>.execute(ctx, branches)"] --> SPLIT["Flux.fromIterable(branches)<br/>（上限 20 分支）"]

    SPLIT --> B1["分支 1"]
    SPLIT --> B2["分支 2"]
    SPLIT --> BN["..."]
    SPLIT --> B20["分支 N"]

    B1 --> B1_T{"独立超时<br/>min(节点配置, 30s)"}
    B2 --> B2_T{"独立超时<br/>min(节点配置, 30s)"}
    BN --> BN_T{"..."}
    B20 --> B20_T{"独立超时<br/>min(节点配置, 30s)"}

    B1_T -->|"完成"| R1["成功/失败"]
    B1_T -->|"超时/异常"| ERR1["onErrorResume<br/>→ branchFailed"]
    B2_T -->|"完成"| R2["成功/失败"]
    B2_T -->|"超时/异常"| ERR2["onErrorResume<br/>→ branchFailed"]
    BN_T -->|"完成"| RN["成功/失败"]
    B20_T -->|"完成"| R20["成功/失败"]
    B20_T -->|"超时/异常"| ERR20["onErrorResume<br/>→ branchFailed"]

    ERR1 --> R1
    ERR2 --> R2
    ERR20 --> R20

    R1 --> MERGE["collectList()<br/>汇聚所有分支结果"]
    R2 --> MERGE
    RN --> MERGE
    R20 --> MERGE

    MERGE --> MERGE_OP["mergeToParallelResult()<br/>按 nodeId 组织"]
    MERGE_OP --> OUTPUT["{ nodeId: { output, status } }"]

    style SPLIT fill:#fff3e0,stroke:#e65100
    style MERGE fill:#e8f5e9,stroke:#1b5e20
    style OUTPUT fill:#e1f5fe,stroke:#01579b
    style ERR1 fill:#ffccbc,stroke:#b71c1c
    style ERR2 fill:#ffccbc,stroke:#b71c1c
    style ERR20 fill:#ffccbc,stroke:#b71c1c
```

### 2.3 关键约束

| 约束 | 说明 |
|------|------|
| 并行分支数上限 | 20（硬限制，防止线程池耗尽） |
| 分支独立超时 | 每分支取 `min(节点超时配置, 30s)` |
| 错误不扩散 | 一个分支失败不影响其他分支执行 |
| 汇聚等待 | 所有分支完成后（成功或失败）才进入下游节点 |
| 结果合并 | 各分支输出按 `nodeId` 组织：`{ nodeId: { output: {...}, status: "success|failed" } }` |

---

## 3. flowConfig 解析器（FlowConfigParser）

### 3.1 flowConfig 结构

```json
{
  "timeout": {
    "perNode": 10,       // 每节点默认超时（秒），0=不限
    "global": 60          // 全流总超时（秒）
  },
  "rateLimit": {
    "mode": "QPS",        // QPS | CONCURRENCY
    "value": 100           // 上限值，0=关闭
  },
  "cache": {
    "enabled": true,
    "keyTemplate": "${$.node.trigger.input.userId}",  // 缓存键表达式
    "ttl": 300            // 缓存 TTL（秒），0=永不过期
  }
}
```

### 3.2 解析时机

```mermaid
sequenceDiagram
    participant Request as "HTTP 触发请求"
    participant FCP as "FlowConfigParser"
    participant FV as "FlowVersion"
    participant EC as "ExecutionContext"

    Request->>FCP: 解析触发
    FCP->>FV: 读取 orchestrationConfig.flowConfig
    FV-->>FCP: JSON {timeout, rateLimit, cache}

    alt 解析成功
        FCP->>EC: 注入超时/限流/缓存配置
    else 解析失败
        FCP->>EC: 注入默认值（无超时/无限流/无缓存）
        Note over FCP: ⚠️ 记录告警日志
    end
```

- HTTP 触发请求到达时，从 `FlowVersion.orchestrationConfig.flowConfig` JSON 解析
- 解析结果注入 `ExecutionContext`，供后续节点获取
- 解析失败 → 使用默认值（无超时/无限流/无缓存），记录告警日志

### 3.2a 节点超时上限控制

运行时单节点超时 = **min(节点配置值, 应用最大超时值)**。应用最大超时值从系统配置读取（默认 5s，平台管理员可按应用覆盖）。节点配置值超过应用最大超时值时，以应用最大超时值为准，确保平台管理员可控制全局超时上限。

---

## 4. [已移除] 数据处理节点执行器

> ❌ 已移除。数据处理节点（FR-040）已被脚本节点（FR-040a）替代。脚本节点执行器设计详见 [plan-script.md](./plan-script.md)。并行处理节点执行器见 §3 并行分支执行器。

---

## 5. 入站限流拦截器（InboundRateLimiter）

> 详见 ADR-005

### 5.1 WebFilter 拦截链

```mermaid
sequenceDiagram
    participant Client
    participant Filter as "InboundRateLimiter<br/>WebFilter (order=-100)"
    participant Redis
    participant Handler as "Request Handler"

    Client->>Filter: HTTP Request
    Filter->>Filter: 解析 flowConfig.rateLimit

    alt mode = QPS（令牌桶）
        Filter->>Redis: EVAL Lua 令牌桶脚本
        Redis-->>Filter: 结果
    else mode = CONCURRENCY（并发）
        Filter->>Redis: INCR concurrency key
        Redis-->>Filter: 当前并发数
    end

    alt 允许
        Filter->>Handler: chain.filter(exchange)
        Handler-->>Client: 正常响应
        opt mode = CONCURRENCY
            Filter->>Redis: DECR concurrency key
        end
    else 拒绝
        Filter-->>Client: 429 Too Many Requests<br/>Retry-After: N
    else Redis 不可用
        Note over Filter: ⚠️ 降级放行
        Filter->>Handler: chain.filter(exchange)
    end
```

### 5.2 Redis Lua 脚本

```lua
-- key: cp:ratelimit:qps:{flowId}:{second}
-- ARGV[1]: maxTokens
-- ARGV[2]: currentSecond (用于清理过期 key)
local current = redis.call('GET', KEYS[1])
if current == false then
    redis.call('SET', KEYS[1], ARGV[1], 'EX', 2)
    return 1
elseif tonumber(current) > 0 then
    redis.call('DECR', KEYS[1])
    return 1
else
    return 0
end
```

### 5.3 并发模式

- Key: `cp:ratelimit:concurrency:{flowId}`
- 请求到达 → `INCR`，超 `maxConcurrency` → `DECR` + 返回 429
- 请求完成 → `DECR`
- 为防止死锁（进程崩溃），key 设置 TTL = 300s

---

## 6. 缓存管理（FlowCacheManager）

### 6.1 缓存模式

```mermaid
flowchart TD
    START["触发请求到达"] --> PARSE["解析 cacheConfig"]
    PARSE --> ENABLED{"cache.enabled?"}

    ENABLED -->|"false"| EXEC_DAG["正常执行 DAG"]
    ENABLED -->|"true"| COMPUTE_KEY["计算缓存键<br/>模板: keyTemplate + 上下文解析<br/>例: cp:flow:{flowId}:cache:u001"]

    COMPUTE_KEY --> REDIS_GET["Redis GET"]
    REDIS_GET --> HIT{"缓存命中?"}

    HIT -->|"命中"| RETURN_CACHED["💾 直接返回缓存结果<br/>跳过 DAG 执行"]
    HIT -->|"未命中"| EXEC_DAG

    EXEC_DAG --> REDIS_SET["Redis SET(key, result, TTL)"]
    REDIS_SET --> RETURN["返回结果"]

    RETURN_CACHED --> RETURN

    style HIT fill:#fff3e0,stroke:#e65100
    style RETURN_CACHED fill:#c8e6c9,stroke:#1b5e20
    style REDIS_SET fill:#e1f5fe,stroke:#01579b
```

### 6.2 缓存键生成

- 模板：`${$.node.trigger.input.userId}`
- 运行时解析为: `cp:flow:{flowId}:cache:u001`
- 多个键用 `:` 拼接

### 6.3 缓存失效

| 触发条件 | 失效范围 |
|---------|---------|
| FlowVersion 发布 | 清空该 Flow 所有缓存 |
| FlowVersion 标记失效 | 清空该版本缓存 |
| Flow 部署新版本 | 清空该 Flow 所有缓存 |
| Flow 停止 | 清空该 Flow 所有缓存 |
| TTL 自然过期 | 自动失效 |

---

## 7. 日志采集（ExecutionRecordService + ExecutionStepService）

> 详见 ADR-006

### 7.1 写入模型

```mermaid
flowchart TD
    START["请求完成<br/>（成功/失败/超时）"] --> SWITCH{"应用级<br/>日志采集开关?"}

    SWITCH -->|"开启（默认）"| FULL["写入 execution_record_t<br/>+ execution_step_t"]
    SWITCH -->|"关闭"| BASE["仅写入 execution_record_t<br/>（基础信息）"]

    FULL --> SANITIZE["🔒 敏感信息脱敏<br/>移除 password/token/secretKey"]
    BASE --> SANITIZE

    SANITIZE --> WRITE["异步写入 DB"]
    WRITE --> CHECK{"写入成功?"}
    CHECK -->|"成功"| DONE["✅ 完成"]
    CHECK -->|"失败"| LOG_ERR["⚠️ 仅记录错误日志<br/>不影响业务响应"]

    style SWITCH fill:#fff3e0,stroke:#e65100
    style SANITIZE fill:#fce4ec,stroke:#880e4f
    style LOG_ERR fill:#ffccbc,stroke:#b71c1c
    style DONE fill:#c8e6c9,stroke:#1b5e20
```

### 7.2 敏感信息脱敏

```java
private Map<String, Object> sanitize(Map<String, Object> data) {
    // 移除 marked sensitive 的字段（如 password, token, secretKey）
    return data.entrySet().stream()
        .filter(e -> !SENSITIVE_FIELDS.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}
```

### 7.3 定时清理

- 每天 03:00 执行
- 删除 `trigger_time < NOW() - INTERVAL 30 DAY` 的记录
- 分批删除（每批 1000 条），避免长事务

### 7.3a FIFO 条数清理

每次 `execution_record` 写入后检查该 `flow_id` 的记录数是否超限（默认 1000，按应用可配），超限时按 `create_time ASC` 批量 DELETE 最早的多余记录。与 30 天定时清理互补。

---

## 8. 调试执行器（DebugExecutionService）

### 8.1 执行模式

```mermaid
sequenceDiagram
    participant Client
    participant DebugAPI as "POST /api/v1/debug/execute"
    participant DebugSvc as "DebugExecutionService"
    participant ThreadPool as "独立线程池<br/>(max 5)"
    participant Engine as "FlowRuntimeEngine"
    participant DB

    Client->>DebugAPI: { flowId, versionId, triggerData }
    DebugAPI->>DebugSvc: 校验

    alt versionId 对应版本已失效
        DebugSvc-->>Client: ❌ EC-014 拒绝
    else 用户并发调试数 > 3
        DebugSvc-->>Client: ❌ 并发超限
    else 校验通过
        DebugSvc->>ThreadPool: 提交执行任务（超时 30s）
        ThreadPool->>Engine: 同步执行 DAG
        Engine-->>ThreadPool: 各节点执行状态 + 输入输出 + 耗时
        ThreadPool-->>DebugSvc: 完整结果
        DebugSvc->>DB: 写入运行记录<br/>(trigger_type=2 debug)
        DebugSvc-->>Client: ✅ 调试结果
    end
```

### 8.2 约束

| 约束 | 说明 |
|------|------|
| 版本限制 | 仅草稿和已发布版本可调试；已失效版本拒绝（EC-014） |
| 并发限制 | 同一用户最多 3 个并发调试请求 |
| 运行记录 | 调试执行生成运行记录，不计入正常运行指标 |
| 不影响运行中 Flow | 调试使用版本快照，不绑定 deployed_version_id |

---

## 9. 认证注入器扩展

### 9.1 注入器注册

V1 已有 `CredentialInjectorRegistry`（Strategy 模式），通过 Spring Bean 自动发现。V2 新增 3 个注入器：

```mermaid
classDiagram
    class CredentialInjector {
        <<interface>>
        +supports(AuthType) bool
        +inject(context) Mono~Context~
    }

    class CredentialInjectorRegistry {
        -List~CredentialInjector~ injectors
        +resolve(AuthType) CredentialInjector
    }

    class SoaCredentialInjector {
        AuthType.SOA
    }
    class ApigCredentialInjector {
        AuthType.APIG
    }
    class AkskCredentialInjector {
        AuthType.AKSK
    }
    class SystokenCredentialInjector {
        AuthType.SYSTOKEN
    }
    class CookieCredentialInjector {
        AuthType.COOKIE
        （V2 新增）
    }
    class DigitalSignCredentialInjector {
        AuthType.DIGITAL_SIGN
        （V2 新增）
    }
    class MultiAuthCredentialInjector {
        AuthType.MULTI
        （V2 新增）
    }

    CredentialInjector <|.. SoaCredentialInjector
    CredentialInjector <|.. ApigCredentialInjector
    CredentialInjector <|.. AkskCredentialInjector
    CredentialInjector <|.. SystokenCredentialInjector
    CredentialInjector <|.. CookieCredentialInjector
    CredentialInjector <|.. DigitalSignCredentialInjector
    CredentialInjector <|.. MultiAuthCredentialInjector
    CredentialInjectorRegistry o-- CredentialInjector : 管理
```

### 9.2 认证类型枚举扩展

```java
public enum AuthType {
    SOA(1),
    APIG(2),
    NONE(4),
    AKSK(5),
    SYSTOKEN(7),
    COOKIE(8),       // V2 新增
    DIGITAL_SIGN(9),  // V2 新增
    MULTI(10);        // V2 新增（多选组合）
}
```

---

## 10. SYSTOKEN 白名单校验器（SystokenWhitelistValidator）

### 10.1 校验流程

```mermaid
flowchart TD
    START["HTTP 触发请求"] --> EXTRACT["🔑 提取 SYSTOKEN 凭证"]
    EXTRACT --> READ_WL["从 FlowVersion.orchestrationConfig<br/>.trigger.authConfig 读取白名单"]
    READ_WL --> EMPTY{"白名单是否为空?"}
    EMPTY -->|"空"| E401_EMPTY["❌ 401<br/>No SYSTOKEN whitelisted"]
    EMPTY -->|"非空"| CHECK{"systoken 是否在<br/>白名单中?"}
    CHECK -->|"不在"| E401_NO["❌ 401<br/>SYSTOKEN not in whitelist"]
    CHECK -->|"在"| PASS["✅ 通过，继续执行"]

    style E401_EMPTY fill:#ffccbc,stroke:#b71c1c
    style E401_NO fill:#ffccbc,stroke:#b71c1c
    style PASS fill:#c8e6c9,stroke:#1b5e20
```

### 10.2 白名单配置位置

白名单存储在 `FlowVersion.orchestrationConfig.trigger.authConfig.systokenWhitelist` 中：

```json
{
  "trigger": {
    "type": "http",
    "authConfig": {
      "type": "SYSTOKEN",
      "systokenWhitelist": ["token_abc123", "token_xyz789"]
    }
  }
}
```

---

## 11. URL 白名单校验器（UrlWhitelistValidator）

### 11.1 校验流程

```mermaid
flowchart TD
    START["连接器 HTTP 调用"] --> CONSTRUCT["构造实际请求 URL"]
    CONSTRUCT --> CACHE["从 Caffeine Cache 加载<br/>该 connector 的组合正则<br/>（各条目用 | 拼接为单一 Pattern）<br/>TTL 5min，懒加载"]
    CACHE --> EMPTY{"白名单是否为空?"}
    EMPTY -->|"空"| ALLOW["✅ 允许（不限制）"]
    EMPTY -->|"非空"| MATCH{"单一正则匹配<br/>pattern.matcher(url).matches()"}
    MATCH -->|"匹配"| ALLOW
    MATCH -->|"不匹配"| DENY["❌ 403<br/>URL not in whitelist: {actualUrl}"]

    style ALLOW fill:#c8e6c9,stroke:#1b5e20
    style DENY fill:#ffccbc,stroke:#b71c1c
```

### 11.2 正则编译与缓存

各白名单条目用 `|` 拼接为**单一正则**编译并缓存，一次 `Matcher.matches()` 调用完成校验，无逐条遍历：

```java
@Component
public class UrlWhitelistValidator {
    // 缓存的是合并后的单一 Pattern，非 List
    private final LoadingCache<Long, Pattern> patternCache =
        Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(connectorId -> {
                List<String> entries = loadWhitelistEntries(connectorId); // 从 DB 取
                if (entries.isEmpty()) return null;                      // 空白名单 = 不限制
                String combined = String.join("|", entries);             // 用 | 拼接
                return Pattern.compile(combined);
            });

    public void validate(Long connectorId, String actualUrl) {
        Pattern pattern = patternCache.get(connectorId);
        if (pattern == null) return;                        // 空白名单 = 不限制
        if (!pattern.matcher(actualUrl).matches()) {
            throw new UrlNotWhitelistedException(connectorId, actualUrl);
        }
    }
}
```
