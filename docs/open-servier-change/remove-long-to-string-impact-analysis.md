# 删除 Long→String 全局序列化策略 — 影响分析（v4）

> 日期：2026-06-02
>
> 背景：当前 `open-server` / `api-server` / `event-server` / `market-server` 四个模块的 `JacksonConfig` 全局注册了 `ToStringSerializer`，将所有 `Long` / `long` 字段序列化为 JSON 字符串。前端现在要求强校验 Long 类型，需要删除该转换策略，恢复为 JSON 数字输出。
>
> **v4 更新**：新增 market-server 微服务分析。market-server 的 VO 类直接使用 `Long` 类型定义 ID 字段（与其他三个模块 DTO 层使用 `String` 不同），删除转换后 ID 字段会直接从字符串变为数字。但 ID 为数据库自增（非雪花 ID），值域小，无精度丢失风险。

---

## 一、关键发现

**open-server / api-server / event-server** 所有 HTTP 响应 DTO 的 ID 字段均为 `String` 类型，Entity 中的 Long 字段经过 Service 层转换后才返回前端。

**market-server** 的 VO 类直接使用 `Long` 类型定义 ID 字段（`classifyId`, `itemId`, `id`），删除转换后这些字段会从字符串变为数字。

| 模块 | ID 字段定义方式 | 删除后影响 |
|------|:---:|------|
| open-server | DTO 层 `String` | 无变化 |
| api-server | DTO 层 `String` | 无变化 |
| event-server | DTO 层 `String` | 无变化 |
| **market-server** | **VO 层 `Long`** | **ID 从字符串变为数字** |

因此，删除 Long→String 全局策略后：

| 类别 | 影响 |
|------|------|
| 雪花 ID 精度丢失 | ✅ **无风险** — 雪花 ID 不经过 Long 序列化 |
| 前端接口契约变更 | ⚠️ `PageResponse.total`（4 个模块）+ market-server VO ID 字段 |
| 审计日志 JSON 格式 | ⚠️ Entity 快照序列化格式变化（内部存储，非接口响应，可忽略） |

---

## 二、当前 JacksonConfig 配置详情（4 个模块）

### 2.1 open-server

| 配置项 | 值 | 与 Long→String 相关？ |
|--------|---|:---:|
| `JavaTimeModule` | 注册 | ❌ |
| `WRITE_DATES_AS_TIMESTAMPS` | 禁用 | ❌ |
| **`FAIL_ON_UNKNOWN_PROPERTIES`** | **禁用** | **❌（需保留）** |
| `setTimeZone("Asia/Shanghai")` | 设置 | ❌ |
| `Long.class → ToStringSerializer` | 注册 | ✅ **删除** |
| `Long.TYPE → ToStringSerializer` | 注册 | ✅ **删除** |

### 2.2 api-server

| 配置项 | 值 | 与 Long→String 相关？ |
|--------|---|:---:|
| `JavaTimeModule` | 注册 | ❌ |
| `WRITE_DATES_AS_TIMESTAMPS` | 禁用 | ❌ |
| `Long.class → ToStringSerializer` | 注册 | ✅ **删除** |
| `Long.TYPE → ToStringSerializer` | 注册 | ✅ **删除** |

### 2.3 event-server

| 配置项 | 值 | 与 Long→String 相关？ |
|--------|---|:---:|
| `JavaTimeModule` | 注册 | ❌ |
| `WRITE_DATES_AS_TIMESTAMPS` | 禁用 | ❌ |
| `Long.class → ToStringSerializer` | 注册 | ✅ **删除** |
| `Long.TYPE → ToStringSerializer` | 注册 | ✅ **删除** |

### 2.4 market-server（新增）

| 配置项 | 值 | 与 Long→String 相关？ |
|--------|---|:---:|
| `@AutoConfigureBefore(JacksonAutoConfiguration.class)` | 注解 | ❌ |
| `WRITE_DATES_AS_TIMESTAMPS` | 禁用 | ❌ |
| `setTimeZone("Asia/Shanghai")` | 设置 | ❌ |
| `LocalDateTimeSerializer`（`yyyy-MM-dd HH:mm:ss`） | 自定义序列化器 | ❌ |
| `LocalDateSerializer`（`yyyy-MM-dd`） | 自定义序列化器 | ❌ |
| `HttpMessageConverters` Bean | 注册自定义 Converter | ❌ |
| `Long.class → ToStringSerializer` | 注册 | ✅ **删除** |
| `Long.TYPE → ToStringSerializer` | 注册 | ✅ **删除** |

---

## 三、前端契约变更 — 受影响接口完整清单

### 3.1 `PageResponse.total` 类型变化（`String` → `Number`）

`ApiResponse.PageResponse.total` 定义为 `Long` 类型，删除转换后：

```json
// 改前
{ "data": [...], "page": { "total": "128", "curPage": 1, "pageSize": 20, "totalPages": 7 } }

// 改后
{ "data": [...], "page": { "total": 128, "curPage": 1, "pageSize": 20, "totalPages": 7 } }
```

> `total` 为计数值，远小于 `2^53`，**不存在精度丢失风险**。

#### open-server 分页接口（9 个）

| # | 方法 | 路径 | 说明 |
|:-:|------|------|------|
| 1 | GET | `/service/open/v2/apis` | API 列表 |
| 2 | GET | `/service/open/v2/events` | 事件列表 |
| 3 | GET | `/service/open/v2/callbacks` | 回调列表 |
| 4 | GET | `/service/open/v2/categories` | 分类列表 |
| 5 | GET | `/service/open/v2/approval-flows` | 审批流程模板列表 |
| 6 | GET | `/service/open/v2/approvals/pending` | 待审批列表 |
| 7 | GET | `/service/open/v2/apps/{appId}/apis` | 应用 API 权限订阅列表 |
| 8 | GET | `/service/open/v2/apps/{appId}/events` | 应用事件权限订阅列表 |
| 9 | GET | `/service/open/v2/apps/{appId}/callbacks` | 应用回调权限订阅列表 |

#### api-server 分页接口（1 个）

| # | 方法 | 路径 | 说明 |
|:-:|------|------|------|
| 10 | GET | `/api/v1/user-authorizations` | 用户授权列表 |

#### market-server 分页接口（3 个）

| # | 方法 | 路径 | 说明 |
|:-:|------|------|------|
| 11 | GET | `/service/open/v2/lookup/classify/list` | 分类列表 |
| 12 | GET | `/service/open/v2/lookup/classify/{classifyId}/items` | 字典项列表 |
| 13 | GET | `/service/open/v2/dictionary/list` | 字典列表 |

> event-server 无分页接口。

---

### 3.2 Sync 模块内部 DTO（5 个 Long 字段，4 个接口）

> Sync 模块为内部数据迁移工具，非前端常规业务接口。

#### 受影响的 DTO 字段

| DTO 类 | 字段 | 类型 | 语义 |
|--------|------|------|------|
| `SyncDetail` | `id` | `Long` | 同步详情记录 ID |
| `EmergencyDetail` | `id` | `Long` | 应急详情记录 ID |
| `SubscriptionData` | `id` | `Long` | 订阅数据记录 ID |
| `SubscriptionData` | `appId` | `Long` | 应用 ID |
| `SubscriptionData` | `permissionId` | `Long` | 权限 ID |

#### 受影响的接口（4 个）

| # | 方法 | 路径 | 响应中涉及的 Long 字段 |
|:-:|------|------|---------|
| 1 | POST | `/service/open/v2/sync/subscription/migrate` | `SyncResult.details[].id` |
| 2 | POST | `/service/open/v2/sync/subscription/rollback` | `SyncResult.details[].id` |
| 3 | POST | `/service/open/v2/sync/subscription/emergency/update-old` | `EmergencyResult.details[].id` |
| 4 | POST | `/service/open/v2/sync/subscription/emergency/update-new` | `EmergencyResult.details[].id` |

---

### 3.3 market-server VO 类 Long 字段（新增，10 个字段，8 个接口）

> market-server 与其他三个模块不同：VO 类直接使用 `Long` 类型定义 ID 字段，删除 Long→String 后这些字段会从字符串变为数字。
>
> **无精度丢失风险**：ID 为数据库自增（非雪花 ID），值域远小于 `2^53`。

#### 受影响的 VO 字段

| VO 类 | 字段 | 类型 | 语义 |
|--------|------|------|------|
| `DictionaryListVO` | `id` | `Long` | 字典主键（自增） |
| `DictionaryVO` | `id` | `Long` | 字典主键（自增） |
| `ClassifyListVO` | `classifyId` | `Long` | 分类主键（自增） |
| `ClassifyVO` | `classifyId` | `Long` | 分类主键（自增） |
| `ItemListVO` | `itemId` | `Long` | 字典项主键（自增） |
| `ItemListVO` | `classifyId` | `Long` | 分类外键 |
| `ItemDetailVO` | `itemId` | `Long` | 字典项主键（自增） |
| `ItemDetailVO` | `classifyId` | `Long` | 分类外键 |
| `PageVO` | `total` | `Long` | 分页总数 |
| `ApiResponse.PageResponse` | `total` | `Long` | 分页总数 |

#### 受影响的接口（8 个）

| # | 方法 | 路径 | 返回类型 | 变化的 Long 字段 |
|:-:|------|------|---------|-----------------|
| 1 | GET | `/service/open/v2/lookup/classify/list` | `ApiResponse<PageVO<ClassifyListVO>>` | `total` + `list[].classifyId` |
| 2 | GET | `/service/open/v2/lookup/classify/{classifyId}` | `ApiResponse<ClassifyVO>` | `classifyId` |
| 3 | GET | `/service/open/v2/lookup/classify/{classifyId}/items` | `ApiResponse<PageVO<ItemListVO>>` | `total` + `list[].itemId` + `list[].classifyId` |
| 4 | GET | `/service/open/v2/lookup/items/{itemId}` | `ApiResponse<ItemDetailVO>` | `itemId` + `classifyId` |
| 5 | GET | `/service/open/v2/dictionary/list` | `ApiResponse<PageVO<DictionaryListVO>>` | `total` + `list[].id` |
| 6 | GET | `/service/open/v2/dictionary/{id}` | `ApiResponse<DictionaryVO>` | `id` |
| 7 | POST | `/service/open/v2/lookup/classify` | `ApiResponse<Void>` | 无（响应无数据） |
| 8 | POST | `/service/open/v2/lookup/classify/{classifyId}/items` | `ApiResponse<Void>` | 无（响应无数据） |

#### 响应示例

```json
// 改前
{ "code": "200", "data": { "classifyId": "123", "name": "分类A" } }

// 改后
{ "code": "200", "data": { "classifyId": 123, "name": "分类A" } }
```

---

### 3.4 前端契约变更接口汇总（无精度丢失风险）

**以下所有接口的 ID 字段均为 `String` 类型定义，不受 Long→String 删除影响。**

仅 `page.total` 从字符串变为数字。

#### open-server（79 个接口，完整列表）

##### HealthController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 1 | GET | `/service/open/v2/health` | `ApiResponse<Map>` | 无 |
| 2 | GET | `/service/open/v2/user-info` | `ApiResponse<Map>` | 无 |

##### CategoryController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 3 | GET | `/service/open/v2/categories` | `ApiResponse<List<CategoryTreeResponse>>` | `page.total` |
| 4 | GET | `/service/open/v2/categories/{id}` | `ApiResponse<CategoryResponse>` | 无 |
| 5 | POST | `/service/open/v2/categories` | `ApiResponse<CategoryResponse>` | 无 |
| 6 | PUT | `/service/open/v2/categories/{id}` | `ApiResponse<CategoryResponse>` | 无 |
| 7 | DELETE | `/service/open/v2/categories/{id}` | `ApiResponse<Void>` | 无 |
| 8 | POST | `/service/open/v2/categories/{id}/owners` | `ApiResponse<CategoryOwnerResponse>` | 无 |
| 9 | GET | `/service/open/v2/categories/{id}/owners` | `ApiResponse<List<CategoryOwnerResponse>>` | 无 |
| 10 | DELETE | `/service/open/v2/categories/{id}/owners/{userId}` | `ApiResponse<Void>` | 无 |

##### ConnectorController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 11 | POST | `/api/v1/connectors` | `ApiResponse<ConnectorCreateResponse>` | 无 |
| 12 | GET | `/api/v1/connectors` | `ApiResponse<List<ConnectorListResponse>>` | `page.total` |
| 13 | GET | `/api/v1/connectors/{connectorId}` | `ApiResponse<ConnectorDetailResponse>` | 无 |
| 14 | PUT | `/api/v1/connectors/{connectorId}` | `ApiResponse<Void>` | 无 |
| 15 | DELETE | `/api/v1/connectors/{connectorId}` | `ApiResponse<Void>` | 无 |
| 16 | GET | `/api/v1/connectors/{connectorId}/config` | `ApiResponse<ConnectorConfigResponse>` | 无 |
| 17 | PUT | `/api/v1/connectors/{connectorId}/config` | `ApiResponse<Void>` | 无 |

##### FlowController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 18 | POST | `/api/v1/flows` | `ApiResponse<FlowCreateResponse>` | 无 |
| 19 | GET | `/api/v1/flows` | `ApiResponse<List<FlowListResponse>>` | `page.total` |
| 20 | GET | `/api/v1/flows/{flowId}` | `ApiResponse<FlowDetailResponse>` | 无 |
| 21 | PUT | `/api/v1/flows/{flowId}` | `ApiResponse<Void>` | 无 |
| 22 | DELETE | `/api/v1/flows/{flowId}` | `ApiResponse<Void>` | 无 |
| 23 | POST | `/api/v1/flows/{flowId}/start` | `ApiResponse<Void>` | 无 |
| 24 | POST | `/api/v1/flows/{flowId}/stop` | `ApiResponse<Void>` | 无 |
| 25 | GET | `/api/v1/flows/{flowId}/config` | `ApiResponse<FlowConfigResponse>` | 无 |
| 26 | PUT | `/api/v1/flows/{flowId}/config` | `ApiResponse<Void>` | 无 |

##### DebugProxyController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 27 | POST | `/api/v1/flows/{flowId}/test-run` | `ApiResponse<Map>` | 无 |

##### ApiController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 28 | GET | `/service/open/v2/apis` | `ApiResponse<List<ApiListResponse>>` | `page.total` |
| 29 | GET | `/service/open/v2/apis/{id}` | `ApiResponse<ApiDetailResponse>` | 无 |
| 30 | POST | `/service/open/v2/apis` | `ApiResponse<ApiDetailResponse>` | 无 |
| 31 | PUT | `/service/open/v2/apis/{id}` | `ApiResponse<ApiDetailResponse>` | 无 |
| 32 | DELETE | `/service/open/v2/apis/{id}` | `ApiResponse<Void>` | 无 |
| 33 | POST | `/service/open/v2/apis/{id}/withdraw` | `ApiResponse<ApiDetailResponse>` | 无 |

##### EventController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 34 | GET | `/service/open/v2/events` | `ApiResponse<List<EventListResponse>>` | `page.total` |
| 35 | GET | `/service/open/v2/events/{id}` | `ApiResponse<EventResponse>` | 无 |
| 36 | POST | `/service/open/v2/events` | `ApiResponse<EventResponse>` | 无 |
| 37 | PUT | `/service/open/v2/events/{id}` | `ApiResponse<EventResponse>` | 无 |
| 38 | DELETE | `/service/open/v2/events/{id}` | `ApiResponse<Void>` | 无 |
| 39 | POST | `/service/open/v2/events/{id}/withdraw` | `ApiResponse<EventResponse>` | 无 |

##### CallbackController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 40 | GET | `/service/open/v2/callbacks` | `ApiResponse<List<CallbackListResponse>>` | `page.total` |
| 41 | GET | `/service/open/v2/callbacks/{id}` | `ApiResponse<CallbackResponse>` | 无 |
| 42 | POST | `/service/open/v2/callbacks` | `ApiResponse<CallbackResponse>` | 无 |
| 43 | PUT | `/service/open/v2/callbacks/{id}` | `ApiResponse<CallbackResponse>` | 无 |
| 44 | DELETE | `/service/open/v2/callbacks/{id}` | `ApiResponse<Void>` | 无 |
| 45 | POST | `/service/open/v2/callbacks/{id}/withdraw` | `ApiResponse<CallbackResponse>` | 无 |

##### PermissionController — API 权限

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 46 | GET | `/service/open/v2/apps/{appId}/apis` | `ApiResponse<List<ApiSubscriptionListResponse>>` | `page.total` |
| 47 | GET | `/service/open/v2/categories/{id}/apis` | `ApiResponse<List<CategoryPermissionListResponse>>` | 无 |
| 48 | POST | `/service/open/v2/apps/{appId}/apis/subscribe` | `ApiResponse<PermissionSubscribeResponse>` | 无 |
| 49 | POST | `/service/open/v2/apps/{appId}/apis/{id}/withdraw` | `ApiResponse<WithdrawResponse>` | 无 |
| 50 | DELETE | `/service/open/v2/apps/{appId}/apis/{id}` | `ApiResponse<WithdrawResponse>` | 无 |

##### PermissionController — 事件权限

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 51 | GET | `/service/open/v2/apps/{appId}/events` | `ApiResponse<List<EventSubscriptionListResponse>>` | `page.total` |
| 52 | GET | `/service/open/v2/categories/{id}/events` | `ApiResponse<List<CategoryPermissionListResponse>>` | 无 |
| 53 | POST | `/service/open/v2/apps/{appId}/events/subscribe` | `ApiResponse<PermissionSubscribeResponse>` | 无 |
| 54 | PUT | `/service/open/v2/apps/{appId}/events/{id}/config` | `ApiResponse<WithdrawResponse>` | 无 |
| 55 | POST | `/service/open/v2/apps/{appId}/events/{id}/withdraw` | `ApiResponse<WithdrawResponse>` | 无 |
| 56 | DELETE | `/service/open/v2/apps/{appId}/events/{id}` | `ApiResponse<WithdrawResponse>` | 无 |

##### PermissionController — 回调权限

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 57 | GET | `/service/open/v2/apps/{appId}/callbacks` | `ApiResponse<List<CallbackSubscriptionListResponse>>` | `page.total` |
| 58 | GET | `/service/open/v2/categories/{id}/callbacks` | `ApiResponse<List<CategoryPermissionListResponse>>` | 无 |
| 59 | POST | `/service/open/v2/apps/{appId}/callbacks/subscribe` | `ApiResponse<PermissionSubscribeResponse>` | 无 |
| 60 | PUT | `/service/open/v2/apps/{appId}/callbacks/{id}/config` | `ApiResponse<WithdrawResponse>` | 无 |
| 61 | POST | `/service/open/v2/apps/{appId}/callbacks/{id}/withdraw` | `ApiResponse<WithdrawResponse>` | 无 |
| 62 | DELETE | `/service/open/v2/apps/{appId}/callbacks/{id}` | `ApiResponse<WithdrawResponse>` | 无 |

##### SyncController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 63 | POST | `/service/open/v2/sync/subscription/migrate` | `ApiResponse<SyncResult>` | **`details[].id`** |
| 64 | POST | `/service/open/v2/sync/subscription/rollback` | `ApiResponse<SyncResult>` | **`details[].id`** |
| 65 | POST | `/service/open/v2/sync/subscription/emergency/update-old` | `ApiResponse<EmergencyResult>` | **`details[].id`** |
| 66 | POST | `/service/open/v2/sync/subscription/emergency/update-new` | `ApiResponse<EmergencyResult>` | **`details[].id`** |

##### ApprovalController — 流程模板

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 67 | GET | `/service/open/v2/approval-flows` | `ApiResponse<List<ApprovalFlowListResponse>>` | `page.total` |
| 68 | GET | `/service/open/v2/approval-flows/{id}` | `ApiResponse<ApprovalFlowDetailResponse>` | 无 |
| 69 | POST | `/service/open/v2/approval-flows` | `ApiResponse<ApprovalFlowDetailResponse>` | 无 |
| 70 | PUT | `/service/open/v2/approval-flows/{id}` | `ApiResponse<ApprovalFlowDetailResponse>` | 无 |
| 71 | DELETE | `/service/open/v2/approval-flows/{id}` | `ApiResponse<Void>` | 无 |

##### ApprovalController — 审批执行

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 72 | GET | `/service/open/v2/approvals/pending` | `ApiResponse<List<ApprovalPendingListResponse>>` | `page.total` |
| 73 | GET | `/service/open/v2/approvals/{id}` | `ApiResponse<ApprovalDetailResponse>` | 无 |
| 74 | POST | `/service/open/v2/approvals/{id}/approve` | `ApiResponse<ApprovalActionResponse>` | 无 |
| 75 | POST | `/service/open/v2/approvals/{id}/reject` | `ApiResponse<ApprovalActionResponse>` | 无 |
| 76 | POST | `/service/open/v2/approvals/{id}/cancel` | `ApiResponse<ApprovalActionResponse>` | 无 |
| 77 | POST | `/service/open/v2/approvals/batch-approve` | `ApiResponse<BatchApprovalResponse>` | 无 |
| 78 | POST | `/service/open/v2/approvals/batch-reject` | `ApiResponse<BatchApprovalResponse>` | 无 |
| 79 | POST | `/service/open/v2/approvals/{id}/urge` | `ApiResponse<ApprovalActionResponse>` | 无 |

---

#### api-server（11 个接口，完整列表）

##### HealthController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 80 | GET | `/api/v1/health` | `ApiResponse<Map>` | 无 |

##### DataQueryController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 81 | GET | `/gateway/permissions/check` | `ApiResponse<PermissionCheckResponse>` | 无 |
| 82 | GET | `/gateway/permissions/subscribers` | `ApiResponse<List<String>>` | 无 |
| 83 | GET | `/gateway/subscriptions/config` | `ApiResponse<Map>` | 无 |
| 84 | GET | `/gateway/permissions/detail` | `ApiResponse<Map>` | 无 |

##### ApiGatewayController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 85 | ANY | `/gateway/api/**` | `ResponseEntity<String>` | 无（原始 JSON 透传） |
| 86 | POST | `/gateway/assistant/callbacks/config` | `ApiResponse<CallbackConfigResponse>` | 无 |

##### ScopeController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 87 | GET | `/api/v1/user-authorizations` | `ApiResponse<List<UserAuthorizationListResponse>>` | `page.total` |
| 88 | POST | `/api/v1/user-authorizations` | `ApiResponse<UserAuthorizationResponse>` | 无 |
| 89 | DELETE | `/api/v1/user-authorizations/{id}` | `ApiResponse<Void>` | 无 |

##### ApprovalCallbackController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 90 | POST | `/api/v1/approvals/callback` | `ApprovalCallbackResponse<?>` | 无 |

---

#### event-server（10 个接口，完整列表）

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 91 | GET | `/api/v1/health` | `ApiResponse<Map>` | 无 |
| 92 | GET | `/sse/connect/{connectionId}` | `SseEmitter`（流式） | 无 |
| 93 | DELETE | `/sse/disconnect/{connectionId}` | `ApiResponse<Void>` | 无 |
| 94 | GET | `/sse/status` | `ApiResponse<Map>` | 无 |
| 95 | GET | `/ws/status` | `ApiResponse<Map>` | 无 |
| 96 | GET | `/ws/count` | `ApiResponse<Map>` | 无 |
| 97 | POST | `/gateway/events/publish` | `ApiResponse<EventPublishResponse>` | 无 |
| 98 | DELETE | `/gateway/events/cache/{topic}` | `ApiResponse<Void>` | 无 |
| 99 | POST | `/gateway/callbacks/invoke` | `ApiResponse<CallbackInvokeResponse>` | 无 |
| 100 | DELETE | `/gateway/callbacks/cache/{scope}` | `ApiResponse<Void>` | 无 |

---

#### connector-api（2 个接口，完整列表）

> connector-api 原本就没有 JacksonConfig，**无任何变化**。

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 101 | POST | `/api/v1/internal/test-run/{flowId}` | `Mono<ExecutionResult>` | `totalDurationMs: long`（已有） |
| 102 | POST | `/api/v1/trigger/{flowId}/invoke` | `Mono<ExecutionResult>` | `totalDurationMs: long`（已有） |

---

#### market-server（17 个接口，完整列表，新增）

##### HealthController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 103 | GET | `/service/open/v2/health` | `ApiResponse<Map>` | 无 |
| 104 | GET | `/service/open/v2/user-info` | `ApiResponse<Map>` | 无 |

##### ClassifyController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 105 | GET | `/service/open/v2/lookup/classify/list` | `ApiResponse<PageVO<ClassifyListVO>>` | **`total`** + **`list[].classifyId`** |
| 106 | POST | `/service/open/v2/lookup/classify` | `ApiResponse<Void>` | 无 |
| 107 | PUT | `/service/open/v2/lookup/classify/{classifyId}` | `ApiResponse<Void>` | 无 |
| 108 | DELETE | `/service/open/v2/lookup/classify/{classifyId}` | `ApiResponse<Void>` | 无 |
| 109 | GET | `/service/open/v2/lookup/classify/{classifyId}` | `ApiResponse<ClassifyVO>` | **`classifyId`** |

##### LookUpItemController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 110 | GET | `/service/open/v2/lookup/classify/{classifyId}/items` | `ApiResponse<PageVO<ItemListVO>>` | **`total`** + **`list[].itemId`** + **`list[].classifyId`** |
| 111 | POST | `/service/open/v2/lookup/classify/{classifyId}/items` | `ApiResponse<Void>` | 无 |
| 112 | PUT | `/service/open/v2/lookup/items/{itemId}` | `ApiResponse<Void>` | 无 |
| 113 | DELETE | `/service/open/v2/lookup/items/{itemId}` | `ApiResponse<Void>` | 无 |
| 114 | GET | `/service/open/v2/lookup/items/{itemId}` | `ApiResponse<ItemDetailVO>` | **`itemId`** + **`classifyId`** |

##### DictionaryController

| # | 方法 | 路径 | 返回类型 | Long 字段 |
|:-:|------|------|---------|----------|
| 115 | GET | `/service/open/v2/dictionary/list` | `ApiResponse<PageVO<DictionaryListVO>>` | **`total`** + **`list[].id`** |
| 116 | POST | `/service/open/v2/dictionary` | `ApiResponse<Void>` | 无 |
| 117 | GET | `/service/open/v2/dictionary/{id}` | `ApiResponse<DictionaryVO>` | **`id`** |
| 118 | PUT | `/service/open/v2/dictionary/{id}` | `ApiResponse<Void>` | 无 |
| 119 | DELETE | `/service/open/v2/dictionary/{id}` | `ApiResponse<Void>` | 无 |

---

## 四、精度丢失风险评估

### ✅ 结论：无精度丢失风险

**原因**：项目所有 HTTP 响应 DTO 的 ID 字段均定义为 `String` 类型，Entity 中的雪花 ID（Long）在 Service 层已转换为 String 后才放入 DTO。

验证：

| DTO 类别 | ID 字段类型 | 示例 |
|----------|:---:|------|
| `ApiDetailResponse` | `String id` | ✅ |
| `ApiListResponse` | `String id` | ✅ |
| `EventResponse` | `String id` | ✅ |
| `CallbackResponse` | `String id` | ✅ |
| `CategoryResponse` | `String id` | ✅ |
| `CategoryTreeResponse` | `String id` | ✅ |
| `ApprovalFlowDetailResponse` | `String id` | ✅ |
| `ApprovalPendingListResponse` | `String id` | ✅ |
| `ApiSubscriptionListResponse` | `String id` | ✅ |
| `EventSubscriptionListResponse` | `String id` | ✅ |
| `CallbackSubscriptionListResponse` | `String id` | ✅ |
| `PermissionSubscribeResponse` | `String id` | ✅ |
| `ConnectorCreateResponse` | `String id` | ✅ |
| `FlowCreateResponse` | `String id` | ✅ |
| `UserAuthorizationResponse` | `String id` | ✅ |
| `ConnectorDetailResponse` | `String id` | ✅ |
| `FlowDetailResponse` | `String id` | ✅ |

### Sync 模块 DTO 例外

以下 DTO 的 ID 为 `Long` 类型，删除转换后会变为数字。但均为旧系统数据库自增 ID（通常 < 10 万），**不超过 `2^53`，无精度丢失风险**。

| DTO | 字段 | ID 来源 | 风险 |
|-----|------|---------|------|
| `SyncDetail.id` | `Long` | 旧系统自增 ID | ✅ 无风险 |
| `EmergencyDetail.id` | `Long` | 旧系统自增 ID | ✅ 无风险 |
| `SubscriptionData.id` | `Long` | 旧系统自增 ID | ✅ 无风险 |
| `SubscriptionData.appId` | `Long` | 旧系统自增 ID | ✅ 无风险 |
| `SubscriptionData.permissionId` | `Long` | 旧系统自增 ID | ✅ 无风险 |

---

## 五、不受影响的场景

| 场景 | 原因 |
|------|------|
| connector-api HTTP 响应 | 原本就没有 JacksonConfig，Long 已是数字输出 |
| event-server WebSocket 消息 | `WebSocketChannel` 使用 `new ObjectMapper()`，不走 Spring Bean |
| RestTemplate 跨服务调用 | `new RestTemplate()` 使用默认转换器，不走 JacksonConfig |
| 测试代码 | 全部使用 `new ObjectMapper()`，不受影响 |
| Entity 雪花 ID → HTTP 响应 | Service 层已转换为 String，不经过 Jackson Long 序列化 |

---

## 六、变更影响统计

### 前端契约变更

| 变更类型 | 影响接口数 | 涉及字段 |
|---------|:---:|---------|
| `page.total`: String → Number | 13 个分页接口 | `ApiResponse.PageResponse.total` / `PageVO.total` |
| Sync DTO `id`: Long(String) → Long(Number) | 4 个 Sync 接口 | `SyncDetail.id`, `EmergencyDetail.id` |
| **market-server VO ID**: Long(String) → Long(Number) | **6 个查询接口** | **`classifyId`, `itemId`, `id`** |
| **合计** | **23 个** | |

### 无精度丢失风险

| 类别 | 接口数 | 原因 |
|------|:---:|------|
| 雪花 ID（open/api/event 业务接口） | 88 个 | DTO 层 ID 已定义为 String，不经过 Jackson Long 序列化 |
| 分页 total | 13 个 | 计数值远小于 `2^53` |
| Sync 模块 ID | 4 个 | 旧系统自增 ID，值域小 |
| **market-server VO ID** | **6 个** | **数据库自增 ID，值域小** |
| connector-api | 2 个 | 原本就无转换 |
| **合计** | **119 个** | **全部无风险** |

### 内部影响

| 影响项 | 严重程度 | 说明 |
|--------|:---:|------|
| 审计日志 JSON 格式 | ✅ 可忽略 | Entity 快照 Long 字段从字符串变为数字，仅内部存储 |
