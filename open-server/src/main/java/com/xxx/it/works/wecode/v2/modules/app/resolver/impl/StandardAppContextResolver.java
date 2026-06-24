package com.xxx.it.works.wecode.v2.modules.app.resolver.impl;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.modules.app.entity.App;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppMapper;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppAccessException;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import com.xxx.it.works.wecode.v2.modules.member.entity.AppMember;
import com.xxx.it.works.wecode.v2.modules.member.mapper.AppMemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Standard Environment Application Context Resolver
 *
 * <p>Production environment implementation:</p>
 * <ul>
 *   <li>ID conversion: Query AppMapper to get internal ID from external appId</li>
 *   <li>Permission validation: Check current user's membership in the app</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.resolver.type", havingValue = "standard")
public class StandardAppContextResolver implements AppContextResolver {

    @Autowired
    private AppMapper appMapper;
    @Autowired
    private AppMemberMapper appMemberMapper;

    @Override
    public AppContext resolveAndValidate(String externalAppId) {
        log.info("Standard environment resolving appId: {}", externalAppId);

        // 1. Validate parameters
        if (StringUtils.isEmpty(externalAppId)) {
            throw AppAccessException.notFound(externalAppId);
        }

        // 2. 查询应用是否存在
        App app = appMapper.selectByAppId(externalAppId);
        if (app == null) {
            throw AppAccessException.notFound(externalAppId);
        }

        // 3. 校验当前用户是否为应用成员
        Long internalId = app.getId();
        String currentUserId = UserContextHolder.getUserId();
        List<AppMember> members = appMemberMapper.selectByAppIdAndAccountId(internalId, currentUserId);
        if (CollectionUtils.isEmpty(members)) {
            throw AppAccessException.noPermission(externalAppId);
        }

        // 4. Return context
        return AppContext.builder()
                .internalId(internalId)
                .externalId(externalAppId)
                .app(app)
                .build();
    }

    @Override
    public String toExternalId(Long internalId) {
        App app = appMapper.selectById(internalId);
        if (app == null) {
            throw AppAccessException.notFound(String.valueOf(internalId));
        }
        return app.getAppId();
    }

}
