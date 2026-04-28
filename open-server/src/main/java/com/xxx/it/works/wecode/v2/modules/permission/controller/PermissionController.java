package com.xxx.it.works.wecode.v2.modules.permission.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.permission.dto.*;
import com.xxx.it.works.wecode.v2.modules.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理 Controller
 * 
 * <p>提供权限申请与订阅管理接口</p>
 * <p>接口编号：#27 ~ #40</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "权限管理", description = "权限申请与订阅管理接口")
public class PermissionController {

    private final PermissionService permissionService;

    // ==================== API 权限管理 (#27-30) ====================

    /**
     * #27 获取应用 API 权限列表
     */
    @GetMapping("/api/v1/apps/{appId}/apis")
    @Operation(summary = "#27 获取应用 API 权限列表", 
               description = "返回应用 API 权限列表，支持分页")
    public ApiResponse<List<ApiSubscriptionListResponse>> getApiSubscriptionList(
            @Parameter(description = "应用ID") 
            @PathVariable String appId,
            @Parameter(description = "订阅状态过滤") 
            @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词") 
            @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码") 
            @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量") 
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        
        log.info("Get app API permission list, appId={}", appId);
        
        return permissionService.getApiSubscriptionList(appId, status, keyword, curPage, pageSize);
    }

    /**
     * #28 获取分类下 API 权限列表（权限树懒加载）
     */
    @GetMapping("/api/v1/categories/{id}/apis")
    @Operation(summary = "#28 获取分类下 API 权限列表", 
               description = "返回分类下 API 权限列表（权限树懒加载），返回 category 对象含 path 和 categoryPath")
    public ApiResponse<List<CategoryPermissionListResponse>> getCategoryApiPermissions(
            @Parameter(description = "分类ID") 
            @PathVariable String id,
            @Parameter(description = "应用ID（用于查询订阅状态）") 
            @RequestParam(required = false) String appId,
            @Parameter(description = "搜索关键词") 
            @RequestParam(required = false) String keyword,
            @Parameter(description = "是否需要审核过滤") 
            @RequestParam(required = false) Integer needApproval,
            @Parameter(description = "是否包含子分类权限（默认 true）") 
            @RequestParam(required = false) Boolean includeChildren,
            @Parameter(description = "当前页码") 
            @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量") 
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        
        log.info("Get API permission list under category, categoryId={}, appId={}", id, appId);
        
        return permissionService.getCategoryApiPermissions(id, appId, keyword, needApproval, includeChildren, curPage, pageSize);
    }

    /**
     * #29 申请 API 权限（支持批量）
     */
    @PostMapping("/api/v1/apps/{appId}/apis/subscribe")
    @Operation(summary = "#29 申请 API 权限", 
               description = "申请 API 权限，支持批量提交，每条权限申请生成独立审批单")
    public ApiResponse<PermissionSubscribeResponse> subscribeApiPermissions(
            @Parameter(description = "应用ID") 
            @PathVariable String appId,
            @Valid @RequestBody PermissionSubscribeRequest request) {
        
        log.info("Apply for API permission, appId={}, permissionIds={}", appId, request.getPermissionIds());
        
        PermissionSubscribeResponse response = permissionService.subscribeApiPermissions(appId, request);
        return ApiResponse.success(response);
    }

    /**
     * #30 撤回审核中的 API 权限申请
     */
    @PostMapping("/api/v1/apps/{appId}/apis/{id}/withdraw")
    @Operation(summary = "#30 撤回 API 权限申请", 
               description = "撤回审核中的申请")
    public ApiResponse<WithdrawResponse> withdrawApiSubscription(
            @Parameter(description = "应用ID") 
            @PathVariable String appId,
            @Parameter(description = "订阅ID") 
            @PathVariable String id) {
        
        log.info("Withdraw API permission application, appId={}, subscriptionId={}", appId, id);
        
        WithdrawResponse response = permissionService.withdrawApiSubscription(appId, id);
        return ApiResponse.success(response);
    }

    // ==================== 事件权限管理 (#31-35) ====================

    /**
     * #31 获取应用事件订阅列表
     */
    @GetMapping("/api/v1/apps/{appId}/events")
    @Operation(summary = "#31 获取应用事件订阅列表", 
               description = "返回应用事件订阅列表，支持分页")
    public ApiResponse<List<EventSubscriptionListResponse>> getEventSubscriptionList(
            @Parameter(description = "应用ID") 
            @PathVariable String appId,
            @Parameter(description = "订阅状态过滤") 
            @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词") 
            @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码") 
            @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量") 
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        
        log.info("Get app event subscription list, appId={}", appId);
        
        return permissionService.getEventSubscriptionList(appId, status, keyword, curPage, pageSize);
    }

    /**
     * #32 获取分类下事件权限列表（权限树懒加载）
     */
    @GetMapping("/api/v1/categories/{id}/events")
    @Operation(summary = "#32 获取分类下事件权限列表", 
               description = "返回分类下事件权限列表（权限树懒加载），返回 category 对象含 path 和 categoryPath")
    public ApiResponse<List<CategoryPermissionListResponse>> getCategoryEventPermissions(
            @Parameter(description = "分类ID") 
            @PathVariable String id,
            @Parameter(description = "应用ID（用于查询订阅状态）") 
            @RequestParam(required = false) String appId,
            @Parameter(description = "搜索关键词") 
            @RequestParam(required = false) String keyword,
            @Parameter(description = "是否需要审核过滤") 
            @RequestParam(required = false) Integer needApproval,
            @Parameter(description = "是否包含子分类权限（默认 true）") 
            @RequestParam(required = false) Boolean includeChildren,
            @Parameter(description = "当前页码") 
            @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量") 
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        
        log.info("Get event permission list under category, categoryId={}, appId={}", id, appId);
        
        return permissionService.getCategoryEventPermissions(id, appId, keyword, needApproval, includeChildren, curPage, pageSize);
    }

    /**
     * #33 申请事件权限（支持批量）
     */
    @PostMapping("/api/v1/apps/{appId}/events/subscribe")
    @Operation(summary = "#33 申请事件权限", 
               description = "申请事件权限，支持批量提交")
    public ApiResponse<PermissionSubscribeResponse> subscribeEventPermissions(
            @Parameter(description = "应用ID") 
            @PathVariable String appId,
            @Valid @RequestBody PermissionSubscribeRequest request) {
        
        log.info("Apply for event permission, appId={}, permissionIds={}", appId, request.getPermissionIds());
        
        PermissionSubscribeResponse response = permissionService.subscribeEventPermissions(appId, request);
        return ApiResponse.success(response);
    }

    /**
     * #34 配置事件消费参数
     */
    @PutMapping("/api/v1/apps/{appId}/events/{id}/config")
    @Operation(summary = "#34 配置事件消费参数", 
               description = "配置事件消费参数（通道/地址/认证）")
    public ApiResponse<WithdrawResponse> configEventSubscription(
            @Parameter(description = "应用ID") 
            @PathVariable String appId,
            @Parameter(description = "订阅ID") 
            @PathVariable String id,
            @Valid @RequestBody SubscriptionConfigRequest request) {
        
        log.info("Configure event consumption params, appId={}, subscriptionId={}", appId, id);
        
        WithdrawResponse response = permissionService.configEventSubscription(appId, id, request);
        return ApiResponse.success(response);
    }

    /**
     * #35 撤回审核中的事件权限申请
     */
    @PostMapping("/api/v1/apps/{appId}/events/{id}/withdraw")
    @Operation(summary = "#35 撤回事件权限申请", 
               description = "撤回审核中的申请")
    public ApiResponse<WithdrawResponse> withdrawEventSubscription(
            @Parameter(description = "应用ID") 
            @PathVariable String appId,
            @Parameter(description = "订阅ID") 
            @PathVariable String id) {
        
        log.info("Withdraw event permission application, appId={}, subscriptionId={}", appId, id);
        
        WithdrawResponse response = permissionService.withdrawEventSubscription(appId, id);
        return ApiResponse.success(response);
    }

    // ==================== 回调权限管理 (#36-40) ====================

    /**
     * #36 获取应用回调订阅列表
     */
    @GetMapping("/api/v1/apps/{appId}/callbacks")
    @Operation(summary = "#36 获取应用回调订阅列表", 
               description = "返回应用回调订阅列表，支持分页")
    public ApiResponse<List<CallbackSubscriptionListResponse>> getCallbackSubscriptionList(
            @Parameter(description = "应用ID") 
            @PathVariable String appId,
            @Parameter(description = "订阅状态过滤") 
            @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词") 
            @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码") 
            @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量") 
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        
        log.info("Get app callback subscription list, appId={}", appId);
        
        return permissionService.getCallbackSubscriptionList(appId, status, keyword, curPage, pageSize);
    }

    /**
     * #37 获取分类下回调权限列表（权限树懒加载）
     */
    @GetMapping("/api/v1/categories/{id}/callbacks")
    @Operation(summary = "#37 获取分类下回调权限列表", 
               description = "返回分类下回调权限列表（权限树懒加载），返回 category 对象含 path 和 categoryPath")
    public ApiResponse<List<CategoryPermissionListResponse>> getCategoryCallbackPermissions(
            @Parameter(description = "分类ID") 
            @PathVariable String id,
            @Parameter(description = "应用ID（用于查询订阅状态）") 
            @RequestParam(required = false) String appId,
            @Parameter(description = "搜索关键词") 
            @RequestParam(required = false) String keyword,
            @Parameter(description = "是否需要审核过滤") 
            @RequestParam(required = false) Integer needApproval,
            @Parameter(description = "是否包含子分类权限（默认 true）") 
            @RequestParam(required = false) Boolean includeChildren,
            @Parameter(description = "当前页码") 
            @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量") 
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        
        log.info("Get callback permission list under category, categoryId={}, appId={}", id, appId);
        
        return permissionService.getCategoryCallbackPermissions(id, appId, keyword, needApproval, includeChildren, curPage, pageSize);
    }

    /**
     * #38 申请回调权限（支持批量）
     */
    @PostMapping("/api/v1/apps/{appId}/callbacks/subscribe")
    @Operation(summary = "#38 申请回调权限", 
               description = "申请回调权限，支持批量提交")
    public ApiResponse<PermissionSubscribeResponse> subscribeCallbackPermissions(
            @Parameter(description = "应用ID") 
            @PathVariable String appId,
            @Valid @RequestBody PermissionSubscribeRequest request) {
        
        log.info("Apply for callback permission, appId={}, permissionIds={}", appId, request.getPermissionIds());
        
        PermissionSubscribeResponse response = permissionService.subscribeCallbackPermissions(appId, request);
        return ApiResponse.success(response);
    }

    /**
     * #39 配置回调消费参数
     */
    @PutMapping("/api/v1/apps/{appId}/callbacks/{id}/config")
    @Operation(summary = "#39 配置回调消费参数", 
               description = "配置回调消费参数")
    public ApiResponse<WithdrawResponse> configCallbackSubscription(
            @Parameter(description = "应用ID") 
            @PathVariable String appId,
            @Parameter(description = "订阅ID") 
            @PathVariable String id,
            @Valid @RequestBody SubscriptionConfigRequest request) {
        
        log.info("Configure callback consumption params, appId={}, subscriptionId={}", appId, id);
        
        WithdrawResponse response = permissionService.configCallbackSubscription(appId, id, request);
        return ApiResponse.success(response);
    }

    /**
     * #40 撤回审核中的回调权限申请
     */
    @PostMapping("/api/v1/apps/{appId}/callbacks/{id}/withdraw")
    @Operation(summary = "#40 撤回回调权限申请", 
               description = "撤回审核中的申请")
    public ApiResponse<WithdrawResponse> withdrawCallbackSubscription(
            @Parameter(description = "应用ID") 
            @PathVariable String appId,
            @Parameter(description = "订阅ID") 
            @PathVariable String id) {
        
        log.info("Withdraw callback permission application, appId={}, subscriptionId={}", appId, id);
        
        WithdrawResponse response = permissionService.withdrawCallbackSubscription(appId, id);
        return ApiResponse.success(response);
    }
}
