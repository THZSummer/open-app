package com.xxx.it.works.wecode.v2.modules.auth.credential;

import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 统一凭证处理器 — 合并 Resolver + Processor + Injector 三条链。
 *
 * <p>输入 authConfigs 数组 + 目标容器，直接完成凭据解析和注入。
 * 流程：遍历 authConfigs → 按 authType 分派 CredentialSupplier → 注入 header/query。</p>
 *
 * @author SDDU Build Agent
 * @version 1.2.0
 * @see plan-json-schema.md §4.3.2 authConfigDef
 */
@Component
public class UnifiedCredentialProcessor {

    private static final Logger log = LoggerFactory.getLogger(UnifiedCredentialProcessor.class);

    private final CredentialSupplierRegistry supplierRegistry;

    public UnifiedCredentialProcessor(CredentialSupplierRegistry supplierRegistry) {
        this.supplierRegistry = supplierRegistry;
    }

    /**
     * 统一入口：遍历 authConfigs 数组，逐项解析凭据并注入目标容器。
     *
     * @param authConfigsObj authConfigs 数组
     * @param headers        目标 HTTP headers（会被 mutate）
     * @param queryParams    目标 query 参数（会被 mutate）
     * @param context        运行时上下文
     */
    @SuppressWarnings("unchecked")
    public void apply(Object authConfigsObj,
                      Map<String, String> headers,
                      Map<String, Object> queryParams,
                      ExecutionContext context) {
        if (!(authConfigsObj instanceof List)) { return; }
        List<Map<String, Object>> authConfigs = (List<Map<String, Object>>) authConfigsObj;
        for (Map<String, Object> ac : authConfigs) {
            if (ac != null) {
                injectOne(ac, headers, queryParams, context);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void injectOne(Map<String, Object> authConfig,
                           Map<String, String> headers,
                           Map<String, Object> queryParams,
                           ExecutionContext context) {
        String authType = (String) authConfig.get("type");
        if (authType == null) {
            log.warn("authConfig missing 'type', skipping");
            return;
        }

        Map<String, Object> headerContainer = (Map<String, Object>) authConfig.get("header");
        if (headerContainer != null && headers != null) {
            injectContainer(authType, authConfig, headerContainer, (Map) headers, true, context);
        }

        Map<String, Object> queryContainer = (Map<String, Object>) authConfig.get("query");
        if (queryContainer != null && queryParams != null) {
            injectContainer(authType, authConfig, queryContainer, queryParams, false, context);
        }
    }

    @SuppressWarnings("unchecked")
    private void injectContainer(String authType, Map<String, Object> authConfig,
                                  Map<String, Object> container,
                                  Map<String, Object> target,
                                  boolean stringTarget,
                                  ExecutionContext context) {
        Map<String, Object> properties = (Map<String, Object>) container.get("properties");
        if (properties == null || properties.isEmpty()) {
            return;
        }

        Map<String, String> exprToValue = supplierRegistry.resolve(authType, properties, authConfig, context);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String fieldName = entry.getKey();
            Map<String, Object> fieldDef = (Map<String, Object>) entry.getValue();
            if (fieldDef == null) { continue; }

            String expr = (String) fieldDef.get("value");
            String resolved = exprToValue.get(expr);
            Object value;
            if (resolved != null) {
                value = resolved;
            } else if (expr != null) {
                value = expr;
            } else {
                value = "";
            }

            if (stringTarget) {
                target.put(fieldName, value != null ? value.toString() : "");
            } else {
                target.put(fieldName, value);
            }
        }
    }
}
