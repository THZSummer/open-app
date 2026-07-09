package com.xxx.it.works.wecode.v2.modules.runtime.expression;

import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * v5.6: 支持两种表达式:
 * <ol>
 *   <li>{@code ${$.node.{nodeId}.{input/output}.{path}}} - 从 NodeContext 的 input 或 output 分区解析</li>
 *   <li>{@code ${$.constant:value}} - 常量字面值</li>
 * </ol>
 */
public class ExpressionResolver {

    private static final Logger log = LoggerFactory.getLogger(ExpressionResolver.class);


    /**
     * 解析表达式并返回结果值
     *
     * @param expression   表达式字符串 (e.g. "${$.node.n1.output.name}", "${$.constant:value}")
     * @param nodeContexts 所有已执行节点的上下文 Map (nodeId -> NodeContext)
     * @return 解析后的值, 无法解析或表达式为空时返回 null
     */
    @SuppressWarnings("unchecked")
    public Object resolve(String expression, Map<String, NodeContext> nodeContexts) {
        if (expression == null) {
            return null;
        }


        // 2. & 3. & 4. 路径引用模式: "${...}"
        if (expression.startsWith("${") && expression.endsWith("}")) {
            String ref = expression.substring(2, expression.length() - 1).trim();

            // 2. v5.6 常量: "${$.constant:xxx}"
            if (ref.startsWith("$.constant:")) {
                return ref.substring("$.constant:".length());
            }

            // 3. v5.5 新语法: "$.node.{nodeId}.{input/output}.{path}"
            if (ref.startsWith("$.node.")) {
                return resolveNewSyntax(expression, ref, nodeContexts);
            }

            // 4. 向后兼容: "${nodeId.fieldPath}" -> 默认从 output 分区解析
            log.warn("Expression '{}' has unknown format, expected $.node.* or $.constant:*, returning null", expression);
            return null;
        }

        // 非表达式, 原样返回
        return expression;
    }

    /**
     * 解析 v5.5 新语法: {@code $.node.{nodeId}.{input/output}.{path}}
     * 示例: $.node.n1.output.name.first
     */
    @SuppressWarnings("unchecked")
    private Object resolveNewSyntax(String expression, String ref, Map<String, NodeContext> nodeContexts) {
        // 去掉 "$.node." 前缀
        String remainder = ref.substring("$.node.".length());

        // 分割 nodeId 与剩余路径
        int dotIndex = remainder.indexOf('.');
        if (dotIndex <= 0) {
            log.warn("Expression '{}': invalid node reference, missing '.' after nodeId in '{}', returning null",
                    expression, remainder);
            return null; // 格式不合法
        }

        String nodeId = remainder.substring(0, dotIndex);
        String pathAfterNodeId = remainder.substring(dotIndex + 1);

        // 剩余路径第一部分必须是 "input" 或 "output"
        int secondDot = pathAfterNodeId.indexOf('.');
        if (secondDot <= 0) {
            log.warn("Expression '{}': missing field path after partition '{}', returning null",
                    expression, pathAfterNodeId);
            return null; // 缺少 fieldPath
        }

        String partition = pathAfterNodeId.substring(0, secondDot);
        String fieldPath = pathAfterNodeId.substring(secondDot + 1);

        NodeContext nodeContext = nodeContexts.get(nodeId);
        if (nodeContext == null) {
            log.warn("Expression '{}': node '{}' not found in execution context, available nodes: {}, returning null",
                    expression, nodeId, nodeContexts.keySet());
            return null; // 节点不存在
        }

        Map<String, Object> data;
        if ("input".equals(partition)) {
            data = nodeContext.getInput();
        } else if ("output".equals(partition)) {
            data = nodeContext.getOutput();
        } else {
            log.warn("Expression '{}': invalid partition '{}', expected 'input' or 'output', returning null",
                    expression, partition);
            return null; // 非法分区名称
        }

        if (data == null) {
            log.warn("Expression '{}': {} partition of node '{}' is null, returning null",
                    expression, partition, nodeId);
            return null;
        }

        return resolveNestedField(expression, data, fieldPath);
    }


    /**
     * 递归解析嵌套字段路径 (如 "user.name.first")
     * 支持 Map 嵌套和 List+index 路径
     */
    @SuppressWarnings("unchecked")
    private Object resolveNestedField(String expression, Map<String, Object> data, String fieldPath) {
        String[] parts = fieldPath.split("\\.", 2);
        String key = parts[0];

        Object value = data.get(key);

        if (parts.length == 1) {
            if (value == null && !data.containsKey(key)) {
                log.warn("Expression '{}': field '{}' not found, available keys: {}, returning null",
                        expression, key, data.keySet());
            }
            return value;
        }

        if (value instanceof Map) {
            return resolveNestedField(expression, (Map<String, Object>) value, parts[1]);
        }

        // 如果路径上有数组索引
        if (value instanceof java.util.List) {
            return resolveListElement(expression, (java.util.List<Object>) value, parts[1]);
        }

        if (value != null) {
            log.warn("Expression '{}': cannot traverse path '{}' through value of type {} (field '{}'), returning null",
                    expression, parts[1], value.getClass().getSimpleName(), key);
        } else {
            log.warn("Expression '{}': field '{}' is null, cannot traverse path '{}', returning null",
                    expression, key, parts[1]);
        }
        return null;
    }

    /**
     * 解析 List 元素索引 (如 "items[0].name")
     */
    @SuppressWarnings("unchecked")
    private Object resolveListElement(String expression, java.util.List<Object> list, String remainingPath) {
        if (!remainingPath.startsWith("[")) {
            log.warn("Expression '{}': list index expected '[' but got '{}', returning null",
                    expression, remainingPath);
            return null;
        }
        int endBracket = remainingPath.indexOf(']');
        if (endBracket <= 0) {
            log.warn("Expression '{}': list index missing ']' in '{}', returning null",
                    expression, remainingPath);
            return null;
        }
        try {
            int index = Integer.parseInt(remainingPath.substring(1, endBracket));
            String afterIndex = remainingPath.substring(endBracket + 1);
            if (index < 0 || index >= list.size()) {
                log.warn("Expression '{}': list index {} out of bounds (list size: {}), returning null",
                        expression, index, list.size());
                return null;
            }
            Object element = list.get(index);
            if (afterIndex.startsWith(".") && element instanceof Map) {
                return resolveNestedField(expression, (Map<String, Object>) element, afterIndex.substring(1));
            }
            return element;
        } catch (NumberFormatException e) {
            log.warn("Expression '{}': invalid list index format in '{}', returning null",
                    expression, remainingPath);
            return null;
        }
    }
}
