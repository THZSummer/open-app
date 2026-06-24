package com.xxx.it.works.wecode.v2.modules.version.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 版本关联能力 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-14
 */
@Data
public class AppVersionAbilityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 能力ID
     */
    private String id;

    /**
     * 中文名
     */
    private String abilityNameCn;

    /**
     * 英文名
     */
    private String abilityNameEn;

    /**
     * 图标URL
     */
    private String iconUrl;
}
