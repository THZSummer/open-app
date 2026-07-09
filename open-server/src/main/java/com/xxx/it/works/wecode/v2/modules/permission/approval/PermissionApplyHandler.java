package com.xxx.it.works.wecode.v2.modules.permission.approval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalBusinessHandler;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;
import com.xxx.it.works.wecode.v2.modules.permission.entity.Subscription;
import com.xxx.it.works.wecode.v2.modules.permission.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionApplyHandler implements ApprovalBusinessHandler {

    private final PermissionMapper permissionMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String supportedBusinessType() {
        return ApprovalEngine.BusinessType.API_PERMISSION_APPLY;
    }

    @Override
    public List<ApprovalNodeDto> resolveResourceNodes(Long permissionId) {
        if (permissionId == null) return Collections.emptyList();
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null || permission.getNeedApproval() == null || permission.getNeedApproval() != 1) {
            return Collections.emptyList();
        }
        String json = permission.getResourceNodes();
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            List<ApprovalNodeDto> nodes = objectMapper.readValue(json, new TypeReference<List<ApprovalNodeDto>>() {});
            return nodes != null ? nodes : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to parse resource_nodes for permissionId={}", permissionId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        updateSubscription(record.getBusinessId(), 1, record.getApplicantId(), new Date());
    }

    @Override
    public void onRejected(ApprovalRecord record) {
        updateSubscription(record.getBusinessId(), 2, record.getApplicantId(), null);
    }

    @Override
    public void onCancelled(ApprovalRecord record) {
        updateSubscription(record.getBusinessId(), 3, record.getApplicantId(), null);
    }

    private void updateSubscription(Long subscriptionId, int status, String operator, Date approvedAt) {
        Subscription subscription = subscriptionMapper.selectById(subscriptionId);
        if (subscription == null) {
            log.warn("Subscription not found: subscriptionId={}", subscriptionId);
            return;
        }
        subscription.setStatus(status);
        subscription.setLastUpdateTime(new Date());
        subscription.setLastUpdateBy(operator);
        if (approvedAt != null) {
            subscription.setApprovedAt(approvedAt);
            subscription.setApprovedBy(operator);
        }
        subscriptionMapper.update(subscription);
        log.info("Updated subscription status: subscriptionId={}, status={}", subscriptionId, status);
    }
}
