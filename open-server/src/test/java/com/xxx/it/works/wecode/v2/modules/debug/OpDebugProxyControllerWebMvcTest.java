package com.xxx.it.works.wecode.v2.modules.debug;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

/**
 * OpDebugProxyController 接口层测试
 *
 * <p>测试目标: 请求参数绑定 + 服务调用 + 响应序列化
 * 接口: #17 POST /service/open/v2/flows/{flowId}/test-run
 * 测试层次: L2 接口层
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OpDebugProxyController 测试")
class OpDebugProxyControllerWebMvcTest {

    @Mock
    private OpDebugProxyService debugProxyService;

    @InjectMocks
    private OpDebugProxyController controller;

    @Nested
    @DisplayName("#17 POST /service/open/v2/flows/{flowId}/test-run")
    class TestRun {

        @Test
        @DisplayName("TC-065: 正常测试运行")
        void testRunSuccess() {
            Map<String, Object> resultData = Map.of(
                    "executionId", "1122334455667788990",
                    "status", "success",
                    "totalDurationMs", 1270
            );
            when(debugProxyService.forwardTestRun(eq(100L), eq(200L), any(), any()))
                    .thenReturn(ApiResponse.success(resultData));

            OpDebugProxyController.TestRunRequest request = new OpDebugProxyController.TestRunRequest();
            request.setTriggerData(Map.of("sender", "test_user", "content", "测试消息"));
            request.setCredentials(Map.of("9876543210", Map.of("accessKey", "test_ak")));

            ApiResponse<Map<String, Object>> response = controller.testRun(100L, 200L, request);

            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("1122334455667788990", response.getData().get("executionId"));
            assertEquals("success", response.getData().get("status"));
        }

        @Test
        @DisplayName("TC-066: 无 mockTriggerData")
        void testRunNoTriggerData() {
            when(debugProxyService.forwardTestRun(eq(100L), eq(200L), any(), any()))
                    .thenReturn(ApiResponse.success(Map.of("status", "success")));

            ApiResponse<Map<String, Object>> response = controller.testRun(100L, 200L, null);

            assertEquals("200", response.getCode());
            assertEquals("success", response.getData().get("status"));
        }

        @Test
        @DisplayName("TC-067: flowId 不存在")
        void testRunFlowNotFound() {
            when(debugProxyService.forwardTestRun(eq(999L), eq(200L), any(), any()))
                    .thenReturn(ApiResponse.error("404", "连接流不存在", "Flow not found"));

            OpDebugProxyController.TestRunRequest request = new OpDebugProxyController.TestRunRequest();
            request.setTriggerData(Map.of("sender", "test"));

            ApiResponse<Map<String, Object>> response = controller.testRun(999L, 200L, request);

            assertEquals("404", response.getCode());
            assertEquals("连接流不存在", response.getMessageZh());
        }

        @Test
        @DisplayName("TC-068: 转发失败")
        void testRunForwardFailed() {
            when(debugProxyService.forwardTestRun(eq(100L), eq(200L), any(), any()))
                    .thenReturn(ApiResponse.error("500", "测试运行转发失败: Connection refused",
                            "Test run forwarding failed: Connection refused"));

            OpDebugProxyController.TestRunRequest request = new OpDebugProxyController.TestRunRequest();
            request.setTriggerData(Map.of("sender", "test"));

            ApiResponse<Map<String, Object>> response = controller.testRun(100L, 200L, request);

            assertEquals("500", response.getCode());
            assertEquals("测试运行转发失败: Connection refused", response.getMessageZh());
        }
    }
}
