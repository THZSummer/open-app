package com.xxx.open.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建审批流程请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalFlowCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 中文名称
     */
    private String nameCn;

    /**
     * 英文名称
     */
    private String nameEn;

    /**
     * 流程编码，全局唯一
     * 
     * v2.8.0变更：通过 code 标识审批类型
     * - code='global' 表示全局审批流程
     * - code='api_permission_apply' 表示场景审批流程
     */
    private String code;

    // ✅ v2.8.0 变更：移除 isDefault 字段
    // 原因：用 code='global' 标识全局审批，无需单独设置 isDefault

    /**
     * 审批节点列表
     */
    private List<ApprovalNodeDto> nodes;
}
