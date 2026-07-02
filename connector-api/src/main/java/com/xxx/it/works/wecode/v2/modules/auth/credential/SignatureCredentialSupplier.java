package com.xxx.it.works.wecode.v2.modules.auth.credential;

import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数字签名凭据提供 (SIGNATURE)
 * <p>
 * FR-012②: 仅需输入 Secret Key, 算法由平台统一处理 (HMAC-SHA256 + Base64)
 * <p>
 * 签名格式: {@code timestamp.base64(HMAC-SHA256(secretKey, timestamp))}
 * <p>
 * 下游验签: 按 '.' 拆分 → 取 timestamp → 用相同 secretKey 重新计算 HMAC-SHA256 → Base64 比对
 * <p>
 * 密钥来源: {@code authConfig.secretKey.properties.signSecretKey.value} (用户配置常量, 落库加密存储)
 * 签名值注入位置: 由 authConfig.header / authConfig.query 的 properties 中的 value 表达式
 * {@code ${$.system.env.signature}} 标记, 引擎动态计算后填充
 *
 * @author SDDU Build Agent
 * @see plan-json-schema.md §4.3.2 authConfigDef — SIGNATURE 示例
 * @see spec.md FR-012 认证类型 / FR-013 凭证位置
 */
@Component
public class SignatureCredentialSupplier implements CredentialSupplier {

    private static final Logger log = LoggerFactory.getLogger(SignatureCredentialSupplier.class);

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_EXPR = "${$.system.env.signature}";

    @Override
    public String getAuthType() {
        return "SIGNATURE";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> resolve(Map<String, Object> fieldDefs, Map<String, Object> authConfig,
                                        ExecutionContext context) {
        Map<String, String> result = new LinkedHashMap<>();
        String secretKey = extractSecretKey(authConfig);
        if (secretKey == null || secretKey.isEmpty()) {
            log.warn("SIGNATURE auth: secretKey is empty, signature will be blank");
            result.put(SIGNATURE_EXPR, "");
            return result;
        }
        String signature = computeSignature(secretKey);
        result.put(SIGNATURE_EXPR, signature);
        return result;
    }

    /**
     * 从 authConfig.secretKey.properties.signSecretKey.value 读取签名密钥
     */
    @SuppressWarnings("unchecked")
    private String extractSecretKey(Map<String, Object> authConfig) {
        if (authConfig == null) {
            return null;
        }
        Object secretKeyContainer = authConfig.get("secretKey");
        if (!(secretKeyContainer instanceof Map)) {
            return null;
        }
        Map<String, Object> skMap = (Map<String, Object>) secretKeyContainer;
        Object properties = skMap.get("properties");
        if (!(properties instanceof Map)) {
            return null;
        }
        Map<String, Object> props = (Map<String, Object>) properties;
        Object signSecretKeyDef = props.get("signSecretKey");
        if (!(signSecretKeyDef instanceof Map)) {
            return null;
        }
        Object value = ((Map<String, Object>) signSecretKeyDef).get("value");
        return value != null ? value.toString() : null;
    }

    /**
     * 计算签名: timestamp.base64(HMAC-SHA256(secretKey, timestamp))
     */
    private String computeSignature(String secretKey) {
        long timestamp = System.currentTimeMillis();
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hmacBytes = mac.doFinal(String.valueOf(timestamp).getBytes(StandardCharsets.UTF_8));
            String hmacBase64 = Base64.getEncoder().encodeToString(hmacBytes);
            return timestamp + "." + hmacBase64;
        } catch (Exception e) {
            log.error("Failed to compute HMAC-SHA256 signature: {}", e.getMessage());
            return "";
        }
    }
}
