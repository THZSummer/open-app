package com.xxx.it.works.wecode.v2.modules.connectorversion.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connectorversion.dto.ConnectorVersionDetailResponse;
import com.xxx.it.works.wecode.v2.modules.connectorversion.dto.ConnectorVersionListResponse;
import com.xxx.it.works.wecode.v2.modules.connectorversion.dto.ConnectorVersionSaveRequest;
import com.xxx.it.works.wecode.v2.modules.connectorversion.service.ConnectorVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 连接器版本管理 Controller（V3 多版本模型 v2.0.0）
 * <p>
 * 连接器版本管理（API #8~#16）
 * V3 核心特性：
 * - 多版本模型：每连接器最多 1000 个版本
 * - 发布时统一校验 / 保存时仅 DB 存储级校验
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/connectors")
@Tag(name = "连接器版本管理", description = "连接器版本 CRUD 及生命周期管理接口")
public class ConnectorVersionController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConnectorVersionController.class);

    private final ConnectorVersionService connectorVersionService;

    @Autowired
    public ConnectorVersionController(ConnectorVersionService connectorVersionService) {
        this.connectorVersionService = connectorVersionService;
    }

    /**
     * #8 创建草稿版本
     */
    @PostMapping("/{connectorId}/versions")
    @Operation(summary = "#8 创建连接器草稿版本", description = "创建空草稿，版本上限 1000 校验")
    public ResponseEntity<ApiResponse<?>> createDraft(
            @Parameter(description = "连接器ID") @PathVariable Long connectorId) {
        log.info("POST /connectors/{}/versions - create draft", connectorId);
        return toResponseEntity(connectorVersionService.createDraft(connectorId));
    }

    /**
     * #9 查询版本列表
     */
    @GetMapping("/{connectorId}/versions")
    @Operation(summary = "#9 查询连接器版本列表", description = "查询版本列表，支持 status 过滤")
    public ResponseEntity<ApiResponse<List<ConnectorVersionListResponse>>> getVersionList(
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本状态过滤") @RequestParam(required = false) Integer status) {
        log.info("GET /connectors/{}/versions - list: status={}", connectorId, status);
        return toResponseEntity(connectorVersionService.getVersionList(connectorId, status));
    }

    /**
     * #10 查询版本详情
     */
    @GetMapping("/{connectorId}/versions/{versionId}")
    @Operation(summary = "#10 查询连接器版本详情", description = "查询版本详情，含 connectionConfig 快照")
    public ResponseEntity<ApiResponse<ConnectorVersionDetailResponse>> getVersionDetail(
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("GET /connectors/{}/versions/{} - detail", connectorId, versionId);
        return toResponseEntity(connectorVersionService.getVersionDetail(connectorId, versionId));
    }

    /**
     * #11 更新草稿版本
     */
    @PutMapping("/{connectorId}/versions/{versionId}")
    @Operation(summary = "#11 更新连接器版本", description = "更新草稿（仅 DB 存储级校验）")
    public ResponseEntity<ApiResponse<?>> updateDraft(
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId,
            @Valid @RequestBody ConnectorVersionSaveRequest request) {
        log.info("PUT /connectors/{}/versions/{} - update draft", connectorId, versionId);
        return toResponseEntity(connectorVersionService.updateDraft(connectorId, versionId, request.getConnectionConfig().toString()));
    }

    /**
     * #12 发布版本
     */
    @PutMapping("/{connectorId}/versions/{versionId}/publish")
    @Operation(summary = "#12 发布连接器版本", description = "发布版本：全量业务校验 + 状态联动")
    public ResponseEntity<ApiResponse<?>> publish(
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("PUT /connectors/{}/versions/{}/publish", connectorId, versionId);
        return toResponseEntity(connectorVersionService.publish(connectorId, versionId));
    }

    /**
     * #13 复制版本到草稿
     */
    @PostMapping("/{connectorId}/versions/{versionId}/copy-to-draft")
    @Operation(summary = "#13 复制连接器版本到草稿", description = "复制已有版本到新草稿")
    public ResponseEntity<ApiResponse<?>> copyToDraft(
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "源版本ID") @PathVariable Long versionId) {
        log.info("POST /connectors/{}/versions/{}/copy-to-draft", connectorId, versionId);
        return toResponseEntity(connectorVersionService.copyToDraft(connectorId, versionId));
    }

    /**
     * #14 失效版本
     */
    @PutMapping("/{connectorId}/versions/{versionId}/invalidate")
    @Operation(summary = "#14 失效连接器版本", description = "失效版本，校验无连接流引用")
    public ResponseEntity<ApiResponse<?>> invalidateVersion(
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("PUT /connectors/{}/versions/{}/invalidate", connectorId, versionId);
        return toResponseEntity(connectorVersionService.invalidateVersion(connectorId, versionId));
    }

    /**
     * #15 恢复版本
     */
    @PutMapping("/{connectorId}/versions/{versionId}/recover")
    @Operation(summary = "#15 恢复连接器版本", description = "恢复版本到已发布状态")
    public ResponseEntity<ApiResponse<?>> recoverVersion(
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("PUT /connectors/{}/versions/{}/recover", connectorId, versionId);
        return toResponseEntity(connectorVersionService.recoverVersion(connectorId, versionId));
    }

    /**
     * #16 删除版本
     */
    @DeleteMapping("/{connectorId}/versions/{versionId}")
    @Operation(summary = "#16 删除连接器版本", description = "删除版本（仅草稿或已失效状态可删除）")
    public ResponseEntity<ApiResponse<Void>> deleteVersion(
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("DELETE /connectors/{}/versions/{}", connectorId, versionId);
        return toResponseEntity(connectorVersionService.deleteVersion(connectorId, versionId));
    }

    // ==================== 辅助方法 ====================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity toResponseEntity(ApiResponse response) {
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.success());
        }
        String code = response.getCode();
        if ("200".equals(code)) return ResponseEntity.ok(response);
        if ("400".equals(code)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        if ("404".equals(code)) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        if ("409".equals(code)) return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        if ("422".equals(code)) return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        if ("500".equals(code)) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
