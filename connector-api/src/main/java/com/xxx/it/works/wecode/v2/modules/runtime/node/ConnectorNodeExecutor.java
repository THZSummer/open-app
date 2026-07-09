package com.xxx.it.works.wecode.v2.modules.runtime.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.error.ErrorCode;
import com.xxx.it.works.wecode.v2.modules.auth.credential.UnifiedCredentialProcessor;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.expression.ExpressionResolver;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 连接器节点执行器
 * <p>
 * v6.0: 编排自包含 — 直接从 {@code data.connectorVersionConfig} 快照取连接器配置,
 * 不再查询 connector_version_t.
 * <ul>
 *   <li>从 {@code data.connectorVersionConfig} 快照提取 {@code protocolConfig.url/method}、{@code authConfigs}、{@code timeoutMs}</li>
 *   <li>从 {@code data.input} 结构化配置构建请求参数</li>
 *   <li>无快照时走 legacy 模式 (从 data.protocolConfig 直接读取)</li>
 * </ul>
 * </p>
 */
public class ConnectorNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ConnectorNodeExecutor.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final ExpressionResolver expressionResolver;
    private final UnifiedCredentialProcessor credentialProcessor;

    /** 默认超时时间 (30秒) */
    private static final long DEFAULT_TIMEOUT_MS = 30000;

    public ConnectorNodeExecutor(ObjectMapper objectMapper, WebClient webClient,
                                   UnifiedCredentialProcessor credentialProcessor) {
        this.objectMapper = objectMapper;
        this.webClient = webClient;
        this.expressionResolver = new ExpressionResolver();
        this.credentialProcessor = credentialProcessor;
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

        // React Flow 格式 — 节点配置在 data 字段内
        Map<String, Object> data = (Map<String, Object>) config.getOrDefault("data", config);

        log.info("Connector node executing: nodeId={}", nodeId);

        // v6.0: 优先从 connectorVersionConfig 快照读取 (编排自包含)
        Object snapshotObj = data.get("connectorVersionConfig");
        if (snapshotObj instanceof Map) {
            Map<String, Object> snapshot = (Map<String, Object>) snapshotObj;
            if (!snapshot.isEmpty()) {
                return executeWithSnapshot(context, data, nodeId, snapshot);
            }
        }

        // 无快照时走 legacy 模式 (从 data.protocolConfig 直接读取)
        log.info("No connectorVersionConfig snapshot, using legacy mode: nodeId={}", nodeId);
        return executeLegacy(context, data, nodeId);
    }

    /**
     * v6.0: 从 connectorVersionConfig 快照提取配置并执行
     */
    @SuppressWarnings("unchecked")
    private Mono<NodeOutput> executeWithSnapshot(ExecutionContext context,
                                                  Map<String, Object> data,
                                                  String nodeId,
                                                  Map<String, Object> snapshot) {
        // 提取 protocolConfig
        Map<String, Object> protocolConfig = (Map<String, Object>) snapshot.get("protocolConfig");
        String url = null;
        String method = "POST";
        if (protocolConfig != null) {
            url = (String) protocolConfig.get("url");
            Object m = protocolConfig.get("method");
            if (m != null) {
                method = m.toString();
            }
        }

        if (url == null || url.isBlank()) {
            log.warn("No url found in connectorVersionConfig snapshot for node {}, falling back to legacy", nodeId);
            return executeLegacy(context, data, nodeId);
        }

        // 提取 timeoutMs: node 级 > 快照级 > 默认
        long timeoutMs = DEFAULT_TIMEOUT_MS;
        Object dataTimeoutObj = data.get("timeoutMs");
        if (dataTimeoutObj instanceof Number && ((Number) dataTimeoutObj).longValue() > 0) {
            timeoutMs = ((Number) dataTimeoutObj).longValue();
        } else {
            Object snapshotTimeoutObj = snapshot.get("timeoutMs");
            if (snapshotTimeoutObj instanceof Number && ((Number) snapshotTimeoutObj).longValue() > 0) {
                timeoutMs = ((Number) snapshotTimeoutObj).longValue();
            }
        }

        // 从 data.input 结构化配置构建请求参数
        Map<String, Object> params = buildRequestParams(data, context);
        Map<String, String> headers = (Map<String, String>) params.get("headers");
        Map<String, Object> queryParams = (Map<String, Object>) params.get("queryParams");
        Map<String, Object> requestBody = (Map<String, Object>) params.get("requestBody");

        // v2: 凭证注入
        credentialProcessor.apply(snapshot.get("authConfigs"), headers, queryParams, context);

        // 构建 input 分区
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
     * 向后兼容: 从 data 直接读取 url/method (旧格式)
     */
    @SuppressWarnings("unchecked")
    private Mono<NodeOutput> executeLegacy(ExecutionContext context,
                                            Map<String, Object> data, String nodeId) {
        log.info("Connector node executing (legacy mode): nodeId={}", nodeId);

        long timeoutMs = DEFAULT_TIMEOUT_MS;

        Map<String, Object> protocolConfig = (Map<String, Object>) data.get("protocolConfig");
        String url = protocolConfig != null ? (String) protocolConfig.get("url") : null;
        String method = (protocolConfig != null && protocolConfig.get("method") != null)
                ? protocolConfig.get("method").toString() : "POST";
        Integer timeout = null;
        Object timeoutObj = data.get("timeoutMs");
        if (timeoutObj instanceof Number) {
            timeout = ((Number) timeoutObj).intValue();
        }
        if (timeout != null && timeout > 0) {
            timeoutMs = timeout;
        }

        Map<String, Object> params = buildRequestParams(data, context);
        Map<String, String> headers = (Map<String, String>) params.get("headers");
        Map<String, Object> queryParams = (Map<String, Object>) params.get("queryParams");
        Map<String, Object> requestBody = (Map<String, Object>) params.get("requestBody");

        // v2: 凭证注入已由 UnifiedCredentialProcessor 统一处理
        // legacy 模式下 authConfigs 为空，不做注入

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
     * 从映射字段提取值，兼容旧格式（裸字符串）和新格式（{type, value} 对象）
     */
    @SuppressWarnings("unchecked")
    private Object extractMappedValue(Object fieldDef) {
        if (fieldDef instanceof Map) {
            return ((Map<String, Object>) fieldDef).get("value");
        }
        return null;
    }

    /**
     * 规范化映射段（{type, properties: {key: {type, value}}}）
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
     * 构建 HTTP 请求参数 (从 input 提取)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRequestParams(Map<String, Object> data, ExecutionContext context) {
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> queryParams = new HashMap<>();
        Map<String, Object> requestBody = new HashMap<>();

        // v6.0: 优先从 data.input 读取映射 (新格式)
        Object inputObj = data.get("input");
        if (inputObj instanceof Map) {
            return buildFromInput((Map<String, Object>) inputObj, context);
        }

        // 向后兼容: 从 data.inputMapping 读取 (旧格式)
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
    private Map<String, Object> buildFromInput(Map<String, Object> input, ExecutionContext context) {
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> queryParams = new HashMap<>();
        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> headerMap = normalizeMappingSegment(input.get("header"));
        for (Map.Entry<String, Object> entry : headerMap.entrySet()) {
            Object resolved = resolveValue(context, entry.getValue());
            if (resolved != null) {
                headers.put(entry.getKey(), String.valueOf(resolved));
            }
        }

        Map<String, Object> queryMap = normalizeMappingSegment(input.get("query"));
        for (Map.Entry<String, Object> entry : queryMap.entrySet()) {
            Object resolved = resolveValue(context, entry.getValue());
            if (resolved != null) {
                queryParams.put(entry.getKey(), resolved);
            }
        }

        Map<String, Object> bodyMap = normalizeMappingSegment(input.get("body"));
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

        // 构建 URI，包含 query 参数
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

        return requestSpec
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .map(responseBody -> {
                            log.info("Connector HTTP call succeeded: nodeId={}, status=success", nodeId);
                            Map<String, Object> outputData = new HashMap<>();

                            // response headers (plan-json-schema §3.2: output.header)
                            Map<String, Object> respHeaders = new HashMap<>();
                            response.headers().asHttpHeaders().forEach((k, v) -> {
                                if (v != null && !v.isEmpty()) {
                                    respHeaders.put(k, v.size() == 1 ? v.get(0) : v);
                                }
                            });
                            outputData.put("header", respHeaders);

                            // response body (plan-json-schema §3.2: output.body)
                            Map<String, Object> bodyData = new HashMap<>();
                            if (responseBody != null) {
                                try {
                                    Map<String, Object> parsed = objectMapper.readValue(responseBody, Map.class);
                                    bodyData.putAll(parsed);
                                } catch (Exception e) {
                                    bodyData.put("rawResponse", responseBody);
                                }
                            }
                            outputData.put("body", bodyData);
                            outputData.put("__status", "success");
                            output.setOutput(outputData);
                            output.setStatus("success");
                            output.setDurationMs(System.currentTimeMillis() - startTime);
                            return output;
                        }))
                .onErrorResume(e -> {
                    log.warn("Connector HTTP call failed: url={}, error={}", url, e.getMessage());
                    return Mono.just(buildErrorOutput(nodeId, url, timeoutMs, startTime, input, e));
                });
    }

    private NodeOutput buildErrorOutput(String nodeId, String url, long timeoutMs,
                                         long startTime, Map<String, Object> input, Throwable e) {
        NodeOutput output = new NodeOutput();
        output.setNodeId(nodeId);
        output.setNodeType("connector");
        output.setInput(input);

        String errMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String code;
        String msgZh;

        if (e instanceof java.util.concurrent.TimeoutException || errMsg.contains("timeout")) {
            code = ErrorCode.CONNECTOR_READ_TIMEOUT;
            msgZh = "连接器调用超时（超过" + timeoutMs + "ms），目标地址 [" + url + "] 未在规定时间内响应";
        } else if (errMsg.contains("connection refused")) {
            code = ErrorCode.CONNECTOR_CONNECT_TIMEOUT;
            msgZh = "连接器连接超时，目标地址 [" + url + "] 不可达";
        } else if (errMsg.contains("unknown host") || errMsg.contains("dns")) {
            code = ErrorCode.CONNECTOR_DNS_FAILED;
            msgZh = "连接器目标地址 DNS 解析失败，请检查 URL 配置";
        } else if (errMsg.contains("ssl") || errMsg.contains("certificate")) {
            code = ErrorCode.CONNECTOR_SSL_FAILED;
            msgZh = "连接器 SSL 证书校验失败，目标地址可能使用了不受信任的证书";
        } else {
            code = ErrorCode.CONNECTOR_HTTP_FAILED;
            msgZh = "连接器调用失败: " + e.getMessage();
        }

        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("code", code);
        errorInfo.put("messageZh", msgZh);
        errorInfo.put("messageEn", "Connector call failed: " + e.getMessage());

        Map<String, Object> outputData = new HashMap<>();
        outputData.put("__status", "failed");
        output.setOutput(outputData);
        output.setStatus("failed");
        output.setErrorInfo(errorInfo);
        output.setDurationMs(System.currentTimeMillis() - startTime);
        return output;
    }
}
