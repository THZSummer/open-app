package com.xxx.it.works.wecode.v2.modules.flow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;

import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.common.IdGenerator;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import com.xxx.it.works.wecode.v2.modules.cache.FlowCacheManager;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionRecordService;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionStepService;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionStepService.StepLog;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.DagScheduler;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.expression.ExpressionResolver;
import com.xxx.it.works.wecode.v2.common.error.ErrorCode;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.runtime.model.FlowConfig;
import com.xxx.it.works.wecode.v2.modules.runtime.model.TransparentFlowResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.xxx.it.works.wecode.v2.common.annotation.StandardTodo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Duration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.Locale;

/**
 * HTTP 触发服务 (v5.8)
 * <p>
 * 处理外部系统的 HTTP 触发请求.
 * v5.8 变更:
 * <ul>
 *   <li>返回值从 {@code ExecutionResult} 信封模式改为 {@code TransparentFlowResponse} 透明穿透模式</li>
 *   <li>出口节点的 output.body 直接作为 HTTP 响应体 (裸数据)</li>
 *   <li>出口节点的 output.header 映射为用户自定义 HTTP 响应头</li>
 *   <li>平台元数据 (executionId/flowId/status/durationMs/code/message) 改为 X- 前缀 HTTP 响应头</li>
 *   <li>前置校验失败按异常类型返回不同 HTTP Status (401/404/403/500)</li>
 * </ul>
 * v5.7 变更:
 * <ul>
 *   <li>触发器节点上下文结构化: input = {@code {header: {...}, query: {...}, body: {...}}}</li>
 *   <li>校验 {@code data.type} 子类型 ({@code http} / {@code manual})</li>
 *   <li>完整校验 {@code data.inputContract} 的 header / query / body 三段契约</li>
 *   <li>捕获 URL query 参数进行校验和存储</li>
 *   <li>常量: 触发器不直接使用常量映射 (数据来自外部 HTTP 请求), 常量由下游节点 mapping 中的 {@code ${$.constant:xxx}} 处理</li>
 * </ul>
 * </p>
 */
@Service
public class FlowInvokeService {

    private static final Logger log = LoggerFactory.getLogger(FlowInvokeService.class);

    private static final String TRIGGER_NODE_ID = "node_trigger";

    private final ObjectMapper objectMapper;
    private final ReactiveSequentialExecutor executor;
    private final DagScheduler dagScheduler;
    private final OpFlowVersionReadRepository flowVersionReadRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final EntityCacheManager entityCacheManager;
    private final FlowCacheManager cacheManager;
    private final ExecutionRecordService executionRecordService;
    private final ExecutionStepService executionStepService;
    private final IdGenerator idGenerator;
    private final ConnectorApiPropertyService propertyService;

    public FlowInvokeService(
            ObjectMapper objectMapper,
            ReactiveSequentialExecutor executor,
            DagScheduler dagScheduler,
            OpFlowVersionReadRepository flowVersionReadRepository,
            ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            FlowCacheManager cacheManager,
            EntityCacheManager entityCacheManager,
            ExecutionRecordService executionRecordService,
            ExecutionStepService executionStepService,
            IdGenerator idGenerator,
            ConnectorApiPropertyService propertyService) {
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.dagScheduler = dagScheduler;
        this.flowVersionReadRepository = flowVersionReadRepository;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.cacheManager = cacheManager;
        this.entityCacheManager = entityCacheManager;
        this.executionRecordService = executionRecordService;
        this.executionStepService = executionStepService;
        this.idGenerator = idGenerator;
        this.propertyService = propertyService;
    }

    /**
     * 加载流版本配置（优先使用已部署版本, 带 Redis 缓存 read-through）
     * <p>
     * FR-043: 先加载 Flow entity 检查 deployed_version_id.
     * 如果已设置, 使用 findById() 加载特定版本; 否则回退到 findByFlowId().
     * 缓存 Key: {@code cp:flow:config:{flowId}}, TTL 120s.
     * </p>
     *
     * @return (FlowVersionEntity, FlowEntity) 的 Tuple2，FlowEntity 可能为 null
     */
    private Mono<Tuple2<FlowVersionEntity, Optional<FlowEntity>>> loadFlowVersion(Long flowId) {
        return entityCacheManager.getFlow(flowId)
                .flatMap(flow -> {
                    // 校验连接流状态：仅运行中(RUNNING=2)才允许执行
                    if (flow.getLifecycleStatus() == null || flow.getLifecycleStatus() != 2) {
                        return Mono.error(new RuntimeException(
                                "FLOW_NOT_RUNNING:" + flowId));  // marker prefix for classifyError replacement
                    }
                    Long deployedVersionId = flow.getDeployedVersionId();
                    if (deployedVersionId != null) {
                        log.debug("Flow {} has deployed_version_id={}, loading specific version",
                                flowId, deployedVersionId);
                        return entityCacheManager.getFlowVersion(deployedVersionId)
                                .map(fv -> Tuples.of(fv, Optional.of(flow)));
                    }
                    // 未设置部署版本, 回退到按 flowId 查询
                    return loadFlowVersionByFlowId(flowId)
                            .map(fv -> Tuples.of(fv, Optional.of(flow)));
                })
                .switchIfEmpty(
                    loadFlowVersionByFlowId(flowId)
                            .map(fv -> Tuples.of(fv, Optional.empty()))
                );
    }

    /**
     * 按 flowId 加载版本配置（带缓存 read-through）
     */
    private Mono<FlowVersionEntity> loadFlowVersionByFlowId(Long flowId) {
        String key = "cp:flow:config:" + flowId;
        return reactiveRedisTemplate.opsForValue().get(key)
                .flatMap(cachedJson -> {
                    FlowVersionEntity entity = new FlowVersionEntity();
                    entity.setOrchestrationConfig(cachedJson);
                    entity.setFlowId(flowId);
                    return Mono.just(entity);
                })
                .switchIfEmpty(
                    flowVersionReadRepository.findByFlowId(flowId)
                            .flatMap(entity -> reactiveRedisTemplate.opsForValue()
                                    .set(key, entity.getOrchestrationConfig(), Duration.ofSeconds(120))
                                    .thenReturn(entity))
                )
                .onErrorResume(e -> {
                    log.warn("Redis unavailable for flow config cache, falling back to DB: flowId={}", flowId);
                    return flowVersionReadRepository.findByFlowId(flowId);
                });
    }

    /**
     * 按版本 ID 直接加载流版本配置
     */
    private Mono<FlowVersionEntity> loadFlowVersionByVersionId(Long versionId) {
        return flowVersionReadRepository.findById(versionId);
    }

    /**
     * 执行 HTTP 触发
     * <p>
     * 1. 查询 flow 版本配置<br>
     * 2. 解析 React Flow 格式编排配置, 查找 trigger/entry 节点<br>
     * 3. 校验 {@code data.type} 子类型<br>
     * 4. 校验 {@code data.authConfig} 认证声明<br>
     * 5. 校验 {@code flowConfig.rateLimitConfig} 限流配置<br>
     * 6. 校验 {@code data.inputContract} header / query / body 三段契约<br>
     * 7. 构建 ExecutionContext (结构化 NodeContext: {header, query, body})<br>
     * 8. 缓存检查 (FR-037): 若 flowConfig.cacheTtl > 0, 先查缓存<br>
     * 9. 执行连接流并返回结果 (缓存命中时跳过执行)<br>
     * </p>
     *
     * @param flowId      连接流ID
     * @param triggerData 请求体 (Map)
     * @param headers     HTTP 请求头
     * @param queryParams URL Query 参数
     */
    @SuppressWarnings("unchecked")
    public Mono<TransparentFlowResponse> invokeFlow(Long flowId, Map<String, Object> triggerData,
                                                     Map<String, String> headers, Map<String, String> queryParams) {
        String executionId = UUID.randomUUID().toString().replace("-", "");
        Long recordId = idGenerator.nextId();

        return loadFlowVersion(flowId)
                .switchIfEmpty(Mono.error(new RuntimeException("Flow not found: " + flowId)))
                .flatMap(tuple -> {
                    FlowVersionEntity flowVersion = tuple.getT1();
                    FlowEntity flow = tuple.getT2().orElse(null);

                    // ★ 初始化执行记录
                    initExecutionRecord(recordId, flowId, executionId, flowVersion, flow);

                    // 1. 解析编排配置
                    Map<String, Object> config = flowVersion.parseOrchestrationConfigAsMap(objectMapper);
                    TriggerParseResult trigger = parseAndValidateTrigger(config, headers, queryParams, triggerData);
                    String triggerNodeId = trigger.triggerNodeId;
                    List<Map<String, Object>> nodes = (List<Map<String, Object>>) config.get("nodes");

                    // 2. 构建执行上下文
                    ExecutionContext context = new ExecutionContext(executionId, String.valueOf(flowId));
                    context.setTriggerData(triggerData);
                    context.setTriggerType(1); // HTTP触发
                    context.setDebug(false);
                    context.setTriggerHeaders(headers);
                    context.setTriggerQueryParams(queryParams);

                    // 3. 建立触发节点的 NodeContext (结构化: {header, query, body})
                    NodeContext triggerNodeCtx = new NodeContext();
                    triggerNodeCtx.setNodeId(triggerNodeId);
                    triggerNodeCtx.setNodeType("trigger");
                    Map<String, Object> triggerInput = buildStructuredTriggerInput(headers, queryParams, triggerData);
                    triggerNodeCtx.setInput(triggerInput);
                    triggerNodeCtx.setOutput(new HashMap<>());
                    triggerNodeCtx.setStatus("success");
                    context.setNodeContext(triggerNodeCtx);

                    // 4. 解析 flowConfig 缓存配置
                    FlowConfig flowConfig = parseFlowConfigFromOrchMap(config);

                    // 5. 缓存检查 -> 执行连接流
                    String flowIdStr = String.valueOf(flowId);
                    boolean cacheEnabled = flowConfig.getCacheTtl() != null && flowConfig.getCacheTtl() > 0;
                    final String cacheKey = cacheEnabled ? buildCacheKey(context, flowConfig.getCacheKeys()) : null;

                    Mono<ExecutionResult> executionMono;
                    if (cacheEnabled) {
                        final int cacheTtl = flowConfig.getCacheTtl();
                        executionMono = cacheManager.checkCache(flowId, cacheKey)
                                .flatMap(cachedResult -> {
                                    log.info("Cache hit: flowId={}, cacheKey={}", flowId, cacheKey);
                                    cachedResult.setCacheHit(true);
                                    return Mono.just(cachedResult);
                                })
                                .switchIfEmpty(
                                    dagScheduler.schedule(flowVersion.getOrchestrationConfig(), context)
                                            .flatMap(updatedCtx -> {
                                                ExecutionResult result = buildResultFromExecutionContext(
                                                        updatedCtx, executionId, flowIdStr, recordId);
                                                result.setCacheHit(false);
                                                return cacheManager.writeCache(flowId, cacheKey,
                                                        result, cacheTtl)
                                                        .thenReturn(result);
                                            })
                                );
                    } else {
                        executionMono = dagScheduler.schedule(flowVersion.getOrchestrationConfig(), context)
                                .map(updatedCtx -> buildResultFromExecutionContext(
                                        updatedCtx, executionId, flowIdStr, recordId));
                    }

                    return executionMono.flatMap(result -> {
                        // ★ 执行成功 - 更新记录
                        Integer finalStatus = "success".equals(result.getStatus()) ? 0 : 1;
                        long totalMs = result.getTotalDurationMs();
                        Integer duration = totalMs > 0 ? (int) totalMs : null;
                        String errCode = result.getErrorInfo() != null ? (String) result.getErrorInfo().get("code") : null;
                        String errMsg = result.getErrorInfo() != null ? (String) result.getErrorInfo().get("messageZh") : null;
                        // 异步取①平台全局最大记录数, 避免在 reactor/lettuce 线程上 block 导致自死锁
                        return finalizeExecutionRecord(recordId, flowId, finalStatus, duration, errCode, errMsg)
                                .thenReturn(buildTransparentResponse(flowIdStr, result));
                    });
                })
                .onErrorResume(e -> {
                    log.error("Trigger invoke failed: flowId={}, error={}", flowId, e.getMessage());
                    String flowIdStr = String.valueOf(flowId);
                    String msg = e.getMessage() != null ? e.getMessage() : "";

                    String[] classified = classifyError(msg, e);
                    String errorCode = classified[0];
                    String errorMsg = classified[1];
                    TransparentFlowResponse response = buildErrorResponse(flowIdStr, errorCode, errorMsg, msg);

                    // ★ 执行失败 - 更新记录
                    return finalizeExecutionRecord(recordId, flowId, 1, null, errorCode, errorMsg)
                            .thenReturn(response);
                });
    }

    /**
     * 触发节点解析结果
     */
    private static class TriggerParseResult {
        String triggerNodeId;
    }

    /**
     * 初始化执行记录并补充流元数据
     */
    private void initExecutionRecord(Long recordId, Long flowId, String executionId,
                                      FlowVersionEntity flowVersion, FlowEntity flow) {
        Long appId = flow != null ? flow.getAppId() : null;
        try {
            executionRecordService.startRecord(recordId, flowId,
                    flowVersion.getId(), appId, 1);
            log.debug("Execution record started: recordId={}, executionId={}", recordId, executionId);
        } catch (Exception ex) {
            log.warn("Failed to start execution record: {}", ex.getMessage());
        }
        if (flow != null) {
            try {
                executionRecordService.updateFlowMeta(recordId,
                        flowVersion.getId(),
                        flowVersion.getVersionNumber(),
                        flow.getAppId(),
                        flow.getNameCn(),
                        flow.getNameEn(),
                        flowVersion.getOrchestrationConfig());
            } catch (Exception ex) {
                log.warn("Failed to update flow meta for record {}: {}", recordId, ex.getMessage());
            }
        }
    }

    /**
     * 解析编排配置并校验触发节点合法性
     */
    @SuppressWarnings("unchecked")
    private TriggerParseResult parseAndValidateTrigger(Map<String, Object> config,
                                                        Map<String, String> headers,
                                                        Map<String, String> queryParams,
                                                        Map<String, Object> triggerData) {
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) config.get("nodes");

        if (nodes == null || nodes.isEmpty()) {
            throw new RuntimeException("Orchestration config has no nodes");
        }

        Map<String, Object> triggerNode = findTriggerNode(nodes);
        if (triggerNode == null) {
            throw new RuntimeException("No trigger/entry node found in orchestration config");
        }

        Map<String, Object> nodeData = (Map<String, Object>) triggerNode.get("data");
        if (nodeData == null) {
            throw new RuntimeException("Trigger node has no data field");
        }

        String triggerNodeId = (String) triggerNode.get("id");

        String triggerType = (String) nodeData.get("triggerType");
        if (triggerType == null || triggerType.isBlank()) {
            throw new RuntimeException("Trigger node data.triggerType is required");
        }
        if (!"http".equals(triggerType) && !"manual".equals(triggerType)) {
            throw new RuntimeException("Unknown trigger type: " + triggerType);
        }

        validateAuthConfig(nodeData, headers, triggerType);
        validateRateLimitConfig(config);

        Map<String, Object> input = (Map<String, Object>) nodeData.get("input");
        if ("http".equals(triggerType) && input == null) {
            throw new RuntimeException("HTTP trigger requires input");
        }
        if (input != null) {
            validateInputContractSections(input, headers, queryParams, triggerData);
        }

        TriggerParseResult result = new TriggerParseResult();
        result.triggerNodeId = triggerNodeId;
        return result;
    }

    /**
     * 根据异常消息前缀分类错误码与错误消息
     * <p>
     * 前置校验层抛出的异常使用特定前缀标记，执行层异常携带 NodeOutput.errorInfo 中的细分码.
     * </p>
     * @return 长度为2的数组：[errorCode, errorMsg]
     */
    private String[] classifyError(String msg, Throwable e) {
        if (msg == null) { msg = ""; }
        if (msg.startsWith("FLOW_NOT_RUNNING:")) {
            String flowIdStr = msg.substring("FLOW_NOT_RUNNING:".length());
            return new String[]{ErrorCode.PRECHECK_FLOW_NOT_RUNNING, "连接流未启动，请先启动后再调用"};
        }
        if (msg.contains("Flow not found")) {
            return new String[]{ErrorCode.PRECHECK_FLOW_NOT_FOUND, "连接流不存在"};
        }
        if (msg.contains("whitelist") || msg.contains("Whitelist") || msg.contains("URL not in")) {
            return new String[]{ErrorCode.PRECHECK_URL_WHITELIST_DENIED, "URL 白名单拒绝: " + msg};
        }
        if (msg.contains("token") || msg.contains("Token") || msg.contains("auth")
                || msg.contains("X-Sys-Token") || msg.contains("认证")) {
            return new String[]{ErrorCode.PRECHECK_AUTH_FAILED, "认证失败: " + msg};
        }
        if (msg.contains("required field") || msg.contains("input")
                || msg.contains("must be") || msg.contains("Missing required")
                || e instanceof IllegalArgumentException) {
            return new String[]{ErrorCode.PRECHECK_BAD_REQUEST, "请求参数错误: " + msg};
        }
        return new String[]{"500", "调用执行失败: " + msg};
    }

    /**
     * 根据错误码构建对应的错误响应
     */
    private TransparentFlowResponse buildErrorResponse(String flowIdStr, String errorCode,
                                                        String errorMsg, String msg) {
        return switch (errorCode) {
            case "404" -> TransparentFlowResponse.preExecutionError(
                    flowIdStr, HttpStatus.NOT_FOUND, errorCode,
                    errorMsg, "Flow not found: " + msg);
            case "403" -> TransparentFlowResponse.preExecutionError(
                    flowIdStr, HttpStatus.FORBIDDEN, errorCode,
                    errorMsg, "URL whitelist denied: " + msg);
            case "401" -> TransparentFlowResponse.preExecutionError(
                    flowIdStr, HttpStatus.UNAUTHORIZED, errorCode,
                    errorMsg, "Authentication failed: " + msg);
            case "400" -> TransparentFlowResponse.preExecutionError(
                    flowIdStr, HttpStatus.BAD_REQUEST, errorCode,
                    errorMsg, "Bad request: " + msg);
            case "409" -> TransparentFlowResponse.preExecutionError(
                    flowIdStr, HttpStatus.CONFLICT, errorCode,
                    errorMsg, "Flow not running: " + msg);
            default -> TransparentFlowResponse.preExecutionError(
                    flowIdStr, HttpStatus.INTERNAL_SERVER_ERROR, errorCode,
                    errorMsg, "Trigger execution failed: " + msg);
        };
    }

    /**
     * 从 ExecutionContext 构建 ExecutionResult (DagScheduler 输出桥接)
     */
    private ExecutionResult buildResultFromExecutionContext(
            ExecutionContext ctx, String executionId, String flowId, Long recordId) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(executionId);
        result.setFlowId(flowId);
        result.setDebug(ctx.isDebug());

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

        // 优先使用 exit 节点的输出作为响应体——按类型查找，而非硬编码 ID
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

        persistStepLogs(ctx, recordId);

        return result;
    }

    /**
     * 查找 exit 节点的输出（按类型查找，而非硬编码 ID）
     */
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
     * 持久化步骤日志（fire-and-forget，不影响业务响应）
     */
    private void persistStepLogs(ExecutionContext ctx, Long recordId) {
        try {
            List<StepLog> stepLogs = new ArrayList<>();
            for (Map.Entry<String, NodeContext> entry : ctx.getNodeContexts().entrySet()) {
                NodeContext nodeCtx = entry.getValue();
                StepLog sl = new StepLog();
                sl.stepId = idGenerator.nextId();
                sl.nodeId = nodeCtx.getNodeId();
                sl.nodeType = mapNodeType(nodeCtx.getNodeType());
                sl.nodeName = nodeCtx.getNodeId();
                sl.status = "success".equals(nodeCtx.getStatus()) ? 0 : 1;
                sl.input = nodeCtx.getInput();
                sl.output = nodeCtx.getOutput();
                sl.error = nodeCtx.getErrorInfo() != null ? String.valueOf(nodeCtx.getErrorInfo()) : null;
                sl.durationMs = (int) nodeCtx.getDurationMs();
                stepLogs.add(sl);
            }
            if (!stepLogs.isEmpty()) {
                executionStepService.logStepsBatch(recordId, stepLogs);
            }
        } catch (Exception ex) {
            log.warn("Failed to log execution steps for record {}: {}", recordId, ex.getMessage());
        }
    }

    /**
     * 将节点类型字符串映射为数字
     * @param nodeType 节点类型字符串 (trigger/connector/script/parallel/exit)
     * @return 1=trigger, 2=connector, 3=script, 4=parallel, 5=exit, 0=unknown
     */
    private Integer mapNodeType(String nodeType) {
        if (nodeType == null) { return 0; }
        return switch (nodeType.toLowerCase(Locale.ROOT)) {
            case "trigger" -> 1;
            case "connector" -> 2;
            case "script" -> 3;
            case "parallel" -> 4;
            case "exit" -> 5;
            default -> 0;
        };
    }

    /**
     * 从 ExecutionResult 构建 TransparentFlowResponse (透明穿透)
     * <p>
     * 拆解逻辑:
     * <ul>
     *   <li>resultData["header"] → userHeaders</li>
     *   <li>resultData["body"] → body</li>
     *   <li>如果 resultData 没有 body 字段 (exit 节点无 outputMapping 降级),
     *       则将整个 resultData 去掉 __ 前缀的元字段作为 body</li>
     * </ul>
     * </p>
     */
    @SuppressWarnings("unchecked")
    private TransparentFlowResponse buildTransparentResponse(String flowId, ExecutionResult result) {
        Map<String, Object> resultData = result.getResultData();
        Map<String, String> userHeaders = new LinkedHashMap<>();
        Object responseBody = null;

        if (resultData != null) {
            // 提取 header
            Object headerObj = resultData.get("header");
            if (headerObj instanceof Map) {
                ((Map<String, ?>) headerObj).forEach((k, v) -> userHeaders.put(String.valueOf(k), String.valueOf(v)));
            }
            // 提取 body
            Object bodyObj = resultData.get("body");
            if (bodyObj != null) {
                responseBody = bodyObj;
            } else {
                // 降级: 无 outputMapping 时, 整个 resultData 去掉 __ 元字段作为 body
                Map<String, Object> fallback = new LinkedHashMap<>();
                resultData.forEach((k, v) -> { if (!k.startsWith("__")) { fallback.put(k, v); } });
                responseBody = fallback.isEmpty() ? null : fallback;
            }
        }

        int execStatus = "success".equals(result.getStatus()) ? 0
                : ("timeout".equals(result.getStatus()) ? 2 : 1);

        TransparentFlowResponse r = TransparentFlowResponse.success(
                flowId, result.getExecutionId(), execStatus, result.getTotalDurationMs(),
                userHeaders, responseBody);

        // v5.8: 执行失败/超时时补充 X-Code / X-Message-* 错误头
        if (execStatus != 0 && result.getErrorInfo() != null) {
            Map<String, Object> err = result.getErrorInfo();
            if (err.containsKey("code")) {
                r.getPlatformHeaders().put("X-Code", String.valueOf(err.get("code")));
            }
            if (err.containsKey("messageZh")) {
                r.getPlatformHeaders().put("X-Message-Zh", String.valueOf(err.get("messageZh")));
            }
            if (err.containsKey("messageEn")) {
                r.getPlatformHeaders().put("X-Message-En", String.valueOf(err.get("messageEn")));
            }
        }

        r.getPlatformHeaders().put("X-Cache-Status", result.isCacheHit() ? "1" : "0");

        // v5.9: 失败时设置正确的 HTTP 状态码（原先始终返回 200）
        if (execStatus != 0) {
            String xCode = r.getPlatformHeaders().get("X-Code");
            if (xCode != null && xCode.contains("timeout")) {
                r.setHttpStatus(HttpStatus.GATEWAY_TIMEOUT);
            } else {
                r.setHttpStatus(HttpStatus.BAD_GATEWAY);
            }
        }

        return r;
    }

    /**
     * v5.7: 构建结构化 trigger input: {header: {...}, query: {...}, body: {...}}
     * query 参数从 HTTP 接收均为 String，尝试按数值类型自动转换
     */
    private Map<String, Object> buildStructuredTriggerInput(Map<String, String> headers,
                                                             Map<String, String> queryParams,
                                                             Map<String, Object> triggerData) {
        Map<String, Object> input = new HashMap<>();

        Map<String, Object> headerPart = new HashMap<>();
        if (headers != null) {
            headerPart.putAll(headers);
        }
        input.put("header", headerPart);

        Map<String, Object> queryPart = new HashMap<>();
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                queryPart.put(entry.getKey(), coerceValue(entry.getValue()));
            }
        }
        input.put("query", queryPart);

        Map<String, Object> bodyPart = new HashMap<>();
        if (triggerData != null) {
            bodyPart.putAll(triggerData);
        }
        input.put("body", bodyPart);

        return input;
    }

    /**
     * 将字符串值自动转换为合适的数值类型 (整型/浮点优先)
     */
    private Object coerceValue(String value) {
        if (value == null) { return null; }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                return value;
            }
        }
    }

    /**
     * 从 nodes 数组中查找 trigger 节点
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findTriggerNode(List<Map<String, Object>> nodes) {
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");
            if ("trigger".equals(type)) {
                return node;
            }
        }
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    /**
     * 校验 inputContract 的 header / query / body 三段
     */
    @SuppressWarnings("unchecked")
    private void validateInputContractSections(Map<String, Object> inputContract,
                                                Map<String, String> headers,
                                                Map<String, String> queryParams,
                                                Map<String, Object> triggerData) {
        // 校验 header 段
        Map<String, Object> headerSchema = (Map<String, Object>) inputContract.get("header");
        if (headerSchema != null) {
            Map<String, Object> headerData = headers != null ? new HashMap<>(headers) : new HashMap<>();
            validateInputContract(headerSchema, headerData, "header");
        }

        // 校验 query 段
        Map<String, Object> querySchema = (Map<String, Object>) inputContract.get("query");
        if (querySchema != null) {
            Map<String, Object> queryData = queryParams != null ? new HashMap<>(queryParams) : new HashMap<>();
            validateInputContract(querySchema, queryData, "query");
        }

        // 校验 body 段
        Map<String, Object> bodySchema = (Map<String, Object>) inputContract.get("body");
        if (bodySchema != null && triggerData != null) {
            validateInputContract(bodySchema, triggerData, "body");
        }
    }

    /**
     * 校验输入数据符合契约 schema
     *
     * @param schema 契约定义 ({type, properties, required})
     * @param data   实际数据
     * @param sectionName 段名称 (header/query/body), 用于错误提示
     */
    @SuppressWarnings("unchecked")
    private void validateInputContract(Map<String, Object> schema, Map<String, Object> data, String sectionName) {
        // 校验 required 字段
        List<String> required = (List<String>) schema.get("required");
        if (required != null && !required.isEmpty()) {
            for (String field : required) {
                if (!data.containsKey(field) || data.get(field) == null) {
                    throw new IllegalArgumentException(
                            "Missing required field in trigger " + sectionName + ": " + field);
                }
            }
        }

        // 校验 properties 类型
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        if (properties != null && data != null) {
            for (Map.Entry<String, Object> prop : properties.entrySet()) {
                String fieldName = prop.getKey();
                if (!data.containsKey(fieldName)) { continue; }
                Object value = data.get(fieldName);
                Map<String, Object> propSchema = (Map<String, Object>) prop.getValue();
                if (propSchema != null) {
                    String expectedType = (String) propSchema.get("type");
                    if (expectedType != null && value != null) {
                        validateFieldType(fieldName + " (" + sectionName + ")", expectedType, value);
                    }
                }
            }
        }
    }

    /**
     * 校验单个字段类型 (v5.7: 支持 String→Number 自动转换，适配 header/query 均为 String 的场景)
     */
    private void validateFieldType(String fieldName, String expectedType, Object value) {
        switch (expectedType) {
            case "string":
                if (!(value instanceof String)) {
                    throw new IllegalArgumentException(
                            "Field '" + fieldName + "' must be string, got: " + value.getClass().getSimpleName());
                }
                break;
            case "integer":
            case "number":
                validateNumberType(fieldName, value);
                break;
            case "boolean":
                validateBooleanType(fieldName, value);
                break;
            case "object":
                if (!(value instanceof Map)) {
                    throw new IllegalArgumentException(
                            "Field '" + fieldName + "' must be object, got: " + value.getClass().getSimpleName());
                }
                break;
            default:
                break;
        }
    }

    /**
     * 校验数值类型字段（支持 String→Number 自动转换）
     */
    private void validateNumberType(String fieldName, Object value) {
        if (value instanceof Number) {
            return;
        }
        if (value instanceof String) {
            try {
                Long.parseLong((String) value);
                return;
            } catch (NumberFormatException e1) {
                try {
                    Double.parseDouble((String) value);
                    return;
                } catch (NumberFormatException e2) {
                    throw new IllegalArgumentException(
                            "Field '" + fieldName + "' must be number, got: '" + value + "'");
                }
            }
        }
        throw new IllegalArgumentException(
                "Field '" + fieldName + "' must be number, got: " + value.getClass().getSimpleName());
    }

    /**
     * 校验布尔类型字段
     */
    private void validateBooleanType(String fieldName, Object value) {
        if (value instanceof Boolean) {
            return;
        }
        if (value instanceof String && ("true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value))) {
            return;
        }
        throw new IllegalArgumentException(
                "Field '" + fieldName + "' must be boolean, got: " + value.getClass().getSimpleName());
    }

    /**
     * 校验 authConfig (HTTP 触发时必须存在)
     */
    @SuppressWarnings("unchecked")
    private void validateAuthConfig(Map<String, Object> nodeData, Map<String, String> headers, String triggerType) {
        List<Map<String, Object>> authConfigs = (List<Map<String, Object>>) nodeData.get("authConfigs");
        if ("http".equals(triggerType)) {
            if (authConfigs == null || authConfigs.isEmpty()) {
                throw new RuntimeException("HTTP trigger requires authConfigs");
            }
            for (Map<String, Object> ac : authConfigs) {
                String authType = (String) ac.get("type");
                if (authType == null || authType.isBlank()) {
                    throw new RuntimeException("authConfigs[].type is required for HTTP trigger");
                }
            }
        }
        if (authConfigs != null) {
            for (Map<String, Object> ac : authConfigs) {
                String authType = (String) ac.get("type");
                if ("SYSTOKEN".equals(authType)) {
                    validateSystoken(ac, headers);
                }
            }
        }
    }

    /**
     * SYSTOKEN 白名单校验 — 从 authConfig header.properties 中提取 token 字段，
     * 校验请求 headers 中包含该字段且值在 sysAccountWhitelist 内。
     */
    @SuppressWarnings("unchecked")
    private void validateSystoken(Map<String, Object> authConfig, Map<String, String> headers) {
        Map<String, Object> headerContainer = (Map<String, Object>) authConfig.get("header");
        if (headerContainer == null) { return; }

        Map<String, Object> properties = (Map<String, Object>) headerContainer.get("properties");
        if (properties == null || properties.isEmpty()) { return; }

        List<String> whitelistTokens = (List<String>) authConfig.get("sysAccountWhitelist");

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String fieldName = entry.getKey();
            String token = headers != null ? headers.get(fieldName) : null;
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Missing " + fieldName + " for SYSTOKEN auth");
            }
            if (whitelistTokens != null) {
                if (whitelistTokens.isEmpty()) {
                    throw new RuntimeException("SysAccount whitelist is empty, all requests rejected");
                }
                if (!whitelistTokens.contains(resolveSysAccount(token))) {
                    throw new RuntimeException("SysAccount not in whitelist: " + resolveSysAccount(token));
                }
            }
        }
    }

    @StandardTodo("对接 token 解析服务，根据 SysToken 解析出 SysAccount")
    private String resolveSysAccount(String token) {
        return token;
    }

    /**
     * 校验 flowConfig.rateLimitConfig 基本合法性（§3.3.4: 运行态不做上限截断，设计态已校验）
     */
    @SuppressWarnings("unchecked")
    private void validateRateLimitConfig(Map<String, Object> config) {
        Map<String, Object> flowConfig = (Map<String, Object>) config.get("flowConfig");
        if (flowConfig == null) {
            return;
        }
        Map<String, Object> rateLimitConfig = (Map<String, Object>) flowConfig.get("rateLimitConfig");
        if (rateLimitConfig == null) {
            return;
        }
        // 仅校验基本合法性（>0），不做上限截断（设计态已校验 ③ <= ②覆盖①）
        Object maxQpsObj = rateLimitConfig.get("maxQps");
        if (maxQpsObj instanceof Number) {
            int maxQps = ((Number) maxQpsObj).intValue();
            if (maxQps < 1) {
                throw new RuntimeException("Rate limit maxQps must be greater than 0, got: " + maxQps);
            }
        }
        Object maxConcurrencyObj = rateLimitConfig.get("maxConcurrency");
        if (maxConcurrencyObj instanceof Number) {
            int maxConcurrency = ((Number) maxConcurrencyObj).intValue();
            if (maxConcurrency < 1) {
                throw new RuntimeException("Rate limit maxConcurrency must be greater than 0, got: " + maxConcurrency);
            }
        }
    }



    @SuppressWarnings("unchecked")
    private FlowConfig parseFlowConfigFromOrchMap(Map<String, Object> config) {
        try {
            Map<String, Object> flowConfigMap = (Map<String, Object>) config.get("flowConfig");
            if (flowConfigMap == null || flowConfigMap.isEmpty()) {
                return FlowConfig.defaults();
            }
            FlowConfig fc = new FlowConfig();
            Object cacheObj = flowConfigMap.get("cache");
            if (cacheObj instanceof Map) {
                Map<String, Object> cacheMap = (Map<String, Object>) cacheObj;
                Object ttl = cacheMap.get("ttl");
                if (ttl instanceof Number) {
                    fc.setCacheTtl(((Number) ttl).intValue());
                }
                Object key = cacheMap.get("key");
                if (key instanceof List) {
                    List<String> keys = new ArrayList<>();
                    for (Object k : (List<?>) key) {
                        keys.add(String.valueOf(k));
                    }
                    fc.setCacheKeys(keys);
                }
            }
            return fc;
        } catch (Exception e) {
            log.warn("Failed to parse flowConfig from orchestration map: {}", e.getMessage());
            return FlowConfig.defaults();
        }
    }

    /**
     * 异步收尾执行记录: 读取①平台全局最大记录数后更新记录并执行 FIFO 清理.
     * <p>
     * 读取配置改为异步, 避免在 reactor/lettuce 线程上 block 导致 Redis 命令自死锁超时.
     * updateRecord / checkAndCleanFifo 本身为 fire-and-forget (内部 subscribe), 不阻塞响应链.
     * §3.3.4: 读①平台全局值.
     */
    private Mono<Void> finalizeExecutionRecord(Long recordId, Long flowId, Integer status,
                                                Integer durationMs, String errorCode, String errorMsg) {
        return propertyService.loadPlatformDefaults()
                .map(config -> {
                    String val = config.get("Max.Execution.Records.Per.Flow");
                    if (val == null || val.isEmpty()) return 1000;
                    try { return Integer.parseInt(val.trim()); }
                    catch (NumberFormatException e) { return 1000; }
                })
                .defaultIfEmpty(1000)
                .onErrorReturn(1000)
                .doOnNext(maxRecords -> {
                    try {
                        executionRecordService.updateRecord(recordId, status, durationMs, errorCode, errorMsg);
                        // FR-005: FIFO 清理 — 单流记录数超过上限时删除最早记录 (§3.3.4: 读①)
                        executionRecordService.checkAndCleanFifo(flowId, maxRecords);
                    } catch (Exception ex) {
                        log.warn("Failed to update execution record: {}", ex.getMessage());
                    }
                })
                .then();
    }

    private String buildCacheKey(ExecutionContext ctx, List<String> cacheKeyExpressions) {
        if (cacheKeyExpressions == null || cacheKeyExpressions.isEmpty()) {
            return "";
        }
        ExpressionResolver resolver = new ExpressionResolver();
        StringBuilder sb = new StringBuilder();
        for (String expr : cacheKeyExpressions) {
            Object resolved = resolver.resolve(expr, ctx.getNodeContexts());
            if (sb.length() > 0) { sb.append(":"); }
            sb.append(resolved != null ? resolved.toString() : "");
        }
        return sb.toString();
    }

}
