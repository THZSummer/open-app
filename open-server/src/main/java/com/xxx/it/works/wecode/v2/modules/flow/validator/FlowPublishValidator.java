package com.xxx.it.works.wecode.v2.modules.flow.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.ConnectorPlatformPropertyService;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorPlatformConstants;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorVersionStatus;
import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersion;
import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersionRef;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.ConnectorVersionRefMapper;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.OpConnectorVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.ResourceLimits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 连接流版本发布校验器
 * <p>
 * 实现 FR-026 全部 9 项发布校验：
 * 1. 业务必填字段非空（nameCn/nameEn）
 * 2. 编排配置非空（nodes、edges 至少含非 trigger/exit 节点）
 * 3. 入站限流值 ≤ 应用最大值
 * 4. 节点超时值 ≤ 应用最大值
 * 5. 缓存 TTL ≤ 1296000
 * 6. 并行分支数 ≤ 8
 * 7. 连接器版本引用可用性
 * 8. JSON 语法合法性
 * 9. 脚本语法合法性（GraalJS parse 预检）
 * </p>
 */
@Slf4j
@Component
public class FlowPublishValidator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FlowPublishValidator.class);




    @Autowired
    public FlowPublishValidator(ObjectMapper objectMapper, ConnectorVersionRefMapper connectorVersionRefMapper, OpConnectorVersionMapper connectorVersionMapper, ConnectorPlatformPropertyService propertyService) {
        this.objectMapper = objectMapper;
        this.connectorVersionRefMapper = connectorVersionRefMapper;
        this.connectorVersionMapper = connectorVersionMapper;
        this.propertyService = propertyService;
    }
    private final ObjectMapper objectMapper;
    private final ConnectorVersionRefMapper connectorVersionRefMapper;
    private final OpConnectorVersionMapper connectorVersionMapper;
    private final ConnectorPlatformPropertyService propertyService;

    /**
     * 校验编排配置，返回校验错误列表
     *
     * @param orchestrationConfig 编排配置 JSON 字符串
     * @param appId               应用ID（用于按应用查询 Property 上限值）
     * @return 校验错误信息列表，空列表表示全部通过
     */
    public List<String> validateOrchestrationConfig(String orchestrationConfig, String appId) {
        List<String> errors = new ArrayList<>();

        // 校验 8：JSON 语法合法性
        JsonNode config;
        try {
            config = objectMapper.readTree(orchestrationConfig);
        } catch (Exception e) {
            errors.add("编排配置 JSON 格式无效：" + e.getMessage());
            return errors; // JSON 解析失败则后续校验无意义
        }

        // 校验 7b：编排配置 JSON 长度上限（仅当 maxBytes > 0 时生效）
        int maxBytes = propertyService.getFlowConfigMaxBytes(appId);
        if (maxBytes > 0) {
            int actualBytes = orchestrationConfig.getBytes(StandardCharsets.UTF_8).length;
            if (actualBytes > maxBytes) {
                errors.add("编排配置 JSON 超过最大字节数限制 " + maxBytes + "，当前：" + actualBytes + "字节");
            }
        }

        // 校验 2：编排配置非空
        JsonNode nodes = config.get("nodes");
        if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
            errors.add("编排配置必须包含至少一个节点");
        }

        JsonNode edges = config.get("edges");
        boolean hasEdges = edges != null && edges.isArray() && edges.size() > 0;

        // 校验 2 补充：至少含非 trigger/exit 节点
        if (nodes != null && nodes.isArray()) {
            boolean hasBusinessNode = false;
            for (JsonNode node : nodes) {
                JsonNode type = node.get("type");
                if (type != null) {
                    String typeStr = type.asText();
                    if (!"trigger".equals(typeStr) && !"exit".equals(typeStr)) {
                        hasBusinessNode = true;
                        break;
                    }
                }
            }
            if (!hasBusinessNode && hasEdges) {
                errors.add("编排配置至少需要一个业务节点（connector 或 script）");
            }
        }

        // 校验 6：并行分支数 ≤ 8
        if (edges != null && edges.isArray()) {
            int parallelBranchCount = 0;
            for (JsonNode edge : edges) {
                JsonNode data = edge.get("data");
                if (data != null) {
                    JsonNode connectionMode = data.get("connectionMode");
                    if (connectionMode != null && "parallel".equals(connectionMode.asText())) {
                        parallelBranchCount++;
                    }
                    // 无 data 或无 connectionMode 时，业务类型可能为 parallel-gateway
                    JsonNode businessType = data != null ? data.get("businessType") : null;
                    if (businessType != null && "parallel-gateway".equals(businessType.asText())) {
                        parallelBranchCount++;
                    }
                }
            }
            int maxParallelBranches = propertyService.getFlowMaxParallelBranches(appId);
            if (parallelBranchCount > maxParallelBranches) {
                errors.add("并行分支数超过上限 " + maxParallelBranches
                        + "，当前：" + parallelBranchCount);
            }
        }

        // 校验 3：入站限流值 ≤ 应用最大值（从 flowConfig 读取）
        JsonNode flowConfig = config.get("flowConfig");
        if (flowConfig != null) {
            // 校验 5：缓存 TTL ≤ 上限（从 PropertyService 获取）
            JsonNode cache = flowConfig.get("cache");
            if (cache != null) {
                JsonNode ttl = cache.get("ttl");
                if (ttl != null && ttl.isNumber()) {
                    long ttlValue = ttl.asLong();
                    int maxCacheTtl = propertyService.getFlowMaxCacheTtlSeconds(appId);
                    if (ttlValue > maxCacheTtl) {
                        errors.add("缓存 TTL 超过上限 " + maxCacheTtl
                                + "秒，当前：" + ttlValue + "秒");
                    }
                    if (ttlValue < ConnectorPlatformConstants.MIN_CACHE_TTL_SECONDS) {
                        errors.add("缓存 TTL 必须 ≥ " + ConnectorPlatformConstants.MIN_CACHE_TTL_SECONDS + "秒");
                    }
                }
            }

            // 校验 3：入站限流上限（当前仅记录校验项，应用最大值需外部传入）
            // 需由调用方传入 appMaxQps/appMaxConcurrency 参数
            // 此处仅做基本范围校验
            JsonNode rateLimit = flowConfig.get("rateLimit");
            if (rateLimit != null) {
                JsonNode qps = rateLimit.get("qps");
                if (qps != null && qps.isNumber() && qps.asInt() <= 0) {
                    errors.add("入站限流 QPS 必须大于 0");
                }
                JsonNode concurrency = rateLimit.get("concurrency");
                if (concurrency != null && concurrency.isNumber() && concurrency.asInt() <= 0) {
                    errors.add("入站限流并发数必须大于 0");
                }
            }

            // 校验 4：超时上限
            JsonNode timeout = flowConfig.get("timeout");
            if (timeout != null && timeout.isNumber() && timeout.asInt() <= 0) {
                errors.add("节点超时必须大于 0");
            }
        }

        // 校验 9：脚本语法合法性（FR-026 校验项 i: GraalJS parse 预检）
        if (nodes != null && nodes.isArray()) {
            int scriptNodeCount = 0;
            for (JsonNode node : nodes) {
                JsonNode type = node.get("type");
                if (type != null && "script".equals(type.asText())) {
                    scriptNodeCount++;
                    JsonNode data = node.get("data");
                    if (data != null) {
                        JsonNode scriptSource = data.get("script");
                        if (scriptSource != null && !scriptSource.isNull()) {
                            String source = scriptSource.asText();
                            String nodeId = node.has("id") ? node.get("id").asText() : "unknown";
                            int maxSourceLength = propertyService.getScriptMaxLengthChars(appId);
                            if (source == null || source.trim().isEmpty()) {
                                errors.add(String.format("脚本节点 [%s] 源码不能为空", nodeId));
                            } else if (source.length() > maxSourceLength) {
                                errors.add("脚本源码超过最大长度限制 "
                                        + maxSourceLength + "字符");
                            } else {
                                // FR-026 校验项 i): 使用 GraalJS polyglot 进行语法预检
                                try (Context ctx = Context.newBuilder("js")
                                        .allowExperimentalOptions(true)
                                        .option("js.ecmascript-version", "2022")
                                        .resourceLimits(ResourceLimits.newBuilder()
                                                .statementLimit(1000, null)
                                                .build())
                                        .build()) {
                                    ctx.eval("js", source);
                                } catch (PolyglotException e) {
                                    errors.add(String.format("脚本节点 [%s] 语法错误: %s",
                                            nodeId, e.getMessage()));
                                } catch (Exception e) {
                                    log.error("脚本节点 [{}] GraalJS 校验异常", nodeId, e);
                                    errors.add(String.format("脚本节点 [%s] 校验异常: %s",
                                            nodeId, e.getMessage()));
                                }
                            }
                            // 校验 13：脚本节点超时值上限
                            JsonNode scriptTimeout = data.get("timeout");
                            if (scriptTimeout != null && scriptTimeout.isNumber()) {
                                int timeoutValue = scriptTimeout.asInt();
                                int maxTimeoutSeconds = propertyService.getScriptMaxTimeoutSeconds(appId);
                                if (timeoutValue > maxTimeoutSeconds) {
                                    errors.add(String.format("脚本节点 [%s] 超时值(%d秒) 超过上限(%d秒)",
                                            nodeId, timeoutValue, maxTimeoutSeconds));
                                }
                            }
                        }
                    }
                }
            }
            if (scriptNodeCount > ConnectorPlatformConstants.MAX_SCRIPT_NODES_PER_FLOW) {
                errors.add("脚本节点数量超过上限 " + ConnectorPlatformConstants.MAX_SCRIPT_NODES_PER_FLOW
                        + "，当前：" + scriptNodeCount);
            }
        }

        // 校验 7：连接器版本引用可用性（需传入 flowVersionId 由调用方处理）
        // 此处由调用方在外部调用 validateConnectorVersionRefs()

        return errors;
    }

    /**
     * 校验连接器版本引用可用性（校验 7）
     * <p>
     * 检查编排中引用的连接器版本是否处于 PUBLISHED 状态
     * </p>
     *
     * @param flowVersionId 连接流版本ID
     * @return 校验错误信息列表
     */
    public List<String> validateConnectorVersionRefs(Long flowVersionId) {
        List<String> errors = new ArrayList<>();
        List<ConnectorVersionRef> refs = connectorVersionRefMapper.selectByFlowVersionId(flowVersionId);
        if (refs == null || refs.isEmpty()) {
            // 无连接器引用时不做校验（编排可能只含脚本节点或只有 trigger/exit）
            return errors;
        }

        for (ConnectorVersionRef ref : refs) {
            if (ref.getConnectorVersionId() == null) continue;
            ConnectorVersion cv = connectorVersionMapper.selectById(ref.getConnectorVersionId());
            if (cv == null) {
                errors.add("节点 [" + ref.getNodeId() + "] 引用的连接器版本不存在（versionId="
                        + ref.getConnectorVersionId() + "）");
            } else if (cv.getStatus() == null
                    || cv.getStatus() != ConnectorVersionStatus.PUBLISHED.getCode()) {
                errors.add("节点 [" + ref.getNodeId() + "] 引用的连接器版本不可用（status="
                        + cv.getStatus() + "，需为已发布状态）");
            }
        }

        return errors;
    }

    /**
     * 校验业务必填字段（校验 1）
     *
     * @param nameCn 中文名称
     * @param nameEn 英文名称
     * @return 校验错误信息列表
     */
    public List<String> validateBusinessFields(String nameCn, String nameEn) {
        List<String> errors = new ArrayList<>();
        if (nameCn == null || nameCn.trim().isEmpty()) {
            errors.add("中文名称不能为空");
        }
        if (nameEn == null || nameEn.trim().isEmpty()) {
            errors.add("英文名称不能为空");
        }
        return errors;
    }

    /**
     * 校验 rateLimit 不超过应用上限（校验 3 补充）
     *
     * @param orchestrationConfig 编排配置 JSON
     * @param appMaxQps           应用最大 QPS
     * @param appMaxConcurrency   应用最大并发
     * @return 校验错误信息列表
     */
    public List<String> validateRateLimitAgainstAppMax(String orchestrationConfig,
                                                        int appMaxQps, int appMaxConcurrency) {
        List<String> errors = new ArrayList<>();
        try {
            JsonNode config = objectMapper.readTree(orchestrationConfig);
            JsonNode flowConfig = config.get("flowConfig");
            if (flowConfig != null) {
                JsonNode rateLimit = flowConfig.get("rateLimit");
                if (rateLimit != null) {
                    JsonNode qps = rateLimit.get("qps");
                    if (qps != null && qps.isNumber() && qps.asInt() > appMaxQps) {
                        errors.add("入站限流 QPS(" + qps.asInt() + ") 超过应用上限(" + appMaxQps + ")");
                    }
                    JsonNode concurrency = rateLimit.get("concurrency");
                    if (concurrency != null && concurrency.isNumber() && concurrency.asInt() > appMaxConcurrency) {
                        errors.add("入站限流并发(" + concurrency.asInt() + ") 超过应用上限(" + appMaxConcurrency + ")");
                    }
                }
            }
        } catch (Exception e) {
            errors.add("解析限流配置失败：" + e.getMessage());
        }
        return errors;
    }

    /**
     * 校验超时不超过应用上限（校验 4 补充）
     *
     * @param orchestrationConfig 编排配置 JSON
     * @param appMaxTimeoutMs     应用最大超时（毫秒）
     * @return 校验错误信息列表
     */
    public List<String> validateTimeoutAgainstAppMax(String orchestrationConfig, int appMaxTimeoutMs) {
        List<String> errors = new ArrayList<>();
        try {
            JsonNode config = objectMapper.readTree(orchestrationConfig);
            JsonNode flowConfig = config.get("flowConfig");
            if (flowConfig != null) {
                JsonNode timeout = flowConfig.get("timeout");
                if (timeout != null && timeout.isNumber() && timeout.asInt() > appMaxTimeoutMs) {
                    errors.add("节点超时(" + timeout.asInt() + "ms) 超过应用上限(" + appMaxTimeoutMs + "ms)");
                }
            }

            // 同时检查各 connector 节点的 timeoutMs
            JsonNode nodes = config.get("nodes");
            if (nodes != null && nodes.isArray()) {
                for (JsonNode node : nodes) {
                    JsonNode data = node.get("data");
                    if (data != null) {
                        JsonNode nodeTimeout = data.get("timeoutMs");
                        if (nodeTimeout != null && nodeTimeout.isNumber()
                                && nodeTimeout.asInt() > appMaxTimeoutMs) {
                            errors.add("节点 [" + node.get("id").asText() + "] 超时("
                                    + nodeTimeout.asInt() + "ms) 超过应用上限(" + appMaxTimeoutMs + "ms)");
                        }
                    }
                }
            }
        } catch (Exception e) {
            errors.add("解析超时配置失败：" + e.getMessage());
        }
        return errors;
    }
}
