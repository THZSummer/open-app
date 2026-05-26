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
        // v5.5: 触发数据放入 input 分区, output 分区仅存元数据
        assertEquals("test_user", output.getInput().get("sender"));
        assertEquals("hello", output.getInput().get("content"));
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
    @DisplayName("ExitNodeExecutor - 按outputMapping提取")
    void testExitNodeExecutor_WithOutputFields() {
        context.setNodeOutput("node_upstream", Map.of("result", "ok", "count", 42));

        ExitNodeExecutor executor = new ExitNodeExecutor(objectMapper);

        // v5.5: 使用 data.outputMapping.{header, body} 替代扁平 outputFields
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> bodyMapping = new HashMap<>();
        bodyMapping.put("result", "${node_upstream.result}");
        bodyMapping.put("count", "${node_upstream.count}");
        data.put("outputMapping", Map.of("body", bodyMapping));

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "node_exit");
        nodeConfig.put("type", "exit");
        nodeConfig.put("data", data);

        NodeOutput output = executor.execute(context, nodeConfig).block();

        assertNotNull(output);
        assertEquals("success", output.getStatus());
        // v5.5: outputMapping.body 映射到 output 分区的 body 字段
        Map<String, Object> body = (Map<String, Object>) output.getOutput().get("body");
        assertNotNull(body);
        assertEquals("ok", body.get("result"));
        assertEquals(42, body.get("count"));
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