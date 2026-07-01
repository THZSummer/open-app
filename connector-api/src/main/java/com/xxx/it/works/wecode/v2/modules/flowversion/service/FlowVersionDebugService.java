package com.xxx.it.works.wecode.v2.modules.flowversion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.common.error.ErrorCode;
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
 * 支持传入模拟触发数据
 * <br>
 * v5.5 变更:
 * <ul>
 *   <li>{@code triggerType = 3} (运行时记录维度, 非编排 JSON 中的 trigger.type)</li>
   *   <li>{@code isDebug = true}</li>
 *   <li>mockTriggerData 存入 {@code NodeContext.input} 分区</li>
 *   <li>凭证从 {@code config.nodes[].data.authConfig} 声明读取</li>
 *   <li>响应 {@code errorInfo} 使用结构化格式</li>
 * </ul>
 * <br>
 * 注意: debug 请求不写入执行记录表（execution_record / execution_step），
 * 仅用于即时调试反馈，不产生持久化记录。
 * </p>
 */
@Service
public class FlowVersionDebugService {

    private static final Logger log = LoggerFactory.getLogger(FlowVersionDebugService.class);

    private final ObjectMapper objectMapper;
    private final ReactiveSequentialExecutor executor;
    private final OpFlowVersionReadRepository flowVersionReadRepository;

    public FlowVersionDebugService(
            ObjectMapper objectMapper,
            ReactiveSequentialExecutor executor,
            OpFlowVersionReadRepository flowVersionReadRepository) {
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.flowVersionReadRepository = flowVersionReadRepository;
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

        return flowVersionReadRepository.findById(versionId)
                .switchIfEmpty(Mono.error(new PreCheckException(
                        ErrorCode.PRECHECK_VERSION_NOT_FOUND, "版本不存在，请检查版本 ID", "Version not found")))
                .flatMap(flowVersion -> {
                    Integer status = flowVersion.getStatus();
                    // 版本已失效
                    if (status != null && status == 6) {
                        return Mono.error(new PreCheckException(
                                ErrorCode.PRECHECK_VERSION_INVALIDATED,
                                "版本已失效，不可调试",
                                "The version has been invalidated and cannot be debugged"));
                    }
                    // 版本状态不支持调试: 仅 DRAFT(1) 和 PUBLISHED(5) 可调试
                    if (status != null && status != 1 && status != 5) {
                        String statusName = switch (status) {
                            case 2 -> "待审批"; case 3 -> "已撤回"; case 4 -> "已驳回";
                            default -> String.valueOf(status);
                        };
                        return Mono.error(new PreCheckException(
                                ErrorCode.PRECHECK_VERSION_STATUS_NOT_DEBUGGABLE,
                                "版本状态为「" + statusName + "」，仅草稿和已发布版本可调试",
                                "Version status is '" + statusName + "', only draft and published versions can be debugged"));
                    }
                    // 编排配置非空校验
                    String orchConfig = flowVersion.getOrchestrationConfig();
                    if (orchConfig == null || orchConfig.isBlank()) {
                        return Mono.error(new PreCheckException(
                                ErrorCode.PRECHECK_ORCHESTRATION_EMPTY,
                                "编排配置为空，请先完成编排后再调试",
                                "Orchestration config is empty"));
                    }
                    // 解析编排配置
                    Map<String, Object> config = flowVersion.parseOrchestrationConfigAsMap(objectMapper);
                    List<Map<String, Object>> nodes = (List<Map<String, Object>>) config.get("nodes");
                    String triggerNodeId = resolveTriggerNodeId(nodes);

                    // 创建执行上下文 (标记为调试模式)
                    ExecutionContext context = new ExecutionContext(executionId, String.valueOf(flowId));
                    context.setTriggerData(mockTriggerData != null ? mockTriggerData : Map.of());
                    context.setTriggerType(3);
                    context.setDebug(true);

                    // 建立触发节点的 NodeContext
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

                    // 执行连接流 (debug 不写执行记录)
                    return executor.execute(context, orchConfig);
                })
                .onErrorResume(PreCheckException.class, e -> {
                    log.warn("Pre-check failed for debug: flowId={}, versionId={}, code={}, msg={}",
                            flowId, versionId, e.getCode(), e.getMessageZh());
                    ExecutionResult errorResult = new ExecutionResult();
                    errorResult.setExecutionId(executionId);
                    errorResult.setFlowId(String.valueOf(flowId));
                    errorResult.setStatus("failed");
                    errorResult.setDebug(true);
                    errorResult.setErrorInfo(e.toErrorInfo());
                    return Mono.just(errorResult);
                })
                .onErrorResume(e -> {
                    // 执行层错误 — 保留 DagScheduler/NodeExecutor 产出的 errorInfo
                    log.error("Test run failed: flowId={}, error={}", flowId, e.getMessage());
                    ExecutionResult errorResult = new ExecutionResult();
                    errorResult.setExecutionId(executionId);
                    errorResult.setFlowId(String.valueOf(flowId));
                    errorResult.setStatus("failed");
                    errorResult.setDebug(true);
                    // 兜底 errorInfo
                    Map<String, Object> errInfo = new HashMap<>();
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    errInfo.put("code", "60001");
                    errInfo.put("messageZh", "测试执行失败: " + msg);
                    errInfo.put("messageEn", "Test execution failed: " + msg);
                    errorResult.setErrorInfo(errInfo);
                    return Mono.just(errorResult);
                });
    }

    /**
     * 解析触发节点ID (匹配 type=trigger)
     */
    @SuppressWarnings("unchecked")
    private String resolveTriggerNodeId(List<Map<String, Object>> nodes) {
        if (nodes == null || nodes.isEmpty()) { return null; }
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

    /**
     * 前置校验异常 — 携带结构化错误码
     */
    public static class PreCheckException extends RuntimeException {
        private final String code;
        private final String messageZh;
        private final String messageEn;

        public PreCheckException(String code, String messageZh, String messageEn) {
            super(messageZh);
            this.code = code;
            this.messageZh = messageZh;
            this.messageEn = messageEn;
        }

        public String getCode() { return code; }
        public String getMessageZh() { return messageZh; }
        public String getMessageEn() { return messageEn; }

        public Map<String, Object> toErrorInfo() {
            Map<String, Object> info = new HashMap<>();
            info.put("code", code);
            info.put("messageZh", messageZh);
            info.put("messageEn", messageEn != null ? messageEn : messageZh);
            return info;
        }
    }
}
