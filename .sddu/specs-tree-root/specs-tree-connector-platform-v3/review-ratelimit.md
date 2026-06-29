# 限流配置位置审查

> 审查日期：2026-06-29
> 审查依据：`plan-json-schema.md` §6.3.3 flowConfig — 流级配置
> 审查范围：connector-api 限流配置读取逻辑

---

## 1. 问题描述

### 1.1 规范定义

根据 `plan-json-schema.md` §6.3.3，入站限流配置应定义在 **`flowConfig.rateLimitConfig`**（连接流级配置）：

```json
{
  "flowConfig": {
    "flowMode": "single",
    "rateLimitConfig": { "maxQps": 100, "maxConcurrency": 20 },
    "cache": { "key": [...], "ttl": 60 }
  }
}
```

> `rateLimitConfig`：入站限流配置，复用 `rateLimitConfigDef`（§4.3.3）。`maxQps`：每秒最大请求数（1-1000）；`maxConcurrency`：最大并发数（1-1000）。

触发器等其他节点暂不开放限流配置（§4.3.10 connectorNodeDataDef v2 已移除节点层 rateLimitConfig）。

### 1.2 代码实际读取位置

当前代码从 **`triggerNode.data.rateLimitConfig`**（触发器节点 data 下）读取限流配置，与规范不符。

---

## 2. 涉及的代码位置

### 2.1 RateLimitConfigReader — 限流配置读取器（核心问题）

**文件**：`connector-api/src/main/java/.../modules/ratelimit/RateLimitConfigReader.java`

**方法**：`extractRateLimitConfig(FlowVersionEntity entity)`（第70-118行）

**当前逻辑**：
```
FlowVersionEntity
  → entity.getTriggerConfig(objectMapper)     // 取 trigger 节点
  → triggerConfig.get("data")                  // 取 data
  → data.get("rateLimitConfig")               // ❌ 从 trigger.data 读取
```

**应改为**：
```
FlowVersionEntity
  → entity.parseOrchestrationConfigAsMap(objectMapper)
  → config.get("flowConfig")                   // 取 flowConfig
  → flowConfig.get("rateLimitConfig")          // ✅ 从 flowConfig 读取
```

### 2.2 FlowInvokeService.validateRateLimitConfig — 运行时校验

**文件**：`connector-api/src/main/java/.../modules/flow/service/FlowInvokeService.java`

**方法**：`validateRateLimitConfig(Map<String, Object> nodeData)`（第898行）

**当前逻辑**：从 `nodeData`（trigger 节点的 data）中取 `rateLimitConfig` 校验。

**调用处**（第376行）：`validateRateLimitConfig(nodeData)` — 传入的是 trigger 节点的 data。

**应改为**：从编排配置的 `flowConfig.rateLimitConfig` 读取并校验。

### 2.3 FlowInvokeController — 注释错误

**文件**：`connector-api/src/main/java/.../modules/flow/controller/FlowInvokeController.java`

**第37行注释**：`限流校验: data.rateLimitConfig.maxQps`

**应改为**：`限流校验: flowConfig.rateLimitConfig.maxQps`

### 2.4 FlowVersionEntity — 注释过时

**文件**：`connector-api/src/main/java/.../modules/flow/entity/FlowVersionEntity.java`

**第21行注释**：提到 rateLimitConfig 在 data 下，已过时。

### 2.5 已正确解析但未使用的代码

**文件**：`connector-api/src/main/java/.../modules/runtime/FlowConfigParser.java`

**第46-52行**：已正确从 `flowConfig.rateLimitConfig` 解析 `maxQps`/`maxConcurrency` 到 `FlowConfig` 对象，但运行时限流器（`InboundRateLimiter`）未使用此对象，而是独立通过 `RateLimitConfigReader` 重新查 DB 读取。

---

## 3. 影响分析

### 3.1 功能影响

| 场景 | 影响 |
|------|------|
| 用户在 `flowConfig.rateLimitConfig` 配置限流 | ❌ **不生效** — 限流器读的是 trigger.data，读不到 flowConfig 的配置，使用默认值 1000 |
| 用户在 `triggerNode.data.rateLimitConfig` 配置限流 | ✅ 生效（但不规范，该位置不应有限流配置） |
| 用户两处都配了 | 以 trigger.data 为准，flowConfig 的被忽略 |

### 3.2 数据一致性

- 发布校验（open-server `FlowPublishValidator`）校验的是 `flowConfig.rateLimitConfig` — ✅ 正确
- 运行时读取（connector-api `RateLimitConfigReader`）读的是 `triggerNode.data.rateLimitConfig` — ❌ 错误

**两者读不同位置，导致发布时校验通过的配置运行时不生效。**

---

## 4. 推荐解决方案

### 4.1 修改 RateLimitConfigReader（核心修复）

将 `extractRateLimitConfig` 的读取路径从 `triggerNode.data.rateLimitConfig` 改为 `flowConfig.rateLimitConfig`。

```java
private RateLimitConfig extractRateLimitConfig(FlowVersionEntity entity) {
    try {
        Map<String, Object> config = entity.parseOrchestrationConfigAsMap(objectMapper);
        Object flowConfigObj = config.get("flowConfig");
        if (!(flowConfigObj instanceof Map)) {
            return defaultConfig();
        }
        Map<String, Object> flowConfig = (Map<String, Object>) flowConfigObj;

        Object rateLimitObj = flowConfig.get("rateLimitConfig");
        if (!(rateLimitObj instanceof Map)) {
            return defaultConfig();
        }
        Map<String, Object> rateLimitConfig = (Map<String, Object>) rateLimitObj;

        // 读取 mode (默认为 "qps")
        String mode = "qps";
        Object modeObj = rateLimitConfig.get("mode");
        if (modeObj instanceof String) {
            mode = (String) modeObj;
        }

        // 读取 maxQps (取 min(flow值, app最大值))
        int maxQps = DEFAULT_QPS;
        Object maxQpsObj = rateLimitConfig.get("maxQps");
        if (maxQpsObj instanceof Number) {
            maxQps = Math.min(((Number) maxQpsObj).intValue(), APP_MAX_QPS);
            if (maxQps < 1) {
                maxQps = 1;
            }
        }

        // 读取 maxConcurrency (取 min(flow值, app最大值))
        int maxConcurrency = DEFAULT_CONCURRENCY;
        Object maxConObj = rateLimitConfig.get("maxConcurrency");
        if (maxConObj instanceof Number) {
            maxConcurrency = Math.min(((Number) maxConObj).intValue(), APP_MAX_CONCURRENCY);
            if (maxConcurrency < 1) {
                maxConcurrency = 1;
            }
        }

        return new RateLimitConfig(mode, maxQps, maxConcurrency);

    } catch (Exception e) {
        log.warn("Failed to parse rate limit config for flowId={}, using defaults", entity.getFlowId(), e);
        return defaultConfig();
    }
}
```

### 4.2 修改 FlowInvokeService.validateRateLimitConfig

将校验方法改为从 `flowConfig.rateLimitConfig` 读取：

```java
private void validateRateLimitConfig(Map<String, Object> config) {
    Object flowConfigObj = config.get("flowConfig");
    if (!(flowConfigObj instanceof Map)) {
        return;
    }
    Map<String, Object> flowConfig = (Map<String, Object>) flowConfigObj;
    Map<String, Object> rateLimitConfig = (Map<String, Object>) flowConfig.get("rateLimitConfig");
    if (rateLimitConfig == null) {
        return;
    }
    // ... 校验 maxQps / maxConcurrency 范围（保持不变）
}
```

调用处（第376行）改为传入编排配置 `config` 而非 `nodeData`。

### 4.3 修正注释

- `FlowInvokeController.java:37`：`data.rateLimitConfig` → `flowConfig.rateLimitConfig`
- `FlowVersionEntity.java:21`：更新编排配置结构注释，移除 data 下的 rateLimitConfig 描述

### 4.4 统一上限不一致

当前两处校验上限不一致：
- `RateLimitConfigReader`：`APP_MAX_QPS = 1000`
- `FlowInvokeService.validateRateLimitConfig`：`maxQps > 10000`

建议统一为 1000（与规范 §6.3.3 的 1-1000 一致）。

### 4.5 可选优化：限流配置缓存

当前 `RateLimitConfigReader` 每次请求都查 DB（无缓存）。建议复用 `EntityCacheManager.getFlowVersion()` 缓存，或单独缓存限流配置（短 TTL，如 30s）。

---

## 5. 测试影响

### 5.1 Python E2E 测试

需检查 E2E 测试中限流配置的放置位置：
- 如果测试在 `triggerNode.data.rateLimitConfig` 放限流配置，需迁移到 `flowConfig.rateLimitConfig`
- 如果测试在 `flowConfig.rateLimitConfig` 放，则当前测试可能一直用默认值（1000），限流未真正测到

### 5.2 Java 单元测试

- `RateLimitConfigReaderTest`：需更新 mock 数据，限流配置从 trigger.data 移到 flowConfig
- `FlowInvokeServiceTest`：如有涉及 validateRateLimitConfig 的测试需同步

---

## 6. 修复优先级

| 优先级 | 修复项 | 说明 |
|--------|--------|------|
| 🔴 高 | RateLimitConfigReader 读取路径改正确 | 核心功能，当前用户配置不生效 |
| 🔴 高 | FlowInvokeService.validateRateLimitConfig 改正确 | 运行时校验位置一致 |
| 🟠 中 | 统一上限（10000 → 1000） | 与规范一致 |
| 🟠 中 | 修正注释 | 避免误导 |
| 🟡 低 | 限流配置缓存 | 性能优化 |
| 🟡 低 | 测试适配 | 确保 E2E 测试覆盖正确位置 |
