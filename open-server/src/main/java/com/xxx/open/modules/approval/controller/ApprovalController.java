package com.xxx.open.modules.approval.controller;

import com.xxx.open.common.model.ApiResponse;
import com.xxx.open.modules.approval.dto.*;
import com.xxx.open.modules.approval.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 审批管理 Controller
 * 
 * <p>实现审批管理相关的 11 个接口：</p>
 * <ul>
 *   <li>#41 GET /api/v1/approval-flows - 返回审批流程模板列表</li>
 *   <li>#42 GET /api/v1/approval-flows/:id - 返回审批流程模板详情</li>
 *   <li>#43 POST /api/v1/approval-flows - 创建审批流程模板</li>
 *   <li>#44 PUT /api/v1/approval-flows/:id - 更新审批流程模板</li>
 *   <li>#45 GET /api/v1/approvals/pending - 返回待审批列表</li>
 *   <li>#46 GET /api/v1/approvals/:id - 返回审批详情</li>
 *   <li>#47 POST /api/v1/approvals/:id/approve - 同意审批</li>
 *   <li>#48 POST /api/v1/approvals/:id/reject - 驳回审批</li>
 *   <li>#49 POST /api/v1/approvals/:id/cancel - 撤销审批</li>
 *   <li>#50 POST /api/v1/approvals/batch-approve - 批量同意审批</li>
 *   <li>#51 POST /api/v1/approvals/batch-reject - 批量驳回审批</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    // ==================== 审批流程模板管理 (#41-44) ====================

    /**
     * #41 获取审批流程模板列表
     * 
     * @param keyword 搜索关键词
     * @param curPage 当前页码
     * @param pageSize 每页数量
     * @return 审批流程模板列表
     */
    @GetMapping("/approval-flows")
    public ApiResponse<List<ApprovalFlowListResponse>> getFlowList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer curPage,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        log.info("获取审批流程列表: keyword={}, curPage={}, pageSize={}", keyword, curPage, pageSize);

        ApprovalFlowListRequest request = new ApprovalFlowListRequest();
        request.setKeyword(keyword);
        request.setCurPage(curPage);
        request.setPageSize(pageSize);

        List<ApprovalFlowListResponse> data = approvalService.getFlowList(request);
        Long total = approvalService.countFlowList(keyword);

        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(curPage)
                .pageSize(pageSize)
                .total(total)
                .totalPages((int) Math.ceil((double) total / pageSize))
                .build();

        return ApiResponse.success(data, page);
    }

    /**
     * #42 获取审批流程模板详情
     * 
     * @param id 流程ID
     * @return 审批流程模板详情
     */
    @GetMapping("/approval-flows/{id}")
    public ApiResponse<ApprovalFlowDetailResponse> getFlowDetail(@PathVariable String id) {
        log.info("获取审批流程详情: id={}", id);

        ApprovalFlowDetailResponse data = approvalService.getFlowDetail(Long.parseLong(id));
        return ApiResponse.success(data);
    }

    /**
     * #43 创建审批流程模板
     * 
     * @param request 创建请求
     * @return 创建的审批流程模板
     */
    @PostMapping("/approval-flows")
    public ApiResponse<ApprovalFlowDetailResponse> createFlow(
            @Valid @RequestBody ApprovalFlowCreateRequest request) {
        log.info("创建审批流程: code={}", request.getCode());

        String operator = "system"; // TODO: 从上下文获取当前用户
        ApprovalFlowDetailResponse data = approvalService.createFlow(request, operator);
        return ApiResponse.success(data);
    }

    /**
     * #44 更新审批流程模板
     * 
     * @param id 流程ID
     * @param request 更新请求
     * @return 更新后的审批流程模板
     */
    @PutMapping("/approval-flows/{id}")
    public ApiResponse<ApprovalFlowDetailResponse> updateFlow(
            @PathVariable String id,
            @Valid @RequestBody ApprovalFlowUpdateRequest request) {
        log.info("更新审批流程: id={}", id);

        String operator = "system"; // TODO: 从上下文获取当前用户
        ApprovalFlowDetailResponse data = approvalService.updateFlow(Long.parseLong(id), request, operator);
        return ApiResponse.success(data);
    }

    // ==================== 审批执行管理 (#45-51) ====================

    /**
     * #45 获取待审批列表
     * 
     * @param type 审批类型
     * @param keyword 搜索关键词
     * @param curPage 当前页码
     * @param pageSize 每页数量
     * @return 待审批列表
     */
    @GetMapping("/approvals/pending")
    public ApiResponse<List<ApprovalPendingListResponse>> getPendingList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer curPage,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        log.info("获取待审批列表: type={}, keyword={}, curPage={}, pageSize={}", type, keyword, curPage, pageSize);

        ApprovalPendingListRequest request = new ApprovalPendingListRequest();
        request.setType(type);
        request.setKeyword(keyword);
        request.setCurPage(curPage);
        request.setPageSize(pageSize);

        List<ApprovalPendingListResponse> data = approvalService.getPendingList(request);
        Long total = approvalService.countPendingList(type, keyword);

        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(curPage)
                .pageSize(pageSize)
                .total(total)
                .totalPages((int) Math.ceil((double) total / pageSize))
                .build();

        return ApiResponse.success(data, page);
    }

    /**
     * #46 获取审批详情
     * 
     * @param id 审批记录ID
     * @return 审批详情
     */
    @GetMapping("/approvals/{id}")
    public ApiResponse<ApprovalDetailResponse> getApprovalDetail(@PathVariable String id) {
        log.info("获取审批详情: id={}", id);

        ApprovalDetailResponse data = approvalService.getApprovalDetail(Long.parseLong(id));
        return ApiResponse.success(data);
    }

    /**
     * #47 同意审批
     * 
     * @param id 审批记录ID
     * @param request 审批请求
     * @return 审批操作结果
     */
    @PostMapping("/approvals/{id}/approve")
    public ApiResponse<ApprovalActionResponse> approve(
            @PathVariable String id,
            @RequestBody ApprovalActionRequest request) {
        log.info("同意审批: id={}", id);

        String operatorId = "user001"; // TODO: 从上下文获取当前用户
        String operatorName = "审批人"; // TODO: 从上下文获取当前用户
        String operator = "system"; // TODO: 从上下文获取当前用户

        ApprovalActionResponse data = approvalService.approve(
                Long.parseLong(id), request, operatorId, operatorName, operator);
        return ApiResponse.success(data);
    }

    /**
     * #48 驳回审批
     * 
     * @param id 审批记录ID
     * @param request 驳回请求
     * @return 审批操作结果
     */
    @PostMapping("/approvals/{id}/reject")
    public ApiResponse<ApprovalActionResponse> reject(
            @PathVariable String id,
            @Valid @RequestBody ApprovalActionRequest request) {
        log.info("驳回审批: id={}", id);

        String operatorId = "user001"; // TODO: 从上下文获取当前用户
        String operatorName = "审批人"; // TODO: 从上下文获取当前用户
        String operator = "system"; // TODO: 从上下文获取当前用户

        ApprovalActionResponse data = approvalService.reject(
                Long.parseLong(id), request, operatorId, operatorName, operator);
        return ApiResponse.success(data);
    }

    /**
     * #49 撤销审批
     * 
     * @param id 审批记录ID
     * @return 审批操作结果
     */
    @PostMapping("/approvals/{id}/cancel")
    public ApiResponse<ApprovalActionResponse> cancel(@PathVariable String id) {
        log.info("撤销审批: id={}", id);

        String operator = "system"; // TODO: 从上下文获取当前用户

        ApprovalActionResponse data = approvalService.cancel(Long.parseLong(id), operator);
        return ApiResponse.success(data);
    }

    /**
     * #50 批量同意审批
     * 
     * @param request 批量审批请求
     * @return 批量审批结果
     */
    @PostMapping("/approvals/batch-approve")
    public ApiResponse<BatchApprovalResponse> batchApprove(
            @Valid @RequestBody BatchApprovalRequest request) {
        log.info("批量同意审批: approvalIds={}", request.getApprovalIds());

        String operatorId = "user001"; // TODO: 从上下文获取当前用户
        String operatorName = "审批人"; // TODO: 从上下文获取当前用户
        String operator = "system"; // TODO: 从上下文获取当前用户

        BatchApprovalResponse data = approvalService.batchApprove(request, operatorId, operatorName, operator);
        return ApiResponse.success(data);
    }

    /**
     * #51 批量驳回审批
     * 
     * @param request 批量驳回请求
     * @return 批量驳回结果
     */
    @PostMapping("/approvals/batch-reject")
    public ApiResponse<BatchApprovalResponse> batchReject(
            @Valid @RequestBody BatchApprovalRequest request) {
        log.info("批量驳回审批: approvalIds={}", request.getApprovalIds());

        String operatorId = "user001"; // TODO: 从上下文获取当前用户
        String operatorName = "审批人"; // TODO: 从上下文获取当前用户
        String operator = "system"; // TODO: 从上下文获取当前用户

        BatchApprovalResponse data = approvalService.batchReject(request, operatorId, operatorName, operator);
        return ApiResponse.success(data);
    }
}
