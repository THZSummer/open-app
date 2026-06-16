# 运行时引擎设计：连接器平台 V2

**Feature ID**: CONN-PLAT-002  
**关联文档**: plan.md（§4.2 connector-api 模块），plan-db.md（§3 表结构），plan-json-schema.md（JSON 结构定义）  
**版本**: v1.0  
**创建日期**: 2026-06-09  
**对齐基线**: spec.md v2.24-draft，ADR-005（限流），ADR-006（运行记录）

---

## 0. 概述

V2 运行时在 V1 的串行调度引擎基础上新增 6 个核心模块：

| 模块 | 触发条件 | 关键能力 |
|------|---------|---------|
| 版本配置解析器 | 每次 HTTP/调试 触发 | 按 `deployed_version_id` 读取 FlowVersion → ConnectorVersion |
| 并行分支执行器 | 编排中包含并行边 | Reactor `Flux.merge()` 并发执行，独立超时 + 错误汇聚 |
| flowConfig 解析器 | 版本配置加载后 | 解析超时/限流/缓存配置，初始化运行环境 |
| 数据处理节点执行器 | 编排中包含 data_processor 节点 | 字段类型转换（toString/toNumber/toBoolean/formatDate），递归值解析 |
| 入站限流拦截器 | HTTP 触发请求到达 | Redis 令牌桶（QPS）或并发计数器（Concurrency） |
| 认证注入器扩展 | 连接器 HTTP 调用 | Cookie/DigitalSign/MultiAuth 注入器注册到现有 Strategy 模式 |

---

## 1. 版本配置解析器（VersionConfigResolver）

### 1.1 设计

```
HTTP Request → TriggerHandler
    → VersionConfigResolver.resolve(flowId)
        → FlowEntity.findById(flowId)                        # 获取 deployed_version_id
        → FlowVersionEntity.findById(deployedVersionId)       # 读取编排快照
        → 解析 nodes[] → 对每个 connector 节点:
            → ConnectorVersionEntity.findById(connectorVersionId)  # 读取连接配置快照
        → 返回 ResolvedConfig { flowVersion, connectorVersionMap }
    → FlowRuntimeEngine.execute(resolvedConfig, triggerData)
```

### 1.2 错误处理

| 场景 | 处理 |
|------|------|
| `deployed_version_id` 为 NULL | 返回 503 "Flow not deployed" |
| FlowVersion 不存在或已删除 | 返回 500 "Deployed version not found" |
| ConnectorVersion 不存在或已失效 | 标记对应节点为失败，继续执行其余节点（降级） |

### 1.3 缓存策略

- FlowVersion + ConnectorVersion 配置读后写入 Redis 缓存
- Key: `cp:config:flow:{flowId}:{versionId}`, TTL: 5 分钟
- 版本切换时主动失效对应缓存

---

## 2. 并行分支执行器（ParallelBranchExecutor）

### 2.1 分支识别

编排保存时，从 FlowEdge 中识别并行边：
- `edge.data.connectionMode = "parallel"` → 该边的目标节点可与其他同源并行边并发执行
- `edge.data.connectionMode = "serial"`（默认）→ 串行执行

### 2.2 执行模型

```java
public Mono<ParallelResult> execute(NodeContext ctx, List<Branch> branches) {
    return Flux.fromIterable(branches)
        .flatMap(branch -> executeBranch(branch, ctx)
            .timeout(branch.getTimeout())           // 每个分支独立超时
            .onErrorResume(e -> Mono.just(branchFailed(branch, e)))  // 错误不中断其他分支
        )
        .collectList()
        .map(results -> mergeToParallelResult(results));  // 汇聚所有分支结果
}
```

### 2.3 关键约束

| 约束 | 说明 |
|------|------|
| 并行分支数上限 | 20（硬限制，防止线程池耗尽） |
| 分支独立超时 | 每分支取 `min(节点超时配置, 30s)` |
| 错误不扩散 | 一个分支失败不影响其他分支执行 |
| 汇聚等待 | 所有分支完成后（成功或失败）才进入下游节点 |
| 结果合并 | 各分支输出按 `nodeId` 组织：`{ nodeId: { output: {...}, status: "success|failed" } }` |

---

## 3. flowConfig 解析器（FlowConfigParser）

### 3.1 flowConfig 结构

```json
{
  "timeout": {
    "perNode": 10,       // 每节点默认超时（秒），0=不限
    "global": 60          // 全流总超时（秒）
  },
  "rateLimit": {
    "mode": "QPS",        // QPS | CONCURRENCY
    "value": 100           // 上限值，0=关闭
  },
  "cache": {
    "enabled": true,
    "keyTemplate": "${$.node.trigger.input.userId}",  // 缓存键表达式
    "ttl": 300            // 缓存 TTL（秒），0=永不过期
  }
}
```

### 3.2 解析时机

- HTTP 触发请求到达时，从 `FlowVersion.orchestrationConfig.flowConfig` JSON 解析
- 解析结果注入 `ExecutionContext`，供后续节点获取
- 解析失败 → 使用默认值（无超时/无限流/无缓存），记录告警日志

### 3.2a 节点超时上限控制

运行时单节点超时 = **min(节点配置值, 应用最大超时值)**。应用最大超时值从系统配置读取（默认 5s，平台管理员可按应用覆盖）。节点配置值超过应用最大超时值时，以应用最大超时值为准，确保平台管理员可控制全局超时上限。

---

## 4. 数据处理节点执行器（DataProcessorExecutor）

### 4.1 节点配置结构

```json
{
  "outputFields": [
    {
      "name": "userId",
      "type": "string",
      "sourceType": "function",
      "function": {
        "name": "toString",
        "args": [
          { "sourceType": "reference", "value": "${$.node.trigger.input.userId}" }
        ]
      }
    },
    {
      "name": "count",
      "type": "number",
      "sourceType": "function",
      "function": {
        "name": "toNumber",
        "args": [
          { "sourceType": "reference", "value": "${$.node.connector_1.output.count}" }
        ]
      }
    }
  ]
}
```

### 4.2 值来源解析（递归）

```java
private Object resolveValue(ValueSource source, NodeContext ctx) {
    return switch (source.getSourceType()) {
        case "constant"  -> source.getValue();         // 静态值
        case "reference" -> ctx.resolvePath(source.getValue());  // 引用路径
        case "function"  -> {
            var func = converters.get(source.getFunction().getName());
            var args = source.getFunction().getArgs().stream()
                .map(arg -> resolveValue(arg, ctx))   // 递归解析入参
                .toArray();
            yield func.apply(args);
        }
    };
}
```

### 4.3 本期支持的转换函数

| 函数名 | 输入 | 输出 | 失败处理 |
|--------|------|------|---------|
| `toString` | any | string | 原始值 → `String.valueOf()` |
| `toNumber` | string/number | number | 非数字 → 标记失败，保留原始值 |
| `toBoolean` | string/number | boolean | "true"/1/true → true, 其余 → false |
| `formatDate` | string/number(ms) | string | 非法日期 → 标记失败，保留原始值 |

### 4.4 错误处理

- 任一字段转换失败 → 节点标记为 "failed"
- 成功转换的字段正常输出
- 失败字段保留原始值 + 错误信息（EC-013）

---

## 5. 入站限流拦截器（InboundRateLimiter）

> 详见 ADR-005

### 5.1 WebFilter 拦截链

```
HTTP Request → InboundRateLimiter WebFilter (order=-100)
    → Redis Lua eval (令牌桶检查)
        → 允许 → chain.filter(exchange)
        → 拒绝 → 返回 429 "Too Many Requests" + "Retry-After" header
        → Redis 不可用 → 降级放行
```

### 5.2 Redis Lua 脚本

```lua
-- key: cp:ratelimit:qps:{flowId}:{second}
-- ARGV[1]: maxTokens
-- ARGV[2]: currentSecond (用于清理过期 key)
local current = redis.call('GET', KEYS[1])
if current == false then
    redis.call('SET', KEYS[1], ARGV[1], 'EX', 2)
    return 1
elseif tonumber(current) > 0 then
    redis.call('DECR', KEYS[1])
    return 1
else
    return 0
end
```

### 5.3 并发模式

- Key: `cp:ratelimit:concurrency:{flowId}`
- 请求到达 → `INCR`，超 `maxConcurrency` → `DECR` + 返回 429
- 请求完成 → `DECR`
- 为防止死锁（进程崩溃），key 设置 TTL = 300s

---

## 6. 缓存管理（FlowCacheManager）

### 6.1 缓存模式

```
触发请求 → 解析 cacheConfig → 计算缓存键 → Redis GET
    → 命中 → 直接返回缓存结果，跳过 DAG 执行
    → 未命中 → 正常执行 DAG → Redis SET(key, result, TTL) → 返回结果
```

### 6.2 缓存键生成

- 模板：`${$.node.trigger.input.userId}`
- 运行时解析为: `cp:flow:{flowId}:cache:u001`
- 多个键用 `:` 拼接

### 6.3 缓存失效

| 触发条件 | 失效范围 |
|---------|---------|
| FlowVersion 发布 | 清空该 Flow 所有缓存 |
| FlowVersion 标记失效 | 清空该版本缓存 |
| Flow 部署新版本 | 清空该 Flow 所有缓存 |
| Flow 停止 | 清空该 Flow 所有缓存 |
| TTL 自然过期 | 自动失效 |

---

## 7. 日志采集（ExecutionRecordService + ExecutionStepService）

> 详见 ADR-006

### 7.1 写入模型

```
请求完成（成功/失败/超时）
    → 检查应用级日志采集开关：
        - 开启（默认）→ 正常写入 execution_record_t + execution_step_t
        - 关闭 → 仅写入 execution_record_t（基础信息），不写 execution_step_t
    → 写入失败 → 仅记录错误日志，不影响业务
```

### 7.2 敏感信息脱敏

```java
private Map<String, Object> sanitize(Map<String, Object> data) {
    // 移除 marked sensitive 的字段（如 password, token, secretKey）
    return data.entrySet().stream()
        .filter(e -> !SENSITIVE_FIELDS.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}
```

### 7.3 定时清理

- 每天 03:00 执行
- 删除 `trigger_time < NOW() - INTERVAL 30 DAY` 的记录
- 分批删除（每批 1000 条），避免长事务

### 7.3a FIFO 条数清理

每次 `execution_record` 写入后检查该 `flow_id` 的记录数是否超限（默认 1000，按应用可配），超限时按 `create_time ASC` 批量 DELETE 最早的多余记录。与 30 天定时清理互补。

---

## 8. 调试执行器（DebugExecutionService）

### 8.1 执行模式

```
POST /api/v1/debug/execute
Body: { flowId, versionId, triggerData }

→ 同步执行（阻塞等待完整结果）
→ 独立线程池（max 5 线程，防止调试占用正常运行资源）
→ 超时：30s（独立于正常运行超时）
→ 返回：各节点执行状态 + 输入输出数据 + 耗时
→ 运行记录写入（trigger_type=2 debug）
```

### 8.2 约束

| 约束 | 说明 |
|------|------|
| 版本限制 | 仅草稿和已发布版本可调试；已失效版本拒绝（EC-014） |
| 并发限制 | 同一用户最多 3 个并发调试请求 |
| 运行记录 | 调试执行生成运行记录，不计入正常运行指标 |
| 不影响运行中 Flow | 调试使用版本快照，不绑定 deployed_version_id |

---

## 9. 认证注入器扩展

### 9.1 注入器注册

V1 已有 `CredentialInjectorRegistry`（Strategy 模式），通过 Spring Bean 自动发现。V2 新增 3 个注入器：

```java
@Component
public class CookieCredentialInjector implements CredentialInjector {
    // 从 connector_version.connectionConfig.authConfig 读取 cookie name
    // 运行时从 inputMapping 读取对应 cookie 值
    @Override
    public boolean supports(AuthType type) { return type == AuthType.COOKIE; }
}

@Component
public class DigitalSignCredentialInjector implements CredentialInjector {
    // 读取 secretKey（加密存储）+ 签名算法（HMAC-SHA256）
    // 根据 carrier（header/query）注入签名
    @Override
    public boolean supports(AuthType type) { return type == AuthType.DIGITAL_SIGN; }
}

@Component
public class MultiAuthCredentialInjector implements CredentialInjector {
    // 按 authConfig.fields 排序，依次调用各单一注入器
    // 任意一个注入失败 → 节点执行失败
    @Override
    public boolean supports(AuthType type) { return type == AuthType.MULTI; }
}
```

### 9.2 认证类型枚举扩展

```java
public enum AuthType {
    SOA(1),
    APIG(2),
    NONE(4),
    AKSK(5),
    SYSTOKEN(7),
    COOKIE(8),       // V2 新增
    DIGITAL_SIGN(9),  // V2 新增
    MULTI(10);        // V2 新增（多选组合）
}
```

---

## 10. SYSTOKEN 白名单校验器（SystokenWhitelistValidator）

### 10.1 校验流程

```
HTTP 触发请求 → 提取 SYSTOKEN 凭证
    → SystokenWhitelistValidator.validate(flowVersion, systoken)
        → 从 triggerData.authConfig 读取白名单列表
        → 白名单为空 → 返回 401 "No SYSTOKEN whitelisted"
        → systoken 不在白名单 → 返回 401 "SYSTOKEN not in whitelist"
        → 通过 → 继续执行
```

### 10.2 白名单配置位置

白名单存储在 `FlowVersion.orchestrationConfig.trigger.authConfig.systokenWhitelist` 中：

```json
{
  "trigger": {
    "type": "http",
    "authConfig": {
      "type": "SYSTOKEN",
      "systokenWhitelist": ["token_abc123", "token_xyz789"]
    }
  }
}
```

---

## 11. URL 白名单校验器（UrlWhitelistValidator）

### 11.1 校验流程

```
连接器 HTTP 调用 → 构造实际请求 URL
    → UrlWhitelistValidator.validate(connectorId, actualUrl)
        → 查询 connector_url_whitelist_t（is_deleted=0）
        → 空白名单 → 允许（不限制）
        → 逐条匹配正则：
            → 命中任一 → 允许
            → 全部不命中 → 拒绝，返回 403 "URL not in whitelist: {actualUrl}"
```

### 11.2 正则编译与缓存

```java
@Component
public class UrlWhitelistValidator {
    private final LoadingCache<Long, List<Pattern>> patternCache = 
        Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(connectorId -> loadPatterns(connectorId));
    
    public void validate(Long connectorId, String actualUrl) {
        List<Pattern> patterns = patternCache.get(connectorId);
        if (patterns.isEmpty()) return;  // 空白名单 = 不限制
        boolean matched = patterns.stream().anyMatch(p -> p.matcher(actualUrl).matches());
        if (!matched) {
            throw new UrlNotWhitelistedException(connectorId, actualUrl);
        }
    }
}
```
