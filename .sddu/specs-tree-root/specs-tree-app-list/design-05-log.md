# 操作审计日志 - 详细设计

> 模板参照：需求设计说明书
> 父文档：[design-00-overview.md](./design-00-overview.md)
> 业务素材：plan.md §4.6 / spec.md FR-019
> 编写日期：2026-06-16
> 文档版本：v1.0

---

## 修订记录

| 版本 | 日期 | 修订人 | 修订内容 |
|:----:|------|--------|----------|
| v1.0 | 2026-06-16 | SDDU | 依据 plan.md §4.6 首次编写 |

---

## 目录

- 1 需求价值和概述
- 2 上下文分析（可选）
- 3 初始需求分析（可选）
- 4 需求影响分析
- 5 系统用例分析（可选）
    - 5.1 用例清单
    - 5.2 用例分析
- 6 功能设计
    - 6.1 业界方案实现（可选）
    - 6.2 功能实现整体设计方案（可选）
    - 6.3 架构设计方案（可选）
    - 6.4 功能实现
        - 6.4.1 复用现有基础设施
        - 6.4.2 审计覆盖范围（13 个接口）
        - 6.4.3 新增 OperateEnum 枚举值（13 个）
        - 6.4.4 操作日志描述模板（11 类）
        - 6.4.5 新增 EntitySnapshotLoader 实现（4 个）
        - 6.4.6 Controller @AuditLog 注解分配
        - 6.4.7 before/after 数据捕获矩阵
        - 6.4.8 切面扩展
        - 6.4.9 与现有切面/注解的兼容性
        - 6.4.10 文件清单
        - 6.4.11 验证场景
        - 6.4.12 与 spec.md FR-019 的映射
- 7 系统级非功能设计
- 8 checkList（必填）

---

## 1 需求价值和概述

### 1.1 价值主张

通过 AOP 切面自动记录应用管理模块的全部增删改操作，写入 `openplatform_operate_log_t` 表，用于安全审计、合规追溯与故障定位。实现对业务零侵入、对主流程零阻塞、对存量接口零影响。

### 1.2 需求概述

开放平台应用承载企业内部 API 凭证、成员权限、能力订阅等核心资产，需对所有改动型操作留痕。应用管理模块涉及 13 个增删改接口，全部通过 `@AuditLog` 注解 + `OperateLogV2Aspect` 切面实现审计，**不新建任何审计基础设施**。

**涉及需求**：FR-019

| 需求标号 | 需求名称 | 需求描述 |
|---------|---------|---------|
| FR-019 | 操作审计日志 | 应用管理模块的全部"增、删、改"操作均须记录审计日志；纯查询（GET）与文件上传不纳入审计；失败也记录 |

---

## 2 上下文分析（可选）

不涉及

---

## 3 初始需求分析（可选）

不涉及

---

## 4 需求影响分析

### 4.1 特性影响分析

| 现有特性 | 影响方式 | 说明 |
|----------|----------|------|
| 操作审计 | 扩展 | 新增 13 个 @AuditLog 注解；新增 4 个 EntitySnapshotLoader；扩展 OperateLogV2Aspect |

---

## 5 系统用例分析（可选）

### 5.1 用例清单

| 角色名称 | 用例名称 | 用例简要说明 | 是否需要细化分析 |
|----------|----------|-------------|:----------------:|
| 系统 | UC-A01 操作留痕 | 所有增删改接口自动记录审计日志 | 否 |

### 5.2 用例分析

#### 5.2.1 UC-A01 操作留痕

**简要说明**：通过 `@AuditLog` 注解标记增删改接口，切面自动记录操作人、操作时间、操作类型、操作对象、操作前后快照。

**Actor**：系统（切面自动执行）

**前置条件**：接口被 `@AuditLog` 注解标记

**成功保证**：日志写入 `openplatform_operate_log_t`；失败时 `status=0` 仍记录一条；主业务接口不受影响。

**主成功场景**：
1. 用户发起增删改请求
2. `OperateLogV2Aspect` 拦截，Phase 1 提取 appId
3. 执行业务方法
4. Phase 3 渲染 descCn/descEn
5. `@Async` 异步写入审计日志
6. 返回业务响应

---

## 6 功能设计

### 6.1 业界方案实现（可选）

不涉及

### 6.2 功能实现整体设计方案（可选）

不涉及（见 design-00-overview.md §6.2）

### 6.3 架构设计方案（可选）

不涉及（见 design-00-overview.md §6.3）

### 6.4 功能实现

#### 6.4.1 复用现有基础设施

应用管理模块**不新建**任何审计基础设施，全部复用：

| 复用组件 | 路径 | 职责 |
|---------|------|------|
| `@AuditLog` 注解 | `common/annotation/` | 标记需审计的 Controller 方法 |
| `OperateEnum` 枚举 | `common/enums/` | 维护 type / object / descCn / descEn（本期追加 13 个值） |
| `OperateLogV2Aspect` 切面 | `common/interceptor/` | `@Around` 四阶段错误隔离 + 异步落库 |
| `AuditLogService` | `modules/auditlog/service/` | `@Async + REQUIRES_NEW` 独立事务写入 |
| `EntitySnapshotLoaderFactory` | `common/snapshot/` | 根据 `operateObject` 自动路由到对应 Loader |
| `AppContextResolver` | `modules/app/resolver/` | varchar appId ↔ Long internalId 转换 |
| 线程池 `auditLogExecutor` | `common/config/AsyncConfig.java` | core=2 / max=5 / queue=200 / CallerRunsPolicy |
| 表 `openplatform_operate_log_t` | DB | **不变更 DDL**，字段完全复用 |

**表 6-1 `openplatform_operate_log_t` 结构**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `bigint` | 主键 |
| `app_id` | `varchar(100)` | 应用 ID |
| `operate_type` | `varchar(10)` | 操作类型（CREATE/UPDATE/DELETE） |
| `operate_object` | `varchar(64)` | 操作对象（如 APP、APP_MEMBER、APP_VERSION） |
| `operate_desc_cn` | `text` | 中文描述（最大 500 字符） |
| `operate_desc_en` | `text` | 英文描述 |
| `operate_user` | `varchar(255)` | 操作人 |
| `ip_address` | `varchar(255)` | 操作人地址 |
| `before_data` | `text` | 操作前数据（JSON） |
| `after_data` | `text` | 操作后数据（JSON） |
| `status` | `tinyint(1)` | 0=失败 / 1=成功 |
| `create_by` | `varchar(100)` | 创建人 |
| `create_time` | `datetime(3)` | 创建时间 |
| `last_update_by` | `varchar(100)` | 最后更新人 |
| `last_update_time` | `datetime(3)` | 最后更新时间 |

#### 6.4.2 审计覆盖范围（13 个接口）

依据 FR-019：**含新增应用（接口 1.1）**，所有增删改接口均须加 `@AuditLog`。GET 接口与文件上传不审计。

**表 6-2 审计接口清单**

| 模块 | 接口# | REST | 是否审计 | OperateEnum |
|------|:-----:|------|:--------:|-------------|
| 应用 | 1.1 | POST `/app` | ✅ | CREATE_APP |
| 应用 | 1.2 | PUT `/app/{appId}` | ✅ | UPDATE_APP |
| 应用 | 1.7 | PUT `/app/{appId}/verify-type` | ✅ | UPDATE_APP_VERIFY_TYPE |
| 应用 | 1.10 | POST `/app/{appId}/bind-eamap` | ✅ | BIND_APP_EAMAP |
| 应用 | 1.12 | POST `/file/upload` | ❌ | — 文件上传豁免 |
| 应用 | 1.3~1.6 / 1.8 / 1.9 / 1.11 | GET 多个 | ❌ | — GET 豁免 |
| 成员 | 2.2 | POST `/app/{appId}/members` | ✅ | ADD_APP_MEMBER |
| 成员 | 2.3 | DELETE `/app/{appId}/members/{id}` | ✅ | DELETE_APP_MEMBER |
| 成员 | 2.4 | POST `/app/{appId}/transfer-owner` | ✅ | TRANSFER_APP_OWNER |
| 成员 | 2.1 / 2.5 | GET 多个 | ❌ | — GET 豁免 |
| 能力 | 3.2 | POST `/app/{appId}/abilities` | ✅ | ADD_APP_ABILITY |
| 能力 | 3.1 / 3.3 | GET 多个 | ❌ | — GET 豁免 |
| 版本 | 4.2 | POST `/app/{appId}/versions` | ✅ | CREATE_APP_VERSION |
| 版本 | 4.4 | POST `/app/{appId}/versions/{versionId}/publish` | ✅ | PUBLISH_APP_VERSION |
| 版本 | 4.5 | POST `/app/{appId}/versions/{versionId}/withdraw` | ✅ | WITHDRAW_APP_VERSION |
| 版本 | 4.6 | DELETE `/app/{appId}/versions/{versionId}` | ✅ | DELETE_APP_VERSION |
| 版本 | 4.7 | PUT `/app/{appId}/versions/{versionId}` | ✅ | UPDATE_APP_VERSION |
| 版本 | 4.1 / 4.3 | GET 多个 | ❌ | — GET 豁免 |

**合计**：13 个 `@AuditLog` 接口（应用 4 + 成员 3 + 能力 1 + 版本 5）。

#### 6.4.3 新增 OperateEnum 枚举值（13 个）

在 `common/enums/OperateEnum.java` 末尾追加 13 个枚举值。每个值含 6 个属性：operateType、operateObject（英文路由 key）、operateObjectCn（中文落库）、descCn、descEn、templateCn、templateEn。

**表 6-3 新增 OperateEnum 枚举值**

| 枚举值 | operateType | operateObject | operateObjectCn | templateCn | templateEn |
|--------|:-----------:|:-------------:|:---------------:|-----------|-----------|
| CREATE_APP | CREATE | APP | 应用基础信息 | `新增应用:${appNameCn}` | `Create application:${appNameEn}` |
| UPDATE_APP | UPDATE | APP | 应用基础信息 | `修改基础信息:\n${diffFields}` | `Update basic info:\n${diffFields}` |
| UPDATE_APP_VERIFY_TYPE | UPDATE | APP_VERIFY_TYPE | 认证方式 | `修改认证方式为${verifyType}` | `Update verify type to ${verifyType}` |
| BIND_APP_EAMAP | UPDATE | APP | 应用基础信息 | `绑定应用服务${eamapAppName}` | `Bind application service ${eamapAppName}` |
| ADD_APP_MEMBER | CREATE | APP_MEMBER | 成员管理 | `新增${memberType}:${accountIdStr}` | `Add ${memberType}:${accountIdStr}` |
| DELETE_APP_MEMBER | DELETE | APP_MEMBER | 成员管理 | `删除人员:${accountIdStr}` | `Delete member:${accountIdStr}` |
| TRANSFER_APP_OWNER | UPDATE | APP_MEMBER | 成员管理 | `转移Owner给${accountIdStr}` | `Transfer Owner to ${accountIdStr}` |
| ADD_APP_ABILITY | CREATE | APP_ABILITY | 应用能力 | `新增${abilityTypeDesc}` | `Add ${abilityTypeDesc}` |
| CREATE_APP_VERSION | CREATE | APP_VERSION | 版本 | `新增版本${versionCode}` | `Create version ${versionCode}` |
| UPDATE_APP_VERSION | UPDATE | APP_VERSION | 版本 | `更新版本${versionCode}:\n${diffFields}` | `Update version ${versionCode}:\n${diffFields}` |
| PUBLISH_APP_VERSION | UPDATE | APP_VERSION | 版本 | `发布版本${versionCode}` | `Publish version ${versionCode}` |
| WITHDRAW_APP_VERSION | UPDATE | APP_VERSION | 版本 | `撤回版本${versionCode}` | `Withdraw version ${versionCode}` |
| DELETE_APP_VERSION | DELETE | APP_VERSION | 版本 | `删除版本${versionCode}` | `Delete version ${versionCode}` |

**表 6-4 needsBeforeData / needsAfterData 行为**

| operateType | needsBeforeData | needsAfterData | afterData 取法 |
|-------------|:---------------:|:--------------:|---------------|
| CREATE | ❌ | ✅ | `extractEntityFromResult(ApiResponse.data)` |
| UPDATE | ✅ | ✅ | 操作后通过 Loader 重查 |
| DELETE | ✅ | ❌ | null（实体已删除） |

#### 6.4.4 操作日志描述模板（11 类）

每个 `OperateEnum` 对应一条日志，`descCn` 字段按以下模板动态渲染（`${xxx}` 为占位符，运行时替换）：

**表 6-5 操作日志描述模板**

| # | 模板 | 适用枚举 | templateCn | templateEn | 占位符说明 |
|:-:|------|---------|------------|------------|-----------|
| 0 | 新增应用 | CREATE_APP | `新增应用:${appNameCn}` | `Create application:${appNameEn}` | `appNameCn/En` = 应用中/英文名 |
| 1 | 修改基础信息 | UPDATE_APP | `修改基础信息:\n${diffFields}` | `Update basic info:\n${diffFields}` | `diffFields` = 仅列出实际修改的字段 |
| 1.1 | 子模板：应用图标 | UPDATE_APP（图标字段） | `应用图标` | `App icon` | - |
| 1.2 | 子模板：中文名 | UPDATE_APP（nameCn 字段） | `中文名:由"${before}"改为"${after}"` | `Chinese name:from "${before}" to "${after}"` | before/after = 修改前/后的名称 |
| 1.3 | 子模板：英文名 | UPDATE_APP（nameEn 字段） | `英文名:由"${before}"改为"${after}"` | `English name:from "${before}" to "${after}"` | before/after = 修改前/后的名称 |
| 1.4 | 子模板：中文描述 | UPDATE_APP（descCn 字段） | `中文描述:由"${before}"改为"${after}"` | `Chinese desc:from "${before}" to "${after}"` | before/after = 修改前/后的描述 |
| 1.5 | 子模板：英文描述 | UPDATE_APP（descEn 字段） | `英文描述:由"${before}"改为"${after}"` | `English desc:from "${before}" to "${after}"` | before/after = 修改前/后的描述 |
| 1.6 | 子模板：功能示意图 | UPDATE_APP（diagramIdList 字段） | `功能示意图` | `Function diagram` | - |
| 2 | 修改认证方式 | UPDATE_APP_VERIFY_TYPE | `修改认证方式为${verifyType}` | `Update verify type to ${verifyType}` | `verifyType` = 中文用顿号拼接（如"Cookie、数字签名"），英文用逗号 |
| 3 | 绑定 EAMAP | BIND_APP_EAMAP | `绑定应用服务${eamapAppName}` | `Bind application service ${eamapAppName}` | `eamapAppName` = EAMAP 名称 |
| 4 | 新增成员 | ADD_APP_MEMBER | `新增${memberType}:${accountIdStr}` | `Add ${memberType}:${accountIdStr}` | `memberType` = 中文"开发者/管理员"，英文"Developer/Admin" |
| 5 | 转移 Owner | TRANSFER_APP_OWNER | `转移Owner给${accountIdStr}` | `Transfer Owner to ${accountIdStr}` | `accountIdStr` = 新 Owner 账号 ID |
| 6 | 删除人员 | DELETE_APP_MEMBER | `删除人员:${accountIdStr}` | `Delete member:${accountIdStr}` | `accountIdStr` = 被删成员账号 ID |
| 7 | 添加能力 | ADD_APP_ABILITY | `新增${abilityTypeDesc}` | `Add ${abilityTypeDesc}` | `abilityTypeDesc` = 中文"群置顶"，英文"Group Top" |
| 8 | 新增版本 | CREATE_APP_VERSION | `新增版本${versionCode}` | `Create version ${versionCode}` | `versionCode` = 版本号 |
| 9 | 发布版本 | PUBLISH_APP_VERSION | `发布版本${versionCode}` | `Publish version ${versionCode}` | `versionCode` = 版本号 |
| 10 | 删除版本 | DELETE_APP_VERSION | `删除版本${versionCode}` | `Delete version ${versionCode}` | `versionCode` = 版本号 |
| 11 | 撤回版本 | WITHDRAW_APP_VERSION | `撤回版本${versionCode}` | `Withdraw version ${versionCode}` | `versionCode` = 版本号 |

> **说明**：
> - 模板 1 的子模板（1.1 ~ 1.6）仅在对应字段实际修改时拼接到 `diffFields`，未修改的字段不展示
> - `descCn` / `descEn` 字段最大 500 字符
> - 模板渲染在 `OperateLogV2Aspect Phase 3.5` 触发（after 阶段），失败时回退为 `OperateEnum.descCn / descEn` 静态值

#### 6.4.5 新增 EntitySnapshotLoader 实现（4 个）

应用管理涉及 4 类业务实体，每类新增一个 Loader（路径 `common/snapshot/`），注入对应 Mapper 加载快照。`EntitySnapshotLoaderFactory` 通过 `@PostConstruct` 自动扫描注册，**工厂代码不动**。

**表 6-6 新增 Loader 清单**

| Loader | 路由 key | 数据源 | 适用场景 |
|--------|---------|--------|---------|
| `AppSnapshotLoader` | `APP`, `APP_VERIFY_TYPE` | `AppMapper` + `AppPropertyMapper`（主表 + 属性表合并）| CREATE_APP / UPDATE_APP / UPDATE_APP_VERIFY_TYPE / BIND_APP_EAMAP |
| `AppMemberSnapshotLoader` | `APP_MEMBER` | `AppMemberMapper` | ADD_APP_MEMBER / DELETE_APP_MEMBER / TRANSFER_APP_OWNER |
| `AppAbilityRelationSnapshotLoader` | `APP_ABILITY` | `AppAbilityRelationMapper` | ADD_APP_ABILITY |
| `AppVersionSnapshotLoader` | `APP_VERSION` | `AppVersionMapper` + `AppVersionPropertyMapper` | CREATE / UPDATE / PUBLISH / WITHDRAW / DELETE_APP_VERSION |

**表 6-7 Loader 注册结果**

| operateObject | Loader 实现 |
|---------------|------------|
| APP | AppSnapshotLoader |
| APP_MEMBER | AppMemberSnapshotLoader |
| APP_ABILITY | AppAbilityRelationSnapshotLoader |
| APP_VERSION | AppVersionSnapshotLoader |
| API_PERMISSION / EVENT_PERMISSION / CALLBACK_PERMISSION | SubscriptionSnapshotLoader（已有）|

#### 6.4.6 Controller @AuditLog 注解分配

13 个目标接口分布在 4 个 Controller。**12 个接口路径均含 `{appId}`**，appIdSource=PATH_VARIABLE；**1 个新增应用接口（POST /app）无 path 变量 appId**，需新增 `appIdSource=RESPONSE_FIELD` 策略。

**表 6-8 @AuditLog 注解分配**

| Controller | 方法 | OperateEnum | resourceIdParam | 备注 |
|------------|------|-------------|----------------|------|
| AppController | createApp | CREATE_APP | 响应体 `data.appId` | CREATE 类型，before=null，after=新建应用；需新增 `appIdSource=RESPONSE_FIELD` 策略 |
| AppController | updateApp | UPDATE_APP | `"appId"` | varchar appId 由切面扩展自动解析 |
| AppController | updateVerifyType | UPDATE_APP_VERIFY_TYPE | `"appId"` | 同上 |
| AppController | bindEamap | BIND_APP_EAMAP | `"appId"` | 同上 |
| MemberController | addMember | ADD_APP_MEMBER | 默认 `"id"`（未指定）| CREATE 无 beforeData |
| MemberController | deleteMember | DELETE_APP_MEMBER | `"accountId"` | 复合键由切面传入 appId，Loader 内部按 (appId, accountId) 查 |
| MemberController | transferOwner | TRANSFER_APP_OWNER | 请求体中目标成员 id | before=目标成员转移前 / after=转移后，记一条日志 |
| AbilityController | addAbility | ADD_APP_ABILITY | 默认 `"id"`（未指定）| CREATE 无 beforeData |
| VersionController | createVersion | CREATE_APP_VERSION | 默认 `"id"`（未指定）| CREATE 无 beforeData |
| VersionController | updateVersion | UPDATE_APP_VERSION | `"versionId"` | 直接是 Long 主键 |
| VersionController | publishVersion | PUBLISH_APP_VERSION | `"versionId"` | 同上 |
| VersionController | withdrawVersion | WITHDRAW_APP_VERSION | `"versionId"` | 同上 |
| VersionController | deleteVersion | DELETE_APP_VERSION | `"versionId"` | 同上，afterData=null |

#### 6.4.7 before/after 数据捕获矩阵

**表 6-9 数据捕获矩阵**

| OperateEnum | beforeData | afterData | 备注 |
|-------------|-----------|----------|------|
| CREATE_APP | null（CREATE） | `ApiResponse.data` 新建的应用 | resourceId 与 appId 均取响应中 `data.appId`（需切面扩展 ③） |
| UPDATE_APP | AppSnapshotLoader 操作前快照 | 同 Loader 操作后重查 | resourceIdParam=appId，切面扩展自动解析 varchar |
| UPDATE_APP_VERIFY_TYPE | 同上 | 同上 | 同上 |
| BIND_APP_EAMAP | 同上 | 同上（app_type / app_sub_type 已变更） | 同上 |
| ADD_APP_MEMBER | null（CREATE） | `ApiResponse.data` 新建的 MemberResponse | — |
| DELETE_APP_MEMBER | AppMemberSnapshotLoader 按 (appId, accountId) 查 | null（实体已删） | 复合键由切面传入 appId |
| TRANSFER_APP_OWNER | AppMemberSnapshotLoader 加载目标成员转移前快照 | 同 Loader 操作后重查 | resourceIdParam=请求体中目标成员 id，记一条日志 |
| ADD_APP_ABILITY | null（CREATE） | `ApiResponse.data` 新建的能力关联记录 | — |
| CREATE_APP_VERSION | null（CREATE） | `ApiResponse.data` 新建的 AppVersionResponse | — |
| UPDATE_APP_VERSION | AppVersionSnapshotLoader 操作前快照 | 同 Loader 操作后重查 | — |
| PUBLISH_APP_VERSION | 同上（status=待发布） | 同上（status=审批中） | — |
| WITHDRAW_APP_VERSION | 同上（status=审批中） | 同上（status=待发布） | — |
| DELETE_APP_VERSION | 同上 | null（实体已删） | — |

> **TRANSFER_APP_OWNER**：一次转移操作记一条审计日志。`resourceIdParam` 取请求体中新 Owner 候选的成员 id，before 是该成员转移前记录，after 是转移后记录（role 变为 OWNER）。原 Owner 角色变化由审计消费端基于同 appId 的成员记录差异比对还原。

#### 6.4.8 切面扩展

现有 `extractResourceId()` 只支持纯数字 String → Long，且 `loadEntitySnapshot()` 只传 `resourceId` 给 Loader，无法满足**三类需求**：① varchar appId（UPDATE_APP 等）；② 复合键 (appId, accountId)（DELETE_APP_MEMBER）；③ 响应中提取 appId（CREATE_APP）。统一扩展如下：

**表 6-10 切面扩展**

| 改动点 | 现状 | 扩展后 |
|--------|------|--------|
| `extractResourceId()` | String → `Long.parseLong()`，失败返回 null | 失败时回退 `appContextResolver.resolveAndValidate(s).getInternalId()`（适用 varchar appId） |
| `loadEntitySnapshot()` | 仅按 `resourceId` 调 `loader.loadById(id)` | 额外把 `appId`（已在切面中由 `extractAppIdFromParams()` 提取）传给需要复合键的 Loader |
| `extractAppId()` 扩展 | 仅 `appIdSource=PATH_VARIABLE` 策略 | 新增 `appIdSource=RESPONSE_FIELD` 策略：方法正常返回后从 `ApiResponse.data.appId` 提取（适用 POST /app） |

**复合键 Loader 的处理**：`AppMemberSnapshotLoader` 新增 `loadByAppIdAndAccountId(String appId, Long accountId)` 方法。切面在 `loadEntitySnapshot()` 内判断 Loader 类型，对 `AppMemberSnapshotLoader` 调用该重载方法，其他 Loader 仍走 `loadById()`。**不修改 `EntitySnapshotLoader` 接口**。

**响应提取策略的处理**：`@AuditLog` 注解新增 `appIdSource` 取值 `RESPONSE_FIELD`，切面在 Phase 1 缓存 `ApiResponse`，Phase 4 从 `response.data.appId` 提取 appId。**不修改 `@AuditLog` 注解现有字段**（仅追加枚举值，向后兼容）。

**表 6-11 影响范围**

| 文件 | 改动 |
|------|------|
| `OperateLogV2Aspect.java` | 注入 `AppContextResolver`；`extractResourceId()` 加 varchar 回退；`loadEntitySnapshot()` 加 appId 上下文传递；`extractAppId()` 加 `RESPONSE_FIELD` 策略 |
| `AppMemberSnapshotLoader.java` | 新增 `loadByAppIdAndAccountId()` 重载方法 |

#### 6.4.9 与现有切面/注解的兼容性

**表 6-12 兼容性矩阵**

| 维度 | 现状 | 应用管理模块的适配 |
|------|------|-------------------|
| `@AuditLog` 注解定义 | `value / appIdSource / resourceIdParam` 3 字段 | **不修改**，复用 |
| `OperateLogV2Aspect` 切面 | 四阶段错误隔离，已支持 CREATE/UPDATE/DELETE/WITHDRAW/CONFIG | **不修改**，复用 |
| `EntitySnapshotLoaderFactory` | `@PostConstruct` 自动注册所有实现 | **新增 4 个 Loader**，工厂代码不动 |
| `extractResourceId()` 类型支持 | 仅 String（可转 Long）/ Long | 本期扩展：`parseLong` 失败时回退 `appContextResolver.resolveAndValidate()` |
| `extractAppIdFromParams()` | 扫描方法参数中名为 `appId` 的项 | 12 个接口含 `@PathVariable String appId`，1 个接口（POST /app）通过新策略 `RESPONSE_FIELD` 从响应提取 |
| 异步线程池 `auditLogExecutor` | core=2 / max=5 / queue=200 | 应用管理接口流量极低，**容量充裕** |
| 与连接流 `AuditLogAspect` | SLF4J 日志，无 DB 写入 | 无冲突 |

**向后兼容**：现有 11 个权限订阅审计接口的 `resourceIdParam` 均为 `"id"`（纯数字）或不指定，`extractResourceId()` 的新分支只在 `parseLong` 失败时才进入，**对存量接口零影响**。

#### 6.4.10 文件清单

> Java 路径基于 `open-server/src/main/java/com/xxx/it/works/wecode/v2/`

**表 6-13 新增/修改文件清单**

| # | 文件 | 类型 | 说明 |
|:-:|------|:----:|------|
| 1 | `common/enums/OperateEnum.java` | **修改** | 末尾追加 13 个枚举值 |
| 2 | `common/interceptor/OperateLogV2Aspect.java` | **修改** | 注入 `AppContextResolver`；`extractResourceId()` 加 varchar 回退；`loadEntitySnapshot()` 加 appId 上下文传递；`extractAppId()` 加 `RESPONSE_FIELD` 策略 |
| 3 | `common/snapshot/AppSnapshotLoader.java` | **新建** | 应用快照（主表 + 属性表合并） |
| 4 | `common/snapshot/AppMemberSnapshotLoader.java` | **新建** | 成员快照；新增 `loadByAppIdAndAccountId(appId, accountId)` 重载 |
| 5 | `common/snapshot/AppAbilityRelationSnapshotLoader.java` | **新建** | 能力关联快照 |
| 6 | `common/snapshot/AppVersionSnapshotLoader.java` | **新建** | 版本快照（主表 + 属性表合并） |
| 7 | `modules/app/controller/AppController.java` | 新建（已规划） | 声明 4 个 `@AuditLog`（createApp / updateApp / updateVerifyType / bindEamap） |
| 8 | `modules/app/controller/MemberController.java` | 新建（已规划） | 声明 3 个 `@AuditLog`（addMember / deleteMember / transferOwner） |
| 9 | `modules/app/controller/AbilityController.java` | 新建（已规划） | 声明 1 个 `@AuditLog` |
| 10 | `modules/app/controller/VersionController.java` | 新建（已规划） | 声明 5 个 `@AuditLog` |

**完全不动**：`@AuditLog` 注解 / `AuditLogService` / `AsyncConfig` / `OperateLog` 实体 / `OperateLogMapper` / 表 DDL / `EntitySnapshotLoaderFactory` / `EntitySnapshotLoader` 接口。

#### 6.4.11 验证场景

**表 6-14 验证场景**

| # | 场景 | 请求 | 预期 `openplatform_operate_log_t` |
|:-:|------|------|----------------------------------|
| 1 | 更新应用名（varchar appId 自动解析） | `PUT /app/{appId}` body=`{appName:"新"}` | 新增 1 条：type=UPDATE / object=应用基础信息 / before=旧 JSON / after=新 JSON / status=1 |
| 2 | 删除成员（按主键 id 定位） | `DELETE /app/{appId}/members/{id}` | 新增 1 条：type=DELETE / object=成员管理 / before={appId,accountId,role,...} / after=null / status=1 |
| 3 | 转移 Owner | `POST /app/{appId}/transfer-owner` body=`{targetMemberId:...}` | 新增 1 条：type=UPDATE / object=成员管理 / before=目标成员转移前 / after=目标成员转移后（role=OWNER） / status=1 |
| 4 | 删除版本 | `DELETE /app/{appId}/versions/{versionId}` | 新增 1 条：type=DELETE / object=版本 / before=完整版本 JSON / after=null / status=1 |
| 5 | 创建应用 | `POST /app` body=`{appName:"测试"}` | 新增 1 条：type=CREATE / object=应用基础信息 / before=null / after=新应用 JSON / resourceId 与 appId 均为新应用 appId / status=1 |
| 6 | 异常隔离 | `PUT /app/{appId}/verify-type` body=非法值 | 接口返回 400；同时新增 1 条 status=0 失败日志 |
| 7 | 转移 Owner 业务失败回滚 | `POST /app/{appId}/transfer-owner` body=非法新 Owner | 接口返回 400；`openplatform_operate_log_t` **无新增**（`AFTER_COMMIT` 事件未触发） |

#### 6.4.12 与 spec.md FR-019 的映射

**表 6-15 FR-019 业务规则映射**

| FR-019 业务规则 | 技术实现 |
|----------------|---------|
| 覆盖范围（含新增应用的全部增删改） | 13 个 `@AuditLog` 注解 |
| 审计字段（10 项） | `openplatform_operate_log_t` 既有字段 |
| 失败也要记录（status=失败） | `OperateLogV2Aspect` Phase 2 `record status=0` 后重抛 |
| 不阻塞主业务 | `@Async + REQUIRES_NEW + CallerRunsPolicy + 四阶段错误隔离` |
| 操作前后快照 | `EntitySnapshotLoader` 策略路由 + `extractEntityFromResult` 回退 |

---

## 7 系统级非功能设计

> 见 design-00-overview.md §7

### 7.1 系统级FMEA影响分析

| 失效模式 | 影响 | 对策 |
|----------|------|------|
| 审计日志写入失败 | 主业务已成功但无日志 | `@Async + REQUIRES_NEW` 独立事务；失败打印 ERROR 日志但不影响主业务 |
| appId 解析失败 | 审计日志 app_id 为空或错误 | `extractResourceId()` 扩展 varchar 回退逻辑，失败抛异常但不阻止切面继续 |
| 快照加载失败 | before/after 数据为空 | `EntitySnapshotLoader` 捕获异常返回 null，日志记录 ERROR |

### 7.2 系统级安全影响分析

| 安全维度 | 措施 |
|----------|------|
| 操作可追溯 | 13 个增删改接口全覆盖，失败也记录 |
| 数据完整性 | 快照 JSON 最大 500 字符，超长截断 |

### 7.3 兼容性

**后向兼容性**：现有切面逻辑不变，仅扩展 `extractResourceId()` 分支和 `extractAppId()` 策略。

### 7.4 可运维

| 运维维度 | 措施 |
|----------|------|
| 日志 | 切面各阶段打印 DEBUG/ERROR 日志 |
| 监控 | 审计写入失败时 ERROR 日志可接入告警 |

---

## 8 checkList（必填）

### 8.1 设计自检清单要求（必填）

**表 8-1 设计自检清单**

| check 点 | 是否达标 | 备注 |
|----------|:--------:|------|
| 文档结构完整覆盖模板 8 大章节 | 是 | §1~§8 全部覆盖 |
| 包含需求价值/来源 | 是 | §1 |
| 包含审计覆盖范围（13 个接口） | 是 | §6.4.2 |
| 包含 OperateEnum 枚举值（13 个） | 是 | §6.4.3 |
| 包含 EntitySnapshotLoader（4 个） | 是 | §6.4.5 |
| 包含 @AuditLog 注解分配 | 是 | §6.4.6 |
| 包含 before/after 数据捕获矩阵 | 是 | §6.4.7 |
| 包含切面扩展方案 | 是 | §6.4.8 |
| 包含兼容性分析 | 是 | §6.4.9 |
| 包含文件清单 | 是 | §6.4.10 |
| 包含验证场景 | 是 | §6.4.11 |
| 包含 FR-019 映射 | 是 | §6.4.12 |
| 包含 FMEA 分析 | 是 | §7.1 |
| 包含安全影响分析 | 是 | §7.2 |
| 设计自检清单全部勾选完毕 | 是 | 本表 14 项全部达标 |

---

**文档结束**
