package com.xxx.it.works.wecode.v2.modules.auth.credential;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 多认证组合凭据注入器
 *
 * <p>按顺序执行多个认证类型的凭据注入，所有认证信息累积注入到同一个请求头 Map 中</p>
 * <p>委托 {@link CredentialInjectorRegistry} 分发到各子认证注入器</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see spec.md §3.3 连接器配置认证类型
 */
@Slf4j
@Component
public class MultiAuthCredentialInjector implements CredentialInjector {

    private final CredentialInjectorRegistry registry;

    /**
     * 构造函数注入 CredentialInjectorRegistry
     *
     * @param registry 凭据注入器注册中心
     */
    public MultiAuthCredentialInjector(@Lazy CredentialInjectorRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getAuthType() {
        return "MULTI_AUTH";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void inject(Map<String, Object> authConfig, Map<String, String> headers) {
        // 从认证配置中读取子认证配置列表
        List<Map<String, Object>> authConfigs = (List<Map<String, Object>>) authConfig.get("authConfigs");
        if (authConfigs == null || authConfigs.isEmpty()) {
            log.warn("Multi-auth injection skipped: authConfigs is empty");
            return;
        }

        // 按顺序注入每个子认证配置
        int successCount = 0;
        int failCount = 0;
        for (Map<String, Object> subAuthConfig : authConfigs) {
            String subType = (String) subAuthConfig.get("type");
            if (subType == null || subType.isEmpty()) {
                log.warn("Multi-auth sub-config skipped: type is empty");
                failCount++;
                continue;
            }
            try {
                // 委托 Registry 根据子类型分发到对应的注入器
                registry.inject(subAuthConfig, headers);
                successCount++;
            } catch (Exception e) {
                log.error("Multi-auth sub-config injection failed for type={}", subType, e);
                failCount++;
            }
        }
        log.info("Multi-auth injection completed, configsCount={}, successCount={}, failCount={}",
                authConfigs.size(), successCount, failCount);
    }
}
