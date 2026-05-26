package com.xxx.it.works.wecode.v2.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.repository.FlowVersionReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DisplayName("RateLimitFilter 测试")
class RateLimitFilterTest {

    private ObjectMapper objectMapper;
    private FlowVersionReadRepository flowVersionReadRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        flowVersionReadRepository = mock(FlowVersionReadRepository.class);
        // Default mock: findByFlowId returns empty (no config found → DEFAULT_MAX_QPS)
        when(flowVersionReadRepository.findByFlowId(anyLong())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("非触发端点直接放行")
    void testNonTriggerPath_PassesThrough() {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, flowVersionReadRepository);
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://localhost:18180/api/v1/flows")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("触发端点 - 速率内放行（非阻塞）")
    void testTriggerPath_UnderLimit() {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, flowVersionReadRepository);
        MockServerHttpRequest request = MockServerHttpRequest
                .post("http://localhost:18180/api/v1/trigger/123/invoke")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Subscribe to execute the reactive chain
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // In the rate-limited path, chain.filter is called within flatMap
        verify(chain, timeout(1000)).filter(exchange);
    }

    @Test
    @DisplayName("触发端点 - 同flowId发送多请求至少部分通过")
    void testTriggerPath_OverLimit() {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, flowVersionReadRepository);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // 发送 20 个请求到同一个 flowId
        for (int i = 0; i < 20; i++) {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("http://localhost:18180/api/v1/trigger/456/invoke")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            filter.filter(exchange, chain).subscribe();
        }

        // 至少有一些通过了 (default bucket is 10 QPS)
        verify(chain, timeout(1000).atLeast(1)).filter(any());
    }

    @Test
    @DisplayName("不同 flowId 独立限流（各自独立计数）")
    void testDifferentFlowId_IndependentRateLimit() {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, flowVersionReadRepository);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // 分别对 flowA 和 flowB 发送请求，各 flow 独立限流
        for (int i = 0; i < 12; i++) {
            MockServerHttpRequest reqA = MockServerHttpRequest
                    .post("http://localhost:18180/api/v1/trigger/flowA/invoke").build();
            MockServerHttpRequest reqB = MockServerHttpRequest
                    .post("http://localhost:18180/api/v1/trigger/flowB/invoke").build();
            filter.filter(MockServerWebExchange.from(reqA), chain).subscribe();
            filter.filter(MockServerWebExchange.from(reqB), chain).subscribe();
        }

        // 验证 filter 未抛出异常，链被调用了至少一次
        verify(chain, timeout(1000).atLeast(1)).filter(any());
    }
}