package com.xxx.api.internal.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.api.internal.entity.AppEntity;
import com.xxx.api.internal.entity.AppMemberEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
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
public class InternalCacheManager {

    private static final String KEY_APP_BY_APPID = "OPENPLATFORM:APP:APPID:";
    private static final String KEY_APP_BY_HISAPPID = "OPENPLATFORM:APP:HISAPPID:";
    private static final String KEY_MEMBER_LIST = "OPENPLATFORM:MEMBER:LIST:";

    private static final Duration TTL_APP = Duration.ofMinutes(30);
    private static final Duration TTL_MEMBER = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean cacheAvailable;

    public InternalCacheManager(Optional<RedisTemplate<String, Object>> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate.orElse(null);
        this.objectMapper = objectMapper;
        this.cacheAvailable = this.redisTemplate != null;
    }

    // ──────────────────── AppEntity ────────────────────

    public Optional<AppEntity> getAppByAppId(String appId) {
        return get(KEY_APP_BY_APPID + appId, AppEntity.class);
    }

    public void setAppByAppId(String appId, AppEntity app) {
        set(KEY_APP_BY_APPID + appId, app, TTL_APP);
    }

    public Optional<AppEntity> getAppByHisAppId(String hisAppId) {
        return get(KEY_APP_BY_HISAPPID + hisAppId, AppEntity.class);
    }

    public void setAppByHisAppId(String hisAppId, AppEntity app) {
        set(KEY_APP_BY_HISAPPID + hisAppId, app, TTL_APP);
    }

    // ──────────────────── AppMemberEntity ────────────────────

    @SuppressWarnings("unchecked")
    public Optional<List<AppMemberEntity>> getMembers(Long appId) {
        Optional<List> raw = get(KEY_MEMBER_LIST + appId, List.class);
        return raw.map(list -> (List<AppMemberEntity>) list);
    }

    public void setMembers(Long appId, List<AppMemberEntity> members) {
        set(KEY_MEMBER_LIST + appId, members, TTL_MEMBER);
    }

    // ──────────────────── 内部方法 ────────────────────

    @SuppressWarnings("unchecked")
    private <T> Optional<T> get(String key, Class<T> type) {
        if (!cacheAvailable) {
            return Optional.empty();
        }
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Cache miss: {}", key);
                return Optional.empty();
            }
            log.debug("Cache hit: {}", key);

            // RedisTemplate 配置了 GenericJackson2JsonRedisSerializer，
            // 简单类型直接返回，复杂对象需要反序列化
            if (type.isInstance(value)) {
                return Optional.of((T) value);
            }

            // Jackson LinkedHashMap → target type fallback
            String json = objectMapper.writeValueAsString(value);
            T result = objectMapper.readValue(json, type);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.warn("Cache read error for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private void set(String key, Object value, Duration ttl) {
        if (!cacheAvailable) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cache set: {} (ttl={})", key, ttl);
        } catch (Exception e) {
            log.warn("Cache write error for key={}: {}", key, e.getMessage());
        }
    }
}
