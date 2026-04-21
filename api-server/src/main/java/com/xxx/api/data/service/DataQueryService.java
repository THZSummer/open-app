package com.xxx.api.data.service;

import com.xxx.api.common.entity.Permission;
import com.xxx.api.common.entity.Subscription;
import com.xxx.api.common.mapper.PermissionMapper;
import com.xxx.api.common.mapper.SubscriptionMapper;
import com.xxx.api.gateway.dto.PermissionCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据查询服务
 * 
 * <p>提供数据查询接口，供 event-server 或其他服务调用</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQueryService {

    private final PermissionMapper permissionMapper;
    private final SubscriptionMapper subscriptionMapper;

    /**
     * 权限校验接口
     * 
     * <p>接口编号：#58</p>
     * <p>供 event-server 或其他服务调用</p>
     * 
     * @param appId 应用ID
     * @param scope 权限标识
     * @return 校验结果
     */
    public PermissionCheckResponse checkPermission(String appId, String scope) {
        Long appIdLong;
        try {
            appIdLong = Long.parseLong(appId);
        } catch (NumberFormatException e) {
            return PermissionCheckResponse.builder()
                    .authorized(false)
                    .reason("应用ID格式错误")
                    .build();
        }

        // 查询权限
        Permission permission = permissionMapper.selectByScope(scope);
        if (permission == null) {
            return PermissionCheckResponse.builder()
                    .authorized(false)
                    .reason("权限不存在")
                    .build();
        }

        // 查询订阅关系
        Subscription subscription = subscriptionMapper.selectByAppIdAndPermissionId(
                appIdLong, permission.getId());

        if (subscription == null) {
            return PermissionCheckResponse.builder()
                    .authorized(false)
                    .reason("应用未订阅该权限")
                    .build();
        }

        // 检查订阅状态
        if (subscription.getStatus() != 1) {
            return PermissionCheckResponse.builder()
                    .authorized(false)
                    .reason("订阅状态异常")
                    .subscriptionId(String.valueOf(subscription.getId()))
                    .subscriptionStatus(subscription.getStatus())
                    .build();
        }

        return PermissionCheckResponse.builder()
                .authorized(true)
                .subscriptionId(String.valueOf(subscription.getId()))
                .subscriptionStatus(subscription.getStatus())
                .build();
    }

    /**
     * 查询订阅某权限的所有应用
     * 
     * @param scope 权限标识
     * @return 应用ID列表
     */
    public List<String> getSubscribedApps(String scope) {
        Permission permission = permissionMapper.selectByScope(scope);
        if (permission == null) {
            return new ArrayList<>();
        }

        List<Subscription> subscriptions = subscriptionMapper.selectAuthorizedByPermissionId(
                permission.getId());

        return subscriptions.stream()
                .map(sub -> String.valueOf(sub.getAppId()))
                .toList();
    }

    /**
     * 查询应用的订阅配置
     * 
     * @param appId 应用ID
     * @param scope 权限标识
     * @return 订阅配置
     */
    public Map<String, Object> getSubscriptionConfig(String appId, String scope) {
        Long appIdLong;
        try {
            appIdLong = Long.parseLong(appId);
        } catch (NumberFormatException e) {
            return new HashMap<>();
        }

        Permission permission = permissionMapper.selectByScope(scope);
        if (permission == null) {
            return new HashMap<>();
        }

        Subscription subscription = subscriptionMapper.selectByAppIdAndPermissionId(
                appIdLong, permission.getId());

        if (subscription == null) {
            return new HashMap<>();
        }

        Map<String, Object> config = new HashMap<>();
        config.put("id", String.valueOf(subscription.getId()));
        config.put("appId", String.valueOf(subscription.getAppId()));
        config.put("permissionId", String.valueOf(subscription.getPermissionId()));
        config.put("status", subscription.getStatus());
        config.put("channelType", subscription.getChannelType());
        config.put("channelAddress", subscription.getChannelAddress());
        config.put("authType", subscription.getAuthType());

        return config;
    }

    /**
     * 查询权限详情
     * 
     * @param scope 权限标识
     * @return 权限详情
     */
    public Map<String, Object> getPermissionByScope(String scope) {
        Permission permission = permissionMapper.selectByScope(scope);
        if (permission == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", String.valueOf(permission.getId()));
        result.put("nameCn", permission.getNameCn());
        result.put("nameEn", permission.getNameEn());
        result.put("scope", permission.getScope());
        result.put("resourceType", permission.getResourceType());
        result.put("resourceId", String.valueOf(permission.getResourceId()));
        result.put("status", permission.getStatus());

        return result;
    }
}
