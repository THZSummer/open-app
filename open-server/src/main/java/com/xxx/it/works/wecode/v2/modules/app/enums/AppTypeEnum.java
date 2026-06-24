package com.xxx.it.works.wecode.v2.modules.app.enums;

import lombok.Getter;

/**
 * 应用类型枚举
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
public enum AppTypeEnum {

    PERSONAL(0, "个人应用"),
    BUSINESS(1, "业务应用");

    private final int code;
    private final String desc;

    AppTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
