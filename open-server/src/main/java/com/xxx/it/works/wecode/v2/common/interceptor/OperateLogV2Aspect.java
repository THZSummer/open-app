package com.xxx.it.works.wecode.v2.common.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.xxx.it.works.wecode.v2.common.annotation.AuditLog;
import com.xxx.it.works.wecode.v2.common.constants.CommonConstants;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoaderFactory;
import com.xxx.it.works.wecode.v2.common.util.CommonUtils;
import com.xxx.it.works.wecode.v2.common.util.JsonUtils;
import com.xxx.it.works.wecode.v2.modules.app.constants.AppPropertyConstants;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作日志持久化切面 V2
 *
 * <p>基于 @AuditLog 注解的 @Around 切面，自动捕获操作前后实体快照、
 * 用户信息和 IP 地址，异步写入操作日志表。</p>
 *
 * <p><b>appId 自动提取</b>：方法参数 → beforeData 快照 → 返回值（按顺序自动尝试）</p>
 * <p><b>错误隔离</b>：切面任何环节异常不得影响主业务</p>
 *
 * @author SDDU Build Agent
 * @version 5.0.0
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

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    private static class AuditContext {
        OperateEnum op;
        Long resourceId;
        String beforeData;
        String appId;
    }

    /**
     * 拦截所有标注 @AuditLog 注解的方法
     *
     * <p>错误隔离策略：
     * <ul>
     *   <li>Phase 1（before proceed）：审计准备异常 → 仅记录日志，主方法照常执行</li>
     *   <li>Phase 2（proceed）：主方法异常 → 记录 status=0 审计日志后重新抛出</li>
     *   <li>Phase 3（after proceed）：审计后处理异常 → 仅记录日志，主方法结果正常返回</li>
     * </ul>
     */
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
            // 失败时不加载 afterData：业务方法可能在 @Transactional 中，
            // catch 时事务尚未回滚，查 DB 会拿到未提交的脏数据。
            writeAuditLog(ctx, null, 0);
            throw ex;
        }

        String afterData = loadAfterData(ctx, joinPoint, result);
        writeAuditLog(ctx, afterData, 1);
        return result;
    }

    // ===== Phase 1: 审计前准备 =====

    /**
     * Phase 1: 审计前准备。失败返回 null，主方法将照常执行但不写审计。
     */
    private AuditContext prepareAuditContext(ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        AuditContext ctx = new AuditContext();
        ctx.op = auditLog.value();
        try {
            ctx.resourceId = extractResourceId(joinPoint, auditLog.resourceIdParam());

            // appId 自动策略 1: 从方法参数提取
            ctx.appId = extractAppIdFromParams(joinPoint);

            // appId → internalId 转换（用于 EntitySnapshotLoader 加载快照）
            if (ctx.resourceId == null && isValid(ctx.appId)) {
                ctx.resourceId = resolveInternalIdFromAppId(ctx.appId);
            }

            loadBeforeData(ctx, joinPoint);
            return ctx;
        } catch (Exception e) {
            log.error("[OPERATE_LOG] Pre-proceed audit logic failed, skipping audit", e);
            return null;
        }
    }

    private void loadBeforeData(AuditContext ctx, ProceedingJoinPoint joinPoint) {
        if (ctx.op.needsBeforeData()) {
            EntitySnapshotLoader loader = snapshotLoaderFactory.getLoader(ctx.op.getOperateObject());
            if (loader != null) {
                ctx.beforeData = JsonUtils.toJson(loader.loadBeforeData(joinPoint, ctx.resourceId));
            }
        }

        // appId 自动策略 2: 从 beforeData 快照提取
        if (!isValid(ctx.appId)) {
            ctx.appId = extractAppIdFromEntity(ctx.beforeData);
        }
    }

    // ===== Phase 3: 加载 afterData =====

    private String loadAfterData(AuditContext ctx, ProceedingJoinPoint joinPoint, Object result) {
        try {
            // appId 自动策略 3: 从返回值提取
            if (!isValid(ctx.appId)) {
                ctx.appId = extractAppIdFromResponse(result);
                if (ctx.resourceId == null && isValid(ctx.appId)) {
                    ctx.resourceId = resolveInternalIdFromAppId(ctx.appId);
                }
            }

            if (ctx.op.needsAfterData()) {
                EntitySnapshotLoader loader = snapshotLoaderFactory.getLoader(ctx.op.getOperateObject());
                if (loader != null) {
                    return JsonUtils.toJson(loader.loadAfterData(joinPoint, ctx.resourceId, result));
                }
                // 无 Loader，从返回值提取
                return extractEntityFromResult(result);
            }
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Post-proceed snapshot extraction failed", e);
        }
        return null;
    }

    /**
     * Phase 4: 渲染中英文描述 + 异步保存审计日志。
     */
    private void writeAuditLog(AuditContext ctx, String afterData, int status) {
        try {
            OperateLog logEntry = new OperateLog();
            String userId = UserContextHolder.getUserId();
            Date now = new Date();

            logEntry.setAppId(ctx.appId);
            logEntry.setOperateType(ctx.op.getOperateType());
            logEntry.setOperateObject(ctx.op.getOperateObjectCn());
            logEntry.setOperateDescCn(renderDesc(ctx, afterData, true));
            logEntry.setOperateDescEn(renderDesc(ctx, afterData, false));
            logEntry.setOperateUser(userId);
            logEntry.setIpAddress(CommonUtils.extractIpAddress());
            logEntry.setBeforeData(ctx.beforeData);
            logEntry.setAfterData(afterData);
            logEntry.setStatus(status);
            logEntry.setCreateBy(userId);
            logEntry.setLastUpdateBy(userId);
            logEntry.setCreateTime(now);
            logEntry.setLastUpdateTime(now);

            auditLogService.saveAsync(logEntry);
        } catch (Exception e) {
            log.error("[OPERATE_LOG] Failed to construct/save log entry", e);
        }
    }

    /**
     * 渲染描述模板，模板为空或渲染异常时回退到静态 descCn/descEn
     */
    private String renderDesc(AuditContext ctx, String afterData, boolean isChinese) {
        String template = isChinese ? ctx.op.getTemplateCn() : ctx.op.getTemplateEn();
        String fallback = isChinese ? ctx.op.getDescCn() : ctx.op.getDescEn();
        if (template == null || template.isEmpty()) {
            return fallback;
        }
        try {
            String rendered = renderTemplate(template, ctx.op, ctx.beforeData,
                    afterData, isChinese);
            return rendered != null ? rendered : fallback;
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Template rendering failed", e);
            return fallback;
        }
    }

    /**
     * 渲染审计描述模板，将 ${xxx} 占位符替换为实际值。${diffFields} 触发字段级 diff 渲染。
     */
    private String renderTemplate(String template, OperateEnum op,
                                  String beforeData, String afterData,
                                  boolean isChinese) {
        if (template == null || template.isEmpty()) {
            return null;
        }

        JsonNode before = JsonUtils.parseJson(beforeData);
        JsonNode after = JsonUtils.parseJson(afterData);

        if (template.contains(DiffConfig.DIFF_FIELDS_PLACEHOLDER)) {
            String diffResult = renderDiffFields(op, before, after, isChinese);
            template = template.replace(DiffConfig.DIFF_FIELDS_PLACEHOLDER, diffResult);
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String value = resolvePlaceholder(placeholder, before, after);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 渲染字段级 diff（仅展示实际修改的字段）。
     * 字段映射、中/英格式模板、labelOnly 格式、分隔符全部是 DiffConfig 配置数据，切面零硬编码。
     */
    private String renderDiffFields(OperateEnum op, JsonNode before, JsonNode after, boolean isChinese) {
        if (before == null || after == null) {
            return "";
        }
        DiffConfig config = op.diffConfig();
        if (config == null) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        for (DiffField field : config.fields()) {
            String beforeVal = JsonUtils.getFieldText(before, field.name());
            String afterVal = JsonUtils.getFieldText(after, field.name());
            if (Objects.equals(beforeVal, afterVal)) {
                continue;
            }
            lines.add(config.renderField(field, beforeVal, afterVal, isChinese));
        }
        return String.join(config.separator(), lines);
    }

    /**
     * 从 beforeData/afterData JSON 中提取占位符值
     */
    private String resolvePlaceholder(String placeholder, JsonNode before, JsonNode after) {
        String value = JsonUtils.getFieldText(after, placeholder);
        if (value == null) {
            value = JsonUtils.getFieldText(before, placeholder);
        }
        return value != null ? value : "";
    }

    private boolean isValid(String appId) {
        return appId != null && !CommonConstants.UNKNOWN.equals(appId);
    }

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
        return CommonConstants.UNKNOWN;
    }

    /**
     * 从实体快照中提取 appId（numeric app_id → varchar app_id 转换）。
     * 实体 JSON 中的 appId 是 openplatform_app_t.id（内部主键），
     * 通过 AppContextResolver.toExternalId() 转换为 openplatform_app_t.app_id（varchar 业务 ID）。
     */
    private String extractAppIdFromEntity(String entityJson) {
        if (entityJson == null) {
            return CommonConstants.UNKNOWN;
        }
        try {
            JsonNode node = JsonUtils.parseJson(entityJson);
            if (node == null) {
                return CommonConstants.UNKNOWN;
            }
            JsonNode appIdNode = node.get("appId");
            if (appIdNode == null) {
                appIdNode = node.get(AppPropertyConstants.COL_APP_ID);
            }
            if (appIdNode == null || appIdNode.isNull()) {
                return CommonConstants.UNKNOWN;
            }
            Long internalId = appIdNode.asLong();
            return appContextResolver.toExternalId(internalId);
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to extract appId from entity", e);
            return CommonConstants.UNKNOWN;
        }
    }

    /**
     * 从方法返回值 ApiResponse.data.appId 中提取 appId。
     * 适用于 CREATE 类操作（如 createApp），方法参数中无 appId，
     * 但返回值中包含新建实体的 appId。
     */
    private String extractAppIdFromResponse(Object result) {
        if (result == null) {
            return CommonConstants.UNKNOWN;
        }
        try {
            if (result instanceof ApiResponse<?> apiResponse) {
                Object data = apiResponse.getData();
                if (data != null) {
                    JsonNode dataNode = JsonUtils.toTree(data);
                    JsonNode appIdNode = dataNode != null ? dataNode.get("appId") : null;
                    if (appIdNode != null && !appIdNode.isNull()) {
                        return appIdNode.asText();
                    }
                }
            }
            return CommonConstants.UNKNOWN;
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to extract appId from response", e);
            return CommonConstants.UNKNOWN;
        }
    }

    private String extractEntityFromResult(Object result) {
        if (result == null) {
            return null;
        }
        if (result instanceof ApiResponse<?> apiResponse) {
            return JsonUtils.toJson(apiResponse.getData());
        }
        return JsonUtils.toJson(result);
    }
}
