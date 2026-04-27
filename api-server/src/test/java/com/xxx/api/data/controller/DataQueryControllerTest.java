package com.xxx.api.data.controller;

import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.data.service.DataQueryService;
import com.xxx.api.gateway.dto.PermissionCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("数据查询控制器测试")
class DataQueryControllerTest {

    @Mock
    private DataQueryService dataQueryService;

    @InjectMocks
    private DataQueryController dataQueryController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("checkPermission 测试")
    class CheckPermissionTests {

        @Test
        @DisplayName("权限校验成功")
        void testCheckPermission_Success() {
            String appId = "1001";
            String scope = "api:v1:messages:get";

            PermissionCheckResponse mockResponse = PermissionCheckResponse.builder()
                    .authorized(true)
                    .subscriptionId("sub001")
                    .subscriptionStatus(1)
                    .build();

            when(dataQueryService.checkPermission(appId, scope)).thenReturn(mockResponse);

            ApiResponse<PermissionCheckResponse> response = dataQueryController.checkPermission(appId, scope);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertTrue(response.getData().getAuthorized());
            assertEquals("sub001", response.getData().getSubscriptionId());
            assertEquals(1, response.getData().getSubscriptionStatus());
            verify(dataQueryService).checkPermission(appId, scope);
        }
    }

    @Nested
    @DisplayName("getSubscribedApps 测试")
    class GetSubscribedAppsTests {

        @Test
        @DisplayName("查询成功")
        void testGetSubscribedApps_Success() {
            String scope = "api:v1:messages:get";

            List<String> mockAppIds = List.of("1001", "1002", "1003");

            when(dataQueryService.getSubscribedApps(scope)).thenReturn(mockAppIds);

            ApiResponse<List<String>> response = dataQueryController.getSubscribedApps(scope);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(3, response.getData().size());
            assertTrue(response.getData().contains("1001"));
            assertTrue(response.getData().contains("1002"));
            assertTrue(response.getData().contains("1003"));
            verify(dataQueryService).getSubscribedApps(scope);
        }
    }

    @Nested
    @DisplayName("getSubscriptionConfig 测试")
    class GetSubscriptionConfigTests {

        @Test
        @DisplayName("查询成功")
        void testGetSubscriptionConfig_Success() {
            String appId = "1001";
            String scope = "api:v1:messages:get";

            Map<String, Object> mockConfig = Map.of(
                    "id", "sub001",
                    "appId", "1001",
                    "permissionId", "perm001",
                    "status", 1,
                    "channelType", 1,
                    "channelAddress", "https://example.com/webhook",
                    "authType", 1
            );

            when(dataQueryService.getSubscriptionConfig(appId, scope)).thenReturn(mockConfig);

            ApiResponse<Map<String, Object>> response = dataQueryController.getSubscriptionConfig(appId, scope);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("sub001", response.getData().get("id"));
            assertEquals("1001", response.getData().get("appId"));
            assertEquals(1, response.getData().get("status"));
            verify(dataQueryService).getSubscriptionConfig(appId, scope);
        }
    }

    @Nested
    @DisplayName("getPermissionByScope 测试")
    class GetPermissionByScopeTests {

        @Test
        @DisplayName("查询成功")
        void testGetPermissionByScope_Success() {
            String scope = "api:v1:messages:get";

            Map<String, Object> mockPermission = Map.of(
                    "id", "perm001",
                    "nameCn", "消息获取权限",
                    "nameEn", "Messages Get Permission",
                    "scope", "api:v1:messages:get",
                    "resourceType", "api",
                    "resourceId", "res001",
                    "status", 1
            );

            when(dataQueryService.getPermissionByScope(scope)).thenReturn(mockPermission);

            ApiResponse<Map<String, Object>> response = dataQueryController.getPermissionByScope(scope);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("perm001", response.getData().get("id"));
            assertEquals("消息获取权限", response.getData().get("nameCn"));
            assertEquals("api:v1:messages:get", response.getData().get("scope"));
            verify(dataQueryService).getPermissionByScope(scope);
        }

        @Test
        @DisplayName("权限不存在")
        void testGetPermissionByScope_NotFound() {
            String scope = "api:v1:not-exist:get";

            when(dataQueryService.getPermissionByScope(scope)).thenReturn(null);

            ApiResponse<Map<String, Object>> response = dataQueryController.getPermissionByScope(scope);

            assertNotNull(response);
            assertEquals("404", response.getCode());
            assertNull(response.getData());
            assertEquals("权限不存在", response.getMessageZh());
            verify(dataQueryService).getPermissionByScope(scope);
        }
    }
}
