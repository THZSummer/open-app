package com.xxx.it.works.wecode.v2.common.config;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import org.springframework.context.annotation.Configuration;

/**
 * JetCache 配置
 *
 * <p>启用 JetCache 注解功能，使 @Cached、@CacheInvalidate 等注解生效</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Configuration
@EnableCreateCacheAnnotation
public class JetCacheConfig {
}