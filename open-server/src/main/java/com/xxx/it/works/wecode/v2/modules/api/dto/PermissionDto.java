package com.xxx.it.works.wecode.v2.modules.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 权限 DTO
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "权限信息")
public class PermissionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    @Schema(description = "权限ID（string 类型）")
    private String id;

    /**
     * 权限中文名称
     */
    @Schema(description = "权限中文名称")
    private String nameCn;

    /**
     * 权限英文名称
     */
    @Schema(description = "权限英文名称")
    private String nameEn;

    /**
     * Scope 标识
     */
    @Schema(description = "Scope 标识")
    private String scope;

    /**
     * 状态：0=禁用, 1=启用
     */
    @Schema(description = "权限状态")
    private Integer status;

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
}
