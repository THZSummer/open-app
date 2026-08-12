# Bugfix 分析：连接流部署/停止接口慢 — 执行结果缓存清理 SCAN 遍历成本爆炸

**Feature ID**: CONN-PLAT-003
**关联文档**: plan-cache.md（§2 缓存架构、§3.3.4 TTL 模型）, spec.md（§3.7 FR-037）
**关联代码**: `FlowCacheEvictor.java`（open-server）, `FlowCacheManager.java`（connector-api）, `EntityCacheManager.java`（connector-api）
**版本**: v1.0-draft
**创建日期**: 2026-08-10
**状态**: 🔵 分析完成，方案待决策（A/B/C/D/E 候选，推荐 D+A）
**修复分支**: `perf/flow-cache-evict-scan`

---

## 1. 问题背景

### 1.1 现象

标准环境部署后，**连接流的部署、停止、失效、删除等涉及 SCAN 方式清理缓存的接口响应特别慢**。

调用链排查发现：`RedisScan` 命令被执行了**很多次**（数千次量级）。标准环境 Redis 中约存在 **50 万条数据**，怀疑与 SCAN 遍历全量数据有关。

### 1.2 触发链路

```
部署/停止/失效/删除流（open-server）
  → FlowCacheEvictor.evictExecutionResults(flowId)     # open-server 同步调用
      → redis.scan(ScanOptions{match="cp:cache:flow:{flowId}:*", count=100})
          # SCAN 游标分批迭代（count=100 为每批 hint），
          # 50w 数据下需 5,000~10,000 次 SCAN 命令往返 —— 即排查中观测到的"RedisScan 执行了很多次"
      → 收集完所有 key 后一次性 redis.delete(keysToDelete)   # 删除不分批
```

> 💡 说明：`c212b97f` 已把最初的 `redis.keys()` 改为 `redis.scan()` 游标迭代，即**当前代码已经是"分批 scan"**（每批 count=100 hint）。但 SCAN 的 COUNT 只是 hint 而非限制——要确认没有遗漏，游标必须遍历完整个哈希表（50w 数据 → 数千批），这正是慢的根源。文档 §2.1 有完整推导。

涉及该方法的所有生命周期操作：

| 操作 | 调用点 | 清理动作 |
|------|--------|---------|
| 部署 | `FlowDeployService.deployVersion()` L78-80 | evictFlowConfig + evictFlowEntity + **evictExecutionResults** |
| 停止 | `FlowService.stopFlow()` L363-364 | evictFlowEntity + **evictExecutionResults** |
| 失效 | `FlowService.invalidateFlow()` L402-403 | evictFlowEntity + **evictExecutionResults** |
| 删除 | `FlowService.deleteFlow()` L468-470 | evictFlowConfig + evictFlowEntity + **evictExecutionResults** |

### 1.3 相关背景（git 历史）

| 提交 | 内容 | 与本问题的关系 |
|------|------|---------------|
| `c212b97f` | open-server `FlowCacheEvictor.evictExecutionResults` 从 `redis.keys()` 改为 `redis.scan()` 游标遍历 | 消除了 KEYS 全库阻塞，但 **SCAN 仍是 O(N) 全量遍历**，50w 数据量下同样慢 |
| `924228a7`（perf 分支） | 删除 connector-api `FlowCacheManager.invalidateFlowCache()` 僵尸方法（KEYS + DEL，生产无调用点） | 已清理 KEYS 僵尸代码，connector-api 不再持有 KEYS 隐患 |

---

## 2. 分析结论

### 2.1 根因：SCAN 的遍历成本与**总 key 数**成正比，与匹配数无关

SCAN 的耗时取决于 Redis 哈希表大小（总 key 数），**不是**匹配到的 key 数。即使某个 flowId 只有几个执行结果缓存需要清理，SCAN 也必须完整遍历哈希表（游标归零）才能确认没有遗漏。

| 项目 | 数值 | 说明 |
|------|------|------|
| Redis 总 key 数 | ~50 万 | 标准环境实测 |
| Redis 哈希表槽位数 | 2^19 = 524,288 | Redis 哈希表恒为 2 的幂，50w key 负载因子 ~1 时扩容到此 |
| `count(100)` | 每次迭代遍历约 100 槽 | **COUNT 只是 hint**，决定每次迭代处理的哈希桶数，非返回 key 数 |
| 总迭代次数 | ≈ 5,000 ~ 10,000 次 | 524,288 ÷ 100，必须完整遍历才能结束 |
| 每次迭代 | 1 次 SCAN 命令 + 1 次同步网络往返 | Lettuce 同步连接，阻塞等待 |
| 总耗时 | 跨机 RTT 0.5~2ms × 5000~10000 ≈ **2.5~20 秒** | 与观察到的接口慢现象吻合 |

### 2.2 叠加根因：执行结果缓存 TTL 过长 → key 持续膨胀

`FlowCacheManager.writeCache()` 中，用户未配置 `ttlSeconds` 时回退平台全局默认 **1296000 秒（15 天）**。高流量流的 cacheKey 高基数，key 无限累积，持续推高 SCAN 的遍历成本。

### 2.3 隐藏问题：Redis Cluster 下 SCAN 只扫一个节点

源码验证（spring-data-redis 3.5.x）：

- `LettuceClusterConnection` **未重写** `scan(ScanOptions)`，直接继承 `LettuceConnection` 的实现
- 继承实现走 `getAsyncConnection()` → `StatefulRedisClusterConnection.async()` 的 `scan()` —— **Lettuce 集群连接的 SCAN 默认只作用于当前绑定的单个节点（default node）**，不会自动遍历全部节点

双重影响：
1. **慢**：单节点上也有 ~8~10w key，SCAN 仍需上千次迭代
2. **清理不完整**：`cp:cache:flow:{flowId}:*` 的 key 按 hash slot 分散在多个节点，只扫一个节点会**漏删其他节点上的 key** → 缓存残留，旧执行结果可能继续被命中

### 2.4 结论

> 是 50w 条数据放大了 SCAN 的 O(N) 遍历成本——代码设计（count=100 的分批 SCAN 游标迭代 + 同步阻塞 + 必须遍历完整哈希表才能结束）在小数据量下无问题，数据规模上来后每次部署/停止都要数千次 SCAN 命令往返，接口慢是必然结果。集群模式下还存在只扫单节点导致的漏删问题。

---

## 3. 解决方案（候选，待决策）

### 方案 A：缩短执行结果缓存 TTL（❌ 已决策：不采纳）

- TTL 优先级（§3.3.4）：**③用户配置 `flowConfig.cache.ttl` > ①平台全局 `Flow.Max.Cache.Ttl.Seconds` > 默认 1296000s**
- TTL 本质是**用户配置项、用户自己控制**——平台调整默认值只影响"未配置的用户"，且不应强制覆盖用户配置
- 调整默认 `1296000s` → 1~24h 可配（对齐 `cp:flow:config` 120s 思路）
- **优点**：控制 key 总量，从源头降低 SCAN 成本；改动仅 1 处配置回退值
- **缺点**：**优先级低**——用户已配置 TTL 的流不受影响，覆盖面有限；缓存命中率下降（业务侧可接受度需评估）
- **定位**：辅助方案，配合主方案（D/C）使用，不作为独立主推方案

### 方案 B：UNLINK 替代 DEL（❌ 已决策：不采纳，方案 D 已内含 UNLINK）

- `redis.unlink(keys)` 替代 `redis.delete(keys)`，Redis 4.0+ 异步释放内存
- **优点**：删除大批 key 时不阻塞主线程
- **缺点**：只解决删除环节，SCAN 遍历本身仍慢（问题主因未解决）

### 方案 C：集群感知 SCAN + 分批删除（❌ 已决策：不采纳）

- 用 `RedisClusterConnection.scan(RedisClusterNode, options)` 遍历所有节点 + 每批 UNLINK
- **优点**：解决漏删；分批删除降低阻塞
- **缺点**：仍是 O(N) 全量遍历，50w 数据下遍历成本仍在；实现复杂度高

### 方案 D：flow 维度 key 索引（结构性替代 SCAN，✅ 已采纳）

写入缓存时同步维护索引，清理时精确删除。**默认按大量成员场景设计**——统一采用 SPOP 批量弹出 + 每批 UNLINK，不做"小成员量用 SMEMBERS 一次性"的分支优化：无论单个 flow 有多少缓存 key 都安全，代码路径单一。

> 📌 索引 key 命名（决策 #2）：**`cp:cache:flow:keys:{{flowId}}`** — 对齐 `cp:cache:flow:{{flowId}}:*` 命名空间（同属 `cp:cache:flow:` 前缀、针对同一 flow），Set 为具体 key 不带 `:*` 通配。**业务 key 与索引 key 均含 `{flowId}` hash tag**，确保集群模式下同 slot（Lua 原子写不触发 CROSSSLOT，见 §5.2 Step 1 注）。

**① 写入缓存时（FlowCacheManager.writeCache）——在现有写缓存逻辑上追加一步索引维护（Lua 原子化，决策 #5）**：

```java
// Lua 脚本原子化：SET 业务数据 + SADD 索引，一次往返保证一致性
// KEYS[1]=redisKey, KEYS[2]="cp:cache:flow:keys:{flowId}", ARGV[1]=json, ARGV[2]=ttl
SET KEYS[1] ARGV[1] EX ARGV[2]
SADD KEYS[2] KEYS[1]

// ⚠️ key 格式必须含 {flowId} hash tag（集群同 slot 约束）:
//   业务 key: cp:cache:flow:{flowId}:{cacheKey}
//   索引 key: cp:cache:flow:keys:{flowId}
//   Redis 对 hash tag 内内容做 CRC16 → 两 key 必同 slot，无 CROSSSLOT
```

**② 清理时（FlowCacheEvictor.evictExecutionResults）——不再 SCAN，SPOP 批量弹出 + 分批 UNLINK**：

```java
// SPOP: 弹出即从索引 Set 移除; null (Redisson key 不存在) 或空 (Lettuce) = Set 已空, 循环终止
List<String> batch;
while ((batch = redis.opsForSet().pop(indexKey, evictBatchSize)) != null && !batch.isEmpty()) {
    redis.unlink(batch);                    // 每批 1000 异步删除
}
redis.delete(indexKey);                     // 最后删索引本身
```

> 🔄 **实现演进记录（v260812-5/v260812-6）**：原方案用 SSCAN 游标分批取索引成员，标准环境暴露 **Redisson 不实现 Spring Data 的 `opsForSet().scan()`**（SSCAN）→ 改用 **SPOP 原生命令**（Lettuce/Redisson 均兼容）。SPOP 弹出即从 Set 移除，天然分批、不重复、无需游标。后续补 NPE 防御（Redisson key 不存在时 `pop()` 返回 null，需判空）。

- **优点**：清理 O(flow 的 key 数)，**完全不依赖全库规模**；精确删除无漏删；默认大量成员设计天然适配存量用户流（已有较多 key 也能安全清理）；SPOP 为原生命令两环境兼容
- **缺点**：写入路径多一次 Redis 往返（可用 pipeline 合并）；**存量 key 无索引**（本方案不考虑存量迁移——存量靠 TTL 自然过期兜底，≤15 天）
- **存量策略**：🟢 **不考虑存量数据**。方案设计面向增量，存量 50w key 无索引，清理时不受影响（SPOP 只操作索引集合，存量 key 不进入索引），靠现有 TTL 自然过期回收。**无需一次性迁移/重建索引/特殊清理**
- **大批量键控**：即便存量用户流已有几十万 key 未入索引，索引集合也只含增量 key；SPOP 每批 1000 弹出 + UNLINK，天然分批不一次性加载，单命令不过大、主线程不阻塞

### 方案 E：清理移出事务 + 按需清理（✅ 已采纳，配套优化）

- 缓存清理通过 **事件驱动 + 异步线程池** 执行：`FlowCacheEvictor` 发布事件 → `FlowCacheEvictListener`（`@Async("cacheEvictExecutor")` + `@TransactionalEventListener(AFTER_COMMIT)`）异步执行清理
- 部署时仅**版本号变化**才清空执行结果缓存（同版本重部署幂等短路，不重复执行 DB 部署与清理）
- **优点**：不阻塞请求线程（独立线程池）；解耦（发布方不关心清理细节）；`AFTER_COMMIT` 保证 DB 事务成功提交后才清理（避免"DB 回滚但缓存已清"）
- **缺点**：与 A/B/C/D 正交，需组合使用

> 🔄 **实现演进记录**：最初用 `TransactionSynchronization.afterCommit`（runAfterCommit）→ 改为 `@TransactionalEventListener(AFTER_COMMIT)`（事件驱动 + 事务提交后执行，当前实现）。曾尝试简化为纯 `@EventListener`（不绑定事务），经权衡**保留 `@TransactionalEventListener`**——`AFTER_COMMIT` 提供"DB 成功才清缓存"的严格语义，且 fallbackExecution 场景通过事件发布点保证（发布在 @Transactional 方法内）。

### 推荐组合（✅ 已决策）

> **D（SADD 索引替代 SCAN，Lua 原子化写入）+ E（事务外清理）**；A（TTL 默认值调整）**不采纳**——保持现有逻辑不变（决策 #3）
>
> - D 从执行路径上移除 SCAN，不受全库数据量影响
> - E 缩短事务持有时间，降低 DB 连接池占用
> - 🟢 **存量 50w 数据：不处理**。靠 TTL 自然过期兜底（≤15 天），无迁移成本

---

## 4. 决策记录（✅ 已全部决策）

| # | 决策点 | 决策 | 说明 |
|---|--------|------|------|
| 1 | 是否采用方案 D（SADD 索引） | ✅ **采纳 D** | 清理路径彻底摆脱 SCAN |
| 2 | 方案 D 索引 key 命名 | ✅ `cp:cache:flow:keys:{flowId}` | 对齐 `cp:cache:flow:{flowId}:*` 命名空间 |
| 3 | 方案 A 默认 TTL 值 | ✅ **不调整，保持现有逻辑不变** | 方案 A 不采纳，TTL 维持现有 ③用户配置 > ①平台全局 1296000 |
| 4 | 存量 50w 数据处理 | ✅ **不处理** | TTL 自然过期（≤15 天）兜底，存量 key 不入索引 |
| 5 | 索引写入原子性 | ✅ **Lua 原子化** | SET + SADD 一次往返，保证一致性 |
| 6 | 是否包含方案 E | ✅ **是** | 清理移出事务 + 按需清理 |
| 7 | 分支实施范围 | ✅ **一次性完成** | 分支 `perf/flow-cache-evict-scan` |

---
## 5. 实施细则（对应 §3 决策，可落地）

> 范围：方案 D（SADD 索引 + Lua 原子化写入 + SPOP 分批清理）+ 方案 E（事件驱动异步清理）。核心实现在分支 `perf/flow-cache-evict-scan` 完成，后续演进（SPOP/NPE）在 main 上补丁完成。

### 5.1 变更文件总览

| # | 文件 | 变更类型 | 内容 |
|---|------|---------|------|
| 1 | `connector-api/src/main/resources/lua/flow_cache_write.lua` | 新增 | Lua 脚本：SET 业务数据 + SADD 索引（原子） |
| 2 | `connector-api/.../modules/cache/FlowCacheManager.java` | 修改 | `writeCache` 改走 Lua 脚本；新增索引 key 常量 |
| 3 | `open-server/.../modules/flow/service/FlowCacheEvictor.java` | 修改 | `evictExecutionResults` 由 SCAN 改为 SPOP 索引清理（含 null 防御） |
| 4 | `open-server/.../modules/flow/service/FlowDeployService.java` | 修改 | 部署清理改事件发布（publishEvent） |
| 5 | `open-server/.../modules/flow/service/FlowService.java` | 修改 | 停止/失效/删除清理改事件发布（publishEvent） |
| 6 | `open-server/.../modules/flow/service/FlowCacheEvictEvent.java` | 新增 | 清理事件（flowId + 清理范围） |
| 7 | `open-server/.../modules/flow/service/FlowCacheEvictListener.java` | 新增 | 异步事件监听器（@Async + @TransactionalEventListener） |
| 8 | `open-server/.../common/config/AsyncConfig.java` | 修改 | 新增 cacheEvictExecutor 线程池 |
| 9 | `connector-api/.../modules/cache/FlowCacheManagerTest.java` | 修改 | 新增 Lua 写入用例（含 hash tag 断言） |
| 10 | `open-server/src/test/.../FlowCacheEvictorTest.java` | 新增 | 新增 SPOP 分批清理用例（含 null 防御） |

### 5.2 实施步骤（按依赖顺序）

#### Step 1：新增 Lua 脚本 `flow_cache_write.lua`（connector-api）

```lua
-- KEYS[1] = 业务数据 key: cp:cache:flow:{flowId}:{cacheKey}
-- KEYS[2] = 索引 key:    cp:cache:flow:keys:{flowId}
-- ARGV[1] = json 值
-- ARGV[2] = ttl（秒）
redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
redis.call('SADD', KEYS[2], KEYS[1])
return 1
```

> ⚠️ 集群约束：KEYS[1] 与 KEYS[2] 必须同 slot —— 靠 **hash tag** 保证：业务 key `cp:cache:flow:{flowId}:{cacheKey}` 与索引 key `cp:cache:flow:keys:{flowId}` 都含 `{flowId}`，Redis 对 hash tag 内内容做 CRC16，两 key 必同 slot，无 CROSSSLOT 风险。~~前缀相同→同 slot~~ 是错误假设（Redis Cluster 对无 hash tag 的 key 是对整个 key 做 CRC16，前缀相同不保证同 slot，review 实测证伪）。

#### Step 2：修改 `FlowCacheManager.writeCache`（connector-api）

```java
// 常量
private static final String INDEX_KEY_PREFIX = "cp:cache:flow:keys:";
private static final RedisScript<Long> WRITE_SCRIPT;   // 参照 InboundRateLimiter 的 DefaultRedisScript 加载 lua/flow_cache_write.lua

// writeCache 核心改动（替换原 opsForValue().set 调用）
String indexKey = INDEX_KEY_PREFIX + flowId;
return reactiveRedisTemplate.execute(
        WRITE_SCRIPT,
        List.of(redisKey, indexKey),
        List.of(json, String.valueOf(effectiveTtl)))
    .then()
    .onErrorResume(e -> Mono.empty());   // Redis 异常不阻断执行（保持现有降级语义）
```

#### Step 3：修改 `FlowCacheEvictor.evictExecutionResults`（open-server）

```java
/** 索引 key 前缀（对齐 cp:cache:flow:{flowId}:* 命名空间） */
private static final String INDEX_KEY_PREFIX = "cp:cache:flow:keys:";
/** SPOP 每批弹出数量（可配置, 默认 1000） */
@Value("${platform.flow-cache.sscan-batch-size:1000}")
private int evictBatchSize = 1000;

public void evictExecutionResults(Long flowId) {
    if (redis == null) return;
    String indexKey = INDEX_KEY_PREFIX + flowId;
    try {
        // 1. SPOP 批量弹出并移除索引成员（弹出即删, 天然分批; 兼容 Lettuce/Redisson）
        List<String> batch;
        while ((batch = redis.opsForSet().pop(indexKey, evictBatchSize)) != null && !batch.isEmpty()) {
            redis.unlink(batch);
        }
        // 2. 删索引本身
        redis.delete(indexKey);
        log.info("Evicted execution result caches for flowId={} via index {}", flowId, indexKey);
    } catch (Exception e) {
        log.warn("Failed to evict execution result caches for flowId={}: {}", flowId, e.getMessage());
    }
}
```

> 🔄 原实现用 SSCAN 游标（`opsForSet().scan`），标准环境 Redisson 不实现该方法 → 改用 **SPOP**（原生命令两环境兼容）。`pop()` 返回 null（Redisson key 不存在）需判空，避免 NPE。

#### Step 4：方案 E — 清理移出事务（open-server 4 个方法，事件驱动 + 异步）

**原则**：Redis 清理从 `@Transactional` 方法体移出，改为发布事件 + `@Async` 异步监听器执行（不阻塞请求线程，不依赖事务时序）。

```java
// FlowDeployService.deployVersion — 发布清理事件（事务方法内调用）
flowMapper.deploy(flowId, versionId, version.getVersionNumber(), now, currentUser);
eventPublisher.publishEvent(new FlowCacheEvictEvent(flowId,
        FlowCacheEvictEvent.SCOPE_FLOW_CONFIG,
        FlowCacheEvictEvent.SCOPE_FLOW_ENTITY,
        FlowCacheEvictEvent.SCOPE_EXECUTION_RESULTS));
```

```java
// FlowCacheEvictListener — 异步事件监听器（事务提交后执行）
@Async("cacheEvictExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onEvict(FlowCacheEvictEvent event) {
    // 按 event 的 scopes 执行 evictFlowConfig / evictFlowEntity / evictExecutionResults
}
```

**涉及方法**（4 处，统一改发布事件）：

| 方法 | 原调用（L 行号） | 改动 |
|------|----------------|------|
| `FlowDeployService.deployVersion` | L78-80 | 3 个 evict → publishEvent |
| `FlowService.stopFlow` | L363-364 | 2 个 evict → publishEvent |
| `FlowService.invalidateFlow` | L402-403 | 2 个 evict → publishEvent |
| `FlowService.deleteFlow` | L468-470 | 3 个 evict → publishEvent |

> 📌 一致性语义：缓存清理通过事件 + `@Async` 异步执行，不阻塞请求线程。缓存是删除操作，事务回滚仅导致缓存缺失 → 下次读 DB 重建，无需等待事务提交（TTL 兜底最终一致）。

#### Step 5：单元测试

- `FlowCacheManagerTest` 新增：
  - `writeCache` 调用 `redisTemplate.execute(script, keys, args)`，断言 script 为 WRITE_SCRIPT、keys 含业务 key + 索引 key（含 `{flowId}` hash tag）、args 含 json + ttl
- `FlowCacheEvictorTest` 新增（mock StringRedisTemplate + SetOperations）：
  - `evictExecutionResults`：mock `opsForSet().pop` 分批返回列表（最后空/null 终止），断言 `unlink` 按每批分批调用、索引 key 最终被 delete、pop 返回 null 不 NPE

#### Step 6：功能/性能/漏删验证

按 §6 执行，脚本清单：`test_cache_evict.py`、`prepare_50w_keys.py`、`bench_deploy_stop.py`、`verify_no_residue.py`

### 5.3 风险与对策

| 风险 | 对策 |
|------|------|
| Lua 脚本跨 slot 异常 | **已修复**：业务/索引 key 引入 `{flowId}` hash tag 强制同 slot（review 实测证伪"前缀相同同 slot"，见 §5.2 Step 1）；单测断言 hash tag 一致 + 集群 EVAL 实测验证 |
| 索引滞后（SADD 失败但 SET 成功） | Lua 原子化已消除写路径竞态；清理侧 SPOP 只操作索引集合，滞后仅导致该次清理少删，TTL 兜底 |
| 存量 50w key 无索引 | 决策 #4：不处理，TTL 自然过期；SPOP 只操作索引集合不受影响 |
| Redisson 不实现 SSCAN | **已修复**（v260812-5）：改用 SPOP 原生命令，Lettuce/Redisson 均兼容 |
| SPOP 返回 null（Redisson key 不存在） | **已修复**（v260812-6）：`pop() != null` 判空防御，避免 NPE |
| 事件监听不触发 | **排查结论**：非注解问题——标准环境曾部署不含事件发布链路的旧版本，导致 `publishEvent` 后无清理；重新部署含事件链路版本（≥ v260812-2）后事件正常触发。当前保留 `@TransactionalEventListener(AFTER_COMMIT)`，事件发布点在 `@Transactional` 方法内保证事务上下文 |
| afterCommit 内异常吞掉 | 保留 try-catch + warn 日志；清理失败不影响接口成功返回 |
| 部署同版本重部署 | 方案 E 含"版本号变化才清理"——需在 afterCommit 回调内比较新旧版本号（deployVersion 内先取旧值再比对） |

### 5.4 验收口径

- 代码评审：变更仅限上表 7 个文件，无越界改动
- 单测全绿；`test_cache_evict.py` 全绿
- 性能 p95 < 100ms；漏删 6 节点 0 残留；SLOWLOG 无 SCAN 慢命令

---
## 6. 验证方案（可落地）

### 6.1 前置条件

| 项目 | 要求 |
|------|------|
| 服务 | open-server(:18080) + connector-api(:18180) 运行中（生产/预发环境可连接 Redis 集群） |
| Redis | 6 节点集群，需**预置 50w 存量 key**（性能验证用） |
| 测试框架 | pytest（已有 `open-server/src/test/python/` 全套） |
| 测试账号 | `common/config.py` 中 TEST_APP_ID / SYSTOKEN / Cookie（沿用现有） |

### 6.2 单元测试（Java，随代码提交）

**文件**：`connector-api/src/test/java/.../modules/cache/FlowCacheManagerTest.java`（新增用例）
`open-server/src/test/java/.../modules/flow/service/FlowCacheEvictorTest.java`（新增）

| 用例 | 验证点 |
|------|--------|
| `writeCache` Lua 脚本 | SET + SADD 一次往返；断言 mock 上 Lua script 被调用、参数含 redisKey + 索引 key |
| `evictExecutionResults` | SSCAN 分批 → UNLINK 每批 1000 → 删索引；mock Cursor 分批返回，断言 UNLINK 分批调用次数 |
| 索引 key 格式 | `cp:cache:flow:keys:{flowId}` 与 `cp:cache:flow:{flowId}:{cacheKey}` 命名空间对齐 |

**执行**：`cd connector-api && mvn test -Dtest=FlowCacheManagerTest && cd ../open-server && mvn test -Dtest=FlowCacheEvictorTest`

### 6.3 功能集成测试（Python，复用现有框架）

**文件**：新增 `open-server/src/test/python/modules/flow/test_cache_evict.py`

```python
# 核心断言链路（复用 conftest 的 deployed_flow fixture + os_redis）
def test_deploy_evicts_index(self, deployed_flow):
    fid, fvid = deployed_flow
    api("POST", f"/flows/{fid}/deploy", {"versionId": fvid})
    # 断言：索引 key 被清理（部署后 cp:cache:flow:keys:{fid} 不存在）
    assert os_redis("EXISTS", f"cp:cache:flow:keys:{fid}") in (0, None)

def test_stop_evicts_index(self, deployed_flow):
    api("POST", f"/flows/{fid}/start")     # 先启动
    api("POST", f"/flows/{fid}/stop")      # 停止 → 触发 evictExecutionResults
    assert os_redis("EXISTS", f"cp:cache:flow:keys:{fid}") in (0, None)
```

**执行**：
```bash
cd open-server/src/test/python
pytest modules/flow/test_cache_evict.py -v
```

### 6.4 性能验证（核心验收，针对 50w 存量数据）

**目的**：证明部署/停止接口耗时从 2.5~20s 降至 <100ms

**步骤**：

1. **预置 50w 存量 key 脚本**：新增 `open-server/src/test/python/scripts/prepare_50w_keys.py`

   ```python
   # 用法: python3 scripts/prepare_50w_keys.py 500000
   # 向集群均匀写入 cp:cache:flow:{随机flowId}:{随机key} 共 N 个（模拟存量），
   # 并用 redis-cli --cluster 或 RedisCluster 客户端写满 6 节点
   ```

2. **基准采集（修复前）**：若分支已合入代码不可回退，则从 git 检出 `main` 版本临时部署，或在预发环境先跑一轮
   ```bash
   python3 scripts/prepare_50w_keys.py 500000        # 预置数据
   python3 scripts/bench_deploy_stop.py --flows 20    # 采集修复前基线
   ```

3. **修复后采集**：
   ```bash
   python3 scripts/bench_deploy_stop.py --flows 20
   ```

4. **bench 脚本**：新增 `open-server/src/test/python/scripts/bench_deploy_stop.py`
   - 对 N 个 flow 循环执行：deploy → start → stop
   - 用 `time.perf_counter()` 记录每个接口耗时
   - 输出：avg / p95 / p99 / max，写入 `open-server/src/test/python/reports/bench_result.json`
   - 判定：p95 < 100ms 且相对基线提升 ≥ 20 倍

**执行要求**：
- 服务需连接**含 50w 存量 key 的 Redis 集群**（与生产同规模）
- 全程监控 Redis 慢日志：`redis-cli -p 6379 SLOWLOG GET 50`，验证无 SCAN 慢命令残留
- 压测期间避免并发业务流量，保证数据干净

### 6.5 漏删验证（集群全节点）

**目的**：证明 6 节点上 `cp:cache:flow:{flowId}:*` 全部清理，无残留

**脚本**：`open-server/src/test/python/scripts/verify_no_residue.py`

```python
# 用法: python3 scripts/verify_no_residue.py <flowId>
# 遍历 6 节点（对每节点执行 SCAN cp:cache:flow:{flowId}:*）
# 断言：所有节点匹配数为 0；且索引 key cp:cache:flow:keys:{flowId} 不存在
# 输出: 每节点匹配数汇总，非 0 即 FAIL
```

**执行步骤**：
1. 创建 flow → 编排 → 部署 → start → 触发 invoke 若干次（产生 `cp:cache:flow:{fid}:*` 执行结果缓存 + 索引）
2. 手动向**多个节点**写入同 flow 的假 key（用 hash tag 或直接 SET 到不同节点）
3. stop → 执行 `verify_no_residue.py <flowId>`
4. 断言全 6 节点 0 残留

**执行要求**：
- 必须在 6 节点集群上执行（单机 Redis 无法验证集群漏删场景）
- 与 §6.4 共用同一预置环境

### 6.6 验收判定标准

| 指标 | 目标 |
|------|------|
| 单元测试 | 全部通过 |
| 功能集成测试 | `test_cache_evict.py` 全绿 |
| 性能 p95 | < 100ms（基线 2.5~20s） |
| 漏删验证 | 6 节点全部 0 残留 |
| Redis 慢日志 | 无 SCAN 慢命令（SLOWLOG 无 evict 相关 >100ms 记录） |

---
## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v3.0-draft | 同步实际实现：方案 D 清理由 SSCAN 改为 SPOP（Redisson 兼容）+ NPE 防御；方案 E 保留 @TransactionalEventListener(AFTER_COMMIT)（纠正此前误记为 @EventListener）；同步 §3/§5/§6 代码示例、变更文件、风险表；修正事件监听排查结论（部署版本问题非注解问题） | 2026-08-12 | SDDU Fast Agent |
| v2.1-draft | review 修复记录：业务/索引 key 引入 `{flowId}` hash tag 修复集群 CROSSSLOT 阻断（原"前缀相同→同 slot"假设实测证伪）；同步更新 §3 方案 D key 格式、§5.2 Step 1 集群约束说明、§5.3 风险对策 | 2026-08-10 | SDDU Fast Agent |
| v2.0-draft | 章节顺序调整：实施细则移到验证方案之前（§4 决策 → §5 实施 → §6 验证），修正编号与交叉引用 | 2026-08-10 | SDDU Fast Agent |
| v1.9-draft | 新增 §6 实施细则：变更文件总览（7 文件）、6 步实施步骤（Lua 脚本→writeCache→evictor→事务外清理→单测→验证）、风险对策表、验收口径 | 2026-08-10 | SDDU Fast Agent |
| v1.8-draft | §5 验证方案细化为可落地执行方案：前置条件、单元测试文件与用例、功能集成测试脚本、50w 数据性能验证（预置脚本+bench 脚本+执行要求）、集群漏删验证脚本、验收判定标准 | 2026-08-10 | SDDU Fast Agent |
| v1.7-draft | 7 项决策全部记录：采纳 D（索引命名 `cp:cache:flow:keys:{flowId}` + Lua 原子化写入）+ E；A/B/C 不采纳（TTL 保持现有逻辑）；存量不处理；一次性实施 | 2026-08-10 | SDDU Fast Agent |
| v1.6-draft | 方案 D 代码进一步精简：写入侧只保留新增索引一行（现有 set 逻辑标注不变），清理侧去除无关方法签名 | 2026-08-10 | SDDU Fast Agent |
| v1.2-draft | 方案 D 默认按大量成员场景设计——统一 SSCAN 分批取 + 每批 UNLINK 作为默认实现，移除 SMEMBERS 一次性方案分支，不做小成员量优化 | 2026-08-10 | SDDU Fast Agent |
| v1.1-draft | 方案定位调整：A 降级为低优先级辅助（TTL 用户配置优先）；方案 D 明确存量策略——不考虑存量数据，TTL 自然过期兜底；推荐组合改为 D+E 为主、A 为辅助；同步更新 §1.2 触发链路为分批 SCAN 准确描述 | 2026-08-10 | SDDU Fast Agent |
| v1.0-draft | 初始创建 — 问题背景、根因分析、5 候选方案、7 决策项 | 2026-08-10 | SDDU Fast Agent |
