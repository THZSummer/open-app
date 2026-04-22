package com.xxx.api.common.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 权限实体
 * 
 * <p>对应表 openplatform_v2_permission_t</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nameCn;
    private String nameEn;
    private String scope;
    private String resourceType;
    private Long resourceId;
    private Long categoryId;
    private Integer status;
    private Date createTime;
    private Date lastUpdateTime;
    private String createBy;
    private String lastUpdateBy;
}
