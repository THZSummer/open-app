package com.xxx.it.works.wecode.v2.modules.flow.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.config.ConnectorPlatformPropertyService;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorVersionStatus;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersion;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersionRef;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.ConnectorVersionRefMapper;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.OpConnectorVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlowPublishValidator 测试")
class FlowPublishValidatorTest {

    @Mock
    private ConnectorVersionRefMapper connectorVersionRefMapper;

    @Mock
    private OpConnectorVersionMapper connectorVersionMapper;

    @Mock
    private ConnectorPlatformPropertyService propertyService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private FlowPublishValidator validator;

    private static final String TEST_APP_ID = "test_app";

    @BeforeEach
    void setUp() {
        // Set up default property values matching the old hardcoded constants
        // so existing tests pass without changes to assertion values
        lenient().when(propertyService.getFlowMaxCacheTtlSeconds(anyString())).thenReturn(1296000);
        lenient().when(propertyService.getFlowMaxParallelBranches(anyString())).thenReturn(8);
        lenient().when(propertyService.getScriptMaxLengthChars(anyString())).thenReturn(10000);
        lenient().when(propertyService.getScriptMaxTimeoutSeconds(anyString())).thenReturn(30);
        lenient().when(propertyService.getFlowConfigMaxBytes(anyString())).thenReturn(0); // 0 = not enforced
    }

    // ===== 校验 1: 业务必填字段 =====

    @Nested
    @DisplayName("校验1: 业务必填字段")
    class ValidateBusinessFieldsTest {

        @Test
        @DisplayName("nameCn 和 nameEn 都有值 → 通过")
        void testBothFieldsPresent_Pass() {
            List<String> errors = validator.validateBusinessFields("测试流", "test_flow");
            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("nameCn 为空 → 失败")
        void testNameCnEmpty_Fail() {
            List<String> errors = validator.validateBusinessFields(null, "test_flow");
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).contains("中文名称"));
        }

        @Test
        @DisplayName("nameEn 为空 → 失败")
        void testNameEnEmpty_Fail() {
            List<String> errors = validator.validateBusinessFields("测试流", null);
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).contains("英文名称"));
        }

        @Test
        @DisplayName("nameCn 和 nameEn 都为空 → 失败 2 条")
        void testBothFieldsEmpty_Fail() {
            List<String> errors = validator.validateBusinessFields("", "");
            assertEquals(2, errors.size());
        }
    }

    // ===== 校验 2/8: 编排配置非空 + JSON 语法 =====

    @Nested
    @DisplayName("校验2/8: 编排配置校验")
    class ValidateOrchestrationConfigTest {

        @Test
        @DisplayName("合法编排配置含 connector 节点 → 通过")
        void testValidConfigWithConnector_Pass() {
            String config = "{\"nodes\":[" +
                    "{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}}," +
                    "{\"id\":\"c\",\"type\":\"connector\",\"data\":{\"type\":\"connector\"}}," +
                    "{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}" +
                    "],\"edges\":[" +
                    "{\"id\":\"e1\",\"source\":\"t\",\"target\":\"c\"}," +
                    "{\"id\":\"e2\",\"source\":\"c\",\"target\":\"e\"}" +
                    "],\"flowConfig\":{\"flowMode\":\"serial\"}}";
            List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
            assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
        }

        @Test
        @DisplayName("非法 JSON → 失败")
        void testInvalidJson_Fail() {
            String config = "{ invalid json";
            List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).contains("JSON 格式无效"));
        }

        @Test
        @DisplayName("无节点 → 失败")
        void testNoNodes_Fail() {
            String config = "{\"nodes\":[],\"edges\":[],\"flowConfig\":{\"flowMode\":\"serial\"}}";
            List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).contains("至少一个节点"));
        }

        @Test
        @DisplayName("仅有 trigger/exit 无业务节点 → 失败")
        void testOnlyTriggerExit_Fail() {
            String config = "{\"nodes\":[" +
                    "{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}}," +
                    "{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}" +
                    "],\"edges\":[" +
                    "{\"id\":\"e1\",\"source\":\"t\",\"target\":\"e\"}" +
                    "],\"flowConfig\":{\"flowMode\":\"serial\"}}";
            List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).contains("业务节点"));
        }
    }

    // ===== 校验 5: 缓存 TTL 上限 =====

    @Nested
    @DisplayName("校验5: 缓存 TTL 上限")
    class ValidateCacheTTLTest {

        @Test
        @DisplayName("缓存 TTL ≤ 1296000 → 通过")
        void testCacheTtlUnderLimit_Pass() {
            String config = "{\"nodes\":[{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}},{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}]," +
                    "\"edges\":[]," +
                    "\"flowConfig\":{\"flowMode\":\"serial\",\"cache\":{\"ttl\":600}}}";
            // 无 business node 会报一个错，但 cache ttl 不报错
            List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
            // 只有 "业务节点" 一个错误，cache ttl 600 不报错
            assertTrue(errors.stream().noneMatch(e -> e.contains("缓存 TTL")));
        }

        @Test
        @DisplayName("缓存 TTL > 1296000 → 失败")
        void testCacheTtlExceeded_Fail() {
            String config = "{\"nodes\":[{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}},{\"id\":\"c\",\"type\":\"connector\",\"data\":{\"type\":\"connector\"}},{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}]," +
                    "\"edges\":[{\"id\":\"e1\",\"source\":\"t\",\"target\":\"c\"},{\"id\":\"e2\",\"source\":\"c\",\"target\":\"e\"}]," +
                    "\"flowConfig\":{\"flowMode\":\"serial\",\"cache\":{\"ttl\":2000000}}}";
            List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
            assertTrue(errors.stream().anyMatch(e -> e.contains("缓存 TTL 超过上限")));
        }

        @Test
        @DisplayName("缓存 TTL < 最小值 → 失败")
        void testCacheTtlTooLow_Fail() {
            String config = "{\"nodes\":[{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}},{\"id\":\"c\",\"type\":\"connector\",\"data\":{\"type\":\"connector\"}},{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}]," +
                    "\"edges\":[{\"id\":\"e1\",\"source\":\"t\",\"target\":\"c\"},{\"id\":\"e2\",\"source\":\"c\",\"target\":\"e\"}]," +
                    "\"flowConfig\":{\"flowMode\":\"serial\",\"cache\":{\"ttl\":0}}}";
            List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
            assertTrue(errors.stream().anyMatch(e -> e.contains("缓存 TTL 必须 ≥")));
        }
    }

    // ===== 校验 6: 并行分支数上限 =====

    @Nested
    @DisplayName("校验6: 并行分支数上限")
    class ValidateParallelBranchesTest {

        @Test
        @DisplayName("并行分支数 ≤ 8 → 通过")
        void testParallelBranchesUnderLimit_Pass() {
            String config = "{\"nodes\":[" +
                    "{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}}," +
                    "{\"id\":\"c\",\"type\":\"connector\",\"data\":{\"type\":\"connector\"}}," +
                    "{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}" +
                    "],\"edges\":[" +
                    "{\"id\":\"e1\",\"source\":\"t\",\"target\":\"c\"}," +
                    "{\"id\":\"e2\",\"source\":\"c\",\"target\":\"e\"}" +
                    "],\"flowConfig\":{\"flowMode\":\"serial\"}}";
            List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
            assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
        }

        @Test
        @DisplayName("并行分支超过 8 → 失败")
        void testParallelBranchesExceeded_Fail() {
            StringBuilder edges = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                if (i > 0) edges.append(",");
                edges.append("{\"id\":\"pe").append(i).append("\",\"source\":\"t\",\"target\":\"e\",\"data\":{\"connectionMode\":\"parallel\"}}");
            }
            String config = "{\"nodes\":[" +
                    "{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}}," +
                    "{\"id\":\"c\",\"type\":\"connector\",\"data\":{\"type\":\"connector\"}}," +
                    "{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}" +
                    "],\"edges\":[" + edges + "],\"flowConfig\":{\"flowMode\":\"serial\"}}";
            List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
            assertTrue(errors.stream().anyMatch(e -> e.contains("并行分支数超过上限")));
        }
    }

    // ===== 校验 3: 限流上限 =====

    @Test
    @DisplayName("校验3: QPS 超过应用上限 → 失败")
    void testRateLimitQpsExceeded_Fail() {
        String config = "{\"nodes\":[{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}},{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}]," +
                "\"edges\":[]," +
                "\"flowConfig\":{\"flowMode\":\"serial\",\"rateLimitConfig\":{\"maxQps\":200}}}";
        List<String> errors = validator.validateRateLimitAgainstAppMax(config, 100, 50);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("QPS"));
    }

    @Test
    @DisplayName("校验3: QPS ≤ 应用上限 → 通过")
    void testRateLimitQpsUnderLimit_Pass() {
        String config = "{\"nodes\":[],\"edges\":[],\"flowConfig\":{\"flowMode\":\"serial\",\"rateLimitConfig\":{\"maxQps\":50}}}";
        List<String> errors = validator.validateRateLimitAgainstAppMax(config, 100, 50);
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("校验3: 并发超过应用上限 → 失败")
    void testConcurrencyExceeded_Fail() {
        String config = "{\"nodes\":[],\"edges\":[],\"flowConfig\":{\"flowMode\":\"serial\",\"rateLimitConfig\":{\"maxConcurrency\":60}}}";
        List<String> errors = validator.validateRateLimitAgainstAppMax(config, 100, 50);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("并发"));
    }

    // ===== 校验 4: 超时上限 =====

    @Test
    @DisplayName("校验4: 节点超时超过应用上限 → 失败")
    void testTimeoutExceeded_Fail() {
        String config = "{\"nodes\":[{\"id\":\"c\",\"type\":\"connector\",\"data\":{\"type\":\"connector\",\"timeoutMs\":60000}}]," +
                "\"edges\":[]," +
                "\"flowConfig\":{\"flowMode\":\"serial\",\"timeout\":50000}}";
        List<String> errors = validator.validateTimeoutAgainstAppMax(config, 30000);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("超时")));
    }

    @Test
    @DisplayName("校验4: 超时 ≤ 应用上限 → 通过")
    void testTimeoutUnderLimit_Pass() {
        String config = "{\"nodes\":[],\"edges\":[],\"flowConfig\":{\"flowMode\":\"serial\",\"timeout\":20000}}";
        List<String> errors = validator.validateTimeoutAgainstAppMax(config, 30000);
        assertTrue(errors.isEmpty());
    }

    // ===== 校验 7: 连接器版本引用可用性 =====

    @Test
    @DisplayName("校验7: 无引用 → 通过")
    void testNoConnectorRefs_Pass() {
        when(connectorVersionRefMapper.selectByFlowVersionId(100L)).thenReturn(List.of());
        List<String> errors = validator.validateConnectorVersionRefs(100L);
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("校验7: 引用已发布连接器版本 → 通过")
    void testPublishedConnectorVersionRefs_Pass() {
        ConnectorVersionRef ref = new ConnectorVersionRef();
        ref.setNodeId("node_conn");
        ref.setConnectorVersionId(50L);

        ConnectorVersion cv = new ConnectorVersion();
        cv.setId(50L);
        cv.setStatus(ConnectorVersionStatus.PUBLISHED.getCode());

        when(connectorVersionRefMapper.selectByFlowVersionId(200L)).thenReturn(List.of(ref));
        when(connectorVersionMapper.selectById(50L)).thenReturn(cv);

        List<String> errors = validator.validateConnectorVersionRefs(200L);
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("校验7: 引用已失效连接器版本 → 失败")
    void testInvalidatedConnectorVersionRef_Fail() {
        ConnectorVersionRef ref = new ConnectorVersionRef();
        ref.setNodeId("node_conn");
        ref.setConnectorVersionId(50L);

        ConnectorVersion cv = new ConnectorVersion();
        cv.setId(50L);
        cv.setStatus(ConnectorVersionStatus.INVALIDATED.getCode());

        when(connectorVersionRefMapper.selectByFlowVersionId(300L)).thenReturn(List.of(ref));
        when(connectorVersionMapper.selectById(50L)).thenReturn(cv);

        List<String> errors = validator.validateConnectorVersionRefs(300L);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("不可用"));
    }

    @Test
    @DisplayName("校验7: 引用不存在的连接器版本 → 失败")
    void testConnectorVersionNotFound_Fail() {
        ConnectorVersionRef ref = new ConnectorVersionRef();
        ref.setNodeId("node_conn");
        ref.setConnectorVersionId(999L);

        when(connectorVersionRefMapper.selectByFlowVersionId(400L)).thenReturn(List.of(ref));
        when(connectorVersionMapper.selectById(999L)).thenReturn(null);

        List<String> errors = validator.validateConnectorVersionRefs(400L);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("不存在"));
    }

    // ===== 校验 9: 脚本节点 =====

    @Test
    @DisplayName("校验9: 脚本源码超长 → 失败")
    void testScriptSourceTooLong_Fail() {
        String longSource = "x".repeat(50001);
        String config = "{\"nodes\":[" +
                "{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}}," +
                "{\"id\":\"s\",\"type\":\"script\",\"data\":{\"type\":\"script\",\"script\":\"" + longSource + "\"}}," +
                "{\"id\":\"c\",\"type\":\"connector\",\"data\":{\"type\":\"connector\"}}," +
                "{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"t\",\"target\":\"s\"}," +
                "{\"id\":\"e2\",\"source\":\"s\",\"target\":\"c\"}," +
                "{\"id\":\"e3\",\"source\":\"c\",\"target\":\"e\"}" +
                "],\"flowConfig\":{\"flowMode\":\"serial\"}}";
        List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
        assertTrue(errors.stream().anyMatch(e -> e.contains("脚本源码超过最大长度")));
    }

    @Test
    @DisplayName("校验9: 脚本节点数超限 → 失败")
    void testTooManyScriptNodes_Fail() {
        StringBuilder nodes = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0) nodes.append(",");
            nodes.append("{\"id\":\"s").append(i).append("\",\"type\":\"script\",\"data\":{\"type\":\"script\"}}");
        }
        String config = "{\"nodes\":[" +
                "{\"id\":\"c\",\"type\":\"connector\",\"data\":{\"type\":\"connector\"}}," +
                nodes +
                "],\"edges\":[]}";
        List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
        assertTrue(errors.stream().anyMatch(e -> e.contains("脚本节点数量超过上限")));
    }

    @Test
    @DisplayName("校验9: 合法脚本语法 → 通过 (GraalJS parse)")
    void testValidScriptSyntax_Pass() {
        String validScript = "function main(ctx) { return { result: ctx.input.value + 1 }; }";
        String config = "{\"nodes\":[" +
                "{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}}," +
                "{\"id\":\"s\",\"type\":\"script\",\"data\":{\"type\":\"script\",\"script\":\"" + escapeJson(validScript) + "\"}}," +
                "{\"id\":\"c\",\"type\":\"connector\",\"data\":{\"type\":\"connector\"}}," +
                "{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"t\",\"target\":\"s\"}," +
                "{\"id\":\"e2\",\"source\":\"s\",\"target\":\"c\"}," +
                "{\"id\":\"e3\",\"source\":\"c\",\"target\":\"e\"}" +
                "],\"flowConfig\":{\"flowMode\":\"serial\"}}";
        List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
    }

    @Test
    @DisplayName("校验9: 非法脚本语法 → 失败 (GraalJS parse)")
    void testInvalidScriptSyntax_Fail() {
        String invalidScript = "function main(ctx) { return {; }"; // missing value after return
        String config = "{\"nodes\":[" +
                "{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}}," +
                "{\"id\":\"s\",\"type\":\"script\",\"data\":{\"type\":\"script\",\"script\":\"" + escapeJson(invalidScript) + "\"}}," +
                "{\"id\":\"c\",\"type\":\"connector\",\"data\":{\"type\":\"connector\"}}," +
                "{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"t\",\"target\":\"s\"}," +
                "{\"id\":\"e2\",\"source\":\"s\",\"target\":\"c\"}," +
                "{\"id\":\"e3\",\"source\":\"c\",\"target\":\"e\"}" +
                "],\"flowConfig\":{\"flowMode\":\"serial\"}}";
        List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
        assertTrue(errors.stream().anyMatch(e -> e.contains("语法错误")),
                "Expected syntax error but got: " + errors);
    }

    @Test
    @DisplayName("校验9: 空脚本源码 → 失败")
    void testEmptyScriptSource_Fail() {
        String config = "{\"nodes\":[" +
                "{\"id\":\"t\",\"type\":\"trigger\",\"data\":{\"type\":\"trigger\"}}," +
                "{\"id\":\"s\",\"type\":\"script\",\"data\":{\"type\":\"script\",\"script\":\"\"}}," +
                "{\"id\":\"c\",\"type\":\"connector\",\"data\":{\"type\":\"connector\"}}," +
                "{\"id\":\"e\",\"type\":\"exit\",\"data\":{\"type\":\"exit\"}}" +
                "],\"edges\":[" +
                "{\"id\":\"e1\",\"source\":\"t\",\"target\":\"s\"}," +
                "{\"id\":\"e2\",\"source\":\"s\",\"target\":\"c\"}," +
                "{\"id\":\"e3\",\"source\":\"c\",\"target\":\"e\"}" +
                "],\"flowConfig\":{\"flowMode\":\"serial\"}}";
        List<String> errors = validator.validateOrchestrationConfig(config, TEST_APP_ID);
        assertTrue(errors.stream().anyMatch(e -> e.contains("源码不能为空")),
                "Expected empty source error but got: " + errors);
    }

    /**
     * 转义 JSON 字符串中的特殊字符，避免拼接 JSON 时格式错误
     */
    private static String escapeJson(String raw) {
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
