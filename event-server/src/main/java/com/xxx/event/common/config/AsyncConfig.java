package com.xxx.event.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步配置
 * 
 * <p>启用异步支持，用于 WebHook 异步发送</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
