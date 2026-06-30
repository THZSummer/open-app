package com.xxx.it.works.wecode.v2.modules.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitConfigReader 测试")
class RateLimitConfigReaderTest {

    @Mock
    private OpFlowVersionReadRepository repository;

    @Mock
    private ConnectorApiPropertyService propertyService;

    private ObjectMapper objectMapper;
    private RateLimitConfigReader reader;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(propertyService.getFlowMaxQps()).thenReturn(Mono.just(1000));
        when(propertyService.getFlowMaxConcurrency()).thenReturn(Mono.just(1000));
        reader = new RateLimitConfigReader(objectMapper, repository, propertyService);
    }

    @Test
    @DisplayName("流未找到 → 返回默认配置")
    void testFlowNotFound_ReturnsDefault() {
        when(repository.findByFlowId(anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals("qps", config.getMode());
                    assertEquals(1000, config.getMaxQps());
                    assertEquals(1000, config.getMaxConcurrency());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("流配置了自定义 QPS → 使用流配置值")
    void testCustomQps_UseFlowConfig() {
        FlowVersionEntity entity = new FlowVersionEntity();
        entity.setFlowId(100L);
        entity.setOrchestrationConfig("{\"nodes\":[{\"type\":\"trigger\",\"id\":\"t1\",\"data\":{}}],\"edges\":[],\"flowConfig\":{\"rateLimitConfig\":{\"mode\":\"qps\",\"maxQps\":500}}}");
        when(repository.findByFlowId(anyLong())).thenReturn(Mono.just(entity));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals("qps", config.getMode());
                    assertEquals(500, config.getMaxQps());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("QPS 使用流配置值（不由 Reader 截断）")
    void testQpsUsesFlowConfig() {
        FlowVersionEntity entity = new FlowVersionEntity();
        entity.setFlowId(100L);
        entity.setOrchestrationConfig("{\"nodes\":[{\"type\":\"trigger\",\"id\":\"t1\",\"data\":{}}],\"edges\":[],\"flowConfig\":{\"rateLimitConfig\":{\"mode\":\"qps\",\"maxQps\":99999}}}");
        when(repository.findByFlowId(anyLong())).thenReturn(Mono.just(entity));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals(99999, config.getMaxQps());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("并发上限使用流配置值（不由 Reader 截断）")
    void testConcurrencyUsesFlowConfig() {
        FlowVersionEntity entity = new FlowVersionEntity();
        entity.setFlowId(100L);
        entity.setOrchestrationConfig("{\"nodes\":[{\"type\":\"trigger\",\"id\":\"t1\",\"data\":{}}],\"edges\":[],\"flowConfig\":{\"rateLimitConfig\":{\"mode\":\"concurrency\",\"maxConcurrency\":5000}}}");
        when(repository.findByFlowId(anyLong())).thenReturn(Mono.just(entity));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals(5000, config.getMaxConcurrency());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("flowConfig 中无 rateLimitConfig → 返回默认配置")
    void testEmptyFlowConfig_Defaults() {
        FlowVersionEntity entity = new FlowVersionEntity();
        entity.setFlowId(100L);
        entity.setOrchestrationConfig("{\"nodes\":[{\"type\":\"trigger\",\"id\":\"t1\",\"data\":{}}],\"edges\":[]}");
        when(repository.findByFlowId(anyLong())).thenReturn(Mono.just(entity));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals(1000, config.getMaxQps());
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
                    assertEquals(1000, config.getMaxQps());
                })
                .verifyComplete();
    }
}
