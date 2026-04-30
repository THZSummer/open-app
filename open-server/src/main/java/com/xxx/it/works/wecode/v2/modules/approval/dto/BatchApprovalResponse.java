package com.xxx.it.works.wecode.v2.modules.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@NoArgsConstructor
@AllArgsConstructor
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

    /**
     * 失败项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
