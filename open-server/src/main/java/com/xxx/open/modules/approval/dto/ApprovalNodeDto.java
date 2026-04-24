package com.xxx.open.modules.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

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
     * 审批级别：global=全局, scene=场景, resource=资源
     * 
     * v2.8.0新增字段，用于标记审批节点属于哪一级审批
     * 
     * 三级审批顺序（从具体到一般）：
     * 1. resource - 资源审批（资源提供方审核）
     * 2. scene - 场景审批（业务场景审核）
     * 3. global - 全局审批（平台运营审核）
     */
    private String level;

    /**
     * 节点状态（用于审批详情）
     * 0=待审批, 1=已通过, 2=已拒绝
     */
    private Integer status;

    /**
     * 审批时间（v2.8.1新增）
     * 
     * 记录该节点的审批时间，用于前端显示审批进度
     */
    private Date approveTime;

    /**
     * 审批意见（v2.8.1新增）
     * 
     * 记录该节点的审批意见，用于前端显示审批详情
     */
    private String comment;
}
