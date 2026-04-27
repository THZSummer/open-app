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
 * <p>设计变更：</p>
 * <ul>
 *   <li>三方系统只配置：协议类型、接口地址、认证类型</li>
 *   <li>平台根据 authType 自动获取凭证，无需手动配置</li>
 *   <li>authCredentials 字段保留用于向后兼容，但已弃用</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 3.0.0
 * @since 2026-04-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthContext {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 认证类型
     */
    private AuthTypeEnum authType;

    /**
     * 认证凭证（已弃用，保留用于向后兼容）
     * 
     * @deprecated 使用 appId + authType 自动获取凭证，无需手动设置
     */
    @Deprecated
    private String authCredentials;

    /**
     * 创建免认证上下文
     * 
     * @param appId 应用ID
     * @return 免认证上下文
     */
    public static AuthContext noAuth(String appId) {
        return AuthContext.builder()
                .appId(appId)
                .authType(AuthTypeEnum.NONE)
                .build();
    }

    /**
     * 创建免认证上下文（向后兼容）
     * 
     * @return 免认证上下文
     * @deprecated 使用 noAuth(String appId) 代替
     */
    @Deprecated
    public static AuthContext noAuth() {
        return AuthContext.builder()
                .authType(AuthTypeEnum.NONE)
                .build();
    }

    /**
     * 创建认证上下文
     * 
     * @param appId 应用ID
     * @param authType 认证类型
     * @return 认证上下文
     */
    public static AuthContext of(String appId, AuthTypeEnum authType) {
        return AuthContext.builder()
                .appId(appId)
                .authType(authType)
                .build();
    }

    /**
     * 创建 COOKIE (应用类凭证A) 上下文
     * 
     * @param appId 应用ID
     * @return 认证上下文
     */
    public static AuthContext cookie(String appId) {
        return AuthContext.builder()
                .appId(appId)
                .authType(AuthTypeEnum.COOKIE)
                .build();
    }

    /**
     * 创建 SOA (应用类凭证B) 上下文
     * 
     * @param appId 应用ID
     * @return 认证上下文
     */
    public static AuthContext soa(String appId) {
        return AuthContext.builder()
                .appId(appId)
                .authType(AuthTypeEnum.SOA)
                .build();
    }

    /**
     * 创建 IAM (Bearer Token) 上下文
     * 
     * @param appId 应用ID
     * @return 认证上下文
     */
    public static AuthContext iam(String appId) {
        return AuthContext.builder()
                .appId(appId)
                .authType(AuthTypeEnum.IAM)
                .build();
    }

    /**
     * 创建AKSK上下文
     * 
     * @param appId 应用ID
     * @return 认证上下文
     */
    public static AuthContext aksk(String appId) {
        return AuthContext.builder()
                .appId(appId)
                .authType(AuthTypeEnum.AKSK)
                .build();
    }

    /**
     * 创建认证上下文（向后兼容）
     * 
     * @param authType 认证类型
     * @param authCredentials 认证凭证（已弃用）
     * @return 认证上下文
     * @deprecated 使用 of(String appId, AuthTypeEnum authType) 代替
     */
    @Deprecated
    public static AuthContext of(AuthTypeEnum authType, String authCredentials) {
        return AuthContext.builder()
                .authType(authType)
                .authCredentials(authCredentials)
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
        if (authType == AuthTypeEnum.NONE) {
            return true;
        }
        
        // 新设计：必须有 appId
        return appId != null && !appId.trim().isEmpty();
    }
}