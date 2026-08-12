package com.xxx.it.works.wecode.v2.modules.flow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FlowCacheEvictor 测试
 * <p>
 * 覆盖方案 D 核心：evictExecutionResults 通过 flow 维度索引 (SPOP 分批弹出 + UNLINK) 清理，
 * 不再 SCAN 全库；SPOP 为 Redis 原生命令 (Lettuce/Redisson 均兼容)。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlowCacheEvictor 测试")
class FlowCacheEvictorTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private FlowCacheEvictor evictor;

    private static final String INDEX_KEY = "cp:cache:flow:keys:{100}";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(evictor, "redis", redis);
        when(redis.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("evictExecutionResults: 小批量 → 一次 UNLINK + 删索引")
    void testEvictExecutionResults_SmallBatch() {
        // SPOP 一次弹出全部, 第二次返回空 → 循环终止
        when(setOperations.pop(eq(INDEX_KEY), anyLong()))
                .thenReturn(List.of("cp:cache:flow:100:key1", "cp:cache:flow:100:key2"))
                .thenReturn(List.of());

        evictor.evictExecutionResults(100L);

        verify(redis).unlink(List.of("cp:cache:flow:100:key1", "cp:cache:flow:100:key2"));
        verify(redis).delete(INDEX_KEY);
    }

    @Test
    @DisplayName("evictExecutionResults: 大批量(2500个) → 按每批1000分3次 UNLINK")
    void testEvictExecutionResults_LargeBatch_BatchedUnlink() {
        List<String> batch1 = new ArrayList<>();
        List<String> batch2 = new ArrayList<>();
        List<String> batch3 = new ArrayList<>();
        for (int i = 0; i < 1000; i++) batch1.add("cp:cache:flow:100:key" + i);
        for (int i = 1000; i < 2000; i++) batch2.add("cp:cache:flow:100:key" + i);
        for (int i = 2000; i < 2500; i++) batch3.add("cp:cache:flow:100:key" + i);

        when(setOperations.pop(eq(INDEX_KEY), anyLong()))
                .thenReturn(batch1)
                .thenReturn(batch2)
                .thenReturn(batch3)
                .thenReturn(List.of());

        evictor.evictExecutionResults(100L);

        // 2500 个 → 3 批 UNLINK + 删索引
        verify(redis, times(3)).unlink(anyList());
        verify(redis).delete(INDEX_KEY);
    }

    @Test
    @DisplayName("evictExecutionResults: 索引不存在(空) → 仅删索引, 不 UNLINK")
    void testEvictExecutionResults_EmptyIndex() {
        when(setOperations.pop(eq(INDEX_KEY), anyLong())).thenReturn(List.of());

        evictor.evictExecutionResults(100L);

        verify(redis, never()).unlink(anyList());
        verify(redis).delete(INDEX_KEY);
    }

    @Test
    @DisplayName("evictExecutionResults: Redis 异常 → 不抛异常")
    void testEvictExecutionResults_RedisError() {
        when(setOperations.pop(eq(INDEX_KEY), anyLong()))
                .thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> evictor.evictExecutionResults(100L));
    }
}
