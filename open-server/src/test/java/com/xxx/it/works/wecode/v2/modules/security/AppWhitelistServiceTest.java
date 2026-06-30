package com.xxx.it.works.wecode.v2.modules.security;

import com.xxx.it.works.wecode.v2.common.enums.ConnectorPlatformConstants;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookupWhitelistMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppWhitelistService 测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppWhitelistServiceTest {

    @Mock
    private LookupWhitelistMapper lookupWhitelistMapper;

    @InjectMocks
    private AppWhitelistService service;

    @BeforeEach
    void setUp() {
        lenient().when(lookupWhitelistMapper.selectItemValuesByClassifyCode(ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_WHITELIST)).thenReturn(null);
    }

    @Test
    @DisplayName("Lookup 返回 null → 拒绝（安全默认）")
    void testLookupNull_DeniesAll() {
        assertFalse(service.isWhitelisted("1"));
        assertFalse(service.isWhitelisted("999"));
    }

    @Test
    @DisplayName("Lookup 返回空列表 → 拒绝（安全默认）")
    void testLookupEmpty_DeniesAll() {
        when(lookupWhitelistMapper.selectItemValuesByClassifyCode(ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_WHITELIST))
                .thenReturn(List.of());

        assertFalse(service.isWhitelisted("1"));
    }

    @Test
    @DisplayName("appId 为 null → 拒绝")
    void testNullAppId_Denied() {
        when(lookupWhitelistMapper.selectItemValuesByClassifyCode(ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_WHITELIST))
                .thenReturn(List.of("1", "2", "3"));

        assertFalse(service.isWhitelisted((String) null));
    }

    @Test
    @DisplayName("appId 为空字符串 → 拒绝")
    void testEmptyAppId_Denied() {
        when(lookupWhitelistMapper.selectItemValuesByClassifyCode(ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_WHITELIST))
                .thenReturn(List.of("1", "2", "3"));

        assertFalse(service.isWhitelisted(""));
        assertFalse(service.isWhitelisted("  "));
    }

    @Test
    @DisplayName("appId 在白名单中 → 通过")
    void testAppIdInWhitelist_Pass() {
        when(lookupWhitelistMapper.selectItemValuesByClassifyCode(ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_WHITELIST))
                .thenReturn(List.of("app_001", "app_002", "app_003"));

        assertTrue(service.isWhitelisted("app_002"));
    }

    @Test
    @DisplayName("appId 不在白名单中 → 拒绝")
    void testAppIdNotInWhitelist_Denied() {
        when(lookupWhitelistMapper.selectItemValuesByClassifyCode(ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_WHITELIST))
                .thenReturn(List.of("app_001", "app_002"));

        assertFalse(service.isWhitelisted("app_999"));
    }

    @Test
    @DisplayName("Lookup 查询返回结果 → 使用 Lookup 数据")
    void testLookupWhitelist_Used() {
        when(lookupWhitelistMapper.selectItemValuesByClassifyCode(ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_WHITELIST))
                .thenReturn(List.of("app_a", "app_b", "app_c"));

        assertTrue(service.isWhitelisted("app_a"));
        assertFalse(service.isWhitelisted("app_x"));
    }

    @Test
    @DisplayName("Lookup 抛出异常 → 拒绝（安全默认）")
    void testLookupException_Denied() {
        when(lookupWhitelistMapper.selectItemValuesByClassifyCode(ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_WHITELIST))
                .thenThrow(new RuntimeException("DB error"));

        assertFalse(service.isWhitelisted("5"));
    }

    @Test
    @DisplayName("Long 兼容方法 → 内部转 String 后校验")
    void testLongCompatMethod() {
        when(lookupWhitelistMapper.selectItemValuesByClassifyCode(ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_WHITELIST))
                .thenReturn(List.of("100", "200", "300"));

        assertTrue(service.isWhitelisted(200L));
        assertFalse(service.isWhitelisted(999L));
    }

}
