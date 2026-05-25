package com.xxx.it.works.wecode.v2.modules.debug;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DebugProxyController 接口层测试 (WebMvcTest)
 *
 * <p>测试目标: HTTP 请求绑定 + 响应序列化 + 代理转发
 * 接口: #17 POST /api/v1/flows/{flowId}/test-run
 * 测试层次: L2 接口层
 * </p>
 */
@WebMvcTest(DebugProxyController.class)
@DisplayName("DebugProxyController WebMvcTest")
class DebugProxyControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DebugProxyService debugProxyService;

    @Nested
    @DisplayName("#17 POST /api/v1/flows/{flowId}/test-run")
    class TestRun {

        @Test
        @DisplayName("✅ TC-065: 正常测试运行")
        void testRunSuccess() throws Exception {
            Map<String, Object> resultData = Map.of(
                    "executionId", "1122334455667788990",
                    "status", "success",
                    "totalDurationMs", 1270
            );
            when(debugProxyService.forwardTestRun(eq(100L), any(), any()))
                    .thenReturn(ApiResponse.success(resultData));

            mockMvc.perform(post("/api/v1/flows/{flowId}/test-run", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "mockTriggerData": {
                                            "sender": "test_user",
                                            "content": "测试消息"
                                        },
                                        "credentials": {
                                            "9876543210": {
                                                "accessKey": "test_ak"
                                            }
                                        }
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"))
                    .andExpect(jsonPath("$.data.executionId").isString())
                    .andExpect(jsonPath("$.data.status").value("success"));
        }

        @Test
        @DisplayName("✅ TC-066: 无 mockTriggerData")
        void testRunNoTriggerData() throws Exception {
            when(debugProxyService.forwardTestRun(eq(100L), any(), any()))
                    .thenReturn(ApiResponse.success(Map.of("status", "success")));

            mockMvc.perform(post("/api/v1/flows/{flowId}/test-run", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"));
        }

        @Test
        @DisplayName("❌ TC-067: flowId 不存在")
        void testRunFlowNotFound() throws Exception {
            when(debugProxyService.forwardTestRun(eq(999L), any(), any()))
                    .thenReturn(ApiResponse.error("404", "连接流不存在", "Flow not found"));

            mockMvc.perform(post("/api/v1/flows/{flowId}/test-run", "999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"mockTriggerData": {"sender": "test"}}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("404"));
        }

        @Test
        @DisplayName("❌ TC-068: 转发失败")
        void testRunForwardFailed() throws Exception {
            when(debugProxyService.forwardTestRun(eq(100L), any(), any()))
                    .thenReturn(ApiResponse.error("500", "测试运行转发失败: Connection refused",
                            "Test run forwarding failed: Connection refused"));

            mockMvc.perform(post("/api/v1/flows/{flowId}/test-run", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"mockTriggerData": {"sender": "test"}}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("500"));
        }
    }
}
