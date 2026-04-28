package com.xxx.it.works.wecode.v2.modules.permission.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.permission.dto.*;
import com.xxx.it.works.wecode.v2.modules.permission.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("权限管理控制器测试")
class PermissionControllerTest {

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PermissionController permissionController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("API 权限管理测试")
    class ApiPermissionTests {

        @Test
        @DisplayName("#27 获取应用 API 权限列表成功")
        void testGetApiSubscriptionList_Success() {
            ApiSubscriptionListResponse response1 = ApiSubscriptionListResponse.builder()
                    .id("sub001")
                    .appId("app001")
                    .permissionId("perm001")
                    .permission(ApiSubscriptionListResponse.PermissionInfo.builder()
                            .nameCn("API权限1")
                            .scope("api:v1:test:get")
                            .build())
                    .api(ApiSubscriptionListResponse.ApiInfo.builder()
                            .path("/api/v1/test")
                            .method("GET")
                            .authType(5)
                            .build())
                    .category(ApiSubscriptionListResponse.CategoryInfo.builder()
                            .id("cat001")
                            .nameCn("测试分类")
                            .path("/root/test")
                            .build())
                    .status(1)
                    .authType(5)
                    .build();

            ApiResponse.PageResponse pageResponse = ApiResponse.PageResponse.builder()
                    .curPage(1)
                    .pageSize(20)
                    .total(1L)
                    .totalPages(1)
                    .build();

            ApiResponse<List<ApiSubscriptionListResponse>> mockResponse = ApiResponse.success(
                    Collections.singletonList(response1), pageResponse);

            when(permissionService.getApiSubscriptionList("app001", null, null, 1, 20))
                    .thenReturn(mockResponse);

            ApiResponse<List<ApiSubscriptionListResponse>> response = permissionController
                    .getApiSubscriptionList("app001", null, null, 1, 20);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertEquals("sub001", response.getData().get(0).getId());
            assertEquals("app001", response.getData().get(0).getAppId());
            assertNotNull(response.getPage());
            assertEquals(1, response.getPage().getTotal());
            verify(permissionService).getApiSubscriptionList("app001", null, null, 1, 20);
        }

        @Test
        @DisplayName("#27 获取应用 API 权限列表带状态过滤")
        void testGetApiSubscriptionList_WithStatusFilter() {
            ApiSubscriptionListResponse response1 = ApiSubscriptionListResponse.builder()
                    .id("sub002")
                    .appId("app001")
                    .permissionId("perm002")
                    .status(0)
                    .build();

            ApiResponse<List<ApiSubscriptionListResponse>> mockResponse = ApiResponse.success(
                    Collections.singletonList(response1));

            when(permissionService.getApiSubscriptionList("app001", 0, "test", 1, 10))
                    .thenReturn(mockResponse);

            ApiResponse<List<ApiSubscriptionListResponse>> response = permissionController
                    .getApiSubscriptionList("app001", 0, "test", 1, 10);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertEquals(1, response.getData().size());
            assertEquals(0, response.getData().get(0).getStatus());
            verify(permissionService).getApiSubscriptionList("app001", 0, "test", 1, 10);
        }

        @Test
        @DisplayName("#29 申请 API 权限成功")
        void testSubscribeApiPermissions_Success() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(Arrays.asList("perm001", "perm002"));

            PermissionSubscribeResponse.SubscriptionRecord record1 = PermissionSubscribeResponse.SubscriptionRecord.builder()
                    .id("sub001")
                    .appId("app001")
                    .permissionId("perm001")
                    .status(0)
                    .build();

            PermissionSubscribeResponse.SubscriptionRecord record2 = PermissionSubscribeResponse.SubscriptionRecord.builder()
                    .id("sub002")
                    .appId("app001")
                    .permissionId("perm002")
                    .status(0)
                    .build();

            PermissionSubscribeResponse subscribeResponse = PermissionSubscribeResponse.builder()
                    .successCount(2)
                    .failedCount(0)
                    .records(Arrays.asList(record1, record2))
                    .build();

            when(permissionService.subscribeApiPermissions(eq("app001"), any(PermissionSubscribeRequest.class)))
                    .thenReturn(subscribeResponse);

            ApiResponse<PermissionSubscribeResponse> response = permissionController
                    .subscribeApiPermissions("app001", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(2, response.getData().getSuccessCount());
            assertEquals(0, response.getData().getFailedCount());
            assertEquals(2, response.getData().getRecords().size());
            verify(permissionService).subscribeApiPermissions(eq("app001"), any(PermissionSubscribeRequest.class));
        }

        @Test
        @DisplayName("#29 申请 API 权限部分成功")
        void testSubscribeApiPermissions_PartialSuccess() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(Arrays.asList("perm001", "perm002", "perm003"));

            PermissionSubscribeResponse.SubscriptionRecord record = PermissionSubscribeResponse.SubscriptionRecord.builder()
                    .id("sub001")
                    .appId("app001")
                    .permissionId("perm001")
                    .status(0)
                    .build();

            PermissionSubscribeResponse.FailedRecord failedRecord1 = PermissionSubscribeResponse.FailedRecord.builder()
                    .permissionId("perm002")
                    .reason("权限已申请")
                    .build();

            PermissionSubscribeResponse.FailedRecord failedRecord2 = PermissionSubscribeResponse.FailedRecord.builder()
                    .permissionId("perm003")
                    .reason("权限不存在")
                    .build();

            PermissionSubscribeResponse subscribeResponse = PermissionSubscribeResponse.builder()
                    .successCount(1)
                    .failedCount(2)
                    .records(Collections.singletonList(record))
                    .failedRecords(Arrays.asList(failedRecord1, failedRecord2))
                    .build();

            when(permissionService.subscribeApiPermissions(eq("app001"), any(PermissionSubscribeRequest.class)))
                    .thenReturn(subscribeResponse);

            ApiResponse<PermissionSubscribeResponse> response = permissionController
                    .subscribeApiPermissions("app001", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertEquals(1, response.getData().getSuccessCount());
            assertEquals(2, response.getData().getFailedCount());
            assertEquals(2, response.getData().getFailedRecords().size());
            verify(permissionService).subscribeApiPermissions(eq("app001"), any(PermissionSubscribeRequest.class));
        }

        @Test
        @DisplayName("#31 删除 API 权限订阅成功")
        void testDeleteApiSubscription_Success() {
            WithdrawResponse withdrawResponse = WithdrawResponse.builder()
                    .id("sub001")
                    .message("订阅记录删除成功")
                    .build();

            when(permissionService.deleteApiSubscription("app001", "sub001"))
                    .thenReturn(withdrawResponse);

            ApiResponse<WithdrawResponse> response = permissionController
                    .deleteApiSubscription("app001", "sub001");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("sub001", response.getData().getId());
            assertEquals("订阅记录删除成功", response.getData().getMessage());
            verify(permissionService).deleteApiSubscription("app001", "sub001");
        }
    }

    @Nested
    @DisplayName("事件权限管理测试")
    class EventPermissionTests {

        @Test
        @DisplayName("#31 获取应用事件订阅列表成功")
        void testGetEventSubscriptionList_Success() {
            EventSubscriptionListResponse response1 = EventSubscriptionListResponse.builder()
                    .id("esub001")
                    .appId("app001")
                    .permissionId("eperm001")
                    .permission(EventSubscriptionListResponse.PermissionInfo.builder()
                            .nameCn("事件权限1")
                            .scope("event:v1:user:created")
                            .build())
                    .event(EventSubscriptionListResponse.EventInfo.builder()
                            .topic("user.created")
                            .docUrl("https://docs.example.com/events/user.created")
                            .build())
                    .category(EventSubscriptionListResponse.CategoryInfo.builder()
                            .id("cat001")
                            .nameCn("用户事件")
                            .path("/events/user")
                            .build())
                    .status(1)
                    .channelType(0)
                    .authType(0)
                    .build();

            ApiResponse.PageResponse pageResponse = ApiResponse.PageResponse.builder()
                    .curPage(1)
                    .pageSize(20)
                    .total(1L)
                    .totalPages(1)
                    .build();

            ApiResponse<List<EventSubscriptionListResponse>> mockResponse = ApiResponse.success(
                    Collections.singletonList(response1), pageResponse);

            when(permissionService.getEventSubscriptionList("app001", null, null, 1, 20))
                    .thenReturn(mockResponse);

            ApiResponse<List<EventSubscriptionListResponse>> response = permissionController
                    .getEventSubscriptionList("app001", null, null, 1, 20);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertEquals("esub001", response.getData().get(0).getId());
            assertEquals("app001", response.getData().get(0).getAppId());
            assertEquals("user.created", response.getData().get(0).getEvent().getTopic());
            assertNotNull(response.getPage());
            verify(permissionService).getEventSubscriptionList("app001", null, null, 1, 20);
        }

        @Test
        @DisplayName("#31 获取应用事件订阅列表带关键词搜索")
        void testGetEventSubscriptionList_WithKeyword() {
            EventSubscriptionListResponse response1 = EventSubscriptionListResponse.builder()
                    .id("esub002")
                    .appId("app001")
                    .permissionId("eperm002")
                    .status(1)
                    .build();

            ApiResponse<List<EventSubscriptionListResponse>> mockResponse = ApiResponse.success(
                    Collections.singletonList(response1));

            when(permissionService.getEventSubscriptionList("app001", 1, "user", 1, 10))
                    .thenReturn(mockResponse);

            ApiResponse<List<EventSubscriptionListResponse>> response = permissionController
                    .getEventSubscriptionList("app001", 1, "user", 1, 10);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertEquals(1, response.getData().size());
            verify(permissionService).getEventSubscriptionList("app001", 1, "user", 1, 10);
        }

        @Test
        @DisplayName("#33 申请事件权限成功")
        void testSubscribeEventPermissions_Success() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(Arrays.asList("eperm001", "eperm002"));

            PermissionSubscribeResponse.SubscriptionRecord record = PermissionSubscribeResponse.SubscriptionRecord.builder()
                    .id("esub001")
                    .appId("app001")
                    .permissionId("eperm001")
                    .status(0)
                    .build();

            PermissionSubscribeResponse subscribeResponse = PermissionSubscribeResponse.builder()
                    .successCount(2)
                    .failedCount(0)
                    .records(Collections.singletonList(record))
                    .build();

            when(permissionService.subscribeEventPermissions(eq("app001"), any(PermissionSubscribeRequest.class)))
                    .thenReturn(subscribeResponse);

            ApiResponse<PermissionSubscribeResponse> response = permissionController
                    .subscribeEventPermissions("app001", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(2, response.getData().getSuccessCount());
            verify(permissionService).subscribeEventPermissions(eq("app001"), any(PermissionSubscribeRequest.class));
        }

        @Test
        @DisplayName("#34 配置事件消费参数成功")
        void testConfigEventSubscription_Success() {
            SubscriptionConfigRequest request = new SubscriptionConfigRequest();
            request.setChannelType(1);
            request.setChannelAddress("https://webhook.example.com/event");
            request.setAuthType(0);

            WithdrawResponse withdrawResponse = WithdrawResponse.builder()
                    .id("esub001")
                    .status(1)
                    .message("配置成功")
                    .channelType(1)
                    .channelAddress("https://webhook.example.com/event")
                    .authType(0)
                    .build();

            when(permissionService.configEventSubscription(eq("app001"), eq("esub001"), any(SubscriptionConfigRequest.class)))
                    .thenReturn(withdrawResponse);

            ApiResponse<WithdrawResponse> response = permissionController
                    .configEventSubscription("app001", "esub001", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("esub001", response.getData().getId());
            assertEquals(1, response.getData().getChannelType());
            assertEquals("https://webhook.example.com/event", response.getData().getChannelAddress());
            verify(permissionService).configEventSubscription(eq("app001"), eq("esub001"), any(SubscriptionConfigRequest.class));
        }

        @Test
        @DisplayName("#34 配置事件消费参数-内部消息队列")
        void testConfigEventSubscription_InternalQueue() {
            SubscriptionConfigRequest request = new SubscriptionConfigRequest();
            request.setChannelType(0);
            request.setAuthType(1);

            WithdrawResponse withdrawResponse = WithdrawResponse.builder()
                    .id("esub002")
                    .status(1)
                    .message("配置成功")
                    .channelType(0)
                    .authType(1)
                    .build();

            when(permissionService.configEventSubscription(eq("app001"), eq("esub002"), any(SubscriptionConfigRequest.class)))
                    .thenReturn(withdrawResponse);

            ApiResponse<WithdrawResponse> response = permissionController
                    .configEventSubscription("app001", "esub002", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertEquals(0, response.getData().getChannelType());
            verify(permissionService).configEventSubscription(eq("app001"), eq("esub002"), any(SubscriptionConfigRequest.class));
        }

        @Test
        @DisplayName("#37 删除事件权限订阅成功")
        void testDeleteEventSubscription_Success() {
            WithdrawResponse withdrawResponse = WithdrawResponse.builder()
                    .id("esub001")
                    .message("订阅记录删除成功")
                    .build();

            when(permissionService.deleteEventSubscription("app001", "esub001"))
                    .thenReturn(withdrawResponse);

            ApiResponse<WithdrawResponse> response = permissionController
                    .deleteEventSubscription("app001", "esub001");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("esub001", response.getData().getId());
            assertEquals("订阅记录删除成功", response.getData().getMessage());
            verify(permissionService).deleteEventSubscription("app001", "esub001");
        }
    }

    @Nested
    @DisplayName("回调权限管理测试")
    class CallbackPermissionTests {

        @Test
        @DisplayName("#36 获取应用回调订阅列表成功")
        void testGetCallbackSubscriptionList_Success() {
            CallbackSubscriptionListResponse response1 = CallbackSubscriptionListResponse.builder()
                    .id("csub001")
                    .appId("app001")
                    .permissionId("cperm001")
                    .permission(CallbackSubscriptionListResponse.PermissionInfo.builder()
                            .nameCn("回调权限1")
                            .scope("callback:v1:order:created")
                            .build())
                    .callback(CallbackSubscriptionListResponse.CallbackInfo.builder()
                            .nameCn("订单创建回调")
                            .docUrl("https://docs.example.com/callbacks/order.created")
                            .build())
                    .category(CallbackSubscriptionListResponse.CategoryInfo.builder()
                            .id("cat001")
                            .nameCn("订单回调")
                            .path("/callbacks/order")
                            .build())
                    .status(1)
                    .channelType(0)
                    .authType(0)
                    .build();

            ApiResponse.PageResponse pageResponse = ApiResponse.PageResponse.builder()
                    .curPage(1)
                    .pageSize(20)
                    .total(1L)
                    .totalPages(1)
                    .build();

            ApiResponse<List<CallbackSubscriptionListResponse>> mockResponse = ApiResponse.success(
                    Collections.singletonList(response1), pageResponse);

            when(permissionService.getCallbackSubscriptionList("app001", null, null, 1, 20))
                    .thenReturn(mockResponse);

            ApiResponse<List<CallbackSubscriptionListResponse>> response = permissionController
                    .getCallbackSubscriptionList("app001", null, null, 1, 20);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertEquals("csub001", response.getData().get(0).getId());
            assertEquals("app001", response.getData().get(0).getAppId());
            assertEquals("订单创建回调", response.getData().get(0).getCallback().getNameCn());
            assertNotNull(response.getPage());
            verify(permissionService).getCallbackSubscriptionList("app001", null, null, 1, 20);
        }

        @Test
        @DisplayName("#36 获取应用回调订阅列表带状态过滤")
        void testGetCallbackSubscriptionList_WithStatus() {
            CallbackSubscriptionListResponse response1 = CallbackSubscriptionListResponse.builder()
                    .id("csub002")
                    .appId("app001")
                    .permissionId("cperm002")
                    .status(0)
                    .build();

            ApiResponse<List<CallbackSubscriptionListResponse>> mockResponse = ApiResponse.success(
                    Collections.singletonList(response1));

            when(permissionService.getCallbackSubscriptionList("app001", 0, "order", 1, 10))
                    .thenReturn(mockResponse);

            ApiResponse<List<CallbackSubscriptionListResponse>> response = permissionController
                    .getCallbackSubscriptionList("app001", 0, "order", 1, 10);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertEquals(1, response.getData().size());
            assertEquals(0, response.getData().get(0).getStatus());
            verify(permissionService).getCallbackSubscriptionList("app001", 0, "order", 1, 10);
        }

        @Test
        @DisplayName("#38 申请回调权限成功")
        void testSubscribeCallbackPermissions_Success() {
            PermissionSubscribeRequest request = new PermissionSubscribeRequest();
            request.setPermissionIds(Arrays.asList("cperm001", "cperm002"));

            PermissionSubscribeResponse.SubscriptionRecord record = PermissionSubscribeResponse.SubscriptionRecord.builder()
                    .id("csub001")
                    .appId("app001")
                    .permissionId("cperm001")
                    .status(0)
                    .build();

            PermissionSubscribeResponse subscribeResponse = PermissionSubscribeResponse.builder()
                    .successCount(2)
                    .failedCount(0)
                    .records(Collections.singletonList(record))
                    .build();

            when(permissionService.subscribeCallbackPermissions(eq("app001"), any(PermissionSubscribeRequest.class)))
                    .thenReturn(subscribeResponse);

            ApiResponse<PermissionSubscribeResponse> response = permissionController
                    .subscribeCallbackPermissions("app001", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals(2, response.getData().getSuccessCount());
            verify(permissionService).subscribeCallbackPermissions(eq("app001"), any(PermissionSubscribeRequest.class));
        }

        @Test
        @DisplayName("#39 配置回调消费参数-WebHook")
        void testConfigCallbackSubscription_WebHook() {
            SubscriptionConfigRequest request = new SubscriptionConfigRequest();
            request.setChannelType(0);
            request.setChannelAddress("https://webhook.example.com/callback");
            request.setAuthType(0);

            WithdrawResponse withdrawResponse = WithdrawResponse.builder()
                    .id("csub001")
                    .status(1)
                    .message("配置成功")
                    .channelType(0)
                    .channelAddress("https://webhook.example.com/callback")
                    .authType(0)
                    .build();

            when(permissionService.configCallbackSubscription(eq("app001"), eq("csub001"), any(SubscriptionConfigRequest.class)))
                    .thenReturn(withdrawResponse);

            ApiResponse<WithdrawResponse> response = permissionController
                    .configCallbackSubscription("app001", "csub001", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("csub001", response.getData().getId());
            assertEquals(0, response.getData().getChannelType());
            assertEquals("https://webhook.example.com/callback", response.getData().getChannelAddress());
            verify(permissionService).configCallbackSubscription(eq("app001"), eq("csub001"), any(SubscriptionConfigRequest.class));
        }

        @Test
        @DisplayName("#39 配置回调消费参数-WebSocket")
        void testConfigCallbackSubscription_WebSocket() {
            SubscriptionConfigRequest request = new SubscriptionConfigRequest();
            request.setChannelType(2);
            request.setChannelAddress("wss://ws.example.com/callback");
            request.setAuthType(1);

            WithdrawResponse withdrawResponse = WithdrawResponse.builder()
                    .id("csub002")
                    .status(1)
                    .message("配置成功")
                    .channelType(2)
                    .channelAddress("wss://ws.example.com/callback")
                    .authType(1)
                    .build();

            when(permissionService.configCallbackSubscription(eq("app001"), eq("csub002"), any(SubscriptionConfigRequest.class)))
                    .thenReturn(withdrawResponse);

            ApiResponse<WithdrawResponse> response = permissionController
                    .configCallbackSubscription("app001", "csub002", request);

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertEquals(2, response.getData().getChannelType());
            assertEquals("wss://ws.example.com/callback", response.getData().getChannelAddress());
            verify(permissionService).configCallbackSubscription(eq("app001"), eq("csub002"), any(SubscriptionConfigRequest.class));
        }

        @Test
        @DisplayName("#43 删除回调权限订阅成功")
        void testDeleteCallbackSubscription_Success() {
            WithdrawResponse withdrawResponse = WithdrawResponse.builder()
                    .id("csub001")
                    .message("订阅记录删除成功")
                    .build();

            when(permissionService.deleteCallbackSubscription("app001", "csub001"))
                    .thenReturn(withdrawResponse);

            ApiResponse<WithdrawResponse> response = permissionController
                    .deleteCallbackSubscription("app001", "csub001");

            assertNotNull(response);
            assertEquals("200", response.getCode());
            assertNotNull(response.getData());
            assertEquals("csub001", response.getData().getId());
            assertEquals("订阅记录删除成功", response.getData().getMessage());
            verify(permissionService).deleteCallbackSubscription("app001", "csub001");
        }
    }
}