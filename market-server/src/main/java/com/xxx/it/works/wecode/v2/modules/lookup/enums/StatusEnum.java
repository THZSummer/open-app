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
     * 根据code获取枚举，找不到返回ACTIVE(有效)
     *
     * @param code 状态码
     * @return 状态枚举
     */
    public static StatusEnum of(Integer code) {
        if (code == null) {
            return ACTIVE;
        }
        for (StatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return ACTIVE;
    }

    /**
     * 根据code获取名称，找不到返回"有效"
     *
     * @param code 状态码
     * @return 状态名称
     */
    public static String getNameByCode(Integer code) {
        StatusEnum status = of(code);
        return status == null ? "有效" : status.getName();
    }

    /**
     * 根据文本或数字字符串获取枚举（导入时用）
     * 匹配"有效"/"1" → ACTIVE, "失效"/"0" → INACTIVE
     * 未知值默认返回ACTIVE(有效)
     *
     * @param text 文本或数字字符串
     * @return 状态枚举
     */
    public static StatusEnum fromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return ACTIVE;
        }
        String trimmed = text.trim();
        for (StatusEnum status : values()) {
            if (status.name.equals(trimmed) || String.valueOf(status.code).equals(trimmed)) {
                return status;
            }
        }
        return ACTIVE; // 未知值默认有效
    }
}