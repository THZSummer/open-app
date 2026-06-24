# 卡片设置板块 - 技术规格 (Spec)

**Document ID:** SPEC-CARD-SETTING-001
**Version:** 0.5 (Draft)
**Created:** 2026-06-10
**Updated:** 2026-06-22
**Scope Owner:** open-server / open-web（卡片设置板块）
**Status:** 待评审

> **使用说明**
> - 每一条"结论"都标注了来源：`[src/path:line]` 表示从代码读取的事实，`#ASSUMED` 表示基于现有信息的推断（待确认）。
> - 本 Spec 不覆盖：应用详情 page 整体、应用凭证板块、应用基本信息板块、认证方式板块（这些由其他同事负责）。
> - 本 Spec 覆盖：在应用详情 page **末尾**新增的"卡片设置"板块（前端组件 + 后端接口 + 卡片服务客户端）。

---

## 1. Scope

### 1.1 业务背景

- 应用（App）是一种权益载体，其"卡片"权益由**卡片服务**（外部独立服务）提供，页面落在 open-web。
  - 来源：用户澄清（2026-06-10；本次会话 2026-06-22 确认服务名称为"卡片服务"）
- 卡片是**应用级**资源：一个应用对应一套"定期失效时间 + 定期删除时间"配置，**无 cardId 维度**。
  - 来源：用户澄清（2026-06-10）
- 数据的权威来源（SoT）在卡片服务；open-server **不持久化**该配置，仅做权限校验 + 请求转发。
  - 来源：用户澄清（2026-06-10）
- 关键标识来源：
  - `appId`：`openplatform_app_t` 表的 `appId` 字段（path 参数）
  - `tenantId`：当前用户上下文（#ASSUMED 通过 `TenantContext` 或等价机制获取）
  - `clientId`（即 `eamapAppId`）：从 `openplatform_app_p_t` 表按 `appId + property_name='eamap_app_code'` 查 `property_value`
  - 来源：用户澄清（本次会话，2026-06-22）
- 第三方查询返回值域为**任意整数**（可能超出用户可写范围）。例如失效时间的系统默认值为 14 天，但用户可配置范围为 1~7 天；删除时间系统默认 7 天，用户可配置范围 1~30 天。
  - 来源：用户澄清（本次会话，2026-06-22）
- 前端**只读态展示实际值**（如 14 天就显示 14 天），**编辑态按 §3.2.1 裁剪**到用户可写范围。保存成功后重新 GET 回填。
  - 来源：用户澄清（本次会话，2026-06-22）

### 1.2 范围边界

| In Scope | Out of Scope |
|----------|--------------|
| 应用详情 page 末尾新增"卡片设置"板块（前端组件） | 应用详情 page 整体结构 / 路由 / 导航 |
| 前端：`CardSettingSection` 组件 + `card.service.ts` + `useCardSetting.ts` | 应用凭证 / 应用基本信息 / 认证方式板块 |
| 后端：新增 `card` 模块（controller / service / dto / 卡片服务客户端） | 本地数据库 entity / mapper（卡片设置数据不存本地） |
| 卡片服务客户端（查询 / 修改两个接口，封装统一响应） | 卡片服务的实现与部署 |
| 权限校验（复用 `AppContextResolver`） | 细粒度权限点（如 OperateEnum.MANAGE_CARD_SETTING） |
| 应用属性查询（从 `openplatform_app_p_t` 读取 `eamap_app_code` 作为 `clientId`） | `openplatform_app_p_t` 表本身的维护 |

### 1.3 依赖

| 依赖方 | 内容 | 状态 |
|--------|------|------|
| 同事 A | 应用详情 page + 三个已有板块（凭证/基本信息/认证方式） | #ASSUMED 在并行开发中，本 Spec 假设已存在并可挂载 |
| 卡片服务 | 查询 / 修改 两个接口（详见 §2.5） | 接口定义**已提供**（本次会话 2026-06-22） |
| `AppContextResolver` | 应用访问权限校验 | ✅ 已存在 [`open-server/.../modules/app/resolver/AppContextResolver.java:15-32`]；注意 `StandardAppContextResolver` 实现是 TODO（OQ-14），开发环境走 `DevAppContextResolver` |
| `ApiResponse<T>` | 统一响应包装 | ✅ 已存在 [`open-server/.../common/model/ApiResponse.java:25`]；字段 `code/messageZh/messageEn/data/page` |
| `BusinessException` | 三参数业务异常 | ✅ 已存在 [`open-server/.../common/exception/BusinessException.java:33`]；构造 `(code, zh, en)`；工厂方法 `badRequest/unauthorized/forbidden/notFound/internalError` |
| `request.ts` | 前端 HTTP 客户端 | ✅ 已存在 [`open-web/src/utils/request.ts:31-63`]；baseURL=/api/v1，自动 Bearer+X-App-Id；**L75-78 业务错误已自动 `message.error`，hook 不应重复处理（OQ-15 已解决）** |
| `AppPropertyMapper`（新建） | 按 `appId + property_name='eamap_app_code'` 查 `property_value` 作为 `clientId` | ⚠️ **不存在**，需新建 `modules/app/mapper/AppPropertyMapper.java` + XML（OQ-13 已决策方案 A）；可参考 `modules/sync/entity/OldAppProperty.java` 的表结构 |
| `tenantId` 获取 | 调卡片服务需传 `tenantId` | ✅ **获取工具类已存在**（具体类名/方法名 TODO 由人工二开时确认并对接）；本 Spec 不新建工具类/拦截器 |
| `OperateEnum`（新增枚举） | 卡片设置审计日志枚举 | ⚠️ **不存在**，需按 5 参数构造 `(operateType, operateObject, operateObjectCn, descCn, descEn)` 新增 `UPDATE_CARD_PERIOD`（OQ-4 待决策是否加审计） |

### 1.4 接口编号

本 Spec 涉及的接口编号**未分配**。建议占位符（共 2 个，已合并失效/删除为单一 PUT）：
- `#TBD-CS01` GET `/apps/{appId}/card-settings`（查询）
- `#TBD-CS02` PUT `/apps/{appId}/card-settings`（更新周期：通过 body 中 `periodType` 区分失效/删除）

> 待与 PermissionController 的 #27~#43 体系统一分配正式编号。 [`open-server/.../modules/permission/controller/PermissionController.java:40`]

---

## 2. Interface

### 2.1 前端 — `CardSettingSection` 组件

#### 2.1.1 Props 契约

```ts
// 来源：约定式推断，参考 useCallbackManager 等既有模式 [open-web/src/hooks/useCallback.ts:20-144]
export interface CardSettingSectionProps {
  appId: string; // 当前应用 ID（外部业务 ID，长 ID）
}
```

> **#ASSUMED** 应用详情 page 会把 `appId` 作为 prop 透传给板块组件。需同事 A 确认。

#### 2.1.2 布局（自上而下平铺，两态切换）

> UI 区分**只读态**与**编辑态**：默认展示只读态，用户点击"修改"后切换到编辑态；保存/取消后回到只读态。
> 来源：用户澄清（本次会话，2026-06-22）

**只读态（默认）**

```
┌─ 卡片设置 ────────────────────────────────────────────────┐
│                                                            │
│  定期失效时间  [❓]   14 天  ‹gap›  [修改]                 │
│                                                            │
│  定期删除时间  [❓]   7 天   ‹gap›  [修改]                 │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

- 展示第三方返回的实际值（可能超出可写范围，例如失效时间显示 14 天）
- 字段未设置（null）时 #ASSUMED 显示 `—` 或 placeholder（文案待 OQ-8）

**编辑态（点击"修改"后）**

```
┌─ 卡片设置 ────────────────────────────────────────────────┐
│                                                            │
│  定期失效时间  [❓]  [  ▯ 7  ▲  ▼  ]  天  ‹gap› [保存] ‹gap› [取消] │
│                                                            │
│  定期删除时间  [❓]  [  ▯ 7  ▲  ▼  ]  天  ‹gap› [保存] ‹gap› [取消] │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

- 每行**独立状态、独立保存/取消**（因为后端 PUT 通过 `periodType` 区分失效/删除，每次调用只设置一种周期）
  - 来源：用户澄清（2026-06-10，"分别调用不同的接口"；2026-06-22 调整为单一 PUT 接口 + `periodType` 字段）
- 编辑态 InputNumber 初始值按 §3.2.1 裁剪（上例中失效时间原值 14 → 裁剪到 max=7）
- ❓ 图标：hover 显示 tooltip（内容见 2.1.3）
- 输入框：支持手动输入 + 右侧 ▲/▼ 步进按钮（点击 ±1 天）
- 单位"天"紧跟输入框

**间距约束（统一）**

三组间距**全部相等**，对齐现有 PermissionDrawer 的按钮间距规范：

| 间距对 | 对齐到 |
|--------|-------|
| 只读态：`天` ↔ `修改` | antd `<Space>` 默认间距 |
| 编辑态：`天` ↔ `保存` | antd `<Space>` 默认间距 |
| 编辑态：`保存` ↔ `取消` | antd `<Space>` 默认间距 |

来源：用户澄清（本次会话，2026-06-22）"保存和天之间间隔和现状的保存与取消一致；修改与天之间间隔也与保存与取消现状一致"。
代码参考：`open-web/src/pages/permission/ApiPermissionDrawer.tsx:195-207`（`<Space>` 包裹 `取消 + 提交申请`，无显式 size，使用 antd 默认 8px）。

> **#ASSUMED** 采用 antd 组件：`InputNumber`（内置步进按钮）+ `Tooltip` + `Button` + `<Space>`。
> 依据：`open-web` 已引入 antd（见 `useCallback.ts:2` 的 `import { message } from 'antd'`）。

#### 2.1.3 Tooltip 文案（完整业务说明）

> 来源：用户澄清（本次会话，2026-06-22）。原文中"再端侧"按"在端侧"修正（typo）。

| 字段 | Tooltip 文案 |
|------|--------------|
| 定期失效时间 | "根据每张消息卡片第一次投放时间开始计算，系统按设置的时间自动对卡片进行失效，失效的卡片在端侧不再支持交互" |
| 定期删除时间 | "只有失效的卡片可以删除，根据每张消息卡片失效时间开始计算，系统按照设置的时间自动对卡片进行删除" |

> 注：原 v0.1 中"最小1天，最大7天"等数值范围说明**从 Tooltip 中移除**（数值约束改由 §3.2 的前后端校验保证，不在 Tooltip 重复）。如设计/产品希望同时展示范围提示，待 OQ-10 决策。

#### 2.1.4 交互约束

> 来源：用户澄清（本次会话，2026-06-22）+ 用户澄清（2026-06-10）

**生命周期**

| 交互 | 行为 |
|------|------|
| 进入板块 | 调 `GET /apps/{appId}/card-settings` 获取两字段当前值，以**只读态**展示 |
| 点击"修改" | 当前行切换到**编辑态**，InputNumber 初始值按 §3.2.1 裁剪 |
| 点击"取消" | 当前行还原到**修改前的展示值**（即上次 GET 返回值），切回只读态 |
| 保存成功 | `message.success('保存成功')` → **重新调用 GET** → 用新返回值回填只读态展示 |
| 保存失败 | **`request.ts` 拦截器已自动 `message.error(后端 messageZh)`**（[request.ts:75-78]），hook 的 catch 里**不要**再调 `message.error`（避免双重提示），只做：① `console.error` 调试日志；② 保留当前输入值供用户重试；③ 停留在编辑态 |

**编辑态输入约束**

| 交互 | 行为 |
|------|------|
| 输入值越界 | `InputNumber` 自带 min/max 约束（失效 1-7 / 删除 1-30），超出自动裁剪 |
| 未输入合法值时 | #ASSUMED 保存按钮 disabled |

**并发 / 一致性**

| 交互 | 行为 |
|------|------|
| 并发编辑 | #ASSUMED 依赖卡片服务的语义，本板块不实现乐观锁 |
| 两字段独立保存 | 允许"失效时间已保存但删除时间保存失败"的中间态（§3.4 已说明为预期行为） |

### 2.2 前端 — `card.service.ts`

> 命名/风格对齐现有 `callback.service.ts` [`open-web/src/services/callback.service.ts`]

```ts
import { get, put } from '@/utils/request';

// ===== 类型定义 =====

/** 卡片设置查询响应 */
export interface CardSetting {
  expirationDays: number | null; // 定期失效时间（天），null 表示未设置
  deletionDays: number | null;   // 定期删除时间（天），null 表示未设置
}

/** 卡片周期类型 */
export type PeriodType = 0 | 1; // 0=定期删除周期，1=定期失效周期

/** 卡片周期更新请求（失效/删除合并为单一接口） */
export interface UpdateCardPeriodRequest {
  periodDays: number;       // 周期天数（失效：1..7；删除：1..30）
  periodType: PeriodType;   // 0=删除周期，1=失效周期
}

// ===== API 函数 =====

/** #TBD-CS01 查询卡片设置 */
export const getCardSetting = (appId: string) =>
  get<CardSetting>(`/apps/${appId}/card-settings`);

/** #TBD-CS02 更新卡片周期（失效/删除合并） */
export const updateCardPeriod = (appId: string, data: UpdateCardPeriodRequest) =>
  put<void>(`/apps/${appId}/card-settings`, data);
```

> 注：open-server 这边用 `periodDays`，卡片服务那边对应字段叫 `period`（由后端做映射，前端不感知）。

### 2.3 前端 — `useCardSetting.ts`

> 风格对齐 `useCallbackManager` [`open-web/src/hooks/useCallback.ts:20-144`]

```ts
export const useCardSetting = () => {
  // state: loading / cardSetting / 两个字段各自的 saving 状态
  // actions:
  //   fetchCardSetting()                                 → 调 GET
  //   handleSavePeriod(appId, periodDays, periodType)    → 调 PUT，成功后再 GET 回填
  // 返回：{
  //   loading,
  //   savingExpiration, savingDeletion,
  //   cardSetting,
  //   fetchCardSetting,
  //   handleSavePeriod,
  // }
};
```

> 实现细节留给实现者；Spec 只约束**对外 API 形状**和**错误处理语义**（见 2.1.4）。

### 2.4 后端 — HTTP 接口

> 风格对齐 `PermissionController` [`open-server/.../modules/permission/controller/PermissionController.java:31`]

#### 2.4.1 #TBD-CS01 查询卡片设置

| 项 | 值 |
|----|----|
| Method | `GET` |
| Path | `/service/open/v2/apps/{appId}/card-settings` |
| Auth | `AppContextResolver.resolveAndValidate(appId)`（见 3.1） |
| AuditLog | #ASSUMED 不加（只读）；如需加，新增 `OperateEnum.QUERY_CARD_SETTING` |

**响应**（200）

```json
{
  "code": "200",
  "messageZh": "success",
  "messageEn": "success",
  "data": {
    "expirationDays": 14,
    "deletionDays": 7
  },
  "page": null
}
```

`data` 字段类型：`CardSettingResponse { expirationDays: Integer, deletionDays: Integer }`，两字段**均可 null**（表示卡片服务未配置）。

> 字段映射：open-server 的 `expirationDays` / `deletionDays` 来自卡片服务响应的 `expirationPeriod` / `deletionPeriod`（由 `CardServiceClient` 实现做映射）。

#### 2.4.2 #TBD-CS02 更新卡片周期（失效/删除合并）

| 项 | 值 |
|----|----|
| Method | `PUT` |
| Path | `/service/open/v2/apps/{appId}/card-settings` |
| Auth | `AppContextResolver.resolveAndValidate(appId)` |
| AuditLog | #ASSUMED 新增 `OperateEnum.UPDATE_CARD_PERIOD`（待确认 OQ-4） |

**请求体**

```json
{
  "periodDays": 7,
  "periodType": 1
}
```

| 字段 | 类型 | 规则 | 说明 |
|------|------|------|------|
| `periodDays` | Integer | 非空；整数；范围取决于 `periodType` | 周期天数 |
| `periodType` | Integer | 非空；枚举 `0` / `1` | `0`=定期删除周期（1..30）；`1`=定期失效周期（1..7） |

**校验规则**

- `periodType` 必须为 0 或 1，否则 400
- 当 `periodType = 1`（失效）：`1 ≤ periodDays ≤ 7`，否则 400
- 当 `periodType = 0`（删除）：`1 ≤ periodDays ≤ 30`，否则 400

**内部处理流程**

1. `AppContextResolver.resolveAndValidate(appId)` 校验权限
2. 从 `openplatform_app_p_t` 按 `appId + property_name='eamap_app_code'` 查 `clientId`
3. 从当前上下文取 `tenantId`
4. 调 `CardServiceClient.updatePeriod(tenantId, clientId, periodType, periodDays)`
5. 卡片服务内部会把 `periodDays` 映射为 `period` 字段传给卡片服务

**响应**（200）

```json
{
  "code": "200",
  "messageZh": "success",
  "messageEn": "success",
  "data": null,
  "page": null
}
```

> 注：卡片服务修改接口返回 `{status:1, data:"success"}`，open-server 不再回查，直接返回 200 空 data。前端保存成功后会**重新 GET** 以刷新展示（§2.1.4）。

### 2.5 后端 — `CardServiceClient`（卡片服务客户端）

> 卡片服务接口定义**已提供**（本次会话 2026-06-22）。本 Spec 规定我方调用它的 contract + 响应封装。

#### 2.5.1 卡片服务原始接口（外部）

**查询 GET**

| 项 | 值 |
|----|----|
| Path | `/interactive/card/businesscenter/period/setting/v1` |
| Query | `tenantId=${tenantId}&clientId=${eamapAppId}` |
| 成功响应 | `{status:1, data:{expirationPeriod:15, deletionPeriod:7}}` |
| 失败响应 | `{status:0, error:{code:400100, userMessageZh:"...", userMessageEn:"..."}}` |

**修改 PUT**

| 项 | 值 |
|----|----|
| Path | `/interactive/card/businesscenter/period/setting/v1`（**与查询同路径**，仅 HTTP 方法不同） |
| Body | `{tenantId, clientId, periodType, period}` |
| 成功响应 | `{status:1, data:"success"}` |
| 失败响应 | `{status:0, error:{code, userMessageZh, userMessageEn}}` |

> 来源：用户澄清（本次会话，2026-06-22）

#### 2.5.2 统一响应封装（open-server 内部）

```java
package com.xxx.it.works.wecode.v2.modules.card.client;

import lombok.Data;

/**
 * 卡片服务统一响应封装。
 * 成功：status=1, data 有值, error=null
 * 失败：status=0, data=null, error 有值
 */
@Data
public class CardServiceResponse<T> {
    private Integer status;
    private T data;
    private CardServiceError error;

    public boolean isSuccess() {
        return status != null && status == 1;
    }
}

@Data
public class CardServiceError {
    private Integer code;
    private String userMessageZh;
    private String userMessageEn;
}

/** 查询响应 data 部分 */
@Data
public class CardServicePeriodDTO {
    private Integer expirationPeriod;  // 映射为 open-server 的 expirationDays
    private Integer deletionPeriod;    // 映射为 open-server 的 deletionDays
}
```

#### 2.5.3 `CardServiceClient` 接口

```java
package com.xxx.it.works.wecode.v2.modules.card.client;

/**
 * 卡片服务客户端接口
 */
public interface CardServiceClient {

    /** 查询应用卡片周期设置 */
    CardServiceResponse<CardServicePeriodDTO> queryCardPeriod(String tenantId, String clientId);

    /** 更新卡片周期（periodType: 0=删除, 1=失效；period: 天数） */
    CardServiceResponse<String> updateCardPeriod(String tenantId, String clientId,
                                                  int periodType, int period);
}
```

> `clientId` 入参由 `CardSettingService` 从 `openplatform_app_p_t` 按 `appId + property_name='eamap_app_code'` 查出后传入。

---

## 3. Constraints

### 3.1 权限约束

- 两个接口统一使用 `AppContextResolver.resolveAndValidate(appId)` 校验：
  - 无效 appId → 抛 `AppAccessException`（不暴露内部细节）
  - 当前用户无该应用访问权 → 同上
- 来源：既有约定 [`open-server/.../modules/permission/service/PermissionService.java:103-104`]

### 3.2 数值约束（前端 + 后端双重校验）

> 校验规则现在按 `periodType` 动态决定范围。

| 字段 | 前端（InputNumber.min/max） | 后端（动态校验） |
|------|------------------------------|------------------|
| `periodDays`（失效，periodType=1） | `min=1, max=7` | `@NotNull` + `1 ≤ periodDays ≤ 7`（当 periodType=1） |
| `periodDays`（删除，periodType=0） | `min=1, max=30` | `@NotNull` + `1 ≤ periodDays ≤ 30`（当 periodType=0） |
| `periodType` | 前端不暴露（由 UI 哪个字段进入编辑态决定） | 必须为 0 或 1，否则 400 |

#### 3.2.1 编辑态值裁剪规则

> 来源：用户澄清（本次会话，2026-06-22）。

由于第三方查询返回值域为**任意整数**（可能超出用户可写范围，例如失效时间系统默认 14 天 > max=7），前端在从只读态切换到编辑态时，对 InputNumber 初始值做裁剪：

| 当前展示值 `v`（来自 GET） | InputNumber 初始编辑值 |
|----------------------------|------------------------|
| `null` | #ASSUMED 清空，显示 placeholder（文案待 OQ-8） |
| `v < min` | `min`（失效=1，删除=1） |
| `min ≤ v ≤ max` | `v` |
| `v > max` | `max`（失效=7，删除=30） |

保存时由 `@Min`/`@Max` 后端校验 + `InputNumber` 前端约束双重保障。

> 注：此规则仅影响**编辑态的初始值**，不影响**只读态的展示**——只读态始终展示 GET 返回的实际值（如 14）。

### 3.3 错误码约定

复用现有 `BusinessException(code, zhMsg, enMsg)` 三参数构造 [`open-server/.../common/exception/BusinessException.java`，参考 PermissionService.java:149 等]：

| 场景 | code | messageZh | messageEn |
|------|------|-----------|-----------|
| appId 无效或无权 | `403` | "无权限访问该应用" | "No permission to access this app" |
| 参数校验失败（数值越界/空/periodType 非法） | `400` | "参数校验失败：{field}" | "Validation failed: {field}" |
| `openplatform_app_p_t` 中查不到 `eamap_app_code` | `400` | "应用缺少 eamap_app_code 属性" | "App missing eamap_app_code property" |
| 卡片服务调用失败（网络异常/超时） | `502` | "卡片服务暂时不可用" | "Card service unavailable" |
| 卡片服务返回业务错误（status=0） | 透传 `error.code`（默认 `500`） | 透传 `error.userMessageZh` | 透传 `error.userMessageEn` |

> 卡片服务**网络层**调用失败（超时/连接异常）统一对外表现为 502（Bad Gateway），避免暴露卡片服务细节；卡片服务**业务层**错误（status=0）则透传其 code/message。

### 3.4 并发 / 一致性

- open-server **无本地状态**，不承担并发控制；所有写入直接转发到卡片服务。
- #ASSUMED 卡片服务自身负责幂等/并发控制；本板块不实现乐观锁或版本号。
- 两个字段**独立保存**（每次 PUT 仅携带一个 `periodType` + `periodDays`），可能出现"失效时间已保存但删除时间保存失败"的中间态——这是预期行为，前端通过分别的 message 提示即可。

### 3.5 可观测性

- Service 层入参打 info 日志，风格对齐 `PermissionService.java:100` 等。
- 卡片服务调用异常打 error 日志，包含 `appId` + `tenantId` + `clientId` + 入参 + 异常栈。
- #ASSUMED 不引入新的 Metrics/MDC 字段。

### 3.6 配置项

> **#ASSUMED** 卡片服务连接信息通过 `application.yml` 注入：

```yaml
card-service:
  base-url: ${CARD_SERVICE_BASE_URL:}           # 卡片服务 base URL
  timeout-ms: ${CARD_SERVICE_TIMEOUT_MS:5000}
  period-path: /interactive/card/businesscenter/period/setting/v1   # 查询 / 修改共用同一路径
  # 其他鉴权字段（如 API Key）待卡片服务方确认后补充
```

> 查询（GET）和修改（PUT）共用 `period-path`，仅 HTTP 方法不同。来源：用户澄清（本次会话，2026-06-22）。

---

## 4. Data

### 4.1 不引入本地数据表（卡片设置数据）

- 卡片设置数据 SoT 在卡片服务，open-server **不建表**存储该配置。
- 因此 `modules/card/` 模块**无 entity 目录、无 mapper 目录**。
- 但 `CardSettingService` 需要**读取** `openplatform_app_p_t` 表的 `eamap_app_code` 属性（#ASSUMED 复用已有 `AppPropertyRepository`，不新建表结构）。

### 4.2 后端 DTO（位于 `modules/card/dto/`）

> 卡片服务响应封装类（`CardServiceResponse` / `CardServiceError` / `CardServicePeriodDTO`）放在 `modules/card/client/` 下，详见 §2.5.2。

```java
// CardSettingResponse.java —— open-server GET 响应
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CardSettingResponse {
    private Integer expirationDays; // 可为 null（卡片服务未配置）
    private Integer deletionDays;   // 可为 null（卡片服务未配置）
}

// UpdateCardPeriodRequest.java —— open-server PUT 请求
@Data
public class UpdateCardPeriodRequest {
    @NotNull
    private Integer periodDays;   // 周期天数

    @NotNull
    private Integer periodType;   // 0=删除周期, 1=失效周期

    // 注意：范围校验依赖 periodType，无法用 @Min/@Max 静态表达，
    // 由 Service 层做动态校验（§3.2）或在 Controller 层用自定义 @Validator。
}
```

### 4.3 前端类型定义（位于 `services/card.service.ts`，详见 2.2）

`CardSetting / PeriodType / UpdateCardPeriodRequest`

### 4.4 代码落位（建议）

**卡片设置板块（`modules/card/`）**

| 位置 | 文件 | 说明 |
|------|------|------|
| `open-server/.../modules/card/controller/CardSettingController.java` | 新建 | 对外 2 个接口 |
| `open-server/.../modules/card/service/CardSettingService.java` | 新建 | 权限校验 + clientId 查询 + 调卡片服务 + 字段映射 |
| `open-server/.../modules/card/dto/CardSettingResponse.java` | 新建 | GET 响应 |
| `open-server/.../modules/card/dto/UpdateCardPeriodRequest.java` | 新建 | PUT 请求 |
| `open-server/.../modules/card/client/CardServiceClient.java` | 新建 | interface（§2.5.3） |
| `open-server/.../modules/card/client/CardServiceResponse.java` | 新建 | 卡片服务统一响应封装（§2.5.2） |
| `open-server/.../modules/card/client/CardServiceError.java` | 新建 | 卡片服务错误体（§2.5.2） |
| `open-server/.../modules/card/client/CardServicePeriodDTO.java` | 新建 | 卡片服务查询 data 部分（§2.5.2） |
| `open-server/.../modules/card/client/CardServiceClientImpl.java` | 新建 | HTTP 实现（RestTemplate / WebClient） |
| `open-server/.../modules/card/client/CardServiceClientStub.java` | #ASSUMED 新建 | stub 实现，便于联调前跑通 |

**应用属性查询（`modules/app/`，OQ-13 决策方案 A）**

| 位置 | 文件 | 说明 |
|------|------|------|
| `open-server/.../modules/app/entity/AppProperty.java` | 新建 | 对应 `openplatform_app_p_t` 表；参考 `modules/sync/entity/OldAppProperty.java` 字段（`parentId/propertyName/propertyValue`） |
| `open-server/.../modules/app/mapper/AppPropertyMapper.java` | 新建 | MyBatis Mapper 接口；关键方法：`selectByAppIdAndPropertyName(Long appId, String propertyName)` |
| `open-server/src/main/resources/mapper/app/AppPropertyMapper.xml` | 新建 | 对应 SQL |

> 注：卡片设置板块只在 `CardSettingService` 里通过 `AppPropertyMapper` 查 `eamap_app_code`。该 mapper 放在 `modules/app/` 下可被其他模块复用（未来如有需要）。

**前端（`open-web/src/`）**

| 位置 | 文件 | 说明 |
|------|------|------|
| `open-web/src/services/card.service.ts` | 新建 | §2.2 |
| `open-web/src/hooks/useCardSetting.ts` | 新建 | §2.3 |
| `open-web/src/components/CardSettingSection.tsx` | 新建 | §2.1（或放在 `pages/` 下，由同事 A 决定挂载点） |
| `open-web/src/components/CardSettingSection.module.less` | #ASSUMED 新建 | 样式文件 |

---

## 5. Test Cases

### 5.1 前端

| ID | 场景 | 步骤 | 预期 |
|----|------|------|------|
| TC-F01a | 正常加载（值在范围内） | 打开应用详情 → 滚动到卡片设置，GET 返回 `{expirationDays: 3, deletionDays: 7}` | 只读态显示：失效 `3 天 [修改]`、删除 `7 天 [修改]` |
| TC-F01b | 正常加载（值超出可写范围） | GET 返回 `{expirationDays: 14, deletionDays: 7}` | 只读态失效时间显示 `14 天 [修改]`（展示实际值，不裁剪） |
| TC-F01c | 正常加载（值为 null） | GET 返回 `{expirationDays: null, deletionDays: null}` | 只读态显示 `—` 或 placeholder #ASSUMED |
| TC-F02 | Tooltip 展示 | hover ❓ 图标 | 显示 §2.1.3 完整业务说明文案 |
| TC-F03 | 输入下限校验 | 进入编辑态后失效时间输入 `0` | 自动裁剪为 `1`（antd InputNumber min 行为） |
| TC-F04 | 输入上限校验 | 进入编辑态后失效时间输入 `10` | 自动裁剪为 `7` |
| TC-F05 | 步进按钮 | 点击 ▲ | 值 +1，不越 max |
| TC-F06 | 步进按钮 | 点击 ▼ | 值 -1，不低于 min |
| TC-F07 | 取消还原 | 修改值但未保存 → 点"取消" | 当前行还原到修改前的展示值，切回只读态 |
| TC-F08 | 保存成功 | 修改值 → 点"保存" → 接口 200 | 显示"保存成功"；**重新 GET**，用新返回值回填只读态展示 |
| TC-F09 | 保存失败 | 修改值 → 点"保存" → 接口 502 | 显示错误 message；保留当前输入值，停留在编辑态 |
| TC-F10 | 未输入合法值禁用 | 进入编辑态但未输入合法值（InputNumber 为空或越界） | #ASSUMED 保存按钮 disabled |
| TC-F11 | 编辑态裁剪（值 > max） | 当前只读态显示 14 天 → 点"修改" | InputNumber 初始显示 7（max），用户可在此基础上调整 |
| TC-F12 | 编辑态裁剪（null） | 当前只读态显示 `—` → 点"修改" | InputNumber 清空，显示 placeholder，保存按钮禁用 |
| TC-F13 | 间距一致性 | 目视检查只读态 `天 ↔ 修改` 与 编辑态 `天 ↔ 保存` 与 `保存 ↔ 取消` 间距 | 三组间距相等（对齐 antd `<Space>` 默认间距） |

### 5.2 后端 — Controller / Service

| ID | 场景 | 输入 | 预期 |
|----|------|------|------|
| TC-B01 | 查询成功 | 合法 appId，卡片服务返回 `{status:1, data:{expirationPeriod:14, deletionPeriod:7}}` | 200，`data={expirationDays:14, deletionDays:7}` |
| TC-B02 | 查询 - appId 无效 | `appId = "invalid"` | 抛 `AppAccessException` → 403 |
| TC-B03 | 查询 - 卡片服务未配置 | 卡片服务返回 `{status:1, data:{expirationPeriod:null, deletionPeriod:null}}` | 200，对应字段为 null |
| TC-B04 | 查询 - `openplatform_app_p_t` 缺 `eamap_app_code` | 应用无此属性 | 400 "应用缺少 eamap_app_code 属性"（查询不需要 clientId？#ASSUMED 查询也要） |
| TC-B05 | 更新失效周期 - 越界低 | `periodType=1, periodDays=0` | 400，参数校验失败 |
| TC-B06 | 更新失效周期 - 越界高 | `periodType=1, periodDays=8` | 400 |
| TC-B07 | 更新失效周期 - 成功 | `periodType=1, periodDays=3` | 200，调卡片服务 `updateCardPeriod(tenantId, clientId, 1, 3)` |
| TC-B08 | 更新删除周期 - 越界 | `periodType=0, periodDays=31` | 400 |
| TC-B09 | 更新删除周期 - 成功 | `periodType=0, periodDays=15` | 200，调卡片服务 `updateCardPeriod(tenantId, clientId, 0, 15)` |
| TC-B10 | 更新 - `periodType` 非法 | `periodType=2` | 400 |
| TC-B11 | 卡片服务网络异常 | 卡片服务超时 | 502 "卡片服务暂时不可用"，错误日志含 appId + tenantId + clientId + 入参 |
| TC-B12 | 卡片服务业务错误 | 卡片服务返回 `{status:0, error:{code:400100, userMessageZh:"租户ID或应用ID为空", ...}}` | 透传 400100 + 中/英文 message |
| TC-B13 | 更新请求体缺字段 | `{}` | 400（`@NotNull` 校验） |

### 5.3 卡片服务客户端（stub / mock 测试）

| ID | 场景 | 预期 |
|----|------|------|
| TC-T01 | stub 查询返回固定值 `{expirationPeriod:14, deletionPeriod:7}` | Service 正确映射为 `CardSettingResponse { expirationDays:14, deletionDays:7 }` |
| TC-T02 | stub 模拟网络超时 | Service 抛 `BusinessException("502", "卡片服务暂时不可用", ...)` |
| TC-T03 | stub 模拟卡片服务业务错误 `{status:0, error:{...}}` | Service 透传 code + message（§3.3） |
| TC-T04 | stub 查询响应 status 非 0/1 | #ASSUMED 视为异常，502 包装 |

> 卡片服务真实联调测试待网络打通后补充。

---

## 6. 待确认项（Open Questions）

> 以下项未解决不会阻塞实现骨架，但会阻塞最终联调。实现过程中应主动推进澄清。

| # | 问题 | 状态 | 影响 | 建议推进方 |
|---|------|------|------|-----------|
| OQ-1 | 同事 A 的应用详情 page 何时可挂载？`appId` prop 命名？ | 待解决 | 影响组件接入 | 同事 A |
| OQ-2 | ~~卡片服务接口定义~~ | ✅ 已解决（2026-06-22） | — | — |
| OQ-3 | ~~卡片服务是否以 externalAppId 作为入参~~ | ✅ 已解决（2026-06-22）：改为 `tenantId`（上下文取）+ `clientId`（`openplatform_app_p_t.eamap_app_code`） | — | — |
| OQ-4 | 是否需要 AuditLog（`OperateEnum.QUERY_CARD_SETTING / UPDATE_CARD_PERIOD`）？ | 待解决 | 影响 Controller 注解和 `OperateEnum` 新增 | 架构师 |
| OQ-5 | 正式接口编号（替换 `#TBD-CS01~02`） | 待解决 | 文档对齐 | 架构师 |
| OQ-6 | ~~未修改时保存按钮是否禁用？~~ | ✅ 已解决（2026-06-22）：未输入合法值时禁用 | — | — |
| OQ-7 | 板块视觉样式是否沿用现有板块（圆角卡片 / 标题字号 / padding）？ | 待解决 | 样式细节 | 设计 |
| OQ-8 | null 时只读态展示 `—` 还是 placeholder 文案？编辑态 placeholder 文案具体是什么？ | 待解决 | §2.1.2 / §3.2.1 的 #ASSUMED 落地 | 产品/设计 |
| OQ-9 | 两态 UI（只读态 / 编辑态切换）是否需要设计稿确认？ | 待解决 | §2.1.2 布局最终形态 | 设计 |
| OQ-10 | Tooltip 是否要同时展示数值范围提示（如 "（1~7 天）" 后缀）？ | 待解决 | §2.1.3 文案最终形态 | 产品/设计 |
| OQ-11 | 卡片服务**修改接口**的完整路径（查询是 `/interactive/card/businesscenter/period/setting/v1`，修改呢？） | 待解决 | §3.6 `card-service.update-path` 配置 | 卡片服务方 |
| OQ-12 | ~~`tenantId` 在 open-server 内如何获取？~~ | ✅ 已解决（2026-06-22）：**获取工具类已存在**；TODO 由人工二开时确认具体类名/方法名并对接；本 Spec **不新建** TenantContextHolder / TenantInterceptor | — | — |
| OQ-13 | ~~`openplatform_app_p_t` 是否有 mapper 可复用？~~ | ✅ 已解决（2026-06-22）：源码验证确认不存在，决策**方案 A**：新建 `modules/app/mapper/AppPropertyMapper.java` + 对应 MyBatis XML；可参考 `modules/sync/entity/OldAppProperty.java` 的表结构 | — | — |
| OQ-14 | `StandardAppContextResolver` 实现全是 TODO（L61-62 直接抛 `UnsupportedOperationException`），仅 `DevAppContextResolver` 可用（无权限校验）。**已决策：这是项目级 TODO，不在本 Spec 范围**。卡片设置板块在 Dev 模式下可正常运行；生产部署前由架构师/项目负责人单独排期解决。 | 已明确不纳入本 Spec | 项目整体生产化（不影响卡片板块实现） | 架构师 / 项目负责人（另案跟踪） |
| OQ-15 | ~~`request.ts` 拦截器已自动 `message.error`，hook 是否再手动提示？~~ | ✅ 已解决（2026-06-22）：**hook 不再手动 `message.error`**（避免双重提示），只做 console.error + 状态管理。§2.1.4 已更新。 | — | — |

---

## 7. 变更日志

| 版本 | 日期 | 变更 | 作者 |
|------|------|------|------|
| 0.1 | 2026-06-10 | 初稿 | Spec Agent |
| 0.2 | 2026-06-22 | UI 两态、默认值/裁剪规则、Tooltip 业务文案、间距约束 | Spec Agent + 用户澄清 |
| 0.3 | 2026-06-22 | 卡片服务接口明确；接口 3→2 合并；字段 `retentionDays`→`deletionDays`；新增 `tenantId`/`clientId` 来源；DTO 重构 | Spec Agent + 用户澄清 |
| 0.4 | 2026-06-22 | **源码验证**：8 个 Spec 锚点全部 ✅ 成立；发现 `TenantContext`/`AppPropertyMapper`/`StandardAppContextResolver` TODO 现状；决策 OQ-13 方案 A（新建 AppPropertyMapper）；明确 OQ-14 不在本 Spec 范围；修正 §2.1.4 错误提示避免双重 message | Spec Agent |
| 0.5 | 2026-06-22 | OQ-11 解决（修改接口路径与查询同 `/interactive/card/businesscenter/period/setting/v1`）；OQ-12 解决（tenantId 工具类已存在，TODO 人工二开对接）；§3.6 配置简化 | Spec Agent + 用户澄清 |
