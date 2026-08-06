package com.xxx.it.works.wecode.v2.modules.sdk.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SDK版本状态枚举
 */
@Getter
@AllArgsConstructor
public enum SdkVersionStatusEnum {

    PUBLISHED(1, "已发布"),
    DEPRECATED(2, "已废弃");

    private final int value;
    private final String description;

    public static SdkVersionStatusEnum fromValue(int value) {
        for (SdkVersionStatusEnum status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }
}
