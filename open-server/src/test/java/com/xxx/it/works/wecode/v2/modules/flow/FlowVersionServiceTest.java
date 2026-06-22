package com.xxx.it.works.wecode.v2.modules.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.approval.FlowVersionApprovalService;
import com.xxx.it.works.wecode.v2.modules.flow.dto.FlowPublishResponse;
import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowMapper;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowVersionMapper;
import com.xxx.it.works.wecode.v2.modules.flow.validator.FlowPublishValidator;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.ConnectorVersionRefMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FlowVersionService 测试")
class FlowVersionServiceTest {

    @Mock
    private OpFlowMapper flowMapper;
    @Mock
    private OpFlowVersionMapper flowVersionMapper;
    @Mock
    private ConnectorVersionRefMapper connectorVersionRefMapper;
    @Mock
    private IdGeneratorStrategy idGenerator;
    @Mock
    private FlowPublishValidator publishValidator;
    @Mock
    private FlowVersionApprovalService approvalService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private FlowVersionService flowVersionService;

    private Flow flow;
    private final Long appId = 1L;
    private final Long flowId = 100L;

    @BeforeEach
    void setUp() {
        flow = new Flow();
        flow.setId(flowId);
        flow.setNameCn("测试流");
        flow.setNameEn("test_flow");
        flow.setAppId(appId);
        flow.setLifecycleStatus(2);
        when(idGenerator.nextId()).thenReturn(1000L);
    }

    // ===== 创建草稿 =====

    @Nested
    @DisplayName("创建草稿")
    class CreateDraftTest {

        @Test
        @DisplayName("正常创建草稿 → 成功")
        void testCreateDraft_Success() {
            when(flowMapper.selectById(flowId)).thenReturn(flow);
            when(flowVersionMapper.selectListByFlowId(flowId, null)).thenReturn(new ArrayList<>());
            when(flowVersionMapper.selectMaxVersionNumberByFlowId(flowId)).thenReturn(null);

            ApiResponse<?> response = flowVersionService.createDraft(flowId, appId);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            verify(flowVersionMapper).insert(any(FlowVersion.class));
        }

        @Test
        @DisplayName("已存在草稿 → 409")
        void testCreateDraft_ExistingDraft_Returns409() {
            when(flowMapper.selectById(flowId)).thenReturn(flow);
            FlowVersion existingDraft = new FlowVersion();
            existingDraft.setId(1L);
            existingDraft.setStatus(FlowVersionStatus.DRAFT.getCode());
            when(flowVersionMapper.selectListByFlowId(flowId, null))
                    .thenReturn(List.of(existingDraft));

            ApiResponse<?> response = flowVersionService.createDraft(flowId, appId);

            assertEquals("409", response.getCode());
            verify(flowVersionMapper, never()).insert(any());
        }

        @Test
        @DisplayName("版本数量达到上限 1000 → 422")
        void testCreateDraft_VersionLimitExceeded_Returns422() {
            when(flowMapper.selectById(flowId)).thenReturn(flow);
            List<FlowVersion> fullList = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                FlowVersion fv = new FlowVersion();
                fv.setId((long) i);
                fv.setStatus(FlowVersionStatus.PUBLISHED.getCode());
                fullList.add(fv);
            }
            when(flowVersionMapper.selectListByFlowId(flowId, null)).thenReturn(fullList);

            ApiResponse<?> response = flowVersionService.createDraft(flowId, appId);

            assertEquals("422", response.getCode());
        }

        @Test
        @DisplayName("999个版本时 → 正常创建第1000个")
        void testCreateDraft_Version999_Success() {
            when(flowMapper.selectById(flowId)).thenReturn(flow);
            List<FlowVersion> list999 = new ArrayList<>();
            for (int i = 0; i < 999; i++) {
                FlowVersion fv = new FlowVersion();
                fv.setId((long) i);
                fv.setStatus(FlowVersionStatus.PUBLISHED.getCode());
                list999.add(fv);
            }
            when(flowVersionMapper.selectListByFlowId(flowId, null)).thenReturn(list999);
            when(flowVersionMapper.selectMaxVersionNumberByFlowId(flowId)).thenReturn(999);

            ApiResponse<?> response = flowVersionService.createDraft(flowId, appId);

            assertEquals("200", response.getCode());
            verify(flowVersionMapper).insert(any(FlowVersion.class));
        }
    }

    // ===== 发布 =====

    @Nested
    @DisplayName("发布版本")
    class PublishTest {

        @Test
        @DisplayName("全部校验通过 → 发布成功, 提交审批")
        void testPublish_AllValidationsPass_Success() {
            FlowVersion version = makeDraftVersion(200L);
            when(flowMapper.selectById(flowId)).thenReturn(flow);
            when(flowVersionMapper.selectById(200L)).thenReturn(version);
            when(publishValidator.validateBusinessFields(anyString(), anyString())).thenReturn(List.of());
            when(publishValidator.validateOrchestrationConfig(anyString())).thenReturn(List.of());
            when(publishValidator.validateConnectorVersionRefs(200L)).thenReturn(List.of());
            when(publishValidator.validateRateLimitAgainstAppMax(anyString(), anyInt(), anyInt())).thenReturn(List.of());
            when(publishValidator.validateTimeoutAgainstAppMax(anyString(), anyInt())).thenReturn(List.of());

            ApiResponse<FlowPublishResponse> response = flowVersionService.publish(flowId, 200L, appId);

            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("200", response.getData().getVersionId());
            assertEquals(FlowVersionStatus.PENDING_APPROVAL.getCode(), response.getData().getStatus());
            verify(approvalService).submitApproval(eq(200L), eq(flowId), anyString(), anyString(), eq(appId), any(), any());
        }

        @Test
        @DisplayName("业务必填字段校验失败 → 422")
        void testPublish_BusinessFieldsFail_Returns422() {
            FlowVersion version = makeDraftVersion(200L);
            when(flowMapper.selectById(flowId)).thenReturn(flow);
            when(flowVersionMapper.selectById(200L)).thenReturn(version);
            when(publishValidator.validateBusinessFields(anyString(), anyString()))
                    .thenReturn(List.of("中文名称不能为空"));

            ApiResponse<FlowPublishResponse> response = flowVersionService.publish(flowId, 200L, appId);

            assertEquals("422", response.getCode());
        }

        @Test
        @DisplayName("编排配置为空 → 422")
        void testPublish_EmptyConfig_Returns422() {
            FlowVersion version = makeDraftVersion(200L);
            version.setOrchestrationConfig(null);
            when(flowMapper.selectById(flowId)).thenReturn(flow);
            when(flowVersionMapper.selectById(200L)).thenReturn(version);
            when(publishValidator.validateBusinessFields(anyString(), anyString())).thenReturn(List.of());

            ApiResponse<FlowPublishResponse> response = flowVersionService.publish(flowId, 200L, appId);

            assertEquals("422", response.getCode());
            assertTrue(response.getMessageZh().contains("编排配置为空"));
        }

        @Test
        @DisplayName("编排配置校验失败 → 422")
        void testPublish_OrchestrationValidationFail_Returns422() {
            FlowVersion version = makeDraftVersion(200L);
            when(flowMapper.selectById(flowId)).thenReturn(flow);
            when(flowVersionMapper.selectById(200L)).thenReturn(version);
            when(publishValidator.validateBusinessFields(anyString(), anyString())).thenReturn(List.of());
            when(publishValidator.validateOrchestrationConfig(anyString()))
                    .thenReturn(List.of("编排配置 JSON 格式无效"));

            ApiResponse<FlowPublishResponse> response = flowVersionService.publish(flowId, 200L, appId);

            assertEquals("422", response.getCode());
        }
    }

    // ===== 复制到草稿 =====

    @Nested
    @DisplayName("复制到草稿")
    class CopyToDraftTest {

        @Test
        @DisplayName("复制已发布版本到草稿 → 成功")
        void testCopyToDraft_Success() {
            FlowVersion source = makePublishedVersion(200L);
            when(flowMapper.selectById(flowId)).thenReturn(flow);
            when(flowVersionMapper.selectById(200L)).thenReturn(source);
            when(flowVersionMapper.selectListByFlowId(flowId, null)).thenReturn(List.of(source));
            when(flowVersionMapper.selectMaxVersionNumberByFlowId(flowId)).thenReturn(2);

            ApiResponse<?> response = flowVersionService.copyToDraft(flowId, 200L, appId);

            assertEquals("200", response.getCode());
            verify(flowVersionMapper).insert(any(FlowVersion.class));
        }

        @Test
        @DisplayName("存在待审批版本 → 409")
        void testCopyToDraft_HasPendingVersion_Returns409() {
            FlowVersion source = makePublishedVersion(200L);
            FlowVersion pending = new FlowVersion();
            pending.setId(201L);
            pending.setStatus(FlowVersionStatus.PENDING_APPROVAL.getCode());

            when(flowMapper.selectById(flowId)).thenReturn(flow);
            when(flowVersionMapper.selectById(200L)).thenReturn(source);
            when(flowVersionMapper.selectListByFlowId(flowId, null)).thenReturn(List.of(source, pending));

            ApiResponse<?> response = flowVersionService.copyToDraft(flowId, 200L, appId);

            assertEquals("409", response.getCode());
        }
    }

    // ===== 失效版本 =====

    @Test
    @DisplayName("失效已发布版本 → 成功")
    void testInvalidateVersion_Success() {
        FlowVersion version = makePublishedVersion(200L);
        when(flowMapper.selectById(flowId)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<?> response = flowVersionService.invalidateVersion(flowId, 200L, appId);

        assertEquals("200", response.getCode());
        assertEquals(FlowVersionStatus.INVALIDATED.getCode(), version.getStatus());
    }

    @Test
    @DisplayName("失效当前部署版本 → 422")
    void testInvalidateVersion_DeployedVersion_Returns422() {
        flow.setDeployedVersionId(200L);
        FlowVersion version = makePublishedVersion(200L);
        when(flowMapper.selectById(flowId)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<?> response = flowVersionService.invalidateVersion(flowId, 200L, appId);

        assertEquals("422", response.getCode());
        assertTrue(response.getMessageZh().contains("当前部署版本"));
    }

    // ===== 撤回审批 =====

    @Test
    @DisplayName("撤回待审批版本 → 成功")
    void testCancelApproval_Success() {
        FlowVersion version = makeDraftVersion(200L);
        version.setStatus(FlowVersionStatus.PENDING_APPROVAL.getCode());
        when(flowMapper.selectById(flowId)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<?> response = flowVersionService.cancelApproval(flowId, 200L, appId);

        assertEquals("200", response.getCode());
        assertEquals(FlowVersionStatus.WITHDRAWN.getCode(), version.getStatus());
    }

    @Test
    @DisplayName("撤回非待审批版本 → 409")
    void testCancelApproval_NotPending_Returns409() {
        FlowVersion version = makeDraftVersion(200L);
        version.setStatus(FlowVersionStatus.DRAFT.getCode());
        when(flowMapper.selectById(flowId)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<?> response = flowVersionService.cancelApproval(flowId, 200L, appId);

        assertEquals("409", response.getCode());
    }

    // ===== 恢复版本 =====

    @Test
    @DisplayName("恢复已失效版本 → 成功")
    void testRecoverVersion_Success() {
        FlowVersion version = makePublishedVersion(200L);
        version.setStatus(FlowVersionStatus.INVALIDATED.getCode());
        when(flowMapper.selectById(flowId)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<?> response = flowVersionService.recoverVersion(flowId, 200L, appId);

        assertEquals("200", response.getCode());
        assertEquals(FlowVersionStatus.PUBLISHED.getCode(), version.getStatus());
    }

    // ===== 删除版本 =====

    @Test
    @DisplayName("删除草稿版本 → 成功")
    void testDeleteDraftVersion_Success() {
        FlowVersion version = makeDraftVersion(200L);
        version.setStatus(FlowVersionStatus.DRAFT.getCode());
        when(flowMapper.selectById(flowId)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<Void> response = flowVersionService.deleteVersion(flowId, 200L, appId);

        assertEquals("200", response.getCode());
        verify(flowVersionMapper).deleteById(200L);
    }

    @Test
    @DisplayName("删除已发布版本 → 409")
    void testDeletePublishedVersion_Returns409() {
        FlowVersion version = makePublishedVersion(200L);
        when(flowMapper.selectById(flowId)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<Void> response = flowVersionService.deleteVersion(flowId, 200L, appId);

        assertEquals("409", response.getCode());
        verify(flowVersionMapper, never()).deleteById(anyLong());
    }

    // ===== 更新草稿 =====

    @Test
    @DisplayName("更新草稿编排配置 → 成功")
    void testUpdateDraft_Success() {
        FlowVersion version = makeDraftVersion(200L);
        version.setStatus(FlowVersionStatus.DRAFT.getCode());
        when(flowMapper.selectById(flowId)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        String config = "{\"nodes\":[{\"id\":\"t\",\"type\":\"trigger\"}],\"edges\":[]}";
        ApiResponse<?> response = flowVersionService.updateDraft(flowId, 200L, config, appId);

        assertEquals("200", response.getCode());
        verify(flowVersionMapper).update(any(FlowVersion.class));
    }

    @Test
    @DisplayName("更新草稿非法 JSON → 400")
    void testUpdateDraft_InvalidJson_Returns400() {
        FlowVersion version = makeDraftVersion(200L);
        version.setStatus(FlowVersionStatus.DRAFT.getCode());
        when(flowMapper.selectById(flowId)).thenReturn(flow);
        when(flowVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<?> response = flowVersionService.updateDraft(flowId, 200L, "{invalid json", appId);

        assertEquals("400", response.getCode());
    }

    // ===== 版本列表 =====

    @Test
    @DisplayName("版本列表含 deployed 标记 → 正确")
    void testGetVersionList_WithDeployedMark() {
        flow.setDeployedVersionId(200L);
        FlowVersion v1 = makePublishedVersion(200L);
        FlowVersion v2 = makeDraftVersion(201L);

        when(flowMapper.selectById(flowId)).thenReturn(flow);
        when(flowVersionMapper.selectListByFlowId(flowId, null)).thenReturn(List.of(v1, v2));

        var response = flowVersionService.getVersionList(flowId, null, appId);

        assertEquals("200", response.getCode());
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
    }

    // ===== Helpers =====

    private FlowVersion makeDraftVersion(Long id) {
        FlowVersion v = new FlowVersion();
        v.setId(id);
        v.setFlowId(flowId);
        v.setVersionNumber(1);
        v.setStatus(FlowVersionStatus.DRAFT.getCode());
        v.setOrchestrationConfig("{\"nodes\":[{\"id\":\"t\",\"type\":\"trigger\"},{\"id\":\"c\",\"type\":\"connector\"},{\"id\":\"e\",\"type\":\"exit\"}],\"edges\":[{\"id\":\"e1\",\"source\":\"t\",\"target\":\"c\"},{\"id\":\"e2\",\"source\":\"c\",\"target\":\"e\"}]}");
        v.setCreateTime(new Date());
        v.setLastUpdateTime(new Date());
        v.setCreateBy("admin");
        v.setLastUpdateBy("admin");
        return v;
    }

    private FlowVersion makePublishedVersion(Long id) {
        FlowVersion v = makeDraftVersion(id);
        v.setStatus(FlowVersionStatus.PUBLISHED.getCode());
        v.setPublishedTime(new Date());
        v.setPublishedBy("admin");
        return v;
    }
}
