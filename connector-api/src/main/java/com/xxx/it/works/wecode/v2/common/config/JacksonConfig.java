package com.xxx.it.works.wecode.v2.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 全局配置
 * <p>
 * v5.5: 统一 ObjectMapper 配置, 确保 JSON Schema v5.5 字段名风格一致性.
 * <ul>
 *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES = false} — 兼容新旧字段名过渡</li>
 *   <li>{@code JavaTimeModule} — Java 8 时间序列化</li>
 *   <li>{@code CamelCase} 策略 — 与 Java POJO 字段命名一致</li>
 * </ul>
 * </p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 遇到未知字段时不抛异常 (支持字段名过渡)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 使用驼峰命名策略 (与 Java 字段名一致)
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        return mapper;
    }
}