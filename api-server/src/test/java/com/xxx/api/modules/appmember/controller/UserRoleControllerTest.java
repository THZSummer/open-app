package com.xxx.api.modules.appmember.controller;

import com.xxx.api.common.exception.BusinessException;
import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.modules.appmember.dto.UserRoleQueryRequest;
import com.xxx.api.modules.appmember.dto.UserRoleQueryResponse;
import com.xxx.api.modules.appmember.service.UserRoleService;
import jakarta.servlet.http.HttpServletRequest;
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

@DisplayName("用户角色查询控制器测试")
class UserRoleControllerTest {

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private UserRoleController userRoleController;

    private static final String TOKEN = "dev-token-001";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(httpRequest.getHeader("X-Internal-Token")).thenReturn(TOKEN);
    }

    @Nested
    @DisplayName("正常流程")
    class SuccessTests {

        @Test
        @DisplayName("查询成功 → 200")
        void testSuccess() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app");
            request.setUserAccount("admin");

            UserRoleQueryResponse mockResp = UserRoleQueryResponse.builder()
                    .appId("test-app")
                    .roles(new Integer[]{1})
                    .build();
            when(userRoleService.queryUserRoles(request, TOKEN)).thenReturn(mockResp);

            ApiResponse<UserRoleQueryResponse> response = userRoleController.queryUserRoles(request, httpRequest);

            assertEquals("200", response.getCode());
            assertEquals("test-app", response.getData().getAppId());
            assertArrayEquals(new Integer[]{1}, response.getData().getRoles());
        }
    }

    @Nested
    @DisplayName("异常映射")
    class ErrorMappingTests {

        @Test
        @DisplayName("Service 抛 401 → ApiResponse 401")
        void testUnauthorized() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app");
            request.setUserAccount("admin");

            when(userRoleService.queryUserRoles(request, TOKEN))
                    .thenThrow(BusinessException.unauthorized("凭证缺失", "Missing token"));

            ApiResponse<UserRoleQueryResponse> response = userRoleController.queryUserRoles(request, httpRequest);

            assertEquals("401", response.getCode());
            verify(userRoleService).queryUserRoles(request, TOKEN);
        }

        @Test
        @DisplayName("Service 抛 400 → ApiResponse 400")
        void testBadRequest() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setUserAccount("admin");

            when(userRoleService.queryUserRoles(request, TOKEN))
                    .thenThrow(BusinessException.badRequest("参数缺失", "Missing param"));

            ApiResponse<UserRoleQueryResponse> response = userRoleController.queryUserRoles(request, httpRequest);

            assertEquals("400", response.getCode());
        }

        @Test
        @DisplayName("Service 抛 404 → ApiResponse 404")
        void testNotFound() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("nonexistent");
            request.setUserAccount("admin");

            when(userRoleService.queryUserRoles(request, TOKEN))
                    .thenThrow(BusinessException.notFound("应用不存在", "Not found"));

            ApiResponse<UserRoleQueryResponse> response = userRoleController.queryUserRoles(request, httpRequest);

            assertEquals("404", response.getCode());
        }

        @Test
        @DisplayName("Service 抛 403 → ApiResponse 403")
        void testForbidden() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app");
            request.setUserAccount("admin");

            when(userRoleService.queryUserRoles(request, TOKEN))
                    .thenThrow(BusinessException.forbidden("无权限", "Forbidden"));

            ApiResponse<UserRoleQueryResponse> response = userRoleController.queryUserRoles(request, httpRequest);

            assertEquals("403", response.getCode());
        }
    }
}
