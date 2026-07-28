package com.xxx.it.works.wecode.v2.modules.runtime.expression;

import com.xxx.it.works.wecode.v2.modules.runtime.context.NodeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExpressionResolver 解析逻辑单元测试.
 * <p>
 * 重点覆盖 HTTP header 大小写敏感性场景: 当 header 由大小写敏感的 HashMap 承载时,
 * Nginx 网关小写化 header name 会导致表达式引用原始大小写时查不到 (生产 bug).
 * </p>
 */
@DisplayName("ExpressionResolver 解析逻辑测试")
class ExpressionResolverTest {

    private ExpressionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ExpressionResolver();
    }

    /** 构建带 input 的 NodeContext */
    private NodeContext nodeWithInput(Map<String, Object> input) {
        NodeContext ctx = new NodeContext();
        ctx.setNodeId("n1");
        ctx.setNodeType("trigger");
        ctx.setInput(input);
        ctx.setOutput(new HashMap<>());
        ctx.setStatus("success");
        return ctx;
    }

    @Nested
    @DisplayName("非表达式 / 常量")
    class ConstantAndPassthrough {

        @Test
        @DisplayName("null 表达式返回 null")
        void nullExpression() {
            assertNull(resolver.resolve(null, Map.of()));
        }

        @Test
        @DisplayName("非 ${...} 字符串原样返回")
        void plainString() {
            assertEquals("hello", resolver.resolve("hello", Map.of()));
        }

        @Test
        @DisplayName("常量表达式 ${$.constant:xxx} 返回字面值")
        void constant() {
            assertEquals("hello", resolver.resolve("${$.constant:hello}", Map.of()));
        }

        @Test
        @DisplayName("常量表达式 ${$.constant:} 返回空字符串")
        void constantEmpty() {
            assertEquals("", resolver.resolve("${$.constant:}", Map.of()));
        }
    }

    @Nested
    @DisplayName("节点引用 - 基本解析")
    class NodeReference {

        @Test
        @DisplayName("${$.node.n1.output.name} 精确匹配命中")
        void outputExactMatch() {
            NodeContext ctx = nodeWithInput(new HashMap<>());
            ctx.setOutput(Map.of("name", "Alice"));
            assertEquals("Alice",
                    resolver.resolve("${$.node.n1.output.name}", Map.of("n1", ctx)));
        }

        @Test
        @DisplayName("嵌套字段路径 user.name.first")
        void nestedField() {
            Map<String, Object> input = new HashMap<>();
            Map<String, Object> user = new HashMap<>();
            user.put("name", Map.of("first", "Bob"));
            input.put("user", user);
            NodeContext ctx = nodeWithInput(input);
            assertEquals("Bob",
                    resolver.resolve("${$.node.n1.input.user.name.first}", Map.of("n1", ctx)));
        }

        @Test
        @DisplayName("数组索引 items[0].name (已知 items[n] 解析 bug, 当前返回 null)")
        void listIndex() {
            // 已知问题 (不在本次 header 大小写修复范围): resolveNestedField 用 split(".", 2)
            // 切分 "items[0].name" 得到 ["items[0]", "name"], 随后 data.get("items[0]") 失败
            // (实际 key 是 "items"). 详见 test_trigger_invoke.py IT-060 注释.
            Map<String, Object> input = new HashMap<>();
            input.put("items", List.of(
                    Map.of("name", "first"),
                    Map.of("name", "second")));
            NodeContext ctx = nodeWithInput(input);
            // 当前行为: 返回 null (已知 bug)
            assertNull(resolver.resolve("${$.node.n1.input.items[0].name}", Map.of("n1", ctx)),
                    "已知 items[n] 解析 bug: split 破坏括号语法, 当前返回 null");
        }
    }

    @Nested
    @DisplayName("HTTP header 大小写敏感性 (核心问题)")
    class HeaderCaseSensitivity {

        /**
         * v5.9 修复验证: 即使 header 由大小写敏感 HashMap 承载,
         * resolveNestedField 的大小写不敏感回退确保表达式以原始大小写引用仍能命中.
         * 标准环境 Nginx 将 header name 小写化 (content-type), 表达式仍用 Content-Type 引用.
         */
        @Test
        @DisplayName("[修复验证] header 用 HashMap 承载, key 大小写不匹配 -> v5.9 回退查找命中")
        void headerCaseMismatch_HashMap_resolvesWithFallback() {
            // 模拟标准环境: Nginx 小写化后的 header, 用普通 HashMap 承载
            Map<String, Object> input = new HashMap<>();
            Map<String, Object> headerPart = new HashMap<>();   // 大小写敏感
            headerPart.put("content-type", "application/json"); // 小写 key
            input.put("header", headerPart);
            NodeContext ctx = nodeWithInput(input);

            // 表达式用原始大小写引用 — v5.9 大小写不敏感回退应命中
            Object result = resolver.resolve(
                    "${$.node.n1.input.header.Content-Type}", Map.of("n1", ctx));

            assertEquals("application/json", result,
                    "v5.9 getCaseInsensitive 回退: content-type 应被 Content-Type 匹配");
        }

        /**
         * 验证修复方向: 若 header 由大小写不敏感 Map (LinkedCaseInsensitiveMap) 承载,
         * ExpressionResolver 无需任何改动即可正确解析任意大小写引用.
         * 该用例在源头 Map 改为 LinkedCaseInsensitiveMap 后即通过.
         */
        @Test
        @DisplayName("[修复目标] header 用 LinkedCaseInsensitiveMap 承载, 任意大小写引用均可命中")
        void headerCaseMismatch_CaseInsensitiveMap_resolves() {
            // 模拟修复后: header 用大小写不敏感 Map 承载
            Map<String, Object> input = new HashMap<>();
            Map<String, Object> headerPart = new LinkedCaseInsensitiveMap<>();
            headerPart.put("content-type", "application/json"); // 存入小写 key
            input.put("header", headerPart);
            NodeContext ctx = nodeWithInput(input);
            Map<String, NodeContext> contexts = Map.of("n1", ctx);

            // 任意大小写引用都应命中
            assertEquals("application/json",
                    resolver.resolve("${$.node.n1.input.header.Content-Type}", contexts),
                    "PascalCase 引用应命中");
            assertEquals("application/json",
                    resolver.resolve("${$.node.n1.input.header.CONTENT-TYPE}", contexts),
                    "UPPERCASE 引用应命中");
            assertEquals("application/json",
                    resolver.resolve("${$.node.n1.input.header.content-type}", contexts),
                    "lowercase 引用应命中");
        }

        @Test
        @DisplayName("[对照] header 用 TreeMap(CASE_INSENSITIVE_ORDER) 承载同样可命中")
        void headerCaseMismatch_TreeMap_resolves() {
            Map<String, Object> input = new HashMap<>();
            Map<String, Object> headerPart = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            headerPart.put("content-type", "application/json");
            input.put("header", headerPart);
            NodeContext ctx = nodeWithInput(input);

            assertEquals("application/json",
                    resolver.resolve("${$.node.n1.input.header.Content-Type}", Map.of("n1", ctx)));
        }

        @Test
        @DisplayName("[基准] header key 大小写完全一致时 HashMap 也能命中 (开发环境行为)")
        void headerCaseMatch_HashMap_resolves() {
            // 模拟开发环境: 无 Nginx, header name 保留原始大小写
            Map<String, Object> input = new HashMap<>();
            Map<String, Object> headerPart = new HashMap<>();
            headerPart.put("Content-Type", "application/json"); // 原始大小写 key
            input.put("header", headerPart);
            NodeContext ctx = nodeWithInput(input);

            assertEquals("application/json",
                    resolver.resolve("${$.node.n1.input.header.Content-Type}", Map.of("n1", ctx)),
                    "开发环境 key 大小写一致, HashMap 也能命中");
        }
    }

    @Nested
    @DisplayName("错误分支")
    class ErrorBranches {

        @Test
        @DisplayName("未知表达式格式返回 null")
        void unknownFormat() {
            assertNull(resolver.resolve("${unknown}", Map.of()));
        }

        @Test
        @DisplayName("节点不存在返回 null")
        void nodeNotFound() {
            assertNull(resolver.resolve("${$.node.missing.output.x}", Map.of()));
        }

        @Test
        @DisplayName("非法分区名返回 null")
        void invalidPartition() {
            NodeContext ctx = nodeWithInput(new HashMap<>());
            assertNull(resolver.resolve("${$.node.n1.result.x}", Map.of("n1", ctx)));
        }

        @Test
        @DisplayName("缺少 fieldPath 返回 null")
        void missingFieldPath() {
            NodeContext ctx = nodeWithInput(new HashMap<>());
            assertNull(resolver.resolve("${$.node.n1.output}", Map.of("n1", ctx)));
        }
    }
}
