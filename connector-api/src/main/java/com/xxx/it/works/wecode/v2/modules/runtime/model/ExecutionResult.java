package com.xxx.it.works.wecode.v2.modules.runtime.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 执行结果
 * <p>
 * 连接流单次执行的完整结果.
 * v5.5:
 * <ul>
 *   <li>{@code errorMessage} → {@code errorInfo} 结构化 Map</li>
 *   <li>{@code StepDetail} 中 {@code errorMessage/downstreamStatusCode} → {@code errorInfo} Map</li>
 *   <li>{@code StepDetail} 增加 {@code labelCn}/{@code labelEn}</li>
 * </ul>
 * </p>
 */
public class ExecutionResult {

    /** 执行ID (雪花ID string) */
    private String executionId;

    /** 连接流ID */
    private String flowId;

    /** 总体状态: success/failed/timeout */
    private String status;

    /** 最终结果数据 (出口节点输出) */
    private Map<String, Object> resultData;

    /** 总耗时(毫秒) */
    private long totalDurationMs;

    /** 各步骤详情 (仅 hasSteps=true 时返回) */
    private List<StepDetail> steps;

    /** 结构化错误信息 (整体失败时): {code, messageZh, messageEn, ...} */
    @JsonAlias({"errorInfo", "errorMessage"})
    private Map<String, Object> errorInfo;

    /** 是否测试运行 */
    private boolean isTest;

    /** 缓存命中标记 (FR-037), 默认 false */
    private boolean cacheHit;

    public ExecutionResult() {
        this.steps = new ArrayList<>();
    }

    // ===== Nested Types =====

    /**
     * 单步执行详情
     * <p>
     * v5.5: errorMessage/downstreamStatusCode → errorInfo Map; 增加 labelCn/labelEn.
     * </p>
     */
    public static class StepDetail {
        private String nodeId;
        private String nodeType;

        /** 节点中文标签 */
        @JsonAlias({"labelCn", "nodeLabelCn"})
        private String labelCn;

        /** 节点英文标签 */
        @JsonAlias({"labelEn", "nodeLabelEn"})
        private String labelEn;

        private Map<String, Object> inputData;
        private Map<String, Object> outputData;
        private String status;
        private long durationMs;

        /** 结构化错误信息: {code, messageZh, messageEn, ...} */
        @JsonAlias({"errorInfo", "errorMessage"})
        private Map<String, Object> errorInfo;

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getNodeType() { return nodeType; }
        public void setNodeType(String nodeType) { this.nodeType = nodeType; }

        public String getLabelCn() { return labelCn; }
        public void setLabelCn(String labelCn) { this.labelCn = labelCn; }

        public String getLabelEn() { return labelEn; }
        public void setLabelEn(String labelEn) { this.labelEn = labelEn; }

        public Map<String, Object> getInputData() { return inputData; }
        public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }

        public Map<String, Object> getOutputData() { return outputData; }
        public void setOutputData(Map<String, Object> outputData) { this.outputData = outputData; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

        public Map<String, Object> getErrorInfo() { return errorInfo; }
        public void setErrorInfo(Map<String, Object> errorInfo) { this.errorInfo = errorInfo; }

        // ===== Backward Compat =====

        @Deprecated
        public String getNodeLabelCn() { return labelCn; }
        @Deprecated
        public void setNodeLabelCn(String labelCn) { this.labelCn = labelCn; }

        @Deprecated
        public String getNodeLabelEn() { return labelEn; }
        @Deprecated
        public void setNodeLabelEn(String labelEn) { this.labelEn = labelEn; }

        @Deprecated
        public String getErrorMessage() {
            if (errorInfo != null) {
                Object msg = errorInfo.get("message");
                if (msg instanceof String) { return (String) msg; }
                Object msgEn = errorInfo.get("messageEn");
                if (msgEn instanceof String) { return (String) msgEn; }
            }
            return null;
        }

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

        @Deprecated
        public Integer getDownstreamStatusCode() {
            if (errorInfo != null) {
                Object code = errorInfo.get("downstreamStatusCode");
                if (code instanceof Number) { return ((Number) code).intValue(); }
                if (code instanceof String) {
                    try { return Integer.parseInt((String) code); } catch (NumberFormatException ignored) { return null; }
                }
            }
            return null;
        }

        @Deprecated
        public void setDownstreamStatusCode(Integer downstreamStatusCode) {
            if (downstreamStatusCode != null) {
                if (this.errorInfo == null) {
                    this.errorInfo = new java.util.HashMap<>();
                }
                this.errorInfo.put("downstreamStatusCode", downstreamStatusCode);
            }
        }
    }

    // ===== Getters & Setters =====

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getFlowId() { return flowId; }
    public void setFlowId(String flowId) { this.flowId = flowId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getResultData() { return resultData; }
    public void setResultData(Map<String, Object> resultData) { this.resultData = resultData; }

    public long getTotalDurationMs() { return totalDurationMs; }
    public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }

    public List<StepDetail> getSteps() { return steps; }
    public void setSteps(List<StepDetail> steps) { this.steps = steps; }

    public Map<String, Object> getErrorInfo() { return errorInfo; }
    public void setErrorInfo(Map<String, Object> errorInfo) { this.errorInfo = errorInfo; }

    public boolean isTest() { return isTest; }
    public void setTest(boolean test) { isTest = test; }

    public boolean isCacheHit() { return cacheHit; }
    public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }

    public void addStep(StepDetail step) {
        this.steps.add(step);
    }

    // ===== Backward Compat =====

    @Deprecated
    public String getErrorMessage() {
        if (errorInfo != null) {
            Object msg = errorInfo.get("message");
            if (msg instanceof String) { return (String) msg; }
            Object msgEn = errorInfo.get("messageEn");
            if (msgEn instanceof String) { return (String) msgEn; }
        }
        return null;
    }

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
}