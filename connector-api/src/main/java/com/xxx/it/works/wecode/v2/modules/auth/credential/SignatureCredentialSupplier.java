package com.xxx.it.works.wecode.v2.modules.auth.credential;

import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SignatureCredentialSupplier implements CredentialSupplier {

    @Override
    public String getAuthType() { return "SIGNATURE"; }

    @Override
    public Map<String, String> resolve(Map<String, Object> fieldDefs, ExecutionContext context) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("${$.constant:user-configured-secret-key}", "");
        result.put("${$.system.env.signature}", "");
        // TODO: 对接凭据管理服务，实现签名逻辑
        return result;
    }
}
