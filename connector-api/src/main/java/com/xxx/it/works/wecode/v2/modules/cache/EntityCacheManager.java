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

    public EntityCacheManager(ReactiveRedisTemplate<String, String> redisTemplate,
                               ObjectMapper objectMapper,
                               OpFlowVersionReadRepository flowVersionReadRepository,
                               OpFlowReadRepository flowReadRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.flowVersionReadRepository = flowVersionReadRepository;
        this.flowReadRepository = flowReadRepository;
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
                .map(count -> count > 0)
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

}
