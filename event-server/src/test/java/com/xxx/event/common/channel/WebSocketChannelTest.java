package com.xxx.event.common.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * WebSocketChannel 单元测试
 * 
 * <p>测试 WebSocket 通道的所有功能：</p>
 * <ul>
 *   <li>连接管理：添加、移除、查询连接</li>
 *   <li>消息发送：事件消息、回调消息</li>
 *   <li>广播消息：广播事件、广播回调</li>
 *   <li>状态查询：连接数、连接存在性、连接ID列表</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WebSocketChannel 单元测试")
class WebSocketChannelTest {

    private WebSocketChannel webSocketChannel;

    @Mock
    private WebSocketSession mockSession1;

    @Mock
    private WebSocketSession mockSession2;

    @Mock
    private WebSocketSession mockSession3;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        webSocketChannel = new WebSocketChannel();
        objectMapper = new ObjectMapper();
        
        // 使用 lenient 标记这些 stubbing，允许不被使用（解决 Mockito 严格模式问题）
        lenient().when(mockSession1.getId()).thenReturn("session-1");
        lenient().when(mockSession2.getId()).thenReturn("session-2");
        lenient().when(mockSession3.getId()).thenReturn("session-3");
    }

    // ==================== addConnection 测试 ====================

    @Nested
    @DisplayName("addConnection 方法测试")
    class AddConnectionTests {

        @Test
        @DisplayName("添加新连接 - 成功")
        void testAddNewConnection_Success() {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);

            // When
            webSocketChannel.addConnection(connectionId, mockSession1);

            // Then
            assertEquals(1, webSocketChannel.getActiveConnectionCount());
            assertTrue(webSocketChannel.hasConnection(connectionId));
        }

        @Test
        @DisplayName("添加多个不同连接 - 成功")
        void testAddMultipleConnections_Success() {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);
            when(mockSession3.isOpen()).thenReturn(true);

            // When
            webSocketChannel.addConnection("conn-001", mockSession1);
            webSocketChannel.addConnection("conn-002", mockSession2);
            webSocketChannel.addConnection("conn-003", mockSession3);

            // Then
            assertEquals(3, webSocketChannel.getActiveConnectionCount());
            assertTrue(webSocketChannel.hasConnection("conn-001"));
            assertTrue(webSocketChannel.hasConnection("conn-002"));
            assertTrue(webSocketChannel.hasConnection("conn-003"));
        }

        @Test
        @DisplayName("替换已存在的连接 - 旧连接被关闭")
        void testReplaceExistingConnection_OldConnectionClosed() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);

            // 先添加第一个连接
            webSocketChannel.addConnection(connectionId, mockSession1);

            // When - 添加相同 connectionId 的新连接
            webSocketChannel.addConnection(connectionId, mockSession2);

            // Then
            assertEquals(1, webSocketChannel.getActiveConnectionCount());
            assertTrue(webSocketChannel.hasConnection(connectionId));
            
            // 验证旧连接被关闭
            verify(mockSession1).close();
        }

        @Test
        @DisplayName("替换已关闭的旧连接 - 不调用 close")
        void testReplaceClosedConnection_NoCloseCall() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(false); // 旧连接已关闭
            when(mockSession2.isOpen()).thenReturn(true);

            // 先添加第一个连接
            webSocketChannel.addConnection(connectionId, mockSession1);

            // When - 添加相同 connectionId 的新连接
            webSocketChannel.addConnection(connectionId, mockSession2);

            // Then
            assertEquals(1, webSocketChannel.getActiveConnectionCount());
            
            // 验证旧连接未被关闭（因为已经关闭了）
            verify(mockSession1, never()).close();
        }

        @Test
        @DisplayName("关闭旧连接时抛出异常 - 不影响新连接")
        void testCloseOldConnectionException_DoesNotAffectNewConnection() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);
            doThrow(new IOException("Close failed")).when(mockSession1).close();

            // 先添加第一个连接
            webSocketChannel.addConnection(connectionId, mockSession1);

            // When - 添加相同 connectionId 的新连接（即使关闭旧连接会抛异常）
            webSocketChannel.addConnection(connectionId, mockSession2);

            // Then - 新连接仍然成功添加
            assertEquals(1, webSocketChannel.getActiveConnectionCount());
            assertTrue(webSocketChannel.hasConnection(connectionId));
        }
    }

    // ==================== removeConnection 测试 ====================

    @Nested
    @DisplayName("removeConnection 方法测试")
    class RemoveConnectionTests {

        @Test
        @DisplayName("移除存在的连接 - 成功")
        void testRemoveExistingConnection_Success() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            webSocketChannel.addConnection(connectionId, mockSession1);

            // When
            webSocketChannel.removeConnection(connectionId);

            // Then
            assertEquals(0, webSocketChannel.getActiveConnectionCount());
            assertFalse(webSocketChannel.hasConnection(connectionId));
            verify(mockSession1).close();
        }

        @Test
        @DisplayName("移除已关闭的连接 - 不调用 close")
        void testRemoveClosedConnection_NoCloseCall() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            webSocketChannel.addConnection(connectionId, mockSession1);
            
            // 会话已关闭
            when(mockSession1.isOpen()).thenReturn(false);

            // When
            webSocketChannel.removeConnection(connectionId);

            // Then
            assertEquals(0, webSocketChannel.getActiveConnectionCount());

            // 验证 close 未被调用（因为会话已关闭）
            verify(mockSession1, never()).close();
        }

        @Test
        @DisplayName("移除不存在的连接 - 不抛异常")
        void testRemoveNonExistentConnection_NoException() {

            // Given
            String connectionId = "conn-999";

            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> webSocketChannel.removeConnection(connectionId));
            assertEquals(0, webSocketChannel.getActiveConnectionCount());
        }

        @Test
        @DisplayName("关闭连接时抛出异常 - 连接仍被移除")
        void testCloseConnectionException_ConnectionStillRemoved() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            doThrow(new IOException("Close failed")).when(mockSession1).close();
            webSocketChannel.addConnection(connectionId, mockSession1);

            // When
            webSocketChannel.removeConnection(connectionId);

            // Then - 连接仍被移除
            assertEquals(0, webSocketChannel.getActiveConnectionCount());
            assertFalse(webSocketChannel.hasConnection(connectionId));
        }
    }

    // ==================== sendEvent 测试 ====================

    @Nested
    @DisplayName("sendEvent 方法测试")
    class SendEventTests {

        @Test
        @DisplayName("发送事件消息 - 连接存在且打开")
        void testSendEvent_ConnectionExistsAndOpen() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            webSocketChannel.addConnection(connectionId, mockSession1);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "user.created");
            payload.put("userId", "123");

            // When
            boolean result = webSocketChannel.sendEvent(connectionId, payload);

            // Then
            assertTrue(result);
            
            // 验证发送的消息内容
            ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
            verify(mockSession1).sendMessage(messageCaptor.capture());
            
            String sentJson = messageCaptor.getValue().getPayload();
            Map<String, Object> sentMessage = objectMapper.readValue(sentJson, Map.class);
            
            assertEquals("event", sentMessage.get("type"));
            assertNotNull(sentMessage.get("data"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> sentData = (Map<String, Object>) sentMessage.get("data");
            assertEquals("user.created", sentData.get("event"));
            assertEquals("123", sentData.get("userId"));
        }

        @Test
        @DisplayName("发送事件消息 - 连接不存在")
        void testSendEvent_ConnectionNotExists() {

            // Given
            String connectionId = "conn-999";
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "test");

            // When
            boolean result = webSocketChannel.sendEvent(connectionId, payload);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("发送事件消息 - 连接已关闭")
        void testSendEvent_ConnectionClosed() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            webSocketChannel.addConnection(connectionId, mockSession1);
            
            // 连接已关闭
            when(mockSession1.isOpen()).thenReturn(false);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "test");

            // When
            boolean result = webSocketChannel.sendEvent(connectionId, payload);

            // Then
            assertFalse(result);
            verify(mockSession1, never()).sendMessage(any());
        }

        @Test
        @DisplayName("发送事件消息 - 发送时抛出 IO 异常")
        void testSendEvent_IOException() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            doThrow(new IOException("Send failed")).when(mockSession1).sendMessage(any());
            webSocketChannel.addConnection(connectionId, mockSession1);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "test");

            // When
            boolean result = webSocketChannel.sendEvent(connectionId, payload);

            // Then
            assertFalse(result);
        }
    }

    // ==================== sendCallback 测试 ====================

    @Nested
    @DisplayName("sendCallback 方法测试")
    class SendCallbackTests {

        @Test
        @DisplayName("发送回调消息 - 连接存在且打开")
        void testSendCallback_ConnectionExistsAndOpen() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            webSocketChannel.addConnection(connectionId, mockSession1);

            Map<String, Object> payload = new HashMap<>();
            payload.put("callbackId", "cb-123");
            payload.put("result", "success");

            // When
            boolean result = webSocketChannel.sendCallback(connectionId, payload);

            // Then
            assertTrue(result);
            
            // 验证发送的消息内容
            ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
            verify(mockSession1).sendMessage(messageCaptor.capture());
            
            String sentJson = messageCaptor.getValue().getPayload();
            Map<String, Object> sentMessage = objectMapper.readValue(sentJson, Map.class);
            
            assertEquals("callback", sentMessage.get("type"));
            assertNotNull(sentMessage.get("data"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> sentData = (Map<String, Object>) sentMessage.get("data");
            assertEquals("cb-123", sentData.get("callbackId"));
            assertEquals("success", sentData.get("result"));
        }

        @Test
        @DisplayName("发送回调消息 - 连接不存在")
        void testSendCallback_ConnectionNotExists() {

            // Given
            String connectionId = "conn-999";
            Map<String, Object> payload = new HashMap<>();
            payload.put("callbackId", "cb-123");

            // When
            boolean result = webSocketChannel.sendCallback(connectionId, payload);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("发送回调消息 - 连接已关闭")
        void testSendCallback_ConnectionClosed() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            webSocketChannel.addConnection(connectionId, mockSession1);
            
            // 连接已关闭
            when(mockSession1.isOpen()).thenReturn(false);

            Map<String, Object> payload = new HashMap<>();
            payload.put("callbackId", "cb-123");

            // When
            boolean result = webSocketChannel.sendCallback(connectionId, payload);

            // Then
            assertFalse(result);
            verify(mockSession1, never()).sendMessage(any());
        }

        @Test
        @DisplayName("发送回调消息 - 发送时抛出 IO 异常")
        void testSendCallback_IOException() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            doThrow(new IOException("Send failed")).when(mockSession1).sendMessage(any());
            webSocketChannel.addConnection(connectionId, mockSession1);

            Map<String, Object> payload = new HashMap<>();
            payload.put("callbackId", "cb-123");

            // When
            boolean result = webSocketChannel.sendCallback(connectionId, payload);

            // Then
            assertFalse(result);
        }
    }

    // ==================== broadcastEvent 测试 ====================

    @Nested
    @DisplayName("broadcastEvent 方法测试")
    class BroadcastEventTests {

        @Test
        @DisplayName("广播事件消息 - 多个连接全部成功")
        void testBroadcastEvent_AllSuccess() throws IOException {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);
            when(mockSession3.isOpen()).thenReturn(true);
            
            webSocketChannel.addConnection("conn-001", mockSession1);
            webSocketChannel.addConnection("conn-002", mockSession2);
            webSocketChannel.addConnection("conn-003", mockSession3);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "system.maintenance");
            payload.put("message", "System will restart in 5 minutes");

            // When
            webSocketChannel.broadcastEvent(payload);

            // Then - 所有连接都收到消息
            verify(mockSession1).sendMessage(any(TextMessage.class));
            verify(mockSession2).sendMessage(any(TextMessage.class));
            verify(mockSession3).sendMessage(any(TextMessage.class));
            
            // 所有连接仍然存在
            assertEquals(3, webSocketChannel.getActiveConnectionCount());
        }

        @Test
        @DisplayName("广播事件消息 - 无连接")
        void testBroadcastEvent_NoConnections() {

            // Given - 无连接
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "test");

            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> webSocketChannel.broadcastEvent(payload));
        }

        @Test
        @DisplayName("广播事件消息 - 部分连接失败")
        void testBroadcastEvent_PartialFailure() throws IOException {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);
            when(mockSession3.isOpen()).thenReturn(true);
            
            webSocketChannel.addConnection("conn-001", mockSession1);
            webSocketChannel.addConnection("conn-002", mockSession2);
            webSocketChannel.addConnection("conn-003", mockSession3);
            
            // Session2 发送失败
            doThrow(new IOException("Send failed")).when(mockSession2).sendMessage(any());

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "test");

            // When
            webSocketChannel.broadcastEvent(payload);

            // Then - 失败的连接被移除
            assertEquals(2, webSocketChannel.getActiveConnectionCount());
            assertTrue(webSocketChannel.hasConnection("conn-001"));
            assertFalse(webSocketChannel.hasConnection("conn-002"));
            assertTrue(webSocketChannel.hasConnection("conn-003"));
        }

        @Test
        @DisplayName("广播事件消息 - 验证 JSON 格式")
        void testBroadcastEvent_VerifyJsonFormat() throws IOException {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            webSocketChannel.addConnection("conn-001", mockSession1);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "user.login");
            payload.put("timestamp", System.currentTimeMillis());

            // When
            webSocketChannel.broadcastEvent(payload);

            // Then
            ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
            verify(mockSession1).sendMessage(messageCaptor.capture());
            
            String sentJson = messageCaptor.getValue().getPayload();
            Map<String, Object> sentMessage = objectMapper.readValue(sentJson, Map.class);
            
            assertEquals("event", sentMessage.get("type"));
            assertNotNull(sentMessage.get("data"));
        }
    }

    // ==================== broadcastCallback 测试 ====================

    @Nested
    @DisplayName("broadcastCallback 方法测试")
    class BroadcastCallbackTests {

        @Test
        @DisplayName("广播回调消息 - 多个连接全部成功")
        void testBroadcastCallback_AllSuccess() throws IOException {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);
            
            webSocketChannel.addConnection("conn-001", mockSession1);
            webSocketChannel.addConnection("conn-002", mockSession2);

            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "refresh");
            payload.put("reason", "data-updated");

            // When
            webSocketChannel.broadcastCallback(payload);

            // Then - 所有连接都收到消息
            verify(mockSession1).sendMessage(any(TextMessage.class));
            verify(mockSession2).sendMessage(any(TextMessage.class));
            
            assertEquals(2, webSocketChannel.getActiveConnectionCount());
        }

        @Test
        @DisplayName("广播回调消息 - 无连接")
        void testBroadcastCallback_NoConnections() {

            // Given - 无连接
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "refresh");

            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> webSocketChannel.broadcastCallback(payload));
        }

        @Test
        @DisplayName("广播回调消息 - 部分连接失败")
        void testBroadcastCallback_PartialFailure() throws IOException {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);
            
            webSocketChannel.addConnection("conn-001", mockSession1);
            webSocketChannel.addConnection("conn-002", mockSession2);
            
            // Session1 发送失败
            doThrow(new IOException("Send failed")).when(mockSession1).sendMessage(any());

            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "test");

            // When
            webSocketChannel.broadcastCallback(payload);

            // Then - 失败的连接被移除
            assertEquals(1, webSocketChannel.getActiveConnectionCount());
            assertFalse(webSocketChannel.hasConnection("conn-001"));
            assertTrue(webSocketChannel.hasConnection("conn-002"));
        }

        @Test
        @DisplayName("广播回调消息 - 验证 JSON 格式")
        void testBroadcastCallback_VerifyJsonFormat() throws IOException {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            webSocketChannel.addConnection("conn-001", mockSession1);

            Map<String, Object> payload = new HashMap<>();
            payload.put("callbackId", "cb-global");
            payload.put("status", "completed");

            // When
            webSocketChannel.broadcastCallback(payload);

            // Then
            ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
            verify(mockSession1).sendMessage(messageCaptor.capture());
            
            String sentJson = messageCaptor.getValue().getPayload();
            Map<String, Object> sentMessage = objectMapper.readValue(sentJson, Map.class);
            
            assertEquals("callback", sentMessage.get("type"));
            assertNotNull(sentMessage.get("data"));
        }
    }

    // ==================== getActiveConnectionCount 测试 ====================

    @Nested
    @DisplayName("getActiveConnectionCount 方法测试")
    class GetActiveConnectionCountTests {

        @Test
        @DisplayName("获取连接数 - 无连接")
        void testGetActiveConnectionCount_NoConnections() {

            // When
            int count = webSocketChannel.getActiveConnectionCount();

            // Then
            assertEquals(0, count);
        }

        @Test
        @DisplayName("获取连接数 - 有多个连接")
        void testGetActiveConnectionCount_MultipleConnections() {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);
            
            webSocketChannel.addConnection("conn-001", mockSession1);
            webSocketChannel.addConnection("conn-002", mockSession2);

            // When
            int count = webSocketChannel.getActiveConnectionCount();

            // Then
            assertEquals(2, count);
        }

        @Test
        @DisplayName("获取连接数 - 添加和移除后")
        void testGetActiveConnectionCount_AfterAddAndRemove() {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);
            
            webSocketChannel.addConnection("conn-001", mockSession1);
            webSocketChannel.addConnection("conn-002", mockSession2);
            webSocketChannel.removeConnection("conn-001");

            // When
            int count = webSocketChannel.getActiveConnectionCount();

            // Then
            assertEquals(1, count);
        }
    }

    // ==================== hasConnection 测试 ====================

    @Nested
    @DisplayName("hasConnection 方法测试")
    class HasConnectionTests {

        @Test
        @DisplayName("检查连接 - 存在且打开")
        void testHasConnection_ExistsAndOpen() {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            webSocketChannel.addConnection("conn-001", mockSession1);

            // When
            boolean exists = webSocketChannel.hasConnection("conn-001");

            // Then
            assertTrue(exists);
        }

        @Test
        @DisplayName("检查连接 - 存在但已关闭")
        void testHasConnection_ExistsButClosed() {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            webSocketChannel.addConnection("conn-001", mockSession1);
            
            // 会话已关闭
            when(mockSession1.isOpen()).thenReturn(false);

            // When
            boolean exists = webSocketChannel.hasConnection("conn-001");

            // Then
            assertFalse(exists);
        }

        @Test
        @DisplayName("检查连接 - 不存在")
        void testHasConnection_NotExists() {

            // When
            boolean exists = webSocketChannel.hasConnection("conn-999");

            // Then
            assertFalse(exists);
        }
    }

    // ==================== getConnectionIds 测试 ====================

    @Nested
    @DisplayName("getConnectionIds 方法测试")
    class GetConnectionIdsTests {

        @Test
        @DisplayName("获取所有连接ID - 无连接")
        void testGetConnectionIds_NoConnections() {

            // When
            Set<String> connectionIds = webSocketChannel.getConnectionIds();

            // Then
            assertNotNull(connectionIds);
            assertTrue(connectionIds.isEmpty());
        }

        @Test
        @DisplayName("获取所有连接ID - 有多个连接")
        void testGetConnectionIds_MultipleConnections() {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);
            when(mockSession3.isOpen()).thenReturn(true);
            
            webSocketChannel.addConnection("conn-001", mockSession1);
            webSocketChannel.addConnection("conn-002", mockSession2);
            webSocketChannel.addConnection("conn-003", mockSession3);

            // When
            Set<String> connectionIds = webSocketChannel.getConnectionIds();

            // Then
            assertNotNull(connectionIds);
            assertEquals(3, connectionIds.size());
            assertTrue(connectionIds.contains("conn-001"));
            assertTrue(connectionIds.contains("conn-002"));
            assertTrue(connectionIds.contains("conn-003"));
        }

        @Test
        @DisplayName("获取所有连接ID - 修改返回集合不影响内部状态")
        void testGetConnectionIds_ModifyReturnedSetDoesNotAffectInternalState() {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            webSocketChannel.addConnection("conn-001", mockSession1);

            // When
            Set<String> connectionIds = webSocketChannel.getConnectionIds();
            connectionIds.clear(); // 尝试修改返回的集合

            // Then - 内部状态不受影响
            Set<String> connectionIdsAfter = webSocketChannel.getConnectionIds();
            assertEquals(1, connectionIdsAfter.size());
            assertTrue(connectionIdsAfter.contains("conn-001"));
        }
    }

    // ==================== 集成测试 ====================

    @Nested
    @DisplayName("集成场景测试")
    class IntegrationTests {

        @Test
        @DisplayName("完整生命周期 - 添加、发送、移除")
        void testFullLifecycle() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);

            // When - 添加连接
            webSocketChannel.addConnection(connectionId, mockSession1);
            
            // Then - 连接存在
            assertTrue(webSocketChannel.hasConnection(connectionId));
            assertEquals(1, webSocketChannel.getActiveConnectionCount());

            // When - 发送事件
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "test");
            boolean sendResult = webSocketChannel.sendEvent(connectionId, payload);
            
            // Then - 发送成功
            assertTrue(sendResult);

            // When - 移除连接
            webSocketChannel.removeConnection(connectionId);
            
            // Then - 连接不存在
            assertFalse(webSocketChannel.hasConnection(connectionId));
            assertEquals(0, webSocketChannel.getActiveConnectionCount());
        }

        @Test
        @DisplayName("并发场景 - 多次添加相同连接")
        void testConcurrentAddSameConnection() throws IOException {

            // Given
            String connectionId = "conn-001";
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);

            // When - 连续添加相同 connectionId 的不同 session
            webSocketChannel.addConnection(connectionId, mockSession1);
            webSocketChannel.addConnection(connectionId, mockSession2);

            // Then - 只有最新的连接存在
            assertEquals(1, webSocketChannel.getActiveConnectionCount());
            
            // 验证旧连接被关闭
            verify(mockSession1).close();
        }

        @Test
        @DisplayName("复杂场景 - 混合操作")
        void testComplexScenario() throws IOException {

            // Given
            when(mockSession1.isOpen()).thenReturn(true);
            when(mockSession2.isOpen()).thenReturn(true);
            when(mockSession3.isOpen()).thenReturn(true);

            // When - 添加三个连接
            webSocketChannel.addConnection("conn-001", mockSession1);
            webSocketChannel.addConnection("conn-002", mockSession2);
            webSocketChannel.addConnection("conn-003", mockSession3);

            // Then - 验证初始状态
            assertEquals(3, webSocketChannel.getActiveConnectionCount());

            // When - 广播事件
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("event", "broadcast");
            webSocketChannel.broadcastEvent(eventPayload);

            // Then - 所有连接都收到消息
            verify(mockSession1).sendMessage(any(TextMessage.class));
            verify(mockSession2).sendMessage(any(TextMessage.class));
            verify(mockSession3).sendMessage(any(TextMessage.class));

            // When - 移除一个连接
            webSocketChannel.removeConnection("conn-002");

            // Then - 连接数减少
            assertEquals(2, webSocketChannel.getActiveConnectionCount());
            assertFalse(webSocketChannel.hasConnection("conn-002"));
            assertTrue(webSocketChannel.hasConnection("conn-001"));
            assertTrue(webSocketChannel.hasConnection("conn-003"));
        }
    }
}
