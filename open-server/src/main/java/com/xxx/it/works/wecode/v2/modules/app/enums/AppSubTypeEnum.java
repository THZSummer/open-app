package com.xxx.it.works.wecode.v2.modules.app.enums;

import lombok.Getter;

/**
 * 应用子类型枚举
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
public enum AppSubTypeEnum {

    LEGACY_PERSONAL(0, "存量个人应用"),
    PLUGIN(1, "技能"),
    PERSONAL_ASSISTANT(2, "个人助理"),
    BUSINESS_ASSISTANT(3, "业务助理"),
    BUSINESS_STANDARD(4, "业务应用-标准");

    private final int code;
    private final String desc;

    AppSubTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
