package com.xxx.it.works.wecode.v2.modules.runtime.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.runtime.node.ConnectorNodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.DataProcessorExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.EntryNodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.ExitNodeExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReactiveSequentialExecutor 测试")
class ReactiveSequentialExecutorTest {

    private ReactiveSequentialExecutor executor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        WebClient webClient = WebClient.builder().build();

        EntryNodeExecutor entryExecutor = new EntryNodeExecutor(objectMapper);
        ConnectorNodeExecutor connectorExecutor = new ConnectorNodeExecutor(objectMapper, webClient);
        DataProcessorExecutor dataProcessor = new DataProcessorExecutor(objectMapper);
        ExitNodeExecutor exitExecutor = new ExitNodeExecutor(objectMapper);

        executor = new ReactiveSequentialExecutor(
                objectMapper, entryExecutor, connectorExecutor, dataProcessor, exitExecutor);
    }

    @Test
    @DisplayName("简单线性执行: entry → exit")
    void testSimpleLinearFlow_EntryToExit() {
        ExecutionContext context = new ExecutionContext("test-exec-1", "test-flow");
        context.setTriggerData(Map.of("sender", "test_user", "content", "hello"));

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("nodes", List.of(
                Map.of("id", "node_entry", "type", "entry",
                        "position", Map.of("x", 0, "y", 0),
                        "data", Map.of("labelCn", "接收")),
                Map.of("id", "node_exit", "type", "exit",
                        "position", Map.of("x", 300, "y", 0),
                        "data", Map.of("labelCn", "返回",
                                "outputMapping", Map.of("header", Map.of(), "body", Map.of("sender", "${$.node.node_entry.output.sender}"))))
        ));
        config.put("edges", List.of(
                Map.of("id", "e1", "source", "node_entry", "target", "node_exit")
        ));

        String configJson = toJson(config);

        Mono<ExecutionResult> resultMono = executor.execute(context, configJson);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("success", result.getStatus());
                    assertNotNull(result.getResultData());
                    assertTrue(result.getTotalDurationMs() >= 0);
                    assertEquals(2, result.getSteps().size());
                    assertEquals("node_entry", result.getSteps().get(0).getNodeId());
                    assertEquals("node_exit", result.getSteps().get(1).getNodeId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("含数据处理节点: entry → processor → exit")
    void testFlowWithDataProcessor() {
        ExecutionContext context = new ExecutionContext("test-exec-2", "test-flow");
        context.setTriggerData(Map.of("name", "Alice"));

        List<Map<String, Object>> fieldMappings = List.of(
                Map.of("targetField", "greeting", "sourceValue", "${$.node.node_entry.output.name}", "sourceType", "reference")
        );

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("nodes", List.of(
                Map.of("id", "node_entry", "type", "entry",
                        "position", Map.of("x", 0, "y", 0),
                        "data", Map.of("labelCn", "入口")),
                Map.of("id", "node_proc", "type", "data_processor",
                        "position", Map.of("x", 300, "y", 0),
                        "data", Map.of("labelCn", "处理",
                                "fieldMappings", fieldMappings)),
                Map.of("id", "node_exit", "type", "exit",
                        "position", Map.of("x", 600, "y", 0),
                        "data", Map.of("labelCn", "出口",
                                "outputMapping", Map.of("header", Map.of(), "body", Map.of("greeting", "${$.node.node_proc.output.greeting}"))))
        ));
        config.put("edges", List.of(
                Map.of("id", "e1", "source", "node_entry", "target", "node_proc"),
                Map.of("id", "e2", "source", "node_proc", "target", "node_exit")
        ));

        Mono<ExecutionResult> resultMono = executor.execute(context, toJson(config));

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("success", result.getStatus());
                    assertEquals(3, result.getSteps().size());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("空编排 - 无节点返回failed")
    void testEmptyOrchestration() {
        ExecutionContext context = new ExecutionContext("test-exec-3", "test-flow");

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("nodes", List.of());
        config.put("edges", List.of());

        Mono<ExecutionResult> resultMono = executor.execute(context, toJson(config));

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("failed", result.getStatus());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("未知节点类型 - 跳过不报错")
    void testUnknownNodeType_Skipped() {
        ExecutionContext context = new ExecutionContext("test-exec-4", "test-flow");
        context.setTriggerData(Map.of("data", "test"));

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("nodes", List.of(
                Map.of("id", "node_entry", "type", "entry",
                        "position", Map.of("x", 0, "y", 0),
                        "data", Map.of()),
                Map.of("id", "node_unknown", "type", "unknown_type",
                        "position", Map.of("x", 300, "y", 0),
                        "data", Map.of("labelCn", "未知")),
                Map.of("id", "node_exit", "type", "exit",
                        "position", Map.of("x", 600, "y", 0),
                        "data", Map.of("outputMapping", Map.of("header", Map.of(), "body", Map.of("data", "${$.node.node_entry.output.data}"))))
        ));
        config.put("edges", List.of(
                Map.of("id", "e1", "source", "node_entry", "target", "node_unknown"),
                Map.of("id", "e2", "source", "node_unknown", "target", "node_exit")
        ));

        Mono<ExecutionResult> resultMono = executor.execute(context, toJson(config));

        StepVerifier.create(resultMono)
                .assertNext(result -> assertEquals("success", result.getStatus()))
                .verifyComplete();
    }

    @Test
    @DisplayName("错误处理 - 非法的JSON格式")
    void testInvalidJson() {
        ExecutionContext context = new ExecutionContext("test-exec-5", "test-flow");

        Mono<ExecutionResult> resultMono = executor.execute(context, "not valid json");

        StepVerifier.create(resultMono)
                .assertNext(result -> assertEquals("failed", result.getStatus()))
                .verifyComplete();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}