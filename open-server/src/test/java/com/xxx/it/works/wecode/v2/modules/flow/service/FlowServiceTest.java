package com.xxx.it.works.wecode.v2.modules.flow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.flow.dto.*;
import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowMapper;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpFlowService 测试")
class OpFlowServiceTest {

    @Mock
    private OpFlowMapper flowMapper;

    @Mock
    private OpFlowVersionMapper flowVersionMapper;

    @Mock
    private IdGeneratorStrategy idGenerator;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OpFlowService flowService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Flow existingFlow;

    @BeforeEach
    void setUp() {
        existingFlow = new Flow();
        existingFlow.setId(100L);
        existingFlow.setNameCn("原始流");
        existingFlow.setNameEn("Original Flow");
        existingFlow.setLifecycleStatus(OpFlowService.LIFECYCLE_RUNNING);
        existingFlow.setCreateTime(new Date());
        existingFlow.setLastUpdateTime(new Date());
        existingFlow.setCreateBy("admin");
        existingFlow.setLastUpdateBy("admin");
    }

    @Nested
    @DisplayName("#8 创建连接流")
    class CreateFlowTest {

        @Test
        @DisplayName("创建成功，默认 running")
        void testCreateFlow_Success() {
            FlowCreateRequest request = new FlowCreateRequest();
            request.setNameCn("新流");
            request.setNameEn("New Flow");

            when(idGenerator.nextId()).thenReturn(200L);
            when(flowMapper.insert(any(Flow.class))).thenReturn(1);

            ApiResponse<FlowCreateResponse> response = flowService.createFlow(request);

            assertEquals("200", response.getCode());
            assertEquals("200", response.getData().getFlowId());

            verify(flowMapper).insert(argThat(f ->
                    f.getLifecycleStatus() == OpFlowService.LIFECYCLE_RUNNING &&
                    "新流".equals(f.getNameCn())
            ));
        }
    }

    @Nested
    @DisplayName("#9 列表查询")
    class GetFlowListTest {

        @Test
        @DisplayName("分页查询成功")
        void testGetFlowList() {
            Flow f = new Flow(); f.setId(1L); f.setNameCn("F"); f.setNameEn("F"); f.setLifecycleStatus(1);
            when(flowMapper.selectList(isNull(), isNull(), eq(0), eq(20))).thenReturn(List.of(f));
            when(flowMapper.countList(isNull(), isNull())).thenReturn(1L);

            FlowListRequest req = new FlowListRequest();
            ApiResponse<List<FlowListResponse>> response = flowService.getFlowList(req);

            assertEquals("200", response.getCode());
            assertEquals(1, response.getData().size());
            assertEquals(1L, response.getPage().getTotal());
        }

        @Test
        @DisplayName("按状态过滤")
        void testGetFlowList_WithStatus() {
            when(flowMapper.selectList(eq(1), any(), anyInt(), anyInt())).thenReturn(List.of());
            when(flowMapper.countList(eq(1), any())).thenReturn(0L);

            FlowListRequest req = new FlowListRequest();
            req.setLifecycleStatus(1);

            ApiResponse<List<FlowListResponse>> response = flowService.getFlowList(req);
            assertEquals("200", response.getCode());
        }
    }

    @Nested
    @DisplayName("#10 详情查询")
    class GetFlowDetailTest {

        @Test
        @DisplayName("查询成功")
        void testGetFlowDetail_Success() {
            when(flowMapper.selectById(100L)).thenReturn(existingFlow);

            ApiResponse<FlowDetailResponse> response = flowService.getFlowDetail(100L);
            assertEquals("200", response.getCode());
            assertEquals("原始流", response.getData().getNameCn());
        }

        @Test
        @DisplayName("不存在返回404")
        void testGetFlowDetail_NotFound() {
            when(flowMapper.selectById(999L)).thenReturn(null);
            assertEquals("404", flowService.getFlowDetail(999L).getCode());
        }
    }

    @Nested
    @DisplayName("#11 编辑基本信息")
    class UpdateFlowTest {

        @Test
        @DisplayName("更新成功")
        void testUpdateFlow_Success() {
            FlowUpdateRequest request = new FlowUpdateRequest();
            request.setNameCn("更新");

            when(flowMapper.selectById(100L)).thenReturn(existingFlow);
            when(flowMapper.update(any(Flow.class))).thenReturn(1);

            assertEquals("200", flowService.updateFlow(100L, request).getCode());
            verify(flowMapper).update(argThat(f -> "更新".equals(f.getNameCn())));
        }

        @Test
        @DisplayName("不存在返回404")
        void testUpdateFlow_NotFound() {
            when(flowMapper.selectById(999L)).thenReturn(null);
            assertEquals("404", flowService.updateFlow(999L, new FlowUpdateRequest()).getCode());
            verify(flowMapper, never()).update(any());
        }
    }

    @Nested
    @DisplayName("#12 删除连接流")
    class DeleteFlowTest {

        @Test
        @DisplayName("stopped 状态可删除")
        void testDeleteFlow_Stopped() {
            existingFlow.setLifecycleStatus(OpFlowService.LIFECYCLE_STOPPED);
            when(flowMapper.selectById(100L)).thenReturn(existingFlow);
            when(flowVersionMapper.deleteByFlowId(100L)).thenReturn(1);
            when(flowMapper.deleteById(100L)).thenReturn(1);

            assertEquals("200", flowService.deleteFlow(100L).getCode());
            verify(flowMapper).deleteById(100L);
        }

        @Test
        @DisplayName("running 状态不可删除")
        void testDeleteFlow_Running() {
            when(flowMapper.selectById(100L)).thenReturn(existingFlow);
            assertEquals("400", flowService.deleteFlow(100L).getCode());
            verify(flowMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("不存在返回404")
        void testDeleteFlow_NotFound() {
            when(flowMapper.selectById(999L)).thenReturn(null);
            assertEquals("404", flowService.deleteFlow(999L).getCode());
        }
    }

    @Nested
    @DisplayName("#13/#14 启停")
    class LifecycleTest {

        @Test
        @DisplayName("启动 stopped→running")
        void testStartFlow() {
            existingFlow.setLifecycleStatus(OpFlowService.LIFECYCLE_STOPPED);
            when(flowMapper.selectById(100L)).thenReturn(existingFlow);

            assertEquals("200", flowService.startFlow(100L).getCode());
            verify(flowMapper).updateLifecycleStatus(eq(100L), eq(OpFlowService.LIFECYCLE_RUNNING), any(), any());
        }

        @Test
        @DisplayName("已经运行中再启动返回400")
        void testStartFlow_AlreadyRunning() {
            when(flowMapper.selectById(100L)).thenReturn(existingFlow);
            assertEquals("400", flowService.startFlow(100L).getCode());
            verify(flowMapper, never()).updateLifecycleStatus(any(), any(), any(), any());
        }

        @Test
        @DisplayName("停止 running→stopped")
        void testStopFlow() {
            when(flowMapper.selectById(100L)).thenReturn(existingFlow);

            assertEquals("200", flowService.stopFlow(100L).getCode());
            verify(flowMapper).updateLifecycleStatus(eq(100L), eq(OpFlowService.LIFECYCLE_STOPPED), any(), any());
        }

        @Test
        @DisplayName("已经停止再停止返回400")
        void testStopFlow_AlreadyStopped() {
            existingFlow.setLifecycleStatus(OpFlowService.LIFECYCLE_STOPPED);
            when(flowMapper.selectById(100L)).thenReturn(existingFlow);

            assertEquals("400", flowService.stopFlow(100L).getCode());
            verify(flowMapper, never()).updateLifecycleStatus(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("#15/#16 编排配置")
    class FlowConfigTest {

        @Test
        @DisplayName("查看配置 - 有配置")
        void testGetFlowConfig_HasConfig() {
            FlowVersion version = new FlowVersion();
            version.setFlowId(100L);
            version.setOrchestrationConfig("{\"nodes\":[{\"id\":\"n1\",\"type\":\"exit\",\"position\":{\"x\":0,\"y\":0},\"data\":{}}],\"edges\":[]}");

            when(flowMapper.selectById(100L)).thenReturn(existingFlow);
            when(flowVersionMapper.selectByFlowId(100L)).thenReturn(version);

            ApiResponse<FlowConfigResponse> response = flowService.getFlowConfig(100L);
            assertTrue(response.getData().isHasConfig());
        }

        @Test
        @DisplayName("查看配置 - 无配置")
        void testGetFlowConfig_NoConfig() {
            when(flowMapper.selectById(100L)).thenReturn(existingFlow);
            when(flowVersionMapper.selectByFlowId(100L)).thenReturn(null);

            ApiResponse<FlowConfigResponse> response = flowService.getFlowConfig(100L);
            assertFalse(response.getData().isHasConfig());
        }

        @Test
        @DisplayName("保存配置 - 校验通过")
        void testUpdateFlowConfig_Valid() throws Exception {
            FlowConfigUpdateRequest request = new FlowConfigUpdateRequest();
            request.setOrchestrationConfig(MAPPER.readTree("{\"nodes\":[{\"id\":\"n1\",\"type\":\"entry\",\"position\":{\"x\":0,\"y\":0},\"data\":{\"labelCn\":\"入口\"}}],\"edges\":[]}"));

            when(flowMapper.selectById(100L)).thenReturn(existingFlow);
            when(flowVersionMapper.selectByFlowId(100L)).thenReturn(null);
            when(idGenerator.nextId()).thenReturn(300L);

            com.fasterxml.jackson.databind.node.ArrayNode arrNode = mock(com.fasterxml.jackson.databind.node.ArrayNode.class);
            when(arrNode.isArray()).thenReturn(true);
            when(arrNode.isEmpty()).thenReturn(false);
            com.fasterxml.jackson.databind.JsonNode cfgNode = mock(com.fasterxml.jackson.databind.JsonNode.class);
            when(cfgNode.get("nodes")).thenReturn(arrNode);
            when(objectMapper.readTree(anyString())).thenReturn(cfgNode);

            assertEquals("200", flowService.updateFlowConfig(100L, request).getCode());
            verify(flowVersionMapper).insert(any(FlowVersion.class));
        }

        @Test
        @DisplayName("保存配置 - 无节点拒绝")
        void testUpdateFlowConfig_EmptyNodes() throws Exception {
            FlowConfigUpdateRequest request = new FlowConfigUpdateRequest();
            request.setOrchestrationConfig(MAPPER.readTree("{\"nodes\":[],\"edges\":[]}"));

            when(flowMapper.selectById(100L)).thenReturn(existingFlow);

            com.fasterxml.jackson.databind.node.ArrayNode emptyArr = mock(com.fasterxml.jackson.databind.node.ArrayNode.class);
            when(emptyArr.isArray()).thenReturn(true);
            when(emptyArr.isEmpty()).thenReturn(true);
            com.fasterxml.jackson.databind.JsonNode cfgNode = mock(com.fasterxml.jackson.databind.JsonNode.class);
            when(cfgNode.get("nodes")).thenReturn(emptyArr);
            when(objectMapper.readTree(anyString())).thenReturn(cfgNode);

            assertEquals("400", flowService.updateFlowConfig(100L, request).getCode());
            verify(flowVersionMapper, never()).insert(any());
        }
    }
}