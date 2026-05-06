package com.xxx.it.works.wecode.v2.modules.sync.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 旧权限实体
 * 对应表：openplatform_permission_t
 */
@Data
public class OldPermission implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String permissionNameCn;
    private String permissionNameEn;
    private Long moduleId;
    private String scopeId;
    private String permissionType; // 权限类型
    private Integer isApprovalRequired;
    private Integer authType;
    private Integer status;
    private String createBy;
    private Date createTime;
    private String lastUpdateBy;
    private Date lastUpdateTime;
}