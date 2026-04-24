package com.xxx.open.common.context;

import com.xxx.open.common.model.UserContext;

/**
 * 用户上下文持有者(ThreadLocal 工具类)
 * 
 * <p>使用 ThreadLocal 存储当前请求的用户上下文，确保线程安全</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public final class UserContextHolder {

    /**
     * ThreadLocal 存储用户上下文
     */
    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 私有构造函数，防止实例化
     */
    private UserContextHolder() {
    }

    /**
     * 设置用户上下文
     * 
     * @param context 用户上下文
     */
    public static void set(UserContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取用户上下文
     * 
     * @return 用户上下文，如果未设置则返回默认系统用户
     */
    public static UserContext get() {
        UserContext context = CONTEXT_HOLDER.get();
        return context != null ? context : UserContext.empty();
    }

    /**
     * 获取当前用户ID
     * 
     * @return 用户ID
     */
    public static String getUserId() {
        return get().getUserId();
    }

    /**
     * 获取当前用户名称
     * 如果用户名称为空，则返回用户ID
     * 
     * @return 用户名称
     */
    public static String getUserName() {
        UserContext context = get();
        return context.getUserName() != null ? context.getUserName() : context.getUserId();
    }

    /**
     * 清除用户上下文
     * 应在请求结束时调用，防止内存泄漏
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}