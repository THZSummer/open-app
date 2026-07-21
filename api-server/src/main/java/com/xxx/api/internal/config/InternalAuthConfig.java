package com.xxx.api.internal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内部凭证配置映射
 *
 * <p>从 application.yml 读取 internal.auth 配置：</p>
 * <ul>
 *   <li><b>tokens</b> — 有效凭证列表（支持多服务方独立配置）</li>
 *   <li><b>bypass</b> — 开发阶段绕过开关（true=跳过凭证校验）</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "internal.auth")
public class InternalAuthConfig {

    /**
     * 有效内部凭证列表
     */
    private List<String> tokens;

    /**
     * 开发阶段是否绕过凭证校验
     */
    private boolean bypass = false;
}
