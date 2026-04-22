package com.xxx.open.modules.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 事件权限订阅列表响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSubscriptionListResponse implements Serializable {

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
     * 权限信息
     */
    private PermissionInfo permission;

    /**
     * 事件信息
     */
    private EventInfo event;

    /**
     * 分类信息
     */
    private CategoryInfo category;

    /**
     * 状态：0=待审, 1=已授权, 2=已拒绝, 3=已取消
     */
    private Integer status;

    /**
     * 通道类型：0=内部消息队列, 1=WebHook
     */
    private Integer channelType;

    /**
     * 通道地址
     */
    private String channelAddress;

    /**
     * 认证类型：0=应用类凭证A, 1=应用类凭证B
     */
    private Integer authType;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 权限信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionInfo implements Serializable {
        
        private static final long serialVersionUID = 1L;

        /**
         * 权限名称
         */
        private String nameCn;

        /**
         * Scope
         */
        private String scope;
    }

    /**
     * 事件信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventInfo implements Serializable {
        
        private static final long serialVersionUID = 1L;

        /**
         * Topic
         */
        private String topic;
    }

    /**
     * 分类信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo implements Serializable {
        
        private static final long serialVersionUID = 1L;

        /**
         * 分类ID
         */
        private String id;

        /**
         * 分类名称
         */
        private String nameCn;

        /**
         * 分类路径
         */
        private String path;

        /**
         * 完整分类路径名称数组
         */
        private List<String> categoryPath;
    }
}
