package com.xxx.it.works.wecode.v2.modules.approval.handler;

import com.xxx.it.works.wecode.v2.modules.approval.constant.ApprovalConstants;
import com.xxx.it.works.wecode.v2.modules.approval.constant.AppVersionStatusEnum;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.AppVersionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 应用版本上架审批处理器
 *
 * <p>处理 app_version_publish 类型的审批通过/驳回回调，更新版本状态</p>
 */
@Slf4j
@Component
public class AppVersionPublishHandler implements ApprovalHandler {

    @Autowired
    private AppVersionMapper appVersionMapper;

    @Override
    public String getBusinessType() {
        return ApprovalConstants.BUSINESS_TYPE_APP_VERSION_PUBLISH;
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        log.info("App version publish approved, businessId={}", record.getBusinessId());
        Long versionId = Long.parseLong(record.getBusinessId());
        appVersionMapper.updateStatus(versionId, AppVersionStatusEnum.APPROVED.getValue());
        log.info("Version status updated to APPROVED(4), versionId={}", versionId);
    }

    @Override
    public void onRejected(ApprovalRecord record) {
        log.info("App version publish rejected, businessId={}", record.getBusinessId());
        Long versionId = Long.parseLong(record.getBusinessId());
        appVersionMapper.updateStatus(versionId, AppVersionStatusEnum.REJECTED.getValue());
        log.info("Version status updated to REJECTED(3), versionId={}", versionId);
    }
}
