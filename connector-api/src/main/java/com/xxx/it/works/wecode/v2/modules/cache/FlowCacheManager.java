package com.xxx.it.works.wecode.v2.modules.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * 连接流执行结果缓存管理器 (connector-api)
 * <p>
 * Phase 5 缓存管理:
 * 在连接流执行完成后将结果写入 Redis 缓存,
 * 后续相同 cacheKey 的请求可直接命中缓存返回。
 * §3.3.4: ③(ttlSeconds)存在用③不截断, ③不存在回退①(平台全局)。
 * </p>
 * <p>
 * 缓存 Key 格式: {@code cp:cache:flow:{flowId}:{cacheKey}}
 * </p>
 */
@Component
public class FlowCacheManager {

    private static final Logger log = LoggerFactory.getLogger(FlowCacheManager.class);

    /** Redis Key 前缀 */
    private static final String CACHE_KEY_PREFIX = "cp:cache:flow:";

    /** 索引 Key 前缀（对齐 cp:cache:flow:{flowId}:* 命名空间, Set 存储该 flow 的缓存 key 名） */
    private static final String INDEX_KEY_PREFIX = "cp:cache:flow:keys:";

    /** SCAN 每批数量 */
    private static final int SCAN_BATCH_SIZE = 100;

    /** 写缓存 Lua 脚本: SET 业务数据 + SADD 索引 (原子化, 一次往返保证一致性) */
    private static final RedisScript<Long> WRITE_SCRIPT;
    static {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/flow_cache_write.lua")));
        script.setResultType(Long.class);
        WRITE_SCRIPT = script;
    }

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ConnectorApiPropertyService propertyService;
    public FlowCacheManager(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                             ObjectMapper objectMapper,
                             ConnectorApiPropertyService propertyService) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
        this.propertyService = propertyService;
    }

    /**
     * 检查缓存是否命中
     * <p>
     * Redis GET 查询, 命中时反序列化并返回 ExecutionResult。
     * 未命中时返回 Mono.empty()。
     * </p>
     *
     * @param flowId   连接流 ID
     * @param cacheKey 缓存 key (已解析)
     * @return Mono&lt;ExecutionResult&gt; (或 Mono.empty())
     */
    public Mono<ExecutionResult> checkCache(Long flowId, String cacheKey) {
        String redisKey = buildCacheKey(flowId, cacheKey);
        return reactiveRedisTemplate.opsForValue().get(redisKey)
                .flatMap(cachedJson -> {
                    try {
                        ExecutionResult result = objectMapper.readValue(cachedJson, ExecutionResult.class);
                        log.debug("Cache hit: flowId={}, cacheKey={}", flowId, cacheKey);
                        return Mono.just(result);
                    } catch (Exception e) {
                        log.warn("Failed to deserialize cached result for flowId={}, cacheKey={}: {}",
                                flowId, cacheKey, e.getMessage());
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.debug("Cache miss: flowId={}, cacheKey={}", flowId, cacheKey)
                ).then(Mono.empty()));
    }

    /**
     * 写入缓存
     * <p>
     * 将执行结果序列化为 JSON 写入 Redis, 并同步维护 flow 维度索引。
     * 通过 Lua 脚本原子化执行 SET(业务数据) + SADD(索引), 一次往返保证一致性。
     * §3.3.4: ③(ttlSeconds)存在用③不截断, ③不存在回退①(平台全局)。
     * </p>
     *
     * @param flowId      连接流 ID
     * @param cacheKey    缓存 key (已解析)
     * @param result      执行结果
     * @param ttlSeconds  缓存 TTL (秒)
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> writeCache(Long flowId, String cacheKey, Object result, int ttlSeconds) {
        String redisKey = buildCacheKey(flowId, cacheKey);
        String indexKey = buildIndexKey(flowId);
        // §3.3.4: ③(ttlSeconds)存在直接用，不截断；③不存在回退①
        // 异步取①平台全局 TTL, 避免在 reactor/lettuce 线程上 block 导致自死锁
        final String json;
        try {
            json = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("Failed to serialize result for cache write: flowId={}, cacheKey={}: {}",
                    flowId, cacheKey, e.getMessage());
            return Mono.empty();
        }

        Mono<Integer> ttlMono = ttlSeconds > 0
                ? Mono.just(ttlSeconds)
                : propertyService.getFlowMaxCacheTtlSeconds()
                        .defaultIfEmpty(1296000)
                        .onErrorReturn(1296000);

        return ttlMono
                .map(t -> t > 0 ? t : 60)
                .flatMap(effectiveTtl ->
                        reactiveRedisTemplate.execute(
                                        WRITE_SCRIPT,
                                        List.of(redisKey, indexKey),
                                        List.of(json, String.valueOf(effectiveTtl)))
                                .doOnNext(v -> log.debug("Cache written: flowId={}, cacheKey={}, ttl={}s",
                                        flowId, cacheKey, effectiveTtl))
                                .doOnError(err -> log.warn("Failed to write cache for flowId={}, cacheKey={}: {}",
                                        flowId, cacheKey, err.getMessage()))
                                .then()
                )
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 构建 Redis 缓存 key
     */
    private String buildCacheKey(Long flowId, String cacheKey) {
        return CACHE_KEY_PREFIX + flowId + ":" + cacheKey;
    }

    /**
     * 构建 flow 维度索引 key (Set: 存储该 flow 的所有缓存 key 名)
     */
    private String buildIndexKey(Long flowId) {
        return INDEX_KEY_PREFIX + flowId;
    }
}
