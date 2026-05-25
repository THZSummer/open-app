package com.xxx.it.works.wecode.v2.modules.runtime.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 执行结果
 * <p>
 * 连接流单次执行的完整结果
 * 含: status + 各步骤 input/output/duration + 最终 resultData
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

    /** 各步骤详情 */
    private List<StepDetail> steps;

    /** 错误信息 (整体失败时) */
    private String errorMessage;

    /** 是否测试运行 */
    private boolean isTest;

    public ExecutionResult() {
        this.steps = new ArrayList<>();
    }

    // ===== Nested Types =====

    /**
     * 单步执行详情
     */
    public static class StepDetail {
        private String nodeId;
        private String nodeType;
        private String nodeLabelCn;
        private String nodeLabelEn;
        private Map<String, Object> inputData;
        private Map<String, Object> outputData;
        private String status;
        private long durationMs;
        private String errorMessage;
        private Integer downstreamStatusCode;

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getNodeType() { return nodeType; }
        public void setNodeType(String nodeType) { this.nodeType = nodeType; }

        public String getNodeLabelCn() { return nodeLabelCn; }
        public void setNodeLabelCn(String nodeLabelCn) { this.nodeLabelCn = nodeLabelCn; }

        public String getNodeLabelEn() { return nodeLabelEn; }
        public void setNodeLabelEn(String nodeLabelEn) { this.nodeLabelEn = nodeLabelEn; }

        public Map<String, Object> getInputData() { return inputData; }
        public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }

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

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean isTest() { return isTest; }
    public void setTest(boolean test) { isTest = test; }

    public void addStep(StepDetail step) {
        this.steps.add(step);
    }
}