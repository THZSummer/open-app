package com.xxx.it.works.wecode.v2.modules.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowReadRepository;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import com.xxx.it.works.wecode.v2.modules.ratelimit.RateLimitConfig;
import com.xxx.it.works.wecode.v2.common.config.OpenplatformLookupRepository;
import com.xxx.it.works.wecode.v2.common.config.LookupItemEntity;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平台实体缓存管理器 (Cache-Aside 模式)
 * <p>
 * 管理 Redis 缓存中的 Flow / FlowVersion 实体,
 * 优先从 Redis 读取, miss 时回源 R2DBC 并回写缓存.
 * TTL: 7 天 ± 2 小时随机 jitter (防缓存雪崩).
 * <p>
 * 缓存 Key 格式:
 * <ul>
 *   <li>{@code cp:entity:flow:{flowId}}</li>
 *   <li>{@code cp:entity:flowversion:{versionId}}</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class EntityCacheManager {

    /** 缓存 TTL 基础值: 7 天 (秒) */
    private static final long CACHE_TTL_BASE_SECONDS = 7 * 24 * 3600;

    /** 缓存 TTL jitter 范围: ±2 小时 (秒) */
    private static final long CACHE_TTL_JITTER_SECONDS = 2 * 3600;

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final OpFlowVersionReadRepository flowVersionReadRepository;
    private final OpFlowReadRepository flowReadRepository;
    private final OpenplatformLookupRepository lookupRepository;

    public EntityCacheManager(ReactiveRedisTemplate<String, String> redisTemplate,
                               ObjectMapper objectMapper,
                               OpFlowVersionReadRepository flowVersionReadRepository,
                               OpFlowReadRepository flowReadRepository,
                               OpenplatformLookupRepository lookupRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.flowVersionReadRepository = flowVersionReadRepository;
        this.flowReadRepository = flowReadRepository;
        this.lookupRepository = lookupRepository;
    }

    // ===== Flow 缓存 =====

    /**
     * 获取 Flow: 优先 Redis → miss 回源 R2DBC → 回写 Redis
     */
    public Mono<FlowEntity> getFlow(Long flowId) {
        String key = flowKey(flowId);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        FlowEntity entity = objectMapper.readValue(json, FlowEntity.class);
                        log.debug("Flow cache hit: flowId={}", flowId);
                        return Mono.just(entity);
                    } catch (Exception e) {
                        log.warn("Failed to deserialize cached Flow, will reload from DB: flowId={}", flowId);
                        return loadFlowFromDb(flowId);
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Flow cache miss, loading from DB: flowId={}", flowId);
                    return loadFlowFromDb(flowId);
                }));
    }

    /**
     * 缓存 Flow 到 Redis
     */
    public Mono<Boolean> cacheFlow(FlowEntity entity) {
        if (entity == null || entity.getId() == null) {
            return Mono.just(false);
        }
        try {
            String json = objectMapper.writeValueAsString(entity);
            String key = flowKey(entity.getId());
            Duration ttl = randomTtl();
            return redisTemplate.opsForValue().set(key, json, ttl)
                    .doOnSuccess(success -> log.debug("Flow cached: flowId={}, ttl={}s",
                            entity.getId(), ttl.getSeconds()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Flow for cache: flowId={}", entity.getId(), e);
            return Mono.just(false);
        }
    }

    /**
     * 失效 Flow 缓存
     */
    public Mono<Boolean> invalidateFlowCache(Long flowId) {
        String key = flowKey(flowId);
        return redisTemplate.opsForValue().delete(key)
                .doOnSuccess(deleted -> log.info("Flow cache invalidated: flowId={}, deleted={}",
                        flowId, deleted));
    }

    // ===== FlowVersion 缓存 =====

    /**
     * 获取 FlowVersion: 优先 Redis → miss 回源 R2DBC → 回写 Redis
     */
    public Mono<FlowVersionEntity> getFlowVersion(Long versionId) {
        String key = flowVersionKey(versionId);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        FlowVersionEntity entity = objectMapper.readValue(json, FlowVersionEntity.class);
                        log.debug("FlowVersion cache hit: versionId={}", versionId);
                        return Mono.just(entity);
                    } catch (Exception e) {
                        log.warn("Failed to deserialize cached FlowVersion, will reload from DB: versionId={}", versionId);
                        return loadFlowVersionFromDb(versionId);
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("FlowVersion cache miss, loading from DB: versionId={}", versionId);
                    return loadFlowVersionFromDb(versionId);
                }));
    }

    /**
     * 缓存 FlowVersion 到 Redis
     */
    public Mono<Boolean> cacheFlowVersion(FlowVersionEntity entity) {
        if (entity == null || entity.getId() == null) {
            return Mono.just(false);
        }
        try {
            String json = objectMapper.writeValueAsString(entity);
            String key = flowVersionKey(entity.getId());
            Duration ttl = randomTtl();
            return redisTemplate.opsForValue().set(key, json, ttl)
                    .doOnSuccess(success -> log.debug("FlowVersion cached: versionId={}, ttl={}s",
                            entity.getId(), ttl.getSeconds()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize FlowVersion for cache: versionId={}", entity.getId(), e);
            return Mono.just(false);
        }
    }

    /**
     * 失效 FlowVersion 缓存
     */
    public Mono<Boolean> invalidateFlowVersionCache(Long versionId) {
        String key = flowVersionKey(versionId);
        return redisTemplate.opsForValue().delete(key)
                .doOnSuccess(count -> log.debug("FlowVersion cache invalidated: versionId={}, deleted={}",
                        versionId, count));
    }

    // ===== RateLimitConfig 缓存 (TTL 60s) =====

    private static final Duration RATELIMIT_TTL = Duration.ofSeconds(60);

    /**
     * 获取限流配置: Redis cache-aside, miss 时从 FlowVersion 提取并回写
     *
     * @param flowId 连接流ID
     * @return RateLimitConfig
     */
    public Mono<RateLimitConfig> getRateLimitConfig(Long flowId) {
        String key = ratelimitKey(flowId);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, RateLimitConfig.class));
                    } catch (Exception e) {
                        log.warn("RateLimitConfig cache deserialize failed: flowId={}", flowId);
                        return Mono.<RateLimitConfig>empty();
                    }
                })
                .switchIfEmpty(Mono.defer(() -> loadRateLimitFromDb(flowId)));
    }

    /**
     * 失效限流配置缓存
     */
    public Mono<Boolean> invalidateRateLimitCache(Long flowId) {
        return redisTemplate.opsForValue().delete(ratelimitKey(flowId));
    }

    private Mono<RateLimitConfig> loadRateLimitFromDb(Long flowId) {
        // 通过已缓存的 FlowVersion 获取限流配置
        return getFlowVersionByFlowId(flowId)
                .map(this::extractRateLimitConfig)
                .flatMap(config -> {
                    try {
                        String json = objectMapper.writeValueAsString(config);
                        return redisTemplate.opsForValue().set(ratelimitKey(flowId), json, RATELIMIT_TTL)
                                .thenReturn(config)
                                .onErrorResume(e -> Mono.just(config));
                    } catch (Exception e) {
                        return Mono.just(config);
                    }
                })
                .defaultIfEmpty(new RateLimitConfig("qps", 1000, 1000));
    }

    @SuppressWarnings("unchecked")
    private Mono<RateLimitConfig> extractRateLimitConfig(FlowVersionEntity version) {
        try {
            Map<String, Object> config = version.parseOrchestrationConfigAsMap(objectMapper);
            if (config == null) return Mono.empty();
            Map<String, Object> flowConfig = (Map<String, Object>) config.get("flowConfig");
            if (flowConfig == null) return Mono.empty();
            Object rlObj = flowConfig.get("rateLimitConfig");
            if (!(rlObj instanceof Map)) return Mono.empty();
            Map<String, Object> rl = (Map<String, Object>) rlObj;
            String mode = rl.get("mode") instanceof String ? (String) rl.get("mode") : "qps";
            int maxQps = rl.get("maxQps") instanceof Number ? ((Number) rl.get("maxQps")).intValue() : 1000;
            int maxCon = rl.get("maxConcurrency") instanceof Number ? ((Number) rl.get("maxConcurrency")).intValue() : 1000;
            return Mono.just(new RateLimitConfig(mode, Math.max(1, maxQps), Math.max(1, maxCon)));
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    /**
     * 按 flowId 获取 FlowVersion (不走 versionId 缓存, 用 flowId 查)
     */
    private Mono<FlowVersionEntity> getFlowVersionByFlowId(Long flowId) {
        return flowVersionReadRepository.findByFlowId(flowId).next();
    }

    // ===== Lookup 配置缓存 (TTL 5min) =====

    private static final Duration LOOKUP_TTL = Duration.ofMinutes(5);
    private static final String LOOKUP_PATH = "CEC.Open";

    /**
     * 获取 Lookup 配置: Redis cache-aside, miss 时从 DB 查并回写
     *
     * @param classifyCode 分类编码
     * @return item_code → item_value 的 Map
     */
    public Mono<Map<String, String>> getLookupConfig(String classifyCode) {
        String key = lookupKey(classifyCode);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        Map<String, String> map = objectMapper.readValue(json,
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                        log.debug("Lookup cache hit: classifyCode={}", classifyCode);
                        return Mono.just(map);
                    } catch (Exception e) {
                        log.warn("Lookup cache deserialize failed: classifyCode={}", classifyCode);
                        return Mono.<Map<String, String>>empty();
                    }
                })
                .switchIfEmpty(Mono.defer(() -> loadLookupFromDb(classifyCode)));
    }

    private Mono<Map<String, String>> loadLookupFromDb(String classifyCode) {
        return lookupRepository.findByPathAndClassifyCode(LOOKUP_PATH, classifyCode)
                .collectList()
                .map(items -> {
                    Map<String, String> result = new LinkedHashMap<>();
                    for (LookupItemEntity item : items) {
                        if (item.getItemCode() != null) {
                            result.put(item.getItemCode(), item.getItemValue());
                        }
                    }
                    return result;
                })
                .flatMap(map -> {
                    if (map.isEmpty()) return Mono.just(map);
                    try {
                        String json = objectMapper.writeValueAsString(map);
                        return redisTemplate.opsForValue().set(lookupKey(classifyCode), json, LOOKUP_TTL)
                                .thenReturn(map)
                                .onErrorResume(e -> Mono.just(map));
                    } catch (Exception e) {
                        return Mono.just(map);
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Lookup DB load failed: classifyCode={}", classifyCode, e);
                    return Mono.just(new LinkedHashMap<>());
                });
    }

    /**
     * 失效 Lookup 配置缓存
     */
    public Mono<Boolean> invalidateLookupCache(String classifyCode) {
        return redisTemplate.opsForValue().delete(lookupKey(classifyCode));
    }

    // ===== 内部方法 =====

    /**
     * 从 R2DBC 加载 Flow, 并回写 Redis
     */
    private Mono<FlowEntity> loadFlowFromDb(Long flowId) {
        return flowReadRepository.findById(flowId)
                .flatMap(entity -> {
                    log.info("Flow loaded from DB: flowId={}", flowId);
                    return cacheFlow(entity).thenReturn(entity);
                })
                .doOnError(e -> log.error("Failed to load Flow from DB: flowId={}", flowId, e));
    }

    /**
     * 从 R2DBC 加载 FlowVersion, 并回写 Redis
     */
    private Mono<FlowVersionEntity> loadFlowVersionFromDb(Long versionId) {
        return flowVersionReadRepository.findById(versionId)
                .flatMap(entity -> {
                    log.info("FlowVersion loaded from DB: versionId={}", versionId);
                    return cacheFlowVersion(entity).thenReturn(entity);
                })
                .doOnError(e -> log.error("Failed to load FlowVersion from DB: versionId={}", versionId, e));
    }

    /**
     * 生成随机 TTL: 7 天 ± 2 小时 (防缓存雪崩)
     */
    private Duration randomTtl() {
        long jitter = ThreadLocalRandom.current().nextLong(
                -CACHE_TTL_JITTER_SECONDS, CACHE_TTL_JITTER_SECONDS + 1);
        long ttlSeconds = CACHE_TTL_BASE_SECONDS + jitter;
        return Duration.ofSeconds(Math.max(ttlSeconds, 3600)); // 至少 1 小时
    }

    // ===== Key 构建 =====

    private String flowKey(Long flowId) {
        return "cp:entity:flow:" + flowId;
    }

    private String flowVersionKey(Long versionId) {
        return "cp:entity:flowversion:" + versionId;
    }

    private String ratelimitKey(Long flowId) {
        return "cp:entity:ratelimit:" + flowId;
    }

    private String lookupKey(String classifyCode) {
        return "cp:entity:lookup:" + classifyCode;
    }


}
