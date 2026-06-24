package com.xxx.it.works.wecode.v2.modules.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CacheKeyResolver 测试")
class CacheKeyResolverTest {

    private CacheKeyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CacheKeyResolver();
    }

    @Test
    @DisplayName("keyTemplate 为 null → 返回 _default")
    void testNullTemplate_ReturnsDefault() {
        assertEquals(CacheKeyResolver.DEFAULT_KEY, resolver.resolveKey(null, Map.of()));
    }

    @Test
    @DisplayName("keyTemplate 为空字符串 → 返回 _default")
    void testEmptyTemplate_ReturnsDefault() {
        assertEquals(CacheKeyResolver.DEFAULT_KEY, resolver.resolveKey("", Map.of()));
    }

    @Test
    @DisplayName("模板解析 — 单个 ${} 变量替换为实际值")
    void testSingleVariable_Resolved() {
        String template = "user_${$.trigger.input.body.userId}";
        Map<String, Object> context = new HashMap<>();
        context.put("$.trigger.input.body.userId", "u123");

        assertEquals("user_u123", resolver.resolveKey(template, context));
    }

    @Test
    @DisplayName("模板解析 — 多个 ${} 变量同时替换")
    void testMultipleVariables_Resolved() {
        String template = "flow_${$.flowId}_user_${$.trigger.input.body.userId}";
        Map<String, Object> context = new HashMap<>();
        context.put("$.flowId", 100L);
        context.put("$.trigger.input.body.userId", "u456");

        assertEquals("flow_100_user_u456", resolver.resolveKey(template, context));
    }

    @Test
    @DisplayName("模板变量不存在 → 保留路径去除 ${}")
    void testVariableNotFound_KeepsStrippedPath() {
        String template = "user_${$.trigger.input.body.userId}";
        Map<String, Object> context = new HashMap<>();
        context.put("$.other", "other-value");

        String result = resolver.resolveKey(template, context);
        assertEquals("user_$.trigger.input.body.userId", result);
    }

    @Test
    @DisplayName("上下文为 null → 返回去除 ${} 的第一条表达式")
    void testNullContext_ReturnsStripped() {
        String template = "${$.var}_suffix";
        assertEquals("$.var", resolver.resolveKey(template, null));
    }
}
