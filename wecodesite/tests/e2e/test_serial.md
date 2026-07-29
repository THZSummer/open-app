# 串行流 — trigger → script₁ → connector → script₂ → exit

## 1. 背景

串行流模式验证数据在多个节点间的顺序传递和脚本节点的转换能力。Trigger 接收原始数据 → Script₁ 格式转换 → Connector 调用下游 API → Script₂ 抽取响应 → Exit 返回。全流程通过前端 UI 操作完成。

## 2. 业务场景

### 2.1 流程拓扑

```
Trigger → Script₁(格式转换) → Connector(调用Mock) → Script₂(结果抽取) → Exit
  (serial)            (serial)              (serial)           (serial)
```

### 2.2 数据流

```
调用方 POST /api/v1/flows/{flowId}/invoke
  body: {"name": "OpenApp", "value": 7}
  ↓
Trigger 接收 body.name, body.value
  ↓
Script₁: 格式转换
  输入: ctx.trigger.input.body = {name: "OpenApp", value: 7}
  逻辑: {message: "Hello, " + name, doubled: value * 2, tags: ["a","b"]}
  输出: {message: "Hello, OpenApp!", doubled: 14, tags: ["a","b"]}
  ↓
Connector 引用 script₁ 输出:
  Query: msg=${$.node.script_1.output.message}
  Body:  {"doubled": ${$.node.script_1.output.doubled}, "tags": ${$.node.script_1.output.tags}}
  → POST /api/echo_body
  ← {"echo_body": {"message": "Hello, OpenApp!", "doubled": 14, "tags": ["a","b"]}}
  ↓
Script₂: 结果抽取
  输入: ctx.conn.output.body = {echo_body: {message, doubled, tags}}
  逻辑: {echoedMessage: echo_body.message, echoedDoubled: echo_body.doubled}
  输出: {echoedMessage: "Hello, OpenApp!", echoedDoubled: 14}
  ↓
Exit 映射 script₂ 输出:
  body: {echoedMessage, echoedDoubled}
```

### 2.3 入参

| 位置 | 字段 | 类型 | 说明 |
|------|------|------|------|
| Body | `name` | `string` | 名称 |
| Body | `value` | `number` | 数值 |

### 2.4 响应

| 位置 | 字段 | 来源 |
|------|------|------|
| Body | `echoedMessage` | `${$.node.script_2.output.echoedMessage}` |
| Body | `echoedDoubled` | `${$.node.script_2.output.echoedDoubled}` |

## 3. 配置详情

### 3.1 Mock 服务

```
POST /api/echo_body
  body: {"doubled": 14, "tags": ["a","b"], "msg": "Hello, OpenApp!"}
  → 200 {"echo_body": {"message": "...", "doubled": 14, "tags": ["a","b"]}}
```

### 3.2 连接器配置

| 配置项 | 值 |
|--------|-----|
| 名称 | E2E_MockEchoBody |
| 协议 | HTTP |
| URL | `http://localhost:18999/api/echo_body` |
| Method | POST |
| 认证 | NONE |
| 输入 Body | `{doubled, tags, msg}` |
| 输出 Body | `{echo_body: {message, doubled, tags}}` |

### 3.3 Trigger 节点

- **类型**: HTTP Trigger
- **认证**: SYSTOKEN

**入参 Schema**:

| 位置 | 字段 | 类型 | 必填 |
|------|------|------|:--:|
| Body | `name` | `string` | 是 |
| Body | `value` | `number` | 是 |

### 3.4 Script₁ 节点: 格式转换

```javascript
function main(ctx) {
    var body = ctx.trigger.input.body;
    return {
        message: "Hello, " + body.name + "!",
        doubled: body.value * 2,
        tags: ["a", "b"]
    };
}
```

**输出 Schema**: `{message: string, doubled: number, tags: array}`

### 3.5 Connector 节点

**Input 映射**:

| 位置 | 字段 | 表达式 |
|------|------|--------|
| Body | `msg` | `${$.node.script_1.output.message}` |
| Body | `doubled` | `${$.node.script_1.output.doubled}` |
| Body | `tags` | `${$.node.script_1.output.tags}` |

### 3.6 Script₂ 节点: 结果抽取

```javascript
function main(ctx) {
    var echo = ctx.conn.output.body.echo_body;
    return {
        echoedMessage: echo.message,
        echoedDoubled: echo.doubled
    };
}
```

**输出 Schema**: `{echoedMessage: string, echoedDoubled: number}`

### 3.7 Exit 节点

**Output 映射**:

| 位置 | 字段 | 表达式 |
|------|------|--------|
| Body | `echoedMessage` | `${$.node.script_2.output.echoedMessage}` |
| Body | `echoedDoubled` | `${$.node.script_2.output.echoedDoubled}` |

## 4. 前端 UI 操作步骤

| 步骤 | 页面 | 操作 |
|------|------|------|
| 0 | — | 启动 Mock Server |
| 1 | `#/connectorList` | 创建连接器 E2E_MockEchoBody, 配置 POST /api/echo_body |
| 2 | `#/flowList` | 创建连接流 |
| 3 | `#/flowEditor` | 选择「串行」模式 |
| 4 | `#/flowEditor` | 配置 Trigger: 添加 body.name (string), body.value (number) |
| 5 | `#/flowEditor` | 添加 Script₁ 节点, Monaco Editor 输入转换代码 |
| 6 | `#/flowEditor` | 添加 Connector 节点, 选择 E2E_MockEchoBody, 配置 Body 映射 |
| 7 | `#/flowEditor` | 添加 Script₂ 节点, Monaco Editor 输入抽取代码 |
| 8 | `#/flowEditor` | 配置 Exit 节点 outputMapping |
| 9 | `#/flowEditor` | 保存草稿 |
| 10 | `#/flowEditor` | 调试: 输入 `{"name":"OpenApp","value":7}`, 验证 steps 顺序 |
| 11 | `#/flowEditor` | 发布 + 审批 |
| 12 | `#/flowList` | 部署 + 启动 |
| 13 | HTTP | POST `/api/v1/flows/{flowId}/invoke` |

## 5. 验证点

| # | 验证 | 预期 |
|---|------|------|
| V1 | 调试 steps 顺序 | `trigger → script_1 → connector → script_2 → exit` |
| V2 | Script₁ 输出 | `message="Hello, OpenApp!"`, `doubled=14` |
| V3 | HTTP 调用 200 | `body.echoedMessage == "Hello, OpenApp!"` |
| V4 | HTTP 调用 200 | `body.echoedDoubled == 14` |
| V5 | 节点状态 | 全部 `status=success` |
