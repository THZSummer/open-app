package com.xxx.it.works.wecode.v2.common.interceptor;

import com.xxx.it.works.wecode.v2.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service 入参日志切面
 *
 * <p>自动拦截 5 个模块（common/app/version/member/ability）下所有 @Service 类的 public 方法，
 * 在方法入口打印方法名和入参。简单类型直接打印，实体类转 JSON。</p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Aspect
@Component
public class ServiceLogAspect {

    @Pointcut("@within(org.springframework.stereotype.Service) && within(com.xxx.it.works.wecode.v2.modules..*)")
    public void serviceMethod() {
    }

    @Before("serviceMethod()")
    public void logEntry(JoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String className = sig.getDeclaringType().getSimpleName();
        String methodName = sig.getName();
        String[] paramNames = sig.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            String name = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
            sb.append(name).append("=").append(formatArg(args[i]));
        }

        log.info("[{}] {}({})", className, methodName, sb);
    }

    private String formatArg(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
            return String.valueOf(arg);
        }
        if (arg instanceof MultipartFile) {
            return arg.getClass().getSimpleName();
        }
        String json = JsonUtils.toJson(arg);
        return json != null ? json : arg.toString();
    }
}
