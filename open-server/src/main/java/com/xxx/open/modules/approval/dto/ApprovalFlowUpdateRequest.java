package com.xxx.open.modules.approval.dto;

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

    /**
     * 是否默认流程
     */
    private Integer isDefault;

    /**
     * 审批节点列表
     */
    private List<ApprovalNodeDto> nodes;
}
