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
            log.info("SSE 连接完成: connectionId={}", connectionId);
            connections.remove(connectionId);
        });
        
        // 设置超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时: connectionId={}", connectionId);
            connections.remove(connectionId);
        });
        
        // 设置错误回调
        emitter.onError(throwable -> {
            log.error("SSE 连接异常: connectionId={}", connectionId, throwable);
            connections.remove(connectionId);
        });
        
        // 存储连接
        SseEmitter oldEmitter = connections.put(connectionId, emitter);
        if (oldEmitter != null) {
            log.warn("SSE 连接已存在，将被新连接替换: connectionId={}", connectionId);
            oldEmitter.complete();
        }
        
        log.info("SSE 连接已建立: connectionId={}, 当前活跃连接数={}", connectionId, connections.size());
        
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
            log.info("SSE 连接已移除: connectionId={}, 当前活跃连接数={}", connectionId, connections.size());
        } else {
            log.warn("SSE 连接不存在，无法移除: connectionId={}", connectionId);
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
        log.info("广播事件到所有连接: 连接数={}", connections.size());
        
        connections.forEach((connectionId, emitter) -> {
            try {
                SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                        .name("event")
                        .data(payload);
                emitter.send(eventBuilder);
                log.debug("广播事件成功: connectionId={}", connectionId);
            } catch (IOException e) {
                log.error("广播事件失败: connectionId={}", connectionId, e);
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
            log.warn("SSE 连接不存在，无法发送消息: connectionId={}, eventType={}", connectionId, eventType);
            return;
        }
        
        try {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                    .name(eventType)
                    .data(payload);
            emitter.send(eventBuilder);
            log.info("SSE 消息发送成功: connectionId={}, eventType={}", connectionId, eventType);
        } catch (IOException e) {
            log.error("SSE 消息发送失败: connectionId={}, eventType={}", connectionId, eventType, e);
            connections.remove(connectionId);
        }
    }
}
