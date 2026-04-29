package com.xxx.it.works.wecode.v2.modules.approval.dto;

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

    // ✅ v2.8.0 变更：移除 flowId 字段
    // 原因：审批记录直接存储 combinedNodes，不再关联审批流程表
    // 审批节点从 combinedNodes 解析，数据完全独立

    /**
     * 组合审批节点列表（v2.8.0新增）
     *
     * 包含完整的三级审批节点信息：
     * - 资源审批节点（level='resource')
     * - 场景审批节点（level='scene')
     * - 全局审批节点（level='global')
     */
    private List<ApprovalNodeDto> combinedNodes;

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
