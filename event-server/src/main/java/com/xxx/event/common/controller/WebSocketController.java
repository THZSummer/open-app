package com.xxx.event.common.controller;

import com.xxx.event.common.channel.WebSocketChannel;
import com.xxx.event.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket 管理控制器
 * 
 * <p>提供 WebSocket 连接状态查询接口</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Tag(name = "WebSocket 管理", description = "WebSocket 连接状态查询接口")
@RestController
@RequestMapping("/ws")
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketChannel webSocketChannel;

    /**
     * 查询 WebSocket 连接状态
     * 
     * <p>返回当前活跃的连接数和连接ID列表</p>
     * 
     * @return 连接状态信息
     */
    @Operation(summary = "查询 WebSocket 连接状态", description = "返回当前活跃的连接数和连接ID列表")
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        Set<String> connectionIds = webSocketChannel.getConnectionIds();
        int activeCount = webSocketChannel.getActiveConnectionCount();
        
        Map<String, Object> data = new HashMap<>();
        data.put("activeConnectionCount", activeCount);
        data.put("connectionIds", connectionIds);
        
        return ApiResponse.success(data);
    }

    /**
     * 查询 WebSocket 连接数
     * 
     * <p>仅返回当前活跃的连接数</p>
     * 
     * @return 连接数
     */
    @Operation(summary = "查询 WebSocket 连接数", description = "返回当前活跃的连接数")
    @GetMapping("/count")
    public ApiResponse<Map<String, Object>> getCount() {
        int activeCount = webSocketChannel.getActiveConnectionCount();
        
        Map<String, Object> data = new HashMap<>();
        data.put("activeConnectionCount", activeCount);
        
        return ApiResponse.success(data);
    }
}
