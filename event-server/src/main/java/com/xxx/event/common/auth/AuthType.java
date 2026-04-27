package com.xxx.event.common.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 认证类型枚举
 * 
 * <p>定义系统支持的认证类型，对应不同的身份验证方式</p>
 * 
 * <p>重点支持：</p>
 * <ul>
 *   <li>APP_TYPE_A (0): 应用类凭证A（事件/回调配置主要使用）</li>
 *   <li>APP_TYPE_B (1): 应用类凭证B（事件/回调配置主要使用）</li>
 *   <li>AKSK (5): AKSK认证（API鉴权使用）</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 * @since 2026-04-27
 */
@Getter
@AllArgsConstructor
public enum AuthType {

    /**
     * 应用类凭证A（或 Cookie）
     */
    APP_TYPE_A(0, "应用类凭证A", "App Type A Credentials"),

    /**
     * 应用类凭证B（或 SOA）
     */
    APP_TYPE_B(1, "应用类凭证B", "App Type B Credentials"),

    /**
     * APIG
     */
    APIG(2, "APIG认证", "APIG Authentication"),

    /**
     * IAM（或 Bearer Token）
     */
    BEARER_TOKEN(3, "Bearer Token", "Bearer Token Authentication"),

    /**
     * 免认证
     */
    NONE(4, "免认证", "No Authentication"),

    /**
     * AKSK
     */
    AKSK(5, "AKSK认证", "AKSK Authentication"),

    /**
     * CLITOKEN
     */
    CLITOKEN(6, "CLITOKEN认证", "CLITOKEN Authentication");

    /**
     * 认证类型编码
     */
    private final Integer code;

    /**
     * 中文名称
     */
    private final String nameCn;

    /**
     * 英文名称
     */
    private final String nameEn;

    /**
     * 根据编码获取认证类型
     * 
     * @param code 认证类型编码
     * @return 认证类型枚举，未找到则返回 null
     */
    public static AuthType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (AuthType authType : values()) {
            if (authType.getCode().equals(code)) {
                return authType;
            }
        }
        
        return null;
    }

    /**
     * 判断是否为应用类凭证
     * 
     * @return true=应用类凭证，false=非应用类凭证
     */
    public boolean isAppCredentials() {
        return this == APP_TYPE_A || this == APP_TYPE_B;
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
