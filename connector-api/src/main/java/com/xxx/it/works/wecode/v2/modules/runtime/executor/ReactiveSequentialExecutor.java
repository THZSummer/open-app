package com.xxx.it.works.wecode.v2.modules.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import com.xxx.it.works.wecode.v2.modules.runtime.node.ConnectorNodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.DataProcessorExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.EntryNodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.ExitNodeExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 反应式顺序执行器
 * <p>
 * v5.5:
 * <ul>
 *   <li>使用 {@link NodeContext} 替代旧 {@code Map<String, Map<String, Object>>} 存储节点数据</li>
 *   <li>edges 中 {@code sourceNodeId} → {@code source}, {@code targetNodeId} → {@code target}</li>
 *   <li>节点配置访问 {@code node.data.xxx} (React Flow 格式)</li>
 *   <li>{@link ExecutionResult.StepDetail} 使用 {@code errorInfo} Map 和 {@code labelCn}/{@code labelEn}</li>
 * </ul>
 * 从入口节点开始, 按 edges 拓扑顺序 flatMap 串联各节点 Mono&lt;NodeOutput&gt;.
 * 对调用方呈现为同步请求-响应语义.
 * </p>
 */
public class ReactiveSequentialExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReactiveSequentialExecutor.class);

    private final ObjectMapper objectMapper;
    private final Map<String, NodeExecutor> executorMap;

    public ReactiveSequentialExecutor(
            ObjectMapper objectMapper,
            EntryNodeExecutor entryNodeExecutor,
            ConnectorNodeExecutor connectorNodeExecutor,
            DataProcessorExecutor dataProcessorExecutor,
            ExitNodeExecutor exitNodeExecutor) {

        this.objectMapper = objectMapper;
        this.executorMap = new HashMap<>();
        executorMap.put("entry", entryNodeExecutor);
        executorMap.put("connector", connectorNodeExecutor);
        executorMap.put("data_processor", dataProcessorExecutor);
        executorMap.put("exit", exitNodeExecutor);
    }

    /**
     * 执行连接流
     *
     * @param context                 执行上下文
     * @param orchestrationConfigJson 编排配置JSON字符串 (v5.5 React Flow 格式)
     * @return Mono&lt;ExecutionResult&gt; 完整执行结果
     */
    public Mono<ExecutionResult> execute(ExecutionContext context, String orchestrationConfigJson) {
        long startTime = System.currentTimeMillis();

        return Mono.fromCallable(() -> objectMapper.readTree(orchestrationConfigJson))
                .flatMap(config -> {
                    JsonNode nodes = config.get("nodes");
                    JsonNode edges = config.get("edges");

                    if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
                        return Mono.just(buildErrorResult(context, null, startTime));
                    }

                    // 构建拓扑顺序: 从 entry 节点开始, 按 edges 确定顺序
                    List<JsonNode> orderedNodes = topologicalSort(nodes, edges);
                    log.info("Flow execution start: nodeCount={}, edgeCount={}", orderedNodes.size(), edges != null ? edges.size() : 0);

                    // 使用 reduce 累积执行: 从起始节点开始, 按顺序串联
                    NodeOutput startMarker = new NodeOutput("__start__", "__start__",
                            new HashMap<>(), new HashMap<>());
                    return Flux.fromIterable(orderedNodes)
                            .reduce(Mono.just(startMarker), (prevMono, nodeConfig) ->
                                    prevMono.flatMap(prevOutput -> executeNode(context, prevOutput, nodeConfig, startTime)))
                            .flatMap(mono -> mono) // 展平 Mono<Mono<NodeOutput>> → Mono<NodeOutput>
                            .flatMap(lastOutput -> buildResult(context, orderedNodes, lastOutput, startTime, edges));
                })
                .onErrorResume(e -> {
                    log.error("Flow execution error: {}", e.getMessage(), e);
                    return Mono.just(buildErrorResult(context, e.getMessage(), startTime));
                });
    }

    /**
     * 执行单个节点
     */
    private Mono<NodeOutput> executeNode(ExecutionContext context, NodeOutput prevOutput,
                                          JsonNode nodeConfig, long startTime) {
        // 将上一个节点输出写入上下文 (跳过起始标记)
        if (!"__start__".equals(prevOutput.getNodeId())) {
            storeNodeOutputToContext(context, prevOutput);
        }

        String nodeType = nodeConfig.get("type").asText();
        NodeExecutor executor = executorMap.get(nodeType);
        if (executor == null) {
            log.warn("Unknown node type: {}, skipping", nodeType);
            return Mono.just(new NodeOutput(
                    nodeConfig.get("id").asText(), nodeType,
                    new HashMap<>(), new HashMap<>()));
        }

        long nodeStart = System.currentTimeMillis();
        // v5.5: 传递完整 config (含 data 字段), executor 自行提取 data.xxx
        Map<String, Object> configMap = objectMapper.convertValue(nodeConfig, Map.class);
        log.info("Executing node: id={}, type={}", nodeConfig.get("id"), nodeConfig.get("type"));
                            log.info("Executing node: id={}, type={}", nodeConfig.get("id"), nodeConfig.get("type"));
                    return executor.execute(context, configMap)
                .doOnNext(output -> {
                    output.setDurationMs(System.currentTimeMillis() - nodeStart);
                    log.info("Node {} executed: status={}, duration={}ms",
                            output.getNodeId(), output.getStatus(), output.getDurationMs());
                });
    }

    /**
     * 将 NodeOutput 转换为 NodeContext 并存入上下文
     */
    @SuppressWarnings("unchecked")
    private void storeNodeOutputToContext(ExecutionContext context, NodeOutput output) {
        NodeContext ctx = new NodeContext();
        ctx.setNodeId(output.getNodeId());
        ctx.setNodeType(output.getNodeType());
        ctx.setInput(output.getInput());
        ctx.setOutput(output.getOutput());
        ctx.setStatus(output.getStatus());
        ctx.setDurationMs(output.getDurationMs());
        ctx.setErrorInfo(output.getErrorInfo());
        context.setNodeContext(ctx);
    }

    /**
     * 构建单个步骤详情
     */
    private ExecutionResult.StepDetail buildStepDetail(JsonNode node, ExecutionContext context) {
        String nodeId = node.get("id").asText();
        String nodeType = node.get("type").asText();
        NodeContext nodeCtx = context.getNodeContext(nodeId);

        ExecutionResult.StepDetail step = new ExecutionResult.StepDetail();
        step.setNodeId(nodeId);
        step.setNodeType(nodeType);

        // 读取标签
        JsonNode data = node.get("data");
        if (data != null) {
            JsonNode labelCn = data.get("labelCn");
            JsonNode labelEn = data.get("labelEn");
            step.setLabelCn(labelCn != null ? labelCn.asText() : null);
            step.setLabelEn(labelEn != null ? labelEn.asText() : null);
        }
        // 兼容旧格式
        if (step.getLabelCn() == null && node.get("labelCn") != null) {
            step.setLabelCn(node.get("labelCn").asText());
        }
        if (step.getLabelEn() == null && node.get("labelEn") != null) {
            step.setLabelEn(node.get("labelEn").asText());
        }

        // 填充运行时数据
        if (nodeCtx != null) {
            applyNodeContextToStep(step, nodeCtx);
        } else {
            applyFallbackOutputToStep(step, context.getNodeOutput(nodeId));
        }
        return step;
    }

    private void applyNodeContextToStep(ExecutionResult.StepDetail step, NodeContext nodeCtx) {
        step.setInputData(nodeCtx.getInput());
        step.setOutputData(nodeCtx.getOutput());
        step.setStatus(nodeCtx.getStatus());
        step.setDurationMs(nodeCtx.getDurationMs());
        step.setErrorInfo(nodeCtx.getErrorInfo());
    }

    @SuppressWarnings("unchecked")
    private void applyFallbackOutputToStep(ExecutionResult.StepDetail step, Map<String, Object> nodeOutput) {
        step.setOutputData(nodeOutput);
        String nodeStatus = "success";
        if (nodeOutput != null && nodeOutput.containsKey("__status")) {
            nodeStatus = (String) nodeOutput.get("__status");
        }
        step.setStatus(nodeStatus);
        if (nodeOutput != null && nodeOutput.containsKey("__input")) {
            step.setInputData((Map<String, Object>) nodeOutput.get("__input"));
        }
    }

    /**
     * 构建执行结果
     */
    @SuppressWarnings("unchecked")
    private Mono<ExecutionResult> buildResult(ExecutionContext context, List<JsonNode> orderedNodes,
                                               NodeOutput lastOutput, long startTime, JsonNode edges) {
        // 最后一个节点的输出写入上下文
        if (!"__start__".equals(lastOutput.getNodeId())) {
            storeNodeOutputToContext(context, lastOutput);
        }

        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(context.getExecutionId());
        result.setFlowId(context.getFlowId());
        result.setTest(context.isTest());
        result.setTotalDurationMs(System.currentTimeMillis() - startTime);

        boolean anyFailed = false;

        // 收集所有步骤详情
        for (JsonNode node : orderedNodes) {
            ExecutionResult.StepDetail step = buildStepDetail(node, context);

            // 检查失败状态
            if ("failed".equals(step.getStatus()) || "timeout".equals(step.getStatus())) {
                anyFailed = true;
            }

            result.addStep(step);
        }

        // 设置整体状态
        result.setStatus(anyFailed ? "failed" : "success");
        log.info("Flow execution complete: status={}, totalDurationMs={}", result.getStatus(), result.getTotalDurationMs());

        // resultData 取最后一个输出 (不含 __status/__input/__error 等元字段)
        if (lastOutput != null) {
            Map<String, Object> cleanOutput = new HashMap<>();
            Map<String, Object> outputData = lastOutput.getOutput();
            if (outputData != null) {
                cleanOutput.putAll(outputData);
                cleanOutput.remove("__status");
                cleanOutput.remove("__input");
                cleanOutput.remove("__error");
            }
            result.setResultData(cleanOutput);
        }


        return Mono.just(result);
    }

    /**
     * 拓扑排序: 按 edges 确定节点执行顺序
     * v5.5: 使用 {@code source}/{@code target} 替代 {@code sourceNodeId}/{@code targetNodeId}
     */
    private List<JsonNode> topologicalSort(JsonNode nodes, JsonNode edges) {
        Map<String, JsonNode> nodeMap = new LinkedHashMap<>();
        for (JsonNode node : nodes) {
            nodeMap.put(node.get("id").asText(), node);
        }

        List<JsonNode> ordered = new ArrayList<>();

        // 找 entry 节点
        JsonNode entryNode = null;
        for (JsonNode node : nodes) {
            if ("entry".equals(node.get("type").asText())) {
                entryNode = node;
                break;
            }
        }

        if (entryNode == null) {
            nodes.forEach(ordered::add);
            return ordered;
        }

        ordered.add(entryNode);

        if (edges == null || !edges.isArray()) {
            return ordered;
        }

        Set<String> visited = new HashSet<>();
        visited.add(entryNode.get("id").asText());

        // v5.5: 使用 source/target 替代 sourceNodeId/targetNodeId
        Map<String, String> edgeMap = new HashMap<>();
        for (JsonNode edge : edges) {
            String source = getEdgeField(edge, "source", "sourceNodeId");
            String target = getEdgeField(edge, "target", "targetNodeId");
            if (source != null && target != null) {
                edgeMap.put(source, target);
            }
        }

        String currentId = entryNode.get("id").asText();
        while (edgeMap.containsKey(currentId)) {
            String nextId = edgeMap.get(currentId);
            if (visited.contains(nextId)) break;
            visited.add(nextId);
            JsonNode nextNode = nodeMap.get(nextId);
            if (nextNode != null) {
                ordered.add(nextNode);
            }
            currentId = nextId;
        }

        // 添加尚未加入的节点
        for (JsonNode node : nodes) {
            if (!visited.contains(node.get("id").asText())) {
                ordered.add(node);
            }
        }

        return ordered;
    }

    /**
     * 兼容读取 edge 字段: 先读 v5.5 新字段名, 再读旧字段名
     */
    private String getEdgeField(JsonNode edge, String newField, String oldField) {
        JsonNode val = edge.get(newField);
        if (val != null) return val.asText();
        val = edge.get(oldField);
        if (val != null) return val.asText();
        return null;
    }

    private ExecutionResult buildErrorResult(ExecutionContext context, String errorMessage, long startTime) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(context.getExecutionId());
        result.setFlowId(context.getFlowId());
        result.setStatus("failed");

        String msg = (errorMessage != null) ? errorMessage : "Orchestration config has no nodes";
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("code", "6001");
        errorInfo.put("message", msg);
        errorInfo.put("messageEn", msg);
        errorInfo.put("messageZh", msg);
        result.setErrorInfo(errorInfo);

        result.setTotalDurationMs(System.currentTimeMillis() - startTime);
        result.setTest(context.isTest());
        return result;
    }
}