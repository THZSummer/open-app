package com.xxx.it.works.wecode.v2.modules.approval.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.AuthRole;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalListRequest;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalProcessRequest;
import com.xxx.it.works.wecode.v2.modules.approval.service.ApprovalService;
import com.xxx.it.works.wecode.v2.modules.approval.vo.ApprovalListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/service/open/v2/apps")
@Tag(name = "审批管理", description = "通用审批管理接口")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @GetMapping("/pending")
    @AuthRole
    @Operation(summary = "查询待审批应用列表")
    public ApiResponse<List<ApprovalListVo>> getPendingList(
            @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        log.info("Get pending list, curPage={}, pageSize={}", curPage, pageSize);
        ApprovalListRequest request = new ApprovalListRequest();
        request.setCurPage(curPage);
        request.setPageSize(pageSize);
        return approvalService.getPendingList(request);
    }

    @GetMapping("/publish")
    @AuthRole
    @Operation(summary = "查询已上架应用列表")
    public ApiResponse<List<ApprovalListVo>> getPublishedList(
            @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        log.info("Get published list, curPage={}, pageSize={}", curPage, pageSize);
        ApprovalListRequest request = new ApprovalListRequest();
        request.setCurPage(curPage);
        request.setPageSize(pageSize);
        return approvalService.getPublishedList(request);
    }

    @PostMapping("/approval")
    @AuthRole
    @Operation(summary = "审批操作（通过/驳回）")
    public ApiResponse<Void> processApproval(
            @Valid @RequestBody ApprovalProcessRequest request) {
        log.info("Process approval, id={}, action={}", request.getId(), request.getAction());
        return approvalService.processApproval(request);
    }
}
