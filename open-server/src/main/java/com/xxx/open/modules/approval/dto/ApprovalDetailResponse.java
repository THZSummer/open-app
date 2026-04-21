package com.xxx.open.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 审批详情响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审批记录ID
     */
    private String id;

    /**
     * 审批类型
     */
    private String type;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务对象ID
     */
    private String businessId;

    /**
     * 业务数据
     */
    private Map<String, Object> businessData;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 申请人名称
     */
    private String applicantName;

    /**
     * 审批状态
     */
    private Integer status;

    /**
     * 审批流程ID
     */
    private String flowId;

    /**
     * 当前节点索引
     */
    private Integer currentNode;

    /**
     * 审批节点列表（含状态）
     */
    private List<ApprovalNodeDto> nodes;

    /**
     * 操作日志列表
     */
    private List<ApprovalLogDto> logs;

    /**
     * 创建时间
     */
    private Date createTime;
}
