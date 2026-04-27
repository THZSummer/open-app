package com.xxx.it.works.wecode.v2.modules.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 权限申请响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionSubscribeResponse implements Serializable {

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
     * 订阅记录列表
     */
    private List<SubscriptionRecord> records;

    /**
     * 失败项列表
     */
    private List<FailedRecord> failedRecords;

    /**
     * 订阅记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionRecord implements Serializable {
        
        private static final long serialVersionUID = 1L;

        /**
         * 订阅ID
         */
        private String id;

        /**
         * 应用ID
         */
        private String appId;

        /**
         * 权限ID
         */
        private String permissionId;

        /**
         * 状态：0=待审, 1=已授权, 2=已拒绝, 3=已取消
         */
        private Integer status;
    }

    /**
     * 失败记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedRecord implements Serializable {
        
        private static final long serialVersionUID = 1L;

        /**
         * 权限ID
         */
        private String permissionId;

        /**
         * 失败原因
         */
        private String reason;
    }
}
