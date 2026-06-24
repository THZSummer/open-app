package com.xxx.it.works.wecode.v2.modules.debug.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.IdGenerator;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionRecordService;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionStepService;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionStepService.StepLog;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 测试执行服务 (v5.5)
 * <p>
 * 供 open-server debug-proxy 内部转发调用
 * 支持传入模拟触发数据
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
public class OpTestRunService {

    private static final Logger log = LoggerFactory.getLogger(OpTestRunService.class);

    private final ObjectMapper objectMapper;
    private final ReactiveSequentialExecutor executor;
    private final OpFlowVersionReadRepository flowVersionReadRepository;
    private final ExecutionRecordService executionRecordService;
    private final ExecutionStepService executionStepService;
    private final IdGenerator idGenerator;

    public OpTestRunService(
            ObjectMapper objectMapper,
            ReactiveSequentialExecutor executor,
            OpFlowVersionReadRepository flowVersionReadRepository,
            ExecutionRecordService executionRecordService,
            ExecutionStepService executionStepService,
            IdGenerator idGenerator) {
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.flowVersionReadRepository = flowVersionReadRepository;
        this.executionRecordService = executionRecordService;
        this.executionStepService = executionStepService;
        this.idGenerator = idGenerator;
    }

    /**
     * 执行测试运行
     *
     * @param flowId 连接流ID
     * @param versionId 版本ID
     * @param mockTriggerData 模拟触发数据 (存入 NodeContext.input 分区)
     * @return ExecutionResult 含各步骤详情
     */
    @SuppressWarnings("unchecked")
    public Mono<ExecutionResult> executeTestRun(
            Long flowId,
            Long versionId,
            Map<String, Object> mockTriggerData) {

        String executionId = UUID.randomUUID().toString().replace("-", "");
        // ★ 执行记录持久化: 创建记录 (status=pending, triggerType=2=调试触发)
        Long recordId = idGenerator.nextId();
        try {
            executionRecordService.startRecord(recordId, flowId, versionId, null, 2);
            log.debug("Execution record started: recordId={}, executionId={}", recordId, executionId);
        } catch (Exception ex) {
            log.warn("Failed to start execution record: {}", ex.getMessage());
        }

        return flowVersionReadRepository.findById(versionId)
                .switchIfEmpty(Mono.error(new RuntimeException("Flow not found: " + flowId)))
                .flatMap(flowVersion -> {
                    Integer status = flowVersion.getStatus();
                    if (status != null && status == 6) {
                        return Mono.error(new RuntimeException("该版本已失效，不可调试"));
                    }
                    // 1. 解析编排配置 (React Flow 格式)
                    Map<String, Object> config = flowVersion.parseOrchestrationConfigAsMap(objectMapper);
                    List<Map<String, Object>> nodes = (List<Map<String, Object>>) config.get("nodes");
                    String triggerNodeId = resolveTriggerNodeId(nodes);

                    // 2. 创建执行上下文 (标记为测试模式)
                    ExecutionContext context = new ExecutionContext(executionId, String.valueOf(flowId));
                    context.setTriggerData(mockTriggerData != null ? mockTriggerData : Map.of());
                    // v5.5: triggerType = 3 (运行时记录维度, 非编排 JSON 中的 trigger.type)
                    context.setTriggerType(3);
                    context.setTest(true);

                    // 3. 建立触发节点的 NodeContext (mockTriggerData 作为 input 分区)
                    if (triggerNodeId != null) {
                        NodeContext triggerNodeCtx = new NodeContext();
                        triggerNodeCtx.setNodeId(triggerNodeId);
                        triggerNodeCtx.setNodeType("trigger");
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
                    return executor.execute(context, flowVersion.getOrchestrationConfig())
                            .doOnSuccess(result -> {
                                // ★ 步骤日志持久化 (fire-and-forget, 不影响业务响应)
                                try {
                                    if (result.getSteps() != null && !result.getSteps().isEmpty()) {
                                        List<StepLog> stepLogs = new ArrayList<>();
                                        for (ExecutionResult.StepDetail step : result.getSteps()) {
                                            StepLog sl = new StepLog();
                                            sl.stepId = idGenerator.nextId();
                                            sl.nodeId = step.getNodeId();
                                            sl.nodeType = mapNodeType(step.getNodeType());
                                            sl.nodeName = step.getLabelCn() != null ? step.getLabelCn() : step.getNodeId();
                                            sl.status = "success".equals(step.getStatus()) ? 0 : 1;
                                            sl.input = step.getInputData();
                                            sl.output = step.getOutputData();
                                            sl.error = step.getErrorInfo() != null ? String.valueOf(step.getErrorInfo()) : null;
                                            sl.durationMs = (int) step.getDurationMs();
                                            stepLogs.add(sl);
                                        }
                                        executionStepService.logStepsBatch(recordId, stepLogs);
                                    }
                                } catch (Exception ex) {
                                    log.warn("Failed to log test run steps: {}", ex.getMessage());
                                }

                                // ★ 执行成功 - 更新记录
                                try {
                                    executionRecordService.updateRecord(recordId, 0, null, null, null);
                                } catch (Exception ex) {
                                    log.warn("Failed to update execution record: {}", ex.getMessage());
                                }
                            });
                })
                .onErrorResume(e -> {
                    log.error("Test run failed: flowId={}, error={}", flowId, e.getMessage());
                    ExecutionResult errorResult = new ExecutionResult();
                    errorResult.setExecutionId(executionId);
                    errorResult.setFlowId(String.valueOf(flowId));
                    errorResult.setStatus("failed");
                    // v5.5: 使用结构化 errorInfo
                    Map<String, Object> errInfo = new HashMap<>();
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("已失效")) {
                        errInfo.put("code", "6004");
                        errInfo.put("messageZh", msg);
                        errInfo.put("messageEn", "The version has been invalidated and cannot be debugged");
                    } else {
                        errInfo.put("code", "6002");
                        errInfo.put("messageZh", "测试执行失败: " + msg);
                        errInfo.put("messageEn", "Test execution failed: " + msg);
                    }
                    errInfo.put("cause", msg);
                    errorResult.setErrorInfo(errInfo);
                    errorResult.setTest(true);
                    // ★ 执行失败 - 更新记录
                    try {
                        String errorCode = (String) errInfo.get("code");
                        String errorMsg = (String) errInfo.get("messageZh");
                        executionRecordService.updateRecord(recordId, 1, null, errorCode, errorMsg);
                    } catch (Exception ex) {
                        log.warn("Failed to update execution record: {}", ex.getMessage());
                    }
                    return Mono.just(errorResult);
                });
    }

    /**
     * 将节点类型字符串映射为数字
     * @param nodeType 节点类型字符串 (trigger/connector/script/parallel/exit)
     * @return 1=trigger, 2=connector, 3=script, 4=parallel, 5=exit, 0=unknown
     */
    private Integer mapNodeType(String nodeType) {
        if (nodeType == null) return 0;
        return switch (nodeType.toLowerCase()) {
            case "trigger" -> 1;
            case "connector" -> 2;
            case "script" -> 3;
            case "parallel" -> 4;
            case "exit" -> 5;
            default -> 0;
        };
    }

    /**
     * 解析触发节点ID (匹配 type=trigger)
     */
    @SuppressWarnings("unchecked")
    private String resolveTriggerNodeId(List<Map<String, Object>> nodes) {
        if (nodes == null || nodes.isEmpty()) return null;
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");
            if ("trigger".equals(type)) {
                Object id = node.get("id");
                return id != null ? id.toString() : null;
            }
        }
        // 退化为第一个节点
        Object id = nodes.get(0).get("id");
        return id != null ? id.toString() : null;
    }
}