# JSON Schema 设计规范：连接器平台

**关联文档**: plan.md (§4.2 数据库设计, §4.4 API 接口设计), plan-db.md (§3 表结构定义), plan-api.md (§3 接口详细定义)  
**版本**: v1.0  
**创建日期**: 2026-05-22  

---

## 1. 设计哲学

### 1.1 设计目标

| 目标 | 说明 |
|------|------|
| **自描述** | Schema 本身能说清字段含义、类型、约束，不需要散落在代码注释中 |
| **一致性** | 同一语义的字段在不同上下文中命名统一（如 `authTypeSchema` 在触发器和连接器中结构一致） |
| **可扩展** | 预留 `extensions` 或 `x_*` 字段，V1 需求按 namespace 新增不破坏已有结构 |
| **无冗余** | 触发器不需要的字段（如 `timeoutMs`、`outputSchema`）不在其 Schema 中出现 |

### 1.2 参考标准

| 标准 | 参考程度 | 说明 |
|------|---------|------|
| JSON Schema (draft-07) | ⭐⭐⭐ **核心参考** | `type`/`properties`/`required`/`description` 等元字段直接复用；`inputSchema`/`outputSchema` 字段的值本身就是 JSON Schema 对象 |
| OpenAPI 3.0 `components/schemas` | ⭐⭐⭐ **结构参考** | 借鉴「可复用组件+按场景组合」的思想：`authTypeSchema` / `rateLimit` 作为共享组件，触发器和连接器各取所需 |
| AsyncAPI `message.payload` | ⭐⭐ **概念参考** | 入站（trigger）与出站（connector）的 Schema 分离设计 |

### 1.3 两条核心原则

```
原则一：同一事物同一个名
────────────────────────────────
authTypeSchema 结构 → 触发器和连接器都用同一套
rateLimit 结构     → 入站限流和出站限流都用同一套
inputSchema        → 触发器的入参和连接器的入参统一命名

原则二：不用的字段不出现在 Schema 中
────────────────────────────────
trigger 不需要 protocolConfig（HTTP 路径已是固定端点）
trigger 不需要 timeoutMs（超时由执行引擎统一控制）
trigger 不需要 outputSchema（输出由编排的 exit 节点定义）
```---

## 2. 统一字段命名规则

| 上下文 | 规则 | 示例 |
|--------|------|------|
| **JSON 内部所有键名** | camelCase | `nameCn` / `authTypeSchema` / `connectorVersionId` |
| **引用外部资源 ID** | `*Id` 后缀 + string 类型 | `connectorVersionId: "1234567890"` |
| **时间字段** | `*Time` 后缀 | `createTime` / `publishedTime` / `startedTime` |
| **布尔字段** | `is*` 前缀 | `isDeleted` / `isTest` |
| **扩展字段** | `x_*` 前缀（V1） | `x_customMetadata` |
| **枚举值** | UPPER_SNAKE_CASE | `SOA` / `APIG` / `SYSTOKEN` / `AKSK` / `NONE` |

---

## 3. 共享 Schema 组件

以下组件在触发器和连接器中复用，定义一次多处引用。

### 3.1 authTypeSchema — 认证类型声明

```json
{
  "authTypeSchema": {
    "type": "SOA | APIG | SYSTOKEN | AKSK | NONE",
    "carrier": "header | query",
    "fieldName": "Authorization | X-Sys-Token | ...",
    "required": true,
    "fields": [
      { "name": "accessKey", "required": true, "sensitive": true },
      { "name": "secretKey", "required": true, "sensitive": true }
    ]
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `type` | string(UPPER) | ✅ | 认证类型枚举，见下文 |
| `carrier` | string | ✅ | 凭证传递位置：`header` / `query` |
| `fieldName` | string | ✅ | 凭证字段名（如 `Authorization` / `X-Sys-Token`）|
| `required` | boolean | ❌ 默认 true | 该认证是否强制 |
| `fields` | array[object] | ❌ | 凭证字段声明列表（用于 AKSK 等多字段认证）|

**fields 数组元素**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `name` | string | ✅ | 字段名（如 `accessKey` / `secretKey`）|
| `required` | boolean | ❌ 默认 true | 是否必填 |
| `sensitive` | boolean | ❌ 默认 false | 是否为敏感字段（运行时脱敏）|

**type 枚举上下文**：

| 上下文 | 可用枚举 | 说明 |
|--------|---------|------|
| 连接器认证（调用下游 API） | `SOA` / `APIG` / `NONE` / `AKSK` | 接入开放平台认证体系 |
| 触发器认证（外部触发流） | `SYSTOKEN` | 本版本仅此一种，其余按需扩展 |

### 3.2 rateLimit — 限流配置

```json
{
  "rateLimit": {
    "maxQps": 10,
    "maxConcurrency": 5
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `maxQps` | integer | ❌ 默认无限制 | 每秒最大请求数（QPS） |
| `maxConcurrency` | integer | ❌ 默认无限制 | 最大并发数 |

> 触发器和连接器的 rateLimit 结构**完全一致**，仅限流维度不同：触发器限流按 `flowId`，连接器限流按 `connectorVersionId`。

### 3.3 inputSchema / outputSchema — 数据契约

```json
{
  "inputSchema": {
    "type": "object",
    "properties": {
      "sender": { "type": "string", "description": "发送者 ID" },
      "content": { "type": "string", "description": "消息内容" }
    },
    "required": ["sender", "content"]
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `type` | string | ✅ | 固定 `"object"`（顶层） |
| `properties` | object | ✅ | 字段定义，value 为 [JSON Schema](https://json-schema.org/draft-07/json-schema-validation.html) 字段规则 |
| `required` | array[string] | ❌ | 必填字段列表 |
| `description` | string | ❌ | Schema 说明 |

> 遵循 JSON Schema draft-07 子集：支持 `type` / `properties` / `required` / `description` / `items`（array）/ `enum` / `default` / `minimum` / `maximum`。---

## 4. 各上下文 Schema 定义

### 4.1 触发器 — `orchestration_config.trigger`

**职责**：声明外部系统如何触发连接流、需携带什么凭证、传入什么数据、频率上限。

```json
{
  "trigger": {
    "type": "http | manual",
    "authTypeSchema": { "...见 §3.1 共享组件" },
    "inputSchema": { "...见 §3.3 数据契约" },
    "rateLimit": { "...见 §3.2 限流配置" }
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `type` | string | ✅ | 触发方式：`http` / `manual` / `test` |
| `authTypeSchema` | object | ❌ | 认证声明，`type=manual` 时可为空 |
| `inputSchema` | object | ❌ | 请求体 Schema，`type=manual` 时可为空 |
| `rateLimit` | object | ❌ 默认 100 qps | HTTP 触发限流 |

> 💡 **设计说明**：触发器不包含 `protocolConfig`（HTTP 路径 `/trigger/{flowId}/invoke` 固定）、`timeoutMs`（超时由执行引擎统一设定）、`outputSchema`（响应结构由编排出口节点定义）。这些字段不在触发器中出现，符合「不用的字段不出现」原则。

### 4.2 连接器 — `connection_config`

**职责**：声明连接器如何调用下游 API——去哪调、协议、认证、数据契约、超时、限流。

```json
{
  "connectionConfig": {
    "protocol": "HTTP",
    "protocolConfig": {
      "url": "https://api.example.com/im/send",
      "method": "POST",
      "headers": { "Content-Type": "application/json" }
    },
    "authTypeSchema": { "...见 §3.1 共享组件" },
    "inputSchema": { "...见 §3.3 数据契约" },
    "outputSchema": { "...见 §3.3 数据契约" },
    "timeoutMs": 30000,
    "rateLimit": { "...见 §3.2 限流配置" }
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `protocol` | string | ✅ | 协议，MVP 仅 `HTTP` |
| `protocolConfig` | object | ✅ | 协议配置（url / method / headers） |
| `authTypeSchema` | object | ❌ | 认证声明 |
| `inputSchema` | object | ❌ | 入参 Schema，下游 API 请求体结构 |
| `outputSchema` | object | ❌ | 出参 Schema，下游 API 响应体结构 |
| `timeoutMs` | integer | ❌ 默认 30000 | 单次调用超时 |
| `rateLimit` | object | ❌ | 出站限流 |

### 4.3 编排配置 — `orchestration_config`

```json
{
  "orchestrationConfig": {
    "trigger": { "...见 §4.1" },
    "nodes": [
      {
        "id": "node_entry",
        "type": "entry | connector | data_processor | exit",
        "labelCn": "接收请求",
        "labelEn": "Receive Request",
        "connectorVersionId": "1234567890",
        "inputMapping": { "receiver": "${trigger.sender}" },
        "config": { "fieldMappings": [...] },
        "outputFields": ["result.id", "result.status"],
        "position": { "x": 100, "y": 200 }
      }
    ],
    "edges": [
      {
        "id": "e1",
        "sourceNodeId": "node_entry",
        "targetNodeId": "node_1"
      }
    ]
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `trigger` | object | ✅ | 触发器定义，见 §4.1 |
| `nodes` | array[object] | ✅ | 编排节点列表，有序 |
| `edges` | array[object] | ✅ | 编排连线列表 |

**nodes[].type 各类专属字段**：

| 节点类型 | 专属字段 | 说明 |
|---------|---------|------|
| `entry` | 无（配置在 `trigger` 中） | 入口节点，不占用额外配置 |
| `connector` | `connectorVersionId` / `inputMapping` | 引用的连接器版本 + 参数映射 |
| `data_processor` | `config.fieldMappings` | 字段映射规则 |
| `exit` | `outputFields` | 定义哪些字段出现在返回结果中 |

### 4.4 执行数据 — `execution_record_t` / `execution_step_t`

```json
// execution_record_t.trigger_data / result_data
{
  "sender": "user_001",
  "content": "你好"
}

// execution_step_t.input_data / output_data
{
  "msgId": "msg_xxxx",
  "code": 0
}

// execution_step_t.error_info（失败时）
{
  "code": "DOWNSTREAM_UNAVAILABLE",
  "message": "HTTP 503 服务不可用",
  "downstreamStatus": 503,
  "downstreamBody": "Service Unavailable"
}
```

> 💡 **说明**：执行数据的结构由对应节点的 `inputSchema` / `outputSchema` / `errorInfo` 动态决定，不在数据库层约束。`error_info` 统一使用结构化格式（含 code/message/downstreamStatus/downstreamBody），便于前端分类展示。---

## 5. 版本演进规则

| 场景 | 处理方式 |
|------|---------|
| **新增可选字段** | 直接加，不影响已有数据 |
| **新增必填字段** | 发新版本，旧数据迁移赋默认值 |
| **字段改名** | ❌ 不允许，如确需改直接废弃旧字段 + 新增新字段 |
| **字段废弃** | 保留字段名，标注 `deprecated: true` + `x_replacedBy: "newField"`，至少保持一版本兼容 |
| **枚举值新增** | 直接加，应用层做好未知值降级处理 |
| **枚举值删除** | ❌ 不允许，标记为 `deprecated` |

> 💡 **向后兼容**：所有 Schema 变更遵循「加不加删、改不删」原则——可以加新字段、新枚举值，但不可以删已有字段、改已有字段名。

---

## 附录 A：修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-05-22 | 初始版本——统一连接器平台 JSON Schema 设计规范（共享组件 + 上下文 Schema + 演进规则） | SDDU Plan Agent |