# 临时方案: #54 调用连接流 — 返回值格式调整（透明穿透模式）

**Feature ID**: CONN-PLAT-002  
**关联文档**: plan-api.md §3.9 #54, ADR-008  
**创建日期**: 2026-06-18  
**状态**: DRAFT — **待评审**  

---

## 0. 概述 & 约束

### 出发点 (Motivation)

当前 `#54` 接口的响应被包裹在 `ExecutionResult` 信封中，用户通过出口节点配置的 `output.body` 实际被嵌套在 `resultData.body` 内，`output.header` 被嵌套在 `resultData.header` 中。这导致**调用连接流的第三方系统必须适配平台的信封格式**，无法直接按照连接流中声明的 Schema 来解析响应——它们需要先解包 `resultData.body` 才能拿到自己的业务数据。

### 改造目标

本方案仅针对 `POST /api/v1/trigger/{flowId}/invoke` 的**返回值格式**进行改造：

- ✅ **响应 Body**：从 `ExecutionResult.resultData.body` 变为出口节点 `output.body` 裸数据
- ✅ **用户自定义响应头**：从 `resultData.header`（JSON 内嵌）提出来变为真正的 HTTP 响应头
- ✅ **平台元数据**：从 Body JSON 字段上移为 `X-` 前缀 HTTP 响应头
- ❌ **接口 URL 不变**：`POST /api/v1/trigger/{flowId}/invoke`
- ❌ **业务逻辑不变**：编排执行、认证、限流、入参校验全部保持原样

---

## 1. 当前状态 vs 目标状态

### 1.1 当前实现（现网代码）

| 维度 | 当前值 | 代码位置 |
|------|--------|----------|
| **URL** | `POST /api/v1/trigger/{flowId}/invoke` | `OpTriggerController:35,58` |
| **返回类型** | `Mono<ExecutionResult>` | `OpTriggerController:63` |
| **响应 Body** | 完整 `ExecutionResult` JSON：`{executionId, flowId, status, resultData, totalDurationMs, steps, errorInfo, isTest}` | `ExecutionResult.java` |
| **出口 header** | 嵌套在 `resultData.header` 中（JSON body 内部） | `ExitNodeExecutor:101` → `ReactiveSequentialExecutor:245 setResultData(cleanOutput)` |
| **出口 body** | 嵌套在 `resultData.body` 中（JSON body 内部） | 同上 |
| **平台元数据** | 混在 Body 的 `ExecutionResult` 字段中 | 同上 |
| **成功执行 HTTP Status** | `200`，Body 含完整 `ExecutionResult` | Spring WebFlux 自动序列化 |
| **前置校验失败格式** | 各场景不统一（见下表） | — |
| **hasSteps 控制** | 在 Controller 层通过 `result.getSteps().clear()` 实现 | `OpTriggerController:76-80` |

**前置校验/异常场景当前响应：**

| 场景 | HTTP Status | Body | 来源 |
|------|-------------|------|------|
| 限流 (429) | 429 | `{status:"failed", errorInfo:{code:"429", messageZh, messageEn}}` | `OpRateLimitFilter:154-177` |
| 通用异常 (500) | 500 | `{status:"failed", errorInfo:{code:"500", messageZh, messageEn, cause}}` | `DefaultErrorHandler:104-113` |
| 服务层业务错误 | 200 | `ExecutionResult{status:"failed", errorInfo:{code:"6001", ...}}` | `OpTriggerService:190-203` |
| 编排无节点 | 200 | `ExecutionResult{status:"failed", errorInfo:{code:"6001", ...}}` | `ReactiveSequentialExecutor:330-347` |

### 1.2 目标状态（对齐 ADR-008 / plan-api.md §3.9 #54 v7.0）

| 维度 | 当前值 | 目标值 |
|------|--------|--------|
| **URL** | `POST /api/v1/trigger/{flowId}/invoke` | **不变** |
| **响应 Body** | 完整 `ExecutionResult` JSON 信封 | 出口节点 `output.body` 裸数据，**无任何平台信封包装** |
| **出口 header** | 嵌套在 `resultData.header`（JSON body 内部） | 设置为实际 HTTP 响应头 |
| **平台元数据** | 混在 Body 的 `ExecutionResult` 字段中 | 全部上移为 `X-` 前缀 HTTP 响应头 |
| **成功执行 HTTP Status** | `200` + `ExecutionResult` body | `200` + 出口 body |
| **前置校验失败** | 格式不统一（见 §1.1 表） | 对应 HTTP Status + X- 头，**Body 为空** |

**平台元数据响应头（X- 前缀）:**

| Header | 出现条件 |
|--------|---------|
| `X-Flow-Id` | 始终返回 |
| `X-Execution-Id` | 连接流已执行（含执行失败） |
| `X-Status` | 连接流已执行：`0`=成功 / `1`=失败 / `2`=超时 |
| `X-Duration-Ms` | 连接流已执行 |
| `X-Cache-Status` | 缓存已生效：`0`=未命中 / `1`=全流命中 / `2`=部分命中 |
| `X-Code` | 始终返回：`200`=成功，其余见错误码 |
| `X-Message-Zh` | 始终返回 |
| `X-Message-En` | 始终返回 |

**前置校验失败的 HTTP Status 约定：**

| 场景 | HTTP Status | X-Code | 返回 X- 头 | Body |
|------|-------------|--------|-----------|------|
| SYSTOKEN 不在白名单 | `401` | 401 | X-Flow-Id, X-Code, X-Message-Zh, X-Message-En | 空 |
| 入站限流 | `429` | 429 | X-Flow-Id, X-Code, X-Message-Zh, X-Message-En | 空 |
| 连接流未部署 | `503` | 503 | X-Flow-Id, X-Code, X-Message-Zh, X-Message-En | 空 |
| 引擎内部错误 | `500` | 500 | X-Flow-Id, X-Code, X-Message-Zh, X-Message-En | 空 |

---

## 2. 文件影响分析

### 2.1 需要修改的文件（核心路径，4个文件）

```
[MODIFY] connector-api/.../trigger/controller/OpTriggerController.java
[MODIFY] connector-api/.../trigger/service/OpTriggerService.java
[MODIFY] connector-api/.../common/interceptor/OpRateLimitFilter.java
[MODIFY] connector-api/.../common/exception/DefaultErrorHandler.java
```

### 2.2 需要新增的文件

```
[NEW] connector-api/.../runtime/model/TransparentFlowResponse.java
```

### 2.3 需要更新的测试文件

```
[MODIFY] connector-api/src/test/python/inspect/trigger_invoke.py
[MODIFY] connector-api/src/test/java/.../trigger/controller/TriggerControllerWebFluxTest.java
```

### 2.4 不受影响的文件（明确排除）

```
[NO CHANGE] ReactiveSequentialExecutor.java — 零侵入。仍产出 ExecutionResult，透明穿透转换在 Service/Controller 层完成
[NO CHANGE] ExitNodeExecutor.java            — 出口节点 output 构建逻辑完全不变（仍产出 {header, body} 结构）
[NO CHANGE] ConnectorNodeExecutor.java       — 连接器节点执行逻辑不变
[NO CHANGE] DataProcessorExecutor.java       — 数据处理节点执行逻辑不变
[NO CHANGE] TriggerNodeExecutor.java         — 触发器节点执行逻辑不变
[NO CHANGE] ExpressionResolver.java          — 表达式解析逻辑不变
[NO CHANGE] ExecutionResult.java             — 保留作为内部模型（#53 调试接口继续使用）
[NO CHANGE] OpDebugProxyController.java      — open-server 代理不变
[NO CHANGE] OpTestRunController.java         — 测试运行接口不变
```

---

## 3. 详细修改方案

### 3.1 Controller 层改造 — `OpTriggerController.java`

**目标**: 将返回类型从 `Mono<ExecutionResult>` 改为直接操作 `ServerWebExchange` 构造 HTTP 响应。

**改造步骤**:

```java
// 当前:
public Mono<ExecutionResult> invokeFlow(
    @PathVariable Long flowId,
    @RequestBody(required = false) Map<String, Object> triggerData,
    @RequestHeader Map<String, String> allHeaders,
    @RequestParam(required = false) Map<String, String> queryParams)

// 目标:
public Mono<Void> invokeFlow(
    @PathVariable Long flowId,
    @RequestBody(required = false) Map<String, Object> triggerData,
    @RequestHeader Map<String, String> allHeaders,
    @RequestParam(required = false) Map<String, String> queryParams,
    ServerWebExchange exchange)
```

**或使用 `ResponseEntity` 方案（推荐，测试友好）**:

引入一个中间模型 `TransparentFlowResponse`：

```java
public class TransparentFlowResponse {
    Map<String, Object> body;           // 出口节点 body (null=空Body)
    Map<String, String> userHeaders;    // 出口节点 header
    Map<String, String> platformHeaders;// X- 平台元数据头
    HttpStatus httpStatus;              // HTTP 状态码
}
```

Controller 返回 `Mono<ResponseEntity<?>>` 或 `Mono<TransparentFlowResponse>`，在 Controller 层（或一个 HandlerMethodReturnValueHandler）将 `TransparentFlowResponse` 展开为原始 HTTP 响应。

**推荐方案**: 使用 `Mono<ResponseEntity<?>>`，在 Service 返回 `TransparentFlowResponse`，Controller 将其转换为 `ResponseEntity`：

```java
public Mono<ResponseEntity<Object>> invokeFlow(...) {
    return triggerService.invokeFlow(...)
        .map(response -> {
            HttpHeaders headers = new HttpHeaders();
            response.getPlatformHeaders().forEach(headers::add);
            response.getUserHeaders().forEach((k,v) -> {
                if (!k.startsWith("X-Flow-") && !k.startsWith("X-Execution-")
                    && !k.startsWith("X-Status") && !k.startsWith("X-Duration-")
                    && !k.startsWith("X-Cache-") && !k.startsWith("X-Code")
                    && !k.startsWith("X-Message-")) {
                    headers.add(k, String.valueOf(v));
                }
            });
            return new ResponseEntity<>(response.getBody(), headers, response.getHttpStatus());
        });
}
```

### 3.2 新增模型 — `TransparentFlowResponse.java`

```java
package com.xxx.it.works.wecode.v2.modules.runtime.model;

import org.springframework.http.HttpStatus;
import java.util.LinkedHashMap;
import java.util.Map;

public class TransparentFlowResponse {
    private Object body;                         // null = 空Body
    private Map<String, String> userHeaders = new LinkedHashMap<>();    // 出口 header
    private Map<String, String> platformHeaders = new LinkedHashMap<>(); // X- 平台元数据
    private HttpStatus httpStatus = HttpStatus.OK;

    // Builder 模式
    public static TransparentFlowResponse success(
            String flowId, String executionId, int status, long durationMs,
            Map<String, String> userHeaders, Object body) {
        TransparentFlowResponse r = new TransparentFlowResponse();
        r.body = body;
        r.userHeaders = (userHeaders != null) ? userHeaders : new LinkedHashMap<>();
        r.platformHeaders.put("X-Flow-Id", flowId);
        r.platformHeaders.put("X-Execution-Id", executionId);
        r.platformHeaders.put("X-Status", String.valueOf(status));
        r.platformHeaders.put("X-Duration-Ms", String.valueOf(durationMs));
        r.platformHeaders.put("X-Code", "200");
        r.platformHeaders.put("X-Message-Zh", "成功");
        r.platformHeaders.put("X-Message-En", "Success");
        r.httpStatus = HttpStatus.OK;
        return r;
    }

    public static TransparentFlowResponse preExecutionError(
            String flowId, HttpStatus httpStatus, String code,
            String messageZh, String messageEn) {
        TransparentFlowResponse r = new TransparentFlowResponse();
        r.body = null; // 空 Body
        r.platformHeaders.put("X-Flow-Id", flowId);
        r.platformHeaders.put("X-Code", code);
        r.platformHeaders.put("X-Message-Zh", messageZh);
        r.platformHeaders.put("X-Message-En", messageEn);
        r.httpStatus = httpStatus;
        return r;
    }

    // getters...
}
```

### 3.3 Service 层改造 — `OpTriggerService.java`

**改造要点**:
1. 返回值从 `Mono<ExecutionResult>` 改为 `Mono<TransparentFlowResponse>`
2. 成功路径：从 `ReactiveSequentialExecutor` 的执行结果中提取 `resultData.header`/`resultData.body`，构建 `TransparentFlowResponse.success(...)`
3. `onErrorResume` 中的校验失败错误：区分"前置校验失败"（401/422/503）和"执行期异常"（500），使用 `TransparentFlowResponse.preExecutionError(...)`
4. flowId 需要始终保持可用（即使在异常路径）

**关键代码变更**（伪代码）:

```java
// 当前返回: Mono<ExecutionResult>
// 目标返回: Mono<TransparentFlowResponse>

public Mono<TransparentFlowResponse> invokeFlow(...) {
    String executionId = UUID.randomUUID().toString().replace("-", "");
    String flowIdStr = String.valueOf(flowId);

    return loadFlowVersion(flowId)
        .switchIfEmpty(Mono.error(new FlowNotFoundException(flowIdStr)))
        .flatMap(flowVersion -> {
            // ... 校验逻辑不变 ...
            return executor.execute(context, flowVersion.getOrchestrationConfig())
                .map(executionResult -> buildTransparentResponse(flowIdStr, executionResult));
        })
        .onErrorResume(FlowNotFoundException.class, e -> 
            Mono.just(TransparentFlowResponse.preExecutionError(
                flowIdStr, HttpStatus.NOT_FOUND, "404",
                "连接流不存在", "Flow not found")))
        .onErrorResume(AuthException.class, e ->
            Mono.just(TransparentFlowResponse.preExecutionError(
                flowIdStr, HttpStatus.UNAUTHORIZED, "401",
                "SYSTOKEN 不在白名单中", "SYSTOKEN not in whitelist")))
        .onErrorResume(e -> {
            log.error("Trigger invoke failed: flowId={}, error={}", flowId, e.getMessage());
            return Mono.just(TransparentFlowResponse.preExecutionError(
                flowIdStr, HttpStatus.INTERNAL_SERVER_ERROR, "500",
                "触发执行失败: " + e.getMessage(),
                "Trigger execution failed: " + e.getMessage()));
        });
}
```

### 3.4 执行器层改造 — `ReactiveSequentialExecutor.java`

**核心变更**: `buildResult()` 方法不改变逻辑，但 `execute()` 返回的 `ExecutionResult` 中 `resultData` 需保持包含 `{header: {...}, body: {...}}` 结构。Service 层从 `resultData` 中提取 `header` 和 `body` 分别放入 HTTP 响应头和响应体。

**最小变更方案**:
- `execute()` 返回类型保持 `Mono<ExecutionResult>`
- Service 层（`OpTriggerService`）从 `ExecutionResult.resultData` 中提取 `header`/`body` 来构建 `TransparentFlowResponse`
- `buildResult()` 中清理 `__status`/`__input`/`__error` 的逻辑保持不变

> 💡 **此方案零侵入 `ReactiveSequentialExecutor`**：执行器仍产出完整 `ExecutionResult`，透明穿透转换全部在 Service/Controller 层完成。

### 3.5 限流过滤器改造 — `OpRateLimitFilter.java`

**当前**:
```java
// HTTP 429, Body: {status:"failed", errorInfo:{code:"429", messageZh, messageEn}}
exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
// write JSON body...
```

**目标**:
```java
// HTTP 429, 空 Body, X- 响应头带错误信息（URL 匹配不变：/api/v1/trigger/.../invoke）
exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
String flowId = extractFlowIdFromPath(exchange); // 从 /api/v1/trigger/{flowId}/invoke 提取
exchange.getResponse().getHeaders().add("X-Flow-Id", flowId);
exchange.getResponse().getHeaders().add("X-Code", "429");
exchange.getResponse().getHeaders().add("X-Message-Zh", "请求频率超限");
exchange.getResponse().getHeaders().add("X-Message-En", "Too many requests");
// 不写 Body
return exchange.getResponse().setComplete();
```

> ⚠️ URL 匹配规则保持现有 `/api/v1/trigger/\\d+/invoke`，**不改动**。

### 3.6 全局异常处理器改造 — `DefaultErrorHandler.java`

**当前**:
```java
@ExceptionHandler(Exception.class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public Mono<Map<String, Object>> handleException(Exception e) {
    // 返回 {status:"failed", errorInfo:{...}} JSON body
}
```

**目标**: 返回带 `X-` 头的空 Body 响应。但 `@RestControllerAdvice` 的 `@ExceptionHandler` 返回 `Mono<Void>` 需要直接操作 `ServerWebExchange`。

**方案 A**（推荐）: 让 Service 层 `.onErrorResume()` 捕获所有异常，`DefaultErrorHandler` 不再兜底 #54 的异常。

**方案 B**: 在 `DefaultErrorHandler` 中判断请求路径，对 `/api/v1/flows/` 返回特殊格式。

**推荐方案 A**: 因为 `OpTriggerService.invokeFlow()` 中已有 `.onErrorResume()` 兜底所有异常（line 190-203），可将最终的 `RuntimeException` 也转换为 `TransparentFlowResponse`。同时缩小 `DefaultErrorHandler` 的 `basePackages` 或从 `OpTriggerController` 包中排除。

---

## 4. 实施步骤

### Phase 1: 模型层（低风险、可先行）

| 步骤 | 文件 | 操作 | 预估 |
|------|------|------|:--:|
| 1.1 | `TransparentFlowResponse.java` | **新增** 中间模型，含 Builder 方法 | 15 min |
| 1.2 | — | 编译验证 | 5 min |

### Phase 2: Service 层（核心逻辑）

| 步骤 | 文件 | 操作 | 预估 |
|------|------|------|:--:|
| 2.1 | `OpTriggerService.java` | 返回类型 `ExecutionResult` → `TransparentFlowResponse` | 30 min |
| 2.2 | `OpTriggerService.java` | 从 `resultData` 中拆解 `header`/`body` | 20 min |
| 2.3 | `OpTriggerService.java` | `onErrorResume` 改造：分类错误码 | 20 min |
| 2.4 | — | 单元测试 `TriggerServiceTest.java` 适配 | 20 min |

### Phase 3: Controller 层

| 步骤 | 文件 | 操作 | 预估 |
|------|------|------|:--:|
| 3.1 | `OpTriggerController.java` | 返回类型 `Mono<ResponseEntity<?>>` | 15 min |
| 3.2 | `OpTriggerController.java` | 移除 `hasSteps` 逻辑（或改为通过 X-Steps header 返回） | 10 min |
| 3.3 | — | `TriggerControllerWebFluxTest.java` 适配 | 20 min |

### Phase 4: 错误路径

| 步骤 | 文件 | 操作 | 预估 |
|------|------|------|:--:|
| 4.1 | `OpRateLimitFilter.java` | 429 响应格式改为 X- 头 + 空 Body | 20 min |
| 4.2 | `DefaultErrorHandler.java` | 确认不再需要为 #54 兜底 | 10 min |

### Phase 5: 集成测试适配

| 步骤 | 文件 | 操作 | 预估 |
|------|------|------|:--:|
| 5.1 | `trigger_invoke.py` | 响应断言从 `body['resultData']['body']` → `response.json()` | 30 min |
| 5.2 | `trigger_invoke.py` | 新增 X- 响应头断言 | 20 min |
| 5.3 | — | 端到端联调 | 30 min |

---

## 5. 转换逻辑伪代码

```java
// ===== OpTriggerService 中新增的转换方法 =====

private TransparentFlowResponse buildTransparentResponse(
        String flowId, ExecutionResult executionResult) {
    
    // 1. 提取出口节点的 header 和 body
    Map<String, Object> resultData = executionResult.getResultData();
    Map<String, String> userHeaders = new LinkedHashMap<>();
    Object responseBody = null;
    
    if (resultData != null) {
        // 提取 header（出口节点 output.header）放入 HTTP 响应头
        Object headerObj = resultData.get("header");
        if (headerObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> headers = (Map<String, Object>) headerObj;
            headers.forEach((k, v) -> userHeaders.put(k, String.valueOf(v)));
        }
        
        // 提取 body（出口节点 output.body）作为 HTTP 响应体
        Object bodyObj = resultData.get("body");
        if (bodyObj != null) {
            responseBody = bodyObj; // 直接透传，不包装
        }
    }
    
    // 2. 确定执行状态
    int execStatus = mapStatus(executionResult.getStatus());
    
    // 3. 构建透明穿透响应
    TransparentFlowResponse response = TransparentFlowResponse.success(
        flowId,
        executionResult.getExecutionId(),
        execStatus,
        executionResult.getTotalDurationMs(),
        userHeaders,
        responseBody
    );
    
    // 4. 设置缓存状态（V3 扩展，当前固定为 0=未命中）
    response.addPlatformHeader("X-Cache-Status", "0");
    
    return response;
}

private int mapStatus(String status) {
    switch (status) {
        case "success": return 0;
        case "failed":  return 1;
        case "timeout": return 2;
        default:        return 1;
    }
}
```

---

## 6. 兼容性说明

### 6.1 不影响的功能

| 功能 | 说明 |
|------|------|
| #53 调试执行 | 继续使用 `ExecutionResult` 格式 |
| #51 调试代理 | open-server 代理不变 |
| open-server 管理面 54 个接口 | 继续使用标准响应信封 |
| 编排执行引擎 | `ReactiveSequentialExecutor` 逻辑零变更 |
| 节点执行器 | 全部 4 种 Executor 零变更 |

### 6.2 需注意的边界

| 场景 | 处理方式 |
|------|---------|
| `hasSteps` 功能 | 目标方案中不再有 `hasSteps` 概念（原为 `ExecutionResult` 内嵌功能）。如需保留，可改为 `X-Steps: true` 请求头触发，steps 数据通过 `X-Steps-Data` 头（JSON 编码）返回 |
| exit 节点无 `outputMapping` 时 | `ExitNodeExecutor.collectFallbackOutputs()` 会产出 `{__collectMode:true, ...}` 格式，此时 `resultData` 不含 `header`/`body` 分区。需在 Service 层 fallback：若无 `body` 则整个 `resultData`（去除 `__` 前缀字段）作为 Body |
| exit 节点无 header 映射 | `resultData.header` 可能为空 Map 或不存在，此时不设置任何用户自定义 HTTP 响应头 |
| 缓存命中状态 | 当前执行引擎未实现全流缓存，`X-Cache-Status` 固定返回 `0`（未命中），待 V3 节点级缓存实现后更新 |

---

## 7. 风险与缓解

| 风险 | 级别 | 缓解措施 |
|------|:--:|------|
| 外部调用方依赖当前 `ExecutionResult` 格式 | 🔴 高 | 同步排查是否有外部系统已接入此接口。如有，提前通知变更窗口 |
| `hasSteps` 功能丢失影响调试 | 🟡 中 | 改为 `X-Steps: true` 请求头触发，steps 数据通过 `X-Steps-Data` 响应头（JSON 编码）返回，或单独查询 #50 运行记录接口 |
| 异常路径格式不一致 | 🟢 低 | 统一通过 `TransparentFlowResponse.preExecutionError()` 工厂方法 |
| 集成测试大量失败 | 🟡 中 | Phase 5 专项处理，建议先跑集成测试摸底断点再批量修复 |
| `DefaultErrorHandler` 影响到其他端点 | 🟢 低 | 只调整 `/api/v1/trigger/` 路径下的异常处理，不影响其他 Controller |

---

## 8. 附录：调用方对比（Before / After）

### 8.1 前提：用户在连接流出口节点配置了什么

```
出口节点 outputMapping:
  header → X-Request-Id = ${$.execution.id}
  body   → { msgId: ${$.node.conn_1.output.body.msgId}, code: ${$.node.conn_1.output.body.code} }
```

调用方期望按这个 Schema 直接消费响应——拿到 `body` 就是 `{msgId, code}`，拿到 `header` 就是 `X-Request-Id`。

### 8.2 成功执行场景

**Before（当前）**——调用方实际收到的：

```
HTTP 200
Content-Type: application/json

{
  "executionId": "abc123",        ← 平台字段
  "flowId": "4444",               ← 平台字段
  "status": "success",
  "resultData": {                 ← 信封层：用户配置的 header/body 被塞在这里
    "header": {"X-Request-Id": "req-001"},
    "body": {"msgId": "msg_xxx", "code": 0}
  },
  "totalDurationMs": 234,
  "steps": [],
  ...                              ← 还有更多平台字段
}
```

**问题**：调用方按出口 Schema 期望 `response.msgId`，实际必须写 `response.resultData.body.msgId`。

---

**After（目标）**——调用方实际收到的：

```
HTTP 200
X-Flow-Id: 4444                 ← 平台元数据 → 响应头
X-Execution-Id: abc123
X-Status: 0
X-Duration-Ms: 234
X-Code: 200
X-Request-Id: req-001           ← 用户自定义头（来自出口 output.header）

{"msgId": "msg_xxx", "code": 0}  ← 响应体就是出口 output.body，无信封
```

**效果**：调用方 `response.msgId`，与出口 Schema 完全一致，零适配。

### 8.3 限流场景

| | Before | After |
|---|--------|-------|
| HTTP Status | 429 | 429（不变） |
| Body | `{"status":"failed","errorInfo":{...}}` | **空** |
| 错误信息 | 埋在 JSON body 里 | `X-Code: 429` / `X-Message-Zh: 请求频率超限`（响应头） |

### 8.4 前置校验失败（SYSTOKEN 不在白名单）

| | Before | After |
|---|--------|-------|
| HTTP Status | **200**（错误） | **401**（正确） |
| Body | `ExecutionResult{status:"failed", errorInfo:{code:"6001", ...}}` | **空** |
| 错误信息 | 埋在 JSON body 里，且 code 是 `6001`（平台内部码） | `X-Code: 401`（标准 HTTP 语义）/ `X-Message-Zh: SYSTOKEN 不在白名单中` |

> 💡 **Before 中认证失败返回 HTTP 200 是个 bug**：调用方的 HTTP 客户端不会认为出错了，必须解析 JSON body 才能判断。After 中返回 401 + 空 Body + X- 头语义正确。

---

> **下一步**: 将本临时方案提交评审，确认后拆解为 `@sddu-tasks` 可执行原子任务。
