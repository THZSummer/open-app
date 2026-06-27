package com.xxx.it.works.wecode.v2.modules.auth.credential;

import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApigCredentialSupplier implements CredentialSupplier {

    @Value("${openapp.credential.apig.appkey:}")
    private String appKey;

    @Value("${openapp.credential.apig.appsecret:}")
    private String appSecret;

    @Override
    public String getAuthType() { return "APIG"; }

    @Override
    public Map<String, String> resolve(Map<String, Object> fieldDefs, ExecutionContext context) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("${$.system.env.apigAppKey}", appKey);
        result.put("${$.system.env.apigAppSecret}", appSecret);
        return result;
    }
}
