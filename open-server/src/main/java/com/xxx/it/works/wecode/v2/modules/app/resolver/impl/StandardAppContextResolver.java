package com.xxx.it.works.wecode.v2.modules.app.resolver.impl;

import com.xxx.it.works.wecode.v2.modules.app.resolver.AppAccessException;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Standard Environment Application Context Resolver
 *
 * <p>Production environment implementation:</p>
 * <ul>
 *   <li>ID conversion: Call application management service to get mapping</li>
 *   <li>Permission validation: Validate current user's access permission to the application</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.resolver.type", havingValue = "standard")
public class StandardAppContextResolver implements AppContextResolver {

    // TODO: Inject application management service
    // @Autowired
    // private AppManageService appManageService;

    @Override
    public AppContext resolveAndValidate(String externalAppId) {
        log.info("Standard environment resolving appId: {}", externalAppId);

        // 1. Validate parameters
        if (externalAppId == null || externalAppId.isEmpty()) {
            throw AppAccessException.notFound(externalAppId);
        }

        // TODO: Standard environment implementation, integrate with application management service
        // 2. Call application management service to get internal ID
        // Long internalId = appManageService.getInternalIdByExternalId(externalAppId);
        // if (internalId == null) {
        //     throw AppAccessException.notFound(externalAppId);
        // }

        // 3. Validate current user's access permission to the application
        // String currentUserId = UserContextHolder.getUserId();
        // boolean hasPermission = appManageService.checkUserAppPermission(
        //     currentUserId, internalId);
        // if (!hasPermission) {
        //     throw AppAccessException.noPermission(externalAppId);
        // }

        // 4. Return context
        // return AppContext.builder()
        //     .internalId(internalId)
        //     .externalId(externalAppId)
        //     .build();

        throw new UnsupportedOperationException(
            "StandardAppContextResolver not implemented yet, please implement it in standard environment");
    }

    @Override
    public String toExternalId(Long internalId) {
        // TODO: Standard environment implementation
        // return appManageService.getExternalIdByInternalId(internalId);
        throw new UnsupportedOperationException(
            "StandardAppContextResolver not implemented yet, please implement it in standard environment");
    }
}
