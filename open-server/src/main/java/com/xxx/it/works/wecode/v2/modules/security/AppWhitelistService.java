package com.xxx.it.works.wecode.v2.modules.security;

import com.xxx.it.works.wecode.v2.common.enums.ConnectorPlatformConstants;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookupWhitelistMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 应用白名单服务
 *
 * <p>校验应用是否开通了连接器平台能力（白名单准入）。</p>
 *
 * <p>查询优先级：
 * <ol>
 *   <li>Lookup 数据源（openplatform_lookup_classify_t / openplatform_lookup_item_t）
 *       — classify_code=app_whitelist</li>
 *   <li>Spring 属性 cp.app-whitelist（逗号分隔的应用 ID，降级回退）</li>
 * </ol>
 *
 * <p>白名单为空时拒绝所有应用（安全默认），不再放行。</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 * @see ConnectorPlatformConstants#APP_WHITELIST_CLASSIFY_CODE
 */
@Slf4j
@Service
public class AppWhitelistService {

    /**
     * 应用白名单配置（逗号分隔的应用 ID 列表）
     * Lookup 不可用时降级回退到此属性。
     */
    @Value("${cp.app-whitelist:}")
    private String whitelistConfig;

    /**
     * Lookup 白名单 Mapper（查询 openplatform_lookup_item_t）
     */
    private final LookupWhitelistMapper lookupWhitelistMapper;

    public AppWhitelistService(LookupWhitelistMapper lookupWhitelistMapper) {
        this.lookupWhitelistMapper = lookupWhitelistMapper;
    }

    /**
     * 检查指定应用是否在白名单内
     *
     * <p>查询顺序：
     * <ol>
     *   <li>调用 Lookup 查询 classify_code=app_whitelist 的 item_value 列表</li>
     *   <li>Lookup 不可用或返回 null 时降级为 Spring 属性 cp.app-whitelist</li>
     *   <li>白名单为空 → 拒绝所有应用（返回 false）</li>
     * </ol>
     *
     * @param appId 应用 ID
     * @return true 表示在白名单内，false 表示不在
     */
    public boolean isWhitelisted(Long appId) {
        if (appId == null) {
            log.warn("AppId is null, whitelist check cannot proceed");
            return false;
        }

        String appIdStr = String.valueOf(appId);

        // 1. Try Lookup (openplatform_lookup_classify_t + openplatform_lookup_item_t)
        List<String> lookupApps = queryLookupWhitelist();
        if (lookupApps != null && !lookupApps.isEmpty()) {
            boolean allowed = lookupApps.contains(appIdStr);
            if (!allowed) {
                log.warn("App {} is not in the Lookup whitelist, access denied", appId);
            }
            return allowed;
        }

        // 2. Fallback to Spring property
        return isWhitelistedByProperty(appId);
    }

    /**
     * 从 Lookup 数据源查询白名单应用列表
     *
     * @return classify_code=app_whitelist 的 item_value 列表；异常时返回 null 触发降级
     */
    private List<String> queryLookupWhitelist() {
        try {
            return lookupWhitelistMapper.selectItemValuesByClassifyCode("app_whitelist");
        } catch (Exception e) {
            log.warn("Failed to read Lookup whitelist, falling back to property", e);
            return null;
        }
    }

    /**
     * 基于 Spring 属性的白名单检查（降级回退）
     *
     * <p>白名单为空时拒绝所有应用（安全默认）。</p>
     *
     * @param appId 应用 ID
     * @return true 表示在白名单内，false 表示不在
     */
    private boolean isWhitelistedByProperty(Long appId) {
        Set<Long> whitelist = parseWhitelist();

        // 白名单为空 → 拒绝所有应用（安全默认）
        if (whitelist.isEmpty()) {
            log.warn("Whitelist is empty (both Lookup and property), access denied for app {}", appId);
            return false;
        }

        boolean allowed = whitelist.contains(appId);
        if (!allowed) {
            log.warn("App {} is not in the whitelist, access denied", appId);
        }
        return allowed;
    }

    /**
     * 解析白名单配置字符串为 Long 集合
     *
     * @return 白名单应用 ID 集合（不可变）
     */
    private Set<Long> parseWhitelist() {
        if (whitelistConfig == null || whitelistConfig.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> whitelist = new HashSet<>();
        for (String part : whitelistConfig.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    whitelist.add(Long.parseLong(trimmed));
                } catch (NumberFormatException e) {
                    log.warn("Invalid app ID in whitelist config: '{}', skipping", trimmed);
                }
            }
        }
        return Collections.unmodifiableSet(whitelist);
    }

    /**
     * 市场服务降级检查（生产环境使用）
     *
     * <p>TODO: 当 market-server 集成就绪后，调用 Lookup API 替代当前的属性配置方式。
     * market-server 不可用时降级放行（记录告警日志）。</p>
     *
     * @param appId 应用 ID
     * @return true 表示在白名单内或 market-server 不可用（降级放行）
     */
    public boolean isWhitelistedWithMarketFallback(Long appId) {
        try {
            // TODO: 调用 market-server Lookup API
            // MarketLookupResponse response = marketClient.lookup(appId, APP_WHITELIST_CLASSIFY_CODE);
            // return response.isFound();
            log.warn("Market server integration not yet implemented, falling back to property-based whitelist");
            return isWhitelisted(appId);
        } catch (Exception e) {
            log.warn("Market server unavailable, whitelist check degraded to pass for appId={}", appId, e);
            return true;
        }
    }
}
