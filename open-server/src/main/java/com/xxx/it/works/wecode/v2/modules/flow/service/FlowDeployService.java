package com.xxx.it.works.wecode.v2.modules.flow.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.flow.dto.FlowDeployResponse;
import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import com.xxx.it.works.wecode.v2.modules.flowversion.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowMapper;
import com.xxx.it.works.wecode.v2.modules.flowversion.mapper.OpFlowVersionMapper;
import com.xxx.it.works.wecode.v2.modules.security.AppContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 连接流部署服务
 * <p>
 * 部署版本到连接流：纯版本绑定，不改变生命周期状态
 * </p>
 */
@Slf4j
@Service
public class FlowDeployService {




    @Autowired
    public FlowDeployService(OpFlowMapper flowMapper, OpFlowVersionMapper flowVersionMapper) {
        this.flowMapper = flowMapper;
        this.flowVersionMapper = flowVersionMapper;
    }
    private final OpFlowMapper flowMapper;
    private final OpFlowVersionMapper flowVersionMapper;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private FlowCacheEvictor flowCacheEvictor;

    /**
     * 部署版本到连接流
     * <p>
     * 校验版本为已发布状态，设置 flow.deployedVersionId
     * 部署不改变生命周期状态
     * </p>
     *
     * @param flowId    连接流ID
     * @param versionId 要部署的版本ID
     * @return 部署结果
     */
    @Transactional
    public ApiResponse<FlowDeployResponse> deployVersion(Long flowId, Long versionId) {
        Long appId = AppContextHolder.requireInternalAppId();
        Flow flow = flowMapper.selectById(flowId);
        if (flow == null || !appId.equals(flow.getAppId())) {
            return ApiResponse.error("404", "连接流不存在", "Flow not found");
        }

        FlowVersion version = flowVersionMapper.selectById(versionId);
        if (version == null || !flowId.equals(version.getFlowId())) {
            return ApiResponse.error("404", "版本不存在", "Version not found");
        }

        // 仅已发布状态可部署
        if (version.getStatus() == null
                || version.getStatus() != FlowVersionStatus.PUBLISHED.getCode()) {
            return ApiResponse.error("409",
                    "仅已发布状态的版本可部署",
                    "Only published versions can be deployed");
        }

        Date now = new Date();
        String currentUser = UserContextHolder.getUserName();

        // 部署：仅绑定版本
        flowMapper.deploy(flowId, versionId, version.getVersionNumber(), now, currentUser);

        flowCacheEvictor.evictFlowConfig(flowId, stringRedisTemplate);
        flowCacheEvictor.evictFlowEntity(flowId, stringRedisTemplate);
        flowCacheEvictor.evictExecutionResults(flowId, stringRedisTemplate);

        log.info("Flow deployed: flowId={}, versionId={}, versionNumber={}, appId={}",
                flowId, versionId, version.getVersionNumber(), appId);

        FlowDeployResponse response = FlowDeployResponse.builder()
                .flowId(String.valueOf(flowId))
                .deployedVersionId(String.valueOf(versionId))
                .deployedVersionNumber(version.getVersionNumber())
                .message("部署成功，版本 " + version.getVersionNumber() + " 已绑定")
                .build();

        return ApiResponse.success(response);
    }

}
