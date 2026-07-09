package com.xxx.it.works.wecode.v2.modules.flowversion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.NodeTypeResolver;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.DagScheduler;
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
    private final DagScheduler dagScheduler;
    private final OpFlowVersionReadRepository flowVersionReadRepository;

    public FlowVersionDebugService(
            ObjectMapper objectMapper,
            DagScheduler dagScheduler,
            OpFlowVersionReadRepository flowVersionReadRepository) {
        this.objectMapper = objectMapper;
        this.dagScheduler = dagScheduler;
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
                    // 调试接口不判断版本状态: 草稿/已发布/待审批/已撤回/已驳回/已失效 均可调试
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

                    // debug 前置校验提示 (不阻断, 仅 WARN 告知用户 invoke 时会校验)
                    warnDebugValidationGaps(config, nodes, mockTriggerData);

                    // 创建执行上下文 (标记为调试模式)
                    ExecutionContext context = new ExecutionContext(executionId, String.valueOf(flowId));
                    context.setTriggerData(mockTriggerData != null ? mockTriggerData : Map.of());
                    context.setTriggerType(2);
                    context.setDebug(true);

                    // 建立触发节点的 NodeContext (结构化: {header, query, body})
                    if (triggerNodeId != null) {
                        NodeContext triggerNodeCtx = new NodeContext();
                        triggerNodeCtx.setNodeId(triggerNodeId);
                        triggerNodeCtx.setNodeType("trigger");
                        Map<String, Object> input = buildDebugTriggerInput(mockTriggerData);
                        triggerNodeCtx.setInput(input);
                        triggerNodeCtx.setOutput(new HashMap<>());
                        triggerNodeCtx.setStatus("success");
                        context.setNodeContext(triggerNodeCtx);
                    }

                    // 执行连接流 (debug 不写执行记录, 统一使用 DagScheduler)
                    return dagScheduler.schedule(orchConfig, context)
                            .map(updatedCtx -> buildDebugResult(updatedCtx, executionId, nodes));
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
                    errInfo.put("code", ErrorCode.ORCH_NODE_EXECUTION_FAILED);
                    errInfo.put("messageZh", "测试执行失败: " + msg);
                    errInfo.put("messageEn", "Test execution failed: " + msg);
                    errorResult.setErrorInfo(errInfo);
                    return Mono.just(errorResult);
                });
    }

    /**
     * debug 前置校验差异提示: invoke 接口会校验但这些在 debug 中被跳过的项
     * (不阻断执行, 仅 WARN 日志告知用户)
     */
    @SuppressWarnings("unchecked")
    private void warnDebugValidationGaps(Map<String, Object> config,
                                          List<Map<String, Object>> nodes,
                                          Map<String, Object> mockTriggerData) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        // 查找 trigger 节点
        Map<String, Object> triggerNode = null;
        for (Map<String, Object> node : nodes) {
            String type = NodeTypeResolver.businessType(node);
            if ("trigger".equals(type)) {
                triggerNode = (Map<String, Object>) node.get("data");
                break;
            }
        }
        if (triggerNode == null) {
            return;
        }

        // triggerType 校验
        String triggerType = (String) triggerNode.get("triggerType");
        if (triggerType == null || triggerType.isBlank()) {
            log.warn("Debug: 触发节点 data.triggerType 未配置 (invoke 接口会拒绝执行)");
        } else if (!"http".equals(triggerType) && !"manual".equals(triggerType)) {
            log.warn("Debug: 未知的触发类型 '{}' (invoke 接口会拒绝执行)", triggerType);
        }

        // authConfigs 校验
        List<Map<String, Object>> authConfigs = (List<Map<String, Object>>) triggerNode.get("authConfigs");
        if ("http".equals(triggerType) && (authConfigs == null || authConfigs.isEmpty())) {
            log.warn("Debug: HTTP 触发器未配置 authConfigs (invoke 接口会拒绝执行)");
        }

        // inputContract 校验: 检查 mockTriggerData 是否满足 required 字段
        Map<String, Object> inputContract = (Map<String, Object>) triggerNode.get("input");
        if (inputContract != null && mockTriggerData != null) {
            warnInputContractGap(inputContract, mockTriggerData);
        }
    }

    /**
     * 检查 mockTriggerData 是否满足 inputContract 的 required 字段
     */
    @SuppressWarnings("unchecked")
    private void warnInputContractGap(Map<String, Object> inputContract, Map<String, Object> mockTriggerData) {
        for (String section : new String[]{"header", "query", "body"}) {
            Map<String, Object> schema = (Map<String, Object>) inputContract.get(section);
            if (schema == null) {
                continue;
            }
            List<String> required = (List<String>) schema.get("required");
            if (required == null || required.isEmpty()) {
                continue;
            }
            Map<String, Object> sectionData;
            if ("body".equals(section)) {
                sectionData = mockTriggerData;
            } else {
                Object sectionObj = mockTriggerData.get(section);
                sectionData = sectionObj instanceof Map ? (Map<String, Object>) sectionObj : new HashMap<>();
            }
            for (String field : required) {
                if (!sectionData.containsKey(field) || sectionData.get(field) == null) {
                    log.warn("Debug: mockTriggerData {} 缺少必填字段 '{}' (invoke 接口会拒绝执行)", section, field);
                }
            }
        }
    }

    /**
     * 从 ExecutionContext 构建 ExecutionResult (debug 专用, 不写执行记录)
     */
    private ExecutionResult buildDebugResult(ExecutionContext ctx, String executionId,
                                               List<Map<String, Object>> nodes) {
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();
        if (nodes != null) {
            for (Map<String, Object> node : nodes) {
                Object id = node.get("id");
                if (id != null) {
                    nodeMap.put(id.toString(), node);
                }
            }
        }

        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(executionId);
        result.setFlowId(ctx.getFlowId());
        result.setDebug(true);

        boolean anyFailed = false;
        Map<String, Object> lastOutput = null;
        long totalDurationMs = 0;

        for (Map.Entry<String, NodeContext> entry : ctx.getNodeContexts().entrySet()) {
            NodeContext nodeCtx = entry.getValue();
            if (nodeCtx.getDurationMs() > totalDurationMs) {
                totalDurationMs = nodeCtx.getDurationMs();
            }

            ExecutionResult.StepDetail step = new ExecutionResult.StepDetail();
            step.setNodeId(nodeCtx.getNodeId());
            step.setNodeType(nodeCtx.getNodeType());
            step.setInputData(nodeCtx.getInput());
            step.setOutputData(nodeCtx.getOutput());
            step.setStatus(nodeCtx.getStatus());
            step.setDurationMs(nodeCtx.getDurationMs());
            step.setErrorInfo(nodeCtx.getErrorInfo());

            Map<String, Object> nodeConfig = nodeMap.get(nodeCtx.getNodeId());
            if (nodeConfig != null) {
                Map<String, Object> data = (Map<String, Object>) nodeConfig.get("data");
                if (data != null) {
                    step.setLabelCn((String) data.get("labelCn"));
                    step.setLabelEn((String) data.get("labelEn"));
                }
            }

            if ("failed".equals(nodeCtx.getStatus()) || "timeout".equals(nodeCtx.getStatus())) {
                anyFailed = true;
                if (result.getErrorInfo() == null && nodeCtx.getErrorInfo() != null
                        && !nodeCtx.getErrorInfo().isEmpty()) {
                    result.setErrorInfo(new HashMap<>(nodeCtx.getErrorInfo()));
                }
            }

            result.addStep(step);

            if (nodeCtx.getOutput() != null && !nodeCtx.getOutput().isEmpty()) {
                lastOutput = nodeCtx.getOutput();
            }
        }

        Map<String, Object> exitOutput = findExitNodeOutput(ctx);
        if (exitOutput != null) {
            lastOutput = exitOutput;
        }

        result.setStatus(anyFailed ? "failed" : "success");
        result.setTotalDurationMs(totalDurationMs);

        if (lastOutput != null) {
            Map<String, Object> cleanOutput = new HashMap<>(lastOutput);
            cleanOutput.remove("__status");
            cleanOutput.remove("__input");
            cleanOutput.remove("__error");
            result.setResultData(cleanOutput);
        }

        return result;
    }

    private Map<String, Object> findExitNodeOutput(ExecutionContext ctx) {
        for (NodeContext nc : ctx.getNodeContexts().values()) {
            if ("exit".equals(nc.getNodeType())) {
                if (nc.getOutput() != null && !nc.getOutput().isEmpty()) {
                    return nc.getOutput();
                }
                return null;
            }
        }
        return null;
    }

    /**
     * 解析触发节点ID (匹配 type=trigger)
     */
    @SuppressWarnings("unchecked")
    private String resolveTriggerNodeId(List<Map<String, Object>> nodes) {
        if (nodes == null || nodes.isEmpty()) { return null; }
        for (Map<String, Object> node : nodes) {
            String type = NodeTypeResolver.businessType(node);
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
     * 构建调试模式的 trigger input 结构 ({header, query, body})
     * <p>
     * mockTriggerData 中可能有 {@code header}/{@code query} 子对象，
     * 提取后剩余字段放入 body。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildDebugTriggerInput(Map<String, Object> mockTriggerData) {
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> headerPart = new HashMap<>();
        Map<String, Object> queryPart = new HashMap<>();
        Map<String, Object> bodyPart = new HashMap<>();

        if (mockTriggerData != null) {
            Object h = mockTriggerData.get("header");
            if (h instanceof Map) {
                headerPart.putAll((Map<String, Object>) h);
            }
            Object q = mockTriggerData.get("query");
            if (q instanceof Map) {
                queryPart.putAll((Map<String, Object>) q);
            }
            for (Map.Entry<String, Object> entry : mockTriggerData.entrySet()) {
                String key = entry.getKey();
                if (!"header".equals(key) && !"query".equals(key)) {
                    bodyPart.put(key, entry.getValue());
                }
            }
        }

        input.put("header", headerPart);
        input.put("query", queryPart);
        input.put("body", bodyPart);
        return input;
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
