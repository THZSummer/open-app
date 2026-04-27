package com.xxx.api.data.service;

import com.xxx.api.common.entity.Permission;
import com.xxx.api.common.entity.Subscription;
import com.xxx.api.common.mapper.PermissionMapper;
import com.xxx.api.common.mapper.SubscriptionMapper;
import com.xxx.api.gateway.dto.PermissionCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("数据查询服务测试")
class DataQueryServiceTest {

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private DataQueryService dataQueryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("权限校验测试")
    class CheckPermissionTests {

        @Test
        @DisplayName("授权成功")
        void testCheckPermission_Authorized() {
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

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong())).thenReturn(subscription);

            PermissionCheckResponse response = dataQueryService.checkPermission(appId, scope);

            assertNotNull(response);
            assertTrue(response.getAuthorized());
            assertEquals("300", response.getSubscriptionId());
            assertEquals(1, response.getSubscriptionStatus());

            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, times(1)).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("应用ID格式错误")
        void testCheckPermission_InvalidAppId() {
            String appId = "invalid-app-id";
            String scope = "api:im:send-message";

            PermissionCheckResponse response = dataQueryService.checkPermission(appId, scope);

            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("应用ID格式错误", response.getReason());

            verify(permissionMapper, never()).selectByScope(anyString());
            verify(subscriptionMapper, never()).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("权限不存在")
        void testCheckPermission_PermissionNotFound() {
            String appId = "10";
            String scope = "api:im:send-message";

            when(permissionMapper.selectByScope(anyString())).thenReturn(null);

            PermissionCheckResponse response = dataQueryService.checkPermission(appId, scope);

            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("权限不存在", response.getReason());

            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, never()).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("应用未订阅该权限")
        void testCheckPermission_NotSubscribed() {
            String appId = "10";
            String scope = "api:im:send-message";

            Permission permission = new Permission();
            permission.setId(200L);
            permission.setScope(scope);
            permission.setStatus(1);

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong())).thenReturn(null);

            PermissionCheckResponse response = dataQueryService.checkPermission(appId, scope);

            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("应用未订阅该权限", response.getReason());

            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, times(1)).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("订阅状态异常 - 待审批")
        void testCheckPermission_PendingApproval() {
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
            subscription.setStatus(0);

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong())).thenReturn(subscription);

            PermissionCheckResponse response = dataQueryService.checkPermission(appId, scope);

            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("订阅状态异常", response.getReason());
            assertEquals("300", response.getSubscriptionId());
            assertEquals(0, response.getSubscriptionStatus());

            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, times(1)).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("订阅状态异常 - 已拒绝")
        void testCheckPermission_Rejected() {
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
            subscription.setStatus(2);

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong())).thenReturn(subscription);

            PermissionCheckResponse response = dataQueryService.checkPermission(appId, scope);

            assertNotNull(response);
            assertFalse(response.getAuthorized());
            assertEquals("订阅状态异常", response.getReason());
            assertEquals("300", response.getSubscriptionId());
            assertEquals(2, response.getSubscriptionStatus());

            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, times(1)).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("查询订阅应用列表测试")
    class GetSubscribedAppsTests {

        @Test
        @DisplayName("查询成功 - 返回多个应用")
        void testGetSubscribedApps_Success() {
            String scope = "api:im:send-message";

            Permission permission = new Permission();
            permission.setId(200L);
            permission.setScope(scope);

            List<Subscription> subscriptions = new ArrayList<>();
            Subscription sub1 = new Subscription();
            sub1.setId(301L);
            sub1.setAppId(10L);
            sub1.setPermissionId(200L);
            sub1.setStatus(1);

            Subscription sub2 = new Subscription();
            sub2.setId(302L);
            sub2.setAppId(20L);
            sub2.setPermissionId(200L);
            sub2.setStatus(1);

            Subscription sub3 = new Subscription();
            sub3.setId(303L);
            sub3.setAppId(30L);
            sub3.setPermissionId(200L);
            sub3.setStatus(1);

            subscriptions.add(sub1);
            subscriptions.add(sub2);
            subscriptions.add(sub3);

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);
            when(subscriptionMapper.selectAuthorizedByPermissionId(anyLong())).thenReturn(subscriptions);

            List<String> result = dataQueryService.getSubscribedApps(scope);

            assertNotNull(result);
            assertEquals(3, result.size());
            assertTrue(result.contains("10"));
            assertTrue(result.contains("20"));
            assertTrue(result.contains("30"));

            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, times(1)).selectAuthorizedByPermissionId(anyLong());
        }

        @Test
        @DisplayName("权限不存在 - 返回空列表")
        void testGetSubscribedApps_PermissionNotFound() {
            String scope = "api:im:send-message";

            when(permissionMapper.selectByScope(anyString())).thenReturn(null);

            List<String> result = dataQueryService.getSubscribedApps(scope);

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, never()).selectAuthorizedByPermissionId(anyLong());
        }

        @Test
        @DisplayName("无订阅应用 - 返回空列表")
        void testGetSubscribedApps_NoSubscriptions() {
            String scope = "api:im:send-message";

            Permission permission = new Permission();
            permission.setId(200L);
            permission.setScope(scope);

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);
            when(subscriptionMapper.selectAuthorizedByPermissionId(anyLong())).thenReturn(new ArrayList<>());

            List<String> result = dataQueryService.getSubscribedApps(scope);

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, times(1)).selectAuthorizedByPermissionId(anyLong());
        }
    }

    @Nested
    @DisplayName("查询订阅配置测试")
    class GetSubscriptionConfigTests {

        @Test
        @DisplayName("查询成功")
        void testGetSubscriptionConfig_Success() {
            String appId = "10";
            String scope = "api:im:send-message";

            Permission permission = new Permission();
            permission.setId(200L);
            permission.setScope(scope);

            Subscription subscription = new Subscription();
            subscription.setId(300L);
            subscription.setAppId(10L);
            subscription.setPermissionId(200L);
            subscription.setStatus(1);
            subscription.setChannelType(1);
            subscription.setChannelAddress("https://webhook.example.com/callbacks");
            subscription.setAuthType(1);

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong())).thenReturn(subscription);

            Map<String, Object> result = dataQueryService.getSubscriptionConfig(appId, scope);

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals("300", result.get("id"));
            assertEquals("10", result.get("appId"));
            assertEquals("200", result.get("permissionId"));
            assertEquals(1, result.get("status"));
            assertEquals(1, result.get("channelType"));
            assertEquals("https://webhook.example.com/callbacks", result.get("channelAddress"));
            assertEquals(1, result.get("authType"));

            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, times(1)).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("应用ID格式错误")
        void testGetSubscriptionConfig_InvalidAppId() {
            String appId = "invalid-app-id";
            String scope = "api:im:send-message";

            Map<String, Object> result = dataQueryService.getSubscriptionConfig(appId, scope);

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(permissionMapper, never()).selectByScope(anyString());
            verify(subscriptionMapper, never()).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("权限不存在")
        void testGetSubscriptionConfig_PermissionNotFound() {
            String appId = "10";
            String scope = "api:im:send-message";

            when(permissionMapper.selectByScope(anyString())).thenReturn(null);

            Map<String, Object> result = dataQueryService.getSubscriptionConfig(appId, scope);

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, never()).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("未找到订阅")
        void testGetSubscriptionConfig_SubscriptionNotFound() {
            String appId = "10";
            String scope = "api:im:send-message";

            Permission permission = new Permission();
            permission.setId(200L);
            permission.setScope(scope);

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong())).thenReturn(null);

            Map<String, Object> result = dataQueryService.getSubscriptionConfig(appId, scope);

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(permissionMapper, times(1)).selectByScope(anyString());
            verify(subscriptionMapper, times(1)).selectByAppIdAndPermissionId(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("查询权限详情测试")
    class GetPermissionByScopeTests {

        @Test
        @DisplayName("查询成功")
        void testGetPermissionByScope_Success() {
            String scope = "api:im:send-message";

            Permission permission = new Permission();
            permission.setId(200L);
            permission.setNameCn("发送消息");
            permission.setNameEn("Send Message");
            permission.setScope(scope);
            permission.setResourceType("1");
            permission.setResourceId(100L);
            permission.setStatus(1);

            when(permissionMapper.selectByScope(anyString())).thenReturn(permission);

            Map<String, Object> result = dataQueryService.getPermissionByScope(scope);

            assertNotNull(result);
            assertEquals("200", result.get("id"));
            assertEquals("发送消息", result.get("nameCn"));
            assertEquals("Send Message", result.get("nameEn"));
            assertEquals(scope, result.get("scope"));
            assertEquals("1", result.get("resourceType"));
            assertEquals("100", result.get("resourceId"));
            assertEquals(1, result.get("status"));

            verify(permissionMapper, times(1)).selectByScope(anyString());
        }

        @Test
        @DisplayName("权限不存在 - 返回null")
        void testGetPermissionByScope_PermissionNotFound() {
            String scope = "api:im:send-message";

            when(permissionMapper.selectByScope(anyString())).thenReturn(null);

            Map<String, Object> result = dataQueryService.getPermissionByScope(scope);

            assertNull(result);

            verify(permissionMapper, times(1)).selectByScope(anyString());
        }
    }
}
