package com.xxx.open.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量审批请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class BatchApprovalRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审批单ID列表
     */
    private List<String> approvalIds;

    /**
     * 审批意见（同意/驳回时均可填写）
     */
    private String comment;
}
