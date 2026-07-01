package com.xxx.it.works.wecode.v2.modules.connector;

import com.xxx.it.works.wecode.v2.common.config.ConnectorPlatformPropertyService;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorStatus;
import com.xxx.it.works.wecode.v2.common.enums.ConnectorVersionStatus;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.auditlog.service.AuditLogService;
import com.xxx.it.works.wecode.v2.modules.connectorversion.service.ConnectorVersionService;
import com.xxx.it.works.wecode.v2.modules.connector.entity.Connector;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersion;
import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersionRef;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.ConnectorVersionRefMapper;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.OpConnectorMapper;
import com.xxx.it.works.wecode.v2.modules.connectorversion.mapper.OpConnectorVersionMapper;
import com.xxx.it.works.wecode.v2.modules.security.AppContextHolder;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConnectorVersionService 测试")
class ConnectorVersionServiceTest {

    @Mock
    private OpConnectorMapper connectorMapper;
    @Mock
    private OpConnectorVersionMapper connectorVersionMapper;
    @Mock
    private ConnectorVersionRefMapper connectorVersionRefMapper;
    @Mock
    private IdGeneratorStrategy idGenerator;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ConnectorPlatformPropertyService propertyService;

    @InjectMocks
    private ConnectorVersionService connectorVersionService;

    private Connector connector;
    private final Long connectorId = 100L;
    private static final Long TEST_APP_ID = 1L;

    @BeforeEach
    void setUp() {
        connector = new Connector();
        connector.setId(connectorId);
        connector.setNameCn("测试连接器");
        connector.setNameEn("test_connector");
        connector.setAppId(TEST_APP_ID);
        connector.setStatus(ConnectorStatus.UNAVAILABLE.getCode()); // 有效不可用
        connector.setConnectorType(1);
        when(idGenerator.nextId()).thenReturn(1000L);
        // propertyService defaults for tests
        lenient().when(propertyService.getConnectorMaxVersions()).thenReturn(1000);
        lenient().when(propertyService.getConnectorConfigMaxBytes(anyString())).thenReturn(0); // 0 = not enforced
        lenient().when(propertyService.getUrlRegexPattern()).thenReturn(null); // no URL validation
        AppContextHolder.setCurrentContext(AppContext.builder().internalId(TEST_APP_ID).build());
    }

    // ===== 创建草稿 =====

    @Nested
    @DisplayName("创建草稿")
    class CreateDraftTest {

        @Test
        @DisplayName("正常创建草稿 → 成功")
        void testCreateDraft_Success() {
            when(connectorMapper.selectById(connectorId)).thenReturn(connector);
            when(connectorVersionMapper.selectListByConnectorId(connectorId, null)).thenReturn(new ArrayList<>());
            when(connectorVersionMapper.selectMaxVersionNumberByConnectorId(connectorId)).thenReturn(null);

            ApiResponse<?> response = connectorVersionService.createDraft(connectorId);

            assertEquals("200", response.getCode());
            verify(connectorVersionMapper).insert(any(ConnectorVersion.class));
        }

        @Test
        @DisplayName("已存在草稿 → 409")
        void testCreateDraft_ExistingDraft_Returns409() {
            when(connectorMapper.selectById(connectorId)).thenReturn(connector);
            ConnectorVersion existingDraft = new ConnectorVersion();
            existingDraft.setId(1L);
            existingDraft.setStatus(ConnectorVersionStatus.DRAFT.getCode());
            when(connectorVersionMapper.selectListByConnectorId(connectorId, null))
                    .thenReturn(List.of(existingDraft));

            ApiResponse<?> response = connectorVersionService.createDraft(connectorId);

            assertEquals("409", response.getCode());
        }

        @Test
        @DisplayName("版本数量达到上限 1000 → 422")
        void testCreateDraft_VersionLimitExceeded_Returns422() {
            when(connectorMapper.selectById(connectorId)).thenReturn(connector);
            List<ConnectorVersion> fullList = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                ConnectorVersion cv = new ConnectorVersion();
                cv.setId((long) i);
                cv.setStatus(ConnectorVersionStatus.PUBLISHED.getCode());
                fullList.add(cv);
            }
            when(connectorVersionMapper.selectListByConnectorId(connectorId, null)).thenReturn(fullList);

            ApiResponse<?> response = connectorVersionService.createDraft(connectorId);

            assertEquals("422", response.getCode());
        }
    }

    // ===== 发布版本 =====

    @Nested
    @DisplayName("发布版本")
    class PublishTest {

        @Test
        @DisplayName("正常发布 → 成功 + 首次发布触发状态联动 AVAILABLE")
        void testPublish_Success_FirstPublish_ConnectorBecomesAvailable() {
            ConnectorVersion draft = makeDraftVersion(200L);
            draft.setConnectionConfig("{\"url\":\"https://example.com\"}");
            when(connectorMapper.selectById(connectorId)).thenReturn(connector);
            when(connectorVersionMapper.selectById(200L)).thenReturn(draft);
            when(connectorVersionMapper.selectListByConnectorId(connectorId, null)).thenReturn(new ArrayList<>());

            ApiResponse<?> response = connectorVersionService.publish(connectorId, 200L);

            assertEquals("200", response.getCode());
            assertEquals(ConnectorVersionStatus.PUBLISHED.getCode(), draft.getStatus());
            // 首次发布 → 连接器状态变为 AVAILABLE
            assertEquals(ConnectorStatus.AVAILABLE.getCode(), connector.getStatus());
            verify(connectorMapper).update(connector);
        }

        @Test
        @DisplayName("非草稿状态发布 → 409")
        void testPublish_NotDraft_Returns409() {
            ConnectorVersion published = makeDraftVersion(200L);
            published.setStatus(ConnectorVersionStatus.PUBLISHED.getCode());
            when(connectorMapper.selectById(connectorId)).thenReturn(connector);
            when(connectorVersionMapper.selectById(200L)).thenReturn(published);

            ApiResponse<?> response = connectorVersionService.publish(connectorId, 200L);

            assertEquals("409", response.getCode());
        }

        @Test
        @DisplayName("草稿配置为空 → 422")
        void testPublish_EmptyConfig_Returns422() {
            ConnectorVersion draft = makeDraftVersion(200L);
            draft.setConnectionConfig(null);
            when(connectorMapper.selectById(connectorId)).thenReturn(connector);
            when(connectorVersionMapper.selectById(200L)).thenReturn(draft);

            ApiResponse<?> response = connectorVersionService.publish(connectorId, 200L);

            assertEquals("422", response.getCode());
            assertTrue(response.getMessageZh().contains("草稿配置为空"));
        }

        @Test
        @DisplayName("URL 白名单正则无效 → 422")
        void testPublish_InvalidUrlRegex_Returns422() {
            ConnectorVersion draft = makeDraftVersion(200L);
            draft.setConnectionConfig("{\"urlWhitelist\":[{\"pattern\":\"[invalid\"}]}");
            when(connectorMapper.selectById(connectorId)).thenReturn(connector);
            when(connectorVersionMapper.selectById(200L)).thenReturn(draft);

            ApiResponse<?> response = connectorVersionService.publish(connectorId, 200L);

            assertEquals("422", response.getCode());
            assertTrue(response.getMessageZh().contains("URL 白名单正则无效"));
        }
    }

    // ===== 复制到草稿 =====

    @Nested
    @DisplayName("复制到草稿")
    class CopyToDraftTest {

        @Test
        @DisplayName("复制已发布版本到草稿 → 成功")
        void testCopyToDraft_Success() {
            ConnectorVersion source = makePublishedVersion(200L);
            source.setConnectionConfig("{\"url\":\"https://example.com\"}");
            when(connectorMapper.selectById(connectorId)).thenReturn(connector);
            when(connectorVersionMapper.selectById(200L)).thenReturn(source);
            when(connectorVersionMapper.selectListByConnectorId(connectorId, null)).thenReturn(List.of(source));
            when(connectorVersionMapper.selectMaxVersionNumberByConnectorId(connectorId)).thenReturn(2);

            ApiResponse<?> response = connectorVersionService.copyToDraft(connectorId, 200L);

            assertEquals("200", response.getCode());
            verify(connectorVersionMapper).insert(any(ConnectorVersion.class));
        }

        @Test
        @DisplayName("复制时覆盖已有草稿 → 成功")
        void testCopyToDraft_OverwriteExistingDraft() {
            ConnectorVersion source = makePublishedVersion(200L);
            ConnectorVersion existingDraft = new ConnectorVersion();
            existingDraft.setId(99L);
            existingDraft.setStatus(ConnectorVersionStatus.DRAFT.getCode());
            when(connectorMapper.selectById(connectorId)).thenReturn(connector);
            when(connectorVersionMapper.selectById(200L)).thenReturn(source);
            when(connectorVersionMapper.selectListByConnectorId(connectorId, null))
                    .thenReturn(List.of(source, existingDraft));
            when(connectorVersionMapper.selectMaxVersionNumberByConnectorId(connectorId)).thenReturn(2);

            ApiResponse<?> response = connectorVersionService.copyToDraft(connectorId, 200L);

            assertEquals("200", response.getCode());
            verify(connectorVersionMapper).deleteById(99L);
            verify(connectorVersionMapper).insert(any(ConnectorVersion.class));
        }
    }

    // ===== 失效版本 =====

    @Nested
    @DisplayName("失效版本")
    class InvalidateVersionTest {

        @Test
        @DisplayName("失效已发布版本 → 成功 + 状态联动 UNAVAILABLE")
        void testInvalidateVersion_Success_LastPublished_ConnectorBecomesUnavailable() {
            ConnectorVersion version = makePublishedVersion(200L);
            when(connectorMapper.selectById(connectorId)).thenReturn(connector);
            when(connectorVersionMapper.selectById(200L)).thenReturn(version);
            when(connectorVersionRefMapper.selectByConnectorVersionId(200L)).thenReturn(List.of());
            // 无其他已发布版本 → 状态联动
            when(connectorVersionMapper.selectListByConnectorId(connectorId, null)).thenReturn(List.of(version));

            ApiResponse<?> response = connectorVersionService.invalidateVersion(connectorId, 200L);

            assertEquals("200", response.getCode());
            assertEquals(ConnectorVersionStatus.INVALIDATED.getCode(), version.getStatus());
            assertEquals(ConnectorStatus.UNAVAILABLE.getCode(), connector.getStatus());
            verify(connectorMapper).update(connector);
        }

        @Test
        @DisplayName("失效有引用的版本 → 422")
        void testInvalidateVersion_WithRefs_Returns422() {
            ConnectorVersion version = makePublishedVersion(200L);
            when(connectorMapper.selectById(connectorId)).thenReturn(connector);
            when(connectorVersionMapper.selectById(200L)).thenReturn(version);
            when(connectorVersionRefMapper.selectRunningFlowNamesByConnectorVersionId(200L)).thenReturn(List.of("运行中的连接流"));

            ApiResponse<?> response = connectorVersionService.invalidateVersion(connectorId, 200L);

            assertEquals("422", response.getCode());
            assertTrue(response.getMessageZh().contains("引用"));
        }
    }

    // ===== 恢复版本 =====

    @Test
    @DisplayName("恢复已失效版本 → 成功 + 状态联动 AVAILABLE")
    void testRecoverVersion_Success() {
        ConnectorVersion version = makePublishedVersion(200L);
        version.setStatus(ConnectorVersionStatus.INVALIDATED.getCode());
        when(connectorMapper.selectById(connectorId)).thenReturn(connector);
        when(connectorVersionMapper.selectById(200L)).thenReturn(version);
        // 无其他已发布版本 → 连接器恢复为 AVAILABLE
        when(connectorVersionMapper.selectListByConnectorId(connectorId, null)).thenReturn(List.of(version));

        ApiResponse<?> response = connectorVersionService.recoverVersion(connectorId, 200L);

        assertEquals("200", response.getCode());
        assertEquals(ConnectorVersionStatus.PUBLISHED.getCode(), version.getStatus());
        assertEquals(ConnectorStatus.AVAILABLE.getCode(), connector.getStatus());
    }

    // ===== 删除版本 =====

    @Test
    @DisplayName("删除草稿版本 → 成功")
    void testDeleteDraftVersion_Success() {
        ConnectorVersion version = makeDraftVersion(200L);
        when(connectorMapper.selectById(connectorId)).thenReturn(connector);
        when(connectorVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<Void> response = connectorVersionService.deleteVersion(connectorId, 200L);

        assertEquals("200", response.getCode());
        verify(connectorVersionMapper).deleteById(200L);
    }

    @Test
    @DisplayName("删除已发布版本 → 409")
    void testDeletePublishedVersion_Returns409() {
        ConnectorVersion version = makePublishedVersion(200L);
        when(connectorMapper.selectById(connectorId)).thenReturn(connector);
        when(connectorVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<Void> response = connectorVersionService.deleteVersion(connectorId, 200L);

        assertEquals("409", response.getCode());
    }

    // ===== 更新草稿 =====

    @Test
    @DisplayName("更新草稿配置 → 成功")
    void testUpdateDraft_Success() {
        ConnectorVersion version = makeDraftVersion(200L);
        when(connectorMapper.selectById(connectorId)).thenReturn(connector);
        when(connectorVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<?> response = connectorVersionService.updateDraft(connectorId, 200L,
                "{\"url\":\"https://updated.com\"}");

        assertEquals("200", response.getCode());
        verify(connectorVersionMapper).update(any(ConnectorVersion.class));
    }

    @Test
    @DisplayName("更新草稿非法 JSON → 400")
    void testUpdateDraft_InvalidJson_Returns400() {
        ConnectorVersion version = makeDraftVersion(200L);
        when(connectorMapper.selectById(connectorId)).thenReturn(connector);
        when(connectorVersionMapper.selectById(200L)).thenReturn(version);

        ApiResponse<?> response = connectorVersionService.updateDraft(connectorId, 200L,
                "{invalid json");

        assertEquals("400", response.getCode());
    }

    // ===== 版本列表 =====

    @Test
    @DisplayName("版本列表查询 → 正确")
    void testGetVersionList() {
        ConnectorVersion v1 = makePublishedVersion(200L);
        when(connectorMapper.selectById(connectorId)).thenReturn(connector);
        when(connectorVersionMapper.selectListByConnectorId(connectorId, null)).thenReturn(List.of(v1));

        var response = connectorVersionService.getVersionList(connectorId, null);

        assertEquals("200", response.getCode());
        assertEquals(1, response.getData().size());
    }

    // ===== Helpers =====

    private ConnectorVersion makeDraftVersion(Long id) {
        ConnectorVersion v = new ConnectorVersion();
        v.setId(id);
        v.setConnectorId(connectorId);
        v.setVersionNumber(1);
        v.setStatus(ConnectorVersionStatus.DRAFT.getCode());
        v.setConnectionConfig("{\"url\":\"https://example.com\"}");
        v.setCreateTime(new Date());
        v.setLastUpdateTime(new Date());
        v.setCreateBy("admin");
        v.setLastUpdateBy("admin");
        return v;
    }

    private ConnectorVersion makePublishedVersion(Long id) {
        ConnectorVersion v = makeDraftVersion(id);
        v.setStatus(ConnectorVersionStatus.PUBLISHED.getCode());
        v.setPublishedTime(new Date());
        v.setPublishedBy("admin");
        return v;
    }
}
