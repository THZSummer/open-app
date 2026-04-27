package com.xxx.event.common.auth;

import org.springframework.http.HttpHeaders;

/**
 * 认证处理器接口
 * 
 * <p>定义认证处理的标准接口，支持为 WebHook 请求添加认证头</p>
 * 
 * <p>主要功能：</p>
 * <ul>
 *   <li>为 WebHook 请求添加认证头</li>
 *   <li>验证认证凭证有效性（预留）</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 * @since 2026-04-27
 */
public interface AuthHandler {

    /**
     * 应用认证到 HTTP 请求
     * 
     * <p>根据认证类型，为 HTTP 请求添加相应的认证头部信息</p>
     * 
     * @param headers HTTP 请求头
     * @param authType 认证类型
     * @param authCredentials 认证凭证（从订阅配置获取）
     */
    void applyAuth(HttpHeaders headers, AuthType authType, String authCredentials);

    /**
     * 验证认证凭证有效性（预留）
     * 
     * <p>用于验证应用提供的认证凭证是否有效</p>
     * 
     * @param appId 应用ID
     * @param authType 认证类型
     * @param authCredentials 认证凭证
     * @return true=凭证有效，false=凭证无效
     */
    boolean validateAuth(String appId, AuthType authType, String authCredentials);
}
