# api-server 接口测试结果

测试时间：2026-04-28 19:44:01

基础地址：`http://localhost:18081/api-server`

临时测试用户：`codex_test_20260428194359`

创建后撤销的授权 ID：`1777376558971`

清理说明：本轮测试额外创建的临时授权 `1777376558972`、`1777376558973` 已在测试后通过撤销接口清理。

## 1. 结果概览

| 结论 | 数量 |
| --- | ---: |
| 待确认 | 3 |
| 失败 | 16 |
| 通过 | 43 |
| 未执行 | 7 |

## 2. 失败用例

| 用例编号 | 用例名称 | 请求 | 预期结果 | 实际结果 |
| --- | --- | --- | --- | --- |
| HEALTH-002 | 健康接口不支持 POST | `POST /api/v1/health` | HTTP 405 | HTTP 500, code=500 |
| AUTH-CREATE-008 | 日期格式错误 | `POST /api/v1/user-authorizations` | HTTP 400，请求体解析失败 | HTTP 500, code=500 |
| GATEWAY-PROXY-001 | GET 代理 | `GET /gateway/api/v1/messages` | HTTP 200，返回 Mock 成功 JSON；若 Scope 与库不匹配则可能 403 | HTTP 403 |
| GATEWAY-PROXY-009 | 不支持 PATCH | `PATCH /gateway/api/v1/messages/1` | HTTP 405 | HTTP 500, code=500 |
| PERM-CHECK-001 | 已授权 | `GET /gateway/permissions/check?appId=10&scope=api:im:send-message` | HTTP 200，code=200，authorized=true | HTTP 200, code=200, authorized=False, data=object |
| PERM-CHECK-004 | 缺少应用 ID | `GET /gateway/permissions/check?scope=api:im:send-message` | HTTP 400 | HTTP 500, code=500 |
| PERM-CHECK-005 | 缺少 Scope | `GET /gateway/permissions/check?appId=10` | HTTP 400 | HTTP 500, code=500 |
| SUBSCRIBERS-003 | 缺少 Scope | `GET /gateway/permissions/subscribers` | HTTP 400 | HTTP 500, code=500 |
| SUB-CONFIG-002 | 应用 ID 格式错误 | `GET /gateway/subscriptions/config?appId=abc&scope=api:im:send-message` | HTTP 200，data={} | HTTP 200, code=200, data=object |
| SUB-CONFIG-003 | 权限不存在 | `GET /gateway/subscriptions/config?appId=10&scope=api:not-exist` | HTTP 200，data={} | HTTP 200, code=200, data=object |
| SUB-CONFIG-004 | 订阅不存在 | `GET /gateway/subscriptions/config?appId=999999&scope=api:im:send-message` | HTTP 200，data={} | HTTP 200, code=200, data=object |
| SUB-CONFIG-005 | 缺少应用 ID | `GET /gateway/subscriptions/config?scope=api:im:send-message` | HTTP 400 | HTTP 500, code=500 |
| SUB-CONFIG-006 | 缺少 Scope | `GET /gateway/subscriptions/config?appId=10` | HTTP 400 | HTTP 500, code=500 |
| PERM-DETAIL-001 | 查询权限详情 | `GET /gateway/permissions/detail?scope=api:im:send-message` | HTTP 200，code=200，返回权限详情 | HTTP 200, code=404 |
| PERM-DETAIL-003 | 缺少 Scope | `GET /gateway/permissions/detail` | HTTP 400 | HTTP 500, code=500 |
| OPENAPI-001 | OpenAPI JSON | `GET /api-docs` | HTTP 200 | HTTP 500, code=500 |

## 3. 待确认或未执行用例

| 用例编号 | 用例名称 | 请求 | 预期结果 | 实际结果 | 结论 |
| --- | --- | --- | --- | --- | --- |
| AUTH-LIST-008 | 异常分页参数 | `GET /api/v1/user-authorizations?curPage=-1&pageSize=20` | 风险用例，当前代码无参数校验，记录实际结果 | HTTP 500, code=500 | 待确认 |
| AUTH-CREATE-009 | 用户 ID 为空字符串 | `POST /api/v1/user-authorizations` | 风险用例，当前 @NotNull 不拦截空字符串，记录实际结果 | HTTP 200, code=200, data=object | 待确认 |
| AUTH-REVOKE-005 | 更新失败 | `DELETE /api/v1/user-authorizations/{id}` | HTTP 200，响应体 code=500 | HTTP -1, 该场景需要并发修改或 Mock Mapper，真实 HTTP 不稳定复现，未执行 | 未执行 |
| GATEWAY-PROXY-010 | 服务内部异常 | `GET /gateway/api/**` | HTTP 500，code=500 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |
| CALLBACK-007 | 服务内部异常 | `POST /gateway/callbacks/config` | HTTP 200，响应体 code=400 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |
| CALLBACK-008 | 缺少内部认证 Header | `POST /gateway/callbacks/config` | 风险用例，当前未校验 Authorization，仍按正常流程执行 | HTTP 200, code=200 | 待确认 |
| PERM-CHECK-007 | 服务内部异常 | `GET /gateway/permissions/check` | HTTP 500 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |
| SUBSCRIBERS-004 | 服务内部异常 | `GET /gateway/permissions/subscribers` | HTTP 500 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |
| SUB-CONFIG-007 | 服务内部异常 | `GET /gateway/subscriptions/config` | HTTP 500 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |
| PERM-DETAIL-004 | 服务内部异常 | `GET /gateway/permissions/detail` | HTTP 500 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |

## 4. 全量执行明细

| 用例编号 | 用例名称 | 请求 | 请求参数 | 预期结果 | 实际结果 | 结论 |
| --- | --- | --- | --- | --- | --- | --- |
| HEALTH-001 | 查询服务健康状态 | `GET /api/v1/health` |  | HTTP 200，code=200，data.status=UP | HTTP 200, code=200, data.status=UP | 通过 |
| HEALTH-002 | 健康接口不支持 POST | `POST /api/v1/health` |  | HTTP 405 | HTTP 500, code=500 | 失败 |
| AUTH-LIST-001 | 查询默认第一页授权 | `GET /api/v1/user-authorizations` |  | HTTP 200，code=200 | HTTP 200, code=200, dataCount=2 | 通过 |
| AUTH-LIST-002 | 按用户过滤 | `GET /api/v1/user-authorizations?userId=user001` |  | HTTP 200，返回该用户授权或空列表 | HTTP 200, code=200, dataCount=1 | 通过 |
| AUTH-LIST-003 | 按应用过滤 | `GET /api/v1/user-authorizations?appId=10` |  | HTTP 200，返回该应用授权或空列表 | HTTP 200, code=200, dataCount=2 | 通过 |
| AUTH-LIST-004 | 按关键词查询 | `GET /api/v1/user-authorizations?keyword=user` |  | HTTP 200，按 user_id LIKE 查询 | HTTP 200, code=200, dataCount=2 | 通过 |
| AUTH-LIST-005 | 分页查询 | `GET /api/v1/user-authorizations?curPage=2&pageSize=10` |  | HTTP 200，分页信息正确 | HTTP 200, code=200, dataCount=0 | 通过 |
| AUTH-LIST-006 | 应用 ID 格式错误 | `GET /api/v1/user-authorizations?appId=abc` |  | HTTP 200，响应体 code=400 | HTTP 200, code=400 | 通过 |
| AUTH-LIST-007 | 用户不存在 | `GET /api/v1/user-authorizations?userId=not_exist` |  | HTTP 200，data=[] | HTTP 200, code=200, dataCount=0 | 通过 |
| AUTH-LIST-008 | 异常分页参数 | `GET /api/v1/user-authorizations?curPage=-1&pageSize=20` |  | 风险用例，当前代码无参数校验，记录实际结果 | HTTP 500, code=500 | 待确认 |
| AUTH-CREATE-001 | 创建带过期时间的授权 | `POST /api/v1/user-authorizations` | {"appId":"10","scopes":["api:im:send-message"],"userId":"codex_test_20260428194359","expiresAt":"2026-12-31T23:59:59"} | HTTP 200，code=200，返回授权 ID | HTTP 200, code=200, data=object | 通过 |
| AUTH-CREATE-002 | 创建永久授权 | `POST /api/v1/user-authorizations` | {"appId":"10","scopes":["api:im:send-message"],"userId":"codex_test_20260428194359_forever"} | HTTP 200 或 code=400；如果相同 userId+appId 已创建则重复授权 | HTTP 200, code=200, data=object | 通过 |
| AUTH-CREATE-003 | 缺少用户 ID | `POST /api/v1/user-authorizations` | {"appId":"10","scopes":["api:im:send-message"]} | HTTP 400，参数校验失败 | HTTP 400, code=400 | 通过 |
| AUTH-CREATE-004 | 缺少应用 ID | `POST /api/v1/user-authorizations` | {"userId":"codex_test_20260428194359","scopes":["api:im:send-message"]} | HTTP 400，参数校验失败 | HTTP 400, code=400 | 通过 |
| AUTH-CREATE-005 | Scope 为空 | `POST /api/v1/user-authorizations` | {"appId":"10","scopes":[],"userId":"codex_test_20260428194359"} | HTTP 400，参数校验失败 | HTTP 400, code=400 | 通过 |
| AUTH-CREATE-006 | 应用 ID 格式错误 | `POST /api/v1/user-authorizations` | {"appId":"abc","scopes":["api:im:send-message"],"userId":"codex_test_20260428194359_badapp"} | HTTP 200，响应体 code=400 | HTTP 200, code=400 | 通过 |
| AUTH-CREATE-007 | 重复授权 | `POST /api/v1/user-authorizations` | {"appId":"10","scopes":["api:im:send-message"],"userId":"codex_test_20260428194359","expiresAt":"2026-12-31T23:59:59"} | HTTP 200，响应体 code=400 | HTTP 200, code=400 | 通过 |
| AUTH-CREATE-008 | 日期格式错误 | `POST /api/v1/user-authorizations` | {"userId":"bad_date","appId":"10","scopes":["api:im:send-message"],"expiresAt":"bad-date"} | HTTP 400，请求体解析失败 | HTTP 500, code=500 | 失败 |
| AUTH-CREATE-009 | 用户 ID 为空字符串 | `POST /api/v1/user-authorizations` | {"appId":"10","scopes":["api:im:send-message"],"userId":""} | 风险用例，当前 @NotNull 不拦截空字符串，记录实际结果 | HTTP 200, code=200, data=object | 待确认 |
| AUTH-REVOKE-001 | 撤销成功 | `DELETE /api/v1/user-authorizations/1777376558971` |  | HTTP 200，code=200 | HTTP 200, code=200 | 通过 |
| AUTH-REVOKE-002 | 授权不存在 | `DELETE /api/v1/user-authorizations/999999999999` |  | HTTP 200，响应体 code=404 | HTTP 200, code=404 | 通过 |
| AUTH-REVOKE-003 | 授权 ID 格式错误 | `DELETE /api/v1/user-authorizations/abc` |  | HTTP 200，响应体 code=400 | HTTP 200, code=400 | 通过 |
| AUTH-REVOKE-004 | 重复撤销 | `DELETE /api/v1/user-authorizations/1777376558971` |  | HTTP 200，响应体 code=400 | HTTP 200, code=400 | 通过 |
| AUTH-REVOKE-005 | 更新失败 | `DELETE /api/v1/user-authorizations/{id}` |  | HTTP 200，响应体 code=500 | HTTP -1, 该场景需要并发修改或 Mock Mapper，真实 HTTP 不稳定复现，未执行 | 未执行 |
| GATEWAY-PROXY-001 | GET 代理 | `GET /gateway/api/v1/messages` |  | HTTP 200，返回 Mock 成功 JSON；若 Scope 与库不匹配则可能 403 | HTTP 403 | 失败 |
| GATEWAY-PROXY-002 | POST 代理 | `POST /gateway/api/v1/messages` | {"content":"hello"} | HTTP 200 或 403，取决于生成 Scope 是否有订阅 | HTTP 403 | 通过 |
| GATEWAY-PROXY-003 | 缺少应用 ID | `GET /gateway/api/v1/messages` |  | HTTP 401，code=401 | HTTP 401 | 通过 |
| GATEWAY-PROXY-004 | 缺少认证凭证 | `GET /gateway/api/v1/messages` |  | HTTP 401，code=401 | HTTP 401 | 通过 |
| GATEWAY-PROXY-005 | 未订阅权限 | `GET /gateway/api/not-subscribed` |  | HTTP 403，code=403 | HTTP 403 | 通过 |
| GATEWAY-PROXY-006 | 应用 ID 格式错误 | `GET /gateway/api/v1/messages` |  | HTTP 403，原因包含应用 ID 格式错误 | HTTP 403 | 通过 |
| GATEWAY-PROXY-007 | PUT 请求 | `PUT /gateway/api/v1/messages/1` | {"content":"hello"} | HTTP 200 或 403 | HTTP 403 | 通过 |
| GATEWAY-PROXY-008 | DELETE 请求 | `DELETE /gateway/api/v1/messages/1` |  | HTTP 200 或 403 | HTTP 403 | 通过 |
| GATEWAY-PROXY-009 | 不支持 PATCH | `PATCH /gateway/api/v1/messages/1` |  | HTTP 405 | HTTP 500, code=500 | 失败 |
| GATEWAY-PROXY-010 | 服务内部异常 | `GET /gateway/api/**` |  | HTTP 500，code=500 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |
| CALLBACK-001 | 查询回调配置 | `POST /gateway/callbacks/config` | {"scope":"callback:approval:completed","ak":"AK123456789"} | HTTP 200，code=200，返回配置或 data=null | HTTP 200, code=200 | 通过 |
| CALLBACK-002 | AK 不存在 | `POST /gateway/callbacks/config` | {"scope":"callback:approval:completed","ak":"AK_NOT_EXIST"} | HTTP 200，code=200，data=null | HTTP 200, code=200 | 通过 |
| CALLBACK-003 | 缺少 AK | `POST /gateway/callbacks/config` | {"scope":"callback:approval:completed"} | HTTP 400，参数校验失败 | HTTP 400, code=400 | 通过 |
| CALLBACK-004 | AK 为空字符串 | `POST /gateway/callbacks/config` | {"scope":"callback:approval:completed","ak":""} | HTTP 400，参数校验失败 | HTTP 400, code=400 | 通过 |
| CALLBACK-005 | 缺少 Scope | `POST /gateway/callbacks/config` | {"ak":"AK123456789"} | HTTP 400，参数校验失败 | HTTP 400, code=400 | 通过 |
| CALLBACK-006 | Scope 为空字符串 | `POST /gateway/callbacks/config` | {"scope":"","ak":"AK123456789"} | HTTP 400，参数校验失败 | HTTP 400, code=400 | 通过 |
| CALLBACK-007 | 服务内部异常 | `POST /gateway/callbacks/config` |  | HTTP 200，响应体 code=400 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |
| CALLBACK-008 | 缺少内部认证 Header | `POST /gateway/callbacks/config` | {"scope":"callback:approval:completed","ak":"AK123456789"} | 风险用例，当前未校验 Authorization，仍按正常流程执行 | HTTP 200, code=200 | 待确认 |
| PERM-CHECK-001 | 已授权 | `GET /gateway/permissions/check?appId=10&scope=api:im:send-message` |  | HTTP 200，code=200，authorized=true | HTTP 200, code=200, authorized=False, data=object | 失败 |
| PERM-CHECK-002 | 权限不存在 | `GET /gateway/permissions/check?appId=10&scope=api:not-exist` |  | HTTP 200，authorized=false | HTTP 200, code=200, authorized=False, data=object | 通过 |
| PERM-CHECK-003 | 应用 ID 格式错误 | `GET /gateway/permissions/check?appId=abc&scope=api:im:send-message` |  | HTTP 200，authorized=false | HTTP 200, code=200, authorized=False, data=object | 通过 |
| PERM-CHECK-004 | 缺少应用 ID | `GET /gateway/permissions/check?scope=api:im:send-message` |  | HTTP 400 | HTTP 500, code=500 | 失败 |
| PERM-CHECK-005 | 缺少 Scope | `GET /gateway/permissions/check?appId=10` |  | HTTP 400 | HTTP 500, code=500 | 失败 |
| PERM-CHECK-006 | 订阅状态异常 | `GET /gateway/permissions/check?appId=10&scope=api:im:get-message` |  | HTTP 200，authorized=false | HTTP 200, code=200, authorized=False, data=object | 通过 |
| PERM-CHECK-007 | 服务内部异常 | `GET /gateway/permissions/check` |  | HTTP 500 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |
| SUBSCRIBERS-001 | 查询订阅应用列表 | `GET /gateway/permissions/subscribers?scope=api:im:send-message` |  | HTTP 200，data 为 appId 列表 | HTTP 200, code=200, dataCount=0 | 通过 |
| SUBSCRIBERS-002 | 权限不存在 | `GET /gateway/permissions/subscribers?scope=api:not-exist` |  | HTTP 200，data=[] | HTTP 200, code=200, dataCount=0 | 通过 |
| SUBSCRIBERS-003 | 缺少 Scope | `GET /gateway/permissions/subscribers` |  | HTTP 400 | HTTP 500, code=500 | 失败 |
| SUBSCRIBERS-004 | 服务内部异常 | `GET /gateway/permissions/subscribers` |  | HTTP 500 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |
| SUB-CONFIG-001 | 查询订阅配置 | `GET /gateway/subscriptions/config?appId=10&scope=api:im:send-message` |  | HTTP 200，返回订阅配置 | HTTP 200, code=200, data=object | 通过 |
| SUB-CONFIG-002 | 应用 ID 格式错误 | `GET /gateway/subscriptions/config?appId=abc&scope=api:im:send-message` |  | HTTP 200，data={} | HTTP 200, code=200, data=object | 失败 |
| SUB-CONFIG-003 | 权限不存在 | `GET /gateway/subscriptions/config?appId=10&scope=api:not-exist` |  | HTTP 200，data={} | HTTP 200, code=200, data=object | 失败 |
| SUB-CONFIG-004 | 订阅不存在 | `GET /gateway/subscriptions/config?appId=999999&scope=api:im:send-message` |  | HTTP 200，data={} | HTTP 200, code=200, data=object | 失败 |
| SUB-CONFIG-005 | 缺少应用 ID | `GET /gateway/subscriptions/config?scope=api:im:send-message` |  | HTTP 400 | HTTP 500, code=500 | 失败 |
| SUB-CONFIG-006 | 缺少 Scope | `GET /gateway/subscriptions/config?appId=10` |  | HTTP 400 | HTTP 500, code=500 | 失败 |
| SUB-CONFIG-007 | 服务内部异常 | `GET /gateway/subscriptions/config` |  | HTTP 500 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |
| PERM-DETAIL-001 | 查询权限详情 | `GET /gateway/permissions/detail?scope=api:im:send-message` |  | HTTP 200，code=200，返回权限详情 | HTTP 200, code=404 | 失败 |
| PERM-DETAIL-002 | 权限不存在 | `GET /gateway/permissions/detail?scope=api:not-exist` |  | HTTP 200，响应体 code=404 | HTTP 200, code=404 | 通过 |
| PERM-DETAIL-003 | 缺少 Scope | `GET /gateway/permissions/detail` |  | HTTP 400 | HTTP 500, code=500 | 失败 |
| PERM-DETAIL-004 | 服务内部异常 | `GET /gateway/permissions/detail` |  | HTTP 500 | HTTP -1, 该场景需 Mock 服务层抛异常，真实 HTTP 未执行 | 未执行 |
| ACTUATOR-001 | Actuator health | `GET /actuator/health` |  | HTTP 200 | HTTP 200 | 通过 |
| ACTUATOR-002 | Actuator info | `GET /actuator/info` |  | HTTP 200 | HTTP 200 | 通过 |
| ACTUATOR-003 | Actuator metrics | `GET /actuator/metrics` |  | HTTP 200 | HTTP 200 | 通过 |
| OPENAPI-001 | OpenAPI JSON | `GET /api-docs` |  | HTTP 200 | HTTP 500, code=500 | 失败 |
| OPENAPI-002 | Swagger UI | `GET /swagger-ui.html` |  | HTTP 200 或重定向 | HTTP 200 | 通过 |

## 5. 主要观察

- 多个缺少必填 `@RequestParam` 的接口实际返回 HTTP 500，而测试文档预期为 HTTP 400。
- `GET /api-docs` 实际返回 HTTP 500，日志显示 `springdoc-openapi` 与当前 Spring Framework 版本存在 `NoSuchMethodError` 兼容性问题。
- `POST /api/v1/health` 实际被全局异常处理包装为 HTTP 500，而不是框架默认 HTTP 405。
- `AUTH-CREATE-009` 验证了空字符串 `userId` 会被接受并创建记录。
- `PERM-CHECK-001` 预期已授权，但实际 `authorized=false`，说明当前数据库权限/订阅数据与用例预期不一致。
- 网关代理接口实际多为 HTTP 403，主要原因是真实运行时生成的 Scope 与当前数据库权限数据不匹配。

## 6. 原始结果

原始 JSON 结果保存在 `api-test-results.json`。

## 7. 代码覆盖率测试

覆盖率结果文档：`API_COVERAGE_RESULTS.md`

| 覆盖维度 | 覆盖率 |
| --- | ---: |
| 指令 Instruction | 87.19% |
| 分支 Branch | 78.00% |
| 行 Line | 86.23% |
| 方法 Method | 83.33% |

本次覆盖率测试执行 `91` 个测试，失败 `0`，错误 `0`，跳过 `0`。



