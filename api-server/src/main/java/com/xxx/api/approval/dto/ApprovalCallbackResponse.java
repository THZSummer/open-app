package com.xxx.api.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 审批卡片回调响应
 *
 * <p>自定义响应格式（非 ApiResponse），适配 IM 平台回调要求。
 * 构造逻辑由人工实现。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCallbackResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     */
    private Integer status;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 错误信息（成功时为 null）
     */
    private ErrorInfo errorInfo;

    // TODO: 工厂方法由人工实现构造逻辑
    // public static <T> ApprovalCallbackResponse<T> success(T data) { ... }
    // public static ApprovalCallbackResponse<?> error(int status, int code, String msgZh, String msgEn) { ... }

    /**
     * 错误信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 错误码
         */
        private Integer code;

        /**
         * 中文错误描述
         */
        private String userMessageZh;

        /**
         * 英文错误描述
         */
        private String userMessageEn;
    }
}
