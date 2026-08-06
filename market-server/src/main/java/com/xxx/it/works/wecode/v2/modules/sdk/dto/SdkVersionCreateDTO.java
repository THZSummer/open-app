package com.xxx.it.works.wecode.v2.modules.sdk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * SDK版本上架请求 DTO
 */
@Data
@Schema(description = "SDK版本上架请求")
public class SdkVersionCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "版本号不能为空")
    @Size(max = 64, message = "版本号长度不能超过64字符")
    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String versionName;

    @Size(max = 2000, message = "更新说明长度不能超过2000字符")
    @Schema(description = "更新说明")
    private String updateNotes;

    @Size(max = 512, message = "构建产物下载地址长度不能超过512字符")
    @Schema(description = "构建产物下载地址")
    private String artifactUrl;

    @Schema(description = "关联权限ID（选填）")
    private Long permissionId;
}
