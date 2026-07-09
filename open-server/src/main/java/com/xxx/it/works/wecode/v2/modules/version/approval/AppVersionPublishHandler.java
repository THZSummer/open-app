package com.xxx.it.works.wecode.v2.modules.version.approval;

import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalBusinessHandler;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.version.entity.AppVersion;
import com.xxx.it.works.wecode.v2.modules.version.enums.VersionStatusEnum;
import com.xxx.it.works.wecode.v2.modules.version.mapper.AppVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppVersionPublishHandler implements ApprovalBusinessHandler {

    private final AppVersionMapper appVersionMapper;

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
}
