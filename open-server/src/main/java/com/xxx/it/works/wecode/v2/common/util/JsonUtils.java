package com.xxx.it.works.wecode.v2.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
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
     * 从 JsonNode 中安全提取 Long 字段值。
     * <ul>
     *   <li>null node 或 null fieldName → null</li>
     *   <li>字段不存在或为 null 值 → null</li>
     *   <li>其他 → asLong()，转换失败返回 null</li>
     * </ul>
     */
    public static Long getFieldLong(JsonNode node, String fieldName) {
        if (node == null || fieldName == null) {
            return null;
        }
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        try {
            return field.asLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过 Jackson convertValue 收集对象的顶层简单值字段（String/Number/Boolean）。
     * 内部走 getter 方法读取，不使用反射 setAccessible。
     * List/Map/嵌套对象序列化后自动过滤。
     */
    public static Map<String, String> extractSimpleProperties(Object obj) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            Map<String, Object> map = objectMapper.convertValue(obj,
                    new TypeReference<>() {
                    });
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object v = entry.getValue();
                if (v instanceof CharSequence || v instanceof Number || v instanceof Boolean) {
                    result.put(entry.getKey(), v.toString());
                }
            }
        } catch (Exception e) {
            log.debug("[JsonUtils] Failed to extract simple properties", e);
        }
        return result;
    }

    /**
     * 将参数名和参数值展平为 JsonNode。
     * 简单类型按参数名直接放入，复杂对象提取顶层简单值字段。
     * 用于审计日志模板渲染的降级数据源。
     *
     * @param paramNames 参数名数组
     * @param args       参数值数组（与 paramNames 一一对应）
     * @return JsonNode，无数据时返回 null
     */
    public static JsonNode toFlatNode(String[] paramNames, Object[] args) {
        Map<String, String> flat = buildFlatMap(paramNames, args);
        return flat.isEmpty() ? null : objectMapper.valueToTree(flat);
    }

    private static Map<String, String> buildFlatMap(String[] paramNames, Object[] args) {
        Map<String, String> flat = new LinkedHashMap<>();
        int len = Math.min(paramNames.length, args.length);
        for (int i = 0; i < len; i++) {
            if (args[i] == null) {
                continue;
            }
            if (args[i] instanceof CharSequence || args[i] instanceof Number || args[i] instanceof Boolean) {
                flat.put(paramNames[i], args[i].toString());
            } else {
                flat.putAll(extractSimpleProperties(args[i]));
            }
        }
        return flat;
    }
}
