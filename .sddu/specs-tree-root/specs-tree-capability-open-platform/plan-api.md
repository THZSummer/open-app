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
| 28 | | GET | `/api/v1/categories/:id/apis` | 获取分类下 API 权限列表 | FR-017 |
| 29 | | POST | `/api/v1/apps/:appId/apis/subscribe` | 申请 API 权限（支持批量） | FR-018 |
| 30 | | POST | `/api/v1/apps/:appId/apis/:id/withdraw` | 撤回审核中的申请 | FR-016 |
| 31 | **事件权限管理** | GET | `/api/v1/apps/:appId/events` | 获取应用事件订阅列表 | FR-019 |
| 32 | | GET | `/api/v1/categories/:id/events` | 获取分类下事件权限列表 | FR-020 |
| 33 | | POST | `/api/v1/apps/:appId/events/subscribe` | 申请事件权限（支持批量） | FR-021 |
| 34 | | PUT | `/api/v1/apps/:appId/events/:id/config` | 配置事件消费参数（通道/地址/认证） | FR-019 |
| 35 | | POST | `/api/v1/apps/:appId/events/:id/withdraw` | 撤回审核中的申请 | FR-019 |
| 36 | **回调权限管理** | GET | `/api/v1/apps/:appId/callbacks` | 获取应用回调订阅列表 | FR-022 |
| 37 | | GET | `/api/v1/categories/:id/callbacks` | 获取分类下回调权限列表 | FR-023 |
| 38 | | POST | `/api/v1/apps/:appId/callbacks/subscribe` | 申请回调权限（支持批量） | FR-024 |
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
| 50 | | POST | `/api/v1/approvals/batch-approve` | 批量同意审批 | FR-026/FR-027 |
| 51 | | POST | `/api/v1/approvals/batch-reject` | 批量驳回审批（需填写原因） | FR-026/FR-027 |
| 52 | **Scope 授权管理** | GET | `/api/v1/user-authorizations` | 获取用户授权列表 | FR-031 |
| 53 | | POST | `/api/v1/user-authorizations` | 用户授权（设置有效期） | FR-031 |
| 54 | | DELETE | `/api/v1/user-authorizations/:id` | 取消授权 | FR-031 |
| 55 | **消费网关** | ANY | `/gateway/api/*` | API 请求代理与鉴权 | FR-028 |
| 56 | | POST | `/gateway/events/publish` | 事件发布接口 | FR-029 |
| 57 | | POST | `/gateway/callbacks/invoke` | 回调触发接口 | FR-030 |
| 58 | | GET | `/gateway/permissions/check` | 权限校验接口 | FR-028/029/030 |

> **接口统计**：共 58 个接口，覆盖 FR-001 ~ FR-031
>
> **权限树设计说明**：采用懒加载模式，分为两个步骤：
> 1. 查树：`GET /api/v1/categories` 获取分类树
> 2. 查权限：`GET /api/v1/categories/:id/apis` 获取某分类下的权限列表

---

## 2. 接口详情

### 2.1 分类管理

#### 1. GET /api/v1/categories

获取分类列表（树形结构）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_alias | string | 否 | 分类别名过滤，用于获取指定权限树 |

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

---

#### 2. GET /api/v1/categories/:id

获取分类详情。

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 2,
    "category_alias": null,
    "name_cn": "IM 业务",
    "name_en": "IM Business",
    "parent_id": 1,
    "path": "/1/2/",
    "sort_order": 0,
    "status": 1,
    "create_time": "2026-04-20T10:00:00.000Z",
    "create_by": "admin"
  }
}
```

---

#### 3. POST /api/v1/categories

创建分类（一级分类）。

> **说明**：仅支持创建一级分类（根分类），子分类通过 `parent_id` 指定父节点。一级分类需设置 `category_alias` 以区分不同权限树。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_alias | string | 条件 | 分类别名，一级分类必填，子分类为空 |
| name_cn | string | 是 | 中文名称 |
| name_en | string | 是 | 英文名称 |
| parent_id | long | 否 | 父分类ID，创建一级分类时为 null |
| sort_order | int | 否 | 排序序号，默认 0 |

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

#### 4. PUT /api/v1/categories/:id

更新分类。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name_cn | string | 是 | 中文名称 |
| name_en | string | 是 | 英文名称 |
| sort_order | int | 否 | 排序序号 |

```json
{
  "name_cn": "IM 业务能力",
  "name_en": "IM Business Capability",
  "sort_order": 1
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 2,
    "name_cn": "IM 业务能力",
    "name_en": "IM Business Capability",
    "sort_order": 1
  }
}
```

---

#### 5. DELETE /api/v1/categories/:id

删除分类（检查关联资源）。

> **说明**：删除前检查是否存在关联的 API/事件/回调资源。若存在关联资源，返回错误提示。

**响应示例（成功）**：

```json
{
  "code": 0,
  "data": null,
  "message": "分类删除成功"
}
```

**响应示例（失败-存在关联资源）**：

```json
{
  "code": 409,
  "message": "分类下存在 5 个 API 资源，无法删除"
}
```

---

#### 6. POST /api/v1/categories/:id/owners

添加分类责任人。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_id | string | 是 | 用户ID |
| user_name | string | 否 | 用户名称（用于展示） |

```json
{
  "user_id": "user001",
  "user_name": "张三"
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 100,
    "category_id": 2,
    "user_id": "user001",
    "user_name": "张三"
  }
}
```

---

#### 7. GET /api/v1/categories/:id/owners

获取分类责任人列表。

**响应示例**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 100,
      "category_id": 2,
      "user_id": "user001",
      "user_name": "张三"
    },
    {
      "id": 101,
      "category_id": 2,
      "user_id": "user002",
      "user_name": "李四"
    }
  ]
}
```

---

#### 8. DELETE /api/v1/categories/:id/owners/:userId

移除分类责任人。

**响应示例**：

```json
{
  "code": 0,
  "data": null,
  "message": "责任人移除成功"
}
```

---

### 2.2 API 管理

#### 9. GET /api/v1/apis

获取 API 列表（按分类过滤）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_id | long | 否 | 分类ID过滤 |
| status | int | 否 | 状态过滤（0=草稿, 1=待审, 2=已发布, 3=已下线） |
| keyword | string | 否 | 搜索关键词（名称、Scope） |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 20 |

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "total": 50,
    "page": 1,
    "size": 20,
    "list": [
      {
        "id": 100,
        "name_cn": "发送消息",
        "name_en": "Send Message",
        "path": "/api/v1/messages",
        "method": "POST",
        "category_id": 2,
        "category_name": "IM 业务",
        "status": 2,
        "permission": {
          "id": 200,
          "scope": "api:im:send-message",
          "status": 1
        }
      }
    ]
  }
}
```

---

#### 10. GET /api/v1/apis/:id

获取 API 详情（含权限信息及属性）。

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
    "category_id": 2,
    "status": 2,
    "create_time": "2026-04-20T10:00:00.000Z",
    "create_by": "user001",
    "permission": {
      "id": 200,
      "name_cn": "发送消息权限",
      "name_en": "Send Message Permission",
      "scope": "api:im:send-message",
      "status": 1
    },
    "properties": [
      { "property_name": "description_cn", "property_value": "发送消息API的中文描述" },
      { "property_name": "description_en", "property_value": "Send message API description" },
      { "property_name": "doc_url", "property_value": "https://docs.example.com/api/send-message" }
    ]
  }
}
```

---

#### 11. POST /api/v1/apis

注册 API（附带权限定义）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name_cn | string | 是 | 中文名称 |
| name_en | string | 是 | 英文名称 |
| path | string | 是 | API 路径 |
| method | string | 是 | HTTP 方法（GET/POST/PUT/DELETE） |
| category_id | long | 是 | 所属分类ID |
| permission | object | 是 | 权限定义 |
| properties | array | 否 | 扩展属性列表 |

**permission 对象**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name_cn | string | 是 | 权限中文名称 |
| name_en | string | 是 | 权限英文名称 |
| scope | string | 是 | Scope标识，格式 `api:{模块}:{资源标识}` |
| approval_flow_id | long | 否 | 审批流程ID，不填使用默认流程 |

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
    "status": 1,
    "permission": {
      "id": 200,
      "scope": "api:im:send-message",
      "status": 1
    }
  },
  "message": "API 注册成功，等待审批"
}
```

---

#### 12. PUT /api/v1/apis/:id

更新 API 及权限信息。

> **说明**：核心属性（path、method、scope）变更需重新审批。

**请求体**：

```json
{
  "name_cn": "发送消息V2",
  "name_en": "Send Message V2",
  "category_id": 2,
  "permission": {
    "name_cn": "发送消息权限V2",
    "name_en": "Send Message Permission V2"
  },
  "properties": [
    { "property_name": "description_cn", "property_value": "发送消息API的中文描述（更新）" }
  ]
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 100,
    "name_cn": "发送消息V2",
    "status": 1,
    "message": "API 更新成功，核心属性变更需重新审批"
  }
}
```

---

#### 13. DELETE /api/v1/apis/:id

删除 API（检查订阅关系）。

> **说明**：已订阅的 API 无法删除，需先取消所有订阅。

**响应示例（成功）**：

```json
{
  "code": 0,
  "data": null,
  "message": "API 删除成功"
}
```

**响应示例（失败-存在订阅）**：

```json
{
  "code": 409,
  "message": "API 被 3 个应用订阅，无法删除"
}
```

---

#### 14. POST /api/v1/apis/:id/withdraw

撤回审核中的 API。

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 100,
    "status": 0,
    "message": "API 已撤回，状态变为草稿"
  }
}
```

---

### 2.3 事件管理

#### 15. GET /api/v1/events

获取事件列表（按分类过滤）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_id | long | 否 | 分类ID过滤 |
| status | int | 否 | 状态过滤 |
| keyword | string | 否 | 搜索关键词（名称、Scope、Topic） |
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "total": 30,
    "list": [
      {
        "id": 101,
        "name_cn": "消息接收事件",
        "name_en": "Message Received Event",
        "topic": "im.message.received",
        "category_id": 2,
        "status": 2,
        "permission": {
          "id": 201,
          "scope": "event:im:message-received"
        }
      }
    ]
  }
}
```

---

#### 16. GET /api/v1/events/:id

获取事件详情（含权限信息及属性）。

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 101,
    "name_cn": "消息接收事件",
    "name_en": "Message Received Event",
    "topic": "im.message.received",
    "category_id": 2,
    "status": 2,
    "permission": {
      "id": 201,
      "name_cn": "消息接收权限",
      "name_en": "Message Received Permission",
      "scope": "event:im:message-received",
      "status": 1
    },
    "properties": [
      { "property_name": "description_cn", "property_value": "消息接收事件描述" },
      { "property_name": "doc_url", "property_value": "https://docs.example.com/event/message-received" }
    ]
  }
}
```

---

#### 17. POST /api/v1/events

注册事件（附带权限定义）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name_cn | string | 是 | 中文名称 |
| name_en | string | 是 | 英文名称 |
| topic | string | 是 | 事件 Topic，全局唯一 |
| category_id | long | 是 | 所属分类ID |
| permission | object | 是 | 权限定义 |
| properties | array | 否 | 扩展属性 |

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
    { "property_name": "description_cn", "property_value": "消息接收事件描述" },
    { "property_name": "doc_url", "property_value": "https://docs.example.com/event/message-received" }
  ]
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 101,
    "name_cn": "消息接收事件",
    "topic": "im.message.received",
    "status": 1,
    "permission": {
      "id": 201,
      "scope": "event:im:message-received"
    }
  },
  "message": "事件注册成功，等待审批"
}
```

---

#### 18. PUT /api/v1/events/:id

更新事件及权限信息。

**请求体**：

```json
{
  "name_cn": "消息接收事件V2",
  "category_id": 2,
  "permission": {
    "name_cn": "消息接收权限V2"
  }
}
```

---

#### 19. DELETE /api/v1/events/:id

删除事件（检查订阅关系）。

> **说明**：已订阅的事件无法删除。

---

#### 20. POST /api/v1/events/:id/withdraw

撤回审核中的事件。

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 101,
    "status": 0,
    "message": "事件已撤回"
  }
}
```

---

### 2.4 回调管理

#### 21. GET /api/v1/callbacks

获取回调列表（按分类过滤）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_id | long | 否 | 分类ID过滤 |
| status | int | 否 | 状态过滤 |
| keyword | string | 否 | 搜索关键词 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "total": 20,
    "list": [
      {
        "id": 102,
        "name_cn": "审批完成回调",
        "name_en": "Approval Completed Callback",
        "category_id": 3,
        "status": 2,
        "permission": {
          "id": 202,
          "scope": "callback:approval:completed"
        }
      }
    ]
  }
}
```

---

#### 22. GET /api/v1/callbacks/:id

获取回调详情（含权限信息及属性）。

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 102,
    "name_cn": "审批完成回调",
    "name_en": "Approval Completed Callback",
    "category_id": 3,
    "status": 2,
    "permission": {
      "id": 202,
      "name_cn": "审批完成回调权限",
      "name_en": "Approval Completed Callback Permission",
      "scope": "callback:approval:completed",
      "status": 1
    },
    "properties": [
      { "property_name": "description_cn", "property_value": "审批完成后的回调通知" },
      { "property_name": "doc_url", "property_value": "https://docs.example.com/callback/approval-completed" }
    ]
  }
}
```

---

#### 23. POST /api/v1/callbacks

注册回调（附带权限定义）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name_cn | string | 是 | 中文名称 |
| name_en | string | 是 | 英文名称 |
| category_id | long | 是 | 所属分类ID |
| permission | object | 是 | 权限定义，scope 格式 `callback:{模块}:{资源标识}` |
| properties | array | 否 | 扩展属性 |

```json
{
  "name_cn": "审批完成回调",
  "name_en": "Approval Completed Callback",
  "category_id": 3,
  "permission": {
    "name_cn": "审批完成回调权限",
    "name_en": "Approval Completed Callback Permission",
    "scope": "callback:approval:completed"
  },
  "properties": [
    { "property_name": "description_cn", "property_value": "审批完成后的回调通知" },
    { "property_name": "doc_url", "property_value": "https://docs.example.com/callback/approval-completed" }
  ]
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 102,
    "name_cn": "审批完成回调",
    "status": 1,
    "permission": {
      "id": 202,
      "scope": "callback:approval:completed"
    }
  },
  "message": "回调注册成功，等待审批"
}
```

---

#### 24. PUT /api/v1/callbacks/:id

更新回调及权限信息。

**请求体**：

```json
{
  "name_cn": "审批完成回调V2",
  "category_id": 3,
  "permission": {
    "name_cn": "审批完成回调权限V2"
  }
}
```

---

#### 25. DELETE /api/v1/callbacks/:id

删除回调（检查订阅关系）。

---

#### 26. POST /api/v1/callbacks/:id/withdraw

撤回审核中的回调。

---

### 2.5 API 权限管理

#### 27. GET /api/v1/apps/:appId/apis

获取应用 API 权限列表。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 否 | 订阅状态过滤（0=待审, 1=已授权, 2=已拒绝, 3=已取消） |

**响应示例**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 300,
      "app_id": 10,
      "permission_id": 200,
      "permission": {
        "name_cn": "发送消息权限",
        "scope": "api:im:send-message"
      },
      "status": 1,
      "auth_type": 0,
      "doc_url": "https://docs.example.com/api/send-message",
      "approval_url": "https://platform.example.com/approval/300",
      "create_time": "2026-04-20T10:00:00.000Z"
    }
  ]
}
```

---

#### 28. GET /api/v1/categories/:id/apis

获取分类下 API 权限列表。

> **说明**：权限树采用懒加载模式。分类树通过 `GET /api/v1/categories` 获取，点击分类节点时调用此接口获取该分类下的权限列表。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词（名称、Scope） |
| need_approval | int | 否 | 是否需要审核过滤（0=不需要审核, 1=需要审核） |

**响应示例**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 200,
      "name_cn": "发送消息权限",
      "name_en": "Send Message Permission",
      "scope": "api:im:send-message",
      "status": 1,
      "api": {
        "path": "/api/v1/messages",
        "method": "POST"
      }
    }
  ]
}
```

---

#### 29. POST /api/v1/apps/:appId/apis/subscribe

申请 API 权限（支持批量）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| permission_ids | array[long] | 是 | 权限ID列表（支持批量提交） |

```json
{
  "permission_ids": [200, 201, 202]
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "success_count": 3,
    "failed_count": 0,
    "records": [
      { "id": 300, "app_id": 10, "permission_id": 200, "status": 0 },
      { "id": 301, "app_id": 10, "permission_id": 201, "status": 0 },
      { "id": 302, "app_id": 10, "permission_id": 202, "status": 0 }
    ],
    "message": "申请已提交，共3条，等待审批"
  }
}
```

---

#### 30. POST /api/v1/apps/:appId/apis/:id/withdraw

撤回审核中的 API 权限申请。

> **说明**：仅状态为"待审"的申请可撤回。

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 300,
    "status": 3,
    "message": "申请已撤回"
  }
}
```

---

### 2.6 事件权限管理

#### 31. GET /api/v1/apps/:appId/events

获取应用事件订阅列表。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 否 | 订阅状态过滤 |

**响应示例**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 301,
      "app_id": 10,
      "permission_id": 201,
      "permission": {
        "name_cn": "消息接收权限",
        "scope": "event:im:message-received"
      },
      "event": {
        "topic": "im.message.received"
      },
      "status": 1,
      "channel_type": 1,
      "channel_address": "https://webhook.example.com/events",
      "auth_type": 0
    }
  ]
}
```

---

#### 32. GET /api/v1/categories/:id/events

获取分类下事件权限列表。

> **说明**：权限树懒加载，点击分类节点时调用。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词 |
| need_approval | int | 否 | 是否需要审核过滤（0=不需要审核, 1=需要审核） |

**响应示例**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 201,
      "name_cn": "消息接收权限",
      "name_en": "Message Received Permission",
      "scope": "event:im:message-received",
      "status": 1,
      "event": {
        "topic": "im.message.received"
      }
    }
  ]
}
```

---

#### 33. POST /api/v1/apps/:appId/events/subscribe

申请事件权限（支持批量）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| permission_ids | array[long] | 是 | 权限ID列表（支持批量提交） |

```json
{
  "permission_ids": [201, 202, 203]
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "success_count": 3,
    "failed_count": 0,
    "records": [
      { "id": 301, "app_id": 10, "permission_id": 201, "status": 0 },
      { "id": 302, "app_id": 10, "permission_id": 202, "status": 0 },
      { "id": 303, "app_id": 10, "permission_id": 203, "status": 0 }
    ],
    "message": "申请已提交，共3条，等待审批"
  }
}
```

---

#### 34. PUT /api/v1/apps/:appId/events/:id/config

配置事件消费参数。

> **说明**：权限审批通过后，消费方需配置事件接收方式。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| channel_type | int | 是 | 通道类型（0=企业内部消息队列, 1=WebHook） |
| channel_address | string | 条件 | 通道地址，WebHook 类型时必填 |
| auth_type | int | 是 | 认证类型（0=应用类凭证A, 1=应用类凭证B） |

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

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 301,
    "channel_type": 1,
    "channel_address": "https://webhook.example.com/events",
    "auth_type": 0,
    "message": "事件消费参数配置成功"
  }
}
```

---

#### 35. POST /api/v1/apps/:appId/events/:id/withdraw

撤回审核中的事件权限申请。

---

### 2.7 回调权限管理

#### 36. GET /api/v1/apps/:appId/callbacks

获取应用回调订阅列表。

**响应示例**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 302,
      "app_id": 10,
      "permission_id": 202,
      "permission": {
        "name_cn": "审批完成回调权限",
        "scope": "callback:approval:completed"
      },
      "status": 1,
      "channel_type": 1,
      "channel_address": "https://webhook.example.com/callbacks",
      "auth_type": 0
    }
  ]
}
```

---

#### 37. GET /api/v1/categories/:id/callbacks

获取分类下回调权限列表。

> **说明**：权限树懒加载，点击分类节点时调用。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词 |
| need_approval | int | 否 | 是否需要审核过滤（0=不需要审核, 1=需要审核） |

**响应示例**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 202,
      "name_cn": "审批完成回调权限",
      "name_en": "Approval Completed Callback Permission",
      "scope": "callback:approval:completed",
      "status": 1
    }
  ]
}
```

---

#### 38. POST /api/v1/apps/:appId/callbacks/subscribe

申请回调权限（支持批量）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| permission_ids | array[long] | 是 | 权限ID列表（支持批量提交） |

```json
{
  "permission_ids": [202, 203, 204]
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "success_count": 3,
    "failed_count": 0,
    "records": [
      { "id": 302, "app_id": 10, "permission_id": 202, "status": 0 },
      { "id": 303, "app_id": 10, "permission_id": 203, "status": 0 },
      { "id": 304, "app_id": 10, "permission_id": 204, "status": 0 }
    ],
    "message": "申请已提交，共3条，等待审批"
  }
}
```

---

#### 39. PUT /api/v1/apps/:appId/callbacks/:id/config

配置回调消费参数。

> **说明**：权限审批通过后，消费方需配置回调接收方式。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| channel_type | int | 是 | 通道类型（0=WebHook, 1=SSE, 2=WebSocket） |
| channel_address | string | 条件 | 通道地址 |
| auth_type | int | 是 | 认证类型 |

```json
{
  "channel_type": 0,
  "channel_address": "https://webhook.example.com/callbacks",
  "auth_type": 0
}
```

**枚举值说明**：

| 字段 | 值 | 说明 |
|------|-----|------|
| channel_type | 0 | WebHook |
| channel_type | 1 | SSE |
| channel_type | 2 | WebSocket |

---

#### 40. POST /api/v1/apps/:appId/callbacks/:id/withdraw

撤回审核中的回调权限申请。

---

### 2.8 审批管理

#### 41. GET /api/v1/approval-flows

获取审批流程模板列表。

**响应示例**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "name_cn": "默认审批流",
      "name_en": "Default Approval Flow",
      "code": "default",
      "is_default": 1,
      "status": 1
    },
    {
      "id": 2,
      "name_cn": "API注册审批流",
      "name_en": "API Registration Approval Flow",
      "code": "api_register",
      "is_default": 0,
      "status": 1
    }
  ]
}
```

---

#### 42. GET /api/v1/approval-flows/:id

获取审批流程模板详情。

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 2,
    "name_cn": "API注册审批流",
    "name_en": "API Registration Approval Flow",
    "code": "api_register",
    "is_default": 0,
    "status": 1,
    "nodes": [
      { "type": "approver", "user_id": "user001", "user_name": "张三", "order": 1 },
      { "type": "approver", "user_id": "user002", "user_name": "李四", "order": 2 }
    ]
  }
}
```

---

#### 43. POST /api/v1/approval-flows

创建审批流程模板。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name_cn | string | 是 | 中文名称 |
| name_en | string | 是 | 英文名称 |
| code | string | 是 | 流程编码，全局唯一 |
| is_default | int | 否 | 是否默认流程（0=否, 1=是） |
| nodes | array | 是 | 审批节点列表 |

**nodes 数组元素**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 节点类型（approver=审批人） |
| user_id | string | 是 | 审批人ID |
| order | int | 是 | 节点顺序 |

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

---

#### 44. PUT /api/v1/approval-flows/:id

更新审批流程模板。

**请求体**：

```json
{
  "name_cn": "API注册审批流V2",
  "nodes": [
    { "type": "approver", "user_id": "user003", "order": 1 }
  ]
}
```

---

#### 45. GET /api/v1/approvals/pending

获取待审批列表。

> **说明**：返回当前用户待处理的审批单。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 审批类型（resource_register=资源注册, permission_apply=权限申请） |
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "total": 10,
    "list": [
      {
        "id": 500,
        "type": "resource_register",
        "business_type": "api",
        "business_id": 100,
        "business_name": "发送消息",
        "applicant_id": "user003",
        "applicant_name": "王五",
        "status": 0,
        "current_node": 1,
        "create_time": "2026-04-20T10:00:00.000Z"
      }
    ]
  }
}
```

---

#### 46. GET /api/v1/approvals/:id

获取审批详情。

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 500,
    "type": "resource_register",
    "business_type": "api",
    "business_id": 100,
    "business_data": {
      "name_cn": "发送消息",
      "path": "/api/v1/messages",
      "method": "POST"
    },
    "applicant_id": "user003",
    "applicant_name": "王五",
    "status": 0,
    "flow_id": 2,
    "current_node": 1,
    "nodes": [
      { "order": 1, "user_id": "user001", "user_name": "张三", "status": 0 },
      { "order": 2, "user_id": "user002", "user_name": "李四", "status": null }
    ],
    "logs": []
  }
}
```

---

#### 47. POST /api/v1/approvals/:id/approve

同意审批。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| comment | string | 否 | 审批意见 |

```json
{
  "comment": "API 设计合理，同意上架"
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 500,
    "status": 1,
    "message": "审批通过，API 已上架"
  }
}
```

---

#### 48. POST /api/v1/approvals/:id/reject

驳回审批（需填写原因）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | string | 是 | 驳回原因 |

```json
{
  "reason": "API 文档缺失，请补充后重新提交"
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 500,
    "status": 2,
    "message": "审批已驳回"
  }
}
```

---

#### 49. POST /api/v1/approvals/:id/cancel

撤销审批。

> **说明**：仅申请人可撤销，且审批未完成时可撤销。

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 500,
    "status": 3,
    "message": "审批已撤销"
  }
}
```

---

#### 50. POST /api/v1/approvals/batch-approve

批量同意审批。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approval_ids | array[long] | 是 | 审批单ID列表 |
| comment | string | 否 | 审批意见（统一填写） |

```json
{
  "approval_ids": [500, 501, 502],
  "comment": "批量审批通过"
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "success_count": 3,
    "failed_count": 0,
    "message": "批量审批通过，共3条"
  }
}
```

---

#### 51. POST /api/v1/approvals/batch-reject

批量驳回审批（需填写原因）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approval_ids | array[long] | 是 | 审批单ID列表 |
| reason | string | 是 | 驳回原因（统一填写） |

```json
{
  "approval_ids": [500, 501, 502],
  "reason": "文档不完整，请补充后重新提交"
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "success_count": 3,
    "failed_count": 0,
    "message": "批量驳回成功，共3条"
  }
}
```

---

### 2.9 Scope 授权管理

#### 52. GET /api/v1/user-authorizations

获取用户授权列表。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_id | string | 否 | 用户ID过滤 |
| app_id | long | 否 | 应用ID过滤 |

**响应示例**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 600,
      "user_id": "user001",
      "user_name": "张三",
      "app_id": 10,
      "app_name": "消息助手",
      "scopes": ["api:im:send-message", "api:im:get-message"],
      "expires_at": "2026-12-31T23:59:59.000Z",
      "create_time": "2026-04-20T10:00:00.000Z"
    }
  ]
}
```

---

#### 53. POST /api/v1/user-authorizations

用户授权（设置有效期）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_id | string | 是 | 用户ID |
| app_id | long | 是 | 应用ID |
| scopes | array | 是 | Scope 列表 |
| expires_at | string | 否 | 过期时间，不填则永久有效 |

```json
{
  "user_id": "user001",
  "app_id": 10,
  "scopes": ["api:im:send-message", "api:im:get-message"],
  "expires_at": "2026-12-31T23:59:59"
}
```

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "id": 600,
    "user_id": "user001",
    "app_id": 10,
    "scopes": ["api:im:send-message", "api:im:get-message"],
    "expires_at": "2026-12-31T23:59:59.000Z"
  }
}
```

---

#### 54. DELETE /api/v1/user-authorizations/:id

取消授权。

**响应示例**：

```json
{
  "code": 0,
  "data": null,
  "message": "授权已取消"
}
```

---

### 2.10 消费网关

#### 55. ANY /gateway/api/*

API 请求代理与鉴权。

> **说明**：三方应用通过此网关调用内部 API。网关验证订阅关系后转发请求。

**请求头**：

| Header | 必填 | 说明 |
|--------|------|------|
| X-App-Id | 是 | 应用ID |
| X-Auth-Type | 是 | 认证类型 |
| Authorization | 是 | 认证凭证（AKSK签名/Bearer Token） |

**处理流程**：

1. 验证应用身份（AKSK/Bearer Token）
2. 查询应用订阅关系
3. 验证请求路径与方法是否在授权 Scope 范围内
4. 转发请求到内部中台网关
5. 返回响应

**响应示例（鉴权失败）**：

```json
{
  "code": 403,
  "message": "应用未订阅该 API 权限"
}
```

---

#### 56. POST /gateway/events/publish

事件发布接口。

> **说明**：业务模块通过此接口发布事件，网关分发至订阅的消费方。
>
> **流程**：提供方 → 内部消息网关 → event-server → 消费方

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| topic | string | 是 | 事件 Topic |
| payload | object | 是 | 事件内容 |

```json
{
  "topic": "im.message.received",
  "payload": {
    "message_id": "msg001",
    "content": "Hello World",
    "sender": "user001"
  }
}
```

**处理流程**：

1. 验证 Topic 对应的事件资源存在且已发布
2. 查询订阅该事件的应用列表
3. 按订阅配置分发事件：
   - WebHook：POST 到 channel_address
   - 企业内部消息队列：推送到对应队列

**响应示例**：

```json
{
  "code": 0,
  "data": {
    "topic": "im.message.received",
    "subscribers": 5,
    "message": "事件已分发至 5 个订阅方"
  }
}
```

---

#### 57. POST /gateway/callbacks/invoke

回调触发接口。

> **说明**：业务模块通过此接口触发回调，网关调用已订阅的消费方回调地址。
>
> **流程**：提供方 → event-server → 消费方（不经内部消息网关）

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| callback_scope | string | 是 | 回调 Scope |
| payload | object | 是 | 回调内容 |

```json
{
  "callback_scope": "callback:approval:completed",
  "payload": {
    "approval_id": "app001",
    "status": "approved",
    "approver": "user001"
  }
}
```

**处理流程**：

1. 验证 callback_scope 对应的回调资源存在
2. 查询订阅该回调的应用列表
3. 按订阅配置调用消费方：
   - WebHook：POST 到 channel_address
   - SSE：推送到 SSE 连接
   - WebSocket：推送到 WebSocket 连接

---

#### 58. GET /gateway/permissions/check

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
    "subscription_id": 300,
    "subscription_status": 1
  }
}
```

**响应示例（未授权）**：

```json
{
  "code": 0,
  "data": {
    "authorized": false,
    "reason": "应用未订阅该权限"
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

### 3.3 通道类型枚举

**事件通道类型**：

| 值 | 说明 |
|-----|------|
| 0 | 企业内部消息队列 |
| 1 | WebHook |

**回调通道类型**：

| 值 | 说明 |
|-----|------|
| 0 | WebHook |
| 1 | SSE |
| 2 | WebSocket |

### 3.4 认证类型枚举

| 值 | 说明 |
|-----|------|
| 0 | 应用类凭证A |
| 1 | 应用类凭证B |
| 2 | AKSK |
| 3 | Bearer Token |