package com.xxx.it.works.wecode.v2.modules.lookup.dto.classify;

import com.xxx.it.works.wecode.v2.modules.lookup.dto.common.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询分类请求DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "查询分类请求")
public class ClassifyQueryDTO extends PageDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 分类编码，模糊匹配
     */
    @Schema(description = "分类编码，模糊匹配",
            example = "USER")
    private String classifyCode;

    /**
     * 分类名称，模糊匹配
     */
    @Schema(description = "分类名称，模糊匹配",
            example = "用户")
    private String classifyName;

    /**
     * 分类描述，模糊匹配
     */
    @Schema(description = "分类描述，模糊匹配",
            example = "身份")
    private String classifyDesc;

    /**
     * 状态：0-失效，1-有效
     */
    @Schema(description = "状态：0-失效，1-有效，空-全部",
            allowableValues = {"0", "1"},
            example = "1")
    private Integer status;

}
