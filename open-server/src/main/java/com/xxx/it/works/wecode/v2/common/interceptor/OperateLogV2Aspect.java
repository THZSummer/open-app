package com.xxx.it.works.wecode.v2.common.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.annotation.AuditLog;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.AppIdSourceEnum;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoaderFactory;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import com.xxx.it.works.wecode.v2.modules.auditlog.entity.OperateLog;
import com.xxx.it.works.wecode.v2.modules.auditlog.service.AuditLogService;
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
 * 操作日志持久化切面 V2
 *
 * <p>基于 @AuditLog 注解的 @Around 切面，自动捕获操作前后实体快照、
 * 用户信息和 IP 地址，异步写入 openplateform_operate_log_t 表</p>
 *
 * <p>与 AuditLogAspect（连接流 SLF4J 日志）互不影响</p>
 *
 * <p>实体快照加载采用策略模式（EntitySnapshotLoader + Factory），
 * 根据 OperateEnum.operateObject 路由到对应的 Loader 实现，支持不同资源类型从不同表加载</p>
 *
 * @author SDDU Build Agent
 * @version 2.1.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(2)
public class OperateLogV2Aspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final EntitySnapshotLoaderFactory snapshotLoaderFactory;
    private final AppContextResolver appContextResolver;

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

        OperateEnum op = auditLog.value();
        AppIdSourceEnum appIdSource = auditLog.appIdSource();

        // 1. 提取资源 ID（从方法参数中按 resourceIdParam 名称匹配）
        Long resourceId = extractResourceId(joinPoint, auditLog.resourceIdParam());

        // 2. 加载 before_data（WITHDRAW/DELETE/CONFIG 需要操作前实体快照；ENTITY 策略同时用于提取 appId）
        String beforeData = null;
        if ((op.needsBeforeData() || appIdSource == AppIdSourceEnum.ENTITY) && resourceId != null) {
            beforeData = loadEntitySnapshot(resourceId, op.getOperateObject());
        }

        // 3. 解析 app_id（openplatform_app_t.app_id，varchar 外部业务 ID）
        String appId;
        if (appIdSource == AppIdSourceEnum.PATH_VARIABLE) {
            appId = extractAppIdFromParams(joinPoint);
        } else {
            // ENTITY 策略：从 before_data 实体快照中提取 numeric app_id，再转换为 varchar app_id
            appId = extractAppIdFromEntity(beforeData);
        }

        // 4. 执行目标方法
        Object result;
        int status = 1; // 1=成功
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            // 主操作失败，记录 status=0 的审计日志
            status = 0;
            saveOperateLog(op, appId, joinPoint, beforeData, null, status);
            throw ex; // 重新抛出，由全局异常处理器处理
        }

        // 5. 加载 after_data
        String afterData = null;
        if (op.needsAfterData()) {
            if (resourceId != null) {
                // WITHDRAW/CONFIG: 从数据库重新查询操作后实体
                afterData = loadEntitySnapshot(resourceId, op.getOperateObject());
            } else {
                // SUBSCRIBE: 从返回值提取创建的订阅记录（resourceId 为 null）
                afterData = extractEntityFromResult(result);
            }
        }

        // 6. 保存审计日志
        saveOperateLog(op, appId, joinPoint, beforeData, afterData, status);

        return result;
    }

    // ===== 私有辅助方法 =====

    /**
     * 构造并保存操作日志
     */
    private void saveOperateLog(OperateEnum op, String appId, ProceedingJoinPoint joinPoint,
                                String beforeData, String afterData, int status) {
        try {
            OperateLog logEntry = new OperateLog();

            // 基础字段（从 OperateEnum 统一获取）
            logEntry.setAppId(appId);
            logEntry.setOperateType(op.getOperateType());
            logEntry.setOperateObject(op.getOperateObject());
            logEntry.setOperateDescCn(op.getDescCn());
            logEntry.setOperateDescEn(op.getDescEn());

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
     * PATH_VARIABLE 策略：从方法参数中提取 appId
     *
     * <p>路径 {appId} 已是 openplatform_app_t.app_id（varchar 外部业务 ID），直接使用</p>
     */
    private String extractAppIdFromParams(ProceedingJoinPoint joinPoint) {
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
     * ENTITY 策略：从实体快照中提取 numeric app_id，再转换为 varchar app_id
     *
     * <p>数据链路：
     * 实体 JSON 中的 appId (Long) = openplatform_app_t.id (内部主键)
     * → AppContextResolver.toExternalId(internalId)
     * → openplatform_app_t.app_id (varchar 外部业务 ID) = 审计日志 app_id</p>
     */
    private String extractAppIdFromEntity(String entityJson) {
        if (entityJson == null) {
            return "unknown";
        }
        try {
            JsonNode node = objectMapper.readTree(entityJson);
            JsonNode appIdNode = node.get("appId");
            if (appIdNode == null) {
                appIdNode = node.get("app_id");
            }
            if (appIdNode == null || appIdNode.isNull()) {
                return "unknown";
            }
            // 实体中存储的是 numeric app_id (openplatform_app_t.id)
            Long internalId = appIdNode.asLong();
            // 通过 AppContextResolver 转换为 varchar app_id (openplatform_app_t.app_id)
            return appContextResolver.toExternalId(internalId);
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to extract appId from entity", e);
            return "unknown";
        }
    }

    /**
     * 通过策略工厂加载实体快照（JSON 序列化）
     *
     * <p>根据 operateObject 路由到对应的 EntitySnapshotLoader 实现</p>
     *
     * @param id             资源 ID
     * @param operateObject  操作对象标识（如 API_PERMISSION / EVENT_PERMISSION）
     * @return 实体 JSON 字符串，未找到或加载失败返回 null
     */
    private String loadEntitySnapshot(Long id, String operateObject) {
        try {
            EntitySnapshotLoader loader = snapshotLoaderFactory.getLoader(operateObject);
            if (loader == null) {
                log.warn("[OPERATE_LOG] No snapshot loader for: {}", operateObject);
                return null;
            }
            Object entity = loader.loadById(id);
            return entity != null ? objectMapper.writeValueAsString(entity) : null;
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Entity snapshot load failed: id={}, object={}", id, operateObject, e);
            return null;
        }
    }

    /**
     * 从 ApiResponse 返回值中提取实体数据作为 afterData
     *
     * <p>适用于 SUBSCRIBE 等批量操作，resourceId 为 null 的场景。
     * 返回的 PermissionSubscribeResponse 包含创建的订阅记录列表。</p>
     */
    private String extractEntityFromResult(Object result) {
        if (result == null) {
            return null;
        }
        try {
            if (result instanceof ApiResponse<?> apiResponse) {
                Object data = apiResponse.getData();
                return data != null ? objectMapper.writeValueAsString(data) : null;
            }
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to extract entity from result", e);
            return null;
        }
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
