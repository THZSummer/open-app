package com.xxx.it.works.wecode.v2.modules.security;

import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 应用数据隔离切面（v2.1.0）
 *
 * <p>拦截连接器和连接流模块的服务层方法，从 HTTP 请求 Header 中获取 X-App-Id（外部 ID），
 * 调用 AppContextResolver 解析校验后将完整的 AppContext 注入 AppContextHolder。</p>
 *
 * <p>执行流程：
 * <ol>
 *   <li>从 RequestContextHolder 获取当前 HttpServletRequest</li>
 *   <li>读取 X-App-Id Header（String 外部 ID）</li>
 *   <li>调用 appContextResolver.resolveAndValidate(externalAppId)
 *       — 查询 app_t 确认存在 + 校验成员权限 + 外部→内部 ID 转换</li>
 *   <li>将 AppContext（含 internalId）注入 AppContextHolder</li>
 *   <li>方法执行完毕后清除上下文</li>
 * </ol>
 *
 * <p>拦截范围：
 * <ul>
 *   <li>com.xxx.it.works.wecode.v2.modules.connector.ConnectorService.*</li>
 *   <li>com.xxx.it.works.wecode.v2.modules.connectorversion.service.ConnectorVersionService.*</li>
 *   <li>com.xxx.it.works.wecode.v2.modules.flow.service.*Service.*</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 2.1.0
 * @see AppContextHolder
 * @see AppWhitelistInterceptor
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AppDataIsolationAspect {

    private static final String HEADER_APP_ID = "X-App-Id";

    private final AppContextResolver appContextResolver;

    @Around("execution(* com.xxx.it.works.wecode.v2.modules.connector.service.ConnectorService.*(..)) || "
            + "execution(* com.xxx.it.works.wecode.v2.modules.connectorversion.service.ConnectorVersionService.*(..))")
    public Object validateConnectorAppIsolation(ProceedingJoinPoint joinPoint) throws Throwable {
        return validateAndProceed(joinPoint);
    }

    @Around("execution(* com.xxx.it.works.wecode.v2.modules.flow.service.*Service.*(..))")
    public Object validateFlowAppIsolation(ProceedingJoinPoint joinPoint) throws Throwable {
        return validateAndProceed(joinPoint);
    }

    private Object validateAndProceed(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            log.debug("No HttpServletRequest available, skipping isolation check for: {}",
                    joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        }

        String appIdHeader = request.getHeader(HEADER_APP_ID);
        if (appIdHeader == null || appIdHeader.trim().isEmpty()) {
            log.debug("No X-App-Id header, skipping isolation check for: {}",
                    joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        }

        String externalAppId = appIdHeader.trim();

        AppContext ctx = appContextResolver.resolveAndValidate(externalAppId);
        AppContextHolder.setCurrentContext(ctx);

        log.debug("AppContext resolved: externalId={}, internalId={}, method={}",
                externalAppId, ctx.getInternalId(), joinPoint.getSignature().toShortString());

        try {
            return joinPoint.proceed();
        } finally {
            AppContextHolder.clear();
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            log.debug("Failed to get HttpServletRequest: {}", e.getMessage());
            return null;
        }
    }
}
