package com.xxx.api.internal.controller;

import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.internal.dto.UserRoleQueryRequest;
import com.xxx.api.internal.dto.UserRoleQueryResponse;
import com.xxx.api.internal.resolver.AppIdentifierResolver;
import com.xxx.api.internal.service.UserRoleService;
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
 * 用户角色查询控制器测试
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@DisplayName("用户角色查询控制器测试")
class UserRoleControllerTest {

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private AppIdentifierResolver appIdentifierResolver;

    @InjectMocks
    private UserRoleController userRoleController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("queryUserRoles 测试")
    class QueryUserRolesTests {

        @Test
        @DisplayName("按 appId 查询成功 - 返回角色列表")
        void testQueryUserRoles_ByAppId_Success() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("1234567890123456789");
            request.setUserAccount("zhangsan@xxx.com");

            when(appIdentifierResolver.resolve("1234567890123456789", null))
                    .thenReturn("1234567890123456789");

            UserRoleQueryResponse mockResponse = UserRoleQueryResponse.builder()
                    .appId("1234567890123456789")
                    .roles(new Integer[]{1, 2})
                    .build();

            when(userRoleService.queryUserRoles(any(), anyString())).thenReturn(mockResponse);

            ApiResponse<UserRoleQueryResponse> response = userRoleController.queryUserRoles(request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("1234567890123456789", response.getData().getAppId());
            assertArrayEquals(new Integer[]{1, 2}, response.getData().getRoles());

            verify(appIdentifierResolver).resolve("1234567890123456789", null);
            verify(userRoleService).queryUserRoles(request, "1234567890123456789");
        }

        @Test
        @DisplayName("按 hisAppId 查询成功 - 返回角色列表")
        void testQueryUserRoles_ByHisAppId_Success() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setHisAppId("EAMAP_APP_001");
            request.setUserAccount("lisi@xxx.com");

            when(appIdentifierResolver.resolve(null, "EAMAP_APP_001"))
                    .thenReturn("9876543210987654321");

            UserRoleQueryResponse mockResponse = UserRoleQueryResponse.builder()
                    .appId("9876543210987654321")
                    .roles(new Integer[]{0})
                    .build();

            when(userRoleService.queryUserRoles(any(), anyString())).thenReturn(mockResponse);

            ApiResponse<UserRoleQueryResponse> response = userRoleController.queryUserRoles(request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("9876543210987654321", response.getData().getAppId());
            assertArrayEquals(new Integer[]{0}, response.getData().getRoles());

            verify(appIdentifierResolver).resolve(null, "EAMAP_APP_001");
            verify(userRoleService).queryUserRoles(request, "9876543210987654321");
        }

        @Test
        @DisplayName("appId 和 hisAppId 均为空 - 返回400")
        void testQueryUserRoles_MissingAppIdentifier_Returns400() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId(null);
            request.setHisAppId(null);
            request.setUserAccount("test@xxx.com");

            ApiResponse<UserRoleQueryResponse> response = userRoleController.queryUserRoles(request);

            assertNotNull(response);
            assertEquals("400", response.getCode());
            assertNull(response.getData());

            verify(appIdentifierResolver, never()).resolve(any(), any());
            verify(userRoleService, never()).queryUserRoles(any(), any());
        }

        @Test
        @DisplayName("userAccount 为空 - 返回400")
        void testQueryUserRoles_MissingUserAccount_Returns400() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("1234567890123456789");
            request.setUserAccount(null);

            ApiResponse<UserRoleQueryResponse> response = userRoleController.queryUserRoles(request);

            assertNotNull(response);
            assertEquals("400", response.getCode());
            assertNull(response.getData());

            verify(appIdentifierResolver, never()).resolve(any(), any());
            verify(userRoleService, never()).queryUserRoles(any(), any());
        }

        @Test
        @DisplayName("应用不存在 - 返回404")
        void testQueryUserRoles_AppNotFound_Returns404() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("nonexistent-app-id");
            request.setUserAccount("test@xxx.com");

            when(appIdentifierResolver.resolve("nonexistent-app-id", null))
                    .thenThrow(com.xxx.api.common.exception.BusinessException.notFound("应用不存在", "Application not found"));

            ApiResponse<UserRoleQueryResponse> response = userRoleController.queryUserRoles(request);

            assertNotNull(response);
            assertEquals("404", response.getCode());
            assertNull(response.getData());

            verify(appIdentifierResolver).resolve("nonexistent-app-id", null);
            verify(userRoleService, never()).queryUserRoles(any(), any());
        }

        @Test
        @DisplayName("用户无角色 - 返回空角色列表")
        void testQueryUserRoles_NoRoles_ReturnsEmptyList() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("1234567890123456789");
            request.setUserAccount("unknown@xxx.com");

            when(appIdentifierResolver.resolve("1234567890123456789", null))
                    .thenReturn("1234567890123456789");

            UserRoleQueryResponse mockResponse = UserRoleQueryResponse.builder()
                    .appId("1234567890123456789")
                    .roles(new Integer[0])
                    .build();

            when(userRoleService.queryUserRoles(any(), anyString())).thenReturn(mockResponse);

            ApiResponse<UserRoleQueryResponse> response = userRoleController.queryUserRoles(request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(0, response.getData().getRoles().length);

            verify(appIdentifierResolver).resolve("1234567890123456789", null);
            verify(userRoleService).queryUserRoles(request, "1234567890123456789");
        }
    }
}
