package com.xxx.it.works.wecode.v2.modules.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.model.FlowConfig;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ResolvedFlowConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * VersionConfigResolver 单元测试 (v6.0)
 * <p>
 * v6.0: 移除 ConnectorVersion 查询逻辑 — 连接器配置直接从 node.data.connectorVersionConfig 快照获取.
 * 仅测试 Flow/FlowVersion 加载和 flowConfig 解析.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VersionConfigResolverTest {

    @Mock
    private OpFlowReadRepository flowReadRepository;

    @Mock
    private EntityCacheManager entityCacheManager;

    @Mock
    private FlowConfigParser flowConfigParser;

    private ObjectMapper objectMapper = new ObjectMapper();

    private VersionConfigResolver resolver;

    private static final Long FLOW_ID = 1L;
    private static final Long DEPLOYED_VERSION_ID = 100L;

    @BeforeEach
    void setUp() {
        resolver = new VersionConfigResolver(flowReadRepository, entityCacheManager, flowConfigParser, objectMapper);
    }

    // ===== 正常: 部署的流 → 成功解析 =====

    @Test
    @DisplayName("正常: 已部署连接流 → 返回 ResolvedFlowConfig")
    void testResolveDeployedFlow_Success() {
        FlowEntity flow = createFlow(FLOW_ID, DEPLOYED_VERSION_ID);
        FlowVersionEntity flowVersion = createFlowVersion(DEPLOYED_VERSION_ID, emptyOrchConfig());

        when(flowReadRepository.findById(FLOW_ID)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(DEPLOYED_VERSION_ID)).thenReturn(Mono.just(flowVersion));
        when(flowConfigParser.parseFlowConfig(anyString())).thenReturn(FlowConfig.defaults());

        StepVerifier.create(resolver.resolveFlowVersion(FLOW_ID))
                .assertNext(resolved -> {
                    assertNotNull(resolved);
                    assertEquals(FLOW_ID, resolved.getFlow().getId());
                    assertEquals(DEPLOYED_VERSION_ID, resolved.getFlowVersion().getId());
                    assertNotNull(resolved.getFlowConfig());
                })
                .verifyComplete();

    }

    // ===== 异常: 流不存在 =====

    @Test
    @DisplayName("异常: 流不存在 → 抛出 FlowNotDeployedException")
    void testFlowNotFound_ThrowsException() {
        when(flowReadRepository.findById(FLOW_ID)).thenReturn(Mono.empty());

        StepVerifier.create(resolver.resolveFlowVersion(FLOW_ID))
                .expectError(VersionConfigResolver.FlowNotDeployedException.class)
                .verify();
    }

    // ===== 异常: deployed_version_id 为 NULL → 503 =====

    @Test
    @DisplayName("异常: deployed_version_id 为 NULL → FlowNotDeployedException")
    void testDeployedVersionIdNull_Throws503() {
        FlowEntity flow = createFlow(FLOW_ID, null);
        when(flowReadRepository.findById(FLOW_ID)).thenReturn(Mono.just(flow));

        StepVerifier.create(resolver.resolveFlowVersion(FLOW_ID))
                .expectError(VersionConfigResolver.FlowNotDeployedException.class)
                .verify();
    }

    // ===== 异常: FlowVersion 不存在 → 500 =====

    @Test
    @DisplayName("异常: 已部署版本不存在 → DeployedVersionNotFoundException")
    void testDeployedVersionNotFound_Throws500() {
        FlowEntity flow = createFlow(FLOW_ID, DEPLOYED_VERSION_ID);
        when(flowReadRepository.findById(FLOW_ID)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(DEPLOYED_VERSION_ID)).thenReturn(Mono.empty());

        StepVerifier.create(resolver.resolveFlowVersion(FLOW_ID))
                .expectError(VersionConfigResolver.DeployedVersionNotFoundException.class)
                .verify();
    }

    // ===== 正常: flowConfig 解析失败 → 使用默认值 =====

    @Test
    @DisplayName("flowConfig 解析异常 → 降级使用默认 FlowConfig")
    void testFlowConfigParseError_ReturnsDefaults() {
        FlowEntity flow = createFlow(FLOW_ID, DEPLOYED_VERSION_ID);
        FlowVersionEntity fv = createFlowVersion(DEPLOYED_VERSION_ID, "invalid-json{{{");

        when(flowReadRepository.findById(FLOW_ID)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(DEPLOYED_VERSION_ID)).thenReturn(Mono.just(fv));

        StepVerifier.create(resolver.resolveFlowVersion(FLOW_ID))
                .assertNext(resolved -> {
                    assertNotNull(resolved.getFlowConfig());
                    assertNull(resolved.getFlowConfig().getCacheTtl());
                })
                .verifyComplete();
    }

    // ===== 正常: 空编排 → 使用默认 FlowConfig =====

    @Test
    @DisplayName("空编排配置 → 返回默认 FlowConfig")
    void testEmptyOrchestration_ReturnsDefaultFlowConfig() {
        FlowEntity flow = createFlow(FLOW_ID, DEPLOYED_VERSION_ID);
        FlowVersionEntity fv = createFlowVersion(DEPLOYED_VERSION_ID, null);

        when(flowReadRepository.findById(FLOW_ID)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(DEPLOYED_VERSION_ID)).thenReturn(Mono.just(fv));

        StepVerifier.create(resolver.resolveFlowVersion(FLOW_ID))
                .assertNext(resolved -> assertNotNull(resolved.getFlowConfig()))
                .verifyComplete();
    }

    // ===== 辅助方法 =====

    private FlowEntity createFlow(Long id, Long deployedVersionId) {
        FlowEntity flow = new FlowEntity();
        flow.setId(id);
        flow.setDeployedVersionId(deployedVersionId);
        flow.setNameCn("测试连接流");
        flow.setNameEn("Test Flow");
        flow.setLifecycleStatus(2); // 运行中
        flow.setCreateTime(LocalDateTime.now());
        return flow;
    }

    private FlowVersionEntity createFlowVersion(Long id, String orchestrationConfig) {
        FlowVersionEntity fv = new FlowVersionEntity();
        fv.setId(id);
        fv.setFlowId(FLOW_ID);
        fv.setVersionNumber(1);
        fv.setStatus(5); // 已发布
        fv.setOrchestrationConfig(orchestrationConfig);
        fv.setCreateTime(LocalDateTime.now());
        return fv;
    }

    private String emptyOrchConfig() {
        return "{\"nodes\":[],\"edges\":[]}";
    }
}
