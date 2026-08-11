package com.xxx.it.works.wecode.v2.modules.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
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

    @Mock
    private ConnectorApiPropertyService propertyService;

    private ObjectMapper objectMapper;
    private FlowCacheManager cacheManager;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        objectMapper = new ObjectMapper();
        cacheManager = new FlowCacheManager(redisTemplate, objectMapper, propertyService);
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
    @DisplayName("写入缓存 → Lua 脚本 SET + SADD 原子化 (业务 key 与索引 key 同 hash tag)")
    void testWriteCache_Normal() {
        when(redisTemplate.execute(any(), anyList(), anyList()))
                .thenReturn(reactor.core.publisher.Flux.just(1L));

        Map<String, Object> data = new HashMap<>();
        data.put("result", "test-value");

        StepVerifier.create(cacheManager.writeCache(100L, "key1", data, 3600))
                .verifyComplete();

        // 断言: 脚本身份 + keys 含业务 key + 索引 key (含 {flowId} hash tag) + args 含 json + ttl
        verify(redisTemplate).execute(
                org.mockito.ArgumentMatchers.<org.springframework.data.redis.core.script.RedisScript<Long>>argThat(
                        script -> script != null && script.getScriptAsString() != null
                                && script.getScriptAsString().contains("SADD")),
                eq(List.of("cp:cache:flow:{100}:key1", "cp:cache:flow:keys:{100}")),
                org.mockito.ArgumentMatchers.<java.util.List<String>>argThat(
                        args -> args.size() == 2
                                && ((String) args.get(0)).contains("test-value")
                                && "3600".equals(args.get(1))));
    }

    @Test
    @DisplayName("写入缓存使用给定 TTL（不截断）")
    void testWriteCache_UseGivenTtl() {
        when(redisTemplate.execute(any(), anyList(), anyList()))
                .thenReturn(reactor.core.publisher.Flux.just(1L));

        Map<String, Object> data = new HashMap<>();
        data.put("result", "test");

        StepVerifier.create(cacheManager.writeCache(100L, "key1", data, 9999999))
                .verifyComplete();

        verify(redisTemplate).execute(
                org.mockito.ArgumentMatchers.<org.springframework.data.redis.core.script.RedisScript<Long>>argThat(
                        script -> script != null && script.getScriptAsString() != null
                                && script.getScriptAsString().contains("SADD")),
                eq(List.of("cp:cache:flow:{100}:key1", "cp:cache:flow:keys:{100}")),
                org.mockito.ArgumentMatchers.<java.util.List<String>>argThat(
                        args -> args.size() == 2
                                && ((String) args.get(0)).contains("test")
                                && "9999999".equals(args.get(1))));
    }

    @Test
    @DisplayName("业务 key 与索引 key 含同一 {flowId} hash tag (集群同 slot 约束)")
    void testCacheKey_SameHashTag() {
        // 通过 writeCache 的 execute 调用断言 keys 中两个 key 的 hash tag 一致
        when(redisTemplate.execute(any(), anyList(), anyList()))
                .thenReturn(reactor.core.publisher.Flux.just(1L));

        Map<String, Object> data = new HashMap<>();
        data.put("result", "v");

        StepVerifier.create(cacheManager.writeCache(345539333287051264L, "k1", data, 60))
                .verifyComplete();

        verify(redisTemplate).execute(
                any(),
                org.mockito.ArgumentMatchers.<java.util.List<String>>argThat(keys -> {
                    String biz = (String) keys.get(0);   // cp:cache:flow:{flowId}:k1
                    String idx = (String) keys.get(1);   // cp:cache:flow:keys:{flowId}
                    String tagBiz = biz.substring(biz.indexOf("{"), biz.indexOf("}") + 1);
                    String tagIdx = idx.substring(idx.indexOf("{"), idx.indexOf("}") + 1);
                    return tagBiz.equals(tagIdx) && tagBiz.startsWith("{") && tagBiz.endsWith("}");
                }),
                anyList());
    }

}
