package com.xxx.it.works.wecode.v2.modules.connector.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.model.UserContext;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connector.dto.*;
import com.xxx.it.works.wecode.v2.modules.connector.entity.Connector;
import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersion;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.ConnectorMapper;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.ConnectorVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 连接器管理服务
 * <p>
 * 实现连接器 CRUD (FR-001~004) 和连接配置管理 (FR-005~006)
 * 接口编号: #1 ~ #7
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectorService {

    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final ConnectorMapper connectorMapper;
    private final ConnectorVersionMapper connectorVersionMapper;
    private final IdGeneratorStrategy idGenerator;

    // ==================== #1 创建连接器 ====================

    /**
     * API #1: POST /api/v1/connectors
     * 创建连接器基本信息, 连接配置默认为空
     */
    @Transactional
    public ApiResponse<ConnectorCreateResponse> createConnector(ConnectorCreateRequest request) {
        log.info("Creating connector: nameCn={}, nameEn={}, type={}",
                request.getNameCn(), request.getNameEn(), request.getConnectorType());

        // 生成雪花ID
        long connectorId = idGenerator.nextId();
        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        // 创建连接器基本信息
        Connector connector = new Connector();
        connector.setId(connectorId);
        connector.setNameCn(request.getNameCn());
        connector.setNameEn(request.getNameEn());
        connector.setDescriptionCn(request.getDescriptionCn());
        connector.setDescriptionEn(request.getDescriptionEn());
        connector.setIconFileId(request.getIconFileId());
        connector.setConnectorType(request.getConnectorType());
        connector.setStatus(0);
        connector.setCreateTime(now);
        connector.setLastUpdateTime(now);
        connector.setCreateBy(currentUser);
        connector.setLastUpdateBy(currentUser);

        connectorMapper.insert(connector);

        log.info("Connector created: id={}", connectorId);
        return ApiResponse.success(ConnectorCreateResponse.builder()
                .id(String.valueOf(connectorId))
                .build());
    }

    // ==================== #2 列表查询 ====================

    /**
     * API #2: GET /api/v1/connectors
     * 列表查询, 支持 type 过滤 + keyword 搜索 + 分页
     */
    public ApiResponse<List<ConnectorListResponse>> getConnectorList(ConnectorListRequest request) {
        int offset = (request.getCurPage() - 1) * request.getPageSize();

        List<Connector> connectors = connectorMapper.selectList(
                request.getConnectorType(),
                request.getKeyword(),
                offset,
                request.getPageSize()
        );

        Long total = connectorMapper.countList(
                request.getConnectorType(),
                request.getKeyword()
        );

        List<ConnectorListResponse> items = connectors.stream()
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

    // ==================== #3 详情查询 ====================

    /**
     * API #3: GET /api/v1/connectors/{connectorId}
     * 详情查询, 含基本信息
     */
    public ApiResponse<ConnectorDetailResponse> getConnectorDetail(Long connectorId) {
        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }
        return ApiResponse.success(toDetailResponse(connector));
    }

    // ==================== #4 编辑基本信息 ====================

    /**
     * API #4: PUT /api/v1/connectors/{connectorId}
     * 编辑基本信息 (直接更新字段, 不创建新版本)
     */
    @Transactional
    public ApiResponse<Void> updateConnector(Long connectorId, ConnectorUpdateRequest request) {
        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        if (request.getNameCn() != null) connector.setNameCn(request.getNameCn());
        if (request.getNameEn() != null) connector.setNameEn(request.getNameEn());
        if (request.getDescriptionCn() != null) connector.setDescriptionCn(request.getDescriptionCn());
        if (request.getDescriptionEn() != null) connector.setDescriptionEn(request.getDescriptionEn());
        if (request.getIconFileId() != null) connector.setIconFileId(request.getIconFileId());
        if (request.getConnectorType() != null) connector.setConnectorType(request.getConnectorType());

        connector.setLastUpdateTime(now);
        connector.setLastUpdateBy(currentUser);

        connectorMapper.update(connector);
        log.info("Connector updated: id={}", connectorId);
        return ApiResponse.success();
    }

    // ==================== #5 删除连接器 ====================

    /**
     * API #5: DELETE /api/v1/connectors/{connectorId}
     * 删除前校验无运行中连接流引用
     */
    @Transactional
    public ApiResponse<Void> deleteConnector(Long connectorId) {
        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        // 校验是否有运行中的连接流引用
        Long refCount = connectorMapper.countFlowReferences(connectorId);
        if (refCount > 0) {
            return ApiResponse.error("400",
                    "该连接器被 " + refCount + " 个连接流引用, 请先删除引用关系",
                    "Connector is referenced by " + refCount + " flows, remove references first");
        }

        // 删除版本配置
        connectorVersionMapper.deleteByConnectorId(connectorId);
        // 删除连接器基本信息
        connectorMapper.deleteById(connectorId);

        log.info("Connector deleted: id={}", connectorId);
        return ApiResponse.success();
    }

    // ==================== #6 查看连接配置 ====================

    /**
     * API #6: GET /api/v1/connectors/{connectorId}/config
     * 查看连接配置 (authConfig/inputContract/outputContract/rateLimitConfig/超时)
     */
    public ApiResponse<ConnectorConfigResponse> getConnectorConfig(Long connectorId) {
        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        ConnectorVersion version = connectorVersionMapper.selectByConnectorId(connectorId);
        if (version == null || version.getConnectionConfig() == null || version.getConnectionConfig().isEmpty()) {
            return ApiResponse.success(ConnectorConfigResponse.empty());
        }

        return ApiResponse.success(ConnectorConfigResponse.of(version.getConnectionConfig()));
    }

    // ==================== #7 编辑连接配置 ====================

    /**
     * API #7: PUT /api/v1/connectors/{connectorId}/config
     * 编辑连接配置 (编辑即生效，connectionConfig JSON 全文替换，支持 authConfig/inputContract/outputContract/rateLimitConfig)
     */
    @Transactional
    public ApiResponse<Void> updateConnectorConfig(Long connectorId, ConnectorConfigUpdateRequest request) {
        Connector connector = connectorMapper.selectById(connectorId);
        if (connector == null) {
            return ApiResponse.error("404", "连接器不存在", "Connector not found");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        // 查找或创建版本记录
        ConnectorVersion version = connectorVersionMapper.selectByConnectorId(connectorId);
        if (version == null) {
            // 首次配置 - 创建版本记录
            version = new ConnectorVersion();
            version.setId(idGenerator.nextId());
            version.setConnectorId(connectorId);
            version.setConnectionConfig(request.getConnectionConfig());
            version.setCreateTime(now);
            version.setLastUpdateTime(now);
            version.setCreateBy(currentUser);
            version.setLastUpdateBy(currentUser);
            connectorVersionMapper.insert(version);
        } else {
            // 更新现有版本 - 全文替换
            version.setConnectionConfig(request.getConnectionConfig());
            version.setLastUpdateTime(now);
            version.setLastUpdateBy(currentUser);
            connectorVersionMapper.update(version);
        }

        log.info("Connector config updated: connectorId={}", connectorId);
        return ApiResponse.success();
    }

    // ==================== 公开查询接口 (供 flow 模块调用) ====================

    /**
     * 检查连接器是否存在
     */
    public boolean exists(Long connectorId) {
        return connectorMapper.selectById(connectorId) != null;
    }

    // ==================== 内部转换方法 ====================

    private ConnectorListResponse toListResponse(Connector c) {
        ConnectorListResponse r = new ConnectorListResponse();
        r.setId(String.valueOf(c.getId()));
        r.setNameCn(c.getNameCn());
        r.setNameEn(c.getNameEn());
        r.setDescriptionCn(c.getDescriptionCn());
        r.setDescriptionEn(c.getDescriptionEn());
        r.setIconFileId(c.getIconFileId());
        r.setConnectorType(c.getConnectorType());
        r.setCreateTime(formatIso(c.getCreateTime()));
        r.setLastUpdateTime(formatIso(c.getLastUpdateTime()));
        return r;
    }

    private ConnectorDetailResponse toDetailResponse(Connector c) {
        ConnectorDetailResponse r = new ConnectorDetailResponse();
        r.setId(String.valueOf(c.getId()));
        r.setNameCn(c.getNameCn());
        r.setNameEn(c.getNameEn());
        r.setDescriptionCn(c.getDescriptionCn());
        r.setDescriptionEn(c.getDescriptionEn());
        r.setIconFileId(c.getIconFileId());
        r.setConnectorType(c.getConnectorType());
        r.setCreateTime(formatIso(c.getCreateTime()));
        r.setLastUpdateTime(formatIso(c.getLastUpdateTime()));
        return r;
    }

    private String formatIso(Date date) {
        return date != null ? ISO_FORMAT.format(date) : null;
    }
}