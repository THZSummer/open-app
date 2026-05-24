package com.xxx.it.works.wecode.v2.modules.runtime.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ExecutionContextProvider;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 出口节点执行器
 * <p>
 * 按 outputFields 从上下文提取字段构造返回值
 * </p>
 */
public class ExitNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ExitNodeExecutor.class);

    private final ObjectMapper objectMapper;

    public ExitNodeExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getNodeType() {
        return "exit";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeOutput> execute(ExecutionContextProvider provider, Object nodeConfig) {
        return Mono.fromCallable(() -> {
            Map<String, Object> config;
            if (nodeConfig instanceof Map) {
                config = (Map<String, Object>) nodeConfig;
            } else {
                config = objectMapper.convertValue(nodeConfig, Map.class);
            }

            ExecutionContext context = provider.getContext();
            String nodeId = (String) config.get("id");

            log.debug("Exit node executing: nodeId={}", nodeId);

            Map<String, Object> outputData = new HashMap<>();

            // 按 outputFields 从上下文提取
            Object outputFields = config.get("outputFields");
            if (outputFields instanceof List) {
                List<String> fields = (List<String>) outputFields;
                for (String field : fields) {
                    Object value = context.resolveFieldReference("${" + field + "}");
                    if (value != null) {
                        String fieldName = field.contains(".") ? field.substring(field.lastIndexOf('.') + 1) : field;
                        outputData.put(fieldName, value);
                    }
                }
            } else {
                // 无 outputFields 配置, 尝试收集所有节点输出
                for (Map.Entry<String, Map<String, Object>> entry : context.getNodeOutputs().entrySet()) {
                    String nodeId2 = entry.getKey();
                    Map<String, Object> nodeData = entry.getValue();
                    if (nodeData != null) {
                        for (Map.Entry<String, Object> field : nodeData.entrySet()) {
                            if (!field.getKey().startsWith("__")) { // 跳过元数据
                                outputData.put(nodeId2 + "_" + field.getKey(), field.getValue());
                            }
                        }
                    }
                }
            }

            // 标记元数据
            outputData.put("__status", "success");

            NodeOutput output = new NodeOutput(nodeId, "exit", outputData);
            output.setStatus("success");

            log.debug("Exit node completed: nodeId={}, outputFields={}", nodeId, outputData.keySet());
            return output;
        });
    }
}