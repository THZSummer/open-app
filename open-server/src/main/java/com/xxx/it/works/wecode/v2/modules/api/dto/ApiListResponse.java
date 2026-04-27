package com.xxx.it.works.wecode.v2.modules.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * API 列表响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 列表项")
public class ApiListResponse implements Serializable {

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
     * 认证方式：0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN
     */
    @Schema(description = "认证方式")
    private Integer authType;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    private String categoryId;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    private String categoryName;

    /**
     * 状态：0=草稿, 1=待审, 2=已发布, 3=已下线
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 权限信息
     */
    @Schema(description = "权限信息")
    private PermissionDto permission;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 文档链接（从properties中提取）
     */
    @Schema(description = "文档链接")
    private String docUrl;
}
