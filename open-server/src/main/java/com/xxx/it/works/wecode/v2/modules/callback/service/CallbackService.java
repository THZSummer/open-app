package com.xxx.it.works.wecode.v2.modules.callback.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.callback.dto.*;
import com.xxx.it.works.wecode.v2.modules.callback.entity.Callback;
import com.xxx.it.works.wecode.v2.modules.callback.entity.CallbackProperty;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.category.entity.Category;
import com.xxx.it.works.wecode.v2.modules.category.mapper.CategoryMapper;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalFlow;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 回调服务
 *
 * <p>提供回调资源管理功能，包括回调注册、编辑、删除、撤回等</p>
 * <p>接口编号：#21 ~ #26</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackService {

    private final CallbackMapper callbackMapper;
    private final CallbackPropertyMapper callbackPropertyMapper;
    private final PermissionMapper permissionMapper;
    private final CategoryMapper categoryMapper;
    private final IdGeneratorStrategy idGenerator;
    private final ApprovalEngine approvalEngine;
    private final ApprovalFlowMapper approvalFlowMapper;

    /**
     * Scope 格式正则表达式
     * 格式：callback:{module}:{identifier}
     */
    private static final Pattern SCOPE_PATTERN = Pattern.compile("^callback:[a-z][a-z0-9_]*:[a-z][a-z0-9_-]*$");

    // ==================== 回调 CRUD ====================

    /**
     * #21 获取回调列表
     *
     * @param categoryId 分类ID（可选）
     * @param status 状态（可选）
     * @param keyword 搜索关键词（可选）
     * @param curPage 当前页码（默认 1）
     * @param pageSize 每页大小（默认 20）
     * @return 分页响应
     */
    public ApiResponse<List<CallbackListResponse>> getCallbackList(
            String categoryId, Integer status, String keyword,
            Integer curPage, Integer pageSize) {

        // 默认值处理
        int page = curPage != null && curPage > 0 ? curPage : 1;
        int size = pageSize != null && pageSize > 0 ? Math.min(pageSize, 100) : 20;
        int offset = (page - 1) * size;

        // 解析分类ID
        Long categoryIdLong = null;
        if (categoryId != null && !categoryId.isEmpty()) {
            try {
                categoryIdLong = Long.parseLong(categoryId);
            } catch (NumberFormatException e) {
                throw BusinessException.badRequest("分类ID格式错误", "Invalid category ID format");
            }
        }

        // 查询列表
        List<Callback> callbacks = callbackMapper.selectList(
                categoryIdLong, status, keyword, offset, size);

        // 查询总数
        long total = callbackMapper.countList(categoryIdLong, status, keyword);

        // ✅ 新增：批量查询所有回调的properties，提取docUrl
        Map<Long, String> docUrlMap = new HashMap<>();
        if (!callbacks.isEmpty()) {
            List<Long> callbackIds = callbacks.stream().map(Callback::getId).collect(Collectors.toList());
            List<CallbackProperty> allProperties = callbackPropertyMapper.selectByParentIds(callbackIds);

            // 构建 Map: callbackId -> docUrl
            for (CallbackProperty prop : allProperties) {
                if ("docUrl".equals(prop.getPropertyName()) && prop.getPropertyValue() != null) {
                    docUrlMap.put(prop.getParentId(), prop.getPropertyValue());
                }
            }
        }

        // 转换响应（传入docUrlMap）
        List<CallbackListResponse> responseList = callbacks.stream()
                .map(callback -> convertToListResponse(callback, docUrlMap))
                .collect(Collectors.toList());

        // 构建分页信息
        ApiResponse.PageResponse pageResponse = ApiResponse.PageResponse.builder()
                .curPage(page)
                .pageSize(size)
                .total(total)
                .totalPages((int) Math.ceil((double) total / size))
                .build();

        return ApiResponse.success(responseList, pageResponse);
    }

    /**
     * #22 获取回调详情
     *
     * @param id 回调ID
     * @return 回调详情
     */
    public CallbackResponse getCallbackById(Long id) {

        // 查询回调
        Callback callback = callbackMapper.selectById(id);
        if (callback == null) {
            throw BusinessException.notFound("回调不存在", "Callback not found");
        }

        // 查询权限信息
        Permission permission = permissionMapper.selectByResource("callback", id);

        // 查询属性列表
        List<CallbackProperty> properties = callbackPropertyMapper.selectByParentId(id);

        return convertToResponse(callback, permission, properties);
    }

    /**
     * #23 注册回调
     *
     * <p>注册回调成功，同时创建权限资源</p>
     *
     * @param request 创建请求
     * @return 回调响应
     */
    @Transactional(rollbackFor = Exception.class)
    public CallbackResponse createCallback(CallbackCreateRequest request) {

        // 解析分类ID
        Long categoryId;
        try {
            categoryId = Long.parseLong(request.getCategoryId());
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest("分类ID格式错误", "Invalid category ID format");
        }

        // 验证分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BusinessException.notFound("分类不存在", "Category not found");
        }

        // 验证 Scope 格式
        String scope = request.getPermission().getScope();
        if (!SCOPE_PATTERN.matcher(scope).matches()) {
            throw BusinessException.badRequest(
                    "Scope 格式错误，正确格式：callback:{module}:{identifier}",
                    "Invalid scope format. Expected: callback:{module}:{identifier}");
        }

        // 检查 Scope 是否已存在
        Permission existingPermission = permissionMapper.selectByScope(scope);
        if (existingPermission != null) {
            throw new BusinessException("409",
                    "Scope 已存在: " + scope,
                    "Scope already exists: " + scope);
        }

        // 生成回调ID
        Long callbackId = idGenerator.nextId();

        // 创建回调实体
        Callback callback = new Callback();
        callback.setId(callbackId);
        callback.setNameCn(request.getNameCn());
        callback.setNameEn(request.getNameEn());
        callback.setCategoryId(categoryId); // 设置分类ID

        // 设置 needApproval（仅影响权限申请审批，不影响注册审批）
        Integer needApproval = request.getPermission().getNeedApproval() != null
            ? request.getPermission().getNeedApproval() : 1;

        // 先插入回调（状态暂设为待审）
        callback.setStatus(1); // 待审
        callback.setCreateTime(new Date());
        callback.setLastUpdateTime(new Date());
        callback.setCreateBy(UserContextHolder.getUserId());
        callback.setLastUpdateBy(UserContextHolder.getUserId());

        // 保存回调
        callbackMapper.insert(callback);

        // 创建权限（先创建权限，再创建审批记录）
        Long permissionId = idGenerator.nextId();
        Permission permission = new Permission();
        permission.setId(permissionId);
        permission.setNameCn(request.getPermission().getNameCn());
        permission.setNameEn(request.getPermission().getNameEn());
        permission.setScope(scope);
        permission.setResourceType("callback");
        permission.setResourceId(callbackId);
        permission.setCategoryId(categoryId);

        // ✅ v2.8.0新增：审批配置字段
        permission.setNeedApproval(needApproval);
        permission.setResourceNodes(request.getPermission().getResourceNodes());
        permission.setStatus(1); // 启用
        permission.setCreateTime(new Date());
        permission.setLastUpdateTime(new Date());
        permission.setCreateBy(UserContextHolder.getUserId());
        permission.setLastUpdateBy(UserContextHolder.getUserId());

        // 保存权限
        permissionMapper.insert(permission);

        // ✅ 获取注册审批节点（两级：场景+全局）
        List<ApprovalNodeDto> approvalNodes = approvalEngine.composeApprovalNodes(
            ApprovalEngine.BusinessType.CALLBACK_REGISTER, null);

        if (approvalNodes.isEmpty()) {

            // 无审批节点配置，直接发布
            callback.setStatus(2); // 已发布
            callbackMapper.update(callback);
            log.info("Callback registration does not require approval (no approval nodes configured), publish directly: callbackId={}", callbackId);
        } else {

            // 有审批节点，创建审批单
            try {
                approvalEngine.createApproval(
                    ApprovalEngine.BusinessType.CALLBACK_REGISTER,
                    permissionId,
                    callbackId,
                    UserContextHolder.getUserId(),
                    UserContextHolder.getUserName(),
                    UserContextHolder.getUserId()
                );
                log.info("Create callback registration approval record: callbackId={}, nodesCount={}", callbackId, approvalNodes.size());
            } catch (Exception e) {
                log.warn("Failed to create approval record, callback remains pending status: callbackId={}", callbackId, e);
            }
        }

        // 保存属性（如果有）
        List<CallbackProperty> savedProperties = new ArrayList<>();
        if (request.getProperties() != null && !request.getProperties().isEmpty()) {
            savedProperties = saveProperties(callbackId, request.getProperties());
        }

        log.info("Callback registered successfully: id={}, nameCn={}, scope={}", callbackId, request.getNameCn(), scope);

        return convertToResponse(callback, permission, savedProperties);
    }

    /**
     * #24 更新回调
     *
     * @param id 回调ID
     * @param request 更新请求
     * @return 回调响应
     */
    @Transactional(rollbackFor = Exception.class)
    public CallbackResponse updateCallback(Long id, CallbackUpdateRequest request) {

        // 查询回调
        Callback callback = callbackMapper.selectById(id);
        if (callback == null) {
            throw BusinessException.notFound("回调不存在", "Callback not found");
        }


        // 更新回调基本信息
        if (request.getNameCn() != null) {
            callback.setNameCn(request.getNameCn());
        }
        if (request.getNameEn() != null) {
            callback.setNameEn(request.getNameEn());
        }
        callback.setLastUpdateTime(new Date());
        callback.setLastUpdateBy(UserContextHolder.getUserId());

        // 如果更新分类ID
        Long categoryId = callback.getCategoryId();
        if (request.getCategoryId() != null) {
            try {
                categoryId = Long.parseLong(request.getCategoryId());
                Category category = categoryMapper.selectById(categoryId);
                if (category == null) {
                    throw BusinessException.notFound("分类不存在", "Category not found");
                }
                callback.setCategoryId(categoryId); // 更新分类ID
            } catch (NumberFormatException e) {
                throw BusinessException.badRequest("分类ID格式错误", "Invalid category ID format");
            }
        }

        // 保存回调
        callbackMapper.update(callback);

        // 更新权限
        Permission permission = permissionMapper.selectByResource("callback", id);
        if (permission != null && request.getPermission() != null) {
            if (request.getPermission().getNameCn() != null) {
                permission.setNameCn(request.getPermission().getNameCn());
            }
            if (request.getPermission().getNameEn() != null) {
                permission.setNameEn(request.getPermission().getNameEn());
            }
            if (categoryId != null) {
                permission.setCategoryId(categoryId);
            }
            if (request.getPermission().getNeedApproval() != null) {
                permission.setNeedApproval(request.getPermission().getNeedApproval());
            }
            if (request.getPermission().getResourceNodes() != null) {
                permission.setResourceNodes(request.getPermission().getResourceNodes());
            }
            permission.setLastUpdateTime(new Date());
            permission.setLastUpdateBy(UserContextHolder.getUserId());
            permissionMapper.update(permission);
        }

        // 更新属性（如果有）
        List<CallbackProperty> savedProperties = new ArrayList<>();
        if (request.getProperties() != null) {

            // 删除原有属性
            callbackPropertyMapper.deleteByParentId(id);

            // 保存新属性
            if (!request.getProperties().isEmpty()) {
                savedProperties = saveProperties(id, request.getProperties());
            }
        } else {

            // 查询现有属性
            savedProperties = callbackPropertyMapper.selectByParentId(id);
        }

        log.info("Callback updated successfully: id={}, nameCn={}", id, callback.getNameCn());

        return convertToResponse(callback, permission, savedProperties);
    }

    /**
     * #25 删除回调
     *
     * <p>删除回调，检查订阅关系</p>
     *
     * @param id 回调ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCallback(Long id) {

        // 查询回调
        Callback callback = callbackMapper.selectById(id);
        if (callback == null) {
            throw BusinessException.notFound("回调不存在", "Callback not found");
        }

        // 检查订阅关系
        Permission permission = permissionMapper.selectByResource("callback", id);
        if (permission != null) {
            long subscriptionCount = permissionMapper.countSubscriptionsByPermissionId(permission.getId());
            if (subscriptionCount > 0) {
                throw new BusinessException("409",
                        "回调被 " + subscriptionCount + " 个应用订阅，无法删除",
                        "Callback is subscribed by " + subscriptionCount + " applications, cannot delete");
            }

            // 删除权限
            permissionMapper.deleteById(permission.getId());
        }

        // 删除属性
        callbackPropertyMapper.deleteByParentId(id);

        // 删除回调
        callbackMapper.deleteById(id);

        log.info("Callback deleted successfully: id={}", id);
    }

    /**
     * #26 撤回审核中的回调
     *
     * @param id 回调ID
     * @return 回调响应
     */
    @Transactional(rollbackFor = Exception.class)
    public CallbackResponse withdrawCallback(Long id) {

        // 查询回调
        Callback callback = callbackMapper.selectById(id);
        if (callback == null) {
            throw BusinessException.notFound("回调不存在", "Callback not found");
        }

        // 检查状态（只有待审状态可以撤回）
        if (callback.getStatus() != 1) {
            throw new BusinessException("400",
                    "只有待审状态的回调可以撤回",
                    "Only pending callbacks can be withdrawn");
        }

        // 更新状态为草稿
        callback.setStatus(0); // 草稿
        callback.setLastUpdateTime(new Date());
        callback.setLastUpdateBy(UserContextHolder.getUserId());

        // 保存
        callbackMapper.update(callback);

        log.info("Callback withdrawn successfully: id={}", id);

        // 查询权限和属性
        Permission permission = permissionMapper.selectByResource("callback", id);
        List<CallbackProperty> properties = callbackPropertyMapper.selectByParentId(id);

        return convertToResponse(callback, permission, properties);
    }

    // ==================== 私有方法 ====================

    /**
     * 保存属性列表
     */
    private List<CallbackProperty> saveProperties(Long callbackId, List<CallbackPropertyDto> propertyDtos) {
        List<CallbackProperty> properties = new ArrayList<>();
        Date now = new Date();

        for (CallbackPropertyDto dto : propertyDtos) {
            CallbackProperty property = new CallbackProperty();
            property.setId(idGenerator.nextId());
            property.setParentId(callbackId);
            property.setPropertyName(dto.getPropertyName());
            property.setPropertyValue(dto.getPropertyValue());
            property.setStatus(1); // 启用
            property.setCreateTime(now);
            property.setLastUpdateTime(now);
            property.setCreateBy(UserContextHolder.getUserId());
            property.setLastUpdateBy(UserContextHolder.getUserId());
            properties.add(property);
        }

        // 批量插入
        if (!properties.isEmpty()) {
            callbackPropertyMapper.batchInsert(properties);
        }

        return properties;
    }

    /**
     * 转换为列表响应
     */
    private CallbackListResponse convertToListResponse(Callback callback, Map<Long, String> docUrlMap) {

        // 查询权限信息
        Permission permission = permissionMapper.selectByResource("callback", callback.getId());

        // ✅ 从 Map 中获取 docUrl
        String docUrl = docUrlMap.get(callback.getId());

        return CallbackListResponse.builder()
                .id(String.valueOf(callback.getId()))
                .nameCn(callback.getNameCn())
                .nameEn(callback.getNameEn())
                .categoryId(String.valueOf(callback.getCategoryId()))
                .categoryName(callback.getCategoryName()) // 从 JOIN 查询获取
                .status(callback.getStatus())
                .permission(permission != null ? convertToPermissionDto(permission) : null)
                .docUrl(docUrl)  // ✅ 新增
                .createTime(callback.getCreateTime())
                .build();
    }

    /**
     * 转换为详情响应
     */
    private CallbackResponse convertToResponse(Callback callback, Permission permission,
                                                List<CallbackProperty> properties) {
        return CallbackResponse.builder()
                .id(String.valueOf(callback.getId()))
                .nameCn(callback.getNameCn())
                .nameEn(callback.getNameEn())
                .categoryId(String.valueOf(callback.getCategoryId()))
                .categoryName(callback.getCategoryName()) // 从 JOIN 查询获取
                .status(callback.getStatus())
                .permission(permission != null ? convertToPermissionDto(permission) : null)
                .properties(convertToPropertyDtos(properties))
                .createTime(callback.getCreateTime())
                .createBy(callback.getCreateBy())
                .lastUpdateTime(callback.getLastUpdateTime())
                .lastUpdateBy(callback.getLastUpdateBy())
                .build();
    }

    /**
     * 转换为权限 DTO
     */
    private PermissionDto convertToPermissionDto(Permission permission) {
        return PermissionDto.builder()
                .id(String.valueOf(permission.getId()))
                .nameCn(permission.getNameCn())
                .nameEn(permission.getNameEn())
                .scope(permission.getScope())
                .status(permission.getStatus())

                // ✅ v2.8.0新增：审批配置字段
                .needApproval(permission.getNeedApproval())
                .resourceNodes(permission.getResourceNodes())
                .build();
    }

    /**
     * 转换为属性 DTO 列表
     */
    private List<CallbackPropertyDto> convertToPropertyDtos(List<CallbackProperty> properties) {
        if (properties == null || properties.isEmpty()) {
            return new ArrayList<>();
        }
        return properties.stream()
                .map(p -> CallbackPropertyDto.builder()
                        .propertyName(p.getPropertyName())
                        .propertyValue(p.getPropertyValue())
                        .build())
                .collect(Collectors.toList());
    }
}
