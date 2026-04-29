package com.xxx.it.works.wecode.v2.modules.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SyncDetail;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SyncRequest;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SyncResult;
import com.xxx.it.works.wecode.v2.modules.sync.entity.*;
import com.xxx.it.works.wecode.v2.modules.sync.mapper.SyncMapper;
import com.xxx.it.works.wecode.v2.modules.permission.entity.Subscription;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalLog;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import com.xxx.it.works.wecode.v2.modules.event.entity.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 数据同步服务
 *
 * <p>提供订阅关系数据的双向同步功能：</p>
 * <ul>
 *   <li>migrate: 旧表 → 新表（迁移）</li>
 *   <li>rollback: 新表 → 旧表（回退）</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SyncMapper syncMapper;
    private final ObjectMapper objectMapper;

    // ==================== 迁移：旧表 → 新表 ====================

    /**
     * 迁移数据：旧表 → 新表
     */
    @Transactional(rollbackFor = Exception.class)
    public SyncResult migrate(SyncRequest request) {
        log.info("Starting data migration, ids={}", request.getIds());

        List<SyncDetail> details = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int skipped = 0;

        // 1. 查询旧订阅关系
        List<OldSubscription> oldSubscriptions = syncMapper.selectOldSubscriptions(request.getIds());
        log.info("Found {} old subscriptions", oldSubscriptions.size());

        for (OldSubscription oldSub : oldSubscriptions) {
            SyncDetail detail = SyncDetail.builder()
                    .id(oldSub.getId())
                    .build();

            try {

                // 2. 检查新表是否已存在
                Subscription existing = syncMapper.selectNewSubscriptionById(oldSub.getId());
                if (existing != null) {
                    detail.setStatus("skipped");
                    detail.setError("订阅关系已存在");
                    skipped++;
                    details.add(detail);
                    continue;
                }

                // 3. 查找新权限ID
                Long newPermissionId = findNewPermissionId(oldSub);
                if (newPermissionId == null) {
                    detail.setStatus("failed");
                    detail.setError("未找到对应的新权限，请先在新系统注册API/事件");
                    failed++;
                    details.add(detail);
                    continue;
                }

                // 4. 获取通道配置（仅事件订阅）
                Integer channelType = null;
                String channelAddress = null;
                Integer authType = null;

                log.info("Subscription {}, permissionType: {}, permissionId: {}", 
                    oldSub.getId(), oldSub.getPermisssionType(), oldSub.getPermissionId());

                if ("1".equals(oldSub.getPermisssionType())) {
                    OldAppProperty channelConfig = syncMapper.selectAppChannelConfig(oldSub.getAppId());
                    log.info("Event subscription {}, channelConfig: {}", oldSub.getId(), channelConfig);
                    if (channelConfig != null) {
                        channelType = channelConfig.getChannelType();
                        channelAddress = channelConfig.getChannelAddress();
                        authType = channelConfig.getAuthType();
                    }
                } else if ("0".equals(oldSub.getPermisssionType())) {
                    OldApi oldApi = syncMapper.selectOldApiByPermissionId(oldSub.getPermissionId());
                    log.info("API subscription {}, oldApi: {}", oldSub.getId(), oldApi);
                    if (oldApi != null) {
                        authType = oldApi.getAuthType();
                    }
                }

                log.info("Subscription {} final authType: {}", oldSub.getId(), authType);

                // 5. 构造新订阅关系
                Subscription newSub = new Subscription();
                newSub.setId(oldSub.getId());
                newSub.setAppId(oldSub.getAppId());
                newSub.setPermissionId(newPermissionId);
                newSub.setStatus(oldSub.getStatus());
                newSub.setChannelType(channelType);
                newSub.setChannelAddress(channelAddress);
                newSub.setAuthType(authType);
                newSub.setCreateTime(oldSub.getCreateTime());
                newSub.setLastUpdateTime(oldSub.getLastUpdateTime());
                newSub.setCreateBy(oldSub.getCreateBy());
                newSub.setLastUpdateBy(oldSub.getLastUpdateBy());

                // 6. 写入新订阅关系
                int inserted = syncMapper.insertNewSubscriptionIfNotExists(newSub);
                if (inserted > 0) {
                    success++;
                    detail.setStatus("success");

                    // 7. 同步审批记录（失败不影响订阅关系）
                    syncApprovalRecordsMigrate(oldSub, detail);
                } else {
                    detail.setStatus("skipped");
                    detail.setError("订阅关系已存在");
                    skipped++;
                }

            } catch (Exception e) {
                log.error("Failed to migrate subscription, id={}", oldSub.getId(), e);
                detail.setStatus("failed");
                detail.setError(e.getMessage());
                failed++;
            }

            details.add(detail);
        }

        return SyncResult.builder()
                .success(success)
                .failed(failed)
                .skipped(skipped)
                .details(details)
                .build();
    }

    /**
     * 查找新权限ID
     */
    private Long findNewPermissionId(OldSubscription oldSub) {

        // 1. 查询旧权限
        OldPermission oldPermission = syncMapper.selectOldPermissionById(oldSub.getPermissionId());
        if (oldPermission == null) {
            log.warn("Old permission not found, permissionId={}", oldSub.getPermissionId());
            return null;
        }

        String resourceType = oldPermission.getPermisssionType();
        Long resourceId = null;

        // 2. 根据类型查找对应的新资源
        if ("api".equalsIgnoreCase(resourceType) || "0".equals(oldSub.getPermisssionType())) {

            // API 权限
            OldApi oldApi = syncMapper.selectOldApiById(oldPermission.getModuleId());
            if (oldApi == null) {
                log.warn("Old API not found, id={}", oldPermission.getModuleId());
                return null;
            }

            // 通过 path+method 匹配新API
            Api newApi = syncMapper.selectNewApiByPathAndMethod(oldApi.getPath(), oldApi.getMethod());
            if (newApi == null) {
                log.warn("New API not found, path={}, method={}", oldApi.getPath(), oldApi.getMethod());
                return null;
            }
            resourceId = newApi.getId();

        } else if ("event".equalsIgnoreCase(resourceType) || "1".equals(oldSub.getPermisssionType())) {

            // 事件权限
            OldEvent oldEvent = syncMapper.selectOldEventById(oldPermission.getModuleId());
            if (oldEvent == null) {
                log.warn("Old event not found, id={}", oldPermission.getModuleId());
                return null;
            }

            // 通过 topic 匹配新事件
            Event newEvent = syncMapper.selectNewEventByTopic(oldEvent.getTopic());
            if (newEvent == null) {
                log.warn("New event not found, topic={}", oldEvent.getTopic());
                return null;
            }
            resourceId = newEvent.getId();

        } else {
            log.warn("Unknown resource type, type={}", resourceType);
            return null;
        }

        // 3. 查找新权限
        Permission newPermission = syncMapper.selectNewPermissionByResource(resourceType, resourceId);
        return newPermission != null ? newPermission.getId() : null;
    }

    /**
     * 同步审批记录（迁移）
     */
    private void syncApprovalRecordsMigrate(OldSubscription oldSub, SyncDetail detail) {
        try {

            // 查询旧审批记录
            List<OldEflow> oldEflows = syncMapper.selectOldEflowByResourceId("app_permission", oldSub.getId());
            if (oldEflows == null || oldEflows.isEmpty()) {
                detail.setApprovalStatus("无审批记录");
                return;
            }

            int recordCount = 0;
            int logCount = 0;
            for (OldEflow oldEflow : oldEflows) {

                // 检查是否已存在
                ApprovalRecord existing = syncMapper.selectNewApprovalRecordById(oldEflow.getEflowId());
                if (existing != null) {
                    continue;
                }

                // 构造 combinedNodes
                String combinedNodes = constructCombinedNodes(oldEflow.getEflowAuditUser());

                // 构造新审批记录
                ApprovalRecord record = new ApprovalRecord();
                record.setId(oldEflow.getEflowId());
                record.setCombinedNodes(combinedNodes);
                record.setBusinessType("api_permission_apply".equals(oldEflow.getEflowType()) ?
                        "api_permission_apply" : "event_permission_apply");
                record.setBusinessId(oldSub.getId());
                record.setApplicantId(oldEflow.getEflowSubmitUser());
                record.setStatus(oldEflow.getEflowStatus());
                record.setCreateTime(oldEflow.getCreateTime());

                // 写入新审批记录
                if (syncMapper.insertNewApprovalRecordIfNotExists(record) > 0) {
                    recordCount++;
                }

                // 同步审批日志
                List<OldEflowLog> oldLogs = syncMapper.selectOldEflowLogByEflowId(oldEflow.getEflowId());
                if (oldLogs != null && !oldLogs.isEmpty()) {
                    List<ApprovalLog> newLogs = new ArrayList<>();
                    for (OldEflowLog oldLog : oldLogs) {
                        ApprovalLog newLog = new ApprovalLog();
                        newLog.setId(oldLog.getEflowLogId());
                        newLog.setRecordId(oldEflow.getEflowId());
                        newLog.setNodeIndex(0);
                        newLog.setLevel("resource");
                        newLog.setOperatorId(oldLog.getEflowLogUser());
                        newLog.setComment(oldLog.getEflowLogMessage());
                        newLog.setAction(convertAction(oldLog.getEflowLogType()));
                        newLog.setCreateTime(oldLog.getCreateTime());
                        newLogs.add(newLog);
                    }

                    if (!newLogs.isEmpty()) {
                        syncMapper.batchInsertNewApprovalLogs(newLogs);
                        logCount += newLogs.size();
                    }
                }
            }

            detail.setApprovalStatus("同步" + recordCount + "条审批记录");
            detail.setApprovalLogStatus("同步" + logCount + "条审批日志");

        } catch (Exception e) {
            log.error("Failed to sync approval records, subscriptionId={}", oldSub.getId(), e);
            detail.setApprovalStatus("失败: " + e.getMessage());
        }
    }

    /**
     * 构造 combinedNodes JSON
     */
    protected String constructCombinedNodes(String auditUser) {
        if (auditUser == null || auditUser.isEmpty()) {
            return null;
        }

        try {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("level", "审批人");
            node.put("approver", auditUser);

            List<Map<String, Object>> nodes = new ArrayList<>();
            nodes.add(node);

            Map<String, Object> combinedNodes = new LinkedHashMap<>();
            combinedNodes.put("nodes", nodes);

            return objectMapper.writeValueAsString(combinedNodes);
        } catch (Exception e) {
            log.error("Failed to construct combinedNodes", e);
            return null;
        }
    }

    /**
     * 转换操作类型
     */
    protected Integer convertAction(String eflowLogType) {
        if (eflowLogType == null) {
            return 0;
        }

        switch (eflowLogType.toLowerCase(Locale.ROOT)) {
            case "approve":
            case "同意":
                return 0;
            case "reject":
            case "拒绝":
                return 1;
            case "cancel":
            case "撤销":
                return 2;
            case "transfer":
            case "转交":
                return 3;
            default:
                return 0;
        }
    }

    // ==================== 回退：新表 → 旧表 ====================

    /**
     * 回退数据：新表 → 旧表
     */
    @Transactional(rollbackFor = Exception.class)
    public SyncResult rollback(SyncRequest request) {
        log.info("Starting data rollback, ids={}", request.getIds());

        List<SyncDetail> details = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int skipped = 0;

        // 1. 查询新订阅关系
        List<Subscription> newSubscriptions = syncMapper.selectNewSubscriptions(request.getIds());
        log.info("Found {} new subscriptions", newSubscriptions.size());

        for (Subscription newSub : newSubscriptions) {
            SyncDetail detail = SyncDetail.builder()
                    .id(newSub.getId())
                    .build();

            try {

                // 2. 检查旧表是否已存在
                OldSubscription existing = syncMapper.selectOldSubscriptionById(newSub.getId());
                if (existing != null) {
                    detail.setStatus("skipped");
                    detail.setError("订阅关系已存在");
                    skipped++;
                    details.add(detail);
                    continue;
                }

                // 3. 反向查找旧权限ID
                Long oldPermissionId = findOldPermissionId(newSub);
                if (oldPermissionId == null) {
                    detail.setStatus("failed");
                    detail.setError("未找到对应的旧权限");
                    failed++;
                    details.add(detail);
                    continue;
                }

                // 4. 构造旧订阅关系
                OldSubscription oldSub = new OldSubscription();
                oldSub.setId(newSub.getId());
                oldSub.setAppId(newSub.getAppId());
                oldSub.setPermissionId(oldPermissionId);
                oldSub.setStatus(newSub.getStatus());
                oldSub.setCreateTime(newSub.getCreateTime());
                oldSub.setLastUpdateTime(newSub.getLastUpdateTime());
                oldSub.setCreateBy(newSub.getCreateBy());
                oldSub.setLastUpdateBy(newSub.getLastUpdateBy());

                // 5. 写入旧订阅关系
                int inserted = syncMapper.insertOldSubscriptionIfNotExists(oldSub);
                if (inserted > 0) {
                    success++;
                    detail.setStatus("success");

                    // 6. 同步审批记录
                    syncApprovalRecordsRollback(newSub, detail);
                } else {
                    detail.setStatus("skipped");
                    detail.setError("订阅关系已存在");
                    skipped++;
                }

            } catch (Exception e) {
                log.error("Failed to rollback subscription, id={}", newSub.getId(), e);
                detail.setStatus("failed");
                detail.setError(e.getMessage());
                failed++;
            }

            details.add(detail);
        }

        return SyncResult.builder()
                .success(success)
                .failed(failed)
                .skipped(skipped)
                .details(details)
                .build();
    }

    /**
     * 反向查找旧权限ID
     */
    private Long findOldPermissionId(Subscription newSub) {

        // 1. 查询新权限
        Permission newPermission = syncMapper.selectNewPermissionById(newSub.getPermissionId());
        if (newPermission == null) {
            log.warn("New permission not found, permissionId={}", newSub.getPermissionId());
            return null;
        }

        String resourceType = newPermission.getResourceType();
        Long resourceId = newPermission.getResourceId();

        // 2. 根据类型查找对应的旧资源
        if ("api".equalsIgnoreCase(resourceType)) {
            // 查询新API详情
            Api newApi = syncMapper.selectNewApiById(resourceId);
            if (newApi == null) {
                log.warn("New API not found, id={}", resourceId);
                return null;
            }

            // 通过 path+method 匹配旧API
            OldApi oldApi = syncMapper.selectOldApiByPathAndMethod(newApi.getPath(), newApi.getMethod());
            if (oldApi == null) {
                log.warn("Old API not found, path={}, method={}", newApi.getPath(), newApi.getMethod());
                return null;
            }

            // 返回旧API关联的权限ID
            return oldApi.getPermissionId();

        } else if ("event".equalsIgnoreCase(resourceType)) {
            // 查询新Event详情
            Event newEvent = syncMapper.selectNewEventById(resourceId);
            if (newEvent == null) {
                log.warn("New event not found, id={}", resourceId);
                return null;
            }

            // 通过 topic 匹配旧Event
            OldEvent oldEvent = syncMapper.selectOldEventByTopic(newEvent.getTopic());
            if (oldEvent == null) {
                log.warn("Old event not found, topic={}", newEvent.getTopic());
                return null;
            }

            // 旧Event没有直接关联权限ID，需要查找对应的旧权限
            // 旧权限通过 module_id 关联旧Event
            OldPermission oldPermission = syncMapper.selectOldPermissionByModuleIdAndType(oldEvent.getId(), "event");
            if (oldPermission == null) {
                log.warn("Old permission not found for event, moduleId={}", oldEvent.getId());
                return null;
            }
            return oldPermission.getId();
        }

        log.warn("Unknown resource type, type={}", resourceType);
        return null;
    }

    /**
     * 同步审批记录（回退）
     */
    private void syncApprovalRecordsRollback(Subscription newSub, SyncDetail detail) {

        // TODO: 实现审批记录回退逻辑
        detail.setApprovalStatus("回退暂不支持审批数据");
    }
}