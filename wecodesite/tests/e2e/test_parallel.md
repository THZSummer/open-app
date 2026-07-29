# 并行流 — trigger → script → [parallel] → connₐ / conn_b → merge → exit

## 1. 背景

并行流模式验证 Flow 引擎的并行执行能力：预处理脚本统一提取参数 → 并行分叉两个 Connector 同时调用下游 API → 合并脚本汇总结果统一返回。全流程通过前端 UI 操作完成。

## 2. 业务场景

### 2.1 流程拓扑

```
Trigger → Script_prepare → [Parallel] ┬→ Connector_A(GET /api/branch-a) ┬→ Script_merge → Exit
                                      └→ Connector_B(GET /api/branch-b) ┘
```

### 2.2 数据流

```
调用方 POST /api/v1/flows/{flowId}/invoke
  query: keyword=test&pageSize=3
  body: {"X-Echo-To-Header": "echo-test"}
  ↓
Trigger 接收 query + body
  ↓
Script_prepare: 提取共用参数
  输入: ctx.trigger.input.query, ctx.trigger.input.body
  输出: {keyword, pageSize, echoTo}
  ↓
Parallel 分叉:
  ┌─ Connector_A → GET /api/branch-a?keyword=test&pageSize=3
  │   ← {"service": "branch-a", "items": [...], "date": "Mon, ..."}
  └─ Connector_B → GET /api/branch-b?keyword=test&pageSize=3
      ← {"service": "branch-b", "items": [...], "date": "Mon, ..."}
  ↓
Script_merge: 合并两分支结果
  输入: ctx.conn_a.output.body, ctx.conn_b.output.body
  输出: {code, a_items, b_items, a_date, b_date, echoTo}
  ↓
Exit 映射:
  body: {code, a_items, b_items}
  header: {X-Echo-To-Header, X-Branch-A-Date, X-Branch-B-Date}
```

### 2.3 入参

| 位置 | 字段 | 类型 | 说明 |
|------|------|------|------|
| Query | `keyword` | `string` | 搜索关键词 |
| Query | `pageSize` | `number` | 每页条数 |
| Body | `X-Echo-To-Header` | `string` | 透传回响应头 |

### 2.4 响应

| 位置 | 字段 | 来源 |
|------|------|------|
| Body | `code` | `"200"` |
| Body | `a_items` | `${$.node.script_merge.output.a_items}` |
| Body | `b_items` | `${$.node.script_merge.output.b_items}` |
| Header | `X-Echo-To-Header` | `${$.node.script_merge.output.echoTo}` |
| Header | `X-Branch-A-Date` | `${$.node.script_merge.output.a_date}` |
| Header | `X-Branch-B-Date` | `${$.node.script_merge.output.b_date}` |

## 3. 配置详情

### 3.1 Mock 服务

```
GET /api/branch-a?keyword=test&pageSize=3
  → 200 {"service":"branch-a","items":[{"id":"a1","val":100}],"date":"Mon, 28 Jul 2026 00:00:00 GMT"}

GET /api/branch-b?keyword=test&pageSize=3
  → 200 {"service":"branch-b","items":[{"id":"b1","val":200}],"date":"Mon, 28 Jul 2026 00:00:01 GMT"}
```

### 3.2 连接器 A 配置

| 配置项 | 值 |
|--------|-----|
| 名称 | E2E_BranchA |
| URL | `http://localhost:18999/api/branch-a` |
| Method | GET |

### 3.3 连接器 B 配置

| 配置项 | 值 |
|--------|-----|
| 名称 | E2E_BranchB |
| URL | `http://localhost:18999/api/branch-b` |
| Method | GET |

### 3.4 Trigger 节点

- **类型**: HTTP Trigger
- **认证**: SYSTOKEN

**入参 Schema**:

| 位置 | 字段 | 类型 | 必填 |
|------|------|------|:--:|
| Query | `keyword` | `string` | 否 |
| Query | `pageSize` | `number` | 否 |
| Body | `X-Echo-To-Header` | `string` | 否 |

### 3.5 Script_prepare: 预处理

```javascript
function main(ctx) {
    var q = ctx.trigger.input.query || {};
    var body = ctx.trigger.input.body || {};
    return {
        keyword: q.keyword || "",
        pageSize: q.pageSize || 3,
        echoTo: body["X-Echo-To-Header"] || ""
    };
}
```

**输出**: `{keyword, pageSize, echoTo}`

### 3.6 Connector A / B

**Input 映射** (两个连接器相同):

| 位置 | 字段 | 表达式 |
|------|------|--------|
| Query | `keyword` | `${$.node.script_prepare.output.keyword}` |
| Query | `pageSize` | `${$.node.script_prepare.output.pageSize}` |

### 3.7 Script_merge: 合并结果

```javascript
function main(ctx) {
    var a = ctx.conn_a.output.body;
    var b = ctx.conn_b.output.body;
    var p = ctx.script_prepare.output;
    return {
        code: "200",
        a_items: a.items || [],
        b_items: b.items || [],
        a_date: (a.date) || "",
        b_date: (b.date) || "",
        echoTo: p.echoTo || ""
    };
}
```

**输出**: `{code, a_items, b_items, a_date, b_date, echoTo}`

### 3.8 Exit 节点

| 位置 | 字段 | 表达式 |
|------|------|--------|
| Body | `code` | `${$.node.script_merge.output.code}` |
| Body | `a_items` | `${$.node.script_merge.output.a_items}` |
| Body | `b_items` | `${$.node.script_merge.output.b_items}` |
| Header | `X-Echo-To-Header` | `${$.node.script_merge.output.echoTo}` |
| Header | `X-Branch-A-Date` | `${$.node.script_merge.output.a_date}` |
| Header | `X-Branch-B-Date` | `${$.node.script_merge.output.b_date}` |

## 4. 前端 UI 操作步骤

| 步骤 | 页面 | 操作 |
|------|------|------|
| 0 | — | 启动 Mock Server (port 18999, 两个端点) |
| 1 | `#/connectorList` | 创建连接器 E2E_BranchA (GET /api/branch-a) |
| 2 | `#/connectorList` | 创建连接器 E2E_BranchB (GET /api/branch-b) |
| 3 | `#/flowList` | 创建连接流 |
| 4 | `#/flowEditor` | 选择「并行」模式 |
| 5 | `#/flowEditor` | 配置 Trigger: query.keyword, query.pageSize, body.X-Echo-To-Header |
| 6 | `#/flowEditor` | 添加 Script_prepare 节点, 输入预处理代码 |
| 7 | `#/flowEditor` | 添加 Parallel 分叉节点 |
| 8 | `#/flowEditor` → 分支A | 添加 Connector A, 选择 E2E_BranchA, 配置 Query 映射 |
| 9 | `#/flowEditor` → 分支B | 添加 Connector B, 选择 E2E_BranchB, 配置 Query 映射 |
| 10 | `#/flowEditor` | 添加 Script_merge 节点, 输入合并代码 |
| 11 | `#/flowEditor` | 配置 Exit 节点 outputMapping |
| 12 | `#/flowEditor` | 保存草稿 |
| 13 | `#/flowEditor` | 调试: query `keyword=test&pageSize=3`, body `{"X-Echo-To-Header":"echo-test"}` |
| 14 | `#/flowEditor` | 发布 + 审批 |
| 15 | `#/flowList` | 部署 + 启动 |
| 16 | HTTP | POST `/api/v1/flows/{flowId}/invoke?keyword=test&pageSize=3` |

## 5. 验证点

| # | 验证 | 预期 |
|---|------|------|
| V1 | 调试 steps | 两个 Connector 都 `status=success` |
| V2 | HTTP 调用 200 | `body.a_items` 包含 branch-a 数据 |
| V3 | HTTP 调用 200 | `body.b_items` 包含 branch-b 数据 |
| V4 | HTTP 响应头 | `X-Echo-To-Header == "echo-test"` |
| V5 | HTTP 响应头 | `X-Branch-A-Date`, `X-Branch-B-Date` 非空 |
| V6 | 并行性能 | 总耗时接近最慢分支耗时 (非两分支之和) |
