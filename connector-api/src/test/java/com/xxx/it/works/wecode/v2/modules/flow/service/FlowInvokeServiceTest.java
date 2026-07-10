package com.xxx.it.works.wecode.v2.modules.flow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;

import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.DagScheduler;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.runtime.model.TransparentFlowResponse;
import com.xxx.it.works.wecode.v2.common.IdGenerator;
import com.xxx.it.works.wecode.v2.modules.auth.SysTokenResolver;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionRecordService;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionStepService;
import com.xxx.it.works.wecode.v2.modules.flow.service.FlowInvokeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import com.xxx.it.works.wecode.v2.modules.cache.FlowCacheManager;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlowInvokeService 测试")
class FlowInvokeServiceTest {

    @Mock
    private OpFlowVersionReadRepository flowVersionReadRepository;

    @Mock
    private DagScheduler dagScheduler;

    @Mock
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private FlowCacheManager cacheManager;

    @Mock
    private EntityCacheManager entityCacheManager;

    @Mock
    private ExecutionRecordService executionRecordService;

    @Mock
    private ExecutionStepService executionStepService;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private ConnectorApiPropertyService propertyService;

    private ObjectMapper objectMapper;
    private FlowInvokeService triggerService;

    /**
     * v5.7 有效的编排配置: 包含 trigger 节点 (triggerType=http, authConfigs, input) + exit 节点
     */
    private static final String VALID_ORCHESTRATION_CONFIG =
            "{\"nodes\":["
            + "{\"id\":\"node_trigger\",\"type\":\"trigger\",\"position\":{\"x\":0,\"y\":0},"
            + "\"data\":{\"triggerType\":\"http\",\"authConfigs\":[{\"type\":\"SYSTOKEN\"}],\"input\":{\"body\":{}}}},"
            + "{\"id\":\"n1\",\"type\":\"exit\",\"position\":{\"x\":300,\"y\":0},"
            + "\"data\":{\"labelCn\":\"结束\"}}"
            + "],\"edges\":[]}";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());
        lenient().when(valueOperations.set(anyString(), anyString(), any())).thenReturn(Mono.just(true));
        when(idGenerator.nextId()).thenReturn(1L);
        lenient().when(propertyService.isLogCollectionEnabled()).thenReturn(Mono.just(true));
        lenient().when(propertyService.loadPlatformDefaults()).thenReturn(Mono.just(java.util.Map.of()));
        triggerService = new FlowInvokeService(objectMapper,
                dagScheduler, flowVersionReadRepository,
                reactiveRedisTemplate, cacheManager, entityCacheManager,
                executionRecordService, executionStepService, idGenerator,
                propertyService, new SysTokenResolver());
    }

    @Test
    @DisplayName("HTTP 触发成功执行 — 返回 TransparentFlowResponse")
    void testInvokeFlow_Success() {
        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setFlowId(100L);
        flowVersion.setOrchestrationConfig(VALID_ORCHESTRATION_CONFIG);

        ExecutionContext mockCtx = new ExecutionContext("exec-001", "100");
        NodeContext exitNodeCtx = new NodeContext();
        exitNodeCtx.setNodeId("n1");
        exitNodeCtx.setNodeType("exit");
        exitNodeCtx.setStatus("success");
        exitNodeCtx.setDurationMs(150L);
        exitNodeCtx.setOutput(Map.of("body", Map.of("msgId", "msg_001"),
                "header", Map.of("X-Custom", "val1")));
        mockCtx.setNodeContext(exitNodeCtx);

        when(flowVersionReadRepository.findByFlowId(100L)).thenReturn(Mono.just(flowVersion));
        when(dagScheduler.schedule(anyString(), any())).thenReturn(Mono.just(mockCtx));
        when(entityCacheManager.getFlow(100L)).thenReturn(Mono.empty());

        Mono<TransparentFlowResponse> resultMono = triggerService.invokeFlow(
                100L, Map.of("sender", "test"), Map.of(), Map.of());

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertEquals("100", response.getPlatformHeaders().get("X-Flow-Id"));
                    assertNotNull(response.getPlatformHeaders().get("X-Execution-Id"), "Execution ID should not be null");
                    assertTrue(response.getPlatformHeaders().get("X-Execution-Id").matches("[a-f0-9]{32}"), "Should be a 32-char UUID hex");
                    assertEquals("0", response.getPlatformHeaders().get("X-Status"));
                    assertEquals("150", response.getPlatformHeaders().get("X-Duration-Ms"));
                    assertEquals("0", response.getPlatformHeaders().get("X-Cache-Status"));
                    assertEquals("val1", response.getUserHeaders().get("X-Custom"));
                    assertTrue(response.getBody() instanceof Map);
                    Map<?, ?> body = (Map<?, ?>) response.getBody();
                    assertEquals("msg_001", body.get("msgId"));
                    assertEquals(200, response.getHttpStatus().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("已停止的连接流调用返回 409")
    void testInvokeFlow_Stopped() {
        FlowEntity stoppedFlow = mock(FlowEntity.class);
        when(stoppedFlow.getLifecycleStatus()).thenReturn(1); // STOPPED

        when(entityCacheManager.getFlow(100L)).thenReturn(Mono.just(stoppedFlow));
        // switchIfEmpty fallback 兜底 mock (虽然不应执行到, 但 NPE 会掩盖真实错误)
        when(flowVersionReadRepository.findByFlowId(100L)).thenReturn(Mono.empty());

        Mono<TransparentFlowResponse> resultMono = triggerService.invokeFlow(
                100L, Map.of(), Map.of(), Map.of());

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertEquals(409, response.getHttpStatus().value());
                    assertNull(response.getBody(), "错误响应应为空Body");
                    assertEquals("409", response.getPlatformHeaders().get("X-Code"));
                    assertNotNull(response.getPlatformHeaders().get("X-Message-Zh"),
                            "should have X-Message-Zh");
                    assertTrue(
                            response.getPlatformHeaders().get("X-Message-Zh").contains("未启动"),
                            "should mention 未启动");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("已失效的连接流调用返回 409")
    void testInvokeFlow_Invalidated() {
        FlowEntity invalidatedFlow = mock(FlowEntity.class);
        when(invalidatedFlow.getLifecycleStatus()).thenReturn(3); // INVALIDATED

        when(entityCacheManager.getFlow(100L)).thenReturn(Mono.just(invalidatedFlow));
        when(flowVersionReadRepository.findByFlowId(100L)).thenReturn(Mono.empty());

        Mono<TransparentFlowResponse> resultMono = triggerService.invokeFlow(
                100L, Map.of(), Map.of(), Map.of());

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertEquals(409, response.getHttpStatus().value());
                    assertNull(response.getBody());
                    assertEquals("409", response.getPlatformHeaders().get("X-Code"));
                    assertNotNull(response.getPlatformHeaders().get("X-Message-Zh"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("流不存在返回 404 (preExecutionError)")
    void testInvokeFlow_NotFound() {
        when(entityCacheManager.getFlow(999L)).thenReturn(Mono.empty());
        when(flowVersionReadRepository.findByFlowId(999L)).thenReturn(Mono.empty());

        Mono<TransparentFlowResponse> resultMono = triggerService.invokeFlow(
                999L, Map.of(), Map.of(), Map.of());

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertEquals(404, response.getHttpStatus().value());
                    assertNull(response.getBody(), "错误响应应为空Body");
                    assertEquals("404", response.getPlatformHeaders().get("X-Code"));
                    assertNotNull(response.getPlatformHeaders().get("X-Message-Zh"),
                            "should have X-Message-Zh");
                    assertNotNull(response.getPlatformHeaders().get("X-Message-En"),
                            "should have X-Message-En");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("执行异常时返回 500 (preExecutionError)")
    void testInvokeFlow_ExecutionError() {
        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setFlowId(100L);
        flowVersion.setOrchestrationConfig(VALID_ORCHESTRATION_CONFIG);

        when(flowVersionReadRepository.findByFlowId(100L)).thenReturn(Mono.just(flowVersion));
        when(dagScheduler.schedule(anyString(), any())).thenReturn(Mono.error(new RuntimeException("Execution error")));
        when(entityCacheManager.getFlow(100L)).thenReturn(Mono.empty());

        Mono<TransparentFlowResponse> resultMono = triggerService.invokeFlow(
                100L, Map.of(), Map.of(), Map.of());

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertEquals(500, response.getHttpStatus().value());
                    assertNull(response.getBody(), "错误响应应为空Body");
                    assertEquals("500", response.getPlatformHeaders().get("X-Code"));
                    assertNotNull(response.getPlatformHeaders().get("X-Message-Zh"));
                })
                .verifyComplete();
    }
}
