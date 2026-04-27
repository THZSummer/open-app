package com.xxx.it.works.wecode.v2.modules.callback.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.callback.dto.*;
import com.xxx.it.works.wecode.v2.modules.callback.service.CallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("回调管理控制器测试")
@ExtendWith(MockitoExtension.class)
class CallbackControllerTest {

    @Mock
    private CallbackService callbackService;

    @InjectMocks
    private CallbackController callbackController;

    private CallbackResponse testCallbackResponse;
    private CallbackListResponse testCallbackListResponse;
    private PermissionDto testPermissionDto;

    @BeforeEach
    void setUp() {
        testPermissionDto = PermissionDto.builder()
                .id("1001")
                .nameCn("测试权限")
                .nameEn("Test Permission")
                .scope("callback:test:demo")
                .status(1)
                .needApproval(1)
                .build();

        testCallbackResponse = CallbackResponse.builder()
                .id("1")
                .nameCn("测试回调")
                .nameEn("Test Callback")
                .categoryId("1")
                .categoryName("测试分类")
                .status(2)
                .permission(testPermissionDto)
                .properties(new ArrayList<>())
                .createTime(new Date())
                .createBy("user001")
                .lastUpdateTime(new Date())
                .lastUpdateBy("user001")
                .build();

        testCallbackListResponse = CallbackListResponse.builder()
                .id("1")
                .nameCn("测试回调")
                .nameEn("Test Callback")
                .categoryId("1")
                .categoryName("测试分类")
                .status(2)
                .permission(testPermissionDto)
                .createTime(new Date())
                .docUrl("https://example.com/doc")
                .build();
    }

    @Test
    @DisplayName("获取回调列表 - 成功")
    void testGetCallbackList_Success() {
        List<CallbackListResponse> callbackList = new ArrayList<>();
        callbackList.add(testCallbackListResponse);

        ApiResponse<List<CallbackListResponse>> expectedResponse = ApiResponse.success(callbackList);
        expectedResponse.setPage(ApiResponse.PageResponse.builder()
                .curPage(1)
                .pageSize(20)
                .total(1L)
                .totalPages(1)
                .build());

        when(callbackService.getCallbackList(eq("1"), eq(2), eq("test"), eq(1), eq(20)))
                .thenReturn(expectedResponse);

        ApiResponse<List<CallbackListResponse>> response = callbackController.getCallbackList(
                "1", 2, "test", 1, 20);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("测试回调", response.getData().get(0).getNameCn());
        assertEquals("https://example.com/doc", response.getData().get(0).getDocUrl());

        verify(callbackService, times(1)).getCallbackList("1", 2, "test", 1, 20);
    }

    @Test
    @DisplayName("获取回调详情 - 成功")
    void testGetCallbackById_Success() {
        when(callbackService.getCallbackById(1L)).thenReturn(testCallbackResponse);

        ApiResponse<CallbackResponse> response = callbackController.getCallbackById("1");

        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals("1", response.getData().getId());
        assertEquals("测试回调", response.getData().getNameCn());
        assertEquals("Test Callback", response.getData().getNameEn());
        assertNotNull(response.getData().getPermission());
        assertEquals("callback:test:demo", response.getData().getPermission().getScope());

        verify(callbackService, times(1)).getCallbackById(1L);
    }

    @Test
    @DisplayName("注册回调 - 成功")
    void testCreateCallback_Success() {
        CallbackCreateRequest request = new CallbackCreateRequest();
        request.setNameCn("新回调");
        request.setNameEn("New Callback");
        request.setCategoryId("1");
        
        PermissionDefinitionDto permissionDto = new PermissionDefinitionDto();
        permissionDto.setNameCn("新权限");
        permissionDto.setNameEn("New Permission");
        permissionDto.setScope("callback:test:new");
        permissionDto.setNeedApproval(1);
        request.setPermission(permissionDto);

        CallbackResponse newCallbackResponse = CallbackResponse.builder()
                .id("2")
                .nameCn("新回调")
                .nameEn("New Callback")
                .categoryId("1")
                .status(1)
                .permission(PermissionDto.builder()
                        .id("1002")
                        .nameCn("新权限")
                        .nameEn("New Permission")
                        .scope("callback:test:new")
                        .status(1)
                        .needApproval(1)
                        .build())
                .createTime(new Date())
                .build();

        when(callbackService.createCallback(any(CallbackCreateRequest.class)))
                .thenReturn(newCallbackResponse);

        ApiResponse<CallbackResponse> response = callbackController.createCallback(request);

        assertNotNull(response);
        assertEquals("2", response.getData().getId());
        assertEquals("新回调", response.getData().getNameCn());
        assertEquals("回调注册成功，等待审批", response.getMessageZh());
        assertEquals("Callback registered successfully, waiting for approval", response.getMessageEn());

        verify(callbackService, times(1)).createCallback(any(CallbackCreateRequest.class));
    }

    @Test
    @DisplayName("更新回调 - 成功")
    void testUpdateCallback_Success() {
        CallbackUpdateRequest request = new CallbackUpdateRequest();
        request.setNameCn("更新后的回调");
        request.setNameEn("Updated Callback");

        CallbackResponse updatedResponse = CallbackResponse.builder()
                .id("1")
                .nameCn("更新后的回调")
                .nameEn("Updated Callback")
                .categoryId("1")
                .status(2)
                .permission(testPermissionDto)
                .createTime(new Date())
                .lastUpdateTime(new Date())
                .build();

        when(callbackService.updateCallback(eq(1L), any(CallbackUpdateRequest.class)))
                .thenReturn(updatedResponse);

        ApiResponse<CallbackResponse> response = callbackController.updateCallback("1", request);

        assertNotNull(response);
        assertEquals("1", response.getData().getId());
        assertEquals("更新后的回调", response.getData().getNameCn());
        assertEquals("Updated Callback", response.getData().getNameEn());

        verify(callbackService, times(1)).updateCallback(eq(1L), any(CallbackUpdateRequest.class));
    }

    @Test
    @DisplayName("删除回调 - 成功")
    void testDeleteCallback_Success() {
        doNothing().when(callbackService).deleteCallback(1L);

        ApiResponse<Void> response = callbackController.deleteCallback("1");

        assertNotNull(response);
        assertNull(response.getData());

        verify(callbackService, times(1)).deleteCallback(1L);
    }

    @Test
    @DisplayName("撤回回调 - 成功")
    void testWithdrawCallback_Success() {
        CallbackResponse withdrawnResponse = CallbackResponse.builder()
                .id("1")
                .nameCn("测试回调")
                .nameEn("Test Callback")
                .categoryId("1")
                .status(0)
                .permission(testPermissionDto)
                .createTime(new Date())
                .lastUpdateTime(new Date())
                .build();

        when(callbackService.withdrawCallback(1L)).thenReturn(withdrawnResponse);

        ApiResponse<CallbackResponse> response = callbackController.withdrawCallback("1");

        assertNotNull(response);
        assertEquals("1", response.getData().getId());
        assertEquals(0, response.getData().getStatus());
        assertEquals("回调已撤回，状态变为草稿", response.getMessageZh());
        assertEquals("Callback withdrawn, status changed to draft", response.getMessageEn());

        verify(callbackService, times(1)).withdrawCallback(1L);
    }

    @Test
    @DisplayName("获取回调详情 - ID格式无效")
    void testGetCallbackById_InvalidIdFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            callbackController.getCallbackById("invalid-id");
        });
    }

    @Test
    @DisplayName("删除回调 - ID格式无效")
    void testDeleteCallback_InvalidIdFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            callbackController.deleteCallback("invalid-id");
        });
    }

    @Test
    @DisplayName("更新回调 - ID格式无效")
    void testUpdateCallback_InvalidIdFormat() {
        CallbackUpdateRequest request = new CallbackUpdateRequest();
        request.setNameCn("更新后的回调");

        assertThrows(IllegalArgumentException.class, () -> {
            callbackController.updateCallback("invalid-id", request);
        });
    }

    @Test
    @DisplayName("撤回回调 - ID格式无效")
    void testWithdrawCallback_InvalidIdFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            callbackController.withdrawCallback("invalid-id");
        });
    }
}