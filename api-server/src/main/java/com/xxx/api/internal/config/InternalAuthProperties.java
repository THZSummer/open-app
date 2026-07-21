package com.xxx.api.internal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 内部接口认证配置属性
 *
 * <p>绑定 {@code internal.auth} 前缀的 yml 配置：</p>
 * <ul>
 *   <li>{@code bypass}：开发阶段是否跳过凭证校验</li>
 *   <li>{@code allowed-accounts}：允许调用内部接口的账号白名单</li>
 * </ul>
 *
 * @author SDDU Build Agent
 */
@Data
@Component
@ConfigurationProperties(prefix = "internal.auth")
public class InternalAuthProperties {

    /** 是否跳过凭证校验（开发阶段可设为 true） */
    private boolean bypass = false;

    /** 允许调用内部接口的账号白名单 */
    private List<String> allowedAccounts = new ArrayList<>();
}
