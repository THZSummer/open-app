package com.xxx.it.works.wecode.v2.modules.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 日志脱敏器
 *
 * <p>对 input/output 中的敏感字段进行脱敏处理。
 * 脱敏字段：password, token, secretKey, signSecretKey, accessToken, refreshToken,
 * apiKey, credential, authorization</p>
 * <p>脱敏策略：将值替换为 "***"，嵌套 Map 递归处理</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Component
public class LogSanitizer {

    private static final Logger log = LoggerFactory.getLogger(LogSanitizer.class);

    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "password", "passwd", "pwd",
            "token", "accessToken", "access_token", "refreshToken", "refresh_token",
            "secretKey", "secret_key", "signSecretKey", "sign_secret_key",
            "apiKey", "api_key", "apikey",
            "credential", "authorization", "auth",
            "privateKey", "private_key"
    ));

    private final ObjectMapper objectMapper;

    public LogSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 脱敏 Map 数据中的敏感字段
     *
     * @param data 原始数据（可能含敏感字段）
     * @return 脱敏后的数据
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> sanitize(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (isSensitive(key)) {
                sanitized.put(key, "***");
            } else if (value instanceof Map) {
                sanitized.put(key, sanitize((Map<String, Object>) value));
            } else if (value instanceof List) {
                sanitized.put(key, sanitizeList((List<Object>) value));
            } else {
                sanitized.put(key, value);
            }
        }
        return sanitized;
    }

    /**
     * 脱敏 JSON 字符串中的敏感字段
     *
     * @param jsonData JSON 字符串
     * @return 脱敏后的 JSON 字符串，解析失败返回原字符串
     */
    @SuppressWarnings("unchecked")
    public String sanitizeJson(String jsonData) {
        if (jsonData == null || jsonData.isEmpty()) {
            return jsonData;
        }
        try {
            Map<String, Object> data = objectMapper.readValue(jsonData, Map.class);
            Map<String, Object> sanitized = sanitize(data);
            return objectMapper.writeValueAsString(sanitized);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse/sanitize JSON data, returning original: {}", e.getMessage());
            return jsonData;
        }
    }

    /**
     * 判断字段名是否为敏感字段（不区分大小写）
     */
    private boolean isSensitive(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lower = fieldName.toLowerCase();
        for (String sensitive : SENSITIVE_FIELDS) {
            if (lower.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 递归脱敏 List 中的元素
     */
    @SuppressWarnings("unchecked")
    private List<Object> sanitizeList(List<Object> list) {
        if (list == null) {
            return null;
        }
        return list.stream()
                .map(item -> {
                    if (item instanceof Map) {
                        return sanitize((Map<String, Object>) item);
                    } else if (item instanceof List) {
                        return sanitizeList((List<Object>) item);
                    }
                    return item;
                })
                .collect(Collectors.toList());
    }
}
