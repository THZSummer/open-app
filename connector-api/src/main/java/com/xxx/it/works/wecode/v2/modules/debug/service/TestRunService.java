package com.xxx.it.works.wecode.v2.modules.debug.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.FlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 测试执行服务 (v5.5)
 * <p>
 * 供 open-server debug-proxy 内部转发调用
 * 支持传入模拟触发数据和 credentials
 * <br>
 * v5.5 变更:
 * <ul>
 *   <li>{@code triggerType = 3} (运行时记录维度, 非编排 JSON 中的 trigger.type)</li>
 *   <li>{@code isTest = true}</li>
 *   <li>mockTriggerData 存入 {@code NodeContext.input} 分区</li>
 *   <li>凭证从 {@code config.nodes[].data.authConfig} 声明读取</li>
 *   <li>响应 {@code errorInfo} 使用结构化格式</li>
 * </ul>
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
     * @param mockTriggerData 模拟触发数据 (存入 NodeContext.input 分区)
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
                    // 1. 解析编排配置 (React Flow 格式)
                    Map<String, Object> config = flowVersion.parseOrchestrationConfigAsMap(objectMapper);
                    List<Map<String, Object>> nodes = (List<Map<String, Object>>) config.get("nodes");
                    String triggerNodeId = resolveTriggerNodeId(nodes);

                    // 2. 创建执行上下文 (标记为测试模式)
                    ExecutionContext context = new ExecutionContext(executionId, String.valueOf(flowId));
                    context.setTriggerData(mockTriggerData != null ? mockTriggerData : Map.of());
                    context.setCredentials(credentials != null ? credentials : Map.of());
                    // v5.5: triggerType = 3 (运行时记录维度, 非编排 JSON 中的 trigger.type)
                    context.setTriggerType(3);
                    context.setTest(true);

                    // 3. 建立触发节点的 NodeContext (mockTriggerData 作为 input 分区)
                    if (triggerNodeId != null) {
                        NodeContext triggerNodeCtx = new NodeContext();
                        triggerNodeCtx.setNodeId(triggerNodeId);
                        triggerNodeCtx.setNodeType("entry");
                        Map<String, Object> input = new HashMap<>();
                        if (mockTriggerData != null) {
                            input.putAll(mockTriggerData);
                        }
                        triggerNodeCtx.setInput(input);
                        triggerNodeCtx.setOutput(new HashMap<>());
                        triggerNodeCtx.setStatus("success");
                        context.setNodeContext(triggerNodeCtx);
                    }

                    // 4. 执行连接流
                    return executor.execute(context, flowVersion.getOrchestrationConfig());
                })
                .onErrorResume(e -> {
                    log.error("Test run failed: flowId={}, error={}", flowId, e.getMessage());
                    ExecutionResult errorResult = new ExecutionResult();
                    errorResult.setExecutionId(executionId);
                    errorResult.setFlowId(String.valueOf(flowId));
                    errorResult.setStatus("failed");
                    // v5.5: 使用结构化 errorInfo
                    Map<String, Object> errInfo = new HashMap<>();
                    errInfo.put("code", "6002");
                    errInfo.put("messageZh", "测试执行失败: " + e.getMessage());
                    errInfo.put("messageEn", "Test execution failed: " + e.getMessage());
                    errInfo.put("cause", e.getMessage());
                    errorResult.setErrorInfo(errInfo);
                    errorResult.setTest(true);
                    return Mono.just(errorResult);
                });
    }

    /**
     * 解析触发节点ID (优先 type=trigger, 其次 type=entry, 最后 nodes[0])
     */
    @SuppressWarnings("unchecked")
    private String resolveTriggerNodeId(List<Map<String, Object>> nodes) {
        if (nodes == null || nodes.isEmpty()) return null;
        // 优先 trigger 类型
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");
            if ("trigger".equals(type)) {
                Object id = node.get("id");
                return id != null ? id.toString() : null;
            }
        }
        // 其次 entry 类型
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");
            if ("entry".equals(type)) {
                Object id = node.get("id");
                return id != null ? id.toString() : null;
            }
        }
        // 退化为第一个节点
        Object id = nodes.get(0).get("id");
        return id != null ? id.toString() : null;
    }
}