package com.xxx.it.works.wecode.v2.modules.security;

import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookupWhitelistMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppWhitelistService 测试")
@ExtendWith(MockitoExtension.class)
class AppWhitelistServiceTest {

    @Mock
    private LookupWhitelistMapper lookupWhitelistMapper;

    @InjectMocks
    private AppWhitelistService service;

    @BeforeEach
    void setUp() {
        // Default: Lookup returns null → triggers property fallback
        lenient().when(lookupWhitelistMapper.selectItemValuesByClassifyCode("app_whitelist")).thenReturn(null);
    }

    @Test
    @DisplayName("空白名单配置 + Lookup 降级 → 拒绝（安全默认）")
    void testEmptyWhitelist_DeniesAll() {
        ReflectionTestUtils.setField(service, "whitelistConfig", "");

        assertFalse(service.isWhitelisted(1L));
        assertFalse(service.isWhitelisted(999L));
    }

    @Test
    @DisplayName("null 白名单配置 + Lookup 降级 → 拒绝（安全默认）")
    void testNullWhitelist_DeniesAll() {
        ReflectionTestUtils.setField(service, "whitelistConfig", null);

        assertFalse(service.isWhitelisted(1L));
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
    @DisplayName("Lookup 查询返回结果 → 使用 Lookup 数据")
    void testLookupWhitelist_UsedWhenAvailable() {
        ReflectionTestUtils.setField(service, "whitelistConfig", "999");
        when(lookupWhitelistMapper.selectItemValuesByClassifyCode("app_whitelist"))
                .thenReturn(java.util.List.of("1", "2", "3"));

        assertTrue(service.isWhitelisted(1L));
        assertFalse(service.isWhitelisted(999L));
    }

    @Test
    @DisplayName("Lookup 抛出异常 → 降级到属性配置")
    void testLookupException_FallbackToProperty() {
        ReflectionTestUtils.setField(service, "whitelistConfig", "5,6");
        when(lookupWhitelistMapper.selectItemValuesByClassifyCode("app_whitelist"))
                .thenThrow(new RuntimeException("DB error"));

        assertTrue(service.isWhitelisted(5L));
        assertFalse(service.isWhitelisted(1L));
    }

    @Test
    @DisplayName("market-server 不可用 → isWhitelistedWithMarketFallback 降级放行")
    void testMarketFallback_Pass() {
        when(lookupWhitelistMapper.selectItemValuesByClassifyCode("app_whitelist"))
                .thenThrow(new RuntimeException("Market unavailable"));

        assertTrue(service.isWhitelistedWithMarketFallback(1L));
    }

    @Test
    @DisplayName("非白名单应用 → isWhitelistedWithMarketFallback 拒绝")
    void testMarketFallback_Denied() {
        ReflectionTestUtils.setField(service, "whitelistConfig", "1,2");

        assertFalse(service.isWhitelistedWithMarketFallback(999L));
    }
}
