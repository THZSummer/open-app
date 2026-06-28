package com.xxx.it.works.wecode.v2.modules.connector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorStatus;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connector.dto.*;
import com.xxx.it.works.wecode.v2.modules.connector.entity.Connector;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersion;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersionRef;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.ConnectorVersionRefMapper;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.OpConnectorVersionMapper;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.OpConnectorMapper;
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

/**
 * 连接器管理服务（V3 应用隔离版本 v2.1.0）
 * <p>
 * 实现连接器实体 CRUD（API #1~#7），涵盖 FR-001~FR-004
 * </p>
 * <p>
 * v2.1.0 变更：
 * - 移除方法签名的 appId 参数，由 AppDataIsolationAspect 从请求 Header 解析
 *   后注入 AppContextHolder，Service 体内通过 AppContextHolder.requireInternalAppId() 获取
 * - SQL 层 app_id 过滤实现数据库级应用数据隔离
 * </p>
 */
@Slf4j
@Service
public class ConnectorService {

    private final OpConnectorMapper connectorMapper;
    private final OpConnectorVersionMapper connectorVersionMapper;
    private final ConnectorVersionRefMapper connectorVersionRefMapper;
    private final IdGeneratorStrategy idGenerator;

    @Autowired
    public ConnectorService(OpConnectorMapper connectorMapper, OpConnectorVersionMapper connectorVersionMapper,
                            ConnectorVersionRefMapper connectorVersionRefMapper, IdGeneratorStrategy idGenerator) {
        this.connectorMapper = connectorMapper;
        this.connectorVersionMapper = connectorVersionMapper;
        this.connectorVersionRefMapper = connectorVersionRefMapper;
        this.idGenerator = idGenerator;
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== #1 创建连接器 ====================

    @Transactional
    public ApiResponse<ConnectorCreateResponse> createConnector(ConnectorCreateRequest request) {
        Long internalAppId = AppContextHolder.requireInternalAppId();
        log.info("Creating connector: nameCn={}, nameEn={}, type={}, internalAppId={}",
                request.getNameCn(), request.getNameEn(), request.getConnectorType(), internalAppId);

        long connectorId = idGenerator.nextId();
        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        Connector connector = new Connector();
        connector.setId(connectorId);
        connector.setNameCn(request.getNameCn());
        connector.setNameEn(request.getNameEn());
        connector.setDescriptionCn(request.getDescriptionCn());
        connector.setDescriptionEn(request.getDescriptionEn());
        connector.setConnectorType(request.getConnectorType());
        connector.setStatus(ConnectorStatus.UNAVAILABLE.getCode());
        connector.setAppId(internalAppId);
        connector.setCreateTime(now);
        connector.setLastUpdateTime(now);
        connector.setCreateBy(currentUser);
        connector.setLastUpdateBy(currentUser);

        connectorMapper.insert(connector);

        log.info("Connector created: id={}, internalAppId={}", connectorId, internalAppId);

        ConnectorCreateResponse response = ConnectorCreateResponse.builder()
                .connectorId(String.valueOf(connectorId))
                .nameCn(request.getNameCn())
                .nameEn(request.getNameEn())
                .connectorType(request.getConnectorType())
                .status(ConnectorStatus.UNAVAILABLE.getCode())
                .appId(String.valueOf(internalAppId))
                .createTime(DATE_FORMATTER.format(now.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()))
                .note("创建连接器后需手动创建草稿版本")
                .build();

        return ApiResponse.success(response);
    }

    // ==================== #2 查询连接器列表 ====================

    public ApiResponse<List<ConnectorListResponse>> getConnectorList(
            Integer status, Integer connectorType, String keyword,
            Integer curPage, Integer pageSize) {

        Long internalAppId = AppContextHolder.requireInternalAppId();

        int page = curPage != null ? curPage : 1;
        int size = pageSize != null ? pageSize : 20;
        int offset = (page - 1) * size;

        // SQL 层过滤 + 分页（v2.1.0: status/appId/connectorType/keyword 全部下推到 SQL）
        List<Connector> pageItems = connectorMapper.selectList(
                connectorType, keyword, internalAppId, status, offset, size);
        long total = connectorMapper.countList(
                connectorType, keyword, internalAppId, status);

        List<ConnectorListResponse> items = new ArrayList<>();
        for (Connector c : pageItems) {
            ConnectorListResponse item = new ConnectorListResponse();
            item.setConnectorId(String.valueOf(c.getId()));
            item.setNameCn(c.getNameCn());
            item.setNameEn(c.getNameEn());
            item.setDescriptionCn(c.getDescriptionCn());
            item.setDescriptionEn(c.getDescriptionEn());
            item.setConnectorType(c.getConnectorType());
            item.setStatus(c.getStatus());
            item.setAppId(String.valueOf(c.getAppId()));
            item.setCreateTime(c.getCreateTime() != null ? DATE_FORMATTER.format(c.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()) : null);
            item.setCreateBy(c.getCreateBy());
            item.setLastUpdateBy(c.getLastUpdateBy());
            item.setLastUpdateTime(c.getLastUpdateTime() != null ? DATE_FORMATTER.format(c.getLastUpdateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()) : null);

            List<ConnectorVersion> versions = connectorVersionMapper.selectListByConnectorId(c.getId(), null);
            Integer latestPublished = null;
            Integer draftNumber = null;
            if (versions != null) {
                for (ConnectorVersion v : versions) {
                    if (v.getStatus() != null && v.getStatus() == 2) {
                        if (latestPublished == null || v.getVersionNumber() > latestPublished) {
                            latestPublished = v.getVersionNumber();
                        }
                    }
                    if (v.getStatus() != null && v.getStatus() == 1) {
                        draftNumber = v.getVersionNumber();
                    }
                }
            }
            item.setLatestPublishedVersionNumber(latestPublished);
            item.setDraftVersionNumber(draftNumber);

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

    // ==================== #3 查询连接器详情 ====================

    public ApiResponse<ConnectorDetailResponse> getConnectorDetail(Long connectorId) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }
        if (!internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }
        return ApiResponse.success(toDetailResponse(connector));
    }

    // ==================== #4 更新连接器基本信息 ====================

    @Transactional
    public ApiResponse<Void> updateConnector(Long connectorId, ConnectorUpdateRequest request) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        if (request.getNameCn() != null) {
            connector.setNameCn(request.getNameCn());
        }
        if (request.getNameEn() != null) {
            connector.setNameEn(request.getNameEn());
        }
        if (request.getDescriptionCn() != null) {
            connector.setDescriptionCn(request.getDescriptionCn());
        }
        if (request.getDescriptionEn() != null) {
            connector.setDescriptionEn(request.getDescriptionEn());
        }

        connector.setLastUpdateTime(now);
        connector.setLastUpdateBy(currentUser);

        connectorMapper.update(connector);
        log.info("Connector updated: id={}, internalAppId={}", connectorId, internalAppId);
        return ApiResponse.success();
    }

    // ==================== #5 失效连接器 ====================

    @Transactional
    public ApiResponse<?> invalidateConnector(Long connectorId) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorStatus currentStatus = ConnectorStatus.fromValue(connector.getStatus());
        if (currentStatus == null || !ConnectorStatus.isValidTransition(currentStatus, ConnectorStatus.INVALIDATED)) {
            return ApiResponse.error("409", "非有效状态，不可失效", "Invalid status for invalidation");
        }

        List<ConnectorVersionRef> refs = connectorVersionRefMapper.selectByConnectorId(connectorId);
        if (refs != null && !refs.isEmpty()) {
            return ApiResponse.error("422",
                    "有 " + refs.size() + " 个连接流引用此连接器，请先移除引用关系",
                    "Connector is referenced by " + refs.size() + " flows, remove references first");
        }

        Date now = new Date();
        connector.setStatus(ConnectorStatus.INVALIDATED.getCode());
        connector.setLastUpdateTime(now);
        connector.setLastUpdateBy(UserContextHolder.getUserName());
        connectorMapper.update(connector);

        log.info("Connector invalidated: id={}, internalAppId={}", connectorId, internalAppId);
        return ApiResponse.success();
    }

    // ==================== #6 恢复连接器 ====================

    @Transactional
    public ApiResponse<?> recoverConnector(Long connectorId) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorStatus currentStatus = ConnectorStatus.fromValue(connector.getStatus());
        if (currentStatus != ConnectorStatus.INVALIDATED) {
            return ApiResponse.error("409", "非已失效状态，不可恢复", "Only invalidated connectors can be recovered");
        }

        List<ConnectorVersion> versions = connectorVersionMapper.selectListByConnectorId(connectorId, null);
        boolean hasPublishedVersion = versions != null && versions.stream()
                .anyMatch(v -> v.getStatus() != null && v.getStatus() == 2);

        ConnectorStatus targetStatus;
        if (hasPublishedVersion) {
            targetStatus = ConnectorStatus.AVAILABLE;
        } else {
            targetStatus = ConnectorStatus.UNAVAILABLE;
        }

        Date now = new Date();
        connector.setStatus(targetStatus.getCode());
        connector.setLastUpdateTime(now);
        connector.setLastUpdateBy(UserContextHolder.getUserName());
        connectorMapper.update(connector);

        log.info("Connector recovered: id={}, status={}, internalAppId={}", connectorId, targetStatus.getCode(), internalAppId);
        return ApiResponse.success();
    }

    // ==================== #7 删除连接器 ====================

    @Transactional
    public ApiResponse<Void> deleteConnector(Long connectorId) {
        Long internalAppId = AppContextHolder.requireInternalAppId();

        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !internalAppId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorStatus currentStatus = ConnectorStatus.fromValue(connector.getStatus());
        if (currentStatus != ConnectorStatus.INVALIDATED) {
            return ApiResponse.error("409",
                    "仅已失效状态的连接器可删除",
                    "Only invalidated connectors can be deleted");
        }

        connectorVersionMapper.deleteByConnectorId(connectorId);
        connectorMapper.deleteById(connectorId);

        log.info("Connector deleted: id={}, internalAppId={}", connectorId, internalAppId);
        return ApiResponse.success();
    }

    // ==================== 内部转换方法 ====================

    private ConnectorDetailResponse toDetailResponse(Connector c) {
        ConnectorDetailResponse r = new ConnectorDetailResponse();
        r.setConnectorId(String.valueOf(c.getId()));
        r.setNameCn(c.getNameCn());
        r.setNameEn(c.getNameEn());
        r.setDescriptionCn(c.getDescriptionCn());
        r.setDescriptionEn(c.getDescriptionEn());
        r.setConnectorType(c.getConnectorType());
        r.setStatus(c.getStatus());
        r.setAppId(c.getAppId() != null ? String.valueOf(c.getAppId()) : null);
        r.setCreateTime(c.getCreateTime() != null ? DATE_FORMATTER.format(c.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()) : null);
        r.setLastUpdateTime(c.getLastUpdateTime() != null ? DATE_FORMATTER.format(c.getLastUpdateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()) : null);
        return r;
    }
}
