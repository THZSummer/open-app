package com.xxx.it.works.wecode.v2.modules.runtime.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.expression.ExpressionResolver;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 出口节点执行器
 * <p>
 * v5.5:
 * <ul>
 *   <li>访问 {@code data.outputMapping} 结构化 {@code {header, body}} 替代扁平 {@code outputFields}</li>
 *   <li>使用 {@link ExpressionResolver} 解析表达式</li>
 *   <li>构建包含 {@code header}/{@code body} 分区的响应</li>
 * </ul>
 * </p>
 */
public class ExitNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ExitNodeExecutor.class);

    private final ObjectMapper objectMapper;
    private final ExpressionResolver expressionResolver;

    public ExitNodeExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.expressionResolver = new ExpressionResolver();
    }

    @Override
    public String getNodeType() {
        return "exit";
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

            log.info("Exit node executing: nodeId={}", nodeId);

            // input 分区: 记录配的 outputMapping
            Map<String, Object> input = new HashMap<>();

            // output 分区: 构建最终响应
            Map<String, Object> outputData = new HashMap<>();

            // v5.5: 从 data.outputMapping 结构化配置构建响应
            // outputMapping 格式: {header: {...}, body: {...}}
            Object outputMappingObj = data.get("output");
            if (outputMappingObj instanceof Map) {
                Map<String, Object> outputMapping = (Map<String, Object>) outputMappingObj;

                // 存储原始 outputMapping 配置到 input 分区
                input.put("output", new HashMap<>(outputMapping));

                // 处理 header 映射
                Object headerMapping = outputMapping.get("header");
                Map<String, Object> responseHeaders = new HashMap<>();
                Map<String, Object> headerMap = normalizeMappingSegment(headerMapping);
                for (Map.Entry<String, Object> entry : headerMap.entrySet()) {
                    Object resolved = resolveValue(context, entry.getValue());
                    if (resolved != null) {
                        responseHeaders.put(entry.getKey(), resolved);
                    }
                }
                outputData.put("header", responseHeaders);

                // 处理 body 映射
                Object bodyMapping = outputMapping.get("body");
                if (bodyMapping instanceof String) {
                    Object resolved = resolveValue(context, bodyMapping);
                    if (resolved != null) {
                        outputData.put("body", resolved);
                    }
                } else {
                    Map<String, Object> responseBody = new HashMap<>();
                    Map<String, Object> bodyMap = normalizeMappingSegment(bodyMapping);
                    // 单一 value 键 → 直接透传, 不包裹在 Map 中
                    if (bodyMap.size() == 1 && bodyMap.containsKey("value")) {
                        Object resolved = resolveValue(context, bodyMap.get("value"));
                        if (resolved != null) {
                            outputData.put("body", resolved);
                        }
                    } else {
                        for (Map.Entry<String, Object> entry : bodyMap.entrySet()) {
                            Object resolved = resolveValue(context, entry.getValue());
                            if (resolved != null) {
                                responseBody.put(entry.getKey(), resolved);
                            }
                        }
                        outputData.put("body", responseBody);
                    }
                }

            } else {
                outputData = collectFallbackOutputs(context);
                input.put("output", null);
            }

            // 标记元数据
            outputData.put("__status", "success");

            // v5.5: input/output 双分区 NodeOutput
            NodeOutput result = new NodeOutput(nodeId, "exit", input, outputData);
            result.setStatus("success");

            log.info("Exit node completed: nodeId={}, outputFields={}", nodeId, outputData.keySet());
            return result;
        });
    }


    /**
     * 降级收集所有节点输出 (无 outputMapping 时)
     */
    private Map<String, Object> collectFallbackOutputs(ExecutionContext context) {
        Map<String, Object> outputData = new HashMap<>();
        for (Map.Entry<String, ? extends Object> entry : context.getNodeContexts().entrySet()) {
            Object nodeCtx = entry.getValue();
            if (!(nodeCtx instanceof NodeContext)) {
                continue;
            }
            NodeContext nc = (NodeContext) nodeCtx;
            Map<String, Object> nodeOutput = nc.getOutput();
            if (nodeOutput == null) {
                continue;
            }
            for (Map.Entry<String, Object> field : nodeOutput.entrySet()) {
                if (!field.getKey().startsWith("__")) {
                    outputData.put(entry.getKey() + "_" + field.getKey(), field.getValue());
                }
            }
        }
        outputData.put("__collectMode", true);
        return outputData;
    }

    /**
     * v5.6: 从映射字段提取值（{type, value} 对象）
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

        // {type: "object", properties: {field: {type, value}}}
        Object props = segMap.get("properties");
        if (props instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) props).entrySet()) {
                result.put(entry.getKey(), extractMappedValue(entry.getValue()));
            }
            return result;
        }

        // 扁平: {field: {type, value}}
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : segMap.entrySet()) {
            Object value = extractMappedValue(entry.getValue());
            if (value != null) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }
    @SuppressWarnings("unchecked")
    private Object resolveValue(ExecutionContext context, Object value) {
        if (value instanceof String) {
            String expr = (String) value;
            return expressionResolver.resolve(expr, context.getNodeContexts());
        }
        return value;
    }
}