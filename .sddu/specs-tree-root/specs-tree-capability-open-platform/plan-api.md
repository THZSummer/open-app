# 接口设计

> 本文档为 `plan.md` 的子文档，定义能力开放平台的详细接口设计。
> 基于 spec.md 第 3 章 FR 清单编写，确保功能需求完整覆盖。

## 接口清单概览

| 模块 | 对应 FR | 接口数 | 角色 |
|------|---------|--------|------|
| 分类管理 | FR-001, FR-002 | 8 | 运营方 |
| API 管理 | FR-004~FR-007 | 6 | 分类责任人 |
| 事件管理 | FR-008~FR-011 | 6 | 分类责任人 |
| 回调管理 | FR-012~FR-015 | 6 | 分类责任人 |
| API 权限管理 | FR-016~FR-018 | 4 | 消费方 |
| 事件权限管理 | FR-019~FR-021 | 5 | 消费方 |
| 回调权限管理 | FR-022~FR-024 | 5 | 消费方 |
| 审批管理 | FR-025~FR-027 | 10 | 运营方/审批人 |
| Scope 用户授权 | FR-031 | 3 | 用户 |
| 消费网关 | FR-028~FR-030 | 4 | 三方应用/业务模块 |
| **总计** | **FR-001~FR-031** | **57** | - |

---

## 1. 分类管理（运营方）

> 对应 FR：FR-001 分类创建/编辑、FR-002 分类责任人配置

### 1.1 接口列表

| Method | Path | 说明 | FR |
|--------|------|------|-----|
| GET | `/api/v1/categories` | 获取分类列表（树形） | FR-001 |
| GET | `/api/v1/categories/:id` | 获取分类详情 | FR-001 |
| POST | `/api/v1/categories` | 创建分类（一级分类） | FR-001 |
| PUT | `/api/v1/categories/:id` | 更新分类 | FR-001 |
| DELETE | `/api/v1/categories/:id` | 删除分类（检查关联资源） | FR-001 |
| POST | `/api/v1/categories/:id/owners` | 添加分类责任人 | FR-002 |
| GET | `/api/v1/categories/:id/owners` | 获取分类责任人列表 | FR-002 |
| DELETE | `/api/v1/categories/:id/owners/:userId` | 移除分类责任人 | FR-002 |

### 1.2 接口详情

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

## 2. API 管理（提供方）

> 对应 FR：FR-004 API 权限列表查看、FR-005 API 权限注册、FR-006 API 权限编辑、FR-007 API 权限删除

### 2.1 接口列表

| Method | Path | 说明 | FR |
|--------|------|------|-----|
| GET | `/api/v1/apis` | 获取 API 列表（按分类过滤） | FR-004 |
| GET | `/api/v1/apis/:id` | 获取 API 详情（含权限信息） | FR-004 |
| POST | `/api/v1/apis` | 注册 API（附带权限定义） | FR-005 |
| PUT | `/api/v1/apis/:id` | 更新 API 及权限信息 | FR-006 |
| DELETE | `/api/v1/apis/:id` | 删除 API（检查订阅关系） | FR-007 |
| POST | `/api/v1/apis/:id/withdraw` | 撤回审核中的 API | FR-004 |

### 2.2 接口详情

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

## 3. 事件管理（提供方）

> 对应 FR：FR-008 事件权限列表查看、FR-009 事件权限注册、FR-010 事件权限编辑、FR-011 事件权限删除

### 3.1 接口列表

| Method | Path | 说明 | FR |
|--------|------|------|-----|
| GET | `/api/v1/events` | 获取事件列表（按分类过滤） | FR-008 |
| GET | `/api/v1/events/:id` | 获取事件详情（含权限信息） | FR-008 |
| POST | `/api/v1/events` | 注册事件（附带权限定义） | FR-009 |
| PUT | `/api/v1/events/:id` | 更新事件及权限信息 | FR-010 |
| DELETE | `/api/v1/events/:id` | 删除事件（检查订阅关系） | FR-011 |
| POST | `/api/v1/events/:id/withdraw` | 撤回审核中的事件 | FR-008 |

### 3.2 接口详情

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

## 4. 回调管理（提供方）

> 对应 FR：FR-012 回调权限列表查看、FR-013 回调权限注册、FR-014 回调权限编辑、FR-015 回调权限删除

### 4.1 接口列表

| Method | Path | 说明 | FR |
|--------|------|------|-----|
| GET | `/api/v1/callbacks` | 获取回调列表（按分类过滤） | FR-012 |
| GET | `/api/v1/callbacks/:id` | 获取回调详情（含权限信息） | FR-012 |
| POST | `/api/v1/callbacks` | 注册回调（附带权限定义） | FR-013 |
| PUT | `/api/v1/callbacks/:id` | 更新回调及权限信息 | FR-014 |
| DELETE | `/api/v1/callbacks/:id` | 删除回调（检查订阅关系） | FR-015 |
| POST | `/api/v1/callbacks/:id/withdraw` | 撤回审核中的回调 | FR-012 |

---

## 5. API 权限管理（消费方）

> 对应 FR：FR-016 应用权限列表查看、FR-017 API 权限树形选择、FR-018 API 权限申请提交

### 5.1 接口列表

| Method | Path | 说明 | FR |
|--------|------|------|-----|
| GET | `/api/v1/apps/:appId/apis` | 获取应用 API 权限列表 | FR-016 |
| GET | `/api/v1/permissions/apis/tree` | 获取 API 权限树（抽屉数据源） | FR-017 |
| POST | `/api/v1/apps/:appId/apis/subscribe` | 申请 API 权限（独立单据） | FR-018 |
| POST | `/api/v1/apps/:appId/apis/:id/withdraw` | 撤回审核中的申请 | FR-016 |

### 5.2 接口详情

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

## 6. 事件权限管理（消费方）

> 对应 FR：FR-019 应用事件列表查看与配置、FR-020 事件权限树形选择、FR-021 事件权限申请提交

### 6.1 接口列表

| Method | Path | 说明 | FR |
|--------|------|------|-----|
| GET | `/api/v1/apps/:appId/events` | 获取应用事件订阅列表 | FR-019 |
| GET | `/api/v1/permissions/events/tree` | 获取事件权限树（抽屉数据源） | FR-020 |
| POST | `/api/v1/apps/:appId/events/subscribe` | 申请事件权限（独立单据） | FR-021 |
| PUT | `/api/v1/apps/:appId/events/:id/config` | 配置事件消费参数（通道/地址/认证） | FR-019 |
| POST | `/api/v1/apps/:appId/events/:id/withdraw` | 撤回审核中的申请 | FR-019 |

### 6.2 接口详情

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

## 7. 回调权限管理（消费方）

> 对应 FR：FR-022 应用回调列表查看与配置、FR-023 回调权限树形选择、FR-024 回调权限申请提交

### 7.1 接口列表

| Method | Path | 说明 | FR |
|--------|------|------|-----|
| GET | `/api/v1/apps/:appId/callbacks` | 获取应用回调订阅列表 | FR-022 |
| GET | `/api/v1/permissions/callbacks/tree` | 获取回调权限树（抽屉数据源） | FR-023 |
| POST | `/api/v1/apps/:appId/callbacks/subscribe` | 申请回调权限（独立单据） | FR-024 |
| PUT | `/api/v1/apps/:appId/callbacks/:id/config` | 配置回调消费参数（通道/地址/认证） | FR-022 |
| POST | `/api/v1/apps/:appId/callbacks/:id/withdraw` | 撤回审核中的申请 | FR-022 |

---

## 8. 审批管理

> 对应 FR：FR-025 审批流程配置、FR-026 资源注册审批、FR-027 权限申请审批

### 8.1 接口列表

| Method | Path | 说明 | FR |
|--------|------|------|-----|
| GET | `/api/v1/approval-flows` | 获取审批流程模板列表 | FR-025 |
| GET | `/api/v1/approval-flows/:id` | 获取审批流程模板详情 | FR-025 |
| POST | `/api/v1/approval-flows` | 创建审批流程模板 | FR-025 |
| PUT | `/api/v1/approval-flows/:id` | 更新审批流程模板 | FR-025 |
| GET | `/api/v1/approvals/pending` | 获取待审批列表 | FR-026/FR-027 |
| GET | `/api/v1/approvals/:id` | 获取审批详情 | FR-026/FR-027 |
| POST | `/api/v1/approvals/:id/approve` | 同意审批 | FR-026/FR-027 |
| POST | `/api/v1/approvals/:id/reject` | 驳回审批（需填写原因） | FR-026/FR-027 |
| POST | `/api/v1/approvals/:id/cancel` | 撤销审批 | FR-026/FR-027 |

### 8.2 接口详情

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

## 9. Scope 用户授权管理

> 对应 FR：FR-031 用户授权授予

### 9.1 接口列表

| Method | Path | 说明 | FR |
|--------|------|------|-----|
| GET | `/api/v1/user-authorizations` | 获取用户授权列表 | FR-031 |
| POST | `/api/v1/user-authorizations` | 用户授权（设置有效期） | FR-031 |
| DELETE | `/api/v1/user-authorizations/:id` | 取消授权 | FR-031 |

### 9.2 接口详情

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

## 10. 消费网关接口（数据面）

> 对应 FR：FR-028 API 鉴权、FR-029 事件分发、FR-030 回调路由

### 10.1 接口列表

| Method | Path | 说明 | FR | 调用方 |
|--------|------|------|-----|--------|
| ANY | `/gateway/api/*` | API 请求代理与鉴权 | FR-028 | 三方应用 |
| POST | `/gateway/events/publish` | 事件发布接口 | FR-029 | 业务模块 |
| POST | `/gateway/callbacks/invoke` | 回调触发接口 | FR-030 | 业务模块 |
| GET | `/gateway/permissions/check` | 权限校验接口 | FR-028/029/030 | 内部调用 |

### 10.2 接口详情

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

## 附录

### A. 状态码定义

| 状态码 | 说明 |
|--------|------|
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器内部错误 |

### B. 资源状态枚举

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
