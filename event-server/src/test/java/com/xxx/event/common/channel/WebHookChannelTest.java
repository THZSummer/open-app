package com.xxx.event.common.channel;

import com.xxx.event.common.auth.AuthContext;
import com.xxx.event.common.auth.AuthHandler;
import com.xxx.event.common.auth.AuthTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebHookChannel 单元测试
 * 
 * <p>测试 WebHookChannel 的所有公开方法</p>
 * 
 * <p>测试覆盖：</p>
 * <ul>
 *   <li>sendEvent 方法测试（带appId+authType、带AuthContext）</li>
 *   <li>sendCallback 方法测试（带appId+authType、带AuthContext）</li>
 *   <li>sendWebHook 方法测试（成功发送、失败处理、异常处理）</li>
 *   <li>sendSync 方法测试（成功、失败、异常）</li>
 *   <li>认证头设置验证</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 * @since 2026-04-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebHookChannel 单元测试")
class WebHookChannelTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuthHandler authHandler;

    private WebHookChannel webHookChannel;

    private static final String TEST_URL = "http://test.example.com/webhook";
    private static final String TEST_APP_ID = "app-123";
    private static final Map<String, Object> TEST_PAYLOAD = createTestPayload();

    private static Map<String, Object> createTestPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", "event-001");
        payload.put("type", "user.created");
        payload.put("data", Map.of("userId", "user-123", "name", "Test User"));
        return payload;
    }

    @BeforeEach
    void setUp() {
        webHookChannel = new WebHookChannel(restTemplate, authHandler);
    }

    @Nested
    @DisplayName("sendEvent 方法测试")
    class SendEventTests {

        @Test
        @DisplayName("sendEvent - 带appId和authType - 成功发送")
        void testSendEvent_WithAppIdAndAuthType_Success() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendEvent(TEST_URL, TEST_PAYLOAD, TEST_APP_ID, AuthTypeEnum.IAM);

            // Then
            ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(),
                    eq(String.class)
            );

            // 验证认证处理器被调用
            verify(authHandler).applyAuth(any(HttpHeaders.class), eq(TEST_APP_ID), eq(AuthTypeEnum.IAM));

            // 验证请求头
            HttpEntity<Map<String, Object>> capturedEntity = entityCaptor.getValue();
            assertEquals(MediaType.APPLICATION_JSON, capturedEntity.getHeaders().getContentType());
            assertEquals(TEST_PAYLOAD, capturedEntity.getBody());
        }

        @Test
        @DisplayName("sendEvent - 带AuthContext - 成功发送")
        void testSendEvent_WithAuthContext_Success() {

            // Given
            AuthContext authContext = AuthContext.iam(TEST_APP_ID);
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendEvent(TEST_URL, TEST_PAYLOAD, authContext);

            // Then
            verify(authHandler).applyAuth(any(HttpHeaders.class), eq(TEST_APP_ID), eq(AuthTypeEnum.IAM));
            verify(restTemplate).exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("sendEvent - 免认证模式 - 不调用认证处理器")
        void testSendEvent_NoAuthMode_ShouldNotCallAuthHandler() {

            // Given
            AuthContext authContext = AuthContext.noAuth(TEST_APP_ID);
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendEvent(TEST_URL, TEST_PAYLOAD, authContext);

            // Then - 免认证模式下不应调用认证处理器
            verify(authHandler, never()).applyAuth(any(), any(), any());
            verify(restTemplate).exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("sendEvent - null AuthContext - 不调用认证处理器")
        void testSendEvent_NullAuthContext_ShouldNotCallAuthHandler() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendEvent(TEST_URL, TEST_PAYLOAD, (AuthContext) null);

            // Then
            verify(authHandler, never()).applyAuth(any(), any(), any());
            verify(restTemplate).exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("sendEvent - HTTP状态码非2xx - 记录警告日志")
        void testSendEvent_Non2xxStatus_ShouldLogWarning() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendEvent(TEST_URL, TEST_PAYLOAD, TEST_APP_ID, AuthTypeEnum.SOA);

            // Then - 方法应正常完成，不抛异常
            verify(restTemplate).exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("sendEvent - 发送异常 - 捕获并记录错误")
        void testSendEvent_ExceptionThrown_ShouldCatchAndLog() {

            // Given
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("Connection refused"));

            // When & Then - 方法应正常完成，不抛异常
            assertDoesNotThrow(() -> 
                webHookChannel.sendEvent(TEST_URL, TEST_PAYLOAD, TEST_APP_ID, AuthTypeEnum.COOKIE)
            );

            verify(restTemplate).exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    @Nested
    @DisplayName("sendCallback 方法测试")
    class SendCallbackTests {

        @Test
        @DisplayName("sendCallback - 带appId和authType - 成功发送")
        void testSendCallback_WithAppIdAndAuthType_Success() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendCallback(TEST_URL, TEST_PAYLOAD, TEST_APP_ID, AuthTypeEnum.APIG);

            // Then
            verify(authHandler).applyAuth(any(HttpHeaders.class), eq(TEST_APP_ID), eq(AuthTypeEnum.APIG));
            verify(restTemplate).exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("sendCallback - 带AuthContext - 成功发送")
        void testSendCallback_WithAuthContext_Success() {

            // Given
            AuthContext authContext = AuthContext.aksk(TEST_APP_ID);
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendCallback(TEST_URL, TEST_PAYLOAD, authContext);

            // Then
            verify(authHandler).applyAuth(any(HttpHeaders.class), eq(TEST_APP_ID), eq(AuthTypeEnum.AKSK));
            verify(restTemplate).exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("sendCallback - 使用COOKIE认证 - 正确应用认证")
        void testSendCallback_WithCookieAuth_ShouldApplyCorrectly() {

            // Given
            AuthContext authContext = AuthContext.cookie(TEST_APP_ID);
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendCallback(TEST_URL, TEST_PAYLOAD, authContext);

            // Then
            verify(authHandler).applyAuth(any(HttpHeaders.class), eq(TEST_APP_ID), eq(AuthTypeEnum.COOKIE));
        }

        @Test
        @DisplayName("sendCallback - 使用SOA认证 - 正确应用认证")
        void testSendCallback_WithSoaAuth_ShouldApplyCorrectly() {

            // Given
            AuthContext authContext = AuthContext.soa(TEST_APP_ID);
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendCallback(TEST_URL, TEST_PAYLOAD, authContext);

            // Then
            verify(authHandler).applyAuth(any(HttpHeaders.class), eq(TEST_APP_ID), eq(AuthTypeEnum.SOA));
        }

        @Test
        @DisplayName("sendCallback - 免认证模式 - 不调用认证处理器")
        void testSendCallback_NoAuthMode_ShouldNotCallAuthHandler() {

            // Given
            AuthContext authContext = AuthContext.noAuth(TEST_APP_ID);
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendCallback(TEST_URL, TEST_PAYLOAD, authContext);

            // Then
            verify(authHandler, never()).applyAuth(any(), any(), any());
        }

        @Test
        @DisplayName("sendCallback - 发送异常 - 捕获并记录错误")
        void testSendCallback_ExceptionThrown_ShouldCatchAndLog() {

            // Given
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("Network error"));

            // When & Then - 方法应正常完成，不抛异常
            assertDoesNotThrow(() -> 
                webHookChannel.sendCallback(TEST_URL, TEST_PAYLOAD, TEST_APP_ID, AuthTypeEnum.IAM)
            );
        }
    }

    @Nested
    @DisplayName("sendSync 方法测试")
    class SendSyncTests {

        @Test
        @DisplayName("sendSync - 无认证 - 成功发送返回true")
        void testSendSync_WithoutAuth_Success() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertTrue(result);
            verify(restTemplate).exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
            verify(authHandler, never()).applyAuth(any(), any(), any());
        }

        @Test
        @DisplayName("sendSync - 带认证 - 成功发送返回true")
        void testSendSync_WithAuth_Success() {

            // Given
            AuthContext authContext = AuthContext.iam(TEST_APP_ID);
            ResponseEntity<String> mockResponse = new ResponseEntity<>("Created", HttpStatus.CREATED);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD, authContext);

            // Then
            assertTrue(result);
            verify(authHandler).applyAuth(any(HttpHeaders.class), eq(TEST_APP_ID), eq(AuthTypeEnum.IAM));
        }

        @Test
        @DisplayName("sendSync - HTTP状态码非2xx - 返回false")
        void testSendSync_Non2xxStatus_ShouldReturnFalse() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("Bad Request", HttpStatus.BAD_REQUEST);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("sendSync - 服务器错误 - 返回false")
        void testSendSync_ServerError_ShouldReturnFalse() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("sendSync - 发送异常 - 返回false")
        void testSendSync_ExceptionThrown_ShouldReturnFalse() {

            // Given
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("Connection timeout"));

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("sendSync - 带认证异常 - 返回false")
        void testSendSync_WithAuthException_ShouldReturnFalse() {

            // Given
            AuthContext authContext = AuthContext.iam(TEST_APP_ID);
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("Auth failed"));

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD, authContext);

            // Then
            assertFalse(result);
            verify(authHandler).applyAuth(any(HttpHeaders.class), eq(TEST_APP_ID), eq(AuthTypeEnum.IAM));
        }

        @Test
        @DisplayName("sendSync - 免认证模式 - 不调用认证处理器")
        void testSendSync_NoAuthMode_ShouldNotCallAuthHandler() {

            // Given
            AuthContext authContext = AuthContext.noAuth(TEST_APP_ID);
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD, authContext);

            // Then
            assertTrue(result);
            verify(authHandler, never()).applyAuth(any(), any(), any());
        }

        @Test
        @DisplayName("sendSync - null AuthContext - 不调用认证处理器")
        void testSendSync_NullAuthContext_ShouldNotCallAuthHandler() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD, null);

            // Then
            assertTrue(result);
            verify(authHandler, never()).applyAuth(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("认证应用测试")
    class AuthApplicationTests {

        @Test
        @DisplayName("认证处理器被正确调用 - IAM认证")
        void testAuthHandler_CalledCorrectly_ForIamAuth() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendEvent(TEST_URL, TEST_PAYLOAD, TEST_APP_ID, AuthTypeEnum.IAM);

            // Then
            verify(authHandler).applyAuth(
                    any(HttpHeaders.class), 
                    eq(TEST_APP_ID), 
                    eq(AuthTypeEnum.IAM)
            );
        }

        @Test
        @DisplayName("认证处理器被正确调用 - AKSK认证")
        void testAuthHandler_CalledCorrectly_ForAkskAuth() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendEvent(TEST_URL, TEST_PAYLOAD, TEST_APP_ID, AuthTypeEnum.AKSK);

            // Then
            verify(authHandler).applyAuth(
                    any(HttpHeaders.class), 
                    eq(TEST_APP_ID), 
                    eq(AuthTypeEnum.AKSK)
            );
        }

        @Test
        @DisplayName("认证处理器被正确调用 - APIG认证")
        void testAuthHandler_CalledCorrectly_ForApigAuth() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendCallback(TEST_URL, TEST_PAYLOAD, TEST_APP_ID, AuthTypeEnum.APIG);

            // Then
            verify(authHandler).applyAuth(
                    any(HttpHeaders.class), 
                    eq(TEST_APP_ID), 
                    eq(AuthTypeEnum.APIG)
            );
        }

        @Test
        @DisplayName("HTTP请求头设置验证")
        void testHttpHeaders_SetCorrectly() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendEvent(TEST_URL, TEST_PAYLOAD, TEST_APP_ID, AuthTypeEnum.IAM);

            // Then
            HttpEntity<Map<String, Object>> capturedEntity = entityCaptor.getValue();
            HttpHeaders headers = capturedEntity.getHeaders();
            
            // 验证 Content-Type
            assertNotNull(headers.getContentType());
            assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
            
            // 验证请求体
            assertEquals(TEST_PAYLOAD, capturedEntity.getBody());
        }
    }

    @Nested
    @DisplayName("不同认证类型测试")
    class DifferentAuthTypesTests {

        @Test
        @DisplayName("COOKIE认证 - 正确传递认证参数")
        void testCookieAuth_ShouldPassCorrectParams() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    anyString(), any(), any(), eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD, AuthContext.cookie(TEST_APP_ID));

            // Then
            verify(authHandler).applyAuth(any(), eq(TEST_APP_ID), eq(AuthTypeEnum.COOKIE));
        }

        @Test
        @DisplayName("SOA认证 - 正确传递认证参数")
        void testSoaAuth_ShouldPassCorrectParams() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    anyString(), any(), any(), eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD, AuthContext.soa(TEST_APP_ID));

            // Then
            verify(authHandler).applyAuth(any(), eq(TEST_APP_ID), eq(AuthTypeEnum.SOA));
        }

        @Test
        @DisplayName("CLITOKEN认证 - 正确传递认证参数")
        void testClitokenAuth_ShouldPassCorrectParams() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            AuthContext authContext = AuthContext.of(TEST_APP_ID, AuthTypeEnum.CLITOKEN);
            
            when(restTemplate.exchange(
                    anyString(), any(), any(), eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD, authContext);

            // Then
            verify(authHandler).applyAuth(any(), eq(TEST_APP_ID), eq(AuthTypeEnum.CLITOKEN));
        }

        @Test
        @DisplayName("多种认证类型 - 验证requiresAuth逻辑")
        void testVariousAuthTypes_RequiresAuth() {

            // 需要认证的类型
            assertTrue(AuthTypeEnum.COOKIE.requiresAuth());
            assertTrue(AuthTypeEnum.SOA.requiresAuth());
            assertTrue(AuthTypeEnum.APIG.requiresAuth());
            assertTrue(AuthTypeEnum.IAM.requiresAuth());
            assertTrue(AuthTypeEnum.AKSK.requiresAuth());
            assertTrue(AuthTypeEnum.CLITOKEN.requiresAuth());
            
            // 不需要认证的类型
            assertFalse(AuthTypeEnum.NONE.requiresAuth());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("空payload - 应正常发送")
        void testEmptyPayload_ShouldSendSuccessfully() {

            // Given
            Map<String, Object> emptyPayload = new HashMap<>();
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, emptyPayload);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("空appId - 应正常处理")
        void testEmptyAppId_ShouldHandleGracefully() {

            // Given
            AuthContext authContext = AuthContext.of("", AuthTypeEnum.IAM);
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    anyString(), any(), any(), eq(String.class)
            )).thenReturn(mockResponse);

            // When
            webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD, authContext);

            // Then - 即使appId为空，也应该调用认证处理器（由AuthHandler决定如何处理）
            verify(authHandler).applyAuth(any(), eq(""), eq(AuthTypeEnum.IAM));
        }

        @Test
        @DisplayName("null payload - 应正常发送")
        void testNullPayload_ShouldSendSuccessfully() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, null);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("大型payload - 应正常发送")
        void testLargePayload_ShouldSendSuccessfully() {

            // Given
            Map<String, Object> largePayload = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                largePayload.put("key" + i, "value" + i);
            }
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(TEST_URL),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, largePayload);

            // Then
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("HTTP状态码测试")
    class HttpStatusCodeTests {

        @Test
        @DisplayName("状态码200 - 返回true")
        void testStatusCode200_ShouldReturnTrue() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("状态码201 - 返回true")
        void testStatusCode201_ShouldReturnTrue() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("Created", HttpStatus.CREATED);
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("状态码204 - 返回true")
        void testStatusCode204_ShouldReturnTrue() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>(HttpStatus.NO_CONTENT);
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("状态码301 - 返回false")
        void testStatusCode301_ShouldReturnFalse() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>(HttpStatus.MOVED_PERMANENTLY);
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("状态码401 - 返回false")
        void testStatusCode401_ShouldReturnFalse() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("状态码500 - 返回false")
        void testStatusCode500_ShouldReturnFalse() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("状态码503 - 返回false")
        void testStatusCode503_ShouldReturnFalse() {

            // Given
            ResponseEntity<String> mockResponse = new ResponseEntity<>("Service Unavailable", HttpStatus.SERVICE_UNAVAILABLE);
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(mockResponse);

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionTests {

        @Test
        @DisplayName("RestClientException - sendSync返回false")
        void testRestClientException_ShouldReturnFalse() {

            // Given
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("RuntimeException - sendSync返回false")
        void testRuntimeException_ShouldReturnFalse() {

            // Given
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("InterruptedException - sendEvent捕获异常")
        void testInterruptedException_ShouldCatchException() {

            // Given
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenThrow(new RuntimeException("Thread interrupted"));

            // When & Then
            assertDoesNotThrow(() -> 
                webHookChannel.sendEvent(TEST_URL, TEST_PAYLOAD, TEST_APP_ID, AuthTypeEnum.IAM)
            );
        }

        @Test
        @DisplayName("IllegalArgumentException - sendSync返回false")
        void testIllegalArgumentException_ShouldReturnFalse() {

            // Given
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenThrow(new IllegalArgumentException("Invalid URL"));

            // When
            boolean result = webHookChannel.sendSync(TEST_URL, TEST_PAYLOAD);

            // Then
            assertFalse(result);
        }
    }
}
