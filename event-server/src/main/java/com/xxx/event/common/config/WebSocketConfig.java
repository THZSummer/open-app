package com.xxx.event.common.config;

import com.xxx.event.common.channel.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类
 * 
 * <p>启用 WebSocket 支持，注册 WebSocket 端点</p>
 * 
 * <p>端点路径：/ws/{connectionId}</p>
 * <ul>
 *   <li>connectionId: 连接唯一标识，由客户端生成或服务端分配</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;

    /**
     * 注册 WebSocket 处理器
     * 
     * <p>端点配置：</p>
     * <ul>
     *   <li>路径：/ws/{connectionId}</li>
     *   <li>跨域：开发环境允许所有来源，生产环境应配置具体域名</li>
     * </ul>
     * 
     * @param registry WebSocket 处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/*")
                .setAllowedOrigins("*");
    }
}
