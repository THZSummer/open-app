package com.xxx.it.works.wecode.v2.modules.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ResolvedFlowConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAG 调度器
 * <p>
 * 基于 Reactor 响应式编程, 按边关系遍历 DAG 节点,
 * 根据节点类型分发到对应执行器.
 * <p>
 * 支持:
 * <ul>
 *   <li>串行边: flatMap 顺序链式执行</li>
 *   <li>并行边 (edge.data.connectionMode=parallel): Flux.merge() 并发执行</li>
 *   <li>节点超时: min(node config timeout, 30s)</li>
 *   <li>单分支失败不影响其他并行分支</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class DagScheduler {

    private final ObjectMapper objectMapper;
    private final Map<String, NodeExecutor> executorMap;

    /** script 节点执行器 (TASK-011 实现后注入, 当前可为 null) */
    @Autowired(required = false)
    private NodeExecutor scriptNodeExecutor;

    public DagScheduler(ObjectMapper objectMapper,
                         List<NodeExecutor> nodeExecutors) {
        this.objectMapper = objectMapper;
        this.executorMap = new HashMap<>();
        // 自动注册所有 NodeExecutor Bean
        for (NodeExecutor executor : nodeExecutors) {
            executorMap.put(executor.getNodeType(), executor);
            log.info("Registered NodeExecutor: type={}", executor.getNodeType());
        }
    }

    /**
     * 调度执行 DAG
     *
     * @param resolved 已解析的连接流配置
     * @param ctx      执行上下文
     * @return Mono<ExecutionContext> 填充了所有节点执行结果的上下文
     */
    public Mono<ExecutionContext> schedule(ResolvedFlowConfig resolved, ExecutionContext ctx) {
        long startTime = System.currentTimeMillis();
        String orchestrationConfig = resolved.getFlowVersion().getOrchestrationConfig();

        return Mono.fromCallable(() -> objectMapper.readTree(orchestrationConfig))
                .flatMap(config -> {
                    JsonNode nodes = config.get("nodes");
                    JsonNode edges = config.get("edges");

                    if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
                        log.warn("Orchestration config has no nodes: flowId={}", ctx.getFlowId());
                        return Mono.just(ctx);
                    }

                    // 构建节点映射
                    Map<String, JsonNode> nodeMap = buildNodeMap(nodes);

                    // 构建 DAG 邻接表: source → list of targets
                    Map<String, List<String>> adjacencyMap = buildAdjacencyMap(edges, nodeMap);

                    // 查找入口节点 (trigger)
                    String entryNodeId = findEntryNode(nodes);
                    if (entryNodeId == null) {
                        log.warn("No trigger node found, executing all nodes sequentially");
                        return executeSequentially(nodes, ctx, startTime);
                    }

                    // 按 DAG 拓扑执行
                    return executeDag(entryNodeId, nodeMap, adjacencyMap, ctx, startTime)
                            .thenReturn(ctx);
                });
    }

    /**
     * 构建节点 ID → JsonNode 映射
     */
    private Map<String, JsonNode> buildNodeMap(JsonNode nodes) {
        Map<String, JsonNode> map = new LinkedHashMap<>();
        for (JsonNode node : nodes) {
            String id = node.get("id").asText();
            map.put(id, node);
        }
        return map;
    }

    /**
     * 构建 DAG 邻接表: source node ID → target node ID 列表
     * <p>
     * 解析 edge.data.connectionMode 判断串行/并行:
     * <ul>
     *   <li>"parallel" → 并行分支</li>
     *   <li>默认/其他 → 串行</li>
     * </ul>
     * </p>
     */
    private Map<String, List<String>> buildAdjacencyMap(JsonNode edges, Map<String, JsonNode> nodeMap) {
        Map<String, List<String>> adj = new LinkedHashMap<>();
        if (edges == null || !edges.isArray()) {
            return adj;
        }
        for (JsonNode edge : edges) {
            String source = getEdgeField(edge, "source", "sourceNodeId");
            String target = getEdgeField(edge, "target", "targetNodeId");
            if (source != null && target != null && nodeMap.containsKey(target)) {
                adj.computeIfAbsent(source, k -> new ArrayList<>()).add(target);
            }
        }
        return adj;
    }

    /**
     * 按 DAG 递归执行 (深度优先, 支持并行分支)
     */
    private Mono<Void> executeDag(String nodeId, Map<String, JsonNode> nodeMap,
                                   Map<String, List<String>> adjacencyMap,
                                   ExecutionContext ctx, long startTime) {
        JsonNode nodeConfig = nodeMap.get(nodeId);
        if (nodeConfig == null) {
            return Mono.empty();
        }

        // 执行当前节点
        return executeNode(ctx, nodeConfig, startTime)
                .flatMap(output -> {
                    // 将节点输出写入上下文
                    storeNodeOutputToContext(ctx, output);
                    // 获取下游节点
                    List<String> targets = adjacencyMap.getOrDefault(nodeId, List.of());
                    if (targets.isEmpty()) {
                        return Mono.empty();
                    }
                    if (targets.size() == 1) {
                        // 单一下游: 串行执行
                        return executeDag(targets.get(0), nodeMap, adjacencyMap, ctx, startTime);
                    } else {
                        // 多个下游: 判断是否并行分支
                        return executeParallelBranches(nodeId, targets, nodeMap, adjacencyMap, ctx, startTime);
                    }
                })
                .onErrorResume(e -> {
                    log.error("DAG execution error at node {}: {}", nodeId, e.getMessage(), e);
                    return Mono.empty(); // 不中断其他分支
                });
    }

    /**
     * 执行并行分支
     */
    private Mono<Void> executeParallelBranches(String sourceNodeId, List<String> targetIds,
                                                Map<String, JsonNode> nodeMap,
                                                Map<String, List<String>> adjacencyMap,
                                                ExecutionContext ctx, long startTime) {
        log.info("Executing {} parallel branches from node {}", targetIds.size(), sourceNodeId);

        List<Mono<Void>> branches = new ArrayList<>();
        for (String targetId : targetIds) {
            // 每个分支独立执行, 失败不影响其他分支
            Mono<Void> branch = executeDag(targetId, nodeMap, adjacencyMap, ctx, startTime)
                    .subscribeOn(Schedulers.parallel())
                    .onErrorResume(e -> {
                        log.warn("Parallel branch {} failed: {}", targetId, e.getMessage());
                        return Mono.empty();
                    });
            branches.add(branch);
        }

        if (branches.isEmpty()) {
            return Mono.empty();
        }
        if (branches.size() == 1) {
            return branches.get(0);
        }

        // 等待所有分支完成, publishOn 避免 parallel scheduler 线程争用
        return Flux.merge(branches).then().publishOn(Schedulers.boundedElastic());
    }

    /**
     * 顺序执行所有节点 (无 trigger 时的 fallback)
     */
    private Mono<ExecutionContext> executeSequentially(JsonNode nodes, ExecutionContext ctx, long startTime) {
        return Flux.fromIterable(toList(nodes))
                .concatMap(node -> executeNode(ctx, node, startTime)
                        .doOnNext(output -> storeNodeOutputToContext(ctx, output))
                        .onErrorResume(e -> {
                            log.warn("Node execution error: nodeId={}, error={}",
                                    node.get("id").asText(), e.getMessage());
                            return Mono.empty();
                        }))
                .then(Mono.just(ctx));
    }

    /**
     * 执行单个节点
     */
    private Mono<NodeOutput> executeNode(ExecutionContext ctx, JsonNode nodeConfig, long startTime) {
        String nodeId = nodeConfig.get("id").asText();
        String nodeType = nodeConfig.get("type").asText();
        long nodeStart = System.currentTimeMillis();

        NodeExecutor executor = resolveExecutor(nodeType);
        if (executor == null) {
            log.warn("No executor for node type: {}, skipping node {}", nodeType, nodeId);
            NodeOutput skipOutput = new NodeOutput(nodeId, nodeType,
                    new HashMap<>(), new HashMap<>());
            skipOutput.setStatus("skipped");
            skipOutput.setDurationMs(0);
            return Mono.just(skipOutput);
        }

        // 提取节点配置 Map
        Map<String, Object> configMap = objectMapper.convertValue(nodeConfig, Map.class);

        log.info("DAG executing node: id={}, type={}", nodeId, nodeType);

        // 获取节点超时时间
        Duration timeout = resolveNodeTimeout(nodeConfig);

        return executor.execute(ctx, configMap)
                .doOnNext(output -> {
                    output.setDurationMs(System.currentTimeMillis() - nodeStart);
                    log.info("DAG node completed: id={}, type={}, status={}, duration={}ms",
                            output.getNodeId(), output.getNodeType(),
                            output.getStatus(), output.getDurationMs());
                })
                .timeout(timeout)
                .onErrorResume(e -> {
                    NodeOutput errorOutput = new NodeOutput(nodeId, nodeType,
                            new HashMap<>(), new HashMap<>());
                    errorOutput.setStatus("timeout");
                    errorOutput.setDurationMs(System.currentTimeMillis() - nodeStart);
                    Map<String, Object> errorInfo = new HashMap<>();
                    errorInfo.put("code", "6002");
                    errorInfo.put("message", "Node execution timeout or error: " + e.getMessage());
                    errorInfo.put("messageEn", "Node execution timeout or error: " + e.getMessage());
                    errorInfo.put("messageZh", "节点执行超时或错误: " + e.getMessage());
                    errorOutput.setErrorInfo(errorInfo);
                    return Mono.just(errorOutput);
                });
    }

    /**
     * 根据节点类型获取执行器
     */
    private NodeExecutor resolveExecutor(String nodeType) {
        // 优先从注册的执行器中查找
        NodeExecutor executor = executorMap.get(nodeType);
        if (executor != null) {
            return executor;
        }
        // script 节点特殊处理
        if ("script".equals(nodeType) && scriptNodeExecutor != null) {
            return scriptNodeExecutor;
        }
        if ("script".equals(nodeType)) {
            log.warn("ScriptNodeExecutor not available, skipping script node (TASK-011 not yet implemented)");
        }
        return null;
    }

    /**
     * 解析节点超时: min(node.data.timeoutMs, 30s), 默认 30s
     */
    private Duration resolveNodeTimeout(JsonNode nodeConfig) {
        long defaultTimeoutMs = 30000;
        JsonNode data = nodeConfig.get("data");
        if (data != null && data.has("timeoutMs")) {
            JsonNode timeoutNode = data.get("timeoutMs");
            if (timeoutNode.isNumber()) {
                long nodeTimeoutMs = timeoutNode.asLong();
                return Duration.ofMillis(Math.min(nodeTimeoutMs, defaultTimeoutMs));
            }
        }
        return Duration.ofMillis(defaultTimeoutMs);
    }

    /**
     * 将 NodeOutput 转换为 NodeContext 并存入上下文
     */
    @SuppressWarnings("unchecked")
    private void storeNodeOutputToContext(ExecutionContext ctx, NodeOutput output) {
        NodeContext nodeCtx = new NodeContext();
        nodeCtx.setNodeId(output.getNodeId());
        nodeCtx.setNodeType(output.getNodeType());
        nodeCtx.setInput(output.getInput());
        nodeCtx.setOutput(output.getOutput());
        nodeCtx.setStatus(output.getStatus());
        nodeCtx.setDurationMs(output.getDurationMs());
        nodeCtx.setErrorInfo(output.getErrorInfo());
        ctx.setNodeContext(nodeCtx);
    }

    /**
     * 查找入口节点 (类型为 trigger)
     */
    private String findEntryNode(JsonNode nodes) {
        for (JsonNode node : nodes) {
            if ("trigger".equals(node.get("type").asText())) {
                return node.get("id").asText();
            }
        }
        return null;
    }

    /**
     * 兼容读取 edge 字段: v5.5 新字段名 → 旧字段名 fallback
     */
    private String getEdgeField(JsonNode edge, String newField, String oldField) {
        JsonNode val = edge.get(newField);
        if (val != null) return val.asText();
        val = edge.get(oldField);
        if (val != null) return val.asText();
        return null;
    }

    /**
     * JsonNode Array → List<JsonNode>
     */
    private List<JsonNode> toList(JsonNode arrayNode) {
        List<JsonNode> list = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                list.add(node);
            }
        }
        return list;
    }
}
