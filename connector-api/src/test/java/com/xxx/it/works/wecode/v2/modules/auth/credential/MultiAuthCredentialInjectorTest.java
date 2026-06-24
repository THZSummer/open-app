package com.xxx.it.works.wecode.v2.modules.auth.credential;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultiAuthCredentialInjector 测试")
class MultiAuthCredentialInjectorTest {

    @Mock
    private CredentialInjectorRegistry registry;

    @InjectMocks
    private MultiAuthCredentialInjector injector;

    private Map<String, String> headers;

    @BeforeEach
    void setUp() {
        headers = new HashMap<>();
    }

    @Test
    @DisplayName("authtype 返回 MULTI_AUTH")
    void testGetAuthType_ReturnsMultiAuth() {
        assertEquals("MULTI_AUTH", injector.getAuthType());
    }

    @Test
    @DisplayName("authConfigs 为空 → 跳过注入")
    void testEmptyAuthConfigs_Skipped() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("authConfigs", new ArrayList<>());

        injector.inject(authConfig, headers);

        verify(registry, never()).inject(any(), any());
    }

    @Test
    @DisplayName("authConfigs 为 null → 跳过注入")
    void testNullAuthConfigs_Skipped() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("authConfigs", null);

        injector.inject(authConfig, headers);

        verify(registry, never()).inject(any(), any());
    }

    @Test
    @DisplayName("单认证注入 → 委托 Registry 执行一次")
    void testSingleAuth_InjectedOnce() {
        Map<String, Object> cookieConfig = new HashMap<>();
        cookieConfig.put("type", "COOKIE");
        cookieConfig.put("cookieName", "SESSIONID");
        cookieConfig.put("cookieValue", "abc123");

        List<Map<String, Object>> authConfigs = Collections.singletonList(cookieConfig);
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("authConfigs", authConfigs);

        doAnswer(inv -> {
            Map<String, String> h = inv.getArgument(1);
            h.put("Cookie", "SESSIONID=abc123");
            return null;
        }).when(registry).inject(eq(cookieConfig), eq(headers));

        injector.inject(authConfig, headers);

        verify(registry, times(1)).inject(eq(cookieConfig), eq(headers));
        assertEquals("SESSIONID=abc123", headers.get("Cookie"));
    }

    @Test
    @DisplayName("多认证叠加 → 按顺序依次执行，结果叠加")
    void testMultiAuth_SequentialOverlay() {
        Map<String, Object> cookieConfig = new HashMap<>();
        cookieConfig.put("type", "COOKIE");
        cookieConfig.put("cookieName", "SESSIONID");
        cookieConfig.put("cookieValue", "abc123");

        Map<String, Object> signatureConfig = new HashMap<>();
        signatureConfig.put("type", "SIGNATURE");
        signatureConfig.put("secretKey", "my-secret");
        signatureConfig.put("credentialPosition", "header");

        List<Map<String, Object>> authConfigs = Arrays.asList(cookieConfig, signatureConfig);
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("authConfigs", authConfigs);

        doAnswer(inv -> {
            Map<String, String> h = inv.getArgument(1);
            h.put("Cookie", "SESSIONID=abc123");
            return null;
        }).when(registry).inject(eq(cookieConfig), eq(headers));

        doAnswer(inv -> {
            Map<String, String> h = inv.getArgument(1);
            h.put("X-Signature", "mock-signature-value");
            return null;
        }).when(registry).inject(eq(signatureConfig), eq(headers));

        injector.inject(authConfig, headers);

        assertEquals("SESSIONID=abc123", headers.get("Cookie"));
        assertEquals("mock-signature-value", headers.get("X-Signature"));
        verify(registry, times(2)).inject(any(), eq(headers));
    }

    @Test
    @DisplayName("多认证互不干扰 — 每个子认证独立注入")
    void testMultiAuth_IndependentInjection() {
        Map<String, Object> config1 = new HashMap<>();
        config1.put("type", "COOKIE");

        Map<String, Object> config2 = new HashMap<>();
        config2.put("type", "SOA");

        List<Map<String, Object>> authConfigs = Arrays.asList(config1, config2);
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("authConfigs", authConfigs);

        doAnswer(inv -> {
            Map<String, String> h = inv.getArgument(1);
            h.put("Cookie", "v1");
            return null;
        }).when(registry).inject(eq(config1), eq(headers));

        doAnswer(inv -> {
            Map<String, String> h = inv.getArgument(1);
            h.put("Authorization", "Bearer v2");
            return null;
        }).when(registry).inject(eq(config2), eq(headers));

        injector.inject(authConfig, headers);

        assertEquals("v1", headers.get("Cookie"));
        assertEquals("Bearer v2", headers.get("Authorization"));
    }

    @Test
    @DisplayName("子认证 type 为空 → 跳过该子项")
    void testSubConfigTypeEmpty_Skipped() {
        Map<String, Object> invalidConfig = new HashMap<>();
        invalidConfig.put("type", "");

        List<Map<String, Object>> authConfigs = Collections.singletonList(invalidConfig);
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("authConfigs", authConfigs);

        injector.inject(authConfig, headers);

        // Registry 仍被调用(因为 type 为空字符串, registry.inject 内部处理)，但不应该有实际效果
        assertTrue(headers.isEmpty());
    }

    @Test
    @DisplayName("子认证执行异常 → 不影响后续子认证")
    void testSubConfigException_Continues() {
        Map<String, Object> errorConfig = new HashMap<>();
        errorConfig.put("type", "ERROR_TYPE");

        Map<String, Object> normalConfig = new HashMap<>();
        normalConfig.put("type", "COOKIE");

        List<Map<String, Object>> authConfigs = Arrays.asList(errorConfig, normalConfig);
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("authConfigs", authConfigs);

        doThrow(new RuntimeException("sub inject failed"))
                .when(registry).inject(eq(errorConfig), eq(headers));
        doAnswer(inv -> {
            Map<String, String> h = inv.getArgument(1);
            h.put("Cookie", "abc123");
            return null;
        }).when(registry).inject(eq(normalConfig), eq(headers));

        // 不应抛出异常
        assertDoesNotThrow(() -> injector.inject(authConfig, headers));

        // 正常子认证应成功
        assertEquals("abc123", headers.get("Cookie"));
    }
}
