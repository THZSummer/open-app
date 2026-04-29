package com.xxx.it.works.wecode.v2.common.user.strategy.impl;

import com.xxx.it.works.wecode.v2.common.user.strategy.UserResolveStrategy;
import com.xxx.it.works.wecode.v2.common.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Standard environment user resolution strategy
 *
 * <p>Default user resolution strategy for non-development environments (test, staging, production, etc.)</p>
 * <p>Implementation to be added after environment authentication method is determined</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class StandardUserStrategy implements UserResolveStrategy {

    @Override
    public UserContext resolve(HttpServletRequest request) {
        // TODO: Reserved implementation for standard environment
        // Possible solutions:
        // 1. Parse from APIG Header: request.getHeader("X-User-Id")
        // 2. Parse from IAM Token: JWT parsing
        // 3. Parse from SOA Header: request.getHeader("SVC-USER-ID")
        // 4. Parse from Session

        log.debug("Standard environment user resolution strategy not yet implemented");
        return null;
    }

    @Override
    public boolean supports(String activeProfile) {
        // Default strategy for non-development environments
        // Supports: test, uat, prod, production and all other non-development environments
        return !"dev".equals(activeProfile)
                && !"development".equals(activeProfile)
                && !"local".equals(activeProfile);
    }
}
