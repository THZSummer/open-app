package com.xxx.it.works.wecode.v2.modules.sync.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SyncDetail;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SyncRequest;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SyncResult;
import com.xxx.it.works.wecode.v2.modules.sync.service.SyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SyncController 测试类
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("数据同步控制器测试")
class SyncControllerTest {

    @Mock
    private SyncService syncService;

    @InjectMocks
    private SyncController syncController;

    private SyncResult createSuccessResult() {
        return SyncResult.builder()
                .success(10)
                .failed(2)
                .skipped(3)
                .details(Arrays.asList(
                        SyncDetail.builder()
                                .id(1L)
                                .status("success")
                                .approvalStatus("同步1条审批记录")
                                .build(),
                        SyncDetail.builder()
                                .id(2L)
                                .status("failed")
                                .error("未找到对应的新权限")
                                .build()
                ))
                .build();
    }

    @Nested
    @DisplayName("migrate 迁移接口测试")
    class MigrateTest {

        @Test
        @DisplayName("批量迁移成功")
        void testMigrate_Batch_Success() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L, 2L, 3L));

            SyncResult mockResult = createSuccessResult();
            when(syncService.migrate(any(SyncRequest.class))).thenReturn(mockResult);

            // When
            ApiResponse<SyncResult> response = syncController.migrate(request);

            // Then
            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertEquals("操作成功", response.getMessageZh());
            assertEquals("Success", response.getMessageEn());
            
            SyncResult result = response.getData();
            assertNotNull(result);
            assertEquals(10, result.getSuccess());
            assertEquals(2, result.getFailed());
            assertEquals(3, result.getSkipped());
            assertEquals(2, result.getDetails().size());

            verify(syncService).migrate(argThat(req ->
                    req.getIds().size() == 3 &&
                    req.getIds().contains(1L) &&
                    req.getIds().contains(2L) &&
                    req.getIds().contains(3L)
            ));
        }

        @Test
        @DisplayName("全量迁移（ids为null）")
        void testMigrate_FullSync_NullIds() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(null);

            SyncResult mockResult = SyncResult.builder()
                    .success(100)
                    .failed(0)
                    .skipped(0)
                    .details(Collections.emptyList())
                    .build();
            when(syncService.migrate(any(SyncRequest.class))).thenReturn(mockResult);

            // When
            ApiResponse<SyncResult> response = syncController.migrate(request);

            // Then
            assertEquals("200", response.getCode());
            assertEquals(100, response.getData().getSuccess());
            
            verify(syncService).migrate(argThat(req -> req.getIds() == null));
        }

        @Test
        @DisplayName("全量迁移（ids为空数组）")
        void testMigrate_FullSync_EmptyIds() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Collections.emptyList());

            SyncResult mockResult = SyncResult.builder()
                    .success(50)
                    .failed(0)
                    .skipped(0)
                    .details(Collections.emptyList())
                    .build();
            when(syncService.migrate(any(SyncRequest.class))).thenReturn(mockResult);

            // When
            ApiResponse<SyncResult> response = syncController.migrate(request);

            // Then
            assertEquals("200", response.getCode());
            assertEquals(50, response.getData().getSuccess());
        }

        @Test
        @DisplayName("迁移返回详细信息")
        void testMigrate_WithDetails() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L));

            SyncResult mockResult = SyncResult.builder()
                    .success(1)
                    .failed(0)
                    .skipped(0)
                    .details(Arrays.asList(
                            SyncDetail.builder()
                                    .id(1L)
                                    .status("success")
                                    .approvalStatus("同步1条审批记录")
                                    .approvalLogStatus("同步2条审批日志")
                                    .build()
                    ))
                    .build();
            when(syncService.migrate(any(SyncRequest.class))).thenReturn(mockResult);

            // When
            ApiResponse<SyncResult> response = syncController.migrate(request);

            // Then
            SyncDetail detail = response.getData().getDetails().get(0);
            assertEquals(1L, detail.getId());
            assertEquals("success", detail.getStatus());
            assertEquals("同步1条审批记录", detail.getApprovalStatus());
            assertEquals("同步2条审批日志", detail.getApprovalLogStatus());
        }
    }

    @Nested
    @DisplayName("rollback 回退接口测试")
    class RollbackTest {

        @Test
        @DisplayName("批量回退成功")
        void testRollback_Batch_Success() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L, 2L));

            SyncResult mockResult = SyncResult.builder()
                    .success(5)
                    .failed(1)
                    .skipped(2)
                    .details(Collections.emptyList())
                    .build();
            when(syncService.rollback(any(SyncRequest.class))).thenReturn(mockResult);

            // When
            ApiResponse<SyncResult> response = syncController.rollback(request);

            // Then
            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertEquals(5, response.getData().getSuccess());
            assertEquals(1, response.getData().getFailed());
            assertEquals(2, response.getData().getSkipped());

            verify(syncService).rollback(argThat(req ->
                    req.getIds().size() == 2
            ));
        }

        @Test
        @DisplayName("全量回退")
        void testRollback_FullSync() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(null);

            SyncResult mockResult = SyncResult.builder()
                    .success(200)
                    .failed(0)
                    .skipped(0)
                    .details(Collections.emptyList())
                    .build();
            when(syncService.rollback(any(SyncRequest.class))).thenReturn(mockResult);

            // When
            ApiResponse<SyncResult> response = syncController.rollback(request);

            // Then
            assertEquals("200", response.getCode());
            assertEquals(200, response.getData().getSuccess());
        }

        @Test
        @DisplayName("回退返回失败详情")
        void testRollback_WithFailure() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L));

            SyncResult mockResult = SyncResult.builder()
                    .success(0)
                    .failed(1)
                    .skipped(0)
                    .details(Arrays.asList(
                            SyncDetail.builder()
                                    .id(1L)
                                    .status("failed")
                                    .error("未找到对应的旧权限")
                                    .build()
                    ))
                    .build();
            when(syncService.rollback(any(SyncRequest.class))).thenReturn(mockResult);

            // When
            ApiResponse<SyncResult> response = syncController.rollback(request);

            // Then
            assertEquals(0, response.getData().getSuccess());
            assertEquals(1, response.getData().getFailed());
            
            SyncDetail detail = response.getData().getDetails().get(0);
            assertEquals("failed", detail.getStatus());
            assertEquals("未找到对应的旧权限", detail.getError());
        }
    }

    @Nested
    @DisplayName("接口参数验证测试")
    class RequestValidationTest {

        @Test
        @DisplayName("migrate 和 rollback 使用相同的请求格式")
        void testSameRequestFormat() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L, 2L, 3L));

            when(syncService.migrate(any())).thenReturn(SyncResult.builder().build());
            when(syncService.rollback(any())).thenReturn(SyncResult.builder().build());

            // When
            syncController.migrate(request);
            syncController.rollback(request);

            // Then
            verify(syncService).migrate(same(request));
            verify(syncService).rollback(same(request));
        }

        @Test
        @DisplayName("响应包含完整字段")
        void testResponseFields() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(null);

            when(syncService.migrate(any())).thenReturn(createSuccessResult());

            // When
            ApiResponse<SyncResult> response = syncController.migrate(request);

            // Then
            assertNotNull(response.getCode());
            assertNotNull(response.getMessageZh());
            assertNotNull(response.getMessageEn());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getSuccess());
            assertNotNull(response.getData().getFailed());
            assertNotNull(response.getData().getSkipped());
            assertNotNull(response.getData().getDetails());
        }
    }
}