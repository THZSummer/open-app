package com.xxx.it.works.wecode.v2.modules.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 事件属性 DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "事件属性")
public class EventPropertyDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 属性名称
     */
    @NotBlank(message = "属性名称不能为空")
    @Schema(description = "属性名称", example = "descriptionCn", requiredMode = Schema.RequiredMode.REQUIRED)
    private String propertyName;

    /**
     * 属性值
     */
    @Schema(description = "属性值", example = "消息接收事件描述")
    private String propertyValue;
}
