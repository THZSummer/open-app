package com.xxx.it.works.wecode.v2.modules.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.CacheToggle;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 连接流执行结果缓存管理器 (connector-api)
 * <p>
 * Phase 5 缓存管理:
 * 在连接流执行完成后将结果写入 Redis 缓存,
 * 后续相同 cacheKey 的请求可直接命中缓存返回。
 * </p>
 * <p>
 * 缓存 Key 格式: {@code cp:cache:flow:{flowId}:{cacheKey}}
 * TTL 上限: 1296000 秒 (15天) 对齐 {@code ConnectorPlatformConstants.MAX_CACHE_TTL}
 * </p>
 */
@Component
public class FlowCacheManager {

    private static final Logger log = LoggerFactory.getLogger(FlowCacheManager.class);

    /** Redis Key 前缀 */
    private static final String CACHE_KEY_PREFIX = "cp:cache:flow:";

    /** 最大缓存 TTL: 1296000 秒 = 15 天 */
    static final int MAX_CACHE_TTL = 1296000;

    /** SCAN 每批数量 */
    private static final int SCAN_BATCH_SIZE = 100;

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheToggle cacheToggle;

    public FlowCacheManager(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                             ObjectMapper objectMapper,
                             CacheToggle cacheToggle) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
        this.cacheToggle = cacheToggle;
    }

    /**
     * 检查缓存是否命中
     * <p>
     * Redis GET 查询, 命中时反序列化并返回 ExecutionResult。
     * 未命中时返回 Mono.empty()。
     * 缓存总开关关闭时, 直接返回 Mono.empty()。
     * </p>
     *
     * @param flowId   连接流 ID
     * @param cacheKey 缓存 key (已解析)
     * @return Mono&lt;ExecutionResult&gt; (或 Mono.empty())
     */
    public Mono<ExecutionResult> checkCache(Long flowId, String cacheKey) {
        if (!cacheToggle.isEnabled()) {
            return Mono.empty();
        }

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
     * 将执行结果序列化为 JSON 写入 Redis。
     * TTL 受 {@link #MAX_CACHE_TTL} 上限约束。
     * 缓存总开关关闭时, 不做任何操作。
     * </p>
     *
     * @param flowId      连接流 ID
     * @param cacheKey    缓存 key (已解析)
     * @param result      执行结果
     * @param ttlSeconds  缓存 TTL (秒), 上限 1296000
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> writeCache(Long flowId, String cacheKey, Object result, int ttlSeconds) {
        if (!cacheToggle.isEnabled()) {
            return Mono.empty();
        }

        String redisKey = buildCacheKey(flowId, cacheKey);
        int effectiveTtl = Math.min(ttlSeconds, MAX_CACHE_TTL);
        if (effectiveTtl <= 0) {
            effectiveTtl = 60; // 最小 60 秒
        }
        final int finalEffectiveTtl = effectiveTtl;

        try {
            String json = objectMapper.writeValueAsString(result);
            return reactiveRedisTemplate.opsForValue()
                    .set(redisKey, json, Duration.ofSeconds(finalEffectiveTtl))
                    .doOnSuccess(v -> log.debug("Cache written: flowId={}, cacheKey={}, ttl={}s",
                            flowId, cacheKey, finalEffectiveTtl))
                    .doOnError(e -> log.warn("Failed to write cache for flowId={}, cacheKey={}: {}",
                            flowId, cacheKey, e.getMessage()))
                    .then();
        } catch (Exception e) {
            log.warn("Failed to serialize result for cache write: flowId={}, cacheKey={}: {}",
                    flowId, cacheKey, e.getMessage());
            return Mono.empty();
        }
    }

    /**
     * 使指定 flow 的所有缓存失效
     * <p>
     * 使用 KEYS + DEL 模式删除所有匹配的缓存 key。
     * 注意: KEYS 在生产环境下可能有性能影响, 后续可替换为 SCAN 模式。
     * </p>
     *
     * @param flowId 连接流 ID
     * @return Mono&lt;Long&gt; 被删除的 key 数量
     */
    public Mono<Long> invalidateFlowCache(Long flowId) {
        if (!cacheToggle.isEnabled()) {
            return Mono.just(0L);
        }

        String pattern = CACHE_KEY_PREFIX + flowId + ":*";
        return reactiveRedisTemplate.keys(pattern)
                .collectList()
                .flatMap(keys -> {
                    if (keys.isEmpty()) {
                        log.debug("No cache keys to invalidate for flowId={}", flowId);
                        return Mono.just(0L);
                    }
                    return reactiveRedisTemplate.delete(keys.toArray(new String[0]))
                            .doOnSuccess(count -> log.debug("Invalidated {} cache keys for flowId={}",
                                    count, flowId));
                })
                .doOnError(e -> log.warn("Failed to invalidate flow cache for flowId={}: {}",
                        flowId, e.getMessage()))
                .onErrorReturn(0L);
    }

    /**
     * 构建 Redis 缓存 key
     */
    private String buildCacheKey(Long flowId, String cacheKey) {
        return CACHE_KEY_PREFIX + flowId + ":" + cacheKey;
    }
}
