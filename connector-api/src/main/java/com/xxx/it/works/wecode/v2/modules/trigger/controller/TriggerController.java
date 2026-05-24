package com.xxx.it.works.wecode.v2.modules.trigger.controller;

import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.trigger.service.TriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * HTTP 触发 Controller
 * <p>
 * 对外暴露 POST /api/v1/trigger/{flowId}/invoke 端点
 * 响应外部系统请求并同步执行连接流
 * </p>
 */
@RestController
@RequestMapping("/api/v1/trigger")
@Tag(name = "HTTP 触发", description = "对外 HTTP 触发端点，同步执行连接流")
public class TriggerController {

    private static final Logger log = LoggerFactory.getLogger(TriggerController.class);

    private final TriggerService triggerService;

    public TriggerController(TriggerService triggerService) {
        this.triggerService = triggerService;
    }

    /**
     * HTTP 触发连接流执行
     * <p>
     * POST /api/v1/trigger/{flowId}/invoke
     * 请求头: X-Sys-Token (SYSTOKEN 认证)
     * 请求体: 触发数据 JSON (按 trigger.inputSchema 校验)
     * </p>
     */
    @PostMapping(value = "/{flowId}/invoke", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "HTTP 触发连接流",
               description = "接收外部系统请求并同步执行连接流，返回完整执行结果")
    public Mono<ExecutionResult> invokeFlow(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId,
            @Parameter(description = "触发数据")
            @RequestBody(required = false) Map<String, Object> triggerData,
            @RequestHeader(value = "X-Sys-Token", required = false) String sysToken,
            @RequestHeader Map<String, String> allHeaders) {

        log.info("HTTP trigger invoke: flowId={}", flowId);

        // 校验 X-Sys-Token (SYSTOKEN 认证)
        if (sysToken == null || sysToken.isEmpty()) {
            ExecutionResult errorResult = new ExecutionResult();
            errorResult.setExecutionId("N/A");
            errorResult.setFlowId(String.valueOf(flowId));
            errorResult.setStatus("failed");
            errorResult.setErrorMessage("Missing X-Sys-Token header");
            return Mono.just(errorResult);
        }

        return triggerService.invokeFlow(flowId, triggerData, allHeaders, null);
    }
}