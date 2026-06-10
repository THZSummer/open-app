# 卡片设置板块 - 技术规格 (Spec)

**Document ID:** SPEC-CARD-SETTING-001
**Version:** 0.1 (Draft)
**Created:** 2026-06-10
**Scope Owner:** open-server / open-web（卡片设置板块）
**Status:** 待评审

> **使用说明**
> - 每一条"结论"都标注了来源：`[src/path:line]` 表示从代码读取的事实，`#ASSUMED` 表示基于现有信息的推断（待确认）。
> - 本 Spec 不覆盖：应用详情 page 整体、应用凭证板块、应用基本信息板块、认证方式板块（这些由其他同事负责）。
> - 本 Spec 覆盖：在应用详情 page **末尾**新增的"卡片设置"板块（前端组件 + 后端接口 + 第三方服务客户端预留）。

---

## 1. Scope

### 1.1 业务背景

- 应用（App）是一种权益载体，其"卡片"权益由**第三方服务**提供，页面落在 open-web。
  - 来源：用户澄清（2026-06-10）
- 卡片是**应用级**资源：一个应用对应一套"定期失效时间 + 定期删除时间"配置，**无 cardId 维度**。
  - 来源：用户澄清（2026-06-10）
- 数据的权威来源（SoT）在第三方服务；open-server **不持久化**该配置，仅做权限校验 + 请求转发。
  - 来源：用户澄清（2026-06-10）

### 1.2 范围边界

| In Scope | Out of Scope |
|----------|--------------|
| 应用详情 page 末尾新增"卡片设置"板块（前端组件） | 应用详情 page 整体结构 / 路由 / 导航 |
| 前端：`CardSettingSection` 组件 + `card.service.ts` + `useCardSetting.ts` | 应用凭证 / 应用基本信息 / 认证方式板块 |
| 后端：新增 `card` 模块（controller / service / dto / 第三方客户端） | 本地数据库 entity / mapper（不存本地） |
| 第三方服务客户端 interface 预留（失效 / 删除两个独立端口） | 第三方服务的实现与部署 |
| 权限校验（复用 `AppContextResolver`） | 细粒度权限点（如 OperateEnum.MANAGE_CARD_SETTING） |

### 1.3 依赖

| 依赖方 | 内容 | 状态 |
|--------|------|------|
| 同事 A | 应用详情 page + 三个已有板块（凭证/基本信息/认证方式） | #ASSUMED 在并行开发中，本 Spec 假设已存在并可挂载 |
| 第三方服务 | 卡片设置查询 / 失效时间保存 / 删除时间保存 三个接口 | 接口定义待提供，本 Spec 用 `ThirdPartyCardClient` interface 占位 |
| `AppContextResolver` | 应用访问权限校验 | 已存在 [`open-server/.../modules/app/resolver/AppContextResolver.java:15-32`] |
| `ApiResponse<T>` | 统一响应包装 | 已存在 [`open-server/.../common/model/ApiResponse.java:25`] |
| `request.ts` | 前端 HTTP 客户端（baseURL=/api/v1，自动 Bearer token + X-App-Id） | 已存在 [`open-web/src/utils/request.ts:31-63`] |

### 1.4 接口编号

本 Spec 涉及的接口编号**未分配**。建议占位符：
- `#TBD-CS01` GET /apps/{appId}/card-settings（查询）
- `#TBD-CS02` PUT /apps/{appId}/card-settings/expiration（保存失效时间）
- `#TBD-CS03` PUT /apps/{appId}/card-settings/retention（保存删除时间）

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

#### 2.1.2 布局（自上而下平铺）

```
┌─ 卡片设置 ────────────────────────────────────────────────┐
│                                                            │
│  定期失效时间  [❓]  [  ▯ 3  ▲  ▼  ]  天   [保存] [取消]    │
│                                                            │
│  定期删除时间  [❓]  [  ▯ 15 ▲  ▼  ]  天   [保存] [取消]    │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

- 每行**独立状态、独立保存/取消**（因为后端是两个独立接口）
  - 来源：用户澄清（2026-06-10，"分别调用第三方服务不同的接口"）
- ❓ 图标：hover 显示 tooltip（内容见 2.1.3）
- 输入框：支持手动输入 + 右侧 ▲/▼ 步进按钮（点击 ±1 天）
- 单位"天"紧跟输入框
- 保存/取消按钮位于本行最右

> **#ASSUMED** 采用 antd 组件：`InputNumber`（内置步进按钮）+ `Tooltip` + `Button`。
> 依据：`open-web` 已引入 antd（见 `useCallback.ts:2` 的 `import { message } from 'antd'`）。

#### 2.1.3 Tooltip 文案（固定）

| 字段 | Tooltip 文案 |
|------|--------------|
| 定期失效时间 | "定期失效时间，最小1天，最大7天" |
| 定期删除时间 | "定期删除时间，最小1天，最大30天" |

来源：用户澄清（2026-06-10，原文）

#### 2.1.4 交互约束

| 交互 | 行为 |
|------|------|
| 进入板块 | 调 `GET /apps/{appId}/card-settings` 回显两字段当前值 |
| 输入值越界 | `InputNumber` 自带 min/max 约束（1-7 / 1-30），超出自动裁剪 |
| 修改后未保存 | 取消按钮 → 还原为上一次服务端值（非当前输入值） |
| 未修改时点保存 | #ASSUMED 禁用保存按钮（disabled）或请求直接跳过 |
| 保存成功 | `message.success('保存成功')`，刷新当前行显示值 |
| 保存失败 | `message.error(后端 messageZh)`，保留当前输入值供用户重试 |
| 并发编辑 | #ASSUMED 依赖第三方服务的语义，本板块不实现乐观锁 |

### 2.2 前端 — `card.service.ts`

> 命名/风格对齐现有 `callback.service.ts` [`open-web/src/services/callback.service.ts`]

```ts
import { get, put } from '@/utils/request';

// ===== 类型定义 =====

export interface CardSetting {
  expirationDays: number | null; // 定期失效时间（天），null 表示未设置
  retentionDays: number | null;  // 定期删除时间（天），null 表示未设置
}

export interface UpdateExpirationRequest {
  expirationDays: number; // 1..7
}

export interface UpdateRetentionRequest {
  retentionDays: number; // 1..30
}

// ===== API 函数 =====

/** #TBD-CS01 查询卡片设置 */
export const getCardSetting = (appId: string) =>
  get<CardSetting>(`/apps/${appId}/card-settings`);

/** #TBD-CS02 保存定期失效时间 */
export const updateCardExpiration = (appId: string, data: UpdateExpirationRequest) =>
  put<CardSetting>(`/apps/${appId}/card-settings/expiration`, data);

/** #TBD-CS03 保存定期删除时间 */
export const updateCardRetention = (appId: string, data: UpdateRetentionRequest) =>
  put<CardSetting>(`/apps/${appId}/card-settings/retention`, data);
```

### 2.3 前端 — `useCardSetting.ts`

> 风格对齐 `useCallbackManager` [`open-web/src/hooks/useCallback.ts:20-144`]

```ts
export const useCardSetting = () => {
  // state: loading / cardSetting / 两个字段各自的 saving 状态
  // actions: fetchCardSetting / handleSaveExpiration / handleSaveRetention
  // 返回：{ loading, savingExpiration, savingRetention, cardSetting, fetchCardSetting, handleSaveExpiration, handleSaveRetention }
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
    "expirationDays": 3,
    "retentionDays": 15
  },
  "page": null
}
```

`data` 字段类型：`CardSettingResponse { expirationDays: Integer, retentionDays: Integer }`，两字段**均可 null**（表示第三方未配置）。

#### 2.4.2 #TBD-CS02 保存定期失效时间

| 项 | 值 |
|----|----|
| Method | `PUT` |
| Path | `/service/open/v2/apps/{appId}/card-settings/expiration` |
| Auth | `AppContextResolver.resolveAndValidate(appId)` |
| AuditLog | #ASSUMED 新增 `OperateEnum.UPDATE_CARD_EXPIRATION`（待确认） |

**请求体**

```json
{ "expirationDays": 3 }
```

**校验规则**

| 字段 | 规则 |
|------|------|
| expirationDays | 非空；整数；`1 ≤ x ≤ 7` |

**响应**（200）：同 2.4.1（返回最新完整配置）

#### 2.4.3 #TBD-CS03 保存定期删除时间

| 项 | 值 |
|----|----|
| Method | `PUT` |
| Path | `/service/open/v2/apps/{appId}/card-settings/retention` |
| Auth | `AppContextResolver.resolveAndValidate(appId)` |
| AuditLog | #ASSUMED 新增 `OperateEnum.UPDATE_CARD_RETENTION`（待确认） |

**请求体**

```json
{ "retentionDays": 15 }
```

**校验规则**

| 字段 | 规则 |
|------|------|
| retentionDays | 非空；整数；`1 ≤ x ≤ 30` |

**响应**（200）：同 2.4.1

### 2.5 后端 — `ThirdPartyCardClient`（预留接口）

> 第三方接口定义待提供；本 Spec 只规定**我方调用它的 contract**，便于实现者后续填入具体实现。

```java
package com.xxx.it.works.wecode.v2.modules.card.client;

/**
 * 第三方卡片服务客户端接口（预留）
 *
 * <p>第三方接口定义待提供；本接口形状为占位，
 * 待第三方接口定义到位后由实现者调整签名与返回类型。</p>
 */
public interface ThirdPartyCardClient {

    /** 查询应用卡片设置 */
    ThirdPartyCardSettingDTO queryCardSetting(String externalAppId);

    /** 更新定期失效时间（第三方接口 #1） */
    ThirdPartyCardSettingDTO updateExpiration(String externalAppId, int expirationDays);

    /** 更新定期删除时间（第三方接口 #2） */
    ThirdPartyCardSettingDTO updateRetention(String externalAppId, int retentionDays);
}
```

> **#ASSUMED** 第三方以 `externalAppId`（长 ID）作为应用标识，与 `AppContext.getExternalId()` 对齐。需第三方接口定义确认后核对。

---

## 3. Constraints

### 3.1 权限约束

- 三个接口统一使用 `AppContextResolver.resolveAndValidate(appId)` 校验：
  - 无效 appId → 抛 `AppAccessException`（不暴露内部细节）
  - 当前用户无该应用访问权 → 同上
- 来源：既有约定 [`open-server/.../modules/permission/service/PermissionService.java:103-104`]

### 3.2 数值约束（前端 + 后端双重校验）

| 字段 | 前端（InputNumber.min/max） | 后端（`@Min` / `@Max`） |
|------|------------------------------|--------------------------|
| expirationDays | `min=1, max=7` | `@NotNull @Min(1) @Max(7)` |
| retentionDays | `min=1, max=30` | `@NotNull @Min(1) @Max(30)` |

### 3.3 错误码约定

复用现有 `BusinessException(code, zhMsg, enMsg)` 三参数构造 [`open-server/.../common/exception/BusinessException.java`，参考 PermissionService.java:149 等]：

| 场景 | code | messageZh | messageEn |
|------|------|-----------|-----------|
| appId 无效或无权 | `403` | "无权限访问该应用" | "No permission to access this app" |
| 参数校验失败（数值越界/空） | `400` | "参数校验失败：{field}" | "Validation failed: {field}" |
| 第三方服务调用失败 | `502` | "卡片服务暂时不可用" | "Card service unavailable" |
| 第三方返回业务错误 | 透传（默认 `500`） | 透传第三方 zh msg | 透传第三方 en msg |

> **#ASSUMED** 第三方调用失败统一对外表现为 502（Bad Gateway），避免暴露第三方细节。

### 3.4 并发 / 一致性

- open-server **无本地状态**，不承担并发控制；所有写入直接转发到第三方。
- #ASSUMED 第三方服务自身负责幂等/并发控制；本板块不实现乐观锁或版本号。
- 两个字段**独立保存**，可能出现"失效时间已保存但删除时间保存失败"的中间态——这是预期行为，前端通过分别的 message 提示即可。

### 3.5 可观测性

- Service 层入参打 info 日志，风格对齐 `PermissionService.java:100` 等。
- 第三方调用异常打 error 日志，包含 appId + 入参 + 异常栈。
- #ASSUMED 不引入新的 Metrics/MDC 字段。

### 3.6 配置项

> **#ASSUMED** 第三方服务连接信息通过 `application.yml` 注入：

```yaml
card-service:
  base-url: ${CARD_SERVICE_BASE_URL:}      # 第三方 base URL
  timeout-ms: ${CARD_SERVICE_TIMEOUT_MS:5000}
  # 其他鉴权字段待第三方接口定义到位后补充
```

---

## 4. Data

### 4.1 不引入本地数据表

- 卡片设置数据 SoT 在第三方服务，open-server **不建表**。
- 因此 `modules/card/` 模块**无 entity 目录、无 mapper 目录**。

### 4.2 后端 DTO（位于 `modules/card/dto/`）

```java
// CardSettingResponse.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CardSettingResponse {
    private Integer expirationDays; // 可为 null
    private Integer retentionDays;  // 可为 null
}

// UpdateExpirationRequest.java
@Data
public class UpdateExpirationRequest {
    @NotNull
    @Min(1) @Max(7)
    private Integer expirationDays;
}

// UpdateRetentionRequest.java
@Data
public class UpdateRetentionRequest {
    @NotNull
    @Min(1) @Max(30)
    private Integer retentionDays;
}

// ThirdPartyCardSettingDTO.java（内部用，第三方响应映射）
@Data @Builder
public class ThirdPartyCardSettingDTO {
    private Integer expirationDays;
    private Integer retentionDays;
    // #ASSUMED 第三方可能返回额外字段（如 cardStatus），统一映射到这里，透传前端不需要
}
```

### 4.3 前端类型定义（位于 `services/card.service.ts`，详见 2.2）

`CardSetting / UpdateExpirationRequest / UpdateRetentionRequest`

### 4.4 代码落位（建议）

| 位置 | 文件 |
|------|------|
| `open-server/.../modules/card/controller/CardSettingController.java` | 新建 |
| `open-server/.../modules/card/service/CardSettingService.java` | 新建 |
| `open-server/.../modules/card/dto/*.java` | 新建 4 个 DTO |
| `open-server/.../modules/card/client/ThirdPartyCardClient.java` | 新建 interface |
| `open-server/.../modules/card/client/ThirdPartyCardClientStub.java` | #ASSUMED 新建一个 stub 实现，便于联调前跑通 |
| `open-web/src/services/card.service.ts` | 新建 |
| `open-web/src/hooks/useCardSetting.ts` | 新建 |
| `open-web/src/components/CardSettingSection.tsx` | 新建（或放在 `pages/` 下，由同事 A 决定挂载点） |
| `open-web/src/components/CardSettingSection.module.less` | 新建 #ASSUMED |

---

## 5. Test Cases

### 5.1 前端

| ID | 场景 | 步骤 | 预期 |
|----|------|------|------|
| TC-F01 | 正常加载 | 打开应用详情 → 滚动到卡片设置 | 显示查询到的当前值；未设置时显示空 |
| TC-F02 | Tooltip 展示 | hover ❓ 图标 | 显示 2.1.3 对应文案 |
| TC-F03 | 输入下限校验 | 失效时间输入 `0` | 自动裁剪为 `1`（antd InputNumber min 行为） |
| TC-F04 | 输入上限校验 | 失效时间输入 `10` | 自动裁剪为 `7` |
| TC-F05 | 步进按钮 | 点击 ▲ | 值 +1，不越 max |
| TC-F06 | 步进按钮 | 点击 ▼ | 值 -1，不低于 min |
| TC-F07 | 取消还原 | 修改值但未保存 → 点"取消" | 值还原为上次服务端值 |
| TC-F08 | 保存成功 | 修改值 → 点"保存" → 接口 200 | 显示"保存成功"；显示值更新为新值 |
| TC-F09 | 保存失败 | 修改值 → 点"保存" → 接口 502 | 显示错误 message；保留当前输入值 |
| TC-F10 | 未修改禁用 | 打开页面后未改动 | #ASSUMED 保存按钮 disabled |

### 5.2 后端 — Controller / Service

| ID | 场景 | 输入 | 预期 |
|----|------|------|------|
| TC-B01 | 查询成功 | 合法 appId，第三方返回 `{3, 15}` | 200，data 完整 |
| TC-B02 | 查询 - appId 无效 | `appId = "invalid"` | 抛 `AppAccessException` → 403 |
| TC-B03 | 查询 - 第三方未配置 | 第三方返回 null 字段 | 200，对应字段为 null |
| TC-B04 | 失效时间保存 - 越界低 | `expirationDays = 0` | 400，参数校验失败 |
| TC-B05 | 失效时间保存 - 越界高 | `expirationDays = 8` | 400 |
| TC-B06 | 失效时间保存 - 成功 | `expirationDays = 3` | 200，调第三方 `updateExpiration`，返回最新配置 |
| TC-B07 | 删除时间保存 - 越界 | `retentionDays = 31` | 400 |
| TC-B08 | 删除时间保存 - 成功 | `retentionDays = 15` | 200 |
| TC-B09 | 第三方调用失败 | 第三方抛异常 | 502 "卡片服务暂时不可用"，错误日志含 appId + 入参 |
| TC-B10 | 保存请求体缺字段 | `{}` | 400（`@NotNull` 校验） |

### 5.3 第三方客户端（stub / mock 测试）

| ID | 场景 | 预期 |
|----|------|------|
| TC-T01 | stub 实现返回固定值 | Service 能正确映射为 `CardSettingResponse` |
| TC-T02 | stub 模拟超时 | Service 抛 `BusinessException("502", ...)` |
| TC-T03 | stub 模拟业务错误 | Service 透传或包装为 500/502（按 3.3 约定） |

> 真实第三方联调测试待接口定义到位后补充。

---

## 6. 待确认项（Open Questions）

> 以下项未解决不会阻塞实现骨架，但会阻塞最终联调。实现过程中应主动推进澄清。

| # | 问题 | 影响 | 建议推进方 |
|---|------|------|-----------|
| OQ-1 | 同事 A 的应用详情 page 何时可挂载？`appId` prop 命名？ | 影响组件接入 | 同事 A |
| OQ-2 | 第三方卡片服务接口定义（query / updateExpiration / updateRetention） | 影响 `ThirdPartyCardClient` 签名和 `ThirdPartyCardSettingDTO` 字段 | 第三方服务负责人 |
| OQ-3 | 第三方是否以 `externalAppId` 作为入参？ | 影响 2.5 节 contract | 第三方 |
| OQ-4 | 是否需要 AuditLog（`OperateEnum.QUERY/UPDATE_CARD_*`）？ | 影响 Controller 注解和 `OperateEnum` 新增 | 架构师 |
| OQ-5 | 正式接口编号（替换 `#TBD-CS01~03`） | 文档对齐 | 架构师 |
| OQ-6 | 未修改时保存按钮是否禁用？ | 前端细节 | 产品/设计 |
| OQ-7 | 板块视觉样式是否沿用现有板块（圆角卡片 / 标题字号 / padding）？ | 样式细节 | 设计 |

---

## 7. 变更日志

| 版本 | 日期 | 变更 | 作者 |
|------|------|------|------|
| 0.1 | 2026-06-10 | 初稿 | Spec Agent |
