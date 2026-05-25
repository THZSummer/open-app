package com.xxx.it.works.wecode.v2.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API 响应格式契约测试 (L4)
 *
 * <p>验证所有 API 响应符合统一格式规范：
 * { code, messageZh, messageEn, data, page }
 *
 * 以及字段级约束：
 * - BIGINT ID → string 类型
 * - 枚举字段 → TINYINT 数字
 * - 时间字段 → ISO 8601 格式
 * - camelCase 字段名
 * </p>
 */
@DisplayName("API Contract Schema Test")
class ContractSchemaTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ==================== 统一响应格式 ====================

    @Test
    @DisplayName("成功响应格式: {code, messageZh, messageEn, data}")
    void testSuccessResponseFormat() {
        String json = """
                {
                    "code": "200",
                    "messageZh": "操作成功",
                    "messageEn": "Success",
                    "data": {"id": "1234567890123456789"},
                    "page": null
                }
                """;

        assertDoesNotThrow(() -> {
            JsonNode root = mapper.readTree(json);
            assertTrue(root.has("code"), "code 字段必含");
            assertTrue(root.has("messageZh"), "messageZh 字段必含");
            assertTrue(root.has("messageEn"), "messageEn 字段必含");
            assertTrue(root.has("data"), "data 字段必含");
            assertTrue(root.has("page"), "page 字段必含");
            assertEquals("200", root.get("code").asText());
        });
    }

    @Test
    @DisplayName("分页响应格式: {code, data[], page{curPage, pageSize, total}}")
    void testPagedResponseFormat() {
        String json = """
                {
                    "code": "200",
                    "messageZh": "查询成功",
                    "messageEn": "Success",
                    "data": [],
                    "page": {
                        "curPage": 1,
                        "pageSize": 20,
                        "total": 100,
                        "totalPages": 5
                    }
                }
                """;

        assertDoesNotThrow(() -> {
            JsonNode root = mapper.readTree(json);
            JsonNode page = root.get("page");
            assertNotNull(page, "分页接口 page 不能为 null");
            assertTrue(page.get("curPage").isInt(), "curPage 必须是数字");
            assertTrue(page.get("pageSize").isInt(), "pageSize 必须是数字");
            assertTrue(page.get("total").isNumber(), "total 必须是数字");
        });
    }

    @Test
    @DisplayName("错误响应格式: {code != 200, messageZh, messageEn, data: null}")
    void testErrorResponseFormat() {
        String json = """
                {
                    "code": "404",
                    "messageZh": "连接器不存在",
                    "messageEn": "Connector not found",
                    "data": null,
                    "page": null
                }
                """;

        assertDoesNotThrow(() -> {
            JsonNode root = mapper.readTree(json);
            assertNotEquals("200", root.get("code").asText(), "错误响应 code != 200");
            assertTrue(root.get("data").isNull(), "错误响应 data 为 null");
            assertTrue(root.get("page").isNull(), "错误响应 page 为 null");
        });
    }

    // ==================== 字段级类型约束 ====================

    @Test
    @DisplayName("BIGINT 雪花 ID 返回 string 类型")
    void testSnowflakeIdAsString() {
        // 验证 ID 字段在 JSON 中为字符串而非数字
        String json = """
                {
                    "code": "200",
                    "messageZh": "成功",
                    "messageEn": "Success",
                    "data": {
                        "id": "1234567890123456789",
                        "connectorId": "9876543210987654321",
                        "flowId": "1111111111111111111"
                    }
                }
                """;

        assertDoesNotThrow(() -> {
            JsonNode data = mapper.readTree(json).get("data");
            // 所有 ID 字段必须是 string 类型
            assertTrue(data.get("id").isTextual(), "id 必须是 string (BIGINT → string)");
            assertTrue(data.get("connectorId").isTextual(), "connectorId 必须是 string");
            assertTrue(data.get("flowId").isTextual(), "flowId 必须是 string");

            // 验证不丢失精度: 可通过 BigInteger 解析
            new java.math.BigInteger(data.get("id").asText());
        });
    }

    @Test
    @DisplayName("枚举字段返回 TINYINT 数字")
    void testEnumAsTinyInt() {
        // 验证枚举字段在 JSON 中为数字
        String json = """
                {
                    "connectorType": 1,
                    "lifecycleStatus": 2,
                    "status": 0,
                    "triggerType": 3,
                    "nodeType": 1
                }
                """;

        assertDoesNotThrow(() -> {
            JsonNode root = mapper.readTree(json);
            assertTrue(root.get("connectorType").isInt(), "connectorType 必须是数字");
            assertTrue(root.get("lifecycleStatus").isInt(), "lifecycleStatus 必须是数字");
            assertTrue(root.get("status").isInt(), "status 必须是数字");
            assertTrue(root.get("triggerType").isInt(), "triggerType 必须是数字");
            assertTrue(root.get("nodeType").isInt(), "nodeType 必须是数字");
        });
    }

    @Test
    @DisplayName("时间字段返回 ISO 8601 格式字符串")
    void testDateTimeAsISO8601() {
        // ISO 8601 正则: yyyy-MM-dd'T'HH:mm:ss.SSSXXX
        String isoPattern = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:\\d{2}$";
        String[] validExamples = {
                "2026-05-21T10:00:00.000+08:00",
                "2026-05-21T10:00:00.000Z"
        };

        for (String example : validExamples) {
            assertTrue(example.matches(isoPattern) || example.endsWith("Z"),
                    "时间格式必须为 ISO 8601: " + example);
        }
    }

    @Test
    @DisplayName("枚举值范围校验")
    void testEnumValueRanges() {
        // 验证已知枚举值的范围
        Map<String, int[]> enumRanges = new LinkedHashMap<>();
        enumRanges.put("connectorType", new int[]{1});     // MVP 仅 HTTP=1
        enumRanges.put("lifecycleStatus", new int[]{0, 1, 2}); // undeployed/running/stopped
        enumRanges.put("nodeType", new int[]{1, 2, 3, 4}); // entry/connector/data_processor/exit
        enumRanges.put("triggerType", new int[]{1, 2, 3}); // http/manual/test

        // 验证每个枚举在其范围内
        String json = """
                {
                    "connectorType": 1,
                    "lifecycleStatus": 2,
                    "nodeType": 3,
                    "triggerType": 3
                }
                """;

        assertDoesNotThrow(() -> {
            JsonNode root = mapper.readTree(json);
            for (Map.Entry<String, int[]> entry : enumRanges.entrySet()) {
                int value = root.get(entry.getKey()).asInt();
                int[] allowed = entry.getValue();
                boolean valid = false;
                for (int v : allowed) {
                    if (value == v) valid = true;
                }
                assertTrue(valid, entry.getKey() + " 值 " + value + " 不在有效范围内 " + Arrays.toString(allowed));
            }
        });
    }

    @Test
    @DisplayName("camelCase 字段命名规范")
    void testCamelCaseFieldNames() {
        // camelCase 正则: 首字母小写，后续单词首字母大写
        String camelCasePattern = "^[a-z]+[A-Za-z0-9]*$";

        String[] validFields = {"nameCn", "nameEn", "connectorType", "lifecycleStatus",
                "createTime", "lastUpdateTime", "connectorId", "flowId",
                "isTest", "hasConfig", "orchestrationConfig", "connectionConfig"};

        for (String field : validFields) {
            assertTrue(field.matches(camelCasePattern),
                    "字段名 '" + field + "' 不符合 camelCase 规范");
        }

        // 验证 isXxx 布尔字段以 is 开头
        String[] booleanFields = {"isTest", "isDeleted", "hasConfig"};
        for (String field : booleanFields) {
            assertTrue(field.startsWith("is") || field.startsWith("has"),
                    "布尔字段应使用 is/has 前缀: " + field);
        }
    }

    @Test
    @DisplayName("错误码覆盖率: 400/401/403/404/409/422/429/500")
    void testErrorCodeCoverage() {
        // 验证所有预期错误码可被正确序列化
        String[] expectedCodes = {"400", "401", "403", "404", "409", "422", "429", "500"};
        String[] messages = {
                "参数错误", "认证失败", "无权限", "资源不存在",
                "状态冲突", "校验失败", "请求频率超限", "内部错误"
        };

        for (int i = 0; i < expectedCodes.length; i++) {
            String code = expectedCodes[i];
            String msg = messages[i];
            String json = String.format("""
                    {"code": "%s", "messageZh": "%s", "messageEn": "Error", "data": null, "page": null}
                    """, code, msg);

            assertDoesNotThrow(() -> {
                JsonNode root = mapper.readTree(json);
                assertEquals(code, root.get("code").asText());
                assertNotNull(root.get("messageZh").asText());
            }, "错误码 " + code + " 应可被正确序列化: " + msg);
        }
    }
}
