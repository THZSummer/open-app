package com.xxx.it.works.wecode.v2.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
 *   <li>所有 ID 字段（Long/BigInteger）返回 string 类型，避免 JavaScript 精度丢失</li>
 *   <li>Java 8 时间类型序列化支持</li>
 *   <li>禁用日期序列化为时间戳</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
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

        // 设置时区为北京时间
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        // Long 类型序列化为 String，避免 JavaScript 精度丢失
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(simpleModule);

        return objectMapper;
    }
}
