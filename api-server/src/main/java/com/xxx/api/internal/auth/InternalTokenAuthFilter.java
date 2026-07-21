package com.xxx.api.internal.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.internal.config.InternalAuthConfig;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 内部凭证校验过滤器
 *
 * <p>拦截 {@code /internal/**} 路径，校验 {@code X-Internal-Token} 请求头。</p>
 *
 * <ul>
 *   <li>bypass=true（开发阶段）: 跳过凭证校验</li>
 *   <li>bypass=false（联调/生产）: 校验 token 是否在配置列表中</li>
 *   <li>无效或缺失 token：返回 401</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class InternalTokenAuthFilter implements Filter {

    private static final String TOKEN_HEADER = "X-Internal-Token";
    private static final String INTERNAL_PATH_PREFIX = "/service/open/v2/internal";

    private final InternalAuthConfig authConfig;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        // 只拦截 /internal/** 路径
        String requestPath = contextPath != null && !contextPath.isEmpty()
                ? path.substring(contextPath.length())
                : path;

        if (!requestPath.startsWith(INTERNAL_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // bypass 模式：跳过凭证校验
        if (authConfig.isBypass()) {
            log.debug("Internal auth bypass enabled, skipping token validation for: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // 正常模式：校验 X-Internal-Token
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            log.warn("Missing X-Internal-Token header for path: {}", requestPath);
            writeUnauthorized(response, "内部凭证缺失", "Missing internal token");
            return;
        }

        boolean valid = authConfig.getTokens() != null
                && authConfig.getTokens().stream().anyMatch(t -> t.equals(token));

        if (!valid) {
            log.warn("Invalid X-Internal-Token for path: {}", requestPath);
            writeUnauthorized(response, "内部凭证无效", "Unauthorized");
            return;
        }

        log.debug("Internal token validated successfully for path: {}", requestPath);
        filterChain.doFilter(request, response);
    }

    /**
     * 写入 401 未授权响应
     */
    private void writeUnauthorized(HttpServletResponse response, String messageZh, String messageEn) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK); // 业务状态码 200，业务 code=401
        ApiResponse<Void> errorResponse = ApiResponse.error("401", messageZh, messageEn);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
