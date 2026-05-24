package com.xxx.it.works.wecode.v2.common.interceptor;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 审计日志切面
 * <p>
 * NFR-013: 连接流启停等关键操作记录审计日志
 * 记录: createBy + action + time + flowId
 * </p>
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===== 切点定义 =====

    /** 启动连接流操作 */
    @Pointcut("execution(* com.xxx.it.works.wecode.v2.modules.flow.service.FlowService.startFlow(..))")
    public void startFlowOperation() {}

    /** 停止连接流操作 */
    @Pointcut("execution(* com.xxx.it.works.wecode.v2.modules.flow.service.FlowService.stopFlow(..))")
    public void stopFlowOperation() {}

    /** 删除连接流操作 */
    @Pointcut("execution(* com.xxx.it.works.wecode.v2.modules.flow.service.FlowService.deleteFlow(..))")
    public void deleteFlowOperation() {}

    // ===== 通知 =====

    /**
     * 记录启停操作审计日志
     */
    @AfterReturning("startFlowOperation() || stopFlowOperation() || deleteFlowOperation()")
    public void logFlowOperation(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) return;

        Long flowId = null;
        if (args[0] instanceof Long) {
            flowId = (Long) args[0];
        }

        String methodName = joinPoint.getSignature().getName();
        String action;
        switch (methodName) {
            case "startFlow":
                action = "start_flow";
                break;
            case "stopFlow":
                action = "stop_flow";
                break;
            case "deleteFlow":
                action = "delete_flow";
                break;
            default:
                action = methodName;
        }

        String currentUser = UserContextHolder.getUserName();
        String timestamp = LocalDateTime.now().format(FORMATTER);

        log.info("[AUDIT] action={} | flowId={} | createBy={} | time={}",
                action, flowId, currentUser, timestamp);
    }
}