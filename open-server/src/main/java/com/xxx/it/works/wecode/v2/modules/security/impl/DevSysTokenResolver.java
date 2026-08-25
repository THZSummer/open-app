package com.xxx.it.works.wecode.v2.modules.security.impl;

import com.xxx.it.works.wecode.v2.modules.security.SysTokenResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 开发环境 SysToken 解析器 (默认实现)
 * <p>
 * 返回配置的默认 token，用于开发/测试环境联调。
 * 可通过 {@code internal.sys-token} 配置覆盖，默认 {@code dev-sys-token}。
 * </p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Component
public class DevSysTokenResolver implements SysTokenResolver {

    @Value("${internal.sys-token:dev-sys-token}")
    private String devToken;

    @Override
    public String obtainSysToken() {
        return devToken;
    }
}
