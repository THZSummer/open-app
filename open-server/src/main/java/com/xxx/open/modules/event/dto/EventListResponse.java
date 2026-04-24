package com.xxx.open.modules.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 事件列表响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "事件列表响应")
public class EventListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    @Schema(description = "事件ID")
    private String id;

    /**
     * 中文名称
     */
    @Schema(description = "中文名称")
    private String nameCn;

    /**
     * 英文名称
     */
    @Schema(description = "英文名称")
    private String nameEn;

    /**
     * 事件 Topic
     */
    @Schema(description = "事件 Topic")
    private String topic;

    /**
     * 所属分类ID
     */
    @Schema(description = "所属分类ID")
    private String categoryId;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    private String categoryName;

    /**
     * 状态：0=草稿, 1=待审, 2=已发布, 3=已下线
     */
    @Schema(description = "状态：0=草稿, 1=待审, 2=已发布, 3=已下线")
    private Integer status;

    /**
     * 权限信息（简化）
     */
    @Schema(description = "权限信息")
    private PermissionSimpleDto permission;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private java.util.Date createTime;

    /**
     * 文档链接（从properties中提取）
     */
    @Schema(description = "文档链接")
    private String docUrl;

    /**
     * 权限简化 DTO（列表时使用）
     */
    @Data
    @Schema(description = "权限简化信息")
    public static class PermissionSimpleDto implements Serializable {
        
        private static final long serialVersionUID = 1L;

        @Schema(description = "权限ID")
        private String id;

        @Schema(description = "权限标识")
        private String scope;

        @Schema(description = "权限状态")
        private Integer status;
    }
}
