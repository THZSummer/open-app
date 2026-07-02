package com.xxx.it.works.wecode.v2.modules.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 入站请求限流过滤器 (connector-api) — Redis Lua 原子限流
 * <p>
 * 支持两种模式:
 * <ul>
 *   <li>QPS 模式: Lua 令牌桶脚本 (SET+EX+DECR 原子操作), 按秒级粒度限流</li>
 *   <li>Concurrency 模式: Redis INCR/DECR, 控制同时在途请求数</li>
 * </ul>
 * </p>
 * <p>
 * 如果 Redis 不可用, 降级放行 (log.warn) 以保证业务连续性。
 * </p>
 */
@Component
@Order(-100)
public class InboundRateLimiter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(InboundRateLimiter.class);

    /** Redis Key 前缀: QPS */
    private static final String QPS_KEY_PREFIX = "rate_limit:";

    /** Redis Key 前缀: 并发 */
    private static final String CONCURRENCY_KEY_PREFIX = "cp:ratelimit:concurrency:";

    /** 限流 Key TTL (秒) — QPS 秒级桶, 1s 足够覆盖当前秒内所有请求, 下秒自动换桶 */
    private static final int QPS_KEY_TTL_SECONDS = 1;

    /** 并发 Key TTL (秒) — 防止异常情况下的 key 泄漏 */
    private static final int CONCURRENCY_KEY_TTL_SECONDS = 300;

    /** 限流命中后的等待时间 (秒) */
    private static final int RETRY_AFTER_SECONDS = 1;

    /** QPS 限流 Lua 脚本 (令牌桶, 原子操作: GET→SET/DECR→return) */
    private static final RedisScript<Long> QPS_SCRIPT;
    static {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/rate_limit_token_bucket.lua")));
        script.setResultType(Long.class);
        QPS_SCRIPT = script;
    }

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final RateLimitConfigReader rateLimitConfigReader;

    public InboundRateLimiter(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                                RateLimitConfigReader rateLimitConfigReader) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.rateLimitConfigReader = rateLimitConfigReader;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 仅对触发调用路径限流
        String flowId = extractFlowId(path);
        if (flowId == null) {
            return chain.filter(exchange);
        }

        Long flowIdLong;
        try {
            flowIdLong = Long.parseLong(flowId);
        } catch (NumberFormatException e) {
            log.warn("Invalid flowId format in path: {}", flowId);
            return chain.filter(exchange);
        }

        // 读取限流配置并执行限流
        return rateLimitConfigReader.readFlowRateLimit(flowIdLong)
                .flatMap(config -> applyRateLimit(exchange, chain, flowId, config))
                .onErrorResume(e -> {
                    // Redis 不可用或配置读取失败 → 降级放行
                    log.warn("Rate limit check degraded for flowId={}", flowId, e);
                    return chain.filter(exchange);
                });
    }

    /**
     * 根据限流模式执行对应的限流策略
     */
    private Mono<Void> applyRateLimit(ServerWebExchange exchange, WebFilterChain chain,
                                       String flowId, RateLimitConfig config) {
        if ("concurrency".equalsIgnoreCase(config.getMode())) {
            return applyConcurrencyLimit(exchange, chain, flowId, config);
        } else {
            // 默认 QPS 模式
            return applyQpsLimit(exchange, chain, flowId, config);
        }
    }

    /**
     * QPS 限流: Lua 令牌桶脚本 (原子操作)
     * <p>
     * 脚本逻辑: GET key → 不存在则 SET(maxTokens-1, EX, ttl) 返回1 →
     * 存在且>0则 DECR 返回1 → 否则返回0
     * </p>
     * <p>
     * 相比 INCR+EXPIRE 两步方案, Lua 脚本保证原子性:
     * 无竞态窗口, 无 EXPIRE 丢失风险, 单次 Redis 往返
     * </p>
     */
    private Mono<Void> applyQpsLimit(ServerWebExchange exchange, WebFilterChain chain,
                                       String flowId, RateLimitConfig config) {
        // 秒级桶: rate_limit:{flowId}:{second_bucket}
        String secondBucket = LocalDateTime.now()
                .truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String key = QPS_KEY_PREFIX + flowId + ":" + secondBucket;

        int maxQps = config.getMaxQps();

        // 执行 Lua 脚本: KEYS[1]=key, ARGV[1]=maxQps, ARGV[2]=ttl
        // 返回 1=允许, 0=拒绝
        return reactiveRedisTemplate.execute(
                QPS_SCRIPT,
                List.of(key),
                List.of(String.valueOf(maxQps), String.valueOf(QPS_KEY_TTL_SECONDS))
        ).next()
        .flatMap(result -> {
            if (result != null && result == 1L) {
                return chain.filter(exchange);
            } else {
                log.warn("QPS rate limit exceeded: flowId={}, maxQps={}", flowId, maxQps);
                return writeRateLimitResponse(exchange, flowId);
            }
        });
    }

    /**
     * 并发限流: Redis INCR/DECR
     */
    private Mono<Void> applyConcurrencyLimit(ServerWebExchange exchange, WebFilterChain chain,
                                              String flowId, RateLimitConfig config) {
        String key = CONCURRENCY_KEY_PREFIX + flowId;
        int maxConcurrency = config.getMaxConcurrency();

        // 先 INCR 检查是否超限
        return reactiveRedisTemplate.opsForValue().increment(key)
                .flatMap(current -> {
                    // 首次使用设置 TTL
                    if (current == 1) {
                        return reactiveRedisTemplate.expire(key, Duration.ofSeconds(CONCURRENCY_KEY_TTL_SECONDS))
                                .thenReturn(current);
                    }
                    return Mono.just(current);
                })
                .flatMap(current -> {
                    if (current <= maxConcurrency) {
                        // 未超限, 放行, 并在完成后 DECR
                        return chain.filter(exchange)
                                .doFinally(signalType -> {
                                    reactiveRedisTemplate.opsForValue().decrement(key)
                                            .subscribe(
                                                    v -> {},
                                                    e -> log.warn("Failed to decrement concurrency key for flowId={}", flowId, e)
                                            );
                                });
                    } else {
                        // 超限, 回退计数器并拒绝
                        log.warn("Concurrency limit exceeded: flowId={}, current={}, max={}",
                                flowId, current, maxConcurrency);
                        return reactiveRedisTemplate.opsForValue().decrement(key)
                                .then(writeRateLimitResponse(exchange, flowId));
                    }
                });
    }

    /**
     * 写入 429 限流响应 (透明穿透: X- 头 + 空Body)。
     */
    private Mono<Void> writeRateLimitResponse(ServerWebExchange exchange, String flowId) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(RETRY_AFTER_SECONDS));

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
     * 从请求路径中提取 flowId
     * <p>
     * 支持两种路径格式:
     * <ul>
     *   <li>{@code /api/v1/trigger/{flowId}/invoke}</li>
     *   <li>{@code /api/v1/flows/{flowId}/invoke}</li>
     * </ul>
     * </p>
     */
    private String extractFlowId(String path) {
        if (path == null) { return null; }

        // path 格式: /api/v1/trigger/{flowId}/invoke 或 /api/v1/flows/{flowId}/invoke
        // split 结果: ["", "api", "v1", "trigger"|"flows", "{flowId}", "invoke"]
        String[] parts = path.split("/");
        if (parts.length >= 5) {
            String segment = parts[3]; // "trigger" or "flows"
            if ("trigger".equals(segment) || "flows".equals(segment)) {
                return parts[4]; // flowId
            }
        }
        return null;
    }
}
