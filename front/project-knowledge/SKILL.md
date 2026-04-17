---
name: "project-knowledge"
description: "Analyzes project structure, pages, components and API calls. Invoke when user wants to generate project knowledge document or understand the codebase."
---

# Project Knowledge Generator

这个技能分析项目并生成全面的知识文档，帮助 AI 快速了解项目，包括项目目的、页面、组件和 API 调用。

## 何时使用

- 用户想要了解项目结构和目的
- 用户想要知道每个页面/组件调用了哪些 API
- 用户想要生成 AI 参考用的知识文档
- 用户询问"这个项目做什么"或"这个项目如何工作"

## 确认工作流

在生成知识文档之前，请与用户确认：

1. **目的确认**: 这个项目的文档背景/目的是什么？
2. **范围确认**: 是否有需要重点关注或排除的特定区域/页面/组件？
3. **输出确认**: 知识文档应该保存在哪里？

## 文档输出

生成的文档将包含：

### 1. 项目概述
- 项目名称和描述
- 技术栈（React、Vue 等）
- 目的和主要功能

### 2. 项目模块结构
- **模块名称**: 项目中每个模块的名称
- **模块描述**: 模块的功能
- **包含的文件**: 每个模块的文件列表
- **依赖**: 其他模块或包的依赖

对于每个模块，识别：
- 核心业务模块（pages）
- 公共组件
- 工具函数
- 配置文件
- 入口文件

### 2.1 架构概览
用文字描述项目的整体架构：

- **入口文件**: main.jsx - 应用入口文件
- **根组件**: App.jsx - 根组件，包含路由配置和主题配置
- **布局组件**: 包含 Header(顶部导航)、Sidebar(左侧菜单)、AppInfoBar(应用信息)、Content(右侧内容区)
- **页面模块**: 每个页面包含 PageName.jsx、route.js、thunk.js、mock.js、*.less
- **公共组件**: 如 AppCard、CreateAppModal、BindEamapModal 等

层级关系：main.jsx → App.jsx → Layout → (Header, Sidebar, AppInfoBar, Content) → Pages/Components

### 2.2 模块关系
用文字描述模块之间的依赖关系：

**布局层模块**:
- Layout: 主布局容器
- Header: 顶部导航栏，包含 Logo、文档链接、用户信息
- Sidebar: 左侧菜单栏，根据 appId 和 caps 动态渲染
- AppInfoBar: 应用信息栏，显示应用名称和绑定状态
- Content: 右侧主内容区，用于展示各个页面

**页面层模块**:
- AppList: 首页，应用列表展示
- BasicInfo: 基础信息页
- Members: 成员管理页
- Capabilities: 应用能力页
- CapabilityDetail: 能力详情页
- ApiManagement: API 管理页
- Events: 事件配置页
- OperationLog: 操作日志页
- VersionRelease: 版本发布页
- VersionForm: 版本详情页

**组件层模块**:
- AppCard: 应用卡片组件
- CreateAppModal: 创建应用弹窗
- BindEamapModal: 绑定 EAMAP 弹窗
- ApiPermissionDrawer: API 权限抽屉（ApiManagement 页面使用）
- EventDrawer: 事件添加抽屉（Events 页面使用）

### 2.3 页面导航流程
用文字描述页面导航流程：

**一级页面**: AppList（首页）

**二级页面（子页面）**: 从首页可以导航到以下页面（通过 appId 参数）：
- BasicInfo（基础信息）
- Capabilities（应用能力）
- Members（成员管理）
- ApiManagement（API管理）
- Events（事件配置）
- OperationLog（操作日志）
- VersionRelease（版本发布）

**三级页面**:
- Capabilities 的子页面：BotDetail（机器人配置）、WebDetail（网页应用配置）、MiniappDetail（小程序配置）、WidgetDetail（小组件配置）- 通过 type 参数导航
- VersionRelease 的子页面：VersionForm（版本详情）- 通过 versionId 参数导航

### 3. 目录结构
完整的文件树，包含每个目录和关键文件的描述

### 4. 页面文档
每个页面需要包含：
- **功能描述**: 页面功能
- **路由**: URL 路由
- **使用的组件**: 子组件列表
- **API 调用**: 从 thunk.js、mock.js 调用的所有 API 端点
- **关键函数**: 重要函数及其功能

### 5. 组件文档
每个组件需要包含：
- **功能描述**: 组件功能
- **Props**: 输入参数
- **Events**: 输出事件/回调

### 6. API 参考
所有 API 端点的完整列表：
- **端点名称**: 函数名
- **文件位置**: 定义位置
- **功能**: 功能描述
- **参数**: 输入参数
- **返回值**: 返回值

## 实施步骤

### 步骤 1: 分析项目结构
1. 扫描 src 目录以了解项目结构
2. 识别页面、组件和工具文件
3. 识别 API 相关文件（thunk.js、mock.js、services/、api/）

### 步骤 2: 分析项目模块
对于项目中识别的每个模块：
1. **核心业务模块（pages/）**: 分析每个页面目录
   - 页面组件（PageName.jsx）
   - 路由配置（route.js）
   - API 调用（thunk.js）
   - Mock 数据（mock.js）
   - 样式文件（PageName.m.less）
2. **公共组件（components/）**: 分析可复用组件
   - 组件文件
   - 样式文件
3. **布局组件**: Header、Sidebar、AppInfoBar
4. **入口文件**: App.jsx、main.jsx
5. 记录每个模块：
   - 模块名称和功能
   - 包含的文件
   - 对其他模块的依赖
   - 关键导出

### 步骤 3: 分析每个页面
对于 pages 目录中的每个页面：
1. 读取页面组件文件
2. 读取关联的 thunk.js 文件以识别 API 调用
3. 读取关联的 mock.js 文件以了解数据结构
4. 记录：
   - 页面路由
   - 功能描述
   - 调用的 API 函数
   - 关键状态和处理函数

### 步骤 4: 分析组件
对于 components 目录中的每个组件：
1. 读取组件文件
2. 记录：
   - 组件功能
   - Props/参数
   - 触发的事件
   - 使用的子组件

### 步骤 5: 聚合 API 信息
从所有 thunk.js 和 mock.js 文件中：
1. 列出所有导出的函数
2. 按功能分类（fetch、create、update、delete）
3. 记录参数和返回值

### 步骤 6: 生成知识文档
创建一个全面的 Markdown 文档，包含：
1. 项目概述
2. 项目模块 - 详细的模块结构和描述
3. 目录结构
4. 包含 API 调用的逐页分析
5. 组件目录
6. 完整的 API 参考

## 输出格式

知识文档应保存到：
`.trae/skills/project-knowledge/output/<timestamp>-knowledge.md`

## 质量标准

- **完整性**: 覆盖所有页面、组件和 API 函数
- **准确性**: 使用代码库中的精确文件路径和函数名
- **结构化**: 使用表格和清晰的标题便于查找
- **可操作性**: 为所有 API 包含参数类型和返回值
