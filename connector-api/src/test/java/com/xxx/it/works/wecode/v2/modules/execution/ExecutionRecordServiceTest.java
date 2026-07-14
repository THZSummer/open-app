package com.xxx.it.works.wecode.v2.modules.execution;

import com.xxx.it.works.wecode.v2.modules.execution.entity.ExecutionRecordEntity;
import com.xxx.it.works.wecode.v2.modules.execution.repository.ExecutionRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionRecordService 测试")
class ExecutionRecordServiceTest {

    @Mock
    private ExecutionRecordRepository repository;

    @InjectMocks
    private ExecutionRecordService service;

    // ===== startRecord =====

    @Test
    @DisplayName("HTTP 触发写入 — triggerType=1")
    void testStartRecord_HttpTrigger() {
        when(repository.save(any())).thenReturn(Mono.just(new ExecutionRecordEntity()));

        Long recordId = service.startRecord(1000L, 100L, 200L, 1L, 1, null, null, null, null, null);
        assertEquals(1000L, recordId);
        verify(repository).save(any(ExecutionRecordEntity.class));
    }

    @Test
    @DisplayName("调试触发写入 — triggerType=2")
    void testStartRecord_DebugTrigger() {
        when(repository.save(any())).thenReturn(Mono.just(new ExecutionRecordEntity()));

        Long recordId = service.startRecord(1001L, 100L, 200L, 1L, 2, null, null, null, null, null);
        assertEquals(1001L, recordId);
        verify(repository).save(any(ExecutionRecordEntity.class));
    }

    @Test
    @DisplayName("写入失败 → 不影响返回值")
    void testStartRecord_InsertFailure_NoException() {
        when(repository.save(any())).thenReturn(Mono.error(new RuntimeException("DB error")));

        Long recordId = assertDoesNotThrow(() ->
                service.startRecord(1000L, 100L, 200L, 1L, 1, null, null, null, null, null));
        assertEquals(1000L, recordId);
    }

    // ===== updateRecord =====

    @Test
    @DisplayName("更新记录 — 状态和耗时正确")
    void testUpdateRecord_Success() {
        when(repository.updateStatus(anyLong(), anyInt(), anyInt(), isNull(), isNull(), any()))
                .thenReturn(Mono.just(1));

        service.updateRecord(1000L, 0, 150, null, null);
        verify(repository).updateStatus(eq(1000L), eq(0), eq(150), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("更新记录含错误信息")
    void testUpdateRecord_WithError() {
        when(repository.updateStatus(anyLong(), anyInt(), anyInt(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(1));

        service.updateRecord(1000L, 1, 200, "EC-001", "执行失败");
        verify(repository).updateStatus(eq(1000L), eq(1), eq(200), eq("EC-001"), eq("执行失败"), any());
    }

    @Test
    @DisplayName("更新失败 → 吞掉异常")
    void testUpdateRecord_Failure_Swallowed() {
        when(repository.updateStatus(anyLong(), anyInt(), anyInt(), isNull(), isNull(), any()))
                .thenReturn(Mono.error(new RuntimeException("DB error")));

        assertDoesNotThrow(() ->
                service.updateRecord(1000L, 0, 150, null, null));
    }

    // ===== FIFO =====

    @Test
    @DisplayName("FIFO 清理 — 记录数超上限删除最早记录")
    void testCheckAndCleanFifo_ExceedsLimit() {
        when(repository.countByFlowId(100L)).thenReturn(Mono.just(150L));
        when(repository.deleteOldestByFlowId(100L, 50)).thenReturn(Mono.just(50));

        service.checkAndCleanFifo(100L, 100);

        verify(repository).countByFlowId(100L);
        verify(repository).deleteOldestByFlowId(100L, 50);
    }

    @Test
    @DisplayName("FIFO 清理 — 记录未超上限不删除")
    void testCheckAndCleanFifo_UnderLimit() {
        when(repository.countByFlowId(100L)).thenReturn(Mono.just(80L));

        service.checkAndCleanFifo(100L, 100);

        verify(repository, never()).deleteOldestByFlowId(anyLong(), anyInt());
    }
}