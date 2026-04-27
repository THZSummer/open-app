package com.xxx.it.works.wecode.v2.modules.category.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.category.dto.*;
import com.xxx.it.works.wecode.v2.modules.category.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("分类管理控制器测试")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private CategoryTreeResponse testTreeResponse;
    private CategoryResponse testCategoryResponse;
    private CategoryOwnerResponse testOwnerResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testCategoryResponse = new CategoryResponse();
        testCategoryResponse.setId("1");
        testCategoryResponse.setCategoryAlias("app_type_a");
        testCategoryResponse.setNameCn("A类应用权限");
        testCategoryResponse.setNameEn("App Type A Permissions");
        testCategoryResponse.setParentId(null);
        testCategoryResponse.setPath("/1/");
        testCategoryResponse.setCategoryPath(Arrays.asList("A类应用权限"));
        testCategoryResponse.setSortOrder(0);
        testCategoryResponse.setStatus(1);
        testCategoryResponse.setCreateTime(new Date());
        testCategoryResponse.setCreateBy("system");

        testTreeResponse = new CategoryTreeResponse();
        testTreeResponse.setId("1");
        testTreeResponse.setCategoryAlias("app_type_a");
        testTreeResponse.setNameCn("A类应用权限");
        testTreeResponse.setNameEn("App Type A Permissions");
        testTreeResponse.setPath("/1/");
        testTreeResponse.setSortOrder(0);
        testTreeResponse.setStatus(1);
        testTreeResponse.setChildren(new ArrayList<>());

        testOwnerResponse = new CategoryOwnerResponse();
        testOwnerResponse.setId("100");
        testOwnerResponse.setCategoryId("1");
        testOwnerResponse.setUserId("user001");
        testOwnerResponse.setUserName("张三");
        testOwnerResponse.setCreateTime(new Date());
    }

    @Nested
    @DisplayName("分类管理测试")
    class CategoryManagementTests {

        @Test
        @DisplayName("获取分类树成功")
        void testGetCategoryTree_Success() {
            List<CategoryTreeResponse> mockTree = new ArrayList<>();
            testTreeResponse.setChildren(new ArrayList<>());
            mockTree.add(testTreeResponse);

            when(categoryService.getCategoryTree(any())).thenReturn(mockTree);

            ApiResponse<List<CategoryTreeResponse>> response = categoryController.getCategoryTree(null);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertEquals("1", response.getData().get(0).getId());
            assertEquals("A类应用权限", response.getData().get(0).getNameCn());
            verify(categoryService).getCategoryTree(null);
        }

        @Test
        @DisplayName("获取分类详情成功")
        void testGetCategoryById_Success() {
            when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse);

            ApiResponse<CategoryResponse> response = categoryController.getCategoryById("1");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("1", response.getData().getId());
            assertEquals("A类应用权限", response.getData().getNameCn());
            assertEquals("/1/", response.getData().getPath());
            verify(categoryService).getCategoryById(1L);
        }

        @Test
        @DisplayName("创建分类成功")
        void testCreateCategory_Success() {
            CategoryCreateRequest request = new CategoryCreateRequest();
            request.setCategoryAlias("app_type_a");
            request.setNameCn("A类应用权限");
            request.setNameEn("App Type A Permissions");
            request.setParentId(null);
            request.setSortOrder(0);

            when(categoryService.createCategory(any(CategoryCreateRequest.class))).thenReturn(testCategoryResponse);

            ApiResponse<CategoryResponse> response = categoryController.createCategory(request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("1", response.getData().getId());
            assertEquals("A类应用权限", response.getData().getNameCn());
            assertEquals("/1/", response.getData().getPath());
            verify(categoryService).createCategory(any(CategoryCreateRequest.class));
        }

        @Test
        @DisplayName("更新分类成功")
        void testUpdateCategory_Success() {
            CategoryUpdateRequest request = new CategoryUpdateRequest();
            request.setNameCn("更新后的名称");
            request.setNameEn("Updated Name");
            request.setSortOrder(1);

            CategoryResponse updatedResponse = new CategoryResponse();
            updatedResponse.setId("1");
            updatedResponse.setNameCn("更新后的名称");
            updatedResponse.setNameEn("Updated Name");
            updatedResponse.setPath("/1/");
            updatedResponse.setSortOrder(1);

            when(categoryService.updateCategory(eq(1L), any(CategoryUpdateRequest.class))).thenReturn(updatedResponse);

            ApiResponse<CategoryResponse> response = categoryController.updateCategory("1", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("1", response.getData().getId());
            assertEquals("更新后的名称", response.getData().getNameCn());
            verify(categoryService).updateCategory(eq(1L), any(CategoryUpdateRequest.class));
        }

        @Test
        @DisplayName("删除分类成功")
        void testDeleteCategory_Success() {
            doNothing().when(categoryService).deleteCategory(1L);

            ApiResponse<Void> response = categoryController.deleteCategory("1");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNull(response.getData());
            verify(categoryService).deleteCategory(1L);
        }
    }

    @Nested
    @DisplayName("责任人管理测试")
    class OwnerManagementTests {

        @Test
        @DisplayName("添加责任人成功")
        void testAddOwner_Success() {
            CategoryOwnerRequest request = new CategoryOwnerRequest();
            request.setUserId("user001");
            request.setUserName("张三");

            when(categoryService.addOwner(eq(1L), any(CategoryOwnerRequest.class))).thenReturn(testOwnerResponse);

            ApiResponse<CategoryOwnerResponse> response = categoryController.addOwner("1", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("100", response.getData().getId());
            assertEquals("user001", response.getData().getUserId());
            assertEquals("张三", response.getData().getUserName());
            verify(categoryService).addOwner(eq(1L), any(CategoryOwnerRequest.class));
        }

        @Test
        @DisplayName("获取责任人列表成功")
        void testGetOwners_Success() {
            List<CategoryOwnerResponse> mockOwners = new ArrayList<>();
            mockOwners.add(testOwnerResponse);

            when(categoryService.getOwners(1L)).thenReturn(mockOwners);

            ApiResponse<List<CategoryOwnerResponse>> response = categoryController.getOwners("1");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertEquals("user001", response.getData().get(0).getUserId());
            assertEquals("张三", response.getData().get(0).getUserName());
            verify(categoryService).getOwners(1L);
        }

        @Test
        @DisplayName("移除责任人成功")
        void testRemoveOwner_Success() {
            doNothing().when(categoryService).removeOwner(1L, "user001");

            ApiResponse<Void> response = categoryController.removeOwner("1", "user001");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNull(response.getData());
            verify(categoryService).removeOwner(1L, "user001");
        }
    }
}
