package com.xxx.it.works.wecode.v2.modules.version.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 版本状态枚举
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum VersionStatusEnum {

    PENDING_RELEASE(1, "待发布"),
    UNDER_REVIEW(2, "审批中"),
    REJECTED(3, "审批未通过"),
    PUBLISHED(4, "已发布");

    private final int code;
    private final String desc;
}
