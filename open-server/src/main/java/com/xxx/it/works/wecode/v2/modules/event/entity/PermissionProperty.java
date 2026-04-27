package com.xxx.it.works.wecode.v2.modules.event.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 权限属性实体
 * 
 * <p>对应表 openplatform_v2_permission_p_t</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class PermissionProperty implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 属性ID
     */
    private Long id;

    /**
     * 父表ID（关联权限主表）
     */
    private Long parentId;

    /**
     * 属性名称
     */
    private String propertyName;

    /**
     * 属性值
     */
    private String propertyValue;

    /**
     * 状态：0=禁用, 1=启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;
}
