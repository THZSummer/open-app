package com.xxx.it.works.wecode.v2.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 审批流程列表查询请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalFlowListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 搜索关键词（名称、编码）
     */
    private String keyword;

    /**
     * 当前页码，从 1 开始
     */
    private Integer curPage = 1;

    /**
     * 每页数量，默认 20
     */
    private Integer pageSize = 20;
}
