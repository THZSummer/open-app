package com.xxx.it.works.wecode.v2.modules.permission.approval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.callback.entity.Callback;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper;
import com.xxx.it.works.wecode.v2.modules.event.entity.Event;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.event.mapper.EventMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;
import com.xxx.it.works.wecode.v2.modules.permission.entity.Subscription;
import com.xxx.it.works.wecode.v2.modules.permission.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限申请审批的共享逻辑（不做 Spring Bean，由 Api/Event/Callback 三个 Handler 委托调用）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
class PermissionApplyHandler {

    final PermissionMapper permissionMapper;
    final SubscriptionMapper subscriptionMapper;
    final ObjectMapper objectMapper;
    final ApiMapper apiMapper;
    final EventMapper eventMapper;
    final CallbackMapper callbackMapper;

    List<ApprovalNodeDto> resolveResourceNodes(Long permissionId) {
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

    void onApproved(ApprovalRecord record) {
        updateSubscription(record.getBusinessId(), 1, record.getApplicantId(), new Date());
    }

    void onRejected(ApprovalRecord record) {
        updateSubscription(record.getBusinessId(), 2, record.getApplicantId(), null);
    }

    void onCancelled(ApprovalRecord record) {
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

    Map<String, Object> getBusinessData(Long businessId) {
        Map<String, Object> data = new HashMap<>();
        Subscription subscription = subscriptionMapper.selectById(businessId);
        if (subscription == null) {
            return data;
        }

        data.put("appId", subscription.getAppId());
        data.put("permissionId", subscription.getPermissionId());

        Permission permission = permissionMapper.selectById(subscription.getPermissionId());
        if (permission == null) {
            return data;
        }

        data.put("nameCn", permission.getNameCn());
        data.put("nameEn", permission.getNameEn());
        data.put("scope", permission.getScope());
        data.put("resourceType", permission.getResourceType());

        fillResourceData(data, permission);

        return data;
    }

    private void fillResourceData(Map<String, Object> data, Permission permission) {
        String resourceType = permission.getResourceType();
        Long resourceId = permission.getResourceId();

        if ("api".equals(resourceType)) {
            Api apiResource = apiMapper.selectById(resourceId);
            if (apiResource != null) {
                data.put("path", apiResource.getPath());
                data.put("method", apiResource.getMethod());
            }
        } else if ("event".equals(resourceType)) {
            Event evt = eventMapper.selectById(resourceId);
            if (evt != null) {
                data.put("topic", evt.getTopic());
            }
        } else if ("callback".equals(resourceType)) {
            Callback cb = callbackMapper.selectById(resourceId);
            if (cb != null) {
                data.put("nameCn", cb.getNameCn());
            }
        }
    }
}
