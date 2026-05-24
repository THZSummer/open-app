package com.xxx.it.works.wecode.v2.modules.runtime.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 连接器节点执行器
 * <p>
 * 通过 WebClient 同步调用下游 HTTP API
 * - 读取 connectionConfig.protocolConfig.url/method/headers
 * - 注入 credentials 到请求头
 * - WebClient timeout + .timeout(Duration) 双重保障
 * </p>
 */
public class ConnectorNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ConnectorNodeExecutor.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    /** 默认超时时间 (30秒) */
    private static final long DEFAULT_TIMEOUT_MS = 30000;

    public ConnectorNodeExecutor(ObjectMapper objectMapper, WebClient webClient) {
        this.objectMapper = objectMapper;
        this.webClient = webClient;
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

            log.debug("Connector node executing: nodeId={}", nodeId);

            // 构建 WebClient 请求
            String connectorId = (String) config.get("connectorId");
            long timeoutMs = DEFAULT_TIMEOUT_MS;

            // 获取上游输入数据
            Map<String, Object> inputData = collectInputData(context, config);

            // 获取凭证
            Map<String, String> credentials = null;
            if (context.getCredentials() != null && connectorId != null) {
                credentials = context.getCredentials().get(connectorId);
            }

            // 从 connectionConfig 提取协议信息 (需从 DB 查询或由编排配置传入)
            // MVP 简化: 连接器配置的 URL/method/headers 在编排节点配置中
            String url = (String) config.get("url");
            String method = (String) config.getOrDefault("method", "POST");
            Integer timeout = (Integer) config.get("timeoutMs");
            if (timeout != null && timeout > 0) {
                timeoutMs = timeout;
            }

            Map<String, String> headers = new HashMap<>();
            Object headersObj = config.get("headers");
            if (headersObj instanceof Map) {
                headers.putAll((Map<String, String>) headersObj);
            }

            // 注入凭证到请求头
            if (credentials != null) {
                for (Map.Entry<String, String> entry : credentials.entrySet()) {
                    headers.put(entry.getKey(), entry.getValue());
                }
            }

            // 执行 HTTP 调用
            return executeHttpCall(nodeId, url, method, headers, inputData, timeoutMs, context);
        }).flatMap(output -> Mono.just(output));
    }

    /**
     * 执行 HTTP 调用
     */
    @SuppressWarnings("unchecked")
    private NodeOutput executeHttpCall(
            String nodeId, String url, String method,
            Map<String, String> headers, Map<String, Object> body,
            long timeoutMs, ExecutionContext context) {

        long startTime = System.currentTimeMillis();
        NodeOutput output = new NodeOutput();
        output.setNodeId(nodeId);
        output.setNodeType("connector");

        try {
            WebClient.RequestBodySpec requestSpec = webClient
                    .method(org.springframework.http.HttpMethod.valueOf(method.toUpperCase()))
                    .uri(URI.create(url))
                    .headers(h -> headers.forEach(h::set));

            if (!"GET".equalsIgnoreCase(method)) {
                requestSpec.bodyValue(body != null ? body : new HashMap<>());
            }

            // 执行请求并等待响应 (同步获取, 因为有超时保障)
            String responseBody = requestSpec
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .onErrorResume(e -> {
                        log.warn("Connector HTTP call failed: url={}, error={}", url, e.getMessage());
                        return Mono.just("{\"error\":\"" + e.getMessage() + "\"}");
                    })
                    .block(Duration.ofMillis(timeoutMs + 5000)); // 额外5秒缓冲

            // 解析响应
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
            output.setOutputData(outputData);
            output.setStatus("success");

        } catch (Exception e) {
            log.error("Connector node execution failed: nodeId={}, url={}, error={}", nodeId, url, e.getMessage());

            Map<String, Object> errorData = new HashMap<>();
            errorData.put("__status", "failed");
            errorData.put("__error", e.getMessage());
            output.setOutputData(errorData);
            output.setStatus("failed");
            output.setErrorMessage(e.getMessage());
        }

        output.setDurationMs(System.currentTimeMillis() - startTime);
        return output;
    }

    /**
     * 收集上游输入数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> collectInputData(ExecutionContext context, Map<String, Object> config) {
        Map<String, Object> inputData = new HashMap<>();

        // 获取输入映射配置
        Object inputMappings = config.get("inputMappings");
        if (inputMappings instanceof List) {
            List<Map<String, String>> mappings = (List<Map<String, String>>) inputMappings;
            for (Map<String, String> mapping : mappings) {
                String source = mapping.get("source");
                String target = mapping.get("target");
                if (source != null && target != null) {
                    Object value = context.resolveFieldReference(source);
                    inputData.put(target, value);
                }
            }
        }

        return inputData;
    }
}