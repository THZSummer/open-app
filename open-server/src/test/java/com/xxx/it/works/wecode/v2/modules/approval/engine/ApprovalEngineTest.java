package com.xxx.it.works.wecode.v2.modules.approval.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approvalflow.entity.ApprovalFlow;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approvalflow.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalLogMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalEngine 测试")
class ApprovalEngineTest {

    @Mock
    private ApprovalFlowMapper flowMapper;
    @Mock
    private ApprovalRecordMapper recordMapper;
    @Mock
    private ApprovalLogMapper logMapper;
    @Mock
    private IdGeneratorStrategy idGenerator;
    @Mock
    private List<ApprovalBusinessHandler> businessHandlers;
    @Mock
    private ApprovalBusinessHandler flowVersionHandler;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ApprovalEngine engine;

    private ApprovalFlow platformFlow;
    private ApprovalFlow appFlow;
    private ApprovalFlow globalFlow;
    private final Long appId = 123L;

    @BeforeEach
    void setUp() {
        lenient().when(idGenerator.nextId()).thenReturn(9999L);
        lenient().when(businessHandlers.iterator()).thenReturn(Collections.emptyListIterator());
        lenient().when(flowVersionHandler.supportedBusinessType())
                .thenReturn(ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH);

        platformFlow = new ApprovalFlow();
        platformFlow.setId(1L);
        platformFlow.setCode("connector_flow_version_publish");
        platformFlow.setAppId(null);
        platformFlow.setNodes("[{\"userId\":\"platform_approver\",\"userName\":\"Platform Approver\"}]");

        appFlow = new ApprovalFlow();
        appFlow.setId(2L);
        appFlow.setCode("connector_flow_version_publish");
        appFlow.setAppId(appId);
        appFlow.setNodes("[{\"userId\":\"app_approver\",\"userName\":\"App Approver\"}]");

        globalFlow = new ApprovalFlow();
        globalFlow.setId(3L);
        globalFlow.setCode("global");
        globalFlow.setAppId(null);
        globalFlow.setNodes("[{\"userId\":\"global_approver\",\"userName\":\"Global Approver\"}]");
    }

    // ==================== composeApprovalNodes ====================

    @Nested
    @DisplayName("composeApprovalNodes")
    class ComposeApprovalNodesTests {

        @Test
        @DisplayName("app+平台双模板 → 叠加返回 app场景 + 平台场景 + 全局")
        void testWithAppId_AppLevelTemplateExists() {
            when(flowMapper.selectByCodeAndAppId(eq("connector_flow_version_publish"), eq(appId)))
                    .thenReturn(appFlow);
            when(flowMapper.selectByCodeAndAppId(eq("connector_flow_version_publish"), isNull()))
                    .thenReturn(platformFlow);
            when(flowMapper.selectByCodeAndAppId(eq("global"), eq(appId)))
                    .thenReturn(null);
            when(flowMapper.selectByCodeAndAppId(eq("global"), isNull()))
                    .thenReturn(globalFlow);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH, null, appId);

            assertEquals(3, nodes.size());
            assertEquals("app_approver", nodes.get(0).getUserId());
            assertEquals("platform_approver", nodes.get(1).getUserId());
            assertEquals("global_approver", nodes.get(2).getUserId());
        }

        @Test
        @DisplayName("仅平台模板 → 平台场景 + 全局")
        void testWithAppId_FallbackToPlatform() {
            when(flowMapper.selectByCodeAndAppId(eq("connector_flow_version_publish"), eq(appId)))
                    .thenReturn(null);
            when(flowMapper.selectByCodeAndAppId(eq("connector_flow_version_publish"), isNull()))
                    .thenReturn(platformFlow);
            when(flowMapper.selectByCodeAndAppId(eq("global"), eq(appId)))
                    .thenReturn(null);
            when(flowMapper.selectByCodeAndAppId(eq("global"), isNull()))
                    .thenReturn(globalFlow);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH, null, appId);

            assertEquals(2, nodes.size());
            assertEquals("platform_approver", nodes.get(0).getUserId());
            assertEquals("global_approver", nodes.get(1).getUserId());
        }

        @Test
        @DisplayName("appId null → 平台场景 + 全局")
        void testWithNullAppId_UsesPlatformDirectly() {
            when(flowMapper.selectByCodeAndAppId(eq("connector_flow_version_publish"), isNull()))
                    .thenReturn(platformFlow);
            when(flowMapper.selectByCodeAndAppId(eq("global"), isNull()))
                    .thenReturn(globalFlow);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH, null, null);

            assertEquals(2, nodes.size());
        }

        @Test
        @DisplayName("存量 API_REGISTER 类型传 null appId")
        void testBackwardCompatibility_ApiRegister() {
            ApprovalFlow apiFlow = new ApprovalFlow();
            apiFlow.setCode("api_register");
            apiFlow.setNodes("[{\"userId\":\"api_approver\",\"userName\":\"API Approver\"}]");

            when(flowMapper.selectByCodeAndAppId(eq("api_register"), isNull()))
                    .thenReturn(apiFlow);
            when(flowMapper.selectByCodeAndAppId(eq("global"), isNull()))
                    .thenReturn(globalFlow);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.API_REGISTER, null, null);

            assertTrue(nodes.size() >= 2);
            assertEquals("api_approver", nodes.get(0).getUserId());
        }

        @Test
        @DisplayName("存量 EVENT_REGISTER 类型传 null appId")
        void testBackwardCompatibility_EventRegister() {
            ApprovalFlow eventFlow = new ApprovalFlow();
            eventFlow.setCode("event_register");
            eventFlow.setNodes("[{\"userId\":\"event_approver\",\"userName\":\"Event Approver\"}]");

            when(flowMapper.selectByCodeAndAppId(eq("event_register"), isNull()))
                    .thenReturn(eventFlow);
            when(flowMapper.selectByCodeAndAppId(eq("global"), isNull()))
                    .thenReturn(globalFlow);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.EVENT_REGISTER, null, null);

            assertEquals("event_approver", nodes.get(0).getUserId());
        }

        @Test
        @DisplayName("场景模板和全局模板都为空 → 返回空列表")
        void testBothTemplatesEmpty_ReturnsEmpty() {
            when(flowMapper.selectByCodeAndAppId(eq("connector_flow_version_publish"), isNull()))
                    .thenReturn(null);
            when(flowMapper.selectByCodeAndAppId(eq("global"), isNull()))
                    .thenReturn(null);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH, null, null);

            assertTrue(nodes.isEmpty());
        }
    }

    // ==================== createApproval ====================

    @Nested
    @DisplayName("createApproval")
    class CreateApprovalTests {

        @Test
        @DisplayName("V3 类型带 appId → 成功")
        void testCreateApproval_WithAppId() {
            when(flowMapper.selectByCodeAndAppId(eq("connector_flow_version_publish"), eq(appId)))
                    .thenReturn(appFlow);
            when(flowMapper.selectByCodeAndAppId(eq("connector_flow_version_publish"), isNull()))
                    .thenReturn(null);
            when(flowMapper.selectByCodeAndAppId(eq("global"), eq(appId)))
                    .thenReturn(null);
            when(flowMapper.selectByCodeAndAppId(eq("global"), isNull()))
                    .thenReturn(globalFlow);

            ApprovalRecord record = engine.createApproval(
                    ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH,
                    null, 200L, "user1", "User One", "user1", appId);

            assertNotNull(record);
            assertEquals(ApprovalEngine.Status.PENDING, record.getStatus());
            verify(recordMapper).insert(any(ApprovalRecord.class));
        }

        @Test
        @DisplayName("存量类型带 null appId → 成功")
        void testCreateApproval_ExistingType() {
            ApprovalFlow apiFlow = new ApprovalFlow();
            apiFlow.setCode("api_register");
            apiFlow.setNodes("[{\"userId\":\"api_approver\",\"userName\":\"API Approver\"}]");

            when(flowMapper.selectByCodeAndAppId(eq("api_register"), isNull()))
                    .thenReturn(apiFlow);
            when(flowMapper.selectByCodeAndAppId(eq("global"), isNull()))
                    .thenReturn(globalFlow);

            ApprovalRecord record = engine.createApproval(
                    ApprovalEngine.BusinessType.API_REGISTER,
                    10L, 1L, "user1", "User One", "user1", null);

            assertNotNull(record);
            verify(recordMapper).insert(any(ApprovalRecord.class));
        }
    }

    // ==================== 回调分发 ====================

    @Nested
    @DisplayName("审批回调分发")
    class FlowVersionPublishCallbackTests {

        @BeforeEach
        void setupHandler() {
            lenient().when(businessHandlers.iterator())
                    .thenReturn(List.of(flowVersionHandler).iterator());
        }

        private ApprovalRecord buildRecord() {
            ApprovalRecord record = new ApprovalRecord();
            record.setId(1000L);
            record.setBusinessType(ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH);
            record.setBusinessId(200L);
            record.setApplicantId("user1");
            record.setStatus(ApprovalEngine.Status.PENDING);
            record.setCurrentNode(0);
            record.setCombinedNodes("[{\"userId\":\"approver1\",\"userName\":\"A\",\"order\":1,\"level\":\"scene\"}]");
            return record;
        }

        @Test
        @DisplayName("通过 → dispatchApproved")
        void testApprove_HandlerCalled() {
            ApprovalRecord record = buildRecord();
            when(recordMapper.selectById(1000L)).thenReturn(record);

            engine.approve(1000L, "approver1", "A", "ok", "approver1");

            verify(flowVersionHandler).onApproved(record);
            assertEquals(ApprovalEngine.Status.APPROVED, record.getStatus());
        }

        @Test
        @DisplayName("驳回 → dispatchRejected")
        void testReject_HandlerCalled() {
            ApprovalRecord record = buildRecord();
            when(recordMapper.selectById(1000L)).thenReturn(record);

            engine.reject(1000L, "approver1", "A", "no", "approver1");

            verify(flowVersionHandler).onRejected(record);
            assertEquals(ApprovalEngine.Status.REJECTED, record.getStatus());
        }

        @Test
        @DisplayName("撤销 → dispatchCancelled")
        void testCancel_HandlerCalled() {
            ApprovalRecord record = buildRecord();
            when(recordMapper.selectById(1000L)).thenReturn(record);

            engine.cancel(1000L, "user1");

            verify(flowVersionHandler).onCancelled(record);
            assertEquals(ApprovalEngine.Status.CANCELLED, record.getStatus());
        }

        @Test
        @DisplayName("非本人的审批人 → 403")
        void testApprove_WrongOperator() {
            ApprovalRecord record = buildRecord();
            when(recordMapper.selectById(1000L)).thenReturn(record);

            assertThrows(Exception.class, () ->
                    engine.approve(1000L, "someone_else", "X", "ok", "someone_else"));
        }

        @Test
        @DisplayName("非匹配的 businessType → 不调 handler")
        void testApprove_NonMatchingType() {
            ApprovalRecord record = buildRecord();
            record.setBusinessType(ApprovalEngine.BusinessType.API_REGISTER);
            when(recordMapper.selectById(1000L)).thenReturn(record);

            engine.approve(1000L, "approver1", "A", "ok", "approver1");

            verify(flowVersionHandler, never()).onApproved(any());
            assertEquals(ApprovalEngine.Status.APPROVED, record.getStatus());
        }
    }

    // ==================== BusinessType 枚举 ====================

    @Test
    @DisplayName("BusinessType 枚举包含全部 8 种类型")
    void testBusinessTypeEnum_AllSevenTypes() {
        assertEquals("api_register", ApprovalEngine.BusinessType.API_REGISTER);
        assertEquals("event_register", ApprovalEngine.BusinessType.EVENT_REGISTER);
        assertEquals("callback_register", ApprovalEngine.BusinessType.CALLBACK_REGISTER);
        assertEquals("api_permission_apply", ApprovalEngine.BusinessType.API_PERMISSION_APPLY);
        assertEquals("event_permission_apply", ApprovalEngine.BusinessType.EVENT_PERMISSION_APPLY);
        assertEquals("callback_permission_apply", ApprovalEngine.BusinessType.CALLBACK_PERMISSION_APPLY);
        assertEquals("app_version_publish", ApprovalEngine.BusinessType.APP_VERSION_PUBLISH);
        assertEquals("connector_flow_version_publish", ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH);
    }
}
