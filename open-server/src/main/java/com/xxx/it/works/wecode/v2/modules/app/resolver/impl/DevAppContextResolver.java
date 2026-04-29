package com.xxx.it.works.wecode.v2.modules.app.resolver.impl;

import com.xxx.it.works.wecode.v2.modules.app.resolver.AppAccessException;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Development environment application context resolver
 *
 * <p>Default implementation for development and testing environments:</p>
 * <ul>
 *   <li>ID conversion: Parse String directly to Long (or return as-is for compatibility)</li>
 *   <li>Permission validation: Skip validation, allow all access</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class DevAppContextResolver implements AppContextResolver {

    @Override
    public AppContext resolveAndValidate(String externalAppId) {
        log.debug("Dev environment resolving appId: {}", externalAppId);

        // Dev environment: Simple parsing, no permission validation
        Long internalId = parseInternalId(externalAppId);

        return AppContext.builder()
            .internalId(internalId)
            .externalId(externalAppId)
            .build();
    }

    @Override
    public String toExternalId(Long internalId) {
        // Dev environment: Return String directly
        return String.valueOf(internalId);
    }

    /**
     * Parse internal ID
     * Supports two formats:
     * 1. Pure numeric string "1001" → 1001L
     * 2. Long business ID, extract trailing number or use default value
     */
    private Long parseInternalId(String externalAppId) {
        if (externalAppId == null || externalAppId.isEmpty()) {
            throw AppAccessException.notFound(externalAppId);
        }

        // Try to parse directly as number
        try {
            return Long.parseLong(externalAppId);
        } catch (NumberFormatException e) {
            // Dev environment: Return default value 1L for non-numeric ID, convenient for testing
            log.debug("Non-numeric appId: {}, using default value 1L", externalAppId);
            return 1L;
        }
    }
}
