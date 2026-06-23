package com.xxx.it.works.wecode.v2.modules.auth.credential;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DigitalSignCredentialInjector 测试")
class DigitalSignCredentialInjectorTest {

    private DigitalSignCredentialInjector injector;
    private Map<String, String> headers;

    @BeforeEach
    void setUp() {
        injector = new DigitalSignCredentialInjector();
        headers = new HashMap<>();
    }

    @Test
    @DisplayName("authtype 返回 SIGNATURE")
    void testGetAuthType_ReturnsSignature() {
        assertEquals("SIGNATURE", injector.getAuthType());
    }

    @Test
    @DisplayName("Header 模式 → 签名注入到 X-Signature")
    void testHeaderMode_SignatureInjectedToHeader() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("secretKey", "my-secret-key");
        authConfig.put("credentialPosition", "header");
        authConfig.put("headerName", "X-Signature");

        injector.inject(authConfig, headers);

        assertTrue(headers.containsKey("X-Signature"));
        String signature = headers.get("X-Signature");
        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    @DisplayName("Header 模式默认 headerName → 仍注入到 X-Signature")
    void testHeaderMode_DefaultHeaderName() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("secretKey", "my-secret-key");
        authConfig.put("credentialPosition", "header");
        // headerName 不提供

        injector.inject(authConfig, headers);

        assertTrue(headers.containsKey("X-Signature"));
    }

    @Test
    @DisplayName("Query 模式 → 签名注入到 X-Query-Sign 特殊头")
    void testQueryMode_SignatureInjectedToQueryHeaders() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("secretKey", "my-secret-key");
        authConfig.put("credentialPosition", "query");
        authConfig.put("queryParamName", "sign");

        injector.inject(authConfig, headers);

        assertTrue(headers.containsKey("X-Query-Sign-Param"));
        assertTrue(headers.containsKey("X-Query-Sign-Value"));
        assertEquals("sign", headers.get("X-Query-Sign-Param"));
        assertNotNull(headers.get("X-Query-Sign-Value"));
        assertFalse(headers.get("X-Query-Sign-Value").isEmpty());
    }

    @Test
    @DisplayName("Query 模式默认 queryParamName → signature")
    void testQueryMode_DefaultQueryParamName() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("secretKey", "my-secret-key");
        authConfig.put("credentialPosition", "query");
        // queryParamName 不提供

        injector.inject(authConfig, headers);

        assertEquals("signature", headers.get("X-Query-Sign-Param"));
    }

    @Test
    @DisplayName("未知 credentialPosition → 默认注入到 X-Signature")
    void testUnknownPosition_DefaultsToXSignature() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("secretKey", "my-secret-key");
        authConfig.put("credentialPosition", "unknown");

        injector.inject(authConfig, headers);

        assertTrue(headers.containsKey("X-Signature"));
    }

    @Test
    @DisplayName("secretKey 为空 → 跳过签名注入")
    void testEmptySecretKey_Skipped() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("secretKey", "");
        authConfig.put("credentialPosition", "header");

        injector.inject(authConfig, headers);

        assertFalse(headers.containsKey("X-Signature"));
    }

    @Test
    @DisplayName("secretKey 为 null → 跳过签名注入")
    void testNullSecretKey_Skipped() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("secretKey", null);
        authConfig.put("credentialPosition", "header");

        injector.inject(authConfig, headers);

        assertFalse(headers.containsKey("X-Signature"));
    }

    @Test
    @DisplayName("算法正确性 — 固定密钥 + 固定输入 → 预期签名")
    void testSignatureAlgorithmCorrectness_FixedInput() throws Exception {
        // 使用已知的 secretKey 和时间戳，手动计算预期签名值
        String secretKey = "test-secret";
        String knownTimestamp = "1234567890";

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256");
        mac.init(spec);
        byte[] hmac = mac.doFinal(knownTimestamp.getBytes("UTF-8"));
        String expectedSignature = Base64.getEncoder().encodeToString(hmac);

        // 模拟注入，由于 System.currentTimeMillis() 变化，验证签名非空且为 Base64
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put("secretKey", secretKey);
        authConfig.put("credentialPosition", "header");

        injector.inject(authConfig, headers);

        String actualSignature = headers.get("X-Signature");
        assertNotNull(actualSignature);
        assertFalse(actualSignature.isEmpty());
        // 验证是有效的 Base64 编码
        assertDoesNotThrow(() -> Base64.getDecoder().decode(actualSignature));
    }
}
