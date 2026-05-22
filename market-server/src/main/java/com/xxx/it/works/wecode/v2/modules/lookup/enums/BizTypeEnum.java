package com.xxx.it.works.wecode.v2.modules.lookup.enums;

import lombok.Getter;

/**
 * 业务类型枚举
 *
 * <p>用于标识任务的业务类型：LookUp或数据字典</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
public enum BizTypeEnum {

    /**
     * LookUp业务类型
     */
    LOOKUP(1, "LookUp"),

    /**
     * 数据字典业务类型
     */
    DATA_DICTIONARY(2, "数据字典");

    private final Integer code;
    private final String name;

    BizTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 业务类型枚举，找不到返回null
     */
    public static BizTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (BizTypeEnum type : values()) {
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
     * @return 业务类型名称，找不到返回空字符串
     */
    public static String getNameByCode(Integer code) {
        BizTypeEnum type = of(code);
        return type == null ? "" : type.getName();
    }
}
