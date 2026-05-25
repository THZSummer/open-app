package com.xxx.it.works.wecode.v2.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.annotation.AuditLog;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.AppIdSourceEnum;
import com.xxx.it.works.wecode.v2.common.enums.OperateObjectEnum;
import com.xxx.it.works.wecode.v2.common.enums.OperateTypeEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper;
import com.xxx.it.works.wecode.v2.modules.auditlog.entity.OperateLog;
import com.xxx.it.works.wecode.v2.modules.auditlog.service.AuditLogService;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.EventMapper;
import com.xxx.it.works.wecode.v2.modules.permission.mapper.SubscriptionMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

/**
 * 操作日志持久化切面
 *
 * <p>基于 @AuditLog 注解的 @Around 切面，自动捕获操作前后实体快照、
 * 用户信息和 IP 地址，异步写入 openplateform_operate_log_t 表</p>
 *
 * <p>与 AuditLogAspect（连接流 SLF4J 日志）互不影响</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(2)
public class OperateLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final ApiMapper apiMapper;
    private final EventMapper eventMapper;
    private final CallbackMapper callbackMapper;
    private final SubscriptionMapper subscriptionMapper;

    /**
     * 拦截所有标注 @AuditLog 注解的方法
     *
     * @param joinPoint 连接点
     * @param auditLog  注解实例
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {

        OperateTypeEnum operateType = auditLog.operateType();
        OperateObjectEnum operateObject = auditLog.operateObject();

        // 1. 提取资源 ID（从方法参数中按 resourceIdParam 名称匹配）
        Long resourceId = extractResourceId(joinPoint, auditLog.resourceIdParam());

        // 2. 加载 before_data（UPDATE/DELETE/WITHDRAW/CONFIG 需要操作前实体快照）
        String beforeData = null;
        if (needsBeforeData(operateType) && resourceId != null) {
            beforeData = loadEntitySnapshot(operateObject, resourceId);
        }

        // 3. 执行目标方法
        Object result;
        int status = 1; // 1=成功
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            // 主操作失败，记录 status=0 的审计日志
            status = 0;
            saveOperateLog(auditLog, joinPoint, beforeData, null, status);
            throw ex; // 重新抛出，由全局异常处理器处理
        }

        // 4. 加载 after_data
        String afterData = null;
        if (operateType == OperateTypeEnum.CREATE) {
            // CREATE: 从返回值 ApiResponse.data 中提取创建后的实体
            afterData = extractEntityFromResult(result);
        } else if (operateType != OperateTypeEnum.DELETE && resourceId != null) {
            // UPDATE/WITHDRAW/CONFIG: 操作后重新查询实体
            afterData = loadEntitySnapshot(operateObject, resourceId);
        }
        // DELETE: afterData 保持 null（实体已删除）

        // 5. 保存审计日志
        saveOperateLog(auditLog, joinPoint, beforeData, afterData, status);

        return result;
    }

    // ===== 私有辅助方法 =====

    /**
     * 构造并保存操作日志
     */
    private void saveOperateLog(AuditLog auditLog, ProceedingJoinPoint joinPoint,
                                String beforeData, String afterData, int status) {
        try {
            OperateLog logEntry = new OperateLog();

            // 基础字段
            logEntry.setAppId(resolveAppId(auditLog, joinPoint));
            logEntry.setOperateType(auditLog.operateType().getCode());
            logEntry.setOperateObject(auditLog.operateObject().getCode());
            logEntry.setOperateDescCn(auditLog.descCn());
            logEntry.setOperateDescEn(auditLog.descEn());

            // 用户信息
            logEntry.setOperateUser(UserContextHolder.getUserName());
            logEntry.setIpAddress(extractIpAddress());

            // 前后数据
            logEntry.setBeforeData(beforeData);
            logEntry.setAfterData(afterData);
            logEntry.setStatus(status);

            // 时间戳和操作人
            Date now = new Date();
            String user = logEntry.getOperateUser();
            logEntry.setCreateBy(user);
            logEntry.setLastUpdateBy(user);
            logEntry.setCreateTime(now);
            logEntry.setLastUpdateTime(now);

            // 异步保存
            auditLogService.saveAsync(logEntry);

        } catch (Exception e) {
            log.error("[OPERATE_LOG] Failed to construct log entry", e);
        }
    }

    /**
     * 从方法参数中提取资源 ID
     */
    private Long extractResourceId(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = sig.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames == null) {
            return null;
        }

        for (int i = 0; i < paramNames.length; i++) {
            if (paramName.equals(paramNames[i])) {
                Object arg = args[i];
                if (arg instanceof String s) {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                } else if (arg instanceof Long l) {
                    return l;
                }
            }
        }
        return null;
    }

    /**
     * 解析 app_id
     */
    private String resolveAppId(AuditLog auditLog, ProceedingJoinPoint joinPoint) {
        if (auditLog.appIdSource() == AppIdSourceEnum.PLATFORM) {
            return "platform";
        }

        // PATH_VARIABLE: 从方法参数中提取 appId
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = sig.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if ("appId".equals(paramNames[i]) && args[i] != null) {
                    return args[i].toString();
                }
            }
        }
        return "unknown";
    }

    /**
     * 加载实体快照（JSON 序列化）
     */
    private String loadEntitySnapshot(OperateObjectEnum obj, Long id) {
        try {
            Object entity = switch (obj) {
                case API -> apiMapper.selectById(id);
                case EVENT -> eventMapper.selectById(id);
                case CALLBACK -> callbackMapper.selectById(id);
                case API_PERMISSION, EVENT_PERMISSION, CALLBACK_PERMISSION
                        -> subscriptionMapper.selectById(id);
            };
            return entity != null ? objectMapper.writeValueAsString(entity) : null;
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Entity snapshot load failed: object={}, id={}", obj.getCode(), id, e);
            return null;
        }
    }

    /**
     * 从返回值中提取实体 JSON
     */
    private String extractEntityFromResult(Object result) {
        try {
            if (result instanceof ApiResponse<?> resp && resp.getData() != null) {
                return objectMapper.writeValueAsString(resp.getData());
            }
            return null;
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to extract entity from result", e);
            return null;
        }
    }

    /**
     * 判断是否需要 before_data
     */
    private boolean needsBeforeData(OperateTypeEnum type) {
        return type == OperateTypeEnum.UPDATE
                || type == OperateTypeEnum.DELETE
                || type == OperateTypeEnum.WITHDRAW
                || type == OperateTypeEnum.CONFIG;
    }

    /**
     * 从 HttpServletRequest 中提取客户端 IP 地址
     *
     * <p>优先级：X-Forwarded-For → X-Real-IP → request.getRemoteAddr()</p>
     */
    private String extractIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }

            HttpServletRequest request = attrs.getRequest();

            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }

            // X-Forwarded-For 可能包含多个 IP，取第一个（客户端真实 IP）
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }

            return ip;
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to extract IP address", e);
            return null;
        }
    }
}
