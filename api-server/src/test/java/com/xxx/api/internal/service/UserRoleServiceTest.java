package com.xxx.api.internal.service;

import com.xxx.api.internal.dto.UserRoleQueryRequest;
import com.xxx.api.internal.dto.UserRoleQueryResponse;
import com.xxx.api.internal.entity.AppEntity;
import com.xxx.api.internal.entity.AppMemberEntity;
import com.xxx.api.internal.mapper.AppEntityMapper;
import com.xxx.api.internal.mapper.AppMemberEntityMapper;
import com.xxx.api.internal.service.impl.UserRoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("用户角色查询服务测试")
class UserRoleServiceTest {

    @Mock
    private AppEntityMapper appEntityMapper;

    @Mock
    private AppMemberEntityMapper appMemberEntityMapper;

    @InjectMocks
    private UserRoleServiceImpl userRoleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("queryUserRoles 测试")
    class QueryUserRolesTests {

        @Test
        @DisplayName("用户有多个角色 - 返回完整角色列表")
        void testQueryUserRoles_MultipleRoles() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app-001");
            request.setUserAccount("zhangsan@xxx.com");

            AppEntity app = new AppEntity();
            app.setId(1001L);
            app.setAppId("test-app-001");
            when(appEntityMapper.selectByAppId("test-app-001")).thenReturn(app);

            AppMemberEntity member1 = new AppMemberEntity();
            member1.setMemberType(1);
            AppMemberEntity member2 = new AppMemberEntity();
            member2.setMemberType(2);
            when(appMemberEntityMapper.selectByAppIdAndAccountId(1001L, "zhangsan@xxx.com"))
                    .thenReturn(Arrays.asList(member1, member2));

            UserRoleQueryResponse response = userRoleService.queryUserRoles(request, "test-app-001");

            assertNotNull(response);
            assertEquals("test-app-001", response.getAppId());
            assertEquals(2, response.getRoles().length);
            assertArrayEquals(new Integer[]{1, 2}, response.getRoles());

            verify(appEntityMapper).selectByAppId("test-app-001");
            verify(appMemberEntityMapper).selectByAppIdAndAccountId(1001L, "zhangsan@xxx.com");
        }

        @Test
        @DisplayName("用户无角色 - 返回空列表")
        void testQueryUserRoles_NoRoles() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app-002");
            request.setUserAccount("unknown@xxx.com");

            AppEntity app = new AppEntity();
            app.setId(1002L);
            app.setAppId("test-app-002");
            when(appEntityMapper.selectByAppId("test-app-002")).thenReturn(app);
            when(appMemberEntityMapper.selectByAppIdAndAccountId(1002L, "unknown@xxx.com"))
                    .thenReturn(Collections.emptyList());

            UserRoleQueryResponse response = userRoleService.queryUserRoles(request, "test-app-002");

            assertNotNull(response);
            assertEquals("test-app-002", response.getAppId());
            assertEquals(0, response.getRoles().length);

            verify(appEntityMapper).selectByAppId("test-app-002");
            verify(appMemberEntityMapper).selectByAppIdAndAccountId(1002L, "unknown@xxx.com");
        }

        @Test
        @DisplayName("应用不存在 - 返回空角色列表")
        void testQueryUserRoles_AppNotFound() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("nonexistent");
            request.setUserAccount("test@xxx.com");

            when(appEntityMapper.selectByAppId("nonexistent")).thenReturn(null);

            UserRoleQueryResponse response = userRoleService.queryUserRoles(request, "nonexistent");

            assertNotNull(response);
            assertEquals("nonexistent", response.getAppId());
            assertEquals(0, response.getRoles().length);

            verify(appEntityMapper).selectByAppId("nonexistent");
            verify(appMemberEntityMapper, never()).selectByAppIdAndAccountId(anyLong(), anyString());
        }

        @Test
        @DisplayName("用户仅有一个角色")
        void testQueryUserRoles_SingleRole() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app-003");
            request.setUserAccount("lisi@xxx.com");

            AppEntity app = new AppEntity();
            app.setId(1003L);
            app.setAppId("test-app-003");
            when(appEntityMapper.selectByAppId("test-app-003")).thenReturn(app);

            AppMemberEntity member = new AppMemberEntity();
            member.setMemberType(0);
            when(appMemberEntityMapper.selectByAppIdAndAccountId(1003L, "lisi@xxx.com"))
                    .thenReturn(Collections.singletonList(member));

            UserRoleQueryResponse response = userRoleService.queryUserRoles(request, "test-app-003");

            assertNotNull(response);
            assertEquals("test-app-003", response.getAppId());
            assertEquals(1, response.getRoles().length);
            assertEquals(0, response.getRoles()[0]);

            verify(appEntityMapper).selectByAppId("test-app-003");
            verify(appMemberEntityMapper).selectByAppIdAndAccountId(1003L, "lisi@xxx.com");
        }
    }
}
