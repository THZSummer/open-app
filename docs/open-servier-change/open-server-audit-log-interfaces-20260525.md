# Open-Server 审计日志接口清单

## 概述

本文档整理了 open-server 服务中所有非 GET 请求接口（POST / PUT / DELETE），按业务模块分类，作为审计日志实现的依据。

**统计**：共 10 个 Controller，51 个非 GET 接口

| HTTP 方法 | 数量 |
|-----------|-----:|
| POST | 30 |
| PUT | 11 |
| DELETE | 10 |
| **合计** | **51** |

## 现有审计日志覆盖情况

当前 `AuditLogAspect.java` 仅覆盖 Flow 模块的 3 个操作（startFlow / stopFlow / deleteFlow），且仅输出到 SLF4J 日志文件，无持久化存储。

---

## 一、API 资源管理（4 个接口）

**Controller**：`ApiController`
**路径前缀**：`/service/open/v2/apis`
**业务说明**：API 资源的注册、更新、删除、撤回，核心属性变更会触发审批流程

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 1 | POST | `/service/open/v2/apis` | `createApi` | 注册 API，同时创建权限资源 | `ApiCreateRequest` | #11 |
| 2 | PUT | `/service/open/v2/apis/{id}` | `updateApi` | 更新 API，核心属性变更触发审批 | `ApiUpdateRequest` | #12 |
| 3 | DELETE | `/service/open/v2/apis/{id}` | `deleteApi` | 删除 API，检查订阅关系 | -- | #13 |
| 4 | POST | `/service/open/v2/apis/{id}/withdraw` | `withdrawApi` | 撤回审核中的 API，状态变为草稿 | -- | #14 |

---

## 二、审批流程模板管理（3 个接口）

**Controller**：`ApprovalController`
**路径前缀**：`/service/open/v2`
**业务说明**：审批流程模板的增删改，影响后续所有审批行为的流转规则

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 5 | POST | `/service/open/v2/approval-flows` | `createFlow` | 创建审批流程模板 | `ApprovalFlowCreateRequest` | #43 |
| 6 | PUT | `/service/open/v2/approval-flows/{id}` | `updateFlow` | 更新审批流程模板 | `ApprovalFlowUpdateRequest` | #44 |
| 7 | DELETE | `/service/open/v2/approval-flows/{id}` | `deleteFlow` | 删除审批流程模板 | -- | #45 |

---

## 三、审批执行管理（5 个接口）

**Controller**：`ApprovalController`
**路径前缀**：`/service/open/v2`
**业务说明**：审批的同意、驳回、撤销及批量操作，直接影响权限和资源的生效状态

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 8 | POST | `/service/open/v2/approvals/{id}/approve` | `approve` | 同意审批 | `ApprovalActionRequest` | #48 |
| 9 | POST | `/service/open/v2/approvals/{id}/reject` | `reject` | 驳回审批 | `ApprovalActionRequest` | #49 |
| 10 | POST | `/service/open/v2/approvals/{id}/cancel` | `cancel` | 撤销审批 | -- | #50 |
| 11 | POST | `/service/open/v2/approvals/batch-approve` | `batchApprove` | 批量同意审批 | `BatchApprovalRequest` | #51 |
| 12 | POST | `/service/open/v2/approvals/batch-reject` | `batchReject` | 批量驳回审批 | `BatchApprovalRequest` | #52 |

---

## 四、回调资源管理（4 个接口）

**Controller**：`CallbackController`
**路径前缀**：`/service/open/v2/callbacks`
**业务说明**：回调资源的注册、更新、删除、撤回，注册时同步创建权限资源

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 13 | POST | `/service/open/v2/callbacks` | `createCallback` | 注册回调，同时创建权限资源 | `CallbackCreateRequest` | #23 |
| 14 | PUT | `/service/open/v2/callbacks/{id}` | `updateCallback` | 更新回调 | `CallbackUpdateRequest` | #24 |
| 15 | DELETE | `/service/open/v2/callbacks/{id}` | `deleteCallback` | 删除回调，检查订阅关系 | -- | #25 |
| 16 | POST | `/service/open/v2/callbacks/{id}/withdraw` | `withdrawCallback` | 撤回审核中的回调，状态变为草稿 | -- | #26 |

---

## 五、分类管理（5 个接口）

**Controller**：`CategoryController`
**路径前缀**：`/service/open/v2/categories`
**业务说明**：分类树形结构的增删改及责任人管理，分类是 API/事件/回调的归属容器

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 17 | POST | `/service/open/v2/categories` | `createCategory` | 创建分类节点 | `CategoryCreateRequest` | #3 |
| 18 | PUT | `/service/open/v2/categories/{id}` | `updateCategory` | 更新分类节点 | `CategoryUpdateRequest` | #4 |
| 19 | DELETE | `/service/open/v2/categories/{id}` | `deleteCategory` | 删除分类，检查关联资源 | -- | #5 |
| 20 | POST | `/service/open/v2/categories/{id}/owners` | `addOwner` | 添加分类责任人 | `CategoryOwnerRequest` | #6 |
| 21 | DELETE | `/service/open/v2/categories/{id}/owners/{userId}` | `removeOwner` | 移除分类责任人 | -- | #8 |

---

## 六、连接器管理（4 个接口）

**Controller**：`ConnectorController`
**路径前缀**：`/api/v1/connectors`
**业务说明**：连接器的创建、编辑、删除及连接配置管理

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 22 | POST | `/api/v1/connectors` | `createConnector` | 创建连接器 | `ConnectorCreateRequest` | #1 |
| 23 | PUT | `/api/v1/connectors/{connectorId}` | `updateConnector` | 编辑连接器基本信息 | `ConnectorUpdateRequest` | #4 |
| 24 | DELETE | `/api/v1/connectors/{connectorId}` | `deleteConnector` | 删除连接器，校验无运行中连接流引用 | -- | #5 |
| 25 | PUT | `/api/v1/connectors/{connectorId}/config` | `updateConnectorConfig` | 编辑连接配置（全文替换） | `ConnectorConfigUpdateRequest` | #7 |

---

## 七、连接流管理（6 个接口）

**Controller**：`FlowController`
**路径前缀**：`/api/v1/flows`
**业务说明**：连接流的创建、编辑、删除、启停及编排配置管理

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 | 已有审计 |
|:-:|------|---------|--------|---------|--------|:------:|:------:|
| 26 | POST | `/api/v1/flows` | `createFlow` | 创建连接流，默认 running 状态 | `FlowCreateRequest` | #8 | -- |
| 27 | PUT | `/api/v1/flows/{flowId}` | `updateFlow` | 编辑连接流基本信息 | `FlowUpdateRequest` | #11 | -- |
| 28 | DELETE | `/api/v1/flows/{flowId}` | `deleteFlow` | 删除连接流（仅 stopped 可删） | -- | #12 | **已有** |
| 29 | POST | `/api/v1/flows/{flowId}/start` | `startFlow` | 启动连接流（stopped -> running） | -- | #13 | **已有** |
| 30 | POST | `/api/v1/flows/{flowId}/stop` | `stopFlow` | 停止连接流（running -> stopped） | -- | #14 | **已有** |
| 31 | PUT | `/api/v1/flows/{flowId}/config` | `updateFlowConfig` | 保存编排配置（trigger/nodes/edges DAG） | `FlowConfigUpdateRequest` | #16 | -- |

---

## 八、调试代理（1 个接口）

**Controller**：`DebugProxyController`
**路径前缀**：`/api/v1/flows`
**业务说明**：转发测试运行请求到 connector-api 内部测试接口

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 32 | POST | `/api/v1/flows/{flowId}/test-run` | `testRun` | 转发测试运行请求到 connector-api | `TestRunRequest` | -- |

---

## 九、事件资源管理（4 个接口）

**Controller**：`EventController`
**路径前缀**：`/service/open/v2/events`
**业务说明**：事件资源的注册、更新、删除、撤回，注册时同步创建权限资源

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 33 | POST | `/service/open/v2/events` | `createEvent` | 注册事件，同时创建权限资源，Topic 唯一性校验 | `EventCreateRequest` | #17 |
| 34 | PUT | `/service/open/v2/events/{id}` | `updateEvent` | 更新事件 | `EventUpdateRequest` | #18 |
| 35 | DELETE | `/service/open/v2/events/{id}` | `deleteEvent` | 删除事件，检查订阅关系 | -- | #19 |
| 36 | POST | `/service/open/v2/events/{id}/withdraw` | `withdrawEvent` | 撤回审核中的事件 | -- | #20 |

---

## 十、权限订阅管理 -- API 权限（3 个接口）

**Controller**：`PermissionController`
**路径前缀**：方法级完整路径
**业务说明**：应用对 API 权限的申请、撤回、删除操作

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 37 | POST | `/service/open/v2/apps/{appId}/apis/subscribe` | `subscribeApiPermissions` | 申请 API 权限（支持批量） | `PermissionSubscribeRequest` | #29 |
| 38 | POST | `/service/open/v2/apps/{appId}/apis/{id}/withdraw` | `withdrawApiSubscription` | 撤回审核中的 API 权限申请 | -- | #30 |
| 39 | DELETE | `/service/open/v2/apps/{appId}/apis/{id}` | `deleteApiSubscription` | 删除终态 API 权限订阅记录 | -- | #31 |

---

## 十一、权限订阅管理 -- 事件权限（4 个接口）

**Controller**：`PermissionController`
**业务说明**：应用对事件权限的申请、配置、撤回、删除操作

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 40 | POST | `/service/open/v2/apps/{appId}/events/subscribe` | `subscribeEventPermissions` | 申请事件权限（支持批量） | `PermissionSubscribeRequest` | #34 |
| 41 | PUT | `/service/open/v2/apps/{appId}/events/{id}/config` | `configEventSubscription` | 配置事件消费参数（通道/地址/认证） | `SubscriptionConfigRequest` | #35 |
| 42 | POST | `/service/open/v2/apps/{appId}/events/{id}/withdraw` | `withdrawEventSubscription` | 撤回审核中的事件权限申请 | -- | #36 |
| 43 | DELETE | `/service/open/v2/apps/{appId}/events/{id}` | `deleteEventSubscription` | 删除终态事件权限订阅记录 | -- | #37 |

---

## 十二、权限订阅管理 -- 回调权限（4 个接口）

**Controller**：`PermissionController`
**业务说明**：应用对回调权限的申请、配置、撤回、删除操作

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 44 | POST | `/service/open/v2/apps/{appId}/callbacks/subscribe` | `subscribeCallbackPermissions` | 申请回调权限（支持批量） | `PermissionSubscribeRequest` | #40 |
| 45 | PUT | `/service/open/v2/apps/{appId}/callbacks/{id}/config` | `configCallbackSubscription` | 配置回调消费参数 | `SubscriptionConfigRequest` | #41 |
| 46 | POST | `/service/open/v2/apps/{appId}/callbacks/{id}/withdraw` | `withdrawCallbackSubscription` | 撤回审核中的回调权限申请 | -- | #42 |
| 47 | DELETE | `/service/open/v2/apps/{appId}/callbacks/{id}` | `deleteCallbackSubscription` | 删除终态回调权限订阅记录 | -- | #43 |

---

## 十三、数据同步（4 个接口）

**Controller**：`SyncController`
**路径前缀**：`/service/open/v2/sync`
**业务说明**：订阅关系数据在新旧系统间的双向迁移与应急数据修复，所有接口均需 `@PlatformAdminPermission` 管理员权限

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 48 | POST | `/service/open/v2/sync/subscription/migrate` | `migrate` | 迁移数据：旧表 -> 新表（幂等） | `SyncRequest` | -- |
| 49 | POST | `/service/open/v2/sync/subscription/rollback` | `rollback` | 回退数据：新表 -> 旧表（幂等） | `SyncRequest` | -- |
| 50 | POST | `/service/open/v2/sync/subscription/emergency/update-old` | `emergencyUpdateOld` | 应急更新旧订阅关系表 | `EmergencyRequest` | -- |
| 51 | POST | `/service/open/v2/sync/subscription/emergency/update-new` | `emergencyUpdateNew` | 应急更新新订阅关系表 | `EmergencyRequest` | -- |

---

## 按审计优先级分类建议

### P0 -- 高优先级（安全敏感 / 不可逆操作）

共 19 个接口，涉及审批决策、数据迁移、资源删除、权限变更等关键操作。

| 分类 | 接口编号 | 操作 |
|------|---------|------|
| 审批执行 | #48, #49, #50, #51, #52 | 同意/驳回/撤销/批量同意/批量驳回 |
| 资源删除 | #13, #25, #19, #5, #45, #24, #12, #28 | 删除 API/回调/事件/分类/审批模板/连接器/连接流 |
| 数据同步 | migrate, rollback, emergencyUpdateOld, emergencyUpdateNew | 数据迁移/回退/应急修复 |

### P1 -- 中优先级（资源创建 / 状态变更）

共 20 个接口，涉及资源注册、更新、启停、权限订阅等。

| 分类 | 接口编号 | 操作 |
|------|---------|------|
| 资源注册 | #11, #23, #17, #3, #43, #8, #1, #22 | 创建 API/回调/事件/分类/审批模板/连接流/连接器 |
| 资源更新 | #12, #24, #18, #4, #44, #11, #4, #7, #16, #25 | 更新各类资源 |
| 生命周期 | #13, #14, #29, #30 | 撤回/启停操作 |

### P2 -- 低优先级（订阅配置 / 辅助操作）

共 12 个接口，涉及权限订阅管理、责任人管理、调试等。

| 分类 | 接口编号 | 操作 |
|------|---------|------|
| 权限订阅 | #29, #30, #31, #34, #35, #36, #37, #40, #41, #42, #43 | 订阅/撤回/配置/删除 |
| 责任人 | #6, #8 | 添加/移除分类责任人 |
| 调试 | testRun | 测试运行 |
