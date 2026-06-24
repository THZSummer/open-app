package com.xxx.it.works.wecode.v2.modules.version.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 版本详情 VO（含关联能力列表）
 *
 * @author SDDU Build Agent
 * @date 2026-06-14
 */
@Data
public class AppVersionDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 版本ID
     */
    private String id;

    /**
     * 版本号
     */
    private String versionCode;

    /**
     * 中文描述
     */
    private String versionDescCn;

    /**
     * 英文描述
     */
    private String versionDescEn;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 关联能力列表
     */
    private List<AppVersionAbilityVO> abilityList;
}
