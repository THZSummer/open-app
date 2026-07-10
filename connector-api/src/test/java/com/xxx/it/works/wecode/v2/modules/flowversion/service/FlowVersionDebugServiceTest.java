package com.xxx.it.works.wecode.v2.modules.flowversion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.error.ErrorCode;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.DagScheduler;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
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
    private DagScheduler dagScheduler;

    @Mock
    private OpFlowVersionReadRepository flowVersionReadRepository;

    private ObjectMapper objectMapper;
    private FlowVersionDebugService service;
    private final Long versionId = 200L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new FlowVersionDebugService(objectMapper, dagScheduler, flowVersionReadRepository);
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

        ExecutionContext mockCtx = new ExecutionContext("exec-001", "100");
        mockCtx.setDebug(true);
        NodeContext exitCtx = new NodeContext();
        exitCtx.setNodeId("n1");
        exitCtx.setNodeType("connector");
        exitCtx.setInput(new HashMap<>());
        Map<String, Object> exitOutput = new HashMap<>();
        exitOutput.put("result", "ok");
        exitOutput.put("__status", "success");
        exitCtx.setOutput(exitOutput);
        exitCtx.setStatus("success");
        exitCtx.setDurationMs(100);
        mockCtx.setNodeContext(exitCtx);

        when(dagScheduler.schedule(anyString(), any(ExecutionContext.class))).thenReturn(Mono.just(mockCtx));

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
                    assertEquals(ErrorCode.PRECHECK_VERSION_NOT_FOUND, result.getErrorInfo().get("code"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("调试触发 → isDebug=true")
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

        ExecutionContext mockCtx = new ExecutionContext("exec-002", "100");
        mockCtx.setDebug(true);
        NodeContext exitCtx = new NodeContext();
        exitCtx.setNodeId("t1");
        exitCtx.setNodeType("trigger");
        exitCtx.setInput(new HashMap<>());
        exitCtx.setOutput(new HashMap<>());
        exitCtx.setStatus("success");
        exitCtx.setDurationMs(0);
        mockCtx.setNodeContext(exitCtx);

        when(dagScheduler.schedule(anyString(), any(ExecutionContext.class))).thenReturn(Mono.just(mockCtx));

        StepVerifier.create(service.executeTestRun(100L, 200L, Map.of()))
                .assertNext(result -> {
                    assertTrue(result.isDebug());
                })
                .verifyComplete();
    }
}
