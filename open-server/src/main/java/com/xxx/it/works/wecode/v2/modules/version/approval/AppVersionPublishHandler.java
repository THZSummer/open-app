package com.xxx.it.works.wecode.v2.modules.version.approval;

import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalBusinessHandler;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.app.entity.App;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppMapper;
import com.xxx.it.works.wecode.v2.modules.version.entity.AppVersion;
import com.xxx.it.works.wecode.v2.modules.version.enums.VersionStatusEnum;
import com.xxx.it.works.wecode.v2.modules.version.mapper.AppVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppVersionPublishHandler implements ApprovalBusinessHandler {

    private final AppVersionMapper appVersionMapper;
    private final AppMapper appMapper;

    @Override
    public String supportedBusinessType() {
        return ApprovalEngine.BusinessType.APP_VERSION_PUBLISH;
    }

    @Override
    public void onApproved(ApprovalRecord record) {
        AppVersion version = appVersionMapper.selectById(record.getBusinessId());
        if (version == null) return;
        version.setStatus(VersionStatusEnum.PUBLISHED.getCode());
        version.setLastUpdateTime(LocalDateTime.now());
        version.setLastUpdateBy(record.getApplicantId());
        appVersionMapper.update(version);
        log.info("Published app version: versionId={}", record.getBusinessId());
    }

    @Override
    public void onRejected(ApprovalRecord record) {
        AppVersion version = appVersionMapper.selectById(record.getBusinessId());
        if (version == null) return;
        version.setStatus(VersionStatusEnum.PENDING_RELEASE.getCode());
        version.setLastUpdateTime(LocalDateTime.now());
        version.setLastUpdateBy(record.getApplicantId());
        appVersionMapper.update(version);
        log.info("Rejected app version: versionId={}", record.getBusinessId());
    }

    @Override
    public void onCancelled(ApprovalRecord record) {
        AppVersion version = appVersionMapper.selectById(record.getBusinessId());
        if (version == null) return;
        version.setStatus(VersionStatusEnum.PENDING_RELEASE.getCode());
        version.setLastUpdateTime(LocalDateTime.now());
        version.setLastUpdateBy(record.getApplicantId());
        appVersionMapper.update(version);
        log.info("Cancelled app version: versionId={}", record.getBusinessId());
    }

    @Override
    public Map<String, Object> getBusinessData(Long businessId) {
        Map<String, Object> data = new HashMap<>();
        AppVersion version = appVersionMapper.selectById(businessId);
        if (version == null) {
            return data;
        }

        data.put("appVersionId", version.getId());
        data.put("versionCode", version.getVersionCode());
        data.put("status", version.getStatus());
        data.put("versionDescCn", version.getVersionDescCn());
        data.put("versionDescEn", version.getVersionDescEn());

        if (version.getAppId() != null) {
            App app = appMapper.selectById(version.getAppId());
            if (app != null) {
                String versionSuffix = " (版本" + version.getVersionCode() + ")";
                data.put("nameCn", app.getAppNameCn() + versionSuffix);
                data.put("nameEn", app.getAppNameEn() + versionSuffix);
                data.put("appNameCn", app.getAppNameCn());
                data.put("appNameEn", app.getAppNameEn());
                data.put("appId", app.getAppId());
            }
        }

        return data;
    }
}
