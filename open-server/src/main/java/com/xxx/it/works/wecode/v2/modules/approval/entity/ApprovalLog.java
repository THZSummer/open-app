package com.xxx.it.works.wecode.v2.modules.approval.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批操作日志实体
 * 
 * <p>对应表 openplatform_v2_approval_log_t</p>
 * <p>记录审批过程中的操作历史</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 审批记录ID
     */
    private Long recordId;

    /**
     * 节点索引
     */
    private Integer nodeIndex;

    /**
     * 审批级别：global=全局, scene=场景, resource=资源
     * 
     * v2.8.0新增字段，用于标记审批操作属于哪一级审批
     */
    private String level;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 操作类型：0=同意, 1=拒绝, 2=撤销, 3=转交
     */
    private Integer action;

    /**
     * 审批意见
     */
    private String comment;

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
