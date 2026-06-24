package com.xxx.it.works.wecode.v2.modules.connector.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.connector.dto.*;
import com.xxx.it.works.wecode.v2.modules.connector.service.OpConnectorService;
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
@DisplayName("OpConnectorController 测试")
class OpConnectorControllerTest {

    @Mock
    private OpConnectorService connectorService;

    @InjectMocks
    private OpConnectorController connectorController;

    @Nested
    @DisplayName("#1 创建连接器")
    class CreateConnectorTest {

        @Test
        @DisplayName("请求委托给 service")
        void testCreateConnector() {
            ConnectorCreateRequest request = new ConnectorCreateRequest();
            request.setNameCn("新连接器");
            request.setNameEn("New Connector");
            request.setConnectorType(1);

            ApiResponse<ConnectorCreateResponse> mockResp = ApiResponse.success(
                    ConnectorCreateResponse.builder().connectorId("200").build());

            when(connectorService.createConnector(any())).thenReturn(mockResp);

            var response = connectorController.createConnector(request).getBody();
            assertEquals("200", response.getCode());
            assertEquals("200", response.getData().getConnectorId());
            verify(connectorService).createConnector(request);
        }
    }

    @Nested
    @DisplayName("#2 连接器列表")
    class GetConnectorListTest {

        @Test
        @DisplayName("请求委托给 service")
        void testGetConnectorList() {
            ApiResponse<List<ConnectorListResponse>> mockResp = ApiResponse.success(List.of());
            when(connectorService.getConnectorList(any())).thenReturn(mockResp);

            var response = connectorController.getConnectorList(
                    null, null, 1, 20).getBody();
            assertEquals("200", response.getCode());
            verify(connectorService).getConnectorList(argThat(r ->
                    r.getCurPage() == 1 && r.getPageSize() == 20));
        }
    }

    @Nested
    @DisplayName("#3 连接器详情")
    class GetConnectorDetailTest {

        @Test
        @DisplayName("请求委托给 service")
        void testGetConnectorDetail() {
            ConnectorDetailResponse detail = new ConnectorDetailResponse();
            detail.setConnectorId("100");
            when(connectorService.getConnectorDetail(100L))
                    .thenReturn(ApiResponse.success(detail));

            var response = connectorController.getConnectorDetail(100L).getBody();
            assertEquals("100", response.getData().getConnectorId());
        }
    }

    @Nested
    @DisplayName("#4 编辑连接器")
    class UpdateConnectorTest {

        @Test
        @DisplayName("请求委托给 service")
        void testUpdateConnector() {
            ConnectorUpdateRequest request = new ConnectorUpdateRequest();
            request.setNameCn("更新");

            when(connectorService.updateConnector(eq(100L), any())).thenReturn(ApiResponse.success());

            var response = connectorController.updateConnector(100L, request).getBody();
            assertEquals("200", response.getCode());
            verify(connectorService).updateConnector(100L, request);
        }
    }

    @Nested
    @DisplayName("#5 删除连接器")
    class DeleteConnectorTest {

        @Test
        @DisplayName("请求委托给 service")
        void testDeleteConnector() {
            when(connectorService.deleteConnector(100L)).thenReturn(ApiResponse.success());

            var response = connectorController.deleteConnector(100L).getBody();
            assertEquals("200", response.getCode());
            verify(connectorService).deleteConnector(100L);
        }
    }
}