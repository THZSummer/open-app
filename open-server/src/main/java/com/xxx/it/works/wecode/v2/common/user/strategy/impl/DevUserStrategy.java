package com.xxx.it.works.wecode.v2.common.user.strategy.impl;

import com.xxx.it.works.wecode.v2.common.user.strategy.UserResolveStrategy;
import com.xxx.it.works.wecode.v2.common.enums.AuthTypeEnum;
import com.xxx.it.works.wecode.v2.common.model.UserContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Dev environment user resolution strategy
 *
 * <p>Parse user_id field from Cookie</p>
 * <p>Cookie format example: user_id=api_admin</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class DevUserStrategy implements UserResolveStrategy {

    /**
     * Cookie key name for user ID
     */
    private static final String USER_ID_COOKIE = "user_id";

    @Override
    public UserContext resolve(HttpServletRequest request) {
        // Parse user_id from Cookie
        String userId = extractCookieValue(request, USER_ID_COOKIE);

        if (userId == null || userId.isEmpty()) {
            log.debug("user_id not found in Cookie");
            return null;
        }

        log.debug("Parsed user ID from Cookie: {}", userId);

        // Build user context
        return UserContext.builder()
                .userId(userId)
                .userName(userId) // Dev environment uses userId as userName by default
                .authType(AuthTypeEnum.COOKIE)
                .build();
    }

    @Override
    public boolean supports(String activeProfile) {
        // Dev environment activation: dev, development, local
        return "dev".equals(activeProfile)
                || "development".equals(activeProfile)
                || "local".equals(activeProfile);
    }

    /**
     * Extract specified Cookie value from request
     *
     * @param request HTTP request
     * @param cookieName Cookie name
     * @return Cookie value, returns null if not found
     */
    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
