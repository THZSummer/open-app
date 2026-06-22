package com.xxx.it.works.wecode.v2.modules.approval;

import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalCallbackHandler 测试")
class ApprovalCallbackHandlerTest {

    @Mock
    private ApprovalRecordMapper recordMapper;
    @Mock
    private OpFlowVersionMapper flowVersionMapper;

    @InjectMocks
    private ApprovalCallbackHandler handler;

    private ApprovalRecord approvalRecord;
    private FlowVersion version;
    private final Long flowVersionId = 200L;

    @BeforeEach
    void setUp() {
        approvalRecord = new ApprovalRecord();
        approvalRecord.setId(1000L);
        approvalRecord.setBusinessType("connector_flow_version_publish");
        approvalRecord.setBusinessId(flowVersionId);
        approvalRecord.setApplicantId("user1");

        version = new FlowVersion();
        version.setId(flowVersionId);
        version.setStatus(FlowVersionStatus.PENDING_APPROVAL.getCode());
    }

    @Test
    @DisplayName("审批通过回调 → FlowVersion 状态变更为已发布(5)")
    void testOnApproved_StatusBecomesPublished() {
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(version);

        handler.onApproved(approvalRecord);

        assertEquals(FlowVersionStatus.PUBLISHED.getCode(), version.getStatus());
        assertNotNull(version.getPublishedTime());
        assertEquals("user1", version.getPublishedBy());
        verify(flowVersionMapper).update(version);
    }

    @Test
    @DisplayName("审批驳回回调 → FlowVersion 状态变更为已驳回(4)")
    void testOnRejected_StatusBecomesRejected() {
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(version);

        handler.onRejected(approvalRecord, "配置不符合要求");

        assertEquals(FlowVersionStatus.REJECTED.getCode(), version.getStatus());
        verify(flowVersionMapper).update(version);
    }

    @Test
    @DisplayName("非连接流版本发布审批 → 忽略")
    void testNonFlowVersionBusinessType_Ignored() {
        approvalRecord.setBusinessType("other_type");

        handler.onApproved(approvalRecord);
        verify(flowVersionMapper, never()).update(any());
    }

    @Test
    @DisplayName("审批通过 → FlowVersion 不存在 → 抛出异常")
    void testOnApproved_VersionNotFound() {
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(null);

        assertThrows(BusinessException.class, () -> handler.onApproved(approvalRecord));
    }

    @Test
    @DisplayName("驳回 → FlowVersion 不存在 → 抛出异常")
    void testOnRejected_VersionNotFound() {
        when(flowVersionMapper.selectById(flowVersionId)).thenReturn(null);

        assertThrows(BusinessException.class, () -> handler.onRejected(approvalRecord, "原因"));
    }

    @Test
    @DisplayName("审批通过 → businessId 为 null → 抛出异常")
    void testOnApproved_BusinessIdNull() {
        approvalRecord.setBusinessId(null);

        assertThrows(BusinessException.class, () -> handler.onApproved(approvalRecord));
    }
}
