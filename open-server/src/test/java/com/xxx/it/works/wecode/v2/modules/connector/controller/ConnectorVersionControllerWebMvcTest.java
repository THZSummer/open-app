package com.xxx.it.works.wecode.v2.modules.connector.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connectorversion.controller.ConnectorVersionController;
import com.xxx.it.works.wecode.v2.modules.connectorversion.service.ConnectorVersionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.connectorversion.dto.ConnectorVersionSaveRequest;
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
@DisplayName("ConnectorVersionController 测试")
class ConnectorVersionControllerWebMvcTest {

    @Mock
    private ConnectorVersionService connectorVersionService;

    @InjectMocks
    private ConnectorVersionController controller;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Long connectorId = 100L;
    private final Long versionId = 200L;

    @BeforeEach
    void setUp() {
        when(connectorVersionService.createDraft(anyLong()))
                .thenReturn(ApiResponse.success());
        when(connectorVersionService.getVersionList(anyLong(), any()))
                .thenReturn(ApiResponse.success(java.util.List.of()));
        when(connectorVersionService.getVersionDetail(anyLong(), anyLong()))
                .thenReturn(ApiResponse.success(null));
        when(connectorVersionService.updateDraft(anyLong(), anyLong(), anyString()))
                .thenReturn(ApiResponse.success());
        when(connectorVersionService.publish(anyLong(), anyLong()))
                .thenReturn(ApiResponse.success(null));
        when(connectorVersionService.copyToDraft(anyLong(), anyLong()))
                .thenReturn(ApiResponse.success(null));
        when(connectorVersionService.invalidateVersion(anyLong(), anyLong()))
                .thenReturn(ApiResponse.success());
        when(connectorVersionService.recoverVersion(anyLong(), anyLong()))
                .thenReturn(ApiResponse.success());
        when(connectorVersionService.deleteVersion(anyLong(), anyLong()))
                .thenReturn(ApiResponse.success());
    }

    @Nested
    @DisplayName("#8 POST 创建草稿")
    class CreateDraftTest {

        @Test
        @DisplayName("正常创建 → 200")
        void testCreateDraft_Success() {
            var response = controller.createDraft(connectorId);
            assertEquals("200", response.getCode());
        }

        @Test
        @DisplayName("已有草稿 → 409")
        void testCreateDraft_Conflict() {
            when(connectorVersionService.createDraft(connectorId))
                    .thenReturn(ApiResponse.error("409", "已存在草稿版本", "Draft exists"));

            var response = controller.createDraft(connectorId);
            assertEquals("409", response.getCode());
        }
    }

    @Test
    @DisplayName("#9 GET 版本列表 → 200")
    void testGetVersionList() {
        var response = controller.getVersionList(connectorId, null);
        assertEquals("200", response.getCode());
    }

    @Test
    @DisplayName("#10 GET 版本详情 → 200")
    void testGetVersionDetail() {
        var response = controller.getVersionDetail(connectorId, versionId);
        assertEquals("200", response.getCode());
    }

    @Test
    @DisplayName("#11 PUT 更新草稿 → 200")
    void testUpdateDraft() throws Exception {
        ConnectorVersionSaveRequest request = new ConnectorVersionSaveRequest();
        request.setConnectionConfig(MAPPER.readTree("{\"url\":\"https://example.com\"}"));

        var response = controller.updateDraft(connectorId, versionId, request);
        assertEquals("200", response.getCode());
    }

    @Test
    @DisplayName("#12 PUT 发布版本 → 200")
    void testPublish() {
        var response = controller.publish(connectorId, versionId);
        assertEquals("200", response.getCode());
    }

    @Test
    @DisplayName("#12 发布版本校验失败 → 422")
    void testPublish_ValidationFail() {
        when(connectorVersionService.publish(connectorId, versionId))
                .thenReturn(ApiResponse.error("422", "草稿配置为空", "Draft config is empty"));

        var response = controller.publish(connectorId, versionId);
        assertEquals("422", response.getCode());
        assertEquals("草稿配置为空", response.getMessageZh());
    }

    @Test
    @DisplayName("#13 POST 复制到草稿 → 200")
    void testCopyToDraft() {
        var response = controller.copyToDraft(connectorId, versionId);
        assertEquals("200", response.getCode());
    }

    @Test
    @DisplayName("#14 PUT 失效版本 → 200")
    void testInvalidateVersion() {
        var response = controller.invalidateVersion(connectorId, versionId);
        assertEquals("200", response.getCode());
    }

    @Test
    @DisplayName("#14 失效有引用的版本 → 422")
    void testInvalidateVersion_WithRefs_Returns422() {
        when(connectorVersionService.invalidateVersion(connectorId, versionId))
                .thenReturn(ApiResponse.error("422", "有 3 个连接流引用此版本", "Version referenced"));

        var response = controller.invalidateVersion(connectorId, versionId);
        assertEquals("422", response.getCode());
        assertEquals("有 3 个连接流引用此版本", response.getMessageZh());
    }

    @Test
    @DisplayName("#15 PUT 恢复版本 → 200")
    void testRecoverVersion() {
        var response = controller.recoverVersion(connectorId, versionId);
        assertEquals("200", response.getCode());
    }

    @Test
    @DisplayName("#16 DELETE 删除版本 → 200")
    void testDeleteVersion() {
        var response = controller.deleteVersion(connectorId, versionId);
        assertEquals("200", response.getCode());
    }
}
