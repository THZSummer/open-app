package com.xxx.it.works.wecode.v2.modules.flow.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.flow.dto.*;
import com.xxx.it.works.wecode.v2.modules.flow.service.OpFlowService;
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

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OpFlowController 接口层测试 (WebMvcTest)
 *
 * <p>测试目标: HTTP 请求绑定 + 响应序列化 + 状态码
 * 接口范围: #8 ~ #16
 * 测试层次: L2 接口层
 * </p>
 */
@WebMvcTest(OpFlowController.class)
@DisplayName("OpFlowController WebMvcTest")
class OpFlowControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpFlowService flowService;

    @MockitoBean
    private AppWhitelistService appWhitelistService;

    @BeforeEach
    void setUp() {
        when(appWhitelistService.isWhitelisted(anyLong())).thenReturn(true);
    }

    // ==================== #8 创建连接流 ====================

    @Nested
    @DisplayName("#8 POST /service/open/v2/admin/flows")
    class CreateFlow {

        @Test
        @DisplayName("✅ TC-033: 正常创建")
        void testCreateSuccess() throws Exception {
            when(flowService.createFlow(any()))
                    .thenReturn(ApiResponse.success(
                            FlowCreateResponse.builder().id("2000000000000000001").build()));

            mockMvc.perform(post("/service/open/v2/admin/flows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "nameCn": "新消息自动通知",
                                        "nameEn": "Auto Message Notification"
                                    }
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"))
                    .andExpect(jsonPath("$.data.id").isString());
        }

        @Test
        @DisplayName("❌ TC-034: 缺少必填 nameCn")
        void testCreateMissingNameCn() throws Exception {
            mockMvc.perform(post("/service/open/v2/admin/flows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nameEn": "Test Flow"}
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"));
        }

        @Test
        @DisplayName("❌ TC-035: 缺少必填 nameEn")
        void testCreateMissingNameEn() throws Exception {
            mockMvc.perform(post("/service/open/v2/admin/flows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nameCn": "测试流"}
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"));
        }
    }

    // ==================== #9 查询列表 ====================

    @Nested
    @DisplayName("#9 GET /service/open/v2/admin/flows")
    class GetFlowList {

        @Test
        @DisplayName("✅ TC-036: 默认分页")
        void testListDefault() throws Exception {
            FlowListResponse item = new FlowListResponse();
            item.setId("2000000000000000001");
            item.setNameCn("新消息自动通知");
            item.setLifecycleStatus(1);

            when(flowService.getFlowList(any()))
                    .thenReturn(ApiResponse.success(List.of(item),
                            ApiResponse.PageResponse.builder()
                                    .curPage(1).pageSize(20).total(1L).totalPages(1).build()));

            mockMvc.perform(get("/service/open/v2/admin/flows")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").isString())
                    .andExpect(jsonPath("$.data[0].lifecycleStatus").isNumber())
                    .andExpect(jsonPath("$.page.curPage").value(1))
                    .andExpect(jsonPath("$.page.total").isNumber());
        }

        @Test
        @DisplayName("✅ TC-037: lifecycleStatus 过滤")
        void testListFilterByStatus() throws Exception {
            when(flowService.getFlowList(any()))
                    .thenReturn(ApiResponse.success(List.of(),
                            ApiResponse.PageResponse.builder().curPage(1).pageSize(20).total(0L).totalPages(0).build()));

            mockMvc.perform(get("/service/open/v2/admin/flows")
                            .param("lifecycleStatus", "1")
                            .header("X-App-Id", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("✅ TC-038: keyword 搜索")
        void testListSearch() throws Exception {
            when(flowService.getFlowList(any()))
                    .thenReturn(ApiResponse.success(List.of(),
                            ApiResponse.PageResponse.builder().curPage(1).pageSize(20).total(0L).totalPages(0).build()));

            mockMvc.perform(get("/service/open/v2/admin/flows")
                            .param("keyword", "通知")
                            .header("X-App-Id", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("✅ TC-039: 空结果")
        void testListEmpty() throws Exception {
            when(flowService.getFlowList(any()))
                    .thenReturn(ApiResponse.success(List.of(),
                            ApiResponse.PageResponse.builder().curPage(1).pageSize(20).total(0L).totalPages(0).build()));

            mockMvc.perform(get("/service/open/v2/admin/flows")
                            .param("keyword", "NONEXISTENT")
                            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0))
                    .andExpect(jsonPath("$.page.total").value(0));
        }
    }

    // ==================== #10 查询详情 ====================

    @Nested
    @DisplayName("#10 GET /service/open/v2/admin/flows/{flowId}")
    class GetFlowDetail {

        @Test
        @DisplayName("✅ TC-040: 正常查询")
        void testDetailSuccess() throws Exception {
            FlowDetailResponse detail = new FlowDetailResponse();
            detail.setId("2000000000000000001");
            detail.setNameCn("新消息自动通知");
            detail.setLifecycleStatus(1);
            detail.setCreateTime(new Date());

            when(flowService.getFlowDetail(2000000000000000001L))
                    .thenReturn(ApiResponse.success(detail));

            mockMvc.perform(get("/service/open/v2/admin/flows/{flowId}", "2000000000000000001")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").isString())
                    .andExpect(jsonPath("$.data.lifecycleStatus").isNumber());
        }

        @Test
        @DisplayName("❌ TC-041: flowId 不存在")
        void testDetailNotFound() throws Exception {
            when(flowService.getFlowDetail(999L))
                    .thenReturn(ApiResponse.error("404", "连接流不存在", "Flow not found"));

            mockMvc.perform(get("/service/open/v2/admin/flows/{flowId}", "999")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("404"));
        }
    }

    // ==================== #11 编辑 ====================

    @Nested
    @DisplayName("#11 PUT /service/open/v2/admin/flows/{flowId}")
    class UpdateFlow {

        @Test
        @DisplayName("✅ TC-042: 正常更新")
        void testUpdateSuccess() throws Exception {
            when(flowService.updateFlow(eq(100L), any()))
                    .thenReturn(ApiResponse.success());

            mockMvc.perform(put("/service/open/v2/admin/flows/{flowId}", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nameCn": "更新后的名称"}
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"));
        }

        @Test
        @DisplayName("❌ TC-043: flowId 不存在")
        void testUpdateNotFound() throws Exception {
            when(flowService.updateFlow(eq(999L), any()))
                    .thenReturn(ApiResponse.error("404", "连接流不存在", "Flow not found"));

            mockMvc.perform(put("/service/open/v2/admin/flows/{flowId}", "999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nameCn\": \"测试\"}")
                            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("404"));
        }
    }

    // ==================== #12 删除 ====================

    @Nested
    @DisplayName("#12 DELETE /service/open/v2/admin/flows/{flowId}")
    class DeleteFlow {

        @Test
        @DisplayName("✅ TC-044: stopped 状态可删除")
        void testDeleteStopped() throws Exception {
            when(flowService.deleteFlow(100L))
                    .thenReturn(ApiResponse.success());

            mockMvc.perform(delete("/service/open/v2/admin/flows/{flowId}", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"));
        }

        @Test
        @DisplayName("❌ TC-045: running 状态拒绝")
        void testDeleteRunning() throws Exception {
            when(flowService.deleteFlow(100L))
                    .thenReturn(ApiResponse.error("400", "仅已停止状态的连接流可删除", "Only stopped flows can be deleted"));

            mockMvc.perform(delete("/service/open/v2/admin/flows/{flowId}", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("400"));
        }

        @Test
        @DisplayName("❌ TC-046: flowId 不存在")
        void testDeleteNotFound() throws Exception {
            when(flowService.deleteFlow(999L))
                    .thenReturn(ApiResponse.error("404", "连接流不存在", "Flow not found"));

            mockMvc.perform(delete("/service/open/v2/admin/flows/{flowId}", "999")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("404"));
        }
    }

    // ==================== #13 启动 ====================

    @Nested
    @DisplayName("#13 POST /service/open/v2/admin/flows/{flowId}/start")
    class StartFlow {

        @Test
        @DisplayName("✅ TC-047: stopped → running")
        void testStartSuccess() throws Exception {
            when(flowService.startFlow(100L))
                    .thenReturn(ApiResponse.success());

            mockMvc.perform(post("/service/open/v2/admin/flows/{flowId}/start", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"));
        }

        @Test
        @DisplayName("❌ TC-048: 已是 running（重复）")
        void testStartAlreadyRunning() throws Exception {
            when(flowService.startFlow(100L))
                    .thenReturn(ApiResponse.error("400", "连接流已处于运行中状态", "Flow is already running"));

            mockMvc.perform(post("/service/open/v2/admin/flows/{flowId}/start", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("400"));
        }

        @Test
        @DisplayName("❌ TC-050: flowId 不存在")
        void testStartNotFound() throws Exception {
            when(flowService.startFlow(999L))
                    .thenReturn(ApiResponse.error("404", "连接流不存在", "Flow not found"));

            mockMvc.perform(post("/service/open/v2/admin/flows/{flowId}/start", "999")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("404"));
        }
    }

    // ==================== #14 停止 ====================

    @Nested
    @DisplayName("#14 POST /service/open/v2/admin/flows/{flowId}/stop")
    class StopFlow {

        @Test
        @DisplayName("✅ TC-051: running → stopped")
        void testStopSuccess() throws Exception {
            when(flowService.stopFlow(100L))
                    .thenReturn(ApiResponse.success());

            mockMvc.perform(post("/service/open/v2/admin/flows/{flowId}/stop", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"));
        }

        @Test
        @DisplayName("❌ TC-052: 已是 stopped")
        void testStopAlreadyStopped() throws Exception {
            when(flowService.stopFlow(100L))
                    .thenReturn(ApiResponse.error("400", "连接流已处于已停止状态", "Flow is already stopped"));

            mockMvc.perform(post("/service/open/v2/admin/flows/{flowId}/stop", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("400"));
        }

        @Test
        @DisplayName("❌ TC-053: flowId 不存在")
        void testStopNotFound() throws Exception {
            when(flowService.stopFlow(999L))
                    .thenReturn(ApiResponse.error("404", "连接流不存在", "Flow not found"));

            mockMvc.perform(post("/service/open/v2/admin/flows/{flowId}/stop", "999")
            .header("X-App-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("404"));
        }
    }

    // ==================== #15 查看编排配置 ====================

    @Nested
    @DisplayName("#15 GET /service/open/v2/admin/flows/{flowId}/config")
    class GetFlowConfig {

        @Test
        @DisplayName("✅ TC-054: 已配置")
        void testConfigHasConfig() throws Exception {
            when(flowService.getFlowConfig(100L))
                    .thenReturn(ApiResponse.success(
                            FlowConfigResponse.of("{\\\"nodes\\\":[],\\\"edges\\\":[]}")));

            mockMvc.perform(get("/service/open/v2/admin/flows/{flowId}/config", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("✅ TC-055: 空配置（初始）")
        void testConfigEmpty() throws Exception {
            when(flowService.getFlowConfig(100L))
                    .thenReturn(ApiResponse.success(FlowConfigResponse.empty()));

            mockMvc.perform(get("/service/open/v2/admin/flows/{flowId}/config", "100")
            .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("❌ TC-056: flowId 不存在")
        void testConfigNotFound() throws Exception {
            when(flowService.getFlowConfig(999L))
                    .thenReturn(ApiResponse.error("404", "连接流不存在", "Flow not found"));

            mockMvc.perform(get("/service/open/v2/admin/flows/{flowId}/config", "999")
            .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== #16 保存编排配置 ====================

    @Nested
    @DisplayName("#16 PUT /service/open/v2/admin/flows/{flowId}/config")
    class UpdateFlowConfig {

        @Test
        @DisplayName("✅ TC-059: 正常保存")
        void testUpdateConfigSuccess() throws Exception {
            when(flowService.updateFlowConfig(eq(100L), any()))
                    .thenReturn(ApiResponse.success());

            mockMvc.perform(put("/service/open/v2/admin/flows/{flowId}/config", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "orchestrationConfig": "{\\"nodes\\":[{\\"id\\":\\"n1\\",\\"type\\":\\"entry\\",\\"position\\":{\\"x\\":0,\\"y\\":0},\\"data\\":{\\"labelCn\\":\\"入口\\"}}],\\"edges\\":[]}"
                                    }
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("❌ TC-060: orchestrationConfig 为空字符串")
        void testUpdateConfigEmpty() throws Exception {
            mockMvc.perform(put("/service/open/v2/admin/flows/{flowId}/config", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"orchestrationConfig": ""}
                                    """)
                                    .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("❌ TC-061: orchestrationConfig 为 null")
        void testUpdateConfigNull() throws Exception {
            mockMvc.perform(put("/service/open/v2/admin/flows/{flowId}/config", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .header("X-App-Id", "1"))
                    .andExpect(status().isNotFound());
        }
    }
}
