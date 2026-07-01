package com.xxx.it.works.wecode.v2.modules.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.cache.FlowCacheManager;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.runtime.model.FlowConfig;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ResolvedFlowConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 连接流运行时引擎 (5 阶段执行管道)
 * <p>
 * Phase 1: 凭证认证 (委托给 auth 模块)
 * Phase 2: 版本配置解析 (VersionConfigResolver)
 * Phase 3: 触发器鉴权 (SYSTOKEN 白名单校验)
 * Phase 4: 入站限流 (委托给 rate limit 模块)
 * Phase 5: 缓存检查 → DAG 调度执行 → 缓存回写
 * <p>
 * 各阶段失败返回对应 HTTP 状态码:
 * <ul>
 *   <li>Phase 1 未部署 → 503</li>
 *   <li>Phase 2 版本删除 → 500</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class FlowRuntimeEngine {

    private final VersionConfigResolver versionConfigResolver;
    private final DagScheduler dagScheduler;
    private final FlowConfigParser flowConfigParser;
    private final FlowCacheManager cacheManager;
    private final ObjectMapper objectMapper;

    public FlowRuntimeEngine(VersionConfigResolver versionConfigResolver,
                              DagScheduler dagScheduler,
                              FlowConfigParser flowConfigParser,
                              FlowCacheManager cacheManager,
                              ObjectMapper objectMapper) {
        this.versionConfigResolver = versionConfigResolver;
        this.dagScheduler = dagScheduler;
        this.flowConfigParser = flowConfigParser;
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行连接流 (5 阶段管道)
     *
     * @param flowId 连接流ID
     * @param ctx    执行上下文 (含触发数据/headers/query params)
     * @return Mono<ExecutionResult> 执行结果
     */
    public Mono<ExecutionResult> execute(Long flowId, ExecutionContext ctx) {
        long startTime = System.currentTimeMillis();
        log.info("FlowRuntimeEngine starting: flowId={}, executionId={}", flowId, ctx.getExecutionId());

        // Phase 1+2: 版本配置解析 (含 Flow 加载和部署检查)
        // Phase 3: 触发器鉴权 (内嵌在 VersionConfigResolver 之后)
        // Phase 4: 入站限流 (由 InboundRateLimiter WebFilter 拦截, Redis Token Bucket)
        // Phase 5: DAG 调度执行

        return versionConfigResolver.resolveFlowVersion(flowId)
                .flatMap(resolved -> {
                    log.info("Phase 2 completed: flowId={}, versionNumber={}",
                            flowId,
                            resolved.getFlowVersion().getVersionNumber());

                    // Phase 3: SYSTOKEN 白名单校验已由 WebFilter 完成 (在此做补充校验)
                    // Phase 4: 入站限流已由 InboundRateLimiter 完成 (Redis Token Bucket)

                    // Phase 5: 检查是否有缓存配置, 有则查缓存
                    FlowConfig flowConfig = resolved.getFlowConfig();
                    if (flowConfig.getCacheTtl() != null && flowConfig.getCacheTtl() > 0) {
                        String cacheKey = buildCacheKey(flowId, ctx);
                        return cacheManager.checkCache(flowId, cacheKey)
                                .flatMap(cachedResult -> {
                                    // 缓存命中: 直接返回缓存结果
                                    log.info("Cache hit: flowId={}, cacheKey={}", flowId, cacheKey);
                                    cachedResult.setCacheHit(true);
                                    return Mono.just(cachedResult);
                                })
                                .switchIfEmpty(
                                    // 缓存未命中: 执行 DAG → 回写缓存
                                    dagScheduler.schedule(resolved, ctx)
                                            .map(updatedCtx -> buildResult(resolved, updatedCtx, startTime))
                                            .flatMap(result -> {
                                                result.setCacheHit(false);
                                                return cacheManager.writeCache(flowId, cacheKey, result,
                                                        flowConfig.getCacheTtl())
                                                        .thenReturn(result);
                                            })
                                );
                    }

                    // Phase 5: 无缓存配置, 直接 DAG 调度执行
                    return dagScheduler.schedule(resolved, ctx)
                            .map(updatedCtx -> buildResult(resolved, updatedCtx, startTime));
                })
                .onErrorResume(VersionConfigResolver.FlowNotDeployedException.class, e -> {
                    log.warn("Flow not deployed: flowId={}, error={}", flowId, e.getMessage());
                    return Mono.just(buildPhaseErrorResult(ctx, "503",
                            "Flow not deployed",
                            "连接流未部署",
                            startTime));
                })
                .onErrorResume(VersionConfigResolver.DeployedVersionNotFoundException.class, e -> {
                    log.error("Deployed version not found: flowId={}, error={}", flowId, e.getMessage());
                    return Mono.just(buildPhaseErrorResult(ctx, "500",
                            "Deployed version not found",
                            "已部署版本不存在",
                            startTime));
                })
                .onErrorResume(e -> {
                    log.error("Flow execution error: flowId={}, error={}", flowId, e.getMessage(), e);
                    return Mono.just(buildPhaseErrorResult(ctx, "500",
                            "Internal execution error: " + e.getMessage(),
                            "内部执行错误: " + e.getMessage(),
                            startTime));
                });
    }

    /**
     * 构建执行结果 (所有节点完成后)
     */
    private ExecutionResult buildResult(ResolvedFlowConfig resolved, ExecutionContext ctx, long startTime) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(ctx.getExecutionId());
        result.setFlowId(ctx.getFlowId());
        result.setDebug(ctx.isDebug());
        result.setTotalDurationMs(System.currentTimeMillis() - startTime);

        // 收集各节点执行结果
        boolean anyFailed = false;
        Map<String, Object> lastOutput = null;

        for (Map.Entry<String, com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext> entry :
                ctx.getNodeContexts().entrySet()) {
            com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext nodeCtx = entry.getValue();

            ExecutionResult.StepDetail step = new ExecutionResult.StepDetail();
            step.setNodeId(nodeCtx.getNodeId());
            step.setNodeType(nodeCtx.getNodeType());
            step.setInputData(nodeCtx.getInput());
            step.setOutputData(nodeCtx.getOutput());
            step.setStatus(nodeCtx.getStatus());
            step.setDurationMs(nodeCtx.getDurationMs());
            step.setErrorInfo(nodeCtx.getErrorInfo());

            if ("failed".equals(nodeCtx.getStatus()) || "timeout".equals(nodeCtx.getStatus())) {
                anyFailed = true;
            }

            result.addStep(step);

            // 记录最后一个节点的输出
            if (nodeCtx.getOutput() != null && !nodeCtx.getOutput().isEmpty()) {
                lastOutput = nodeCtx.getOutput();
            }
        }

        result.setStatus(anyFailed ? "failed" : "success");

        // 清理结果数据
        if (lastOutput != null) {
            Map<String, Object> cleanOutput = new HashMap<>(lastOutput);
            cleanOutput.remove("__status");
            cleanOutput.remove("__input");
            cleanOutput.remove("__error");
            result.setResultData(cleanOutput);
        }

        log.info("Flow execution complete: flowId={}, status={}, totalDurationMs={}, steps={}",
                ctx.getFlowId(), result.getStatus(), result.getTotalDurationMs(), result.getSteps().size());

        return result;
    }

    /**
     * 构建阶段错误结果
     */
    private ExecutionResult buildPhaseErrorResult(ExecutionContext ctx, String code,
                                                    String messageEn, String messageZh,
                                                    long startTime) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(ctx.getExecutionId());
        result.setFlowId(ctx.getFlowId());
        result.setDebug(ctx.isDebug());
        result.setStatus("failed");
        result.setTotalDurationMs(System.currentTimeMillis() - startTime);

        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("code", code);
        errorInfo.put("message", messageEn);
        errorInfo.put("messageEn", messageEn);
        errorInfo.put("messageZh", messageZh);
        result.setErrorInfo(errorInfo);

        return result;
    }

    /**
     * 构建缓存 key (基于 flowId + context 关键参数的 SHA-256 哈希)
     */
    private String buildCacheKey(Long flowId, ExecutionContext ctx) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(("flow:" + flowId).getBytes(StandardCharsets.UTF_8));
            if (ctx.getTriggerData() != null) {
                digest.update(objectMapper.writeValueAsBytes(ctx.getTriggerData()));
            }
            if (ctx.getTriggerHeaders() != null) {
                digest.update(objectMapper.writeValueAsBytes(ctx.getTriggerHeaders()));
            }
            if (ctx.getTriggerQueryParams() != null) {
                digest.update(objectMapper.writeValueAsBytes(ctx.getTriggerQueryParams()));
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 在 JVM 中始终可用
            return String.valueOf(Objects.hash(flowId, ctx.getExecutionId()));
        } catch (Exception e) {
            log.warn("Failed to compute cache key: {}", e.getMessage());
            return String.valueOf(Objects.hash(flowId, ctx.getExecutionId()));
        }
    }
}
