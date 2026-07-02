package com.xxx.it.works.wecode.v2.modules.auth.credential;

import java.util.LinkedHashMap;
import java.util.Map;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import org.springframework.stereotype.Component;

/**
 * NONE 凭据提供 — 无认证场景的显式空实现。
 */
@Component
public class NoneCredentialSupplier implements CredentialSupplier {

    @Override
    public String getAuthType() { return "NONE"; }

    @Override
    public Map<String, String> resolve(Map<String, Object> fieldDefs, Map<String, Object> authConfig, ExecutionContext context) {
        return new LinkedHashMap<>();
    }
}
