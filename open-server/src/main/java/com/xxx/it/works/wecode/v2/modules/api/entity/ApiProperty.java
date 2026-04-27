package com.xxx.it.works.wecode.v2.modules.api.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * API 资源属性实体
 * 
 * <p>对应表 openplatform_v2_api_p_t</p>
 * <p>采用 KV 模式存储扩展属性</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApiProperty implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 属性ID
     */
    private Long id;

    /**
     * 关联 API 主表 ID
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
