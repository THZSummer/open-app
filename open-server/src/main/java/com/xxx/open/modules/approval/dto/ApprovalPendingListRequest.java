package com.xxx.open.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 待审批列表查询请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalPendingListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审批类型
     * resource_register=资源注册, permission_apply=权限申请
     */
    private String type;

    /**
     * 搜索关键词（业务名称、申请人）
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
