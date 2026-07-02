package com.xxx.it.works.wecode.v2.modules.auth.credential;

import com.xxx.it.works.wecode.v2.common.annotation.StandardTodo;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@StandardTodo("默认兜底实现，所有未匹配 authType 走此 Supplier")
@Component
public class StubCredentialSupplier implements CredentialSupplier {

    @Override
    public String getAuthType() { return "DEFAULT"; }

    @Override
    public Map<String, String> resolve(Map<String, Object> fieldDefs, Map<String, Object> authConfig, ExecutionContext context) {
        return new LinkedHashMap<>();
    }
}
