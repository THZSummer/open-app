package com.xxx.it.works.wecode.v2.modules.lookup.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 *
 * <p>用于标识任务的处理状态</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
public enum TaskStatusEnum {

    /**
     * 待处理
     */
    PENDING(0, "待处理"),

    /**
     * 处理中
     */
    PROCESSING(1, "处理中"),

    /**
     * 已完成
     */
    COMPLETED(2, "已完成"),

    /**
     * 失败
     */
    FAILED(3, "失败");

    private final Integer code;
    private final String name;

    TaskStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 任务状态枚举，找不到返回null
     */
    public static TaskStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskStatusEnum status : values()) {
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
     * @return 任务状态名称，找不到返回空字符串
     */
    public static String getNameByCode(Integer code) {
        TaskStatusEnum status = of(code);
        return status == null ? "" : status.getName();
    }
}
