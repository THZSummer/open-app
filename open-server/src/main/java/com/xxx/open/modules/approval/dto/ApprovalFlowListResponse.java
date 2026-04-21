package com.xxx.open.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;

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
     * 是否默认流程
     */
    private Integer isDefault;

    /**
     * 状态
     */
    private Integer status;
}
