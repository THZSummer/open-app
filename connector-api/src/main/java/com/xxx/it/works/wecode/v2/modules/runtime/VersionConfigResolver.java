package com.xxx.it.works.wecode.v2.modules.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.model.FlowConfig;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ResolvedFlowConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 版本配置解析器 (Phase 2 核心)
 * <p>
 * 按 flow_t.deployed_version_id 读取 FlowVersion 快照,
 * 解析 orchestration_config 中的 nodes/edges,
 * 遍历 connector 节点按 connectorVersionRef 加载 ConnectorVersion 快照.
 * 优先 Redis 缓存 (EntityCacheManager), miss 回源 MySQL.
 * <p>
 * 异常处理:
 * <ul>
 *   <li>deployed_version_id 为 NULL → 503 (Flow not deployed)</li>
 *   <li>FlowVersion 已删除 → 500 (Deployed version not found)</li>
 *   <li>ConnectorVersion 不存在/已失效 → 标记节点失败, 其余节点继续</li>
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
        // Step 1: 加载 FlowEntity
        return flowReadRepository.findById(flowId)
                .switchIfEmpty(Mono.error(new FlowNotDeployedException("Flow not found: " + flowId)))
                // Step 2: 校验 deployed_version_id
                .flatMap(flow -> {
                    if (flow.getDeployedVersionId() == null) {
                        return Mono.error(new FlowNotDeployedException(
                                "Flow not deployed: flowId=" + flowId));
                    }
                    // Step 3: 加载 FlowVersion
                    return entityCacheManager.getFlowVersion(flow.getDeployedVersionId())
                            .switchIfEmpty(Mono.error(new DeployedVersionNotFoundException(
                                    "Deployed version not found: versionId=" + flow.getDeployedVersionId())))
                            .flatMap(flowVersion -> resolveConnectorVersions(flow, flowVersion));
                });
    }

    /**
     * 解析连接器版本配置
     */
    private Mono<ResolvedFlowConfig> resolveConnectorVersions(FlowEntity flow, FlowVersionEntity flowVersion) {
        // 解析 flowConfig
        FlowConfig flowConfig = parseFlowConfigFromOrchestration(flowVersion);

        // 遍历连接器节点, 加载对应的 ConnectorVersion
        return loadConnectorVersions(flowVersion)
                .map(connectorConfigs -> new ResolvedFlowConfig(flow, flowVersion, connectorConfigs, flowConfig));
    }

    /**
     * 从编排配置中解析 flowConfig
     */
    private FlowConfig parseFlowConfigFromOrchestration(FlowVersionEntity flowVersion) {
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

    /**
     * 遍历 orchestrationConfig 中的 connector 节点, 并行加载 ConnectorVersion
     */
    private Mono<Map<String, ConnectorVersionEntity>> loadConnectorVersions(FlowVersionEntity flowVersion) {
        Map<String, Long> connectorNodeRefs = extractConnectorRefs(flowVersion);

        if (connectorNodeRefs.isEmpty()) {
            log.debug("No connector nodes found in flow version: versionId={}", flowVersion.getId());
            return Mono.just(Map.of());
        }

        // 并行加载所有 ConnectorVersion
        Map<String, ConnectorVersionEntity> result = new LinkedHashMap<>();
        return Flux.fromIterable(connectorNodeRefs.entrySet())
                .flatMap(entry -> entityCacheManager.getConnectorVersion(entry.getValue())
                        .map(cv -> {
                            result.put(entry.getKey(), cv);
                            return cv;
                        })
                        .onErrorResume(e -> {
                            log.warn("Failed to load ConnectorVersion for node {}: cvId={}, error={}",
                                    entry.getKey(), entry.getValue(), e.getMessage());
                            return Mono.empty(); // 节点失败, 其余继续
                        }),
                        connectorNodeRefs.size()) // 最大并发数
                .then(Mono.just(result));
    }

    /**
     * 从编排配置中提取 connector 节点 → connectorVersionRef 映射
     * <p>
     * 遍历 nodes[] 查找 type='connector' 的节点,
     * 从 node.data.connectorVersionRef 读取连接器版本ID
     * </p>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Long> extractConnectorRefs(FlowVersionEntity flowVersion) {
        Map<String, Long> refs = new LinkedHashMap<>();
        try {
            String orchestrationConfig = flowVersion.getOrchestrationConfig();
            if (orchestrationConfig == null || orchestrationConfig.isBlank()) {
                return refs;
            }
            JsonNode config = objectMapper.readTree(orchestrationConfig);
            JsonNode nodes = config.get("nodes");
            if (nodes == null || !nodes.isArray()) {
                return refs;
            }
            for (JsonNode node : nodes) {
                String nodeType = node.has("type") ? node.get("type").asText() : null;
                if (!"connector".equals(nodeType)) {
                    continue;
                }
                String nodeId = node.get("id").asText();

                // 从 data.connectorVersionRef 读取连接器版本ID
                JsonNode data = node.get("data");
                if (data != null && data.has("connectorVersionRef")) {
                    JsonNode refNode = data.get("connectorVersionRef");
                    if (refNode.isNumber()) {
                        refs.put(nodeId, refNode.asLong());
                    } else if (refNode.isTextual()) {
                        try {
                            refs.put(nodeId, Long.parseLong(refNode.asText()));
                        } catch (NumberFormatException e) {
                            log.warn("Invalid connectorVersionRef for node {}: {}", nodeId, refNode.asText());
                        }
                    }
                }

                // 向后兼容: 直接从 data.connectorVersionId 读取
                if (!refs.containsKey(nodeId) && data != null && data.has("connectorVersionId")) {
                    JsonNode cvIdNode = data.get("connectorVersionId");
                    if (cvIdNode.isNumber()) {
                        refs.put(nodeId, cvIdNode.asLong());
                    } else if (cvIdNode.isTextual()) {
                        try {
                            refs.put(nodeId, Long.parseLong(cvIdNode.asText()));
                        } catch (NumberFormatException e) {
                            log.warn("Invalid connectorVersionId for node {}: {}", nodeId, cvIdNode.asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract connector refs from orchestration config: versionId={}, error={}",
                    flowVersion.getId(), e.getMessage());
        }
        log.debug("Extracted {} connector refs from flow version: versionId={}", refs.size(), flowVersion.getId());
        return refs;
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
