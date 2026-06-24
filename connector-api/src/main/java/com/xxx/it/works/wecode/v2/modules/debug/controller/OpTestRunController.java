package com.xxx.it.works.wecode.v2.modules.debug.controller;

import com.xxx.it.works.wecode.v2.modules.debug.service.OpTestRunService;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 测试执行 Controller
 * <p>
 * 内部测试接口, 供 open-server debug-proxy 转发调用
 * 仅限内网调用
 * </p>
 * <p>
 * v5.5:
 * <ul>
 *   <li>使用 {@code mockTriggerData} 作为触发输入</li>
 *   <li>{@code triggerType = 3} (运行时记录维度, 非编排 JSON 中)</li>
 *   <li>{@code isTest = true}</li>
 *   <li>凭证从 {@code data.authConfig} 声明读取</li>
 *   <li>响应 {@code errorInfo} 使用结构化格式 {@code {code, messageZh, messageEn}}</li>
 * </ul>
 * </p>
 */
@RestController
@Tag(name = "测试执行", description = "内部测试执行接口，供 open-server debug-proxy 调用 (v5.5)")
public class OpTestRunController {

    private static final Logger log = LoggerFactory.getLogger(OpTestRunController.class);

    private final OpTestRunService testRunService;

    public OpTestRunController(OpTestRunService testRunService) {
        this.testRunService = testRunService;
    }

    /**
     * 执行测试运行
     * <p>
     * 接收 mockTriggerData + credentials (按 connectorVersionId 分组)
     * <ul>
     *   <li>{@code triggerType = 3} (运行时记录维度)</li>
     *   <li>{@code isTest = true}</li>
     *   <li>触发数据从 {@code request.mockTriggerData} 读取</li>
     * </ul>
     * </p>
     */
    @PostMapping(value = "/api/v1/flows/{flowId}/versions/{versionId}/debug", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "执行测试运行 (v5.5)",
               description = "内部端点，接收模拟触发数据和凭证，同步执行连接流并返回结果."
                            + " triggerType=3 (运行时记录维度), isTest=true.")
    public Mono<ExecutionResult> executeTestRun(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId,
            @Parameter(description = "版本ID")
            @PathVariable Long versionId,
            @RequestBody TestRunRequest request) {

        log.info("Internal test run: flowId={}, versionId={}", flowId, versionId);

        return testRunService.executeTestRun(
                flowId,
                versionId,
                request != null ? request.getMockTriggerData() : null);
    }

    /**
     * 执行测试运行 (向后兼容旧路径)
     * <p>
     * 接收 {@code {"mockTriggerData": {...}}} JSON 对象, 使用 flowId 作为 versionId 调用服务.
     * </p>
     */
    @PostMapping(value = "/api/v1/internal/test-run/{flowId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "执行测试运行 (向后兼容路径)",
               description = "内部端点 (兼容旧测试脚本), 接收 mockTriggerData 并使用 flowId 作为 versionId.")
    public Mono<ExecutionResult> executeTestRunLegacy(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId,
            @RequestBody(required = false) Map<String, Object> body) {

        log.info("Internal test run (legacy): flowId={}", flowId);

        @SuppressWarnings("unchecked")
        Map<String, Object> mockTriggerData = null;
        if (body != null && body.containsKey("mockTriggerData")) {
            mockTriggerData = (Map<String, Object>) body.get("mockTriggerData");
        }

        return testRunService.executeTestRun(flowId, flowId, mockTriggerData);
    }

    /**
     * 测试运行请求体 (v5.5)
     * <p>
     * {@code mockTriggerData}: 模拟触发数据, 存入 {@code NodeContext.input} 分区<br>
     * </p>
     */
    public static class TestRunRequest {
        private Map<String, Object> mockTriggerData;

        public Map<String, Object> getMockTriggerData() { return mockTriggerData; }
        public void setMockTriggerData(Map<String, Object> mockTriggerData) { this.mockTriggerData = mockTriggerData; }

    }
}