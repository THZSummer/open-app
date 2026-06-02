package com.xxx.it.works.wecode.v2.modules.lookup.dto.item;

import com.xxx.it.works.wecode.v2.modules.lookup.dto.common.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询LookUp项请求DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "查询LookUp项请求")
public class ItemQueryDTO extends PageDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID，必填",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1")
    private Long classifyId;

    /**
     * 项编码，模糊匹配
     */
    @Schema(description = "项编码，模糊匹配",
            example = "ADMIN")
    private String itemCode;

    /**
     * 项名称，模糊匹配
     */
    @Schema(description = "项名称，模糊匹配",
            example = "管理员")
    private String itemName;

    /**
     * 状态：0-失效，1-有效
     */
    @Schema(description = "状态：0-失效，1-有效，空-全部",
            allowableValues = {"0", "1"},
            example = "1")
    private Integer status;
}
