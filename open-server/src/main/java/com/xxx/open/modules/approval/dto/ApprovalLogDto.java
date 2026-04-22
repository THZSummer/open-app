package com.xxx.open.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批日志 DTO
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalLogDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点顺序
     */
    private Integer order;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 操作类型：0=同意, 1=拒绝, 2=撤销
     */
    private Integer action;

    /**
     * 操作类型名称
     */
    private String actionName;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 操作时间
     */
    private Date createTime;
}
