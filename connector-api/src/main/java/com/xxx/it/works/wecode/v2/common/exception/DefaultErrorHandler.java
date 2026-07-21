package com.xxx.it.works.wecode.v2.common.exception;

import com.xxx.it.works.wecode.v2.common.error.ErrorCode;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认错误处理器 (connector-api)
 * <p>
 * FR-023: 单节点失败标记 failed, 连接流整体标记 failed, 失败上下文保留
 * 提供平台默认错误处理行为
 * </p>
 * <p>
 * v5.5: 错误信息使用结构化 {@code errorInfo} Map 格式:
 * <ul>
 *   <li>{@code code}: 数字字符串 (如 "60000")</li>
 *   <li>{@code messageZh}/{@code messageEn}: 替代旧 {@code message}</li>
 *   <li>内部错误 (6xxxx): 额外 {@code cause} 字段 (异常消息)</li>
 *   <li>下游错误 (4xx/5xx): 额外 {@code downstreamStatus} (int) + {@code downstreamBody} (String, 截断至 512 字符)</li>
 * </ul>
 * </p>
 */
@RestControllerAdvice(basePackages = "com.xxx.it.works.wecode.v2")
public class DefaultErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultErrorHandler.class);

    /** 下游响应体截断最大长度 */
    private static final int DOWNSTREAM_BODY_MAX_LENGTH = 512;

    // ===== Error Info 工厂方法 =====

    /**
     * 构建内部错误 errorInfo (code 6xxxx, 携带 cause)
     *
     * @param code      数字错误码字符串 (如 "60000")
     * @param messageZh 中文错误消息
     * @param messageEn 英文错误消息
     * @param cause     异常原因消息
     * @return 结构化 errorInfo Map
     */
    public static Map<String, Object> buildInternalErrorInfo(String code, String messageZh, String messageEn, String cause) {
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("code", code);
        errorInfo.put("messageZh", messageZh);
        errorInfo.put("messageEn", messageEn);
        if (cause != null) {
            errorInfo.put("cause", cause);
        }
        return errorInfo;
    }

    /**
     * 构建下游错误 errorInfo (4xx/5xx, 携带 downstreamStatus + downstreamBody)
     *
     * @param code             数字错误码字符串 (如 "502")
     * @param messageZh        中文错误消息
     * @param messageEn        英文错误消息
     * @param downstreamStatus 下游 HTTP 状态码
     * @param downstreamBody   下游响应体 (自动截断至 512 字符)
     * @return 结构化 errorInfo Map
     */
    public static Map<String, Object> buildDownstreamErrorInfo(String code, String messageZh, String messageEn,
                                                                int downstreamStatus, String downstreamBody) {
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("code", code);
        errorInfo.put("messageZh", messageZh);
        errorInfo.put("messageEn", messageEn);
        errorInfo.put("downstreamStatus", downstreamStatus);
        if (downstreamBody != null) {
            errorInfo.put("downstreamBody",
                    downstreamBody.length() > DOWNSTREAM_BODY_MAX_LENGTH
                            ? downstreamBody.substring(0, DOWNSTREAM_BODY_MAX_LENGTH)
                            : downstreamBody);
        }
        return errorInfo;
    }

    /**
     * 构建通用错误 errorInfo (无额外字段)
     */
    public static Map<String, Object> buildErrorInfo(String code, String messageZh, String messageEn) {
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("code", code);
        errorInfo.put("messageZh", messageZh);
        errorInfo.put("messageEn", messageEn);
        return errorInfo;
    }

    // ===== REST Controller Advice =====

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Map<String, Object>> handleException(Exception e) {
        log.error("Unhandled exception: {}", e.getMessage(), e);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "failed");
        result.put("errorInfo", buildInternalErrorInfo(ErrorCode.ORCH_EXECUTION_FAILED, "系统内部错误", "Internal server error", e.getMessage()));
        return Mono.just(result);
    }

    // ===== 节点失败工厂方法 =====

    /**
     * 创建内部错误导致的失败节点输出
     *
     * @param cause 异常原因消息
     */
    public static NodeOutput createFailedNodeOutput(String nodeId, String nodeType, String code,
                                                     String messageZh, String messageEn, String cause) {
        Map<String, Object> errorInfo = buildInternalErrorInfo(code, messageZh, messageEn, cause);

        Map<String, Object> output = new HashMap<>();
        output.put("__status", "failed");
        output.put("__error", cause != null ? cause : messageEn);

        NodeOutput nodeOutput = new NodeOutput(nodeId, nodeType, new HashMap<>(), output);
        nodeOutput.setStatus("failed");
        nodeOutput.setErrorInfo(errorInfo);
        return nodeOutput;
    }

    /**
     * 创建下游错误导致的失败节点输出
     *
     * @param cause            异常原因消息 (用作 messageEn fallback)
     */
    public static NodeOutput createFailedNodeOutput(String nodeId, String nodeType, String code,
                                                     String messageZh, String messageEn,
                                                     int downstreamStatus, String downstreamBody, String cause) {
        Map<String, Object> errorInfo = buildDownstreamErrorInfo(code, messageZh, messageEn,
                downstreamStatus, downstreamBody);

        Map<String, Object> output = new HashMap<>();
        output.put("__status", "failed");
        output.put("__error", cause != null ? cause : messageEn);

        NodeOutput nodeOutput = new NodeOutput(nodeId, nodeType, new HashMap<>(), output);
        nodeOutput.setStatus("failed");
        nodeOutput.setErrorInfo(errorInfo);
        return nodeOutput;
    }

    /**
     * 创建通用错误导致的失败节点输出 (简洁重载, 兼容旧调用)
     */
    public static NodeOutput createFailedNodeOutput(String nodeId, String nodeType, String errorMessage) {
        Map<String, Object> errorInfo = buildErrorInfo(ErrorCode.ORCH_EXECUTION_FAILED, "节点执行失败", errorMessage);

        Map<String, Object> output = new HashMap<>();
        output.put("__status", "failed");
        output.put("__error", errorMessage);

        NodeOutput nodeOutput = new NodeOutput(nodeId, nodeType, new HashMap<>(), output);
        nodeOutput.setStatus("failed");
        nodeOutput.setErrorInfo(errorInfo);
        return nodeOutput;
    }

    // ===== 执行结果失败工厂方法 =====

    /**
     * 创建内部错误导致的失败执行结果
     */
    public static ExecutionResult createFailedExecutionResult(
            String executionId, String flowId, String code,
            String messageZh, String messageEn, String cause, long durationMs) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(executionId);
        result.setFlowId(flowId);
        result.setStatus("failed");
        result.setErrorInfo(buildInternalErrorInfo(code, messageZh, messageEn, cause));
        result.setTotalDurationMs(durationMs);
        return result;
    }

    /**
     * 创建下游错误导致的失败执行结果
     */
    public static ExecutionResult createFailedExecutionResult(
            String executionId, String flowId, String code,
            String messageZh, String messageEn,
            int downstreamStatus, String downstreamBody, long durationMs) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(executionId);
        result.setFlowId(flowId);
        result.setStatus("failed");
        result.setErrorInfo(buildDownstreamErrorInfo(code, messageZh, messageEn,
                downstreamStatus, downstreamBody));
        result.setTotalDurationMs(durationMs);
        return result;
    }

    /**
     * 创建通用错误导致的失败执行结果 (简洁重载, 兼容旧调用)
     */
    public static ExecutionResult createFailedExecutionResult(
            String executionId, String flowId, String errorMessage, long durationMs) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(executionId);
        result.setFlowId(flowId);
        result.setStatus("failed");
        result.setErrorInfo(buildErrorInfo(ErrorCode.ORCH_EXECUTION_FAILED, "执行失败", errorMessage));
        result.setTotalDurationMs(durationMs);
        return result;
    }
}