# JSON Schema 设计规范：连接器平台

**关联文档**: plan.md, plan-db.md (§3 表结构定义), plan-api.md (§3 接口详细定义), plan-json-schema.md  
**版本**: v2.0  
**创建日期**: 2026-05-22  

---

## 1. 设计哲学

### 1.1 设计目标

| 目标 | 说明 |
|------|------|
| **自描述** | Schema 本身说清字段含义、类型、约束，不散落在代码注释中 |
| **一致性** | 同一语义的字段在不同上下文中命名统一 |
| **可扩展** | 可新增字段，不破坏已有结构 |
| **无冗余** | 不用的字段不出现在 Schema 中 |

### 1.2 参考标准

| 标准 | 参考程度 | 说明 |
|------|---------|------|
| JSON Schema (draft-07) | ⭐⭐⭐ 核心 | `type` / `properties` / `required` / `description` 等元字段直接复用 |
| OpenAPI 3.0 components/schemas | ⭐⭐⭐ 结构 | 可复用组件（authTypeSchema / rateLimit）+ 按场景组合的思想 |

### 1.3 核心原则

```
原则一：同一事物同一个名
  authTypeSchema → 触发器和连接器用同一结构
  rateLimit      → 入站和出站限流用同一结构
  inputSchema    → 触发器和连接器统一命名

原则二：不用的字段不出现
  trigger 不需要 protocolConfig（HTTP 端点固定）
  trigger 不需要 timeoutMs（引擎统一控制）
  trigger 不需要 outputSchema（由编排 exit 节点定义）
```

---

## 2. 统一字段命名规则

| 上下文 | 规则 | 示例 |
|--------|------|------|
| JSON 内部所有键名 | camelCase | `nameCn` / `authTypeSchema` / `connectorVersionId` |
| 引用外部资源 ID | `*Id` 后缀 + string 类型 | `connectorVersionId: "1234567890"` |
| 时间字段 | `*Time` 后缀 | `createTime` / `publishedTime` |
| 布尔字段 | `is*` 前缀 | `isDeleted` / `isTest` |
| 扩展字段（V1） | `x_*` 前缀 | `x_customMetadata` |
| 枚举值 | UPPER_SNAKE_CASE | `SOA` / `APIG` / `SYSTOKEN` / `AKSK` / `NONE` |

---

## 3. 共享 Schema 组件

以下组件在各上下文中复用。

### 3.1 authTypeSchema

**Schema 定义**：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "authTypeSchema",
  "description": "认证类型声明，声明调用方需携带的认证凭证",
  "type": "object",
  "properties": {
    "type": {
      "type": "string",
      "description": "认证类型枚举",
      "enum": ["SOA", "APIG", "SYSTOKEN", "AKSK", "NONE"]
    },
    "fields": {
      "type": "array",
      "description": "凭证字段列表，每个元素定义一个凭证字段的完整信息",
      "items": {
        "type": "object",
        "properties": {
          "name": { "type": "string", "description": "字段名，程序内部标识" },
          "carrier": { "type": "string", "description": "传递位置", "enum": ["header", "query"] },
          "fieldName": { "type": "string", "description": "实际携带时的字段名，如 Authorization / X-Sys-Token" },
          "required": { "type": "boolean", "default": true },
          "sensitive": { "type": "boolean", "default": false, "description": "运行时脱敏" }
        },
        "required": ["name", "carrier", "fieldName"]
      }
    }
  },
  "required": ["type"]
}
```

**示例**：

```json
{
  "authTypeSchema": {
    "type": "SYSTOKEN",
    "fields": [
      { "name": "token", "carrier": "header", "fieldName": "X-Sys-Token", "required": true }
    ]
  }
}
```

```json
{
  "authTypeSchema": {
    "type": "AKSK",
    "fields": [
      { "name": "accessKey", "carrier": "header", "fieldName": "AK", "required": true, "sensitive": true },
      { "name": "secretKey", "carrier": "header", "fieldName": "SK", "required": true, "sensitive": true }
    ]
  }
}
```

**type 枚举上下文**：

| 上下文 | 可用枚举 | 说明 |
|--------|---------|------|
| 连接器认证（调用下游 API） | `SOA` / `APIG` / `NONE` / `AKSK` | 接入开放平台认证体系 |
| 触发器认证（外部触发流） | `SYSTOKEN` | 本版本仅此一种 |

### 3.2 rateLimit

**Schema 定义**：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "rateLimit",
  "description": "限流配置，触发器和连接器复用同一结构",
  "type": "object",
  "properties": {
    "maxQps": {
      "type": "integer",
      "description": "每秒最大请求数",
      "minimum": 1
    },
    "maxConcurrency": {
      "type": "integer",
      "description": "最大并发数",
      "minimum": 1
    }
  }
}
```

**示例**：

```json
{
  "rateLimit": {
    "maxQps": 10,
    "maxConcurrency": 5
  }
}
```

> 触发器和连接器的 rateLimit 结构完全一致，仅限流维度不同：触发器按 `flowId` 限流，连接器按 `connectorVersionId` 限流。

### 3.3 inputSchema / outputSchema

**Schema 定义**（遵循 JSON Schema draft-07 子集）：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "dataSchema",
  "description": "数据契约，描述输入/输出的数据结构。遵循 JSON Schema draft-07 子集",
  "type": "object",
  "properties": {
    "type": {
      "type": "string",
      "description": "顶层固定为 object",
      "enum": ["object"]
    },
    "properties": {
      "type": "object",
      "description": "字段定义，value 为标准 JSON Schema 字段规则",
      "additionalProperties": {
        "type": "object",
        "properties": {
          "type": { "type": "string" },
          "description": { "type": "string" },
          "items": { "type": "object" },
          "enum": { "type": "array" },
          "default": {},
          "minimum": { "type": "number" },
          "maximum": { "type": "number" }
        },
        "required": ["type"]
      }
    },
    "required": {
      "type": "array",
      "description": "必填字段列表",
      "items": { "type": "string" }
    }
  },
  "required": ["type", "properties"]
}
```

**示例**（inputSchema）：

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

---

## 4. 各上下文 Schema 定义

### 4.1 触发器 — orchestrationConfig.trigger

**Schema 定义**：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "trigger",
  "description": "触发器定义，外部系统触发连接流的入口配置",
  "type": "object",
  "properties": {
    "type": {
      "type": "string",
      "description": "触发方式",
      "enum": ["http", "manual", "test"]
    },
    "authTypeSchema": {
      "$ref": "#/definitions/authTypeSchema"
    },
    "inputSchema": {
      "$ref": "#/definitions/inputSchema"
    },
    "rateLimit": {
      "$ref": "#/definitions/rateLimit"
    }
  },
  "required": ["type"]
}
```

**示例**：

```json
{
  "trigger": {
    "type": "http",
    "authTypeSchema": {
      "type": "SYSTOKEN",
      "fields": [
        { "name": "token", "carrier": "header", "fieldName": "X-Sys-Token" }
      ]
    },
    "inputSchema": {
      "type": "object",
      "properties": {
        "sender": { "type": "string", "description": "发送者 ID" },
        "content": { "type": "string", "description": "消息内容" }
      },
      "required": ["sender", "content"]
    },
    "rateLimit": {
      "maxQps": 100
    }
  }
}
```

> 💡 触发器不含 protocolConfig（HTTP 端点固定），不含 timeoutMs（引擎统一），不含 outputSchema（由编排 exit 节点定义）。

### 4.2 连接器 — connectionConfig

**Schema 定义**：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "connectionConfig",
  "description": "连接器配置，声明如何调用下游 API",
  "type": "object",
  "properties": {
    "protocol": {
      "type": "string",
      "description": "协议类型，MVP 仅 HTTP",
      "enum": ["HTTP"]
    },
    "protocolConfig": {
      "type": "object",
      "description": "协议配置",
      "properties": {
        "url": { "type": "string" },
        "method": { "type": "string", "enum": ["GET", "POST", "PUT", "DELETE", "PATCH"] },
        "headers": { "type": "object" }
      },
      "required": ["url", "method"]
    },
    "authTypeSchema": { "$ref": "#/definitions/authTypeSchema" },
    "inputSchema": { "$ref": "#/definitions/inputSchema" },
    "outputSchema": { "$ref": "#/definitions/outputSchema" },
    "timeoutMs": {
      "type": "integer",
      "description": "单次调用超时",
      "default": 30000,
      "minimum": 1000
    },
    "rateLimit": { "$ref": "#/definitions/rateLimit" }
  },
  "required": ["protocol", "protocolConfig"]
}
```

**示例**：

```json
{
  "connectionConfig": {
    "protocol": "HTTP",
    "protocolConfig": {
      "url": "https://api.example.com/im/send",
      "method": "POST",
      "headers": { "Content-Type": "application/json" }
    },
"authTypeSchema": {
      "type": "SYSTOKEN",
      "fields": [
        { "name": "token", "carrier": "header", "fieldName": "X-Sys-Token" }
      ]
    },
    "inputSchema": {
      "type": "object",
      "properties": {
        "receiver": { "type": "string", "description": "接收者 ID" },
        "content": { "type": "string", "description": "消息内容" }
      },
      "required": ["receiver", "content"]
    },
    "outputSchema": {
      "type": "object",
      "properties": {
        "msgId": { "type": "string", "description": "消息 ID" }
      }
    },
    "timeoutMs": 30000,
    "rateLimit": {
      "maxQps": 10,
      "maxConcurrency": 5
    }
  }
}
```

### 4.3 编排配置 — orchestrationConfig

**Schema 定义**：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "orchestrationConfig",
  "type": "object",
  "properties": {
    "trigger": {
      "type": "object",
      "description": "触发器定义，见 §4.1 trigger schema",
      "properties": {
        "type": { "$ref": "#/definitions/triggerType" },
        "authTypeSchema": { "$ref": "#/definitions/authTypeSchema" },
        "inputSchema": { "$ref": "#/definitions/inputSchema" },
        "rateLimit": { "$ref": "#/definitions/rateLimit" }
      },
      "required": ["type"]
    },
    "nodes": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "id": { "type": "string", "description": "节点 ID，编排内部唯一" },
          "type": {
            "type": "string",
            "enum": ["entry", "connector", "data_processor", "exit"]
          },
          "labelCn": { "type": "string" },
          "labelEn": { "type": "string" },
          "connectorVersionId": { "type": "string", "description": "connector 节点专属：引用的连接器版本 ID" },
          "inputMapping": { "type": "object", "description": "connector 节点专属：参数映射" },
          "config": {
            "type": "object",
            "description": "data_processor 节点专属：字段映射配置",
            "properties": {
              "fieldMappings": {
                "type": "array",
                "items": {
                  "type": "object",
                  "properties": {
                    "source": { "type": "string" },
                    "target": { "type": "string" }
                  }
                }
              }
            }
          },
          "outputFields": {
            "type": "array",
            "description": "exit 节点专属：输出字段列表",
            "items": { "type": "string" }
          },
          "position": {
            "type": "object",
            "description": "画布坐标",
            "properties": {
              "x": { "type": "integer" },
              "y": { "type": "integer" }
            }
          }
        },
        "required": ["id", "type"]
      }
    },
    "edges": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "id": { "type": "string" },
          "sourceNodeId": { "type": "string" },
          "targetNodeId": { "type": "string" }
        },
        "required": ["id", "sourceNodeId", "targetNodeId"]
      }
    }
  },
  "required": ["trigger", "nodes", "edges"]
}
```

**示例**：

```json
{
  "trigger": {
    "type": "http",
    "authTypeSchema": {
      "type": "SYSTOKEN",
      "fields": [
        { "name": "token", "carrier": "header", "fieldName": "X-Sys-Token" }
      ]
    },
    "inputSchema": {
      "type": "object",
      "properties": {
        "sender": { "type": "string" },
        "content": { "type": "string" }
      },
      "required": ["sender", "content"]
    },
    "rateLimit": { "maxQps": 100 }
  },
  "nodes": [
    {
      "id": "node_entry",
      "type": "entry",
      "labelCn": "接收请求",
      "labelEn": "Receive Request",
      "position": { "x": 100, "y": 200 }
    },
    {
      "id": "node_1",
      "type": "connector",
      "labelCn": "发送消息",
      "labelEn": "Send Message",
      "connectorVersionId": "1234567890123456789",
      "inputMapping": {
        "receiver": "${trigger.sender}",
        "content": "${trigger.content}"
      },
      "position": { "x": 350, "y": 200 }
    },
    {
      "id": "node_exit",
      "type": "exit",
      "labelCn": "返回结果",
      "labelEn": "Return Result",
      "outputFields": ["result.msgId", "result.code"],
      "position": { "x": 650, "y": 200 }
    }
  ],
  "edges": [
    { "id": "e1", "sourceNodeId": "node_entry", "targetNodeId": "node_1" },
    { "id": "e2", "sourceNodeId": "node_1", "targetNodeId": "node_exit" }
  ]
}
```

### 4.4 执行数据 — executionRecord / executionStep

> 执行数据的结构由对应节点的 inputSchema / outputSchema 动态决定，不在数据库层约束。errorInfo 统一使用结构化格式。

**errorInfo Schema 定义**：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "errorInfo",
  "type": "object",
  "properties": {
    "code": { "type": "string", "description": "错误码" },
    "message": { "type": "string", "description": "错误描述" },
    "downstreamStatus": { "type": "integer", "description": "下游 HTTP 状态码" },
    "downstreamBody": { "type": "string", "description": "下游响应体片段" }
  },
  "required": ["code", "message"]
}
```

**示例**：

```json
// trigger_data / result_data
{
  "sender": "user_001",
  "content": "你好"
}

// input_data / output_data
{
  "msgId": "msg_xxxx",
  "code": 0
}

// errorInfo（失败时）
{
  "code": "DOWNSTREAM_UNAVAILABLE",
  "message": "HTTP 503 服务不可用",
  "downstreamStatus": 503,
  "downstreamBody": "Service Unavailable"
}
```

---

## 5. 版本演进规则

| 场景 | 处理方式 |
|------|---------|
| **新增可选字段** | 直接加，不影响已有数据 |
| **新增必填字段** | 发新版本，旧数据迁移赋默认值 |
| **字段改名** | ❌ 不允许，废弃旧字段 + 新增新字段 |
| **字段废弃** | 保留字段名，标注 `deprecated: true` + `x_replacedBy: "newField"` |
| **枚举值新增** | 直接加，应用层做好未知值降级 |
| **枚举值删除** | ❌ 不允许，标记为 deprecated |

> **向后兼容**：加不加删、改不删。可以加新字段、新枚举值，不可以删已有字段、改已有字段名。

---

## 附录 A：修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-05-22 | 初始版本 | SDDU Plan Agent |
| v2.0 | 2026-05-22 | 重写为标准 JSON Schema 格式 + 示例分离 + 修复跨引用占位符 | SDDU Plan Agent |