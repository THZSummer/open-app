package com.xxx.it.works.wecode.v2.modules.connectorversion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.ConnectorPlatformPropertyService;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorStatus;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorVersionStatus;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connector.dto.*;
import com.xxx.it.works.wecode.v2.modules.connectorversion.dto.ConnectorVersionDetailResponse;
import com.xxx.it.works.wecode.v2.modules.connectorversion.dto.ConnectorVersionListResponse;
import com.xxx.it.works.wecode.v2.modules.connector.entity.Connector;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersion;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersionRef;
import com.xxx.it.works.wecode.v2.modules.auditlog.entity.OperateLog;
import com.xxx.it.works.wecode.v2.modules.auditlog.service.AuditLogService;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.ConnectorVersionRefMapper;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.OpConnectorMapper;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.OpConnectorVersionMapper;
import com.xxx.it.works.wecode.v2.modules.security.AppContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 连接器版本管理服务（V3 多版本模型 v2.1.0）
 * <p>
 * 实现连接器版本管理全流程（API #8~#16），涵盖 FR-005~FR-011
 * </p>
 * <p>
 * v2.1.0 变更：
 * - 移除方法签名的 appId 参数，由 AppDataIsolationAspect 从请求 Header 解析
 *   后注入 AppContextHolder，Service 体内通过 AppContextHolder.requireInternalAppId() 获取
 * </p>
 */
@Slf4j
@Service
public class ConnectorVersionService {

    private final OpConnectorMapper connectorMapper;
    private final OpConnectorVersionMapper connectorVersionMapper;
    private final ConnectorVersionRefMapper connectorVersionRefMapper;
    private final IdGeneratorStrategy idGenerator;
    private final AuditLogService auditLogService;
    private final ConnectorPlatformPropertyService propertyService;

    @Autowired
    public ConnectorVersionService(OpConnectorMapper connectorMapper, OpConnectorVersionMapper connectorVersionMapper,
                                   ConnectorVersionRefMapper connectorVersionRefMapper, IdGeneratorStrategy idGenerator,
                                   AuditLogService auditLogService, ConnectorPlatformPropertyService propertyService) {
        this.connectorMapper = connectorMapper;
        this.connectorVersionMapper = connectorVersionMapper;
        this.connectorVersionRefMapper = connectorVersionRefMapper;
        this.idGenerator = idGenerator;
        this.auditLogService = auditLogService;
        this.propertyService = propertyService;
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== #8 创建草稿版本 ====================

    @Transactional
    public ApiResponse<?> createDraft(Long connectorId) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        int maxVersionCount = propertyService.getConnectorMaxVersions();
        List<ConnectorVersion> existingVersions = connectorVersionMapper.selectListByConnectorId(connectorId, null);
        if (existingVersions != null && existingVersions.size() >= maxVersionCount) {
            return ApiResponse.error("422",
                    "版本数量已达上限 " + maxVersionCount,
                    "Version count exceeds limit " + maxVersionCount);
        }

        boolean hasDraft = existingVersions != null && existingVersions.stream()
                .anyMatch(v -> v.getStatus() != null && v.getStatus() == ConnectorVersionStatus.DRAFT.getCode());
        if (hasDraft) {
            return ApiResponse.error("409",
                    "已存在草稿版本，请先编辑或删除当前草稿",
                    "A draft version already exists, edit or delete it first");
        }

        int versionNumber = 1;
        Integer maxVersion = connectorVersionMapper.selectMaxVersionNumberByConnectorId(connectorId);
        if (maxVersion != null) {
            versionNumber = maxVersion + 1;
        } else if (existingVersions != null && !existingVersions.isEmpty()) {
            versionNumber = existingVersions.stream()
                    .mapToInt(v -> v.getVersionNumber() != null ? v.getVersionNumber() : 0)
                    .max().orElse(0) + 1;
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        ConnectorVersion version = new ConnectorVersion();
        version.setId(idGenerator.nextId());
        version.setConnectorId(connectorId);
        version.setVersionNumber(versionNumber);
        version.setStatus(ConnectorVersionStatus.DRAFT.getCode());
        version.setConnectionConfig(null);
        version.setCreateTime(now);
        version.setLastUpdateTime(now);
        version.setCreateBy(currentUser);
        version.setLastUpdateBy(currentUser);

        connectorVersionMapper.insert(version);

        log.info("Draft version created: connectorId={}, versionId={}, versionNumber={}, internalAppId={}",
                connectorId, version.getId(), versionNumber, internalAppId);
        return ApiResponse.success();
    }

    // ==================== #9 查询版本列表 ====================

    public ApiResponse<List<ConnectorVersionListResponse>> getVersionList(Long connectorId, Integer status) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        List<ConnectorVersion> versions = connectorVersionMapper.selectListByConnectorId(connectorId, status);
        List<ConnectorVersionListResponse> items = new ArrayList<>();

        if (versions != null) {
            for (ConnectorVersion v : versions) {
                ConnectorVersionListResponse item = new ConnectorVersionListResponse();
                item.setVersionId(String.valueOf(v.getId()));
                item.setVersionNumber(v.getVersionNumber());
                item.setStatus(v.getStatus());
                item.setPublishedTime(formatDate(v.getPublishedTime()));
                item.setPublishedBy(v.getPublishedBy());
                item.setCreateTime(formatDate(v.getCreateTime()));
                item.setCreateBy(v.getCreateBy());
                items.add(item);
            }
        }

        return ApiResponse.success(items);
    }

    // ==================== #10 查询版本详情 ====================

    public ApiResponse<ConnectorVersionDetailResponse> getVersionDetail(Long connectorId, Long versionId) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorVersion version = connectorVersionMapper.selectById(versionId);
        if (version == null || !connectorId.equals(version.getConnectorId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        ConnectorVersionDetailResponse response = new ConnectorVersionDetailResponse();
        response.setVersionId(String.valueOf(version.getId()));
        response.setConnectorId(String.valueOf(version.getConnectorId()));
        response.setVersionNumber(version.getVersionNumber());
        response.setStatus(version.getStatus());
        response.setConnectionConfig(version.getConnectionConfig());
        response.setPublishedTime(formatDate(version.getPublishedTime()));
        response.setPublishedBy(version.getPublishedBy());
        response.setCreateTime(formatDate(version.getCreateTime()));
        response.setCreateBy(version.getCreateBy());

        return ApiResponse.success(response);
    }

    // ==================== #11 更新草稿版本 ====================

    @Transactional
    public ApiResponse<?> updateDraft(Long connectorId, Long versionId, String connectionConfig) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorVersion version = connectorVersionMapper.selectById(versionId);
        if (version == null || !connectorId.equals(version.getConnectorId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        if (version.getStatus() == null || version.getStatus() != ConnectorVersionStatus.DRAFT.getCode()) {
            return ApiResponse.error("409", "非草稿状态，不可编辑", "Only draft versions can be edited");
        }

        if (connectionConfig != null && !connectionConfig.isEmpty()) {
            try {
                objectMapper.readTree(connectionConfig);
            } catch (Exception e) {
                return ApiResponse.error("400",
                        "连接配置 JSON 格式无效：" + e.getMessage(),
                        "Invalid connection config JSON: " + e.getMessage());
            }
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        version.setConnectionConfig(connectionConfig);
        version.setLastUpdateTime(now);
        version.setLastUpdateBy(currentUser);

        connectorVersionMapper.update(version);

        log.info("Draft version updated: connectorId={}, versionId={}, versionNumber={}",
                connectorId, versionId, version.getVersionNumber());
        return ApiResponse.success();
    }

    // ==================== #12 发布版本 ====================

    @Transactional
    public ApiResponse<?> publish(Long connectorId, Long versionId) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorVersion version = connectorVersionMapper.selectById(versionId);
        if (version == null || !connectorId.equals(version.getConnectorId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        if (version.getStatus() == null || version.getStatus() != ConnectorVersionStatus.DRAFT.getCode()) {
            return ApiResponse.error("409", "非草稿状态，不可发布", "Only draft versions can be published");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        ApiResponse<?> configError = validateAndApplyPublish(connector, version, internalAppId, now, currentUser);
        if (configError != null) {
            return configError;
        }

        writePublishAuditLog(connector, version, internalAppId, currentUser, now);

        log.info("Version published: connectorId={}, versionId={}, versionNumber={}",
                connectorId, versionId, version.getVersionNumber());

        ConnectorPublishResponse response = ConnectorPublishResponse.builder()
                .versionId(String.valueOf(versionId))
                .versionNumber(version.getVersionNumber())
                .status(ConnectorVersionStatus.PUBLISHED.getCode())
                .connectorStatus(connector.getStatus())
                .publishedTime(formatDate(now))
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 校验发布配置并执行状态更新（JSON 语法/大小/白名单/正则校验 + 版本与连接器状态变更）
     *
     * @return 校验失败时返回 ApiResponse 错误，成功返回 null
     */
    private ApiResponse<?> validateAndApplyPublish(Connector connector, ConnectorVersion version,
                                                    Long internalAppId, Date now, String currentUser) {
        if (version.getConnectionConfig() == null || version.getConnectionConfig().isEmpty()) {
            return ApiResponse.error("422",
                    "草稿配置为空，请先完善连接配置",
                    "Draft config is empty, complete connection config first");
        }

        ApiResponse<?> result = validateConnectionConfigJson(version.getConnectionConfig());
        if (result != null) return result;

        result = validateConnectionConfigSize(version.getConnectionConfig(), internalAppId);
        if (result != null) return result;

        com.fasterxml.jackson.databind.JsonNode root;
        try {
            root = objectMapper.readTree(version.getConnectionConfig());
        } catch (Exception e) {
            return ApiResponse.error("400",
                    "连接配置 JSON 格式无效：" + e.getMessage(),
                    "Invalid connection config JSON: " + e.getMessage());
        }

        result = validateUrlWhitelist(root);
        if (result != null) return result;

        String urlRegexPattern = propertyService.getUrlRegexPattern();
        if (urlRegexPattern != null && !urlRegexPattern.isEmpty()) {
            result = validatePlatformUrlRegex(urlRegexPattern, root);
            if (result != null) return result;
        }

        version.setStatus(ConnectorVersionStatus.PUBLISHED.getCode());
        version.setPublishedTime(now);
        version.setPublishedBy(currentUser);
        version.setLastUpdateTime(now);
        version.setLastUpdateBy(currentUser);
        connectorVersionMapper.update(version);

        boolean isFirstPublished = !hasOtherPublishedVersion(connector.getId(), version.getId());
        if (isFirstPublished) {
            connector.setStatus(ConnectorStatus.AVAILABLE.getCode());
            connector.setLastUpdateTime(now);
            connector.setLastUpdateBy(currentUser);
            connectorMapper.update(connector);
            log.info("First version published, connector status -> AVAILABLE: connectorId={}", connector.getId());
        }

        return null;
    }

    /**
     * 写入发布审计日志（异步，失败仅 warn）
     */
    private void writePublishAuditLog(Connector connector, ConnectorVersion version,
                                       Long internalAppId, String currentUser, Date now) {
        try {
            OperateLog auditLog = new OperateLog();
            auditLog.setOperateType("PUBLISH");
            auditLog.setOperateObject("连接器版本");
            auditLog.setOperateDescCn("发布连接器版本 - " + connector.getNameCn() + " v" + version.getVersionNumber());
            auditLog.setOperateDescEn("Publish connector version - " + connector.getNameEn() + " v" + version.getVersionNumber());
            auditLog.setOperateUser(currentUser);
            auditLog.setAppId(String.valueOf(internalAppId));
            auditLog.setIpAddress(com.xxx.it.works.wecode.v2.common.util.CommonUtils.extractIpAddress());
            auditLog.setCreateBy(currentUser);
            auditLog.setCreateTime(now);
            auditLog.setLastUpdateBy(currentUser);
            auditLog.setLastUpdateTime(now);
            auditLog.setStatus(1);
            auditLogService.saveAsync(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log for connector publish: connectorId={}, versionId={}",
                    connector.getId(), version.getId(), e);
        }
    }

    // ==================== #13 复制到草稿 ====================

    @Transactional
    public ApiResponse<?> copyToDraft(Long connectorId, Long versionId) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorVersion sourceVersion = connectorVersionMapper.selectById(versionId);
        if (sourceVersion == null || !connectorId.equals(sourceVersion.getConnectorId())) {
            return ApiResponse.error("404", "源版本不存在", "Source version not found");
        }

        int maxVersionCount = propertyService.getConnectorMaxVersions();
        List<ConnectorVersion> existingVersions = connectorVersionMapper.selectListByConnectorId(connectorId, null);
        if (existingVersions != null && existingVersions.size() >= maxVersionCount) {
            return ApiResponse.error("422",
                    "版本数量已达上限 " + maxVersionCount,
                    "Version count exceeds limit " + maxVersionCount);
        }

        String message = null;
        if (existingVersions != null) {
            for (ConnectorVersion v : existingVersions) {
                if (v.getStatus() != null && v.getStatus() == ConnectorVersionStatus.DRAFT.getCode()) {
                    connectorVersionMapper.deleteById(v.getId());
                    message = "已覆盖当前草稿内容";
                    log.info("Overwritten existing draft: versionId={}, versionNumber={}", v.getId(), v.getVersionNumber());
                    break;
                }
            }
        }
        if (message == null) {
            message = "已创建新草稿版本";
        }

        int versionNumber = 1;
        Integer maxVersion = connectorVersionMapper.selectMaxVersionNumberByConnectorId(connectorId);
        if (maxVersion != null) {
            versionNumber = maxVersion + 1;
        } else if (existingVersions != null && !existingVersions.isEmpty()) {
            versionNumber = existingVersions.stream()
                    .mapToInt(v -> v.getVersionNumber() != null ? v.getVersionNumber() : 0)
                    .max().orElse(0) + 1;
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        ConnectorVersion newDraft = new ConnectorVersion();
        newDraft.setId(idGenerator.nextId());
        newDraft.setConnectorId(connectorId);
        newDraft.setVersionNumber(versionNumber);
        newDraft.setStatus(ConnectorVersionStatus.DRAFT.getCode());
        newDraft.setConnectionConfig(sourceVersion.getConnectionConfig());
        newDraft.setCreateTime(now);
        newDraft.setLastUpdateTime(now);
        newDraft.setCreateBy(currentUser);
        newDraft.setLastUpdateBy(currentUser);

        connectorVersionMapper.insert(newDraft);

        log.info("Version copied to draft: connectorId={}, sourceVersionNumber={}, newVersionNumber={}",
                connectorId, sourceVersion.getVersionNumber(), versionNumber);

        ConnectorCopyResponse response = ConnectorCopyResponse.builder()
                .versionId(String.valueOf(newDraft.getId()))
                .versionNumber(versionNumber)
                .status(ConnectorVersionStatus.DRAFT.getCode())
                .sourceVersionNumber(sourceVersion.getVersionNumber())
                .message(message)
                .build();

        return ApiResponse.success(response);
    }

    // ==================== #14 失效版本 ====================

    @Transactional
    public ApiResponse<?> invalidateVersion(Long connectorId, Long versionId) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorVersion version = connectorVersionMapper.selectById(versionId);
        if (version == null || !connectorId.equals(version.getConnectorId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        if (version.getStatus() == null || version.getStatus() != ConnectorVersionStatus.PUBLISHED.getCode()) {
            return ApiResponse.error("409", "非已发布状态，不可失效", "Only published versions can be invalidated");
        }

        List<String> runningFlowNames = connectorVersionRefMapper.selectRunningFlowNamesByConnectorVersionId(versionId);
        if (runningFlowNames != null && !runningFlowNames.isEmpty()) {
            return ApiResponse.error("422",
                    "以下运行中的连接流引用了此版本：" + String.join("、", runningFlowNames) + "，请先停止相关连接流",
                    "Version is referenced by running flows: " + runningFlowNames);
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        version.setStatus(ConnectorVersionStatus.INVALIDATED.getCode());
        version.setLastUpdateTime(now);
        version.setLastUpdateBy(currentUser);
        connectorVersionMapper.update(version);

        if (!hasOtherPublishedVersion(connectorId, versionId)) {
            connector.setStatus(ConnectorStatus.UNAVAILABLE.getCode());
            connector.setLastUpdateTime(now);
            connector.setLastUpdateBy(currentUser);
            connectorMapper.update(connector);
            log.info("Last published version invalidated, connector status -> UNAVAILABLE: connectorId={}", connectorId);
        }

        log.info("Version invalidated: connectorId={}, versionId={}, versionNumber={}",
                connectorId, versionId, version.getVersionNumber());
        return ApiResponse.success();
    }

    // ==================== #15 恢复版本 ====================

    @Transactional
    public ApiResponse<?> recoverVersion(Long connectorId, Long versionId) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorVersion version = connectorVersionMapper.selectById(versionId);
        if (version == null || !connectorId.equals(version.getConnectorId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        if (version.getStatus() == null || version.getStatus() != ConnectorVersionStatus.INVALIDATED.getCode()) {
            return ApiResponse.error("409", "非已失效状态，不可恢复", "Only invalidated versions can be recovered");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        version.setStatus(ConnectorVersionStatus.PUBLISHED.getCode());
        version.setLastUpdateTime(now);
        version.setLastUpdateBy(currentUser);
        connectorVersionMapper.update(version);

        boolean isFirstPublished = !hasOtherPublishedVersion(connectorId, versionId);
        if (isFirstPublished) {
            connector.setStatus(ConnectorStatus.AVAILABLE.getCode());
            connector.setLastUpdateTime(now);
            connector.setLastUpdateBy(currentUser);
            connectorMapper.update(connector);
            log.info("Recovered version is the only published, connector status -> AVAILABLE: connectorId={}", connectorId);
        }

        log.info("Version recovered: connectorId={}, versionId={}, versionNumber={}",
                connectorId, versionId, version.getVersionNumber());
        return ApiResponse.success();
    }

    // ==================== #16 删除版本 ====================

    @Transactional
    public ApiResponse<Void> deleteVersion(Long connectorId, Long versionId) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorVersion version = connectorVersionMapper.selectById(versionId);
        if (version == null || !connectorId.equals(version.getConnectorId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        if (version.getStatus() == null ||
                (version.getStatus() != ConnectorVersionStatus.DRAFT.getCode()
                        && version.getStatus() != ConnectorVersionStatus.INVALIDATED.getCode())) {
            return ApiResponse.error("409",
                    "仅草稿或已失效状态可删除",
                    "Only draft or invalidated versions can be deleted");
        }

        connectorVersionMapper.deleteById(versionId);

        log.info("Version deleted: connectorId={}, versionId={}, versionNumber={}",
                connectorId, versionId, version.getVersionNumber());
        return ApiResponse.success();
    }

    // ==================== 辅助方法 ====================

    private boolean hasOtherPublishedVersion(Long connectorId, Long excludeVersionId) {
        List<ConnectorVersion> versions = connectorVersionMapper.selectListByConnectorId(connectorId, null);
        if (versions == null) {
            return false;
        }
        return versions.stream()
                .anyMatch(v -> v.getStatus() != null
                        && v.getStatus() == ConnectorVersionStatus.PUBLISHED.getCode()
                        && !v.getId().equals(excludeVersionId));
    }

    /**
     * 格式化日期为 "yyyy-MM-dd HH:mm:ss" 字符串
     */
    private static String formatDate(Date date) {
        if (date == null) return null;
        return DATE_FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }

    /**
     * 校验连接配置 JSON 语法
     *
     * @return ApiResponse.error 或 null（通过）
     */
    private ApiResponse<?> validateConnectionConfigJson(String connectionConfig) {
        try {
            objectMapper.readTree(connectionConfig);
        } catch (Exception e) {
            return ApiResponse.error("400",
                    "连接配置 JSON 格式无效：" + e.getMessage(),
                    "Invalid connection config JSON: " + e.getMessage());
        }
        return null;
    }

    /**
     * 校验连接配置 JSON 字节数是否超过上限
     *
     * @return ApiResponse.error 或 null（通过）
     */
    private ApiResponse<?> validateConnectionConfigSize(String connectionConfig, Long appId) {
        int maxBytes = propertyService.getConnectorConfigMaxBytes(String.valueOf(appId));
        if (maxBytes > 0) {
            int actualBytes = connectionConfig.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (actualBytes > maxBytes) {
                return ApiResponse.error("422",
                        "连接配置 JSON 超过最大字节数限制 " + maxBytes + "，当前：" + actualBytes + "字节",
                        "Connection config JSON exceeds max bytes " + maxBytes + ", actual: " + actualBytes);
            }
        }
        return null;
    }

    /**
     * 校验 URL 白名单中的正则表达式是否合法
     *
     * @param root 连接配置 JSON 根节点
     * @return ApiResponse.error 或 null（通过）
     */
    private ApiResponse<?> validateUrlWhitelist(com.fasterxml.jackson.databind.JsonNode root) {
        if (!root.has("urlWhitelist") || !root.get("urlWhitelist").isArray()) {
            return null;
        }
        for (com.fasterxml.jackson.databind.JsonNode rule : root.get("urlWhitelist")) {
            if (rule.has("pattern")) {
                String pattern = rule.get("pattern").asText();
                if (pattern != null && !pattern.isEmpty()) {
                    try {
                        Pattern.compile(pattern);
                    } catch (PatternSyntaxException e) {
                        return ApiResponse.error("422",
                                "URL 白名单正则无效：" + pattern + " — " + e.getMessage(),
                                "Invalid URL whitelist regex: " + pattern);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 校验连接器目标 URL 是否符合平台正则规则
     *
     * @param urlRegexPattern 平台 URL 正则表达式
     * @param root            连接配置 JSON 根节点
     * @return ApiResponse.error 或 null（通过）
     */
    private ApiResponse<?> validatePlatformUrlRegex(String urlRegexPattern, com.fasterxml.jackson.databind.JsonNode root) {
        com.fasterxml.jackson.databind.JsonNode protocolConfig = root.get("protocolConfig");
        if (protocolConfig == null || !protocolConfig.has("url") || protocolConfig.get("url").isNull()) {
            return null;
        }
        String targetUrl = protocolConfig.get("url").asText();
        if (targetUrl == null || targetUrl.isEmpty()) {
            return null;
        }
        Pattern urlPattern;
        try {
            urlPattern = Pattern.compile(urlRegexPattern);
        } catch (PatternSyntaxException e) {
            return ApiResponse.error("500",
                    "平台 URL 正则配置无效：" + urlRegexPattern,
                    "Invalid platform URL regex configuration: " + urlRegexPattern);
        }
        if (!urlPattern.matcher(targetUrl).matches()) {
            return ApiResponse.error("422",
                    "连接器目标 URL 不符合平台正则规则：" + urlRegexPattern,
                    "Connector target URL does not match platform regex: " + urlRegexPattern);
        }
        return null;
    }
}
