package com.xxx.api.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 签名工具类
 * 
 * <p>提供 AKSK 签名验证和 Token 验证功能</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
public class SignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA256 = "SHA-256";

    /**
     * 验证 AKSK 签名
     * 
     * @param accessKey 访问密钥
     * @param secretKey 密钥
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param signature 签名
     * @param requestBody 请求体
     * @return 是否验证通过
     */
    public static boolean verifyAKSKSignature(
            String accessKey,
            String secretKey,
            String timestamp,
            String nonce,
            String signature,
            String requestBody) {
        
        try {

            // 1. 验证时间戳（防止重放攻击）
            long currentTime = System.currentTimeMillis();
            long requestTime = Long.parseLong(timestamp);
            
            // 时间差超过 5 分钟，拒绝请求
            if (Math.abs(currentTime - requestTime) > 5 * 60 * 1000) {
                log.warn("AKSK signature verification failed: timestamp expired");
                return false;
            }
            
            // 2. 构建签名字符串
            String stringToSign = buildStringToSign(timestamp, nonce, requestBody);
            
            // 3. 计算签名
            String calculatedSignature = calculateSignature(secretKey, stringToSign);
            
            // 4. 比对签名
            boolean valid = calculatedSignature.equals(signature);
            
            if (!valid) {
                log.warn("AKSK signature verification failed: signature mismatch");
            }
            
            return valid;
            
        } catch (Exception e) {
            log.error("AKSK signature verification exception", e);
            return false;
        }
    }

    /**
     * 构建待签名字符串
     */
    private static String buildStringToSign(String timestamp, String nonce, String requestBody) {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp).append("\n");
        sb.append(nonce).append("\n");
        
        if (requestBody != null && !requestBody.isEmpty()) {
            sb.append(sha256Hex(requestBody));
        }
        
        return sb.toString();
    }

    /**
     * 计算签名
     */
    private static String calculateSignature(String secretKey, String stringToSign) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("Failed to calculate signature", e);
            return "";
        }
    }

    /**
     * SHA-256 哈希
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("SHA-256 hash failed", e);
            return "";
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }

    /**
     * 验证 Bearer Token
     * 
     * <p>Mock 实现：简单验证 token 格式</p>
     * <p>实际项目中应调用认证服务验证 token</p>
     * 
     * @param token Bearer Token
     * @return 是否验证通过
     */
    public static boolean verifyBearerToken(String token) {

        // Mock: 简单验证 token 不为空且以 "Bearer " 开头
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        if (!token.startsWith("Bearer ")) {
            return false;
        }
        
        // Mock: 验证通过
        log.debug("Bearer Token verification passed");
        return true;
    }

    /**
     * 解析 Bearer Token
     * 
     * @param authorization Authorization 头
     * @return Token 值
     */
    public static String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    /**
     * 生成随机字符串（Nonce）
     */
    public static String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成时间戳
     */
    public static String generateTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
}
