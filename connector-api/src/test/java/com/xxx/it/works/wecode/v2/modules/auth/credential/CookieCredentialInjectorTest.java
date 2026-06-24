package com.xxx.it.works.wecode.v2.modules.auth.credential;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CookieCredentialInjector 测试")
class CookieCredentialInjectorTest {

    private CookieCredentialInjector injector;
    private Map<String, String> headers;

    @BeforeEach
    void setUp() {
        injector = new CookieCredentialInjector();
        headers = new HashMap<>();
    }

    @Test
    @DisplayName("authtype 返回 COOKIE")
    void testGetAuthType_ReturnsCookie() {
        assertEquals("COOKIE", injector.getAuthType());
    }

    @Test
    @DisplayName("名称和值都存在 → 正确注入 Cookie Header")
    void testCookieInject_NormalMatch() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("cookieName", "SESSIONID");
        authConfig.put("cookieValue", "abc123");

        injector.inject(authConfig, headers);

        assertTrue(headers.containsKey("Cookie"));
        assertEquals("SESSIONID=abc123", headers.get("Cookie"));
    }

    @Test
    @DisplayName("cookieName 为 null → 跳过注入")
    void testCookieInject_NameIsNull_Skipped() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("cookieName", null);
        authConfig.put("cookieValue", "abc123");

        injector.inject(authConfig, headers);

        assertFalse(headers.containsKey("Cookie"));
    }

    @Test
    @DisplayName("cookieName 为空字符串 → 跳过注入")
    void testCookieInject_NameIsEmpty_Skipped() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("cookieName", "");
        authConfig.put("cookieValue", "abc123");

        injector.inject(authConfig, headers);

        assertFalse(headers.containsKey("Cookie"));
    }

    @Test
    @DisplayName("cookieValue 为 null → 跳过注入")
    void testCookieInject_ValueIsNull_Skipped() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("cookieName", "SESSIONID");
        authConfig.put("cookieValue", null);

        injector.inject(authConfig, headers);

        assertFalse(headers.containsKey("Cookie"));
    }

    @Test
    @DisplayName("cookieValue 为空字符串 → 跳过注入")
    void testCookieInject_ValueIsEmpty_Skipped() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("cookieName", "SESSIONID");
        authConfig.put("cookieValue", "");

        injector.inject(authConfig, headers);

        assertFalse(headers.containsKey("Cookie"));
    }

    @Test
    @DisplayName("不包含 cookieName → 跳过注入")
    void testCookieInject_NameNotInConfig_Skipped() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("otherKey", "otherValue");

        injector.inject(authConfig, headers);

        assertFalse(headers.containsKey("Cookie"));
    }
}
