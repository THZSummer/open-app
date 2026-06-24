package com.xxx.it.works.wecode.v2.modules.runtime.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 连接流运行时配置 (flowConfig)
 * <p>
 * 从编排配置的 flowConfig 字段解析得到, 控制超时/限流/缓存等运行时行为.
 * 解析失败时使用默认值 (无超时/无限流/无缓存).
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowConfig {

    /** 全流超时时间(毫秒), null 表示无限制 */
    private Integer timeoutMs;

    /** QPS 限流值 (次/秒), null 表示无限流 */
    private Integer rateLimitQps;

    /** 并发限流值, null 表示无限制 */
    private Integer rateLimitConcurrency;

    /** 缓存 TTL (秒), null 表示不缓存 */
    private Integer cacheTtl;

    /** 缓存键模板 (支持表达式, 如 "flow:{flowId}:{body.sender}") */
    private String cacheKeyTemplate;

    /**
     * 创建默认配置 (无超时/无限流/无缓存)
     */
    public static FlowConfig defaults() {
        return new FlowConfig(null, null, null, null, null);
    }
}
