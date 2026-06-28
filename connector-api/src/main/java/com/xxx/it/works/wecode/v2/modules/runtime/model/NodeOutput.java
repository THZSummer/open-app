package com.xxx.it.works.wecode.v2.modules.runtime.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Map;

/**
 * 节点执行输出
 * <p>
 * v5.5: 从单 {@code outputData} 改为 {@code input}/{@code output} 双分区,
 * 配合 JSON Schema v5.5 的 inputContract/outputContract 分离定义.
 * 错误信息由 String {@code errorMessage} 改为结构化 Map {@code errorInfo}.
 * </p>
 */
public class NodeOutput {

    /** 节点ID (对应编排配置中的节点ID) */
    private String nodeId;

    /** 节点类型: entry/connector/data_processor/exit */
    private String nodeType;

    /** 输入数据分区 (来自上游节点或触发数据, 对应 inputContract) */
    @JsonAlias({"inputData", "input"})
    private Map<String, Object> input;

    /** 输出数据分区 (本节点执行结果, 对应 outputContract) */
    @JsonAlias({"outputData", "output"})
    private Map<String, Object> output;

    /** 执行状态: success/failed/timeout */
    private String status;

    /** 耗时(毫秒) */
    private long durationMs;

    /** 结构化错误信息 (status=failed时): {code, messageZh, messageEn, ...} */
    @JsonAlias({"errorInfo", "errorMessage"})
    private Map<String, Object> errorInfo;

    public NodeOutput() {}

    /**
     * 向后兼容构造器: 仅提供 output 分区 (旧 outputData)
     */
    @Deprecated
    public NodeOutput(String nodeId, String nodeType, Map<String, Object> output) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.input = new java.util.HashMap<>();
        this.output = output != null ? output : new java.util.HashMap<>();
        this.status = "success";
    }

    public NodeOutput(String nodeId, String nodeType, Map<String, Object> input, Map<String, Object> output) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.input = input;
        this.output = output;
        this.status = "success";
    }

    // ===== Getters & Setters =====

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }

    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public Map<String, Object> getErrorInfo() { return errorInfo; }
    public void setErrorInfo(Map<String, Object> errorInfo) { this.errorInfo = errorInfo; }

    // ===== Backward Compat Helpers =====

    /** 旧版 getter 兼容 (代理到 output) */
    @Deprecated
    public Map<String, Object> getOutputData() { return output; }

    /** 旧版 setter 兼容 (代理到 output) */
    @Deprecated
    public void setOutputData(Map<String, Object> outputData) { this.output = outputData; }

    /** 旧版 getter 兼容 (从 errorInfo 提取 message) */
    @Deprecated
    public String getErrorMessage() {
        if (errorInfo != null) {
            Object msg = errorInfo.get("message");
            if (msg instanceof String) { return (String) msg; }
            Object msgEn = errorInfo.get("messageEn");
            if (msgEn instanceof String) { return (String) msgEn; }
            Object msgZh = errorInfo.get("messageZh");
            if (msgZh instanceof String) { return (String) msgZh; }
        }
        return null;
    }

    /** 旧版 setter 兼容 (代理到 errorInfo) */
    @Deprecated
    public void setErrorMessage(String errorMessage) {
        if (errorMessage != null) {
            if (this.errorInfo == null) {
                this.errorInfo = new java.util.HashMap<>();
            }
            this.errorInfo.put("message", errorMessage);
            this.errorInfo.put("messageEn", errorMessage);
        }
    }

    /** 是否成功 */
    public boolean isSuccess() {
        return "success".equals(status);
    }

    @Override
    public String toString() {
        return "NodeOutput{" +
                "nodeId='" + nodeId + '\'' +
                ", nodeType='" + nodeType + '\'' +
                ", status='" + status + '\'' +
                ", durationMs=" + durationMs +
                '}';
    }
}