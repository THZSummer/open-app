package com.xxx.it.works.wecode.v2.modules.flow.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.flow.FlowVersionController;
import com.xxx.it.works.wecode.v2.modules.flow.FlowVersionService;
import com.xxx.it.works.wecode.v2.modules.flow.dto.FlowVersionSaveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FlowVersionController 测试")
class FlowVersionControllerWebMvcTest {

    @Mock
    private FlowVersionService flowVersionService;

    @InjectMocks
    private FlowVersionController controller;

    private final Long appId = 1L;
    private final Long flowId = 100L;
    private final Long versionId = 200L;

    @BeforeEach
    void setUp() {
        when(flowVersionService.createDraft(anyLong(), anyLong()))
                .thenReturn(ApiResponse.success());
        when(flowVersionService.getVersionList(anyLong(), any(), anyLong()))
                .thenReturn(ApiResponse.success(java.util.List.of()));
        when(flowVersionService.getVersionDetail(anyLong(), anyLong(), anyLong()))
                .thenReturn(ApiResponse.success(null));
        when(flowVersionService.updateDraft(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(ApiResponse.success());
        when(flowVersionService.publish(anyLong(), anyLong(), anyLong()))
                .thenReturn(ApiResponse.success(null));
        when(flowVersionService.copyToDraft(anyLong(), anyLong(), anyLong()))
                .thenReturn(ApiResponse.success());
        when(flowVersionService.invalidateVersion(anyLong(), anyLong(), anyLong()))
                .thenReturn(ApiResponse.success());
        when(flowVersionService.deleteVersion(anyLong(), anyLong(), anyLong()))
                .thenReturn(ApiResponse.success());
        when(flowVersionService.cancelApproval(anyLong(), anyLong(), anyLong()))
                .thenReturn(ApiResponse.success());
        when(flowVersionService.recoverVersion(anyLong(), anyLong(), anyLong()))
                .thenReturn(ApiResponse.success());
        when(flowVersionService.urgeApproval(anyLong(), anyLong(), anyLong()))
                .thenReturn(ApiResponse.success());
    }

    // ===== #28 POST 创建草稿 =====

    @Nested
    @DisplayName("#28 POST 创建草稿")
    class CreateDraftTest {

        @Test
        @DisplayName("正常创建 → 200")
        void testCreateDraft_Success() {
            ApiResponse<?> response = controller.createDraft(appId, flowId);
            assertEquals("200", response.getCode());
        }

        @Test
        @DisplayName("已有草稿 → 409")
        void testCreateDraft_Conflict() {
            when(flowVersionService.createDraft(flowId, appId))
                    .thenReturn(ApiResponse.error("409", "已存在草稿版本", "Draft exists"));

            ApiResponse<?> response = controller.createDraft(appId, flowId);
            assertEquals("409", response.getCode());
        }
    }

    // ===== #29 GET 版本列表 =====

    @Test
    @DisplayName("#29 GET 版本列表 → 200")
    void testGetVersionList() {
        var response = controller.getVersionList(appId, flowId, null);
        assertEquals("200", response.getCode());
    }

    // ===== #30 GET 版本详情 =====

    @Test
    @DisplayName("#30 GET 版本详情 → 200")
    void testGetVersionDetail() {
        var response = controller.getVersionDetail(appId, flowId, versionId);
        assertEquals("200", response.getCode());
    }

    // ===== #31 PUT 更新草稿 =====

    @Test
    @DisplayName("#31 PUT 更新草稿 → 200")
    void testUpdateDraft() {
        FlowVersionSaveRequest request = new FlowVersionSaveRequest();
        request.setOrchestrationConfig("{\"nodes\":[],\"edges\":[]}");

        ApiResponse<?> response = controller.updateDraft(appId, flowId, versionId, request);
        assertEquals("200", response.getCode());
    }

    // ===== #32 POST 发布 =====

    @Test
    @DisplayName("#32 POST 发布版本 → 200")
    void testPublish() {
        var response = controller.publish(appId, flowId, versionId);
        assertEquals("200", response.getCode());
    }

    @Test
    @DisplayName("#32 发布校验失败 → 422")
    void testPublish_ValidationFail_Returns422() {
        when(flowVersionService.publish(flowId, versionId, appId))
                .thenReturn(ApiResponse.error("422", "中文名称不能为空", "nameCn is empty"));

        var response = controller.publish(appId, flowId, versionId);
        assertEquals("422", response.getCode());
    }

    // ===== #33 POST 复制到草稿 =====

    @Test
    @DisplayName("#33 POST 复制到草稿 → 200")
    void testCopyToDraft() {
        var response = controller.copyToDraft(appId, flowId, versionId);
        assertEquals("200", response.getCode());
    }

    // ===== #34 PUT 失效版本 =====

    @Test
    @DisplayName("#34 PUT 失效版本 → 200")
    void testInvalidateVersion() {
        var response = controller.invalidateVersion(appId, flowId, versionId);
        assertEquals("200", response.getCode());
    }

    // ===== #35 PUT 恢复版本 =====

    @Test
    @DisplayName("#35 PUT 恢复版本 → 200")
    void testRecoverVersion() {
        var response = controller.recoverVersion(appId, flowId, versionId);
        assertEquals("200", response.getCode());
    }

    // ===== #36 DELETE 删除版本 =====

    @Test
    @DisplayName("#36 DELETE 删除版本 → 200")
    void testDeleteVersion() {
        var response = controller.deleteVersion(appId, flowId, versionId);
        assertEquals("200", response.getCode());
    }

    // ===== #37 POST 撤回审批 =====

    @Test
    @DisplayName("#37 POST 撤回审批 → 200")
    void testCancelApproval() {
        var response = controller.cancelApproval(appId, flowId, versionId);
        assertEquals("200", response.getCode());
    }

    // ===== #38 POST 催办 =====

    @Test
    @DisplayName("#38 POST 催办 → 200")
    void testUrgeApproval() {
        var response = controller.urgeApproval(appId, flowId, versionId);
        assertEquals("200", response.getCode());
    }
}
