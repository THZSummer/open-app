package com.xxx.it.works.wecode.v2.modules.runtime.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.auth.credential.CredentialInjectorRegistry;
import com.xxx.it.works.wecode.v2.modules.connector.repository.OpConnectorVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.ConnectorNodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.DataProcessorExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.TriggerNodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.ExitNodeExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 运行时模块 Bean 配置
 * <p>
 * 注册 ReactiveSequentialExecutor 及各节点执行器为 Spring Bean
 * </p>
 */
@Configuration
public class RuntimeConfig {

    @Bean
    public TriggerNodeExecutor triggerNodeExecutor(ObjectMapper objectMapper) {
        return new TriggerNodeExecutor(objectMapper);
    }

    @Bean
    public ConnectorNodeExecutor connectorNodeExecutor(ObjectMapper objectMapper, WebClient webClient,
                                                        CredentialInjectorRegistry credentialInjectorRegistry,
                                                        OpConnectorVersionReadRepository connectorVersionReadRepository) {
        return new ConnectorNodeExecutor(objectMapper, webClient, credentialInjectorRegistry, connectorVersionReadRepository);
    }

    @Bean
    public DataProcessorExecutor dataProcessorExecutor(ObjectMapper objectMapper) {
        return new DataProcessorExecutor(objectMapper);
    }

    @Bean
    public ExitNodeExecutor exitNodeExecutor(ObjectMapper objectMapper) {
        return new ExitNodeExecutor(objectMapper);
    }

    @Bean
    public ReactiveSequentialExecutor reactiveSequentialExecutor(
            ObjectMapper objectMapper,
            TriggerNodeExecutor triggerNodeExecutor,
            ConnectorNodeExecutor connectorNodeExecutor,
            DataProcessorExecutor dataProcessorExecutor,
            ExitNodeExecutor exitNodeExecutor) {
        return new ReactiveSequentialExecutor(
                objectMapper,
                triggerNodeExecutor,
                connectorNodeExecutor,
                dataProcessorExecutor,
                exitNodeExecutor);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(config -> config
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();
    }
}
