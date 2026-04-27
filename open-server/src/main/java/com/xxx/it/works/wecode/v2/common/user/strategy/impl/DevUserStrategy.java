package com.xxx.it.works.wecode.v2.common.user.strategy.impl;

import com.xxx.it.works.wecode.v2.common.user.strategy.UserResolveStrategy;
import com.xxx.it.works.wecode.v2.common.enums.AuthTypeEnum;
import com.xxx.it.works.wecode.v2.common.model.UserContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 开发环境用户解析策略
 * 
 * <p>从 Cookie 中解析 user_id 字段</p>
 * <p>Cookie 格式示例: user_id=api_admin</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class DevUserStrategy implements UserResolveStrategy {

    /**
     * Cookie 中用户ID的键名
     */
    private static final String USER_ID_COOKIE = "user_id";

    @Override
    public UserContext resolve(HttpServletRequest request) {
        // 从 Cookie 解析 user_id
        String userId = extractCookieValue(request, USER_ID_COOKIE);
        
        if (userId == null || userId.isEmpty()) {
            log.debug("未从 Cookie 中找到 user_id");
            return null;
        }
        
        log.debug("从 Cookie 解析到用户ID: {}", userId);
        
        // 构建用户上下文
        return UserContext.builder()
                .userId(userId)
                .userName(userId) // 开发环境默认使用 userId 作为 userName
                .authType(AuthTypeEnum.COOKIE)
                .build();
    }

    @Override
    public boolean supports(String activeProfile) {
        // 开发环境激活: dev, development, local
        return "dev".equals(activeProfile) 
                || "development".equals(activeProfile)
                || "local".equals(activeProfile);
    }

    /**
     * 从请求中提取指定名称的 Cookie 值
     * 
     * @param request HTTP 请求
     * @param cookieName Cookie 名称
     * @return Cookie 值，未找到返回 null
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