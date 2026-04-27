package com.xxx.event.common.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 认证方式枚举
 * 
 * <p>用于事件/回调网关的认证方式定义</p>
 * 
 * <p>支持的认证类型：</p>
 * <ul>
 *   <li>COOKIE (0): Cookie认证（应用类凭证A）</li>
 *   <li>SOA (1): SOA认证（应用类凭证B）</li>
 *   <li>APIG (2): API网关认证</li>
 *   <li>IAM (3): IAM统一认证（Bearer Token）</li>
 *   <li>NONE (4): 免认证</li>
 *   <li>AKSK (5): AKSK认证</li>
 *   <li>CLITOKEN (6): Client Token认证</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum AuthTypeEnum {

    /**
     * Cookie 认证（应用类凭证A）
     */
    COOKIE(0, "COOKIE", "Cookie认证"),

    /**
     * SOA 认证（应用类凭证B）
     */
    SOA(1, "SOA", "SOA认证"),

    /**
     * API 网关认证
     */
    APIG(2, "APIG", "API网关认证"),

    /**
     * IAM 统一认证（Bearer Token）
     */
    IAM(3, "IAM", "IAM统一认证"),

    /**
     * 免认证
     */
    NONE(4, "NONE", "免认证"),

    /**
     * AKSK 认证
     */
    AKSK(5, "AKSK", "AK/SK认证"),

    /**
     * Client Token 认证
     */
    CLITOKEN(6, "CLITOKEN", "Client Token认证");

    /**
     * 枚举编码（数据库存储值）
     */
    private final Integer code;

    /**
     * 枚举标识（程序内部使用）
     */
    private final String name;

    /**
     * 枚举描述
     */
    private final String description;

    /**
     * 默认认证方式：SOA
     */
    public static final AuthTypeEnum DEFAULT = SOA;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举实例，未找到返回 null
     */
    public static AuthTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AuthTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据名称获取枚举
     *
     * @param name 名称
     * @return 枚举实例，未找到返回 null
     */
    public static AuthTypeEnum fromName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (AuthTypeEnum type : values()) {
            if (type.getName().equalsIgnoreCase(name)) {
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

    /**
     * 判断是否为应用类凭证
     * 
     * @return true=应用类凭证，false=非应用类凭证
     */
    public boolean isAppCredentials() {
        return this == COOKIE || this == SOA;
    }

    /**
     * 判断是否需要认证
     * 
     * @return true=需要认证，false=免认证
     */
    public boolean requiresAuth() {
        return this != NONE;
    }
}
