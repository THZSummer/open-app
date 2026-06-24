package com.xxx.it.works.wecode.v2.modules.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppWhitelistService 测试")
class AppWhitelistServiceTest {

    private AppWhitelistService service;

    @BeforeEach
    void setUp() {
        service = new AppWhitelistService();
    }

    @Test
    @DisplayName("空白名单配置 → 任意应用放行")
    void testEmptyWhitelist_AllowsAll() {
        ReflectionTestUtils.setField(service, "whitelistConfig", "");

        assertTrue(service.isWhitelisted(1L));
        assertTrue(service.isWhitelisted(999L));
    }

    @Test
    @DisplayName("null 白名单配置 → 任意应用放行")
    void testNullWhitelist_AllowsAll() {
        ReflectionTestUtils.setField(service, "whitelistConfig", null);

        assertTrue(service.isWhitelisted(1L));
    }

    @Test
    @DisplayName("appId 在配置中 → 通过")
    void testAppIdInWhitelist_Pass() {
        ReflectionTestUtils.setField(service, "whitelistConfig", "1,2,3");

        assertTrue(service.isWhitelisted(2L));
    }

    @Test
    @DisplayName("appId 不在配置中 → 拒绝")
    void testAppIdNotInWhitelist_Denied() {
        ReflectionTestUtils.setField(service, "whitelistConfig", "1,2,3");

        assertFalse(service.isWhitelisted(999L));
    }

    @Test
    @DisplayName("appId 为 null → 拒绝")
    void testNullAppId_Denied() {
        ReflectionTestUtils.setField(service, "whitelistConfig", "1,2,3");

        assertFalse(service.isWhitelisted(null));
    }

    @Test
    @DisplayName("白名单含空格 → 正确解析")
    void testWhitelistWithSpaces_ParsedCorrectly() {
        ReflectionTestUtils.setField(service, "whitelistConfig", " 1 , 2 , 3 ");

        assertTrue(service.isWhitelisted(1L));
        assertTrue(service.isWhitelisted(2L));
        assertTrue(service.isWhitelisted(3L));
        assertFalse(service.isWhitelisted(4L));
    }

    @Test
    @DisplayName("market-server 不可用 → isWhitelistedWithMarketFallback 降级放行")
    void testMarketFallback_Pass() {
        ReflectionTestUtils.setField(service, "whitelistConfig", "");

        // isWhitelistedWithMarketFallback 当前直接委托给 isWhitelisted
        assertTrue(service.isWhitelistedWithMarketFallback(1L));
    }

    @Test
    @DisplayName("非白名单应用 → isWhitelistedWithMarketFallback 拒绝")
    void testMarketFallback_Denied() {
        ReflectionTestUtils.setField(service, "whitelistConfig", "1,2");

        assertFalse(service.isWhitelistedWithMarketFallback(999L));
    }
}
