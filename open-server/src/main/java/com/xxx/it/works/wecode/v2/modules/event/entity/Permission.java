package com.xxx.it.works.wecode.v2.modules.event.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 权限实体
 *
 * <p>对应表 openplatform_v2_permission_t</p>
 * <p>权限资源主表，关联 API/事件/回调资源</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 中文名称
     */
    private String nameCn;

    /**
     * 英文名称
     */
    private String nameEn;

    /**
     * 权限标识，如 api:im:send-message, event:im:message-received
     */
    private String scope;

    /**
     * 资源类型：api, event, callback
     */
    private String resourceType;

    /**
     * 关联的资源ID（API/事件/回调 ID）
     */
    private Long resourceId;

    /**
     * 所属分类ID
     */
    private Long categoryId;

    /**
     * 是否需要审批：0=否, 1=是
     *
     * v2.8.0新增字段，用于标识权限是否需要审批流程
     */
    private Integer needApproval;

    /**
     * 资源级审批节点配置（JSON格式字符串）
     *
     * v2.8.0新增字段，直接存储审批节点配置（不关联审批流程表）
     *
     * 格式示例：
     * [
     *   {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":1},
     *   {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":2}
     * ]
     */
    private String resourceNodes;

    /**
     * 状态：0=禁用, 1=启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;
}
