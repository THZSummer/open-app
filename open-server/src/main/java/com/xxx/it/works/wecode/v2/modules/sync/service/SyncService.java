package com.xxx.it.works.wecode.v2.modules.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.sync.dto.EmergencyDetail;
import com.xxx.it.works.wecode.v2.modules.sync.dto.EmergencyRequest;
import com.xxx.it.works.wecode.v2.modules.sync.dto.EmergencyResult;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SubscriptionData;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SyncMapper syncMapper;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public SyncResult migrate(SyncRequest request) {
        log.info("Starting data migration, ids={}", request.getIds());

        List<SyncDetail> details = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int skipped = 0;

        List<OldSubscription> oldSubscriptions = syncMapper.selectOldSubscriptions(request.getIds());
        log.info("Found {} old subscriptions", oldSubscriptions.size());

        for (OldSubscription oldSub : oldSubscriptions) {
            SyncDetail detail = SyncDetail.builder()
                    .id(oldSub.getId())
                    .build();

            try {

                Subscription existing = syncMapper.selectNewSubscriptionById(oldSub.getId());
                if (existing != null) {
                    detail.setStatus("skipped");
                    detail.setError("订阅关系已存在");
                    skipped++;
                    details.add(detail);
                    continue;
                }

                Long newPermissionId = findNewPermissionId(oldSub);
                if (newPermissionId == null) {
                    detail.setStatus("failed");
                    detail.setError("未找到对应的新权限，请先在新系统注册API/事件");
                    failed++;
                    details.add(detail);
                    continue;
                }

                Integer channelType = null;
                String channelAddress = null;
                Integer authType = null;

                log.info("Subscription {}, permissionType: {}, permissionId: {}", 
                    oldSub.getId(), oldSub.getPermissionType(), oldSub.getPermissionId());

                if ("1".equals(oldSub.getPermissionType())) {
                    OldAppProperty channelConfig = syncMapper.selectAppChannelConfig(oldSub.getAppId());
                    log.info("Event subscription {}, channelConfig: {}", oldSub.getId(), channelConfig);
                    if (channelConfig != null) {
                        channelType = channelConfig.getChannelType();
                        channelAddress = channelConfig.getChannelAddress();
                        authType = channelConfig.getAuthType();
                    }
                } else if ("0".equals(oldSub.getPermissionType())) {
                    OldApi oldApi = syncMapper.selectOldApiByPermissionId(oldSub.getPermissionId());
                    log.info("API subscription {}, oldApi: {}", oldSub.getId(), oldApi);
                    if (oldApi != null) {
                        authType = oldApi.getAuthType();
                    }
                }

                log.info("Subscription {} final authType: {}", oldSub.getId(), authType);

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

                int inserted = syncMapper.insertNewSubscriptionIfNotExists(newSub);
                if (inserted > 0) {
                    success++;
                    detail.setStatus("success");

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

    private Long findNewPermissionId(OldSubscription oldSub) {

        OldPermission oldPermission = syncMapper.selectOldPermissionById(oldSub.getPermissionId());
        if (oldPermission == null) {
            log.warn("Old permission not found, permissionId={}", oldSub.getPermissionId());
            return null;
        }

        String resourceType = oldPermission.getPermissionType();
        Long resourceId = null;

        if ("api".equalsIgnoreCase(resourceType) || "0".equals(oldSub.getPermissionType())) {

            OldApi oldApi = syncMapper.selectOldApiById(oldPermission.getModuleId());
            if (oldApi == null) {
                log.warn("Old API not found, id={}", oldPermission.getModuleId());
                return null;
            }

            Api newApi = syncMapper.selectNewApiByPathAndMethod(oldApi.getPath(), oldApi.getMethod());
            if (newApi == null) {
                log.warn("New API not found, path={}, method={}", oldApi.getPath(), oldApi.getMethod());
                return null;
            }
            resourceId = newApi.getId();

        } else if ("event".equalsIgnoreCase(resourceType) || "1".equals(oldSub.getPermissionType())) {

            OldEvent oldEvent = syncMapper.selectOldEventById(oldPermission.getModuleId());
            if (oldEvent == null) {
                log.warn("Old event not found, id={}", oldPermission.getModuleId());
                return null;
            }

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

        Permission newPermission = syncMapper.selectNewPermissionByResource(resourceType, resourceId);
        return newPermission != null ? newPermission.getId() : null;
    }

    private void syncApprovalRecordsMigrate(OldSubscription oldSub, SyncDetail detail) {
        try {

            List<OldEflow> oldEflows = syncMapper.selectOldEflowByResourceId("app_permission", oldSub.getId());
            if (oldEflows == null || oldEflows.isEmpty()) {
                detail.setApprovalStatus("无审批记录");
                return;
            }

            int recordCount = 0;
            int logCount = 0;
            for (OldEflow oldEflow : oldEflows) {

                ApprovalRecord existing = syncMapper.selectNewApprovalRecordById(oldEflow.getEflowId());
                if (existing != null) {
                    continue;
                }

                String combinedNodes = constructCombinedNodes(oldEflow.getEflowAuditUser());

                ApprovalRecord record = new ApprovalRecord();
                record.setId(oldEflow.getEflowId());
                record.setCombinedNodes(combinedNodes);
                record.setBusinessType("api_permission_apply".equals(oldEflow.getEflowType()) ?
                        "api_permission_apply" : "event_permission_apply");
                record.setBusinessId(oldSub.getId());
                record.setApplicantId(oldEflow.getEflowSubmitUser());
                record.setStatus(oldEflow.getEflowStatus());
                record.setCreateTime(oldEflow.getCreateTime());

                if (syncMapper.insertNewApprovalRecordIfNotExists(record) > 0) {
                    recordCount++;
                }

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

    @Transactional(rollbackFor = Exception.class)
    public SyncResult rollback(SyncRequest request) {
        log.info("Starting data rollback, ids={}", request.getIds());

        List<SyncDetail> details = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int skipped = 0;

        List<Subscription> newSubscriptions = syncMapper.selectNewSubscriptions(request.getIds());
        log.info("Found {} new subscriptions", newSubscriptions.size());

        for (Subscription newSub : newSubscriptions) {
            SyncDetail detail = SyncDetail.builder()
                    .id(newSub.getId())
                    .build();

            try {

                OldSubscription existing = syncMapper.selectOldSubscriptionById(newSub.getId());
                if (existing != null) {
                    detail.setStatus("skipped");
                    detail.setError("订阅关系已存在");
                    skipped++;
                    details.add(detail);
                    continue;
                }

                Long oldPermissionId = findOldPermissionId(newSub);
                if (oldPermissionId == null) {
                    detail.setStatus("failed");
                    detail.setError("未找到对应的旧权限");
                    failed++;
                    details.add(detail);
                    continue;
                }

                OldSubscription oldSub = new OldSubscription();
                oldSub.setId(newSub.getId());
                oldSub.setAppId(newSub.getAppId());
                oldSub.setPermissionId(oldPermissionId);
                oldSub.setStatus(newSub.getStatus());
                oldSub.setCreateTime(newSub.getCreateTime());
                oldSub.setLastUpdateTime(newSub.getLastUpdateTime());
                oldSub.setCreateBy(newSub.getCreateBy());
                oldSub.setLastUpdateBy(newSub.getLastUpdateBy());

                int inserted = syncMapper.insertOldSubscriptionIfNotExists(oldSub);
                if (inserted > 0) {
                    success++;
                    detail.setStatus("success");

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

    private Long findOldPermissionId(Subscription newSub) {

        Permission newPermission = syncMapper.selectNewPermissionById(newSub.getPermissionId());
        if (newPermission == null) {
            log.warn("New permission not found, permissionId={}", newSub.getPermissionId());
            return null;
        }

        String resourceType = newPermission.getResourceType();
        Long resourceId = newPermission.getResourceId();

        if ("api".equalsIgnoreCase(resourceType)) {
            Api newApi = syncMapper.selectNewApiById(resourceId);
            if (newApi == null) {
                log.warn("New API not found, id={}", resourceId);
                return null;
            }

            OldApi oldApi = syncMapper.selectOldApiByPathAndMethod(newApi.getPath(), newApi.getMethod());
            if (oldApi == null) {
                log.warn("Old API not found, path={}, method={}", newApi.getPath(), newApi.getMethod());
                return null;
            }

            return oldApi.getPermissionId();

        } else if ("event".equalsIgnoreCase(resourceType)) {
            Event newEvent = syncMapper.selectNewEventById(resourceId);
            if (newEvent == null) {
                log.warn("New event not found, id={}", resourceId);
                return null;
            }

            OldEvent oldEvent = syncMapper.selectOldEventByTopic(newEvent.getTopic());
            if (oldEvent == null) {
                log.warn("Old event not found, topic={}", newEvent.getTopic());
                return null;
            }

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

    private void syncApprovalRecordsRollback(Subscription newSub, SyncDetail detail) {

        detail.setApprovalStatus("回退暂不支持审批数据");
    }

    // ==================== 应急接口：直接更新订阅关系表 ====================

    @Transactional(rollbackFor = Exception.class)
    public EmergencyResult emergencyUpdateOld(EmergencyRequest request) {
        log.info("Starting emergency update old table");

        List<EmergencyDetail> details = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int inserted = 0;
        int updated = 0;

        List<SubscriptionData> subscriptions = request.getSubscriptions();
        if (subscriptions == null || subscriptions.isEmpty()) {
            return EmergencyResult.builder()
                    .success(0)
                    .failed(0)
                    .inserted(0)
                    .updated(0)
                    .details(details)
                    .build();
        }

        for (SubscriptionData data : subscriptions) {
            EmergencyDetail detail = EmergencyDetail.builder()
                    .id(data.getId())
                    .build();

            try {
                OldSubscription existing = syncMapper.selectOldSubscriptionById(data.getId());
                
                if (existing != null) {
                    OldSubscription updateData = new OldSubscription();
                    updateData.setId(data.getId());
                    if (data.getAppId() != null) updateData.setAppId(data.getAppId());
                    if (data.getPermissionId() != null) updateData.setPermissionId(data.getPermissionId());
                    if (data.getTenantId() != null) updateData.setTenantId(data.getTenantId());
                    if (data.getPermissionType() != null) updateData.setPermissionType(data.getPermissionType());
                    if (data.getStatus() != null) updateData.setStatus(data.getStatus());
                    updateData.setLastUpdateBy("emergency-update");
                    updateData.setLastUpdateTime(new Date());
                    
                    syncMapper.updateOldSubscriptionById(updateData);
                    updated++;
                    detail.setStatus("updated");
                    detail.setMessage("更新成功");
                    success++;
                    
                } else {
                    // 数据保护校验
                    if (!checkDataProtectionForOld(data, detail, details)) {
                        failed++;
                        continue;
                    }
                    
                    OldSubscription newData = new OldSubscription();
                    newData.setId(data.getId());
                    newData.setAppId(data.getAppId());
                    newData.setPermissionId(data.getPermissionId());
                    newData.setTenantId(data.getTenantId());
                    newData.setPermissionType(data.getPermissionType());
                    newData.setStatus(data.getStatus());
                    newData.setCreateBy("emergency-update");
                    newData.setCreateTime(new Date());
                    newData.setLastUpdateBy("emergency-update");
                    newData.setLastUpdateTime(new Date());
                    
                    syncMapper.insertOldSubscription(newData);
                    inserted++;
                    detail.setStatus("inserted");
                    detail.setMessage("新增成功");
                    success++;
                }
                
                details.add(detail);
                
            } catch (Exception e) {
                log.error("Emergency update old failed, id={}", data.getId(), e);
                detail.setStatus("failed");
                detail.setError("系统异常: " + e.getMessage());
                failed++;
                details.add(detail);
            }
        }

        return EmergencyResult.builder()
                .success(success)
                .failed(failed)
                .inserted(inserted)
                .updated(updated)
                .details(details)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public EmergencyResult emergencyUpdateNew(EmergencyRequest request) {
        log.info("Starting emergency update new table");

        List<EmergencyDetail> details = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int inserted = 0;
        int updated = 0;

        List<SubscriptionData> subscriptions = request.getSubscriptions();
        if (subscriptions == null || subscriptions.isEmpty()) {
            return EmergencyResult.builder()
                    .success(0)
                    .failed(0)
                    .inserted(0)
                    .updated(0)
                    .details(details)
                    .build();
        }

        for (SubscriptionData data : subscriptions) {
            EmergencyDetail detail = EmergencyDetail.builder()
                    .id(data.getId())
                    .build();

            try {
                Subscription existing = syncMapper.selectNewSubscriptionById(data.getId());
                
                if (existing != null) {
                    Subscription updateData = new Subscription();
                    updateData.setId(data.getId());
                    if (data.getAppId() != null) updateData.setAppId(data.getAppId());
                    if (data.getPermissionId() != null) updateData.setPermissionId(data.getPermissionId());
                    if (data.getStatus() != null) updateData.setStatus(data.getStatus());
                    if (data.getChannelType() != null) updateData.setChannelType(data.getChannelType());
                    if (data.getChannelAddress() != null) updateData.setChannelAddress(data.getChannelAddress());
                    if (data.getAuthType() != null) updateData.setAuthType(data.getAuthType());
                    updateData.setLastUpdateBy("emergency-update");
                    updateData.setLastUpdateTime(new Date());
                    
                    syncMapper.updateNewSubscriptionById(updateData);
                    updated++;
                    detail.setStatus("updated");
                    detail.setMessage("更新成功");
                    success++;
                    
                } else {
                    // 数据保护校验
                    if (!checkDataProtectionForNew(data, detail, details)) {
                        failed++;
                        continue;
                    }
                    
                    Subscription newData = new Subscription();
                    newData.setId(data.getId());
                    newData.setAppId(data.getAppId());
                    newData.setPermissionId(data.getPermissionId());
                    newData.setStatus(data.getStatus());
                    newData.setChannelType(data.getChannelType());
                    newData.setChannelAddress(data.getChannelAddress());
                    newData.setAuthType(data.getAuthType());
                    newData.setCreateBy("emergency-update");
                    newData.setCreateTime(new Date());
                    newData.setLastUpdateBy("emergency-update");
                    newData.setLastUpdateTime(new Date());
                    
                    syncMapper.insertNewSubscription(newData);
                    inserted++;
                    detail.setStatus("inserted");
                    detail.setMessage("新增成功");
                    success++;
                }
                
                details.add(detail);
                
            } catch (Exception e) {
                log.error("Emergency update new failed, id={}", data.getId(), e);
                detail.setStatus("failed");
                detail.setError("系统异常: " + e.getMessage());
                failed++;
                details.add(detail);
            }
        }

        return EmergencyResult.builder()
                .success(success)
                .failed(failed)
                .inserted(inserted)
                .updated(updated)
                .details(details)
                .build();
    }

    /**
     * 数据保护校验：检查旧表是否存在重复订阅关系
     * 
     * @param data 订阅关系数据
     * @param detail 详细信息
     * @param details 详细信息列表
     * @return true=通过校验可新增, false=存在重复不允许新增
     */
    private boolean checkDataProtectionForOld(SubscriptionData data, EmergencyDetail detail, 
                                               List<EmergencyDetail> details) {
        if (data.getAppId() == null || data.getPermissionId() == null) {
            return true;
        }
        
        int count = syncMapper.countOldSubscriptionByAppIdAndPermissionId(
            data.getAppId(), data.getPermissionId()
        );
        
        if (count > 0) {
            detail.setStatus("failed");
            detail.setError(String.format(Locale.ROOT, 
                "数据保护：应用ID=%d 和权限ID=%d 的订阅关系已存在，不允许重复创建",
                data.getAppId(), data.getPermissionId()));
            details.add(detail);
            return false;
        }
        
        return true;
    }

    /**
     * 数据保护校验：检查新表是否存在重复订阅关系
     * 
     * @param data 订阅关系数据
     * @param detail 详细信息
     * @param details 详细信息列表
     * @return true=通过校验可新增, false=存在重复不允许新增
     */
    private boolean checkDataProtectionForNew(SubscriptionData data, EmergencyDetail detail,
                                               List<EmergencyDetail> details) {
        if (data.getAppId() == null || data.getPermissionId() == null) {
            return true;
        }
        
        int count = syncMapper.countNewSubscriptionByAppIdAndPermissionId(
            data.getAppId(), data.getPermissionId()
        );
        
        if (count > 0) {
            detail.setStatus("failed");
            detail.setError(String.format(Locale.ROOT, 
                "数据保护：应用ID=%d 和权限ID=%d 的订阅关系已存在，不允许重复创建",
                data.getAppId(), data.getPermissionId()));
            details.add(detail);
            return false;
        }
        
        return true;
    }
}
