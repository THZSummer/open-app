package com.xxx.it.works.wecode.v2.modules.flow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersionEntity;
import com.xxx.it.works.wecode.v2.modules.connector.repository.OpConnectorVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowReadRepository;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.common.IdGenerator;
import com.xxx.it.works.wecode.v2.modules.cache.FlowCacheManager;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionRecordService;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionStepService;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionStepService.StepLog;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.DagScheduler;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.runtime.model.FlowConfig;
import com.xxx.it.works.wecode.v2.modules.runtime.model.TransparentFlowResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.xxx.it.works.wecode.v2.modules.auth.AuthValidatorRegistry;
import com.xxx.it.works.wecode.v2.modules.security.UrlWhitelistValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Duration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import com.xxx.it.works.wecode.v2.common.config.CacheToggle;
import reactor.core.publisher.Flux;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

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
 *   <li>新增 connector 节点 URL 白名单校验 (data.urlWhitelist), 违规返回 403</li>
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
    private final AuthValidatorRegistry authValidatorRegistry;
    private final UrlWhitelistValidator urlWhitelistValidator;
    private final OpFlowVersionReadRepository flowVersionReadRepository;
    private final OpFlowReadRepository flowReadRepository;
    private final OpConnectorVersionReadRepository connectorVersionReadRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final CacheToggle cacheToggle;
    private final FlowCacheManager cacheManager;
    private final ExecutionRecordService executionRecordService;
    private final ExecutionStepService executionStepService;
    private final IdGenerator idGenerator;

    public FlowInvokeService(
            ObjectMapper objectMapper,
            AuthValidatorRegistry authValidatorRegistry,
            UrlWhitelistValidator urlWhitelistValidator,
            ReactiveSequentialExecutor executor,
            DagScheduler dagScheduler,
            OpFlowVersionReadRepository flowVersionReadRepository,
            OpFlowReadRepository flowReadRepository,
            OpConnectorVersionReadRepository connectorVersionReadRepository,
            ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            CacheToggle cacheToggle,
            FlowCacheManager cacheManager,
            ExecutionRecordService executionRecordService,
            ExecutionStepService executionStepService,
            IdGenerator idGenerator) {
        this.objectMapper = objectMapper;
        this.authValidatorRegistry = authValidatorRegistry;
        this.urlWhitelistValidator = urlWhitelistValidator;
        this.executor = executor;
        this.dagScheduler = dagScheduler;
        this.flowVersionReadRepository = flowVersionReadRepository;
        this.flowReadRepository = flowReadRepository;
        this.connectorVersionReadRepository = connectorVersionReadRepository;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.cacheToggle = cacheToggle;
        this.cacheManager = cacheManager;
        this.executionRecordService = executionRecordService;
        this.executionStepService = executionStepService;
        this.idGenerator = idGenerator;
    }

    /**
     * 加载流版本配置（优先使用已部署版本, 带 Redis 缓存 read-through）
     * <p>
     * FR-043: 先加载 Flow entity 检查 deployed_version_id.
     * 如果已设置, 使用 findById() 加载特定版本; 否则回退到 findByFlowId().
     * 缓存 Key: {@code cp:flow:config:{flowId}}, TTL 120s.
     * {@code connector.cache.enabled=false} 时直接穿透 R2DBC.
     * </p>
     *
     * @return Tuple2 of (FlowVersionEntity, FlowEntity) — FlowEntity may be null if not found
     */
    private Mono<Tuple2<FlowVersionEntity, Optional<FlowEntity>>> loadFlowVersion(Long flowId) {
        return flowReadRepository.findById(flowId)
                .flatMap(flow -> {
                    Long deployedVersionId = flow.getDeployedVersionId();
                    if (deployedVersionId != null) {
                        log.debug("Flow {} has deployed_version_id={}, loading specific version",
                                flowId, deployedVersionId);
                        return flowVersionReadRepository.findById(deployedVersionId)
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
        if (!cacheToggle.isEnabled()) {
            return flowVersionReadRepository.findByFlowId(flowId);
        }
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
     * 5. 校验 {@code data.rateLimitConfig} 限流配置<br>
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

                    // ★ 创建执行记录 (status=pending, 使用正确的 appId)
                    Long appId = flow != null ? flow.getAppId() : null;
                    try {
                        executionRecordService.startRecord(recordId, flowId,
                                flowVersion.getId(), appId, 1);
                        log.debug("Execution record started: recordId={}, executionId={}", recordId, executionId);
                    } catch (Exception ex) {
                        log.warn("Failed to start execution record: {}", ex.getMessage());
                    }

                    // ★ 补充流元数据（flowNameCn/flowNameEn/flowVersionSnapshot）
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

                    // 1. 解析编排配置 (React Flow 格式)
                    Map<String, Object> config = flowVersion.parseOrchestrationConfigAsMap(objectMapper);
                    List<Map<String, Object>> nodes = (List<Map<String, Object>>) config.get("nodes");

                    if (nodes == null || nodes.isEmpty()) {
                        return Mono.error(new RuntimeException("Orchestration config has no nodes"));
                    }

                    // 2. 查找 trigger/entry 节点
                    Map<String, Object> triggerNode = findTriggerNode(nodes);
                    if (triggerNode == null) {
                        return Mono.error(new RuntimeException("No trigger/entry node found in orchestration config"));
                    }

                    // 3. 获取 data 字段 (React Flow: node.data.xxx)
                    Map<String, Object> nodeData = (Map<String, Object>) triggerNode.get("data");
                    if (nodeData == null) {
                        return Mono.error(new RuntimeException("Trigger node has no data field"));
                    }

                    String triggerNodeId = (String) triggerNode.get("id");

                    // 4. 校验 data.type 子类型
                    String subType = (String) nodeData.get("type");
                    if (subType == null || subType.isBlank()) {
                        return Mono.error(new RuntimeException("Trigger node data.type is required"));
                    }
                    if (!"http".equals(subType) && !"manual".equals(subType)) {
                        return Mono.error(new RuntimeException("Unknown trigger type: " + subType));
                    }

                    // 5. 校验 authConfig (HTTP 触发时必须存在)
                    validateAuthConfig(nodeData, headers, subType);

                    // 6. 校验 rateLimitConfig
                    validateRateLimitConfig(nodeData);

                    // 7. 校验 inputContract (header / query / body 三段)
                    Map<String, Object> inputContract = (Map<String, Object>) nodeData.get("inputContract");
                    if ("http".equals(subType) && inputContract == null) {
                        return Mono.error(new RuntimeException("HTTP trigger requires inputContract"));
                    }
                    if (inputContract != null) {
                        validateInputContractSections(inputContract, headers, queryParams, triggerData);
                    }

                    // 7.5 校验 connector 节点 URL 白名单 (FR-015: 从 connector connection_config 读取)
                    // 8. 构建执行上下文
                    ExecutionContext context = new ExecutionContext(executionId, String.valueOf(flowId));
                    context.setTriggerData(triggerData);
                    context.setTriggerType(1); // HTTP触发
                    context.setTest(false);
                    // 存储 header 和 query 到 context 供 TriggerNodeExecutor 使用
                    context.setTriggerHeaders(headers);
                    context.setTriggerQueryParams(queryParams);

                    // 9. 建立触发节点的 NodeContext (结构化: {header, query, body})
                    NodeContext triggerNodeCtx = new NodeContext();
                    triggerNodeCtx.setNodeId(triggerNodeId);
                    triggerNodeCtx.setNodeType("trigger");
                    Map<String, Object> input = buildStructuredTriggerInput(headers, queryParams, triggerData);
                    triggerNodeCtx.setInput(input);
                    triggerNodeCtx.setOutput(new HashMap<>());
                    triggerNodeCtx.setStatus("success");
                    context.setNodeContext(triggerNodeCtx);

                    // 10. 解析 flowConfig 缓存配置
                    FlowConfig flowConfig = parseFlowConfigFromOrchMap(config);

                    // 11. 异步校验 URL 白名单 -> 缓存检查 -> 执行连接流
                    String flowIdStr = String.valueOf(flowId);
                    boolean cacheEnabled = flowConfig.getCacheTtl() != null && flowConfig.getCacheTtl() > 0;
                    final String cacheKey = cacheEnabled ? buildCacheKey(context) : null;

                    Mono<ExecutionResult> executionMono;
                    if (cacheEnabled) {
                        final int cacheTtl = flowConfig.getCacheTtl();
                        executionMono = validateConnectorUrlWhitelist(nodes)
                                .then(cacheManager.checkCache(flowId, cacheKey)
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
                                        ));
                    } else {
                        executionMono = validateConnectorUrlWhitelist(nodes)
                                .then(dagScheduler.schedule(flowVersion.getOrchestrationConfig(), context)
                                        .map(updatedCtx -> buildResultFromExecutionContext(
                                                updatedCtx, executionId, flowIdStr, recordId)));
                    }

                    return executionMono.map(result -> {
                        // ★ 执行成功 - 更新记录
                        Integer finalStatus = "success".equals(result.getStatus()) ? 0 : 1;
                        long totalMs = result.getTotalDurationMs();
                        Integer duration = totalMs > 0 ? (int) totalMs : null;
                        String errCode = result.getErrorInfo() != null ? (String) result.getErrorInfo().get("code") : null;
                        String errMsg = result.getErrorInfo() != null ? (String) result.getErrorInfo().get("messageZh") : null;
                        try {
                            executionRecordService.updateRecord(recordId, finalStatus, duration, errCode, errMsg);
                        } catch (Exception ex) {
                            log.warn("Failed to update execution record: {}", ex.getMessage());
                        }
                        return buildTransparentResponse(flowIdStr, result);
                    });
                })
                .onErrorResume(e -> {
                    log.error("Trigger invoke failed: flowId={}, error={}", flowId, e.getMessage());
                    String flowIdStr = String.valueOf(flowId);
                    String msg = e.getMessage() != null ? e.getMessage() : "";

                    String errorCode;
                    String errorMsg;
                    TransparentFlowResponse response;

                    // 按异常消息分类错误码
                    if (msg.contains("Flow not found")) {
                        errorCode = "404";
                        errorMsg = "流不存在: " + msg;
                        response = TransparentFlowResponse.preExecutionError(
                                flowIdStr, HttpStatus.NOT_FOUND, errorCode,
                                errorMsg, "Flow not found: " + msg);
                    } else if (msg.contains("whitelist") || msg.contains("Whitelist")
                            || msg.contains("URL not in")) {
                        errorCode = "403";
                        errorMsg = "URL 白名单拒绝: " + msg;
                        response = TransparentFlowResponse.preExecutionError(
                                flowIdStr, HttpStatus.FORBIDDEN, errorCode,
                                errorMsg, "URL whitelist denied: " + msg);
                    } else if (msg.contains("token") || msg.contains("Token") || msg.contains("auth")
                            || msg.contains("X-Sys-Token") || msg.contains("认证")) {
                        errorCode = "401";
                        errorMsg = "认证失败: " + msg;
                        response = TransparentFlowResponse.preExecutionError(
                                flowIdStr, HttpStatus.UNAUTHORIZED, errorCode,
                                errorMsg, "Authentication failed: " + msg);
                    } else if (msg.contains("required field") || msg.contains("inputContract")
                            || msg.contains("must be") || msg.contains("Missing required")
                            || e instanceof IllegalArgumentException) {
                        errorCode = "400";
                        errorMsg = "请求参数错误: " + msg;
                        response = TransparentFlowResponse.preExecutionError(
                                flowIdStr, HttpStatus.BAD_REQUEST, errorCode,
                                errorMsg, "Bad request: " + msg);
                    } else {
                        // 其他异常 → 500
                        errorCode = "500";
                        errorMsg = "触发执行失败: " + msg;
                        response = TransparentFlowResponse.preExecutionError(
                                flowIdStr, HttpStatus.INTERNAL_SERVER_ERROR, errorCode,
                                errorMsg, "Trigger execution failed: " + msg);
                    }

                    // ★ 执行失败 - 更新记录
                    try {
                        executionRecordService.updateRecord(recordId, 1, null, errorCode, errorMsg);
                    } catch (Exception ex) {
                        log.warn("Failed to update execution record: {}", ex.getMessage());
                    }

                    return Mono.just(response);
                });
    }

    /**
     * 从 ExecutionContext 构建 ExecutionResult (DagScheduler 输出桥接)
     */
    private ExecutionResult buildResultFromExecutionContext(
            ExecutionContext ctx, String executionId, String flowId, Long recordId) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutionId(executionId);
        result.setFlowId(flowId);
        result.setTest(ctx.isTest());

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

        // Prefer exit node output for response body (ConcurrentHashMap has no guaranteed iteration order)
        NodeContext exitNodeCtx = ctx.getNodeContexts().get("node_exit");
        if (exitNodeCtx != null && exitNodeCtx.getOutput() != null
                && !exitNodeCtx.getOutput().isEmpty()) {
            lastOutput = exitNodeCtx.getOutput();
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

        // ★ 步骤日志持久化 (fire-and-forget, 不影响业务响应)
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

        return result;
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
                resultData.forEach((k, v) -> { if (!k.startsWith("__")) fallback.put(k, v); });
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

        // v5.9: set proper HTTP status for failures (was always 200)
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
        if (value == null) return null;
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
                if (!data.containsKey(fieldName)) continue;
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
                if (value instanceof Number) {
                    break;
                }
                // header/query 参数来自 HTTP，均为 String，尝试转换
                if (value instanceof String) {
                    try {
                        Long.parseLong((String) value);
                        break;
                    } catch (NumberFormatException e1) {
                        try {
                            Double.parseDouble((String) value);
                            break;
                        } catch (NumberFormatException e2) {
                            throw new IllegalArgumentException(
                                    "Field '" + fieldName + "' must be number, got: '" + value + "'");
                        }
                    }
                }
                throw new IllegalArgumentException(
                        "Field '" + fieldName + "' must be number, got: " + value.getClass().getSimpleName());
            case "boolean":
                if (value instanceof Boolean) {
                    break;
                }
                // 字符串 "true"/"false" 也接受
                if (value instanceof String && ("true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value))) {
                    break;
                }
                throw new IllegalArgumentException(
                        "Field '" + fieldName + "' must be boolean, got: " + value.getClass().getSimpleName());
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
     * 校验 authConfig (HTTP 触发时必须存在)
     */
    @SuppressWarnings("unchecked")
    private void validateAuthConfig(Map<String, Object> nodeData, Map<String, String> headers, String subType) {
        Map<String, Object> authConfig = (Map<String, Object>) nodeData.get("authConfig");
        if ("http".equals(subType)) {
            if (authConfig == null || authConfig.isEmpty()) {
                throw new RuntimeException("HTTP trigger requires authConfig");
            }
            String authType = (String) authConfig.get("type");
            if (authType == null || authType.isBlank()) {
                throw new RuntimeException("authConfig.type is required for HTTP trigger");
            }
        }
        authValidatorRegistry.validate(authConfig, headers);
    }

    /**
     * 校验 rateLimitConfig
     */
    @SuppressWarnings("unchecked")
    private void validateRateLimitConfig(Map<String, Object> nodeData) {
        Map<String, Object> rateLimitConfig = (Map<String, Object>) nodeData.get("rateLimitConfig");
        if (rateLimitConfig == null) {
            return;
        }
        Object maxQpsObj = rateLimitConfig.get("maxQps");
        if (maxQpsObj instanceof Number) {
            int maxQps = ((Number) maxQpsObj).intValue();
            if (maxQps < 1 || maxQps > 10000) {
                throw new RuntimeException("Rate limit maxQps must be between 1 and 10000, got: " + maxQps);
            }
        }
        Object maxConcurrencyObj = rateLimitConfig.get("maxConcurrency");
        if (maxConcurrencyObj instanceof Number) {
            int maxConcurrency = ((Number) maxConcurrencyObj).intValue();
            if (maxConcurrency < 1 || maxConcurrency > 1000) {
                throw new RuntimeException("Rate limit maxConcurrency must be between 1 and 1000, got: " + maxConcurrency);
            }
        }
    }

    /**
     * 校验 connector 节点的目标 URL 是否在白名单中 (FR-015)
     * <p>
     * 遍历编排配置中所有 type=connector 节点, 从节点 data.connectorVersionId 加载
     * 连接器版本, 解析 connection_config JSON 中的 urlWhitelist 进行校验.
     * 空白名单（null 或空列表）允许所有 URL 通过, 不影响存量业务.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private Mono<Void> validateConnectorUrlWhitelist(List<Map<String, Object>> nodes) {
        if (nodes == null || nodes.isEmpty()) return Mono.empty();

        List<Mono<Void>> validations = new ArrayList<>();

        for (Map<String, Object> node : nodes) {
            String nodeType = (String) node.get("type");
            if (!"connector".equals(nodeType)) continue;

            Map<String, Object> connData = (Map<String, Object>) node.get("data");
            if (connData == null) continue;

            // 从节点 data 获取 connectorVersionId (支持 Number 和 String 两种格式)
            Object connectorVersionIdObj = connData.get("connectorVersionId");
            if (connectorVersionIdObj == null) continue;

            Long connectorVersionId;
            if (connectorVersionIdObj instanceof Number) {
                connectorVersionId = ((Number) connectorVersionIdObj).longValue();
            } else if (connectorVersionIdObj instanceof String) {
                try {
                    connectorVersionId = Long.valueOf((String) connectorVersionIdObj);
                } catch (NumberFormatException e) {
                    log.warn("Invalid connectorVersionId format: {}", connectorVersionIdObj);
                    continue;
                }
            } else {
                continue;
            }

            // 异步加载连接器版本, 从 connection_config 提取 urlWhitelist 并校验
            Mono<Void> validation = connectorVersionReadRepository.findById(connectorVersionId)
                    .<Void>flatMap(connVersion -> {
                        Map<String, Object> connConfig = connVersion.parseConnectionConfig(objectMapper);

                        // v5.9: extract URL from connection_config.protocolConfig.url (not node data)
                        Map<String, Object> protocolConfig = (Map<String, Object>) connConfig.get("protocolConfig");
                        String url = protocolConfig != null ? (String) protocolConfig.get("url") : null;
                        if (url == null || url.isEmpty()) {
                            return Mono.empty();
                        }

                        List<String> whitelist = extractWhitelist(connConfig.get("urlWhitelist"));
                        if (!urlWhitelistValidator.validate(url, whitelist)) {
                            return Mono.<Void>error(new RuntimeException("URL not in whitelist: " + url));
                        }
                        return Mono.empty();
                    })
                    .onErrorResume(e -> {
                        // 连接器版本未找到时允许通过 (向后兼容)
                        if (e instanceof RuntimeException) {
                            return Mono.error(e);
                        }
                        log.warn("Failed to load connector version {} for URL whitelist check: {}",
                                connectorVersionId, e.getMessage());
                        return Mono.empty();
                    });
            validations.add(validation);
        }

        if (validations.isEmpty()) return Mono.empty();
        return Flux.merge(validations).then();
    }

    /**
     * Extract whitelist patterns from connection config, supporting both
     * single string and list-of-strings formats for backward compatibility.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractWhitelist(Object whitelistObj) {
        if (whitelistObj == null) {
            return null;
        }
        if (whitelistObj instanceof List) {
            return (List<String>) whitelistObj;
        }
        if (whitelistObj instanceof String) {
            return List.of((String) whitelistObj);
        }
        log.warn("urlWhitelist has unexpected type: {}", whitelistObj.getClass().getName());
        return null;
    }

    @SuppressWarnings("unchecked")
    private FlowConfig parseFlowConfigFromOrchMap(Map<String, Object> config) {
        try {
            Map<String, Object> flowConfigMap = (Map<String, Object>) config.get("flowConfig");
            if (flowConfigMap == null || flowConfigMap.isEmpty()) {
                return FlowConfig.defaults();
            }
            FlowConfig fc = new FlowConfig();
            Object ttl = flowConfigMap.get("cacheTtl");
            if (ttl instanceof Number) {
                fc.setCacheTtl(((Number) ttl).intValue());
            }
            Object template = flowConfigMap.get("cacheKeyTemplate");
            if (template instanceof String) {
                fc.setCacheKeyTemplate((String) template);
            }
            return fc;
        } catch (Exception e) {
            log.warn("Failed to parse flowConfig from orchestration map: {}", e.getMessage());
            return FlowConfig.defaults();
        }
    }

    private String buildCacheKey(ExecutionContext ctx) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
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
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(Objects.hash(ctx.getExecutionId()));
        } catch (Exception e) {
            log.warn("Failed to compute cache key: {}", e.getMessage());
            return String.valueOf(Objects.hash(ctx.getExecutionId()));
        }
    }
}
