package com.xxx.open.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.open.common.config.MockConfig;
import com.xxx.open.common.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Mock 拦截器
 * 
 * <p>当 Mock 启用时，拦截特定请求并返回 Mock 数据</p>
 * 
 * <p>设计参考：ADR-002 Mock 策略实现第三方依赖隔离</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockInterceptor implements HandlerInterceptor {

    private final MockConfig mockConfig;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Mock 未启用，放行请求
        if (!mockConfig.isEnabled()) {
            return true;
        }

        // Mock 已启用，记录日志
        String uri = request.getRequestURI();
        String method = request.getMethod();
        log.info("[Mock] 拦截请求: {} {}", method, uri);

        // 根据请求路径返回 Mock 数据（后续扩展）
        // 目前先放行，由具体的 Mock Service 实现
        return true;
    }
}
