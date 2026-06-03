package com.xxx.api.approval.handler;

import com.xxx.api.approval.entity.ApprovalRecord;
import com.xxx.api.common.entity.Subscription;
import com.xxx.api.common.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * API 权限订阅审批回调处理器
 *
 * <p>处理 businessType = "api_permission_apply" 的审批通过后业务副作用：
 * 更新订阅状态（已授权/已拒绝）</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiPermissionApplyHandler implements ApprovalCallbackHandler {

    private final SubscriptionMapper subscriptionMapper;

    @Override
    public String getBusinessType() {
        return "api_permission_apply";
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        updateSubscriptionStatus(record, 1, new Date());
    }

    @Override
    public void onRejected(ApprovalRecord record) {
        updateSubscriptionStatus(record, 2, null);
    }

    private void updateSubscriptionStatus(ApprovalRecord record, int status, Date approvedAt) {
        Subscription subscription = subscriptionMapper.selectById(record.getBusinessId());
        if (subscription == null) {
            log.warn("Subscription not found, cannot update status: businessId={}", record.getBusinessId());
            return;
        }

        subscription.setStatus(status);
        subscription.setLastUpdateTime(new Date());
        subscription.setLastUpdateBy(record.getApplicantId());

        if (approvedAt != null) {
            subscription.setApprovedAt(approvedAt);
            subscription.setApprovedBy(record.getApplicantId());
        }

        subscriptionMapper.updateApprovalStatus(subscription);
        log.info("API permission subscription {} : subscriptionId={}, status={}",
                status == 1 ? "approved" : "rejected", subscription.getId(), status);
    }
}
