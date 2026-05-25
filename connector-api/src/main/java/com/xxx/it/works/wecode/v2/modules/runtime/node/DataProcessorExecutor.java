package com.xxx.it.works.wecode.v2.modules.runtime.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
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
 * 支持 3 种字段映射:
 * 1. 源字段→目标字段: 将上游某节点的输出字段映射到目标字段
 * 2. 常量赋值: 直接设置固定值
 * 3. 路径引用解析: ${nodeId.fieldName} 从上下文引用
 * </p>
 */
public class DataProcessorExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(DataProcessorExecutor.class);

    private final ObjectMapper objectMapper;

    public DataProcessorExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

            log.debug("DataProcessor node executing: nodeId={}", nodeId);

            Map<String, Object> outputData = new HashMap<>();
            Map<String, Object> inputData = new HashMap<>();

            // 处理字段映射配置
            Object fieldMappings = config.get("fieldMappings");
            if (fieldMappings instanceof List) {
                List<Map<String, Object>> mappings = (List<Map<String, Object>>) fieldMappings;
                for (Map<String, Object> mapping : mappings) {
                    String targetField = (String) mapping.get("targetField");
                    Object sourceValue = mapping.get("sourceValue");
                    String sourceType = (String) mapping.getOrDefault("sourceType", "reference");

                    if (targetField == null) continue;

                    Object resolvedValue;
                    switch (sourceType) {
                        case "constant":
                            // 常量赋值
                            resolvedValue = sourceValue;
                            break;
                        case "reference":
                        default:
                            // 路径引用: ${nodeId.fieldName}
                            if (sourceValue instanceof String) {
                                resolvedValue = context.resolveFieldReference((String) sourceValue);
                            } else {
                                resolvedValue = context.resolveFieldReference(String.valueOf(sourceValue));
                            }
                            break;
                    }

                    if (resolvedValue != null) {
                        outputData.put(targetField, resolvedValue);
                    }

                    inputData.put(targetField, sourceValue);
                }
            }

            // 标记元数据
            outputData.put("__status", "success");
            outputData.put("__input", inputData);

            NodeOutput output = new NodeOutput(nodeId, "data_processor", outputData);
            output.setStatus("success");

            log.debug("DataProcessor node completed: nodeId={}, fields={}", nodeId, outputData.keySet());
            return output;
        });
    }
}