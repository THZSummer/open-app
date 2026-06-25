package com.xxx.it.works.wecode.v2.modules.connector;

import com.xxx.it.works.wecode.v2.common.annotation.AuditLog;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connector.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 连接器管理 Controller（V3 应用隔离版本）
 * <p>
 * 连接器实体 CRUD（API #1~#7）
 * V3 变更：
 * - 移除 @PlatformAdminPermission，V3 使用基于 X-App-Id 的应用访问控制
 * - 创建时不自动生成草稿版本
 * - 新增失效/恢复生命周期操作
 * - 所有接口接收 X-App-Id Header
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/connectors")
@Tag(name = "连接器管理", description = "连接器 CRUD 接口")
public class ConnectorController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConnectorController.class);




    @Autowired
    public ConnectorController(ConnectorService connectorService) {
        this.connectorService = connectorService;
    }
    private final ConnectorService connectorService;

    /**
     * #1 创建连接器
     */
    @AuditLog(value = OperateEnum.CREATE_CONNECTOR)
    @PostMapping
    @Operation(summary = "#1 创建连接器", description = "创建连接器基本信息，状态为有效不可用，需手动创建草稿版本")
    public ApiResponse<ConnectorCreateResponse> createConnector(
            @RequestHeader("X-App-Id") Long appId,
            @Valid @RequestBody ConnectorCreateRequest request) {
        log.info("POST /connectors - create connector: nameCn={}, appId={}", request.getNameCn(), appId);
        return connectorService.createConnector(request, appId);
    }

    /**
     * #2 查询连接器列表
     */
    @GetMapping
    @Operation(summary = "#2 查询连接器列表", description = "列表查询，支持 status/connectorType/keyword 过滤 + 分页")
    public ApiResponse<List<ConnectorListResponse>> getConnectorList(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器状态过滤") @RequestParam(required = false) Integer status,
            @Parameter(description = "连接器类型过滤") @RequestParam(required = false) Integer connectorType,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码") @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量") @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        log.info("GET /connectors - list: appId={}, status={}, connectorType={}, keyword={}", appId, status, connectorType, keyword);
        return connectorService.getConnectorList(status, connectorType, keyword, curPage, pageSize, appId);
    }

    /**
     * #3 查询连接器详情
     */
    @GetMapping("/{connectorId}")
    @Operation(summary = "#3 查询连接器详情", description = "详情查询，含基本信息")
    public ApiResponse<ConnectorDetailResponse> getConnectorDetail(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId) {
        log.info("GET /connectors/{} - detail: appId={}", connectorId, appId);
        return connectorService.getConnectorDetail(connectorId, appId);
    }

    /**
     * #4 更新连接器基本信息
     */
    @AuditLog(value = OperateEnum.UPDATE_CONNECTOR, resourceIdParam = "connectorId")
    @PutMapping("/{connectorId}")
    @Operation(summary = "#4 更新连接器", description = "更新名称和描述信息")
    public ApiResponse<Void> updateConnector(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Valid @RequestBody ConnectorUpdateRequest request) {
        log.info("PUT /connectors/{} - update: appId={}", connectorId, appId);
        return connectorService.updateConnector(connectorId, request, appId);
    }

    /**
     * #5 失效连接器
     */
    @PutMapping("/{connectorId}/invalidate")
    @Operation(summary = "#5 失效连接器", description = "标记失效，校验无连接流引用")
    public ApiResponse<?> invalidateConnector(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId) {
        log.info("PUT /connectors/{}/invalidate: appId={}", connectorId, appId);
        return connectorService.invalidateConnector(connectorId, appId);
    }

    /**
     * #6 恢复连接器
     */
    @PutMapping("/{connectorId}/recover")
    @Operation(summary = "#6 恢复连接器", description = "恢复连接器，根据已发布版本有无确定状态")
    public ApiResponse<?> recoverConnector(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId) {
        log.info("PUT /connectors/{}/recover: appId={}", connectorId, appId);
        return connectorService.recoverConnector(connectorId, appId);
    }

    /**
     * #7 删除连接器
     */
    @AuditLog(value = OperateEnum.DELETE_CONNECTOR, resourceIdParam = "connectorId")
    @DeleteMapping("/{connectorId}")
    @Operation(summary = "#7 删除连接器", description = "物理删除，仅已失效状态可删除")
    public ApiResponse<Void> deleteConnector(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId) {
        log.info("DELETE /connectors/{}: appId={}", connectorId, appId);
        return connectorService.deleteConnector(connectorId, appId);
    }
}
