package com.xxx.api.common.mapper;

import com.xxx.api.common.entity.Subscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订阅关系 Mapper 接口（api-server）
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface SubscriptionMapper {

    /**
     * 根据应用ID和权限ID查询订阅
     */
    Subscription selectByAppIdAndPermissionId(
            @Param("appId") Long appId,
            @Param("permissionId") Long permissionId);

    /**
     * 查询应用的所有已授权订阅
     */
    List<Subscription> selectAuthorizedByAppId(@Param("appId") Long appId);

    /**
     * 根据权限ID查询所有已授权订阅
     */
    List<Subscription> selectAuthorizedByPermissionId(@Param("permissionId") Long permissionId);
}
