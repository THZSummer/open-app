package com.xxx.it.works.wecode.v2.modules.security;

/**
 * SysToken 解析器 (出站方向)
 * <p>
 * 获取 open-server 内部使用的 SysToken，用于调用下游服务（如 connector-api 调试接口）
 * 时作为调用方凭证放入 {@code X-Sys-Token} 请求头。
 * </p>
 * <p>
 * 与 connector-api 的 {@code SysTokenResolver}（入站校验方向: 校验收到的 token）互补。
 * 开发环境使用 {@code DevSysTokenResolver}（默认实现），
 * 标准环境使用 {@code StandardSysTokenResolver}（预留实现, 对接 token 管理服务）。
 * </p>
 *
 * @author SDDU Build Agent
 */
public interface SysTokenResolver {

    /**
     * 获取 open-server 内部使用的 SysToken
     *
     * @return SysToken 字符串
     */
    String obtainSysToken();
}
