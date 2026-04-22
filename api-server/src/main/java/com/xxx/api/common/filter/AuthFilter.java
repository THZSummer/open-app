package com.xxx.api.common.filter;

import com.xxx.api.common.util.SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 认证过滤器
 * 
 * <p>拦截需要认证的请求，验证应用身份</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@Order(1)
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        log.debug("AuthFilter: path={}, method={}", path, method);
        
        // 跳过不需要认证的路径
        if (shouldSkip(path)) {
            chain.doFilter(request, response);
            return;
        }
        
        // 提取认证信息
        String appId = httpRequest.getHeader("X-App-Id");
        String authType = httpRequest.getHeader("X-Auth-Type");
        String authorization = httpRequest.getHeader("Authorization");
        
        // 验证必要参数
        if (appId == null || appId.isEmpty()) {
            log.warn("缺少应用ID: path={}", path);
            sendError(httpResponse, 401, "缺少应用ID");
            return;
        }
        
        if (authType == null || authType.isEmpty()) {
            log.warn("缺少认证类型: path={}", path);
            sendError(httpResponse, 401, "缺少认证类型");
            return;
        }
        
        if (authorization == null || authorization.isEmpty()) {
            log.warn("缺少认证凭证: path={}", path);
            sendError(httpResponse, 401, "缺少认证凭证");
            return;
        }
        
        // 验证应用身份
        boolean authenticated = false;
        try {
            int authTypeInt = Integer.parseInt(authType);
            
            switch (authTypeInt) {
                case 0: // AKSK 认证
                    String timestamp = httpRequest.getHeader("X-Timestamp");
                    String nonce = httpRequest.getHeader("X-Nonce");
                    String signature = httpRequest.getHeader("X-Signature");
                    
                    // Mock: 简单验证
                    authenticated = validateAKSKMock(appId, timestamp, nonce, signature);
                    break;
                    
                case 1: // Bearer Token 认证
                    authenticated = SignatureUtil.verifyBearerToken(authorization);
                    break;
                    
                default:
                    log.warn("不支持的认证类型: authType={}", authType);
                    sendError(httpResponse, 401, "不支持的认证类型");
                    return;
            }
            
        } catch (Exception e) {
            log.error("认证验证失败", e);
            sendError(httpResponse, 401, "认证验证失败");
            return;
        }
        
        if (!authenticated) {
            log.warn("应用身份验证失败: appId={}, path={}", appId, path);
            sendError(httpResponse, 401, "应用身份验证失败");
            return;
        }
        
        log.info("应用身份验证通过: appId={}, path={}", appId, path);
        
        // 继续处理请求
        chain.doFilter(request, response);
    }
    
    /**
     * 判断是否跳过认证
     */
    private boolean shouldSkip(String path) {
        // 跳过健康检查、Swagger、API 文档等路径
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/health");
    }
    
    /**
     * 发送错误响应
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String json = String.format(
                "{\"code\":\"%d\",\"messageZh\":\"%s\",\"messageEn\":\"Unauthorized\",\"data\":null,\"page\":null}",
                status, message);
        response.getWriter().write(json);
    }
    
    /**
     * Mock AKSK 验证
     * 
     * <p>实际项目中应该调用应用管理系统验证 AKSK</p>
     */
    private boolean validateAKSKMock(String appId, String timestamp, String nonce, String signature) {
        // Mock: 简单验证参数不为空
        return appId != null && !appId.isEmpty() &&
               timestamp != null && !timestamp.isEmpty() &&
               nonce != null && !nonce.isEmpty() &&
               signature != null && !signature.isEmpty();
    }
}
