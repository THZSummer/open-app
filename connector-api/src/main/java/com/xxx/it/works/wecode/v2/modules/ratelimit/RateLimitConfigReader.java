package com.xxx.it.works.wecode.v2.modules.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 限流配置读取器 (connector-api)
 * <p>
 * 从连接流版本配置中读取 rateLimitConfig, 并与应用级最大限制取 min。
 * MVP 阶段: 先从 R2DBC 查询流配置, 后续可替换为 Redis 缓存读取。
 * </p>
 */
@Component
public class RateLimitConfigReader {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfigReader.class);

    /** 默认 QPS (当流未配置限流时) */
    static final int DEFAULT_QPS = 1000;

    /** 默认并发上限 (当流未配置限流时) */
    static final int DEFAULT_CONCURRENCY = 1000;

    /** 应用级最大 QPS 硬限制 */
    private static final int APP_MAX_QPS = 10000;

    /** 应用级最大并发硬限制 */
    private static final int APP_MAX_CONCURRENCY = 1000;

    private final ObjectMapper objectMapper;
    private final OpFlowVersionReadRepository flowVersionReadRepository;

    public RateLimitConfigReader(ObjectMapper objectMapper,
                                  OpFlowVersionReadRepository flowVersionReadRepository) {
        this.objectMapper = objectMapper;
        this.flowVersionReadRepository = flowVersionReadRepository;
    }

    /**
     * 读取指定 flow 的限流配置
     * <p>
     * 从 R2DBC 查询流版本配置中的 rateLimitConfig, 如果未配置则返回默认值。
     * 最终限流值 = min(流配置值, 应用级最大限制)。
     * </p>
     *
     * @param flowId 连接流 ID
     * @return Mono&lt;RateLimitConfig&gt;
     */
    public Mono<RateLimitConfig> readFlowRateLimit(Long flowId) {
        return flowVersionReadRepository.findByFlowId(flowId)
                .map(this::extractRateLimitConfig)
                .defaultIfEmpty(defaultConfig())
                .onErrorResume(e -> {
                    log.warn("Rate limit config read failed for flowId={}, using defaults: {}",
                            flowId, e.getMessage());
                    return Mono.just(defaultConfig());
                });
    }

    /**
     * 从 FlowVersionEntity 中提取 rateLimitConfig
     */
    @SuppressWarnings("unchecked")
    private RateLimitConfig extractRateLimitConfig(FlowVersionEntity entity) {
        try {
            Map<String, Object> triggerConfig = entity.getTriggerConfig(objectMapper);
            if (triggerConfig == null || triggerConfig.isEmpty()) {
                return defaultConfig();
            }

            Object dataObj = triggerConfig.get("data");
            if (!(dataObj instanceof Map)) {
                return defaultConfig();
            }
            Map<String, Object> data = (Map<String, Object>) dataObj;

            Object rateLimitObj = data.get("rateLimitConfig");
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
                if (maxQps < 1) maxQps = 1;
            }

            // 读取 maxConcurrency (取 min(flow值, app最大值))
            int maxConcurrency = DEFAULT_CONCURRENCY;
            Object maxConObj = rateLimitConfig.get("maxConcurrency");
            if (maxConObj instanceof Number) {
                maxConcurrency = Math.min(((Number) maxConObj).intValue(), APP_MAX_CONCURRENCY);
                if (maxConcurrency < 1) maxConcurrency = 1;
            }

            return new RateLimitConfig(mode, maxQps, maxConcurrency);

        } catch (Exception e) {
            log.warn("Failed to parse rate limit config for flowId={}, using defaults: {}",
                    entity.getFlowId(), e.getMessage());
            return defaultConfig();
        }
    }

    /**
     * 默认限流配置
     */
    private RateLimitConfig defaultConfig() {
        return new RateLimitConfig("qps", DEFAULT_QPS, DEFAULT_CONCURRENCY);
    }
}
