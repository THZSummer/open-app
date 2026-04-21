package com.xxx.open.modules.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * API 详情响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 详情")
public class ApiDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * API ID
     */
    @Schema(description = "API ID（string 类型）")
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
     * API 路径
     */
    @Schema(description = "API 路径")
    private String path;

    /**
     * HTTP 方法
     */
    @Schema(description = "HTTP 方法")
    private String method;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    private String categoryId;

    /**
     * 状态：0=草稿, 1=待审, 2=已发布, 3=已下线
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createBy;

    /**
     * 权限信息
     */
    @Schema(description = "权限信息")
    private PermissionDto permission;

    /**
     * 扩展属性列表
     */
    @Schema(description = "扩展属性列表")
    private List<PropertyDto> properties;
}
