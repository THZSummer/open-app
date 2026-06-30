package com.xxx.it.works.wecode.v2.common.config;

import com.xxx.it.works.wecode.v2.common.enums.ConnectorPlatformConstants;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookupWhitelistMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 连接器平台配置服务（v2.0 Lookup 批量读取）
 *
 * <p>从 openplatform_lookup_classify_t + openplatform_lookup_item_t 批量读取平台配置项，
 * 每项均提供硬编码兜底默认值；DB 不可用或配置缺失时绝不抛异常或返回 null。</p>
 *
 * <p>查找规则：
 * <ul>
 *   <li>平台默认值：path = {@code CEC.Open}, classify_code = {@code Connector.Platform.Config}</li>
 *   <li>应用覆盖值：path = {@code CEC.Open}, classify_code = {@code Connector.Platform.{appId}.Config}</li>
 *   <li>合并策略：应用覆盖值优先，未覆盖项回退平台默认值，仍缺失则使用硬编码默认值</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @date 2026-06-26
 */
@Slf4j
@Service
public class ConnectorPlatformPropertyService {

    private final LookupWhitelistMapper lookupWhitelistMapper;

    public ConnectorPlatformPropertyService(LookupWhitelistMapper lookupWhitelistMapper) {
        this.lookupWhitelistMapper = lookupWhitelistMapper;
    }

    // ================================================================
    // 批量读取（v2.0 核心优化，推荐调用方使用此方法）
    // ================================================================

    /**
     * 加载某应用的全部配置合并 Map。
     * 先查平台默认组，再查应用覆盖组，应用值覆盖平台值。
     *
     * @param appId 应用 ID，为 {@code null} 时仅返回平台默认值
     * @return item_code → item_value 的合并 Map，DB 不可用时返回空 Map
     */
    public Map<String, String> loadConfigBundle(String appId) {
        // 1. 加载平台默认值
        Map<String, String> config = loadClassifyItems(
                ConnectorPlatformConstants.LOOKUP_PATH,
                ConnectorPlatformConstants.LOOKUP_CLASSIFY_PLATFORM_CONFIG);

        // 2. 加载应用覆盖值（如果指定了 appId）
        if (appId != null && !appId.isEmpty()) {
            String appClassifyCode = ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_CONFIG_PREFIX
                    + appId + ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_CONFIG_SUFFIX;
            Map<String, String> appOverrides = loadClassifyItems(
                    ConnectorPlatformConstants.LOOKUP_PATH, appClassifyCode);
            config.putAll(appOverrides); // 应用值覆盖平台值
        }

        return config;
    }

    /**
     * 查询指定 classify 下所有启用的 item_code → item_value。
     *
     * @param path         命名空间
     * @param classifyCode 分类编码
     * @return Map，DB 不可用时返回空 Map
     */
    private Map<String, String> loadClassifyItems(String path, String classifyCode) {
        try {
            List<Map<String, String>> items = lookupWhitelistMapper
                    .selectItemMapByPathAndClassifyCode(path, classifyCode);
            if (items == null || items.isEmpty()) {
                return new HashMap<>();
            }
            Map<String, String> result = new HashMap<>(items.size());
            for (Map<String, String> item : items) {
                String code = item.get("item_code");
                String value = item.get("item_value");
                if (code != null) {
                    result.put(code, value);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to load Lookup items (path={}, classifyCode={}), using defaults",
                    path, classifyCode, e);
            return new HashMap<>();
        }
    }

    // ================================================================
    // 全局配置（无按应用区分）
    // ================================================================

    public int getConnectorMaxVersions() {
        Map<String, String> config = loadConfigBundle(null);
        return getInt(config, ConnectorPlatformConstants.ITEM_CONNECTOR_MAX_VERSIONS,
                ConnectorPlatformConstants.MAX_VERSION_COUNT);
    }

    public String getUrlRegexPattern() {
        Map<String, String> config = loadConfigBundle(null);
        return getString(config, ConnectorPlatformConstants.ITEM_CONNECTOR_URL_REGEX_PATTERN);
    }

    public int getFlowMaxVersions() {
        Map<String, String> config = loadConfigBundle(null);
        return getInt(config, ConnectorPlatformConstants.ITEM_FLOW_MAX_VERSIONS,
                ConnectorPlatformConstants.MAX_VERSION_COUNT);
    }

    // ================================================================
    // 按应用配置
    // ================================================================

    public int getConnectorConfigMaxBytes(String appId) {
        return getInt(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_CONNECTOR_CONFIG_MAX_BYTES, 0);
    }

    public int getMaxExecutionRecordsPerFlow(String appId) {
        return getInt(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_MAX_EXECUTION_RECORDS_PER_FLOW,
                ConnectorPlatformConstants.DEFAULT_EXECUTION_RECORD_LIMIT);
    }

    public int getNodeMaxTimeoutSeconds(String appId) {
        return getInt(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_NODE_MAX_TIMEOUT_SECONDS,
                ConnectorPlatformConstants.DEFAULT_TIMEOUT_SECONDS);
    }

    public int getFlowConfigMaxBytes(String appId) {
        return getInt(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_FLOW_CONFIG_MAX_BYTES, 0);
    }

    public int getFlowMaxQps(String appId) {
        return getInt(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_FLOW_MAX_QPS,
                ConnectorPlatformConstants.DEFAULT_QPS_LIMIT);
    }

    public int getFlowMaxConcurrency(String appId) {
        return getInt(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_FLOW_MAX_CONCURRENCY,
                ConnectorPlatformConstants.DEFAULT_CONCURRENCY_LIMIT);
    }

    public int getFlowMaxCacheTtlSeconds(String appId) {
        return getInt(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_FLOW_MAX_CACHE_TTL_SECONDS,
                ConnectorPlatformConstants.MAX_CACHE_TTL_SECONDS);
    }

    public int getFlowMaxParallelBranches(String appId) {
        return getInt(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_FLOW_MAX_PARALLEL_BRANCHES,
                ConnectorPlatformConstants.MAX_PARALLEL_BRANCHES);
    }

    /** @since v2.0 串行编排连接器节点数量上限 */
    public int getFlowMaxSerialConnectorNodes(String appId) {
        return getInt(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_FLOW_MAX_SERIAL_CONNECTOR_NODES, 3);
    }

    public int getScriptMaxLengthChars(String appId) {
        return getInt(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_SCRIPT_MAX_LENGTH_CHARS,
                ConnectorPlatformConstants.MAX_SCRIPT_SOURCE_LENGTH);
    }

    public int getScriptMaxTimeoutSeconds(String appId) {
        return getInt(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_SCRIPT_MAX_TIMEOUT_SECONDS,
                ConnectorPlatformConstants.MAX_SCRIPT_TIMEOUT_SECONDS);
    }

    public boolean isLogCollectionEnabled(String appId) {
        return getBoolean(loadConfigBundle(appId),
                ConnectorPlatformConstants.ITEM_LOG_COLLECTION_ENABLED, true);
    }

    // ================================================================
    // 私有辅助方法
    // ================================================================

    private String getString(Map<String, String> config, String itemCode) {
        return config.get(itemCode);
    }

    private int getInt(Map<String, String> config, String itemCode, int defaultVal) {
        String value = config.get(itemCode);
        return parseIntOrDefault(value, defaultVal);
    }

    private boolean getBoolean(Map<String, String> config, String itemCode, boolean defaultVal) {
        String value = config.get(itemCode);
        return parseBooleanOrDefault(value, defaultVal);
    }

    private int parseIntOrDefault(String value, int defaultVal) {
        if (value == null || value.isEmpty()) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid int value '{}', using default {}", value, defaultVal);
            return defaultVal;
        }
    }

    private boolean parseBooleanOrDefault(String value, boolean defaultVal) {
        if (value == null || value.isEmpty()) {
            return defaultVal;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
    }
}
