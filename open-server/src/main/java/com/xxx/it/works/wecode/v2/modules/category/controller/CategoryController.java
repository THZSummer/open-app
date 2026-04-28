package com.xxx.it.works.wecode.v2.modules.category.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.category.dto.*;
import com.xxx.it.works.wecode.v2.modules.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理 Controller
 * 
 * <p>提供分类树形结构 CRUD 和责任人管理接口</p>
 * <p>接口编号：#1 ~ #8</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/categories")
@RequiredArgsConstructor
@Tag(name = "分类管理", description = "分类树形结构 CRUD 和责任人管理接口")
public class CategoryController {

    private final CategoryService categoryService;

    // ==================== 分类 CRUD ====================

    /**
     * #1 获取分类列表（树形）
     * 
     * <p>返回树形分类列表，支持 categoryAlias 过滤（权限树查树）</p>
     * 
     * @param categoryAlias 分类别名过滤（可选）
     * @return 树形分类列表
     */
    @GetMapping
    @Operation(summary = "#1 获取分类列表（树形）", 
               description = "返回树形分类列表，支持 categoryAlias 过滤（权限树查树）")
    public ApiResponse<List<CategoryTreeResponse>> getCategoryTree(
            @Parameter(description = "分类别名过滤") 
            @RequestParam(required = false) String categoryAlias) {
        
        log.info("Get category tree list, categoryAlias={}", categoryAlias);
        
        List<CategoryTreeResponse> tree = categoryService.getCategoryTree(categoryAlias);
        return ApiResponse.success(tree);
    }

    /**
     * #2 获取分类详情
     * 
     * @param id 分类ID
     * @return 分类详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "#2 获取分类详情", 
               description = "返回分类详情，包含 path 和 categoryPath 字段")
    public ApiResponse<CategoryResponse> getCategoryById(
            @Parameter(description = "分类ID") 
            @PathVariable String id) {
        
        log.info("Get category detail, id={}", id);
        
        CategoryResponse response = categoryService.getCategoryById(parseId(id));
        return ApiResponse.success(response);
    }

    /**
     * #3 创建分类
     * 
     * @param request 创建请求
     * @return 分类响应
     */
    @PostMapping
    @Operation(summary = "#3 创建分类", 
               description = "创建分类成功，path 字段自动生成")
    public ApiResponse<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        
        log.info("Create category, nameCn={}", request.getNameCn());
        
        CategoryResponse response = categoryService.createCategory(request);
        return ApiResponse.success(response);
    }

    /**
     * #4 更新分类
     * 
     * @param id 分类ID
     * @param request 更新请求
     * @return 分类响应
     */
    @PutMapping("/{id}")
    @Operation(summary = "#4 更新分类", 
               description = "更新分类成功")
    public ApiResponse<CategoryResponse> updateCategory(
            @Parameter(description = "分类ID") 
            @PathVariable String id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        
        log.info("Update category, id={}, nameCn={}", id, request.getNameCn());
        
        CategoryResponse response = categoryService.updateCategory(parseId(id), request);
        return ApiResponse.success(response);
    }

    /**
     * #5 删除分类
     * 
     * <p>删除前检查是否存在关联资源</p>
     * 
     * @param id 分类ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "#5 删除分类", 
               description = "删除分类，检查关联资源")
    public ApiResponse<Void> deleteCategory(
            @Parameter(description = "分类ID") 
            @PathVariable String id) {
        
        log.info("Delete category, id={}", id);
        
        categoryService.deleteCategory(parseId(id));
        return ApiResponse.success();
    }

    // ==================== 责任人管理 ====================

    /**
     * #6 添加分类责任人
     * 
     * @param id 分类ID
     * @param request 添加请求
     * @return 责任人响应
     */
    @PostMapping("/{id}/owners")
    @Operation(summary = "#6 添加分类责任人", 
               description = "添加责任人成功")
    public ApiResponse<CategoryOwnerResponse> addOwner(
            @Parameter(description = "分类ID") 
            @PathVariable String id,
            @Valid @RequestBody CategoryOwnerRequest request) {
        
        log.info("Add category owner, categoryId={}, userId={}", id, request.getUserId());
        
        CategoryOwnerResponse response = categoryService.addOwner(parseId(id), request);
        return ApiResponse.success(response);
    }

    /**
     * #7 获取分类责任人列表
     * 
     * @param id 分类ID
     * @return 责任人列表
     */
    @GetMapping("/{id}/owners")
    @Operation(summary = "#7 获取分类责任人列表", 
               description = "返回责任人列表")
    public ApiResponse<List<CategoryOwnerResponse>> getOwners(
            @Parameter(description = "分类ID") 
            @PathVariable String id) {
        
        log.info("Get category owner list, categoryId={}", id);
        
        List<CategoryOwnerResponse> owners = categoryService.getOwners(parseId(id));
        return ApiResponse.success(owners);
    }

    /**
     * #8 移除分类责任人
     * 
     * @param id 分类ID
     * @param userId 用户ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}/owners/{userId}")
    @Operation(summary = "#8 移除分类责任人", 
               description = "移除责任人成功")
    public ApiResponse<Void> removeOwner(
            @Parameter(description = "分类ID") 
            @PathVariable String id,
            @Parameter(description = "用户ID") 
            @PathVariable String userId) {
        
        log.info("Remove category owner, categoryId={}, userId={}", id, userId);
        
        categoryService.removeOwner(parseId(id), userId);
        return ApiResponse.success();
    }

    // ==================== 私有方法 ====================

    /**
     * 解析 ID
     */
    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID format: " + id);
        }
    }
}
