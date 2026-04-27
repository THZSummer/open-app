package com.xxx.it.works.wecode.v2.modules.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 权限更新请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "权限更新请求")
public class PermissionUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限中文名称
     */
    @NotBlank(message = "权限中文名称不能为空")
    @Schema(description = "权限中文名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nameCn;

    /**
     * 权限英文名称
     */
    @NotBlank(message = "权限英文名称不能为空")
    @Schema(description = "权限英文名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nameEn;
}
