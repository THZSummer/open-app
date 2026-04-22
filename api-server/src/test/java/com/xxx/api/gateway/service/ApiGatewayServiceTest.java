package com.xxx.api.gateway.service;

import com.xxx.api.common.entity.Permission;
import com.xxx.api.common.entity.Subscription;
import com.xxx.api.common.mapper.PermissionMapper;
import com.xxx.api.common.mapper.SubscriptionMapper;
import com.xxx.api.gateway.dto.PermissionCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @InjectMocks
    private ApiGatewayService apiGatewayService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("权限校验 - 授权成功")
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
    @DisplayName("权限校验 - 权限不存在")
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
    @DisplayName("权限校验 - 未订阅")
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
    @DisplayName("应用身份验证 - 成功")
    void testVerifyApplication_Success() {
        // 准备数据
        String appId = "10";
        Integer authType = 0;
        String authCredential = "Bearer token123";

        // 执行测试
        boolean result = apiGatewayService.verifyApplication(appId, authType, authCredential);

        // 验证结果
        assertTrue(result);
    }

    @Test
    @DisplayName("应用身份验证 - 失败（缺少参数）")
    void testVerifyApplication_Failed_MissingParams() {
        // 执行测试
        boolean result = apiGatewayService.verifyApplication(null, 0, "token");

        // 验证结果
        assertFalse(result);
    }
}
