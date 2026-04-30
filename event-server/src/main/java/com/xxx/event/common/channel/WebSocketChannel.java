package com.xxx.event.common.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 通道实现
 * 
 * <p>负责管理 WebSocket 会话，支持向指定连接发送事件/回调消息，以及广播消息到所有连接</p>
 * 
 * <p>功能特性：</p>
 * <ul>
 *   <li>连接管理：添加、移除、查询连接</li>
 *   <li>消息发送：向指定连接发送事件/回调消息</li>
 *   <li>广播消息：向所有连接广播事件消息</li>
 *   <li>线程安全：使用 ConcurrentHashMap 存储连接</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class WebSocketChannel {

    /**
     * 存储活跃的 WebSocket 会话
     * Key: 连接ID（connectionId）
     * Value: WebSocketSession 实例
     */
    private final ConcurrentHashMap<String, WebSocketSession> connections = new ConcurrentHashMap<>();

    /**
     * JSON 序列化器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 添加 WebSocket 连接
     * 
     * <p>如果连接已存在，旧连接将被关闭并替换</p>
     * 
     * @param connectionId 连接ID（唯一标识）
     * @param session WebSocket 会话
     */
    public void addConnection(String connectionId, WebSocketSession session) {
        WebSocketSession oldSession = connections.put(connectionId, session);
        
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close();
                log.warn("WebSocket connection already exists, old connection closed: connectionId={}", connectionId);
            } catch (IOException e) {
                log.error("Failed to close old WebSocket connection: connectionId={}", connectionId, e);
            }
        }
        
        log.info("WebSocket connection added: connectionId={}, current connections={}", 
                connectionId, connections.size());
    }

    /**
     * 移除 WebSocket 连接
     * 
     * @param connectionId 连接ID
     */
    public void removeConnection(String connectionId) {
        WebSocketSession session = connections.remove(connectionId);
        
        if (session != null) {
            try {
                if (session.isOpen()) {
                    session.close();
                }
                log.info("WebSocket connection removed: connectionId={}, current connections={}", 
                        connectionId, connections.size());
            } catch (IOException e) {
                log.error("Failed to close WebSocket connection: connectionId={}", connectionId, e);
            }
        } else {
            log.warn("WebSocket connection not found, cannot remove: connectionId={}", connectionId);
        }
    }

    /**
     * 向指定连接发送事件消息
     * 
     * <p>消息格式：</p>
     * <pre>
     * {
     *   "type": "event",
     *   "data": { ... }
     * }
     * </pre>
     * 
     * @param connectionId 连接ID
     * @param payload 事件内容
     * @return 是否发送成功
     */
    public boolean sendEvent(String connectionId, Map<String, Object> payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "event");
        message.put("data", payload);
        
        return sendMessage(connectionId, message);
    }

    /**
     * 向指定连接发送回调消息
     * 
     * <p>消息格式：</p>
     * <pre>
     * {
     *   "type": "callback",
     *   "data": { ... }
     * }
     * </pre>
     * 
     * @param connectionId 连接ID
     * @param payload 回调内容
     * @return 是否发送成功
     */
    public boolean sendCallback(String connectionId, Map<String, Object> payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "callback");
        message.put("data", payload);
        
        return sendMessage(connectionId, message);
    }

    /**
     * 广播事件消息到所有连接
     * 
     * <p>遍历所有连接，发送事件消息</p>
     * 
     * @param payload 事件内容
     */
    public void broadcastEvent(Map<String, Object> payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "event");
        message.put("data", payload);
        
        log.info("Broadcasting event to all connections: connections={}", connections.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Map.Entry<String, WebSocketSession> entry : connections.entrySet()) {
            String connectionId = entry.getKey();
            
            if (sendMessageInternal(entry.getValue(), message)) {
                successCount++;
            } else {
                failCount++;

                // 发送失败，移除连接
                connections.remove(connectionId);
            }
        }
        
        log.info("Broadcast event completed: success={}, failed={}", successCount, failCount);
    }

    /**
     * 广播回调消息到所有连接
     * 
     * <p>遍历所有连接，发送回调消息</p>
     * 
     * @param payload 回调内容
     */
    public void broadcastCallback(Map<String, Object> payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "callback");
        message.put("data", payload);
        
        log.info("Broadcasting callback to all connections: connections={}", connections.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Map.Entry<String, WebSocketSession> entry : connections.entrySet()) {
            String connectionId = entry.getKey();
            
            if (sendMessageInternal(entry.getValue(), message)) {
                successCount++;
            } else {
                failCount++;

                // 发送失败，移除连接
                connections.remove(connectionId);
            }
        }
        
        log.info("Broadcast callback completed: success={}, failed={}", successCount, failCount);
    }

    /**
     * 获取活跃连接数
     * 
     * @return 当前活跃的连接数
     */
    public int getActiveConnectionCount() {
        return connections.size();
    }

    /**
     * 检查连接是否存在
     * 
     * @param connectionId 连接ID
     * @return 是否存在
     */
    public boolean hasConnection(String connectionId) {
        WebSocketSession session = connections.get(connectionId);
        return session != null && session.isOpen();
    }

    /**
     * 获取所有连接ID
     * 
     * @return 连接ID集合
     */
    public java.util.Set<String> getConnectionIds() {
        return new java.util.HashSet<>(connections.keySet());
    }

    /**
     * 发送消息到指定连接
     * 
     * @param connectionId 连接ID
     * @param message 消息内容
     * @return 是否发送成功
     */
    private boolean sendMessage(String connectionId, Map<String, Object> message) {
        WebSocketSession session = connections.get(connectionId);
        
        if (session == null) {
            log.warn("WebSocket connection not found, cannot send message: connectionId={}", connectionId);
            return false;
        }
        
        return sendMessageInternal(session, message);
    }

    /**
     * 内部发送消息方法
     * 
     * @param session WebSocket 会话
     * @param message 消息内容
     * @return 是否发送成功
     */
    private boolean sendMessageInternal(WebSocketSession session, Map<String, Object> message) {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket session unavailable: sessionId={}", session != null ? session.getId() : "null");
            return false;
        }
        
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            
            log.debug("WebSocket message sent successfully: sessionId={}", session.getId());
            return true;
            
        } catch (IOException e) {
            log.error("Failed to send WebSocket message: sessionId={}", session.getId(), e);
            return false;
        }
    }
}
