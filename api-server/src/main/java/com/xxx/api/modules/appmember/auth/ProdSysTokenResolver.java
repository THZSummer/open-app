package com.xxx.api.modules.appmember.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 标准环境 SysToken 解析器（预留）
 *
 * <p>需对接真实 SysToken 管理服务完成 token 解析和有效性校验。</p>
 *
 * <p>TODO: 对接真实 token 管理服务
 * <ul>
 *   <li>{@link #resolveAccount(String)}：调用 token 服务解析 SysAccount</li>
 *   <li>{@link #isTokenValid(String)}：调用 token 服务校验有效期和状态</li>
 * </ul></p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Component
@Profile("!dev")
public class ProdSysTokenResolver implements SysTokenResolver {

    @Override
    public String resolveAccount(String token) {
        // TODO: 对接真实 SysToken 解析服务，根据 token 查询调用方账号
        log.debug("ProdSysTokenResolver: resolving account from token (stub)");
        return token;
    }

    @Override
    public boolean isTokenValid(String token) {
        // TODO: 对接真实 token 校验服务，检查 token 是否在有效期内且未被吊销
        log.debug("ProdSysTokenResolver: validating token (stub)");
        return token != null && !token.isEmpty();
    }
}
