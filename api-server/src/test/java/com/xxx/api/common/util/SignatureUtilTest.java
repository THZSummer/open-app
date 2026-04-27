package com.xxx.api.common.util;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SignatureUtilTest {

    @Test
    void verifyAKSKSignature_success() {
        String accessKey = "testAccessKey";
        String secretKey = "testSecretKey";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "testNonce123";
        String requestBody = "{\"key\":\"value\"}";
        
        String stringToSign = buildStringToSign(timestamp, nonce, requestBody);
        String signature = calculateSignature(secretKey, stringToSign);
        
        boolean result = SignatureUtil.verifyAKSKSignature(
                accessKey, secretKey, timestamp, nonce, signature, requestBody);
        
        assertTrue(result);
    }

    @Test
    void verifyAKSKSignature_timestampExpired() {
        String accessKey = "testAccessKey";
        String secretKey = "testSecretKey";
        String timestamp = String.valueOf(System.currentTimeMillis() - 6 * 60 * 1000);
        String nonce = "testNonce123";
        String requestBody = "{\"key\":\"value\"}";
        String signature = "anySignature";
        
        boolean result = SignatureUtil.verifyAKSKSignature(
                accessKey, secretKey, timestamp, nonce, signature, requestBody);
        
        assertFalse(result);
    }

    @Test
    void verifyAKSKSignature_signatureMismatch() {
        String accessKey = "testAccessKey";
        String secretKey = "testSecretKey";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "testNonce123";
        String requestBody = "{\"key\":\"value\"}";
        String wrongSignature = "wrongSignature123";
        
        boolean result = SignatureUtil.verifyAKSKSignature(
                accessKey, secretKey, timestamp, nonce, wrongSignature, requestBody);
        
        assertFalse(result);
    }

    @Test
    void verifyAKSKSignature_invalidTimestampFormat() {
        String accessKey = "testAccessKey";
        String secretKey = "testSecretKey";
        String timestamp = "invalid-timestamp";
        String nonce = "testNonce123";
        String requestBody = "{\"key\":\"value\"}";
        String signature = "anySignature";
        
        boolean result = SignatureUtil.verifyAKSKSignature(
                accessKey, secretKey, timestamp, nonce, signature, requestBody);
        
        assertFalse(result);
    }

    @Test
    void verifyAKSKSignature_nullRequestBody() {
        String accessKey = "testAccessKey";
        String secretKey = "testSecretKey";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "testNonce123";
        String requestBody = null;
        
        String stringToSign = buildStringToSign(timestamp, nonce, requestBody);
        String signature = calculateSignature(secretKey, stringToSign);
        
        boolean result = SignatureUtil.verifyAKSKSignature(
                accessKey, secretKey, timestamp, nonce, signature, requestBody);
        
        assertTrue(result);
    }

    @Test
    void verifyBearerToken_success() {
        String token = "Bearer valid-token-123";
        
        boolean result = SignatureUtil.verifyBearerToken(token);
        
        assertTrue(result);
    }

    @Test
    void verifyBearerToken_nullToken() {
        boolean result = SignatureUtil.verifyBearerToken(null);
        
        assertFalse(result);
    }

    @Test
    void verifyBearerToken_invalidFormat() {
        String token = "Basic some-token";
        
        boolean result = SignatureUtil.verifyBearerToken(token);
        
        assertFalse(result);
    }

    @Test
    void extractBearerToken_success() {
        String authorization = "Bearer my-token-value";
        
        String result = SignatureUtil.extractBearerToken(authorization);
        
        assertEquals("my-token-value", result);
    }

    @Test
    void extractBearerToken_nullAuthorization() {
        String result = SignatureUtil.extractBearerToken(null);
        
        assertNull(result);
    }

    @Test
    void extractBearerToken_invalidFormat() {
        String authorization = "Basic some-token";
        
        String result = SignatureUtil.extractBearerToken(authorization);
        
        assertNull(result);
    }

    @Test
    void generateNonce() {
        String nonce = SignatureUtil.generateNonce();
        
        assertNotNull(nonce);
        assertEquals(32, nonce.length());
        assertTrue(nonce.matches("[a-f0-9]{32}"));
    }

    @Test
    void generateTimestamp() {
        String timestamp = SignatureUtil.generateTimestamp();
        
        assertNotNull(timestamp);
        long ts = Long.parseLong(timestamp);
        assertTrue(ts > 0);
        assertTrue(Math.abs(System.currentTimeMillis() - ts) < 1000);
    }

    private String buildStringToSign(String timestamp, String nonce, String requestBody) {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp).append("\n");
        sb.append(nonce).append("\n");
        
        if (requestBody != null && !requestBody.isEmpty()) {
            sb.append(sha256Hex(requestBody));
        }
        
        return sb.toString();
    }

    private String calculateSignature(String secretKey, String stringToSign) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate SHA-256", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
