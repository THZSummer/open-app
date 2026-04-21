package com.xxx.api.scope.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.api.common.exception.BusinessException;
import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.scope.dto.*;
import com.xxx.api.scope.entity.UserAuthorization;
import com.xxx.api.scope.mapper.UserAuthorizationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Scope 授权服务
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScopeService {

    private final UserAuthorizationMapper userAuthorizationMapper;
    private final ObjectMapper objectMapper;

    // 简单的雪花ID生成（实际项目中应使用专门的ID生成器）
    private long idCounter = System.currentTimeMillis();

    private synchronized Long nextId() {
        return ++idCounter;
    }

    /**
     * 获取用户授权列表
     */
    public ApiResponse<List<UserAuthorizationListResponse>> getUserAuthorizations(
            UserAuthorizationListRequest request) {
        
        // 计算分页偏移量
        int offset = (request.getCurPage() - 1) * request.getPageSize();
        
        // 转换 appId
        Long appIdLong = null;
        if (request.getAppId() != null && !request.getAppId().isEmpty()) {
            try {
                appIdLong = Long.parseLong(request.getAppId());
            } catch (NumberFormatException e) {
                throw BusinessException.badRequest("应用ID格式错误", "Invalid app ID format");
            }
        }
        
        // 查询列表
        List<UserAuthorization> list = userAuthorizationMapper.selectList(
                request.getUserId(),
                appIdLong,
                request.getKeyword(),
                offset,
                request.getPageSize());
        
        // 查询总数
        Long total = userAuthorizationMapper.countList(
                request.getUserId(),
                appIdLong,
                request.getKeyword());
        
        // 转换响应
        List<UserAuthorizationListResponse> responseList = list.stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
        
        // 构建分页响应
        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(request.getCurPage())
                .pageSize(request.getPageSize())
                .total(total)
                .totalPages((int) Math.ceil((double) total / request.getPageSize()))
                .build();
        
        return ApiResponse.success(responseList, page);
    }

    /**
     * 创建用户授权
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<UserAuthorizationResponse> createUserAuthorization(
            UserAuthorizationCreateRequest request) {
        
        // 转换 appId
        Long appIdLong;
        try {
            appIdLong = Long.parseLong(request.getAppId());
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest("应用ID格式错误", "Invalid app ID format");
        }
        
        // 检查是否已存在授权
        UserAuthorization existing = userAuthorizationMapper.selectByUserIdAndAppId(
                request.getUserId(),
                appIdLong);
        
        if (existing != null) {
            throw BusinessException.badRequest(
                    "用户已授权该应用",
                    "User has already authorized this application");
        }
        
        // 创建授权记录
        UserAuthorization authorization = new UserAuthorization();
        authorization.setId(nextId());
        authorization.setUserId(request.getUserId());
        authorization.setAppId(appIdLong);
        
        try {
            authorization.setScopes(objectMapper.writeValueAsString(request.getScopes()));
        } catch (JsonProcessingException e) {
            throw BusinessException.internalError(
                    "Scope 序列化失败",
                    "Failed to serialize scopes");
        }
        
        authorization.setExpiresAt(request.getExpiresAt());
        authorization.setCreateBy(request.getUserId());
        authorization.setLastUpdateBy(request.getUserId());
        
        userAuthorizationMapper.insert(authorization);
        
        // 构建响应
        UserAuthorizationResponse response = new UserAuthorizationResponse();
        response.setId(String.valueOf(authorization.getId()));
        response.setUserId(authorization.getUserId());
        response.setAppId(String.valueOf(authorization.getAppId()));
        response.setScopes(request.getScopes());
        response.setExpiresAt(authorization.getExpiresAt());
        
        return ApiResponse.success(response);
    }

    /**
     * 取消授权
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> revokeUserAuthorization(String id) {
        Long idLong;
        try {
            idLong = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest("授权ID格式错误", "Invalid authorization ID format");
        }
        
        UserAuthorization existing = userAuthorizationMapper.selectById(idLong);
        if (existing == null) {
            throw BusinessException.notFound("授权记录不存在", "Authorization not found");
        }
        
        if (existing.getRevokedAt() != null) {
            throw BusinessException.badRequest("授权已被取消", "Authorization has already been revoked");
        }
        
        int updated = userAuthorizationMapper.revokeById(idLong);
        if (updated == 0) {
            throw BusinessException.internalError(
                    "取消授权失败",
                    "Failed to revoke authorization");
        }
        
        return ApiResponse.success();
    }

    /**
     * 转换为列表响应
     */
    private UserAuthorizationListResponse toListResponse(UserAuthorization entity) {
        UserAuthorizationListResponse response = new UserAuthorizationListResponse();
        response.setId(String.valueOf(entity.getId()));
        response.setUserId(entity.getUserId());
        response.setUserName(entity.getUserId()); // 实际应查询用户信息
        response.setAppId(String.valueOf(entity.getAppId()));
        response.setAppName("应用" + entity.getAppId()); // 实际应查询应用信息
        response.setScopes(entity.getScopeList());
        response.setExpiresAt(entity.getExpiresAt());
        response.setCreateTime(entity.getCreateTime());
        return response;
    }
}
