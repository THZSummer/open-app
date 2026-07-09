# 审批设计：连接器平台 V3 — 审批引擎统一接入

**Feature ID**: CONN-PLAT-003
**关联文档**: [spec.md](./spec.md) v3.0 §3.6, [plan-config.md](./plan-config.md), [review-approval.md](./review-approval.md)
**版本**: v1.0
**创建日期**: 2026-07-09
**说明**: 系统化记录全部 8 种审批业务类型的统一设计，覆盖模板叠加策略、审批层级路由、回调节点、DB 模型。本文档取代 review-approval.md 中的优化建议，为该建议的落地设计。

---

## 目录

- [1 业务场景总览](#1-业务场景总览)
- [2 审批模板叠加模型](#2-审批模板叠加模型)
- [3 审批层级路由策略](#3-审批层级路由策略)
- [4 引擎核心流程](#4-引擎核心流程)
- [5 数据模型](#5-数据模型)
- [6 回调设计](#6-回调设计)
- [7 接口与生效路径](#7-接口与生效路径)
- [8 附录：与 V2 标准体系的融合说明](#8-附录与-v2-标准体系的融合说明)
- [9 修订记录](#9-修订记录)

---

## 1 业务场景总览

open-server 审批引擎支持 8 种业务类型，覆盖能力开放平台（V2）和连接器平台（V3）：

| # | businessType | 分类 | 层级 | 说明 | 来源 |
|---|-------------|------|:---:|------|:---:|
| 1 | `api_register` | 资源注册 | 2 级 | API 注册审批 | V2 |
| 2 | `event_register` | 资源注册 | 2 级 | 事件注册审批 | V2 |
| 3 | `callback_register` | 资源注册 | 2 级 | 回调注册审批 | V2 |
| 4 | `api_permission_apply` | 权限申请 | 3 级 | API 权限申请审批 | V2 |
| 5 | `event_permission_apply` | 权限申请 | 3 级 | 事件权限申请审批 | V2 |
| 6 | `callback_permission_apply` | 权限申请 | 3 级 | 回调权限申请审批 | V2 |
| 7 | `app_version_publish` | 版本发布 | 可选 | 应用版本发布（无审批人时免审） | V2 |
| 8 | `connector_flow_version_publish` | 版本发布 | 2 级 | 连接流版本发布审批 | **V3** |

> **关键区分**：
> - 层级数由 `businessType` 决定（`*_register` → 2 级，`*_permission_apply` → 3 级，`app_version_publish` → 可选）。
> - 每级内部是否叠加多个模板由两个正交维度决定：`code`（场景维度，`global`=全部场景）和 `appId`（应用范围维度，NULL=全部应用范围），详见 §2。

---

## 2 审批模板叠加模型

### 2.1 核心设计：两个正交维度

审批模板由 **code**（场景维度）和 **appId**（应用范围维度）两个字段组合定位：

| 维度 | 字段 | 值 | 语义 |
|---|---|---|---|
| 场景维度 | `code` | 具体场景编码（如 `connector_flow_version_publish`） | 仅该场景生效 |
| | `code` | **`global`** | **全部场景**生效（全局审批，所有场景的最后一关） |
| 应用范围维度 | `appId` | 具体值（如 328...） | 仅该应用生效 |
| | `appId` | **NULL** | **全部应用范围**生效 |

> **关键区分**：
> - `code = 'global'` 指全部场景（不区分 api/event/connector_flow，所有审批的最后一级）。
> - `appId = NULL` 指全部应用范围（不区分 app A / app B，所有应用共享此模板）。

两个维度是正交的，可组合出 4 种模板：

| code | appId | 生效范围 |
|---|---|---|
| `connector_flow_version_publish` | 328...（应用A） | 仅应用 A 的连接流版本发布场景 |
| `connector_flow_version_publish` | NULL | 全部应用的连接流版本发布场景 |
| `global` | 328...（应用A） | 应用 A 的全部场景 |
| `global` | NULL | 全部应用的全部场景（最宽泛） |

### 2.2 示例：连接流版本发布

用户发布了应用 A 的连接流版本，审批模板配置如下：

| code | appId | nodes | 来源 |
|---|---|---|---|
| `connector_flow_version_publish` | 328...(应用A) | [张三] | 应用 A 专属 |
| `connector_flow_version_publish` | NULL | [李四] | 全部应用范围 |
| `global` | NULL | [王五] | 全部场景 + 全部应用范围 |

生效的审批链：

```
第1级 (scene): [张三(应用专属), 李四(全部应用范围)]
    ↓ 两人都需审批通过
第2级 (global): [王五(全部场景)]
    ↓ 需审批通过
→ 版本发布
```

### 2.3 叠加规则

V2 时期 `selectByCode(code)` 仅按 code 查一个模板。V3 引入 `appId` 后，同一场景可配置应用专属和全部应用范围两条模板。

**叠加策略**：两条都查，有节点的都加入，合并为同一级审批节点列表。不为优选（二选一）。

```
scene 层 = selectByCodeAndAppId(sceneCode, appId)   ← 应用专属（有则加入）
         + selectByCodeAndAppId(sceneCode, null)    ← 全部应用范围（有则加入）

global 层 = selectByCodeAndAppId("global", appId)   ← 应用专属全局
          + selectByCodeAndAppId("global", null)    ← 全部应用范围 + 全部场景
```

> 空模板（未配置或 nodes 为空）不贡献节点。最终节点数为 0 且非 OPTIONAL 类型时，`createApproval()` 抛出 400。

假设 global(null) 始终配置了审批人，场景层的 4 种配置效果：

| 配置情况 | scene(app) | scene(null) | 结果 |
|---|---|---|---|
| 仅应用专属 | ✅ | ❌ | scene(app) → global(null) |
| 仅全部应用范围 | ❌ | ✅ | scene(null) → global(null) |
| 两者叠加 | ✅ | ✅ | scene(app) + scene(null) → global(null) |
| 两者皆空 | ❌ | ❌ | global(null)（场景层免审） |

---

## 3 审批层级路由策略

### 3.1 `composeApprovalNodes()` 路由表

`ApprovalEngine.composeApprovalNodes(businessType, permissionId, appId)` 是唯一入口。路由逻辑：

```
OPTIONAL_APPROVER_TYPES.contains(businessType)? → 返回空列表（免审）
    ↓ No
businessType.endsWith("_register")? → 2级：scene(appId叠加) + global(appId叠加)
    ↓ No
businessType.endsWith("_permission_apply")? → 3级：resource(permissionId) + scene(appId叠加) + global(appId叠加)
    ↓ No
default → 2级：scene(appId叠加) + global(appId叠加)
```

| businessType | 路由分支 | 第一级来源 | 第二级 | 第三级 |
|---|---|---|---|---|
| `api_register` / `event_register` / `callback_register` | `_register` | scene(code+appId叠加) | global(appId叠加) | — |
| `api_permission_apply` / `event_permission_apply` / `callback_permission_apply` | `_permission_apply` | resource(permission.resourceNodes) | scene(code+appId叠加) | global(appId叠加) |
| `app_version_publish` | OPTIONAL | — | — | — |
| `connector_flow_version_publish` | default (2级) | scene(code+appId叠加) | global(appId叠加) | — |

### 3.2 scene code 映射

`getSceneCodeByBusinessType(businessType)` 返回模板的 `code` 字段值：

| businessType | sceneCode |
|---|---|
| `api_register` | `"api_register"` |
| `event_register` | `"event_register"` |
| `callback_register` | `"callback_register"` |
| `api_permission_apply` | `"api_permission_apply"` |
| `event_permission_apply` | `"event_permission_apply"` |
| `callback_permission_apply` | `"callback_permission_apply"` |
| `app_version_publish` | `"app_version_publish"` |
| `connector_flow_version_publish` | `"connector_flow_version_publish"` |

### 3.3 叠加查询伪代码

```java
List<ApprovalNodeDto> getSceneApprovalNodes(String businessType, Long appId) {
    String code = getSceneCodeByBusinessType(businessType);
    List<ApprovalNodeDto> result = new ArrayList<>();
    if (appId != null) result.addAll(loadFlowNodes(code, appId));   // 应用专属
    result.addAll(loadFlowNodes(code, null));                        // 全部应用范围
    return result;
}

List<ApprovalNodeDto> getGlobalApprovalNodes(Long appId) {
    List<ApprovalNodeDto> result = new ArrayList<>();
    if (appId != null) result.addAll(loadFlowNodes("global", appId));  // 应用专属全局
    result.addAll(loadFlowNodes("global", null));                       // 全部场景
    return result;
}

List<ApprovalNodeDto> loadFlowNodes(String code, Long appId) {
    ApprovalFlow flow = flowMapper.selectByCodeAndAppId(code, appId);
    if (flow == null) return Collections.emptyList();
    return parseNodes(flow.getNodes());
}
```

> `selectByCodeAndAppId(code, null)` 等价于旧版 `selectByCode(code)`（仅按 code 查，无需 `appId IS NULL` 兜底），存量 6 种类型传 `appId=null` 时行为不变。

---

## 4 引擎核心流程

### 4.1 完整时序

```
submitApproval(businessType, businessId, appId)
  │
  ├→ 1. 业务前置动作（各 Service 自行处理，非引擎职责）
  │     例: FlowVersionService 将版本状态 DRAFT → PENDING_APPROVAL
  │
  ├→ 2. composeApprovalNodes(businessType, permissionId, appId)   ← 统一入口
  │      ├→ 路由分支判断（_register / _permission_apply / default / optional）
  │      ├→ getResourceApprovalNodes(permissionId)                 ← 仅 _permission_apply
  │      ├→ getSceneApprovalNodes(businessType, appId)             ← appId 叠加
  │      └→ getGlobalApprovalNodes(appId)                          ← appId 叠加
  │
  ├→ 3. createApproval() ← 统一入口
  │      ├→ combinedNodes.isEmpty()? → 非 OPTIONAL 则抛异常
  │      ├→ serializeNodes → JSON
  │      └→ insert ApprovalRecord(combinedNodes)
  │
  └→ 4. 返回 ApprovalRecord
```

### 4.2 审批执行

```
approve(recordId, operatorId, ...) / reject(recordId, ...) / cancel(recordId, ...)
  │
  ├→ 1. 查询 ApprovalRecord
  ├→ 2. 从 combinedNodes 解析当前节点
  ├→ 3. 记录 ApprovalLog (含 level)
  ├→ 4. 推进 currentNode 或标记 APPROVED/REJECTED/CANCELLED
  ├→ 5. updateResourceStatus(record, status)    ← 引擎内联回调，同事务
  └→ 6. updateSubscriptionStatus(record, status) ← 仅 _permission_apply
```

---

## 5 数据模型

### 5.1 审批流模板表 `openplatform_v2_approval_flow_t`

| 列 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT(20) PK | Snowflake ID |
| `name_cn` | VARCHAR(100) | 中文名称 |
| `name_en` | VARCHAR(100) | 英文名称 |
| **`code`** | VARCHAR(50) | 流程编码（sceneCode 或 `"global"`） |
| **`app_id`** | BIGINT(20) NULL | NULL=全部应用范围，具体值=应用专属（内部 App.id） |
| `nodes` | VARCHAR(2000) | JSON 审批人列表 |
| `status` | TINYINT | 0=禁用, 1=启用 |
| `create_time` / `last_update_time` | DATETIME(3) | |
| `create_by` / `last_update_by` | VARCHAR(100) | |

**唯一约束**（V3 变更）：
```sql
-- V2（旧）:
UNIQUE KEY uk_code (code)

-- V3（新）:
ALTER TABLE DROP INDEX uk_code;
ALTER TABLE ADD COLUMN app_id BIGINT(20) NULL;
ALTER TABLE ADD UNIQUE KEY uk_code_app (code, app_id);
```

> 允许 `(code='connector_flow_version_publish', app_id=NULL)` 和 `(code='connector_flow_version_publish', app_id=328...)` 同时存在。

### 5.2 nodes JSON 格式

```json
[
  {
    "type": "approver",
    "userId": "zhangsan",
    "userName": "张三",
    "order": 1,
    "level": "scene"
  }
]
```

### 5.3 审批记录表 `openplatform_v2_approval_record_t`

| 列 | 说明 |
|---|---|
| `id` | 记录 ID |
| `combined_nodes` | 序列化的完整审批链 JSON（从模板查询后固化） |
| `business_type` | 业务类型（如 `connector_flow_version_publish`） |
| `business_id` | 业务对象 ID（如 FlowVersion.id） |
| `applicant_id` / `applicant_name` | 申请人 |
| `status` | 0=待审, 1=通过, 2=驳回, 3=撤销 |
| `current_node` | 当前审批节点索引 |
| `completed_at` | 完成时间 |

---

## 6 回调设计

### 6.1 引擎内联回调

所有 8 种类型的审批结果回调统一在 `ApprovalEngine.updateResourceStatus()` 中处理，**与审批执行在同一事务内**。

```
审批通过 → updateResourceStatus(record, APPROVED)
  ├─ api_register:          API.status = 已发布(2)
  ├─ event_register:        Event.status = 已发布(2)
  ├─ callback_register:     Callback.status = 已发布(2)
  ├─ app_version_publish:   AppVersion.status = 已发布(4); 驳回→待发布(1)
  ├─ *_permission_apply:    跳过（由 updateSubscriptionStatus 处理）
  └─ connector_flow_version_publish:
       ├─ APPROVED:  FlowVersion.status = 已发布(5), publishedTime, publishedBy
       ├─ REJECTED:  FlowVersion.status = 已驳回(4)
       └─ CANCELLED: FlowVersion.status = 已撤回(3)
```

### 6.2 FlowVersion 状态与审批结果对应

| 审批结果 | FlowVersion.status |
|---|---|
| 提交审批 | DRAFT(1) → PENDING_APPROVAL(2) |
| 全部通过 | PENDING_APPROVAL(2) → PUBLISHED(5) |
| 驳回 | PENDING_APPROVAL(2) → REJECTED(4) |
| 撤回 | PENDING_APPROVAL(2) → WITHDRAWN(3) |

---

## 7 接口与生效路径

### 7.1 管理面：审批流模板 CRUD

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/approval-flows` | 列表（?keyword, ?appId 过滤） |
| GET | `/approval-flows/{id}` | 详情（含 appId） |
| POST | `/approval-flows` | 创建（body 含 code, appId, nodes） |
| PUT | `/approval-flows/{id}` | 更新（可修改 appId） |
| DELETE | `/approval-flows/{id}` | 删除 |

> 权限：`@PlatformAdminPermission`。

### 7.2 执行面：审批操作

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/approvals/pending` | 待审批列表（?businessType 过滤） |
| GET | `/approvals/{id}` | 审批详情（含 combinedNodes） |
| POST | `/approvals/{id}/approve` | 通过 |
| POST | `/approvals/{id}/reject` | 驳回 |
| POST | `/approvals/{id}/cancel` | 撤销（审批中心入口） |
| POST | `/approvals/{id}/urge` | 催办 |

### 7.3 发布入口（触发审批）

| 方法 | 路径 | businessType |
|---|---|---|
| POST | `/flows/{flowId}/versions/{versionId}/publish` | `connector_flow_version_publish` |

> 当前 scope：仅 `connector_flow_version_publish` 场景使用 `appId` 叠加。存量 6 种类型传 `appId=null`，行为不变。

---

## 8 附录：与 V2 标准体系的融合说明

V3 连接流版本发布审批接入标准审批引擎后，review-approval.md 中记录的偏离处置如下：

| # | 偏离项 | 处置 |
|---|--------|:--:|
| 1 | BusinessType 未注册 | ✅ 已注册到 `ApprovalEngine.BusinessType` |
| 2 | 绕过 composeApprovalNodes | ✅ 走引擎统一入口 |
| 3 | 绕过 createApproval | ✅ 走引擎统一入口 |
| 4 | 非标准 level 值 | ✅ 引擎返回标准 `scene`/`global`/`resource` |
| 5 | 独立回调机制 | ✅ 并入 `updateResourceStatus`，同事务 |
| 6 | 取消/催办重复 | ⚠️ 保留 — 两条入口路径是设计意图（审批中心 vs 业务详情页） |
| 7 | 审批前状态管理 | ⚠️ 保留 — `DRAFT → PENDING_APPROVAL` 是各 Service 业务前置动作 |
| 8 | 模板匹配引入 appId | ✅ **变为特性** — 引擎统一支持的叠加维度 |
| 9 | Controller 耦合 | ✅ 消除 — Controller 回归纯粹路由 |
| 10 | 两套取消入口 | ⚠️ 保留 — 同偏离 6 |

---

## 9 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始版本：8 种审批业务类型统一设计，叠加模型、路由策略、回调方案 | 2026-07-09 | SDDU |
