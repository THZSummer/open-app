package com.xxx.it.works.wecode.v2.modules.runtime.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExecutionContext 测试")
class ExecutionContextTest {

    @Test
    @DisplayName("创建上下文并设置触发数据")
    void testCreateAndSetTriggerData() {
        ExecutionContext ctx = new ExecutionContext("exec-001", "flow-001");
        assertEquals("exec-001", ctx.getExecutionId());
        assertEquals("flow-001", ctx.getFlowId());

        ctx.setTriggerData(Map.of("sender", "test_user"));
        assertEquals("test_user", ctx.getTriggerData().get("sender"));
    }

    @Test
    @DisplayName("节点输出读写")
    void testNodeOutput() {
        ExecutionContext ctx = new ExecutionContext("exec-002", "flow-001");
        ctx.setNodeOutput("node_1", Map.of("data", "value1"));

        Map<String, Object> output = ctx.getNodeOutput("node_1");
        assertNotNull(output);
        assertEquals("value1", output.get("data"));

        // 不存在的节点返回null
        assertNull(ctx.getNodeOutput("node_not_exists"));
    }

    @Test
    @DisplayName("字段引用解析 - v5.6 新语法")
    void testResolveFieldReference_Simple() {
        ExecutionContext ctx = new ExecutionContext("exec-003", "flow-001");
        ctx.setNodeOutput("node_a", Map.of("name", "Alice"));

        Object resolved = ctx.resolveFieldReference("${$.node.node_a.output.name}");
        assertEquals("Alice", resolved);
    }

    @Test
    @DisplayName("字段引用解析 - 常量值原样返回")
    void testResolveFieldReference_Constant() {
        ExecutionContext ctx = new ExecutionContext("exec-004", "flow-001");

        Object resolved = ctx.resolveFieldReference("hello");
        assertEquals("hello", resolved);

        resolved = ctx.resolveFieldReference("42");
        assertEquals("42", resolved);
    }

    @Test
    @DisplayName("字段引用解析 - 嵌套字段 (v5.6)")
    void testResolveFieldReference_Nested() {
        ExecutionContext ctx = new ExecutionContext("exec-005", "flow-001");
        ctx.setNodeOutput("node_b", Map.of("user", Map.of("name", "Bob", "age", 30)));

        Object resolved = ctx.resolveFieldReference("${$.node.node_b.output.user.name}");
        assertEquals("Bob", resolved);

        resolved = ctx.resolveFieldReference("${$.node.node_b.output.user}");
        assertNotNull(resolved);
        assertTrue(resolved instanceof Map);
    }

    @Test
    @DisplayName("测试模式和触发类型")
    void testModeAndTriggerType() {
        ExecutionContext ctx = new ExecutionContext("exec-007", "flow-001");

        assertFalse(ctx.isTest());
        ctx.setTest(true);
        assertTrue(ctx.isTest());

        ctx.setTriggerType(2);
        assertEquals(2, ctx.getTriggerType());
    }

    @Test
    @DisplayName("字段引用 - null安全 (v5.6)")
    void testResolveFieldReference_NullSafe() {
        ExecutionContext ctx = new ExecutionContext("exec-008", "flow-001");

        assertNull(ctx.resolveFieldReference(null));
        assertNull(ctx.resolveFieldReference("${$.node.node_not_exists.output.field}"));
        assertNull(ctx.resolveFieldReference("${$.node.node_without_field.output.x}"));
    }
}