package com.xxx.event.gateway.service;

import com.xxx.event.client.ApiServerClient;
import com.xxx.event.common.auth.AuthTypeEnum;
import com.xxx.event.common.channel.MessageQueueChannel;
import com.xxx.event.common.channel.WebHookChannel;
import com.xxx.event.gateway.dto.EventPublishRequest;
import com.xxx.event.gateway.dto.EventPublishResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 事件网关服务
 * 
 * <p>接口编号：#56</p>
 * <p>处理流程：</p>
 * <ol>
 *   <li>验证 Topic 对应的事件资源存在且已发布</li>
 *   <li>查询订阅该事件的应用列表</li>
 *   <li>按订阅配置分发事件：
 *     <ul>
 *       <li>企业内部消息队列 (0)：推送到对应队列</li>
 *       <li>WebHook (1)：POST 到 channel_address</li>
 *     </ul>
 *   </li>
 * </ol>
 * 
 * <p>注意：根据 FR-029 规范，事件分发仅支持企业内部消息队列和 WebHook，不支持 SSE 和 WebSocket</p>
 * 
 * @author SDDU Build Agent
 * @version 1.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventGatewayService {

    private final ApiServerClient apiServerClient;
    private final WebHookChannel webHookChannel;
    private final MessageQueueChannel messageQueueChannel;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis 缓存键前缀
     */
    private static final String CACHE_KEY_PREFIX = "event:subscribers:";
    
    /**
     * 缓存过期时间（秒）
     */
    private static final long CACHE_EXPIRE_SECONDS = 300;

    /**
     * 发布事件
     * 
     * @param request 事件发布请求
     * @return 发布结果
     */
    public EventPublishResponse publishEvent(EventPublishRequest request) {
        String topic = request.getTopic();
        Map<String, Object> payload = request.getPayload();
        
        log.info("Publishing event: topic={}", topic);
        
        // 1. 验证 Topic 对应的事件资源存在且已发布
        Map<String, Object> permission = verifyEventResource(topic);
        if (permission == null) {
            log.warn("Event resource not found or not published: topic={}", topic);
            return EventPublishResponse.builder()
                    .topic(topic)
                    .subscribers(0)
                    .message("Event resource not found or not published")
                    .build();
        }
        
        // 2. 查询订阅该事件的应用列表
        List<String> subscribedApps = getSubscribedApps(topic);
        
        if (subscribedApps.isEmpty()) {
            log.info("Event has no subscribers: topic={}", topic);
            return EventPublishResponse.builder()
                    .topic(topic)
                    .subscribers(0)
                    .message("Event has no subscribers")
                    .build();
        }
        
        // 3. 按订阅配置分发事件
        distributeEvent(topic, payload, subscribedApps);
        
        return EventPublishResponse.builder()
                .topic(topic)
                .subscribers(subscribedApps.size())
                .message("Event dispatched to " + subscribedApps.size() + " subscribers")
                .build();
    }

    /**
     * 验证事件资源
     * 
     * @param topic 事件 Topic
     * @return 权限详情
     */
    private Map<String, Object> verifyEventResource(String topic) {

        // 根据 Topic 查询对应的事件权限
        // Scope 格式：event:{module}:{identifier}
        // Topic 格式：{module}.{identifier}
        // 转换规则：im.message.received -> event:im:message-received
        String scope = convertTopicToScope(topic);
        
        return apiServerClient.getPermissionByScope(scope);
    }

    /**
     * 将 Topic 转换为 Scope
     * 
     * @param topic 事件 Topic（如：im.message.received）
     * @return Scope（如：event:im:message-received）
     */
    private String convertTopicToScope(String topic) {
        if (topic == null || topic.isEmpty()) {
            return null;
        }
        
        // 将点号替换为中划线，并添加 event: 前缀
        String[] parts = topic.split("\\.");
        if (parts.length >= 2) {
            String module = parts[0];
            StringBuilder identifier = new StringBuilder(parts[1]);
            for (int i = 2; i < parts.length; i++) {
                identifier.append("-").append(parts[i]);
            }
            return "event:" + module + ":" + identifier;
        }
        
        return "event:" + topic.replace(".", ":");
    }

    /**
     * 查询订阅应用列表（使用缓存）
     * 
     * @param topic 事件 Topic
     * @return 应用ID列表
     */
    @SuppressWarnings("unchecked")
    private List<String> getSubscribedApps(String topic) {
        String cacheKey = CACHE_KEY_PREFIX + topic;
        
        // 尝试从缓存获取（添加异常处理）
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {

                // 类型安全检查
                if (cached instanceof List) {
                    try {
                        return (List<String>) cached;
                    } catch (ClassCastException e) {
                        log.warn("Cache data type error, clearing cache: topic={}", topic);
                        redisTemplate.delete(cacheKey);
                    }
                } else {
                    log.warn("Cache data type mismatch, clearing cache: topic={}, actualType={}", topic, cached.getClass());
                    redisTemplate.delete(cacheKey);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get subscriber list from Redis, fallback to API query: topic={}", topic, e);

            // 继续执行API查询
        }
        
        // 从 api-server 查询
        String scope = convertTopicToScope(topic);
        List<String> apps = apiServerClient.getSubscribedApps(scope);
        
        // 处理null返回
        if (apps == null) {
            log.warn("API returned null subscriber list: scope={}", scope);
            apps = Collections.emptyList();
        }
        
        // 写入缓存（仅非空列表）
        if (!apps.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, apps, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                log.debug("Subscriber list cached: topic={}, count={}", topic, apps.size());
            } catch (Exception e) {
                log.error("Failed to cache subscriber list: topic={}", topic, e);
            }
        }
        
        return apps;
    }

    /**
     * 分发事件
     * 
     * @param topic 事件 Topic
     * @param payload 事件内容
     * @param appIds 应用ID列表
     */
    private void distributeEvent(String topic, Map<String, Object> payload, List<String> appIds) {
        log.info("Dispatching event: topic={}, subscribers={}", topic, appIds.size());
        
        String scope = convertTopicToScope(topic);
        
        for (String appId : appIds) {
            try {

                // 查询订阅配置
                Map<String, Object> config = apiServerClient.getSubscriptionConfig(appId, scope);
                
                if (config == null || config.isEmpty()) {
                    log.warn("App subscription config is empty: appId={}, topic={}", appId, topic);
                    continue;
                }
                
                // 安全获取通道配置
                Integer channelType = safeGetInteger(config, "channelType");
                String channelAddress = safeGetString(config, "channelAddress");
                Integer authTypeCode = safeGetInteger(config, "authType");
                AuthTypeEnum authType = authTypeCode != null ? AuthTypeEnum.fromCode(authTypeCode) : null;
                
                // 按通道类型分发
                if (channelType != null && channelAddress != null && !channelAddress.isEmpty()) {
                    switch (channelType) {
                        case 0 -> {

                            // 企业内部消息队列
                            log.info("Pushing to internal message queue: appId={}, topic={}, queue={}",
                                    appId, topic, channelAddress);
                            messageQueueChannel.sendEvent(channelAddress, payload);
                        }
                        case 1 -> {

                            // WebHook
                            log.info("Pushing to WebHook: appId={}, topic={}, url={}, authType={}", 
                                    appId, topic, channelAddress, authType);
                            webHookChannel.sendEvent(channelAddress, payload, appId, authType);
                        }
                        default -> 
                            log.warn("Unknown channel type or channel not supported for event: appId={}, channelType={} (event supports: 0-message queue, 1-WebHook)", 
                                    appId, channelType);
                    }
                } else {
                    log.warn("Invalid channel config: appId={}, channelType={}, channelAddress={}", 
                            appId, channelType, channelAddress);
                }
                
            } catch (Exception e) {
                log.error("Failed to dispatch event: appId={}, topic={}", appId, topic, e);
            }
        }
    }

    /**
     * 安全获取Integer值（支持Long、String等类型转换）
     * 
     * @param map 配置Map
     * @param key 键名
     * @return Integer值，转换失败返回null
     */
    private Integer safeGetInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("Cannot convert string to Integer: key={}, value={}", key, value);
                return null;
            }
        }
        log.warn("Cannot convert to Integer: key={}, valueType={}", key, value.getClass());
        return null;
    }

    /**
     * 安全获取String值
     * 
     * @param map 配置Map
     * @param key 键名
     * @return String值，null返回null
     */
    private String safeGetString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return String.valueOf(value);
    }

    /**
     * 清除订阅列表缓存
     * 
     * @param topic 事件 Topic
     */
    public void clearCache(String topic) {
        if (topic == null || topic.isEmpty()) {
            return;
        }
        String cacheKey = CACHE_KEY_PREFIX + topic;
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            log.info("Clearing subscriber list cache: topic={}, result={}", topic, deleted);
        } catch (Exception e) {
            log.error("Failed to clear subscriber list cache: topic={}", topic, e);
        }
    }
}
