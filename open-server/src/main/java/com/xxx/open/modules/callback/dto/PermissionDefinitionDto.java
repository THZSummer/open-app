package com.xxx.open.modules.callback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 权限定义 DTO（用于创建请求）
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDefinitionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限中文名称（必填）
     */
    private String nameCn;

    /**
     * 权限英文名称（必填）
     */
    private String nameEn;

    /**
     * 权限标识（Scope，必填）
     * 格式：callback:{module}:{identifier}
     */
    private String scope;

    /**
     * 是否需要审批：0=不需要, 1=需要
     * 
     * v2.8.0新增字段，用于标识权限申请是否需要审批流程
     */
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
    private String resourceNodes;
}
