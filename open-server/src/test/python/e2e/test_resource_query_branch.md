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
curl -X GET 'http://localhost:18080/open-server/service/open/v2/connectors?curPage=1&keyword=&pageSize=10' \
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
curl -X GET 'http://localhost:18080/open-server/service/open/v2/flows?curPage=1&keyword=&pageSize=10' \
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

### 2.3 连接流封装设计

将上述原始 API 封装为统一的 Flow 入口，调用方只需传入 `{type, query, header, body}`，由 Script 节点路由并透传：

**查询连接器列表：**

```bash
curl -X POST 'http://open-server:18080/api/v1/flows/{flowId}/invoke' \
  -H 'Content-Type: application/json' \
  -H 'X-Sys-Token: tester' \
  -d '{
    "type": "connectors",
    "query": {"curPage": 1, "pageSize": 10, "keyword": ""},
    "header": {"X-App-Id": "20250730213114178360970", "Cookie": "user_id=admin"},
    "body": {}
  }'
```

**查询连接流列表：**

```bash
curl -X POST 'http://open-server:18080/api/v1/flows/{flowId}/invoke' \
  -H 'Content-Type: application/json' \
  -H 'X-Sys-Token: tester' \
  -d '{
    "type": "flows",
    "query": {"curPage": 1, "pageSize": 10, "keyword": ""},
    "header": {"X-App-Id": "20250730213114178360970", "Cookie": "user_id=admin"},
    "body": {}
  }'
```

### 2.4 条件路由规则

| 入参 type | 路由目标 | API 路径 |
|-----------|---------|----------|
| `flows` | Flow 列表 API | `GET /open-server/service/open/v2/flows` |
| `connectors` 或无 | Connector 列表 API | `GET /open-server/service/open/v2/connectors` |

### 2.5 参数透传链路

```
调用方传入 {type, query, header, body}
  ↓
Flow Trigger 接收
  ↓
Script 节点:
  1. 按 type 选择路由
  2. query -> 拼接到 URL (?curPage=&keyword=&pageSize=)
  3. header -> 传递给 ctx.http.request 的 opts.headers
  4. body  -> 预留 POST 场景
  ↓
ctx.http.request('GET', url, {headers: hdrs})  ->  开放平台 OpenAPI
  ↓
返回 {code, data[]}
  ↓
Script 提取 data[] -> {result, domain, group, path}
  ↓
Exit 节点映射到 HTTP 响应
```

### 2.6 响应效果

**请求 connectors：**

```
POST /api/v1/flows/{flowId}/invoke
Body: {"type": "connectors", "query": {"curPage": 1, "pageSize": 10, "keyword": ""}, ...}
```

```json
HTTP 200
{
    "result": [
        {"connectorId": "9000", "nameCn": "连接器A", "status": 2},
        {"connectorId": "9001", "nameCn": "连接器B", "status": 2}
    ],
    "domain": "2",
    "group": "连接器A",
    "path": "connectors"
}
```

**请求 flows：**

```
POST /api/v1/flows/{flowId}/invoke
Body: {"type": "flows", "query": {"curPage": 1, "pageSize": 10, "keyword": ""}, ...}
```

```json
HTTP 200
{
    "result": [
        {"id": "8000", "nameCn": "审批流", "lifecycleStatus": 1},
        {"id": "8001", "nameCn": "数据同步流", "lifecycleStatus": 2}
    ],
    "domain": "2",
    "group": "审批流",
    "path": "flows"
}
```

**字段说明：**

| 字段 | 类型 | 说明 | 数据来源 |
|------|------|------|---------|
| `result` | `array` | 原始列表数据 | 原始 API `data[]` 直接透传 |
| `domain` | `string` | 返回条目数 | 原始 API `data.length` |
| `group` | `string` | 首条记录名称 | 原始 API `data[0].nameCn` |
| `path` | `string` | 路由类型标识 | 入参 `type` |

## 3. 配置详情

### 3.1 流程编排拓扑

```
Trigger Node -> Script Node -> Exit Node
```

### 3.2 Trigger 节点配置

- **类型**: HTTP Trigger
- **鉴权**: SYSTOKEN，白名单 `["tester"]`
- **入参 Body Schema**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | `string` | 否 | 路由类型，默认 `connectors` |
| `query` | `object` | 否 | URL 查询参数 `{curPage, pageSize, keyword}` |
| `header` | `object` | 否 | HTTP 请求头 `{X-App-Id, Cookie}` |
| `body` | `object` | 否 | HTTP 请求体（预留 POST 场景） |

### 3.3 Script 节点逻辑

```javascript
function main(ctx) {
    var input = ctx.trigger.input.body;
    var type = input.type || 'connectors';
    var q = input.query || {};
    var hdrs = input.header || {};
    var curPage = q.curPage || 1;
    var keyword = q.keyword || '';
    var pageSize = q.pageSize || 10;

    var path = (type === 'flows') ? '/flows' : '/connectors';
    var url = 'http://localhost:18080/open-server/service/open/v2'
        + path + '?curPage=' + curPage
        + '&keyword=' + encodeURIComponent(keyword)
        + '&pageSize=' + pageSize;

    var resp = ctx.http.request('GET', url, {headers: hdrs});
    var items = resp.body.data || [];

    return {
        result: items,
        domain: String(items.length),
        group: items.length > 0 ? items[0].nameCn : '-',
        path: type === 'flows' ? 'flows' : 'connectors'
    };
}
```

### 3.4 Script 输出 Schema

| 字段 | 类型 | 说明 |
|------|------|------|
| `result` | `array` | 原始列表数据，直接透传 API `data[]` |
| `domain` | `string` | 列表条目数 |
| `group` | `string` | 首条名称 |
| `path` | `string` | 路由类型标识 |

### 3.5 Exit 节点映射

| 响应字段 | 来源 |
|----------|------|
| `result` | `${$.node.script.output.result}` |
| `domain` | `${$.node.script.output.domain}` |
| `group` | `${$.node.script.output.group}` |
| `path` | `${$.node.script.output.path}` |

### 3.6 对外 API 地址

```
GET http://localhost:18080/open-server/service/open/v2/connectors
GET http://localhost:18080/open-server/service/open/v2/flows
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `curPage` | query | 页码，默认 1 |
| `pageSize` | query | 每页条数，默认 10 |
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
