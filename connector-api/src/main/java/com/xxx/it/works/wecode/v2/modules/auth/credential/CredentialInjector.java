package com.xxx.it.works.wecode.v2.modules.auth.credential;

import java.util.Map;

public interface CredentialInjector {

    String getAuthType();

    void inject(Map<String, Object> authConfig, Map<String, String> headers);
}
