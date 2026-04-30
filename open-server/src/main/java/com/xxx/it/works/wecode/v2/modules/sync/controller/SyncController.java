package com.xxx.it.works.wecode.v2.modules.sync.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.sync.dto.EmergencyRequest;
import com.xxx.it.works.wecode.v2.modules.sync.dto.EmergencyResult;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SyncRequest;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SyncResult;
import com.xxx.it.works.wecode.v2.modules.sync.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 数据同步 Controller
 *
 * <p>提供订阅关系数据的双向同步接口：</p>
 * <ul>
 *   <li>POST /service/open/v2/sync/subscription/migrate - 旧表→新表（迁移）</li>
 *   <li>POST /service/open/v2/sync/subscription/rollback - 新表→旧表（回退）</li>
 * </ul>
 *
 * <p>接口特性：</p>
 * <ul>
 *   <li>支持批量同步（传入ID列表）和全量同步（不传ID或传空数组）</li>
 *   <li>同步订阅关系时，自动同步关联的审批记录和审批日志</li>
 *   <li>支持重复执行（幂等性）</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/sync")
@RequiredArgsConstructor
@Tag(name = "数据同步", description = "订阅关系数据双向同步接口")
public class SyncController {

    private final SyncService syncService;

    /**
     * 迁移数据：旧表 → 新表
     *
     * <p>将旧系统的订阅关系数据迁移到新系统</p>
     * <ul>
     *   <li>自动查找新权限ID（通过API path+method 或 事件 topic 匹配）</li>
     *   <li>事件订阅自动获取通道配置</li>
     *   <li>自动同步关联的审批记录和审批日志</li>
     * </ul>
     *
     * @param request 同步请求，ids=null或空数组表示全量同步
     * @return 同步结果
     */
    @PostMapping("/subscription/migrate")
    @Operation(summary = "迁移数据（旧表→新表）",
               description = "将旧系统的订阅关系数据迁移到新系统，自动同步审批数据")
    public ApiResponse<SyncResult> migrate(
            @Parameter(description = "同步请求，ids=null或空数组表示全量同步")
            @Valid @RequestBody SyncRequest request) {

        checkPermission("sync:migrate");

        log.info("Received migration request, ids={}", request.getIds());

        SyncResult result = syncService.migrate(request);

        log.info("Migration completed, success={}, failed={}, skipped={}",
                result.getSuccess(), result.getFailed(), result.getSkipped());

        return ApiResponse.success(result);
    }

    /**
     * 回退数据：新表 → 旧表
     *
     * <p>将新系统的订阅关系数据回退到旧系统</p>
     * <ul>
     *   <li>自动查找旧权限ID</li>
     *   <li>自动同步关联的审批记录和审批日志</li>
     * </ul>
     *
     * @param request 同步请求，ids=null或空数组表示全量同步
     * @return 同步结果
     */
    @PostMapping("/subscription/rollback")
    @Operation(summary = "回退数据（新表→旧表）",
               description = "将新系统的订阅关系数据回退到旧系统，自动同步审批数据")
    public ApiResponse<SyncResult> rollback(
            @Parameter(description = "同步请求，ids=null或空数组表示全量同步")
            @Valid @RequestBody SyncRequest request) {

        checkPermission("sync:rollback");

        log.info("Received rollback request, ids={}", request.getIds());

        SyncResult result = syncService.rollback(request);

        log.info("Rollback completed, success={}, failed={}, skipped={}",
                result.getSuccess(), result.getFailed(), result.getSkipped());

        return ApiResponse.success(result);
    }

    /**
     * 应急更新旧订阅关系表数据
     *
     * <p>直接更新旧系统的订阅关系表，无需业务校验</p>
     * <ul>
     *   <li>根据主键ID判断记录是否存在</li>
     *   <li>存在则更新，不存在则新增（需通过数据保护校验）</li>
     *   <li>数据保护：新增时检查 app_id + permission_id 组合是否已存在</li>
     * </ul>
     *
     * @param request 应急更新请求
     * @return 应急更新结果
     */
    @PostMapping("/subscription/emergency/update-old")
    @Operation(summary = "应急更新旧订阅关系表",
               description = "直接更新旧订阅关系表数据，自动新增或更新，带基本数据保护")
    public ApiResponse<EmergencyResult> emergencyUpdateOld(
            @Parameter(description = "应急更新请求")
            @Valid @RequestBody EmergencyRequest request) {

        checkSuperAdminPermission();

        log.info("Received emergency update old request, count={}", 
                request.getSubscriptions() != null ? request.getSubscriptions().size() : 0);

        EmergencyResult result = syncService.emergencyUpdateOld(request);

        log.info("Emergency update old completed, success={}, failed={}, inserted={}, updated={}",
                result.getSuccess(), result.getFailed(), result.getInserted(), result.getUpdated());

        return ApiResponse.success(result);
    }

    /**
     * 应急更新新订阅关系表数据
     *
     * <p>直接更新新系统的订阅关系表，无需业务校验</p>
     * <ul>
     *   <li>根据主键ID判断记录是否存在</li>
     *   <li>存在则更新，不存在则新增（需通过数据保护校验）</li>
     *   <li>数据保护：新增时检查 app_id + permission_id 组合是否已存在</li>
     * </ul>
     *
     * @param request 应急更新请求
     * @return 应急更新结果
     */
    @PostMapping("/subscription/emergency/update-new")
    @Operation(summary = "应急更新新订阅关系表",
               description = "直接更新新订阅关系表数据，自动新增或更新，带基本数据保护")
    public ApiResponse<EmergencyResult> emergencyUpdateNew(
            @Parameter(description = "应急更新请求")
            @Valid @RequestBody EmergencyRequest request) {

        checkSuperAdminPermission();

        log.info("Received emergency update new request, count={}", 
                request.getSubscriptions() != null ? request.getSubscriptions().size() : 0);

        EmergencyResult result = syncService.emergencyUpdateNew(request);

        log.info("Emergency update new completed, success={}, failed={}, inserted={}, updated={}",
                result.getSuccess(), result.getFailed(), result.getInserted(), result.getUpdated());

        return ApiResponse.success(result);
    }

    // ==================== 权限校验（预留） ====================

    /**
     * 权限校验：检查用户是否有执行同步操作的权限
     * 
     * <p>当前为预留方法，暂时跳过校验</p>
     * <p>后续集成统一权限管理模块后启用</p>
     *
     * @param permissionCode 权限码
     * @throws PermissionDeniedException 无权限时抛出异常
     */
    private void checkPermission(String permissionCode) {
        // TODO: 权限校验逻辑（后续集成）
        // 示例实现：
        // String currentUser = getCurrentUser();
        // if (!permissionService.hasPermission(currentUser, permissionCode)) {
        //     throw new PermissionDeniedException(
        //         "无权限执行此操作: " + permissionCode,
        //         "Permission denied: " + permissionCode
        //     );
        // }
        
        log.debug("Permission check passed (currently skipped), permissionCode={}", permissionCode);
    }

    /**
     * 权限校验：检查用户是否为超级管理员
     * 
     * <p>当前为预留方法，暂时跳过校验</p>
     * <p>应急接口需要超级管理员权限</p>
     *
     * @throws PermissionDeniedException 非超级管理员时抛出异常
     */
    private void checkSuperAdminPermission() {
        // TODO: 超级管理员权限校验（后续集成）
        // 示例实现：
        // String currentUser = getCurrentUser();
        // if (!permissionService.isSuperAdmin(currentUser)) {
        //     throw new PermissionDeniedException(
        //         "仅超级管理员可执行此操作",
        //         "Only super admin can perform this operation"
        //     );
        // }
        
        log.debug("Super admin permission check passed (currently skipped)");
    }
}
