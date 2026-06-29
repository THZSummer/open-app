package com.xxx.it.works.wecode.v2.modules.security;

import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import org.springframework.stereotype.Component;

/**
 * 应用上下文持有者（ThreadLocal 工具类）
 *
 * <p>使用 ThreadLocal 存储当前请求的完整应用上下文（AppContext），
 * 确保同一请求线程内各层可安全获取当前应用信息。</p>
 *
 * <p>生命周期：
 * <ul>
 *   <li>连接器/连接流模块：由 {@link AppDataIsolationAspect} 在切面中调用
 *       {@code appContextResolver.resolveAndValidate()} 设置，方法返回前清除</li>
 *   <li>权限/应用模块：由业务代码自行调用 {@link #setCurrentContext(AppContext)} 设置，
 *       {@link AppWhitelistInterceptor} 的 afterCompletion 清除</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 * @see AppWhitelistInterceptor
 * @see AppDataIsolationAspect
 */
@Component
public final class AppContextHolder {

    /**
     * ThreadLocal 存储当前请求的完整应用上下文
     */
    private static final ThreadLocal<AppContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 私有构造函数，防止实例化
     */
    private AppContextHolder() {
    }

    // ==================== 主要 API（推荐使用） ====================

    /**
     * 设置当前请求的完整应用上下文
     *
     * @param context 应用上下文（含 internalId、externalId、app 实体）
     */
    public static void setCurrentContext(AppContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前请求的完整应用上下文
     *
     * @return 应用上下文，如果未设置则返回 null
     */
    public static AppContext getCurrentContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 获取当前请求的内部应用 ID（数据库主键），如果未设置则抛出异常
     *
     * <p>业务代码推荐使用此方法获取内部 ID 进行数据库操作</p>
     *
     * @return 内部应用 ID（Long）
     * @throws IllegalStateException 如果上下文未初始化
     */
    public static Long requireInternalAppId() {
        AppContext ctx = CONTEXT_HOLDER.get();
        if (ctx == null || ctx.getInternalId() == null) {
            throw new IllegalStateException("App context not initialized - X-App-Id header is required");
        }
        return ctx.getInternalId();
    }

    // ==================== 兼容 API（向后兼容旧调用方） ====================

    /**
     * 设置当前请求的应用 ID（便捷方法）
     *
     * <p>内部创建一个仅含 internalId 的最小 AppContext。
     * 推荐新代码使用 {@link #setCurrentContext(AppContext)}。</p>
     *
     * @param appId 应用 ID
     * @deprecated 推荐使用 {@link #setCurrentContext(AppContext)}
     */
    @Deprecated
    public static void setCurrentAppId(Long appId) {
        CONTEXT_HOLDER.set(AppContext.builder().internalId(appId).build());
    }

    /**
     * 获取当前请求的应用 ID
     *
     * @return 应用 ID，如果未设置则返回 null
     */
    public static Long getCurrentAppId() {
        AppContext ctx = CONTEXT_HOLDER.get();
        return ctx != null ? ctx.getInternalId() : null;
    }

    /**
     * 获取当前请求的应用 ID，如果未设置则抛出异常
     *
     * @return 应用 ID
     * @throws IllegalStateException 如果上下文未初始化
     * @deprecated 推荐使用 {@link #requireInternalAppId()} 获取内部 ID
     */
    @Deprecated
    public static Long requireCurrentAppId() {
        return requireInternalAppId();
    }

    /**
     * 清除当前请求的应用上下文
     * 应在请求结束时调用，防止内存泄漏
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
