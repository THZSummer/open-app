package com.xxx.it.works.wecode.v2.modules.lookup.enums;

import lombok.Getter;

/**
 * 状态枚举
 *
 * <p>用于分类和LookUp项的状态标识</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
public enum StatusEnum {

    /**
     * 失效
     */
    INACTIVE(0, "失效"),

    /**
     * 有效
     */
    ACTIVE(1, "有效");

    private final Integer code;
    private final String name;

    StatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 状态枚举，找不到返回null
     */
    public static StatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (StatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据code获取名称
     *
     * @param code 状态码
     * @return 状态名称，找不到返回空字符串
     */
    public static String getNameByCode(Integer code) {
        StatusEnum status = of(code);
        return status == null ? "" : status.getName();
    }
}
