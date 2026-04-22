package com.xxx.open.modules.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * API 创建请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "API 创建请求")
public class ApiCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 中文名称
     */
    @NotBlank(message = "中文名称不能为空")
    @Schema(description = "中文名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nameCn;

    /**
     * 英文名称
     */
    @NotBlank(message = "英文名称不能为空")
    @Schema(description = "英文名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nameEn;

    /**
     * API 路径
     */
    @NotBlank(message = "API 路径不能为空")
    @Schema(description = "API 路径", requiredMode = Schema.RequiredMode.REQUIRED, 
            example = "/api/v1/messages")
    private String path;

    /**
     * HTTP 方法
     */
    @NotBlank(message = "HTTP 方法不能为空")
    @Schema(description = "HTTP 方法", requiredMode = Schema.RequiredMode.REQUIRED, 
            allowableValues = {"GET", "POST", "PUT", "DELETE"})
    private String method;

    /**
     * 所属分类ID
     */
    @NotBlank(message = "所属分类ID不能为空")
    @Schema(description = "所属分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryId;

    /**
     * 权限定义
     */
    @NotNull(message = "权限定义不能为空")
    @Valid
    @Schema(description = "权限定义", requiredMode = Schema.RequiredMode.REQUIRED)
    private PermissionCreateRequest permission;

    /**
     * 扩展属性列表
     */
    @Schema(description = "扩展属性列表")
    private List<PropertyDto> properties;
}
