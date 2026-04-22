package com.xxx.api.scope.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.api.common.model.ApiResponse;
import com.xxx.api.scope.dto.*;
import com.xxx.api.scope.entity.UserAuthorization;
import com.xxx.api.scope.mapper.UserAuthorizationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Scope 服务测试
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@DisplayName("Scope 服务测试")
class ScopeServiceTest {

    @Mock
    private UserAuthorizationMapper userAuthorizationMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ScopeService scopeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("获取用户授权列表 - 成功")
    void testGetUserAuthorizations() {
        // 准备数据
        UserAuthorizationListRequest request = new UserAuthorizationListRequest();
        request.setCurPage(1);
        request.setPageSize(20);

        List<UserAuthorization> mockList = new ArrayList<>();
        UserAuthorization auth = new UserAuthorization();
        auth.setId(600L);
        auth.setUserId("user001");
        auth.setAppId(10L);
        auth.setScopes("[\"api:im:send-message\"]");
        auth.setCreateTime(new Date());
        mockList.add(auth);

        when(userAuthorizationMapper.selectList(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(mockList);
        when(userAuthorizationMapper.countList(any(), any(), any()))
                .thenReturn(1L);

        // 执行测试
        ApiResponse<List<UserAuthorizationListResponse>> response = 
                scopeService.getUserAuthorizations(request);

        // 验证结果
        assertNotNull(response);
        assertEquals("200", response.getCode());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("600", response.getData().get(0).getId());
        assertEquals("user001", response.getData().get(0).getUserId());

        // 验证调用
        verify(userAuthorizationMapper, times(1)).selectList(any(), any(), any(), anyInt(), anyInt());
        verify(userAuthorizationMapper, times(1)).countList(any(), any(), any());
    }

    @Test
    @DisplayName("创建用户授权 - 成功")
    void testCreateUserAuthorization() {
        // 准备数据
        UserAuthorizationCreateRequest request = new UserAuthorizationCreateRequest();
        request.setUserId("user001");
        request.setAppId("10");
        request.setScopes(List.of("api:im:send-message", "api:im:get-message"));
        request.setExpiresAt(new Date());

        when(userAuthorizationMapper.selectByUserIdAndAppId(anyString(), anyLong()))
                .thenReturn(null);
        when(userAuthorizationMapper.insert(any(UserAuthorization.class)))
                .thenReturn(1);

        // 执行测试
        ApiResponse<UserAuthorizationResponse> response = 
                scopeService.createUserAuthorization(request);

        // 验证结果
        assertNotNull(response);
        assertEquals("200", response.getCode());
        assertNotNull(response.getData());
        assertEquals("user001", response.getData().getUserId());
        assertEquals("10", response.getData().getAppId());
        assertNotNull(response.getData().getScopes());
        assertEquals(2, response.getData().getScopes().size());

        // 验证调用
        verify(userAuthorizationMapper, times(1)).selectByUserIdAndAppId(anyString(), anyLong());
        verify(userAuthorizationMapper, times(1)).insert(any(UserAuthorization.class));
    }

    @Test
    @DisplayName("取消授权 - 成功")
    void testRevokeUserAuthorization() {
        // 准备数据
        String authId = "600";

        UserAuthorization existing = new UserAuthorization();
        existing.setId(600L);
        existing.setUserId("user001");
        existing.setAppId(10L);
        existing.setRevokedAt(null);

        when(userAuthorizationMapper.selectById(anyLong()))
                .thenReturn(existing);
        when(userAuthorizationMapper.revokeById(anyLong()))
                .thenReturn(1);

        // 执行测试
        ApiResponse<Void> response = scopeService.revokeUserAuthorization(authId);

        // 验证结果
        assertNotNull(response);
        assertEquals("200", response.getCode());

        // 验证调用
        verify(userAuthorizationMapper, times(1)).selectById(anyLong());
        verify(userAuthorizationMapper, times(1)).revokeById(anyLong());
    }
}
