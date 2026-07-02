package com.xxx.it.works.wecode.v2.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 连接器平台属性服务（v2.0 Lookup 批量读取 + Redis 缓存）。
 *
 * <p>connector-api 运行态专用。按 §3.3.4 运行态模型：
 * 只读 ①（平台全局值），不读 ②（应用级覆盖）。
 * 运行态实际生效值 = ③(编排快照)存在 ? ③ : ①，本 Service 仅提供 ① 的读取。</p>
 *
 * <p>每个属性都有硬编码的兜底默认值；读取 DB 失败时不会抛出异常或返回 null。</p>
 *
 * @author SDDU Build Agent
 * @version 2.1.0
 */
@Service
public class ConnectorApiPropertyService {

    private static final Logger log = LoggerFactory.getLogger(ConnectorApiPropertyService.class);

    private static final String PATH = "CEC.Open";
    private static final String CLASSIFY_PLATFORM_CONFIG = "Connector.Platform.Config";

    private final EntityCacheManager entityCacheManager;

    public ConnectorApiPropertyService(EntityCacheManager entityCacheManager) {
        this.entityCacheManager = entityCacheManager;
    }

    // ================================================================
    // 批量读取（只读①平台全局值，不读②应用级覆盖）
    // ================================================================

    /**
     * 加载平台全局配置 Map（①）。
     * 运行态专用，不读应用级覆盖 ②。
     *
     * @return item_code → item_value 的平台默认 Map
     */
    public Mono<Map<String, String>> loadPlatformDefaults() {
        return entityCacheManager.getLookupConfig(CLASSIFY_PLATFORM_CONFIG);
    }

    // ================================================================
    // 单项读取（只读①，运行态回退用）
    // ================================================================

    /** 节点超时上限①（秒），默认 5 */
    public Mono<Integer> getNodeMaxTimeoutSeconds() {
        return loadPlatformDefaults()
                .map(config -> getInt(config, "Node.Max.Timeout.Seconds", 5));
    }

    /** 最大 QPS①，默认 1000 */
    public Mono<Integer> getFlowMaxQps() {
        return loadPlatformDefaults()
                .map(config -> getInt(config, "Flow.Max.Qps", 1000));
    }

    /** 最大并发①，默认 1000 */
    public Mono<Integer> getFlowMaxConcurrency() {
        return loadPlatformDefaults()
                .map(config -> getInt(config, "Flow.Max.Concurrency", 1000));
    }

    /** 缓存 TTL 上限①（秒），默认 1296000 */
    public Mono<Integer> getFlowMaxCacheTtlSeconds() {
        return loadPlatformDefaults()
                .map(config -> getInt(config, "Flow.Max.Cache.Ttl.Seconds", 1296000));
    }

    /** 脚本超时上限①（秒），默认 5 */
    public Mono<Integer> getScriptMaxTimeoutSeconds() {
        return loadPlatformDefaults()
                .map(config -> getInt(config, "Script.Max.Timeout.Seconds", 5));
    }

    /** 日志采集开关①，默认 true */
    public Mono<Boolean> isLogCollectionEnabled() {
        return loadPlatformDefaults()
                .map(config -> getBoolean(config, "Log.Collection.Enabled", true));
    }

    // ================================================================
    // 私有辅助
    // ================================================================

    private int getInt(Map<String, String> config, String key, int defaultVal) {
        String value = config.get(key);
        if (value == null || value.isEmpty()) return defaultVal;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid int value '{}' for key '{}', using default {}", value, key, defaultVal);
            return defaultVal;
        }
    }

    private boolean getBoolean(Map<String, String> config, String key, boolean defaultVal) {
        String value = config.get(key);
        if (value == null || value.isEmpty()) return defaultVal;
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
    }
}
