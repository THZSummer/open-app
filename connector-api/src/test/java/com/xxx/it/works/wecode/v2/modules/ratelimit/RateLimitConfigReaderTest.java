package com.xxx.it.works.wecode.v2.modules.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitConfigReader 测试")
class RateLimitConfigReaderTest {

    @Mock
    private OpFlowVersionReadRepository repository;

    private ObjectMapper objectMapper;
    private RateLimitConfigReader reader;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        reader = new RateLimitConfigReader(objectMapper, repository);
    }

    @Test
    @DisplayName("流未找到 → 返回默认配置")
    void testFlowNotFound_ReturnsDefault() {
        when(repository.findByFlowId(anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals("qps", config.getMode());
                    assertEquals(RateLimitConfigReader.DEFAULT_QPS, config.getMaxQps());
                    assertEquals(RateLimitConfigReader.DEFAULT_CONCURRENCY, config.getMaxConcurrency());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("流配置了自定义 QPS → 取 min(流配置, 应用上限)")
    void testCustomQps_MinApplied() {
        FlowVersionEntity entity = new FlowVersionEntity();
        entity.setFlowId(100L);
        entity.setOrchestrationConfig("{\"nodes\":[{\"type\":\"trigger\",\"id\":\"t1\",\"data\":{\"rateLimitConfig\":{\"mode\":\"qps\",\"maxQps\":500}}}],\"edges\":[]}");
        when(repository.findByFlowId(anyLong())).thenReturn(Mono.just(entity));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals("qps", config.getMode());
                    assertEquals(500, config.getMaxQps());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("QPS 超过应用上限 10000 → 受限于 10000")
    void testQpsExceedsAppLimit_CappedAtMax() {
        FlowVersionEntity entity = new FlowVersionEntity();
        entity.setFlowId(100L);
        entity.setOrchestrationConfig("{\"nodes\":[{\"type\":\"trigger\",\"id\":\"t1\",\"data\":{\"rateLimitConfig\":{\"mode\":\"qps\",\"maxQps\":99999}}}],\"edges\":[]}");
        when(repository.findByFlowId(anyLong())).thenReturn(Mono.just(entity));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals(1000, config.getMaxQps()); // capped at APP_MAX_QPS
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("并发上限超过应用限制 1000 → 受限于 1000")
    void testConcurrencyExceedsAppLimit_CappedAtMax() {
        FlowVersionEntity entity = new FlowVersionEntity();
        entity.setFlowId(100L);
        entity.setOrchestrationConfig("{\"nodes\":[{\"type\":\"trigger\",\"id\":\"t1\",\"data\":{\"rateLimitConfig\":{\"mode\":\"concurrency\",\"maxConcurrency\":5000}}}],\"edges\":[]}");
        when(repository.findByFlowId(anyLong())).thenReturn(Mono.just(entity));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals(1000, config.getMaxConcurrency()); // capped at APP_MAX_CONCURRENCY
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("trigger 节点中无 rateLimitConfig → 返回默认配置")
    void testEmptyTriggerConfig_Defaults() {
        FlowVersionEntity entity = new FlowVersionEntity();
        entity.setFlowId(100L);
        entity.setOrchestrationConfig("{\"nodes\":[{\"type\":\"trigger\",\"id\":\"t1\",\"data\":{}}],\"edges\":[]}");
        when(repository.findByFlowId(anyLong())).thenReturn(Mono.just(entity));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals(RateLimitConfigReader.DEFAULT_QPS, config.getMaxQps());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("JSON 解析异常 → 降级返回默认配置")
    void testParseException_Defaults() {
        FlowVersionEntity entity = new FlowVersionEntity();
        entity.setFlowId(100L);
        entity.setOrchestrationConfig("invalid json");
        when(repository.findByFlowId(anyLong())).thenReturn(Mono.just(entity));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals(RateLimitConfigReader.DEFAULT_QPS, config.getMaxQps());
                })
                .verifyComplete();
    }
}
