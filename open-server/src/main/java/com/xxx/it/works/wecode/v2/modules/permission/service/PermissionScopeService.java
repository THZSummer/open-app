package com.xxx.it.works.wecode.v2.modules.permission.service;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PermissionScopeService {

    private static final Set<String> RESOURCE_TYPES = Set.of("api", "event", "callback");
    private static final Pattern SCOPE_PATTERN =
            Pattern.compile("^(api|event|callback):[a-z][a-z0-9_]*:[a-z][a-z0-9_-]*$");

    public void validateScope(String resourceType, String scope) {
        if (!RESOURCE_TYPES.contains(resourceType)) {
            throw BusinessException.badRequest("Invalid resource type", "Invalid resource type");
        }
        if (scope == null || !SCOPE_PATTERN.matcher(scope).matches()
                || !scope.startsWith(resourceType + ":")) {
            throw BusinessException.badRequest(
                    "Invalid scope format. Expected: " + resourceType + ":{module}:{identifier}",
                    "Invalid scope format. Expected: " + resourceType + ":{module}:{identifier}");
        }
    }
}
