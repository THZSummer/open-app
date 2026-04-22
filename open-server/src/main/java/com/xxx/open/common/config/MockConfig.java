package com.xxx.open.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mock 配置
 * 
 * <p>用于控制 Mock 策略开关，实现一键切换 Mock/真实接口</p>
 * 
 * <p>使用方式：</p>
 * <ul>
 *   <li>application-dev.yml: mock.enabled=true（开发环境使用 Mock）</li>
 *   <li>application-prod.yml: mock.enabled=false（生产环境使用真实接口）</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mock")
public class MockConfig {

    /**
     * 是否启用 Mock（默认 false）
     */
    private boolean enabled = false;
}
