package com.xxx.event.gateway.service;

import com.xxx.event.client.ApiServerClient;
import com.xxx.event.common.auth.AuthTypeEnum;
import com.xxx.event.common.channel.MessageQueueChannel;
import com.xxx.event.common.channel.WebHookChannel;
import com.xxx.event.gateway.dto.EventPublishRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 暴露代码问题的测试
 * 
 * 这些测试用例专门用于暴露代码中的潜在bug
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventGatewayService 问题暴露测试")
class EventGatewayServiceProblemTest {

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

    @BeforeEach
    void setUp() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", "msg-123");
        request = EventPublishRequest.builder()
                .topic("im.message.received")
                .payload(payload)
                .build();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("Redis异常处理问题")
    class RedisExceptionTests {

        @Test
        @DisplayName("Redis连接失败 - 应该降级到数据库查询而不是抛异常")
        void test_RedisConnectionFailure_ShouldDegradeGracefully() {
            // Given: Redis连接失败
            String scope = "event:im:message-received";
            when(valueOperations.get(anyString()))
                    .thenThrow(new RedisConnectionFailureException("Connection refused"));
            
            // Mock API返回
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(Arrays.asList("app-001"));

            // When & Then: 应该不抛异常，降级到API查询
            assertDoesNotThrow(() -> {
                eventGatewayService.publishEvent(request);
            });
            
            // 验证降级到API查询
            verify(apiServerClient).getSubscribedApps(scope);
        }

        @Test
        @DisplayName("Redis超时 - 应该降级处理")
        void test_RedisTimeout_ShouldDegradeGracefully() {
            // Given: Redis超时
            String scope = "event:im:message-received";
            when(valueOperations.get(anyString()))
                    .thenThrow(new QueryTimeoutException("Redis timeout"));
            
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(Collections.emptyList());

            // When & Then
            assertDoesNotThrow(() -> {
                eventGatewayService.publishEvent(request);
            });
        }
    }

    @Nested
    @DisplayName("类型安全问题")
    class TypeSafetyTests {

        @Test
        @DisplayName("缓存中存储了错误类型 - 应该安全处理而不是ClassCastException")
        void test_CacheTypeMismatch_ShouldHandleSafely() {
            // Given: 缓存中存储了字符串而不是List
            String scope = "event:im:message-received";
            when(valueOperations.get(anyString())).thenReturn("invalid-cache-data");
            
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(Arrays.asList("app-001"));

            // When & Then: 应该不抛ClassCastException
            assertDoesNotThrow(() -> {
                eventGatewayService.publishEvent(request);
            });
        }

        @Test
        @DisplayName("配置channelType为字符串 - 应该安全处理")
        void test_ConfigChannelTypeAsString_ShouldHandleSafely() throws Exception {
            // Given: channelType是字符串"1"而不是Integer
            String scope = "event:im:message-received";
            String appId = "app-001";
            
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(Arrays.asList(appId));
            when(valueOperations.get(anyString())).thenReturn(null);

            // 配置中channelType是字符串
            Map<String, Object> config = new HashMap<>();
            config.put("channelType", "1"); // 字符串！
            config.put("channelAddress", "http://example.com/webhook");
            config.put("authType", 1);
            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            // When & Then: 应该不抛ClassCastException
            assertDoesNotThrow(() -> {
                eventGatewayService.publishEvent(request);
            });
        }

        @Test
        @DisplayName("配置channelType为Long - 应该安全处理")
        void test_ConfigChannelTypeAsLong_ShouldHandleSafely() throws Exception {
            // Given: channelType是Long而不是Integer
            String scope = "event:im:message-received";
            String appId = "app-001";
            
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(Arrays.asList(appId));
            when(valueOperations.get(anyString())).thenReturn(null);

            // 配置中channelType是Long (JSON反序列化常见问题)
            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 1L); // Long类型！
            config.put("channelAddress", "http://example.com/webhook");
            config.put("authType", 1);
            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            // When & Then
            assertDoesNotThrow(() -> {
                eventGatewayService.publishEvent(request);
            });
        }
    }

    @Nested
    @DisplayName("Null值处理问题")
    class NullHandlingTests {

        @Test
        @DisplayName("getSubscribedApps返回null - 应该返回空列表而不是NPE")
        void test_ApiReturnsNull_ShouldNotThrowNPE() {
            // Given: API返回null
            String scope = "event:im:message-received";
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(null); // 返回null!

            // When & Then: 应该不抛NPE
            assertDoesNotThrow(() -> {
                eventGatewayService.publishEvent(request);
            });
        }

        @Test
        @DisplayName("配置authType缺失 - 应该使用默认值")
        void test_ConfigMissingAuthType_ShouldUseDefault() throws Exception {
            // Given: 配置中没有authType
            String scope = "event:im:message-received";
            String appId = "app-001";
            
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(Arrays.asList(appId));
            when(valueOperations.get(anyString())).thenReturn(null);

            // 配置中没有authType
            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 1);
            config.put("channelAddress", "http://example.com/webhook");
            // 没有 authType
            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            // When & Then
            assertDoesNotThrow(() -> {
                eventGatewayService.publishEvent(request);
            });
            
            // 验证WebHook被调用（authType为null也能处理）
            verify(webHookChannel).sendEvent(anyString(), any(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("边界条件问题")
    class EdgeCaseTests {

        @Test
        @DisplayName("无效的认证类型code - 应该优雅处理")
        void test_InvalidAuthTypeCode_ShouldHandleGracefully() throws Exception {
            // Given: 无效的authType code
            String scope = "event:im:message-received";
            String appId = "app-001";
            
            Map<String, Object> permission = new HashMap<>();
            permission.put("scope", scope);
            when(apiServerClient.getPermissionByScope(scope)).thenReturn(permission);
            when(apiServerClient.getSubscribedApps(scope)).thenReturn(Arrays.asList(appId));
            when(valueOperations.get(anyString())).thenReturn(null);

            // 配置中authType是无效值
            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 1);
            config.put("channelAddress", "http://example.com/webhook");
            config.put("authType", 999); // 无效值!
            when(apiServerClient.getSubscriptionConfig(appId, scope)).thenReturn(config);

            // When & Then
            assertDoesNotThrow(() -> {
                eventGatewayService.publishEvent(request);
            });
        }

        @Test
        @DisplayName("空的Topic - 应该安全处理")
        void test_EmptyTopic_ShouldHandleSafely() {
            // Given
            EventPublishRequest emptyRequest = EventPublishRequest.builder()
                    .topic("")
                    .payload(new HashMap<>())
                    .build();

            // When & Then
            assertDoesNotThrow(() -> {
                eventGatewayService.publishEvent(emptyRequest);
            });
        }

        @Test
        @DisplayName("Topic只有空格 - 应该安全处理")
        void test_BlankTopic_ShouldHandleSafely() {
            // Given
            EventPublishRequest blankRequest = EventPublishRequest.builder()
                    .topic("   ")
                    .payload(new HashMap<>())
                    .build();

            // When & Then
            assertDoesNotThrow(() -> {
                eventGatewayService.publishEvent(blankRequest);
            });
        }
    }
}
