package com.xxx.it.works.wecode.v2.modules.security;

import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AppDataIsolationAspect 测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppDataIsolationAspectTest {

    @Mock
    private AppContextResolver appContextResolver;

    private AppDataIsolationAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new AppDataIsolationAspect(appContextResolver);
        AppContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("无 HttpServletRequest → 放行")
    void testNoRequest_PassThrough() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(mock(Signature.class));
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.validateConnectorAppIsolation(joinPoint);

        assertEquals("result", result);
        verify(joinPoint).proceed();
        assertNull(AppContextHolder.getCurrentContext());
    }

    @Test
    @DisplayName("有请求但无 X-App-Id Header → 放行")
    void testNoHeader_PassThrough() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("ConnectorService.toString");
        when(joinPoint.proceed()).thenReturn("proceeded");

        Object result = aspect.validateConnectorAppIsolation(joinPoint);

        assertEquals("proceeded", result);
        verify(appContextResolver, never()).resolveAndValidate(any());
    }

    @Test
    @DisplayName("有 X-App-Id Header → resolveAndValidate → 注入上下文 → 执行后清除")
    void testWithHeader_ResolvesAndInjects() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-App-Id", "app_test_001");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AppContext ctx = AppContext.builder().internalId(789L).externalId("app_test_001").build();
        when(appContextResolver.resolveAndValidate("app_test_001")).thenReturn(ctx);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("ConnectorService.create");
        when(joinPoint.proceed()).thenReturn("created");

        Object result = aspect.validateConnectorAppIsolation(joinPoint);

        assertEquals("created", result);
        verify(appContextResolver).resolveAndValidate("app_test_001");

        // 上下文在切面结束后应被清除
        assertNull(AppContextHolder.getCurrentContext());
    }

    @Test
    @DisplayName("resolveAndValidate 抛异常 → 异常向上传播，上下文被清除")
    void testResolveFails_ClearsContext() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-App-Id", "app_invalid");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(appContextResolver.resolveAndValidate("app_invalid"))
                .thenThrow(new RuntimeException("App not found"));

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("ConnectorService.create");

        assertThrows(RuntimeException.class, () -> aspect.validateConnectorAppIsolation(joinPoint));
        verify(joinPoint, never()).proceed();
        assertNull(AppContextHolder.getCurrentContext());
    }

    @Test
    @DisplayName("连接流模块拦截 — 同样解析校验")
    void testFlowModule_AlsoResolves() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-App-Id", "app_flow_001");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AppContext ctx = AppContext.builder().internalId(456L).externalId("app_flow_001").build();
        when(appContextResolver.resolveAndValidate("app_flow_001")).thenReturn(ctx);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("FlowService.update");
        when(joinPoint.proceed()).thenReturn("updated");

        Object result = aspect.validateFlowAppIsolation(joinPoint);

        assertEquals("updated", result);
        verify(appContextResolver).resolveAndValidate("app_flow_001");
    }
}
