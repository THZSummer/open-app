package com.xxx.it.works.wecode.v2.modules.approval.service;

/**
 * 审批通知服务接口
 *
 * <p>定义审批相关的通知能力，如催办卡片消息发送</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface ApprovalNotifyService {

    /**
     * 发送催办卡片消息给审批人
     *
     * @param approverId    审批人用户ID
     * @param approverName  审批人姓名
     * @param recordId      审批记录ID
     * @param businessType  业务类型（api_register/event_register/...）
     * @param businessId    业务ID
     * @param applicantName 申请人姓名
     * @return 卡片ID（第三方返回）
     */
    String sendUrgeCard(String approverId, String approverName,
                        Long recordId, String businessType, Long businessId,
                        String applicantName);
}
