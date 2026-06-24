package com.xxx.it.works.wecode.v2.modules.version.service.impl;

import com.xxx.it.works.wecode.v2.common.constants.CommonConstants;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.ResponseCodeEnum;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.file.entity.FileEntity;
import com.xxx.it.works.wecode.v2.common.file.mapper.FileMapper;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.constants.AbilityPropertyConstants;
import com.xxx.it.works.wecode.v2.modules.ability.entity.Ability;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityProperty;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AppAbilityRelation;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AppAbilityRelationMapper;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.version.constants.VersionPropertyConstants;
import com.xxx.it.works.wecode.v2.modules.version.dto.CreateVersionRequest;
import com.xxx.it.works.wecode.v2.modules.version.dto.UpdateVersionRequest;
import com.xxx.it.works.wecode.v2.modules.version.entity.AppVersion;
import com.xxx.it.works.wecode.v2.modules.version.enums.VersionStatusEnum;
import com.xxx.it.works.wecode.v2.modules.version.mapper.AppVersionMapper;
import com.xxx.it.works.wecode.v2.modules.version.service.VersionService;
import com.xxx.it.works.wecode.v2.modules.version.vo.AppVersionAbilityVO;
import com.xxx.it.works.wecode.v2.modules.version.vo.AppVersionDetailVO;
import com.xxx.it.works.wecode.v2.modules.version.vo.VersionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 版本服务实现
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Slf4j
@Service
public class VersionServiceImpl implements VersionService {

    @Autowired
    private AppVersionMapper appVersionMapper;

    @Autowired
    private AppContextResolver appContextResolver;

    @Autowired
    private IdGeneratorStrategy idGenerator;

    @Autowired
    private ApprovalEngine approvalEngine;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private AppAbilityRelationMapper appAbilityRelationMapper;

    @Autowired
    private AbilityMapper abilityMapper;

    @Autowired
    private AbilityPropertyMapper abilityPropertyMapper;

    @Autowired
    private FileMapper fileMapper;

    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    /**
     * SemVer 版本号比较（支持任意段数，如 1.2 / 1.2.3 / 1.2.3.4）
     * <p>段数不同时，短的视为高位相同、低位补 0，如 1.2 等价于 1.2.0</p>
     *
     * @return 正数 = v1 > v2，负数 = v1 < v2，0 = 相等
     */
    private static int compareSemVer(String v1, String v2) {
        String[] p1 = v1.split("\\.");
        String[] p2 = v2.split("\\.");
        int len = Math.max(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < p1.length ? Integer.parseInt(p1[i]) : 0;
            int n2 = i < p2.length ? Integer.parseInt(p2[i]) : 0;
            int cmp = Integer.compare(n1, n2);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    @Override
    public ApiResponse<List<VersionVO>> getVersionList(String appId, Integer curPage, Integer pageSize) {
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        Long internalAppId = ctx.getInternalId();

        int offset = (curPage - 1) * pageSize;
        List<AppVersion> versions = appVersionMapper.selectByAppId(internalAppId, offset, pageSize);
        long total = appVersionMapper.countByAppId(internalAppId);

        List<VersionVO> list = new ArrayList<>();
        for (AppVersion v : versions) {
            VersionVO vo = new VersionVO();
            vo.setId(String.valueOf(v.getId()));
            vo.setVersionCode(v.getVersionCode());
            vo.setVersionDescCn(v.getVersionDescCn());
            vo.setVersionDescEn(v.getVersionDescEn());
            vo.setStatus(v.getStatus());
            vo.setCreateBy(v.getCreateBy());
            vo.setCreateTime(v.getCreateTime());
            vo.setApprovedTime(Objects.equals(v.getStatus(), VersionStatusEnum.PUBLISHED.getCode())
                    ? v.getLastUpdateTime() : null);
            list.add(vo);
        }

        return ApiResponse.success(list, ApiResponse.buildPage(curPage, pageSize, total));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createVersion(String appId, CreateVersionRequest request) {
        AppContext createCtx = appContextResolver.resolveAndValidate(appId);
        Long internalAppId = createCtx.getInternalId();

        // 校验版本号：格式 + 唯一 + 递增
        validateVersionCode(internalAppId, request.getVersionCode(), null, null);

        // 校验是否存在待发布/审批中/审批未通过的版本
        List<AppVersion> pendingVersions = appVersionMapper.selectByAppIdAndStatus(internalAppId, VersionStatusEnum.PENDING_RELEASE.getCode());
        List<AppVersion> reviewingVersions = appVersionMapper.selectByAppIdAndStatus(internalAppId, VersionStatusEnum.UNDER_REVIEW.getCode());
        List<AppVersion> rejectedVersions = appVersionMapper.selectByAppIdAndStatus(internalAppId, VersionStatusEnum.REJECTED.getCode());
        if (!pendingVersions.isEmpty() || !reviewingVersions.isEmpty() || !rejectedVersions.isEmpty()) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_PENDING_EXISTS.getCode(),
                    ResponseCodeEnum.VERSION_PENDING_EXISTS.getMessageZh(),
                    ResponseCodeEnum.VERSION_PENDING_EXISTS.getMessageEn()
            );
        }

        String currentUser = UserContextHolder.getUserId();
        AppVersion version = new AppVersion();
        version.setId(idGenerator.nextId());
        version.setAppId(internalAppId);
        version.setVersionCode(request.getVersionCode());
        version.setVersionDescCn(request.getVersionDescCn());
        version.setVersionDescEn(request.getVersionDescEn());
        version.setTenantId(CommonConstants.DEFAULT_TENANT_ID);
        version.setStatus(VersionStatusEnum.PENDING_RELEASE.getCode()); // 待发布
        version.setCreateBy(currentUser);
        version.setLastUpdateBy(currentUser);
        appVersionMapper.insert(version);

        // 自动带出当前应用已订阅的能力 ID 列表，写入版本属性表
        List<AppAbilityRelation> relations = appAbilityRelationMapper.selectByAppId(internalAppId);
        String abilityIds = relations.stream()
                .filter(r -> Objects.nonNull(r.getAbilityType()) && !Objects.equals(r.getAbilityType(), 6))
                .map(r -> String.valueOf(r.getAbilityId()))
                .collect(java.util.stream.Collectors.joining(","));
        appVersionMapper.insertProperty(idGenerator.nextId(), version.getId(), VersionPropertyConstants.PROP_ABILITY_IDS, abilityIds);

        return String.valueOf(version.getId());
    }

    @Override
    public AppVersionDetailVO getVersionDetail(String appId, Long versionId) {
        appContextResolver.resolveAndValidate(appId);

        AppVersion version = appVersionMapper.selectById(versionId);
        if (Objects.isNull(version)) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_NOT_FOUND.getCode(),
                    ResponseCodeEnum.VERSION_NOT_FOUND.getMessageZh(),
                    ResponseCodeEnum.VERSION_NOT_FOUND.getMessageEn()
            );
        }

        AppVersionDetailVO vo = new AppVersionDetailVO();
        vo.setId(String.valueOf(version.getId()));
        vo.setVersionCode(version.getVersionCode());
        vo.setVersionDescCn(version.getVersionDescCn());
        vo.setVersionDescEn(version.getVersionDescEn());
        vo.setStatus(version.getStatus());

        // 加载版本关联的能力列表（含图标）
        vo.setAbilityList(buildAbilityList(version.getId(), versionId));

        return vo;
    }

    /**
     * 解析能力ID字符串 → 加载能力详情 → 组装 VO 列表
     */
    private List<AppVersionAbilityVO> buildAbilityList(Long versionDbId, Long versionId) {
        String abilityIdsStr = appVersionMapper.selectPropertyValue(versionDbId, VersionPropertyConstants.PROP_ABILITY_IDS);
        if (!StringUtils.hasText(abilityIdsStr)) {
            return Collections.emptyList();
        }

        List<AppVersionAbilityVO> list = new ArrayList<>();
        for (String idStr : abilityIdsStr.split(",")) {
            Long abilityId = parseLongSafe(idStr.trim(), versionId);
            if (abilityId == null) {
                continue;
            }

            Ability ability = abilityMapper.selectById(abilityId);
            if (ability == null) {
                continue;
            }

            AppVersionAbilityVO vo = new AppVersionAbilityVO();
            vo.setId(String.valueOf(ability.getId()));
            vo.setAbilityNameCn(ability.getAbilityNameCn());
            vo.setAbilityNameEn(ability.getAbilityNameEn());
            vo.setIconUrl(loadAbilityIconUrl(ability.getId()));
            list.add(vo);
        }
        return list;
    }

    /**
     * 安全解析能力ID，解析失败返回 null 并记录日志
     */
    private Long parseLongSafe(String str, Long versionId) {
        if (str.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse version ability ID: versionId={}, abilityId={}", versionId, str, e);
            return null;
        }
    }

    /**
     * 从能力属性表加载图标 URL
     */
    private String loadAbilityIconUrl(Long abilityId) {
        List<AbilityProperty> props = abilityPropertyMapper.selectByParentId(abilityId);
        for (AbilityProperty p : props) {
            if (AbilityPropertyConstants.PROP_ICON.equals(p.getPropertyName())
                    && StringUtils.hasText(p.getPropertyValue())) {
                FileEntity file = fileMapper.selectByFileId(p.getPropertyValue());
                return file != null ? file.getUrl() : "";
            }
        }
        return "";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishVersion(String appId, Long versionId) {
        appContextResolver.resolveAndValidate(appId);

        AppVersion version = appVersionMapper.selectById(versionId);
        if (Objects.isNull(version) || !Objects.equals(version.getStatus(), VersionStatusEnum.PENDING_RELEASE.getCode())) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_STATUS_NOT_EDITABLE.getCode(),
                    ResponseCodeEnum.VERSION_STATUS_NOT_EDITABLE.getMessageZh(),
                    ResponseCodeEnum.VERSION_STATUS_NOT_EDITABLE.getMessageEn()
            );
        }

        String currentUser = UserContextHolder.getUserId();

        // 状态改为审批中
        version.setStatus(VersionStatusEnum.UNDER_REVIEW.getCode());
        version.setLastUpdateBy(currentUser);
        appVersionMapper.update(version);

        // 创建审批单
        approvalEngine.createApproval(
                ApprovalEngine.BusinessType.APP_VERSION_PUBLISH,
                null,
                versionId,
                currentUser,
                currentUser,
                currentUser
        );
        log.info("Version publish approval created: versionId={}", versionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawVersion(String appId, Long versionId) {
        appContextResolver.resolveAndValidate(appId);

        AppVersion version = appVersionMapper.selectById(versionId);
        if (Objects.isNull(version) || !Objects.equals(version.getStatus(), VersionStatusEnum.UNDER_REVIEW.getCode())) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_WITHDRAW_REVIEW_ONLY.getCode(),
                    ResponseCodeEnum.VERSION_WITHDRAW_REVIEW_ONLY.getMessageZh(),
                    ResponseCodeEnum.VERSION_WITHDRAW_REVIEW_ONLY.getMessageEn()
            );
        }

        String currentUser = UserContextHolder.getUserId();

        // 通过审批引擎撤回，引擎内部回调将版本状态改回待发布(1)
        ApprovalRecord pendingRecord = approvalRecordMapper.selectLatestPendingByBusiness(
                ApprovalEngine.BusinessType.APP_VERSION_PUBLISH, versionId);
        if (Objects.isNull(pendingRecord)) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_WITHDRAW_REVIEW_ONLY.getCode(),
                    ResponseCodeEnum.VERSION_WITHDRAW_REVIEW_ONLY.getMessageZh(),
                    ResponseCodeEnum.VERSION_WITHDRAW_REVIEW_ONLY.getMessageEn()
            );
        }
        approvalEngine.cancel(pendingRecord.getId(), currentUser);
        log.info("Version publish approval cancelled: versionId={}", versionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVersion(String appId, Long versionId) {
        appContextResolver.resolveAndValidate(appId);

        AppVersion version = appVersionMapper.selectById(versionId);
        if (Objects.isNull(version)) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_NOT_FOUND.getCode(),
                    ResponseCodeEnum.VERSION_NOT_FOUND.getMessageZh(),
                    ResponseCodeEnum.VERSION_NOT_FOUND.getMessageEn()
            );
        }

        if (!Objects.equals(version.getStatus(), VersionStatusEnum.PENDING_RELEASE.getCode()) && version.getStatus() != VersionStatusEnum.REJECTED.getCode()) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_DELETE_NOT_ALLOWED.getCode(),
                    ResponseCodeEnum.VERSION_DELETE_NOT_ALLOWED.getMessageZh(),
                    ResponseCodeEnum.VERSION_DELETE_NOT_ALLOWED.getMessageEn()
            );
        }

        appVersionMapper.deleteById(versionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVersion(String appId, Long versionId, UpdateVersionRequest request) {
        appContextResolver.resolveAndValidate(appId);

        AppVersion version = appVersionMapper.selectById(versionId);
        if (Objects.isNull(version)) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_NOT_FOUND.getCode(),
                    ResponseCodeEnum.VERSION_NOT_FOUND.getMessageZh(),
                    ResponseCodeEnum.VERSION_NOT_FOUND.getMessageEn()
            );
        }

        if (!Objects.equals(version.getStatus(), VersionStatusEnum.PENDING_RELEASE.getCode())) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_STATUS_NOT_EDITABLE.getCode(),
                    ResponseCodeEnum.VERSION_STATUS_NOT_EDITABLE.getMessageZh(),
                    ResponseCodeEnum.VERSION_STATUS_NOT_EDITABLE.getMessageEn()
            );
        }

        if (Objects.nonNull(request.getVersionCode())) {
            AppContext updateCtx = appContextResolver.resolveAndValidate(appId);
            Long internalAppId = updateCtx.getInternalId();

            // 校验版本号：格式 + 唯一 + 递增（排除自身）
            validateVersionCode(internalAppId, request.getVersionCode(), versionId, version.getVersionCode());

            version.setVersionCode(request.getVersionCode());
        }

        if (Objects.nonNull(request.getVersionDescCn())) {
            version.setVersionDescCn(request.getVersionDescCn());
        }
        if (Objects.nonNull(request.getVersionDescEn())) {
            version.setVersionDescEn(request.getVersionDescEn());
        }

        appVersionMapper.update(version);
    }

    /**
     * 校验版本号：格式 + 唯一性 + 递增
     *
     * @param internalAppId      应用内部 ID
     * @param versionCode        待校验的版本号
     * @param excludeId          排除的版本记录 ID（编辑时传自身 ID，新增时传 null）
     * @param currentVersionCode 当前版本号（编辑时用于排除"版本号没变"的误判，新增时传 null）
     */
    private void validateVersionCode(Long internalAppId, String versionCode, Long excludeId, String currentVersionCode) {
        // 1. 格式校验
        if (!VERSION_PATTERN.matcher(versionCode).matches()) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_CODE_FORMAT_ERROR.getCode(),
                    ResponseCodeEnum.VERSION_CODE_FORMAT_ERROR.getMessageZh(),
                    ResponseCodeEnum.VERSION_CODE_FORMAT_ERROR.getMessageEn()
            );
        }

        // 2. 唯一性校验（编辑时，版本号未改动则跳过）
        if (!versionCode.equals(currentVersionCode)
                && Objects.nonNull(appVersionMapper.selectByAppIdAndVersionCode(internalAppId, versionCode))) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_CODE_DUPLICATED.getCode(),
                    ResponseCodeEnum.VERSION_CODE_DUPLICATED.getMessageZh(),
                    ResponseCodeEnum.VERSION_CODE_DUPLICATED.getMessageEn()
            );
        }

        // 3. 递增校验：必须高于最近创建的版本号
        String latestVersionCode = appVersionMapper.selectLatestVersionCodeExcludeId(internalAppId, excludeId);
        if (Objects.nonNull(latestVersionCode) && compareSemVer(versionCode, latestVersionCode) <= 0) {
            throw new BusinessException(
                    ResponseCodeEnum.VERSION_CODE_NOT_INCREMENTAL.getCode(),
                    ResponseCodeEnum.VERSION_CODE_NOT_INCREMENTAL.getMessageZh(),
                    ResponseCodeEnum.VERSION_CODE_NOT_INCREMENTAL.getMessageEn()
            );
        }
    }
}
