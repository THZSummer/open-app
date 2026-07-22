package com.xxx.api.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.api.modules.app.entity.AppEntity;
import com.xxx.api.modules.appmember.entity.AppMemberEntity;
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
 * <p>RedisTemplate&lt;String,String&gt; 存储 JSON 字符串，
 * 通过 ObjectMapper 手动序列化/反序列化，避免序列化器耦合。</p>
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

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public InternalCacheManager(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
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

    public Optional<List<AppMemberEntity>> getMembers(Long appId) {
        return getList(KEY_MEMBER_LIST + appId);
    }

    public void setMembers(Long appId, List<AppMemberEntity> members) {
        set(KEY_MEMBER_LIST + appId, members, TTL_MEMBER);
    }

    // ──────────────────── 内部方法 ────────────────────

    private <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("Cache miss: {}", key);
                return Optional.empty();
            }
            T value = objectMapper.readValue(json, clazz);
            log.debug("Cache hit: {}", key);
            return Optional.of(value);
        } catch (JsonProcessingException e) {
            log.warn("Cache deserialize error for key={}: {}", key, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Cache read error for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<List<AppMemberEntity>> getList(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("Cache miss: {}", key);
                return Optional.empty();
            }
            List<AppMemberEntity> value = objectMapper.readValue(json,
                    new TypeReference<List<AppMemberEntity>>() {});
            log.debug("Cache hit: {}", key);
            return Optional.of(value);
        } catch (JsonProcessingException e) {
            log.warn("Cache deserialize error for key={}: {}", key, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Cache read error for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private void set(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Cache set: {} (ttl={})", key, ttl);
        } catch (Exception e) {
            log.error("Cache write error for key={}: {}", key, e.getMessage());
        }
    }
}
