package com.xxx.event.common.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证上下文信息
 * 
 * <p>封装认证相关信息，用于在事件/回调分发时传递认证配置</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 * @since 2026-04-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthContext {

    /**
     * 认证类型
     */
    private AuthType authType;

    /**
     * 认证凭证
     */
    private String authCredentials;

    /**
     * 创建免认证上下文
     * 
     * @return 免认证上下文
     */
    public static AuthContext noAuth() {
        return AuthContext.builder()
                .authType(AuthType.NONE)
                .authCredentials(null)
                .build();
    }

    /**
     * 创建应用类凭证A上下文
     * 
     * @param credentials 凭证内容
     * @return 认证上下文
     */
    public static AuthContext appTypeA(String credentials) {
        return AuthContext.builder()
                .authType(AuthType.APP_TYPE_A)
                .authCredentials(credentials)
                .build();
    }

    /**
     * 创建应用类凭证B上下文
     * 
     * @param credentials 凭证内容
     * @return 认证上下文
     */
    public static AuthContext appTypeB(String credentials) {
        return AuthContext.builder()
                .authType(AuthType.APP_TYPE_B)
                .authCredentials(credentials)
                .build();
    }

    /**
     * 创建Bearer Token上下文
     * 
     * @param token Token内容
     * @return 认证上下文
     */
    public static AuthContext bearerToken(String token) {
        return AuthContext.builder()
                .authType(AuthType.BEARER_TOKEN)
                .authCredentials(token)
                .build();
    }

    /**
     * 创建AKSK上下文
     * 
     * @param accessKey Access Key
     * @return 认证上下文
     */
    public static AuthContext aksk(String accessKey) {
        return AuthContext.builder()
                .authType(AuthType.AKSK)
                .authCredentials(accessKey)
                .build();
    }

    /**
     * 判断是否需要认证
     * 
     * @return true=需要认证，false=免认证
     */
    public boolean requiresAuth() {
        return authType != null && authType.requiresAuth();
    }

    /**
     * 判断认证信息是否有效
     * 
     * @return true=有效，false=无效
     */
    public boolean isValid() {
        if (authType == null) {
            return false;
        }
        
        // 免认证模式不需要凭证
        if (authType == AuthType.NONE) {
            return true;
        }
        
        // 其他认证类型需要凭证
        return authCredentials != null && !authCredentials.trim().isEmpty();
    }
}
