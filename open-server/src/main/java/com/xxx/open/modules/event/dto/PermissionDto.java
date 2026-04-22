package com.xxx.open.modules.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 权限 DTO（用于创建请求）
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "权限信息")
public class PermissionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID（响应时使用）
     */
    @Schema(description = "权限ID")
    private String id;

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
     * 权限标识
     * 格式：event:{module}:{identifier}
     */
    @NotBlank(message = "Scope 不能为空")
    @Pattern(regexp = "^event:[a-z]+:[a-z0-9-]+$", message = "Scope 格式不正确，应为 event:{module}:{identifier}")
    @Schema(description = "权限标识，格式：event:{module}:{identifier}", example = "event:im:message-received", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scope;

    /**
     * 审批流程ID（可选）
     */
    @Schema(description = "审批流程ID（可选）")
    private String approvalFlowId;

    /**
     * 权限状态（响应时使用）
     */
    @Schema(description = "权限状态")
    private Integer status;
}
