package com.xxx.it.works.wecode.v2.modules.permission.approval;

import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ResourceApprovalProvider;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 权限申请场景的资源审批节点提供者
 *
 * <p>从 {@code openplatform_v2_permission_t.resource_nodes} 读取审批节点，
 * 由资源提供方配置。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionResourceApprovalProvider implements ResourceApprovalProvider {

    private final PermissionMapper permissionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String businessType) {
        return businessType != null && businessType.endsWith("_permission_apply");
    }

    @Override
    public List<ApprovalNodeDto> resolve(Long permissionId) {
        if (permissionId == null) return Collections.emptyList();

        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            log.warn("Permission not found: permissionId={}", permissionId);
            return Collections.emptyList();
        }

        if (permission.getNeedApproval() == null || permission.getNeedApproval() != 1) {
            log.debug("Permission does not require approval: permissionId={}", permissionId);
            return Collections.emptyList();
        }

        String json = permission.getResourceNodes();
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            log.debug("Permission resource_nodes is empty: permissionId={}", permissionId);
            return Collections.emptyList();
        }

        try {
            List<ApprovalNodeDto> nodes = objectMapper.readValue(json,
                    new TypeReference<List<ApprovalNodeDto>>() {});
            return nodes != null ? nodes : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to parse resource_nodes for permissionId={}", permissionId, e);
            return Collections.emptyList();
        }
    }
}
