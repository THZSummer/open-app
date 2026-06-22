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
 *   "trigger": { "input": {...} }
 * }
 * }</pre>
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
     * @param upstreamNodeIds 上游节点 ID 列表, 为 null 时默认不组装节点数据
     * @return 嵌套 Map, 结构: { nodeId: { input, output }, trigger: { input } }
     */
    public Map<String, Object> assembleCtx(ExecutionContext execCtx, List<String> upstreamNodeIds) {
        Map<String, Object> ctx = new LinkedHashMap<>();

        // 1. 组装上游节点数据 (指针引用, 不深拷贝)
        if (upstreamNodeIds != null) {
            for (String nodeId : upstreamNodeIds) {
                NodeContext nodeCtx = execCtx.getNodeContext(nodeId);
                if (nodeCtx != null) {
                    Map<String, Object> nodeData = new LinkedHashMap<>();
                    // 使用指针引用, 避免深拷贝开销
                    nodeData.put("input", nodeCtx.getInput() != null ? nodeCtx.getInput() : new LinkedHashMap<>());
                    nodeData.put("output", nodeCtx.getOutput() != null ? nodeCtx.getOutput() : new LinkedHashMap<>());
                    ctx.put(nodeId, nodeData);
                } else {
                    log.debug("Node context not found for upstream node: {}", nodeId);
                }
            }
        }

        // 2. 组装 trigger 触发数据
        Map<String, Object> triggerData = new LinkedHashMap<>();
        triggerData.put("input", execCtx.getTriggerData() != null ? execCtx.getTriggerData() : new LinkedHashMap<>());
        ctx.put("trigger", triggerData);

        log.debug("Assembled script ctx: upstreamNodeCount={}, hasTrigger={}",
                upstreamNodeIds != null ? upstreamNodeIds.size() : 0,
                execCtx.getTriggerData() != null);

        return ctx;
    }
}
