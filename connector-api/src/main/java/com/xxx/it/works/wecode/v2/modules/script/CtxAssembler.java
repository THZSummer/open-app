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
 * <p>
 * 采用 DAG 拓扑自发现: 脚本执行时 ExecutionContext 中所有已完成节点自动纳入 ctx,
 * 不依赖前端 {@code upstreamNodeIds} (防止并行分支中 connector 遗漏).
 * </p>
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

    /**
     * 组装脚本执行上下文 Map
     *
     * @param execCtx         执行上下文 (含所有已执行节点的 NodeContext)
     * @param upstreamNodeIds 上游节点 ID 列表, 为 null 时默认只组装 trigger 节点
     * @return 嵌套 Map, 结构: { nodeId: { input, output }, trigger: { input: {header, query, body} } }
     */
    public Map<String, Object> assembleCtx(ExecutionContext execCtx, List<String> upstreamNodeIds) {
        Map<String, Object> ctx = new LinkedHashMap<>();

        // 1. 组装所有已执行节点的数据 (DAG 拓扑保证此时所有上游节点已完成)
        //    不依赖前端传的 upstreamNodeIds (可能遗漏, 如并行分支中的 connector)
        for (Map.Entry<String, NodeContext> entry : execCtx.getNodeContexts().entrySet()) {
            String nodeId = entry.getKey();
            NodeContext nodeCtx = entry.getValue();
            Map<String, Object> nodeData = new LinkedHashMap<>();
            nodeData.put("input", nodeCtx.getInput() != null ? nodeCtx.getInput() : new LinkedHashMap<>());
            nodeData.put("output", nodeCtx.getOutput() != null ? nodeCtx.getOutput() : new LinkedHashMap<>());
            ctx.put(nodeId, nodeData);
        }

        // 2. 组装 trigger 触发数据 (v5.7 结构化格式: {header, query, body})
        // 动态查找 trigger 类型节点 (不再硬编码 ID), 同时以 "trigger" 别名暴露给脚本
        String triggerNodeId = findTriggerNodeId(execCtx);
        NodeContext triggerNodeCtx = triggerNodeId != null ? execCtx.getNodeContext(triggerNodeId) : null;
        Map<String, Object> triggerInput;
        if (triggerNodeCtx != null && triggerNodeCtx.getInput() != null) {
            triggerInput = new LinkedHashMap<>(triggerNodeCtx.getInput());
            log.info("Script ctx: trigger node found, id='{}', accessible via ctx.trigger and ctx.{}",
                    triggerNodeId, triggerNodeId);
        } else {
            // 降级: 从 ExecutionContext 原始字段构建结构化 input
            log.warn("Script ctx: trigger NodeContext not found (triggerNodeId={}), falling back to raw ExecutionContext fields",
                    triggerNodeId != null ? triggerNodeId : "(none)");
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

        log.info("Assembled script ctx: nodeCount={}, hasTrigger={}",
                execCtx.getNodeContexts().size(),
                triggerNodeCtx != null);

        return ctx;
    }

    /**
     * 从执行上下文中按节点类型查找 trigger 节点 ID
     */
    private String findTriggerNodeId(ExecutionContext execCtx) {
        for (Map.Entry<String, NodeContext> entry : execCtx.getNodeContexts().entrySet()) {
            if ("trigger".equals(entry.getValue().getNodeType())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
