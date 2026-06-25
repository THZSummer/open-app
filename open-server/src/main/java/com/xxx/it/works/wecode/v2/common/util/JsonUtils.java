package com.xxx.it.works.wecode.v2.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * JSON 工具类
 *
 * <p>封装 Jackson ObjectMapper 的常用操作（序列化 / 反序列化 / 字段提取）。
 * 纯静态工具类，由 {@link com.xxx.it.works.wecode.v2.common.config.JacksonConfig}
 * 在创建 ObjectMapper Bean 时调用 {@link #init(ObjectMapper)} 注入实例。</p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
public final class JsonUtils {

    private static volatile ObjectMapper objectMapper;

    private JsonUtils() {
    }

    /**
     * 注入 Spring 配置好的 ObjectMapper（由 JacksonConfig 启动时调用一次）
     */
    public static void init(ObjectMapper mapper) {
        objectMapper = mapper.copy();
    }

    /**
     * 序列化对象为 JSON 字符串。
     * <ul>
     *   <li>null → null</li>
     *   <li>String 直接返回</li>
     *   <li>其他对象通过 ObjectMapper 序列化，异常时返回 null</li>
     * </ul>
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[JSON] Failed to serialize object: {}", obj.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * 解析 JSON 字符串为 JsonNode。
     * <ul>
     *   <li>null 或空字符串 → null</li>
     *   <li>解析失败 → null（仅 warn 日志）</li>
     * </ul>
     */
    public static JsonNode parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("[JSON] Failed to parse JSON string", e);
            return null;
        }
    }

    /**
     * 将 Java 对象转换为 JsonNode（无需先序列化为字符串）。
     * null → null；转换失败 → null（仅 warn 日志）。
     */
    public static JsonNode toTree(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.valueToTree(obj);
        } catch (Exception e) {
            log.warn("[JSON] Failed to convert object to tree: {}", obj.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * 将 Java 对象转换为 {@code Map<String, Object>}（通过 Jackson convertValue）。
     * <ul>
     *   <li>null → null</li>
     *   <li>转换失败 → null（仅 warn 日志）</li>
     *   <li>返回的是可变的 LinkedHashMap，调用方可继续 put 扩展字段</li>
     * </ul>
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("[JSON] Failed to convert object to Map: {}", obj.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * 从 JsonNode 中安全提取字段文本值。
     * <ul>
     *   <li>null node 或 null fieldName → null</li>
     *   <li>字段不存在或为 null 值 → null</li>
     *   <li>其他 → field.asText()</li>
     * </ul>
     */
    public static String getFieldText(JsonNode node, String fieldName) {
        if (node == null || fieldName == null) {
            return null;
        }
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return field.asText();
    }

    /**
     * 将对象类型的 JsonNode 字段扁平化合并到目标 Map（已存在的 key 不覆盖）。
     * <ul>
     *   <li>node 为 null 或非对象 → 直接返回</li>
     *   <li>仅当 target 中不存在该 key 时才 put</li>
     * </ul>
     */
    public static void flattenToMap(JsonNode node, Map<String, Object> target) {
        if (node == null || !node.isObject()) {
            return;
        }
        node.fields().forEachRemaining(entry -> {
            if (!target.containsKey(entry.getKey())) {
                target.put(entry.getKey(), entry.getValue());
            }
        });
    }
}
