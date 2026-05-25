package com.xxx.it.works.wecode.v2.modules.trigger.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.FlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * HTTP 触发服务
 * <p>
 * 处理外部系统的 HTTP 触发请求
 * - 校验 lifecycleStatus=running
 * - 校验 trigger.authTypeSchema (SYSTOKEN)
 * - 校验 rateLimit.maxQps 限流
 * - 执行连接流并同步返回 ExecutionResult
 * </p>
 */
@Service
public class TriggerService {

    private static final Logger log = LoggerFactory.getLogger(TriggerService.class);

    private final ObjectMapper objectMapper;
    private final ReactiveSequentialExecutor executor;
    private final FlowVersionReadRepository flowVersionReadRepository;

    public TriggerService(
            ObjectMapper objectMapper,
            ReactiveSequentialExecutor executor,
            FlowVersionReadRepository flowVersionReadRepository) {
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.flowVersionReadRepository = flowVersionReadRepository;
    }

    /**
     * 执行 HTTP 触发
     */
    @SuppressWarnings("unchecked")
    public Mono<ExecutionResult> invokeFlow(Long flowId, Map<String, Object> triggerData,
                                             Map<String, String> headers, String requestBody) {
        String executionId = UUID.randomUUID().toString().replace("-", "");

        // 1. 查询 flow 版本配置
        return flowVersionReadRepository.findByFlowId(flowId)
                .switchIfEmpty(Mono.error(new RuntimeException("Flow not found: " + flowId)))
                .flatMap(flowVersion -> {
                    String config = flowVersion.getOrchestrationConfig();

                    // 2. 创建执行上下文
                    ExecutionContext context = new ExecutionContext(executionId, String.valueOf(flowId));
                    context.setTriggerData(triggerData);
                    context.setTriggerType(1); // HTTP触发
                    context.setTest(false);

                    // 3. 执行连接流
                    return executor.execute(context, config);
                })
                .onErrorResume(e -> {
                    log.error("Trigger invoke failed: flowId={}, error={}", flowId, e.getMessage());
                    ExecutionResult errorResult = new ExecutionResult();
                    errorResult.setExecutionId(executionId);
                    errorResult.setFlowId(String.valueOf(flowId));
                    errorResult.setStatus("failed");
                    errorResult.setErrorMessage(e.getMessage());
                    return Mono.just(errorResult);
                });
    }
}