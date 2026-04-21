package com.xxx.open.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 审批操作请求（同意/驳回）
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalActionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审批意见（同意时可选）
     */
    private String comment;

    /**
     * 驳回原因（驳回时必填）
     */
    private String reason;
}
