package com.xxx.it.works.wecode.v2.modules.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UrlWhitelistValidator 测试")
class UrlWhitelistValidatorTest {

    private UrlWhitelistValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UrlWhitelistValidator();
    }

    @Test
    @DisplayName("空白名单 (null) → 放行任意 URL")
    void testEmptyWhitelistNull_AllowsAll() {
        assertTrue(validator.validate("/api/v1/user/info", null));
    }

    @Test
    @DisplayName("空白名单 (空列表) → 放行任意 URL")
    void testEmptyWhitelistEmptyList_AllowsAll() {
        assertTrue(validator.validate("/api/v1/user/info", Collections.emptyList()));
    }

    @Test
    @DisplayName("单条规则精确匹配 → 通过")
    void testSingleExactMatch_Pass() {
        List<String> whitelist = Collections.singletonList("/api/v1/user/.*");
        assertTrue(validator.validate("/api/v1/user/profile", whitelist));
    }

    @Test
    @DisplayName("多条规则正则组合 → 任意一条匹配即通过")
    void testMultiplePatterns_OneMatch() {
        List<String> whitelist = Arrays.asList("/api/v1/user/.*", "/public/.*", "/health");
        assertTrue(validator.validate("/public/index.html", whitelist));
    }

    @Test
    @DisplayName("多条规则正则组合 → 第二条匹配通过")
    void testMultiplePatterns_SecondMatch() {
        List<String> whitelist = Arrays.asList("/api/v1/admin/.*", "/api/v1/user/.*");
        assertTrue(validator.validate("/api/v1/user/profile", whitelist));
    }

    @Test
    @DisplayName("URL 不在白名单中 → 拒绝")
    void testUrlNotInWhitelist_Denied() {
        List<String> whitelist = Collections.singletonList("/api/v1/public/.*");
        assertFalse(validator.validate("/api/v1/internal/secret", whitelist));
    }

    @Test
    @DisplayName("targetUrl 为空 → 拒绝")
    void testEmptyTargetUrl_Denied() {
        List<String> whitelist = Collections.singletonList("/api/v1/.*");
        assertFalse(validator.validate("", whitelist));
    }

    @Test
    @DisplayName("targetUrl 为 null → 拒绝")
    void testNullTargetUrl_Denied() {
        List<String> whitelist = Collections.singletonList("/api/v1/.*");
        assertFalse(validator.validate(null, whitelist));
    }

    @Test
    @DisplayName("正则缓存 — 相同列表复用编译结果")
    void testRegexPatternCache_ReusesCompiled() {
        List<String> whitelist = Collections.singletonList("/api/v1/.*");

        assertTrue(validator.validate("/api/v1/test1", whitelist));
        assertTrue(validator.validate("/api/v1/test2", whitelist));
        // 第二次调用使用缓存的 Pattern
        assertTrue(validator.validate("/api/v1/test3", whitelist));
    }

    @Test
    @DisplayName("正则缓存 — 不同列表独立编译")
    void testRegexPatternCache_DifferentPatterns() {
        List<String> whitelist1 = Collections.singletonList("/api/v1/.*");
        List<String> whitelist2 = Collections.singletonList("/api/v2/.*");

        assertTrue(validator.validate("/api/v1/test", whitelist1));
        assertFalse(validator.validate("/api/v1/test", whitelist2));
    }

    @Test
    @DisplayName("边界 — 正则捕获特殊字符")
    void testRegexWithSpecialCharacters() {
        List<String> whitelist = Collections.singletonList("/api/v1/data\\.json");
        assertTrue(validator.validate("/api/v1/data.json", whitelist));
    }
}
