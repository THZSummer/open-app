package com.xxx.event.common.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE（Server-Sent Events）通道实现
 * 
 * <p>负责管理 SSE 连接，支持向指定连接发送事件/回调消息，以及广播消息到所有连接</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class SseChannel {

    /**
     * 存储活跃的 SSE 连接
     * Key: 连接ID
     * Value: SseEmitter 实例
     */
    private final ConcurrentHashMap<String, SseEmitter> connections = new ConcurrentHashMap<>();

    /**
     * SSE 超时时间：5分钟
     */
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    /**
     * 添加 SSE 连接
     * 
     * <p>创建并返回 SseEmitter，设置超时时间和回调监听器</p>
     * 
     * @param connectionId 连接ID（唯一标识）
     * @return SseEmitter 实例
     */
    public SseEmitter addConnection(String connectionId) {

        // 创建 SseEmitter，设置超时时间
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // 设置完成回调（连接正常关闭）
        emitter.onCompletion(() -> {
            log.info("SSE connection completed: connectionId={}", connectionId);
            connections.remove(connectionId);
        });
        
        // 设置超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout: connectionId={}", connectionId);
            connections.remove(connectionId);
        });
        
        // 设置错误回调
        emitter.onError(throwable -> {
            log.error("SSE connection error: connectionId={}", connectionId, throwable);
            connections.remove(connectionId);
        });
        
        // 存储连接
        SseEmitter oldEmitter = connections.put(connectionId, emitter);
        if (oldEmitter != null) {
            log.warn("SSE connection already exists, will be replaced by new connection: connectionId={}", connectionId);
            oldEmitter.complete();
        }
        
        log.info("SSE connection established: connectionId={}, active connections={}", connectionId, connections.size());
        
        return emitter;
    }

    /**
     * 移除 SSE 连接
     * 
     * @param connectionId 连接ID
     */
    public void removeConnection(String connectionId) {
        SseEmitter emitter = connections.remove(connectionId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE connection removed: connectionId={}, active connections={}", connectionId, connections.size());
        } else {
            log.warn("SSE connection not found, cannot remove: connectionId={}", connectionId);
        }
    }

    /**
     * 向指定连接发送事件消息
     * 
     * @param connectionId 连接ID
     * @param payload 事件内容
     */
    public void sendEvent(String connectionId, Map<String, Object> payload) {
        sendMessage(connectionId, "event", payload);
    }

    /**
     * 向指定连接发送回调消息
     * 
     * @param connectionId 连接ID
     * @param payload 回调内容
     */
    public void sendCallback(String connectionId, Map<String, Object> payload) {
        sendMessage(connectionId, "callback", payload);
    }

    /**
     * 广播事件消息到所有连接
     * 
     * @param payload 事件内容
     */
    public void broadcastEvent(Map<String, Object> payload) {
        log.info("Broadcasting event to all connections: connections={}", connections.size());
        
        connections.forEach((connectionId, emitter) -> {
            try {
                SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                        .name("event")
                        .data(payload);
                emitter.send(eventBuilder);
                log.debug("Broadcast event succeeded: connectionId={}", connectionId);
            } catch (IOException e) {
                log.error("Broadcast event failed: connectionId={}", connectionId, e);
                connections.remove(connectionId);
            }
        });
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
     * 发送消息到指定连接
     * 
     * @param connectionId 连接ID
     * @param eventType 事件类型（event/callback）
     * @param payload 消息内容
     */
    private void sendMessage(String connectionId, String eventType, Map<String, Object> payload) {
        SseEmitter emitter = connections.get(connectionId);
        
        if (emitter == null) {
            log.warn("SSE connection not found, cannot send message: connectionId={}, eventType={}", connectionId, eventType);
            return;
        }
        
        try {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                    .name(eventType)
                    .data(payload);
            emitter.send(eventBuilder);
            log.info("SSE message sent successfully: connectionId={}, eventType={}", connectionId, eventType);
        } catch (IOException e) {
            log.error("SSE message send failed: connectionId={}, eventType={}", connectionId, eventType, e);
            connections.remove(connectionId);
        }
    }
}
