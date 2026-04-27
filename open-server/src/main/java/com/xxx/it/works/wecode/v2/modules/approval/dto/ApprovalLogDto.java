package com.xxx.it.works.wecode.v2.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批日志 DTO
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalLogDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点顺序
     */
    private Integer order;

    /**
     * 审批级别（v2.8.0新增）
     * 
     * 标识本次审批操作属于哪一级审批：
     * - resource = 资源审批（资源提供方审核）
     * - scene = 场景审批（业务场景审核）
     * - global = 全局审批（平台运营审核）
     */
    private String level;

    /**
     * 审批级别名称（v2.8.0新增）
     * 
     * 用于前端显示审批级别的中文名称
     */
    private String levelName;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 操作类型：0=同意, 1=拒绝, 2=撤销
     */
    private Integer action;

    /**
     * 操作类型名称
     */
    private String actionName;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 操作时间
     */
    private Date createTime;
}
