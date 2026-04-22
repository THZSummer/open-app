package com.xxx.open.modules.permission.mapper;

import com.xxx.open.modules.permission.entity.Subscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 订阅关系 Mapper 接口
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface SubscriptionMapper {

    /**
     * 插入订阅
     */
    int insert(Subscription subscription);

    /**
     * 批量插入订阅
     */
    int batchInsert(@Param("list") List<Subscription> list);

    /**
     * 根据ID查询订阅
     */
    Subscription selectById(@Param("id") Long id);

    /**
     * 根据应用ID和权限ID查询订阅
     */
    Subscription selectByAppIdAndPermissionId(
            @Param("appId") Long appId,
            @Param("permissionId") Long permissionId);

    /**
     * 查询应用的订阅列表（API 权限）
     */
    List<Subscription> selectApiSubscriptionsByAppId(
            @Param("appId") Long appId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计应用的订阅数量（API 权限）
     */
    Long countApiSubscriptionsByAppId(
            @Param("appId") Long appId,
            @Param("status") Integer status,
            @Param("keyword") String keyword);

    /**
     * 查询应用的订阅列表（事件权限）
     */
    List<Subscription> selectEventSubscriptionsByAppId(
            @Param("appId") Long appId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计应用的订阅数量（事件权限）
     */
    Long countEventSubscriptionsByAppId(
            @Param("appId") Long appId,
            @Param("status") Integer status,
            @Param("keyword") String keyword);

    /**
     * 查询应用的订阅列表（回调权限）
     */
    List<Subscription> selectCallbackSubscriptionsByAppId(
            @Param("appId") Long appId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计应用的订阅数量（回调权限）
     */
    Long countCallbackSubscriptionsByAppId(
            @Param("appId") Long appId,
            @Param("status") Integer status,
            @Param("keyword") String keyword);

    /**
     * 更新订阅
     */
    int update(Subscription subscription);

    /**
     * 更新订阅配置（通道/地址/认证）
     */
    int updateConfig(
            @Param("id") Long id,
            @Param("channelType") Integer channelType,
            @Param("channelAddress") String channelAddress,
            @Param("authType") Integer authType,
            @Param("lastUpdateTime") Date lastUpdateTime,
            @Param("lastUpdateBy") String lastUpdateBy);

    /**
     * 更新订阅状态
     */
    int updateStatus(
            @Param("id") Long id,
            @Param("status") Integer status,
            @Param("lastUpdateTime") Date lastUpdateTime,
            @Param("lastUpdateBy") String lastUpdateBy);

    /**
     * 删除订阅
     */
    int deleteById(@Param("id") Long id);

    /**
     * 统计权限被订阅的数量
     */
    Long countByPermissionId(@Param("permissionId") Long permissionId);
}
