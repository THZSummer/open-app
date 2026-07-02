package com.xxx.it.works.wecode.v2.modules.runtime;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * 节点业务类型解析工具
 * <p>
 * v3 规范 (plan-json-schema.md §1.3 原则四「框业分离」):
 * <ul>
 *   <li>{@code node.type} — React Flow 框架渲染字段 (snake_case), 仅用于画布组件注册</li>
 *   <li>{@code node.data.type} — 业务节点类型 (camelCase), 引擎执行路由依据</li>
 * </ul>
 * 引擎应从 {@code node.data.type} 读取业务类型, 缺失时回退 {@code node.type} (向后兼容)。
 * </p>
 *
 * @author SDDU Build Agent
 */
public final class NodeTypeResolver {

    private NodeTypeResolver() {
    }

    /**
     * 从 JsonNode 节点解析业务类型
     *
     * @param node 节点 JSON (含 id/type/position/data)
     * @return 业务节点类型, 缺失时返回 null
     */
    public static String businessType(JsonNode node) {
        if (node == null) {
            return null;
        }
        JsonNode data = node.get("data");
        if (data != null && data.has("type") && !data.get("type").isNull()) {
            return data.get("type").asText();
        }
        JsonNode type = node.get("type");
        return type != null && !type.isNull() ? type.asText() : null;
    }

    /**
     * 从 Map 节点解析业务类型
     *
     * @param node 节点 Map (含 id/type/position/data)
     * @return 业务节点类型, 缺失时返回 null
     */
    @SuppressWarnings("unchecked")
    public static String businessType(Map<String, Object> node) {
        if (node == null) {
            return null;
        }
        Object data = node.get("data");
        if (data instanceof Map) {
            Object type = ((Map<String, Object>) data).get("type");
            if (type instanceof String) {
                return (String) type;
            }
        }
        Object type = node.get("type");
        return type instanceof String ? (String) type : null;
    }
}
