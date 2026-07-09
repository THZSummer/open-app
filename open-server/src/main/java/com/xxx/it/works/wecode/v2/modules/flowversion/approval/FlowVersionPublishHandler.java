package com.xxx.it.works.wecode.v2.modules.flowversion.approval;

import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalBusinessHandler;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.flowversion.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flowversion.mapper.OpFlowVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowVersionPublishHandler implements ApprovalBusinessHandler {

    private final OpFlowVersionMapper flowVersionMapper;

    @Override
    public String supportedBusinessType() {
        return ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH;
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        FlowVersion version = flowVersionMapper.selectById(record.getBusinessId());
        if (version == null) return;
        Date now = new Date();
        version.setStatus(FlowVersionStatus.PUBLISHED.getCode());
        version.setPublishedTime(now);
        version.setPublishedBy(record.getApplicantId());
        version.setLastUpdateTime(now);
        version.setLastUpdateBy(record.getApplicantId());
        flowVersionMapper.update(version);
        log.info("Published flow version: versionId={}", record.getBusinessId());
    }

    @Override
    public void onRejected(ApprovalRecord record) {
        FlowVersion version = flowVersionMapper.selectById(record.getBusinessId());
        if (version == null) return;
        Date now = new Date();
        version.setStatus(FlowVersionStatus.REJECTED.getCode());
        version.setLastUpdateTime(now);
        version.setLastUpdateBy(record.getApplicantId());
        flowVersionMapper.update(version);
        log.info("Rejected flow version: versionId={}", record.getBusinessId());
    }

    @Override
    public void onCancelled(ApprovalRecord record) {
        FlowVersion version = flowVersionMapper.selectById(record.getBusinessId());
        if (version == null) return;
        Date now = new Date();
        version.setStatus(FlowVersionStatus.WITHDRAWN.getCode());
        version.setLastUpdateTime(now);
        version.setLastUpdateBy(record.getApplicantId());
        flowVersionMapper.update(version);
        log.info("Cancelled flow version: versionId={}", record.getBusinessId());
    }
}
