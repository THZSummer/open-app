package com.xxx.it.works.wecode.v2.modules.security;

import com.xxx.it.works.wecode.v2.common.enums.ConnectorPlatformConstants;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookupWhitelistMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 应用白名单服务（v2.0.2）
 *
 * <p>校验应用是否开通了连接器平台能力（白名单准入）。
 * 仅从 Lookup 数据源（openplatform_lookup_item_t, classify_code=Connector.Platform.AppWhitelist）读取白名单。</p>
 *
 * <p>白名单为空或 Lookup 不可用时拒绝所有应用（安全默认）。</p>
 *
 * <p><b>v2.0.2 变更：</b>移除 Spring 属性 cp.app-whitelist 降级逻辑，统一使用 Lookup 数据源。</p>
 * <p><b>v2.0.1 变更：</b>isWhitelisted 主方法接受 String（外部业务 ID）。</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.2
 * @see ConnectorPlatformConstants#LOOKUP_CLASSIFY_APP_WHITELIST
 */
@Slf4j
@Service
public class AppWhitelistService {

    private final LookupWhitelistMapper lookupWhitelistMapper;

    public AppWhitelistService(LookupWhitelistMapper lookupWhitelistMapper) {
        this.lookupWhitelistMapper = lookupWhitelistMapper;
    }

    // ==================== 主 API ====================

    /**
     * 检查指定应用是否在白名单内
     *
     * <p>从 Lookup 数据源查询 classify_code=Connector.Platform.AppWhitelist 的 item_value 列表，
     * 直接以 String 比对。白名单为空或 Lookup 不可用时拒绝所有应用。</p>
     *
     * @param appId 应用外部 ID（String，来自 X-App-Id Header 原值）
     * @return true 表示在白名单内，false 表示不在
     */
    public boolean isWhitelisted(String appId) {
        if (appId == null || appId.trim().isEmpty()) {
            log.warn("AppId is null or empty, whitelist check cannot proceed");
            return false;
        }

        String trimmed = appId.trim();

        List<String> lookupApps = queryLookupWhitelist();
        if (lookupApps == null || lookupApps.isEmpty()) {
            log.warn("Lookup whitelist is empty or unavailable, access denied for app {}", trimmed);
            return false;
        }

        boolean allowed = lookupApps.contains(trimmed);
        if (!allowed) {
            log.warn("App {} is not in the Lookup whitelist, access denied", trimmed);
        }
        return allowed;
    }

    // ==================== 兼容 API ====================

    /**
     * 检查指定应用是否在白名单内（Long 参数兼容方法）
     *
     * @param appId 应用内部 ID（Long）
     * @return true 表示在白名单内，false 表示不在
     * @deprecated 推荐使用 {@link #isWhitelisted(String)}
     */
    @Deprecated
    public boolean isWhitelisted(Long appId) {
        if (appId == null) {
            log.warn("AppId is null, whitelist check cannot proceed");
            return false;
        }
        return isWhitelisted(String.valueOf(appId));
    }

    // ==================== 私有方法 ====================

    private List<String> queryLookupWhitelist() {
        try {
            return lookupWhitelistMapper.selectItemValuesByClassifyCode(
                    ConnectorPlatformConstants.LOOKUP_CLASSIFY_APP_WHITELIST);
        } catch (Exception e) {
            log.warn("Failed to read Lookup whitelist, access denied", e);
            return null;
        }
    }
}
