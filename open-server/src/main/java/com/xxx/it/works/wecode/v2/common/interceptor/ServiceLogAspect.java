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

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service 入参日志切面（含敏感参数脱敏）
 *
 * <p>拦截 modules 下所有 @Service 类的 public 方法，
 * 在方法入口打印方法名和入参。简单类型直接打印，复杂对象转 JSON。</p>
 *
 * <p>敏感参数脱敏（双层拦截）：
 * <ul>
 *   <li>第一层：参数名含敏感关键字（password/token/secret 等）→ 显示 ***</li>
 *   <li>第二层：复杂对象 JSON 中字段名含敏感关键字 → 值替换为 ***</li>
 * </ul>
 * </p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Aspect
@Component
public class ServiceLogAspect {

    /** 敏感关键字集合（构建正则的数据源） */
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "passwd", "secret", "token", "apisecret",
            "privatekey", "sk", "credential", "authorization"
    );

    /** 敏感关键字正则（一次构建，两个 Pattern 共用） */
    private static final String SENSITIVE_REGEX = String.join("|", SENSITIVE_KEYWORDS);

    /** 第一层：参数名子串匹配，如 accessToken 含 token → 命中 */
    private static final Pattern SENSITIVE_NAME_PATTERN = Pattern.compile(
            SENSITIVE_REGEX, Pattern.CASE_INSENSITIVE);

    /** 第二层：JSON 字段名子串匹配 + 值替换，支持字符串/数字/布尔/null 值 */
    private static final Pattern SENSITIVE_VALUE_PATTERN = Pattern.compile(
            "(\"\\w*(?:" + SENSITIVE_REGEX + ")\\w*\"\\s*:\\s*)(?:\"[^\"]*\"|\\d+|true|false|null)",
            Pattern.CASE_INSENSITIVE);

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
            if (isSensitiveName(name)) {
                sb.append(name).append("=").append("***");
            } else {
                sb.append(name).append("=").append(formatArg(args[i]));
            }
        }

        log.info("[{}] {}({})", className, methodName, sb);
    }

    /**
     * 格式化参数值：简单类型直接返回，复杂对象转 JSON 后脱敏
     */
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
        return json != null ? maskSensitiveFields(json) : arg.toString();
    }

    /**
     * 判断参数名是否含敏感关键字（子串匹配，不区分大小写）
     */
    private static boolean isSensitiveName(String name) {
        return SENSITIVE_NAME_PATTERN.matcher(name).find();
    }

    /**
     * 对 JSON 中敏感字段的值替换为 ***（字段名子串匹配，不区分大小写）
     * 支持字符串、数字、布尔、null 值
     * 例如：{"apiSecret":"abc","accessToken":"xyz","secret":123} → {"apiSecret":"***","accessToken":"***","secret":"***"}
     */
    private static String maskSensitiveFields(String json) {
        return SENSITIVE_VALUE_PATTERN.matcher(json).replaceAll("$1\"***\"");
    }
}
