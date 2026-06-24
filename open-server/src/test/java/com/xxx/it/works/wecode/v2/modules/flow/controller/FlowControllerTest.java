package com.xxx.it.works.wecode.v2.modules.flow.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.flow.dto.*;
import com.xxx.it.works.wecode.v2.modules.flow.service.OpFlowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpFlowController 测试")
class OpFlowControllerTest {

    @Mock
    private OpFlowService flowService;

    @InjectMocks
    private OpFlowController flowController;

    @Test
    @DisplayName("#8 创建连接流")
    void testCreateFlow() {
        FlowCreateRequest req = new FlowCreateRequest();
        req.setNameCn("流");

        when(flowService.createFlow(req)).thenReturn(
                ApiResponse.success(FlowCreateResponse.builder().id("200").build()));

        ApiResponse<FlowCreateResponse> resp = flowController.createFlow(req);
        assertEquals("200", resp.getData().getId());
    }

    @Test
    @DisplayName("#9 列表查询")
    void testGetFlowList() {
        when(flowService.getFlowList(any())).thenReturn(ApiResponse.success(List.of()));
        assertNotNull(flowController.getFlowList(null, null, 1, 20));
    }

    @Test
    @DisplayName("#10 详情")
    void testGetFlowDetail() {
        FlowDetailResponse d = new FlowDetailResponse();
        d.setId("100");
        when(flowService.getFlowDetail(100L)).thenReturn(ApiResponse.success(d));
        assertEquals("100", flowController.getFlowDetail(100L).getData().getId());
    }

    @Test
    @DisplayName("#11 编辑")
    void testUpdateFlow() {
        when(flowService.updateFlow(eq(100L), any())).thenReturn(ApiResponse.success());
        assertEquals("200", flowController.updateFlow(100L, new FlowUpdateRequest()).getCode());
    }

    @Test
    @DisplayName("#12 删除")
    void testDeleteFlow() {
        when(flowService.deleteFlow(100L)).thenReturn(ApiResponse.success());
        assertEquals("200", flowController.deleteFlow(100L).getCode());
    }

    @Test
    @DisplayName("#13 启动")
    void testStartFlow() {
        when(flowService.startFlow(100L)).thenReturn(ApiResponse.success());
        assertEquals("200", flowController.startFlow(100L).getCode());
    }

    @Test
    @DisplayName("#14 停止")
    void testStopFlow() {
        when(flowService.stopFlow(100L)).thenReturn(ApiResponse.success());
        assertEquals("200", flowController.stopFlow(100L).getCode());
    }
}