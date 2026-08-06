package com.xxx.it.works.wecode.v2.modules.sdk.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * SDK版本实体（对应表 openplatform_sdk_version_t）
 */
@Data
public class SdkVersionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String versionName;

    private String updateNotes;

    private String artifactUrl;

    private Long permissionId;

    private Integer status;

    private String deprecateReason;

    private String createBy;

    private Date createTime;

    private String lastUpdateBy;

    private Date lastUpdateTime;

    /**
     * 关联权限名称（非表字段，联查 openplatform_v2_permission_t 获得）
     */
    private String permissionName;

    /**
     * 关联权限scope（非表字段，联查 openplatform_v2_permission_t 获得）
     */
    private String permissionScope;
}
