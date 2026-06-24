package com.xxx.it.works.wecode.v2.modules.permission.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * API 权限订阅列表响应
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
public class ApiSubscriptionListResponse implements Serializable {

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
     * 审批创建人ID（订阅发起人，用于前端催办按钮权限控制）
     */
    private String applicantId;

    /**
     * 权限信息
     */
    private PermissionInfo permission;

    /**
     * API 信息
     */
    private ApiInfo api;

    /**
     * 分类信息
     */
    private CategoryInfo category;

    /**
     * 审批人信息
     */
    private ApproverInfo approver;

    /**
     * 状态：0=待审, 1=已授权, 2=已拒绝, 3=已取消
     */
    private Integer status;

    /**
     * 认证方式：0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN
     */
    private Integer authType;

    /**
     * 审批URL
     */
    private String approvalUrl;

    /**
     * 创建时间
     */
    private Date createTime;


    public ApiSubscriptionListResponse() {
    }

    public ApiSubscriptionListResponse(String id, String appId, String permissionId, String applicantId, PermissionInfo permission, ApiInfo api, CategoryInfo category, ApproverInfo approver, Integer status, Integer authType, String approvalUrl, Date createTime) {
        this.id = id;
        this.appId = appId;
        this.permissionId = permissionId;
        this.applicantId = applicantId;
        this.permission = permission;
        this.api = api;
        this.category = category;
        this.approver = approver;
        this.status = status;
        this.authType = authType;
        this.approvalUrl = approvalUrl;
        this.createTime = createTime;
    }
    /**
     * 权限信息
     */
    @Data
    @Builder
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
     * API 信息
     */
    @Data
    @Builder
        public static class ApiInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * API 路径
         */
        private String path;

        /**
         * HTTP 方法
         */
        private String method;

        /**
         * 文档URL
         */
        private String docUrl;

        /**
         * 认证方式
         */
        private Integer authType;
    }

    /**
     * 分类信息
     */
    @Data
    @Builder
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

    /**
     * 审批人信息
     */
    @Data
    @Builder
        public static class ApproverInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        private String userId;

        /**
         * 用户名称
         */
        private String userName;
    }
}
