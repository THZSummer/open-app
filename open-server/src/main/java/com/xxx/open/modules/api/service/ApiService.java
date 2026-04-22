package com.xxx.open.modules.api.service;

import com.xxx.open.common.exception.BusinessException;
import com.xxx.open.common.model.ApiResponse;
import com.xxx.open.common.util.SnowflakeIdGenerator;
import com.xxx.open.modules.api.dto.*;
import com.xxx.open.modules.api.entity.Api;
import com.xxx.open.modules.api.entity.ApiProperty;
import com.xxx.open.modules.api.mapper.ApiMapper;
import com.xxx.open.modules.api.mapper.ApiPropertyMapper;
import com.xxx.open.modules.category.entity.Category;
import com.xxx.open.modules.category.mapper.CategoryMapper;
import com.xxx.open.modules.event.entity.Permission;
import com.xxx.open.modules.event.entity.PermissionProperty;
import com.xxx.open.modules.event.mapper.PermissionMapper;
import com.xxx.open.modules.event.mapper.PermissionPropertyMapper;
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
    private final SnowflakeIdGenerator idGenerator;

    // ==================== 列表查询 (#9) ====================

    /**
     * #9 获取 API 列表
     */
    public ApiResponse<List<ApiListResponse>> getApiList(ApiListRequest request) {
        log.info("获取 API 列表, request={}", request);

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

        // 转换响应
        List<ApiListResponse> responses = apiList.stream()
                .map(this::convertToListResponse)
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
        log.info("获取 API 详情, id={}", id);

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
        log.info("注册 API, nameCn={}, path={}", request.getNameCn(), request.getPath());

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
        api.setMethod(request.getMethod().toUpperCase());
        api.setCategoryId(categoryId); // 设置分类ID
        api.setStatus(1); // 待审
        api.setCreateTime(now);
        api.setLastUpdateTime(now);
        api.setCreateBy(currentUser);
        api.setLastUpdateBy(currentUser);

        apiMapper.insert(api);

        // 创建权限
        Permission permission = new Permission();
        permission.setId(idGenerator.nextId());
        permission.setNameCn(request.getPermission().getNameCn());
        permission.setNameEn(request.getPermission().getNameEn());
        permission.setScope(request.getPermission().getScope());
        permission.setResourceType("api");
        permission.setResourceId(api.getId());
        permission.setCategoryId(categoryId);
        permission.setStatus(1);
        permission.setCreateTime(now);
        permission.setLastUpdateTime(now);
        permission.setCreateBy(currentUser);
        permission.setLastUpdateBy(currentUser);

        permissionMapper.insert(permission);

        // 创建权限属性（审批流程ID）
        if (StringUtils.hasText(request.getPermission().getApprovalFlowId())) {
            PermissionProperty flowProperty = new PermissionProperty();
            flowProperty.setId(idGenerator.nextId());
            flowProperty.setParentId(permission.getId());
            flowProperty.setPropertyName("approval_flow_id");
            flowProperty.setPropertyValue(request.getPermission().getApprovalFlowId());
            flowProperty.setStatus(1);
            flowProperty.setCreateTime(now);
            flowProperty.setLastUpdateTime(now);
            flowProperty.setCreateBy(currentUser);
            flowProperty.setLastUpdateBy(currentUser);

            List<PermissionProperty> permissionProperties = new ArrayList<>();
            permissionProperties.add(flowProperty);
            permissionPropertyMapper.batchInsert(permissionProperties);
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
        log.info("更新 API, id={}, nameCn={}", id, request.getNameCn());

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
        if (categoryId != null) {
            api.setCategoryId(categoryId);
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
        log.info("删除 API, id={}", id);

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
        log.info("撤回 API, id={}", id);

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
    private ApiListResponse convertToListResponse(Api api) {
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

        return ApiListResponse.builder()
                .id(String.valueOf(api.getId()))
                .nameCn(api.getNameCn())
                .nameEn(api.getNameEn())
                .path(api.getPath())
                .method(api.getMethod())
                .categoryId(String.valueOf(api.getCategoryId()))
                .categoryName(api.getCategoryName()) // 从 JOIN 查询获取
                .status(api.getStatus())
                .permission(permissionDto)
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
        // TODO: 从安全上下文获取当前用户
        return "system";
    }
}
