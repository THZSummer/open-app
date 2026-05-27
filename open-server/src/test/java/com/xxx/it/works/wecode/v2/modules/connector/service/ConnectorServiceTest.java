package com.xxx.it.works.wecode.v2.modules.connector.service;

import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connector.dto.*;
import com.xxx.it.works.wecode.v2.modules.connector.entity.Connector;
import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersion;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.OpConnectorMapper;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.OpConnectorVersionMapper;
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
@DisplayName("OpConnectorService 测试")
class OpConnectorServiceTest {

    @Mock
    private OpConnectorMapper connectorMapper;

    @Mock
    private OpConnectorVersionMapper connectorVersionMapper;

    @Mock
    private IdGeneratorStrategy idGenerator;

    @InjectMocks
    private OpConnectorService connectorService;

    private Connector existingConnector;

    @BeforeEach
    void setUp() {
        existingConnector = new Connector();
        existingConnector.setId(100L);
        existingConnector.setNameCn("原始连接器");
        existingConnector.setNameEn("Original Connector");
        existingConnector.setDescriptionCn("原始描述");
        existingConnector.setConnectorType(1);
        existingConnector.setStatus(0);
        existingConnector.setCreateTime(new Date());
        existingConnector.setLastUpdateTime(new Date());
        existingConnector.setCreateBy("admin");
        existingConnector.setLastUpdateBy("admin");
    }

    @Nested
    @DisplayName("#1 创建连接器")
    class CreateConnectorTest {

        @Test
        @DisplayName("创建成功")
        void testCreateConnector_Success() {
            ConnectorCreateRequest request = new ConnectorCreateRequest();
            request.setNameCn("新连接器");
            request.setNameEn("New Connector");
            request.setConnectorType(1);

            when(idGenerator.nextId()).thenReturn(200L);
            when(connectorMapper.insert(any(Connector.class))).thenReturn(1);

            ApiResponse<ConnectorCreateResponse> response = connectorService.createConnector(request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("200", response.getData().getId());

            verify(connectorMapper).insert(argThat(c ->
                    "新连接器".equals(c.getNameCn()) &&
                    c.getId().equals(200L) &&
                    c.getConnectorType() == 1
            ));
        }
    }

    @Nested
    @DisplayName("#2 列表查询")
    class GetConnectorListTest {

        @Test
        @DisplayName("无过滤条件查询")
        void testGetConnectorList_NoFilter() {
            Connector c1 = new Connector(); c1.setId(1L); c1.setNameCn("A"); c1.setNameEn("A");
            when(connectorMapper.selectList(isNull(), isNull(), eq(0), eq(20)))
                    .thenReturn(List.of(c1));
            when(connectorMapper.countList(isNull(), isNull())).thenReturn(1L);

            ConnectorListRequest req = new ConnectorListRequest();
            req.setCurPage(1);
            req.setPageSize(20);

            ApiResponse<List<ConnectorListResponse>> response = connectorService.getConnectorList(req);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertEquals(1, response.getData().size());
            assertNotNull(response.getPage());
            assertEquals(1L, response.getPage().getTotal());
        }

        @Test
        @DisplayName("按类型过滤")
        void testGetConnectorList_WithTypeFilter() {
            when(connectorMapper.selectList(eq(1), any(), anyInt(), anyInt()))
                    .thenReturn(List.of());
            when(connectorMapper.countList(eq(1), any())).thenReturn(0L);

            ConnectorListRequest req = new ConnectorListRequest();
            req.setConnectorType(1);

            ApiResponse<List<ConnectorListResponse>> response = connectorService.getConnectorList(req);
            assertEquals("200", response.getCode());
            assertTrue(response.getData().isEmpty());
        }

        @Test
        @DisplayName("按关键词搜索")
        void testGetConnectorList_WithKeyword() {
            when(connectorMapper.selectList(isNull(), eq("test"), anyInt(), anyInt()))
                    .thenReturn(List.of());
            when(connectorMapper.countList(isNull(), eq("test"))).thenReturn(0L);

            ConnectorListRequest req = new ConnectorListRequest();
            req.setKeyword("test");

            ApiResponse<List<ConnectorListResponse>> response = connectorService.getConnectorList(req);
            assertEquals("200", response.getCode());
        }
    }

    @Nested
    @DisplayName("#3 详情查询")
    class GetConnectorDetailTest {

        @Test
        @DisplayName("查询成功")
        void testGetConnectorDetail_Success() {
            when(connectorMapper.selectById(100L)).thenReturn(existingConnector);

            ApiResponse<ConnectorDetailResponse> response = connectorService.getConnectorDetail(100L);

            assertEquals("200", response.getCode());
            assertEquals("原始连接器", response.getData().getNameCn());
            assertEquals("100", response.getData().getId());
        }

        @Test
        @DisplayName("连接器不存在返回404")
        void testGetConnectorDetail_NotFound() {
            when(connectorMapper.selectById(999L)).thenReturn(null);

            ApiResponse<ConnectorDetailResponse> response = connectorService.getConnectorDetail(999L);

            assertEquals("404", response.getCode());
        }
    }

    @Nested
    @DisplayName("#4 编辑基本信息")
    class UpdateConnectorTest {

        @Test
        @DisplayName("更新中文名称成功")
        void testUpdateConnector_OnlyNameCn() {
            ConnectorUpdateRequest request = new ConnectorUpdateRequest();
            request.setNameCn("更新后的名称");

            when(connectorMapper.selectById(100L)).thenReturn(existingConnector);
            when(connectorMapper.update(any(Connector.class))).thenReturn(1);

            ApiResponse<Void> response = connectorService.updateConnector(100L, request);

            assertEquals("200", response.getCode());
            verify(connectorMapper).update(argThat(c ->
                    "更新后的名称".equals(c.getNameCn()) &&
                    "Original Connector".equals(c.getNameEn())
            ));
        }

        @Test
        @DisplayName("连接器不存在返回404")
        void testUpdateConnector_NotFound() {
            ConnectorUpdateRequest request = new ConnectorUpdateRequest();
            request.setNameCn("新名称");

            when(connectorMapper.selectById(999L)).thenReturn(null);

            ApiResponse<Void> response = connectorService.updateConnector(999L, request);
            assertEquals("404", response.getCode());
            verify(connectorMapper, never()).update(any());
        }
    }

    @Nested
    @DisplayName("#5 删除连接器")
    class DeleteConnectorTest {

        @Test
        @DisplayName("删除成功")
        void testDeleteConnector_Success() {
            when(connectorMapper.selectById(100L)).thenReturn(existingConnector);
            when(connectorMapper.countFlowReferences(100L)).thenReturn(0L);
            when(connectorVersionMapper.deleteByConnectorId(100L)).thenReturn(1);
            when(connectorMapper.deleteById(100L)).thenReturn(1);

            ApiResponse<Void> response = connectorService.deleteConnector(100L);
            assertEquals("200", response.getCode());
            verify(connectorMapper).deleteById(100L);
        }

        @Test
        @DisplayName("有引用时禁止删除")
        void testDeleteConnector_HasReferences() {
            when(connectorMapper.selectById(100L)).thenReturn(existingConnector);
            when(connectorMapper.countFlowReferences(100L)).thenReturn(2L);

            ApiResponse<Void> response = connectorService.deleteConnector(100L);
            assertEquals("400", response.getCode());
            verify(connectorMapper, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("#6/#7 连接配置")
    class ConnectorConfigTest {

        @Test
        @DisplayName("查看配置 - 有配置")
        void testGetConnectorConfig_HasConfig() {
            ConnectorVersion version = new ConnectorVersion();
            version.setConnectorId(100L);
            version.setConnectionConfig("{\"protocol\":\"HTTP\"}");

            when(connectorMapper.selectById(100L)).thenReturn(existingConnector);
            when(connectorVersionMapper.selectByConnectorId(100L)).thenReturn(version);

            ApiResponse<ConnectorConfigResponse> response = connectorService.getConnectorConfig(100L);
            assertEquals("200", response.getCode());
            assertTrue(response.getData().isHasConfig());
            assertEquals("{\"protocol\":\"HTTP\"}", response.getData().getConnectionConfig());
        }

        @Test
        @DisplayName("查看配置 - 无配置")
        void testGetConnectorConfig_NoConfig() {
            when(connectorMapper.selectById(100L)).thenReturn(existingConnector);
            when(connectorVersionMapper.selectByConnectorId(100L)).thenReturn(null);

            ApiResponse<ConnectorConfigResponse> response = connectorService.getConnectorConfig(100L);
            assertEquals("200", response.getCode());
            assertFalse(response.getData().isHasConfig());
        }

        @Test
        @DisplayName("编辑配置 - 首次配置创建版本")
        void testUpdateConnectorConfig_FirstTime() {
            ConnectorConfigUpdateRequest request = new ConnectorConfigUpdateRequest();
            request.setConnectionConfig("{\"protocol\":\"HTTP\"}");

            when(connectorMapper.selectById(100L)).thenReturn(existingConnector);
            when(connectorVersionMapper.selectByConnectorId(100L)).thenReturn(null);
            when(idGenerator.nextId()).thenReturn(300L);

            ApiResponse<Void> response = connectorService.updateConnectorConfig(100L, request);
            assertEquals("200", response.getCode());
            verify(connectorVersionMapper).insert(any(ConnectorVersion.class));
        }

        @Test
        @DisplayName("编辑配置 - 更新现有版本（全文替换）")
        void testUpdateConnectorConfig_Replace() {
            ConnectorConfigUpdateRequest request = new ConnectorConfigUpdateRequest();
            request.setConnectionConfig("{\"protocol\":\"HTTP\",\"newField\":true}");

            ConnectorVersion existingVersion = new ConnectorVersion();
            existingVersion.setId(300L);
            existingVersion.setConnectorId(100L);
            existingVersion.setConnectionConfig("{\"protocol\":\"HTTP\"}");

            when(connectorMapper.selectById(100L)).thenReturn(existingConnector);
            when(connectorVersionMapper.selectByConnectorId(100L)).thenReturn(existingVersion);

            ApiResponse<Void> response = connectorService.updateConnectorConfig(100L, request);
            assertEquals("200", response.getCode());
            verify(connectorVersionMapper).update(argThat(v ->
                    "{\"protocol\":\"HTTP\",\"newField\":true}".equals(v.getConnectionConfig())
            ));
        }
    }
}