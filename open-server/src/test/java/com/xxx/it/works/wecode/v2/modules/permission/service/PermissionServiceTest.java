package com.xxx.it.works.wecode.v2.modules.permission.service;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.category.entity.Category;
import com.xxx.it.works.wecode.v2.modules.category.mapper.CategoryMapper;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.event.mapper.EventMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.EventPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppAccessException;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import com.xxx.it.works.wecode.v2.modules.permission.dto.*;
import com.xxx.it.works.wecode.v2.modules.permission.entity.Subscription;
import com.xxx.it.works.wecode.v2.modules.permission.mapper.SubscriptionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("权限管理服务测试")
class PermissionServiceTest {

    @Mock
    private PermissionMapper permissionMapper;
    
    @Mock
    private PermissionPropertyMapper permissionPropertyMapper;
    
    @Mock
    private SubscriptionMapper subscriptionMapper;
    
    @Mock
    private CategoryMapper categoryMapper;
    
    @Mock
    private ApiMapper apiMapper;
    
    @Mock
    private ApiPropertyMapper apiPropertyMapper;
    
    @Mock
    private EventMapper eventMapper;
    
    @Mock
    private EventPropertyMapper eventPropertyMapper;
    
    @Mock
    private CallbackMapper callbackMapper;
    
    @Mock
    private CallbackPropertyMapper callbackPropertyMapper;
    
    @Mock
    private IdGeneratorStrategy idGenerator;
    
    @Mock
    private ApprovalEngine approvalEngine;
    
    @Mock
    private AppContextResolver appContextResolver;
    
    @InjectMocks
    private PermissionService permissionService;

    private Permission testPermission;
    private Subscription testSubscription;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testPermission = new Permission();
        testPermission.setId(100L);
        testPermission.setNameCn("测试权限");
        testPermission.setNameEn("test_permission");
        testPermission.setScope("api:test:read");
        testPermission.setResourceType("api");
        testPermission.setResourceId(1L);
        testPermission.setCategoryId(10L);
        testPermission.setStatus(1);

        testSubscription = new Subscription();
        testSubscription.setId(200L);
        testSubscription.setAppId(1L);
        testSubscription.setPermissionId(100L);
        testSubscription.setStatus(0);
        testSubscription.setCreateTime(new Date());
        testSubscription.setLastUpdateTime(new Date());

        testCategory = new Category();
        testCategory.setId(10L);
        testCategory.setNameCn("测试分类");
        testCategory.setNameEn("Test Category");
        testCategory.setPath("/1/10/");

        // 默认 mock AppContextResolver
        AppContext defaultAppContext = AppContext.builder()
                .internalId(1L)
                .externalId("1")
                .build();
        when(appContextResolver.resolveAndValidate(anyString())).thenReturn(defaultAppContext);
        when(appContextResolver.toExternalId(anyLong())).thenAnswer(invocation -> String.valueOf(invocation.getArgument(0)));
    }

    @Nested
    @DisplayName("API 权限管理测试")
    class ApiPermissionTests {

        @Test
        @DisplayName("获取订阅列表成功")
        void testGetApiSubscriptionList_Success() {
            List<Subscription> subscriptions = new ArrayList<>();
            subscriptions.add(testSubscription);

            when(subscriptionMapper.selectApiSubscriptionsByAppId(eq(1L), any(), any(), anyInt(), anyInt()))
                    .thenReturn(subscriptions);
            when(subscriptionMapper.countApiSubscriptionsByAppId(eq(1L), any(), any()))
                    .thenReturn(1L);
            when(permissionMapper.selectById(100L)).thenReturn(testPermission);
            when(categoryMapper.selectById(10L)).thenReturn(testCategory);

            ApiResponse<List<ApiSubscriptionListResponse>> response = 
                    permissionService.getApiSubscriptionList("1", null, null, 1, 20);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertNotNull(response.getPage());
            assertEquals(1L, response.getPage().getTotal());
        }

        @Test
        @DisplayName("获取分类权限成功")
        void testGetCategoryApiPermissions_Success() {
            List<Permission> permissions = new ArrayList<>();
            permissions.add(testPermission);

            when(categoryMapper.selectById(10L)).thenReturn(testCategory);
            when(permissionMapper.selectApiPermissionsByCategory(eq(10L), any(), any(), any(), anyBoolean(), anyInt(), anyInt()))
                    .thenReturn(permissions);
            when(permissionMapper.countApiPermissionsByCategory(eq(10L), any(), any(), any(), anyBoolean()))
                    .thenReturn(1L);

            ApiResponse<List<CategoryPermissionListResponse>> response = 
                    permissionService.getCategoryApiPermissions("10", "1", null, null, true, 1, 20);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
        }

        @Test
        @DisplayName("申请权限成功")
        void testSubscribeApiPermissions_Success() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(List.of("100"));

            List<Permission> permissions = new ArrayList<>();
            permissions.add(testPermission);

            when(permissionMapper.selectByIds(anyList())).thenReturn(permissions);
            when(subscriptionMapper.selectByAppIdAndPermissionId(eq(1L), eq(100L))).thenReturn(null);
            when(idGenerator.nextId()).thenReturn(200L);
            when(subscriptionMapper.batchInsert(anyList())).thenReturn(1);

            ApprovalRecord mockRecord = new ApprovalRecord();
            mockRecord.setId(300L);
            when(approvalEngine.createApproval(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn(mockRecord);

            PermissionSubscribeResponse response = 
                    permissionService.subscribeApiPermissions("1", request);

            assertNotNull(response);
            assertEquals(1, response.getSuccessCount());
            assertEquals(0, response.getFailedCount());
            assertNotNull(response.getRecords());
            assertEquals(1, response.getRecords().size());
        }

        @Test
        @DisplayName("申请权限-权限不存在")
        void testSubscribeApiPermissions_PermissionNotFound() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(List.of("100"));

            when(permissionMapper.selectByIds(anyList())).thenReturn(Collections.emptyList());

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                permissionService.subscribeApiPermissions("1", request);
            });

            assertEquals("400", exception.getCode());
        }

        @Test
        @DisplayName("申请权限-已订阅")
        void testSubscribeApiPermissions_AlreadySubscribed() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(List.of("100"));

            List<Permission> permissions = new ArrayList<>();
            permissions.add(testPermission);

            when(permissionMapper.selectByIds(anyList())).thenReturn(permissions);
            when(subscriptionMapper.selectByAppIdAndPermissionId(eq(1L), eq(100L))).thenReturn(testSubscription);

            PermissionSubscribeResponse response = 
                    permissionService.subscribeApiPermissions("1", request);

            assertNotNull(response);
            assertEquals(0, response.getSuccessCount());
            assertEquals(1, response.getFailedCount());
            assertNotNull(response.getFailedRecords());
            assertEquals(1, response.getFailedRecords().size());
            assertEquals("已订阅该权限", response.getFailedRecords().get(0).getReason());
        }

        @Test
        @DisplayName("撤回成功")
        void testWithdrawApiSubscription_Success() {
            testSubscription.setStatus(0);

            when(subscriptionMapper.selectById(200L)).thenReturn(testSubscription);
            when(subscriptionMapper.updateStatus(eq(200L), eq(3), any(Date.class), anyString())).thenReturn(1);

            WithdrawResponse response = 
                    permissionService.withdrawApiSubscription("1", "200");

            assertNotNull(response);
            assertEquals("200", response.getId());
            assertEquals(3, response.getStatus());
            assertEquals("申请已撤回", response.getMessage());
        }
    }

    @Nested
    @DisplayName("事件权限管理测试")
    class EventPermissionTests {

        @BeforeEach
        void setUpEvent() {
            testPermission.setResourceType("event");
        }

        @Test
        @DisplayName("获取订阅列表成功")
        void testGetEventSubscriptionList_Success() {
            List<Subscription> subscriptions = new ArrayList<>();
            subscriptions.add(testSubscription);

            when(subscriptionMapper.selectEventSubscriptionsByAppId(eq(1L), any(), any(), anyInt(), anyInt()))
                    .thenReturn(subscriptions);
            when(subscriptionMapper.countEventSubscriptionsByAppId(eq(1L), any(), any()))
                    .thenReturn(1L);
            when(permissionMapper.selectById(100L)).thenReturn(testPermission);
            when(categoryMapper.selectById(10L)).thenReturn(testCategory);

            ApiResponse<List<EventSubscriptionListResponse>> response = 
                    permissionService.getEventSubscriptionList("1", null, null, 1, 20);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
        }

        @Test
        @DisplayName("申请权限成功")
        void testSubscribeEventPermissions_Success() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(List.of("100"));

            List<Permission> permissions = new ArrayList<>();
            permissions.add(testPermission);

            when(permissionMapper.selectByIds(anyList())).thenReturn(permissions);
            when(subscriptionMapper.selectByAppIdAndPermissionId(eq(1L), eq(100L))).thenReturn(null);
            when(idGenerator.nextId()).thenReturn(200L);
            when(subscriptionMapper.batchInsert(anyList())).thenReturn(1);

            ApprovalRecord mockRecord = new ApprovalRecord();
            mockRecord.setId(300L);
            when(approvalEngine.createApproval(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn(mockRecord);

            PermissionSubscribeResponse response = 
                    permissionService.subscribeEventPermissions("1", request);

            assertNotNull(response);
            assertEquals(1, response.getSuccessCount());
            assertEquals(0, response.getFailedCount());
        }

        @Test
        @DisplayName("配置消费参数成功")
        void testConfigEventSubscription_Success() {
            SubscriptionConfigRequest request = new SubscriptionConfigRequest();
            request.setChannelType(1);
            request.setChannelAddress("https://example.com/webhook");
            request.setAuthType(0);

            when(subscriptionMapper.selectById(200L)).thenReturn(testSubscription);
            when(subscriptionMapper.updateConfig(eq(200L), anyInt(), anyString(), anyInt(), any(Date.class), anyString())).thenReturn(1);

            WithdrawResponse response = 
                    permissionService.configEventSubscription("1", "200", request);

            assertNotNull(response);
            assertEquals("200", response.getId());
            assertEquals("事件消费参数配置成功", response.getMessage());
            assertEquals(1, response.getChannelType());
            assertEquals("https://example.com/webhook", response.getChannelAddress());
            assertEquals(0, response.getAuthType());
        }

        @Test
        @DisplayName("撤回成功")
        void testWithdrawEventSubscription_Success() {
            testSubscription.setStatus(0);

            when(subscriptionMapper.selectById(200L)).thenReturn(testSubscription);
            when(subscriptionMapper.updateStatus(eq(200L), eq(3), any(Date.class), anyString())).thenReturn(1);

            WithdrawResponse response = 
                    permissionService.withdrawEventSubscription("1", "200");

            assertNotNull(response);
            assertEquals("200", response.getId());
            assertEquals(3, response.getStatus());
            assertEquals("申请已撤回", response.getMessage());
        }
    }

    @Nested
    @DisplayName("回调权限管理测试")
    class CallbackPermissionTests {

        @BeforeEach
        void setUpCallback() {
            testPermission.setResourceType("callback");
        }

        @Test
        @DisplayName("获取订阅列表成功")
        void testGetCallbackSubscriptionList_Success() {
            List<Subscription> subscriptions = new ArrayList<>();
            subscriptions.add(testSubscription);

            when(subscriptionMapper.selectCallbackSubscriptionsByAppId(eq(1L), any(), any(), anyInt(), anyInt()))
                    .thenReturn(subscriptions);
            when(subscriptionMapper.countCallbackSubscriptionsByAppId(eq(1L), any(), any()))
                    .thenReturn(1L);
            when(permissionMapper.selectById(100L)).thenReturn(testPermission);
            when(categoryMapper.selectById(10L)).thenReturn(testCategory);

            ApiResponse<List<CallbackSubscriptionListResponse>> response = 
                    permissionService.getCallbackSubscriptionList("1", null, null, 1, 20);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
        }

        @Test
        @DisplayName("申请权限成功")
        void testSubscribeCallbackPermissions_Success() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(List.of("100"));

            List<Permission> permissions = new ArrayList<>();
            permissions.add(testPermission);

            when(permissionMapper.selectByIds(anyList())).thenReturn(permissions);
            when(subscriptionMapper.selectByAppIdAndPermissionId(eq(1L), eq(100L))).thenReturn(null);
            when(idGenerator.nextId()).thenReturn(200L);
            when(subscriptionMapper.batchInsert(anyList())).thenReturn(1);

            ApprovalRecord mockRecord = new ApprovalRecord();
            mockRecord.setId(300L);
            when(approvalEngine.createApproval(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn(mockRecord);

            PermissionSubscribeResponse response = 
                    permissionService.subscribeCallbackPermissions("1", request);

            assertNotNull(response);
            assertEquals(1, response.getSuccessCount());
            assertEquals(0, response.getFailedCount());
        }

        @Test
        @DisplayName("配置消费参数成功")
        void testConfigCallbackSubscription_Success() {
            SubscriptionConfigRequest request = new SubscriptionConfigRequest();
            request.setChannelType(0);
            request.setChannelAddress("https://example.com/callback");
            request.setAuthType(1);

            when(subscriptionMapper.selectById(200L)).thenReturn(testSubscription);
            when(subscriptionMapper.updateConfig(eq(200L), anyInt(), anyString(), anyInt(), any(Date.class), anyString())).thenReturn(1);

            WithdrawResponse response = 
                    permissionService.configCallbackSubscription("1", "200", request);

            assertNotNull(response);
            assertEquals("200", response.getId());
            assertEquals("回调消费参数配置成功", response.getMessage());
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionTests {

        @Test
        @DisplayName("获取分类权限-分类不存在")
        void testGetCategoryApiPermissions_CategoryNotFound() {
            when(categoryMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                permissionService.getCategoryApiPermissions("999", "1", null, null, true, 1, 20);
            });

            assertEquals("404", exception.getCode());
        }

        @Test
        @DisplayName("撤回-订阅记录不存在")
        void testWithdrawApiSubscription_SubscriptionNotFound() {
            when(subscriptionMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                permissionService.withdrawApiSubscription("1", "999");
            });

            assertEquals("404", exception.getCode());
        }

        @Test
        @DisplayName("撤回-状态不是待审")
        void testWithdrawApiSubscription_InvalidStatus() {
            testSubscription.setStatus(1);

            when(subscriptionMapper.selectById(200L)).thenReturn(testSubscription);

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                permissionService.withdrawApiSubscription("1", "200");
            });

            assertEquals("400", exception.getCode());
        }

        @Test
        @DisplayName("配置消费参数-订阅记录不存在")
        void testConfigEventSubscription_SubscriptionNotFound() {
            SubscriptionConfigRequest request = new SubscriptionConfigRequest();
            request.setChannelType(1);
            request.setChannelAddress("https://example.com/webhook");
            request.setAuthType(0);

            when(subscriptionMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                permissionService.configEventSubscription("1", "999", request);
            });

            assertEquals("404", exception.getCode());
        }

        @Test
        @DisplayName("申请权限-无效ID格式")
        void testSubscribeApiPermissions_InvalidIdFormat() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(List.of("invalid"));

            assertThrows(BusinessException.class, () -> {
                permissionService.subscribeApiPermissions("1", request);
            });
        }

        @Test
        @DisplayName("应用ID无效")
        void testAppIdInvalid() {
            when(appContextResolver.resolveAndValidate("invalid_app_id"))
                    .thenThrow(AppAccessException.notFound("invalid_app_id"));

            assertThrows(AppAccessException.class, () -> {
                permissionService.getApiSubscriptionList("invalid_app_id", null, null, 1, 20);
            });
        }

        @Test
        @DisplayName("无应用访问权限")
        void testAppNoPermission() {
            when(appContextResolver.resolveAndValidate("app_without_permission"))
                    .thenThrow(AppAccessException.noPermission("app_without_permission"));

            assertThrows(AppAccessException.class, () -> {
                permissionService.subscribeApiPermissions("app_without_permission", new PermissionSubscribeRequest());
            });
        }
    }

    @Nested
    @DisplayName("分页测试")
    class PaginationTests {

        @Test
        @DisplayName("获取订阅列表-默认分页参数")
        void testGetApiSubscriptionList_DefaultPagination() {
            List<Subscription> subscriptions = new ArrayList<>();
            when(subscriptionMapper.selectApiSubscriptionsByAppId(eq(1L), any(), any(), eq(0), eq(20)))
                    .thenReturn(subscriptions);
            when(subscriptionMapper.countApiSubscriptionsByAppId(eq(1L), any(), any()))
                    .thenReturn(0L);

            ApiResponse<List<ApiSubscriptionListResponse>> response = 
                    permissionService.getApiSubscriptionList("1", null, null, null, null);

            assertNotNull(response);
            assertNotNull(response.getPage());
            assertEquals(1, response.getPage().getCurPage());
            assertEquals(20, response.getPage().getPageSize());
        }

        @Test
        @DisplayName("获取订阅列表-自定义分页参数")
        void testGetApiSubscriptionList_CustomPagination() {
            List<Subscription> subscriptions = new ArrayList<>();
            when(subscriptionMapper.selectApiSubscriptionsByAppId(eq(1L), any(), any(), eq(50), eq(50)))
                    .thenReturn(subscriptions);
            when(subscriptionMapper.countApiSubscriptionsByAppId(eq(1L), any(), any()))
                    .thenReturn(0L);

            ApiResponse<List<ApiSubscriptionListResponse>> response = 
                    permissionService.getApiSubscriptionList("1", null, null, 2, 50);

            assertNotNull(response);
            assertNotNull(response.getPage());
            assertEquals(2, response.getPage().getCurPage());
            assertEquals(50, response.getPage().getPageSize());
        }
    }

    @Nested
    @DisplayName("批量操作测试")
    class BatchOperationTests {

        @Test
        @DisplayName("批量申请权限-部分成功")
        void testSubscribeApiPermissions_PartialSuccess() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(List.of("100", "101", "102"));

            Permission permission1 = new Permission();
            permission1.setId(100L);
            permission1.setResourceType("api");
            permission1.setNameCn("权限1");

            Permission permission2 = new Permission();
            permission2.setId(101L);
            permission2.setResourceType("api");
            permission2.setNameCn("权限2");

            Permission permission3 = new Permission();
            permission3.setId(102L);
            permission3.setResourceType("api");
            permission3.setNameCn("权限3");

            List<Permission> permissions = new ArrayList<>();
            permissions.add(permission1);
            permissions.add(permission2);
            permissions.add(permission3);

            when(permissionMapper.selectByIds(anyList())).thenReturn(permissions);
            when(subscriptionMapper.selectByAppIdAndPermissionId(eq(1L), eq(100L))).thenReturn(null);
            when(subscriptionMapper.selectByAppIdAndPermissionId(eq(1L), eq(101L))).thenReturn(testSubscription);
            when(subscriptionMapper.selectByAppIdAndPermissionId(eq(1L), eq(102L))).thenReturn(null);
            when(idGenerator.nextId()).thenReturn(200L, 201L);
            when(subscriptionMapper.batchInsert(anyList())).thenReturn(1);

            ApprovalRecord mockRecord = new ApprovalRecord();
            mockRecord.setId(300L);
            when(approvalEngine.createApproval(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn(mockRecord);

            PermissionSubscribeResponse response = 
                    permissionService.subscribeApiPermissions("1", request);

            assertNotNull(response);
            assertEquals(2, response.getSuccessCount());
            assertEquals(1, response.getFailedCount());
        }

        @Test
        @DisplayName("批量申请权限-全部失败")
        void testSubscribeApiPermissions_AllFailed() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(List.of("100", "101"));

            Permission permission1 = new Permission();
            permission1.setId(100L);
            permission1.setResourceType("api");

            Permission permission2 = new Permission();
            permission2.setId(101L);
            permission2.setResourceType("api");

            List<Permission> permissions = new ArrayList<>();
            permissions.add(permission1);
            permissions.add(permission2);

            when(permissionMapper.selectByIds(anyList())).thenReturn(permissions);
            when(subscriptionMapper.selectByAppIdAndPermissionId(anyLong(), anyLong())).thenReturn(testSubscription);

            PermissionSubscribeResponse response = 
                    permissionService.subscribeApiPermissions("1", request);

            assertNotNull(response);
            assertEquals(0, response.getSuccessCount());
            assertEquals(2, response.getFailedCount());
        }
    }
}
