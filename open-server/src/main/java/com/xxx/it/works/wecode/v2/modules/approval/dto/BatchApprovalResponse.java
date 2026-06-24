package com.xxx.it.works.wecode.v2.modules.approval.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量审批响应
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
public class BatchApprovalResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failedCount;

    /**
     * 失败详情列表
     */
    private List<FailedItem> failedItems;

    /**
     * 提示消息
     */
    private String message;


    public BatchApprovalResponse() {
    }

    public BatchApprovalResponse(Integer successCount, Integer failedCount, List<FailedItem> failedItems, String message) {
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.failedItems = failedItems;
        this.message = message;
    }
    /**
     * 失败项
     */
    @Data
    @Builder
        public static class FailedItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 审批单ID
         */
        private String approvalId;

        /**
         * 失败原因
         */
        private String reason;
    }
}
