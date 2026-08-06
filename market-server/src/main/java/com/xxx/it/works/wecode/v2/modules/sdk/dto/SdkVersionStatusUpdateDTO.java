package com.xxx.it.works.wecode.v2.modules.sdk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * SDK版本状态变更请求 DTO（废弃/恢复）
 */
@Data
@Schema(description = "SDK版本状态变更请求")
public class SdkVersionStatusUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "id不能为空")
    @Schema(description = "版本ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "status不能为空")
    @Schema(description = "目标状态：1=恢复，2=废弃", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Size(min = 5, max = 2000, message = "废弃原因长度需在5-2000字符之间")
    @Schema(description = "废弃原因（废弃时必填，5-2000字符）")
    private String deprecateReason;
}
