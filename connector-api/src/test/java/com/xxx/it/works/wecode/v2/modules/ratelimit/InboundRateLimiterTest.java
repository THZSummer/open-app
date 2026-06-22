package com.xxx.it.works.wecode.v2.modules.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("InboundRateLimiter 测试")
class InboundRateLimiterTest {

    @SuppressWarnings("unchecked")
    private ReactiveRedisTemplate<String, String> redisTemplate;
    private RateLimitConfigReader rateLimitConfigReader;
    private InboundRateLimiter limiter;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(ReactiveRedisTemplate.class);
        rateLimitConfigReader = mock(RateLimitConfigReader.class);

        // Default: no flow found → no rate limit applied
        when(rateLimitConfigReader.readFlowRateLimit(anyLong()))
                .thenReturn(Mono.just(new RateLimitConfig("qps", 1000, 1000)));

        limiter = new InboundRateLimiter(redisTemplate, rateLimitConfigReader);
    }

    @Test
    @DisplayName("非触发端点直接放行")
    void testNonTriggerPath_PassesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://localhost:18180/api/v1/flows")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(limiter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(rateLimitConfigReader, never()).readFlowRateLimit(anyLong());
    }

    @Test
    @DisplayName("触发端点 - QPS 限流通过")
    void testQpsLimit_Pass() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenReturn(reactor.core.publisher.Flux.just(1L));

        MockServerHttpRequest request = MockServerHttpRequest
                .post("http://localhost:18180/api/v1/trigger/123/invoke")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(limiter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("触发端点 - QPS 超限返回 429")
    void testQpsLimit_Exceeded_Returns429() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenReturn(reactor.core.publisher.Flux.just(0L));

        MockServerHttpRequest request = MockServerHttpRequest
                .post("http://localhost:18180/api/v1/trigger/456/invoke")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        limiter.filter(exchange, chain).block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        assertEquals("1", exchange.getResponse().getHeaders().getFirst("Retry-After"));
    }

    @Test
    @DisplayName("Redis script 返回空 → 降级放行")
    void testRedisEmptyResult_DegradePass() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenReturn(reactor.core.publisher.Flux.empty());

        MockServerHttpRequest request = MockServerHttpRequest
                .post("http://localhost:18180/api/v1/trigger/789/invoke")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(limiter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("Redis 不可用 → 降级放行")
    void testRedisFailure_DegradePass() {
        when(rateLimitConfigReader.readFlowRateLimit(anyLong()))
                .thenReturn(Mono.error(new RuntimeException("Redis connection refused")));

        MockServerHttpRequest request = MockServerHttpRequest
                .post("http://localhost:18180/api/v1/trigger/123/invoke")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(limiter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("并发限流 → 通过")
    void testConcurrencyLimit_Pass() {
        when(rateLimitConfigReader.readFlowRateLimit(anyLong()))
                .thenReturn(Mono.just(new RateLimitConfig("concurrency", 1000, 10)));

        when(redisTemplate.opsForValue()).thenReturn(mock(
                org.springframework.data.redis.core.ReactiveValueOperations.class));
        when(redisTemplate.opsForValue().increment(anyString()))
                .thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(java.time.Duration.class)))
                .thenReturn(Mono.just(true));

        MockServerHttpRequest request = MockServerHttpRequest
                .post("http://localhost:18180/api/v1/trigger/123/invoke")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(limiter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("无效 flowId 格式 → 放行")
    void testInvalidFlowId_PassesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("http://localhost:18180/api/v1/trigger/invalid/invoke")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(limiter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }
}
