package com.xxx.it.works.wecode.v2.modules.flow;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.flow.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 连接流版本管理 Controller（V3 多版本模型）
 * <p>
 * 连接流版本 CRUD + 发布流程 + 审批操作（API #28~#38）
 * V3 核心特性：
 * - 多版本模型：每连接流最多 1000 个版本
 * - 7 状态流转含审批中间态（待审批/已撤回/已驳回）
 * - 发布时统一校验（FR-026 全部 9 项）
 * - 保存时仅 DB 存储级校验（JSON 可解析即可）
 * - 支持撤回审批和催办操作
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/flows")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Tag(name = "连接流版本管理", description = "连接流版本 CRUD、发布审批及配置管理接口")
public class FlowVersionController {

    private final FlowVersionService flowVersionService;

    /**
     * #28 创建空草稿版本
     */
    @PostMapping("/{flowId}/versions")
    @Operation(summary = "#28 创建空草稿", description = "创建空草稿版本，版本上限 1000，已有草稿时返回 409")
    public ApiResponse<?> createDraft(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId) {
        log.info("POST /flows/{}/versions - create draft: appId={}", flowId, appId);
        return flowVersionService.createDraft(flowId, appId);
    }

    /**
     * #29 查询版本列表
     */
    @GetMapping("/{flowId}/versions")
    @Operation(summary = "#29 查询版本列表", description = "查询版本列表，支持 status 过滤，含 deployed 标记")
    public ApiResponse<List<FlowVersionListResponse>> getVersionList(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "版本状态过滤") @RequestParam(required = false) Integer status) {
        log.info("GET /flows/{}/versions - list: appId={}, status={}", flowId, appId, status);
        return flowVersionService.getVersionList(flowId, status, appId);
    }

    /**
     * #30 查询版本详情
     */
    @GetMapping("/{flowId}/versions/{versionId}")
    @Operation(summary = "#30 查询版本详情", description = "版本详情，含编排配置快照")
    public ApiResponse<FlowVersionDetailResponse> getVersionDetail(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("GET /flows/{}/versions/{} - detail: appId={}", flowId, versionId, appId);
        return flowVersionService.getVersionDetail(flowId, versionId, appId);
    }

    /**
     * #31 更新草稿版本
     */
    @PutMapping("/{flowId}/versions/{versionId}")
    @Operation(summary = "#31 更新草稿", description = "更新草稿编排配置（仅 DB 存储级校验：JSON 可解析即可）")
    public ApiResponse<?> updateDraft(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "版本ID") @PathVariable Long versionId,
            @RequestBody FlowVersionSaveRequest request) {
        log.info("PUT /flows/{}/versions/{} - update draft: appId={}", flowId, versionId, appId);
        return flowVersionService.updateDraft(flowId, versionId,
                request != null ? request.getOrchestrationConfig() : null, appId);
    }

    /**
     * #32 发布版本（提交审批）
     */
    @PostMapping("/{flowId}/versions/{versionId}/publish")
    @Operation(summary = "#32 发布版本", description = "发布版本：全部 9 项校验 → 提交审批（status → 2 PENDING_APPROVAL）")
    public ApiResponse<FlowPublishResponse> publish(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("POST /flows/{}/versions/{}/publish: appId={}", flowId, versionId, appId);
        return flowVersionService.publish(flowId, versionId, appId);
    }

    /**
     * #33 复制到草稿
     */
    @PostMapping("/{flowId}/versions/{versionId}/copy-to-draft")
    @Operation(summary = "#33 复制到草稿", description = "复制已有版本到新草稿，校验无待审批/已驳回/已撤回版本")
    public ApiResponse<?> copyToDraft(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "源版本ID") @PathVariable Long versionId) {
        log.info("POST /flows/{}/versions/{}/copy-to-draft: appId={}", flowId, versionId, appId);
        return flowVersionService.copyToDraft(flowId, versionId, appId);
    }

    /**
     * #34 失效版本
     */
    @PutMapping("/{flowId}/versions/{versionId}/invalidate")
    @Operation(summary = "#34 失效版本", description = "失效版本（校验未部署，status → 6 INVALIDATED）")
    public ApiResponse<?> invalidateVersion(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("PUT /flows/{}/versions/{}/invalidate: appId={}", flowId, versionId, appId);
        return flowVersionService.invalidateVersion(flowId, versionId, appId);
    }

    /**
     * #35 恢复版本
     */
    @PutMapping("/{flowId}/versions/{versionId}/recover")
    @Operation(summary = "#35 恢复版本", description = "恢复版本 → 已发布（status 6→5）")
    public ApiResponse<?> recoverVersion(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("PUT /flows/{}/versions/{}/recover: appId={}", flowId, versionId, appId);
        return flowVersionService.recoverVersion(flowId, versionId, appId);
    }

    /**
     * #36 删除版本
     */
    @DeleteMapping("/{flowId}/versions/{versionId}")
    @Operation(summary = "#36 删除版本", description = "删除版本（仅草稿或已失效状态可删除）")
    public ApiResponse<Void> deleteVersion(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("DELETE /flows/{}/versions/{}: appId={}", flowId, versionId, appId);
        return flowVersionService.deleteVersion(flowId, versionId, appId);
    }

    /**
     * #37 撤回审批
     */
    @PostMapping("/{flowId}/versions/{versionId}/cancel")
    @Operation(summary = "#37 撤回审批", description = "撤回审批：待审批 → 已撤回（status 2→3）")
    public ApiResponse<?> cancelApproval(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("POST /flows/{}/versions/{}/cancel: appId={}", flowId, versionId, appId);
        return flowVersionService.cancelApproval(flowId, versionId, appId);
    }

    /**
     * #38 催办审批
     */
    @PostMapping("/{flowId}/versions/{versionId}/urge")
    @Operation(summary = "#38 催办审批", description = "向当前审批级别审批人发送催办通知")
    public ApiResponse<?> urgeApproval(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("POST /flows/{}/versions/{}/urge: appId={}", flowId, versionId, appId);
        return flowVersionService.urgeApproval(flowId, versionId, appId);
    }
}
