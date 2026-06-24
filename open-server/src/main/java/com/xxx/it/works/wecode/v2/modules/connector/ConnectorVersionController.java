package com.xxx.it.works.wecode.v2.modules.connector;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connector.dto.*;
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
 * 连接器版本管理 Controller（V3 多版本模型）
 * <p>
 * 连接器版本管理（API #8~#16）
 * V3 核心特性：
 * - 多版本模型：每连接器最多 1000 个版本
 * - 发布时统一校验 / 保存时仅 DB 存储级校验
 * - 所有接口接收 X-App-Id Header
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/connectors")
@Tag(name = "连接器版本管理", description = "连接器版本 CRUD 及生命周期管理接口")
public class ConnectorVersionController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConnectorVersionController.class);




    @Autowired
    public ConnectorVersionController(ConnectorVersionService connectorVersionService) {
        this.connectorVersionService = connectorVersionService;
    }
    private final ConnectorVersionService connectorVersionService;

    /**
     * #8 创建草稿版本
     */
    @PostMapping("/{connectorId}/versions")
    @Operation(summary = "#8 创建连接器草稿版本", description = "创建空草稿，版本上限 1000 校验")
    public ResponseEntity<ApiResponse<?>> createDraft(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId) {
        log.info("POST /connectors/{}/versions - create draft: appId={}", connectorId, appId);
        return toResponseEntity(connectorVersionService.createDraft(connectorId, appId));
    }

    /**
     * #9 查询版本列表
     */
    @GetMapping("/{connectorId}/versions")
    @Operation(summary = "#9 查询连接器版本列表", description = "查询版本列表，支持 status 过滤")
    public ResponseEntity<ApiResponse<List<ConnectorVersionListResponse>>> getVersionList(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本状态过滤") @RequestParam(required = false) Integer status) {
        log.info("GET /connectors/{}/versions - list: appId={}, status={}", connectorId, appId, status);
        return toResponseEntity(connectorVersionService.getVersionList(connectorId, status, appId));
    }

    /**
     * #10 查询版本详情
     */
    @GetMapping("/{connectorId}/versions/{versionId}")
    @Operation(summary = "#10 查询连接器版本详情", description = "查询版本详情，含 connectionConfig 快照")
    public ResponseEntity<ApiResponse<ConnectorVersionDetailResponse>> getVersionDetail(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("GET /connectors/{}/versions/{} - detail: appId={}", connectorId, versionId, appId);
        return toResponseEntity(connectorVersionService.getVersionDetail(connectorId, versionId, appId));
    }

    /**
     * #11 更新草稿版本
     */
    @PutMapping("/{connectorId}/versions/{versionId}")
    @Operation(summary = "#11 更新连接器版本", description = "更新草稿（仅 DB 存储级校验）")
    public ResponseEntity<ApiResponse<?>> updateDraft(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId,
            @Valid @RequestBody ConnectorVersionSaveRequest request) {
        log.info("PUT /connectors/{}/versions/{} - update draft: appId={}", connectorId, versionId, appId);
        return toResponseEntity(connectorVersionService.updateDraft(connectorId, versionId, request.getConnectionConfig(), appId));
    }

    /**
     * #12 发布版本
     */
    @PutMapping("/{connectorId}/versions/{versionId}/publish")
    @Operation(summary = "#12 发布连接器版本", description = "发布版本：全量业务校验 + 状态联动")
    public ResponseEntity<ApiResponse<?>> publish(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("PUT /connectors/{}/versions/{}/publish: appId={}", connectorId, versionId, appId);
        return toResponseEntity(connectorVersionService.publish(connectorId, versionId, appId));
    }

    /**
     * #13 复制版本到草稿
     */
    @PostMapping("/{connectorId}/versions/{versionId}/copy-to-draft")
    @Operation(summary = "#13 复制连接器版本到草稿", description = "复制已有版本到新草稿")
    public ResponseEntity<ApiResponse<?>> copyToDraft(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "源版本ID") @PathVariable Long versionId) {
        log.info("POST /connectors/{}/versions/{}/copy-to-draft: appId={}", connectorId, versionId, appId);
        return toResponseEntity(connectorVersionService.copyToDraft(connectorId, versionId, appId));
    }

    /**
     * #14 失效版本
     */
    @PutMapping("/{connectorId}/versions/{versionId}/invalidate")
    @Operation(summary = "#14 失效连接器版本", description = "失效版本，校验无连接流引用")
    public ResponseEntity<ApiResponse<?>> invalidateVersion(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("PUT /connectors/{}/versions/{}/invalidate: appId={}", connectorId, versionId, appId);
        return toResponseEntity(connectorVersionService.invalidateVersion(connectorId, versionId, appId));
    }

    /**
     * #15 恢复版本
     */
    @PutMapping("/{connectorId}/versions/{versionId}/recover")
    @Operation(summary = "#15 恢复连接器版本", description = "恢复版本到已发布状态")
    public ResponseEntity<ApiResponse<?>> recoverVersion(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("PUT /connectors/{}/versions/{}/recover: appId={}", connectorId, versionId, appId);
        return toResponseEntity(connectorVersionService.recoverVersion(connectorId, versionId, appId));
    }

    /**
     * #16 删除版本
     */
    @DeleteMapping("/{connectorId}/versions/{versionId}")
    @Operation(summary = "#16 删除连接器版本", description = "删除版本（仅草稿或已失效状态可删除）")
    public ResponseEntity<ApiResponse<Void>> deleteVersion(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接器ID") @PathVariable Long connectorId,
            @Parameter(description = "版本ID") @PathVariable Long versionId) {
        log.info("DELETE /connectors/{}/versions/{}: appId={}", connectorId, versionId, appId);
        return toResponseEntity(connectorVersionService.deleteVersion(connectorId, versionId, appId));
    }

    // ==================== 辅助方法 ====================

    /**
     * 将 ApiResponse 转换为 ResponseEntity，根据 code 映射 HTTP 状态码
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity toResponseEntity(ApiResponse response) {
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.success());
        }
        String code = response.getCode();
        if ("200".equals(code)) {
            return ResponseEntity.ok(response);
        }
        if ("400".equals(code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if ("404".equals(code)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        if ("409".equals(code)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        if ("422".equals(code)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
        if ("500".equals(code)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
