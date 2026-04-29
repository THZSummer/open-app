package com.xxx.it.works.wecode.v2.modules.event.dto;

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
     * 是否需要审批：0=不需要, 1=需要
     *
     * v2.8.0新增字段，用于标识权限申请是否需要审批流程
     */
    @Schema(description = "是否需要审批：0=不需要, 1=需要")
    private Integer needApproval;

    /**
     * 资源级审批节点配置（JSON格式字符串）
     *
     * v2.8.0新增字段，直接存储审批节点配置
     *
     * 格式示例：
     * [
     *   {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":1},
     *   {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":2}
     * ]
     */
    @Schema(description = "资源级审批节点配置（JSON字符串）")
    private String resourceNodes;

    /**
     * 权限状态（响应时使用）
     */
    @Schema(description = "权限状态")
    private Integer status;
}
