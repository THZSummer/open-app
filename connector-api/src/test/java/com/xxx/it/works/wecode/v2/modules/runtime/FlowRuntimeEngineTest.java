package com.xxx.it.works.wecode.v2.modules.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.cache.FlowCacheManager;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ExecutionResult;
import com.xxx.it.works.wecode.v2.modules.runtime.model.FlowConfig;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ResolvedFlowConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlowRuntimeEngine 测试")
class FlowRuntimeEngineTest {

    @Mock
    private VersionConfigResolver versionConfigResolver;

    @Mock
    private DagScheduler dagScheduler;

    @Mock
    private FlowConfigParser flowConfigParser;

    @Mock
    private FlowCacheManager cacheManager;

    private FlowRuntimeEngine engine;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        engine = new FlowRuntimeEngine(versionConfigResolver, dagScheduler, flowConfigParser,
                cacheManager, objectMapper);
    }

    // ===== 正常执行路径 =====

    @Test
    @DisplayName("5阶段管道正常执行: Phase 2 解析成功 → Phase 5 DAG 调度成功")
    void testFullPipelineNormalExecution() {
        // Given: 已部署的连接流
        Long flowId = 100L;
        ExecutionContext ctx = new ExecutionContext("exec-001", String.valueOf(flowId));
        ctx.setTriggerData(Map.of("data", "test"));

        FlowEntity flow = new FlowEntity(flowId, "测试流", "test_flow");
        flow.setDeployedVersionId(200L);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(200L);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(1);
        flowVersion.setOrchestrationConfig(
                "{\"nodes\":[{\"id\":\"node_trigger\",\"type\":\"trigger\"},{\"id\":\"node_exit\",\"type\":\"exit\"}]," +
                "\"edges\":[{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_exit\"}]}");

        FlowConfig flowConfig = FlowConfig.defaults();
        ResolvedFlowConfig resolved = new ResolvedFlowConfig(flow, flowVersion, flowConfig);

        // Mock: VersionConfigResolver 正常返回
        when(versionConfigResolver.resolveFlowVersion(flowId)).thenReturn(Mono.just(resolved));

        // Mock: DagScheduler 返回填充了结果的 ExecutionContext
        when(dagScheduler.schedule(any(ResolvedFlowConfig.class), any(ExecutionContext.class)))
                .thenAnswer(invocation -> {
                    ExecutionContext c = invocation.getArgument(1);
                    NodeContext nc = new NodeContext();
                    nc.setNodeId("node_exit");
                    nc.setNodeType("exit");
                    nc.setInput(new HashMap<>());
                    nc.setOutput(Map.of("result", "ok"));
                    nc.setStatus("success");
                    nc.setDurationMs(10);
                    c.setNodeContext(nc);
                    return Mono.just(c);
                });

        // When
        Mono<ExecutionResult> resultMono = engine.execute(flowId, ctx);

        // Then
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("success", result.getStatus());
                    assertEquals("exec-001", result.getExecutionId());
                    assertEquals(String.valueOf(flowId), result.getFlowId());
                    assertTrue(result.getTotalDurationMs() >= 0);
                    assertNotNull(result.getResultData());
                    assertEquals("ok", result.getResultData().get("result"));
                    assertEquals(1, result.getSteps().size());
                    assertEquals("node_exit", result.getSteps().get(0).getNodeId());
                })
                .verifyComplete();
    }

    // ===== 异常路径: FlowNotDeployedException → 503 =====

    @Test
    @DisplayName("连接流未部署 → Phase 1 失败, 返回 503")
    void testFlowNotDeployed_Returns503() {
        Long flowId = 200L;
        ExecutionContext ctx = new ExecutionContext("exec-503", String.valueOf(flowId));

        when(versionConfigResolver.resolveFlowVersion(flowId))
                .thenReturn(Mono.error(new VersionConfigResolver.FlowNotDeployedException(
                        "Flow not deployed: flowId=" + flowId)));

        Mono<ExecutionResult> resultMono = engine.execute(flowId, ctx);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("failed", result.getStatus());
                    assertNotNull(result.getErrorInfo());
                    assertEquals("503", result.getErrorInfo().get("code"));
                    assertEquals("Flow not deployed", result.getErrorInfo().get("message"));
                    assertEquals("连接流未部署", result.getErrorInfo().get("messageZh"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("连接流不存在 → FlowNotDeployedException, 返回 503")
    void testFlowNotFound_Returns503() {
        Long flowId = 999L;
        ExecutionContext ctx = new ExecutionContext("exec-503b", String.valueOf(flowId));

        when(versionConfigResolver.resolveFlowVersion(flowId))
                .thenReturn(Mono.error(new VersionConfigResolver.FlowNotDeployedException(
                        "Flow not found: " + flowId)));

        Mono<ExecutionResult> resultMono = engine.execute(flowId, ctx);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("failed", result.getStatus());
                    assertEquals("503", result.getErrorInfo().get("code"));
                })
                .verifyComplete();
    }

    // ===== 异常路径: DeployedVersionNotFoundException → 500 =====

    @Test
    @DisplayName("已部署版本不存在 → 返回 500")
    void testDeployedVersionNotFound_Returns500() {
        Long flowId = 300L;
        ExecutionContext ctx = new ExecutionContext("exec-500", String.valueOf(flowId));

        when(versionConfigResolver.resolveFlowVersion(flowId))
                .thenReturn(Mono.error(new VersionConfigResolver.DeployedVersionNotFoundException(
                        "Deployed version not found: versionId=999")));

        Mono<ExecutionResult> resultMono = engine.execute(flowId, ctx);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("failed", result.getStatus());
                    assertNotNull(result.getErrorInfo());
                    assertEquals("500", result.getErrorInfo().get("code"));
                    assertEquals("Deployed version not found", result.getErrorInfo().get("message"));
                    assertEquals("已部署版本不存在", result.getErrorInfo().get("messageZh"));
                })
                .verifyComplete();
    }

    // ===== 异常路径: 通用错误 → 500 =====

    @Test
    @DisplayName("Phase 2 解析时未知运行时异常 → 返回 500")
    void testGenericException_Returns500() {
        Long flowId = 400L;
        ExecutionContext ctx = new ExecutionContext("exec-generic", String.valueOf(flowId));

        when(versionConfigResolver.resolveFlowVersion(flowId))
                .thenReturn(Mono.error(new RuntimeException("Unexpected DB connection error")));

        Mono<ExecutionResult> resultMono = engine.execute(flowId, ctx);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("failed", result.getStatus());
                    assertNotNull(result.getErrorInfo());
                    assertEquals("500", result.getErrorInfo().get("code"));
                    String message = (String) result.getErrorInfo().get("message");
                    assertTrue(message.contains("Unexpected DB connection error"));
                    String messageZh = (String) result.getErrorInfo().get("messageZh");
                    assertTrue(messageZh.contains("内部执行错误"));
                })
                .verifyComplete();
    }

    // ===== DAG 执行失败: 节点超时/失败 → result status=failed =====

    @Test
    @DisplayName("DAG 调度中节点执行失败 → 整体状态为 failed")
    void testDagNodeFailure_OverallFailed() {
        Long flowId = 500L;
        ExecutionContext ctx = new ExecutionContext("exec-dagfail", String.valueOf(flowId));

        FlowEntity flow = new FlowEntity(flowId, "失败流", "fail_flow");
        flow.setDeployedVersionId(201L);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(201L);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(1);
        flowVersion.setOrchestrationConfig("{\"nodes\":[],\"edges\":[]}");

        ResolvedFlowConfig resolved = new ResolvedFlowConfig(flow, flowVersion, FlowConfig.defaults());

        when(versionConfigResolver.resolveFlowVersion(flowId)).thenReturn(Mono.just(resolved));
        when(dagScheduler.schedule(any(ResolvedFlowConfig.class), any(ExecutionContext.class)))
                .thenAnswer(invocation -> {
                    ExecutionContext c = invocation.getArgument(1);
                    NodeContext nc = new NodeContext();
                    nc.setNodeId("node_conn");
                    nc.setNodeType("connector");
                    nc.setStatus("failed");
                    nc.setDurationMs(5000);
                    nc.setErrorInfo(Map.of("code", "60000", "messageZh", "下游服务返回 500"));
                    c.setNodeContext(nc);
                    return Mono.just(c);
                });

        Mono<ExecutionResult> resultMono = engine.execute(flowId, ctx);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("failed", result.getStatus());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("DAG 调度中节点超时 → 整体状态为 failed")
    void testDagNodeTimeout_OverallFailed() {
        Long flowId = 600L;
        ExecutionContext ctx = new ExecutionContext("exec-timeout", String.valueOf(flowId));

        FlowEntity flow = new FlowEntity(flowId, "超时流", "timeout_flow");
        flow.setDeployedVersionId(202L);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(202L);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(1);
        flowVersion.setOrchestrationConfig("{\"nodes\":[],\"edges\":[]}");

        ResolvedFlowConfig resolved = new ResolvedFlowConfig(flow, flowVersion, FlowConfig.defaults());

        when(versionConfigResolver.resolveFlowVersion(flowId)).thenReturn(Mono.just(resolved));
        when(dagScheduler.schedule(any(ResolvedFlowConfig.class), any(ExecutionContext.class)))
                .thenAnswer(invocation -> {
                    ExecutionContext c = invocation.getArgument(1);
                    NodeContext nc = new NodeContext();
                    nc.setNodeId("node_conn");
                    nc.setNodeType("connector");
                    nc.setStatus("timeout");
                    nc.setDurationMs(30000);
                    nc.setErrorInfo(Map.of("code", "60002", "messageZh", "节点执行超时"));
                    c.setNodeContext(nc);
                    return Mono.just(c);
                });

        Mono<ExecutionResult> resultMono = engine.execute(flowId, ctx);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("failed", result.getStatus());
                })
                .verifyComplete();
    }

    // ===== 结果清理: __status/__input/__error 被移除 =====

    @Test
    @DisplayName("最终输出中 __status/__input/__error 内部字段被清除")
    void testResultDataCleaned_InternalFieldsRemoved() {
        Long flowId = 700L;
        ExecutionContext ctx = new ExecutionContext("exec-clean", String.valueOf(flowId));

        FlowEntity flow = new FlowEntity(flowId, "清理流", "clean_flow");
        flow.setDeployedVersionId(203L);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(203L);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(1);
        flowVersion.setOrchestrationConfig("{\"nodes\":[],\"edges\":[]}");

        ResolvedFlowConfig resolved = new ResolvedFlowConfig(flow, flowVersion, FlowConfig.defaults());

        when(versionConfigResolver.resolveFlowVersion(flowId)).thenReturn(Mono.just(resolved));
        when(dagScheduler.schedule(any(ResolvedFlowConfig.class), any(ExecutionContext.class)))
                .thenAnswer(invocation -> {
                    ExecutionContext c = invocation.getArgument(1);
                    NodeContext nc = new NodeContext();
                    nc.setNodeId("node_exit");
                    nc.setNodeType("exit");
                    Map<String, Object> output = new HashMap<>();
                    output.put("data", "clean_value");
                    output.put("__status", "200");
                    output.put("__input", Map.of());
                    output.put("__error", null);
                    nc.setOutput(output);
                    nc.setStatus("success");
                    nc.setDurationMs(5);
                    c.setNodeContext(nc);
                    return Mono.just(c);
                });

        Mono<ExecutionResult> resultMono = engine.execute(flowId, ctx);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("success", result.getStatus());
                    Map<String, Object> resultData = result.getResultData();
                    assertNotNull(resultData);
                    assertEquals("clean_value", resultData.get("data"));
                    assertFalse(resultData.containsKey("__status"), "__status should be removed");
                    assertFalse(resultData.containsKey("__input"), "__input should be removed");
                    assertFalse(resultData.containsKey("__error"), "__error should be removed");
                })
                .verifyComplete();
    }

    // ===== 缓存配置存在但未集成 =====

    @Test
    @DisplayName("编排配置含缓存 TTL → 正常运行 (缓存集成未完成暂不影响)")
    void testCacheConfigured_ProceedsNormally() {
        Long flowId = 800L;
        ExecutionContext ctx = new ExecutionContext("exec-cache", String.valueOf(flowId));

        FlowEntity flow = new FlowEntity(flowId, "缓存流", "cache_flow");
        flow.setDeployedVersionId(204L);

        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setId(204L);
        flowVersion.setFlowId(flowId);
        flowVersion.setVersionNumber(1);
        flowVersion.setOrchestrationConfig("{\"nodes\":[],\"edges\":[]}");

        FlowConfig cachedConfig = new FlowConfig(null, null, null, 600, null);
        ResolvedFlowConfig resolved = new ResolvedFlowConfig(flow, flowVersion, cachedConfig);

        when(versionConfigResolver.resolveFlowVersion(flowId)).thenReturn(Mono.just(resolved));
        when(dagScheduler.schedule(any(ResolvedFlowConfig.class), any(ExecutionContext.class)))
                .thenReturn(Mono.just(ctx));
        when(cacheManager.checkCache(anyLong(), any())).thenReturn(Mono.empty());
        when(cacheManager.writeCache(anyLong(), any(), any(), anyInt())).thenReturn(Mono.empty());

        Mono<ExecutionResult> resultMono = engine.execute(flowId, ctx);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals("success", result.getStatus());
                })
                .verifyComplete();
    }
}
