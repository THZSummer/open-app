package com.xxx.it.works.wecode.v2.modules.api.service;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.api.dto.ApiUpdateRequest;
import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.category.entity.Category;
import com.xxx.it.works.wecode.v2.modules.category.mapper.CategoryMapper;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionPropertyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApiService 测试类
 *
 * 测试部分字段更新功能
 */
@ExtendWith(MockitoExtension.class)
class ApiServiceTest {

    @Mock
    private ApiMapper apiMapper;

    @Mock
    private ApiPropertyMapper apiPropertyMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private PermissionPropertyMapper permissionPropertyMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private IdGeneratorStrategy idGenerator;

    @InjectMocks
    private ApiService apiService;

    private Api existingApi;
    private Category category;

    @BeforeEach
    void setUp() {

        // 准备测试数据
        existingApi = new Api();
        existingApi.setId(100L);
        existingApi.setNameCn("原始中文名称");
        existingApi.setNameEn("Original English Name");
        existingApi.setPath("/test/api");
        existingApi.setMethod("GET");
        existingApi.setCategoryId(2L);
        existingApi.setStatus(1);
        existingApi.setCreateTime(new Date());
        existingApi.setLastUpdateTime(new Date());

        category = new Category();
        category.setId(2L);
        category.setNameCn("测试分类");
        category.setNameEn("Test Category");
    }

    @Test
    @DisplayName("部分字段更新 - 只更新中文名称")
    void testPartialUpdate_OnlyNameCn() {

        // Given: 只提供中文名称
        ApiUpdateRequest request = new ApiUpdateRequest();
        request.setNameCn("新的中文名称");

        // nameEn 和 categoryId 为 null

        when(apiMapper.selectById(100L)).thenReturn(existingApi);
        when(apiMapper.update(any(Api.class))).thenReturn(1);

        // When
        apiService.updateApi("100", request);

        // Then: 验证只更新了 nameCn，其他字段保持不变
        verify(apiMapper).update(argThat(api ->
            "新的中文名称".equals(api.getNameCn()) &&
            "Original English Name".equals(api.getNameEn()) &&
            api.getCategoryId().equals(2L)
        ));
    }

    @Test
    @DisplayName("部分字段更新 - 只更新英文名称")
    void testPartialUpdate_OnlyNameEn() {

        // Given: 只提供英文名称
        ApiUpdateRequest request = new ApiUpdateRequest();
        request.setNameEn("New English Name");

        // nameCn 和 categoryId 为 null

        when(apiMapper.selectById(100L)).thenReturn(existingApi);
        when(apiMapper.update(any(Api.class))).thenReturn(1);

        // When
        apiService.updateApi("100", request);

        // Then: 验证只更新了 nameEn
        verify(apiMapper).update(argThat(api ->
            "原始中文名称".equals(api.getNameCn()) &&
            "New English Name".equals(api.getNameEn())
        ));
    }

    @Test
    @DisplayName("部分字段更新 - 同时更新多个字段")
    void testPartialUpdate_MultipleFields() {

        // Given: 提供多个字段
        ApiUpdateRequest request = new ApiUpdateRequest();
        request.setNameCn("新中文名称");
        request.setNameEn("New Name");
        request.setCategoryId("3");

        Category newCategory = new Category();
        newCategory.setId(3L);
        newCategory.setNameCn("新分类");

        when(apiMapper.selectById(100L)).thenReturn(existingApi);
        when(categoryMapper.selectById(3L)).thenReturn(newCategory);
        when(apiMapper.update(any(Api.class))).thenReturn(1);

        // When
        apiService.updateApi("100", request);

        // Then: 验证所有提供的字段都被更新
        verify(apiMapper).update(argThat(api ->
            "新中文名称".equals(api.getNameCn()) &&
            "New Name".equals(api.getNameEn()) &&
            api.getCategoryId().equals(3L)
        ));
    }

    @Test
    @DisplayName("部分字段更新 - 空字符串应被忽略")
    void testPartialUpdate_EmptyStringShouldBeIgnored() {

        // Given: 提供空字符串
        ApiUpdateRequest request = new ApiUpdateRequest();
        request.setNameCn("  "); // 空格字符串
        request.setNameEn("Valid Name");

        when(apiMapper.selectById(100L)).thenReturn(existingApi);
        when(apiMapper.update(any(Api.class))).thenReturn(1);

        // When
        apiService.updateApi("100", request);

        // Then: nameCn 应保持不变，只有 nameEn 被更新
        verify(apiMapper).update(argThat(api ->
            "原始中文名称".equals(api.getNameCn()) &&
            "Valid Name".equals(api.getNameEn())
        ));
    }

    @Test
    @DisplayName("更新不存在的API应抛出异常")
    void testUpdateNonExistentApi() {

        // Given
        ApiUpdateRequest request = new ApiUpdateRequest();
        request.setNameCn("新名称");

        when(apiMapper.selectById(999L)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            apiService.updateApi("999", request);
        });
    }

    @Test
    @DisplayName("更新分类ID为不存在的分类应抛出异常")
    void testUpdateWithNonExistentCategory() {

        // Given
        ApiUpdateRequest request = new ApiUpdateRequest();
        request.setCategoryId("999");

        when(apiMapper.selectById(100L)).thenReturn(existingApi);
        when(categoryMapper.selectById(999L)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            apiService.updateApi("100", request);
        });
    }
}
