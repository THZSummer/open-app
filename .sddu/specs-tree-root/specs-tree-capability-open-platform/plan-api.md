# 接口设计

> 本文档为 `plan.md` 的子文档，定义能力开放平台的详细接口设计。
> 基于 spec.md 第 3 章 FR 清单编写，确保功能需求完整覆盖。

---

## 1. 接口清单

| # | 模块 | Method | Path | 说明 | FR |
|---|------|--------|------|------|-----|
| 1 | **分类管理** | GET | `/api/v1/categories` | 获取分类列表（树形） | FR-001 |
| 2 | | GET | `/api/v1/categories/:id` | 获取分类详情 | FR-001 |
| 3 | | POST | `/api/v1/categories` | 创建分类（一级分类） | FR-001 |
| 4 | | PUT | `/api/v1/categories/:id` | 更新分类 | FR-001 |
| 5 | | DELETE | `/api/v1/categories/:id` | 删除分类（检查关联资源） | FR-001 |
| 6 | | POST | `/api/v1/categories/:id/owners` | 添加分类责任人 | FR-002 |
| 7 | | GET | `/api/v1/categories/:id/owners` | 获取分类责任人列表 | FR-002 |
| 8 | | DELETE | `/api/v1/categories/:id/owners/:userId` | 移除分类责任人 | FR-002 |
| 9 | **API 管理** | GET | `/api/v1/apis` | 获取 API 列表（按分类过滤） | FR-004 |
| 10 | | GET | `/api/v1/apis/:id` | 获取 API 详情（含权限信息） | FR-004 |
| 11 | | POST | `/api/v1/apis` | 注册 API（附带权限定义） | FR-005 |
| 12 | | PUT | `/api/v1/apis/:id` | 更新 API 及权限信息 | FR-006 |
| 13 | | DELETE | `/api/v1/apis/:id` | 删除 API（检查订阅关系） | FR-007 |
| 14 | | POST | `/api/v1/apis/:id/withdraw` | 撤回审核中的 API | FR-004 |
| 15 | **事件管理** | GET | `/api/v1/events` | 获取事件列表（按分类过滤） | FR-008 |
| 16 | | GET | `/api/v1/events/:id` | 获取事件详情（含权限信息） | FR-008 |
| 17 | | POST | `/api/v1/events` | 注册事件（附带权限定义） | FR-009 |
| 18 | | PUT | `/api/v1/events/:id` | 更新事件及权限信息 | FR-010 |
| 19 | | DELETE | `/api/v1/events/:id` | 删除事件（检查订阅关系） | FR-011 |
| 20 | | POST | `/api/v1/events/:id/withdraw` | 撤回审核中的事件 | FR-008 |
| 21 | **回调管理** | GET | `/api/v1/callbacks` | 获取回调列表（按分类过滤） | FR-012 |
| 22 | | GET | `/api/v1/callbacks/:id` | 获取回调详情（含权限信息） | FR-012 |
| 23 | | POST | `/api/v1/callbacks` | 注册回调（附带权限定义） | FR-013 |
| 24 | | PUT | `/api/v1/callbacks/:id` | 更新回调及权限信息 | FR-014 |
| 25 | | DELETE | `/api/v1/callbacks/:id` | 删除回调（检查订阅关系） | FR-015 |
| 26 | | POST | `/api/v1/callbacks/:id/withdraw` | 撤回审核中的回调 | FR-012 |
| 27 | **API 权限管理** | GET | `/api/v1/apps/:appId/apis` | 获取应用 API 权限列表 | FR-016 |
| 28 | | GET | `/api/v1/permissions/apis/tree` | 获取 API 权限树（抽屉数据源） | FR-017 |
| 29 | | POST | `/api/v1/apps/:appId/apis/subscribe` | 申请 API 权限（独立单据） | FR-018 |
| 30 | | POST | `/api/v1/apps/:appId/apis/:id/withdraw` | 撤回审核中的申请 | FR-016 |
| 31 | **事件权限管理** | GET | `/api/v1/apps/:appId/events` | 获取应用事件订阅列表 | FR-019 |
| 32 | | GET | `/api/v1/permissions/events/tree` | 获取事件权限树（抽屉数据源） | FR-020 |
| 33 | | POST | `/api/v1/apps/:appId/events/subscribe` | 申请事件权限（独立单据） | FR-021 |
| 34 | | PUT | `/api/v1/apps/:appId/events/:id/config` | 配置事件消费参数（通道/地址/认证） | FR-019 |
| 35 | | POST | `/api/v1/apps/:appId/events/:id/withdraw` | 撤回审核中的申请 | FR-019 |
| 36 | **回调权限管理** | GET | `/api/v1/apps/:appId/callbacks` | 获取应用回调订阅列表 | FR-022 |
| 37 | | GET | `/api/v1/permissions/callbacks/tree` | 获取回调权限树（抽屉数据源） | FR-023 |
| 38 | | POST | `/api/v1/apps/:appId/callbacks/subscribe` | 申请回调权限（独立单据） | FR-024 |
| 39 | | PUT | `/api/v1/apps/:appId/callbacks/:id/config` | 配置回调消费参数（通道/地址/认证） | FR-022 |
| 40 | | POST | `/api/v1/apps/:appId/callbacks/:id/withdraw` | 撤回审核中的申请 | FR-022 |
| 41 | **审批管理** | GET | `/api/v1/approval-flows` | 获取审批流程模板列表 | FR-025 |
| 42 | | GET | `/api/v1/approval-flows/:id` | 获取审批流程模板详情 | FR-025 |
| 43 | | POST | `/api/v1/approval-flows` | 创建审批流程模板 | FR-025 |
| 44 | | PUT | `/api/v1/approval-flows/:id` | 更新审批流程模板 | FR-025 |
| 45 | | GET | `/api/v1/approvals/pending` | 获取待审批列表 | FR-026/FR-027 |
| 46 | | GET | `/api/v1/approvals/:id` | 获取审批详情 | FR-026/FR-027 |
| 47 | | POST | `/api/v1/approvals/:id/approve` | 同意审批 | FR-026/FR-027 |
| 48 | | POST | `/api/v1/approvals/:id/reject` | 驳回审批（需填写原因） | FR-026/FR-027 |
| 49 | | POST | `/api/v1/approvals/:id/cancel` | 撤销审批 | FR-026/FR-027 |
| 50 | **Scope 授权管理** | GET | `/api/v1/user-authorizations` | 获取用户授权列表 | FR-031 |
| 51 | | POST | `/api/v1/user-authorizations` | 用户授权（设置有效期） | FR-031 |
| 52 | | DELETE | `/api/v1/user-authorizations/:id` | 取消授权 | FR-031 |
| 53 | **消费网关** | ANY | `/gateway/api/*` | API 请求代理与鉴权 | FR-028 |
| 54 | | POST | `/gateway/events/publish` | 事件发布接口 | FR-029 |
| 55 | | POST | `/gateway/callbacks/invoke` | 回调触发接口 | FR-030 |
| 56 | | GET | `/gateway/permissions/check` | 权限校验接口 | FR-028/029/030 |

> **接口统计**：共 57 个接口，覆盖 FR-001 ~ FR-031

---

## 2. 接口详情

### 2.1 分类管理

#### GET /api/v1/categories

获取分类列表（树形结构）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_alias | string | 否 | 分类别名过滤 |

**响应示例**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "category_alias": "app_type_a",
      "name_cn": "A类应用权限",
      "name_en": "App Type A Permissions",
      "parent_id": null,
      "path": "/1/",
      "sort_order": 0,
      "status": 1,
      "children": [
        {
          "id": 2,
          "category_alias": null,
          "name_cn": "IM 业务",
          "name_en": "IM Business",
          "parent_id": 1,
          "path": "/1/2/",
          "sort_order": 0,
          "status": 1,
          "children": []
        }
      ]
    }
  ]
}
```

#### POST /api/v1/categories

创建分类。

**请求体**：

```json
{
  "category_alias": "app_type_a",
  "name_cn": "A类应用权限",
  "name_en": "App Type A Permissions",
  "parent_id": null,
  "sort_order": 0
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 1,
    "category_alias": "app_type_a",
    "name_cn": "A类应用权限",
    "name_en": "App Type A Permissions",
    "parent_id": null,
    "path": "/1/",
    "sort_order": 0,
    "status": 1
  }
}
```

---

### 2.2 API 管理

#### POST /api/v1/apis

注册 API（附带权限定义）。

**请求体**：

```json
{
  "name_cn": "发送消息",
  "name_en": "Send Message",
  "path": "/api/v1/messages",
  "method": "POST",
  "category_id": 2,
  "permission": {
    "name_cn": "发送消息权限",
    "name_en": "Send Message Permission",
    "scope": "api:im:send-message"
  },
  "properties": [
    { "property_name": "description_cn", "property_value": "发送消息API的中文描述" },
    { "property_name": "description_en", "property_value": "Send message API description" },
    { "property_name": "doc_url", "property_value": "https://docs.example.com/api/send-message" }
  ]
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 100,
    "name_cn": "发送消息",
    "name_en": "Send Message",
    "path": "/api/v1/messages",
    "method": "POST",
    "status": 0,
    "permission": {
      "id": 200,
      "scope": "api:im:send-message",
      "status": 1
    }
  }
}
```

---

### 2.3 事件管理

#### POST /api/v1/events

注册事件（附带权限定义）。

**请求体**：

```json
{
  "name_cn": "消息接收事件",
  "name_en": "Message Received Event",
  "topic": "im.message.received",
  "category_id": 2,
  "permission": {
    "name_cn": "消息接收权限",
    "name_en": "Message Received Permission",
    "scope": "event:im:message-received"
  },
  "properties": [
    { "property_name": "description_cn", "property_value": "消息接收事件的中文描述" },
    { "property_name": "doc_url", "property_value": "https://docs.example.com/event/message-received" }
  ]
}
```

---

### 2.4 API 权限管理

#### GET /api/v1/permissions/apis/tree

获取 API 权限树（抽屉数据源）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_alias | string | 是 | 分类别名 |
| keyword | string | 否 | 搜索关键词（名称、Scope） |

**响应示例**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 2,
      "name": "IM 业务",
      "type": "category",
      "children": [
        {
          "id": 200,
          "name_cn": "发送消息权限",
          "name_en": "Send Message Permission",
          "scope": "api:im:send-message",
          "type": "permission",
          "api": {
            "path": "/api/v1/messages",
            "method": "POST"
          }
        }
      ]
    }
  ]
}
```

#### POST /api/v1/apps/:appId/apis/subscribe

申请 API 权限（独立单据）。

**请求体**：

```json
{
  "permission_id": 200
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 300,
    "permission_id": 200,
    "status": 0,
    "message": "申请已提交，等待审批"
  }
}
```

---

### 2.5 事件权限管理

#### PUT /api/v1/apps/:appId/events/:id/config

配置事件消费参数。

**请求体**：

```json
{
  "channel_type": 1,
  "channel_address": "https://webhook.example.com/events",
  "auth_type": 0
}
```

**枚举值说明**：

| 字段 | 值 | 说明 |
|------|-----|------|
| channel_type | 0 | 企业内部消息队列 |
| channel_type | 1 | WebHook |
| auth_type | 0 | 应用类凭证A |
| auth_type | 1 | 应用类凭证B |

---

### 2.6 审批管理

#### POST /api/v1/approval-flows

创建审批流程模板。

**请求体**：

```json
{
  "name_cn": "API注册审批流",
  "name_en": "API Registration Approval Flow",
  "code": "api_register",
  "is_default": 0,
  "nodes": [
    { "type": "approver", "user_id": "user001", "order": 1 },
    { "type": "approver", "user_id": "user002", "order": 2 }
  ]
}
```

#### POST /api/v1/approvals/:id/approve

同意审批。

**请求体**：

```json
{
  "comment": "同意该申请"
}
```

#### POST /api/v1/approvals/:id/reject

驳回审批。

**请求体**：

```json
{
  "reason": "申请信息不完整，请补充说明"
}
```

---

### 2.7 Scope 授权管理

#### POST /api/v1/user-authorizations

用户授权。

**请求体**：

```json
{
  "user_id": "user001",
  "app_id": 100,
  "scopes": ["api:im:send-message", "api:im:get-message"],
  "expires_at": "2026-12-31T23:59:59"
}
```

---

### 2.8 消费网关

#### ANY /gateway/api/*

API 请求代理与鉴权。

**请求头**：

| Header | 说明 |
|--------|------|
| X-App-Id | 应用ID |
| X-Auth-Type | 认证类型 |
| Authorization | 认证凭证 |

**处理流程**：

1. 验证应用身份（AKSK/Bearer Token）
2. 查询应用订阅关系
3. 验证请求路径是否在授权范围内
4. 转发请求到内部中台网关

#### POST /gateway/events/publish

事件发布接口。

**请求体**：

```json
{
  "topic": "im.message.received",
  "payload": {
    "message_id": "msg001",
    "content": "Hello World"
  }
}
```

**处理流程**：

1. 验证 Topic 对应的事件资源存在
2. 查询订阅该事件的应用列表
3. 按订阅配置分发事件（WebHook/消息队列）

#### POST /gateway/callbacks/invoke

回调触发接口。

**请求体**：

```json
{
  "callback_scope": "callback:approval:completed",
  "payload": {
    "approval_id": "app001",
    "status": "approved"
  }
}
```

#### GET /gateway/permissions/check

权限校验接口（供网关内部调用）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| app_id | long | 是 | 应用ID |
| scope | string | 是 | 权限标识 |

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "authorized": true,
    "subscription_id": 300
  }
}
```

---

## 3. 附录

### 3.1 状态码定义

| 状态码 | 说明 |
|--------|------|
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器内部错误 |

### 3.2 资源状态枚举

**API/事件/回调状态**：

| 值 | 说明 |
|-----|------|
| 0 | 草稿 |
| 1 | 待审 |
| 2 | 已发布 |
| 3 | 已下线 |

**订阅状态**：

| 值 | 说明 |
|-----|------|
| 0 | 待审 |
| 1 | 已授权 |
| 2 | 已拒绝 |
| 3 | 已取消 |

**审批状态**：

| 值 | 说明 |
|-----|------|
| 0 | 待审 |
| 1 | 已通过 |
| 2 | 已拒绝 |
| 3 | 已撤销 |