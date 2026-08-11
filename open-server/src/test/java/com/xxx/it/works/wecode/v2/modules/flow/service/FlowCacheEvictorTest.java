package com.xxx.it.works.wecode.v2.modules.flow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
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
 * 覆盖方案 D 核心：evictExecutionResults 通过 flow 维度索引 (SSCAN 分批 + UNLINK) 清理，
 * 不再 SCAN 全库；runAfterCommit 事务外清理注册。
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

    private static final String INDEX_KEY = "cp:cache:flow:keys:100";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(evictor, "redis", redis);
    }

    @Test
    @DisplayName("evictExecutionResults: 小批量 → 一次 UNLINK + 删索引")
    void testEvictExecutionResults_SmallBatch() {
        Cursor<String> cursor = cursorOf("cp:cache:flow:100:key1", "cp:cache:flow:100:key2");
        when(redis.opsForSet()).thenReturn(setOperations);
        when(setOperations.scan(eq(INDEX_KEY), any(ScanOptions.class))).thenReturn(cursor);

        evictor.evictExecutionResults(100L);

        verify(redis).unlink(List.of("cp:cache:flow:100:key1", "cp:cache:flow:100:key2"));
        verify(redis).delete(INDEX_KEY);
        verify(redis, never()).delete(anyList());
    }

    @Test
    @DisplayName("evictExecutionResults: 大批量(2500个) → 按每批1000分3次 UNLINK")
    void testEvictExecutionResults_LargeBatch_BatchedUnlink() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            keys.add("cp:cache:flow:100:key" + i);
        }
        Cursor<String> cursor = cursorOf(keys.toArray(new String[0]));
        when(redis.opsForSet()).thenReturn(setOperations);
        when(setOperations.scan(eq(INDEX_KEY), any(ScanOptions.class))).thenReturn(cursor);

        evictor.evictExecutionResults(100L);

        // 2500 个 → 1000 + 1000 + 500 三批
        verify(redis, times(3)).unlink(anyList());
        verify(redis).delete(INDEX_KEY);
    }

    @Test
    @DisplayName("evictExecutionResults: 索引不存在(空) → 仅删索引, 不 UNLINK")
    void testEvictExecutionResults_EmptyIndex() {
        Cursor<String> cursor = cursorOf();
        when(redis.opsForSet()).thenReturn(setOperations);
        when(setOperations.scan(eq(INDEX_KEY), any(ScanOptions.class))).thenReturn(cursor);

        evictor.evictExecutionResults(100L);

        verify(redis, never()).unlink(anyList());
        verify(redis).delete(INDEX_KEY);
    }

    @Test
    @DisplayName("evictExecutionResults: Redis 异常 → 不抛异常")
    void testEvictExecutionResults_RedisError() {
        when(redis.opsForSet()).thenReturn(setOperations);
        when(setOperations.scan(eq(INDEX_KEY), any(ScanOptions.class)))
                .thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> evictor.evictExecutionResults(100L));
    }

    @Test
    @DisplayName("runAfterCommit: 无活动事务时立即执行")
    void testRunAfterCommit_NoTransaction() {
        evictor.runAfterCommit(() -> { /* 不抛异常即可 */ });
        // 无事务时直接执行 action, 不抛异常
        assertDoesNotThrow(() -> evictor.runAfterCommit(() -> {
        }));
    }

    // ─── helpers ────────────────────────────────────────────

    /** 构造返回指定元素的 mock Cursor */
    @SuppressWarnings("unchecked")
    private Cursor<String> cursorOf(String... elements) {
        Cursor<String> cursor = mock(Cursor.class);
        List<String> list = List.of(elements);
        final int[] idx = {0};
        lenient().doAnswer(invocation -> idx[0] < list.size()).when(cursor).hasNext();
        lenient().doAnswer(invocation -> list.get(idx[0]++)).when(cursor).next();
        return cursor;
    }
}
