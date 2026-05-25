# JSON Schema 设计规范：连接器平台

**关联文档**: plan.md, plan-db.md (§3 表结构定义), plan-api.md (§3 接口详细定义)  
**版本**: v3.4
**创建日期**: 2026-05-22  
**最后更新**: 2026-05-25
**修订说明**: v3.4 — v3.0 修复 14 个设计问题 + v3.1~v3.4 React Flow 格式对齐系列（§7.2.2 四种内置类型示例 + input/default/output/group 类型表 + §8 差异分析）

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
| JSON Schema (draft-07) | ⭐⭐⭐ 核心 | `type` / `properties` / `required` / `description` / `definitions` / `oneOf` / `allOf` / `if`-`then` 等元字段直接复用 |
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

原则三：DAG（有向无环图）的边也是数据，需要语义
  edge 不仅是"谁连到谁"，还承载执行条件（condition）/ 错误路由（error）/ 优先级
  MVP 仅用 default 边，但数据结构须为 V1 分支/容错/并行留好扩展槽
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
| **数据库列级枚举** | **TINYINT 数字**（plan-db.md §0.7 规范） | `connector_type=1`, `lifecycle_status=2` |
| **JSON 内嵌枚举** | **UPPER_SNAKE_CASE 字符串**（例外，见 §2.1） | `"SOA"` / `"APIG"` / `"SYSTOKEN"` / `"AKSK"` / `"NONE"` |

### 2.1 JSON 内嵌枚举使用字符串的例外说明

> ⚠️ **设计决策**：`authTypeSchema.type` 作为存储在 `MEDIUMTEXT` JSON 字段内的嵌套值，使用**字符串枚举**而非 TINYINT 数字。这与 `plan-db.md` §0.7「所有枚举字段统一 TINYINT(10)」规则表面冲突，但属于**有意为之的例外**：
>
> | 维度 | 数据库列级枚举 | JSON 内嵌枚举 |
> |------|--------------|-------------|
> | **字段位置** | MySQL 列（如 `connector_type tinyint`） | MEDIUMTEXT 列的 JSON 子字段 |
> | **枚举表示** | TINYINT 数字 | 字符串（`"SOA"` / `"AKSK"` 等） |
> | **设计理由** | 存储效率 + 索引效率 | 人类可读：前端 React Flow 属性面板直接展示；跨语言 debugging 无需查字典；版本快照 self-describing |
> | **ORM 映射** | MyBatis/R2DBC 直接映射 int | Jackson 序列化/反序列化字符串 → Java enum |
> | **规范适用** | plan-db.md §0.7 | 本文档 §2（本节） |
>
> 枚举值对应关系（JSON 字符串 ⇄ DB TINYINT，应用层映射）：
>
> | JSON 字符串 | TINYINT 代码 | 使用上下文 |
> |------------|:-----------:|-----------|
> | `SOA` | 1 | 连接器认证 |
> | `APIG` | 2 | 连接器认证 |
> | `NONE` | 4 | 连接器认证 |
> | `AKSK` | 5 | 连接器认证 |
> | `SYSTOKEN` | 7 | 触发器认证 |

---

## 3. definitions 聚合段（🆕 v3.0）

> 💡 **v3.0 新增**：所有 `$ref` 引用的共享组件在此聚合。以下 §4 节中的各上下文 Schema 通过 `#/definitions/xxx` 引用这些组件，保证 `$ref` 路径可解析。
>
> ### §3.0 字段名 ↔ 校验类型映射
>
> 为避免「字段名与 Schema 名相同」产生的歧义，本规范区分两者：
> - **字段名（field name）**：JSON 数据中的属性键，描述「存什么数据」（如 `authTypeSchema`、`rateLimit`）
> - **校验类型（definition key）**：definitions 中的组件键名，描述「用什么规则校验」（如 `authTypeDeclaration`、`rateLimitConfig`）
>
> ```mermaid
> graph LR
>     subgraph Fields["JSON 字段名<br/>（数据侧）"]
>         F1["authTypeSchema<br/>认证 schema 数据"]
>         F2["rateLimit<br/>限流数据"]
>         F3["inputSchema<br/>入参 schema 数据"]
>         F4["outputSchema<br/>出参 schema 数据"]
>         F5["errorInfo<br/>错误信息数据"]
>         F6["position<br/>画布坐标数据"]
>     end
>
>     subgraph Defs["definitions 校验类型<br/>（规则侧）"]
>         D1["authTypeDeclaration<br/>校验认证类型声明"]
>         D2["rateLimitConfig<br/>校验限流配置"]
>         D3["dataContract<br/>校验数据契约结构"]
>         D5["errorDetail<br/>校验错误详情"]
>         D6["canvasPosition<br/>校验画布坐标"]
>     end
>
>     F1 -- "$ref" --> D1
>     F2 -- "$ref" --> D2
>     F3 -- "$ref" --> D3
>     F4 -- "$ref" --> D3
>     F5 -- "$ref" --> D5
>     F6 -- "$ref" --> D6
>
>     style Fields fill:#e3f2fd,stroke:#1565c0
>     style Defs fill:#fff3e0,stroke:#ef6c00
> ```
>
> **副作用**：以下 Schema 定义中，你会看到 `"authTypeSchema": { "$ref": "#/definitions/authTypeDeclaration" }`——左侧是字段名，右侧是校验类型，两者语义关联但字面不同。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "urn:openapp:schema:definitions:v1",
  "title": "共享 Schema 组件聚合",
  "description": "所有上下文 Schema 共用的组件定义",

  "definitions": {

    "authTypeDeclaration": {
      "$id": "urn:openapp:schema:authTypeDeclaration:v1",
      "title": "authTypeDeclaration",
      "description": "认证类型声明。校验 JSON 字段 authTypeSchema 的数据结构，声明调用方需携带的认证凭证。type 使用字符串枚举（见 §2.1 例外说明）",
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "type": {
          "type": "string",
          "description": "认证类型枚举（JSON 内嵌字段用字符串，非 TINYINT；参见 §2.1）",
          "enum": ["SOA", "APIG", "SYSTOKEN", "AKSK", "NONE"]
        },
        "fields": {
          "type": "array",
          "description": "凭证字段列表，每个元素定义一个凭证字段的完整信息",
          "items": {
            "type": "object",
            "additionalProperties": false,
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
    },

    "rateLimitConfig": {
      "$id": "urn:openapp:schema:rateLimitConfig:v1",
      "title": "rateLimitConfig",
      "description": "限流配置。校验 JSON 字段 rateLimit 的数据结构，触发器和连接器复用同一类型",
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "maxQps": {
          "type": "integer",
          "description": "每秒最大请求数（1-10000）",
          "minimum": 1,
          "maximum": 10000
        },
        "maxConcurrency": {
          "type": "integer",
          "description": "最大并发数（1-1000）",
          "minimum": 1,
          "maximum": 1000
        }
      }
    },

    "dataContract": {
      "$id": "urn:openapp:schema:dataContract:v1",
      "title": "dataContract",
      "description": "数据契约。校验 JSON 字段 inputSchema / outputSchema 的数据结构，遵循 JSON Schema draft-07 子集",
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
    },

"dataContractAlias": {
      "$ref": "urn:openapp:schema:dataContract:v1",
      "description": "outputSchema 字段与 inputSchema 共用同一个数据契约校验类型"
    },

    "errorDetail": {
      "$id": "urn:openapp:schema:errorDetail:v1",
      "title": "errorDetail",
      "description": "错误详情。校验 JSON 字段 errorInfo 的数据结构",
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "code": { "type": "string", "description": "错误码" },
        "message": { "type": "string", "description": "错误描述" },
        "cause": {
          "type": "string",
          "description": "根因描述，非下游错误时使用（如 'JSON 解析失败：unexpected token at line 3'、'字段映射失败：source 字段不存在'）"
        },
        "downstreamStatus": { "type": "integer", "description": "下游 HTTP 状态码（下游调用失败时）" },
        "downstreamBody": { "type": "string", "description": "下游响应体片段（截断到 512 字符）" }
      },
      "required": ["code", "message"],
      "oneOf": [
        { "required": ["cause"], "description": "内部错误" },
        { "required": ["downstreamStatus"], "description": "下游错误" }
      ]
    },

    "canvasPosition": {
      "$id": "urn:openapp:schema:canvasPosition:v1",
      "title": "canvasPosition",
      "description": "画布坐标。校验 JSON 字段 position 的数据结构，React Flow (@xyflow/react) 使用浮点坐标",
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "x": { "type": "number", "description": "画布 X 坐标" },
        "y": { "type": "number", "description": "画布 Y 坐标" }
      }
    }
  }
}
```

---

## 4. 各上下文 Schema 定义

> 💡 以下每个 Schema 均为独立校验单元。所有 `$ref` 路径引用 §3 definitions 中的共享组件，保证引用可解析。

### 4.1 触发器 — node type="trigger" 专属配置

该 Schema 定义了 `orchestrationConfig.nodes` 中 `type="trigger"` 节点的配置结构。

> 💡 **trigger ⇄ entry 命名映射**：编排配置中的 `type="trigger"` 对应执行记录 `execution_step_t.node_type = 1 (entry)`。两者语义等价——"trigger" 强调配置视角（声明触发方式），"entry" 强调运行时视角（DAG 入口节点）。跨文档命名已统一见 plan-db.md §0.7 枚举表。

**Schema 定义**：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "urn:openapp:schema:triggerConfig:v1",
  "title": "triggerConfig",
  "description": "触发器节点配置，外部系统触发连接流的 DAG 入口节点",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "type": {
      "type": "string",
      "description": "触发方式。MVP: http/manual/test",
      "enum": ["http", "manual", "test"]
    },
    "authTypeSchema": {
      "$ref": "#/definitions/authTypeDeclaration",
      "description": "HTTP 触发时声明外部调用方需携带的认证凭证类型（仅声明 schema，不含凭证值）"
    },
    "inputSchema": {
      "$ref": "#/definitions/dataContract",
      "description": "触发请求体的 JSON Schema（HTTP 触发时校验请求体）"
    },
    "rateLimit": {
      "$ref": "#/definitions/rateLimitConfig"
    }
  },
  "required": ["type"],
  "allOf": [
    {
      "if": {
        "properties": { "type": { "const": "http" } },
        "required": ["type"]
      },
      "then": {
        "required": ["authTypeSchema", "inputSchema"],
        "description": "HTTP 触发必须声明认证类型 schema 和入参 schema"
      }
    },
    {
      "if": {
        "properties": { "type": { "const": "manual" } },
        "required": ["type"]
      },
      "then": {
        "properties": {
          "authTypeSchema": false,
          "inputSchema": false
        },
        "description": "手动触发不需要认证和入参 schema（管理员手动填写参数）"
      }
    },
    {
      "if": {
        "properties": { "type": { "const": "test" } },
        "required": ["type"]
      },
      "then": {
        "description": "测试运行使用草稿编排配置，入参由管理员在 wecodesite 中填写模拟数据"
      }
    }
  ]
}
```

**示例**（作为 `nodes` 中的一个 trigger 节点）：

```json
{
  "id": "node_trigger",
  "type": "trigger",
  "labelCn": "接收请求",
  "labelEn": "Receive Request",
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
  },
  "position": { "x": 100.0, "y": 200.0 }
}
```

> 💡 触发器节点不含 protocolConfig（HTTP 端点固定）、不含 timeoutMs（引擎统一控制）、不含 outputSchema（由编排 exit 节点定义）。

**type 枚举上下文**：

| 上下文 | 可用枚举 | 说明 |
|--------|---------|------|
| 连接器认证（调用下游 API） | `SOA` / `APIG` / `NONE` / `AKSK` | 接入开放平台认证体系 |
| 触发器认证（外部触发流） | `SYSTOKEN` | 本版本仅此一种 |

---

### 4.2 连接器 — connectionConfig

**Schema 定义**：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "urn:openapp:schema:connectionConfig:v1",
  "title": "connectionConfig",
  "description": "连接器配置，声明如何调用下游 API。该 JSON 存储在 connector_version_t.connection_config MEDIUMTEXT 字段中",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "protocol": {
      "type": "string",
      "description": "协议类型，MVP 仅 HTTP",
      "enum": ["HTTP"]
    },
    "protocolConfig": {
      "type": "object",
      "additionalProperties": false,
      "description": "协议配置",
      "properties": {
        "url": { "type": "string", "description": "下游 API 完整 URL" },
        "method": { "type": "string", "enum": ["GET", "POST", "PUT", "DELETE", "PATCH"] },
        "headers": {
          "type": "object",
          "description": "固定请求头（如 Content-Type），运行时注入的认证头不在此声明"
        }
      },
      "required": ["url", "method"]
    },
    "authTypeSchema": { "$ref": "#/definitions/authTypeDeclaration" },
    "inputSchema": { "$ref": "#/definitions/dataContract" },
    "outputSchema": { "$ref": "#/definitions/dataContract" },
    "timeoutMs": {
      "type": "integer",
      "description": "单次调用超时（毫秒）",
      "default": 30000,
      "minimum": 1000,
      "maximum": 300000
    },
    "rateLimit": { "$ref": "#/definitions/rateLimitConfig" }
  },
  "required": ["protocol", "protocolConfig"]
}
```

**示例**：

```json
{
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
```

> 💡 示例中不再出现 `$schemaName` 字段。版本标识由数据库 `connector_version_t.version_no` 承载，JSON 内容本身无需自标版本——应用层通过 Jackson 反序列化到固定 POJO 即可。

---

### 4.3 编排配置 — orchestrationConfig

> 🎯 **DAG（Directed Acyclic Graph，有向无环图）编排设计思路**
>
> 连接流编排的本质是将一个业务流程建模为有向无环图：每个节点执行一个原子操作（调用 API、转换数据），边决定操作的执行顺序和条件。连接流平台采用 **显式 DAG（nodes + edges）** 模型，参考 Make 平台的模块+路由表模式。
>
> ```mermaid
> graph TD
>     subgraph DAG["DAG = 节点（做什么）+ 边（何时做、以什么条件做）"]
>         subgraph Nodes["节点（nodes[]）"]
>             N1["trigger<br/>DAG 入口<br/>声明触发条件和入参契约"]
>             N2["connector<br/>数据获取/操作节点<br/>引用已发布的连接器版本"]
>             N3["data_processor<br/>管道节点<br/>原地转换数据，不改变拓扑<br/>（字段映射/表达式计算）"]
>             N4["exit<br/>DAG 出口<br/>声明对外暴露的返回值字段"]
>         end
>         subgraph Edges["边（edges[]）"]
>             E1["承载执行语义<br/>default / condition / error / always"]
>             E2["MVP 仅 default<br/>（无条件顺序执行）"]
>             E3["V1 扩展<br/>条件分支 / 错误路由"]
>         end
>     end
> ```
>
> **为什么是显式 edges 而非隐式顺序？**
> - React Flow 画布天然产生 nodes + edges 两个数组
> - 显式边让拓扑排序可纯粹由数据驱动（不依赖 nodes 数组顺序）
> - 条件边/错误边需要独立存储过滤表达式，隐式模型（如嵌套 runAfter）不够用
> - 版本 diff 时，增删一条边比修改嵌套结构更清晰

**Schema 定义**：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "urn:openapp:schema:orchestrationConfig:v1",
  "title": "orchestrationConfig",
  "description": "连接流编排配置，以显式 DAG（nodes + edges）存储完整编排定义。该 JSON 存储在 flow_version_t.orchestration_config MEDIUMTEXT 字段中",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "nodes": {
      "type": "array",
      "minItems": 2,
      "description": "DAG 节点列表。MVP 最少 2 个（1 trigger + 1 exit），无上限",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string",
            "description": "节点 ID，编排内部唯一。由前端 React Flow 画布生成（如 'node_trigger' / 'node_a1b2c3'）"
          },
          "type": {
            "type": "string",
            "enum": ["trigger", "connector", "data_processor", "exit"],
            "description": "节点类型。trigger=入口, connector=连接器调用, data_processor=管道转换, exit=出口"
          },
          "labelCn": { "type": "string", "description": "节点中文标签" },
          "labelEn": { "type": "string", "description": "节点英文标签" },

          "authTypeSchema": {
            "$ref": "#/definitions/authTypeDeclaration",
            "description": "trigger 节点专属：认证类型声明"
          },
          "inputSchema": {
            "$ref": "#/definitions/dataContract",
            "description": "trigger 节点专属：入参 Schema"
          },
          "rateLimit": {
            "$ref": "#/definitions/rateLimitConfig",
            "description": "trigger 节点专属：触发频率限制"
          },

          "connectorVersionId": {
            "type": "string",
            "pattern": "^[1-9][0-9]{15,19}$",
            "description": "connector 节点专属：引用的连接器版本 ID（BIGINT 雪花 ID 转 string，18-20 位数字）"
          },
          "inputMapping": {
            "type": "object",
            "description": "connector 节点专属：上游数据字段 → 连接器 inputSchema 字段的映射。key 为连接器 inputSchema 字段名，value 为表达式（如 ${trigger.sender}）"
          },

          "config": {
            "type": "object",
            "additionalProperties": false,
            "description": "data_processor 节点专属：管道转换配置。data_processor 不改 DAG 拓扑，仅做原地数据转换",
            "properties": {
              "fieldMappings": {
                "type": "array",
                "description": "字段映射列表。source 支持 ${nodeId.fieldPath} 或 constant:value 表达式",
                "minItems": 1,
                "items": {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "source": {
                      "type": "string",
                      "pattern": "^(\\$\\{[a-zA-Z0-9_.]+\\}|constant:[a-zA-Z0-9_]+)$",
                      "description": "数据来源表达式。${nodeId.fieldPath} 引用上游节点输出，constant:xxx 为固定值"
                    },
                    "target": {
                      "type": "string",
                      "description": "目标字段路径，如 result.id / result.status"
                    }
                  },
                  "required": ["source", "target"]
                }
              }
            }
          },

          "outputFields": {
            "type": "array",
            "description": "exit 节点专属：对外暴露的返回值字段列表（如 ['result.msgId', 'result.code']）",
            "items": { "type": "string" },
            "minItems": 1
          },

          "position": { "$ref": "#/definitions/canvasPosition" }
        },
        "required": ["id", "type"]
      }
    },
    "edges": {
      "type": "array",
      "minItems": 1,
      "description": "DAG 边列表。存储节点间的执行顺序与条件。MVP 仅使用 default 边",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string",
            "description": "边 ID，编排内部唯一"
          },
          "sourceNodeId": {
            "type": "string",
            "description": "源节点 ID，必须对应 nodes[] 中的 id"
          },
          "targetNodeId": {
            "type": "string",
            "description": "目标节点 ID，必须对应 nodes[] 中的 id"
          },
          "type": {
            "type": "string",
            "enum": ["default"],
            "default": "default",
            "description": "边类型。MVP 仅 default（无条件顺序传递）；V1 扩展：condition（条件匹配）/ error（失败路由）/ always（无论成败）"
          },
          "label": {
            "type": "string",
            "description": "边标签（画布展示用），如 '成功' / '失败' / 'type=bug'。MVP 选填"
          }
        },
        "required": ["id", "sourceNodeId", "targetNodeId"]
      }
    }
  },
  "required": ["nodes", "edges"]
}
```

#### 4.3.1 DAG 拓扑约束（应用层校验）

> ⚠️ 以下约束由应用层强制校验，JSON Schema 层面不覆盖（跨节点/跨边引用校验需图遍历，非声明式 Schema 可表达）：

| 规则 | 说明 | MVP | 校验时机 |
|------|------|:---:|---------|
| **节点 ID 唯一** | `nodes[].id` 在编排内不重复 | ✅ | 保存/发布 |
| **边引用存在** | `sourceNodeId` / `targetNodeId` 必须对应 `nodes[]` 中已声明的 `id` | ✅ | 保存/发布 |
| **无孤儿节点** | 每个 `node.id`（除 trigger/exit）至少有 1 条入边 + 1 条出边 | ✅ | 保存/发布 |
| **trigger 不可为 target** | DAG 入口节点的入度必须为 0 | ✅ | 保存/发布 |
| **exit 不可为 source** | DAG 出口节点的出度必须为 0 | ✅ | 保存/发布 |
| **无环** | 拓扑排序可完成（Kahn 算法），无环路 | ✅ | 保存/发布 |
| **MVP 线性约束** | 每个节点入度 ≤ 1，出度 ≤ 1（单链，不支持分支/并行） | ✅ | 保存/发布 |
| **禁止重复边** | 同一 source-target 对只允许一条边 | ✅ | 保存/发布 |

> V1 放宽：去掉「入度 ≤ 1 / 出度 ≤ 1」约束后，DAG 天然支持扇出（并行分支）和扇入（聚合），数据结构无需任何改动。

#### 4.3.2 DAG 拓扑排序与执行模型

```mermaid
graph TD
    subgraph Kahn["拓扑排序（Kahn 算法）"]
        direction TB
        S1["① 计算每个节点的入度"]
        S2["② 入度为 0 的节点入队<br/>（必定是 trigger）"]
        S3["③ 依次出队执行<br/>将其所有后继节点的入度减 1"]
        S4["④ 入度变为 0 的后继节点入队"]
        S5["⑤ 直到队列为空"]
        S1 --> S2 --> S3 --> S4 --> S5
    end
```

```mermaid
flowchart LR
    subgraph MVP["MVP 线性 DAG"]
        direction LR
        T["trigger<br/>入度: 0"] --> C["connector<br/>入度: 1"] --> D["data_processor<br/>入度: 1"] --> E["exit<br/>入度: 1"]
    end
    MVP_Result["排序结果: [trigger, connector, data_processor, exit]"]
    MVP --> MVP_Result

    subgraph V1["V1 并行 DAG（相同数据结构，不同执行策略）"]
        direction LR
        T1["trigger<br/>入度: 0"] --> A["connector:A<br/>入度: 1"] & B["connector:B<br/>入度: 1"]
        A & B --> EX["exit<br/>入度: 2"]
    end
    V1_Result["排序结果: [trigger] → [connector:A, connector:B]（同层并行）→ [exit]"]
    V1 --> V1_Result
```

```mermaid
flowchart LR
    subgraph WebFlux["WebFlux 并行执行"]
        WF["Mono.when(<br/>  executeNode('connector:A'),<br/>  executeNode('connector:B')<br/>).flatMap(results -> executeNode('exit'))"]
    end
```

**示例**（MVP 线性 DAG）：

```json
{
  "nodes": [
    {
      "id": "node_trigger",
      "type": "trigger",
      "labelCn": "接收请求",
      "labelEn": "Receive Request",
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
      "rateLimit": { "maxQps": 100 },
      "position": { "x": 100.0, "y": 200.0 }
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
      "position": { "x": 350.0, "y": 200.0 }
    },
    {
      "id": "node_2",
      "type": "data_processor",
      "labelCn": "格式化消息",
      "labelEn": "Format Message",
      "config": {
        "fieldMappings": [
          { "source": "${node_1.msgId}", "target": "result.id" },
          { "source": "constant:success", "target": "result.status" }
        ]
      },
      "position": { "x": 600.0, "y": 200.0 }
    },
    {
      "id": "node_exit",
      "type": "exit",
      "labelCn": "返回结果",
      "labelEn": "Return Result",
      "outputFields": ["result.id", "result.status"],
      "position": { "x": 850.0, "y": 200.0 }
    }
  ],
  "edges": [
    { "id": "e1", "sourceNodeId": "node_trigger", "targetNodeId": "node_1", "type": "default", "label": "触发" },
    { "id": "e2", "sourceNodeId": "node_1",       "targetNodeId": "node_2", "type": "default", "label": "发送完成" },
    { "id": "e3", "sourceNodeId": "node_2",       "targetNodeId": "node_exit", "type": "default", "label": "格式化完成" }
  ]
}
```

---

### 4.4 执行数据 — executionRecord / executionStep

> 执行数据的结构由对应节点的 inputSchema / outputSchema 动态决定，不在数据库层约束。errorInfo 统一使用结构化格式。

**errorInfo Schema 定义**（同 §3 definitions）：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "urn:openapp:schema:errorDetail:v1",
  "title": "errorDetail",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "code": { "type": "string", "description": "错误码" },
    "message": { "type": "string", "description": "错误描述" },
    "cause": {
      "type": "string",
      "description": "根因描述，非下游错误时使用（如 'JSON 解析失败：unexpected token at line 3'、'字段映射失败：source 字段 ${node_1.msgId} 不存在'）"
    },
    "downstreamStatus": { "type": "integer", "description": "下游 HTTP 状态码（下游调用失败时）" },
    "downstreamBody": { "type": "string", "description": "下游响应体片段（截断到 512 字符）" }
  },
  "required": ["code", "message"],
  "oneOf": [
    { "required": ["cause"], "description": "内部错误" },
    { "required": ["downstreamStatus"], "description": "下游错误" }
  ]
}
```

> 💡 `oneOf` 约束确保每种错误场景都有对应的详细字段：内部错误（如 JSON 解析失败）携带 `cause`，下游调用失败携带 `downstreamStatus`。

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

// errorInfo — 下游调用失败
{
  "code": "DOWNSTREAM_UNAVAILABLE",
  "message": "HTTP 503 服务不可用",
  "downstreamStatus": 503,
  "downstreamBody": "Service Unavailable"
}

// errorInfo — 内部错误（🆕 v3.0 新增 cause 字段）
{
  "code": "FIELD_MAPPING_FAILED",
  "message": "字段映射失败",
  "cause": "source 字段 ${node_1.msgId} 在上游节点输出中不存在"
}
```

---

## 5. DAG（Directed Acyclic Graph）编排演进路线图

```mermaid
flowchart LR
    subgraph MVP["MVP (v1.0)"]
        direction TB
        M1["拓扑: 线性链"]
        M2["edge.type: [default]"]
        M3["节点类型: trigger, connector, data_processor, exit"]
        M4["执行: for 循环串行"]
        M5["数据流: 全量透传"]
    end

    subgraph V1["V1 (v1.1)"]
        direction TB
        V1_1["拓扑: DAG（分支+并行）"]
        V1_2["edge.type: [+condition, error]"]
        V1_3["节点类型: +router, aggregator, error_handler"]
        V1_4["执行: 拓扑层级并行<br/>WebFlux Mono.when()"]
        V1_5["数据流: 条件路由+聚合"]
    end

    subgraph V2["V2+"]
        direction TB
        V2_1["拓扑: DAG（循环+子流）"]
        V2_2["edge.type: [+error, always]"]
        V2_3["节点类型: +iterator, sub_flow, parallel"]
        V2_4["执行: 事件驱动+背压<br/>Reactor groupBy"]
        V2_5["数据流: 流式+窗口"]
    end

    MVP --> V1 --> V2

    Foundation["数据结构不变: nodes[] + edges[]<br/>— 只增字段，不删不改，向后兼容"]
    MVP -.-> Foundation
    V1 -.-> Foundation
    V2 -.-> Foundation
```

关键设计决策：

| 决策 | 选择 | 理由 |
|------|------|------|
| **边语义化** | MVP 就在 edge 中加入 `type`/`label`（限定 default） | 避免 V1 做数据迁移；MVP 前端可选择性渲染 label |
| **data_processor 定位** | 纯管道节点，不改 DAG 拓扑 | V1 引入独立 `router` 节点处理分支，职责清晰 |
| **DAG 拓扑约束** | 应用层校验，JSON Schema 不覆盖 | 跨节点引用校验需图遍历，非声明式 Schema 可表达 |
| **线性约束** | MVP 限制入度≤1 出度≤1 | 降低执行引擎复杂度，V1 只需去掉此限制即可支持并行 |

---

## 6. 版本演进规则

| 场景 | 处理方式 |
|------|---------|
| **新增可选字段** | 直接加，不影响已有数据 |
| **新增必填字段** | 发新版本，旧数据迁移赋默认值 |
| **字段改名** | ❌ 不允许，废弃旧字段 + 新增新字段 |
| **字段废弃** | 保留字段名，标注 `deprecated: true` + `x_replacedBy: "newField"` |
| **枚举值新增** | 直接加，应用层做好未知值降级 |
| **枚举值删除** | ❌ 不允许，标记为 deprecated |
| **edge.type 扩展** | MVP 限定 `default`；V1 新增 `condition`/`error`/`always` 时直接加枚举值 |
| **节点 type 扩展** | V1 新增 `router`/`aggregator` 等时直接加枚举值，旧编排不受影响 |

> **向后兼容**：加不加删、改不删。可以加新字段、新枚举值，不可以删已有字段、改已有字段名。

---

## 7. React Flow 标准数据格式

> 📌 **本章目标**：记录 React Flow（@xyflow/react v12）框架本身的**标准 Node / Edge 数据接口**。本章仅涉及框架公开的类型定义，不涉及本项目的业务设计、不讨论与后端 Schema 的差异。这些接口是我们在 `flow_version_t.orchestration_config` 中存储 DAG 编排数据的**格式约束基础**。

### 7.1 React Flow 标准 Node 与 Edge 接口

React Flow（@xyflow/react v12）是连接流编排画布的前端技术选型。本节记录框架公开的 TypeScript 类型定义——这些字段是 React Flow 画布产物的**固定结构**。

> 📎 **官方来源**：
> - **React Flow 类型系统总览**：https://reactflow.dev/api-reference/types
> - **Node 类型定义**：https://reactflow.dev/api-reference/types/node
> - **Edge 类型定义**：https://reactflow.dev/api-reference/types/edge
> - **GitHub 源码（NodeBase）**：https://github.com/xyflow/xyflow/blob/main/packages/system/src/types/nodes.ts
> - **GitHub 源码（Edge）**：https://github.com/xyflow/xyflow/blob/main/packages/react/src/types/edges.ts
> - **API 参考入口**：https://reactflow.dev/api-reference

#### 7.1.1 Node 接口

```typescript
// React Flow 标准 Node 接口（简化，仅列出与存储相关的字段）
interface Node<TData = Record<string, unknown>> {
  id: string;                    // 节点唯一标识，React Flow 内部使用
  type: string;                  // 映射到注册的 React 组件名（详见 §7.2 语义边界）
  position: {                    // 画布坐标（浮点数）
    x: number;
    y: number;
  };
  data: TData;                   // ⚠️ 所有自定义业务数据必须嵌套在此字段内
  // 以下为 React Flow 内部运行时字段，不应持久化：
  // selected?: boolean;         // 是否被选中
  // dragging?: boolean;         // 是否正在拖拽
  // measured?: { width, height }; // 渲染后的尺寸
  // width?, height?;            // 节点尺寸
  // draggable?, selectable?, connectable?; // 交互控制
  // style?, className?;         // 样式
  // sourcePosition?, targetPosition?; // Handle 位置
  // zIndex?;                    // 层级
}
```
> 📎 来源：https://reactflow.dev/api-reference/types/node

**关键约束**：
- `data` 是用户自定义数据的唯一入口。React Flow 不会触碰 `data` 的内容，但**所有非框架字段必须放在 `data` 内**。
- `type` 决定了 React Flow 使用哪个 React 组件来渲染该节点（详见 §7.2）。
- `id`、`type`、`position` 是框架级字段，不可放入 `data`。
- React Flow **没有**任何内置的业务节点类型——`trigger`、`connector`、`exit` 等并非框架的一部分（见 §7.2）。

#### 7.1.2 Edge 接口

```typescript
// React Flow 标准 Edge 接口（简化）
interface Edge<TData = Record<string, unknown>> {
  id: string;                    // 边唯一标识
  source: string;                // ⚠️ 源节点 ID（固定字段名 source）
  target: string;                // ⚠️ 目标节点 ID（固定字段名 target）
  type?: string;                 // 边渲染样式（'default', 'smoothstep', 'straight' 等）
  // 以下为可选/运行时字段，不应持久化：
  // sourceHandle?, targetHandle?; // Handle 锚点 ID
  // animated?: boolean;
  // style?, className?;
  // label?, labelStyle?, labelBgStyle?;
  // markerStart?, markerEnd?;
  // selected?, deletable?, focusable?;
  // data?: TData;               // V12 支持 edge.data，可存放业务扩展数据
}
```
> 📎 来源：https://reactflow.dev/api-reference/types/edge

**关键约束**：
- 连接引用字段名固定为 `source` 和 `target`——React Flow 不提供 `sourceNodeId` 或 `targetNodeId` 变体。
- `edge.type` 在 React Flow 中控制**渲染样式**（如贝塞尔曲线 vs 折线），与业务语义（执行条件、错误路由等）无关。
- `edge.data`（V12 新增）是存放边级别业务扩展数据的位置——例如条件表达式、错误处理策略等。

---

### 7.2 React Flow `type` 字段的语义

#### 7.2.1 `node.type` 在框架中的真实角色

在 React Flow 中，`node.type` **仅是一个任意字符串**。React Flow 本身**只提供一个内置类型**，其余全部由使用方自行注册。框架的行为如下：

| 场景 | React Flow 行为 |
|------|----------------|
| `type: 'default'` | React Flow **唯一内置类型**：一个带有选中/拖拽交互的灰色方框 |
| `type: 'some_custom_type'` | 在 `nodeTypes` 注册表中查找名为 `'some_custom_type'` 的 React 组件 → 渲染之；若未注册，fallback 到 `'default'` |

```typescript
// 泛化示例：任何项目注册自定义节点类型的方式
// React Flow 仅内置 'default' 类型；其余类型需自行注册
import { ReactFlow } from '@xyflow/react';
import CustomNodeA from './CustomNodeA';
import CustomNodeB from './CustomNodeB';

const nodeTypes = {
  custom_a: CustomNodeA,
  custom_b: CustomNodeB,
};

// <ReactFlow nodes={nodes} edges={edges} nodeTypes={nodeTypes} />
```

> ⚠️ React Flow **不定义**任何业务语义的节点类型。`'trigger'`、`'connector'`、`'exit'` 等字符串不是 React Flow 的一部分——它们是使用方自行注册的自定义类型。框架只负责根据 `node.type` 字符串在 `nodeTypes` 映射表中查找对应的 React 组件并渲染。

#### 7.2.2 完整的 React Flow 画布产物示例

以下展示一个 React Flow 画布在运行时实际持有的 JavaScript 对象结构——这是 `JSON.stringify(nodes) + JSON.stringify(edges)` 得到的完整数据。此示例**涵盖 React Flow 全部四种内置节点类型**：`input`（入口，仅有出边 Handle）、`default`（中间节点，有入出双边 Handle）、`output`（出口，仅有入边 Handle）、`group`（分组容器）。不涉及任何项目的自定义节点类型。

```json
{
  "nodes": [
    {
      "id": "1",
      "type": "input",
      "position": { "x": 0, "y": 0 },
      "data": { "label": "入口节点" },
      "width": 150,
      "height": 40,
      "selected": false,
      "dragging": false,
      "sourcePosition": "right",
      "targetPosition": "left"
    },
    {
      "id": "2",
      "type": "default",
      "position": { "x": 250, "y": 0 },
      "data": { "label": "中间节点" },
      "width": 150,
      "height": 40,
      "selected": true,
      "dragging": false,
      "sourcePosition": "right",
      "targetPosition": "left"
    },
    {
      "id": "3",
      "type": "output",
      "position": { "x": 500, "y": 0 },
      "data": { "label": "出口节点" },
      "width": 150,
      "height": 40,
      "selected": false,
      "dragging": false,
      "sourcePosition": "right",
      "targetPosition": "left"
    },
    {
      "id": "4",
      "type": "group",
      "position": { "x": 100, "y": 150 },
      "data": { "label": "分组容器" },
      "width": 400,
      "height": 200,
      "selected": false,
      "dragging": false,
      "style": { "backgroundColor": "rgba(240,240,240,0.5)" }
    }
  ],
  "edges": [
    {
      "id": "e1-2",
      "source": "1",
      "target": "2",
      "sourceHandle": null,
      "targetHandle": null,
      "animated": false,
      "selected": false
    },
    {
      "id": "e2-3",
      "source": "2",
      "target": "3",
      "sourceHandle": null,
      "targetHandle": null,
      "animated": false,
      "selected": false
    }
  ]
}
```

**React Flow 内置节点类型**：

React Flow 自带四种内置节点类型，均基于 `DefaultNode` 组件渲染，区别在于 Handle（连接点）的预设位置不同：

| 内置类型 | 源 Handle（出边） | 目标 Handle（入边） | 典型用途 |
|:--------:|:---:|:---:|------|
| `input` | ✅ 有（默认 right） | ❌ 无 | 流起点——只能作为边的 `source`，不可作为 `target` |
| `default` | ✅ 有（默认 right） | ✅ 有（默认 left） | 流中间节点——既可接收上游也可连接下游 |
| `output` | ❌ 无 | ✅ 有（默认 left） | 流终点——只能作为边的 `target`，不可作为 `source` |
| `group` | 无 Handle | 无 Handle | 分组容器——可包含子节点，用于画布布局组织 |

> ⚠️ 上述四种是 React Flow **框架本身提供**的节点类型，直接可用无需注册。任何其他字符串（如 `'trigger'`、`'connector'`）均为**使用方通过 `nodeTypes` 自行注册**的自定义类型，不属于 React Flow 框架的一部分。

**示例字段说明**：

| 字段 | 所属 | 类型 | 说明 |
|------|------|------|------|
| `id` | node | `string` | 节点唯一标识，React Flow 内部用此 ID 关联边的 source/target |
| `type` | node | `string` | 映射到 `nodeTypes` 注册的组件；`input`/`default`/`output`/`group` 为 React Flow 内置，无需注册 |
| `position` | node | `{ x: number, y: number }` | 画布坐标，原点为左上角 |
| `data` | node | `object` | **用户自定义数据的唯一容器**；`label` 是内置节点用来显示文字的约定键 |
| `width`, `height` | node | `number` | 节点渲染后的像素尺寸（运行时计算，非用户指定） |
| `selected` | node | `boolean` | 是否被用户框选或点击选中（运行时状态） |
| `dragging` | node | `boolean` | 是否正在被拖拽（运行时状态） |
| `sourcePosition` | node | `string` | 出边 Handle 的位置（`'right'` / `'left'` / `'top'` / `'bottom'`） |
| `targetPosition` | node | `string` | 入边 Handle 的位置 |
| `source` | edge | `string` | 源节点的 `id`——React Flow 固定字段名 |
| `target` | edge | `string` | 目标节点的 `id`——React Flow 固定字段名 |
| `sourceHandle`, `targetHandle` | edge | `string \| null` | Handle 锚点 ID，`null` 表示自动连接最近的可连接 Handle |
| `animated` | edge | `boolean` | 是否显示流动动画（运行时状态） |
| `id` | edge | `string` | 边唯一标识 |

> 💡 **持久化提示**：上述字段中，仅 `id`、`type`、`position`、`data`（节点）和 `id`、`source`、`target`（边）是存储时需要保留的。`selected`、`dragging`、`width`、`height`、`animated`、`sourcePosition`、`targetPosition`、`sourceHandle`、`targetHandle` 等运行时/渲染状态字段**不应持久化**到数据库（详见 §8.5）。

> 📎 此示例结构可通过 React Flow 官方文档中的任何可运行示例验证——打开浏览器 DevTools，查看 React Flow 组件内部 state 即可确认 `nodes` / `edges` 数组的完整字段集合。来源：https://reactflow.dev/api-reference/types/node / https://reactflow.dev/api-reference/types/edge

此示例说明了 React Flow 数据格式的核心要点：
1. **顶层结构**：`nodes` 数组 + `edges` 数组，这是 React Flow 的标准数据模型
2. **四种内置类型**：`input`（有出无入）、`default`（出入双全）、`output`（有入无出）、`group`（无 Handle 容器），直接可用无需注册
3. **`node.data` 容器**：所有自定义业务数据必须嵌套在 `data` 内；框架字段（`id`、`type`、`position`）与业务字段在结构上隔离
4. **运行时字段混合**：`selected`、`dragging`、`width` 等运行时状态字段与存储字段在同一个对象中共存——持久化时必须过滤

---

## 8. 后端 Schema 与 React Flow 格式的差异分析及存储决策

> 📌 **本章目标**：将 §7 中确立的 React Flow 标准格式与本项目 §4.3 中定义的 `orchestrationConfig` Schema 进行对比，分析差异、评估影响、给出存储格式决策。

### 8.1 与现有 Schema 的差异

下文将前文 §4.3 中定义的 orchestrationConfig Schema（以下简称「**现有 Schema**」）与 React Flow 标准格式逐一对比。

#### 8.1.1 Edge 字段名差异

| 维度 | React Flow 标准 | 现有 Schema (§4.3) | 差异 |
|------|:---:|:---:|------|
| 源节点引用 | `source` | `sourceNodeId` | 字段名不同 |
| 目标节点引用 | `target` | `targetNodeId` | 字段名不同 |

**影响**：前端从画布拿到的 `edge.source` 必须翻译为 `sourceNodeId` 才能存入后端，加载时再逆向翻译。这是纯粹的字段名映射，数据结构完全等价。

```diff
// React Flow 画布产物       vs       现有 Schema 存储格式
- { "id": "e1", "source": "node_a", "target": "node_b" }
+ { "id": "e1", "sourceNodeId": "node_a", "targetNodeId": "node_b" }
```

#### 8.1.2 Node 业务数据位置差异（⚠️ 结构性差异）

这是最关键的差异——不是字段名的不同，而是**数据嵌套层级**不同：

| 维度 | React Flow 标准 | 现有 Schema (§4.3) | 差异 |
|------|:---:|:---:|------|
| 框架字段 | `id`, `type`, `position` — 顶层 | `id`, `type`, `position` — 顶层 | ✅ 一致 |
| 业务字段 | **所有**在 `node.data` 内嵌套 | **平铺**在 node 顶层 | 🔴 结构不同 |

**可视化对比**：

```
React Flow 格式（前端直接产物）          现有 Schema（后端定义）
┌─────────────────────────┐            ┌─────────────────────────┐
│ node                     │            │ node                     │
│ ├── id: "node_1"        │            │ ├── id: "node_1"        │
│ ├── type: "connector"   │            │ ├── type: "connector"   │
│ ├── position: {x,y}     │            │ ├── position: {x,y}     │
│ └── data: {             │⚠️          │ ├── labelCn: "..."      │⚠️
│       ├── labelCn: "..." │  嵌套      │ ├── connectorVersionId  │  平铺
│       ├── connectorVersionId         │ ├── inputMapping        │
│       └── inputMapping   │            │ └── ...                 │
│     }                    │            │                         │
└─────────────────────────┘            └─────────────────────────┘
```

**影响**：
- 前端保存时：需要 `{ id, type, position, data: { labelCn, connectorVersionId, ... } }` → 展平为 `{ id, type, position, labelCn, connectorVersionId, ... }` 再发送
- 前端加载时：需要反向嵌套回 `data`
- 后端运行时（connector-api）：读取路径从 `node.connectorVersionId` 变为 `node.data.connectorVersionId`（或反之，取决于最终存储格式）

#### 8.1.3 Node type 的双重语义与前后端不一致

React Flow 的 `node.type` 同时承担两个职责（见 §7.2.2）：

| 职责 | React Flow 视角 | 业务视角 |
|------|:---:|:---:|
| **前端渲染** | 映射到 `nodeTypes` 注册表中对应的 React 组件 | 不关心 |
| **业务分类** | 不关心 | 决定运行时执行哪个 NodeExecutor |

**当前代码中的 type 值对比**：

| 现有前端代码（customNodes.jsx） | 后端 Schema (§4.3) | 是否一致 |
|------|------|:---:|
| `trigger` | `trigger` | ✅ |
| `action` | `connector` | 🔴 不一致 |
| `condition` / `delay` / `parallel` / `loop` | —（MVP 无） | — |
| — | `data_processor` | 🟡 前端尚未注册此类型 |
| — | `exit` | 🟡 前端尚未注册此类型 |

> ⚠️ **前端现有代码用 `action`，后端 Schema 用 `connector`**。需要统一。建议以规范对齐为准：后端 Schema 的 `connector` / `data_processor` / `exit` 是业务层面的精确分类，前端 `nodeTypes` 注册时应使用相同值。


#### 8.1.4 项目的语义过载（从 §7 迁移）

连接器平台对 `node.type` 字段施加了**双重语义**，这是有意为之的设计：

```
node.type = "connector"
           │
           ├──▶ React Flow 视角：用 ConnectorNode 组件渲染
           │
           └──▶ 运行时引擎视角：用 ConnectorNodeExecutor 执行
```

| 维度 | React Flow 框架 | 我们的项目 |
|------|:---:|:---:|
| `type: 'trigger'` | 框架无感知 | 业务含义：DAG 入口节点，声明触发条件和入参契约 |
| `type: 'connector'` | 框架无感知 | 业务含义：调用下游 API 的节点，引用 `connectorVersionId` |
| `type: 'data_processor'` | 框架无感知 | 业务含义：纯管道转换节点，不改 DAG 拓扑 |
| `type: 'exit'` | 框架无感知 | 业务含义：DAG 出口节点，声明返回值字段 |
| `type` 值从何而来 | 用户拖拽时前端代码指定 | 与 `customNodes.jsx` 注册的键名一致 |

#### 8.1.5 关键结论

1. **React Flow 标准格式中不存在 `trigger` / `connector` / `data_processor` / `exit` 这些类型**。它们是**本项目通过 `nodeTypes` 注册表自行注册**的自定义类型（见 §7.2.1）。
2. 这些字符串值的选择是**项目的命名约定**，不影响 React Flow 的功能——任何字符串都可以，只要与注册表中的键名一致。
3. 存储格式中 `type` 字段的值 = 注册表中的键名 = 运行时的业务分类。三者必须一致。
4. 当前前后端在这个值上的不一致（前端 `action` vs 后端 `connector`）是**命名约定问题**，不是格式问题，详见上表。

---

### 8.2 场景分析

以下分析在不同技术场景中，应使用哪种格式、在何处做转换。

#### 场景一：前端保存 → 后端存储 → 前端加载（理想路径）

```
┌──────────────┐      PUT /flows/{id}/config       ┌──────────────┐
│  wecodesite  │ ─────────────────────────────────▶ │  open-server  │
│  React Flow  │     orchestrationConfig JSON        │  flow_version_t
│  nodes/edges │                                     │  .orchestration_config
│  (原生格式)  │ ◀───────────────────────────────── │  (MEDIUMTEXT) │
└──────────────┘      GET /flows/{id}/config         └──────────────┘
```

**推荐策略**：后端存储 React Flow 原生格式。

- ✅ 前端零翻译层：`JSON.stringify(reactFlowNodes)` 直接作为请求体，加载时直接 `setNodes(response.orchestrationConfig.nodes)` 渲染
- ✅ 减少 bug 面：不需要维护双向的序列化/反序列化适配器
- ✅ 调试友好：数据库中存的 JSON 与浏览器 DevTools 中看到的完全一致
- ✅ React Flow 版本升级：新版本如果增加框架字段（如 `node.data` 下新增字段），不会与业务字段冲突

**代价**：
- 后端 JSON Schema 校验需要将业务字段放在 `properties.data.properties` 下
- connector-api 运行时读取路径多一层 `node.data.xxx`

**如果继续使用现有 Schema（业务字段平铺）**，则需要在前端 thunk.js 中加入转换层：

```javascript
// 保存时：React Flow → 现有 Schema
const toBackendFormat = (nodes) => nodes.map(n => ({
  id: n.id,
  type: n.type,
  position: n.position,
  ...n.data           // ← 展平 data 到顶层
}));

// 加载时：现有 Schema → React Flow
const toReactFlowFormat = (nodes) => nodes.map(n => ({
  id: n.id,
  type: n.type,
  position: n.position,
  data: {             // ← 收集业务字段回 data
    labelCn: n.labelCn,
    labelEn: n.labelEn,
    authTypeSchema: n.authTypeSchema,
    // ... 需要显式列出所有业务字段
  }
}));
```

> ⚠️ 转换层的风险：增删业务字段时需要同步维护 `toBackendFormat` 和 `toReactFlowFormat` 两个函数，容易产生「改了 Schema 但忘了更新转换函数」的 bug。

#### 场景二：后端运行时（connector-api）读取执行

```
┌────────────────┐     R2DBC 读取      ┌────────────────────┐
│ connector-api  │ ◀────────────────── │ flow_version_t      │
│ runtime 引擎   │  orchestration_config│ .orchestration_config│
│                │  JSON → Jackson POJO │ (MEDIUMTEXT)        │
└────────────────┘                     └────────────────────┘
```

运行时引擎需要从节点配置中提取关键信息来执行连接流。如果存储 React Flow 格式，读取路径为：

| 运行时需要的信息 | 存储格式 (React Flow) | 读取路径 |
|------|------|------|
| 节点 ID | `node.id` | `node.getId()` |
| 节点业务类型 | `node.type`（注意：React Flow 标准中 type 即为业务分类，见 §7.2.2） | `node.getType()` |
| 引用的连接器版本 | `node.data.connectorVersionId` | `node.getData().getConnectorVersionId()` |
| 字段映射配置 | `node.data.inputMapping` | `node.getData().getInputMapping()` |
| DAG 拓扑关系 | `edge.source` → `edge.target` | 拓扑排序的输入 |

> 💡 运行时引擎的读取开销增加极小（仅多一层 `getData()` 调用），Jackson 反序列化到强类型 POJO 后，IDE 自动补全和编译期检查仍然有效。

#### 场景三：API 响应给前端

前端期望的是 React Flow 原生格式——可以直接 `setNodes()` 渲染。如果后端存储的就是 React Flow 格式，API 响应**直接透传**即可，无需任何转换。

---

### 8.3 建议的存储格式

基于以上分析，建议 `flow_version_t.orchestration_config` 存储 **React Flow 原生格式**，即业务字段全部嵌套在 `node.data` 下，边使用 `source` / `target`。

> 💡 **关于 `type` 值**：以下示例中的 `"type": "trigger"`、`"type": "connector"`、`"type": "data_processor"`、`"type": "exit"` 是**本项目的自定义注册类型**（见 §7.2），不是 React Flow 内置类型。React Flow 框架本身不定义这些字符串——它们只在我们的 `nodeTypes` 注册表中有意义。存储格式使用这些值，是因为它们同时兼顾了 React Flow 渲染映射和后端运行时业务分类的双重职责。

```json
{
  "nodes": [
    {
      "id": "node_a1b2c3",
      "type": "trigger",
      "position": { "x": 100.0, "y": 200.0 },
      "data": {
        "labelCn": "接收请求",
        "labelEn": "Receive Request",
        "authTypeSchema": {
          "type": "SYSTOKEN",
          "fields": [{ "name": "token", "carrier": "header", "fieldName": "X-Sys-Token", "required": true }]
        },
        "inputSchema": {
          "type": "object",
          "properties": {
            "sender": { "type": "string", "description": "发送者 ID" },
            "content": { "type": "string", "description": "消息内容" }
          },
          "required": ["sender", "content"]
        },
        "rateLimit": { "maxQps": 100 }
      }
    },
    {
      "id": "node_d4e5f6",
      "type": "connector",
      "position": { "x": 350.0, "y": 200.0 },
      "data": {
        "labelCn": "发送通知",
        "labelEn": "Send Notification",
        "connectorVersionId": "9876543210123456789",
        "inputMapping": {
          "receiver": "${trigger.sender}",
          "content": "${trigger.content}"
        }
      }
    },
    {
      "id": "node_g7h8i9",
      "type": "data_processor",
      "position": { "x": 600.0, "y": 200.0 },
      "data": {
        "labelCn": "格式化消息",
        "labelEn": "Format Message",
        "config": {
          "fieldMappings": [
            { "source": "${node_d4e5f6.msgId}", "target": "result.id" },
            { "source": "constant:success", "target": "result.status" }
          ]
        }
      }
    },
    {
      "id": "node_exit",
      "type": "exit",
      "position": { "x": 850.0, "y": 200.0 },
      "data": {
        "labelCn": "返回结果",
        "labelEn": "Return Result",
        "outputFields": ["result.id", "result.status"]
      }
    }
  ],
  "edges": [
    { "id": "e1", "source": "node_a1b2c3", "target": "node_d4e5f6", "type": "default", "label": "触发" },
    { "id": "e2", "source": "node_d4e5f6", "target": "node_g7h8i9", "type": "default", "label": "发送完成" },
    { "id": "e3", "source": "node_g7h8i9", "target": "node_exit",  "type": "default", "label": "格式化完成" }
  ]
}
```

---

### 8.4 字段映射对照表

以下表格完整列出 React Flow 格式与现有 Schema 的字段对应关系，便于未来统一 Schema 时参考。

#### 8.4.1 Node 顶层字段

| React Flow 字段 | 现有 Schema 字段 | 说明 | 变更方向 |
|------|------|------|:---:|
| `id` | `id` | 节点唯一 ID | ✅ 无变更 |
| `type` | `type` | 节点类型（自定义注册值，见 §7.2） | ⚠️ 值需统一（见 §8.1.3） |
| `position` | `position` | 画布坐标 | ✅ 无变更 |
| `data` | —（业务字段平铺在顶层） | 业务数据容器 | 🔴 新增嵌套层 |

#### 8.4.2 Node.data 内业务字段（按节点类型）

| 节点类型 | React Flow 路径 | 现有 Schema 路径 | 说明 |
|------|------|------|------|
| trigger | `node.data.labelCn` | `node.labelCn` | 中文标签 |
| trigger | `node.data.labelEn` | `node.labelEn` | 英文标签 |
| trigger | `node.data.authTypeSchema` | `node.authTypeSchema` | 认证类型声明 |
| trigger | `node.data.inputSchema` | `node.inputSchema` | 入参 Schema |
| trigger | `node.data.rateLimit` | `node.rateLimit` | 限流配置 |
| connector | `node.data.connectorVersionId` | `node.connectorVersionId` | 引用连接器版本 |
| connector | `node.data.inputMapping` | `node.inputMapping` | 字段映射 |
| data_processor | `node.data.config.fieldMappings` | `node.config.fieldMappings` | 管道转换配置 |
| exit | `node.data.outputFields` | `node.outputFields` | 返回值字段列表 |

#### 8.4.3 Edge 字段

| React Flow 字段 | 现有 Schema 字段 | 说明 | 变更方向 |
|------|------|------|:---:|
| `id` | `id` | 边唯一 ID | ✅ 无变更 |
| `source` | `sourceNodeId` | 源节点 ID | 🔴 字段名改名 |
| `target` | `targetNodeId` | 目标节点 ID | 🔴 字段名改名 |
| `type` | `type` | 边业务类型（default/condition/error） | ✅ 无变更 |
| `label` | `label` | 边显示标签 | ✅ 无变更 |
| `data` | —（不存在） | 边扩展数据（V1 预留）🆕 | 🟢 V12 新能力 |

---

### 8.5 持久化时的字段过滤

React Flow 的 `nodes` 和 `edges` 数组中包含大量运行时状态字段（如 `selected`、`dragging`、`measured`、`style` 等），这些字段**不应持久化**到数据库。

**处理策略**：
- **方案 A（推荐）**：后端接收时只校验已知业务字段，通过 `additionalProperties: false` 在 JSON Schema 层面拒绝未知字段。前端传多余字段会触发 422 校验失败。
- **方案 B**：前端保存前做 `pick` 过滤，只保留白名单字段。风险是白名单维护成本高。
- **方案 C**：前端不做过滤，后端做 `@JsonIgnore` 忽略未知字段。存在存储膨胀风险，但开发体验最好。

> **MVP 建议**：方案 A（JSON Schema 严格校验），既保证存储整洁，又在保存失败时给前端明确的错误提示，促使前端在 thunk 中做 `pick`。

**React Flow 运行时字段清单**（不应持久化）：

| 字段 | 所属 | 说明 |
|------|------|------|
| `selected` | node | 是否被选中 |
| `dragging` | node | 是否正在拖拽 |
| `measured` | node | 渲染后的尺寸对象 |
| `width`, `height` | node | 节点尺寸（V12 可能是 measured 的子字段） |
| `draggable`, `selectable`, `connectable` | node | 交互控制 |
| `style`, `className` | node | 样式 |
| `sourcePosition`, `targetPosition` | node | Handle 位置 |
| `zIndex` | node | 层级 |
| `animated` | edge | 动画状态 |
| `style` | edge | 样式 |
| `markerStart`, `markerEnd` | edge | 箭头样式 |
| `selected` | edge | 是否被选中 |
| `deletable`, `focusable` | edge | 交互控制 |
| `sourceHandle`, `targetHandle` | edge | Handle 锚点 ID |

---

### 8.6 各模块变更影响评估

如果采用「React Flow 原生格式存储」，各模块的变更范围：

| 模块 | 影响 | 工作量 | 说明 |
|------|------|:---:|------|
| **plan-json-schema.md** | 需重写 §4.3 的 `orchestrationConfig` Schema | 中 | 业务字段迁入 `node.data`；edges 字段改为 `source`/`target` |
| **wecodesite FlowEditor** | **几乎零变更** | ✅ 零 | 画布产物直接就是目标格式；如果前端代码中有做过 `toBackendFormat` 转换应删除 |
| **wecodesite mock.js** | 示例数据微调 | 轻 | 当前 mock 数据已经是 React Flow 格式（`data` 嵌套 + `source`/`target`），仅需对齐 `type` 枚举值和 `data` 内的字段名 |
| **plan-api.md** | 请求/响应示例更新 | 轻 | GET/PUT config 接口的 JSON 示例需对齐 |
| **open-server** | JSON Schema 校验规则更新 | 轻 | 校验路径从 `node.labelCn` 变为 `node.data.labelCn` |
| **connector-api runtime** | 读取路径加一层 `data` | 轻 | `node.getConnectorVersionId()` → `node.getData().getConnectorVersionId()`；拓扑排序的 `edge.source` 字段名简化 |
| **plan-db.md** | 示例 JSON 更新 | 轻 | `orchestration_config` 注释示例对齐 |
| **plan-page.md** | 无影响 | 无 | 页面设计不涉及存储格式细节 |

**总评估**：前端零改动，后端轻量调整（JSON Schema 校验 + 运行时读取路径）。最大收益是消除了维护双向转换层的长期成本。

---

### 8.7 决策记录

| 决策项 | 选择 | 理由 |
|-------|------|------|
| 存储格式 | **React Flow 原生格式**（`node.data` 嵌套 + `edge.source`/`target`） | DAG 的编辑器就是 React Flow，其格式是事实标准；前端零翻译层，消除维护成本 |
| 字段过滤 | JSON Schema `additionalProperties: false`（方案 A） | 编译期保证 + 明确错误提示 |
| Node type 统一 | 以后端 Schema 为准：`trigger` / `connector` / `data_processor` / `exit` | 后端的分类更贴近业务语义；前端 `nodeTypes` 注册时对齐即可。注意：这些是项目的自定义注册类型，非 React Flow 内置 |
| 现有 Schema 处理 | 本章为差异分析文档，**不修改** §4.3 现有 Schema | 待团队对齐后，统一发出 v4.0 版本正式重写 Schema |



## 附录 A：修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-05-22 | 初始版本 | SDDU Plan Agent |
| v2.0 | 2026-05-22 | 重写为标准 JSON Schema 格式 + 示例分离 + 修复跨引用占位符 | SDDU Plan Agent |
| **v3.0** | 2026-05-22 | **审查修复**：① 新增 §3 definitions 聚合段修复 `$ref` 悬空（P1）；② 新增 §2.1 JSON 内嵌枚举字符串例外说明（P2）；③ 修复 `outputSchema` 引用指向 dataSchema（P3）；④ 标注 trigger⇄entry 命名映射（P4）；⑤ 移除示例中 `$schemaName` 字段（P5）；⑥ 标注 lifecycleStatus 枚举字典以 plan-db.md 为准（P6）；⑦ `fieldMappings` 增加 `required` + `pattern` 表达式校验（P7）；⑧ `position.x/y` 从 `integer` 改为 `number` 兼容 React Flow 浮点坐标（P8）；⑨ 所有 Schema 增加 `additionalProperties: false`（P9）；⑩ `errorInfo` 新增 `cause` 字段 + `oneOf` 约束（P10）；⑪ `rateLimit` 增加 `maximum` 约束（P11）；⑫ `triggerConfig` 增加 `allOf` + `if`-`then` 按触发类型条件必填（P12）；⑬ `connectorVersionId` 增加 `pattern` 雪花 ID 格式校验（P13）；⑭ 新增 §4.3.1 DAG 拓扑约束文档（P14）；⑮ **DAG 设计优化**：edge 增加 `type`/`label` 语义字段 + §4.3 编排设计思路说明 + §5 DAG 演进路线图 | SDDU Plan Agent |
| **v3.1** | 2026-05-25 | **React Flow 格式对齐**：新增 §7 React Flow 格式对齐指南——记录框架标准 Node/Edge 接口（含官方来源链接），分析三大差异（edge 字段名、node.data 嵌套、type 枚举值），给出场景分析和存储格式建议，附字段映射对照表 + 持久化过滤策略 + 各模块变更影响评估 + 决策记录 | SDDU Plan Agent |
| **v3.2** | 2026-05-25 | **章节重构**：§7 拆为纯框架标准（§7.1~§7.2，仅记录 React Flow Node/Edge 接口与 type 字段语义边界）；新增 §8 承载所有差异分析、场景评估、存储格式建议、变更影响、决策记录。明确 `trigger`/`connector`/`data_processor`/`exit` 为项目自定义注册类型而非 React Flow 内置 | SDDU Plan Agent |
| **v3.3** | 2026-05-25 | **§7 纯净化**：§7.2.1 替换为泛化示例（去除 customNodes.jsx）；§7.2.2 新增纯 React Flow JSON 示例（仅用 `default` 类型 + 完整字段表 + 持久化提示）；原 §7.2.2 语义过载 + §7.2.3 关键结论迁移至 §8.1.4~§8.1.5 | SDDU Plan Agent |
| **v3.4** | 2026-05-25 | **§7.2.2 示例丰富**：JSON 示例从单一 `default` 类型扩展为覆盖全部四种内置类型（`input`/`default`/`output`/`group`）；新增「React Flow 内置节点类型」对照表（Handle 预设位置 + 典型用途）；持久化提示字段清单补全 | SDDU Plan Agent |

## 附录 B：v3.0 审查问题修复对照表

| # | 严重度 | 问题 | 修复位置 |
|---|:---:|------|---------|
| P1 | 🔴 | `$ref` 悬空 — definitions 不存在 | §3 新增 definitions 聚合段 |
| P2 | 🔴 | authTypeSchema 枚举类型 vs DB TINYINT 冲突 | §2.1 新增例外说明 + 枚举对照表 |
| P3 | 🔴 | `outputSchema` 引用目标不存在 | §3 definitions 中 `outputSchema` 指向 `dataSchema` |
| P4 | 🟡 | `trigger` vs `entry` 命名不统一 | §4.1 增加命名映射说明 |
| P5 | 🟡 | `$schemaName` 示例中有但 Schema 中无 | 移除示例中的 `$schemaName`，改为注释说明 |
| P6 | 🟡 | lifecycleStatus 枚举字典跨文档冲突 | 标注权威定义以 plan-db.md 为准 |
| P7 | 🟡 | fieldMappings 缺少 required 和格式约束 | 增加 `required` + `pattern` 表达式校验 |
| P8 | 🟡 | `position.x/y` 应为 number 非 integer | 改为 `number` + 新增 `position` definition |
| P9 | 🟢 | 缺少 `additionalProperties: false` | 全部 Schema 增加此约束 |
| P10 | 🟢 | errorInfo 缺少 cause/stackTrace | 新增 `cause` 字段 + `oneOf` 约束 |
| P11 | 🟢 | rateLimit 无最大值约束 | 增加 `maximum` |
| P12 | 🟢 | trigger.type 条件必填未约束 | 增加 `allOf` + `if`-`then` |
| P13 | 🟢 | connectorVersionId 无格式约束 | 增加 `pattern` 雪花 ID 正则 |
| P14 | 🟢 | DAG 约束仅靠注释说明 | 新增 §4.3.1 拓扑约束规则表 |