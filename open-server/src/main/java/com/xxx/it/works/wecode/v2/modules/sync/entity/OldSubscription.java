package com.xxx.it.works.wecode.v2.modules.sync.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 旧订阅关系实体
 * 对应表：openplatform_app_permission_t
 */
@Data
public class OldSubscription implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long appId;
    private Long permissionId;
    private String tenantId;
    private String permissionType; // 权限类型：0=API, 1=事件
    private Integer status;
    private String createBy;
    private Date createTime;
    private String lastUpdateBy;
    private Date lastUpdateTime;
}