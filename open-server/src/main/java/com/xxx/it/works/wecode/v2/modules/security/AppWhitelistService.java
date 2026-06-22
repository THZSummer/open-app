package com.xxx.it.works.wecode.v2.modules.security;

import com.xxx.it.works.wecode.v2.common.enums.ConnectorPlatformConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 应用白名单服务
 *
 * <p>校验应用是否开通了连接器平台能力（白名单准入）。</p>
 *
 * <p>MVP 实现：基于配置属性 cp.app-whitelist（逗号分隔的应用 ID 列表）。
 * 生产环境应改为调用 market-server Lookup API 查询 classify_code=cp_app_whitelist 的白名单。
 * 空列表表示所有应用均在白名单内（开发/测试便利模式）。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see ConnectorPlatformConstants#APP_WHITELIST_CLASSIFY_CODE
 */
@Slf4j
@Service
public class AppWhitelistService {

    /**
     * 应用白名单配置（逗号分隔的应用 ID 列表）
     * 为空时表示所有应用均在白名单内（开发/测试模式）
     */
    @Value("${cp.app-whitelist:}")
    private String whitelistConfig;

    /**
     * 检查指定应用是否在白名单内
     *
     * <p>MVP 实现逻辑：
     * <ul>
     *   <li>白名单为空 → 所有应用放行（返回 true）</li>
     *   <li>白名单非空 → 检查 appId 是否在列表中</li>
     * </ul>
     *
     * <p>生产环境应改为：调用 market-server Lookup API，
     * 查询 classify_code=cp_app_whitelist 下是否包含该 appId。
     * market-server 不可用时降级放行（返回 true）。</p>
     *
     * @param appId 应用 ID
     * @return true 表示在白名单内，false 表示不在
     */
    public boolean isWhitelisted(Long appId) {
        if (appId == null) {
            log.warn("AppId is null, whitelist check cannot proceed");
            return false;
        }

        Set<Long> whitelist = parseWhitelist();

        // 空白名单 → 所有应用放行（开发/测试便利模式）
        if (whitelist.isEmpty()) {
            log.debug("Whitelist is empty, all apps are allowed (dev/test mode)");
            return true;
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
