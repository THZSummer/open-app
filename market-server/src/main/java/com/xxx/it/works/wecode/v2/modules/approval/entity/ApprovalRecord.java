package com.xxx.it.works.wecode.v2.modules.approval.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * JSON格式的审批节点信息
     */
    private String combinedNodes;

    private String businessType;

    private String businessId;

    private String applicantId;

    private String applicantName;

    private Integer status;

    private Integer currentNode;

    private Date createTime;

    private Date lastUpdateTime;

    private String createBy;

    private String lastUpdateBy;

    private Date completedAt;
}
