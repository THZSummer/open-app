package com.xxx.it.works.wecode.v2.modules.api.approval;

import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalBusinessHandler;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiRegisterHandler implements ApprovalBusinessHandler {

    private final ApiMapper apiMapper;

    @Override
    public String supportedBusinessType() {
        return ApprovalEngine.BusinessType.API_REGISTER;
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        updateApiStatus(record.getBusinessId(), 2, record.getApplicantId());
    }

    @Override
    public void onRejected(ApprovalRecord record) {
        updateApiStatus(record.getBusinessId(), 0, record.getApplicantId());
    }

    @Override
    public void onCancelled(ApprovalRecord record) {
        updateApiStatus(record.getBusinessId(), 0, record.getApplicantId());
    }

    @Override
    public Map<String, Object> getBusinessData(Long businessId) {
        Map<String, Object> data = new HashMap<>();
        Api api = apiMapper.selectById(businessId);
        if (api != null) {
            data.put("nameCn", api.getNameCn());
            data.put("path", api.getPath());
            data.put("method", api.getMethod());
        }
        return data;
    }

    private void updateApiStatus(Long apiId, int status, String operator) {
        Api api = apiMapper.selectById(apiId);
        if (api == null) return;
        api.setStatus(status);
        api.setLastUpdateTime(new Date());
        api.setLastUpdateBy(operator);
        apiMapper.update(api);
        log.info("Updated API status: apiId={}, status={}", apiId, status);
    }
}
