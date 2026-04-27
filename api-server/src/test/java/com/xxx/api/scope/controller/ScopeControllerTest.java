package com.xxx.api.scope.controller;

import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.scope.dto.UserAuthorizationCreateRequest;
import com.xxx.api.scope.dto.UserAuthorizationListResponse;
import com.xxx.api.scope.dto.UserAuthorizationResponse;
import com.xxx.api.scope.service.ScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Scope 授权控制器测试")
class ScopeControllerTest {

    @Mock
    private ScopeService scopeService;

    @InjectMocks
    private ScopeController scopeController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("getUserAuthorizations 测试")
    class GetUserAuthorizationsTests {

        @Test
        @DisplayName("获取列表成功")
        void testGetUserAuthorizations_Success() {
            UserAuthorizationListResponse response1 = new UserAuthorizationListResponse();
            response1.setId("1001");
            response1.setUserId("user001");
            response1.setUserName("张三");
            response1.setAppId("2001");
            response1.setAppName("测试应用");
            response1.setScopes(Arrays.asList("api:v1:messages:get", "api:v1:messages:post"));
            response1.setExpiresAt(new Date());
            response1.setCreateTime(new Date());

            List<UserAuthorizationListResponse> mockList = Collections.singletonList(response1);
            ApiResponse.PageResponse pageResponse = ApiResponse.PageResponse.builder()
                    .curPage(1)
                    .pageSize(20)
                    .total(1L)
                    .totalPages(1)
                    .build();

            ApiResponse<List<UserAuthorizationListResponse>> mockResponse = ApiResponse.success(mockList, pageResponse);

            when(scopeService.getUserAuthorizations(any())).thenReturn(mockResponse);

            ApiResponse<List<UserAuthorizationListResponse>> response = scopeController.getUserAuthorizations(
                    "user001", null, null, 1, 20);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertEquals("user001", response.getData().get(0).getUserId());
            assertNotNull(response.getPage());
            assertEquals(1, response.getPage().getTotal());
            verify(scopeService).getUserAuthorizations(any());
        }
    }

    @Nested
    @DisplayName("createUserAuthorization 测试")
    class CreateUserAuthorizationTests {

        @Test
        @DisplayName("创建授权成功")
        void testCreateUserAuthorization_Success() {
            UserAuthorizationCreateRequest request = new UserAuthorizationCreateRequest();
            request.setUserId("user001");
            request.setAppId("2001");
            request.setScopes(Arrays.asList("api:v1:messages:get", "api:v1:messages:post"));
            request.setExpiresAt(new Date());

            UserAuthorizationResponse mockResponse = new UserAuthorizationResponse();
            mockResponse.setId("1001");
            mockResponse.setUserId("user001");
            mockResponse.setAppId("2001");
            mockResponse.setScopes(Arrays.asList("api:v1:messages:get", "api:v1:messages:post"));
            mockResponse.setExpiresAt(request.getExpiresAt());

            ApiResponse<UserAuthorizationResponse> apiResponse = ApiResponse.success(mockResponse);

            when(scopeService.createUserAuthorization(any())).thenReturn(apiResponse);

            ApiResponse<UserAuthorizationResponse> response = scopeController.createUserAuthorization(request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("user001", response.getData().getUserId());
            assertEquals("2001", response.getData().getAppId());
            assertEquals(2, response.getData().getScopes().size());
            verify(scopeService).createUserAuthorization(any());
        }
    }

    @Nested
    @DisplayName("revokeUserAuthorization 测试")
    class RevokeUserAuthorizationTests {

        @Test
        @DisplayName("取消授权成功")
        void testRevokeUserAuthorization_Success() {
            String authorizationId = "1001";

            ApiResponse<Void> mockResponse = ApiResponse.success();

            when(scopeService.revokeUserAuthorization(authorizationId)).thenReturn(mockResponse);

            ApiResponse<Void> response = scopeController.revokeUserAuthorization(authorizationId);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNull(response.getData());
            verify(scopeService).revokeUserAuthorization(authorizationId);
        }
    }
}
