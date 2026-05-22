# 前端页面设计：连接器平台

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§4.4)  
**版本**: v2.0  
**创建日期**: 2026-05-21  
**更新说明**: 对齐 spec.md v4.0——触发器仅 HTTP/手动，新增数据处理节点，监控简化为执行历史查询，FR 重编号

---

## 0. 主要变更说明（与 v1.x 的差异）

| 变更项 | v1.x（基于 spec v3.x） | v2.0（基于 spec v4.0） |
|--------|----------------------|----------------------|
| 触发器类型 | 事件/Webhook/定时/手动 | **HTTP + 手动** |
| MVP 节点类型 | 连接器节点 | 连接器节点 + **数据处理节点** |
| 监控面板 | 全指标仪表盘 | **执行历史查询** |
| 编排画布入口配置 | 含事件/Webhook/Cron 配置表单 | 仅 HTTP/手动配置 |
| FR 引用 | FR-001~FR-037 | **FR-001~FR-025** |
| 审批相关组件 | ✅ 有 | ❌ 移除 |
| 执行模型 | 异步（轮询状态） | **同步（直接展示结果）** |

---

## 1. 设计总则

| 项 | 说明 |
|---|------|
| 技术栈 | React 18 + Ant Design 4 + Vite + Less |
| 前端项目 | **wecodesite**（替代原 open-web，代码已迁移） |
| 样式方案 | Less Module（`.m.less` / `.less`），与现有项目一致 |
| 布局 | 沿用 `wecodesite` 现有 Layout 组件（侧边导航 + 内容区） |
| 状态管理 | `thunk.js` 模式（现有），新增 connector/flow/execution thunk |
| API 请求 | `src/utils/` 现有工具层 |
| 画布依赖 | `@xyflow/react`（已内置在 wecodesite `package.json`）|

---

## 2. 路由设计

> 💡 以下路由已部分在 `wecodesite` 中实现（`ConnectPlatform` 目录），新增/补充的路由标注"需新增"。

| 路由 | 页面组件 | 已有/需新增 |
|------|---------|------------|
| `/connect/connectors` | `ConnectPlatform/Connector/index.jsx` | ✅ 已有 |
| `/connect/connector-editor` | `ConnectPlatform/ConnectorEditor/index.jsx` | ✅ 已有 |
| `/connect/flows` | `ConnectPlatform/Flow/index.jsx` | ✅ 已有 |
| `/connect/flows/new` | `ConnectPlatform/FlowEditor/index.jsx` | ✅ 已有 |
| `/connect/flows/:id/edit` | `ConnectPlatform/FlowEditor/index.jsx` | ✅ 已有 |
| `/connect/flows/:id` | `ConnectPlatform/FlowDetail.jsx` | 🆕 需新增 |
| `/connect/flows/:id/executions/:execId` | `ConnectPlatform/ExecutionDetail.jsx` | 🆕 需新增 |
| `/connect/monitor` | `ConnectPlatform/Monitor/MonitorDashboard.jsx` | 🆕 需新增 |

**路由配置修改** (`wecodesite/src/App.jsx`):
```jsx
// 新增导入
import FlowDetail from './pages/ConnectPlatform/FlowDetail';
import ExecutionDetail from './pages/ConnectPlatform/ExecutionDetail';
import MonitorDashboard from './pages/ConnectPlatform/Monitor/MonitorDashboard';

// 新增路由（在 Layout 的 Routes 内）
<Route path="connect/flows/:id" element={<FlowDetail />} />
<Route path="connect/flows/:id/executions/:execId" element={<ExecutionDetail />} />
<Route path="connect/monitor" element={<MonitorDashboard />} />
```

---

## 3. 页面设计

### 3.1 连接器目录 (`ConnectorList`)

**路由**: `/connect/connectors` (已有，对应 `ConnectPlatform/Connector/index.jsx`)  
**对应 FR**: FR-004 (连接器列表查看)

**页面结构**:
```
┌─────────────────────────────────────────────────────────────┐
│ [搜索框]                       [筛选: 类型▼]    [创建]     │
├─────────────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────────────────┐│
│ │ 图标  名称          类型     最新版本   状态      操作    ││
│ │ ├───┐ IM 发送消息   HTTP     v1.0.0   已发布    [详情]  ││
│ │ ├───┐ 创建工单       HTTP     v0.0.1   草稿     [编辑]  ││
│ └───────────────────────────────────────────────────────────┘│
│ [分页: < 1 2 3 >]                                            │
└─────────────────────────────────────────────────────────────┘
```

**组件树**:
```
ConnectorList
├── SearchBar (Ant Design Input.Search)
├── FilterBar (Select: connector_type)
├── Button ("创建连接器", 跳转 /connect/connector-editor)
├── Table (Ant Design Table)
│   ├── Column: icon (Avatar)
│   ├── Column: name (Link → /connector-editor?mode=detail)
│   ├── Column: connector_type (Tag)
│   ├── Column: latestVersionNo
│   ├── Column: latestVersionStatus (Badge)
│   └── Column: actions (Button: 详情/编辑/删除)
└── Pagination (Ant Design Pagination)
```

**交互流程**:
1. 加载列表 → 调用 `GET /api/v1/connectors`
2. 搜索 → 实时调用 API（带 keyword 参数）
3. 点击创建 → 跳转 `/connect/connector-editor`
4. 点击连接器名称 → 跳转 `/connect/connector-editor`（编辑模式）
5. 删除 → 确认弹窗 → 调用 `DELETE /api/v1/connectors/{id}`

---

### 3.2 连接器创建/编辑 (`ConnectorEditor`)

**路由**: `/connect/connector-editor` (已有，对应 `ConnectPlatform/ConnectorEditor/index.jsx`)  
**对应 FR**: FR-001 (连接器创建), FR-002 (连接器编辑)

**页面结构**:
```
┌──────────────────────────────────────────────────────────────┐
│  ← 返回连接器目录                                            │
│                                                              │
│  ┌─ Step 1: 基本信息 ──────────────────────────────────────┐ │
│  │  名称:     [________________________]                    │ │
│  │  图标:     [📎 上传] [预设图标选择]                     │ │
│  │  描述:     [________________________________]            │ │
│  │  类型:     [HTTP ▼]                                      │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─ Step 2: 连接配置 ──────────────────────────────────────┐ │
│  │  [连接配置表单 - 根据类型动态渲染]                       │ │
│  │                                                          │ │
│  │  协议: HTTP                                              │ │
│  │  URL: [________________________________]                 │ │
│  │  方法: [GET ▼]                                           │ │
│  │  认证方式: [AKSK ▼]                                     │ │
│  │  AccessKey: [________________]                           │ │
│  │  SecretKey: [________________]                           │ │
│  │                                                          │ │
│  │  入参 Schema: [JSON Editor / 表单模式]                   │ │
│  │  出参 Schema: [JSON Editor / 表单模式]                   │ │
│  │                                                          │ │
│  │  超时(ms): [30000]                                       │ │
│  │  限流 - 每秒: [10]  并发: [5]                            │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                              │
│  [取消]                          [保存草稿] [发布(输入版本)] │
└──────────────────────────────────────────────────────────────┘
```

**交互流程**:
1. 编辑模式加载 → 调用 `GET /api/v1/connectors/{id}`
2. 类型选择变化 → 动态切换协议配置表单字段（MVP 仅 HTTP）
3. 认证方式选择变化 → 动态切换认证配置表单字段
4. 点击"保存草稿" → 调用 `PUT /api/v1/connectors/{id}/versions/{vid}`
5. 点击"发布" → 弹出版本号输入弹窗 → 调用 `POST .../publish`
6. 发布成功 → 跳转到连接器详情页

---

### 3.3 连接器详情 (`ConnectorDetail`)

**路由**: `/connect/connector-editor?mode=detail` (通过编辑器组件的查看模式)  
**对应 FR**: FR-005 (连接配置查看), FR-007 (版本切换)

**页面结构**:
```
┌──────────────────────────────────────────────────────────────┐
│  ← 返回连接器目录                                            │
│                                                              │
│  [图标]  IM 发送消息          [编辑] [删除]                  │
│  类型: HTTP | 版本: v1.0.0 (已发布)                          │
│  ── 描述 ─────────────────────────────────────────────       │
│  封装 IM 消息发送能力，支持文本消息                           │
│                                                              │
│  ┌─ Tab: 连接配置 ── 版本历史 ──────────────────────────┐  │
│  │                                                        │  │
│  │ [版本 v1.0.0 - 已发布] [版本对比]                      │  │
│  │                                                        │  │
│  │ 协议: HTTP                                              │  │
│  │ URL: https://openapi.xxx.com/im/send                    │  │
│  │ 认证: AKSK (凭证脱敏展示)                                │  │
│  │                                                        │  │
│  │ 入参 Schema: (只读 JSON)                                │  │
│  │ 出参 Schema: (只读 JSON)                                │  │
│  │                                                        │  │
│  │ 超时: 30s | 限流: 10/s, 5 并发                          │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

**组件树**:
```
ConnectorDetail
├── PageHeader (icon, name, action buttons)
│   ├── Button: 编辑 → /connector-editor?id=xxx
│   └── Button: 删除 → DELETE (确认弹窗)
├── InfoCards (type, version)
├── Description
└── Tabs (Ant Design Tabs)
    ├── Tab: 连接配置 (ConnectionConfigViewer)
    │   ├── VersionSelector (切换版本)
    │   ├── ProtocolInfo (只读展示)
    │   ├── AuthInfo (脱敏展示)
    │   ├── SchemaViewer (只读 JSON)
    │   └── TimeoutAndRateLimit
    └── Tab: 版本历史 (VersionHistory)
        └── Timeline (Ant Design Timeline)
```

---

### 3.4 连接流列表 (`FlowList`)

**路由**: `/connect/flows` (已有，对应 `ConnectPlatform/Flow/index.jsx`)  
**对应 FR**: FR-012 (连接流列表查看)

**页面结构**:
```
┌──────────────────────────────────────────────────────────────┐
│ [搜索框]          [筛选: 状态▼]                  [创建连接流]│
├──────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ 名称            状态       最后运行        最新版本  操作 │ │
│ │ 新消息通知    ● 运行中   2026-05-21    v1.0.0    [详情] │ │
│ │ 工单同步      ○ 已停止   2026-05-20    v0.0.1    [编辑] │ │
│ └──────────────────────────────────────────────────────────┘ │
│ [分页]                                                        │
└──────────────────────────────────────────────────────────────┘
```

**组件树**:
```
FlowList
├── SearchBar
├── FilterBar (Select: status)
├── Button ("创建连接流", 跳转 /connect/flows/new)
├── Table
│   ├── Column: name (Link → /connect/flows/:id)
│   ├── Column: status (Badge: 运行中/已停止)
│   ├── Column: lastRunTime
│   ├── Column: latestVersion
│   └── Column: actions (详情/编辑/启停)
└── Pagination
```

---

### 3.5 连接流编排画布 (`FlowCanvas`) ⭐ 核心页面

**路由**: `/connect/flows/new` / `/connect/flows/:id/edit` (已有，对应 `ConnectPlatform/FlowEditor/index.jsx`)  
**对应 FR**: FR-017 (连接流配置编辑), FR-020 (测试运行)

**页面结构**:
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  ← 返回连接流列表  |  连接流: 新消息通知  |  [状态: 草稿]  [版本: v0.0.1]│
│ ┌──────────────────────────────────────────┬──────────────────────────────┐│
│ │  ┌─ 节点面板 ───────────────────────┐   │  ┌─ 配置面板 ────────────┐   ││
│ │  │                                   │   │  │                       │   ││
│ │  │  [入口] [连接器] [数据处理] [出口]│   │  │  入口配置             │   ││
│ │  │       拖拽到画布                  │   │  │  触发方式: [HTTP ▼]   │   ││
│ │  │                                   │   │  │  HTTP 方法: [POST ▼]  │   ││
│ │  └───────────────────────────────────┘   │  │  请求体 Schema:        │   ││
│ │                                           │  │  [JSON Editor]        │   ││
│ │  ┌─ 编排画布 (React Flow) ────────────┐  │  │                       │   ││
│ │  │                                     │  │  │  连接器节点配置        │   ││
│ │  │  [📥 接收请求]                     │  │  │  选择连接器: [▼]       │   ││
│ │  │       │                             │  │  │  版本: [v1.0.0 ▼]    │   ││
│ │  │  [🔗 发送通知]                     │  │  │  参数映射:            │   ││
│ │  │       │                             │  │  │  receiver ← trigger. │   ││
│ │  │  [🔄 格式化消息]                    │  │  │  content  ← trigger. │   ││
│ │  │       │                             │  │  │                       │   ││
│ │  │  [📤 返回结果]                     │  │  │  数据处理节点配置      │   ││
│ │  │                                     │  │  │  字段映射:            │   ││
│ │  │  [缩放: 100%] [+][-] [平移]        │  │  │  源字段 → 目标字段    │   ││
│ │  └─────────────────────────────────────┘  │  │  ${node_1.x} → out.x  │   ││
│ │                                           │  └───────────────────────┘   ││
│ └──────────────────────────────────────────┴──────────────────────────────┘│
│                                                                             │
│ [💾 保存草稿] [▶ 测试运行] [📋 发布版本] [📋 部署]                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

**组件树**:
```
FlowCanvas (全屏页面，无 Layout 侧边栏)
├── CanvasHeader
│   ├── BackButton (← 返回)
│   ├── FlowTitle (连接流名称 + status badge)
│   └── VersionDisplay (版本号 + 状态)
├── CanvasBody
│   ├── NodePalette (左侧边栏)
│   │   ├── PaletteItem: EntryNode (可拖拽)
│   │   ├── PaletteItem: ConnectorNode (可拖拽)
│   │   ├── PaletteItem: DataProcessorNode (可拖拽) 🆕
│   │   └── PaletteItem: ExitNode (可拖拽)
│   ├── FlowCanvasArea (ReactFlow 实例)
│   │   ├── CustomNode: EntryNodeComponent
│   │   ├── CustomNode: ConnectorNodeComponent
│   │   ├── CustomNode: DataProcessorNodeComponent 🆕
│   │   ├── CustomNode: ExitNodeComponent
│   │   ├── CustomEdge (带数据映射标识)
│   │   ├── Controls (缩放/平移/适配)
│   │   └── MiniMap (缩略图)
│   └── ConfigPanel (右侧边栏，选中节点时展示)
│       ├── TriggerConfig (entry 节点选中时)
│       │   ├── Select: trigger_type (http / manual)
│       │   └── DynamicConfig (基于 type 切换)
│       │       ├── HTTP: method + schema editor
│       │       └── Manual: 无需额外配置
│       ├── ConnectorConfig (connector 节点选中时)
│       │   ├── Select: 连接器 + 版本
│       │   ├── InputMappingTable (源字段→目标字段)
│       │   └── AuthConfig (选择凭证/添加凭证)
│       ├── DataProcessorConfig (data_processor 节点选中时) 🆕
│       │   └── FieldMappingEditor (字段映射配置)
│       │       ├── SourceFieldSelector (${node_x.xxx} / ${trigger.xxx})
│       │       ├── TargetFieldInput
│       │       └── TransformSelector (base64_encode/uppercase/etc)
│       └── ExitConfig (exit 节点选中时)
│           └── OutputFieldsEditor (定义返回字段)
├── CanvasToolbar (底部)
│   ├── SaveButton (保存草稿)
│   ├── TestRunButton (测试运行)
│   │   └── TestDataDialog (Mock 触发数据)
│   ├── PublishButton (发布版本)
│   └── DeployButton (部署) 🆕
└── CanvasFooter (状态栏)
    ├── ZoomControl
    ├── NodeCount
    └── AutoSaveIndicator
```

**核心交互流程**:

1. **加载画布** → 调用 `GET /api/v1/flows/{id}/versions/{vid}` → 渲染 React Flow
2. **添加节点**: 从左侧面板拖拽节点到画布 → React Flow 的 `onNodesChange` 处理
3. **连接节点**: 从一个节点的 handle 拖拽连线到另一个节点 → MVP 限制为单向线性
4. **入口节点配置**: 点击入口节点 → 右侧面板 → 选择 HTTP 或手动触发
   - HTTP: 配置请求方法 + 触发数据 Schema（JSON Editor）
   - 手动: 无需额外配置（管理员通过界面手动触发）
5. **连接器节点配置**: 点击连接器节点 → 选择已发布连接器版本 → 配置输入参数映射
6. **数据处理节点配置**: 🆕 点击数据处理节点 → 配置源→目标字段映射
7. **出口节点配置**: 点击出口节点 → 定义返回字段列表
8. **数据映射**: 点击连线 → 弹出 DataMappingDialog → 配置源→目标字段映射
9. **保存草稿**: 点击保存 → 调用 `PUT /api/v1/flows/{id}/versions/{vid}`（全量保存）
10. **测试运行**: 
    - 点击测试运行 → 弹出 TestDataDialog
    - 用户输入 Mock 触发数据（基于 trigger schema 生成模板）
    - 调用 `POST /api/v1/flows/{id}/test-run`（同步执行）
    - 展示测试结果（步骤列表 + 输入/输出）
11. **发布**: 点击发布 → 输入版本号 → 调用 `POST .../publish`（无需审批）
12. **部署**: 发布后点击部署 → 调用 `POST /api/v1/flows/{id}/deploy`（无需审批）

> **与 v1.x 的差异**：
> - 入口触发器仅 HTTP + 手动（移除事件/Webhook/Cron 配置）
> - 新增数据处理节点（DataProcessorNode + DataProcessorConfig 面板）
> - 新增部署按钮（FR-013）
> - 测试运行改为同步执行，直接展示结果（不再需要轮询状态）

---

### 3.6 连接流详情 (`FlowDetail`)

**路由**: `/connect/flows/:id` (🆕 需新增，对应 `ConnectPlatform/FlowDetail.jsx`)  
**对应 FR**: FR-016 (配置查看), FR-025 (执行历史)

**页面结构**:
```
┌─────────────────────────────────────────────────────────────────┐
│  ← 返回连接流列表                                               │
│                                                                 │
│  新消息通知                          [编辑画布] [启停开关]      │
│  状态: ● 运行中  |  版本: v1.0.0  |  已部署                     │
│  最后执行: 2026-05-21 10:00:03                                  │
│                                                                 │
│  ┌─ Tab: 概览 ── 版本历史 ── 执行记录 ────────────────┐       │
│  │                                                        │       │
│  │  触发器: HTTP 触发                                     │       │
│  │  触发 URL: https://open.xxx.com/api/v1/trigger/...     │       │
│  │                                                        │       │
│  │  编排预览 (只读流程图):                                 │       │
│  │  ┌────────────────────────────────────────────────┐  │       │
│  │  │  [接收请求] → [发送通知] → [格式化] → [返回]  │  │       │
│  │  └────────────────────────────────────────────────┘  │       │
│  │                                                        │       │
│  │  使用的连接器:                                         │       │
│  │  • IM 发送消息 v1.0.0                                  │       │
│  │                                                        │       │
│  │  最近 10 次执行:                                       │       │
│  │  ✓ 成功  2026-05-21 10:00  耗时 2.2s                  │       │
│  │  ✓ 成功  2026-05-21 09:55  耗时 1.8s                  │       │
│  │  ✕ 失败  2026-05-21 09:50  耗时 5.0s  [查看详情]      │       │
│  └────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

**组件树**:
```
FlowDetail
├── PageHeader
│   ├── FlowName + StatusBadge
│   ├── Button: 编辑画布 → /connect/flows/:id/edit
│   └── Switch: 启停 → POST .../start 或 .../stop
├── InfoCards (status, version, lastRun)
└── Tabs
    ├── Tab: 概览 (Overview)
    │   ├── TriggerInfo (触发方式 + HTTP URL)
    │   ├── FlowPreview (只读 Mini ReactFlow)
    │   ├── UsedConnectors (连接器引用列表)
    │   └── RecentExecutions (最近执行摘要，链接到执行记录 Tab)
    ├── Tab: 版本历史 (VersionHistory)
    │   └── VersionTimeline
    └── Tab: 执行记录 (ExecutionHistory)
        ├── FilterBar (时间范围, 状态)
        ├── Table (executionId, triggerType, time, status, duration)
        │   └── Row → Link to /connect/flows/:id/executions/:execId
        └── Pagination
```

---

### 3.7 执行详情 (`ExecutionDetail`)

**路由**: `/connect/flows/:id/executions/:execId` (🆕 需新增)  
**对应 FR**: FR-025 (执行详情)

**页面结构**:
```
┌─────────────────────────────────────────────────────────────────┐
│  ← 返回连接流详情                                               │
│                                                                 │
│  执行 ID: exec_y5z6a7b8                                         │
│  状态: ✓ 成功  |  触发方式: HTTP                                │
│  耗时: 2.25s  |  时间: 2026-05-21 10:00:01                     │
│                                                                 │
│  ┌─ 执行步骤 ──────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │  ✓ Step 1: 接收请求  [entry]     10ms                   │  │
│  │     ├─ 输入: {"sender":"user_001","content":"你好"}     │  │
│  │     └─ 输出: {"sender":"user_001","content":"你好"}     │  │
│  │                                                          │  │
│  │  ✓ Step 2: 发送通知  [connector]  2.21s                 │  │
│  │     ├─ 输入: {"receiver":"user_001","content":"你好"}   │  │
│  │     └─ 输出: {"msg_id":"msg_xxxx","code":0}             │  │
│  │                                                          │  │
│  │  ✓ Step 3: 格式化消息 [data_processor]  15ms            │  │
│  │     ├─ 输入: {"msg_id":"msg_xxxx"}                      │  │
│  │     └─ 输出: {"result":{"id":"msg_xxxx","status":"ok"}} │  │
│  │                                                          │  │
│  │  ✓ Step 4: 返回结果  [exit]       5ms                   │  │
│  │     ├─ 输入: {"result":{"id":"msg_xxxx","status":"ok"}} │  │
│  │     └─ 输出: {"result":{"id":"msg_xxxx","status":"ok"}} │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  返回值: {"result":{"id":"msg_xxxx","status":"ok"}}             │
└─────────────────────────────────────────────────────────────────┘
```

**组件树**:
```
ExecutionDetail
├── PageHeader (execution_id, status badge)
├── SummaryCards (status, triggerType, duration, time)
├── ExecutionTimeline (Ant Design Steps: vertical)
│   └── ExecutionStep (status icon, name, type, duration)
│       ├── Collapse: 输入数据 (Readonly JSON)
│       └── Collapse: 输出数据 (Readonly JSON)
├── ResultDisplay (返回值 JSON)
```

> **与 v1.x 的差异**：
> - 执行模型改为同步（数据是一次性返回的，不需要轮询状态）
> - 新的步骤类型 data_processor 显示
> - 移除"重试执行"按钮（失败重试 NG15，V1 阶段引入）

---

### 3.8 监控面板 (`MonitorDashboard`)

**路由**: `/connect/monitor` (🆕 需新增)  
**对应 FR**: FR-025 (执行历史查询)

> ⚠️ **与 v1.x 的差异**：监控面板从全指标仪表盘**简化为执行历史查询**（spec v4.0 FR-025 仅定义执行历史查询）。全指标仪表盘（活跃流/总执行/成功率/平均耗时/执行趋势图表/按连接器统计）移至 V1。

**页面结构**:
```
┌──────────────────────────────────────────────────────────────┐
│  执行历史监控                     [时间范围: 最近 7 天 ▼]    │
│                                                              │
│  ┌─ 统计概览 ─────────────────────────────────────────────┐  │
│  │  总执行次数: 4,523  |  成功: 4,455  |  失败: 45  | 超时: 23 │
│  │  成功率: 98.5%  |  平均耗时: 1.85s                      │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌─ 执行列表 ──────────────────────────────────────────────┐  │
│  │  [状态筛选: 全部▼]                                       │  │
│  │                                                          │  │
│  │  执行ID       时间              触发方式  状态   耗时     │  │
│  │  exec_xxxx   2026-05-21 10:00   HTTP     ✓ 成功  2.2s   │  │
│  │  exec_xxxx   2026-05-21 09:55   HTTP     ✓ 成功  1.8s   │  │
│  │  exec_xxxx   2026-05-21 09:50   手动     ✕ 失败  5.0s   │  │
│  │                                                          │  │
│  │  [分页: < 1 2 3 ... 15 >]                                │  │
│  └─────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

**组件树**:
```
MonitorDashboard
├── TimeRangeSelector (Select: 24h/7d/30d)
├── StatSummary (精简统计行：总执行/成功/失败/超时/成功率/平均耗时)
└── ExecutionHistoryTable
    ├── FilterBar (status filter)
    ├── Table
    │   ├── Column: executionId (Link → /connect/flows/:flowId/executions/:id)
    │   ├── Column: startedAt
    │   ├── Column: triggerType (Tag)
    │   ├── Column: status (Badge: 成功/失败/超时)
    │   └── Column: durationMs
    └── Pagination
```

---

## 4. 新增组件

> 💡 **已有组件**（`wecodesite/src/pages/ConnectPlatform/` 已实现）：`Connector/index.jsx`、`ConnectorEditor/index.jsx`、`Flow/index.jsx`、`FlowEditor/index.jsx`、`FlowEditor/customNodes.jsx`、`FlowEditor/NodeLibrary.jsx`、`FlowEditor/NodeProperties.jsx`

### 4.1 画布相关组件（`wecodesite/src/pages/ConnectPlatform/`）

| 组件 | 功能 | 状态 |
|------|------|------|
| `FlowDetail.jsx` | 连接流详情页面 | 🆕 需新增 |
| `ExecutionDetail.jsx` | 执行详情页面（步骤输入/输出/返回值） | 🆕 需新增 |
| `FlowEditor/DataMappingDialog.jsx` | 数据映射配置弹窗 | 🆕 需新增 |
| `FlowEditor/TestRunDialog.jsx` | 测试运行弹窗（Mock 数据输入 + 结果展示） | 🆕 需新增 |

### 4.2 监控页面（`wecodesite/src/pages/ConnectPlatform/Monitor/`）

| 组件 | 功能 | 状态 |
|------|------|------|
| `MonitorDashboard.jsx` | 执行历史监控面板 | 🆕 需新增 |
| `index.jsx` | 页面入口 | 🆕 需新增 |
| `constants.jsx` | 常量定义 | 🆕 需新增 |
| `thunk.js` | 数据请求 | 🆕 需新增 |

### 4.3 画布节点自定义组件扩展

| 组件 | 位置 | 说明 | 状态 |
|------|------|------|------|
| `DataProcessorNodeComponent` | `FlowEditor/customNodes.jsx` | 🆕 数据处理节点自定义组件（带字段映射图形标识） | 🆕 需新增 |
| `DataProcessorConfig` | `FlowEditor/NodeProperties.jsx` | 🆕 数据处理节点配置面板 | 🆕 需新增 |
| `FieldMappingEditor` | `FlowEditor/NodeProperties.jsx` | 🆕 字段映射编辑器（源→目标字段配对） | 🆕 需新增 |

---

## 5. 状态管理 (thunk.js 模式)

> 💡 `wecodesite` 采用 `thunk.js` 模式管理数据和状态（每个页面目录下包含 `constants.jsx` + `thunk.js` + `mock.js`）。以下为新增模块的 thunk 设计。

### 连接器 thunk（`wecodesite/src/pages/ConnectPlatform/Connector/`）

```js
// constants.jsx — 补充 action types
export const FETCH_CONNECTORS = 'FETCH_CONNECTORS';
export const FETCH_CONNECTOR_DETAIL = 'FETCH_CONNECTOR_DETAIL';
export const CREATE_CONNECTOR = 'CREATE_CONNECTOR';
export const UPDATE_CONNECTOR = 'UPDATE_CONNECTOR';
export const DELETE_CONNECTOR = 'DELETE_CONNECTOR';
export const FETCH_VERSIONS = 'FETCH_VERSIONS';
export const PUBLISH_VERSION = 'PUBLISH_VERSION';
```

```js
// thunk.js — 补充 API 调用
export const fetchConnectors = (params) => async (dispatch) => { ... };
export const fetchConnectorDetail = (id) => async (dispatch) => { ... };
export const createConnector = (data) => async (dispatch) => { ... };
export const updateConnector = (id, data) => async (dispatch) => { ... };
export const deleteConnector = (id) => async (dispatch) => { ... };
export const publishVersion = (connectorId, versionId, data) => async (dispatch) => { ... };
```

> **与 v1.x 差异**：移除 `LIST_PUBLIC`, `DELIST`, `FETCH_STATS` action types（无上架/下架/统计）

### 连接流 thunk（`wecodesite/src/pages/ConnectPlatform/Flow/` + `FlowEditor/`）

```js
// constants.jsx — 补充 action types
export const FETCH_FLOWS = 'FETCH_FLOWS';
export const FETCH_FLOW_DETAIL = 'FETCH_FLOW_DETAIL';
export const CREATE_FLOW = 'CREATE_FLOW';
export const UPDATE_FLOW = 'UPDATE_FLOW';
export const DELETE_FLOW = 'DELETE_FLOW';
export const FETCH_VERSIONS = 'FETCH_VERSIONS';
export const PUBLISH_VERSION = 'PUBLISH_VERSION';
export const DEPLOY_FLOW = 'DEPLOY_FLOW';          // 🆕 部署
export const START_FLOW = 'START_FLOW';              // 🆕 启动
export const STOP_FLOW = 'STOP_FLOW';                // 🆕 停止
export const SAVE_CANVAS = 'SAVE_CANVAS';
export const TEST_RUN = 'TEST_RUN';
export const FETCH_EXECUTIONS = 'FETCH_EXECUTIONS';
export const FETCH_EXECUTION_DETAIL = 'FETCH_EXECUTION_DETAIL';
```

```js
// thunk.js — 补充 API 调用
export const fetchFlows = (params) => async (dispatch) => { ... };
export const fetchFlowDetail = (id) => async (dispatch) => { ... };
export const createFlow = (data) => async (dispatch) => { ... };
export const updateFlow = (id, data) => async (dispatch) => { ... };
export const deleteFlow = (id) => async (dispatch) => { ... };
export const saveCanvas = (flowId, versionId, config) => async (dispatch) => { ... };
export const publishVersion = (flowId, versionId) => async (dispatch) => { ... };
export const deployFlow = (flowId, versionId) => async (dispatch) => { ... };
export const startFlow = (id) => async (dispatch) => { ... };
export const stopFlow = (id) => async (dispatch) => { ... };
export const testRun = (flowId, data) => async (dispatch) => { ... };
export const fetchExecutions = (flowId, params) => async (dispatch) => { ... };
export const fetchExecutionDetail = (execId) => async (dispatch) => { ... };
```

> **与 v1.x 差异**：
> - 移除 `ENABLE_FLOW` / `DISABLE_FLOW` → 拆分为 `START_FLOW` / `STOP_FLOW`
> - 移除 `TRIGGER_MANUAL`（手动触发改为同步，直接调 execution API）
> - 新增 `DEPLOY_FLOW`
> - 新增 `FETCH_EXECUTIONS`, `FETCH_EXECUTION_DETAIL`

---

## 6. 服务层 API 封装

| 模块 | 主要导出 | 位置 |
|------|---------|------|
| 连接器 API | `fetchConnectors`, `fetchConnectorDetail`, `createConnector`, `updateConnector`, `deleteConnector`, `fetchVersions`, `publishVersion` | `ConnectPlatform/Connector/thunk.js` |
| 连接流 API | `fetchFlows`, `fetchFlowDetail`, `createFlow`, `updateFlow`, `deleteFlow`, `saveCanvas`, `publishVersion`, `deployFlow`, `startFlow`, `stopFlow` | `ConnectPlatform/Flow/thunk.js` |
| 运行时 API | `testRun`, `fetchExecutionDetail`, `fetchExecutionList` | `ConnectPlatform/Flow/thunk.js` |
| 监控 API | `fetchMetrics`（精简版：仅执行历史 + 统计） | `ConnectPlatform/Monitor/thunk.js` |