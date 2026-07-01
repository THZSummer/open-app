package com.xxx.it.works.wecode.v2.common.util;

import com.xxx.it.works.wecode.v2.common.constants.CommonConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 通用工具类
 */
@Slf4j
public final class CommonUtils {

    private CommonUtils() {
    }

    /**
     * 安全解析字符串为 Long，解析失败返回 null。
     *
     * @param str 数字字符串，允许为空
     * @return Long 值，空串或解析失败返回 null
     */
    public static Long parseLongSafe(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            log.debug("[CommonUtils] Failed to parse long: str={}", str);
            return null;
        }
    }

    /**
     * 从方法参数中查找指定类型的参数
     */
    @SuppressWarnings("unchecked")
    public static <T> T findArg(ProceedingJoinPoint joinPoint, Class<T> clazz) {
        for (Object arg : joinPoint.getArgs()) {
            if (clazz.isInstance(arg)) {
                return (T) arg;
            }
        }
        return null;
    }

    /**
     * 从 HttpServletRequest 中提取客户端 IP 地址。
     * 优先级：X-Forwarded-For → X-Real-IP → request.getRemoteAddr()
     */
    public static String extractIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || CommonConstants.UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || CommonConstants.UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            log.warn("[COMMON] Failed to extract IP address", e);
            return null;
        }
    }
}
