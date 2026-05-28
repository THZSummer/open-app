package com.xxx.it.works.wecode.v2.modules.auth;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AuthValidatorRegistry {
    private final Map<String, AuthValidator> validators = new HashMap<>();

    public AuthValidatorRegistry(List<AuthValidator> validatorList) {
        for (AuthValidator v : validatorList) {
            validators.put(v.getAuthType(), v);
        }
    }

    public void validate(Map<String, Object> authConfig, Map<String, String> headers) {
        if (authConfig == null) {
            return;
        }
        String authType = (String) authConfig.get("type");
        AuthValidator validator = validators.get(authType);
        if (validator == null) {
            validator = validators.get("DEFAULT");
        }
        validator.validate(headers, authConfig);
    }
}
