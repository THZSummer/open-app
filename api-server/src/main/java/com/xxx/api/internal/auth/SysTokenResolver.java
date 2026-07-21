package com.xxx.api.internal.auth;

/**
 * SysToken 解析器（入站方向）
 *
 * <p>从请求头 {@code X-Internal-Token}（即 X-Sys-Token）解析调用方账号并校验有效性。
 * 不同环境通过 Spring {@code @Profile} 注入不同实现。</p>
 *
 * <p>参考：connector-api 中 {@code com.xxx.it.works.wecode.v2.modules.auth.SysTokenResolver}</p>
 *
 * @author SDDU Build Agent
 */
public interface SysTokenResolver {

    /**
     * 从 token 解析调用方账号
     *
     * @param token 请求头中的 X-Internal-Token 值
     * @return 调用方账号，解析失败返回 null
     */
    String resolveAccount(String token);

    /**
     * 校验 token 是否有效
     *
     * @param token 请求头中的 X-Internal-Token 值
     * @return true=有效，false=已失效
     */
    boolean isTokenValid(String token);
}
