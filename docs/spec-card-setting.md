# 卡片设置板块 - 技术规格 (Spec)

**Document ID:** SPEC-CARD-SETTING-001
**Version:** 0.6 (Draft)
**Created:** 2026-06-10
**Updated:** 2026-06-24
**Scope Owner:** open-server / wecodesite（卡片设置板块）
**Status:** 待评审

> **使用说明**
> - 每一条"结论"都标注了来源：`[src/path:line]` 表示从代码读取的事实，`#ASSUMED` 表示基于现有信息的推断（待确认）。
> - 本 Spec 不覆盖：应用详情 `/basic-info` 页面整体、应用凭证 / 基础信息 / 认证方式板块（由其他同事负责）。
> - 本 Spec 覆盖：在应用详情 `/basic-info` 页面**末尾**（"认证方式"Card 下方）新增的"卡片设置" Card 子板块（前端局部渲染函数 + thunk + 后端接口 + 卡片服务客户端）。
> - **v0.6 重大变更**：
>   - 前端承载从 `open-web` 迁移到 `wecodesite`；
>   - 新板块嵌入 BasicInfo 第 4 个 Card 位，沿用 BasicInfo 既有 `.info-card` 视觉 + `extra` 区"编辑"按钮 + `card-footer` 底部按钮区；
>   - 内部交互保留 v0.5 的"每行独立保存（每次 1 个 PUT）"语义；
>   - 技术栈从 TS 改为 JS（wecodesite 全仓库 JS/JSX）；
>   - HTTP 客户端从 `request.ts` 改为 `fetchApi`（wecodesite 原生 fetch 封装）；
>   - 错误处理从"依赖全局拦截器"改为"手动 `message.error`"；
>   - 后端 Spec 全部保留。

---

## 1. Scope

### 1.1 业务背景

- 应用（App）是一种权益载体，其"卡片"权益由**卡片服务**（外部独立服务）提供，页面落在 wecodesite。
  - 来源：用户澄清（2026-06-10；2026-06-24 确认承载页面为 wecodesite，open-web 已废弃）
- 卡片是**应用级**资源：一个应用对应一套"定期失效时间 + 定期删除时间"配置，**无 cardId 维度**。
  - 来源：用户澄清（2026-06-10）
- 数据的权威来源（SoT）在卡片服务；open-server **不持久化**该配置，仅做权限校验 + 请求转发。
  - 来源：用户澄清（2026-06-10）
- 关键标识来源：
  - `appId`：`openplatform_app_t` 表的 `appId` 字段（path 参数）
  - `tenantId`：当前用户上下文（#ASSUMED 通过 `TenantContext` 或等价机制获取；**获取工具类已存在**，TODO 由人工二开对接）
  - `clientId`（即 `eamapAppId`）：从 `openplatform_app_p_t` 表按 `appId + property_name='eamap_app_code'` 查 `property_value`
  - 来源：用户澄清（2026-06-22）
- 第三方查询返回值域为**任意整数**（可能超出用户可写范围）。例如失效时间的系统默认值为 14 天，但用户可配置范围为 1~7 天；删除时间系统默认 7 天，用户可配置范围 1~30 天。
  - 来源：用户澄清（2026-06-22）
- 前端**只读态展示实际值**（如 14 天就显示 14 天），**编辑态按 §3.2.1 裁剪**到用户可写范围。保存成功后重新 GET 回填。
  - 来源：用户澄清（2026-06-22）

### 1.2 范围边界

| In Scope | Out of Scope |
|----------|--------------|
| 应用详情 `/basic-info` 页面末尾新增"卡片设置"Card 子板块（BasicInfo.jsx 内局部 `renderCardSetting` 函数，沿用既有 Card 模式） | 应用详情 page 整体结构 / 路由 / Sidebar 菜单 |
| 前端：`BasicInfo.jsx` 追加 state + `renderCardSetting` + handlers；`BasicInfo.thunk.js` 追加 2 个接口；`BasicInfo.m.less` 追加（可选）样式 | 应用凭证 / 基础信息 / 认证方式板块 |
| 后端：新增 `card` 模块（controller / service / dto / 卡片服务客户端） | 本地数据库 entity / mapper（卡片设置数据不存本地） |
| 卡片服务客户端（查询 / 修改两个接口，封装统一响应） | 卡片服务的实现与部署 |
| 权限校验（复用 `AppContextResolver`） | 细粒度权限点（如 OperateEnum.MANAGE_CARD_SETTING） |
| 应用属性查询（从 `openplatform_app_p_t` 读取 `eamap_app_code` 作为 `clientId`） | `openplatform_app_p_t` 表本身的维护 |

### 1.3 依赖

| 依赖方 | 内容 | 状态 |
|--------|------|------|
| 同事 A | 应用详情 `/basic-info` 页面 + 三个已有 Card（凭证 / 基础信息 / 认证方式） | ✅ 已存在 [`wecodesite/src/pages/BasicInfo/BasicInfo.jsx:182-237`] |
| 卡片服务 | 查询 / 修改 两个接口（详见 §2.5） | 接口定义**已提供** |
| `AppContextResolver` | 应用访问权限校验 | ✅ 已存在 [`open-server/.../modules/app/resolver/AppContextResolver.java:15-32`]；`StandardAppContextResolver` 实现是 TODO（OQ-14，不在本 Spec 范围） |
| `ApiResponse<T>` | 统一响应包装 | ✅ 已存在 [`open-server/.../common/model/ApiResponse.java:25`]；字段 `code/messageZh/messageEn/data/page` |
| `BusinessException` | 三参数业务异常 | ✅ 已存在 [`open-server/.../common/exception/BusinessException.java:33`]；构造 `(code, zh, en)`；工厂方法 `badRequest/unauthorized/forbidden/notFound/internalError` |
| `fetchApi` + `buildApiUrl` | wecodesite HTTP 客户端 | ✅ 已存在 [`wecodesite/configs/web.config.js:117,109`]；baseURL=`/service/open/v2`（与 §2.4 后端路径前缀对齐）；`credentials: 'include'`（Cookie）；**无业务错误拦截器**（板块内手动 `message.error`） |
| `API_CONFIG` | wecodesite URL 路径常量集 | ✅ 已存在 [`wecodesite/configs/web.config.js:1`]；本板块需在 `API_CONFIG.APP_APIS` 追加 `CARD_SETTINGS` 项 |
| `antd` v4 | UI 组件库 | ✅ 已存在 [`wecodesite/package.json:7`]（`antd@4.24.16`）；`<Card>` / `<Form>` / `<InputNumber>` / `<Tooltip>` / `<Button>` / `<Space>` / `message` |
| `useSearchParams` | URL query 参数 | ✅ 已存在 [`wecodesite/src/pages/BasicInfo/BasicInfo.jsx:16-17`]；`appId` 从此取（`searchParams.get('appId')`） |
| `AppPropertyMapper`（新建） | 按 `appId + property_name='eamap_app_code'` 查 `property_value` 作为 `clientId` | ⚠️ **不存在**，需新建 `modules/app/mapper/AppPropertyMapper.java` + XML（OQ-13 已决策方案 A）；可参考 `modules/sync/entity/OldAppProperty.java` 的表结构 |
| `tenantId` 获取 | 调卡片服务需传 `tenantId` | ✅ **获取工具类已存在**（具体类名/方法名 TODO 由人工二开时对接）；本 Spec 不新建工具类/拦截器 |
| `OperateEnum`（新增枚举） | 卡片设置审计日志枚举 | ⚠️ **不存在**，待 OQ-4 决策 |

### 1.4 接口编号

本 Spec 涉及的接口编号**未分配**。建议占位符（共 2 个，已合并失效/删除为单一 PUT）：
- `#TBD-CS01` GET `/apps/{appId}/card-settings`（查询）
- `#TBD-CS02` PUT `/apps/{appId}/card-settings`（更新周期：通过 body 中 `periodType` 区分失效/删除）

> 待与 PermissionController 的 #27~#43 体系统一分配正式编号。[`open-server/.../modules/permission/controller/PermissionController.java:40`]

---

## 2. Interface

### 2.1 前端 — BasicInfo 内"卡片设置"Card 子板块

> **v0.6 设计**：外层视觉沿用 BasicInfo 既有 Card 模式（`.info-card` 容器 + `extra` 区"编辑"按钮 + `card-footer` 底部按钮区），内部交互保留 v0.5 的"每行独立保存（每次 1 个 PUT）"语义。这是用户决策的**折中方案 A**。

#### 2.1.1 接入点

- **位置**：`/basic-info` 路由页内，"认证方式"Card 下方（BasicInfo 末尾）
- **精确插入点**：[`wecodesite/src/pages/BasicInfo/BasicInfo.jsx:235`] `</Card>`（认证方式 Card 闭合）之后、[BasicInfo.jsx:236] `</div>`（`<div className="basic-info">` 闭合）之前
- **容器**：`<Card title="卡片设置" className="info-card" style={{ marginBottom: 16 }}>`
- **`appId` 来源**：BasicInfo 主组件从 `useSearchParams` 取（[BasicInfo.jsx:16-17]），作为局部变量传给 `renderCardSetting` 与 handlers

#### 2.1.2 布局（整体编辑入口 + 每行独立保存）

**只读态（默认）**

```
┌─ 卡片设置 ──────────────────────────────── [编辑] ─┐
│                                                     │
│  定期失效时间  [❓]  14 天                          │
│                                                     │
│  定期删除时间  [❓]  7 天                           │
│                                                     │
└─────────────────────────────────────────────────────┘
```

- Card `extra` 区显示"编辑"按钮（与"基础信息" / "认证方式" Card 一致）
- 每行展示字段值（可能超出可写范围，如 14 天）
- 字段未设置（null）时 #ASSUMED 显示 `— 天`

**整体编辑态（点击"编辑"后）**

```
┌─ 卡片设置 ─────────────────────────────────────────┐
│                                                     │
│  定期失效时间  [❓]  [ ▯ 7 ▲ ▼ ] 天    [保存]      │
│                                                     │
│  定期删除时间  [❓]  [ ▯ 7 ▲ ▼ ] 天    [保存]      │
│                                                     │
│  ─────────────────────────────────────── [取消] ─── │
└─────────────────────────────────────────────────────┘
```

- Card `extra` 区**不再**显示"编辑"按钮（因为已在编辑态）
- 两行同时进入编辑态，InputNumber 初始值按 §3.2.1 裁剪
- 每行末尾独立"保存"按钮（每次只发本行的 1 个 PUT）
- Card 底部 `card-footer` 区整体"取消"按钮（还原所有 editing 行）

**混合态（一行已保存，一行还在编辑）**

```
┌─ 卡片设置 ─────────────────────────────────────────┐
│                                                     │
│  定期失效时间  [❓]  3 天                           │
│                                                     │
│  定期删除时间  [❓]  [ ▯ 7 ▲ ▼ ] 天    [保存]      │
│                                                     │
│  ─────────────────────────────────────── [取消] ─── │
└─────────────────────────────────────────────────────┘
```

- 已保存行自动回到只读态
- 仍在编辑的行保留 InputNumber + 保存按钮
- Card `extra` 区仍无"编辑"按钮（因为还有行在编辑）
- 底部"取消"按钮只还原仍在编辑的行

**退出编辑态的条件**

- 当所有行都回到只读态时，自动退出整体编辑态
- Card `extra` 区重新显示"编辑"按钮

**间距约束（统一）**

三组间距全部对齐 BasicInfo 既有规范：

| 间距对 | 对齐到 |
|--------|--------|
| Card 之间 | `marginBottom: 16`（既有 Card 间距） |
| 编辑态：`天` ↔ 行内 `保存` | antd `<Space>` 默认间距（8px） |
| 编辑态：`card-footer` 内 `取消` ↔ `保存` | 既有 card-footer 样式 [`BasicInfo.m.less:101-108`] |

> **#ASSUMED** 采用 antd 组件：`<Card>` + `<Form>` + `<Form.Item>` + `<InputNumber>` + `<Tooltip>` + `<Button>` + `<Space>` + `QuestionCircleOutlined`。
> 依据：wecodesite 已引入 antd v4（[`package.json:7`]）。

#### 2.1.3 Tooltip 文案（完整业务说明）

> 来源：用户澄清（2026-06-22）。

| 字段 | Tooltip 文案 |
|------|--------------|
| 定期失效时间 | "根据每张消息卡片第一次投放时间开始计算，系统按设置的时间自动对卡片进行失效，失效的卡片在端侧不再支持交互" |
| 定期删除时间 | "只有失效的卡片可以删除，根据每张消息卡片失效时间开始计算，系统按照设置的时间自动对卡片进行删除" |

> 注：原 v0.1 中"最小1天，最大7天"等数值范围说明**从 Tooltip 中移除**（数值约束改由 §3.2 的前后端校验保证，不在 Tooltip 重复）。如设计/产品希望同时展示范围提示，待 OQ-10 决策。

#### 2.1.4 交互约束

**生命周期**

| 交互 | 行为 |
|------|------|
| 进入 `/basic-info` | BasicInfo.jsx `useEffect` 内调用 `fetchCardSetting(appId)` 获取两字段当前值，存入 `cardSetting` state，以**只读态**展示 |
| 点击"编辑" | 整个 Card 进入**整体编辑态**，两行 InputNumber 初始值按 §3.2.1 裁剪，存入 `cardSettingDraft` state |
| 点击某行"保存" | 只发本行的 1 个 PUT（`periodType` + `periodDays`） |
| 单行保存成功 | `message.success('保存成功')` → **重新 GET** → 用新返回值回填 `cardSetting` → 本行自动回只读态；另一行不受影响 |
| 单行保存失败 | **手动 `message.error(res?.messageZh \|\| '保存失败')`** → 本行保留编辑态（值供用户重试）；另一行不受影响 |
| 点击底部"取消" | 所有 editing 行整体还原到上次 GET 的展示值（`cardSetting` 对应字段） → 所有行回只读态 → 退出编辑态 |

> **v0.6 关键语义**：
> - wecodesite **无全局 HTTP 错误拦截器**，错误提示必须**手动**在 thunk / handler 内调用 `message.error`
> - 风格对齐 [`useSubscriptionList.js:53,108,166,211`]、[`ApiPermissionDrawer.jsx:148,174,222`]
> - **无"部分成功"场景**：每次只发 1 个 PUT，每次结果独立处理

**编辑态输入约束**

| 交互 | 行为 |
|------|------|
| 输入值越界 | `InputNumber` 自带 min/max 约束（失效 1-7 / 删除 1-30），超出自动裁剪 |
| 未输入合法值时 | #ASSUMED 本行保存按钮 disabled |

**并发 / 一致性**

| 交互 | 行为 |
|------|------|
| 并发编辑 | #ASSUMED 依赖卡片服务语义，本板块不实现乐观锁 |
| 两行独立保存 | 允许"失效时间已保存但删除时间未保存"的中间态（§3.4 已说明为预期行为） |

### 2.2 前端 — `BasicInfo.thunk.js` 追加接口

> v0.6 变更：从独立 `card.service.ts` 改为并入 `pages/BasicInfo/thunk.js`，与 `fetchAppInfo` / `bindEamapToApp` 并列。

```js
// 在 wecodesite/src/pages/BasicInfo/thunk.js 追加

import { fetchApi, buildApiUrl, API_CONFIG } from '../../configs/web.config';
import { message } from 'antd';

/**
 * #TBD-CS01 查询应用卡片设置（失效周期 + 删除周期）
 * @param {string} appId
 * @returns {Promise<{expirationDays: number|null, deletionDays: number|null}|null>}
 */
export const fetchCardSetting = async (appId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_APIS.CARD_SETTINGS, { appId });
    const res = await fetchApi(url, { method: 'GET' });
    if (res?.code === '200' || res?.code === 200) {
      return res.data;
    }
    message.error(res?.messageZh || res?.message || '查询卡片设置失败');
    return null;
  } catch (err) {
    console.error('fetchCardSetting error:', err);
    message.error('查询卡片设置失败');
    return null;
  }
};

/**
 * #TBD-CS02 更新单个卡片周期（失效或删除）
 * @param {string} appId
 * @param {0|1} periodType - 0=删除周期, 1=失效周期
 * @param {number} periodDays - 周期天数
 * @returns {Promise<{success: boolean, message?: string}>}
 */
export const updateCardPeriod = async (appId, periodType, periodDays) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_APIS.CARD_SETTINGS, { appId });
    const res = await fetchApi(url, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ periodType, periodDays }),
    });
    if (res?.code === '200' || res?.code === 200) {
      return { success: true };
    }
    return {
      success: false,
      message: res?.messageZh || res?.message || '保存失败',
    };
  } catch (err) {
    console.error('updateCardPeriod error:', err);
    return { success: false, message: '卡片服务暂时不可用' };
  }
};
```

> 注：`API_CONFIG.APP_APIS.CARD_SETTINGS` 需在 [`web.config.js:1`] 的 `API_CONFIG.APP_APIS` 中追加，值为 `/apps/{appId}/card-settings`（路径模板，`buildApiUrl` 会替换 `{appId}`）。
>
> 错误处理语义：**thunk 层不直接 `message.error`**（让 handler 决定如何提示），只返回 `{success, message}` 结构化结果。查询接口 `fetchCardSetting` 例外（无 handler 场景，thunk 内直接提示）。

### 2.3 前端 — BasicInfo.jsx 状态与裁剪逻辑

> v0.6 变更：从独立 `useCardSetting.ts` hook 改为 BasicInfo.jsx 内联 state + 工具函数。

#### 2.3.1 字段约束配置（模块级常量）

```js
// 在 BasicInfo.jsx 顶部（模块级）追加

/** 字段约束配置（与 open-web useCardSetting.ts 的 FIELD_CONSTRAINTS 对齐） */
const CARD_FIELD_CONSTRAINTS = {
  expiration: { min: 1, max: 7, periodType: 1 },
  deletion:   { min: 1, max: 30, periodType: 0 },
};

/**
 * 展示值 → 编辑态初值裁剪
 * - null → null（显示 placeholder）
 * - v < min → min
 * - min ≤ v ≤ max → v
 * - v > max → max
 */
const clampToEditable = (v, field) => {
  if (v == null) return null;
  const { min, max } = CARD_FIELD_CONSTRAINTS[field];
  return Math.max(min, Math.min(max, Math.round(v)));
};
```

#### 2.3.2 BasicInfo 组件内追加 state

```js
// 在 BasicInfo 组件内追加
const [cardSetting, setCardSetting] = useState({
  expirationDays: null,
  deletionDays: null,
});
const [cardSettingEditing, setCardSettingEditing] = useState(false);
const [cardSettingDraft, setCardSettingDraft] = useState({
  expiration: null, // 失效行编辑值
  deletion: null,   // 删除行编辑值
});
const [cardSettingRowSaving, setCardSettingRowSaving] = useState({
  expiration: false,
  deletion: false,
});

// 在 useEffect 内追加（随 appId 变化重新拉取）
useEffect(() => {
  fetchCardSetting(appId).then((data) => {
    if (data) setCardSetting(data);
  });
}, [appId]);
```

#### 2.3.3 handlers

```js
/** 进入整体编辑态 */
const handleCardSettingEdit = () => {
  setCardSettingDraft({
    expiration: clampToEditable(cardSetting.expirationDays, 'expiration'),
    deletion: clampToEditable(cardSetting.deletionDays, 'deletion'),
  });
  setCardSettingEditing(true);
};

/** 底部"取消"：还原所有 editing 行，退出编辑态 */
const handleCardSettingCancel = () => {
  setCardSettingEditing(false);
  setCardSettingDraft({ expiration: null, deletion: null });
  setCardSettingRowSaving({ expiration: false, deletion: false });
};

/** 单行"保存"：发本行的 1 个 PUT */
const handleCardSettingSaveRow = async (field) => {
  const constraint = CARD_FIELD_CONSTRAINTS[field];
  const draftValue = cardSettingDraft[field];
  if (draftValue == null || draftValue < constraint.min || draftValue > constraint.max) {
    return; // 非法值，不应该发生（InputNumber 已约束）
  }
  setCardSettingRowSaving({ ...cardSettingRowSaving, [field]: true });
  try {
    const result = await updateCardPeriod(appId, constraint.periodType, draftValue);
    if (result.success) {
      message.success('保存成功');
      // 重新 GET 回填 cardSetting（含两字段）
      const fresh = await fetchCardSetting(appId);
      if (fresh) setCardSetting(fresh);
      // 本行回只读
      setCardSettingDraft({ ...cardSettingDraft, [field]: null });
      // 如果两行都已只读，退出整体编辑态
      const otherField = field === 'expiration' ? 'deletion' : 'expiration';
      if (cardSettingDraft[otherField] == null) {
        setCardSettingEditing(false);
      }
    } else {
      message.error(result.message || '保存失败');
      // 本行保留编辑态，值供重试
    }
  } finally {
    setCardSettingRowSaving({ ...cardSettingRowSaving, [field]: false });
  }
};
```

> **状态机要点**：
> - `cardSetting`：最近一次 GET 的权威值（用于只读态展示）
> - `cardSettingDraft[field]`：某行 editing 时的工作值；`null` 表示该行处于只读态
> - `cardSettingEditing`：整体编辑态 flag（控制 Card `extra` 区是否显示"编辑"按钮）
> - 退出编辑态的条件：**两行 draft 都 == null** 时自动退出

#### 2.3.4 `renderCardSetting` 局部渲染函数

```js
const renderCardSetting = () => (
  <div className="formContent">
    <Form layout="horizontal">
      <Form.Item label="定期失效时间">
        <Space>
          {cardSettingDraft.expiration != null ? (
            <InputNumber
              min={1}
              max={7}
              value={cardSettingDraft.expiration}
              onChange={(v) =>
                setCardSettingDraft({ ...cardSettingDraft, expiration: v })
              }
              disabled={cardSettingRowSaving.expiration}
            />
          ) : (
            <span className="readonly-value">
              {cardSetting.expirationDays ?? '—'} 天
            </span>
          )}
          <Tooltip title="根据每张消息卡片第一次投放时间开始计算，系统按设置的时间自动对卡片进行失效，失效的卡片在端侧不再支持交互">
            <QuestionCircleOutlined />
          </Tooltip>
          {cardSettingDraft.expiration != null && (
            <Button
              type="primary"
              size="small"
              loading={cardSettingRowSaving.expiration}
              onClick={() => handleCardSettingSaveRow('expiration')}
            >
              保存
            </Button>
          )}
        </Space>
      </Form.Item>

      <Form.Item label="定期删除时间">
        <Space>
          {cardSettingDraft.deletion != null ? (
            <InputNumber
              min={1}
              max={30}
              value={cardSettingDraft.deletion}
              onChange={(v) =>
                setCardSettingDraft({ ...cardSettingDraft, deletion: v })
              }
              disabled={cardSettingRowSaving.deletion}
            />
          ) : (
            <span className="readonly-value">
              {cardSetting.deletionDays ?? '—'} 天
            </span>
          )}
          <Tooltip title="只有失效的卡片可以删除，根据每张消息卡片失效时间开始计算，系统按照设置的时间自动对卡片进行删除">
            <QuestionCircleOutlined />
          </Tooltip>
          {cardSettingDraft.deletion != null && (
            <Button
              type="primary"
              size="small"
              loading={cardSettingRowSaving.deletion}
              onClick={() => handleCardSettingSaveRow('deletion')}
            >
              保存
            </Button>
          )}
        </Space>
      </Form.Item>
    </Form>
  </div>
);
```

#### 2.3.5 JSX：在认证方式 Card 后追加

```jsx
{/* BasicInfo.jsx:235 之后插入 */}
<Card
  title="卡片设置"
  className="info-card"
  style={{ marginBottom: 16 }}
  extra={
    !cardSettingEditing && (
      <Button type="link" onClick={handleCardSettingEdit}>
        编辑
      </Button>
    )
  }
>
  {renderCardSetting()}
  {cardSettingEditing && (
    <div className="card-footer">
      <Button onClick={handleCardSettingCancel}>取消</Button>
    </div>
  )}
</Card>
```

> **说明**：上述为伪代码级规格，具体 JSX 细节（Tooltip 位置、`<Form.Item>` label 对齐、`readonly-value` 样式）由实现者按既有 Card 风格微调。Spec 只约束：
> - 容器 = `<Card title="卡片设置" className="info-card">`
> - 整体编辑入口（`extra` 区"编辑"按钮）
> - 每行独立保存（每次 1 个 PUT）
> - 底部整体"取消"
> - 裁剪规则（§3.2.1）
> - 错误提示语义（§2.1.4）
> - 保存成功后重新 GET 回填

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

> 来源：用户澄清（2026-06-22）

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
| `periodType` | 前端不暴露（由 UI 哪一行决定） | 必须为 0 或 1，否则 400 |

#### 3.2.1 编辑态值裁剪规则

> 来源：用户澄清（2026-06-22）。

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
- **v0.6 设计**：前端每次"保存"只发 1 个 PUT（单字段），**无"部分成功"场景**。允许"失效时间已保存但删除时间未保存"的中间态——这是预期行为，前端通过每行独立的 message 提示即可。

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
  # 其他鉴权字段（如 API Key）待卡片服务方确认后补充（OQ-16）
```

> 查询（GET）和修改（PUT）共用 `period-path`，仅 HTTP 方法不同。来源：用户澄清（2026-06-22）。

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

### 4.3 前端类型定义（位于 `BasicInfo.thunk.js`，JSDoc）

> v0.6 变更：wecodesite 为 JavaScript，无静态类型。"类型"以 JSDoc 注释形式存在于 thunk 函数上（详见 §2.2）。

```js
/**
 * @typedef {Object} CardSetting
 * @property {number|null} expirationDays - 定期失效时间（天）
 * @property {number|null} deletionDays - 定期删除时间（天）
 */

/**
 * @typedef {Object} UpdateCardPeriodResult
 * @property {boolean} success - 是否成功
 * @property {string} [message] - 失败时的错误信息
 */
```

### 4.4 代码落位

**后端 `modules/card/`**

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

**后端 `modules/app/`（OQ-13 决策方案 A）**

| 位置 | 文件 | 说明 |
|------|------|------|
| `open-server/.../modules/app/entity/AppProperty.java` | 新建 | 对应 `openplatform_app_p_t` 表；参考 `modules/sync/entity/OldAppProperty.java` 字段（`parentId/propertyName/propertyValue`） |
| `open-server/.../modules/app/mapper/AppPropertyMapper.java` | 新建 | MyBatis Mapper 接口；关键方法：`selectByAppIdAndPropertyName(Long appId, String propertyName)` |
| `open-server/src/main/resources/mapper/app/AppPropertyMapper.xml` | 新建 | 对应 SQL |

**前端 wecodesite（v0.6 修订）**

| 位置 | 文件 | 改动类型 | 说明 |
|------|------|----------|------|
| `wecodesite/src/pages/BasicInfo/BasicInfo.jsx` | 修改 | import + state + handlers + `renderCardSetting` + 1 个 `<Card>` JSX | 详见 §2.3 |
| `wecodesite/src/pages/BasicInfo/thunk.js` | 修改 | 追加 `fetchCardSetting` / `updateCardPeriod` | 详见 §2.2 |
| `wecodesite/src/pages/BasicInfo/BasicInfo.m.less` | 修改（可选） | 追加 `.readonly-value` 等（若既有样式不足） | 由实现者判断 |
| `wecodesite/configs/web.config.js` | 修改 | `API_CONFIG.APP_APIS.CARD_SETTINGS` 路径模板 | 详见 §2.2 注 |

**不新建**：
- ❌ 独立 `CardSetting.jsx` 组件（用户决策：沿用 BasicInfo 局部 render 函数模式）
- ❌ 独立 `useCardSetting.js` hook（state 内联到 BasicInfo）
- ❌ 独立 `CardSetting.m.less` 样式文件（沿用 BasicInfo.m.less）
- ❌ 独立 `card.service.js` 服务文件（并入 BasicInfo.thunk.js）
- ❌ 本地表、entity、mapper（卡片设置数据不存本地）
- ❌ `TenantContextHolder` / `TenantInterceptor`（工具类已存在）

---

## 5. Test Cases

### 5.1 前端

| ID | 场景 | 步骤 | 预期 |
|----|------|------|------|
| TC-F01a | 正常加载（值在范围内） | 打开 `/basic-info?appId=xxx` → 滚动到页面末尾第 4 个 Card；GET 返回 `{expirationDays:3, deletionDays:7}` | 只读态"卡片设置" Card 显示：失效 `3 天 [❓]`、删除 `7 天 [❓]`；Card `extra` 区有"编辑"按钮 |
| TC-F01b | 正常加载（值超出可写范围） | GET 返回 `{expirationDays:14, deletionDays:7}` | 只读态失效时间显示 `14 天`（展示实际值，不裁剪） |
| TC-F01c | 正常加载（值为 null） | GET 返回 `{expirationDays:null, deletionDays:null}` | 只读态显示 `— 天` |
| TC-F02 | Tooltip 展示 | hover ❓ 图标 | 显示 §2.1.3 完整业务说明文案 |
| TC-F03 | 进入整体编辑态 + 值裁剪（v > max） | 只读态失效 14 天 → 点"编辑" | 两行同时进入编辑态；失效 InputNumber 初始显示 7（max），删除 InputNumber 初始显示 7（在范围内） |
| TC-F04 | 进入整体编辑态 + 值裁剪（null） | 只读态失效 `—` → 点"编辑" | 失效 InputNumber 清空，显示 placeholder |
| TC-F05 | 输入下限校验 | 编辑态失效时间输入 `0` | 自动裁剪为 `1`（antd InputNumber min 行为） |
| TC-F06 | 输入上限校验 | 编辑态失效时间输入 `10` | 自动裁剪为 `7` |
| TC-F07 | 步进按钮 | 点击 ▲ | 值 +1，不越 max |
| TC-F08 | 步进按钮 | 点击 ▼ | 值 -1，不低于 min |
| TC-F09 | 底部"取消"整体还原 | 修改两行值 → 点底部"取消" | 两行整体还原到修改前的展示值，切回只读态；Card `extra` 区重新显示"编辑"按钮 |
| TC-F10a | 单行保存成功（另一行保持编辑） | 两行都编辑 → 失效行点"保存" → PUT 200 | 失效行 `message.success('保存成功')` → 重新 GET → 失效行回只读（显示新值）；删除行保持编辑态；Card `extra` 区仍无"编辑"按钮；底部"取消"仍存在 |
| TC-F10b | 第二行保存成功 → 退出编辑态 | 续 TC-F10a → 删除行点"保存" → PUT 200 | 删除行 `message.success` → 重新 GET → 删除行回只读；**两行都只读 → 自动退出编辑态**；Card `extra` 区重新显示"编辑"按钮；底部 `card-footer` 消失 |
| TC-F11 | 单行保存失败 | 编辑态失效行点"保存" → PUT 502 | 失效行 `message.error('卡片服务暂时不可用')`；失效行保留编辑态（值供重试）；删除行不受影响 |
| TC-F12 | 未输入合法值禁用 | 编辑态但 InputNumber 为空或越界 | #ASSUMED 本行保存按钮 disabled |
| TC-F13 | Card 视觉一致性 | 目视检查"卡片设置" Card 与"基础信息" / "认证方式" Card 的外观 | `.info-card` 容器 + `marginBottom:16` + 标题字号 + `card-footer` 分隔线 完全一致 |

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
| OQ-1 | ~~应用详情 page 接入点~~ | ✅ 已解决（v0.6）：BasicInfo 内"认证方式" Card 下方（第 4 个 Card 位） | — | — |
| OQ-2 | ~~卡片服务接口定义~~ | ✅ 已解决 | — | — |
| OQ-3 | ~~tenantId/clientId 来源~~ | ✅ 已解决 | — | — |
| OQ-4 | 是否需要 AuditLog（`OperateEnum.QUERY_CARD_SETTING / UPDATE_CARD_PERIOD`）？ | 待解决 | Controller 注解 + OperateEnum | 架构师 |
| OQ-5 | 正式接口编号（替换 `#TBD-CS01~02`） | 待解决 | 文档对齐 | 架构师 |
| OQ-6 | ~~未修改时保存按钮是否禁用~~ | ✅ 已解决 | — | — |
| OQ-7 | ~~板块视觉样式~~ | ✅ 已解决（v0.6）：沿用 `.info-card` 既有样式 | — | — |
| OQ-8 | null 时只读态展示 `—` 还是 placeholder 文案？编辑态 placeholder 文案具体是什么？ | 待解决 | §2.1.2 / §3.2.1 的 #ASSUMED 落地 | 产品/设计 |
| OQ-9 | ~~两态 UI 设计稿确认~~ | ✅ 已解决（v0.6）：折中方案 A（整体编辑入口 + 每行独立保存） | — | — |
| OQ-10 | Tooltip 是否要同时展示数值范围提示（如 "（1~7 天）" 后缀）？ | 待解决 | §2.1.3 文案最终形态 | 产品/设计 |
| OQ-11 | ~~卡片服务修改接口路径~~ | ✅ 已解决 | — | — |
| OQ-12 | ~~tenantId 获取工具~~ | ✅ 已解决 | — | — |
| OQ-13 | ~~openplatform_app_p_t mapper~~ | ✅ 已解决（方案 A：新建 AppPropertyMapper） | — | — |
| OQ-14 | ~~StandardAppContextResolver TODO~~ | 已明确不纳入本 Spec | — | — |
| OQ-15 | ~~request.ts 拦截器双重提示~~ | ✅ 不再相关（v0.6：wecodesite 无拦截器，手动 `message.error`） | — | — |
| OQ-16 | 卡片服务**鉴权字段**（API Key 等）待卡片服务方确认 | 待解决 | §3.6 `card-service` 配置完整性 | 卡片服务方 |
| OQ-17 | 折中 A 的混合态视觉：一行只读 + 一行编辑时，Card 高度变化 / 布局抖动是否可接受？ | 待解决 | §2.1.2 混合态视觉细节 | 设计 |

---

## 7. 变更日志

| 版本 | 日期 | 变更 | 作者 |
|------|------|------|------|
| 0.1 | 2026-06-10 | 初稿 | Spec Agent |
| 0.2 | 2026-06-22 | UI 两态、默认值/裁剪规则、Tooltip 业务文案、间距约束 | Spec Agent + 用户澄清 |
| 0.3 | 2026-06-22 | 卡片服务接口明确；接口 3→2 合并；字段 `retentionDays`→`deletionDays`；新增 `tenantId`/`clientId` 来源；DTO 重构 | Spec Agent + 用户澄清 |
| 0.4 | 2026-06-22 | **源码验证**：8 个 Spec 锚点全部 ✅ 成立；发现 `TenantContext`/`AppPropertyMapper`/`StandardAppContextResolver` TODO 现状；决策 OQ-13 方案 A（新建 AppPropertyMapper）；明确 OQ-14 不在本 Spec 范围；修正 §2.1.4 错误提示避免双重 message | Spec Agent |
| 0.5 | 2026-06-22 | OQ-11 解决（修改接口路径与查询同 `/interactive/card/businesscenter/period/setting/v1`）；OQ-12 解决（tenantId 工具类已存在，TODO 人工二开对接）；§3.6 配置简化 | Spec Agent + 用户澄清 |
| 0.6 | 2026-06-24 | **前端迁移到 wecodesite**：(1) 承载页面从 open-web 改为 wecodesite（open-web 废弃）；(2) 新板块嵌入 BasicInfo 第 4 个 Card 位（"认证方式"下方）；(3) 组件形态改为 BasicInfo 局部 `renderCardSetting` 函数（不创建独立组件）；(4) **折中方案 A**：整体编辑入口（Card `extra` 区"编辑"按钮）+ 每行独立保存（每次 1 个 PUT）+ 底部整体"取消"；(5) 技术栈从 TS 改为 JS；(6) HTTP 客户端从 `request.ts` 改为 `fetchApi`；(7) 错误处理从"依赖全局拦截器"改为"手动 `message.error`"；(8) 后端 Spec 全部保留；(9) OQ-1/7/9/15 关闭，新增 OQ-16/17 | Spec Agent + 用户澄清 |
