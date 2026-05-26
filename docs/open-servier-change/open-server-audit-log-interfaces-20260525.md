# Open-Server 审计日志接口清单（权限订阅）

**文档版本**：3.2  
**创建日期**：2026-05-25  
**更新日期**：2026-05-26  

---

## 概述

本文档整理 open-server 中 **权限订阅管理** 相关的非 GET 接口，作为审计日志实现的范围依据。

**范围说明**：仅覆盖 PermissionController 中权限订阅管理接口。API / 事件 / 回调的资源管理接口（ApiController / EventController / CallbackController）因无 appId 关联，不纳入审计日志。

**统计**：共 1 个 Controller，**11 个非 GET 接口**

| HTTP 方法 | 数量 |
|-----------|-----:|
| POST | 6 |
| PUT | 2 |
| DELETE | 3 |
| **合计** | **11** |

---

## app_id 数据链路

审计日志 `app_id` 字段统一取 `openplatform_app_t.app_id`（varchar 外部业务 ID）。

```
openplatform_app_t
├── id          BIGINT(20)   ──→  subscription.app_id (内部主键，Long)
└── app_id      VARCHAR(100) ──→  审计日志 app_id (外部业务 ID，String)
```

**当前 11 个接口**：路径均含 `{appId}`，直接取 `openplatform_app_t.app_id`（PATH_VARIABLE 策略）。

**扩展场景**：接口路径无 `{appId}` 时，从实体快照提取 numeric `app_id`（`openplatform_app_t.id`），再通过 `AppContextResolver.toExternalId()` 转换为 varchar `app_id`（ENTITY 策略）。

---

## 一、API 权限订阅管理（3 个接口）

**Controller**：`PermissionController`  
**路径前缀**：`/service/open/v2/apps/{appId}/apis`  
**业务说明**：应用对 API 权限的申请、撤回、删除操作，审计日志 app_id 取路径参数 `{appId}`（即 `openplatform_app_t.app_id`）

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 1 | POST | `/service/open/v2/apps/{appId}/apis/subscribe` | `subscribeApiPermissions` | 申请 API 权限（支持批量） | `PermissionSubscribeRequest` | #29 |
| 2 | POST | `/service/open/v2/apps/{appId}/apis/{id}/withdraw` | `withdrawApiSubscription` | 撤回审核中的 API 权限申请 | -- | #30 |
| 3 | DELETE | `/service/open/v2/apps/{appId}/apis/{id}` | `deleteApiSubscription` | 删除终态 API 权限订阅记录 | -- | #31 |

---

## 二、事件权限订阅管理（4 个接口）

**Controller**：`PermissionController`  
**路径前缀**：`/service/open/v2/apps/{appId}/events`  
**业务说明**：应用对事件权限的申请、配置、撤回、删除操作，审计日志 app_id 取路径参数 `{appId}`（即 `openplatform_app_t.app_id`）

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 4 | POST | `/service/open/v2/apps/{appId}/events/subscribe` | `subscribeEventPermissions` | 申请事件权限（支持批量） | `PermissionSubscribeRequest` | #34 |
| 5 | PUT | `/service/open/v2/apps/{appId}/events/{id}/config` | `configEventSubscription` | 配置事件消费参数（通道/地址/认证） | `SubscriptionConfigRequest` | #35 |
| 6 | POST | `/service/open/v2/apps/{appId}/events/{id}/withdraw` | `withdrawEventSubscription` | 撤回审核中的事件权限申请 | -- | #36 |
| 7 | DELETE | `/service/open/v2/apps/{appId}/events/{id}` | `deleteEventSubscription` | 删除终态事件权限订阅记录 | -- | #37 |

---

## 三、回调权限订阅管理（4 个接口）

**Controller**：`PermissionController`  
**路径前缀**：`/service/open/v2/apps/{appId}/callbacks`  
**业务说明**：应用对回调权限的申请、配置、撤回、删除操作，审计日志 app_id 取路径参数 `{appId}`（即 `openplatform_app_t.app_id`）

| # | HTTP | 完整路径 | 方法名 | 操作说明 | 请求体 | 接口编号 |
|:-:|------|---------|--------|---------|--------|:------:|
| 8 | POST | `/service/open/v2/apps/{appId}/callbacks/subscribe` | `subscribeCallbackPermissions` | 申请回调权限（支持批量） | `PermissionSubscribeRequest` | #40 |
| 9 | PUT | `/service/open/v2/apps/{appId}/callbacks/{id}/config` | `configCallbackSubscription` | 配置回调消费参数 | `SubscriptionConfigRequest` | #41 |
| 10 | POST | `/service/open/v2/apps/{appId}/callbacks/{id}/withdraw` | `withdrawCallbackSubscription` | 撤回审核中的回调权限申请 | -- | #42 |
| 11 | DELETE | `/service/open/v2/apps/{appId}/callbacks/{id}` | `deleteCallbackSubscription` | 删除终态回调权限订阅记录 | -- | #43 |

---

## 按审计优先级分类

### P0 — 高优先级（删除操作，不可逆）

共 **3 个接口**，涉及权限订阅删除，操作不可逆。

| # | 方法 | 操作 |
|:-:|------|------|
| 3 | `deleteApiSubscription` | 删除 API 权限订阅 |
| 7 | `deleteEventSubscription` | 删除事件权限订阅 |
| 11 | `deleteCallbackSubscription` | 删除回调权限订阅 |

### P1 — 中优先级（订阅申请，触发审批流程）

共 **3 个接口**，涉及权限申请，触发审批流程。

| # | 方法 | 操作 |
|:-:|------|------|
| 1 | `subscribeApiPermissions` | 申请 API 权限 |
| 4 | `subscribeEventPermissions` | 申请事件权限 |
| 8 | `subscribeCallbackPermissions` | 申请回调权限 |

### P2 — 低优先级（撤回 / 配置）

共 **5 个接口**，涉及权限撤回和消费参数配置。

| # | 方法 | 操作 |
|:-:|------|------|
| 2 | `withdrawApiSubscription` | 撤回 API 权限申请 |
| 6 | `withdrawEventSubscription` | 撤回事件权限申请 |
| 10 | `withdrawCallbackSubscription` | 撤回回调权限申请 |
| 5 | `configEventSubscription` | 配置事件消费参数 |
| 9 | `configCallbackSubscription` | 配置回调消费参数 |

---

## 排除范围（不在本次审计日志实现中）

| 模块 | Controller | 非 GET 接口数 | 排除原因 |
|------|-----------|:----------:|---------|
| API 资源管理 | `ApiController` | 4 | 资源管理类，无 appId |
| 事件资源管理 | `EventController` | 4 | 资源管理类，无 appId |
| 回调资源管理 | `CallbackController` | 4 | 资源管理类，无 appId |
| 审批流程模板管理 | `ApprovalController` | 3 | 审批配置，非资源操作 |
| 审批执行管理 | `ApprovalController` | 5 | 审批决策，有独立审批日志 |
| 分类管理 | `CategoryController` | 5 | 分类管理，非资源操作 |
| 连接器管理 | `ConnectorController` | 4 | 连接器模块 |
| 连接流管理 | `FlowController` | 6 | 连接流模块 |
| 调试代理 | `DebugProxyController` | 1 | 调试用途 |
| 数据同步 | `SyncController` | 4 | 数据迁移工具 |

---

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0 | 2026-05-25 | 初始版本，整理 51 个非 GET 接口 |
| 2.0 | 2026-05-25 | 缩小范围至 API / 事件 / 回调，23 个接口 |
| 3.0 | 2026-05-26 | 移除资源管理类接口（无 appId），仅保留权限订阅 11 个接口 |
| 3.1 | 2026-05-26 | 新增 app_id 数据链路说明：审计日志 app_id 取 openplatform_app_t.app_id (varchar 外部业务 ID) |
| 3.2 | 2026-05-26 | 同步技术方案 v2.3.0：SUBSCRIBE 操作 afterData 改为从返回值提取订阅记录（非 null）；注解统一使用 `value =` 语法 |
