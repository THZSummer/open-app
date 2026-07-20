package com.xxx.it.works.wecode.v2.modules.ability.common;

import lombok.Getter;

/**
 * 能力属性名枚举（对应 openplatform_ability_p_t.property_name）
 *
 * <p>统一管理属性表查询时使用的 property_name 常量，
 * 避免字符串散落在业务代码中。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
public enum AbilityPropertyEnum {
    ICON("icon"),
    EXAMPLE_DIAGRAM("example_diagram");

    private final String propertyName;

    AbilityPropertyEnum(String propertyName) {
        this.propertyName = propertyName;
    }
}
