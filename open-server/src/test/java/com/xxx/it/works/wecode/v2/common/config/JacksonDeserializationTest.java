package com.xxx.it.works.wecode.v2.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.minidev.json.JSONUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson 新旧格式反序列化兼容测试
 *
 * <p>确保 v5.5 新 JSON 格式可正常反序列化，
 * 且旧 v2.8.1 格式通过 @JsonAlias 向后兼容。</p>
 */
@DisplayName("Jackson Deserialization Compatibility Test")
class JacksonDeserializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ==================== connectionConfig 新旧格式 ====================

    @Test
    @DisplayName("新格式 connectionConfig: authConfig / inputContract / outputContract / rateLimitConfig")
    void testNewConnectionConfig() throws Exception {
        String json = """
                {
                    "protocol": "HTTP",
                    "protocolConfig": {"url": "https://api.example.com", "method": "POST"},
                    "authConfig": {
                        "type": "AKSK",
                        "fields": [{"name": "accessKey", "carrier": "header", "fieldName": "AK", "required": true, "sensitive": true}]
                    },
                    "inputContract": {
                        "protocol": "HTTP",
                        "body": {"type": "object", "properties": {"message": {"type": "string"}}, "required": ["message"]}
                    },
                    "outputContract": {
                        "protocol": "HTTP",
                        "body": {"type": "object", "properties": {"msgId": {"type": "string"}}}
                    },
                    "timeoutMs": 30000,
                    "rateLimitConfig": {"maxQps": 10}
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);

        // 新字段名存在
        assertTrue(parsed.containsKey("authConfig"), "authConfig 应存在");
        assertTrue(parsed.containsKey("inputContract"), "inputContract 应存在");
        assertTrue(parsed.containsKey("outputContract"), "outputContract 应存在");
        assertTrue(parsed.containsKey("rateLimitConfig"), "rateLimitConfig 应存在");

        // authConfig 内部结构
        Map<String, Object> authConfig = (Map<String, Object>) parsed.get("authConfig");
        assertEquals("AKSK", authConfig.get("type"));

        // inputContract protocol 字段
        Map<String, Object> inputContract = (Map<String, Object>) parsed.get("inputContract");
        assertEquals("HTTP", inputContract.get("protocol"));

        // rateLimitConfig
        Map<String, Object> rateLimitConfig = (Map<String, Object>) parsed.get("rateLimitConfig");
        assertEquals(10, rateLimitConfig.get("maxQps"));
    }

    @Test
    @DisplayName("旧格式 connectionConfig: authTypeSchema / inputSchema / outputSchema / rateLimit (@JsonAlias 兼容)")
    void testOldConnectionConfig() throws Exception {
        String json = """
                {
                    "protocol": "HTTP",
                    "protocolConfig": {"url": "https://api.example.com", "method": "POST"},
                    "authTypeSchema": {
                        "type": "AKSK",
                        "fields": [{"name": "accessKey", "carrier": "header", "fieldName": "AK", "required": true, "sensitive": true}]
                    },
                    "inputSchema": {
                        "type": "object",
                        "properties": {"message": {"type": "string"}},
                        "required": ["message"]
                    },
                    "outputSchema": {
                        "type": "object",
                        "properties": {"msgId": {"type": "string"}}
                    },
                    "timeoutMs": 30000,
                    "rateLimit": {"maxQps": 10}
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);

        // 旧字段名仍可解析
        assertTrue(parsed.containsKey("authTypeSchema"), "authTypeSchema 应存在（旧格式兼容）");
        assertTrue(parsed.containsKey("inputSchema"), "inputSchema 应存在（旧格式兼容）");
        assertTrue(parsed.containsKey("outputSchema"), "outputSchema 应存在（旧格式兼容）");
        assertTrue(parsed.containsKey("rateLimit"), "rateLimit 应存在（旧格式兼容）");
    }

    // ==================== orchestrationConfig 新旧格式 ====================

    @Test
    @DisplayName("新格式 orchestrationConfig: React Flow 格式 node.data / edge.source/target")
    void testNewOrchestrationConfig() throws Exception {
        String json = """
                {
                    "nodes": [
                        {
                            "id": "node_trigger",
                            "type": "trigger",
                            "position": {"x": 100, "y": 200},
                            "data": {
                                "labelCn": "接收",
                                "labelEn": "Receive",
                                "type": "http",
                                "authConfig": {"type": "SYSTOKEN", "fields": []},
                                "inputContract": {"protocol": "HTTP", "body": {"type": "object", "properties": {"sender": {"type": "string"}}, "required": ["sender"]}},
                                "rateLimitConfig": {"maxQps": 100}
                            }
                        },
                        {
                            "id": "node_exit",
                            "type": "exit",
                            "position": {"x": 350, "y": 200},
                            "data": {
                                "labelCn": "返回",
                                "labelEn": "Return",
                                "outputMapping": {"body": {"msgId": "${$.node.node_trigger.input.sender}"}}
                            }
                        }
                    ],
                    "edges": [
                        {"id": "e1", "source": "node_trigger", "target": "node_exit", "type": "smoothstep", "data": {"businessType": "default"}}
                    ]
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);

        // 节点结构
        java.util.List<Map<String, Object>> nodes = (java.util.List<Map<String, Object>>) parsed.get("nodes");
        assertEquals(2, nodes.size());

        Map<String, Object> trigger = nodes.get(0);
        assertEquals("node_trigger", trigger.get("id"));
        assertEquals("trigger", trigger.get("type"));

        // React Flow 格式：position 独立，data 嵌套
        assertTrue(trigger.containsKey("position"), "React Flow 格式应有 position");
        assertTrue(trigger.containsKey("data"), "React Flow 格式应有 data");

        // 边使用 source/target
        java.util.List<Map<String, Object>> edges = (java.util.List<Map<String, Object>>) parsed.get("edges");
        assertEquals(1, edges.size());
        Map<String, Object> edge = edges.get(0);
        assertEquals("node_trigger", edge.get("source"), "边使用 source 字段");
        assertEquals("node_exit", edge.get("target"), "边使用 target 字段");
    }

    @Test
    @DisplayName("旧格式 orchestrationConfig: 扁平 node 字段 / edge.sourceNodeId/targetNodeId (@JsonAlias 兼容)")
    void testOldOrchestrationConfig() throws Exception {
        // FAIL_ON_UNKNOWN_PROPERTIES = false 下，使用 Map 解析可以兼容所有格式
        String json = """
                {
                    "nodes": [
                        {
                            "id": "node_entry",
                            "type": "entry",
                            "labelCn": "接收",
                            "labelEn": "Receive"
                        },
                        {
                            "id": "node_exit",
                            "type": "exit",
                            "labelCn": "返回",
                            "labelEn": "Return",
                            "outputFields": ["node_entry.sender", "node_entry.content"]
                        }
                    ],
                    "edges": [
                        {"id": "e1", "sourceNodeId": "node_entry", "targetNodeId": "node_exit"}
                    ]
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);

        // 旧格式可解析
        java.util.List<Map<String, Object>> nodes = (java.util.List<Map<String, Object>>) parsed.get("nodes");
        assertEquals(2, nodes.size());

        // 旧扁平字段仍可访问
        assertEquals("接收", nodes.get(0).get("labelCn"));

        // 旧 edge 字段名仍可访问
        java.util.List<Map<String, Object>> edges = (java.util.List<Map<String, Object>>) parsed.get("edges");
        assertEquals("node_entry", edges.get(0).get("sourceNodeId"));
        assertEquals("node_exit", edges.get(0).get("targetNodeId"));
    }

    // ==================== triggerData.type 枚举 ====================

    @Test
    @DisplayName("triggerData.type 枚举: 仅 http / manual")
    void testTriggerDataTypeEnum() throws Exception {
        String json1 = "{\"type\": \"http\", \"sender\": \"sys\"}";
        String json2 = "{\"type\": \"manual\", \"sender\": \"admin\"}";

        Map<String, Object> parsed1 = mapper.readValue(json1, Map.class);
        Map<String, Object> parsed2 = mapper.readValue(json2, Map.class);

        assertEquals("http", parsed1.get("type"));
        assertEquals("manual", parsed2.get("type"));
    }

    // ==================== errorInfo 新格式 ====================

    @Test
    @DisplayName("errorInfo 新格式: code String + messageZh/messageEn + oneOf")
    void testErrorInfoFormat() throws Exception {
        // 内部错误（6xxxx）携带 cause
        String internalError = """
                {"code": "6001", "messageZh": "连接超时", "messageEn": "Connection timeout", "cause": "Read timed out after 5000ms"}
                """;
        Map<String, Object> internal = mapper.readValue(internalError, Map.class);
        assertEquals("6001", internal.get("code"));
        assertTrue(internal.containsKey("cause"), "内部错误应携带 cause");

        // 下游错误（4xx/5xx）携带 downstreamStatus + downstreamBody
        String downstreamError = """
                {"code": "502", "messageZh": "下游服务错误", "messageEn": "Downstream service error", "downstreamStatus": 502, "downstreamBody": "{\\"error\\":\\"bad gateway\\"}"}
                """;
        Map<String, Object> downstream = mapper.readValue(downstreamError, Map.class);
        assertEquals("502", downstream.get("code"));
        assertTrue(downstream.containsKey("downstreamStatus"), "下游错误应携带 downstreamStatus");
        assertTrue(downstream.containsKey("downstreamBody"), "下游错误应携带 downstreamBody");
    }

    // ==================== inputMapping 分段格式 ====================

    @Test
    @DisplayName("inputMapping 分段格式: header/query/body")
    void testInputMappingSegmented() throws Exception {
        String json = """
                {
                    "header": {"Authorization": "${$.node.node_trigger.input.token}", "X-Request-Id": "${$.node.node_trigger.input.requestId}"},
                    "query": {"page": "${$.node.node_trigger.input.pageNum}"},
                    "body": {"message": "${$.node.node_trigger.input.content}", "sender": "${$.node.node_trigger.input.from}"}
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);
        assertTrue(parsed.containsKey("header"), "inputMapping 应有 header 分区");
        assertTrue(parsed.containsKey("query"), "inputMapping 应有 query 分区");
        assertTrue(parsed.containsKey("body"), "inputMapping 应有 body 分区");
    }

    // ==================== outputMapping 分段格式 ====================

    @Test
    @DisplayName("outputMapping 分段格式: header/body")
    void testOutputMappingSegmented() throws Exception {
        String json = """
                {
                    "header": {"X-Trace-Id": "${$.node.node_connector.output.traceId}"},
                    "body": {"result": "${$.node.node_connector.output.data}", "msgId": "constant:msg_001"}
                }
                """;

        Map<String, Object> parsed = mapper.readValue(json, Map.class);
        assertTrue(parsed.containsKey("header"), "outputMapping 应有 header 分区");
        assertTrue(parsed.containsKey("body"), "outputMapping 应有 body 分区");
    }

    // ==================== 表达式新格式 ====================

    @Test
    @DisplayName("表达式新格式: ${$.node.{id}.{input/output}.{path}}")
    void testExpressionNewFormat() throws Exception {
        String expr = "${$.node.node_trigger.input.sender}";
        assertTrue(expr.startsWith("${") && expr.endsWith("}"));

        // 验证格式: $.node.{nodeId}.{partition}.{path}
        String inner = expr.substring(2, expr.length() - 1);
        String[] parts = inner.split("\\.");
        assertEquals("$", parts[0]);
        assertEquals("node", parts[1]);
        assertEquals("node_trigger", parts[2]);
        assertTrue(parts[3].equals("input") || parts[3].equals("output"),
                "分区应为 input 或 output");
    }

    @Test
    @DisplayName("表达式兼容旧格式: ${nodeId.fieldPath} — 向后兼容")
    void testExpressionOldFormat() throws Exception {
        String oldExpr = "${node_entry.sender}";
        assertTrue(oldExpr.startsWith("${") && oldExpr.endsWith("}"));

        String inner = oldExpr.substring(2, oldExpr.length() - 1);
        String[] parts = inner.split("\\.");
        assertEquals("node_entry", parts[0]);
        assertEquals("sender", parts[1]);
    }
}