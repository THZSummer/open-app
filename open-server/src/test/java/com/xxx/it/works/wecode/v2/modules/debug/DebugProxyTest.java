package com.xxx.it.works.wecode.v2.modules.debug;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpDebugProxyService 测试")
class OpDebugProxyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OpDebugProxyService debugProxyService;

    @Test
    @DisplayName("转发成功")
    void testForwardTestRun_Success() {
        Map<String, Object> mockResult = Map.of("status", "success", "executionId", "exec-001");

        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(mockResult));

        ApiResponse<Map<String, Object>> response = debugProxyService.forwardTestRun(
                100L, Map.of("sender", "test"), Map.of());

        assertEquals("200", response.getCode());
        assertNotNull(response.getData());
        assertEquals("success", response.getData().get("status"));
    }

    @Test
    @DisplayName("转发失败返回500")
    void testForwardTestRun_Failure() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        ApiResponse<Map<String, Object>> response = debugProxyService.forwardTestRun(
                100L, null, null);

        assertEquals("500", response.getCode());
    }
}

@ExtendWith(MockitoExtension.class)
@DisplayName("OpDebugProxyController 测试")
class OpDebugProxyControllerTest {

    @Mock
    private OpDebugProxyService debugProxyService;

    @InjectMocks
    private OpDebugProxyController debugProxyController;

    @Test
    @DisplayName("测试运行委托给 service")
    void testTestRun() {
        OpDebugProxyController.TestRunRequest request = new OpDebugProxyController.TestRunRequest();
        request.setMockTriggerData(Map.of("key", "val"));

        when(debugProxyService.forwardTestRun(eq(100L), any(), any()))
                .thenReturn(ApiResponse.success(Map.of("status", "success")));

        ApiResponse<Map<String, Object>> response = debugProxyController.testRun(100L, request);
        assertEquals("200", response.getCode());
        assertEquals("success", response.getData().get("status"));
    }
}