package com.xxx.it.works.wecode.v2.modules.security;

import org.springframework.stereotype.Component;

/**
 * 应用上下文持有者（ThreadLocal 工具类）
 *
 * <p>使用 ThreadLocal 存储当前请求的应用 ID（来源 X-App-Id Header），
 * 确保同一请求线程内各层可安全获取当前应用上下文。</p>
 *
 * <p>生命周期由 AppWhitelistInterceptor 管理：
 * preHandle 阶段设置，afterCompletion 阶段清除</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see AppWhitelistInterceptor
 */
@Component
public final class AppContextHolder {

    /**
     * ThreadLocal 存储当前应用 ID
     */
    private static final ThreadLocal<Long> APP_ID_HOLDER = new ThreadLocal<>();

    /**
     * 私有构造函数，防止实例化
     */
    private AppContextHolder() {
    }

    /**
     * 设置当前请求的应用 ID
     *
     * @param appId 应用 ID
     */
    public static void setCurrentAppId(Long appId) {
        APP_ID_HOLDER.set(appId);
    }

    /**
     * 获取当前请求的应用 ID
     *
     * @return 应用 ID，如果未设置则返回 null
     */
    public static Long getCurrentAppId() {
        return APP_ID_HOLDER.get();
    }

    /**
     * 获取当前请求的应用 ID，如果未设置则抛出异常
     *
     * @return 应用 ID
     * @throws IllegalStateException 如果未设置应用 ID
     */
    public static Long requireCurrentAppId() {
        Long appId = APP_ID_HOLDER.get();
        if (appId == null) {
            throw new IllegalStateException("App context not initialized - X-App-Id header is required");
        }
        return appId;
    }

    /**
     * 清除当前请求的应用上下文
     * 应在请求结束时调用，防止内存泄漏
     */
    public static void clear() {
        APP_ID_HOLDER.remove();
    }
}
