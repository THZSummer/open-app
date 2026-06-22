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
 * 应用白名单准入拦截器
 *
 * <p>通过 X-App-Id Header 获取应用 ID，校验应用是否在连接器平台白名单内。
 * 非白名单应用返回 403 Forbidden。</p>
 *
 * <p>拦截路径范围：
 * <ul>
 *   <li>/service/open/v2/connectors/**</li>
 *   <li>/service/open/v2/flows/**</li>
 * </ul>
 *
 * <p>校验通过后，将 appId 设置到 AppContextHolder 供后续服务层使用。
 * 请求完成后由 afterCompletion 清除上下文。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see AppWhitelistService
 * @see AppContextHolder
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppWhitelistInterceptor implements HandlerInterceptor {

    /**
     * X-App-Id Header 名称
     */
    private static final String HEADER_APP_ID = "X-App-Id";

    /**
     * 403 错误码
     */
    private static final String ERROR_CODE = "403";

    /**
     * 白名单校验失败 - 中文消息
     */
    private static final String NOT_WHITELISTED_ZH = "该应用未开通连接器平台能力";

    /**
     * 白名单校验失败 - 英文消息
     */
    private static final String NOT_WHITELISTED_EN = "This app does not have connector platform capability";

    /**
     * 缺少 Header - 中文消息
     */
    private static final String MISSING_HEADER_ZH = "缺少 X-App-Id Header";

    /**
     * 缺少 Header - 英文消息
     */
    private static final String MISSING_HEADER_EN = "Missing X-App-Id header";

    private final AppWhitelistService appWhitelistService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 获取 X-App-Id Header
        String appIdHeader = request.getHeader(HEADER_APP_ID);
        if (appIdHeader == null || appIdHeader.trim().isEmpty()) {
            log.warn("Request blocked: missing X-App-Id header, path={}", request.getRequestURI());
            writeErrorResponse(response, MISSING_HEADER_ZH, MISSING_HEADER_EN);
            return false;
        }

        // 解析应用 ID
        Long appId;
        try {
            appId = Long.parseLong(appIdHeader.trim());
        } catch (NumberFormatException e) {
            log.warn("Request blocked: invalid X-App-Id header value '{}', path={}",
                    appIdHeader, request.getRequestURI());
            writeErrorResponse(response, NOT_WHITELISTED_ZH, NOT_WHITELISTED_EN);
            return false;
        }

        // 白名单校验
        if (!appWhitelistService.isWhitelisted(appId)) {
            log.warn("Request blocked: appId {} not in whitelist, path={}, method={}",
                    appId, request.getRequestURI(), request.getMethod());
            writeErrorResponse(response, NOT_WHITELISTED_ZH, NOT_WHITELISTED_EN);
            return false;
        }

        // 设置应用上下文
        AppContextHolder.setCurrentAppId(appId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清除应用上下文，防止内存泄漏
        AppContextHolder.clear();
    }

    /**
     * 写入 JSON 格式的 403 错误响应
     *
     * @param response  HTTP 响应
     * @param messageZh 中文错误消息
     * @param messageEn 英文错误消息
     * @throws IOException 写入失败时抛出
     */
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
