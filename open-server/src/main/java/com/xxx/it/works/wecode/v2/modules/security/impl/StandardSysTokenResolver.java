package com.xxx.it.works.wecode.v2.modules.security.impl;

import com.xxx.it.works.wecode.v2.modules.security.SysTokenResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 标准环境 SysToken 解析器 (预留实现)
 * <p>
 * 通过配置 {@code sys-token.resolver.type=standard} 激活。
 * TODO: 标准环境对接 token 管理服务获取 SysToken
 * （参考 connector-api SystokenCredentialSupplier.getToken() 的预留语义）。
 * </p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sys-token.resolver.type", havingValue = "standard")
public class StandardSysTokenResolver implements SysTokenResolver {

    @Override
    public String obtainSysToken() {
        throw new UnsupportedOperationException("Standard SysToken resolver not implemented yet");
    }
}
