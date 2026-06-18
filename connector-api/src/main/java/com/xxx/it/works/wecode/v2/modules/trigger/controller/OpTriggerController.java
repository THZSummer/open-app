package com.xxx.it.works.wecode.v2.modules.trigger.controller;

import com.xxx.it.works.wecode.v2.modules.runtime.model.TransparentFlowResponse;
import com.xxx.it.works.wecode.v2.modules.trigger.service.OpTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * HTTP 触发 Controller (v5.8)
 * <p>
 * 对外暴露 POST /api/v1/trigger/{flowId}/invoke 端点
 * 响应外部系统请求并同步执行连接流
 * </p>
 * <p>
 * v5.8 变更:
 * <ul>
 *   <li>响应格式从 JSON 信封 ({@code ExecutionResult}) 改为透明穿透</li>
 *   <li>出口节点 output.body → HTTP 响应体 (裸数据)</li>
 *   <li>出口节点 output.header → 用户自定义 HTTP 响应头</li>
 *   <li>平台元数据 → X- 前缀 HTTP 响应头</li>
 * </ul>
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
     * HTTP 触发连接流执行 (v5.8 透明穿透)
     * <p>
     * POST /api/v1/trigger/{flowId}/invoke
     * 认证/限流/入参校验全部由 OpTriggerService 根据编排配置动态处理.
     * <ul>
     *   <li>响应体: 出口节点 output.body 裸数据 (不再是 ExecutionResult JSON 信封)</li>
     *   <li>用户自定义响应头: 出口节点 output.header → HTTP 响应头</li>
     *   <li>平台元数据: X-Flow-Id / X-Execution-Id / X-Status / X-Duration-Ms / X-Code / X-Message-Zh / X-Message-En / X-Cache-Status</li>
     * </ul>
     * </p>
     */
    @PostMapping(value = "/{flowId}/invoke", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "HTTP 触发连接流 (v5.8 透明穿透)",
               description = "接收外部系统请求并同步执行连接流, 出口节点 body/header 直接作为 HTTP 响应体/头返回."
                           + "平台元数据通过 X- 前缀 HTTP 响应头携带.")
    public Mono<ResponseEntity<Object>> invokeFlow(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId,
            @Parameter(description = "触发数据 (按 inputContract 校验)")
            @RequestBody(required = false) Map<String, Object> triggerData,
            @RequestHeader Map<String, String> allHeaders,
            @Parameter(description = "URL Query 参数")
            @RequestParam(required = false) Map<String, String> queryParams) {

        log.info("HTTP trigger invoke: flowId={}", flowId);

        // 认证/限流/入参校验 + 执行全部由 OpTriggerService 处理
        return triggerService.invokeFlow(flowId, triggerData, allHeaders, queryParams)
                .map(response -> {
                    HttpHeaders headers = new HttpHeaders();
                    // 先放平台 X- 头
                    response.getPlatformHeaders().forEach(headers::add);
                    // 再放用户自定义头 (不与平台 X- 头冲突)
                    response.getUserHeaders().forEach((k, v) -> {
                        if (!k.startsWith("X-Flow-") && !k.startsWith("X-Execution-")
                                && !k.startsWith("X-Status") && !k.startsWith("X-Duration-")
                                && !k.startsWith("X-Cache-") && !k.startsWith("X-Code")
                                && !k.startsWith("X-Message-")) {
                            headers.add(k, String.valueOf(v));
                        }
                    });
                    Object body = response.getBody();
                    return new ResponseEntity<>(body, headers, response.getHttpStatus());
                });
    }
}