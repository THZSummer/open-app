package com.xxx.it.works.wecode.v2.modules.approval;

import com.xxx.it.works.wecode.v2.common.enums.ConnectorPlatformConstants;
import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 审批结果回调处理器
 *
 * <p>处理连接流版本发布审批的结果回调（FR-031~FR-033）。
 * 审批通过后发布版本，审批驳回后记录驳回原因。</p>
 *
 * <p>该处理器处理 businessType = "connector_flow_version_publish" 场景：
 * 审批全部通过 → FlowVersion状态变更为已发布(5)
 * 审批驳回 → FlowVersion状态变更为已驳回(4)</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalCallbackHandler {

    private final ApprovalRecordMapper recordMapper;
    private final OpFlowVersionMapper flowVersionMapper;

    /**
     * 审批通过回调
     *
     * <p>当审批全部通过（到达最后一个审批节点且通过）时调用。
     * 从审批记录的businessId中获取flowVersionId，
     * 更新FlowVersion状态为已发布(5)，记录发布时间和发布人。</p>
     *
     * @param approvalRecord 已通过审批的审批记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void onApproved(ApprovalRecord approvalRecord) {
        // 1. 校验业务类型
        if (!ConnectorPlatformConstants.APPROVAL_BUSINESS_TYPE_FLOW_VERSION_PUBLISH
                .equals(approvalRecord.getBusinessType())) {
            log.debug("Ignoring non-connector_flow_version_publish approval: businessType={}",
                      approvalRecord.getBusinessType());
            return;
        }

        // 2. 获取flowVersionId
        Long flowVersionId = approvalRecord.getBusinessId();
        if (flowVersionId == null) {
            log.error("businessId is null in approval record: recordId={}", approvalRecord.getId());
            throw BusinessException.internalError("审批记录中业务ID为空",
                    "businessId is null in approval record");
        }

        // 3. 查询FlowVersion
        FlowVersion version = flowVersionMapper.selectById(flowVersionId);
        if (version == null) {
            log.error("FlowVersion not found: flowVersionId={}, recordId={}",
                      flowVersionId, approvalRecord.getId());
            throw BusinessException.notFound("版本不存在: " + flowVersionId,
                    "FlowVersion not found: " + flowVersionId);
        }

        // 4. 更新FlowVersion状态为已发布
        version.setStatus(FlowVersionStatus.PUBLISHED.getCode());
        version.setPublishedTime(new Date());
        version.setPublishedBy(approvalRecord.getApplicantId());
        version.setLastUpdateTime(new Date());
        version.setLastUpdateBy(approvalRecord.getApplicantId());
        flowVersionMapper.update(version);

        log.info("Flow version approved and published: flowVersionId={}, recordId={}, publishedBy={}",
                 flowVersionId, approvalRecord.getId(), approvalRecord.getApplicantId());
    }

    /**
     * 审批驳回回调
     *
     * <p>当任一级别审批人驳回时调用。
     * 更新FlowVersion状态为已驳回(4)，记录驳回原因到日志。</p>
     *
     * @param approvalRecord 被驳回的审批记录
     * @param rejectReason   驳回原因（审批意见）
     */
    @Transactional(rollbackFor = Exception.class)
    public void onRejected(ApprovalRecord approvalRecord, String rejectReason) {
        // 1. 校验业务类型
        if (!ConnectorPlatformConstants.APPROVAL_BUSINESS_TYPE_FLOW_VERSION_PUBLISH
                .equals(approvalRecord.getBusinessType())) {
            log.debug("Ignoring non-connector_flow_version_publish approval: businessType={}",
                      approvalRecord.getBusinessType());
            return;
        }

        // 2. 获取flowVersionId
        Long flowVersionId = approvalRecord.getBusinessId();
        if (flowVersionId == null) {
            log.error("businessId is null in approval record: recordId={}", approvalRecord.getId());
            throw BusinessException.internalError("审批记录中业务ID为空",
                    "businessId is null in approval record");
        }

        // 3. 查询FlowVersion
        FlowVersion version = flowVersionMapper.selectById(flowVersionId);
        if (version == null) {
            log.error("FlowVersion not found: flowVersionId={}, recordId={}",
                      flowVersionId, approvalRecord.getId());
            throw BusinessException.notFound("版本不存在: " + flowVersionId,
                    "FlowVersion not found: " + flowVersionId);
        }

        // 4. 更新FlowVersion状态为已驳回
        version.setStatus(FlowVersionStatus.REJECTED.getCode());
        version.setLastUpdateTime(new Date());
        version.setLastUpdateBy(approvalRecord.getApplicantId());
        flowVersionMapper.update(version);

        log.info("Flow version approval rejected: flowVersionId={}, recordId={}, rejectReason={}",
                 flowVersionId, approvalRecord.getId(), rejectReason);
    }
}
