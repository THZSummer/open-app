# 前端页面设计：连接器平台

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§4.4)  
**版本**: v1.0  
**创建日期**: 2026-05-19

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
| `/connect/flows/new` | `ConnectPlatform/Flow/FlowCanvas.jsx` | ✅ 已有 |
| `/connect/flows/:id/edit` | `ConnectPlatform/Flow/FlowCanvas.jsx` | ✅ 已有 |
| `/connect/flows/:id` | `ConnectPlatform/Flow/FlowDetail.jsx` | 🆕 需新增 |
| `/connect/flows/:id/executions/:execId` | `ConnectPlatform/Flow/ExecutionDetail.jsx` | 🆕 需新增 |
| `/connect/monitor` | `ConnectPlatform/Monitor/MonitorDashboard.jsx` | 🆕 需新增 |

**路由配置修改** (`wecodesite/src/App.jsx`):
```jsx
// 新增导入
import FlowDetail from './pages/ConnectPlatform/Flow/FlowDetail';
import ExecutionDetail from './pages/ConnectPlatform/Flow/ExecutionDetail';
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
│ [搜索框]                       [筛选: 类型▼ 可见性▼] [创建] │
├─────────────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────────────────┐│
│ │ 图标  名称          类型   可见性   最新版本   操作       ││
│ │ ├───┐ IM 发送消息   HTTP   public    v1.2.0    [详情]   ││
│ │ ├───┐ 创建工单       HTTP   private   v0.0.1    [编辑]   ││
│ └───────────────────────────────────────────────────────────┘│
│ [分页: < 1 2 3 >]                                            │
└─────────────────────────────────────────────────────────────┘
```

**组件树**:
```
ConnectorList
├── SearchBar (Ant Design Input.Search)
├── FilterBar (Select x2: connector_type, visibility)
├── Button ("创建连接器", 跳转 /connectors/new)
├── Table (Ant Design Table)
│   ├── Column: icon (Avatar)
│   ├── Column: name (Link → /connectors/:id)
│   ├── Column: connector_type (Tag)
│   ├── Column: visibility (Tag: public/private)
│   ├── Column: latest_version
│   └── Column: actions (Button: 详情/编辑/更多)
└── Pagination (Ant Design Pagination)
```

**交互流程**:
1. 加载列表 → 调用 `GET /api/v1/connectors`
2. 搜索/筛选 → 实时调用 API（带参数）
3. 点击创建 → 跳转 `/connect/connector-editor`
4. 点击连接器名称 → 跳转 `/connect/connector-editor`（编辑模式）
5. 分页 → 参数 `curPage` / `pageSize`

---

### 3.2 连接器创建/编辑 (`ConnectorEditor`)

**路由**: `/connect/connector-editor` (已有，对应 `ConnectPlatform/ConnectorEditor/index.jsx`)  
**对应 FR**: FR-001 (连接器创建), FR-002 (连接器编辑)

**页面结构**:
```
┌──────────────────────────────────────────────────────────────┐
│  ← 返回连接器目录                                            │
│                                                              │
│  ┌─ Step 1: 基本信息系统 ──────────────────────────────────┐  │
│  │  名称:     [________________________]                    │  │
│  │  图标:     [📎 上传] [预设图标选择]                     │  │
│  │  描述:     [________________________________]            │  │
│  │  类型:     [HTTP ▼]                                      │  │
│  │  可见性:   ○ 私有 (仅当前应用可见)                      │  │
│  │            ○ 公共 (通过审批后对所有应用可见)            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌─ Step 2: 连接配置 ──────────────────────────────────────┐  │
│  │  [连接配置表单 - 根据类型动态渲染]                       │  │
│  │                                                          │  │
│  │  协议: HTTP                                              │  │
│  │  URL: [________________________________]                 │  │
│  │  方法: [GET ▼]                                           │  │
│  │  认证方式: [AKSK ▼]                                     │  │
│  │  AccessKey: [________________]                           │  │
│  │  SecretKey: [________________]                           │  │
│  │                                                          │  │
│  │  入参 Schema: [JSON Editor]                              │  │
│  │  出参 Schema: [JSON Editor]                              │  │
│  │                                                          │  │
│  │  超时(ms): [30000]                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                              │
│  [取消]                                [保存草稿] [发布]     │
└──────────────────────────────────────────────────────────────┘
```

**组件树**:
```
ConnectorForm
├── Steps (Ant Design Steps: 基本信息 → 连接配置)
├── Form (Ant Design Form)
│   ├── Step1: BasicInfoFields
│   │   ├── FormItem: name (Input, max 50)
│   │   ├── FormItem: icon (Upload + PresetIcons)
│   │   ├── FormItem: description (TextArea, max 500)
│   │   ├── FormItem: connector_type (Select)
│   │   └── FormItem: visibility (Radio: private/public)
│   ├── Step2: ConnectionConfigFields
│   │   ├── FormItem: protocol (Select: HTTP/MySQL/...)
│   │   ├── ProtocolFields (dynamic, based on protocol)
│   │   ├── AuthFields (dynamic, based on auth type)
│   │   ├── InputSchemaEditor (JSON editor / form mode)
│   │   ├── OutputSchemaEditor (JSON editor / form mode)
│   │   └── FormItem: timeout_ms (InputNumber)
│   └── Footer: SubmitButtons (保存草稿 / 提交发布)
```

**交互流程**:
1. 编辑模式加载 → 调用 `GET /api/v1/connectors/{id}`
2. 类型选择变化 → 动态切换协议配置表单字段
3. 认证方式选择变化 → 动态切换认证配置表单字段
4. 点击"保存草稿" → 调用 `PUT /api/v1/connectors/{id}/versions/{vid}`
5. 点击"发布" → 创建草稿保存 → 调用 `POST .../publish` → 进入审批流程
6. 审批中 → 跳转审批中心页面或显示审批中状态

---

### 3.3 连接器详情 (`ConnectorDetail`)

**路由**: `/connect/connector-editor?mode=detail` (通过编辑器组件的查看模式)  
**对应 FR**: FR-006 (使用统计), FR-007 (连接配置查看), FR-009 (版本切换)

**页面结构**:
```
┌──────────────────────────────────────────────────────────────┐
│  ← 返回连接器目录                                            │
│                                                              │
│  [图标]  IM 发送消息          [编辑] [上架/下架] [删除]      │
│  类型: HTTP | 可见性: 公共 | 版本: v1.2.0                    │
│  ── 描述 ─────────────────────────────────────────────       │
│  封装 IM 消息发送能力，支持文本、图片、文件等消息类型       │
│                                                              │
│  ┌─ Tab: 连接配置 ── 版本历史 ── 使用统计 ──────────────┐  │
│  │                                                        │  │
│  │ [版本 v1.2.0 - 已发布] [版本对比]                      │  │
│  │                                                        │  │
│  │ 协议: HTTP                                              │  │
│  │ URL: https://openapi.xxx.com/im/send                    │  │
│  │ 认证: AKSK (凭证由消费方在编排时配置)                   │  │
│  │                                                        │  │
│  │ 入参 Schema:                                            │  │
│  │ ┌──────────────────────────────────────────────────┐   │  │
│  │ │ {                                                  │   │  │
│  │ │   "receiver": { "type": "string" },                │   │  │
│  │ │   "content":  { "type": "string" }                 │   │  │
│  │ │ }                                                  │   │  │
│  │ └──────────────────────────────────────────────────┘   │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

**组件树**:
```
ConnectorDetail
├── PageHeader (icon, name, action buttons)
│   ├── Button: 编辑 → /connectors/:id/edit
│   ├── Button: 上架/下架 → POST .../list-public 或 .../delist
│   └── Button: 删除 → DELETE .../connectors/:id (确认弹窗)
├── InfoCards (type, visibility, latest_version)
├── Description
└── Tabs (Ant Design Tabs)
    ├── Tab: 连接配置 (ConnectionConfigViewer)
    │   ├── VersionSelector (切换版本)
    │   ├── ProtocolInfo (只读展示)
    │   ├── AuthInfo (脱敏展示)
    │   ├── SchemaViewer (只读 JSON)
    │   └── TimeoutDisplay
    ├── Tab: 版本历史 (VersionHistory)
    │   ├── Timeline (Ant Design Timeline)
    │   └── VersionCompare (版本差异对比)
    └── Tab: 使用统计 (UsageStats)
        ├── ReferencedFlows (引用的连接流列表)
        └── InvocationChart (调用次数图表)
```

---

### 3.4 连接流列表 (`FlowList`)

**路由**: `/connect/flows` (已有，对应 `ConnectPlatform/Flow/index.jsx`)  
**对应 FR**: FR-014 (连接流列表查看)

**页面结构**:
```
┌──────────────────────────────────────────────────────────────┐
│ [搜索框]               [筛选: 状态▼]             [创建连接流]│
├──────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ 名称            状态       最后运行       最新版本  操作 │ │
│ │ 新消息通知    ● 运行中   2026-05-19    v1.0.0    [详情] │ │
│ │ 定时报表      ○ 已停用   2026-05-18    v1.2.0    [编辑] │ │
│ │ 工单同步      ✕ 执行失败  2026-05-19    v0.0.1    [详情] │ │
│ └──────────────────────────────────────────────────────────┘ │
│ [分页]                                                        │
└──────────────────────────────────────────────────────────────┘
```

**组件树**:
```
FlowList
├── SearchBar
├── FilterBar (Select: status)
├── Button ("创建连接流", 跳转 /flows/:id/canvas)
├── Table
│   ├── Column: name (Link → /flows/:id)
│   ├── Column: status (Badge: 运行中/已停用/执行失败/审批中)
│   ├── Column: last_run_time
│   ├── Column: latest_version
│   └── Column: actions (详情/编辑/启停)
└── Pagination
```

---

### 3.5 连接流编排画布 (`FlowCanvas`) ⭐ 核心页面

**路由**: `/connect/flows/new` / `/connect/flows/:id/edit` (已有，对应 `ConnectPlatform/Flow/FlowCanvas.jsx`)  
**对应 FR**: FR-017 (连接流配置编辑), FR-020 (测试运行), FR-021 (测试数据模拟)

**页面结构**:
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  ← 返回连接流列表  |  连接流: 新消息通知  |  [状态: 草稿]  [版本: v0.0.1]│
│ ┌──────────────────────────────────────────┬──────────────────────────────┐│
│ │  ┌─ 节点面板 ───────────────────────┐   │  ┌─ 配置面板 ────────────┐   ││
│ │  │                                   │   │  │                       │   ││
│ │  │  [入口] [连接器] [出口]          │   │  │  触发器配置            │   ││
│ │  │       拖拽到画布                  │   │  │  类型: [事件 ▼]       │   ││
│ │  │                                   │   │  │  事件源: [选择 ▼]     │   ││
│ │  └───────────────────────────────────┘   │  │                       │   ││
│ │                                           │  │  连接器节点配置        │   ││
│ │  ┌─ 编排画布 (React Flow) ────────────┐  │  │  选择连接器: [▼]      │   ││
│ │  │                                     │  │  │  版本: [v1.2.0 ▼]    │   ││
│ │  │  [📥 收到消息]                     │  │  │  参数映射:            │   ││
│ │  │       │                             │  │  │  receiver ← trigger.  │   ││
│ │  │  [🔗 发送通知]                     │  │  │  content  ← trigger.  │   ││
│ │  │       │                             │  │  │                       │   ││
│ │  │  [📤 输出结果]                     │  │  │  重试策略:            │   ││
│ │  │                                     │  │  │  最多 [3] 次          │   ││
│ │  │  [缩放: 100%] [+][-] [平移]        │  │  │  间隔 [1000] ms       │   ││
│ │  └─────────────────────────────────────┘  │  │                       │   ││
│ │                                           │  └───────────────────────┘   ││
│ └──────────────────────────────────────────┴──────────────────────────────┘│
│                                                                             │
│ [💾 保存草稿] [▶ 测试运行] [📋 发布]                                      │
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
│   │   └── PaletteItem: ExitNode (可拖拽)
│   ├── FlowCanvasArea (ReactFlow 实例)
│   │   ├── CustomNode: EntryNodeComponent
│   │   ├── CustomNode: ConnectorNodeComponent
│   │   ├── CustomNode: ExitNodeComponent
│   │   ├── CustomEdge (带数据映射标识)
│   │   ├── Controls (缩放/平移/适配)
│   │   └── MiniMap (缩略图)
│   └── ConfigPanel (右侧边栏，选中节点时展示)
│       ├── TriggerConfig (entry 节点选中时)
│       │   ├── Select: trigger_type (event/webhook/scheduled/manual)
│       │   └── DynamicConfig (基于 type 切换)
│       ├── ConnectorConfig (connector 节点选中时)
│       │   ├── Select: 连接器 + 版本
│       │   ├── InputMappingTable (源字段→目标字段)
│       │   ├── RetryPolicyForm
│       │   └── AuthConfig (选择已保存的凭证/添加新凭证)
│       └── ExitConfig (exit 节点选中时)
│           └── OutputFieldsEditor (定义返回字段)
├── CanvasToolbar (底部)
│   ├── SaveButton (保存草稿)
│   ├── TestRunButton (测试运行)
│   │   └── TestDataDialog (Mock 触发数据)
│   └── PublishButton (发布版本)
└── CanvasFooter (状态栏)
    ├── ZoomControl
    ├── NodeCount
    └── AutoSaveIndicator
```

**核心交互流程**:

1. **加载画布** → 调用 `GET /api/v1/flows/{id}/versions/{vid}` → 渲染 React Flow
2. **添加节点**: 从左侧面板拖拽节点到画布 → React Flow 的 `onNodesChange` 处理
3. **连接节点**: 从一个节点的 handle 拖拽连线到另一个节点 → MVP 限制为单向线性
4. **配置节点**: 点击节点 → 右侧面板显示配置表单 → 编辑后实时更新 React Flow 节点数据
5. **数据映射**: 点击连线 → 弹出 DataMappingDialog → 配置源→目标字段映射
6. **保存草稿**: 点击保存 → 调用 `PUT /api/v1/flows/{id}/versions/{vid}`（全量保存）
7. **测试运行**: 
   - 点击测试运行 → 弹出 TestDataDialog
   - 用户输入 Mock 触发数据（基于 trigger schema 生成模板）
   - 调用 `POST /api/v1/flows/{id}/test-run`
   - 展示测试结果（步骤列表 + 输入/输出）
8. **发布**: 点击发布 → 调用 `POST .../publish` → 进入审批流程

---

### 3.6 连接流详情 (`FlowDetail`)

**路由**: `/connect/flows/:id` (🆕 需新增，对应 `ConnectPlatform/Flow/FlowDetail.jsx`)  
**对应 FR**: FR-016 (配置查看), FR-031 (运行状态), FR-032 (执行历史)

**页面结构**:
```
┌─────────────────────────────────────────────────────────────────┐
│  ← 返回连接流列表                                               │
│                                                                 │
│  新消息通知                              [编辑画布] [启停开关]  │
│  状态: ● 运行中  |  版本: v1.0.0                                │
│  最后执行: 2026-05-19 10:00:03  |  下次执行: 2026-05-20 09:00  │
│                                                                 │
│  ┌─ Tab: 概览 ── 版本历史 ── 执行记录 ────────────────┐       │
│  │                                                        │       │
│  │  触发器: 事件触发 (im:message:receive)                  │       │
│  │                                                        │       │
│  │  编排预览 (只读流程图):                                 │       │
│  │  ┌────────────────────────────────────────────────┐  │       │
│  │  │  [收到消息] → [发送通知] → [输出结果]          │  │       │
│  │  └────────────────────────────────────────────────┘  │       │
│  │                                                        │       │
│  │  使用的连接器:                                         │       │
│  │  • IM 发送消息 v1.2.0 (来自: 应用 xxx)                │       │
│  │                                                        │       │
│  │  最近 10 次执行:                                       │       │
│  │  ✓ 成功  2026-05-19 10:00  耗时 2.2s                  │       │
│  │  ✓ 成功  2026-05-19 09:55  耗时 1.8s                  │       │
│  │  ✕ 失败  2026-05-19 09:50  耗时 5.0s  [查看详情]      │       │
│  └────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

**组件树**:
```
FlowDetail
├── PageHeader
│   ├── FlowName + StatusBadge
│   ├── Button: 编辑画布 → /flows/:id/canvas
│   └── Switch: 启用/停用 → POST .../enable 或 .../disable
├── InfoCards (status, version, last_run, next_run)
└── Tabs
    ├── Tab: 概览 (Overview)
    │   ├── TriggerInfo
    │   ├── FlowPreview (只读 Mini ReactFlow)
    │   ├── UsedConnectors (连接器引用列表)
    │   └── RecentExecutions (最近执行摘要)
    ├── Tab: 版本历史 (VersionHistory)
    │   └── VersionTimeline
    └── Tab: 执行记录 (ExecutionHistory)
        ├── FilterBar (时间范围, 状态)
        ├── Table (execution_id, time, status, duration)
        │   └── Row → Link to /flows/:id/executions/:execId
        └── Pagination
```

---

### 3.7 执行详情 (`ExecutionDetail`)

**路由**: `/connect/flows/:id/executions/:execId` (🆕 需新增，对应 `ConnectPlatform/Flow/ExecutionDetail.jsx`)  
**对应 FR**: FR-033 (执行详情查看)

**页面结构**:
```
┌─────────────────────────────────────────────────────────────────┐
│  ← 返回连接流详情                                               │
│                                                                 │
│  执行 ID: exec_y5z6a7b8                                         │
│  状态: ✓ 成功  |  触发方式: 手动                                │
│  耗时: 2.25s  |  时间: 2026-05-19 10:00:01                     │
│                                                                 │
│  ┌─ 执行步骤 ──────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │  ✓ Step 1: 收到消息  [entry]     10ms                   │  │
│  │     ├─ 输入: {"sender":"user_001","content":"你好"}     │  │
│  │     └─ 输出: {"sender":"user_001","content":"你好"}     │  │
│  │                                                          │  │
│  │  ✓ Step 2: 发送通知  [connector]  2.21s  [查看响应]    │  │
│  │     ├─ 输入: {"receiver":"user_001","content":"你好"}   │  │
│  │     └─ 输出: {"msg_id":"msg_xxxx","code":0}             │  │
│  │                                                          │  │
│  │  ✓ Step 3: 输出结果  [exit]       5ms                   │  │
│  │     ├─ 输入: {"msg_id":"msg_xxxx"}                      │  │
│  │     └─ 输出: {"msg_id":"msg_xxxx"}                      │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  返回值: {"msg_id":"msg_xxxx"}                                  │
│                                                                 │
│  [重试执行]                                                     │
└─────────────────────────────────────────────────────────────────┘
```

**组件树**:
```
ExecutionDetail
├── PageHeader (execution_id, status badge)
├── SummaryCards (status, trigger_type, duration, time)
├── ExecutionTimeline (Ant Design Steps: vertical)
│   └── ExecutionStep (status icon, name, type, duration)
│       ├── Collapse: 输入数据 (Readonly JSON)
│       ├── Collapse: 输出数据 (Readonly JSON)
│       └── Collapse: 错误信息 (if failed)
├── ResultDisplay (返回值 JSON)
└── ActionButtons
    └── Button: 重试执行 → POST .../executions/:id/retry
```

---

### 3.8 运行监控面板 (`MonitorDashboard`)

**路由**: `/connect/monitor` (🆕 需新增，对应 `ConnectPlatform/Monitor/MonitorDashboard.jsx`)  
**对应 FR**: FR-034 (运行指标统计)

**页面结构**:
```
┌──────────────────────────────────────────────────────────────┐
│  运行监控面板                    [时间范围: 最近 7 天 ▼]    │
│                                                              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │
│  │ 活跃流  │  │ 总执行  │  │成功率   │  │平均耗时 │       │
│  │    12   │  │  4,523  │  │  98.5%  │  │ 1.85s  │       │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘       │
│                                                              │
│  ┌─ 执行趋势 ──────────────────────────────────────────┐   │
│  │                                                     │   │
│  │  [折线图: 成功/失败/超时 按小时分布]                 │   │
│  │                                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌─ 按连接器统计 ───────────────────────────────────────┐   │
│  │                                                     │   │
│  │  连接器          调用次数   成功率   平均耗时         │   │
│  │  IM 发送消息      2,350    99.2%    0.85s            │   │
│  │  创建工单         1,100    97.5%    2.10s            │   │
│  │  查询用户           673    98.1%    0.45s            │   │
│  │                                                     │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

**组件树**:
```
MonitorDashboard
├── TimeRangeSelector (Select: 1h/24h/7d/30d)
├── StatCards (Ant Design Card x4)
│   ├── StatCard: active_flows
│   ├── StatCard: total_executions
│   ├── StatCard: success_rate
│   └── StatCard: avg_duration
├── ExecutionTrendChart (折线图 / 柱状图)
└── ConnectorStatsTable (Table)
    ├── Column: connector_name
    ├── Column: total_invocations
    ├── Column: success_rate
    ├── Column: avg_duration
    └── Column: trend (趋势箭头)
```

---

## 4. 新增组件

> 💡 **已有组件**（`wecodesite/src/pages/ConnectPlatform/` 已实现）：`Connector/index.jsx`、`ConnectorEditor/index.jsx`、`Flow/index.jsx`、`Flow/FlowCanvas.jsx`、`Flow/customNodes.jsx`、`Flow/NodeLibrary.jsx`、`Flow/NodeProperties.jsx`  
> **以下为需新增的组件**：

### 4.1 画布相关组件（`wecodesite/src/pages/ConnectPlatform/Flow/`）

| 组件 | 功能 | 状态 |
|------|------|------|
| `FlowDetail.jsx` | 连接流详情页面 | 🆕 需新增 |
| `ExecutionDetail.jsx` | 执行详情页面（步骤输入/输出/返回值） | 🆕 需新增 |
| `DataMappingDialog.jsx` | 数据映射配置弹窗（源→目标字段映射） | 🆕 需新增 |
| `TestRunDialog.jsx` | 测试运行弹窗（Mock 数据输入 + 结果展示） | 🆕 需新增 |

### 4.2 监控页面（`wecodesite/src/pages/ConnectPlatform/Monitor/`）

| 组件 | 功能 | 状态 |
|------|------|------|
| `MonitorDashboard.jsx` | 运行监控面板 | 🆕 需新增 |
| `index.jsx` | 页面入口 | 🆕 需新增 |
| `constants.jsx` | 常量定义 | 🆕 需新增 |
| `thunk.js` | 数据请求 | 🆕 需新增 |

---

## 5. 状态管理 (thunk.js 模式)

> 💡 `wecodesite` 采用 `thunk.js` 模式管理数据和状态（每个页面目录下包含 `constants.jsx` + `thunk.js` + `mock.js`）。以下为新增模块的 thunk 设计。

### 连接器 thunk（新增至 `wecodesite/src/pages/ConnectPlatform/Connector/`）

```js
// constants.jsx — 已存在，需补充 action types
export const FETCH_CONNECTORS = 'FETCH_CONNECTORS';
export const FETCH_CONNECTOR_DETAIL = 'FETCH_CONNECTOR_DETAIL';
export const CREATE_CONNECTOR = 'CREATE_CONNECTOR';
export const UPDATE_CONNECTOR = 'UPDATE_CONNECTOR';
export const DELETE_CONNECTOR = 'DELETE_CONNECTOR';
export const FETCH_VERSIONS = 'FETCH_VERSIONS';
export const PUBLISH_VERSION = 'PUBLISH_VERSION';
export const LIST_PUBLIC = 'LIST_PUBLIC';
export const DELIST = 'DELIST';
export const FETCH_STATS = 'FETCH_STATS';
```

```js
// thunk.js — 已存在，需补充 API 调用
export const fetchConnectors = (params) => async (dispatch) => { ... };
export const fetchConnectorDetail = (id) => async (dispatch) => { ... };
export const createConnector = (data) => async (dispatch) => { ... };
export const updateConnector = (id, data) => async (dispatch) => { ... };
export const deleteConnector = (id) => async (dispatch) => { ... };
export const publishVersion = (id, vid) => async (dispatch) => { ... };
export const listPublic = (id) => async (dispatch) => { ... };
export const delist = (id) => async (dispatch) => { ... };
export const fetchStats = (id) => async (dispatch) => { ... };
```

### 连接流 thunk（新增至 `wecodesite/src/pages/ConnectPlatform/Flow/`）

```js
// constants.jsx — 已存在，需补充 action types
export const FETCH_FLOWS = 'FETCH_FLOWS';
export const FETCH_FLOW_DETAIL = 'FETCH_FLOW_DETAIL';
export const CREATE_FLOW = 'CREATE_FLOW';
export const UPDATE_FLOW = 'UPDATE_FLOW';
export const DELETE_FLOW = 'DELETE_FLOW';
export const FETCH_VERSIONS = 'FETCH_VERSIONS';
export const PUBLISH_VERSION = 'PUBLISH_VERSION';
export const ENABLE_FLOW = 'ENABLE_FLOW';
export const DISABLE_FLOW = 'DISABLE_FLOW';
export const SAVE_CANVAS = 'SAVE_CANVAS';
export const TRIGGER_MANUAL = 'TRIGGER_MANUAL';
export const TEST_RUN = 'TEST_RUN';
export const RETRY_EXECUTION = 'RETRY_EXECUTION';
```

```js
// thunk.js — 已存在，需补充 API 调用
export const fetchFlows = (params) => async (dispatch) => { ... };
export const fetchFlowDetail = (id) => async (dispatch) => { ... };
export const createFlow = (data) => async (dispatch) => { ... };
export const updateFlow = (id, data) => async (dispatch) => { ... };
export const deleteFlow = (id) => async (dispatch) => { ... };
export const saveCanvas = (flowId, versionId, config) => async (dispatch) => { ... };
export const publishVersion = (flowId, vid) => async (dispatch) => { ... };
export const enableFlow = (id) => async (dispatch) => { ... };
export const disableFlow = (id) => async (dispatch) => { ... };
export const triggerManual = (flowId, data) => async (dispatch) => { ... };
export const testRun = (flowId, data) => async (dispatch) => { ... };
export const retryExecution = (execId) => async (dispatch) => { ... };
```

---

## 6. 服务层 API 封装

> 💡 `wecodesite` 使用 `src/utils/` 工具层进行 API 请求，遵循现有模式即可。

| 模块 | 主要导出 | 位置 |
|------|---------|------|
| 连接器 API | `fetchConnectors`, `fetchConnectorDetail`, `createConnector`, `updateConnector`, `deleteConnector`, `fetchVersions`, `publishVersion`, `fetchStats` | `ConnectPlatform/Connector/thunk.js` |
| 连接流 API | `fetchFlows`, `fetchFlowDetail`, `createFlow`, `updateFlow`, `deleteFlow`, `saveCanvas`, `publishVersion`, `enableFlow`, `disableFlow` | `ConnectPlatform/Flow/thunk.js` |
| 运行时 API | `triggerManual`, `testRun`, `fetchExecutionStatus`, `fetchExecutionDetail`, `fetchExecutionList`, `retryExecution` | `ConnectPlatform/Flow/thunk.js` |
| 监控 API | `fetchMetrics`, `fetchConnectorMetrics` | `ConnectPlatform/Monitor/thunk.js` |