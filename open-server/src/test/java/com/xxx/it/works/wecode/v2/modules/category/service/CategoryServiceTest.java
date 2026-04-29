package com.xxx.it.works.wecode.v2.modules.category.service;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.category.dto.*;
import com.xxx.it.works.wecode.v2.modules.category.entity.Category;
import com.xxx.it.works.wecode.v2.modules.category.entity.CategoryOwner;
import com.xxx.it.works.wecode.v2.modules.category.mapper.CategoryMapper;
import com.xxx.it.works.wecode.v2.modules.category.mapper.CategoryOwnerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 分类服务测试
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CategoryOwnerMapper categoryOwnerMapper;

    @Mock
    private IdGeneratorStrategy idGenerator;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;
    private CategoryOwner testOwner;

    @BeforeEach
    void setUp() {

        // 创建测试分类
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setCategoryAlias("app_type_a");
        testCategory.setNameCn("A类应用权限");
        testCategory.setNameEn("App Type A Permissions");
        testCategory.setParentId(null);
        testCategory.setPath("/1/");
        testCategory.setSortOrder(0);
        testCategory.setStatus(1);
        testCategory.setCreateTime(new Date());
        testCategory.setLastUpdateTime(new Date());
        testCategory.setCreateBy("system");
        testCategory.setLastUpdateBy("system");

        // 创建测试责任人
        testOwner = new CategoryOwner();
        testOwner.setId(100L);
        testOwner.setCategoryId(1L);
        testOwner.setUserId("user001");
        testOwner.setUserName("张三");
        testOwner.setCreateTime(new Date());
        testOwner.setLastUpdateTime(new Date());
        testOwner.setCreateBy("system");
        testOwner.setLastUpdateBy("system");
    }

    @Test
    @DisplayName("获取分类详情 - 成功")
    void testGetCategoryById_Success() {

        // Given
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);

        // When
        CategoryResponse response = categoryService.getCategoryById(1L);

        // Then
        assertNotNull(response);
        assertEquals("1", response.getId());
        assertEquals("A类应用权限", response.getNameCn());
        assertEquals("/1/", response.getPath());
        assertEquals(1, response.getCategoryPath().size());
        assertEquals("A类应用权限", response.getCategoryPath().get(0));
    }

    @Test
    @DisplayName("获取分类详情 - 分类不存在")
    void testGetCategoryById_NotFound() {

        // Given
        when(categoryMapper.selectById(999L)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            categoryService.getCategoryById(999L);
        });
    }

    @Test
    @DisplayName("创建分类 - 根分类")
    void testCreateCategory_Root() {

        // Given
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setCategoryAlias("app_type_a");
        request.setNameCn("A类应用权限");
        request.setNameEn("App Type A Permissions");
        request.setParentId(null);
        request.setSortOrder(0);

        when(idGenerator.nextId()).thenReturn(1L);
        when(categoryMapper.insert(any(Category.class))).thenReturn(1);

        // When
        CategoryResponse response = categoryService.createCategory(request);

        // Then
        assertNotNull(response);
        assertEquals("1", response.getId());
        assertEquals("A类应用权限", response.getNameCn());
        assertEquals("/1/", response.getPath());

        verify(categoryMapper, times(1)).insert(any(Category.class));
    }

    @Test
    @DisplayName("创建分类 - 子分类")
    void testCreateCategory_Child() {

        // Given
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setNameCn("IM业务");
        request.setNameEn("IM Business");
        request.setParentId("1");
        request.setSortOrder(0);

        when(idGenerator.nextId()).thenReturn(2L);
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.insert(any(Category.class))).thenReturn(1);

        // When
        CategoryResponse response = categoryService.createCategory(request);

        // Then
        assertNotNull(response);
        assertEquals("2", response.getId());
        assertEquals("IM业务", response.getNameCn());
        assertEquals("/1/2/", response.getPath());

        verify(categoryMapper, times(1)).insert(any(Category.class));
    }

    @Test
    @DisplayName("更新分类 - 成功")
    void testUpdateCategory_Success() {

        // Given
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setNameCn("更新后的名称");
        request.setNameEn("Updated Name");
        request.setSortOrder(1);

        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.update(any(Category.class))).thenReturn(1);

        // When
        CategoryResponse response = categoryService.updateCategory(1L, request);

        // Then
        assertNotNull(response);
        assertEquals("更新后的名称", response.getNameCn());

        verify(categoryMapper, times(1)).update(any(Category.class));
    }

    @Test
    @DisplayName("删除分类 - 成功")
    void testDeleteCategory_Success() {

        // Given
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.countChildrenByParentId(1L)).thenReturn(0);
        when(categoryMapper.countApisByCategoryId(1L)).thenReturn(0);
        when(categoryMapper.countEventsByCategoryId(1L)).thenReturn(0);
        when(categoryMapper.countCallbacksByCategoryId(1L)).thenReturn(0);
        when(categoryOwnerMapper.selectByCategoryId(1L)).thenReturn(new ArrayList<>());
        when(categoryMapper.deleteById(1L)).thenReturn(1);

        // When
        categoryService.deleteCategory(1L);

        // Then
        verify(categoryMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("删除分类 - 存在子分类")
    void testDeleteCategory_HasChildren() {

        // Given
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.countChildrenByParentId(1L)).thenReturn(2);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            categoryService.deleteCategory(1L);
        });

        assertEquals("409", exception.getCode());
        assertTrue(exception.getMessageZh().contains("子分类"));
    }

    @Test
    @DisplayName("删除分类 - 存在关联资源")
    void testDeleteCategory_HasResources() {

        // Given
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.countChildrenByParentId(1L)).thenReturn(0);
        when(categoryMapper.countApisByCategoryId(1L)).thenReturn(5);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            categoryService.deleteCategory(1L);
        });

        assertEquals("409", exception.getCode());
        assertTrue(exception.getMessageZh().contains("资源"));
    }

    @Test
    @DisplayName("添加责任人 - 成功")
    void testAddOwner_Success() {

        // Given
        CategoryOwnerRequest request = new CategoryOwnerRequest();
        request.setUserId("user001");
        request.setUserName("张三");

        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryOwnerMapper.selectByCategoryIdAndUserId(1L, "user001")).thenReturn(null);
        when(idGenerator.nextId()).thenReturn(100L);
        when(categoryOwnerMapper.insert(any(CategoryOwner.class))).thenReturn(1);

        // When
        CategoryOwnerResponse response = categoryService.addOwner(1L, request);

        // Then
        assertNotNull(response);
        assertEquals("100", response.getId());
        assertEquals("user001", response.getUserId());
        assertEquals("张三", response.getUserName());

        verify(categoryOwnerMapper, times(1)).insert(any(CategoryOwner.class));
    }

    @Test
    @DisplayName("添加责任人 - 已存在")
    void testAddOwner_AlreadyExists() {

        // Given
        CategoryOwnerRequest request = new CategoryOwnerRequest();
        request.setUserId("user001");
        request.setUserName("张三");

        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryOwnerMapper.selectByCategoryIdAndUserId(1L, "user001")).thenReturn(testOwner);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            categoryService.addOwner(1L, request);
        });

        assertEquals("409", exception.getCode());
        assertTrue(exception.getMessageZh().contains("已存在"));
    }

    @Test
    @DisplayName("获取责任人列表 - 成功")
    void testGetOwners_Success() {

        // Given
        List<CategoryOwner> owners = new ArrayList<>();
        owners.add(testOwner);

        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryOwnerMapper.selectByCategoryId(1L)).thenReturn(owners);

        // When
        List<CategoryOwnerResponse> response = categoryService.getOwners(1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("user001", response.get(0).getUserId());
    }

    @Test
    @DisplayName("移除责任人 - 成功")
    void testRemoveOwner_Success() {

        // Given
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryOwnerMapper.deleteByCategoryIdAndUserId(1L, "user001")).thenReturn(1);

        // When
        categoryService.removeOwner(1L, "user001");

        // Then
        verify(categoryOwnerMapper, times(1)).deleteByCategoryIdAndUserId(1L, "user001");
    }

    @Test
    @DisplayName("移除责任人 - 不存在")
    void testRemoveOwner_NotFound() {

        // Given
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryOwnerMapper.deleteByCategoryIdAndUserId(1L, "user999")).thenReturn(0);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            categoryService.removeOwner(1L, "user999");
        });
    }
}
