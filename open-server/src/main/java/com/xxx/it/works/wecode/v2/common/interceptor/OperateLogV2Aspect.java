package com.xxx.it.works.wecode.v2.common.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.xxx.it.works.wecode.v2.common.annotation.AuditLog;
import com.xxx.it.works.wecode.v2.common.constants.CommonConstants;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.enums.OperateResultEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoaderFactory;
import com.xxx.it.works.wecode.v2.common.util.CommonUtils;
import com.xxx.it.works.wecode.v2.common.util.JsonUtils;
import com.xxx.it.works.wecode.v2.modules.app.entity.App;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppMapper;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import com.xxx.it.works.wecode.v2.modules.auditlog.entity.OperateLog;
import com.xxx.it.works.wecode.v2.modules.auditlog.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作日志审计切面
 *
 * <p>四阶段流程，任何阶段异常都不得影响主业务：
 * <ol>
 *   <li>proceed 前：提取参数、appId、beforeData 快照</li>
 *   <li>proceed：执行主业务（异常时写 status=0 日志后 rethrow）</li>
 *   <li>proceed 后：从返回值补全 appId/resourceId、加载 afterData</li>
 *   <li>渲染描述 + 异步保存</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(2)
public class OperateLogV2Aspect {

    private final AuditLogService auditLogService;
    private final EntitySnapshotLoaderFactory snapshotLoaderFactory;
    private final AppContextResolver appContextResolver;
    private final AppMapper appMapper;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    private static class AuditContext {
        OperateEnum op;
        Long resourceId;
        String appId;
        JsonNode before;
        JsonNode after;
        JsonNode request;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        AuditContext ctx = prepareAuditContext(joinPoint, auditLog);
        if (ctx == null) {
            return joinPoint.proceed();
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            writeAuditLog(ctx, OperateResultEnum.FAILED.getCode());
            throw ex;
        }

        loadAfterData(ctx, joinPoint, result);
        writeAuditLog(ctx, OperateResultEnum.SUCCESS.getCode());
        return result;
    }

    /**
     * Phase 1: 提取 resourceId/appId/requestJson/beforeData。失败返回 null（跳过审计）。
     */
    private AuditContext prepareAuditContext(ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        AuditContext ctx = new AuditContext();
        ctx.op = auditLog.value();
        try {
            ctx.resourceId = extractResourceId(joinPoint, auditLog.resourceIdParam());
            ctx.request = buildRequestNode(joinPoint);
            ctx.appId = extractAppIdFromParams(joinPoint);
            if (ctx.resourceId == null && StringUtils.hasText(ctx.appId)) {
                ctx.resourceId = resolveInternalIdFromAppId(ctx.appId);
            }
            loadBeforeData(ctx, joinPoint);
            return ctx;
        } catch (Exception e) {
            log.error("[OPERATE_LOG] Phase 1 failed, skipping audit", e);
            return null;
        }
    }

    /**
     * Phase 3: 从返回值补全 appId/resourceId，加载 afterData。异常不影响主业务。
     */
    private void loadAfterData(AuditContext ctx, ProceedingJoinPoint joinPoint, Object result) {
        try {
            JsonNode dataNode = extractResultData(result);

            // appId 策略 3: 从返回值提取
            if (!StringUtils.hasText(ctx.appId)) {
                ctx.appId = JsonUtils.getFieldText(dataNode, CommonConstants.FIELD_APP_ID);
                if (ctx.resourceId == null && StringUtils.hasText(ctx.appId)) {
                    ctx.resourceId = resolveInternalIdFromAppId(ctx.appId);
                }
            }
            // entityId 覆盖 resourceId
            Long fromResult = JsonUtils.getFieldLong(dataNode, ctx.op.entityIdField());
            if (fromResult != null) {
                ctx.resourceId = fromResult;
            }

            if (!ctx.op.needsAfterData()) {
                return;
            }
            EntitySnapshotLoader loader = snapshotLoaderFactory.getLoader(ctx.op.getOperateObject());
            if (loader != null) {
                ctx.after = JsonUtils.toTree(loader.loadAfterData(joinPoint, ctx.resourceId, result));
            } else {
                ctx.after = dataNode;
            }
            // appId fallback: extract from X-App-Id request header
            if (!StringUtils.hasText(ctx.appId)) {
                ctx.appId = extractAppIdFromRequestHeader();
            }
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Phase 3 failed", e);
        }
    }

    /**
     * Phase 4: 渲染描述 + 异步保存。
     */
    private void writeAuditLog(AuditContext ctx, int status) {
        if (!StringUtils.hasText(ctx.appId)) {
            return;
        }
        try {
            String userId = UserContextHolder.getUserId();
            Date now = new Date();
            OperateLog logEntry = new OperateLog();
            logEntry.setAppId(ctx.appId);
            logEntry.setOperateType(ctx.op.getOperateType());
            logEntry.setOperateObject(ctx.op.getOperateObjectCn());
            logEntry.setOperateDescCn(fillTemplate(ctx, true));
            logEntry.setOperateDescEn(fillTemplate(ctx, false));
            logEntry.setOperateUser(userId);
            logEntry.setIpAddress(CommonUtils.extractIpAddress());
            logEntry.setBeforeData(ctx.before != null ? JsonUtils.toJson(ctx.before) : null);
            logEntry.setAfterData(ctx.after != null ? JsonUtils.toJson(ctx.after) : null);
            logEntry.setStatus(status);
            logEntry.setCreateBy(userId);
            logEntry.setLastUpdateBy(userId);
            logEntry.setCreateTime(now);
            logEntry.setLastUpdateTime(now);

            auditLogService.saveAsync(logEntry);
        } catch (Exception e) {
            log.error("[OPERATE_LOG] Phase 4 failed", e);
        }
    }

    /**
     * 加载 beforeData（UPDATE/DELETE/CONFIG/WITHDRAW），并补全 appId 策略 2。
     */
    private void loadBeforeData(AuditContext ctx, ProceedingJoinPoint joinPoint) {
        if (ctx.op.needsBeforeData()) {
            EntitySnapshotLoader loader = snapshotLoaderFactory.getLoader(ctx.op.getOperateObject());
            if (loader != null) {
                ctx.before = JsonUtils.toTree(loader.loadBeforeData(joinPoint, ctx.resourceId));
            }
        }
        if (!StringUtils.hasText(ctx.appId)) {
            ctx.appId = extractAppIdFromEntity(ctx.before);
        }
        // appId fallback: extract from X-App-Id request header
        if (!StringUtils.hasText(ctx.appId)) {
            ctx.appId = extractAppIdFromRequestHeader();
        }
    }

    /**
     * 从返回值（ApiResponse/ResponseEntity）提取 data 的 JsonNode，只序列化一次。
     */
    private JsonNode extractResultData(Object result) {
        if (result == null) {
            return null;
        }
        Object data = null;
        if (result instanceof org.springframework.http.ResponseEntity<?> re) {
            if (re.getBody() instanceof ApiResponse<?> apiResp) {
                data = apiResp.getData();
            }
        } else if (result instanceof ApiResponse<?> apiResponse) {
            data = apiResponse.getData();
        }
        return data != null ? JsonUtils.toTree(data) : null;
    }

    /**
     * 渲染描述：diff 替换 + 占位符填充，失败回退静态描述。
     */
    private String fillTemplate(AuditContext ctx, boolean cn) {
        String template = cn ? ctx.op.getTemplateCn() : ctx.op.getTemplateEn();
        String fallback = cn ? ctx.op.getDescCn() : ctx.op.getDescEn();
        if (template == null || template.isEmpty()) {
            return fallback;
        }
        try {
            template = renderDiff(template, ctx.op.diffConfig(), ctx, cn);
            return fillPlaceholders(template, ctx);
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Template rendering failed", e);
            return fallback;
        }
    }

    /**
     * 替换 ${diffFields}：比较 before/after 变化字段，拼接为 diff 文本。
     */
    private String renderDiff(String template, DiffConfig config, AuditContext ctx, boolean cn) {
        if (config == null || !template.contains(DiffConfig.DIFF_FIELDS_PLACEHOLDER)) {
            return template;
        }
        StringJoiner diff = new StringJoiner(config.separator());
        for (DiffField field : config.fields()) {
            String beforeVal = JsonUtils.getFieldText(ctx.before, field.name());
            String afterVal = JsonUtils.getFieldText(ctx.after, field.name());
            if (!Objects.equals(beforeVal, afterVal)) {
                diff.add(config.renderField(field, beforeVal, afterVal, cn));
            }
        }
        return template.replace(DiffConfig.DIFF_FIELDS_PLACEHOLDER, diff.toString());
    }

    /**
     * 替换 ${xxx}：按 after → before → request 顺序取值。
     */
    private String fillPlaceholders(String template, AuditContext ctx) {
        Matcher m = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String val = JsonUtils.getFieldText(ctx.after, key);
            if (val == null) {
                val = JsonUtils.getFieldText(ctx.before, key);
            }
            if (val == null) {
                val = JsonUtils.getFieldText(ctx.request, key);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(val != null ? val : ""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 按名查找方法参数值。
     */
    private Object findParam(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = sig.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames == null) {
            return null;
        }
        for (int i = 0; i < paramNames.length; i++) {
            if (paramName.equals(paramNames[i])) {
                return args[i];
            }
        }
        return null;
    }

    /**
     * 从方法参数提取 resourceId。
     */
    private Long extractResourceId(ProceedingJoinPoint joinPoint, String paramName) {
        Object arg = findParam(joinPoint, paramName);
        if (arg instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (arg instanceof Long l) {
            return l;
        }
        return null;
    }

    /**
     * appId 策略 1: 从方法参数提取。
     */
    private String extractAppIdFromParams(ProceedingJoinPoint joinPoint) {
        Object arg = findParam(joinPoint, CommonConstants.FIELD_APP_ID);
        return arg != null ? arg.toString() : null;
    }

    /**
     * appId 策略 2: 从 beforeData 实体快照提取（内部 ID → 外部业务 ID）。
     */
    private String extractAppIdFromEntity(JsonNode node) {
        Long internalId = JsonUtils.getFieldLong(node, CommonConstants.FIELD_APP_ID);
        if (internalId == null) {
            return null;
        }
        return resolveExternalAppId(internalId);
    }

    /**
     * appId → internalId 转换（含权限校验）。
     */
    private Long resolveInternalIdFromAppId(String appId) {
        try {
            AppContext appCtx = appContextResolver.resolveAndValidate(appId);
            if (appCtx != null && appCtx.getInternalId() != null) {
                return appCtx.getInternalId();
            }
            log.warn("[OPERATE_LOG] AppContextResolver returned null internalId for appId={}", appId);
            return null;
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to resolve internalId from appId={}", appId, e);
            return null;
        }
    }

    /**
     * 从 JoinPoint 提取参数，委托 JsonUtils 生成 flat JSON。
     */
    private JsonNode buildRequestNode(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = sig.getParameterNames();
            Object[] args = joinPoint.getArgs();
            if (paramNames == null) {
                return null;
            }
            return JsonUtils.toFlatNode(paramNames, args);
        } catch (Exception e) {
            log.debug("[OPERATE_LOG] Failed to build request JSON", e);
            return null;
        }
    }

    /**
     * 从 X-App-Id 请求头提取外部 appId（Controller 层可用）
     */
    private String extractAppIdFromRequestHeader() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();
                String headerAppId = request.getHeader("X-App-Id");
                if (headerAppId != null && !headerAppId.trim().isEmpty()) {
                    return headerAppId.trim();
                }
            }
        } catch (Exception e) {
            log.debug("[OPERATE_LOG] Failed to extract appId from request header", e);
        }
        return null;
    }

    /**
     * 内部 App.id → 外部 App.appId 转换。
     */
    private String resolveExternalAppId(Long internalId) {
        if (internalId == null) {
            return null;
        }
        try {
            App app = appMapper.selectById(internalId);
            return app != null ? app.getAppId() : null;
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to resolve external appId from internalId={}", internalId, e);
            return null;
        }
    }
}
