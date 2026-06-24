package com.xxx.it.works.wecode.v2.modules.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SystokenWhitelistValidator 测试")
class SystokenWhitelistValidatorTest {

    private SystokenWhitelistValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SystokenWhitelistValidator();
    }

    @Test
    @DisplayName("空白名单 (null) → 全部禁止")
    void testEmptyWhitelistNull_DeniesAll() {
        assertFalse(validator.validate("valid-token", null));
    }

    @Test
    @DisplayName("空白名单 (空列表) → 全部禁止")
    void testEmptyWhitelistEmptyList_DeniesAll() {
        assertFalse(validator.validate("valid-token", Collections.emptyList()));
    }

    @Test
    @DisplayName("systoken 为空 → 拒绝")
    void testEmptySystoken_Denied() {
        List<String> whitelist = Collections.singletonList("valid-token");
        assertFalse(validator.validate("", whitelist));
    }

    @Test
    @DisplayName("systoken 为 null → 拒绝")
    void testNullSystoken_Denied() {
        List<String> whitelist = Collections.singletonList("valid-token");
        assertFalse(validator.validate(null, whitelist));
    }

    @Test
    @DisplayName("在白名单中 → 通过")
    void testTokenInWhitelist_Pass() {
        List<String> whitelist = Arrays.asList("token-a", "token-b", "token-c");
        assertTrue(validator.validate("token-b", whitelist));
    }

    @Test
    @DisplayName("不在白名单中 → 拒绝")
    void testTokenNotInWhitelist_Denied() {
        List<String> whitelist = Arrays.asList("token-a", "token-b");
        assertFalse(validator.validate("token-unknown", whitelist));
    }

    @Test
    @DisplayName("白名单包含多个令牌 → 精确匹配")
    void testMultipleTokens_ExactMatch() {
        List<String> whitelist = Arrays.asList("prod-token-1", "prod-token-2", "prod-token-3");
        assertTrue(validator.validate("prod-token-2", whitelist));
        assertFalse(validator.validate("prod-token-4", whitelist));
    }

    @Test
    @DisplayName("URL 白名单策略相反的验证 — 空白名单全禁")
    void testOppositeToUrlWhitelist_EmptyDenies() {
        // 与 UrlWhitelistValidator 相反: URL 空 → 放行, SYSTOKEN 空 → 拒绝
        assertFalse(validator.validate("any-token", null));
        assertFalse(validator.validate("any-token", Collections.emptyList()));
    }
}
