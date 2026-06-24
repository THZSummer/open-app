package com.xxx.it.works.wecode.v2.modules.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

/**
 * 应用数据隔离切面
 *
 * <p>在服务层方法执行前校验请求方（appId）与操作目标资源的归属关系，
 * 确保应用 A 不可查询/操作应用 B 的连接器和连接流数据。</p>
 *
 * <p>拦截范围：
 * <ul>
 *   <li>com.xxx.it.works.wecode.v2.modules.connector.ConnectorService.*</li>
 *   <li>com.xxx.it.works.wecode.v2.modules.connector.ConnectorVersionService.*</li>
 *   <li>com.xxx.it.works.wecode.v2.modules.flow.service.*Service.*</li>
 * </ul>
 *
 * <p>工作原理：
 * <ol>
 *   <li>从 AppContextHolder 获取当前请求的应用 ID</li>
 *   <li>查找方法参数中名为 appId 的 Long 类型参数</li>
 *   <li>如果 appId 参数存在且与上下文不一致，拒绝执行</li>
 *   <li>如果方法无 appId 参数，放行（由服务层内部自行校验）</li>
 * </ol>
 *
 * <p>注：此切面作为纵深防御层与各 Service 内部的显式 appId 校验互补。
 * Service 方法中的参数 appId 来源应为 Controller 层从 X-App-Id Header 透传的值。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see AppContextHolder
 * @see AppWhitelistInterceptor
 */
@Slf4j
@Aspect
@Component
public class AppDataIsolationAspect {

    /**
     * 拦截连接器模块服务层方法
     */
    @Around("execution(* com.xxx.it.works.wecode.v2.modules.connector.ConnectorService.*(..)) || "
            + "execution(* com.xxx.it.works.wecode.v2.modules.connector.ConnectorVersionService.*(..))")
    public Object validateConnectorAppIsolation(ProceedingJoinPoint joinPoint) throws Throwable {
        return validateAndProceed(joinPoint);
    }

    /**
     * 拦截连接流模块服务层方法
     */
    @Around("execution(* com.xxx.it.works.wecode.v2.modules.flow.service.*Service.*(..))")
    public Object validateFlowAppIsolation(ProceedingJoinPoint joinPoint) throws Throwable {
        return validateAndProceed(joinPoint);
    }

    /**
     * 校验应用归属并继续执行
     *
     * @param joinPoint 切点
     * @return 原方法返回值
     * @throws Throwable 如果校验失败或原方法抛出异常
     */
    private Object validateAndProceed(ProceedingJoinPoint joinPoint) throws Throwable {
        Long contextAppId = AppContextHolder.getCurrentAppId();

        // 上下文未设置时放行（可能在内部调用或测试场景，由 Service 内部校验）
        if (contextAppId == null) {
            log.debug("AppContextHolder not set, skipping isolation check for method: {}",
                    joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        }

        // 查找方法参数中的 appId
        Long paramAppId = findAppIdParameter(joinPoint);

        // 如果方法无 appId 参数，放行（由 Service 内部校验）
        if (paramAppId == null) {
            log.debug("No appId parameter found in method: {}, delegating to service",
                    joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        }

        // 校验应用归属
        if (!contextAppId.equals(paramAppId)) {
            log.error("App isolation violation detected: context appId={}, parameter appId={}, method={}",
                    contextAppId, paramAppId, joinPoint.getSignature().toShortString());
            throw new SecurityException(
                    String.format("App isolation violation: context appId %d does not match requested appId %d",
                            contextAppId, paramAppId));
        }

        return joinPoint.proceed();
    }

    /**
     * 从方法参数中查找名为 appId 的 Long 类型参数
     *
     * @param joinPoint 切点
     * @return appId 参数值，如果未找到则返回 null
     */
    private Long findAppIdParameter(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if ("appId".equals(parameters[i].getName()) && Long.class.equals(parameters[i].getType())) {
                return (Long) args[i];
            }
        }
        return null;
    }
}
