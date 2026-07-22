package com.xxx.it.works.wecode.v2.modules.ability.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 能力 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class AbilityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 能力ID
     */
    private String abilityId;

    /**
     * 能力类型
     */
    private Integer abilityType;

    /**
     * 中文名
     */
    private String nameCn;

    /**
     * 英文名
     */
    private String nameEn;

    /**
     * 中文描述
     */
    private String descCn;

    /**
     * 英文描述
     */
    private String descEn;

    /**
     * 图标URL
     */
    private String iconUrl;

    /**
     * 示意图URL
     */
    private String diagramUrl;

    /**
     * 是否已订阅
     */
    private Boolean subscribed;

    /**
     * 排序号
     */
    private Integer orderNum;

    /**
     * 进入地址（微前端子应用入口，QianKun entry）
     */
    private String entryUrl;

    /**
     * 路由路径（QianKun activeRule）
     */
    private String routePath;

    /**
     * 别名（QianKun name）
     */
    private String aliasName;

    /**
     * 是否需要版本发布才生效（0=即时生效，1=需版本发布）
     */
    private Integer requireRelease;

    /**
     * 加载类型（1=路由加载，2=微前端加载）
     */
    private Integer loadType;
}
