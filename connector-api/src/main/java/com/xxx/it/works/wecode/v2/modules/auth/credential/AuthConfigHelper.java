package com.xxx.it.works.wecode.v2.modules.auth.credential;

import java.util.List;
import java.util.Map;

/**
 * AuthConfig 解析工具 — v2 header/query jsonObjectDef 格式
 *
 * <p>plan-json-schema.md §4.3.2 authConfigDef v2：认证字段不再使用 fields[] 自定结构，
 * 改为 header / query / secretKey 内嵌 jsonObjectDef（字段名直作 properties key，
 * value/sensitive/required 等属性合并在字段定义内）。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public final class AuthConfigHelper {

    private AuthConfigHelper() {
        // 工具类
    }

    /**
     * 从 jsonObjectDef 格式的容器中提取字段名和值，注入到 headers Map。
     *
     * <p>容器结构：{@code { type: "object", properties: { fieldName: { type, value, ... } } }}</p>
     * <p>字段名作为 HTTP header 名，value 作为 header 值。value 缺失时注入空字符串，保证 header key 存在。</p>
     *
     * @param container jsonObjectDef 格式的容器（header / query）
     * @param headers   目标 headers Map，key=fieldName, value=fieldDef.value
     */
    @SuppressWarnings("unchecked")
    public static void injectFields(Map<String, Object> container, Map<String, String> headers) {
        if (container == null || headers == null) {
            return;
        }
        Map<String, Object> properties = (Map<String, Object>) container.get("properties");
        if (properties == null || properties.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String fieldName = entry.getKey();
            Map<String, Object> fieldDef = (Map<String, Object>) entry.getValue();
            String value = "";
            if (fieldDef != null && fieldDef.get("value") instanceof String) {
                value = (String) fieldDef.get("value");
            }
            headers.put(fieldName, value);
        }
    }

    /**
     * 从 jsonObjectDef 格式的容器中提取字段名和值，注入到 params Map（用于 query 参数）。
     *
     * @param container jsonObjectDef 格式的容器
     * @param params    目标 query params Map
     */
    @SuppressWarnings("unchecked")
    public static void injectQueryParams(Map<String, Object> container, Map<String, Object> params) {
        if (container == null || params == null) {
            return;
        }
        Map<String, Object> properties = (Map<String, Object>) container.get("properties");
        if (properties == null || properties.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String fieldName = entry.getKey();
            Map<String, Object> fieldDef = (Map<String, Object>) entry.getValue();
            Object value = "";
            if (fieldDef != null && fieldDef.containsKey("value")) {
                value = fieldDef.get("value");
            }
            params.put(fieldName, value);
        }
    }

    /**
     * 从 jsonObjectDef 容器中提取第一个字段的 value。
     *
     * <p>用于只需要单个值的场景（如 secretKey 提取签名密钥）。</p>
     *
     * @param container jsonObjectDef 格式的容器
     * @return 第一个字段的 value，或 null
     */
    @SuppressWarnings("unchecked")
    public static String extractFirstValue(Map<String, Object> container) {
        if (container == null) {
            return null;
        }
        Map<String, Object> properties = (Map<String, Object>) container.get("properties");
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        Map<String, Object> firstField = (Map<String, Object>) properties.values().iterator().next();
        if (firstField != null && firstField.get("value") instanceof String) {
            return (String) firstField.get("value");
        }
        return null;
    }

    /**
     * 从 authConfig 的 header properties 中查找指定 fieldName 的 fieldDef。
     *
     * @param authConfig 认证配置（含 header jsonObjectDef）
     * @param fieldName  要查找的字段名
     * @return fieldDef Map，或 null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> findFieldDef(Map<String, Object> authConfig, String fieldName) {
        if (authConfig == null || fieldName == null) {
            return null;
        }
        Map<String, Object> header = (Map<String, Object>) authConfig.get("header");
        if (header == null) {
            return null;
        }
        Map<String, Object> properties = (Map<String, Object>) header.get("properties");
        if (properties == null) {
            return null;
        }
        return (Map<String, Object>) properties.get(fieldName);
    }
}
