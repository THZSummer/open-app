package com.xxx.it.works.wecode.v2.modules.api.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.api.dto.*;
import com.xxx.it.works.wecode.v2.modules.api.service.ApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("API 管理控制器测试")
class ApiControllerTest {

    @Mock
    private ApiService apiService;

    @InjectMocks
    private ApiController apiController;

    private ApiDetailResponse createApiDetailResponse(String id, String nameCn, String path) {
        return ApiDetailResponse.builder()
                .id(id)
                .nameCn(nameCn)
                .nameEn("Test API")
                .path(path)
                .method("GET")
                .authType(1)
                .categoryId("1")
                .categoryName("测试分类")
                .status(2)
                .build();
    }

    private ApiListResponse createApiListResponse(String id, String nameCn) {
        return ApiListResponse.builder()
                .id(id)
                .nameCn(nameCn)
                .nameEn("Test API")
                .path("/api/v1/test")
                .method("GET")
                .status(2)
                .build();
    }

    @Nested
    @DisplayName("getApiList 测试")
    class GetApiListTest {

        @Test
        @DisplayName("获取列表成功")
        void testGetApiList_Success() {
            List<ApiListResponse> mockList = Arrays.asList(
                    createApiListResponse("1", "API 1"),
                    createApiListResponse("2", "API 2")
            );

            ApiResponse<List<ApiListResponse>> mockResponse = ApiResponse.<List<ApiListResponse>>builder()
                    .code("200")
                    .messageZh("操作成功")
                    .messageEn("Success")
                    .data(mockList)
                    .page(ApiResponse.PageResponse.builder()
                            .curPage(1)
                            .pageSize(20)
                            .total(2L)
                            .totalPages(1)
                            .build())
                    .build();

            when(apiService.getApiList(any(ApiListRequest.class))).thenReturn(mockResponse);

            ApiResponse<List<ApiListResponse>> response = apiController.getApiList(
                    null, null, null, 1, 20
            );

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertEquals(2, response.getData().size());
            assertEquals("API 1", response.getData().get(0).getNameCn());
            assertNotNull(response.getPage());
            assertEquals(1, response.getPage().getCurPage());
            assertEquals(20, response.getPage().getPageSize());
            assertEquals(2L, response.getPage().getTotal());

            verify(apiService).getApiList(argThat(req ->
                    req.getCurPage() == 1 && req.getPageSize() == 20
            ));
        }

        @Test
        @DisplayName("带过滤条件查询")
        void testGetApiList_WithFilters() {
            List<ApiListResponse> mockList = Collections.singletonList(
                    createApiListResponse("1", "Filtered API")
            );

            ApiResponse<List<ApiListResponse>> mockResponse = ApiResponse.<List<ApiListResponse>>builder()
                    .code("200")
                    .data(mockList)
                    .build();

            when(apiService.getApiList(any(ApiListRequest.class))).thenReturn(mockResponse);

            ApiResponse<List<ApiListResponse>> response = apiController.getApiList(
                    "cat-123", 2, "search", 1, 10
            );

            assertNotNull(response);
            assertEquals(1, response.getData().size());

            verify(apiService).getApiList(argThat(req ->
                    "cat-123".equals(req.getCategoryId()) &&
                    Integer.valueOf(2).equals(req.getStatus()) &&
                    "search".equals(req.getKeyword()) &&
                    req.getCurPage() == 1 &&
                    req.getPageSize() == 10
            ));
        }
    }

    @Nested
    @DisplayName("getApiDetail 测试")
    class GetApiDetailTest {

        @Test
        @DisplayName("获取详情成功")
        void testGetApiDetail_Success() {
            ApiDetailResponse mockDetail = createApiDetailResponse("123", "测试API", "/api/v1/test");

            when(apiService.getApiDetail("123")).thenReturn(mockDetail);

            ApiResponse<ApiDetailResponse> response = apiController.getApiDetail("123");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("123", response.getData().getId());
            assertEquals("测试API", response.getData().getNameCn());
            assertEquals("/api/v1/test", response.getData().getPath());

            verify(apiService).getApiDetail("123");
        }
    }

    @Nested
    @DisplayName("createApi 测试")
    class CreateApiTest {

        @Test
        @DisplayName("注册成功")
        void testCreateApi_Success() {
            ApiCreateRequest request = new ApiCreateRequest();
            request.setNameCn("新API");
            request.setNameEn("New API");
            request.setPath("/api/v1/new");
            request.setMethod("POST");
            request.setAuthType(1);
            request.setCategoryId("1");

            PermissionCreateRequest permissionRequest = new PermissionCreateRequest();
            permissionRequest.setNameCn("新权限");
            permissionRequest.setNameEn("New Permission");
            request.setPermission(permissionRequest);

            ApiDetailResponse mockResponse = createApiDetailResponse("999", "新API", "/api/v1/new");

            when(apiService.createApi(any(ApiCreateRequest.class))).thenReturn(mockResponse);

            ApiResponse<ApiDetailResponse> response = apiController.createApi(request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("999", response.getData().getId());
            assertEquals("新API", response.getData().getNameCn());

            verify(apiService).createApi(argThat(req ->
                    "新API".equals(req.getNameCn()) &&
                    "New API".equals(req.getNameEn()) &&
                    "/api/v1/new".equals(req.getPath())
            ));
        }
    }

    @Nested
    @DisplayName("updateApi 测试")
    class UpdateApiTest {

        @Test
        @DisplayName("更新成功")
        void testUpdateApi_Success() {
            ApiUpdateRequest request = new ApiUpdateRequest();
            request.setNameCn("更新后的API");
            request.setNameEn("Updated API");

            ApiDetailResponse mockResponse = createApiDetailResponse("123", "更新后的API", "/api/v1/test");

            when(apiService.updateApi(eq("123"), any(ApiUpdateRequest.class))).thenReturn(mockResponse);

            ApiResponse<ApiDetailResponse> response = apiController.updateApi("123", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("123", response.getData().getId());
            assertEquals("更新后的API", response.getData().getNameCn());

            verify(apiService).updateApi(eq("123"), argThat(req ->
                    "更新后的API".equals(req.getNameCn())
            ));
        }
    }

    @Nested
    @DisplayName("deleteApi 测试")
    class DeleteApiTest {

        @Test
        @DisplayName("删除成功")
        void testDeleteApi_Success() {
            doNothing().when(apiService).deleteApi("123");

            ApiResponse<Void> response = apiController.deleteApi("123");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNull(response.getData());

            verify(apiService).deleteApi("123");
        }
    }

    @Nested
    @DisplayName("withdrawApi 测试")
    class WithdrawApiTest {

        @Test
        @DisplayName("撤回成功")
        void testWithdrawApi_Success() {
            ApiDetailResponse mockResponse = createApiDetailResponse("123", "测试API", "/api/v1/test");
            mockResponse.setStatus(0);

            when(apiService.withdrawApi("123")).thenReturn(mockResponse);

            ApiResponse<ApiDetailResponse> response = apiController.withdrawApi("123");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("123", response.getData().getId());

            verify(apiService).withdrawApi("123");
        }
    }
}