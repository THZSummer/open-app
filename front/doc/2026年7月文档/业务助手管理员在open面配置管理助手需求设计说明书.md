# 需求设计说明书：业务助手管理员在 Open 面配置管理助手

## 修订记录

| 版本 | 日期 | 修订人 | 修订内容 |
|------|------|--------|---------|
| V1.0 | 2026-07-09 | - | 初始版本，创建需求设计说明书 |

## 目录
- 定位和写作说明
- 需求价值和概述
- 上下文分析
- 初始需求分析
    - 3.1 初始需求场景分析
    - 3.2 结构化IR（必选）
- 需求影响分析
    - 4.1 特性影响分析
- 系统用例分析
    - 5.1 用例清单
    - 5.2 用例分析
- 功能设计
    - 6.1 业界方案实现
    - 6.2 功能实现整体设计方案
    - 6.3 功能实现
- 系统级非功能设计
    - 7.1 系统级FMEA影响分析
    - 7.2 系统级安全影响分析
    - 7.3 兼容性
    - 7.4 可运维
    - 7.5 资料
- checkList（必填）
    - 8.1 设计自检清单要求（必填）

## 表目录
- 表1：wecodesite 动态能力菜单字段映射
- 表2：market-web 能力配置数据模型
- 表3：能力管理 API 接口清单
- 表4：能力配置字段校验规则
- 表5：用例清单
- 表6：功能实现分解分配清单

## 图目录
- 图1：系统上下文架构图
- 图2：wecodesite 动态加载流程时序图
- 图3：能力管理页面交互流程
- 图4：微前端动态注册架构图

## Keywords 关键字

中文：微前端、qiankun、动态注册、能力配置、业务助手、marketadmin、open-server、wecodesite、market-web
英文：Micro Frontend, qiankun, Dynamic Registration, Capability Configuration, Business Assistant, marketadmin, open-server, wecodesite, market-web

## Abstract 摘要

中文：本需求旨在通过微前端架构，实现业务助手管理配置的在线化与动态化。wecodesite 作为主应用（基座），通过 open-server 接口动态读取 marketadmin 中配置的应用能力信息，自动生成左侧菜单入口并加载对应的子应用页面（如助手广场），无需修改 wecodesite 代码即可扩展新的嵌入工程。market-web 作为管理端，提供应用能力的增删改查管理界面，支持配置能力的标题、描述、图标、示意图、排序和访问地址。最终实现运维效率提升、业务体验改善、团队维护成本降低的目标。

英文：This requirement aims to achieve online and dynamic configuration management of business assistants through micro-frontend architecture. wecodesite, as the main application (base), dynamically reads application capability configurations from marketadmin via open-server to automatically generate left sidebar menu entries and load corresponding sub-application pages (such as the Assistant Square). New embedded projects can be added without modifying wecodesite code. market-web, as the management portal, provides CRUD interfaces for capability configuration, supporting title, description, icon, schematic, sort order, and access URL configuration. The ultimate goal is to improve operational efficiency, enhance business experience, and reduce team maintenance costs.

## List 偶发 abbreviations 缩略语清单

| Abbreviations 缩略语 | Full spelling 英文全名 | Chinese explanation 中文解释 |
|---------------------|----------------------|---------------------------|
| IR | Initial Requirement | 初始需求 |
| US | User Story | 用户故事 |
| CRUD | Create, Read, Update, Delete | 增删改查 |
| UMD | Universal Module Definition | 通用模块定义 |
| ESM | ECMAScript Module | ECMAScript 模块 |
| CORS | Cross-Origin Resource Sharing | 跨域资源共享 |
| HMR | Hot Module Replacement | 热模块替换 |
| FMEA | Failure Mode and Effects Analysis | 失效模式与影响分析 |

## 1 需求价值和概述

### 背景

当前每天有 5~10 个业务助手方，需要修改助手生产配置、修改 UAT 配置进行联调验证。这些运营运维工作依赖人工支撑，存在以下问题：
- **支撑不及时**：运维人员需手动修改配置文件、重新部署，响应周期长
- **不准确**：人工操作易出错，配置遗漏或错误频发
- **业务体验不好**：业务方无法自助管理，需排队等待运维处理
- **团队维护成本高**：每次新增/修改助手能力都需要开发人员改代码、发版本

### 需求来源

内部优化需求，来源于业务助手运维团队的日常运维痛点。

### 需求价值

| 价值维度 | 具体说明 |
|---------|---------|
| 效率提升 | 业务方可通过 market-web 自助配置能力信息，无需运维介入，配置即时生效 |
| 释放运维人力 | 将重复性的配置修改工作从运维转移到业务方自助完成，运维团队聚焦平台稳定性 |
| 提升业务体验 | 业务方可自主管理助手能力展示，快速迭代验证，无需等待排期 |
| 降低维护成本 | 新增嵌入工程无需修改 wecodesite 代码，仅配置即可，减少发版频率和代码维护量 |
| 架构可扩展性 | 建立微前端动态注册机制，后续任何工程均可通过配置接入，架构具备长期演进能力 |

### 客户问题

如果没有该特性：
- 业务方每次修改助手配置都需提工单等待运维处理，平均等待 1-2 个工作日
- 新增助手能力需 wecodesite 开发修改代码、测试、发版，周期 3-5 天
- 运维人员被大量重复性配置工作占用，无法投入高价值工作
- 配置分散在代码和文档中，缺乏统一管理入口，出错后排查困难

---

## 2 上下文分析

### 系统上下文架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        业务管理员 / 运维人员                           │
│                                                                     │
│   ┌─────────────────────┐         ┌─────────────────────────┐       │
│   │   market-web        │         │   wecodesite            │       │
│   │  (管理端)            │         │  (主应用/基座)            │       │
│   │                     │         │                         │       │
│   │  - 能力列表管理      │         │  - 左侧动态菜单           │       │
│   │  - 能力增删改查      │  CRUD   │  - 微前端动态注册         │       │
│   │  - 图标/示意图上传   │         │  - 子应用挂载容器         │       │
│   │  - 字段校验         │         │  - 路由分发              │       │
│   └────────┬────────────┘         └───────────┬─────────────┘       │
│            │                                  │ 读取配置             │
│            ▼                                  ▼                     │
│   ┌─────────────────────┐    ┌──────────────────────────┐          │
│   │  marketadmin        │    │  open-server              │          │
│   │  (后端服务)          │    │  (开放服务)                │          │
│   │                     │    │                          │          │
│   │  - 能力配置 CRUD API │    │  - 能力配置查询接口        │          │
│   │  - 文件上传 API      │    │    (读取 marketadmin 配置) │          │
│   │  - 数据持久化        │    │                          │          │
│   └────────┬────────────┘    └────────────┬─────────────┘          │
│            │                              │                        │
│            │         ┌────────┐           │                        │
│            └────────>│ 数据库  │<──────────┘                        │
│                      └────────┘                                   │
│                  (marketadmin 与 open-server 共享同一数据库)          │
│                                                                     │
│                          qiankun 微前端                              │
│   ┌─────────────────────────────────────────────────────────┐       │
│   │              wecodesite #sub-app-viewport                │       │
│   │                                                         │       │
│   │   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │       │
│   │   │  助手广场     │  │  其他工程A    │  │  其他工程B    │ │       │
│   │   │  (子应用)     │  │  (子应用)     │  │  (子应用)     │ │       │
│   │   │              │  │              │  │              │ │       │
│   │   │ - 助手基本信息 │  │ - 业务页面    │  │ - 业务页面    │ │       │
│   │   │ - 技能管理    │  │              │  │              │ │       │
│   │   │ - 首页配置    │  │              │  │              │ │       │
│   │   └──────────────┘  └──────────────┘  └──────────────┘ │       │
│   └─────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────┘
```

**数据流转说明：**

marketadmin 与 open-server 读取的是同一个数据库，配置数据的流转路径如下：

1. **写入路径**：业务管理员在 market-web 中修改能力配置 → market-web 调用 marketadmin 后端服务的 CRUD API → marketadmin 将数据保存到数据库
2. **读取路径**：wecodesite 通过调用 open-server 的接口获取能力配置 → open-server 从数据库中读取 marketadmin 已保存的配置数据 → 返回给 wecodesite 用于动态渲染菜单和加载子应用

通过共享同一数据库，实现 market-web 中的配置修改经由 marketadmin 持久化后，wecodesite 可通过 open-server 实时读取最新配置，无需 marketadmin 与 open-server 之间直接通信。

### 利益相关方

| 利益相关方 | 关注点 | 期望 |
|-----------|--------|------|
| 业务管理员 | 能自助配置助手能力信息，配置后即时生效 | 无需运维介入，操作简单直观 |
| 运维人员 | 减少重复配置工作，聚焦平台稳定性 | 配置自动化，减少人工操作 |
| wecodesite 开发 | 新增嵌入工程无需改代码 | 动态注册机制，配置驱动 |
| 助手广场开发 | 页面能正确嵌入 wecodesite 展示 | 微前端接入规范清晰，样式隔离 |
| 前端架构 | 架构可扩展，后续工程可低成本接入 | 统一接入规范，配置即接入 |

---

## 3 初始需求分析

### 3.1 初始需求场景分析

| 所属场景 | 场景名称 | 场景简要说明 | 涉及角色 |
|---------|---------|------------|---------|
| 助手配置管理 | 能力配置列表查看 | 业务管理员在 market-web 中查看所有已配置的应用能力列表，支持分页和关键词搜索 | 业务管理员 |
| 助手配置管理 | 能力配置创建 | 业务管理员在 market-web 中新增应用能力配置，填写标题、描述、图标、示意图、排序、访问地址 | 业务管理员 |
| 助手配置管理 | 能力配置编辑 | 业务管理员修改已有能力配置，修改后即时生效 | 业务管理员 |
| 助手配置管理 | 能力配置删除 | 业务管理员删除不再需要的能力配置，删除后 wecodesite 菜单同步移除 | 业务管理员 |
| 助手配置管理 | 图标/示意图上传 | 业务管理员上传能力图标和示意图，支持预览和替换 | 业务管理员 |
| 微前端动态展示 | 菜单动态渲染 | wecodesite 启动时从 open-server 读取能力配置，动态生成左侧菜单入口 | 系统（自动） |
| 微前端动态展示 | 子应用动态加载 | 用户点击菜单项，wecodesite 根据 activeRule 通过 qiankun 加载对应子应用页面 | 业务管理员 |
| 微前端动态展示 | 新工程零代码接入 | 运维在 market-web 配置新工程能力信息，wecodesite 自动展示，无需改代码 | 运维人员 |

### 3.2 结构化IR（必选）

| IR 属性 | 具体信息 |
|---------|---------|
| IR 标识 | IR-2026-07-ASSISTANT-CONFIG |
| 名称 | 业务助手管理员在 Open 面配置管理助手 |
| 描述 | 基于 APPID 将企业应用和业务助手关联，在 market-web 提供能力配置管理界面，在 wecodesite 中动态嵌入展示助手广场等子工程页面，实现配置驱动、零代码扩展 |
| 优先级 | 高 |
| 需求描述（why） | 每天有 5~10 个业务助手方需修改生产/UAT 配置，人工运维支撑不及时、不准确，业务体验差，团队维护成本高。需通过在线配置管理替代人工操作，提升效率和体验 |
| what | 1. wecodesite 基于 APPID 关联企业应用和业务助手；2. market-web 提供能力的增删改查管理界面（标题、描述、图标、示意图、排序、访问地址）；3. wecodesite 动态读取配置，嵌入展示子工程页面；4. 后续新增嵌入工程仅需配置，无需改 wecodesite 代码 |
| who | 业务管理员（配置操作）、运维人员（新工程接入）、wecodesite 开发（动态注册机制）、助手广场开发（子应用接入） |
| 其他 | 助手广场相关内容在 wecodesite 中暂不关注，wecodesite 只关注如何嵌入助手广场相关页面 |
| 对架构要素的影响 | 前端架构（微前端动态注册）、安全（跨域资源加载）、可靠性（配置加载容错）、性能（子应用预加载） |

---

## 4 需求影响分析

### 4.1 特性影响分析

**【新增】**

| 特性名称 | 说明 |
|---------|------|
| 应用能力配置管理（market-web） | 新增能力列表、新增/编辑/删除能力的完整 CRUD 管理界面，含图标和示意图上传 |
| 能力配置 API（marketadmin） | 新增能力配置的 CRUD 接口和文件上传接口 |
| 能力配置数据模型（marketadmin） | 新增能力配置数据表，存储标题、描述、图标、示意图、排序、访问地址等字段 |
| 微前端动态加载（wecodesite） | 新增从 API 动态读取能力配置、通过 loadMicroApp 动态加载 qiankun 子应用、动态生成菜单的机制 |

**【修改】**

| 特性名称 | 修改说明 |
|---------|---------|
| wecodesite 左侧导航菜单 | Sidebar 当前已从 microApps.js 动态渲染微前端应用菜单，需改为从 API 配置动态渲染；已订阅能力菜单已从 API 动态加载，保持不变 |
| wecodesite 子应用注册 | 当前通过主动调用 loadMicroApp 加载子应用（在 Layout.jsx 中），配置来源为静态 microApps.js，需改为从 API 异步获取配置后仍通过主动调用 loadMicroApp 加载 |

**【删除】**

无删除项。

---

## 5 系统用例分析

### 5.1 用例清单

| 角色名称 | UseCase 名称 | UseCase 简要说明 | 是否需要细化分析 |
|---------|-------------|-----------------|----------------|
| 业务管理员 | UC-01 查看能力列表 | 在 market-web 中查看所有已配置的应用能力列表 | 是 |
| 业务管理员 | UC-02 新增能力配置 | 在 market-web 中填写能力信息并保存，配置后即时生效 | 是 |
| 业务管理员 | UC-03 编辑能力配置 | 修改已有能力配置信息，保存后即时生效 | 是 |
| 业务管理员 | UC-04 删除能力配置 | 删除不再需要的能力配置，二次确认后删除 | 是 |
| 业务管理员 | UC-05 上传图标/示意图 | 上传能力图标和示意图文件，支持预览和替换 | 是 |
| 业务管理员 | UC-06 查看嵌入页面 | 在 wecodesite 中点击菜单项，查看嵌入的子应用页面 | 是 |
| 系统 | UC-07 动态加载能力配置 | wecodesite 启动时从 open-server API 获取能力配置，通过主动调用 loadMicroApp 加载子应用 | 是 |
| 运维人员 | UC-08 新增工程接入 | 在 market-web 配置新工程能力信息，wecodesite 自动展示 | 是 |

### 5.2 用例分析

#### UC-02 新增能力配置

**【简要说明】** 业务管理员在 market-web 中通过弹窗表单填写能力信息（标题、描述、图标、示意图、排序、访问地址），保存后配置即时生效，wecodesite 刷新后展示新能力菜单。

**【Actor】** 业务管理员

**【前置条件】**
1. 业务管理员已登录 market-web
2. 业务管理员拥有能力配置管理权限
3. 后端 marketadmin 服务正常运行

**【最小保证】** 表单提交失败时，保留用户填写的数据，显示明确的错误提示信息，不影响已有配置。

**【成功保证】** 配置保存成功后，能力列表中新增一条记录，记录操作账号和更新时间。wecodesite 刷新后左侧菜单出现对应入口。

**【主成功场景】**
1. 业务管理员进入"应用能力设置"页面
2. 点击"添加能力"按钮
3. 系统弹出新增能力弹窗表单
4. 业务管理员填写标题（2~30字符）、描述（5~200字符）
5. 业务管理员上传图标（40x40，不超过200K）
6. 业务管理员上传示意图（520x288，不超过500K，非必填）
7. 业务管理员填写排序（正整数，大于1）
8. 业务管理员填写访问地址（合法 http/https 地址，不超过1000字符）
9. 业务管理员点击保存
10. 系统校验所有字段，校验通过后提交到后端
11. 后端保存成功，返回成功响应
12. 系统关闭弹窗，刷新能力列表，新能力显示在列表中

**【扩展场景】**
- 4a. 标题不满足校验规则（为空或长度不符）：表单字段下方显示红色错误提示，阻止提交
- 5a. 图标未上传：显示"图标必传"错误提示，阻止提交
- 5b. 图标尺寸或大小不符：显示具体的尺寸/大小限制提示，阻止提交
- 6a. 示意图尺寸或大小不符：显示具体的尺寸/大小限制提示，阻止提交
- 7a. 排序值不合法（非正整数或小于1）：显示错误提示，阻止提交
- 8a. 访问地址格式不合法：显示"请输入合法的 http 或 https 地址"提示，阻止提交
- 10a. 后端保存失败（网络异常/服务异常）：显示"保存失败，请重试"提示，保留表单数据
- 10b. 排序值与已有能力重复：后端允许重复，按创建时间次序展示

#### UC-07 动态加载能力配置

**【简要说明】** wecodesite 当前在 Layout.jsx 中主动调用 `loadMicroApp` 加载子应用，配置来源为静态 `microApps.js` 文件，使用 HashRouter 路由模式。需改为从 open-server API 异步获取能力配置列表，将配置传入 Layout.jsx 中的 `loadMicroApp` 逻辑，替换静态 `microApps.js` 数据源。同时根据配置动态渲染左侧菜单。

**【Actor】** 系统（wecodesite 前端）

**【前置条件】**
1. wecodesite 前端已加载 qiankun 依赖
2. wecodesite 使用 HashRouter 路由模式，子应用 activeRule 需基于 location.hash 匹配
3. open-server 服务正常运行，能力配置查询接口可访问
4. 配置中的子应用（如助手广场）已部署且可访问

**【最小保证】** API 请求失败时，wecodesite 展示基础功能页面（自有页面），左侧菜单显示加载失败提示，不影响主应用自身功能。

**【成功保证】** wecodesite 启动后，左侧菜单根据配置动态生成，点击菜单项可正确加载对应子应用页面。

**【主成功场景】**
1. wecodesite 页面加载，渲染主应用基础布局（头部、侧边栏骨架、内容区）
2. wecodesite 调用 open-server API 获取能力配置列表
3. API 返回能力配置数组（按 sortOrder 排序）
4. wecodesite 将配置数组转换为两个部分：
   - 菜单数据：渲染左侧导航菜单（标题、图标、路由路径）
   - 子应用注册数据：转换为 qiankun 的 `{ name, entry, container, activeRule }` 格式
5. Layout.jsx 中主动调用 `loadMicroApp` 使用 API 返回的配置数据（替换静态 microApps.js）加载对应子应用
6. 左侧菜单渲染完成，显示所有已配置的能力入口
7. 用户点击菜单项，hash 路由变化触发 Layout useEffect 重新匹配，通过 `loadMicroApp` 加载对应子应用到 `#sub-app-viewport`

**【扩展场景】**
- 2a. API 请求超时（超过 5s）：显示"配置加载超时"提示
- 2b. API 返回空列表：左侧菜单仅显示 wecodesite 自有功能入口，不注册任何子应用
- 2c. API 返回错误（非 200）：显示"配置加载失败"提示，不影响主应用自身页面渲染
- 5a. 子应用 entry 不可达（子应用未部署）：菜单项正常显示，点击时报错并提示"页面加载失败"

#### UC-08 新增工程接入

**【简要说明】** 运维人员在 market-web 中配置新工程的能力信息（标题、描述、图标、访问地址等），保存后 wecodesite 刷新即可展示新工程页面，无需修改 wecodesite 代码。

**【Actor】** 运维人员

**【前置条件】**
1. 新工程已完成微前端子应用接入改造（导出 qiankun 生命周期函数）
2. 新工程已部署且可访问
3. 运维人员拥有 market-web 能力配置管理权限

**【成功保证】** 配置保存后，wecodesite 刷新页面，左侧菜单出现新工程入口，点击可正确加载新工程页面。

**【主成功场景】**
1. 运维人员确认新工程已部署，获取访问地址
2. 运维人员在 market-web 中新增能力配置，填写标题、描述、图标、排序、访问地址
3. 保存配置
4. 运维人员（或业务用户）刷新 wecodesite 页面
5. wecodesite 从 API 获取最新配置，新工程出现在左侧菜单
6. 点击菜单项，新工程页面通过 qiankun 加载到 wecodesite 中展示

---

## 6 功能设计

### 6.1 业界方案实现

业界微前端动态注册方案对比：

| 方案 | 实现方式 | 优点 | 缺点 | 是否采用 |
|------|---------|------|------|---------|
| qiankun manual loadMicroApp + 静态配置（当前现状） | 在 Layout.jsx 中主动调用 loadMicroApp 加载，配置来自静态 microApps.js | 灵活控制加载时机，已验证可用 | 配置静态硬编码，新增子应用需改代码 | 否（当前现状，需改造） |
| qiankun manual loadMicroApp + API 配置 | 保持 loadMicroApp 模式，配置源改为 API 异步获取 | 改动量最小，复用现有已验证逻辑；配置驱动，零代码扩展 | 无预加载能力；需手动管理生命周期 | **是** |
| qiankun 动态注册 | 从 API 获取配置后调用 registerMicroApps + start | 配置驱动；qiankun 自动管理生命周期和路由匹配 | 需从 loadMicroApp 迁移；HashRouter 下 activeRule 处理复杂 | 否 |
| iframe 嵌入 | 使用 iframe 加载子页面 | 天然隔离，简单 | 交互受限、性能差、UX 差 | 否 |
| Module Federation | Webpack 5 模块联邦 | 编译时集成，性能好 | 强依赖 Webpack 5，子应用改造成本高 | 否 |

**选型说明：** 采用 `loadMicroApp` + API 配置方案。wecodesite 当前已在 Layout.jsx 中主动调用 `loadMicroApp` 加载子应用，配置来源为静态 `microApps.js`。仅需将配置源从静态文件改为 API 动态获取，保持主动调用 `loadMicroApp` 加载模式不变，改动量最小且基于已验证的代码。满足"后续新增工程仅需配置"的核心需求。

### 6.2 功能实现整体设计方案

#### 6.2.1 整体方案

**设计原则：**
1. **配置驱动**：所有能力信息通过 marketadmin API 管理，wecodesite 通过 open-server API 动态读取
2. **零代码扩展**：新增嵌入工程仅需在 market-web 配置，wecodesite 无需改代码
3. **渐进增强**：API 加载失败时降级展示主应用基础功能，不影响核心体验
4. **样式隔离**：通过 qiankun 沙箱 `experimentalStyleIsolation` 实现主子应用样式隔离
5. **技术栈无关**：子应用可为 React、Vue 等任意技术栈，通过 qiankun 统一接入

**设计约束：**
1. wecodesite 作为主应用，必须首先从 API 获取能力配置后，通过主动调用 `loadMicroApp` 加载子应用
2. wecodesite 使用 HashRouter 路由模式，子应用 activeRule 需为基于 location.hash 的匹配函数
3. 子应用必须按 qiankun 规范导出 `bootstrap`、`mount`、`unmount` 生命周期函数
4. 子应用必须配置 CORS 允许主应用跨域拉取资源
5. 能力配置 API 必须支持按 sortOrder 排序返回

#### 6.2.2 架构设计方案

##### 逻辑视图

```
┌─────────────────────────────────────────────────────────────┐
│                    wecodesite (主应用)                        │
│                                                             │
│  ┌─────────────┐  ┌──────────────────────────────────────┐ │
│  │ 配置加载模块  │  │ Layout 组件 (父路由 path="/")          │ │
│  │             │  │                                      │ │
│  │ 1.请求API   │  │ ┌──────────────────────────────────┐ │ │
│  │ 2.转换配置   │──>│ │ Header + AppInfoBar (顶部)       │ │ │
│  │ 3.传入Layout │  │ ├────────┬─────────────────────────┤ │ │
│  │  (loadMicroApp)│ │ Sider  │  Content (右侧主区域)     │ │ │
│  └─────────────┘  │ │(左侧   │  ┌─────────────────────┐ │ │ │
│                   │ │ 菜单   │  │  <Outlet />         │ │ │ │
│  ┌─────────────┐  │ │ 240px) │  │  ┌───────────────┐  │ │ │ │
│  │ React Router │  │ │        │  │  │自有页面路由匹配 │  │ │ │ │
│  │ (HashRouter) │  │ │ 菜单项1 │  │  │ appList等     │  │ │ │ │
│  │ qiankun/*    │  │ │ 菜单项2 │  │  └───────────────┘  │ │ │ │
│  │ element=null │  │ │ 菜单项N │  │  或                  │ │ │ │
│  │ path="*"     │  │ │        │  │  ┌───────────────┐  │ │ │ │
│  │ -> Navigate   │  │ │        │  │  │#sub-app-viewport│ │ │ │ │
│  └─────────────┘  │ │        │  │  │(条件渲染，     │ │ │ │ │
│                   │ │        │  │  │ qiankun加载)   │ │ │ │ │                   │ │        │  │  └───────────────┘  │ │ │ │
│                   │ │        │  └─────────────────────┘ │ │ │
│                   │ └────────┴─────────────────────────┘ │ │
│                   └──────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
         │ API请求(读取配置)                      │ fetch entry
         ▼                                      ▼
┌─────────────────┐              ┌──────────────────────────┐
│  open-server    │              │  子应用 (如: 助手广场)     │
│                 │              │                          │
│  /api/capabilities│            │  导出生命周期:            │
│  (查询接口)      │              │  bootstrap/mount/unmount │
│                  │              │  CORS: *                 │
│  读取数据库      │              │  UMD/ESM 格式            │
└────────┬────────┘              └──────────────────────────┘
         │
         │  共享
         ▼
┌─────────────────┐
│  数据库          │
│  (共享)          │
└────────▲────────┘
         │
         │ CRUD + 持久化
┌─────────────────┐
│  marketadmin    │
│  (后端服务)      │
│                  │
│  /api/capabilities│
│  /api/upload     │
└────────▲────────┘
         │ CRUD
┌─────────────────┐
│  market-web     │
│  (管理端)        │
│                 │
│  能力列表页      │
│  新增/编辑弹窗   │
│  文件上传        │
└─────────────────┘
```

##### 开发视图

```
open-app/
├── wecodesite/                    # 主应用（基座）
│   └── src/
│       ├── main.jsx               # 入口：当前仅 React 渲染（HashRouter），需新增异步加载配置并传入 App
│       ├── App.jsx                # 路由：当前含 qiankun/* 路由 + 通配符重定向，无需修改
│       ├── microApps.js           # 当前静态子应用配置（loadMicroApp 使用），改为从 API 动态获取后此文件可废弃
│       ├── components/
│       │   ├── Layout/
│       │   │   ├── Layout.jsx     # 布局：保留 loadMicroApp 逻辑 + 条件渲染 #sub-app-viewport，配置源改为 API
│       │   │   └── Sidebar/
│       │   │       └── Sidebar.jsx  # 侧边栏：已从 microApps.js 动态渲染菜单，需改为从 API 配置渲染
│       │   └── DynamicMenu.jsx    # 动态菜单组件（根据 API 配置渲染，替代现有 Sidebar 中的 microApps.map）
│       ├── pages/
│       │   └── MicroAppContainer/
│       │       └── index.jsx      # 子应用容器页面：渲染 #sub-app-viewport + loading/error + 404重定向
│       ├── services/
│       │   └── capabilityApi.js   # 能力配置 API 请求模块
│       └── utils/
│           └── microAppHelper.js  # 配置转换工具（API数据 -> qiankun注册格式 + 菜单数据，含 hash activeRule 生成）
│
├── market-web/                    # 管理端（React + Vite + Ant Design）
│   ├── index.html                 # HTML 入口模板
│   ├── vite.config.ts             # Vite 构建配置
│   ├── tsconfig.json              # TypeScript 配置
│   └── src/
│       ├── main.tsx               # 应用入口：挂载 React 根组件
│       ├── App.tsx                # 根组件：BrowserRouter + 路由配置
│       ├── index.css              # 全局样式
│       ├── components/
│       │   └── Layout/
│       │       ├── index.js       # 布局组件：Sider 侧边栏 + Header 顶栏 + Content 内容区
│       │       └── index.module.less  # 布局样式
│       ├── router/
│       │   ├── index.tsx          # 路由总入口：声明所有路由（含能力管理路由）
│       │   └── routeRedBlue/
│       │       └── capability-config-manage/  # 能力配置管理模块（新增）
│       │           ├── index.js         # 能力配置列表页主组件
│       │           ├── index.module.less # 列表页样式
│       │           ├── route.js         # 能力配置路由配置
│       │           ├── thunk.js         # 能力配置 CRUD API 请求函数
│       │           ├── constant.js      # 表格列配置、校验规则、常量定义
│       │           └── components/
│       │               ├── CapabilityFormModal.js        # 新增/编辑能力弹窗表单
│       │               ├── CapabilityFormModal.module.less # 弹窗表单样式
│       │               ├── IconUpload.js                 # 图标上传组件（40x40，200K）
│       │               ├── SchematicUpload.js            # 示意图上传组件（520x288，500K）
│       ├── configs/
│       │   └── web.config.js      # API 路径配置（新增能力管理相关接口路径）
│       ├── utils/
│       │   ├── webFetch.js        # 统一 API 请求封装（fetchApi + buildApiUrl）
│       │   ├── common.js          # 通用工具函数（renderAlwaysWithTooltip 等）
│       │   └── validators.js      # 表单校验规则工具（URL 校验、文件校验等）（新增）
│       └── stores/
│           └── global.store.ts    # 全局状态管理（用户信息、侧边栏折叠等）
│
└── marketadmin/                   # 后端服务
    ├── controller/
    │   └── CapabilityController   # 能力配置 CRUD 接口
    ├── service/
    │   └── CapabilityService      # 能力配置业务逻辑
    ├── model/
    │   └── Capability.js          # 能力配置数据模型
    └── routes/
        └── capability.js          # 能力配置路由
```

##### 运行视图

```
用户浏览器
    │
    ├── 1. 加载 wecodesite 静态资源 (HTML/JS/CSS)
    │
    ├── 2. wecodesite 初始化
    │   ├── 2.1 渲染基础布局（头部、侧边栏骨架）
    │   ├── 2.2 异步请求 GET /api/capabilities
    │   ├── 2.3 等待 API 响应（显示 loading 状态）
    │   └── 2.4 收到配置数据
    │
    ├── 3. 配置就绪
    │   ├── 3.1 转换配置 -> loadMicroApp 所需格式（含基于 hash 的 menuKey 匹配）
    │   ├── 3.2 配置数据传入 Layout.jsx，供 loadMicroApp 使用
    │   └── 3.3 渲染左侧动态菜单
    │
    ├── 4. 用户点击菜单项
    │   ├── 4.1 hash 路由变化 (如 /#/qiankun/assistant-square)
    │   ├── 4.2 Layout useEffect 检测路由变化，匹配 API 配置
    │   ├── 4.3 loadMicroApp 加载对应子应用 entry 资源
    │   └── 4.4 子应用 mount 到 #sub-app-viewport
    │
    └── 5. 子应用卸载
        └── 离开子应用路由时，Layout useEffect cleanup 调用 handle.unmount()
```

### 6.3 功能实现

#### 6.3.1 实现思路

**wecodesite 动态注册实现思路：**

1. **异步初始化**：wecodesite 入口文件（main.jsx）当前仅做 React 渲染，qiankun 子应用加载逻辑位于 Layout.jsx 中，通过主动调用 `loadMicroApp` 加载子应用，配置来源为静态 `microApps.js`。需改为异步流程：先请求 API 获取能力配置，将配置数据传入 App/Layout，替换静态 `microApps.js` 作为 `loadMicroApp` 的数据源。保持主动调用 `loadMicroApp` 加载模式不变。

2. **配置转换**：API 返回的能力配置字段需映射为 qiankun 注册格式。映射关系如下：

   **表1：wecodesite 动态能力菜单字段映射**

   | API 返回字段 | qiankun 注册字段 | 菜单渲染字段 | 说明 |
   |-------------|-----------------|-------------|------|
   | id | name（或派生） | key | 子应用唯一标识 |
   | accessUrl | entry | - | 子应用资源入口地址 |
   | - | container | - | 固定为 `#sub-app-viewport` |
   | - | activeRule（派生） | path | 路由激活规则，wecodesite 使用 HashRouter，需生成基于 location.hash 的匹配函数 |
   | title | - | label | 菜单显示名称 |
   | iconUrl | - | icon | 菜单图标 |
   | description | - | - | 菜单 tooltip（可选） |
   | sortOrder | - | - | 菜单排序依据 |
   | routePath | - | menuKey | 菜单高亮 key，当前 microApps.js 中为 `qiankun/xxx` 格式 |

3. **activeRule / menuKey 匹配策略**：wecodesite 使用 HashRouter，URL 形式为 `/#/qiankun/xxx`。当前 `loadMicroApp` 模式不依赖 qiankun 的 activeRule 自动匹配，而是由 Layout.jsx 中的 `useEffect` 根据当前路由 `location.pathname` 匹配 `microApps` 配置的 `menuKey`，主动调用 `loadMicroApp` 加载子应用。当前 `microApps.js` 中 `menuKey` 为 `qiankun/xxx` 格式，Layout 通过 `location.pathname.startsWith('/' + app.menuKey)` 匹配。迁移后由后端配置提供 routePath 字段（如 `/qiankun/assistant-square`），前端生成对应的 menuKey 供 Layout 匹配。

4. **菜单动态渲染**：左侧导航菜单中微前端应用部分当前已从 `microApps.js` 动态 `map` 渲染（位于 Sidebar.jsx），需改为根据 API 返回数据渲染。Sidebar 中已有「已订阅能力」菜单从 API 动态加载的逻辑（`fetchSubscribedAbilities`），保持不变。

5. **子应用容器页面设计（关键）**：wecodesite 需要一个统一的子应用挂载容器页面，确保所有子应用路由都能正确加载渲染到右侧主内容区域，且新增子应用时无需修改 wecodesite 路由代码。

   **问题背景**：wecodesite 布局为「顶部两个 Header + 左侧菜单栏 + 右侧主内容展示区域」。用户点击菜单项后，子应用页面应加载到**右侧主内容区域**内，同时保持 wecodesite 的完整布局（Header + Sidebar 不消失）。wecodesite 使用 HashRouter，现有路由配置中已有 `<Route path="qiankun/*" element={null} />` 路由匹配子应用路径但不渲染内容，同时存在通配符路由 `<Route path="*" element={<Navigate to="/appList" replace />} />` 对其他未匹配路径做重定向。

   **wecodesite 现有路由结构分析**：

   ```jsx
   // App.jsx 现有结构
   <Routes>
     <Route path="/" element={<Layout />}>     {/* Layout 始终渲染，含 Header + Sidebar + Content */}
       <Route path="appList" element={<AppList />} />
       <Route path="basic-info" element={<BasicInfo />} />
       ...（自有页面路由）
       {/* 微前端子应用统一路由（qiankun 挂载到 #sub-app-viewport，element 为 null） */}
       <Route path="qiankun/*" element={null} />
       <Route path="*" element={<Navigate to="/appList" replace />} />  {/* 通配符路由：未匹配路径重定向 */}
     </Route>
   </Routes>
   ```

   ```jsx
   // Layout.jsx 现有结构
   <AntLayout>
     {!isAdminPage && <Header />}           {/* 顶部 Header，仅非 admin 页面显示 */}
     {isDetailPage && <AppInfoBar />}       {/* 顶部第二条，仅详情页面显示 */}
     <AntLayout>
       {isDetailPage && (
         <Sider width={240}>                {/* 左侧菜单 240px，仅详情页面显示 */}
           <Sidebar />
         </Sider>
       )}
       <Content>                            {/* 右侧主内容区域 */}
         {/* 微前端子应用页面：隐藏 Outlet，显示 qiankun 挂载容器 */}
         {isMicroAppPage ? (
           <div id="sub-app-viewport" />    {/* qiankun 子应用挂载容器 */}
         ) : (
           <div>
             <Outlet />                     {/* React Router 路由出口，自有页面在此渲染 */}
           </div>
         )}
       </Content>
     </AntLayout>
   </AntLayout>
   ```
   注：Layout.jsx 中包含主动调用 `loadMicroApp` 加载子应用的逻辑（useEffect 中根据当前路由匹配 microApps 配置，调用 loadMicroApp 加载子应用，离开时 unmount），保持此逻辑不变，仅将 microApps 数据源改为 API 配置。

   **关键约束**：wecodesite 使用 React Router v6 嵌套路由 + HashRouter，`<Layout>` 作为父路由组件（`path="/"`），自有页面路由通过 `<Outlet />` 渲染到 Content 区域内。Layout 组件根据路由条件渲染 Header、AppInfoBar、Sider（仅详情页面显示 Sider）。当前微前端子应用页面（`/qiankun/*`）通过条件渲染 `#sub-app-viewport` 容器替代 `<Outlet />`，并由 Layout 中的 `loadMicroApp` 在 useEffect 中主动加载子应用。

   **方案对比**：

   | 方案 | 实现方式 | 优点 | 缺点 |
   |------|---------|------|------|
   | A. 现有方案优化（Layout loadMicroApp + API 配置） | 保持 Layout.jsx 中主动调用 `loadMicroApp` 加载逻辑和条件渲染 `#sub-app-viewport` 的现有逻辑，配置源从静态 `microApps.js` 改为 API；保留 `qiankun/*` 路由 | 改动量最小，复用现有已验证逻辑；`#sub-app-viewport` 已在 Layout 中条件渲染；`qiankun/*` 路由已存在；useEffect 加载/卸载逻辑保持不变 | 无预加载能力；需手动管理生命周期（现有逻辑已实现） |
   | B. 迁移到 registerMicroApps + start | 从 loadMicroApp 迁移到 registerMicroApps 自动注册，移除 Layout 中的 useEffect 主动加载逻辑 | qiankun 自动管理生命周期和路由匹配；支持 prefetch 预加载 | 迁移成本高；HashRouter 下 activeRule 需改为函数形式；需移除现有已验证的 loadMicroApp 逻辑 |
   | C. 动态路由生成 | 根据 API 配置动态生成 `<Route>` | 路由最精确 | 需异步渲染路由，复杂度高；每次 API 更新需重新生成路由表 |

   **推荐方案 A（保持 loadMicroApp 模式，配置源改为 API）**：

   **理由**：wecodesite Layout.jsx 中已实现主动调用 `loadMicroApp` 加载子应用的完整逻辑（useEffect 中根据路由匹配配置、加载子应用、离开时 unmount），条件渲染 `#sub-app-viewport` 容器也已就绪，`qiankun/*` 路由已存在。仅需将 `microApps` 数据源从静态 `microApps.js` 改为 API 动态获取，改动量最小且基于已验证的代码。

   **改造点（一次性，后续新增子应用无需再改）**：

   1. **main.jsx 改造**：新增异步加载配置流程，启动时请求 API 获取能力配置，将配置数据通过 props 或 context 传入 App/Layout

   2. **Layout.jsx 改造**：
      - 移除 `import microApps from '../../microApps'` 静态导入，改为接收 API 传入的配置数据
      - 保留 `loadMicroApp` 的 useEffect 逻辑不变，仅将 microApps 变量替换为 API 配置数据
      - 保留条件渲染 `#sub-app-viewport` 的现有逻辑

   3. **App.jsx**：保留现有 `qiankun/*` 路由和通配符路由，无需修改

   4. **Sidebar.jsx 改造**：将微前端应用菜单的数据源从 `microApps.js` 改为 API 配置

   **运行机制说明**：

   - **两套独立路由系统并行工作，互不干扰**：
     - wecodesite 的 React Router（HashRouter）负责渲染 Layout 布局和匹配自有页面路由
     - Layout 的 useEffect 监听路由变化，主动调用 `loadMicroApp` 加载匹配的子应用

   - **访问自有页面（如 `/appList`）的流程**：
     1. HashRouter 匹配 `<Route path="appList">` -> `<Outlet />` 渲染 AppList 到 Content 区域
     2. Layout useEffect 检测到非 `/qiankun/` 路由 -> 不加载子应用
     3. 用户看到完整的 wecodesite 布局 + AppList 页面

   - **访问子应用页面（如 `/#/qiankun/assistant-square`）的流程**：
     1. HashRouter 匹配 `<Route path="qiankun/*" element={null}>` -> 不渲染页面内容
     2. Layout 检测到 `isMicroAppPage` -> 条件渲染 `#sub-app-viewport` 容器
     3. Layout useEffect 匹配到 API 配置中的子应用 -> `loadMicroApp` 加载子应用资源 -> 执行 mount -> 子应用渲染到 `#sub-app-viewport`
     4. 用户看到完整的 wecodesite 布局 + 右侧 Content 区域内显示子应用页面

   - **离开子应用页面的流程**：
     1. hash 路由变化，离开 `/qiankun/` 前缀
     2. Layout useEffect cleanup -> `handle.unmount()` 卸载子应用
     3. 条件渲染切回 `<Outlet />`

   - **新增子应用时的流程（零代码修改）**：
     1. 运维在 market-web 配置新能力（含 accessUrl、routePath）
     2. wecodesite 启动时从 API 获取配置 -> 配置数据传入 Layout
     3. 用户点击新菜单项 -> hash 路由变化 -> React Router 匹配 `qiankun/*` -> Layout 条件渲染 `#sub-app-viewport` -> useEffect 匹配新配置 -> `loadMicroApp` 加载新子应用
     4. **无需在 App.jsx 中添加任何 `<Route>`**

6. **容错处理**：API 请求失败时降级展示主应用基础功能；单条配置数据异常时跳过该条，不影响其他配置。

**market-web 能力管理实现思路：**

1. **列表页**：表格形式展示所有能力配置，表格列包括序号、能力名称（图标+标题+描述）、访问地址（超长省略+tooltip）、示意图（缩略图）、操作账号、更新时间、操作（编辑/删除）。

2. **新增/编辑弹窗**：复用同一表单组件，编辑时回填已有数据。表单字段：标题、描述、图标上传、示意图上传、排序、访问地址、路由路径。

3. **文件上传**：图标和示意图通过独立的上传接口上传，上传成功后返回文件 URL，表单中存储 URL 而非文件本身。上传前进行尺寸和大小校验。

4. **字段校验**：前端实时校验 + 后端二次校验，确保数据合法性。

##### market-web 前端详细设计

###### 前端路由设计

能力管理页面接入 market-web 现有路由体系，遵循 `routeRedBlue/[模块名]/` 目录约定：

| 路由路径 | 页面组件 | 说明 |
|---------|---------|------|
| `/capability-config-manage` | `routeRedBlue/capability-config-manage/index.js` | 能力配置列表页，展示所有能力配置 |

**路由注册方式：**

在 `src/router/index.tsx` 中新增路由声明：

```jsx
// 能力配置管理
import CapabilityConfigManage from './routeRedBlue/capability-config-manage';

const Router = () => {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Welcome />} />
        {/* ...已有路由... */}
        <Route path="capability-config-manage" element={<CapabilityConfigManage />} />
        <Route path="404" element={<NotFound />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Route>
    </Routes>
  );
};
```

在 `src/components/Layout/index.js` 的 `menuItems` 中新增侧边栏菜单项：

```jsx
const menuItems = [
  // ...已有菜单...
  {
    key: '/capability-config-manage',
    icon: <ThunderboltOutlined />,
    label: '能力配置管理',
  },
];
```

###### 前端文件功能说明

**1. `router/routeRedBlue/capability-config-manage/index.js`（能力配置列表页主组件）**

| 功能项 | 说明 |
|--------|------|
| 列表数据加载 | 页面挂载时调用 `getCapabilityList` 获取能力列表，按 sortOrder 排序展示 |
| 搜索筛选 | 支持按能力标题关键词搜索，输入后实时过滤或点击搜索按钮触发 |
| 分页 | 支持分页展示，每页 20 条，可切换页码 |
| 新增能力入口 | 点击「添加能力」按钮，打开 CapabilityFormModal 弹窗（新增模式） |
| 编辑能力 | 点击表格行操作列「编辑」按钮，打开 CapabilityFormModal 弹窗（编辑模式），回填当前行数据 |
| 删除能力 | 点击「删除」按钮，弹出二次确认弹窗（复用 ConfirmModal），确认后调用删除接口 |
| 示意图预览 | 点击表格中示意图缩略图，弹出 SchematicPreview 大图预览弹窗 |
| 状态展示 | 表格中状态列用 Tag 标签展示（启用-绿色 / 禁用-红色） |
| loading 状态 | 数据加载时表格显示 loading 动画；提交时按钮显示 loading |
| 错误提示 | API 请求失败时通过 `message.error` 展示错误信息 |

**2. `router/routeRedBlue/capability-config-manage/thunk.js`（API 请求模块）**

| 函数名 | 请求方法 | API 路径 | 说明 |
|--------|---------|---------|------|
| `getCapabilityList` | GET | CAPABILITY_LIST | 获取能力列表（分页+关键词搜索） |
| `getCapabilityDetail` | GET | CAPABILITY_DETAIL | 获取单条能力详情（编辑回填用） |
| `createCapability` | POST | CAPABILITY_CREATE | 新增能力配置 |
| `updateCapability` | PUT | CAPABILITY_UPDATE | 编辑能力配置 |
| `deleteCapability` | DELETE | CAPABILITY_DELETE | 删除能力配置 |
| `uploadIcon` | POST | CAPABILITY_UPLOAD_ICON | 上传图标文件，返回 URL |
| `uploadSchematic` | POST | CAPABILITY_UPLOAD_SCHEMATIC | 上传示意图文件，返回 URL |

所有函数均使用 `fetchApi` 封装，与现有 `lookup-classify/thunk.js` 保持一致的代码风格。

**3. `router/routeRedBlue/capability-config-manage/constant.js`（常量与配置）**

| 内容 | 说明 |
|------|------|
| `DEFAULT_SEARCH_VALUES` | 搜索表单默认值 |
| `MODAL_TITLE_ADD` / `MODAL_TITLE_EDIT` | 弹窗标题常量 |
| `FORM_VALIDATION_RULES` | 表单校验规则（title、description、sortOrder、accessUrl、routePath） |
| `getTableColumns()` | 表格列配置函数，返回 Ant Design Table 的 columns 数组，包含序号列、能力名称列（图标+标题+描述自定义渲染）、访问地址列（ellipsis+tooltip）、示意图列（缩略图自定义渲染）、排序列、状态列（Tag 渲染）、操作账号列、更新时间列、操作列（编辑/删除按钮） |

**4. `components/CapabilityFormModal.js`（新增/编辑弹窗表单）**

| 功能项 | 说明 |
|--------|------|
| 表单字段 | 能力标题（Input）、能力描述（TextArea）、能力图标（IconUpload）、能力示意图（SchematicUpload，非必填）、排序值（InputNumber）、访问地址（Input）、路由路径（Input） |
| 新增/编辑复用 | 通过 `editingId` 区分模式；编辑模式时 `useEffect` 回填 `initialValues` |
| 表单校验 | 使用 Ant Design Form `rules` 配置，提交时 `form.validateFields()` 触发校验 |
| 实时预览 | 表单右侧展示预览卡片，实时反映标题、描述、图标、示意图的填写效果 |
| 提交逻辑 | 校验通过后调用 `onSubmit(values)` 回调，父组件根据模式调用 create/update API |
| 关闭逻辑 | 取消或提交成功后 `form.resetFields()` 清空表单，关闭弹窗 |

**5. `components/IconUpload.js`（图标上传组件）**

| 功能项 | 说明 |
|--------|------|
| 上传区域 | 80x80 的虚线框上传区域，点击触发文件选择 |
| 文件类型校验 | 仅允许 PNG / JPEG / SVG（`accept="image/png,image/jpeg,image/svg+xml"`） |
| 文件大小校验 | 不超过 200KB，超出时 `message.error` 提示 |
| 图片尺寸校验 | 上传前通过 `Image` 对象读取图片尺寸，校验是否为 40x40 |
| 预览 | 上传成功后在区域内显示图片预览 |
| 替换 | 已有图标时点击可重新上传替换 |
| URL 存储 | 上传成功后将返回的 URL 通过 `onChange` 回调传给父表单 |

**6. `components/SchematicUpload.js`（示意图上传组件）**

| 功能项 | 说明 |
|--------|------|
| 上传区域 | 130x72 的虚线框上传区域（与示意图比例 520:288 一致） |
| 文件类型校验 | 仅允许 PNG / JPEG（`accept="image/png,image/jpeg"`） |
| 文件大小校验 | 不超过 500KB |
| 图片尺寸校验 | 校验是否为 520x288 像素 |
| 预览 | 上传成功后显示缩略图预览 |
| 清除 | 支持清除已上传的示意图（非必填字段） |
| URL 存储 | 上传成功后通过 `onChange` 回调传给父表单 |

**7. `components/SchematicPreview.js`（示意图预览弹窗）**

| 功能项 | 说明 |
|--------|------|
| 大图预览 | 点击表格中示意图缩略图时弹出，展示完整尺寸示意图 |
| 关闭 | 点击遮罩或关闭按钮关闭弹窗 |

**8. `configs/web.config.js`（API 路径配置）**

新增能力管理相关接口路径常量：

```js
// 能力配置管理 API 配置
CAPABILITY_LIST: '/market-web/service/open/v2/capability/list',
CAPABILITY_DETAIL: '/market-web/service/open/v2/capability/{id}',
CAPABILITY_CREATE: '/market-web/service/open/v2/capability',
CAPABILITY_UPDATE: '/market-web/service/open/v2/capability/{id}',
CAPABILITY_DELETE: '/market-web/service/open/v2/capability/{id}',
CAPABILITY_UPLOAD_ICON: '/market-web/service/open/v2/capability/upload/icon',
CAPABILITY_UPLOAD_SCHEMATIC: '/market-web/service/open/v2/capability/upload/schematic',
```

**9. `utils/validators.js`（校验工具，新增）**

| 函数名 | 说明 |
|--------|------|
| `validateUrl` | 校验 URL 格式，只允许 http/https 协议或 `//` 开头的协议相对地址 |
| `validateImageSize` | 校验图片尺寸是否符合要求（传入 File 和期望宽高，返回 Promise<boolean>） |
| `validateFileSize` | 校验文件大小是否超过限制（传入 File 和最大字节数） |
| `validateFileType` | 校验文件 MIME 类型是否在允许列表中 |

###### 前端交互逻辑

**能力列表页交互流程：**

```
用户进入 /capability-config-manage
    │
    ├── 1. index.js 挂载，useEffect 触发 fetchData()
    ├── 2. fetchData() 调用 getCapabilityList({ pageNum: 1, pageSize: 20 })
    ├── 3. 返回数据后 setDataSource() + setPagination()
    ├── 4. Table 渲染列表，展示能力名称（图标+标题+描述）、访问地址、示意图缩略图等
    │
    ├── 用户输入搜索关键词 + 点击搜索
    │   ├── setQueryParams({ ...searchValues, pageNum: 1 })
    │   └── useEffect 监听 queryParams 变化，重新 fetchData()
    │
    ├── 用户点击「添加能力」
    │   ├── setModalVisible(true) + setModalTitle('添加能力') + setEditingId(null)
    │   └── CapabilityFormModal 打开，空表单
    │
    ├── 用户点击「编辑」
    │   ├── setModalVisible(true) + setModalTitle('编辑能力') + setEditingId(record.id)
    │   ├── 调用 getCapabilityDetail(id) 获取完整数据
    │   └── CapabilityFormModal 打开，useEffect 回填表单数据
    │
    ├── 用户点击「删除」
    │   ├── setConfirmModalVisible(true) 显示二次确认弹窗
    │   ├── 用户确认 -> 调用 deleteCapability(id)
    │   ├── 删除成功 -> message.success + 刷新列表
    │   └── 删除失败 -> message.error，列表不变
    │
    └── 用户点击示意图缩略图
        └── SchematicPreview 弹窗打开，展示大图
```

**新增/编辑弹窗交互流程：**

```
CapabilityFormModal 打开
    │
    ├── 编辑模式：useEffect -> form.setFieldsValue(initialValues) 回填数据
    ├── 新增模式：空表单
    │
    ├── 用户填写标题（Input，实时校验 2~30 字符）
    ├── 用户填写描述（TextArea，实时校验 5~200 字符，显示字符计数）
    │
    ├── 用户上传图标（IconUpload 组件）
    │   ├── 点击上传区域 -> 触发 <input type="file">
    │   ├── 选择文件 -> 校验类型（PNG/JPEG/SVG）
    │   ├── 校验大小（<= 200KB）
    │   ├── 校验尺寸（40x40，通过 Image 对象读取）
    │   ├── 校验通过 -> 调用 uploadIcon(file) 上传到服务端
    │   ├── 上传成功 -> 返回 URL -> 显示预览图 + onChange(url) 传给表单
    │   └── 上传失败 -> message.error，保留上传区域
    │
    ├── 用户上传示意图（SchematicUpload 组件，非必填）
    │   ├── 同图标流程，校验尺寸 520x288，大小 500K
    │   └── 支持清除已上传的示意图
    │
    ├── 用户填写排序值（InputNumber，>= 1 的正整数）
    ├── 用户填写访问地址（Input，校验 http/https 或 // 开头）
    ├── 用户填写路由路径（Input，校验以 / 开头）
    │
    ├── 右侧预览卡片实时更新（标题、描述、图标、示意图）
    │
    └── 用户点击「保存」
        ├── form.validateFields() 触发全部字段校验
        ├── 校验失败 -> 表单字段下方显示红色错误提示，阻止提交
        ├── 校验通过 -> onSubmit(values) 回调父组件
        ├── 父组件判断 editingId：
        │   ├── null -> 调用 createCapability(values) 新增
        │   └── 非空 -> 调用 updateCapability(editingId, values) 编辑
        ├── API 成功 -> message.success + closeModal + 刷新列表
        └── API 失败 -> message.error，弹窗保持打开，保留表单数据
```

###### 前端功能清单拆解

| 编号 | 功能模块 | 文件 | 功能描述 | 依赖 |
|------|---------|------|---------|------|
| FE-01 | 路由与菜单注册 | `router/index.tsx` + `components/Layout/index.js` | 新增 `/capability-config-manage` 路由声明和侧边栏菜单项 | 无 |
| FE-02 | API 路径配置 | `configs/web.config.js` | 新增 7 个能力配置 API 路径常量 | 无 |
| FE-03 | API 请求函数 | `routeRedBlue/capability-config-manage/thunk.js` | 实现 getCapabilityList、getCapabilityDetail、createCapability、updateCapability、deleteCapability、uploadIcon、uploadSchematic 共 7 个 API 调用函数 | FE-02 |
| FE-04 | 常量与表格配置 | `routeRedBlue/capability-config-manage/constant.js` | 定义搜索默认值、弹窗标题、表单校验规则、表格列配置（含自定义渲染：能力名称、示意图缩略图、状态标签、操作按钮） | 无 |
| FE-05 | 校验工具函数 | `utils/validators.js` | 实现 URL 格式校验、图片尺寸校验、文件大小校验、文件类型校验 4 个工具函数 | 无 |
| FE-06 | 能力配置列表页 | `routeRedBlue/capability-config-manage/index.js` | 实现列表数据加载、分页、搜索、新增/编辑/删除入口、示意图预览入口、loading 状态、错误提示 | FE-03, FE-04 |
| FE-07 | 列表页样式 | `routeRedBlue/capability-config-manage/index.module.less` | 列表页样式，复用现有页面布局风格 | 无 |
| FE-08 | 新增/编辑弹窗 | `components/CapabilityFormModal.js` | 实现表单字段（标题、描述、图标、示意图、排序、访问地址、路由路径）、新增/编辑复用、数据回填、表单校验、实时预览、提交逻辑 | FE-03, FE-04, FE-05, FE-09, FE-10 |
| FE-09 | 图标上传组件 | `components/IconUpload.js` | 实现上传区域、文件类型/大小/尺寸校验、上传请求、预览展示、替换功能 | FE-03, FE-05 |
| FE-10 | 示意图上传组件 | `components/SchematicUpload.js` | 实现上传区域、文件类型/大小/尺寸校验、上传请求、预览展示、清除功能 | FE-03, FE-05 |
| FE-11 | 示意图预览弹窗 | `components/SchematicPreview.js` | 实现点击缩略图弹出大图预览、遮罩关闭 | 无 |
| FE-12 | 弹窗表单样式 | `components/CapabilityFormModal.module.less` | 弹窗表单布局样式，含预览卡片样式 | 无 |

#### 6.3.2 实现设计

##### market-web 能力管理页面交互流程

```
业务管理员         market-web前端         marketadmin后端
    │                  │                      │
    │  进入能力管理页   │                      │
    ├─────────────────>│                      │
    │                  │  GET /api/capabilities│
    │                  ├─────────────────────>│
    │                  │  返回能力列表          │
    │                  │<─────────────────────┤
    │  显示列表页面     │                      │
    │<─────────────────┤                      │
    │                  │                      │
    │  点击"添加能力"   │                      │
    ├─────────────────>│                      │
    │  显示弹窗表单     │                      │
    │<─────────────────┤                      │
    │                  │                      │
    │  填写表单+上传文件 │                      │
    ├─────────────────>│                      │
    │                  │  POST /api/upload     │
    │                  ├─────────────────────>│
    │                  │  返回文件URL           │
    │                  │<─────────────────────┤
    │                  │                      │
    │  点击保存         │                      │
    ├─────────────────>│                      │
    │                  │  前端校验字段          │
    │                  │  POST /api/capabilities│
    │                  ├─────────────────────>│
    │                  │  后端校验+保存         │
    │                  │  返回成功              │
    │                  │<─────────────────────┤
    │  关闭弹窗+刷新列表 │                      │
    │<─────────────────┤                      │
```

##### wecodesite 动态加载流程时序图

```
用户浏览器          wecodesite           open-server API         qiankun
    │                  │                      │                    │
    │  访问页面         │                      │                    │
    ├─────────────────>│                      │                    │
    │                  │  渲染基础布局(骨架)    │                    │
    │  显示骨架屏       │                      │                    │
    │<─────────────────┤                      │                    │
    │                  │  GET /api/capabilities│                    │
    │                  ├─────────────────────>│                    │
    │                  │                      │  查询数据库          │
    │                  │  返回能力配置列表      │                    │
    │                  │<─────────────────────┤                    │
    │                  │                      │                    │
    │                  │  转换配置格式         │                    │
    │                  │  传入 Layout.jsx     │                    │
    │                  │  渲染动态菜单         │                    │
    │  显示完整页面     │                      │                    │
    │<─────────────────┤                      │                    │
    │                  │                      │                    │
    │  点击菜单项       │                      │                    │
    ├─────────────────>│                      │                    │
    │                  │  hash 路由变化        │                    │
    │                  │  Layout useEffect    │                    │
    │                  │  匹配 API 配置       │                    │
    │                  │  loadMicroApp()      │                    │
    │                  ├──────────────────────────────────────────>│
    │                  │                      │  fetch 子应用 entry │
    │                  │                      │  执行 mount()       │
    │                  │  子应用渲染到 #sub-app-viewport            │
    │  显示子应用页面   │                      │                    │
    │<─────────────────┤                      │                    │
    │                  │                      │                    │
    │  离开子应用页面   │                      │                    │
    ├─────────────────>│                      │                    │
    │                  │  useEffect cleanup   │                    │
    │                  │  handle.unmount()    │                    │
    │                  ├──────────────────────────────────────────>│
```

#### 6.3.3 功能可靠性分析

| 故障场景 | 影响 | 应对措施 |
|---------|------|---------|
| open-server API 不可达 | wecodesite 无法加载能力配置，菜单无法展示 | 降级展示主应用基础功能，显示"配置加载失败"提示和重试按钮 |
| 单条能力配置的 accessUrl 不合法 | 该子应用无法注册，菜单项缺失 | 跳过该条配置，控制台输出警告，其他配置正常注册 |
| 子应用 entry 不可达 | 点击菜单后页面空白 | 显示"页面加载失败"错误提示，不影响其他子应用 |
| 文件上传失败 | 图标/示意图无法保存 | 显示上传失败提示，允许重试，表单数据保留 |
| API 响应超时 | wecodesite 初始化阻塞 | 设置 5s 超时，超时后降级展示基础功能 |
| 子应用 JS 执行错误 | 子应用页面白屏 | qiankun 沙箱捕获错误，显示错误边界提示 |

#### 6.3.4 功能安全分析

| 安全风险 | 风险说明 | 应对措施 |
|---------|---------|---------|
| XSS 注入 | 能力配置的标题/描述字段可能包含恶意脚本 | 前端渲染时使用 React/Vue 的自动转义；后端对输入进行 HTML 实体编码 |
| 恶意 URL 注入 | accessUrl 字段可能配置为 javascript: 等恶意协议 | 前后端双重校验：只允许 http/https 协议；正则校验 URL 格式 |
| 文件上传攻击 | 上传恶意文件（如伪装为图片的脚本文件） | 后端校验文件 MIME 类型（仅允许 image/png、image/jpeg、image/svg+xml）；校验文件头魔数；限制文件大小 |
| 越权操作 | 未授权用户增删改能力配置 | 后端接口鉴权校验；market-web 路由级权限控制 |
| CSRF 攻击 | 伪造请求修改能力配置 | 后端接口校验 CSRF Token；关键操作二次确认 |
| 跨域安全 | 主应用 fetch 子应用资源存在跨域 | 子应用配置 CORS 允许主应用域名访问；生产环境限制具体域名而非 `*` |

#### 6.3.5 架构元素影响列表

| 架构元素 | 改动类型 | 改动说明 |
|---------|---------|---------|
| marketadmin CapabilityController | 新增 | 能力配置 CRUD 接口 |
| marketadmin CapabilityService | 新增 | 能力配置业务逻辑 |
| marketadmin Capability Model | 新增 | 能力配置数据模型 |
| marketadmin 文件上传接口 | 新增 | 图标/示意图上传接口 |
| market-web/src/pages/CapabilityManage/ | 新增 | 能力管理页面（列表+弹窗+上传组件） |
| market-web/src/services/capabilityApi.js | 新增 | 能力 CRUD API 请求模块 |
| wecodesite/src/main.jsx | 修改 | 当前仅 React 渲染，需新增异步加载配置并传入 App/Layout；增加容错处理 |
| wecodesite/src/microApps.js | 修改/废弃 | 当前静态子应用配置，改为从 API 动态获取后此文件可废弃 |
| wecodesite/src/App.jsx | 不修改 | 现有 `qiankun/*` 路由和通配符路由保持不变 |
| wecodesite/src/components/Layout/Layout.jsx | 修改 | 移除静态 microApps 导入，改为接收 API 配置；保留 loadMicroApp useEffect 逻辑和条件渲染 #sub-app-viewport 逻辑 |
| wecodesite/src/components/Layout/Sidebar/Sidebar.jsx | 修改 | 微前端应用菜单数据源从 microApps.js 改为 API 配置；已订阅能力菜单的 API 加载逻辑保持不变 |
| wecodesite/src/services/capabilityApi.js | 新增 | 能力配置 API 请求模块 |
| wecodesite/src/utils/microAppHelper.js | 新增 | 配置格式转换工具（API 数据 -> loadMicroApp 所需格式 + 菜单数据，含 menuKey 生成） |
| wecodesite/src/components/DynamicMenu.jsx | 新增 | 动态菜单组件，根据配置渲染菜单项（集成到 Sidebar） |

#### 6.3.6 接口设计
不涉及

###### 6.3.6.2 数据模型设计
不涉及

#### 6.3.7 功能实现分解分配清单

**表6：功能实现分解分配清单**

| 任务编号 | 任务名称 | 所属工程 | 任务描述 | 对应 US |
|---------|---------|---------|---------|---------|
| TASK-01 | 能力配置数据模型与 API 开发 | marketadmin | 创建能力配置数据表；实现 CRUD 接口和文件上传接口；实现字段校验和错误处理 | US-01 |
| TASK-02 | 能力列表页开发 | market-web | 实现能力列表页表格展示，包含序号、能力名称（图标+标题+描述）、访问地址（省略+tooltip）、示意图缩略图、操作账号、更新时间、操作列 | US-02 |
| TASK-03 | 能力新增/编辑弹窗开发 | market-web | 实现新增和编辑弹窗表单，复用同一表单组件；编辑时回填数据；实现前端字段校验 | US-02 |
| TASK-04 | 图标/示意图上传组件开发 | market-web | 实现图标上传（40x40，200K）和示意图上传（520x288，500K）组件；支持预览和替换；上传前尺寸和大小校验 | US-02 |
| TASK-05 | 能力删除功能开发 | market-web | 实现删除功能，二次确认后调用删除接口；删除成功后刷新列表 | US-02 |
| TASK-06 | wecodesite 配置异步加载模块 | wecodesite | 实现从 open-server API 异步获取能力配置；请求超时和失败容错处理；在 main.jsx 中新增异步初始化流程，将配置传入 App/Layout | US-03 |
| TASK-07 | wecodesite 配置转换与传入 | wecodesite | 实现将 API 配置转换为 loadMicroApp 所需格式（含 menuKey 生成）；将配置数据传入 Layout.jsx；保留现有 loadMicroApp useEffect 逻辑，仅替换数据源 | US-03 |
| TASK-08 | wecodesite 子应用容器保留与优化 | wecodesite | 保留 Layout.jsx 中条件渲染 `#sub-app-viewport` 和 loadMicroApp useEffect 的现有逻辑；处理子应用加载 loading/error 状态；App.jsx 现有 `qiankun/*` 路由保持不变 | US-03 |
| TASK-09 | wecodesite 动态菜单集成 | wecodesite | 改造 Sidebar.jsx，将微前端应用菜单数据源从 microApps.js 改为 API 配置（按 sortOrder 排序）；保留已订阅能力菜单的现有 API 加载逻辑 | US-03 |
| TASK-10 | 助手广场子应用微前端接入改造 | 助手广场 | 按照子应用接入规范导出生命周期函数；配置 CORS；配置 UMD/ESM 打包格式 | US-04 |

## 7 系统级非功能设计

### 7.1 系统级 FMEA 影响分析

| 失效模式 | 失效原因 | 失效影响 | 严重度 | 发生度 | 检测度 | RPN | 应对措施 |
|---------|---------|---------|--------|--------|--------|-----|---------|
| API 不可达 | open-server 服务宕机或网络故障 | wecodesite 无法加载菜单和子应用 | 高 | 低 | 高 | 中 | 降级展示主应用基础功能；提供重试机制；配置监控告警 |
| API 响应超时 | 数据库慢查询或网络延迟 | wecodesite 初始化卡顿 | 中 | 中 | 高 | 中 | 设置 5s 请求超时；超时后降级；API 端优化查询性能 |
| 子应用资源加载失败 | 子应用未部署或网络异常 | 对应菜单项页面空白 | 中 | 低 | 高 | 低 | 显示错误提示；不影响其他子应用；qiankun 错误边界捕获 |
| 配置数据异常 | API 返回格式错误或字段缺失 | 部分或全部子应用无法注册 | 中 | 低 | 中 | 低 | 前端做数据格式校验；单条异常跳过不影响整体 |
| 文件上传失败 | 存储服务故障或网络异常 | 图标/示意图无法保存 | 低 | 低 | 高 | 低 | 显示上传失败提示；允许重试；表单数据保留 |
| 并发配置冲突 | 多人同时编辑同一条配置 | 后保存的覆盖先保存的 | 低 | 中 | 低 | 低 | 后端使用乐观锁（updated_at 版本控制）；前端提示冲突 |

### 7.2 系统级安全影响分析

| 安全维度 | 风险分析 | 安全措施 |
|---------|---------|---------|
| 身份认证 | market-web 管理操作需身份认证；wecodesite API 请求需应用级认证 | market-web 用户登录态校验；wecodesite 使用 API Token/AppID 认证 |
| 权限控制 | 非授权用户不应能操作能力配置 | 后端接口鉴权校验用户角色；market-web 路由级权限控制 |
| 数据安全 | 能力配置数据在传输过程中需保护 | 全链路 HTTPS；API 请求携带认证 Token |
| 文件安全 | 上传文件可能包含恶意内容 | 后端校验文件类型（MIME + 文件头）；限制文件大小；上传文件存储到独立 CDN/OSS 域名 |
| 跨域安全 | wecodesite 加载子应用资源存在跨域 | 子应用配置 CORS 允许主应用域名；生产环境限制具体域名 |
| 输入校验 | 恶意输入可能导致 XSS、SQL 注入 | 前后端双重校验；前端 React/Vue 自动转义防 XSS；后端参数化查询防 SQL 注入 |

### 7.3 兼容性

#### 后向兼容性确认

| 兼容性项 | 说明 | 是否兼容 |
|---------|------|---------|
| 现有 wecodesite 自有功能 | 改造仅涉及入口初始化流程和左侧菜单，自有路由页面不受影响 | 是 |
| 现有 market-web 功能 | 新增能力管理页面，不影响已有功能模块 | 是 |
| 现有 marketadmin API | 新增能力配置接口，不影响已有接口 | 是 |
| 现有子应用 | 已接入 qiankun 的子应用无需改动，动态注册与静态注册对子应用透明 | 是 |

#### 前向兼容性确认

| 兼容性项 | 说明 | 是否兼容 |
|---------|------|---------|
| 后续新增子应用接入 | 仅需在 market-web 配置能力信息，wecodesite 自动加载 | 是 |
| API 字段扩展 | 数据模型预留扩展字段，API 返回新增字段时前端忽略未知字段 | 是 |
| 子应用技术栈扩展 | qiankun 支持任意技术栈子应用，后续可接入 Vue/Angular/Svelte 等 | 是 |

### 7.4 可运维

| 运维维度 | 设计说明 |
|---------|---------|
| 配置管理 | 能力配置通过 market-web 在线管理，无需修改配置文件或代码 |
| 日志监控 | marketadmin 记录能力配置操作日志（操作人、操作时间、操作内容）；wecodesite 控制台输出子应用注册和加载日志 |
| 健康检查 | marketadmin 提供能力配置 API 健康检查端点；监控 API 可用性和响应时间 |
| 告警通知 | API 不可达或响应超时阈值告警；文件上传失败率告警 |
| 故障恢复 | API 故障时 wecodesite 降级运行；共享数据库支持定期备份和恢复 |
| 灰度发布 | 能力配置支持按环境（UAT/生产）独立管理；可在 UAT 环境验证后发布到生产 |

### 7.5 资料

| 资料类型 | 说明 |
|---------|------|
| 子应用接入指南 | 编写 qiankun 子应用接入规范文档，包含 React/Vue + Vite/Webpack 的接入步骤 |
| 能力配置操作手册 | 编写 market-web 能力配置管理操作指南，供业务管理员使用 |
| API 接口文档 | 输出能力配置 CRUD API 的 Swagger/OpenAPI 文档 |
| 运维手册 | 输出能力配置相关运维操作手册，包含故障排查和恢复流程 |

---

## 8 checkList（必填）

### 8.1 设计自检清单要求（必填）

| check 点 | 是否达标 | 说明 |
|---------|---------|------|
| 需求背景和价值是否清晰描述 | 是 | 第1章详细描述了背景、价值、客户问题 |
| 涉及的角色和场景是否完整识别 | 是 | 第3.1节识别了9个场景和4类角色 |
| 结构化IR是否完整 | 是 | 第3.2节包含完整的IR属性 |
| 用例分析是否覆盖所有核心功能 | 是 | 第5章分析了8个用例，含主成功场景和扩展场景 |
| 功能设计是否包含接口定义 | 是 | 第6.3.6节定义了5个API接口，含输入参数和返回值 |
| 数据模型设计是否完整 | 是 | 第6.3.6.2节定义了12个字段及索引 |
| 字段校验规则是否明确 | 是 | 第6.3.6.1节和需求说明中明确了6项校验规则 |
| 微前端加载方案是否基于现有代码验证 | 是 | wecodesite 已使用 loadMicroApp 实现 qiankun 子应用加载（Layout.jsx），方案基于现有已验证代码，保持 loadMicroApp 模式，仅将配置源改为 API 驱动 |
| 子应用容器页面设计是否明确 | 是 | 第6.3.1节方案A保持 Layout.jsx 中条件渲染 `#sub-app-viewport` 和 loadMicroApp useEffect 的现有逻辑，配合 `qiankun/*` 路由，保持 Header + Sidebar 布局完整，子应用内容显示在右侧主区域 |
| wecodesite 零代码扩展是否可实现 | 是 | 现有 `qiankun/*` 路由已支持通配，新增子应用只需 API 配置 + loadMicroApp 动态匹配，无需改 App.jsx 路由代码 |
| market-web CRUD 功能是否完整 | 是 | 包含列表、新增、编辑、删除、文件上传5个功能 |
| 可靠性分析是否覆盖关键故障场景 | 是 | 第6.3.3节和第7.1节分析了6类故障场景及应对措施 |
| 安全分析是否覆盖主要安全风险 | 是 | 第6.3.4节和第7.2节分析了6类安全风险及措施 |
| 兼容性是否确认 | 是 | 第7.3节确认了后向和前向兼容性 |
| 任务分解是否可执行 | 是 | 第6.3.7节分解了10个独立 Task，分配到4个工程 |
| 性能指标是否定义 | 是 | API 接口清单定义了 TPS 和时延要求 |
| 是否满足"不使用 useCallback"规则 | 是 | 设计中未涉及 useCallback 的使用 |
| 是否满足"不体现代码提交内容"规则 | 是 | 文档为设计说明，不含代码提交相关内容 |
