# 前端页面 — 页面文档

> **文档定位**: sddu-docs-page — 前端页面文档 — 路由、组件树、交互流程  
> **输出文件名**: frontend-page.md  
> **数据来源**: 代码扫描生成 — wecodesite / market-web / qiankunProject / wecodesiteDemo 路由配置  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. wecodesite 开发者控制台（React 18 + React Router 6）

| 路径 | 页面 | 说明 |
|------|------|------|
| /appList | 应用列表 | 应用管理首页 |
| /appBasicInfo | 应用基本信息 | 应用详情/编辑 |
| /appVersionRelease | 版本发布 | 版本管理 |
| /membersManagement | 成员管理 | 团队成员 |
| /capability/:capabilityId | 能力详情 | 能力配置 |
| /abilities | 能力列表 | 能力目录 |
| /api-management | API 管理 | API 资源 |
| /events | 事件管理 | 事件资源 |
| /callbacks | 回调管理 | 回调资源 |
| /connectorList | 连接器列表 | 连接器管理 |
| /connectorEditor | 连接器编辑器 | 连接配置编辑 |
| /flowList | 连接流列表 | 连接流管理 |
| /flowEditor | 连接流编辑器 V2 | 编排画布（@xyflow） |
| /connect/history/flows/editor | 连接流编辑器（旧） | 历史编排 |
| /run-management | 运行管理 | 执行记录 |
| /admin/apis | 管理-API | 管理面 API |
| /admin/categories | 管理-分类 | 分类管理 |
| /admin/events | 管理-事件 | 管理面事件 |
| /admin/callbacks | 管理-回调 | 管理面回调 |
| /admin/approvals | 管理-审批 | 审批管理 |
| /operationLog | 操作日志 | 审计日志 |

## 2. market-web 市场管理后台（React 18 + React Router 6）

routeRedBlue 模块（Contextroot=''）：

| 路径 | 页面 | 说明 |
|------|------|------|
| /approveManage | 审批管理 | 应用审批 |
| /approveDetail | 审批详情 | 审批详情 + chatbot 绑定 |
| /lookup-classify | LookUp 分类 | 分类管理 |
| /lookup-item | LookUp 项 | 项管理 |
| /lookup-dictionary | LookUp 字典 | 字典管理 |
| /ability-admin | 能力管理 | 能力 CRUD（EditForm/CreateForm） |
| /app-chatbot-bindtab | 聊天机器人绑定 | 账号绑定 |

## 3. qiankunProject 微前端

### 主应用（main-app）
- qiankun `registerMicroApps` 注册 4 个子应用，HashRouter + activeRule 匹配 `location.hash`

| 子应用 | entry | activeRule（hash） |
|--------|-------|-------------------|
| sub-app-b | //localhost.uat.com:5174 | /qiankun/sub-b |
| sub-app-c | //localhost.uat.com:8082 | /qiankun/sub-c |
| sub-app-d | //localhost.uat.com:8083 | /qiankun/sub-d |
| sub-app-e | //localhost.uat.com:5175 | /qiankun/sub-e |

### 子应用路由

| 子应用 | 路由 |
|--------|------|
| sub-app-d | /（列表）、/detail、/edit |
| sub-app-e | /（列表）、/detail、/edit |
| sub-app-b / sub-app-c | 未扫描到独立 router（模块化组件） |

## 4. wecodesiteDemo 静态原型（9 页）

| 页面 | 说明 |
|------|------|
| index.html | 首页 |
| approval-center.html | 审批中心 |
| connector-list.html | 连接器列表 |
| connector-editor.html / connector-editorcopy.html | 连接器编辑器原型 |
| flow-list.html | 连接流列表 |
| flow-editor.html / flow-editorcopy.html | 连接流编辑器原型 |
| flow-run-log.html | 运行日志 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成（~35 页面） | 2026-08-03 | SDDU Docs Agent |
