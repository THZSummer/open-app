package com.xxx.it.works.wecode.v2.modules.runtime.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行上下文
 * <p>
 * v5.5:
 * <ul>
 *   <li>{@code nodeOutputs} → {@code nodeContexts} ({@code Map<String, NodeContext>})</li>
 *   <li>移除内联的 {@code resolveFieldReference()} — 移至 {@code ExpressionResolver}</li>
 *   <li>字段引用模式: {@code ${$.node.{nodeId}.{input/output}.{path}}} / {@code ${nodeId.fieldPath}}</li>
 * </ul>
 * 存储单次连接流执行的上下文数据.
 * 支持线程安全的数据读写 (ConcurrentHashMap).
 * credentials 仅内存生命周期, 节点执行后从上下文中清除.
 * </p>
 */
public class ExecutionContext {

    /** 执行ID */
    private final String executionId;

    /** 连接流ID */
    private final String flowId;

    /** 触发数据 (来自HTTP请求体或测试模拟数据) */
    private Map<String, Object> triggerData;

    /** 节点上下文缓存 (nodeId → NodeContext), 替代旧 nodeOutputs */
    private final Map<String, NodeContext> nodeContexts;

    /** 凭证 (仅内存生命周期, 执行后清除) */
    private Map<String, Map<String, String>> credentials;

    /** 是否测试运行 */
    private boolean isTest;

    /** 触发类型: 1=HTTP触发, 2=测试执行, 3=手动触发 */
    private int triggerType;

    public ExecutionContext(String executionId, String flowId) {
        this.executionId = executionId;
        this.flowId = flowId;
        this.nodeContexts = new ConcurrentHashMap<>();
        this.credentials = new ConcurrentHashMap<>();
    }

    /**
     * 获取上游节点的上下文
     */
    public NodeContext getNodeContext(String nodeId) {
        return nodeContexts.get(nodeId);
    }

    /**
     * 存储节点上下文
     */
    public void setNodeContext(NodeContext nodeContext) {
        if (nodeContext != null && nodeContext.getNodeId() != null) {
            nodeContexts.put(nodeContext.getNodeId(), nodeContext);
        }
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

    public Map<String, NodeContext> getNodeContexts() { return nodeContexts; }

    public Map<String, Map<String, String>> getCredentials() { return credentials; }
    public void setCredentials(Map<String, Map<String, String>> credentials) { this.credentials = credentials; }

    public boolean isTest() { return isTest; }
    public void setTest(boolean test) { isTest = test; }

    public int getTriggerType() { return triggerType; }
    public void setTriggerType(int triggerType) { this.triggerType = triggerType; }

    /**
     * 表达式解析器实例 (延迟初始化)
     */
    private com.xxx.it.works.wecode.v2.modules.runtime.expression.ExpressionResolver expressionResolver;

    /**
     * 设置表达式解析器 (由使用方注入)
     */
    public void setExpressionResolver(com.xxx.it.works.wecode.v2.modules.runtime.expression.ExpressionResolver resolver) {
        this.expressionResolver = resolver;
    }

    /**
     * 获取表达式解析器 (延迟创建)
     */
    private com.xxx.it.works.wecode.v2.modules.runtime.expression.ExpressionResolver getExpressionResolver() {
        if (expressionResolver == null) {
            expressionResolver = new com.xxx.it.works.wecode.v2.modules.runtime.expression.ExpressionResolver();
        }
        return expressionResolver;
    }

    // ===== Backward Compat (deprecated) =====

    /**
     * 解析字段引用表达式 (已废弃, 请直接使用 ExpressionResolver)
     * <p>
     * v5.5 兼容: 支持新旧两种格式:
     * <ul>
     *   <li>新格式: {@code ${$.node.{nodeId}.{input/output}.{path}}}</li>
     *   <li>旧格式: {@code ${nodeId.fieldPath}} (默认查 output 分区)</li>
     * </ul>
     * </p>
     */
    @Deprecated
    public Object resolveFieldReference(String expression) {
        return getExpressionResolver().resolve(expression, nodeContexts);
    }

    /**
     * 递归解析嵌套字段路径 (已废弃, 请直接使用 ExpressionResolver)
     */
    @Deprecated
    public Object resolveNestedField(Map<String, Object> data, String fieldPath) {
        if (data == null || fieldPath == null) return null;
        String[] parts = fieldPath.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    /** 旧版: 获取节点输出数据 (已废弃, 请使用 getNodeContext) */
    @Deprecated
    public Map<String, Object> getNodeOutput(String nodeId) {
        NodeContext ctx = nodeContexts.get(nodeId);
        if (ctx != null) {
            return ctx.getOutput();
        }
        return null;
    }

    /** 旧版: 存储节点输出数据 (已废弃, 请使用 setNodeContext) */
    @Deprecated
    public void setNodeOutput(String nodeId, Map<String, Object> outputData) {
        NodeContext ctx = nodeContexts.get(nodeId);
        if (ctx != null) {
            ctx.setOutput(outputData);
        } else {
            NodeContext newCtx = new NodeContext();
            newCtx.setNodeId(nodeId);
            newCtx.setOutput(outputData);
            nodeContexts.put(nodeId, newCtx);
        }
    }

    /** 旧版: 获取 nodeOutputs Map (已废弃) */
    @Deprecated
    public Map<String, Map<String, Object>> getNodeOutputs() {
        Map<String, Map<String, Object>> legacy = new java.util.HashMap<>();
        for (Map.Entry<String, NodeContext> entry : nodeContexts.entrySet()) {
            legacy.put(entry.getKey(), entry.getValue().getOutput());
        }
        return legacy;
    }
}