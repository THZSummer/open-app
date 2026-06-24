package com.xxx.it.works.wecode.v2.modules.ability.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 已订阅能力详情 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-14
 */
@Data
public class AppAbilityDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private String id;

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
     * 图标URL
     */
    private String iconUrl;
}
