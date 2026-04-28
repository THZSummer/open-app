# 接口设计

**版本**: v1.31  
**最后更新**: 2026-04-29  
**更新说明**: 接口路径从 /api/v1/ 修改为 /service/open/v2/，用于标准环境集成时的路径隔离

> 本文档为 `plan.md` 的子文档，定义能力开放平台的详细接口设计。
> 基于 spec.md 第 3 章 FR 清单编写，确保功能需求完整覆盖。

---

## 0. 接口设计规范

本章节定义能力开放平台接口的命名规范，确保接口的一致性和可维护性。

### 0.1 字段命名规范

**规则**：接口入参和返回值字段统一使用驼峰命名（camelCase）。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `userId` | `user_id` |
| `createTime` | `create_time` |
| `approvalStatus` | `approval_status` |
| `categoryAlias` | `category_alias` |

**命名约定**：
- ID 字段：使用 `Id` 后缀，如 `userId`, `appId`, `categoryId`
- 时间字段：使用 `Time` 后缀，如 `createTime`, `updateTime`，或 `At` 后缀表示时间点，如 `expiresAt`
- 布尔字段：使用 `is` 前缀，如 `isDefault`, `isSubscribed`
- URL 字段：使用 `Url` 后缀，如 `docUrl`, `approvalUrl`

### 0.2 路径命名规范

**规则**：URL 路径使用中划线分隔多个单词（kebab-case）。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `/service/open/v2/user-authorizations` | `/service/open/v2/user_authorizations` |
| `/service/open/v2/approval-flows` | `/service/open/v2/approvalFlows` |
| `/gateway/callbacks/invoke` | `/gateway/callbacks_invoke` |

**命名约定**：
- 资源名称使用复数形式：`/categories`, `/apis`, `/events`
- 子资源使用中划线分隔：`/user-authorizations`, `/approval-flows`
- 路径参数使用驼峰：`/apps/:appId/apis`

### 0.3 数据类型规范

**规则**：长整数（如主键 ID）统一返回 string 类型，避免前端接收精度丢失问题。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `"id": "100"` | `"id": 100` |
| `"appId": "10"` | `"appId": 10` |
| `"permissionId": "200"` | `"permissionId": 200` |

**原因说明**：
- JavaScript 的 `Number` 类型最大安全整数是 `2^53 - 1`（即 `9007199254740991`）
- 当后端使用 `Long` 类型（64 位整数）时，超过安全范围的数值会丢失精度
- 统一使用 `string` 类型可彻底避免此问题，前端需要时自行转换

**适用范围**：
- 所有主键 ID 字段：`id`, `userId`, `appId`, `categoryId`, `permissionId` 等
- 所有外键 ID 字段：`parentId`, `flowId`, `approvalFlowId` 等
- 其他可能超过安全范围的数值型字段

**入参约定**：
- 路径参数中的 ID（如 `/service/open/v2/apis/:id`）可以是数字或字符串，由路由层处理
- 请求体中的 ID 字段统一使用 `string` 类型

### 0.4 通用对象结构

#### category 对象

分类是树形结构，返回时包含完整路径信息：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 分类ID |
| nameCn | string | 分类中文名称 |
| path | string | 分类路径，如 `/1/2/`，用于树形查询优化 |
| categoryPath | array[string] | 完整分类路径名称数组，如 `["A类应用权限", "IM业务"]` |

**示例**：
```json
{
  "category": {
    "id": "2",
    "nameCn": "IM业务",
    "path": "/1/2/",
    "categoryPath": ["A类应用权限", "IM业务"]
  }
}
```

#### 响应格式规范

所有接口统一使用以下响应格式：

| 字段 | 类型 | 说明 |
|------|------|------|
| code | string | 状态码，`"200"` 表示成功，非 `"200"` 表示失败 |
| messageZh | string | 中文提示消息 |
| messageEn | string | 英文提示消息 |
| data | object/array | 业务数据 |
| page | object/null | 分页数据，非列表接口为 null |

**分页数据响应示例**：

```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "100",
      "nameCn": "发送消息"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 123
  }
}
```

**普通数据响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "100",
    "nameCn": "发送消息",
    "status": 1
  },
  "page": null
}
```

**错误响应示例**：

```json
{
  "code": "400",
  "messageZh": "参数错误",
  "messageEn": "Bad Request",
  "data": null,
  "page": null
}
```

#### 分页请求规范

所有列表接口统一支持分页：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| curPage | int | 否 | 当前页码，从 1 开始，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20，最大 100 |

### 0.5 示例对照

```json
// ✅ 符合规范的请求示例
{
  "userId": "user001",
  "appId": "10",
  "permissionIds": ["200", "201"],
  "expiresAt": "2026-12-31T23:59:59"
}

// ❌ 不符合规范的请求示例
{
  "user_id": "user001",      // 应使用 userId
  "app_id": 10,              // 应使用 appId（string 类型）
  "permission_ids": [200, 201], // 应使用 permissionIds（string 类型）
  "expires_at": "2026-12-31T23:59:59" // 应使用 expiresAt
}
```

---

### 0.6 状态枚举定义

#### 订阅状态 (Subscription Status)

用于标识应用对权限资源的订阅状态，适用于 API/事件/回调权限管理。

| 值 | 状态 | 说明 | 可执行操作 |
|----|------|------|-----------|
| 0 | 审核中 | 申请已提交，等待审批 | 撤回申请 |
| 1 | 已授权 | 审批通过，权限生效 | 配置消费参数（事件/回调） |
| 2 | 已拒绝 | 审批驳回 | 重新申请 |
| 3 | 已取消 | 消费方主动撤回 | 重新申请 |

#### 审批状态 (Approval Status)

用于标识审批单的处理状态。

| 值 | 状态 | 说明 |
|----|------|------|
| 0 | 待审批 | 等待审批人处理 |
| 1 | 已通过 | 审批同意 |
| 2 | 已驳回 | 审批拒绝 |
| 3 | 已撤销 | 申请人撤销 |

#### 资源状态 (Resource Status)

用于标识 API/事件/回调资源的生命周期状态。

| 值 | 状态 | 说明 |
|----|------|------|
| 0 | 草稿 | 初始状态或已撤回 |
| 1 | 待审 | 已提交注册，等待审批 |
| 2 | 已发布 | 审批通过，资源上架 |
| 3 | 已下线 | 资源已下线 |

---

## 1. 接口清单

| # | 模块 | Method | Path | 说明 | FR |
|---|------|--------|------|------|-----|
| 1 | **分类管理** | GET | `/service/open/v2/categories` | 获取分类列表（树形） | FR-001 |
| 2 | | GET | `/service/open/v2/categories/:id` | 获取分类详情 | FR-001 |
| 3 | | POST | `/service/open/v2/categories` | 创建分类（一级分类） | FR-001 |
| 4 | | PUT | `/service/open/v2/categories/:id` | 更新分类 | FR-001 |
| 5 | | DELETE | `/service/open/v2/categories/:id` | 删除分类（检查关联资源） | FR-001 |
| 6 | | POST | `/service/open/v2/categories/:id/owners` | 添加分类责任人 | FR-002 |
| 7 | | GET | `/service/open/v2/categories/:id/owners` | 获取分类责任人列表 | FR-002 |
| 8 | | DELETE | `/service/open/v2/categories/:id/owners/:userId` | 移除分类责任人 | FR-002 |
| 9 | **API 管理** | GET | `/service/open/v2/apis` | 获取 API 列表（按分类过滤） | FR-004 |
| 10 | | GET | `/service/open/v2/apis/:id` | 获取 API 详情（含权限信息） | FR-004 |
| 11 | | POST | `/service/open/v2/apis` | 注册 API（附带权限定义） | FR-005 |
| 12 | | PUT | `/service/open/v2/apis/:id` | 更新 API 及权限信息 | FR-006 |
| 13 | | DELETE | `/service/open/v2/apis/:id` | 删除 API（检查订阅关系） | FR-007 |
| 14 | | POST | `/service/open/v2/apis/:id/withdraw` | 撤回审核中的 API | FR-004 |
| 15 | **事件管理** | GET | `/service/open/v2/events` | 获取事件列表（按分类过滤） | FR-008 |
| 16 | | GET | `/service/open/v2/events/:id` | 获取事件详情（含权限信息） | FR-008 |
| 17 | | POST | `/service/open/v2/events` | 注册事件（附带权限定义） | FR-009 |
| 18 | | PUT | `/service/open/v2/events/:id` | 更新事件及权限信息 | FR-010 |
| 19 | | DELETE | `/service/open/v2/events/:id` | 删除事件（检查订阅关系） | FR-011 |
| 20 | | POST | `/service/open/v2/events/:id/withdraw` | 撤回审核中的事件 | FR-008 |
| 21 | **回调管理** | GET | `/service/open/v2/callbacks` | 获取回调列表（按分类过滤） | FR-012 |
| 22 | | GET | `/service/open/v2/callbacks/:id` | 获取回调详情（含权限信息） | FR-012 |
| 23 | | POST | `/service/open/v2/callbacks` | 注册回调（附带权限定义） | FR-013 |
| 24 | | PUT | `/service/open/v2/callbacks/:id` | 更新回调及权限信息 | FR-014 |
| 25 | | DELETE | `/service/open/v2/callbacks/:id` | 删除回调（检查订阅关系） | FR-015 |
| 26 | | POST | `/service/open/v2/callbacks/:id/withdraw` | 撤回审核中的回调 | FR-012 |
| 27 | **API 权限管理** | GET | `/service/open/v2/apps/:appId/apis` | 获取应用 API 权限列表 | FR-016 |
| 28 | | GET | `/service/open/v2/categories/:id/apis` | 获取分类下 API 权限列表 | FR-017 |
| 29 | | POST | `/service/open/v2/apps/:appId/apis/subscribe` | 申请 API 权限（支持批量） | FR-018 |
| 30 | | POST | `/service/open/v2/apps/:appId/apis/:id/withdraw` | 撤回审核中的申请 | FR-016 |
| 31 | | DELETE | `/service/open/v2/apps/:appId/apis/:id` | 删除终态订阅记录 | FR-016 |
| 32 | **事件权限管理** | GET | `/service/open/v2/apps/:appId/events` | 获取应用事件订阅列表 | FR-019 |
| 33 | | GET | `/service/open/v2/categories/:id/events` | 获取分类下事件权限列表 | FR-020 |
| 34 | | POST | `/service/open/v2/apps/:appId/events/subscribe` | 申请事件权限（支持批量） | FR-021 |
| 35 | | PUT | `/service/open/v2/apps/:appId/events/:id/config` | 配置事件消费参数（通道/地址/认证） | FR-019 |
| 36 | | POST | `/service/open/v2/apps/:appId/events/:id/withdraw` | 撤回审核中的申请 | FR-019 |
| 37 | | DELETE | `/service/open/v2/apps/:appId/events/:id` | 删除终态订阅记录 | FR-019 |
| 38 | **回调权限管理** | GET | `/service/open/v2/apps/:appId/callbacks` | 获取应用回调订阅列表 | FR-022 |
| 39 | | GET | `/service/open/v2/categories/:id/callbacks` | 获取分类下回调权限列表 | FR-023 |
| 40 | | POST | `/service/open/v2/apps/:appId/callbacks/subscribe` | 申请回调权限（支持批量） | FR-024 |
| 41 | | PUT | `/service/open/v2/apps/:appId/callbacks/:id/config` | 配置回调消费参数（通道/地址/认证） | FR-022 |
| 42 | | POST | `/service/open/v2/apps/:appId/callbacks/:id/withdraw` | 撤回审核中的申请 | FR-022 |
| 43 | | DELETE | `/service/open/v2/apps/:appId/callbacks/:id` | 删除终态订阅记录 | FR-022 |
| 44 | **审批管理** | GET | `/service/open/v2/approval-flows` | 获取审批流程模板列表 | FR-025 |
| 45 | | GET | `/service/open/v2/approval-flows/:id` | 获取审批流程模板详情 | FR-025 |
| 46 | | POST | `/service/open/v2/approval-flows` | 创建审批流程模板 | FR-025 |
| 47 | | PUT | `/service/open/v2/approval-flows/:id` | 更新审批流程模板 | FR-025 |
| 48 | | DELETE | `/service/open/v2/approval-flows/:id` | 删除审批流程模板 | FR-025 |
| 49 | | GET | `/service/open/v2/approvals/pending` | 获取待审批列表 | FR-026/FR-027 |
| 50 | | GET | `/service/open/v2/approvals/:id` | 获取审批详情 | FR-026/FR-027 |
| 51 | | POST | `/service/open/v2/approvals/:id/approve` | 同意审批 | FR-026/FR-027 |
| 52 | | POST | `/service/open/v2/approvals/:id/reject` | 驳回审批（需填写原因） | FR-026/FR-027 |
| 53 | | POST | `/service/open/v2/approvals/:id/cancel` | 撤销审批 | FR-026/FR-027 |
| 54 | | POST | `/service/open/v2/approvals/batch-approve` | 批量同意审批 | FR-026/FR-027 |
| 55 | | POST | `/service/open/v2/approvals/batch-reject` | 批量驳回审批（需填写原因） | FR-026/FR-027 |
| 56 | **Scope 授权管理** | GET | `/service/open/v2/user-authorizations` | 获取用户授权列表 | FR-031 |
| 57 | | POST | `/service/open/v2/user-authorizations` | 用户授权（设置有效期） | FR-031 |
| 58 | | DELETE | `/service/open/v2/user-authorizations/:id` | 取消授权 | FR-031 |
| 59 | **消费网关** | ANY | `/gateway/api/*` | API 请求代理与鉴权 | FR-028 |
| 60 | | POST | `/gateway/events/publish` | 事件发布接口 | FR-029 |
| 61 | | POST | `/gateway/callbacks/invoke` | 回调触发接口 | FR-030 |
| 62 | | GET | `/gateway/permissions/check` | 权限校验接口 | FR-028/029/030 |
| 63 | | POST | `/gateway/callbacks/config` | 回调配置查询接口（内部） | FR-030 |

> **接口统计**：共 63 个接口，覆盖 FR-001 ~ FR-031
>
> **权限树设计说明**：采用懒加载模式，分为两个步骤：
> 1. 查树：`GET /service/open/v2/categories` 获取分类树
> 2. 查权限：`GET /service/open/v2/categories/:id/apis` 获取某分类下的权限列表

---

## 2. 接口详情

### 2.1 分类管理

#### 1. GET /service/open/v2/categories

获取分类列表（树形结构）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_alias | string | 否 | 分类别名过滤，用于获取指定权限树 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "1",
      "categoryAlias": "app_type_a",
      "nameCn": "A类应用权限",
      "nameEn": "App Type A Permissions",
      "parentId": null,
      "path": "/1/",
      "sortOrder": 0,
      "status": 1,
      "children": [
        {
          "id": "2",
          "categoryAlias": null,
          "nameCn": "IM 业务",
          "nameEn": "IM Business",
          "parentId": "1",
          "path": "/1/2/",
          "sortOrder": 0,
          "status": 1,
          "children": [],
  "page": null
}
      ]
    }
  ]
}
```

---

#### 2. GET /service/open/v2/categories/:id

获取分类详情。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "2",
    "categoryAlias": null,
    "nameCn": "IM 业务",
    "nameEn": "IM Business",
    "parentId": "1",
    "path": "/1/2/",
    "sortOrder": 0,
    "status": 1,
    "createTime": "2026-04-20T10:00:00.000Z",
    "createBy": "admin"
  },
  "page": null
}
```

---

#### 3. POST /service/open/v2/categories

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
  "categoryAlias": "app_type_a",
  "nameCn": "A类应用权限",
  "nameEn": "App Type A Permissions",
  "parentId": null,
  "sortOrder": 0
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "1",
    "categoryAlias": "app_type_a",
    "nameCn": "A类应用权限",
    "nameEn": "App Type A Permissions",
    "parentId": null,
    "path": "/1/",
    "sortOrder": 0,
    "status": 1
  },
  "page": null
}
```

---

#### 4. PUT /service/open/v2/categories/:id

更新分类。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name_cn | string | 是 | 中文名称 |
| name_en | string | 是 | 英文名称 |
| sort_order | int | 否 | 排序序号 |

```json
{
  "nameCn": "IM 业务能力",
  "nameEn": "IM Business Capability",
  "sortOrder": 1
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "2",
    "nameCn": "IM 业务能力",
    "nameEn": "IM Business Capability",
    "sortOrder": 1
  },
  "page": null
}
```

---

#### 5. DELETE /service/open/v2/categories/:id

删除分类（检查关联资源）。

> **说明**：删除前检查是否存在关联的 API/事件/回调资源。若存在关联资源，返回错误提示。

**响应示例（成功）**：

```json
{
  "code": "200",
  "messageZh": "分类删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

**响应示例（失败-存在关联资源）**：

```json
{
  "code": "409",
  "message": "分类下存在 5 个 API 资源，无法删除",
  "data": null,
  "page": null
}
```

---

#### 6. POST /service/open/v2/categories/:id/owners

添加分类责任人。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_id | string | 是 | 用户ID |
| user_name | string | 否 | 用户名称（用于展示） |

```json
{
  "userId": "user001",
  "userName": "张三"
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "100",
    "categoryId": "2",
    "userId": "user001",
    "userName": "张三"
  },
  "page": null
}
```

---

#### 7. GET /service/open/v2/categories/:id/owners

获取分类责任人列表。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "100",
      "categoryId": "2",
      "userId": "user001",
      "userName": "张三"
    },
    {
      "id": "101",
      "categoryId": "2",
      "userId": "user002",
      "userName": "李四"
    }
  ],
  "page": null
}
```

---

#### 8. DELETE /service/open/v2/categories/:id/owners/:userId

移除分类责任人。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "责任人移除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

---

### 2.2 API 管理

#### 9. GET /service/open/v2/apis

获取 API 列表（按分类过滤）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_id | long | 否 | 分类ID过滤 |
| status | int | 否 | 状态过滤（0=草稿, 1=待审, 2=已发布, 3=已下线） |
| keyword | string | 否 | 搜索关键词（名称、Scope） |
| curPage | int | 否 | 当前页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
      {
        "id": "100",
        "nameCn": "发送消息",
        "nameEn": "Send Message",
        "path": "/service/open/v2/messages",
        "method": "POST",
        "authType": 1,
        "categoryId": "2",
        "categoryName": "IM 业务",
        "status": 2,
        "permission": {
          "id": "200",
          "scope": "api:im:send-message",
          "status": 1
        }
      }
    
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 50
  }
}
```

---

#### 10. GET /service/open/v2/apis/:id

获取 API 详情（含权限信息及属性）。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "100",
    "nameCn": "发送消息",
    "nameEn": "Send Message",
    "path": "/service/open/v2/messages",
    "method": "POST",
    "authType": 1,
    "categoryId": "2",
    "status": 2,
    "createTime": "2026-04-20T10:00:00.000Z",
    "createBy": "user001",
    "permission": {
      "id": "200",
      "nameCn": "发送消息权限",
      "nameEn": "Send Message Permission",
      "scope": "api:im:send-message",
      "needApproval": 1,
      "resourceNodes": [
        { "type": "approver", "userId": "api_admin", "userName": "API管理员", "order": 1 },
        { "type": "approver", "userId": "security_admin", "userName": "安全管理员", "order": 2 }
      ],
      "status": 1
    },
    "properties": [
      { "propertyName": "descriptionCn", "propertyValue": "发送消息API的中文描述" },
      { "propertyName": "descriptionEn", "propertyValue": "Send message API description" },
      { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/api/send-message" }
    ]
  },
  "page": null
}
```

---

#### 11. POST /service/open/v2/apis

注册 API（附带权限定义）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name_cn | string | 是 | 中文名称 |
| name_en | string | 是 | 英文名称 |
| path | string | 是 | API 路径 |
| method | string | 是 | HTTP 方法（GET/POST/PUT/DELETE） |
| auth_type | int | 否 | 认证方式: 0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN，默认 1 |
| category_id | long | 是 | 所属分类ID |
| permission | object | 是 | 权限定义 |
| properties | array | 否 | 扩展属性列表 |

**permission 对象**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name_cn | string | 是 | 权限中文名称 |
| name_en | string | 是 | 权限英文名称 |
| scope | string | 是 | Scope标识，格式 `api:{模块}:{资源标识}` |
| need_approval | int | 否 | 是否需要审批：0=否, 1=是，默认 1 |
| resource_nodes | array | 否 | 资源级审批节点数组（JSON数组，存储为VARCHAR字符串） |

**resource_nodes 数组元素**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 节点类型（approver=审批人） |
| user_id | string | 是 | 审批人ID |
| user_name | string | 是 | 审批人姓名 |
| order | int | 是 | 节点顺序 |

```json
{
  "nameCn": "发送消息",
  "nameEn": "Send Message",
  "path": "/service/open/v2/messages",
  "method": "POST",
  "authType": 1,
  "categoryId": "2",
  "permission": {
    "nameCn": "发送消息权限",
    "nameEn": "Send Message Permission",
    "scope": "api:im:send-message",
    "needApproval": 1,
    "resourceNodes": [
      { "type": "approver", "userId": "api_admin", "userName": "API管理员", "order": 1 },
      { "type": "approver", "userId": "security_admin", "userName": "安全管理员", "order": 2 }
    ]
  },
  "properties": [
    { "propertyName": "descriptionCn", "propertyValue": "发送消息API的中文描述" },
    { "propertyName": "descriptionEn", "propertyValue": "Send message API description" },
    { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/api/send-message" }
  ]
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "100",
    "nameCn": "发送消息",
    "nameEn": "Send Message",
    "path": "/service/open/v2/messages",
    "method": "POST",
    "authType": 1,
    "status": 1,
    "permission": {
      "id": "200",
      "scope": "api:im:send-message",
      "status": 1
    }
  },
  "messageZh": "API 注册成功，等待审批",
  "messageEn": "Error"
}
```

---

#### 12. PUT /service/open/v2/apis/:id

更新 API 及权限信息。

> **说明**：核心属性（path、method、scope）变更需重新审批。

**请求体**：

```json
{
  "nameCn": "发送消息V2",
  "nameEn": "Send Message V2",
  "authType": 2,
  "categoryId": "2",
  "permission": {
    "nameCn": "发送消息权限V2",
    "nameEn": "Send Message Permission V2"
  },
  "properties": [
    { "propertyName": "descriptionCn", "propertyValue": "发送消息API的中文描述（更新）" }
  ]
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "100",
    "nameCn": "发送消息V2",
    "authType": 2,
    "status": 1,
    "message": "API 更新成功，核心属性变更需重新审批"
  },
  "page": null
}
```

---

#### 13. DELETE /service/open/v2/apis/:id

删除 API（检查订阅关系）。

> **说明**：已订阅的 API 无法删除，需先取消所有订阅。

**响应示例（成功）**：

```json
{
  "code": "200",
  "messageZh": "API 删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

**响应示例（失败-存在订阅）**：

```json
{
  "code": "409",
  "message": "API 被 3 个应用订阅，无法删除",
  "data": null,
  "page": null
}
```

---

#### 14. POST /service/open/v2/apis/:id/withdraw

撤回审核中的 API。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "100",
    "status": 0,
    "message": "API 已撤回，状态变为草稿"
  },
  "page": null
}
```

---

### 2.3 事件管理

#### 15. GET /service/open/v2/events

获取事件列表（按分类过滤）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_id | long | 否 | 分类ID过滤 |
| status | int | 否 | 状态过滤 |
| keyword | string | 否 | 搜索关键词（名称、Scope、Topic） |
| curPage | int | 否 | 当前页码 |
| pageSize | int | 否 | 每页数量 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "total": 30,
    "list": [
      {
        "id": "101",
        "nameCn": "消息接收事件",
        "nameEn": "Message Received Event",
        "topic": "im.message.received",
        "categoryId": "2",
        "status": 2,
        "permission": {
          "id": "201",
          "scope": "event:im:message-received"
        },
  "page": null
}
    ]
  }
}
```

---

#### 16. GET /service/open/v2/events/:id

获取事件详情（含权限信息及属性）。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "101",
    "nameCn": "消息接收事件",
    "nameEn": "Message Received Event",
    "topic": "im.message.received",
    "categoryId": "2",
    "status": 2,
    "permission": {
      "id": "201",
      "nameCn": "消息接收权限",
      "nameEn": "Message Received Permission",
      "scope": "event:im:message-received",
      "status": 1
    },
    "properties": [
      { "propertyName": "descriptionCn", "propertyValue": "消息接收事件描述" },
      { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/event/message-received" }
    ]
  },
  "page": null
}
```

---

#### 17. POST /service/open/v2/events

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
  "nameCn": "消息接收事件",
  "nameEn": "Message Received Event",
  "topic": "im.message.received",
  "categoryId": "2",
  "permission": {
    "nameCn": "消息接收权限",
    "nameEn": "Message Received Permission",
    "scope": "event:im:message-received"
  },
  "properties": [
    { "propertyName": "descriptionCn", "propertyValue": "消息接收事件描述" },
    { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/event/message-received" }
  ]
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "101",
    "nameCn": "消息接收事件",
    "topic": "im.message.received",
    "status": 1,
    "permission": {
      "id": "201",
      "scope": "event:im:message-received"
    },
  "page": null
},
  "messageZh": "事件注册成功，等待审批",
  "messageEn": "Error"
}
```

---

#### 18. PUT /service/open/v2/events/:id

更新事件及权限信息。

**请求体**：

```json
{
  "nameCn": "消息接收事件V2",
  "categoryId": "2",
  "permission": {
    "nameCn": "消息接收权限V2"
  }
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "101",
    "nameCn": "消息接收事件V2",
    "status": 1,
    "message": "事件更新成功，核心属性变更需重新审批"
  },
  "page": null
}
```

---

#### 19. DELETE /service/open/v2/events/:id

删除事件（检查订阅关系）。

> **说明**：已订阅的事件无法删除，需先取消所有订阅。

**响应示例（成功）**：

```json
{
  "code": "200",
  "messageZh": "事件删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

**响应示例（失败-存在订阅）**：

```json
{
  "code": "409",
  "message": "事件被 3 个应用订阅，无法删除",
  "data": null,
  "page": null
}
```

---

#### 20. POST /service/open/v2/events/:id/withdraw

撤回审核中的事件。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "101",
    "status": 0,
    "message": "事件已撤回"
  },
  "page": null
}
```

---

### 2.4 回调管理

#### 21. GET /service/open/v2/callbacks

获取回调列表（按分类过滤）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_id | long | 否 | 分类ID过滤 |
| status | int | 否 | 状态过滤 |
| keyword | string | 否 | 搜索关键词 |
| curPage | int | 否 | 当前页码 |
| pageSize | int | 否 | 每页数量 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "total": 20,
    "list": [
      {
        "id": "102",
        "nameCn": "审批完成回调",
        "nameEn": "Approval Completed Callback",
        "categoryId": "3",
        "status": 2,
        "permission": {
          "id": "202",
          "scope": "callback:approval:completed"
        },
  "page": null
}
    ]
  }
}
```

---

#### 22. GET /service/open/v2/callbacks/:id

获取回调详情（含权限信息及属性）。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "102",
    "nameCn": "审批完成回调",
    "nameEn": "Approval Completed Callback",
    "categoryId": "3",
    "status": 2,
    "permission": {
      "id": "202",
      "nameCn": "审批完成回调权限",
      "nameEn": "Approval Completed Callback Permission",
      "scope": "callback:approval:completed",
      "status": 1
    },
    "properties": [
      { "propertyName": "descriptionCn", "propertyValue": "审批完成后的回调通知" },
      { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/callback/approval-completed" }
    ]
  },
  "page": null
}
```

---

#### 23. POST /service/open/v2/callbacks

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
  "nameCn": "审批完成回调",
  "nameEn": "Approval Completed Callback",
  "categoryId": "3",
  "permission": {
    "nameCn": "审批完成回调权限",
    "nameEn": "Approval Completed Callback Permission",
    "scope": "callback:approval:completed"
  },
  "properties": [
    { "propertyName": "descriptionCn", "propertyValue": "审批完成后的回调通知" },
    { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/callback/approval-completed" }
  ]
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "102",
    "nameCn": "审批完成回调",
    "status": 1,
    "permission": {
      "id": "202",
      "scope": "callback:approval:completed"
    },
  "page": null
},
  "messageZh": "回调注册成功，等待审批",
  "messageEn": "Error"
}
```

---

#### 24. PUT /service/open/v2/callbacks/:id

更新回调及权限信息。

**请求体**：

```json
{
  "nameCn": "审批完成回调V2",
  "categoryId": "3",
  "permission": {
    "nameCn": "审批完成回调权限V2"
  }
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "102",
    "nameCn": "审批完成回调V2",
    "status": 1,
    "message": "回调更新成功，核心属性变更需重新审批"
  },
  "page": null
}
```

---

#### 25. DELETE /service/open/v2/callbacks/:id

删除回调（检查订阅关系）。

> **说明**：已订阅的回调无法删除，需先取消所有订阅。

**响应示例（成功）**：

```json
{
  "code": "200",
  "messageZh": "回调删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

**响应示例（失败-存在订阅）**：

```json
{
  "code": "409",
  "message": "回调被 2 个应用订阅，无法删除",
  "data": null,
  "page": null
}
```

---

#### 26. POST /service/open/v2/callbacks/:id/withdraw

撤回审核中的回调。

> **说明**：仅状态为"待审"的回调可撤回。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "102",
    "status": 0,
    "message": "回调已撤回，状态变为草稿"
  },
  "page": null
}
```

---

### 2.5 API 权限管理

#### 27. GET /service/open/v2/apps/:appId/apis

获取应用 API 权限列表。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 否 | 订阅状态过滤（0=待审, 1=已授权, 2=已拒绝, 3=已取消） |
| keyword | string | 否 | 搜索关键词（权限名称、Scope） |
| curPage | int | 否 | 当前页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
      {
        "id": "300",
        "appId": "10",
        "permissionId": "200",
        "permission": {
          "nameCn": "发送消息权限",
          "scope": "api:im:send-message"
        },
        "api": {
          "path": "/service/open/v2/messages",
          "method": "POST",
          "authType": 1,
          "docUrl": "https://docs.example.com/api/send-message"
        },
        "category": {
          "id": "2",
          "nameCn": "IM业务",
          "path": "/1/2/",
          "categoryPath": ["A类应用权限", "IM业务"]
        },
        "approver": {
          "userId": "user001",
          "userName": "张三"
        },
        "status": 1,
        "authType": 0,
        "approvalUrl": "https://platform.example.com/approval/300",
        "createTime": "2026-04-20T10:00:00.000Z"
      }
    
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 50
  }
}
```

---

#### 28. GET /service/open/v2/categories/:id/apis

获取分类下 API 权限列表。

> **说明**：权限树采用懒加载模式。分类树通过 `GET /service/open/v2/categories` 获取，点击分类节点时调用此接口获取该分类下的权限列表。
> - `include_children=true`（默认）：递归获取当前分类及所有子分类下的权限（通过 `path` 字段优化查询）
> - `include_children=false`：仅获取当前分类直接关联的权限
> - 传根分类ID + `include_children=true` 可获取全量权限

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词（名称、Scope） |
| need_approval | int | 否 | 是否需要审核过滤（0=不需要审核, 1=需要审核） |
| include_children | boolean | 否 | 是否包含子分类权限（默认 true，递归获取） |
| curPage | int | 否 | 当前页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
      {
        "id": "200",
        "nameCn": "发送消息权限",
        "nameEn": "Send Message Permission",
        "scope": "api:im:send-message",
        "status": 1,
        "needApproval": 1,
        "isSubscribed": 1,
        "api": {
          "path": "/service/open/v2/messages",
          "method": "POST",
          "authType": 1,
          "docUrl": "https://docs.example.com/api/send-message"
        }
      }
    
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 30
  }
}
```

---

#### 29. POST /service/open/v2/apps/:appId/apis/subscribe

申请 API 权限（支持批量）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| permission_ids | array[long] | 是 | 权限ID列表（支持批量提交） |

```json
{
  "permissionIds": ["200", "201", "202"]
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "records": [
      { "id": "300", "appId": "10", "permissionId": "200", "status": 0 },
      { "id": "301", "appId": "10", "permissionId": "201", "status": 0 },
      { "id": "302", "appId": "10", "permissionId": "202", "status": 0 }
    ],
    "messageZh": "申请已提交，共3条，等待审批",
  "messageEn": "Error"
  },
  "page": null
}
```

---

#### 30. POST /service/open/v2/apps/:appId/apis/:id/withdraw

撤回审核中的 API 权限申请。

> **说明**：仅状态为"待审"的申请可撤回。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "300",
    "status": 3,
    "message": "申请已撤回"
  },
  "page": null
}
```

---

#### 31. DELETE /service/open/v2/apps/:appId/apis/:id

删除 API 权限订阅记录。

> **说明**：仅允许删除终态记录（approved/rejected/revoked），申请中状态需使用撤回接口。审批通过状态删除时，系统自动取消已授权的 API 权限。

**删除规则**：

| 订阅状态 | 是否允许删除 | 说明 |
|---------|-------------|------|
| `0`（审核中） | ❌ 不允许 | 应使用"撤回"接口 |
| `1`（已授权） | ✅ 允许 | 系统自动取消权限 |
| `2`（已拒绝） | ✅ 允许 | 删除后可重新申请 |
| `3`（已取消） | ✅ 允许 | 删除后可重新申请 |

**响应示例（成功）**：

```json
{
  "code": "200",
  "messageZh": "订阅记录删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

**响应示例（失败-状态不允许删除）**：

```json
{
  "code": "400",
  "messageZh": "审核中的申请不支持删除，请使用撤回接口",
  "messageEn": "Bad Request",
  "data": null,
  "page": null
}
```

**响应示例（失败-记录不存在）**：

```json
{
  "code": "404",
  "messageZh": "订阅记录不存在",
  "messageEn": "Not Found",
  "data": null,
  "page": null
}
```

---

### 2.6 事件权限管理

#### 32. GET /service/open/v2/apps/:appId/events

获取应用事件订阅列表。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 否 | 订阅状态过滤 |
| keyword | string | 否 | 搜索关键词（权限名称、Scope、Topic） |
| curPage | int | 否 | 当前页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
      {
        "id": "301",
        "appId": "10",
        "permissionId": "201",
        "permission": {
          "nameCn": "消息接收权限",
          "scope": "event:im:message-received",
          "docUrl": "https://docs.example.com/event/message-received"
        },
        "event": {
          "topic": "im.message.received",
          "docUrl": "https://docs.example.com/event/message-received"
        },
        "category": {
          "id": "2",
          "nameCn": "IM业务",
          "path": "/1/2/",
          "categoryPath": ["A类应用权限", "IM业务"]
        },
        "approver": {
          "userId": "user001",
          "userName": "张三"
        },
        "status": 1,
        "approvalUrl": "https://platform.example.com/approval/301",
        "channelType": 1,
        "channelAddress": "https://webhook.example.com/events",
        "authType": 0,
        "createTime": "2026-04-20T10:00:00.000Z"
      }
    
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 30
  }
}
```

---

#### 33. GET /service/open/v2/categories/:id/events

获取分类下事件权限列表。

> **说明**：权限树懒加载，点击分类节点时调用。
> - `include_children=true`（默认）：递归获取当前分类及所有子分类下的权限（通过 `path` 字段优化查询）
> - `include_children=false`：仅获取当前分类直接关联的权限
> - 传根分类ID + `include_children=true` 可获取全量权限

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词 |
| need_approval | int | 否 | 是否需要审核过滤（0=不需要审核, 1=需要审核） |
| include_children | boolean | 否 | 是否包含子分类权限（默认 true，递归获取） |
| curPage | int | 否 | 当前页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
      {
        "id": "201",
        "nameCn": "消息接收权限",
        "nameEn": "Message Received Permission",
        "scope": "event:im:message-received",
        "status": 1,
        "needApproval": 1,
        "isSubscribed": 1,
        "event": {
          "topic": "im.message.received",
          "docUrl": "https://docs.example.com/event/message-received"
        }
      }
    
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 20
  }
}
```

---

#### 34. POST /service/open/v2/apps/:appId/events/subscribe

申请事件权限（支持批量）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| permission_ids | array[long] | 是 | 权限ID列表（支持批量提交） |

```json
{
  "permissionIds": ["201", "202", "203"]
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "records": [
      { "id": "301", "appId": "10", "permissionId": "201", "status": 0 },
      { "id": "302", "appId": "10", "permissionId": "202", "status": 0 },
      { "id": "303", "appId": "10", "permissionId": "203", "status": 0 }
    ],
    "messageZh": "申请已提交，共3条，等待审批",
  "messageEn": "Error"
  },
  "page": null
}
```

---

#### 35. PUT /service/open/v2/apps/:appId/events/:id/config

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
  "channelType": 1,
  "channelAddress": "https://webhook.example.com/events",
  "authType": 0
}
```

**枚举值说明**：

| 字段 | 值 | 说明 |
|------|-----|------|
| channel_type | 0 | 企业内部消息队列 |
| channel_type | 1 | WebHook |
| auth_type | 0 | Cookie |
| auth_type | 1 | SOA |
| auth_type | 2 | APIG |
| auth_type | 3 | IAM |
| auth_type | 4 | 免认证 |
| auth_type | 5 | AKSK |
| auth_type | 6 | CLITOKEN |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "301",
    "channelType": 1,
    "channelAddress": "https://webhook.example.com/events",
    "authType": 0,
    "message": "事件消费参数配置成功"
  },
  "page": null
}
```

---

#### 36. POST /service/open/v2/apps/:appId/events/:id/withdraw

撤回审核中的事件权限申请。

> **说明**：仅状态为"待审"的申请可撤回。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "301",
    "status": 3,
    "message": "申请已撤回"
  },
  "page": null
}
```

---

#### 37. DELETE /service/open/v2/apps/:appId/events/:id

删除事件权限订阅记录。

> **说明**：仅允许删除终态记录（approved/rejected/revoked），申请中状态需使用撤回接口。审批通过状态删除时，系统自动取消事件订阅。

**删除规则**：

| 订阅状态 | 是否允许删除 | 说明 |
|---------|-------------|------|
| `0`（审核中） | ❌ 不允许 | 应使用"撤回"接口 |
| `1`（已授权） | ✅ 允许 | 系统自动取消订阅 |
| `2`（已拒绝） | ✅ 允许 | 删除后可重新申请 |
| `3`（已取消） | ✅ 允许 | 删除后可重新申请 |

**响应示例（成功）**：

```json
{
  "code": "200",
  "messageZh": "订阅记录删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

**响应示例（失败-状态不允许删除）**：

```json
{
  "code": "400",
  "messageZh": "审核中的申请不支持删除，请使用撤回接口",
  "messageEn": "Bad Request",
  "data": null,
  "page": null
}
```

---

### 2.7 回调权限管理

#### 38. GET /service/open/v2/apps/:appId/callbacks

获取应用回调订阅列表。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 否 | 订阅状态过滤 |
| keyword | string | 否 | 搜索关键词（权限名称、Scope） |
| curPage | int | 否 | 当前页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
      {
        "id": "302",
        "appId": "10",
        "permissionId": "202",
        "permission": {
          "nameCn": "审批完成回调权限",
          "scope": "callback:approval:completed"
        },
        "category": {
          "id": "3",
          "nameCn": "审批回调",
          "path": "/1/3/",
          "categoryPath": ["A类应用权限", "审批回调"]
        },
        "status": 1,
        "channelType": 1,
        "channelAddress": "https://webhook.example.com/callbacks",
        "authType": 0,
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

---

#### 39. GET /service/open/v2/categories/:id/callbacks

获取分类下回调权限列表。

> **说明**：权限树懒加载，点击分类节点时调用。
> - `include_children=true`（默认）：递归获取当前分类及所有子分类下的权限（通过 `path` 字段优化查询）
> - `include_children=false`：仅获取当前分类直接关联的权限
> - 传根分类ID + `include_children=true` 可获取全量权限

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词 |
| need_approval | int | 否 | 是否需要审核过滤（0=不需要审核, 1=需要审核） |
| include_children | boolean | 否 | 是否包含子分类权限（默认 true，递归获取） |
| curPage | int | 否 | 当前页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
      {
        "id": "202",
        "nameCn": "审批完成回调权限",
        "nameEn": "Approval Completed Callback Permission",
        "scope": "callback:approval:completed",
        "status": 1,
        "needApproval": 1,
        "isSubscribed": 1,
        "callback": {
          "docUrl": "https://docs.example.com/callback/approval-completed"
        }
      }
    
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 15
  }
}
```
```

---

#### 40. POST /service/open/v2/apps/:appId/callbacks/subscribe

申请回调权限（支持批量）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| permission_ids | array[long] | 是 | 权限ID列表（支持批量提交） |

```json
{
  "permissionIds": ["202", "203", "204"]
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "records": [
      { "id": "302", "appId": "10", "permissionId": "202", "status": 0 },
      { "id": "303", "appId": "10", "permissionId": "203", "status": 0 },
      { "id": "304", "appId": "10", "permissionId": "204", "status": 0 }
    ],
    "messageZh": "申请已提交，共3条，等待审批",
  "messageEn": "Error"
  },
  "page": null
}
```

---

#### 41. PUT /service/open/v2/apps/:appId/callbacks/:id/config

配置回调消费参数。

> **说明**：权限审批通过后，消费方需配置回调接收方式。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| channel_type | int | 是 | 通道类型（1=WebHook, 2=SSE, 3=WebSocket） |
| channel_address | string | 条件 | 通道地址 |
| auth_type | int | 是 | 认证类型 |

```json
{
  "channelType": 1,
  "channelAddress": "https://webhook.example.com/callbacks",
  "authType": 0
}
```

**枚举值说明**：

| 字段 | 值 | 说明 |
|------|-----|------|
| channel_type | 1 | WebHook |
| channel_type | 2 | SSE |
| channel_type | 3 | WebSocket |

---

#### 42. POST /service/open/v2/apps/:appId/callbacks/:id/withdraw

撤回审核中的回调权限申请。

> **说明**：仅状态为"待审"的申请可撤回。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "302",
    "status": 3,
    "message": "申请已撤回"
  },
  "page": null
}
```

#### 43. DELETE /service/open/v2/apps/:appId/callbacks/:id

删除回调权限订阅记录。

> **说明**：仅允许删除终态记录（approved/rejected/revoked），申请中状态需使用撤回接口。审批通过状态删除时，系统自动取消回调订阅。

**删除规则**：

| 订阅状态 | 是否允许删除 | 说明 |
|---------|-------------|------|
| `0`（审核中） | ❌ 不允许 | 应使用"撤回"接口 |
| `1`（已授权） | ✅ 允许 | 系统自动取消订阅 |
| `2`（已拒绝） | ✅ 允许 | 删除后可重新申请 |
| `3`（已取消） | ✅ 允许 | 删除后可重新申请 |

**响应示例（成功）**：

```json
{
  "code": "200",
  "messageZh": "订阅记录删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

**响应示例（失败-状态不允许删除）**：

```json
{
  "code": "400",
  "messageZh": "审核中的申请不支持删除，请使用撤回接口",
  "messageEn": "Bad Request",
  "data": null,
  "page": null
}
```

---

### 2.8 审批管理

#### 44. GET /service/open/v2/approval-flows

获取审批流程模板列表。

**权限要求**：
- 全局审批流程配置：需要超级管理员权限
- 场景审批流程配置：需要平台运营管理员权限
- 查询审批流程列表：所有用户都可以查询

**配置界面**：
- 全局审批流程：平台管理后台 → 审批流程管理 → 全局审批流程
- 场景审批流程：平台管理后台 → 审批流程管理 → 场景审批流程列表

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词（名称、编码） |
| curPage | int | 否 | 当前页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
      {
        "id": "1",
        "nameCn": "全局审批流",
        "nameEn": "Global Approval Flow",
        "code": "global",
        "descriptionCn": "所有申请的平台级审批流程",
        "descriptionEn": "Platform-level approval flow for all applications",
        "status": 1,
        "createTime": "2026-04-20T10:00:00.000Z"
      },
      {
        "id": "2",
        "nameCn": "API权限申请审批流",
        "nameEn": "API Permission Apply Approval Flow",
        "code": "api_permission_apply",
        "descriptionCn": "API权限申请场景审批流程",
        "descriptionEn": "Approval flow for API permission apply",
        "status": 1,
        "createTime": "2026-04-20T10:00:00.000Z"
      }
    
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 5
  }
}
```

---

#### 45. GET /service/open/v2/approval-flows/:id

获取审批流程模板详情。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "1",
    "nameCn": "全局审批流",
    "nameEn": "Global Approval Flow",
    "code": "global",
    "descriptionCn": "所有申请的平台级审批流程",
    "descriptionEn": "Platform-level approval flow for all applications",
    "status": 1,
    "nodes": [
      { "type": "approver", "userId": "admin001", "userName": "系统管理员", "order": 1 },
      { "type": "approver", "userId": "admin002", "userName": "平台管理员", "order": 2 }
    ],
    "createTime": "2026-04-20T10:00:00.000Z",
    "createBy": "system"
  },
  "page": null
}
```

---

#### 46. POST /service/open/v2/approval-flows

创建审批流程模板。

**权限要求**：
- 全局审批流程配置：需要超级管理员权限
- 场景审批流程配置：需要平台运营管理员权限

**配置界面**：
- 全局审批流程：平台管理后台 → 审批流程管理 → 全局审批流程
- 场景审批流程：平台管理后台 → 审批流程管理 → 场景审批流程列表

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name_cn | string | 是 | 中文名称 |
| name_en | string | 是 | 英文名称 |
| code | string | 是 | 流程编码：global=全局审批, api_register=API注册审批, event_register=事件注册审批, callback_register=回调注册审批, api_permission_apply=API权限申请审批, event_permission_apply=事件权限申请审批, callback_permission_apply=回调权限申请审批 |
| description_cn | string | 否 | 中文描述 |
| description_en | string | 否 | 英文描述 |
| nodes | array | 是 | 审批节点列表（JSON数组，存储为VARCHAR字符串） |

**nodes 数组元素**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 节点类型（approver=审批人） |
| user_id | string | 是 | 审批人ID |
| user_name | string | 是 | 审批人姓名 |
| order | int | 是 | 节点顺序 |

```json
{
  "nameCn": "API权限申请审批流",
  "nameEn": "API Permission Apply Approval Flow",
  "code": "api_permission_apply",
  "descriptionCn": "API权限申请场景审批流程",
  "descriptionEn": "Approval flow for API permission apply",
  "nodes": [
    { "type": "approver", "userId": "perm_admin", "userName": "权限管理员", "order": 1 }
  ]
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "3",
    "nameCn": "API权限申请审批流",
    "nameEn": "API Permission Apply Approval Flow",
    "code": "api_permission_apply",
    "status": 1
  },
  "page": null
}
```

---

#### 47. PUT /service/open/v2/approval-flows/:id

更新审批流程模板。

**权限要求**：
- 全局审批流程配置：需要超级管理员权限
- 场景审批流程配置：需要平台运营管理员权限

**配置界面**：
- 全局审批流程：平台管理后台 → 审批流程管理 → 全局审批流程
- 场景审批流程：平台管理后台 → 审批流程管理 → 场景审批流程列表

**请求体**：

```json
{
  "nameCn": "权限申请审批流V2",
  "descriptionCn": "权限申请场景审批流程（更新）",
  "nodes": [
    { "type": "approver", "userId": "perm_admin", "userName": "权限管理员", "order": 1 },
    { "type": "approver", "userId": "security_admin", "userName": "安全管理员", "order": 2 }
  ]
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "3",
    "nameCn": "权限申请审批流V2",
    "status": 1,
    "message": "审批流程模板更新成功"
  },
  "page": null
}
```

---

#### 48. DELETE /service/open/v2/approval-flows/:id

删除审批流程模板。

**权限要求**：
- 全局审批流程配置：需要超级管理员权限
- 场景审批流程配置：需要平台运营管理员权限

**配置界面**：
- 全局审批流程：平台管理后台 → 审批流程管理 → 全局审批流程
- 场景审批流程：平台管理后台 → 审批流程管理 → 场景审批流程列表

**响应示例（成功）**：

```json
{
  "code": "200",
  "messageZh": "审批流程模板删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

**响应示例（失败-流程不存在）**：

```json
{
  "code": "404",
  "messageZh": "审批流程模板不存在",
  "messageEn": "Not Found",
  "data": null,
  "page": null
}
```

---

#### 49. GET /service/open/v2/approvals/pending

获取待审批列表。

> **说明**：返回当前用户待处理的审批单。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 审批类型（resource_register=资源注册, api_permission_apply=API权限申请, event_permission_apply=事件权限申请, callback_permission_apply=回调权限申请） |
| keyword | string | 否 | 搜索关键词（业务名称、申请人） |
| curPage | int | 否 | 当前页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
      {
        "id": "500",
        "type": "resource_register",
        "businessType": "api",
        "businessId": "100",
        "businessName": "发送消息",
        "applicantId": "user003",
        "applicantName": "王五",
        "status": 0,
        "currentNode": 1,
        "createTime": "2026-04-20T10:00:00.000Z"
      }
    
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 10
  }
}
```

---

#### 50. GET /service/open/v2/approvals/:id

获取审批详情。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "500",
    "type": "resource_register",
    "businessType": "api",
    "businessId": "100",
    "businessData": {
      "nameCn": "发送消息",
      "path": "/service/open/v2/messages",
      "method": "POST"
    },
    "applicantId": "user003",
    "applicantName": "王五",
    "status": 0,
    "currentNode": 1,
    "combinedNodes": [
      { 
        "type": "approver", 
        "userId": "user001", 
        "userName": "张三", 
        "order": 1, 
        "level": "scene",
        "status": 0 
      },
      { 
        "type": "approver", 
        "userId": "user002", 
        "userName": "李四", 
        "order": 2, 
        "level": "global",
        "status": null 
      }
    ],
    "logs": []
  },
  "page": null
}
```

---

#### 51. POST /service/open/v2/approvals/:id/approve

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
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "500",
    "status": 1,
    "message": "审批通过，API 已上架"
  },
  "page": null
}
```

---

#### 52. POST /service/open/v2/approvals/:id/reject

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
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "500",
    "status": 2,
    "message": "审批已驳回"
  },
  "page": null
}
```

---

#### 53. POST /service/open/v2/approvals/:id/cancel

撤销审批。

> **说明**：仅申请人可撤销，且审批未完成时可撤销。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "500",
    "status": 3,
    "message": "审批已撤销"
  },
  "page": null
}
```

---

#### 54. POST /service/open/v2/approvals/batch-approve

批量同意审批。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approval_ids | array[long] | 是 | 审批单ID列表 |
| comment | string | 否 | 审批意见（统一填写） |

```json
{
  "approvalIds": ["500", "501", "502"],
  "comment": "批量审批通过"
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "message": "批量审批通过，共3条"
  },
  "page": null
}
```

---

#### 55. POST /service/open/v2/approvals/batch-reject

批量驳回审批（需填写原因）。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approval_ids | array[long] | 是 | 审批单ID列表 |
| reason | string | 是 | 驳回原因（统一填写） |

```json
{
  "approvalIds": ["500", "501", "502"],
  "reason": "文档不完整，请补充后重新提交"
}
```

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "message": "批量驳回成功，共3条"
  },
  "page": null
}
```

---

### 2.9 Scope 授权管理

#### 56. GET /service/open/v2/user-authorizations

获取用户授权列表。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | string | 否 | 用户ID过滤 |
| appId | string | 否 | 应用ID过滤 |
| keyword | string | 否 | 搜索关键词（用户名、应用名） |
| curPage | int | 否 | 当前页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

**响应示例**：

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

---

#### 57. POST /service/open/v2/user-authorizations

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
  "userId": "user001",
  "appId": "10",
  "scopes": ["api:im:send-message", "api:im:get-message"],
  "expiresAt": "2026-12-31T23:59:59"
}
```

**响应示例**：

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

---

#### 58. DELETE /service/open/v2/user-authorizations/:id

取消授权。

**响应示例**：

```json
{
  "code": "200",
  "messageZh": "授权已取消",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

---

### 2.10 消费网关

#### 59. ANY /gateway/api/*

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
  "code": "403",
  "message": "应用未订阅该 API 权限",
  "data": null,
  "page": null
}
```

---

#### 60. POST /gateway/events/publish

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
    "messageId": "msg001",
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
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "topic": "im.message.received",
    "subscribers": 5,
    "message": "事件已分发至 5 个订阅方"
  },
  "page": null
}
```

---

#### 61. POST /gateway/callbacks/invoke

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
  "callbackScope": "callback:approval:completed",
  "payload": {
    "approvalId": "app001",
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

#### 62. GET /gateway/permissions/check

权限校验接口（供网关内部调用）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| app_id | long | 是 | 应用ID |
| scope | string | 是 | 权限标识 |

**响应示例**：

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

**响应示例（未授权）**：

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

---

#### 63. POST /gateway/callbacks/config

回调配置查询接口（供 XX 通讯平台内部业务模块调用）。

> **说明**：XX 通讯平台内部业务模块通过 AK + Scope 查询应用对某个回调的订阅配置，用于触发回调时获取消费方配置信息。
>
> **提供方**：api-server
> **调用方**：event-server（平台统一回调出口）、XX 通讯平台内部其他业务模块
> **场景**：
> - event-server 作为平台统一回调出口，获取配置后统一调用三方平台回调地址
> - 其他业务模块也可直接获取回调配置，自行调用三方平台接口（保留此能力）

**请求头**：

| Header | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | 内部调用凭证（用于验证调用方身份） |

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ak | string | 是 | 应用 Access Key |
| scope | string | 是 | 回调权限标识（Scope） |

```json
{
  "ak": "AK123456789",
  "scope": "callback:approval:completed"
}
```

**响应示例（成功）**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "ak": "AK123456789",
    "scope": "callback:approval:completed",
    "channelType": 1,
    "channelAddress": "https://webhook.example.com/callbacks",
    "authType": 0
  }
}
```

**响应示例（未订阅）**：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": null
}
```

**响应示例（AK 无效）**：

```json
{
  "code": "400",
  "messageZh": "无效的 Access Key",
  "messageEn": "Invalid Access Key",
  "data": null
}
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| ak | string | 应用 Access Key |
| scope | string | 回调权限标识 |
| channelType | int | 通道类型（1=WebHook, 2=SSE, 3=WebSocket） |
| channelAddress | string | 通道地址 |
| authType | int | 认证类型（1=SOA, 2=APIG） |

**错误码说明**：

| 错误码 | 说明 |
|--------|------|
| 400 | 无效的 Access Key 或 Scope |
| 404 | 回调资源不存在 |

---

## 3. 附录

### 3.1 状态码定义

| 状态码 | 说明 |
|--------|------|
| "200" | 成功 |
| "400" | 请求参数错误 |
| "401" | 未授权 |
| "403" | 权限不足 |
| "404" | 资源不存在 |
| "409" | 资源冲突 |
| "500" | 服务器内部错误 |

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