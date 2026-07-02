package com.xxx.it.works.wecode.v2.modules.ratelimit;

import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


/**
 * 限流配置读取器 (connector-api)
 * <p>
 * 从连接流版本配置的 flowConfig.rateLimitConfig 读取限流配置。
 * §3.3.4: ③(flowConfig.rateLimitConfig)存在直接用，不截断；③不存在回退①(平台全局)。
 * </p>
 */
@Component
public class RateLimitConfigReader {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfigReader.class);

    private final EntityCacheManager entityCacheManager;
    private final ConnectorApiPropertyService propertyService;

    public RateLimitConfigReader(EntityCacheManager entityCacheManager,
                                  ConnectorApiPropertyService propertyService) {
        this.entityCacheManager = entityCacheManager;
        this.propertyService = propertyService;
    }

    /**
     * 读取指定 flow 的限流配置
     * <p>
     * §3.3.4: 从 R2DBC 查询流版本配置中的 flowConfig.rateLimitConfig，
     * ③存在直接用不截断，③不存在回退①(平台全局)。
     * </p>
     *
     * @param flowId 连接流 ID
     * @return Mono&lt;RateLimitConfig&gt;
     */
    public Mono<RateLimitConfig> readFlowRateLimit(Long flowId) {
        return entityCacheManager.getRateLimitConfig(flowId)
                .switchIfEmpty(defaultConfig())
                .onErrorResume(e -> {
                    log.warn("Rate limit config read failed for flowId={}, using defaults", flowId, e);
                    return defaultConfig();
                });
    }

    /**
     * 默认限流配置 (从①平台全局读取)
     */
    private Mono<RateLimitConfig> defaultConfig() {
        return propertyService.getFlowMaxQps()
                .zipWith(propertyService.getFlowMaxConcurrency())
                .map(tuple -> new RateLimitConfig("qps", tuple.getT1(), tuple.getT2()))
                .onErrorReturn(new RateLimitConfig("qps", 1000, 1000));
    }
}
