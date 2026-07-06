package com.xxx.it.works.wecode.v2.modules.approvalflow.dto;

import lombok.Data;

import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;

import java.io.Serializable;
import java.util.List;

/**
 * 审批流程列表响应
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalFlowListResponse implements Serializable {

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
     * String 格式返回，避免前端 Long 精度丢失
     */
    private String appId;

    // ✅ v2.8.0 变更：移除 isDefault 字段
    // 原因：用 code='global' 标识全局审批，更语义化且统一规范
    // 查询全局审批流程：WHERE code='global'

    /**
     * 状态
     */
    private Integer status;

    /**
     * 审批节点列表（用于显示节点数）
     */
    private List<ApprovalNodeDto> nodes;
}
