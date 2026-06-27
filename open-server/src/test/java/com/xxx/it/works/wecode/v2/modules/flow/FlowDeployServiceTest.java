package com.xxx.it.works.wecode.v2.modules.flow;

import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.flow.dto.FlowDeployResponse;
import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import com.xxx.it.works.wecode.v2.modules.flow.service.FlowDeployService;
import com.xxx.it.works.wecode.v2.modules.flowversion.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowMapper;
import com.xxx.it.works.wecode.v2.modules.flowversion.mapper.OpFlowVersionMapper;
import com.xxx.it.works.wecode.v2.modules.security.AppContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlowDeployService 测试")
class FlowDeployServiceTest {

    @Mock
    private OpFlowMapper flowMapper;

    @Mock
    private OpFlowVersionMapper flowVersionMapper;

    @InjectMocks
    private FlowDeployService deployService;

    private Flow flow;
    private FlowVersion version;
    private final Long appId = 1L;

    @BeforeEach
    void setUp() {
        AppContextHolder.setCurrentContext(AppContext.builder()
                .internalId(appId)
                .externalId(String.valueOf(appId))
                .build());

        flow = new Flow();
        flow.setId(100L);
        flow.setNameCn("测试流");
        flow.setNameEn("test_flow");
        flow.setAppId(appId);
        flow.setLifecycleStatus(2); // running

        version = new FlowVersion();
        version.setId(200L);
        version.setFlowId(100L);
        version.setVersionNumber(1);
        version.setStatus(FlowVersionStatus.PUBLISHED.getCode());
        version.setOrchestrationConfig("{\"nodes\":[],\"edges\":[]}");
    }

    @AfterEach
    void tearDown() {
        AppContextHolder.clear();
    }

    // ===== 正常部署 =====

    @Test
    @DisplayName("正常部署已发布版本 → 部署成功")
    void testDeployPublishedVersion_Success() {
        when(flowMapper.selectById(100L)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<FlowDeployResponse> response = deployService.deployVersion(100L, 200L);

        assertNotNull(response);
        assertEquals("200", response.getCode());
        assertNotNull(response.getData());
        assertEquals("100", response.getData().getFlowId());
        assertEquals("200", response.getData().getDeployedVersionId());
        assertEquals(1, response.getData().getDeployedVersionNumber());

        verify(flowMapper).deploy(eq(100L), eq(200L), eq(1), any(), any());
    }

    // ===== 连接流不存在 =====

    @Test
    @DisplayName("连接流不存在 → 返回 404")
    void testFlowNotFound_Returns404() {
        when(flowMapper.selectById(999L)).thenReturn(null);

        ApiResponse<FlowDeployResponse> response = deployService.deployVersion(999L, 200L);

        assertNotNull(response);
        assertEquals("404", response.getCode());
        assertNull(response.getData());
        verify(flowVersionMapper, never()).selectById(anyLong());
        verify(flowMapper, never()).deploy(anyLong(), anyLong(), anyInt(), any(), any());
    }

    // ===== appId 不匹配 =====

    @Test
    @DisplayName("appId 不匹配 → 返回 404")
    void testAppIdMismatch_Returns404() {
        flow.setAppId(999L);
        when(flowMapper.selectById(100L)).thenReturn(flow);

        ApiResponse<FlowDeployResponse> response = deployService.deployVersion(100L, 200L);

        assertEquals("404", response.getCode());
    }

    // ===== 版本不存在 =====

    @Test
    @DisplayName("版本不存在 → 返回 404")
    void testVersionNotFound_Returns404() {
        when(flowMapper.selectById(100L)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(null);

        ApiResponse<FlowDeployResponse> response = deployService.deployVersion(100L, 200L);

        assertEquals("404", response.getCode());
    }

    // ===== 版本归属不匹配 =====

    @Test
    @DisplayName("版本归属 flowId 不匹配 → 返回 404")
    void testVersionFlowIdMismatch_Returns404() {
        version.setFlowId(999L);
        when(flowMapper.selectById(100L)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<FlowDeployResponse> response = deployService.deployVersion(100L, 200L);

        assertEquals("404", response.getCode());
    }

    // ===== 部署非已发布版本 → 拒绝 =====

    @Test
    @DisplayName("部署草稿版本 → 409 拒绝")
    void testDeployDraftVersion_Returns409() {
        version.setStatus(FlowVersionStatus.DRAFT.getCode());
        when(flowMapper.selectById(100L)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<FlowDeployResponse> response = deployService.deployVersion(100L, 200L);

        assertEquals("409", response.getCode());
        assertTrue(response.getMessageZh().contains("仅已发布"));
    }

    @Test
    @DisplayName("部署已失效版本 → 409 拒绝")
    void testDeployInvalidatedVersion_Returns409() {
        version.setStatus(FlowVersionStatus.INVALIDATED.getCode());
        when(flowMapper.selectById(100L)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<FlowDeployResponse> response = deployService.deployVersion(100L, 200L);

        assertEquals("409", response.getCode());
    }

    @Test
    @DisplayName("部署待审批版本 → 409 拒绝")
    void testDeployPendingApprovalVersion_Returns409() {
        version.setStatus(FlowVersionStatus.PENDING_APPROVAL.getCode());
        when(flowMapper.selectById(100L)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<FlowDeployResponse> response = deployService.deployVersion(100L, 200L);

        assertEquals("409", response.getCode());
    }

    // ===== 幂等: 重复部署同一版本 =====

    @Test
    @DisplayName("重复部署同一已发布版本 → 幂等, 部署成功")
    void testRedeploySameVersion_Idempotent() {
        when(flowMapper.selectById(100L)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        // 第一次部署
        ApiResponse<FlowDeployResponse> r1 = deployService.deployVersion(100L, 200L);
        assertEquals("200", r1.getCode());

        // 第二次部署同一版本
        ApiResponse<FlowDeployResponse> r2 = deployService.deployVersion(100L, 200L);
        assertEquals("200", r2.getCode());

        // deploy 被调用两次（幂等由 DB 层保证）
        verify(flowMapper, times(2)).deploy(eq(100L), eq(200L), eq(1), any(), any());
    }
}
