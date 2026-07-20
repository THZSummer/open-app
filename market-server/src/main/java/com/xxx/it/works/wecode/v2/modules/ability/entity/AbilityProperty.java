package com.xxx.it.works.wecode.v2.modules.ability.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 能力属性实体（对应表 openplatform_ability_p_t）
 *
 * <p>用于存储能力的扩展属性，如图标 URL、示意图 URL 等。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class AbilityProperty implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 父记录ID（能力ID）
     */
    private Long parentId;

    /**
     * 属性名
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
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;
}
