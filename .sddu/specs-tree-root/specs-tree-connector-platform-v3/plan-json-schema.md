# JSON Schema 设计规范：连接器平台 V3

**关联文档**: plan.md, plan-db.md (§3 表结构定义), plan-api.md (§3 接口详细定义)  
**版本**: v9.11
**创建日期**: 2026-05-22  
**最后更新**: 2026-06-25
**修订说明**: v9.11 — 抽取 connectorVersionConfigDef（连接器配置唯一定义源），§5.2 改 $ref 消除重复；connectorNodeDataDef v2：新增 connectorId + connectorVersionConfig 快照

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
| JSON Schema (draft-07) | 核心 | `type` / `properties` / `required` / `description` / `definitions` / `oneOf` / `allOf` / `if`-`then` 等元字段直接复用 |
| OpenAPI 3.0 components/schemas | 结构 | 可复用组件（authConfig / rateLimitConfig）+ 按场景组合的思想 |
| React Flow (@xyflow/react v12) | 格式 | Node（id/type/position/data）和 Edge（id/source/target/type/data）接口作为编排配置的存储格式骨架；框架字段与业务字段严格分层（见原则四） |

### 1.3 核心原则

| # | 原则 | 规则 | 示例 |
|:---:|------|------|------|
| 一 | **同名同构** | 同一语义的字段在不同上下文中使用同一 Schema 组件，不重复定义 | ① `authConfig` → 触发器和连接器共用 `authConfigDef`<br>② `rateLimitConfig` → 入站和出站共用 `rateLimitConfigDef`<br>③ `input` → 触发器和连接器统一走 `httpInputDef` |
| 二 | **无用不存** | 不适用于当前场景的字段不出现在 JSON 中，由 Schema 的 `if`-`then` + `additionalProperties: false` 约束 | ① trigger 不含 `protocolConfig`（端点固定）<br>② trigger 不含 `timeoutMs`（引擎控制）<br>③ trigger 不含 `output`（由 exit 定义）<br>④ manual 触发不含 `authConfig` |
| 三 | **边即语义** | edge 不仅描述"谁连到谁"，还承载控制流语义——执行条件、错误路由、并行标记 | ① `businessType`：default / condition / error / loop_entry / loop_exit<br>② `connectionMode`：serial / parallel<br>③ `isStructural`：结构辅助边标记 |
| 四 | **框业分离** | React Flow 框架字段（id/type/position/source/target 等）不进 data，业务字段全在 data 内，两者不互串 | ① `node.id` / `node.type` / `node.position` → 框架字段<br>② `node.data.*` → 业务字段<br>③ `edge.source` / `edge.target` → 框架字段<br>④ `edge.data.*` → 业务字段 |

### 1.4 值表达式体系

编排中节点需要从多种来源取值——上游节点产出、固定常量、平台配置、内置函数、用户自定义脚本等。V3 统一为 `${$.scope.path}` 表达式语法，详细定义见 **[§3 值表达式体系](#3-值表达式体系)**。

> **核心要点**：① 5 种值来源（node / constant / system / script / execution）；② `node` 下分 `input`（入参）/ `output`（返回值）/ `current`（过程中参数，仅结构节点）；③ `system` 下分 `env`（环境变量）/ `fn`（内置函数）；④ 表达式支持嵌套引用。

---

## 2. 命名规范与数据约束

### 2.1 统一字段命名规则

| 上下文 | 规则 | 示例 |
|--------|------|------|
| JSON 内部所有键名 | camelCase | `nameCn` / `authConfig` / `connectorVersionId` |
| 引用外部资源 ID | `*Id` 后缀 + string 类型 | `connectorVersionId: "1234567890"` |
| 时间字段 | `*Time` 后缀 | `createTime` / `publishedTime` |
| 布尔字段 | `is*` 前缀 | `isDeleted` / `isTest` |
| 扩展字段 | `x_*` 前缀 | `x_customMetadata` |
| 数据库列级枚举 | TINYINT 数字（plan-db.md §0.7） | `connector_type=1` |
| JSON 内嵌枚举 | UPPER_SNAKE_CASE 字符串（例外，见 §2.2） | `"SOA"` / `"SYSTOKEN"` |
| React Flow node.type | snake_case（注册组件名） | `trigger` / `script` / `data_processor` / `loop-v2` |
| node.data.type（业务节点类型） | camelCase（JSON 内部规范，§2.1 第 1 行） | `trigger` / `script` / `dataProcessor` / `loopV2` |
| React Flow 框架字段 | 遵循官方命名 | `source`/`target`（非 sourceNodeId） |

### 2.2 JSON 内嵌枚举使用字符串的例外说明

> **设计决策**：`authConfig.type` 作为 MEDIUMTEXT JSON 嵌套字段，使用字符串枚举，非 TINYINT。
>
> | 维度 | 数据库列级枚举 | JSON 内嵌枚举 |
> |------|--------------|-------------|
> | **字段位置** | MySQL 列 | MEDIUMTEXT 列的 JSON 子字段 |
> | **枚举表示** | TINYINT | 字符串（`"SOA"` / `"AKSK"` 等） |
> | **设计理由** | 存储/索引效率 | 人类可读、版本快照 self-describing |
> | **规范适用** | plan-db.md §0.7 | 本文档 §2.2 |
>
> 枚举值对应关系（JSON 字符串 ⇄ DB TINYINT）：

| JSON 字符串 | TINYINT | 使用上下文 |
|------------|:---:|-----------|
| `SOA` | 1 | 连接器认证 |
| `APIG` | 2 | 连接器认证 |
| `NONE` | 4 | 连接器认证 |
| `AKSK` | 5 | 连接器认证 |
| `SYSTOKEN` | 7 | 触发器认证 |
| `COOKIE` | 8 | 连接器认证 |
| `SIGNATURE` | 9 | 连接器认证 |

### 2.3 FR-047 数据结构类型严格校验规则

> FR-047 是 V3 跨连接器和连接流的通用数据模型层约束，对所有 JSON Schema 定义的数据结构生效。

#### 2.3.1 基本类型限定

| 规则 | 说明 |
|------|------|
| 允许的基本类型 | `string`、`number`、`boolean`（仅三种） |
| null | 不作为合法字段类型 |
| number | 不区分 integer/float（统一为 `number`） |

#### 2.3.2 object 类型约束

- object 类型字段必须定义子字段结构（`properties` 非空）
- 禁止无子结构的空 object
- 每个子字段递归展开到基本类型

#### 2.3.3 array 类型约束

- array 类型字段必须声明 `items` 元素类型
- items 为 object 时需继续递归展开子字段到基本类型
- items 内各子字段的 value 表达式，最多只能引用一个上游 array 类型字段
- 禁止同时引用两个不同 array 源的字段（避免数组长度不一致歧义）
- 若 items 内所有 value 表达式均未引用 array 类型字段，数组最终长度为 1

#### 2.3.4 映射引用约束

- 禁止非基本类型（object/array）通过 value 表达式整体引用赋值
- object/array 必须逐字段展开，每个叶子字段各自引用基本类型字段
- value 表达式引用的上游字段类型必须与当前字段声明的 type 一致（string→string、number→number、boolean→boolean）
- 严禁隐式类型转换
- 所有映射表达式引用路径终点必须可解析到基本类型字段

#### 2.3.5 校验时机

| 校验时机 | 校验内容 | 行为 |
|---------|---------|------|
| Schema 编辑器输入 | object 无子字段 / array 无 items | 温和提示（虚线框标注），不禁用保存按钮 |
| 草稿保存 | FR-047 数据结构类型约束 | **不做校验**，值可暂存（仅做 DB 存储级别约束） |
| 连接器版本发布 (FR-007) | 入参/出参 Schema 合规性、FR-047 全部约束 | 不满足则**禁止发布**，提示具体字段和原因 |
| 连接流版本提交发布 (FR-026) | 所有节点间数据结构定义合规性、FR-047 全部约束 | 不满足则**禁止提交**，提示具体错误 |

> 💡 设计原则（spec v2.23）：草稿保存仅做数据库存储级别约束校验（列长度溢出等），**其余一律放行**。所有数据格式平台要求限制校验统一推迟到**发布时**执行。

---

## 3. 值表达式体系

编排中每个节点需要的字段值可能来自多种来源——不仅仅是上游节点的输出，也可能是固定常量、系统配置、或内置函数计算结果。V3 统一为**单一表达式语法**，覆盖全部值来源。

### 3.1 值来源总览

| # | 作用域 | 性质 | 语法 | 示例 | 说明 |
|:---:|------|:---:|------|------|------|
| 1 | `node` | 设计态 | `${$.node.{id}.{input\|output\|current}.path}` | 见下方示例 | 引用任意节点的三个数据面：`input`（入参）、`output`（返回值）、`current`（过程中参数，仅结构节点） |
| 2 | `constant` | 设计态 | `${$.constant:value}` | `${$.constant:0}` | 编排设计者填入的固定值 |
| 3 | `system` | 设计态 | `${$.system.env.{key}}` / `.fn.{name}(args)` | `${$.system.env.region}`、`${$.system.fn.upper(...)}` | 双子类：`env`（环境变量含密钥）、`fn`（内置函数，值=Java类全路径.invoke） |
| 4 | `script` | 设计态 | `${$.script.{name}(args)}` | `${$.script.normalize(...)}` | 用户预定义脚本，按名引用传参，脚本名对应 Java 类全路径 `com.openapp.script.XxxScript.invoke` |
| 5 | `execution` | 运行时注入 | `${$.execution.id}` / `.flowId` / `.triggerTime` | `${$.execution.flowId}` | 引擎每次执行时注入的运行时元数据 |

> 表达式层级：`$.` = 根 → `node`/`constant`/`system`/`script`/`execution` = 作用域 → 具体路径或参数。

**`node` 的三个数据面**：

| 路径 | 语义 | 生命周期 | 示例 |
|------|------|---------|------|
| `input` | 节点的入参 | 节点执行期间 | `${$.node.trigger.input.sender}` — 触发器收到的请求字段 |
| `output` | 节点的返回值 | 执行完成后下游可引用 | `${$.node.node_1.output.msgId}` — 连接器调用下游 API 的返回 |
| `current` | 节点的过程中参数 | 仅结构节点（loop/error_handler）体内有效 | `${$.node.loop_1.current.item}` — 循环体内当前迭代元素 |

**结构节点 `current` 引用示例**：

| 场景 | `input` | `output` | `current` |
|------|:--:|:--:|------|
| 触发器收到请求 | `${$.node.trigger.input.sender}` | —（无返回值） | — |
| 连接器调用结果 | — | `${$.node.conn_1.output.msgId}` | — |
| 循环体内当前元素 | — | `${$.node.loop_1.output.items}`（原始数组） | `${$.node.loop_1.current.item}` |
| 循环体内当前索引 | — | — | `${$.node.loop_1.current.index}` |
| 多重循环内层 | — | — | `${$.node.loop_inner.current.item}` |
| 多重循环同时引用外层 | — | — | `${$.node.loop_outer.current.item}` |
| 错误处理体内错误码 | — | — | `${$.node.err_1.current.code}` |

### 3.2 运行时上下文对象

表达式 `${$.node.trigger.input.body.sender}` 中的 `$` 代表引擎构造的**运行时上下文 JSON 根对象**。以下按 HTTP 协议场景完整展开 trigger（入参三段式）、connector（入参三段式 + 出参两段式）、exit（出参两段式）、loop/error_handler（current 运行时上下文）：

```json
{
  "node": {
    "trigger": {
      "input": {
        "header": {
          "Authorization": "Bearer token-xxx",
          "Content-Type": "application/json"
        },
        "query": {
          "page": "1",
          "size": "20"
        },
        "body": {
          "sender": "u001",
          "content": "你好",
          "items": ["a", "b", "c"]
        }
      }
    },
    "conn_1": {
      "input": {
        "header": {
          "Authorization": "Bearer sk-xxxxxxxxxxxx"
        },
        "query": {
          "page": "1"
        },
        "body": {
          "itemId": "b",
          "size": 20
        }
      },
      "output": {
        "header": {
          "X-Request-Id": "req-001",
          "X-RateLimit-Remaining": "99"
        },
        "body": {
          "msgId": "msg_001",
          "name": "alice",
          "data": "raw data"
        }
      }
    },
    "exit": {
      "output": {
        "header": {
          "X-Trace-Id": "trace-abc"
        },
        "body": {
          "total": 3,
          "execId": "exec-2026-001"
        }
      }
    },
    "loop_1": {
      "output": { "items": ["a", "b", "c"] },
      "current": { "item": "b", "index": 1, "total": 3 }
    },
    "err_1": {
      "current": {
        "code": "503",
        "messageZh": "下游服务不可用",
        "messageEn": "Service Unavailable",
        "cause": "连接超时"
      }
    }
  },
  "system": {
    "env": {
      "apiKey": "sk-xxxxxxxxxxxx",
      "region": "cn-east",
      "locale": "zh-CN",
      "timeout": 5000
    },
    "fn": {
      "upper":      "com.xxx.it.works.wecode.v2.modules.runtime.fn.string.UpperFunction.invoke",
      "concat":     "com.xxx.it.works.wecode.v2.modules.runtime.fn.string.ConcatFunction.invoke",
      "substring":  "com.xxx.it.works.wecode.v2.modules.runtime.fn.string.SubstringFunction.invoke",
      "add":        "com.xxx.it.works.wecode.v2.modules.runtime.fn.math.AddFunction.invoke",
      "length":     "com.xxx.it.works.wecode.v2.modules.runtime.fn.array.LengthFunction.invoke",
      "if":         "com.xxx.it.works.wecode.v2.modules.runtime.fn.logic.IfFunction.invoke",
      "toString":   "com.xxx.it.works.wecode.v2.modules.runtime.fn.convert.ToStringFunction.invoke",
      "toNumber":   "com.xxx.it.works.wecode.v2.modules.runtime.fn.convert.ToNumberFunction.invoke",
      "toBoolean":  "com.xxx.it.works.wecode.v2.modules.runtime.fn.convert.ToBooleanFunction.invoke",
      "formatDate": "com.xxx.it.works.wecode.v2.modules.runtime.fn.date.FormatDateFunction.invoke"
    }
  },
  "script": {
    "normalize":     "com.openapp.script.NormalizeScript.invoke",
    "randomUserInfo": "com.openapp.script.RandomUserInfoScript.invoke"
  },
  "execution": { "id": "exec-2026-001", "flowId": "flow-12345", "triggerTime": "2026-06-10T10:00:00Z" }
}
```

> `constant` 不在运行时 JSON 中：`${$.constant:20}` 的值 `20` 直接写在表达式里，引擎解析表达式语法即得值，无需存入运行时上下文对象。

**各节点的数据面**：

| 节点类型 | `input` | `output` | `current` | 说明 |
|---------|:------:|:------:|:------:|------|
| trigger | ✅ header / query / body | — | — | 仅入参，HTTP 请求的三段 |
| connector | ✅ header / query / body（镜像 input） | ✅ header / body（镜像 output） | — | 入参 + 出参 |
| exit | — | ✅ header / body | — | 仅出参，对外 HTTP 响应 |
| loop_v2 | — | ✅（原始数组等） | ✅ item / index / total | output 为持久化属性，current 为迭代上下文 |
| error_handler | — | ✅（错误统计等） | ✅ code / messageZh / messageEn / cause | current 跟随 errorInfoDef |

> ⚠️ **引用约束**：
> - **不支持引用触发器节点的 output**：触发器无出参，HTTP 请求数据通过 `input` 获取（header/query/body）
> - **不支持引用连接器节点的 input**：连接器的入参来源于上游节点，引用 `${$.node.conn_1.input.xxx}` 无意义——应引用上游节点的 `output`

**Path 解析对照 — 按作用域分组**：

| JSON Path | 解析结果 | 作用域 |
|-----------|---------|:---:|
| `$.node.trigger.input.header.Authorization` | `"Bearer token-xxx"` | node : input |
| `$.node.trigger.input.header.Content-Type` | `"application/json"` | node : input |
| `$.node.trigger.input.query.page` | `"1"` | node : input |
| `$.node.trigger.input.query.size` | `"20"` | node : input |
| `$.node.trigger.input.body.sender` | `"u001"` | node : input |
| `$.node.trigger.input.body.content` | `"你好"` | node : input |
| `$.node.trigger.input.body.items` | `["a", "b", "c"]` | node : input |
| `$.node.conn_1.input.header.Authorization` | `"Bearer sk-xxxxxxxxxxxx"` | node : input |
| `$.node.conn_1.input.query.page` | `"1"` | node : input |
| `$.node.conn_1.input.body.itemId` | `"b"` | node : input |
| `$.node.conn_1.input.body.size` | `20` | node : input |
| `$.node.conn_1.output.header.X-Request-Id` | `"req-001"` | node : output |
| `$.node.conn_1.output.header.X-RateLimit-Remaining` | `"99"` | node : output |
| `$.node.conn_1.output.body.msgId` | `"msg_001"` | node : output |
| `$.node.conn_1.output.body.name` | `"alice"` | node : output |
| `$.node.conn_1.output.body.data` | `"raw data"` | node : output |
| `$.node.exit.output.header.X-Trace-Id` | `"trace-abc"` | node : output |
| `$.node.exit.output.body.total` | `3` | node : output |
| `$.node.exit.output.body.execId` | `"exec-2026-001"` | node : output |
| `$.node.loop_1.current.item` | `"b"` | node : current |
| `$.node.loop_1.current.index` | `1` | node : current |
| `$.node.loop_1.current.total` | `3` | node : current |
| `$.node.err_1.current.code` | `"503"` | node : current |
| `$.node.err_1.current.messageZh` | `"下游服务不可用"` | node : current |
| `$.node.err_1.current.cause` | `"连接超时"` | node : current |
| `$.system.apiKey` | `"sk-xxxxxxxxxxxx"` | system |
| `$.system.env.region` | `"cn-east"` | system |
| `$.system.env.locale` | `"zh-CN"` | system |
| `$.system.fn.upper($.node.conn_1.output.body.name)` | `"ALICE"` | system.fn |
| `$.script.normalize($.node.conn_1.output.body.data, $.system.env.locale)` | `"normalized raw data"` | script |
| `$.execution.flowId` | `"flow-12345"` | execution |
| `$.execution.triggerTime` | `"2026-06-10T10:00:00Z"` | execution |

> `current` 不是独立作用域，是 `node` 下结构节点的运行时子路径，与 `input`/`output` 平级。仅在对应结构体内有效，多重循环按节点 ID 精确区分。
>
> `loop_v2` / `error_handler` 节点的 `output` 字段结构（持久化属性）和 `current` 下可用字段的完整列表，待 §4.3.14~§4.3.15 `errorHandlerNodeDataDef` / `loopNodeDataDef` 专项细化后确定。

### 3.3 设计原则

| # | 原则 | 说明 |
|:---:|------|------|
| 1 | **映射结构镜像需求结构** | connector 的 input 分 header/query/body，则 input 也分 header/query/body |
| 2 | **Schema 不硬编码协议** | input/output 定义为 `"type": "object"`，具体分段由应用层按协议校验 |
| 3 | **表达式体系统一** | 5 种值来源共用同一套 `${$.scope.path}` 语法，不因来源不同而异 |
| 4 | **必填检查在应用层** | mapping 是否覆盖 required 字段，由应用层校验，JSON Schema 不做跨对象约束 |

### 3.4 节点数据引用详述

DAG 中节点按拓扑顺序执行，上游节点的输出数据需要传递给下游节点。引用模型基于 **JSON 节点上下文对象**，区分设计态和运行态：

| | 设计态（Design-time） | 运行态（Runtime） |
|------|-------------------|---------------|
| **是什么** | 节点上下文对象的 Schema 定义 | 引擎根据设计态构造的实际 JSON 对象 |
| **谁来定义** | connector 的 input/output 等 | 引擎在节点执行时自动构造 |
| **示例** | `{ type: "object", properties: { sender: { type: "string" } } }` | `{ sender: "u001", content: "你好" }` |

> 编排配置中存储设计态定义，引擎运行时根据定义构造 JSON 节点上下文对象，再按 mapping 映射到当前节点。

```

设计态（编排配置中存储）                    运行态（引擎执行时构造）
┌─────────────────────────┐              ┌─────────────────────────┐
│ trigger.input         │              │ trigger.context         │
│ {                       │    构造      │ {                       │
│   body: {               │  ────────▶   │   input: {             │
│     properties: {       │              │     sender: "u001",    │
│       sender: {...},    │              │     content: "你好"     │
│       content: {...}    │              │   },                   │
│     }                   │              │   output: { ... }      │
│   }                     │              │ }                      │
│ }                       │              └─────────────────────────┘
└─────────────────────────┘
```

### 3.5 系统内置函数

数据处理器（`data_processor`）节点可在映射表达式中使用系统内置函数。`system.fn.{name}` 在引擎中解析为 Java 类全路径，反射调用并返回值，参数由引擎自动传入：

| 类别 | 函数名 | 类路径 | 说明 |
|------|--------|--------|------|
| 字符串 | `upper` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.string.UpperFunction.invoke` | 转大写 |
| 字符串 | `lower` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.string.LowerFunction.invoke` | 转小写 |
| 字符串 | `concat` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.string.ConcatFunction.invoke` | 多字符串拼接 |
| 字符串 | `substring` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.string.SubstringFunction.invoke` | 截取子串 |
| 数学 | `add` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.math.AddFunction.invoke` | 加法 |
| 数学 | `multiply` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.math.MultiplyFunction.invoke` | 乘法 |
| 数学 | `round` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.math.RoundFunction.invoke` | 四舍五入 |
| 数学 | `abs` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.math.AbsFunction.invoke` | 绝对值 |
| 数组 | `length` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.array.LengthFunction.invoke` | 数组长度 |
| 数组 | `first` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.array.FirstFunction.invoke` | 首元素 |
| 数组 | `join` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.array.JoinFunction.invoke` | 用分隔符连接 |
| 逻辑 | `if` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.logic.IfFunction.invoke` | 三元条件 (cond, then, else) |
| 逻辑 | `equals` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.logic.EqualsFunction.invoke` | 相等比较 |
| 逻辑 | `isEmpty` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.logic.IsEmptyFunction.invoke` | 空值检测 |
| 类型转换 | `toString` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.convert.ToStringFunction.invoke` | 转为 string |
| 类型转换 | `toNumber` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.convert.ToNumberFunction.invoke` | 转为 number |
| 类型转换 | `toBoolean` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.convert.ToBooleanFunction.invoke` | 转为 boolean |
| 日期 | `formatDate` | `com.xxx.it.works.wecode.v2.modules.runtime.fn.date.FormatDateFunction.invoke` | 日期格式转换 (value, fromFormat, toFormat) |

### 3.6 自定义脚本

用户可预先定义脚本（如 Groovy / JavaScript），存储到平台脚本库中。编排设计时按名引用，传入参数，引擎运行时执行并返回结果。

```

${$.script.validateItem($.node.loop_1.current.item, $.system.env.region)}
```

脚本参数可嵌套引用任意值来源（node / constant / system / system.fn / execution / script）。

### 3.7 嵌套规则

所有值来源的参数支持互相嵌套：

```

// 系统函数 + 节点引用 + 常量
${$.system.fn.concat($.node.trigger.input.firstName, $.constant:" ", $.node.trigger.input.lastName)}

// 自定义脚本 + 循环上下文 + 系统变量
${$.script.validate($.node.loop_1.current.item, $.system.env.region)}

// 错误处理中引用错误信息 + 执行元数据
${$.system.fn.format($.node.err_1.current.messageZh, $.execution.flowId)}
```

---

## 4. 共享 Schema 组件库

> 以下 §5（连接器配置）和 §6（连接流编排配置）均引用本章定义的共享组件。

### 4.1 设计思路与整体架构

为避免「JSON 字段与 JSON 字段定义同名」的歧义，本规范区分两者：
- **JSON 字段（field name）**：JSON 数据中的属性键，描述「存什么数据」（如 `authConfig`）
- **JSON 字段定义（definition key）**：校验规则组件键名（如 `authConfigDef`）

**命名规则**：

| # | 规则 | 适用对象 | 示例 |
|:---:|------|------|------|
| 1 | **字段全名 + `Def`** | 有直接对应 JSON 字段的组件 | `authConfig` → `authConfigDef`、`rateLimitConfig` → `rateLimitConfigDef`、`input` → `httpInputDef`、`errorInfo` → `errorInfoDef` |
| 2 | **`{type}NodeDataDef`** | 第二层节点 data 定义，通过 allOf 继承 `nodeDataBaseDef` | `triggerNodeDataDef`、`connectorNodeDataDef`、`exitNodeDataDef` 等 |
| 3 | **`{协议}InputDef` / `{协议}OutputDef`** | 第三层协议具体实现，不额外加 `Contract`/`Schema` 后缀 | `httpInputDef`、`httpOutputDef` |
| 4 | **语义化名 + `Def`** | 第四层基础复用组件，无直接对应 JSON 字段 | `jsonObjectDef`（v2 合并 mappedFieldDef/mappedJsonSchemaObjectDef，value 可选） |

**18 个组件的关系全景**：

```mermaid
graph TB
    subgraph L1["第一层：协议无关路由器"]
        ND["nodeDataDef<br/>按 node.type 分发"]
    end

    subgraph L2["第二层：节点 data 定义"]
        TD["triggerNodeDataDef"]
        CD["connectorNodeDataDef"]
        DD["dataProcessorNodeDataDef"]
        ED["exitNodeDataDef"]
        LD["loopNodeDataDef"]
        PD["parallelNodeDataDef"]
        BD["conditionBranchNodeDataDef"]
        HD["errorHandlerNodeDataDef"]
        TX["textNodeDataDef"]
    end

    subgraph L3["第三层：协议实现"]
        HI["httpInputDef"]
        HO["httpOutputDef"]
    end

    subgraph L4["第四层：基础复用"]
        JS["jsonObjectDef<br/>value可选"]
    end

    subgraph LX["横跨层"]
        CC["connectorVersionConfigDef"]
        AU["authConfigDef"]
        RL["rateLimitConfigDef"]
        NB["nodeDataBaseDef"]
        ER["errorInfoDef"]
    end

    ND --> TD
    ND --> CD
    ND --> DD
    ND --> ED
    ND --> LD
    ND --> PD
    ND --> BD
    ND --> HD
    ND --> TX
    HI --> JS
    HO --> JS
    CD --> JS
    CD --> CC
    ED --> JS
    TD --> AU

    style L1 fill:#e8eaf6,stroke:#3f51b5
    style L2 fill:#e3f2fd,stroke:#1565c0
    style L3 fill:#fff3e0,stroke:#ef6c00
    style L4 fill:#f3e5f5,stroke:#7b1fa2
    style LX fill:#e8f5e9,stroke:#2e7d32
```

| 层 | 职责 | 组件 |
|:--:|------|------|
| 第一层 | 路由器（oneOf 按 node.type 分发） | `nodeDataDef` |
| 第二层 | 9 种节点 data 定义（业务数据载体） | `triggerNodeDataDef` ~ `textNodeDataDef` |
| 第三层 | 协议具体实现（HTTP header/query/body） | `httpInputDef`、`httpOutputDef` |
| 第四层 | 基础复用组件（被上层引用） | `jsonObjectDef`（v2 合并 mappedFieldDef/mappedJsonSchemaObjectDef，value 可选） |
| 横跨层 | 多场景复用（认证/限流/基类/错误） | `connectorVersionConfigDef`、`authConfigDef`、`rateLimitConfigDef`、`nodeDataBaseDef`、`errorInfoDef` |

### 4.2 组件速查表

| # | 组件名 | 层 | 用途 | 被引用方 |
|:---:|--------|:---:|------|---------|
| 1 | `jsonObjectDef` | 第四层 | 基础复用：字段定义（value 可选，递归嵌套） | §6.4 orchestrationConfig 等全部组件 |
| 2 | `authConfigDef` | 横跨 | 认证类型声明（含凭证字段列表） | §5 connectionConfig, §4.3.9 triggerNodeDataDef |
| 3 | `rateLimitConfigDef` | 横跨 | 限流配置（QPS + 并发） | §5 connectionConfig, §4.3.9 triggerNodeDataDef |
| 4 | `errorInfoDef` | 横跨 | 错误详情（code + 双语 message + 根因） | §7 执行数据 |
| 5 | `nodeDataBaseDef` | 横跨 | 节点 data 公共基类（type / labelCn / labelEn / structConfig） | 全部节点 data 子 Def（allOf 继承） |
| 6 | `httpInputDef` | 第三层 | HTTP 入参声明（header/query/body 三段式） | §4.3.9 triggerNodeDataDef, §5 connectionConfig |
| 7 | `httpOutputDef` | 第三层 | HTTP 出参声明（header/body 两段式） | §5 connectionConfig |
| 8 | `connectorVersionConfigDef` | 横跨 | 连接器配置完整定义（供 §5 connectionConfig 和编排快照共用） | §4.3.10 connectorNodeDataDef, §5.2 connectionConfig |
| 9 | `triggerNodeDataDef` | 第二层 | 触发器节点业务数据 | §4.3.9 |
| 10 | `connectorNodeDataDef` | 第二层 | 连接器节点业务数据（身份 + 配置快照 + 超时 + 字段映射） | §4.3.10 |
| 11 | `exitNodeDataDef` | 第二层 | 出口节点业务数据 | §4.3.11 |
| 12 | `dataProcessorNodeDataDef` | 第二层 | 数据处理器节点业务数据 | §4.3.12 |
| 13 | `errorHandlerNodeDataDef` | 第二层 | 错误处理节点业务数据（继承 nodeDataBaseDef） | §4.3.14 |
| 14 | `loopNodeDataDef` | 第二层 | 循环节点业务数据（继承 nodeDataBaseDef） | §4.3.15 |
| 15 | `textNodeDataDef` | 第二层 | text 标记节点数据（继承 nodeDataBaseDef） | §4.3.16 |
| 16 | `parallelNodeDataDef` | 第二层 | 并行节点业务数据（继承 nodeDataBaseDef） | §4.3.17 |
| 17 | `conditionBranchNodeDataDef` | 第二层 | 条件分支节点业务数据（继承 nodeDataBaseDef） | §4.3.18 |
| 18 | `nodeDataDef` | 第一层 | 节点 data 路由器（oneOf 按 node.type 分发至 9 种 data） | §4.3.19, §6.4 orchestrationConfig |

### 4.3 组件详解

> 以 `jsonObjectDef` 为基础，按依赖关系逐层展开。

#### 4.3.1 jsonObjectDef

> 基础复用组件。value 可选——有值=编排映射场景，无值=纯声明场景。每个字段自维护所有属性（required/sensitive/value 等），不做顶层 `required` 数组。

> **Def**

```json
{
  "$id": "urn:openapp:schema:jsonObjectDef:v2",
  "type": "object",
  "properties": {
    "type": { "type": "string", "enum": ["object"] },
    "properties": {
      "type": "object",
      "additionalProperties": {
        "oneOf": [
          {
            "description": "叶子字段：基本类型",
            "type": "object",
            "properties": {
              "type":        { "type": "string", "enum": ["string", "number", "boolean"] },
              "description": { "type": "string" },
              "value":       { "type": "string", "description": "映射表达式（可选）。遵循 §3 值表达式体系" },
              "required":    { "type": "boolean", "default": false, "description": "是否必填" },
              "sensitive":   { "type": "boolean", "default": false, "description": "敏感字段标记：① 落库加密存储 ② 日志脱敏打印" },
              "readonly":    { "type": "boolean", "default": false, "description": "字段是否只读" },
              "deprecated":  { "type": "boolean", "default": false, "description": "是否已废弃" },
              "nullable":    { "type": "boolean", "default": false, "description": "字段值是否允许为 null（传 null 或不传）。注意：与字段类型声明无关——FR-047 禁止声明 type=\"null\"，nullable 仅控制值的可选性" },
              "placeholder": { "type": "string",  "description": "输入框占位提示" },
              "pattern":     { "type": "string",  "description": "正则校验表达式，如 ^1[3-9]\\d{9}$" },
              "minLength":   { "type": "number",  "description": "字符串最小长度" },
              "maxLength":   { "type": "number",  "description": "字符串最大长度" },
              "enum":        { "type": "array" },
              "default":     {},
              "minimum":     { "type": "number" },
              "maximum":     { "type": "number" }
            },
            "required": ["type"]
          },
          {
            "description": "嵌套 object：递归引用自身",
            "$ref": "#/definitions/jsonObjectDef"
          },
          {
            "description": "数组字段",
            "type": "object",
            "properties": {
              "type":        { "type": "string", "enum": ["array"] },
              "description": { "type": "string" },
              "required":    { "type": "boolean", "default": false },
              "readonly":    { "type": "boolean", "default": false },
              "deprecated":  { "type": "boolean", "default": false },
              "minItems":    { "type": "number",  "description": "数组最少元素数" },
              "maxItems":    { "type": "number",  "description": "数组最多元素数" },
              "items":       { "$ref": "#/definitions/jsonObjectDef" }
            },
            "required": ["type", "items"]
          }
        ]
      }
    }
  },
  "required": ["type", "properties"]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 适用 | 说明 |
|-----------|------|:----:|:--:|------|
| type | string | ✅ | 全部 | 顶层固定 `"object"` |
| properties | object | ✅ | 全部 | 字段定义集合，每个字段自维护所有属性 |
| properties.{key}.type | string | ✅ | 全部 | 字段类型：`string` / `number` / `boolean` / `object`（递归）/ `array` |
| properties.{key}.required | boolean | ❌ | 全部 | 是否必填，默认 `false` |
| properties.{key}.value | string | ❌ | 叶子 | 映射表达式。遵循 §3 值表达式体系 |
| properties.{key}.sensitive | boolean | ❌ | 叶子 | 敏感字段标记：① 落库加密存储 ② 日志脱敏打印，默认 `false` |
| properties.{key}.readonly | boolean | ❌ | 全部 | 字段是否只读，默认 `false` |
| properties.{key}.deprecated | boolean | ❌ | 全部 | 是否已废弃，默认 `false` |
| properties.{key}.description | string | ❌ | 全部 | 字段描述 |
| properties.{key}.placeholder | string | ❌ | 叶子 | 输入框占位提示（string 类型可用） |
| properties.{key}.pattern | string | ❌ | 叶子 | 正则校验，如 `^1[3-9]\d{9}$`（string 类型可用） |
| properties.{key}.minLength | number | ❌ | 叶子 | 字符串最小长度（string 类型可用） |
| properties.{key}.maxLength | number | ❌ | 叶子 | 字符串最大长度（string 类型可用） |
| properties.{key}.enum | array | ❌ | 叶子 | 枚举值列表 |
| properties.{key}.default | any | ❌ | 叶子 | 默认值 |
| properties.{key}.minimum | number | ❌ | 叶子 | 最小值（number 类型可用） |
| properties.{key}.maximum | number | ❌ | 叶子 | 最大值（number 类型可用） |
| properties.{key}.nullable | boolean | ❌ | 叶子 | 字段值是否允许为 null（传 null 或不传）。与类型声明无关——FR-047 禁止 type="null"，nullable 仅控制值的可选性。默认 `false` |
| properties.{key}.minItems | number | ❌ | array | 数组最少元素数 |
| properties.{key}.maxItems | number | ❌ | array | 数组最多元素数 |
| properties.{key}.items | object | ❌ | array | 数组元素定义（type=array 时必填） |

> **示例** — sender/content 必填，phone 脱敏，全部字段级声明：

```json
{
  "type": "object",
  "properties": {
    "sender":  { "type": "string", "required": true,  "description": "发送者 ID" },
    "content": { "type": "string", "required": true,  "description": "消息内容" },
    "phone":   { "type": "string", "required": false, "description": "手机号", "sensitive": true }
  }
}
```

> **示例** — 带 value 映射 + 嵌套 object：

```json
{
  "type": "object",
  "properties": {
    "receiver": { "type": "string", "required": true, "value": "${$.node.trigger.input.body.sender}" },
    "content":  { "type": "string", "required": true, "value": "${$.node.trigger.input.body.content}" },
    "metadata": {
      "type": "object",
      "properties": {
        "source":    { "type": "string", "required": true,  "value": "${$.constant:openplatform}" },
        "timestamp": { "type": "string", "required": false, "value": "${$.node.trigger.input.body.ts}" }
      }
    }
  }
}
```

> **示例** — 带 value 映射（编排 input）：

```json
{
  "type": "object",
  "properties": {
    "receiver": { "type": "string", "value": "${$.node.trigger.input.body.sender}" },
    "content":  { "type": "string", "value": "${$.node.trigger.input.body.content}" },
    "phone":    { "type": "string", "value": "${$.node.trigger.input.body.phone}", "sensitive": true }
  },
  "required": ["receiver", "content"]
}
```

#### 4.3.2 authConfigDef

> **Def** — v2 重构：`fields[]` 自定结构改为 `header/query` 复用 `jsonObjectDef`，对齐参数定义规范。

```json
{
  "$id": "urn:openapp:schema:authConfigDef:v2",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "type": { "type": "string", "enum": ["SOA", "APIG", "SYSTOKEN", "AKSK", "NONE", "COOKIE", "SIGNATURE"] },
    "header": { "$ref": "#/definitions/jsonObjectDef", "description": "放置在请求头的认证字段。字段名即 HTTP 字段名" },
    "query":  { "$ref": "#/definitions/jsonObjectDef", "description": "放置在 Query 的认证字段。字段名即 Query 参数名" },
    "secretKey": {
      "$ref": "#/definitions/jsonObjectDef",
      "description": "签名密钥定义（仅 SIGNATURE 类型使用）。字段含 sensitive=true 标记加密存储 + 运行时脱敏"
    },
    "sysAccountWhitelist": {
      "type": "array",
      "items": { "type": "string" },
      "uniqueItems": true,
      "description": "允许触发此连接流的 SYSTOKEN 账号 ID 列表（仅触发器使用）。运行时凭证解析出 sysAccountId 后校验。空数组 = 全部禁止（EC-011）"
    }
  },
  "required": ["type"],
  "anyOf": [
    { "required": ["header"] },
    { "required": ["query"] }
  ],
  "allOf": [
    {
      "if": {
        "properties": { "type": { "const": "SYSTOKEN" } },
        "required": ["type"]
      },
      "then": {
        "required": ["sysAccountWhitelist"]
      }
    },
    {
      "if": {
        "properties": { "type": { "const": "SIGNATURE" } },
        "required": ["type"]
      },
      "then": {
        "required": ["secretKey"]
      }
    }
  ]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 说明 |
|-----------|------|:----:|------|
| type | string | ✅ | 认证类型：`SOA` / `APIG` / `SYSTOKEN` / `AKSK` / `NONE` / `COOKIE` / `SIGNATURE` |
| header | object | ❌ ⚡ | 放置在请求头的认证字段。与 `query` 至少二选一。字段名即 HTTP 字段名 |
| query | object | ❌ ⚡ | 放置在 Query 的认证字段。与 `header` 至少二选一。字段名即 Query 参数名 |
| sysAccountWhitelist[] | array | ⚡⚡ | 允许触发此流的 SYSTOKEN 账号 ID 列表。运行时解析凭证得到 sysAccountId 后校验。type=SYSTOKEN 时必填。空数组=全部禁止 |
| secretKey | object | ⚡⚡⚡ | 签名密钥定义（仅 SIGNATURE 类型使用）。复用 jsonObjectDef，sensitive 标记加密 + 脱敏。type=SIGNATURE 时必填 |

⚡ = anyOf：`header` / `query` 至少声明其一。
⚡⚡ = allOf：`type=SYSTOKEN` 时必填。
⚡⚡⚡ = allOf：`type=SIGNATURE` 时必填。

> **认证类型值来源总览**

| 认证类型 | 凭据数 | 值来源 | value 表达式 |
|---------|:---:|------|-------------|
| `SOA` | 1 | 凭据库静态值 | `${$.system.env.soaToken}` |
| `APIG` | 2 | 凭据库静态值 | `${$.system.env.apigAppKey}` / `${$.system.env.apigAppSecret}` |
| `SYSTOKEN` | 1 | 凭据库静态值 + tokenId 白名单校验 | `${$.system.env.sysToken}` |
| `AKSK` | 1 个字段 | 凭据库密钥对，引擎动态签名 | `${$.system.env.akskSignature}` |
| `NONE` | 0 | — | — |
| `COOKIE` | 1 个字段 | 上游触发器请求头 | `${$.node.trigger.input.header.Cookie}` |
| `SIGNATURE` | 1 个字段 + 1 个密钥 | 用户配置常量为签名密钥；引擎动态签名 | 字段: `${$.system.env.signature}`；密钥: `secretKey` (jsonObjectDef) |

> **示例**

> **SOA** — 值来源：凭据库静态 token

```json
{
  "type": "SOA",
  "header": {
    "type": "object",
    "properties": {
      "X-Soa-Token": {
        "type": "string",
        "required": true,
        "sensitive": true,
        "value": "${$.system.env.soaToken}",
        "description": "SOA 认证令牌。值来源：凭据库"
      }
    }
  }
}
```

> **APIG** — 值来源：凭据库静态 key + secret

```json
{
  "type": "APIG",
  "query": {
    "type": "object",
    "properties": {
      "apigAppKey": {
        "type": "string",
        "required": true,
        "value": "${$.system.env.apigAppKey}",
        "description": "APIG 应用标识。值来源：凭据库"
      },
      "apigAppSecret": {
        "type": "string",
        "required": true,
        "sensitive": true,
        "value": "${$.system.env.apigAppSecret}",
        "description": "APIG 应用密钥。值来源：凭据库"
      }
    }
  }
}
```

> **SYSTOKEN** — 值来源：凭据库静态 token + 账号白名单

```json
{
  "type": "SYSTOKEN",
  "header": {
    "type": "object",
    "properties": {
      "X-Sys-Token": {
        "type": "string",
        "required": true,
        "sensitive": true,
        "value": "${$.system.env.sysToken}",
        "description": "系统凭证令牌。值来源：凭据库"
      }
    }
  },
  "sysAccountWhitelist": ["stk-u001", "stk-u002"]
}
```

> **AKSK** — 值来源：凭据库密钥对，引擎动态签名

```json
{
  "type": "AKSK",
  "header": {
    "type": "object",
    "properties": {
      "X-AKSK-Signature": {
        "type": "string",
        "required": true,
        "sensitive": true,
        "value": "${$.system.env.akskSignature}",
        "description": "AKSK 签名值。引擎运行时从凭据库取密钥对动态签名计算"
      }
    }
  }
}
```

> **NONE** — 无认证

```json
{
  "type": "NONE"
}
```

> **COOKIE** — 值来源：上游触发器请求头 Cookie 字段（用户浏览器/设备）

```json
{
  "type": "COOKIE",
  "header": {
    "type": "object",
    "properties": {
      "Cookie": {
        "type": "string",
        "required": true,
        "sensitive": true,
        "value": "${$.node.trigger.input.header.Cookie}",
        "description": "Cookie 请求头。值来源：上游触发器请求头中的 Cookie 字段（用户浏览器/设备携带）"
      }
    }
  }
}
```

> **SIGNATURE** — 值来源：用户配置常量签名密钥 + 引擎动态签名

```json
{
  "type": "SIGNATURE",
  "secretKey": {
    "type": "object",
    "properties": {
      "signSecretKey": {
        "type": "string",
        "required": true,
        "sensitive": true,
        "value": "${$.constant:user-configured-secret-key}",
        "description": "签名密钥。用户配置常量值，落库加密存储（sensitive 标记）"
      }
    }
  },
  "header": {
    "type": "object",
    "properties": {
      "X-Signature": {
        "type": "string",
        "required": true,
        "sensitive": true,
        "value": "${$.system.env.signature}",
        "description": "签名值。引擎运行时使用 secretKey 指向的密钥动态签名计算。密钥 + 签名值双脱敏"
      }
    }
  }
}
```

#### 4.3.3 rateLimitConfigDef

> **Def**

```json
{
  "$id": "urn:openapp:schema:rateLimitConfigDef:v1",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "maxQps": { "type": "integer", "minimum": 1, "maximum": 1000 },
    "maxConcurrency": { "type": "integer", "minimum": 1, "maximum": 1000 }
  }
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 说明 |
|-----------|------|:----:|------|
| maxQps | integer | ❌ | 每秒最大请求数，范围 1-1000 |
| maxConcurrency | integer | ❌ | 最大并发数，范围 1-1000 |

> **示例**

```json
{ "maxQps": 100, "maxConcurrency": 10 }
```

#### 4.3.4 errorInfoDef

> **Def**

```json
{
  "$id": "urn:openapp:schema:errorInfoDef:v2",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "code": { "type": "string", "pattern": "^[1-9][0-9]{2,4}$" },
    "messageZh": { "type": "string" },
    "messageEn": { "type": "string" },
    "cause": { "type": "string" },
    "downstreamStatus": { "type": "integer" },
    "downstreamBody": { "type": "string" }
  },
  "required": ["code", "messageZh", "messageEn"],
  "oneOf": [
    { "required": ["cause"] },
    { "required": ["downstreamStatus"] }
  ]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 说明 |
|-----------|------|:----:|------|
| code | string | ✅ | 错误码。4xx/5xx=下游错误，6xxxx=内部错误 |
| messageZh | string | ✅ | 错误中文描述 |
| messageEn | string | ✅ | 错误英文描述 |
| cause | string | ❌ ⚡ | 根因描述（内部错误时必填） |
| downstreamStatus | integer | ❌ ⚡ | 下游 HTTP 状态码（下游错误时必填） |
| downstreamBody | string | ❌ | 下游响应体片段（截断到 512 字符） |

**错误码字典**：

| Code | messageZh | messageEn | 来源 |
|:----:|-----------|-----------|:----:|
| `400` | 下游请求参数错误 | Bad Request | downstream |
| `401` | 下游未授权 | Unauthorized | downstream |
| `403` | 下游无权限 | Forbidden | downstream |
| `404` | 下游资源不存在 | Not Found | downstream |
| `500` | 下游内部错误 | Internal Server Error | downstream |
| `502` | 下游网关错误 | Bad Gateway | downstream |
| `503` | 下游服务不可用 | Service Unavailable | downstream |
| `504` | 下游网关超时 | Gateway Timeout | downstream |
| `6001` | 字段映射失败 | Field Mapping Failed | internal |
| `6002` | JSON 解析失败 | JSON Parse Failed | internal |
| `6003` | 编排执行超时 | Orchestration Timeout | internal |
| `6004` | 连接器版本未找到 | Connector Version Not Found | internal |

> **示例**

```json
// 下游调用失败
{ "code": "503", "messageZh": "下游服务不可用", "messageEn": "Service Unavailable", "downstreamStatus": 503 }

// 内部错误
{ "code": "6001", "messageZh": "字段映射失败", "messageEn": "Field Mapping Failed", "cause": "source 字段 ${node_1.msgId} 不存在" }
```

#### 4.3.5 httpInputDef

> HTTP 入参，按传输位置分为 header / query / body 三段。

> **Def**

```json
{
  "$id": "urn:openapp:schema:httpInputDef:v1",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "protocol": { "type": "string", "const": "HTTP" },
    "header": { "$ref": "#/definitions/jsonObjectDef" },
    "query":  { "$ref": "#/definitions/jsonObjectDef" },
    "body":   { "$ref": "#/definitions/jsonObjectDef" }
  },
  "required": ["protocol"],
  "anyOf": [
    { "required": ["header"] },
    { "required": ["query"] },
    { "required": ["body"] }
  ]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 说明 |
|-----------|------|:----:|------|
| protocol | string | ✅ | 协议标识，固定为 `HTTP` |
| header | object | ❌ ⚡ | 请求头参数定义 |
| query | object | ❌ ⚡ | Query 参数定义 |
| body | object | ❌ ⚡ | 请求体参数定义 |

⚡ = anyOf：必须至少声明 header / query / body 其中之一。

> **示例**

```json
{
  "protocol": "HTTP",
  "query": {
    "type": "object",
    "properties": { "page": { "type": "integer", "description": "页码" } },
    "required": ["page"]
  },
  "body": {
    "type": "object",
    "properties": {
      "receiver": { "type": "string", "description": "接收者 ID" },
      "content":  { "type": "string", "description": "消息内容" }
    },
    "required": ["receiver", "content"]
  }
}
```

#### 4.3.6 httpOutputDef

> HTTP 出参，按传输位置分为 header / body 两段。

> **Def**

```json
{
  "$id": "urn:openapp:schema:httpOutputDef:v1",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "protocol": { "type": "string", "const": "HTTP" },
    "header": { "$ref": "#/definitions/jsonObjectDef" },
    "body":   { "$ref": "#/definitions/jsonObjectDef" }
  },
  "required": ["protocol"],
  "anyOf": [
    { "required": ["header"] },
    { "required": ["body"] }
  ]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 说明 |
|-----------|------|:----:|------|
| protocol | string | ✅ | 协议标识，固定为 `HTTP` |
| header | object | ❌ ⚡ | 响应头字段定义 |
| body | object | ❌ ⚡ | 响应体字段定义 |

⚡ = anyOf：必须至少声明 header / body 其中之一。

> **示例**

```json
{
  "protocol": "HTTP",
  "header": {
    "type": "object",
    "properties": { "X-Request-Id": { "type": "string", "description": "请求追踪 ID" } }
  },
  "body": {
    "type": "object",
    "properties": {
      "msgId": { "type": "string", "description": "消息 ID" },
      "code":  { "type": "integer", "description": "状态码" }
    }
  }
}
```

#### 4.3.7 connectorVersionConfigDef

> 连接器配置完整定义。供 §5 `connectionConfig`（存储）和 §4.3.10 `connectorNodeDataDef.connectorVersionConfig`（快照）共同引用，连接器配置的唯一定义源。

> **Def**

```json
{
  "$id": "urn:openapp:schema:connectorVersionConfigDef:v1",
  "type": "object",
  "additionalProperties": false,
  "description": "连接器配置完整定义。存储在 connector_version_t.connection_config，编排快照时全量复制",
  "properties": {
    "labelCn":       { "type": "string", "description": "连接器中文标签" },
    "labelEn":       { "type": "string", "description": "连接器英文标签" },
    "protocol":      { "type": "string", "enum": ["HTTP"] },
    "protocolConfig": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "url":    { "type": "string" },
        "method": { "type": "string", "enum": ["GET", "POST", "PUT", "DELETE", "PATCH"] }
      },
      "required": ["url", "method"]
    },
    "authConfigs": {
      "type": "array",
      "minItems": 1,
      "items": { "$ref": "#/definitions/authConfigDef" },
      "description": "认证配置列表。含 sensitive 标记字段"
    },
    "input": {
      "$ref": "#/definitions/httpInputDef",
      "description": "API 入参 Schema 声明。纯结构定义（header/query/body 三段式）"
    },
    "output": {
      "$ref": "#/definitions/httpOutputDef",
      "description": "API 出参 Schema 声明。供下游节点引用字段提示"
    },
    "timeoutMs": {
      "type": "integer",
      "default": 3000,
      "minimum": 1000,
      "maximum": 300000,
      "description": "单次调用超时（毫秒），默认 3000"
    },
    "rateLimitConfig": {
      "$ref": "#/definitions/rateLimitConfigDef",
      "description": "出站限流配置"
    }
  },
  "required": ["protocol", "protocolConfig"]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 说明 |
|-----------|------|:----:|------|
| labelCn | string | ❌ | 连接器中文标签 |
| labelEn | string | ❌ | 连接器英文标签 |
| protocol | string | ✅ | 协议类型，MVP 仅 `HTTP` |
| protocolConfig | object | ✅ | 协议配置（url + method） |
| authConfigs[] | array | ❌ | 认证配置列表，minItems 1。每项见 §4.3.2 |
| input | object | ❌ | API 入参 Schema 声明，见 §4.3.5 |
| output | object | ❌ | API 出参 Schema 声明，见 §4.3.6 |
| timeoutMs | integer | ❌ | 单次调用超时（毫秒），默认 3000，范围 1000~300000 |
| rateLimitConfig | object | ❌ | 出站限流配置，见 §4.3.3 |

> **示例** — 连接器完整配置：

```json
{
  "labelCn": "创建订单",
  "labelEn": "Create Order",
  "protocol": "HTTP",
  "protocolConfig": {
    "url": "https://api.example.com/v2/order/create",
    "method": "POST"
  },
  "authConfigs": [
    {
      "type": "SOA",
      "header": {
        "type": "object",
        "properties": {
          "X-Soa-Token": {
            "type": "string",
            "required": true,
            "sensitive": true,
            "value": "${$.system.env.soaToken}",
            "description": "SOA 认证令牌。值来源：凭据库"
          }
        }
      }
    }
  ],
  "input": {
    "protocol": "HTTP",
    "body": {
      "type": "object",
      "properties": {
        "receiver": { "type": "string", "required": true, "description": "接收者 ID" },
        "content":  { "type": "string", "required": true, "description": "消息内容" }
      },
      "required": ["receiver", "content"]
    }
  },
  "output": {
    "protocol": "HTTP",
    "body": {
      "type": "object",
      "properties": {
        "msgId": { "type": "string", "description": "消息 ID" },
        "status": { "type": "string", "description": "处理状态" }
      }
    }
  },
  "timeoutMs": 8000,
  "rateLimitConfig": { "maxQps": 50, "maxConcurrency": 10 }
}
```


#### 4.3.8 nodeDataBaseDef

> 所有节点 data 的公共基类。`type` 为**业务节点类型**（引擎执行路由依据），与 React Flow 框架层的 `node.type`（渲染组件名）**分属两层**。

> **两层 type 对照**：以下为 `node.data.type` 的 9 个枚举值与对应 Data Schema 的完整映射。

| `node.data.type` | Data Schema | 说明 |
|------|------|------|
| `trigger` | triggerNodeDataDef | 触发器。激活方式由独立字段 `triggerType`（http / manual）区分 |
| `connector` | connectorNodeDataDef | |
| `dataProcessor` | dataProcessorNodeDataDef | ⚠️ 与框架层 `node.type="data_processor"` 的命名差异遵循 §2.1：框架 snake_case → 业务 camelCase |
| `exit` | exitNodeDataDef | |
| `loopV2` | loopNodeDataDef | |
| `parallel` | parallelNodeDataDef | |
| `conditionBranch` | conditionBranchNodeDataDef | |
| `errorHandler` | errorHandlerNodeDataDef | |
| `text` | textNodeDataDef | 纯渲染标记节点，不参与 DAG 执行 |

> **Def**

```json
{
  "$id": "urn:openapp:schema:nodeDataBaseDef:v2",
  "type": "object",
  "properties": {
    "type": {
      "type": "string",
      "enum": ["trigger", "connector", "dataProcessor", "exit", "loopV2", "parallel", "conditionBranch", "errorHandler", "text"],
      "description": "业务节点类型。引擎按此路由执行逻辑。与 React Flow node.type（渲染组件名）分离"
    },
    "labelCn": { "type": "string", "description": "节点中文标签" },
    "labelEn": { "type": "string", "description": "节点英文标签" },
    "structConfig": {
      "type": "object",
      "description": "DAG 拓扑配置。用于 React Flow 画布中构建、解析和运行流程 DAG 结构：\n- 结构节点（循环/并行/条件分支/错误处理）及文本标记节点通过此字段声明分组归属关系\n- 典型字段：loopV2GroupId / loopV2Role（循环/错误处理）；parallelGroupId / parallelRole / parallelBranchId / parallelBranchIndex（并行/条件分支）\n- 嵌套场景：parentLoopV2GroupId / parentParallelGroupId 等\n- 不参与运行时数据传递，仅用于前端布局与引擎拓扑解析"
    }
  },
  "required": ["type"]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 说明 |
|-----------|------|:----:|------|
| type | string | ✅ | 业务节点类型：`trigger` / `connector` / `script` / `dataProcessor` / `exit` / `loopV2` / `parallel` / `conditionBranch` / `errorHandler` / `text` |
| labelCn | string | ❌ | 节点中文标签 |
| labelEn | string | ❌ | 节点英文标签 |
| structConfig | object | ❌ | DAG 拓扑配置。结构节点和文本标记节点的分组归属关系，用于前端布局和引擎拓扑解析 |

> **示例**

```json
{ "type": "trigger", "labelCn": "接收请求", "labelEn": "Receive Request" }
```

#### 4.3.9 triggerNodeDataDef

> 继承 `nodeDataBaseDef`（type / labelCn / labelEn），扩展触发器独有字段。`node.data.type` 固定为 `"trigger"`，激活方式由独立字段 `triggerType` 区分。

> **Def**

```json
{
  "allOf": [
    { "$ref": "#/definitions/nodeDataBaseDef" },
    {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "triggerType": {
          "type": "string",
          "enum": ["http", "manual"],
          "description": "触发器激活方式。http=对外暴露 HTTP 端点，manual=仅内部/手动触发"
        },
        "authConfigs": {
          "type": "array",
          "minItems": 1,
          "items": { "$ref": "#/definitions/authConfigDef" },
          "description": "认证配置列表。支持多选组合，运行时按序校验。至少一种认证方式"
        },
        "input": { "$ref": "#/definitions/httpInputDef" }
      },
      "required": ["triggerType"]
    },
    {
      "if": { "properties": { "triggerType": { "const": "http" } }, "required": ["triggerType"] },
      "then": { "required": ["authConfigs", "input"] }
    },
    {
      "if": { "properties": { "triggerType": { "const": "manual" } }, "required": ["triggerType"] },
      "then": { "properties": { "authConfigs": false, "input": false } }
    }
  ]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 来源 | 说明 |
|-----------|------|:----:|:--:|------|
| type | string | ✅ | 基类 | `"trigger"`。固定值，区分于其他 8 种业务节点类型 |
| triggerType | string | ✅ | 独有 | 触发器激活方式：`http`（对外暴露 HTTP 端点，需配 authConfigs + input）/ `manual`（仅内部触发，无需认证和入参声明） |
| labelCn | string | ❌ | 基类 | 节点中文标签 |
| labelEn | string | ❌ | 基类 | 节点英文标签 |
| authConfigs[] | array | ❌ ⚡ | 独有 | 认证配置列表，minItems 1。triggerType=http 时必填。每项见 §4.3.2 |
| input | object | ❌ ⚡ | 独有 | 入参声明。triggerType=http 时必填，见 §4.3.5 |

| rateLimitConfig | object | ❌ | 独有 | 限流配置，见 §4.3.3 |

⚡ = `triggerType="http"` 时必填。

> **示例** — HTTP 触发，SYSTOKEN 认证：

```json
{
  "type": "trigger",
  "triggerType": "http",
  "labelCn": "接收请求",
  "authConfigs": [
    {
      "type": "SYSTOKEN",
      "header": {
        "type": "object",
        "properties": {
          "X-Sys-Token": { "type": "string", "required": true, "sensitive": true }
        }
      }
    }
  ],
  "input": {
    "protocol": "HTTP",
    "body": {
      "type": "object",
      "properties": {
        "sender":  { "type": "string", "required": true },
        "content": { "type": "string", "required": true }
      }
    }
  },
  "rateLimitConfig": { "maxQps": 100 }
}
```

#### 4.3.10 connectorNodeDataDef

> 继承 `nodeDataBaseDef`（type / labelCn / labelEn / structConfig），扩展连接器独有字段。v2 重构：新增 `connectorId`（归属标识）+ `connectorVersionConfig`（§5 connectionConfig 完整快照，选版本时立即抓取），实现编排自包含——不再需要跨表 JOIN 即可理解连接器节点的完整 API 画像。移除节点层 `rateLimitConfig`（连接器出站限流由 `connectorVersionConfig` 快照中的连接器级配置管控）。

> **Def**

```json
{
  "allOf": [
    { "$ref": "#/definitions/nodeDataBaseDef" },
    {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "connectorId": {
          "type": "string",
          "pattern": "^[1-9][0-9]{15,19}$",
          "description": "引用的连接器 ID"
        },
        "connectorVersionId": {
          "type": "string",
          "pattern": "^[1-9][0-9]{15,19}$",
          "description": "引用的连接器版本 ID，必须为已发布版本。更换版本时 connectorVersionConfig 全量替换为新版本的 connectionConfig"
        },
        "connectorVersionConfig": {
          "$ref": "#/definitions/connectorVersionConfigDef",
          "description": "连接器配置完整快照。选择连接器版本时立即从 connection_config 抓取（排除 timeoutMs 和 rateLimitConfig）。编排自包含：无需查看连接器表即可理解 API 画像"
        },
        "timeoutMs": {
          "type": "integer",
          "minimum": 0,
          "maximum": 300000,
          "default": 0,
          "description": "节点超时（毫秒）。0 = 不限制，走系统默认上限。运行时取 min(该值, 系统上限)"
        },
        "input": {
          "type": "object",
          "properties": {
            "header": { "$ref": "#/definitions/jsonObjectDef" },
            "query":  { "$ref": "#/definitions/jsonObjectDef" },
            "body":   { "$ref": "#/definitions/jsonObjectDef" }
          },
          "description": "字段映射。properties 中通过 value 字段声明映射表达式（遵循 §3 值表达式体系），字段名与 connectorVersionConfig.input 中的声明对应"
        }
      },
      "required": ["connectorId", "connectorVersionId", "connectorVersionConfig"]
    }
  ]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 来源 | 说明 |
|-----------|------|:----:|:--:|------|
| type | string | ✅ | 基类 | `"connector"` |
| labelCn | string | ❌ | 基类 | 节点中文标签 |
| labelEn | string | ❌ | 基类 | 节点英文标签 |
| connectorId | string | ✅ | 独有 | 引用的连接器 ID |
| connectorVersionId | string | ✅ | 独有 | 引用的连接器版本 ID。更换版本时 connectorVersionConfig 全量替换 |
| connectorVersionConfig | object | ✅ | 独有 | 连接器配置完整快照 |
| timeoutMs | integer | ❌ | 独有 | 节点超时（ms）。0=不限制走系统默认。范围 0~300000 |
| input | object | ❌ | 独有 | 字段映射。value 遵循 §3 值表达式体系 |

> **四层职责**

| 层 | 字段 | 管什么 | 何时确定 | 可改？ |
|:---:|------|------|------|:---:|
| 身份 | `connectorId` + `connectorVersionId` | 归属和版本引用 | 设计者选版本时 | 换版本则更新 |
| 快照 | `connectorVersionConfig` | API 完整画像（url / method / auth / 入参声明 / 出参声明） | 选版本时立即从 connection_config 抓取 | ❌ 只读 |
| 定制 | `timeoutMs` | 这条流的运行时超时 | 编排设计时 | ✅ |
| 映射 | `input` | 字段值从哪来（value 表达式） | 编排设计时 | ✅ |

> **示例** — 完整的连接器节点配置，展示快照自包含 + 映射分离：

```json
{
  "type": "connector",
  "labelCn": "发送消息",
  "labelEn": "Send Message",
  "connectorId": "1234567890123456000",
  "connectorVersionId": "1234567890123456789",
  "connectorVersionConfig": {
    "labelCn": "创建订单",
    "labelEn": "Create Order",
    "protocol": "HTTP",
    "protocolConfig": {
      "url": "https://api.example.com/v2/order/create",
      "method": "POST"
    },
    "authConfigs": [
      {
        "type": "SOA",
        "header": {
          "type": "object",
          "properties": {
            "X-Soa-Token": {
              "type": "string",
              "required": true,
              "sensitive": true,
              "value": "${$.system.env.soaToken}",
              "description": "SOA 认证令牌。值来源：凭据库"
            }
          }
        }
      }
    ],
    "input": {
      "protocol": "HTTP",
      "body": {
        "type": "object",
        "properties": {
          "receiver": { "type": "string", "required": true, "description": "接收者 ID" },
          "content":  { "type": "string", "required": true, "description": "消息内容" }
        },
        "required": ["receiver", "content"]
      }
    },
    "output": {
      "protocol": "HTTP",
      "body": {
        "type": "object",
        "properties": {
          "msgId": { "type": "string", "description": "消息 ID" },
          "status": { "type": "string", "description": "处理状态" }
        }
      }
    }
  },
  "timeoutMs": 8000,
  "input": {
    "body": {
      "type": "object",
      "properties": {
        "receiver": { "type": "string", "required": true, "value": "${$.node.trigger.input.body.sender}" },
        "content":  { "type": "string", "required": true, "value": "${$.node.trigger.input.body.content}" }
      }
    }
  }
}
```

#### 4.3.11 exitNodeDataDef

> 继承 `nodeDataBaseDef`（type / labelCn / labelEn / structConfig），扩展出口独有字段。

> **Def**

```json
{
  "allOf": [
    { "$ref": "#/definitions/nodeDataBaseDef" },
    {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "output": {
          "type": "object",
          "properties": {
            "header": { "$ref": "#/definitions/jsonObjectDef" },
            "body":   { "$ref": "#/definitions/jsonObjectDef" }
          }
        }
      },
      "required": ["output"]
    }
  ]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 来源 | 说明 |
|-----------|------|:----:|:--:|------|
| type | string | ✅ | 基类 | `"exit"` |
| labelCn | string | ❌ | 基类 | 节点中文标签 |
| labelEn | string | ❌ | 基类 | 节点英文标签 |
| output | object | ✅ | 独有 | 字段映射，value 遵循 §3 值表达式体系 |

> **示例**

```json
{
  "type": "exit",
  "labelCn": "返回结果",
  "output": {
    "body": {
      "type": "object",
      "properties": {
        "msgId":  { "type": "string",  "value": "${$.node.conn_1.output.body.msgId}" },
        "status": { "type": "string",  "value": "${$.constant:success}" }
      }
    }
  }
}
```

#### 4.3.12 dataProcessorNodeDataDef

> 继承 `nodeDataBaseDef`（type / labelCn / labelEn / structConfig），扩展数据处理器独有字段。无协议包袱，纯内存运行。

> **Def**

```json
{
  "allOf": [
    { "$ref": "#/definitions/nodeDataBaseDef" },
    {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "output": { "$ref": "#/definitions/jsonObjectDef" }
      },
      "required": ["output"]
    }
  ]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 来源 | 说明 |
|-----------|------|:----:|:--:|------|
| type | string | ✅ | 基类 | `"dataProcessor"` |
| labelCn | string | ❌ | 基类 | 节点中文标签 |
| labelEn | string | ❌ | 基类 | 节点英文标签 |
| output | object | ✅ | 独有 | 输出字段定义。复用 jsonObjectDef，每个字段的 value 遵循 §3 值表达式体系（支持静态值/引用字段/函数输出） |

> **示例**

```json
{
  "type": "dataProcessor",
  "labelCn": "格式化输出",
  "output": {
    "type": "object",
    "properties": {
      "upperName": {
        "type": "string",
        "value": "${$.system.fn.upper($.node.conn_1.output.body.name)}",
        "description": "转大写的用户名"
      },
      "id": {
        "type": "string",
        "value": "${$.node.conn_1.output.body.msgId}"
      },
      "status": {
        "type": "string",
        "value": "${$.constant:processed}"
      }
    },
    "required": ["upperName"]
  }
}
```

#### 4.3.13 scriptNodeDataDef

> 继承 `nodeDataBaseDef`（type / labelCn / labelEn / structConfig），扩展脚本节点独有字段。`script` 存储 JS 源码，运行时由 GraalJS 沙箱执行，`function main(ctx) { ... return ... }`。`output` 声明出参结构供下游节点引用提示。详细设计见 [plan-script.md](./plan-script.md)。

> **Def**

```json
{
  "allOf": [
    { "$ref": "#/definitions/nodeDataBaseDef" },
    {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "script": {
          "type": "string",
          "maxLength": 10000,
          "description": "标准 JS 函数声明 function main(ctx) { ... return ... }。ctx 为上游全量数据，return 显式输出"
        },
        "output": {
          "$ref": "#/definitions/jsonObjectDef",
          "description": "出参 Schema 声明（选填），供下游节点引用字段提示"
        },
        "timeout": {
          "type": "integer",
          "minimum": 1,
          "maximum": 30,
          "default": 5,
          "description": "脚本执行超时秒数，默认 5s"
        },
        "description": {
          "type": "string",
          "description": "节点说明"
        }
      },
      "required": ["script"]
    }
  ]
}
```

> **字段说明**

| JSON 字段 | 类型 | 必填 | 来源 | 说明 |
|-----------|------|:----:|:--:|------|
| type | string | ✅ | 基类 | `"script"` |
| labelCn | string | ❌ | 基类 | 节点中文标签 |
| labelEn | string | ❌ | 基类 | 节点英文标签 |
| script | string | ✅ | 独有 | 最大 10000 字符。ctx 为上游节点数据 Map，return 为节点 output |
| output | object | ❌ | 独有 | 复用 jsonObjectDef，供下游引用提示，不参与运行时校验 |
| timeout | integer | ❌ | 独有 | 1~30，默认 5 |
| description | string | ❌ | 独有 | 节点说明 |

> **示例**

```json
{
  "type": "script",
  "labelCn": "数据清洗与聚合",
  "script": "function main(ctx) {\n  const users = ctx.conn_1.output.body.data.users;\n  const total = users.length;\n  const avgAge = users.reduce((s,u) => s + u.age, 0) / total;\n  return { total, avgAge };\n}",
  "output": {
    "type": "object",
    "properties": {
      "total":  { "type": "number", "description": "用户总数" },
      "avgAge": { "type": "number", "description": "平均年龄" }
    }
  },
  "timeout": 5
}
```

#### 4.3.14 errorHandlerNodeDataDef

> 继承 `nodeDataBaseDef`（含 `structConfig`）。错误处理结构的**入口主节点**，每流最多 1 个，仅作用于连接器节点（try-catch 语义）。先选错误类型，再选处理策略：同一错误类型仅一种策略，不同错误类型可配不同策略。

> **Def**

```json
{
  "$id": "urn:openapp:schema:errorHandlerNodeDataDef:v2",
  "allOf": [
    { "$ref": "#/definitions/nodeDataBaseDef" },
    {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "errors": {
          "type": "object",
          "minProperties": 1,
          "description": "错误类型 → 处理策略映射。key 为错误类型枚举，value 为策略配置",
          "patternProperties": {
            "^(all|timeout|connection_error|other)$": {
              "type": "object",
              "additionalProperties": false,
              "properties": {
                "strategy": {
                  "type": "string",
                  "enum": ["retry", "ignore", "terminate"],
                  "description": "处理策略。retry=重试；ignore=忽略并继续；terminate=终止执行"
                },
                "retries": {
                  "type": "integer",
                  "minimum": 1,
                  "maximum": 5,
                  "description": "重试次数，仅 strategy=retry 时必填，范围 1~5"
                },
                "interval": {
                  "type": "integer",
                  "minimum": 1,
                  "maximum": 300,
                  "description": "重试间隔（秒），仅 strategy=retry 时必填，范围 1~300"
                }
              },
              "required": ["strategy"],
              "if": { "properties": { "strategy": { "const": "retry" } } },
              "then": { "required": ["retries", "interval"] }
            }
          },
          "additionalProperties": false
        }
      },
      "required": ["errors"],
      "allOf": [
        {
          "description": "all 与其他错误类型互斥",
          "if": { "required": ["errors"], "properties": { "errors": { "required": ["all"] } } },
          "then": { "properties": { "errors": { "maxProperties": 1 } } }
        }
      ]
    }
  ]
}
```

> **字段说明**

| 字段 | 类型 | 必填 | 来源 | 说明 |
|------|------|:----:|:--:|------|
| type | string | ✅ | 基类 | `"errorHandler"` |
| labelCn | string | ❌ | 基类 | 节点中文标签 |
| labelEn | string | ❌ | 基类 | 节点英文标签 |
| structConfig | object | ❌ | 基类 | `loopV2GroupId` 分组关联 |
| errors | object | ✅ | 独有 | 错误类型→策略映射。key 取值：`all` / `timeout` / `connection_error` / `other`。`all` 与其他类型互斥（选了 `all` 则不可再选其他） |
| errors.{type}.strategy | string | ✅ | 独有 | 处理策略：`retry`（重试）/ `ignore`（忽略并继续）/ `terminate`（终止执行） |
| errors.{type}.retries | integer | ⚡ | 独有 | 重试次数 1~5，仅 strategy=retry 时必填 |
| errors.{type}.interval | integer | ⚡ | 独有 | 重试间隔（秒）1~300，仅 strategy=retry 时必填 |

⚡ = strategy=retry 时必填。

> **示例 — 多错误类型不同策略**

```json
{
  "type": "errorHandler",
  "labelCn": "错误处理",
  "labelEn": "Error Handler",
  "structConfig": { "loopV2GroupId": "err-1" },
  "errors": {
    "timeout": { "strategy": "retry", "retries": 3, "interval": 10 },
    "connection_error": { "strategy": "retry", "retries": 2, "interval": 30 },
    "other": { "strategy": "ignore" }
  }
}
```

> **示例 — 全部错误统一重试**

```json
{
  "type": "errorHandler",
  "labelCn": "错误处理",
  "labelEn": "Error Handler",
  "structConfig": { "loopV2GroupId": "err-1" },
  "errors": {
    "all": { "strategy": "retry", "retries": 4, "interval": 20 }
  }
}
```

> **错误处理完整示例**（5 nodes + 7 edges）

```json
{
  "nodes": [
    { "id":"trigger-1",     "type":"trigger",      "position":{"x":250,"y":50},  "data":{ "type":"trigger",      "labelCn":"触发器",         "labelEn":"Trigger",        "structConfig":{} } },
    { "id":"err-1",         "type":"error-handler", "position":{"x":250,"y":160}, "data":{ "type":"errorHandler", "labelCn":"错误处理",       "labelEn":"Error Handler",  "structConfig":{ "loopV2GroupId":"err-1" }, "errors":{ "timeout":{ "strategy":"retry","retries":3,"interval":10 }, "connection_error":{ "strategy":"retry","retries":3,"interval":10 } } } },
    { "id":"err-region-1",  "type":"text",          "position":{"x":-10,"y":300}, "data":{ "type":"text",          "labelCn":"错误处理区域",   "labelEn":"Error Region",   "structConfig":{ "loopV2GroupId":"err-1","loopV2Role":"region" } } },
    { "id":"err-start-1",   "type":"text",          "position":{"x":510,"y":300}, "data":{ "type":"text",          "labelCn":"错误处理开始",   "labelEn":"Error Start",    "structConfig":{ "loopV2GroupId":"err-1","loopV2Role":"start" } } },
    { "id":"conn-1",        "type":"connector",     "position":{"x":510,"y":400}, "data":{ "type":"connector",     "labelCn":"被保护节点",     "connectorId":"1234567890123456000", "connectorVersionId":"1234567890123456789", "connectorVersionConfig":{/* 连接器完整配置，简写 */}, "input":{}, "structConfig":{ "parentLoopV2GroupId":"err-1","parentLoopV2Role":"right-column-node" } } },
    { "id":"err-end-1",     "type":"text",          "position":{"x":510,"y":560}, "data":{ "type":"text",          "labelCn":"错误处理结束",   "labelEn":"Error End",      "structConfig":{ "loopV2GroupId":"err-1","loopV2Role":"end" } } },
    { "id":"err-break-1",   "type":"text",          "position":{"x":250,"y":640}, "data":{ "type":"text",          "labelCn":"错误处理跳出",   "labelEn":"Error Break",    "structConfig":{ "loopV2GroupId":"err-1","loopV2Role":"break" } } },
    { "id":"end-1",         "type":"exit",          "position":{"x":250,"y":800}, "data":{ "type":"exit",          "labelCn":"结束",           "labelEn":"End",            "output":{}, "structConfig":{} } }
  ],
  "edges": [
    { "id":"e-t-err",         "source":"trigger-1",  "target":"err-1",        "type":"smoothstep", "data":{ "businessType":"default",    "connectionMode":"serial" } },
    { "id":"e-err-region",    "source":"err-1",      "target":"err-region-1", "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-err-start",     "source":"err-1",      "target":"err-start-1",  "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-start-conn",    "source":"err-start-1", "target":"conn-1",       "type":"smoothstep", "data":{ "businessType":"default",    "connectionMode":"serial" } },
    { "id":"e-conn-end",      "source":"conn-1",      "target":"err-end-1",    "type":"smoothstep", "data":{ "businessType":"default",    "connectionMode":"serial" } },
    { "id":"e-region-break",  "source":"err-region-1","target":"err-break-1", "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-end-break",     "source":"err-end-1",   "target":"err-break-1", "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-break-end",     "source":"err-break-1", "target":"end-1",       "type":"smoothstep", "data":{ "businessType":"default",    "connectionMode":"serial" } }
  ]
}
```

> **引擎解析逻辑**：
> 1. 错误处理节点仅包裹连接器节点（try-catch 语义），触发器/数据处理/数据输出节点不受影响
> 2. 包裹区域内连接器节点执行失败（HTTP 非 2xx / 超时 / 连接错误）且匹配 errors 中定义的错误类型 → 按对应 strategy 执行：
>    - **retry**：按 errors.{type}.retries 次数和 errors.{type}.interval 间隔循环重试，全部重试失败后标记节点为"失败"
>    - **ignore**：捕获错误后标记节点为"已忽略"并继续执行下游节点
>    - **terminate**：捕获错误后终止整个连接流执行
> 3. 所有被保护节点正常完成 → 错误处理节点透明通过，流继续下游
> 4. 嵌套错误处理：内层 retry 全部失败且未终止 → 冒泡到外层错误处理节点取值

#### 4.3.15 loopNodeDataDef

> 继承 `nodeDataBaseDef`（含 `structConfig`）。循环结构的**入口主节点**。

> **设计说明**：一个完整的循环结构由 **1 个主节点（loop-v2）+ 4 个 text 标记节点 + 7 条 edge** 组成。前端插入时一次性生成全量 nodes + edges 并持久化。`structConfig.loopV2GroupId` 将这些分散的节点关联为一个逻辑整体——主节点和所有标记节点共享同一个 `loopV2GroupId`。引擎执行时通过 edge 拓扑找到 `loop-start → loop-end` 之间的子图作为循环体迭代执行。

> **Def**

```json
{
  "allOf": [{ "$ref": "#/definitions/nodeDataBaseDef" }]
}
```

> **structConfig 字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| loopV2GroupId | string | 循环分组 ID，等于主节点 ID。主节点和 4 个标记节点通过此字段关联 |
| parentLoopV2GroupId | string? | 嵌套在父级循环右侧链路时，记录父循环 ID |
| parentLoopV2Role | string? | 属于父级循环右侧链路时，值为 `"right-column-node"` |
| parentParallelGroupId | string? | 嵌套在并行/条件分支内部时，记录父分支分组 ID |
| parentParallelBranchId | string? | 嵌套在并行/条件分支内部时，记录父分支 ID |

> **示例**

```json
{
  "type": "loopV2",
  "labelCn": "遍历处理",
  "labelEn": "Loop",
  "structConfig": {
    "loopV2GroupId": "loop-1"
  }
}
```

> **循环结构的 5 个 node + 7 条 edge 完整示例**（来源：前端 FlowCanvas 持久化数据，已对齐 Schema 命名）

```json
{
  "nodes": [
    {
      "id": "trigger-1",
      "type": "trigger",
      "position": { "x": 250, "y": 50 },
      "data": {
        "type": "trigger",
        "labelCn": "触发器",
        "labelEn": "Trigger",
        "structConfig": {}
      }
    },
    {
      "id": "loop-1",
      "type": "loop-v2",
      "position": { "x": 250, "y": 160 },
      "data": {
        "type": "loopV2",
        "labelCn": "循环节点",
        "labelEn": "Loop",
        "structConfig": {
          "loopV2GroupId": "loop-1"
        }
      }
    },
    {
      "id": "loop-region-1",
      "type": "text",
      "position": { "x": -10, "y": 300 },
      "data": {
        "type": "text",
        "labelCn": "循环区域",
        "labelEn": "Loop Region",
        "structConfig": {
          "loopV2GroupId": "loop-1",
          "loopV2Role": "region"
        }
      }
    },
    {
      "id": "loop-start-1",
      "type": "text",
      "position": { "x": 510, "y": 300 },
      "data": {
        "type": "text",
        "labelCn": "循环开始",
        "labelEn": "Loop Start",
        "structConfig": {
          "loopV2GroupId": "loop-1",
          "loopV2Role": "start"
        }
      }
    },
    {
      "id": "loop-end-1",
      "type": "text",
      "position": { "x": 510, "y": 500 },
      "data": {
        "type": "text",
        "labelCn": "循环结束",
        "labelEn": "Loop End",
        "structConfig": {
          "loopV2GroupId": "loop-1",
          "loopV2Role": "end"
        }
      }
    },
    {
      "id": "loop-break-1",
      "type": "text",
      "position": { "x": 250, "y": 580 },
      "data": {
        "type": "text",
        "labelCn": "循环跳出",
        "labelEn": "Loop Break",
        "structConfig": {
          "loopV2GroupId": "loop-1",
          "loopV2Role": "break"
        }
      }
    },
    {
      "id": "end-1",
      "type": "exit",
      "position": { "x": 250, "y": 740 },
      "data": {
        "type": "exit",
        "labelCn": "结束",
        "labelEn": "End",
        "output": {},
        "structConfig": {}
      }
    }
  ],
  "edges": [
    { "id":"edge-trigger-loop",   "source":"trigger-1",     "target":"loop-1",        "type":"smoothstep", "data":{ "businessType":"default",    "connectionMode":"serial" } },
    { "id":"edge-loop-region",    "source":"loop-1",        "target":"loop-region-1", "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"edge-loop-start",     "source":"loop-1",        "target":"loop-start-1",  "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"edge-start-end",      "source":"loop-start-1",  "target":"loop-end-1",    "type":"smoothstep", "data":{ "businessType":"loop_entry", "iterationVar":"item" } },
    { "id":"edge-region-break",   "source":"loop-region-1", "target":"loop-break-1",  "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"edge-end-break",      "source":"loop-end-1",    "target":"loop-break-1",  "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"edge-break-end",      "source":"loop-break-1",  "target":"end-1",         "type":"smoothstep", "data":{ "businessType":"default",    "connectionMode":"serial" } }
  ]
}
```

> **引擎解析逻辑**：
> 1. 过滤 `node.type === "text"` 的标记节点，过滤 `edge.data.isStructural === true` 的辅助边
> 2. 剩余节点中，从 `loop-start-1 → loop-end-1` 之间的子图即为循环体
> 3. 对循环体中的每个节点按 Kahn 算法拓扑排序后，按 `iterationVar` 声明的变量名进行迭代执行

#### 4.3.16 textNodeDataDef

> 继承 `nodeDataBaseDef`（含 `structConfig`）。**结构节点的标记节点**——循环/错误处理中的"循环区域""循环开始""循环结束""循环跳出"，并行/条件分支中的"分支开始""分支结束""并行合并"。

**循环/错误处理** — `loopV2GroupId` + `loopV2Role`：

| loopV2Role | 含义 | 位置 |
|-----------|------|------|
| `region` | 左侧辅助说明节点 | 主节点左下方 |
| `start` | 右侧可编辑链路入口 | 引擎从此边的 target 开始识别循环体子图 |
| `end` | 右侧可编辑链路出口 | 引擎在此结束循环体子图 |
| `break` | 结构最终汇合出口 | 左侧路径和右侧路径在此汇合后进入下游 |

**并行/条件分支** — `parallelGroupId` + `parallelRole` + `parallelBranchId` + `parallelBranchIndex`：

| parallelRole | 含义 | 说明 |
|------------|------|------|
| `branch-start` | 分支入口 | 显示删除按钮，引擎从此边的 target 开始识别分支子图 |
| `branch-end` | 分支出口 | 引擎在此结束分支子图 |
| `merge` | 所有分支汇合点 | 各分支结束后在此汇合，进入下游 |

> **Def**

```json
{
  "allOf": [{ "$ref": "#/definitions/nodeDataBaseDef" }]
}
```

> **示例**

```json
// 循环区域说明
{ "type":"text", "labelCn":"循环区域", "structConfig":{ "loopV2GroupId":"loop-1", "loopV2Role":"region" } }

// 循环开始（引擎识别循环体入口）
{ "type":"text", "labelCn":"循环开始", "structConfig":{ "loopV2GroupId":"loop-1", "loopV2Role":"start" } }

// 分支开始（引擎识别分支子图入口）
{ "type":"text", "labelCn":"分支1开始", "structConfig":{ "parallelGroupId":"p-1", "parallelRole":"branch-start", "parallelBranchId":"b1", "parallelBranchIndex":1 } }

// 并行合并（引擎等待所有分支完成后汇合）
{ "type":"text", "labelCn":"并行合并", "structConfig":{ "parallelGroupId":"p-1", "parallelRole":"merge" } }
```

#### 4.3.17 parallelNodeDataDef

> 继承 `nodeDataBaseDef`（含 `structConfig`）。并行处理结构的**入口主节点**。一个完整并行结构由 **1 个主节点（parallel）+ 默认 2 组分支标记节点 ×2（start+end）+ 1 个 merge 标记节点 = 6 个 node + 8 条 edge** 组成。每组分支的 start→end 之间是可编辑的子图。引擎执行时通过 edge 拓扑识别各分支，按 `connectionMode=parallel` 并发执行。

> **Def**

```json
{
  "allOf": [{ "$ref": "#/definitions/nodeDataBaseDef" }]
}
```

> **structConfig 字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| parallelGroupId | string | 并行分组 ID，等于主节点 ID。主节点和所有标记节点通过此字段关联 |
| parallelRole | string | 固定为 `"root"`，标识为并行结构主节点 |
| parentParallelGroupId | string? | 嵌套在另一个并行/条件分支内部时，记录父分支分组 ID |
| parentParallelBranchId | string? | 嵌套在另一个并行/条件分支内部时，记录父分支 ID |
| parentParallelBranchIndex | number? | 嵌套在另一个并行/条件分支内部时，记录父分支序号 |

> **示例**

```json
{
  "type": "parallel",
  "labelCn": "并行处理",
  "labelEn": "Parallel",
  "structConfig": {
    "parallelGroupId": "parallel-1",
    "parallelRole": "root"
  }
}
```

> **并行结构的 6 个 node + 8 条 edge 完整示例**（来源：前端 FlowCanvas 持久化数据，已对齐 Schema 命名）

```json
{
  "nodes": [
    {
      "id": "trigger-1",
      "type": "trigger",
      "position": { "x": 250, "y": 50 },
      "data": { "type": "trigger", "labelCn": "触发器", "labelEn": "Trigger", "structConfig": {} }
    },
    {
      "id": "parallel-1",
      "type": "parallel",
      "position": { "x": 250, "y": 160 },
      "data": {
        "type": "parallel",
        "labelCn": "并行处理",
        "labelEn": "Parallel",
        "structConfig": { "parallelGroupId": "parallel-1", "parallelRole": "root" }
      }
    },
    {
      "id": "parallel-branch-1-start",
      "type": "text",
      "position": { "x": 250, "y": 320 },
      "data": {
        "type": "text", "labelCn": "分支1开始", "labelEn": "Branch1 Start",
        "structConfig": { "parallelGroupId":"parallel-1", "parallelRole":"branch-start", "parallelBranchId":"b1", "parallelBranchIndex":1 }
      }
    },
    {
      "id": "parallel-branch-1-end",
      "type": "text",
      "position": { "x": 250, "y": 520 },
      "data": {
        "type": "text", "labelCn": "分支1结束", "labelEn": "Branch1 End",
        "structConfig": { "parallelGroupId":"parallel-1", "parallelRole":"branch-end", "parallelBranchId":"b1", "parallelBranchIndex":1 }
      }
    },
    {
      "id": "parallel-branch-2-start",
      "type": "text",
      "position": { "x": 570, "y": 320 },
      "data": {
        "type": "text", "labelCn": "分支2开始", "labelEn": "Branch2 Start",
        "structConfig": { "parallelGroupId":"parallel-1", "parallelRole":"branch-start", "parallelBranchId":"b2", "parallelBranchIndex":2 }
      }
    },
    {
      "id": "parallel-branch-2-end",
      "type": "text",
      "position": { "x": 570, "y": 520 },
      "data": {
        "type": "text", "labelCn": "分支2结束", "labelEn": "Branch2 End",
        "structConfig": { "parallelGroupId":"parallel-1", "parallelRole":"branch-end", "parallelBranchId":"b2", "parallelBranchIndex":2 }
      }
    },
    {
      "id": "parallel-merge-1",
      "type": "text",
      "position": { "x": 410, "y": 580 },
      "data": {
        "type": "text", "labelCn": "并行合并", "labelEn": "Merge",
        "structConfig": { "parallelGroupId":"parallel-1", "parallelRole":"merge" }
      }
    },
    {
      "id": "end-1",
      "type": "exit",
      "position": { "x": 410, "y": 740 },
      "data": { "type": "exit", "labelCn": "结束", "labelEn": "End", "output": {}, "structConfig": {} }
    }
  ],
  "edges": [
    { "id":"e-t-parallel",       "source":"trigger-1",              "target":"parallel-1",              "type":"smoothstep", "data":{ "businessType":"default", "connectionMode":"serial" } },
    { "id":"e-p-b1start",        "source":"parallel-1",             "target":"parallel-branch-1-start",  "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-p-b2start",        "source":"parallel-1",             "target":"parallel-branch-2-start",  "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-b1start-b1end",    "source":"parallel-branch-1-start","target":"parallel-branch-1-end",    "type":"smoothstep", "data":{ "businessType":"default", "connectionMode":"parallel" } },
    { "id":"e-b2start-b2end",    "source":"parallel-branch-2-start","target":"parallel-branch-2-end",    "type":"smoothstep", "data":{ "businessType":"default", "connectionMode":"parallel" } },
    { "id":"e-b1end-merge",      "source":"parallel-branch-1-end",  "target":"parallel-merge-1",         "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-b2end-merge",      "source":"parallel-branch-2-end",  "target":"parallel-merge-1",         "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-merge-end",        "source":"parallel-merge-1",       "target":"end-1",                    "type":"smoothstep", "data":{ "businessType":"default", "connectionMode":"serial" } }
  ]
}
```

> **引擎解析逻辑**：
> 1. 过滤 `node.type === "text"` 的标记节点、`edge.data.isStructural === true` 的辅助边
> 2. 剩余节点中，`branch-start → branch-end` 之间的子图即为各分支体。`connectionMode=parallel` 的分支引擎并发执行
> 3. 所有分支完成后在 `merge` 节点汇合，继续下游

#### 4.3.18 conditionBranchNodeDataDef

> 继承 `nodeDataBaseDef`（含 `structConfig`）。条件分支结构的**入口主节点**。与 parallel 同构——主节点结构、分组字段体系（`parallelGroupId` + `parallelRole`）、node/edge 数量（6+8）完全一致。区别：① 前端文案（"条件"替代"分支"）② 引擎按 `conditionExpr` 匹配分支执行（而非全部并发）。

> **Def**

```json
{
  "allOf": [{ "$ref": "#/definitions/nodeDataBaseDef" }]
}
```

> **示例**

```json
{
  "type": "conditionBranch",
  "labelCn": "条件分支",
  "labelEn": "Condition",
  "structConfig": {
    "parallelGroupId": "cond-1",
    "parallelRole": "root"
  }
}
```

> **条件分支完整示例**（6 nodes + 8 edges，来源于前端 FlowCanvas 持久化数据）

```json
{
  "nodes": [
    { "id":"trigger-1",    "type":"trigger",         "position":{"x":250,"y":50},  "data":{ "type":"trigger",         "labelCn":"触发器",     "labelEn":"Trigger",      "structConfig":{} } },
    { "id":"cond-1",       "type":"condition-branch", "position":{"x":250,"y":160}, "data":{ "type":"conditionBranch", "labelCn":"条件分支",   "labelEn":"Condition",    "structConfig":{ "parallelGroupId":"cond-1", "parallelRole":"root" } } },
    { "id":"cond-b1-start","type":"text",             "position":{"x":250,"y":320}, "data":{ "type":"text",             "labelCn":"条件1开始",  "labelEn":"Cond1 Start",  "structConfig":{ "parallelGroupId":"cond-1","parallelRole":"branch-start","parallelBranchId":"c1","parallelBranchIndex":1 } } },
    { "id":"cond-b1-end",  "type":"text",             "position":{"x":250,"y":520}, "data":{ "type":"text",             "labelCn":"条件1结束",  "labelEn":"Cond1 End",    "structConfig":{ "parallelGroupId":"cond-1","parallelRole":"branch-end",  "parallelBranchId":"c1","parallelBranchIndex":1 } } },
    { "id":"cond-b2-start","type":"text",             "position":{"x":570,"y":320}, "data":{ "type":"text",             "labelCn":"条件2开始",  "labelEn":"Cond2 Start",  "structConfig":{ "parallelGroupId":"cond-1","parallelRole":"branch-start","parallelBranchId":"c2","parallelBranchIndex":2 } } },
    { "id":"cond-b2-end",  "type":"text",             "position":{"x":570,"y":520}, "data":{ "type":"text",             "labelCn":"条件2结束",  "labelEn":"Cond2 End",    "structConfig":{ "parallelGroupId":"cond-1","parallelRole":"branch-end",  "parallelBranchId":"c2","parallelBranchIndex":2 } } },
    { "id":"cond-merge-1", "type":"text",             "position":{"x":410,"y":580}, "data":{ "type":"text",             "labelCn":"条件合并",   "labelEn":"Merge",        "structConfig":{ "parallelGroupId":"cond-1","parallelRole":"merge" } } },
    { "id":"end-1",        "type":"exit",             "position":{"x":410,"y":740}, "data":{ "type":"exit",             "labelCn":"结束",       "labelEn":"End",          "output":{}, "structConfig":{} } }
  ],
  "edges": [
    { "id":"e-t-cond",      "source":"trigger-1",   "target":"cond-1",        "type":"smoothstep", "data":{ "businessType":"default",    "connectionMode":"serial" } },
    { "id":"e-c-b1start",   "source":"cond-1",      "target":"cond-b1-start",  "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-c-b2start",   "source":"cond-1",      "target":"cond-b2-start",  "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-b1s-b1e",     "source":"cond-b1-start","target":"cond-b1-end",   "type":"smoothstep", "data":{ "businessType":"condition", "conditionExpr":"${$.node.trigger.input.body.type} == 'A'" } },
    { "id":"e-b2s-b2e",     "source":"cond-b2-start","target":"cond-b2-end",   "type":"smoothstep", "data":{ "businessType":"condition", "conditionExpr":"${$.node.trigger.input.body.type} == 'B'" } },
    { "id":"e-b1e-merge",   "source":"cond-b1-end",  "target":"cond-merge-1",   "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-b2e-merge",   "source":"cond-b2-end",  "target":"cond-merge-1",   "type":"smoothstep", "data":{ "isStructural":true } },
    { "id":"e-merge-end",   "source":"cond-merge-1", "target":"end-1",          "type":"smoothstep", "data":{ "businessType":"default",    "connectionMode":"serial" } }
  ]
}
```

> **引擎解析逻辑**：
> 1. 过滤 `node.type === "text"` 的标记节点、`edge.data.isStructural === true` 的辅助边
> 2. 对每条 `businessType=condition` 的边，按 `conditionExpr` 匹配，命中的分支执行其内部子图
> 3. 匹配完成后在 `merge` 节点汇合，继续下游

#### 4.3.19 nodeDataDef（路由汇总）

> 节点业务数据路由器，按 `node.data.type` 分发到对应 data Schema。以下 oneOf 为文档化路由规则，运行时由应用层基于 `node.data.type` 选择对应 Def 校验。

| `node.data.type` | data Schema | 详见 |
|------|------|:--:|
| `trigger` | triggerNodeDataDef | §4.3.9 |
| `connector` | connectorNodeDataDef | §4.3.10 |
| `script` | scriptNodeDataDef | §4.3.13 |
| `dataProcessor` | dataProcessorNodeDataDef | §4.3.12 |
| `exit` | exitNodeDataDef | §4.3.11 |
| `loopV2` | loopNodeDataDef | §4.3.15 |
| `parallel` | parallelNodeDataDef | §4.3.17 |
| `conditionBranch` | conditionBranchNodeDataDef | §4.3.18 |
| `errorHandler` | errorHandlerNodeDataDef | §4.3.14 |
| `text` | textNodeDataDef | §4.3.16 |
### 4.4 完整组件定义

以下 JSON 为所有共享组件定义的聚合，是整个文档中 JSON Schema 定义的唯一权威来源。各组件通过 `#/definitions/xxx` 路径被 §5 和 §6 引用。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "urn:openapp:schema:definitions:v8",
  "title": "共享 Schema 组件聚合",
  "description": "所有上下文 Schema 共用的组件定义。v8：合并 mappedFieldDef/mappedJsonSchemaObjectDef 至 jsonObjectDef v2（value 可选）",

  "definitions": {

    "authConfigDef": {
      "$id": "urn:openapp:schema:authConfigDef:v2",
      "title": "authConfigDef",
      "description": "认证类型声明。v2 重构：fields[] 自定结构改为 header/query 复用 jsonObjectDef。type 使用字符串枚举（见 §2.2）",
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "type": {
          "type": "string",
          "enum": ["SOA", "APIG", "SYSTOKEN", "AKSK", "NONE", "COOKIE", "SIGNATURE"]
        },
        "header": { "$ref": "#/definitions/jsonObjectDef", "description": "放置在请求头的认证字段" },
        "query":  { "$ref": "#/definitions/jsonObjectDef", "description": "放置在 Query 的认证字段" },
        "secretKey": {
          "$ref": "#/definitions/jsonObjectDef",
          "description": "签名密钥定义（仅 SIGNATURE 类型使用）。字段含 sensitive=true 标记加密存储 + 运行时脱敏"
        },
        "sysAccountWhitelist": {
          "type": "array",
          "items": { "type": "string" },
          "uniqueItems": true,
          "description": "允许触发此连接流的 SYSTOKEN 凭证标识列表（仅触发器使用）"
        }
      },
      "required": ["type"],
      "anyOf": [
        { "required": ["header"] },
        { "required": ["query"] }
      ],
      "allOf": [
        {
          "if": {
            "properties": { "type": { "const": "SYSTOKEN" } },
            "required": ["type"]
          },
          "then": {
            "required": ["sysAccountWhitelist"]
          }
        },
        {
          "if": {
            "properties": { "type": { "const": "SIGNATURE" } },
            "required": ["type"]
          },
          "then": {
            "required": ["secretKey"]
          }
        }
      ]
    },

    "rateLimitConfigDef": {
      "$id": "urn:openapp:schema:rateLimitConfigDef:v1",
      "title": "rateLimitConfigDef",
      "type": "object",
      "additionalProperties": false,
      "properties": {
    "maxQps": { "type": "integer", "minimum": 1, "maximum": 1000 },
        "maxConcurrency": { "type": "integer", "minimum": 1, "maximum": 1000 }
      }
    },

    "jsonObjectDef": {
      "$id": "urn:openapp:schema:jsonObjectDef:v2",
      "title": "jsonObjectDef",
      "description": "对象字段定义（v2 合并 mappedFieldDef/mappedJsonSchemaObjectDef）。properties 各字段可选 value（映射表达式）——有值=编排映射场景，无值=纯声明场景。支持递归嵌套 object/array",
      "type": "object",
      "properties": {
        "type": { "type": "string", "enum": ["object"] },
        "properties": {
          "type": "object",
          "additionalProperties": {
            "oneOf": [
              {
                "description": "叶子字段：基本类型 + 可选 value",
                "type": "object",
                "properties": {
                   "type":        { "type": "string", "enum": ["string", "number", "boolean"] },
                   "description": { "type": "string" },
                   "value":       { "type": "string", "description": "映射表达式（可选）。遵循 §3 值表达式体系" },
                   "required":    { "type": "boolean", "default": false, "description": "是否必填" },
                   "sensitive":   { "type": "boolean", "default": false, "description": "敏感字段标记：① 落库加密存储 ② 日志脱敏打印" },
                  "enum":        { "type": "array" },
                  "default":     {},
                  "minimum":     { "type": "number" },
                  "maximum":     { "type": "number" }
                },
                "required": ["type"]
              },
              {
                "description": "嵌套 object：递归引用自身",
                "$ref": "#/definitions/jsonObjectDef"
              },
              {
                "description": "数组字段：items 递归引用此组件",
                "type": "object",
                "properties": {
                  "type":        { "type": "string", "enum": ["array"] },
                  "description": { "type": "string" },
                  "required":    { "type": "boolean", "default": false },
                  "readonly":    { "type": "boolean", "default": false },
                  "deprecated":  { "type": "boolean", "default": false },
                  "minItems":    { "type": "number",  "description": "数组最少元素数" },
                  "maxItems":    { "type": "number",  "description": "数组最多元素数" },
                  "items":       { "$ref": "#/definitions/jsonObjectDef" }
                },
                "required": ["type", "items"]
              }
            ]
          }
        }
      },
      "required": ["type", "properties"]
    },

    "httpInputDef": {
      "$id": "urn:openapp:schema:httpInputDef:v1",
      "title": "httpInputDef",
      "description": "HTTP 入参——header / query / body 三段式",
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "protocol": { "type": "string", "const": "HTTP" },
        "header": { "$ref": "#/definitions/jsonObjectDef" },
        "query":  { "$ref": "#/definitions/jsonObjectDef" },
        "body":   { "$ref": "#/definitions/jsonObjectDef" }
      },
      "required": ["protocol"],
      "anyOf": [
        { "required": ["header"] },
        { "required": ["query"] },
        { "required": ["body"] }
      ]
    },

    "httpOutputDef": {
      "$id": "urn:openapp:schema:httpOutputDef:v1",
      "title": "httpOutputDef",
      "description": "HTTP 出参——header / body 两段式",
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "protocol": { "type": "string", "const": "HTTP" },
        "header": { "$ref": "#/definitions/jsonObjectDef" },
        "body":   { "$ref": "#/definitions/jsonObjectDef" }
      },
      "required": ["protocol"],
      "anyOf": [
        { "required": ["header"] },
        { "required": ["body"] }
      ]
    },

    "errorInfoDef": {
      "$id": "urn:openapp:schema:errorInfoDef:v2",
      "title": "errorInfoDef",
      "description": "错误详情。code 为数字字符串，message 双语拆分",
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "code": { "type": "string", "pattern": "^[1-9][0-9]{2,4}$" },
        "messageZh": { "type": "string" },
        "messageEn": { "type": "string" },
        "cause": { "type": "string", "description": "根因描述（内部错误时携带）" },
        "downstreamStatus": { "type": "integer", "description": "下游 HTTP 状态码" },
        "downstreamBody": { "type": "string", "description": "下游响应体片段" }
      },
      "required": ["code", "messageZh", "messageEn"],
      "oneOf": [
        { "required": ["cause"] },
        { "required": ["downstreamStatus"] }
      ]
    },

    "nodeDataBaseDef": {
      "$id": "urn:openapp:schema:nodeDataBaseDef:v2",
      "title": "nodeDataBaseDef",
      "description": "节点 data 公共基类。所有节点 data 的 allOf 基础，type 为业务节点类型（与 React Flow node.type 分离）",
      "type": "object",
      "properties": {
        "type": {
          "type": "string",
          "enum": ["trigger", "connector", "dataProcessor", "exit", "loopV2", "parallel", "conditionBranch", "errorHandler", "text"],
          "description": "业务节点类型"
        },
        "labelCn": { "type": "string", "description": "节点中文标签" },
        "labelEn": { "type": "string", "description": "节点英文标签" },
        "structConfig": {
          "type": "object",
          "description": "DAG 拓扑配置。结构节点和文本标记节点的分组归属关系，用于前端布局和引擎拓扑解析"
        }
      },
      "required": ["type"]
    },

    "nodeDataDef": {
      "$id": "urn:openapp:schema:nodeDataDef:v3",
      "title": "nodeDataDef",
      "description": "节点业务数据路由器。按 node.data.type 分发到对应 data Schema（文档化路由规则，运行时由应用层基于 node.data.type 校验）",
      "type": "object",
      "oneOf": [
        { "$ref": "#/definitions/triggerNodeDataDef",          "description": "node.data.type='trigger'" },
        { "$ref": "#/definitions/connectorNodeDataDef",         "description": "node.data.type='connector'" },
        { "$ref": "#/definitions/dataProcessorNodeDataDef",     "description": "node.data.type='dataProcessor'" },
        { "$ref": "#/definitions/exitNodeDataDef",              "description": "node.data.type='exit'" },
        { "$ref": "#/definitions/loopNodeDataDef",              "description": "node.data.type='loopV2'" },
        { "$ref": "#/definitions/parallelNodeDataDef",           "description": "node.data.type='parallel'" },
        { "$ref": "#/definitions/conditionBranchNodeDataDef",    "description": "node.data.type='conditionBranch'" },
        { "$ref": "#/definitions/errorHandlerNodeDataDef",       "description": "node.data.type='errorHandler'" },
        { "$ref": "#/definitions/textNodeDataDef",         "description": "node.data.type='text'" }
      ]
    },

    "triggerNodeDataDef": {
      "$id": "urn:openapp:schema:triggerNodeDataDef:v3",
      "title": "triggerNodeDataDef",
      "description": "触发器节点业务数据",
      "allOf": [
        { "$ref": "#/definitions/nodeDataBaseDef" },
        {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "triggerType": {
              "type": "string",
              "enum": ["http", "manual"],
              "description": "触发器激活方式。http=对外暴露 HTTP 端点，manual=仅内部/手动触发"
            },
    "authConfigs": {
      "type": "array",
      "minItems": 1,
      "items": { "$ref": "#/definitions/authConfigDef" },
      "description": "认证配置列表。支持多选组合，运行时按数组顺序依次附加。至少一种认证方式"
    },
            "input": { "$ref": "#/definitions/httpInputDef" },
            "rateLimitConfig": { "$ref": "#/definitions/rateLimitConfigDef" }
          },
          "required": ["triggerType"]
        },
        {
          "if": { "properties": { "triggerType": { "const": "http" } }, "required": ["triggerType"] },
          "then": { "required": ["authConfigs", "input"] }
        },
        {
          "if": { "properties": { "triggerType": { "const": "manual" } }, "required": ["triggerType"] },
          "then": { "properties": { "authConfigs": false, "input": false } }
        }
      ]
    },

    "connectorVersionConfigDef": {
      "$id": "urn:openapp:schema:connectorVersionConfigDef:v1",
      "title": "connectorVersionConfigDef",
      "description": "连接器配置完整定义。供 §5 connectionConfig 和 §4.3.10 快照共用",
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "labelCn":       { "type": "string" },
        "labelEn":       { "type": "string" },
        "protocol":      { "type": "string", "enum": ["HTTP"] },
        "protocolConfig": {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "url":    { "type": "string" },
            "method": { "type": "string", "enum": ["GET","POST","PUT","DELETE","PATCH"] }
          },
          "required": ["url", "method"]
        },
        "authConfigs": {
          "type": "array",
          "minItems": 1,
          "items": { "$ref": "#/definitions/authConfigDef" }
        },
        "input":  { "$ref": "#/definitions/httpInputDef" },
        "output": { "$ref": "#/definitions/httpOutputDef" },
        "timeoutMs": {
          "type": "integer",
          "default": 3000,
          "minimum": 1000,
          "maximum": 300000
        },
        "rateLimitConfig": {
          "$ref": "#/definitions/rateLimitConfigDef"
        }
      },
      "required": ["protocol", "protocolConfig"]
    },

    "connectorNodeDataDef": {
      "$id": "urn:openapp:schema:connectorNodeDataDef:v2",
      "title": "connectorNodeDataDef",
      "description": "连接器节点业务数据。v2 新增 connectorId + connectorVersionConfig 快照，移除 rateLimitConfig",
      "allOf": [
        { "$ref": "#/definitions/nodeDataBaseDef" },
        {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "connectorId": {
              "type": "string",
              "pattern": "^[1-9][0-9]{15,19}$",
              "description": "引用的连接器 ID"
            },
            "connectorVersionId": {
              "type": "string",
              "pattern": "^[1-9][0-9]{15,19}$",
              "description": "引用的连接器版本 ID，必须为已发布版本。更换版本时 connectorVersionConfig 全量替换"
            },
            "connectorVersionConfig": {
              "$ref": "#/definitions/connectorVersionConfigDef",
              "description": "连接器配置完整快照"
            },
            "timeoutMs": {
              "type": "integer",
              "minimum": 0,
              "maximum": 300000,
              "default": 0,
              "description": "节点超时（毫秒）。0 = 不限制，走系统默认上限。运行时取 min(该值, 系统上限)"
            },
            "input": {
              "type": "object",
              "properties": {
                "header": { "$ref": "#/definitions/jsonObjectDef" },
                "query":  { "$ref": "#/definitions/jsonObjectDef" },
                "body":   { "$ref": "#/definitions/jsonObjectDef" }
              },
              "description": "字段映射。properties 中通过 value 字段声明映射表达式"
            }
          },
          "required": ["connectorId", "connectorVersionId", "connectorVersionConfig"]
        }
      ]
    },

    "dataProcessorNodeDataDef": {
      "$id": "urn:openapp:schema:dataProcessorNodeDataDef:v2",
      "title": "dataProcessorNodeDataDef",
      "description": "数据处理器节点业务数据。无协议包袱，纯内存运行",
      "allOf": [
        { "$ref": "#/definitions/nodeDataBaseDef" },
        {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "output": { "$ref": "#/definitions/jsonObjectDef" }
          },
          "required": ["output"]
        }
      ]
    },

    "exitNodeDataDef": {
      "$id": "urn:openapp:schema:exitNodeDataDef:v1",
      "title": "exitNodeDataDef",
      "description": "出口节点业务数据",
      "allOf": [
        { "$ref": "#/definitions/nodeDataBaseDef" },
        {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "output": {
              "type": "object",
              "properties": {
                "header": { "$ref": "#/definitions/jsonObjectDef" },
                "body":   { "$ref": "#/definitions/jsonObjectDef" }
              }
            }
          },
          "required": ["output"]
        }
      ]
    },

    "loopNodeDataDef": {
      "$id": "urn:openapp:schema:loopNodeDataDef:v1",
      "title": "loopNodeDataDef",
      "description": "循环节点业务数据。继承 nodeDataBaseDef，无额外独有字段",
      "allOf": [{ "$ref": "#/definitions/nodeDataBaseDef" }]
    },

    "parallelNodeDataDef": {
      "$id": "urn:openapp:schema:parallelNodeDataDef:v1",
      "title": "parallelNodeDataDef",
      "description": "并行节点业务数据。继承 nodeDataBaseDef，无额外独有字段",
      "allOf": [{ "$ref": "#/definitions/nodeDataBaseDef" }]
    },

    "conditionBranchNodeDataDef": {
      "$id": "urn:openapp:schema:conditionBranchNodeDataDef:v1",
      "title": "conditionBranchNodeDataDef",
      "description": "条件分支节点业务数据。继承 nodeDataBaseDef，无额外独有字段",
      "allOf": [{ "$ref": "#/definitions/nodeDataBaseDef" }]
    },

    "errorHandlerNodeDataDef": {
      "$id": "urn:openapp:schema:errorHandlerNodeDataDef:v2",
      "title": "errorHandlerNodeDataDef",
      "description": "错误处理节点业务数据。errors 错误类型→策略映射，继承 nodeDataBaseDef",
      "allOf": [
        { "$ref": "#/definitions/nodeDataBaseDef" },
        {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "errors": {
              "type": "object",
              "minProperties": 1,
              "patternProperties": {
                "^(all|timeout|connection_error|other)$": {
                  "type": "object",
                  "properties": {
                    "strategy": { "type": "string", "enum": ["retry", "ignore", "terminate"] },
                    "retries": { "type": "integer", "minimum": 1, "maximum": 5 },
                    "interval": { "type": "integer", "minimum": 1, "maximum": 300 }
                  },
                  "required": ["strategy"]
                }
              },
              "additionalProperties": false
            }
          },
          "required": ["errors"]
        }
      ]
    },

    "textNodeDataDef": {
      "$id": "urn:openapp:schema:textNodeDataDef:v1",
      "title": "textNodeDataDef",
      "description": "text 标记节点数据。继承 nodeDataBaseDef，纯渲染，不参与 DAG 执行",
      "allOf": [{ "$ref": "#/definitions/nodeDataBaseDef" }]
    }
  }
}
```

---

## 5. 连接器配置 Schema

### 5.1 定位与存储

| 维度 | 说明 |
|------|------|
| 存储位置 | connector_version_t.connection_config (MEDIUMTEXT) |
| 框架归属 | 无 — 与 React Flow 完全无关 |
| 数据性质 | 连接器版本自身的对外 API 声明 |
| 生命周期 | 随连接器版本创建/发布，独立于编排 |
| 引用方式 | 编排中 connector 节点通过 connectorVersionId 引用已发布版本 |

### 5.2 Schema 定义

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "urn:openapp:schema:connectionConfig:v4",
  "title": "connectionConfig",
  "description": "连接器配置，声明如何调用下游 API。定义为 connectorVersionConfigDef（§4.3.7），存储在 connector_version_t.connection_config MEDIUMTEXT 字段中",
  "$ref": "#/definitions/connectorVersionConfigDef"
}
```

### 5.3 字段说明

字段定义见 §4.3.7 `connectorVersionConfigDef`。以下为 §5 特有的存储说明：

| 维度 | 说明 |
|------|------|
| 存储位置 | `connector_version_t.connection_config` (MEDIUMTEXT) |
| 框架归属 | 无 — 与 React Flow 完全无关 |
| 数据性质 | 连接器版本自身的对外 API 声明 |
| 生命周期 | 随连接器版本创建/发布，独立于编排 |
| 引用方式 | 编排中 connector 节点通过 connectorVersionId 引用已发布版本，选版本时立即全量快照至 connectorVersionConfig |

### 5.4 示例

以下为「创建订单」连接器的完整配置示例，覆盖 HTTP POST 协议下 **入参三段式（header / query / body）+ 出参两段式（header / body）+ 多认证组合 + 全量字段属性**：

```json
{
  "labelCn": "创建订单",
  "labelEn": "Create Order",
  "protocol": "HTTP",
  "protocolConfig": {
    "url": "https://api.example.com/v2/order/create",
    "method": "POST"
  },
  "authConfigs": [
    {
      "type": "SOA",
      "header": {
        "type": "object",
        "properties": {
          "X-Soa-Token": {
            "type": "string",
            "required": true,
            "sensitive": true,
            "value": "${$.system.env.soaToken}",
            "description": "SOA 认证令牌，值来源：凭据库"
          }
        }
      }
    },
    {
      "type": "APIG",
      "query": {
        "type": "object",
        "properties": {
          "apigAppKey": {
            "type": "string",
            "required": true,
            "value": "${$.system.env.apigAppKey}",
            "description": "APIG 应用标识"
          },
          "apigAppSecret": {
            "type": "string",
            "required": true,
            "sensitive": true,
            "value": "${$.system.env.apigAppSecret}",
            "description": "APIG 应用密钥"
          }
        }
      }
    }
  ],
  "input": {
    "protocol": "HTTP",
    "header": {
      "type": "object",
      "properties": {
        "Content-Type": {
          "type": "string",
          "required": true,
          "default": "application/json",
          "enum": ["application/json", "application/x-www-form-urlencoded"],
          "description": "请求内容类型"
        },
        "X-Request-Id": {
          "type": "string",
          "required": true,
          "pattern": "^[a-f0-9]{32}$",
          "placeholder": "请输入 32 位 hex 请求追踪 ID",
          "description": "请求追踪 ID，用于全链路日志关联"
        },
        "Accept-Language": {
          "type": "string",
          "required": false,
          "default": "zh-CN",
          "enum": ["zh-CN", "en-US"],
          "description": "客户端语言偏好"
        }
      },
      "required": ["Content-Type", "X-Request-Id"]
    },
    "query": {
      "type": "object",
      "properties": {
        "version": {
          "type": "string",
          "required": true,
          "pattern": "^v[1-9]\\d*$",
          "description": "API 版本号，如 v2"
        },
        "dryRun": {
          "type": "boolean",
          "required": false,
          "default": false,
          "description": "是否试运行模式，true 时仅校验不落库"
        }
      },
      "required": ["version"]
    },
    "body": {
      "type": "object",
      "properties": {
        "orderId": {
          "type": "string",
          "required": true,
          "pattern": "^ORD\\d{14}$",
          "placeholder": "ORD20260612153000",
          "description": "订单号，格式 ORD + 14 位时间戳"
        },
        "amount": {
          "type": "number",
          "required": true,
          "minimum": 0.01,
          "maximum": 999999.99,
          "description": "订单金额（元），精确到分"
        },
        "currency": {
          "type": "string",
          "required": true,
          "enum": ["CNY", "USD", "EUR", "JPY"],
          "default": "CNY",
          "description": "货币类型"
        },
        "items": {
          "type": "array",
          "required": true,
          "minItems": 1,
          "maxItems": 50,
          "description": "订单商品明细",
          "items": {
            "type": "object",
            "properties": {
              "skuCode": {
                "type": "string",
                "required": true,
                "minLength": 6,
                "maxLength": 32,
                "description": "SKU 编码"
              },
              "quantity": {
                "type": "number",
                "required": true,
                "minimum": 1,
                "maximum": 999,
                "description": "购买数量"
              },
              "unitPrice": {
                "type": "number",
                "required": true,
                "minimum": 0,
                "description": "单价（元）"
              }
            },
            "required": ["skuCode", "quantity", "unitPrice"]
          }
        },
        "contactInfo": {
          "type": "object",
          "required": false,
          "description": "联系信息（嵌套对象）",
          "properties": {
            "name": {
              "type": "string",
              "required": true,
              "maxLength": 50,
              "description": "收货人姓名"
            },
            "phone": {
              "type": "string",
              "required": true,
              "sensitive": true,
              "pattern": "^1[3-9]\\d{9}$",
              "placeholder": "请输入 11 位手机号",
              "description": "收货人手机号（敏感字段：落库加密 + 日志脱敏）"
            },
            "email": {
              "type": "string",
              "required": false,
              "pattern": "^[\\w.-]+@[\\w.-]+\\.\\w{2,}$",
              "description": "收货人邮箱"
            }
          },
          "required": ["name", "phone"]
        },
        "remark": {
          "type": "string",
          "required": false,
          "maxLength": 500,
          "nullable": true,
          "description": "订单备注，可为空"
        }
      },
      "required": ["orderId", "amount", "currency", "items"]
    }
  },
  "output": {
    "protocol": "HTTP",
    "header": {
      "type": "object",
      "properties": {
        "X-Trace-Id": {
          "type": "string",
          "description": "服务端追踪 ID"
        },
        "X-RateLimit-Remaining": {
          "type": "number",
          "description": "本次调用后剩余配额"
        },
        "X-Process-Time-Ms": {
          "type": "number",
          "description": "服务端处理耗时（毫秒）"
        }
      }
    },
    "body": {
      "type": "object",
      "properties": {
        "success": {
          "type": "boolean",
          "description": "是否成功"
        },
        "data": {
          "type": "object",
          "description": "业务数据（嵌套对象）",
          "properties": {
            "orderCode": {
              "type": "string",
              "description": "平台生成的订单编号"
            },
            "status": {
              "type": "string",
              "enum": ["PENDING", "PROCESSING", "SUCCESS", "FAILED"],
              "description": "订单状态"
            },
            "totalAmount": {
              "type": "number",
              "description": "实付总金额（含优惠）"
            },
            "createdAt": {
              "type": "string",
              "description": "订单创建时间 (ISO 8601)"
            }
          }
        },
        "errorCode": {
          "type": "string",
          "description": "错误码（success=false 时返回）"
        },
        "errorMsg": {
          "type": "string",
          "description": "错误描述（success=false 时返回）"
        }
      }
    }
  },
  "timeoutMs": 8000,
  "rateLimitConfig": {
    "maxQps": 50,
    "maxConcurrency": 10
  }
}
```

> **字段覆盖清单**：
>
> | 维度 | 覆盖内容 |
> |------|---------|
> | 入参 header | `Content-Type` (required + default + enum)、`X-Request-Id` (required + pattern + placeholder)、`Accept-Language` (optional + default + enum) |
> | 入参 query | `version` (required + pattern)、`dryRun` (boolean + default) |
> | 入参 body | 基本类型 `orderId`/`amount`/`currency`/`remark`、枚举 `currency`、array `items`（含 object items，字段含 minLength/maxLength/minimum）、嵌套 object `contactInfo`（含 sensitive 字段 `phone`）、nullable `remark` |
> | 出参 header | `X-Trace-Id` (string)、`X-RateLimit-Remaining` (number)、`X-Process-Time-Ms` (number) |
> | 出参 body | 基本类型 `success`(boolean)/`errorCode`/`errorMsg`、嵌套 object `data`（含枚举 `status`、number `totalAmount`） |
> | 认证 | SOA（header 单个敏感 token）+ APIG（query 双字段 key+secret），展示多认证组合按序附加 |
> | 全局 | `timeoutMs`、`rateLimitConfig`（maxQps + maxConcurrency） |

---

## 6. 连接流编排配置 Schema

### 6.1 结构总览

编排配置以 DAG（有向无环图）形式存储在 `flow_version_t.orchestration_config`（MEDIUMTEXT）中，由一个三元组构成：

```mermaid
graph TB
    subgraph Storage["orchestrationConfig"]
        nodes["nodes[]<br/>节点列表（框架坐标 + 业务配置）"]
        edges["edges[]<br/>边列表（连线拓扑 + 控制流语义）"]
        flowConfig["flowConfig<br/>连接流级配置（限流 + 缓存）"]
    end
```

| 组成 | 类型 | 说明 | 详细定义 |
|------|:--:|------|:--:|
| `nodes[]` | array | DAG 节点，至少 2 个（1 trigger + 1 exit）。框架字段 + 业务 data 两层结构 | §6.3.1 |
| `edges[]` | array | DAG 边，至少 1 条。框架字段 + 控制流 data | §6.3.2 |
| `flowConfig` | object | 连接流级全局配置（入站限流、响应缓存） | §6.3.3 |

> **框架/业务分离**：React Flow 框架字段（`id`/`type`/`position`/`source`/`target`）与业务字段（`node.data`/`edge.data`）严格分层，这是 §1.3 原则四「框业分离」在本章的直接体现。

### 6.2 完整 Schema（orchestrationConfig）

完整的 `orchestrationConfig` JSON Schema 定义：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "urn:openapp:schema:orchestrationConfig:v7",
  "title": "orchestrationConfig",
  "description": "连接流编排配置，以显式 DAG（nodes + edges）存储完整编排定义。V7：node.type 扩展至 9 种，edge.data 承载完整控制流语义",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "nodes": {
      "type": "array",
      "minItems": 2,
      "description": "DAG 节点列表。最少 2 个（1 trigger + 1 exit），可含结构节点和 text 标记",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": { "type": "string" },
          "type": {
            "type": "string",
            "enum": [
              "trigger", "connector", "data_processor", "exit",
              "loop-v2", "error-handler", "parallel", "condition-branch",
              "text"
            ]
          },
          "position": {
            "type": "object",
            "additionalProperties": false,
            "properties": { "x": { "type": "number" }, "y": { "type": "number" } }
          },
          "data": { "$ref": "#/definitions/nodeDataDef" }
        },
        "required": ["id", "type", "data"]
      }
    },
    "edges": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": { "type": "string" },
          "source": { "type": "string" },
          "target": { "type": "string" },
          "type": { "type": "string", "default": "smoothstep" },
          "label": { "type": "string" },
          "data": {
            "type": "object",
            "properties": {
              "businessType": { "type": "string", "enum": ["default","condition","error","always","loop_entry","loop_exit"], "default": "default" },
              "conditionExpr": { "type": "string" },
              "connectionMode": { "type": "string", "enum": ["serial","parallel"], "default": "serial" },
              "isStructural": { "type": "boolean", "default": false },
              "iterationVar": { "type": "string" }
            }
          }
        },
        "required": ["id", "source", "target"]
      }
    },
    "flowConfig": {
      "type": "object",
      "additionalProperties": false,
      "description": "连接流级配置（入站限流、缓存）。快照在 FlowVersion 中，不独立建表",
      "properties": {
        "flowMode": {
          "type": "string",
          "enum": ["single", "serial", "parallel"],
          "description": "编排模式：single(单节点)/serial(串行)/parallel(并行)。前端渲染编排画布必需"
        },
        "rateLimitConfig": { "$ref": "#/definitions/rateLimitConfigDef" },
        "cache": {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "key": {
              "type": "array",
              "items": { "type": "string" },
              "minItems": 1,
              "description": "缓存键表达式列表。每个元素遵循 §3 值表达式体系，运行时按序解析后以冒号(:)拼接为完整缓存键"
            },
            "ttl": { "type": "number", "minimum": 1, "description": "缓存时长（秒）" }
          },
          "required": ["key", "ttl"]
        }
      }
    }
  },
  "required": ["nodes", "edges"]
}
```

### 6.3 字段说明

#### 6.3.1 nodes[] — 节点

每个节点由 **框架字段**（React Flow 画布渲染必需）和 **业务字段**（`node.data`）构成。`node.data` 的具体结构由 `node.data.type` 决定，通过 `nodeDataDef`（§4.3.19）按 9 种类型路由。框架层的 `node.type` 仅用于 React Flow 组件注册。

**框架字段**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `id` | string | ✅ | 节点 ID，编排内部唯一 |
| `type` | string | ✅ | React Flow 注册组件名（9 种枚举）。纯渲染类型，不承载业务语义 |
| `position` | object | ❌ | 画布坐标 `{ x: number, y: number }`。编排保存时前端注入，API 提交时可选 |

> `node.type` 的 10 种枚举值（框架层）：`trigger` / `connector` / `script` / `data_processor` / `exit` / `loop-v2` / `error-handler` / `parallel` / `condition-branch` / `text`

**node.data 路由**：

| `node.type`（框架） | `node.data.type`（业务） | `data` Schema | 必填字段 | 说明 |
|------|------|------------|:--:|------|
| `trigger` | `trigger` | triggerNodeDataDef | triggerType, authConfigs(⍟), input(⍟) | 触发器（激活方式由 triggerType 区分） |
| `connector` | `connector` | connectorNodeDataDef | connectorId, connectorVersionId, connectorVersionConfig | 连接器调用（含配置快照 + 字段映射） |
| `script` | `script` | scriptNodeDataDef | script | GraalJS 沙箱脚本执行 |
| `data_processor` | `dataProcessor` | dataProcessorNodeDataDef | output | 数据管道 |
| `exit` | `exit` | exitNodeDataDef | output | 出口响应 |
| `loop-v2` | `loopV2` | loopNodeDataDef | type | 循环主节点 |
| `parallel` | `parallel` | parallelNodeDataDef | type | 并行主节点 |
| `condition-branch` | `conditionBranch` | conditionBranchNodeDataDef | type | 条件分支主节点 |
| `error-handler` | `errorHandler` | errorHandlerNodeDataDef | type | 错误处理主节点 |
| `text` | `text` | textNodeDataDef | — | 纯渲染标记（引擎跳过） |

⍟ = `triggerType="http"` 时必填。各 data Schema 完整定义见 §4.3.9 ~ §4.3.18。

#### 6.3.2 edges[] — 边

每条边由 **框架字段**（连线渲染）和 **业务字段**（`edge.data`，承载控制流语义）构成。

**框架字段**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `id` | string | ✅ | 边 ID |
| `source` | string | ✅ | 源节点 ID |
| `target` | string | ✅ | 目标节点 ID |
| `type` | string | ❌ | 渲染样式，默认 `smoothstep` |
| `label` | string | ❌ | 边标签（画布展示用） |

**edge.data — 控制流语义**：

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:----:|:------:|------|
| `businessType` | string | ❌ | `"default"` | 边业务类型：`default`（普通串行）/ `condition`（条件分支）/ `error`（错误路由）/ `always`（无条件通过）/ `loop_entry`（循环入口）/ `loop_exit`（循环出口） |
| `conditionExpr` | string | ❌ | — | 条件表达式，`businessType=condition` 时必填，遵循 §3 值表达式体系 |
| `connectionMode` | string | ❌ | `"serial"` | 连接模式：`serial`（串行）或 `parallel`（并发） |
| `isStructural` | boolean | ❌ | `false` | 结构辅助边标记。`true` 时引擎拓扑排序跳过，等价前端 `hideInsertButton` |
| `iterationVar` | string | ❌ | — | 循环迭代变量名，`businessType=loop_entry` 时使用 |

#### 6.3.3 flowConfig — 流级配置

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `flowMode` | string | ✅ | 编排模式：`single`（单节点：触发器→连接器→输出）/ `serial`（串行编排，可追加多节点）/ `parallel`（并行编排，多分支）。前端渲染编排画布必需 |
| `rateLimitConfig` | object | ❌ | 入站限流配置，复用 `rateLimitConfigDef`（§4.3.3）。`maxQps`：每秒最大请求数（1-1000）；`maxConcurrency`：最大并发数（1-1000） |
| `cache` | object | ❌ | 响应缓存配置，命中后跳过 DAG 执行直接返回 |
| `cache.key` | string[] | ✅⚡ | 缓存键表达式列表，minItems 1。每个元素遵循 §3 值表达式体系，运行时按序解析后以冒号(`:`)拼接为完整缓存键 |
| `cache.ttl` | number | ✅⚡ | 缓存时长（秒），≥1 |

⚡ = `cache` 存在时必填。

> **示例** — 单节点模式 + 限流 + 缓存：
>
> ```json
> {
>   "flowMode": "single",
>   "rateLimitConfig": { "maxQps": 100, "maxConcurrency": 20 },
>   "cache": {
>     "key": ["${$.node.trigger.input.body.sender}", "${$.constant:msg_query}"],
>     "ttl": 60
>   }
> }
> ```
>
> **缓存语义**：`cache.key` 按表达式列表解析为具体值后拼接（如 `"${$.node.trigger.input.body.userId}"` → `"u001"`），再以冒号拼接为最终键（如 `"u001:queryOrders"`），查 Redis。命中则跳过 DAG 执行，直接返回上次缓存的响应。

### 6.4 DAG 拓扑约束

> 以下约束由应用层强制校验，JSON Schema 无法覆盖（跨节点引用需图遍历）。

| 规则 | 说明 | 校验时机 |
|------|------|:--:|
| **节点 ID 唯一** | `nodes[].id` 在编排内不重复 | 保存/发布 |
| **边引用存在** | `source`/`target` 必须对应已声明的 `id` | 保存/发布 |
| **text 节点不参与拓扑** | `node.type === "text"` 在拓扑排序前过滤 | 运行时 |
| **trigger 入度为 0** | trigger 节点不准有入边 | 保存/发布 |
| **exit 出度为 0** | exit 节点不准有出边 | 保存/发布 |
| **无环** | 非 text 节点间拓扑排序可完成（Kahn 算法） | 保存/发布 |
| **禁止重复边** | 同一 source-target 对只允许一条边 | 保存/发布 |
| **结构体完整性** | 结构主节点必须有完整闭合的 text 标记链路 | 保存/发布 |
| **并行一致性** | `connectionMode="parallel"` 的多条出边，source 相同、target 互不重叠 | 保存/发布 |
| **isStructural 边不执行** | `edge.data.isStructural=true` 的边在拓扑排序前过滤 | 运行时 |

### 6.5 编排示例

#### 6.5.1 MVP 线性编排

```json
{
  "nodes": [
    {
      "id": "node_trigger", "type": "trigger",
      "position": { "x": 100, "y": 200 },
      "data": {
        "labelCn": "接收请求", "type": "trigger", "triggerType": "http",
        "authConfigs": [{ "type": "SYSTOKEN", "header": { "type": "object", "properties": { "X-Sys-Token": { "type": "string", "required": true, "sensitive": true } } } }],
        "input": { "protocol": "HTTP", "body": { "type": "object", "properties": { "sender":{"type":"string"}, "content":{"type":"string"} }, "required":["sender","content"] } }
      }
    },
    {
      "id": "node_1", "type": "connector",
      "position": { "x": 350, "y": 200 },
      "data": {
        "labelCn": "发送消息", "type": "connector",
        "connectorId": "1234567890123456000",
        "connectorVersionId": "1234567890123456789",
        "connectorVersionConfig": {
          "protocol": "HTTP", "protocolConfig": { "url": "https://api.example.com/msg/send", "method": "POST" },
          "authConfigs": [{ "type": "SOA", "header": { "type": "object", "properties": { "X-Soa-Token": { "type": "string", "required": true, "sensitive": true, "value": "${$.system.env.soaToken}" } } } }],
          "input": { "protocol": "HTTP", "body": { "type": "object", "properties": { "receiver": { "type": "string", "required": true }, "content": { "type": "string", "required": true } }, "required": ["receiver", "content"] } },
          "output": { "protocol": "HTTP", "body": { "type": "object", "properties": { "msgId": { "type": "string" } } } },
          "timeoutMs": 3000
        },
        "input": { "body": { "type": "object", "properties": { "receiver":{"type":"string","value":"${$.node.trigger.input.sender}"}, "content":{"type":"string","value":"${$.node.trigger.input.content}"} }, "required":["receiver","content"] } }
      }
    },
    {
      "id": "node_exit", "type": "exit",
      "position": { "x": 600, "y": 200 },
      "data": {
        "labelCn": "返回结果",
        "output": { "body": { "type": "object", "properties": { "msgId":{"type":"string","value":"${$.node.node_1.output.msgId}"}, "status":{"type":"string","value":"${$.constant:success}"} } } }
      }
    }
  ],
  "edges": [
    { "id":"e1", "source":"node_trigger", "target":"node_1",    "data":{"businessType":"default"} },
    { "id":"e2", "source":"node_1",       "target":"node_exit", "data":{"businessType":"default"} }
  ],
  "flowConfig": {
    "flowMode": "single",
    "rateLimitConfig": { "maxQps": 100, "maxConcurrency": 20 },
    "cache": {
      "key": ["${$.node.trigger.input.body.sender}", "${$.constant:msg_query}"],
      "ttl": 60
    }
  }
}
```

#### 6.5.2 V2 循环编排

循环体内嵌 connector → data_processor → script → error_handler，展示结构节点间的包含关系。

```json
{
  "nodes": [
    // ==================== 流程入口 ====================
    {
      "id": "node_trigger", "type": "trigger",
      "position": { "x": 250, "y": 50 },
      "data": {
        "labelCn": "批量处理触发", "type": "trigger", "triggerType": "http",
        "authConfigs": [{ "type": "SYSTOKEN", "header": { "type": "object", "properties": { "X-Sys-Token": { "type": "string", "required": true, "sensitive": true } } } }],
        "input": { "protocol": "HTTP", "body": { "type": "object", "properties": { "items": { "type":"array","items":{"type":"string"} } } } }
      }
    },

    // ==================== 循环结构 (5 nodes) ====================
    { "id":"loop_1",        "type":"loop-v2", "position":{"x":250,"y":160}, "data":{ "type":"loopV2", "labelCn":"遍历处理",  "structConfig":{ "loopV2GroupId":"loop_1" } } },
    { "id":"loop_region_1", "type":"text",    "position":{"x":-10,"y":300}, "data":{ "type":"text",    "labelCn":"循环区域",  "structConfig":{ "loopV2GroupId":"loop_1","loopV2Role":"region" } } },
    // ↓ 循环体入口
    { "id":"loop_start_1",  "type":"text",    "position":{"x":510,"y":300}, "data":{ "type":"text",    "labelCn":"循环开始",  "structConfig":{ "loopV2GroupId":"loop_1","loopV2Role":"start" } } },

    // ==================== 循环体：connector → data_processor ====================
    {
      "id": "conn_1", "type": "connector",
      "position": { "x": 760, "y": 300 },
      "data": {
        "labelCn": "获取详情", "type": "connector",
        "connectorId": "1234567890123456000",
        "connectorVersionId": "1234567890123456789",
        "connectorVersionConfig": { "protocol": "HTTP", "protocolConfig": { "url": "https://api.example.com/item/detail", "method": "GET" }, "authConfigs": [{ "type": "SOA", "header": { "type": "object", "properties": { "X-Soa-Token": { "type": "string", "required": true, "sensitive": true, "value": "${$.system.env.soaToken}" } } } }], "input": { "protocol": "HTTP", "body": { "type": "object", "properties": { "itemId": { "type": "string", "required": true } } } }, "output": { "protocol": "HTTP", "body": { "type": "object", "properties": { "name": { "type": "string" } } } }, "timeoutMs": 5000 },
        "input": { "body": { "type": "object", "properties": { "itemId": { "type":"string","value":"${$.node.loop_1.current.item}" } } } },
        "structConfig": { "parentLoopV2GroupId":"loop_1","parentLoopV2Role":"right-column-node" }
      }
    },
    {
      "id": "data_proc_1", "type": "data_processor",
      "position": { "x": 760, "y": 460 },
      "data": {
        "labelCn": "格式化结果",
        "type": "dataProcessor",
        "output": { "type": "object", "properties": { "upperName": { "type":"string","value":"${$.system.fn.upper($.node.conn_1.output.body.name)}" } } },
        "structConfig": { "parentLoopV2GroupId":"loop_1","parentLoopV2Role":"right-column-node" }
      }
    },
    {
      "id": "script_1", "type": "script",
      "position": { "x": 1010, "y": 460 },
      "data": {
        "labelCn": "聚合统计",
        "type": "script",
        "script": "function main(ctx) {\n  const name = ctx.data_proc_1.output.upperName ?? '';\n  const detail = ctx.conn_1.output.body;\n  const hasDetail = !!detail;\n  return { name, hasDetail };\n}",
        "output": {
          "type": "object",
          "properties": {
            "name":     { "type": "string" },
            "hasDetail": { "type": "boolean" }
          }
        },
        "structConfig": { "parentLoopV2GroupId":"loop_1","parentLoopV2Role":"right-column-node" }
      }
    },

    // ==================== 嵌套错误处理 (5 nodes，位于循环体内部) ====================
    { "id":"err_1",        "type":"error-handler", "position":{"x":760,"y":620}, "data":{ "type":"errorHandler", "labelCn":"错误处理",    "structConfig":{ "loopV2GroupId":"err_1","parentLoopV2GroupId":"loop_1" }, "errors":{ "timeout":{ "strategy":"retry","retries":3,"interval":10 }, "connection_error":{ "strategy":"retry","retries":3,"interval":10 } } } },
    { "id":"err_region_1", "type":"text",          "position":{"x":500,"y":760}, "data":{ "type":"text",          "labelCn":"错误处理区域","structConfig":{ "loopV2GroupId":"err_1","loopV2Role":"region" } } },
    { "id":"err_start_1",  "type":"text",          "position":{"x":1010,"y":760},"data":{ "type":"text",          "labelCn":"错误处理开始","structConfig":{ "loopV2GroupId":"err_1","loopV2Role":"start" } } },
    // err body: retry connector
    {
      "id": "conn_retry", "type": "connector",
      "position": { "x": 1260, "y": 760 },
      "data": {
        "labelCn": "重试请求", "type": "connector",
        "connectorId": "1234567890123456000",
        "connectorVersionId": "1234567890123456789",
        "connectorVersionConfig": { "protocol": "HTTP", "protocolConfig": { "url": "https://api.example.com/item/detail", "method": "GET" }, "authConfigs": [{ "type": "SOA", "header": { "type": "object", "properties": { "X-Soa-Token": { "type": "string", "required": true, "sensitive": true, "value": "${$.system.env.soaToken}" } } } }], "input": { "protocol": "HTTP", "body": { "type": "object", "properties": { "itemId": { "type": "string", "required": true } } } }, "output": { "protocol": "HTTP", "body": { "type": "object", "properties": { "name": { "type": "string" } } } }, "timeoutMs": 5000 },
        "input": { "body": { "type": "object", "properties": { "itemId": { "type":"string","value":"${$.node.loop_1.current.item}" } } } },
        "structConfig": { "parentLoopV2GroupId":"err_1","parentLoopV2Role":"right-column-node" }
      }
    },
    { "id":"err_end_1",    "type":"text",          "position":{"x":1010,"y":920},"data":{ "type":"text",          "labelCn":"错误处理结束","structConfig":{ "loopV2GroupId":"err_1","loopV2Role":"end" } } },
    { "id":"err_break_1",  "type":"text",          "position":{"x":760,"y":1000},"data":{ "type":"text",          "labelCn":"错误处理跳出","structConfig":{ "loopV2GroupId":"err_1","loopV2Role":"break" } } },

    // ↑ 循环体出口
    { "id":"loop_end_1",   "type":"text", "position":{"x":760,"y":1160}, "data":{ "type":"text", "labelCn":"循环结束",  "structConfig":{ "loopV2GroupId":"loop_1","loopV2Role":"end" } } },
    { "id":"loop_break_1", "type":"text", "position":{"x":250,"y":1160}, "data":{ "type":"text", "labelCn":"循环跳出",  "structConfig":{ "loopV2GroupId":"loop_1","loopV2Role":"break" } } },

    // ==================== 流程出口 ====================
    {
      "id": "node_exit", "type": "exit",
      "position": { "x": 250, "y": 1320 },
      "data": {
        "labelCn": "返回结果", "type": "exit",
        "output": { "body": { "type": "object", "properties": { "count": { "type":"integer","value":"${$.node.loop_1.current.total}" } } } }
      }
    }
  ],
  "edges": [
    // trigger → loop
    { "id":"e_t_loop",       "source":"node_trigger", "target":"loop_1",        "data":{"businessType":"default",    "connectionMode":"serial" } },

    // loop 辅助边 (isStructural)
    { "id":"e_loop_rgn",     "source":"loop_1",       "target":"loop_region_1", "data":{"isStructural":true } },
    { "id":"e_rgn_brk",      "source":"loop_region_1","target":"loop_break_1",  "data":{"isStructural":true } },
    // loop 入口 → 循环体
    { "id":"e_loop_start",   "source":"loop_1",       "target":"loop_start_1",  "data":{"isStructural":true } },
    { "id":"e_s_conn",       "source":"loop_start_1", "target":"conn_1",        "data":{"businessType":"loop_entry","iterationVar":"item" } },
    // 循环体：connector → data_processor → script → error_handler
    { "id":"e_c_dp",         "source":"conn_1",       "target":"data_proc_1",   "data":{"businessType":"default",    "connectionMode":"serial" } },
    { "id":"e_dp_sc",        "source":"data_proc_1",  "target":"script_1",      "data":{"businessType":"default",    "connectionMode":"serial" } },
    { "id":"e_sc_err",       "source":"script_1",     "target":"err_1",         "data":{"businessType":"error",      "connectionMode":"serial" } },

    // error_handler 内部 (isStructural + error body)
    { "id":"e_err_rgn",      "source":"err_1",        "target":"err_region_1",  "data":{"isStructural":true } },
    { "id":"e_err_rgn_brk",  "source":"err_region_1", "target":"err_break_1",   "data":{"isStructural":true } },
    { "id":"e_err_start",    "source":"err_1",        "target":"err_start_1",   "data":{"isStructural":true } },
    { "id":"e_err_s_retry",  "source":"err_start_1",  "target":"conn_retry",    "data":{"businessType":"default",    "connectionMode":"serial" } },
    { "id":"e_retry_end",    "source":"conn_retry",   "target":"err_end_1",     "data":{"businessType":"default",    "connectionMode":"serial" } },
    { "id":"e_err_end_brk",  "source":"err_end_1",    "target":"err_break_1",   "data":{"isStructural":true } },
    // error_handler → 回到循环体
    { "id":"e_err_brk_end",  "source":"err_break_1",  "target":"loop_end_1",    "data":{"businessType":"always",      "connectionMode":"serial" } },

    // loop 出口
    { "id":"e_end_brk",      "source":"loop_end_1",   "target":"loop_break_1",  "data":{"isStructural":true } },
    // loop → exit
    { "id":"e_brk_exit",     "source":"loop_break_1", "target":"node_exit",     "data":{"businessType":"default",    "connectionMode":"serial" } }
  ],
  "flowConfig": {
    "rateLimitConfig": { "maxQps": 50, "maxConcurrency": 5 },
    "cache": {
      "key": ["${$.node.trigger.input.header.Authorization}", "${$.constant:batch_query}"],
      "ttl": 30
    }
  }
}
```

> **结构解析示意**：
> ```
> trigger → loop ──┬── region ──────────────────────────▶ break ──▶ exit
>                  └── start
>                       │   // ====== 循环体 ======
>                       ├── conn_1 (获取详情)
>                       ├── data_proc_1 (格式化)
>                       ├── script_1 (聚合统计)
>                       └── err_1 ──┬── region ──▶ break
>                                   └── start → conn_retry → end ──▶ break
>                                           // ====== err body ======
>                       │   // ====== 循环体结束 ======
>                       └── end ──▶ break
> ```


## 7. 执行数据约定

执行数据的结构由对应节点的 input / output 动态决定，不在数据库层约束。errorInfo 使用结构化格式（定义见 §4.3.4）。
示例：
```json
// 下游调用失败
{ "code":"503", "messageZh":"下游服务不可用", "messageEn":"Downstream Service Unavailable", "downstreamStatus":503 }
// 内部错误
{ "code":"6001", "messageZh":"字段映射失败", "messageEn":"Field Mapping Failed", "cause":"source 字段 ${node_1.msgId} 不存在" }
```

---

## 附录 A：DAG 编排演进路线图

```mermaid
flowchart LR
    subgraph MVP["MVP (v1.0)"]
        M1["拓扑: 线性链"]
        M2["businessType: [default]"]
        M3["节点: trigger, connector, data_processor, exit"]
        M4["执行: for 循环串行"]
    end
    subgraph V2["V2 (当前)"]
        V2_1["拓扑: DAG（分支+并行+循环）"]
        V2_2["businessType: +condition, error, loop_entry, loop_exit"]
        V2_3["connectionMode: serial, parallel"]
        V2_4["节点: +loop_v2, parallel, condition_branch, error_handler, text"]
        V2_5["执行: 拓扑层级并行 + 结构体迭代"]
    end
    subgraph V3["V3+"]
        V3_1["拓扑: DAG（子流嵌套）"]
        V3_2["节点: +iterator, sub_flow"]
        V3_3["执行: 事件驱动+背压"]
    end
    MVP --> V2 --> V3
    Foundation["数据结构不变: nodes[] + edges[]<br/>只增字段，不删不改，向后兼容"]
    MVP -.-> Foundation
    V2 -.-> Foundation
    V3 -.-> Foundation
```

| 决策 | 选择 | 理由 |
| **边语义化** | edge.data 承载完整控制流 | businessType 区分路由，connectionMode 区分串行并行，isStructural 标记辅助边 |
| **node.type 纯渲染** | node.type 仅决定 React Flow 组件，业务语义进 node.data | 框架/业务严格分离 |
| **text 标记持久化** | 全量持久化，执行时过滤 | 零翻译、版本 diff 清晰 |
| **V2 循环/并行** | 通过结构节点 + edge 语义组合 | 不引入新数据结构层次 |

---

## 附录 B：版本演进规则

| 场景 | 处理方式 |
|------|---------|
| **新增可选字段** | 直接加，不影响已有数据 |
| **新增必填字段** | 发新版本，旧数据迁移赋默认值 |
| **字段改名** | ❌ 不允许，废弃旧字段 + 新增新字段 |
| **字段废弃** | 保留字段名，标注 deprecated + replacedBy |
| **枚举值新增** | 直接加，应用层做好未知值降级 |
| **枚举值删除** | ❌ 不允许，标记为 deprecated |
| **node.type 扩展** | 新增类型直接加枚举值，旧编排不受影响 |
| **edge.data 字段扩展** | 新增字段使用可选类型 + 默认值 |
> **向后兼容**：加不加删、改不删。

---

## 附录 C：React Flow 标准格式参考

### C.1 React Flow 标准 Node 与 Edge 接口
React Flow (@xyflow/react v12) 是画布引擎，不管业务逻辑。
| React Flow 管的（框架层） | 业务自己管的（应用层） |
|---|---|
| `node.id`、`node.position`、`node.type` | `node.data` 里的一切 |
| 拖拽、选中、连线、Handle 交互 | 节点长什么样 |
| 边的渲染样式 | 节点间数据流转逻辑 |
**Node 接口**（简化）：
```typescript
interface Node<TData> {
  id: string;
  type: string;            // 映射到注册的 React 组件名
  position: { x: number; y: number };
  data: TData;             // 所有自定义业务数据
}
```

**Edge 接口**（简化）：
```typescript
interface Edge<TData> {
  id: string;
  source: string;          // 源节点 ID（固定字段名）
  target: string;          // 目标节点 ID（固定字段名）
  type?: string;           // 边框样式
  data?: TData;            // V12 业务扩展槽
}
```

### C.2 `node.type` 的语义边界
React Flow 中 `node.type` 仅是一个字符串，在 `nodeTypes` 注册表中查找对应组件。本项目注册的节点类型（V2），以及对应的 `node.data.type`（引擎执行路由）：

| node.type（框架层） | React 组件渲染 | node.data.type（业务层） | 业务数据关键字段 |
|------|------|------|------|
| `trigger` | 触发器 UI | `trigger` | triggerType（http/manual）、authConfigs、input |
| `connector` | 连接器 UI | `connector` | connectorVersionId、input |
| `script` | 脚本编辑器 UI | `script` | script、output、timeout |
| `data_processor` | 数据处理器 UI | `dataProcessor` | output |
| `exit` | 出口 UI | `exit` | output |
| `loop-v2` | 循环结构框架 | `loopV2` | structConfig = { loopV2GroupId } |
| `parallel` | 并行结构框架 | `parallel` | structConfig = { parallelGroupId, parallelRole: "root" } |
| `condition-branch` | 条件分支框架 | `conditionBranch` | structConfig = { parallelGroupId, parallelRole: "root" } |
| `error-handler` | 错误处理框架 | `errorHandler` | structConfig = { loopV2GroupId } |
| `text` | 纯文本标签 | `text` | structConfig 承载归属关系，不参与执行 |

---

## 附录 D：决策记录

| 决策项 | 选择 | 理由 | 状态 |
|-------|------|------|:---:|
| 存储格式 | React Flow 原生格式（node.data + edge.source/target） | DAG 编辑器是 React Flow，零翻译 | ✅ |
| 字段过滤 | JSON Schema `additionalProperties: false` | 编译期保证 | ✅ |
| Node type 枚举 | V2 扩展至 9 种（含结构节点 + text） | 纯渲染类型，业务语义进 data | ✅ |
| Edge 语义拆分 | `edge.type` → 渲染；`edge.data.*` → 业务 | 遵循 React Flow 规范 | ✅ |
| node.data 路由 | nodeDataDef oneOf 9 分支 | V2 新增 loop/parallel/condition-branch/error-handler 四个结构节点 + text 标记节点 | ✅ |
| 文档结构 | 正文按认知递进，参考材料归附录 | 减少阅读跳转 | ✅ |
| 值表达式体系 | 5 种值来源统一 `${$.scope.path}` 语法（node/constant/system/script/execution），node 下分 input/output/current | 覆盖全量设计态 + 运行时场景 | ✅ v7.2 |
| 入参出参按协议区分 | 删除 dataContractDef，新增协议专用 contract + 路由器 | 消除协议无感问题 | ✅ |
| 结构节点 data | 新增 loopNodeDataDef / parallelNodeDataDef / conditionBranchNodeDataDef / errorHandlerNodeDataDef，均继承 nodeDataBaseDef | 先定义框架，各节点独有字段待专项细化 | ✅ |
| text 标记持久化 | 全量持久化，执行时引擎过滤 | 零翻译、diff 清晰 | ✅ |
| DAG 拓扑约束 | 移除 MVP 线性限制，新增 V2 结构体完整性/并行一致性 | V2 不再是线性 DAG | ✅ |

---

## 附录 E：修订记录

| 版本 | 日期 | 修订内容 |
|------|------|---------|
| v1.0 | 2026-05-22 | 初始版本 |
| v2.0 | 2026-05-22 | 标准 JSON Schema 格式重写 |
| v3.0 | 2026-05-22 | 审查修复：definitions 聚合段、字段过滤等 15 项 |
| v3.1–v3.5 | 2026-05-25 | React Flow 格式对齐 |
| v4.0 | 2026-05-25 | React Flow 原生格式全文档对齐 |
| v4.1 | 2026-05-25 | 字段命名体系重构 |
| v5.0 | 2026-05-26 | 按存储目标与框架归属结构性重构 |
| v5.1 | 2026-05-26 | 入参出参协议感知重构 |
| v5.2 | 2026-05-26 | errorInfoDef 重构 + 删除 positionDef |
| v5.3 | 2026-05-26 | triggerNodeDataDef 移除 test |
| v5.4 | 2026-05-26 | 节点间传值映射重构 |
| v5.5 | 2026-05-26 | triggerNodeDataDef 两处同步修复 |
| v5.6 | 2026-05-27 | 映射字段结构化：mappedFieldDef / jsonObjectDef |
| v6.0 | 2026-06-09 | V2 增量对齐规划（未执行到正文） |
| v7.0 | 2026-06-10 | V2 全量重写：node.type 9 种 + oneOf 9 分支 + edge.data 5 字段 + DAG 约束重设计 |
| v7.1 | 2026-06-10 | 章节重构：正文按认知递进，§7-§11 降级为附录 A-D，新增组件速查表，§6 精简 |
| v7.2 | 2026-06-10 | §1.4 值表达式体系独立为 §3（原 §3→§4，§4→§5，§5→§6，§6→§7）；§1.4 精简为简述；值来源定为 5 种（node/constant/system/script/execution），node 下分 input/output/current；新增内置函数/自定义脚本/嵌套规则章节 |
| v7.3 | 2026-06-10 | 全文对齐 §1-3 最新内容：§1.4 核心要点重写、嵌套规则/编排示例修复过期引用、附录 8 种→5 种 |
| v8.0 | 2026-06-11 | 组件命名规范化：rateLimitDef→rateLimitConfigDef、httpInputContractDef→httpInputDef、httpOutputContractDef→httpOutputDef、inputContract/outputContract→nodeInput/nodeOutput、第二层统一 {type}NodeDataDef；§4.1 命名规则 4 条按层归类；§4.2 速查表新增「层」列；§4 章节重排（设计思路→速查表→JSON 聚合→详解） |
| v8.1 | 2026-06-11 | §4.4 全部 15 节重构为三段式（> **Def** → **字段说明** → **示例**），各节自包含 Def 不依赖 §4.3 聚合块；§1.3 核心原则表格化；§1.4 升级为值表达式体系独立 §3；§5 新增完整连接器配置示例 |
| v8.2 | 2026-06-11 | 第四层精简：合并 mappedFieldDef + mappedJsonSchemaObjectDef → jsonObjectDef v2（value 可选，递归嵌套）；组件数 17→15；jsonSchemaObjectDef→jsonObjectDef |
| v8.3 | 2026-06-11 | 删除 nodeInputDef/nodeOutputDef（单协议无存在价值），$ref 直指 httpInputDef/httpOutputDef；组件数 15→13；附录 A~E 格式修复 |
| v9.0 | 2026-06-11 | **V2 全量缺口修复**：① authConfigDef v2 重构（fields[]→header/query 复用 jsonObjectDef，新增 COOKIE/SIGNATURE）② 认证多选数组化（authConfig→authConfigs）③ sysAccountWhitelist（SYSTOKEN 账号白名单）④ dataProcessorNodeDataDef 重构（output→jsonObjectDef）+ Mapping 后缀移除 ⑤ 类型转换函数 4 个 + 全 18 函数路径对齐项目包 ⑥ 顶层 flowConfig（限流+缓存）⑦ 连接器节点超时 ⑧ 枚举统一 kebab-case ⑨ nullable 描述澄清 ⑩ structConfig 重构：回归 nodeDataBaseDef 基类，拆分 structureNodeDataDef → loop/parallel/conditionBranch/errorHandler 四个独立 Def，均继承基类 |
| v9.1 | 2026-06-11 | nodeDataBaseDef 重新引入 structConfig 作为 DAG 拓扑配置基类字段；structureNodeDataDef 拆分为 4 种独立节点 Def；nodeDataDef 路由扩展至 9 分支 |
| v9.2 | 2026-06-11 | 结构节点 Def 深度完善：依据前端 FlowCanvas 持久化数据补充 loop/parallel 完整 nodes+edges 示例（含 trigger/exit 上下文），condition-branch/error-handler 标注同构引用；textMarkerNodeDataDef → textNodeDataDef 命名简化 |
| v9.3 | 2026-06-11 | §6 全线对齐：mermaid 路由计数 7→9、§6.7.2 循环编排示例 structConfig 补全 + connector 节点嵌套归属标记、附录 C 组件映射表更新 kebab-case + structConfig 字段说明 |
| v9.4 | 2026-06-12 | connectionConfig: nodeInput/nodeOutput→input/output；protocolConfig 删除 headers（由 input.header 覆盖）；§5.4 示例改为 SOA+COOKIE 双认证含 value 表达式 |
| v9.5 | 2026-06-12 | §4.3.8 补充两层 type 对照表；§6.7.2 示例增强（新增 data_processor + 嵌套 error_handler）；§4.3.11 标记 V2 新增 |
| v9.6 | 2026-06-12 | 章节重排：exit(10)/dataProcessor(11)、textNodeDataDef(4.3.15 紧邻 loop)、errorHandlerNodeDataDef(4.3.14 紧邻 loop 前) |
| v9.7 | 2026-06-12 | errorHandlerNodeDataDef 按 spec FR-039a 重构：新增 successCondition(checkField+expectedValue) + failureResponse(复用 jsonObjectDef)；§6.7.2 示例同步 |
| v9.8 | 2026-06-12 | §6 章节重构：Schema 组装提前到 §6.2、框架层/业务层按字段路径合并至 §6.3、新增 §6.3.3 flowConfig 独立说明、§6.6 执行模型降级为附录 F；§5.4 示例增强为完整 HTTP 入参三段式+出参两段式；§4.2 速查表/§4.3.19 路由表引用号修复；§4.3.16 textNodeDataDef 重复内容删除 |
| v9.9 | 2026-06-12 | **双层 type 规范化**：① triggerNodeDataDef 拆分 `triggerType`（http/manual），`node.data.type` 恢复为固定值 `"trigger"`；② nodeDataBaseDef type enum 中 kebab-case 值改为 camelCase（`loopV2`/`errorHandler`/`conditionBranch`），严格遵循 §2.1；③ nodeDataDef oneOf 描述措辞修正（`node.type`→`node.data.type`）；④ §4.3.8/§6.3.1/附录 C.2 新增完整 9 型双层 type 映射表 |
| v9.10 | 2026-06-15 | **errorHandler 策略模型重构（对齐 spec v2.22 FR-039a）**：① §4.3.14 从旧 try-catch 模型（successCondition + failureResponse）重构为 retry/ignore/terminate 策略模型 ② 新增 strategy（三选一）、errorTypes（all 与其他互斥）、retryConfig（maxRetries 1~5 / retryInterval 1~300）③ §6.7.2 循环编排示例中的 error_handler 节点数据同步更新 |
| v9.11 | 2026-06-25 | **connectorVersionConfigDef 抽取 + connectorNodeDataDef v2 重构**：① 新增 `connectorVersionConfigDef` 共享组件（§4.3.7），连接器配置唯一定义源（含 timeoutMs / rateLimitConfig）② §5.2 `connectionConfig` 改为 `$ref` connectorVersionConfigDef，消除重复定义 ③ `connectorNodeDataDef`（§4.3.10）新增 `connectorId` + `connectorVersionConfig`（`$ref` connectorVersionConfigDef）快照，移除节点层 `rateLimitConfig` ④ §4.1/§4.2/§4.4/§5.3 同步更新 ⑤ 组件总数 17→18 |

---

## 附录 F：编排执行模型

> 以下为引擎执行 DAG 编排的参考流程，非数据结构 Schema 规范。

### 执行步骤

1. 从 `nodes[]` 中过滤掉 `node.type === "text"` 的标记节点
2. 从 `edges[]` 中过滤掉 `edge.data.isStructural === true` 的辅助边
3. 按过滤后的 nodes + edges 构建邻接表
4. Kahn 算法拓扑排序 → 分层执行队列
5. 同层无依赖节点通过 WebFlux `Mono.when()` 并发执行（`connectionMode` 允许时）
6. 结构体主节点（loop-v2 / parallel / error-handler 等）按其 config 执行迭代/并发/分发策略

```mermaid
graph TB
    subgraph Execution["DAG 执行流程"]
        S1["① 过滤 text 节点 + isStructural 边"] --> S2["② 计算入度"]
        S2 --> S3["③ 入度=0 入队（必定是 trigger）"]
        S3 --> S4["④ 依次出队执行，后继入度-1"]
        S4 --> S5["⑤ 入度=0 的后继入队"]
        S5 --> S6["⑥ 直到队列为空"]
    end
```

---

> **相关文档**: plan.md, plan-db.md, plan-api.md  
> **V1 备份**: `.sddu/specs-tree-root/specs-tree-connector-platform/plan-json-schema.md`
