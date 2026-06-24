package com.xxx.it.works.wecode.v2.modules.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorStatus;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connector.dto.*;
import com.xxx.it.works.wecode.v2.modules.connector.entity.Connector;
import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersion;
import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersionRef;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.ConnectorVersionRefMapper;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.OpConnectorMapper;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.OpConnectorVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 连接器管理服务（V3 应用隔离版本）
 * <p>
 * 实现连接器实体 CRUD（API #1~#7），涵盖 FR-001~FR-004
 * V3 变更：
 * - 创建时不自动生成草稿版本
 * - 支持失效/恢复生命周期操作
 * - 所有操作校验 appId 数据归属
 * - 状态流转使用 ConnectorStatus.isValidTransition()
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ConnectorService {

    private final OpConnectorMapper connectorMapper;
    private final OpConnectorVersionMapper connectorVersionMapper;
    private final ConnectorVersionRefMapper connectorVersionRefMapper;
    private final IdGeneratorStrategy idGenerator;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // ==================== #1 创建连接器 ====================

    /**
     * API #1: POST /service/open/v2/connectors
     * 创建连接器基本信息，不自动生成草稿版本
     */
    @Transactional
    public ApiResponse<ConnectorCreateResponse> createConnector(ConnectorCreateRequest request, Long appId) {
        log.info("Creating connector: nameCn={}, nameEn={}, type={}, appId={}",
                request.getNameCn(), request.getNameEn(), request.getConnectorType(), appId);

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
        connector.setAppId(appId);
        connector.setCreateTime(now);
        connector.setLastUpdateTime(now);
        connector.setCreateBy(currentUser);
        connector.setLastUpdateBy(currentUser);

        connectorMapper.insert(connector);

        log.info("Connector created: id={}, appId={}", connectorId, appId);

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        ConnectorCreateResponse response = ConnectorCreateResponse.builder()
                .connectorId(String.valueOf(connectorId))
                .nameCn(request.getNameCn())
                .nameEn(request.getNameEn())
                .connectorType(request.getConnectorType())
                .status(ConnectorStatus.UNAVAILABLE.getCode())
                .appId(String.valueOf(appId))
                .createTime(sdf.format(now))
                .note("创建连接器后需手动创建草稿版本")
                .build();

        return ApiResponse.success(response);
    }

    // ==================== #2 查询连接器列表 ====================

    /**
     * API #2: GET /service/open/v2/connectors
     * 列表查询，支持 connectorType / status / keyword 过滤 + 分页
     */
    public ApiResponse<List<ConnectorListResponse>> getConnectorList(
            Integer status, Integer connectorType, String keyword,
            Integer curPage, Integer pageSize, Long appId) {

        int page = curPage != null ? curPage : 1;
        int size = pageSize != null ? pageSize : 20;
        int offset = (page - 1) * size;

        // 查询所有符合基本过滤条件的记录（不含 appId，在 Java 层过滤）
        List<Connector> allConnectors = connectorMapper.selectAll(connectorType, keyword);

        // 按 appId 和 status 过滤
        List<Connector> filtered = allConnectors.stream()
                .filter(c -> appId.equals(c.getAppId()))
                .filter(c -> status == null || status.equals(c.getStatus()))
                .collect(Collectors.toList());

        long total = filtered.size();
        int fromIndex = Math.min(offset, filtered.size());
        int toIndex = Math.min(offset + size, filtered.size());
        List<Connector> pageItems = filtered.subList(fromIndex, toIndex);

        // 转换为响应 DTO
        List<ConnectorListResponse> items = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
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
            item.setCreateTime(c.getCreateTime() != null ? sdf.format(c.getCreateTime()) : null);
            item.setCreateBy(c.getCreateBy());
            item.setLastUpdateBy(c.getLastUpdateBy());
            item.setLastUpdateTime(c.getLastUpdateTime() != null ? sdf.format(c.getLastUpdateTime()) : null);

            // 查询版本信息
            List<ConnectorVersion> versions = connectorVersionMapper.selectListByConnectorId(c.getId(), null);
            Integer latestPublished = null;
            Integer draftNumber = null;
            if (versions != null) {
                for (ConnectorVersion v : versions) {
                    if (v.getStatus() != null && v.getStatus() == 2) { // PUBLISHED
                        if (latestPublished == null || v.getVersionNumber() > latestPublished) {
                            latestPublished = v.getVersionNumber();
                        }
                    }
                    if (v.getStatus() != null && v.getStatus() == 1) { // DRAFT
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

    /**
     * API #3: GET /service/open/v2/connectors/{connectorId}
     * 详情查询，校验 appId 归属
     */
    public ApiResponse<ConnectorDetailResponse> getConnectorDetail(Long connectorId, Long appId) {
        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }
        if (!appId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }
        return ApiResponse.success(toDetailResponse(connector));
    }

    // ==================== #4 更新连接器基本信息 ====================

    /**
     * API #4: PUT /service/open/v2/connectors/{connectorId}
     * 编辑基本信息
     */
    @Transactional
    public ApiResponse<Void> updateConnector(Long connectorId, ConnectorUpdateRequest request, Long appId) {
        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !appId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        if (request.getNameCn() != null) connector.setNameCn(request.getNameCn());
        if (request.getNameEn() != null) connector.setNameEn(request.getNameEn());
        if (request.getDescriptionCn() != null) connector.setDescriptionCn(request.getDescriptionCn());
        if (request.getDescriptionEn() != null) connector.setDescriptionEn(request.getDescriptionEn());

        connector.setLastUpdateTime(now);
        connector.setLastUpdateBy(currentUser);

        connectorMapper.update(connector);
        log.info("Connector updated: id={}, appId={}", connectorId, appId);
        return ApiResponse.success();
    }

    // ==================== #5 失效连接器 ====================

    /**
     * API #5: PUT /service/open/v2/connectors/{connectorId}/invalidate
     * 标记失效，校验无连接流引用
     */
    @Transactional
    public ApiResponse<?> invalidateConnector(Long connectorId, Long appId) {
        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !appId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorStatus currentStatus = ConnectorStatus.fromValue(connector.getStatus());
        if (currentStatus == null || !ConnectorStatus.isValidTransition(currentStatus, ConnectorStatus.INVALIDATED)) {
            return ApiResponse.error("409", "非有效状态，不可失效", "Invalid status for invalidation");
        }

        // 校验是否有连接流引用此连接器
        List<ConnectorVersionRef> refs = connectorVersionRefMapper.selectByConnectorId(connectorId);
        if (refs != null && !refs.isEmpty()) {
            // 收集引用流名称（简单处理，返回引用数量）
            return ApiResponse.error("422",
                    "有 " + refs.size() + " 个连接流引用此连接器，请先移除引用关系",
                    "Connector is referenced by " + refs.size() + " flows, remove references first");
        }

        Date now = new Date();
        connector.setStatus(ConnectorStatus.INVALIDATED.getCode());
        connector.setLastUpdateTime(now);
        connector.setLastUpdateBy(UserContextHolder.getUserName());
        connectorMapper.update(connector);

        log.info("Connector invalidated: id={}, appId={}", connectorId, appId);
        return ApiResponse.success();
    }

    // ==================== #6 恢复连接器 ====================

    /**
     * API #6: PUT /service/open/v2/connectors/{connectorId}/recover
     * 恢复连接器，根据已发布版本有无确定状态
     */
    @Transactional
    public ApiResponse<?> recoverConnector(Long connectorId, Long appId) {
        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !appId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorStatus currentStatus = ConnectorStatus.fromValue(connector.getStatus());
        if (currentStatus != ConnectorStatus.INVALIDATED) {
            return ApiResponse.error("409", "非已失效状态，不可恢复", "Only invalidated connectors can be recovered");
        }

        // 检查是否有已发布版本
        List<ConnectorVersion> versions = connectorVersionMapper.selectListByConnectorId(connectorId, null);
        boolean hasPublishedVersion = versions != null && versions.stream()
                .anyMatch(v -> v.getStatus() != null && v.getStatus() == 2);

        ConnectorStatus targetStatus;
        String note = null;
        if (hasPublishedVersion) {
            targetStatus = ConnectorStatus.AVAILABLE;
        } else {
            targetStatus = ConnectorStatus.UNAVAILABLE;
            note = "无已发布版本，连接器处于有效不可用状态，请先发布版本";
        }

        Date now = new Date();
        connector.setStatus(targetStatus.getCode());
        connector.setLastUpdateTime(now);
        connector.setLastUpdateBy(UserContextHolder.getUserName());
        connectorMapper.update(connector);

        log.info("Connector recovered: id={}, status={}, appId={}", connectorId, targetStatus.getCode(), appId);
        return ApiResponse.success();
    }

    // ==================== #7 删除连接器 ====================

    /**
     * API #7: DELETE /service/open/v2/connectors/{connectorId}
     * 物理删除（仅已失效状态可删除）
     */
    @Transactional
    public ApiResponse<Void> deleteConnector(Long connectorId, Long appId) {
        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null || !appId.equals(connector.getAppId())) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorStatus currentStatus = ConnectorStatus.fromValue(connector.getStatus());
        if (currentStatus != ConnectorStatus.INVALIDATED) {
            return ApiResponse.error("409",
                    "仅已失效状态的连接器可删除",
                    "Only invalidated connectors can be deleted");
        }

        // 删除版本配置
        connectorVersionMapper.deleteByConnectorId(connectorId);
        // 删除连接器基本信息
        connectorMapper.deleteById(connectorId);

        log.info("Connector deleted: id={}, appId={}", connectorId, appId);
        return ApiResponse.success();
    }

    // ==================== 内部转换方法 ====================

    private ConnectorDetailResponse toDetailResponse(Connector c) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        ConnectorDetailResponse r = new ConnectorDetailResponse();
        r.setConnectorId(String.valueOf(c.getId()));
        r.setNameCn(c.getNameCn());
        r.setNameEn(c.getNameEn());
        r.setDescriptionCn(c.getDescriptionCn());
        r.setDescriptionEn(c.getDescriptionEn());
        r.setConnectorType(c.getConnectorType());
        r.setStatus(c.getStatus());
        r.setAppId(c.getAppId() != null ? String.valueOf(c.getAppId()) : null);
        r.setCreateTime(c.getCreateTime() != null ? sdf.format(c.getCreateTime()) : null);
        r.setLastUpdateTime(c.getLastUpdateTime() != null ? sdf.format(c.getLastUpdateTime()) : null);
        return r;
    }
}
