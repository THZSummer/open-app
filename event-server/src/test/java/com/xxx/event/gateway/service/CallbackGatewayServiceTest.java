package com.xxx.event.gateway.service;

import com.xxx.event.client.ApiServerClient;
import com.xxx.event.common.auth.AuthTypeEnum;
import com.xxx.event.common.channel.SseChannel;
import com.xxx.event.common.channel.WebHookChannel;
import com.xxx.event.common.channel.WebSocketChannel;
import com.xxx.event.gateway.dto.CallbackInvokeRequest;
import com.xxx.event.gateway.dto.CallbackInvokeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
 * 回调网关服务单元测试
 * 
 * <p>测试覆盖：</p>
 * <ul>
 *   <li>invokeCallback 方法测试（正常流程、资源不存在、无订阅者）</li>
 *   <li>verifyCallbackResource 方法测试</li>
 *   <li>getSubscribedApps 方法测试（缓存命中、缓存未命中）</li>
 *   <li>distributeCallback 方法测试（WebHook分发、SSE分发、WebSocket分发、配置为空、异常处理）</li>
 *   <li>buildCallbackPayload 方法测试</li>
 *   <li>clearCache 方法测试</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackGatewayServiceTest {

    @Mock
    private ApiServerClient apiServerClient;

    @Mock
    private WebHookChannel webHookChannel;

    @Mock
    private SseChannel sseChannel;

    @Mock
    private WebSocketChannel webSocketChannel;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CallbackGatewayService callbackGatewayService;

    private static final String CACHE_KEY_PREFIX = "callback:subscribers:";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== invokeCallback 测试 ====================

    @Nested
    @DisplayName("invokeCallback 方法测试")
    class InvokeCallbackTests {

        @Test
        @DisplayName("正常流程 - 成功分发回调")
        void testInvokeCallback_Success() {
            // Given
            String callbackScope = "callback:approval:completed";
            Map<String, Object> payload = Map.of(
                    "approvalId", "app001",
                    "status", "approved"
            );

            CallbackInvokeRequest request = CallbackInvokeRequest.builder()
                    .callbackScope(callbackScope)
                    .payload(payload)
                    .build();

            // Mock: 权限存在
            Map<String, Object> permission = Map.of("scope", callbackScope, "name", "审批完成回调");
            when(apiServerClient.getPermissionByScope(callbackScope)).thenReturn(permission);

            // Mock: 订阅者列表
            List<String> subscribedApps = Arrays.asList("app-001", "app-002");
            when(valueOperations.get(CACHE_KEY_PREFIX + callbackScope)).thenReturn(null);
            when(apiServerClient.getSubscribedApps(callbackScope)).thenReturn(subscribedApps);

            // Mock: 订阅配置
            Map<String, Object> config1 = new HashMap<>();
            config1.put("channelType", 0);
            config1.put("channelAddress", "https://example.com/webhook");
            config1.put("authType", 1); // SOA
            when(apiServerClient.getSubscriptionConfig("app-001", callbackScope)).thenReturn(config1);

            Map<String, Object> config2 = new HashMap<>();
            config2.put("channelType", 1);
            config2.put("channelAddress", "sse-connection-001");
            config2.put("authType", 4); // NONE
            when(apiServerClient.getSubscriptionConfig("app-002", callbackScope)).thenReturn(config2);

            // When
            CallbackInvokeResponse response = callbackGatewayService.invokeCallback(request);

            // Then
            assertNotNull(response);
            assertEquals(callbackScope, response.getCallbackScope());
            assertEquals(2, response.getSubscribers());
            assertTrue(response.getMessage().contains("2"));

            // 验证 WebHook 调用
            verify(webHookChannel).sendCallback(
                    eq("https://example.com/webhook"),
                    eq(payload),
                    eq("app-001"),
                    eq(AuthTypeEnum.SOA)
            );

            // 验证 SSE 调用
            verify(sseChannel).sendCallback(
                    eq("sse-connection-001"),
                    any(Map.class)
            );

            // 验证缓存写入
            verify(valueOperations).set(
                    eq(CACHE_KEY_PREFIX + callbackScope),
                    eq(subscribedApps),
                    eq(300L),
                    eq(TimeUnit.SECONDS)
            );
        }

        @Test
        @DisplayName("资源不存在 - 返回错误响应")
        void testInvokeCallback_ResourceNotFound() {
            // Given
            String callbackScope = "callback:not:exist";
            Map<String, Object> payload = Map.of("key", "value");

            CallbackInvokeRequest request = CallbackInvokeRequest.builder()
                    .callbackScope(callbackScope)
                    .payload(payload)
                    .build();

            // Mock: 权限不存在
            when(apiServerClient.getPermissionByScope(callbackScope)).thenReturn(null);

            // When
            CallbackInvokeResponse response = callbackGatewayService.invokeCallback(request);

            // Then
            assertNotNull(response);
            assertEquals(callbackScope, response.getCallbackScope());
            assertEquals(0, response.getSubscribers());
            assertEquals("回调资源不存在", response.getMessage());

            // 验证没有进一步调用
            verify(apiServerClient, never()).getSubscribedApps(anyString());
            verify(webHookChannel, never()).sendCallback(anyString(), any(), anyString(), any());
            verify(sseChannel, never()).sendCallback(anyString(), any());
            verify(webSocketChannel, never()).sendCallback(anyString(), any());
        }

        @Test
        @DisplayName("无订阅者 - 返回成功但订阅者为0")
        void testInvokeCallback_NoSubscribers() {
            // Given
            String callbackScope = "callback:empty:scope";
            Map<String, Object> payload = Map.of("key", "value");

            CallbackInvokeRequest request = CallbackInvokeRequest.builder()
                    .callbackScope(callbackScope)
                    .payload(payload)
                    .build();

            // Mock: 权限存在
            Map<String, Object> permission = Map.of("scope", callbackScope);
            when(apiServerClient.getPermissionByScope(callbackScope)).thenReturn(permission);

            // Mock: 无订阅者
            when(valueOperations.get(CACHE_KEY_PREFIX + callbackScope)).thenReturn(null);
            when(apiServerClient.getSubscribedApps(callbackScope)).thenReturn(Collections.emptyList());

            // When
            CallbackInvokeResponse response = callbackGatewayService.invokeCallback(request);

            // Then
            assertNotNull(response);
            assertEquals(callbackScope, response.getCallbackScope());
            assertEquals(0, response.getSubscribers());
            assertEquals("回调无订阅者", response.getMessage());

            // 验证没有分发调用
            verify(webHookChannel, never()).sendCallback(anyString(), any(), anyString(), any());
            verify(sseChannel, never()).sendCallback(anyString(), any());
            verify(webSocketChannel, never()).sendCallback(anyString(), any());
        }
    }

    // ==================== getSubscribedApps 测试（缓存逻辑）====================

    @Nested
    @DisplayName("getSubscribedApps 方法测试")
    class GetSubscribedAppsTests {

        @Test
        @DisplayName("缓存命中 - 直接返回缓存数据")
        void testGetSubscribedApps_CacheHit() throws Exception {
            // Given
            String callbackScope = "callback:test:scope";
            List<String> cachedApps = Arrays.asList("app-001", "app-002", "app-003");
            when(valueOperations.get(CACHE_KEY_PREFIX + callbackScope)).thenReturn(cachedApps);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod("getSubscribedApps", String.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(callbackGatewayService, callbackScope);

            // Then
            assertEquals(3, result.size());
            assertTrue(result.contains("app-001"));

            // 验证没有调用 API Server
            verify(apiServerClient, never()).getSubscribedApps(anyString());
            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("缓存未命中 - 从API Server获取并缓存")
        void testGetSubscribedApps_CacheMiss() throws Exception {
            // Given
            String callbackScope = "callback:new:scope";
            List<String> apps = Arrays.asList("app-004", "app-005");

            when(valueOperations.get(CACHE_KEY_PREFIX + callbackScope)).thenReturn(null);
            when(apiServerClient.getSubscribedApps(callbackScope)).thenReturn(apps);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod("getSubscribedApps", String.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(callbackGatewayService, callbackScope);

            // Then
            assertEquals(2, result.size());
            assertTrue(result.contains("app-004"));

            // 验证调用了 API Server
            verify(apiServerClient).getSubscribedApps(callbackScope);

            // 验证写入了缓存
            verify(valueOperations).set(
                    eq(CACHE_KEY_PREFIX + callbackScope),
                    eq(apps),
                    eq(300L),
                    eq(TimeUnit.SECONDS)
            );
        }

        @Test
        @DisplayName("缓存未命中且订阅者为空 - 不写入缓存")
        void testGetSubscribedApps_EmptyListNotCached() throws Exception {
            // Given
            String callbackScope = "callback:empty:apps";

            when(valueOperations.get(CACHE_KEY_PREFIX + callbackScope)).thenReturn(null);
            when(apiServerClient.getSubscribedApps(callbackScope)).thenReturn(Collections.emptyList());

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod("getSubscribedApps", String.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(callbackGatewayService, callbackScope);

            // Then
            assertTrue(result.isEmpty());

            // 验证没有写入缓存（空列表不缓存）
            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
        }
    }

    // ==================== distributeCallback 测试 ====================

    @Nested
    @DisplayName("distributeCallback 方法测试")
    class DistributeCallbackTests {

        @Test
        @DisplayName("WebHook分发 - channelType=0")
        void testDistributeCallback_WebHook() throws Exception {
            // Given
            String callbackScope = "callback:webhook:test";
            Map<String, Object> payload = Map.of("key", "value");
            List<String> appIds = List.of("app-001");

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 0);
            config.put("channelAddress", "https://webhook.example.com/callback");
            config.put("authType", 1); // SOA

            when(apiServerClient.getSubscriptionConfig("app-001", callbackScope)).thenReturn(config);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "distributeCallback", String.class, Map.class, List.class);
            method.setAccessible(true);
            method.invoke(callbackGatewayService, callbackScope, payload, appIds);

            // Then
            ArgumentCaptor<AuthTypeEnum> authTypeCaptor = ArgumentCaptor.forClass(AuthTypeEnum.class);
            verify(webHookChannel).sendCallback(
                    eq("https://webhook.example.com/callback"),
                    eq(payload),
                    eq("app-001"),
                    authTypeCaptor.capture()
            );
            assertEquals(AuthTypeEnum.SOA, authTypeCaptor.getValue());

            // 验证其他通道没有被调用
            verify(sseChannel, never()).sendCallback(anyString(), any());
            verify(webSocketChannel, never()).sendCallback(anyString(), any());
        }

        @Test
        @DisplayName("SSE分发 - channelType=1")
        void testDistributeCallback_Sse() throws Exception {
            // Given
            String callbackScope = "callback:sse:test";
            Map<String, Object> payload = Map.of("data", "test-data");
            List<String> appIds = List.of("app-002");

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 1);
            config.put("channelAddress", "sse-conn-12345");
            config.put("authType", 4); // NONE

            when(apiServerClient.getSubscriptionConfig("app-002", callbackScope)).thenReturn(config);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "distributeCallback", String.class, Map.class, List.class);
            method.setAccessible(true);
            method.invoke(callbackGatewayService, callbackScope, payload, appIds);

            // Then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(sseChannel).sendCallback(
                    eq("sse-conn-12345"),
                    payloadCaptor.capture()
            );

            Map<String, Object> sentPayload = payloadCaptor.getValue();
            assertEquals(callbackScope, sentPayload.get("scope"));
            assertEquals(payload, sentPayload.get("data"));
            assertNotNull(sentPayload.get("timestamp"));

            // 验证其他通道没有被调用
            verify(webHookChannel, never()).sendCallback(anyString(), any(), anyString(), any());
            verify(webSocketChannel, never()).sendCallback(anyString(), any());
        }

        @Test
        @DisplayName("WebSocket分发 - channelType=2")
        void testDistributeCallback_WebSocket() throws Exception {
            // Given
            String callbackScope = "callback:websocket:test";
            Map<String, Object> payload = Map.of("message", "hello");
            List<String> appIds = List.of("app-003");

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 2);
            config.put("channelAddress", "ws-conn-67890");
            config.put("authType", 3); // IAM

            when(apiServerClient.getSubscriptionConfig("app-003", callbackScope)).thenReturn(config);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "distributeCallback", String.class, Map.class, List.class);
            method.setAccessible(true);
            method.invoke(callbackGatewayService, callbackScope, payload, appIds);

            // Then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(webSocketChannel).sendCallback(
                    eq("ws-conn-67890"),
                    payloadCaptor.capture()
            );

            Map<String, Object> sentPayload = payloadCaptor.getValue();
            assertEquals(callbackScope, sentPayload.get("scope"));
            assertEquals(payload, sentPayload.get("data"));
            assertNotNull(sentPayload.get("timestamp"));

            // 验证其他通道没有被调用
            verify(webHookChannel, never()).sendCallback(anyString(), any(), anyString(), any());
            verify(sseChannel, never()).sendCallback(anyString(), any());
        }

        @Test
        @DisplayName("配置为空 - 跳过分发")
        void testDistributeCallback_EmptyConfig() throws Exception {
            // Given
            String callbackScope = "callback:empty:config";
            Map<String, Object> payload = Map.of("key", "value");
            List<String> appIds = List.of("app-004");

            when(apiServerClient.getSubscriptionConfig("app-004", callbackScope))
                    .thenReturn(Collections.emptyMap());

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "distributeCallback", String.class, Map.class, List.class);
            method.setAccessible(true);
            method.invoke(callbackGatewayService, callbackScope, payload, appIds);

            // Then - 验证没有调用任何通道
            verify(webHookChannel, never()).sendCallback(anyString(), any(), anyString(), any());
            verify(sseChannel, never()).sendCallback(anyString(), any());
            verify(webSocketChannel, never()).sendCallback(anyString(), any());
        }

        @Test
        @DisplayName("异常处理 - 单个应用异常不影响其他应用")
        void testDistributeCallback_ExceptionHandling() throws Exception {
            // Given
            String callbackScope = "callback:exception:test";
            Map<String, Object> payload = Map.of("key", "value");
            List<String> appIds = Arrays.asList("app-005", "app-006");

            // app-005 配置正常
            Map<String, Object> config1 = new HashMap<>();
            config1.put("channelType", 0);
            config1.put("channelAddress", "https://example.com/webhook");
            config1.put("authType", 1);

            // app-006 会抛出异常
            Map<String, Object> config2 = new HashMap<>();
            config2.put("channelType", 1);
            config2.put("channelAddress", "sse-conn-exception");
            config2.put("authType", 4);

            when(apiServerClient.getSubscriptionConfig("app-005", callbackScope)).thenReturn(config1);
            when(apiServerClient.getSubscriptionConfig("app-006", callbackScope)).thenReturn(config2);

            // SSE 发送时抛出异常
            doThrow(new RuntimeException("SSE connection error"))
                    .when(sseChannel).sendCallback(anyString(), any());

            // When - 使用反射调用私有方法（不应该抛出异常）
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "distributeCallback", String.class, Map.class, List.class);
            method.setAccessible(true);
            method.invoke(callbackGatewayService, callbackScope, payload, appIds);

            // Then - 验证两个应用都被尝试处理
            verify(webHookChannel).sendCallback(
                    eq("https://example.com/webhook"),
                    eq(payload),
                    eq("app-005"),
                    eq(AuthTypeEnum.SOA)
            );
            verify(sseChannel).sendCallback(eq("sse-conn-exception"), any());
        }

        @Test
        @DisplayName("未知通道类型 - 记录警告日志")
        void testDistributeCallback_UnknownChannelType() throws Exception {
            // Given
            String callbackScope = "callback:unknown:channel";
            Map<String, Object> payload = Map.of("key", "value");
            List<String> appIds = List.of("app-007");

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 99); // 未知的通道类型
            config.put("channelAddress", "some-address");
            config.put("authType", 4);

            when(apiServerClient.getSubscriptionConfig("app-007", callbackScope)).thenReturn(config);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "distributeCallback", String.class, Map.class, List.class);
            method.setAccessible(true);
            method.invoke(callbackGatewayService, callbackScope, payload, appIds);

            // Then - 验证没有调用任何通道
            verify(webHookChannel, never()).sendCallback(anyString(), any(), anyString(), any());
            verify(sseChannel, never()).sendCallback(anyString(), any());
            verify(webSocketChannel, never()).sendCallback(anyString(), any());
        }

        @Test
        @DisplayName("channelType为null - 跳过分发")
        void testDistributeCallback_NullChannelType() throws Exception {
            // Given
            String callbackScope = "callback:null:channel";
            Map<String, Object> payload = Map.of("key", "value");
            List<String> appIds = List.of("app-008");

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", null);
            config.put("channelAddress", "some-address");
            config.put("authType", 4);

            when(apiServerClient.getSubscriptionConfig("app-008", callbackScope)).thenReturn(config);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "distributeCallback", String.class, Map.class, List.class);
            method.setAccessible(true);
            method.invoke(callbackGatewayService, callbackScope, payload, appIds);

            // Then - 验证没有调用任何通道
            verify(webHookChannel, never()).sendCallback(anyString(), any(), anyString(), any());
            verify(sseChannel, never()).sendCallback(anyString(), any());
            verify(webSocketChannel, never()).sendCallback(anyString(), any());
        }

        @Test
        @DisplayName("channelAddress为null - 跳过分发")
        void testDistributeCallback_NullChannelAddress() throws Exception {
            // Given
            String callbackScope = "callback:null:address";
            Map<String, Object> payload = Map.of("key", "value");
            List<String> appIds = List.of("app-009");

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 0);
            config.put("channelAddress", null);
            config.put("authType", 1);

            when(apiServerClient.getSubscriptionConfig("app-009", callbackScope)).thenReturn(config);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "distributeCallback", String.class, Map.class, List.class);
            method.setAccessible(true);
            method.invoke(callbackGatewayService, callbackScope, payload, appIds);

            // Then - 验证没有调用任何通道
            verify(webHookChannel, never()).sendCallback(anyString(), any(), anyString(), any());
        }

        @Test
        @DisplayName("多种认证类型 - APIG认证")
        void testDistributeCallback_ApigAuth() throws Exception {
            // Given
            String callbackScope = "callback:apig:test";
            Map<String, Object> payload = Map.of("key", "value");
            List<String> appIds = List.of("app-010");

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 0);
            config.put("channelAddress", "https://apig.example.com/callback");
            config.put("authType", 2); // APIG

            when(apiServerClient.getSubscriptionConfig("app-010", callbackScope)).thenReturn(config);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "distributeCallback", String.class, Map.class, List.class);
            method.setAccessible(true);
            method.invoke(callbackGatewayService, callbackScope, payload, appIds);

            // Then
            ArgumentCaptor<AuthTypeEnum> authTypeCaptor = ArgumentCaptor.forClass(AuthTypeEnum.class);
            verify(webHookChannel).sendCallback(
                    eq("https://apig.example.com/callback"),
                    eq(payload),
                    eq("app-010"),
                    authTypeCaptor.capture()
            );
            assertEquals(AuthTypeEnum.APIG, authTypeCaptor.getValue());
        }

        @Test
        @DisplayName("多种认证类型 - AKSK认证")
        void testDistributeCallback_AkskAuth() throws Exception {
            // Given
            String callbackScope = "callback:aksk:test";
            Map<String, Object> payload = Map.of("key", "value");
            List<String> appIds = List.of("app-011");

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 0);
            config.put("channelAddress", "https://aksk.example.com/callback");
            config.put("authType", 5); // AKSK

            when(apiServerClient.getSubscriptionConfig("app-011", callbackScope)).thenReturn(config);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "distributeCallback", String.class, Map.class, List.class);
            method.setAccessible(true);
            method.invoke(callbackGatewayService, callbackScope, payload, appIds);

            // Then
            ArgumentCaptor<AuthTypeEnum> authTypeCaptor = ArgumentCaptor.forClass(AuthTypeEnum.class);
            verify(webHookChannel).sendCallback(
                    eq("https://aksk.example.com/callback"),
                    eq(payload),
                    eq("app-011"),
                    authTypeCaptor.capture()
            );
            assertEquals(AuthTypeEnum.AKSK, authTypeCaptor.getValue());
        }

        @Test
        @DisplayName("authType为null - 使用null作为认证类型")
        void testDistributeCallback_NullAuthType() throws Exception {
            // Given
            String callbackScope = "callback:null:auth";
            Map<String, Object> payload = Map.of("key", "value");
            List<String> appIds = List.of("app-012");

            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 0);
            config.put("channelAddress", "https://example.com/callback");
            config.put("authType", null);

            when(apiServerClient.getSubscriptionConfig("app-012", callbackScope)).thenReturn(config);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "distributeCallback", String.class, Map.class, List.class);
            method.setAccessible(true);
            method.invoke(callbackGatewayService, callbackScope, payload, appIds);

            // Then
            verify(webHookChannel).sendCallback(
                    eq("https://example.com/callback"),
                    eq(payload),
                    eq("app-012"),
                    isNull()
            );
        }
    }

    // ==================== buildCallbackPayload 测试 ====================

    @Nested
    @DisplayName("buildCallbackPayload 方法测试")
    class BuildCallbackPayloadTests {

        @Test
        @DisplayName("构建回调消息体 - 包含scope、timestamp、data")
        void testBuildCallbackPayload() throws Exception {
            // Given
            String callbackScope = "callback:test:payload";
            Map<String, Object> payload = Map.of(
                    "field1", "value1",
                    "field2", 123,
                    "nested", Map.of("key", "value")
            );

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "buildCallbackPayload", String.class, Map.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) method.invoke(
                    callbackGatewayService, callbackScope, payload);

            // Then
            assertNotNull(result);
            assertEquals(callbackScope, result.get("scope"));
            assertNotNull(result.get("timestamp"));
            assertTrue(result.get("timestamp") instanceof Long);
            assertEquals(payload, result.get("data"));
        }

        @Test
        @DisplayName("构建回调消息体 - 空payload")
        void testBuildCallbackPayload_EmptyPayload() throws Exception {
            // Given
            String callbackScope = "callback:empty:payload";
            Map<String, Object> payload = Collections.emptyMap();

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "buildCallbackPayload", String.class, Map.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) method.invoke(
                    callbackGatewayService, callbackScope, payload);

            // Then
            assertNotNull(result);
            assertEquals(callbackScope, result.get("scope"));
            assertNotNull(result.get("timestamp"));
            assertEquals(Collections.emptyMap(), result.get("data"));
        }
    }

    // ==================== clearCache 测试 ====================

    @Nested
    @DisplayName("clearCache 方法测试")
    class ClearCacheTests {

        @Test
        @DisplayName("清除指定scope的缓存")
        void testClearCache() {
            // Given
            String callbackScope = "callback:clear:test";

            // When
            callbackGatewayService.clearCache(callbackScope);

            // Then
            verify(redisTemplate).delete(CACHE_KEY_PREFIX + callbackScope);
        }

        @Test
        @DisplayName("清除缓存 - 不同scope")
        void testClearCache_DifferentScopes() {
            // Given
            String scope1 = "callback:scope:one";
            String scope2 = "callback:scope:two";

            // When
            callbackGatewayService.clearCache(scope1);
            callbackGatewayService.clearCache(scope2);

            // Then
            verify(redisTemplate).delete(CACHE_KEY_PREFIX + scope1);
            verify(redisTemplate).delete(CACHE_KEY_PREFIX + scope2);
            verify(redisTemplate, times(2)).delete(anyString());
        }
    }

    // ==================== verifyCallbackResource 测试 ====================

    @Nested
    @DisplayName("verifyCallbackResource 方法测试")
    class VerifyCallbackResourceTests {

        @Test
        @DisplayName("验证回调资源 - 资源存在")
        void testVerifyCallbackResource_Exists() throws Exception {
            // Given
            String callbackScope = "callback:exists:test";
            Map<String, Object> expectedPermission = Map.of(
                    "scope", callbackScope,
                    "name", "测试回调",
                    "type", "callback"
            );
            when(apiServerClient.getPermissionByScope(callbackScope)).thenReturn(expectedPermission);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "verifyCallbackResource", String.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) method.invoke(
                    callbackGatewayService, callbackScope);

            // Then
            assertNotNull(result);
            assertEquals(expectedPermission, result);
            verify(apiServerClient).getPermissionByScope(callbackScope);
        }

        @Test
        @DisplayName("验证回调资源 - 资源不存在")
        void testVerifyCallbackResource_NotExists() throws Exception {
            // Given
            String callbackScope = "callback:not:exists";
            when(apiServerClient.getPermissionByScope(callbackScope)).thenReturn(null);

            // When - 使用反射调用私有方法
            Method method = CallbackGatewayService.class.getDeclaredMethod(
                    "verifyCallbackResource", String.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) method.invoke(
                    callbackGatewayService, callbackScope);

            // Then
            assertNull(result);
            verify(apiServerClient).getPermissionByScope(callbackScope);
        }
    }

    // ==================== 集成场景测试 ====================

    @Nested
    @DisplayName("集成场景测试")
    class IntegrationTests {

        @Test
        @DisplayName("完整流程 - 多应用多通道类型")
        void testFullFlow_MultipleAppsMultipleChannels() {
            // Given
            String callbackScope = "callback:full:flow";
            Map<String, Object> payload = Map.of(
                    "eventId", "evt-001",
                    "action", "created"
            );

            CallbackInvokeRequest request = CallbackInvokeRequest.builder()
                    .callbackScope(callbackScope)
                    .payload(payload)
                    .build();

            // Mock: 权限存在
            when(apiServerClient.getPermissionByScope(callbackScope))
                    .thenReturn(Map.of("scope", callbackScope));

            // Mock: 三个订阅者，分别使用不同通道
            List<String> subscribedApps = Arrays.asList("webhook-app", "sse-app", "websocket-app");
            when(valueOperations.get(CACHE_KEY_PREFIX + callbackScope)).thenReturn(null);
            when(apiServerClient.getSubscribedApps(callbackScope)).thenReturn(subscribedApps);

            // WebHook 配置
            Map<String, Object> webhookConfig = new HashMap<>();
            webhookConfig.put("channelType", 0);
            webhookConfig.put("channelAddress", "https://webhook.example.com/callback");
            webhookConfig.put("authType", 1);
            when(apiServerClient.getSubscriptionConfig("webhook-app", callbackScope))
                    .thenReturn(webhookConfig);

            // SSE 配置
            Map<String, Object> sseConfig = new HashMap<>();
            sseConfig.put("channelType", 1);
            sseConfig.put("channelAddress", "sse-connection-abc");
            sseConfig.put("authType", 4);
            when(apiServerClient.getSubscriptionConfig("sse-app", callbackScope))
                    .thenReturn(sseConfig);

            // WebSocket 配置
            Map<String, Object> wsConfig = new HashMap<>();
            wsConfig.put("channelType", 2);
            wsConfig.put("channelAddress", "ws-connection-xyz");
            wsConfig.put("authType", 3);
            when(apiServerClient.getSubscriptionConfig("websocket-app", callbackScope))
                    .thenReturn(wsConfig);

            // When
            CallbackInvokeResponse response = callbackGatewayService.invokeCallback(request);

            // Then
            assertNotNull(response);
            assertEquals(callbackScope, response.getCallbackScope());
            assertEquals(3, response.getSubscribers());

            // 验证三种通道都被调用
            verify(webHookChannel).sendCallback(
                    eq("https://webhook.example.com/callback"),
                    eq(payload),
                    eq("webhook-app"),
                    eq(AuthTypeEnum.SOA)
            );
            verify(sseChannel).sendCallback(eq("sse-connection-abc"), any(Map.class));
            verify(webSocketChannel).sendCallback(eq("ws-connection-xyz"), any(Map.class));
        }

        @Test
        @DisplayName("缓存命中场景 - 完整流程")
        void testFullFlow_CacheHit() {
            // Given
            String callbackScope = "callback:cache:hit";
            Map<String, Object> payload = Map.of("key", "value");

            CallbackInvokeRequest request = CallbackInvokeRequest.builder()
                    .callbackScope(callbackScope)
                    .payload(payload)
                    .build();

            // Mock: 权限存在
            when(apiServerClient.getPermissionByScope(callbackScope))
                    .thenReturn(Map.of("scope", callbackScope));

            // Mock: 缓存命中
            List<String> cachedApps = List.of("cached-app");
            when(valueOperations.get(CACHE_KEY_PREFIX + callbackScope)).thenReturn(cachedApps);

            // Mock: 订阅配置
            Map<String, Object> config = new HashMap<>();
            config.put("channelType", 0);
            config.put("channelAddress", "https://cached.example.com/callback");
            config.put("authType", 1);
            when(apiServerClient.getSubscriptionConfig("cached-app", callbackScope))
                    .thenReturn(config);

            // When
            CallbackInvokeResponse response = callbackGatewayService.invokeCallback(request);

            // Then
            assertEquals(1, response.getSubscribers());

            // 验证没有调用 API Server 获取订阅列表（因为缓存命中）
            verify(apiServerClient, never()).getSubscribedApps(callbackScope);

            // 验证没有写入缓存
            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
        }
    }
}
