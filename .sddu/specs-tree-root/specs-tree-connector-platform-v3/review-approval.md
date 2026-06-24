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

#### 5.1 标准 6 种（V2）：引擎内联回调

回调逻辑写在 `ApprovalEngine` 内部，作为审批执行方法的一部分，在**同一事务**中完成。

```
Controller → Service → Engine.approve()
                          │
                          ├─ 记录日志
                          ├─ 更新 ApprovalRecord 状态
                          ├─ updateResourceStatus()     ← 回调：API/Event/Callback 状态变更
                          └─ updateSubscriptionStatus() ← 回调：Subscription 状态变更
```

**`updateResourceStatus()` — 资源注册回调**（`ApprovalEngine` 第 642 行）：

```java
private void updateResourceStatus(ApprovalRecord record, int status) {
    // 审批通过→已发布(2)，拒绝/撤销→草稿(0)
    int resourceStatus = (status == Status.APPROVED) ? 2 : 0;

    switch (businessType) {
        case "api_register":      apiMapper.update(api);       break;
        case "event_register":    eventMapper.update(event);   break;
        case "callback_register": callbackMapper.update(cb);   break;
        case "*_permission_apply": /* 跳过，由 updateSubscriptionStatus 处理 */ break;
        default: log.warn("Unknown");  // ← connector_flow_version_publish 落入这里，被忽略
    }
}
```

**`updateSubscriptionStatus()` — 权限申请回调**（`ApprovalEngine` 第 706 行）：

```java
private void updateSubscriptionStatus(ApprovalRecord record, int status) {
    // 仅处理 api/event/callback_permission_apply
    if (!是权限申请类型) return;

    if (status == APPROVED)  subscriptionStatus = 1; // 已授权
    if (status == REJECTED)  subscriptionStatus = 2; // 已拒绝
    if (status == CANCELLED) subscriptionStatus = 3; // 已取消

    subscriptionMapper.update(subscription);
}
```

**特点**：
- 回调与审批执行**同事务**（`@Transactional` 在 `approve()`/`reject()`/`cancel()` 上）
- 通过 `businessType` 的 switch/if 分支分发
- 对 Controller 完全透明

#### 5.2 V3 流版本发布：独立回调处理器

回调逻辑在独立的 `ApprovalCallbackHandler` 中，由 **Controller 层显式调用**，在引擎事务**之外**。

```
Controller.approve()
  │
  ├─ Service.approve() → Engine.approve()     ← 引擎事务（不含 V3 回调）
  │     └─ updateResourceStatus()             ← 遇到 connector_flow_version_publish → 落入 default，忽略
  │
  └─ approvalCallbackHandler.onApproved(record)  ← V3 回调（独立事务）
        └─ flowVersionMapper.update(version)     ← FlowVersion 状态变更
```

**Controller 层触发**（`ApprovalController` 第 262-270 行）：

```java
// 引擎先执行
ApprovalActionResponse data = approvalService.approve(...);

// V3 新增：引擎执行完后，Controller 再手动触发回调
ApprovalRecord record = approvalRecordMapper.selectById(Long.parseLong(id));
if (record.getStatus() == Status.APPROVED) {
    approvalCallbackHandler.onApproved(record);   // 独立事务
}
```

**`ApprovalCallbackHandler.onApproved()`**：

```java
@Transactional(rollbackFor = Exception.class)  // ← 独立事务！
public void onApproved(ApprovalRecord record) {
    if (!"connector_flow_version_publish".equals(record.getBusinessType())) {
        return;  // 非 V3 类型直接忽略
    }
    // 更新 FlowVersion 状态为已发布(5)
    version.setStatus(FlowVersionStatus.PUBLISHED.getCode());
    version.setPublishedTime(new Date());
    flowVersionMapper.update(version);
}
```

**`ApprovalCallbackHandler.onRejected()`**：

```java
@Transactional(rollbackFor = Exception.class)  // ← 独立事务！
public void onRejected(ApprovalRecord record, String rejectReason) {
    if (!"connector_flow_version_publish".equals(record.getBusinessType())) return;
    // 更新 FlowVersion 状态为已驳回(4)
    version.setStatus(FlowVersionStatus.REJECTED.getCode());
    flowVersionMapper.update(version);
}
```

#### 5.3 回调对比总结

| 维度 | 标准 6 种（V2） | V3 流版本发布 |
|------|:--:|:--:|
| **回调位置** | `ApprovalEngine` 内部 | `ApprovalCallbackHandler` 独立类 |
| **触发者** | 引擎自身 | Controller 层显式调用 |
| **事务边界** | 与审批执行**同事务** | **独立事务**（`@Transactional` 单独标注） |
| **分发方式** | `businessType` switch/if 分支 | `businessType` 前置判断 + 直接忽略非 V3 |
| **Controller 感知** | ❌ 不感知 | ✅ 注入 `ApprovalCallbackHandler` + `ApprovalRecordMapper` |
| **回调内容** | API/Event/Callback 状态 / Subscription 状态 | FlowVersion 状态 + 发布时间/发布人 |
| **引擎是否感知** | ✅ 引擎内部方法 | ❌ 引擎 `updateResourceStatus` 的 default 分支直接忽略 |

#### 5.4 回调机制的关键问题

1. **事务隔离风险**：V3 回调在独立事务中，引擎事务已提交。如果回调失败（如 FlowVersion 不存在），审批记录已经是 APPROVED 状态，无法回滚 → **数据不一致**

2. **引擎盲区**：`updateResourceStatus()` 的 `default` 分支对 `connector_flow_version_publish` 只打一行 `log.warn`，不做任何处理。引擎完全不知道这个类型的存在

3. **Controller 越权**：Controller 原本只做参数解析和路由转发，现在直接操作 Mapper 查询 `ApprovalRecord` 并调用业务回调，打破了分层架构

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

## 八、引擎侧统一改造方案（落地实施）

> 核心原则：**不改变存量 6 种类型的任何行为，但让引擎原生感知第 7 种类型和 `appId` 维度。一视同仁。**

---

### 8.1 问题诊断结论

经过代码对照分析，得出三个核心判断：

| 判断 | 代码证据 | 结论 |
|------|---------|:--:|
| 流版本发布场景无特殊性 | `ApprovalService.getBusinessData()` 已用统一 switch 接纳 7 种类型；V3 节点组合结构（三级回退）是通用模式 | ✅ |
| `appId` 应平台级增加 | `selectByCodeAndAppId(code, null)` 等价于 `selectByCode(code)`，存量完全兼容 | ✅ |
| 存量 `appId = NULL` | schema 定义为 `BIGINT NULL`，旧行自然为 NULL，唯一约束 `uk_code_app (code, app_id)` 允许 | ✅ |

**根本原因**：不是流版本发布特殊，而是引擎的设计落后了一步——`BusinessType` 枚举、`composeApprovalNodes()` 方法签名、模板匹配策略都还没接纳 `appId` 这个维度。让引擎原生支持 `appId`，V3 就能回归标准路径，10 项偏离中的 7 项会自然消失。

---

### 8.2 改造点 1：扩展 `BusinessType` 枚举

**文件**：`ApprovalEngine.java` 第 99 行

**当前**（只有 6 个）：
```java
public static class BusinessType {
    public static final String API_REGISTER = "api_register";
    public static final String EVENT_REGISTER = "event_register";
    public static final String CALLBACK_REGISTER = "callback_register";
    public static final String API_PERMISSION_APPLY = "api_permission_apply";
    public static final String EVENT_PERMISSION_APPLY = "event_permission_apply";
    public static final String CALLBACK_PERMISSION_APPLY = "callback_permission_apply";
}
```

**改造后**（新增第 7 个）：
```java
public static class BusinessType {
    public static final String API_REGISTER = "api_register";
    public static final String EVENT_REGISTER = "event_register";
    public static final String CALLBACK_REGISTER = "callback_register";
    public static final String API_PERMISSION_APPLY = "api_permission_apply";
    public static final String EVENT_PERMISSION_APPLY = "event_permission_apply";
    public static final String CALLBACK_PERMISSION_APPLY = "callback_permission_apply";
    // ✅ 新增
    public static final String CONNECTOR_FLOW_VERSION_PUBLISH = "connector_flow_version_publish";
}
```

**影响**：消除 `ConnectorPlatformConstants` 中孤立常量的必要性。存量代码不受影响（只是多了一个常量）。

---

### 8.3 改造点 2：扩展 `composeApprovalNodes()` — 核心

**文件**：`ApprovalEngine.java` 第 139 行

**当前签名**：
```java
public List<ApprovalNodeDto> composeApprovalNodes(String businessType, Long permissionId)
```

**改造后签名**：
```java
public List<ApprovalNodeDto> composeApprovalNodes(String businessType, Long permissionId, Long appId)
```

**关键逻辑变更**——`getSceneApprovalNodes()` 从 `selectByCode` 改为 `selectByCodeAndAppId`：

```java
// 改造前 — 只按 code 匹配
private List<ApprovalNodeDto> getSceneApprovalNodes(String businessType) {
    String sceneCode = getSceneCodeByBusinessType(businessType);
    ApprovalFlow sceneFlow = flowMapper.selectByCode(sceneCode);  // ← 旧
    ...
}

// 改造后 — 支持 code + appId 联合匹配，三级回退
private List<ApprovalNodeDto> getSceneApprovalNodes(String businessType, Long appId) {
    String sceneCode = getSceneCodeByBusinessType(businessType);
    // 1. 先用 code+appId 精确匹配（应用级）
    ApprovalFlow sceneFlow = flowMapper.selectByCodeAndAppId(sceneCode, appId);
    // 2. 若无，回退到 code+NULL（平台级）
    if (sceneFlow == null && appId != null) {
        sceneFlow = flowMapper.selectByCodeAndAppId(sceneCode, null);
    }
    ...
}
```

**`getGlobalApprovalNodes()` 同步改造**：
```java
// 改造前
ApprovalFlow globalFlow = flowMapper.selectByCode("global");

// 改造后 — 全局模板不绑定 appId，始终用 NULL
ApprovalFlow globalFlow = flowMapper.selectByCodeAndAppId("global", null);
```

**向下兼容性**：存量调用方传 `appId = null`，`selectByCodeAndAppId(code, null)` 等价于原 `selectByCode(code)`，**行为完全不变**。

---

### 8.4 改造点 3：扩展 `createApproval()` — 透传 `appId`

**文件**：`ApprovalEngine.java` 第 378 行

**当前签名**：
```java
public ApprovalRecord createApproval(String businessType, Long permissionId, Long businessId,
                                      String applicantId, String applicantName, String operator)
```

**改造后签名**（`appId` 加在末尾，存量调用方只需加一个 `null`）：
```java
public ApprovalRecord createApproval(String businessType, Long permissionId, Long businessId,
                                      String applicantId, String applicantName, String operator,
                                      Long appId)  // ← 新增参数
```

内部调用：
```java
List<ApprovalNodeDto> combinedNodes = composeApprovalNodes(businessType, permissionId, appId);
```

**存量 6 种类型的调用方改动**（每个文件只需在末尾加一个 `null`）：

| 文件 | 行号 | 改动 |
|------|------|------|
| `ApiService.java` | 237 | `createApproval(API_REGISTER, permissionId, apiId, ..., null)` |
| `EventService.java` | 234 | `createApproval(EVENT_REGISTER, permissionId, eventId, ..., null)` |
| `CallbackService.java` | 251 | `createApproval(CALLBACK_REGISTER, permissionId, callbackId, ..., null)` |
| `PermissionService.java` | 291, 523, 789 | `createApproval(*_PERMISSION_APPLY, permissionId, subscriptionId, ..., null)` ×3 |

---

### 8.5 改造点 4：`getSceneCodeByBusinessType` 补全

**文件**：`ApprovalEngine.java` 第 340 行

```java
private String getSceneCodeByBusinessType(String businessType) {
    switch (businessType) {
        case BusinessType.API_REGISTER:              return "api_register";
        case BusinessType.EVENT_REGISTER:            return "event_register";
        case BusinessType.CALLBACK_REGISTER:         return "callback_register";
        case BusinessType.API_PERMISSION_APPLY:      return "api_permission_apply";
        case BusinessType.EVENT_PERMISSION_APPLY:    return "event_permission_apply";
        case BusinessType.CALLBACK_PERMISSION_APPLY: return "callback_permission_apply";
        // ✅ 新增
        case BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH: return "connector_flow_version_publish";
        default:
            log.warn("Unknown business type: {}, using default scene code", businessType);
            return "api_permission_apply";
    }
}
```

---

### 8.6 改造点 5：`updateResourceStatus` 补全回调 — **消除 Controller 耦合的关键**

**文件**：`ApprovalEngine.java` 第 642 行

**当前**：V3 类型落入 `default`，只打 warn，被忽略。

**改造后**：
```java
private void updateResourceStatus(ApprovalRecord record, int status) {
    String businessType = record.getBusinessType();
    Long businessId = record.getBusinessId();
    int resourceStatus = (status == Status.APPROVED) ? 2 : 0;

    try {
        switch (businessType) {
            case BusinessType.API_REGISTER:    ... break;
            case BusinessType.EVENT_REGISTER:  ... break;
            case BusinessType.CALLBACK_REGISTER: ... break;
            case BusinessType.API_PERMISSION_APPLY:
            case BusinessType.EVENT_PERMISSION_APPLY:
            case BusinessType.CALLBACK_PERMISSION_APPLY:
                break;  // 由 updateSubscriptionStatus 处理

            // ✅ 新增：连接流版本发布回调（从 ApprovalCallbackHandler 搬入）
            case BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH:
                handleFlowVersionPublishResult(record, status);
                break;

            default:
                log.warn("Unknown business type: {}", businessType);
        }
    } catch (Exception e) {
        log.error("Failed to update resource status: businessType={}, businessId={}", businessType, businessId, e);
    }
}
```

**新增私有方法**（从 `ApprovalCallbackHandler` 搬过来，逻辑不变）：

```java
/**
 * 处理连接流版本发布审批结果
 *
 * <p>审批通过（APPROVED）→ FlowVersion 状态变更为已发布(5)
 * 审批驳回（REJECTED）→ FlowVersion 状态变更为已驳回(4)
 * 审批撤销（CANCELLED）→ FlowVersion 状态变更为已撤回</p>
 */
private void handleFlowVersionPublishResult(ApprovalRecord record, int status) {
    Long flowVersionId = record.getBusinessId();
    FlowVersion version = flowVersionMapper.selectById(flowVersionId);
    if (version == null) {
        log.error("FlowVersion not found: flowVersionId={}", flowVersionId);
        return;
    }
    if (status == Status.APPROVED) {
        version.setStatus(FlowVersionStatus.PUBLISHED.getCode());
        version.setPublishedTime(new Date());
        version.setPublishedBy(record.getApplicantId());
    } else if (status == Status.REJECTED) {
        version.setStatus(FlowVersionStatus.REJECTED.getCode());
    } else if (status == Status.CANCELLED) {
        version.setStatus(FlowVersionStatus.WITHDRAWN.getCode());
    }
    version.setLastUpdateTime(new Date());
    version.setLastUpdateBy(record.getApplicantId());
    flowVersionMapper.update(version);

    log.info("Flow version publish result handled: flowVersionId={}, status={}, businessType={}",
             flowVersionId, status, record.getBusinessType());
}
```

**关键收益**：
- V3 回调与审批执行在**同一事务**内（`approve`/`reject`/`cancel` 都有 `@Transactional`）
- Controller 不再需要注入 `ApprovalCallbackHandler` 和 `ApprovalRecordMapper`
- `FlowVersionApprovalService.cancelApproval()` 不再需要手动改 FlowVersion 状态（引擎 `cancel()` 已处理）
- 新增依赖：引擎需注入 `OpFlowVersionMapper`

---

### 8.7 改造点 6：简化 `FlowVersionApprovalService`

**文件**：`FlowVersionApprovalService.java`

**改造后 `submitApproval()`**——核心逻辑大幅简化：

```java
@Transactional(rollbackFor = Exception.class)
public ApprovalRecord submitApproval(Long flowVersionId, Long flowId, String flowNameCn,
                                      String flowNameEn, Long appId,
                                      String applicantId, String applicantName) {
    // 1. 校验版本状态（不变）
    FlowVersion version = flowVersionMapper.selectById(flowVersionId);
    if (version == null) {
        throw BusinessException.notFound("版本不存在: " + flowVersionId, ...);
    }
    if (!FlowVersionStatus.DRAFT.getCode().equals(version.getStatus())) {
        throw BusinessException.badRequest("仅草稿状态的版本可提交审批", ...);
    }

    // 2. 更新 FlowVersion 状态为待审批（业务前置动作，保留）
    version.setStatus(FlowVersionStatus.PENDING_APPROVAL.getCode());
    version.setLastUpdateTime(new Date());
    version.setLastUpdateBy(applicantId);
    flowVersionMapper.update(version);

    // 3. ✅ 直接调用引擎统一入口（不再手动 new ApprovalRecord + 手动 insert）
    ApprovalRecord record = approvalEngine.createApproval(
        ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH,
        null,              // permissionId — 流版本发布不涉及权限
        flowVersionId,     // businessId
        applicantId, applicantName, applicantId,
        appId              // ✅ 传入 appId，引擎内部做三级回退
    );

    log.info("Flow version approval submitted: recordId={}, flowVersionId={}, appId={}",
             record.getId(), flowVersionId, appId);

    return record;
}
```

**关于 `cancelApproval()` 的补充说明**：

撤回操作存在两条入口路径，这是**设计意图，非偏离**：

| 入口 | 路径 | 适用场景 |
|------|------|----------|
| 审批中心统一入口 | `ApprovalController.cancel()` → `ApprovalService` → `ApprovalEngine.cancel()` | 用户在审批中心页面操作 |
| 业务对象专属入口 | `FlowVersionApprovalService.cancelApproval()` → `ApprovalEngine.cancel()` + 更新 FlowVersion 状态 | 用户在连接流版本详情页操作 |

**设计原则**：前端是谁的页面，入口在哪个业务对象，就用哪个业务对象的接口。审批中心用统一接口，业务详情页用业务专属接口。

引擎的职责是审批记录本身的状态变更（PENDING → CANCELLED）。业务副作用（如 FlowVersion 状态从 PENDING_APPROVAL → WITHDRAWN）由触发方自行处理。`FlowVersionApprovalService.cancelApproval()` 作为业务方，在调引擎 cancel 后额外更新 FlowVersion 状态是合理的，**无需消除**。

注意：如果引擎的 `updateResourceStatus()` 已补全了 `handleFlowVersionPublishResult(record, CANCELLED)`（见 8.6 节），则统一入口路径也能正确处理 FlowVersion 状态回退，两条路径在业务结果上保持一致。

**可删除的方法**：
- `composeApprovalNodes(Long appId)` — 逻辑已并入引擎
- `getApprovalNodesByCodeAndAppId(String code, Long appId)` — 引擎直接调 Mapper

---

### 8.8 改造点 7：清理 Controller

**文件**：`ApprovalController.java`

**改造后**——移除 V3 专属依赖和手动回调：

```java
// ❌ 删除以下注入
// private final ApprovalCallbackHandler approvalCallbackHandler;
// private final ApprovalRecordMapper approvalRecordMapper;

@PostMapping("/approvals/{id}/approve")
public ApiResponse<ApprovalActionResponse> approve(...) {
    // 改造前：先调引擎，再手动调 callbackHandler
    // ApprovalActionResponse data = approvalService.approve(...);
    // ApprovalRecord record = approvalRecordMapper.selectById(...);
    // if (record.getStatus() == APPROVED) { approvalCallbackHandler.onApproved(record); }

    // 改造后：调引擎即可，回调已在引擎内部同一事务中执行
    ApprovalActionResponse data = approvalService.approve(
        Long.parseLong(id), request, operatorId, operatorName, operator);
    return ApiResponse.success(data);
}
```

`reject` 方法同理。`cancel` 和 `batch-approve`/`batch-reject` 原本就没有 V3 回调逻辑，无需改动。

---

### 8.9 可消除的文件

| 文件 | 处置 | 原因 |
|------|:--:|------|
| `ApprovalCallbackHandler.java` | 删除 | 逻辑并入 `ApprovalEngine.handleFlowVersionPublishResult()` |
| `ConnectorPlatformConstants.APPROVAL_BUSINESS_TYPE_FLOW_VERSION_PUBLISH` | 可保留（作为别名常量）或删除 | `ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH` 已提供相同值 |

---

### 8.10 改造总览：影响面

| 文件 | 改动 | 风险 |
|------|------|:--:|
| **`ApprovalEngine.java`** | | |
| 　`BusinessType` 枚举 | +1 常量 | 🟢 新增 |
| 　`composeApprovalNodes()` | +`appId` 参数 | 🟢 加参数 |
| 　`createApproval()` | +`appId` 参数 | 🟢 加参数 |
| 　`getSceneApprovalNodes()` | 改用 `selectByCodeAndAppId` + 回退 | 🟢 等价扩展 |
| 　`getGlobalApprovalNodes()` | 改用 `selectByCodeAndAppId("global", null)` | 🟢 等价替换 |
| 　`getSceneCodeByBusinessType()` | +1 case | 🟢 新增分支 |
| 　`updateResourceStatus()` | +1 case → `handleFlowVersionPublishResult()` | 🟡 新方法 |
| 　新依赖注入 | +`OpFlowVersionMapper` | 🟡 新依赖 |
| **存量 Service 调用方** | | |
| 　`ApiService.java` | `createApproval(..., null)` | 🟢 +1 arg |
| 　`EventService.java` | `createApproval(..., null)` | 🟢 +1 arg |
| 　`CallbackService.java` | `createApproval(..., null)` | 🟢 +1 arg |
| 　`PermissionService.java` | `createApproval(..., null)` ×3 | 🟢 +1 arg |
| **V3 相关文件** | | |
| 　`FlowVersionApprovalService.java` | `submitApproval` → 调引擎 `createApproval` | 🟡 大简化 |
| 　 | `cancelApproval` → 只调引擎 `cancel` | 🟡 大简化 |
| 　 | `composeApprovalNodes(appId)` → **删除** | 🟢 移除 |
| 　 | `getApprovalNodesByCodeAndAppId` → **删除** | 🟢 移除 |
| 　`ApprovalCallbackHandler.java` | **删除** | 🟢 类消除 |
| 　`ApprovalController.java` | 移除 `approvalCallbackHandler` + `approvalRecordMapper` 注入和手动回调 | 🟢 简化 |
| **已有基础设施（无需改动）** | | |
| 　`db/migration/V3_xxx.sql` | 不变 — schema 已就绪（`uk_code_app`） | 🟢 已就绪 |
| 　`ApprovalFlowMapper.java` | 不变 — `selectByCodeAndAppId` 已存在 | 🟢 已就绪 |

---

### 8.11 改造后调用路径对比

```
存量 6 种（行为完全不变）:
  composeApprovalNodes("api_register", null, null)
    → getSceneApprovalNodes("api_register", null)
      → selectByCodeAndAppId("api_register", null)  // 等价于原 selectByCode
    → getGlobalApprovalNodes()
      → selectByCodeAndAppId("global", null)         // 等价于原 selectByCode

V3 流版本发布（回归标准路径）:
  composeApprovalNodes("connector_flow_version_publish", null, 123)
    → getSceneApprovalNodes("connector_flow_version_publish", 123)
      → selectByCodeAndAppId("connector_flow_version_publish", 123)  // app 级
      → selectByCodeAndAppId("connector_flow_version_publish", null) // platform 级回退
    → getGlobalApprovalNodes()
      → selectByCodeAndAppId("global", null)                          // global 级兜底
```

**一视同仁**：存量 6 种类型和 V3 流版本发布走完全相同的代码路径，区别仅在于 `appId` 传 `null` 还是具体值。

---

### 8.12 消除的偏离项

改造完成后，原 10 项偏离的处置：

| # | 偏离项 | 处置 |
|---|--------|:--:|
| 1 | BusinessType 未注册到引擎枚举 | ✅ 消除 — 注册到 `ApprovalEngine.BusinessType` |
| 2 | 绕过引擎的 `composeApprovalNodes()` | ✅ 消除 — V3 走引擎统一入口 |
| 3 | 绕过引擎的 `createApproval()` | ✅ 消除 — V3 走引擎统一入口 |
| 4 | 使用非标准 level 值 | ✅ 消除 — 引擎返回标准 `scene`/`global`，不再有 `app`/`platform` |
| 5 | 独立回调机制 | ✅ 消除 — 回调并入引擎 `updateResourceStatus`，同事务 |
| 6 | 取消/催办逻辑重复 | ⚠️ **设计意图** — 取消存在两条入口路径（审批中心统一入口 + 业务对象专属入口），这是由前端 UI 上下文决定的，非偏离。引擎负责审批记录状态，业务副作用由各自 Service 处理。催办同理保留在 `FlowVersionApprovalService` |
| 7 | 审批前状态管理耦合 | ⚠️ 保留 — 提交审批时将 FlowVersion 从 DRAFT 改为 PENDING_APPROVAL 是业务前置动作，不放入引擎 |
| 8 | 模板匹配引入 appId 维度 | ✅ **变为特性** — 不再是偏离，而是引擎统一支持的维度 |
| 9 | Controller 层耦合 V3 逻辑 | ✅ 消除 — Controller 回归纯粹路由 |
| 10 | 两套取消入口并存 | ⚠️ **设计意图** — 同偏离 6。前端上下文决定入口：审批中心页面用统一接口，业务详情页用业务专属接口。两条路径在 `ApprovalEngine.cancel()` 处汇合，业务副作用各自处理 |

**偏离 7 保留的理由**：审批前状态管理（草稿 → 待审批）是提交方的业务逻辑，不是审批引擎的职责。引擎只负责：组合节点 → 创建记录 → 审批执行 → 结果回调。`FlowVersionApprovalService` 作为提交方保留这个前置动作是合理的。

---

## 九、修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始版本，完成 V2/V3 审批体系差异分析 | 2026-06-23 | SDDU 路由调度专家 |
| v1.1 | 扩展偏离 5：补充回调机制详细实现对比（引擎内联 vs 独立处理器）、事务边界差异、关键问题分析 | 2026-06-23 | SDDU 路由调度专家 |
| v1.2 | 新增第八节：引擎侧统一改造方案（落地实施），含 12 个子节的详细实施步骤和影响面分析 | 2026-06-23 | SDDU 路由调度专家 |
