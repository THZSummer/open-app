package com.xxx.it.works.wecode.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 缓存总开关（临时方案）
 * &lt;p&gt;
 * 通过 {@code connector.cache.enabled} 控制是否启用 Redis 缓存.
 * {@code false} 时所有查询直接穿透 R2DBC, 与原行为一致.
 * &lt;/p&gt;
 */
@Component
@ConfigurationProperties(prefix = "connector.cache")
public class CacheToggle {

    /** 缓存总开关, 默认开启 */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
