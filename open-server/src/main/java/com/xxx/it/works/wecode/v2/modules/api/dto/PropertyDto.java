package com.xxx.it.works.wecode.v2.modules.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 属性 DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "属性信息")
public class PropertyDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 属性名称
     */
    @Schema(description = "属性名称")
    private String propertyName;

    /**
     * 属性值
     */
    @Schema(description = "属性值")
    private String propertyValue;

    public PropertyDto(String propertyName, String propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }
}
