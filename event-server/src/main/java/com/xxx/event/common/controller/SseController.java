package com.xxx.event.common.controller;

import com.xxx.event.common.channel.SseChannel;
import com.xxx.event.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * SSE（Server-Sent Events）连接控制器
 * 
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET /sse/connect/{connectionId} - 建立 SSE 连接</li>
 *   <li>DELETE /sse/disconnect/{connectionId} - 断开 SSE 连接</li>
 *   <li>GET /sse/status - 查询连接状态</li>
 * </ul>
 * 
 * <p>SSE 特点：单向推送（服务器 → 客户端），适合实时通知场景</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Tag(name = "SSE 连接", description = "Server-Sent Events 连接管理")
@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseChannel sseChannel;

    /**
     * 建立 SSE 连接
     * 
     * <p>客户端通过此端点建立 SSE 连接，返回 text/event-stream</p>
     * <p>连接建立后，服务器可主动推送事件/回调消息到客户端</p>
     * 
     * <p>使用说明：</p>
     * <ol>
     *   <li>客户端生成唯一的 connectionId（建议使用 UUID）</li>
     *   <li>调用此接口建立连接</li>
     *   <li>连接保持 5 分钟，超时或断开后需重新建立</li>
     *   <li>订阅事件时，将此 connectionId 作为 channel_address</li>
     * </ol>
     */
    @Operation(summary = "建立 SSE 连接", description = "客户端通过此端点建立 SSE 长连接，接收服务器推送的事件和回调")
    @GetMapping(value = "/connect/{connectionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @Parameter(description = "连接ID（唯一标识，建议使用 UUID）", required = true)
            @PathVariable String connectionId) {
        
        log.info("Received SSE connection request: connectionId={}", connectionId);
        
        return sseChannel.addConnection(connectionId);
    }

    /**
     * 手动断开 SSE 连接
     * 
     * <p>客户端主动断开连接时调用</p>
     */
    @Operation(summary = "断开 SSE 连接", description = "手动断开指定的 SSE 连接")
    @DeleteMapping("/disconnect/{connectionId}")
    public ApiResponse<Void> disconnect(
            @Parameter(description = "连接ID", required = true)
            @PathVariable String connectionId) {
        
        log.info("Received SSE disconnect request: connectionId={}", connectionId);
        
        sseChannel.removeConnection(connectionId);
        
        return ApiResponse.success();
    }

    /**
     * 查询连接状态
     * 
     * <p>返回当前活跃的 SSE 连接数</p>
     */
    @Operation(summary = "查询连接状态", description = "查询当前活跃的 SSE 连接数")
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        log.info("Querying SSE connection status");
        
        Map<String, Object> data = new HashMap<>();
        data.put("activeConnections", sseChannel.getActiveConnectionCount());
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiResponse.success(data);
    }
}
