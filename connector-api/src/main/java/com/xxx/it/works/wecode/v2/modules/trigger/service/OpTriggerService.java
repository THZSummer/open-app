package com.xxx.it.works.wecode.v2.modules.trigger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
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
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class OpTriggerService {

    private static final Logger log = LoggerFactory.getLogger(OpTriggerService.class);

    private static final String TRIGGER_NODE_ID = "node_trigger";

    private final ObjectMapper objectMapper;
    private final ReactiveSequentialExecutor executor;
    private final AuthValidatorRegistry authValidatorRegistry;
    private final UrlWhitelistValidator urlWhitelistValidator;
    private final OpFlowVersionReadRepository flowVersionReadRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final CacheToggle cacheToggle;

    public OpTriggerService(
            ObjectMapper objectMapper,
            AuthValidatorRegistry authValidatorRegistry,
            UrlWhitelistValidator urlWhitelistValidator,
            ReactiveSequentialExecutor executor,
            OpFlowVersionReadRepository flowVersionReadRepository,
            ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            CacheToggle cacheToggle) {
        this.objectMapper = objectMapper;
        this.authValidatorRegistry = authValidatorRegistry;
        this.urlWhitelistValidator = urlWhitelistValidator;
        this.executor = executor;
        this.flowVersionReadRepository = flowVersionReadRepository;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.cacheToggle = cacheToggle;
    }

    /**
     * 加载流版本配置（带 Redis 缓存 read-through）
     * <p>
     * 缓存 Key: {@code cp:flow:config:{flowId}}, TTL 120s.
     * 命中时用缓存的 orchestrationConfig 重建最小 entity, 跳过 R2DBC.
     * 未命中时 R2DBC 查询 → 回填 Redis.
     * {@code connector.cache.enabled=false} 时直接穿透 R2DBC.
     * </p>
     */
    private Mono<FlowVersionEntity> loadFlowVersion(Long flowId) {
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
     * 执行 HTTP 触发
     * <p>
     * 1. 查询 flow 版本配置<br>
     * 2. 解析 React Flow 格式编排配置, 查找 trigger/entry 节点<br>
     * 3. 校验 {@code data.type} 子类型<br>
     * 4. 校验 {@code data.authConfig} 认证声明<br>
     * 5. 校验 {@code data.rateLimitConfig} 限流配置<br>
     * 6. 校验 {@code data.inputContract} header / query / body 三段契约<br>
     * 7. 构建 ExecutionContext (结构化 NodeContext: {header, query, body})<br>
     * 8. 执行连接流并返回结果<br>
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

        return loadFlowVersion(flowId)
                .switchIfEmpty(Mono.error(new RuntimeException("Flow not found: " + flowId)))
                .flatMap(flowVersion -> {
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

                    // 7.5 校验 connector 节点 URL 白名单
                    List<String> urlWhitelist = (List<String>) nodeData.get("urlWhitelist");
                    validateConnectorUrlWhitelist(nodes, urlWhitelist);

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

                    // 10. 执行连接流
                    String flowIdStr = String.valueOf(flowId);
                    return executor.execute(context, flowVersion.getOrchestrationConfig())
                            .map(result -> buildTransparentResponse(flowIdStr, result));
                })
                .onErrorResume(e -> {
                    log.error("Trigger invoke failed: flowId={}, error={}", flowId, e.getMessage());
                    String flowIdStr = String.valueOf(flowId);
                    String msg = e.getMessage() != null ? e.getMessage() : "";

                    // 按异常消息分类错误码
                    if (msg.contains("Flow not found")) {
                        return Mono.just(TransparentFlowResponse.preExecutionError(
                                flowIdStr, HttpStatus.NOT_FOUND, "404",
                                "流不存在: " + msg, "Flow not found: " + msg));
                    }
                    if (msg.contains("whitelist") || msg.contains("Whitelist")
                            || msg.contains("URL not in")) {
                        return Mono.just(TransparentFlowResponse.preExecutionError(
                                flowIdStr, HttpStatus.FORBIDDEN, "403",
                                "URL 白名单拒绝: " + msg, "URL whitelist denied: " + msg));
                    }
                    if (msg.contains("token") || msg.contains("Token") || msg.contains("auth")
                            || msg.contains("X-Sys-Token") || msg.contains("认证")) {
                        return Mono.just(TransparentFlowResponse.preExecutionError(
                                flowIdStr, HttpStatus.UNAUTHORIZED, "401",
                                "认证失败: " + msg, "Authentication failed: " + msg));
                    }
                    // 输入校验错误 → 400
                    if (msg.contains("required field") || msg.contains("inputContract")
                            || msg.contains("must be") || msg.contains("Missing required")
                            || e instanceof IllegalArgumentException) {
                        return Mono.just(TransparentFlowResponse.preExecutionError(
                                flowIdStr, HttpStatus.BAD_REQUEST, "400",
                                "请求参数错误: " + msg, "Bad request: " + msg));
                    }
                    // 其他异常 → 500
                    return Mono.just(TransparentFlowResponse.preExecutionError(
                            flowIdStr, HttpStatus.INTERNAL_SERVER_ERROR, "500",
                            "触发执行失败: " + msg, "Trigger execution failed: " + msg));
                });
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

        r.getPlatformHeaders().put("X-Cache-Status", "0"); // 当前未实现缓存
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
     * 校验 connector 节点的目标 URL 是否在白名单中
     * <p>
     * 遍历编排配置中所有 type=connector 节点，提取 data.url 进行白名单校验。
     * 空白名单（null 或空列表）允许所有 URL 通过，不影响存量业务。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private void validateConnectorUrlWhitelist(List<Map<String, Object>> nodes, List<String> urlWhitelist) {
        if (nodes == null || nodes.isEmpty()) return;

        for (Map<String, Object> node : nodes) {
            String nodeType = (String) node.get("type");
            if (!"connector".equals(nodeType)) continue;

            Map<String, Object> connData = (Map<String, Object>) node.get("data");
            if (connData == null) continue;

            String url = (String) connData.get("url");
            if (url == null || url.isEmpty()) continue;

            if (!urlWhitelistValidator.validate(url, urlWhitelist)) {
                throw new RuntimeException("URL not in whitelist: " + url);
            }
        }
    }
}
