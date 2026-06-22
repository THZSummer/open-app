package com.xxx.it.works.wecode.v2.modules.execution;

import com.xxx.it.works.wecode.v2.modules.execution.mapper.ExecutionRecordMapper;
import com.xxx.it.works.wecode.v2.modules.execution.mapper.ExecutionStepMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionCleanupJob 测试")
class ExecutionCleanupJobTest {

    @Mock
    private ExecutionRecordMapper executionRecordMapper;

    @Mock
    private ExecutionStepMapper executionStepMapper;

    @InjectMocks
    private ExecutionCleanupJob cleanupJob;

    @Test
    @DisplayName("30天定时清理 — 分批删除过期记录")
    void testCleanExpiredRecords_BatchDeletion() {
        // 第一次调用返回 1000 条（一批），第二次返回 0（清理完成）
        when(executionRecordMapper.deleteByTriggerTimeBefore(any(Date.class), eq(1000)))
                .thenReturn(1000, 500, 0);

        cleanupJob.cleanExpiredRecords();

        verify(executionRecordMapper, times(3)).deleteByTriggerTimeBefore(any(Date.class), eq(1000));
    }

    @Test
    @DisplayName("无过期记录 → 直接退出")
    void testCleanExpiredRecords_NoExpiredRecords() {
        when(executionRecordMapper.deleteByTriggerTimeBefore(any(Date.class), eq(1000)))
                .thenReturn(0);

        cleanupJob.cleanExpiredRecords();

        verify(executionRecordMapper, times(1)).deleteByTriggerTimeBefore(any(Date.class), eq(1000));
    }

    @Test
    @DisplayName("清理异常 → 吞掉异常")
    void testCleanExpiredRecords_Exception() {
        when(executionRecordMapper.deleteByTriggerTimeBefore(any(Date.class), eq(1000)))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertDoesNotThrow(() -> cleanupJob.cleanExpiredRecords());
    }
}
