package com.xxx.it.works.wecode.v2.modules.flow.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.PlatformAdminPermission;
import com.xxx.it.works.wecode.v2.modules.flow.dto.*;
import com.xxx.it.works.wecode.v2.modules.flow.service.FlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 连接流管理 Controller
 * <p>
 * 连接流 CRUD (FR-009~012)、配置管理 (FR-016~017) 和 lifecycle (FR-014~015)
 * 接口编号: #8 ~ #16
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/flows")
@RequiredArgsConstructor
@Tag(name = "连接流管理", description = "连接流 CRUD、编排配置及生命周期管理接口")
public class FlowController {

    private final FlowService flowService;

    /**
     * #8 创建连接流
     */
    @PostMapping
    @PlatformAdminPermission
    @Operation(summary = "#8 创建连接流",
               description = "创建后默认 lifecycle_status=1 running，自动创建 flow_version 记录")
    public ApiResponse<FlowCreateResponse> createFlow(
            @Valid @RequestBody FlowCreateRequest request) {
        log.info("POST /api/v1/flows - create flow: nameCn={}", request.getNameCn());
        return flowService.createFlow(request);
    }

    /**
     * #9 获取连接流列表
     */
    @GetMapping
    @PlatformAdminPermission
    @Operation(summary = "#9 获取连接流列表",
               description = "列表查询，支持 lifecycleStatus 过滤 + keyword 搜索 + 分页")
    public ApiResponse<List<FlowListResponse>> getFlowList(
            @Parameter(description = "生命周期状态过滤")
            @RequestParam(required = false) Integer lifecycleStatus,
            @Parameter(description = "搜索关键词")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码")
            @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量")
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        FlowListRequest request = new FlowListRequest();
        request.setLifecycleStatus(lifecycleStatus);
        request.setKeyword(keyword);
        request.setCurPage(curPage);
        request.setPageSize(pageSize);

        return flowService.getFlowList(request);
    }

    /**
     * #10 获取连接流详情
     */
    @GetMapping("/{flowId}")
    @PlatformAdminPermission
    @Operation(summary = "#10 获取连接流详情",
               description = "含 lifecycleStatus")
    public ApiResponse<FlowDetailResponse> getFlowDetail(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId) {
        return flowService.getFlowDetail(flowId);
    }

    /**
     * #11 编辑连接流基本信息
     */
    @PutMapping("/{flowId}")
    @PlatformAdminPermission
    @Operation(summary = "#11 编辑连接流基本信息",
               description = "直接更新字段")
    public ApiResponse<Void> updateFlow(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId,
            @Valid @RequestBody FlowUpdateRequest request) {
        return flowService.updateFlow(flowId, request);
    }

    /**
     * #12 删除连接流
     */
    @DeleteMapping("/{flowId}")
    @PlatformAdminPermission
    @Operation(summary = "#12 删除连接流",
               description = "仅 stopped 状态可删除，级联删除关联记录")
    public ApiResponse<Void> deleteFlow(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId) {
        return flowService.deleteFlow(flowId);
    }

    /**
     * #13 启动连接流
     */
    @PostMapping("/{flowId}/start")
    @PlatformAdminPermission
    @Operation(summary = "#13 启动连接流",
               description = "启动 (stopped→running)")
    public ApiResponse<Void> startFlow(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId) {
        return flowService.startFlow(flowId);
    }

    /**
     * #14 停止连接流
     */
    @PostMapping("/{flowId}/stop")
    @PlatformAdminPermission
    @Operation(summary = "#14 停止连接流",
               description = "停止 (running→stopped)")
    public ApiResponse<Void> stopFlow(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId) {
        return flowService.stopFlow(flowId);
    }

    /**
     * #15 查看编排配置
     */
    @GetMapping("/{flowId}/config")
    @PlatformAdminPermission
    @Operation(summary = "#15 查看编排配置",
               description = "查看编排配置（含 trigger/nodes/edges 完整 DAG）")
    public ApiResponse<FlowConfigResponse> getFlowConfig(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId) {
        return flowService.getFlowConfig(flowId);
    }

    /**
     * #16 保存编排配置
     */
    @PutMapping("/{flowId}/config")
    @PlatformAdminPermission
    @Operation(summary = "#16 保存编排配置",
               description = "编辑即生效，含 HTTP trigger 配置/连接器节点/数据处理节点/edges")
    public ApiResponse<Void> updateFlowConfig(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId,
            @Valid @RequestBody FlowConfigUpdateRequest request) {
        return flowService.updateFlowConfig(flowId, request);
    }
}