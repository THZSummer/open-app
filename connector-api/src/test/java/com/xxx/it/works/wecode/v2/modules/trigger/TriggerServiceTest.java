package com.xxx.it.works.wecode.v2.modules.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.trigger.service.OpTriggerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpTriggerService 测试")
class OpTriggerServiceTest {

    @Mock
    private OpFlowVersionReadRepository flowVersionReadRepository;

    @Mock
    private ReactiveSequentialExecutor executor;

    private ObjectMapper objectMapper;
    private OpTriggerService triggerService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        triggerService = new OpTriggerService(objectMapper, executor, flowVersionReadRepository);
    }

    @Test
    @DisplayName("HTTP 触发成功执行")
    void testInvokeFlow_Success() {
        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setFlowId(100L);
        flowVersion.setOrchestrationConfig("{\"nodes\":[{\"id\":\"n1\",\"type\":\"exit\",\"position\":{\"x\":0,\"y\":0},\"data\":{\"labelCn\":\"结束\"}}],\"edges\":[]}");

        ExecutionResult mockResult = new ExecutionResult();
        mockResult.setExecutionId("exec-001");
        mockResult.setFlowId("100");
        mockResult.setStatus("success");

        when(flowVersionReadRepository.findByFlowId(100L)).thenReturn(Mono.just(flowVersion));
        when(executor.execute(any(), anyString())).thenReturn(Mono.just(mockResult));

        Mono<ExecutionResult> resultMono = triggerService.invokeFlow(
                100L, Map.of("sender", "test"), Map.of(), null);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("success", result.getStatus());
                    assertEquals("100", result.getFlowId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("流不存在返回failed")
    void testInvokeFlow_NotFound() {
        when(flowVersionReadRepository.findByFlowId(999L)).thenReturn(Mono.empty());

        Mono<ExecutionResult> resultMono = triggerService.invokeFlow(
                999L, Map.of(), Map.of(), null);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("failed", result.getStatus());
                    assertNotNull(result.getErrorInfo());
                    assertNotNull(result.getErrorInfo().get("messageZh"),
                            "errorInfo should contain messageZh");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("执行异常时返回failed")
    void testInvokeFlow_ExecutionError() {
        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setFlowId(100L);
        // Use a valid orchestrationConfig so OpTriggerService reaches the executor
        flowVersion.setOrchestrationConfig("{\"nodes\":[{\"id\":\"n1\",\"type\":\"exit\",\"position\":{\"x\":0,\"y\":0},\"data\":{\"labelCn\":\"结束\"}}],\"edges\":[]}");

        when(flowVersionReadRepository.findByFlowId(100L)).thenReturn(Mono.just(flowVersion));
        when(executor.execute(any(), anyString())).thenReturn(Mono.error(new RuntimeException("Execution error")));

        Mono<ExecutionResult> resultMono = triggerService.invokeFlow(
                100L, Map.of(), Map.of(), null);

        StepVerifier.create(resultMono)
                .assertNext(result -> assertEquals("failed", result.getStatus()))
                .verifyComplete();
    }
}