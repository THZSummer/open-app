package com.xxx.it.works.wecode.v2.modules.auth.credential;

import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 凭据提供注册中心 — 按 authType 分发到对应的 CredentialSupplier 实现。
 */
@Component
public class CredentialSupplierRegistry {

    private final Map<String, CredentialSupplier> supplierMap = new HashMap<>();
    private final CredentialSupplier defaultSupplier;

    public CredentialSupplierRegistry(List<CredentialSupplier> supplierList) {
        CredentialSupplier fallback = null;
        for (CredentialSupplier s : supplierList) {
            if ("DEFAULT".equals(s.getAuthType())) {
                fallback = s;
            } else {
                supplierMap.put(s.getAuthType(), s);
            }
        }
        this.defaultSupplier = fallback;
    }

    public Map<String, String> resolve(String authType, Map<String, Object> fieldDefs,
                                        Map<String, Object> authConfig, ExecutionContext context) {
        CredentialSupplier supplier = supplierMap.getOrDefault(authType, defaultSupplier);
        return supplier != null ? supplier.resolve(fieldDefs, authConfig, context) : new HashMap<>();
    }
}
