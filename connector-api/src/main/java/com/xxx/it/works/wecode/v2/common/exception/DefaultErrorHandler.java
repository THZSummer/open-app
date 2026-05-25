package com.xxx.it.works.wecode.v2.common.exception;

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
 */
@RestControllerAdvice(basePackages = "com.xxx.it.works.wecode.v2")
public class DefaultErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultErrorHandler.class);

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Map<String, Object>> handleException(Exception e) {
        log.error("Unhandled exception: {}", e.getMessage(), e);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "failed");
        result.put("errorMessage", e.getMessage());
        result.put("errorCode", "500");
        result.put("messageZh", "系统内部错误");
        result.put("messageEn", "Internal server error");
        return Mono.just(result);
    }

    /**
     * 处理节点执行失败 (供 NodeExecutor 调用)
     */
    public static NodeOutput createFailedNodeOutput(String nodeId, String nodeType, String errorMessage) {
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("__status", "failed");
        outputData.put("__error", errorMessage);

        NodeOutput output = new NodeOutput(nodeId, nodeType, outputData);
        output.setStatus("failed");
        output.setErrorMessage(errorMessage);
        return output;
    }

    /**
     * 创建失败的执行结果
     */
    public static ExecutionResult createFailedExecutionResult(
            String executionId, String flowId, String errorMessage, long durationMs) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(executionId);
        result.setFlowId(flowId);
        result.setStatus("failed");
        result.setErrorMessage(errorMessage);
        result.setTotalDurationMs(durationMs);
        return result;
    }
}