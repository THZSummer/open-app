package com.xxx.it.works.wecode.v2.modules.sync.service;

import com.xxx.it.works.wecode.v2.modules.sync.dto.*;
import com.xxx.it.works.wecode.v2.modules.sync.entity.*;
import com.xxx.it.works.wecode.v2.modules.sync.mapper.SyncMapper;
import com.xxx.it.works.wecode.v2.modules.permission.entity.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 应急接口测试类
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("应急接口服务测试")
class EmergencyServiceTest {

    @Mock
    private SyncMapper syncMapper;

    @InjectMocks
    private SyncService syncService;

    private SubscriptionData subscriptionData;

    @BeforeEach
    void setUp() {
        subscriptionData = new SubscriptionData();
        subscriptionData.setId(1L);
        subscriptionData.setAppId(100L);
        subscriptionData.setPermissionId(10L);
        subscriptionData.setStatus(1);
    }

    @Nested
    @DisplayName("emergencyUpdateOld 应急更新旧表测试")
    class EmergencyUpdateOldTest {

        @Test
        @DisplayName("更新已存在的记录")
        void testUpdateExistingRecord() {
            // Given
            EmergencyRequest request = new EmergencyRequest();
            request.setSubscriptions(Arrays.asList(subscriptionData));

            OldSubscription existing = new OldSubscription();
            existing.setId(1L);

            when(syncMapper.selectOldSubscriptionById(1L)).thenReturn(existing);
            when(syncMapper.updateOldSubscriptionById(any(OldSubscription.class))).thenReturn(1);

            // When
            EmergencyResult result = syncService.emergencyUpdateOld(request);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getSuccess());
            assertEquals(0, result.getFailed());
            assertEquals(0, result.getInserted());
            assertEquals(1, result.getUpdated());

            EmergencyDetail detail = result.getDetails().get(0);
            assertEquals(1L, detail.getId());
            assertEquals("updated", detail.getStatus());

            verify(syncMapper).updateOldSubscriptionById(argThat(sub ->
                sub.getId().equals(1L) &&
                sub.getAppId().equals(100L) &&
                sub.getPermissionId().equals(10L)
            ));
        }

        @Test
        @DisplayName("新增记录（无重复）")
        void testInsertNewRecord_NoDuplicate() {
            // Given
            EmergencyRequest request = new EmergencyRequest();
            request.setSubscriptions(Arrays.asList(subscriptionData));

            when(syncMapper.selectOldSubscriptionById(1L)).thenReturn(null);
            when(syncMapper.countOldSubscriptionByAppIdAndPermissionId(100L, 10L)).thenReturn(0);
            when(syncMapper.insertOldSubscription(any(OldSubscription.class))).thenReturn(1);

            // When
            EmergencyResult result = syncService.emergencyUpdateOld(request);

            // Then
            assertEquals(1, result.getSuccess());
            assertEquals(1, result.getInserted());

            EmergencyDetail detail = result.getDetails().get(0);
            assertEquals("inserted", detail.getStatus());

            verify(syncMapper).insertOldSubscription(argThat(sub ->
                sub.getCreateBy().equals("emergency-update")
            ));
        }

        @Test
        @DisplayName("数据保护：拒绝重复订阅关系")
        void testDataProtection_DuplicateSubscription() {
            // Given
            EmergencyRequest request = new EmergencyRequest();
            request.setSubscriptions(Arrays.asList(subscriptionData));

            when(syncMapper.selectOldSubscriptionById(1L)).thenReturn(null);
            when(syncMapper.countOldSubscriptionByAppIdAndPermissionId(100L, 10L)).thenReturn(1);

            // When
            EmergencyResult result = syncService.emergencyUpdateOld(request);

            // Then
            assertEquals(0, result.getSuccess());
            assertEquals(1, result.getFailed());

            EmergencyDetail detail = result.getDetails().get(0);
            assertEquals("failed", detail.getStatus());
            assertTrue(detail.getError().contains("数据保护"));
            assertTrue(detail.getError().contains("应用ID=100"));
            assertTrue(detail.getError().contains("权限ID=10"));

            verify(syncMapper, never()).insertOldSubscription(any());
        }

        @Test
        @DisplayName("批量操作：包含成功和失败")
        void testBatchOperation_MixedResults() {
            // Given
            SubscriptionData data1 = new SubscriptionData();
            data1.setId(1L);
            data1.setAppId(100L);
            data1.setPermissionId(10L);

            SubscriptionData data2 = new SubscriptionData();
            data2.setId(2L);
            data2.setAppId(100L);
            data2.setPermissionId(20L);

            EmergencyRequest request = new EmergencyRequest();
            request.setSubscriptions(Arrays.asList(data1, data2));

            when(syncMapper.selectOldSubscriptionById(1L)).thenReturn(null);
            when(syncMapper.countOldSubscriptionByAppIdAndPermissionId(100L, 10L)).thenReturn(0);
            when(syncMapper.insertOldSubscription(any())).thenReturn(1);

            when(syncMapper.selectOldSubscriptionById(2L)).thenReturn(null);
            when(syncMapper.countOldSubscriptionByAppIdAndPermissionId(100L, 20L)).thenReturn(1);

            // When
            EmergencyResult result = syncService.emergencyUpdateOld(request);

            // Then
            assertEquals(1, result.getSuccess());
            assertEquals(1, result.getFailed());
            assertEquals(1, result.getInserted());
        }
    }

    @Nested
    @DisplayName("emergencyUpdateNew 应急更新新表测试")
    class EmergencyUpdateNewTest {

        @Test
        @DisplayName("更新已存在的记录")
        void testUpdateExistingRecord() {
            // Given
            subscriptionData.setChannelType(1);
            subscriptionData.setChannelAddress("http://test.com");

            EmergencyRequest request = new EmergencyRequest();
            request.setSubscriptions(Arrays.asList(subscriptionData));

            Subscription existing = new Subscription();
            existing.setId(1L);

            when(syncMapper.selectNewSubscriptionById(1L)).thenReturn(existing);
            when(syncMapper.updateNewSubscriptionById(any(Subscription.class))).thenReturn(1);

            // When
            EmergencyResult result = syncService.emergencyUpdateNew(request);

            // Then
            assertEquals(1, result.getSuccess());
            assertEquals(1, result.getUpdated());

            verify(syncMapper).updateNewSubscriptionById(argThat(sub ->
                sub.getChannelType().equals(1) &&
                "http://test.com".equals(sub.getChannelAddress())
            ));
        }

        @Test
        @DisplayName("新增记录（无重复）")
        void testInsertNewRecord_NoDuplicate() {
            // Given
            EmergencyRequest request = new EmergencyRequest();
            request.setSubscriptions(Arrays.asList(subscriptionData));

            when(syncMapper.selectNewSubscriptionById(1L)).thenReturn(null);
            when(syncMapper.countNewSubscriptionByAppIdAndPermissionId(100L, 10L)).thenReturn(0);
            when(syncMapper.insertNewSubscription(any(Subscription.class))).thenReturn(1);

            // When
            EmergencyResult result = syncService.emergencyUpdateNew(request);

            // Then
            assertEquals(1, result.getSuccess());
            assertEquals(1, result.getInserted());

            verify(syncMapper).insertNewSubscription(argThat(sub ->
                sub.getCreateBy().equals("emergency-update")
            ));
        }

        @Test
        @DisplayName("数据保护：拒绝重复订阅关系")
        void testDataProtection_DuplicateSubscription() {
            // Given
            EmergencyRequest request = new EmergencyRequest();
            request.setSubscriptions(Arrays.asList(subscriptionData));

            when(syncMapper.selectNewSubscriptionById(1L)).thenReturn(null);
            when(syncMapper.countNewSubscriptionByAppIdAndPermissionId(100L, 10L)).thenReturn(1);

            // When
            EmergencyResult result = syncService.emergencyUpdateNew(request);

            // Then
            assertEquals(1, result.getFailed());
            assertEquals(0, result.getInserted());

            EmergencyDetail detail = result.getDetails().get(0);
            assertTrue(detail.getError().contains("数据保护"));

            verify(syncMapper, never()).insertNewSubscription(any());
        }

        @Test
        @DisplayName("空请求列表")
        void testEmptyRequest() {
            // Given
            EmergencyRequest request = new EmergencyRequest();
            request.setSubscriptions(null);

            // When
            EmergencyResult result = syncService.emergencyUpdateNew(request);

            // Then
            assertEquals(0, result.getSuccess());
            assertEquals(0, result.getFailed());
            assertEquals(0, result.getInserted());
            assertEquals(0, result.getUpdated());
            assertEquals(0, result.getDetails().size());
        }
    }
}
