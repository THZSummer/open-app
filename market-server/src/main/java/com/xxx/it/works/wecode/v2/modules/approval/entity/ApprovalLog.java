package com.xxx.it.works.wecode.v2.modules.approval.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ApprovalLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long recordId;

    private Integer nodeIndex;

    private String level;

    private String operatorId;

    private String operatorName;

    private Integer action;

    private String comment;

    private Date createTime;
}
