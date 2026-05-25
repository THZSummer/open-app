# 🔍 测试方案：连接器平台 — 后端接口测试

**Feature ID**: CONN-PLAT-001  
**创建日期**: 2026-05-25  
**对齐基线**: spec.md v5.0 / plan.md v2.8.1 / plan-api.md v2.8.0  
**测试范围**: 后端 API #1~#18（open-server 17 个 + connector-api 1 个）  
**测试目标**: 逐个测试每个后端接口，验证是否符合 plan-api.md 设计预期

---

## 一、现有测试分析

### 1.1 现有测试清单（79 个）

| 模块 | 文件 | 测试数 | 覆盖内容 | 层次 |
|------|------|:------:|---------|:----:|
| ConnectorService | ConnectorServiceTest | 12 | CRUD + 配置查看/编辑 + 引用校验 | L1 |
| ConnectorController | ConnectorControllerTest | 7 | 请求委托 + 400/404 场景 | L1 |
| FlowService | FlowServiceTest | 15 | CRUD + 启停 + 配置/编排校验 | L1 |
| FlowController | FlowControllerTest | 9 | 请求委托 | L1 |
| DebugProxy | DebugProxyTest | 2 | 转发成功/失败 | L1 |
| ExecutionContext | ExecutionContextTest | 8 | 创建读写/引用解析/凭证清除 | L1 |
| NodeExecutors | NodeExecutorsTest | 6 | Entry/Connector/DataProcessor/Exit | L1 |
| ReactiveExecutor | ReactiveSequentialExecutorTest | 5 | 线性/空编排/未知节点/非法JSON | L1 |
| TriggerService | TriggerServiceTest | 3 | 触发成功/流不存在/执行异常 | L1 |
| RateLimitFilter | RateLimitFilterTest | 4 | 非触发放行/速率限流/独立ID | L1 |
| **合计** | **10 文件** | **79** | | **L1** |

### 1.2 关键缺口

| 缺口 | 描述 | 风险 |
|------|------|------|
| **L2 接口层测试缺失** | 无 `@WebMvcTest`/`WebTestClient` 测试，未验证 HTTP 序列化、状态码、请求绑定 | 接口兼容性问题无法提前发现 |
| **L4 契约测试缺失** | 无 JSON Schema 断言验证响应格式符合 plan-api.md 规范 | 响应格式漂移无法被检测 |
| **异常场景覆盖不足** | 缺少参数校验、边界值、非法输入等 HTTP 层面的异常路径测试 | 接口健壮性不足 |
| **雪花 ID 字符串类型** | 未验证 BIGINT → string 的 JSON 序列化 | JS 前端精度丢失风险 |
| **枚举 TINYINT 一致性** | 未验证枚举值统一返回数字 | 前后端枚举映射不一致 |

---

## 二、测试策略

### 2.1 测试层次

| 层次 | 目标 | 工具 | 优先级 | 状态 |
|------|------|------|:------:|:----:|
| **L1 — 单元测试** | Service 逻辑 + 运行时引擎 | JUnit 5 + Mockito | ✅ 已有 | ✅ 79 个 |
| **L2 — 接口层测试** | HTTP 请求绑定 + 响应序列化 + 状态码 | `@WebMvcTest` + `MockMvc`（open-server）/ `@WebFluxTest` + `WebTestClient`（connector-api） | **⭐ 新增** | ❌ 0 个 |
| **L3 — 集成测试** | 全链路（Controller → Service → DB） | `@SpringBootTest` + 内嵌 DB | ⚠️ 已有 1 个 | ⚠️ 需扩充 |
| **L4 — 契约测试** | API 响应格式一致性（JSON Schema） | JSON Schema 断言 + 字段类型/格式校验 | **⭐ 新增** | ❌ 0 个 |

### 2.2 测试工具选型

| 服务 | 框架 | 工具 | 说明 |
|------|------|------|------|
| open-server（#1~#17） | Spring MVC（Servlet） | `@WebMvcTest` + `MockMvc` | 仅启动 Controller 层，mock Service |
| connector-api（#18） | Spring WebFlux（Reactive） | `@WebFluxTest` + `WebTestClient` | 仅启动 Controller 层，mock Service |

### 2.3 测试数据约定

| 项目 | 约定 |
|------|------|
| 测试连接器 ID | `"1000000000000000001"`（雪花 ID 字符串） |
| 测试连接流 ID | `"2000000000000000001"`（雪花 ID 字符串） |
| 测试版本 ID | `"3000000000000000001"`（雪花 ID 字符串） |
| 枚举字段 | 统一 TINYINT 数字（int 类型） |
| 时间字段 | ISO 8601 格式（`yyyy-MM-dd'T'HH:mm:ss.SSSXXX`）|
| 响应格式 | 统一 `{ code, messageZh, messageEn, data, page }` |

---

## 三、L2 接口层测试用例（按模块分组）

### 3.1 连接器 CRUD — ConnectorController（#1~#5）

#### #1 POST /api/v1/connectors — 创建连接器

| 编号 | 场景 | 输入 | 预期 HTTP 状态 | 验证点 |
|------|------|------|:--------------:|--------|
| TC-001 | ✅ 正常创建（完整字段） | body: nameCn/nameEn/iconFileId/descriptionCn/descriptionEn/connectorType=1 | 200 | `data.connectorId` 为字符串；`status=1`；`createTime` ISO 8601 格式 |
| TC-002 | ❌ 缺少必填 nameCn | body 无 nameCn | 400 | `messageZh` 含"参数错误" |
| TC-003 | ❌ connectorType 非法 | connectorType=99 | 422 | 校验失败提示 |
| TC-004 | ❌ nameCn 超长（>500 字符） | 超长字符串 | 422 | 字段长度校验 |
| TC-005 | ❌ 未认证 | 无 Cookie/SSO | 401 | 未授权提示 |
| TC-006 | ❌ 非管理员 | 普通用户权限 | 403 | 无权限提示 |

#### #2 GET /api/v1/connectors — 查询列表

| 编号 | 场景 | 输入 | 预期 | 验证点 |
|------|------|------|:----:|--------|
| TC-007 | ✅ 默认分页 | 无参数 | 200 | `page.curPage=1`、`pageSize=20`、`data` 为数组 |
| TC-008 | ✅ connectorType 过滤 | `?connectorType=1` | 200 | 仅返回 type=1 的连接器 |
| TC-009 | ✅ keyword 搜索 | `?keyword=IM` | 200 | nameCn/nameEn 模糊匹配 |
| TC-010 | ✅ 自定义分页 | `?curPage=2&pageSize=10` | 200 | 分页参数正确 |
| TC-011 | ❌ pageSize 超限 | `?pageSize=200` | 200 | 截断为 100（非 400） |
| TC-012 | ✅ 空结果 | `?keyword=NONEXISTENT` | 200 | `data=[]`, `page.total=0` |

#### #3 GET /api/v1/connectors/{connectorId} — 查询详情

| 编号 | 场景 | 预期 | 验证点 |
|------|------|:----:|--------|
| TC-013 | ✅ 正常查询 | 200 | 返回完整字段：nameCn/nameEn/connectorType/status/createTime/lastUpdateTime |
| TC-014 | ❌ connectorId 不存在 | 404 | 资源不存在提示 |
| TC-015 | ❌ connectorId 格式非法（字母） | 400 | 参数错误 |
| TC-016 | ✅ 雪花 ID 为 string 类型 | 200 | JSON 中 connectorId 为 `"string"` 非数字 |

#### #4 PUT /api/v1/connectors/{connectorId} — 更新

| 编号 | 场景 | 输入 | 预期 | 验证点 |
|------|------|------|:----:|--------|
| TC-017 | ✅ 正常更新 | 完整 body | 200 | `lastUpdateTime` 更新 |
| TC-018 | ✅ 仅部分字段 | 仅传 nameCn | 200 | 其余字段保持不变 |
| TC-019 | ❌ 不存在的连接器 | ID 不存在 | 404 | 资源不存在 |
| TC-020 | ❌ body 为空 | `{}` | 400 | 至少更新一个字段 |

#### #5 DELETE /api/v1/connectors/{connectorId} — 删除

| 编号 | 场景 | 预期 | 验证点 |
|------|------|:----:|--------|
| TC-021 | ✅ 正常删除（无引用） | 200 | 删除成功 |
| TC-022 | ❌ 被连接流引用 | 409 | 引用冲突提示 |
| TC-023 | ❌ 不存在 | 404 | 资源不存在 |

---

### 3.2 连接器配置 — ConnectorController（#6~#7）

#### #6 GET /api/v1/connectors/{connectorId}/config — 获取连接配置

| 编号 | 场景 | 预期 | 验证点 |
|------|------|:----:|--------|
| TC-024 | ✅ 已配置 | 200 | `connectionConfig` 含 protocol/protocolConfig/authTypeSchema/inputSchema/outputSchema/timeoutMs/rateLimit |
| TC-025 | ✅ 未配置（空） | 200 | `connectionConfig` 为 `{}` 或 null |
| TC-026 | ❌ 连接器不存在 | 404 | 资源不存在 |

#### #7 PUT /api/v1/connectors/{connectorId}/config — 编辑连接配置

| 编号 | 场景 | 输入 | 预期 | 验证点 |
|------|------|------|:----:|--------|
| TC-027 | ✅ 正常编辑（全文替换） | 完整 connectionConfig | 200 | `lastUpdateTime` 更新 |
| TC-028 | ❌ connectionConfig 为空 | `{"connectionConfig": {}}` | 422 | 配置不能为空 |
| TC-029 | ❌ protocol 非法 | `protocol: "GRPC"` | 422 | MVP 仅支持 HTTP |
| TC-030 | ❌ timeoutMs < 0 | `timeoutMs: -1` | 422 | 校验失败 |
| TC-031 | ❌ authTypeSchema.type 非法 | `type: "INVALID"` | 422 | 校验失败 |
| TC-032 | ✅ 编辑即生效验证 | 编辑后 GET | 200 | 返回新配置 |

---

### 3.3 连接流 CRUD — FlowController（#8~#14）

#### #8 POST /api/v1/flows — 创建连接流

| 编号 | 场景 | 预期 | 验证点 |
|------|------|:----:|--------|
| TC-033 | ✅ 正常创建 | 201 | `flowId` 字符串, `lifecycleStatus=0` |
| TC-034 | ❌ 缺少必填名称 | 400 | 参数错误 |
| TC-035 | ❌ 非管理员 | 403 | 无权限 |

#### #9 GET /api/v1/flows — 查询流列表

| 编号 | 场景 | 输入 | 预期 |
|------|------|------|:----:|
| TC-036 | ✅ 默认分页 | 无参数 | 200, data 数组 |
| TC-037 | ✅ lifecycleStatus 过滤 | `?lifecycleStatus=1` | 仅 running 状态 |
| TC-038 | ✅ keyword 搜索 | `?keyword=通知` | 模糊匹配 |
| TC-039 | ✅ 空结果 | `?keyword=NONEXISTENT` | data=[] |

#### #10 GET /api/v1/flows/{flowId} — 查看流详情

| 编号 | 场景 | 预期 |
|------|------|:----:|
| TC-040 | ✅ 正常 | 200, 完整字段含 lifecycleStatus/currentPublishedVersionId |
| TC-041 | ❌ 不存在 | 404 |

#### #11 PUT /api/v1/flows/{flowId} — 更新流信息

| 编号 | 场景 | 预期 |
|------|------|:----:|
| TC-042 | ✅ 正常更新 | 200 |
| TC-043 | ❌ 不存在 | 404 |

#### #12 DELETE /api/v1/flows/{flowId} — 删除流

| 编号 | 场景 | 预期 |
|------|------|:----:|
| TC-044 | ✅ stopped 状态可删除 | 200 |
| TC-045 | ❌ running 状态拒绝 | 409 → 需先 stop |
| TC-046 | ❌ 不存在 | 404 |

#### #13 POST /api/v1/flows/{flowId}/start — 启动

| 编号 | 场景 | 预期 |
|------|------|:----:|
| TC-047 | ✅ stopped → running | 200, lifecycleStatus=1 |
| TC-048 | ❌ 已是 running（重复） | 409 |
| TC-049 | ❌ undeployed（无配置） | 409 |
| TC-050 | ❌ 不存在 | 404 |

#### #14 POST /api/v1/flows/{flowId}/stop — 停止

| 编号 | 场景 | 预期 |
|------|------|:----:|
| TC-051 | ✅ running → stopped | 200, lifecycleStatus=2 |
| TC-052 | ❌ 已是 stopped | 409 |
| TC-053 | ❌ 不存在 | 404 |

---

### 3.4 连接流配置 — FlowController（#15~#16）

#### #15 GET /api/v1/flows/{flowId}/config — 获取编排配置

| 编号 | 场景 | 预期 |
|------|------|:----:|
| TC-054 | ✅ 已配置 | 200, `orchestrationConfig.nodes[]` + `edges[]` |
| TC-055 | ✅ 空配置（初始） | 200, nodes=[], edges=[] |
| TC-056 | ❌ flow 不存在 | 404 |
| TC-057 | ✅ 节点结构校验 | 每个 node 含 id/type/labelCn/labelEn/position |
| TC-058 | ✅ 节点类型枚举 | type 为 entry/connector/data_processor/exit |

#### #16 PUT /api/v1/flows/{flowId}/config — 保存编排配置

| 编号 | 场景 | 预期 |
|------|------|:----:|
| TC-059 | ✅ 正常保存 | 200 |
| TC-060 | ❌ nodes 为空 | 422（FR-017 空节点拒绝） |
| TC-061 | ❌ 缺失 exit 节点 | 422 |
| TC-062 | ❌ connector 节点引用不存在的 connectorVersionId | 422 |
| TC-063 | ❌ node.type 非法 | 422 |
| TC-064 | ✅ 编辑即生效 | 保存后 GET 验证 |

---

### 3.5 测试代理 — DebugProxyController（#17）

#### #17 POST /api/v1/flows/{flowId}/test-run — 测试运行

| 编号 | 场景 | 预期 |
|------|------|:----:|
| TC-065 | ✅ 正常测试运行 | 200, executionId/status=2/steps[] 完整 |
| TC-066 | ✅ isTest=true 标记 | 返回 `isTest=true` |
| TC-067 | ❌ flow 不存在 | 404 |
| TC-068 | ❌ mockTriggerData 不匹配 inputSchema | 422 |
| TC-069 | ✅ 凭证传递处理 | credentials 传递给下游节点 |
| TC-070 | ❌ 连接器超时 | 504 |
| TC-071 | ✅ 步骤详情完整 | 含 nodeId/nodeType/status/inputData/outputData/durationMs |

---

### 3.6 HTTP 触发 — TriggerController（#18）

#### #18 POST /api/v1/trigger/{flowId}/invoke — HTTP 触发

| 编号 | 场景 | 预期 |
|------|------|:----:|
| TC-072 | ✅ 正常触发（running + 合法凭证） | 200, executionId/status=2/resultData/durationMs |
| TC-073 | ❌ flow 未运行（stopped） | 403, messageZh="连接流未运行" |
| TC-074 | ❌ flow 未部署（undeployed） | 403 |
| TC-075 | ❌ 凭证缺失（无 X-Sys-Token） | 401, messageZh="认证失败" |
| TC-076 | ❌ 凭证错误 | 401 |
| TC-077 | ❌ 超出限流阈值 | 429 + Retry-After Header |
| TC-078 | ❌ flow 不存在 | 404 |
| TC-079 | ✅ SYSTOKEN 认证 Header | X-Sys-Token Header |
| TC-080 | ✅ 同步返回 | 响应在超时时间内返回 |

---

## 四、L4 契约测试

### 4.1 通用响应格式断言

所有接口响应必须符合统一格式：

```json
{
  "code": "200",           // string
  "messageZh": "操作成功",  // string（非空）
  "messageEn": "Success",  // string（非空）
  "data": { ... },         // object 或 null
  "page": { ... }          // object 或 null（分页接口非 null）
}
```

### 4.2 字段级契约验证

| 规范项 | 验证方式 | 涉及接口 |
|--------|----------|---------|
| BIGINT ID → string | JSON 类型断言（`instanceof String`） | 所有含 ID 的响应 |
| 枚举 → TINYINT 数字 | JSON 类型断言 + 值范围校验 | status/connectorType/nodeType/lifecycleStatus 等 |
| 时间 → ISO 8601 | 正则 `^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}[+-]\d{2}:\d{2}$` | createTime/lastUpdateTime |
| camelCase 字段名 | 字段名正则 `^[a-z]+[A-Za-z0-9]*$` | 全部字段 |
| `is*` 布尔前缀 | 布尔字段前缀校验 | isTest/isDeleted |
| 分页结构 | page.curPage/pageSize/total 均为 number | #2, #9 |

### 4.3 错误码覆盖率验证

| 错误码 | 验证接口 | 触发方式 |
|:------:|---------|---------|
| 400 | #1~#16 | 传非法参数 |
| 401 | #18 | 缺 X-Sys-Token |
| 403 | #1~#17（权限）；#18（流状态）| 非管理员 / 流未运行 |
| 404 | #3~#7, #10~#18 | 不存在的 ID |
| 409 | #5, #12~#14 | 状态冲突 / 引用中 |
| 422 | #1, #7, #16, #17 | 校验失败 |
| 429 | #18 | 超限流阈值 |
| 500 | 全部 | 内部异常 |

---

## 五、新增测试文件清单

| # | 文件路径 | 类型 | 覆盖接口 | 预计用例数 |
|---|---------|------|---------|:----------:|
| 1 | `open-server/src/test/.../connector/controller/ConnectorControllerWebMvcTest.java` | `@WebMvcTest` + MockMvc | #1~#7 | ~32 |
| 2 | `open-server/src/test/.../flow/controller/FlowControllerWebMvcTest.java` | `@WebMvcTest` + MockMvc | #8~#16 | ~32 |
| 3 | `open-server/src/test/.../debug/DebugProxyControllerWebMvcTest.java` | `@WebMvcTest` + MockMvc | #17 | ~7 |
| 4 | `connector-api/src/test/.../trigger/TriggerControllerWebFluxTest.java` | `@WebFluxTest` + WebTestClient | #18 | ~9 |
| 5 | `connector-api/src/test/.../integration/ContractTest.java` | JSON Schema + 响应格式断言 | 全部 | ~18 |
| 6 | `connector-api/src/test/.../integration/ConnectorFlowE2ETest.java` | `@SpringBootTest`（补充用例） | 全链路 | ~5（补充）|
| **合计** | **6 个新增文件** | | | **~103 个用例** |

---

## 六、与现有测试的覆盖边界

| 维度 | 现有测试（79 个） | 新增测试（~103 个） |
|------|-------------------|--------------------|
| 层次 | L1 单元测试 | L2 接口层 + L4 契约测试 |
| 模拟方式 | Mockito mock 全部依赖 | `@WebMvcTest` mock Service |
| 验证重点 | 业务逻辑正确性 | HTTP 请求绑定 + 响应格式 + 状态码 |
| 执行速度 | 毫秒级 | 毫秒级（仅 Controller 层） |
| 发现的问题类型 | 逻辑 Bug | 序列化失真、校验遗漏、字段命名错误 |

---

## 七、执行计划

| 阶段 | 内容 | 用例数 | 预计工时 |
|------|------|:------:|:--------:|
| Phase 1 | ConnectorController 接口层测试（WebMvcTest） | ~32 | 1 天 |
| Phase 2 | FlowController 接口层测试（WebMvcTest） | ~32 | 1 天 |
| Phase 3 | DebugProxy + TriggerController 测试 | ~16 | 0.5 天 |
| Phase 4 | 契约测试 + E2E 补充 | ~23 | 0.5 天 |
| Phase 5 | 统一执行 + 缺陷修复 + 覆盖率报告 | — | 0.5 天 |
| **合计** | | **~103** | **3.5 天** |

---

## 八、风险与假设

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| MockMvc 无法覆盖 R2DBC reactive 链路 | L3 集成测试不足 | 使用 `@SpringBootTest` 补充 E2E |
| 接口契约与实现已有偏差 | 测试发现大量字段问题 | 先运行契约快照，对比 plan-api.md |
| 雪花 ID 序列化为 string 的配置遗漏 | JS 前端精度丢失 | 契约测试中验证 ID 字段 JSON 类型 |
| 枚举值在数据库中变更但 API 未同步 | 前后端枚举不同步 | 契约测试中添加枚举值范围校验 |

---

*测试方案状态*: ✅ 完成  
*下一步*: 按 Phase 1~5 顺序生成实际测试代码并执行
