package com.xxx.it.works.wecode.v2.modules.flow.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.ConnectorPlatformPropertyService;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorPlatformConstants;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorVersionStatus;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersion;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersionRef;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.ConnectorVersionRefMapper;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.OpConnectorVersionMapper;
import com.xxx.it.works.wecode.v2.modules.flow.model.NodeTypeResolver;
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
import java.util.Locale;
import java.util.Map;
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
        Map<String, String> propertyConfig = propertyService.loadConfigBundle(appId);

        // 校验 8：JSON 语法合法性
        JsonNode config;
        try {
            config = objectMapper.readTree(orchestrationConfig);
        } catch (Exception e) {
            errors.add("编排配置 JSON 格式无效：" + e.getMessage());
            return errors;
        }

        validateConfigSize(orchestrationConfig, propertyConfig, errors);
        validateNodes(config, errors);
        validateFlowMode(config, propertyConfig, errors);
        validateParallelBranches(config, propertyConfig, errors);

        JsonNode flowConfig = config.get("flowConfig");
        if (flowConfig != null) {
            validateCacheConfig(flowConfig, propertyConfig, errors);
            validateRateLimit(flowConfig, errors);
            validateTimeout(flowConfig, errors);
        }

        JsonNode nodes = config.get("nodes");
        validateScriptNodes(nodes, propertyConfig, errors);
        validateConnectorRefs(errors);

        return errors;
    }

    /**
     * 校验编排配置 JSON 字节大小是否超过应用上限
     *
     * @param orchestrationConfig 编排配置 JSON 字符串
     * @param appId               应用ID
     * @param errors              错误列表
     */
    private void validateConfigSize(String orchestrationConfig, Map<String, String> propertyConfig, List<String> errors) {
        int maxBytes = getIntFromConfig(propertyConfig, ConnectorPlatformConstants.ITEM_FLOW_CONFIG_MAX_BYTES, 0);
        if (maxBytes > 0) {
            int actualBytes = orchestrationConfig.getBytes(StandardCharsets.UTF_8).length;
            if (actualBytes > maxBytes) {
                errors.add("编排配置 JSON 超过最大字节数限制 " + maxBytes + "，当前：" + actualBytes + "字节");
            }
        }
    }

    /**
     * 校验节点非空且至少包含一个业务节点（非 trigger/exit）
     *
     * @param config 编排配置 JSON 节点
     * @param errors 错误列表
     */
    private void validateNodes(JsonNode config, List<String> errors) {
        JsonNode nodes = config.get("nodes");
        if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
            errors.add("编排配置必须包含至少一个节点");
            return;
        }

        JsonNode edges = config.get("edges");
        boolean hasEdges = edges != null && edges.isArray() && edges.size() > 0;

        boolean hasBusinessNode = false;
        for (JsonNode node : nodes) {
            String typeStr = NodeTypeResolver.businessType(node);
            if (typeStr != null) {
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

    /**
     * 校验 flowMode 模式约束 (§6.3.3.1~§6.3.3.3)
     * <p>
     * single: 仅允许 trigger + connector + exit，不可增删
     * serial:  仅允许 script/connector 节点，connector 数量 ≤ Flow.Max.Serial.Connector.Nodes
     * parallel: 必须含 trigger + parallel + exit，分支数由 validateParallelBranches 校验
     * </p>
     */
    private void validateFlowMode(JsonNode config, Map<String, String> propertyConfig, List<String> errors) {
        JsonNode flowConfigNode = config.get("flowConfig");
        if (flowConfigNode == null) {
            errors.add("编排配置缺少 flowConfig");
            return;
        }
        JsonNode fm = flowConfigNode.get("flowMode");
        if (fm == null || fm.isNull()) {
            errors.add("缺少编排模式 flowMode（single / serial / parallel）");
            return;
        }
        String mode = fm.asText();
        JsonNode nodes = config.get("nodes");

        // 无节点时由 validateNodes 处理, 不再追加 flowMode 错误
        if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
            return;
        }

        switch (mode) {
            case "single" -> validateSingleMode(nodes, errors);
            case "serial" -> validateSerialMode(nodes, propertyConfig, errors);
            case "parallel" -> validateParallelMode(nodes, errors);
            default -> errors.add("不支持的编排模式：" + mode + "（支持 single / serial / parallel）");
        }
    }

    private void validateSingleMode(JsonNode nodes, List<String> errors) {
        boolean hasTrigger = false, hasConnector = false, hasExit = false;
        int connectorCount = 0;
        for (JsonNode node : nodes) {
            String type = NodeTypeResolver.businessType(node);
            if (type == null) continue;
            switch (type) {
                case "trigger" -> hasTrigger = true;
                case "connector" -> { hasConnector = true; connectorCount++; }
                case "exit" -> hasExit = true;
                default -> errors.add("单节点模式不允许包含 \"" + type + "\" 节点（仅允许 trigger / connector / exit）");
            }
        }
        if (!hasTrigger) { errors.add("单节点模式缺少触发节点（trigger）"); }
        if (!hasConnector) { errors.add("单节点模式缺少连接器节点（connector）"); }
        if (!hasExit) { errors.add("单节点模式缺少数据输出节点（exit）"); }
        if (connectorCount > 1) { errors.add("单节点模式只允许 1 个连接器节点，当前：" + connectorCount); }
    }

    private void validateSerialMode(JsonNode nodes, Map<String, String> propertyConfig, List<String> errors) {
        boolean hasTrigger = false, hasExit = false;
        int connectorCount = 0;
        for (JsonNode node : nodes) {
            String type = NodeTypeResolver.businessType(node);
            if (type == null) continue;
            switch (type) {
                case "trigger" -> hasTrigger = true;
                case "exit" -> hasExit = true;
                case "connector" -> connectorCount++;
                case "script" -> { /* 允许 */ }
                case "parallel" -> errors.add("串行模式不允许包含并行节点（parallel），请切换为并行模式");
                default -> errors.add("串行模式不支持节点类型：" + type);
            }
        }
        if (!hasTrigger) { errors.add("串行模式缺少触发节点（trigger）"); }
        if (!hasExit) { errors.add("串行模式缺少数据输出节点（exit）"); }

        int maxConnectorNodes = getIntFromConfig(propertyConfig,
                ConnectorPlatformConstants.ITEM_FLOW_MAX_SERIAL_CONNECTOR_NODES, 3);
        if (connectorCount > maxConnectorNodes) {
            errors.add("串行模式连接器节点数超过上限 " + maxConnectorNodes + "，当前：" + connectorCount);
        }
    }

    private void validateParallelMode(JsonNode nodes, List<String> errors) {
        boolean hasTrigger = false, hasParallel = false, hasExit = false;
        for (JsonNode node : nodes) {
            String type = NodeTypeResolver.businessType(node);
            if (type == null) continue;
            switch (type) {
                case "trigger" -> hasTrigger = true;
                case "parallel" -> hasParallel = true;
                case "exit" -> hasExit = true;
                case "connector", "script" -> { /* 允许 */ }
                default -> errors.add("并行模式不支持节点类型：" + type);
            }
        }
        if (!hasTrigger) { errors.add("并行模式缺少触发节点（trigger）"); }
        if (!hasParallel) { errors.add("并行模式缺少并行节点（parallel），请添加并行网关"); }
        if (!hasExit) { errors.add("并行模式缺少数据输出节点（exit）"); }
    }

    /**
     * 校验并行分支数是否超过应用上限
     *
     * @param config 编排配置 JSON 节点
     * @param appId  应用ID
     * @param errors 错误列表
     */
    private void validateParallelBranches(JsonNode config, Map<String, String> propertyConfig, List<String> errors) {
        JsonNode edges = config.get("edges");
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
            int maxParallelBranches = getIntFromConfig(propertyConfig, ConnectorPlatformConstants.ITEM_FLOW_MAX_PARALLEL_BRANCHES, ConnectorPlatformConstants.MAX_PARALLEL_BRANCHES);
            if (parallelBranchCount > maxParallelBranches) {
                errors.add("并行分支数超过上限 " + maxParallelBranches
                        + "，当前：" + parallelBranchCount);
            }
        }
    }

    /**
     * 校验缓存 TTL 是否在允许范围内
     *
     * @param flowConfig flowConfig 子节点
     * @param appId      应用ID
     * @param errors     错误列表
     */
    private void validateCacheConfig(JsonNode flowConfig, Map<String, String> propertyConfig, List<String> errors) {
        JsonNode cache = flowConfig.get("cache");
        if (cache != null) {
            JsonNode ttl = cache.get("ttl");
            if (ttl != null && ttl.isNumber()) {
                long ttlValue = ttl.asLong();
                int maxCacheTtl = getIntFromConfig(propertyConfig, ConnectorPlatformConstants.ITEM_FLOW_MAX_CACHE_TTL_SECONDS, ConnectorPlatformConstants.MAX_CACHE_TTL_SECONDS);
                if (ttlValue > maxCacheTtl) {
                    errors.add("缓存 TTL 超过上限 " + maxCacheTtl
                            + "秒，当前：" + ttlValue + "秒");
                }
                if (ttlValue < ConnectorPlatformConstants.MIN_CACHE_TTL_SECONDS) {
                    errors.add("缓存 TTL 必须 ≥ " + ConnectorPlatformConstants.MIN_CACHE_TTL_SECONDS + "秒");
                }
            }
        }
    }

    /**
     * 校验入站限流配置基本范围合法性
     *
     * @param flowConfig flowConfig 子节点
     * @param errors     错误列表
     */
    private void validateRateLimit(JsonNode flowConfig, List<String> errors) {
        JsonNode rateLimitConfig = flowConfig.get("rateLimitConfig");
        if (rateLimitConfig != null) {
            JsonNode maxQps = rateLimitConfig.get("maxQps");
            if (maxQps != null && maxQps.isNumber() && maxQps.asInt() <= 0) {
                errors.add("入站限流 QPS 必须大于 0");
            }
            JsonNode maxConcurrency = rateLimitConfig.get("maxConcurrency");
            if (maxConcurrency != null && maxConcurrency.isNumber() && maxConcurrency.asInt() <= 0) {
                errors.add("入站限流并发数必须大于 0");
            }
        }
    }

    /**
     * 校验 flowConfig 级别的超时值基本合法性
     *
     * @param flowConfig flowConfig 子节点
     * @param errors     错误列表
     */
    private void validateTimeout(JsonNode flowConfig, List<String> errors) {
        JsonNode timeout = flowConfig.get("timeout");
        if (timeout != null && timeout.isNumber() && timeout.asInt() <= 0) {
            errors.add("节点超时必须大于 0");
        }
    }

    /**
     * 校验脚本节点的源码合法性、长度限制、GraalJS 语法预检及超时值上限
     *
     * @param nodes  nodes 节点数组
     * @param appId  应用ID
     * @param errors 错误列表
     */
    private void validateScriptNodes(JsonNode nodes, Map<String, String> propertyConfig, List<String> errors) {
        if (nodes == null || !nodes.isArray()) {
            return;
        }
        int scriptNodeCount = 0;
        for (JsonNode node : nodes) {
            String typeStr = NodeTypeResolver.businessType(node);
            if (typeStr != null && "script".equals(typeStr)) {
                scriptNodeCount++;
                JsonNode data = node.get("data");
                if (data != null) {
                    JsonNode scriptSource = data.get("script");
                    if (scriptSource != null && !scriptSource.isNull()) {
                        String source = scriptSource.asText();
                        String nodeId = node.has("id") ? node.get("id").asText() : "unknown";
                        int maxSourceLength = getIntFromConfig(propertyConfig, ConnectorPlatformConstants.ITEM_SCRIPT_MAX_LENGTH_CHARS, ConnectorPlatformConstants.MAX_SCRIPT_SOURCE_LENGTH);
                        if (source == null || source.trim().isEmpty()) {
                            errors.add(String.format(Locale.ROOT, "脚本节点 [%s] 源码不能为空", nodeId));
                        } else if (source.length() > maxSourceLength) {
                            errors.add("脚本源码超过最大长度限制 "
                                    + maxSourceLength + "字符");
                        } else {
                            executeScriptValidation(source, nodeId, errors);
                        }
                        // 校验 13：脚本节点超时值上限
                        JsonNode scriptTimeout = data.get("timeout");
                        if (scriptTimeout != null && scriptTimeout.isNumber()) {
                            int timeoutValue = scriptTimeout.asInt();
                            int maxTimeoutSeconds = getIntFromConfig(propertyConfig, ConnectorPlatformConstants.ITEM_SCRIPT_MAX_TIMEOUT_SECONDS, ConnectorPlatformConstants.MAX_SCRIPT_TIMEOUT_SECONDS);
                            if (timeoutValue > maxTimeoutSeconds) {
                                errors.add(String.format(Locale.ROOT, "脚本节点 [%s] 超时值(%d秒) 超过上限(%d秒)",
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

    private int getIntFromConfig(Map<String, String> config, String key, int defaultVal) {
        String value = config.get(key);
        if (value == null || value.isEmpty()) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid int value '{}' for key '{}', using default {}", value, key, defaultVal);
            return defaultVal;
        }
    }

    private void executeScriptValidation(String source, String nodeId, List<String> errors) {
        try (Context ctx = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .option("js.ecmascript-version", "2022")
                .resourceLimits(ResourceLimits.newBuilder()
                        .statementLimit(1000, null)
                        .build())
                .build()) {
            ctx.eval("js", source);
        } catch (PolyglotException e) {
            errors.add(String.format(Locale.ROOT, "脚本节点 [%s] 语法错误: %s",
                    nodeId, e.getMessage()));
        } catch (Exception e) {
            log.error("Script node [{}] GraalJS validation error", nodeId, e);
            errors.add(String.format(Locale.ROOT, "脚本节点 [%s] 校验异常: %s",
                    nodeId, e.getMessage()));
        }
    }

    /**
     * 占位方法：连接器版本引用可用性由调用方通过 validateConnectorVersionRefs 校验
     *
     * @param errors 错误列表
     */
    private void validateConnectorRefs(List<String> errors) {
        // 校验 7：连接器版本引用可用性（需传入 flowVersionId 由调用方处理）
        // 此处由调用方在外部调用 validateConnectorVersionRefs()
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
            if (ref.getConnectorVersionId() == null) {
                continue;
            }
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
                JsonNode rateLimitConfig = flowConfig.get("rateLimitConfig");
                if (rateLimitConfig != null) {
                    JsonNode maxQps = rateLimitConfig.get("maxQps");
                    if (maxQps != null && maxQps.isNumber() && maxQps.asInt() > appMaxQps) {
                        errors.add("入站限流 QPS(" + maxQps.asInt() + ") 超过应用上限(" + appMaxQps + ")");
                    }
                    JsonNode maxConcurrency = rateLimitConfig.get("maxConcurrency");
                    if (maxConcurrency != null && maxConcurrency.isNumber() && maxConcurrency.asInt() > appMaxConcurrency) {
                        errors.add("入站限流并发(" + maxConcurrency.asInt() + ") 超过应用上限(" + appMaxConcurrency + ")");
                    }
                }
            }
        } catch (Exception e) {
            errors.add("解析限流配置失败：" + e.getMessage());
        }
        return errors;
    }

    /**
     * 校验各节点超时不超过应用上限
     *
     * @param config           编排配置 JSON 节点
     * @param appMaxTimeoutMs  应用最大超时（毫秒）
     * @param errors           错误列表
     */
    private void validateNodeTimeouts(JsonNode config, int appMaxTimeoutMs, List<String> errors) {
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

            validateNodeTimeouts(config, appMaxTimeoutMs, errors);
        } catch (Exception e) {
            errors.add("解析超时配置失败：" + e.getMessage());
        }
        return errors;
    }
}
