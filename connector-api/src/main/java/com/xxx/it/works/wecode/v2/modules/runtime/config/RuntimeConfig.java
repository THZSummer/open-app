package com.xxx.it.works.wecode.v2.modules.runtime.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.auth.credential.UnifiedCredentialProcessor;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import com.xxx.it.works.wecode.v2.modules.cache.FlowCacheManager;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowReadRepository;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import io.netty.channel.ChannelOption;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
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
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
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
                                                         UnifiedCredentialProcessor credentialProcessor) {
        return new ConnectorNodeExecutor(objectMapper, webClient, credentialProcessor);
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
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
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
                                                   OpFlowReadRepository flowReadRepository) {
        return new EntityCacheManager(reactiveRedisTemplate, objectMapper,
                flowVersionReadRepository, flowReadRepository);
    }

    @Bean
    public VersionConfigResolver versionConfigResolver(EntityCacheManager entityCacheManager,
                                                         FlowConfigParser flowConfigParser,
                                                         ObjectMapper objectMapper) {
        return new VersionConfigResolver(entityCacheManager,
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
                                                 FlowConfigParser flowConfigParser,
                                                 FlowCacheManager cacheManager,
                                                 ObjectMapper objectMapper) {
        return new FlowRuntimeEngine(versionConfigResolver, dagScheduler, flowConfigParser,
                cacheManager, objectMapper);
    }
}
