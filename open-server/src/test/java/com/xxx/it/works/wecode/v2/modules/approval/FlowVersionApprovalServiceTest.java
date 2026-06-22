package com.xxx.it.works.wecode.v2.modules.approval;

import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalFlow;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.approval.service.ApprovalNotifyService;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlowVersionApprovalService 测试")
class FlowVersionApprovalServiceTest {

    @Mock
    private ApprovalFlowMapper flowMapper;
    @Mock
    private ApprovalRecordMapper recordMapper;
    @Mock
    private OpFlowVersionMapper flowVersionMapper;
    @Mock
    private ApprovalEngine approvalEngine;
    @Mock
    private ApprovalNotifyService approvalNotifyService;
    @Mock
    private IdGeneratorStrategy idGenerator;

    @InjectMocks
    private FlowVersionApprovalService service;

    private FlowVersion version;
    private final Long appId = 1L;
    private final Long flowId = 100L;
    private final Long flowVersionId = 200L;

    @BeforeEach
    void setUp() {
        version = new FlowVersion();
        version.setId(flowVersionId);
        version.setFlowId(flowId);
        version.setVersionNumber(1);
        version.setStatus(FlowVersionStatus.DRAFT.getCode());

        lenient().when(idGenerator.nextId()).thenReturn(1000L);
    }

    // ===== submitApproval =====

    @Test
    @DisplayName("提交审批 → 三级依次通过路径")
    void testSubmitApproval_Normal() {
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(version);
        ApprovalFlow approvalFlow = new ApprovalFlow();
        approvalFlow.setId(1L);
        approvalFlow.setNodes("[{\"userId\":\"approver1\",\"userName\":\"Approver One\"}]");
        when(flowMapper.selectByCodeAndAppId(eq("connector_flow_version_publish"), eq(appId)))
                .thenReturn(null); // no app-level
        when(flowMapper.selectByCodeAndAppId(eq("connector_flow_version_publish"), eq((Long) null)))
                .thenReturn(approvalFlow); // platform-level
        when(flowMapper.selectByCodeAndAppId(eq("global"), eq((Long) null)))
                .thenReturn(null);

        List<ApprovalNodeDto> nodes = new ArrayList<>();
        ApprovalNodeDto node = new ApprovalNodeDto();
        node.setUserId("approver1");
        node.setUserName("Approver One");
        nodes.add(node);

        when(approvalEngine.parseNodes(anyString())).thenReturn(nodes);
        when(approvalEngine.serializeNodes(anyList())).thenReturn("[{\"userId\":\"approver1\",\"userName\":\"Approver One\",\"order\":1,\"level\":\"platform\"}]");

        ApprovalRecord result = service.submitApproval(flowVersionId, flowId,
                "测试流", "test_flow", appId, "user1", "User One");

        assertNotNull(result);
        assertEquals(flowVersionId, result.getBusinessId());
        verify(flowVersionMapper).update(version);
        verify(recordMapper).insert(any(ApprovalRecord.class));
    }

    @Test
    @DisplayName("提交审批 → 版本不存在 → 抛出 BusinessException")
    void testSubmitApproval_VersionNotFound() {
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                service.submitApproval(flowVersionId, flowId, "流", "flow", appId, "u1", "U"));
    }

    @Test
    @DisplayName("提交审批 → 版本非草稿状态 → 拒绝")
    void testSubmitApproval_NotDraft() {
        version.setStatus(FlowVersionStatus.PUBLISHED.getCode());
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(version);

        assertThrows(BusinessException.class, () ->
                service.submitApproval(flowVersionId, flowId, "流", "flow", appId, "u1", "U"));
    }

    // ===== cancelApproval =====

    @Test
    @DisplayName("撤回审批 → 待审批状态撤回成功")
    void testCancelApproval_Normal() {
        version.setStatus(FlowVersionStatus.PENDING_APPROVAL.getCode());
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(version);

        ApprovalRecord record = new ApprovalRecord();
        record.setId(1000L);
        record.setStatus(ApprovalEngine.Status.PENDING);
        when(recordMapper.selectByBusiness(anyString(), eq(flowVersionId))).thenReturn(record);

        service.cancelApproval(flowVersionId, appId, "user1");

        verify(approvalEngine).cancel(1000L, "user1");
        verify(flowVersionMapper).update(version);
    }

    @Test
    @DisplayName("撤回审批 → 非待审批状态 → 拒绝")
    void testCancelApproval_NotPending() {
        version.setStatus(FlowVersionStatus.PUBLISHED.getCode());
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(version);

        assertThrows(BusinessException.class, () ->
                service.cancelApproval(flowVersionId, appId, "user1"));
    }

    // ===== urgeApproval =====

    @Test
    @DisplayName("催办审批 → 正常催办成功")
    void testUrgeApproval_Normal() {
        ApprovalRecord record = new ApprovalRecord();
        record.setId(1000L);
        record.setApplicantId("user1");
        record.setBusinessType("connector_flow_version_publish");
        record.setBusinessId(flowVersionId);
        record.setApplicantName("User One");
        record.setStatus(ApprovalEngine.Status.PENDING);
        record.setCurrentNode(0);
        record.setCombinedNodes("[{\"userId\":\"approver1\",\"userName\":\"Approver One\"}]");

        when(recordMapper.selectLatestPendingByBusiness(anyString(), eq(flowVersionId)))
                .thenReturn(record);

        List<ApprovalNodeDto> nodes = new ArrayList<>();
        ApprovalNodeDto node = new ApprovalNodeDto();
        node.setUserId("approver1");
        node.setUserName("Approver One");
        nodes.add(node);
        when(approvalEngine.parseNodes(anyString())).thenReturn(nodes);
        when(approvalNotifyService.sendUrgeCard(anyString(), anyString(), anyLong(), anyString(), anyLong(), anyString()))
                .thenReturn("card-001");
        when(approvalEngine.serializeNodes(anyList()))
                .thenReturn("[{\"userId\":\"approver1\",\"userName\":\"Approver One\",\"cardIds\":[\"card-001\"]}]");

        service.urgeApproval(flowVersionId, appId, "user1");

        verify(approvalNotifyService).sendUrgeCard(
                eq("approver1"), eq("Approver One"), eq(1000L),
                eq("connector_flow_version_publish"), eq(flowVersionId), eq("User One"));
    }
}
