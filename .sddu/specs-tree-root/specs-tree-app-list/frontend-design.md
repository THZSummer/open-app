# 开放平台应用管理 — 前端设计

> **Feature**: APP-MGMT-001 开放平台应用管理
> **版本**: v4.0（对齐 wecodesite 开发规范）
> **读者**: 前端开发（后端辅助）
> **对应规范**: spec.md / plan.md / demo-app-list.html
>
> ⚠️ **生成代码前必读** [`front/doc/开发规范文档.md`](../../../../../front/doc/开发规范文档.md)——包含 10 大章节 + 附录（项目结构 / 文件编写 / 命名 / 代码风格 / 错误处理 / 状态管理 / 复用等）。本前端设计文档只列高频速查（§ 13.5 编码规范速查），**完整规范以开发规范文档为准**。

---

## § 1  前端目录设计

### 1.1 目录树

> 基于 wecodesite 已有项目结构扩展。每个页面遵循开发规范标准结构：`index.jsx` + `route.js` + `thunk.js` + `constant.js` + `.m.less` + `components/`。

```
wecodesite/src/
├── pages/                           # 页面级组件（按 URL 路径）
│   ├── AppList/                     # §5 应用列表页
│   │   ├── index.jsx                # 入口组件
│   │   ├── route.js                 # 路由配置
│   │   ├── thunk.js                 # API接口（1.4, 1.5, 1.6, 1.1, 1.12）
│   │   ├── constant.js              # 常量/校验规则
│   │   ├── AppList.m.less           # 页面样式
│   │   └── components/             # 页面组件
│   │       ├── Hero.jsx             # 顶部 Hero 区
│   │       ├── Hero.m.less
│   │       └── EmptyState.jsx       # 无应用时展示（AppCard 复用公共 components/AppCard/，不在此处）
│   ├── BasicInfo/                   # §8 凭证与基础信息页
│   │   ├── index.jsx                # 入口组件（被 Layout 包裹）
│   │   ├── route.js                 # 路由配置
│   │   ├── thunk.js                 # API接口（1.3, 1.2, 1.7, 1.8, 1.9, 1.10, 1.11, 1.12）
│   │   ├── constant.js              # 常量/校验规则/凭证字段映射
│   │   ├── BasicInfo.m.less         # 页面样式
│   │   └── components/             # 页面组件
│   │       ├── CredentialCard.jsx   # 应用凭证卡片
│   │       ├── CredentialCard.m.less
│   │       ├── BasicInfoCard.jsx    # 基本信息卡片
│   │       ├── BasicInfoCard.m.less
│   │       ├── AuthMethodCard.jsx   # 认证方式卡片
│   │       ├── AuthMethodCard.m.less
│   │       ├── EamapBindCard.jsx    # 升级 EAMAP 卡片（仅存量个人应用）
│   │       ├── EamapBindCard.m.less
│   │       └── LegacyBindBanner.jsx # 存量个人应用顶部横幅
│   │   # AppHeader / SideMenu 移到公共组件 components/AppHeader/、components/SimpleSidebar/
│   ├── Members/                     # §9 成员管理页
│   │   ├── index.jsx                # 入口组件（被 Layout 包裹）
│   │   ├── route.js                 # 路由配置
│   │   ├── thunk.js                 # API接口（2.1-2.5, 1.11）
│   │   ├── constant.js              # 常量/表格列/校验规则
│   │   ├── Members.m.less           # 页面样式
│   │   └── components/             # 页面组件
│   │       ├── MemberTable.jsx      # 成员列表
│   │       ├── MemberTable.m.less
│   │       ├── AddMemberModal.jsx   # 添加成员弹窗
│   │       ├── AddMemberModal.m.less
│   │       └── TransferOwnerModal.jsx # 转移 Owner 弹窗
│   ├── Capabilities/                # §10 能力管理页
│   │   ├── index.jsx                # 入口组件（被 Layout 包裹）
│   │   ├── route.js                 # 路由配置
│   │   ├── thunk.js                 # API接口（3.1-3.3）
│   │   ├── constant.js              # 常量/能力类型映射
│   │   ├── Capabilities.m.less      # 页面样式
│   │   └── components/             # 页面组件
│   │       ├── AbilityCard.jsx      # 能力卡片
│   │       ├── AbilityCard.m.less
│   │       └── AbilityGrid.jsx      # 能力卡片网格
│   │   # AbilitySidebar 与 SimpleSidebar 合并，由公共 SimpleSidebar 接收 menuConfig 实现
│   └── VersionRelease/              # §11-12 版本管理 + 版本详情页
│       ├── index.jsx                # 入口组件（版本列表，被 Layout 包裹）
│       ├── route.js                 # 路由配置
│       ├── thunk.js                 # API接口（4.1-4.7）
│       ├── constant.js              # 常量/版本状态映射/表格列
│       ├── VersionRelease.m.less     # 页面样式
│       └── components/             # 页面组件
│           ├── VersionForm.jsx      # 创建/编辑版本表单
│           ├── VersionForm.m.less
│           ├── VersionDetail.jsx    # 版本详情
│           └── VersionDetail.m.less
├── components/                      # 公共组件（已有 + 新增标注）
│   ├── Layout/                      # ✅ 已有 — 全局布局（§4.7 详细描述）
│   ├── SimpleSidebar/               # ✅ 已有 — 侧边栏（复用于应用详情侧导航）
│   ├── AppCard/                     # ✅ 已有 — 应用卡片（需扩展字段）
│   ├── AppHeader/                   # 🆕 新增 §4.6.1 — 应用详情页顶部摘要（4 详情页共用）
│   ├── CreateAppModal/              # ✅ 已有 — 创建应用弹窗（需扩展字段）
│   ├── DeleteConfirmModal/          # ✅ 已有 — 删除确认弹窗
│   ├── BindEamapModal/              # ✅ 已有 — 绑定 EAMAP 弹窗
│   ├── BindEamapSelect/             # ✅ 已有 — EAMAP 下拉选择
│   ├── PageHeader/                  # ✅ 已有 — 页面头部
│   ├── PageList/                    # ✅ 已有 — 分页列表
│   ├── TopNav/                      # 🆕 新增 §4.1 顶部导航
│   ├── EmptyState/                  # 🆕 新增 §4.2 空状态
│   └── CardGrid/                    # 🆕 新增 §4.5 卡片网格
├── configs/
│   └── web.config.js                # ✅ 已有 — 追加 APP 模块 URL 配置（§1.4）
├── utils/
│   ├── common.js                    # ✅ 已有 — 追加公共渲染函数
│   ├── constants.js                 # ✅ 已有 — 追加 APP 相关枚举映射
│   ├── commonTableConfigs.jsx       # ✅ 已有 — 公共表格列配置
│   ├── cookie.js                    # ✅ 已有
│   ├── request.js                   # ✅ 已有
│   └── flowUtils.js                 # ✅ 已有
├── hooks/
│   ├── useAdminList.js              # ✅ 已有
│   └── useSubscriptionList.js       # ✅ 已有
└── styles/
    └── global.m.less                # ✅ 已有
```

### 1.2 命名规范

> 遵守 `front/doc/开发规范文档.md` 第四章。

| 类型 | 规范 | 示例 |
|------|------|------|
| 组件文件 | PascalCase + `.jsx` | `AppCard.jsx` |
| 工具/接口 | camelCase + `.js` | `thunk.js`、`constant.js` |
| 样式文件 | PascalCase + `.m.less` | `AppCard.m.less` |
| 常量 | 全大写下划线 | `STATUS_MAP`、`INIT_PAGECONFIG` |
| 布尔变量 | `is/has/can` 前缀 | `isEamapBound`、`isSubscribed` |
| 事件处理 | `handle + 动作` | `handleCreateApp`、`handleDelete` |
| API 函数 | `动词 + 名词` | `fetchAppList`、`createApp`、`updateApp` |
| 表格列函数 | `get + 名称 + Columns` | `getMemberColumns`、`getVersionColumns` |

### 1.3 模块依赖图

```
pages/  →  components/  →  (无依赖)
   ↓
thunk.js  →  configs/web.config.js
   ↓
constant.js  →  utils/constants.js
```

依赖方向：pages → thunk → API_CONFIG；constant → utils/constants。**禁止反向依赖**。

---

## § 2  总体结构

### 2.1 8 个用户旅程场景速查

| 场景 | URL | 关键操作 | 主要接口 | 页面目录 |
|------|-----|---------|---------|---------|
| A 浏览应用列表 | `/` | 翻页/进入详情 | 1.4 | `AppList/` |
| B 创建应用 | `/`（弹窗）| 选图标/填表/提交 | 1.5/1.6/1.12/1.1 | `AppList/` + 公共 `CreateAppModal` |
| C 进入应用详情 | `/basic-info?appId=xxx` | 入口校验/加载/菜单切换 | 1.11/1.3 | `BasicInfo/` |
| D 管理凭证与基础信息 | `/basic-info?appId=xxx` | 复制/编辑/升级EAMAP | 1.8/1.2/1.9/1.7/1.10 | `BasicInfo/` |
| E 管理成员 | `/members?appId=xxx` | 添加/删除/转移Owner | 2.1-2.5 | `Members/` |
| F 管理能力 | `/capabilities?appId=xxx` | 添加/配置能力 | 3.1-3.3 | `Capabilities/` |
| G 管理版本 | `/version-release?appId=xxx` | 创建/撤回/删除 | 4.1/4.2/4.5/4.6 | `VersionRelease/` |
| H 查看版本详情 | `/version-release?appId=xxx&versionId=xxx` | 编辑/发布/撤回 | 4.3/4.7/4.4 | `VersionRelease/` |

> **appId 传递方式**：所有详情页通过 URL 查询参数 `?appId=xxx` 传递，与 wecodesite 现有模式一致（`AppCard` 已使用 `navigate('/basic-info?appId=${app.id}')`）。

---

## § 3  路由结构

### 3.1 各页面 route.js

> 每个页面独立 `route.js`，导出 `ROUTE_PATH` + `ROUTE_CONFIG`，与 wecodesite 现有模式一致。

| 页面 | ROUTE_PATH | name |
|------|-----------|------|
| AppList | `/` | 应用列表 |
| BasicInfo | `/basic-info` | 凭证与基础信息 |
| Members | `/members` | 成员管理 |
| Capabilities | `/capabilities` | 应用能力 |
| VersionRelease | `/version-release` | 版本管理 |

### 3.2 路由守卫（页面内实现）

> 不使用独立 guards 文件。守卫逻辑在页面 `index.jsx` 的 `useEffect` 中实现。

| 守卫 | 实现位置 | 逻辑 | 不通过行为 |
|------|---------|------|----------|
| 成员校验 | `BasicInfo/index.jsx` | 调 1.11，`role` 为 null → 非成员 | 跳转回应用列表页 `/` |
| 业务应用校验 | `Members/index.jsx` `Capabilities/index.jsx` `VersionRelease/index.jsx` | 调 1.3，`appType !== 1` → 非业务应用 | 跳 `/basic-info?appId=xxx` |

---

## § 4  全局通用组件

### 4.1 TopNav（顶部导航）🆕

**位置**：全站固定在顶部。

**展示内容**（从左到右）：

| 元素 | 内容 | 交互 |
|------|------|------|
| Logo | 平台 Logo | 点击 → 跳首页 |
| 平台名 | "开放平台" | 静态 |
| 菜单（中间）| "首页 / 应用管理 / ..." | 路由跳转 |
| 开发文档 | 链接 | 跳外部系统 |
| 用户菜单（最右）| 头像 + 下拉 | 下拉：API 审核 / 退出登录 |

### 4.2 EmptyState 🆕

**位置**：列表为空时展示。

**Props**：`{ icon, title, description, actionText, onAction }`

**示例**：
- 应用列表无数据：`icon=空盒子`, `title="您还没有应用"`, `actionText="立即创建"`, `onAction=打开createModal`

### 4.3 Toast / Modal 容器

> 使用 antd `message` + `Modal`，与 wecodesite 现有模式一致。无需额外封装。

- **Toast**：`message.success(msg)` / `message.error(msg)` — 遵守 `result?.messageZh || result?.message || '固定文案'` 格式
- **Modal**：antd `<Modal>` — `destroyOnClose` + `centered`

### 4.4 Pagination

> 使用 antd `<Pagination>`，配合 `utils/constants.js` 中的 `INIT_PAGECONFIG`。

### 4.5 CardGrid 🆕

**Props**：`{ items, renderItem, columns, gap }`

**展示**：响应式卡片网格（默认 3 列，移动端 1 列）。

### 4.6 可复用的已有公共组件

| 组件 | 路径 | 用途 |
|------|------|------|
| `AppCard` | `components/AppCard/` | 应用卡片（需扩展字段：eamapBound、appType、currentUserRole、lastUpdateTime） |
| `AppHeader` | `components/AppHeader/` | **🆕 新增** 应用详情页顶部摘要（BasicInfo / Members / Capabilities / VersionRelease 4 详情页共用，按 appType + appSubType 决定摘要区内容） |
| `CreateAppModal` | `components/CreateAppModal/` | 创建应用弹窗（需扩展字段：descCn/descEn、图标上传） |
| `DeleteConfirmModal` | `components/DeleteConfirmModal/` | 删除确认弹窗（直接复用） |
| `BindEamapModal` | `components/BindEamapModal/` | 绑定 EAMAP 弹窗（直接复用） |
| `BindEamapSelect` | `components/BindEamapSelect/` | EAMAP 下拉选择（直接复用） |
| `PageHeader` | `components/PageHeader/` | 页面头部（直接复用） |
| `SimpleSidebar` | `components/SimpleSidebar/` | 侧边栏（**扩展为支持外部 menuConfig**，同时复用于应用详情侧导航和应用能力的子菜单导航） |
| `PageList` | `components/PageList/` | 分页列表（参考复用） |

#### 4.6.1 AppHeader（🆕 新增公共组件）

**位置**：`wecodesite/src/components/AppHeader/`

**应用场景**：BasicInfo / Members / Capabilities / VersionRelease **4 个应用详情页共用**（由 Layout 统一注入）。

**展示内容**（从左到右，按应用类型决定，**对齐 spec FR-020 展示矩阵**）：

| 应用类型 | HOME 图标 + 应用图标 | 应用名称 | 右侧区域 |
|---------|:-----------------:|---------|---------|
| **业务应用**（`appType=1`）| ✅ | ✅ | `已绑定: {eamapAppName} {eamapAppCode}`（如"已绑定: 工作流引擎 eamap_workflow_001"）|
| **存量个人应用**（`appType=0 && appSubType=0`）| ✅ | ✅ | 「绑定」按钮（点击 → 触发 FR-018 绑定 EAMAP 流程）|
| **普通个人应用**（`appType=0 && appSubType≠0`）| ✅ | ✅ | （不显示任何右侧内容）|

**Props 签名**：

```js
<AppHeader
  appId={appId}              // 必传：用于获取应用信息（调 1.3）
  onBindEamapClick={() => ...} // 可选：仅存量个人应用有效
/>
```

**内部行为**：
- 自动调 1.3 获取应用基本信息
- 根据 `appType` + `appSubType` 决定右侧区域展示
- 不调 1.11（入口校验由 Layout 统一处理，AppHeader 不重复）

---

## § 4.7  Layout 布局组件（全局布局）

**位置**：`wecodesite/src/components/Layout/`

**职责**：包裹所有页面，提供统一的全局布局（顶部导航 + 应用摘要 + 侧边栏 + 主内容区）。

### 4.7.1 内部结构（自上而下）

```
┌─────────────────────────────────────────────┐
│  TopNav（顶部导航，固定）                     │  §4.1 全局共用
├─────────────────────────────────────────────┤
│  AppHeader（应用摘要，4 详情页共用）          │  §4.6.1 公共组件
├──────────────────┬──────────────────────────┤
│                  │                          │
│  SimpleSidebar   │   主内容区（页面 children）│
│  （左侧导航）      │                          │
│  - 应用详情菜单   │   当前页面渲染内容         │
│  - 能力子菜单     │                          │
│  - ...           │                          │
│                  │                          │
└──────────────────┴──────────────────────────┘
```

### 4.7.2 应用场景（按页面决定是否使用 Layout）

| 页面 | 路由 | 是否用 Layout | 备注 |
|------|------|:------------:|------|
| AppList | `/` | ❌ **不用** | 首页只需 TopNav + 主内容区（无 AppHeader、无 Sidebar）|
| BasicInfo | `/basic-info?appId=xxx` | ✅ | TopNav + AppHeader + SimpleSidebar(4 项) + 主内容区 |
| Members | `/members?appId=xxx` | ✅ | TopNav + AppHeader + SimpleSidebar(4 项) + 主内容区 |
| Capabilities | `/capabilities?appId=xxx` | ✅ | TopNav + AppHeader + SimpleSidebar(4 项) + 主内容区 |
| VersionRelease | `/version-release?appId=xxx` | ✅ | TopNav + AppHeader + SimpleSidebar(4 项) + 主内容区 |

**AppList 不用 Layout 的原因**：
- 不需要应用摘要（应用列表页显示的是所有应用，不针对单个应用）
- 不需要侧边栏（应用列表页是首页，无子菜单）
- TopNav 通过 `AppList/index.jsx` 直接 import 即可

### 4.7.3 Props 签名

```js
<Layout
  appId={appId}                  // 必传（4 详情页）：从 URL ?appId=xxx 提取
  currentMenuKey="basic-info"    // 必传：决定 SimpleSidebar 高亮项
                                 // 可选值: 'basic-info' | 'members' | 'capabilities' | 'version-release'
  sidebarMenuConfig={[...]}      // 可选：覆盖 SimpleSidebar 默认菜单（用于 Capabilities 页面传入子菜单）
>
  {页面内容}
</Layout>
```

### 4.7.4 内部组件行为

**TopNav（§4.1）**：
- 固定在顶部，所有页面共用
- Layout 直接 import，包裹 children

**AppHeader（§4.6.1）**：
- 从 `appId` 自动调 1.3 获取应用信息
- 按 `appType` + `appSubType` 决定摘要区右侧内容
- 不做入口校验（由各页面 useEffect 调 1.11）

**SimpleSidebar（§4.6 扩展）**：
- 默认菜单（应用详情 4 项）：凭证与基础信息 / 成员管理 / 应用能力 / 版本管理
- 按 `appType` 决定显示项数：
  - 业务应用（`appType=1`）：4 项
  - 个人应用（`appType=0`）：1 项（仅凭证与基础信息）
- 点击菜单项时 `navigate('/xxx?appId=xxx')`，保持 `appId` 查询参数
- Capabilities 页面传入 `sidebarMenuConfig` 覆盖默认菜单，显示能力的子菜单（添加应用能力 + 已订阅能力列表）

**主内容区**：
- 渲染 `children`（即各页面的 `index.jsx` 实际内容）
- 4 详情页共用同一主内容区布局（不同页面渲染不同组件）

---

## § 5  用户旅程 A：浏览应用列表（AppList）

### 5.1 入口与数据加载

**入口**：顶部全局导航的"应用管理"菜单 → 跳 `/`

**首次加载**：
- 调用 **接口 1.4** 获取应用列表（默认 `curPage=1&pageSize=10`）

### 5.2 展示内容

**布局**（从上到下）：

| 区域 | 内容 |
|------|------|
| TopNav（顶部导航）| 全局共用（§4.1）— AppList 页面**不包裹 Layout**，直接 import TopNav |
| Hero 区 | 标题"应用管理" + 副标题"管理您创建的 WeLink 应用" + 右侧"立即创建"按钮 |
| 卡片网格 | 应用卡片（3 列响应式，AppCard 来自公共 `components/AppCard/`，**不在 AppList/components/ 中重复**）|
| 分页器 | 底部居中 |
| 空状态 | 无应用时展示 EmptyState |

**应用卡片元素**：

| 元素 | 数据源（1.4 出参）| 视觉 |
|------|---------------|------|
| 图标 | `icon.url` | 64×64 圆角方形 |
| 中文名 | `nameCn` | 16px 600 |
| EAMAP 状态 | `eamapBound` | 绿色"已绑定" / 橙色"未绑定" 徽标 |
| 所有者 | `owner.chineseName` + `owner.w3Account` | 姓名 + 工号（**相同颜色**）|
| 我的角色 | `currentUserRole` | Owner/Admin/Developer 彩色徽标 |
| 最后更新 | `lastUpdateTime` | `yyyy-MM-dd HH:mm:ss` 格式 |

### 5.3 按钮与交互

| 按钮 | 位置 | 交互 |
|------|------|------|
| 立即创建 | Hero 右上 | 点击 → 打开 § 6 CreateAppModal |
| 进入详情 | 卡片整体可点击 | 点击 → 跳 `/basic-info?appId={appId}` |
| 翻页 | 分页器 | 点击 → 重新调 1.4（带 `curPage`）|
| 每页大小 | 分页器 | 切换 → 重新调 1.4（带 `pageSize`）|

### 5.4 后端接口

| 接口 | 用途 |
|------|------|
| 1.4 `GET /service/open/v2/app?curPage&pageSize` | 获取应用列表（单次返回，含每项的 `currentUserRole`）|

**接口 1.4 关键响应字段**（每项 `AppVO`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `appId` | `string` | 应用 ID（隐藏，用于跳转）|
| `nameCn` | `string` | 中文名（卡片标题）|
| `nameEn` | `string` | **接口返回,但卡片不展示** |
| `icon` | `FileVO` | 图标（用 `url`）|
| `appType` | `int` | 0=个人 / 1=业务（**接口返回,但卡片不展示**）|
| `appSubType` | `int` | 子类型（**接口返回,但卡片不展示**）|
| `eamapBound` | `boolean` | EAMAP 状态 |
| `owner` | `EmployeeInfoVO` | 所有者（展示 `chineseName` + `w3Account`）|
| `currentUserRole` | `int` | 我的角色 |
| `lastUpdateTime` | `string` | 最后更新 |

### 5.5 thunk.js 设计

> 遵守开发规范：每个函数用 try-catch 包裹 fetchApi，引用 API_CONFIG，正常返回 `result || {}`，异常返回 `{}`。

| 函数名 | 接口 | API_CONFIG 键 | 方法 | 参数 |
|--------|------|--------------|------|------|
| `fetchAppList` | 1.4 | `APP.LIST` | GET | `params`（curPage, pageSize）|
| `fetchDefaultIcons` | 1.6 | `APP.ICONS` | GET | — |
| `fetchEamapOptions` | 1.5 | `APP.EAMAP_LIST` | GET | `params`（curPage, pageSize）|
| `createApp` | 1.1 | `APP.CREATE` | POST | `data`（表单数据）|
| `uploadFile` | 1.12 | `APP.FILE_UPLOAD` | POST | `bizType, data`（文件数据）|

### 5.6 constant.js 设计

| 常量名 | 类型 | 说明 |
|--------|------|------|
| `DEFAULT_SEARCH_VALUES` | Object | 搜索默认值（预留） |
| `ROLE_MAP` | Object | 角色映射：0=开发者 / 1=Owner / 2=管理员 |
| `APP_TYPE_MAP` | Object | 应用类型映射：0=个人应用 / 1=业务应用 |
| `FORM_VALIDATION_RULES` | Object | 创建应用表单校验规则（nameCn/nameEn 必填 1-255 字符, eamapAppCode 必填）|

> 分页初始配置直接引用 `utils/constants.js` 的 `INIT_PAGECONFIG`，不重复导出。

### 5.7 错误处理

| 错误 | 展示 | 恢复 |
|------|------|------|
| 列表加载失败 | 卡片网格区显示"加载失败，点击重试" | 点击 → 重调 1.4 |
| 无应用数据 | EmptyState："您还没有应用，点击立即创建" | 点击 → 打开 CreateAppModal |
| 未登录（401）| 跳登录页 | — |

### 5.8 限制

- 单页最大 50 条

---

## § 6  用户旅程 B：创建应用（CreateAppModal）

### 6.1 入口

从 § 5 Hero 右上角点击"立即创建"按钮 → 弹出 Modal（无独立 URL）。

> 复用公共组件 `components/CreateAppModal/CreateAppModal.jsx`，需扩展字段。

### 6.2 展示内容

**Modal 标题**："创建应用"

**表单字段**（6 个）：

| 字段 | 类型 | 必填 | 校验 |
|------|------|:---:|------|
| 应用图标 | 2 选 1（默认图标列表 / 自定义上传）| ✅ | 类型 png/jpg/jpeg + 尺寸 128×128px + ≤100KB |
| 中文名 | input | ✅ | 1-255 字符,**达到上限后前端阻止继续输入（不弹错）** |
| 英文名 | input | ✅ | 1-255 字符,**达到上限后前端阻止继续输入（不弹错）** |
| 描述（中）| textarea | ❌ | 0-2000 字符,**达到上限后前端阻止继续输入（不弹错）** |
| 描述（英）| textarea | ❌ | 0-2000 字符,**达到上限后前端阻止继续输入（不弹错）** |
| EAMAP | select（下拉）| ✅ | 从 1.5 接口获取的列表选，**字段旁有 ? 帮助图标，鼠标悬浮提示文案**：`开放平台按照应用服务维度做权限隔离，如需申请API权限等开放能力需要先绑定应用服务, 如无权限请前往应用中心查询对应责任人` |

### 6.3 按钮与交互（8 步流程）

| 步骤 | 用户操作 | 系统行为 |
|---|---------|---------|
| 1 | 点击"立即创建" | 调 1.6 获取默认图标列表 |
| 2 | 选择图标方式 | 显示对应 UI（**默认图标列表网格 / 自定义上传按钮**）|
| 3 | 上传自定义图标 | 3a **前端校验**（选文件时立即执行）：<br>- 文件类型：仅 png/jpg/jpeg<br>- **图片尺寸：128×128px**<br>- 文件大小：≤100KB<br>- 校验失败 → Toast 提示 + 不调接口<br>3b 校验通过后 → 调 1.12（`bizType=1`）<br>3c 拿到 `fileId` + `url` 后回填 |
| 3.1 | 聚焦 EAMAP 字段 | **显示 ? 帮助图标的悬浮提示**（文案见 6.2 表 EAMAP 行）|
| 4 | 输入名称/描述 | 实时校验（onChange）：<br>- nameCn 必填,1-255 字符<br>- nameEn 必填,1-255 字符<br>- descCn/descEn 可选,0-2000 字符 |
| 5 | 展开 EAMAP 下拉 | 调 1.5 获取 EAMAP 列表 → 渲染下拉 |
| 6 | 选择 EAMAP | 记录 `eamapAppCode` |
| 7 | 点击"创建" | 调 1.1 创建应用 |
| 8 | 成功 | 关闭 Modal + 跳 `/basic-info?appId={newAppId}`（新应用详情）|

**其他按钮**：
- 取消：关闭 Modal
- 关闭 X：关闭 Modal

### 6.4 后端接口

| 接口 | 用途 | 调用时机 |
|------|------|---------|
| 1.6 `GET /service/open/v2/app/icons` | 获取默认图标列表 | 打开 Modal 时 |
| 1.5 `GET /service/open/v2/app/eamap?curPage&pageSize` | 获取 EAMAP 列表（下拉）| 展开 EAMAP 下拉时 |
| 1.12 `POST /service/open/v2/file/upload?bizType=1` | 上传图片（图标）| 用户选"上传自定义"时 |
| 1.1 `POST /service/open/v2/app` | 创建应用 | 提交时 |

> 这些接口在 `AppList/thunk.js` 中定义（CreateAppModal 是 AppList 页面的子组件）。

### 6.5 错误处理

| 错误 | 展示 | 恢复 |
|------|------|------|
| 1.6 失败 | 预设图标网格区显示"加载失败" | 点击重试 |
| 1.5 失败 | EAMAP 下拉显示"加载失败" | 点击重试 |
| 1.12 失败 | Toast "上传失败" | 重新选文件 |
| 1.1 失败 400103（EAMAP 编码不存在）| 表单下方红字 | 重新选 EAMAP |
| 1.1 失败 403104（当前用户不是 EAMAP 的 owner）| 表单下方红字 | 切换 EAMAP 或退出 |
| 1.1 失败 409102（EAMAP 已被其他应用绑定）| 表单下方红字 | 切换 EAMAP |
| 1.1 失败 409100（应用名重复）| 中文名下方红字 | 改名 |

### 6.6 限制

- **应用名 1-255 字符**（不限字符集）
- **应用描述 0-2000 字符**
- **图标 ≤ 100KB,类型 png/jpg/jpeg**（`bizType=1`）
- 一次只能选 1 个 EAMAP（不能多选）

---

## § 7  用户旅程 C：进入应用详情（BasicInfo 入口页）

### 7.0 布局说明

**本页是 4 个应用详情页（BasicInfo / Members / Capabilities / VersionRelease）的入口**。页面结构使用公共 `Layout` 组件（§4.7）包裹：

```
<Layout appId={appId} currentMenuKey="basic-info">
  {BasicInfo 主内容}
</Layout>
```

Layout 自动渲染：TopNav + AppHeader（应用摘要，按 appType 决定）+ SimpleSidebar（4 项菜单，按 appType 决定显隐）+ 主内容区。**本页面 useEffect 调 1.11 做入口校验，校验通过后再调 1.3 获取应用信息**（用于 AppHeader 和 SimpleSidebar 内部渲染）。

### 7.1 入口

- **主入口**：从 § 5 卡片点击 → 跳 `/basic-info?appId=xxx`
- **次入口**：直接 URL 访问（分享链接/书签）→ 需走 7.2 入口校验

> BasicInfo 是应用详情的**默认入口页**，承载入口校验（1.11）和应用头部摘要（1.3）。其他详情页（Members、Capabilities、VersionRelease）独立路由，各自调用必要的接口。

### 7.2 数据加载（顺序执行）

| 步骤 | 调用 | 用途 | 失败处理 |
|---|------|------|---------|
| 1 | **1.11** | 入口校验：`role` 是否 null | `role=null` → 跳转回应用列表页 `/`（**不调 1.3**）|
| 2 | **1.3** | 获取应用基本信息（含 `appType` + `appSubType`）| 跳错误页 + 重试按钮 |

**关键原则**：
- 1.11 **先调**（入口校验优先）
- 1.11 通过后**再调** 1.3（获取头部摘要）
- 1.3 返回的 `appType` + `appSubType` 决定左侧菜单（见 7.3）

### 7.3 展示内容

**布局**（从上到下）：

| 区域 | 内容 |
|------|------|
| 顶部摘要 | HOME 图标 + **按应用类型决定**（见下表）|
| 左侧菜单 | 按 appType 决定（见下表）|
| 主内容区 | 当前激活菜单对应页面 |

**HOME 图标右边展示（按应用类型，对齐 FR-020 展示矩阵）**：

| 应用类型 | 展示内容 |
|---------|---------|
| **业务应用**（`appType=1`）| 应用名称 + `已绑定: {eamapAppName} {eamapAppCode}` |
| **存量个人应用**（`appType=0 && appSubType=0`）| 应用名称 + 「绑定」按钮 |
| **普通个人应用**（`appType=0 && appSubType≠0`）| **只展示应用名称** |

**左侧菜单（按应用类型决定）**：

| 应用类型 | 显示菜单 | 对应路由 |
|---------|---------|---------|
| **业务应用**（`appType=1`）| ① 凭证与基础信息 ② 成员管理 ③ 应用能力 ④ 版本管理 | `/basic-info` `/members` `/capabilities` `/version-release` |
| **个人应用**（`appType=0`）| ① 凭证与基础信息 | `/basic-info` |
| **存量个人应用**（`appType=0 && appSubType=0`）| ① 凭证与基础信息 | `/basic-info` |

**菜单显隐原则**：
- **基于 appType**（不基于角色）
- 默认激活菜单：凭证与基础信息

> **SimpleSidebar 实现方式**：由公共 `components/SimpleSidebar/`（§4.6）承载，按 1.3 返回的 `appType` 动态生成菜单项，点击菜单项时 `navigate('/members?appId=xxx')`，保持 `appId` 查询参数。BasicInfo / Members / Capabilities / VersionRelease 4 详情页都通过公共 Layout 组件（§4.7）使用同一 SimpleSidebar。

### 7.4 按钮与交互

| 按钮 | 交互 |
|------|------|
| 返回 | 跳 `/`（保留分页状态）|
| 切换菜单 | 跳对应路由（带 `?appId=xxx`）|
| 顶部"立即绑定"（仅存量个人应用）| 跳 EamapBindCard + 高亮提示 |

### 7.5 后端接口

| 接口 | 用途 |
|------|------|
| 1.11 `GET /service/open/v2/app/{appId}/current-role` | 入口校验 + 成员管理页按钮级显隐 |
| 1.3 `GET /service/open/v2/app/{appId}` | 获取应用基本信息（含 `appType` + `appSubType`）|

**接口 1.11 作用范围（仅 2 处用）**：

| 位置 | 用法 |
|------|------|
| ① BasicInfo 入口校验 | `role=null` → 跳转回应用列表页 `/` |
| ② Members 页内按钮级显隐 | 添加/删除/转移 Owner 按钮（1.11 返回最高权限角色，多角色时后端已取最高） |

**1.11 不影响**：
- 菜单显隐（基于 appType）
- 其他页内按钮（凭证/能力/版本 不基于角色）
- 列表页/创建应用 等操作

**接口 1.3 关键响应字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `appId` | `string` | 应用 ID |
| `nameCn` | `string` | 中文名 |
| `nameEn` | `string` | 英文名 |
| `icon` | `FileVO` | 图标 |
| `appType` | `int` | 0=个人 / 1=业务（**决定菜单**）|
| `appSubType` | `int` | 0=存量个人 / 1-3=其他 / 4=业务-标准（**决定是否显示绑定按钮**）|
| `eamapAppCode` | `string` | EAMAP 编码 |
| `eamapAppName` | `string` | EAMAP 名称（用于 AppHeader 展示 `已绑定: {eamapAppName} {eamapAppCode}`）|
| `eamapBound` | `boolean` | EAMAP 状态 |
| `diagramIdList` | `FileVO[]` | 功能示意图列表 |

### 7.6 错误处理

| 错误 | 展示 | 恢复 |
|------|------|------|
| 1.11 返回 `role=null` | 跳转回应用列表页 `/`（"应用不存在或您无权限访问"）| — |
| 1.3 失败 | 跳错误页 + "重试"按钮 | 重调 1.3 |
| 应用不存在（404100）| 跳转回应用列表页 `/` | — |

---

## § 8  用户旅程 D：管理凭证与基础信息（BasicInfo）

### 8.0 布局说明

**本页是 4 详情页之一**，使用公共 `Layout` 组件（§4.7）包裹：

```
<Layout appId={appId} currentMenuKey="basic-info">
  {BasicInfo 主内容（4 卡片）}
</Layout>
```

**主内容区**（4 卡片，本页面独有）：

| 卡片 | 组件 | 复用状态 |
|------|------|---------|
| 应用凭证 | `CredentialCard` | BasicInfo 私有 |
| 基本信息 | `BasicInfoCard` | BasicInfo 私有 |
| 认证方式 | `AuthMethodCard` | BasicInfo 私有 |
| 升级 EAMAP（仅存量个人应用）| `EamapBindCard` | BasicInfo 私有 |

> 顶部"立即绑定"按钮、HOME 图标、应用名称、EAMAP 状态展示统一在 `AppHeader`（§4.6.1）中，本页面不重复。

### 8.1 入口

从 § 7 左侧菜单或直接 URL `/basic-info?appId=xxx`。

### 8.2 展示内容（4 卡片）

| 卡片 | 展示字段 | 字段来源（1.3 出参）|
|------|---------|------------------|
| **应用凭证** | AppID、AppSecret、复制按钮 | `appId`（隐藏 ID 字段）|
| **基本信息** | 中文名、英文名、描述（中）、描述（英）、图标、编辑按钮 | `nameCn`/`nameEn`/`descCn`/`descEn`/`icon` |
| **认证方式** | 当前认证方式（**多选**：Cookie/数字签名/SOAHeader/SOAURL/**APIG**）、配置按钮 | 调 1.9 获取 |
| **升级 EAMAP**（仅存量个人应用）| 立即绑定按钮 | `appSubType=0` |

### 8.3 按钮与交互

#### 8.3.1 复制凭证

| 步骤 | 行为 |
|---|------|
| 1 | 点击 AppID 旁的"复制"图标 |
| 2 | 调 1.8 获取完整凭证（**后端按权限脱敏**）|
| 3 | 写入剪贴板 + Toast "已复制" |
| 4 | 显示完整 AppSecret（明文，仅复制时短暂显示）|

#### 8.3.2 编辑基本信息

**可编辑字段**：

| 字段 | 校验规则 |
|------|---------|
| `nameCn` | 必填,1-255 字符,**达到上限后前端阻止继续输入（不弹错）** |
| `nameEn` | 必填,1-255 字符,**达到上限后前端阻止继续输入（不弹错）** |
| `descCn` | 可选,0-2000 字符,**达到上限后前端阻止继续输入（不弹错）** |
| `descEn` | 可选,0-2000 字符,**达到上限后前端阻止继续输入（不弹错）** |
| `iconId`（图标）| 调 1.12 上传,**前端校验**：png/jpg/jpeg + ≤100KB |
| `diagramIdList`（功能示意图列表）| 调 1.12 上传,**前端校验**：png/jpg/jpeg + **360×200px** + ≤500KB |

| 步骤 | 行为 |
|---|------|
| 1 | 点击"编辑" |
| 2 | 卡片切换为编辑态（input/textarea/图片上传组件）|
| 3 | 输入名称/描述 → 实时校验字符数 |
| 4 | 改图标：点击"更换图标" → 选文件 → **前端校验**（类型/大小）→ 调 1.12 上传 → 回填 `iconId` |
| 5 | 改示意图：点击"添加示意图" → 选文件 → **前端校验**（类型/大小/尺寸）→ 调 1.12 上传 → 追加到 `diagramIdList`<br>**功能示意图字段旁有「查看示例」入口，点击展示示例图片** |
| 5.1 | 上传图片下方 | **推荐 2 倍图 720×400px**（仅作提示，不作校验）|
| 6 | 点击"保存"→ 调 1.2 更新应用 |
| 7 | 成功 → 卡片切回展示态 + Toast |
| 8 | 失败 → 字段下方红字 |

#### 8.3.3 配置认证方式

| 认证方式 | `verifyType` | 是否需要 `apiSecret` |
|---------|:-----------:|:-------------------:|
| Cookie | 0 | ❌ |
| SOAHeader | 1 | ❌ |
| 数字签名 | 2 | ✅ |
| SOAURL | 3 | ❌ |
| **APIG** | **4** | ❌ |

| 步骤 | 行为 |
|---|------|
| 1 | 点击"配置" → 弹出认证方式列表（**5 选 N，多选**，如 `[0, 2]` = Cookie+数字签名），**标题下方显示红色警告**：`认证方式切换后,将影响已发送卡片的数据回调,请谨慎选择。` |
| 2 | **SOAHeader(1) 与 SOAURL(3) 互斥，选中其中一个时，另一个自动取消选中** |
| 3 | 选中"数字签名"时，展开 `apiSecret` 输入框（必填） |
| 4 | **前端校验** `apiSecret`：16 位，**必须同时包含字母和数字** |
| 5 | **所有**认证方式都不包含"数字签名"时，`apiSecret` 输入框隐藏 |
| 6 | 调 1.7 更新认证方式（`verifyType` 传数组）|
| 7 | 成功 → 卡片更新 + Toast |
| 8 | 失败 → 400106 错误时提示"AppSecret 格式错误" |

#### 8.3.4 个人应用升级 EAMAP

**显示条件**：仅存量个人应用（`appType=0 && appSubType=0`）

**入口**：
- 主入口：tab-basic 内的"升级 EAMAP"卡片
- 快捷入口：§ 7.3 顶部"立即绑定"按钮（点击 → 滚动到本卡片 + 高亮提示 3 秒）

| 步骤 | 行为 |
|---|------|
| 1 | 点击"立即绑定" |
| 2 | 弹出绑定 Modal（含 EAMAP 下拉）— **复用 `components/BindEamapModal/`** |
| 3 | 调 1.5 获取 EAMAP 列表 → 渲染下拉 |
| 4 | 选择 EAMAP |
| 5 | 点击"绑定"→ 调 1.10 绑定 EAMAP |
| 6 | 成功 → 关闭 Modal + 刷新当前页（应用已升级为业务应用）|
| 7 | 失败 → 错误码处理（见 8.5）|

#### 8.3.5 EAMAP管理（FR-018 + FR-020）

> **依据**：spec.md §2.7「EAMAP管理」。本节汇总 EAMAP 相关的前端展示规则，后端接口见 1.3（`eamapAppName` 出参）、1.10（绑定 EAMAP）。

**业务规则**：

| 规则 | 说明 |
|------|------|
| 业务应用必绑 EAMAP | 创建时即绑定，绑定后不可更换 |
| 存量个人应用可升级 | 通过「绑定」按钮（AppHeader 或 EamapBindCard）触发 FR-018 绑定 EAMAP 升级为业务应用 |
| 一码一应用 | 同一 EAMAP 编码只能被一个应用绑定 |
| 升级不可逆 | 绑定后 `app_type` 从 0→1，不可降级 |
| 升级后菜单扩展 | 刷新页面即可看到全部 4 个 Tab 菜单 |

**FR-020 EAMAP 信息展示矩阵**（AppHeader，对齐 spec FR-020）：

| 应用类型 | HOME 按钮旁展示 | 是否展示「绑定」按钮 |
|----------|----------------|:-------------------:|
| 业务应用（`appType=1`） | `应用名称` + `已绑定: {eamapAppName} {eamapAppCode}` | ❌ |
| 存量个人应用（`appType=0 && appSubType=0`） | `应用名称` | ✅（触发 FR-018 绑定流程）|
| 其他个人应用（`appType=0 && appSubType≠0`） | `应用名称` | ❌ |

> `eamapAppName` 来自接口 1.3 出参；`eamapAppCode` 同样来自 1.3。业务应用**必须同时展示 EAMAP 名称 + EAMAP 编码**。

### 8.4 后端接口

| 接口 | 用途 | 调用时机 |
|------|------|---------|
| 1.8 `GET /service/open/v2/app/{appId}/identity` | 获取应用凭证 | 复制凭证 / 查看凭证 |
| 1.2 `PUT /service/open/v2/app/{appId}` | 更新应用 | 编辑基本信息 |
| 1.9 `GET /service/open/v2/app/{appId}/verify-type` | 获取认证方式 | 激活 BasicInfo |
| 1.7 `PUT /service/open/v2/app/{appId}/verify-type` | 更新认证方式 | 配置认证方式 |
| 1.5 `GET /service/open/v2/app/eamap?curPage&pageSize` | 获取 EAMAP 列表 | 打开升级 EAMAP Modal |
| 1.10 `POST /service/open/v2/app/{appId}/bind-eamap` | 绑定 EAMAP（升级）| 提交绑定时 |

**接口 1.8 出参关键字段**（后端返回 `ak` / `sk`，前端展示为 APP Key / APP Secret）：

| 后端字段 | 前端展示名 | 类型 | 说明 |
|------|------|------|------|
| `ak` | APP Key | `string` | 前端标签"APP Key"，值来自 1.8 的 `ak` |
| `sk` | APP Secret | `string` | 前端标签"APP Secret"，值来自 1.8 的 `sk`（**前端需谨慎展示，建议 mask 后再揭示**）|

> APPID 不由 1.8 返回，直接取 1.3 响应中的 `appId` 字段展示。

### 8.5 thunk.js 设计

> BasicInfo 页面承载最多接口（8 个）。遵守开发规范：每个函数用 try-catch 包裹 fetchApi，引用 API_CONFIG，正常返回 `result || {}`，异常返回 `{}`。

| 函数名 | 接口 | API_CONFIG 键 | 方法 | 参数 | 分组 |
|--------|------|--------------|------|------|------|
| `fetchCurrentRole` | 1.11 | `APP.CURRENT_ROLE` | GET | `appId` | 入口校验 |
| `fetchAppDetail` | 1.3 | `APP.DETAIL` | GET | `appId` | 应用信息 |
| `updateApp` | 1.2 | `APP.UPDATE` | PUT | `appId, data` | 应用信息 |
| `fetchAppIdentity` | 1.8 | `APP.IDENTITY` | GET | `appId` | 凭证与认证 |
| `fetchVerifyType` | 1.9 | `APP.VERIFY_TYPE_GET` | GET | `appId` | 凭证与认证 |
| `updateVerifyType` | 1.7 | `APP.VERIFY_TYPE_UPDATE` | PUT | `appId, data` | 凭证与认证 |
| `fetchEamapList` | 1.5 | `APP.EAMAP_LIST` | GET | `params` | EAMAP 绑定 |
| `bindEamap` | 1.10 | `APP.BIND_EAMAP` | POST | `appId, data` | EAMAP 绑定 |
| `uploadFile` | 1.12 | `APP.FILE_UPLOAD` | POST | `bizType, data` | 文件上传 |

### 8.6 constant.js 设计

| 常量名 | 类型 | 说明 |
|--------|------|------|
| `VERIFY_TYPE_MAP` | Object | 认证方式映射（**多选**）：0=Cookie / 1=SOAHeader / 2=数字签名(needApiSecret=true) / 3=SOAURL / **4=APIG(needApiSecret=false)** |
| `FORM_VALIDATION_RULES` | Object | 表单校验规则：nameCn/nameEn 必填 1-255, descCn/descEn 0-2000, **apiSecret 列表中包含 2（数字签名）时必填（16 位,必须同时包含字母和数字）**, eamapAppCode 必填, **verifyType 至少选 1 项** |
| `MODAL_TITLE_BIND_EAMAP` | String | '绑定 EAMAP' |

### 8.7 错误处理

| 错误 | 展示 | 恢复 |
|------|------|------|
| 1.2 失败 400101（参数非法）| 字段下方红字 | 修改后重提 |
| 1.2 失败 409100（应用名重复）| 中文名下方红字 | 改名 |
| 1.7 失败 404100（应用不存在）| Toast + 跳列表 | — |
| 1.10 失败 400103（EAMAP 编码不存在）| Modal 内红字 | 重选 EAMAP |
| 1.10 失败 403104（当前用户不是 EAMAP 的 owner）| Modal 内红字 | 切换 EAMAP 或退出 |
| 1.10 失败 409102（EAMAP 已被其他应用绑定）| Modal 内红字 | 切换 EAMAP |
| 1.10 失败 409103（应用类型不支持，业务应用误调）| Modal 内红字 | 业务应用不应调此接口 |

### 8.8 限制

- 业务应用升级 EAMAP：**不可降级**回个人应用
- AppSecret 仅在"复制凭证"操作时**短时显示**（如 30 秒后自动 mask）
- 应用名重复校验在租户内

---

## § 9  用户旅程 E：管理成员（Members）

### 9.0 布局说明

**本页是 4 详情页之一**，使用公共 `Layout` 组件（§4.7）包裹：

```
<Layout appId={appId} currentMenuKey="members">
  {Members 主内容（MemberTable + 操作按钮）}
</Layout>
```

**主内容区**（Members 页面独有）：

| 组件 | 复用状态 |
|------|---------|
| `MemberTable` | Members 私有 |
| `AddMemberModal` | Members 私有 |
| `TransferOwnerModal` | Members 私有 |

> 页面级 useEffect 守卫：调 1.3，校验 `appType === 1`（业务应用），非业务应用跳 `/basic-info?appId=xxx`。

### 9.1 入口

从 § 7 左侧菜单跳 `/members?appId=xxx`（**仅业务应用**，个人应用无此菜单）。

### 9.2 展示内容（成员列表）

**列表元素**：

| 列 | 数据 | 视觉 |
|---|------|------|
| 姓名 | `chineseName` | 主文本 |
| 工号 | `w3Account` | 正常显示 |
| 角色 | `roleName` | Owner/管理员/开发者 彩色徽标 |
| 操作 | **按行角色显隐**：<br>- 普通成员行：删除按钮（仅 Owner/Admin 可见）<br>- Owner 行：**只展示"转移"按钮**（让当前 Owner 把权限转给该成员）| 删除图标 / 转移图标 |

**特殊标记**：
- 当前用户自己：行右侧加"我"标签
- Owner：行右侧加"Owner"金色徽标

**多角色共存规则**（spec v2.2）：
- **同一成员可在多个角色下同时存在**（如张三可同时出现在 Owner 行 / 管理员 行 / 开发者 行，列表中显示为多行）
- **同一角色下不能有相同成员**（如 Owner 角色下不能有两个张三）
- 删除操作按成员记录主键 `id` 删除单条记录，不影响该成员的其他角色记录

**分页**（对齐 spec FR-006）：

| 项目 | 说明 |
|------|------|
| 每页条数 | 可选 10 / 20 / 50，**默认 20** |
| 分页信息 | 左侧显示总条数，右侧显示分页控件和条数选择器 |
| 跳转到某页 | 支持**跳转到某页**（输入页码直跳） |
| 排序 | 按成员姓名拼音字母顺序 A-Z 升序；姓名相同时按 id 倒序 |
| 分页组件 | 使用 antd `<Pagination>`，配合 `utils/constants.js` 的 `INIT_PAGECONFIG` |

### 9.3 按钮与交互

#### 9.3.1 添加成员 → AddMemberModal

**Modal 标题**："添加成员"

**展示内容**：

| 元素 | 内容 |
|------|------|
| 成员选择器 | 共用「成员选择器规范」（见下方），下拉项展示**姓名/工号/部门**三字段，选中后输入框组合展示 |
| 角色选择 | **单选下拉（按当前用户角色过滤选项 + 智能默认）**<br>- Owner：可选"开发者"或"管理员",**默认"管理员"**<br>- 管理员：仅"开发者",**默认"开发者"**<br>**角色为必选**，弹窗打开时即有默认值，不存在"未选"状态 |
| 角色权限说明面板 | 位于「角色」下拉框下方，**内容根据所选角色动态切换**（文案详见下方「角色权限文案」表）|
| 确认按钮 | "添加"（禁用条件：已选为空）|

**角色权限文案**（spec §2.3 角色权限文案总表，**本节为文案单一来源**）：

| 选中角色 | 面板标题 | 面板内容（按行展示） |
|----------|----------|---------------------|
| **管理员** | `管理员权限` | · 可添加删除应用开发者<br>· 可以添加、删除各种开放服务<br>· 可以申请 API 权限<br>· 可以查看应用基本信息<br>· 可以查看申请后的服务<br>· 可以查看运营报表等 |
| **开发者** | `开发权限` | · 可以查看应用基本信息<br>· 可以添加、删除各种服务<br>· 可以申请 API 权限<br>· 可以查看申请后的服务<br>· 可以查看运营报表等 |

> **布局建议**（仅记录，不强制）：内容项可按 2 列网格布局，每行 2 条；列表项用 `·` 分隔。

**成员选择器规范**（添加成员 + 转移 Owner 共用，对齐 spec §2.3）：

| 位置 | 内容 |
|------|------|
| 下拉项 | 展示 **姓名、工号、部门** 三个字段 |
| 选中后输入框 | 展示选中的成员信息（含姓名、工号、部门） |

**8 步流程**：

| 步骤 | 行为 |
|---|------|
| 1 | 点击"添加成员" |
| 2 | 打开 Modal，角色下拉框已有默认值，角色权限说明面板显示对应文案 |
| 3 | 在成员选择器中输入关键字 → 调 2.5 search-users（防抖 300ms）|
| 4 | 搜索结果以 Select 下拉展示，每项显示**姓名/工号/部门**三字段 |
| 5 | 选中用户 → 输入框组合展示选中成员信息 |
| 6 | **选择角色（按当前操作者角色动态过滤选项 + 智能默认）**：<br>- 当前用户是 Owner：可选"开发者"或"管理员",**默认选中"管理员"**<br>- 当前用户是管理员：仅"开发者"可单选（"管理员"选项禁用）,**默认选中"开发者"**<br>**切换角色后，角色权限说明面板内容实时跟随切换** |
| 7 | 点击"添加"→ 调 2.2 批量添加（`accountIds[]`）|
| 8 | 成功 → 关闭 Modal + 刷新列表 + Toast "已添加 N 位成员" |

#### 9.3.2 删除成员 → DeleteConfirmModal

> **复用公共组件 `components/DeleteConfirmModal/DeleteConfirmModal.jsx`**。

**确认 Modal**：

| 元素 | 内容 |
|------|------|
| 标题 | "确认删除" |
| 内容 | "将删除 XXX 在 [角色] 角色下的成员记录，该操作不可撤销" |
| 取消/确认按钮 | 取消 / 确认删除 |

**流程**：
1. 点击"删除" → 打开 DeleteConfirmModal（传入成员姓名 + 角色名称）
2. 点击"确认删除" → 调 2.3 删除（传成员记录主键 `id`）
3. 成功 → 关闭 Modal + 列表移除该行 + Toast
4. 失败 → Toast `result?.messageZh || result?.message || '删除失败'`

**保护逻辑**：

| 操作者角色 | 能删除的成员 | 错误码 |
|-----------|------------|--------|
| 0=开发者 | ❌ 无权删除任何人 | `403202` |
| 2=管理员 | 只能删除 `memberType=0`（**开发者**）| `403203`（删管理员时）|
| 1=Owner | 可删除 `memberType=0`（开发者）或 `memberType=2`（管理员）| — |
| 任何角色 | **不能删除 Owner**（受保护）| `409201` |

**前端体现**：
- Owner 行的"删除"按钮**隐藏**（保护 Owner）
- 管理员的"删除"按钮对**管理员行隐藏**（不能删其他管理员）
- 管理员的"删除"按钮对**开发者行可见**（可删开发者）

#### 9.3.3 转移 Owner → TransferOwnerModal

**Modal 标题**："转移 Owner"

**展示内容**：

| 元素 | 内容 |
|------|------|
| 成员选择器 | 共用「成员选择器规范」（见 §9.3.1），下拉项展示**姓名/工号/部门**三字段，选中后输入框组合展示；**单选模式**（Select + Radio） |
| 提示 | "转移后，您将不再是该应用的成员，XXX 将成为新 Owner。如需继续访问，请让新 Owner 重新添加您" |
| 二次确认 | 必勾选"我已了解转移后果"复选框 |
| 确认按钮 | "转移"（禁用条件：未选 + 未勾选）|

**流程**：
1. 点击"转移 Owner"（**仅 Owner 行有**）
2. 打开 Modal
3. 在成员选择器中输入关键字 → 调 2.5 search-users → 下拉展示姓名/工号/部门 → 单选
4. 勾选确认框
5. 点击"转移"→ 调 2.4 转移 Owner
6. 成功 → 关闭 Modal + 刷新列表 + Toast
7. 失败 → Toast `result?.messageZh || result?.message || '转移失败'`

### 9.4 后端接口

| 接口 | 用途 | 调用时机 |
|------|------|---------|
| 2.1 `GET /service/open/v2/app/{appId}/members?curPage&pageSize` | 获取应用成员列表 | 页面加载 |
| 2.5 `GET /service/open/v2/app/{appId}/search-users?keyword` | 搜索可添加的用户 | 搜索框输入 |
| 2.2 `POST /service/open/v2/app/{appId}/members` | 添加成员 | 提交 AddMemberModal |
| 2.3 `DELETE /service/open/v2/app/{appId}/members/{id}` | 删除成员（按成员记录主键 id） | 提交 DeleteConfirmModal |
| 2.4 `POST /service/open/v2/app/{appId}/transfer-owner` | 转移 Owner | 提交 TransferOwnerModal |

### 9.5 thunk.js 设计

> 遵守开发规范：每个函数用 try-catch 包裹 fetchApi，引用 API_CONFIG，正常返回 `result || {}`，异常返回 `{}`。

| 函数名 | 接口 | API_CONFIG 键 | 方法 | 参数 |
|--------|------|--------------|------|------|
| `fetchMemberList` | 2.1 | `APP_MEMBERS.LIST` | GET | `appId, params`（curPage, pageSize）|
| `searchUsers` | 2.5 | `APP_MEMBERS.SEARCH_USERS` | GET | `appId, keyword` |
| `addMembers` | 2.2 | `APP_MEMBERS.ADD` | POST | `appId, data`（accountIds[], role）|
| `deleteMember` | 2.3 | `APP_MEMBERS.DELETE` | DELETE | `appId, id`（成员记录主键 id） |
| `transferOwner` | 2.4 | `APP_MEMBERS.TRANSFER_OWNER` | POST | `appId, data`（toAccountId）|
| `fetchCurrentRole` | 1.11 | `APP.CURRENT_ROLE` | GET | `appId` |

### 9.6 constant.js 设计

| 常量名 | 类型 | 说明 |
|--------|------|------|
| `MEMBER_ROLE_MAP` | Object | 成员角色映射：0=开发者 / 1=Owner / 2=管理员 |
| `getMemberColumns` | Function | 表格列配置函数，参数 `{ renderRole, renderAction }`，列：姓名(chineseName) / 工号(w3Account) / 角色(roleName) / 操作(action) |
| `ADD_MEMBER_ROLE_OPTIONS` | Object | 添加成员角色选项：owner 可选管理员+开发者，admin 仅开发者 |

### 9.7 错误处理

| 错误 | 展示 | 恢复 |
|------|------|------|
| 2.1 失败 | 列表显示"加载失败" | 点击重试 |
| 2.5 失败 | 搜索结果区显示"搜索失败" | 重输关键字 |
| 2.2 失败 403200（当前用户角色无添加成员权限）| Modal 内红字 | 退出/重登录 |
| 2.2 失败 403201（当前用户角色无添加该角色成员的权限）| Modal 内红字 | 提示权限不足 |
| 2.3 失败 409201（不能删除 Owner 角色）| Toast | 不允许此操作 |

### 9.8 限制

- Owner 至少 1 个（不能删除最后一个 Owner）
- Owner 不能被删除（必须先转移）
- 成员角色：0=开发者 / 1=Owner / 2=管理员
- **多角色共存**（spec v2.2）：同一成员可在多个角色下同时存在（列表中出现多行）；同一角色下不能有相同成员
- 转移 Owner：原 Owner 仅 Owner 角色下的记录被删除，其他角色下的记录保留；新成员若已有其他角色记录，保留并新增 Owner 记录；**允许转移给自己**（spec v3.8 移除限制）
- 同时选中数量不限（添加时）
- 删除成员按主键 `id` 精确删除单条记录，不影响该成员其他角色记录

### 9.9 权限说明

**本页按钮受 1.11 角色控制**（1.11 返回最高权限角色，多角色时后端已取最高）：

| 按钮 | Owner | Admin | Developer |
|------|:-----:|:-----:|:---------:|
| 添加成员 | ✅ | ✅ | ❌ |
| 删除成员 | ✅ | ✅ | ❌ |
| 转移 Owner | ✅ | ❌ | ❌ |

---

## § 10  用户旅程 F：管理能力（Capabilities）

### 10.0 布局说明

**本页是 4 详情页之一**，使用公共 `Layout` 组件（§4.7）包裹，**但 SimpleSidebar 接收外部 menuConfig 覆盖默认菜单**（显示能力的子菜单：添加应用能力 + 已订阅能力列表）：

```
<Layout
  appId={appId}
  currentMenuKey="capabilities"
  sidebarMenuConfig={[
    { key: 'add', label: '添加应用能力', path: '/capabilities?appId=xxx' },
    { key: 'subscribed', label: '已订阅能力', path: '/capabilities?appId=xxx&tab=subscribed' },
    // 动态：已订阅能力列表（从 3.3 接口返回）
  ]}
>
  {Capabilities 主内容}
</Layout>
```

**主内容区**（Capabilities 页面独有）：

| 组件 | 复用状态 |
|------|---------|
| `AbilityCard` | Capabilities 私有 |
| `AbilityGrid` | Capabilities 私有 |
| ~~AbilitySidebar~~ | **删除**（由 SimpleSidebar 通过 menuConfig 覆盖） |

> 页面级 useEffect 守卫：调 1.3，校验 `appType === 1`（业务应用），非业务应用跳 `/basic-info?appId=xxx`。

### 10.1 入口

从 § 7 左侧菜单跳 `/capabilities?appId=xxx`（**仅业务应用**）。

### 10.2 展示内容

**布局**（与"凭证/成员/版本"菜单同级的**"应用能力"分组**）：

| 区域 | 内容 |
|------|------|
| **左侧菜单**（应用能力分组下）| 固定 1 个 + 已订阅能力动态添加 |
| **主内容区** | 按当前选中的左侧菜单项显示 |

**左侧菜单（应用能力分组）**：

| 菜单项 | 数据源 | 状态 |
|--------|--------|------|
| **添加应用能力** | 前端固定（始终存在）| 始终显示 |
| **群置顶** | 调 3.3 已订阅能力 | **仅已订阅时显示**（动态添加） |
| **群通知** | 调 3.3 已订阅能力 | 仅已订阅时显示（动态添加） |
| **链接增强** | 调 3.3 已订阅能力 | 仅已订阅时显示（动态添加） |
| **点对点通知** | 调 3.3 已订阅能力 | 仅已订阅时显示（动态添加） |
| **We码** | 调 3.3 已订阅能力 | 仅已订阅时显示（动态添加） |
| **应用入群通知** | 调 3.3 已订阅能力 | 仅已订阅时显示（动态添加） |
| **助手广场卡片** | 调 3.3 已订阅能力 | 仅已订阅时显示（动态添加） |

**主内容区（按左侧菜单项切换）**：

| 当前选中菜单项 | 主内容区展示 |
|--------------|------------|
| **添加应用能力**（默认）| **6 个能力卡片网格**（所有能力,每张可"添加"或"配置"）|
| **已订阅能力**（动态项,如"群置顶"）| 该能力的"配置"页 |

**6 个能力卡片元素**：

| 元素 | 数据 | 视觉 |
|------|------|------|
| 能力图标 | `iconUrl` | 48×48 |
| 能力名称 | `nameCn` | 16px 600 |
| 能力描述 | `descCn` | 灰色小字 |
| 示意图 | `diagramUrl` | 200×120 缩略图 |
| 操作按钮 | "添加" / "配置" | 动态显示（已订阅显示"配置"，未订阅显示"添加"）|

### 10.3 按钮与交互

**添加能力流程**：

| 步骤 | 行为 |
|------|------|
| 1 | 点击"添加应用能力" → 调 3.1 获取能力列表 → 渲染所有能力卡片 |
| 2 | 点击某未订阅卡片的"添加"按钮 |
| 3 | 调 3.2 添加能力 |
| 4 | 成功 → **直接跳转到对应的能力菜单项**（如添加"群置顶"则自动跳转到群置顶菜单）|
| 5 | 该菜单项可点击进入"配置"页 |

**配置能力流程**：

| 步骤 | 行为 |
|------|------|
| 1 | 点击已订阅能力菜单项（如"群置顶"）|
| 2 | 主内容区切换为该能力的"配置"页 |
| 3 | 配置页由能力方实现,openPlatform 不实现具体配置 UI |

### 10.4 后端接口

| 接口 | 用途 | 调用时机 |
|------|------|---------|
| 3.3 `GET /service/open/v2/app/{appId}/abilities` | 获取已订阅能力列表 | 页面加载 |
| 3.1 `GET /service/open/v2/abilities?appId=xxx` | 能力列表（主内容卡片）| 切换子菜单时 |
| 3.2 `POST /service/open/v2/app/{appId}/abilities` | 添加能力 | 点击"添加" |

### 10.5 thunk.js 设计

> 遵守开发规范：每个函数用 try-catch 包裹 fetchApi，引用 API_CONFIG，正常返回 `result || {}`，异常返回 `{}`。

| 函数名 | 接口 | API_CONFIG 键 | 方法 | 参数 |
|--------|------|--------------|------|------|
| `fetchSubscribedAbilities` | 3.3 | `APP_ABILITIES.SUBSCRIBED` | GET | `appId` |
| `fetchAbilityList` | 3.1 | `APP_ABILITIES.LIST` | GET | `appId` |
| `addAbility` | 3.2 | `APP_ABILITIES.ADD` | POST | `appId, data`（abilityType）|

### 10.6 constant.js 设计

| 常量名 | 类型 | 说明 |
|--------|------|------|
| `ABILITY_TYPE_MAP` | Object | 能力类型映射：1=群置顶 / 2=群通知 / 3=链接增强 / 4=点对点通知 / 5=We码 / 6=应用入群通知 / 7=助手广场卡片 |

### 10.7 错误处理

| 错误 | 展示 | 恢复 |
|------|------|------|
| 3.3 失败 | 左侧子菜单显示"加载失败" | 点击重试 |
| 3.1 失败 | 右侧主内容显示"加载失败" | 点击重试 |
| 3.2 失败 409400（能力已订阅）| Toast "该能力已添加" | 不需要操作 |
| 3.2 失败 403100（无权访问应用）| Toast "无权限" | 退出/重登录 |

### 10.8 限制

- 能力类型 7 种（1=群置顶 / 2=群通知 / 3=链接增强 / 4=点对点通知 / 5=We码 / 6=应用入群通知 / 7=助手广场卡片）
- 同一能力只能添加 1 次
- 取消订阅的接口 plan.md 暂无独立接口（**Open Question**，见 § 13.3）

### 10.9 权限说明

- 本页内按钮**不基于角色**（开发者也能添加/配置）

---

## § 11  用户旅程 G：管理版本（VersionRelease 列表）

### 11.0 布局说明

**本页是 4 详情页之一**，使用公共 `Layout` 组件（§4.7）包裹：

```
<Layout appId={appId} currentMenuKey="version-release">
  {VersionRelease 主内容（版本列表 + 创建表单）}
</Layout>
```

**主内容区**（VersionRelease 页面独有）：

| 组件 | 复用状态 |
|------|---------|
| `VersionForm` | VersionRelease 私有 |
| `VersionDetail` | VersionRelease 私有（§12 描述） |

> 页面级 useEffect 守卫：调 1.3，校验 `appType === 1`（业务应用），非业务应用跳 `/basic-info?appId=xxx`。

### 11.1 入口

从 § 7 左侧菜单跳 `/version-release?appId=xxx`（**仅业务应用**）。

### 11.2 展示内容（版本列表）

**列表元素**：

| 列 | 数据 | 视觉 |
|---|------|------|
| 版本号 | `versionCode` | 主文本，如 "1.0.0" |
| 版本状态 | `status` | 待发布/审批中/审批未通过/已发布 徽标（不同颜色）|
| 发布人 | `createBy` | 主文本 |
| 审核通过时间 | — | 已发布显示时间,未发布显示 "-" |
| 操作 | 查看/撤回/删除 按钮 | 动态显示（按状态）|

**分页**（对齐 spec FR-011 + §9 成员列表分页）：

| 项目 | 说明 |
|------|------|
| 每页条数 | 可选 10 / 20 / 50，**默认 20** |
| 分页信息 | 左侧显示总条数，右侧显示分页控件和条数选择器 |
| 跳转到某页 | 支持**跳转到某页**（输入页码直跳） |
| 分页组件 | 使用 antd `<Pagination>`，配合 `utils/constants.js` 的 `INIT_PAGECONFIG` |

**状态机**（plan.md 4.1 出参 `status`）：

| 状态值 | 名称 | 4.4 发布 | 4.5 撤回 | 4.6 删除 | 4.7 更新 |
|:----:|------|:---:|:---:|:---:|:---:|
| 1 | 待发布 | ✅ | ❌ | ✅ | ✅ |
| 2 | 审批中 | ❌ | ✅ | ❌ | ❌ |
| 3 | 审批未通过 | ❌ | ❌ | ✅ | ❌ |
| 4 | 已发布 | ❌ | ❌ | ❌ | ❌ |

### 11.3 按钮与交互

#### 11.3.1 创建版本（行内表单）

| 字段 | 校验规则 |
|------|---------|
| `versionCode`（版本号）| 必填,SemVer 格式（`^\d+\.\d+\.\d+$`,X/Y/Z 为非负整数）|
| `versionDescCn`（中文描述）| 必填,1-2000 字符 |
| `versionDescEn`（英文描述）| 可选,1-2000 字符 |

> **说明**：版本关联的能力**前端不传**——后端**自动从该应用已订阅的能力中带出**。

| 步骤 | 行为 |
|---|------|
| 1 | 点击"创建版本" |
| 2 | 表格上方展开创建行（行内表单）|
| 3 | 输入版本号 → **前端实时校验 SemVer 格式**（正则 `^\d+\.\d+\.\d+$`）|
| 4 | 输入中文描述（必填,1-2000 字符）|
| 5 | 输入英文描述（可选,1-2000 字符）|
| 6 | **前端整体校验**：版本号格式 + 中文描述 |
| 7 | "提交"按钮禁用条件：校验未通过 |
| 8 | 点击"提交"→ 调 4.2 创建版本 |
| 9 | 成功 → 关闭行内表单 + 列表新增行 + Toast |
| 10 | 失败 → 行内红字 |

#### 11.3.2 查看版本 → VersionDetail 组件

点击"查看"按钮 → 页面内切换到 `VersionDetail` 组件（非独立路由跳转，同页面内切换展示）。

#### 11.3.3 删除/撤回 → DeleteConfirmModal

> **复用公共组件 `components/DeleteConfirmModal/DeleteConfirmModal.jsx`**。

| 操作 | 显示条件 | DeleteConfirmModal 内容 |
|------|---------|------------------------|
| 删除 | 待发布/审批未通过 | "确认删除版本 1.0.0？该操作不可撤销" |
| 撤回 | 审批中 | "确认撤回版本 1.0.0 的发布申请？" |

**流程**：
1. 点击"删除"/"撤回" → 打开 DeleteConfirmModal
2. 点击"确认"→ 调 4.6 删除 / 4.5 撤回
3. 成功 → 关闭 Modal + 列表更新 + Toast
4. 失败 → Toast `result?.messageZh || result?.message || '操作失败'`

### 11.4 后端接口

| 接口 | 用途 | 调用时机 |
|------|------|---------|
| 4.1 `GET /service/open/v2/app/{appId}/versions?curPage&pageSize` | 获取版本列表（**支持分页 10/20/50 + 跳转到某页**） | 页面加载 / 翻页 / 切换条数 / 跳页 |
| 4.2 `POST /service/open/v2/app/{appId}/versions` | 创建版本 | 提交行内表单 |
| 4.5 `POST /service/open/v2/app/{appId}/versions/{versionId}/withdraw` | 撤回版本 | 提交撤回 |
| 4.6 `DELETE /service/open/v2/app/{appId}/versions/{versionId}` | 删除版本 | 提交删除 |

### 11.5 thunk.js 设计

> 遵守开发规范：每个函数用 try-catch 包裹 fetchApi，引用 API_CONFIG，正常返回 `result || {}`，异常返回 `{}`。

| 函数名 | 接口 | API_CONFIG 键 | 方法 | 参数 |
|--------|------|--------------|------|------|
| `fetchVersionList` | 4.1 | `APP_VERSIONS.LIST` | GET | `appId, params`（curPage, pageSize）|
| `createVersion` | 4.2 | `APP_VERSIONS.CREATE` | POST | `appId, data`（versionCode, versionDescCn, versionDescEn）|
| `fetchVersionDetail` | 4.3 | `APP_VERSIONS.DETAIL` | GET | `appId, versionId` |
| `publishVersion` | 4.4 | `APP_VERSIONS.PUBLISH` | POST | `appId, versionId` |
| `withdrawVersion` | 4.5 | `APP_VERSIONS.WITHDRAW` | POST | `appId, versionId` |
| `deleteVersion` | 4.6 | `APP_VERSIONS.DELETE` | DELETE | `appId, versionId` |
| `updateVersion` | 4.7 | `APP_VERSIONS.UPDATE` | PUT | `appId, versionId, data` |

### 11.6 constant.js 设计

| 常量名 | 类型 | 说明 |
|--------|------|------|
| `VERSION_STATUS_MAP` | Object | 版本状态映射：1=待发布 / 2=审批中 / 3=审批未通过 / 4=已发布 |
| `getVersionColumns` | Function | 表格列配置函数，参数 `{ renderStatus, renderAction }`，列：版本号(versionCode) / 版本状态(status) / 发布人(createBy) / 审核通过时间(approvedTime) / 操作(action) |
| `VERSION_CODE_PATTERN` | RegExp | 版本号校验正则 `^\d+\.\d+\.\d+$` |
| `VERSION_FORM_RULES` | Object | 表单校验规则：versionCode 必填 SemVer 格式, versionDescCn 必填 1-2000 字符, versionDescEn 可选 0-2000 字符 |

### 11.7 错误处理

| 错误 | 展示 | 恢复 |
|------|------|------|
| 4.1 失败 | 列表显示"加载失败" | 点击重试 |
| 4.2 失败 400105（版本号格式错误）| 版本号下方红字 | 修改后重提 |
| 4.2 失败 409300（版本号已被其他版本占用）| 版本号下方红字 | 改版本号 |
| 4.5 失败 409303（状态转换非法）| Toast | 不允许此操作 |
| 4.6 失败 403100（无权访问）/ 409303（状态转换非法）| Toast | 需先撤回或下线 |

### 11.8 限制

- 版本号 SemVer 格式（X.Y.Z）
- 同一应用内版本号唯一
- 状态机约束（见 11.2）
- 已发布版本不能直接删除（需先撤回审批或标记下线）

---

## § 12 用户旅程 H：查看版本详情（VersionDetail 组件）

> 版本详情不作为独立路由页面，而是在 VersionRelease 页面内通过组件切换展示。

### 12.0 布局说明

**VersionDetail 是 VersionRelease 页面内的子组件**（不是独立页面），继承 VersionRelease 的 Layout 包裹（§11.0）。本节不重复 Layout 描述。

### 12.1 入口

从 § 11 列表点击"查看"按钮 → 页面内切换到 `VersionDetail` 组件

### 12.2 数据加载

- 调 4.3 获取版本详情（含版本号、版本名、状态、能力列表、创建/更新时间）

### 12.3 展示内容

**布局**（从上到下）：

| 区域 | 内容 |
|------|------|
| 顶部 | 返回按钮（→ 版本列表） + 版本号 + 状态徽标 |
| 版本信息卡片 | 版本号、版本名、状态、创建时间、创建者、最后更新时间 |
| 关联能力卡片 | 已选能力列表（卡片网格，每项含名称+描述+示意图）|
| 操作按钮区 | 按状态显示（见下表）|

**操作按钮（按状态显隐）**：

> **产品实际**：**只有"待发布(1)"状态进入详情页才有操作按钮**，其他状态进入详情页**没有操作按钮**。
> **待发布(1) 状态**有"编辑"和"发布"按钮（**没有"删除"按钮**）。

| 状态 | 编辑 | 发布 | 撤回 |
|------|:---:|:---:|:---:|
| 待发布(1) | ✅ | ✅ | ❌ |
| 审批中(2) | ❌ | ❌ | ❌ |
| 审批未通过(3) | ❌ | ❌ | ❌ |
| 已发布(4) | ❌ | ❌ | ❌ |

### 12.4 按钮与交互

| 按钮 | 交互 |
|------|------|
| 编辑 | 卡片切到编辑态 → 保存调 4.7 |
| 发布 | DeleteConfirmModal → 调 4.4 发布（提交审批）|
| 撤回 | DeleteConfirmModal → 调 4.5 撤回 |
| 返回 | 切回版本列表视图 |

### 12.5 后端接口

| 接口 | 用途 | 调用时机 |
|------|------|---------|
| 4.3 `GET /service/open/v2/app/{appId}/versions/{versionId}` | 获取版本详情 | 切换到详情 |
| 4.7 `PUT /service/open/v2/app/{appId}/versions/{versionId}` | 更新版本 | 编辑保存 |
| 4.4 `POST /service/open/v2/app/{appId}/versions/{versionId}/publish` | 发布版本（提交审批）| 提交发布 |
| 4.5 `POST /service/open/v2/app/{appId}/versions/{versionId}/withdraw` | 撤回版本 | 提交撤回 |

### 12.6 错误处理

| 错误 | 展示 | 恢复 |
|------|------|------|
| 4.3 失败 404300（版本不存在）| Toast + 切回列表 | 返回列表 |
| 4.4 失败 409303（状态转换非法）| Toast | 不允许此操作 |
| 4.5 失败 409303（状态转换非法）| Toast | 不允许此操作 |
| 4.7 失败 400105（版本号格式错误）/ 409303（当前状态非待发布，不可编辑）| 字段下方红字 | 修改后重提 |

### 12.7 限制

- 已发布版本不能再编辑/发布/撤回/删除
- 状态机约束（同 11.8）

---

## § 13 横切关注点

### 13.1 关键状态约束

**应用状态**（1.3 出参 `status`）：

| 值 | 含义 | 前端处理 |
|---|------|---------|
| 1 | 有效 | 正常展示 |
| 0 | 失效 | 跳错误页 + "应用已失效"提示 |

**版本状态**（4.x 出参 `status`）：

| 值 | 含义 | 前端处理 |
|---|------|---------|
| 1 | 待发布 | 可编辑/发布/删除 |
| 2 | 审批中 | 可撤回 |
| 3 | 审批未通过 | 可删除（不可编辑/重新发布） |
| 4 | 已发布 | 不可操作（只读）|

### 13.2 角色权限原则（核心规则）

**1.11 作用范围（仅 2 处用）**：

| 位置 | 用法 |
|------|------|
| ① BasicInfo 入口校验 | `role=null` → 跳转回应用列表页 `/` |
| ② Members 页内按钮级显隐 | 添加/删除/转移成员（1.11 返回最高权限角色，多角色时后端已取最高） |

**菜单显隐**（基于 appType，不基于角色）：

| 应用类型 | 菜单数 |
|---------|:-----:|
| 业务应用（`appType=1`）| 4 个 |
| 个人应用（`appType=0`）| 1 个 |
| 存量个人应用（`appType=0 && appSubType=0`）| 1 个 + 顶部"绑定 eamap"按钮 |

**菜单内按钮权限矩阵**：

| 页面 | 按钮 | Owner | Admin | Developer |
|-----|------|:-----:|:-----:|:---------:|
| 凭证 | 编辑基本信息 | ✅ | ✅ | ✅ |
| 凭证 | 配置认证方式 | ✅ | ✅ | ✅ |
| 凭证 | 升级 EAMAP（仅存量个人） | ✅ | ✅ | ✅ |
| **成员** | **添加成员** | ✅ | ✅ | ❌ |
| **成员** | **删除成员** | ✅ | ✅ | ❌ |
| **成员** | **转移 Owner** | ✅ | ❌ | ❌ |
| 能力 | 添加能力 | ✅ | ✅ | ✅ |
| 能力 | 取消订阅 | ✅ | ✅ | ✅ |
| 版本 | 创建版本 | ✅ | ✅ | ✅ |
| 版本 | 删除/撤回 | ✅ | ✅ | ✅ |

> 粗体行表示受 1.11 控制；其他行**不基于角色**（开发者也能操作）。

### 13.3 业务规则汇总

| 规则 | 章节 | 说明 |
|------|------|------|
| 应用类型不可变 | § 6 | 创建时决定，不能改 |
| 业务应用必绑 EAMAP | § 6 / § 8 | 个人应用不绑 |
| 存量个人应用 = `appSubType=0` | § 7 | 升级路径：绑 EAMAP |
| 升级后 `appSubType` 从 0→4 | § 8 | 由 1.10 自动完成 |
| 一码一应用 | § 6 / § 8 | 同一 EAMAP 只能绑一个应用 |
| 4 菜单显隐基于 appType | § 7 | 不基于角色 |
| 成员管理页按钮基于 role | § 9 | 受 1.11 控制 |
| Owner 至少 1 个 | § 9 | 不可删/转走最后一个 |
| 版本号 SemVer | § 11 | X.Y.Z |
| 已发布版本不可改 | § 11/§ 12 | 只读 |
| 审计日志（FR-019） | 全局 | 后端自动完成（`@AuditLog` 注解 + 异步持久化），**前端无需实现** |

### 13.4 前端必做校验清单

> 为提升用户体验、减少后端往返，**所有"调用接口前的字段校验"前端必须先做**。后端校验是兜底。
> 校验规则统一定义在各页面 `constant.js` 的 `FORM_VALIDATION_RULES` 中。

| 接口 | 字段 | 校验规则 | 所在 constant.js |
|------|------|---------|-----------------|
| **1.1** 创建应用 | `nameCn` | 必填,1-255 字符 | `AppList/constant.js` |
| **1.1** 创建应用 | `nameEn` | 必填,1-255 字符 | `AppList/constant.js` |
| **1.1** 创建应用 | `eamapAppCode` | 必填 | `AppList/constant.js` |
| **1.2** 更新应用 | `nameCn` / `nameEn` | 必填,1-255 字符 | `BasicInfo/constant.js` |
| **1.2** 更新应用 | `descCn` / `descEn` | 0-2000 字符 | `BasicInfo/constant.js` |
| **1.7** 更新认证方式 | `verifyType` | `int[]`（**多选**）：0/1/2/3/**4** | `BasicInfo/constant.js` |
| **1.7** 更新认证方式 | `apiSecret` | **列表中含 2（数字签名）时**必填,**16 位,必须同时包含字母和数字** | `BasicInfo/constant.js` |
| **1.10** 绑定 EAMAP | `eamapAppCode` | 必填 | `BasicInfo/constant.js` |
| **1.12** 上传图片(图标) | `bizType=1` | 类型 png/jpg/jpeg + **尺寸 128×128px** + 大小 ≤100KB | `BasicInfo/constant.js` |
| **1.12** 上传图片(示意图) | `bizType=2` | 类型 png/jpg/jpeg + **尺寸 360×200px** + 大小 ≤500KB | `BasicInfo/constant.js` |
| **2.2** 添加成员 | `accountIds[]` | 必填,数量不限 | `Members/constant.js` |
| **2.2** 添加成员 | `role` | 0/2 | `Members/constant.js` |
| **4.2** 创建版本 | `versionCode` | SemVer 格式 X.Y.Z | `VersionRelease/constant.js` |
| **4.2** 创建版本 | `versionDescCn` | 必填,1-2000 字符 | `VersionRelease/constant.js` |
| **4.7** 更新版本 | 同 4.2 | 同 4.2 | `VersionRelease/constant.js` |

### 13.5 编码规范速查

> ⚠️ **生成代码前必读** [`front/doc/开发规范文档.md`](../../../../../front/doc/开发规范文档.md) —— 本节只列 9 条速查，**完整规范包含 10 大章节 + 附录**：
>
> | 章节 | 标题 |
> |------|------|
> | 一 | 项目结构规范（页面/公共目录）|
> | 二 | 文件编写规范（index.js / route.js / thunk.js / constant.js / 样式）|
> | 三 | 公共规范（API 配置 / 工具函数 / 公共组件）|
> | 四 | 命名规范（变量/函数/组件/常量/API）|
> | 五 | 代码风格规范（缩进/引号/大括号/空格/空行）|
> | 六 | 特殊规则（禁止 useCallback / 参数处理 / 注释 / 重复度）|
> | 七 | 组件编写规范（SearchBar / ConfirmModal / FormModal）|
> | 八 | 代码复用规范（公共常量/方法抽取）|
> | 九 | 错误处理规范（API 错误 / 统一提示）|
> | 十 | 状态管理规范（页面级状态 / 状态分组）|
> | 附录 | 文件清单 + 检查清单（7 项）|
>
> **执行生成代码时必须严格遵守上述 10 大章节**——本节下方 9 条是**最高频**的速查。

| 规则 | 说明 |
|------|------|
| 禁止 useCallback | 所有函数使用普通箭头函数 |
| 禁止行内样式 | 全部使用 `.m.less` 类名 |
| thunk.js try-catch | 每个接口函数必须 `try { ... return result \|\| {}; } catch (err) { return {}; }` |
| 错误提示格式 | `result?.messageZh \|\| result?.message \|\| '固定文案'` |
| 3+ 参数用对象 | `const fn = (params = {}) => { const { a, b, c } = params; ... }` |
| 公共常量直接导入 | 不要在页面 constant.js 中重复导出 `utils/constants.js` 的内容 |
| 表格列函数化 | 使用 `getColumns({ renderXxx })` 放在 constant.js 中 |
| 组件目录结构 | 页面子组件放 `components/` 子目录，配套 `.m.less` |
| 入口文件精简 | index.jsx 只做：权限判断 + 通用数据方法 + 组合组件 |

---

## § 14 前端必读的源码位置

### 14.1 spec.md

- 15 个核心 FR + FR-016/017/018/019/020
- 5 个核心模块
- 字段定义、状态机

### 14.2 plan.md

- 9 张表 DDL
- 27 个 API 详细设计（1.1-4.7）
- 错误码（4xx 段）
- 数据模型 + 业务规则

### 14.3 demo-app-list.html

- 3963 行高保真原型
- 65 个 JS 函数（页面行为参考）
- 实际视觉效果参考
- 65% 还原度即可，重点是逻辑而非像素

### 14.4 wecodesite 目录

- **实际前端项目根目录**：`wecodesite/`
- **已有页面骨架**：`pages/AppList/`、`pages/BasicInfo/`、`pages/Members/`、`pages/Capabilities/`、`pages/VersionRelease/`（均为 mock 数据，需替换为真实 API）
- **已有公共组件**：`components/CreateAppModal/`、`components/AppCard/`、`components/DeleteConfirmModal/`、`components/BindEamapModal/` 等
- **API 配置**：`configs/web.config.js`（需追加 APP 模块 URL）
- **公共常量**：`utils/constants.js`（需追加 APP 相关枚举）
- **开发规范**：`front/doc/开发规范文档.md`

---

## § 15 27 个后端 API 速查

**应用模块（1.1-1.12，共 12 个）**：

| 编号 | REST | API_CONFIG 键 | 用途 | thunk.js 所在页面 |
|------|------|--------------|------|------------------|
| 1.1 | `POST /app` | `APP.CREATE` | 创建应用 | AppList |
| 1.2 | `PUT /app/{appId}` | `APP.UPDATE` | 更新应用 | BasicInfo |
| 1.3 | `GET /app/{appId}` | `APP.DETAIL` | 获取应用基本信息 | BasicInfo |
| 1.4 | `GET /app?curPage&pageSize` | `APP.LIST` | 获取应用列表 | AppList |
| 1.5 | `GET /app/eamap?curPage&pageSize` | `APP.EAMAP_LIST` | 获取 EAMAP 列表 | AppList + BasicInfo |
| 1.6 | `GET /app/icons` | `APP.ICONS` | 获取默认图标列表 | AppList |
| 1.7 | `PUT /app/{appId}/verify-type` | `APP.VERIFY_TYPE_UPDATE` | 更新认证方式 | BasicInfo |
| 1.8 | `GET /app/{appId}/identity` | `APP.IDENTITY` | 获取应用凭证 | BasicInfo |
| 1.9 | `GET /app/{appId}/verify-type` | `APP.VERIFY_TYPE_GET` | 获取认证方式 | BasicInfo |
| 1.10 | `POST /app/{appId}/bind-eamap` | `APP.BIND_EAMAP` | 绑定 EAMAP | BasicInfo |
| 1.11 | `GET /app/{appId}/current-role` | `APP.CURRENT_ROLE` | 获取当前用户角色 | BasicInfo + Members |
| 1.12 | `POST /file/upload?bizType=N` | `APP.FILE_UPLOAD` | 上传图片 | AppList + BasicInfo |

**成员模块（2.1-2.5，共 5 个）**：

| 编号 | REST | API_CONFIG 键 | 用途 | thunk.js 所在页面 |
|------|------|--------------|------|------------------|
| 2.1 | `GET /app/{appId}/members?curPage&pageSize` | `APP_MEMBERS.LIST` | 获取成员列表 | Members |
| 2.2 | `POST /app/{appId}/members` | `APP_MEMBERS.ADD` | 添加成员 | Members |
| 2.3 | `DELETE /app/{appId}/members/{id}` | `APP_MEMBERS.DELETE` | 删除成员（按主键 id） | Members |
| 2.4 | `POST /app/{appId}/transfer-owner` | `APP_MEMBERS.TRANSFER_OWNER` | 转移 Owner | Members |
| 2.5 | `GET /app/{appId}/search-users?keyword` | `APP_MEMBERS.SEARCH_USERS` | 搜索用户 | Members |

**能力模块（3.1-3.3，共 3 个）**：

| 编号 | REST | API_CONFIG 键 | 用途 | thunk.js 所在页面 |
|------|------|--------------|------|------------------|
| 3.1 | `GET /abilities?appId=xxx` | `APP_ABILITIES.LIST` | 能力列表 | Capabilities |
| 3.2 | `POST /app/{appId}/abilities` | `APP_ABILITIES.ADD` | 添加能力 | Capabilities |
| 3.3 | `GET /app/{appId}/abilities` | `APP_ABILITIES.SUBSCRIBED` | 已订阅能力列表 | Capabilities |

**版本模块（4.1-4.7，共 7 个）**：

| 编号 | REST | API_CONFIG 键 | 用途 | thunk.js 所在页面 |
|------|------|--------------|------|------------------|
| 4.1 | `GET /app/{appId}/versions?curPage&pageSize` | `APP_VERSIONS.LIST` | 获取版本列表 | VersionRelease |
| 4.2 | `POST /app/{appId}/versions` | `APP_VERSIONS.CREATE` | 创建版本 | VersionRelease |
| 4.3 | `GET /app/{appId}/versions/{versionId}` | `APP_VERSIONS.DETAIL` | 获取版本详情 | VersionRelease |
| 4.4 | `POST /app/{appId}/versions/{versionId}/publish` | `APP_VERSIONS.PUBLISH` | 发布版本 | VersionRelease |
| 4.5 | `POST /app/{appId}/versions/{versionId}/withdraw` | `APP_VERSIONS.WITHDRAW` | 撤回版本 | VersionRelease |
| 4.6 | `DELETE /app/{appId}/versions/{versionId}` | `APP_VERSIONS.DELETE` | 删除版本 | VersionRelease |
| 4.7 | `PUT /app/{appId}/versions/{versionId}` | `APP_VERSIONS.UPDATE` | 更新版本 | VersionRelease |

---

## § 16 configs/web.config.js 追加内容

> 在现有 `API_CONFIG` 中追加以下 4 组配置：

| 组名 | 键名 | URL 路径 | 说明 |
|------|------|---------|------|
| **APP** | `LIST` | `/app` | 获取应用列表 |
| | `CREATE` | `/app` | 创建应用 |
| | `DETAIL` | `/app/{appId}` | 获取应用详情 |
| | `UPDATE` | `/app/{appId}` | 更新应用 |
| | `EAMAP_LIST` | `/app/eamap` | 获取 EAMAP 列表 |
| | `ICONS` | `/app/icons` | 获取默认图标列表 |
| | `VERIFY_TYPE_GET` | `/app/{appId}/verify-type` | 获取认证方式 |
| | `VERIFY_TYPE_UPDATE` | `/app/{appId}/verify-type` | 更新认证方式 |
| | `IDENTITY` | `/app/{appId}/identity` | 获取应用凭证 |
| | `BIND_EAMAP` | `/app/{appId}/bind-eamap` | 绑定 EAMAP |
| | `CURRENT_ROLE` | `/app/{appId}/current-role` | 获取当前用户角色 |
| | `FILE_UPLOAD` | `/file/upload` | 上传图片（bizType 通过 buildApiUrl 传入）|
| **APP_MEMBERS** | `LIST` | `/app/{appId}/members` | 获取成员列表 |
| | `ADD` | `/app/{appId}/members` | 添加成员 |
| | `DELETE` | `/app/{appId}/members/{id}` | 删除成员（按主键 id） |
| | `TRANSFER_OWNER` | `/app/{appId}/transfer-owner` | 转移 Owner |
| | `SEARCH_USERS` | `/app/{appId}/search-users` | 搜索用户 |
| **APP_ABILITIES** | `LIST` | `/abilities` | 能力列表 |
| | `ADD` | `/app/{appId}/abilities` | 添加能力 |
| | `SUBSCRIBED` | `/app/{appId}/abilities` | 已订阅能力列表 |
| **APP_VERSIONS** | `LIST` | `/app/{appId}/versions` | 获取版本列表 |
| | `CREATE` | `/app/{appId}/versions` | 创建版本 |
| | `DETAIL` | `/app/{appId}/versions/{versionId}` | 获取版本详情 |
| | `PUBLISH` | `/app/{appId}/versions/{versionId}/publish` | 发布版本 |
| | `WITHDRAW` | `/app/{appId}/versions/{versionId}/withdraw` | 撤回版本 |
| | `DELETE` | `/app/{appId}/versions/{versionId}` | 删除版本 |
| | `UPDATE` | `/app/{appId}/versions/{versionId}` | 更新版本 |

---

## § 17 utils/constants.js 追加内容

> 在现有 `utils/constants.js` 中追加以下枚举：

| 常量名 | 说明 | 值 |
|--------|------|-----|
| `APP_TYPE_MAP` | 应用类型映射 | 0={ text: '个人应用', color: 'default' } / 1={ text: '业务应用', color: 'blue' } |
| `APP_STATUS_MAP` | 应用状态映射 | 1={ text: '有效', color: 'success' } / 0={ text: '失效', color: 'error' } |
| `VERIFY_TYPE_MAP` | 认证方式映射（**多选**）| 0={ text: 'Cookie', needApiSecret: false } / 1={ text: 'SOAHeader', needApiSecret: false } / 2={ text: '数字签名', needApiSecret: true } / 3={ text: 'SOAURL', needApiSecret: false } / **4={ text: 'APIG', needApiSecret: false }** |
| `VERSION_STATUS_MAP` | 版本状态映射 | 1={ text: '待发布', color: 'default' } / 2={ text: '审批中', color: 'processing' } / 3={ text: '审批未通过', color: 'error' } / 4={ text: '已发布', color: 'success' } |

> **注意**：各页面 `constant.js` 中如果需要这些映射，**直接从 `utils/constants.js` 导入**，不要重复导出。

---

## § 18 新旧 UI 动态切换（基于 Lookup · 应用白名单）

> 对应 plan.md §4.7「新旧 UI 动态切换（基于 Lookup · 应用白名单）」的前端实现细节。

### 18.1 设计目标

| # | 目标 | 说明 |
|:-:|------|------|
| 1 | **应用维度灰度** | 后端 Lookup 配置**应用白名单**（appId 列表），白名单内的应用使用新 UI + V2 接口，不在白名单的应用使用旧 UI + 旧接口。**白名单为空时，所有应用都使用新页面**（灰度全量） |
| 2 | **路由级切换** | 通过路由路径区分新旧页（`/basic-info` 旧 vs `/basic-info-v2` 新），由全局路由守卫判断是否跳转 |
| 3 | **热切换** | 运维修改 Lookup 后，用户路由切换/页面刷新时拉取最新白名单 |
| 4 | **降级安全** | 白名单 API 失败时降级为"白名单空 = 所有应用新页面" |
| 5 | **零停机** | 新旧代码共存，Nginx 无需改配置 |

### 18.2 整体流程

```
用户进入应用详情页（带 appId 参数）
  ↓
Layout 挂载 → useRouteAppWhitelistGuard() 触发
  ↓
fetchAppWhitelist() 拉取应用白名单（GET /lookup/whitelist）
  ↓
  后端返回: data: [{ itemCode: "app_20260603_xyz789" }, { itemCode: "app_20260604_abc456" }]
  ↓
  前端提取: appIds = data.map(item => item.itemCode) = ["app_20260603_xyz789", "app_20260604_abc456"]
  ↓
  从 URL 获取当前 appId（location.search 中的 appId 参数）
  ↓
  白名单为空 → 所有应用走新页面（全量）
  白名单非空 → isInWhitelist = appIds.includes(currentAppId)
  ↓
  isInWhitelist + 旧页 → 跳新页 /basic-info-v2 ✅
```

### 18.3 4 个分支判断

| 当前 appId | 白名单状态 | 当前访问页 | 期望停留页 | 动作 |
|:----------:|:----------:|:----------:|:----------:|------|
| 在白名单内 | 非空 | 旧页 | 新页 | 跳新页（白名单内升级） |
| 在白名单内 | 非空 | 新页 | 新页 | 不动 |
| 不在白名单 | 非空 | 旧页 | 旧页 | 不动 |
| 不在白名单 | 非空 | 新页 | 旧页 | 跳旧页（白名单外降级） |
| 任意 | **空** | 旧页 | 新页 | 跳新页（白名单空 = 全量新） |
| 任意 | **空** | 新页 | 新页 | 不动 |

**等价判断**：
- 白名单为空 → `useNewPage = true`（全量新页面）
- 白名单非空 → `useNewPage = appIds.includes(currentAppId)`
- `isNewPage === useNewPage` → 不动；否则切换。

### 18.4 新旧路由映射

**维护位置**：`src/configs/web.config.js` 的 `ROUTE_VERSION_MAP`

| 旧路由 | 新路由 | 备注 |
|--------|--------|------|
| `/` | `/app-list-v2` | AppList |
| `/basic-info` | `/basic-info-v2` | BasicInfo |
| `/members` | `/members-v2` | Members |
| `/capabilities` | `/capabilities-v2` | Capabilities |
| `/capability-detail` | `/capability-detail-v2` | CapabilityDetail |
| `/version-release` | `/version-release-v2` | VersionRelease |
| `/operation-log` | `/operation-log-v2` | OperationLog |

> 暂未涉及的页面（api-management、events、callbacks、admin/*、connect/*）保持单版本，不参与灰度。

### 18.5 白名单接口响应

**接口**：`GET /service/open/v2/lookup/whitelist`

**响应体**（后端从 `openplatform_lookup_item_t` 表中 `classify_code = APP_UI_WHITELIST` 且 `status = 1` 的所有 item 提取 `item_code` 字段）：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "itemCode": "app_20260603_xyz789" },
    { "itemCode": "app_20260604_abc456" }
  ]
}
```

> **关键点**：`data` 是**对象数组**，每个对象的 `itemCode` 字段（对应 lookup item 的 `item_code` 列）就是 appId。前端必须从每个对象中取 `.itemCode` 字段。

**核心规则**：

| `data` 内容 | 含义 | 效果 |
|------|------|------|
| `[]`（空数组） | 白名单为空 | **所有应用走新页面**（灰度全量） |
| `[{itemCode:"app_20260603_xyz789"}]` | 白名单有应用 | 仅该应用走新页面，其他应用走旧页面 |

**前端处理伪代码**：

```js
// 获取应用白名单 appId 列表
const appIds = whitelist.map(item => item.itemCode);

// 核心判断：白名单为空 = 全量新页面；非空 = 按应用判断
const useNewPage = appIds.length === 0 || appIds.includes(currentAppId);
```

### 18.6 路由守卫 Hook

**文件**：`src/hooks/useRouteAppWhitelistGuard.js`

**挂载位置**：`Layout.jsx` 根组件，全局生效。

**核心逻辑**：

```
监听 location.pathname 变化
  → 判断 pathname 是新页（NEW_PAGE_ROUTES.includes）还是旧页（ROUTE_VERSION_MAP keys includes）
  → 既不在新页也不在旧页 → 跳过（如 /api-management）
  → fetchAppWhitelist()（带内存缓存）
  → appIds = whitelist.map(item => item.itemCode)
  → 从 URL search 参数获取当前 appId
  → if (appIds.length === 0) useNewPage = true        // 白名单空 = 全量新
    else useNewPage = appIds.includes(currentAppId)    // 按应用判断
  → if (isNewPage === useNewPage) return // 同向不动
  → 否则跳转：navigate(target + location.search, { replace: true })
  → 跳转时保留 search 参数，避免 appId 等参数丢失
```

**关键边界**：

- 首屏加载（Layout 挂载时）触发一次
- 路由切换触发（location.pathname 变化）
- 同一 pathname 不重复触发（用 `useRef` 记录上次 pathname）
- 白名单接口失败 → catch 中缓存空数组 → 走"所有应用新页面"（白名单空 = 全量新）
- 应用列表页（无 appId 参数）→ 始终走新页面（应用列表无旧版概念）

### 18.7 应用 ID 获取

**文件**：`src/utils/common.js`

当前实现：从 URL search 参数中获取 `appId`。

```js
export const getCurrentAppId = () => {
  const search = new URLSearchParams(location.search);
  return search.get('appId') || '';
};
```

### 18.8 Layout 副作用

挂载路由守卫后，**Layout 内的 `isDetailPage` 判断必须同步更新**：

| 路径 | isDetailPage | Sidebar | AppInfoBar |
|------|:-----------:|:-------:|:----------:|
| `/` | false | ❌ | ❌ |
| `/app-list-v2` | false | ❌ | ❌ |
| `/basic-info` | true | ✅ | ✅ |
| `/basic-info-v2` | true | ✅ | ✅ |
| `/members-v2` | true | ✅ | ✅ |
| `/admin/*` | false | ❌ | ❌ |
| `/connect/*` | false | ❌ | ❌ |

> **关键**：AppList 的旧/新路径（`/` 和 `/app-list-v2`）都不能渲染 Sidebar；其他应用内页面的新路径（`/basic-info-v2`、`/members-v2` 等）必须保留 Sidebar。

**Layout 判断伪代码**：

```js
const isAdminPage = location.pathname.startsWith('/admin') || location.pathname.startsWith('/connect');
const isAppListPage = location.pathname === '/' || location.pathname === '/app-list-v2';
const isDetailPage = !isAdminPage && !isAppListPage;
```

### 18.9 新增/修改文件清单

| # | 文件路径 | 类型 | 说明 |
|:-:|---------|:----:|------|
| 1 | `src/hooks/useRouteAppWhitelistGuard.js` | 新建 | 全局路由守卫 Hook（应用白名单版） |
| 2 | `src/configs/web.config.js` | 修改 | +`ROUTE_VERSION_MAP` / `NEW_PAGE_ROUTES` / `LOOKUP.WHITELIST` / `fetchAppWhitelist` |
| 3 | `src/utils/common.js` | 修改 | +`getCurrentAppId()` |
| 4 | `src/components/Layout/Layout.jsx` | 修改 | +`useRouteAppWhitelistGuard()` 调用 + isDetailPage 判断调整 |
| 5 | `src/App.jsx` | 修改 | +7 个 `*-v2` 路由（v1 占位，后续手动替换） |

### 18.10 全量上线流程

1. 新页面组件就绪后，修改 `App.jsx` 的 v2 路由指向新组件
2. 灰度验证通过后，删除 `App.jsx` 旧路由
3. 从 `ROUTE_VERSION_MAP` 中移除对应条目
4. 删除 `useRouteAppWhitelistGuard` 引用和 hook 文件

### 18.11 降级策略

| 场景 | 行为 |
|------|------|
| 白名单 API 调用失败 | `fetchAppWhitelist()` catch 中缓存空数组 `[]` → 白名单空 = **所有应用走新页面** |
| 白名单 API 响应非 200 | 同上，降级为空数组 → 所有应用走新页面 |
| Lookup 表中无 `APP_UI_WHITELIST` 分类 | 后端返回空数组 → 所有应用走新页面 |
| `data` 数组为空 `[]` | 所有应用走新页面（灰度全量） |
| 数据格式异常（非对象数组） | `appIds.map()` 取 `.itemCode` 得 `undefined`，结果为空数组 → 所有应用走新页面 |
| URL 无 appId 参数（应用列表页） | 始终走新页面（应用列表无旧版概念） |

### 18.12 E2E 验证场景

| 场景 | 后端白名单（data） | 当前 appId | 访问路径 | 预期最终路径 |
|------|------------------|-----------|---------|------------|
| 白名单内应用访问旧页 | `[{itemCode:"app_001"}]` | `app_001` | `/basic-info` | `/basic-info-v2` |
| 白名单内应用访问新页 | `[{itemCode:"app_001"}]` | `app_001` | `/basic-info-v2` | `/basic-info-v2` |
| 白名单外应用访问旧页 | `[{itemCode:"app_001"}]` | `app_002` | `/basic-info` | `/basic-info` |
| 白名单外应用访问新页 | `[{itemCode:"app_001"}]` | `app_002` | `/basic-info-v2` | `/basic-info` |
| 白名单空（全量新） | `[]` | `app_001` | `/basic-info` | `/basic-info-v2` |
| 白名单空访问新页 | `[]` | `app_001` | `/basic-info-v2` | `/basic-info-v2` |
| 白名单多应用 | `[{itemCode:"app_001"},{itemCode:"app_002"}]` | `app_002` | `/basic-info` | `/basic-info-v2` |
| 未映射页面 | 任意 | 任意 | `/api-management` | `/api-management`（不变） |
| AppList 页面 | 任意 | 无 | `/` 或 `/app-list-v2` | 保持原路径（无 Sidebar） |
| 接口失败 | （请求失败 → 缓存为 `[]`） | `app_001` | `/basic-info` | `/basic-info-v2`（降级为全量新） |

---

**版本**：v4.1（新增 §18 新旧 UI 动态切换）
**最后更新**：2026-06-08
**配套文档**：spec.md v6.5 / plan.md v2.1 / demo-app-list.html / `front/doc/开发规范文档.md`
