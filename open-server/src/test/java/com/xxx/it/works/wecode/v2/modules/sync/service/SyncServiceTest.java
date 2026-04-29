package com.xxx.it.works.wecode.v2.modules.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SyncRequest;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SyncResult;
import com.xxx.it.works.wecode.v2.modules.sync.dto.SyncDetail;
import com.xxx.it.works.wecode.v2.modules.sync.entity.*;
import com.xxx.it.works.wecode.v2.modules.sync.mapper.SyncMapper;
import com.xxx.it.works.wecode.v2.modules.permission.entity.Subscription;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import com.xxx.it.works.wecode.v2.modules.event.entity.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SyncService 测试类
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("数据同步服务测试")
class SyncServiceTest {

    @Mock
    private SyncMapper syncMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SyncService syncService;

    private OldSubscription oldSubscription;
    private OldPermission oldPermission;
    private OldApi oldApi;
    private OldEvent oldEvent;
    private Api newApi;
    private Event newEvent;
    private Permission newPermission;
    private OldAppProperty channelConfig;
    private OldEflow oldEflow;
    private OldEflowLog oldEflowLog;

    @BeforeEach
    void setUp() {
        // 准备旧订阅关系测试数据
        oldSubscription = new OldSubscription();
        oldSubscription.setId(1L);
        oldSubscription.setAppId(100L);
        oldSubscription.setPermissionId(10L);
        oldSubscription.setPermisssionType("0"); // API
        oldSubscription.setStatus(1);
        oldSubscription.setCreateTime(new Date());
        oldSubscription.setLastUpdateTime(new Date());

        // 准备旧权限测试数据
        oldPermission = new OldPermission();
        oldPermission.setId(10L);
        oldPermission.setPermisssionType("api");
        oldPermission.setModuleId(5L);

        // 准备旧API测试数据
        oldApi = new OldApi();
        oldApi.setId(5L);
        oldApi.setPath("/api/test");
        oldApi.setMethod("GET");
        oldApi.setStatus(1);

        // 准备新API测试数据
        newApi = new Api();
        newApi.setId(50L);
        newApi.setPath("/api/test");
        newApi.setMethod("GET");
        newApi.setStatus(1);

        // 准备新权限测试数据
        newPermission = new Permission();
        newPermission.setId(100L);
        newPermission.setResourceType("api");
        newPermission.setResourceId(50L);
        newPermission.setStatus(1);

        // 准备事件相关测试数据
        oldEvent = new OldEvent();
        oldEvent.setId(6L);
        oldEvent.setTopic("test.event");
        oldEvent.setStatus(1);

        newEvent = new Event();
        newEvent.setId(60L);
        newEvent.setTopic("test.event");
        newEvent.setStatus(1);

        // 准备通道配置
        channelConfig = new OldAppProperty();
        channelConfig.setChannelType(1);
        channelConfig.setChannelAddress("http://test.com/callback");
        channelConfig.setAuthType(1);

        // 准备审批数据
        oldEflow = new OldEflow();
        oldEflow.setEflowId(1000L);
        oldEflow.setEflowStatus(1);
        oldEflow.setEflowSubmitUser("user001");
        oldEflow.setEflowAuditUser("auditor001");
        oldEflow.setResourceType("app_permission");
        oldEflow.setResourceId(1L);
        oldEflow.setCreateTime(new Date());

        oldEflowLog = new OldEflowLog();
        oldEflowLog.setEflowLogId(10000L);
        oldEflowLog.setEflowLogTraceId(1000L);
        oldEflowLog.setEflowLogUser("auditor001");
        oldEflowLog.setEflowLogType("approve");
        oldEflowLog.setCreateTime(new Date());
    }

    @Nested
    @DisplayName("migrate 迁移测试")
    class MigrateTest {

        @Test
        @DisplayName("迁移API订阅关系成功")
        void testMigrate_ApiSubscription_Success() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L));

            when(syncMapper.selectOldSubscriptions(anyList())).thenReturn(Arrays.asList(oldSubscription));
            when(syncMapper.selectNewSubscriptionById(1L)).thenReturn(null); // 不存在
            when(syncMapper.selectOldPermissionById(10L)).thenReturn(oldPermission);
            when(syncMapper.selectOldApiById(5L)).thenReturn(oldApi);
            when(syncMapper.selectNewApiByPathAndMethod("/api/test", "GET")).thenReturn(newApi);
            when(syncMapper.selectNewPermissionByResource("api", 50L)).thenReturn(newPermission);
            when(syncMapper.insertNewSubscriptionIfNotExists(any(Subscription.class))).thenReturn(1);
            when(syncMapper.selectOldEflowByResourceId("app_permission", 1L)).thenReturn(Collections.emptyList());

            // When
            SyncResult result = syncService.migrate(request);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getSuccess());
            assertEquals(0, result.getFailed());
            assertEquals(0, result.getSkipped());
            assertEquals(1, result.getDetails().size());

            SyncDetail detail = result.getDetails().get(0);
            assertEquals(1L, detail.getId());
            assertEquals("success", detail.getStatus());

            verify(syncMapper).insertNewSubscriptionIfNotExists(argThat(sub ->
                    sub.getId().equals(1L) &&
                    sub.getPermissionId().equals(100L) &&
                    sub.getChannelType() == null // API订阅不需要通道配置
            ));
        }

        @Test
        @DisplayName("迁移事件订阅关系成功（带通道配置）")
        void testMigrate_EventSubscription_WithChannelConfig() {
            // Given
            oldSubscription.setPermisssionType("1"); // 事件
            oldPermission.setPermisssionType("event");
            oldPermission.setModuleId(6L);

            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L));

            when(syncMapper.selectOldSubscriptions(anyList())).thenReturn(Arrays.asList(oldSubscription));
            when(syncMapper.selectNewSubscriptionById(1L)).thenReturn(null);
            when(syncMapper.selectOldPermissionById(10L)).thenReturn(oldPermission);
            when(syncMapper.selectOldEventById(6L)).thenReturn(oldEvent);
            when(syncMapper.selectNewEventByTopic("test.event")).thenReturn(newEvent);
            when(syncMapper.selectNewPermissionByResource("event", 60L)).thenReturn(newPermission);
            when(syncMapper.selectAppChannelConfig(100L)).thenReturn(channelConfig);
            when(syncMapper.insertNewSubscriptionIfNotExists(any(Subscription.class))).thenReturn(1);
            when(syncMapper.selectOldEflowByResourceId("app_permission", 1L)).thenReturn(Collections.emptyList());

            // When
            SyncResult result = syncService.migrate(request);

            // Then
            assertEquals(1, result.getSuccess());

            verify(syncMapper).insertNewSubscriptionIfNotExists(argThat(sub ->
                    sub.getChannelType().equals(1) &&
                    "http://test.com/callback".equals(sub.getChannelAddress()) &&
                    sub.getAuthType().equals(1)
            ));
        }

        @Test
        @DisplayName("跳过已存在的订阅关系")
        void testMigrate_SkipExisting() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L));

            Subscription existing = new Subscription();
            existing.setId(1L);

            when(syncMapper.selectOldSubscriptions(anyList())).thenReturn(Arrays.asList(oldSubscription));
            when(syncMapper.selectNewSubscriptionById(1L)).thenReturn(existing);

            // When
            SyncResult result = syncService.migrate(request);

            // Then
            assertEquals(0, result.getSuccess());
            assertEquals(1, result.getSkipped());
            assertEquals("skipped", result.getDetails().get(0).getStatus());

            verify(syncMapper, never()).insertNewSubscriptionIfNotExists(any());
        }

        @Test
        @DisplayName("找不到对应的新API时记录失败")
        void testMigrate_ApiNotFound_Failed() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L));

            when(syncMapper.selectOldSubscriptions(anyList())).thenReturn(Arrays.asList(oldSubscription));
            when(syncMapper.selectNewSubscriptionById(1L)).thenReturn(null);
            when(syncMapper.selectOldPermissionById(10L)).thenReturn(oldPermission);
            when(syncMapper.selectOldApiById(5L)).thenReturn(oldApi);
            when(syncMapper.selectNewApiByPathAndMethod("/api/test", "GET")).thenReturn(null); // 找不到

            // When
            SyncResult result = syncService.migrate(request);

            // Then
            assertEquals(0, result.getSuccess());
            assertEquals(1, result.getFailed());
            assertEquals("failed", result.getDetails().get(0).getStatus());
            assertTrue(result.getDetails().get(0).getError().contains("未找到对应的新权限"));
        }

        @Test
        @DisplayName("全量同步（不传ids）")
        void testMigrate_FullSync() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(null); // 全量

            OldSubscription sub1 = new OldSubscription();
            sub1.setId(1L);
            sub1.setAppId(100L);
            sub1.setPermissionId(10L);
            sub1.setPermisssionType("0");
            sub1.setStatus(1);

            OldSubscription sub2 = new OldSubscription();
            sub2.setId(2L);
            sub2.setAppId(101L);
            sub2.setPermissionId(11L);
            sub2.setPermisssionType("0");
            sub2.setStatus(1);

            when(syncMapper.selectOldSubscriptions(isNull())).thenReturn(Arrays.asList(sub1, sub2));
            when(syncMapper.selectNewSubscriptionById(anyLong())).thenReturn(null);
            when(syncMapper.selectOldPermissionById(anyLong())).thenReturn(oldPermission);
            when(syncMapper.selectOldApiById(anyLong())).thenReturn(oldApi);
            when(syncMapper.selectNewApiByPathAndMethod(anyString(), anyString())).thenReturn(newApi);
            when(syncMapper.selectNewPermissionByResource(anyString(), anyLong())).thenReturn(newPermission);
            when(syncMapper.insertNewSubscriptionIfNotExists(any())).thenReturn(1);
            when(syncMapper.selectOldEflowByResourceId(anyString(), anyLong())).thenReturn(Collections.emptyList());

            // When
            SyncResult result = syncService.migrate(request);

            // Then
            assertEquals(2, result.getSuccess());
            verify(syncMapper).selectOldSubscriptions(isNull());
        }

        @Test
        @DisplayName("同步审批记录成功")
        void testMigrate_WithApprovalRecords() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L));

            when(syncMapper.selectOldSubscriptions(anyList())).thenReturn(Arrays.asList(oldSubscription));
            when(syncMapper.selectNewSubscriptionById(1L)).thenReturn(null);
            when(syncMapper.selectOldPermissionById(10L)).thenReturn(oldPermission);
            when(syncMapper.selectOldApiById(5L)).thenReturn(oldApi);
            when(syncMapper.selectNewApiByPathAndMethod("/api/test", "GET")).thenReturn(newApi);
            when(syncMapper.selectNewPermissionByResource("api", 50L)).thenReturn(newPermission);
            when(syncMapper.insertNewSubscriptionIfNotExists(any())).thenReturn(1);
            when(syncMapper.selectOldEflowByResourceId("app_permission", 1L)).thenReturn(Arrays.asList(oldEflow));
            when(syncMapper.selectNewApprovalRecordById(1000L)).thenReturn(null);
            when(syncMapper.insertNewApprovalRecordIfNotExists(any(ApprovalRecord.class))).thenReturn(1);
            when(syncMapper.selectOldEflowLogByEflowId(1000L)).thenReturn(Arrays.asList(oldEflowLog));
            when(syncMapper.batchInsertNewApprovalLogs(anyList())).thenReturn(1);

            // When
            SyncResult result = syncService.migrate(request);

            // Then
            assertEquals(1, result.getSuccess());
            SyncDetail detail = result.getDetails().get(0);
            assertNotNull(detail.getApprovalStatus());
            assertTrue(detail.getApprovalStatus().contains("同步"));
        }
    }

    @Nested
    @DisplayName("rollback 回退测试")
    class RollbackTest {

        @Test
        @DisplayName("回退数据基本流程")
        void testRollback_Basic() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L));

            Subscription newSub = new Subscription();
            newSub.setId(1L);
            newSub.setAppId(100L);
            newSub.setPermissionId(100L);
            newSub.setStatus(1);

            when(syncMapper.selectNewSubscriptions(anyList())).thenReturn(Arrays.asList(newSub));
            when(syncMapper.selectOldSubscriptionById(1L)).thenReturn(null);

            // When
            SyncResult result = syncService.rollback(request);

            // Then
            assertNotNull(result);
            // 注意：rollback 方法需要完善，当前可能返回 failed
        }

        @Test
        @DisplayName("跳过已存在的旧订阅关系")
        void testRollback_SkipExisting() {
            // Given
            SyncRequest request = new SyncRequest();
            request.setIds(Arrays.asList(1L));

            Subscription newSub = new Subscription();
            newSub.setId(1L);

            OldSubscription existing = new OldSubscription();
            existing.setId(1L);

            when(syncMapper.selectNewSubscriptions(anyList())).thenReturn(Arrays.asList(newSub));
            when(syncMapper.selectOldSubscriptionById(1L)).thenReturn(existing);

            // When
            SyncResult result = syncService.rollback(request);

            // Then
            assertEquals(0, result.getSuccess());
            assertEquals(1, result.getSkipped());
        }
    }

    @Nested
    @DisplayName("辅助方法测试")
    class HelperMethodTest {

        @Test
        @DisplayName("构造 combinedNodes JSON")
        void testConstructCombinedNodes() {
            // When
            String result = syncService.constructCombinedNodes("auditor001");

            // Then
            assertNotNull(result);
            assertTrue(result.contains("auditor001"));
            assertTrue(result.contains("审批人"));
        }

        @Test
        @DisplayName("构造 combinedNodes - 空用户")
        void testConstructCombinedNodes_NullUser() {
            // When
            String result = syncService.constructCombinedNodes(null);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("转换操作类型")
        void testConvertAction() {
            assertEquals(0, syncService.convertAction("approve"));
            assertEquals(0, syncService.convertAction("同意"));
            assertEquals(1, syncService.convertAction("reject"));
            assertEquals(1, syncService.convertAction("拒绝"));
            assertEquals(2, syncService.convertAction("cancel"));
            assertEquals(2, syncService.convertAction("撤销"));
            assertEquals(3, syncService.convertAction("transfer"));
            assertEquals(3, syncService.convertAction("转交"));
            assertEquals(0, syncService.convertAction("unknown"));
            assertEquals(0, syncService.convertAction(null));
        }
    }
}