package com.xxx.it.works.wecode.v2.modules.ability.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 能力属性表实体
 *
 * <p>对应表 openplatform_ability_p_t</p>
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
     * 状态：0=禁用, 1=启用（参见 StatusEnum）
     */
    private Integer status;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdateTime;
}
