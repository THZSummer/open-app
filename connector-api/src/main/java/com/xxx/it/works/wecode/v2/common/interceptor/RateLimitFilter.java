package com.xxx.it.works.wecode.v2.common.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流过滤器 (connector-api)
 * <p>
 * FR-024: HTTP 触发超过 trigger.rateLimit.maxQps → 返回 429
 * 限流维度: 按 flowId
 * 使用 Token Bucket 算法 (Bucket4j)
 * </p>
 */
@Component
public class RateLimitFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ObjectMapper objectMapper;

    /** 限流桶缓存: flowId → Bucket */
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    /** 默认限流: 10 QPS (当未配置限流时) */
    private static final int DEFAULT_MAX_QPS = 10;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

        // 获取或创建限流桶
        // MVP 简化: 使用默认限流值 (V1 从 flow_version_t 读取 trigger.rateLimit.maxQps)
        Bucket bucket = bucketCache.computeIfAbsent(flowId, k -> createBucket(DEFAULT_MAX_QPS));

        // 尝试消费 token
        if (bucket.tryConsume(1)) {
            return chain.filter(exchange);
        } else {
            // 超过限流, 返回 429
            log.warn("Rate limit exceeded for flowId={}", flowId);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Retry-After", "1");
            return exchange.getResponse().setComplete();
        }
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