package com.xxx.open.modules.approval.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批流程模板实体
 * 
 * <p>对应表 openplatform_v2_approval_flow_t</p>
 * <p>审批流程模板定义，支持动态节点配置</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程ID
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
     * 流程编码，全局唯一，如 default, api_register, permission_apply
     */
    private String code;

    /**
     * 中文描述
     */
    private String descriptionCn;

    /**
     * 英文描述
     */
    private String descriptionEn;

    /**
     * 是否默认流程：0=否, 1=是
     */
    private Integer isDefault;

    /**
     * 审批节点配置（JSON 格式）
     * 
     * 格式示例：
     * [
     *   {"type": "approver", "userId": "user001", "userName": "张三", "order": 1},
     *   {"type": "approver", "userId": "user002", "userName": "李四", "order": 2}
     * ]
     */
    private String nodes;

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
