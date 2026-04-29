package com.xxx.it.works.wecode.v2.modules.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新事件请求
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "更新事件请求")
public class EventUpdateRequest implements Serializable {

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
     * 所属分类ID
     */
    @Schema(description = "所属分类ID")
    private String categoryId;

    /**
     * 权限更新信息
     */
    @Valid
    @Schema(description = "权限更新信息")
    private PermissionDto permission;

    /**
     * 扩展属性列表
     */
    @Schema(description = "扩展属性列表")
    private List<EventPropertyDto> properties;
}
