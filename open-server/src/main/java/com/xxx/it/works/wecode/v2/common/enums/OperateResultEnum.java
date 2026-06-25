package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作结果状态枚举
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum OperateResultEnum {

    FAILED(0, "失败"),
    SUCCESS(1, "成功");

    private final int code;
    private final String desc;
}
