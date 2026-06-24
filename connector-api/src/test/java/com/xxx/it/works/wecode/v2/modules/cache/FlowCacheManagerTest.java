package com.xxx.it.works.wecode.v2.modules.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.CacheToggle;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlowCacheManager 测试")
class FlowCacheManagerTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private CacheToggle cacheToggle;
    private FlowCacheManager cacheManager;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        objectMapper = new ObjectMapper();
        cacheToggle = new CacheToggle();
        cacheToggle.setEnabled(true);
        cacheManager = new FlowCacheManager(redisTemplate, objectMapper, cacheToggle);
    }

    @Test
    @DisplayName("缓存命中 → 返回 ExecutionResult")
    void testCheckCache_Hit() throws Exception {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionId("exec-001");
        result.setFlowId("100");
        result.setStatus("success");
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("key", "value");
        result.setResultData(resultData);

        String cachedJson = objectMapper.writeValueAsString(result);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.just(cachedJson));

        StepVerifier.create(cacheManager.checkCache(100L, "key1"))
                .expectNextMatches(r -> "exec-001".equals(r.getExecutionId())
                        && "success".equals(r.getStatus()))
                .verifyComplete();
    }

    @Test
    @DisplayName("缓存未命中 → 返回 Mono.empty()")
    void testCheckCache_Miss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(cacheManager.checkCache(100L, "key1"))
                .verifyComplete();
    }

    @Test
    @DisplayName("缓存开关关闭 → checkCache 直接返回空")
    void testCheckCache_ToggleDisabled() {
        cacheToggle = new CacheToggle();
        cacheToggle.setEnabled(false);
        cacheManager = new FlowCacheManager(redisTemplate, objectMapper, cacheToggle);

        StepVerifier.create(cacheManager.checkCache(100L, "key1"))
                .verifyComplete();

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("写入缓存 → 正确调用 SETEX")
    void testWriteCache_Normal() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        Map<String, Object> data = new HashMap<>();
        data.put("result", "test-value");

        StepVerifier.create(cacheManager.writeCache(100L, "key1", data, 3600))
                .verifyComplete();

        verify(valueOperations).set(contains("cp:cache:flow:100:key1"), anyString(),
                eq(Duration.ofSeconds(3600)));
    }

    @Test
    @DisplayName("写入缓存 TTL 超过上限 → 受限于 max 1296000")
    void testWriteCache_TtlCapped() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        Map<String, Object> data = new HashMap<>();
        data.put("result", "test");

        StepVerifier.create(cacheManager.writeCache(100L, "key1", data, 9999999))
                .verifyComplete();

        verify(valueOperations).set(anyString(), anyString(),
                eq(Duration.ofSeconds(1296000)));
    }

    @Test
    @DisplayName("版本变更 → invalidateFlowCache 清空相关 key")
    void testInvalidateFlowCache() {
        when(redisTemplate.keys(contains("cp:cache:flow:100:*")))
                .thenReturn(reactor.core.publisher.Flux.just("cp:cache:flow:100:key1", "cp:cache:flow:100:key2"));
        when(redisTemplate.delete(any(String[].class)))
                .thenReturn(Mono.just(2L));

        StepVerifier.create(cacheManager.invalidateFlowCache(100L))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    @DisplayName("缓存开关关闭 → writeCache 不做操作")
    void testWriteCache_ToggleDisabled() {
        cacheToggle = new CacheToggle();
        cacheToggle.setEnabled(false);
        cacheManager = new FlowCacheManager(redisTemplate, objectMapper, cacheToggle);

        StepVerifier.create(cacheManager.writeCache(100L, "key1", "data", 3600))
                .verifyComplete();

        verify(redisTemplate, never()).opsForValue();
    }
}
