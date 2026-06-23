# 连接流版本发布审批 — 架构审查报告

> 审查日期：2026-06-23
> 审查范围：open-server 审批模块，聚焦 `connector_flow_version_publish` 与标准 6 种业务场景的差异
> 版本：V3（连接器平台）

---

## 一、背景

open-server 审批模块最初为 V2 能力开放平台设计，支持 6 种标准业务类型：

| 分类 | businessType | 层级 |
|------|-------------|:---:|
| 资源注册 | `api_register` / `event_register` / `callback_register` | 2 级（场景→全局） |
| 权限申请 | `api_permission_apply` / `event_permission_apply` / `callback_permission_apply` | 3 级（资源→场景→全局） |

V3 连接器平台新增第 7 种业务类型 `connector_flow_version_publish`（连接流版本发布审批），引入了应用级审批模板、独立回调机制等新特性。

本文档记录该场景的实现方式与原有规范的偏离点，并提出统一化建议。

---

## 二、标准审批体系（V2）

### 2.1 核心组件

```
┌─────────────────────────────────────────────────┐
│  ApprovalController  ← HTTP 入口                  │
│         ↓                                        │
│  ApprovalService     ← 业务编排                    │
│         ↓                                        │
│  ApprovalEngine      ← 核心引擎                    │
│    ├─ composeApprovalNodes()  ← 节点组合（统一入口）│
│    ├─ createApproval()        ← 记录创建（统一入口）│
│    ├─ approve() / reject() / cancel()  ← 审批执行  │
│    ├─ updateResourceStatus()   ← 注册回调（内联）   │
│    └─ updateSubscriptionStatus() ← 权限回调（内联） │
└─────────────────────────────────────────────────┘
```

### 2.2 设计原则

1. **统一入口**：所有业务类型通过 `ApprovalEngine.composeApprovalNodes()` 组合节点，通过 `createApproval()` 创建记录
2. **引擎内联回调**：审批通过/驳回后的业务状态变更在引擎内部完成（`updateResourceStatus` / `updateSubscriptionStatus`）
3. **Controller 透明**：Controller 层不感知具体业务类型的回调逻辑
4. **Level 枚举统一**：`ApprovalEngine.Level` 定义 `resource` / `scene` / `global` 三种级别
5. **BusinessType 集中注册**：所有类型定义在 `ApprovalEngine.BusinessType` 中
6. **模板匹配**：仅按 `code` 匹配审批流程模板（`selectByCode`）

---

## 三、V3 连接流版本发布的实现方式

### 3.1 新增组件

```
┌──────────────────────────────────────────────────┐
│  FlowVersionApprovalService  ← 独立审批服务        │
│    ├─ submitApproval()       ← 提交审批（绕过引擎） │
│    ├─ cancelApproval()       ← 取消审批（重复实现） │
│    └─ urgeApproval()         ← 催办审批（重复实现） │
│                                                   │
│  ApprovalCallbackHandler     ← 独立回调处理器       │
│    ├─ onApproved()           ← 审批通过 → 发布版本  │
│    └─ onRejected()           ← 审批驳回 → 标记驳回  │
│                                                   │
│  ConnectorPlatformConstants  ← 常量定义             │
│    └─ APPROVAL_BUSINESS_TYPE_FLOW_VERSION_PUBLISH  │
└──────────────────────────────────────────────────┘
```

### 3.2 审批层级

```
应用级审批 (app)       ← code + appId 精确匹配
    ↓
平台级审批 (platform)  ← code 匹配，appId IS NULL
    ↓
全局审批 (global)      ← code='global'，appId IS NULL
```

---

## 四、偏离点详细分析

### 偏离 1：BusinessType 未注册到引擎枚举

**现状**：
- `ApprovalEngine.BusinessType` 只定义了 6 种标准类型
- `connector_flow_version_publish` 定义在 `ConnectorPlatformConstants` 中，与引擎枚举完全隔离

**风险**：如果该类型意外进入 `composeApprovalNodes()`，会落入 `else` 分支被当作"未知类型"以默认 2 级处理。

```java
// ApprovalEngine.composeApprovalNodes() 的分支逻辑
if (isRegisterApproval)      { /* 2级 */ }
else if (isPermissionApply)  { /* 3级 */ }
else {
    // ← connector_flow_version_publish 会落入这里
    log.warn("Unknown business type: {}, using default two-level approval");
}
```

---

### 偏离 2：绕过引擎的 composeApprovalNodes()

**标准路径**：所有 6 种类型统一走 `ApprovalEngine.composeApprovalNodes(businessType, permissionId)`，引擎内部根据 `businessType` 后缀判断层级。

**V3 路径**：`FlowVersionApprovalService` 自行实现 `composeApprovalNodes(appId)`，按 `appId` 三级回退匹配，逻辑完全不兼容。

| 维度 | 标准 6 种 | V3 流版本发布 |
|------|----------|-------------|
| 层级判断依据 | `businessType` 后缀 | `appId` 回退 |
| 第一级来源 | `Permission.resourceNodes` 或 `ApprovalFlow` 表 | `ApprovalFlow` 表（code+appId） |
| 模板匹配 | `selectByCode(code)` | `selectByCodeAndAppId(code, appId)` |

---

### 偏离 3：绕过引擎的 createApproval()

**标准路径**：`approvalEngine.createApproval(businessType, permissionId, businessId, ...)` 统一管控记录创建。

**V3 路径**：`FlowVersionApprovalService.submitApproval()` 手动 new `ApprovalRecord`、手动 set 所有字段、手动 insert。

```java
// V3 手动拼装（绕过了引擎的统一入口）
ApprovalRecord record = new ApprovalRecord();
record.setId(idGenerator.nextId());
record.setCombinedNodes(combinedNodesJson);
record.setBusinessType(...);
// ... 手动 set 所有字段
recordMapper.insert(record);
```

---

### 偏离 4：使用非标准 level 值

**标准**：`ApprovalEngine.Level` 定义 `resource` / `scene` / `global`

**V3**：使用 `"app"` / `"platform"` / `"global"`

```java
// FlowVersionApprovalService.composeApprovalNodes()
node.setLevel("app");       // ❌ 不在 Level 枚举中
node.setLevel("platform");  // ❌ 不在 Level 枚举中
node.setLevel("global");    // ✅ 唯一重合的
```

| 场景 | level 语义 |
|------|-----------|
| 标准 6 种 | 审批的**业务层级**（资源→场景→全局） |
| V3 流版本 | 审批模板的**来源层级**（应用→平台→全局） |

两者虽然都叫 level，但含义完全不同。前端展示、日志查询、权限判断需要特殊处理。

---

### 偏离 5：独立回调机制

**标准**：回调内联在 `ApprovalEngine` 中，对 Controller 透明。

```java
// ApprovalEngine.approve() 内部
updateResourceStatus(record, Status.APPROVED);     // 注册场景
updateSubscriptionStatus(record, Status.APPROVED);  // 权限申请场景
```

**V3**：使用独立的 `ApprovalCallbackHandler`，由 Controller 层显式触发。

```java
// ApprovalController.approve() — V3 新增代码
ApprovalRecord record = approvalRecordMapper.selectById(Long.parseLong(id));
if (record.getStatus() == Status.APPROVED) {
    approvalCallbackHandler.onApproved(record);  // ← Controller 感知业务类型
}
```

**问题**：Controller 层需要注入 `ApprovalCallbackHandler` 和 `ApprovalRecordMapper`，并在通用审批接口中硬编码 V3 回调逻辑。

---

### 偏离 6：取消/催办逻辑重复实现

标准场景的取消和催办走 `ApprovalService` → `ApprovalEngine`。V3 在 `FlowVersionApprovalService` 中重复实现了取消和催办，且额外管理 FlowVersion 状态。

| 操作 | 标准路径 | V3 路径 | 额外副作用 |
|------|---------|---------|-----------|
| 取消 | Controller → Service → Engine | FlowVersionApprovalService.cancelApproval() | 更新 FlowVersion.status = WITHDRAWN |
| 催办 | ApprovalService.urge() | FlowVersionApprovalService.urgeApproval() | FlowVersion 状态校验 |

---

### 偏离 7：审批前状态管理耦合

**标准**：创建审批记录时不修改业务对象状态（资源注册时资源已是草稿，权限申请时订阅已是待审批）。

**V3**：提交审批时主动修改 FlowVersion 状态，与审批记录创建耦合在同一事务中。

```java
// FlowVersionApprovalService.submitApproval()
version.setStatus(FlowVersionStatus.PENDING_APPROVAL.getCode());  // 草稿 → 待审批
flowVersionMapper.update(version);
recordMapper.insert(record);  // 同一事务
```

---

### 偏离 8：模板匹配引入 appId 维度

**标准**：仅按 `code` 匹配模板（`selectByCode`）。

**V3**：引入 `code + appId` 联合匹配（`selectByCodeAndAppId`），需要数据库 schema 变更。

```sql
-- V3 migration 对核心表的修改
ALTER TABLE openplatform_v2_approval_flow_t
    DROP INDEX uk_code,
    ADD COLUMN app_id BIGINT(20) NULL,
    ADD UNIQUE KEY uk_code_app (code, app_id);
```

这允许 `(code='global', app_id=NULL)` 和 `(code='global', app_id=123)` 同时存在，改变了唯一约束语义。

---

### 偏离 9：Controller 层耦合 V3 逻辑

```java
// ApprovalController — 注入了 V3 专属依赖
private final ApprovalCallbackHandler approvalCallbackHandler;  // V3 专属
private final ApprovalRecordMapper approvalRecordMapper;        // 原不属于 Controller
```

Controller 本应只做路由转发，现在直接依赖 V3 组件。

---

### 偏离 10：两套取消入口并存

| 入口 | 路径 | 适用场景 | 副作用 |
|------|------|----------|--------|
| `POST /approvals/{id}/cancel` | Controller → Service → Engine | 标准 6 种 | 仅改 ApprovalRecord |
| `FlowVersionApprovalService.cancelApproval()` | 直接调用 | V3 流版本 | 额外改 FlowVersion |

---

## 五、偏离总览

```
                    标准 6 种（V2）             流版本发布（V3）                 评估
                    ──────────────              ──────────────                  ────
节点组合            Engine.composeApprovalNodes  FlowVersionApprovalService      ❌ 绕过
记录创建            Engine.createApproval        FlowVersionApprovalService      ❌ 绕过
审批执行            Engine.approve/reject/cancel Engine.approve/reject/cancel    ✅ 复用
回调处理            引擎内联                      ApprovalCallbackHandler         ❌ 独立
level 值            resource/scene/global        app/platform/global             ❌ 不同
模板匹配            selectByCode(code)           selectByCodeAndAppId(code,appId)❌ 扩展
BusinessType 注册   Engine.BusinessType 枚举      ConnectorPlatformConstants      ❌ 未注册
取消入口            Controller → Engine          FlowVersionApprovalService      ❌ 重复
催办入口            ApprovalService.urge         FlowVersionApprovalService      ❌ 重复
审批前状态管理      不修改业务对象                修改 FlowVersion 状态            ❌ 新增
DB 影响             无                           ALTER TABLE 改唯一约束           ❌ 改表
Controller 耦合     无                           @Autowired V3 组件               ❌ 耦合
```

---

## 六、风险评估

| 风险 | 严重度 | 说明 |
|------|:---:|------|
| 两套体系并存 | 🔴 高 | 后续新增业务场景时，开发者需判断走 V2 还是 V3 路径，容易选错 |
| level 语义分裂 | 🟡 中 | `"app"`/`"platform"` 不在 `Level` 枚举中，前端展示、日志查询需特殊处理 |
| Controller 耦合 | 🟡 中 | 通用审批接口中混入 V3 回调，违反单一职责原则 |
| 取消/催办重复 | 🟡 中 | 两套实现需同步维护，容易遗漏 |
| BusinessType 分散 | 🟢 低 | 常量定义在两个类中，运行时不影响 |
| DB 唯一约束变更 | 🟢 低 | 向下兼容（`app_id IS NULL` 等价于旧行为），但语义上允许同名 code 多行 |

---

## 七、优化建议

### 7.1 短期（向后兼容，降低风险）

| 优先级 | 建议 | 说明 |
|:---:|------|------|
| P0 | 将 `connector_flow_version_publish` 注册到 `ApprovalEngine.BusinessType` | 让引擎感知该类型存在，避免落入 else 分支 |
| P0 | 将 `"app"` / `"platform"` 加入 `ApprovalEngine.Level` 枚举 | 统一 level 语义，消除前端/日志的特殊处理 |
| P1 | 将 `ApprovalCallbackHandler` 的回调逻辑下沉到 `ApprovalEngine` | 像 `updateResourceStatus` 一样内联处理，消除 Controller 耦合 |
| P1 | 统一取消/催办入口 | 让 `FlowVersionApprovalService` 委托给 `ApprovalService` 而非直接操作 Mapper |

### 7.2 长期（架构统一）

如果后续还有更多需要 `appId` 维度的审批场景，建议将 V3 的模式**反向合并到引擎**：

1. **扩展 `composeApprovalNodes()`**：支持 `appId` 参数，当 `businessType` 为 `connector_flow_version_publish` 时走 appId 三级回退
2. **扩展 `createApproval()`**：支持 `appId` 参数，统一记录创建入口
3. **回调策略模式**：将 `updateResourceStatus` / `updateSubscriptionStatus` / `ApprovalCallbackHandler` 统一为 `ApprovalCallback` 接口，由引擎根据 `businessType` 分发
4. **Controller 回归纯粹**：移除 Controller 中的 `ApprovalCallbackHandler` 和 `ApprovalRecordMapper` 依赖

### 7.3 目标架构

```
ApprovalEngine
├─ composeApprovalNodes(businessType, permissionId, appId)  ← 统一入口
├─ createApproval(...)                                       ← 统一入口
├─ approve() / reject() / cancel()                           ← 统一执行
└─ ApprovalCallback (接口)
    ├─ ResourceRegisterCallback      (api/event/callback_register)
    ├─ PermissionApplyCallback       (*_permission_apply)
    └─ FlowVersionPublishCallback    (connector_flow_version_publish)
```

---

## 八、修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始版本，完成 V2/V3 审批体系差异分析 | 2026-06-23 | SDDU 路由调度专家 |
