package com.xxx.it.works.wecode.v2.modules.script;

import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 脚本执行上下文组装器
 * <p>
 * 将上游节点的 input/output 数据组装为嵌套 Map, 作为 GraalJS 脚本的 {@code main(ctx)} 参数.
 * 使用指针引用 (非深拷贝) 以保证性能.
 * </p>
 *
 * <h3>组装结构</h3>
 * <pre>{@code
 * {
 *   "nodeId": { "input": {...}, "output": {...} },
 *   "trigger": { "input": { "header": {...}, "query": {...}, "body": {...} } }
 * }
 * }</pre>
 *
 * <p>
 * v5.7: trigger 段 input 使用结构化格式 {@code {header, query, body}},
 * 与 TriggerNodeExecutor 保持一致, 使脚本可通过 {@code ctx.trigger.input.body.xxx} 访问请求体字段.
 * </p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Component
public class CtxAssembler {

    private static final String TRIGGER_NODE_ID = "node_trigger";

    /**
     * 组装脚本执行上下文 Map
     *
     * @param execCtx         执行上下文 (含所有已执行节点的 NodeContext)
     * @param upstreamNodeIds 上游节点 ID 列表, 为 null 时默认只组装 trigger 节点
     * @return 嵌套 Map, 结构: { nodeId: { input, output }, trigger: { input: {header, query, body} } }
     */
    public Map<String, Object> assembleCtx(ExecutionContext execCtx, List<String> upstreamNodeIds) {
        Map<String, Object> ctx = new LinkedHashMap<>();

        // 1. 组装上游节点数据 (指针引用, 不深拷贝)
        List<String> effectiveUpstream = upstreamNodeIds;
        if (effectiveUpstream == null || effectiveUpstream.isEmpty()) {
            // 未显式指定上游节点时, 包含所有已执行节点
            effectiveUpstream = new java.util.ArrayList<>(execCtx.getNodeContexts().keySet());
        }

        for (String nodeId : effectiveUpstream) {
            NodeContext nodeCtx = execCtx.getNodeContext(nodeId);
            if (nodeCtx != null) {
                Map<String, Object> nodeData = new LinkedHashMap<>();
                nodeData.put("input", nodeCtx.getInput() != null ? nodeCtx.getInput() : new LinkedHashMap<>());
                nodeData.put("output", nodeCtx.getOutput() != null ? nodeCtx.getOutput() : new LinkedHashMap<>());
                ctx.put(nodeId, nodeData);
            } else {
                log.debug("Node context not found for upstream node: {}", nodeId);
            }
        }

        // 2. 组装 trigger 触发数据 (v5.7 结构化格式: {header, query, body})
        // 优先从 trigger NodeContext 获取已构建的结构化 input
        NodeContext triggerNodeCtx = execCtx.getNodeContext(TRIGGER_NODE_ID);
        Map<String, Object> triggerInput;
        if (triggerNodeCtx != null && triggerNodeCtx.getInput() != null) {
            triggerInput = new LinkedHashMap<>(triggerNodeCtx.getInput());
        } else {
            // 降级: 从 ExecutionContext 原始字段构建结构化 input
            triggerInput = new LinkedHashMap<>();
            triggerInput.put("header", execCtx.getTriggerHeaders() != null
                    ? new LinkedHashMap<>(execCtx.getTriggerHeaders()) : new LinkedHashMap<>());
            triggerInput.put("query", execCtx.getTriggerQueryParams() != null
                    ? new LinkedHashMap<>(execCtx.getTriggerQueryParams()) : new LinkedHashMap<>());
            triggerInput.put("body", execCtx.getTriggerData() != null
                    ? execCtx.getTriggerData() : new LinkedHashMap<>());
        }

        Map<String, Object> triggerData = new LinkedHashMap<>();
        triggerData.put("input", triggerInput);
        ctx.put("trigger", triggerData);

        log.debug("Assembled script ctx: upstreamNodeCount={}, hasTrigger={}",
                effectiveUpstream.size(),
                execCtx.getTriggerData() != null);

        return ctx;
    }
}
