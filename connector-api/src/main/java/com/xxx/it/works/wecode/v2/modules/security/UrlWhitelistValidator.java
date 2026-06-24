package com.xxx.it.works.wecode.v2.modules.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * URL 白名单验证器
 *
 * <p>验证目标 URL 是否匹配白名单正则表达式模式列表</p>
 * <p>安全策略：空白名单（null 或空列表）视为无限制，允许所有 URL 通过</p>
 * <p>使用 ConcurrentHashMap 缓存编译后的正则表达式 Pattern，提升重复验证性能</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see spec.md §5.2 安全白名单校验
 */
@Slf4j
@Component
public class UrlWhitelistValidator {

    /** 模式缓存，key 为白名单列表的 hashCode，value 为编译后的组合正则表达式 */
    private final Map<Integer, Pattern> patternCache = new ConcurrentHashMap<>();

    /**
     * 验证目标 URL 是否在白名单中
     *
     * @param targetUrl         目标 URL（如 "/api/v1/user/info"）
     * @param whitelistPatterns 白名单正则表达式模式列表（如 ["/api/v1/.*", "/public/.*"]）
     * @return true-允许访问, false-拒绝访问
     */
    public boolean validate(String targetUrl, List<String> whitelistPatterns) {
        // 空白名单 = 无限制，允许所有 URL
        if (whitelistPatterns == null || whitelistPatterns.isEmpty()) {
            return true;
        }

        // 目标 URL 为空则拒绝
        if (targetUrl == null || targetUrl.isEmpty()) {
            log.warn("URL whitelist validation rejected: targetUrl is empty");
            return false;
        }

        try {
            // 编译白名单模式（优先使用缓存）
            Pattern combinedPattern = getOrCompilePattern(whitelistPatterns);

            // 匹配目标 URL
            if (combinedPattern.matcher(targetUrl).matches()) {
                return true;
            }

            // 未匹配到任何模式
            log.warn("URL not in whitelist: targetUrl={}", targetUrl);
            return false;
        } catch (Exception e) {
            log.error("URL whitelist validation error: targetUrl={}", targetUrl, e);
            return false;
        }
    }

    /**
     * 获取或编译白名单正则表达式（带缓存）
     *
     * <p>将多个模式用 "|" 连接，组合为 ^(pattern1|pattern2|...)$ 格式</p>
     *
     * @param patterns 白名单模式列表
     * @return 编译后的组合正则表达式
     */
    private Pattern getOrCompilePattern(List<String> patterns) {
        int cacheKey = patterns.hashCode();
        Pattern cached = patternCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 编译组合正则表达式：^(pattern1|pattern2|...)$
        String combinedRegex = "^(" + String.join("|", patterns) + ")$";
        Pattern newPattern = Pattern.compile(combinedRegex);
        patternCache.put(cacheKey, newPattern);
        log.debug("URL whitelist pattern compiled and cached, patternsCount={}", patterns.size());
        return newPattern;
    }
}
