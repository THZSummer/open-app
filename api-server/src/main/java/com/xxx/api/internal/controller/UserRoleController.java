package com.xxx.api.internal.controller;

import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.internal.dto.UserRoleQueryRequest;
import com.xxx.api.internal.dto.UserRoleQueryResponse;
import com.xxx.api.internal.resolver.AppIdentifierResolver;
import com.xxx.api.internal.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户角色查询控制器（内部接口）
 *
 * <p>接口路径：{@code POST /service/open/v2/internal/user/roles}</p>
 *
 * <p>供嵌入能力方后端调用，查询用户在指定应用中的角色。
 * 请求需携带内部凭证 {@code X-Internal-Token}，由 {@code InternalTokenAuthFilter} 校验。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Tag(name = "内部接口 - 用户角色查询", description = "嵌入能力方查询用户在应用中的角色")
@RestController
@RequestMapping("/service/open/v2/internal/user/roles")
@RequiredArgsConstructor
@Validated
public class UserRoleController {

    private final UserRoleService userRoleService;
    private final AppIdentifierResolver appIdentifierResolver;

    /**
     * 查询用户角色
     *
     * <p>输入应用标识（平台 appId 或 hisAppId）+ 用户账号，返回角色列表。</p>
     *
     * <p>响应格式：</p>
     * <ul>
     *   <li>200 - 成功（含角色列表）</li>
     *   <li>400 - 参数错误</li>
     *   <li>401 - 凭证无效（由过滤器返回）</li>
     *   <li>404 - 应用不存在</li>
     * </ul>
     */
    @Operation(summary = "查询用户角色", description = "输入应用标识 + 用户账号，返回角色列表")
    @PostMapping
    public ApiResponse<UserRoleQueryResponse> queryUserRoles(
            @Valid @RequestBody UserRoleQueryRequest request) {

        log.info("Query user roles request: appId={}, hisAppId={}, userAccount={}",
                request.getAppId(), request.getHisAppId(), request.getUserAccount());

        // 校验参数：appId 和 hisAppId 至少二选一
        if ((request.getAppId() == null || request.getAppId().isBlank())
                && (request.getHisAppId() == null || request.getHisAppId().isBlank())) {
            return ApiResponse.error("400", "appId 和 hisAppId 至少传入一个", "appId or hisAppId is required");
        }

        // 校验参数：userAccount 必填
        if (request.getUserAccount() == null || request.getUserAccount().isBlank()) {
            return ApiResponse.error("400", "用户账号不能为空", "userAccount is required");
        }

        // 解析应用标识
        String resolvedAppId;
        try {
            resolvedAppId = appIdentifierResolver.resolve(request.getAppId(), request.getHisAppId());
        } catch (com.xxx.api.common.exception.BusinessException e) {
            return ApiResponse.error(e.getCode(), e.getMessageZh(), e.getMessageEn());
        }

        // 查询用户角色
        UserRoleQueryResponse response = userRoleService.queryUserRoles(request, resolvedAppId);

        log.info("Query user roles success: appId={}, roles={}",
                response.getAppId(), java.util.Arrays.toString(response.getRoles()));

        return ApiResponse.success(response);
    }
}
