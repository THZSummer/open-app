# API 接口设计：调用连接流（#55 invoke）

**Feature ID**: CONN-PLAT-002  
**关联文档**: plan-api.md §3.9 #55, plan-error.md §3.2, plan-flow-invoke-temp.md, ADR-008  
**版本**: v1.0  
**创建日期**: 2026-07-28  

---

## 1. 接口概述

### 1.1 基本信息

| 维度 | 说明 |
|------|------|
| **端点** | `POST /api/v1/flows/{flowId}/invoke` |
| **模块** | `connector-api`（端口 18180） |
| **认证** | SYSTOKEN（`X-Sys-Token` 请求头），必须在触发器白名单内 |
| **调用方式** | 同步阻塞（等待 DAG 编排完整执行后返回） |
| **设计模式** | **透明穿透**（Transparent Passthrough）— 请求/响应均由用户自定义，平台元数据通过 `X-` 前缀响应头携带 |

### 1.2 设计理念

`#55` 接口采用**透明穿透**模式——除了 URL 路径由平台固定，请求和响应的所有参数、返回值均由用户在连接流中自定义：

- **请求侧**：完全遵循触发器节点的 `input` (httpInputDef) 定义。Header、Query、Body 均由触发器 Schema 决定。
- **响应侧**：完全遵循出口节点的 `output` (httpOutputDef) 定义。Body 即为出口节点出参数据，无任何平台信封包装。
- **平台元数据**：执行 ID、状态、耗时、错误信息等统一放在 `X-` 前缀响应头，与用户数据完全分离。

> **不使用** `{ code, messageZh, messageEn, data, page }` 标准信封。

### 1.3 前置条件

调用方必须满足以下 4 项前置条件才能成功调用：

| # | 条件 | 校验时机 | 不满足时 |
|---|------|:---:|------|
| 1 | 连接流存在 | 前置 | HTTP 404 |
| 2 | `flow.lifecycleStatus = 2`（运行中） | 前置 | HTTP 409 |
| 3 | 已部署版本可用（`deployedVersionId` 非空，版本未失效） | 前置 | HTTP 422 |
| 4 | `X-Sys-Token` 在触发器 `authConfig.sysAccountWhitelist` 白名单内 | 前置 | HTTP 401 |
| 5 | 未超过入站限流阈值（`flowConfig.rateLimitConfig.maxQps`） | 前置 | HTTP 429 |
| 6 | 触发器 `inputContract` 校验通过（header/query/body 三段契约） | 前置 | HTTP 400 |

---

## 2. 请求规范

### 2.1 URL 路径

```
POST /api/v1/flows/{flowId}/invoke
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| `flowId` | Long | ✅ | 连接流 ID（雪花 ID），如 `340518008730419200` |

### 2.2 请求头 — 平台认证

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| `X-Sys-Token` | string | ✅ | SYSTOKEN 凭证，必须在触发器的 `sysAccountWhitelist` 内 |

> 触发器的 `authConfig.sysAccountWhitelist` 配置示例：`["tester", "admin"]`

### 2.3 请求头 — 用户自定义

由触发器节点的 `input.header` (httpInputDef.header) 定义。调用方按 Schema 传入，字段需满足必填/可选/类型约束（FR-047）。

示例（触发器配置了 `X-App-Id`、`Cookie`、`X-XSRF-TOKEN` 三个 header 字段）：

```
X-App-Id: 20250730213114178360970
Cookie: user_id=admin001
X-XSRF-TOKEN: user_id=admin001
```

### 2.4 查询参数 — 用户自定义

由触发器节点的 `input.query` (httpInputDef.query) 定义。调用方以标准 URL query string 传入。

```
?curPage=1&pageSize=10&keyword=test
```

### 2.5 请求体 — 用户自定义

由触发器节点的 `input.body` (httpInputDef.body) 定义。`Content-Type` 固定为 `application/json`。

```json
{"echoToHeader": "hello-from-curl"}
```

---

## 3. 响应规范

### 3.1 响应格式总览

响应分为三部分，互不干扰：

```
HTTP/1.1 200 OK
X-Flow-Id: 340518008730419200       ← 平台元数据 (X- 前缀)
X-Execution-Id: abc123...
X-Status: 0
X-Duration-Ms: 194
X-Code: 200
X-Message-Zh: Flow not running      ← 仅前置校验失败时出现
X-Cache-Status: 0
Echo-To-Header: hello               ← 用户自定义响应头 (出口节点 output.header)

{"code":"200","page":{...}}          ← 响应体 = 出口节点 output.body (裸数据)
```

### 3.2 平台元数据响应头

| Header | 类型 | 说明 | 出现条件 |
|--------|------|------|---------|
| `X-Flow-Id` | string | 连接流 ID（雪花 ID） | **始终返回** |
| `X-Execution-Id` | string | 执行记录 ID | 连接流已执行（含失败） |
| `X-Status` | int | `0`=成功 / `1`=失败 | 连接流已执行 |
| `X-Duration-Ms` | int | 执行耗时（毫秒） | 连接流已执行 |
| `X-Cache-Status` | int | `0`=未命中 / `1`=全流命中 | 缓存生效时 |
| `X-Code` | string | 平台结果码，见 §4 错误码 | **始终返回** |
| `X-Message-Zh` | string | 提示信息（英文，HTTP 头仅支持 ASCII） | **始终返回** |
| `X-Message-En` | string | 英文提示信息 | **始终返回** |
| `X-Error-Node` | string | 失败节点 ID | 执行失败时 |
| `X-Error-Node-Type` | string | 失败节点类型 | 执行失败时 |

> ⚠️ **关于 `X-Message-Zh`**：由于 HTTP 协议头仅支持 ASCII 字符，中文会被 Netty 转为 `?`，当前实现中 `X-Message-Zh` 与 `X-Message-En` 使用相同的英文消息。详细技术背景见 [plan-flow-invoke-temp.md](./plan-flow-invoke-temp.md)。

> 前置校验失败时，连接流未实际执行，`X-Execution-Id`、`X-Status`、`X-Duration-Ms` 不出现。`X-Flow-Id`、`X-Code`、`X-Message-Zh`、`X-Message-En` 始终返回。
>
> 执行层错误时（连接流已执行但节点失败/超时），`X-Execution-Id`、`X-Status`（1 或 2）、`X-Duration-Ms`、`X-Error-Node`、`X-Error-Node-Type` 均会出现。

### 3.3 用户自定义响应头

由出口节点的 `output.header` (httpOutputDef.header) 定义，值来源于值表达式体系解析结果。不会与 `X-` 前缀平台头冲突。

### 3.4 响应体

由出口节点的 `output.body` (httpOutputDef.body) 定义，**不经过任何平台信封包装**。前置校验失败时 Body 为空（`content-length: 0`）。

---

## 4. 错误码清单

### 4.1 错误码分层体系

| 码段 | 层级 | 含义 | HTTP Status |
|:---:|------|------|:---:|
| `200` | — | 执行完成（含业务失败） | 200 |
| `400` | 前置校验 | 请求参数不合法 | 400 |
| `401` | 前置校验 | SYSTOKEN 认证失败 | 401 |
| `403` | 前置校验 | URL 白名单拒绝 | 403 |
| `404` | 前置校验 | 资源不存在 | 404 |
| `409` | 前置校验 | 状态冲突（如流未运行） | 409 |
| `422` | 前置校验 | 前置条件不满足 | 422 |
| `500` | 兜底 | 内部未知错误 | 500 |
| `61xxx` | 执行层 | 编排/配置错误 | 400 (X-Status: 1) |
| `62xxx` | 执行层 | 连接器节点错误 | 400 (X-Status: 1) |
| `63xxx` | 执行层 | 脚本节点错误 | 400 (X-Status: 1) |
| `64xxx` | 执行层 | 超时 | 400 (X-Status: 2) |
| `65xxx` | 执行层 | 并行节点错误 | 400 (X-Status: 1) |
| `66xxx` | 执行层 | 出口节点错误 | 400 (X-Status: 1) |

> 💡 **关键区分**：前置校验失败 → 400/401/403 + `X-Code` 错误码 + 空 Body；执行层错误 → **同样 400**（用户连接流配置/运行时问题） + `X-Code` 细分码 + 出口 body（按 errorHandler 策略产出）。500 仅平台内部异常时触发。

### 4.2 前置校验错误

#### 400 — 请求参数错误

| X-Code | 场景 | 消息（X-Message-En） |
|:---:|------|------|
| `400` | 请求参数缺失或格式不合法 | `Bad request` |
| `400` | 触发器 `inputContract` 校验失败（header/query/body 三段契约不匹配） | `Bad request` |
| `400` | 触发方式未知（仅支持 `http` / `manual`） | `Bad request` |
| `400` | HTTP 触发器缺少 `input` 契约 | `Bad request` |

#### 401 — 认证失败

| X-Code | 场景 | 消息（X-Message-En） |
|:---:|------|------|
| `401` | `X-Sys-Token` 不在触发器 `sysAccountWhitelist` 白名单内 | `Authentication failed` |
| `401` | `X-Sys-Token` 缺失或过期 | `Authentication failed` |

#### 403 — 权限拒绝

| X-Code | 场景 | 消息（X-Message-En） |
|:---:|------|------|
| `403` | 连接器调用的目标 URL 未通过白名单校验 | `URL whitelist denied` |

#### 404 — 资源不存在

| X-Code | 场景 | 消息（X-Message-En） |
|:---:|------|------|
| `404` | 连接流不存在或已被删除 | `Flow not found` |
| `404` | 连接器不存在或已被删除 | `Flow not found` |
| `404` | 连接器版本不存在 | `Flow not found` |

#### 409 — 状态冲突

| X-Code | 场景 | 消息（X-Message-En） |
|:---:|------|------|
| `409` | 连接流未启动（`lifecycleStatus ≠ 2`） | `Flow not running` |

#### 422 — 前置条件不满足

| X-Code | 场景 | 消息（X-Message-En） |
|:---:|------|------|
| `422` | 已部署版本不可用（版本已被失效） | 见错误详情 |
| `422` | 连接器版本已失效 | 见错误详情 |
| `422` | 连接器已失效 | 见错误详情 |

#### 500 — 内部错误（兜底）

| X-Code | 场景 | 消息（X-Message-En） |
|:---:|------|------|
| `500` | 编排配置无节点 | `Trigger execution failed` |
| `500` | 编排配置无触发器节点 | `Trigger execution failed` |
| `500` | 无法归类的运行时异常 | `Trigger execution failed` |

### 4.3 执行层错误（HTTP 200 + X-Status: 1/2）

执行层错误发生在 DAG 编排执行过程中。与前置校验相同，HTTP Status 返回 `400`（用户侧问题），通过 `X-Status`（1=失败/2=超时）和 `X-Code` 携带细分错误码。

#### 编排通用错误 (61xxx / 60xxx)

| X-Code | 场景 | 消息模板（X-Message-En） |
|:---:|------|------|
| `61001` | 编排配置 JSON 解析失败 | `Trigger execution failed` |
| `61002` | 编排中缺少触发器节点 | `Trigger execution failed` |
| `61003` | 编排中缺少出口节点 | `Trigger execution failed` |
| `61004` | 节点间边关系缺失 | `Trigger execution failed` |
| `60000` | DAG 执行整体失败 | `Trigger execution failed` |
| `60001` | 节点执行失败（通用兜底） | `Trigger execution failed` |
| `60002` | 节点超时或错误 | `Trigger execution failed` |

#### 触发器节点 (6101x)

| X-Code | 场景 | 消息模板 |
|:---:|------|------|
| `61010` | 触发方式未配置（`data.triggerType` 缺失） | `Trigger execution failed` |
| `61011` | SYSTOKEN 凭证不存在或已过期 | `Trigger execution failed` |
| `61012` | 调用凭证不在白名单中 | `Trigger execution failed` |

#### 连接器节点 — 配置错误 (6102x)

| X-Code | 场景 | 消息模板 |
|:---:|------|------|
| `61020` | 未选择连接器（`connectorId` 缺失） | `Trigger execution failed` |
| `61021` | 未选择连接器版本（`connectorVersionId` 缺失） | `Trigger execution failed` |
| `61022` | 节点超时值超过应用上限 | `Trigger execution failed` |
| `61023` | 入参映射引用了不存在的字段 | `Trigger execution failed` |
| `61024` | 连接器缺少认证配置 | `Trigger execution failed` |
| `61025` | 认证类型未选择 | `Trigger execution failed` |

#### 连接器节点 — 运行时错误 (62xxx)

| X-Code | 场景 | 消息模板 |
|:---:|------|------|
| `62001` | HTTP 调用下游失败（含下游 statusCode + 截断 body） | `Trigger execution failed` |
| `62002` | 连接目标超时（TCP 连接不可达） | `Trigger execution failed` |
| `62003` | 读取超时（下游未在规定时间内响应） | `Trigger execution failed` |
| `62004` | DNS 解析失败（目标 host 不存在） | `Trigger execution failed` |
| `62005` | SSL 证书校验失败 | `Trigger execution failed` |
| `62006` | 请求参数序列化失败 | `Trigger execution failed` |
| `62007` | 下游响应体超过限制 | `Trigger execution failed` |

#### 脚本节点 — 配置错误 (6103x)

| X-Code | 场景 | 消息模板 |
|:---:|------|------|
| `61030` | 脚本源码为空 | `Trigger execution failed` |
| `61031` | 脚本源码超过字符上限 | `Trigger execution failed` |
| `61032` | 缺少 `main(ctx)` 函数定义 | `Trigger execution failed` |
| `61033` | 脚本语法错误 | `Trigger execution failed` |

#### 脚本节点 — 运行时错误 (63xxx)

| X-Code | 场景 | 消息模板 |
|:---:|------|------|
| `63001` | 脚本执行时抛出异常（含错误详情） | `Trigger execution failed` |
| `63002` | 脚本执行超时（超过节点 `timeoutMs`） | `Trigger execution failed` |
| `63003` | 脚本执行超过语句上限 | `Trigger execution failed` |
| `63004` | 脚本返回值不是 Object 类型 | `Trigger execution failed` |
| `63005` | 脚本访问了不存在的上游字段 | `Trigger execution failed` |

#### 超时 (64xxx)

| X-Code | 场景 | 消息模板 | X-Status |
|:---:|------|------|:---:|
| `64000` | 单节点执行超时 | `Trigger execution failed` | 2 |

#### 并行节点 — 配置错误 (6104x)

| X-Code | 场景 | 消息模板 |
|:---:|------|------|
| `61040` | 分支数不足（最少 2 个） | `Trigger execution failed` |
| `61041` | 分支数超过上限（最多 8 个） | `Trigger execution failed` |
| `61042` | 分支内无节点 | `Trigger execution failed` |

#### 并行节点 — 运行时错误 (65xxx)

| X-Code | 场景 | 消息模板 |
|:---:|------|------|
| `65001` | 并行分支执行失败 | `Trigger execution failed` |
| `65002` | 并行分支执行超时 | `Trigger execution failed` |
| `65003` | 所有并行分支均失败 | `Trigger execution failed` |

#### 出口节点 — 配置错误 (6105x)

| X-Code | 场景 | 消息模板 |
|:---:|------|------|
| `61050` | 输出映射引用了不存在的字段 | `Trigger execution failed` |
| `61051` | 输出映射格式错误 | `Trigger execution failed` |

#### 出口节点 — 运行时错误 (66xxx)

| X-Code | 场景 | 消息模板 |
|:---:|------|------|
| `66001` | 出口响应体序列化失败 | `Trigger execution failed` |
| `66002` | 出口响应头设置失败 | `Trigger execution failed` |

### 4.4 HTTP Status → X-Code 映射规则

#### 设计原则

HTTP Status 仅区分**责任方**：200 成功 / 400 用户侧问题 / 401-403 鉴权问题 / 500 平台问题。具体错误原因通过 `X-Code` 承载。

| HTTP Status | 含义 | 责任方 | 判定口径 |
|:---:|------|:---:|------|
| `200` | 执行成功 | — | DAG 编排完整执行完毕，节点无失败（X-Status: 0） |
| `400` | 请求不合法 | **用户** | 参数/资源/状态/前置条件/编排配置/运行时错误 — 所有调用方可自主修复的问题 |
| `401` | 未认证 | **用户** | SYSTOKEN 凭证不在白名单、缺失或过期 |
| `403` | 无权限 | **用户** | 连接器目标 URL 未通过白名单校验 |
| `500` | 平台错误 | **平台** | 引擎无法归类的内部异常（极少触发，调用方无法自行修复） |

> **400 的判定核心**：只要错误原因是用户可通过修改请求、编辑连接流配置、调整编排参数来修复的，一律归 `400`。包括但不限于：流不存在/未运行、连接器配置缺失/版本失效、脚本语法/运行时异常、下游 HTTP 调用失败/超时 — 这些都是用户的连接流"没写好"或"下游挂了"，不是平台问题。

> **500 的判定核心**：仅当平台自身代码逻辑错误（如 NPE、ConcurrentModificationException）或基础设施故障（如 DAG 调度器内部崩溃）且不属于前述用户侧范畴时才归 500。调用方看到 500 意味着"平台出了 bug，联系平台方"。

#### 映射速查

| X-Code | HTTP Status | 场景 | Body |
|:---:|:---:|------|------|
| `200` | `200` | 执行成功（X-Status: 0） | 出口 body |
| `400` | `400` | 请求参数/inputContract 校验失败 | 空 |
| `404` | `400` | 连接流不存在或已被删除 | 空 |
| `409` | `400` | 连接流未运行（lifecycleStatus ≠ 2） | 空 |
| `422` | `400` | 版本/连接器已失效等前置条件不满足 | 空 |
| `60xxx`~`66xxx` | `400` | 编排配置错误 / 连接器/脚本/并行/出口运行时错误 / 超时 | 出口 body（按 errorHandler 策略产出） |
| `401` | `401` | SYSTOKEN 认证失败 | 空 |
| `403` | `403` | URL 白名单拒绝 | 空 |
| `500` | `500` | 引擎内部未知异常 | 空 |

### 4.5 X-Status 含义

| X-Status | 含义 |
|:---:|------|
| `0` | 执行成功（所有节点 `status=success`） |
| `1` | 执行失败（至少一个节点失败） |
| `2` | 执行超时（节点超时或整体超时） |

> `X-Status` 仅在连接流已实际执行（DAG 编排进入调度阶段）时出现。前置校验失败时不包含此头。执行成功时 X-Status=0 且 HTTP 200；执行失败时 X-Status≠0 且 HTTP 400。

---

## 5. 调用示例

### 5.1 成功调用（完整示例）

假设触发器定义了 `key`（query）、`echoToHeader`（body）入参：

```bash
curl -s -D - -X POST \
  "http://localhost:18180/api/v1/flows/340518008730419200/invoke?curPage=1&pageSize=1&keyword=c" \
  -H "X-Sys-Token: tester" \
  -H "Cookie: user_id=admin001" \
  -H "X-XSRF-TOKEN: user_id=admin001" \
  -H "Content-Type: application/json" \
  -d '{"echoToHeader": "hello"}'
```

**响应：**
```
HTTP/1.1 200 OK
X-Flow-Id: 340518008730419200
X-Execution-Id: 3480bf89739a42119c6c9c329ffe2142
X-Status: 0
X-Duration-Ms: 194
X-Code: 200
X-Message-Zh: Flow not running
X-Message-En: Flow not running
X-Cache-Status: 0
Echo-To-Header: hello                              ← 用户自定义响应头（出口 output.header）
Content-Type: application/json

{"code":"200","messageEn":"Success",...}            ← 出口 output.body（透传）
```

### 5.2 连接流未运行（400）

```bash
curl -s -D - -X POST \
  "http://localhost:18180/api/v1/flows/340534104363630592/invoke?curPage=1" \
  -H "X-Sys-Token: tester" \
  -H "Content-Type: application/json" \
  -d '{"echoToHeader": "hello"}'
```

**响应：**
```
HTTP/1.1 400 Bad Request
X-Flow-Id: 340534104363630592
X-Code: 409
X-Message-Zh: Flow not running
X-Message-En: Flow not running
content-length: 0
```

### 5.3 SYSTOKEN 不在白名单（401）

```bash
curl -s -D - -X POST \
  "http://localhost:18180/api/v1/flows/340518008730419200/invoke" \
  -H "X-Sys-Token: invalid_token" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**响应：**
```
HTTP/1.1 401 Unauthorized
X-Flow-Id: 340518008730419200
X-Code: 401
X-Message-Zh: Authentication failed
X-Message-En: Authentication failed
content-length: 0
```

### 5.4 执行层错误（连接器调用失败）

假设连接器节点调用的下游 API 返回 HTTP 500：

**响应：**
```
HTTP/1.1 400 Bad Request
X-Flow-Id: 340518008730419200
X-Execution-Id: 1234567890123456789
X-Status: 1
X-Duration-Ms: 5123
X-Code: 62001
X-Message-Zh: Trigger execution failed
X-Message-En: Trigger execution failed
X-Error-Node: connector_1785224227367_hpwie2j62
X-Error-Node-Type: connector

{"code": null, "data": null}                        ← 出口节点按 errorHandler 策略产出
```

---

## 6. 与调试接口的对比

| 维度 | #55 调用 (invoke) | #54 调试 (debug) |
|------|:---:|:---:|
| **端点** | `POST /api/v1/flows/{flowId}/invoke` | `POST /api/v1/flows/{flowId}/versions/{versionId}/debug` |
| **版本来源** | `flow.deployedVersionId` | 传入 `versionId`（可指定任意版本） |
| **运行状态要求** | `lifecycleStatus = 2`（必须运行中） | 无要求 |
| **认证** | SYSTOKEN 白名单强制校验 | 跳过 |
| **入站限流** | 强制校验 | 跳过 |
| **响应缓存** | 命中/写入 | 跳过 |
| **执行记录** | 写入 | 不写入 |
| **响应格式** | 透明穿透（出口 body 裸数据 + X- 头） | `ExecutionResult` JSON（含 steps 详情） |
| **triggerType** | `1`（http） | `3`（manual） |
| **isDebug** | `false` | `true` |
| **HTTP Status（执行失败）** | 400 + X-Status: 1 | 200 + errorInfo.code |

---

## 7. 实现架构

```
外部调用方
  │ POST /api/v1/flows/{flowId}/invoke
  ▼
FlowInvokeController.invokeFlow()
  │ 采集 headers / queryParams / triggerData
  │ 包装为大小写不敏感 Map（RFC 7230 §3.2）
  ▼
FlowInvokeService.invokeFlow()
  │
  ├─1. loadFlowVersion(flowId)
  │    ├─ entityCacheManager.getFlow(flowId)  ─── DB/Redis 缓存
  │    ├─ lifecycleStatus ≠ 2 → PreCheckException("409", "连接流未启动…")
  │    └─ deployedVersionId → 加载已部署版本编排配置
  │
  ├─2. parseAndValidateTrigger(config)
  │    ├─ 查找 trigger 节点（type=trigger, triggerType=http）
  │    ├─ validateAuthConfig()  ─── SYSTOKEN 白名单校验
  │    ├─ validateRateLimitConfig() ─── 入站限流
  │    └─ validateInputContractSections() ─── header/query/body 三段契约
  │
  ├─3. 构建 ExecutionContext
  │    ├─ 结构化触发输入: NodeContext { header, query, body }
  │    └─ isDebug = false, triggerType = 1
  │
  ├─4. 缓存检查 (flowConfig.cacheTtl > 0)
  │    ├─ 命中 → 直接返回缓存结果
  │    └─ 未命中 → 继续执行
  │
  ├─5. dagScheduler.schedule(orchestrationConfig, context)
  │    └─ 串行/并行 DAG 执行各节点
  │
  ├─6. buildTransparentResponse() / buildErrorResponse()
  │    └─ 构建 TransparentFlowResponse (body + userHeaders + platformHeaders)
  │
  └─7. 返回 ResponseEntity
       └─ platformHeaders → HTTP 响应头
          userHeaders → HTTP 响应头 (过滤 X- 前缀冲突)
          body → HTTP 响应体
```

---

## 8. 注意事项

1. **`X-Message-Zh` 当前为英文**：由于 Netty/HTTP 协议头仅支持 ASCII，中文会被转换为 `?`，当前实现中 `X-Message-Zh` 与 `X-Message-En` 取值相同（英文消息）。技术背景见 [plan-flow-invoke-temp.md](./plan-flow-invoke-temp.md) §8.3。

2. **连接器版本快照**：`#55` 执行时使用 `deployedVersionId` 指向的 FlowVersion 中的 `orchestrationConfig`，其中包含 `connectorVersionConfig` 快照。连接器版本后续发布不会自动更新已在运行的流。

3. **HTTP Header 大小写**：触发器 `input.header` 校验时 header 名大小写不敏感（`TreeMap(CASE_INSENSITIVE_ORDER)`），符合 RFC 7230。

4. **`errorHandler` 策略**：节点失败/超时后的行为由编排配置中该节点的 `errorHandler` 决定（`ignore`=跳过继续 / `retry`=重试 / `terminate`=终止执行），影响最终出口节点的产出。

---

## 修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-07-28 | 初始版本：接口概述 + 请求/响应规范 + 完整错误码清单（前置校验 + 执行层 53 个错误码） | SDDU Fast Agent |
