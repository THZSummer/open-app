package com.xxx.it.works.wecode.v2.modules.sync.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
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

        log.info("Received rollback request, ids={}", request.getIds());

        SyncResult result = syncService.rollback(request);

        log.info("Rollback completed, success={}, failed={}, skipped={}",
                result.getSuccess(), result.getFailed(), result.getSkipped());

        return ApiResponse.success(result);
    }
}