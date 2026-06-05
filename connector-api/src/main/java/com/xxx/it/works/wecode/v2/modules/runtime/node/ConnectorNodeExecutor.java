package com.xxx.it.works.wecode.v2.modules.runtime.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.auth.credential.CredentialInjectorRegistry;
import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersionEntity;
import com.xxx.it.works.wecode.v2.modules.connector.repository.OpConnectorVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.expression.ExpressionResolver;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import com.xxx.it.works.wecode.v2.common.config.CacheToggle;
import reactor.core.publisher.Mono;

import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 连接器节点执行器
 * <p>
 * v5.7:
 * <ul>
 *   <li>从 {@code data.connectorVersionId} 查找 {@code connector_version_t.connection_config}</li>
 *   <li>从 connectionConfig 提取 {@code protocolConfig.url/method}、{@code authConfig}、{@code timeoutMs}</li>
 *   <li>从 {@code data.inputMapping} 结构化配置构建请求参数</li>
 *   <li>向后兼容: 无 connectorVersionId 时从 data 直接读取 url/method</li>
 * </ul>
 * </p>
 */
public class ConnectorNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ConnectorNodeExecutor.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final ExpressionResolver expressionResolver;
    private final CredentialInjectorRegistry credentialInjectorRegistry;
    private final OpConnectorVersionReadRepository connectorVersionReadRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final CacheToggle cacheToggle;

    /** 默认超时时间 (30秒) */
    private static final long DEFAULT_TIMEOUT_MS = 30000;

    public ConnectorNodeExecutor(ObjectMapper objectMapper, WebClient webClient,
                                  CredentialInjectorRegistry credentialInjectorRegistry,
                                  OpConnectorVersionReadRepository connectorVersionReadRepository,
                                  ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                                  CacheToggle cacheToggle) {
        this.objectMapper = objectMapper;
        this.webClient = webClient;
        this.expressionResolver = new ExpressionResolver();
        this.credentialInjectorRegistry = credentialInjectorRegistry;
        this.connectorVersionReadRepository = connectorVersionReadRepository;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.cacheToggle = cacheToggle;
    }

    /**
     * 加载连接器版本配置（带 Redis 缓存 read-through）
     * <p>
     * 缓存 Key: {@code cp:conn:config:{connVersionId}}, TTL 300s.
     * 命中时用缓存的 connectionConfig 重建最小 entity, 跳过 R2DBC.
     * {@code connector.cache.enabled=false} 时直接穿透 R2DBC.
     * </p>
     */
    private Mono<ConnectorVersionEntity> loadConnectorVersion(Long connectorVersionId) {
        if (!cacheToggle.isEnabled()) {
            return connectorVersionReadRepository.findById(connectorVersionId);
        }
        String key = "cp:conn:config:" + connectorVersionId;
        return reactiveRedisTemplate.opsForValue().get(key)
                .flatMap(cachedJson -> {
                    ConnectorVersionEntity entity = new ConnectorVersionEntity();
                    entity.setConnectionConfig(cachedJson);
                    entity.setId(connectorVersionId);
                    return Mono.just(entity);
                })
                .switchIfEmpty(
                    connectorVersionReadRepository.findById(connectorVersionId)
                            .flatMap(entity -> reactiveRedisTemplate.opsForValue()
                                    .set(key, entity.getConnectionConfig(), Duration.ofSeconds(300))
                                    .thenReturn(entity))
                );
    }

    @Override
    public String getNodeType() {
        return "connector";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeOutput> execute(ExecutionContext context, Object nodeConfig) {
        Map<String, Object> config;
        if (nodeConfig instanceof Map) {
            config = (Map<String, Object>) nodeConfig;
        } else {
            config = objectMapper.convertValue(nodeConfig, Map.class);
        }

        String nodeId = (String) config.get("id");

        // v5.5: React Flow 格式 — 节点配置在 data 字段内
        Map<String, Object> data = (Map<String, Object>) config.getOrDefault("data", config);

        log.info("Connector node executing: nodeId={}", nodeId);

        // v5.7: 从 connectorVersionId 查找连接器配置
        Object connectorVersionIdObj = data.get("connectorVersionId");
        if (connectorVersionIdObj != null) {
            Long connectorVersionId;
            if (connectorVersionIdObj instanceof Number) {
                connectorVersionId = ((Number) connectorVersionIdObj).longValue();
            } else {
                connectorVersionId = Long.valueOf(connectorVersionIdObj.toString());
            }
            return loadConnectorVersion(connectorVersionId)
                    .flatMap(versionEntity -> executeWithConnectionConfig(context, config, data, nodeId, versionEntity))
                    .switchIfEmpty(Mono.defer(() -> executeLegacy(context, config, data, nodeId)));
        }

        // 向后兼容: 无 connectorVersionId 时从 data 直接读取
        return executeLegacy(context, config, data, nodeId);
    }

    /**
     * v5.7: 从 connectionConfig 提取配置并执行
     */
    @SuppressWarnings("unchecked")
    private Mono<NodeOutput> executeWithConnectionConfig(ExecutionContext context,
                                                          Map<String, Object> config,
                                                          Map<String, Object> data,
                                                          String nodeId,
                                                          ConnectorVersionEntity versionEntity) {
        Map<String, Object> connectionConfig = versionEntity.parseConnectionConfig(objectMapper);
        if (connectionConfig.isEmpty()) {
            log.warn("Connector version {} has empty connectionConfig, falling back to legacy", versionEntity.getId());
            return executeLegacy(context, config, data, nodeId);
        }

        // 提取 protocolConfig
        Map<String, Object> protocolConfig = (Map<String, Object>) connectionConfig.get("protocolConfig");
        String url = null;
        String method = "POST";
        if (protocolConfig != null) {
            url = (String) protocolConfig.get("url");
            Object m = protocolConfig.get("method");
            if (m != null) {
                method = m.toString();
            }
        } else {
            // 向后兼容: protocolConfig 可能直接在顶层
            url = (String) connectionConfig.get("url");
            Object m = connectionConfig.get("method");
            if (m != null) {
                method = m.toString();
            }
        }

        if (url == null || url.isBlank()) {
            log.warn("No url found in connectionConfig for connector version {}, falling back to legacy",
                    versionEntity.getId());
            return executeLegacy(context, config, data, nodeId);
        }

        // 提取 timeoutMs
        long timeoutMs = DEFAULT_TIMEOUT_MS;
        Object timeoutObj = connectionConfig.get("timeoutMs");
        if (timeoutObj instanceof Number) {
            timeoutMs = ((Number) timeoutObj).longValue();
        }

        // 提取 authConfig
        Map<String, Object> authConfig = versionEntity.getAuthConfig(objectMapper);

        // v5.6: 从 data.inputMapping 结构化配置构建请求
        Map<String, Object> params = buildRequestParams(data, context);
        Map<String, String> headers = (Map<String, String>) params.get("headers");
        Map<String, Object> queryParams = (Map<String, Object>) params.get("queryParams");
        Map<String, Object> requestBody = (Map<String, Object>) params.get("requestBody");

        // 注入凭证到请求头
        credentialInjectorRegistry.inject(authConfig, headers);

        // 构建 input 分区
        Map<String, Object> input = new HashMap<>();
        input.put("url", url);
        input.put("method", method);
        input.put("connectorVersionId", versionEntity.getId());
        input.put("headers", new HashMap<>(headers));
        input.put("body", new HashMap<>(requestBody));
        if (queryParams != null && !queryParams.isEmpty()) {
            input.put("query", new HashMap<>(queryParams));
        }

        long startTime = System.currentTimeMillis();
        String finalUrl = url;
        String finalMethod = method;

        return executeHttpCallReactive(nodeId, finalUrl, finalMethod, headers, queryParams,
                requestBody, timeoutMs, context, input, startTime);
    }

    /**
     * 向后兼容: 从 data 直接读取 url/method (旧格式)
     */
    @SuppressWarnings("unchecked")
    private Mono<NodeOutput> executeLegacy(ExecutionContext context, Map<String, Object> config,
                                            Map<String, Object> data, String nodeId) {
        log.info("Connector node executing (legacy mode): nodeId={}", nodeId);

        long timeoutMs = DEFAULT_TIMEOUT_MS;

        String url = (String) data.get("url");
        String method = (String) data.getOrDefault("method", "POST");
        Integer timeout = (Integer) data.get("timeoutMs");
        if (timeout != null && timeout > 0) {
            timeoutMs = timeout;
        }

        Map<String, Object> params = buildRequestParams(data, context);
        Map<String, String> headers = (Map<String, String>) params.get("headers");
        Map<String, Object> queryParams = (Map<String, Object>) params.get("queryParams");
        Map<String, Object> requestBody = (Map<String, Object>) params.get("requestBody");

        Map<String, Object> authConfig = (Map<String, Object>) data.get("authConfig");
        credentialInjectorRegistry.inject(authConfig, headers);

        Map<String, Object> input = new HashMap<>();
        input.put("url", url);
        input.put("method", method);
        input.put("headers", new HashMap<>(headers));
        input.put("body", new HashMap<>(requestBody));
        if (queryParams != null && !queryParams.isEmpty()) {
            input.put("query", new HashMap<>(queryParams));
        }

        long startTime = System.currentTimeMillis();
        return executeHttpCallReactive(nodeId, url, method, headers, queryParams,
                requestBody, timeoutMs, context, input, startTime);
    }

    /**
     * v5.6: 从映射字段提取值，兼容旧格式（裸字符串）和新格式（{type, value} 对象）
     */
    @SuppressWarnings("unchecked")
    private Object extractMappedValue(Object fieldDef) {
        if (fieldDef instanceof Map) {
            return ((Map<String, Object>) fieldDef).get("value");
        }
        return null;
    }

    /**
     * v5.6: 规范化映射段（{type, properties: {key: {type, value}}}）
     * 返回 {字段名 -> 表达式值} 的 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeMappingSegment(Object segment) {
        if (!(segment instanceof Map)) {
            return Collections.emptyMap();
        }
        Map<String, Object> segMap = (Map<String, Object>) segment;

        Object props = segMap.get("properties");
        if (props instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) props).entrySet()) {
                result.put(entry.getKey(), extractMappedValue(entry.getValue()));
            }
            return result;
        }

        return Collections.emptyMap();
    }

    /**
     * 构建 HTTP 请求参数 (从 inputMapping 提取)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRequestParams(Map<String, Object> data, ExecutionContext context) {
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> queryParams = new HashMap<>();
        Map<String, Object> requestBody = new HashMap<>();

        Object inputMappingObj = data.get("inputMapping");
        if (!(inputMappingObj instanceof Map)) {
            return Map.of("headers", headers, "queryParams", queryParams, "requestBody", requestBody);
        }
        Map<String, Object> inputMapping = (Map<String, Object>) inputMappingObj;

        // 处理 header 映射
        Map<String, Object> headerMap = normalizeMappingSegment(inputMapping.get("header"));
        for (Map.Entry<String, Object> entry : headerMap.entrySet()) {
            Object resolved = resolveValue(context, entry.getValue());
            if (resolved != null) {
                headers.put(entry.getKey(), String.valueOf(resolved));
            }
        }

        // 处理 query 映射
        Map<String, Object> queryMap = normalizeMappingSegment(inputMapping.get("query"));
        for (Map.Entry<String, Object> entry : queryMap.entrySet()) {
            Object resolved = resolveValue(context, entry.getValue());
            if (resolved != null) {
                queryParams.put(entry.getKey(), resolved);
            }
        }

        // 处理 body 映射
        Map<String, Object> bodyMap = normalizeMappingSegment(inputMapping.get("body"));
        for (Map.Entry<String, Object> entry : bodyMap.entrySet()) {
            Object resolved = resolveValue(context, entry.getValue());
            if (resolved != null) {
                requestBody.put(entry.getKey(), resolved);
            }
        }

        return Map.of("headers", headers, "queryParams", queryParams, "requestBody", requestBody);
    }

    @SuppressWarnings("unchecked")
    private Object resolveValue(ExecutionContext context, Object value) {
        if (value instanceof String) {
            String expr = (String) value;
            return expressionResolver.resolve(expr, context.getNodeContexts());
        }
        return value;
    }

    /**
     * 全 reactive 方式执行 HTTP 调用
     */
    private Mono<NodeOutput> executeHttpCallReactive(
            String nodeId, String url, String method,
            Map<String, String> headers, Map<String, Object> queryParams,
            Map<String, Object> body,
            long timeoutMs, ExecutionContext context, Map<String, Object> input,
            long startTime) {

        NodeOutput output = new NodeOutput();
        output.setNodeId(nodeId);
        output.setNodeType("connector");
        output.setInput(input);

        // 构建 URI，包含 query 参数（UriComponentsBuilder 自动处理 URL 编码）
        String requestUri;
        if (queryParams != null && !queryParams.isEmpty()) {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                uriBuilder.queryParam(entry.getKey(), entry.getValue());
            }
            requestUri = uriBuilder.build(true).toUriString();
        } else {
            requestUri = url;
        }

        WebClient.RequestBodySpec requestSpec = webClient
                .method(org.springframework.http.HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)))
                .uri(URI.create(requestUri))
                .headers(h -> headers.forEach(h::set));

        if (!"GET".equalsIgnoreCase(method) && body != null && !body.isEmpty()) {
            requestSpec.bodyValue(body);
        }

        // 全 reactive 链: 无 .block()
        return requestSpec
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .map(responseBody -> {
                    log.info("Connector HTTP call succeeded: nodeId={}, status=success", nodeId);
                    Map<String, Object> outputData = new HashMap<>();
                    if (responseBody != null) {
                        try {
                            Map<String, Object> parsed = objectMapper.readValue(responseBody, Map.class);
                            outputData.putAll(parsed);
                        } catch (Exception e) {
                            outputData.put("rawResponse", responseBody);
                        }
                    }
                    outputData.put("__status", "success");
                    output.setOutput(outputData);
                    output.setStatus("success");
                    output.setDurationMs(System.currentTimeMillis() - startTime);
                    return output;
                })
                .onErrorResume(e -> {
                    log.warn("Connector HTTP call failed: url={}, error={}", url, e.getMessage());

                    Map<String, Object> errorInfo = new HashMap<>();
                    errorInfo.put("code", "6001");
                    errorInfo.put("message", e.getMessage());
                    errorInfo.put("messageEn", e.getMessage());
                    errorInfo.put("messageZh", e.getMessage());

                    Map<String, Object> outputData = new HashMap<>();
                    outputData.put("__status", "failed");
                    output.setOutput(outputData);
                    output.setStatus("failed");
                    output.setErrorInfo(errorInfo);
                    output.setDurationMs(System.currentTimeMillis() - startTime);
                    return Mono.just(output);
                });
    }
}
