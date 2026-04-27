package com.xxx.event.common.auth;

import java.util.Map;

/**
 * 凭证获取器接口
 * 
 * <p>根据认证类型自动获取对应的凭证</p>
 * 
 * <p>返回格式：直接返回 HTTP 头字段映射</p>
 * <pre>
 * COOKIE -> {"X-Auth-Type": "0", "X-Auth-Token": "token-value"}
 * APIG -> {"X-APIG-App-Id": "app-123", "X-APIG-Token": "token-value"}
 * AKSK -> {"X-Auth-Type": "5", "X-Access-Key": "key", "X-Signature": "sig"}
 * </pre>
 * 
 * @author SDDU Build Agent
 * @version 6.0.0
 * @since 2026-04-27
 */
public interface CredentialProvider {
    
    /**
     * 获取认证凭证（直接返回 HTTP 头字段）
     * 
     * <p>根据应用ID和认证类型，从凭证服务或应用管理系统获取对应的认证凭证</p>
     * <p>返回的 Map 直接是 HTTP 头字段名和值，无需二次映射</p>
     * 
     * @param appId 应用ID
     * @param authType 认证类型
     * @return HTTP 头字段映射（key=头字段名，value=头字段值）
     */
    Map<String, String> getCredentials(String appId, AuthTypeEnum authType);
}