package com.xxx.it.works.wecode.v2.modules.auth.credential;

import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.expression.ExpressionResolver;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * COOKIE 凭据提供 — 值从 ExecutionContext 按表达式动态解析。
 *
 * <p>复用 {@link ExpressionResolver} 解析 {@code ${$.node.{id}.{input|output}.{path}}}，
 * 不硬编码字段名或来源，完全由用户配置的 value 表达式决定取值路径。</p>
 */
@Component
public class CookieCredentialSupplier implements CredentialSupplier {

    private final ExpressionResolver expressionResolver = new ExpressionResolver();

    @Override
    public String getAuthType() { return "COOKIE"; }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> resolve(Map<String, Object> fieldDefs, ExecutionContext context) {
        Map<String, String> result = new LinkedHashMap<>();
        if (fieldDefs == null || context == null) return result;

        for (Map.Entry<String, Object> entry : fieldDefs.entrySet()) {
            Map<String, Object> def = (Map<String, Object>) entry.getValue();
            if (def == null) continue;
            String expr = (String) def.get("value");
            if (expr == null) continue;

            Object resolved = expressionResolver.resolve(expr, context.getNodeContexts());
            result.put(expr, resolved != null ? resolved.toString() : "");
        }
        return result;
    }
}
