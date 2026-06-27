package com.xxx.it.works.wecode.v2.modules.auth.credential;

import java.util.LinkedHashMap;
import java.util.Map;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import org.springframework.stereotype.Component;

/**
 * 凭据提供桩实现 — 默认降级，兜底所有未匹配的 authType。
 */
@Component
public class StubCredentialSupplier implements CredentialSupplier {

    @Override
    public String getAuthType() { return "DEFAULT"; }

    @Override
    public Map<String, String> resolve(Map<String, Object> fieldDefs, ExecutionContext context) {
        return new LinkedHashMap<>();
    }
}
