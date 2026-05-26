# 任务分解：JSON Schema v5.5 升级实施

**Feature ID**: CONN-PLAT-001  
**创建日期**: 2026-05-26  
**对齐基线**: plan-json-schema.md v5.5 / plan-json-schema-impl.md v1.0 / plan-db.md v3.0 / plan-api.md v3.0 / build.md  
**背景**: 全部 10 个实施任务（TASK-001~012）已完成（参考 build.md），但代码基于旧 JSON Schema 格式实现。本任务集聚焦**将已有代码升级到 plan-json-schema.md v5.5**。

---

## 任务汇总

| 总计 | 当前实施任务 | 外部占位任务 | S | M | L | 波次 |
|:----:|:-----------:|:-----------:|:--:|:--:|:--:|:----:|
| 12 | 10 | 2 | 2 | 6 | 2 | 4 |

### 三大核心升级模块 → 任务映射

| 核心模块 | 变更内容 | open-server 任务 | connector-api 任务 | 前端（外部占位） |
|:---------|:---------|:---------------:|:-----------------:|:---------------:|
| **🔌 连接器连接配置** | `connectionConfig` 字段重命名 + `inputContract`/`outputContract` 协议感知分段 + `authConfig` 字符串枚举 | [U01] [U04] | [U02] | [U07] 占位 |
| **🔀 连接流编排配置** | `orchestrationConfig` React Flow 格式 + `inputMapping`/`outputMapping` 分段 + JSON Path 表达式 + `trigger.type` 去 test | [U01] [U04] | [U02] [U06] | [U08] 占位 |
| **⚙️ 运行时解析执行** | 表达式解析引擎升级 + NodeContext 构造 + 分段 mapping 解析 → HTTP 请求构造 + `errorInfo` 新格式 | — | [U05] [U06] | — |

> 横切任务：[U03](#task-u03-jackson-序列化兼容配置) Jackson 兼容配置 / [U09](#task-u09-java-单元测试-json-数据对齐) Java 单元测试 / [U10](#task-u10-错误处理--限流--审计适配) 错误限流审计 / [U11](#task-u11-python-真调接口测试-json-数据对齐) Python 接口测试 / [U12](#task-u12-全链路回归验证) 回归验证

---

## 升级范围总览

```
plan-json-schema.md v5.5 变更                         涉及代码层
──────────────────────────                           ──────────
① 字段重命名                                         POJO / Entity / DTO / Controller
   authTypeSchema → authConfig
   inputSchema → inputContract
   outputSchema → outputContract
   rateLimit → rateLimitConfig
   outputFields[] → outputMapping{}

② React Flow 格式                                    Jackson 序列化 / 前端
   node.{id,type,position} + node.data.{...}
   edge.{source,target} (非 sourceNodeId/targetNodeId)
   edge.data.businessType

③ 协议感知 Contract                                  POJO / Controller / 前端
   inputContract: {protocol, header, query, body}
   outputContract: {protocol, header, body}

④ inputMapping 分段                                  Controller / 运行时引擎 / 前端
   {header:{...}, query:{...}, body:{...}}

⑤ 表达式升级                                        运行时引擎 expression parser
   ${trigger.xxx} → ${$.node.trigger.input.xxx}

⑥ trigger.type 枚举移除 test                        TriggerData POJO

⑦ authConfig.type 字符串枚举                         AuthConfig POJO

⑧ errorInfo 结构升级                                 ErrorInfo POJO
   code 数字字符串 + oneOf 约束
```

---

## Wave 1：数据模型层（3 任务 · 可并行）

---

## TASK-U01: open-server POJO / DTO 字段升级

**复杂度**: M  
**前置依赖**: 无  
**执行波次**: 1  

### 描述
将 open-server 中所有与 JSON Schema 相关的 POJO 和 DTO 字段名对齐 plan-json-schema.md v5.5。这是纯重命名 + 结构调整，不涉及业务逻辑变更。

涉及文件（全部 MODIFY）：
- `open-server/.../modules/connector/entity/ConnectorVersion.java` — connectionConfig Jackson 反序列化字段名
- `open-server/.../modules/connector/dto/*.java` — 连接器配置请求/响应 DTO
- `open-server/.../modules/flow/entity/FlowVersion.java` — orchestrationConfig 反序列化
- `open-server/.../modules/flow/dto/*.java` — 连接流配置请求/响应 DTO
- `open-server/.../modules/debug/DebugProxyService.java` — 测试运行请求构造

### 验收标准
- [ ] `connectionConfig` POJO：`authTypeSchema` → `authConfig`，`inputSchema` → `inputContract`，`outputSchema` → `outputContract`，`rateLimit` → `rateLimitConfig`
- [ ] `inputContract` / `outputContract` POJO 新增 `protocol` 字段 + `header`/`query`/`body` 分段结构
- [ ] `orchestrationConfig` POJO：React Flow 格式 — `node.data` 嵌套（`labelCn`/`type`/`authConfig` 等迁入 data 子对象）
- [ ] 边 POJO：`sourceNodeId` → `source`，`targetNodeId` → `target`
- [ ] `inputMapping` POJO：从 `Map<String,String>` 改为结构化对象（`header`/`query`/`body` 各为 `Map<String,String>`）
- [ ] `outputMapping` POJO：从 `List<String>` 改为结构化对象（`header`/`body` 各为 `Map<String,String>`）
- [ ] `triggerData` POJO：`type` 枚举移除 `test`
- [ ] `authConfig` POJO：`type` 字段为 String（非 int）
- [ ] `errorInfo` POJO：`code` 为 String，`message` → `messageZh` + `messageEn`，新增 `cause`/`downstreamStatus`/`downstreamBody`
- [ ] 编译通过（旧字段无残留引用）

### 验证命令
```bash
cd open-server && mvn compile
# 预期：无编译错误，无旧字段名残留
```

---

## TASK-U02: connector-api R2DBC Entity / Model 字段升级

**复杂度**: M  
**前置依赖**: 无（可与 TASK-U01 并行）  
**执行波次**: 1  

### 描述
将 connector-api 中所有与 JSON Schema 相关的 R2DBC Entity 和运行时 Model 字段名对齐 plan-json-schema.md v5.5。

涉及文件（全部 MODIFY）：
- `connector-api/.../modules/connector/entity/ConnectorVersionEntity.java`
- `connector-api/.../modules/flow/entity/FlowVersionEntity.java`
- `connector-api/.../modules/runtime/model/NodeOutput.java`
- `connector-api/.../modules/runtime/model/ExecutionResult.java`
- `connector-api/.../modules/runtime/context/ExecutionContext.java`

### 验收标准
- [ ] ConnectorVersionEntity / FlowVersionEntity 的 JSON 反序列化字段名对齐 U01
- [ ] NodeOutput 新增 `nodeId`/`nodeType`/`input`(Map)/`output`(Map)/`status`/`errorInfo` 字段
- [ ] ExecutionResult 中 `errorInfo` 结构升级（code String + messageZh/messageEn + oneOf 约束）
- [ ] ExecutionContext 中 `nodeContexts` 改为 `Map<String, NodeContext>`（新增 NodeContext 类，含 input/output 分区）
- [ ] 编译通过

### 验证命令
```bash
cd connector-api && mvn compile
```

---

## TASK-U03: Jackson 序列化兼容配置

**复杂度**: S  
**前置依赖**: TASK-U01, TASK-U02  
**执行波次**: 1  

### 描述
配置 Jackson 以支持新 JSON 格式的序列化/反序列化，同时保留对旧格式的向后兼容（过渡期）。

涉及文件（全部 MODIFY）：
- `open-server/.../common/config/JacksonConfig.java`（或新建）
- `connector-api/.../common/config/JacksonConfig.java`（或新建）

### 验收标准
- [ ] `connectionConfig` 反序列化：新格式（`authConfig`/`inputContract`）正常解析；旧格式（`authTypeSchema`/`inputSchema`）通过 `@JsonAlias` 兼容，日志打印 deprecation warning
- [ ] `orchestrationConfig` 反序列化：React Flow 格式正常解析；旧扁平格式通过 `@JsonAlias` 兼容 labelCn/labelEn 等字段名
- [ ] `_$$_` 占位说明Jackson需配置忽略未知JSON属性
- [ ] 枚举字段序列化：Jackson 配置 `authConfig.type` 字符串枚举映射到 Java enum
- [ ] 单元测试覆盖新旧两种格式反序列化

### 验证命令
```bash
cd open-server && mvn test -Dtest=JacksonDeserializationTest
cd connector-api && mvn test -Dtest=JacksonDeserializationTest
```

---

## Wave 2：业务逻辑层（3 任务 · 可并行）

---

## TASK-U04: open-server Controller / Service 适配

**复杂度**: M  
**前置依赖**: TASK-U01, TASK-U03  
**执行波次**: 2  

### 描述
更新 open-server 中 Controller 和 Service 层代码，适配新 POJO 结构。curl 验证命令中的 JSON 示例同步更新。

涉及文件（全部 MODIFY）：
- `open-server/.../modules/connector/controller/ConnectorController.java`
- `open-server/.../modules/connector/service/ConnectorService.java`
- `open-server/.../modules/flow/controller/FlowController.java`
- `open-server/.../modules/flow/service/FlowService.java`
- `open-server/.../modules/debug/DebugProxyController.java`
- `open-server/.../modules/debug/DebugProxyService.java`

### 验收标准
- [ ] API #7 `PUT .../config`：请求体 `connectionConfig` 使用新字段名（`authConfig.inputContract.outputContract.rateLimitConfig`）
- [ ] API #9~#16：Flow 创建/编辑/启停接口适配新 POJO
- [ ] API #15~#16：`orchestrationConfig` 请求/响应使用 React Flow 格式（`node.data` 嵌套 + `edge.source/target`）
- [ ] API #17 test-run：转发至 connector-api 的请求体使用新 JSON 格式
- [ ] 所有接口的 curl 示例对齐 plan-api.md v3.0
- [ ] 编译通过 + 接口回归测试通过

### 验证命令
```bash
# 编辑连接配置（新字段名）
curl -X PUT http://localhost:18080/open-server/api/v1/connectors/{id}/config \
  -H 'Content-Type: application/json' \
  -d '{"connectionConfig":{"protocol":"HTTP","protocolConfig":{"url":"https://api.example.com","method":"POST"},"authConfig":{"type":"AKSK","fields":[{"name":"accessKey","carrier":"header","fieldName":"AK","required":true,"sensitive":true}]},"inputContract":{"protocol":"HTTP","body":{"type":"object","properties":{"message":{"type":"string"}},"required":["message"]}},"outputContract":{"protocol":"HTTP","body":{"type":"object","properties":{"msgId":{"type":"string"}}}},"timeoutMs":30000,"rateLimitConfig":{"maxQps":10}}}'

# 保存编排配置（React Flow 格式）
curl -X PUT http://localhost:18080/open-server/api/v1/flows/{id}/config \
  -H 'Content-Type: application/json' \
  -d '{"orchestrationConfig":{"nodes":[{"id":"node_trigger","type":"trigger","position":{"x":100,"y":200},"data":{"labelCn":"接收","labelEn":"Receive","type":"http","authConfig":{"type":"SYSTOKEN","fields":[{"name":"token","carrier":"header","fieldName":"X-Sys-Token"}]},"inputContract":{"protocol":"HTTP","body":{"type":"object","properties":{"sender":{"type":"string"}},"required":["sender"]}},"rateLimitConfig":{"maxQps":100}}},{"id":"node_exit","type":"exit","position":{"x":350,"y":200},"data":{"labelCn":"返回","labelEn":"Return","outputMapping":{"body":{"msgId":"${$.node.node_trigger.input.sender}","code":"constant:0"}}}}],"edges":[{"id":"e1","source":"node_trigger","target":"node_exit","type":"smoothstep","data":{"businessType":"default"}}]}}'
```

---

## TASK-U05: connector-api 运行时引擎适配

**复杂度**: L  
**前置依赖**: TASK-U02, TASK-U03  
**执行波次**: 2  

### 描述
更新 connector-api 运行时引擎以适配新 JSON 结构。核心变更：① 表达式解析器升级为 JSON Path 格式；② inputMapping/outputMapping 解析支持分段结构；③ NodeContext 构建逻辑按 plan-json-schema-impl.md §2 实现；④ ConnectorNodeExecutor 按分段 inputMapping 构造 HTTP 请求。

涉及文件（全部 MODIFY）：
- `connector-api/.../modules/runtime/executor/ReactiveSequentialExecutor.java`
- `connector-api/.../modules/runtime/context/ExecutionContext.java`
- `connector-api/.../modules/runtime/node/EntryNodeExecutor.java`
- `connector-api/.../modules/runtime/node/ConnectorNodeExecutor.java`
- `connector-api/.../modules/runtime/node/DataProcessorExecutor.java`
- `connector-api/.../modules/runtime/node/ExitNodeExecutor.java`

此外需要 NEW 文件：
- `connector-api/.../modules/runtime/expression/ExpressionResolver.java` — 表达式解析引擎
- `connector-api/.../modules/runtime/context/NodeContext.java` — 节点上下文对象

### 验收标准
- [ ] `ExpressionResolver` 实现三模式解析（见 impl.md §3）：
  - `constant:xxx` → 直接返回字面值
  - `${$.node.{nodeId}.{input/output}.{path}}` → 从 NodeContext 取值
  - 向后兼容旧 `${nodeId.fieldPath}` 格式（默认查 output 分区）
- [ ] `NodeContext` 含 `input`(Map) + `output`(Map) 两个分区
- [ ] `EntryNodeExecutor`：从 HTTP 请求构造 trigger 的 `NodeContext.input`（含 `authConfig` 声明提取的凭证字段）
- [ ] `ConnectorNodeExecutor`：按分段 `inputMapping` 解析 header/query/body 各部分表达式 → 构造 HTTP 请求（固定头 + 动态头 + authConfig 注入 + body）
- [ ] `ConnectorNodeExecutor`：按 `outputContract` 分段提取响应构造 `NodeContext.output`
- [ ] `DataProcessorExecutor`：`fieldMappings[].source` 兼容新旧表达式格式
- [ ] `ExitNodeExecutor`：按 `outputMapping` 分段（header/body）解析表达式 → 构造最终 HTTP 响应
- [ ] `ReactiveSequentialExecutor`：`ExecutionContext.nodeContexts` 改为 `Map<String, NodeContext>`，下游可访问所有前置 NodeContext
- [ ] 单元测试覆盖各种表达式解析 + mapping 解析

### 验证命令
```bash
cd connector-api && mvn test -Dtest=ExpressionResolverTest
cd connector-api && mvn test -Dtest=ConnectorNodeExecutorTest
cd connector-api && mvn test -Dtest=ReactiveSequentialExecutorTest
```

---

## TASK-U06: connector-api HTTP 触发 + 测试端点适配

**复杂度**: M  
**前置依赖**: TASK-U05  
**执行波次**: 2  

### 描述
更新 HTTP 触发端点和测试执行端点以适配新的 NodeContext 和 ExecutionResult 结构。

涉及文件（全部 MODIFY）：
- `connector-api/.../modules/trigger/controller/TriggerController.java`
- `connector-api/.../modules/trigger/service/TriggerService.java`
- `connector-api/.../modules/debug/controller/TestRunController.java`
- `connector-api/.../modules/debug/service/TestRunService.java`

### 验收标准
- [ ] HTTP 触发 `POST /api/v1/trigger/{flowId}/invoke`：
  - 解析 trigger 节点 `data.inputContract` 校验请求体
  - 解析 `data.authConfig` 提取凭证类型声明
  - 校验 `data.rateLimitConfig.maxQps` 限流
  - 触发请求体中 `trigger.type` 仅 `http`/`manual`（不含 test）
- [ ] 测试执行端点：
  - `triggerType = 3`（运行时记录维度，非编排 JSON 中）
  - `isTest = true`
- [ ] 响应格式含 `executionId`(string) + `status`(TINYINT) + `steps`(含 nodeId/input/output/duration/errorInfo) + `resultData` + `durationMs`
- [ ] `errorInfo` 响应符合 oneOf 约束：6xxxx → `cause`，4xx/5xx → `downstreamStatus`

### 验证命令
```bash
# HTTP 触发
curl -X POST http://localhost:18180/connector-api/api/v1/trigger/{flowId}/invoke \
  -H 'X-Sys-Token: test-token' -H 'Content-Type: application/json' \
  -d '{"sender":"ext_sys","content":"hello"}'

# 测试运行
curl -X POST http://localhost:18080/open-server/api/v1/flows/{flowId}/test-run \
  -H 'Content-Type: application/json' \
  -d '{"mockTriggerData":{"sender":"test"},"credentials":{}}'
```

---

## Wave 3：前端 + 验证（5 任务 · 分段并行）

---

## TASK-U07: wecodesite 连接器配置页面适配

**类型**: 外部占位任务  
**实施范围**: 不在此 Tasks 文档实施  
**执行方式**: 由其他渠道并行完成  
**对齐基线**: plan-json-schema.md v5.5 / plan-api.md v3.0

> 说明：仅保留标题占位，不展开描述、涉及文件、验收标准、验证命令；不作为当前 SDDU build 阶段的实施任务或强依赖。需适配变更：`connectionConfig` 字段重命名（`authConfig`/`inputContract`/`outputContract`/`rateLimitConfig`）+ `inputContract`/`outputContract` 协议感知分段编辑 UI。

---

## TASK-U08: wecodesite 编排画布 + 连接流页面适配

**类型**: 外部占位任务  
**实施范围**: 不在此 Tasks 文档实施  
**执行方式**: 由其他渠道并行完成  
**对齐基线**: plan-json-schema.md v5.5 / plan-api.md v3.0

> 说明：仅保留标题占位，不展开描述、涉及文件、验收标准、验证命令；不作为当前 SDDU build 阶段的实施任务或强依赖。需适配变更：React Flow 格式（`node.data` 嵌套 + `edge.source`/`target`）+ `inputMapping`/`outputMapping` 分段编辑 + JSON Path 表达式（`${$.node.{id}.{input/output}.{path}}`）+ `trigger.type` 仅 http/manual。

---

## TASK-U09: Java 单元测试 JSON 数据对齐

**复杂度**: M  
**前置依赖**: TASK-U01, U04（open-server POJO/API 就绪）；TASK-U02, U05, U06（connector-api Entity/引擎/端点就绪）  
**执行波次**: 3  

### 描述
将 open-server 和 connector-api 中所有 Java 单元测试的 JSON fixture/mock 数据对齐 plan-json-schema.md v5.5。参考测试目录 `open-server/src/test/java/` 和 `connector-api/src/test/java/`。

涉及文件（全部 MODIFY）：

**open-server**：
- `src/test/.../modules/connector/controller/ConnectorControllerTest.java`
- `src/test/.../modules/connector/controller/ConnectorControllerWebMvcTest.java`
- `src/test/.../modules/connector/service/ConnectorServiceTest.java`
- `src/test/.../modules/flow/controller/FlowControllerTest.java`
- `src/test/.../modules/flow/controller/FlowControllerWebMvcTest.java`
- `src/test/.../modules/flow/service/FlowServiceTest.java`
- `src/test/.../modules/debug/DebugProxyTest.java`
- `src/test/.../modules/debug/DebugProxyControllerWebMvcTest.java`

**connector-api**：
- `src/test/.../modules/runtime/executor/ReactiveSequentialExecutorTest.java`
- `src/test/.../modules/runtime/node/NodeExecutorsTest.java`
- `src/test/.../modules/runtime/context/ExecutionContextTest.java`
- `src/test/.../modules/trigger/TriggerServiceTest.java`
- `src/test/.../modules/trigger/controller/TriggerControllerWebFluxTest.java`
- `src/test/.../integration/ConnectorFlowE2ETest.java`
- `src/test/.../common/ContractSchemaTest.java`
- `src/test/.../common/interceptor/RateLimitFilterTest.java`

### 验收标准
- [ ] **open-server 测试**：所有 `connectionConfig` Mock JSON 使用新字段名（`authConfig`/`inputContract`/`outputContract`/`rateLimitConfig`）
- [ ] **open-server 测试**：所有 `orchestrationConfig` Mock JSON 使用 React Flow 格式（`{id, type, position, data: {...}}`）
- [ ] **open-server 测试**：边 JSON 使用 `source`/`target`（非 `sourceNodeId`/`targetNodeId`）
- [ ] **connector-api 测试**：`ConnectorFlowE2ETest` JSON fixture 全面对齐新格式
- [ ] **connector-api 测试**：运行时引擎测试（`ReactiveSequentialExecutorTest`/`NodeExecutorsTest`）Mock 数据使用分段 `inputMapping`/`outputMapping`
- [ ] **connector-api 测试**：表达式测试 Mock 数据使用 `${$.node.{id}.{input/output}.xxx}` 格式
- [ ] **connector-api 测试**：`errorInfo` Mock 数据含 `code`(String) + `messageZh`/`messageEn` + oneOf 约束
- [ ] **connector-api 测试**：`triggerData.type` 仅 `http`/`manual`
- [ ] **connector-api 测试**：`ContractSchemaTest` 验证新 JSON Schema 格式
- [ ] 全部测试通过（open-server `mvn test` + connector-api `mvn test`）

### 验证命令
```bash
# open-server 连接器/连接流相关测试
cd open-server
mvn test -Dtest="ConnectorServiceTest,ConnectorControllerWebMvcTest,FlowServiceTest,FlowControllerWebMvcTest,DebugProxyTest"

# connector-api 运行时相关测试
cd connector-api
mvn test -Dtest="ReactiveSequentialExecutorTest,NodeExecutorsTest,ExecutionContextTest,ConnectorFlowE2ETest"

# 全量回归
cd open-server && mvn test
cd connector-api && mvn test
```

---

## Wave 4：横切与收尾（3 任务）

---

## TASK-U10: 错误处理 + 限流 + 审计适配

**复杂度**: M  
**前置依赖**: TASK-U05, TASK-U06  
**执行波次**: 4  

### 描述
更新错误处理、限流、审计日志相关代码，适配新 JSON 结构（errorInfo 新格式 + rateLimitConfig 字段名）。

涉及文件（全部 MODIFY）：
- `connector-api/.../common/exception/DefaultErrorHandler.java`
- `connector-api/.../common/interceptor/RateLimitFilter.java`
- `open-server/.../common/interceptor/AuditLogAspect.java`

### 验收标准
- [ ] `DefaultErrorHandler`：构造 `errorInfo` 使用新格式 —— `code` 为数字字符串（如 `"6001"`），`message` 拆为 `messageZh`/`messageEn`
- [ ] `DefaultErrorHandler`：内部错误（6xxxx）携带 `cause`，下游错误（4xx/5xx）携带 `downstreamStatus` + `downstreamBody`（截断 512 字符）
- [ ] `RateLimitFilter`：读取 `trigger.data.rateLimitConfig.maxQps`（非旧名 `rateLimit`）
- [ ] `RateLimitFilter`：限流拒绝时 errorInfo 返回 `code: "429"` + `messageZh: "请求频率超限"` + `messageEn: "Too many requests"`
- [ ] `AuditLogAspect`：记录启停操作时正确引用新字段路径

### 验证命令
```bash
# 限流场景：超过 maxQps 的请求返回 429 + 新格式 errorInfo
for i in {1..20}; do
  curl -X POST http://localhost:18180/connector-api/api/v1/trigger/{flowId}/invoke \
    -H 'X-Sys-Token: test' -H 'Content-Type: application/json' -d '{"sender":"test"}' &
done
# 预期 429 响应体含 {"code":"429","messageZh":"请求频率超限","messageEn":"Too many requests"}
```

---

## TASK-U11: Python 真调接口测试 JSON 数据对齐

**复杂度**: M  
**前置依赖**: TASK-U04（open-server 业务层就绪）；TASK-U06（connector-api 端点就绪）  
**执行波次**: 4  
**测试方案参考**: [test-plan-integration.md](./test-plan-integration.md)

### 描述
将 open-server 和 connector-api 中所有 Python 真调接口测试脚本的请求体 JSON 对齐 plan-json-schema.md v5.5。参考现有测试目录 `open-server/src/test/python/inspect/`。

涉及文件（全部 MODIFY）：

**open-server**（18 个文件）：
- `src/test/python/inspect/connector_config_set.py` — `connectionConfig` 请求体字段重命名 + inputContract/outputContract 分段
- `src/test/python/inspect/connector_config_get.py` — 响应断言更新
- `src/test/python/inspect/flow_config_set.py` — `orchestrationConfig` 换 React Flow 格式 + inputMapping/outputMapping 分段
- `src/test/python/inspect/flow_config_get.py` — 响应断言更新
- `src/test/python/inspect/debug_test_run.py` — 请求体 JSON 对齐新格式
- `src/test/python/inspect/connector_create.py` — CRUD 请求体不做大改（基本字段无变），确认通过
- `src/test/python/inspect/connector_list.py` — 确认通过
- `src/test/python/inspect/connector_detail.py` — 确认通过
- `src/test/python/inspect/connector_update.py` — 确认通过
- `src/test/python/inspect/connector_delete.py` — 确认通过
- `src/test/python/inspect/flow_create.py` — 确认通过
- `src/test/python/inspect/flow_list.py` — 确认通过
- `src/test/python/inspect/flow_detail.py` — 确认通过
- `src/test/python/inspect/flow_update.py` — 确认通过
- `src/test/python/inspect/flow_delete.py` — 确认通过
- `src/test/python/inspect/flow_start.py` — 确认通过
- `src/test/python/inspect/flow_stop.py` — 确认通过
- `src/test/python/inspect/all.py` — 全量回归执行器（如涉及 JSON 断言则更新）

**connector-api**（可选，4 个文件）：
- `src/test/python/inspect/trigger_invoke.py` — HTTP 触发请求体对齐
- `src/test/python/inspect/contract_response.py` — L4 契约校验更新

### 验收标准
- [ ] `connector_config_set.py`：请求体使用新字段名（`authConfig`/`inputContract`/`outputContract`/`rateLimitConfig`）+ `inputContract` 含 `protocol` + 分段 headers
- [ ] `connector_config_get.py`：响应 `connectionConfig` JSON 断言使用新字段路径
- [ ] `flow_config_set.py`：请求体使用 React Flow 格式（`{id, type, position, data: {...}}`）+ 边使用 `source`/`target` + `edge.data.businessType`
- [ ] `flow_config_set.py`：connector 节点 `inputMapping` 分段（header/query/body）+ exit 节点 `outputMapping` 分段（header/body）
- [ ] `flow_config_set.py`：表达式使用 `${$.node.{id}.{input/output}.{path}}` 格式
- [ ] 所有 CRUD 接口脚本（创建/列表/详情/更新/删除/启停）正常执行，响应断言通过
- [ ] `all.py` 全量回归通过
- [ ] `--quiet` 模式输出 `X/X PASS, 0 FAIL`

### 验证命令
```bash
# open-server 全量接口测试
cd open-server/src/test/python/inspect
python3 all.py --quiet

# connector-api 接口测试（如已启动）
cd connector-api/src/test/python/inspect
python3 all.py --quiet
```

---

## TASK-U12: 全链路回归验证

**复杂度**: L  
**前置依赖**: TASK-U01 ~ U11 全部  
**执行波次**: 4  

### 描述
运行全部 Java 单元测试 + Python 集成测试，确保 JSON Schema 升级后全链路正常工作。覆盖旧数据向后兼容 + 新接口正确性。

### 验收标准
- [ ] **Java 单元测试**：`open-server mvn test` 全量通过（含 connector/flow/debug 模块）
- [ ] **Java 单元测试**：`connector-api mvn test` 全量通过（含 runtime/trigger/integration）
- [ ] **Python 集成测试**：`open-server inspect/all.py --quiet` 全量通过
- [ ] **Python 集成测试**：`connector-api inspect/all.py --quiet` 全量通过（如服务已启动）
- [ ] 向后兼容：旧 JSON 格式数据（如旧版 curl 示例）可被新代码正确解析（Jackson @JsonAlias 验证）
- [ ] 全链路：HTTP 触发 → trigger → connector（expression → HTTP → output）→ exit → response
- [ ] 错误场景：未运行（403）/ 限流（429）/ 编排为空（拒绝保存）

### 验证命令
```bash
# 1. Java 全量测试
cd open-server && mvn test
cd connector-api && mvn test

# 2. Python 全量接口测试
cd open-server/src/test/python/inspect && python3 all.py --quiet
cd connector-api/src/test/python/inspect && python3 all.py --quiet  # 如服务已启动

# 3. 预期输出：所有 PASS，无 FAIL
```

---

## 附录

### 变更文件汇总

| 工程 | MODIFY 文件数（约） | NEW 文件数 |
|:-----|:-----:|:-----:|
| **open-server** | ~18 | 0~1 |
| **connector-api** | ~16 | 2 |
| **open-server Java 测试** | ~8 | 0 |
| **connector-api Java 测试** | ~8 | 0 |
| **Python 接口测试** | ~20 | 0 |
| **wecodesite** | — | —（外部占位） |
| **合计** | ~70 | 2~3 |

### 依赖图

```
Wave 1:  [U01 open-server POJO]  [U02 connector-api Entity]  (并行)
                   │                     │
                   └──────┬──────────────┘
                          │
                     [U03 Jackson 配置]
                          │
Wave 2:  [U04 open-server 业务]   [U05 运行时引擎]  (并行)
                   │                     │
                   │                [U06 端点适配]
                   │                     │
Wave 3:       [U09 Java单元测试]          │         [U07 前端占位]
                   │                     │         [U08 前端占位]
                   │                     │         (由其他渠道并行)
                   └─────────────────────┘
                          │
Wave 4:    [U10 错误/限流/审计]    [U11 Python接口测试]
                          │               │
                          └──────┬────────┘
                                 │
                            [U12 回归验证]

注：U07/U08 为 wecodesite 外部占位任务，不作为当前 SDDU build 实施链路的强依赖。
```
---

**任务分解状态**: ✅ 完成  
**下一步**: 运行 `@sddu-build TASK-U01` 开始 JSON Schema 升级第一个任务
