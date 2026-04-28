package com.xxx.it.works.wecode.v2.modules.sync.mapper;

import com.xxx.it.works.wecode.v2.modules.sync.entity.*;
import com.xxx.it.works.wecode.v2.modules.permission.entity.Subscription;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalLog;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import com.xxx.it.works.wecode.v2.modules.event.entity.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据同步 Mapper 接口
 * 
 * <p>包含旧表查询和新表写入的所有方法</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface SyncMapper {

    // ==================== 旧表查询 ====================

    /**
     * 查询旧订阅关系列表
     * @param ids 指定ID列表，null或空表示全量
     */
    List<OldSubscription> selectOldSubscriptions(@Param("ids") List<Long> ids);

    /**
     * 根据ID查询旧订阅关系
     */
    OldSubscription selectOldSubscriptionById(@Param("id") Long id);

    /**
     * 根据ID查询旧权限
     */
    OldPermission selectOldPermissionById(@Param("id") Long id);

    /**
     * 根据ID查询旧API
     */
    OldApi selectOldApiById(@Param("id") Long id);

    /**
     * 根据ID查询旧事件
     */
    OldEvent selectOldEventById(@Param("id") Long id);

    /**
     * 根据path和method查询旧API
     */
    OldApi selectOldApiByPathAndMethod(@Param("path") String path, @Param("method") String method);

    /**
     * 根据topic查询旧事件
     */
    OldEvent selectOldEventByTopic(@Param("topic") String topic);

    /**
     * 查询旧审批记录（根据resourceId）
     */
    List<OldEflow> selectOldEflowByResourceId(@Param("resourceType") String resourceType, 
                                               @Param("resourceId") Long resourceId);

    /**
     * 查询旧审批日志（根据eflowId）
     */
    List<OldEflowLog> selectOldEflowLogByEflowId(@Param("eflowId") Long eflowId);

    /**
     * 查询应用属性（通道配置）
     * 通过 parent_id 关联应用，查询 event_msg_recive_mode, event_push_url, event_push_auth_type
     */
    OldAppProperty selectAppChannelConfig(@Param("appId") Long appId);

    // ==================== 新表查询 ====================

    /**
     * 查询新订阅关系列表
     * @param ids 指定ID列表，null或空表示全量
     */
    List<Subscription> selectNewSubscriptions(@Param("ids") List<Long> ids);

    /**
     * 根据ID查询新订阅关系
     */
    Subscription selectNewSubscriptionById(@Param("id") Long id);

    /**
     * 根据ID查询新权限
     */
    Permission selectNewPermissionById(@Param("id") Long id);

    /**
     * 根据resourceType和resourceId查询新权限
     */
    Permission selectNewPermissionByResource(@Param("resourceType") String resourceType, 
                                              @Param("resourceId") Long resourceId);

    /**
     * 根据path和method查询新API
     */
    Api selectNewApiByPathAndMethod(@Param("path") String path, @Param("method") String method);

    /**
     * 根据topic查询新事件
     */
    Event selectNewEventByTopic(@Param("topic") String topic);

    /**
     * 根据ID查询新审批记录
     */
    ApprovalRecord selectNewApprovalRecordById(@Param("id") Long id);

    /**
     * 根据ID查询新审批日志
     */
    ApprovalLog selectNewApprovalLogById(@Param("id") Long id);

    // ==================== 新表写入（迁移） ====================

    /**
     * 插入新订阅关系（如果不存在）
     * @return 1=插入成功, 0=已存在
     */
    int insertNewSubscriptionIfNotExists(Subscription subscription);

    /**
     * 插入新审批记录（如果不存在）
     * @return 1=插入成功, 0=已存在
     */
    int insertNewApprovalRecordIfNotExists(ApprovalRecord record);

    /**
     * 插入新审批日志（如果不存在）
     * @return 1=插入成功, 0=已存在
     */
    int insertNewApprovalLogIfNotExists(ApprovalLog log);

    /**
     * 批量插入新审批日志
     */
    int batchInsertNewApprovalLogs(@Param("list") List<ApprovalLog> logs);

    // ==================== 旧表写入（回退） ====================

    /**
     * 插入旧订阅关系（如果不存在）
     * @return 1=插入成功, 0=已存在
     */
    int insertOldSubscriptionIfNotExists(OldSubscription subscription);

    /**
     * 插入旧审批记录（如果不存在）
     * @return 1=插入成功, 0=已存在
     */
    int insertOldEflowIfNotExists(OldEflow eflow);

    /**
     * 插入旧审批日志（如果不存在）
     * @return 1=插入成功, 0=已存在
     */
    int insertOldEflowLogIfNotExists(OldEflowLog log);

    /**
     * 批量插入旧审批日志
     */
    int batchInsertOldEflowLogs(@Param("list") List<OldEflowLog> logs);
}