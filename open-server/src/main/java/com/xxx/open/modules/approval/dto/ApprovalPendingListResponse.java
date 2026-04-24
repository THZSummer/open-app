package com.xxx.open.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 待审批列表响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalPendingListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审批记录ID
     */
    private String id;

    /**
     * 业务类型：api, event, callback, permission
     */
    private String businessType;

    /**
     * 业务对象ID
     */
    private String businessId;

    /**
     * 业务名称
     */
    private String businessName;

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
     * 当前节点索引
     */
    private Integer currentNode;

    /**
     * 创建时间
     */
    private Date createTime;
}
