package com.xxx.it.works.wecode.v2.common.interceptor;

import com.xxx.it.works.wecode.v2.common.user.strategy.UserResolveStrategy;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Objects;

/**
 * User Resolve Interceptor
 *
 * <p>Intercepts HTTP requests, resolves user info and stores in ThreadLocal</p>
 * <p>Selects corresponding resolve strategy based on spring.profiles.active</p>
 * <p>User authentication in standard environment is handled by other classes</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserResolveInterceptor implements HandlerInterceptor {

    private final List<UserResolveStrategy> strategies;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
        try {
            // 1. Select and execute strategy
            UserContext userContext = resolveUser(request);

            // 2. Store in ThreadLocal
            UserContextHolder.set(userContext);

            // 3. Log user context
            logUserContext(userContext);

            return true;
        } catch (Exception e) {
            log.warn("Failed to resolve user info, using default user: {}", e.getMessage());
            // Use default user when resolution fails
            UserContextHolder.set(UserContext.empty());
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // Clear ThreadLocal to prevent memory leak
        UserContextHolder.clear();
    }

    /**
     * Resolve user info
     * Selects and executes the first supported strategy by priority
     *
     * @param request HTTP request
     * @return User context, returns default system user if resolution fails
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
     * Log user context
     *
     * @param context User context
     */
    private void logUserContext(UserContext context) {
        if (log.isDebugEnabled()) {
            log.debug("User context: userId={}, userName={}, authType={}",
                    context.getUserId(),
                    context.getUserName(),
                    context.getAuthType());
        }
    }
}
