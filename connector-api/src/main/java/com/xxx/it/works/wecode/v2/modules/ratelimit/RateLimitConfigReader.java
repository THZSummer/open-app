package com.xxx.it.works.wecode.v2.modules.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
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
 * 从连接流版本配置的 flowConfig.rateLimitConfig 读取限流配置。
 * §3.3.4: ③(flowConfig.rateLimitConfig)存在直接用，不截断；③不存在回退①(平台全局)。
 * </p>
 */
@Component
public class RateLimitConfigReader {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfigReader.class);

    private final ObjectMapper objectMapper;
    private final OpFlowVersionReadRepository flowVersionReadRepository;
    private final ConnectorApiPropertyService propertyService;

    public RateLimitConfigReader(ObjectMapper objectMapper,
                                  OpFlowVersionReadRepository flowVersionReadRepository,
                                  ConnectorApiPropertyService propertyService) {
        this.objectMapper = objectMapper;
        this.flowVersionReadRepository = flowVersionReadRepository;
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
        return flowVersionReadRepository.findByFlowId(flowId)
                .flatMap(this::extractRateLimitConfig)
                .switchIfEmpty(defaultConfig())
                .onErrorResume(e -> {
                    log.warn("Rate limit config read failed for flowId={}, using defaults", flowId, e);
                    return defaultConfig();
                });
    }

    /**
     * 从 FlowVersionEntity 中提取 rateLimitConfig
     * §3.3.4: 从 flowConfig.rateLimitConfig 读取, ③存在用③不截断, ③不存在回退①
     */
    @SuppressWarnings("unchecked")
    private Mono<RateLimitConfig> extractRateLimitConfig(FlowVersionEntity entity) {
        try {
            Map<String, Object> config = entity.parseOrchestrationConfigAsMap(objectMapper);
            if (config == null || config.isEmpty()) {
                return defaultConfig();
            }

            Map<String, Object> flowConfig = (Map<String, Object>) config.get("flowConfig");
            if (flowConfig == null) {
                return defaultConfig();
            }

            Object rateLimitObj = flowConfig.get("rateLimitConfig");
            if (!(rateLimitObj instanceof Map)) {
                return defaultConfig();
            }
            Map<String, Object> rateLimitConfig = (Map<String, Object>) rateLimitObj;

            Object modeObj = rateLimitConfig.get("mode");
            String mode = (modeObj instanceof String) ? (String) modeObj : "qps";

            Object maxQpsObj = rateLimitConfig.get("maxQps");
            Object maxConObj = rateLimitConfig.get("maxConcurrency");

            // ③的 maxQps 和 maxConcurrency 都存在 → 直接用③，不读①
            if (maxQpsObj instanceof Number && maxConObj instanceof Number) {
                int maxQps = ((Number) maxQpsObj).intValue();
                if (maxQps < 1) maxQps = 1;
                int maxConcurrency = ((Number) maxConObj).intValue();
                if (maxConcurrency < 1) maxConcurrency = 1;
                return Mono.just(new RateLimitConfig(mode, maxQps, maxConcurrency));
            }

            // ③的某个字段不存在 → 该字段回退①
            return propertyService.getFlowMaxQps()
                    .zipWith(propertyService.getFlowMaxConcurrency())
                    .map(tuple -> {
                        int maxQps = tuple.getT1();
                        int maxConcurrency = tuple.getT2();
                        if (maxQpsObj instanceof Number) {
                            maxQps = ((Number) maxQpsObj).intValue();
                            if (maxQps < 1) maxQps = 1;
                        }
                        if (maxConObj instanceof Number) {
                            maxConcurrency = ((Number) maxConObj).intValue();
                            if (maxConcurrency < 1) maxConcurrency = 1;
                        }
                        return new RateLimitConfig(mode, maxQps, maxConcurrency);
                    });
        } catch (Exception e) {
            log.warn("Failed to parse rate limit config for flowId={}, using defaults", entity.getFlowId(), e);
            return defaultConfig();
        }
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
