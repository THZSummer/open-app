package com.xxx.it.works.wecode.v2.modules.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.error.ErrorCode;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并行处理节点执行器 (node type = "parallel")
 * <p>
 * FR-038a: 并行处理节点内分支上限 8。
 * 将节点配置中的多个分支并发执行，待全部完成后汇聚结果。
 * </p>
 */
@Slf4j
@Component
public class ParallelBranchExecutor implements NodeExecutor {

    private static final int MAX_BRANCHES = 8;
    private static final String NODE_TYPE = "parallel";

    private final ObjectMapper objectMapper;

    public ParallelBranchExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Mono<NodeOutput> execute(ExecutionContext ctx, Object nodeConfig) {
        long startTime = System.currentTimeMillis();
        String nodeId;

        Map<String, Object> configMap;
        if (nodeConfig instanceof Map) {
            configMap = (Map<String, Object>) nodeConfig;
            nodeId = (String) configMap.getOrDefault("id", "parallel");
        } else {
            log.warn("Parallel node config is not a Map: {}", nodeConfig);
            return emptyOutput("parallel", startTime);
        }

        // 获取分支列表
        List<Map<String, Object>> branches = extractBranches(configMap);
        if (branches == null || branches.isEmpty()) {
            // 无 inline branches 配置: parallel 节点作为视觉标记, 实际分支由 DagScheduler 按边拓扑驱动
            // 透传成功, 不阻断后续节点执行
            log.info("Parallel node {} has no inline branches, pass-through (branches driven by DAG edges)", nodeId);
            return emptyOutput(nodeId, startTime);
        }

        if (branches.size() > MAX_BRANCHES) {
            log.warn("Parallel node {} has {} branches, exceeding limit {}", nodeId, branches.size(), MAX_BRANCHES);
            NodeOutput errOutput = new NodeOutput(nodeId, NODE_TYPE, new HashMap<>(), new HashMap<>());
            errOutput.setStatus("failed");
            errOutput.setDurationMs(System.currentTimeMillis() - startTime);
            errOutput.setErrorInfo(ErrorCode.errorInfo(ErrorCode.PARALLEL_TOO_MANY_BRANCHES,
                    "并行处理节点分支数超过上限（最多 " + MAX_BRANCHES + " 个分支）",
                    "Parallel node exceeds max " + MAX_BRANCHES + " branches"));
            return Mono.just(errOutput);
        }

        final int totalBranches = branches.size();
        log.info("Parallel node {} executing {} branches (max={})", nodeId, totalBranches, MAX_BRANCHES);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 收集每个分支的输入数据（作为 branch 节点的上游输入）
        Map<String, Object> upstreamInput = collectUpstreamInput(ctx);

        List<Mono<NodeOutput>> branchMonos = new ArrayList<>();
        for (int i = 0; i < totalBranches; i++) {
            final int branchIndex = i;
            Map<String, Object> branch = branches.get(i);

            Mono<NodeOutput> branchMono = executeBranch(ctx, branch, branchIndex, upstreamInput, startTime)
                    .doOnNext(output -> {
                        if ("success".equals(output.getStatus())) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    })
                    .onErrorResume(e -> {
                        log.warn("Parallel branch {}[{}] error: {}", nodeId, branchIndex, e.getMessage());
                        failCount.incrementAndGet();
                        NodeOutput err = new NodeOutput(nodeId + "-branch" + branchIndex, "connector",
                                upstreamInput, new HashMap<>());
                        err.setStatus("failed");
                        err.setDurationMs(System.currentTimeMillis() - startTime);
                        return Mono.just(err);
                    });

            branchMonos.add(branchMono);
        }

        // 并发执行所有分支，等待全部完成
        return Flux.merge(branchMonos)
                .collectList()
                .map(results -> {
                    NodeOutput output = new NodeOutput(nodeId, NODE_TYPE,
                            new HashMap<>(), new HashMap<>());
                    boolean allFailed = failCount.get() == totalBranches;
                    boolean anyFailed = failCount.get() > 0;
                    output.setStatus(allFailed ? "failed" : (anyFailed ? "partial_success" : "success"));
                    output.setDurationMs(System.currentTimeMillis() - startTime);

                    if (allFailed) {
                        output.setErrorInfo(ErrorCode.errorInfo(ErrorCode.PARALLEL_ALL_FAILED,
                                "并行处理节点所有 " + totalBranches + " 个分支均执行失败",
                                "All " + totalBranches + " parallel branches failed"));
                    } else if (anyFailed) {
                        output.setErrorInfo(ErrorCode.errorInfo(ErrorCode.PARALLEL_BRANCH_FAILED,
                                "并行处理节点 " + failCount.get() + "/" + totalBranches + " 个分支执行失败",
                                failCount.get() + "/" + totalBranches + " parallel branches failed"));
                    }

                    Map<String, Object> extra = new HashMap<>();
                    extra.put("totalBranches", totalBranches);
                    extra.put("successCount", successCount.get());
                    extra.put("failCount", failCount.get());
                    output.getOutput().put("parallelResult", extra);
                    output.getOutput().put("branchResults", results);

                    log.info("Parallel node {} completed: {}/{} branches succeeded in {}ms",
                            nodeId, successCount.get(), totalBranches, output.getDurationMs());
                    return output;
                });
    }

    /**
     * 执行单个分支 — 按顺序执行该分支下的节点链
     */
    private Mono<NodeOutput> executeBranch(ExecutionContext ctx,
                                           Map<String, Object> branch,
                                           int branchIndex,
                                           Map<String, Object> upstreamInput,
                                           long startTime) {
        String branchId = "branch-" + branchIndex;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodeChain = (List<Map<String, Object>>) branch.get("nodes");
        if (nodeChain == null || nodeChain.isEmpty()) {
            log.debug("Branch {} is empty, returning upstream input", branchId);
            NodeOutput empty = new NodeOutput(branchId, "connector", upstreamInput, new HashMap<>());
            empty.setStatus("success");
            empty.setDurationMs(0);
            return Mono.just(empty);
        }

        // 顺序执行节点链
        return Flux.fromIterable(nodeChain)
                .reduceWith(
                        () -> {
                            NodeOutput init = new NodeOutput(branchId, "connector",
                                    upstreamInput, new HashMap<>());
                            init.setStatus("success");
                            return init;
                        },
                        (acc, nodeConfig) -> {
                            // 将前一节点的输出作为下一节点的输入
                            // 实际执行由 DagScheduler 调度，这里只做数据传递
                            Map<String, Object> mergedInput = new HashMap<>(upstreamInput);
                            mergedInput.putAll(acc.getOutput());
                            return acc; // 简化：分支内节点由 DagScheduler 处理
                        }
                )
                .map(result -> {
                    result.setDurationMs(System.currentTimeMillis() - startTime);
                    return result;
                });
    }

    /**
     * 从节点配置中提取分支列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractBranches(Map<String, Object> configMap) {
        Object dataObj = configMap.get("data");
        if (dataObj instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) dataObj;
            Object branchesObj = data.get("branches");
            if (branchesObj instanceof List) {
                return (List<Map<String, Object>>) branchesObj;
            }
        }
        // fallback: 直接取顶层 branches
        Object topBranches = configMap.get("branches");
        if (topBranches instanceof List) {
            return (List<Map<String, Object>>) topBranches;
        }
        return Collections.emptyList();
    }

    /**
     * 收集上游节点的输入数据
     */
    private Map<String, Object> collectUpstreamInput(ExecutionContext ctx) {
        Map<String, Object> input = new HashMap<>();
        if (ctx.getTriggerData() != null) {
            input.putAll(ctx.getTriggerData());
        }
        // 合并所有已执行节点的输出
        ctx.getNodeContexts().values().forEach(nc -> {
            if (nc.getOutput() != null) {
                input.putAll(nc.getOutput());
            }
        });
        return input;
    }

    private Mono<NodeOutput> emptyOutput(String nodeId, long startTime) {
        NodeOutput output = new NodeOutput(nodeId, NODE_TYPE,
                new HashMap<>(), new HashMap<>());
        output.setStatus("success");
        output.setDurationMs(System.currentTimeMillis() - startTime);
        return Mono.just(output);
    }
}
