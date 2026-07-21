package com.xxx.api.internal.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Dev 环境 SysToken 解析器（桩实现）
 *
 * <p>token 即账号，不做真实校验。生产环境将替换为对接真实 token 管理服务的实现。</p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Component
@Profile("dev")
public class DevSysTokenResolver implements SysTokenResolver {

    @Override
    public String resolveAccount(String token) {
        // 桩：token 即账号
        log.debug("DevSysTokenResolver: token='{}' -> account='{}'", token, token);
        return token;
    }

    @Override
    public boolean isTokenValid(String token) {
        // 桩：非空即有效
        return token != null && !token.isEmpty();
    }
}
