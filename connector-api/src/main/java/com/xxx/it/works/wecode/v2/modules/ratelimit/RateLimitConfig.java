package com.xxx.it.works.wecode.v2.modules.ratelimit;

/**
 * 限流配置模型
 * <p>
 * 描述单个连接流的限流参数。
 * </p>
 */
public class RateLimitConfig {

    /** 限流模式: "qps" | "concurrency" */
    private String mode;

    /** QPS 上限 */
    private int maxQps;

    /** 并发上限 */
    private int maxConcurrency;

    public RateLimitConfig() {
    }

    public RateLimitConfig(String mode, int maxQps, int maxConcurrency) {
        this.mode = mode;
        this.maxQps = maxQps;
        this.maxConcurrency = maxConcurrency;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getMaxQps() {
        return maxQps;
    }

    public void setMaxQps(int maxQps) {
        this.maxQps = maxQps;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    public String toString() {
        return "RateLimitConfig{mode='" + mode + "', maxQps=" + maxQps + ", maxConcurrency=" + maxConcurrency + "}";
    }
}
