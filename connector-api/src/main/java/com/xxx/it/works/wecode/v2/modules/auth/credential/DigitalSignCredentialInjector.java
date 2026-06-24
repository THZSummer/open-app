package com.xxx.it.works.wecode.v2.modules.auth.credential;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 数字签名认证凭据注入器
 *
 * <p>使用 HMAC-SHA256 算法生成数字签名，注入到请求头或查询参数中</p>
 * <p>MVP 阶段：签名内容为当前时间戳（秒级），避免依赖请求体</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see spec.md §3.3 连接器配置认证类型
 */
@Slf4j
@Component
public class DigitalSignCredentialInjector implements CredentialInjector {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Override
    public String getAuthType() {
        return "SIGNATURE";
    }

    @Override
    public void inject(Map<String, Object> authConfig, Map<String, String> headers) {
        // 从认证配置中读取签名相关参数
        String secretKey = (String) authConfig.get("secretKey");
        String credentialPosition = (String) authConfig.get("credentialPosition");
        String headerName = (String) authConfig.get("headerName");
        String queryParamName = (String) authConfig.get("queryParamName");

        // 校验 secretKey 必须存在
        if (secretKey == null || secretKey.isEmpty()) {
            log.warn("Digital signature injection skipped: secretKey is empty");
            return;
        }

        // 计算签名：base64(hmacSha256(secretKey, 当前时间戳/1000))
        // MVP 阶段使用时间戳作为签名内容，后续可扩展为请求体签名
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = computeHmacSha256(secretKey, timestamp);

        // 根据凭据位置注入签名
        if ("header".equals(credentialPosition)) {
            // 注入到请求头
            String targetHeader = (headerName != null && !headerName.isEmpty()) ? headerName : "X-Signature";
            headers.put(targetHeader, signature);
            log.info("Digital signature injected, position=header, headerName={}", targetHeader);
        } else if ("query".equals(credentialPosition)) {
            // 查询参数模式：通过特殊请求头透传，由下游 WebClient 过滤器拼接到 URL
            String targetParam = (queryParamName != null && !queryParamName.isEmpty()) ? queryParamName : "signature";
            headers.put("X-Query-Sign-Param", targetParam);
            headers.put("X-Query-Sign-Value", signature);
            log.info("Digital signature injected, position=query, queryParamName={}", targetParam);
        } else {
            // 未指定位置或未知位置，默认注入到请求头 X-Signature
            headers.put("X-Signature", signature);
            log.info("Digital signature injected, position=header (default), headerName=X-Signature");
        }
    }

    /**
     * 计算 HMAC-SHA256 签名并进行 Base64 编码
     *
     * @param secretKey 密钥
     * @param data      待签名数据
     * @return Base64 编码的签名字符串
     */
    private String computeHmacSha256(String secretKey, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            log.error("Failed to compute HMAC-SHA256 signature", e);
            return "";
        }
    }
}
