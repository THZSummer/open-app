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
     */
    private String code;

    /**
     * 是否默认流程
     */
    private Integer isDefault;

    /**
     * 审批节点列表
     */
    private List<ApprovalNodeDto> nodes;
}
