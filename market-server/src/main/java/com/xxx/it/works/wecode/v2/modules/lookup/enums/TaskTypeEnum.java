package com.xxx.it.works.wecode.v2.modules.lookup.enums;

import lombok.Getter;

/**
 * 任务类型枚举
 *
 * <p>用于标识任务的类型：导入或导出</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
public enum TaskTypeEnum {

    /**
     * 导入任务
     */
    IMPORT(1, "导入"),

    /**
     * 导出任务
     */
    EXPORT(2, "导出");

    private final Integer code;
    private final String name;

    TaskTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 任务类型枚举，找不到返回null
     */
    public static TaskTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据code获取名称
     *
     * @param code 状态码
     * @return 任务类型名称，找不到返回空字符串
     */
    public static String getNameByCode(Integer code) {
        TaskTypeEnum type = of(code);
        return type == null ? "" : type.getName();
    }
}
