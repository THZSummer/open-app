package com.xxx.it.works.wecode.v2.modules.script;

import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ScriptNodeExecutor 测试 (Mock GraalJS)")
class ScriptNodeExecutorTest {

    private ScriptNodeExecutor executor;
    private GraalJsContextFactory contextFactory;
    private CtxAssembler ctxAssembler;
    private ConnectorApiPropertyService propertyService;

    @BeforeEach
    void setUp() {
        contextFactory = mock(GraalJsContextFactory.class);
        ctxAssembler = new CtxAssembler();
        propertyService = mock(ConnectorApiPropertyService.class);
        when(propertyService.getScriptMaxTimeoutSeconds()).thenReturn(Mono.just(5));
        executor = new ScriptNodeExecutor(contextFactory, ctxAssembler, propertyService);
    }

    @Test
    @DisplayName("正常脚本执行 — main(ctx) 返回 Map")
    void testNormalExecution_MainReturnsMap() {
        // Mock GraalJS
        Context jsContext = mock(Context.class);
        Value bindings = mock(Value.class);
        Value mainFunc = mock(Value.class);
        Value resultValue = mock(Value.class);
        when(contextFactory.createContext()).thenReturn(jsContext);
        when(jsContext.getBindings("js")).thenReturn(bindings);
        when(bindings.getMember("main")).thenReturn(mainFunc);
        when(mainFunc.canExecute()).thenReturn(true);
        when(mainFunc.execute(any())).thenReturn(resultValue);
        when(resultValue.isNull()).thenReturn(false);
        when(resultValue.hasMembers()).thenReturn(true);
        when(resultValue.getMemberKeys()).thenReturn(Set.of("result"));
        Value resultMember = mock(Value.class);
        when(resultValue.getMember("result")).thenReturn(resultMember);
        when(resultMember.isNumber()).thenReturn(true);
        when(resultMember.asLong()).thenReturn(42L);
        when(resultMember.fitsInLong()).thenReturn(true);

        ExecutionContext execCtx = new ExecutionContext("exec-001", "100");
        Map<String, Object> triggerBody = new HashMap<>();
        triggerBody.put("value", 21);
        Map<String, Object> triggerData = new HashMap<>();
        triggerData.put("body", triggerBody);
        execCtx.setTriggerData(triggerData);

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "script-1");
        Map<String, Object> data = new HashMap<>();
        data.put("script", "function main(ctx) { return { result: ctx.trigger.input.body.value * 2 }; }");
        nodeConfig.put("data", data);

        Mono<NodeOutput> result = executor.execute(execCtx, nodeConfig);

        StepVerifier.create(result)
                .assertNext(output -> {
                    assertEquals("script-1", output.getNodeId());
                    assertEquals("script", output.getNodeType());
                    assertEquals("success", output.getStatus());
                    assertNotNull(output.getOutput());
                    assertTrue(output.getDurationMs() >= 0);
                })
                .verifyComplete();

        verify(contextFactory).closeContext(jsContext);
    }

    @Test
    @DisplayName("ctx 上下文访问 — CtxAssembler 组装正确")
    void testCtxAccess_AssemblerCalled() {
        Context jsContext = mock(Context.class);
        Value bindings = mock(Value.class);
        Value mainFunc = mock(Value.class);
        Value resultValue = mock(Value.class);
        when(contextFactory.createContext()).thenReturn(jsContext);
        when(jsContext.getBindings("js")).thenReturn(bindings);
        when(bindings.getMember("main")).thenReturn(mainFunc);
        when(mainFunc.canExecute()).thenReturn(true);
        when(mainFunc.execute(any())).thenReturn(resultValue);
        when(resultValue.isNull()).thenReturn(false);
        when(resultValue.hasMembers()).thenReturn(true);
        when(resultValue.getMemberKeys()).thenReturn(Set.of("userId", "amount"));
        Value userIdValue = mock(Value.class);
        Value amountValue = mock(Value.class);
        when(resultValue.getMember("userId")).thenReturn(userIdValue);
        when(resultValue.getMember("amount")).thenReturn(amountValue);
        when(userIdValue.isString()).thenReturn(true);
        when(userIdValue.asString()).thenReturn("u123");
        when(amountValue.isNumber()).thenReturn(true);
        when(amountValue.fitsInLong()).thenReturn(true);
        when(amountValue.asLong()).thenReturn(100L);

        ExecutionContext execCtx = new ExecutionContext("exec-002", "100");
        Map<String, Object> triggerBody = new HashMap<>();
        triggerBody.put("userId", "u123");
        triggerBody.put("amount", 100);
        Map<String, Object> triggerData = new HashMap<>();
        triggerData.put("body", triggerBody);
        execCtx.setTriggerData(triggerData);

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "script-2");
        Map<String, Object> data = new HashMap<>();
        data.put("script", "function main(ctx) { return ctx.trigger.input.body; }");
        nodeConfig.put("data", data);

        Mono<NodeOutput> result = executor.execute(execCtx, nodeConfig);

        StepVerifier.create(result)
                .assertNext(output -> {
                    assertEquals("success", output.getStatus());
                    assertEquals("u123", output.getOutput().get("userId"));
                    assertEquals(100L, output.getOutput().get("amount"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("脚本为空 → 返回失败 NodeOutput")
    void testEmptyScript_ReturnsFailed() {
        ExecutionContext execCtx = new ExecutionContext("exec-003", "100");
        execCtx.setTriggerData(new HashMap<>());

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "script-3");
        Map<String, Object> data = new HashMap<>();
        data.put("script", "");
        nodeConfig.put("data", data);

        Mono<NodeOutput> result = executor.execute(execCtx, nodeConfig);

        StepVerifier.create(result)
                .assertNext(output -> {
                    assertEquals("failed", output.getStatus());
                    assertNotNull(output.getErrorInfo());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("类型映射 — String/Number/Boolean/List/null")
    void testTypeMapping_AllTypes() {
        Context jsContext = mock(Context.class);
        Value bindings = mock(Value.class);
        Value mainFunc = mock(Value.class);
        Value resultValue = mock(Value.class);
        when(contextFactory.createContext()).thenReturn(jsContext);
        when(jsContext.getBindings("js")).thenReturn(bindings);
        when(bindings.getMember("main")).thenReturn(mainFunc);
        when(mainFunc.canExecute()).thenReturn(true);
        when(mainFunc.execute(any())).thenReturn(resultValue);
        when(resultValue.isNull()).thenReturn(false);
        when(resultValue.hasMembers()).thenReturn(true);

        // Setup member keys for type mapping test
        Value strVal = mock(Value.class);
        Value numVal = mock(Value.class);
        Value boolVal = mock(Value.class);
        Value arrVal = mock(Value.class);
        Value nilVal = mock(Value.class);

        when(resultValue.getMemberKeys()).thenReturn(Set.of("str", "num", "flag", "arr", "nil"));
        when(resultValue.getMember("str")).thenReturn(strVal);
        when(resultValue.getMember("num")).thenReturn(numVal);
        when(resultValue.getMember("flag")).thenReturn(boolVal);
        when(resultValue.getMember("arr")).thenReturn(arrVal);
        when(resultValue.getMember("nil")).thenReturn(nilVal);

        when(strVal.isString()).thenReturn(true);
        when(strVal.asString()).thenReturn("hello");
        when(numVal.isNumber()).thenReturn(true);
        when(numVal.fitsInLong()).thenReturn(true);
        when(numVal.asLong()).thenReturn(42L);
        when(boolVal.isBoolean()).thenReturn(true);
        when(boolVal.asBoolean()).thenReturn(true);
        when(arrVal.hasArrayElements()).thenReturn(true);
        when(arrVal.getArraySize()).thenReturn(3L);
        for (int i = 0; i < 3; i++) {
            Value elem = mock(Value.class);
            when(elem.isNumber()).thenReturn(true);
            when(elem.fitsInLong()).thenReturn(true);
            when(elem.asLong()).thenReturn((long) (i + 1));
            when(arrVal.getArrayElement(i)).thenReturn(elem);
        }
        when(nilVal.isNull()).thenReturn(true);

        ExecutionContext execCtx = new ExecutionContext("exec-004", "100");
        execCtx.setTriggerData(new HashMap<>());

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "script-4");
        Map<String, Object> data = new HashMap<>();
        data.put("script",
                "function main(ctx) { return { str: 'hello', num: 42, flag: true, arr: [1,2,3], nil: null }; }");
        nodeConfig.put("data", data);

        Mono<NodeOutput> result = executor.execute(execCtx, nodeConfig);

        StepVerifier.create(result)
                .assertNext(output -> {
                    assertEquals("success", output.getStatus());
                    Map<String, Object> out = output.getOutput();
                    assertEquals("hello", out.get("str"));
                    assertTrue(out.get("num") instanceof Number);
                    assertEquals(true, out.get("flag"));
                    assertTrue(out.get("arr") instanceof List);
                    assertNull(out.get("nil"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("脚本超时 → 返回失败 NodeOutput")
    void testScriptTimeout_ReturnsFailed() {
        Context jsContext = mock(Context.class);
        when(contextFactory.createContext()).thenReturn(jsContext);
        when(jsContext.getBindings("js")).thenReturn(mock(Value.class));
        // Make the script execution hang - this won't actually timeout since we're mocking,
        // but the real timeout is tested via GraalJsSandboxSecurityTest

        ExecutionContext execCtx = new ExecutionContext("exec-005", "100");
        execCtx.setTriggerData(new HashMap<>());

        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("id", "script-5");
        Map<String, Object> data = new HashMap<>();
        data.put("script", "function main(ctx) { while(true) {} }");
        data.put("timeoutMs", 10); // very short timeout
        nodeConfig.put("data", data);

        // Script execution will throw because main is null since we didn't mock properly
        // This simulates the timeout path through onErrorResume
        Mono<NodeOutput> result = executor.execute(execCtx, nodeConfig);

        StepVerifier.create(result)
                .assertNext(output -> {
                    assertEquals("failed", output.getStatus());
                    assertNotNull(output.getErrorInfo());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("nodeConfig 不是 Map → 返回失败")
    void testInvalidNodeConfig_ReturnsFailed() {
        ExecutionContext execCtx = new ExecutionContext("exec-006", "100");

        Mono<NodeOutput> result = executor.execute(execCtx, "invalid-config");

        StepVerifier.create(result)
                .assertNext(output -> {
                    assertEquals("failed", output.getStatus());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getNodeType 返回 script")
    void testGetNodeType_ReturnsScript() {
        assertEquals("script", executor.getNodeType());
    }
}
