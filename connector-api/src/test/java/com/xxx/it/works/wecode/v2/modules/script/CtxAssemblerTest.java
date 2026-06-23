package com.xxx.it.works.wecode.v2.modules.script;

import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CtxAssembler 测试")
class CtxAssemblerTest {

    private CtxAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new CtxAssembler();
    }

    @Test
    @DisplayName("组装上游节点数据 — 嵌套 Map {nodeId: {input, output}}")
    void testAssembleCtx_WithUpstreamNodes() {
        ExecutionContext execCtx = new ExecutionContext("exec-001", "100");
        execCtx.setTriggerData(Map.of("input", Map.of("body", Map.of("key", "value"))));

        // 设置上游节点上下文
        NodeContext nodeCtx1 = new NodeContext();
        nodeCtx1.setNodeId("node-1");
        Map<String, Object> input1 = new HashMap<>();
        input1.put("a", 1);
        Map<String, Object> output1 = new HashMap<>();
        output1.put("b", 2);
        nodeCtx1.setInput(input1);
        nodeCtx1.setOutput(output1);
        execCtx.setNodeContext(nodeCtx1);

        Map<String, Object> ctx = assembler.assembleCtx(execCtx, Arrays.asList("node-1"));

        assertTrue(ctx.containsKey("node-1"));
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) ctx.get("node-1");
        assertEquals(input1, nodeData.get("input"));
        assertEquals(output1, nodeData.get("output"));
    }

    @Test
    @DisplayName("组装 trigger 数据 — 包含 trigger.input")
    void testAssembleCtx_TriggerData() {
        ExecutionContext execCtx = new ExecutionContext("exec-002", "100");
        Map<String, Object> triggerBody = new HashMap<>();
        triggerBody.put("userId", "u123");
        execCtx.setTriggerData(Map.of("input", Map.of("body", triggerBody)));

        Map<String, Object> ctx = assembler.assembleCtx(execCtx, null);

        assertTrue(ctx.containsKey("trigger"));
        @SuppressWarnings("unchecked")
        Map<String, Object> triggerData = (Map<String, Object>) ctx.get("trigger");
        assertTrue(triggerData.containsKey("input"));
    }

    @Test
    @DisplayName("upstreamNodeIds 为 null → 只组装 trigger")
    void testAssembleCtx_NullUpstreamOnlyTrigger() {
        ExecutionContext execCtx = new ExecutionContext("exec-003", "100");
        execCtx.setTriggerData(Map.of("input", "body-data"));

        Map<String, Object> ctx = assembler.assembleCtx(execCtx, null);

        assertTrue(ctx.containsKey("trigger"));
        assertEquals(1, ctx.size()); // 只有 trigger
    }

    @Test
    @DisplayName("上游节点不存在 → 跳过该节点")
    void testAssembleCtx_NodeNotFound_Skipped() {
        ExecutionContext execCtx = new ExecutionContext("exec-004", "100");
        execCtx.setTriggerData(Map.of("input", Map.of()));

        Map<String, Object> ctx = assembler.assembleCtx(execCtx, Arrays.asList("nonexistent"));

        assertTrue(ctx.containsKey("trigger"));
        assertFalse(ctx.containsKey("nonexistent"));
    }

    @Test
    @DisplayName("指针引用 — 修改原始数据不影响 ctx")
    void testPointerReference_Shared() {
        ExecutionContext execCtx = new ExecutionContext("exec-005", "100");
        execCtx.setTriggerData(Map.of("input", Map.of("body", Map.of("key", "original"))));

        NodeContext nodeCtx = new NodeContext();
        nodeCtx.setNodeId("node-1");
        Map<String, Object> nodeInput = new HashMap<>();
        nodeInput.put("data", "original-value");
        nodeCtx.setInput(nodeInput);
        nodeCtx.setOutput(new HashMap<>());
        execCtx.setNodeContext(nodeCtx);

        Map<String, Object> ctx = assembler.assembleCtx(execCtx, Arrays.asList("node-1"));

        // 修改原始数据
        nodeInput.put("data", "modified-value");

        // ctx 中使用指针引用，因此也发生了变化（指针引用行为）
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) ctx.get("node-1");
        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) nodeData.get("input");
        assertEquals("modified-value", input.get("data"));
    }
}
