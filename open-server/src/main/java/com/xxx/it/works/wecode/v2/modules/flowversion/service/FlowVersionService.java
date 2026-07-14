package com.xxx.it.works.wecode.v2.modules.flowversion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.ConnectorPlatformPropertyService;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorPlatformConstants;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.FlowLifecycleStatus;
import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalActionResponse;
import com.xxx.it.works.wecode.v2.modules.flow.model.NodeTypeResolver;
import com.xxx.it.works.wecode.v2.modules.approval.FlowVersionApprovalService;
import com.xxx.it.works.wecode.v2.modules.approval.service.ApprovalService;
import com.fasterxml.jackson.databind.JsonNode;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalLog;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalLogMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import com.xxx.it.works.wecode.v2.modules.auditlog.entity.OperateLog;
import com.xxx.it.works.wecode.v2.modules.auditlog.service.AuditLogService;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersionRef;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.ConnectorVersionRefMapper;
import com.xxx.it.works.wecode.v2.modules.flow.dto.*;
import com.xxx.it.works.wecode.v2.modules.flowversion.dto.FlowVersionDetailResponse;
import com.xxx.it.works.wecode.v2.modules.flowversion.dto.FlowVersionListResponse;
import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import com.xxx.it.works.wecode.v2.modules.flowversion.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowMapper;
import com.xxx.it.works.wecode.v2.modules.flow.service.FlowCacheEvictor;
import com.xxx.it.works.wecode.v2.modules.flowversion.mapper.OpFlowVersionMapper;
import com.xxx.it.works.wecode.v2.modules.flow.validator.FlowPublishValidator;
import com.xxx.it.works.wecode.v2.modules.security.AppContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 连接流版本管理服务（V3 多版本模型）
 * <p>
 * 连接流版本 CRUD + 发布流程 + 审批操作
 * V3 核心特性：
 * - 多版本模型：每连接流最多 1000 个版本
 * - 7 状态流转含审批中间态（待审批/已撤回/已驳回）
 * - 发布时统一校验（FR-026 全部 9 项）
 * - 保存时仅 DB 存储级校验（JSON 可解析即可）
 * </p>
 */
@Slf4j
@Service
public class FlowVersionService {

    @Autowired
    public FlowVersionService(OpFlowMapper flowMapper, OpFlowVersionMapper flowVersionMapper, ConnectorVersionRefMapper connectorVersionRefMapper, IdGeneratorStrategy idGenerator, ObjectMapper objectMapper, FlowPublishValidator publishValidator, FlowVersionApprovalService approvalService, ApprovalService genericApprovalService, AuditLogService auditLogService, ConnectorPlatformPropertyService propertyService, ApprovalRecordMapper approvalRecordMapper, ApprovalLogMapper approvalLogMapper) {
        this.flowMapper = flowMapper;
        this.flowVersionMapper = flowVersionMapper;
        this.connectorVersionRefMapper = connectorVersionRefMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.publishValidator = publishValidator;
        this.approvalService = approvalService;
        this.genericApprovalService = genericApprovalService;
        this.auditLogService = auditLogService;
        this.propertyService = propertyService;
        this.approvalRecordMapper = approvalRecordMapper;
        this.approvalLogMapper = approvalLogMapper;
    }
    private final OpFlowMapper flowMapper;
    private final OpFlowVersionMapper flowVersionMapper;
    private final ConnectorVersionRefMapper connectorVersionRefMapper;
    private final IdGeneratorStrategy idGenerator;
    private final ObjectMapper objectMapper;
    private final FlowPublishValidator publishValidator;
    private final FlowVersionApprovalService approvalService;
    private final ApprovalService genericApprovalService;
    private final AuditLogService auditLogService;
    private final ConnectorPlatformPropertyService propertyService;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalLogMapper approvalLogMapper;

    @Autowired
    private FlowCacheEvictor flowCacheEvictor;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${platform.approval-url-prefix}")
    private String approvalUrlPrefix;

    // ==================== #28 创建草稿 ====================

    /**
     * API #28: POST /service/open/v2/flows/{flowId}/versions
     * 创建空草稿，需版本上限校验
     */
    @Transactional
    public ApiResponse<?> createDraft(Long flowId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        // 版本上限校验
        int maxVersionCount = propertyService.getFlowMaxVersions();
        List<FlowVersion> existingVersions = flowVersionMapper.selectListByFlowId(flowId, null);
        if (existingVersions != null && existingVersions.size() >= maxVersionCount) {
            return ApiResponse.error("422",
                    "版本数量已达上限 " + maxVersionCount,
                    "Version count exceeds limit " + maxVersionCount);
        }

        // 检查是否已有草稿
        boolean hasDraft = existingVersions != null && existingVersions.stream()
                .anyMatch(v -> v.getStatus() != null && v.getStatus().equals(FlowVersionStatus.DRAFT.getCode()));
        if (hasDraft) {
            return ApiResponse.error("409",
                    "已存在草稿版本，请先编辑或删除当前草稿",
                    "A draft version already exists, edit or delete it first");
        }

        // 生成版本号
        int versionNumber = 1;
        Integer maxVersion = flowVersionMapper.selectMaxVersionNumberByFlowId(flowId);
        if (maxVersion != null && maxVersion > 0) {
            versionNumber = maxVersion + 1;
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        FlowVersion version = new FlowVersion();
        version.setId(idGenerator.nextId());
        version.setFlowId(flowId);
        version.setVersionNumber(versionNumber);
        version.setStatus(FlowVersionStatus.DRAFT.getCode());
        version.setOrchestrationConfig(null);
        version.setCreateTime(now);
        version.setLastUpdateTime(now);
        version.setCreateBy(currentUser);
        version.setLastUpdateBy(currentUser);

        flowVersionMapper.insert(version);

        log.info("Draft version created: flowId={}, versionId={}, versionNumber={}",
                flowId, version.getId(), versionNumber);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", version.getId());
        data.put("versionId", version.getId());
        data.put("versionNumber", versionNumber);
        return ApiResponse.success(data);
    }

    // ==================== #29 版本列表 ====================

    /**
     * API #29: GET /service/open/v2/flows/{flowId}/versions
     * 查询版本列表，支持 status 过滤，含 deployed 标记
     */
    public ApiResponse<List<FlowVersionListResponse>> getVersionList(Long flowId, Integer status) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        List<FlowVersion> versions = flowVersionMapper.selectListByFlowId(flowId, status);
        List<FlowVersionListResponse> items = new ArrayList<>();

        Long deployedVersionId = flow.getDeployedVersionId();

        if (versions != null) {
            for (FlowVersion v : versions) {
                FlowVersionListResponse item = new FlowVersionListResponse();
                item.setVersionId(String.valueOf(v.getId()));
                item.setVersionNumber(v.getVersionNumber());
                item.setStatus(v.getStatus());
                item.setPublishedTime(formatDate(v.getPublishedTime()));
                item.setPublishedBy(v.getPublishedBy());
                item.setCreateTime(formatDate(v.getCreateTime()));
                item.setCreateBy(v.getCreateBy());

                // 设置 deployed 标记
                item.setDeployed(deployedVersionId != null && deployedVersionId.equals(v.getId()));

                items.add(item);
            }
        }

        return ApiResponse.success(items);
    }

    // ==================== #30 版本详情 ====================

    /**
     * API #30: GET /service/open/v2/flows/{flowId}/versions/{versionId}
     * 版本详情，含编排配置快照
     */
    public ApiResponse<FlowVersionDetailResponse> getVersionDetail(Long flowId, Long versionId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowVersion version = flowVersionMapper.selectById(versionId);
        if (version == null || !flowId.equals(version.getFlowId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        FlowVersionDetailResponse response = new FlowVersionDetailResponse();
        response.setVersionId(String.valueOf(version.getId()));
        response.setFlowId(String.valueOf(version.getFlowId()));
        response.setVersionNumber(version.getVersionNumber());
        response.setStatus(version.getStatus());
        response.setOrchestrationConfig(version.getOrchestrationConfig());
        response.setPublishedTime(formatDate(version.getPublishedTime()));
        response.setPublishedBy(version.getPublishedBy());
        response.setCreateTime(formatDate(version.getCreateTime()));
        response.setCreateBy(version.getCreateBy());
        response.setLastUpdateTime(formatDate(version.getLastUpdateTime()));
        response.setLastUpdateBy(version.getLastUpdateBy());

        // 查询审批信息
        if (version.getStatus() != null) {
            Integer versionStatus = version.getStatus();
            // 非草稿状态时查询审批记录和操作日志
            if (!Objects.equals(versionStatus, FlowVersionStatus.DRAFT.getCode())) {
                try {
                    ApprovalRecord record = approvalRecordMapper.selectLatestByBusiness(
                            ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH, versionId);
                    if (record != null) {
                        // 待审批状态：返回当前审批人
                        if (Objects.equals(versionStatus, FlowVersionStatus.PENDING_APPROVAL.getCode())
                                && record.getCombinedNodes() != null) {
                            JsonNode nodesNode = objectMapper.readTree(record.getCombinedNodes());
                            Integer currentNode = record.getCurrentNode();
                            if (currentNode != null && currentNode >= 0
                                    && nodesNode.isArray() && currentNode < nodesNode.size()) {
                                JsonNode nodeInfo = nodesNode.get(currentNode);
                                String approverUserId = nodeInfo.has("userId")
                                        ? nodeInfo.get("userId").asText() : null;
                                String approverUserName = nodeInfo.has("userName")
                                        ? nodeInfo.get("userName").asText() : null;

                                FlowVersionDetailResponse.ApproverInfo approverInfo =
                                        new FlowVersionDetailResponse.ApproverInfo();
                                approverInfo.setUserId(approverUserId);
                                approverInfo.setUserName(approverUserName);
                                response.setApprover(approverInfo);
                            }
                        }

                        // 查询最近一条审批操作日志（通过/驳回/撤回）
                        List<ApprovalLog> logs = approvalLogMapper.selectByRecordId(record.getId());
                        if (logs != null && !logs.isEmpty()) {
                            // 取最新一条（按 create_time 倒序的第一条）
                            ApprovalLog latestLog = logs.get(logs.size() - 1);  // 已按时间排序，最后一条最新
                            FlowVersionDetailResponse.ApprovalLogInfo logInfo =
                                    new FlowVersionDetailResponse.ApprovalLogInfo();
                            logInfo.setOperatorId(latestLog.getOperatorId());
                            logInfo.setOperatorName(latestLog.getOperatorName());
                            logInfo.setAction(latestLog.getAction());
                            logInfo.setComment(latestLog.getComment());
                            logInfo.setActionTime(formatDate(latestLog.getCreateTime()));
                            response.setLatestApprovalLog(logInfo);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to query approval info for flow version: versionId={}, error={}",
                            versionId, e.getMessage());
                }
            }
        }

        // 审批地址
        response.setApprovalUrl(approvalUrlPrefix + versionId);

        return ApiResponse.success(response);
    }

    // ==================== #31 更新草稿 ====================

    /**
     * API #31: PUT /service/open/v2/flows/{flowId}/versions/{versionId}
     * 更新草稿（仅 DB 存储级校验：JSON 可解析即可）
     */
    @Transactional
    public ApiResponse<?> updateDraft(Long flowId, Long versionId, String orchestrationConfig) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowVersion version = flowVersionMapper.selectById(versionId);
        if (version == null || !flowId.equals(version.getFlowId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        // 草稿、已撤回、已驳回状态可编辑（spec §1.7.4）
        Integer currentStatus = version.getStatus();
        if (currentStatus == null ||
                (!Objects.equals(currentStatus, FlowVersionStatus.DRAFT.getCode())
                        && !Objects.equals(currentStatus, FlowVersionStatus.WITHDRAWN.getCode())
                        && !Objects.equals(currentStatus, FlowVersionStatus.REJECTED.getCode()))) {
            return ApiResponse.error("409",
                    "非草稿/已撤回/已驳回状态，不可编辑",
                    "Only draft, withdrawn, or rejected versions can be edited");
        }

        // 已撤回/已驳回 → 自动转为草稿
        if (Objects.equals(currentStatus, FlowVersionStatus.WITHDRAWN.getCode())
                || Objects.equals(currentStatus, FlowVersionStatus.REJECTED.getCode())) {
            version.setStatus(FlowVersionStatus.DRAFT.getCode());
        }

        // DB 存储级校验：JSON 可解析
        if (orchestrationConfig != null && !orchestrationConfig.isEmpty()) {
            try {
                objectMapper.readTree(orchestrationConfig);
            } catch (Exception e) {
                return ApiResponse.error("400",
                        "编排配置 JSON 格式无效：" + e.getMessage(),
                        "Invalid orchestration config JSON: " + e.getMessage());
            }
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        version.setOrchestrationConfig(orchestrationConfig);
        version.setLastUpdateTime(now);
        version.setLastUpdateBy(currentUser);

        flowVersionMapper.update(version);

        // 同步 connector_version_ref 中间表
        syncConnectorVersionRefs(flowId, versionId, orchestrationConfig, currentUser, now);

        log.info("Draft version updated: flowId={}, versionId={}, versionNumber={}",
                flowId, versionId, version.getVersionNumber());
        return ApiResponse.success();
    }

    // ==================== #32 发布 ====================

    /**
     * API #32: POST /service/open/v2/flows/{flowId}/versions/{versionId}/publish
     * 发布版本：全量 9 项校验 → 提交审批
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public ApiResponse<FlowPublishResponse> publish(Long flowId, Long versionId) {
        Long appId = AppContextHolder.requireInternalAppId();
        String externalAppId = AppContextHolder.getCurrentContext().getExternalId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowVersion version = flowVersionMapper.selectById(versionId);
        if (version == null || !flowId.equals(version.getFlowId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        ApiResponse<?> result = validateFlowVersionFields(version);
        if (result != null) return (ApiResponse<FlowPublishResponse>) result;

        result = validateOrchestration(flow, version, externalAppId);
        if (result != null) return (ApiResponse<FlowPublishResponse>) result;

        // ── 同步引用并校验连接器版本引用可用性 ──
        String config = version.getOrchestrationConfig();
        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();
        syncConnectorVersionRefs(flowId, versionId, config, currentUser, now);

        result = validateConnectorRefs(versionId);
        if (result != null) return (ApiResponse<FlowPublishResponse>) result;

        // ── 应用上限校验 ──
        result = validateRateLimits(config, externalAppId);
        if (result != null) return (ApiResponse<FlowPublishResponse>) result;

        // ── 全部校验通过，提交审批 ──
        try {
            approvalService.submitApproval(versionId, flowId,
                    flow.getNameCn(), flow.getNameEn(), appId,
                    UserContextHolder.getUserId(), UserContextHolder.getUserName());
            log.info("Approval submitted for flow version: flowId={}, versionId={}", flowId, versionId);
        } catch (Exception e) {
            log.error("Failed to submit approval for flow version: flowId={}, versionId={}, error={}",
                    flowId, versionId, e.getMessage(), e);
            return ApiResponse.error("500",
                    "提交审批失败：" + e.getMessage(),
                    "Failed to submit approval: " + e.getMessage());
        }

        log.info("Flow version submitted for approval: flowId={}, versionId={}, versionNumber={}",
                flowId, versionId, version.getVersionNumber());

        // 记录审计日志（FR-046）
        try {
            OperateLog auditLog = new OperateLog();
            auditLog.setOperateType("PUBLISH");
            auditLog.setOperateObject("流版本");
            auditLog.setOperateDescCn("提交连接流版本发布审批 - " + flow.getNameCn() + " v" + version.getVersionNumber());
            auditLog.setOperateDescEn("Submit flow version publish approval - " + flow.getNameEn() + " v" + version.getVersionNumber());
            auditLog.setOperateUser(currentUser);
            auditLog.setAppId(String.valueOf(appId));
            auditLog.setIpAddress(com.xxx.it.works.wecode.v2.common.util.CommonUtils.extractIpAddress());
            auditLog.setCreateBy(currentUser);
            auditLog.setCreateTime(now);
            auditLog.setLastUpdateBy(currentUser);
            auditLog.setLastUpdateTime(now);
            auditLog.setStatus(1);
            auditLogService.saveAsync(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log for flow publish: flowId={}, versionId={}", flowId, versionId, e);
        }

        FlowPublishResponse response = FlowPublishResponse.builder()
                .versionId(String.valueOf(versionId))
                .versionNumber(version.getVersionNumber())
                .status(FlowVersionStatus.PENDING_APPROVAL.getCode())
                .submittedTime(formatDate(now))
                .message("已提交审批，请等待审批结果")
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 校验版本状态是否为草稿
     */
    private ApiResponse<?> validateFlowVersionFields(FlowVersion version) {
        if (version.getStatus() == null || !version.getStatus().equals(FlowVersionStatus.DRAFT.getCode())) {
            return ApiResponse.error("409",
                    "非草稿状态，不可发布",
                    "Only draft versions can be published");
        }
        return null;
    }

    /**
     * 校验编排配置不为空且通过编排校验器
     */
    private ApiResponse<?> validateOrchestration(Flow flow, FlowVersion version, String externalAppId) {
        List<String> errors = publishValidator.validateBusinessFields(flow.getNameCn(), flow.getNameEn());
        if (!errors.isEmpty()) {
            return ApiResponse.error("422", String.join("；", errors), "Validation failed: " + String.join("; ", errors));
        }
        String config = version.getOrchestrationConfig();
        if (config == null || config.trim().isEmpty()) {
            return ApiResponse.error("422",
                    "草稿编排配置为空，请先保存编排配置",
                    "Draft orchestration config is empty");
        }
        errors = publishValidator.validateOrchestrationConfig(config, externalAppId);
        if (!errors.isEmpty()) {
            return ApiResponse.error("422", String.join("；", errors), "Validation failed: " + String.join("; ", errors));
        }
        return null;
    }

    /**
     * 校验连接器版本引用可用性
     */
    private ApiResponse<?> validateConnectorRefs(Long versionId) {
        List<String> errors = publishValidator.validateConnectorVersionRefs(versionId);
        if (!errors.isEmpty()) {
            return ApiResponse.error("422", String.join("；", errors), "Validation failed: " + String.join("; ", errors));
        }
        return null;
    }

    /**
     * 校验应用 QPS、并发数、超时时间上限
     */
    private ApiResponse<?> validateRateLimits(String config, String externalAppId) {
        String appIdStr = externalAppId;
        Map<String, String> propertyConfig = propertyService.loadConfigBundle(appIdStr);
        int appMaxQps = getIntFromConfig(propertyConfig, ConnectorPlatformConstants.ITEM_FLOW_MAX_QPS, ConnectorPlatformConstants.DEFAULT_QPS_LIMIT);
        int appMaxConcurrency = getIntFromConfig(propertyConfig, ConnectorPlatformConstants.ITEM_FLOW_MAX_CONCURRENCY, ConnectorPlatformConstants.DEFAULT_CONCURRENCY_LIMIT);
        int appMaxTimeoutMs = getIntFromConfig(propertyConfig, ConnectorPlatformConstants.ITEM_NODE_MAX_TIMEOUT_SECONDS, ConnectorPlatformConstants.DEFAULT_TIMEOUT_SECONDS) * 1000;
        List<String> errors = publishValidator.validateRateLimitAgainstAppMax(config, appMaxQps, appMaxConcurrency);
        if (!errors.isEmpty()) {
            return ApiResponse.error("422", String.join("；", errors), "Validation failed: " + String.join("; ", errors));
        }
        errors = publishValidator.validateTimeoutAgainstAppMax(config, appMaxTimeoutMs);
        if (!errors.isEmpty()) {
            return ApiResponse.error("422", String.join("；", errors), "Validation failed: " + String.join("; ", errors));
        }
        return null;
    }

    private int getIntFromConfig(Map<String, String> config, String key, int defaultVal) {
        String value = config.get(key);
        if (value == null || value.isEmpty()) return defaultVal;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    // ==================== #33 复制到草稿 ====================

    /**
     * API #33: POST /service/open/v2/flows/{flowId}/versions/{versionId}/copy-to-draft
     * 复制已有版本到新草稿，校验无待审批/已驳回/已撤回版本
     */
    @Transactional
    public ApiResponse<?> copyToDraft(Long flowId, Long versionId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowVersion sourceVersion = flowVersionMapper.selectById(versionId);
        if (sourceVersion == null || !flowId.equals(sourceVersion.getFlowId())) {
            return ApiResponse.error("404", "源版本不存在", "Source version not found");
        }

        // 版本上限校验（提前做，因为后续可能删除旧草稿）
        List<FlowVersion> existingVersions = flowVersionMapper.selectListByFlowId(flowId, null);

        // 检查是否有待审批/已驳回/已撤回版本
        boolean hasBlockingVersion = existingVersions != null && existingVersions.stream()
                .anyMatch(v -> v.getStatus() != null && (
                        v.getStatus().equals(FlowVersionStatus.PENDING_APPROVAL.getCode())
                                || v.getStatus().equals(FlowVersionStatus.REJECTED.getCode())
                                || v.getStatus().equals(FlowVersionStatus.WITHDRAWN.getCode())));
        if (hasBlockingVersion) {
            return ApiResponse.error("409",
                    "存在待审批/已驳回/已撤回的版本，请先处理后再创建草稿",
                    "A pending/rejected/withdrawn version exists, resolve it first");
        }

        // 版本上限再次检查（含即将新建的版本）
        int currentCount = existingVersions != null ? existingVersions.size() : 0;
        // 如果有旧草稿且会被覆盖，不计入总数
        long draftCount = existingVersions != null ? existingVersions.stream()
                .filter(v -> v.getStatus() != null && v.getStatus().equals(FlowVersionStatus.DRAFT.getCode()))
                .count() : 0;
        int maxVersionCount = propertyService.getFlowMaxVersions();
        if (currentCount - draftCount >= maxVersionCount) {
            return ApiResponse.error("422",
                    "版本数量已达上限 " + maxVersionCount,
                    "Version count exceeds limit " + maxVersionCount);
        }

        // 查找并删除已有草稿
        String message;
        if (existingVersions != null) {
            for (FlowVersion v : existingVersions) {
                if (v.getStatus() != null && v.getStatus().equals(FlowVersionStatus.DRAFT.getCode())) {
                    flowVersionMapper.deleteById(v.getId());
                    log.info("Overwritten existing draft: versionId={}, versionNumber={}", v.getId(), v.getVersionNumber());
                    message = "已覆盖当前草稿内容";
                    // 继续生成新草稿
                    break;
                }
            }
        }

        // 生成新版本号
        int versionNumber = 1;
        Integer maxVersion = flowVersionMapper.selectMaxVersionNumberByFlowId(flowId);
        if (maxVersion != null && maxVersion > 0) {
            versionNumber = maxVersion + 1;
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        FlowVersion newDraft = new FlowVersion();
        newDraft.setId(idGenerator.nextId());
        newDraft.setFlowId(flowId);
        newDraft.setVersionNumber(versionNumber);
        newDraft.setStatus(FlowVersionStatus.DRAFT.getCode());
        newDraft.setOrchestrationConfig(sourceVersion.getOrchestrationConfig());
        newDraft.setCreateTime(now);
        newDraft.setLastUpdateTime(now);
        newDraft.setCreateBy(currentUser);
        newDraft.setLastUpdateBy(currentUser);

        flowVersionMapper.insert(newDraft);

        log.info("Version copied to draft: flowId={}, sourceVersionNumber={}, newVersionNumber={}",
                flowId, sourceVersion.getVersionNumber(), versionNumber);

        return ApiResponse.success("已创建新草稿版本，版本号：" + versionNumber);
    }

    // ==================== #34 失效版本 ====================

    /**
     * API #34: PUT /service/open/v2/flows/{flowId}/versions/{versionId}/invalidate
     * 失效版本，校验未部署
     */
    @Transactional
    public ApiResponse<?> invalidateVersion(Long flowId, Long versionId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowVersion version = flowVersionMapper.selectById(versionId);
        if (version == null || !flowId.equals(version.getFlowId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        // 仅已发布状态可失效
        if (version.getStatus() == null || !version.getStatus().equals(FlowVersionStatus.PUBLISHED.getCode())) {
            return ApiResponse.error("409",
                    "非已发布状态，不可失效",
                    "Only published versions can be invalidated");
        }

        // 校验未部署：compare with flow.deployedVersionId
        if (flow.getDeployedVersionId() != null && flow.getDeployedVersionId().equals(versionId)) {
            return ApiResponse.error("422",
                    "该版本为当前部署版本，请先部署其他版本后再失效",
                    "Version is currently deployed, deploy another version first");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        version.setStatus(FlowVersionStatus.INVALIDATED.getCode());
        version.setLastUpdateTime(now);
        version.setLastUpdateBy(currentUser);
        flowVersionMapper.update(version);

        flowCacheEvictor.evictFlowVersion(versionId);

        log.info("Flow version invalidated: flowId={}, versionId={}, versionNumber={}",
                flowId, versionId, version.getVersionNumber());
        return ApiResponse.success();
    }

    // ==================== #35 恢复版本 ====================

    /**
     * API #35: PUT /service/open/v2/flows/{flowId}/versions/{versionId}/recover
     * 恢复版本 → 已发布
     */
    @Transactional
    public ApiResponse<?> recoverVersion(Long flowId, Long versionId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowVersion version = flowVersionMapper.selectById(versionId);
        if (version == null || !flowId.equals(version.getFlowId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        // 仅已失效状态可恢复
        if (version.getStatus() == null || !version.getStatus().equals(FlowVersionStatus.INVALIDATED.getCode())) {
            return ApiResponse.error("409",
                    "非已失效状态，不可恢复",
                    "Only invalidated versions can be recovered");
        }

        FlowVersionStatus currentStatus = FlowVersionStatus.fromValue(version.getStatus());
        if (!FlowVersionStatus.isValidTransition(currentStatus, FlowVersionStatus.PUBLISHED)) {
            return ApiResponse.error("409",
                    "当前状态不可恢复",
                    "Invalid status transition");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        version.setStatus(FlowVersionStatus.PUBLISHED.getCode());
        version.setLastUpdateTime(now);
        version.setLastUpdateBy(currentUser);
        flowVersionMapper.update(version);

        flowCacheEvictor.evictFlowVersion(versionId);

        log.info("Flow version recovered: flowId={}, versionId={}, versionNumber={}",
                flowId, versionId, version.getVersionNumber());
        return ApiResponse.success();
    }

    // ==================== #36 删除版本 ====================

    /**
     * API #36: DELETE /service/open/v2/flows/{flowId}/versions/{versionId}
     * 删除版本（仅草稿或已失效状态可删除）
     */
    @Transactional
    public ApiResponse<Void> deleteVersion(Long flowId, Long versionId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowVersion version = flowVersionMapper.selectById(versionId);
        if (version == null || !flowId.equals(version.getFlowId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        // 仅草稿、已失效、已撤回、已驳回状态可删除 (FR-029)
        if (version.getStatus() == null ||
                (!version.getStatus().equals(FlowVersionStatus.DRAFT.getCode())
                        && !version.getStatus().equals(FlowVersionStatus.INVALIDATED.getCode())
                        && !version.getStatus().equals(FlowVersionStatus.WITHDRAWN.getCode())
                        && !version.getStatus().equals(FlowVersionStatus.REJECTED.getCode()))) {
            return ApiResponse.error("409",
                    "仅草稿/已失效/已撤回/已驳回状态可删除",
                    "Only draft, invalidated, withdrawn, or rejected versions can be deleted");
        }

        flowVersionMapper.deleteById(versionId);

        flowCacheEvictor.evictFlowVersion(versionId);

        log.info("Flow version deleted: flowId={}, versionId={}, versionNumber={}",
                flowId, versionId, version.getVersionNumber());
        return ApiResponse.success();
    }

    // ==================== #37 撤回审批 ====================

    /**
     * API #37: POST /service/open/v2/flows/{flowId}/versions/{versionId}/cancel
     * 撤回审批：待审批 → 已撤回
     */
    @Transactional
    public ApiResponse<?> cancelApproval(Long flowId, Long versionId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowVersion version = flowVersionMapper.selectById(versionId);
        if (version == null || !flowId.equals(version.getFlowId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        // 仅待审批状态可撤回
        if (version.getStatus() == null || !version.getStatus().equals(FlowVersionStatus.PENDING_APPROVAL.getCode())) {
            return ApiResponse.error("409",
                    "非待审批状态，不可撤回",
                    "Only pending approval versions can be cancelled");
        }

        // 委托给 FlowVersionApprovalService，同时更新 approval_record 和 flow_version 状态
        String operator = UserContextHolder.getUserId();
        approvalService.cancelApproval(versionId, appId, operator);

        log.info("Flow version approval cancelled: flowId={}, versionId={}, versionNumber={}",
                flowId, versionId, version.getVersionNumber());
        return ApiResponse.success();
    }

    // ==================== #38 催办 ====================

    /**
     * API #38: POST /service/open/v2/flows/{flowId}/versions/{versionId}/urge
     * 催办：通知当前审批节点审批人
     */
    public ApiResponse<?> urgeApproval(Long flowId, Long versionId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowVersion version = flowVersionMapper.selectById(versionId);
        if (version == null || !flowId.equals(version.getFlowId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        // 仅待审批状态可催办
        if (version.getStatus() == null || !version.getStatus().equals(FlowVersionStatus.PENDING_APPROVAL.getCode())) {
            return ApiResponse.error("409",
                    "非待审批状态，无需催办",
                    "Only pending approval versions can be urged");
        }

        // 调用通用审批服务催办
        try {
            String currentUser = UserContextHolder.getUserName();
            ApprovalActionResponse result = genericApprovalService.urge(
                    versionId, "connector_flow_version_publish", currentUser);
            log.info("Approval urged for flow version: flowId={}, versionId={}, message={}",
                    flowId, versionId, result.getMessage());
            return ApiResponse.success(result.getMessage());
        } catch (Exception e) {
            log.error("Failed to urge approval for flow version: flowId={}, versionId={}, error={}",
                    flowId, versionId, e.getMessage(), e);
            return ApiResponse.error("500", "催办失败：" + e.getMessage(), "Urge failed: " + e.getMessage());
        }
    }

    // ==================== 缓存失效 ====================

    // ==================== 辅助方法 ====================

    /**
     * 同步 connector_version_ref 中间表
     * <p>
     * 解析编排配置中的 connector 节点，维护引用关系。
     * 先删除已有引用，再批量插入新引用。
     * </p>
     *
     * @param flowId    连接流ID
     * @param versionId 连接流版本ID
     * @param config    编排配置 JSON
     * @param operator  操作人
     * @param now       当前时间
     */
    private void syncConnectorVersionRefs(Long flowId, Long versionId, String config,
                                            String operator, Date now) {
        if (config == null || config.trim().isEmpty()) {
            return;
        }

        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(config);
            com.fasterxml.jackson.databind.JsonNode nodes = root.get("nodes");
            if (nodes == null || !nodes.isArray()) {
                return;
            }

            // 删除已有引用
            connectorVersionRefMapper.deleteByFlowVersionId(versionId);

            // 遍历节点，为 connector 节点创建引用
            java.util.List<ConnectorVersionRef> refs = new java.util.ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
                processConnectorRefNode(node, flowId, versionId, refs, operator, now);
            }

            if (!refs.isEmpty()) {
                connectorVersionRefMapper.insertBatch(refs);
                log.info("Synced {} connector version refs: flowId={}, versionId={}",
                        refs.size(), flowId, versionId);
            }
        } catch (Exception e) {
            log.warn("Failed to sync connector version refs: flowId={}, versionId={}, error={}",
                    flowId, versionId, e.getMessage());
        }
    }

    /**
     * 处理单个 connector 引用节点，解析 connectorId/connectorVersionId 并构建引用对象
     *
     * @param node      当前节点
     * @param flowId    连接流ID
     * @param versionId 连接流版本ID
     * @param refs      引用列表（已初始化）
     * @param operator  操作人
     * @param now       当前时间
     */
    private void processConnectorRefNode(com.fasterxml.jackson.databind.JsonNode node, Long flowId, Long versionId,
                                          java.util.List<ConnectorVersionRef> refs, String operator, Date now) {
        String type = NodeTypeResolver.businessType(node);
        if (!"connector".equals(type)) {
            return;
        }

        String nodeId = node.has("id") ? node.get("id").asText() : null;
        if (nodeId == null) {
            return;
        }

        com.fasterxml.jackson.databind.JsonNode data = node.get("data");
        if (data == null) {
            return;
        }

        Long connectorId = parseLongSafely(data, "connectorId");
        Long connectorVersionId = parseLongSafely(data, "connectorVersionId");

        if (connectorVersionId != null) {
            ConnectorVersionRef ref = new ConnectorVersionRef();
            ref.setId(idGenerator.nextId());
            ref.setFlowId(flowId);
            ref.setFlowVersionId(versionId);
            ref.setNodeId(nodeId);
            ref.setConnectorId(connectorId);
            ref.setConnectorVersionId(connectorVersionId);
            ref.setCreateTime(now);
            ref.setLastUpdateTime(now);
            ref.setCreateBy(operator);
            ref.setLastUpdateBy(operator);
            refs.add(ref);
        }
    }

    /**
     * 安全地从 JsonNode 中解析 Long 字段值，解析失败时返回 null
     *
     * @param data      JsonNode 数据节点
     * @param fieldName 字段名
     * @return Long 值，解析失败返回 null
     */
    private Long parseLongSafely(com.fasterxml.jackson.databind.JsonNode data, String fieldName) {
        if (data.has(fieldName) && !data.get(fieldName).isNull()) {
            try {
                return Long.parseLong(data.get(fieldName).asText());
            } catch (NumberFormatException ignored) {
                log.debug("Number format parsing ignored: {}", ignored.getMessage());
            }
        }
        return null;
    }

    /**
     * 格式化日期为 "yyyy-MM-dd HH:mm:ss" 字符串
     */
    private static String formatDate(Date date) {
        if (date == null) return null;
        return DATE_FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }
}
