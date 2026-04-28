package com.xxx.event.gateway.service;

import com.xxx.event.client.ApiServerClient;
import com.xxx.event.common.auth.AuthTypeEnum;
import com.xxx.event.common.channel.MessageQueueChannel;
import com.xxx.event.common.channel.WebHookChannel;
import com.xxx.event.gateway.dto.EventPublishRequest;
import com.xxx.event.gateway.dto.EventPublishResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EventGatewayService 单元测试
 * 
 * <p>测试覆盖：</p>
 * <ul>
 *   <li>publishEvent 方法测试（正常流程、资源不存在、无订阅者）</li>
 *   <li>verifyEventResource 方法测试</li>
 *   <li>convertTopicToScope 方法测试（各种格式转换）</li>
 *   <li>getSubscribedApps 方法测试（缓存命中、缓存未命中）</li>
 *   <li>distributeEvent 方法测试（消息队列分发、WebHook分发、配置为空、异常处理）</li>
 *   <li>clearCache 方法测试</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventGatewayService 单元测试")
class EventGatewayServiceTest {

    @Mock
    private ApiServerClient apiServerClient;

    @Mock
    private WebHookChannel webHookChannel;

    @Mock
    private MessageQueueChannel messageQueueChannel;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private EventGatewayService eventGatewayService;

    private EventPublishRequest request;
    private Map<String, Object> payload;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        payload = new HashMap<>();
        payload.put("messageId", "msg-123");
        payload.put("content", "Hello World");

        request = EventPublishRequest.builder()
                .topic("im.message.received")
                .payload(payload)
                .build();

        // 设置 Redis ValueOperations mock（使用 lenient 避免不必要的 stubbing 警告）
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("publishEvent 方法测试")
    class PublishEventTest {

        @Test
        @DisplayName("正常流程 - 成功发布事件")
        void testPublishEvent_Success() {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            
            // Mock: 权限验证返回成功
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            permission.put("status", "published");
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);

            // Mock: 订阅应用列表
            List<String> subscribedApps = Arrays.asList("app-001", "app-002");
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(subscribedApps);

            // Mock: 订阅配置
            Map<String, Object> config1 = new HashMap<>();
            config1.put("channelType", 0);
            config1.put("channelAddress", "queue-app-001");
            config1.put("authType", 1);
            when(apiServerClient.getSubscriptionConfig("app-001", scope)).thenReturn(config1);

            Map<String, Object> config2 = new HashMap<>();
            config2.put("channelType", 1);
            config2.put("channelAddress", "http://example.com/webhook");
            config2.put("authType", 3);
            when(apiServerClient.getSubscriptionConfig("app-002", scope)).thenReturn(config2);

            // Mock: 缓存未命中
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            EventPublishResponse response = eventGatewayService.publishEvent(request);

            // Then
            assertNotNull(response);
            assertEquals(topic, response.getTopic());
            assertEquals(2, response.getSubscribers());
            assertTrue(response.getMessage().contains("Event dispatched"));

            // 验证调用
            verify(apiServerClient).getPermissionByScope(scope);
            verify(apiServerClient).getSubscribedApps(scope);
            verify(messageQueueChannel).sendEvent(eq("queue-app-001"), eq(payload));
            verify(webHookChannel).sendEvent(eq("http://example.com/webhook"), eq(payload), eq("app-002"), eq(AuthTypeEnum.IAM));
        }

        @Test
        @DisplayName("事件资源不存在或未发布")
        void testPublishEvent_ResourceNotFound() {
            // Given
            String scope = "event:im:message-received";
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(null);

            // When
            EventPublishResponse response = eventGatewayService.publishEvent(request);

            // Then
            assertNotNull(response);
            assertEquals("im.message.received", response.getTopic());
            assertEquals(0, response.getSubscribers());
            assertEquals("Event resource not found or not published", response.getMessage());

            // 验证不继续调用订阅查询
            verify(apiServerClient, never()).getSubscribedApps(anyString());
        }

        @Test
        @DisplayName("事件无订阅者")
        void testPublishEvent_NoSubscribers() {
            // Given
            String scope = "event:im:message-received";
            
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(Collections.emptyList());

            // When
            EventPublishResponse response = eventGatewayService.publishEvent(request);

            // Then
            assertNotNull(response);
            assertEquals("im.message.received", response.getTopic());
            assertEquals(0, response.getSubscribers());
            assertEquals("Event has no subscribers", response.getMessage());
        }
    }

    @Nested
    @DisplayName("convertTopicToScope 方法测试")
    class ConvertTopicToScopeTest {

        @Test
        @DisplayName("标准两段格式转换")
        void testConvertTopicToScope_TwoParts() throws Exception {
            // Given
            Method method = EventGatewayService.class.getDeclaredMethod("convertTopicToScope", String.class);
            method.setAccessible(true);

            // When & Then
            assertEquals("event:im:message-received", method.invoke(eventGatewayService, "im.message.received"));
            assertEquals("event:calendar:event-created", method.invoke(eventGatewayService, "calendar.event.created"));
            assertEquals("event:workflow:task-completed", method.invoke(eventGatewayService, "workflow.task.completed"));
        }

        @Test
        @DisplayName("三段格式转换")
        void testConvertTopicToScope_ThreeParts() throws Exception {
            // Given
            Method method = EventGatewayService.class.getDeclaredMethod("convertTopicToScope", String.class);
            method.setAccessible(true);

            // When & Then
            assertEquals("event:im:message-received-sent", method.invoke(eventGatewayService, "im.message.received.sent"));
            assertEquals("event:workflow:task-completed-approved", method.invoke(eventGatewayService, "workflow.task.completed.approved"));
        }

        @Test
        @DisplayName("四段及以上格式转换")
        void testConvertTopicToScope_MultipleParts() throws Exception {
            // Given
            Method method = EventGatewayService.class.getDeclaredMethod("convertTopicToScope", String.class);
            method.setAccessible(true);

            // When & Then
            assertEquals("event:im:message-received-sent-delivered", 
                    method.invoke(eventGatewayService, "im.message.received.sent.delivered"));
        }

        @Test
        @DisplayName("空字符串处理")
        void testConvertTopicToScope_EmptyString() throws Exception {
            // Given
            Method method = EventGatewayService.class.getDeclaredMethod("convertTopicToScope", String.class);
            method.setAccessible(true);

            // When & Then
            assertNull(method.invoke(eventGatewayService, ""));
        }

        @Test
        @DisplayName("null 值处理")
        void testConvertTopicToScope_Null() throws Exception {
            // Given
            Method method = EventGatewayService.class.getDeclaredMethod("convertTopicToScope", String.class);
            method.setAccessible(true);

            // When & Then
            assertNull(method.invoke(eventGatewayService, (String) null));
        }

        @Test
        @DisplayName("单段格式处理")
        void testConvertTopicToScope_SinglePart() throws Exception {
            // Given
            Method method = EventGatewayService.class.getDeclaredMethod("convertTopicToScope", String.class);
            method.setAccessible(true);

            // When & Then
            assertEquals("event:test", method.invoke(eventGatewayService, "test"));
        }
    }

    @Nested
    @DisplayName("getSubscribedApps 方法测试")
    class GetSubscribedAppsTest {

        @Test
        @DisplayName("缓存命中 - 直接返回缓存数据")
        void testGetSubscribedApps_CacheHit() throws Exception {
            // Given
            String topic = "im.message.received";
            String cacheKey = "event:subscribers:" + topic;
            List<String> cachedApps = Arrays.asList("app-001", "app-002", "app-003");

            when(valueOperations.get(cacheKey)).thenReturn(cachedApps);

            Method method = EventGatewayService.class.getDeclaredMethod("getSubscribedApps", String.class);
            method.setAccessible(true);

            // When
            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(eventGatewayService, topic);

            // Then
            assertEquals(3, result.size());
            assertEquals("app-001", result.get(0));

            // 验证没有调用 api-server
            verify(apiServerClient, never()).getSubscribedApps(anyString());
        }

        @Test
        @DisplayName("缓存未命中 - 从 api-server 查询并缓存")
        void testGetSubscribedApps_CacheMiss() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String cacheKey = "event:subscribers:" + topic;
            List<String> apps = Arrays.asList("app-001", "app-002");

            when(valueOperations.get(cacheKey)).thenReturn(null);
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(apps);

            Method method = EventGatewayService.class.getDeclaredMethod("getSubscribedApps", String.class);
            method.setAccessible(true);

            // When
            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(eventGatewayService, topic);

            // Then
            assertEquals(2, result.size());
            
            // 验证调用了 api-server
            verify(apiServerClient).getSubscribedApps(scope);
            
            // 验证写入了缓存
            verify(valueOperations).set(eq(cacheKey), eq(apps), eq(300L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("订阅列表为空 - 不缓存空列表")
        void testGetSubscribedApps_EmptyList() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String cacheKey = "event:subscribers:" + topic;

            when(valueOperations.get(cacheKey)).thenReturn(null);
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(Collections.emptyList());

            Method method = EventGatewayService.class.getDeclaredMethod("getSubscribedApps", String.class);
            method.setAccessible(true);

            // When
            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(eventGatewayService, topic);

            // Then
            assertTrue(result.isEmpty());
            
            // 验证不缓存空列表
            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("Redis连接异常 - 应该降级处理而不是直接失败")
        void testGetSubscribedApps_RedisConnectionError() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String cacheKey = "event:subscribers:" + topic;
            List<String> apps = Arrays.asList("app-001", "app-002");

            // 模拟 Redis 抛出连接异常
            when(valueOperations.get(cacheKey)).thenThrow(new RedisConnectionFailureException("Connection refused"));
            // 期望：应该降级到数据库查询
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(apps);

            Method method = EventGatewayService.class.getDeclaredMethod("getSubscribedApps", String.class);
            method.setAccessible(true);

            // When
            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(eventGatewayService, topic);

            // Then - 应该降级到数据库查询，返回结果而不是抛出异常
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains("app-001"));
            assertTrue(result.contains("app-002"));
            
            // 验证降级到数据库查询
            verify(apiServerClient).getSubscribedApps(scope);
        }

        @Test
        @DisplayName("缓存类型错误 - 应该清除错误缓存并重新查询")
        void testGetSubscribedApps_CacheTypeMismatch() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String cacheKey = "event:subscribers:" + topic;
            List<String> apps = Arrays.asList("app-001", "app-002");

            // 模拟缓存中存储了非 List<String> 类型的数据
            when(valueOperations.get(cacheKey)).thenReturn("invalid-type-string");
            // 期望：应该清除缓存并重新查询
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(apps);

            Method method = EventGatewayService.class.getDeclaredMethod("getSubscribedApps", String.class);
            method.setAccessible(true);

            // When
            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(eventGatewayService, topic);

            // Then - 应该清除错误缓存并返回正确结果
            assertNotNull(result);
            assertEquals(2, result.size());
            
            // 验证清除了错误缓存
            verify(redisTemplate).delete(cacheKey);
            // 验证从数据库重新查询
            verify(apiServerClient).getSubscribedApps(scope);
        }

        @Test
        @DisplayName("getSubscribedApps返回null - 应该返回空列表而不是NPE")
        void testGetSubscribedApps_ApiReturnsNull() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String cacheKey = "event:subscribers:" + topic;

            when(valueOperations.get(cacheKey)).thenReturn(null);
            // 模拟 API 返回 null
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(null);

            Method method = EventGatewayService.class.getDeclaredMethod("getSubscribedApps", String.class);
            method.setAccessible(true);

            // When
            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(eventGatewayService, topic);

            // Then - 应该返回空列表，而不是抛出 NPE
            assertNotNull(result);
            assertTrue(result.isEmpty());
            
            // 验证不缓存 null
            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        }
    }

    @Nested
    @DisplayName("distributeEvent 方法测试")
    class DistributeEventTest {

        @Test
        @DisplayName("消息队列分发 - channelType=0")
        void testDistributeEvent_MessageQueue() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String appId = "app-001";
            List<String> appIds = List.of(appId);

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 0);
            config.put("channelAddress", "queue-test-001");
            config.put("authType", 1);

            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            Method method = EventGatewayService.class.getDeclaredMethod("distributeEvent", 
                    String.class, Map.class, List.class);
            method.setAccessible(true);

            // When
            method.invoke(eventGatewayService, topic, payload, appIds);

            // Then
            verify(messageQueueChannel).sendEvent("queue-test-001", payload);
            verify(webHookChannel, never()).sendEvent(anyString(), any(), anyString(), any(AuthTypeEnum.class));
        }

        @Test
        @DisplayName("WebHook分发 - channelType=1")
        void testDistributeEvent_WebHook() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String appId = "app-002";
            List<String> appIds = List.of(appId);

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 1);
            config.put("channelAddress", "http://example.com/webhook");
            config.put("authType", 3);

            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            Method method = EventGatewayService.class.getDeclaredMethod("distributeEvent", 
                    String.class, Map.class, List.class);
            method.setAccessible(true);

            // When
            method.invoke(eventGatewayService, topic, payload, appIds);

            // Then
            verify(webHookChannel).sendEvent(
                    eq("http://example.com/webhook"), 
                    eq(payload), 
                    eq(appId), 
                    eq(AuthTypeEnum.IAM)
            );
            verify(messageQueueChannel, never()).sendEvent(anyString(), any());
        }

        @Test
        @DisplayName("未知通道类型 - 记录警告日志")
        void testDistributeEvent_UnknownChannelType() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String appId = "app-003";
            List<String> appIds = List.of(appId);

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 99); // 未知类型
            config.put("channelAddress", "http://example.com/webhook");

            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            Method method = EventGatewayService.class.getDeclaredMethod("distributeEvent", 
                    String.class, Map.class, List.class);
            method.setAccessible(true);

            // When
            method.invoke(eventGatewayService, topic, payload, appIds);

            // Then - 不应该调用任何通道
            verify(messageQueueChannel, never()).sendEvent(anyString(), any());
            verify(webHookChannel, never()).sendEvent(anyString(), any(), anyString(), any(AuthTypeEnum.class));
        }

        @Test
        @DisplayName("SSE/WebSocket 通道类型 - 不支持")
        void testDistributeEvent_SSENotSupported() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String appId = "app-004";
            List<String> appIds = List.of(appId);

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 2); // 假设2是SSE（不支持）
            config.put("channelAddress", "ws://example.com/ws");

            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            Method method = EventGatewayService.class.getDeclaredMethod("distributeEvent", 
                    String.class, Map.class, List.class);
            method.setAccessible(true);

            // When
            method.invoke(eventGatewayService, topic, payload, appIds);

            // Then - 不应该调用任何通道，事件只支持消息队列和WebHook
            verify(messageQueueChannel, never()).sendEvent(anyString(), any());
            verify(webHookChannel, never()).sendEvent(anyString(), any(), anyString(), any(AuthTypeEnum.class));
        }

        @Test
        @DisplayName("订阅配置为空 - 跳过处理")
        void testDistributeEvent_EmptyConfig() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String appId = "app-005";
            List<String> appIds = List.of(appId);

            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(Collections.emptyMap());

            Method method = EventGatewayService.class.getDeclaredMethod("distributeEvent", 
                    String.class, Map.class, List.class);
            method.setAccessible(true);

            // When
            method.invoke(eventGatewayService, topic, payload, appIds);

            // Then
            verify(messageQueueChannel, never()).sendEvent(anyString(), any());
            verify(webHookChannel, never()).sendEvent(anyString(), any(), anyString(), any(AuthTypeEnum.class));
        }

        @Test
        @DisplayName("channelType 为 null - 不处理")
        void testDistributeEvent_NullChannelType() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String appId = "app-006";
            List<String> appIds = List.of(appId);

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", null);
            config.put("channelAddress", "http://example.com/webhook");

            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            Method method = EventGatewayService.class.getDeclaredMethod("distributeEvent", 
                    String.class, Map.class, List.class);
            method.setAccessible(true);

            // When
            method.invoke(eventGatewayService, topic, payload, appIds);

            // Then
            verify(messageQueueChannel, never()).sendEvent(anyString(), any());
            verify(webHookChannel, never()).sendEvent(anyString(), any(), anyString(), any(AuthTypeEnum.class));
        }

        @Test
        @DisplayName("channelAddress 为 null - 不处理")
        void testDistributeEvent_NullChannelAddress() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String appId = "app-007";
            List<String> appIds = List.of(appId);

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 0);
            config.put("channelAddress", null);

            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            Method method = EventGatewayService.class.getDeclaredMethod("distributeEvent", 
                    String.class, Map.class, List.class);
            method.setAccessible(true);

            // When
            method.invoke(eventGatewayService, topic, payload, appIds);

            // Then
            verify(messageQueueChannel, never()).sendEvent(anyString(), any());
        }

        @Test
        @DisplayName("异常处理 - 单个应用失败不影响其他应用")
        void testDistributeEvent_ExceptionHandling() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            List<String> appIds = Arrays.asList("app-001", "app-002", "app-003");

            // 第一个应用配置查询抛出异常
            when(apiServerClient.getSubscriptionConfig("app-001", scope))
                    .thenThrow(new RuntimeException("Network error"));

            // 第二个应用正常
            Map<String, Object> config2 = new HashMap<>();
            config2.put("channelType", 0);
            config2.put("channelAddress", "queue-app-002");
            when(apiServerClient.getSubscriptionConfig("app-002", scope)).thenReturn(config2);

            // 第三个应用正常
            Map<String, Object> config3 = new HashMap<>();
            config3.put("channelType", 1);
            config3.put("channelAddress", "http://example.com/webhook");
            config3.put("authType", 1);
            when(apiServerClient.getSubscriptionConfig("app-003", scope)).thenReturn(config3);

            Method method = EventGatewayService.class.getDeclaredMethod("distributeEvent", 
                    String.class, Map.class, List.class);
            method.setAccessible(true);

            // When
            method.invoke(eventGatewayService, topic, payload, appIds);

            // Then - 第二和第三个应用应该正常处理
            verify(messageQueueChannel).sendEvent("queue-app-002", payload);
            verify(webHookChannel).sendEvent(eq("http://example.com/webhook"), eq(payload), eq("app-003"), eq(AuthTypeEnum.SOA));
        }

        @Test
        @DisplayName("认证类型解析 - 各种 AuthType")
        void testDistributeEvent_VariousAuthTypes() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String appId = "app-auth-test";
            List<String> appIds = List.of(appId);

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 1);
            config.put("channelAddress", "http://example.com/webhook");
            config.put("authType", 2); // APIG

            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            Method method = EventGatewayService.class.getDeclaredMethod("distributeEvent", 
                    String.class, Map.class, List.class);
            method.setAccessible(true);

            // When
            method.invoke(eventGatewayService, topic, payload, appIds);

            // Then
            verify(webHookChannel).sendEvent(
                    eq("http://example.com/webhook"), 
                    eq(payload), 
                    eq(appId), 
                    eq(AuthTypeEnum.APIG)
            );
        }

        @Test
        @DisplayName("认证类型为 null - 传递 null")
        void testDistributeEvent_NullAuthType() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String appId = "app-no-auth";
            List<String> appIds = List.of(appId);

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 1);
            config.put("channelAddress", "http://example.com/webhook");
            config.put("authType", null);

            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            Method method = EventGatewayService.class.getDeclaredMethod("distributeEvent", 
                    String.class, Map.class, List.class);
            method.setAccessible(true);

            // When
            method.invoke(eventGatewayService, topic, payload, appIds);

            // Then
            verify(webHookChannel).sendEvent(
                    eq("http://example.com/webhook"), 
                    eq(payload), 
                    eq(appId), 
                    isNull()
            );
        }

        @Test
        @DisplayName("配置channelType为字符串 - 应该安全处理而不是抛出异常")
        void testDistributeEvent_ChannelTypeAsString() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String appId = "app-string-type";
            List<String> appIds = List.of(appId);

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", "1"); // 字符串 "1" 而不是整数 1
            config.put("channelAddress", "http://example.com/webhook");
            config.put("authType", 1);

            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            Method method = EventGatewayService.class.getDeclaredMethod("distributeEvent", 
                    String.class, Map.class, List.class);
            method.setAccessible(true);

            // When
            method.invoke(eventGatewayService, topic, payload, appIds);

            // Then - 应该能够处理字符串类型的 channelType，转换为整数
            verify(webHookChannel).sendEvent(
                    eq("http://example.com/webhook"), 
                    eq(payload), 
                    eq(appId), 
                    eq(AuthTypeEnum.SOA)
            );
            verify(messageQueueChannel, never()).sendEvent(anyString(), any());
        }
    }

    @Nested
    @DisplayName("clearCache 方法测试")
    class ClearCacheTest {

        @Test
        @DisplayName("清除缓存成功")
        void testClearCache_Success() {
            // Given
            String topic = "im.message.received";
            String cacheKey = "event:subscribers:" + topic;

            // When
            eventGatewayService.clearCache(topic);

            // Then
            verify(redisTemplate).delete(cacheKey);
        }

        @Test
        @DisplayName("清除不同 Topic 的缓存")
        void testClearCache_DifferentTopics() {
            // Given
            String topic1 = "calendar.event.created";
            String topic2 = "workflow.task.completed";

            // When
            eventGatewayService.clearCache(topic1);
            eventGatewayService.clearCache(topic2);

            // Then
            verify(redisTemplate).delete("event:subscribers:" + topic1);
            verify(redisTemplate).delete("event:subscribers:" + topic2);
        }
    }

    @Nested
    @DisplayName("verifyEventResource 方法测试")
    class VerifyEventResourceTest {

        @Test
        @DisplayName("验证已发布的事件资源")
        void testVerifyEventResource_Published() throws Exception {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";

            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            permission.put("status", "published");
            permission.put("name", "消息接收事件");

            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);

            Method method = EventGatewayService.class.getDeclaredMethod("verifyEventResource", String.class);
            method.setAccessible(true);

            // When
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) method.invoke(eventGatewayService, topic);

            // Then
            assertNotNull(result);
            assertEquals(scope, result.get("scope"));
            assertEquals("published", result.get("status"));
        }

        @Test
        @DisplayName("事件资源不存在")
        void testVerifyEventResource_NotFound() throws Exception {
            // Given
            String topic = "unknown.event.type";
            String scope = "event:unknown:event-type";

            when(apiServerClient.getPermissionByScope(scope)).thenReturn(null);

            Method method = EventGatewayService.class.getDeclaredMethod("verifyEventResource", String.class);
            method.setAccessible(true);

            // When
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) method.invoke(eventGatewayService, topic);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("事件资源未发布")
        void testVerifyEventResource_NotPublished() throws Exception {
            // Given
            String topic = "draft.event.type";
            String scope = "event:draft:event-type";

            when(apiServerClient.getPermissionByScope(scope)).thenReturn(null);

            Method method = EventGatewayService.class.getDeclaredMethod("verifyEventResource", String.class);
            method.setAccessible(true);

            // When
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) method.invoke(eventGatewayService, topic);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("集成场景测试")
    class IntegrationScenarioTest {

        @Test
        @DisplayName("完整流程 - 多个订阅者使用不同通道")
        void testFullFlow_MultipleSubscribersDifferentChannels() {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";

            // Mock 权限验证
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);

            // Mock 订阅列表（3个应用）
            List<String> subscribedApps = Arrays.asList("mq-app", "webhook-app", "unknown-app");
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(subscribedApps);

            // Mock 各应用的订阅配置
            // MQ 应用
            Map<String, Object> mqConfig = new HashMap<>();
            mqConfig.put("channelType", 0);
            mqConfig.put("channelAddress", "queue-mq-app");
            when(apiServerClient.getSubscriptionConfig("mq-app", scope)).thenReturn(mqConfig);

            // WebHook 应用
            Map<String, Object> webhookConfig = new HashMap<>();
            webhookConfig.put("channelType", 1);
            webhookConfig.put("channelAddress", "http://example.com/hook");
            webhookConfig.put("authType", 3);
            when(apiServerClient.getSubscriptionConfig("webhook-app", scope)).thenReturn(webhookConfig);

            // 未知通道应用
            Map<String, Object> unknownConfig = new HashMap<>();
            unknownConfig.put("channelType", 99);
            unknownConfig.put("channelAddress", "unknown");
            when(apiServerClient.getSubscriptionConfig("unknown-app", scope)).thenReturn(unknownConfig);

            // Mock 缓存
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            EventPublishResponse response = eventGatewayService.publishEvent(request);

            // Then
            assertNotNull(response);
            assertEquals(3, response.getSubscribers());
            assertTrue(response.getMessage().contains("Event dispatched to 3 subscribers"));

            // 验证调用
            verify(messageQueueChannel).sendEvent("queue-mq-app", payload);
            verify(webHookChannel).sendEvent(
                    eq("http://example.com/hook"), 
                    eq(payload), 
                    eq("webhook-app"), 
                    eq(AuthTypeEnum.IAM)
            );
            // 未知通道不应该调用任何通道
        }

        @Test
        @DisplayName("完整流程 - 缓存命中场景")
        void testFullFlow_CacheHit() {
            // Given
            String topic = "im.message.received";
            String scope = "event:im:message-received";
            String cacheKey = "event:subscribers:" + topic;

            // Mock 权限验证
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);

            // Mock 缓存命中
            List<String> cachedApps = Arrays.asList("cached-app-1", "cached-app-2");
            when(valueOperations.get(cacheKey)).thenReturn(cachedApps);

            // Mock 订阅配置
            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 0);
            config.put("channelAddress", "queue-cached");
            when(apiServerClient.getSubscriptionConfig(anyString(), eq(scope))).thenReturn(config);

            // When
            EventPublishResponse response = eventGatewayService.publishEvent(request);

            // Then
            assertEquals(2, response.getSubscribers());

            // 验证没有从 api-server 查询订阅列表（使用了缓存）
            verify(apiServerClient, never()).getSubscribedApps(anyString());
        }
    }
}
