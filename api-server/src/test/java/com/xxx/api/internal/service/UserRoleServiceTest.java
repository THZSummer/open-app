package com.xxx.api.internal.service;

import com.xxx.api.common.exception.BusinessException;
import com.xxx.api.internal.auth.SysTokenResolver;
import com.xxx.api.internal.config.InternalAuthProperties;
import com.xxx.api.internal.dto.UserRoleQueryRequest;
import com.xxx.api.internal.dto.UserRoleQueryResponse;
import com.xxx.api.internal.entity.AppEntity;
import com.xxx.api.internal.entity.AppMemberEntity;
import com.xxx.api.internal.entity.AppPropertyEntity;
import com.xxx.api.internal.mapper.AppEntityMapper;
import com.xxx.api.internal.mapper.AppMemberEntityMapper;
import com.xxx.api.internal.mapper.AppPropertyEntityMapper;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("用户角色查询服务测试")
class UserRoleServiceTest {

    @Mock
    private AppEntityMapper appEntityMapper;

    @Mock
    private AppMemberEntityMapper appMemberEntityMapper;

    @Mock
    private AppPropertyEntityMapper appPropertyEntityMapper;

    @Mock
    private SysTokenResolver sysTokenResolver;

    @Mock
    private InternalAuthProperties authProperties;

    @InjectMocks
    private UserRoleServiceImpl userRoleService;

    private static final String VALID_TOKEN = "dev-token-001";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(authProperties.isBypass()).thenReturn(false);
        when(authProperties.getAllowedAccounts()).thenReturn(List.of("dev-token-001"));
        when(sysTokenResolver.resolveAccount(VALID_TOKEN)).thenReturn("dev-token-001");
        when(sysTokenResolver.isTokenValid(VALID_TOKEN)).thenReturn(true);
    }

    @Nested
    @DisplayName("正常流程")
    class SuccessTests {

        @Test
        @DisplayName("按 appId 查询 - 多角色")
        void testByAppId_MultipleRoles() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app-001");
            request.setUserAccount("admin");

            AppEntity app = new AppEntity();
            app.setId(1001L);
            app.setAppId("test-app-001");
            when(appEntityMapper.selectByAppId("test-app-001")).thenReturn(app);

            AppMemberEntity m1 = new AppMemberEntity();
            m1.setMemberType(1);
            AppMemberEntity m2 = new AppMemberEntity();
            m2.setMemberType(2);
            when(appMemberEntityMapper.selectByAppIdAndAccountId(1001L, "admin"))
                    .thenReturn(Arrays.asList(m1, m2));

            UserRoleQueryResponse response = userRoleService.queryUserRoles(request, VALID_TOKEN);

            assertNotNull(response);
            assertEquals("test-app-001", response.getAppId());
            assertArrayEquals(new Integer[]{1, 2}, response.getRoles());
        }

        @Test
        @DisplayName("按 hisAppId 查询")
        void testByHisAppId() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setHisAppId("eamap_001");
            request.setUserAccount("user_001");

            AppPropertyEntity prop = new AppPropertyEntity();
            prop.setParentId(2001L);
            when(appPropertyEntityMapper.selectByEamapAppCode("eamap_001")).thenReturn(prop);

            AppEntity app = new AppEntity();
            app.setId(2001L);
            app.setAppId("resolved-app-001");
            when(appEntityMapper.selectById(2001L)).thenReturn(app);
            when(appEntityMapper.selectByAppId("resolved-app-001")).thenReturn(app);

            AppMemberEntity member = new AppMemberEntity();
            member.setMemberType(0);
            when(appMemberEntityMapper.selectByAppIdAndAccountId(2001L, "user_001"))
                    .thenReturn(Collections.singletonList(member));

            UserRoleQueryResponse response = userRoleService.queryUserRoles(request, VALID_TOKEN);

            assertEquals("resolved-app-001", response.getAppId());
            assertEquals(1, response.getRoles().length);
            assertEquals(0, response.getRoles()[0]);
        }

        @Test
        @DisplayName("用户无角色 → 空列表")
        void testNoRoles() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app-001");
            request.setUserAccount("unknown");

            AppEntity app = new AppEntity();
            app.setId(1001L);
            app.setAppId("test-app-001");
            when(appEntityMapper.selectByAppId("test-app-001")).thenReturn(app);
            when(appMemberEntityMapper.selectByAppIdAndAccountId(1001L, "unknown"))
                    .thenReturn(Collections.emptyList());

            UserRoleQueryResponse response = userRoleService.queryUserRoles(request, VALID_TOKEN);

            assertEquals("test-app-001", response.getAppId());
            assertEquals(0, response.getRoles().length);
        }

        @Test
        @DisplayName("bypass 模式 → 跳过校验")
        void testBypass() {
            when(authProperties.isBypass()).thenReturn(true);

            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app-001");
            request.setUserAccount("admin");

            AppEntity app = new AppEntity();
            app.setId(1001L);
            app.setAppId("test-app-001");
            when(appEntityMapper.selectByAppId("test-app-001")).thenReturn(app);
            when(appMemberEntityMapper.selectByAppIdAndAccountId(1001L, "admin"))
                    .thenReturn(Collections.emptyList());

            UserRoleQueryResponse response = userRoleService.queryUserRoles(request, null);

            assertEquals("200", "200"); // no exception
            verify(sysTokenResolver, never()).resolveAccount(any());
        }
    }

    @Nested
    @DisplayName("凭证校验")
    class TokenTests {

        @Test
        @DisplayName("无凭证 → 401")
        void testNoToken() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app-001");
            request.setUserAccount("admin");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userRoleService.queryUserRoles(request, null));
            assertEquals("401", ex.getCode());
        }

        @Test
        @DisplayName("不在白名单 → 403")
        void testNotInWhitelist() {
            when(sysTokenResolver.resolveAccount("bad-token")).thenReturn("bad-token");
            when(sysTokenResolver.isTokenValid("bad-token")).thenReturn(true);

            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app-001");
            request.setUserAccount("admin");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userRoleService.queryUserRoles(request, "bad-token"));
            assertEquals("403", ex.getCode());
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ParamTests {

        @Test
        @DisplayName("appId + hisAppId 均为空 → 400")
        void testMissingAppIdentifier() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setUserAccount("admin");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userRoleService.queryUserRoles(request, VALID_TOKEN));
            assertEquals("400", ex.getCode());
        }

        @Test
        @DisplayName("userAccount 为空 → 400")
        void testMissingUserAccount() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("test-app-001");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userRoleService.queryUserRoles(request, VALID_TOKEN));
            assertEquals("400", ex.getCode());
        }
    }

    @Nested
    @DisplayName("应用解析")
    class AppResolveTests {

        @Test
        @DisplayName("应用不存在 → 404")
        void testAppNotFound() {
            UserRoleQueryRequest request = new UserRoleQueryRequest();
            request.setAppId("nonexistent");
            request.setUserAccount("admin");

            when(appEntityMapper.selectByAppId("nonexistent")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userRoleService.queryUserRoles(request, VALID_TOKEN));
            assertEquals("404", ex.getCode());
        }
    }
}
