package com.xxx.it.works.wecode.v2.modules.ability.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 能力实体（对应表 openplatform_ability_t）
 */
@Data
public class AbilityEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String abilityNameCn;

    private String abilityNameEn;

    private String abilityDescCn;

    private String abilityDescEn;

    private Integer abilityType;

    private Integer orderNum;

    private Integer status;

    private String createBy;

    private Date createTime;

    private String lastUpdateBy;

    private Date lastUpdateTime;
}
