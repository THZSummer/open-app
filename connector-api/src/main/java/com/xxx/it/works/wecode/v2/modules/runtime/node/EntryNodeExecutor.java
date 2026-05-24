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
import java.util.Map;

/**
 * 入口节点执行器
 * <p>
 * 透传触发数据到 ExecutionContext
 * 触发数据作为下游节点的输入
 * </p>
 */
public class EntryNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(EntryNodeExecutor.class);

    private final ObjectMapper objectMapper;

    public EntryNodeExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getNodeType() {
        return "entry";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeOutput> execute(ExecutionContextProvider provider, Object nodeConfig) {
        return Mono.fromCallable(() -> {
            ExecutionContext context = provider.getContext();
            Map<String, Object> config;
            if (nodeConfig instanceof Map) {
                config = (Map<String, Object>) nodeConfig;
            } else {
                config = objectMapper.convertValue(nodeConfig, Map.class);
            }

            log.debug("Entry node executing: nodeId={}", config.get("id"));

            // 透传触发数据作为输出
            Map<String, Object> outputData = new HashMap<>();
            if (context.getTriggerData() != null) {
                outputData.putAll(context.getTriggerData());
            }

            // 标记元数据
            outputData.put("__status", "success");

            NodeOutput output = new NodeOutput(
                    (String) config.get("id"),
                    "entry",
                    outputData
            );
            output.setStatus("success");

            log.debug("Entry node completed: nodeId={}", config.get("id"));
            return output;
        });
    }
}