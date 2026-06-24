package com.xxx.it.works.wecode.v2.modules.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AppDataIsolationAspect 测试")
class AppDataIsolationAspectTest {

    private AppDataIsolationAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new AppDataIsolationAspect();
        AppContextHolder.clear();
    }

    @Test
    @DisplayName("上下文 appId 未设置 → 放行")
    void testContextAppIdNotSet_PassThrough() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.validateConnectorAppIsolation(joinPoint);

        assertEquals("result", result);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("上下文 appId 已设置 → 无 appId 参数时放行")
    void testConnectorAppIsolation_NoAppIdParam_Passes() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("ConnectorService.toString");
        when(signature.getMethod()).thenReturn(Object.class.getMethod("toString"));
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn("created");

        AppContextHolder.setCurrentAppId(1L);

        Object result = aspect.validateConnectorAppIsolation(joinPoint);

        assertEquals("created", result);
        AppContextHolder.clear();
    }

    @Test
    @DisplayName("连接流模块拦截 → 无 appId 参数时放行")
    void testFlowAppIsolation_NoAppIdParam_Passes() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("FlowService.toString");
        when(signature.getMethod()).thenReturn(Object.class.getMethod("toString"));
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn("updated");

        AppContextHolder.setCurrentAppId(1L);

        Object result = aspect.validateFlowAppIsolation(joinPoint);

        assertEquals("updated", result);
        AppContextHolder.clear();
    }

    @Test
    @DisplayName("跨应用拒绝 — appId 不匹配 → 抛出 SecurityException")
    void testCrossAppAccess_Denied() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("ConnectorService.getById");

        // Use a real method that has Long appId parameter
        Method method = TestService.class.getMethod("getById", Long.class, Long.class);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, 999L}); // id, appId
        when(joinPoint.proceed()).thenReturn("some-result");

        AppContextHolder.setCurrentAppId(1L); // context appId=1, param appId=999

        assertThrows(SecurityException.class, () -> aspect.validateConnectorAppIsolation(joinPoint));
        AppContextHolder.clear();
    }

    @Test
    @DisplayName("相同 appId → 通过校验")
    void testSameAppId_Passes() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("ConnectorService.getById");

        Method method = TestService.class.getMethod("getById", Long.class, Long.class);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, 1L}); // id, appId match
        when(joinPoint.proceed()).thenReturn("matched");

        AppContextHolder.setCurrentAppId(1L);

        Object result = aspect.validateConnectorAppIsolation(joinPoint);

        assertEquals("matched", result);
        AppContextHolder.clear();
    }

    /**
     * 用于反射测试的内部类
     */
    static class TestService {
        @SuppressWarnings("unused")
        public String getById(Long id, Long appId) {
            return "test";
        }
    }
}
