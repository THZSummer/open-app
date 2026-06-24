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
}
