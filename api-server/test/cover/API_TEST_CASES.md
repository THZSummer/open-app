# api-server 接口测试用例

## 1. 测试范围

本文档根据 `api-server` 当前项目代码整理接口测试用例，覆盖以下入口：

- `ScopeController`: 用户授权管理接口
- `ApiGatewayController`: API 网关代理、回调配置查询接口
- `DataQueryController`: 权限和订阅查询接口
- `HealthController`: 服务健康检查接口
- Spring Boot Actuator 与 SpringDoc OpenAPI 暴露接口

基础地址：

```text
http://localhost:18081/api-server
```

## 2. 通用前置条件

- 服务使用 `dev` profile 启动。
- MySQL 可访问，且包含项目 Mapper 使用的三张核心表。
- 测试数据覆盖权限、订阅、用户授权三类数据。
- 普通业务接口返回结构通常为：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {},
  "page": {}
}
```

核心表：

| 表名 | 用途 |
| --- | --- |
| `openplatform_v2_permission_t` | 权限和 Scope 定义 |
| `openplatform_v2_subscription_t` | 应用订阅权限关系 |
| `openplatform_v2_user_authorization_t` | 用户对应用的 Scope 授权 |

## 3. 建议测试数据

```sql
INSERT INTO openplatform_v2_permission_t
(id, name_cn, name_en, scope, resource_type, resource_id, category_id, status)
VALUES
(1000, '发送消息权限', 'Send Message Permission', 'api:im:send-message', 'api', 100, 2, 1),
(1001, '获取消息权限', 'Get Message Permission', 'api:im:get-message', 'api', 101, 2, 1),
(2000, '审批完成回调', 'Approval Completed Callback', 'callback:approval:completed', 'callback', 200, 3, 1);

INSERT INTO openplatform_v2_subscription_t
(id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time, last_update_time)
VALUES
(300, 10, 1000, 1, 1, 'https://example.com/messages', 3, NOW(3), NOW(3)),
(301, 10, 1001, 0, 1, 'https://example.com/messages', 3, NOW(3), NOW(3)),
(302, 10, 2000, 1, 1, 'https://webhook.example.com/callbacks', 1, NOW(3), NOW(3));

INSERT INTO openplatform_v2_user_authorization_t
(id, user_id, app_id, scopes, expires_at, create_time, last_update_time, create_by, last_update_by)
VALUES
(600, 'user001', 10, '["api:im:send-message","api:im:get-message"]', '2026-12-31 23:59:59', NOW(3), NOW(3), 'test', 'test'),
(601, 'user002', 10, '["api:im:send-message"]', NULL, NOW(3), NOW(3), 'test', 'test');
```

## 4. 健康检查接口

### HEALTH-001 查询服务健康状态

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/v1/health` |
| 请求参数 | 无 |
| 预期结果 | HTTP 200，`code=200`，`data.status=UP`，`data.service=api-server` |

### HEALTH-002 不支持的请求方法

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/v1/health` |
| 请求参数 | 无 |
| 预期结果 | HTTP 405 |

## 5. 用户授权列表接口

接口：`GET /api/v1/user-authorizations`

| 用例编号 | 场景 | 请求参数 | 预期结果 |
| --- | --- | --- | --- |
| AUTH-LIST-001 | 查询默认第一页 | 无 | HTTP 200，`code=200`，默认 `curPage=1`、`pageSize=20` |
| AUTH-LIST-002 | 按用户过滤 | `userId=user001` | HTTP 200，仅返回该用户未撤销授权 |
| AUTH-LIST-003 | 按应用过滤 | `appId=10` | HTTP 200，仅返回该应用授权 |
| AUTH-LIST-004 | 按关键词查询 | `keyword=user` | HTTP 200，按 `user_id LIKE` 查询 |
| AUTH-LIST-005 | 分页查询 | `curPage=2&pageSize=10` | HTTP 200，分页信息正确 |
| AUTH-LIST-006 | 应用 ID 格式错误 | `appId=abc` | HTTP 200，响应体 `code=400` |
| AUTH-LIST-007 | 用户不存在 | `userId=not_exist` | HTTP 200，`data=[]`，`page.total=0` |
| AUTH-LIST-008 | 异常分页参数 | `curPage=-1&pageSize=20` | 当前代码无参数校验，可能产生 SQL 分页异常，记录为风险 |

示例请求：

```bash
curl -X GET "http://localhost:18081/api-server/api/v1/user-authorizations?userId=user001&curPage=1&pageSize=20"
```

## 6. 创建用户授权接口

接口：`POST /api/v1/user-authorizations`

| 用例编号 | 场景 | 请求体 | 预期结果 |
| --- | --- | --- | --- |
| AUTH-CREATE-001 | 创建带过期时间的授权 | `userId`、`appId`、`scopes`、`expiresAt` 均合法 | HTTP 200，`code=200`，返回授权 ID |
| AUTH-CREATE-002 | 创建永久授权 | 不传 `expiresAt` | HTTP 200，`expiresAt=null` |
| AUTH-CREATE-003 | 缺少用户 ID | 缺少 `userId` | HTTP 400，参数校验失败 |
| AUTH-CREATE-004 | 缺少应用 ID | 缺少 `appId` | HTTP 400，参数校验失败 |
| AUTH-CREATE-005 | Scope 为空 | `scopes=[]` | HTTP 400，参数校验失败 |
| AUTH-CREATE-006 | 应用 ID 格式错误 | `appId=abc` | HTTP 200，响应体 `code=400` |
| AUTH-CREATE-007 | 重复授权 | 相同 `userId+appId` 已存在未撤销记录 | HTTP 200，响应体 `code=400` |
| AUTH-CREATE-008 | 日期格式错误 | `expiresAt=bad-date` | HTTP 400，请求体解析失败 |
| AUTH-CREATE-009 | 用户 ID 为空字符串 | `userId=""` | 当前代码 `@NotNull` 不拦截空字符串，可能创建记录，记录为风险 |

示例请求：

```bash
curl -X POST "http://localhost:18081/api-server/api/v1/user-authorizations" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user001",
    "appId": "10",
    "scopes": ["api:im:send-message"],
    "expiresAt": "2026-12-31T23:59:59"
  }'
```

## 7. 撤销用户授权接口

接口：`DELETE /api/v1/user-authorizations/{id}`

| 用例编号 | 场景 | 请求 | 预期结果 |
| --- | --- | --- | --- |
| AUTH-REVOKE-001 | 撤销成功 | `id=600` 且存在未撤销记录 | HTTP 200，`code=200` |
| AUTH-REVOKE-002 | 授权不存在 | `id=999999` | HTTP 200，响应体 `code=404` |
| AUTH-REVOKE-003 | 授权 ID 格式错误 | `id=abc` | HTTP 200，响应体 `code=400` |
| AUTH-REVOKE-004 | 重复撤销 | 已撤销的 `id` | HTTP 200，响应体 `code=400` |
| AUTH-REVOKE-005 | 更新失败 | 记录存在但更新影响行数为 0 | HTTP 200，响应体 `code=500` |

示例请求：

```bash
curl -X DELETE "http://localhost:18081/api-server/api/v1/user-authorizations/600"
```

## 8. API 网关代理接口

接口：`GET|POST|PUT|DELETE /gateway/api/**`

| 用例编号 | 场景 | 请求 | 预期结果 |
| --- | --- | --- | --- |
| GATEWAY-PROXY-001 | GET 代理成功 | Header 包含 `X-App-Id=10`、`X-Auth-Type=3`、`Authorization=Bearer token` | HTTP 200，返回 Mock 成功 JSON |
| GATEWAY-PROXY-002 | POST 代理成功 | Header 合法，Body 合法 | HTTP 200 或 403，取决于生成 Scope 是否有订阅 |
| GATEWAY-PROXY-003 | 缺少应用 ID | 缺少 `X-App-Id` | HTTP 401，`code=401` |
| GATEWAY-PROXY-004 | 缺少认证凭证 | 缺少 `Authorization` | HTTP 401，`code=401` |
| GATEWAY-PROXY-005 | 未订阅权限 | 应用认证通过，但订阅不存在 | HTTP 403，`code=403` |
| GATEWAY-PROXY-006 | 应用 ID 格式错误 | `X-App-Id=abc` 且认证通过 | HTTP 403，原因包含应用 ID 格式错误 |
| GATEWAY-PROXY-007 | PUT 请求 | `PUT /gateway/api/v1/messages/1` | 按权限校验结果返回 HTTP 200 或 403 |
| GATEWAY-PROXY-008 | DELETE 请求 | `DELETE /gateway/api/v1/messages/1` | 按权限校验结果返回 HTTP 200 或 403 |
| GATEWAY-PROXY-009 | 不支持 PATCH | `PATCH /gateway/api/v1/messages/1` | HTTP 405 |
| GATEWAY-PROXY-010 | 服务内部异常 | 服务层抛异常 | HTTP 500，`code=500` |

示例请求：

```bash
curl -X POST "http://localhost:18081/api-server/gateway/api/v1/messages" \
  -H "X-App-Id: 10" \
  -H "X-Auth-Type: 3" \
  -H "Authorization: Bearer token" \
  -H "Content-Type: application/json" \
  -d '{"content":"hello"}'
```

## 9. 回调配置查询接口

接口：`POST /gateway/callbacks/config`

| 用例编号 | 场景 | 请求体 | 预期结果 |
| --- | --- | --- | --- |
| CALLBACK-001 | 查询成功 | `{"ak":"AK123456789","scope":"callback:approval:completed"}` | HTTP 200，`code=200`，返回 channel 配置或 `data=null` |
| CALLBACK-002 | AK 不存在 | `ak=AK_NOT_EXIST` | HTTP 200，`code=200`，`data=null` |
| CALLBACK-003 | 缺少 AK | 缺少 `ak` | HTTP 400，参数校验失败 |
| CALLBACK-004 | AK 为空字符串 | `ak=""` | HTTP 400，参数校验失败 |
| CALLBACK-005 | 缺少 Scope | 缺少 `scope` | HTTP 400，参数校验失败 |
| CALLBACK-006 | Scope 为空字符串 | `scope=""` | HTTP 400，参数校验失败 |
| CALLBACK-007 | 服务内部异常 | 服务层抛异常 | HTTP 200，响应体 `code=400` |
| CALLBACK-008 | 缺少内部认证 Header | 不传 `Authorization` | 当前代码未校验该 Header，仍按正常流程执行，记录为安全风险 |

示例请求：

```bash
curl -X POST "http://localhost:18081/api-server/gateway/callbacks/config" \
  -H "Content-Type: application/json" \
  -d '{
    "ak": "AK123456789",
    "scope": "callback:approval:completed"
  }'
```

## 10. 权限校验接口

接口：`GET /gateway/permissions/check`

| 用例编号 | 场景 | 请求参数 | 预期结果 |
| --- | --- | --- | --- |
| PERM-CHECK-001 | 已授权 | `appId=10&scope=api:im:send-message` | HTTP 200，`code=200`，`data.authorized=true` |
| PERM-CHECK-002 | 权限不存在 | `appId=10&scope=api:not-exist` | HTTP 200，`authorized=false` |
| PERM-CHECK-003 | 应用 ID 格式错误 | `appId=abc&scope=api:im:send-message` | HTTP 200，`authorized=false` |
| PERM-CHECK-004 | 缺少应用 ID | 缺少 `appId` | HTTP 400 |
| PERM-CHECK-005 | 缺少 Scope | 缺少 `scope` | HTTP 400 |
| PERM-CHECK-006 | 订阅状态异常 | 订阅状态为 0、2 或 3 | HTTP 200，`authorized=false` |
| PERM-CHECK-007 | 服务内部异常 | 服务层抛异常 | HTTP 500 |

示例请求：

```bash
curl -X GET "http://localhost:18081/api-server/gateway/permissions/check?appId=10&scope=api:im:send-message"
```

## 11. 查询订阅应用列表接口

接口：`GET /gateway/permissions/subscribers`

| 用例编号 | 场景 | 请求参数 | 预期结果 |
| --- | --- | --- | --- |
| SUBSCRIBERS-001 | 查询成功 | `scope=api:im:send-message` | HTTP 200，`data` 为订阅该权限的 appId 列表 |
| SUBSCRIBERS-002 | 权限不存在 | `scope=api:not-exist` | HTTP 200，`data=[]` |
| SUBSCRIBERS-003 | 缺少 Scope | 缺少 `scope` | HTTP 400 |
| SUBSCRIBERS-004 | 服务内部异常 | 服务层抛异常 | HTTP 500 |

示例请求：

```bash
curl -X GET "http://localhost:18081/api-server/gateway/permissions/subscribers?scope=api:im:send-message"
```

## 12. 查询订阅配置接口

接口：`GET /gateway/subscriptions/config`

| 用例编号 | 场景 | 请求参数 | 预期结果 |
| --- | --- | --- | --- |
| SUB-CONFIG-001 | 查询成功 | `appId=10&scope=api:im:send-message` | HTTP 200，返回订阅配置 |
| SUB-CONFIG-002 | 应用 ID 格式错误 | `appId=abc&scope=api:im:send-message` | HTTP 200，`data={}` |
| SUB-CONFIG-003 | 权限不存在 | `scope=api:not-exist` | HTTP 200，`data={}` |
| SUB-CONFIG-004 | 订阅不存在 | 合法 `appId` 和未订阅 `scope` | HTTP 200，`data={}` |
| SUB-CONFIG-005 | 缺少应用 ID | 缺少 `appId` | HTTP 400 |
| SUB-CONFIG-006 | 缺少 Scope | 缺少 `scope` | HTTP 400 |
| SUB-CONFIG-007 | 服务内部异常 | 服务层抛异常 | HTTP 500 |

示例请求：

```bash
curl -X GET "http://localhost:18081/api-server/gateway/subscriptions/config?appId=10&scope=api:im:send-message"
```

## 13. 查询权限详情接口

接口：`GET /gateway/permissions/detail`

| 用例编号 | 场景 | 请求参数 | 预期结果 |
| --- | --- | --- | --- |
| PERM-DETAIL-001 | 查询成功 | `scope=api:im:send-message` | HTTP 200，`code=200`，返回权限详情 |
| PERM-DETAIL-002 | 权限不存在 | `scope=api:not-exist` | HTTP 200，响应体 `code=404` |
| PERM-DETAIL-003 | 缺少 Scope | 缺少 `scope` | HTTP 400 |
| PERM-DETAIL-004 | 服务内部异常 | 服务层抛异常 | HTTP 500 |

示例请求：

```bash
curl -X GET "http://localhost:18081/api-server/gateway/permissions/detail?scope=api:im:send-message"
```

## 14. Actuator 和 OpenAPI 接口

| 用例编号 | 接口 | 场景 | 预期结果 |
| --- | --- | --- | --- |
| ACTUATOR-001 | `GET /actuator/health` | 查询健康状态 | HTTP 200 |
| ACTUATOR-002 | `GET /actuator/info` | 查询服务信息 | HTTP 200 |
| ACTUATOR-003 | `GET /actuator/metrics` | 查询指标列表 | HTTP 200 |
| OPENAPI-001 | `GET /api-docs` | 查询 OpenAPI JSON | HTTP 200 |
| OPENAPI-002 | `GET /swagger-ui.html` | 打开 Swagger UI | HTTP 200 或重定向 |

## 15. 当前代码行为风险用例

| 编号 | 风险点 | 说明 |
| --- | --- | --- |
| RISK-001 | 应用认证为 Mock | `verifyApplication` 只校验 `appId` 和凭证非空，没有真实 AKSK 或 Bearer Token 验证 |
| RISK-002 | 网关转发为 Mock | 鉴权通过后没有真实转发内部网关，只返回模拟 JSON |
| RISK-003 | Scope 映射为路径拼接 | `/gateway/api/**` 的 Scope 由路径和方法生成，不查 API 资源表 |
| RISK-004 | 用户授权 ID 为进程内递增 | 多实例或重启后存在 ID 冲突风险 |
| RISK-005 | 部分参数校验不足 | `userId` 和 `appId` 使用 `@NotNull`，空字符串不会被拦截 |
| RISK-006 | 回调配置内部认证未实现 | `Authorization` Header 被接收但没有校验 |
| RISK-007 | 业务异常 HTTP 状态不统一 | 多数业务异常返回 HTTP 200，仅通过响应体 `code` 表达失败 |

## 16. 回归验证建议

- 单元测试：执行 `mvn test`。
- 接口测试：先初始化测试数据，再按本文档接口顺序执行。
- 网关相关用例需要确保生成的 Scope 与数据库中权限数据一致。
- 如果要验证真实生产行为，需要先替换 Mock 应用认证和 Mock 网关转发逻辑。
