package com.xxx.api.approval.controller;

import com.xxx.api.approval.dto.ApprovalCallbackRequest;
import com.xxx.api.approval.dto.ApprovalCallbackResponse;
import com.xxx.api.approval.service.ApprovalCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 审批卡片回调 Controller
 *
 * <p>接收 IM 平台的审批卡片回调请求，处理审批通过/驳回操作。
 * 响应体使用自定义格式 ApprovalCallbackResponse（非 ApiResponse）。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Slf4j
public class ApprovalCallbackController {

    private final ApprovalCallbackService approvalCallbackService;

    /**
     * 处理审批卡片回调
     *
     * @param businessId   业务ID（对应审批记录的 business_id）
     * @param businessType 业务类型（如 api_permission_apply）
     * @param request      回调请求体
     * @return 自定义回调响应
     */
    @PostMapping("/callback")
    public ApprovalCallbackResponse<?> handleCallback(
            @RequestParam Long businessId,
            @RequestParam String businessType,
            @RequestBody ApprovalCallbackRequest request) {
        log.info("Approval callback: businessId={}, businessType={}, accountId={}, cardId={}",
                businessId, businessType, request.getAccountId(), request.getCardId());
        return approvalCallbackService.handleCallback(businessId, businessType, request);
    }
}
