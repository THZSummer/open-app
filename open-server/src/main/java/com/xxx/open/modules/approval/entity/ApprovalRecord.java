package com.xxx.open.modules.approval.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批记录实体
 * 
 * <p>对应表 openplatform_v2_approval_record_t</p>
 * <p>记录具体的审批单据，关联业务对象</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审批记录ID
     */
    private Long id;

    /**
     * 审批流程ID
     */
    private Long flowId;

    /**
     * 业务类型：api_register, event_register, permission_apply
     */
    private String businessType;

    /**
     * 业务对象ID
     */
    private Long businessId;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 申请人名称
     */
    private String applicantName;

    /**
     * 审批状态：0=待审, 1=已通过, 2=已拒绝, 3=已撤销
     */
    private Integer status;

    /**
     * 当前审批节点索引（从 0 开始）
     */
    private Integer currentNode;

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

    /**
     * 完成时间
     */
    private Date completedAt;
}
