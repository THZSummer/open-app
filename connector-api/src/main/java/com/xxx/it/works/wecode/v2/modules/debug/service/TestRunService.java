package com.xxx.it.works.wecode.v2.modules.debug.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.repository.FlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * 测试执行服务
 * <p>
 * 供 open-server debug-proxy 内部转发调用
 * 支持传入模拟触发数据和 credentials
 * </p>
 */
@Service
public class TestRunService {

    private static final Logger log = LoggerFactory.getLogger(TestRunService.class);

    private final ObjectMapper objectMapper;
    private final ReactiveSequentialExecutor executor;
    private final FlowVersionReadRepository flowVersionReadRepository;

    public TestRunService(
            ObjectMapper objectMapper,
            ReactiveSequentialExecutor executor,
            FlowVersionReadRepository flowVersionReadRepository) {
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.flowVersionReadRepository = flowVersionReadRepository;
    }

    /**
     * 执行测试运行
     *
     * @param flowId 连接流ID
     * @param mockTriggerData 模拟触发数据
     * @param credentials 按 connectorVersionId 分组的凭证
     * @return ExecutionResult 含各步骤详情
     */
    @SuppressWarnings("unchecked")
    public Mono<ExecutionResult> executeTestRun(
            Long flowId,
            Map<String, Object> mockTriggerData,
            Map<String, Map<String, String>> credentials) {

        String executionId = UUID.randomUUID().toString().replace("-", "");

        return flowVersionReadRepository.findByFlowId(flowId)
                .switchIfEmpty(Mono.error(new RuntimeException("Flow not found: " + flowId)))
                .flatMap(flowVersion -> {
                    String config = flowVersion.getOrchestrationConfig();

                    // 创建执行上下文 (标记为测试模式)
                    ExecutionContext context = new ExecutionContext(executionId, String.valueOf(flowId));
                    context.setTriggerData(mockTriggerData != null ? mockTriggerData : Map.of());
                    context.setCredentials(credentials != null ? credentials : Map.of());
                    context.setTriggerType(2); // 测试执行
                    context.setTest(true);

                    // 执行连接流
                    return executor.execute(context, config);
                })
                .onErrorResume(e -> {
                    log.error("Test run failed: flowId={}, error={}", flowId, e.getMessage());
                    ExecutionResult errorResult = new ExecutionResult();
                    errorResult.setExecutionId(executionId);
                    errorResult.setFlowId(String.valueOf(flowId));
                    errorResult.setStatus("failed");
                    errorResult.setErrorMessage(e.getMessage());
                    errorResult.setTest(true);
                    return Mono.just(errorResult);
                });
    }
}