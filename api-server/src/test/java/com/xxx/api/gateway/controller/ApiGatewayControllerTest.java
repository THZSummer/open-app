package com.xxx.api.gateway.controller;

import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.gateway.dto.CallbackConfigRequest;
import com.xxx.api.gateway.dto.CallbackConfigResponse;
import com.xxx.api.gateway.dto.PermissionCheckResponse;
import com.xxx.api.gateway.service.ApiGatewayService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("API 网关控制器测试")
class ApiGatewayControllerTest {

    @Mock
    private ApiGatewayService apiGatewayService;

    @InjectMocks
    private ApiGatewayController apiGatewayController;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("proxyApiRequest 测试")
    class ProxyApiRequestTests {

        @Test
        @DisplayName("代理成功 - 验证通过、权限校验通过")
        void testProxyApiRequest_Success() {
            String appId = "app001";
            Integer authType = 1;
            String authCredential = "valid-credential";
            String body = "{\"data\":\"test\"}";

            when(apiGatewayService.verifyApplication(appId, authType, authCredential)).thenReturn(true);
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/gateway/api/v1/messages");
            when(apiGatewayService.findScopeByPathAndMethod(anyString(), anyString())).thenReturn("api:v1:messages:get");
            when(apiGatewayService.checkPermission(eq(appId), anyString()))
                    .thenReturn(PermissionCheckResponse.builder()
                            .authorized(true)
                            .subscriptionId("sub001")
                            .subscriptionStatus(1)
                            .build());

            ResponseEntity<String> response = apiGatewayController.proxyApiRequest(appId, authType, authCredential, body, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().contains("\"code\":\"200\""));
            verify(apiGatewayService).verifyApplication(appId, authType, authCredential);
            verify(apiGatewayService).checkPermission(eq(appId), anyString());
        }

        @Test
        @DisplayName("身份验证失败 - verifyApplication 返回 false，返回 401")
        void testProxyApiRequest_AuthenticationFailed() {
            String appId = "app001";
            Integer authType = 1;
            String authCredential = "invalid-credential";
            String body = "{\"data\":\"test\"}";

            when(apiGatewayService.verifyApplication(appId, authType, authCredential)).thenReturn(false);

            ResponseEntity<String> response = apiGatewayController.proxyApiRequest(appId, authType, authCredential, body, request);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().contains("\"code\":\"401\""));
            assertTrue(response.getBody().contains("应用身份验证失败"));
            verify(apiGatewayService).verifyApplication(appId, authType, authCredential);
            verify(apiGatewayService, never()).checkPermission(anyString(), anyString());
        }

        @Test
        @DisplayName("权限校验失败 - checkPermission 返回 authorized=false，返回 403")
        void testProxyApiRequest_PermissionDenied() {
            String appId = "app001";
            Integer authType = 1;
            String authCredential = "valid-credential";
            String body = "{\"data\":\"test\"}";

            when(apiGatewayService.verifyApplication(appId, authType, authCredential)).thenReturn(true);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/gateway/api/v1/messages");
            when(apiGatewayService.findScopeByPathAndMethod(anyString(), anyString())).thenReturn("api:v1:messages:post");
            when(apiGatewayService.checkPermission(eq(appId), anyString()))
                    .thenReturn(PermissionCheckResponse.builder()
                            .authorized(false)
                            .reason("应用未订阅该权限")
                            .build());

            ResponseEntity<String> response = apiGatewayController.proxyApiRequest(appId, authType, authCredential, body, request);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().contains("\"code\":\"403\""));
            assertTrue(response.getBody().contains("应用未订阅该权限"));
            verify(apiGatewayService).verifyApplication(appId, authType, authCredential);
            verify(apiGatewayService).checkPermission(eq(appId), anyString());
        }

        @Test
        @DisplayName("异常处理 - 抛出异常，返回 500")
        void testProxyApiRequest_Exception() {
            String appId = "app001";
            Integer authType = 1;
            String authCredential = "valid-credential";
            String body = "{\"data\":\"test\"}";

            when(apiGatewayService.verifyApplication(appId, authType, authCredential)).thenReturn(true);
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/gateway/api/v1/messages");
            when(apiGatewayService.findScopeByPathAndMethod(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Database connection error"));

            ResponseEntity<String> response = apiGatewayController.proxyApiRequest(appId, authType, authCredential, body, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().contains("\"code\":\"500\""));
            assertTrue(response.getBody().contains("服务器内部错误"));
        }
    }

    @Nested
    @DisplayName("getCallbackConfig 测试")
    class GetCallbackConfigTests {

        @Test
        @DisplayName("查询成功")
        void testGetCallbackConfig_Success() {
            CallbackConfigRequest configRequest = new CallbackConfigRequest();
            configRequest.setAk("AK123456789");
            configRequest.setScope("callback:approval:completed");

            CallbackConfigResponse mockResponse = CallbackConfigResponse.builder()
                    .ak("AK123456789")
                    .scope("callback:approval:completed")
                    .channelType(1)
                    .channelAddress("https://webhook.example.com/callbacks")
                    .authType(1)
                    .build();

            when(apiGatewayService.getCallbackConfig("AK123456789", "callback:approval:completed"))
                    .thenReturn(mockResponse);

            ApiResponse<CallbackConfigResponse> response = apiGatewayController.getCallbackConfig("Bearer token", configRequest);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("AK123456789", response.getData().getAk());
            assertEquals("callback:approval:completed", response.getData().getScope());
            assertEquals(1, response.getData().getChannelType());
            verify(apiGatewayService).getCallbackConfig("AK123456789", "callback:approval:completed");
        }

        @Test
        @DisplayName("未找到配置 - 返回 null")
        void testGetCallbackConfig_NotFound() {
            CallbackConfigRequest configRequest = new CallbackConfigRequest();
            configRequest.setAk("AK_NOT_EXIST");
            configRequest.setScope("callback:approval:completed");

            when(apiGatewayService.getCallbackConfig("AK_NOT_EXIST", "callback:approval:completed"))
                    .thenReturn(null);

            ApiResponse<CallbackConfigResponse> response = apiGatewayController.getCallbackConfig("Bearer token", configRequest);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNull(response.getData());
            verify(apiGatewayService).getCallbackConfig("AK_NOT_EXIST", "callback:approval:completed");
        }

        @Test
        @DisplayName("异常处理 - 抛出异常")
        void testGetCallbackConfig_Exception() {
            CallbackConfigRequest configRequest = new CallbackConfigRequest();
            configRequest.setAk("AK123456789");
            configRequest.setScope("callback:approval:completed");

            when(apiGatewayService.getCallbackConfig("AK123456789", "callback:approval:completed"))
                    .thenThrow(new RuntimeException("Database error"));

            ApiResponse<CallbackConfigResponse> response = apiGatewayController.getCallbackConfig("Bearer token", configRequest);

            assertNotNull(response);
            assertEquals("400", response.getCode());
            assertTrue(response.getMessageZh().contains("查询失败"));
            verify(apiGatewayService).getCallbackConfig("AK123456789", "callback:approval:completed");
        }
    }
}
