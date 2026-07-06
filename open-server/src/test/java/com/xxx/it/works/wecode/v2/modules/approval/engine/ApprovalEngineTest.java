package com.xxx.it.works.wecode.v2.modules.approval.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.enums.FlowVersionStatus;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approvalflow.entity.ApprovalFlow;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approvalflow.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalLogMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.flowversion.entity.FlowVersion;
import com.xxx.it.works.wecode.v2.modules.flowversion.mapper.OpFlowVersionMapper;
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
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApprovalEngine 单元测试 — 聚焦 V3 改造新增功能
 *
 * <p>测试范围：</p>
 * <ul>
 *   <li>composeApprovalNodes — appId 参数化，三级回退匹配</li>
 *   <li>createApproval — appId 透传</li>
 *   <li>handleFlowVersionPublishResult — 通过 approve/reject/cancel 间接验证</li>
 *   <li>存量兼容 — 存量类型传 null appId 行为不变</li>
 * </ul>
 */
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
    private com.xxx.it.works.wecode.v2.modules.permission.mapper.SubscriptionMapper subscriptionMapper;
    @Mock
    private com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper permissionMapper;
    @Mock
    private IdGeneratorStrategy idGenerator;
    @Mock
    private com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper apiMapper;
    @Mock
    private com.xxx.it.works.wecode.v2.modules.event.mapper.EventMapper eventMapper;
    @Mock
    private com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper callbackMapper;
    @Mock
    private OpFlowVersionMapper flowVersionMapper;

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

        // 平台级审批模板 (app_id IS NULL)
        platformFlow = new ApprovalFlow();
        platformFlow.setId(1L);
        platformFlow.setCode("connector_flow_version_publish");
        platformFlow.setAppId(null);
        platformFlow.setNodes("[{\"userId\":\"platform_approver\",\"userName\":\"Platform Approver\"}]");

        // 应用级审批模板 (app_id = 123)
        appFlow = new ApprovalFlow();
        appFlow.setId(2L);
        appFlow.setCode("connector_flow_version_publish");
        appFlow.setAppId(appId);
        appFlow.setNodes("[{\"userId\":\"app_approver\",\"userName\":\"App Approver\"}]");

        // 全局审批模板
        globalFlow = new ApprovalFlow();
        globalFlow.setId(3L);
        globalFlow.setCode("global");
        globalFlow.setAppId(null);
        globalFlow.setNodes("[{\"userId\":\"global_approver\",\"userName\":\"Global Approver\"}]");
    }

    // ==================== composeApprovalNodes ====================

    @Nested
    @DisplayName("composeApprovalNodes — appId 参数化")
    class ComposeApprovalNodesTests {

        @Test
        @DisplayName("appId 非 null，应用级模板存在 → 返回应用级+全局两级")
        void testWithAppId_AppLevelTemplateExists() {
            when(flowMapper.selectByCode(eq("connector_flow_version_publish")))
                    .thenReturn(appFlow);
            when(flowMapper.selectByCode(eq("global")))
                    .thenReturn(globalFlow);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH,
                    null,    // permissionId
                    appId    // ✅ appId
            );

            assertNotNull(nodes);
            // 应用级 1 人 + 全局 1 人 = 2 个节点
            assertEquals(2, nodes.size());
            assertEquals("scene", nodes.get(0).getLevel());  // 引擎统一使用 scene/global
            assertEquals("global", nodes.get(1).getLevel());
            assertEquals("app_approver", nodes.get(0).getUserId());
        }

        @Test
        @DisplayName("appId 非 null，无应用模板 → 回退到平台级 + 全局")
        void testWithAppId_FallbackToPlatform() {
            when(flowMapper.selectByCode(eq("connector_flow_version_publish")))
                    .thenReturn(platformFlow);
            when(flowMapper.selectByCode(eq("global")))
                    .thenReturn(globalFlow);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH, null, appId);

            assertEquals(2, nodes.size());
            assertEquals("platform_approver", nodes.get(0).getUserId());
            assertEquals("global_approver", nodes.get(1).getUserId());
        }

        @Test
        @DisplayName("appId 为 null → 直接使用平台级 + 全局")
        void testWithNullAppId_UsesPlatformDirectly() {
            when(flowMapper.selectByCode(eq("connector_flow_version_publish")))
                    .thenReturn(platformFlow);
            when(flowMapper.selectByCode(eq("global")))
                    .thenReturn(globalFlow);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH, null, null);

            assertEquals(2, nodes.size());
            assertEquals("platform_approver", nodes.get(0).getUserId());
            assertEquals("global_approver", nodes.get(1).getUserId());
        }

        @Test
        @DisplayName("存量 API_REGISTER 类型传 null appId → 行为完全不变")
        void testBackwardCompatibility_ApiRegister() {
            ApprovalFlow apiRegisterFlow = new ApprovalFlow();
            apiRegisterFlow.setCode("api_register");
            apiRegisterFlow.setNodes("[{\"userId\":\"api_approver\",\"userName\":\"API Approver\"}]");

            when(flowMapper.selectByCode(eq("api_register")))
                    .thenReturn(apiRegisterFlow);
            when(flowMapper.selectByCode(eq("global")))
                    .thenReturn(globalFlow);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.API_REGISTER, null, null);

            assertNotNull(nodes);
            assertTrue(nodes.size() >= 2);
            assertEquals("api_approver", nodes.get(0).getUserId());
        }

        @Test
        @DisplayName("存量 EVENT_REGISTER 类型传 null appId → 行为不变")
        void testBackwardCompatibility_EventRegister() {
            ApprovalFlow eventFlow = new ApprovalFlow();
            eventFlow.setCode("event_register");
            eventFlow.setNodes("[{\"userId\":\"event_approver\",\"userName\":\"Event Approver\"}]");

            when(flowMapper.selectByCode(eq("event_register")))
                    .thenReturn(eventFlow);
            when(flowMapper.selectByCode(eq("global")))
                    .thenReturn(globalFlow);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.EVENT_REGISTER, null, null);

            assertEquals("event_approver", nodes.get(0).getUserId());
        }

        @Test
        @DisplayName("场景模板和全局模板都为空 → 返回空列表，不抛异常")
        void testBothTemplatesEmpty_ReturnsEmpty() {
            when(flowMapper.selectByCode(eq("connector_flow_version_publish")))
                    .thenReturn(null);
            when(flowMapper.selectByCode(eq("global")))
                    .thenReturn(null);

            List<ApprovalNodeDto> nodes = engine.composeApprovalNodes(
                    ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH, null, null);

            assertTrue(nodes.isEmpty());
        }
    }

    // ==================== createApproval ====================

    @Nested
    @DisplayName("createApproval — appId 透传")
    class CreateApprovalTests {

        @Test
        @DisplayName("V3 类型带 appId 创建审批记录 → 成功")
        void testCreateApproval_WithAppId() {
            when(flowMapper.selectByCode(eq("connector_flow_version_publish")))
                    .thenReturn(appFlow);
            when(flowMapper.selectByCode(eq("global")))
                    .thenReturn(globalFlow);

            ApprovalRecord record = engine.createApproval(
                    ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH,
                    null,          // permissionId
                    200L,          // businessId (flowVersionId)
                    "user1",       // applicantId
                    "User One",    // applicantName
                    "user1",       // operator
                    appId          // ✅ appId
            );

            assertNotNull(record);
            assertEquals("connector_flow_version_publish", record.getBusinessType());
            assertEquals(Long.valueOf(200L), record.getBusinessId());
            assertEquals(ApprovalEngine.Status.PENDING, record.getStatus());
            assertNotNull(record.getCombinedNodes());
            verify(recordMapper).insert(any(ApprovalRecord.class));
        }

        @Test
        @DisplayName("存量类型带 null appId 创建审批记录 → 成功")
        void testCreateApproval_ExistingType() {
            ApprovalFlow apiFlow = new ApprovalFlow();
            apiFlow.setCode("api_register");
            apiFlow.setNodes("[{\"userId\":\"api_approver\",\"userName\":\"API Approver\"}]");

            when(flowMapper.selectByCode(eq("api_register")))
                    .thenReturn(apiFlow);
            when(flowMapper.selectByCode(eq("global")))
                    .thenReturn(globalFlow);

            ApprovalRecord record = engine.createApproval(
                    ApprovalEngine.BusinessType.API_REGISTER,
                    10L, 1L, "user1", "User One", "user1",
                    null);  // ✅ appId = null

            assertNotNull(record);
            assertEquals("api_register", record.getBusinessType());
            verify(recordMapper).insert(any(ApprovalRecord.class));
        }
    }

    // ==================== 审批执行 → 流版本发布回调 ====================

    @Nested
    @DisplayName("审批执行 → handleFlowVersionPublishResult 回调")
    class FlowVersionPublishCallbackTests {

        private ApprovalRecord buildApprovalRecord() {
            // 构建一个完整的审批记录（含 combinedNodes），模拟刚创建后待审批的状态
            ApprovalRecord record = new ApprovalRecord();
            record.setId(1000L);
            record.setBusinessType(ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH);
            record.setBusinessId(200L);  // flowVersionId
            record.setApplicantId("user1");
            record.setApplicantName("User One");
            record.setStatus(ApprovalEngine.Status.PENDING);
            record.setCurrentNode(0);
            // combinedNodes: 只有一个审批人，所以 approve 一次就全部通过
            record.setCombinedNodes("[{\"userId\":\"approver1\",\"userName\":\"Approver One\",\"order\":1,\"level\":\"scene\"}]");
            return record;
        }

        @Test
        @DisplayName("审批通过 → FlowVersion 状态变更为已发布(5)")
        void testApprove_FlowVersionPublished() {
            ApprovalRecord record = buildApprovalRecord();
            when(recordMapper.selectById(1000L)).thenReturn(record);

            FlowVersion version = new FlowVersion();
            version.setId(200L);
            version.setStatus(FlowVersionStatus.PENDING_APPROVAL.getCode());
            when(flowVersionMapper.selectById(200L)).thenReturn(version);

            // 执行审批
            engine.approve(1000L, "approver1", "Approver One", "同意", "approver1");

            // 验证 FlowVersion 状态已更新
            assertEquals(FlowVersionStatus.PUBLISHED.getCode(), version.getStatus());
            assertNotNull(version.getPublishedTime());
            assertEquals("user1", version.getPublishedBy());
            verify(flowVersionMapper).update(version);
            verify(recordMapper).update(record);
            assertEquals(ApprovalEngine.Status.APPROVED, record.getStatus());
        }

        @Test
        @DisplayName("审批驳回 → FlowVersion 状态变更为已驳回(4)")
        void testReject_FlowVersionRejected() {
            ApprovalRecord record = buildApprovalRecord();
            when(recordMapper.selectById(1000L)).thenReturn(record);

            FlowVersion version = new FlowVersion();
            version.setId(200L);
            when(flowVersionMapper.selectById(200L)).thenReturn(version);

            engine.reject(1000L, "approver1", "Approver One", "不符合要求", "approver1");

            assertEquals(FlowVersionStatus.REJECTED.getCode(), version.getStatus());
            verify(flowVersionMapper).update(version);
            assertEquals(ApprovalEngine.Status.REJECTED, record.getStatus());
        }

        @Test
        @DisplayName("审批撤销 → FlowVersion 状态变更为已撤回")
        void testCancel_FlowVersionWithdrawn() {
            ApprovalRecord record = buildApprovalRecord();
            when(recordMapper.selectById(1000L)).thenReturn(record);

            FlowVersion version = new FlowVersion();
            version.setId(200L);
            when(flowVersionMapper.selectById(200L)).thenReturn(version);

            engine.cancel(1000L, "user1");

            assertEquals(FlowVersionStatus.WITHDRAWN.getCode(), version.getStatus());
            verify(flowVersionMapper).update(version);
            assertEquals(ApprovalEngine.Status.CANCELLED, record.getStatus());
        }

        @Test
        @DisplayName("回调时 FlowVersion 不存在 → 记录错误日志，不抛异常")
        void testApprove_VersionNotFound_NoException() {
            ApprovalRecord record = buildApprovalRecord();
            when(recordMapper.selectById(1000L)).thenReturn(record);
            when(flowVersionMapper.selectById(200L)).thenReturn(null);

            // 不应抛异常
            assertDoesNotThrow(() ->
                    engine.approve(1000L, "approver1", "Approver One", "同意", "approver1"));

            // 审批记录状态仍应更新
            assertEquals(ApprovalEngine.Status.APPROVED, record.getStatus());
            // 但不应尝试更新不存在的 FlowVersion
            verify(flowVersionMapper, never()).update(any());
        }

        @Test
        @DisplayName("非 V3 类型审批通过 → 不触发 FlowVersion 回调")
        void testApprove_NonV3Type_NoFlowVersionCallback() {
            ApprovalRecord record = buildApprovalRecord();
            record.setBusinessType(ApprovalEngine.BusinessType.API_REGISTER);
            when(recordMapper.selectById(1000L)).thenReturn(record);

            // API 注册审批不涉及 FlowVersion
            engine.approve(1000L, "approver1", "Approver One", "同意", "approver1");

            // 不应查询 FlowVersion
            verify(flowVersionMapper, never()).selectById(anyLong());
            verify(flowVersionMapper, never()).update(any());
            assertEquals(ApprovalEngine.Status.APPROVED, record.getStatus());
        }
    }

    // ==================== BusinessType 枚举 ====================

    @Test
    @DisplayName("BusinessType 枚举包含全部 7 种类型")
    void testBusinessTypeEnum_AllSevenTypes() {
        assertEquals("api_register", ApprovalEngine.BusinessType.API_REGISTER);
        assertEquals("event_register", ApprovalEngine.BusinessType.EVENT_REGISTER);
        assertEquals("callback_register", ApprovalEngine.BusinessType.CALLBACK_REGISTER);
        assertEquals("api_permission_apply", ApprovalEngine.BusinessType.API_PERMISSION_APPLY);
        assertEquals("event_permission_apply", ApprovalEngine.BusinessType.EVENT_PERMISSION_APPLY);
        assertEquals("callback_permission_apply", ApprovalEngine.BusinessType.CALLBACK_PERMISSION_APPLY);
        assertEquals("connector_flow_version_publish", ApprovalEngine.BusinessType.CONNECTOR_FLOW_VERSION_PUBLISH);
    }
}
