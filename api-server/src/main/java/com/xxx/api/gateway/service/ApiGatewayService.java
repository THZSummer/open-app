package com.xxx.api.gateway.service;

import com.xxx.api.common.entity.Permission;
import com.xxx.api.common.entity.Subscription;
import com.xxx.api.common.exception.BusinessException;
import com.xxx.api.common.mapper.PermissionMapper;
import com.xxx.api.common.mapper.SubscriptionMapper;
import com.xxx.api.common.service.ApplicationService;
import com.xxx.api.gateway.dto.CallbackConfigResponse;
import com.xxx.api.gateway.dto.PermissionCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * API 网关服务
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiGatewayService {

    private final PermissionMapper permissionMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ApplicationService applicationService;

    /**
     * 校验应用权限
     * 
     * @param appId 应用ID
     * @param scope 权限标识
     * @return 校验结果
     */
    public PermissionCheckResponse checkPermission(String appId, String scope) {
        // 转换 appId
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

        // 检查订阅状态（1=已授权）
        if (subscription.getStatus() != 1) {
            String statusReason = switch (subscription.getStatus()) {
                case 0 -> "订阅待审批";
                case 2 -> "订阅已拒绝";
                case 3 -> "订阅已取消";
                default -> "订阅状态异常";
            };
            return PermissionCheckResponse.builder()
                    .authorized(false)
                    .reason(statusReason)
                    .subscriptionId(String.valueOf(subscription.getId()))
                    .subscriptionStatus(subscription.getStatus())
                    .build();
        }

        // 返回成功结果
        return PermissionCheckResponse.builder()
                .authorized(true)
                .subscriptionId(String.valueOf(subscription.getId()))
                .subscriptionStatus(subscription.getStatus())
                .build();
    }

    /**
     * 验证应用身份（AKSK/Bearer Token）
     * 
     * @param appId 应用ID
     * @param authType 认证类型
     * @param authCredential 认证凭证
     * @return 是否验证通过
     */
    public boolean verifyApplication(String appId, Integer authType, String authCredential) {
        return applicationService.verifyApplication(appId, authType, authCredential);
    }

    /**
     * 根据路径和方法查找对应的 Scope
     * 
     * @param path API路径
     * @param method HTTP方法
     * @return Scope 标识
     */
    public String findScopeByPathAndMethod(String path, String method) {
        // Mock 实现：根据路径和方法生成 Scope
        // 实际项目中应该查询 API 资源表，找到对应的权限
        
        // 示例：/api/v1/messages + POST -> api:im:send-message
        if (path == null || method == null) {
            return null;
        }
        
        // 简单的路径转换规则
        String scope = "api:" + path.replaceAll("[^a-zA-Z0-9]", ":") + ":" + method.toLowerCase();
        log.debug("Generated Scope: path={}, method={}, scope={}", path, method, scope);
        
        return scope;
    }

    /**
     * 获取回调配置
     * 
     * <p>通过 AK + Scope 查询应用对某个回调的订阅配置</p>
     * 
     * @param ak 应用 Access Key
     * @param scope 回调权限标识
     * @return 回调配置，未找到返回 null
     */
    public CallbackConfigResponse getCallbackConfig(String ak, String scope) {
        // 1. 通过 AK 获取应用ID（预留接口，对接现有 AKSK 管理系统）
        Long appId = applicationService.getAppIdByAk(ak);
        if (appId == null) {
            log.warn("Invalid Access Key: {}", ak);
            return null;
        }

        // 2. 查询回调订阅配置
        Subscription subscription = subscriptionMapper.selectCallbackConfigByAppIdAndScope(appId, scope);
        if (subscription == null) {
            log.info("Callback subscription configuration not found: appId={}, scope={}", appId, scope);
            return null;
        }

        // 3. 构建响应
        return CallbackConfigResponse.builder()
                .ak(ak)
                .scope(scope)
                .channelType(subscription.getChannelType())
                .channelAddress(subscription.getChannelAddress())
                .authType(subscription.getAuthType())
                .build();
    }
}