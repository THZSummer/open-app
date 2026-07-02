package com.xxx.it.works.wecode.v2.modules.ratelimit;

import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
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
    private EntityCacheManager entityCacheManager;

    @Mock
    private ConnectorApiPropertyService propertyService;

    private RateLimitConfigReader reader;

    @BeforeEach
    void setUp() {
        when(propertyService.getFlowMaxQps()).thenReturn(Mono.just(1000));
        when(propertyService.getFlowMaxConcurrency()).thenReturn(Mono.just(1000));
        reader = new RateLimitConfigReader(entityCacheManager, propertyService);
    }

    @Test
    @DisplayName("流未找到 → 返回默认配置")
    void testFlowNotFound_ReturnsDefault() {
        when(entityCacheManager.getRateLimitConfig(anyLong())).thenReturn(Mono.empty());

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
        when(entityCacheManager.getRateLimitConfig(anyLong())).thenReturn(Mono.just(new RateLimitConfig("qps", 500, 1000)));

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
        when(entityCacheManager.getRateLimitConfig(anyLong())).thenReturn(Mono.just(new RateLimitConfig("qps", 99999, 1000)));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals(99999, config.getMaxQps());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("并发上限使用流配置值（不由 Reader 截断）")
    void testConcurrencyUsesFlowConfig() {
        when(entityCacheManager.getRateLimitConfig(anyLong())).thenReturn(Mono.just(new RateLimitConfig("concurrency", 1000, 5000)));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals(5000, config.getMaxConcurrency());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("无限流配置（Mono.empty）→ 返回默认配置")
    void testEmptyFlowConfig_Defaults() {
        when(entityCacheManager.getRateLimitConfig(anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals(1000, config.getMaxQps());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("读取异常 → 降级返回默认配置")
    void testReadException_Defaults() {
        when(entityCacheManager.getRateLimitConfig(anyLong())).thenReturn(Mono.error(new RuntimeException("simulated error")));

        StepVerifier.create(reader.readFlowRateLimit(100L))
                .assertNext(config -> {
                    assertEquals(1000, config.getMaxQps());
                })
                .verifyComplete();
    }
}
