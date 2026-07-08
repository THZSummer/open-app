# 连接器或连接流列表数据查询

## 1. 背景

开放平台提供统一的资源查询 OpenAPI，用于查询连接器列表和连接流列表。外部系统调用时需携带 `X-App-Id` Header 进行鉴权，并通过 `curPage`、`keyword`、`pageSize` 等 Query 参数进行分页搜索。

本测试验证 Flow 引擎 Script 节点的 `ctx.http` 能力：根据 `type` 参数条件路由到不同 API，并在 HTTP 调用中完整透传 Header、Query、Body 参数。

## 2. 业务场景详情

### 2.1 场景概述

调用方传入查询参数（type、query、header、body），Flow 根据 type 路由到对应的开放平台 OpenAPI，透传参数并发起 HTTP 请求，返回列表数据。

### 2.2 原始 OpenAPI 接口

Script 节点最终调用的底层开放平台 API：

**查询连接器列表：**

```bash
curl -X GET 'http://localhost:18080/open-server/service/open/v2/connectors?curPage=1&keyword=&pageSize=3' \
  -H 'X-App-Id: 20250730213114178360970' \
  -H 'Cookie: user_id=admin'
```

响应：

```json
{
    "code": "200",
    "messageZh": "操作成功",
    "data": [
        {"connectorId": "9000", "nameCn": "连接器A", "status": 2},
        {"connectorId": "9001", "nameCn": "连接器B", "status": 2}
    ]
}
```

**查询连接流列表：**

```bash
curl -X GET 'http://localhost:18080/open-server/service/open/v2/flows?curPage=1&keyword=&pageSize=3' \
  -H 'X-App-Id: 20250730213114178360970' \
  -H 'Cookie: user_id=admin'
```

响应：

```json
{
    "code": "200",
    "messageZh": "操作成功",
    "data": [
        {"id": "8000", "nameCn": "审批流", "lifecycleStatus": 1},
        {"id": "8001", "nameCn": "数据同步流", "lifecycleStatus": 2}
    ]
}
```

### 2.3 连接流接口设计

将原始 OpenAPI 封装为统一 Flow 入口。入参按 HTTP 语义分布在 Header/Query/Body 中，响应透传原始数据。

**查询连接器列表：**

请求：

```bash
curl -X POST 'http://open-server:18080/api/v1/flows/{flowId}/invoke?curPage=1&pageSize=3&keyword=' \
  -H 'Content-Type: application/json' \
  -H 'X-Type: connectors' \
  -H 'X-App-Id: 20250730213114178360970' \
  -H 'Cookie: user_id=admin' \
  -H 'X-Sys-Token: tester' \
  -d '{"X-Echo-To-Header": "echo-value"}'
```

响应：

```
HTTP 200
X-Type: connectors
X-Echo-To-Header: echo-value
Content-Type: application/json

{
    "code": "200",
    "messageZh": "操作成功",
    "data": [
        {"connectorId": "9000", "nameCn": "连接器A", "status": 2},
        {"connectorId": "9001", "nameCn": "连接器B", "status": 2}
    ]
}
```

**查询连接流列表：**

请求：

```bash
curl -X POST 'http://open-server:18080/api/v1/flows/{flowId}/invoke?curPage=1&pageSize=3&keyword=' \
  -H 'Content-Type: application/json' \
  -H 'X-Type: flows' \
  -H 'X-App-Id: 20250730213114178360970' \
  -H 'Cookie: user_id=admin' \
  -H 'X-Sys-Token: tester' \
  -d '{"X-Echo-To-Header": "echo-value"}'
```

响应：

```
HTTP 200
X-Type: flows
X-Echo-To-Header: echo-value
Content-Type: application/json

{
    "code": "200",
    "messageZh": "操作成功",
    "data": [
        {"id": "8000", "nameCn": "审批流", "lifecycleStatus": 1},
        {"id": "8001", "nameCn": "数据同步流", "lifecycleStatus": 2}
    ]
}
```

**入参说明：**

| 位置 | 字段 | 类型 | 说明 |
|------|------|------|------|
| Header | `X-Type` | `string` | 路由类型: `connectors` / `flows` |
| Header | `X-App-Id` | `string` | 应用 ID，鉴权必填 |
| Header | `Cookie` | `string` | 用户身份 |
| Query | `curPage` | `number` | 页码，默认 1 |
| Query | `pageSize` | `number` | 每页条数，默认 3 |
| Query | `keyword` | `string` | 搜索关键字 |
| Body | `X-Echo-To-Header` | `string` | 透传回响应头 |

**响应说明：**

| 位置 | 字段 | 类型 | 说明 |
|------|------|------|------|
| Body | `{}` | `object` | 透传原始 API 响应体 `{code, data[], ...}` |
| Header | `X-Type` | `string` | 路由类型标识，与入参一致 |
| Header | `X-Echo-To-Header` | `string` | 与入参 Body 的 `X-Echo-To-Header` 一致 |

### 2.4 条件路由规则

| `X-Type` | 路由目标 | API 路径 |
|-----------|---------|----------|
| `flows` | Flow 列表 API | `GET /open-server/service/open/v2/flows` |
| `connectors` 或无 | Connector 列表 API | `GET /open-server/service/open/v2/connectors` |

### 2.5 参数透传链路

```
调用方:
  Header: X-Type / X-App-Id / Cookie
  Query:  curPage / pageSize / keyword
  Body:   {X-Echo-To-Header}
  ↓
Flow Trigger 接收:
  - ctx.trigger.input.header = {X-Type, X-App-Id, Cookie}
  - ctx.trigger.input.query  = {curPage, pageSize, keyword}
  - ctx.trigger.input.body   = {X-Echo-To-Header}
  ↓
Script 节点:
  1. X-Type -> 选择路由 (/connectors 或 /flows)
  2. query -> 拼接到 URL
  3. header -> 透传至 ctx.http.request
  4. X-Echo-To-Header -> 透传至响应头
  ↓
ctx.http.request('GET', url, {headers: hdrs})  ->  开放平台 OpenAPI
  ↓
Exit 节点:
  - header -> X-Type / X-Echo-To-Header
  - body   -> 透传原始响应体
```

## 3. 配置详情

### 3.1 流程编排拓扑

```
Trigger Node -> Script Node -> Exit Node
```

### 3.2 Trigger 节点配置

- **类型**: HTTP Trigger
- **鉴权**: SYSTOKEN，白名单 `["tester"]`

**入参 Schema：**

| 位置 | 字段 | 类型 | 必填 | 说明 |
|------|------|------|:--:|------|
| Header | `X-Type` | `string` | 否 | 路由类型，默认 `connectors` |
| Header | `X-App-Id` | `string` | 是 | 应用 ID |
| Header | `Cookie` | `string` | 否 | 用户身份 |
| Query | `curPage` | `number` | 否 | 页码，默认 1 |
| Query | `pageSize` | `number` | 否 | 每页条数，默认 3 |
| Query | `keyword` | `string` | 否 | 搜索关键字 |

| Body | `X-Echo-To-Header` | `string` | 否 | 透传回响应头 |

**调用示例：**

```bash
curl -X POST 'http://open-server:18080/api/v1/flows/{flowId}/invoke?curPage=1&pageSize=3&keyword=' \
  -H 'X-Type: connectors' \
  -H 'X-App-Id: 20250730213114178360970' \
  -H 'Cookie: user_id=admin' \
  -H 'X-Sys-Token: tester' \
  -d '{"X-Echo-To-Header": "echo-value"}'
```

### 3.3 Script 节点逻辑

```javascript
function main(ctx) {
    var hdrs = ctx.trigger.input.header;
    var q = ctx.trigger.input.query;
    var body = ctx.trigger.input.body;
    var type = hdrs['X-Type'] || 'connectors';
    var curPage = q.curPage || 1;
    var keyword = q.keyword || '';
    var pageSize = q.pageSize || 3;

    var path = (type === 'flows') ? '/flows' : '/connectors';
    var url = 'http://localhost:18080/open-server/service/open/v2'
        + path + '?curPage=' + curPage
        + '&keyword=' + encodeURIComponent(keyword)
        + '&pageSize=' + pageSize;

    var resp = ctx.http.request('GET', url, {headers: hdrs});

    return {
        result: resp.body,
        type: type,
        echoTo: body['X-Echo-To-Header'] || ''
    };
}
```

### 3.4 Script 输出 Schema

| 字段 | 类型 | 说明 |
|------|------|------|
| `result` | `object` | 透传原始 API 响应体 |
| `type` | `string` | 路由类型标识 |
| `echoTo` | `string` | 透传回响应头 |

### 3.5 Exit 节点映射

| 位置 | 字段 | 表达式 |
|------|------|--------|
| Header | `X-Type` | `${$.node.script.output.type}` |
| Header | `X-Echo-To-Header` | `${$.node.script.output.echoTo}` |
| Body | `{}` | `${$.node.script.output.result}` (value 键透传) |

### 3.6 对外 API 地址

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

### 3.7 响应格式

```json
{
    "code": "200",
    "messageZh": "操作成功",
    "data": [
        {"connectorId": 9000, "nameCn": "连接器A", "status": 2},
        {"connectorId": 9001, "nameCn": "连接器B", "status": 2}
    ]
}
```
