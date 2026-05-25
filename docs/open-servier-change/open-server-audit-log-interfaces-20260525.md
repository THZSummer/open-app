# Open-Server 审计日志接口清单（API / 事件 / 回调）

**文档版本**：2.0  
**创建日期**：2026-05-25  
**更新日期**：2026-05-25  

---

## 概述

本文档整理 open-server 中 **API、事件、回调** 三类业务资源相关的所有非 GET 接口，作为审计日志实现的范围依据。

**范围说明**：仅覆盖 API / 事件 / 回调 的资源管理 + 权限订阅管理接口，不涉及审批、分类、连接器、连接流、同步等模块。

**统计**：共 4 个 Controller，**23 个非 GET 接口**

| HTTP 方法 | 数量 |
|-----------|-----:|
| POST | 12 |
| PUT | 6 |
| DELETE | 5 |
| **合计** | **23** |

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

## 二、事件资源管理（4 个接口）

**Controller**：`EventController`  
**路径前缀**：`/service/open/v2/events`  
**业务说明**：事件资源的注册、更新、删除、撤回，注册时同步创建权限资源

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 5 | POST | `/service/open/v2/events` | `createEvent` | 注册事件，同时创建权限资源，Topic 唯一性校验 | `EventCreateRequest` | #17 |
| 6 | PUT | `/service/open/v2/events/{id}` | `updateEvent` | 更新事件 | `EventUpdateRequest` | #18 |
| 7 | DELETE | `/service/open/v2/events/{id}` | `deleteEvent` | 删除事件，检查订阅关系 | -- | #19 |
| 8 | POST | `/service/open/v2/events/{id}/withdraw` | `withdrawEvent` | 撤回审核中的事件 | -- | #20 |

---

## 三、回调资源管理（4 个接口）

**Controller**：`CallbackController`  
**路径前缀**：`/service/open/v2/callbacks`  
**业务说明**：回调资源的注册、更新、删除、撤回，注册时同步创建权限资源

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 9 | POST | `/service/open/v2/callbacks` | `createCallback` | 注册回调，同时创建权限资源 | `CallbackCreateRequest` | #23 |
| 10 | PUT | `/service/open/v2/callbacks/{id}` | `updateCallback` | 更新回调 | `CallbackUpdateRequest` | #24 |
| 11 | DELETE | `/service/open/v2/callbacks/{id}` | `deleteCallback` | 删除回调，检查订阅关系 | -- | #25 |
| 12 | POST | `/service/open/v2/callbacks/{id}/withdraw` | `withdrawCallback` | 撤回审核中的回调，状态变为草稿 | -- | #26 |

---

## 四、API 权限订阅管理（3 个接口）

**Controller**：`PermissionController`  
**业务说明**：应用对 API 权限的申请、撤回、删除操作

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 13 | POST | `/service/open/v2/apps/{appId}/apis/subscribe` | `subscribeApiPermissions` | 申请 API 权限（支持批量） | `PermissionSubscribeRequest` | #29 |
| 14 | POST | `/service/open/v2/apps/{appId}/apis/{id}/withdraw` | `withdrawApiSubscription` | 撤回审核中的 API 权限申请 | -- | #30 |
| 15 | DELETE | `/service/open/v2/apps/{appId}/apis/{id}` | `deleteApiSubscription` | 删除终态 API 权限订阅记录 | -- | #31 |

---

## 五、事件权限订阅管理（4 个接口）

**Controller**：`PermissionController`  
**业务说明**：应用对事件权限的申请、配置、撤回、删除操作

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 16 | POST | `/service/open/v2/apps/{appId}/events/subscribe` | `subscribeEventPermissions` | 申请事件权限（支持批量） | `PermissionSubscribeRequest` | #34 |
| 17 | PUT | `/service/open/v2/apps/{appId}/events/{id}/config` | `configEventSubscription` | 配置事件消费参数（通道/地址/认证） | `SubscriptionConfigRequest` | #35 |
| 18 | POST | `/service/open/v2/apps/{appId}/events/{id}/withdraw` | `withdrawEventSubscription` | 撤回审核中的事件权限申请 | -- | #36 |
| 19 | DELETE | `/service/open/v2/apps/{appId}/events/{id}` | `deleteEventSubscription` | 删除终态事件权限订阅记录 | -- | #37 |

---

## 六、回调权限订阅管理（4 个接口）

**Controller**：`PermissionController`  
**业务说明**：应用对回调权限的申请、配置、撤回、删除操作

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 20 | POST | `/service/open/v2/apps/{appId}/callbacks/subscribe` | `subscribeCallbackPermissions` | 申请回调权限（支持批量） | `PermissionSubscribeRequest` | #40 |
| 21 | PUT | `/service/open/v2/apps/{appId}/callbacks/{id}/config` | `configCallbackSubscription` | 配置回调消费参数 | `SubscriptionConfigRequest` | #41 |
| 22 | POST | `/service/open/v2/apps/{appId}/callbacks/{id}/withdraw` | `withdrawCallbackSubscription` | 撤回审核中的回调权限申请 | -- | #42 |
| 23 | DELETE | `/service/open/v2/apps/{appId}/callbacks/{id}` | `deleteCallbackSubscription` | 删除终态回调权限订阅记录 | -- | #43 |

---

## 按审计优先级分类

### P0 — 高优先级（删除 / 撤回等不可逆操作）

共 **8 个接口**，涉及资源删除和权限删除，操作不可逆。

| 分类 | # | 操作 |
|------|:-:|------|
| 资源删除 | 3, 7, 11 | 删除 API / 事件 / 回调 |
| 权限删除 | 15, 19, 23 | 删除 API / 事件 / 回调权限订阅 |
| 资源撤回 | 4, 8, 12 | 撤回审核中的 API / 事件 / 回调 |

### P1 — 中优先级（创建 / 状态变更）

共 **6 个接口**，涉及资源注册和权限申请，触发审批流程。

| 分类 | # | 操作 |
|------|:-:|------|
| 资源注册 | 1, 5, 9 | 注册 API / 事件 / 回调 |
| 权限申请 | 13, 16, 20 | 申请 API / 事件 / 回调权限 |

### P2 — 低优先级（更新 / 配置）

共 **9 个接口**，涉及资源更新、消费参数配置和权限撤回。

| 分类 | # | 操作 |
|------|:-:|------|
| 资源更新 | 2, 6, 10 | 更新 API / 事件 / 回调 |
| 消费配置 | 17, 21 | 配置事件 / 回调消费参数 |
| 权限撤回 | 14, 18, 22 | 撤回 API / 事件 / 回调权限申请 |

---

## 排除范围（不在本次审计日志实现中）

| 模块 | Controller | 非 GET 接口数 | 排除原因 |
|------|-----------|:----------:|---------|
| 审批流程模板管理 | `ApprovalController` | 3 | 审批配置，非资源操作 |
| 审批执行管理 | `ApprovalController` | 5 | 审批决策，有独立审批日志 |
| 分类管理 | `CategoryController` | 5 | 分类管理，非资源操作 |
| 连接器管理 | `ConnectorController` | 4 | 连接器模块 |
| 连接流管理 | `FlowController` | 6 | 连接流模块 |
| 调试代理 | `DebugProxyController` | 1 | 调试用途 |
| 数据同步 | `SyncController` | 4 | 数据迁移工具 |
