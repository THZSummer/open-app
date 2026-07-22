package com.xxx.api.internal.cache;

import com.xxx.api.internal.entity.AppEntity;
import com.xxx.api.internal.entity.AppMemberEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 内部接口缓存管理器（只读）
 *
 * <p>Cache-Aside 模式：get → miss → 回源 → set。api-server 为只读缓存，
 * 清理由 open-server 侧负责。</p>
 *
 * <p>Key 格式对齐 market-server {@code OPENPLATFORM:{对象}:{字段}:{值}} 风格。</p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalCacheManager {

    private static final String KEY_APP_BY_APPID = "OPENPLATFORM:APP:APPID:";
    private static final String KEY_APP_BY_HISAPPID = "OPENPLATFORM:APP:HISAPPID:";
    private static final String KEY_MEMBER_LIST = "OPENPLATFORM:MEMBER:LIST:";

    private static final Duration TTL_APP = Duration.ofMinutes(30);
    private static final Duration TTL_MEMBER = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    // ──────────────────── AppEntity ────────────────────

    public Optional<AppEntity> getAppByAppId(String appId) {
        return get(KEY_APP_BY_APPID + appId);
    }

    public void setAppByAppId(String appId, AppEntity app) {
        set(KEY_APP_BY_APPID + appId, app, TTL_APP);
    }

    public Optional<AppEntity> getAppByHisAppId(String hisAppId) {
        return get(KEY_APP_BY_HISAPPID + hisAppId);
    }

    public void setAppByHisAppId(String hisAppId, AppEntity app) {
        set(KEY_APP_BY_HISAPPID + hisAppId, app, TTL_APP);
    }

    // ──────────────────── AppMemberEntity ────────────────────

    @SuppressWarnings("unchecked")
    public Optional<List<AppMemberEntity>> getMembers(Long appId) {
        return get(KEY_MEMBER_LIST + appId);
    }

    public void setMembers(Long appId, List<AppMemberEntity> members) {
        set(KEY_MEMBER_LIST + appId, members, TTL_MEMBER);
    }

    // ──────────────────── 内部方法 ────────────────────

    @SuppressWarnings("unchecked")
    private <T> Optional<T> get(String key) {
        try {
            T value = (T) redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Cache miss: {}", key);
                return Optional.empty();
            }
            log.debug("Cache hit: {}", key);
            return Optional.of(value);
        } catch (Exception e) {
            log.error("Cache read error for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cache set: {} (ttl={})", key, ttl);
        } catch (Exception e) {
            log.error("Cache write error for key={}: {}", key, e.getMessage());
        }
    }
}
