package com.xxx.it.works.wecode.v2.modules.runtime.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行上下文
 * <p>
 * 存储单次连接流执行的上下文数据
 * 支持线程安全的数据读写 (ConcurrentHashMap)
 * credentials 仅内存生命周期, 节点执行后从上下文中清除
 * </p>
 */
public class ExecutionContext {

    /** 执行ID */
    private final String executionId;

    /** 连接流ID */
    private final String flowId;

    /** 触发数据 (来自HTTP请求体或测试模拟数据) */
    private Map<String, Object> triggerData;

    /** 节点输出数据缓存 (nodeId → NodeOutput) */
    private final Map<String, Map<String, Object>> nodeOutputs;

    /** 凭证 (仅内存生命周期, 执行后清除) */
    private Map<String, Map<String, String>> credentials;

    /** 是否测试运行 */
    private boolean isTest;

    /** 触发类型: 1=HTTP触发, 2=测试执行, 3=手动触发 */
    private int triggerType;

    public ExecutionContext(String executionId, String flowId) {
        this.executionId = executionId;
        this.flowId = flowId;
        this.nodeOutputs = new ConcurrentHashMap<>();
        this.credentials = new ConcurrentHashMap<>();
    }

    /**
     * 获取上游节点输出数据
     */
    public Map<String, Object> getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }

    /**
     * 存储节点输出数据
     */
    public void setNodeOutput(String nodeId, Map<String, Object> outputData) {
        nodeOutputs.put(nodeId, outputData);
    }

    /**
     * 从上下文中解析字段值, 支持路径引用: ${nodeId.fieldName}
     */
    @SuppressWarnings("unchecked")
    public Object resolveFieldReference(String expression) {
        if (expression == null) return null;

        // 检查是否是路径引用 ${nodeId.fieldName}
        if (expression.startsWith("${") && expression.endsWith("}")) {
            String ref = expression.substring(2, expression.length() - 1);
            String[] parts = ref.split("\\.", 2);
            if (parts.length == 2) {
                Map<String, Object> nodeOutput = nodeOutputs.get(parts[0]);
                if (nodeOutput != null) {
                    return resolveNestedField(nodeOutput, parts[1]);
                }
                // 引用节点不存在时返回null
                return null;
            }
            // 格式不合法 (如 ${nodeId} 无字段名) 返回null
            return null;
        }

        // 常量值直接返回
        return expression;
    }

    /**
     * 递归解析嵌套字段 (如 "user.name.first")
     */
    @SuppressWarnings("unchecked")
    private Object resolveNestedField(Map<String, Object> data, String fieldPath) {
        String[] parts = fieldPath.split("\\.", 2);
        Object value = data.get(parts[0]);
        if (parts.length == 1) {
            return value;
        }
        if (value instanceof Map) {
            return resolveNestedField((Map<String, Object>) value, parts[1]);
        }
        return null;
    }

    /**
     * 清除凭证 (节点执行后调用)
     */
    public void clearCredentials() {
        this.credentials = null;
    }

    // ===== Getters & Setters =====

    public String getExecutionId() { return executionId; }
    public String getFlowId() { return flowId; }

    public Map<String, Object> getTriggerData() { return triggerData; }
    public void setTriggerData(Map<String, Object> triggerData) { this.triggerData = triggerData; }

    public Map<String, Map<String, String>> getCredentials() { return credentials; }
    public void setCredentials(Map<String, Map<String, String>> credentials) { this.credentials = credentials; }

    public boolean isTest() { return isTest; }
    public void setTest(boolean test) { isTest = test; }

    public int getTriggerType() { return triggerType; }
    public void setTriggerType(int triggerType) { this.triggerType = triggerType; }

    public Map<String, Map<String, Object>> getNodeOutputs() { return nodeOutputs; }
}