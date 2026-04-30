package com.xxx.it.works.wecode.v2.modules.permission.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import com.xxx.it.works.wecode.v2.modules.api.entity.ApiProperty;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.callback.entity.Callback;
import com.xxx.it.works.wecode.v2.modules.callback.entity.CallbackProperty;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.category.entity.Category;
import com.xxx.it.works.wecode.v2.modules.category.mapper.CategoryMapper;
import com.xxx.it.works.wecode.v2.modules.event.entity.Event;
import com.xxx.it.works.wecode.v2.modules.event.entity.EventProperty;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.event.entity.PermissionProperty;
import com.xxx.it.works.wecode.v2.modules.event.mapper.EventMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.EventPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.permission.dto.*;
import com.xxx.it.works.wecode.v2.modules.permission.entity.Subscription;
import com.xxx.it.works.wecode.v2.modules.permission.mapper.SubscriptionMapper;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限管理服务
 *
 * <p>实现权限申请与订阅管理功能</p>
 * <p>接口编号：#27 ~ #43</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;
    private final PermissionPropertyMapper permissionPropertyMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final CategoryMapper categoryMapper;
    private final ApiMapper apiMapper;
    private final ApiPropertyMapper apiPropertyMapper;
    private final EventMapper eventMapper;
    private final EventPropertyMapper eventPropertyMapper;
    private final CallbackMapper callbackMapper;
    private final CallbackPropertyMapper callbackPropertyMapper;
    private final IdGeneratorStrategy idGenerator;
    private final ApprovalEngine approvalEngine;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final AppContextResolver appContextResolver;



    /**
     * 审批URL前缀（域名+路径）
     */
    @Value("${platform.approval-url-prefix}")
    private String approvalUrlPrefix;

    /**
     * 构建审批URL
     *
     * @param id 订阅ID
     * @return 完整的审批URL
     */
    private String buildApprovalUrl(Long id) {
        return approvalUrlPrefix + id;
    }

    // ==================== API 权限管理 (#27-31) ====================

    /**
     * #27 获取应用 API 权限列表
     */
    public ApiResponse<List<ApiSubscriptionListResponse>> getApiSubscriptionList(
            String appId, Integer status, String keyword, Integer curPage, Integer pageSize) {

        log.info("Get app API permission list, appId={}, status={}, keyword={}", appId, status, keyword);

        // 解析并校验应用访问权限
        AppContext appContext = appContextResolver.resolveAndValidate(appId);
        Long appIdLong = appContext.getInternalId();

        // 分页参数
        int page = curPage != null ? curPage : 1;
        int size = pageSize != null ? pageSize : 20;
        int offset = (page - 1) * size;

        // 查询列表
        List<Subscription> subscriptions = subscriptionMapper.selectApiSubscriptionsByAppId(
                appIdLong, status, keyword, offset, size);

        // 查询总数
        Long total = subscriptionMapper.countApiSubscriptionsByAppId(
                appIdLong, status, keyword);

        // 转换响应
        List<ApiSubscriptionListResponse> responses = subscriptions.stream()
                .map(sub -> convertToApiSubscriptionResponse(sub))
                .collect(Collectors.toList());

        // 构建分页响应
        ApiResponse.PageResponse pageResponse = ApiResponse.PageResponse.builder()
                .curPage(page)
                .pageSize(size)
                .total(total)
                .totalPages((int) Math.ceil((double) total / size))
                .build();

        return ApiResponse.success(responses, pageResponse);
    }

    /**
     * #28 获取分类下 API 权限列表（权限树懒加载）
     */
    public ApiResponse<List<CategoryPermissionListResponse>> getCategoryApiPermissions(
            String categoryId, String appId, String keyword, Integer needApproval,
            Boolean includeChildren, Integer curPage, Integer pageSize) {

        log.info("Get API permission list under category, categoryId={}, appId={}, includeChildren={}", categoryId, appId, includeChildren);

        Long categoryIdLong = parseId(categoryId);

        // 获取分类信息
        Category category = categoryMapper.selectById(categoryIdLong);
        if (category == null) {
            throw new BusinessException("404", "分类不存在", "Category not found");
        }

        // 处理包含子分类
        boolean include = includeChildren != null ? includeChildren : true;
        String categoryPath = include ? category.getPath() : null;

        // 分页参数
        int page = curPage != null ? curPage : 1;
        int size = pageSize != null ? pageSize : 20;
        int offset = (page - 1) * size;

        // 查询权限列表
        List<Permission> permissions = permissionMapper.selectApiPermissionsByCategory(
                categoryIdLong, keyword, needApproval, categoryPath, include, offset, size);

        // 查询总数
        Long total = permissionMapper.countApiPermissionsByCategory(
                categoryIdLong, keyword, needApproval, categoryPath, include);

        // 解析应用ID（用于查询订阅状态，可选参数）
        Long appIdLong = null;
        if (appId != null && !appId.isEmpty()) {
            try {
                AppContext appCtx = appContextResolver.resolveAndValidate(appId);
                appIdLong = appCtx.getInternalId();
            } catch (Exception e) {

                // 分类查询时，appId 是可选参数，转换失败时忽略
                log.warn("Failed to parse appId, skip subscription status query: {}", appId);
            }
        }

        final Long finalAppIdLong = appIdLong;
        final String finalExternalAppId = appId;

        // 转换响应
        List<CategoryPermissionListResponse> responses = permissions.stream()
                .map(p -> convertToCategoryPermissionResponse(p, finalAppIdLong))
                .collect(Collectors.toList());

        // 构建分页响应
        ApiResponse.PageResponse pageResponse = ApiResponse.PageResponse.builder()
                .curPage(page)
                .pageSize(size)
                .total(total)
                .totalPages((int) Math.ceil((double) total / size))
                .build();

        return ApiResponse.success(responses, pageResponse);
    }

    /**
     * #29 申请 API 权限（支持批量）
     */
    @Transactional(rollbackFor = Exception.class)
    public PermissionSubscribeResponse subscribeApiPermissions(String appId, PermissionSubscribeRequest request) {

        log.info("Apply for API permission, appId={}, permissionIds={}", appId, request.getPermissionIds());

        // 解析并校验应用访问权限
        AppContext appContext = appContextResolver.resolveAndValidate(appId);
        Long appIdLong = appContext.getInternalId();
        String externalAppId = appContext.getExternalId();

        // 解析权限ID列表
        List<Long> permissionIds = request.getPermissionIds().stream()
                .map(this::parseId)
                .collect(Collectors.toList());

        // 查询权限信息
        List<Permission> permissions = permissionMapper.selectByIds(permissionIds);
        if (permissions.isEmpty()) {
            throw new BusinessException("400", "权限不存在", "Permissions not found");
        }

        // 过滤出 API 类型的权限
        List<Permission> apiPermissions = permissions.stream()
                .filter(p -> "api".equals(p.getResourceType()))
                .collect(Collectors.toList());

        if (apiPermissions.isEmpty()) {
            throw new BusinessException("400", "未找到 API 类型的权限", "No API permissions found");
        }

        Date now = new Date();
        String currentUser = getCurrentUser();

        // 批量创建订阅
        List<Subscription> subscriptions = new ArrayList<>();
        List<PermissionSubscribeResponse.SubscriptionRecord> successRecords = new ArrayList<>();
        List<PermissionSubscribeResponse.FailedRecord> failedRecords = new ArrayList<>();

        for (Permission permission : apiPermissions) {

            // 检查是否已订阅
            Subscription existing = subscriptionMapper.selectByAppIdAndPermissionId(appIdLong, permission.getId());
            if (existing != null) {
                failedRecords.add(PermissionSubscribeResponse.FailedRecord.builder()
                        .permissionId(String.valueOf(permission.getId()))
                        .reason("已订阅该权限")
                        .build());
                continue;
            }

            // 创建订阅记录
            Subscription subscription = new Subscription();
            subscription.setId(idGenerator.nextId());
            subscription.setAppId(appIdLong);
            subscription.setPermissionId(permission.getId());
            subscription.setStatus(0); // 待审
            subscription.setCreateTime(now);
            subscription.setLastUpdateTime(now);
            subscription.setCreateBy(currentUser);
            subscription.setLastUpdateBy(currentUser);

            subscriptions.add(subscription);

            successRecords.add(PermissionSubscribeResponse.SubscriptionRecord.builder()
                    .id(String.valueOf(subscription.getId()))
                    .appId(externalAppId)
                    .permissionId(String.valueOf(permission.getId()))
                    .status(0)
                    .build());
        }

        // 批量插入订阅
        if (!subscriptions.isEmpty()) {
            subscriptionMapper.batchInsert(subscriptions);

            // 为每个订阅记录创建审批流程
            for (Subscription subscription : subscriptions) {
                try {
                    ApprovalRecord approvalRecord = approvalEngine.createApproval(
                            ApprovalEngine.BusinessType.API_PERMISSION_APPLY,  // ✅ v2.8.0变更
                            subscription.getPermissionId(),  // ✅ permissionId
                            subscription.getId(),             // businessId
                            currentUser,
                            currentUser,
                            currentUser
                    );
                    log.info("Approval record created successfully: subscriptionId={}, approvalId={}",
                            subscription.getId(), approvalRecord.getId());
                } catch (Exception e) {
                    log.error("Failed to create approval record: subscriptionId={}", subscription.getId(), e);
                }
            }
        }

        return PermissionSubscribeResponse.builder()
                .successCount(successRecords.size())
                .failedCount(failedRecords.size())
                .records(successRecords)
                .failedRecords(failedRecords.isEmpty() ? null : failedRecords)
                .build();
    }

    /**
     * #30 撤回审核中的 API 权限申请
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse withdrawApiSubscription(String appId, String subscriptionId) {

        log.info("Withdraw API permission application, appId={}, subscriptionId={}", appId, subscriptionId);

        // 解析并校验应用访问权限
        appContextResolver.resolveAndValidate(appId);

        return withdrawSubscription(subscriptionId);
    }

    /**
     * #31 删除 API 权限订阅记录
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse deleteApiSubscription(String appId, String subscriptionId) {

        log.info("Delete API permission subscription, appId={}, subscriptionId={}", appId, subscriptionId);

        // 解析并校验应用访问权限
        appContextResolver.resolveAndValidate(appId);

        return deleteSubscription(subscriptionId);
    }

    // ==================== 事件权限管理 (#32-37) ====================

    /**
     * #32 获取应用事件订阅列表
     */
    public ApiResponse<List<EventSubscriptionListResponse>> getEventSubscriptionList(
            String appId, Integer status, String keyword, Integer curPage, Integer pageSize) {

        log.info("Get app event subscription list, appId={}", appId);

        // 解析并校验应用访问权限
        AppContext appContext = appContextResolver.resolveAndValidate(appId);
        Long appIdLong = appContext.getInternalId();

        int page = curPage != null ? curPage : 1;
        int size = pageSize != null ? pageSize : 20;
        int offset = (page - 1) * size;

        List<Subscription> subscriptions = subscriptionMapper.selectEventSubscriptionsByAppId(
                appIdLong, status, keyword, offset, size);

        Long total = subscriptionMapper.countEventSubscriptionsByAppId(
                appIdLong, status, keyword);

        List<EventSubscriptionListResponse> responses = subscriptions.stream()
                .map(this::convertToEventSubscriptionResponse)
                .collect(Collectors.toList());

        ApiResponse.PageResponse pageResponse = ApiResponse.PageResponse.builder()
                .curPage(page)
                .pageSize(size)
                .total(total)
                .totalPages((int) Math.ceil((double) total / size))
                .build();

        return ApiResponse.success(responses, pageResponse);
    }

    /**
     * #33 获取分类下事件权限列表（权限树懒加载）
     */
    public ApiResponse<List<CategoryPermissionListResponse>> getCategoryEventPermissions(
            String categoryId, String appId, String keyword, Integer needApproval,
            Boolean includeChildren, Integer curPage, Integer pageSize) {

        log.info("Get event permission list under category, categoryId={}, appId={}", categoryId, appId);

        Long categoryIdLong = parseId(categoryId);

        Category category = categoryMapper.selectById(categoryIdLong);
        if (category == null) {
            throw new BusinessException("404", "分类不存在", "Category not found");
        }

        boolean include = includeChildren != null ? includeChildren : true;
        String categoryPath = include ? category.getPath() : null;

        int page = curPage != null ? curPage : 1;
        int size = pageSize != null ? pageSize : 20;
        int offset = (page - 1) * size;

        List<Permission> permissions = permissionMapper.selectEventPermissionsByCategory(
                categoryIdLong, keyword, needApproval, categoryPath, include, offset, size);

        Long total = permissionMapper.countEventPermissionsByCategory(
                categoryIdLong, keyword, needApproval, categoryPath, include);

        // 解析应用ID（用于查询订阅状态，可选参数）
        Long appIdLong = null;
        if (appId != null && !appId.isEmpty()) {
            try {
                AppContext appCtx = appContextResolver.resolveAndValidate(appId);
                appIdLong = appCtx.getInternalId();
            } catch (Exception e) {
                log.warn("Failed to parse appId, skip subscription status query: {}", appId);
            }
        }

        final Long finalAppIdLong = appIdLong;

        List<CategoryPermissionListResponse> responses = permissions.stream()
                .map(p -> convertToCategoryPermissionResponse(p, finalAppIdLong))
                .collect(Collectors.toList());

        ApiResponse.PageResponse pageResponse = ApiResponse.PageResponse.builder()
                .curPage(page)
                .pageSize(size)
                .total(total)
                .totalPages((int) Math.ceil((double) total / size))
                .build();

        return ApiResponse.success(responses, pageResponse);
    }

    /**
     * #34 申请事件权限（支持批量）
     */
    @Transactional(rollbackFor = Exception.class)
    public PermissionSubscribeResponse subscribeEventPermissions(String appId, PermissionSubscribeRequest request) {

        log.info("Apply for event permission, appId={}", appId);

        // 解析并校验应用访问权限
        AppContext appContext = appContextResolver.resolveAndValidate(appId);
        Long appIdLong = appContext.getInternalId();
        String externalAppId = appContext.getExternalId();

        List<Long> permissionIds = request.getPermissionIds().stream()
                .map(this::parseId)
                .collect(Collectors.toList());

        List<Permission> permissions = permissionMapper.selectByIds(permissionIds);
        if (permissions.isEmpty()) {
            throw new BusinessException("400", "权限不存在", "Permissions not found");
        }

        List<Permission> eventPermissions = permissions.stream()
                .filter(p -> "event".equals(p.getResourceType()))
                .collect(Collectors.toList());

        if (eventPermissions.isEmpty()) {
            throw new BusinessException("400", "未找到事件类型的权限", "No event permissions found");
        }

        Date now = new Date();
        String currentUser = getCurrentUser();

        List<Subscription> subscriptions = new ArrayList<>();
        List<PermissionSubscribeResponse.SubscriptionRecord> successRecords = new ArrayList<>();
        List<PermissionSubscribeResponse.FailedRecord> failedRecords = new ArrayList<>();

        for (Permission permission : eventPermissions) {
            Subscription existing = subscriptionMapper.selectByAppIdAndPermissionId(appIdLong, permission.getId());
            if (existing != null) {
                failedRecords.add(PermissionSubscribeResponse.FailedRecord.builder()
                        .permissionId(String.valueOf(permission.getId()))
                        .reason("已订阅该权限")
                        .build());
                continue;
            }

            Subscription subscription = new Subscription();
            subscription.setId(idGenerator.nextId());
            subscription.setAppId(appIdLong);
            subscription.setPermissionId(permission.getId());
            subscription.setStatus(0);
            subscription.setCreateTime(now);
            subscription.setLastUpdateTime(now);
            subscription.setCreateBy(currentUser);
            subscription.setLastUpdateBy(currentUser);

            subscriptions.add(subscription);

            successRecords.add(PermissionSubscribeResponse.SubscriptionRecord.builder()
                    .id(String.valueOf(subscription.getId()))
                    .appId(externalAppId)
                    .permissionId(String.valueOf(permission.getId()))
                    .status(0)
                    .build());
        }

        if (!subscriptions.isEmpty()) {
            subscriptionMapper.batchInsert(subscriptions);

            for (Subscription subscription : subscriptions) {
                try {
                    ApprovalRecord approvalRecord = approvalEngine.createApproval(
                            ApprovalEngine.BusinessType.EVENT_PERMISSION_APPLY,  // ✅ v2.8.0变更
                            subscription.getPermissionId(),  // ✅ permissionId
                            subscription.getId(),             // businessId
                            currentUser,
                            currentUser,
                            currentUser
                    );
                    log.info("Approval record created successfully: subscriptionId={}, approvalId={}",
                            subscription.getId(), approvalRecord.getId());
                } catch (Exception e) {
                    log.error("Failed to create approval record: subscriptionId={}", subscription.getId(), e);
                }
            }
        }

        return PermissionSubscribeResponse.builder()
                .successCount(successRecords.size())
                .failedCount(failedRecords.size())
                .records(successRecords)
                .failedRecords(failedRecords.isEmpty() ? null : failedRecords)
                .build();
    }

    /**
     * #35 配置事件消费参数
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse configEventSubscription(String appId, String subscriptionId, SubscriptionConfigRequest request) {

        log.info("Configure event consumption params, appId={}, subscriptionId={}", appId, subscriptionId);

        // 解析并校验应用访问权限
        appContextResolver.resolveAndValidate(appId);

        Long subIdLong = parseId(subscriptionId);

        Subscription subscription = subscriptionMapper.selectById(subIdLong);
        if (subscription == null) {
            throw new BusinessException("404", "订阅记录不存在", "Subscription not found");
        }

        // 更新配置
        Date now = new Date();
        String currentUser = getCurrentUser();

        subscriptionMapper.updateConfig(subIdLong, request.getChannelType(),
                request.getChannelAddress(), request.getAuthType(), now, currentUser);

        return WithdrawResponse.builder()
                .id(subscriptionId)
                .message("事件消费参数配置成功")
                .channelType(request.getChannelType())
                .channelAddress(request.getChannelAddress())
                .authType(request.getAuthType())
                .build();
    }

    /**
     * #36 撤回审核中的事件权限申请
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse withdrawEventSubscription(String appId, String subscriptionId) {

        log.info("Withdraw event permission application, appId={}, subscriptionId={}", appId, subscriptionId);

        // 解析并校验应用访问权限
        appContextResolver.resolveAndValidate(appId);

        return withdrawSubscription(subscriptionId);
    }

    /**
     * #37 删除事件权限订阅记录
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse deleteEventSubscription(String appId, String subscriptionId) {

        log.info("Delete event permission subscription, appId={}, subscriptionId={}", appId, subscriptionId);

        // 解析并校验应用访问权限
        appContextResolver.resolveAndValidate(appId);

        return deleteSubscription(subscriptionId);
    }

    // ==================== 回调权限管理 (#38-43) ====================

    /**
     * #38 获取应用回调订阅列表
     */
    public ApiResponse<List<CallbackSubscriptionListResponse>> getCallbackSubscriptionList(
            String appId, Integer status, String keyword, Integer curPage, Integer pageSize) {

        log.info("Get app callback subscription list, appId={}", appId);

        // 解析并校验应用访问权限
        AppContext appContext = appContextResolver.resolveAndValidate(appId);
        Long appIdLong = appContext.getInternalId();

        int page = curPage != null ? curPage : 1;
        int size = pageSize != null ? pageSize : 20;
        int offset = (page - 1) * size;

        List<Subscription> subscriptions = subscriptionMapper.selectCallbackSubscriptionsByAppId(
                appIdLong, status, keyword, offset, size);

        Long total = subscriptionMapper.countCallbackSubscriptionsByAppId(
                appIdLong, status, keyword);

        List<CallbackSubscriptionListResponse> responses = subscriptions.stream()
                .map(this::convertToCallbackSubscriptionResponse)
                .collect(Collectors.toList());

        ApiResponse.PageResponse pageResponse = ApiResponse.PageResponse.builder()
                .curPage(page)
                .pageSize(size)
                .total(total)
                .totalPages((int) Math.ceil((double) total / size))
                .build();

        return ApiResponse.success(responses, pageResponse);
    }

    /**
     * #39 获取分类下回调权限列表（权限树懒加载）
     */
    public ApiResponse<List<CategoryPermissionListResponse>> getCategoryCallbackPermissions(
            String categoryId, String appId, String keyword, Integer needApproval,
            Boolean includeChildren, Integer curPage, Integer pageSize) {

        log.info("Get callback permission list under category, categoryId={}, appId={}", categoryId, appId);

        Long categoryIdLong = parseId(categoryId);

        Category category = categoryMapper.selectById(categoryIdLong);
        if (category == null) {
            throw new BusinessException("404", "分类不存在", "Category not found");
        }

        boolean include = includeChildren != null ? includeChildren : true;
        String categoryPath = include ? category.getPath() : null;

        int page = curPage != null ? curPage : 1;
        int size = pageSize != null ? pageSize : 20;
        int offset = (page - 1) * size;

        List<Permission> permissions = permissionMapper.selectCallbackPermissionsByCategory(
                categoryIdLong, keyword, needApproval, categoryPath, include, offset, size);

        Long total = permissionMapper.countCallbackPermissionsByCategory(
                categoryIdLong, keyword, needApproval, categoryPath, include);

        // 解析应用ID（用于查询订阅状态，可选参数）
        Long appIdLong = null;
        if (appId != null && !appId.isEmpty()) {
            try {
                AppContext appCtx = appContextResolver.resolveAndValidate(appId);
                appIdLong = appCtx.getInternalId();
            } catch (Exception e) {
                log.warn("Failed to parse appId, skip subscription status query: {}", appId);
            }
        }

        final Long finalAppIdLong = appIdLong;

        List<CategoryPermissionListResponse> responses = permissions.stream()
                .map(p -> convertToCategoryPermissionResponse(p, finalAppIdLong))
                .collect(Collectors.toList());

        ApiResponse.PageResponse pageResponse = ApiResponse.PageResponse.builder()
                .curPage(page)
                .pageSize(size)
                .total(total)
                .totalPages((int) Math.ceil((double) total / size))
                .build();

        return ApiResponse.success(responses, pageResponse);
    }

    /**
     * #40 申请回调权限（支持批量）
     */
    @Transactional(rollbackFor = Exception.class)
    public PermissionSubscribeResponse subscribeCallbackPermissions(String appId, PermissionSubscribeRequest request) {

        log.info("Apply for callback permission, appId={}", appId);

        // 解析并校验应用访问权限
        AppContext appContext = appContextResolver.resolveAndValidate(appId);
        Long appIdLong = appContext.getInternalId();
        String externalAppId = appContext.getExternalId();

        List<Long> permissionIds = request.getPermissionIds().stream()
                .map(this::parseId)
                .collect(Collectors.toList());

        List<Permission> permissions = permissionMapper.selectByIds(permissionIds);
        if (permissions.isEmpty()) {
            throw new BusinessException("400", "权限不存在", "Permissions not found");
        }

        List<Permission> callbackPermissions = permissions.stream()
                .filter(p -> "callback".equals(p.getResourceType()))
                .collect(Collectors.toList());

        if (callbackPermissions.isEmpty()) {
            throw new BusinessException("400", "未找到回调类型的权限", "No callback permissions found");
        }

        Date now = new Date();
        String currentUser = getCurrentUser();

        List<Subscription> subscriptions = new ArrayList<>();
        List<PermissionSubscribeResponse.SubscriptionRecord> successRecords = new ArrayList<>();
        List<PermissionSubscribeResponse.FailedRecord> failedRecords = new ArrayList<>();

        for (Permission permission : callbackPermissions) {
            Subscription existing = subscriptionMapper.selectByAppIdAndPermissionId(appIdLong, permission.getId());
            if (existing != null) {
                failedRecords.add(PermissionSubscribeResponse.FailedRecord.builder()
                        .permissionId(String.valueOf(permission.getId()))
                        .reason("已订阅该权限")
                        .build());
                continue;
            }

            Subscription subscription = new Subscription();
            subscription.setId(idGenerator.nextId());
            subscription.setAppId(appIdLong);
            subscription.setPermissionId(permission.getId());
            subscription.setStatus(0);
            subscription.setCreateTime(now);
            subscription.setLastUpdateTime(now);
            subscription.setCreateBy(currentUser);
            subscription.setLastUpdateBy(currentUser);

            subscriptions.add(subscription);

            successRecords.add(PermissionSubscribeResponse.SubscriptionRecord.builder()
                    .id(String.valueOf(subscription.getId()))
                    .appId(externalAppId)
                    .permissionId(String.valueOf(permission.getId()))
                    .status(0)
                    .build());
        }

        if (!subscriptions.isEmpty()) {
            subscriptionMapper.batchInsert(subscriptions);

            // 为每个订阅记录创建审批流程
            for (Subscription subscription : subscriptions) {
                try {
                    ApprovalRecord approvalRecord = approvalEngine.createApproval(
                            ApprovalEngine.BusinessType.CALLBACK_PERMISSION_APPLY,  // ✅ v2.8.0修正：使用 CALLBACK_PERMISSION_APPLY
                            subscription.getPermissionId(),  // ✅ permissionId
                            subscription.getId(),             // businessId
                            currentUser,
                            currentUser,
                            currentUser
                    );
                    log.info("Approval record created successfully: subscriptionId={}, approvalId={}",
                            subscription.getId(), approvalRecord.getId());
                } catch (Exception e) {
                    log.error("Failed to create approval record: subscriptionId={}", subscription.getId(), e);
                }
            }
        }

        return PermissionSubscribeResponse.builder()
                .successCount(successRecords.size())
                .failedCount(failedRecords.size())
                .records(successRecords)
                .failedRecords(failedRecords.isEmpty() ? null : failedRecords)
                .build();
    }

    /**
     * #41 配置回调消费参数
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse configCallbackSubscription(String appId, String subscriptionId, SubscriptionConfigRequest request) {

        log.info("Configure callback consumption params, appId={}, subscriptionId={}", appId, subscriptionId);

        // 解析并校验应用访问权限
        appContextResolver.resolveAndValidate(appId);

        Long subIdLong = parseId(subscriptionId);

        Subscription subscription = subscriptionMapper.selectById(subIdLong);
        if (subscription == null) {
            throw new BusinessException("404", "订阅记录不存在", "Subscription not found");
        }

        Date now = new Date();
        String currentUser = getCurrentUser();

        subscriptionMapper.updateConfig(subIdLong, request.getChannelType(),
                request.getChannelAddress(), request.getAuthType(), now, currentUser);

        return WithdrawResponse.builder()
                .id(subscriptionId)
                .message("回调消费参数配置成功")
                .build();
    }

    /**
     * #42 撤回审核中的回调权限申请
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse withdrawCallbackSubscription(String appId, String subscriptionId) {

        log.info("Withdraw callback permission application, appId={}, subscriptionId={}", appId, subscriptionId);

        // 解析并校验应用访问权限
        appContextResolver.resolveAndValidate(appId);

        return withdrawSubscription(subscriptionId);
    }

    /**
     * #43 删除回调权限订阅记录
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse deleteCallbackSubscription(String appId, String subscriptionId) {

        log.info("Delete callback permission subscription, appId={}, subscriptionId={}", appId, subscriptionId);

        // 解析并校验应用访问权限
        appContextResolver.resolveAndValidate(appId);

        return deleteSubscription(subscriptionId);
    }

    // ==================== 私有方法 ====================

    /**
     * 撤回订阅（通用方法）
     */
    private WithdrawResponse withdrawSubscription(String subscriptionId) {
        Long subIdLong = parseId(subscriptionId);

        Subscription subscription = subscriptionMapper.selectById(subIdLong);
        if (subscription == null) {
            throw new BusinessException("404", "订阅记录不存在", "Subscription not found");
        }

        // 检查状态
        if (subscription.getStatus() != 0) {
            throw new BusinessException("400", "只能撤回待审状态的申请", "Can only withdraw pending subscription");
        }

        // 更新状态为已取消
        Date now = new Date();
        String currentUser = getCurrentUser();

        subscriptionMapper.updateStatus(subIdLong, 3, now, currentUser);

        return WithdrawResponse.builder()
                .id(subscriptionId)
                .status(3)
                .message("申请已撤回")
                .build();
    }

    /**
     * 删除订阅（通用方法）
     *
     * <p>仅允许删除终态记录（已授权/已拒绝/已取消），审核中的申请不支持删除</p>
     */
    private WithdrawResponse deleteSubscription(String subscriptionId) {
        Long subIdLong = parseId(subscriptionId);

        Subscription subscription = subscriptionMapper.selectById(subIdLong);
        if (subscription == null) {
            throw new BusinessException("404", "订阅记录不存在", "Subscription not found");
        }

        // 检查状态：审核中(0)不允许删除
        if (subscription.getStatus() == 0) {
            throw new BusinessException("400",
                    "审核中的申请不支持删除，请使用撤回接口",
                    "Cannot delete pending subscription, please use withdraw API");
        }

        // 执行删除
        subscriptionMapper.deleteById(subIdLong);

        return WithdrawResponse.builder()
                .id(subscriptionId)
                .message("订阅记录删除成功")
                .build();
    }

    /**
     * 转换为 API 订阅响应
     */
    private ApiSubscriptionListResponse convertToApiSubscriptionResponse(Subscription subscription) {

        // 查询权限
        Permission permission = permissionMapper.selectById(subscription.getPermissionId());

        // 查询 API
        Api api = null;
        if (permission != null) {
            api = apiMapper.selectById(permission.getResourceId());
        }

        // 查询分类
        Category category = null;
        List<String> categoryPath = null;
        if (permission != null && permission.getCategoryId() != null) {
            category = categoryMapper.selectById(permission.getCategoryId());
            if (category != null) {
                categoryPath = buildCategoryPath(category);
            }
        }

        // 构建 DTO
        ApiSubscriptionListResponse.PermissionInfo permissionInfo = null;
        if (permission != null) {
            permissionInfo = ApiSubscriptionListResponse.PermissionInfo.builder()
                    .nameCn(permission.getNameCn())
                    .scope(permission.getScope())
                    .build();
        }

        ApiSubscriptionListResponse.ApiInfo apiInfo = null;
        if (api != null) {

            // 查询文档URL
            String docUrl = getApiDocUrl(api.getId());

            apiInfo = ApiSubscriptionListResponse.ApiInfo.builder()
                    .path(api.getPath())
                    .method(api.getMethod())
                    .authType(api.getAuthType())
                    .docUrl(docUrl)
                    .build();
        }

        ApiSubscriptionListResponse.CategoryInfo categoryInfo = null;
        if (category != null) {
            categoryInfo = ApiSubscriptionListResponse.CategoryInfo.builder()
                    .id(String.valueOf(category.getId()))
                    .nameCn(category.getNameCn())
                    .path(category.getPath())
                    .categoryPath(categoryPath)
                    .build();
        }

        // 查询审批人信息
        java.util.Map<String, String> approverMap = getApproverInfo(subscription.getId(), "api_permission_apply");
        ApiSubscriptionListResponse.ApproverInfo approverInfo = approverMap != null
                ? ApiSubscriptionListResponse.ApproverInfo.builder()
                        .userId(approverMap.get("userId"))
                        .userName(approverMap.get("userName"))
                        .build()
                : null;

        return ApiSubscriptionListResponse.builder()
                .id(String.valueOf(subscription.getId()))
                .appId(appContextResolver.toExternalId(subscription.getAppId()))
                .permissionId(String.valueOf(subscription.getPermissionId()))
                .permission(permissionInfo)
                .api(apiInfo)
                .category(categoryInfo)
                .approver(approverInfo)
                .status(subscription.getStatus())
                .authType(subscription.getAuthType())
                .approvalUrl(buildApprovalUrl(subscription.getId()))
                .createTime(subscription.getCreateTime())
                .build();
    }

    /**
     * 转换为事件订阅响应
     */
    private EventSubscriptionListResponse convertToEventSubscriptionResponse(Subscription subscription) {
        Permission permission = permissionMapper.selectById(subscription.getPermissionId());

        Event event = null;
        if (permission != null) {
            event = eventMapper.selectById(permission.getResourceId());
        }

        Category category = null;
        List<String> categoryPath = null;
        if (permission != null && permission.getCategoryId() != null) {
            category = categoryMapper.selectById(permission.getCategoryId());
            if (category != null) {
                categoryPath = buildCategoryPath(category);
            }
        }

        EventSubscriptionListResponse.PermissionInfo permissionInfo = null;
        if (permission != null) {
            permissionInfo = EventSubscriptionListResponse.PermissionInfo.builder()
                    .nameCn(permission.getNameCn())
                    .scope(permission.getScope())
                    .build();
        }

        EventSubscriptionListResponse.EventInfo eventInfo = null;
        if (event != null) {

            // 查询事件文档URL
            String eventDocUrl = getEventDocUrl(event.getId());

            eventInfo = EventSubscriptionListResponse.EventInfo.builder()
                    .topic(event.getTopic())
                    .docUrl(eventDocUrl)
                    .build();
        }

        EventSubscriptionListResponse.CategoryInfo categoryInfo = null;
        if (category != null) {
            categoryInfo = EventSubscriptionListResponse.CategoryInfo.builder()
                    .id(String.valueOf(category.getId()))
                    .nameCn(category.getNameCn())
                    .path(category.getPath())
                    .categoryPath(categoryPath)
                    .build();
        }

        // 查询审批人信息
        java.util.Map<String, String> approverMap = getApproverInfo(subscription.getId(), "event_permission_apply");
        EventSubscriptionListResponse.ApproverInfo approverInfo = approverMap != null
                ? EventSubscriptionListResponse.ApproverInfo.builder()
                        .userId(approverMap.get("userId"))
                        .userName(approverMap.get("userName"))
                        .build()
                : null;

        // 构造审批链接
        String approvalUrl = buildApprovalUrl(subscription.getId());

        return EventSubscriptionListResponse.builder()
                .id(String.valueOf(subscription.getId()))
                .appId(appContextResolver.toExternalId(subscription.getAppId()))
                .permissionId(String.valueOf(subscription.getPermissionId()))
                .permission(permissionInfo)
                .event(eventInfo)
                .category(categoryInfo)
                .status(subscription.getStatus())
                .channelType(subscription.getChannelType())
                .channelAddress(subscription.getChannelAddress())
                .authType(subscription.getAuthType())
                .createTime(subscription.getCreateTime())
                .approver(approverInfo)
                .approvalUrl(approvalUrl)
                .build();
    }

    /**
     * 转换为回调订阅响应
     */
    private CallbackSubscriptionListResponse convertToCallbackSubscriptionResponse(Subscription subscription) {
        Permission permission = permissionMapper.selectById(subscription.getPermissionId());

        Callback callback = null;
        if (permission != null) {
            callback = callbackMapper.selectById(permission.getResourceId());
        }

        Category category = null;
        List<String> categoryPath = null;
        if (permission != null && permission.getCategoryId() != null) {
            category = categoryMapper.selectById(permission.getCategoryId());
            if (category != null) {
                categoryPath = buildCategoryPath(category);
            }
        }

        CallbackSubscriptionListResponse.PermissionInfo permissionInfo = null;
        if (permission != null) {
            permissionInfo = CallbackSubscriptionListResponse.PermissionInfo.builder()
                    .nameCn(permission.getNameCn())
                    .scope(permission.getScope())
                    .build();
        }

        CallbackSubscriptionListResponse.CallbackInfo callbackInfo = null;
        if (callback != null) {
            String docUrl = getCallbackDocUrl(callback.getId());
            callbackInfo = CallbackSubscriptionListResponse.CallbackInfo.builder()
                    .nameCn(callback.getNameCn())
                    .docUrl(docUrl)
                    .build();
        }

        CallbackSubscriptionListResponse.CategoryInfo categoryInfo = null;
        if (category != null) {
            categoryInfo = CallbackSubscriptionListResponse.CategoryInfo.builder()
                    .id(String.valueOf(category.getId()))
                    .nameCn(category.getNameCn())
                    .path(category.getPath())
                    .categoryPath(categoryPath)
                    .build();
        }

        // 查询审批人信息
        java.util.Map<String, String> approverMap = getApproverInfo(subscription.getId(), "callback_permission_apply");
        CallbackSubscriptionListResponse.ApproverInfo approverInfo = approverMap != null
                ? CallbackSubscriptionListResponse.ApproverInfo.builder()
                        .userId(approverMap.get("userId"))
                        .userName(approverMap.get("userName"))
                        .build()
                : null;

        // 构造审批链接
        String approvalUrl = buildApprovalUrl(subscription.getId());

        return CallbackSubscriptionListResponse.builder()
                .id(String.valueOf(subscription.getId()))
                .appId(appContextResolver.toExternalId(subscription.getAppId()))
                .permissionId(String.valueOf(subscription.getPermissionId()))
                .permission(permissionInfo)
                .callback(callbackInfo)
                .category(categoryInfo)
                .status(subscription.getStatus())
                .channelType(subscription.getChannelType())
                .channelAddress(subscription.getChannelAddress())
                .authType(subscription.getAuthType())
                .createTime(subscription.getCreateTime())
                .approver(approverInfo)
                .approvalUrl(approvalUrl)
                .build();
    }

    /**
     * 转换为分类权限响应
     */
    private CategoryPermissionListResponse convertToCategoryPermissionResponse(Permission permission, Long appIdLong) {

        // 获取资源信息
        CategoryPermissionListResponse.ResourceInfo resourceInfo = null;

        if ("api".equals(permission.getResourceType())) {
            Api api = apiMapper.selectById(permission.getResourceId());
            if (api != null) {
                String docUrl = getApiDocUrl(api.getId());
                resourceInfo = CategoryPermissionListResponse.ResourceInfo.builder()
                        .path(api.getPath())
                        .method(api.getMethod())
                        .authType(api.getAuthType())
                        .docUrl(docUrl)
                        .build();
            }
        } else if ("event".equals(permission.getResourceType())) {
            Event event = eventMapper.selectById(permission.getResourceId());
            if (event != null) {
                String docUrl = getEventDocUrl(event.getId());
                resourceInfo = CategoryPermissionListResponse.ResourceInfo.builder()
                        .topic(event.getTopic())
                        .docUrl(docUrl)
                        .build();
            }
        } else if ("callback".equals(permission.getResourceType())) {
            Callback callback = callbackMapper.selectById(permission.getResourceId());
            if (callback != null) {
                String docUrl = getCallbackDocUrl(callback.getId());
                resourceInfo = CategoryPermissionListResponse.ResourceInfo.builder()
                        .docUrl(docUrl)
                        .build();
            }
        }

        // 获取是否需要审核
        Integer needApproval = permission.getNeedApproval() != null ? permission.getNeedApproval() : 1;

        // 查询是否已订阅
        int isSubscribed = 99;
        if (appIdLong != null) {
            Subscription subscription = subscriptionMapper.selectByAppIdAndPermissionId(appIdLong, permission.getId());
            if (subscription != null) {
                isSubscribed = subscription.getStatus();
            }
        }

        // 获取分类信息
        CategoryPermissionListResponse.CategoryInfo categoryInfo = null;
        if (permission.getCategoryId() != null) {
            Category category = categoryMapper.selectById(permission.getCategoryId());
            if (category != null) {
                List<String> categoryPath = buildCategoryPath(category);
                categoryInfo = CategoryPermissionListResponse.CategoryInfo.builder()
                        .id(String.valueOf(category.getId()))
                        .nameCn(category.getNameCn())
                        .path(category.getPath())
                        .categoryPath(categoryPath)
                        .build();
            }
        }

        return CategoryPermissionListResponse.builder()
                .id(String.valueOf(permission.getId()))
                .nameCn(permission.getNameCn())
                .nameEn(permission.getNameEn())
                .scope(permission.getScope())
                .status(permission.getStatus())
                .needApproval(needApproval)
                .isSubscribed(isSubscribed)
                .resource(resourceInfo)
                .category(categoryInfo)
                .build();
    }

    /**
     * 构建分类路径
     */
    private List<String> buildCategoryPath(Category category) {
        List<String> path = new ArrayList<>();
        if (category.getPath() != null) {

            // 解析 path 字段，如 /1/2/ -> [1, 2]
            String[] ids = category.getPath().split("/");
            for (String idStr : ids) {
                if (StringUtils.hasText(idStr)) {
                    try {
                        Long id = Long.parseLong(idStr);
                        Category c = categoryMapper.selectById(id);
                        if (c != null) {
                            path.add(c.getNameCn());
                        }
} catch (NumberFormatException e) {
                        log.warn("Invalid number format for path parsing", e);
}
                }
            }
        }
        return path;
    }

    /**
     * 获取 API 文档URL
     */
    private String getApiDocUrl(Long apiId) {
        List<ApiProperty> properties = apiPropertyMapper.selectByParentId(apiId);
        return properties.stream()
                .filter(p -> "docUrl".equals(p.getPropertyName()))
                .findFirst()
                .map(ApiProperty::getPropertyValue)
                .orElse(null);
    }

    /**
     * 获取回调文档URL
     */
    private String getCallbackDocUrl(Long callbackId) {
        List<CallbackProperty> properties = callbackPropertyMapper.selectByParentId(callbackId);
        return properties.stream()
                .filter(p -> "docUrl".equals(p.getPropertyName()))
                .findFirst()
                .map(CallbackProperty::getPropertyValue)
                .orElse(null);
    }

    /**
     * 获取权限文档URL
     */
    private String getPermissionDocUrl(Long permissionId) {

        // 从 permission_property 表查询 doc_url
        List<PermissionProperty> properties = permissionPropertyMapper.selectByParentId(permissionId);
        return properties.stream()
                .filter(p -> "docUrl".equals(p.getPropertyName()))
                .findFirst()
                .map(PermissionProperty::getPropertyValue)
                .orElse(null);
    }

    /**
     * 获取事件文档URL
     */
    private String getEventDocUrl(Long eventId) {

        // 从 event_property 表查询 doc_url
        List<EventProperty> properties = eventPropertyMapper.selectByParentId(eventId);
        return properties.stream()
                .filter(p -> "docUrl".equals(p.getPropertyName()))
                .findFirst()
                .map(EventProperty::getPropertyValue)
                .orElse(null);
    }

    /**
     * 获取审批人信息
     *
     * @param subscriptionId 订阅ID
     * @param businessType 业务类型（api_permission_apply, event_permission_apply, callback_permission_apply）
     * @return 包含 userId 和 userName 的 Map，如果没有审批记录则返回 null
     */
    private java.util.Map<String, String> getApproverInfo(Long subscriptionId, String businessType) {
        ApprovalRecord record = approvalRecordMapper.selectLatestByBusiness(businessType, subscriptionId);
        if (record == null || record.getCombinedNodes() == null) {
            return null;
        }

        // 解析 combinedNodes JSON，获取当前审批人
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, Object>> nodes = objectMapper.readValue(
                    record.getCombinedNodes(),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {}
            );

            // 获取当前审批节点索引
            Integer currentNode = record.getCurrentNode();
            if (currentNode == null || currentNode < 0 || currentNode >= nodes.size()) {
                return null;
            }

            // 获取当前审批人
            java.util.Map<String, Object> currentNodeInfo = nodes.get(currentNode);
            String userId = (String) currentNodeInfo.get("userId");
            String userName = (String) currentNodeInfo.get("userName");

            java.util.Map<String, String> result = new java.util.HashMap<>();
            result.put("userId", userId);
            result.put("userName", userName);
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse approval node information: subscriptionId={}, error={}", subscriptionId, e.getMessage());
            return null;
        }
    }

    /**
     * 解析 ID
     */
    private Long parseId(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BusinessException("400", "无效的ID格式", "Invalid ID format");
        }
    }

    /**
     * 获取当前用户
     */
    private String getCurrentUser() {
        return UserContextHolder.getUserId();
    }
}
