package com.xxx.it.works.wecode.v2.modules.runtime.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.auth.credential.CredentialInjectorRegistry;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import com.xxx.it.works.wecode.v2.modules.connector.repository.OpConnectorVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowReadRepository;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import com.xxx.it.works.wecode.v2.common.config.CacheToggle;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.ReactiveSequentialExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.ConnectorNodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.DataProcessorExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.TriggerNodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.node.ExitNodeExecutor;
import com.xxx.it.works.wecode.v2.modules.script.ScriptNodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.DagScheduler;
import com.xxx.it.works.wecode.v2.modules.runtime.FlowConfigParser;
import com.xxx.it.works.wecode.v2.modules.runtime.FlowRuntimeEngine;
import com.xxx.it.works.wecode.v2.modules.runtime.ParallelBranchExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.VersionConfigResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

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
                                                        OpConnectorVersionReadRepository connectorVersionReadRepository,
                                                        ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                                                        CacheToggle cacheToggle) {
        return new ConnectorNodeExecutor(objectMapper, webClient, credentialInjectorRegistry,
                connectorVersionReadRepository, reactiveRedisTemplate, cacheToggle);
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
            ExitNodeExecutor exitNodeExecutor,
            ScriptNodeExecutor scriptNodeExecutor) {
        return new ReactiveSequentialExecutor(
                objectMapper,
                triggerNodeExecutor,
                connectorNodeExecutor,
                dataProcessorExecutor,
                exitNodeExecutor,
                scriptNodeExecutor);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(config -> config
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();
    }

    // ===== V3 运行时引擎 Bean =====

    @Bean
    public FlowConfigParser flowConfigParser(ObjectMapper objectMapper) {
        return new FlowConfigParser(objectMapper);
    }

    @Bean
    public EntityCacheManager entityCacheManager(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                                                   ObjectMapper objectMapper,
                                                   OpFlowVersionReadRepository flowVersionReadRepository,
                                                   OpConnectorVersionReadRepository connectorVersionReadRepository) {
        return new EntityCacheManager(reactiveRedisTemplate, objectMapper,
                flowVersionReadRepository, connectorVersionReadRepository);
    }

    @Bean
    public VersionConfigResolver versionConfigResolver(OpFlowReadRepository flowReadRepository,
                                                         EntityCacheManager entityCacheManager,
                                                         FlowConfigParser flowConfigParser,
                                                         ObjectMapper objectMapper) {
        return new VersionConfigResolver(flowReadRepository, entityCacheManager,
                flowConfigParser, objectMapper);
    }

    @Bean
    public ParallelBranchExecutor parallelBranchExecutor(ObjectMapper objectMapper) {
        return new ParallelBranchExecutor(objectMapper);
    }

    @Bean
    public DagScheduler dagScheduler(ObjectMapper objectMapper,
                                       List<NodeExecutor> nodeExecutors) {
        return new DagScheduler(objectMapper, nodeExecutors);
    }

    @Bean
    public FlowRuntimeEngine flowRuntimeEngine(VersionConfigResolver versionConfigResolver,
                                                 DagScheduler dagScheduler,
                                                 FlowConfigParser flowConfigParser) {
        return new FlowRuntimeEngine(versionConfigResolver, dagScheduler, flowConfigParser);
    }
}
