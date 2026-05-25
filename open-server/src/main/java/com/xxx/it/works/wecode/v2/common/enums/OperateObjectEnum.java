package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计日志操作对象枚举
 *
 * <p>对应 openplateform_operate_log_t.operate_object 字段（varchar(64)）</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum OperateObjectEnum {

    API("API", "API资源"),
    EVENT("EVENT", "事件资源"),
    CALLBACK("CALLBACK", "回调资源"),
    API_PERMISSION("API_PERMISSION", "API权限订阅"),
    EVENT_PERMISSION("EVENT_PERMISSION", "事件权限订阅"),
    CALLBACK_PERMISSION("CALLBACK_PERMISSION", "回调权限订阅");

    /** DB 存储值 */
    private final String code;

    /** 中文描述 */
    private final String description;

    /**
     * 根据 code 获取枚举值
     */
    public static OperateObjectEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OperateObjectEnum obj : values()) {
            if (obj.getCode().equals(code)) {
                return obj;
            }
        }
        return null;
    }

    /**
     * 判断 code 是否有效
     */
    public static boolean isValidCode(String code) {
        return fromCode(code) != null;
    }
}
