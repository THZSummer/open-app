package com.xxx.it.works.wecode.v2.modules.lookup.dto.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 编辑LookUp项请求DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Schema(description = "编辑LookUp项请求")
public class ItemUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项名称
     */
    @NotBlank(message = "项名称不能为空")
    @Size(max = 100, message = "项名称长度不能超过100字符")
    @Schema(description = "项名称，必填，1-100字符",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "管理员")
    private String itemName;

    /**
     * 项值
     */
    @Size(max = 2000, message = "项值长度不能超过2000字符")
    @Schema(description = "项值，0-2000字符",
            example = "1")
    private String itemValue;

    /**
     * 排序序号
     */
    @Positive(message = "排序序号必须是正整数")
    @Schema(description = "排序序号，正整数",
            example = "1")
    private Integer itemIndex;

    /**
     * 项描述
     */
    @Size(max = 4000, message = "项描述长度不能超过4000字符")
    @Schema(description = "项描述，0-4000字符",
            example = "系统管理员")
    private String itemDesc;

    /**
     * 扩展属性1
     */
    @Schema(description = "扩展属性1",
            example = "super")
    private String itemAttr1;

    /**
     * 扩展属性2
     */
    @Schema(description = "扩展属性2")
    private String itemAttr2;

    /**
     * 扩展属性3
     */
    @Schema(description = "扩展属性3")
    private String itemAttr3;

    /**
     * 扩展属性4
     */
    @Schema(description = "扩展属性4")
    private String itemAttr4;

    /**
     * 扩展属性5
     */
    @Schema(description = "扩展属性5")
    private String itemAttr5;

    /**
     * 扩展属性6
     */
    @Schema(description = "扩展属性6")
    private String itemAttr6;

    /**
     * 状态：0-失效，1-有效
     */
    @Schema(description = "状态：0-失效，1-有效，不传则不修改状态",
            allowableValues = {"0", "1"},
            example = "1")
    private Integer status;
}
