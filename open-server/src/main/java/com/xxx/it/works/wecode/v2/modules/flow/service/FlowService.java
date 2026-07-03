package com.xxx.it.works.wecode.v2.modules.flow.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.FlowLifecycleStatus;
import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.flow.dto.*;
import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import com.xxx.it.works.wecode.v2.modules.flowversion.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowMapper;
import com.xxx.it.works.wecode.v2.modules.flowversion.mapper.OpFlowVersionMapper;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.ConnectorVersionRefMapper;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.OpConnectorVersionMapper;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersion;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersionRef;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.OpConnectorMapper;
import com.xxx.it.works.wecode.v2.modules.connector.entity.Connector;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorStatus;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorVersionStatus;
import com.xxx.it.works.wecode.v2.modules.security.AppContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 连接流管理服务（V3 多版本模型）
 * <p>
 * 连接流实体 CRUD + 生命周期管理 + 部署/启动/停止/复制
 * V3 变更：
 * - 创建时不自动生成草稿版本
 * - 生命周期状态流转使用 FlowLifecycleStatus.isValidTransition()
 * - 所有操作校验 appId 数据归属
 * - 支持失效/恢复生命周期操作
 * </p>
 */
@Slf4j
@Service
public class FlowService {




    @Autowired
    public FlowService(OpFlowMapper flowMapper, OpFlowVersionMapper flowVersionMapper,
                       ConnectorVersionRefMapper connectorVersionRefMapper,
                       OpConnectorVersionMapper connectorVersionMapper,
                       OpConnectorMapper connectorMapper,
                       IdGeneratorStrategy idGenerator) {
        this.flowMapper = flowMapper;
        this.flowVersionMapper = flowVersionMapper;
        this.connectorVersionRefMapper = connectorVersionRefMapper;
        this.connectorVersionMapper = connectorVersionMapper;
        this.connectorMapper = connectorMapper;
        this.idGenerator = idGenerator;
    }
    private final OpFlowMapper flowMapper;
    private final OpFlowVersionMapper flowVersionMapper;
    private final ConnectorVersionRefMapper connectorVersionRefMapper;
    private final OpConnectorVersionMapper connectorVersionMapper;
    private final OpConnectorMapper connectorMapper;
    private final IdGeneratorStrategy idGenerator;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String formatDate(Date date) {
        return DATE_FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    // ==================== #17 创建连接流 ====================

    /**
     * API #17: POST /service/open/v2/flows
     * 创建连接流基本信息，不自动生成草稿版本，lifecycleStatus=1 STOPPED
     */
    @Transactional
    public ApiResponse<FlowCreateResponse> createFlow(FlowCreateRequest request) {
        Long appId = AppContextHolder.requireInternalAppId();
        log.info("Creating flow: nameCn={}, nameEn={}, appId={}", request.getNameCn(), request.getNameEn(), appId);

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
        flow.setLifecycleStatus(FlowLifecycleStatus.STOPPED.getCode()); // 已停止
        flow.setAppId(appId);
        flow.setCreateTime(now);
        flow.setLastUpdateTime(now);
        flow.setCreateBy(currentUser);
        flow.setLastUpdateBy(currentUser);

        flowMapper.insert(flow);

        log.info("Flow created: id={}, appId={}", flowId, appId);

        FlowCreateResponse response = FlowCreateResponse.builder()
                .flowId(String.valueOf(flowId))
                .nameCn(request.getNameCn())
                .nameEn(request.getNameEn())
                .lifecycleStatus(FlowLifecycleStatus.STOPPED.getCode())
                .appId(String.valueOf(appId))
                .createTime(formatDate(now))
                .note("创建连接流后需手动创建草稿版本")
                .build();

        return ApiResponse.success(response);
    }

    // ==================== #18 列表查询 ====================

    /**
     * API #18: GET /service/open/v2/flows
     * 列表查询，支持 lifecycleStatus/keyword 过滤 + 分页，按 appId 隔离
     */
    public ApiResponse<List<FlowListResponse>> getFlowList(
            Integer lifecycleStatus, String keyword,
            Integer curPage, Integer pageSize) {

        Long appId = AppContextHolder.requireInternalAppId();
        int page = curPage != null ? curPage : 1;
        int size = pageSize != null ? pageSize : 20;
        int offset = (page - 1) * size;

        // v2.1.0: SQL 层按 app_id 过滤，数据库层实现应用数据隔离
        List<Flow> pageItems = flowMapper.selectList(lifecycleStatus, keyword, appId, offset, size);
        long total = flowMapper.countList(lifecycleStatus, keyword, appId);

        // 转换为响应 DTO
        List<FlowListResponse> items = new ArrayList<>();
        for (Flow f : pageItems) {
            FlowListResponse item = toListResponse(f);

            // 查询版本信息：草稿版本号
            List<FlowVersion> versions = flowVersionMapper.selectListByFlowId(f.getId(), null);
            if (versions != null) {
                for (FlowVersion v : versions) {
                    if (v.getStatus() != null && v.getStatus() == FlowVersionStatus.DRAFT.getCode()) {
                        item.setDraftVersionNumber(v.getVersionNumber());
                        break;
                    }
                }
            }

            items.add(item);
        }

        int totalPages = (int) Math.ceil((double) total / size);

        return ApiResponse.success(items, ApiResponse.PageResponse.builder()
                .curPage(page)
                .pageSize(size)
                .total(total)
                .totalPages(totalPages)
                .build());
    }

    // ==================== #19 详情查询 ====================

    /**
     * API #19: GET /service/open/v2/flows/{flowId}
     * 详情查询，含 invokeUrl
     */
    public ApiResponse<FlowDetailResponse> getFlowDetail(Long flowId, String invokeUrlPrefix) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowDetailResponse response = toDetailResponse(flow);

        // 构建 invokeUrl
        if (invokeUrlPrefix != null && !invokeUrlPrefix.isEmpty()) {
            response.setInvokeUrl(invokeUrlPrefix + "/api/v1/flows/" + flowId + "/invoke");
        } else {
            response.setInvokeUrl("/api/v1/flows/" + flowId + "/invoke");
        }

        // 查询版本信息
        List<FlowVersion> versions = flowVersionMapper.selectListByFlowId(flowId, null);
        if (versions != null) {
            for (FlowVersion v : versions) {
                if (v.getStatus() != null && v.getStatus() == FlowVersionStatus.PUBLISHED.getCode()) {
                    if (response.getLatestPublishedVersionNumber() == null
                            || v.getVersionNumber() > response.getLatestPublishedVersionNumber()) {
                        response.setLatestPublishedVersionNumber(v.getVersionNumber());
                    }
                }
                if (v.getStatus() != null && v.getStatus() == FlowVersionStatus.DRAFT.getCode()) {
                    response.setDraftVersionNumber(v.getVersionNumber());
                }
            }
        }

        return ApiResponse.success(response);
    }

    // ==================== #20 编辑基本信息 ====================

    /**
     * API #20: PUT /service/open/v2/flows/{flowId}
     * 更新连接流基本信息
     */
    @Transactional
    public ApiResponse<Void> updateFlow(Long flowId, FlowUpdateRequest request) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        if (request.getNameCn() != null) {
            flow.setNameCn(request.getNameCn());
        }
        if (request.getNameEn() != null) {
            flow.setNameEn(request.getNameEn());
        }
        if (request.getDescriptionCn() != null) {
            flow.setDescriptionCn(request.getDescriptionCn());
        }
        if (request.getDescriptionEn() != null) {
            flow.setDescriptionEn(request.getDescriptionEn());
        }
        if (request.getIconFileId() != null) {
            flow.setIconFileId(request.getIconFileId());
        }

        flow.setLastUpdateTime(now);
        flow.setLastUpdateBy(currentUser);

        flowMapper.update(flow);
        log.info("Flow updated: id={}, appId={}", flowId, appId);
        return ApiResponse.success();
    }

    // ==================== #22 部署（委托给 FlowDeployService） ====================

    // 部署由 FlowDeployService 处理，从 FlowController 调用

    // ==================== #23 启动 ====================

    /**
     * API #23: POST /service/open/v2/flows/{flowId}/start
     * 启动连接流，必须有已部署版本
     */
    @Transactional
    public ApiResponse<FlowLifecycleResponse> startFlow(Long flowId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        // 必须有已部署版本
        if (flow.getDeployedVersionId() == null) {
            return ApiResponse.error("422",
                    "连接流尚未部署版本，请先部署一个版本",
                    "Flow has no deployed version, deploy a version first");
        }

        // 校验已部署版本状态（防止版本被失效后仍能启动）
        FlowVersion deployedVersion = flowVersionMapper.selectById(flow.getDeployedVersionId());
        if (deployedVersion == null) {
            return ApiResponse.error("422",
                    "已部署版本不存在，请重新部署有效版本后再启动",
                    "Deployed version not found, please redeploy");
        }
        if (deployedVersion.getStatus() == null
                || deployedVersion.getStatus() != FlowVersionStatus.PUBLISHED.getCode()) {
            return ApiResponse.error("422",
                    "已部署版本（版本" + deployedVersion.getVersionNumber() + "）已失效，请重新部署有效版本后再启动",
                    "Deployed version is invalidated, please redeploy");
        }

        // 校验引用的连接器版本及其所属连接器状态
        List<ConnectorVersionRef> refs = connectorVersionRefMapper.selectByFlowVersionId(deployedVersion.getId());
        if (refs != null) {
            for (ConnectorVersionRef ref : refs) {
                ConnectorVersion cv = connectorVersionMapper.selectById(ref.getConnectorVersionId());
                if (cv == null || cv.getStatus() == null
                        || cv.getStatus() != ConnectorVersionStatus.PUBLISHED.getCode()) {
                    return ApiResponse.error("422",
                            "引用的连接器版本（版本" + (cv != null ? cv.getVersionNumber() : ref.getConnectorVersionId()) + "）已失效，请重新编排部署后再启动",
                            "Referenced connector version is invalidated");
                }
                Connector connector = connectorMapper.selectById(ref.getConnectorId());
                if (connector == null || connector.getStatus() == null
                        || connector.getStatus() == ConnectorStatus.INVALIDATED.getCode()) {
                    return ApiResponse.error("422",
                            "连接器（" + (connector != null ? connector.getNameCn() : ref.getConnectorId()) + "）已失效，请重新编排部署后再启动",
                            "Referenced connector is invalidated");
                }
            }
        }

        FlowLifecycleStatus currentStatus = FlowLifecycleStatus.fromValue(flow.getLifecycleStatus());
        if (!FlowLifecycleStatus.isValidTransition(currentStatus, FlowLifecycleStatus.RUNNING)) {
            return ApiResponse.error("409",
                    "非已停止状态，不可启动",
                    "Only stopped flows can be started");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();
        flowMapper.updateLifecycleStatus(flowId, FlowLifecycleStatus.RUNNING.getCode(), now, currentUser);

        log.info("Flow started: id={}, appId={}", flowId, appId);

        FlowLifecycleResponse response = FlowLifecycleResponse.builder()
                .flowId(String.valueOf(flowId))
                .lifecycleStatus(FlowLifecycleStatus.RUNNING.getCode())
                .lastUpdateTime(formatDate(now))
                .build();

        return ApiResponse.success(response);
    }

    // ==================== #24 停止 ====================

    /**
     * API #24: POST /service/open/v2/flows/{flowId}/stop
     * 停止连接流
     */
    @Transactional
    public ApiResponse<FlowLifecycleResponse> stopFlow(Long flowId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowLifecycleStatus currentStatus = FlowLifecycleStatus.fromValue(flow.getLifecycleStatus());
        if (currentStatus != FlowLifecycleStatus.RUNNING) {
            return ApiResponse.error("409",
                    "仅运行中状态可停止",
                    "Only running flows can be stopped");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();
        flowMapper.updateLifecycleStatus(flowId, FlowLifecycleStatus.STOPPED.getCode(), now, currentUser);

        log.info("Flow stopped: id={}, appId={}", flowId, appId);

        FlowLifecycleResponse response = FlowLifecycleResponse.builder()
                .flowId(String.valueOf(flowId))
                .lifecycleStatus(FlowLifecycleStatus.STOPPED.getCode())
                .lastUpdateTime(formatDate(now))
                .build();

        return ApiResponse.success(response);
    }

    // ==================== #25 失效 ====================

    /**
     * API #25: PUT /service/open/v2/flows/{flowId}/invalidate
     * 标记失效，仅已停止状态可失效
     */
    @Transactional
    public ApiResponse<?> invalidateFlow(Long flowId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowLifecycleStatus currentStatus = FlowLifecycleStatus.fromValue(flow.getLifecycleStatus());
        if (!FlowLifecycleStatus.isValidTransition(currentStatus, FlowLifecycleStatus.INVALIDATED)) {
            return ApiResponse.error("409",
                    "仅已停止状态可标记失效",
                    "Only stopped flows can be invalidated");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();
        flowMapper.updateLifecycleStatus(flowId, FlowLifecycleStatus.INVALIDATED.getCode(), now, currentUser);

        log.info("Flow invalidated: id={}, appId={}", flowId, appId);
        return ApiResponse.success();
    }

    // ==================== #26 恢复 ====================

    /**
     * API #26: PUT /service/open/v2/flows/{flowId}/recover
     * 恢复连接流 → 已停止状态
     */
    @Transactional
    public ApiResponse<?> recoverFlow(Long flowId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowLifecycleStatus currentStatus = FlowLifecycleStatus.fromValue(flow.getLifecycleStatus());
        if (currentStatus != FlowLifecycleStatus.INVALIDATED) {
            return ApiResponse.error("409",
                    "仅已失效状态可恢复",
                    "Only invalidated flows can be recovered");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();
        flowMapper.updateLifecycleStatus(flowId, FlowLifecycleStatus.STOPPED.getCode(), now, currentUser);

        log.info("Flow recovered: id={}, status=STOPPED, appId={}", flowId, appId);
        return ApiResponse.success();
    }

    // ==================== #27 删除 ====================

    /**
     * API #27: DELETE /service/open/v2/flows/{flowId}
     * 删除连接流，仅已失效状态可删除
     */
    @Transactional
    public ApiResponse<Void> deleteFlow(Long flowId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowLifecycleStatus currentStatus = FlowLifecycleStatus.fromValue(flow.getLifecycleStatus());
        if (currentStatus != FlowLifecycleStatus.INVALIDATED) {
            return ApiResponse.error("409",
                    "仅已失效状态的连接流可删除",
                    "Only invalidated flows can be deleted");
        }

        // 级联删除连接器版本引用记录
        connectorVersionRefMapper.deleteByFlowId(flowId);
        // 级联删除版本记录
        flowVersionMapper.deleteByFlowId(flowId);
        // 删除连接流基本信息
        flowMapper.deleteById(flowId);

        evictFlowConfigCache(flowId);

        log.info("Flow deleted: id={}, appId={}", flowId, appId);
        return ApiResponse.success();
    }

    // ==================== 缓存失效 ====================

    /**
     * 使 cp:flow:config:{flowId} 缓存失效, 避免缓存返回已删除版本的旧数据.
     */
    private void evictFlowConfigCache(Long flowId) {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.delete("cp:flow:config:" + flowId);
            // 删除 FlowEntity 缓存 (含 lifecycle_status/deployed_version_id，状态变更后需失效)
            stringRedisTemplate.delete("cp:entity:flow:" + flowId);
            log.debug("Evicted flow config cache: flowId={}", flowId);
        } catch (Exception e) {
            log.warn("Failed to evict flow config cache: flowId={}, error={}", flowId, e.getMessage());
        }
    }

    // ==================== 内部转换方法 ====================

    private FlowListResponse toListResponse(Flow f) {
        FlowListResponse r = new FlowListResponse();
        r.setId(String.valueOf(f.getId()));
        r.setNameCn(f.getNameCn());
        r.setNameEn(f.getNameEn());
        r.setDescriptionCn(f.getDescriptionCn());
        r.setDescriptionEn(f.getDescriptionEn());
        r.setIconFileId(f.getIconFileId());
        r.setLifecycleStatus(f.getLifecycleStatus());
        r.setDeployedVersionId(f.getDeployedVersionId() != null ? String.valueOf(f.getDeployedVersionId()) : null);
        r.setDeployedVersionNumber(f.getDeployedVersionNumber());
        r.setAppId(f.getAppId() != null ? String.valueOf(f.getAppId()) : null);
        r.setCreateTime(f.getCreateTime());
        r.setLastUpdateTime(f.getLastUpdateTime());
        r.setCreateBy(f.getCreateBy());
        r.setLastUpdateBy(f.getLastUpdateBy());
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
        r.setDeployedVersionId(f.getDeployedVersionId() != null ? String.valueOf(f.getDeployedVersionId()) : null);
        r.setDeployedVersionNumber(f.getDeployedVersionNumber());
        r.setAppId(f.getAppId() != null ? String.valueOf(f.getAppId()) : null);
        r.setCreateTime(f.getCreateTime());
        r.setLastUpdateTime(f.getLastUpdateTime());
        r.setCreateBy(f.getCreateBy());
        r.setLastUpdateBy(f.getLastUpdateBy());
        return r;
    }
}
