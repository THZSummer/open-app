package com.xxx.event.gateway.service;

import com.xxx.event.client.ApiServerClient;
import com.xxx.event.common.channel.WebHookChannel;
import com.xxx.event.gateway.dto.CallbackInvokeRequest;
import com.xxx.event.gateway.dto.CallbackInvokeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
 *       <li>WebHook：POST 到 channel_address</li>
 *       <li>SSE：推送到 SSE 连接</li>
 *       <li>WebSocket：推送到 WebSocket 连接</li>
 *     </ul>
 *   </li>
 * </ol>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackGatewayService {

    private final ApiServerClient apiServerClient;
    private final WebHookChannel webHookChannel;
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
        
        log.info("触发回调: scope={}", callbackScope);
        
        // 1. 验证回调资源存在
        Map<String, Object> permission = verifyCallbackResource(callbackScope);
        if (permission == null) {
            log.warn("回调资源不存在: scope={}", callbackScope);
            return CallbackInvokeResponse.builder()
                    .callbackScope(callbackScope)
                    .subscribers(0)
                    .message("回调资源不存在")
                    .build();
        }
        
        // 2. 查询订阅该回调的应用列表
        List<String> subscribedApps = getSubscribedApps(callbackScope);
        
        if (subscribedApps.isEmpty()) {
            log.info("回调无订阅者: scope={}", callbackScope);
            return CallbackInvokeResponse.builder()
                    .callbackScope(callbackScope)
                    .subscribers(0)
                    .message("回调无订阅者")
                    .build();
        }
        
        // 3. 按订阅配置调用消费方
        distributeCallback(callbackScope, payload, subscribedApps);
        
        return CallbackInvokeResponse.builder()
                .callbackScope(callbackScope)
                .subscribers(subscribedApps.size())
                .message("回调已分发至 " + subscribedApps.size() + " 个订阅方")
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
        
        // 尝试从缓存获取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取订阅列表: scope={}", callbackScope);
            return (List<String>) cached;
        }
        
        // 从 api-server 查询
        List<String> apps = apiServerClient.getSubscribedApps(callbackScope);
        
        // 写入缓存
        if (!apps.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, apps, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.debug("订阅列表已缓存: scope={}, count={}", callbackScope, apps.size());
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
        log.info("分发回调: scope={}, subscribers={}", callbackScope, appIds.size());
        
        for (String appId : appIds) {
            try {
                // 查询订阅配置
                Map<String, Object> config = apiServerClient.getSubscriptionConfig(appId, callbackScope);
                
                if (config.isEmpty()) {
                    log.warn("应用订阅配置为空: appId={}, scope={}", appId, callbackScope);
                    continue;
                }
                
                // 获取通道类型
                Integer channelType = (Integer) config.get("channelType");
                String channelAddress = (String) config.get("channelAddress");
                
                // 按通道类型分发
                if (channelType != null && channelAddress != null) {
                    switch (channelType) {
                        case 0 -> {
                            // WebHook
                            log.info("推送到 WebHook: appId={}, scope={}, url={}", 
                                    appId, callbackScope, channelAddress);
                            webHookChannel.sendCallback(channelAddress, payload);
                        }
                        case 1 -> {
                            // SSE（Server-Sent Events）
                            log.info("推送到 SSE: appId={}, scope={}, url={}", 
                                    appId, callbackScope, channelAddress);
                            // TODO: 实际项目中应实现 SSE 推送逻辑
                            log.warn("SSE 通道暂未实现");
                        }
                        case 2 -> {
                            // WebSocket
                            log.info("推送到 WebSocket: appId={}, scope={}, url={}", 
                                    appId, callbackScope, channelAddress);
                            // TODO: 实际项目中应实现 WebSocket 推送逻辑
                            log.warn("WebSocket 通道暂未实现");
                        }
                        default -> 
                            log.warn("未知的通道类型: appId={}, channelType={}", appId, channelType);
                    }
                }
                
            } catch (Exception e) {
                log.error("分发回调失败: appId={}, scope={}", appId, callbackScope, e);
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
        redisTemplate.delete(cacheKey);
        log.info("清除订阅列表缓存: scope={}", callbackScope);
    }
}
