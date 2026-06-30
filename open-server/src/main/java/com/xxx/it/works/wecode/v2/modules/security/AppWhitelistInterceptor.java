package com.xxx.it.works.wecode.v2.modules.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 应用白名单准入拦截器（v2.0.1）
 *
 * <p>通过 X-App-Id Header 获取应用外部 ID（String），直接校验应用是否在连接器平台白名单内。
 * 非白名单应用返回 403 Forbidden。</p>
 *
 * <p>拦截路径范围：
 * <ul>
 *   <li>/service/open/v2/connectors/**</li>
 *   <li>/service/open/v2/flows/**</li>
 * </ul>
 *
 * <p><b>v2.0.1 变更：</b>
 * Header 值直接以 String 透传给白名单服务，不再做 Long.parseLong 类型转换。
 * 因为白名单数据源（Lookup item_value / Spring 属性）存的本身就是字符串，
 * String → Long → String.valueOf() 是完全冗余的。</p>
 *
 * <p><b>v2.0.0 变更：</b>
 * 本拦截器仅负责平台级白名单准入校验，不再设置 ThreadLocal 上下文。
 * 应用上下文（含内部 ID、成员校验）由 {@link AppDataIsolationAspect}
 * 在服务层方法切面中通过 {@code AppContextResolver.resolveAndValidate()} 统一注入。</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.1
 * @see AppWhitelistService
 * @see AppDataIsolationAspect
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppWhitelistInterceptor implements HandlerInterceptor {

    private static final String HEADER_APP_ID = "X-App-Id";
    private static final String ERROR_CODE = "403";

    private static final String NOT_WHITELISTED_ZH = "该应用未开通连接器平台能力";
    private static final String NOT_WHITELISTED_EN = "This app does not have connector platform capability";
    private static final String MISSING_HEADER_ZH = "缺少 X-App-Id Header";
    private static final String MISSING_HEADER_EN = "Missing X-App-Id header";

    private final AppWhitelistService appWhitelistService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String appId = request.getHeader(HEADER_APP_ID);
        if (appId == null || appId.trim().isEmpty()) {
            log.warn("Request blocked: missing X-App-Id header, path={}", request.getRequestURI());
            writeErrorResponse(response, MISSING_HEADER_ZH, MISSING_HEADER_EN);
            return false;
        }

        appId = appId.trim();

        // v2.0.1: 直接以 String 外部 ID 校验白名单，不经过 Long.parseLong 转换
        if (!appWhitelistService.isWhitelisted(appId)) {
            log.warn("Request blocked: appId {} not in whitelist, path={}, method={}",
                    appId, request.getRequestURI(), request.getMethod());
            writeErrorResponse(response, NOT_WHITELISTED_ZH, NOT_WHITELISTED_EN);
            return false;
        }

        // 不设 ThreadLocal — 交由 AppDataIsolationAspect 统一管理
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AppContextHolder.clear();
    }

    private void writeErrorResponse(HttpServletResponse response, String messageZh, String messageEn)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Void> errorResponse = ApiResponse.error(ERROR_CODE, messageZh, messageEn);
        String jsonBody = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonBody);
    }
}
