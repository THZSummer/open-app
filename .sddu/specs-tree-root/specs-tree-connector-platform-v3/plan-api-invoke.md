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
| 1 | 连接流存在 | 前置 | HTTP 400（X-Code: 404） |
| 2 | `flow.lifecycleStatus = 2`（运行中） | 前置 | HTTP 400（X-Code: 409） |
| 3 | 已部署版本可用（`deployedVersionId` 非空，版本未失效） | 前置 | HTTP 400（X-Code: 422） |
| 4 | `X-Sys-Token` 在触发器 `authConfig.sysAccountWhitelist` 白名单内 | 前置 | HTTP 401 |
| 5 | 未超过入站限流阈值（`flowConfig.rateLimitConfig.maxQps`） | 前置 | HTTP 429 |
| 6 | 触发器 `inputContract` 校验通过（header/query/body 三段契约） | 前置 | HTTP 400 |

> 注：`不满足时` 列中 `X-Code` 为当前实现的旧码值，HTTP Status 与 §4.2 表格对齐（前置校验错误统一映射到 400/401/403，详见 §4.1.1）。

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
X-Duration-Ms: 194
X-Code: 200                        ← 当前实现旧码（建议 20000，见 §4.1.2）
X-Message-Zh: Success               ← 始终返回（当前实现返回英文；设计目标为中文，见 §9.6）
X-Message-En: Success               ← 始终返回
X-Cache-Status: 0
Echo-To-Header: hello               ← 用户自定义响应头 (出口节点 output.header)

{"code":"200","page":{...}}          ← 响应体 = 出口节点 output.body (裸数据)
```

> **码制口径说明**：本节示例与 §5 一致，采用**当前实现**口径——`X-Code` 为旧码值（`200`），`X-Message-Zh` 返回英文消息（§9.6 降级策略）。设计目标为五位码（`20000`）与中文消息（§4.2 表格），切换方式见 §9.6。

### 3.2 平台元数据响应头

| Header | 类型 | 说明 | 出现条件 |
|--------|------|------|---------|
| `X-Flow-Id` | string | 连接流 ID（雪花 ID） | **始终返回** |
| `X-Execution-Id` | string | 执行记录 ID | 连接流已执行（含失败） |
| `X-Duration-Ms` | int | 执行耗时（毫秒） | 连接流已执行 |
| `X-Cache-Status` | int | `0`=未命中 / `1`=全流命中 | 缓存生效时 |
| `X-Code` | string | 平台结果码，见 §4 错误码 | **始终返回** |
| `X-Message-Zh` | string | 提示信息 | **始终返回** |
| `X-Message-En` | string | 英文提示信息 | **始终返回** |
| `X-Error-Node` | string | 失败节点 ID | 执行失败时 |
| `X-Error-Node-Type` | string | 失败节点类型 | 执行失败时 |

> ⚠️ **`X-Message-Zh` 实现说明**：HTTP 响应头仅支持 ASCII，当前 `X-Message-Zh` 实际返回 `X-Message-En`（英文消息）。§4.2 中的中文值为设计目标，待 HTTP 头编码方案确定后切换。详见 §9.6。

> 前置校验失败时，连接流未实际执行，`X-Execution-Id`、`X-Duration-Ms` 不出现。`X-Flow-Id`、`X-Code`、`X-Message-Zh`、`X-Message-En` 始终返回。
>
> 执行层错误时（连接流已执行但节点失败/超时），`X-Execution-Id`、`X-Duration-Ms`、`X-Error-Node`、`X-Error-Node-Type` 均会出现。

### 3.3 用户自定义响应头

由出口节点的 `output.header` (httpOutputDef.header) 定义，值来源于值表达式体系解析结果。不会与 `X-` 前缀平台头冲突。

### 3.4 响应体

由出口节点的 `output.body` (httpOutputDef.body) 定义，**不经过任何平台信封包装**。前置校验失败时 Body 为空（`content-length: 0`）。

---

## 4. 错误码清单

### 4.1 错误码设计规范

#### 4.1.1 设计原则

**X-Code 按首位数字区分错误大类**，完全脱离 HTTP Status 语义。调用方可仅看首位判断方向，后续位定位原因。

| 首位 | 大类 | 含义 | 流是否已执行 | HTTP Status |
|:---:|------|------|:---:|:---:|
| `2` | 成功 | 执行成功 | ✅ | 200 |
| `4` | 校验错误 | **前置拦截**：请求/认证/鉴权/资源/状态不合法，连接流**未执行** | ❌ | 400 / 401 / 403 |
| `6` | 执行错误 | **运行失败**：流已执行，但节点失败或超时 | ✅ | 400 |
| `5` | 系统错误 | 平台内部未知异常（极少触发） | — | 500 |

> **核心区分：4xx vs 6xx** — 看连接流是否被调度执行。`4xxxxx` 在执行前拦截（无 X-Execution-Id），`6xxxxx` 在执行中/后产出（有 X-Execution-Id）。

**HTTP Status 仅区分责任方**，具体原因由 X-Code 承载。

| HTTP Status | 含义 | 责任方 | 判定口径 |
|:---:|------|:---:|------|
| `200` | 执行成功 | — | DAG 编排完整执行完毕，节点无失败 |
| `400` | 请求不合法 | **用户** | 参数/资源/状态/前置条件/编排配置/运行时错误 — 所有调用方可自主修复的问题 |
| `401` | 未认证 | **用户** | SYSTOKEN 凭证不在白名单、缺失或过期 |
| `403` | 无权限 | **用户** | 连接器目标 URL 未通过白名单校验 |
| `500` | 平台错误 | **平台** | 引擎无法归类的内部异常（极少触发，调用方无法自行修复） |

> **400 vs 500 判定**：只要错误原因是用户可通过修改请求、编辑连接流配置、调整编排参数来修复的，一律归 `400`（包括：流不存在/未运行、连接器配置缺失/版本失效、脚本语法/运行时异常、下游调用失败/超时）。仅当平台自身 bug（NPE、DAG 内部崩溃）且不属于用户侧范畴时才归 `500`。调用方看到 500 意味着"联系平台方"。

#### 4.1.2 码段规划

X-Code 统一 **5 位数字**，格式为 `{大类1位}{子类2位}{序号2位}`。

```
位置:  [1]  [2][3]  [4][5]
含义:  大类  子类     序号
示例:  4    10      01    → 41001 (校验层 → 请求不合法 → 第1个场景)
      6    20      01    → 62001 (执行层 → 连接器运行时 → HTTP调用失败)
      2    00      00    → 20000 (成功)
      5    00      00    → 50000 (系统错误)
```

| 大类 (位1) | 含义 | 子类 (位2-3) | 流已执行? | HTTP Status |
|:---:|------|------|:---:|:---:|
| `2` | 成功 | `00` (固定) | ✅ | 200 |
| `4` | 校验错误 | `10`~`30` | ❌ | 400 / 401 / 403 |
| `6` | 执行错误 | `00`~`66` | ✅ | 400 |
| `5` | 系统错误 | `00` (固定) | — | 500 |

> ⚠️ 当前实现：`2`/`5` 大类尚未按 5 位格式（现用 `200`/`500`），`4` 大类的码值仍为旧格式（`400`/`404`/`409` 等混用）。下表为建议方案。

---

**校验层 `4xxxxx`（建议码段）**

```
4  10  01                    ← 请求不合法 → 参数缺失
4  11  01                    ← 资源不存在 → 连接流不存在
4  12  01                    ← 状态冲突   → 流未运行
4  13  01                    ← 前置条件   → 版本已失效
4  20  01                    ← 认证失败   → SYSTOKEN 不在白名单
4  30  01                    ← 鉴权拒绝   → URL 白名单拒绝
```

| 建议 X-Code | 旧码 | 子类 | 场景 |
|:---:|:---:|------|------|
| `41001` | 400 | 请求不合法 (41xxx) | 请求参数/inputContract 校验失败 |
| `41002` | 400 | | 触发方式缺失或未知 |
| `41003` | 400 | | HTTP 触发器缺少 input 契约 |
| `41101` | 404 | 资源不存在 (411xx) | 连接流不存在或已被删除 |
| `41102` | 404 | | 连接器/连接器版本不存在 |
| `41201` | 409 | 状态冲突 (412xx) | 连接流未运行（lifecycleStatus ≠ 2） |
| `41301` | 422 | 前置条件 (413xx) | 版本/连接器已失效 |
| `41302` | 422 | | 已部署版本不可用 |
| `42001` | 401 | 认证失败 (42xxx) | SYSTOKEN 不在白名单 |
| `42002` | 401 | | SYSTOKEN 缺失或过期 |
| `43001` | 403 | 鉴权拒绝 (43xxx) | URL 白名单拒绝 |

---

**执行层 `6xxxxx`（现有，不动）**

```
6  01  01      ← 编排配置解析失败 (61001)
6  20  01      ← 连接器HTTP调用失败 (62001)
6  30  01      ← 脚本运行时异常 (63001)
6  40  00      ← 节点超时 (64000)
6  50  01      ← 并行分支失败 (65001)
6  60  01      ← 出口序列化失败 (66001)
```

| 码段 | 子类 | 说明 |
|:---:|------|------|
| `60xxx` | DAG 通用 (600xx) | 执行失败/节点失败/超时兜底码 |
| `61xxx` | 编排/配置 (610xx) | JSON解析失败 61001 / 缺触发节点 61002 / 缺出口节点 61003 / 边缺失 61004 |
| `61xxx` | 节点配置 (6101x~6105x) | 触发器配置 6101x / 连接器配置 6102x / 脚本配置 6103x / 并行配置 6104x / 出口配置 6105x |
| `62xxx` | 连接器运行时 (620xx) | HTTP失败 62001 / 连接超时 62002 / 读取超时 62003 / DNS 62004 / SSL 62005 / 序列化 62006 / 响应过大 62007 |
| `63xxx` | 脚本运行时 (630xx) | 异常 63001 / 超时 63002 / 语句上限 63003 / 返回值类型 63004 / 字段不存在 63005 |
| `64xxx` | 超时 (640xx) | 节点级超时 64000 |
| `65xxx` | 并行节点 (650xx) | 分支失败 65001 / 分支超时 65002 / 全部失败 65003 |
| `66xxx` | 出口节点 (660xx) | 序列化失败 66001 / 响应头设置失败 66002 |

---

**系统层**

| 建议 X-Code | 旧码 | 含义 |
|:---:|:---:|------|
| `20000` | 200 | 执行成功 |
| `50000` | 500 | 平台内部未知异常 |

### 4.2 错误码全表

> 当前实现：旧码列。建议 X-Code 列按 §4.1.2 五位码段方案。执行层（6xxxxx）额外携带 `X-Execution-Id` + `X-Error-Node` 等诊断头。

#### 成功 / 系统错误

| X-Code | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|------|------|------|
| `20000` | `200` | `成功` | `Success` | 执行成功 |
| `50000` | `500` | `平台内部异常，请联系管理员` | `Trigger execution failed` | 平台内部未知异常 |

#### 校验错误 — 请求不合法 (41xxx)

| 建议 X-Code | 旧码 | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|:---:|------|------|------|
| `41001` | 400 | 400 | `请求参数缺失或格式不合法` | `Bad request` | 请求参数缺失或格式不合法 |
| `41002` | 400 | 400 | `触发器输入参数校验失败` | `Bad request` | 触发器 inputContract 校验失败（header/query/body 三段契约不匹配） |
| `41003` | 400 | 400 | `触发方式缺失或未知` | `Bad request` | 触发方式缺失或未知（仅支持 http / manual） |
| `41004` | 400 | 400 | `HTTP 触发器缺少输入契约配置` | `Bad request` | HTTP 触发器缺少 input 契约 |

#### 校验错误 — 资源不存在 (411xx)

| 建议 X-Code | 旧码 | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|:---:|------|------|------|
| `41101` | 404 | 400 | `连接流不存在或已被删除` | `Flow not found` | 连接流不存在或已被删除 |
| `41102` | 404 | 400 | `连接器不存在或已被删除` | `Flow not found` | 连接器不存在或已被删除 |
| `41103` | 404 | 400 | `连接器版本不存在` | `Flow not found` | 连接器版本不存在 |
| `41104` | 404 | 400 | `版本不存在，请检查版本 ID` | `Version not found` | 版本不存在（调试传入 versionId 无效） |

#### 校验错误 — 状态冲突 (412xx)

| 建议 X-Code | 旧码 | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|:---:|------|------|------|
| `41201` | 409 | 400 | `连接流未启动，请先启动后再调用` | `Flow not running` | 连接流未启动（lifecycleStatus ≠ 2） |

#### 校验错误 — 前置条件 (413xx)

| 建议 X-Code | 旧码 | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|:---:|------|------|------|
| `41301` | 422 | 400 | `已部署版本不可用，请重新部署` | `Trigger execution failed` | 已部署版本不可用（版本已被失效） |
| `41302` | 422 | 400 | `连接器版本已失效` | `Trigger execution failed` | 连接器版本已失效 |
| `41303` | 422 | 400 | `连接器已失效` | `Trigger execution failed` | 连接器已失效 |

#### 校验错误 — 编排/调试 (414xx)

| 建议 X-Code | 旧码 | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|:---:|------|------|------|
| `41401` | 422 | 400 | `编排配置为空，请先完成编排后再调试` | `Orchestration config is empty` | 编排配置为空（调试时校验） |
| `41402` | 422 | 400 | `版本状态不支持调试` | `Version status not debuggable` | 版本状态不支持调试（仅草稿/已发布可调试） |

#### 校验错误 — 认证失败 (42xxx)

| 建议 X-Code | 旧码 | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|:---:|------|------|------|
| `42001` | 401 | 401 | `调用凭证不在白名单中` | `Authentication failed` | X-Sys-Token 不在触发器 sysAccountWhitelist 白名单内 |
| `42002` | 401 | 401 | `调用凭证缺失或已过期` | `Authentication failed` | X-Sys-Token 缺失或过期 |

#### 校验错误 — 鉴权拒绝 (43xxx)

| 建议 X-Code | 旧码 | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|:---:|------|------|------|
| `43001` | 403 | 403 | `目标 URL 未通过白名单校验` | `URL whitelist denied` | 连接器调用的目标 URL 未通过白名单校验 |

---

#### 执行错误 — DAG 编排通用 (60xxx)

| X-Code | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|------|------|------|
| `60000` | 400 | `编排执行失败` | `Trigger execution failed` | DAG 执行整体失败 |
| `60001` | 400 | `节点执行失败` | `Trigger execution failed` | 节点执行失败（通用兜底） |
| `60002` | 400 | `节点超时或执行错误` | `Trigger execution failed` | 节点超时或错误 |

#### 执行错误 — 编排/配置 (61xxx)

| X-Code | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|------|------|------|
| `61001` | 400 | `编排配置 JSON 解析失败` | `Trigger execution failed` | 编排配置 JSON 解析失败 |
| `61002` | 400 | `编排配置中缺少触发器节点` | `Trigger execution failed` | 编排中缺少触发器节点 |
| `61003` | 400 | `编排配置中缺少出口节点` | `Trigger execution failed` | 编排中缺少出口节点 |
| `61004` | 400 | `节点间连接关系缺失` | `Trigger execution failed` | 节点间边关系缺失 |
| `61010` | 400 | `触发器节点未配置触发方式` | `Trigger execution failed` | 触发器：触发方式未配置（data.triggerType 缺失） |
| `61011` | 400 | `触发器 SYSTOKEN 凭证不存在或已过期` | `Trigger execution failed` | 触发器：SYSTOKEN 凭证不存在或已过期 |
| `61012` | 400 | `触发器调用凭证不在白名单中` | `Trigger execution failed` | 触发器：调用凭证不在白名单中 |
| `61020` | 400 | `连接器节点未选择连接器` | `Trigger execution failed` | 连接器：未选择连接器（connectorId 缺失） |
| `61021` | 400 | `连接器节点未选择版本` | `Trigger execution failed` | 连接器：未选择连接器版本（connectorVersionId 缺失） |
| `61022` | 400 | `连接器节点超时值超过上限` | `Trigger execution failed` | 连接器：节点超时值超过应用上限 |
| `61023` | 400 | `连接器入参映射引用了不存在的字段` | `Trigger execution failed` | 连接器：入参映射引用了不存在的字段 |
| `61024` | 400 | `连接器缺少认证配置` | `Trigger execution failed` | 连接器：缺少认证配置 |
| `61025` | 400 | `连接器未选择认证类型` | `Trigger execution failed` | 连接器：认证类型未选择 |
| `61030` | 400 | `脚本节点源码为空` | `Trigger execution failed` | 脚本：源码为空 |
| `61031` | 400 | `脚本节点源码超过字符上限` | `Trigger execution failed` | 脚本：源码超过字符上限 |
| `61032` | 400 | `脚本节点缺少 main(ctx) 函数` | `Trigger execution failed` | 脚本：缺少 main(ctx) 函数定义 |
| `61033` | 400 | `脚本节点存在语法错误` | `Trigger execution failed` | 脚本：语法错误 |
| `61040` | 400 | `并行节点分支数不足` | `Trigger execution failed` | 并行：分支数不足（最少 2 个） |
| `61041` | 400 | `并行节点分支数超过上限` | `Trigger execution failed` | 并行：分支数超过上限（最多 8 个） |
| `61042` | 400 | `并行节点分支内无节点` | `Trigger execution failed` | 并行：分支内无节点 |
| `61050` | 400 | `出口节点输出映射引用了不存在的字段` | `Trigger execution failed` | 出口：输出映射引用了不存在的字段 |
| `61051` | 400 | `出口节点输出映射格式错误` | `Trigger execution failed` | 出口：输出映射格式错误 |

#### 执行错误 — 连接器运行时 (62xxx)

| X-Code | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|------|------|------|
| `62001` | 400 | `连接器调用下游失败` | `Trigger execution failed` | HTTP 调用下游失败（含下游 statusCode） |
| `62002` | 400 | `连接器连接目标超时` | `Trigger execution failed` | TCP 连接超时（目标不可达） |
| `62003` | 400 | `连接器读取响应超时` | `Trigger execution failed` | 读取超时（下游未在规定时间内响应） |
| `62004` | 400 | `连接器目标地址解析失败` | `Trigger execution failed` | DNS 解析失败（host 不存在） |
| `62005` | 400 | `连接器 SSL 证书校验失败` | `Trigger execution failed` | SSL 证书校验失败 |
| `62006` | 400 | `连接器请求参数序列化失败` | `Trigger execution failed` | 请求参数序列化失败 |
| `62007` | 400 | `连接器下游响应体超过限制` | `Trigger execution failed` | 下游响应体超过大小限制 |

#### 执行错误 — 脚本运行时 (63xxx)

| X-Code | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|------|------|------|
| `63001` | 400 | `脚本节点运行时异常` | `Trigger execution failed` | 脚本执行时抛出异常 |
| `63002` | 400 | `脚本节点执行超时` | `Trigger execution failed` | 脚本执行超时（超过节点 timeoutMs） |
| `63003` | 400 | `脚本节点执行超过语句上限` | `Trigger execution failed` | 脚本执行超过语句上限 |
| `63004` | 400 | `脚本节点返回值不是对象类型` | `Trigger execution failed` | 脚本返回值不是 Object 类型 |
| `63005` | 400 | `脚本节点访问了不存在的上游字段` | `Trigger execution failed` | 脚本访问了不存在的上游字段 |

#### 执行错误 — 超时 / 并行 / 出口 (64xxx~66xxx)

| X-Code | HTTP Status | X-Message-Zh | X-Message-En | 场景 |
|:---:|:---:|------|------|------|
| `64000` | 400 | `节点执行超时` | `Trigger execution failed` | 单节点执行超时 |
| `65001` | 400 | `并行分支执行失败` | `Trigger execution failed` | 并行分支执行失败 |
| `65002` | 400 | `并行分支执行超时` | `Trigger execution failed` | 并行分支执行超时 |
| `65003` | 400 | `所有并行分支均执行失败` | `Trigger execution failed` | 所有并行分支均失败 |
| `66001` | 400 | `出口节点响应体序列化失败` | `Trigger execution failed` | 出口响应体序列化失败 |
| `66002` | 400 | `出口节点响应头设置失败` | `Trigger execution failed` | 出口响应头设置失败 |

### 4.3 X-Status 含义

> ⚠️ **已废弃**。X-Code 已可区分成功/失败：`200`=成功，其余=失败。X-Status 冗余，后续版本移除。

---

## 5. 调用示例

> **码制口径说明**：以下示例中的 `X-Code` 采用**当前实现**的旧码值（`200`/`400`/`409`/`401`/`62001` 等），与 §4.2 表格"旧码"列一致。§4.1.2 建议的五位码段（`20000`/`41201`/`42001` 等）尚未实施，对应关系见 §4.2"建议 X-Code"列。`X-Message-Zh` 按 §9.6 降级策略返回英文消息（与 `X-Message-En` 相同）。

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
X-Duration-Ms: 194
X-Code: 200
X-Message-Zh: Success
X-Message-En: Success
X-Cache-Status: 0
Echo-To-Header: hello                              ← 用户自定义响应头（出口 output.header）
Content-Type: application/json

{"code":"200","messageEn":"Success",...}            ← 出口 output.body（透传）
```

> 按 §9.6 降级策略，当前实现 `X-Message-Zh` 与 `X-Message-En` 均返回英文 `Success`；设计目标为 `成功`（见 §4.2 错误码表）。`X-Status` 已废弃（§4.3），示例中不再返回。

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
| **HTTP Status（执行失败）** | 400 + X-Error-Node/Type 诊断头 | 200 + errorInfo.code |

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

## 9. 重构计划

### 9.1 ErrorCode 枚举化

**现状问题**

`ErrorCode.java` 是纯常量类，码值和消息完全脱离。同一个 `"404"` 对应 `PRECHECK_FLOW_NOT_FOUND`、`PRECHECK_CONNECTOR_NOT_FOUND` 等多个常量，且消息散落在各 Service/Executor 中硬编码，无法内聚。

**目标**

改为 `enum ErrorCode`，每个枚举值绑定四个维度：

```
enum ErrorCode {
    FLOW_NOT_RUNNING("41201", "连接流未启动，请先启动后再调用", "Flow not running", 400),
    ...
    
    String code();
    String messageZh();
    String messageEn();
    int   httpStatus();
    Map<String,Object> toErrorInfo();
}
```

**影响面**

| 维度 | 旧 | 新 |
|------|------|------|
| 引用方式 | `ErrorCode.PRECHECK_FLOW_NOT_RUNNING`（String） | `ErrorCode.FLOW_NOT_RUNNING.code()` |
| PreCheckException | `new PreCheckException(String code, String zh)` | `new PreCheckException(ErrorCode)` |
| errorInfo 构建 | `ErrorCode.errorInfo(code, zh, en)` 手动传参 | `code.toErrorInfo()` |
| buildErrorResponse switch | `case "404"` 硬编码字符串 | `case ErrorCode.FLOW_NOT_FOUND` 枚举匹配 |

**改动文件**：~15（ErrorCode.java + 2 个 Service + 6 个 Executor + 4 个 Java 测试 + ~3 个 Python 测试）

### 9.2 五位码值对齐（200/500 → 20000/50000）

当前代码中 `200`/`500` 仅 3 位，改为 `20000`/`50000` 与 5 位码段体系统一：

| 常量 | 旧值 | 新值 |
|------|:---:|:---:|
| (新增 SUCCESS) | — | `20000` |
| PRECHECK_INTERNAL_ERROR | `500` | `50000` |

### 9.3 X-Status 移除

X-Code 已可区分成功/失败（`20000`=成功，其余=失败），`X-Status` 冗余。

**代码侧**：
- `TransparentFlowResponse.success()` 移除 `X-Status` 写入
- `FlowInvokeController` 响应头过滤移除 `X-Status` 相关逻辑

**测试侧**：
- 移除所有 `X-Status` 断言

### 9.4 实施顺序

| 步骤 | 内容 | 预估 |
|:---:|------|:---:|
| 1 | 重写 `ErrorCode.java` 为 enum，含全部 54 个枚举值 | 基准 |
| 2 | `PreCheckException` 改造，接受 `ErrorCode` 参数 | 依赖 1 |
| 3 | `FlowInvokeService` + `FlowRuntimeEngine` + `InboundRateLimiter` 硬编码字符串替换为枚举 | 依赖 1 |
| 4 | 6 个 Executor 层 `errorInfo.put("code", ...)` 替换为 `ErrorCode.xxx.toErrorInfo()` | 依赖 1 |
| 5 | `buildErrorResponse` switch 硬编码 → 枚举匹配 | 依赖 1 |
| 6 | X-Status 移除 | 独立 |
| 7 | 200/500 → 20000/50000 码值迁移 | 依赖 1 |
| 8 | Java + Python 测试对齐 | 依赖 1~7 |

### 9.5 验证标准

每步完成后的验证流程：

```
1. 编译通过   mvn compile -q -pl connector-api
2. 启动服务   bash connector-api/scripts/restart.sh
3. 单元测试   mvn test -pl connector-api
              测试路径: connector-api/src/test/java
4. 集成测试   pytest connector-api/src/test/python -x
5. 手动冒烟   curl POST /api/v1/flows/{flowId}/invoke 验证核心场景
```

### 9.6 X-Message-Zh 降级策略

**当前实现**：`X-Message-Zh` = `X-Message-En`（英文消息）。

**原因**：HTTP 响应头仅支持 ASCII 字符，Netty 的 `AsciiString` 会将中文转为 `?`。

**设计目标**（§4.2 标注的中文消息）待以下任一条件满足后激活：

| 方案 | 说明 |
|------|------|
| A | Netty pipeline 层拦截，UTF-8 编码写入响应头（见 [plan-flow-invoke-temp.md](./plan-flow-invoke-temp.md)） |
| B | 错误信息放入 Response Body，X- 头仅保留机器可读的 X-Code / X-Flow-Id |

**代码位置**：`TransparentFlowResponse.preExecutionError()` — 当前 `X-Message-Zh` 取 `messageEn` 参数；`FlowInvokeService.populateErrorHeaders()` — 当前从 `errorInfo.get("messageZh")` 取值。

**切换方式**：方案确定后，将上述两处改为取 `ErrorCode.messageZh()` 即可，§4.2 全表中的中文消息直接生效。

---

## 修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-07-28 | 初始版本：接口概述 + 请求/响应规范 + 完整错误码清单（前置校验 + 执行层 54 个错误码） | SDDU Fast Agent |
| v1.1 | 2026-07-28 | §4 重构：码段统一 5 位、表结构合并 + §4.1.1 融合 HTTP Status 映射 + X-Status 废弃 + §9 重构计划 | SDDU Fast Agent |
