package com.xxx.it.works.wecode.v2.modules.ability.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 能力主表实体
 *
 * <p>对应表 openplatform_ability_t</p>
 */
@Data
public class Ability implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 能力中文名
     */
    private String abilityNameCn;

    /**
     * 能力英文名
     */
    private String abilityNameEn;

    /**
     * 能力中文描述
     */
    private String abilityDescCn;

    /**
     * 能力英文描述
     */
    private String abilityDescEn;

    /**
     * 能力类型（参见 AbilityTypeEnum）
     */
    private Integer abilityType;

    /**
     * 排序号
     */
    private Integer orderNum;

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
