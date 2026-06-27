package com.xxx.it.works.wecode.v2.modules.approval;

import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.approval.service.ApprovalNotifyService;
import com.xxx.it.works.wecode.v2.modules.flowversion.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flowversion.mapper.OpFlowVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FlowVersionApprovalService 单元测试
 *
 * <p>v1.2 重写：submitApproval 改为委托 ApprovalEngine.createApproval()，
 * 移除 flowMapper 和 idGenerator 的 mock。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlowVersionApprovalService 测试")
class FlowVersionApprovalServiceTest {

    @Mock
    private ApprovalRecordMapper recordMapper;
    @Mock
    private OpFlowVersionMapper flowVersionMapper;
    @Mock
    private ApprovalEngine approvalEngine;
    @Mock
    private ApprovalNotifyService approvalNotifyService;

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
    }

    // ===== submitApproval =====

    @Test
    @DisplayName("提交审批 → 委托引擎创建审批记录，更新版本状态为待审批")
    void testSubmitApproval_Normal() {
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(version);

        ApprovalRecord expectedRecord = new ApprovalRecord();
        expectedRecord.setId(1000L);
        expectedRecord.setBusinessType(ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH);
        expectedRecord.setBusinessId(flowVersionId);
        expectedRecord.setApplicantId("user1");
        expectedRecord.setApplicantName("User One");
        expectedRecord.setStatus(ApprovalEngine.Status.PENDING);

        // 核心验证：引擎的 createApproval 被正确调用
        when(approvalEngine.createApproval(
                eq(ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH),
                isNull(),          // permissionId — 流版本发布不涉及权限
                eq(flowVersionId), // businessId
                eq("user1"),       // applicantId
                eq("User One"),    // applicantName
                eq("user1"),       // operator
                eq(appId)          // appId
        )).thenReturn(expectedRecord);

        ApprovalRecord result = service.submitApproval(flowVersionId, flowId,
                "测试流", "test_flow", appId, "user1", "User One");

        assertNotNull(result);
        assertEquals(flowVersionId, result.getBusinessId());
        assertEquals(ApprovalEngine.Status.PENDING, result.getStatus());

        // 验证 FlowVersion 状态更新为待审批
        verify(flowVersionMapper).update(version);
        assertEquals(FlowVersionStatus.PENDING_APPROVAL.getCode(), version.getStatus());
    }

    @Test
    @DisplayName("提交审批 → appId 为 null → 引擎以 null appId 调用")
    void testSubmitApproval_NullAppId() {
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(version);

        ApprovalRecord expectedRecord = new ApprovalRecord();
        expectedRecord.setId(1001L);
        when(approvalEngine.createApproval(anyString(), isNull(), anyLong(),
                anyString(), anyString(), anyString(), isNull()))
                .thenReturn(expectedRecord);

        ApprovalRecord result = service.submitApproval(flowVersionId, flowId,
                "测试流", "test_flow", null, "user1", "User One");

        assertNotNull(result);
        // 验证引擎被调用时 appId 为 null
        verify(approvalEngine).createApproval(
                eq(ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH),
                isNull(), eq(flowVersionId), eq("user1"), eq("User One"), eq("user1"),
                isNull());
    }

    @Test
    @DisplayName("提交审批 → 版本不存在 → 抛出 BusinessException")
    void testSubmitApproval_VersionNotFound() {
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                service.submitApproval(flowVersionId, flowId, "流", "flow", appId, "u1", "U"));

        // 版本不存在时不应调用引擎
        verify(approvalEngine, never()).createApproval(anyString(), any(), anyLong(),
                anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("提交审批 → 版本非草稿状态 → 拒绝")
    void testSubmitApproval_NotDraft() {
        version.setStatus(FlowVersionStatus.PUBLISHED.getCode());
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(version);

        assertThrows(BusinessException.class, () ->
                service.submitApproval(flowVersionId, flowId, "流", "flow", appId, "u1", "U"));

        verify(approvalEngine, never()).createApproval(anyString(), any(), anyLong(),
                anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("提交审批 → 引擎抛出异常 → 异常向上传播")
    void testSubmitApproval_EngineThrows() {
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(version);
        when(approvalEngine.createApproval(anyString(), isNull(), anyLong(),
                anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("引擎内部错误"));

        assertThrows(RuntimeException.class, () ->
                service.submitApproval(flowVersionId, flowId, "流", "flow", appId, "u1", "U"));

        // 引擎异常时不应更新 FlowVersion
        verify(flowVersionMapper, never()).update(any());
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
        when(recordMapper.selectByBusiness(
                eq(ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH), eq(flowVersionId)))
                .thenReturn(record);

        service.cancelApproval(flowVersionId, appId, "user1");

        verify(approvalEngine).cancel(1000L, "user1");
        verify(flowVersionMapper).update(version);
        assertEquals(FlowVersionStatus.WITHDRAWN.getCode(), version.getStatus());
    }

    @Test
    @DisplayName("撤回审批 → 非待审批状态 → 拒绝")
    void testCancelApproval_NotPending() {
        version.setStatus(FlowVersionStatus.PUBLISHED.getCode());
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(version);

        assertThrows(BusinessException.class, () ->
                service.cancelApproval(flowVersionId, appId, "user1"));

        verify(approvalEngine, never()).cancel(anyLong(), anyString());
    }

    @Test
    @DisplayName("撤回审批 → 版本不存在 → 拒绝")
    void testCancelApproval_VersionNotFound() {
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                service.cancelApproval(flowVersionId, appId, "user1"));
    }

}
