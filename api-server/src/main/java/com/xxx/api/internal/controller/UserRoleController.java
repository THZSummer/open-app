package com.xxx.api.internal.controller;

import com.xxx.api.common.exception.BusinessException;
import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.internal.dto.UserRoleQueryRequest;
import com.xxx.api.internal.dto.UserRoleQueryResponse;
import com.xxx.api.internal.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
 * <p>职责：取 Header → 调 Service → 异常映射。业务逻辑全部下沉 {@link UserRoleService}。</p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Tag(name = "内部接口 - 用户角色查询", description = "嵌入能力方查询用户在应用中的角色")
@RestController
@RequestMapping("/service/open/v2/internal/user/roles")
@RequiredArgsConstructor
@Validated
public class UserRoleController {

    private final UserRoleService userRoleService;

    @Operation(summary = "查询用户角色", description = "输入应用标识 + 用户账号，返回角色列表")
    @PostMapping
    public ApiResponse<UserRoleQueryResponse> queryUserRoles(
            @Valid @RequestBody UserRoleQueryRequest request,
            HttpServletRequest httpRequest) {

        log.info("Query user roles request: appId={}, hisAppId={}, userAccount={}",
                request.getAppId(), request.getHisAppId(), request.getUserAccount());

        try {
            UserRoleQueryResponse response = userRoleService.queryUserRoles(
                    request, httpRequest.getHeader("X-Internal-Token"));
            return ApiResponse.success(response);
        } catch (BusinessException e) {
            return ApiResponse.error(e.getCode(), e.getMessageZh(), e.getMessageEn());
        }
    }
}
