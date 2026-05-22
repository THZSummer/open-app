package com.xxx.it.works.wecode.v2.modules.dictionary.api.dto;

import com.xxx.it.works.wecode.v2.modules.lookup.dto.common.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询数据字典请求DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "查询数据字典请求")
public class DictionaryQueryDTO extends PageDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 编码，模糊匹配
     */
    @Schema(description = "编码，模糊匹配",
            example = "USER_STATUS")
    private String code;

    /**
     * 名称，模糊匹配
     */
    @Schema(description = "名称，模糊匹配",
            example = "用户状态")
    private String name;

    /**
     * 路径，模糊匹配
     */
    @Schema(description = "路径，模糊匹配",
            example = "system")
    private String path;

    /**
     * 语言：1-中文，2-英文
     */
    @Schema(description = "语言：1-中文，2-英文，空-全部",
            allowableValues = {"1", "2"},
            example = "1")
    private Integer language;

    /**
     * 状态：0-失效，1-有效
     */
    @Schema(description = "状态：0-失效，1-有效，空-全部",
            allowableValues = {"0", "1"},
            example = "1")
    private Integer status;
}
