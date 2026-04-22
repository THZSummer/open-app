package com.xxx.open.modules.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 权限创建请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "权限创建请求")
public class PermissionCreateRequest implements Serializable {

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

    /**
     * Scope 标识，格式：api:{模块}:{资源标识}
     */
    @NotBlank(message = "Scope 不能为空")
    @Pattern(regexp = "^api:[a-z0-9]+:[a-z0-9-]+$", message = "Scope 格式不正确，应为：api:{模块}:{资源标识}")
    @Schema(description = "Scope 标识，格式：api:{模块}:{资源标识}", requiredMode = Schema.RequiredMode.REQUIRED, 
            example = "api:im:send-message")
    private String scope;

    /**
     * 审批流程ID（可选）
     */
    @Schema(description = "审批流程ID")
    private String approvalFlowId;
}
