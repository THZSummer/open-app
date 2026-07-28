# 单节点流 — 调用真实 open-server 接口

## 1. 背景

单节点流模式，连接器调用真实的 open-server OpenAPI（`GET /service/open/v2/connectors`），不使用 Mock。Trigger 接收 Header/Query/Body 三段参数，Connector 透传至 open-server，Exit 同时输出 Body 和 Header。全流程通过前端 UI 操作完成。

参照：`open-server/src/test/python/e2e/test_resource_query_branch.py`

## 2. 业务场景

### 2.1 流程拓扑

```
Trigger (HTTP, header+query+body) → Connector (GET open-server /connectors) → Exit (body+header)
```

### 2.2 数据流（逐字段追踪）

#### ① 调用方发起请求

```
POST /api/v1/flows/{flowId}/invoke?keyword=&pageSize=3

Header:
  X-App-Id:          20250730213114178360970
  Cookie:            user_id=admin
  X-XSRF-TOKEN:      user_id=admin
  X-Sys-Token:       tester                    ← SYSTOKEN 认证（不进 flow 数据流）

Body:
  {"X-Echo-To-Header": "echo-test"}
```

#### ② Trigger 节点接收（三段分区）

```
trigger.input.header:
  X-App-Id        = "20250730213114178360970"    ← 来自调用方 Header
  Cookie          = "user_id=admin"              ← 来自调用方 Header
  X-XSRF-TOKEN    = "user_id=admin"              ← 来自调用方 Header

trigger.input.query:
  keyword         = ""                           ← 来自调用方 Query  ?keyword=
  pageSize        = 3                            ← 来自调用方 Query  ?pageSize=3

trigger.input.body:
  X-Echo-To-Header = "echo-test"                 ← 来自调用方 Body   {"X-Echo-To-Header":"echo-test"}
```

#### ③ Connector 节点：发起真实 HTTP 调用

Connector 配置了 URL `http://localhost:18080/open-server/service/open/v2/connectors`、Method `GET`，Input 映射如下（在 FlowEditorV2 中逐个字段配置表达式）：

```
实际发出的 HTTP 请求:
  GET /service/open/v2/connectors?keyword=&pageSize=3

  请求 Header:
    X-App-Id       = "${$.node.trigger.input.header.X-App-Id}"        → "20250730213114178360970"
    Cookie         = "${$.node.trigger.input.header.Cookie}"          → "user_id=admin"
    X-XSRF-TOKEN   = "${$.node.trigger.input.header.X-XSRF-TOKEN}"   → "user_id=admin"

  请求 Query:
    keyword        = "${$.node.trigger.input.query.keyword}"          → ""
    pageSize       = "${$.node.trigger.input.query.pageSize}"         → 3
```

#### ④ open-server 返回响应

open-server `/connectors` 接口返回：

```
HTTP 200
响应 Header:
  Date:             Tue, 28 Jul 2026 03:00:00 GMT
  Content-Type:     application/json

响应 Body:
{
  "code":      "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "page": {
    "curPage":   1,
    "pageSize":  3,
    "total":     7003,
    "totalPages": 2335
  },
  "data": [
    {"connectorId": "340439125289074688", "nameCn": "测试连接器A", "status": 2, ...},
    {"connectorId": "340439237046304768", "nameCn": "测试连接器B", "status": 2, ...},
    {"connectorId": "340439243736219648", "nameCn": "测试连接器C", "status": 2, ...}
  ]
}
```

Connector 节点收到响应后存入自己的 output 分区：

```
conn.output.header:
  Date          = "Tue, 28 Jul 2026 03:00:00 GMT"
  Content-Type  = "application/json"

conn.output.body:
  code          = "200"
  messageZh     = "操作成功"
  messageEn     = "Success"
  page.total    = 7003
  page.totalPages = 2335
  data          = [{connectorId:..., nameCn:..., status:2}, ...]
```

#### ⑤ Exit 节点：提取 Connector 输出 + Trigger 原始值，构造最终响应

Exit 节点 Output 映射如下（在 FlowEditorV2 中逐个字段配置表达式）：

```
Exit Body 字段:
  code          ← ${$.node.conn.output.body.code}          = "200"
  messageZh     ← ${$.node.conn.output.body.messageZh}     = "操作成功"
  total         ← ${$.node.conn.output.body.page.total}    = 7003
  items         ← ${$.node.conn.output.body.data}          = [{...}, {...}, {...}]

Exit Header 字段:
  X-Echo-To-Header ← ${$.node.trigger.input.body.X-Echo-To-Header}   = "echo-test"
  X-Connector-Date ← ${$.node.conn.output.header.Date}               = "Tue, 28 Jul 2026 03:00:00 GMT"
```

#### ⑥ 调用方收到最终响应

```
HTTP 200
响应 Header:
  X-Echo-To-Header:   echo-test                       ← exit header 映射
  X-Connector-Date:   Tue, 28 Jul 2026 03:00:00 GMT   ← exit header 映射

响应 Body:
{
  "code":      "200",
  "messageZh": "操作成功",
  "total":     7003,
  "items": [
    {"connectorId": "340439125289074688", "nameCn": "测试连接器A", "status": 2},
    {"connectorId": "340439237046304768", "nameCn": "测试连接器B", "status": 2},
    {"connectorId": "340439243736219648", "nameCn": "测试连接器C", "status": 2}
  ]
}
```

#### ⑦ 完整表达式映射速查表

| 节点 | 位置 | 字段 | 表达式 | 值来源 |
|------|------|------|--------|--------|
| Connector | Header | `X-App-Id` | `${$.node.trigger.input.header.X-App-Id}` | 调用方 Header |
| Connector | Header | `Cookie` | `${$.node.trigger.input.header.Cookie}` | 调用方 Header |
| Connector | Header | `X-XSRF-TOKEN` | `${$.node.trigger.input.header.X-XSRF-TOKEN}` | 调用方 Header |
| Connector | Query | `keyword` | `${$.node.trigger.input.query.keyword}` | 调用方 Query |
| Connector | Query | `pageSize` | `${$.node.trigger.input.query.pageSize}` | 调用方 Query |
| Exit | Body | `code` | `${$.node.conn.output.body.code}` | open-server 响应 |
| Exit | Body | `messageZh` | `${$.node.conn.output.body.messageZh}` | open-server 响应 |
| Exit | Body | `total` | `${$.node.conn.output.body.page.total}` | open-server 响应 |
| Exit | Body | `items` | `${$.node.conn.output.body.data}` | open-server 响应 |
| Exit | Header | `X-Echo-To-Header` | `${$.node.trigger.input.body.X-Echo-To-Header}` | 调用方 Body |
| Exit | Header | `X-Connector-Date` | `${$.node.conn.output.header.Date}` | open-server 响应头 |

### 2.3 入参

| 位置 | 字段 | 类型 | 必填 | 说明 |
|------|------|------|:--:|------|
| Header | `X-App-Id` | `string` | 是 | 应用 ID |
| Header | `Cookie` | `string` | 否 | 用户身份 `user_id=admin` |
| Header | `X-XSRF-TOKEN` | `string` | 否 | CSRF 令牌，与 Cookie 一致 |
| Query | `keyword` | `string` | 否 | 搜索关键词 |
| Query | `pageSize` | `number` | 否 | 每页条数，默认 3 |
| Body | `X-Echo-To-Header` | `string` | 否 | 透传回响应头 |

### 2.4 响应

| 位置 | 字段 | 类型 | 来源 |
|------|------|------|------|
| Body | `code` | `string` | `${$.node.conn.output.body.code}` |
| Body | `messageZh` | `string` | `${$.node.conn.output.body.messageZh}` |
| Body | `total` | `number` | `${$.node.conn.output.body.page.total}` |
| Body | `items` | `array` | `${$.node.conn.output.body.data}` |
| Header | `X-Echo-To-Header` | `string` | `${$.node.trigger.input.body.X-Echo-To-Header}` |
| Header | `X-Connector-Date` | `string` | `${$.node.conn.output.header.Date}` |

## 3. 配置详情（按页面操作顺序）

> 以下为 2026-07-28 实测 UI 对应的操作步骤。每个步骤描述一次屏幕上的交互。
> ConnectorEditor 和 FlowEditor 均需**先点击「创建草稿」**才能进入配置界面。
> 所有页面 URL 均需带 `appId` 参数。

---

### 阶段一：创建连接器

#### 步骤 1.1 — 打开连接器列表
```
URL:  http://192.168.3.110:5173/#/connectorList?appId=20250730213114178360970
操作: 浏览器导航到此地址，等待表格加载完成
```

#### 步骤 1.2 — 新建连接器（Modal）
```
操作: 点击按钮「新建连接器」
弹出: Modal 对话框
填写:
  ┌────────────────┬─────────────────────────────────┐
  │ 字段 (Label)    │ 值                               │
  ├────────────────┼─────────────────────────────────┤
  │ 中文名称         │ E2E_QueryConnectors              │
  │ 英文名称         │ e2e_query_connectors             │
  │ (类型默认 HTTP)  │ 不操作（默认值已是 HTTP）          │
  └────────────────┴─────────────────────────────────┘
操作: 点击「保存」
结果: Modal 关闭 → 表格刷新 → 表中出现新行
取参: 从表格行中正则提取连接器 ID（行首数字）
      如: 340460XXXX → connector_id
```

#### 步骤 1.3 — 进入连接器编辑器 + 创建草稿 + 进入编辑模式
```
URL:  http://192.168.3.110:5173/#/connectorEditor?appId=20250730213114178360970&id={connector_id}
操作: 页面显示「当前版本 暂无版本，点击"创建草稿"开始配置」
操作: 点击「创建草稿」→ 版本变为 v1 草稿
操作: 点击「编 辑」→ 进入编辑模式，出现「取消编辑」和「保 存」按钮
结果: 表单变为可编辑状态，显示「基础配置」区 + 两个 Schema 配置区
```

#### 步骤 1.4 — 基础配置
```
当前状态: 已进入编辑模式（顶部有「取消编辑」「保 存」按钮）
页面结构: 从上到下分为三个区块:
  ① 基础配置 — 协议类型、协议地址、认证方式
  ② 入参配置 — Tab 组: HTTP 请求头 | HTTP 请求体 | URL 查询参数
  ③ 出参配置 — Tab 组: HTTP 响应头 | HTTP 响应体

① 基础配置:
  ┌──────────────────────┬───────────────────┬─────────────────────────────┐
  │ 元素                  │ 类型               │ 操作                         │
  ├──────────────────────┼───────────────────┼─────────────────────────────┤
  │ 协议类型               │ Radio (4 个按钮)    │ 点击「GET」（默认已选中）       │
  │ 协议地址               │ Input              │ 填入 URL (见下方)             │
  │ 认证方式（可多选）       │ Checkbox (4 个)     │ 全不勾选 (NONE)              │
  └──────────────────────┴───────────────────┴─────────────────────────────┘

URL 值: http://localhost:18080/open-server/service/open/v2/connectors
```

#### 步骤 1.5 — 入参配置：HTTP 请求头
```
操作: 找到「入参配置」区域 → 点击 Tab「HTTP 请求头」
操作: 点击「+ 添加参数」
每行结构: [参数名称 Input] [类型 Select: string/number/boolean/object/array]
逐行添加:
  ┌─────────────┬──────────┬──────┐
  │ 参数名称       │ 类型      │ 说明  │
  ├─────────────┼──────────┼──────┤
  │ X-App-Id    │ string   │      │
  │ Cookie      │ string   │      │
  │ X-XSRF-TOKEN│ string   │      │
  └─────────────┴──────────┴──────┘
```

#### 步骤 1.6 — 入参配置：URL 查询参数
```
操作: 点击 Tab「URL 查询参数」→ 点击「+ 添加参数」
逐行添加:
  ┌──────────┬──────────┬──────┐
  │ 参数名称   │ 类型      │ 说明  │
  ├──────────┼──────────┼──────┤
  │ keyword  │ string   │      │
  │ pageSize │ number   │      │
  └──────────┴──────────┴──────┘
```

#### 步骤 1.7 — 出参配置：HTTP 响应体
```
操作: 找到「出参配置」区域 → 点击 Tab「HTTP 响应体」
操作: 点击「+ 添加参数」
逐行添加:
  ┌───────────┬──────────┬──────┐
  │ 参数名称    │ 类型      │ 说明  │
  ├───────────┼──────────┼──────┤
  │ code      │ string   │      │
  │ messageZh │ string   │      │
  │ total     │ number   │      │
  │ items     │ array    │      │
  └───────────┴──────────┴──────┘
```

#### 步骤 1.8 — 出参配置：HTTP 响应头
```
操作: 点击 Tab「HTTP 响应头」→ 点击「+ 添加参数」
逐行添加:
  ┌──────┬──────────┬──────┐
  │ 参数名称│ 类型      │ 说明  │
  ├──────┼──────────┼──────┤
  │ Date │ string   │      │
  └──────┴──────────┴──────┘
```

#### 步骤 1.9 — 保存并发布连接器版本
```
操作: 点击「保 存」（保存草稿）
操作: 点击「发 布」→ 弹出确认 Modal「确认发布...是否继续？」
操作: 点击「确 定」
结果: 版本进入审批流程（审批通过后状态变为"已发布"，才可在 FlowEditor 中选用）
```

---

### 阶段二：创建连接流

#### 步骤 2.1 — 打开连接流列表
```
URL:  http://192.168.3.110:5173/#/flowList?appId=20250730213114178360970
操作: 浏览器导航到此地址，等待表格加载完成
```

#### 步骤 2.2 — 新建连接流（Modal）
```
操作: 点击按钮「新建连接流」
弹出: Modal 对话框
填写:
  ┌────────────┬─────────────────────┐
  │ 字段         │ 值                   │
  ├────────────┼─────────────────────┤
  │ 中文名称      │ E2E_SingleNode_Flow │
  │ 英文名称      │ e2e_single_node_flow│
  └────────────┴─────────────────────┘
操作: 点击「保存」
结果: Modal 关闭 → 表格刷新 → 表中出现新行
取参: 从表格行中正则提取连接流 ID（行首数字）
```

#### 步骤 2.3 — 进入 FlowEditor + 创建草稿
```
URL:  http://192.168.3.110:5173/#/flowEditor?appId=20250730213114178360970&id={flow_id}
操作: 页面显示「当前版本 暂无版本，点击"创建草稿"开始配置」
操作: 点击「创建草稿」
结果: 页面进入编排模式，显示模式选择卡片
```

#### 步骤 2.4 — 选择编排模式
```
可见: 3 张 mode-card 卡片
  ┌──────────────────────────────────────────────┐
  │ Ⅰ 单节点                                       │
  │ 触发器 → 连接器 → 数据输出，节点结构固定            │
  ├──────────────────────────────────────────────┤
  │ → 串行编排                                     │
  │ 按顺序追加连接器和脚本节点                         │
  ├──────────────────────────────────────────────┤
  │ ∥ 并行编排                                     │
  │ 触发器 → 并行节点 → 数据输出                      │
  └──────────────────────────────────────────────┘
操作: 点击「单节点」卡片
结果: 页面变为单节点编排画布（展示 Trigger / Connector / Exit 三节点）
```

#### 步骤 2.5 — 配置 Connector 节点（选择连接器 + 填写 Input 映射）
```
操作: 在 Connector 卡片中找到连接器下拉选择框
操作: 打开下拉 → 选择「E2E_QueryConnectors」

操作: 对每个入参字段逐个填写表达式:
  ┌─────────────┬───────────────────────────────────────┐
  │ 字段 (Label)  │ 表达式 (值)                            │
  ├─────────────┼───────────────────────────────────────┤
  │ X-App-Id    │ ${$.node.trigger.input.header.X-App-Id}│
  │ Cookie      │ ${$.node.trigger.input.header.Cookie} │
  │ X-XSRF-TOKEN│ ${$.node.trigger.input.header.X-XSRF-TOKEN}│
  │ keyword     │ ${$.node.trigger.input.query.keyword} │
  │ pageSize    │ ${$.node.trigger.input.query.pageSize}│
  └─────────────┴───────────────────────────────────────┘
```

#### 步骤 2.6 — 配置 Exit 节点（Output 映射）
```
操作: 在 Exit / 数据输出卡片中，切换到「响应体」Tab → 添加参数:
  ┌───────────┬─────────────────────────────────────┐
  │ 参数名      │ 表达式                                │
  ├───────────┼─────────────────────────────────────┤
  │ code      │ ${$.node.conn.output.body.code}      │
  │ messageZh │ ${$.node.conn.output.body.messageZh} │
  │ total     │ ${$.node.conn.output.body.page.total}│
  │ items     │ ${$.node.conn.output.body.data}      │
  └───────────┴─────────────────────────────────────┘

操作: 切换到「响应头」Tab → 添加参数:
  ┌───────────────────┬──────────────────────────────────────────┐
  │ 参数名              │ 表达式                                     │
  ├───────────────────┼──────────────────────────────────────────┤
  │ X-Echo-To-Header  │ ${$.node.trigger.input.body.X-Echo-To-Header}│
  │ X-Connector-Date  │ ${$.node.conn.output.header.Date}        │
  └───────────────────┴──────────────────────────────────────────┘
```

#### 步骤 2.7 — 保存草稿
```
操作: 点击「保存」
结果: 成功提示，草稿已保存
```

#### 步骤 2.8 — 调试
```
操作: 点击「调试」→ 弹出 DebugDrawer
填写:
  ┌──────────┬────────────────────────────────────┐
  │ 分区       │ 参数                                 │
  ├──────────┼────────────────────────────────────┤
  │ Header   │ X-App-Id = 20250730213114178360970 │
  │          │ Cookie = user_id=admin              │
  │          │ X-XSRF-TOKEN = user_id=admin        │
  │ Query    │ keyword = (空)                       │
  │          │ pageSize = 3                        │
  │ Body     │ {"X-Echo-To-Header": "echo-test"}   │
  └──────────┴────────────────────────────────────┘
操作: 点击「执行」
结果: 调试结果面板展示响应，无报错
```

#### 步骤 2.9 — 发布
```
操作: 点击「发布」→ 弹出确认 Modal
操作: 点击「提交发布」
结果: 成功提示，版本已发布
```

#### 步骤 2.10 — 部署 + 启动
```
操作: 回到连接流列表 (#/flowList)
操作: 找到 E2E_SingleNode_Flow 所在行
操作: 点击「部署」→ 等待成功提示
操作: 点击「启动」→ 等待成功提示
```

---

### 阶段三：HTTP 调用验证

#### 步骤 3.1 — 发起调用
```
POST http://localhost:18180/api/v1/flows/{flow_id}/invoke?keyword=&pageSize=3

Header:
  Content-Type:  application/json
  X-Sys-Token:   tester
  X-App-Id:      20250730213114178360970
  Cookie:        user_id=admin
  X-XSRF-TOKEN:  user_id=admin

Body:
  {"X-Echo-To-Header": "echo-test"}
```

#### 步骤 3.2 — 验证点
| # | 验证项 | 预期值 |
|---|--------|--------|
| V1 | HTTP 状态码 | `200` |
| V2 | `body.code` | `"200"` |
| V3 | `body.messageZh` | `"操作成功"` |
| V4 | `body.total` | `> 0`（数据库有数据） |
| V5 | `body.items` | 数组，length > 0，每项含 `connectorId`,`nameCn`,`status` |
| V6 | 响应头 `X-Echo-To-Header` | `"echo-test"` |
| V7 | 响应头 `X-Connector-Date` | 非空 |

## 4. 操作步骤速查（→ 对应第 3 章详细步骤）

| # | 页面 | 对应 § | 关键操作 |
|---|------|--------|---------|
| 1 | connectorList | 1.1–1.2 | 新建连接器 → Modal 填中/英文名 → 保存 → 提取 ID |
| 2 | connectorEditor | 1.3–1.9 | 创建草稿 → 配协议地址/类型 → 配 5 个 Schema Tab → 发布 |
| 3 | flowList | 2.1–2.2 | 新建连接流 → Modal 填中/英文名 → 保存 → 提取 ID |
| 4 | flowEditor | 2.3–2.4 | 创建草稿 → 选「单节点」卡片 |
| 5 | flowEditor | 2.5 | Connector 卡片: 下拉选连接器 + 填 5 个表达式 |
| 6 | flowEditor | 2.6 | Exit 卡片: 配 Body(4) + Header(2) 共 6 个表达式 |
| 7 | flowEditor | 2.7–2.8 | 保存草稿 → 调试（三段参数） |
| 8 | flowEditor | 2.9 | 发布 → 提交发布 |
| 9 | flowList | 2.10 | 部署 → 启动 |
| 10 | HTTP | 3.1–3.2 | POST invoke → 验证 7 项 |

## 5. 验证点

| # | 验证 | 预期 |
|---|------|------|
| V1 | HTTP 状态码 | 200 |
| V2 | `body.code` | `"200"` |
| V3 | `body.messageZh` | `"操作成功"` |
| V4 | `body.total` | > 0（数据库有连接器数据） |
| V5 | `body.items` | 数组，length > 0，每个 item 含 `connectorId`,`nameCn`,`status` |
| V6 | 响应头 `X-Echo-To-Header` | `"echo-test"`（等于入参 Body.X-Echo-To-Header） |
| V7 | 响应头 `X-Connector-Date` | 非空（open-server 响应头 Date） |
