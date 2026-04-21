package com.xxx.api.scope.controller;

import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.scope.dto.*;
import com.xxx.api.scope.service.ScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Scope 授权控制器
 * 
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET /api/v1/user-authorizations - 获取用户授权列表（#52）</li>
 *   <li>POST /api/v1/user-authorizations - 用户授权（#53）</li>
 *   <li>DELETE /api/v1/user-authorizations/:id - 取消授权（#54）</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Tag(name = "Scope 授权管理", description = "用户授权管理接口")
@RestController
@RequestMapping("/api/v1/user-authorizations")
@RequiredArgsConstructor
@Validated
public class ScopeController {

    private final ScopeService scopeService;

    /**
     * 获取用户授权列表
     * 
     * <p>接口编号：#52</p>
     */
    @Operation(summary = "获取用户授权列表", description = "查询用户授权列表，支持分页和过滤")
    @GetMapping
    public ApiResponse<List<UserAuthorizationListResponse>> getUserAuthorizations(
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "应用ID") @RequestParam(required = false) String appId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        
        UserAuthorizationListRequest request = new UserAuthorizationListRequest();
        request.setUserId(userId);
        request.setAppId(appId);
        request.setKeyword(keyword);
        request.setCurPage(curPage);
        request.setPageSize(pageSize);
        
        return scopeService.getUserAuthorizations(request);
    }

    /**
     * 用户授权
     * 
     * <p>接口编号：#53</p>
     */
    @Operation(summary = "用户授权", description = "为用户授权应用 Scope，支持设置有效期")
    @PostMapping
    public ApiResponse<UserAuthorizationResponse> createUserAuthorization(
            @Valid @RequestBody UserAuthorizationCreateRequest request) {
        return scopeService.createUserAuthorization(request);
    }

    /**
     * 取消授权
     * 
     * <p>接口编号：#54</p>
     */
    @Operation(summary = "取消授权", description = "取消用户的授权")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> revokeUserAuthorization(
            @Parameter(description = "授权ID") @PathVariable String id) {
        return scopeService.revokeUserAuthorization(id);
    }
}
