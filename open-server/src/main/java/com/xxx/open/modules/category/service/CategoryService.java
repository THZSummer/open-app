package com.xxx.open.modules.category.service;

import com.xxx.open.common.exception.BusinessException;
import com.xxx.open.common.util.SnowflakeIdGenerator;
import com.xxx.open.modules.category.dto.*;
import com.xxx.open.modules.category.entity.Category;
import com.xxx.open.modules.category.entity.CategoryOwner;
import com.xxx.open.modules.category.mapper.CategoryMapper;
import com.xxx.open.modules.category.mapper.CategoryOwnerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分类服务
 * 
 * <p>提供分类树形结构 CRUD 和责任人管理功能</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryOwnerMapper categoryOwnerMapper;
    private final SnowflakeIdGenerator idGenerator;

    // ==================== 分类 CRUD ====================

    /**
     * 获取分类树形列表
     * 
     * @param categoryAlias 分类别名过滤（可选）
     * @return 树形分类列表
     */
    public List<CategoryTreeResponse> getCategoryTree(String categoryAlias) {
        // 查询所有分类
        List<Category> categories;
        if (categoryAlias != null && !categoryAlias.isEmpty()) {
            // 根据分类别名查询（用于权限树查询）
            categories = categoryMapper.selectByCategoryAlias(categoryAlias);
        } else {
            // 查询所有分类
            categories = categoryMapper.selectAll();
        }

        if (categories.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建分类 ID 到实体的映射
        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        // 构建树形结构
        Map<Long, CategoryTreeResponse> responseMap = new HashMap<>();
        List<CategoryTreeResponse> roots = new ArrayList<>();

        // 第一遍遍历：创建所有响应对象
        for (Category category : categories) {
            CategoryTreeResponse response = convertToTreeResponse(category);
            responseMap.put(category.getId(), response);
        }

        // 第二遍遍历：构建树形关系
        for (Category category : categories) {
            CategoryTreeResponse response = responseMap.get(category.getId());
            if (category.getParentId() == null) {
                // 根节点
                roots.add(response);
            } else {
                // 子节点
                CategoryTreeResponse parent = responseMap.get(category.getParentId());
                if (parent != null) {
                    parent.getChildren().add(response);
                }
            }
        }

        return roots;
    }

    /**
     * 获取分类详情
     * 
     * @param id 分类ID
     * @return 分类详情
     */
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在", "Category not found");
        }
        return convertToResponse(category);
    }

    /**
     * 创建分类
     * 
     * @param request 创建请求
     * @return 分类响应
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        // 解析父分类ID
        Long parentId = null;
        if (request.getParentId() != null && !request.getParentId().isEmpty()) {
            try {
                parentId = Long.parseLong(request.getParentId());
            } catch (NumberFormatException e) {
                throw BusinessException.badRequest("父分类ID格式错误", "Invalid parent ID format");
            }
        }

        // 验证父分类是否存在
        String parentPath = "/";
        if (parentId != null) {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw BusinessException.notFound("父分类不存在", "Parent category not found");
            }
            parentPath = parent.getPath();
            // 子分类不能设置 categoryAlias
            if (request.getCategoryAlias() != null && !request.getCategoryAlias().isEmpty()) {
                log.warn("子分类不能设置分类别名，将被忽略: {}", request.getCategoryAlias());
            }
        }

        // 生成ID
        Long id = idGenerator.nextId();

        // 生成 path 字段
        String path = parentPath + id + "/";

        // 创建实体
        Category category = new Category();
        category.setId(id);
        category.setCategoryAlias(parentId == null ? request.getCategoryAlias() : null);
        category.setNameCn(request.getNameCn());
        category.setNameEn(request.getNameEn());
        category.setParentId(parentId);
        category.setPath(path);
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setStatus(1); // 默认启用
        category.setCreateTime(new Date());
        category.setLastUpdateTime(new Date());
        category.setCreateBy("system"); // TODO: 从上下文获取当前用户
        category.setLastUpdateBy("system");

        // 保存
        categoryMapper.insert(category);

        log.info("分类创建成功: id={}, nameCn={}, path={}", id, request.getNameCn(), path);

        return convertToResponse(category);
    }

    /**
     * 更新分类
     * 
     * @param id 分类ID
     * @param request 更新请求
     * @return 分类响应
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        // 查询分类
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在", "Category not found");
        }

        // 更新字段
        category.setNameCn(request.getNameCn());
        category.setNameEn(request.getNameEn());
        category.setSortOrder(request.getSortOrder());
        category.setLastUpdateTime(new Date());
        category.setLastUpdateBy("system"); // TODO: 从上下文获取当前用户

        // 保存
        categoryMapper.update(category);

        log.info("分类更新成功: id={}, nameCn={}", id, request.getNameCn());

        return convertToResponse(category);
    }

    /**
     * 删除分类
     * 
     * @param id 分类ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        // 查询分类
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在", "Category not found");
        }

        // 检查是否存在子分类
        int childCount = categoryMapper.countChildrenByParentId(id);
        if (childCount > 0) {
            throw new BusinessException("409", 
                    "分类下存在 " + childCount + " 个子分类，无法删除", 
                    "Category has " + childCount + " children, cannot delete");
        }

        // 检查是否存在关联资源
        int apiCount = categoryMapper.countApisByCategoryId(id);
        int eventCount = categoryMapper.countEventsByCategoryId(id);
        int callbackCount = categoryMapper.countCallbacksByCategoryId(id);
        int totalResources = apiCount + eventCount + callbackCount;

        if (totalResources > 0) {
            String resourceInfo = String.format("API: %d, 事件: %d, 回调: %d", apiCount, eventCount, callbackCount);
            throw new BusinessException("409", 
                    "分类下存在 " + totalResources + " 个资源（" + resourceInfo + "），无法删除", 
                    "Category has " + totalResources + " resources, cannot delete");
        }

        // 删除分类责任人
        List<CategoryOwner> owners = categoryOwnerMapper.selectByCategoryId(id);
        for (CategoryOwner owner : owners) {
            categoryOwnerMapper.deleteById(owner.getId());
        }

        // 删除分类
        categoryMapper.deleteById(id);

        log.info("分类删除成功: id={}", id);
    }

    // ==================== 责任人管理 ====================

    /**
     * 添加分类责任人
     * 
     * @param categoryId 分类ID
     * @param request 添加请求
     * @return 责任人响应
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryOwnerResponse addOwner(Long categoryId, CategoryOwnerRequest request) {
        // 验证分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BusinessException.notFound("分类不存在", "Category not found");
        }

        // 检查责任人是否已存在
        CategoryOwner existingOwner = categoryOwnerMapper.selectByCategoryIdAndUserId(
                categoryId, request.getUserId());
        if (existingOwner != null) {
            throw new BusinessException("409", 
                    "责任人已存在", 
                    "Owner already exists");
        }

        // 生成ID
        Long id = idGenerator.nextId();

        // 创建实体
        CategoryOwner owner = new CategoryOwner();
        owner.setId(id);
        owner.setCategoryId(categoryId);
        owner.setUserId(request.getUserId());
        owner.setUserName(request.getUserName());
        owner.setCreateTime(new Date());
        owner.setLastUpdateTime(new Date());
        owner.setCreateBy("system"); // TODO: 从上下文获取当前用户
        owner.setLastUpdateBy("system");

        // 保存
        categoryOwnerMapper.insert(owner);

        log.info("分类责任人添加成功: categoryId={}, userId={}", categoryId, request.getUserId());

        return convertToOwnerResponse(owner);
    }

    /**
     * 获取分类责任人列表
     * 
     * @param categoryId 分类ID
     * @return 责任人列表
     */
    public List<CategoryOwnerResponse> getOwners(Long categoryId) {
        // 验证分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BusinessException.notFound("分类不存在", "Category not found");
        }

        List<CategoryOwner> owners = categoryOwnerMapper.selectByCategoryId(categoryId);
        return owners.stream()
                .map(this::convertToOwnerResponse)
                .collect(Collectors.toList());
    }

    /**
     * 移除分类责任人
     * 
     * @param categoryId 分类ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeOwner(Long categoryId, String userId) {
        // 验证分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BusinessException.notFound("分类不存在", "Category not found");
        }

        // 删除责任人
        int deleted = categoryOwnerMapper.deleteByCategoryIdAndUserId(categoryId, userId);
        if (deleted == 0) {
            throw BusinessException.notFound("责任人不存在", "Owner not found");
        }

        log.info("分类责任人移除成功: categoryId={}, userId={}", categoryId, userId);
    }

    // ==================== 私有方法 ====================

    /**
     * 转换为树形响应
     */
    private CategoryTreeResponse convertToTreeResponse(Category category) {
        CategoryTreeResponse response = new CategoryTreeResponse();
        response.setId(String.valueOf(category.getId()));
        response.setCategoryAlias(category.getCategoryAlias());
        response.setNameCn(category.getNameCn());
        response.setNameEn(category.getNameEn());
        response.setParentId(category.getParentId() != null ? String.valueOf(category.getParentId()) : null);
        response.setPath(category.getPath());
        response.setCategoryPath(buildCategoryPath(category.getPath()));
        response.setSortOrder(category.getSortOrder());
        response.setStatus(category.getStatus());
        response.setCreateTime(category.getCreateTime());
        response.setCreateBy(category.getCreateBy());
        response.setChildren(new ArrayList<>());
        return response;
    }

    /**
     * 转换为响应
     */
    private CategoryResponse convertToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(String.valueOf(category.getId()));
        response.setCategoryAlias(category.getCategoryAlias());
        response.setNameCn(category.getNameCn());
        response.setNameEn(category.getNameEn());
        response.setParentId(category.getParentId() != null ? String.valueOf(category.getParentId()) : null);
        response.setPath(category.getPath());
        response.setCategoryPath(buildCategoryPath(category.getPath()));
        response.setSortOrder(category.getSortOrder());
        response.setStatus(category.getStatus());
        response.setCreateTime(category.getCreateTime());
        response.setCreateBy(category.getCreateBy());
        return response;
    }

    /**
     * 构建分类路径名称数组
     * 
     * @param path 分类路径，如 /1/2/3/
     * @return 分类路径名称数组，如 ["A类应用权限", "IM业务", "消息服务"]
     */
    private List<String> buildCategoryPath(String path) {
        if (path == null || path.isEmpty()) {
            return new ArrayList<>();
        }

        // 解析路径中的 ID
        String[] parts = path.split("/");
        List<Long> ids = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                try {
                    ids.add(Long.parseLong(part));
                } catch (NumberFormatException e) {
                    log.warn("无效的路径部分: {}", part);
                }
            }
        }

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询分类名称
        List<String> names = new ArrayList<>();
        for (Long id : ids) {
            Category category = categoryMapper.selectById(id);
            if (category != null) {
                names.add(category.getNameCn());
            }
        }

        return names;
    }

    /**
     * 转换为责任人响应
     */
    private CategoryOwnerResponse convertToOwnerResponse(CategoryOwner owner) {
        CategoryOwnerResponse response = new CategoryOwnerResponse();
        response.setId(String.valueOf(owner.getId()));
        response.setCategoryId(String.valueOf(owner.getCategoryId()));
        response.setUserId(owner.getUserId());
        response.setUserName(owner.getUserName());
        response.setCreateTime(owner.getCreateTime());
        return response;
    }
}
