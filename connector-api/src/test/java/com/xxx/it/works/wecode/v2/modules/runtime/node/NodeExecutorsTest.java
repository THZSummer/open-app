package com.xxx.it.works.wecode.v2.modules.runtime.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("节点执行器测试")
class NodeExecutorsTest {

    private ObjectMapper objectMapper;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        context = new ExecutionContext("exec-test", "flow-test");
    }

    @Test
    @DisplayName("EntryNodeExecutor - 透传触发数据")
    void testEntryNodeExecutor() {
        EntryNodeExecutor executor = new EntryNodeExecutor(objectMapper);
        context.setTriggerData(Map.of("sender", "test_user", "content", "hello"));

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "node_entry");
        nodeConfig.put("type", "entry");

        NodeOutput output = executor.execute(context, nodeConfig).block();

        assertNotNull(output);
        assertEquals("node_entry", output.getNodeId());
        assertEquals("entry", output.getNodeType());
        assertEquals("success", output.getStatus());
        assertEquals("test_user", output.getOutputData().get("sender"));
        assertEquals("hello", output.getOutputData().get("content"));
    }

    @Test
    @DisplayName("EntryNodeExecutor - 无触发数据时输出为空")
    void testEntryNodeExecutor_NoTriggerData() {
        EntryNodeExecutor executor = new EntryNodeExecutor(objectMapper);

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "node_entry");
        nodeConfig.put("type", "entry");

        NodeOutput output = executor.execute(context, nodeConfig).block();
        assertNotNull(output);
        assertTrue(output.getOutputData().containsKey("__status"));
    }

    @Test
    @DisplayName("DataProcessorExecutor - 路径引用字段映射")
    void testDataProcessor_FieldMapping() {
        // 准备上游数据
        context.setNodeOutput("node_upstream", Map.of("name", "Alice", "age", 30));

        DataProcessorExecutor executor = new DataProcessorExecutor(objectMapper);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> fieldMappings = List.of(
                Map.of("targetField", "userName", "sourceValue", "${node_upstream.name}", "sourceType", "reference"),
                Map.of("targetField", "userAge", "sourceValue", "${node_upstream.age}", "sourceType", "reference")
        );

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "node_processor");
        nodeConfig.put("type", "data_processor");
        nodeConfig.put("fieldMappings", fieldMappings);

        NodeOutput output = executor.execute(context, nodeConfig).block();

        assertNotNull(output);
        assertEquals("success", output.getStatus());
        assertEquals("Alice", output.getOutputData().get("userName"));
        assertEquals(30, output.getOutputData().get("userAge"));
    }

    @Test
    @DisplayName("DataProcessorExecutor - 常量赋值")
    void testDataProcessor_Constant() {
        DataProcessorExecutor executor = new DataProcessorExecutor(objectMapper);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> fieldMappings = List.of(
                Map.of("targetField", "fixedValue", "sourceValue", "hello_const", "sourceType", "constant")
        );

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "node_processor");
        nodeConfig.put("fieldMappings", fieldMappings);

        NodeOutput output = executor.execute(context, nodeConfig).block();
        assertNotNull(output);
        assertEquals("hello_const", output.getOutputData().get("fixedValue"));
    }

    @Test
    @DisplayName("ExitNodeExecutor - 按outputFields提取")
    void testExitNodeExecutor_WithOutputFields() {
        context.setNodeOutput("node_upstream", Map.of("result", "ok", "count", 42));

        ExitNodeExecutor executor = new ExitNodeExecutor(objectMapper);

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "node_exit");
        nodeConfig.put("type", "exit");
        nodeConfig.put("outputFields", List.of("node_upstream.result", "node_upstream.count"));

        NodeOutput output = executor.execute(context, nodeConfig).block();

        assertNotNull(output);
        assertEquals("success", output.getStatus());
        // outputFields 取值后取最后一个字段名
        assertEquals("ok", output.getOutputData().get("result"));
        assertEquals(42, output.getOutputData().get("count"));
    }

    @Test
    @DisplayName("ExitNodeExecutor - 无outputFields时收集所有非元数据字段")
    void testExitNodeExecutor_NoOutputFields() {
        context.setNodeOutput("node_a", Map.of("field1", "val1", "__meta", "ignore"));
        context.setNodeOutput("node_b", Map.of("field2", "val2"));

        ExitNodeExecutor executor = new ExitNodeExecutor(objectMapper);

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "node_exit");
        nodeConfig.put("type", "exit");

        NodeOutput output = executor.execute(context, nodeConfig).block();

        assertNotNull(output);
        // 应该包含 field1, field2，不包含 __meta
        assertTrue(output.getOutputData().containsKey("field1") || output.getOutputData().containsKey("node_a_field1"));
        assertTrue(output.getOutputData().containsKey("field2") || output.getOutputData().containsKey("node_b_field2"));
        assertFalse(output.getOutputData().containsValue("ignore"));
    }
}