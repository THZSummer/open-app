package com.xxx.open.modules.permission.service;

import com.xxx.open.common.exception.BusinessException;
import com.xxx.open.common.model.ApiResponse;
import com.xxx.open.common.util.SnowflakeIdGenerator;
import com.xxx.open.modules.api.entity.Api;
import com.xxx.open.modules.api.mapper.ApiMapper;
import com.xxx.open.modules.api.mapper.ApiPropertyMapper;
import com.xxx.open.modules.callback.entity.Callback;
import com.xxx.open.modules.callback.mapper.CallbackMapper;
import com.xxx.open.modules.callback.mapper.CallbackPropertyMapper;
import com.xxx.open.modules.category.entity.Category;
import com.xxx.open.modules.category.mapper.CategoryMapper;
import com.xxx.open.modules.event.entity.Event;
import com.xxx.open.modules.event.entity.Permission;
import com.xxx.open.modules.event.entity.PermissionProperty;
import com.xxx.open.modules.event.mapper.EventMapper;
import com.xxx.open.modules.event.mapper.PermissionMapper;
import com.xxx.open.modules.event.mapper.PermissionPropertyMapper;
import com.xxx.open.modules.permission.dto.*;
import com.xxx.open.modules.permission.entity.Subscription;
import com.xxx.open.modules.permission.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限管理服务
 * 
 * <p>实现权限申请与订阅管理功能</p>
 * <p>接口编号：#27 ~ #40</p>
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
    private final CallbackMapper callbackMapper;
    private final CallbackPropertyMapper callbackPropertyMapper;
    private final SnowflakeIdGenerator idGenerator;

    // ==================== API 权限管理 (#27-30) ====================

    /**
     * #27 获取应用 API 权限列表
     */
    public ApiResponse<List<ApiSubscriptionListResponse>> getApiSubscriptionList(
            String appId, Integer status, String keyword, Integer curPage, Integer pageSize) {
        
        log.info("获取应用 API 权限列表, appId={}, status={}, keyword={}", appId, status, keyword);

        Long appIdLong = parseId(appId);
        
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
            String categoryId, String keyword, Integer needApproval, 
            Boolean includeChildren, Integer curPage, Integer pageSize) {
        
        log.info("获取分类下 API 权限列表, categoryId={}, includeChildren={}", categoryId, includeChildren);

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

        // 转换响应
        List<CategoryPermissionListResponse> responses = permissions.stream()
                .map(this::convertToCategoryPermissionResponse)
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
        
        log.info("申请 API 权限, appId={}, permissionIds={}", appId, request.getPermissionIds());

        Long appIdLong = parseId(appId);
        
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
                    .appId(appId)
                    .permissionId(String.valueOf(permission.getId()))
                    .status(0)
                    .build());
        }

        // 批量插入订阅
        if (!subscriptions.isEmpty()) {
            subscriptionMapper.batchInsert(subscriptions);
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
        
        log.info("撤回 API 权限申请, appId={}, subscriptionId={}", appId, subscriptionId);

        return withdrawSubscription(subscriptionId);
    }

    // ==================== 事件权限管理 (#31-35) ====================

    /**
     * #31 获取应用事件订阅列表
     */
    public ApiResponse<List<EventSubscriptionListResponse>> getEventSubscriptionList(
            String appId, Integer status, String keyword, Integer curPage, Integer pageSize) {
        
        log.info("获取应用事件订阅列表, appId={}", appId);

        Long appIdLong = parseId(appId);
        
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
     * #32 获取分类下事件权限列表（权限树懒加载）
     */
    public ApiResponse<List<CategoryPermissionListResponse>> getCategoryEventPermissions(
            String categoryId, String keyword, Integer needApproval,
            Boolean includeChildren, Integer curPage, Integer pageSize) {
        
        log.info("获取分类下事件权限列表, categoryId={}", categoryId);

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

        List<CategoryPermissionListResponse> responses = permissions.stream()
                .map(this::convertToCategoryPermissionResponse)
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
     * #33 申请事件权限（支持批量）
     */
    @Transactional(rollbackFor = Exception.class)
    public PermissionSubscribeResponse subscribeEventPermissions(String appId, PermissionSubscribeRequest request) {
        
        log.info("申请事件权限, appId={}", appId);

        Long appIdLong = parseId(appId);
        
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
                    .appId(appId)
                    .permissionId(String.valueOf(permission.getId()))
                    .status(0)
                    .build());
        }

        if (!subscriptions.isEmpty()) {
            subscriptionMapper.batchInsert(subscriptions);
        }

        return PermissionSubscribeResponse.builder()
                .successCount(successRecords.size())
                .failedCount(failedRecords.size())
                .records(successRecords)
                .failedRecords(failedRecords.isEmpty() ? null : failedRecords)
                .build();
    }

    /**
     * #34 配置事件消费参数
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse configEventSubscription(String appId, String subscriptionId, SubscriptionConfigRequest request) {
        
        log.info("配置事件消费参数, appId={}, subscriptionId={}", appId, subscriptionId);

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
                .build();
    }

    /**
     * #35 撤回审核中的事件权限申请
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse withdrawEventSubscription(String appId, String subscriptionId) {
        
        log.info("撤回事件权限申请, appId={}, subscriptionId={}", appId, subscriptionId);

        return withdrawSubscription(subscriptionId);
    }

    // ==================== 回调权限管理 (#36-40) ====================

    /**
     * #36 获取应用回调订阅列表
     */
    public ApiResponse<List<CallbackSubscriptionListResponse>> getCallbackSubscriptionList(
            String appId, Integer status, String keyword, Integer curPage, Integer pageSize) {
        
        log.info("获取应用回调订阅列表, appId={}", appId);

        Long appIdLong = parseId(appId);
        
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
     * #37 获取分类下回调权限列表（权限树懒加载）
     */
    public ApiResponse<List<CategoryPermissionListResponse>> getCategoryCallbackPermissions(
            String categoryId, String keyword, Integer needApproval,
            Boolean includeChildren, Integer curPage, Integer pageSize) {
        
        log.info("获取分类下回调权限列表, categoryId={}", categoryId);

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

        List<CategoryPermissionListResponse> responses = permissions.stream()
                .map(this::convertToCategoryPermissionResponse)
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
     * #38 申请回调权限（支持批量）
     */
    @Transactional(rollbackFor = Exception.class)
    public PermissionSubscribeResponse subscribeCallbackPermissions(String appId, PermissionSubscribeRequest request) {
        
        log.info("申请回调权限, appId={}", appId);

        Long appIdLong = parseId(appId);
        
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
                    .appId(appId)
                    .permissionId(String.valueOf(permission.getId()))
                    .status(0)
                    .build());
        }

        if (!subscriptions.isEmpty()) {
            subscriptionMapper.batchInsert(subscriptions);
        }

        return PermissionSubscribeResponse.builder()
                .successCount(successRecords.size())
                .failedCount(failedRecords.size())
                .records(successRecords)
                .failedRecords(failedRecords.isEmpty() ? null : failedRecords)
                .build();
    }

    /**
     * #39 配置回调消费参数
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse configCallbackSubscription(String appId, String subscriptionId, SubscriptionConfigRequest request) {
        
        log.info("配置回调消费参数, appId={}, subscriptionId={}", appId, subscriptionId);

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
     * #40 撤回审核中的回调权限申请
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse withdrawCallbackSubscription(String appId, String subscriptionId) {
        
        log.info("撤回回调权限申请, appId={}, subscriptionId={}", appId, subscriptionId);

        return withdrawSubscription(subscriptionId);
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

        return ApiSubscriptionListResponse.builder()
                .id(String.valueOf(subscription.getId()))
                .appId(String.valueOf(subscription.getAppId()))
                .permissionId(String.valueOf(subscription.getPermissionId()))
                .permission(permissionInfo)
                .api(apiInfo)
                .category(categoryInfo)
                .status(subscription.getStatus())
                .authType(subscription.getAuthType())
                .approvalUrl("https://platform.example.com/approval/" + subscription.getId())
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
            eventInfo = EventSubscriptionListResponse.EventInfo.builder()
                    .topic(event.getTopic())
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

        return EventSubscriptionListResponse.builder()
                .id(String.valueOf(subscription.getId()))
                .appId(String.valueOf(subscription.getAppId()))
                .permissionId(String.valueOf(subscription.getPermissionId()))
                .permission(permissionInfo)
                .event(eventInfo)
                .category(categoryInfo)
                .status(subscription.getStatus())
                .channelType(subscription.getChannelType())
                .channelAddress(subscription.getChannelAddress())
                .authType(subscription.getAuthType())
                .createTime(subscription.getCreateTime())
                .build();
    }

    /**
     * 转换为回调订阅响应
     */
    private CallbackSubscriptionListResponse convertToCallbackSubscriptionResponse(Subscription subscription) {
        Permission permission = permissionMapper.selectById(subscription.getPermissionId());

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

        CallbackSubscriptionListResponse.CategoryInfo categoryInfo = null;
        if (category != null) {
            categoryInfo = CallbackSubscriptionListResponse.CategoryInfo.builder()
                    .id(String.valueOf(category.getId()))
                    .nameCn(category.getNameCn())
                    .path(category.getPath())
                    .categoryPath(categoryPath)
                    .build();
        }

        return CallbackSubscriptionListResponse.builder()
                .id(String.valueOf(subscription.getId()))
                .appId(String.valueOf(subscription.getAppId()))
                .permissionId(String.valueOf(subscription.getPermissionId()))
                .permission(permissionInfo)
                .category(categoryInfo)
                .status(subscription.getStatus())
                .channelType(subscription.getChannelType())
                .channelAddress(subscription.getChannelAddress())
                .authType(subscription.getAuthType())
                .createTime(subscription.getCreateTime())
                .build();
    }

    /**
     * 转换为分类权限响应
     */
    private CategoryPermissionListResponse convertToCategoryPermissionResponse(Permission permission) {
        // 获取资源信息
        CategoryPermissionListResponse.ResourceInfo resourceInfo = null;
        
        if ("api".equals(permission.getResourceType())) {
            Api api = apiMapper.selectById(permission.getResourceId());
            if (api != null) {
                String docUrl = getApiDocUrl(api.getId());
                resourceInfo = CategoryPermissionListResponse.ResourceInfo.builder()
                        .path(api.getPath())
                        .method(api.getMethod())
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

        // 查询是否需要审核（从属性表）
        Integer needApproval = getNeedApproval(permission.getId());

        // TODO: 查询是否已订阅（需要当前用户的应用ID）

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
                .isSubscribed(0) // 默认未订阅
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
                        // 忽略
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
        // 从属性表查询 doc_url
        // TODO: 实现属性查询
        return null;
    }

    /**
     * 获取事件文档URL
     */
    private String getEventDocUrl(Long eventId) {
        // TODO: 实现属性查询
        return null;
    }

    /**
     * 获取回调文档URL
     */
    private String getCallbackDocUrl(Long callbackId) {
        // TODO: 实现属性查询
        return null;
    }

    /**
     * 获取是否需要审核
     */
    private Integer getNeedApproval(Long permissionId) {
        // 从属性表查询 need_approval
        // 默认需要审核
        return 1;
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
        // TODO: 从安全上下文获取当前用户
        return "system";
    }
}
