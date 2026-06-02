package com.xxx.it.works.wecode.v2.modules.approval.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审批通知服务默认实现
 *
 * <p>当前为预留实现，返回 mock cardId</p>
 * <p>实际对接 IM 系统（钉钉/飞书/企微等）时，替换 sendUrgeCard 方法逻辑</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
public class DefaultApprovalNotifyService implements ApprovalNotifyService {

    @Override
    public String sendUrgeCard(String approverId, String approverName,
                               Long recordId, String businessType, Long businessId,
                               String applicantName) {

        // TODO: 对接实际 IM 系统（钉钉/飞书/企微等）发送卡片消息
        // 实际实现时替换为第三方接口调用，返回真实 cardId
        log.info("[APPROVAL_URGE] Sending urge card: approver={}, approverName={}, recordId={}, " +
                 "businessType={}, businessId={}, applicant={}",
                 approverId, approverName, recordId, businessType, businessId, applicantName);

        return "mock_card_" + System.currentTimeMillis();
    }
}
