package com.xxx.it.works.wecode.v2.modules.trigger.controller;

import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.model.TransparentFlowResponse;
import com.xxx.it.works.wecode.v2.modules.trigger.service.OpTriggerService;
import com.xxx.it.works.wecode.v2.modules.ratelimit.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import com.xxx.it.works.wecode.v2.modules.ratelimit.RateLimitConfigReader;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OpTriggerController 接口层测试 (WebFluxTest) (v5.8)
 *
 * <p>测试目标: HTTP 请求绑定 + 透明穿透响应
 * 接口: #54 POST /api/v1/flows/{flowId}/invoke
 * 测试层次: L2 接口层
 * </p>
 */
@WebFluxTest(OpTriggerController.class)
@DisplayName("OpTriggerController WebFluxTest")
class OpTriggerControllerWebFluxTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OpTriggerService triggerService;

    @MockitoBean
    private OpFlowVersionReadRepository flowVersionReadRepository;

    @MockitoBean
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private RateLimitConfigReader rateLimitConfigReader;

    @BeforeEach
    void setUp() {
        // Prevent rate limit filter dependency injection failure in test context
        when(flowVersionReadRepository.findByFlowId(anyLong())).thenReturn(Mono.empty());

        // Stub rate limiter to allow all requests
        when(rateLimitConfigReader.readFlowRateLimit(anyLong()))
                .thenReturn(Mono.just(new RateLimitConfig("qps", Integer.MAX_VALUE, Integer.MAX_VALUE)));

        // Stub redis to allow rate limit checks
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> mockOps =
                mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));
    }

    @Nested
    @DisplayName("#54 POST /api/v1/flows/{flowId}/invoke (v5.8 透明穿透)")
    class InvokeFlow {

        @Test
        @DisplayName("✅ TC-072: 正常触发 — 裸Body + X-平台头 + 用户自定义头")
        void testInvokeSuccess() {
            TransparentFlowResponse response = TransparentFlowResponse.success(
                    "2000000000000000001", "1122334455667788990", 0, 2250,
                    Map.of("X-Custom", "val1"),
                    Map.of("msgId", "msg_xxxx"));

            when(triggerService.invokeFlow(eq(2000000000000000001L), any(), any(), any()))
                    .thenReturn(Mono.just(response));

            webTestClient.post()
                    .uri("/api/v1/flows/{flowId}/invoke", "2000000000000000001")
                    .header("X-Sys-Token", "valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"sender\":\"external_system\",\"content\":\"test\"}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("X-Flow-Id", "2000000000000000001")
                    .expectHeader().valueEquals("X-Execution-Id", "1122334455667788990")
                    .expectHeader().valueEquals("X-Status", "0")
                    .expectHeader().valueEquals("X-Duration-Ms", "2250")
                    .expectHeader().valueEquals("X-Cache-Status", "0")
                    .expectHeader().valueEquals("X-Custom", "val1")
                    .expectBody()
                    .jsonPath("$.msgId").isEqualTo("msg_xxxx");
        }

        @Test
        @DisplayName("❌ TC-073: 缺少 X-Sys-Token — 返回 401 + X-Code 头 + 空Body")
        void testInvokeNoToken() {
            TransparentFlowResponse authError = TransparentFlowResponse.preExecutionError(
                    "2000000000000000001", HttpStatus.UNAUTHORIZED, "401",
                    "认证失败: 缺少认证令牌", "Authentication failed: Missing token");

            when(triggerService.invokeFlow(eq(2000000000000000001L), any(), any(), any()))
                    .thenReturn(Mono.just(authError));

            webTestClient.post()
                    .uri("/api/v1/flows/{flowId}/invoke", "2000000000000000001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"sender\":\"test\"}")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectHeader().valueEquals("X-Code", "401")
                    .expectHeader().valueEquals("X-Flow-Id", "2000000000000000001")
                    .expectBody().isEmpty();
        }

        @Test
        @DisplayName("❌ TC-074: flowId 不存在 — 返回 404 + X-Code 头 + 空Body")
        void testInvokeFlowNotFound() {
            TransparentFlowResponse notFound = TransparentFlowResponse.preExecutionError(
                    "999", HttpStatus.NOT_FOUND, "404",
                    "流不存在: Flow not found: 999", "Flow not found: 999");

            when(triggerService.invokeFlow(eq(999L), any(), any(), any()))
                    .thenReturn(Mono.just(notFound));

            webTestClient.post()
                    .uri("/api/v1/flows/{flowId}/invoke", "999")
                    .header("X-Sys-Token", "valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"sender\":\"test\"}")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectHeader().valueEquals("X-Code", "404")
                    .expectHeader().valueEquals("X-Flow-Id", "999")
                    .expectBody().isEmpty();
        }

        @Test
        @DisplayName("✅ TC-075: 正常触发 — 验证 Body 为裸数据 (非 envelope)")
        void testInvokeBodyIsBareData() {
            TransparentFlowResponse response = TransparentFlowResponse.success(
                    "200", "exec-001", 0, 1270,
                    Map.of(), // no user headers
                    Map.of("code", 0, "message", "success", "data", Map.of("count", 5)));

            when(triggerService.invokeFlow(eq(200L), any(), any(), any()))
                    .thenReturn(Mono.just(response));

            webTestClient.post()
                    .uri("/api/v1/flows/{flowId}/invoke", "200")
                    .header("X-Sys-Token", "valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("X-Flow-Id", "200")
                    .expectHeader().valueEquals("X-Execution-Id", "exec-001")
                    .expectHeader().valueEquals("X-Status", "0")
                    .expectBody()
                    // 验证: Body 直接是出口数据, 没有 executionId/status/resultData 信封
                    .jsonPath("$.executionId").doesNotExist()
                    .jsonPath("$.status").doesNotExist()
                    .jsonPath("$.resultData").doesNotExist()
                    .jsonPath("$.code").isEqualTo(0)
                    .jsonPath("$.message").isEqualTo("success")
                    .jsonPath("$.data.count").isEqualTo(5);
        }

        @Test
        @DisplayName("✅ TC-076: X-Sys-Token 在 Header 中传递 — 验证平台头完整")
        void testInvokeTokenInHeader() {
            TransparentFlowResponse response = TransparentFlowResponse.success(
                    "100", "exec-002", 0, 100, Map.of(), Map.of("echo", "ok"));

            when(triggerService.invokeFlow(eq(100L), any(), any(), any()))
                    .thenReturn(Mono.just(response));

            webTestClient.post()
                    .uri("/api/v1/flows/{flowId}/invoke", "100")
                    .header("X-Sys-Token", "my-sys-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("X-Flow-Id", "100")
                    .expectHeader().valueEquals("X-Execution-Id", "exec-002")
                    .expectHeader().valueEquals("X-Status", "0")
                    .expectHeader().valueEquals("X-Duration-Ms", "100")
                    .expectBody()
                    .jsonPath("$.echo").isEqualTo("ok");
        }

        @Test
        @DisplayName("❌ 前置校验失败返回 500")
        void testInvokeServerError() {
            TransparentFlowResponse error = TransparentFlowResponse.preExecutionError(
                    "50", HttpStatus.INTERNAL_SERVER_ERROR, "500",
                    "触发执行失败: something broke", "Trigger execution failed: something broke");

            when(triggerService.invokeFlow(eq(50L), any(), any(), any()))
                    .thenReturn(Mono.just(error));

            webTestClient.post()
                    .uri("/api/v1/flows/{flowId}/invoke", "50")
                    .header("X-Sys-Token", "valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().is5xxServerError()
                    .expectHeader().valueEquals("X-Code", "500")
                    .expectHeader().valueEquals("X-Flow-Id", "50")
                    .expectBody().isEmpty();
        }
    }
}
