package com.xxx.it.works.wecode.v2.modules.api.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.api.dto.*;
import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import com.xxx.it.works.wecode.v2.modules.api.entity.ApiProperty;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.category.entity.Category;
import com.xxx.it.works.wecode.v2.modules.category.mapper.CategoryMapper;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.event.entity.PermissionProperty;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionPropertyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalFlow;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;

/**
 * API 管理服务
 *
 * <p>实现 API 注册、编辑、删除、撤回等功能</p>
 * <p>接口编号：#9 ~ #14</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiService {

    private final ApiMapper apiMapper;
    private final ApiPropertyMapper apiPropertyMapper;
    private final PermissionMapper permissionMapper;
    private final PermissionPropertyMapper permissionPropertyMapper;
    private final CategoryMapper categoryMapper;
    private final IdGeneratorStrategy idGenerator;
    private final ApprovalEngine approvalEngine;
    private final ApprovalFlowMapper approvalFlowMapper;

    // ==================== 列表查询 (#9) ====================

    /**
     * #9 获取 API 列表
     */
    public ApiResponse<List<ApiListResponse>> getApiList(ApiListRequest request) {
        log.info("Get API list, request={}", request);

        // 解析参数
        Long categoryId = parseId(request.getCategoryId());

        // 分页参数
        int curPage = request.getCurPage() != null ? request.getCurPage() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        int offset = (curPage - 1) * pageSize;

        // 查询列表
        List<Api> apiList = apiMapper.selectList(
                categoryId,
                request.getStatus(),
                request.getKeyword(),
                offset,
                pageSize
        );

        // 查询总数
        Long total = apiMapper.countList(
                categoryId,
                request.getStatus(),
                request.getKeyword()
        );

        // ✅ 新增：批量查询所有API的properties，提取docUrl
        Map<Long, String> docUrlMap = new HashMap<>();
        if (!apiList.isEmpty()) {
            List<Long> apiIds = apiList.stream().map(Api::getId).collect(Collectors.toList());
            List<ApiProperty> allProperties = apiPropertyMapper.selectByParentIds(apiIds);

            // 构建 Map: apiId -> docUrl
            for (ApiProperty prop : allProperties) {
                if ("docUrl".equals(prop.getPropertyName()) && prop.getPropertyValue() != null) {
                    docUrlMap.put(prop.getParentId(), prop.getPropertyValue());
                }
            }
        }

        // 转换响应（传入docUrlMap）
        List<ApiListResponse> responses = apiList.stream()
                .map(api -> convertToListResponse(api, docUrlMap))
                .collect(Collectors.toList());

        // 构建分页响应
        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(curPage)
                .pageSize(pageSize)
                .total(total)
                .totalPages((int) Math.ceil((double) total / pageSize))
                .build();

        return ApiResponse.success(responses, page);
    }

    // ==================== 详情查询 (#10) ====================

    /**
     * #10 获取 API 详情
     */
    public ApiDetailResponse getApiDetail(String id) {
        log.info("Get API detail, id={}", id);

        Long apiId = parseId(id);

        // 查询 API
        Api api = apiMapper.selectById(apiId);
        if (api == null) {
            throw new BusinessException("404", "API 不存在", "API not found");
        }

        // 查询权限
        Permission permission = permissionMapper.selectByResource("api", apiId);

        // 查询属性
        List<ApiProperty> properties = apiPropertyMapper.selectByParentId(apiId);

        // 转换响应
        return convertToDetailResponse(api, permission, properties);
    }

    // ==================== 注册 API (#11) ====================

    /**
     * #11 注册 API
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiDetailResponse createApi(ApiCreateRequest request) {
        log.info("Register API, nameCn={}, path={}", request.getNameCn(), request.getPath());

        Long categoryId = parseId(request.getCategoryId());

        // 检查分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("400", "分类不存在", "Category not found");
        }

        // 检查 Scope 是否已存在
        Integer scopeCount = permissionMapper.countByScope(request.getPermission().getScope());
        if (scopeCount > 0) {
            throw new BusinessException("409", "Scope 已存在", "Scope already exists");
        }

        Date now = new Date();
        String currentUser = getCurrentUser();

        // 创建 API
        Api api = new Api();
        api.setId(idGenerator.nextId());
        api.setNameCn(request.getNameCn());
        api.setNameEn(request.getNameEn());
        api.setPath(request.getPath());
        api.setMethod(request.getMethod().toUpperCase(Locale.ROOT));
        api.setAuthType(request.getAuthType() != null ? request.getAuthType() : 1); // 默认 SOA
        api.setCategoryId(categoryId); // 设置分类ID

        // 设置 needApproval（仅影响权限申请审批，不影响注册审批）
        Integer needApproval = request.getPermission().getNeedApproval() != null ?
            request.getPermission().getNeedApproval() : 1;

        // 先插入 API（状态暂设为待审）
        api.setStatus(1); // 待审
        api.setCreateTime(now);
        api.setLastUpdateTime(now);
        api.setCreateBy(currentUser);
        api.setLastUpdateBy(currentUser);

        apiMapper.insert(api);

        // 创建权限（先创建权限，再创建审批记录）
        Permission permission = new Permission();
        permission.setId(idGenerator.nextId());
        permission.setNameCn(request.getPermission().getNameCn());
        permission.setNameEn(request.getPermission().getNameEn());
        permission.setScope(request.getPermission().getScope());
        permission.setResourceType("api");
        permission.setResourceId(api.getId());
        permission.setCategoryId(categoryId);

        // ✅ v2.8.0新增：设置 needApproval 和 resourceNodes 字段
        permission.setNeedApproval(needApproval);
        permission.setResourceNodes(request.getPermission().getResourceNodes());
        permission.setStatus(1);
        permission.setCreateTime(now);
        permission.setLastUpdateTime(now);
        permission.setCreateBy(currentUser);
        permission.setLastUpdateBy(currentUser);

        permissionMapper.insert(permission);

        // ✅ 获取注册审批节点（两级：场景+全局）
        List<ApprovalNodeDto> approvalNodes = approvalEngine.composeApprovalNodes(
            ApprovalEngine.BusinessType.API_REGISTER, null);

        if (approvalNodes.isEmpty()) {

            // 无审批节点配置，直接发布
            api.setStatus(2); // 已发布
            apiMapper.update(api);
            log.info("API registration does not require approval (no approval nodes configured), publish directly: apiId={}", api.getId());
        } else {

            // 有审批节点，创建审批单
            try {
                approvalEngine.createApproval(
                    ApprovalEngine.BusinessType.API_REGISTER,
                    permission.getId(),
                    api.getId(),
                    currentUser,
                    currentUser,
                    currentUser
                );
                log.info("Create API registration approval record: apiId={}, nodesCount={}", api.getId(), approvalNodes.size());
            } catch (Exception e) {
                log.warn("Failed to create approval record, API remains pending status: apiId={}", api.getId(), e);
            }
        }

        // 创建 API 属性
        if (request.getProperties() != null && !request.getProperties().isEmpty()) {
            List<ApiProperty> apiProperties = request.getProperties().stream()
                    .map(prop -> {
                        ApiProperty apiProp = new ApiProperty();
                        apiProp.setId(idGenerator.nextId());
                        apiProp.setParentId(api.getId());
                        apiProp.setPropertyName(prop.getPropertyName());
                        apiProp.setPropertyValue(prop.getPropertyValue());
                        apiProp.setStatus(1);
                        apiProp.setCreateTime(now);
                        apiProp.setLastUpdateTime(now);
                        apiProp.setCreateBy(currentUser);
                        apiProp.setLastUpdateBy(currentUser);
                        return apiProp;
                    })
                    .collect(Collectors.toList());

            apiPropertyMapper.batchInsert(apiProperties);
        }

        // 返回详情
        return getApiDetail(String.valueOf(api.getId()));
    }

    // ==================== 更新 API (#12) ====================

    /**
     * #12 更新 API
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiDetailResponse updateApi(String id, ApiUpdateRequest request) {
        log.info("Update API, id={}, nameCn={}", id, request.getNameCn());

        Long apiId = parseId(id);

        // 查询 API
        Api api = apiMapper.selectById(apiId);
        if (api == null) {
            throw new BusinessException("404", "API 不存在", "API not found");
        }

        Long categoryId = null;
        if (request.getCategoryId() != null && !request.getCategoryId().trim().isEmpty()) {
            categoryId = parseId(request.getCategoryId());

            // 检查分类是否存在
            Category category = categoryMapper.selectById(categoryId);
            if (category == null) {
                throw new BusinessException("400", "分类不存在", "Category not found");
            }
        }

        Date now = new Date();
        String currentUser = getCurrentUser();

        // 更新 API（只更新非null字段）
        if (request.getNameCn() != null && !request.getNameCn().trim().isEmpty()) {
            api.setNameCn(request.getNameCn());
        }
        if (request.getNameEn() != null && !request.getNameEn().trim().isEmpty()) {
            api.setNameEn(request.getNameEn());
        }
        if (request.getPath() != null && !request.getPath().trim().isEmpty()) {
            api.setPath(request.getPath());
        }
        if (request.getMethod() != null && !request.getMethod().trim().isEmpty()) {
            api.setMethod(request.getMethod().toUpperCase(Locale.ROOT));
        }
        if (categoryId != null) {
            api.setCategoryId(categoryId);
        }
        if (request.getAuthType() != null) {
            api.setAuthType(request.getAuthType());
        }
        api.setLastUpdateTime(now);
        api.setLastUpdateBy(currentUser);

        apiMapper.update(api);

        // 更新权限
        Permission permission = permissionMapper.selectByResource("api", apiId);
        if (permission != null && request.getPermission() != null) {
            if (request.getPermission().getNameCn() != null && !request.getPermission().getNameCn().trim().isEmpty()) {
                permission.setNameCn(request.getPermission().getNameCn());
            }
            if (request.getPermission().getNameEn() != null && !request.getPermission().getNameEn().trim().isEmpty()) {
                permission.setNameEn(request.getPermission().getNameEn());
            }
            if (request.getPermission().getScope() != null && !request.getPermission().getScope().trim().isEmpty()) {
                permission.setScope(request.getPermission().getScope());
            }
            if (request.getPermission().getNeedApproval() != null) {
                permission.setNeedApproval(request.getPermission().getNeedApproval());
            }
            if (request.getPermission().getResourceNodes() != null) {
                permission.setResourceNodes(request.getPermission().getResourceNodes());
            }
            if (categoryId != null) {
                permission.setCategoryId(categoryId);
            }
            permission.setLastUpdateTime(now);
            permission.setLastUpdateBy(currentUser);

            permissionMapper.update(permission);
        }

        // 更新属性（删除旧的，插入新的）
        if (request.getProperties() != null) {
            apiPropertyMapper.deleteByParentId(apiId);

            if (!request.getProperties().isEmpty()) {
                List<ApiProperty> apiProperties = request.getProperties().stream()
                        .map(prop -> {
                            ApiProperty apiProp = new ApiProperty();
                            apiProp.setId(idGenerator.nextId());
                            apiProp.setParentId(api.getId());
                            apiProp.setPropertyName(prop.getPropertyName());
                            apiProp.setPropertyValue(prop.getPropertyValue());
                            apiProp.setStatus(1);
                            apiProp.setCreateTime(now);
                            apiProp.setLastUpdateTime(now);
                            apiProp.setCreateBy(currentUser);
                            apiProp.setLastUpdateBy(currentUser);
                            return apiProp;
                        })
                        .collect(Collectors.toList());

                apiPropertyMapper.batchInsert(apiProperties);
            }
        }

        // 返回详情
        return getApiDetail(String.valueOf(api.getId()));
    }

    // ==================== 删除 API (#13) ====================

    /**
     * #13 删除 API
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteApi(String id) {
        log.info("Delete API, id={}", id);

        Long apiId = parseId(id);

        // 查询 API
        Api api = apiMapper.selectById(apiId);
        if (api == null) {
            throw new BusinessException("404", "API 不存在", "API not found");
        }

        // TODO: 检查订阅关系（需要 Subscription 表）
        // 目前先跳过，等 TASK-008 实现后再补充

        // 删除属性
        apiPropertyMapper.deleteByParentId(apiId);

        // 删除权限属性
        Permission permission = permissionMapper.selectByResource("api", apiId);
        if (permission != null) {
            permissionPropertyMapper.deleteByParentId(permission.getId());
            permissionMapper.deleteById(permission.getId());
        }

        // 删除 API
        apiMapper.deleteById(apiId);
    }

    // ==================== 撤回 API (#14) ====================

    /**
     * #14 撤回审核中的 API
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiDetailResponse withdrawApi(String id) {
        log.info("Withdraw API, id={}", id);

        Long apiId = parseId(id);

        // 查询 API
        Api api = apiMapper.selectById(apiId);
        if (api == null) {
            throw new BusinessException("404", "API 不存在", "API not found");
        }

        // 检查状态
        if (api.getStatus() != 1) {
            throw new BusinessException("400", "只能撤回待审状态的 API", "Can only withdraw pending API");
        }

        // 更新状态为草稿
        Date now = new Date();
        String currentUser = getCurrentUser();

        api.setStatus(0); // 草稿
        api.setLastUpdateTime(now);
        api.setLastUpdateBy(currentUser);

        apiMapper.update(api);

        // 返回详情
        return getApiDetail(String.valueOf(api.getId()));
    }

    // ==================== 私有方法 ====================

    /**
     * 转换为列表响应
     */
    private ApiListResponse convertToListResponse(Api api, Map<Long, String> docUrlMap) {

        // 构建权限 DTO
        Permission permission = permissionMapper.selectByResource("api", api.getId());

        PermissionDto permissionDto = null;
        if (permission != null) {
            permissionDto = PermissionDto.builder()
                    .id(String.valueOf(permission.getId()))
                    .scope(permission.getScope())
                    .status(permission.getStatus())
                    .build();
        }

        // ✅ 从 Map 中获取 docUrl
        String docUrl = docUrlMap.get(api.getId());

        return ApiListResponse.builder()
                .id(String.valueOf(api.getId()))
                .nameCn(api.getNameCn())
                .nameEn(api.getNameEn())
                .path(api.getPath())
                .method(api.getMethod())
                .authType(api.getAuthType())
                .categoryId(String.valueOf(api.getCategoryId()))
                .categoryName(api.getCategoryName()) // 从 JOIN 查询获取
                .status(api.getStatus())
                .permission(permissionDto)
                .docUrl(docUrl)  // ✅ 新增
                .createTime(api.getCreateTime())
                .build();
    }

    /**
     * 转换为详情响应
     */
    private ApiDetailResponse convertToDetailResponse(Api api, Permission permission, List<ApiProperty> properties) {

        // 构建权限 DTO
        PermissionDto permissionDto = null;
        if (permission != null) {
            permissionDto = PermissionDto.builder()
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

        // 构建属性 DTO 列表
        List<PropertyDto> propertyDtos = properties.stream()
                .map(prop -> PropertyDto.builder()
                        .propertyName(prop.getPropertyName())
                        .propertyValue(prop.getPropertyValue())
                        .build())
                .collect(Collectors.toList());

        return ApiDetailResponse.builder()
                .id(String.valueOf(api.getId()))
                .nameCn(api.getNameCn())
                .nameEn(api.getNameEn())
                .path(api.getPath())
                .method(api.getMethod())
                .authType(api.getAuthType())
                .categoryId(String.valueOf(api.getCategoryId()))
                .categoryName(api.getCategoryName()) // 从 JOIN 查询获取
                .status(api.getStatus())
                .createTime(api.getCreateTime())
                .createBy(api.getCreateBy())
                .permission(permissionDto)
                .properties(propertyDtos)
                .build();
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
