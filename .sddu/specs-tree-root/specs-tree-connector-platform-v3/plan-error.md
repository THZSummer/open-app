# 错误处理设计：connector-api 调试与调用接口

**Feature ID**: CONN-PLAT-002
**版本**: v1.0-draft
**创建日期**: 2026-07-01
**作者**: Summer

---

## 1. 概述

connector-api 有两个面向外部/内部的执行接口：

| 接口 | 端点 | 用途 | 调用方 |
|------|------|------|--------|
| 调试接口 | `POST /api/v1/flows/{flowId}/versions/{versionId}/debug` | 草稿/已发布版本调试执行 | wecodesite 前端 |
| 调用接口 | `POST /api/v1/flows/{flowId}/invoke` | 运行中连接流 HTTP 调用 | 外部系统 |

两个接口共用同一套执行引擎（DagScheduler），但前置校验和错误返回方式完全不同，导致：
- 调试接口几乎所有错误都是 `code: 6002`，用户无法区分具体原因
- 调用接口靠字符串匹配分类错误，"流未运行"被归为 500
- 两个接口错误码体系不统一

本文档定义统一的错误码体系和错误消息规范。

---

## 2. 接口架构设计

### 2.1 共享与隔离

调试和调用共享**节点执行层**（NodeExecutor），隔离**流程控制层**（FlowService）。

```
                          ┌──────────────────────────┐
                          │    NodeExecutor (共享)     │
                          │  ConnectorNodeExecutor    │
                          │  ScriptNodeExecutor       │
                          │  ParallelBranchExecutor   │
                          │  TriggerNodeExecutor      │
                          │  ExitNodeExecutor         │
                          └──────────┬───────────────┘
                                     │ 统一错误码 + 消息模板
                           ┌──────────┴───────────────┐
                           │                          │
               ┌───────────▼──────────────────────────────────┐
               │              DagScheduler (共享)              │
               │        DAG 拓扑 串行 + 并行分支                │
               └─────────────────────┬───────────────────────┘
                                     │
               ┌─────────────────────┴───────────────────────┐
               │                                             │
               ┌──────────▼──────────┐  ┌───────────────────▼┐
               │  FlowVersionDebug   │  │  FlowInvokeService │
               │     Service         │  │                     │
               │                     │  │                     │
               │  前置校验:           │  │  前置校验:           │
               │  • 版本存在性        │  │  • 流存在性+绑定版本   │
               │  • 版本状态(非已失效) │  │  • 运行状态(RUNNING) │
               │  • 编排非空          │  │  • SYSTOKEN 认证    │
               │  • 版本状态(草稿/已发布)│ │  • 入站限流          │
               │                     │  │                     │
               │  执行策略:           │  │  执行策略:           │
               │  • 不写执行记录       │  │  • 写执行记录+步骤日志 │
               │  • 不走缓存           │  │  • 命中/写入响应缓存  │
               │  • 不走限流           │  │  • FIFO 清理旧记录   │
               │  • 跳过认证           │  │  • triggerType = 1  │
               │  • triggerType = 3   │  │  • isDebug = false  │
               │  • isDebug = true    │  │                     │
               │                     │  │                     │
               │  返回格式:           │  │  返回格式:           │
               │  ExecutionResult JSON│  │  透传HTTP(body+header)│
               └─────────────────────┘  └─────────────────────┘
```

### 2.2 业务差异清单

调试和调用有 9 项合理业务差异、5 项应统一的不合理差异。

#### 合理差异（两套服务路径，逻辑不同）

| # | 维度 | 调试 | 调用 | 理由 |
|---|------|:---:|:---:|------|
| 1 | 版本绑定 | 传入 versionId | deployed_version_id | 调试可指定任意版本 |
| 2 | 运行状态 | 不检查 lifecycle_status | 必须 RUNNING(2) | 调试不需要流运行 |
| 3 | 执行记录 | 不写 | 写 execution_record + step_log | 调试不产生运维数据 |
| 4 | 认证校验 | 跳过 | SYSTOKEN 白名单 | 调试不需要外部凭证 |
| 5 | 入站限流 | 跳过 | QPS/并发限制 | 调试不限制频率 |
| 6 | 响应缓存 | 跳过 | 命中/写入 cache | 调试每次真实执行 |
| 7 | 输入格式 | 扁平 Map | {header,query,body} 结构化 | 调试简化输入 |
| 8 | 响应格式 | ExecutionResult JSON（含 steps） | 透传 HTTP body + X- 响应头 | 调试要看到步骤详情 |
| 9 | isDebug | true | false | 节点层通过此标记判断行为 |

> 💡 执行引擎统一使用 DagScheduler，同时支持串行和并行拓扑。调试和调用共享同一引擎。

#### 不合理差异（应统一）

| # | 维度 | 调试 | 调用 | 应改为 |
|---|------|:---:|:---:|------|
| 11 | 错误码 | 6002/6004 | HTTP 码 (404/403/500) | 统一使用本文档第 5 章错误码 |
| 12 | 错误消息 | 底层异常透传 | 字符串匹配分类 | 统一从 errorInfo 读取 |
| 13 | 错误包装 | `onErrorResume` 重新包为 6002 | `classifyError` 字符串匹配分类 | 节点层产出细分错误码，两边透传不二次包装 |
| 14 | isDebug 字段 | 无统一字段，硬编码 | 硬编码 | ExecutionContext.isDebug |
| 15 | 编排解析 | 只读 nodes | 完整解析 flowConfig | 统一解析逻辑 |

### 2.3 isDebug 标记

在 `ExecutionContext` 中增加 `isDebug` 字段，替代当前分散在各处的硬编码判断。

```java
public class ExecutionContext {
    private boolean debug;  // true=调试模式, false=调用模式
    // ...
}
```

| 使用位置 | 判断逻辑 | 影响 |
|---------|---------|------|
| DagScheduler | `if (ctx.isDebug()) skip = true` | 跳过执行记录写入 |
| ConnectorNodeExecutor | `if (ctx.isDebug()) skip auth validation` | 跳过认证校验 |
| ScriptNodeExecutor | `if (ctx.isDebug()) skip statementLimit` | 放宽脚本资源限制 |
| FlowInvokeService | `if (!ctx.isDebug()) write cache` | 仅调用模式写缓存 |
| FlowVersionDebugService | `ctx.setDebug(true)` | 创建 context 时标记 |

### 2.4 错误码统一路径

节点执行器（共享层）产出细分错误码，调试和调用两侧都不再重新包装，只做格式转换：

```
节点执行器产出:
  NodeOutput.errorInfo { code: 63001, messageZh: "脚本节点[xxx]语法错误...", messageEn: "..." }
       │
       ├── 调试侧 → ExecutionResult.errorInfo = NodeOutput.errorInfo（原样透传）
       │
       └── 调用侧 → X-Code: 63001, X-Message-Zh: NodeOutput.messageZh
                    HTTP 状态码按 code 首段映射: 63xxx→502
```

两侧不再各自 `onErrorResume` 重新生成 errorInfo。

---

## 3. 现状分析

### 3.1 调试接口（当前）

**返回方式**：始终 HTTP 200，错误封装在 `ExecutionResult` JSON body 中。

```json
{
  "flowId": "xxx",
  "status": "failed",
  "test": true,
  "errorInfo": {
    "code": "6002",
    "messageZh": "测试执行失败: <底层异常message>",
    "messageEn": "Test execution failed: <底层异常message>",
    "cause": "<底层异常message>"
  }
}
```

| 场景 | 当前 code | 当前 messageZh | 问题 |
|------|:---:|-----------|------|
| 版本不存在 | 6002 | "测试执行失败: Flow not found: xxx" | ❌ 用户看不懂，不应该进入执行 |
| 版本已失效 | **6004** | "该版本已失效，不可调试" | ✅ 唯一已区分的 |
| 版本状态不支持调试（待审批/已驳回/已撤回） | 6002 | "测试执行失败: xxx" | ❌ 未校验，直接进入执行报通用错 |
| 编排配置为空 | 6002 | "测试执行失败: xxx" | ❌ 未校验 |
| 编排配置解析失败 | 6002 | "测试执行失败: Failed to parse JSON: ..." | ❌ 底层异常透传 |
| 连接器节点调用失败 | 6002 | "测试执行失败: Connection refused: ..." | ❌ 底层异常透传 |
| 脚本节点执行失败 | 6002 | "测试执行失败: SyntaxError: ..." | ❌ 底层异常透传 |
| 执行超时 | 6002 | "测试执行失败: timeout" | ❌ 无具体节点信息 |
| 其他运行时错误 | 6002 | "测试执行失败: <raw message>" | ❌ 通用兜底 |

**核心问题**：前置校验不足，几乎所有错误都进入执行流程后报 6002，用户看到的全是底层技术异常 message。

### 3.2 调用接口（当前）

**返回方式**：HTTP 状态码 + X- 响应头，body 为 null。

```
HTTP/1.1 500 Internal Server Error
X-Flow-Id: xxx
X-Code: 500
X-Message-Zh: 调用执行失败: Flow is not running: flowId=xxx
```

| 场景 | HTTP | X-Code | X-Message-Zh | 问题 |
|------|:---:|:---:|-------------|------|
| 流不存在 | 404 | 404 | "流不存在: Flow not found: xxx" | ⚠️ 半中半英 |
| 流未运行 | **500** | 500 | "调用执行失败: Flow is not running: xxx" | ❌ 应该是 409/422 |
| URL 白名单拒绝 | 403 | 403 | "URL 白名单拒绝: xxx" | ✅ |
| 认证失败 | 401 | 401 | "认证失败: xxx" | ✅ |
| 请求参数错误 | 400 | 400 | "请求参数错误: xxx" | ⚠️ 底层细节 |
| 运行时执行失败 | 502 | — | X-Status: 1 | ❌ 无具体原因 |
| 执行超时 | 504 | timeout | X-Status: 2 | ⚠️ 不知道哪个节点超时 |

**核心问题**：`classifyError()` 靠字符串匹配异常 message，漏匹配就归为 500。"流未运行"应该用 409，但匹配不到正确分支。

### 3.3 错误码现状对比

| 错误类型 | 调试接口 code | 调用接口 X-Code | 统一？ |
|---------|:---:|:---:|:---:|
| 版本/流不存在 | 6002 | 404 | ❌ |
| 版本已失效 | **6004** | 无（调试专属） | ❌ |
| 流未运行 | 无（调试不检查） | **500** | ❌ 码值错误 |
| 认证失败 | 6002 | 401 | ❌ |
| URL 白名单拒绝 | 6002 | 403 | ❌ |
| 参数错误 | 6002 | 400 | ❌ |
| 编排错误 | 6002 | 500/502 | ❌ |
| 连接器调用失败 | 6002 | 500/502 | ❌ |
| 脚本执行失败 | 6002 | 500/502 | ❌ |
| 执行超时 | 6002 | timeout | ❌ |

---

## 4. 场景矩阵

### 4.1 调试独有场景（调用不会有）

| 场景 | 原因 | 时机 |
|------|------|:---:|
| 版本不存在 | 调试传入 versionId 可能不在 DB | 前置 |
| 版本已失效 | 调试可指定任意版本，已失效版本不可调试 | 前置 |
| 版本状态不支持调试 | 仅草稿(1)和已发布(5)可调试，待审批(2)/已撤回(3)/已驳回(4)不可调试 | 前置 |
| 编排配置为空 | 草稿版本编排可能为空 | 前置 |

### 4.2 调用独有场景（调试不会有）

| 场景 | 原因 | 时机 |
|------|------|:---:|
| 流不存在 | 调用通过 flowId 查流 | 前置 |
| 流未运行 | 调用要求 lifecycle_status=2 | 前置 |
| 已部署版本不可用 | 已部署版本可能被失效（FR-028 校验失败时） | 前置 |
| SYSTOKEN 认证失败 | 调用需校验凭证白名单；调试不校验 | 前置 |
| 入站限流 | 调用有限流保护；调试不限流 | 前置 |

### 4.3 共用场景（两个接口都可能）

| 场景 | 时机 |
|------|:---:|
| 编排配置解析失败 | 执行中 |
| 连接器节点调用失败（HTTP 错误/超时/连接拒绝） | 执行中 |
| 脚本节点执行失败（语法错误/运行时异常/超时） | 执行中 |
| URL 白名单拒绝 | 执行中 |
| 单节点执行超时 | 执行中 |
| 并行分支执行失败 | 执行中 |
| 内部未知错误 | 任意 |

---

## 5. 统一错误码设计

### 5.1 错误码分层

| 码段 | 含义 | 适用场景 |
|:---:|------|------|
| 400 | 请求参数错误 | 前置校验：入参格式/必填/类型 |
| 401 | 认证失败 | 前置校验：SYSTOKEN 白名单/凭证校验 |
| 403 | 权限拒绝 | 前置/执行中：URL 白名单拒绝 |
| 404 | 资源不存在 | 前置校验：流/版本/连接器/连接器版本不存在 |
| 409 | 状态冲突 | 前置校验：流未运行/版本状态不支持 |
| 422 | 前置条件不满足 | 前置校验：编排为空/版本已失效/节点配置缺失 |
| 61xxx | 执行层 — 编排错误 | 执行中：JSON 解析失败/节点配置错误/引用字段不存在 |
| 62xxx | 执行层 — 连接器节点 | 执行中：下游 HTTP 错误/连接超时/认证失败 |
| 63xxx | 执行层 — 脚本节点 | 执行中：脚本语法/运行时/超时/资源限制 |
| 64xxx | 执行层 — 超时 | 执行中：单节点超时 |
| 65xxx | 执行层 — 并行节点 | 执行中：分支执行失败 |
| 66xxx | 执行层 — 出口节点 | 执行中：输出映射错误 |
| 500 | 内部未知错误 | 兜底：无法归类的系统异常 |

### 5.2 前置校验错误（调试 + 调用共用 HTTP 码）

| code | 场景 | messageZh 模板 | 调试? | 调用? |
|:---:|------|-----------|:---:|:---:|
| 400 | 请求参数错误 | "请求参数错误：{具体字段}" | ✅ | ✅ |
| 401 | 认证失败 | "调用凭证校验失败：SYSTOKEN 不在白名单中" | — | ✅ |
| 403 | URL 白名单拒绝 | "连接器[{连接器名}] URL [{url}] 未通过白名单校验" | ✅ | ✅ |
| 404 | 流不存在 | "连接流不存在" | — | ✅ |
| 404 | 版本不存在 | "版本不存在，请检查版本 ID" | ✅ | — |
| 404 | 连接器不存在 | "连接器[{连接器名}]不存在或已被删除" | ✅ | ✅ |
| 404 | 连接器版本不存在 | "连接器[{连接器名}]的版本[{版本号}]不存在" | ✅ | ✅ |
| 409 | 流未运行 | "连接流未启动，请先启动后再调用" | — | ✅ |
| 422 | 版本已失效 | "版本已失效，不可{操作}" | ✅ | ✅ |
| 422 | 版本状态不支持调试 | "版本状态为「{状态名}」，仅草稿和已发布版本可调试" | ✅ | — |
| 422 | 编排配置为空 | "编排配置为空，请先完成编排后再调试" | ✅ | — |
| 422 | 已部署版本不可用 | "已部署版本不可用，请重新部署后再调用" | — | ✅ |
| 422 | 连接器版本已失效 | "连接器[{连接器名}]版本[{版本号}]已失效，请更新编排引用" | ✅ | ✅ |
| 422 | 连接器已失效 | "连接器[{连接器名}]已失效，请更新编排引用后再运行" | ✅ | ✅ |

### 5.3 触发器节点 (trigger)

用户在页面上配置：触发方式（HTTP）、SYSTOKEN 认证

| code | 场景 | messageZh 模板 |
|:---:|------|-----------|
| 61010 | 触发方式未配置 | "触发器节点未配置触发方式" |
| 61011 | 触发凭证不存在 | "触发器节点的 SYSTOKEN 凭证不存在或已过期" |
| 61012 | 触发凭证不在白名单 | "调用凭证不在白名单中，请检查触发器节点的 SYSTOKEN 配置" |

### 5.4 连接器节点 (connector)

用户在页面上配置：连接器选择、版本选择、超时时间、入参映射、认证配置

#### 前置/配置错误

| code | 场景 | messageZh 模板 |
|:---:|------|-----------|
| 61020 | 未选择连接器 | "连接器节点[{节点名}]未选择连接器" |
| 61021 | 未选择版本 | "连接器节点[{节点名}]未选择连接器版本" |
| 61022 | 节点超时值超限 | "连接器节点[{节点名}]超时值（{value}ms）超过应用上限（{max}ms）" |
| 61023 | 入参映射字段不存在 | "连接器节点[{节点名}]入参映射引用了不存在的字段：{field}" |
| 61024 | 认证配置缺失 | "连接器[{连接器名}]缺少认证配置，请在连接器设置中配置" |
| 61025 | 认证类型未选择 | "连接器[{连接器名}]未选择认证类型" |

#### 运行时错误

| code | 场景 | messageZh 模板 |
|:---:|------|-----------|
| 62001 | HTTP 调用失败 | "连接器[{连接器名}]调用失败：HTTP {statusCode}，下游返回：{截断body}" |
| 62002 | 连接超时 | "连接器[{连接器名}]连接超时，目标地址 [{url}] 不可达" |
| 62003 | 读取超时 | "连接器[{连接器名}]读取超时（超过{timeout}ms），下游未在规定时间内响应" |
| 62004 | DNS 解析失败 | "连接器[{连接器名}]目标地址 [{host}] 解析失败，请检查 URL 配置" |
| 62005 | SSL 握手失败 | "连接器[{连接器名}]SSL 证书校验失败，目标地址可能使用了不受信任的证书" |
| 62006 | 请求体序列化失败 | "连接器[{连接器名}]请求参数序列化失败：{detail}" |
| 62007 | 响应体过大 | "连接器[{连接器名}]下游响应体超过 {max} 字节，已截断" |

### 5.5 脚本节点 (script)

用户在页面上配置：脚本源码、超时时间

#### 前置/配置错误

| code | 场景 | messageZh 模板 |
|:---:|------|-----------|
| 61030 | 脚本为空 | "脚本节点[{节点名}]源码为空，请编写脚本" |
| 61031 | 脚本超长 | "脚本节点[{节点名}]源码超过{maxLen}字符上限，当前{actualLen}字符" |
| 61032 | 缺少 main 函数 | "脚本节点[{节点名}]缺少 main(ctx) 函数定义" |
| 61033 | 脚本语法错误 | "脚本节点[{节点名}]语法错误：{detail}" |

#### 运行时错误

| code | 场景 | messageZh 模板 |
|:---:|------|-----------|
| 63001 | 脚本执行异常 | "脚本节点[{节点名}]运行时错误：{detail}，请检查 ctx 访问和变量引用" |
| 63002 | 脚本执行超时 | "脚本节点[{节点名}]执行超时（超过{timeout}ms），请优化脚本逻辑或调大超时时间" |
| 63003 | 脚本超过语句上限 | "脚本节点[{节点名}]执行超过语句上限（{limit}条），请简化脚本逻辑" |
| 63004 | 脚本返回值不是对象 | "脚本节点[{节点名}]返回值不是对象类型，请确保 return 一个对象 map" |
| 63005 | 访问不存在的上游字段 | "脚本节点[{节点名}]访问了不存在的上游字段：{field}" |

### 5.6 并行处理节点 (parallel)

用户在页面上配置：分支数（2~8）、各分支的编排节点

| code | 场景 | messageZh 模板 |
|:---:|------|-----------|
| 61040 | 分支数不足 | "并行处理节点最少需要 2 个分支" |
| 61041 | 分支数超过上限 | "并行处理节点分支数超过上限（最多 8 个分支）" |
| 61042 | 分支内无节点 | "并行处理节点分支[{index}]内无节点" |
| 65001 | 分支执行失败 | "并行处理节点分支[{index}]执行失败，{n}/{total} 个分支失败" |
| 65002 | 分支执行超时 | "并行处理节点分支[{index}]执行超时" |
| 65003 | 所有分支均失败 | "并行处理节点所有 {total} 个分支均执行失败" |

### 5.7 出口节点 (exit)

用户在页面上配置：响应体映射、响应头映射、状态码映射

| code | 场景 | messageZh 模板 |
|:---:|------|-----------|
| 61050 | 输出映射字段不存在 | "出口节点输出映射引用了不存在的字段：{field}" |
| 61051 | 输出映射格式错误 | "出口节点输出映射格式错误：{detail}" |
| 66001 | 响应体序列化失败 | "出口节点响应体序列化失败：{detail}" |
| 66002 | 响应头设置失败 | "出口节点响应头[{headerName}]设置失败：{detail}" |

### 5.8 编排通用错误

| code | 场景 | messageZh 模板 |
|:---:|------|-----------|
| 61001 | 编排配置解析失败 | "编排配置解析失败：{detail}" |
| 61002 | 触发节点缺失 | "编排中缺少触发器节点" |
| 61003 | 出口节点缺失 | "编排中缺少出口节点" |
| 61004 | 节点间边关系缺失 | "节点[{节点名}]缺少上游/下游连接" |

### 5.9 返回格式

#### 调试接口（始终 HTTP 200）

```json
{
  "flowId": "xxx",
  "executionId": "xxx",
  "status": "success|failed",
  "test": true,
  "steps": [
    {
      "nodeId": "script_1",
      "nodeType": "script",
      "labelCn": "数据转换",
      "status": "failed",
      "errorInfo": {
        "code": "63001",
        "messageZh": "脚本节点[数据转换]运行时错误：TypeError: Cannot read property 'data' of null，请检查 ctx 访问和变量引用",
        "messageEn": "Script node [数据转换] runtime error: TypeError: Cannot read property 'data' of null"
      }
    }
  ],
  "errorInfo": {
    "code": "63001",
    "messageZh": "脚本节点[数据转换]运行时错误：TypeError: Cannot read property 'data' of null，请检查 ctx 访问和变量引用",
    "messageEn": "Script node [数据转换] runtime error: TypeError: Cannot read property 'data' of null"
  }
}
```

#### 调用接口（HTTP 状态码 + X- 响应头）

```
HTTP/1.1 502 Bad Gateway
X-Flow-Id: xxx
X-Execution-Id: xxx
X-Code: 62001
X-Message-Zh: 连接器[发送消息]调用失败：HTTP 500，下游返回：{"error":"Internal Server Error"}
X-Message-En: Connector [发送消息] call failed: HTTP 500
X-Status: 1

(null body)
```

---

## 6. 错误消息规范

### 6.1 前置校验错误

前置校验在执行编排之前完成，消息直接返回给调用方，不应包含底层技术细节。

| 规范 | 正确示例 | 错误示例 |
|------|---------|---------|
| 用业务语言描述 | "连接流未启动，请先启动后再调用" | "Flow is not running: flowId=123" |
| 告诉用户下一步 | "编排配置为空，请先完成编排后再调试" | "NullPointerException: nodes is null" |
| 指明具体对象 | "版本状态为「待审批」，仅草稿和已发布版本可调试" | "Invalid status" |

### 6.2 执行层错误

执行层错误发生在编排执行过程中，消息需包含：
- **定位信息**：哪个节点出错（节点名/连接器名/脚本节点名）
- **错误类型**：连接器调用失败 / 脚本执行失败 / 超时
- **关键上下文**：下游 HTTP 状态码（连接器错误）、超时阈值（超时错误）

| 规范 | 正确示例 | 错误示例 |
|------|---------|---------|
| 含节点名 | "连接器[发送消息]调用失败：HTTP 500" | "Connection refused: localhost:18980" |
| 含上下文 | "脚本节点[data_transform]执行失败：SyntaxError at line 3" | "Script execution failed" |
| 含关键数据 | "节点[conn_1]执行超时（超过5000ms）" | "timeout" |

### 6.3 下游错误截断

连接器节点调用下游返回的错误 body 可能很长，截断至 **512 字符**，保留关键信息。

---

## 7. 实现计划

### Phase 1：isDebug 基础设施

| # | 修改 | 文件 |
|---|------|------|
| 1 | ExecutionContext 新增 `isDebug` 字段 | DagScheduler.ExecutionContext |
| 2 | DagScheduler 读 `ctx.isDebug()` 跳过执行记录写入 | DagScheduler |
| 3 | ConnectorNodeExecutor 读 `ctx.isDebug()` 跳过认证校验 | ConnectorNodeExecutor |
| 4 | ScriptNodeExecutor 读 `ctx.isDebug()` 放宽脚本语句上限 | ScriptNodeExecutor |
| 5 | FlowInvokeService 创建 context 时 `setDebug(false)` | FlowInvokeService |
| 6 | FlowVersionDebugService 创建 context 时 `setDebug(true)` | FlowVersionDebugService |

### Phase 2：前置校验补全

#### 2.1 调试侧新增校验

| # | 修改 | 文件 |
|---|------|------|
| 7 | 版本不存在 → 404（不再进入执行报 6002） | FlowVersionDebugService |
| 8 | 版本状态不支持调试（仅 DRAFT/PUBLISHED） → 422 | FlowVersionDebugService |
| 9 | 编排配置为空 → 422 | FlowVersionDebugService |
| 10 | 连接器不存在 → 404 | FlowVersionDebugService |
| 11 | 连接器版本不存在 → 404 | FlowVersionDebugService |
| 12 | 连接器版本已失效 → 422 | FlowVersionDebugService |
| 13 | 连接器已失效 → 422 | FlowVersionDebugService |

#### 2.2 调用侧新增校验

| # | 修改 | 文件 |
|---|------|------|
| 14 | 流不存在 → 404 | FlowInvokeService |
| 15 | 流未运行 → 409（由 500 修正） | FlowInvokeService.classifyError |
| 16 | 已部署版本不可用 → 422 | FlowInvokeService.loadFlowVersion |
| 17 | SYSTOKEN 认证失败 → 401 | FlowInvokeService |
| 18 | 连接器不存在 → 404 | FlowInvokeService |
| 19 | 连接器版本不存在 → 404 | FlowInvokeService |
| 20 | 连接器版本已失效 → 422 | FlowInvokeService |
| 21 | 连接器已失效 → 422 | FlowInvokeService |

#### 2.3 调用侧前置校验统一

| # | 修改 | 文件 |
|---|------|------|
| 22 | 请求参数错误 → 400（统一消息模板） | FlowInvokeService |
| 23 | 统一错误消息模板，去掉英文堆栈信息 | FlowInvokeService.buildErrorResponse |

### Phase 3：错误处理框架重构

| # | 修改 | 文件 |
|---|------|------|
| 24 | 定义 ErrorInfo 结构体（code + messageZh + messageEn + cause） | 新建 common 类 |
| 25 | 定义错误码常量类（61xxx~66xxx 全部码值） | 新建 ErrorCode 常量类 |
| 26 | DagScheduler 异常捕获后产出 NodeOutput.errorInfo（不吞异常细节） | DagScheduler |
| 27 | 调试侧移除 `onErrorResume` 重新包装，原样透传 NodeOutput.errorInfo | FlowVersionDebugService |
| 28 | 调用侧移除 `classifyError()` 字符串匹配，从 NodeOutput.errorInfo 读取业务码 | FlowInvokeService |
| 29 | 调用侧 HTTP 状态码映射：61xxx~66xxx → 502，超时 → 504 | FlowInvokeService.buildErrorResponse |

### Phase 4：节点层错误码产出

#### 4.1 编排通用

| # | 修改 | 文件 |
|---|------|------|
| 30 | 61001 编排配置 JSON 解析失败 | DagScheduler / FlowConfigParser |
| 31 | 61002 触发器节点缺失 | DagScheduler |
| 32 | 61003 出口节点缺失 | DagScheduler |
| 33 | 61004 节点间边关系缺失 | DagScheduler |

#### 4.2 触发器节点

| # | 修改 | 文件 |
|---|------|------|
| 34 | 61010 触发方式未配置 | TriggerNodeExecutor |
| 35 | 61011 触发凭证不存在 | TriggerNodeExecutor |
| 36 | 61012 触发凭证不在白名单 | TriggerNodeExecutor |

#### 4.3 连接器节点

| # | 修改 | 文件 |
|---|------|------|
| 37 | 61020~61025 连接器节点前置/配置错误（未选连接器、未选版本、超时超限、字段不存在、认证缺失、认证类型未选） | ConnectorNodeExecutor |
| 38 | 62001 HTTP 调用失败（含下游 statusCode + 截断 body） | ConnectorNodeExecutor |
| 39 | 62002 连接超时 | ConnectorNodeExecutor |
| 40 | 62003 读取超时 | ConnectorNodeExecutor |
| 41 | 62004 DNS 解析失败 | ConnectorNodeExecutor |
| 42 | 62005 SSL 握手失败 | ConnectorNodeExecutor |
| 43 | 62006 请求体序列化失败 | ConnectorNodeExecutor |
| 44 | 62007 响应体过大（截断逻辑） | ConnectorNodeExecutor |

#### 4.4 脚本节点

| # | 修改 | 文件 |
|---|------|------|
| 45 | 61030~61033 脚本前置/配置错误（空源码、超长、缺 main、语法错误） | ScriptNodeExecutor |
| 46 | 63001 脚本运行时异常 | ScriptNodeExecutor |
| 47 | 63002 脚本执行超时 | ScriptNodeExecutor |
| 48 | 63003 脚本超过语句上限 | ScriptNodeExecutor |
| 49 | 63004 脚本返回值不是对象 | ScriptNodeExecutor |
| 50 | 63005 访问不存在的上游字段 | ScriptNodeExecutor |

#### 4.5 并行处理节点

| # | 修改 | 文件 |
|---|------|------|
| 51 | 61040~61042 并行配置错误（分支数不足/超限、分支内无节点） | ParallelBranchExecutor |
| 52 | 65001 分支执行失败 | ParallelBranchExecutor |
| 53 | 65002 分支执行超时 | ParallelBranchExecutor |
| 54 | 65003 所有分支均失败 | ParallelBranchExecutor |

#### 4.6 出口节点

| # | 修改 | 文件 |
|---|------|------|
| 55 | 61050~61051 出口配置错误（映射字段不存在、格式错误） | ExitNodeExecutor |
| 56 | 66001 响应体序列化失败 | ExitNodeExecutor |
| 57 | 66002 响应头设置失败 | ExitNodeExecutor |

### Phase 5：返回格式适配

| # | 修改 | 文件 |
|---|------|------|
| 58 | 调试侧 ExecutionResult.steps[].errorInfo 产出细分 code（非 6002） | FlowVersionDebugService |
| 59 | 调试侧 ExecutionResult.errorInfo 产出细分 code（非 6002） | FlowVersionDebugService |
| 60 | 调用侧 X-Code 响应头写入业务错误码（6xxxx 码段） | FlowInvokeService |
| 61 | 调用侧 X-Message-Zh/X-Message-En 从 ErrorInfo 读取 | FlowInvokeService |

### Phase 6：下游错误截断

| # | 修改 | 文件 |
|---|------|------|
| 62 | 连接器节点下游响应 body 截断 ≤ 512 字符 | ConnectorNodeExecutor |

### Phase 7：测试对齐

#### 7.1 Java 单元测试（connector-api）

| # | 修改 | 文件 |
|---|------|------|
| 63 | ExecutionContext.isDebug 字段 getter/setter + 默认值 false | ExecutionContextTest |
| 64 | DagScheduler：isDebug=true 时跳过执行记录写入 | DagSchedulerTest |
| 65 | DagScheduler：节点异常后产出结构化 ErrorInfo（含 code + 节点名） | DagSchedulerTest |
| 66 | ConnectorNodeExecutor：61020~61025 配置错误各 1 条用例 | NodeExecutorsTest |
| 67 | ConnectorNodeExecutor：62001~62007 运行时错误各 1 条用例 | NodeExecutorsTest |
| 68 | ConnectorNodeExecutor：isDebug=true 跳过认证校验 | NodeExecutorsTest |
| 69 | ScriptNodeExecutor：61030~61033 配置错误各 1 条用例 | ScriptNodeExecutorTest |
| 70 | ScriptNodeExecutor：63001~63005 运行时错误各 1 条用例 | ScriptNodeExecutorTest |
| 71 | ScriptNodeExecutor：isDebug=true 放宽语句上限不抛 63003 | ScriptNodeExecutorTest |
| 72 | ParallelBranchExecutor：61040~61042 配置错误各 1 条用例 | 新建 ParallelBranchExecutorTest |
| 73 | ParallelBranchExecutor：65001~65003 运行时错误各 1 条用例 | ParallelBranchExecutorTest |
| 74 | ExitNodeExecutor：61050~61051 配置错误各 1 条用例 | NodeExecutorsTest |
| 75 | ExitNodeExecutor：66001~66002 运行时错误各 1 条用例 | NodeExecutorsTest |
| 76 | TriggerNodeExecutor：61010~61012 配置错误各 1 条用例 | NodeExecutorsTest |
| 77 | FlowVersionDebugService：版本不存在→404、状态不支持→422、编排为空→422 | FlowVersionDebugServiceTest |
| 78 | FlowVersionDebugService：连接器/版本不存在→404、连接器/版本失效→422 | FlowVersionDebugServiceTest |
| 79 | FlowVersionDebugService：执行失败后 ExecutionResult.errorInfo.code ≠ 6002（细分码） | FlowVersionDebugServiceTest |
| 80 | FlowInvokeService：流不存在→404、流未运行→409、版本不可用→422 | FlowInvokeServiceTest |
| 81 | FlowInvokeService：连接器/版本不存在→404、连接器/版本失效→422 | FlowInvokeServiceTest |
| 82 | FlowInvokeService：X-Code 正确映射业务错误码（非 classifyError 字符串匹配） | FlowInvokeServiceTest |
| 83 | ErrorCode 常量类所有码值唯一性校验 | 新建 ErrorCodeTest |

#### 7.2 Python 集成测试（connector-api）

| # | 修改 | 文件 |
|---|------|------|
| 84 | 调试接口：版本不存在 → HTTP 200 + errorInfo.code=404 | test_debug_draft_invoke.py |
| 85 | 调试接口：版本状态不支持调试（待审批/已撤回/已驳回）→ code=422 | test_debug_draft_invoke.py |
| 86 | 调试接口：编排为空 → code=422 | test_debug_draft_invoke.py |
| 87 | 调试接口：连接器不存在/版本不存在/失效 → code=404/422 | test_debug_draft_invoke.py |
| 88 | 调试接口：脚本语法错误 → code=61033（非 6002） | test_internal_test_run.py |
| 89 | 调用接口：流未运行 → HTTP 409 + X-Message-Zh 中文提示 | test_trigger_invoke.py |
| 90 | 调用接口：连接器调用失败 → X-Code=62001 + X-Message-Zh 含连接器名 | test_trigger_invoke.py |
| 91 | 调用接口：脚本执行失败 → X-Code=63001 | test_script_node_execution.py |
| 92 | 调用接口：脚本为空 → X-Code=61030 | test_script_node_execution.py |
| 93 | 调用接口：并行分支失败 → X-Code=65001 | test_parallel_branch.py |
| 94 | 调用接口：所有分支均失败 → X-Code=65003 | test_parallel_branch.py |
| 95 | 调用接口：节点超时 → X-Code 含 62003/63002/65002 之一 + X-Message-Zh 含超时阈值 | test_node_timeout.py |
| 96 | 调用接口：SYSTOKEN 白名单拒绝 → X-Code=61012 | test_systoken_whitelist.py |
| 97 | 下游 body 截断：mock 服务器返回 >512 字符 → X-Message-Zh 截断 | 新建 test_error_response.py |
| 98 | 综合：6 种节点类型各至少 1 条错误码场景覆盖 | test_error_response.py |

### Phase 8：isTest → isDebug 重命名

| # | 修改 | 文件 |
|---|------|------|
| 99 | `isTest` 字段/变量/方法全量重命名为 `isDebug` | connector-api 全模块 |
| 100 | ExecutionResult 中 `test` JSON 字段改为 `debug` | ExecutionResult |
| 101 | FlowVersionDebugService 中 `isTest` 参数重命名 | FlowVersionDebugService |
| 102 | 前端调试面板中 `isTest` 参数重命名（需协调） | 非本文档范围，记录备忘 |

---

## 修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0-draft | 2026-07-01 | 初始版本：现状分析 + 统一错误码体系 | Summer |
| v1.1-draft | 2026-07-01 | 重写 §7 实现计划为 8 个 Phase 102 条；新增测试对齐 + isTest→isDebug | Summer |
