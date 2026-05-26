package com.xxx.it.works.wecode.v2.modules.runtime.expression;

import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;

import java.util.Map;

/**
 * 表达式解析器
 * <p>
 * v5.5: 支持三种模式:
 * <ol>
 *   <li>{@code constant:xxx} — 直接返回字面量 {@code "xxx"}</li>
 *   <li>{@code ${$.node.{nodeId}.{input/output}.{path}}} — 从 NodeContext 的 input 或 output 分区解析</li>
 *   <li>向后兼容: {@code ${nodeId.fieldPath}} — 默认从 output 分区解析</li>
 * </ol>
 * 方法签名: {@code resolve(String expression, Map<String, NodeContext> nodeContexts) -> Object}
 * </p>
 */
public class ExpressionResolver {

    private static final String CONSTANT_PREFIX = "constant:";

    /**
     * 解析表达式并返回结果值
     *
     * @param expression   表达式字符串 (e.g. "constant:hello", "${$.node.n1.output.name}", "${n1.name}")
     * @param nodeContexts 所有已执行节点的上下文 Map (nodeId → NodeContext)
     * @return 解析后的值, 无法解析或表达式为空时返回 null
     */
    @SuppressWarnings("unchecked")
    public Object resolve(String expression, Map<String, NodeContext> nodeContexts) {
        if (expression == null) {
            return null;
        }

        // 1. 常量模式: "constant:xxx"
        if (expression.startsWith(CONSTANT_PREFIX)) {
            return expression.substring(CONSTANT_PREFIX.length());
        }

        // 2. & 3. 路径引用模式: "${...}"
        if (expression.startsWith("${") && expression.endsWith("}")) {
            String ref = expression.substring(2, expression.length() - 1).trim();

            // 2. v5.5 新语法: "$.node.{nodeId}.{input/output}.{path}"
            if (ref.startsWith("$.node.")) {
                return resolveNewSyntax(ref, nodeContexts);
            }

            // 3. 向后兼容: "${nodeId.fieldPath}" -> 默认从 output 分区解析
            return resolveLegacySyntax(ref, nodeContexts);
        }

        // 非表达式, 原样返回
        return expression;
    }

    /**
     * 解析 v5.5 新语法: {@code $.node.{nodeId}.{input/output}.{path}}
     * 示例: $.node.n1.output.name.first
     */
    @SuppressWarnings("unchecked")
    private Object resolveNewSyntax(String ref, Map<String, NodeContext> nodeContexts) {
        // 去掉 "$.node." 前缀
        String remainder = ref.substring("$.node.".length());

        // 分割 nodeId 与剩余路径
        int dotIndex = remainder.indexOf('.');
        if (dotIndex <= 0) {
            return null; // 格式不合法
        }

        String nodeId = remainder.substring(0, dotIndex);
        String pathAfterNodeId = remainder.substring(dotIndex + 1);

        // 剩余路径第一部分必须是 "input" 或 "output"
        int secondDot = pathAfterNodeId.indexOf('.');
        if (secondDot <= 0) {
            return null; // 缺少 fieldPath
        }

        String partition = pathAfterNodeId.substring(0, secondDot);
        String fieldPath = pathAfterNodeId.substring(secondDot + 1);

        NodeContext nodeContext = nodeContexts.get(nodeId);
        if (nodeContext == null) {
            return null; // 节点不存在
        }

        Map<String, Object> data;
        if ("input".equals(partition)) {
            data = nodeContext.getInput();
        } else if ("output".equals(partition)) {
            data = nodeContext.getOutput();
        } else {
            return null; // 非法分区名称
        }

        if (data == null) {
            return null;
        }

        return resolveNestedField(data, fieldPath);
    }

    /**
     * 解析向后兼容语法: {@code nodeId.fieldPath}
     * 默认从 output 分区解析
     */
    @SuppressWarnings("unchecked")
    private Object resolveLegacySyntax(String ref, Map<String, NodeContext> nodeContexts) {
        String[] parts = ref.split("\\.", 2);
        if (parts.length != 2) {
            return null; // 格式不合法 (如 ${nodeId} 无字段名)
        }

        String nodeId = parts[0];
        String fieldPath = parts[1];

        NodeContext nodeContext = nodeContexts.get(nodeId);
        if (nodeContext == null) {
            return null; // 节点不存在
        }

        Map<String, Object> output = nodeContext.getOutput();
        if (output == null) {
            return null;
        }

        return resolveNestedField(output, fieldPath);
    }

    /**
     * 递归解析嵌套字段路径 (如 "user.name.first")
     * 支持 Map 嵌套和 List+index 路径
     */
    @SuppressWarnings("unchecked")
    private Object resolveNestedField(Map<String, Object> data, String fieldPath) {
        String[] parts = fieldPath.split("\\.", 2);
        String key = parts[0];

        Object value = data.get(key);

        if (parts.length == 1) {
            return value;
        }

        if (value instanceof Map) {
            return resolveNestedField((Map<String, Object>) value, parts[1]);
        }

        // 如果路径上有数组索引
        if (value instanceof java.util.List) {
            java.util.List<Object> list = (java.util.List<Object>) value;
            String remainingPath = parts[1];
            // 检查是否以 [index] 开头
            if (remainingPath.startsWith("[")) {
                int endBracket = remainingPath.indexOf(']');
                if (endBracket > 0) {
                    try {
                        int index = Integer.parseInt(remainingPath.substring(1, endBracket));
                        String afterIndex = remainingPath.substring(endBracket + 1);
                        if (index >= 0 && index < list.size()) {
                            Object element = list.get(index);
                            if (afterIndex.startsWith(".") && element instanceof Map) {
                                return resolveNestedField((Map<String, Object>) element, afterIndex.substring(1));
                            }
                            return element;
                        }
                    } catch (NumberFormatException ignored) {
                        // ignore
                    }
                }
            }
        }

        return null;
    }
}