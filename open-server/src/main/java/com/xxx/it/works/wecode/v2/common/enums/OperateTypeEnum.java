package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计日志操作类型枚举
 *
 * <p>对应 openplateform_operate_log_t.operate_type 字段（varchar(10)）</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum OperateTypeEnum {

    CREATE("CREATE", "创建"),
    UPDATE("UPDATE", "更新"),
    DELETE("DELETE", "删除"),
    WITHDRAW("WITHDRAW", "撤回"),
    SUBSCRIBE("SUBSCRIBE", "订阅"),
    CONFIG("CONFIG", "配置");

    /** DB 存储值 */
    private final String code;

    /** 中文描述 */
    private final String description;

    /**
     * 根据 code 获取枚举值
     */
    public static OperateTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OperateTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
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
