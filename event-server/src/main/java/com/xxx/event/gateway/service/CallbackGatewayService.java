package com.xxx.event.gateway.service;

import com.xxx.event.client.ApiServerClient;
import com.xxx.event.common.auth.AuthTypeEnum;
import com.xxx.event.common.channel.SseChannel;
import com.xxx.event.common.channel.WebHookChannel;
import com.xxx.event.common.channel.WebSocketChannel;
import com.xxx.event.gateway.dto.CallbackInvokeRequest;
import com.xxx.event.gateway.dto.CallbackInvokeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 回调网关服务
 * 
 * <p>接口编号：#57</p>
 * <p>处理流程：</p>
 * <ol>
 *   <li>验证 callback_scope 对应的回调资源存在</li>
 *   <li>查询订阅该回调的应用列表</li>
 *   <li>按订阅配置调用消费方：
 *     <ul>
 *       <li>WebHook (0)：POST 到 channel_address</li>
 *       <li>SSE (1)：推送到 SSE 连接</li>
 *       <li>WebSocket (2)：推送到 WebSocket 连接</li>
 *     </ul>
 *   </li>
 * </ol>
 * 
 * @author SDDU Build Agent
 * @version 1.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackGatewayService {

    private final ApiServerClient apiServerClient;
    private final WebHookChannel webHookChannel;
    private final SseChannel sseChannel;
    private final WebSocketChannel webSocketChannel;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis 缓存键前缀
     */
    private static final String CACHE_KEY_PREFIX = "callback:subscribers:";
    
    /**
     * 缓存过期时间（秒）
     */
    private static final long CACHE_EXPIRE_SECONDS = 300;

    /**
     * 触发回调
     * 
     * @param request 回调触发请求
     * @return 触发结果
     */
    public CallbackInvokeResponse invokeCallback(CallbackInvokeRequest request) {
        String callbackScope = request.getCallbackScope();
        Map<String, Object> payload = request.getPayload();
        
        log.info("Triggering callback: scope={}", callbackScope);
        
        // 1. 验证回调资源存在
        Map<String, Object> permission = verifyCallbackResource(callbackScope);
        if (permission == null) {
            log.warn("Callback resource not found: scope={}", callbackScope);
            return CallbackInvokeResponse.builder()
                    .callbackScope(callbackScope)
                    .subscribers(0)
                    .message("Callback resource not found")
                    .build();
        }
        
        // 2. 查询订阅该回调的应用列表
        List<String> subscribedApps = getSubscribedApps(callbackScope);
        
        if (subscribedApps.isEmpty()) {
            log.info("Callback has no subscribers: scope={}", callbackScope);
            return CallbackInvokeResponse.builder()
                    .callbackScope(callbackScope)
                    .subscribers(0)
                    .message("Callback has no subscribers")
                    .build();
        }
        
        // 3. 按订阅配置调用消费方
        distributeCallback(callbackScope, payload, subscribedApps);
        
        return CallbackInvokeResponse.builder()
                .callbackScope(callbackScope)
                .subscribers(subscribedApps.size())
                .message("Callback dispatched to " + subscribedApps.size() + " subscribers")
                .build();
    }

    /**
     * 验证回调资源
     * 
     * @param callbackScope 回调 Scope
     * @return 权限详情
     */
    private Map<String, Object> verifyCallbackResource(String callbackScope) {
        return apiServerClient.getPermissionByScope(callbackScope);
    }

    /**
     * 查询订阅应用列表（使用缓存）
     * 
     * @param callbackScope 回调 Scope
     * @return 应用ID列表
     */
    @SuppressWarnings("unchecked")
    private List<String> getSubscribedApps(String callbackScope) {
        String cacheKey = CACHE_KEY_PREFIX + callbackScope;
        
        try {
            // 尝试从缓存获取
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Getting subscriber list from cache: scope={}", callbackScope);
                return (List<String>) cached;
            }
        } catch (Exception e) {
            log.error("Failed to get subscriber list cache from Redis: scope={}", callbackScope, e);
            // 继续从api-server查询
        }
        
        // 从 api-server 查询
        List<String> apps = apiServerClient.getSubscribedApps(callbackScope);
        
        // 写入缓存
        if (!apps.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, apps, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                log.debug("Subscriber list cached: scope={}, count={}", callbackScope, apps.size());
            } catch (Exception e) {
                log.error("Failed to cache subscriber list to Redis: scope={}", callbackScope, e);
                // 缓存失败不影响业务流程
            }
        }
        
        return apps;
    }

    /**
     * 分发回调
     * 
     * @param callbackScope 回调 Scope
     * @param payload 回调内容
     * @param appIds 应用ID列表
     */
    private void distributeCallback(String callbackScope, Map<String, Object> payload, List<String> appIds) {
        log.info("Dispatching callback: scope={}, subscribers={}", callbackScope, appIds.size());
        
        for (String appId : appIds) {
            try {
                // 查询订阅配置
                Map<String, Object> config = apiServerClient.getSubscriptionConfig(appId, callbackScope);
                
                if (config.isEmpty()) {
                    log.warn("App subscription config is empty: appId={}, scope={}", appId, callbackScope);
                    continue;
                }
                
                // 获取通道配置（类型安全转换）
                Integer channelType = safeGetInteger(config, "channelType");
                String channelAddress = safeGetString(config, "channelAddress");
                
                // 获取认证类型（三方配置）
                Integer authTypeCode = safeGetInteger(config, "authType");
                AuthTypeEnum authType = AuthTypeEnum.fromCode(authTypeCode);
                
                // 按通道类型分发
                if (channelType != null && channelAddress != null) {
                    switch (channelType) {
                        case 0 -> {
                            // WebHook（使用新的认证方式：appId + authType）
                            log.info("Pushing to WebHook: appId={}, scope={}, url={}, authType={}", 
                                    appId, callbackScope, channelAddress, authType);
                            webHookChannel.sendCallback(channelAddress, payload, appId, authType);
                        }
                        case 1 -> {
                            // SSE（Server-Sent Events）
                            log.info("Pushing to SSE: appId={}, scope={}, connectionId={}", 
                                    appId, callbackScope, channelAddress);
                            // channelAddress 作为 SSE 连接ID
                            Map<String, Object> ssePayload = buildCallbackPayload(callbackScope, payload);
                            sseChannel.sendCallback(channelAddress, ssePayload);
                        }
                        case 2 -> {
                            // WebSocket
                            log.info("Pushing to WebSocket: appId={}, scope={}, connectionId={}", 
                                    appId, callbackScope, channelAddress);
                            // channelAddress 作为 WebSocket 连接ID
                            Map<String, Object> wsPayload = buildCallbackPayload(callbackScope, payload);
                            webSocketChannel.sendCallback(channelAddress, wsPayload);
                        }
                        default -> 
                            log.warn("Unknown channel type: appId={}, channelType={}", appId, channelType);
                    }
                }
                
            } catch (Exception e) {
                log.error("Failed to dispatch callback: appId={}, scope={}", appId, callbackScope, e);
            }
        }
    }

    /**
     * 清除订阅列表缓存
     * 
     * @param callbackScope 回调 Scope
     */
    public void clearCache(String callbackScope) {
        String cacheKey = CACHE_KEY_PREFIX + callbackScope;
        try {
            redisTemplate.delete(cacheKey);
            log.info("Clearing subscriber list cache: scope={}", callbackScope);
        } catch (Exception e) {
            log.error("Failed to clear subscriber list cache: scope={}", callbackScope, e);
            // 缓存清除失败不影响业务流程
        }
    }

    /**
     * 构建回调消息体
     * 
     * <p>包含回调 Scope 和实际载荷</p>
     * 
     * @param callbackScope 回调 Scope
     * @param payload 实际载荷
     * @return 完整的回调消息体
     */
    private Map<String, Object> buildCallbackPayload(String callbackScope, Map<String, Object> payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("scope", callbackScope);
        message.put("timestamp", System.currentTimeMillis());
        message.put("data", payload);
        return message;
    }

    /**
     * 安全获取Integer类型配置值
     * 
     * @param config 配置Map
     * @param key 键名
     * @return Integer值，类型不匹配时返回null
     */
    private Integer safeGetInteger(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        log.warn("Config value type mismatch: key={}, expected=Integer, actual={}", key, value.getClass().getSimpleName());
        return null;
    }

    /**
     * 安全获取String类型配置值
     * 
     * @param config 配置Map
     * @param key 键名
     * @return String值，类型不匹配时返回null
     */
    private String safeGetString(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        log.warn("Config value type mismatch: key={}, expected=String, actual={}", key, value.getClass().getSimpleName());
        return null;
    }
}
