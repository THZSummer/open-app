# 连接器与连接流并行数据查询

## 1. 背景

开放平台提供连接器与连接流两个独立的 OpenAPI。外部系统通常需要同时获取两类资源数据，在单次请求中完成并行查询并合并返回，减少客户端往返次数。

本测试验证 Flow 引擎 Script 节点在单次执行中同时调用两个外部 API，合并数据后统一返回。

## 2. 业务场景详情

### 2.1 场景概述

调用方传入查询参数（keyword、pageSize 等），Flow 同时查询连接器列表和连接流列表，脚本合并两个 API 的返回数据，统一输出。

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

将两个原始 OpenAPI 封装为统一 Flow 入口，一次请求并行查询两类数据。

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
Content-Type: application/json

{
    "code": "200",
    "messageZh": "操作成功",
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
| Body | `connectors` | `array` | 连接器列表数据 |
| Body | `flows` | `array` | 连接流列表数据 |
| Header | `X-Echo-To-Header` | `string` | 与入参 Body 的 `X-Echo-To-Header` 一致 |

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
Script 节点:
  1. query -> 拼接到两个 URL
  2. header -> 透传至两个 ctx.http.request
  3. 合并 connectors + flows 数据
  4. X-Echo-To-Header -> 透传至响应头
  ↓
ctx.http.request('GET', connectorsUrl, ...)  ->  连接器 API
ctx.http.request('GET', flowsUrl, ...)       ->  连接流 API
  ↓
Exit 节点:
  - header -> X-Echo-To-Header
  - body   -> {code, messageZh, connectors[], flows[]}
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
| Header | `X-App-Id` | `string` | 是 | 应用 ID |
| Header | `Cookie` | `string` | 否 | 用户身份 |
| Header | `X-XSRF-TOKEN` | `string` | 否 | CSRF 令牌 |
| Query | `curPage` | `number` | 否 | 页码，默认 1 |
| Query | `pageSize` | `number` | 否 | 每页条数，默认 3 |
| Query | `keyword` | `string` | 否 | 搜索关键字 |
| Body | `X-Echo-To-Header` | `string` | 否 | 透传回响应头 |

**调用示例：**

```bash
curl -X POST 'http://open-server:18080/api/v1/flows/{flowId}/invoke?curPage=1&pageSize=3&keyword=' \
  -H 'X-App-Id: 20250730213114178360970' \
  -H 'Cookie: user_id=admin' \
  -H 'X-XSRF-TOKEN: user_id=admin' \
  -H 'X-Sys-Token: tester' \
  -d '{"X-Echo-To-Header": "echo-value"}'
```

### 3.3 Script 节点逻辑

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
    var curPage = (q && q.curPage) || 1;
    var keyword = (q && q.keyword) || '';
    var pageSize = (q && q.pageSize) || 3;

    var baseUrl = 'http://localhost:18080/open-server/service/open/v2';
    var params = '?curPage=' + curPage
        + '&keyword=' + encodeURIComponent(keyword)
        + '&pageSize=' + pageSize;

    var connectorsResp = ctx.http.request('GET', baseUrl + '/connectors' + params, {headers: hdrs});
    var flowsResp = ctx.http.request('GET', baseUrl + '/flows' + params, {headers: hdrs});

    return {
        code: '200',
        messageZh: '\u64CD\u4F5C\u6210\u529F',
        messageEn: 'Success',
        connectors: connectorsResp.body.data || [],
        flows: flowsResp.body.data || [],
        echoTo: (body && body['X-Echo-To-Header']) || ''
    };
}
```

### 3.4 Script 输出 Schema

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `string` | 状态码 |
| `messageZh` | `string` | 中文消息 |
| `messageEn` | `string` | 英文消息 |
| `connectors` | `array` | 连接器列表 |
| `flows` | `array` | 连接流列表 |
| `echoTo` | `string` | 透传回响应头 |

### 3.5 Exit 节点映射

| 位置 | 字段 | 表达式 |
|------|------|--------|
| Header | `X-Echo-To-Header` | `${$.node.script.output.echoTo}` |
| Body | `code` | `${$.node.script.output.code}` |
| Body | `messageZh` | `${$.node.script.output.messageZh}` |
| Body | `messageEn` | `${$.node.script.output.messageEn}` |
| Body | `connectors` | `${$.node.script.output.connectors}` |
| Body | `flows` | `${$.node.script.output.flows}` |

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
| `X-XSRF-TOKEN` | header | CSRF 令牌，与 Cookie 值相同 |

### 3.7 响应格式

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
