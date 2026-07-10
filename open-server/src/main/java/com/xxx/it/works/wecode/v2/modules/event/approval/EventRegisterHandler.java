package com.xxx.it.works.wecode.v2.modules.event.approval;

import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalBusinessHandler;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.event.entity.Event;
import com.xxx.it.works.wecode.v2.modules.event.mapper.EventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventRegisterHandler implements ApprovalBusinessHandler {

    private final EventMapper eventMapper;

    @Override
    public String supportedBusinessType() {
        return ApprovalEngine.BusinessType.EVENT_REGISTER;
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        updateEventStatus(record.getBusinessId(), 2, record.getApplicantId());
    }

    @Override
    public void onRejected(ApprovalRecord record) {
        updateEventStatus(record.getBusinessId(), 0, record.getApplicantId());
    }

    @Override
    public void onCancelled(ApprovalRecord record) {
        updateEventStatus(record.getBusinessId(), 0, record.getApplicantId());
    }

    @Override
    public Map<String, Object> getBusinessData(Long businessId) {
        Map<String, Object> data = new HashMap<>();
        Event event = eventMapper.selectById(businessId);
        if (event != null) {
            data.put("nameCn", event.getNameCn());
            data.put("topic", event.getTopic());
        }
        return data;
    }

    private void updateEventStatus(Long eventId, int status, String operator) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) return;
        event.setStatus(status);
        event.setLastUpdateTime(new Date());
        event.setLastUpdateBy(operator);
        eventMapper.update(event);
        log.info("Updated Event status: eventId={}, status={}", eventId, status);
    }
}
