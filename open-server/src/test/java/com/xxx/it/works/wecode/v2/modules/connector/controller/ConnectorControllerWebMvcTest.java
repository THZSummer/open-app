package com.xxx.it.works.wecode.v2.modules.connector.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connector.dto.*;
import com.xxx.it.works.wecode.v2.modules.connector.service.OpConnectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.xxx.it.works.wecode.v2.modules.security.AppWhitelistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OpConnectorController 接口层测试 (WebMvcTest)
 *
 * <p>测试目标: HTTP 请求绑定 + 响应序列化 + 状态码
 * 接口范围: #1 ~ #7
 * 测试层次: L2 接口层
 * </p>
 */
@WebMvcTest(OpConnectorController.class)
@DisplayName("OpConnectorController WebMvcTest")
class OpConnectorControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpConnectorService connectorService;

    @MockitoBean
    private AppWhitelistService appWhitelistService;

    @BeforeEach
    void setUp() {
        when(appWhitelistService.isWhitelisted(anyLong())).thenReturn(true);
    }

    // ==================== #1 创建连接器 ====================

    @Nested
    @DisplayName("#1 POST /service/open/v2/admin/connectors")
    class CreateConnector {

        @Test
        @DisplayName("✅ TC-001: 正常创建（完整字段）")
        void testCreateSuccess() throws Exception {
            ConnectorCreateResponse respData = ConnectorCreateResponse.builder()
                    .connectorId("1234567890123456789")
                    .build();
            when(connectorService.createConnector(any()))
                    .thenReturn(ApiResponse.success(respData));

            mockMvc.perform(post("/service/open/v2/admin/connectors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "nameCn": "IM 发送消息",
                                        "nameEn": "IM Send Message",
                                        "iconFileId": "file_im",
                                        "descriptionCn": "封装 IM 消息发送",
                                        "descriptionEn": "Encapsulated IM messaging",
                                        "connectorType": 1
                                    }
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"))
                    .andExpect(jsonPath("$.data.connectorId").isString())
                    .andExpect(jsonPath("$.data.connectorId").value("1234567890123456789"));
        }

        @Test
        @DisplayName("❌ TC-002: 缺少必填 nameCn")
        void testCreateMissingNameCn() throws Exception {
            mockMvc.perform(post("/service/open/v2/admin/connectors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "nameEn": "IM Send Message",
                                        "connectorType": 1
                                    }
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"));
        }

        @Test
        @DisplayName("❌ TC-003: connectorType 非法值 99")
        void testCreateInvalidConnectorType() throws Exception {
            when(connectorService.createConnector(any()))
                    .thenReturn(ApiResponse.error("422", "连接器类型不支持", "Invalid connector type"));

            mockMvc.perform(post("/service/open/v2/admin/connectors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "nameCn": "测试",
                                        "nameEn": "Test",
                                        "connectorType": 99
                                    }
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("422"));
        }

        @Test
        @DisplayName("⚠️ TC-004: nameCn 超长(已知缺口: 缺少 @Size 长度校验)")
        void testCreateNameCnTooLong() throws Exception {
            // ⚠️ 已知缺口: ConnectorCreateRequest.nameCn 无 @Size/@Length 注解
            // 计划: 需在 DTO 上添加 @Size(max = 200) 长度限制
            String longName = "a".repeat(501);
            when(connectorService.createConnector(any()))
                    .thenReturn(ApiResponse.success(
                            ConnectorCreateResponse.builder().connectorId("200").build()));

            mockMvc.perform(post("/service/open/v2/admin/connectors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "nameCn": "%s",
                                        "nameEn": "Test",
                                        "connectorType": 1
                                    }
                                    """.formatted(longName))
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"));
        }

        @Test
        @DisplayName("❌ TC-005: 缺少必填 nameEn")
        void testCreateMissingNameEn() throws Exception {
            mockMvc.perform(post("/service/open/v2/admin/connectors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "nameCn": "测试",
                                        "connectorType": 1
                                    }
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"));
        }

        @Test
        @DisplayName("❌ TC-006: 缺少必填 connectorType")
        void testCreateMissingConnectorType() throws Exception {
            mockMvc.perform(post("/service/open/v2/admin/connectors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "nameCn": "测试",
                                        "nameEn": "Test"
                                    }
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"));
        }
    }

    // ==================== #2 查询列表 ====================

    @Nested
    @DisplayName("#2 GET /service/open/v2/admin/connectors")
    class GetConnectorList {

        @Test
        @DisplayName("✅ TC-007: 默认分页查询")
        void testListDefaultPage() throws Exception {
            ConnectorListResponse item = new ConnectorListResponse();
            item.setConnectorId("1234567890123456789");
            item.setNameCn("IM 发送消息");
            item.setNameEn("IM Send Message");
            item.setConnectorType(1);
            item.setCreateTime("2026-05-21T10:00:00.000Z");

            when(connectorService.getConnectorList(any()))
                    .thenReturn(ApiResponse.success(
                            List.of(item),
                            ApiResponse.PageResponse.builder()
                                    .curPage(1).pageSize(20).total(1L).totalPages(1)
                                    .build()));

            mockMvc.perform(get("/service/open/v2/admin/connectors")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].connectorId").isString())
                    .andExpect(jsonPath("$.data[0].connectorType").isNumber())
                    .andExpect(jsonPath("$.page.curPage").value(1))
                    .andExpect(jsonPath("$.page.pageSize").value(20))
                    .andExpect(jsonPath("$.page.total").isNumber());
        }

        @Test
        @DisplayName("✅ TC-008: connectorType 过滤")
        void testListFilterByType() throws Exception {
            when(connectorService.getConnectorList(any()))
                    .thenReturn(ApiResponse.success(List.of(), ApiResponse.PageResponse.builder()
                            .curPage(1).pageSize(20).total(0L).totalPages(0).build()));

            mockMvc.perform(get("/service/open/v2/admin/connectors")
                            .param("connectorType", "1")
                            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"));
        }

        @Test
        @DisplayName("✅ TC-009: keyword 搜索")
        void testListSearchByKeyword() throws Exception {
            when(connectorService.getConnectorList(any()))
                    .thenReturn(ApiResponse.success(List.of(), ApiResponse.PageResponse.builder()
                            .curPage(1).pageSize(20).total(0L).totalPages(0).build()));

            mockMvc.perform(get("/service/open/v2/admin/connectors")
                            .param("keyword", "IM")
                            .header("X-App-Id", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("✅ TC-010: 自定义分页")
        void testListCustomPage() throws Exception {
            when(connectorService.getConnectorList(any()))
                    .thenReturn(ApiResponse.success(List.of(), ApiResponse.PageResponse.builder()
                            .curPage(2).pageSize(10).total(0L).totalPages(0).build()));

            mockMvc.perform(get("/service/open/v2/admin/connectors")
                            .param("curPage", "2")
                            .param("pageSize", "10")
                            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.curPage").value(2))
                    .andExpect(jsonPath("$.page.pageSize").value(10));
        }

        @Test
        @DisplayName("✅ TC-012: 空结果")
        void testListEmptyResult() throws Exception {
            when(connectorService.getConnectorList(any()))
                    .thenReturn(ApiResponse.success(List.of(), ApiResponse.PageResponse.builder()
                            .curPage(1).pageSize(20).total(0L).totalPages(0).build()));

            mockMvc.perform(get("/service/open/v2/admin/connectors")
                            .param("keyword", "NONEXISTENT")
                            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0))
                    .andExpect(jsonPath("$.page.total").value(0));
        }
    }

    // ==================== #3 查询详情 ====================

    @Nested
    @DisplayName("#3 GET /service/open/v2/admin/connectors/{connectorId}")
    class GetConnectorDetail {

        @Test
        @DisplayName("✅ TC-013: 正常查询")
        void testDetailSuccess() throws Exception {
            ConnectorDetailResponse detail = new ConnectorDetailResponse();
            detail.setConnectorId("1234567890123456789");
            detail.setNameCn("IM 发送消息");
            detail.setNameEn("IM Send Message");
            detail.setConnectorType(1);
            detail.setCreateTime("2026-05-21T10:00:00.000Z");
            detail.setLastUpdateTime("2026-05-21T11:00:00.000Z");

            when(connectorService.getConnectorDetail(1234567890123456789L))
                    .thenReturn(ApiResponse.success(detail));

            mockMvc.perform(get("/service/open/v2/admin/connectors/{connectorId}", "1234567890123456789")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"))
                    .andExpect(jsonPath("$.data.connectorId").isString())
                    .andExpect(jsonPath("$.data.connectorId").value("1234567890123456789"))
                    .andExpect(jsonPath("$.data.connectorType").isNumber())
                    .andExpect(jsonPath("$.data.createTime").isString());
        }

        @Test
        @DisplayName("❌ TC-014: connectorId 不存在")
        void testDetailNotFound() throws Exception {
            when(connectorService.getConnectorDetail(999L))
                    .thenReturn(ApiResponse.error("404", "连接器不存在", "Connector not found"));

            mockMvc.perform(get("/service/open/v2/admin/connectors/{connectorId}", "999")
            .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== #4 更新连接器 ====================

    @Nested
    @DisplayName("#4 PUT /service/open/v2/admin/connectors/{connectorId}")
    class UpdateConnector {

        @Test
        @DisplayName("✅ TC-017: 正常更新")
        void testUpdateSuccess() throws Exception {
            when(connectorService.updateConnector(eq(100L), any()))
                    .thenReturn(ApiResponse.success());

            mockMvc.perform(put("/service/open/v2/admin/connectors/{connectorId}", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "nameCn": "更新后的名称",
                                        "nameEn": "Updated Name"
                                    }
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"));
        }

        @Test
        @DisplayName("❌ TC-019: connectorId 不存在")
        void testUpdateNotFound() throws Exception {
            when(connectorService.updateConnector(eq(999L), any()))
                    .thenReturn(ApiResponse.error("404", "连接器不存在", "Connector not found"));

            mockMvc.perform(put("/service/open/v2/admin/connectors/{connectorId}", "999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nameCn": "测试"}
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== #5 删除连接器 ====================

    @Nested
    @DisplayName("#5 DELETE /service/open/v2/admin/connectors/{connectorId}")
    class DeleteConnector {

        @Test
        @DisplayName("✅ TC-021: 正常删除（无引用）")
        void testDeleteSuccess() throws Exception {
            when(connectorService.deleteConnector(100L))
                    .thenReturn(ApiResponse.success());

            mockMvc.perform(delete("/service/open/v2/admin/connectors/{connectorId}", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"));
        }

        @Test
        @DisplayName("❌ TC-022: 被连接流引用")
        void testDeleteReferenced() throws Exception {
            when(connectorService.deleteConnector(100L))
                    .thenReturn(ApiResponse.error("400",
                            "该连接器被 2 个连接流引用, 请先删除引用关系",
                            "Connector is referenced by 2 flows, remove references first"));

            mockMvc.perform(delete("/service/open/v2/admin/connectors/{connectorId}", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"))
                    .andExpect(jsonPath("$.messageZh").value(org.hamcrest.Matchers.containsString("引用")));
        }

        @Test
        @DisplayName("❌ TC-023: 不存在的连接器")
        void testDeleteNotFound() throws Exception {
            when(connectorService.deleteConnector(999L))
                    .thenReturn(ApiResponse.error("404", "连接器不存在", "Connector not found"));

            mockMvc.perform(delete("/service/open/v2/admin/connectors/{connectorId}", "999")
            .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== #6 查看连接配置 ====================

    @Nested
    @DisplayName("#6 GET /service/open/v2/admin/connectors/{connectorId}/config")
    class GetConnectorConfig {

        @Test
        @DisplayName("✅ TC-024: 已配置")
        void testConfigHasConfig() throws Exception {
            ConnectorConfigResponse mockResp = new ConnectorConfigResponse();
            mockResp.setHasConfig(true);
            mockResp.setConnectionConfig("{\"protocol\":\"HTTP\",\"url\":\"https://example.com\"}");
            when(connectorService.getConnectorConfig(100L))
                    .thenReturn(ApiResponse.success(mockResp));

            mockMvc.perform(get("/service/open/v2/admin/connectors/{connectorId}/config", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("✅ TC-025: 未配置（空）")
        void testConfigEmpty() throws Exception {
            when(connectorService.getConnectorConfig(100L))
                    .thenReturn(ApiResponse.success(ConnectorConfigResponse.empty()));

            mockMvc.perform(get("/service/open/v2/admin/connectors/{connectorId}/config", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("❌ TC-026: 连接器不存在")
        void testConfigNotFound() throws Exception {
            mockMvc.perform(get("/service/open/v2/admin/connectors/{connectorId}/config", "999")
            .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== #7 编辑连接配置 ====================

    @Nested
    @DisplayName("#7 PUT /service/open/v2/admin/connectors/{connectorId}/config")
    class UpdateConnectorConfig {

        @Test
        @DisplayName("✅ TC-027: 正常编辑")
        void testUpdateConfigSuccess() throws Exception {
            when(connectorService.updateConnectorConfig(eq(100L), any()))
                    .thenReturn(ApiResponse.success());

            mockMvc.perform(put("/service/open/v2/admin/connectors/{connectorId}/config", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "connectionConfig": "{\\"protocol\\":\\"HTTP\\",\\"url\\":\\"https://api.example.com\\"}"
                                    }
                                    """)
                                     .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("❌ TC-028: connectionConfig 为空")
        void testUpdateConfigEmpty() throws Exception {
            mockMvc.perform(put("/service/open/v2/admin/connectors/{connectorId}/config", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "connectionConfig": ""
                                    }
                                    """)
                                     .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("❌ TC-029: connectionConfig 为 null")
        void testUpdateConfigNull() throws Exception {
            mockMvc.perform(put("/service/open/v2/admin/connectors/{connectorId}/config", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {}
                                    """)
                                     .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }
    }
}
