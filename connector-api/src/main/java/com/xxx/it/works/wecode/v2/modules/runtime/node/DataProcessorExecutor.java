package com.xxx.it.works.wecode.v2.modules.runtime.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.expression.ExpressionResolver;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据处理节点执行器
 * <p>
 * v5.5:
 * <ul>
 *   <li>使用 {@link ExpressionResolver} 解析表达式</li>
 *   <li>使用 {@code input}/{@code output} 双分区 NodeOutput</li>
 *   <li>支持 {@code constant:xxx} 和 {@code ${...}} 表达式格式</li>
 * </ul>
 * 支持 3 种字段映射:
 * <ol>
 *   <li>源字段→目标字段: 将上游某节点的输出字段映射到目标字段</li>
 *   <li>常量赋值: 直接设置固定值 ({@code constant:xxx})</li>
 *   <li>路径引用解析: {@code ${nodeId.fieldPath}} / {@code ${$.node.{nodeId}.{input/output}.{path}}}</li>
 * </ol>
 * </p>
 */
public class DataProcessorExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(DataProcessorExecutor.class);

    private final ObjectMapper objectMapper;
    private final ExpressionResolver expressionResolver;

    public DataProcessorExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.expressionResolver = new ExpressionResolver();
    }

    @Override
    public String getNodeType() {
        return "data_processor";
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

            log.info("DataProcessor node executing: nodeId={}", nodeId);

            Map<String, Object> inputRecords = new HashMap<>();
            Map<String, Object> outputData = new HashMap<>();

            // 处理字段映射配置 (v5.5: 仍使用 fieldMappings, 但表达式解析使用 ExpressionResolver)
            Object fieldMappings = data.get("fieldMappings");
            if (fieldMappings instanceof List) {
                List<Map<String, Object>> mappings = (List<Map<String, Object>>) fieldMappings;
                for (Map<String, Object> mapping : mappings) {
                    String targetField = (String) mapping.get("targetField");
                    Object sourceValue = mapping.get("sourceValue");
                    String sourceType = (String) mapping.getOrDefault("sourceType", "reference");

                    if (targetField == null) { continue; }

                    Object resolvedValue;
                    switch (sourceType) {
                        case "constant":
                            // 常量赋值: 直接使用
                            resolvedValue = sourceValue;
                            break;
                        case "reference":
                        default:
                            // 使用 ExpressionResolver 解析 (支持三种模式)
                            if (sourceValue instanceof String) {
                                resolvedValue = expressionResolver.resolve((String) sourceValue, context.getNodeContexts());
                            } else {
                                resolvedValue = expressionResolver.resolve(String.valueOf(sourceValue), context.getNodeContexts());
                            }
                            break;
                    }

                    if (resolvedValue != null) {
                        outputData.put(targetField, resolvedValue);
                    }

                    inputRecords.put(targetField, sourceValue);
                }
            }

            // 标记元数据
            outputData.put("__status", "success");

            // v5.5: input/output 双分区
            NodeOutput result = new NodeOutput(nodeId, "data_processor", inputRecords, outputData);
            result.setStatus("success");

            log.info("DataProcessor node completed: nodeId={}, fields={}", nodeId, outputData.keySet());
            return result;
        });
    }
}