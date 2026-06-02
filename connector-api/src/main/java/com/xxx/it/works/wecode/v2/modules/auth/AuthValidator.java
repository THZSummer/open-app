package com.xxx.it.works.wecode.v2.modules.auth;

import java.util.Map;

public interface AuthValidator {
    String getAuthType();
    void validate(Map<String, String> headers, Map<String, Object> authConfig);
}
