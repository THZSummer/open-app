package com.xxx.it.works.wecode.v2.modules.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.model.FlowConfig;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ResolvedFlowConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 版本配置解析器 (Phase 2 核心)
 * <p>
 * 按 flow_t.deployed_version_id 读取 FlowVersion 编排快照,
 * 解析 orchestration_config 中的 nodes/edges/flowConfig.
 * v6.0: 连接器配置直接从 node.data.connectorVersionConfig 快照获取,
 * 不再查询 connector_version_t (编排自包含).
 * 优先 Redis 缓存 (EntityCacheManager), miss 回源 MySQL.
 * <p>
 * 异常处理:
 * <ul>
 *   <li>deployed_version_id 为 NULL → 503 (Flow not deployed)</li>
 *   <li>FlowVersion 已删除 → 500 (Deployed version not found)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class VersionConfigResolver {

    private final OpFlowReadRepository flowReadRepository;
    private final EntityCacheManager entityCacheManager;
    private final FlowConfigParser flowConfigParser;
    private final ObjectMapper objectMapper;

    public VersionConfigResolver(OpFlowReadRepository flowReadRepository,
                                  EntityCacheManager entityCacheManager,
                                  FlowConfigParser flowConfigParser,
                                  ObjectMapper objectMapper) {
        this.flowReadRepository = flowReadRepository;
        this.entityCacheManager = entityCacheManager;
        this.flowConfigParser = flowConfigParser;
        this.objectMapper = objectMapper;
    }

    /**
     * 解析连接流版本配置
     *
     * @param flowId 连接流ID
     * @return Mono<ResolvedFlowConfig> 已解析的完整配置快照
     */
    public Mono<ResolvedFlowConfig> resolveFlowVersion(Long flowId) {
        return flowReadRepository.findById(flowId)
                .switchIfEmpty(Mono.error(new FlowNotDeployedException("Flow not found: " + flowId)))
                .flatMap(flow -> {
                    if (flow.getDeployedVersionId() == null) {
                        return Mono.error(new FlowNotDeployedException(
                                "Flow not deployed: flowId=" + flowId));
                    }
                    return entityCacheManager.getFlowVersion(flow.getDeployedVersionId())
                            .switchIfEmpty(Mono.error(new DeployedVersionNotFoundException(
                                    "Deployed version not found: versionId=" + flow.getDeployedVersionId())))
                            .map(flowVersion -> {
                                FlowConfig flowConfig = parseFlowConfig(flowVersion);
                                return new ResolvedFlowConfig(flow, flowVersion, flowConfig);
                            });
                });
    }

    /**
     * 从编排配置中解析 flowConfig
     */
    private FlowConfig parseFlowConfig(FlowVersionEntity flowVersion) {
        try {
            String orchestrationConfig = flowVersion.getOrchestrationConfig();
            if (orchestrationConfig == null || orchestrationConfig.isBlank()) {
                return FlowConfig.defaults();
            }
            JsonNode config = objectMapper.readTree(orchestrationConfig);
            JsonNode flowConfigNode = config.get("flowConfig");
            if (flowConfigNode != null) {
                return flowConfigParser.parseFlowConfig(flowConfigNode.toString());
            }
            return FlowConfig.defaults();
        } catch (Exception e) {
            log.warn("Failed to parse flowConfig from orchestration, using defaults: versionId={}, error={}",
                    flowVersion.getId(), e.getMessage());
            return FlowConfig.defaults();
        }
    }

    // ===== 异常类 =====

    /**
     * 连接流未部署异常 (→ HTTP 503)
     */
    public static class FlowNotDeployedException extends RuntimeException {
        public FlowNotDeployedException(String message) {
            super(message);
        }
    }

    /**
     * 已部署版本不存在异常 (→ HTTP 500)
     */
    public static class DeployedVersionNotFoundException extends RuntimeException {
        public DeployedVersionNotFoundException(String message) {
            super(message);
        }
    }
}
