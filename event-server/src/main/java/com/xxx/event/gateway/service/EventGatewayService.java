package com.xxx.event.gateway.service;

import com.xxx.event.client.ApiServerClient;
import com.xxx.event.common.channel.WebHookChannel;
import com.xxx.event.gateway.dto.EventPublishRequest;
import com.xxx.event.gateway.dto.EventPublishResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
 *       <li>WebHook：POST 到 channel_address</li>
 *       <li>企业内部消息队列：推送到对应队列</li>
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
public class EventGatewayService {

    private final ApiServerClient apiServerClient;
    private final WebHookChannel webHookChannel;
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
        
        log.info("发布事件: topic={}", topic);
        
        // 1. 验证 Topic 对应的事件资源存在且已发布
        Map<String, Object> permission = verifyEventResource(topic);
        if (permission == null) {
            log.warn("事件资源不存在或未发布: topic={}", topic);
            return EventPublishResponse.builder()
                    .topic(topic)
                    .subscribers(0)
                    .message("事件资源不存在或未发布")
                    .build();
        }
        
        // 2. 查询订阅该事件的应用列表
        List<String> subscribedApps = getSubscribedApps(topic);
        
        if (subscribedApps.isEmpty()) {
            log.info("事件无订阅者: topic={}", topic);
            return EventPublishResponse.builder()
                    .topic(topic)
                    .subscribers(0)
                    .message("事件无订阅者")
                    .build();
        }
        
        // 3. 按订阅配置分发事件
        distributeEvent(topic, payload, subscribedApps);
        
        return EventPublishResponse.builder()
                .topic(topic)
                .subscribers(subscribedApps.size())
                .message("事件已分发至 " + subscribedApps.size() + " 个订阅方")
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
        
        // 尝试从缓存获取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取订阅列表: topic={}", topic);
            return (List<String>) cached;
        }
        
        // 从 api-server 查询
        String scope = convertTopicToScope(topic);
        List<String> apps = apiServerClient.getSubscribedApps(scope);
        
        // 写入缓存
        if (!apps.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, apps, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.debug("订阅列表已缓存: topic={}, count={}", topic, apps.size());
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
        log.info("分发事件: topic={}, subscribers={}", topic, appIds.size());
        
        String scope = convertTopicToScope(topic);
        
        for (String appId : appIds) {
            try {
                // 查询订阅配置
                Map<String, Object> config = apiServerClient.getSubscriptionConfig(appId, scope);
                
                if (config.isEmpty()) {
                    log.warn("应用订阅配置为空: appId={}, topic={}", appId, topic);
                    continue;
                }
                
                // 获取通道类型
                Integer channelType = (Integer) config.get("channelType");
                String channelAddress = (String) config.get("channelAddress");
                
                // 按通道类型分发
                if (channelType != null && channelAddress != null) {
                    switch (channelType) {
                        case 0 -> {
                            // 企业内部消息队列（Mock 实现）
                            log.info("推送到内部消息队列: appId={}, topic={}, queue={}", 
                                    appId, topic, channelAddress);
                            // TODO: 实际项目中应调用内部消息网关 SDK
                        }
                        case 1 -> {
                            // WebHook
                            log.info("推送到 WebHook: appId={}, topic={}, url={}", 
                                    appId, topic, channelAddress);
                            webHookChannel.sendEvent(channelAddress, payload);
                        }
                        default -> 
                            log.warn("未知的通道类型: appId={}, channelType={}", appId, channelType);
                    }
                }
                
            } catch (Exception e) {
                log.error("分发事件失败: appId={}, topic={}", appId, topic, e);
            }
        }
    }

    /**
     * 清除订阅列表缓存
     * 
     * @param topic 事件 Topic
     */
    public void clearCache(String topic) {
        String cacheKey = CACHE_KEY_PREFIX + topic;
        redisTemplate.delete(cacheKey);
        log.info("清除订阅列表缓存: topic={}", topic);
    }
}
