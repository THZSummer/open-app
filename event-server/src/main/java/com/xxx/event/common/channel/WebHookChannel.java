package com.xxx.event.common.channel;

import com.xxx.event.common.auth.AuthContext;
import com.xxx.event.common.auth.AuthHandler;
import com.xxx.event.common.auth.AuthType;
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
 *   <li>应用类凭证A/B：添加自定义头部（X-Auth-Token）</li>
 *   <li>AKSK：添加签名头部（预留签名计算逻辑）</li>
 *   <li>Bearer Token：添加 Authorization: Bearer {token}</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.1.0
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
     * @param channelAddress WebHook 地址
     * @param payload 事件内容
     * @param authType 认证类型
     * @param authCredentials 认证凭证
     */
    @Async
    public void sendEvent(String channelAddress, Map<String, Object> payload, 
                         AuthType authType, String authCredentials) {
        AuthContext authContext = AuthContext.builder()
                .authType(authType)
                .authCredentials(authCredentials)
                .build();
        sendWebHook(channelAddress, "event", payload, authContext);
    }

    /**
     * 发送事件到 WebHook 地址（使用认证上下文）
     * 
     * @param channelAddress WebHook 地址
     * @param payload 事件内容
     * @param authContext 认证上下文
     */
    @Async
    public void sendEvent(String channelAddress, Map<String, Object> payload, AuthContext authContext) {
        sendWebHook(channelAddress, "event", payload, authContext);
    }

    /**
     * 发送回调到 WebHook 地址
     * 
     * @param channelAddress WebHook 地址
     * @param payload 回调内容
     * @param authType 认证类型
     * @param authCredentials 认证凭证
     */
    @Async
    public void sendCallback(String channelAddress, Map<String, Object> payload,
                            AuthType authType, String authCredentials) {
        AuthContext authContext = AuthContext.builder()
                .authType(authType)
                .authCredentials(authCredentials)
                .build();
        sendWebHook(channelAddress, "callback", payload, authContext);
    }

    /**
     * 发送回调到 WebHook 地址（使用认证上下文）
     * 
     * @param channelAddress WebHook 地址
     * @param payload 回调内容
     * @param authContext 认证上下文
     */
    @Async
    public void sendCallback(String channelAddress, Map<String, Object> payload, AuthContext authContext) {
        sendWebHook(channelAddress, "callback", payload, authContext);
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
            log.info("发送 WebHook: type={}, url={}, authType={}", type, url, 
                    authContext != null ? authContext.getAuthType() : "无");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 应用认证
            if (authContext != null && authContext.requiresAuth()) {
                authHandler.applyAuth(headers, authContext.getAuthType(), authContext.getAuthCredentials());
                log.debug("已应用认证: type={}", authContext.getAuthType());
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
                log.info("WebHook 发送成功: type={}, url={}, status={}, duration={}ms",
                        type, url, response.getStatusCode(), duration);
            } else {
                log.warn("WebHook 发送失败: type={}, url={}, status={}, duration={}ms",
                        type, url, response.getStatusCode(), duration);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("WebHook 发送异常: type={}, url={}, duration={}ms", type, url, duration, e);
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
        return sendSync(url, payload, AuthContext.noAuth());
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
                authHandler.applyAuth(headers, authContext.getAuthType(), authContext.getAuthCredentials());
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
            log.error("WebHook 同步发送失败: url={}", url, e);
            return false;
        }
    }
}
