package com.xxx.it.works.wecode.v2.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

/**
 * Jackson 配置
 *
 * <p>配置 JSON 序列化规则：</p>
 * <ul>
 *   <li>Java 8 时间类型序列化支持</li>
 *   <li>禁用日期序列化为时间戳</li>
 *   <li>忽略未知 JSON 属性，支持向后兼容（新旧字段共存）</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.1.0
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // Java 8 时间模块
        objectMapper.registerModule(new JavaTimeModule());

        // 禁用日期序列化为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 忽略未知 JSON 属性（支持向后兼容，新旧字段共存时不会报错）
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 设置时区为北京时间
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        return objectMapper;
    }
}
