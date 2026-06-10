package com.xxx.it.works.wecode.v2.modules.approval.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ApprovalFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String nameCn;

    private String nameEn;

    private String code;

    private String descriptionCn;

    private String descriptionEn;

    /**
     * JSON格式的审批流程节点定义
     */
    private String nodes;

    private Integer status;
}
