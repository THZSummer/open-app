package com.xxx.it.works.wecode.v2.common.config;

import com.xxx.it.works.wecode.v2.common.enums.ConnectorPlatformConstants;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 连接器平台属性配置服务
 *
 * <p>从 openplatform_property_t 数据字典表读取平台配置项，
 * 每项均提供硬编码兜底默认值；DB 不可用或配置缺失时绝不抛异常或返回 null。</p>
 *
 * <p>查找规则：
 * <ul>
 *   <li>全局配置：path = {@code connector_platform}，code = 配置键</li>
 *   <li>按应用配置：优先 path = {@code connector_platform_app_{appId}}，
 *       找不到则回退到 path = {@code connector_platform}，
 *       仍找不到则使用硬编码默认值</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @date 2026-06-26
 */
@Slf4j
@Service
public class ConnectorPlatformPropertyService {

    private static final String PATH_CONNECTOR_PLATFORM = "connector_platform";
    private static final String PATH_CONNECTOR_PLATFORM_APP_PREFIX = "connector_platform_app_";

    private final AppMapper appMapper;

    public ConnectorPlatformPropertyService(AppMapper appMapper) {
        this.appMapper = appMapper;
    }

    // ================================================================
    // 全局配置（无按应用区分）
    // ================================================================

    /**
     * 连接器最大版本数
     *
     * @see ConnectorPlatformConstants#MAX_VERSION_COUNT
     */
    public int getConnectorMaxVersions() {
        String value = queryProperty(PATH_CONNECTOR_PLATFORM, "connector_max_versions");
        return parseIntOrDefault(value, ConnectorPlatformConstants.MAX_VERSION_COUNT);
    }

    /**
     * URL 正则校验表达式
     *
     * @return 配置的正则字符串；{@code null} 表示不启用正则校验
     */
    public String getUrlRegexPattern() {
        return queryProperty(PATH_CONNECTOR_PLATFORM, "connector_url_regex_pattern");
    }

    /**
     * 连接流最大版本数
     *
     * @see ConnectorPlatformConstants#MAX_VERSION_COUNT
     */
    public int getFlowMaxVersions() {
        String value = queryProperty(PATH_CONNECTOR_PLATFORM, "flow_max_versions");
        return parseIntOrDefault(value, ConnectorPlatformConstants.MAX_VERSION_COUNT);
    }

    // ================================================================
    // 按应用配置
    // ================================================================

    /**
     * 连接器配置最大字节数（0 表示不限制）
     */
    public int getConnectorConfigMaxBytes(String appId) {
        return getPerAppInt(appId, "connector_config_max_bytes", 0);
    }

    /**
     * 每连接流最大运行记录数
     *
     * @see ConnectorPlatformConstants#DEFAULT_EXECUTION_RECORD_LIMIT
     */
    public int getMaxExecutionRecordsPerFlow(String appId) {
        return getPerAppInt(appId, "max_execution_records_per_flow",
                ConnectorPlatformConstants.DEFAULT_EXECUTION_RECORD_LIMIT);
    }

    /**
     * 节点最大超时时间（秒）
     *
     * @see ConnectorPlatformConstants#DEFAULT_TIMEOUT_SECONDS
     */
    public int getNodeMaxTimeoutSeconds(String appId) {
        return getPerAppInt(appId, "node_max_timeout_seconds",
                ConnectorPlatformConstants.DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 连接流配置最大字节数（0 表示不限制）
     */
    public int getFlowConfigMaxBytes(String appId) {
        return getPerAppInt(appId, "flow_config_max_bytes", 0);
    }

    /**
     * 连接流最大 QPS
     *
     * @see ConnectorPlatformConstants#DEFAULT_QPS_LIMIT
     */
    public int getFlowMaxQps(String appId) {
        return getPerAppInt(appId, "flow_max_qps",
                ConnectorPlatformConstants.DEFAULT_QPS_LIMIT);
    }

    /**
     * 连接流最大并发数
     *
     * @see ConnectorPlatformConstants#DEFAULT_CONCURRENCY_LIMIT
     */
    public int getFlowMaxConcurrency(String appId) {
        return getPerAppInt(appId, "flow_max_concurrency",
                ConnectorPlatformConstants.DEFAULT_CONCURRENCY_LIMIT);
    }

    /**
     * 连接流缓存最大 TTL（秒）
     *
     * @see ConnectorPlatformConstants#MAX_CACHE_TTL_SECONDS
     */
    public int getFlowMaxCacheTtlSeconds(String appId) {
        return getPerAppInt(appId, "flow_max_cache_ttl_seconds",
                ConnectorPlatformConstants.MAX_CACHE_TTL_SECONDS);
    }

    /**
     * 连接流最大并行分支数
     *
     * @see ConnectorPlatformConstants#MAX_PARALLEL_BRANCHES
     */
    public int getFlowMaxParallelBranches(String appId) {
        return getPerAppInt(appId, "flow_max_parallel_branches",
                ConnectorPlatformConstants.MAX_PARALLEL_BRANCHES);
    }

    /**
     * 脚本源码最大字符数
     *
     * @see ConnectorPlatformConstants#MAX_SCRIPT_SOURCE_LENGTH
     */
    public int getScriptMaxLengthChars(String appId) {
        return getPerAppInt(appId, "script_max_length_chars",
                ConnectorPlatformConstants.MAX_SCRIPT_SOURCE_LENGTH);
    }

    /**
     * 脚本最大超时时间（秒）
     *
     * @see ConnectorPlatformConstants#MAX_SCRIPT_TIMEOUT_SECONDS
     */
    public int getScriptMaxTimeoutSeconds(String appId) {
        return getPerAppInt(appId, "script_max_timeout_seconds",
                ConnectorPlatformConstants.MAX_SCRIPT_TIMEOUT_SECONDS);
    }

    /**
     * 是否开启运行日志采集
     *
     * @return {@code true} 开启（默认）；{@code false} 关闭
     */
    public boolean isLogCollectionEnabled(String appId) {
        return getPerAppBoolean(appId, "log_collection_enabled", true);
    }

    // ================================================================
    // 私有辅助方法
    // ================================================================

    /**
     * 查询数据字典值，异常时返回 null（绝不抛异常）
     */
    private String queryProperty(String path, String code) {
        try {
            return appMapper.selectDictionaryValue(path, code);
        } catch (Exception e) {
            log.warn("Failed to read Property (path={}, code={}), using default", path, code, e);
            return null;
        }
    }

    /**
     * 按应用优先，全局兜底，解析为 int
     */
    private int getPerAppInt(String appId, String code, int defaultVal) {
        String value = queryProperty(PATH_CONNECTOR_PLATFORM_APP_PREFIX + appId, code);
        if (value == null) {
            value = queryProperty(PATH_CONNECTOR_PLATFORM, code);
        }
        return parseIntOrDefault(value, defaultVal);
    }

    /**
     * 按应用优先，全局兜底，解析为 boolean
     */
    private boolean getPerAppBoolean(String appId, String code, boolean defaultVal) {
        String value = queryProperty(PATH_CONNECTOR_PLATFORM_APP_PREFIX + appId, code);
        if (value == null) {
            value = queryProperty(PATH_CONNECTOR_PLATFORM, code);
        }
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
        return Boolean.parseBoolean(value.trim());
    }
}
