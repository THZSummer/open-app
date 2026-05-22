# 前端页面设计：连接器平台

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§4.5), plan-api.md (§2-§5), plan-db.md  
**版本**: v2.7.5  
**创建日期**: 2026-05-21  
**最后更新**: 2026-05-22  
**对齐基线**: plan.md v2.7.5（含 v2.0 → v2.7.5 全部决策）

---

## 0. 版本对齐说明

| 维度 | v2.0 | v2.7.5 | 决策来源 |
|------|------|--------|---------|
| 触发器类型 | 事件/Webhook/定时/手动 | **HTTP / 手动 / 测试** | v2.0 |
| MVP 节点类型 | 连接器节点 | 连接器节点 + **数据处理节点** | v2.0 |
| 监控面板 | 全指标仪表盘 | **执行历史查询 + 精简统计** | v2.0 |
| 编排画布触发器配置 | 含事件/Webhook/Cron 配置表单 | **仅 HTTP/手动配置 + 认证类型 schema 配置**（不输入凭证值） | v2.0 + v2.6 |
| 执行模型 | 异步（轮询状态） | **同步（直接展示结果）** | v2.0 |
| **名称/描述** | 单语 `name`/`description` | **双语 `nameCn`/`nameEn` + `descriptionCn`/`descriptionEn`** | v2.7 |
| **凭证输入界面** | 连接器编辑器内输入 AccessKey/SecretKey 等凭证值并保存 | **❌ 移除凭证输入**——连接器编辑器仅配置 `authTypeSchema`（声明类型与字段名）；凭证由**调用方在触发请求时携带**（手动调试/测试运行时在弹窗输入并仅当次使用） | v2.6 |
| **触发器配置** | 触发器单独管理（含 token） | **触发器配置内嵌于编排 JSON**，与编排同保存/发布 | v2.7.3 |
| **ID 显示** | 业务前缀 ID（如 `con_xxxx`） | **数字 ID** 直接显示（必要时显示前 12 位 + ...）；内部传递用 string 类型 | v2.7 + API 决策 |
| **状态展示** | 字符串直接展示 | **API 返回 TINYINT 数字，前端通过字典转译为本地化标签** | API 决策 |
| **执行详情** | 步骤输入/输出直接展示 | 新增**大字段外置感知**：检测 `{"$externalized": "<blobId>"}` 标记时显示"查看完整内容（外置）"按钮；新增 **`externalResourceId` 标识展示**（外部资源 ID 用于反查溯源） | v2.4 + v2.7.5 |
| FR 引用 | FR-001~FR-037 | **FR-001~FR-025** | v2.0 |

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

> ⚠️ **凭证不持久化**（v2.6 决策）：连接器编辑器**仅配置认证类型 schema**（声明类型 + 字段名 + 是否敏感），**不接受任何凭证值输入**；凭证由调用方在触发/调试时携带。

**页面结构**:
```
┌──────────────────────────────────────────────────────────────┐
│  ← 返回连接器目录                                            │
│                                                              │
│  ┌─ Step 1: 基本信息 ──────────────────────────────────────┐ │
│  │  中文名称: [________________________]                    │ │
│  │  英文名称: [________________________]                    │ │
│  │  图标:     [📎 上传] [预设图标选择]                     │ │
│  │  中文描述: [________________________________]            │ │
│  │  英文描述: [________________________________]            │ │
│  │  标签:     [tag1, tag2, ...]                             │ │
│  │  类型:     [HTTP ▼]                                      │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─ Step 2: 连接配置 ──────────────────────────────────────┐ │
│  │  [连接配置表单 - 根据类型动态渲染]                       │ │
│  │                                                          │ │
│  │  协议: HTTP                                              │ │
│  │  URL: [________________________________]                 │ │
│  │  方法: [POST ▼]                                          │ │
│  │  Headers: [键值对编辑器]                                 │ │
│  │                                                          │ │
│  │  ┌─ 认证类型 Schema（不输入凭证值）─────────────────┐  │ │
│  │  │  认证类型: [AKSK ▼]                                 │  │ │
│  │  │  携带位置: [header ▼]                              │  │ │
│  │  │  ┌─ 凭证字段声明 ────────────────────────────┐  │  │ │
│  │  │  │  字段名         必填   敏感（脱敏）        │  │  │ │
│  │  │  │  accessKey       ✓      ✓                  │  │  │ │
│  │  │  │  secretKey       ✓      ✓                  │  │  │ │
│  │  │  │  [+ 添加字段]                              │  │  │ │
│  │  │  └────────────────────────────────────────────┘  │  │ │
│  │  │  💡 凭证值不在此处输入，由调用方在触发请求时携带   │  │ │
│  │  └─────────────────────────────────────────────────────┘  │ │
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

**关键交互变更**（v2.0 → v2.7.5）：
- **基本信息表单**：`name` 单输入框 → `nameCn`/`nameEn` 两个输入框；`description` 单输入框 → `descriptionCn`/`descriptionEn` 两个输入框（建议用 Tabs 切换语言）
- **❌ 凭证值输入字段移除**：原 `AccessKey: [______]` / `SecretKey: [______]` 输入框 → 改为字段声明表（仅声明字段名 + 必填 + 敏感标记）
- **认证类型选择联动**：选择不同 `authTypeSchema.type` 时，预设默认字段声明（AKSK → `accessKey`/`secretKey`；BASIC_AUTH → `username`/`password`；BEARER → `token`；API_KEY → `keyValue`），用户可调整

**交互流程**:
1. 编辑模式加载 → 调用 `GET /api/v1/connectors/{id}/versions/{vid}`
2. 类型选择变化 → 动态切换协议配置表单字段（MVP 仅 HTTP）
3. 认证类型选择变化 → 预设字段声明 + 引导用户调整
4. 点击"保存草稿" → 调用 `PUT /api/v1/connectors/{id}/versions/{vid}` 传 `authTypeSchema`（不含凭证值）
5. 点击"发布" → 弹出版本号 + `versionDescriptionCn`/`versionDescriptionEn` 输入弹窗 → 调用 `POST .../publish`
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
│  [图标]  IM 发送消息 / IM Send Message    [编辑] [删除]     │
│  类型: HTTP | 版本: v1.0.0 (已发布)                          │
│  ── 描述（中/英 Tab 切换） ──────────────────────────────    │
│  封装 IM 消息发送能力 / Encapsulated IM messaging capability │
│                                                              │
│  ┌─ Tab: 连接配置 ── 版本历史 ──────────────────────────┐  │
│  │                                                        │  │
│  │ [版本 v1.0.0 - 已发布] [版本对比]                      │  │
│  │                                                        │  │
│  │ 协议: HTTP                                              │  │
│  │ URL: https://openapi.xxx.com/im/send                    │  │
│  │ 方法: POST                                              │  │
│  │ Headers: { "Content-Type": "application/json" }         │  │
│  │                                                        │  │
│  │ ┌─ 认证类型 Schema ──────────────────────────────┐    │  │
│  │ │  类型: AKSK                                      │    │  │
│  │ │  携带位置: header                               │    │  │
│  │ │  凭证字段:                                       │    │  │
│  │ │  ├─ accessKey  [必填] [敏感字段]                │    │  │
│  │ │  └─ secretKey  [必填] [敏感字段]                │    │  │
│  │ │  💡 凭证值由调用方在触发请求时携带，平台不存储   │    │  │
│  │ └──────────────────────────────────────────────────┘    │  │
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
├── PageHeader (icon, nameCn/nameEn, action buttons)
│   ├── LanguageTab (中/英切换显示名称、描述)
│   ├── Button: 编辑 → /connector-editor?id=xxx
│   └── Button: 删除 → DELETE (确认弹窗)
├── InfoCards (type, version)
├── DescriptionTabs (中/英描述切换)
└── Tabs (Ant Design Tabs)
    ├── Tab: 连接配置 (ConnectionConfigViewer)
    │   ├── VersionSelector (切换版本)
    │   ├── ProtocolInfo (只读展示)
    │   ├── AuthTypeSchemaViewer (只读：类型/位置/字段声明列表)
    │   ├── SchemaViewer (只读 JSON：input/output schema)
    │   └── TimeoutAndRateLimit
    └── Tab: 版本历史 (VersionHistory)
        └── Timeline (Ant Design Timeline)
            └── VersionItem: versionNo + versionStatus(数字→标签) + publishedTime + versionDescriptionCn/En
```

> **与 v2.0 的差异**：
> - 名称/描述双语展示（Tab 切换或左右并排）
> - `AuthInfo (脱敏展示)` → `AuthTypeSchemaViewer`（只展示字段声明，不存储任何凭证值，无需脱敏）
> - 版本历史新增 `versionDescriptionCn`/`versionDescriptionEn` 双语显示

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
   - HTTP: 配置 `authTypeSchema`（认证类型 schema，仅声明类型/字段名/必填/敏感，**不输入凭证值**）+ `inParamSchema`（请求体 Schema，JSON Editor）+ `rateLimit.qpm`（限流）
   - 手动: 无需额外配置（管理员通过界面手动触发）
5. **连接器节点配置**: 点击连接器节点 → 选择已发布连接器版本 → 配置输入参数映射；**节点详情面板展示该连接器需要的凭证字段声明（仅 schema，不输入值）**，提示用户"测试运行/触发时需提供"
6. **数据处理节点配置**: 🆕 点击数据处理节点 → 配置源→目标字段映射
7. **出口节点配置**: 点击出口节点 → 定义返回字段列表
8. **数据映射**: 点击连线 → 弹出 DataMappingDialog → 配置源→目标字段映射
9. **保存草稿**: 点击保存 → 调用 `PUT /api/v1/flows/{id}/versions/{vid}`（全量保存 orchestrationConfig）
10. **测试运行**: 
    - 点击测试运行 → 弹出 **TestRunDialog**（必须包含两个输入区）：
      - **Mock 触发数据**：基于 `trigger.inParamSchema` 生成模板
      - **凭证输入区**（v2.6 决策）：扫描所有连接器节点的 `authTypeSchema`，按 `connectorVersionId` 分组渲染凭证字段输入框（敏感字段用 password input + 复制按钮），用户填写后**仅当次使用**
    - 调用 `POST /api/v1/flows/{id}/test-run`（同步执行）
    - 展示测试结果（步骤列表 + 输入/输出，敏感字段已脱敏为 `***`）
11. **发布**: 点击发布 → 输入版本号 + `versionDescriptionCn`/`En` → 调用 `POST .../publish`（无需审批）
12. **部署**: 发布后点击部署 → 调用 `POST /api/v1/flows/{id}/deploy`（无需审批）

> **与 v2.0 的差异**：
> - 入口触发器仅 HTTP + 手动（移除事件/Webhook/Cron 配置）；HTTP 触发器配置**新增 `authTypeSchema` 子配置面板**（仅声明类型与字段，不输入凭证值）
> - 新增数据处理节点（DataProcessorNode + DataProcessorConfig 面板）
> - 新增部署按钮（FR-013）
> - 测试运行改为同步执行，直接展示结果（不再需要轮询状态）
> - **TestRunDialog 新增凭证输入区**（v2.6）：扫描编排中所有连接器节点的 `authTypeSchema`，让用户在测试时临时输入凭证值，仅本次执行使用，不存储

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
│  │  触发 URL: https://open.xxx.com/api/v1/trigger/        │       │
│  │              1234567890123456789/invoke                │       │
│  │  认证类型: BEARER (Header: Authorization)              │       │
│  │  💡 凭证由调用方携带，平台不存储                        │       │
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
│  执行 ID: 1122334455667788990  [📋 复制]                       │
│  状态: ✓ 成功(status=2)  |  触发方式: HTTP(triggerType=1)      │
│  耗时: 2.25s  |  时间: 2026-05-21 10:00:01                     │
│  CorrelationId: corr_abc123  [🔗 查看全链路]                   │
│                                                                 │
│  ┌─ 执行步骤 ──────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │  ✓ Step 1: 接收请求 / Receive Request  [entry]   10ms   │  │
│  │     ├─ 输入: {"sender":"user_001","content":"你好"}     │  │
│  │     └─ 输出: {"sender":"user_001","content":"你好"}     │  │
│  │                                                          │  │
│  │  ✓ Step 2: 发送通知 / Send Notification  [connector] 2.21s │
│  │     ├─ 输入: {"receiver":"user_001","content":"你好",    │  │
│  │     │         "accessKey":"***(12)","secretKey":"***(32)"}│  │
│  │     │     💡 敏感字段已自动脱敏                          │  │
│  │     └─ 输出: {"$externalized": "9988776655443322110"}    │  │
│  │             [📥 查看完整输出（外置 2.3MB，OSS）]         │  │
│  │             外部资源 ID: msg_xxxx_dingtalk_media_id      │  │
│  │                                                          │  │
│  │  ✓ Step 3: 格式化消息 / Format Message [data_processor] 15ms │
│  │     ├─ 输入: {"msgId":"msg_xxxx"}                        │  │
│  │     └─ 输出: {"result":{"id":"msg_xxxx","status":"ok"}}  │  │
│  │                                                          │  │
│  │  ✓ Step 4: 返回结果 / Return Result  [exit]      5ms    │  │
│  │     ├─ 输入: {"result":{"id":"msg_xxxx","status":"ok"}}  │  │
│  │     └─ 输出: {"result":{"id":"msg_xxxx","status":"ok"}}  │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  返回值: {"result":{"id":"msg_xxxx","status":"ok"}}             │
└─────────────────────────────────────────────────────────────────┘
```

**组件树**:
```
ExecutionDetail
├── PageHeader (executionId, status badge—数字→标签转译)
│   ├── CopyIdButton (复制完整数字 ID)
│   └── CorrelationLink (跨服务链路追踪)
├── SummaryCards (status, triggerType, durationMs, time)
├── ExecutionTimeline (Ant Design Steps: vertical)
│   └── ExecutionStep (status icon, stepOrder, nodeNameCn/En, nodeType—数字→标签转译, durationMs)
│       ├── Collapse: 输入数据 (DataViewer)
│       │   ├── InlineJsonView (小字段直接 JSON 展示)
│       │   ├── ExternalizedBadge (检测 {"$externalized": "<blobId>"} 标记 → 显示"查看完整内容（外置 N MB）"按钮)
│       │   │   └── BlobViewer (点击后调 GET /api/v1/blobs/{blobId} 拉取 OSS 内容预览)
│       │   ├── ExternalResourceIdBadge (若 blob 含 externalResourceId → 显示"外部资源 ID: xxx" + 提供复制按钮)
│       │   └── MaskedFieldIndicator (检测 *** 模式 → 提示"敏感字段已脱敏")
│       └── Collapse: 输出数据 (同上)
│       └── Collapse: 错误信息 (失败时；展示 errorInfo 结构化字段：code/message/downstreamStatus/downstreamBody)
├── ResultDisplay (返回值 JSON，同样支持外置感知)
```

**关键交互**：

| 场景 | 前端行为 |
|------|---------|
| 数据小于 64KB | 直接 inline 展示 JSON |
| 数据 ≥ 64KB（`{"$externalized": "<blobId>"}`）| 显示"📥 查看完整内容（外置 N MB，OSS）"按钮 → 点击调 `GET /api/v1/blobs/{blobId}` 拉取 |
| Blob 有 `externalResourceId` | 在 blob 展示区显示"外部资源 ID: xxx"标签（用于反查溯源，可点击复制） |
| 敏感字段（值为 `***(N)` 模式） | 显示锁图标 + "敏感字段已脱敏" tooltip，告知用户原值长度但不展示原值 |
| 步骤失败 | 错误信息 Collapse 默认展开，结构化展示 `errorInfo.code` / `message` / `downstreamStatus` / `downstreamBody` |

> **与 v2.0 的差异**：
> - 执行模型改为同步（数据是一次性返回的，不需要轮询状态）
> - 新的步骤类型 `data_processor` 显示
> - 状态/类型从字符串改为数字 + 前端字典转译展示
> - **新增大字段外置感知**（v2.4）：检测 `$externalized` 标记并提供 BlobViewer 按需拉取
> - **新增外部资源 ID 展示**（v2.7.5）：blob 含 `externalResourceId` 时展示并提供复制（用于反查外部系统）
> - **新增敏感字段脱敏指示器**（v2.6）：检测 `***` 模式提示用户
> - 节点名称双语显示 `nodeNameCn`/`nodeNameEn`
> - 新增 `correlationId` 链路追踪入口
> - 步骤新增 `stepOrder` 显示
> - 错误信息从单一 `errorMessage` 字符串升级为结构化 `errorInfo` JSON 展示
> - 移除"重试执行"按钮（失败重试 NG15，V1 阶段引入）

---

### 3.8 监控面板 (`MonitorDashboard`)

**路由**: `/connect/monitor` (🆕 需新增)  
**对应 FR**: FR-025 (执行历史查询)

> ⚠️ **与 v2.0 的差异**：监控面板从全指标仪表盘**简化为执行历史查询 + 精简统计**（spec v4.0 FR-025 仅定义执行历史查询）。全指标仪表盘（活跃流/总执行/成功率/平均耗时/执行趋势图表/按连接器统计）移至 V1。

**页面结构**:
```
┌──────────────────────────────────────────────────────────────┐
│  执行历史监控                     [时间范围: 最近 7 天 ▼]    │
│                                                              │
│  ┌─ 统计概览（精简版）──────────────────────────────────┐  │
│  │  总执行次数: 4,523  |  成功(status=2): 4,455           │  │
│  │  失败(status=3): 45 |  超时(status=4): 23              │  │
│  │  成功率: 98.5%      |  平均耗时: 1.85s                 │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌─ 执行列表 ──────────────────────────────────────────────┐  │
│  │  [状态筛选: 全部▼] [触发方式筛选: 全部▼]                 │  │
│  │                                                          │  │
│  │  执行ID         时间              触发方式  状态   耗时   │  │
│  │  1122334455...  2026-05-21 10:00  HTTP      ✓ 成功 2.2s  │  │
│  │  1122334456...  2026-05-21 09:55  HTTP      ✓ 成功 1.8s  │  │
│  │  1122334457...  2026-05-21 09:50  手动      ✕ 失败 5.0s  │  │
│  │                                                          │  │
│  │  [分页: < 1 2 3 ... 15 >]                                │  │
│  └─────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

**组件树**:
```
MonitorDashboard
├── TimeRangeSelector (Select: 24h/7d/30d)
├── StatSummary (精简统计行：总执行/成功/失败/超时/成功率/平均耗时；前端基于查询结果聚合)
└── ExecutionHistoryTable
    ├── FilterBar (status TINYINT filter, triggerType TINYINT filter)
    ├── Table
    │   ├── Column: executionId (Link → /connect/flows/:flowId/executions/:id；显示前 12 位 + ... 截断)
    │   ├── Column: startedTime
    │   ├── Column: triggerType (Tag: 数字→标签转译，1→HTTP, 2→手动, 3→测试)
    │   ├── Column: status (Badge: 数字→标签 + 颜色，2→绿✓成功, 3→红✕失败, 4→橙⏱超时)
    │   └── Column: durationMs
    └── Pagination
```

> **与 v2.0 的差异**：
> - 列表字段名/类型对齐 API：`executionId` 数字 string、`startedTime` 替代 `startedAt`、`triggerType`/`status` 用 TINYINT 数字 + 前端字典转译
> - ID 显示截断（前 12 位 + ...），完整 ID 在 Tooltip 或详情页展示
> - 移除复杂指标图表（移至 V1）

---

## 4. 新增组件

> 💡 **已有组件**（`wecodesite/src/pages/ConnectPlatform/` 已实现）：`Connector/index.jsx`、`ConnectorEditor/index.jsx`、`Flow/index.jsx`、`FlowEditor/index.jsx`、`FlowEditor/customNodes.jsx`、`FlowEditor/NodeLibrary.jsx`、`FlowEditor/NodeProperties.jsx`

### 4.1 页面级组件（`wecodesite/src/pages/ConnectPlatform/`）

| 组件 | 功能 | 状态 |
|------|------|------|
| `FlowDetail.jsx` | 连接流详情页面 | 🆕 需新增 |
| `ExecutionDetail.jsx` | 执行详情页面（步骤输入/输出/返回值；**含大字段外置感知 + externalResourceId 展示 + 敏感字段脱敏指示器**） | 🆕 需新增 |
| `FlowEditor/DataMappingDialog.jsx` | 数据映射配置弹窗 | 🆕 需新增 |
| `FlowEditor/TestRunDialog.jsx` | **测试运行弹窗（Mock 数据输入 + 凭证临时输入区 + 结果展示）** | 🆕 需新增 |
| `Monitor/MonitorDashboard.jsx` | 执行历史监控面板（精简版） | 🆕 需新增 |
| `Monitor/index.jsx` | 监控页面入口 | 🆕 需新增 |
| `Monitor/constants.jsx` | 监控页面常量 | 🆕 需新增 |
| `Monitor/thunk.js` | 监控数据请求 | 🆕 需新增 |

### 4.2 编排画布节点组件扩展（`wecodesite/src/pages/ConnectPlatform/FlowEditor/`）

| 组件 | 位置 | 说明 | 状态 |
|------|------|------|------|
| `DataProcessorNodeComponent` | `customNodes.jsx` | 🆕 数据处理节点自定义组件（带字段映射图形标识） | 🆕 |
| `DataProcessorConfig` | `NodeProperties.jsx` | 🆕 数据处理节点配置面板 | 🆕 |
| `FieldMappingEditor` | `NodeProperties.jsx` | 🆕 字段映射编辑器（源→目标字段配对，支持 `${node_x.xxx}` / `${trigger.xxx}` 引用） | 🆕 |
| `AuthTypeSchemaEditor` | `NodeProperties.jsx` / `ConnectorEditor` | 🆕 **认证类型 schema 编辑器**（仅声明字段名/必填/敏感标记，不输入凭证值，v2.6） | 🆕 |
| `AuthTypeSchemaViewer` | `ConnectorEditor`（详情态） | 🆕 认证类型 schema 只读展示器 | 🆕 |
| `TriggerAuthConfig` | `NodeProperties.jsx` | 🆕 入口节点 HTTP 触发的 authTypeSchema + rateLimit 配置子面板 | 🆕 |

### 4.3 通用辅助组件（`wecodesite/src/pages/ConnectPlatform/common/`）

| 组件 | 说明 | 状态 |
|------|------|------|
| `BilingualTextInput` | 双语文本输入器（`*Cn`/`*En` Tab 切换或左右并排，配合 `nameCn/nameEn`/`descriptionCn/descriptionEn`） | 🆕 |
| `BilingualTextViewer` | 双语文本只读展示器（按当前语言切换或并排展示） | 🆕 |
| `EnumLabelMapper` | TINYINT 枚举数字 → 本地化标签转译器（基于字典配置；详见 plan-api.md §1.4 枚举数字字典） | 🆕 |
| `BigintIdDisplay` | BIGINT 雪花 ID 显示器（前 12 位 + ... 截断；Tooltip 显示完整 ID；含复制按钮） | 🆕 |
| `BlobViewer` | 大字段外置内容查看器（检测 `{"$externalized": "<blobId>"}` 标记 → 拉取 `GET /api/v1/blobs/{blobId}` 展示；展示元数据：`uri`/`sizeBytes`/`contentType`/`externalResourceId`） | 🆕 |
| `ExternalResourceIdBadge` | 外部资源 ID 标签展示（来源于 `storageBlobRef.externalResourceId`，含复制按钮，用于反查外部系统） | 🆕 |
| `MaskedFieldIndicator` | 敏感字段脱敏指示器（检测 `***` 模式 → 显示锁图标 + tooltip） |  🆕 |
| `StructuredErrorViewer` | 结构化错误信息查看器（展示 `errorInfo.code` / `message` / `downstreamStatus` / `downstreamBody` 等字段） | 🆕 |

---

## 5. 状态管理 (thunk.js 模式)

> 💡 `wecodesite` 采用 `thunk.js` 模式管理数据和状态（每个页面目录下包含 `constants.jsx` + `thunk.js` + `mock.js`）。以下为新增模块的 thunk 设计。
>
> ⚠️ **API 字段对齐**：所有 thunk 请求/响应字段使用 camelCase（详见 plan-api.md §1.2）；ID 字段使用 string 类型（避免 JS 精度丢失）；枚举字段使用 TINYINT 数字（前端通过 `EnumLabelMapper` 转译）。

### 连接器 thunk（`wecodesite/src/pages/ConnectPlatform/Connector/`）

```js
// constants.jsx — 补充 action types
export const FETCH_CONNECTORS = 'FETCH_CONNECTORS';
export const FETCH_CONNECTOR_DETAIL = 'FETCH_CONNECTOR_DETAIL';
export const CREATE_CONNECTOR = 'CREATE_CONNECTOR';
export const UPDATE_CONNECTOR = 'UPDATE_CONNECTOR';
export const DELETE_CONNECTOR = 'DELETE_CONNECTOR';
export const FETCH_VERSIONS = 'FETCH_VERSIONS';
export const UPDATE_VERSION_CONFIG = 'UPDATE_VERSION_CONFIG';   // PUT connectionConfig（含 authTypeSchema，不含凭证值）
export const PUBLISH_VERSION = 'PUBLISH_VERSION';
```

```js
// thunk.js — 补充 API 调用
export const fetchConnectors = (params) => async (dispatch) => { /* GET /api/v1/connectors */ };
export const fetchConnectorDetail = (id) => async (dispatch) => { /* GET /api/v1/connectors/{id} */ };
export const createConnector = (data) => async (dispatch) => { /* POST /api/v1/connectors，body 含 nameCn/nameEn/descriptionCn/descriptionEn/iconUrl/connectorType */ };
export const updateConnector = (id, data) => async (dispatch) => { /* PUT /api/v1/connectors/{id} */ };
export const deleteConnector = (id) => async (dispatch) => { /* DELETE /api/v1/connectors/{id} */ };
export const updateVersionConfig = (connectorId, versionId, connectionConfig) => async (dispatch) => { /* PUT .../versions/{vid}，含 authTypeSchema 但不含凭证值 */ };
export const publishVersion = (connectorId, versionId, payload) => async (dispatch) => { /* POST .../publish，含 versionNo/versionDescriptionCn/versionDescriptionEn */ };
```

> **与 v2.0 差异**：
> - 移除 `LIST_PUBLIC`, `DELIST`, `FETCH_STATS` action types（无上架/下架/统计）
> - **❌ 不再有 `SAVE_CREDENTIAL` / `FETCH_CREDENTIALS` 等凭证相关 action**（v2.6 凭证不持久化）
> - `UPDATE_VERSION_CONFIG` 入参 `connectionConfig.authTypeSchema` 仅声明类型与字段，前端必须 validate 凭证值字段未填写

### 连接流 thunk（`wecodesite/src/pages/ConnectPlatform/Flow/` + `FlowEditor/`）

```js
// constants.jsx — 补充 action types
export const FETCH_FLOWS = 'FETCH_FLOWS';
export const FETCH_FLOW_DETAIL = 'FETCH_FLOW_DETAIL';
export const CREATE_FLOW = 'CREATE_FLOW';
export const UPDATE_FLOW = 'UPDATE_FLOW';
export const DELETE_FLOW = 'DELETE_FLOW';
export const FETCH_VERSIONS = 'FETCH_VERSIONS';
export const SAVE_ORCHESTRATION_CONFIG = 'SAVE_ORCHESTRATION_CONFIG'; // PUT 编排配置（trigger/nodes/edges 整体）
export const PUBLISH_VERSION = 'PUBLISH_VERSION';
export const DEPLOY_FLOW = 'DEPLOY_FLOW';          // POST /flows/{id}/deploy
export const START_FLOW = 'START_FLOW';            // POST /flows/{id}/start
export const STOP_FLOW = 'STOP_FLOW';              // POST /flows/{id}/stop
export const MANUAL_EXECUTE = 'MANUAL_EXECUTE';    // POST /flows/{id}/executions（同步，带 credentials）
export const TEST_RUN = 'TEST_RUN';                // POST /flows/{id}/test-run（同步，带 credentials）
export const FETCH_EXECUTIONS = 'FETCH_EXECUTIONS';
export const FETCH_EXECUTION_DETAIL = 'FETCH_EXECUTION_DETAIL';
export const FETCH_BLOB = 'FETCH_BLOB';            // GET /api/v1/blobs/{blobId}（大字段外置内容拉取）
```

```js
// thunk.js — 补充 API 调用
export const fetchFlows = (params) => async (dispatch) => { /* GET /api/v1/flows */ };
export const fetchFlowDetail = (id) => async (dispatch) => { /* GET /api/v1/flows/{id} */ };
export const createFlow = (data) => async (dispatch) => { /* POST /api/v1/flows，body 含 nameCn/nameEn/descriptionCn/descriptionEn */ };
export const updateFlow = (id, data) => async (dispatch) => { /* PUT /api/v1/flows/{id} */ };
export const deleteFlow = (id) => async (dispatch) => { /* DELETE /api/v1/flows/{id} */ };
export const saveOrchestrationConfig = (flowId, versionId, orchestrationConfig) => async (dispatch) => { /* PUT .../versions/{vid}，trigger 内嵌于 orchestrationConfig.trigger */ };
export const publishVersion = (flowId, versionId, payload) => async (dispatch) => { /* POST .../publish */ };
export const deployFlow = (flowId, versionId) => async (dispatch) => { /* POST .../deploy */ };
export const startFlow = (id) => async (dispatch) => { /* POST .../start */ };
export const stopFlow = (id) => async (dispatch) => { /* POST .../stop */ };
export const manualExecute = (flowId, payload) => async (dispatch) => { /* POST .../executions，body 含 triggerData + credentials（按 connectorVersionId 分组）*/ };
export const testRun = (flowId, payload) => async (dispatch) => { /* POST .../test-run，body 含 mockTriggerData + credentials */ };
export const fetchExecutions = (flowId, params) => async (dispatch) => { /* GET .../executions */ };
export const fetchExecutionDetail = (execId) => async (dispatch) => { /* GET /executions/{execId} */ };
export const fetchBlob = (blobId) => async (dispatch) => { /* GET /api/v1/blobs/{blobId}（懒加载大字段外置内容）*/ };
```

> **与 v2.0 差异**：
> - 移除 `ENABLE_FLOW` / `DISABLE_FLOW` → 拆分为 `START_FLOW` / `STOP_FLOW`
> - 移除 `TRIGGER_MANUAL` → 改为 `MANUAL_EXECUTE`（同步，请求体含 `credentials`，v2.6）
> - 新增 `DEPLOY_FLOW`
> - 新增 `FETCH_EXECUTIONS`, `FETCH_EXECUTION_DETAIL`
> - **新增 `FETCH_BLOB`**（v2.4 大字段外置感知；ExecutionDetail 页面按需调用）
> - `SAVE_CANVAS` → `SAVE_ORCHESTRATION_CONFIG`（语义更准确；触发器配置内嵌于 `orchestrationConfig.trigger`，v2.7.3）
> - `testRun` / `manualExecute` 请求体新增 `credentials` 字段（按 connectorVersionId 分组）

---

## 6. 服务层 API 封装

| 模块 | 主要导出 | 位置 |
|------|---------|------|
| 连接器 API | `fetchConnectors`, `fetchConnectorDetail`, `createConnector`, `updateConnector`, `deleteConnector`, `fetchVersions`, `updateVersionConfig`（含 authTypeSchema 不含凭证值）, `publishVersion`（含 versionDescriptionCn/En） | `ConnectPlatform/Connector/thunk.js` |
| 连接流 API | `fetchFlows`, `fetchFlowDetail`, `createFlow`, `updateFlow`, `deleteFlow`, `saveOrchestrationConfig`（trigger 内嵌）, `publishVersion`, `deployFlow`, `startFlow`, `stopFlow` | `ConnectPlatform/Flow/thunk.js` |
| 运行时 API | `manualExecute`（带 credentials）, `testRun`（带 credentials）, `fetchExecutionDetail`, `fetchExecutionList` | `ConnectPlatform/Flow/thunk.js` |
| 监控 API | `fetchExecutionHistory`（执行列表 + 前端聚合精简统计） | `ConnectPlatform/Monitor/thunk.js` |
| 大字段外置 API | `fetchBlob`（GET /api/v1/blobs/{blobId}）；BlobViewer 组件按需调用 | 通用 utils 层 |

---

## 附录 A：修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| **v2.0** | 2026-05-21 | 初始版本——对齐 spec.md v4.0：触发器仅 HTTP/手动，新增数据处理节点，监控简化为执行历史查询，FR 重编号 | SDDU Plan Agent |
| **v2.7.5** | **2026-05-22** | **全面对齐 plan.md v2.7.5（含 v2.0 → v2.7.5 全部决策）**，本次为彻底对齐重写。核心变更：① **§0 重写**版本对齐说明，列出全部 v2.0 → v2.7.5 变更项；② **§3.2 连接器编辑器** 基本信息表单加双语字段（`nameCn`/`nameEn` + `descriptionCn`/`descriptionEn`）+ `iconUrl` + `tags`；**❌ 凭证值输入字段彻底移除**（v2.6），改为认证类型 schema 字段声明表（仅声明字段名/必填/敏感标记）；③ **§3.3 连接器详情** 加双语展示 + `AuthInfo (脱敏展示)` → `AuthTypeSchemaViewer`（只展示字段声明，无需脱敏）+ 版本历史含 `versionDescriptionCn`/`En`；④ **§3.5 编排画布** 触发器配置增加 `authTypeSchema` 子面板（v2.7.3）；**TestRunDialog 新增凭证临时输入区**（v2.6，扫描连接器节点 authTypeSchema 让用户填入临时凭证仅当次使用）；保存改为 `saveOrchestrationConfig`（trigger 内嵌于 orchestrationConfig）；版本号弹窗加双语 `versionDescription`；⑤ **§3.7 执行详情** 新增大字段外置感知（检测 `{"$externalized": "<blobId>"}` 显示 BlobViewer 按需拉取）+ **`externalResourceId` 标识展示**（v2.7.5，外部资源 ID 反查溯源）+ 敏感字段脱敏指示器（v2.6，检测 `***` 模式）+ 状态/类型从字符串改为 TINYINT 数字 + 前端字典转译展示 + 节点名称双语 `nodeNameCn`/`En` + `correlationId` 链路追踪 + `stepOrder` + `errorMessage` 升级为结构化 `errorInfo`；⑥ **§3.8 监控面板** 精简版（保留统计概览 + 执行列表，移除复杂图表）；ID 显示前 12 位 + ... 截断；状态/触发方式用 TINYINT 数字 + 前端字典转译；⑦ **§4 新增组件清单**重组为 4.1 页面级 / 4.2 编排画布扩展 / **4.3 通用辅助组件**（新增 8 个通用组件：`BilingualTextInput`/`BilingualTextViewer`/`EnumLabelMapper`/`BigintIdDisplay`/`BlobViewer`/`ExternalResourceIdBadge`/`MaskedFieldIndicator`/`StructuredErrorViewer`）；⑧ **§5 状态管理**：thunk action types/方法签名全面对齐 API（`SAVE_CANVAS` → `SAVE_ORCHESTRATION_CONFIG`、新增 `MANUAL_EXECUTE`/`FETCH_BLOB`、`testRun`/`manualExecute` 入参新增 `credentials` 按 connectorVersionId 分组）；**移除凭证持久化相关 action**（无 `SAVE_CREDENTIAL`/`FETCH_CREDENTIALS`）；⑨ **§6 服务层** 字段对齐；⑩ 顶部版本号 v2.0 → v2.7.5；⑪ 修订记录追加 v2.7.5 条目。**未变更项**：§1 设计总则 / §2 路由设计 / §3.1 连接器目录 / §3.4 连接流列表 / §3.6 连接流详情（基础结构） | SDDU Plan Agent |