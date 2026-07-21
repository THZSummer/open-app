package com.xxx.api.internal.service.impl;

import com.xxx.api.common.exception.BusinessException;
import com.xxx.api.internal.auth.SysTokenResolver;
import com.xxx.api.internal.config.InternalAuthProperties;
import com.xxx.api.internal.dto.UserRoleQueryRequest;
import com.xxx.api.internal.dto.UserRoleQueryResponse;
import com.xxx.api.internal.entity.AppEntity;
import com.xxx.api.internal.entity.AppMemberEntity;
import com.xxx.api.internal.mapper.AppEntityMapper;
import com.xxx.api.internal.mapper.AppMemberEntityMapper;
import com.xxx.api.internal.mapper.AppPropertyEntityMapper;
import com.xxx.api.internal.entity.AppPropertyEntity;
import com.xxx.api.internal.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 用户角色查询服务实现
 *
 * <p>完整业务流程：凭证校验 → 参数校验 → 应用标识解析 → 角色查询</p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final AppEntityMapper appEntityMapper;
    private final AppMemberEntityMapper appMemberEntityMapper;
    private final AppPropertyEntityMapper appPropertyEntityMapper;
    private final SysTokenResolver sysTokenResolver;
    private final InternalAuthProperties authProperties;

    @Override
    public UserRoleQueryResponse queryUserRoles(UserRoleQueryRequest request, String token) {
        log.info("Query user roles: appId={}, hisAppId={}, userAccount={}",
                request.getAppId(), request.getHisAppId(), request.getUserAccount());

        // ---- 1. 凭证校验 ----
        validateToken(token);

        // ---- 2. 参数校验 ----
        validateParams(request);

        // ---- 3. 解析应用标识 ----
        String resolvedAppId = resolveAppIdentifier(request.getAppId(), request.getHisAppId());

        // ---- 4. 查询用户角色 ----
        Integer[] roles = queryRoles(resolvedAppId, request.getUserAccount());

        log.info("Query result: appId={}, userAccount={}, roles={}",
                resolvedAppId, request.getUserAccount(), Arrays.toString(roles));

        return UserRoleQueryResponse.builder()
                .appId(resolvedAppId)
                .roles(roles)
                .build();
    }

    // ──────────────────── 凭证校验 ────────────────────

    private void validateToken(String token) {
        if (authProperties.isBypass()) {
            log.debug("Internal auth bypass enabled");
            return;
        }

        if (token == null || token.isBlank()) {
            log.warn("Missing X-Internal-Token");
            throw BusinessException.unauthorized("内部凭证缺失", "Missing internal token");
        }

        String account = sysTokenResolver.resolveAccount(token);
        if (account == null) {
            log.warn("Failed to resolve account from token");
            throw BusinessException.unauthorized("无法解析内部凭证", "Unable to resolve internal token");
        }

        if (!sysTokenResolver.isTokenValid(token)) {
            log.warn("Invalid or expired token");
            throw BusinessException.unauthorized("内部凭证已失效", "Internal token expired");
        }

        if (authProperties.getAllowedAccounts() != null && !authProperties.getAllowedAccounts().isEmpty()) {
            if (!authProperties.getAllowedAccounts().contains(account)) {
                log.warn("Account '{}' not in whitelist", account);
                throw BusinessException.forbidden("无权限调用内部接口", "Access denied: account not in whitelist");
            }
        }

        log.debug("Token validated: account='{}'", account);
    }

    // ──────────────────── 参数校验 ────────────────────

    private void validateParams(UserRoleQueryRequest request) {
        if ((request.getAppId() == null || request.getAppId().isBlank())
                && (request.getHisAppId() == null || request.getHisAppId().isBlank())) {
            throw BusinessException.badRequest(
                    "appId 和 hisAppId 至少传入一个", "appId or hisAppId is required");
        }

        if (request.getUserAccount() == null || request.getUserAccount().isBlank()) {
            throw BusinessException.badRequest(
                    "用户账号不能为空", "userAccount is required");
        }
    }

    // ──────────────────── 应用标识解析 ────────────────────

    private String resolveAppIdentifier(String appId, String hisAppId) {
        if (appId != null && !appId.isBlank()) {
            AppEntity app = appEntityMapper.selectByAppId(appId);
            if (app != null) {
                log.debug("App resolved by appId: {} -> id={}", appId, app.getId());
                return app.getAppId();
            }
            log.debug("App not found by appId: {}, trying hisAppId", appId);
        }

        if (hisAppId != null && !hisAppId.isBlank()) {
            AppPropertyEntity prop = appPropertyEntityMapper.selectByEamapAppCode(hisAppId);
            if (prop != null) {
                AppEntity app = appEntityMapper.selectById(prop.getParentId());
                if (app != null) {
                    log.debug("App resolved by hisAppId: {} -> appId={}", hisAppId, app.getAppId());
                    return app.getAppId();
                }
            }
            log.debug("App not found by hisAppId: {}", hisAppId);
        }

        log.warn("No matching application: appId={}, hisAppId={}", appId, hisAppId);
        throw BusinessException.notFound("应用不存在", "Application not found");
    }

    // ──────────────────── 角色查询 ────────────────────

    private Integer[] queryRoles(String appId, String userAccount) {
        AppEntity app = appEntityMapper.selectByAppId(appId);
        if (app == null) {
            log.warn("App not found for resolved appId={}", appId);
            return new Integer[0];
        }

        List<AppMemberEntity> members = appMemberEntityMapper.selectByAppIdAndAccountId(
                app.getId(), userAccount);

        return members.stream()
                .map(AppMemberEntity::getMemberType)
                .toArray(Integer[]::new);
    }
}
