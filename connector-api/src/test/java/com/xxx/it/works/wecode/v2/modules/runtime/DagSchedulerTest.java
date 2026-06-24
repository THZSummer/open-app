package com.xxx.it.works.wecode.v2.modules.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.FlowConfig;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import com.xxx.it.works.wecode.v2.modules.runtime.model.ResolvedFlowConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DagScheduler 测试")
class DagSchedulerTest {

    @Mock
    private NodeExecutor triggerExecutor;

    @Mock
    private NodeExecutor connectorExecutor;

    @Mock
    private NodeExecutor exitExecutor;

    private ObjectMapper objectMapper;
    private DagScheduler scheduler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // 注册 Mock 执行器
        when(triggerExecutor.getNodeType()).thenReturn("trigger");
        when(connectorExecutor.getNodeType()).thenReturn("connector");
        when(exitExecutor.getNodeType()).thenReturn("exit");

        scheduler = new DagScheduler(objectMapper,
                List.of(triggerExecutor, connectorExecutor, exitExecutor));
    }

    // ===== 辅助方法 =====

    private ResolvedFlowConfig makeResolvedFlowConfig(String orchestrationConfig) {
        FlowEntity flow = new FlowEntity(100L, "测试流", "test_flow");
        flow.setDeployedVersionId(200L);
        FlowVersionEntity fv = new FlowVersionEntity();
        fv.setId(200L);
        fv.setFlowId(100L);
        fv.setVersionNumber(1);
        fv.setOrchestrationConfig(orchestrationConfig);
        return new ResolvedFlowConfig(flow, fv, Map.of(), FlowConfig.defaults());
    }

    private NodeOutput successOutput(String nodeId, String nodeType) {
        NodeOutput output = new NodeOutput(nodeId, nodeType,
                new HashMap<>(), Map.of("data", "ok_from_" + nodeId));
        output.setDurationMs(10);
        return output;
    }

    // ===== 串行调度测试 =====

    @Test
    @DisplayName("串行 DAG: trigger → connector → exit 按序执行")
    void testSerialDag_TriggerConnectorExit() {
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_conn\",\"type\":\"connector\"}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_conn\"}," +
                "{\"id\":\"e2\",\"source\":\"node_conn\",\"target\":\"node_exit\"}" +
                "]}";

        ResolvedFlowConfig resolved = makeResolvedFlowConfig(orchestrationConfig);
        ExecutionContext ctx = new ExecutionContext("exec-serial", "100");
        ctx.setTriggerData(Map.of("data", "test"));

        // Mock: 各执行器返回成功
        when(triggerExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_trigger", "trigger")));
        when(connectorExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_conn", "connector")));
        when(exitExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_exit", "exit")));

        Mono<ExecutionContext> resultMono = scheduler.schedule(resolved, ctx);

        StepVerifier.create(resultMono)
                .assertNext(resultCtx -> {
                    assertNotNull(resultCtx);
                    // 验证所有节点都被执行
                    assertNotNull(resultCtx.getNodeContext("node_trigger"));
                    assertNotNull(resultCtx.getNodeContext("node_conn"));
                    assertNotNull(resultCtx.getNodeContext("node_exit"));
                    assertEquals("success", resultCtx.getNodeContext("node_trigger").getStatus());
                    assertEquals("success", resultCtx.getNodeContext("node_conn").getStatus());
                    assertEquals("success", resultCtx.getNodeContext("node_exit").getStatus());
                })
                .verifyComplete();

        // 验证各执行器都被调用
        verify(triggerExecutor, times(1)).execute(any(ExecutionContext.class), any());
        verify(connectorExecutor, times(1)).execute(any(ExecutionContext.class), any());
        verify(exitExecutor, times(1)).execute(any(ExecutionContext.class), any());
    }

    @Test
    @DisplayName("串行 DAG: 仅 trigger → exit")
    void testSerialDag_TriggerExit_SimpleLinear() {
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_exit\"}" +
                "]}";

        ResolvedFlowConfig resolved = makeResolvedFlowConfig(orchestrationConfig);
        ExecutionContext ctx = new ExecutionContext("exec-linear", "100");

        when(triggerExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_trigger", "trigger")));
        when(exitExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_exit", "exit")));

        Mono<ExecutionContext> resultMono = scheduler.schedule(resolved, ctx);

        StepVerifier.create(resultMono)
                .assertNext(resultCtx -> {
                    assertEquals("success", resultCtx.getNodeContext("node_trigger").getStatus());
                    assertEquals("success", resultCtx.getNodeContext("node_exit").getStatus());
                })
                .verifyComplete();
    }

    // ===== 并行调度测试 =====

    @Test
    @DisplayName("并行 DAG: trigger → 2 个并行 connector → 汇聚到 exit")
    void testParallelDag_TwoBranches_Converge() {
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_conn_a\",\"type\":\"connector\"}," +
                "{\"id\":\"node_conn_b\",\"type\":\"connector\"}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_conn_a\"}," +
                "{\"id\":\"e2\",\"source\":\"node_trigger\",\"target\":\"node_conn_b\"}," +
                "{\"id\":\"e3\",\"source\":\"node_conn_a\",\"target\":\"node_exit\"}," +
                "{\"id\":\"e4\",\"source\":\"node_conn_b\",\"target\":\"node_exit\"}" +
                "]}";

        ResolvedFlowConfig resolved = makeResolvedFlowConfig(orchestrationConfig);
        ExecutionContext ctx = new ExecutionContext("exec-parallel", "100");

        when(triggerExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_trigger", "trigger")));

        // 两个 connector 并行执行
        when(connectorExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.delay(Duration.ofMillis(20))
                        .then(Mono.just(successOutput("node_conn_a", "connector"))))
                .thenReturn(Mono.delay(Duration.ofMillis(10))
                        .then(Mono.just(successOutput("node_conn_b", "connector"))));

        when(exitExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_exit", "exit")));

        Mono<ExecutionContext> resultMono = scheduler.schedule(resolved, ctx);

        StepVerifier.create(resultMono)
                .assertNext(resultCtx -> {
                    assertNotNull(resultCtx.getNodeContext("node_trigger"));
                    assertNotNull(resultCtx.getNodeContext("node_conn_a"));
                    assertNotNull(resultCtx.getNodeContext("node_conn_b"));
                    assertNotNull(resultCtx.getNodeContext("node_exit"));
                    // 两个并行分支都成功
                    assertEquals("success", resultCtx.getNodeContext("node_conn_a").getStatus());
                    assertEquals("success", resultCtx.getNodeContext("node_conn_b").getStatus());
                })
                .verifyComplete();
    }

    // ===== 节点超时测试 =====

    @Test
    @DisplayName("节点执行超时 → 标记为 timeout 状态, 不中断其他流程")
    void testNodeTimeout_MarkedAsTimeout() {
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_conn\",\"type\":\"connector\",\"data\":{\"timeoutMs\":100}}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_conn\"}," +
                "{\"id\":\"e2\",\"source\":\"node_conn\",\"target\":\"node_exit\"}" +
                "]}";

        ResolvedFlowConfig resolved = makeResolvedFlowConfig(orchestrationConfig);
        ExecutionContext ctx = new ExecutionContext("exec-timeout", "100");

        when(triggerExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_trigger", "trigger")));

        // connector 模拟超时: 远大于 timeoutMs=100
        when(connectorExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.delay(Duration.ofMillis(500))
                        .then(Mono.just(successOutput("node_conn", "connector"))));

        when(exitExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_exit", "exit")));

        Mono<ExecutionContext> resultMono = scheduler.schedule(resolved, ctx);

        StepVerifier.create(resultMono)
                .assertNext(resultCtx -> {
                    // connector 因超时应被标记为 timeout
                    assertNotNull(resultCtx.getNodeContext("node_conn"));
                    assertEquals("timeout", resultCtx.getNodeContext("node_conn").getStatus());
                    assertNotNull(resultCtx.getNodeContext("node_conn").getErrorInfo());
                    // exit 节点仍应执行
                    assertNotNull(resultCtx.getNodeContext("node_exit"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("节点无自定义超时 → 使用默认 30s 超时")
    void testNodeDefaultTimeout_30s() {
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_exit\"}" +
                "]}";

        ResolvedFlowConfig resolved = makeResolvedFlowConfig(orchestrationConfig);
        ExecutionContext ctx = new ExecutionContext("exec-default-timeout", "100");

        when(triggerExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_trigger", "trigger")));
        when(exitExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_exit", "exit")));

        Mono<ExecutionContext> resultMono = scheduler.schedule(resolved, ctx);

        StepVerifier.create(resultMono)
                .assertNext(resultCtx -> {
                    assertEquals("success", resultCtx.getNodeContext("node_trigger").getStatus());
                    assertEquals("success", resultCtx.getNodeContext("node_exit").getStatus());
                })
                .verifyComplete();
    }

    // ===== 未知节点类型 =====

    @Test
    @DisplayName("未知节点类型 → 标记为 skipped, 不中断 DAG")
    void testUnknownNodeType_Skipped() {
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_unknown\",\"type\":\"unknown_type\"}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_unknown\"}," +
                "{\"id\":\"e2\",\"source\":\"node_unknown\",\"target\":\"node_exit\"}" +
                "]}";

        ResolvedFlowConfig resolved = makeResolvedFlowConfig(orchestrationConfig);
        ExecutionContext ctx = new ExecutionContext("exec-unknown", "100");

        when(triggerExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_trigger", "trigger")));
        when(exitExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_exit", "exit")));

        Mono<ExecutionContext> resultMono = scheduler.schedule(resolved, ctx);

        StepVerifier.create(resultMono)
                .assertNext(resultCtx -> {
                    // 未知类型节点被跳过
                    assertNotNull(resultCtx.getNodeContext("node_unknown"));
                    assertEquals("skipped", resultCtx.getNodeContext("node_unknown").getStatus());
                    // exit 仍然执行
                    assertEquals("success", resultCtx.getNodeContext("node_exit").getStatus());
                })
                .verifyComplete();
    }

    // ===== 空编排 =====

    @Test
    @DisplayName("编排配置无节点 → 立即返回上下文")
    void testEmptyOrchestration_ReturnsImmediately() {
        String orchestrationConfig = "{\"nodes\":[],\"edges\":[]}";
        ResolvedFlowConfig resolved = makeResolvedFlowConfig(orchestrationConfig);
        ExecutionContext ctx = new ExecutionContext("exec-empty", "100");

        Mono<ExecutionContext> resultMono = scheduler.schedule(resolved, ctx);

        StepVerifier.create(resultMono)
                .assertNext(resultCtx -> {
                    assertNotNull(resultCtx);
                    assertEquals("exec-empty", resultCtx.getExecutionId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("编排配置无 trigger 节点 → 顺序执行所有节点")
    void testNoTriggerNode_SequentialExecution() {
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_conn_a\",\"type\":\"connector\"}," +
                "{\"id\":\"node_conn_b\",\"type\":\"connector\"}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[]}";

        ResolvedFlowConfig resolved = makeResolvedFlowConfig(orchestrationConfig);
        ExecutionContext ctx = new ExecutionContext("exec-no-trigger", "100");

        when(connectorExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_conn_a", "connector")))
                .thenReturn(Mono.just(successOutput("node_conn_b", "connector")));
        when(exitExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_exit", "exit")));

        Mono<ExecutionContext> resultMono = scheduler.schedule(resolved, ctx);

        StepVerifier.create(resultMono)
                .assertNext(resultCtx -> {
                    assertNotNull(resultCtx.getNodeContext("node_conn_a"));
                    assertNotNull(resultCtx.getNodeContext("node_conn_b"));
                    assertNotNull(resultCtx.getNodeContext("node_exit"));
                })
                .verifyComplete();
    }

    // ===== 并行分支中某一分支失败 → 不影响其他 =====

    @Test
    @DisplayName("并行分支中一个分支失败 → 其他分支正常完成并汇聚")
    void testParallelBranchOneFails_OthersProceed() {
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_conn_a\",\"type\":\"connector\"}," +
                "{\"id\":\"node_conn_b\",\"type\":\"connector\"}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_conn_a\"}," +
                "{\"id\":\"e2\",\"source\":\"node_trigger\",\"target\":\"node_conn_b\"}," +
                "{\"id\":\"e3\",\"source\":\"node_conn_a\",\"target\":\"node_exit\"}," +
                "{\"id\":\"e4\",\"source\":\"node_conn_b\",\"target\":\"node_exit\"}" +
                "]}";

        ResolvedFlowConfig resolved = makeResolvedFlowConfig(orchestrationConfig);
        ExecutionContext ctx = new ExecutionContext("exec-branch-fail", "100");

        when(triggerExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_trigger", "trigger")));

        // branch A 正常返回
        // branch B 直接失败
        when(connectorExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_conn_a", "connector")))
                .thenReturn(Mono.error(new RuntimeException("Branch B downstream error")));

        when(exitExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_exit", "exit")));

        Mono<ExecutionContext> resultMono = scheduler.schedule(resolved, ctx);

        StepVerifier.create(resultMono)
                .assertNext(resultCtx -> {
                    // branch A 正常完成
                    assertNotNull(resultCtx.getNodeContext("node_conn_a"));
                    assertEquals("success", resultCtx.getNodeContext("node_conn_a").getStatus());
                    // exit 仍然执行
                    assertNotNull(resultCtx.getNodeContext("node_exit"));
                })
                .verifyComplete();
    }

    // ===== 并行执行顺序验证 =====

    @Test
    @DisplayName("并行 DAG: 两分支并发执行总耗时 ≈ max(分支耗时)")
    void testParallelDag_ConcurrentExecution() {
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}," +
                "{\"id\":\"node_conn_a\",\"type\":\"connector\"}," +
                "{\"id\":\"node_conn_b\",\"type\":\"connector\"}," +
                "{\"id\":\"node_exit\",\"type\":\"exit\"}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"node_trigger\",\"target\":\"node_conn_a\"}," +
                "{\"id\":\"e2\",\"source\":\"node_trigger\",\"target\":\"node_conn_b\"}," +
                "{\"id\":\"e3\",\"source\":\"node_conn_a\",\"target\":\"node_exit\"}," +
                "{\"id\":\"e4\",\"source\":\"node_conn_b\",\"target\":\"node_exit\"}" +
                "]}";

        ResolvedFlowConfig resolved = makeResolvedFlowConfig(orchestrationConfig);
        ExecutionContext ctx = new ExecutionContext("exec-concurrent", "100");

        when(triggerExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_trigger", "trigger")));

        // 两个分支各延迟不同时间
        when(connectorExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.delay(Duration.ofMillis(200))
                        .then(Mono.just(successOutput("node_conn_a", "connector"))))
                .thenReturn(Mono.delay(Duration.ofMillis(100))
                        .then(Mono.just(successOutput("node_conn_b", "connector"))));

        when(exitExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_exit", "exit")));

        long start = System.currentTimeMillis();

        Mono<ExecutionContext> resultMono = scheduler.schedule(resolved, ctx);

        StepVerifier.create(resultMono)
                .assertNext(resultCtx -> {
                    long elapsed = System.currentTimeMillis() - start;
                    // 并行执行: 总耗时 < sum(200+100) = 300ms, 应近似 max(200,100)=200ms
                    assertTrue(elapsed < 350,
                            "Parallel execution should take ~max(branch times), not sum. Actual: " + elapsed + "ms");
                    assertNotNull(resultCtx.getNodeContext("node_conn_a"));
                    assertNotNull(resultCtx.getNodeContext("node_conn_b"));
                })
                .verifyComplete();
    }

    // ===== 单节点 DAG =====

    @Test
    @DisplayName("单节点 DAG: 仅 trigger → 无下游")
    void testSingleNodeDag_TriggerOnly() {
        String orchestrationConfig = "{\"nodes\":[" +
                "{\"id\":\"node_trigger\",\"type\":\"trigger\"}" +
                "],\"edges\":[]}";

        ResolvedFlowConfig resolved = makeResolvedFlowConfig(orchestrationConfig);
        ExecutionContext ctx = new ExecutionContext("exec-single", "100");

        when(triggerExecutor.execute(any(ExecutionContext.class), any()))
                .thenReturn(Mono.just(successOutput("node_trigger", "trigger")));

        Mono<ExecutionContext> resultMono = scheduler.schedule(resolved, ctx);

        StepVerifier.create(resultMono)
                .assertNext(resultCtx -> {
                    assertNotNull(resultCtx.getNodeContext("node_trigger"));
                    assertEquals("success", resultCtx.getNodeContext("node_trigger").getStatus());
                })
                .verifyComplete();
    }
}
