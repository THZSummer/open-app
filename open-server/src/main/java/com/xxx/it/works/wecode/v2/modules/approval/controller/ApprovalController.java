package com.xxx.it.works.wecode.v2.modules.approval.controller;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.approval.dto.*;
import com.xxx.it.works.wecode.v2.modules.approval.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 审批执行管理 Controller
 *
 * <p>实现审批执行相关的接口：</p>
 * <ul>
 *   <li>#46 GET /service/open/v2/approvals/pending - 返回待审批列表</li>
 *   <li>#47 GET /service/open/v2/approvals/:id - 返回审批详情</li>
 *   <li>#48 POST /service/open/v2/approvals/:id/approve - 同意审批</li>
 *   <li>#49 POST /service/open/v2/approvals/:id/reject - 驳回审批</li>
 *   <li>#50 POST /service/open/v2/approvals/:id/cancel - 撤销审批</li>
 *   <li>#51 POST /service/open/v2/approvals/batch-approve - 批量同意审批</li>
 *   <li>#52 POST /service/open/v2/approvals/batch-reject - 批量驳回审批</li>
 *   <li>#53 POST /service/open/v2/approvals/:id/urge - 催办审批</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    // ==================== 审批执行管理 (#46-52) ====================

    /**
     * #46 获取待审批列表
     *
     * @param type 审批类型
     * @param businessType 业务类型（V3 新增，如 connector_flow_version_publish）
     * @param keyword 搜索关键词
     * @param status 审批状态
     * @param applicantId 申请人ID
     * @param approverId 审批人ID
     * @param curPage 当前页码
     * @param pageSize 每页数量
     * @return 待审批列表
     */
    @GetMapping("/approvals/pending")
    public ApiResponse<List<ApprovalPendingListResponse>> getPendingList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String applicantId,
            @RequestParam(required = false) String approverId,
            @RequestParam(defaultValue = "1") Integer curPage,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        log.info("Get pending approval list: type={}, businessType={}, keyword={}, status={}, applicantId={}, approverId={}, curPage={}, pageSize={}",
                type, businessType, keyword, status, applicantId, approverId, curPage, pageSize);

        ApprovalPendingListRequest request = new ApprovalPendingListRequest();
        // V3: 如果传了businessType参数，则用businessType作为type（精确匹配业务类型）
        if (businessType != null && !businessType.isEmpty()) {
            request.setType(businessType);
        } else {
            request.setType(type);
        }
        request.setKeyword(keyword);
        request.setStatus(status);

        // 处理 applicantId=current 特殊值，获取当前用户ID
        if ("current".equals(applicantId)) {
            request.setApplicantId(UserContextHolder.getUserId());
        } else {
            request.setApplicantId(applicantId);
        }


        // 处理 approverId=current 特殊值
        String actualApproverId = "current".equals(approverId) ? UserContextHolder.getUserId() : approverId;
        request.setApproverId(actualApproverId);
        request.setCurPage(curPage);
        request.setPageSize(pageSize);

        List<ApprovalPendingListResponse> data = approvalService.getPendingList(request);
        String actualApplicantId = "current".equals(applicantId) ? UserContextHolder.getUserId() : applicantId;
        Long total = approvalService.countPendingList(type, keyword, status, actualApplicantId, actualApproverId);

        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(curPage)
                .pageSize(pageSize)
                .total(total)
                .totalPages((int) Math.ceil((double) total / pageSize))
                .build();

        return ApiResponse.success(data, page);
    }

    /**
     * #47 获取审批详情
     *
     * @param id 审批记录ID
     * @return 审批详情
     */
    @GetMapping("/approvals/{id}")
    public ApiResponse<ApprovalDetailResponse> getApprovalDetail(@PathVariable String id) {
        log.info("Get approval detail: id={}", id);

        ApprovalDetailResponse data = approvalService.getApprovalDetail(Long.parseLong(id));
        return ApiResponse.success(data);
    }

    /**
     * #48 同意审批
     *
     * @param id 审批记录ID
     * @param request 审批请求
     * @return 审批操作结果
     */
    @PostMapping("/approvals/{id}/approve")
    public ApiResponse<ApprovalActionResponse> approve(
            @PathVariable String id,
            @RequestBody ApprovalActionRequest request) {
        log.info("Approve: id={}", id);

        String operatorId = UserContextHolder.getUserId();
        String operatorName = UserContextHolder.getUserName();
        String operator = UserContextHolder.getUserId();

        ApprovalActionResponse data = approvalService.approve(
                Long.parseLong(id), request, operatorId, operatorName, operator);
        return ApiResponse.success(data);
    }

    /**
     * #49 驳回审批
     *
     * @param id 审批记录ID
     * @param request 驳回请求
     * @return 审批操作结果
     */
    @PostMapping("/approvals/{id}/reject")
    public ApiResponse<ApprovalActionResponse> reject(
            @PathVariable String id,
            @Valid @RequestBody ApprovalActionRequest request) {
        log.info("Reject: id={}", id);

        String operatorId = UserContextHolder.getUserId();
        String operatorName = UserContextHolder.getUserName();
        String operator = UserContextHolder.getUserId();

        ApprovalActionResponse data = approvalService.reject(
                Long.parseLong(id), request, operatorId, operatorName, operator);
        return ApiResponse.success(data);
    }

    /**
     * #50 撤销审批
     *
     * @param id 审批记录ID
     * @return 审批操作结果
     */
    @PostMapping("/approvals/{id}/cancel")
    public ApiResponse<ApprovalActionResponse> cancel(@PathVariable String id) {
        log.info("Cancel approval: id={}", id);

        String operator = UserContextHolder.getUserId();

        ApprovalActionResponse data = approvalService.cancel(Long.parseLong(id), operator);
        return ApiResponse.success(data);
    }

    /**
     * #51 批量同意审批
     *
     * @param request 批量审批请求
     * @return 批量审批结果
     */
    @PostMapping("/approvals/batch-approve")
    public ApiResponse<BatchApprovalResponse> batchApprove(
            @Valid @RequestBody BatchApprovalRequest request) {
        log.info("Batch approve: approvalIds={}", request.getApprovalIds());

        String operatorId = UserContextHolder.getUserId();
        String operatorName = UserContextHolder.getUserName();
        String operator = UserContextHolder.getUserId();

        BatchApprovalResponse data = approvalService.batchApprove(request, operatorId, operatorName, operator);
        return ApiResponse.success(data);
    }

    /**
     * #52 批量驳回审批
     *
     * @param request 批量驳回请求
     * @return 批量驳回结果
     */
    @PostMapping("/approvals/batch-reject")
    public ApiResponse<BatchApprovalResponse> batchReject(
            @Valid @RequestBody BatchApprovalRequest request) {
        log.info("Batch reject: approvalIds={}", request.getApprovalIds());

        String operatorId = UserContextHolder.getUserId();
        String operatorName = UserContextHolder.getUserName();
        String operator = UserContextHolder.getUserId();

        BatchApprovalResponse data = approvalService.batchReject(request, operatorId, operatorName, operator);
        return ApiResponse.success(data);
    }

    // ==================== 催办 (#53) ====================

    /**
     * #53 催办审批
     *
     * <p>申请人催办当前审批人，发送卡片消息通知。
     * 根据 businessId + businessType 查询最新待审批记录（命中 idx_business 索引）。</p>
     *
     * @param id      路径参数（保持不变）
     * @param request 请求体（businessId + businessType）
     * @return 催办结果
     */
    @PostMapping("/approvals/{id}/urge")
    public ApiResponse<ApprovalActionResponse> urge(
            @PathVariable String id,
            @RequestBody UrgeRequest request) {
        log.info("Urge approval: id={}, businessId={}, businessType={}", id, request.getBusinessId(), request.getBusinessType());

        String operator = UserContextHolder.getUserId();
        ApprovalActionResponse data = approvalService.urge(request.getBusinessId(), request.getBusinessType(), operator);
        return ApiResponse.success(data);
    }
}
