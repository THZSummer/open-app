package com.xxx.it.works.wecode.v2.modules.approval.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用实体（对应表 openplatform_app_t）
 */
@Data
public class AppEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String appId;

    private String tenantId;

    private String iconId;

    private String appNameCn;

    private String appNameEn;

    private String appDescCn;

    private String appDescEn;

    private Integer appType;

    private Integer appSubType;

    private Integer status;

    private String createBy;

    private Date createTime;

    private String lastUpdateBy;

    private Date lastUpdateTime;
}
