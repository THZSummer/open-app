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
    @DisplayName("TriggerNodeExecutor - 结构化触发数据 {header, query, body}")
    void testTriggerNodeExecutor() {
        TriggerNodeExecutor executor = new TriggerNodeExecutor(objectMapper);
        context.setTriggerData(Map.of("sender", "test_user", "content", "hello"));

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "node_trigger");
        nodeConfig.put("type", "trigger");

        NodeOutput output = executor.execute(context, nodeConfig).block();

        assertNotNull(output);
        assertEquals("node_trigger", output.getNodeId());
        assertEquals("trigger", output.getNodeType());
        assertEquals("success", output.getStatus());
        // v5.7: 结构化 input = {header, query, body}
        Map<String, Object> body = (Map<String, Object>) output.getInput().get("body");
        assertNotNull(body, "input 应有 body 分区");
        assertEquals("test_user", body.get("sender"));
        assertEquals("hello", body.get("content"));
        assertTrue(output.getInput().containsKey("header"), "input 应有 header 分区");
        assertTrue(output.getInput().containsKey("query"), "input 应有 query 分区");
    }

    @Test
    @DisplayName("TriggerNodeExecutor - 无触发数据时输出为空")
    void testTriggerNodeExecutor_NoTriggerData() {
        TriggerNodeExecutor executor = new TriggerNodeExecutor(objectMapper);

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "node_trigger");
        nodeConfig.put("type", "trigger");

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
                Map.of("targetField", "userName", "sourceValue", "${$.node.node_upstream.output.name}", "sourceType", "reference"),
                Map.of("targetField", "userAge", "sourceValue", "${$.node.node_upstream.output.age}", "sourceType", "reference")
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

        // v5.6: 使用 mappedJsonSchemaObjectDef 格式
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> resultField = Map.of("type", "string", "value", "${$.node.node_upstream.output.result}");
        Map<String, Object> countField = Map.of("type", "integer", "value", "${$.node.node_upstream.output.count}");
        Map<String, Object> bodyMapping = Map.of("type", "object", "properties", Map.of("result", resultField, "count", countField));
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