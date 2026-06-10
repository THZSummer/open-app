package com.xxx.it.works.wecode.v2.modules.approval.handler;

import com.xxx.it.works.wecode.v2.modules.approval.constant.ApprovalConstants;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 应用版本上架审批处理器
 *
 * <p>处理 app_version_publish 类型的审批通过/驳回回调</p>
 */
@Slf4j
@Component
public class AppVersionPublishHandler implements ApprovalHandler {

    @Override
    public String getBusinessType() {
        return ApprovalConstants.BUSINESS_TYPE_APP_VERSION_PUBLISH;
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        log.info("App version publish approved, businessId={}", record.getBusinessId());
        // TODO: Update openplatform_app_version_t.status = 4 (APPROVED)
        // This requires a mapper for app_version_t - for now just log
    }

    @Override
    public void onRejected(ApprovalRecord record) {
        log.info("App version publish rejected, businessId={}", record.getBusinessId());
        // TODO: Update openplatform_app_version_t.status = 3 (REJECTED)
    }
}
