package com.xxx.it.works.wecode.v2.modules.flow.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.FlowLifecycleStatus;
import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersionRef;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.ConnectorVersionRefMapper;
import com.xxx.it.works.wecode.v2.modules.flow.dto.FlowCopyResponse;
import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import com.xxx.it.works.wecode.v2.modules.flowversion.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowMapper;
import com.xxx.it.works.wecode.v2.modules.flowversion.mapper.OpFlowVersionMapper;
import com.xxx.it.works.wecode.v2.modules.security.AppContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 连接流复制服务
 * <p>
 * 复制连接流及全部版本历史
 * V3 特性：
 * - 仅同应用内复制
 * - 名称追加 _copy_xxxxx（4 位十六进制随机后缀），不做名称唯一性校验（与创建/更新一致）
 * - 新连接流状态为已停止（STOPPED），部署版本清空
 * - 版本复制：待审批/已驳回/已撤回 → 草稿；草稿/已发布/已失效 → 保持原状态；已物理删除 → 跳过
 * - 同步复制 connector_version_ref 引用关系（新版本 ID 重建引用记录）
 * - 审批记录不复制
 * - 源连接流及其版本、引用关系不受任何影响
 * </p>
 */
@Slf4j
@Service
public class FlowCopyService {

    private final OpFlowMapper flowMapper;
    private final OpFlowVersionMapper flowVersionMapper;
    private final ConnectorVersionRefMapper connectorVersionRefMapper;
    private final IdGeneratorStrategy idGenerator;

    @Autowired
    public FlowCopyService(OpFlowMapper flowMapper, OpFlowVersionMapper flowVersionMapper,
                           ConnectorVersionRefMapper connectorVersionRefMapper, IdGeneratorStrategy idGenerator) {
        this.flowMapper = flowMapper;
        this.flowVersionMapper = flowVersionMapper;
        this.connectorVersionRefMapper = connectorVersionRefMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 复制连接流
     *
     * @param sourceFlowId 源连接流ID
     * @return 复制的连接流信息
     */
    @Transactional
    public ApiResponse<FlowCopyResponse> copyFlow(Long sourceFlowId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow sourceFlow = flowMapper.selectById(sourceFlowId);
        if (sourceFlow == null || !appId.equals(sourceFlow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        // 生成新名称：原始名称 + _copy_ + 4位十六进制随机数（不做唯一性校验）
        String newNameCn = generateCopyName(sourceFlow.getNameCn());
        String newNameEn = generateCopyName(sourceFlow.getNameEn());

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();
        long newFlowId = idGenerator.nextId();

        // 创建新连接流实体
        Flow newFlow = new Flow();
        newFlow.setId(newFlowId);
        newFlow.setNameCn(newNameCn);
        newFlow.setNameEn(newNameEn);
        newFlow.setDescriptionCn(sourceFlow.getDescriptionCn());
        newFlow.setDescriptionEn(sourceFlow.getDescriptionEn());
        newFlow.setIconFileId(sourceFlow.getIconFileId());
        newFlow.setLifecycleStatus(FlowLifecycleStatus.STOPPED.getCode());
        newFlow.setAppId(appId);
        newFlow.setDeployedVersionId(null);
        newFlow.setDeployedVersionNumber(null);
        newFlow.setCreateTime(now);
        newFlow.setLastUpdateTime(now);
        newFlow.setCreateBy(currentUser);
        newFlow.setLastUpdateBy(currentUser);

        flowMapper.insert(newFlow);

        // 复制全部版本（已物理删除的跳过）
        List<FlowVersion> sourceVersions = flowVersionMapper.selectListByFlowId(sourceFlowId, null);
        int versionCount = 0;
        if (sourceVersions != null) {
            for (FlowVersion sourceVersion : sourceVersions) {
                // 跳过已物理删除的版本
                if (sourceVersion.getStatus() != null
                        && sourceVersion.getStatus() == FlowVersionStatus.DELETED.getCode()) {
                    continue;
                }

                long newVersionId = idGenerator.nextId();
                FlowVersion newVersion = new FlowVersion();
                newVersion.setId(newVersionId);
                newVersion.setFlowId(newFlowId);
                newVersion.setVersionNumber(sourceVersion.getVersionNumber());

                // 版本状态转换：待审批/已撤回/已驳回 → 草稿；其他保持原状态
                Integer convertedStatus = convertCopiedVersionStatus(sourceVersion.getStatus());
                newVersion.setStatus(convertedStatus);

                newVersion.setOrchestrationConfig(sourceVersion.getOrchestrationConfig());
                // 状态改为草稿的版本（原非草稿），清除发布时间和发布人
                if (convertedStatus == FlowVersionStatus.DRAFT.getCode()
                        && sourceVersion.getStatus() != FlowVersionStatus.DRAFT.getCode()) {
                    newVersion.setPublishedTime(null);
                    newVersion.setPublishedBy(null);
                } else {
                    newVersion.setPublishedTime(sourceVersion.getPublishedTime());
                    newVersion.setPublishedBy(sourceVersion.getPublishedBy());
                }
                newVersion.setCreateTime(now);
                newVersion.setLastUpdateTime(now);
                newVersion.setCreateBy(currentUser);
                newVersion.setLastUpdateBy(currentUser);
                flowVersionMapper.insert(newVersion);

                // 同步复制 connector_version_ref 引用关系（源数据只读，不修改）
                copyConnectorVersionRefs(sourceVersion.getId(), newFlowId, newVersionId, currentUser, now);

                versionCount++;
            }
        }

        log.info("Flow copied: sourceFlowId={}, newFlowId={}, nameCn={}, versionCount={}, appId={}",
                sourceFlowId, newFlowId, newNameCn, versionCount, appId);

        FlowCopyResponse response = FlowCopyResponse.builder()
                .flowId(String.valueOf(newFlowId))
                .nameCn(newNameCn)
                .nameEn(newNameEn)
                .lifecycleStatus(FlowLifecycleStatus.STOPPED.getCode())
                .versionCount(versionCount)
                .message("复制成功，共复制 " + versionCount + " 个版本")
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 版本状态转换：待审批/已撤回/已驳回 → 草稿；其他保持原状态
     */
    private Integer convertCopiedVersionStatus(Integer sourceStatus) {
        if (sourceStatus == null) {
            return FlowVersionStatus.DRAFT.getCode();
        }
        FlowVersionStatus status = FlowVersionStatus.fromValue(sourceStatus);
        if (status == FlowVersionStatus.PENDING_APPROVAL
                || status == FlowVersionStatus.WITHDRAWN
                || status == FlowVersionStatus.REJECTED) {
            return FlowVersionStatus.DRAFT.getCode();
        }
        return sourceStatus;
    }

    /**
     * 复制连接器版本引用关系（只读源数据，写入新版本 ID 的引用记录）
     */
    private void copyConnectorVersionRefs(Long sourceVersionId, Long newFlowId, Long newVersionId,
                                          String operator, Date now) {
        List<ConnectorVersionRef> sourceRefs = connectorVersionRefMapper.selectByFlowVersionId(sourceVersionId);
        if (sourceRefs == null || sourceRefs.isEmpty()) {
            return;
        }
        List<ConnectorVersionRef> newRefs = new ArrayList<>();
        for (ConnectorVersionRef sourceRef : sourceRefs) {
            ConnectorVersionRef newRef = new ConnectorVersionRef();
            newRef.setId(idGenerator.nextId());
            newRef.setFlowId(newFlowId);
            newRef.setFlowVersionId(newVersionId);
            newRef.setNodeId(sourceRef.getNodeId());
            newRef.setConnectorId(sourceRef.getConnectorId());
            newRef.setConnectorVersionId(sourceRef.getConnectorVersionId());
            newRef.setCreateTime(now);
            newRef.setLastUpdateTime(now);
            newRef.setCreateBy(operator);
            newRef.setLastUpdateBy(operator);
            newRefs.add(newRef);
        }
        if (!newRefs.isEmpty()) {
            connectorVersionRefMapper.insertBatch(newRefs);
        }
    }

    /**
     * 生成复制名称：original + _copy_ + 4位十六进制随机数
     */
    private String generateCopyName(String originalName) {
        Random random = new Random();
        int suffix = random.nextInt(0x10000);
        return originalName + "_copy_" + String.format(Locale.ROOT, "%04x", suffix);
    }
}
