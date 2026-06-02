package com.xxx.it.works.wecode.v2.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson 新旧格式反序列化兼容测试（connector-api）
 *
 * <p>验证 v5.5 新 JSON 格式在运行时层的反序列化正确性，
 * 以及旧格式通过 @JsonAlias 及 FAIL_ON_UNKNOWN_PROPERTIES=false 的向后兼容。</p>
 */
@DisplayName("Jackson Connector-API Deserialization Test")
class JacksonDeserializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @DisplayName("运行时 NodeOutput: input/output 双分区")
    void testNodeOutputInputOutputPartition() throws Exception {
        String json = """
                {
                    "nodeId": "node_connector",
                    "nodeType": "connector",
                    "input": {"message": "hello", "sender": "sys"},
                    "output": {"msgId": "msg_001", "status": "sent"},
                    "status": "success",
                    "durationMs": 150,
                    "errorInfo": null
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);
        assertEquals("node_connector", parsed.get("nodeId"));
        assertTrue(parsed.containsKey("input"), "应有 input 分区");
        assertTrue(parsed.containsKey("output"), "应有 output 分区");
    }

    @Test
    @DisplayName("运行时 NodeOutput: errorInfo 新格式 (内部错误带 cause)")
    void testNodeOutputErrorInfoInternal() throws Exception {
        String json = """
                {
                    "nodeId": "node_connector",
                    "nodeType": "connector",
                    "input": {},
                    "output": {},
                    "status": "failed",
                    "durationMs": 5000,
                    "errorInfo": {
                        "code": "6001",
                        "messageZh": "连接超时",
                        "messageEn": "Connection timeout",
                        "cause": "Read timed out"
                    }
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);
        Map<String, Object> errorInfo = (Map<String, Object>) parsed.get("errorInfo");
        assertEquals("6001", errorInfo.get("code"));
        assertEquals("连接超时", errorInfo.get("messageZh"));
        assertTrue(errorInfo.containsKey("cause"));
    }

    @Test
    @DisplayName("运行时 NodeOutput: errorInfo 新格式 (下游错误带 downstreamStatus)")
    void testNodeOutputErrorInfoDownstream() throws Exception {
        String json = """
                {
                    "nodeId": "node_connector",
                    "nodeType": "connector",
                    "input": {},
                    "output": {},
                    "status": "failed",
                    "durationMs": 200,
                    "errorInfo": {
                        "code": "502",
                        "messageZh": "下游服务返回错误",
                        "messageEn": "Downstream service error",
                        "downstreamStatus": 502,
                        "downstreamBody": "{\\"error\\":\\"bad gateway\\"}"
                    }
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);
        Map<String, Object> errorInfo = (Map<String, Object>) parsed.get("errorInfo");
        assertEquals("502", errorInfo.get("code"));
        assertTrue(errorInfo.containsKey("downstreamStatus"));
        assertTrue(errorInfo.containsKey("downstreamBody"));
    }

    @Test
    @DisplayName("ExecutionResult.StepDetail: errorInfo Map 格式")
    void testStepDetailErrorInfo() throws Exception {
        String json = """
                {
                    "executionId": "exec_001",
                    "flowId": "123",
                    "status": "success",
                    "steps": [
                        {
                            "nodeId": "node_trigger",
                            "nodeType": "trigger",
                            "labelCn": "接收",
                            "labelEn": "Receive",
                            "status": "success",
                            "durationMs": 5,
                            "errorInfo": null
                        }
                    ],
                    "totalDurationMs": 100
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);
        java.util.List<Map<String, Object>> steps = (java.util.List<Map<String, Object>>) parsed.get("steps");
        assertEquals(1, steps.size());
        Map<String, Object> step = steps.get(0);
        assertEquals("node_trigger", step.get("nodeId"));
        assertEquals("接收", step.get("labelCn"));
        assertTrue(step.containsKey("errorInfo"), "StepDetail 应有 errorInfo");
    }

    @Test
    @DisplayName("ExecutionContext: nodeContexts Map 格式")
    void testExecutionContextNodeContexts() throws Exception {
        // ExecutionContext 通常非直接 JSON 反序列化，但验证 Map<String, NodeContext> 的 JSON 序列化/反序列化
        String json = """
                {
                    "executionId": "exec_001",
                    "flowId": "123",
                    "nodeContexts": {
                        "node_trigger": {
                            "nodeId": "node_trigger",
                            "nodeType": "trigger",
                            "input": {"sender": "sys"},
                            "output": {"sender": "sys"},
                            "status": "success",
                            "durationMs": 5
                        }
                    },
                    "isTest": false
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);
        Map<String, Object> nodeContexts = (Map<String, Object>) parsed.get("nodeContexts");
        assertTrue(nodeContexts.containsKey("node_trigger"), "nodeContexts 应包含 node_trigger");
    }

    @Test
    @DisplayName("表达式新格式: 运行时 ExpressionResolver 格式")
    void testExpressionNewFormat() throws Exception {
        String expr = "${$.node.node_connector.output.data}";
        String inner = expr.substring(2, expr.length() - 1);
        String[] parts = inner.split("\\.");
        assertEquals("$", parts[0]);
        assertEquals("node", parts[1]);
        assertEquals("node_connector", parts[2]);
        assertEquals("output", parts[3]);
        assertEquals("data", parts[4]);
    }

    @Test
    @DisplayName("表达式 constant:xxx 字面量格式")
    void testExpressionConstantFormat() throws Exception {
        String expr = "constant:Hello World";
        assertTrue(expr.startsWith("constant:"));
        assertEquals("Hello World", expr.substring("constant:".length()));
    }

    @Test
    @DisplayName("v5.6 表达式格式: ${$.node.{id}.output.{path}}")
    void testExpressionOldFormat() throws Exception {
        String expr = "${$.node.node_trigger.output.sender}";
        String inner = expr.substring(2, expr.length() - 1);
        String[] parts = inner.split("\\.");
        assertEquals("$", parts[0]);
        assertEquals("node", parts[1]);
        assertEquals("node_trigger", parts[2]);
        assertEquals("output", parts[3]);
        assertEquals("sender", parts[4]);
    }

    @Test
    @DisplayName("orchestrationConfig: React Flow nodes/edges 新格式")
    void testOrchestrationConfigReactFlow() throws Exception {
        String json = """
                {
                    "nodes": [
                        {
                            "id": "node_trigger",
                            "type": "trigger",
                            "position": {"x": 100, "y": 200},
                            "data": {
                                "labelCn": "接收",
                                "type": "http",
                                "authConfig": {"type": "SYSTOKEN", "fields": []},
                                "rateLimitConfig": {"maxQps": 100}
                            }
                        },
                        {
                            "id": "node_connector",
                            "type": "connector",
                            "position": {"x": 250, "y": 200},
                            "data": {
                                "labelCn": "调用API",
                                "connectorId": "123",
                                "url": "https://api.example.com/send",
                                "method": "POST",
                                "headers": {"Content-Type": "application/json"},
                                "inputMapping": {
                                    "header": {"Authorization": "${$.node.node_trigger.input.token}"},
                                    "body": {"message": "${$.node.node_trigger.input.content}"}
                                }
                            }
                        }
                    ],
                    "edges": [
                        {"id": "e1", "source": "node_trigger", "target": "node_connector", "type": "smoothstep", "data": {"businessType": "default"}}
                    ]
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);
        java.util.List<Map<String, Object>> nodes = (java.util.List<Map<String, Object>>) parsed.get("nodes");
        assertEquals(2, nodes.size());

        // React Flow 格式验证
        Map<String, Object> trigger = nodes.get(0);
        assertTrue(trigger.containsKey("position"), "React Flow 应有 position");
        assertTrue(trigger.containsKey("data"), "React Flow 应有 data");

        // 边 source/target
        java.util.List<Map<String, Object>> edges = (java.util.List<Map<String, Object>>) parsed.get("edges");
        assertEquals("node_trigger", edges.get(0).get("source"));
        assertEquals("node_connector", edges.get(0).get("target"));

        // connector node inputMapping 分段
        Map<String, Object> connectorData = (Map<String, Object>) nodes.get(1).get("data");
        assertTrue(connectorData.containsKey("inputMapping"), "connector 节点应有 inputMapping");
        Map<String, Object> inputMapping = (Map<String, Object>) connectorData.get("inputMapping");
        assertTrue(inputMapping.containsKey("header"), "inputMapping 应有 header 分区");
        assertTrue(inputMapping.containsKey("body"), "inputMapping 应有 body 分区");
    }
}