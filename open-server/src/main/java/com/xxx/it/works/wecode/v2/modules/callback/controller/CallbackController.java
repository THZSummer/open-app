package com.xxx.it.works.wecode.v2.modules.callback.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.callback.dto.*;
import com.xxx.it.works.wecode.v2.modules.callback.service.CallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 回调管理 Controller
 * 
 * <p>提供回调资源管理功能，包括回调注册、编辑、删除、撤回等</p>
 * <p>接口编号：#21 ~ #26</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/callbacks")
@RequiredArgsConstructor
@Tag(name = "回调管理", description = "回调资源管理接口，覆盖 FR-012~FR-015")
public class CallbackController {

    private final CallbackService callbackService;

    // ==================== 回调 CRUD ====================

    /**
     * #21 获取回调列表
     * 
     * <p>返回回调列表，支持按分类过滤，支持分页参数</p>
     * 
     * @param categoryId 分类ID过滤（可选）
     * @param status 状态过滤（可选）
     * @param keyword 搜索关键词（可选）
     * @param curPage 当前页码，默认 1
     * @param pageSize 每页数量，默认 20
     * @return 分页回调列表
     */
    @GetMapping
    @Operation(summary = "#21 获取回调列表", 
               description = "返回回调列表，支持按分类过滤，支持分页参数 curPage 和 pageSize")
    public ApiResponse<List<CallbackListResponse>> getCallbackList(
            @Parameter(description = "分类ID过滤") 
            @RequestParam(required = false) String categoryId,
            @Parameter(description = "状态过滤（0=草稿, 1=待审, 2=已发布, 3=已下线）") 
            @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词（名称）") 
            @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码，默认 1") 
            @RequestParam(required = false) Integer curPage,
            @Parameter(description = "每页数量，默认 20") 
            @RequestParam(required = false) Integer pageSize) {
        
        log.info("Get callback list: categoryId={}, status={}, keyword={}, curPage={}, pageSize={}", 
                categoryId, status, keyword, curPage, pageSize);
        
        return callbackService.getCallbackList(categoryId, status, keyword, curPage, pageSize);
    }

    /**
     * #22 获取回调详情
     * 
     * <p>返回回调详情及权限信息、属性</p>
     * 
     * @param id 回调ID
     * @return 回调详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "#22 获取回调详情", 
               description = "返回回调详情及权限信息、属性")
    public ApiResponse<CallbackResponse> getCallbackById(
            @Parameter(description = "回调ID") 
            @PathVariable String id) {
        
        log.info("Get callback detail: id={}", id);
        
        CallbackResponse response = callbackService.getCallbackById(parseId(id));
        return ApiResponse.success(response);
    }

    /**
     * #23 注册回调
     * 
     * <p>注册回调成功，同时创建权限资源</p>
     * 
     * @param request 创建请求
     * @return 回调响应
     */
    @PostMapping
    @Operation(summary = "#23 注册回调", 
               description = "注册回调成功，同时创建权限资源。Scope 格式：callback:{module}:{identifier}")
    public ApiResponse<CallbackResponse> createCallback(
            @Valid @RequestBody CallbackCreateRequest request) {
        
        log.info("Register callback: nameCn={}, scope={}", request.getNameCn(), 
                request.getPermission() != null ? request.getPermission().getScope() : null);
        
        CallbackResponse response = callbackService.createCallback(request);
        
        ApiResponse<CallbackResponse> apiResponse = ApiResponse.success(response);
        apiResponse.setMessageZh("回调注册成功，等待审批");
        apiResponse.setMessageEn("Callback registered successfully, waiting for approval");
        
        return apiResponse;
    }

    /**
     * #24 更新回调
     * 
     * <p>更新回调成功</p>
     * 
     * @param id 回调ID
     * @param request 更新请求
     * @return 回调响应
     */
    @PutMapping("/{id}")
    @Operation(summary = "#24 更新回调", 
               description = "更新回调成功")
    public ApiResponse<CallbackResponse> updateCallback(
            @Parameter(description = "回调ID") 
            @PathVariable String id,
            @Valid @RequestBody CallbackUpdateRequest request) {
        
        log.info("Update callback: id={}, nameCn={}", id, request.getNameCn());
        
        CallbackResponse response = callbackService.updateCallback(parseId(id), request);
        return ApiResponse.success(response);
    }

    /**
     * #25 删除回调
     * 
     * <p>删除回调，检查订阅关系</p>
     * 
     * @param id 回调ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "#25 删除回调", 
               description = "删除回调，检查订阅关系。已订阅的回调无法删除，需先取消所有订阅。")
    public ApiResponse<Void> deleteCallback(
            @Parameter(description = "回调ID") 
            @PathVariable String id) {
        
        log.info("Delete callback: id={}", id);
        
        callbackService.deleteCallback(parseId(id));
        return ApiResponse.success();
    }

    /**
     * #26 撤回审核中的回调
     * 
     * <p>撤回审核中的回调</p>
     * 
     * @param id 回调ID
     * @return 回调响应
     */
    @PostMapping("/{id}/withdraw")
    @Operation(summary = "#26 撤回审核中的回调", 
               description = "仅状态为待审的回调可撤回，撤回后状态变为草稿")
    public ApiResponse<CallbackResponse> withdrawCallback(
            @Parameter(description = "回调ID") 
            @PathVariable String id) {
        
        log.info("Withdraw callback: id={}", id);
        
        CallbackResponse response = callbackService.withdrawCallback(parseId(id));
        
        ApiResponse<CallbackResponse> apiResponse = ApiResponse.success(response);
        apiResponse.setMessageZh("回调已撤回，状态变为草稿");
        apiResponse.setMessageEn("Callback withdrawn, status changed to draft");
        
        return apiResponse;
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
