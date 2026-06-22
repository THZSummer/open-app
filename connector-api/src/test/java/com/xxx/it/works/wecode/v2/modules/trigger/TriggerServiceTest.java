package com.xxx.it.works.wecode.v2.modules.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.runtime.model.TransparentFlowResponse;
import com.xxx.it.works.wecode.v2.modules.trigger.service.OpTriggerService;
import com.xxx.it.works.wecode.v2.modules.auth.AuthValidatorRegistry;
import com.xxx.it.works.wecode.v2.modules.security.UrlWhitelistValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.xxx.it.works.wecode.v2.common.config.CacheToggle;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
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

    @Mock
    private AuthValidatorRegistry authValidatorRegistry;

    @Mock
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Mock
    private CacheToggle cacheToggle;

    @Mock
    private UrlWhitelistValidator urlWhitelistValidator;

    private ObjectMapper objectMapper;
    private OpTriggerService triggerService;

    /**
     * v5.7 有效的编排配置: 包含 trigger 节点 (type=http, authConfig, inputContract) + exit 节点
     */
    private static final String VALID_ORCHESTRATION_CONFIG =
            "{\"nodes\":["
            + "{\"id\":\"node_trigger\",\"type\":\"trigger\",\"position\":{\"x\":0,\"y\":0},"
            + "\"data\":{\"type\":\"http\",\"authConfig\":{\"type\":\"SYSTOKEN\"},\"inputContract\":{\"body\":{}}}},"
            + "{\"id\":\"n1\",\"type\":\"exit\",\"position\":{\"x\":300,\"y\":0},"
            + "\"data\":{\"labelCn\":\"结束\"}}"
            + "],\"edges\":[]}";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        triggerService = new OpTriggerService(objectMapper, authValidatorRegistry, urlWhitelistValidator,
                executor, flowVersionReadRepository, reactiveRedisTemplate, cacheToggle);
    }

    @Test
    @DisplayName("HTTP 触发成功执行 — 返回 TransparentFlowResponse")
    void testInvokeFlow_Success() {
        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setFlowId(100L);
        flowVersion.setOrchestrationConfig(VALID_ORCHESTRATION_CONFIG);

        ExecutionResult mockResult = new ExecutionResult();
        mockResult.setExecutionId("exec-001");
        mockResult.setFlowId("100");
        mockResult.setStatus("success");
        mockResult.setTotalDurationMs(150L);
        mockResult.setResultData(Map.of("body", Map.of("msgId", "msg_001"),
                "header", Map.of("X-Custom", "val1")));

        when(flowVersionReadRepository.findByFlowId(100L)).thenReturn(Mono.just(flowVersion));
        when(executor.execute(any(), anyString())).thenReturn(Mono.just(mockResult));

        Mono<TransparentFlowResponse> resultMono = triggerService.invokeFlow(
                100L, Map.of("sender", "test"), Map.of(), Map.of());

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    // 平台元数据头
                    assertEquals("100", response.getPlatformHeaders().get("X-Flow-Id"));
                    assertEquals("exec-001", response.getPlatformHeaders().get("X-Execution-Id"));
                    assertEquals("0", response.getPlatformHeaders().get("X-Status")); // success=0
                    assertEquals("150", response.getPlatformHeaders().get("X-Duration-Ms"));
                    assertEquals("0", response.getPlatformHeaders().get("X-Cache-Status"));
                    // 用户自定义头
                    assertEquals("val1", response.getUserHeaders().get("X-Custom"));
                    // Body
                    assertTrue(response.getBody() instanceof Map);
                    Map<?, ?> body = (Map<?, ?>) response.getBody();
                    assertEquals("msg_001", body.get("msgId"));
                    // HTTP Status
                    assertEquals(200, response.getHttpStatus().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("流不存在返回 404 (preExecutionError)")
    void testInvokeFlow_NotFound() {
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
        when(executor.execute(any(), anyString())).thenReturn(Mono.error(new RuntimeException("Execution error")));

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
