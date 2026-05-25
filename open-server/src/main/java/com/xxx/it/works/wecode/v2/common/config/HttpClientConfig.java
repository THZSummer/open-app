package com.xxx.it.works.wecode.v2.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端配置
 * <p>
 * 用于 open-server 内部 HTTP 调用 (如转发至 connector-api)
 * </p>
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}