package com.xxx.it.works.wecode.v2.modules.auth.credential;

import com.xxx.it.works.wecode.v2.common.annotation.StandardTodo;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AkskCredentialSupplier implements CredentialSupplier {

    @Override
    public String getAuthType() { return "AKSK"; }

    @Override
    public Map<String, String> resolve(Map<String, Object> fieldDefs, Map<String, Object> authConfig, ExecutionContext context) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("${$.system.env.akskSignature}", getSignature());
        return result;
    }

    @StandardTodo("对接凭据管理服务获取 AKSK 密钥对，引擎动态签名")
    private String getSignature() { return ""; }
}
