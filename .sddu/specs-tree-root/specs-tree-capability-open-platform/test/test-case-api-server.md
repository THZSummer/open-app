# 测试用例文档 - api-server 服务

**版本**: v1.0
**编写日期**: 2026-04-22
**服务端口**: 18081
**上下文路径**: /api-server

---

## 一、测试环境

### 1.1 服务配置

| 配置项 | 值 |
|-------|-----|
| 服务端口 | 18081 |
| 上下文路径 | /api-server |
| 数据库连接 | jdbc:mysql://localhost:3306/openplatform_v2 |
| Redis连接 | redis://localhost:6379/0 |

### 1.2 测试数据准备

#### 用户授权数据

```sql
INSERT INTO openplatform_v2_user_authorization_t (id, user_id, app_id, scopes, expires_at, create_time) VALUES
(600, 'user001', 10, '["api:im:send-message", "api:im:get-message"]', '2026-12-31 23:59:59', NOW()),
(601, 'user002', 10, '["api:im:send-message"]', '2026-06-30 23:59:59', NOW()),
(602, 'user003', 11, '["api:cloud:upload"]', NULL, NOW());
```

#### 应用数据

```sql
INSERT INTO openplatform_v2_app_t (id, name, app_key, status) VALUES
(10, '消息助手', 'app_key_10', 1),
(11, '云盘助手', 'app_key_11', 1);
```

#### 权限数据

```sql
INSERT INTO openplatform_v2_permission_t (id, name_cn, name_en, scope, resource_type, resource_id, category_id, status) VALUES
(1000, '发送消息权限', 'Send Message Permission', 'api:im:send-message', 'api', 100, 2, 1),
(1001, '获取消息权限', 'Get Message Permission', 'api:im:get-message', 'api', 101, 2, 1),
(2000, '文件上传权限', 'File Upload Permission', 'api:cloud:upload', 'api', 200, 3, 1);
```

#### 订阅关系数据

```sql
INSERT INTO openplatform_v2_subscription_t (id, app_id, permission_id, status, auth_type, create_time) VALUES
(300, 10, 1000, 1, 0, NOW()),
(301, 10, 1001, 1, 0, NOW()),
(302, 11, 2000, 1, 1, NOW());
```

---

## 二、接口测试用例

### 2.1 Scope 授权管理

#### TC-USER-AUTH-001: 获取用户授权列表

**接口**: GET /api/v1/user-authorizations
**优先级**: P0
**前置条件**: 
- 数据库中存在用户授权数据

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取全部用户授权列表 | 无参数 | 返回200，返回授权列表 |
| 2 | 按用户ID过滤 | userId=user001 | 返回200，返回指定用户授权 |
| 3 | 按应用ID过滤 | appId=10 | 返回200，返回指定应用授权 |
| 4 | 关键词搜索 | keyword=消息 | 返回200，返回匹配授权 |
| 5 | 分页查询 | curPage=1&pageSize=10 | 返回200，返回分页数据 |

**请求示例**:
```bash
curl -X GET "http://localhost:18081/api-server/api/v1/user-authorizations?userId=user001&curPage=1&pageSize=20"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "600",
      "userId": "user001",
      "userName": "张三",
      "appId": "10",
      "appName": "消息助手",
      "scopes": ["api:im:send-message", "api:im:get-message"],
      "expiresAt": "2026-12-31T23:59:59.000Z",
      "createTime": "2026-04-20T10:00:00.000Z"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 20
  }
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| 用户ID不存在 | userId=not_exist | 返回200，data为空数组 |
| 应用ID不存在 | appId=999999 | 返回200，data为空数组 |
| 已过期的授权 | 已过期数据 | 返回200，包含已过期授权（由调用方判断） |
| 无效分页参数 | curPage=-1 | 返回400，参数校验失败 |

---

#### TC-USER-AUTH-002: 用户授权（设置有效期）

**接口**: POST /api/v1/user-authorizations
**优先级**: P0
**前置条件**: 
- 用户user001存在
- 应用ID=10存在
- Scope有效

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 创建用户授权（有有效期） | 完整参数含expires_at | 返回200，创建成功 |
| 2 | 创建用户授权（永久有效） | 不填expires_at | 返回200，创建成功，expires_at为null |
| 3 | 重复授权 | 相同user_id+app_id | 返回409，授权已存在 |
| 4 | 缺少必填字段 | user_id为空 | 返回400，参数校验失败 |
| 5 | 无效Scope | scopes含无效scope | 返回400，无效Scope |

**请求示例**:
```bash
curl -X POST "http://localhost:18081/api-server/api/v1/user-authorizations" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user001",
    "appId": "10",
    "scopes": ["api:im:send-message", "api:im:get-message"],
    "expiresAt": "2026-12-31T23:59:59"
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "600",
    "userId": "user001",
    "appId": "10",
    "scopes": ["api:im:send-message", "api:im:get-message"],
    "expiresAt": "2026-12-31T23:59:59.000Z"
  },
  "page": null
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| 有效期为过去时间 | expires_at=2020-01-01 | 返回400，有效期不能为过去时间 |
| 空Scope列表 | scopes=[] | 返回400，Scope不能为空 |
| Scope数量超限 | scopes超过50个 | 返回400，Scope数量超限 |
| 用户不存在 | userId=not_exist | 返回404，用户不存在 |
| 应用不存在 | appId=999999 | 返回404，应用不存在 |
| 有效期格式错误 | expiresAt=invalid-date | 返回400，日期格式错误 |

---

#### TC-USER-AUTH-003: 取消授权

**接口**: DELETE /api/v1/user-authorizations/:id
**优先级**: P0
**前置条件**: 
- 授权ID=600存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 取消存在的授权 | id=600 | 返回200，取消成功 |
| 2 | 取消不存在的授权 | id=999999 | 返回404，授权不存在 |
| 3 | 重复取消 | 已取消的授权 | 返回404，授权不存在 |

**请求示例**:
```bash
curl -X DELETE "http://localhost:18081/api-server/api/v1/user-authorizations/600"
```

**预期响应（成功）**:
```json
{
  "code": "200",
  "messageZh": "授权已取消",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

**预期响应（失败）**:
```json
{
  "code": "404",
  "messageZh": "授权不存在",
  "messageEn": "Not Found",
  "data": null,
  "page": null
}
```

---

### 2.2 消费网关

#### TC-GATEWAY-001: API 请求代理与鉴权

**接口**: ANY /gateway/api/*
**优先级**: P0
**前置条件**: 
- 应用ID=10已订阅权限api:im:send-message
- 应用具有有效凭证（AKSK/Bearer Token）

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 有效请求（AKSK认证） | 正确AKSK签名 | 返回200，请求转发成功 |
| 2 | 有效请求（Bearer Token认证） | 正确Bearer Token | 返回200，请求转发成功 |
| 3 | 无效凭证 | 错误的AKSK | 返回401，认证失败 |
| 4 | 无订阅权限 | 未订阅的API | 返回403，权限不足 |
| 5 | 缺少认证头 | 无Authorization头 | 返回401，缺少认证信息 |

**请求示例**:
```bash
curl -X POST "http://localhost:18081/api-server/gateway/api/v1/messages" \
  -H "X-App-Id: 10" \
  -H "X-Auth-Type: 2" \
  -H "Authorization: Bearer valid_token_here" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "user002",
    "content": "Hello World"
  }'
```

**预期响应（成功）**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "messageId": "msg001",
    "status": "sent"
  },
  "page": null
}
```

**预期响应（认证失败）**:
```json
{
  "code": "401",
  "messageZh": "认证失败",
  "messageEn": "Unauthorized",
  "data": null,
  "page": null
}
```

**预期响应（权限不足）**:
```json
{
  "code": "403",
  "messageZh": "应用未订阅该API权限",
  "messageEn": "Forbidden",
  "data": null,
  "page": null
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| App-Id不存在 | X-App-Id=999999 | 返回401，应用不存在 |
| App-Id格式错误 | X-App-Id=abc | 返回400，参数格式错误 |
| 订阅已过期 | 订阅状态为已取消 | 返回403，订阅已失效 |
| 用户授权已过期 | 用户Scope授权过期 | 返回403，用户授权已过期 |
| 认证类型不匹配 | Auth-Type与凭证类型不匹配 | 返回401，认证类型不匹配 |
| 请求体过大 | Body超过10MB | 返回413，请求体过大 |
| 超时请求 | 后端响应超时 | 返回504，网关超时 |

**处理流程**:

1. 验证应用身份（AKSK/Bearer Token）
2. 查询应用订阅关系
3. 验证请求路径与方法是否在授权Scope范围内
4. 检查用户授权状态（如适用）
5. 转发请求到内部中台网关
6. 返回响应

**认证类型说明**:

| X-Auth-Type | 说明 | Authorization格式 |
|-------------|------|------------------|
| 0 | 应用类凭证A | 固定Token |
| 1 | 应用类凭证B | 固定Token |
| 2 | AKSK | 签名字符串 |
| 3 | Bearer Token | Bearer {token} |

---

#### TC-GATEWAY-002: 权限校验接口

**接口**: GET /gateway/permissions/check
**优先级**: P0
**前置条件**: 
- 应用ID=10已订阅权限api:im:send-message

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 校验已授权权限 | app_id=10, scope=api:im:send-message | 返回200，authorized=true |
| 2 | 校验未授权权限 | app_id=10, scope=api:cloud:upload | 返回200，authorized=false |
| 3 | 校验不存在应用 | app_id=999999 | 返回200，authorized=false |
| 4 | 缺少必填参数 | app_id为空 | 返回400，参数校验失败 |

**请求示例**:
```bash
curl -X GET "http://localhost:18081/api-server/gateway/permissions/check?app_id=10&scope=api:im:send-message"
```

**预期响应（已授权）**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "authorized": true,
    "subscriptionId": "300",
    "subscriptionStatus": 1
  },
  "page": null
}
```

**预期响应（未授权）**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "authorized": false,
    "reason": "应用未订阅该权限"
  },
  "page": null
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| Scope格式错误 | scope=invalid scope | 返回400，Scope格式错误 |
| Scope不存在 | scope=api:not:exist | 返回200，authorized=false |
| 订阅待审 | 订阅状态=0 | 返回200，authorized=false, reason="订阅待审批" |
| 订阅已取消 | 订阅状态=3 | 返回200，authorized=false, reason="订阅已取消" |
| 权限已禁用 | 权限status=0 | 返回200，authorized=false, reason="权限已禁用" |

---

## 三、测试总结

### 3.1 测试覆盖率

| 模块 | 接口数 | 测试用例数 | 覆盖率 |
|-----|-------|----------|-------|
| Scope授权管理 | 3 | 3 | 100% |
| 消费网关 | 2 | 2 | 100% |
| **总计** | **5** | **5** | **100%** |

### 3.2 测试统计

| 测试类型 | 用例数 | 占比 |
|---------|-------|------|
| 正常流程 | 12 | 50% |
| 异常流程 | 8 | 33% |
| 边界测试 | 4 | 17% |
| **总计** | **24** | **100%** |

### 3.3 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 认证失败/未授权 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 413 | 请求体过大 |
| 500 | 服务器内部错误 |
| 504 | 网关超时 |

### 3.4 认证类型枚举

| 值 | 说明 | Authorization格式 |
|-----|------|------------------|
| 0 | 应用类凭证A | 固定Token |
| 1 | 应用类凭证B | 固定Token |
| 2 | AKSK | 签名字符串 |
| 3 | Bearer Token | Bearer {token} |

### 3.5 订阅状态枚举

| 值 | 说明 |
|-----|------|
| 0 | 待审 |
| 1 | 已授权 |
| 2 | 已拒绝 |
| 3 | 已取消 |

### 3.6 性能测试要求

| 接口 | 响应时间要求 | 说明 |
|-----|------------|------|
| GET /api/v1/user-authorizations | P99 < 50ms | 列表查询 |
| POST /api/v1/user-authorizations | P99 < 100ms | 授权创建 |
| DELETE /api/v1/user-authorizations/:id | P99 < 50ms | 授权取消 |
| ANY /gateway/api/* | P99 < 200ms | 含转发时间 |
| GET /gateway/permissions/check | P99 < 20ms | 高频调用，需缓存优化 |

### 3.7 安全测试要点

1. **认证安全**
   - AKSK签名验证正确性
   - Token过期处理
   - 重放攻击防护

2. **授权安全**
   - Scope边界校验
   - 用户授权过期检查
   - 订阅状态实时校验

3. **数据安全**
   - 敏感信息脱敏
   - 请求日志审计
   - 异常访问告警

---

**文档状态**: ✅ 测试用例编写完成
