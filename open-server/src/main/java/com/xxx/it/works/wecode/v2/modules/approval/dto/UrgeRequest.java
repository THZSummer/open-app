package com.xxx.it.works.wecode.v2.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 催办审批请求
 *
 * <p>申请人催办当前审批人，发送卡片消息通知</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class UrgeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务ID（对应审批记录的 business_id，如 subscription.id）
     */
    private Long businessId;

    /**
     * 业务类型（如 api_permission_apply / event_permission_apply / callback_permission_apply）
     */
    private String businessType;
}
