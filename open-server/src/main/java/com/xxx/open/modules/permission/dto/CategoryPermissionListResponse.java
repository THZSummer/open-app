package com.xxx.open.modules.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分类权限列表响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPermissionListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    private String id;

    /**
     * 权限中文名称
     */
    private String nameCn;

    /**
     * 权限英文名称
     */
    private String nameEn;

    /**
     * Scope
     */
    private String scope;

    /**
     * 状态：0=禁用, 1=启用
     */
    private Integer status;

    /**
     * 是否需要审核：0=不需要, 1=需要
     */
    private Integer needApproval;

    /**
     * 是否已订阅：0=未订阅, 1=已订阅
     */
    private Integer isSubscribed;

    /**
     * 资源信息（API/事件/回调）
     */
    private ResourceInfo resource;

    /**
     * 资源信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceInfo implements Serializable {
        
        private static final long serialVersionUID = 1L;

        /**
         * API 路径（仅 API 类型）
         */
        private String path;

        /**
         * HTTP 方法（仅 API 类型）
         */
        private String method;

        /**
         * Topic（仅事件类型）
         */
        private String topic;

        /**
         * 文档URL
         */
        private String docUrl;
    }
}
