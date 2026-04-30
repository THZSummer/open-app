package com.xxx.it.works.wecode.v2.modules.sync.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 旧API实体
 * 对应表：openplatform_permission_api_t
 */
@Data
public class OldApi implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String apiNameCn;
    private String apiNameEn;
    private Long permissionId;
    private String path;
    private String method;
    private Integer authType;
    private Integer status;
    private String createBy;
    private Date createTime;
    private String lastUpdateBy;
    private Date lastUpdateTime;
}