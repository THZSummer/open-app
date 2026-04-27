package com.xxx.it.works.wecode.v2.modules.api.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.api.dto.*;
import com.xxx.it.works.wecode.v2.modules.api.service.ApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API 管理 Controller
 * 
 * <p>提供 API 注册、编辑、删除、撤回等接口</p>
 * <p>接口编号：#9 ~ #14</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/apis")
@RequiredArgsConstructor
@Tag(name = "API 管理", description = "API 资源管理接口")
public class ApiController {

    private final ApiService apiService;

    /**
     * #9 获取 API 列表
     * 
     * <p>返回 API 列表，支持按分类过滤和分页</p>
     * 
     * @param categoryId 分类ID过滤（可选）
     * @param status 状态过滤（可选）
     * @param keyword 搜索关键词（可选）
     * @param curPage 当前页码（默认 1）
     * @param pageSize 每页数量（默认 20）
     * @return API 列表
     */
    @GetMapping
    @Operation(summary = "#9 获取 API 列表", 
               description = "返回 API 列表，支持按分类过滤和分页")
    public ApiResponse<List<ApiListResponse>> getApiList(
            @Parameter(description = "分类ID过滤") 
            @RequestParam(required = false) String categoryId,
            @Parameter(description = "状态过滤") 
            @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词") 
            @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码") 
            @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量") 
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        
        log.info("获取 API 列表, categoryId={}, status={}, keyword={}, curPage={}, pageSize={}", 
                categoryId, status, keyword, curPage, pageSize);
        
        ApiListRequest request = new ApiListRequest();
        request.setCategoryId(categoryId);
        request.setStatus(status);
        request.setKeyword(keyword);
        request.setCurPage(curPage);
        request.setPageSize(pageSize);
        
        return apiService.getApiList(request);
    }

    /**
     * #10 获取 API 详情
     * 
     * <p>返回 API 详情及权限信息、属性</p>
     * 
     * @param id API ID
     * @return API 详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "#10 获取 API 详情", 
               description = "返回 API 详情及权限信息、属性")
    public ApiResponse<ApiDetailResponse> getApiDetail(
            @Parameter(description = "API ID") 
            @PathVariable String id) {
        
        log.info("获取 API 详情, id={}", id);
        
        ApiDetailResponse response = apiService.getApiDetail(id);
        return ApiResponse.success(response);
    }

    /**
     * #11 注册 API
     * 
     * <p>注册 API 成功，同时创建权限资源</p>
     * 
     * @param request 注册请求
     * @return API 详情
     */
    @PostMapping
    @Operation(summary = "#11 注册 API", 
               description = "注册 API 成功，同时创建权限资源")
    public ApiResponse<ApiDetailResponse> createApi(
            @Valid @RequestBody ApiCreateRequest request) {
        
        log.info("注册 API, nameCn={}, path={}", request.getNameCn(), request.getPath());
        
        ApiDetailResponse response = apiService.createApi(request);
        return ApiResponse.success(response);
    }

    /**
     * #12 更新 API
     * 
     * <p>更新 API 成功，核心属性变更触发审批</p>
     * 
     * @param id API ID
     * @param request 更新请求
     * @return API 详情
     */
    @PutMapping("/{id}")
    @Operation(summary = "#12 更新 API", 
               description = "更新 API 成功，核心属性变更触发审批")
    public ApiResponse<ApiDetailResponse> updateApi(
            @Parameter(description = "API ID") 
            @PathVariable String id,
            @Valid @RequestBody ApiUpdateRequest request) {
        
        log.info("更新 API, id={}, nameCn={}", id, request.getNameCn());
        
        ApiDetailResponse response = apiService.updateApi(id, request);
        return ApiResponse.success(response);
    }

    /**
     * #13 删除 API
     * 
     * <p>删除 API，检查订阅关系</p>
     * 
     * @param id API ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "#13 删除 API", 
               description = "删除 API，检查订阅关系")
    public ApiResponse<Void> deleteApi(
            @Parameter(description = "API ID") 
            @PathVariable String id) {
        
        log.info("删除 API, id={}", id);
        
        apiService.deleteApi(id);
        return ApiResponse.success();
    }

    /**
     * #14 撤回审核中的 API
     * 
     * <p>撤回审核中的 API</p>
     * 
     * @param id API ID
     * @return API 详情
     */
    @PostMapping("/{id}/withdraw")
    @Operation(summary = "#14 撤回审核中的 API", 
               description = "撤回审核中的 API，状态变为草稿")
    public ApiResponse<ApiDetailResponse> withdrawApi(
            @Parameter(description = "API ID") 
            @PathVariable String id) {
        
        log.info("撤回 API, id={}", id);
        
        ApiDetailResponse response = apiService.withdrawApi(id);
        return ApiResponse.success(response);
    }
}
