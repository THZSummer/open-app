package com.xxx.it.works.wecode.v2.modules.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
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
 * 从入口节点开始, 按 edges 拓扑顺序 flatMap 串联各节点 Mono<NodeOutput>
 * 对调用方呈现为同步请求-响应语义
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
     * @param orchestrationConfigJson 编排配置JSON字符串
     * @return Mono<ExecutionResult> 完整执行结果
     */
    public Mono<ExecutionResult> execute(ExecutionContext context, String orchestrationConfigJson) {
        long startTime = System.currentTimeMillis();

        return Mono.fromCallable(() -> objectMapper.readTree(orchestrationConfigJson))
                .flatMap(config -> {
                    JsonNode nodes = config.get("nodes");
                    JsonNode edges = config.get("edges");

                    if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
                        return Mono.just(buildErrorResult(context, "编排配置无节点", startTime));
                    }

                    // 构建拓扑顺序: 从 entry 节点开始, 按 edges 确定顺序
                    List<JsonNode> orderedNodes = topologicalSort(nodes, edges);

                    // 使用 reduce 累积执行: 从起始节点开始, 按顺序串联
                    NodeOutput startMarker = new NodeOutput("__start__", "__start__", new HashMap<>());
                    return Flux.fromIterable(orderedNodes)
                            .reduce(Mono.just(startMarker), (prevMono, nodeConfig) ->
                                    prevMono.flatMap(prevOutput -> executeNode(context, prevOutput, nodeConfig, startTime)))
                            .flatMap(mono -> mono) // 展平 Mono<Mono<NodeOutput>> → Mono<NodeOutput>
                            .flatMap(lastOutput -> buildResult(context, orderedNodes, lastOutput, startTime));
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
            context.setNodeOutput(prevOutput.getNodeId(), prevOutput.getOutputData());
        }

        String nodeType = nodeConfig.get("type").asText();
        NodeExecutor executor = executorMap.get(nodeType);
        if (executor == null) {
            log.warn("Unknown node type: {}, skipping", nodeType);
            return Mono.just(new NodeOutput(
                    nodeConfig.get("id").asText(), nodeType, new HashMap<>()));
        }

        long nodeStart = System.currentTimeMillis();
        // 将 JsonNode 转为 Map 传递给执行器
        Map<String, Object> configMap = objectMapper.convertValue(nodeConfig, Map.class);
        return executor.execute(context, configMap)
                .doOnNext(output -> {
                    output.setDurationMs(System.currentTimeMillis() - nodeStart);
                    log.debug("Node {} executed: status={}, duration={}ms",
                            output.getNodeId(), output.getStatus(), output.getDurationMs());
                });
    }

    /**
     * 构建执行结果
     */
    private Mono<ExecutionResult> buildResult(ExecutionContext context, List<JsonNode> orderedNodes,
                                               NodeOutput lastOutput, long startTime) {
        // 最后一个节点的输出写入上下文
        if (!"__start__".equals(lastOutput.getNodeId())) {
            context.setNodeOutput(lastOutput.getNodeId(), lastOutput.getOutputData());
        }

        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(context.getExecutionId());
        result.setFlowId(context.getFlowId());
        result.setTest(context.isTest());
        result.setTotalDurationMs(System.currentTimeMillis() - startTime);

        boolean anyFailed = false;

        // 收集所有步骤详情
        for (JsonNode node : orderedNodes) {
            String nodeId = node.get("id").asText();
            String nodeType = node.get("type").asText();
            Map<String, Object> nodeOutput = context.getNodeOutput(nodeId);

            ExecutionResult.StepDetail step = new ExecutionResult.StepDetail();
            step.setNodeId(nodeId);
            step.setNodeType(nodeType);

            JsonNode labelCn = node.get("labelCn");
            JsonNode labelEn = node.get("labelEn");
            if (labelCn != null) step.setNodeLabelCn(labelCn.asText());
            if (labelEn != null) step.setNodeLabelEn(labelEn.asText());

            step.setOutputData(nodeOutput);

            // 检查节点执行状态
            String nodeStatus = "success";
            if (nodeOutput != null && nodeOutput.containsKey("__status")) {
                nodeStatus = (String) nodeOutput.get("__status");
                if ("failed".equals(nodeStatus) || "timeout".equals(nodeStatus)) {
                    anyFailed = true;
                    step.setErrorMessage((String) nodeOutput.get("__error"));
                }
            }
            step.setStatus(nodeStatus);

            // 获取原始 input
            if (nodeOutput != null && nodeOutput.containsKey("__input")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> input = (Map<String, Object>) nodeOutput.get("__input");
                step.setInputData(input);
            }

            result.addStep(step);
        }

        // 设置整体状态
        result.setStatus(anyFailed ? "failed" : "success");

        // resultData 取最后一个输出 (不含 __status/__input 等元字段)
        if (lastOutput != null && lastOutput.getOutputData() != null) {
            Map<String, Object> cleanOutput = new HashMap<>(lastOutput.getOutputData());
            cleanOutput.remove("__status");
            cleanOutput.remove("__input");
            cleanOutput.remove("__error");
            result.setResultData(cleanOutput);
        }

        // 清除凭证
        context.clearCredentials();

        return Mono.just(result);
    }

    /**
     * 拓扑排序: 按 edges 确定节点执行顺序
     * MVP 线性编排, 按 edges 顺序 flatMap 串联
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

        Map<String, String> edgeMap = new HashMap<>();
        for (JsonNode edge : edges) {
            edgeMap.put(edge.get("sourceNodeId").asText(), edge.get("targetNodeId").asText());
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

    private ExecutionResult buildErrorResult(ExecutionContext context, String errorMessage, long startTime) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(context.getExecutionId());
        result.setFlowId(context.getFlowId());
        result.setStatus("failed");
        result.setErrorMessage(errorMessage);
        result.setTotalDurationMs(System.currentTimeMillis() - startTime);
        result.setTest(context.isTest());
        return result;
    }
}