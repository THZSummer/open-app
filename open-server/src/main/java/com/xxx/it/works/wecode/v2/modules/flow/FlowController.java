package com.xxx.it.works.wecode.v2.modules.flow;

import com.xxx.it.works.wecode.v2.common.annotation.AuditLog;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.flow.dto.*;
import com.xxx.it.works.wecode.v2.modules.flow.dto.FlowLifecycleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 连接流管理 Controller（V3 应用隔离版本）
 * <p>
 * 连接流实体 CRUD + 生命周期管理（API #17~#27）
 * V3 变更：
 * - 移除 @PlatformAdminPermission，V3 使用基于 X-App-Id 的应用访问控制
 * - 创建时不自动生成草稿版本（lifecycleStatus=1 STOPPED）
 * - 新增失效/恢复生命周期操作
 * - 新增部署（纯版本绑定）、启动（需已部署版本）、复制功能
 * - 所有接口接收 X-App-Id Header
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/flows")
@Tag(name = "连接流管理", description = "连接流 CRUD、生命周期及部署管理接口")
public class FlowController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FlowController.class);




    @Autowired
    public FlowController(FlowService flowService, FlowDeployService flowDeployService, FlowCopyService flowCopyService) {
        this.flowService = flowService;
        this.flowDeployService = flowDeployService;
        this.flowCopyService = flowCopyService;
    }
    private final FlowService flowService;
    private final FlowDeployService flowDeployService;
    private final FlowCopyService flowCopyService;

    /**
     * #17 创建连接流
     */
    @AuditLog(value = OperateEnum.CREATE_FLOW, resourceIdParam = "appId")
    @PostMapping
    @Operation(summary = "#17 创建连接流", description = "创建连接流基本信息，lifecycleStatus=1（已停止），不自动生成草稿版本")
    public ResponseEntity<ApiResponse<FlowCreateResponse>> createFlow(
            @RequestHeader("X-App-Id") Long appId,
            @Valid @RequestBody FlowCreateRequest request) {
        log.info("POST /flows - create flow: nameCn={}, appId={}", request.getNameCn(), appId);
        return toResponseEntity(flowService.createFlow(request, appId));
    }

    /**
     * #18 查询连接流列表
     */
    @GetMapping
    @Operation(summary = "#18 查询连接流列表", description = "列表查询，支持 lifecycleStatus/keyword 过滤 + 分页")
    public ResponseEntity<ApiResponse<List<FlowListResponse>>> getFlowList(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "生命周期状态过滤") @RequestParam(required = false) Integer lifecycleStatus,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码") @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量") @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        log.info("GET /flows - list: appId={}, lifecycleStatus={}, keyword={}", appId, lifecycleStatus, keyword);
        return toResponseEntity(flowService.getFlowList(lifecycleStatus, keyword, curPage, pageSize, appId));
    }

    /**
     * #19 查询连接流详情
     */
    @GetMapping("/{flowId}")
    @Operation(summary = "#19 查询连接流详情", description = "详情查询，含 invokeUrl")
    public ResponseEntity<ApiResponse<FlowDetailResponse>> getFlowDetail(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            HttpServletRequest request) {
        log.info("GET /flows/{} - detail: appId={}", flowId, appId);

        // 构建 invokeUrl 前缀
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String invokeUrlPrefix = scheme + "://" + host;
        if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
            invokeUrlPrefix += ":" + port;
        }

        return toResponseEntity(flowService.getFlowDetail(flowId, appId, invokeUrlPrefix));
    }

    /**
     * #20 编辑连接流基本信息
     */
    @AuditLog(value = OperateEnum.UPDATE_FLOW, resourceIdParam = "flowId")
    @PutMapping("/{flowId}")
    @Operation(summary = "#20 更新连接流", description = "更新名称和描述信息")
    public ResponseEntity<ApiResponse<Void>> updateFlow(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Valid @RequestBody FlowUpdateRequest request) {
        log.info("PUT /flows/{} - update: appId={}", flowId, appId);
        return toResponseEntity(flowService.updateFlow(flowId, request, appId));
    }

    /**
     * #21 复制连接流
     */
    @PostMapping("/{flowId}/copy")
    @Operation(summary = "#21 复制连接流", description = "复制全部版本历史，名称追加 _copy_xxxxx")
    public ResponseEntity<ApiResponse<FlowCopyResponse>> copyFlow(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "源连接流ID") @PathVariable Long flowId) {
        log.info("POST /flows/{}/copy: appId={}", flowId, appId);
        return toResponseEntity(flowCopyService.copyFlow(flowId, appId));
    }

    /**
     * #22 部署连接流
     */
    @AuditLog(value = OperateEnum.DEPLOY_FLOW, resourceIdParam = "flowId")
    @PostMapping("/{flowId}/deploy")
    @Operation(summary = "#22 部署连接流", description = "部署版本，纯版本绑定，不改变生命周期状态")
    public ResponseEntity<ApiResponse<FlowDeployResponse>> deployFlow(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Valid @RequestBody FlowDeployRequest request) {
        log.info("POST /flows/{}/deploy: appId={}, versionId={}", flowId, appId, request.getVersionId());
        return toResponseEntity(flowDeployService.deployVersion(flowId, request.getVersionId(), appId));
    }

    /**
     * #23 启动连接流
     */
    @AuditLog(value = OperateEnum.START_FLOW, resourceIdParam = "flowId")
    @PostMapping("/{flowId}/start")
    @Operation(summary = "#23 启动连接流", description = "启动（需有已部署版本），状态 1→2")
    public ResponseEntity<ApiResponse<FlowLifecycleResponse>> startFlow(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId) {
        log.info("POST /flows/{}/start: appId={}", flowId, appId);
        return toResponseEntity(flowService.startFlow(flowId, appId));
    }

    /**
     * #24 停止连接流
     */
    @AuditLog(value = OperateEnum.STOP_FLOW, resourceIdParam = "flowId")
    @PostMapping("/{flowId}/stop")
    @Operation(summary = "#24 停止连接流", description = "停止（仅运行中），状态 2→1")
    public ResponseEntity<ApiResponse<FlowLifecycleResponse>> stopFlow(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId) {
        log.info("POST /flows/{}/stop: appId={}", flowId, appId);
        return toResponseEntity(flowService.stopFlow(flowId, appId));
    }

    /**
     * #25 失效连接流
     */
    @AuditLog(value = OperateEnum.INVALIDATE_FLOW, resourceIdParam = "flowId")
    @PutMapping("/{flowId}/invalidate")
    @Operation(summary = "#25 失效连接流", description = "标记失效（仅已停止状态），状态 1→3")
    public ResponseEntity<ApiResponse<?>> invalidateFlow(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId) {
        log.info("PUT /flows/{}/invalidate: appId={}", flowId, appId);
        return toResponseEntity(flowService.invalidateFlow(flowId, appId));
    }

    /**
     * #26 恢复连接流
     */
    @AuditLog(value = OperateEnum.RECOVER_FLOW, resourceIdParam = "flowId")
    @PutMapping("/{flowId}/recover")
    @Operation(summary = "#26 恢复连接流", description = "恢复 → 已停止状态（需手动启动）")
    public ResponseEntity<ApiResponse<?>> recoverFlow(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId) {
        log.info("PUT /flows/{}/recover: appId={}", flowId, appId);
        return toResponseEntity(flowService.recoverFlow(flowId, appId));
    }

    /**
     * #27 删除连接流
     */
    @AuditLog(value = OperateEnum.DELETE_FLOW, resourceIdParam = "flowId")
    @DeleteMapping("/{flowId}")
    @Operation(summary = "#27 删除连接流", description = "物理删除（仅已失效状态可删除）")
    public ResponseEntity<ApiResponse<Void>> deleteFlow(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId) {
        log.info("DELETE /flows/{}: appId={}", flowId, appId);
        return toResponseEntity(flowService.deleteFlow(flowId, appId));
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
