package com.xxx.it.works.wecode.v2.modules.auth.credential.impl;

import com.xxx.it.works.wecode.v2.modules.auth.credential.CredentialInjector;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DefaultCredentialInjector implements CredentialInjector {

    @Override
    public String getAuthType() { return "DEFAULT"; }

    @Override
    @SuppressWarnings("unchecked")
    public void inject(Map<String, Object> authConfig, Map<String, String> headers) {
        List<Map<String, Object>> fields = (List<Map<String, Object>>) authConfig.get("fields");
        if (fields == null) return;
        for (Map<String, Object> field : fields) {
            String carrier = (String) field.get("carrier");
            String fieldName = (String) field.get("fieldName");
            if ("header".equals(carrier) && fieldName != null && !headers.containsKey(fieldName)) {
                headers.put(fieldName, "");
            }
        }
    }
}
