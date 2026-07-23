package com.xxx.it.works.wecode.v2.modules.flow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 连接流缓存清理器 — 统一管理 open-server 侧所有流相关 Redis 缓存清理
 * <p>
 * 对照 {@code spec.md §1.7.3/§1.7.4} 生命周期状态变化,
 * 在 Flow/FlowVersion 状态变更时清理 connector-api 依赖的缓存 Key:
 * </p>
 * <table>
 * <tr><th>Key</th><th>TTL</th><th>清理时机</th></tr>
 * <tr><td>cp:entity:flow:{flowId}</td><td>7d</td><td>start/stop/invalidate/recover/deploy/delete Flow</td></tr>
 * <tr><td>cp:entity:flowversion:{versionId}</td><td>7d</td><td>approve/invalidate/recover/delete FlowVersion</td></tr>
 * <tr><td>cp:flow:config:{flowId}</td><td>120s</td><td>deploy Flow (活跃编排版本变更)</td></tr>
 * <tr><td>cp:cache:flow:{flowId}:*</td><td>可配</td><td>deploy/stop/invalidate/delete Flow (执行结果失效)</td></tr>
 * </table>
 */
@Component
public class FlowCacheEvictor {

    private static final Logger log = LoggerFactory.getLogger(FlowCacheEvictor.class);

    @Autowired(required = false)
    private StringRedisTemplate redis;

    /** 清理 Flow 实体缓存 (lifecycleStatus / deployedVersionId 变更时) */
    public void evictFlowEntity(Long flowId) {
        if (redis == null) return;
        String key = "cp:entity:flow:" + flowId;
        try {
            redis.delete(key);
            log.info("Evicted flow entity cache: {}", key);
        } catch (Exception e) {
            log.warn("Failed to evict flow entity cache {}: {}", key, e.getMessage());
        }
    }

    /** 清理 FlowVersion 实体缓存 (status / orchestrationConfig 变更时) */
    public void evictFlowVersion(Long versionId) {
        if (redis == null) return;
        String key = "cp:entity:flowversion:" + versionId;
        try {
            redis.delete(key);
            log.info("Evicted flow version cache: {}", key);
        } catch (Exception e) {
            log.warn("Failed to evict flow version cache {}: {}", key, e.getMessage());
        }
    }

    /** 清理流配置缓存 (部署切换版本时) */
    public void evictFlowConfig(Long flowId) {
        if (redis == null) return;
        String key = "cp:flow:config:" + flowId;
        try {
            redis.delete(key);
            log.info("Evicted flow config cache: cp:flow:config:{}", flowId);
        } catch (Exception e) {
            log.warn("Failed to evict flow config cache: {}", e.getMessage());
        }
    }

    /** 清理执行结果缓存 (流状态变更导致缓存结果失效时) */
    public void evictExecutionResults(Long flowId) {
        if (redis == null) return;
        String pattern = "cp:cache:flow:" + flowId + ":*";
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build();
            List<String> keysToDelete = new ArrayList<>();
            try (Cursor<String> cursor = redis.scan(options)) {
                while (cursor.hasNext()) {
                    keysToDelete.add(cursor.next());
                }
            }
            if (!keysToDelete.isEmpty()) {
                redis.delete(keysToDelete);
                log.info("Evicted execution result caches: {} keys matching {}", keysToDelete.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Failed to evict execution result caches for flowId={}: {}", flowId, e.getMessage());
        }
    }
}
