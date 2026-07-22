package com.xxx.api.modules.appmember.service.impl;

import com.xxx.api.common.exception.BusinessException;
import com.xxx.api.modules.appmember.auth.SysTokenResolver;
import com.xxx.api.common.cache.InternalCacheManager;
import com.xxx.api.modules.appmember.config.InternalAuthProperties;
import com.xxx.api.modules.appmember.dto.UserRoleQueryRequest;
import com.xxx.api.modules.appmember.dto.UserRoleQueryResponse;
import com.xxx.api.modules.app.entity.AppEntity;
import com.xxx.api.modules.appmember.entity.AppMemberEntity;
import com.xxx.api.modules.app.entity.AppPropertyEntity;
import com.xxx.api.modules.app.mapper.AppEntityMapper;
import com.xxx.api.modules.appmember.mapper.AppMemberEntityMapper;
import com.xxx.api.modules.app.mapper.AppPropertyEntityMapper;
import com.xxx.api.modules.appmember.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 用户角色查询服务实现
 *
 * <p>完整业务流程：凭证校验 → 参数校验 → 应用标识解析（缓存） → 角色查询（缓存）</p>
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
    private final InternalCacheManager cacheManager;

    @Override
    public UserRoleQueryResponse queryUserRoles(UserRoleQueryRequest request, String token) {
        log.info("Query user roles: appId={}, hisAppId={}, userAccount={}",
                request.getAppId(), request.getHisAppId(), request.getUserAccount());

        // ---- 1. 凭证校验 ----
        validateToken(token);

        // ---- 2. 参数校验 ----
        validateParams(request);

        // ---- 3. 解析应用标识（带缓存） ----
        AppEntity app = resolveAppIdentifier(request.getAppId(), request.getHisAppId());

        // ---- 4. 查全量成员（带缓存）+ 内存过滤 ----
        List<AppMemberEntity> members = getCachedMembers(app.getId());
        Integer[] roles = members.stream()
                .filter(m -> request.getUserAccount().equals(m.getAccountId())
                        && isActive(m.getStatus()))
                .map(AppMemberEntity::getMemberType)
                .toArray(Integer[]::new);

        log.info("Query result: appId={}, userAccount={}, totalMembers={}, matchedRoles={}",
                app.getAppId(), request.getUserAccount(), members.size(), Arrays.toString(roles));

        return UserRoleQueryResponse.builder()
                .appId(app.getAppId())
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

    // ──────────────────── 应用标识解析（带缓存） ────────────────────

    private AppEntity resolveAppIdentifier(String appId, String hisAppId) {
        // 1. appId 字段：先查缓存 → miss 则查 DB → 写缓存
        if (appId != null && !appId.isBlank()) {
            Optional<AppEntity> cached = cacheManager.getAppByAppId(appId);
            if (cached.isPresent()) {
                log.debug("App resolved from cache by appId: {}", appId);
                return cached.get();
            }

            AppEntity app = appEntityMapper.selectByAppId(appId);
            if (app != null && isActive(app.getStatus())) {
                cacheManager.setAppByAppId(appId, app);
                log.debug("App resolved by appId: {} -> id={}", appId, app.getId());
                return app;
            }
            log.debug("App not found or inactive by appId: {}", appId);
        }

        // 2. hisAppId 字段：先查缓存 → miss 则查 DB → 写缓存
        if (hisAppId != null && !hisAppId.isBlank()) {
            Optional<AppEntity> cached = cacheManager.getAppByHisAppId(hisAppId);
            if (cached.isPresent()) {
                log.debug("App resolved from cache by hisAppId: {}", hisAppId);
                return cached.get();
            }

            AppPropertyEntity prop = appPropertyEntityMapper.selectByEamapAppCode(hisAppId);
            if (prop != null && isActive(prop.getStatus())) {
                AppEntity app = appEntityMapper.selectById(prop.getParentId());
                if (app != null && isActive(app.getStatus())) {
                    cacheManager.setAppByHisAppId(hisAppId, app);
                    log.debug("App resolved by hisAppId: {} -> appId={}", hisAppId, app.getAppId());
                    return app;
                }
            }
            log.debug("App not found or inactive by hisAppId: {}", hisAppId);
        }

        log.warn("No matching application: appId={}, hisAppId={}", appId, hisAppId);
        throw BusinessException.notFound("应用不存在", "Application not found");
    }

    // ──────────────────── 成员查询（带缓存） ────────────────────

    private List<AppMemberEntity> getCachedMembers(Long appId) {
        Optional<List<AppMemberEntity>> cached = cacheManager.getMembers(appId);
        if (cached.isPresent()) {
            log.debug("Members resolved from cache: appId={}, count={}", appId, cached.get().size());
            return cached.get();
        }

        List<AppMemberEntity> members = appMemberEntityMapper.selectByAppId(appId);
        cacheManager.setMembers(appId, members);
        log.debug("Members resolved from DB: appId={}, count={}", appId, members.size());
        return members;
    }

    // ──────────────────── 工具方法 ────────────────────

    private static boolean isActive(Integer status) {
        return status != null && status == 1;
    }
}
