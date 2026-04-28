package com.xxx.event.common.channel;

import com.xxx.event.common.auth.AuthContext;
import com.xxx.event.common.auth.AuthHandler;
import com.xxx.event.common.auth.AuthTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * WebHook 通道实现
 * 
 * <p>负责通过 WebHook 方式发送事件和回调，支持多种认证类型</p>
 * 
 * <p>支持的认证类型：</p>
 * <ul>
 *   <li>COOKIE/SOA：添加自定义头部</li>
 *   <li>APIG：添加API网关认证头（支持多个头字段）</li>
 *   <li>AKSK：添加签名头部（预留签名计算逻辑）</li>
 *   <li>IAM：添加 Authorization: Bearer {token}</li>
 * </ul>
 * 
 * <p>认证设计：</p>
 * <ul>
 *   <li>三方系统只配置：协议类型、接口地址、认证类型</li>
 *   <li>平台根据 authType 自动获取凭证</li>
 *   <li>头字段支持配置文件配置</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 3.0.0
 * @since 2026-04-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebHookChannel {

    private final RestTemplate restTemplate;
    private final AuthHandler authHandler;

    /**
     * 发送事件到 WebHook 地址
     * 
     * @param url WebHook 地址
     * @param payload 事件内容
     * @param appId 应用ID
     * @param authType 认证类型
     */
    @Async
    public void sendEvent(String url, Map<String, Object> payload, String appId, AuthTypeEnum authType) {
        AuthContext authContext = AuthContext.of(appId, authType);
        sendWebHook(url, "event", payload, authContext);
    }

    /**
     * 发送事件到 WebHook 地址（使用认证上下文）
     * 
     * @param url WebHook 地址
     * @param payload 事件内容
     * @param authContext 认证上下文
     */
    @Async
    public void sendEvent(String url, Map<String, Object> payload, AuthContext authContext) {
        sendWebHook(url, "event", payload, authContext);
    }

    /**
     * 发送回调到 WebHook 地址
     * 
     * @param url WebHook 地址
     * @param payload 回调内容
     * @param appId 应用ID
     * @param authType 认证类型
     */
    @Async
    public void sendCallback(String url, Map<String, Object> payload, String appId, AuthTypeEnum authType) {
        AuthContext authContext = AuthContext.of(appId, authType);
        sendWebHook(url, "callback", payload, authContext);
    }

    /**
     * 发送回调到 WebHook 地址（使用认证上下文）
     * 
     * @param url WebHook 地址
     * @param payload 回调内容
     * @param authContext 认证上下文
     */
    @Async
    public void sendCallback(String url, Map<String, Object> payload, AuthContext authContext) {
        sendWebHook(url, "callback", payload, authContext);
    }

    /**
     * 发送 WebHook 请求
     * 
     * @param url WebHook 地址
     * @param type 类型（event/callback）
     * @param payload 内容
     * @param authContext 认证上下文
     */
    private void sendWebHook(String url, String type, Map<String, Object> payload, AuthContext authContext) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Sending WebHook: type={}, url={}, appId={}, authType={}", 
                    type, url, 
                    authContext != null ? authContext.getAppId() : "none",
                    authContext != null ? authContext.getAuthType() : "none");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 应用认证
            if (authContext != null && authContext.requiresAuth()) {
                authHandler.applyAuth(headers, authContext.getAppId(), authContext.getAuthType());
                log.debug("Auth applied: authType={}", authContext.getAuthType());
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            long duration = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WebHook sent successfully: type={}, url={}, status={}, duration={}ms",
                        type, url, response.getStatusCode(), duration);
            } else {
                log.warn("WebHook send failed: type={}, url={}, status={}, duration={}ms",
                        type, url, response.getStatusCode(), duration);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("WebHook send exception: type={}, url={}, duration={}ms", type, url, duration, e);
        }
    }

    /**
     * 同步发送 WebHook 请求（用于测试）
     * 
     * @param url WebHook 地址
     * @param payload 内容
     * @return 是否发送成功
     */
    public boolean sendSync(String url, Map<String, Object> payload) {
        return sendSync(url, payload, null);
    }

    /**
     * 同步发送 WebHook 请求（用于测试，支持认证）
     * 
     * @param url WebHook 地址
     * @param payload 内容
     * @param authContext 认证上下文
     * @return 是否发送成功
     */
    public boolean sendSync(String url, Map<String, Object> payload, AuthContext authContext) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 应用认证
            if (authContext != null && authContext.requiresAuth()) {
                authHandler.applyAuth(headers, authContext.getAppId(), authContext.getAuthType());
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("WebHook sync send failed: url={}", url, e);
            return false;
        }
    }
}