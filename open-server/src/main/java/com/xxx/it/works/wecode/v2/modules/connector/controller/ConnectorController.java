package com.xxx.it.works.wecode.v2.modules.connector.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.PlatformAdminPermission;
import com.xxx.it.works.wecode.v2.modules.connector.dto.*;
import com.xxx.it.works.wecode.v2.modules.connector.service.ConnectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 连接器管理 Controller
 * <p>
 * 连接器 CRUD (FR-001~004) 和连接配置管理 (FR-005~006)
 * 接口编号: #1 ~ #7
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/connectors")
@RequiredArgsConstructor
@Tag(name = "连接器管理", description = "连接器 CRUD 及连接配置管理接口")
public class ConnectorController {

    private final ConnectorService connectorService;

    /**
     * #1 创建连接器
     */
    @PostMapping
    @PlatformAdminPermission
    @Operation(summary = "#1 创建连接器", description = "创建连接器基本信息")
    public ApiResponse<ConnectorCreateResponse> createConnector(
            @Valid @RequestBody ConnectorCreateRequest request) {
        log.info("POST /service/open/v2/connectors - create connector: nameCn={}", request.getNameCn());
        return connectorService.createConnector(request);
    }

    /**
     * #2 获取连接器列表
     */
    @GetMapping
    @PlatformAdminPermission
    @Operation(summary = "#2 获取连接器列表",
               description = "列表查询，支持 type 过滤 + keyword 搜索 + 分页")
    public ApiResponse<List<ConnectorListResponse>> getConnectorList(
            @Parameter(description = "连接器类型过滤")
            @RequestParam(required = false) Integer connectorType,
            @Parameter(description = "搜索关键词")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码")
            @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量")
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        ConnectorListRequest request = new ConnectorListRequest();
        request.setConnectorType(connectorType);
        request.setKeyword(keyword);
        request.setCurPage(curPage);
        request.setPageSize(pageSize);

        return connectorService.getConnectorList(request);
    }

    /**
     * #3 获取连接器详情
     */
    @GetMapping("/{connectorId}")
    @PlatformAdminPermission
    @Operation(summary = "#3 获取连接器详情",
               description = "详情查询，含基本信息")
    public ApiResponse<ConnectorDetailResponse> getConnectorDetail(
            @Parameter(description = "连接器ID")
            @PathVariable Long connectorId) {
        return connectorService.getConnectorDetail(connectorId);
    }

    /**
     * #4 编辑连接器基本信息
     */
    @PutMapping("/{connectorId}")
    @PlatformAdminPermission
    @Operation(summary = "#4 编辑连接器基本信息",
               description = "编辑基本信息，直接更新字段，不创建新版本")
    public ApiResponse<Void> updateConnector(
            @Parameter(description = "连接器ID")
            @PathVariable Long connectorId,
            @Valid @RequestBody ConnectorUpdateRequest request) {
        return connectorService.updateConnector(connectorId, request);
    }

    /**
     * #5 删除连接器
     */
    @DeleteMapping("/{connectorId}")
    @PlatformAdminPermission
    @Operation(summary = "#5 删除连接器",
               description = "删除前校验无运行中连接流引用")
    public ApiResponse<Void> deleteConnector(
            @Parameter(description = "连接器ID")
            @PathVariable Long connectorId) {
        return connectorService.deleteConnector(connectorId);
    }

    /**
     * #6 查看连接配置
     */
    @GetMapping("/{connectorId}/config")
    @PlatformAdminPermission
    @Operation(summary = "#6 查看连接配置",
               description = "查看连接配置（authConfig/inputContract/outputContract/rateLimitConfig/超时）")
    public ApiResponse<ConnectorConfigResponse> getConnectorConfig(
            @Parameter(description = "连接器ID")
            @PathVariable Long connectorId) {
        return connectorService.getConnectorConfig(connectorId);
    }

    /**
     * #7 编辑连接配置
     */
    @PutMapping("/{connectorId}/config")
    @PlatformAdminPermission
    @Operation(summary = "#7 编辑连接配置",
               description = "编辑连接配置，编辑即生效，connectionConfig JSON 全文替换（authConfig/inputContract/outputContract/rateLimitConfig）")
    public ApiResponse<Void> updateConnectorConfig(
            @Parameter(description = "连接器ID")
            @PathVariable Long connectorId,
            @Valid @RequestBody ConnectorConfigUpdateRequest request) {
        return connectorService.updateConnectorConfig(connectorId, request);
    }
}