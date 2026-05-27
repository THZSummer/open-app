package com.xxx.api.approval.handler;

import com.xxx.api.approval.entity.ApprovalRecord;
import com.xxx.api.common.entity.Subscription;
import com.xxx.api.common.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 回调权限订阅审批回调处理器
 *
 * <p>处理 businessType = "callback_permission_apply" 的审批通过后业务副作用：
 * 更新订阅状态（已授权/已拒绝）</p>
 *
 * <p>预留扩展点：回调审批通过后可初始化通道配置等回调特有逻辑</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CallbackPermissionApplyHandler implements ApprovalCallbackHandler {

    private final SubscriptionMapper subscriptionMapper;

    @Override
    public String getBusinessType() {
        return "callback_permission_apply";
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        updateSubscriptionStatus(record, 1, new Date());
        // TODO: 回调审批通过后扩展逻辑（如初始化通道配置等）
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
        log.info("Callback permission subscription {} : subscriptionId={}, status={}",
                status == 1 ? "approved" : "rejected", subscription.getId(), status);
    }
}
