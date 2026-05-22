package com.xxx.it.works.wecode.v2.modules.dictionary.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增数据字典请求DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "新增数据字典请求")
public class DictionaryCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 编码
     */
    @NotBlank(message = "编码不能为空")
    @Size(max = 100, message = "编码长度不能超过100字符")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "编码只支持字母、数字、下划线、点、横杠")
    @Schema(description = "编码，必填，1-100字符，支持字母、数字、下划线、点、横杠，同一路径下唯一",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "USER_STATUS")
    private String code;

    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100字符")
    @Schema(description = "名称，必填，1-100字符",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "用户状态")
    private String name;

    /**
     * 值
     */
    @Size(max = 2000, message = "值长度不能超过2000字符")
    @Schema(description = "值，0-2000字符",
            example = "active")
    private String value;

    /**
     * 路径
     */
    @Size(max = 100, message = "路径长度不能超过100字符")
    @Schema(description = "路径，0-100字符",
            example = "system/user")
    private String path;

    /**
     * 描述
     */
    @Size(max = 4000, message = "描述长度不能超过4000字符")
    @Schema(description = "描述，0-4000字符",
            example = "用户账户状态字典")
    private String description;

    /**
     * 语言：1-中文，2-英文
     */
    @NotNull(message = "语言不能为空")
    @Schema(description = "语言：1-中文，2-英文，必填，默认1",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"1", "2"},
            example = "1")
    private Integer language;
}
