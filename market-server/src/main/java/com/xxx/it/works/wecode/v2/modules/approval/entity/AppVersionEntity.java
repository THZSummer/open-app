package com.xxx.it.works.wecode.v2.modules.approval.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用版本实体（对应表 openplatform_app_version_t）
 */
@Data
public class AppVersionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long appId;

    private String versionDescCn;

    private String versionDescEn;

    private String versionCode;

    private String tenantId;

    private Integer status;

    private String createBy;

    private Date createTime;

    private String lastUpdateBy;

    private Date lastUpdateTime;
}
