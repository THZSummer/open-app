package com.xxx.it.works.wecode.v2.modules.runtime.model;

import java.util.Map;

/**
 * 节点执行输出
 * <p>
 * 每个节点执行完成后返回的输出数据
 * </p>
 */
public class NodeOutput {

    /** 节点ID (对应编排配置中的节点ID) */
    private String nodeId;

    /** 节点类型: entry/connector/data_processor/exit */
    private String nodeType;

    /** 输出数据 (字段名→值) */
    private Map<String, Object> outputData;

    /** 执行状态: success/failed/timeout */
    private String status;

    /** 耗时(毫秒) */
    private long durationMs;

    /** 错误信息 (status=failed时) */
    private String errorMessage;

    /** 下游HTTP状态码 (connector节点专属) */
    private Integer downstreamStatusCode;

    public NodeOutput() {}

    public NodeOutput(String nodeId, String nodeType, Map<String, Object> outputData) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.outputData = outputData;
        this.status = "success";
    }

    // ===== Getters & Setters =====

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public Map<String, Object> getOutputData() { return outputData; }
    public void setOutputData(Map<String, Object> outputData) { this.outputData = outputData; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getDownstreamStatusCode() { return downstreamStatusCode; }
    public void setDownstreamStatusCode(Integer downstreamStatusCode) { this.downstreamStatusCode = downstreamStatusCode; }

    /** 是否成功 */
    public boolean isSuccess() {
        return "success".equals(status);
    }
}