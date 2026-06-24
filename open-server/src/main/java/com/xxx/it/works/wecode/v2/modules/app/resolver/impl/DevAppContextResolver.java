package com.xxx.it.works.wecode.v2.modules.app.resolver.impl;

import com.xxx.it.works.wecode.v2.modules.app.entity.App;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppMapper;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppAccessException;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Development environment application context resolver
 *
 * <p>Default implementation for development and testing environments:</p>
 * <ul>
 *   <li>ID conversion: Query AppMapper to get internal ID from external appId</li>
 *   <li>Permission validation: Skip validation, allow all access</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class DevAppContextResolver implements AppContextResolver {

    @Autowired
    private AppMapper appMapper;

    @Override
    public AppContext resolveAndValidate(String externalAppId) {
        log.debug("Dev environment resolving appId: {}", externalAppId);

        if (StringUtils.isEmpty(externalAppId)) {
            throw AppAccessException.notFound(externalAppId);
        }

        // 尝试从数据库查询真实 ID
        App app = appMapper.selectByAppId(externalAppId);
        if (app != null) {
            return AppContext.builder()
                    .internalId(app.getId())
                    .externalId(DEV_ACCOUNT_ID)
                    .app(app)
                    .build();
        }

        // 兜底：尝试解析为数字
        Long internalId = parseInternalId(externalAppId);
        return AppContext.builder()
                .internalId(internalId)
                .externalId(externalAppId)
                .build();
    }

    // 开发环境默认用户ID
    private static final String DEV_ACCOUNT_ID = "currentUser";

    @Override
    public String toExternalId(Long internalId) {
        if (internalId == null) {
            return null;
        }
        App app = appMapper.selectById(internalId);
        return app != null ? app.getAppId() : String.valueOf(internalId);
    }

    /**
     * Parse internal ID
     * Supports two formats:
     * 1. Pure numeric string "1001" → 1001L
     * 2. Long business ID, extract trailing number or use default value
     */
    private Long parseInternalId(String externalAppId) {
        try {
            return Long.parseLong(externalAppId);
        } catch (NumberFormatException e) {
            log.debug("Non-numeric appId: {}, using default value 1L", externalAppId);
            return 1L;
        }
    }

}
