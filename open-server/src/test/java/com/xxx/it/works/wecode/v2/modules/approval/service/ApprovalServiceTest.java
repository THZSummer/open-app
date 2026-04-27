package com.xxx.it.works.wecode.v2.modules.approval.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper;
import com.xxx.it.works.wecode.v2.modules.approval.dto.*;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalFlow;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalLog;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalLogMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.EventMapper;
import com.xxx.it.works.wecode.v2.modules.permission.mapper.SubscriptionMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("审批管理服务测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalServiceTest {

    @Mock
    private ApprovalFlowMapper flowMapper;
    @Mock
    private ApprovalRecordMapper recordMapper;
    @Mock
    private ApprovalLogMapper logMapper;
    @Mock
    private ApprovalEngine approvalEngine;
    @Mock
    private IdGeneratorStrategy idGenerator;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ApiMapper apiMapper;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private CallbackMapper callbackMapper;
    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private ApprovalService approvalService;

    @Nested
    @DisplayName("审批流程模板管理测试")
    class FlowManagementTests {

        @Test
        @DisplayName("获取流程列表成功")
        void getFlowList_success() {
            ApprovalFlowListRequest request = new ApprovalFlowListRequest();
            request.setKeyword("test");
            request.setCurPage(1);
            request.setPageSize(10);

            ApprovalFlow flow = new ApprovalFlow();
            flow.setId(1L);
            flow.setNameCn("测试流程");
            flow.setNameEn("test_flow");
            flow.setCode("test");
            flow.setStatus(1);
            flow.setNodes("[]");

            when(flowMapper.selectList("test", 0, 10)).thenReturn(List.of(flow));
            when(approvalEngine.parseNodes("[]")).thenReturn(new ArrayList<>());

            List<ApprovalFlowListResponse> result = approvalService.getFlowList(request);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("1", result.get(0).getId());
            assertEquals("测试流程", result.get(0).getNameCn());
            verify(flowMapper).selectList("test", 0, 10);
        }

        @Test
        @DisplayName("获取流程详情成功")
        void getFlowDetail_success() {
            ApprovalFlow flow = new ApprovalFlow();
            flow.setId(1L);
            flow.setNameCn("测试流程");
            flow.setNameEn("test_flow");
            flow.setCode("test");
            flow.setStatus(1);
            flow.setNodes("[]");

            when(flowMapper.selectById(1L)).thenReturn(flow);
            when(approvalEngine.parseNodes("[]")).thenReturn(new ArrayList<>());

            ApprovalFlowDetailResponse result = approvalService.getFlowDetail(1L);

            assertNotNull(result);
            assertEquals("1", result.getId());
            assertEquals("测试流程", result.getNameCn());
            assertEquals("test", result.getCode());
        }

        @Test
        @DisplayName("获取流程详情-流程不存在")
        void getFlowDetail_notFound() {
            when(flowMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class, 
                () -> approvalService.getFlowDetail(999L));

            assertEquals("404", exception.getCode());
        }

        @Test
        @DisplayName("创建流程成功")
        void createFlow_success() {
            ApprovalFlowCreateRequest request = new ApprovalFlowCreateRequest();
            request.setNameCn("新流程");
            request.setNameEn("new_flow");
            request.setCode("new_code");
            request.setNodes(new ArrayList<>());

            when(flowMapper.countByCode("new_code")).thenReturn(0);
            when(idGenerator.nextId()).thenReturn(1L);
            when(approvalEngine.serializeNodes(any())).thenReturn("[]");
            when(flowMapper.insert(any(ApprovalFlow.class))).thenReturn(1);

            ApprovalFlow savedFlow = new ApprovalFlow();
            savedFlow.setId(1L);
            savedFlow.setNameCn("新流程");
            savedFlow.setNameEn("new_flow");
            savedFlow.setCode("new_code");
            savedFlow.setStatus(1);
            savedFlow.setNodes("[]");
            when(flowMapper.selectById(1L)).thenReturn(savedFlow);
            when(approvalEngine.parseNodes("[]")).thenReturn(new ArrayList<>());

            ApprovalFlowDetailResponse result = approvalService.createFlow(request, "admin");

            assertNotNull(result);
            assertEquals("新流程", result.getNameCn());
            verify(flowMapper).insert(any(ApprovalFlow.class));
        }

        @Test
        @DisplayName("创建流程-编码已存在")
        void createFlow_codeExists() {
            ApprovalFlowCreateRequest request = new ApprovalFlowCreateRequest();
            request.setNameCn("新流程");
            request.setNameEn("new_flow");
            request.setCode("existing_code");
            request.setNodes(new ArrayList<>());

            when(flowMapper.countByCode("existing_code")).thenReturn(1);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> approvalService.createFlow(request, "admin"));

            assertEquals("409", exception.getCode());
            verify(flowMapper, never()).insert(any());
        }

        @Test
        @DisplayName("更新流程成功")
        void updateFlow_success() {
            ApprovalFlowUpdateRequest request = new ApprovalFlowUpdateRequest();
            request.setNameCn("更新流程");
            request.setNameEn("updated_flow");
            request.setNodes(new ArrayList<>());

            ApprovalFlow existingFlow = new ApprovalFlow();
            existingFlow.setId(1L);
            existingFlow.setNameCn("原流程");
            existingFlow.setCode("test");
            existingFlow.setStatus(1);

            when(flowMapper.selectById(1L)).thenReturn(existingFlow);
            when(approvalEngine.serializeNodes(any())).thenReturn("[]");
            when(flowMapper.update(any(ApprovalFlow.class))).thenReturn(1);

            ApprovalFlow updatedFlow = new ApprovalFlow();
            updatedFlow.setId(1L);
            updatedFlow.setNameCn("更新流程");
            updatedFlow.setNameEn("updated_flow");
            updatedFlow.setCode("test");
            updatedFlow.setStatus(1);
            updatedFlow.setNodes("[]");
            when(flowMapper.selectById(1L)).thenReturn(updatedFlow);
            when(approvalEngine.parseNodes("[]")).thenReturn(new ArrayList<>());

            ApprovalFlowDetailResponse result = approvalService.updateFlow(1L, request, "admin");

            assertNotNull(result);
            assertEquals("更新流程", result.getNameCn());
            verify(flowMapper).update(any(ApprovalFlow.class));
        }

        @Test
        @DisplayName("更新流程-流程不存在")
        void updateFlow_notFound() {
            ApprovalFlowUpdateRequest request = new ApprovalFlowUpdateRequest();
            request.setNameCn("更新流程");

            when(flowMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> approvalService.updateFlow(999L, request, "admin"));

            assertEquals("404", exception.getCode());
        }

        @Test
        @DisplayName("删除流程成功")
        void deleteFlow_success() {
            ApprovalFlow flow = new ApprovalFlow();
            flow.setId(1L);
            flow.setCode("test");
            flow.setNameCn("测试流程");

            when(flowMapper.selectById(1L)).thenReturn(flow);
            when(flowMapper.deleteById(1L)).thenReturn(1);

            assertDoesNotThrow(() -> approvalService.deleteFlow(1L, "admin"));

            verify(flowMapper).deleteById(1L);
        }

        @Test
        @DisplayName("删除流程-流程不存在")
        void deleteFlow_notFound() {
            when(flowMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> approvalService.deleteFlow(999L, "admin"));

            assertEquals("404", exception.getCode());
        }

        @Test
        @DisplayName("统计流程数量成功")
        void countFlowList_success() {
            when(flowMapper.countList("test")).thenReturn(10L);

            Long result = approvalService.countFlowList("test");

            assertEquals(10L, result);
        }
    }

    @Nested
    @DisplayName("审批执行管理测试")
    class ApprovalExecutionTests {

        @Test
        @DisplayName("获取待审批列表成功")
        void getPendingList_success() {
            ApprovalPendingListRequest request = new ApprovalPendingListRequest();
            request.setType("api_register");
            request.setKeyword("");
            request.setCurPage(1);
            request.setPageSize(10);

            ApprovalRecord record = new ApprovalRecord();
            record.setId(1L);
            record.setBusinessType("api_register");
            record.setBusinessId(100L);
            record.setApplicantId("user001");
            record.setApplicantName("张三");
            record.setStatus(0);
            record.setCurrentNode(0);
            record.setCreateTime(new Date());
            record.setCombinedNodes("[]");

            when(recordMapper.selectPendingList(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(record));
            when(approvalEngine.parseNodes("[]")).thenReturn(new ArrayList<>());

            List<ApprovalPendingListResponse> result = approvalService.getPendingList(request);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("1", result.get(0).getId());
            assertEquals("api_register", result.get(0).getBusinessType());
        }

        @Test
        @DisplayName("获取审批详情成功")
        void getApprovalDetail_success() {
            ApprovalRecord record = new ApprovalRecord();
            record.setId(1L);
            record.setBusinessType("api_register");
            record.setBusinessId(100L);
            record.setApplicantId("user001");
            record.setApplicantName("张三");
            record.setStatus(0);
            record.setCurrentNode(0);
            record.setCreateTime(new Date());
            record.setCombinedNodes("[]");

            when(recordMapper.selectById(1L)).thenReturn(record);
            when(approvalEngine.parseNodes("[]")).thenReturn(new ArrayList<>());
            when(logMapper.selectByRecordId(1L)).thenReturn(new ArrayList<>());

            ApprovalDetailResponse result = approvalService.getApprovalDetail(1L);

            assertNotNull(result);
            assertEquals("1", result.getId());
            assertEquals("api_register", result.getBusinessType());
        }

        @Test
        @DisplayName("获取审批详情-记录不存在")
        void getApprovalDetail_notFound() {
            when(recordMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> approvalService.getApprovalDetail(999L));

            assertEquals("404", exception.getCode());
        }

        @Test
        @DisplayName("同意审批成功")
        void approve_success() {
            ApprovalActionRequest request = new ApprovalActionRequest();
            request.setComment("同意");

            ApprovalRecord approvedRecord = new ApprovalRecord();
            approvedRecord.setId(1L);
            approvedRecord.setStatus(1);

            when(approvalEngine.approve(eq(1L), eq("user001"), eq("张三"), eq("同意"), eq("admin")))
                .thenReturn(approvedRecord);

            ApprovalActionResponse result = approvalService.approve(
                1L, request, "user001", "张三", "admin");

            assertNotNull(result);
            assertEquals("1", result.getId());
            assertEquals(1, result.getStatus());
        }

        @Test
        @DisplayName("同意审批-节点通过等待下一节点")
        void approve_nodeApproved() {
            ApprovalActionRequest request = new ApprovalActionRequest();
            request.setComment("同意");

            ApprovalRecord pendingRecord = new ApprovalRecord();
            pendingRecord.setId(1L);
            pendingRecord.setStatus(0);
            pendingRecord.setCurrentNode(1);

            when(approvalEngine.approve(eq(1L), eq("user001"), eq("张三"), eq("同意"), eq("admin")))
                .thenReturn(pendingRecord);

            ApprovalActionResponse result = approvalService.approve(
                1L, request, "user001", "张三", "admin");

            assertNotNull(result);
            assertTrue(result.getMessage().contains("等待下一节点审批"));
        }

        @Test
        @DisplayName("驳回审批成功")
        void reject_success() {
            ApprovalActionRequest request = new ApprovalActionRequest();
            request.setComment("驳回原因");

            ApprovalRecord rejectedRecord = new ApprovalRecord();
            rejectedRecord.setId(1L);
            rejectedRecord.setStatus(2);

            when(approvalEngine.reject(eq(1L), eq("user001"), eq("张三"), eq("驳回原因"), eq("admin")))
                .thenReturn(rejectedRecord);

            ApprovalActionResponse result = approvalService.reject(
                1L, request, "user001", "张三", "admin");

            assertNotNull(result);
            assertEquals("1", result.getId());
            assertEquals(2, result.getStatus());
            assertEquals("审批已驳回", result.getMessage());
        }

        @Test
        @DisplayName("驳回审批-意见为空")
        void reject_commentEmpty() {
            ApprovalActionRequest request = new ApprovalActionRequest();
            request.setComment("");

            BusinessException exception = assertThrows(BusinessException.class,
                () -> approvalService.reject(1L, request, "user001", "张三", "admin"));

            assertEquals("400", exception.getCode());
        }

        @Test
        @DisplayName("撤销审批成功")
        void cancel_success() {
            ApprovalRecord cancelledRecord = new ApprovalRecord();
            cancelledRecord.setId(1L);
            cancelledRecord.setStatus(3);

            when(approvalEngine.cancel(1L, "admin")).thenReturn(cancelledRecord);

            ApprovalActionResponse result = approvalService.cancel(1L, "admin");

            assertNotNull(result);
            assertEquals("1", result.getId());
            assertEquals(3, result.getStatus());
            assertEquals("审批已撤销", result.getMessage());
        }

        @Test
        @DisplayName("统计待审批数量成功")
        void countPendingList_success() {
            when(recordMapper.countPendingList("api_register", null, 0, null)).thenReturn(5L);

            Long result = approvalService.countPendingList("api_register", null, 0, null);

            assertEquals(5L, result);
        }
    }

    @Nested
    @DisplayName("批量审批测试")
    class BatchApprovalTests {

        @Test
        @DisplayName("批量同意审批成功")
        void batchApprove_success() {
            BatchApprovalRequest request = new BatchApprovalRequest();
            request.setApprovalIds(List.of("1", "2", "3"));
            request.setComment("批量同意");

            ApprovalRecord approvedRecord = new ApprovalRecord();
            approvedRecord.setId(1L);
            approvedRecord.setStatus(1);

            when(approvalEngine.approve(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(approvedRecord);

            BatchApprovalResponse result = approvalService.batchApprove(
                request, "user001", "张三", "admin");

            assertNotNull(result);
            assertEquals(3, result.getSuccessCount());
            assertEquals(0, result.getFailedCount());
        }

        @Test
        @DisplayName("批量同意审批-部分失败")
        void batchApprove_partialFailure() {
            BatchApprovalRequest request = new BatchApprovalRequest();
            request.setApprovalIds(List.of("1", "2", "3"));
            request.setComment("批量同意");

            ApprovalRecord approvedRecord = new ApprovalRecord();
            approvedRecord.setId(1L);
            approvedRecord.setStatus(1);

            when(approvalEngine.approve(eq(1L), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(approvedRecord);
            when(approvalEngine.approve(eq(2L), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new BusinessException("404", "记录不存在", "Record not found"));
            when(approvalEngine.approve(eq(3L), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(approvedRecord);

            BatchApprovalResponse result = approvalService.batchApprove(
                request, "user001", "张三", "admin");

            assertNotNull(result);
            assertEquals(2, result.getSuccessCount());
            assertEquals(1, result.getFailedCount());
            assertNotNull(result.getFailedItems());
            assertEquals(1, result.getFailedItems().size());
        }

        @Test
        @DisplayName("批量驳回审批成功")
        void batchReject_success() {
            BatchApprovalRequest request = new BatchApprovalRequest();
            request.setApprovalIds(List.of("1", "2"));
            request.setComment("批量驳回");

            ApprovalRecord rejectedRecord = new ApprovalRecord();
            rejectedRecord.setId(1L);
            rejectedRecord.setStatus(2);

            when(approvalEngine.reject(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(rejectedRecord);

            BatchApprovalResponse result = approvalService.batchReject(
                request, "user001", "张三", "admin");

            assertNotNull(result);
            assertEquals(2, result.getSuccessCount());
            assertEquals(0, result.getFailedCount());
        }

        @Test
        @DisplayName("批量驳回审批-意见为空")
        void batchReject_commentEmpty() {
            BatchApprovalRequest request = new BatchApprovalRequest();
            request.setApprovalIds(List.of("1", "2"));
            request.setComment("");

            BusinessException exception = assertThrows(BusinessException.class,
                () -> approvalService.batchReject(request, "user001", "张三", "admin"));

            assertEquals("400", exception.getCode());
        }
    }
}
