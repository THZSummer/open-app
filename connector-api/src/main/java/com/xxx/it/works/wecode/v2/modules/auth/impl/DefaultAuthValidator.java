package com.xxx.it.works.wecode.v2.modules.auth.impl;

import com.xxx.it.works.wecode.v2.modules.auth.AuthValidator;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class DefaultAuthValidator implements AuthValidator {
    @Override
    public String getAuthType() {
        return "DEFAULT";
    }

    @Override
    public void validate(Map<String, String> headers, Map<String, Object> authConfig) {
    }
}
