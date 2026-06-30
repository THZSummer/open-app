package com.xxx.it.works.wecode.v2.modules.permission.validator;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChannelAddressWhitelistValidator 单元测试
 *
 * @author Summer
 * @since 2026-06-29
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelAddressWhitelistValidator 测试")
class ChannelAddressWhitelistValidatorTest {

    @Mock
    private AppMapper appMapper;

    @InjectMocks
    private ChannelAddressWhitelistValidator validator;

    // ===== 空白名单场景 =====

    @Nested
    @DisplayName("空白名单 = 不限制")
    class EmptyWhitelistTest {

        @Test
        @DisplayName("白名单为空列表 → 放行任意地址")
        void testEmptyWhitelist_AllowAll() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(
                    "channel_address_whitelist", "callback_url_regex"))
                    .thenReturn(Collections.emptyList());

            assertDoesNotThrow(() ->
                    validator.validate("https://any-domain.com/callback", "callback_url_regex"));
        }

        @Test
        @DisplayName("白名单为 null → 放行")
        void testNullWhitelist_Allow() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(
                    "channel_address_whitelist", "event_url_regex"))
                    .thenReturn(null);

            assertDoesNotThrow(() ->
                    validator.validate("https://any-domain.com/event", "event_url_regex"));
        }
    }

    // ===== 地址为空场景 =====

    @Nested
    @DisplayName("地址为空时不校验")
    class EmptyAddressTest {

        @Test
        @DisplayName("channelAddress 为 null → 放行，不查 DB")
        void testNullAddress_Skip() {
            validator.validate(null, "callback_url_regex");
            verify(appMapper, never()).selectDictionaryValuesByPathAndCodePrefix(anyString(), anyString());
        }

        @Test
        @DisplayName("channelAddress 为空字符串 → 放行，不查 DB")
        void testEmptyAddress_Skip() {
            validator.validate("", "callback_url_regex");
            verify(appMapper, never()).selectDictionaryValuesByPathAndCodePrefix(anyString(), anyString());
        }

        @Test
        @DisplayName("channelAddress 为纯空格 → 放行，不查 DB")
        void testBlankAddress_Skip() {
            validator.validate("   ", "event_url_regex");
            verify(appMapper, never()).selectDictionaryValuesByPathAndCodePrefix(anyString(), anyString());
        }
    }

    // ===== 命中放行场景 =====

    @Nested
    @DisplayName("命中白名单规则 → 放行")
    class MatchAllowedTest {

        @Test
        @DisplayName("命中第一条规则 → 放行")
        void testMatchFirstRule_Allow() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(
                    "channel_address_whitelist", "callback_url_regex"))
                    .thenReturn(List.of("^https://safe\\.com/.*$"));

            assertDoesNotThrow(() ->
                    validator.validate("https://safe.com/callback", "callback_url_regex"));
        }

        @Test
        @DisplayName("命中最后一条规则 → 放行")
        void testMatchLastRule_Allow() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(
                    "channel_address_whitelist", "callback_url_regex"))
                    .thenReturn(Arrays.asList(
                            "^https://a\\.com/.*$",
                            "^https://b\\.com/.*$",
                            "^https://safe\\.com/.*$"));

            assertDoesNotThrow(() ->
                    validator.validate("https://safe.com/callback", "callback_url_regex"));
        }

        @Test
        @DisplayName("全串匹配：精确地址 → 放行")
        void testExactMatch_Allow() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(
                    "channel_address_whitelist", "event_url_regex"))
                    .thenReturn(List.of("^https://exact\\.com/webhook$"));

            assertDoesNotThrow(() ->
                    validator.validate("https://exact.com/webhook", "event_url_regex"));
        }
    }

    // ===== 不命中拒绝场景 =====

    @Nested
    @DisplayName("不命中白名单规则 → 拒绝")
    class NotMatchRejectTest {

        @Test
        @DisplayName("全部不命中 → 抛 400 BusinessException")
        void testNoMatch_Throw400() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(
                    "channel_address_whitelist", "callback_url_regex"))
                    .thenReturn(List.of("^https://safe\\.com/.*$"));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    validator.validate("https://evil.com/steal", "callback_url_regex"));

            assertEquals("400", ex.getCode());
            assertTrue(ex.getMessageZh().contains("通道地址不在允许范围内"));
        }

        @Test
        @DisplayName("全串匹配：地址有额外路径 → 拒绝（非 find）")
        void testPartialMatch_Reject() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(
                    "channel_address_whitelist", "event_url_regex"))
                    .thenReturn(List.of("^https://exact\\.com/webhook$"));

            // 规则要求精确匹配，地址多了 /extra 路径 → 不匹配
            assertThrows(BusinessException.class, () ->
                    validator.validate("https://exact.com/webhook/extra", "event_url_regex"));
        }

        @Test
        @DisplayName("通配符域名规则 → 放行")
        void testWildcardDomain_Allow() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(
                    "channel_address_whitelist", "callback_url_regex"))
                    .thenReturn(List.of("^https://.*\\.corp\\.example\\.com/.*$"));

            assertDoesNotThrow(() ->
                    validator.validate("https://api.corp.example.com/callback", "callback_url_regex"));
        }

        @Test
        @DisplayName("通配符域名规则：不在域名范围内 → 拒绝")
        void testWildcardDomain_Reject() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(
                    "channel_address_whitelist", "callback_url_regex"))
                    .thenReturn(List.of("^https://.*\\.corp\\.example\\.com/.*$"));

            assertThrows(BusinessException.class, () ->
                    validator.validate("https://evil.com/corp.example.com", "callback_url_regex"));
        }
    }

    // ===== 防御性场景 =====

    @Nested
    @DisplayName("防御性设计")
    class DefensiveTest {

        @Test
        @DisplayName("DB 查询异常 → 降级放行（不阻塞业务）")
        void testDbException_FallbackAllow() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(anyString(), anyString()))
                    .thenThrow(new RuntimeException("DB connection lost"));

            assertDoesNotThrow(() ->
                    validator.validate("https://any.com/callback", "callback_url_regex"));
        }

        @Test
        @DisplayName("白名单含非法正则 → 跳过非法规则，继续匹配")
        void testInvalidRegex_Skip() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(
                    "channel_address_whitelist", "callback_url_regex"))
                    .thenReturn(Arrays.asList(
                            "[invalid-regex",      // 非法正则，应跳过
                            "^https://safe\\.com/.*$"  // 合法正则，应命中
                    ));

            assertDoesNotThrow(() ->
                    validator.validate("https://safe.com/callback", "callback_url_regex"));
        }

        @Test
        @DisplayName("全部为非法正则 → 无有效规则可命中 → 拒绝")
        void testAllInvalidRegex_Reject() {
            when(appMapper.selectDictionaryValuesByPathAndCodePrefix(
                    "channel_address_whitelist", "callback_url_regex"))
                    .thenReturn(Arrays.asList("[invalid-1", "[invalid-2"));

            assertThrows(BusinessException.class, () ->
                    validator.validate("https://any.com/callback", "callback_url_regex"));
        }
    }

    // ===== 常量验证 =====

    @Nested
    @DisplayName("常量定义")
    class ConstantsTest {

        @Test
        @DisplayName("code 前缀常量值正确")
        void testCodePrefixConstants() {
            assertEquals("callback_url_regex", ChannelAddressWhitelistValidator.CODE_PREFIX_CALLBACK);
            assertEquals("event_url_regex", ChannelAddressWhitelistValidator.CODE_PREFIX_EVENT);
        }
    }
}
