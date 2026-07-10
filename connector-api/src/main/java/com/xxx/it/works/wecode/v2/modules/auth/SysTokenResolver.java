package com.xxx.it.works.wecode.v2.modules.auth;

import com.xxx.it.works.wecode.v2.common.annotation.StandardTodo;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * SysToken 解析器 (入站方向)
 * <p>
 * 将请求中携带的 SysToken 解析为系统账号 (SysAccount)，
 * 用于触发鉴权白名单校验和执行记录归档。
 * </p>
 * <p>
 * 与 {@code SystokenCredentialSupplier} (出站方向: 获取 sysToken 供下游调用) 互补，
 * 后续对接同一个 token 管理服务时只需修改本类。
 * </p>
 *
 * @author SDDU Build Agent
 */
@Component
public class SysTokenResolver {

    /**
     * 校验 SysToken 是否有效
     *
     * @param token 请求中携带的 SysToken
     * @return true 表示 token 格式合法且可解析, false 表示 token 无效
     */
    @StandardTodo("对接 token 解析服务，校验 SysToken 有效性")
    public boolean isTokenValid(String token) {
        return token != null && !token.isEmpty();
    }

    /**
     * 根据 SysToken 解析出 SysAccount
     *
     * @param token 请求中携带的 SysToken (应先通过 {@link #isTokenValid} 校验)
     * @return 解析出的系统账号, 解析失败时返回 empty
     */
    @StandardTodo("对接 token 解析服务，根据 SysToken 解析出 SysAccount")
    public Optional<String> resolveSysAccount(String token) {
        return Optional.ofNullable(token).filter(t -> !t.isEmpty());
    }
}
