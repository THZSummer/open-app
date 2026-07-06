package com.xxx.it.works.wecode.v2.modules.approvalflow.controller;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.PlatformAdminPermission;
import com.xxx.it.works.wecode.v2.modules.approvalflow.dto.*;
import com.xxx.it.works.wecode.v2.modules.approvalflow.service.ApprovalFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 审批流程模板管理 Controller
 *
 * <p>实现审批流程模板的 CRUD 接口：</p>
 * <ul>
 *   <li>#41 GET /service/open/v2/approval-flows - 返回审批流程模板列表</li>
 *   <li>#42 GET /service/open/v2/approval-flows/:id - 返回审批流程模板详情</li>
 *   <li>#43 POST /service/open/v2/approval-flows - 创建审批流程模板</li>
 *   <li>#44 PUT /service/open/v2/approval-flows/:id - 更新审批流程模板</li>
 *   <li>#45 DELETE /service/open/v2/approval-flows/:id - 删除审批流程模板</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2")
@RequiredArgsConstructor
public class ApprovalFlowController {

    private final ApprovalFlowService approvalFlowService;

    /**
     * #41 获取审批流程模板列表
     */
    @GetMapping("/approval-flows")
    @PlatformAdminPermission
    public ApiResponse<List<ApprovalFlowListResponse>> getFlowList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String appId,
            @RequestParam(defaultValue = "1") Integer curPage,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        log.info("Get approval flow list: keyword={}, appId={}, curPage={}, pageSize={}", keyword, appId, curPage, pageSize);

        ApprovalFlowListRequest request = new ApprovalFlowListRequest();
        request.setKeyword(keyword);
        request.setAppId(appId);
        request.setCurPage(curPage);
        request.setPageSize(pageSize);

        List<ApprovalFlowListResponse> data = approvalFlowService.getFlowList(request);
        Long total = approvalFlowService.countFlowList(keyword, appId);

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
     */
    @GetMapping("/approval-flows/{id}")
    @PlatformAdminPermission
    public ApiResponse<ApprovalFlowDetailResponse> getFlowDetail(@PathVariable String id) {
        log.info("Get approval flow detail: id={}", id);

        ApprovalFlowDetailResponse data = approvalFlowService.getFlowDetail(Long.parseLong(id));
        return ApiResponse.success(data);
    }

    /**
     * #43 创建审批流程模板
     */
    @PostMapping("/approval-flows")
    @PlatformAdminPermission
    public ApiResponse<ApprovalFlowDetailResponse> createFlow(
            @Valid @RequestBody ApprovalFlowCreateRequest request) {
        log.info("Create approval flow: code={}", request.getCode());

        String operator = UserContextHolder.getUserId();
        ApprovalFlowDetailResponse data = approvalFlowService.createFlow(request, operator);
        return ApiResponse.success(data);
    }

    /**
     * #44 更新审批流程模板
     */
    @PutMapping("/approval-flows/{id}")
    @PlatformAdminPermission
    public ApiResponse<ApprovalFlowDetailResponse> updateFlow(
            @PathVariable String id,
            @Valid @RequestBody ApprovalFlowUpdateRequest request) {
        log.info("Update approval flow: id={}", id);

        String operator = UserContextHolder.getUserId();
        ApprovalFlowDetailResponse data = approvalFlowService.updateFlow(Long.parseLong(id), request, operator);
        return ApiResponse.success(data);
    }

    /**
     * #45 删除审批流程模板
     */
    @DeleteMapping("/approval-flows/{id}")
    @PlatformAdminPermission
    public ApiResponse<Void> deleteFlow(@PathVariable String id) {
        log.info("Delete approval flow: id={}", id);

        String operator = UserContextHolder.getUserId();
        approvalFlowService.deleteFlow(Long.parseLong(id), operator);

        return ApiResponse.success(null);
    }
}
