package com.xxx.it.works.wecode.v2.modules.approval.controller;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.model.UserContext;
import com.xxx.it.works.wecode.v2.modules.approval.dto.*;
import com.xxx.it.works.wecode.v2.modules.approval.service.ApprovalService;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("审批管理控制器测试")
class ApprovalControllerTest {

    @Mock
    private ApprovalService approvalService;

    @InjectMocks
    private ApprovalController approvalController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        UserContextHolder.set(UserContext.builder()
                .userId("testUser001")
                .userName("测试用户")
                .build());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Nested
    @DisplayName("审批流程模板管理测试")
    class ApprovalFlowTests {

        @Test
        @DisplayName("#41 获取审批流程列表成功")
        void testGetFlowList_Success() {
            ApprovalFlowListResponse response1 = new ApprovalFlowListResponse();
            response1.setId("1001");
            response1.setNameCn("全局审批流程");
            response1.setNameEn("Global Approval Flow");
            response1.setCode("global");
            response1.setStatus(1);
            response1.setNodes(Collections.emptyList());

            List<ApprovalFlowListResponse> mockList = Collections.singletonList(response1);

            when(approvalService.getFlowList(any())).thenReturn(mockList);
            when(approvalService.countFlowList(any())).thenReturn(1L);

            ApiResponse<List<ApprovalFlowListResponse>> response = approvalController.getFlowList(
                    "审批", 1, 20);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertEquals("global", response.getData().get(0).getCode());
            assertNotNull(response.getPage());
            assertEquals(1L, response.getPage().getTotal());
            verify(approvalService).getFlowList(any());
            verify(approvalService).countFlowList(any());
        }

        @Test
        @DisplayName("#42 获取审批流程详情成功")
        void testGetFlowDetail_Success() {
            ApprovalFlowDetailResponse mockResponse = new ApprovalFlowDetailResponse();
            mockResponse.setId("1001");
            mockResponse.setNameCn("全局审批流程");
            mockResponse.setNameEn("Global Approval Flow");
            mockResponse.setCode("global");
            mockResponse.setStatus(1);
            mockResponse.setNodes(Collections.emptyList());

            when(approvalService.getFlowDetail(1001L)).thenReturn(mockResponse);

            ApiResponse<ApprovalFlowDetailResponse> response = approvalController.getFlowDetail("1001");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("1001", response.getData().getId());
            assertEquals("global", response.getData().getCode());
            verify(approvalService).getFlowDetail(1001L);
        }

        @Test
        @DisplayName("#43 创建审批流程成功")
        void testCreateFlow_Success() {
            ApprovalFlowCreateRequest request = new ApprovalFlowCreateRequest();
            request.setNameCn("测试审批流程");
            request.setNameEn("Test Approval Flow");
            request.setCode("test_flow");
            request.setNodes(Collections.emptyList());

            ApprovalFlowDetailResponse mockResponse = new ApprovalFlowDetailResponse();
            mockResponse.setId("1001");
            mockResponse.setNameCn("测试审批流程");
            mockResponse.setNameEn("Test Approval Flow");
            mockResponse.setCode("test_flow");
            mockResponse.setStatus(1);
            mockResponse.setNodes(Collections.emptyList());

            when(approvalService.createFlow(any(), eq("testUser001"))).thenReturn(mockResponse);

            ApiResponse<ApprovalFlowDetailResponse> response = approvalController.createFlow(request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("test_flow", response.getData().getCode());
            verify(approvalService).createFlow(any(), eq("testUser001"));
        }

        @Test
        @DisplayName("#44 更新审批流程成功")
        void testUpdateFlow_Success() {
            ApprovalFlowUpdateRequest request = new ApprovalFlowUpdateRequest();
            request.setNameCn("更新后的审批流程");
            request.setNameEn("Updated Approval Flow");
            request.setNodes(Collections.emptyList());

            ApprovalFlowDetailResponse mockResponse = new ApprovalFlowDetailResponse();
            mockResponse.setId("1001");
            mockResponse.setNameCn("更新后的审批流程");
            mockResponse.setNameEn("Updated Approval Flow");
            mockResponse.setCode("test_flow");
            mockResponse.setStatus(1);
            mockResponse.setNodes(Collections.emptyList());

            when(approvalService.updateFlow(eq(1001L), any(), eq("testUser001"))).thenReturn(mockResponse);

            ApiResponse<ApprovalFlowDetailResponse> response = approvalController.updateFlow("1001", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("更新后的审批流程", response.getData().getNameCn());
            verify(approvalService).updateFlow(eq(1001L), any(), eq("testUser001"));
        }

        @Test
        @DisplayName("#45 删除审批流程成功")
        void testDeleteFlow_Success() {
            doNothing().when(approvalService).deleteFlow(1001L, "testUser001");

            ApiResponse<Void> response = approvalController.deleteFlow("1001");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            verify(approvalService).deleteFlow(1001L, "testUser001");
        }
    }

    @Nested
    @DisplayName("审批执行管理测试")
    class ApprovalExecutionTests {

        @Test
        @DisplayName("#46 获取待审批列表成功")
        void testGetPendingList_Success() {
            ApprovalPendingListResponse response1 = new ApprovalPendingListResponse();
            response1.setId("2001");
            response1.setBusinessType("api_register");
            response1.setBusinessId("3001");
            response1.setBusinessName("测试API");
            response1.setApplicantId("user001");
            response1.setApplicantName("申请人");
            response1.setStatus(0);
            response1.setCurrentNode(0);

            List<ApprovalPendingListResponse> mockList = Collections.singletonList(response1);

            when(approvalService.getPendingList(any())).thenReturn(mockList);
            when(approvalService.countPendingList(any(), any(), any(), any())).thenReturn(1L);

            ApiResponse<List<ApprovalPendingListResponse>> response = approvalController.getPendingList(
                    "api_register", null, null, null, null, 1, 20);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertEquals("api_register", response.getData().get(0).getBusinessType());
            assertNotNull(response.getPage());
            assertEquals(1L, response.getPage().getTotal());
            verify(approvalService).getPendingList(any());
            verify(approvalService).countPendingList(any(), any(), any(), any());
        }

        @Test
        @DisplayName("#47 获取审批详情成功")
        void testGetApprovalDetail_Success() {
            ApprovalDetailResponse mockResponse = new ApprovalDetailResponse();
            mockResponse.setId("2001");
            mockResponse.setBusinessType("api_register");
            mockResponse.setBusinessId("3001");
            mockResponse.setApplicantId("user001");
            mockResponse.setApplicantName("申请人");
            mockResponse.setStatus(0);
            mockResponse.setCurrentNode(0);
            mockResponse.setNodes(Collections.emptyList());
            mockResponse.setCombinedNodes(Collections.emptyList());
            mockResponse.setLogs(Collections.emptyList());

            when(approvalService.getApprovalDetail(2001L)).thenReturn(mockResponse);

            ApiResponse<ApprovalDetailResponse> response = approvalController.getApprovalDetail("2001");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("2001", response.getData().getId());
            assertEquals("api_register", response.getData().getBusinessType());
            verify(approvalService).getApprovalDetail(2001L);
        }

        @Test
        @DisplayName("#48 同意审批成功")
        void testApprove_Success() {
            ApprovalActionRequest request = new ApprovalActionRequest();
            request.setComment("同意申请");

            ApprovalActionResponse mockResponse = ApprovalActionResponse.builder()
                    .id("2001")
                    .status(1)
                    .message("审批通过")
                    .build();

            when(approvalService.approve(eq(2001L), any(), eq("testUser001"), eq("测试用户"), eq("testUser001")))
                    .thenReturn(mockResponse);

            ApiResponse<ApprovalActionResponse> response = approvalController.approve("2001", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("2001", response.getData().getId());
            assertEquals(1, response.getData().getStatus());
            verify(approvalService).approve(eq(2001L), any(), eq("testUser001"), eq("测试用户"), eq("testUser001"));
        }

        @Test
        @DisplayName("#49 驳回审批成功")
        void testReject_Success() {
            ApprovalActionRequest request = new ApprovalActionRequest();
            request.setComment("驳回申请");

            ApprovalActionResponse mockResponse = ApprovalActionResponse.builder()
                    .id("2001")
                    .status(2)
                    .message("审批已驳回")
                    .build();

            when(approvalService.reject(eq(2001L), any(), eq("testUser001"), eq("测试用户"), eq("testUser001")))
                    .thenReturn(mockResponse);

            ApiResponse<ApprovalActionResponse> response = approvalController.reject("2001", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("2001", response.getData().getId());
            assertEquals(2, response.getData().getStatus());
            verify(approvalService).reject(eq(2001L), any(), eq("testUser001"), eq("测试用户"), eq("testUser001"));
        }

        @Test
        @DisplayName("#50 撤销审批成功")
        void testCancel_Success() {
            ApprovalActionResponse mockResponse = ApprovalActionResponse.builder()
                    .id("2001")
                    .status(3)
                    .message("审批已撤销")
                    .build();

            when(approvalService.cancel(2001L, "testUser001")).thenReturn(mockResponse);

            ApiResponse<ApprovalActionResponse> response = approvalController.cancel("2001");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("2001", response.getData().getId());
            assertEquals(3, response.getData().getStatus());
            verify(approvalService).cancel(2001L, "testUser001");
        }

        @Test
        @DisplayName("#51 批量同意审批成功")
        void testBatchApprove_Success() {
            BatchApprovalRequest request = new BatchApprovalRequest();
            request.setApprovalIds(Arrays.asList("2001", "2002"));
            request.setComment("批量同意");

            BatchApprovalResponse mockResponse = BatchApprovalResponse.builder()
                    .successCount(2)
                    .failedCount(0)
                    .message("批量审批完成，成功2条，失败0条")
                    .build();

            when(approvalService.batchApprove(any(), eq("testUser001"), eq("测试用户"), eq("testUser001")))
                    .thenReturn(mockResponse);

            ApiResponse<BatchApprovalResponse> response = approvalController.batchApprove(request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(2, response.getData().getSuccessCount());
            assertEquals(0, response.getData().getFailedCount());
            verify(approvalService).batchApprove(any(), eq("testUser001"), eq("测试用户"), eq("testUser001"));
        }

        @Test
        @DisplayName("#52 批量驳回审批成功")
        void testBatchReject_Success() {
            BatchApprovalRequest request = new BatchApprovalRequest();
            request.setApprovalIds(Arrays.asList("2001", "2002"));
            request.setComment("批量驳回");

            BatchApprovalResponse mockResponse = BatchApprovalResponse.builder()
                    .successCount(2)
                    .failedCount(0)
                    .message("批量驳回完成，成功2条，失败0条")
                    .build();

            when(approvalService.batchReject(any(), eq("testUser001"), eq("测试用户"), eq("testUser001")))
                    .thenReturn(mockResponse);

            ApiResponse<BatchApprovalResponse> response = approvalController.batchReject(request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(2, response.getData().getSuccessCount());
            assertEquals(0, response.getData().getFailedCount());
            verify(approvalService).batchReject(any(), eq("testUser001"), eq("测试用户"), eq("testUser001"));
        }
    }
}