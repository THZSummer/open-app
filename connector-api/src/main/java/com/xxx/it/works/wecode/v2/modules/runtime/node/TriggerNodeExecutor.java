package com.xxx.it.works.wecode.v2.modules.runtime.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 入口节点执行器 (v5.7)
 * <p>
 * 结构化 trigger input: {header: {...}, query: {...}, body: {...}}
 * 优先复用 OpTriggerService 预置的 NodeContext, 否则从 triggerData/triggerHeaders/triggerQueryParams 构建.
 * </p>
 */
public class TriggerNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(TriggerNodeExecutor.class);

    private final ObjectMapper objectMapper;

    public TriggerNodeExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getNodeType() {
        return "trigger";
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
        log.info("Entry node executing: nodeId={}", nodeId);

        // 优先复用 OpTriggerService 预置的结构化 NodeContext
        Map<String, Object> input;
        NodeContext preSeeded = context.getNodeContext(nodeId);
        if (preSeeded != null && preSeeded.getInput() != null
                && preSeeded.getInput().containsKey("header")
                && preSeeded.getInput().containsKey("body")) {
            input = new HashMap<>(preSeeded.getInput());
        } else {
            // 降级: 从 context 的独立字段构建结构化 input
            input = buildStructuredInput(context);
        }

        log.info("Entry node trigger input structure: header={}, query={}, bodyKeys={}",
                input.get("header") instanceof Map ? ((Map) input.get("header")).size() + " fields" : "null",
                input.get("query") instanceof Map ? ((Map) input.get("query")).size() + " fields" : "null",
                input.get("body") instanceof Map ? ((Map) input.get("body")).keySet() : "null");

        Map<String, Object> output = new HashMap<>();
        output.put("__status", "success");

        NodeOutput result = new NodeOutput(nodeId, "trigger", input, output);
        result.setStatus("success");

        log.info("Entry node completed: nodeId={}", nodeId);
        return Mono.just(result);
    }

    /**
     * 降级构建结构化 trigger input (v5.7: query 参数自动数值转换)
     */
    private Map<String, Object> buildStructuredInput(ExecutionContext context) {
        Map<String, Object> input = new HashMap<>();

        Map<String, Object> headerPart = new HashMap<>();
        if (context.getTriggerHeaders() != null) {
            headerPart.putAll(context.getTriggerHeaders());
        }
        input.put("header", headerPart);

        Map<String, Object> queryPart = new HashMap<>();
        if (context.getTriggerQueryParams() != null) {
            for (Map.Entry<String, String> entry : context.getTriggerQueryParams().entrySet()) {
                queryPart.put(entry.getKey(), coerceValue(entry.getValue()));
            }
        }
        input.put("query", queryPart);

        Map<String, Object> bodyPart = new HashMap<>();
        if (context.getTriggerData() != null) {
            bodyPart.putAll(context.getTriggerData());
        }
        input.put("body", bodyPart);

        return input;
    }

    /**
     * 将字符串值自动转换为合适的数值类型
     */
    private Object coerceValue(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                return value;
            }
        }
    }
}
