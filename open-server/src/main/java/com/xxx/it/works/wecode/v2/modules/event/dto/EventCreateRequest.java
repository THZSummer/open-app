package com.xxx.it.works.wecode.v2.modules.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建事件请求
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "创建事件请求")
public class EventCreateRequest implements Serializable {

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
     * 事件 Topic，全局唯一
     */
    @NotBlank(message = "Topic 不能为空")
    @Schema(description = "事件 Topic，全局唯一", example = "im.message.received", requiredMode = Schema.RequiredMode.REQUIRED)
    private String topic;

    /**
     * 所属分类ID
     */
    @NotBlank(message = "分类ID不能为空")
    @Schema(description = "所属分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryId;

    /**
     * 权限定义
     */
    @NotNull(message = "权限定义不能为空")
    @Valid
    @Schema(description = "权限定义", requiredMode = Schema.RequiredMode.REQUIRED)
    private PermissionDto permission;

    /**
     * 扩展属性列表
     */
    @Schema(description = "扩展属性列表")
    private List<EventPropertyDto> properties;
}
