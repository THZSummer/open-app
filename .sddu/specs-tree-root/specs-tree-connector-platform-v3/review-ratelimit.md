# 限流配置审查

> 审查日期：2026-06-29
> 审查依据：`plan-json-schema.md` §6.3.3 flowConfig — 流级配置、§4.3.3 rateLimitConfigDef
> 审查范围：connector-api 运行时限流、open-server 设计态校验

---

## 1. 限流配置模型

### 1.1 三种配置

| # | 配置 | 作用域 | 存储位置 | 说明 |
|---|------|--------|---------|------|
| ① | 平台全局最大限流值 | 平台统一 | `openplatform_property_t`（`path=connector_platform`） | 平台硬上限，所有应用共享，如 `max_qps=1000` |
| ② | 应用下最大限流值 | 按应用区分 | `openplatform_property_t`（`path=connector_platform_app_{appId}`） | 特殊应用诉求，覆盖平台默认值，如某应用上限 `max_qps=500` |
| ③ | 连接流版本实际配置的限流值 | 单个连接流 | `flow_version_t.orchestration_config` JSON 的 `flowConfig.rateLimitConfig` | 可选，用户按需配置 |

### 1.2 两种场景

**场景一：设计态校验（发布时）**

```
连接流版本配置值 ③ <= max(平台全局 ①, 应用级 ②)
```

- 读取 ① 和 ②，取两者中较大的作为该应用的实际上限（应用级 ② 覆盖平台 ①）
- 校验 ③ 不超过该上限，超过则拒绝发布
- ③ 为空（未配置）则不校验，发布通过

**场景二：运行态执行（每次请求）**

```
实际生效限流值 = ③ 存在 ? ③ : ①
```

- ③ 存在 → 实际流量受 ③ 约束（`actualQps <= maxQps_③`）
- ③ 不存在 → 实际流量受 ① 约束（`actualQps <= maxQps_①`）
- 运行态不需要读 ②（设计态已保证 ③ <= max(①,②)，运行态只需读 ③ 或回退 ①）

### 1.3 数据流示意

```
                        ┌─────────────────────────────────┐
                        │  openplatform_property_t        │
                        │  ① path=connector_platform      │
                        │     max_qps=1000 (平台全局)      │
                        │  ② path=connector_platform_app_5│
                        │     max_qps=500 (应用5特殊)      │
                        └────────┬────────────────────────┘
                                 │
                    设计态（发布时）│ 取 max(①,②) 作为上限
                                 │
    ┌────────────────────────────┼────────────────────────────┐
    │  flow_version_t.orchestration_config                    │
    │  ③ flowConfig.rateLimitConfig.maxQps=300                │
    │  校验：300 <= max(1000,500)=1000 ✅ 通过                │
    └────────────────────────────┬────────────────────────────┘
                                 │
                    运行态（每次请求）│ 读 ③，不存在则回退 ①
                                 │
                                 ▼
                    实际限流：300（③存在，用③）
                    若 ③ 不存在：1000（回退①）
```

---

## 2. 当前代码问题

### 2.1 读取位置错误（connector-api）

**问题**：运行态限流器 `RateLimitConfigReader` 从 `triggerNode.data.rateLimitConfig` 读取配置 ③，而规范要求从 `flowConfig.rateLimitConfig` 读取。

| 代码位置 | 当前读取路径 | 规范要求 | 状态 |
|---------|------------|---------|:----:|
| `RateLimitConfigReader.extractRateLimitConfig()` | `triggerNode.data.rateLimitConfig` | `flowConfig.rateLimitConfig` | ❌ |
| `FlowInvokeService.validateRateLimitConfig()` | `nodeData.rateLimitConfig`（trigger 的 data）| `flowConfig.rateLimitConfig` | ❌ |
| `FlowPublishValidator.validateRateLimit()` (open-server) | `flowConfig.rateLimitConfig` | `flowConfig.rateLimitConfig` | ✅ |
| `FlowPublishValidator.validateRateLimitAgainstAppMax()` (open-server) | `flowConfig.rateLimitConfig` | `flowConfig.rateLimitConfig` | ✅ |

**后果**：用户在 `flowConfig.rateLimitConfig` 配的限流值不生效，运行态读不到，使用默认值 1000。

### 2.2 运行态兜底值用错配置

**问题**：配置 ③ 不存在时，运行态应回退到 ①（平台全局值），但当前代码回退到硬编码 `DEFAULT_QPS = 1000`。

| 代码位置 | 当前回退值 | 应回退到 | 状态 |
|---------|----------|---------|:----:|
| `RateLimitConfigReader` | 硬编码 `DEFAULT_QPS = 1000` | 读 ① 平台全局 `max_qps` | ❌ |

**后果**：如果平台全局上限改为 500，运行态仍用 1000 作为回退值，与平台策略不一致。

### 2.3 设计态未取 max(①,②)

**问题**：设计态校验 `validateRateLimitAgainstAppMax()` 传入的是应用级上限 ②，未与平台全局 ① 取 max。

| 代码位置 | 当前逻辑 | 应改为 | 状态 |
|---------|---------|-------|:----:|
| `FlowVersionService.publish()` | 传入 `appMaxQps`（仅 ②）| 传入 `max(①, ②)` | ❌ |

**后果**：如果平台全局 ①=1000，应用 ② 未配置（null），则 `max(1000, null)` 应为 1000。但如果代码只传 ② 且 ② 为 null/0，可能导致校验逻辑异常或跳过校验。

### 2.4 运行态不需要 Math.min 截断

**问题**：`RateLimitConfigReader` 做了 `Math.min(配置值③, APP_MAX_QPS=1000)` 静默截断。按配置模型，运行态不需要截断——设计态已保证 ③ <= max(①,②)，运行态直接用 ③ 即可。

| 代码位置 | 当前逻辑 | 应改为 | 状态 |
|---------|---------|-------|:----:|
| `RateLimitConfigReader` | `Math.min(③, 1000)` 静默截断 | 直接用 ③（设计态已校验）| ❌ |

**后果**：如果平台全局 ①=2000，应用 ② 未配置，用户配 ③=1500 通过设计态校验，但运行态 `Math.min(1500, 1000)=1000` 错误截断。

### 2.5 上限不一致

| 代码位置 | 上限值 | 说明 |
|---------|-------|------|
| `RateLimitConfigReader.APP_MAX_QPS` | 1000 | 运行态截断 |
| `FlowInvokeService.validateRateLimitConfig` | 10000 | 运行态校验拒绝 |
| `FlowPublishValidator` | `appMaxQps`（动态） | 设计态校验 |

同一运行态两处上限不一致（1000 vs 10000），且与设计态的动态上限脱节。

### 2.6 其他问题

| # | 问题 | 文件 | 说明 |
|---|------|------|------|
| 1 | 注释错误 | `FlowInvokeController.java:37` | `data.rateLimitConfig` → `flowConfig.rateLimitConfig` |
| 2 | 注释过时 | `FlowVersionEntity.java:21` | 提到 rateLimitConfig 在 data 下 |
| 3 | 已解析未使用 | `FlowConfigParser.java:46-52` | 已正确从 flowConfig 解析但运行时未消费 |
| 4 | 无缓存 | `RateLimitConfigReader` | 每次请求查 DB |

---

## 3. 推荐解决方案

### 3.1 运行态：修正读取位置 + 回退到平台全局值

**文件**：`connector-api/.../ratelimit/RateLimitConfigReader.java`

修改 `extractRateLimitConfig()`：
1. 读取路径改为 `flowConfig.rateLimitConfig`（配置 ③）
2. ③ 不存在时，回退到平台全局值 ①（而非硬编码 1000）
3. 去掉 `Math.min` 截断（设计态已校验）

```java
public Mono<RateLimitConfig> readFlowRateLimit(Long flowId) {
    // 先读平台全局值 ① 作为回退
    return platformPropertyService.getPlatformMaxQps()
            .defaultIfEmpty(DEFAULT_QPS)
            .flatMap(platformMaxQps ->
                flowVersionReadRepository.findByFlowId(flowId)
                    .map(entity -> extractRateLimitConfig(entity, platformMaxQps))
                    .defaultIfEmpty(new RateLimitConfig("qps", platformMaxQps, platformMaxConcurrency))
                    .onErrorResume(...)
            );
}

private RateLimitConfig extractRateLimitConfig(FlowVersionEntity entity, int fallbackQps) {
    Map<String, Object> config = entity.parseOrchestrationConfigAsMap(objectMapper);
    Map<String, Object> flowConfig = (Map<String, Object>) config.get("flowConfig");
    if (flowConfig == null) {
        return new RateLimitConfig("qps", fallbackQps, fallbackConcurrency);
    }
    Map<String, Object> rateLimitConfig = (Map<String, Object>) flowConfig.get("rateLimitConfig");
    if (rateLimitConfig == null) {
        // ③ 不存在，回退到 ①
        return new RateLimitConfig("qps", fallbackQps, fallbackConcurrency);
    }
    // ③ 存在，直接用（不做 Math.min 截断，设计态已校验）
    int maxQps = fallbackQps;
    Object maxQpsObj = rateLimitConfig.get("maxQps");
    if (maxQpsObj instanceof Number) {
        maxQps = ((Number) maxQpsObj).intValue();
    }
    // ... maxConcurrency 同理
    return new RateLimitConfig(mode, maxQps, maxConcurrency);
}
```

> 注意：connector-api 目前不读 `openplatform_property_t`（该表由 open-server 的 `ConnectorPlatformPropertyService` 读取）。方案一：connector-api 增加读取平台属性的能力；方案二：回退值仍用硬编码，但需与平台全局值保持同步（通过配置文件或启动时加载）。

### 3.2 设计态：取 max(①, ②)

**文件**：`open-server/.../flowversion/service/FlowVersionService.java`

修改 `publish()` 中调用 `validateRateLimitAgainstAppMax` 前的逻辑：
1. 读取平台全局 ①
2. 读取应用级 ②
3. 取 `max(①, ②)` 作为校验上限

```java
int platformMaxQps = propertyService.getPlatformMaxQps();
int platformMaxConcurrency = propertyService.getPlatformMaxConcurrency();
int appMaxQps = propertyService.getAppMaxQps(appId);
int appMaxConcurrency = propertyService.getAppMaxConcurrency(appId);
int effectiveMaxQps = Math.max(platformMaxQps, appMaxQps);
int effectiveMaxConcurrency = Math.max(platformMaxConcurrency, appMaxConcurrency);
List<String> errors = publishValidator.validateRateLimitAgainstAppMax(
    config, effectiveMaxQps, effectiveMaxConcurrency);
```

### 3.3 运行态：修正 FlowInvokeService 校验位置

**文件**：`connector-api/.../flow/service/FlowInvokeService.java`

`validateRateLimitConfig` 改为从 `flowConfig.rateLimitConfig` 读取，调用处传入 `config` 而非 `nodeData`。

### 3.4 修正注释

| 文件 | 改动 |
|------|------|
| `FlowInvokeController.java:37` | `data.rateLimitConfig` → `flowConfig.rateLimitConfig` |
| `FlowVersionEntity.java:21` | 更新编排配置结构注释 |

### 3.5 可选优化

| 优化项 | 说明 |
|--------|------|
| 限流配置缓存 | `RateLimitConfigReader` 复用 `EntityCacheManager` 缓存，避免每次请求查 DB |
| 统一上限常量 | 删除 `RateLimitConfigReader.APP_MAX_QPS` 和 `FlowInvokeService` 的 10000，运行态不再做截断 |

---

## 4. 涉及文件清单

| 模块 | 文件 | 改动类型 |
|------|------|---------|
| connector-api | `RateLimitConfigReader.java` | 读取路径 + 回退逻辑 + 去截断 |
| connector-api | `FlowInvokeService.java` | 校验位置 + 调用处 |
| connector-api | `FlowInvokeController.java` | 注释修正 |
| connector-api | `FlowVersionEntity.java` | 注释修正 |
| open-server | `FlowVersionService.java` | 设计态取 max(①,②) |
| connector-api | `RateLimitConfigReaderTest.java` | 测试适配 |
| connector-api | `FlowInvokeServiceTest.java` | 测试适配 |
| open-server | `FlowPublishValidatorTest.java` | 测试适配 |

---

## 5. 修复优先级

| 优先级 | 修复项 | 说明 |
|--------|--------|------|
| 🔴 高 | RateLimitConfigReader 读取路径改为 flowConfig | 核心功能，当前用户配置不生效 |
| 🔴 高 | FlowInvokeService.validateRateLimitConfig 改正确 | 运行时校验位置一致 |
| 🟠 中 | 运行态回退到平台全局值 ① | 当前硬编码 1000 与平台策略脱节 |
| 🟠 中 | 设计态取 max(①,②) | 确保应用级覆盖平台级 |
| 🟠 中 | 去掉运行态 Math.min 截断 | 设计态已校验，运行态无需重复截断 |
| 🟠 中 | 修正注释 | 避免误导 |
| 🟡 低 | 限流配置缓存 | 性能优化 |
| 🟡 低 | 测试适配 | 确保覆盖正确位置 |
