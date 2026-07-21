package com.xxx.api.internal.service.impl;

import com.xxx.api.internal.dto.UserRoleQueryRequest;
import com.xxx.api.internal.dto.UserRoleQueryResponse;
import com.xxx.api.internal.entity.AppEntity;
import com.xxx.api.internal.entity.AppMemberEntity;
import com.xxx.api.internal.mapper.AppEntityMapper;
import com.xxx.api.internal.mapper.AppMemberEntityMapper;
import com.xxx.api.internal.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final AppEntityMapper appEntityMapper;
    private final AppMemberEntityMapper appMemberEntityMapper;

    @Override
    public UserRoleQueryResponse queryUserRoles(UserRoleQueryRequest request, String resolvedAppId) {
        log.info("Querying user roles: appId={}, userAccount={}", resolvedAppId, request.getUserAccount());

        // 通过 varchar app_id 查 app_t 获取内部 id (bigint)
        AppEntity app = appEntityMapper.selectByAppId(resolvedAppId);
        if (app == null) {
            log.warn("App not found for resolvedAppId={}", resolvedAppId);
            return UserRoleQueryResponse.builder()
                    .appId(resolvedAppId)
                    .roles(new Integer[0])
                    .build();
        }

        // 查询成员角色
        List<AppMemberEntity> members = appMemberEntityMapper.selectByAppIdAndAccountId(
                app.getId(), request.getUserAccount());

        Integer[] roles = members.stream()
                .map(AppMemberEntity::getMemberType)
                .toArray(Integer[]::new);

        log.info("User role query result: appId={}, userAccount={}, roles={}",
                resolvedAppId, request.getUserAccount(), java.util.Arrays.toString(roles));

        return UserRoleQueryResponse.builder()
                .appId(resolvedAppId)
                .roles(roles)
                .build();
    }
}
