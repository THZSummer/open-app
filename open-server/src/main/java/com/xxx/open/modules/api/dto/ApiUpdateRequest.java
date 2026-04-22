package com.xxx.open.modules.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * API 更新请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "API 更新请求")
public class ApiUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 中文名称（可选，更新时只更新非null字段）
     */
    @Schema(description = "中文名称（可选）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nameCn;

    /**
     * 英文名称（可选，更新时只更新非null字段）
     */
    @Schema(description = "英文名称（可选）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nameEn;

    /**
     * 所属分类ID（可选，更新时只更新非null字段）
     */
    @Schema(description = "所属分类ID（可选）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String categoryId;

    /**
     * 权限定义
     */
    @Valid
    @Schema(description = "权限定义")
    private PermissionUpdateRequest permission;

    /**
     * 扩展属性列表
     */
    @Schema(description = "扩展属性列表")
    private List<PropertyDto> properties;
}
