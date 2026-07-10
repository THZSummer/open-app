package com.xxx.it.works.wecode.v2.modules.flowversion.approval;

import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalBusinessHandler;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.flowversion.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flowversion.mapper.OpFlowVersionMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowMapper;
import com.xxx.it.works.wecode.v2.modules.flow.service.FlowCacheEvictor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowVersionPublishHandler implements ApprovalBusinessHandler {

    private final OpFlowVersionMapper flowVersionMapper;

    @Autowired
    private FlowCacheEvictor flowCacheEvictor;

    @Autowired
    private OpFlowMapper flowMapper;

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

        flowCacheEvictor.evictFlowVersion(record.getBusinessId());

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

    @Override
    public Map<String, Object> getBusinessData(Long businessId) {
        Map<String, Object> data = new HashMap<>();
        FlowVersion version = flowVersionMapper.selectById(businessId);
        if (version == null) {
            return data;
        }

        data.put("flowVersionId", version.getId());
        data.put("flowId", version.getFlowId());
        data.put("versionNumber", version.getVersionNumber());
        data.put("status", version.getStatus());

        if (version.getFlowId() != null) {
            Flow flow = flowMapper.selectById(version.getFlowId());
            if (flow != null) {
                String versionSuffix = " (版本" + version.getVersionNumber() + ")";
                data.put("nameCn", flow.getNameCn() + versionSuffix);
                data.put("nameEn", flow.getNameEn() + versionSuffix);
                data.put("flowNameCn", flow.getNameCn());
                data.put("flowNameEn", flow.getNameEn());
                data.put("appId", flow.getAppId());
            }
        }

        return data;
    }
}
