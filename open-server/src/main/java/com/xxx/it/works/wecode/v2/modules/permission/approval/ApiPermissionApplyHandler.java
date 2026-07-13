package com.xxx.it.works.wecode.v2.modules.permission.approval;

import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalBusinessHandler;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApiPermissionApplyHandler implements ApprovalBusinessHandler {

    private final PermissionApplyHandler handler;

    @Override
    public String supportedBusinessType() {
        return ApprovalEngine.BusinessType.API_PERMISSION_APPLY;
    }

    @Override
    public List<ApprovalNodeDto> resolveResourceNodes(Long businessId) {
        return handler.resolveResourceNodes(businessId);
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        handler.onApproved(record);
    }

    @Override
    public void onRejected(ApprovalRecord record) {
        handler.onRejected(record);
    }

    @Override
    public void onCancelled(ApprovalRecord record) {
        handler.onCancelled(record);
    }

    @Override
    public Map<String, Object> getBusinessData(Long businessId) {
        return handler.getBusinessData(businessId);
    }
}
