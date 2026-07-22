package com.xxx.it.works.wecode.v2.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 开放平台缓存清理器 — 统一管理 API 面 (api-server) 依赖的 OPENPLATFORM 命名空间缓存清理
 * <p>
 * 对照 {@code plan.md §13 缓存设计}，在应用信息或成员变更时清理 api-server InternalCacheManager 的缓存 Key：
 * </p>
 * <table>
 * <tr><th>Key</th><th>TTL</th><th>清理时机</th></tr>
 * <tr><td>OPENPLATFORM:APP:APPID:{appId}</td><td>30min</td><td>应用信息变更 (updateApp / updateVerifyType / saveEamapBinding)</td></tr>
 * <tr><td>OPENPLATFORM:APP:HISAPPID:{hisAppId}</td><td>30min</td><td>应用信息变更 (同上，需 eamap_app_code 属性)</td></tr>
 * <tr><td>OPENPLATFORM:MEMBER:LIST:{internalAppId}</td><td>10min</td><td>成员变更 (addMembers / deleteMember / transferOwner)</td></tr>
 * </table>
 * <p>
 * api-server 为只读缓存方（Cache-Aside get/set），open-server 为写操作方负责 delete 清理。
 * </p>
 */
@Component
public class OpenPlatformCacheEvictor {

    private static final Logger log = LoggerFactory.getLogger(OpenPlatformCacheEvictor.class);

    @Autowired(required = false)
    private StringRedisTemplate redis;

    private static final String KEY_APP_BY_APPID    = "OPENPLATFORM:APP:APPID:";
    private static final String KEY_APP_BY_HISAPPID = "OPENPLATFORM:APP:HISAPPID:";
    private static final String KEY_MEMBER_LIST     = "OPENPLATFORM:MEMBER:LIST:";

    /**
     * 应用信息变更时清理 APP 缓存 (appId + hisAppId 两个 Key)
     *
     * @param appId    String 应用标识 (如 "app-xxx123")
     * @param hisAppId EAMAP 编码 (可为 null)
     */
    public void evictApp(String appId, String hisAppId) {
        if (redis == null) return;
        try {
            redis.delete(KEY_APP_BY_APPID + appId);
            log.info("Cache evicted: {} {}", KEY_APP_BY_APPID, appId);
        } catch (Exception e) {
            log.warn("Failed to evict {} {}: {}", KEY_APP_BY_APPID, appId, e.getMessage());
        }
        if (hisAppId != null && !hisAppId.isEmpty()) {
            try {
                redis.delete(KEY_APP_BY_HISAPPID + hisAppId);
                log.info("Cache evicted: {} {}", KEY_APP_BY_HISAPPID, hisAppId);
            } catch (Exception e) {
                log.warn("Failed to evict {} {}: {}", KEY_APP_BY_HISAPPID, hisAppId, e.getMessage());
            }
        }
    }

    /**
     * 成员列表变更时清理 MEMBER:LIST 缓存
     *
     * @param internalAppId 应用内部 Long id (app_t.id)
     */
    public void evictMembers(Long internalAppId) {
        if (redis == null) return;
        try {
            redis.delete(KEY_MEMBER_LIST + internalAppId);
            log.info("Cache evicted: {} {}", KEY_MEMBER_LIST, internalAppId);
        } catch (Exception e) {
            log.warn("Failed to evict {} {}: {}", KEY_MEMBER_LIST, internalAppId, e.getMessage());
        }
    }
}
