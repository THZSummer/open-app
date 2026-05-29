package com.xxx.it.works.wecode.v2.modules.trigger.controller;

import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.trigger.service.OpTriggerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * OpTriggerController 接口层测试 (WebFluxTest)
 *
 * <p>测试目标: HTTP 请求绑定 + 响应序列化 + 认证校验
 * 接口: #18 POST /api/v1/trigger/{flowId}/invoke
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

    @BeforeEach
    void setUp() {
        // Prevent OpRateLimitFilter from failing when it calls findByFlowId
        when(flowVersionReadRepository.findByFlowId(anyLong())).thenReturn(Mono.empty());
    }

    @Nested
    @DisplayName("#18 POST /api/v1/trigger/{flowId}/invoke")
    class InvokeFlow {

        @Test
        @DisplayName("✅ TC-072: 正常触发（running + 合法凭证）")
        void testInvokeSuccess() throws Exception {
            ExecutionResult result = new ExecutionResult();
            result.setExecutionId("1122334455667788990");
            result.setFlowId("2000000000000000001");
            result.setStatus("success");
            result.setTotalDurationMs(2250);
            result.setResultData(Map.of("msgId", "msg_xxxx"));

            when(triggerService.invokeFlow(eq(2000000000000000001L), any(), any(), any()))
                    .thenReturn(Mono.just(result));

            webTestClient.post()
                    .uri("/api/v1/trigger/{flowId}/invoke", "2000000000000000001")
                    .header("X-Sys-Token", "valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"sender\":\"external_system\",\"content\":\"test\"}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.executionId").isEqualTo("1122334455667788990")
                    .jsonPath("$.status").isEqualTo("success")
                    .jsonPath("$.totalDurationMs").isNumber()
                    .jsonPath("$.resultData.msgId").isEqualTo("msg_xxxx");
        }

        @Test
        @DisplayName("❌ TC-073: 缺少 X-Sys-Token（返回 failed 非 401）")
        void testInvokeNoToken() throws Exception {
            // Mock the service to return an auth error (no X-Sys-Token in headers)
            ExecutionResult authError = new ExecutionResult();
            authError.setExecutionId("N/A");
            authError.setFlowId("2000000000000000001");
            authError.setStatus("failed");
            java.util.Map<String, Object> errInfo = new java.util.HashMap<>();
            errInfo.put("code", "6001");
            errInfo.put("messageZh", "缺少认证令牌");
            errInfo.put("messageEn", "Missing authentication token");
            authError.setErrorInfo(errInfo);
            when(triggerService.invokeFlow(eq(2000000000000000001L), any(), any(), any()))
                    .thenReturn(Mono.just(authError));

            webTestClient.post()
                    .uri("/api/v1/trigger/{flowId}/invoke", "2000000000000000001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"sender\":\"test\"}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("failed")
                    .jsonPath("$.errorInfo.messageZh").value(msg -> 
                        ((String)msg).contains("缺少认证令牌"));
        }

        @Test
        @DisplayName("❌ TC-074: flowId 不存在")
        void testInvokeFlowNotFound() throws Exception {
            when(triggerService.invokeFlow(eq(999L), any(), any(), any()))
                    .thenReturn(Mono.error(new RuntimeException("Flow not found: 999")));

            webTestClient.post()
                    .uri("/api/v1/trigger/{flowId}/invoke", "999")
                    .header("X-Sys-Token", "valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"sender\":\"test\"}")
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("✅ TC-075: 同步返回完整执行结果")
        void testInvokeWithSteps() throws Exception {
            ExecutionResult result = new ExecutionResult();
            result.setExecutionId("exec-001");
            result.setFlowId("200");
            result.setStatus("success");
            result.setTotalDurationMs(1270);

            ExecutionResult.StepDetail step = new ExecutionResult.StepDetail();
            step.setNodeId("node_trigger");
            step.setNodeType("trigger");
            step.setStatus("success");
            step.setDurationMs(10);
            result.addStep(step);

            when(triggerService.invokeFlow(eq(200L), any(), any(), any()))
                    .thenReturn(Mono.just(result));

            webTestClient.post()
                    .uri("/api/v1/trigger/{flowId}/invoke", "200")
                    .header("X-Sys-Token", "valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.executionId").isEqualTo("exec-001")
                    .jsonPath("$.status").isEqualTo("success")
                    .jsonPath("$.steps.length()").isEqualTo(1)
                    .jsonPath("$.steps[0].nodeId").isEqualTo("node_trigger");
        }

        @Test
        @DisplayName("✅ TC-076: X-Sys-Token 在 Header 中传递")
        void testInvokeTokenInHeader() throws Exception {
            ExecutionResult result = new ExecutionResult();
            result.setExecutionId("exec-002");
            result.setStatus("success");
            result.setTotalDurationMs(100);

            when(triggerService.invokeFlow(eq(100L), any(), any(), any()))
                    .thenReturn(Mono.just(result));

            webTestClient.post()
                    .uri("/api/v1/trigger/{flowId}/invoke", "100")
                    .header("X-Sys-Token", "my-sys-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.executionId").isEqualTo("exec-002");
        }
    }
}
