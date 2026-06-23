package com.xxx.it.works.wecode.v2.modules.lookup.dto.classify;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增分类请求DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "新增分类请求")
public class ClassifyCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类编码
     */
    @NotBlank(message = "分类编码不能为空")
    @Size(max = 100, message = "分类编码长度不能超过100字符")
    @Pattern(regexp = "^[a-zA-Z0-9_./*-]+$", message = "分类编码只能包含字母、数字、下划线、点、横杠、斜杠和星号")
    @Schema(description = "分类编码，必填，1-100字符，支持字母、数字、下划线、点、横杠、斜杠、星号",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "USER_TYPE")
    private String classifyCode;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100字符")
    @Schema(description = "分类名称，必填，1-100字符",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "用户类型")
    private String classifyName;

    /**
     * 路径
     */
    @NotBlank(message = "路径不能为空")
    @Size(max = 100, message = "路径长度不能超过100字符")
    @Schema(description = "路径，必填，用于层级归类，1-100字符",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "system/user")
    private String path;

    /**
     * 分类描述
     */
    @Size(max = 4000, message = "分类描述长度不能超过4000字符")
    @Schema(description = "分类描述，0-4000字符",
            example = "用户身份分类")
    private String classifyDesc;
}
