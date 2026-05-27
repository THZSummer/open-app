package com.xxx.api.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 审批卡片回调请求
 *
 * <p>IM 平台回调 api-server 审批接口时的请求体</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCallbackRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息卡片 ID
     */
    private String cardId;

    /**
     * 卡片内容（JSON 字符串，反序列化后含 url/type/title/verb/data）
     */
    private String content;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 类型（暂无业务逻辑）
     */
    private String type;

    /**
     * 企业 ID
     */
    private String corpId;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * IM 平台用户标识
     */
    private String userId;

    /**
     * 审批人 ID，对应 combinedNodes 当前节点 userId
     */
    private String accountId;
}
