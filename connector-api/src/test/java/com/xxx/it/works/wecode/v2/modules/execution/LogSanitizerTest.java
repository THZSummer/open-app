package com.xxx.it.works.wecode.v2.modules.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LogSanitizer 测试")
class LogSanitizerTest {

    private LogSanitizer sanitizer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sanitizer = new LogSanitizer(objectMapper);
    }

    @Test
    @DisplayName("password 字段脱敏为 ***")
    void testSanitize_PasswordField() {
        Map<String, Object> data = new HashMap<>();
        data.put("username", "admin");
        data.put("password", "secret123");

        Map<String, Object> result = sanitizer.sanitize(data);

        assertEquals("admin", result.get("username"));
        assertEquals("***", result.get("password"));
    }

    @Test
    @DisplayName("token 字段脱敏为 ***")
    void testSanitize_TokenField() {
        Map<String, Object> data = new HashMap<>();
        data.put("token", "bearer-xyz-123");

        Map<String, Object> result = sanitizer.sanitize(data);

        assertEquals("***", result.get("token"));
    }

    @Test
    @DisplayName("secretKey 和 signSecretKey 字段脱敏 — 使用 snake_case 匹配")
    void testSanitize_SecretKeyFields() {
        Map<String, Object> data = new HashMap<>();
        data.put("secret_key", "sk-abc");
        data.put("sign_secret_key", "sig-def");

        Map<String, Object> result = sanitizer.sanitize(data);

        assertEquals("***", result.get("secret_key"));
        assertEquals("***", result.get("sign_secret_key"));
    }

    @Test
    @DisplayName("嵌套 JSON 递归脱敏")
    void testSanitize_NestedJson() {
        Map<String, Object> inner = new HashMap<>();
        inner.put("password", "inner-secret");
        inner.put("data", "normal");

        Map<String, Object> data = new HashMap<>();
        data.put("config", inner);
        data.put("name", "test");

        Map<String, Object> result = sanitizer.sanitize(data);

        assertEquals("test", result.get("name"));
        @SuppressWarnings("unchecked")
        Map<String, Object> resultInner = (Map<String, Object>) result.get("config");
        assertEquals("***", resultInner.get("password"));
        assertEquals("normal", resultInner.get("data"));
    }

    @Test
    @DisplayName("非敏感字段保持原样")
    void testSanitize_NonSensitiveFields() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", "u123");
        data.put("amount", 100);
        data.put("description", "test");

        Map<String, Object> result = sanitizer.sanitize(data);

        assertEquals("u123", result.get("userId"));
        assertEquals(100, result.get("amount"));
        assertEquals("test", result.get("description"));
    }

    @Test
    @DisplayName("sanitizeJson — JSON 字符串脱敏")
    void testSanitizeJson() {
        String json = "{\"username\":\"admin\",\"password\":\"secret\",\"data\":{\"token\":\"tok123\"}}";

        String result = sanitizer.sanitizeJson(json);

        assertNotNull(result);
        assertTrue(result.contains("\"***\""));
        assertFalse(result.contains("\"secret\""));
        assertFalse(result.contains("\"tok123\""));
    }

    @Test
    @DisplayName("sanitizeJson — 非法 JSON 返回原字符串")
    void testSanitizeJson_InvalidReturnsOriginal() {
        String invalid = "not a json";

        String result = sanitizer.sanitizeJson(invalid);

        assertEquals(invalid, result);
    }

    @Test
    @DisplayName("输入为 null → 返回 null")
    void testSanitize_NullInput() {
        assertNull(sanitizer.sanitize(null));
    }

    @Test
    @DisplayName("api_key 字段脱敏")
    void testSanitize_ApiKeyField() {
        Map<String, Object> data = new HashMap<>();
        data.put("api_key", "api-key-xyz");

        Map<String, Object> result = sanitizer.sanitize(data);

        assertEquals("***", result.get("api_key"));
    }
}
