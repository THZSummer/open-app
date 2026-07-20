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

    /**
     * 进入地址（微前端子应用入口）
     */
    private String entryUrl;

    /**
     * 是否在开放面展示：0=展示, 1=隐藏
     */
    private Integer hidden;

    /**
     * 路由路径（子应用激活路由）
     */
    private String routePath;

    /**
     * 别名（子应用唯一标识）
     */
    private String aliasName;

    /**
     * 是否需要版本发布才生效：0=即时生效, 1=需版本发布
     */
    private Integer requireRelease;

    /**
     * 加载类型：1=路由加载, 2=微前端加载
     */
    private Integer loadType;
}
