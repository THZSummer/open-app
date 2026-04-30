package com.xxx.api.data.controller;

import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.data.service.DataQueryService;
import com.xxx.api.gateway.dto.PermissionCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据查询控制器
 * 
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET /gateway/permissions/check - 权限校验接口（#58）</li>
 * </ul>
 * 
 * <p>供 event-server 或其他服务调用</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Tag(name = "数据查询接口", description = "提供数据查询接口，供其他服务调用")
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class DataQueryController {

    private final DataQueryService dataQueryService;

    /**
     * 权限校验接口
     * 
     * <p>接口编号：#58</p>
     * <p>供 event-server 调用，校验应用是否拥有指定权限</p>
     */
    @Operation(summary = "权限校验", description = "校验应用是否拥有指定权限")
    @GetMapping("/permissions/check")
    public ApiResponse<PermissionCheckResponse> checkPermission(
            @Parameter(description = "应用ID", required = true) @RequestParam String appId,
            @Parameter(description = "权限标识", required = true) @RequestParam String scope) {
        
        log.debug("Permission check request: appId={}, scope={}", appId, scope);
        
        PermissionCheckResponse response = dataQueryService.checkPermission(appId, scope);
        return ApiResponse.success(response);
    }

    /**
     * 查询订阅某权限的所有应用
     */
    @Operation(summary = "查询订阅应用列表", description = "查询订阅某权限的所有应用")
    @GetMapping("/permissions/subscribers")
    public ApiResponse<List<String>> getSubscribedApps(
            @Parameter(description = "权限标识", required = true) @RequestParam String scope) {
        
        log.debug("Query subscribed application list: scope={}", scope);
        
        List<String> appIds = dataQueryService.getSubscribedApps(scope);
        return ApiResponse.success(appIds);
    }

    /**
     * 查询应用的订阅配置
     */
    @Operation(summary = "查询订阅配置", description = "查询应用对某权限的订阅配置")
    @GetMapping("/subscriptions/config")
    public ApiResponse<Map<String, Object>> getSubscriptionConfig(
            @Parameter(description = "应用ID", required = true) @RequestParam String appId,
            @Parameter(description = "权限标识", required = true) @RequestParam String scope) {
        
        log.debug("Query subscription configuration: appId={}, scope={}", appId, scope);
        
        Map<String, Object> config = dataQueryService.getSubscriptionConfig(appId, scope);
        return ApiResponse.success(config);
    }

    /**
     * 查询权限详情
     */
    @Operation(summary = "查询权限详情", description = "根据 Scope 查询权限详情")
    @GetMapping("/permissions/detail")
    public ApiResponse<Map<String, Object>> getPermissionByScope(
            @Parameter(description = "权限标识", required = true) @RequestParam String scope) {
        
        log.debug("Query permission details: scope={}", scope);
        
        Map<String, Object> permission = dataQueryService.getPermissionByScope(scope);
        if (permission == null) {
            return ApiResponse.error("404", "权限不存在", "Permission not found");
        }
        return ApiResponse.success(permission);
    }
}
