package com.xxx.it.works.wecode.v2.modules.auth.impl;

import com.xxx.it.works.wecode.v2.modules.auth.AuthValidator;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class SystokenAuthValidator implements AuthValidator {
    @Override
    public String getAuthType() {
        return "SYSTOKEN";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void validate(Map<String, String> headers, Map<String, Object> authConfig) {
        if (headers == null) {
            throw new RuntimeException("Missing X-Sys-Token for SYSTOKEN auth");
        }
        List<Map<String, Object>> fields = (List<Map<String, Object>>) authConfig.get("fields");
        if (fields == null || fields.isEmpty()) {
            return;
        }
        for (Map<String, Object> field : fields) {
            String carrier = (String) field.get("carrier");
            String fieldName = (String) field.get("fieldName");
            if (!"header".equals(carrier) || fieldName == null) {
                continue;
            }
            String token = headers.get(fieldName);
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Missing " + fieldName + " for SYSTOKEN auth");
            }
        }
    }
}
