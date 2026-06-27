package com.xxx.it.works.wecode.v2.modules.auth.credential;

import com.xxx.it.works.wecode.v2.common.annotation.StandardTodo;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SystokenCredentialSupplier implements CredentialSupplier {

    @Override
    public String getAuthType() { return "SYSTOKEN"; }

    @Override
    public Map<String, String> resolve(Map<String, Object> fieldDefs, ExecutionContext context) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("${$.system.env.sysToken}", getToken());
        return result;
    }

    @StandardTodo("对接凭据管理服务获取 sysToken")
    private String getToken() { return ""; }
}
