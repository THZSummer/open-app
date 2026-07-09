# 连接器与连接流并行数据查询

## 1. 背景

开放平台提供连接器与连接流两个独立的 OpenAPI。外部系统通常需要同时获取两类资源数据。本测试验证 Flow 引擎的**并行节点**能力：通过并行分流，两个 Connector 分支**同时**调用两个外部 API，再由合并脚本汇总后统一返回。

## 2. 业务场景详情

### 2.1 场景概述

调用方传入查询参数（keyword、pageSize 等），Flow 并行查询连接器列表和连接流列表，合并脚本汇总后统一输出，并在响应头中透传上游 API 的 `Date` 字段。

### 2.2 原始 OpenAPI 接口

**查询连接器列表：**

```bash
curl -X GET 'http://localhost:18080/open-server/service/open/v2/connectors?curPage=1&keyword=&pageSize=3' \
  -H 'X-App-Id: 20250730213114178360970' \
  -H 'Cookie: user_id=admin' \
  -H 'X-XSRF-TOKEN: user_id=admin'
```

**查询连接流列表：**

```bash
curl -X GET 'http://localhost:18080/open-server/service/open/v2/flows?curPage=1&keyword=&pageSize=3' \
  -H 'X-App-Id: 20250730213114178360970' \
  -H 'Cookie: user_id=admin' \
  -H 'X-XSRF-TOKEN: user_id=admin'
```

### 2.3 连接流接口设计

将两个原始 OpenAPI 封装为统一 Flow 入口，通过并行节点同时查询两类数据。

请求：

```bash
curl -X POST 'http://open-server:18080/api/v1/flows/{flowId}/invoke?curPage=1&pageSize=3&keyword=' \
  -H 'Content-Type: application/json' \
  -H 'X-App-Id: 20250730213114178360970' \
  -H 'Cookie: user_id=admin' \
  -H 'X-XSRF-TOKEN: user_id=admin' \
  -H 'X-Sys-Token: tester' \
  -d '{"X-Echo-To-Header": "echo-value"}'
```

响应：

```
HTTP 200
X-Echo-To-Header: echo-value
X-Connector-Date: Wed, 08 Jul 2026 14:11:40 GMT
X-Flow-Date: Wed, 08 Jul 2026 14:11:40 GMT
Content-Type: application/json

{
    "code": "200",
    "messageZh": "操作成功",
    "messageEn": "Success",
    "connectors": [
        {"connectorId": "9000", "nameCn": "连接器A", "status": 2},
        {"connectorId": "9001", "nameCn": "连接器B", "status": 2}
    ],
    "flows": [
        {"id": "8000", "nameCn": "审批流", "lifecycleStatus": 1},
        {"id": "8001", "nameCn": "数据同步流", "lifecycleStatus": 2}
    ]
}
```

**入参说明：**

| 位置 | 字段 | 类型 | 说明 |
|------|------|------|------|
| Header | `X-App-Id` | `string` | 应用 ID，鉴权必填 |
| Header | `Cookie` | `string` | 用户身份 |
| Header | `X-XSRF-TOKEN` | `string` | CSRF 令牌，值与 Cookie 相同 |
| Query | `curPage` | `number` | 页码，默认 1 |
| Query | `pageSize` | `number` | 每页条数，默认 3 |
| Query | `keyword` | `string` | 搜索关键字 |
| Body | `X-Echo-To-Header` | `string` | 透传回响应头 |

**响应说明：**

| 位置 | 字段 | 类型 | 说明 |
|------|------|------|------|
| Body | `code` | `string` | 状态码 `"200"` |
| Body | `messageZh` | `string` | 中文消息 |
| Body | `messageEn` | `string` | 英文消息 |
| Body | `connectors` | `array` | 连接器列表数据 |
| Body | `flows` | `array` | 连接流列表数据 |
| Header | `X-Echo-To-Header` | `string` | 与入参 Body 的 `X-Echo-To-Header` 一致 |
| Header | `X-Connector-Date` | `string` | 连接器 API 响应头 `Date` |
| Header | `X-Flow-Date` | `string` | 连接流 API 响应头 `Date` |

### 2.4 参数透传链路

```
调用方:
  Header: X-App-Id / Cookie / X-XSRF-TOKEN
  Query:  curPage / pageSize / keyword
  Body:   {X-Echo-To-Header}
  ↓
Flow Trigger 接收:
  - ctx.trigger.input.header = {X-App-Id, Cookie, X-XSRF-TOKEN}
  - ctx.trigger.input.query  = {curPage, pageSize, keyword}
  - ctx.trigger.input.body   = {X-Echo-To-Header}
  ↓
预处理脚本 (script_prepare):
  - 提取 header (X-App-Id, Cookie, X-XSRF-TOKEN) 和 query (curPage, pageSize, keyword)
  - 统一输出给两个并行分支
  ↓
并行分流 (parallel node):
  ┌─ connector_connectors: GET /service/open/v2/connectors?curPage=&keyword=&pageSize=
  │   返回: {code, messageZh, data[]} + 响应头 Date
  └─ connector_flows:      GET /service/open/v2/flows?curPage=&keyword=&pageSize=
      返回: {code, messageZh, data[]} + 响应头 Date
  ↓
合并脚本 (script_merge):
  - 读取 ctx.conn_connectors.output.data  → connectors[]
  - 读取 ctx.conn_flows.output.data       → flows[]
  - 读取 ctx.conn_connectors.output.headers.Date → connectorDate
  - 读取 ctx.conn_flows.output.headers.Date      → flowDate
  - 读取 ctx.script_prepare.output.echoTo → echoTo
  ↓
Exit 节点:
  - header → X-Echo-To-Header / X-Connector-Date / X-Flow-Date
  - body   → {code, messageZh, messageEn, connectors[], flows[]}
```

## 3. 配置详情

### 3.1 流程编排拓扑

```
        Trigger Node
             │  (serial)
             ▼
     Script: 预处理 (script_prepare)
             │  (serial)
             ▼
        Parallel Node
    ┌────(parallel)────┐
    ▼                   ▼
Connector:           Connector:
查询连接器             查询连接流
(conn_connectors)    (conn_flows)
    │                   │
    └───(parallel)──────┘
             │
             ▼
     Script: 合并结果 (script_merge)
             │  (serial)
             ▼
         Exit Node
```

### 3.2 Trigger 节点配置

- **类型**: HTTP Trigger
- **鉴权**: SYSTOKEN，白名单 `["tester"]`

**入参 Schema：**

| 位置 | 字段 | 类型 | 必填 | 说明 |
|------|------|------|:--:|------|
| Header | `X-App-Id` | `string` | 是 | 应用 ID |
| Header | `Cookie` | `string` | 否 | 用户身份 |
| Header | `X-XSRF-TOKEN` | `string` | 否 | CSRF 令牌 |
| Query | `curPage` | `number` | 否 | 页码，默认 1 |
| Query | `pageSize` | `number` | 否 | 每页条数，默认 3 |
| Query | `keyword` | `string` | 否 | 搜索关键字 |
| Body | `X-Echo-To-Header` | `string` | 否 | 透传回响应头 |

### 3.3 Script 节点: 预处理 (script_prepare)

从 trigger 提取 headers、query、body 数据，统一输出供并行分支引用。

```javascript
function main(ctx) {
    var hdrs = ctx.trigger.input.header;
    var q = ctx.trigger.input.query;
    var body = ctx.trigger.input.body;
    function hdr(name) {
        return hdrs[name] || hdrs[name.toLowerCase()] || hdrs[name.toUpperCase()];
    }
    if (!hdrs || !hdr('X-App-Id')) {
        hdrs = (body && body.header) ? body.header : {};
        q = (body && body.query) ? body.query : {};
    }
    return {
        appId: hdr('X-App-Id') || '',
        cookie: hdr('Cookie') || '',
        xsrfToken: hdr('X-XSRF-TOKEN') || '',
        curPage: (q && q.curPage) || 1,
        pageSize: (q && q.pageSize) || 3,
        keyword: (q && q.keyword) || '',
        echoTo: (body && body['X-Echo-To-Header']) || ''
    };
}
```

**输出 Schema：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `appId` | `string` | X-App-Id |
| `cookie` | `string` | Cookie |
| `xsrfToken` | `string` | X-XSRF-TOKEN |
| `curPage` | `number` | 页码 |
| `pageSize` | `number` | 每页条数 |
| `keyword` | `string` | 搜索关键字 |
| `echoTo` | `string` | 透传回响应头 |

### 3.4 并行节点

```
Node Type: parallel
分流模式: 并行执行两个分支，各自独立调用外部 API
```

### 3.5 Connector 节点: 查询连接器 (conn_connectors)

**连接器配置：**

| 配置项 | 值 |
|--------|-----|
| Protocol | HTTP |
| Method | GET |
| URL | `http://localhost:18080/open-server/service/open/v2/connectors` |
| Auth | NONE（headers 由 input 映射传入） |

**Input 映射（从 script_prepare 输出引用）：**

| 位置 | 字段 | 表达式 |
|------|------|--------|
| Header | `X-App-Id` | `${$.node.script_prepare.output.appId}` |
| Header | `Cookie` | `${$.node.script_prepare.output.cookie}` |
| Header | `X-XSRF-TOKEN` | `${$.node.script_prepare.output.xsrfToken}` |
| Query | `curPage` | `${$.node.script_prepare.output.curPage}` |
| Query | `pageSize` | `${$.node.script_prepare.output.pageSize}` |
| Query | `keyword` | `${$.node.script_prepare.output.keyword}` |

### 3.6 Connector 节点: 查询连接流 (conn_flows)

**连接器配置：**

| 配置项 | 值 |
|--------|-----|
| Protocol | HTTP |
| Method | GET |
| URL | `http://localhost:18080/open-server/service/open/v2/flows` |
| Auth | NONE（headers 由 input 映射传入） |

**Input 映射（同 conn_connectors，URL 指向 /flows）：**

| 位置 | 字段 | 表达式 |
|------|------|--------|
| Header | `X-App-Id` | `${$.node.script_prepare.output.appId}` |
| Header | `Cookie` | `${$.node.script_prepare.output.cookie}` |
| Header | `X-XSRF-TOKEN` | `${$.node.script_prepare.output.xsrfToken}` |
| Query | `curPage` | `${$.node.script_prepare.output.curPage}` |
| Query | `pageSize` | `${$.node.script_prepare.output.pageSize}` |
| Query | `keyword` | `${$.node.script_prepare.output.keyword}` |

### 3.7 Script 节点: 合并结果 (script_merge)

```javascript
function main(ctx) {
    var c = ctx.conn_connectors.output || {};
    var f = ctx.conn_flows.output || {};
    var p = ctx.script_prepare.output || {};
    return {
        code: '200',
        messageZh: '操作成功',
        messageEn: 'Success',
        connectors: c.data || [],
        flows: f.data || [],
        connectorDate: (c.headers && c.headers['Date'])
                    || (c.headers && c.headers['date']) || '',
        flowDate: (f.headers && f.headers['Date'])
               || (f.headers && f.headers['date']) || '',
        echoTo: p.echoTo || ''
    };
}
```

**输出 Schema：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `string` | 状态码 |
| `messageZh` | `string` | 中文消息 |
| `messageEn` | `string` | 英文消息 |
| `connectors` | `array` | 连接器列表 |
| `flows` | `array` | 连接流列表 |
| `connectorDate` | `string` | 连接器 API 响应 Date |
| `flowDate` | `string` | 连接流 API 响应 Date |
| `echoTo` | `string` | 透传回响应头 |

### 3.8 Exit 节点映射

| 位置 | 字段 | 表达式 |
|------|------|--------|
| Header | `X-Echo-To-Header` | `${$.node.script_merge.output.echoTo}` |
| Header | `X-Connector-Date` | `${$.node.script_merge.output.connectorDate}` |
| Header | `X-Flow-Date` | `${$.node.script_merge.output.flowDate}` |
| Body | `code` | `${$.node.script_merge.output.code}` |
| Body | `messageZh` | `${$.node.script_merge.output.messageZh}` |
| Body | `messageEn` | `${$.node.script_merge.output.messageEn}` |
| Body | `connectors` | `${$.node.script_merge.output.connectors}` |
| Body | `flows` | `${$.node.script_merge.output.flows}` |

### 3.9 对外 API 地址

```
GET http://localhost:18080/open-server/service/open/v2/connectors
GET http://localhost:18080/open-server/service/open/v2/flows
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `curPage` | query | 页码，默认 1 |
| `pageSize` | query | 每页条数，默认 3 |
| `keyword` | query | 搜索关键字 |
| `X-App-Id` | header | 应用 ID，必填 |
| `Cookie` | header | 用户身份 `user_id=admin` |
| `X-XSRF-TOKEN` | header | CSRF 令牌，与 Cookie 值相同 |

### 3.10 响应格式

```json
{
    "code": "200",
    "messageZh": "操作成功",
    "messageEn": "Success",
    "connectors": [
        {"connectorId": 9000, "nameCn": "连接器A", "status": 2},
        {"connectorId": 9001, "nameCn": "连接器B", "status": 2}
    ],
    "flows": [
        {"id": 8000, "nameCn": "审批流", "lifecycleStatus": 1},
        {"id": 8001, "nameCn": "数据同步流", "lifecycleStatus": 2}
    ]
}
```
