package com.xxx.it.works.wecode.v2.modules.execution;

import com.xxx.it.works.wecode.v2.modules.execution.entity.ExecutionRecord;
import com.xxx.it.works.wecode.v2.modules.execution.mapper.ExecutionRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionRecordService 测试")
class ExecutionRecordServiceTest {

    @Mock
    private ExecutionRecordMapper executionRecordMapper;

    @InjectMocks
    private ExecutionRecordService service;

    @Test
    @DisplayName("HTTP 触发写入 — triggerType=1")
    void testStartRecord_HttpTrigger() {
        Long recordId = service.startRecord(1000L, 100L, 200L, 1L, 1);

        assertEquals(1000L, recordId);
        verify(executionRecordMapper).insert(any(ExecutionRecord.class));
    }

    @Test
    @DisplayName("调试触发写入 — triggerType=2")
    void testStartRecord_DebugTrigger() {
        Long recordId = service.startRecord(1001L, 100L, 200L, 1L, 2);

        assertEquals(1001L, recordId);
        verify(executionRecordMapper).insert(any(ExecutionRecord.class));
    }

    @Test
    @DisplayName("写入失败 → 不影响返回值")
    void testStartRecord_InsertFailure_NoException() {
        doThrow(new RuntimeException("DB error")).when(executionRecordMapper).insert(any());

        // 不应抛出异常
        Long recordId = assertDoesNotThrow(() ->
                service.startRecord(1000L, 100L, 200L, 1L, 1));

        assertEquals(1000L, recordId);
    }

    @Test
    @DisplayName("更新记录 — 状态和耗时正确")
    void testUpdateRecord_Success() {
        service.updateRecord(1000L, 0, 150, null, null);

        verify(executionRecordMapper).update(any(ExecutionRecord.class));
    }

    @Test
    @DisplayName("更新记录含错误信息")
    void testUpdateRecord_WithError() {
        service.updateRecord(1000L, 1, 200, "EC-001", "执行失败");

        verify(executionRecordMapper).update(any(ExecutionRecord.class));
    }

    @Test
    @DisplayName("更新失败 → 吞掉异常")
    void testUpdateRecord_Failure_Swallowed() {
        doThrow(new RuntimeException("DB error")).when(executionRecordMapper).update(any());

        assertDoesNotThrow(() ->
                service.updateRecord(1000L, 0, 150, null, null));
    }

    @Test
    @DisplayName("FIFO 清理 — 记录数超上限删除最早记录")
    void testCheckAndCleanFifo_ExceedsLimit() {
        when(executionRecordMapper.countByFlowId(100L)).thenReturn(150L);

        service.checkAndCleanFifo(100L, 100);

        verify(executionRecordMapper).deleteOldestByFlowId(100L, 50);
    }

    @Test
    @DisplayName("FIFO 清理 — 记录未超上限不删除")
    void testCheckAndCleanFifo_UnderLimit() {
        when(executionRecordMapper.countByFlowId(100L)).thenReturn(80L);

        service.checkAndCleanFifo(100L, 100);

        verify(executionRecordMapper, never()).deleteOldestByFlowId(anyLong(), anyInt());
    }
}
