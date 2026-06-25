package com.xxx.it.works.wecode.v2.modules.card.client;

import lombok.Data;

import java.io.Serializable;

/**
 * 卡片服务错误体
 *
 * <p>卡片服务返回 {@code status=0} 时携带的错误信息。</p>
 *
 * <p>字段示例：</p>
 * <pre>
 * {
 *   "code": 400100,
 *   "userMessageZh": "租户ID或应用ID为空",
 *   "userMessageEn": "tenantId or clientId is empty"
 * }
 * </pre>
 *
 * @author Spec Agent
 * @version 1.0.0
 * @see CardServiceResponse
 */
@Data
public class CardServiceError implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码（卡片服务定义）
     */
    private Integer code;

    /**
     * 中文错误消息（卡片服务定义，可直接面向用户）
     */
    private String userMessageZh;

    /**
     * 英文错误消息（卡片服务定义，可直接面向用户）
     */
    private String userMessageEn;
}
