package com.xxx.open.modules.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 审批节点 DTO
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalNodeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点类型：approver=审批人
     */
    private String type;

    /**
     * 审批人ID
     */
    private String userId;

    /**
     * 审批人名称
     */
    private String userName;

    /**
     * 节点顺序
     */
    private Integer order;

    /**
     * 节点状态（用于审批详情）
     * 0=待审批, 1=已通过, 2=已拒绝
     */
    private Integer status;
}
