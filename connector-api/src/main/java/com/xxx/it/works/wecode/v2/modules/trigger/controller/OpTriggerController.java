package com.xxx.it.works.wecode.v2.modules.trigger.controller;

import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.trigger.service.OpTriggerService;
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
 * <p>
 * v5.5:
 * <ul>
 *   <li>触发配置从 {@code config.nodes[].data.*} (React Flow 格式) 读取</li>
 *   <li>认证校验: {@code data.authConfig.type} (SYSTOKEN 等)</li>
 *   <li>限流校验: {@code data.rateLimitConfig.maxQps}</li>
 *   <li>请求体验证: {@code data.inputContract} JSON Schema</li>
 *   <li>响应 {@code errorInfo} 使用结构化格式 {@code {code, messageZh, messageEn}}</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/trigger")
@Tag(name = "HTTP 触发", description = "对外 HTTP 触发端点，同步执行连接流 (v5.5)")
public class OpTriggerController {

    private static final Logger log = LoggerFactory.getLogger(OpTriggerController.class);

    private final OpTriggerService triggerService;

    public OpTriggerController(OpTriggerService triggerService) {
        this.triggerService = triggerService;
    }

    /**
     * HTTP 触发连接流执行
     * <p>
     * POST /api/v1/trigger/{flowId}/invoke
     * 认证校验已移至 OpTriggerService 中按 data.authConfig.type 动态处理。
     * <ul>
     *   <li>请求体: 触发数据 JSON (按 {@code data.inputContract} JSON Schema 校验)</li>
     *   <li>触发数据存入 {@code nodeContexts["node_trigger"].input}</li>
     * </ul>
     * </p>
     */
    @PostMapping(value = "/{flowId}/invoke", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "HTTP 触发连接流 (v5.5)",
               description = "接收外部系统请求并同步执行连接流，返回完整执行结果。"
                           + "请求体按 data.inputContract 校验，认证按 data.authConfig 校验，限流按 data.rateLimitConfig.maxQps."
                           + "请求头 hasSteps=true 时返回各步骤详情，默认不返回。")
    public Mono<ExecutionResult> invokeFlow(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId,
            @Parameter(description = "触发数据 (按 inputContract 校验)")
            @RequestBody(required = false) Map<String, Object> triggerData,
            @RequestHeader Map<String, String> allHeaders,
            @Parameter(description = "URL Query 参数")
            @RequestParam(required = false) Map<String, String> queryParams) {

        log.info("HTTP trigger invoke: flowId={}", flowId);

        // 认证/限流/入参校验全部由 OpTriggerService 根据编排配置中的 data.authConfig/data.rateLimitConfig/data.inputContract 动态处理
        return triggerService.invokeFlow(flowId, triggerData, allHeaders, queryParams)
                .map(result -> {
                    if (!"true".equalsIgnoreCase(allHeaders.get("hasSteps"))) {
                        result.getSteps().clear();
                    }
                    return result;
                });
    }
}