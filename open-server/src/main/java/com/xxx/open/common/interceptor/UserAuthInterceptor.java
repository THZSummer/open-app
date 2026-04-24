package com.xxx.open.common.interceptor;

import com.xxx.open.common.auth.strategy.UserResolveStrategy;
import com.xxx.open.common.context.UserContextHolder;
import com.xxx.open.common.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 用户认证拦截器
 * 
 * <p>拦截 HTTP 请求，解析用户信息并存入 ThreadLocal</p>
 * <p>根据 spring.profiles.active 选择对应的解析策略</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthInterceptor implements HandlerInterceptor {

    private final List<UserResolveStrategy> strategies;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        try {
            // 1. 选择并执行策略
            UserContext userContext = resolveUser(request);
            
            // 2. 存入 ThreadLocal
            UserContextHolder.set(userContext);
            
            // 3. 记录日志
            logUserContext(userContext);
            
            return true;
        } catch (Exception e) {
            log.warn("用户信息解析失败，使用默认用户: {}", e.getMessage());
            // 解析失败时使用默认用户
            UserContextHolder.set(UserContext.empty());
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, 
                                HttpServletResponse response, 
                                Object handler, 
                                Exception ex) {
        // 清理 ThreadLocal，防止内存泄漏
        UserContextHolder.clear();
    }

    /**
     * 解析用户信息
     * 按策略优先级选择支持的策略执行解析
     * 
     * @param request HTTP 请求
     * @return 用户上下文，解析失败返回默认系统用户
     */
    private UserContext resolveUser(HttpServletRequest request) {
        return strategies.stream()
                .filter(s -> s.supports(activeProfile))
                .map(s -> s.resolve(request))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(UserContext.empty());
    }

    /**
     * 记录用户上下文日志
     * 
     * @param context 用户上下文
     */
    private void logUserContext(UserContext context) {
        if (log.isDebugEnabled()) {
            log.debug("用户上下文: userId={}, userName={}, authType={}", 
                    context.getUserId(), 
                    context.getUserName(), 
                    context.getAuthType());
        }
    }
}