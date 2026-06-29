package com.xxx.it.works.wecode.v2.modules.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建版本请求
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class CreateVersionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "版本号不能为空")
    private String versionCode;

    @NotBlank(message = "中文描述不能为空")
    @Size(max = 200, message = "中文描述不能超过200个字符")
    private String versionDescCn;

    @Size(max = 200, message = "英文描述不能超过200个字符")
    private String versionDescEn;
}
