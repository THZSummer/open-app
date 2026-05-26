package com.xxx.it.works.wecode.v2.modules.runtime.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
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
 * v5.5: 触发数据作为 {@code input} 分区, {@code output} 分区记录执行元数据.
 * 透传触发数据到 ExecutionContext.
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
    public Mono<NodeOutput> execute(ExecutionContext context, Object nodeConfig) {
        Map<String, Object> config;
        if (nodeConfig instanceof Map) {
            config = (Map<String, Object>) nodeConfig;
        } else {
            config = objectMapper.convertValue(nodeConfig, Map.class);
        }

        // v5.5: React Flow 格式 — 节点配置在 data 字段内
        Map<String, Object> data = (Map<String, Object>) config.getOrDefault("data", config);

        String nodeId = (String) config.get("id");
        log.debug("Entry node executing: nodeId={}", nodeId);

        // input 分区: 触发数据
        Map<String, Object> input = new HashMap<>();
        if (context.getTriggerData() != null) {
            input.putAll(context.getTriggerData());
        }

        // output 分区: 元数据
        Map<String, Object> output = new HashMap<>();
        output.put("__status", "success");

        NodeOutput result = new NodeOutput(nodeId, "entry", input, output);
        result.setStatus("success");

        log.debug("Entry node completed: nodeId={}", nodeId);
        return Mono.just(result);
    }
}