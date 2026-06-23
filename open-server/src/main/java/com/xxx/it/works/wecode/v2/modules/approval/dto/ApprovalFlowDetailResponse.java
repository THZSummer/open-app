package com.xxx.it.works.wecode.v2.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 审批流程详情响应
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalFlowDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程ID
     */
    private String id;

    /**
     * 中文名称
     */
    private String nameCn;

    /**
     * 英文名称
     */
    private String nameEn;

    /**
     * 流程编码
     */
    private String code;

    /**
     * 归属应用ID（V3 新增）
     * NULL=平台级/全局模板，非NULL=应用级定制模板
     */
    private Long appId;

    // ✅ v2.8.0 变更：移除 isDefault 字段
    // 原因：用 code='global' 标识全局审批，更语义化且统一规范

    /**
     * 状态
     */
    private Integer status;

    /**
     * 审批节点列表
     */
    private List<ApprovalNodeDto> nodes;
}
