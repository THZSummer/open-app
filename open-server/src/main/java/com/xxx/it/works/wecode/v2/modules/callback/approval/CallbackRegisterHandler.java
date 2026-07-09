package com.xxx.it.works.wecode.v2.modules.callback.approval;

import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalBusinessHandler;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.callback.entity.Callback;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackRegisterHandler implements ApprovalBusinessHandler {

    private final CallbackMapper callbackMapper;

    @Override
    public String supportedBusinessType() {
        return ApprovalEngine.BusinessType.CALLBACK_REGISTER;
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        updateCallbackStatus(record.getBusinessId(), 2, record.getApplicantId());
    }

    @Override
    public void onRejected(ApprovalRecord record) {
        updateCallbackStatus(record.getBusinessId(), 0, record.getApplicantId());
    }

    @Override
    public void onCancelled(ApprovalRecord record) {
        updateCallbackStatus(record.getBusinessId(), 0, record.getApplicantId());
    }

    private void updateCallbackStatus(Long callbackId, int status, String operator) {
        Callback callback = callbackMapper.selectById(callbackId);
        if (callback == null) return;
        callback.setStatus(status);
        callback.setLastUpdateTime(new Date());
        callback.setLastUpdateBy(operator);
        callbackMapper.update(callback);
        log.info("Updated Callback status: callbackId={}, status={}", callbackId, status);
    }
}
