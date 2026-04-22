package com.xxx.api.gateway.controller;

import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.gateway.dto.ApiGatewayResponse;
import com.xxx.api.gateway.dto.PermissionCheckResponse;
import com.xxx.api.gateway.service.ApiGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * API 网关控制器
 * 
 * <p>接口列表：</p>
 * <ul>
 *   <li>ANY /gateway/api/* - API 请求代理与鉴权（#55）</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Tag(name = "API 网关", description = "API 请求代理与鉴权")
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class ApiGatewayController {

    private final ApiGatewayService apiGatewayService;

    /**
     * API 请求代理与鉴权
     * 
     * <p>接口编号：#55</p>
     * <p>处理流程：</p>
     * <ol>
     *   <li>验证应用身份（AKSK/Bearer Token）</li>
     *   <li>查询应用订阅关系</li>
     *   <li>验证请求路径与方法是否在授权 Scope 范围内</li>
     *   <li>转发请求到内部中台网关</li>
     *   <li>返回响应</li>
     * </ol>
     */
    @Operation(summary = "API 请求代理与鉴权", description = "验证权限后转发请求到内部中台网关")
    @RequestMapping(value = "/api/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> proxyApiRequest(
            @Parameter(description = "应用ID") @RequestHeader(value = "X-App-Id", required = false) String appId,
            @Parameter(description = "认证类型") @RequestHeader(value = "X-Auth-Type", required = false) Integer authType,
            @Parameter(description = "认证凭证") @RequestHeader(value = "Authorization", required = false) String authCredential,
            @RequestBody(required = false) String body,
            HttpServletRequest request) {
        
        log.info("API 网关请求: method={}, path={}", request.getMethod(), request.getRequestURI());
        
        try {
            // 1. 验证应用身份
            if (!apiGatewayService.verifyApplication(appId, authType, authCredential)) {
                log.warn("应用身份验证失败: appId={}", appId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"code\":\"401\",\"messageZh\":\"应用身份验证失败\",\"messageEn\":\"Unauthorized\"}");
            }
            
            // 2. 获取请求路径和方法
            String path = extractPath(request);
            String method = request.getMethod();
            
            // 3. 查找对应的 Scope
            String scope = apiGatewayService.findScopeByPathAndMethod(path, method);
            
            // 4. 校验权限
            PermissionCheckResponse checkResult = apiGatewayService.checkPermission(appId, scope);
            if (!checkResult.getAuthorized()) {
                log.warn("权限校验失败: appId={}, scope={}, reason={}", appId, scope, checkResult.getReason());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"code\":\"403\",\"messageZh\":\"" + checkResult.getReason() + "\",\"messageEn\":\"Forbidden\"}");
            }
            
            // 5. 转发请求到内部中台网关（Mock 实现）
            log.info("权限校验通过，转发请求到内部网关: appId={}, scope={}", appId, scope);
            
            // Mock: 返回成功响应
            // 实际项目中应该使用 RestTemplate 或 WebClient 转发请求
            String mockResponse = "{\"code\":\"200\",\"messageZh\":\"操作成功\",\"messageEn\":\"Success\",\"data\":{\"result\":\"API调用成功（Mock响应）\"}}";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mockResponse);
            
        } catch (Exception e) {
            log.error("API 网关处理失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"code\":\"500\",\"messageZh\":\"服务器内部错误\",\"messageEn\":\"Internal Server Error\"}");
        }
    }
    
    /**
     * 提取请求路径
     */
    private String extractPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 移除 /gateway/api 前缀
        if (uri.startsWith("/gateway/api")) {
            return uri.substring("/gateway/api".length());
        }
        return uri;
    }
    
    /**
     * 提取请求头
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}
