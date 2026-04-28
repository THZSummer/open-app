package com.xxx.it.works.wecode.v2.common.config;

import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

/**
 * ID 生成器配置
 * 
 * <p>根据环境自动选择合适的 ID 生成策略</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class IdGeneratorConfig {

    private final List<IdGeneratorStrategy> strategies;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 根据当前环境选择合适的 ID 生成策略
     * 
     * @return ID 生成器策略实例
     */
    @Bean
    public IdGeneratorStrategy idGenerator() {
        IdGeneratorStrategy strategy = strategies.stream()
                .filter(s -> s.supports(activeProfile))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        String.format("No ID generator strategy found for current environment [%s]", activeProfile)));
        
        log.info("ID generator strategy loaded: {}, active profile: {}", 
                strategy.getClass().getSimpleName(), activeProfile);
        
        return strategy;
    }
}
