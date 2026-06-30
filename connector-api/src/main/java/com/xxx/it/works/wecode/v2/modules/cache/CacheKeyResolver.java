package com.xxx.it.works.wecode.v2.modules.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 缓存 Key 解析器 (connector-api)
 * <p>
 * 将带有表达式的 keyTemplate (如 {@code ${$.trigger.input.body.userId}})
 * 解析为实际的缓存 key 字符串。
 * MVP 阶段: 简单正则替换, 从 context Map 中取值。
 * </p>
 */
@Component
public class CacheKeyResolver {

    private static final Logger log = LoggerFactory.getLogger(CacheKeyResolver.class);

    /** 表达式模式: ${...} */
    private static final Pattern EXPR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /** 默认 key (当 keyTemplate 为空时) */
    static final String DEFAULT_KEY = "_default";

    public CacheKeyResolver() {
    }

    /**
     * 解析缓存 key 模板为实际 key 字符串
     *
     * @param keyTemplate key 模板, 如 {@code ${$.trigger.input.body.userId}}
     * @param context     上下文变量, key 为表达式路径 (如 "$.trigger.input.body.userId"), value 为实际值
     * @return 解析后的 key 字符串
     */
    public String resolveKey(String keyTemplate, Map<String, Object> context) {
        if (keyTemplate == null || keyTemplate.isBlank()) {
            return DEFAULT_KEY;
        }

        if (context == null || context.isEmpty()) {
            // 无上下文, 直接返回模板本身 (去掉 ${} 包裹)
            return stripExpression(keyTemplate);
        }

        String result = keyTemplate;
        Matcher matcher = EXPR_PATTERN.matcher(keyTemplate);

        while (matcher.find()) {
            String fullExpr = matcher.group(0); // 示例：${$.trigger.input.body.userId}
            String exprPath = matcher.group(1); // 示例：$.trigger.input.body.userId

            // 从 context 中查找对应的值
            Object value = context.get(exprPath);
            if (value != null) {
                result = result.replace(fullExpr, String.valueOf(value));
            } else {
                // 未找到值时, 保留原表达式但去掉 ${}
                result = result.replace(fullExpr, exprPath);
                log.debug("Expression '{}' not resolved in context, using raw path", exprPath);
            }
        }

        return result;
    }

    /**
     * 去掉表达式中的 ${} 包裹
     */
    private String stripExpression(String expr) {
        Matcher m = EXPR_PATTERN.matcher(expr);
        if (m.find()) {
            return m.group(1);
        }
        return expr;
    }
}
