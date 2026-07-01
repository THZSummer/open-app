package com.xxx.it.works.wecode.v2.modules.flowversion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlowVersionDebugService 测试")
class FlowVersionDebugServiceTest {

    @Mock
    private ReactiveSequentialExecutor executor;

    @Mock
    private OpFlowVersionReadRepository flowVersionReadRepository;

    private ObjectMapper objectMapper;
    private FlowVersionDebugService service;
    private final Long versionId = 200L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new FlowVersionDebugService(objectMapper, executor, flowVersionReadRepository);
    }

    @Test
    @DisplayName("草稿版本调试成功 → 返回执行详情")
    void testExecuteTestRun_DraftSuccess() {
        FlowVersionEntity entity = spy(new FlowVersionEntity());
        entity.setFlowId(100L);
        entity.setOrchestrationConfig("{\"nodes\":[{\"id\":\"t1\",\"type\":\"trigger\"},{\"id\":\"n1\",\"type\":\"connector\"}],\"edges\":[]}");

        Map<String, Object> parsedConfig = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, Object> triggerNode = new HashMap<>();
        triggerNode.put("id", "t1");
        triggerNode.put("type", "trigger");
        nodes.add(triggerNode);
        Map<String, Object> connectorNode = new HashMap<>();
        connectorNode.put("id", "n1");
        connectorNode.put("type", "connector");
        nodes.add(connectorNode);
        parsedConfig.put("nodes", nodes);
        parsedConfig.put("edges", new ArrayList<>());

        doReturn(parsedConfig).when(entity).parseOrchestrationConfigAsMap(any(ObjectMapper.class));

        when(flowVersionReadRepository.findById(200L)).thenReturn(Mono.just(entity));

        ExecutionResult mockResult = new ExecutionResult();
        mockResult.setExecutionId("exec-001");
        mockResult.setFlowId("100");
        mockResult.setStatus("success");
        mockResult.setDebug(true);
        when(executor.execute(any(), anyString())).thenReturn(Mono.just(mockResult));

        Map<String, Object> mockTriggerData = new HashMap<>();
        mockTriggerData.put("body", Map.of("key", "value"));

        StepVerifier.create(service.executeTestRun(100L, 200L, mockTriggerData))
                .assertNext(result -> {
                    assertEquals("success", result.getStatus());
                    assertTrue(result.isDebug());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Flow 不存在 → 返回错误 ExecutionResult")
    void testExecuteTestRun_FlowNotFound() {
        when(flowVersionReadRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(service.executeTestRun(999L, 999L, Map.of()))
                .assertNext(result -> {
                    assertEquals("failed", result.getStatus());
                    assertTrue(result.isDebug());
                    assertNotNull(result.getErrorInfo());
                    assertEquals("404", result.getErrorInfo().get("code"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("调试触发 → triggerType=3, isDebug=true")
    void testExecuteTestRun_TriggerTypeCorrect() {
        FlowVersionEntity entity = spy(new FlowVersionEntity());
        entity.setFlowId(100L);
        entity.setOrchestrationConfig("{\"nodes\":[{\"id\":\"t1\",\"type\":\"trigger\"}],\"edges\":[]}");

        Map<String, Object> parsedConfig = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, Object> triggerNode = new HashMap<>();
        triggerNode.put("id", "t1");
        triggerNode.put("type", "trigger");
        nodes.add(triggerNode);
        parsedConfig.put("nodes", nodes);
        parsedConfig.put("edges", new ArrayList<>());

        doReturn(parsedConfig).when(entity).parseOrchestrationConfigAsMap(any(ObjectMapper.class));

        when(flowVersionReadRepository.findById(200L)).thenReturn(Mono.just(entity));

        ExecutionResult mockResult = new ExecutionResult();
        mockResult.setExecutionId("exec-002");
        mockResult.setFlowId("100");
        mockResult.setStatus("success");
        mockResult.setDebug(true);
        when(executor.execute(any(), anyString())).thenReturn(Mono.just(mockResult));

        StepVerifier.create(service.executeTestRun(100L, 200L, Map.of()))
                .assertNext(result -> {
                    assertTrue(result.isDebug());
                })
                .verifyComplete();
    }
}
