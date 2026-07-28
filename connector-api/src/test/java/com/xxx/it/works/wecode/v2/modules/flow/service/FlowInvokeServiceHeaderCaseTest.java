package com.xxx.it.works.wecode.v2.modules.flow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.IdGenerator;
import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
import com.xxx.it.works.wecode.v2.modules.auth.SysTokenResolver;
import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;
import com.xxx.it.works.wecode.v2.modules.cache.FlowCacheManager;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionRecordService;
import com.xxx.it.works.wecode.v2.modules.execution.ExecutionStepService;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;
import com.xxx.it.works.wecode.v2.modules.runtime.DagScheduler;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.model.TransparentFlowResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FlowInvokeService header 大小写不敏感传递链路测试.
 * <p>
 * 验证 Controller 层构建的大小写不敏感 ciHeaders (TreeMap) 是否能贯穿到
 * NodeContext.input.header, 供 ExpressionResolver 大小写不敏感地解析.
 * </p>
 * <p>
 * 核心断点: {@link FlowInvokeService#buildStructuredTriggerInput} 中
 * {@code new HashMap<>().putAll(headers)} 会将 TreeMap 降级为大小写敏感的 HashMap,
 * 导致 Nginx 小写化后的 header name 无法被表达式原始大小写引用命中.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlowInvokeService header 大小写不敏感传递链路测试")
class FlowInvokeServiceHeaderCaseTest {

    @Mock
    private OpFlowVersionReadRepository flowVersionReadRepository;
    @Mock
    private DagScheduler dagScheduler;
    @Mock
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOperations;
    @Mock
    private FlowCacheManager cacheManager;
    @Mock
    private EntityCacheManager entityCacheManager;
    @Mock
    private ExecutionRecordService executionRecordService;
    @Mock
    private ExecutionStepService executionStepService;
    @Mock
    private IdGenerator idGenerator;
    @Mock
    private ConnectorApiPropertyService propertyService;

    private FlowInvokeService triggerService;

    /**
     * trigger 节点 input 契约仅声明 body (无 header/query 段),
     * 故 header 校验被跳过, 但 buildStructuredTriggerInput 仍会把 headers 放入 input.header.
     */
    private static final String ORCHESTRATION_CONFIG =
            "{\"nodes\":["
            + "{\"id\":\"node_trigger\",\"type\":\"trigger\",\"position\":{\"x\":0,\"y\":0},"
            + "\"data\":{\"triggerType\":\"http\",\"authConfigs\":[{\"type\":\"SYSTOKEN\"}],\"input\":{\"body\":{}}}},"
            + "{\"id\":\"n1\",\"type\":\"exit\",\"position\":{\"x\":300,\"y\":0},"
            + "\"data\":{\"labelCn\":\"结束\"}}"
            + "],\"edges\":[]}";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        lenient().when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(Mono.empty());
        lenient().when(valueOperations.set(anyString(), anyString(), any())).thenReturn(Mono.just(true));
        lenient().when(idGenerator.nextId()).thenReturn(1L);
        lenient().when(propertyService.isLogCollectionEnabled()).thenReturn(Mono.just(true));
        lenient().when(propertyService.loadPlatformDefaults()).thenReturn(Mono.just(Map.of()));
        triggerService = new FlowInvokeService(objectMapper,
                dagScheduler, flowVersionReadRepository,
                reactiveRedisTemplate, cacheManager, entityCacheManager,
                executionRecordService, executionStepService, idGenerator,
                propertyService, new SysTokenResolver());
    }

    /**
     * v5.9 验证: buildStructuredTriggerInput 仍产出 HashMap (大小写敏感),
     * 但 ExpressionResolver.getCaseInsensitive 回退查找使表达式可正确解析.
     *
     * 该用例验证 Map 层的事实行为: header 用 HashMap 承载时 get 大小写敏感.
     * 修复策略已从分散的 Map 类型修改集中到 ExpressionResolver 一处.
     */
    @Test
    @DisplayName("[v5.9] header Map 仍为 HashMap (大小写敏感), 但 ExpressionResolver 回退查找兜底")
    void headerMap_isHashMap_caseSensitive_resolverHandlesCaseInsensitivity() {
        FlowVersionEntity flowVersion = new FlowVersionEntity();
        flowVersion.setFlowId(100L);
        flowVersion.setOrchestrationConfig(ORCHESTRATION_CONFIG);

        FlowEntity runningFlow = new FlowEntity();
        runningFlow.setLifecycleStatus(2);
        when(entityCacheManager.getFlow(100L)).thenReturn(Mono.just(runningFlow));
        when(flowVersionReadRepository.findByFlowId(100L)).thenReturn(Mono.just(flowVersion));

        // dagScheduler 返回一个含 exit 节点的上下文 (供 buildResult 使用)
        ExecutionContext returnedCtx = new ExecutionContext("exec-001", "100");
        NodeContext exitCtx = new NodeContext();
        exitCtx.setNodeId("n1");
        exitCtx.setNodeType("exit");
        exitCtx.setStatus("success");
        exitCtx.setOutput(Map.of("body", Map.of("ok", true)));
        returnedCtx.setNodeContext(exitCtx);
        when(dagScheduler.schedule(anyString(), any())).thenReturn(Mono.just(returnedCtx));

        // ★ 模拟 Controller: 用大小写不敏感 TreeMap 承载 header (与 FlowInvokeController L86 一致)
        // 模拟标准环境 Nginx 小写化: header name 全小写
        Map<String, String> ciHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ciHeaders.put("x-sys-token", "test-token");    // 认证用 (SYSTOKEN)
        ciHeaders.put("x-trace-id", "trace-lower-123"); // 业务 header, 小写

        Mono<TransparentFlowResponse> resultMono = triggerService.invokeFlow(
                100L, Map.of("sender", "test"), ciHeaders, Map.of());

        // 触发执行
        StepVerifier.create(resultMono)
                .assertNext(response -> assertEquals(200, response.getHttpStatus().value()))
                .verifyComplete();

        // ★ 捕获传给 dagScheduler 的 ExecutionContext, 检查 trigger 节点 input.header
        ArgumentCaptor<ExecutionContext> ctxCaptor = ArgumentCaptor.forClass(ExecutionContext.class);
        verify(dagScheduler).schedule(anyString(), ctxCaptor.capture());
        ExecutionContext capturedCtx = ctxCaptor.getValue();

        NodeContext triggerNodeCtx = capturedCtx.getNodeContext("node_trigger");
        assertNotNull(triggerNodeCtx, "trigger 节点上下文应存在");
        assertNotNull(triggerNodeCtx.getInput(), "trigger input 应存在");

        @SuppressWarnings("unchecked")
        Map<String, Object> header = (Map<String, Object>) triggerNodeCtx.getInput().get("header");
        assertNotNull(header, "input.header 分区应存在");

        // ★ v5.9: header Map 本身仍为 HashMap (大小写敏感), get("X-Trace-Id") 返回 null
        assertNull(header.get("X-Trace-Id"),
                "header Map 为 HashMap, get 大小写敏感: x-trace-id != X-Trace-Id -> null. "
                + "实际类型=" + header.getClass().getSimpleName()
                + ", keys=" + header.keySet());
        // 基准: 小写 key 直接查询能命中
        assertEquals("trace-lower-123", header.get("x-trace-id"),
                "HashMap 精确小写 key 查询应命中");
    }

    /**
     * 对照基准: Controller 传入的 ciHeaders (TreeMap) 本身大小写不敏感,
     * 证明问题不在 Controller, 而在 buildStructuredTriggerInput 的转换.
     */
    @Test
    @DisplayName("[对照] Controller 的 ciHeaders(TreeMap) 本身大小写不敏感 (证明断点在下游)")
    void controllerCiHeaders_isCaseInsensitive() {
        Map<String, String> ciHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ciHeaders.put("x-trace-id", "trace-lower-123");

        // TreeMap 本身大小写不敏感查询
        assertEquals("trace-lower-123", ciHeaders.get("X-Trace-Id"),
                "TreeMap(CASE_INSENSITIVE_ORDER) 大小写不敏感查询应命中");
        assertEquals("trace-lower-123", ciHeaders.get("X-TRACE-ID"),
                "TreeMap 任意大小写查询应命中");

        // ★ 但 putAll 到 HashMap 后丢失特性
        Map<String, String> downgraded = new HashMap<>(ciHeaders);
        assertNull(downgraded.get("X-Trace-Id"),
                "new HashMap<>(treeMap) 后大小写不敏感特性丢失, X-Trace-Id 查不到 (复现断点)");
        assertEquals("trace-lower-123", downgraded.get("x-trace-id"),
                "HashMap 只能用精确小写 key 查到");
    }
}
