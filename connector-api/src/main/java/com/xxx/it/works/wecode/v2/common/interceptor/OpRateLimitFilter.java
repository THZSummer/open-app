package com.xxx.it.works.wecode.v2.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流过滤器 (connector-api) (v5.8)
 * <p>
 * FR-024: HTTP 触发超过 trigger.data.rateLimitConfig.maxQps → 返回 429
 * 限流维度: 按 flowId
 * 使用 Token Bucket 算法 (Bucket4j)
 * </p>
 * <p>
 * v5.8: 429 响应改为透明穿透格式 — X- 平台头 + 空Body (不再返回 JSON errorInfo).
 * v5.5: 从 orchestrationConfig.trigger.data.rateLimitConfig.maxQps 动态读取限流配置,
 * 而非硬编码默认值.
 * </p>
 */
@Component
public class OpRateLimitFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(OpRateLimitFilter.class);

    private final ObjectMapper objectMapper;
    private final OpFlowVersionReadRepository flowVersionReadRepository;

    /** 限流桶缓存: flowId → Bucket */
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    /** 默认限流: 10 QPS (当未配置限流时) */
    private static final int DEFAULT_MAX_QPS = 10;

    public OpRateLimitFilter(ObjectMapper objectMapper,
                           OpFlowVersionReadRepository flowVersionReadRepository) {
        this.objectMapper = objectMapper;
        this.flowVersionReadRepository = flowVersionReadRepository;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 仅对 HTTP 触发端点进行限流
        if (!path.matches("/api/v1/trigger/\\d+/invoke")) {
            return chain.filter(exchange);
        }

        // 从路径提取 flowId
        String flowId = extractFlowId(path);
        if (flowId == null) {
            return chain.filter(exchange);
        }

        // 获取或创建限流桶 (如果已缓存则直接使用, 否则从配置查询)
        return getOrCreateBucket(flowId)
                .flatMap(bucket -> {
                    if (bucket.tryConsume(1)) {
                        return chain.filter(exchange);
                    } else {
                        // 超过限流, 返回 429 结构化错误响应
                        log.warn("Rate limit exceeded for flowId={}", flowId);
                        return writeRateLimitResponse(exchange);
                    }
                });
    }

    /**
     * 获取或创建限流桶 (优先查缓存, 未命中则从配置读取 QPS)
     */
    private Mono<Bucket> getOrCreateBucket(String flowId) {
        Bucket existing = bucketCache.get(flowId);
        if (existing != null) {
            return Mono.just(existing);
        }
        return lookupMaxQps(flowId)
                .map(qps -> bucketCache.computeIfAbsent(flowId, k -> createBucket(qps)));
    }

    /**
     * 从 orchestrationConfig.trigger.data.rateLimitConfig.maxQps 查询 QPS 配置
     */
    @SuppressWarnings("unchecked")
    private Mono<Integer> lookupMaxQps(String flowId) {
        try {
            Long id = Long.parseLong(flowId);
            return flowVersionReadRepository.findByFlowId(id)
                    .map(entity -> extractMaxQpsFromConfig(entity))
                    .defaultIfEmpty(DEFAULT_MAX_QPS);
        } catch (NumberFormatException e) {
            log.warn("Invalid flowId format: {}", flowId);
            return Mono.just(DEFAULT_MAX_QPS);
        }
    }

    /**
     * 从 FlowVersionEntity 的 orchestrationConfig 提取 rateLimitConfig.maxQps
     * <p>
     * 访问路径: orchestrationConfig → trigger → data → rateLimitConfig → maxQps
     * </p>
     */
    @SuppressWarnings("unchecked")
    private int extractMaxQpsFromConfig(FlowVersionEntity entity) {
        try {
            Map<String, Object> triggerConfig = entity.getTriggerConfig(objectMapper);
            if (triggerConfig == null || triggerConfig.isEmpty()) {
                return DEFAULT_MAX_QPS;
            }

            Object dataObj = triggerConfig.get("data");
            if (!(dataObj instanceof Map)) {
                return DEFAULT_MAX_QPS;
            }
            Map<String, Object> data = (Map<String, Object>) dataObj;

            Object rateLimitObj = data.get("rateLimitConfig");
            if (!(rateLimitObj instanceof Map)) {
                return DEFAULT_MAX_QPS;
            }
            Map<String, Object> rateLimitConfig = (Map<String, Object>) rateLimitObj;

            Object maxQpsObj = rateLimitConfig.get("maxQps");
            if (maxQpsObj instanceof Number) {
                return ((Number) maxQpsObj).intValue();
            }
        } catch (Exception e) {
            log.warn("Failed to parse rate limit config for flow, using default: {}", e.getMessage());
        }
        return DEFAULT_MAX_QPS;
    }

    /**
     * 写入 429 限流响应 (v5.8 透明穿透: X- 头 + 空Body)
     */
    private Mono<Void> writeRateLimitResponse(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String flowId = extractFlowId(path);

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Retry-After", "1");

        // 平台元数据 X- 头
        if (flowId != null) {
            exchange.getResponse().getHeaders().add("X-Flow-Id", flowId);
        }
        exchange.getResponse().getHeaders().add("X-Code", "429");
        exchange.getResponse().getHeaders().add("X-Message-Zh", "请求频率超限");
        exchange.getResponse().getHeaders().add("X-Message-En", "Too many requests");

        // 空 Body (无 Content-Type)
        return exchange.getResponse().setComplete();
    }

    /**
     * 从路径中提取 flowId
     */
    private String extractFlowId(String path) {
        // path 格式: /api/v1/trigger/{flowId}/invoke
        String[] parts = path.split("/");
        if (parts.length >= 5) {
            return parts[4]; // trigger 后面的部分
        }
        return null;
    }

    /**
     * 创建限流桶
     */
    private Bucket createBucket(int maxQps) {
        Bandwidth limit = Bandwidth.classic(maxQps, Refill.greedy(maxQps, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}