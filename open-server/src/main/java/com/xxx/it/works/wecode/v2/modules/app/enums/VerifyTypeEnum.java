package com.xxx.it.works.wecode.v2.modules.app.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 应用认证方式枚举
 *
 * <p>用于接口 1.7 更新认证方式、接口 1.9 获取认证方式</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum VerifyTypeEnum {

    COOKIE(0, "Cookie"),
    SOA_HEADER(1, "SOAHeader"),
    DIGITAL_SIGNATURE(2, "数字签名"),
    SOA_URL(3, "SOAURL"),
    APIG(4, "APIG"),
    INTEGRATE_TOKEN(5, "IntegrateToken");

    private final Integer code;
    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举实例，未找到返回 null
     */
    public static VerifyTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (VerifyTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 校验编码是否有效
     *
     * @param code 编码
     * @return true-有效, false-无效
     */
    public static boolean isValidCode(Integer code) {
        return fromCode(code) != null;
    }
}
