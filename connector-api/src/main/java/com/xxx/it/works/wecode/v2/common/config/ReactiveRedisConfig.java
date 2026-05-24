package com.xxx.it.works.wecode.v2.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Reactive Redis 配置
 * <p>
 * 配置 Lettuce Reactive 驱动的 ReactiveRedisTemplate,
 * 用于响应式缓存访问。
 * </p>
 */
@Configuration
public class ReactiveRedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> context =
                RedisSerializationContext.<String, String>newSerializationContext()
                        .key(serializer)
                        .value(serializer)
                        .hashKey(serializer)
                        .hashValue(serializer)
                        .build();
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}