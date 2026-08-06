package com.xxx.it.works.wecode.v2.modules.sdk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * SDK版本编辑请求 DTO
 *
 * <p>versionName 不可修改，故不暴露此字段</p>
 */
@Data
@Schema(description = "SDK版本编辑请求")
public class SdkVersionUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 2000, message = "更新说明长度不能超过2000字符")
    @Schema(description = "更新说明")
    private String updateNotes;

    @Size(max = 512, message = "构建产物下载地址长度不能超过512字符")
    @Schema(description = "构建产物下载地址")
    private String artifactUrl;

    @Schema(description = "关联权限ID（选填）")
    private Long permissionId;
}
