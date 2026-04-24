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

    /**
     * 审批状态：0=待审, 1=已通过, 2=已拒绝, 3=已撤销
     * 不传则查询所有状态
     */
    private Integer status;

    /**
     * 申请人ID
     * 传 "current" 表示查询当前用户发起的审批
     */
    private String applicantId;

    /**
     * 审批人ID（用于筛选我的待审）
     */
    private String approverId;
}
