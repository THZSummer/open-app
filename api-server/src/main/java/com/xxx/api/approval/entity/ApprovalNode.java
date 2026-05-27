package com.xxx.api.approval.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 审批节点 DTO
 *
 * <p>combinedNodes JSON 数组中每个元素的反序列化对象</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点类型（如 "approver"）
     */
    private String type;

    /**
     * 审批人 ID
     */
    private String userId;

    /**
     * 审批人姓名
     */
    private String userName;

    /**
     * 顺序（1-based）
     */
    private Integer order;

    /**
     * 审批级别：resource / scene / global
     */
    private String level;

    /**
     * 状态：0=待审, 1=已通过, 2=已驳回
     */
    private Integer status;

    /**
     * 审批时间
     */
    private Date approveTime;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 催办卡片 ID 列表
     */
    private List<String> cardIds;
}
