package com.xxx.it.works.wecode.v2.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新审批流程请求
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalFlowUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 中文名称
     */
    private String nameCn;

    /**
     * 英文名称
     */
    private String nameEn;

    // ✅ v2.8.0 变更：移除 isDefault 字段
    // 原因：审批流程类型由 code 标识，无法通过更新修改

    /**
     * 审批节点列表
     */
    private List<ApprovalNodeDto> nodes;
}
