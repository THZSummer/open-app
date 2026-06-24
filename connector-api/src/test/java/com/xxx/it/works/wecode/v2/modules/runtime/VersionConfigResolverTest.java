package com.xxx.it.works.wecode.v2.modules.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersionEntity;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VersionConfigResolver 测试")
class VersionConfigResolverTest {

    @Mock
    private OpFlowReadRepository flowReadRepository;

    @Mock
    private EntityCacheManager entityCacheManager;

    @Mock
    private FlowConfigParser flowConfigParser;

    private ObjectMapper objectMapper;

    private VersionConfigResolver resolver;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(flowConfigParser.parseFlowConfig(anyString())).thenReturn(FlowConfig.defaults());
        resolver = new VersionConfigResolver(flowReadRepository, entityCacheManager, flowConfigParser, objectMapper);
    }

    // ===== 异常: FlowEntity 不存在 → FlowNotDeployedException =====

    @Test
    @DisplayName("FlowEntity 不存在 → FlowNotDeployedException (503)")
    void testFlowNotFound_ThrowsFlowNotDeployedException() {
        Long flowId = 999L;
        when(flowReadRepository.findById(flowId)).thenReturn(Mono.empty());

        Mono<ResolvedFlowConfig> resultMono = resolver.resolveFlowVersion(flowId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(e ->
                        e instanceof VersionConfigResolver.FlowNotDeployedException &&
                        e.getMessage().contains("Flow not found"))
                .verify();
    }

    // ===== 异常: deployed_version_id is null → FlowNotDeployedException =====

    @Test
    @DisplayName("已部署版本ID为 NULL → FlowNotDeployedException (503)")
    void testFlowNotDeployed_NullVersionId_ThrowsException() {
        Long flowId = 100L;
        FlowEntity flow = new FlowEntity(flowId, "未部署流", "undeployed_flow");
        flow.setDeployedVersionId(null); // 未部署

        when(flowReadRepository.findById(flowId)).thenReturn(Mono.just(flow));

        Mono<ResolvedFlowConfig> resultMono = resolver.resolveFlowVersion(flowId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(e ->
                        e instanceof VersionConfigResolver.FlowNotDeployedException &&
                        e.getMessage().contains("Flow not deployed"))
                .verify();
    }

    // ===== 异常: FlowVersion 已删除 → DeployedVersionNotFoundException =====

    @Test
    @DisplayName("已部署版本不存在 (版本已删除) → DeployedVersionNotFoundException (500)")
    void testDeployedVersionDeleted_ThrowsException() {
        Long flowId = 200L;
        Long versionId = 300L;

        FlowEntity flow = new FlowEntity(flowId, "版本缺失流", "missing_version_flow");
        flow.setDeployedVersionId(versionId);

        when(flowReadRepository.findById(flowId)).thenReturn(Mono.just(flow));
        // EntityCacheManager 返回空 (缓存miss + DB也找不到)
        when(entityCacheManager.getFlowVersion(versionId)).thenReturn(Mono.empty());

        Mono<ResolvedFlowConfig> resultMono = resolver.resolveFlowVersion(flowId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(e ->
                        e instanceof VersionConfigResolver.DeployedVersionNotFoundException &&
                        e.getMessage().contains("Deployed version not found"))
                .verify();
    }

    // ===== 正常: 无连接器节点的连接流 → 成功解析 =====

    @Test
    @DisplayName("编排配置无连接器节点 → 成功解析, connectorConfigs 为空")
    void testNoConnectorNodes_ResolvesSuccessfully() {
        Long flowId = 300L;
        Long versionId = 400L;

        FlowEntity flow = new FlowEntity(flowId, "无连接器流", "no_conn_flow");
        flow.setDeployedVersionId(versionId);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(versionId);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(1);
        // 编排配置中没有 connector 类型节点
        flowVersion.setOrchestrationConfig(
                "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_processor\",\"type\":\"data_processor\"}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_processor\"}," +
                "{\"id\":\"e2\",\"source\":\"node_processor\",\"target\":\"node_exit\"}" +
                "]}");

        when(flowReadRepository.findById(flowId)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(versionId)).thenReturn(Mono.just(flowVersion));

        Mono<ResolvedFlowConfig> resultMono = resolver.resolveFlowVersion(flowId);

        StepVerifier.create(resultMono)
                .assertNext(resolved -> {
                    assertNotNull(resolved);
                    assertEquals(flowId, resolved.getFlow().getId());
                    assertEquals(versionId, resolved.getFlowVersion().getId());
                    assertNotNull(resolved.getConnectorConfigs());
                    assertTrue(resolved.getConnectorConfigs().isEmpty());
                })
                .verifyComplete();
    }

    // ===== 正常: 含连接器节点 → 加载 ConnectorVersion =====

    @Test
    @DisplayName("编排配置含连接器节点 → 加载 ConnectorVersion 并填充 connectorConfigs")
    void testWithConnectorNodes_LoadsConnectorVersions() {
        Long flowId = 400L;
        Long versionId = 500L;
        Long cvId = 100L;

        FlowEntity flow = new FlowEntity(flowId, "含连接器流", "with_conn_flow");
        flow.setDeployedVersionId(versionId);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(versionId);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(2);

        // 编排包含一个 connector 节点，引用 cvId=100
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_conn\",\"type\":\"connector\",\"data\":{\"connectorVersionRef\":" + cvId + "}}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_conn\"}," +
                "{\"id\":\"e2\",\"source\":\"node_conn\",\"target\":\"node_exit\"}" +
                "]}";
        flowVersion.setOrchestrationConfig(orchestrationConfig);

        ConnectorVersionEntity cv = new ConnectorVersionEntity();
        cv.setId(cvId);
        cv.setConnectorId(10L);
        cv.setVersionNumber(1);
        cv.setStatus(2); // published
        cv.setConnectionConfig("{\"url\":\"https://api.example.com\"}");

        when(flowReadRepository.findById(flowId)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(versionId)).thenReturn(Mono.just(flowVersion));
        when(entityCacheManager.getConnectorVersion(cvId)).thenReturn(Mono.just(cv));

        Mono<ResolvedFlowConfig> resultMono = resolver.resolveFlowVersion(flowId);

        StepVerifier.create(resultMono)
                .assertNext(resolved -> {
                    assertNotNull(resolved);
                    assertEquals(flowId, resolved.getFlow().getId());
                    assertEquals(1, resolved.getConnectorConfigs().size());
                    assertTrue(resolved.getConnectorConfigs().containsKey("node_conn"));
                    ConnectorVersionEntity loadedCv = resolved.getConnectorConfigs().get("node_conn");
                    assertEquals(cvId, loadedCv.getId());
                    assertEquals(2, loadedCv.getStatus());
                })
                .verifyComplete();
    }

    // ===== 正常: 多个连接器节点并行加载 =====

    @Test
    @DisplayName("编排配置含多个连接器节点 → 并行加载所有 ConnectorVersion")
    void testMultipleConnectorNodes_ParallelLoad() {
        Long flowId = 500L;
        Long versionId = 600L;
        Long cvId1 = 101L;
        Long cvId2 = 102L;

        FlowEntity flow = new FlowEntity(flowId, "多连接器流", "multi_conn_flow");
        flow.setDeployedVersionId(versionId);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(versionId);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(3);

        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_conn_a\",\"type\":\"connector\",\"data\":{\"connectorVersionRef\":" + cvId1 + "}}," +
                "{\"id\":\"node_conn_b\",\"type\":\"connector\",\"data\":{\"connectorVersionRef\":" + cvId2 + "}}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_conn_a\"}," +
                "{\"id\":\"e2\",\"source\":\"node_trigger\",\"target\":\"node_conn_b\"}," +
                "{\"id\":\"e3\",\"source\":\"node_conn_a\",\"target\":\"node_exit\"}," +
                "{\"id\":\"e4\",\"source\":\"node_conn_b\",\"target\":\"node_exit\"}" +
                "]}";
        flowVersion.setOrchestrationConfig(orchestrationConfig);

        ConnectorVersionEntity cv1 = new ConnectorVersionEntity();
        cv1.setId(cvId1);
        cv1.setConnectorId(11L);
        cv1.setVersionNumber(1);
        cv1.setStatus(2);
        cv1.setConnectionConfig("{\"url\":\"https://api1.example.com\"}");

        ConnectorVersionEntity cv2 = new ConnectorVersionEntity();
        cv2.setId(cvId2);
        cv2.setConnectorId(12L);
        cv2.setVersionNumber(2);
        cv2.setStatus(2);
        cv2.setConnectionConfig("{\"url\":\"https://api2.example.com\"}");

        when(flowReadRepository.findById(flowId)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(versionId)).thenReturn(Mono.just(flowVersion));
        when(entityCacheManager.getConnectorVersion(cvId1)).thenReturn(Mono.just(cv1));
        when(entityCacheManager.getConnectorVersion(cvId2)).thenReturn(Mono.just(cv2));

        Mono<ResolvedFlowConfig> resultMono = resolver.resolveFlowVersion(flowId);

        StepVerifier.create(resultMono)
                .assertNext(resolved -> {
                    assertEquals(2, resolved.getConnectorConfigs().size());
                    assertTrue(resolved.getConnectorConfigs().containsKey("node_conn_a"));
                    assertTrue(resolved.getConnectorConfigs().containsKey("node_conn_b"));
                })
                .verifyComplete();
    }

    // ===== ConnectorVersion 加载失败 → 不影响其他节点 =====

    @Test
    @DisplayName("某个 ConnectorVersion 加载失败 → 该节点被跳过, 其余正常加载")
    void testConnectorVersionLoadFailure_SkipsFailedNode() {
        Long flowId = 600L;
        Long versionId = 700L;
        Long cvIdGood = 201L;
        Long cvIdFail = 202L;

        FlowEntity flow = new FlowEntity(flowId, "容错流", "fault_tolerant_flow");
        flow.setDeployedVersionId(versionId);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(versionId);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(4);

        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_conn_good\",\"type\":\"connector\",\"data\":{\"connectorVersionRef\":" + cvIdGood + "}}," +
                "{\"id\":\"node_conn_bad\",\"type\":\"connector\",\"data\":{\"connectorVersionRef\":" + cvIdFail + "}}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_conn_good\"}," +
                "{\"id\":\"e2\",\"source\":\"node_conn_good\",\"target\":\"node_conn_bad\"}," +
                "{\"id\":\"e3\",\"source\":\"node_conn_bad\",\"target\":\"node_exit\"}" +
                "]}";
        flowVersion.setOrchestrationConfig(orchestrationConfig);

        ConnectorVersionEntity cvGood = new ConnectorVersionEntity();
        cvGood.setId(cvIdGood);
        cvGood.setConnectorId(21L);
        cvGood.setVersionNumber(1);
        cvGood.setStatus(2);

        when(flowReadRepository.findById(flowId)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(versionId)).thenReturn(Mono.just(flowVersion));
        // 好的连接器版本正常返回
        when(entityCacheManager.getConnectorVersion(cvIdGood)).thenReturn(Mono.just(cvGood));
        // 坏的连接器版本加载失败 (如已失效)
        when(entityCacheManager.getConnectorVersion(cvIdFail))
                .thenReturn(Mono.error(new RuntimeException("ConnectorVersion not found or invalidated")));

        Mono<ResolvedFlowConfig> resultMono = resolver.resolveFlowVersion(flowId);

        StepVerifier.create(resultMono)
                .assertNext(resolved -> {
                    // 只有好的连接器节点被加载
                    assertEquals(1, resolved.getConnectorConfigs().size());
                    assertTrue(resolved.getConnectorConfigs().containsKey("node_conn_good"));
                    assertFalse(resolved.getConnectorConfigs().containsKey("node_conn_bad"));
                })
                .verifyComplete();
    }

    // ===== FlowVersion 的 flowConfig 解析 =====

    @Test
    @DisplayName("编排配置含 flowConfig → 正确解析超时/限流/缓存配置")
    void testFlowConfigParsedCorrectly() {
        Long flowId = 700L;
        Long versionId = 800L;

        FlowEntity flow = new FlowEntity(flowId, "配置流", "config_flow");
        flow.setDeployedVersionId(versionId);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(versionId);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(5);
        flowVersion.setOrchestrationConfig(
                "{\"nodes\":[{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}]," +
                "\"edges\":[{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_exit\"}]," +
                "\"flowConfig\":{\"timeoutMs\":15000,\"rateLimitQps\":100,\"cacheTtl\":600}}");

        FlowConfig parsedConfig = new FlowConfig(15000, 100, null, 600, null);
        when(flowConfigParser.parseFlowConfig(anyString())).thenReturn(parsedConfig);

        when(flowReadRepository.findById(flowId)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(versionId)).thenReturn(Mono.just(flowVersion));

        Mono<ResolvedFlowConfig> resultMono = resolver.resolveFlowVersion(flowId);

        StepVerifier.create(resultMono)
                .assertNext(resolved -> {
                    assertNotNull(resolved.getFlowConfig());
                    assertEquals(15000, resolved.getFlowConfig().getTimeoutMs());
                    assertEquals(100, resolved.getFlowConfig().getRateLimitQps());
                    assertEquals(600, resolved.getFlowConfig().getCacheTtl());
                })
                .verifyComplete();
    }

    // ===== FlowVersion 的解析异常使用默认值 =====

    @Test
    @DisplayName("编排配置解析异常 → 使用默认 FlowConfig, 不抛异常")
    void testOrchestrationConfigParseException_UsesDefaults() {
        Long flowId = 800L;
        Long versionId = 900L;

        FlowEntity flow = new FlowEntity(flowId, "异常配置流", "error_config_flow");
        flow.setDeployedVersionId(versionId);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(versionId);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(6);
        flowVersion.setOrchestrationConfig("{ invalid json "); // 非法 JSON

        when(flowConfigParser.parseFlowConfig(anyString())).thenReturn(FlowConfig.defaults());

        when(flowReadRepository.findById(flowId)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(versionId)).thenReturn(Mono.just(flowVersion));

        Mono<ResolvedFlowConfig> resultMono = resolver.resolveFlowVersion(flowId);

        StepVerifier.create(resultMono)
                .assertNext(resolved -> {
                    assertNotNull(resolved);
                    assertNotNull(resolved.getFlowConfig());
                    // 使用默认值
                    assertNull(resolved.getFlowConfig().getTimeoutMs());
                    assertNull(resolved.getFlowConfig().getRateLimitQps());
                    assertNull(resolved.getFlowConfig().getCacheTtl());
                })
                .verifyComplete();
    }

    // ===== 边界: 空编排配置 =====

    @Test
    @DisplayName("编排配置为 null → 使用默认 FlowConfig, 无连接器节点")
    void testNullOrchestrationConfig_UsesDefaults() {
        Long flowId = 900L;
        Long versionId = 1000L;

        FlowEntity flow = new FlowEntity(flowId, "空配置流", "empty_config_flow");
        flow.setDeployedVersionId(versionId);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(versionId);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(7);
        flowVersion.setOrchestrationConfig(null); // 空白

        when(flowReadRepository.findById(flowId)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(versionId)).thenReturn(Mono.just(flowVersion));

        Mono<ResolvedFlowConfig> resultMono = resolver.resolveFlowVersion(flowId);

        StepVerifier.create(resultMono)
                .assertNext(resolved -> {
                    assertNotNull(resolved);
                    assertTrue(resolved.getConnectorConfigs().isEmpty());
                })
                .verifyComplete();
    }

    // ===== 向后兼容: connectorVersionId 旧字段 =====

    @Test
    @DisplayName("向后兼容: 使用旧字段 connectorVersionId 仍可加载")
    void testBackwardCompat_OldConnectorVersionIdField() {
        Long flowId = 1000L;
        Long versionId = 1100L;
        Long cvId = 301L;

        FlowEntity flow = new FlowEntity(flowId, "旧格式流", "legacy_flow");
        flow.setDeployedVersionId(versionId);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(versionId);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(8);

        // 使用旧字段名 connectorVersionId
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_conn\",\"type\":\"connector\",\"data\":{\"connectorVersionId\":" + cvId + "}}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_conn\"}," +
                "{\"id\":\"e2\",\"source\":\"node_conn\",\"target\":\"node_exit\"}" +
                "]}";
        flowVersion.setOrchestrationConfig(orchestrationConfig);

        ConnectorVersionEntity cv = new ConnectorVersionEntity();
        cv.setId(cvId);
        cv.setConnectorId(31L);
        cv.setVersionNumber(3);
        cv.setStatus(2);

        when(flowReadRepository.findById(flowId)).thenReturn(Mono.just(flow));
        when(entityCacheManager.getFlowVersion(versionId)).thenReturn(Mono.just(flowVersion));
        when(entityCacheManager.getConnectorVersion(cvId)).thenReturn(Mono.just(cv));

        Mono<ResolvedFlowConfig> resultMono = resolver.resolveFlowVersion(flowId);

        StepVerifier.create(resultMono)
                .assertNext(resolved -> {
                    assertEquals(1, resolved.getConnectorConfigs().size());
                    assertTrue(resolved.getConnectorConfigs().containsKey("node_conn"));
                })
                .verifyComplete();
    }
}
