# 配置设计：连接器平台 V3 — 全量配置场景

**Feature ID**: CONN-PLAT-003
**关联文档**: [spec.md](./spec.md) v3.0, [plan.md](./plan.md) v3.0, [plan-api.md](./plan-api.md), [plan-script.md](./plan-script.md)
**版本**: v2.2
**创建日期**: 2026-06-30
**说明**: 系统化记录 V3 全部配置场景，覆盖开关、白名单、名单、上限、阈值、审批人等。v2.0 新增 §3「Lookup 化优化方案」。v2.2 逐项验证：16 项配置均已接入 Lookup 并正确实现。以下各节状态标注已更新为真实实现状态。

---

## 目录

- [1 配置清单](#1-配置清单)
- [2 配置详情](#2-配置详情)
  - [2.1 连接器](#21-连接器)
  - [2.2 连接流](#22-连接流)
  - [2.3 平台管控](#23-平台管控)
  - [2.4 实体级配置](#24-实体级配置)
  - [2.5 校验时机策略](#25-校验时机策略)
  - [3 Lookup 化优化方案 v2.0](#3-lookup-化优化方案-v20)
    - [3.1 背景与痛点](#31-背景与痛点)
    - [3.2 数据模型](#32-数据模型)
    - [3.3 查询逻辑](#33-查询逻辑)
      - [3.3.1 批量读取（核心优化）](#331-批量读取核心优化)
      - [3.3.2 白名单读取](#332-白名单读取)
      - [3.3.3 兜底约束](#333-兜底约束)
      - [3.3.4 设计态与运行态读取模型](#334-设计态与运行态读取模型)
    - [3.4 缓存策略](#34-缓存策略)
    - [3.5 代码改造范围](#35-代码改造范围)
    - [3.6 效果对比](#36-效果对比)
    - [3.8 实施优先级](#38-实施优先级)
- [4 配置生效接口矩阵](#4-配置生效接口矩阵)
  - [4.1 管理面 — 设计态校验](#41-管理面--设计态校验)
  - [4.2 执行面 — 运行态实施](#42-执行面--运行态实施)
  - [4.3 汇总速查表](#43-汇总速查表)
- [附录 A：平台配置能力](#附录-a平台配置能力)
  - [A.1 Spring 配置文件](#a1-spring-配置文件)
  - [A.2 业务对象 JSON 配置](#a2-业务对象-json-配置)
  - [A.3 Lookup](#a3-lookup)
  - [A.4 Property](#a4-property)
- [附录 B：关联文档索引](#附录-b关联文档索引)
- [附录 C：修订记录](#附录-c修订记录)

---

## 1 配置清单

> 💡 **设计思路**：连接器和连接流属于动态编排平台，用户的输入内容变化性极大——可以任意编排节点拓扑、编写任意脚本、配置任意参数。为保护系统安全稳定运行，平台需要对用户的输入施加硬性限制，这些限制即为本章记录的「配置」。
>
> 至于连接器名称、认证类型、编排图结构、JSON Schema 定义等，属于开放给用户的自由输入内容，不在配置约束范畴，不列入此清单。

| # | 配置项 | 作用域 | 按应用 | 设计态 | 运行态 | 默认值 | 存储 | path | classify_code | item_code | FR |
|:--|--------|--------|:---:|:---:|:---:|:--|:---:|:---:|------|------|----|
| 1 | 连接器版本数量上限 | 连接器 | ❌ | ✅ | — | 1000 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Connector.Max.Versions` | FR-005a |
| 2 | 连接器URL正则规则 | 连接器版本配置 | ❌ | ✅ | — | — | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Connector.Url.Regex.Pattern` | FR-015 |
| 3 | 连接器配置JSON长度上限 | 连接器版本配置 | ✅ | ✅ | — | 0 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Connector.Config.Max.Bytes` | FR-047 |
| 4 | 连接流版本数量上限 | 连接流 | ❌ | ✅ | — | 1000 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Flow.Max.Versions` | FR-024a |
| 5 | 运行记录条数上限 | 连接流 | ✅ | — | ✅ | 1000 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Max.Execution.Records.Per.Flow` | FR-042 |
| 6 | 连接器节点超时上限 | 连接流版本配置 | ✅ | ✅ | ✅ | 5 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Node.Max.Timeout.Seconds` | FR-034 |
| 7 | 连接流配置JSON长度上限 | 连接流版本配置 | ✅ | ✅ | — | 0 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Flow.Config.Max.Bytes` | FR-047 |
| 8 | 连接流最大QPS | 连接流版本配置 | ✅ | ✅ | ✅ | 1000 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Flow.Max.Qps` | FR-035 |
| 9 | 连接流最大并发 | 连接流版本配置 | ✅ | ✅ | ✅ | 1000 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Flow.Max.Concurrency` | FR-035 |
| 10 | 连接流缓存TTL上限 | 连接流版本配置 | ✅ | ✅ | ✅ | 1296000 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Flow.Max.Cache.Ttl.Seconds` | FR-037 |
| 11 | 连接流并行节点分支上限 | 连接流版本配置 | ✅ | ✅ | — | 8 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Flow.Max.Parallel.Branches` | FR-038a |
| 12 | 串行编排连接器节点数量上限 | 连接流版本配置 | ✅ | ✅ | — | 3 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Flow.Max.Serial.Connector.Nodes` | FR-026 |
| 13 | 脚本源码长度上限 | 连接流版本配置 | ✅ | ✅ | — | 10000 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Script.Max.Length.Chars` | FR-040a |
| 14 | 脚本超时范围 | 连接流版本配置 | ✅ | ✅ | ✅ | 5 | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Script.Max.Timeout.Seconds` | FR-040a |
| 15 | 日志采集开关 | 平台管控 | ✅ | — | ✅ | true | Lookup | `CEC.Open` | `Connector.Platform.Config` | `Log.Collection.Enabled` | FR-044 |
| 16 | 连接器平台开放应用范围清单 | 平台管控 | ❌ | ✅ | — | — | Lookup | `CEC.Open` | `Connector.Platform.AppWhitelist` | `appId` | FR-045 |
---

## 2 配置详情

当前全部 16 项配置均已从 Property 逐项查询迁移到 Lookup 批量读取（详见 §3），存储格式统一为 `path=CEC.Open` + `classify_code=Connector.Platform.Config`（或 `Connector.Platform.AppWhitelist`） + PascalCase item_code。以下逐项记录实现现状（v2.2 已逐项代码审计验证）。

---

### 2.1 连接器

#### 2.1.1 #1 连接器版本数量上限

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Connector.Max.Versions` |
| **默认值** | 1000 |
| **按应用区分** | ❌ |
| **实现状态** | ✅ 已实现 |
| **实现位置** | `ConnectorPlatformPropertyService.getConnectorMaxVersions()` → `ConnectorVersionService.createDraft()` / `copyToDraft()` |
| **生效说明** | 从 Lookup 读取 `Connector.Max.Versions`，达上限返回 422。未命中时回退 `MAX_VERSION_COUNT = 1000`。设计态校验，无运行态读取。 |
| **状态更新** | v2.2 验证通过：已从硬编码迁移到 Lookup |

---

#### 2.1.2 #2 连接器URL正则规则

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Connector.Url.Regex.Pattern` |
| **默认值** | —（空白 = 不限制） |
| **按应用区分** | ❌ |
| **实现状态** | ✅ 已实现 |
| **实现位置** | `ConnectorPlatformPropertyService.getUrlRegexPattern()` → `ConnectorVersionService.publish()` → `validatePlatformUrlRegex()` |
| **生效说明** | 从 Lookup 读取 `Connector.Url.Regex.Pattern`，若配置则编译正则并匹配 connector target URL。未配置时放行所有 URL。与 `urlWhitelist[]` 并行生效——前者是平台级兜底规则，后者是用户自配的连接器级规则。 |
| **状态更新** | v2.2 验证通过：已实现 |

---

#### 2.1.3 #3 连接器配置JSON长度上限

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Connector.Config.Max.Bytes` |
| **默认值** | 0 |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置** | `ConnectorPlatformPropertyService.getConnectorConfigMaxBytes(appId)` → `ConnectorVersionService.publish()` → `validateConnectionConfigSize()` |
| **生效说明** | 从 Lookup 读取 `Connector.Config.Max.Bytes`（支持按应用覆盖），UTF-8 字节数 ≤ max。0 表示不限制。超限提示具体字节数与上限。 |
| **状态更新** | v2.2 验证通过：已实现 |

---

### 2.2 连接流

#### 2.2.1 #4 连接流版本数量上限

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Flow.Max.Versions` |
| **默认值** | 1000 |
| **按应用区分** | ❌ |
| **实现状态** | ✅ 已实现 |
| **实现位置** | `ConnectorPlatformPropertyService.getFlowMaxVersions()` → `FlowVersionService.createDraft()` / `copyFromVersion()` |
| **生效说明** | 从 Lookup 读取 `Flow.Max.Versions`，达上限返回 422。未命中时回退 `MAX_VERSION_COUNT = 1000`。已与 #1 解耦。 |
| **状态更新** | v2.2 验证通过：已从硬编码迁移到 Lookup |

---

#### 2.2.2 #5 运行记录条数上限

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Max.Execution.Records.Per.Flow` |
| **默认值** | 1000 |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置** | `FlowInvokeService.finalizeExecutionRecord()` → `ExecutionRecordService.checkAndCleanFifo(flowId, maxRecords)` |
| **生效说明** | 从 Lookup 读取 `Max.Execution.Records.Per.Flow`（`ConnectorApiPropertyService.loadPlatformDefaults()`），每次 invoke 后 FIFO 删除最早超限记录。与 `@Scheduled` 30 天定期清理互补。 |
| **状态更新** | v2.2 验证通过：`checkAndCleanFifo()` 已在 `doOnNext` 中接入调用链 |

---

#### 2.2.3 #6 连接器节点超时上限

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Node.Max.Timeout.Seconds` |
| **默认值** | 5 |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置（设计态）** | `FlowPublishValidator.validateNodeTimeouts()` → 应用上限由 `FlowVersionService` 从 Lookup 读取 |
| **实现位置（运行态）** | `DagScheduler.resolveNodeTimeout()` / `ReactiveSequentialExecutor.resolveNodeTimeout()` → `ConnectorApiPropertyService.getNodeMaxTimeoutSeconds()` |
| **生效说明** | 运行态：`node.data.timeoutMs`（③）存在直接用，不截断；③不存在回退 Lookup `Node.Max.Timeout.Seconds`（①）。无 `Math.min` 截断。设计态：`FlowPublishValidator` 校验 ③ ≤ max(①, ②)。 |
| **状态更新** | v2.2 验证通过：已接入 Lookup，无 Math.min 截断，回退逻辑正确 |

---

#### 2.2.4 #7 连接流配置JSON长度上限

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Flow.Config.Max.Bytes` |
| **默认值** | 0 |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置** | `FlowPublishValidator.validateConfigSize()` → 从 Lookup 批量读取 `Flow.Config.Max.Bytes` |
| **生效说明** | 校验 orchestration JSON 的 UTF-8 字节数 ≤ max。0 表示不限制。支持按应用覆盖。 |
| **状态更新** | v2.2 验证通过：已实现 |

---

#### 2.2.5 #8 连接流最大QPS

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Flow.Max.Qps` |
| **默认值** | 1000 |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置（设计态）** | `FlowVersionService.validateRateLimits()` → `FlowPublishValidator.validateRateLimitAgainstAppMax()` |
| **实现位置（运行态）** | `RateLimitConfigReader.defaultConfig()` → `ConnectorApiPropertyService.getFlowMaxQps()` → `InboundRateLimiter.applyQpsLimit()` |
| **生效说明** | 运行态：`flowConfig.rateLimitConfig.maxQps`（③）存在直接用，不截断；③不存在回退 Lookup `Flow.Max.Qps`（①）。`InboundRateLimiter` 通过 Redis Lua 令牌桶实际限流。设计态：校验 ③ ≤ max(①, ②)。 |
| **状态更新** | v2.2 验证通过：已接入 Lookup，无 Math.min 截断，ReadPath 正确 |

---

#### 2.2.6 #9 连接流最大并发

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Flow.Max.Concurrency` |
| **默认值** | 1000 |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置（设计态）** | 同 #8，`FlowPublishValidator.validateRateLimitAgainstAppMax()` |
| **实现位置（运行态）** | `RateLimitConfigReader.defaultConfig()` → `ConnectorApiPropertyService.getFlowMaxConcurrency()` → `InboundRateLimiter.applyConcurrencyLimit()` |
| **生效说明** | 运行态：`rateLimitConfig.maxConcurrency`（③）存在直接用，不截断；③不存在回退 Lookup `Flow.Max.Concurrency`（①）。Redis Lua 信号量实际限流。 |
| **状态更新** | v2.2 验证通过：已接入 Lookup，与 #8 一致 |

---

#### 2.2.7 #10 连接流缓存TTL上限

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Flow.Max.Cache.Ttl.Seconds` |
| **默认值** | 1296000（15 天） |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置（运行时）** | `FlowCacheManager.writeCache()` → `ConnectorApiPropertyService.getFlowMaxCacheTtlSeconds()` |
| **实现位置（发布时）** | `FlowPublishValidator.validateCacheConfig()` → 从 Lookup 批量读取 `Flow.Max.Cache.Ttl.Seconds` |
| **生效说明** | 运行态：`flowConfig.cache.ttl`（③）存在直接用不截断；③不存在回退 Lookup（①）。发布时校验 TTL ∈ [1, max]。 |
| **状态更新** | v2.2 验证通过：已接入 Lookup，无 Math.min 截断，回退逻辑正确 |

---

#### 2.2.8 #11 连接流并行节点分支上限

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Flow.Max.Parallel.Branches` |
| **默认值** | 8 |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置** | `FlowPublishValidator.validateParallelBranches()` → 从 Lookup 批量读取，fallback `MAX_PARALLEL_BRANCHES = 8` |
| **状态更新** | v2.2 验证通过：已接入 Lookup |

---

#### 2.2.9 #12 串行编排连接器节点数量上限

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Flow.Max.Serial.Connector.Nodes` |
| **默认值** | 3 |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置** | `FlowPublishValidator.validateSerialMode()` → 从 Lookup 批量读取 `Flow.Max.Serial.Connector.Nodes`，fallback 3 |
| **生效说明** | flowMode=serial 时，统计连接器节点数量，超出上限则拒绝发布。 |
| **状态更新** | v2.2 验证通过：已实现 |

---

#### 2.2.10 #13 脚本源码长度上限

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Script.Max.Length.Chars` |
| **默认值** | 10000 |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置** | `FlowPublishValidator.validateScriptNodes()` → 从 Lookup 批量读取 `Script.Max.Length.Chars`，fallback `MAX_SCRIPT_SOURCE_LENGTH = 10000` |
| **状态更新** | v2.2 验证通过：已接入 Lookup |

---

#### 2.2.11 #14 脚本超时范围

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Script.Max.Timeout.Seconds` |
| **默认值** | 5 |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置（设计态）** | `FlowPublishValidator.validateScriptNodes()` → 从 Lookup 批量读取 `Script.Max.Timeout.Seconds`，fallback `MAX_SCRIPT_TIMEOUT_SECONDS = 30` |
| **实现位置（运行态）** | `ScriptNodeExecutor` → `ConnectorApiPropertyService.getScriptMaxTimeoutSeconds()` |
| **生效说明** | 运行态：`data.timeoutMs`（③）存在直接用；③不存在回退 Lookup `Script.Max.Timeout.Seconds`（①）。设计态校验 ③ ≤ max。 |
| **状态更新** | v2.2 验证通过：设计态+运行态均已接入 Lookup |



---

### 2.3 平台管控

#### 2.3.1 #15 日志采集开关

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.Config` |
| **item_code** | `Log.Collection.Enabled` |
| **默认值** | true |
| **按应用区分** | ✅ |
| **实现状态** | ✅ 已实现 |
| **实现位置** | `FlowInvokeService.invokeFlow()` → `ConnectorApiPropertyService.isLogCollectionEnabled()` → 全流程门控 |
| **生效说明** | `false` 时跳过：`initExecutionRecord()`（运行记录创建）、`persistStepLogs()`（步骤日志写入）、`finalizeExecutionRecord()`（记录终态更新）。`ExecutionStepService.logStep()` / `logStepsBatch()` 同样受控。 |
| **状态更新** | v2.2 验证通过：已实现全流程门控 |

---

#### 2.3.2 #16 连接器平台开放应用范围清单

| 属性 | 值 |
|------|-----|
| **存储** | Lookup |
| **path** | `CEC.Open` |
| **classify_code** | `Connector.Platform.AppWhitelist` |
| **item_code** | `appId` |
| **默认值** | —（空白 = 全部禁止） |
| **按应用区分** | ❌ |
| **实现状态** | ✅ 已实现 |
| **实现位置** | `AppWhitelistInterceptor.preHandle()` → `AppWhitelistService.isWhitelisted(appId)` → `LookupWhitelistMapper.selectItemValuesByClassifyCode("Connector.Platform.AppWhitelist")` |
| **生效说明** | 拦截 `/service/open/v2/connectors/**`、`/service/open/v2/flows/**`、`/service/open/v2/executions/**`，从 `X-App-Id` Header 取 appId，不在白名单 → 403。空白名单 = 全部拒绝（secure default）。 |
| **状态更新** | v2.2 验证通过：已接入 Lookup，secure default 行为正确 |

---

### 2.4 实体级配置

以下为连接器和连接流版本的 JSON 配置快照中，受平台限制约束的关键字段。这些字段由用户自由输入，平台通过 §2.1~§2.3 中的配置项对其施加硬性限制。

#### 2.4.1 连接器版本配置快照

| 配置字段 | 受限于 | 存储位置 | 校验位置 |
|---------|--------|---------|---------|
| 目标 URL | #2 连接器URL正则规则 | ConnectorVersion 配置快照 → url | `ConnectorVersionService.publish()` |
| 配置 JSON 大小 | #3 连接器配置JSON长度上限 | ConnectorVersion 配置快照（整体） | `ConnectorVersionService.publish()` |
| 版本数量 | #1 连接器版本数量上限 | 每连接器的版本总数 | `ConnectorVersionService.createDraft()` / `copyToDraft()` |

#### 2.4.2 连接流版本配置快照

| 配置字段 | 受限于 | 存储位置 | 校验位置 |
|---------|--------|---------|---------|
| 连接器节点超时值 | #6 连接器节点超时上限 | FlowVersion 编排快照 → 连接器节点.timeout | `FlowPublishValidator` / `DagScheduler` |
| 配置 JSON 大小 | #7 连接流配置JSON长度上限 | FlowVersion 编排快照（整体） | `FlowPublishValidator` |
| flowConfig.QPS | #8 连接流最大QPS | FlowVersion 编排快照 → flowConfig.qps | `FlowPublishValidator` / `RateLimitConfigReader` |
| flowConfig.并发数 | #9 连接流最大并发 | FlowVersion 编排快照 → flowConfig.concurrency | `FlowPublishValidator` / `RateLimitConfigReader` |
| flowConfig.缓存TTL | #10 连接流缓存TTL上限 | FlowVersion 编排快照 → flowConfig.cacheTtl | `FlowPublishValidator` / `FlowCacheManager` |
| 并行节点分支数 | #11 连接流并行节点分支上限 | FlowVersion 编排快照 → 并行节点.branches | `FlowPublishValidator` |
| 脚本源码长度 | #13 脚本源码长度上限 | FlowVersion 编排快照 → 脚本节点.script | `FlowPublishValidator` |
| 脚本超时值 | #14 脚本超时范围 | FlowVersion 编排快照 → 脚本节点.timeout | `FlowPublishValidator` |
| 版本数量 | #4 连接流版本数量上限 | 每连接流的版本总数 | `FlowVersionService.createDraft()` / `copyFromVersion()` |

### 2.5 校验时机策略

V3 采用**「保存时不校验，发布时统一卡口」**的策略：

```
草稿创建/编辑/复制 ─── 仅 DB 存储约束校验（字段长度、数据类型等）
   │
   ▼
发布时 ───── 全部校验集中执行：
   ├── 业务必填字段（名称、描述等非空）
   ├── 配置非空（编排/入参出参 Schema）
    ├── 平台 URL 正则校验 ─── #2 Connector.Url.Regex.Pattern
    ├── JSON 长度校验 ─────── #3 Connector.Config.Max.Bytes / #7 Flow.Config.Max.Bytes
    ├── JSON 语法合法性 (FR-047)
    ├── 脚本语法合法性 (FR-040a)
    ├── 串行编排连接器节点数 ≤ #12 Flow.Max.Serial.Connector.Nodes (FR-026)
    ├── 节点超时 ≤ #6 Node.Max.Timeout.Seconds (EC-028)
    ├── flowConfig QPS ≤ #8 Flow.Max.Qps (EC-025)
    ├── flowConfig 并发 ≤ #9 Flow.Max.Concurrency (EC-025)
    ├── flowConfig 缓存TTL ≤ #10 Flow.Max.Cache.Ttl.Seconds (EC-026)
    ├── 并行分支数 ≤ #11 Flow.Max.Parallel.Branches (EC-027)
     ├── 脚本长度 ≤ #13 Script.Max.Length.Chars
     ├── 脚本超时 ≤ #14 Script.Max.Timeout.Seconds
   └── 连接器版本可用性 (FR-039)
```

| 场景 | DB 约束 | 业务必填 | 平台限制 | JSON 语法 | 正则合法性 | 引用可用性 |
|------|:---:|:---:|:---:|:---:|:---:|:---:|
| 创建空草稿 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 编辑草稿保存 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 复制到草稿 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 一键复制连接流 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **发布时 (FR-007 / FR-026)** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

> 💡 "平台限制"列指 #1~#16 中所有可配置上限项——发布时取 Lookup 实际值与用户输入做对比，超出即拦截。

## 3 Lookup 化优化方案 v2.0

> 方案状态：已确认待实施 | 创建日期：2026-06-29 | 核心收益：连接流发布校验 DB 查询从最多 16 次降至 2 次

### 3.1 背景与痛点

#### 3.1.1 N+1 查询问题

原 Property 化方案存在严重的 N+1 查询问题。逐项查询 openplatform_property_t，每项配置最多 2 次 DB 查询（应用 path 未命中后回退平台 path）。

| 消费场景 | 读取配置项 | 当前 DB 查询次数 |
|---------|-----------|---------------|
| 连接流发布校验（最痛） | #6 #7 #8 #9 #10 #11 #13 #14（8 项） | 最多 16 次 |
| 连接器发布校验 | #2 #3（2 项） | 最多 4 次 |
| 创建草稿 | #1 或 #4（1 项） | 1 次 |
| 运行时执行/限流/缓存 | #6 #8 #9 #10（分散） | 各最多 2 次 |

根因：openplatform_property_t 是扁平键值对，SELECT value WHERE path=? AND code=? 一次只能取一项，无法表达一组配置的语义。

#### 3.1.2 优化思路

将一起使用的配置归入同一 LookupClassify，利用 classify 1:N item 结构一次联查 classify_t + item_t 拿回整组键值对，N 次 DB 查询降为 2 次（应用组 + 平台组）。

### 3.2 数据模型

#### 3.2.1 分组策略

| classify_code | 包含配置项 | 说明 |
|---------------|-----------|------|
| Connector.Platform.Config | #1~#15 全部 15 项 | 平台默认值 |
| Connector.Platform.{appId}.Config | #1~#15 中需覆盖的项 | 应用覆盖值，如 Connector.Platform.app_001.Config |
| Connector.Platform.AppWhitelist | #16 应用白名单 | 白名单独立 classify |

设计要点：#1~#15 归为一个 classify（而非按限流/超时等拆多组），因为消费方（尤其发布校验）同时需要多类配置，一个 classify 一次拿回全部最省查询。

#### 3.2.2 命名规则

统一 PascalCase + 点号分隔：

| 层级 | 字段 | 规则 | 示例 |
|------|------|------|------|
| path（命名空间） | classify_t.path | 统一固定值 | CEC.Open |
| classify_code（分组） | classify_t.classify_code | 平台默认 / 应用覆盖 / 白名单 | Connector.Platform.Config |
| item_code（配置项） | item_t.item_code | PascalCase.点号 | Flow.Max.Qps |

#### 3.2.3 item_code 映射表

| # | 原 Property code | 新 item_code | 默认值 | 类型 | 按应用区分 |
|---|-----------------|-------------|--------|------|-----------|
| 1 | connector_max_versions | Connector.Max.Versions | 1000 | int | 否 |
| 2 | connector_url_regex_pattern | Connector.Url.Regex.Pattern | null | String | 否 |
| 3 | connector_config_max_bytes | Connector.Config.Max.Bytes | 0 | int | 是 |
| 4 | flow_max_versions | Flow.Max.Versions | 1000 | int | 否 |
| 5 | max_execution_records_per_flow | Max.Execution.Records.Per.Flow | 1000 | int | 是 |
| 6 | node_max_timeout_seconds | Node.Max.Timeout.Seconds | 5 | int | 是 |
| 7 | flow_config_max_bytes | Flow.Config.Max.Bytes | 0 | int | 是 |
| 8 | flow_max_qps | Flow.Max.Qps | 1000 | int | 是 |
| 9 | flow_max_concurrency | Flow.Max.Concurrency | 1000 | int | 是 |
| 10 | flow_max_cache_ttl_seconds | Flow.Max.Cache.Ttl.Seconds | 1296000 | int | 是 |
| 11 | flow_max_parallel_branches | Flow.Max.Parallel.Branches | 8 | int | 是 |
| 12 | — | Flow.Max.Serial.Connector.Nodes | 3 | int | 是 |
| 13 | script_max_length_chars | Script.Max.Length.Chars | 10000 | int | 是 |
| 14 | script_max_timeout_seconds | Script.Max.Timeout.Seconds | 5 | int | 是 |
| 15 | log_collection_enabled | Log.Collection.Enabled | true | boolean | 是 |

#16 白名单：item_code = appId，item_value = appId（如 app_001 / app_001）

说明：#1/#2/#4 不支持按应用区分，仅在 Connector.Platform.Config 下存在；#3/#5~#15 支持按应用区分，可在 Connector.Platform.{appId}.Config 下覆盖。#12 同理。

#### 3.2.4 数据示例

openplatform_lookup_classify_t：

| classify_code | path | classify_name |
|---------------|------|--------------|
| Connector.Platform.Config | CEC.Open | 连接器平台配置（平台默认） |
| Connector.Platform.app_001.Config | CEC.Open | 连接器平台配置（app_001 覆盖） |
| Connector.Platform.app_002.Config | CEC.Open | 连接器平台配置（app_002 覆盖） |
| Connector.Platform.AppWhitelist | CEC.Open | 连接器平台应用白名单 |

openplatform_lookup_item_t（平台默认组，15 项）：

| item_code | item_value |
|-----------|-----------|
| Connector.Max.Versions | 1000 |
| Connector.Url.Regex.Pattern | (空) |
| Connector.Config.Max.Bytes | 0 |
| Flow.Max.Versions | 1000 |
| Max.Execution.Records.Per.Flow | 1000 |
| Node.Max.Timeout.Seconds | 5 |
| Flow.Config.Max.Bytes | 0 |
| Flow.Max.Qps | 1000 |
| Flow.Max.Concurrency | 1000 |
| Flow.Max.Cache.Ttl.Seconds | 1296000 |
| Flow.Max.Parallel.Branches | 8 |
| Flow.Max.Serial.Connector.Nodes | 3 |
| Script.Max.Length.Chars | 10000 |
| Script.Max.Timeout.Seconds | 5 |
| Log.Collection.Enabled | true |

openplatform_lookup_item_t（app_001 覆盖组，仅覆盖项）：

| item_code | item_value |
|-----------|-----------|
| Node.Max.Timeout.Seconds | 10 |
| Script.Max.Length.Chars | 20000 |

openplatform_lookup_item_t（白名单组）：

| item_code | item_value |
|-----------|-----------|
| app_001 | app_001 |
| app_002 | app_002 |
| app_003 | app_003 |

### 3.3 查询逻辑

#### 3.3.1 批量读取（核心优化）

读取某 appId 的全部 15 项配置：
1. 查 (CEC.Open, Connector.Platform.{appId}.Config) → 应用覆盖项 Map
2. 查 (CEC.Open, Connector.Platform.Config) → 平台默认项 Map
3. 合并：平台 Map 为 base，应用 Map 覆盖之 → 最终 Map
4. 按 item_code 取值，解析为 int/boolean，缺失/异常用硬编码默认值

DB 查询次数：2 次（应用组 + 平台组），vs 优化前最多 16 次。

#### 3.3.2 白名单读取

查 (CEC.Open, Connector.Platform.AppWhitelist) → item_value 列表 → 1 次 DB。空白名单 = 全部拒绝。

#### 3.3.3 兜底约束

沿用以下兜底约束原则：DB 不可用/配置缺失/格式异常时返回硬编码默认值，禁止抛异常。

| 约束 | 说明 |
|------|------|
| DB 不可用 | 跳过查询，全部使用硬编码默认值 + WARN |
| classify 缺失 | 该组返回空 Map，不影响另一组 |
| item 缺失 | 该项使用硬编码默认值 |
| value 格式异常 | 返回默认值 + ERROR 日志 |

#### 3.3.4 设计态与运行态读取模型

> 参考 `review-ratelimit.md` §1.2，配置分为三种值、两种场景。

##### 三种配置值

| 符号 | 含义 | 存储位置 |
|:---:|------|---------|
| ① | 平台全局上限 | Lookup `(CEC.Open, Connector.Platform.Config)` 中的 item |
| ② | 应用级覆盖上限 | Lookup `(CEC.Open, Connector.Platform.{appId}.Config)` 中的 item |
| ③ | 连接流版本实际配置值 | `flow_version_t.orchestration_config` JSON 快照中的字段 |

各配置项的 ③ 位置：

| # | item_code | ③ 是否存在 | ③ 位置 |
|---|-----------|:---------:|--------|
| 5 | Max.Execution.Records.Per.Flow | ❌ | 无 ③，运行态直接读 ① |
| 6 | Node.Max.Timeout.Seconds | ✅ | `nodes[].data.timeoutMs` |
| 8 | Flow.Max.Qps | ✅ | `flowConfig.rateLimitConfig.maxQps` |
| 9 | Flow.Max.Concurrency | ✅ | `flowConfig.rateLimitConfig.maxConcurrency` |
| 10 | Flow.Max.Cache.Ttl.Seconds | ✅ | `flowConfig.cache.ttl` |
| 12 | Flow.Max.Serial.Connector.Nodes | ❌ | 纯设计态校验，无 ③ |
| 14 | Script.Max.Timeout.Seconds | ✅ | `script节点.data.timeout` |

##### 设计态（发布时校验）

```
校验规则：③ <= 应用实际上限（② 覆盖 ①）
```

- 读取 ① 和 ②（通过 `loadConfigBundle(appId)` 一次拿到合并 Map，② 覆盖 ①）
- 应用实际上限 = ② 存在时用 ②（无论大小），② 不存在时用 ①
- 校验 ③ 不超过此上限，超过则拒绝发布（返回 422）
- ③ 为空（未配置）则不校验，发布通过

> 已在 `FlowPublishValidator.validateOrchestrationConfig()` 和 `FlowVersionService.validateRateLimits()` 中实现。

##### 运行态（每次请求执行）

```
实际生效值 = ③ 存在 ? ③ : ①
```

- ③ 存在 → 直接用 ③（**不做 Math.min 截断**，设计态已保证 ③ <= max(①,②)）
- ③ 不存在 → 回退到 ①（平台全局值，通过 `loadConfigBundle(null)` 读取）
- **运行态不读 ②**（设计态已保证 ③ <= max(①,②)，运行态无需重复校验应用级覆盖）

##### 关键原则

| 原则 | 说明 |
|------|------|
| 运行态不截断 | 删除 `Math.min(③, 硬编码)` 截断逻辑，设计态已校验 |
| 运行态不读 ② | 仅读 ③ 和 ①，应用级覆盖在设计态处理 |
| ③ 优先 | ③ 存在时直接用，不与 ① 取 min |
| ① 仅作回退 | ③ 不存在时才读 ① 作为默认值 |

### 3.4 缓存策略

| 服务 | 缓存 | key 规则 | TTL | 清理方 |
|------|------|---------|-----|--------|
| open-server | 否 | — | — | — |
| connector-api | Redis | OPENPLATFORM:LOOK:UP:ITEM:{path}:{classifyCode} | 7d±2h | market-server clearLookUpItemCache |

关键：connector-api 缓存 key 与 market-server 清理 key 完全一致，确保 market-server 修改配置后能精准失效 connector-api 缓存。

缓存 key 示例：
- 平台默认组: OPENPLATFORM:LOOK:UP:ITEM:CEC.Open:Connector.Platform.Config
- app_001 组: OPENPLATFORM:LOOK:UP:ITEM:CEC.Open:Connector.Platform.app_001.Config
- 白名单组: OPENPLATFORM:LOOK:UP:ITEM:CEC.Open:Connector.Platform.AppWhitelist

### 3.5 代码改造范围

#### 3.5.1 open-server 侧

| 文件 | 改造内容 |
|------|---------|
| LookupWhitelistMapper.java + XML | 扩展查询方法增加 path 参数；新增 selectItemMapByPathAndClassifyCode(path, classifyCode) 返回 Map |
| ConnectorPlatformPropertyService.java | 重构核心：从逐项查 Property 改为批量查 Lookup；保留原方法签名，内部从合并 Map 取值 |
| AppWhitelistService.java | classify_code 改为 Connector.Platform.AppWhitelist，path 传 CEC.Open |
| ConnectorPlatformConstants.java | 新增 path / classify_code / item_code 常量 |

#### 3.5.2 connector-api 侧

| 文件 | 改造内容 |
|------|---------|
| 新建 Lookup R2DBC Repository | 支持按 (path, classify_code) 联查 |
| ConnectorApiPropertyService.java | 扩展为支持全部 15 项批量读取 + Redis 缓存 |
| DagScheduler.java / ReactiveSequentialExecutor.java | #6 运行态：③存在用③不截断，③不存在回退①，删除 Math.min |
| RateLimitConfigReader.java | #8/#9 运行态：读取路径改为 flowConfig.rateLimitConfig，③不截断，③不存在回退① |
| FlowCacheManager.java | #10 运行态：③存在用③不截断，③不存在回退①，删除 Math.min |

#### 3.5.3 market-server 侧

market-server 无需改动。`CacheServiceV2.clearLookUpItemCache(path, classifyCode)` 已就绪，与 connector-api 缓存 key 对齐。

### 3.6 效果对比

| 指标 | 优化前（Property） | 优化后（Lookup） |
|------|-------------------|-----------------|
| 连接流发布校验 DB 查询 | 最多 16 次 | 2 次 |
| 连接器发布校验 DB 查询 | 最多 4 次 | 2 次 |
| 运行时单次执行配置读取 | 多次分散 + Math.min 截断 | ③优先（0 次 DB），③不存在回退①（1 次 DB，含缓存则 0 次） |
| 存储表 | openplatform_property_t | openplatform_lookup_classify_t + item_t |
| 按应用区分 | path 区分 | classify_code 区分 |
| 批量读取 | 不支持 | 一次拿回整组 15 项 |

### 3.8 实施优先级

| 优先级 | 项目 | 原因 |
|--------|------|------|
| P0 | LookupWhitelistMapper 扩展（open-server） | 新增 path 参数 + selectItemMapByPathAndClassifyCode，所有后续改造的基础 |
| P0 | ConnectorPlatformPropertyService 重构（open-server） | 核心重构：逐项查 Property → 批量查 Lookup |
| P1 | ConnectorApiPropertyService 扩展（connector-api） | 运行时批量读取 + Redis 缓存 |
| P1 | AppWhitelistService 迁移 | classify_code 改为新命名规则 |
| P2 | 运行时消费方接入 | DagScheduler / RateLimitConfigReader / FlowCacheManager |
| P2 | Property 数据迁移脚本 | openplatform_property_t → openplatform_lookup_*_t |
| P3 | openplatform_property_t 清理 | 确认迁移完成后，清理连接器平台相关 Property 数据 |

## 4 配置生效接口矩阵

以下按接口维度列出每项配置在哪些 API 的什么阶段生效。配置号对应 §1 配置清单。

### 4.1 管理面 — 设计态校验

#### 4.1.1 创建 / 复制实体 → 版本

| 接口 | 方法 | 路径 | 生效配置 | 校验逻辑 |
|------|------|------|---------|----------|
| 创建连接器版本 | POST | `/service/open/v2/connectors/{id}/versions` | #1 连接器版本数量上限 | 统计已有版本数，超限返回 422 |
| 复制连接器版本 | POST | `.../versions/{versionId}/copy` | #1 连接器版本数量上限 | 同上 |
| 创建连接流版本 | POST | `/service/open/v2/flows/{flowId}/versions` | #4 连接流版本数量上限 | 统计已有版本数，超限返回 422 |
| 复制连接流版本 | POST | `.../versions/{versionId}/copy` | #4 连接流版本数量上限 | 同上 |

#### 4.1.2 发布 → 连接器版本

| 接口 | 方法 | 路径 | 生效配置 | 校验逻辑 |
|------|------|------|---------|----------|
| 发布连接器版本 | POST | `/service/open/v2/connectors/versions/{versionId}/publish` | #2 URL 正则规则 | 正则匹配 connector URL，失败返回 422 |
| — | — | — | #3 配置 JSON 长度上限 | UTF-8 字节数 ≤ max，超限返回 422 |

#### 4.1.3 发布 → 连接流版本

| 接口 | 方法 | 路径 | 生效配置 | 校验逻辑 |
|------|------|------|---------|----------|
| 发布连接流版本 | POST | `/service/open/v2/flows/versions/{versionId}/publish` | #6 连接器节点超时上限 | 节点 data.timeoutMs ≤ 应用上限 |
| — | — | — | #7 连接流配置 JSON 长度上限 | orchestration JSON 字节数 ≤ max |
| — | — | — | #8 连接流最大 QPS | 流级 QPS ≤ 应用上限 |
| — | — | — | #9 连接流最大并发 | 流级并发 ≤ 应用上限 |
| — | — | — | #10 连接流缓存 TTL 上限 | TTL ∈ [min, max] |
| — | — | — | #11 连接流并行分支上限 | 并行分支数 ≤ max |
| — | — | — | #12 串行编排连接器节点数量上限 | serial 模式下 connector 节点数 ≤ max |
| — | — | — | #13 脚本源码长度上限 | 每段脚本 source 长度 ≤ max |
| — | — | — | #14 脚本超时范围 | 脚本节点 data.timeout ≤ 上限 |

#### 4.1.4 HTTP 拦截器 — 全局准入

| 拦截范围 | 路径模式 | 生效配置 | 校验逻辑 |
|---------|---------|---------|----------|
| 连接器/连接流/运行记录所有管理面接口 | `/service/open/v2/connectors/**`<br>`/service/open/v2/flows/**`<br>`/service/open/v2/executions/**` | #16 连接器平台开放应用范围清单 | 从 `X-App-Id` Header 取 appId，不在白名单 → 403 |

### 4.2 执行面 — 运行态实施

#### 4.2.1 调用连接流 (invoke)

| 接口 | 方法 | 路径 | 生效配置 | 阶段 | 实施逻辑 |
|------|------|------|---------|------|---------|
| 调用连接流 | POST | `/api/v1/flows/{flowId}/invoke` | #5 运行记录条数上限 | 执行完成后 | 写入新记录后 FIFO 删除最早超限记录 |
| — | — | — | #6 连接器节点超时上限 | 节点执行中 | `node.timeoutMs` 存在用节点值；不存在回退该上限 |
| — | — | — | #8 连接流最大 QPS | 流入口 | Redis Lua 令牌桶 QPS 限流 |
| — | — | — | #9 连接流最大并发 | 流入口 | Redis Lua 信号量并发限流 |
| — | — | — | #10 连接流缓存 TTL 上限 | 缓存写入 | 流级未配 cacheTtl 时回退此上限 |
| — | — | — | #14 脚本超时范围 | 脚本执行中 | `node.timeoutMs` 存在用节点值；不存在回退该上限 |
| — | — | — | #15 日志采集开关 | 全流程 | `false` 时跳过运行记录创建、步骤日志写入、记录终态更新 |

#### 4.2.2 调试连接流 (debug)

| 接口 | 方法 | 路径 | 生效配置 | 与 invoke 差异 |
|------|------|------|---------|---------------|
| 调试连接流 | POST | `/api/v1/flows/{flowId}/versions/{versionId}/debug` | #6, #8, #9, #10, #14 | 不包含 #5（debug 不写运行记录）、#15（debug 不写执行日志）；走 `FlowVersionDebugService.executeTestRun()` 独立路径 |

> **说明**：debug 与 invoke 的执行管线（DAG 调度/节点执行/缓存）共享同一套 executor，因此 #6, #8, #9, #10, #14 两路径同时生效。但 debug 不产生持久化运行记录，因此 #5（记录条数上限）、#15（日志采集开关）仅对 invoke 生效。

### 4.3 汇总速查表

| 配置号 | 配置项 | 生效接口 | 阶段 |
|:---:|------|------|------|
| #1 | 连接器版本数量上限 | 创建/复制连接器版本 | 设计态 |
| #2 | URL 正则规则 | 发布连接器版本 | 设计态 |
| #3 | 配置 JSON 长度上限 | 发布连接器版本 | 设计态 |
| #4 | 连接流版本数量上限 | 创建/复制连接流版本 | 设计态 |
| #5 | 运行记录条数上限 | invoke | 运行态 |
| #6 | 连接器节点超时上限 | 发布 + invoke + debug | 设计态 + 运行态 |
| #7 | 连接流配置 JSON 长度上限 | 发布连接流版本 | 设计态 |
| #8 | 连接流最大 QPS | 发布 + invoke + debug | 设计态 + 运行态 |
| #9 | 连接流最大并发 | 发布 + invoke + debug | 设计态 + 运行态 |
| #10 | 连接流缓存 TTL 上限 | 发布 + invoke + debug | 设计态 + 运行态 |
| #11 | 连接流并行分支上限 | 发布连接流版本 | 设计态 |
| #12 | 串行编排连接器节点上限 | 发布连接流版本 | 设计态 |
| #13 | 脚本源码长度上限 | 发布连接流版本 | 设计态 |
| #14 | 脚本超时范围 | 发布 + invoke + debug | 设计态 + 运行态 |
| #15 | 日志采集开关 | invoke | 运行态 |
| #16 | 开放应用范围清单 | `/service/open/v2/connectors/**`<br>`/service/open/v2/flows/**`<br>`/service/open/v2/executions/**` | 设计态（HTTP 拦截器） |

> 💡 **#8 / #9 运行态触发条件**：连接器平台的 `/api/v1/flows/{flowId}/invoke` 接口本身已受外层全局限流保护（WeCodeSite 页面用户共用，阈值很低）。平台级 Lookup 配置 `Flow.Max.Qps` / `Flow.Max.Concurrency` 仅设定**上限天花板**，运行态实际生效的是用户自配的 `flowConfig.rateLimitConfig.{maxQps, maxConcurrency}`。因此 #8 / #9 的平台上限只有在该用户自配限流值**低于**接口外层全局限流时才会实际触发——即用户的限制比平台全局限制更严格。

## 附录 A：平台配置能力

### A.1 Spring 配置文件

V3 涉及两个 Spring Boot 服务，均采用 `application.yml` + Profile 机制管理环境差异化配置。

#### A.1.1 open-server（管理面，端口 18080）

| 文件 | 用途 |
|------|------|
| `open-server/src/main/resources/application.yml` | 公共配置（端口、编码、日志、OpenAPI） |
| `application-dev.yml` | 开发环境（MySQL 数据源、Redis 集群、MyBatis、健康检查） |
| `application-prod.yml` | 生产环境（环境变量占位符 `${...}`、更大连接池） |

**V3 相关配置项**：

| 配置路径 | 默认值 | 说明 |
|----------|--------|------|
| `server.port` | 18080 | open-server 服务端口 |
| `server.servlet.context-path` | `/open-server` | 上下文路径 |
| `spring.datasource.url` | `jdbc:mysql://...` | MySQL 数据源（JDBC，HikariCP 连接池） |
| `spring.datasource.hikari.maximum-pool-size` | dev:20 / prod:50 | 数据库连接池最大连接数 |
| `spring.redis.cluster.nodes` | 5 节点集群 | Redis 集群节点列表 |
| `spring.redis.lettuce.pool.max-active` | dev:8 / prod:20 | Redis 连接池最大活跃连接 |
| `mybatis.mapper-locations` | `classpath:mapper/*.xml` | MyBatis XML Mapper 扫描路径 |
| `platform.approval-url-prefix` | dev:`localhost:3000` / prod:`platform.example.com` | 审批页面跳转地址前缀（FR-031~033 审批流程使用） |
| `management.health.db.enabled` | dev:false / prod:true | 健康检查是否包含数据库 |
| `logging.level.com.xxx.it.works.wecode.v2` | dev:DEBUG / prod:INFO | V3 模块日志级别 |

**Profile 激活**：`application.yml` 中 `spring.profiles.active: dev`（开发环境默认），生产通过环境变量 `SPRING_PROFILES_ACTIVE=prod` 覆盖。

#### A.1.2 connector-api（运行时，端口 18180）

| 文件 | 用途 |
|------|------|
| `connector-api/src/main/resources/application.yml` | 统一配置（端口、R2DBC、Redis 集群、缓存开关） |

**V3 相关配置项**：

| 配置路径 | 默认值 | 说明 |
|----------|--------|------|
| `server.port` | 18180 | connector-api 服务端口 |
| `spring.r2dbc.url` | `r2dbc:mysql://192.168.3.155:3306/openapp` | Reactive MySQL 数据源 |
| `spring.r2dbc.pool.max-size` | 20 | R2DBC 连接池最大连接数 |
| `spring.r2dbc.pool.initial-size` | 5 | R2DBC 连接池初始连接数 |
| `spring.data.redis.cluster.nodes` | 5 节点集群 | Redis 集群节点（同 open-server） |
| `spring.data.redis.lettuce.pool.max-active` | 8 | Redis 连接池最大活跃连接 |
| `connector.cache.enabled` | true | **缓存总开关** — 控制是否启用 Redis 缓存（`CacheToggle`），`false` 时所有查询穿透 R2DBC |
| `connector.snowflake.worker-id` | 0 | Snowflake ID 生成器 Worker ID（`IdGenerator`） |
| `connector.snowflake.datacenter-id` | 0 | Snowflake ID 生成器数据中心 ID |
| `management.health.db.enabled` | true | 健康检查是否包含 R2DBC |
| `management.health.redis.enabled` | true | 健康检查是否包含 Redis |

#### A.1.3 V3 硬编码配置（Java 常量/注解）

以下配置不在 `application.yml` 中，以 Java 代码常量或注解形式存在：

| 配置项 | 所在位置 | 值 | 说明 |
|--------|---------|-----|------|
| `@EnableScheduling` | `ConnectorApiApplication.java` | — | 启用定时任务调度（运行记录 FIFO 清理、30 天定期清理） |
| GraalJS `statementLimit` | `GraalJsContextFactory.java` | 10000 | 脚本节点执行语句数上限（沙箱安全限制） |
| GraalJS `HostAccess.EXPLICIT` | `GraalJsContextFactory.java` | — | 禁止 Java 宿主对象隐式访问（五层纵深防御第 3 层） |
| 运行记录 30 天清理 | `ExecutionCleanupJob.java` | 30 天 | `@Scheduled` 定时清理过期运行记录 |

#### A.1.4 .gitignore 排除

生产环境配置文件中的敏感信息（数据库密码、Redis 密码）通过环境变量 `${MYSQL_PASSWORD}` / `${REDIS_PASSWORD}` 注入，不写入配置文件。`application-prod.yml` 中的占位符默认值仅用于本地开发验证。

### A.2 业务对象 JSON 配置

V3 采用 **「配置即快照」** 策略——连接器和连接流的运行时配置均存储在版本快照 JSON 中，不独立建配置表。

| 配置载体 | 存储方式 | 包含的配置项 | 变更生效 |
|----------|---------|-------------|---------|
| **ConnectorVersion 快照** | `connector_version_t.config_snapshot` JSON 列 | 认证类型、凭证位置、URL 白名单规则、入参/出参 Schema | 发布后立即生效，运行时按引用版本号读取 |
| **FlowVersion 快照** | `flow_version_t.orchestration_config` JSON 列 | 编排图（nodes/edges）、flowConfig（限流/缓存）、触发器认证/SYSACCOUNT白名单、节点超时、脚本源码、并行分支 | 部署绑定版本后生效 |

**关键设计原则**：配置随版本走，而非随实体走。每个已发布版本携带完整的自包含配置快照，保证了：
- 版本切换时配置原子性（不会出现新旧配置混淆）
- 历史版本可完整追溯（任意历史版本的配置均可查看）
- 草稿编辑不影响运行中实例（草稿修改仅作用于当前工作区）

### A.3 Lookup

**用途**：存储枚举型、列表型平台配置。平台管理员通过 market-web 界面维护，数据写入 MySQL；open-server / connector-api 直读 MySQL 获取配置。

**数据库表**（2 张）：

| 表名 | 用途 | 关键列 |
|------|------|--------|
| `openplatform_lookup_classify_t` | 分类（分组） | `classify_id` (PK), `classify_code`, `classify_name`, `path`, `status` |
| `openplatform_lookup_item_t` | 分类下的具体项 | `item_id` (PK), `classify_id` (FK), `item_code`, `item_name`, `item_value`, `status` |

**ER 关系**：`classify_t` 1 ─── N `item_t`，通过 `classify_id` 关联。

**设计模型**：类比文件系统 — `path` 相当于目录，`classify_code` 相当于一个配置文件，文件内是一对多的列表数据（`classify_t` 1:N `item_t`）。`UK(path, classify_code)` 保证同一目录下文件名唯一；`UK(classify_id, item_code)` 保证同一文件内条目不重复。

**目录结构与示例数据**：

```
  /connector_platform/ (path="connector_platform")    ← 连接器平台命名空间
  ├── app_whitelist                                       ← 应用白名单（1:N 列表）
  │     ├── app_001
  │     ├── app_002
  │     └── app_003
  │
  /connector_platform_gray/ (path="connector_platform_gray")  ← 灰度目录
  └── app_whitelist                                            ← 灰度白名单（独立分组）
        ├── app_001
        └── app_005
```

| path（目录） | classify_code（文件名） | item_value（文件内容） | 说明 |
|:--:|------|------|------|
| `connector_platform` | `app_whitelist` | `app_001`, `app_002`, `app_003` | 生产环境：三个应用开通连接器平台 |
| `connector_platform_gray` | `app_whitelist` | `app_001`, `app_005` | 灰度环境：仅两个应用可见 |

**查询逻辑**：先按 `(path, classify_code)` 定位 `classify_id`，再联查 `item_t` 获取所有有效 `item_value`。


**读取方式**：
- open-server 通过 `LookupWhitelistMapper` 联查两张表：先按 `(path, classify_code)` 定位 `classify_id`，再查该分类下所有有效 item 的 `item_value`
- 本地 Caffeine 缓存（TTL 5min）
- connector-api 通过 R2DBC 直查同两张表，Redis 缓存（TTL 7d±2h），miss 回源 MySQL

**准入流程**：
```
用户请求 → 准入拦截器 → 查本地缓存
                           ├── 命中 → 判断 appId 是否在 item_value 列表中 → 放行/拒绝
                           └── 未命中 → 联查 classify_t + item_t
                                          ├── 成功 → 更新缓存 → 判断
                                          └── MySQL 不可用 → 降级放行 + 告警
```

> 💡 **path 使用约定**：`path` 用于对配置归类分组。V3 连接器平台的应用白名单放在 `path="connector_platform"` 下。若后续需要按版本、环境等维度隔离，可通过不同 `path`（如 `path="connector_platform_v4"`、`path="connector_platform_gray"`）创建独立分组，互不干扰。

### A.4 Property

> ⚠️ **v2.0 废弃提示**：本节描述的 Property 存储方案已被 §3「Lookup 化优化方案」取代。连接器平台配置已全量迁移到 Lookup（openplatform_lookup_classify_t + openplatform_lookup_item_t），openplatform_property_t 不再用于连接器平台配置。本节保留作历史参考。

**用途**：存储键值对型应用级参数。平台管理员通过 market-web 界面维护，数据写入 MySQL；open-server / connector-api 直读 MySQL 获取配置。

**数据库表**（1 张）：

| 表名 | 用途 | 关键列 |
|------|------|--------|
| `openplatform_property_t` | 键值对属性 | `id` (PK, 雪花ID), `path`, `code`, `name`, `value`, `status` |

**唯一约束**：`(path, code)` 联合唯一索引。这保证了同一个命名空间下 code 唯一，不会出现重复配置。

**设计模型**：`path` 是命名空间，所有连接器平台配置统一放在 `path=connector_platform` 下，与其他业务模块（能力开放平台、数据开放平台等）的配置隔离。`code` 是配置项名称，同一 path 下 code 唯一。`UK(path, code)` 联合唯一索引保证不会重复。

**按应用区分机制**：对于支持按应用区分的配置项，平台默认值存于 `path=connector_platform`，应用覆盖值存于 `path=connector_platform_app_{appId}`。查询时优先搜应用专属路径，找不到再回退平台默认路径。

**二层回退模型**：`应用独立值` > `平台统一默认值`

**命名空间与示例数据**：

```
openplatform_property_t
  /connector_platform/                          ← 连接器平台命名空间（平台默认值）
  ├── max_timeout_seconds               = 5
  ├── max_qps                           = 1000
  ├── max_concurrency                   = 1000
  ├── max_execution_records_per_flow    = 1000
  ├── connector_url_regex_pattern                 = "^https://api\\.example\\.com/.*"
  ├── connector_max_versions            = 1000
  ├── flow_max_versions                 = 1000
  ├── flow_max_cache_ttl_seconds        = 1296000
  ├── flow_max_parallel_branches        = 8
  ├── script_max_length_chars           = 10000
  ├── script_max_timeout_seconds        = 30
  └── log_collection_enabled            = true

  /connector_platform_app_001/                  ← 应用 app_001 覆盖值
  ├── max_timeout_seconds               = 10
  └── script_max_length_chars           = 20000

  /connector_platform_app_002/                  ← 应用 app_002 覆盖值
  ├── max_timeout_seconds               = 8
  ├── max_qps                           = 500
  └── log_collection_enabled            = false
```

| path（命名空间） | code（配置项） | value | 说明 |
|:--:|------|:--:|------|
| `connector_platform` | `max_timeout_seconds` | `5` | 平台默认超时 5s |
| `connector_platform_app_001` | `max_timeout_seconds` | `10` | app_001 覆盖为 10s |
| `connector_platform_app_002` | `max_timeout_seconds` | `8` | app_002 覆盖为 8s |
| `connector_platform` | `max_qps` | `1000` | 平台默认 QPS |
| `connector_platform` | `log_collection_enabled` | `true` | 平台默认日志采集开启 |
| `connector_platform_app_002` | `log_collection_enabled` | `false` | app_002 关闭日志采集 |

**查询逻辑**：
```
不支持按应用区分的项（path 固定为 connector_platform）:
  SELECT value FROM openplatform_property_t
   WHERE path = 'connector_platform' AND code = 'connector_max_versions'

支持按应用区分的项（优先查应用专属路径）:
  1. SELECT value FROM openplatform_property_t
      WHERE path = 'connector_platform_app_{appId}' AND code = '{key}'
     → 命中则返回（应用覆盖值）
  2. 未命中 → SELECT value FROM openplatform_property_t
                WHERE path = 'connector_platform' AND code = '{key}'
     → 返回平台默认值
  3. 仍未命中 → 使用代码级 fallback 常量
```

**实例推演**：
- `app_001` 查 `max_timeout_seconds` → 命中 `(connector_platform_app_001, max_timeout_seconds)`，返回 `10`
- `app_003` 查 `max_timeout_seconds` → `connector_platform_app_003` 未命中 → 回退 `(connector_platform, max_timeout_seconds)`，返回 `5`
- `app_002` 查 `log_collection_enabled` → 命中 `(connector_platform_app_002, log_collection_enabled)`，返回 `false`
- 任意应用查 `connector_max_versions` → 直接查 `(connector_platform, connector_max_versions)`，返回 `1000`（不支持按应用区分）

**读取方式**：
- open-server 通过 MyBatis Mapper 直查 `openplatform_property_t`，按 `(path, code)` 查询
- connector-api 通过 R2DBC 直查同表，Redis 缓存（TTL 7d±2h），miss 回源 MySQL
- 版本发布/部署时主动清空对应应用路径的 Property 缓存

**与 Lookup 的区别**：

| 维度 | Lookup | Property |
|------|--------|----------|
| 数据库表 | `openplatform_lookup_classify_t` + `openplatform_lookup_item_t` | `openplatform_property_t` |
| 数据结构 | 分组-项 二级结构（1:N 列表） | 键值对（`path` 命名空间 + `code` 配置项） |
| 是否支持按应用覆盖 | ❌（全局值） | ✅（`path` 追加 `_app_{appId}` 后缀） |
| 回退模型 | 无 | 二层回退（应用值 > 平台默认值） |
| V3 用途 | 开放应用范围清单 | 超时/QPS/并发/缓存/分支/脚本/日志等 15 项 |
| path 约定 | `connector_platform` | `connector_platform` 或 `connector_platform_app_{appId}` |
| open-server 读取 | `LookupWhitelistMapper` 联查两张表 | MyBatis Mapper 直查 `openplatform_property_t` |

> 💡 **命名空间隔离**：`path=connector_platform` 将连接器平台的全部配置收敛到一个命名空间下，与 path 下其他业务模块（如能力开放平台 `capability_open_platform`）互不干扰。`code` 不再需要业务前缀，简洁直观。

## 附录 B：关联文档索引

| 文档 | 内容 |
|------|------|
| [spec.md §1.6](./spec.md#16-关键设计决策) | 关键设计决策：超时归属、限流归属、缓存、脚本节点、日志采集开关、引用稽核 |
| [spec.md §3.3](./spec.md#33-连接器配置g3g12) | 连接器配置 FR（认证类型、凭证位置、URL 白名单） |
| [spec.md §3.7](./spec.md#37-连接流编排--流级配置g8) | 流级配置 FR（超时、入站限流、SYSACCOUNT白名单、缓存） |
| [spec.md §3.8e](./spec.md#38e-连接流编排--脚本节点g8) | 脚本节点 FR（数量限制、源码长度、超时范围） |
| [spec.md §5.5](./spec.md#55-依赖关系) | 依赖关系：应用级参数存储于 market-server |
| [spec.md §6](./spec.md#6-边界情况) | 边界情况：EC-001~032 覆盖配置异常场景 |
| [plan-db.md](./plan-db.md) | 数据库设计：配置快照存储于 version 表的 JSON 列 |
| [plan-api.md](./plan-api.md) | API 设计：配置相关端点 |
| [plan-script.md](./plan-script.md) | 脚本执行引擎：GraalJS 沙箱配置细节 |
| [plan-cache.md](./plan-cache.md) | 缓存方案：缓存配置与版本切换一致性 |

## 附录 C：修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-06-26 | 初始版本：全量配置场景系统化记录（开关/白名单/上限/阈值/审批人/实体配置/校验策略） | SDDU |
| v1.2 | 2026-06-26 | 重构附录：新增 A.1 Spring 配置文件（open-server + connector-api），原 A.1 改名为 A.2 业务对象 JSON 配置 | SDDU |
| v1.1 | 2026-06-26 | 重构目录结构：拆为配置清单（§1）+ 配置详情（§2）+ 附录：平台配置能力（配置文件/Lookup/Property） | SDDU |
| v2.0 | 2026-06-29 | §3 Lookup 化优化方案：将 15 项配置从 Property 迁移到 Lookup 批量读取，查询次数从 16 次降至 2 次；命名统一 PascalCase.点号（path=CEC.Open, classify_code=Connector.Platform.Config 等） | SDDU |
| v2.1 | 2026-06-30 | 统一命名：§2/§2.5 的 Property 残留命名全部对齐 §1 Lookup 命名（path=CEC.Open, classify_code/item_code=PascalCase）；修正默认值（#3/#7=0, #14=5）；补充遗漏的 #12（串行编排节点上限）；删除已废弃的 §2.6 Property 化实施路线 | SDDU |
| v2.2 | 2026-07-08 | §4 配置生效接口矩阵；§2 全量实现状态审计验证（16 项均已接入 Lookup，更新#1~#16标注+概况+方案为实际代码位置）；修订文档说明 | SDDU |

---

*最后更新: 2026-07-08 | 基于 spec.md v3.0 + plan.md v3.0*
