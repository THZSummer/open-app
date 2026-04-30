package com.xxx.event.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.event.common.model.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApiServerClient 单元测试
 * 
 * <p>测试覆盖所有公开方法和私有方法</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiServerClient 单元测试")
class ApiServerClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ApiServerClient apiServerClient;

    private static final String API_SERVER_URL = "http://localhost:18081";
    private static final String SCOPE = "user:read";
    private static final String APP_ID = "app-001";

    @BeforeEach
    void setUp() {

        // 设置配置属性
        ReflectionTestUtils.setField(apiServerClient, "apiServerUrl", API_SERVER_URL);
        ReflectionTestUtils.setField(apiServerClient, "authEnabled", true);
    }

    /**
     * 测试 getPermissionByScope 方法
     */
    @Nested
    @DisplayName("getPermissionByScope 方法测试")
    class GetPermissionByScopeTest {

        @Test
        @DisplayName("成功查询权限详情")
        void testGetPermissionByScope_Success() throws Exception {

            // 准备测试数据
            Map<String, Object> permissionData = new HashMap<>();
            permissionData.put("scope", SCOPE);
            permissionData.put("name", "用户读取权限");
            permissionData.put("description", "读取用户基本信息");

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(permissionData);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            Map<String, Object> result = apiServerClient.getPermissionByScope(SCOPE);

            // 验证结果
            assertNotNull(result);
            assertEquals(SCOPE, result.get("scope"));
            assertEquals("用户读取权限", result.get("name"));

            // 验证方法调用
            verify(restTemplate, times(1)).exchange(
                    eq(API_SERVER_URL + "/gateway/permissions/detail?scope=" + SCOPE),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("查询权限详情失败 - API返回错误码")
        void testGetPermissionByScope_ApiError() throws Exception {

            // 准备测试数据 - API 返回错误
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.error("404", "权限不存在", "Permission not found");
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            Map<String, Object> result = apiServerClient.getPermissionByScope(SCOPE);

            // 验证结果 - 应该返回 null
            assertNull(result);

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("查询权限详情异常 - 网络错误")
        void testGetPermissionByScope_NetworkError() {

            // Mock RestTemplate 抛出异常
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("网络连接失败"));

            // 执行测试
            Map<String, Object> result = apiServerClient.getPermissionByScope(SCOPE);

            // 验证结果 - 异常情况返回 null
            assertNull(result);

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("查询权限详情异常 - JSON解析错误")
        void testGetPermissionByScope_JsonParseError() {

            // Mock RestTemplate 返回无效 JSON
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok("invalid json"));

            // 执行测试
            Map<String, Object> result = apiServerClient.getPermissionByScope(SCOPE);

            // 验证结果 - 解析失败返回 null
            assertNull(result);

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    /**
     * 测试 getSubscribedApps 方法
     */
    @Nested
    @DisplayName("getSubscribedApps 方法测试")
    class GetSubscribedAppsTest {

        @Test
        @DisplayName("成功查询订阅应用列表")
        void testGetSubscribedApps_Success() throws Exception {

            // 准备测试数据
            List<String> appList = Arrays.asList("app-001", "app-002", "app-003");
            ApiResponse<List<String>> apiResponse = ApiResponse.success(appList);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            List<String> result = apiServerClient.getSubscribedApps(SCOPE);

            // 验证结果
            assertNotNull(result);
            assertEquals(3, result.size());
            assertTrue(result.contains("app-001"));
            assertTrue(result.contains("app-002"));
            assertTrue(result.contains("app-003"));

            // 验证方法调用
            verify(restTemplate, times(1)).exchange(
                    eq(API_SERVER_URL + "/gateway/permissions/subscribers?scope=" + SCOPE),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("查询订阅应用列表失败 - API返回错误码")
        void testGetSubscribedApps_ApiError() throws Exception {

            // 准备测试数据 - API 返回错误
            ApiResponse<List<String>> apiResponse = ApiResponse.error("500", "服务器内部错误", "Internal server error");
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            List<String> result = apiServerClient.getSubscribedApps(SCOPE);

            // 验证结果 - 应该返回空列表
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("查询订阅应用列表异常 - 网络错误")
        void testGetSubscribedApps_NetworkError() {

            // Mock RestTemplate 抛出异常
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("连接超时"));

            // 执行测试
            List<String> result = apiServerClient.getSubscribedApps(SCOPE);

            // 验证结果 - 异常情况返回空列表
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("查询订阅应用列表 - 空列表")
        void testGetSubscribedApps_EmptyList() throws Exception {

            // 准备测试数据 - 空列表
            ApiResponse<List<String>> apiResponse = ApiResponse.success(List.of());
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            List<String> result = apiServerClient.getSubscribedApps(SCOPE);

            // 验证结果
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    /**
     * 测试 getSubscriptionConfig 方法
     */
    @Nested
    @DisplayName("getSubscriptionConfig 方法测试")
    class GetSubscriptionConfigTest {

        @Test
        @DisplayName("成功查询订阅配置")
        void testGetSubscriptionConfig_Success() throws Exception {

            // 准备测试数据
            Map<String, Object> configData = new HashMap<>();
            configData.put("appId", APP_ID);
            configData.put("scope", SCOPE);
            configData.put("callbackUrl", "https://app.example.com/callback");
            configData.put("enabled", true);

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(configData);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            Map<String, Object> result = apiServerClient.getSubscriptionConfig(APP_ID, SCOPE);

            // 验证结果
            assertNotNull(result);
            assertEquals(APP_ID, result.get("appId"));
            assertEquals(SCOPE, result.get("scope"));
            assertEquals("https://app.example.com/callback", result.get("callbackUrl"));

            // 验证方法调用
            verify(restTemplate, times(1)).exchange(
                    eq(API_SERVER_URL + "/gateway/subscriptions/config?appId=" + APP_ID + "&scope=" + SCOPE),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("查询订阅配置失败 - API返回错误码")
        void testGetSubscriptionConfig_ApiError() throws Exception {

            // 准备测试数据 - API 返回错误
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.error("404", "订阅配置不存在", "Subscription config not found");
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            Map<String, Object> result = apiServerClient.getSubscriptionConfig(APP_ID, SCOPE);

            // 验证结果 - 应该返回空 Map
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("查询订阅配置异常 - 网络错误")
        void testGetSubscriptionConfig_NetworkError() {

            // Mock RestTemplate 抛出异常
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("服务不可用"));

            // 执行测试
            Map<String, Object> result = apiServerClient.getSubscriptionConfig(APP_ID, SCOPE);

            // 验证结果 - 异常情况返回空 Map
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    /**
     * 测试 checkPermission 方法
     */
    @Nested
    @DisplayName("checkPermission 方法测试")
    class CheckPermissionTest {

        @Test
        @DisplayName("权限校验 - 已授权")
        void testCheckPermission_Authorized() throws Exception {

            // 准备测试数据
            Map<String, Object> data = new HashMap<>();
            data.put("authorized", true);
            data.put("appId", APP_ID);
            data.put("scope", SCOPE);

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(data);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            boolean result = apiServerClient.checkPermission(APP_ID, SCOPE);

            // 验证结果
            assertTrue(result);

            // 验证方法调用
            verify(restTemplate, times(1)).exchange(
                    eq(API_SERVER_URL + "/gateway/permissions/check?appId=" + APP_ID + "&scope=" + SCOPE),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("权限校验 - 未授权")
        void testCheckPermission_Unauthorized() throws Exception {

            // 准备测试数据
            Map<String, Object> data = new HashMap<>();
            data.put("authorized", false);
            data.put("appId", APP_ID);
            data.put("scope", SCOPE);
            data.put("reason", "应用未订阅此权限");

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(data);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            boolean result = apiServerClient.checkPermission(APP_ID, SCOPE);

            // 验证结果
            assertFalse(result);

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("权限校验 - 数据为null")
        void testCheckPermission_NullData() throws Exception {

            // 准备测试数据 - data 为 null
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(null);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            boolean result = apiServerClient.checkPermission(APP_ID, SCOPE);

            // 验证结果
            assertFalse(result);

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("权限校验 - authorized字段缺失")
        void testCheckPermission_MissingAuthorizedField() throws Exception {

            // 准备测试数据 - 没有 authorized 字段
            Map<String, Object> data = new HashMap<>();
            data.put("appId", APP_ID);
            data.put("scope", SCOPE);

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(data);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            boolean result = apiServerClient.checkPermission(APP_ID, SCOPE);

            // 验证结果 - authorized 字段缺失，返回 false
            assertFalse(result);

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("权限校验 - API返回错误")
        void testCheckPermission_ApiError() throws Exception {

            // 准备测试数据 - API 返回错误
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.error("403", "权限不足", "Forbidden");
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            boolean result = apiServerClient.checkPermission(APP_ID, SCOPE);

            // 验证结果
            assertFalse(result);

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("权限校验异常 - 网络错误")
        void testCheckPermission_NetworkError() {

            // Mock RestTemplate 抛出异常
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("连接被拒绝"));

            // 执行测试
            boolean result = apiServerClient.checkPermission(APP_ID, SCOPE);

            // 验证结果 - 异常情况返回 false
            assertFalse(result);

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    /**
     * 测试 applyAuth 方法（私有方法）
     */
    @Nested
    @DisplayName("applyAuth 方法测试")
    class ApplyAuthTest {

        @Test
        @DisplayName("认证启用 - 添加认证头")
        void testApplyAuth_AuthEnabled() throws Exception {

            // 设置认证启用
            ReflectionTestUtils.setField(apiServerClient, "authEnabled", true);

            // 准备测试数据
            Map<String, Object> permissionData = new HashMap<>();
            permissionData.put("scope", SCOPE);
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(permissionData);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应并捕获 HttpEntity
            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    entityCaptor.capture(),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            apiServerClient.getPermissionByScope(SCOPE);

            // 验证 HttpEntity 中的 Header
            HttpEntity<String> capturedEntity = entityCaptor.getValue();
            assertNotNull(capturedEntity);
            assertNotNull(capturedEntity.getHeaders());
            
            // 由于 getApiServerCredential() 返回空 Map，Header 应该是空的
            // 但方法仍然会被调用
            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("认证禁用 - 不添加认证头")
        void testApplyAuth_AuthDisabled() throws Exception {

            // 设置认证禁用
            ReflectionTestUtils.setField(apiServerClient, "authEnabled", false);

            // 准备测试数据
            Map<String, Object> permissionData = new HashMap<>();
            permissionData.put("scope", SCOPE);
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(permissionData);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            // Mock RestTemplate 响应并捕获 HttpEntity
            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    entityCaptor.capture(),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            apiServerClient.getPermissionByScope(SCOPE);

            // 验证 HttpEntity 中的 Header
            HttpEntity<String> capturedEntity = entityCaptor.getValue();
            assertNotNull(capturedEntity);
            assertNotNull(capturedEntity.getHeaders());

            // 认证禁用时，Header 应该是空的
            assertTrue(capturedEntity.getHeaders().isEmpty());

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("直接测试 applyAuth 私有方法 - 认证启用")
        void testApplyAuthDirectly_AuthEnabled() throws Exception {

            // 设置认证启用
            ReflectionTestUtils.setField(apiServerClient, "authEnabled", true);

            // 使用反射调用私有方法
            Method applyAuthMethod = ApiServerClient.class.getDeclaredMethod("applyAuth", org.springframework.http.HttpHeaders.class);
            applyAuthMethod.setAccessible(true);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            applyAuthMethod.invoke(apiServerClient, headers);

            // 验证：由于 getApiServerCredential() 返回空 Map，Header 应该是空的
            assertTrue(headers.isEmpty());
        }

        @Test
        @DisplayName("直接测试 applyAuth 私有方法 - 认证禁用")
        void testApplyAuthDirectly_AuthDisabled() throws Exception {

            // 设置认证禁用
            ReflectionTestUtils.setField(apiServerClient, "authEnabled", false);

            // 使用反射调用私有方法
            Method applyAuthMethod = ApiServerClient.class.getDeclaredMethod("applyAuth", org.springframework.http.HttpHeaders.class);
            applyAuthMethod.setAccessible(true);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            applyAuthMethod.invoke(apiServerClient, headers);

            // 验证：认证禁用时，Header 应该是空的
            assertTrue(headers.isEmpty());
        }
    }

    /**
     * 测试 getApiServerCredential 方法（私有方法）
     */
    @Nested
    @DisplayName("getApiServerCredential 方法测试")
    class GetApiServerCredentialTest {

        @Test
        @DisplayName("获取认证凭证 - 预留实现返回空Map")
        void testGetApiServerCredential_ReturnsEmptyMap() throws Exception {

            // 使用反射调用私有方法
            Method getCredentialMethod = ApiServerClient.class.getDeclaredMethod("getApiServerCredential");
            getCredentialMethod.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, String> result = (Map<String, String>) getCredentialMethod.invoke(apiServerClient);

            // 验证结果 - 预留实现应该返回空 Map
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    /**
     * 测试 HTTP 请求构建
     */
    @Nested
    @DisplayName("HTTP 请求构建测试")
    class HttpRequestBuildTest {

        @Test
        @DisplayName("验证请求URL构建正确")
        void testRequestUrlConstruction() throws Exception {

            // 准备测试数据
            Map<String, Object> data = new HashMap<>();
            data.put("scope", SCOPE);
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(data);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            apiServerClient.getPermissionByScope(SCOPE);

            // 验证 URL 参数
            verify(restTemplate).exchange(
                    eq(API_SERVER_URL + "/gateway/permissions/detail?scope=" + SCOPE),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("验证HTTP方法为GET")
        void testHttpMethodIsGet() throws Exception {

            // 准备测试数据
            Map<String, Object> data = new HashMap<>();
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(data);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            when(restTemplate.exchange(
                    anyString(),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            apiServerClient.getPermissionByScope(SCOPE);

            // 验证 HTTP 方法
            verify(restTemplate).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    /**
     * 测试边界条件
     */
    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTest {

        @Test
        @DisplayName("scope为null时的处理")
        void testNullScope() throws Exception {

            // 准备测试数据
            Map<String, Object> data = new HashMap<>();
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(data);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试 - scope 为 null
            Map<String, Object> result = apiServerClient.getPermissionByScope(null);

            // 验证结果
            assertNotNull(result);

            // URL 中会包含 "scope=null"
            verify(restTemplate).exchange(
                    contains("scope=null"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("appId为null时的处理")
        void testNullAppId() throws Exception {

            // 准备测试数据
            Map<String, Object> data = new HashMap<>();
            data.put("authorized", false);
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(data);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试 - appId 为 null
            boolean result = apiServerClient.checkPermission(null, SCOPE);

            // 验证结果
            assertFalse(result);

            // URL 中会包含 "appId=null"
            verify(restTemplate).exchange(
                    contains("appId=null"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("scope包含特殊字符")
        void testSpecialCharactersInScope() throws Exception {

            // 准备测试数据
            String specialScope = "user:read&write";
            Map<String, Object> data = new HashMap<>();
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(data);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            Map<String, Object> result = apiServerClient.getPermissionByScope(specialScope);

            // 验证结果
            assertNotNull(result);

            // 注意：实际项目中应该对特殊字符进行 URL 编码
            verify(restTemplate).exchange(
                    contains(specialScope),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    /**
     * 测试响应数据为空的场景
     */
    @Nested
    @DisplayName("空响应数据测试")
    class EmptyResponseTest {

        @Test
        @DisplayName("API响应data字段为null")
        void testApiResponseDataNull() throws Exception {

            // 准备测试数据 - data 为 null
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(null);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            Map<String, Object> result = apiServerClient.getPermissionByScope(SCOPE);

            // 验证结果
            assertNull(result);
        }

        @Test
        @DisplayName("API响应体为空字符串")
        void testEmptyResponseBody() {

            // Mock RestTemplate 返回空字符串
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(""));

            // 执行测试
            Map<String, Object> result = apiServerClient.getPermissionByScope(SCOPE);

            // 验证结果 - 解析失败返回 null
            assertNull(result);
        }
    }

    /**
     * 测试不同HTTP状态码
     */
    @Nested
    @DisplayName("HTTP状态码测试")
    class HttpStatusCodeTest {

        @Test
        @DisplayName("HTTP 200 成功")
        void testHttp200() throws Exception {

            // 准备测试数据
            Map<String, Object> data = new HashMap<>();
            data.put("authorized", true);
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(data);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(responseJson));

            // 执行测试
            boolean result = apiServerClient.checkPermission(APP_ID, SCOPE);

            // 验证结果
            assertTrue(result);
        }

        @Test
        @DisplayName("HTTP 500 服务器错误但响应体有效")
        void testHttp500WithValidBody() throws Exception {

            // 准备测试数据 - HTTP 500 但返回有效的 JSON
            Map<String, Object> data = new HashMap<>();
            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(data);
            String responseJson = objectMapper.writeValueAsString(apiResponse);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(responseJson, HttpStatus.INTERNAL_SERVER_ERROR));

            // 执行测试 - 即使 HTTP 状态码不是 200，只要响应体有效就能解析
            Map<String, Object> result = apiServerClient.getPermissionByScope(SCOPE);

            // 验证结果 - 能够解析响应体
            assertNotNull(result);
        }
    }
}
