package com.xxx.it.works.wecode.v2.modules.runtime.node;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 连接器节点执行器
 * <p>
 * v5.5: 
 * <ul>
 *   <li>访问 {@code data.inputMapping} 结构化 {@code {header, query, body}} 替代扁平 {@code inputMappings}</li>
 *   <li>NodeConfig 位于 {@code config.data.*} (React Flow 格式)</li>
 *   <li>使用 {@link ExpressionResolver} 解析表达式</li>
 *   <li>错误信息使用 {@code errorInfo} 结构化 Map</li>
 * </ul>
 * 通过 WebClient reactive 调用下游 HTTP API (全 reactive 无 .block()).
 * </p>
 */
public class ConnectorNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ConnectorNodeExecutor.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final ExpressionResolver expressionResolver;

    /** 默认超时时间 (30秒) */
    private static final long DEFAULT_TIMEOUT_MS = 30000;

    public ConnectorNodeExecutor(ObjectMapper objectMapper, WebClient webClient) {
        this.objectMapper = objectMapper;
        this.webClient = webClient;
        this.expressionResolver = new ExpressionResolver();
    }

    @Override
    public String getNodeType() {
        return "connector";
    }

@Override
    @SuppressWarnings("unchecked")
    public Mono<NodeOutput> execute(ExecutionContext context, Object nodeConfig) {
        return Mono.fromCallable(() -> {
            Map<String, Object> config;
            if (nodeConfig instanceof Map) {
                config = (Map<String, Object>) nodeConfig;
            } else {
                config = objectMapper.convertValue(nodeConfig, Map.class);
            }

            String nodeId = (String) config.get("id");

            // v5.5: React Flow 格式 — 节点配置在 data 字段内
            Map<String, Object> data = (Map<String, Object>) config.getOrDefault("data", config);

            log.debug("Connector node executing: nodeId={}", nodeId);

            long timeoutMs = DEFAULT_TIMEOUT_MS;

            // 获取凭证
            String connectorId = (String) data.get("connectorId");
            Map<String, String> credentials = null;
            if (context.getCredentials() != null && connectorId != null) {
                credentials = context.getCredentials().get(connectorId);
            }

            // v5.5: 从 data 字段读取 url/method/timeoutMs
            String url = (String) data.get("url");
            String method = (String) data.getOrDefault("method", "POST");
            Integer timeout = (Integer) data.get("timeoutMs");
            if (timeout != null && timeout > 0) {
                timeoutMs = timeout;
            }

            // v5.5: 从 data.inputMapping 结构化配置构建请求
            // inputMapping 格式: {header: {...}, query: {...}, body: {...}}
            Map<String, String> headers = new HashMap<>();
            Map<String, Object> queryParams = new HashMap<>();
            Map<String, Object> requestBody = new HashMap<>();

            Object inputMappingObj = data.get("inputMapping");
            if (inputMappingObj instanceof Map) {
                Map<String, Object> inputMapping = (Map<String, Object>) inputMappingObj;

                // 处理 header 映射
                Object headerMapping = inputMapping.get("header");
                Map<String, Object> headerMap = normalizeMappingSegment(headerMapping);
                for (Map.Entry<String, Object> entry : headerMap.entrySet()) {
                    Object resolved = resolveValue(context, entry.getValue());
                    if (resolved != null) {
                        headers.put(entry.getKey(), String.valueOf(resolved));
                    }
                }

                // 处理 query 映射
                Object queryMapping = inputMapping.get("query");
                Map<String, Object> queryMap = normalizeMappingSegment(queryMapping);
                for (Map.Entry<String, Object> entry : queryMap.entrySet()) {
                    Object resolved = resolveValue(context, entry.getValue());
                    if (resolved != null) {
                        queryParams.put(entry.getKey(), resolved);
                    }
                }

                // 处理 body 映射
                Object bodyMapping = inputMapping.get("body");
                Map<String, Object> bodyMap = normalizeMappingSegment(bodyMapping);
                for (Map.Entry<String, Object> entry : bodyMap.entrySet()) {
                    Object resolved = resolveValue(context, entry.getValue());
                    if (resolved != null) {
                        requestBody.put(entry.getKey(), resolved);
                    }
                }
            }

            // 注入凭证到请求头
            if (credentials != null) {
                for (Map.Entry<String, String> entry : credentials.entrySet()) {
                    headers.put(entry.getKey(), entry.getValue());
                }
            }

            // 构建 input 分区: 记录本节点的输入
            Map<String, Object> input = new HashMap<>();
            input.put("url", url);
            input.put("method", method);
            input.put("headers", new HashMap<>(headers));
            input.put("body", new HashMap<>(requestBody));
            if (queryParams != null && !queryParams.isEmpty()) {
                input.put("query", new HashMap<>(queryParams));
            }

            long startTime = System.currentTimeMillis();

            // 返回参数元组用于后续 reactive 调用
            return Arrays.asList(nodeId, url, method, headers, queryParams, requestBody, timeoutMs, context, input, startTime);
        }).flatMap(params -> {
            // 解包参数并执行 HTTP 调用 (全 reactive 方式)
            String nodeId = (String) params.get(0);
            String url = (String) params.get(1);
            String method = (String) params.get(2);
            Map<String, String> headers = (Map<String, String>) params.get(3);
            Map<String, Object> queryParams = (Map<String, Object>) params.get(4);
            Map<String, Object> requestBody = (Map<String, Object>) params.get(5);
            long timeoutMs = (Long) params.get(6);
            ExecutionContext ctx = (ExecutionContext) params.get(7);
            Map<String, Object> input = (Map<String, Object>) params.get(8);
            long startTime = (Long) params.get(9);
            return executeHttpCallReactive(nodeId, url, method, headers, queryParams, requestBody, timeoutMs, ctx, input, startTime);
        });
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
        String requestUri = url;
        if (queryParams != null && !queryParams.isEmpty()) {
            StringBuilder uriBuilder = new StringBuilder(url);
            boolean first = !url.contains("?");
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                uriBuilder.append(first ? '?' : '&')
                        .append(entry.getKey())
                        .append('=')
                        .append(entry.getValue());
                first = false;
            }
            requestUri = uriBuilder.toString();
        }

        WebClient.RequestBodySpec requestSpec = webClient
                .method(org.springframework.http.HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)))
                .uri(URI.create(requestUri))
                .headers(h -> headers.forEach(h::set));

        if (!"GET".equalsIgnoreCase(method)) {
            requestSpec.bodyValue(body != null && !body.isEmpty() ? body : new HashMap<>());
        }

        // 全 reactive 链: 无 .block()
        return requestSpec
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .map(responseBody -> {
                    // 解析响应为 output 分区
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

                    // v5.5: 使用结构化 errorInfo
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