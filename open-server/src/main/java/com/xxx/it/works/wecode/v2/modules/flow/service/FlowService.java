package com.xxx.it.works.wecode.v2.modules.flow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.flow.dto.*;
import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.FlowMapper;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.FlowVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 连接流管理服务
 * <p>
 * 实现连接流 CRUD (FR-009~012)、配置管理 (FR-016~017) 和 lifecycle (FR-014~015)
 * 接口编号: #8 ~ #16
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowService {

    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /** 生命周期状态常量 */
    public static final int LIFECYCLE_RUNNING = 1;
    public static final int LIFECYCLE_STOPPED = 2;

    private final FlowMapper flowMapper;
    private final FlowVersionMapper flowVersionMapper;
    private final IdGeneratorStrategy idGenerator;
    private final ObjectMapper objectMapper;

    // ==================== #8 创建连接流 ====================

    /**
     * API #8: POST /service/open/v2/flows
     * 创建后默认 lifecycle_status=1 running
     */
    @Transactional
    public ApiResponse<FlowCreateResponse> createFlow(FlowCreateRequest request) {
        log.info("Creating flow: nameCn={}, nameEn={}", request.getNameCn(), request.getNameEn());

        long flowId = idGenerator.nextId();
        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        Flow flow = new Flow();
        flow.setId(flowId);
        flow.setNameCn(request.getNameCn());
        flow.setNameEn(request.getNameEn());
        flow.setDescriptionCn(request.getDescriptionCn());
        flow.setDescriptionEn(request.getDescriptionEn());
        flow.setIconFileId(request.getIconFileId());
        flow.setLifecycleStatus(LIFECYCLE_RUNNING); // 创建后默认运行中
        flow.setCreateTime(now);
        flow.setLastUpdateTime(now);
        flow.setCreateBy(currentUser);
        flow.setLastUpdateBy(currentUser);

        flowMapper.insert(flow);

        log.info("Flow created: id={}", flowId);
        return ApiResponse.success(FlowCreateResponse.builder()
                .id(String.valueOf(flowId))
                .build());
    }

    // ==================== #9 列表查询 ====================

    /**
     * API #9: GET /service/open/v2/flows
     * 列表查询, 支持 lifecycleStatus 过滤 + keyword 搜索 + 分页
     */
    public ApiResponse<List<FlowListResponse>> getFlowList(FlowListRequest request) {
        int offset = (request.getCurPage() - 1) * request.getPageSize();

        List<Flow> flows = flowMapper.selectList(
                request.getLifecycleStatus(),
                request.getKeyword(),
                offset,
                request.getPageSize()
        );

        Long total = flowMapper.countList(
                request.getLifecycleStatus(),
                request.getKeyword()
        );

        List<FlowListResponse> items = flows.stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) total / request.getPageSize());

        return ApiResponse.success(items, ApiResponse.PageResponse.builder()
                .curPage(request.getCurPage())
                .pageSize(request.getPageSize())
                .total(total)
                .totalPages(totalPages)
                .build());
    }

    // ==================== #10 详情查询 ====================

    /**
     * API #10: GET /service/open/v2/flows/{flowId}
     */
    public ApiResponse<FlowDetailResponse> getFlowDetail(Long flowId) {
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }
        return ApiResponse.success(toDetailResponse(flow));
    }

    // ==================== #11 编辑基本信息 ====================

    /**
     * API #11: PUT /service/open/v2/flows/{flowId}
     */
    @Transactional
    public ApiResponse<Void> updateFlow(Long flowId, FlowUpdateRequest request) {
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        if (request.getNameCn() != null) flow.setNameCn(request.getNameCn());
        if (request.getNameEn() != null) flow.setNameEn(request.getNameEn());
        if (request.getDescriptionCn() != null) flow.setDescriptionCn(request.getDescriptionCn());
        if (request.getDescriptionEn() != null) flow.setDescriptionEn(request.getDescriptionEn());
        if (request.getIconFileId() != null) flow.setIconFileId(request.getIconFileId());

        flow.setLastUpdateTime(now);
        flow.setLastUpdateBy(currentUser);

        flowMapper.update(flow);
        log.info("Flow updated: id={}", flowId);
        return ApiResponse.success();
    }

    // ==================== #12 删除连接流 ====================

    /**
     * API #12: DELETE /service/open/v2/flows/{flowId}
     * 仅 stopped 状态可删除
     */
    @Transactional
    public ApiResponse<Void> deleteFlow(Long flowId) {
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        if (flow.getLifecycleStatus() != LIFECYCLE_STOPPED) {
            return ApiResponse.error("400", "仅已停止状态的连接流可删除", "Only stopped flows can be deleted");
        }

        // 级联删除版本记录
        flowVersionMapper.deleteByFlowId(flowId);
        // 删除连接流基本信息
        flowMapper.deleteById(flowId);

        log.info("Flow deleted: id={}", flowId);
        return ApiResponse.success();
    }

    // ==================== #13 启动 ====================

    /**
     * API #13: POST /service/open/v2/flows/{flowId}/start
     * 启动 (stopped→running)
     */
    @Transactional
    public ApiResponse<Void> startFlow(Long flowId) {
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        if (flow.getLifecycleStatus() == LIFECYCLE_RUNNING) {
            return ApiResponse.error("400", "连接流已处于运行中状态", "Flow is already running");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();
        flowMapper.updateLifecycleStatus(flowId, LIFECYCLE_RUNNING, now, currentUser);

        log.info("Flow started: id={}", flowId);
        return ApiResponse.success();
    }

    // ==================== #14 停止 ====================

    /**
     * API #14: POST /service/open/v2/flows/{flowId}/stop
     * 停止 (running→stopped)
     */
    @Transactional
    public ApiResponse<Void> stopFlow(Long flowId) {
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        if (flow.getLifecycleStatus() == LIFECYCLE_STOPPED) {
            return ApiResponse.error("400", "连接流已处于已停止状态", "Flow is already stopped");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();
        flowMapper.updateLifecycleStatus(flowId, LIFECYCLE_STOPPED, now, currentUser);

        log.info("Flow stopped: id={}", flowId);
        return ApiResponse.success();
    }

    // ==================== #15 查看编排配置 ====================

    /**
     * API #15: GET /service/open/v2/flows/{flowId}/config
     * 查看编排配置 (React Flow 格式：nodes/edges/trigger 完整 DAG)
     */
    public ApiResponse<FlowConfigResponse> getFlowConfig(Long flowId) {
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowVersion version = flowVersionMapper.selectByFlowId(flowId);
        if (version == null || version.getOrchestrationConfig() == null || version.getOrchestrationConfig().isEmpty()) {
            return ApiResponse.success(FlowConfigResponse.empty());
        }

        return ApiResponse.success(FlowConfigResponse.of(version.getOrchestrationConfig()));
    }

    // ==================== #16 保存编排配置 ====================

    /**
     * API #16: PUT /service/open/v2/flows/{flowId}/config
     * 保存编排配置 (React Flow 格式：nodes/edges/trigger，编辑即生效)
     * 编排校验: 检查 React Flow nodes 数组，无节点时拒绝保存
     */
    @Transactional
    public ApiResponse<Void> updateFlowConfig(Long flowId, FlowConfigUpdateRequest request) {
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        // 编排校验: 检查是否有节点
        try {
            JsonNode config = objectMapper.readTree(request.getOrchestrationConfig());
            JsonNode nodes = config.get("nodes");
            if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
                return ApiResponse.error("400", "编排配置至少需要一个节点", "At least one node is required");
            }
        } catch (Exception e) {
            return ApiResponse.error("400", "编排配置JSON格式错误", "Invalid orchestration config JSON format");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        FlowVersion version = flowVersionMapper.selectByFlowId(flowId);
        if (version == null) {
            // 首次配置 - 创建版本记录
            version = new FlowVersion();
            version.setId(idGenerator.nextId());
            version.setFlowId(flowId);
            version.setOrchestrationConfig(request.getOrchestrationConfig());
            version.setCreateTime(now);
            version.setLastUpdateTime(now);
            version.setCreateBy(currentUser);
            version.setLastUpdateBy(currentUser);
            flowVersionMapper.insert(version);
        } else {
            // 更新现有版本 - 全文替换
            version.setOrchestrationConfig(request.getOrchestrationConfig());
            version.setLastUpdateTime(now);
            version.setLastUpdateBy(currentUser);
            flowVersionMapper.update(version);
        }

        log.info("Flow config updated: flowId={}", flowId);
        return ApiResponse.success();
    }

    // ==================== 内部转换方法 ====================

    private FlowListResponse toListResponse(Flow f) {
        FlowListResponse r = new FlowListResponse();
        r.setId(String.valueOf(f.getId()));
        r.setNameCn(f.getNameCn());
        r.setNameEn(f.getNameEn());
        r.setDescriptionCn(f.getDescriptionCn());
        r.setDescriptionEn(f.getDescriptionEn());
        r.setLifecycleStatus(f.getLifecycleStatus());
        r.setCreateTime(formatIso(f.getCreateTime()));
        r.setLastUpdateTime(formatIso(f.getLastUpdateTime()));
        return r;
    }

    private FlowDetailResponse toDetailResponse(Flow f) {
        FlowDetailResponse r = new FlowDetailResponse();
        r.setId(String.valueOf(f.getId()));
        r.setNameCn(f.getNameCn());
        r.setNameEn(f.getNameEn());
        r.setDescriptionCn(f.getDescriptionCn());
        r.setDescriptionEn(f.getDescriptionEn());
        r.setIconFileId(f.getIconFileId());
        r.setLifecycleStatus(f.getLifecycleStatus());
        r.setCreateTime(formatIso(f.getCreateTime()));
        r.setLastUpdateTime(formatIso(f.getLastUpdateTime()));
        return r;
    }

    private String formatIso(Date date) {
        return date != null ? ISO_FORMAT.format(date) : null;
    }
}