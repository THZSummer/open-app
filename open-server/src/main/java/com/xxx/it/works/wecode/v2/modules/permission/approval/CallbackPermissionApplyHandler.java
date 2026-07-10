package com.xxx.it.works.wecode.v2.modules.permission.approval;

import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalBusinessHandler;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 回调权限申请审批处理器，委托 {@link PermissionApplyHandler}。
 */
@Component
@RequiredArgsConstructor
public class CallbackPermissionApplyHandler implements ApprovalBusinessHandler {

    private final PermissionApplyHandler permissionApplyHandler;

    @Override
    public String supportedBusinessType() {
        return ApprovalEngine.BusinessType.CALLBACK_PERMISSION_APPLY;
    }

    @Override
    public List<ApprovalNodeDto> resolveResourceNodes(Long businessId) {
        return permissionApplyHandler.resolveResourceNodes(businessId);
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        permissionApplyHandler.onApproved(record);
    }

    @Override
    public void onRejected(ApprovalRecord record) {
        permissionApplyHandler.onRejected(record);
    }

    @Override
    public void onCancelled(ApprovalRecord record) {
        permissionApplyHandler.onCancelled(record);
    }

    @Override
    public Map<String, Object> getBusinessData(Long businessId) {
        return permissionApplyHandler.getBusinessData(businessId);
    }
}
