package com.xxx.api.gateway.service;

import com.xxx.api.common.entity.Permission;
import com.xxx.api.common.entity.Subscription;
import com.xxx.api.common.mapper.PermissionMapper;
import com.xxx.api.common.mapper.SubscriptionMapper;
import com.xxx.api.common.service.ApplicationService;
import com.xxx.api.gateway.dto.CallbackConfigResponse;
import com.xxx.api.gateway.dto.PermissionCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * API 网关服务测试
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@DisplayName("API 网关服务测试")
class ApiGatewayServiceTest {

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private ApiGatewayService apiGatewayService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ==================== 权限校验测试 ====================

    @Nested
    @DisplayName("权限校验测试")
    class CheckPermissionTests {

        @Test
        @DisplayName("授权成功")
        void testCheckPermission_Authorized() {
            // 准备数据
            String appId = "10";
            String scope = "api:im:send-message";

            Permission permission = new Permission();
            permission.setId(200L);
            permission.setScope(scope);
            permission.setStatus(1);

            Subscription subscription = new Subscription();
            subscription.setId(300L);
            subscription.setAppId(10L);
            subscription.setPermissionId(200L);
            subscription.setStatus(1);

            when(permissionMapper.selectByScope(anyString()))
                    .thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong()))
                    .thenReturn(subscription);

            // 执行测试
            PermissionCheckResponse response = apiGatewayService.checkPermission(appId, scope);

            // 验证结果
            assertNotNull(response);
            assertTrue(response.getAuthorized());
            assertEquals("300", response.getSubscriptionId());
            assertEquals(1, response.getSubscriptionStatus());

            // 验证调用
            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, times(1)).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("权限不存在")
        void testCheckPermission_PermissionNotFound() {
            // 准备数据
            String appId = "10";
            String scope = "api:im:send-message";

            when(permissionMapper.selectByScope(anyString()))
                    .thenReturn(null);

            // 执行测试
            PermissionCheckResponse response = apiGatewayService.checkPermission(appId, scope);

            // 验证结果
            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("权限不存在", response.getReason());

            // 验证调用
            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, never()).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("未订阅")
        void testCheckPermission_NotSubscribed() {
            // 准备数据
            String appId = "10";
            String scope = "api:im:send-message";

            Permission permission = new Permission();
            permission.setId(200L);
            permission.setScope(scope);
            permission.setStatus(1);

            when(permissionMapper.selectByScope(anyString()))
                    .thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong()))
                    .thenReturn(null);

            // 执行测试
            PermissionCheckResponse response = apiGatewayService.checkPermission(appId, scope);

            // 验证结果
            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("应用未订阅该权限", response.getReason());

            // 验证调用
            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, times(1)).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("订阅待审批")
        void testCheckPermission_PendingApproval() {
            // 准备数据
            String appId = "10";
            String scope = "api:im:send-message";

            Permission permission = new Permission();
            permission.setId(200L);
            permission.setScope(scope);
            permission.setStatus(1);

            Subscription subscription = new Subscription();
            subscription.setId(300L);
            subscription.setAppId(10L);
            subscription.setPermissionId(200L);
            subscription.setStatus(0); // 待审批

            when(permissionMapper.selectByScope(anyString()))
                    .thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong()))
                    .thenReturn(subscription);

            // 执行测试
            PermissionCheckResponse response = apiGatewayService.checkPermission(appId, scope);

            // 验证结果
            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("订阅待审批", response.getReason());

            // 验证调用
            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, times(1)).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }
    }

    // ==================== 权限校验补充分支测试 ====================

        @Test
        @DisplayName("应用ID格式错误")
        void testCheckPermission_InvalidAppId() {
            PermissionCheckResponse response = apiGatewayService.checkPermission("abc", "api:im:send-message");

            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("应用ID格式错误", response.getReason());

            verify(permissionMapper, never()).selectByScope(anyString());
            verify(subscriptionMapper, never()).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("订阅已拒绝")
        void testCheckPermission_Rejected() {
            Permission permission = new Permission();
            permission.setId(200L);

            Subscription subscription = new Subscription();
            subscription.setId(300L);
            subscription.setStatus(2);

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong()))
                    .thenReturn(subscription);

            PermissionCheckResponse response = apiGatewayService.checkPermission("10", "api:im:send-message");

            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("订阅已拒绝", response.getReason());
            assertEquals("300", response.getSubscriptionId());
            assertEquals(2, response.getSubscriptionStatus());
        }

        @Test
        @DisplayName("订阅已取消")
        void testCheckPermission_Cancelled() {
            Permission permission = new Permission();
            permission.setId(200L);

            Subscription subscription = new Subscription();
            subscription.setId(300L);
            subscription.setStatus(3);

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong()))
                    .thenReturn(subscription);

            PermissionCheckResponse response = apiGatewayService.checkPermission("10", "api:im:send-message");

            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("订阅已取消", response.getReason());
            assertEquals("300", response.getSubscriptionId());
            assertEquals(3, response.getSubscriptionStatus());
        }

        @Test
        @DisplayName("订阅状态异常")
        void testCheckPermission_UnknownStatus() {
            Permission permission = new Permission();
            permission.setId(200L);

            Subscription subscription = new Subscription();
            subscription.setId(300L);
            subscription.setStatus(9);

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong()))
                    .thenReturn(subscription);

            PermissionCheckResponse response = apiGatewayService.checkPermission("10", "api:im:send-message");

            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("订阅状态异常", response.getReason());
            assertEquals("300", response.getSubscriptionId());
            assertEquals(9, response.getSubscriptionStatus());
        }

    @Nested
    @DisplayName("Scope 匹配测试")
    class FindScopeByPathAndMethodTests {

        @Test
        @DisplayName("根据路径和方法生成Scope")
        void testFindScopeByPathAndMethod_Success() {
            String scope = apiGatewayService.findScopeByPathAndMethod("/v1/messages", "GET");

            assertEquals("api::v1:messages:get", scope);
        }

        @Test
        @DisplayName("路径中非字母数字字符转换为冒号")
        void testFindScopeByPathAndMethod_NormalizesSpecialCharacters() {
            String scope = apiGatewayService.findScopeByPathAndMethod("/v1/messages/{id}", "POST");

            assertEquals("api::v1:messages::id::post", scope);
        }

        @Test
        @DisplayName("路径为空返回null")
        void testFindScopeByPathAndMethod_NullPath() {
            String scope = apiGatewayService.findScopeByPathAndMethod(null, "GET");

            assertNull(scope);
        }

        @Test
        @DisplayName("方法为空返回null")
        void testFindScopeByPathAndMethod_NullMethod() {
            String scope = apiGatewayService.findScopeByPathAndMethod("/v1/messages", null);

            assertNull(scope);
        }
    }

    // ==================== 应用身份验证测试 ====================

    @Nested
    @DisplayName("应用身份验证测试")
    class VerifyApplicationTests {

        @Test
        @DisplayName("验证成功")
        void testVerifyApplication_Success() {
            // 准备数据
            String appId = "10";
            Integer authType = 5;
            String authCredential = "signature123";

            when(applicationService.verifyApplication(anyString(), anyInt(), anyString()))
                    .thenReturn(true);

            // 执行测试
            boolean result = apiGatewayService.verifyApplication(appId, authType, authCredential);

            // 验证结果
            assertTrue(result);

            // 验证调用
            verify(applicationService, times(1)).verifyApplication(appId, authType, authCredential);
        }

        @Test
        @DisplayName("验证失败")
        void testVerifyApplication_Failed() {
            // 准备数据
            String appId = "10";
            Integer authType = 5;
            String authCredential = "invalid-signature";

            when(applicationService.verifyApplication(anyString(), anyInt(), anyString()))
                    .thenReturn(false);

            // 执行测试
            boolean result = apiGatewayService.verifyApplication(appId, authType, authCredential);

            // 验证结果
            assertFalse(result);

            // 验证调用
            verify(applicationService, times(1)).verifyApplication(appId, authType, authCredential);
        }
    }

    // ==================== 回调配置查询测试 ====================

    @Nested
    @DisplayName("回调配置查询测试")
    class GetCallbackConfigTests {

        @Test
        @DisplayName("查询成功 - WebHook通道")
        void testGetCallbackConfig_Success_WebHook() {
            // 准备数据
            String ak = "AK123456789";
            String scope = "callback:approval:completed";
            Long appId = 10L;

            Subscription subscription = new Subscription();
            subscription.setId(302L);
            subscription.setAppId(appId);
            subscription.setPermissionId(202L);
            subscription.setStatus(1);
            subscription.setChannelType(1); // WebHook
            subscription.setChannelAddress("https://webhook.example.com/callbacks");
            subscription.setAuthType(1); // SOA

            when(applicationService.getAppIdByAk(ak)).thenReturn(appId);
            when(subscriptionMapper.selectCallbackConfigByAppIdAndScope(appId, scope))
                    .thenReturn(subscription);

            // 执行测试
            CallbackConfigResponse response = apiGatewayService.getCallbackConfig(ak, scope);

            // 验证结果
            assertNotNull(response);
            assertEquals(ak, response.getAk());
            assertEquals(scope, response.getScope());
            assertEquals(1, response.getChannelType());
            assertEquals("https://webhook.example.com/callbacks", response.getChannelAddress());
            assertEquals(1, response.getAuthType());

            // 验证调用
            verify(applicationService, times(1)).getAppIdByAk(ak);
            verify(subscriptionMapper, times(1)).selectCallbackConfigByAppIdAndScope(appId, scope);
        }

        @Test
        @DisplayName("查询成功 - SSE通道")
        void testGetCallbackConfig_Success_SSE() {
            // 准备数据
            String ak = "AK987654321";
            String scope = "callback:message:received";
            Long appId = 20L;

            Subscription subscription = new Subscription();
            subscription.setId(303L);
            subscription.setAppId(appId);
            subscription.setPermissionId(203L);
            subscription.setStatus(1);
            subscription.setChannelType(2); // SSE
            subscription.setChannelAddress("/sse/callbacks");
            subscription.setAuthType(2); // APIG

            when(applicationService.getAppIdByAk(ak)).thenReturn(appId);
            when(subscriptionMapper.selectCallbackConfigByAppIdAndScope(appId, scope))
                    .thenReturn(subscription);

            // 执行测试
            CallbackConfigResponse response = apiGatewayService.getCallbackConfig(ak, scope);

            // 验证结果
            assertNotNull(response);
            assertEquals(ak, response.getAk());
            assertEquals(scope, response.getScope());
            assertEquals(2, response.getChannelType());
            assertEquals("/sse/callbacks", response.getChannelAddress());
            assertEquals(2, response.getAuthType());

            // 验证调用
            verify(applicationService, times(1)).getAppIdByAk(ak);
            verify(subscriptionMapper, times(1)).selectCallbackConfigByAppIdAndScope(appId, scope);
        }

        @Test
        @DisplayName("查询成功 - WebSocket通道")
        void testGetCallbackConfig_Success_WebSocket() {
            // 准备数据
            String ak = "AK111222333";
            String scope = "callback:notification:push";
            Long appId = 30L;

            Subscription subscription = new Subscription();
            subscription.setId(304L);
            subscription.setAppId(appId);
            subscription.setPermissionId(204L);
            subscription.setStatus(1);
            subscription.setChannelType(3); // WebSocket
            subscription.setChannelAddress("/ws/callbacks");
            subscription.setAuthType(1); // SOA

            when(applicationService.getAppIdByAk(ak)).thenReturn(appId);
            when(subscriptionMapper.selectCallbackConfigByAppIdAndScope(appId, scope))
                    .thenReturn(subscription);

            // 执行测试
            CallbackConfigResponse response = apiGatewayService.getCallbackConfig(ak, scope);

            // 验证结果
            assertNotNull(response);
            assertEquals(3, response.getChannelType());
            assertEquals("/ws/callbacks", response.getChannelAddress());

            // 验证调用
            verify(applicationService, times(1)).getAppIdByAk(ak);
            verify(subscriptionMapper, times(1)).selectCallbackConfigByAppIdAndScope(appId, scope);
        }

        @Test
        @DisplayName("无效的 AK")
        void testGetCallbackConfig_InvalidAk() {
            // 准备数据
            String ak = "INVALID_AK";
            String scope = "callback:approval:completed";

            when(applicationService.getAppIdByAk(ak)).thenReturn(null);

            // 执行测试
            CallbackConfigResponse response = apiGatewayService.getCallbackConfig(ak, scope);

            // 验证结果
            assertNull(response);

            // 验证调用
            verify(applicationService, times(1)).getAppIdByAk(ak);
            verify(subscriptionMapper, never()).selectCallbackConfigByAppIdAndScope(anyLong(), anyString());
        }

        @Test
        @DisplayName("未找到订阅配置")
        void testGetCallbackConfig_NotSubscribed() {
            // 准备数据
            String ak = "AK123456789";
            String scope = "callback:approval:completed";
            Long appId = 10L;

            when(applicationService.getAppIdByAk(ak)).thenReturn(appId);
            when(subscriptionMapper.selectCallbackConfigByAppIdAndScope(appId, scope))
                    .thenReturn(null);

            // 执行测试
            CallbackConfigResponse response = apiGatewayService.getCallbackConfig(ak, scope);

            // 验证结果
            assertNull(response);

            // 验证调用
            verify(applicationService, times(1)).getAppIdByAk(ak);
            verify(subscriptionMapper, times(1)).selectCallbackConfigByAppIdAndScope(appId, scope);
        }

        @Test
        @DisplayName("AK为空")
        void testGetCallbackConfig_EmptyAk() {
            // 准备数据
            String ak = "";
            String scope = "callback:approval:completed";

            when(applicationService.getAppIdByAk(ak)).thenReturn(null);

            // 执行测试
            CallbackConfigResponse response = apiGatewayService.getCallbackConfig(ak, scope);

            // 验证结果
            assertNull(response);

            // 验证调用
            verify(applicationService, times(1)).getAppIdByAk(ak);
            verify(subscriptionMapper, never()).selectCallbackConfigByAppIdAndScope(anyLong(), anyString());
        }

        @Test
        @DisplayName("AK为null")
        void testGetCallbackConfig_NullAk() {
            // 准备数据
            String ak = null;
            String scope = "callback:approval:completed";

            when(applicationService.getAppIdByAk(ak)).thenReturn(null);

            // 执行测试
            CallbackConfigResponse response = apiGatewayService.getCallbackConfig(ak, scope);

            // 验证结果
            assertNull(response);

            // 验证调用
            verify(applicationService, times(1)).getAppIdByAk(ak);
            verify(subscriptionMapper, never()).selectCallbackConfigByAppIdAndScope(anyLong(), anyString());
        }
    }
}
