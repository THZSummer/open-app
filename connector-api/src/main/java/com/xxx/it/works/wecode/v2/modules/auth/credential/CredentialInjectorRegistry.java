package com.xxx.it.works.wecode.v2.modules.auth.credential;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CredentialInjectorRegistry {

    private final Map<String, CredentialInjector> injectors = new HashMap<>();

    public CredentialInjectorRegistry(List<CredentialInjector> injectorList) {
        for (CredentialInjector inj : injectorList) {
            injectors.put(inj.getAuthType(), inj);
        }
    }

    public void inject(Map<String, Object> authConfig, Map<String, String> headers) {
        if (authConfig == null) return;
        String authType = (String) authConfig.get("type");
        CredentialInjector injector = injectors.get(authType);
        if (injector == null) {
            injector = injectors.get("DEFAULT");
        }
        injector.inject(authConfig, headers);
    }
}
