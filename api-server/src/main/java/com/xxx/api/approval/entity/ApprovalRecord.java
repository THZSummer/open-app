package com.xxx.api.approval.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批记录实体
 *
 * <p>对应表 openplatform_v2_approval_record_t</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 审批节点链 JSON
     */
    private String combinedNodes;

    /**
     * 业务类型（如 api_permission_apply）
     */
    private String businessType;

    /**
     * 业务 ID（如 subscription.id）
     */
    private Long businessId;

    /**
     * 申请人 ID
     */
    private String applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 状态：0=待审, 1=已通过, 2=已驳回, 3=已取消
     */
    private Integer status;

    /**
     * 当前审批节点索引
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
