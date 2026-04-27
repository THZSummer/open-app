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
 * <p>设计要点：</p>
 * <ul>
 *   <li>三方系统只配置：协议类型、接口地址、认证类型</li>
 *   <li>平台根据 authType 调用 CredentialProvider 获取凭证</li>
 *   <li>根据 authType 从配置获取对应的头字段映射</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 3.0.0
 * @since 2026-04-27
 */
public interface AuthHandler {

    /**
     * 应用认证到 HTTP 请求
     * 
     * <p>根据认证类型，为 HTTP 请求添加相应的认证头部信息</p>
     * 
     * <p>流程：</p>
     * <ol>
     *   <li>根据 authType 调用 CredentialProvider 获取凭证</li>
     *   <li>根据 authType 从配置获取对应的头字段映射</li>
     *   <li>应用凭证到HTTP头</li>
     * </ol>
     * 
     * @param headers HTTP 请求头
     * @param appId 应用ID
     * @param authType 认证类型（三方配置的类型）
     */
    void applyAuth(HttpHeaders headers, String appId, AuthTypeEnum authType);

    /**
     * 验证认证凭证有效性（预留）
     * 
     * <p>用于验证应用提供的认证凭证是否有效</p>
     * 
     * @param appId 应用ID
     * @param authType 认证类型
     * @return true=凭证有效，false=凭证无效
     */
    boolean validateAuth(String appId, AuthTypeEnum authType);
}