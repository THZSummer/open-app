package com.xxx.it.works.wecode.v2.modules.sdk.service.impl;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.ResponseCodeEnum;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.sdk.constant.SdkVersionStatusEnum;
import com.xxx.it.works.wecode.v2.modules.sdk.dto.SdkVersionCreateDTO;
import com.xxx.it.works.wecode.v2.modules.sdk.dto.SdkVersionStatusUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.sdk.dto.SdkVersionUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.sdk.entity.SdkVersionEntity;
import com.xxx.it.works.wecode.v2.modules.sdk.mapper.PermissionMapper;
import com.xxx.it.works.wecode.v2.modules.sdk.mapper.SdkVersionMapper;
import com.xxx.it.works.wecode.v2.modules.sdk.service.SdkVersionService;
import com.xxx.it.works.wecode.v2.modules.sdk.vo.SdkPermissionVO;
import com.xxx.it.works.wecode.v2.modules.sdk.vo.SdkVersionDetailVO;
import com.xxx.it.works.wecode.v2.modules.sdk.vo.SdkVersionListVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * SDK版本管理服务实现
 */
@Slf4j
@Service
public class SdkVersionServiceImpl implements SdkVersionService {

    private final SdkVersionMapper sdkVersionMapper;
    private final PermissionMapper permissionMapper;
    private final IdGeneratorStrategy idGenerator;

    @Value("${sdk.permission-category-alias:api_bussiness_app_sdk}")
    private String permissionCategoryAlias;

    public SdkVersionServiceImpl(SdkVersionMapper sdkVersionMapper,
                                  PermissionMapper permissionMapper,
                                  IdGeneratorStrategy idGenerator) {
        this.sdkVersionMapper = sdkVersionMapper;
        this.permissionMapper = permissionMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> create(SdkVersionCreateDTO request) {
        try {
            // 1. 查重（联合唯一，不区分状态）
            int dupCount = sdkVersionMapper.countDuplicate(request.getVersionName(), request.getPermissionId(), null);
            if (dupCount > 0) {
                return ApiResponse.error(ResponseCodeEnum.SDK_VERSION_ALREADY_EXISTS);
            }

            // 2. 若有 permissionId，校验权限存在于 SDK 分类下
            if (request.getPermissionId() != null) {
                int permCount = permissionMapper.countByIdAndCategoryAlias(request.getPermissionId(), permissionCategoryAlias);
                if (permCount == 0) {
                    return ApiResponse.error(ResponseCodeEnum.SDK_PERMISSION_NOT_FOUND);
                }
            }

            // 3. 构建 Entity
            String currentUser = UserContextHolder.getUserId();
            Date now = new Date();

            SdkVersionEntity entity = new SdkVersionEntity();
            entity.setId(idGenerator.nextId());
            entity.setVersionName(request.getVersionName());
            entity.setUpdateNotes(request.getUpdateNotes());
            entity.setArtifactUrl(request.getArtifactUrl());
            entity.setPermissionId(request.getPermissionId());
            entity.setStatus(SdkVersionStatusEnum.PUBLISHED.getValue());
            entity.setCreateBy(currentUser);
            entity.setCreateTime(now);
            entity.setLastUpdateBy(currentUser);
            entity.setLastUpdateTime(now);

            sdkVersionMapper.insert(entity);

            log.info("SDK version created: versionName={}, id={}", request.getVersionName(), entity.getId());
            return ApiResponse.success(String.valueOf(entity.getId()));
        } catch (Exception e) {
            log.error("Failed to create SDK version", e);
            return ApiResponse.error(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> update(Long id, SdkVersionUpdateDTO request) {
        try {
            // 1. 查存在
            SdkVersionEntity existing = sdkVersionMapper.selectByPrimaryKey(id);
            if (existing == null) {
                return ApiResponse.error(ResponseCodeEnum.SDK_VERSION_NOT_FOUND);
            }

            // 2. 校验状态：仅 status=1 可编辑
            if (existing.getStatus() != SdkVersionStatusEnum.PUBLISHED.getValue()) {
                return ApiResponse.error(ResponseCodeEnum.SDK_VERSION_DEPRECATED_CANNOT_EDIT);
            }

            // 3. 唯一性校验：(versionName, permissionId) 组合全局唯一（排除自身）
            //    versionName 创建后不可修改，用 existing 的 versionName
            int dupCount = sdkVersionMapper.countDuplicate(existing.getVersionName(), request.getPermissionId(), id);
            if (dupCount > 0) {
                return ApiResponse.error(ResponseCodeEnum.SDK_VERSION_ALREADY_EXISTS);
            }

            // 4. 若有 permissionId，校验权限存在于 SDK 分类下
            if (request.getPermissionId() != null) {
                int permCount = permissionMapper.countByIdAndCategoryAlias(request.getPermissionId(), permissionCategoryAlias);
                if (permCount == 0) {
                    return ApiResponse.error(ResponseCodeEnum.SDK_PERMISSION_NOT_FOUND);
                }
            }

            // 5. 构建更新实体（全字段更新，null 表示清空字段）
            String currentUser = UserContextHolder.getUserId();

            SdkVersionEntity entity = new SdkVersionEntity();
            entity.setId(id);
            entity.setUpdateNotes(request.getUpdateNotes());
            entity.setArtifactUrl(request.getArtifactUrl());
            entity.setPermissionId(request.getPermissionId());
            entity.setLastUpdateBy(currentUser);

            sdkVersionMapper.updateByPrimaryKey(entity);

            log.info("SDK version updated: id={}", id);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Failed to update SDK version, id={}", id, e);
            return ApiResponse.error(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> updateStatus(SdkVersionStatusUpdateDTO request) {
        try {
            // 1. 查存在
            SdkVersionEntity existing = sdkVersionMapper.selectByPrimaryKey(request.getId());
            if (existing == null) {
                return ApiResponse.error(ResponseCodeEnum.SDK_VERSION_NOT_FOUND);
            }

            String currentUser = UserContextHolder.getUserId();
            int targetStatus = request.getStatus();

            // 2. 废弃（status=2）
            if (targetStatus == SdkVersionStatusEnum.DEPRECATED.getValue()) {
                if (existing.getStatus() != SdkVersionStatusEnum.PUBLISHED.getValue()) {
                    return ApiResponse.error(ResponseCodeEnum.SDK_VERSION_ALREADY_DEPRECATED);
                }

                SdkVersionEntity entity = new SdkVersionEntity();
                entity.setId(request.getId());
                entity.setStatus(SdkVersionStatusEnum.DEPRECATED.getValue());
                entity.setDeprecateReason(request.getDeprecateReason());
                entity.setLastUpdateBy(currentUser);

                sdkVersionMapper.updateByPrimaryKeySelective(entity);

                log.info("SDK version deprecated: id={}, reason={}", request.getId(), request.getDeprecateReason());
                return ApiResponse.success();
            }

            // 3. 恢复（status=1）
            if (targetStatus == SdkVersionStatusEnum.PUBLISHED.getValue()) {
                if (existing.getStatus() != SdkVersionStatusEnum.DEPRECATED.getValue()) {
                    return ApiResponse.error(ResponseCodeEnum.SDK_VERSION_ALREADY_PUBLISHED);
                }

                SdkVersionEntity entity = new SdkVersionEntity();
                entity.setId(request.getId());
                entity.setStatus(SdkVersionStatusEnum.PUBLISHED.getValue());
                entity.setDeprecateReason("");
                entity.setLastUpdateBy(currentUser);

                sdkVersionMapper.updateByPrimaryKeySelective(entity);

                log.info("SDK version restored: id={}", request.getId());
                return ApiResponse.success();
            }

            return ApiResponse.error(ResponseCodeEnum.PARAM_ERROR, "无效的状态值: " + targetStatus, "Invalid status value: " + targetStatus);
        } catch (Exception e) {
            log.error("Failed to update SDK version status, id={}", request.getId(), e);
            return ApiResponse.error(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }

    @Override
    public ApiResponse<SdkVersionDetailVO> getById(Long id) {
        try {
            SdkVersionEntity entity = sdkVersionMapper.selectDetailByPrimaryKey(id);
            if (entity == null) {
                return ApiResponse.error(ResponseCodeEnum.SDK_VERSION_NOT_FOUND);
            }

            SdkVersionDetailVO vo = SdkVersionDetailVO.builder()
                    .id(entity.getId())
                    .versionName(entity.getVersionName())
                    .updateNotes(entity.getUpdateNotes())
                    .artifactUrl(entity.getArtifactUrl())
                    .permissionId(entity.getPermissionId())
                    .permissionName(entity.getPermissionName())
                    .permissionScope(entity.getPermissionScope())
                    .status(entity.getStatus())
                    .deprecateReason(entity.getDeprecateReason())
                    .createBy(entity.getCreateBy())
                    .createTime(entity.getCreateTime())
                    .lastUpdateBy(entity.getLastUpdateBy())
                    .lastUpdateTime(entity.getLastUpdateTime())
                    .build();

            return ApiResponse.success(vo);
        } catch (Exception e) {
            log.error("Failed to get SDK version detail, id={}", id, e);
            return ApiResponse.error(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }

    @Override
    public ApiResponse<List<SdkVersionListVO>> list(Integer status, String versionName, Integer curPage, Integer pageSize) {
        try {
            int page = curPage != null ? Math.max(1, curPage) : 1;
            int size = pageSize != null ? Math.min(100, Math.max(1, pageSize)) : 10;
            int offset = (page - 1) * size;

            // versionName 模糊匹配参数由服务层拼接（与 lookup 模块一致）
            String fuzzyVersionName = (versionName != null && !versionName.isEmpty())
                    ? "%" + versionName + "%" : null;

            long total = sdkVersionMapper.countByPage(status, fuzzyVersionName);
            List<SdkVersionEntity> entities = sdkVersionMapper.selectPage(status, fuzzyVersionName, offset, size);

            List<SdkVersionListVO> voList = new ArrayList<>(entities.size());
            for (SdkVersionEntity entity : entities) {
                SdkVersionListVO vo = SdkVersionListVO.builder()
                        .id(entity.getId())
                        .versionName(entity.getVersionName())
                        .updateNotes(entity.getUpdateNotes())
                        .permissionId(entity.getPermissionId())
                        .permissionName(entity.getPermissionName())
                        .permissionScope(entity.getPermissionScope())
                        .status(entity.getStatus())
                        .createBy(entity.getCreateBy())
                        .createTime(entity.getCreateTime())
                        .lastUpdateBy(entity.getLastUpdateBy())
                        .lastUpdateTime(entity.getLastUpdateTime())
                        .build();
                voList.add(vo);
            }

            int totalPages = (int) ((total + size - 1) / size);
            ApiResponse.PageResponse pageResp = ApiResponse.PageResponse.builder()
                    .curPage(page)
                    .pageSize(size)
                    .total(total)
                    .totalPages(totalPages)
                    .build();

            return ApiResponse.success(voList, pageResp);
        } catch (Exception e) {
            log.error("Failed to query SDK version list", e);
            return ApiResponse.error(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }

    @Override
    public ApiResponse<List<SdkPermissionVO>> listPermissions() {
        try {
            List<SdkPermissionVO> permissions = permissionMapper.selectByCategoryAlias(permissionCategoryAlias);
            return ApiResponse.success(permissions);
        } catch (Exception e) {
            log.error("Failed to query SDK permissions", e);
            return ApiResponse.error(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }
}
