package com.xxx.event.common.channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

/**
 * WebSocket 处理器
 * 
 * <p>处理 WebSocket 连接的生命周期事件：</p>
 * <ul>
 *   <li>连接建立：提取 connectionId 并注册到 WebSocketChannel</li>
 *   <li>消息接收：处理客户端发送的消息</li>
 *   <li>连接关闭：从 WebSocketChannel 中移除连接</li>
 *   <li>异常处理：记录错误日志</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final WebSocketChannel webSocketChannel;

    /**
     * 连接建立后调用
     * 
     * <p>从 URL 路径中提取 connectionId 并注册到 WebSocketChannel</p>
     * <p>URL 格式：/ws/{connectionId}</p>
     * 
     * @param session WebSocket 会话
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String connectionId = extractConnectionId(session);
        
        if (connectionId == null || connectionId.isEmpty()) {
            log.warn("WebSocket connection missing connectionId, closing connection: sessionId={}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        
        // 注册连接
        webSocketChannel.addConnection(connectionId, session);
        
        log.info("WebSocket connection established: connectionId={}, sessionId={}, remoteAddress={}", 
                connectionId, session.getId(), session.getRemoteAddress());
    }

    /**
     * 接收到消息时调用
     * 
     * <p>处理客户端发送的文本消息</p>
     * 
     * @param session WebSocket 会话
     * @param message 文本消息
     * @throws Exception 异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String connectionId = extractConnectionId(session);
        String payload = message.getPayload();
        
        log.debug("WebSocket received message: connectionId={}, payload={}", connectionId, payload);
        
        // 心跳响应（支持 ping/pong）
        if ("ping".equalsIgnoreCase(payload) || "heartbeat".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("pong"));
            log.debug("WebSocket heartbeat response: connectionId={}", connectionId);
            return;
        }
        
        // 其他消息处理（可根据业务需求扩展）
        // 例如：解析 JSON 消息，根据类型分发处理
        log.info("WebSocket message received: connectionId={}, message={}", connectionId, payload);
    }

    /**
     * 连接关闭后调用
     * 
     * <p>从 WebSocketChannel 中移除连接</p>
     * 
     * @param session WebSocket 会话
     * @param status 关闭状态
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String connectionId = extractConnectionId(session);
        
        if (connectionId != null) {
            webSocketChannel.removeConnection(connectionId);
            log.info("WebSocket connection closed: connectionId={}, sessionId={}, status={}", 
                    connectionId, session.getId(), status);
        }
    }

    /**
     * 传输错误时调用
     * 
     * <p>记录错误日志，连接会被自动关闭</p>
     * 
     * @param session WebSocket 会话
     * @param exception 异常
     * @throws Exception 异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String connectionId = extractConnectionId(session);
        
        log.error("WebSocket transport error: connectionId={}, sessionId={}, error={}", 
                connectionId, session.getId(), exception.getMessage(), exception);
        
        // 移除连接
        if (connectionId != null) {
            webSocketChannel.removeConnection(connectionId);
        }
    }

    /**
     * 是否支持分片消息
     * 
     * @return false - 不支持分片消息
     */
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 从 WebSocket 会话中提取 connectionId
     * 
     * <p>URL 格式：/ws/{connectionId}</p>
     * 
     * @param session WebSocket 会话
     * @return connectionId，提取失败返回 null
     */
    private String extractConnectionId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) {
                return null;
            }
            
            String path = uri.getPath();

            // 路径格式：/ws/{connectionId}
            if (path != null && path.startsWith("/ws/")) {
                return path.substring(4); // 去掉 "/ws/" 前缀
            }
            
            return null;
        } catch (Exception e) {
            log.error("Failed to extract connectionId: sessionId={}", session.getId(), e);
            return null;
        }
    }
}
