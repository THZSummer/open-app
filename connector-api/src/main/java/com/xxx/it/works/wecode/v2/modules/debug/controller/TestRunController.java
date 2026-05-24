package com.xxx.it.works.wecode.v2.modules.debug.controller;

import com.xxx.it.works.wecode.v2.modules.debug.service.TestRunService;
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
 */
@RestController
@RequestMapping("/api/v1/internal/test-run")
@Tag(name = "测试执行", description = "内部测试执行接口，供 open-server debug-proxy 调用")
public class TestRunController {

    private static final Logger log = LoggerFactory.getLogger(TestRunController.class);

    private final TestRunService testRunService;

    public TestRunController(TestRunService testRunService) {
        this.testRunService = testRunService;
    }

    /**
     * 执行测试运行
     * <p>
     * 接收 mockTriggerData + credentials (按 connectorVersionId 分组)
     * 标记 isTest=true, triggerType=3
     * </p>
     */
    @PostMapping(value = "/{flowId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "执行测试运行",
               description = "内部端点，接收模拟触发数据和凭证，同步执行连接流并返回结果")
    public Mono<ExecutionResult> executeTestRun(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId,
            @RequestBody TestRunRequest request) {

        log.info("Internal test run: flowId={}", flowId);

        return testRunService.executeTestRun(
                flowId,
                request != null ? request.getMockTriggerData() : null,
                request != null ? request.getCredentials() : null
        );
    }

    /**
     * 测试运行请求体
     */
    public static class TestRunRequest {
        private Map<String, Object> mockTriggerData;
        private Map<String, Map<String, String>> credentials;

        public Map<String, Object> getMockTriggerData() { return mockTriggerData; }
        public void setMockTriggerData(Map<String, Object> mockTriggerData) { this.mockTriggerData = mockTriggerData; }

        public Map<String, Map<String, String>> getCredentials() { return credentials; }
        public void setCredentials(Map<String, Map<String, String>> credentials) { this.credentials = credentials; }
    }
}