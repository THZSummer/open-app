# 前端工程 — 全景入口

> **文档定位**: sddu-docs-overview — 本级全景入口  
> **输出文件名**: docs-overview.md  
> **数据来源**: 代码扫描生成 — wecodesite / market-web / qiankunProject / wecodesiteDemo  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  
> **生成方式**: 全量生成

---

## 1. 业务全景

### 1.1 自身概述

| 属性 | 值 |
|------|-----|
| **类型** | 前端工程组（4 个工程） |
| **职责描述** | 能力开放平台开发者控制台 + 市场管理后台 + 嵌入能力微前端 + 原型演示 |
| **所属业务域** | 前端层 |
| **版本** | wecodesite 1.0.0 / market-web 0.1.0 |

### 1.2 子组件

| 组件 | 类型 | 描述 | 关系说明 |
|------|------|------|---------|
| **wecodesite** | React 18 + Vite | 开发者控制台（feishu-developer-console）：应用管理、API/事件/回调、连接器/连接流编排 | 主前端 |
| **market-web** | React 18 + Vite | 市场管理后台（open-web）：审批、LookUp、字典、能力管理 | 管理前端 |
| **qiankunProject** | 微前端 | main-app + sub-app-b/c/d/e，嵌入能力容器 | 嵌入层 |
| **wecodesiteDemo** | 静态 HTML | 连接器/流编辑器原型（flow-editor / connector-editor 等 9 页） | 演示资产 |

### 1.3 子组件分类

| 分类 | 包含组件 |
|------|---------|
| **开发者控制台** | wecodesite（20 页面） |
| **市场管理** | market-web（8 模块） |
| **嵌入微前端** | qiankunProject（4 子应用） |
| **原型演示** | wecodesiteDemo（9 页） |

---

## 2. 技术全景

### 2.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18.2 | 框架 |
| React Router | 6.20 | 路由 |
| Vite | 5.x | 构建 |
| antd | 4.24 | UI |
| @xyflow/react | 12.10 | 连接流编排画布（wecodesite） |
| qiankun | 2.10.16 | 微前端 |
| zustand | 4.4 | market-web 状态管理 |
| Redux Toolkit | 2.12 | wecodesite 状态管理 |
| monaco-editor | 0.55 | 脚本/JSON 编辑器 |
| @monaco-editor/react | 4.7 | Monaco React 封装 |
| xlsx | 0.18 | Excel 导入导出 |
| puppeteer | 25 | 截图/测试（market-web devDependency） |

### 2.2 前端与后端对应

| 前端工程 | 后端对接 | 说明 |
|---------|---------|------|
| wecodesite | open-server (18080)、connector-api (18180)、event-server (18082) | 开发者控制台 |
| market-web | market-server (18083) | 市场管理 |
| qiankunProject | open-server (18080) | 嵌入能力 |

### 2.3 本域文档

| 文档 | 说明 |
|------|------|
| `page.md` | 前端页面/路由清单 |
| `integration.md` | 微前端集成（qiankun） |

---

## 修订记录

| 生成时间 | 变更 Feature | 生成方式 | 修订人 |
|---------|-------------|:--:|--------|
| 2026-08-03 | 代码扫描全量生成 | code-scan | SDDU Docs Agent |
