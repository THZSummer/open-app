package com.xxx.it.works.wecode.v2.common.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.annotation.AuditLog;
import com.xxx.it.works.wecode.v2.common.constants.CommonConstants;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoaderFactory;
import com.xxx.it.works.wecode.v2.modules.app.constants.AppPropertyConstants;
import com.xxx.it.works.wecode.v2.common.enums.AppIdSourceEnum;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作日志持久化切面 V2
 *
 * <p>基于 @AuditLog 注解的 @Around 切面，自动捕获操作前后实体快照、
 * 用户信息和 IP 地址，异步写入 openplatform_operate_log_t 表</p>
 *
 * <p>与 AuditLogAspect（连接流 SLF4J 日志）互不影响</p>
 *
 * <p>实体快照加载采用策略模式（EntitySnapshotLoader + Factory），
 * 根据 OperateEnum.operateObject 路由到对应的 Loader 实现，支持不同资源类型从不同表加载</p>
 *
 * <p><b>错误隔离原则</b>：切面任何环节的异常均不得影响主业务接口，
 * 所有非 proceed 操作均包裹在 try-catch 中，异常仅记录日志并跳过审计</p>
 *
 * <p><b>TemplateRenderer</b>：若 OperateEnum.templateCn 非空，基于 beforeData/afterData
 * 渲染 ${xxx} 占位符，生成可读性强的描述文案写入 desc_cn / desc_en；否则回退静态 descCn/descEn</p>
 *
 * @author SDDU Build Agent
 * @version 3.0.0
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
    private final List<LogFieldResolver> resolvers;
    private final List<AuditDataProvider> dataProviders;

    /**
     * 占位符正则：${fieldName}
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    /**
     * 占位符解析器映射（懒初始化）
     */
    private Map<String, LogFieldResolver> resolverMap;

    /**
     * AuditDataProvider 映射（懒初始化）
     */
    private Map<OperateEnum, AuditDataProvider> dataProviderMap;

    /**
     * 审计上下文，用于在 before / proceed / after 阶段之间传递中间状态
     */
    private static class AuditContext {
        OperateEnum op;
        AppIdSourceEnum appIdSource;
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
     * </p>
     *
     * @param joinPoint 连接点
     * @param auditLog  注解实例
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的异常（审计日志不影响异常传播）
     */
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {

        // Phase 1: 审计前准备（失败则跳过审计，主方法照常执行）
        AuditContext ctx = prepareAuditContext(joinPoint, auditLog);
        if (ctx == null) {
            return joinPoint.proceed();
        }

        // Phase 2: 执行目标方法（失败时记录 status=0 审计日志后重新抛出）
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            recordFailureAudit(ctx, joinPoint);
            throw ex;
        }

        // Phase 3 & 4: 加载 afterData + 渲染模板 + 保存审计日志
        finalizeSuccessAudit(ctx, joinPoint, result);
        return result;
    }

    // ===== Phase 实现 =====

    /**
     * Phase 1: 审计前准备
     *
     * <p>依次完成：resourceId 提取 → appId 提取 → beforeData 快照加载。
     * 任何异常仅记录日志并返回 null，主方法将照常执行但不写审计。</p>
     *
     * @return 准备好的审计上下文；失败返回 null
     */
    private AuditContext prepareAuditContext(ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        AuditContext ctx = new AuditContext();
        ctx.op = auditLog.value();
        ctx.appIdSource = auditLog.appIdSource();
        try {
            extractResourceAndAppId(joinPoint, auditLog, ctx);
            loadBeforeData(ctx, joinPoint);
            return ctx;
        } catch (Exception e) {
            log.error("[OPERATE_LOG] Pre-proceed audit logic failed, skipping audit for this request", e);
            return null;
        }
    }

    /**
     * Phase 1a/1b/1c: 提取 resourceId 和 appId
     *
     * <ul>
     *   <li>1a: 从方法参数提取数字 resourceId（versionId 等 Long 参数）</li>
     *   <li>1b: PATH_VARIABLE 策略提前提取 appId（RESPONSE_FIELD / ENTITY 留待后续 Phase）</li>
     *   <li>1c: 若 resourceId 仍为 null 但有 appId，通过 AppContextResolver 转为 internalId</li>
     * </ul>
     */
    private void extractResourceAndAppId(ProceedingJoinPoint joinPoint, AuditLog auditLog, AuditContext ctx) {
        // 1a: 从方法参数提取数字 resourceId
        ctx.resourceId = extractResourceId(joinPoint, auditLog.resourceIdParam());

        // 1b: PATH_VARIABLE 策略提前提取 appId（RESPONSE_FIELD 等 proceed 后再说，ENTITY 等 beforeData 加载后再说）
        if (ctx.appIdSource == AppIdSourceEnum.PATH_VARIABLE) {
            ctx.appId = extractAppIdFromParams(joinPoint);
        }

        // 1c: appId → internalId 转换，用于 EntitySnapshotLoader 加载快照
        if (ctx.resourceId == null && ctx.appId != null && !CommonConstants.UNKNOWN.equals(ctx.appId)) {
            ctx.resourceId = resolveInternalIdFromAppId(ctx.appId);
        }
    }

    /**
     * Phase 1d/1e/1f: 加载 beforeData 快照
     *
     * <ul>
     *   <li>1d: 通过 EntitySnapshotLoader 加载（needsBeforeData 或 ENTITY 策略触发）</li>
     *   <li>1e: ENTITY 策略从 beforeData 中提取 appId</li>
     *   <li>1f: EntitySnapshotLoader 未命中时回退到 AuditDataProvider</li>
     * </ul>
     */
    private void loadBeforeData(AuditContext ctx, ProceedingJoinPoint joinPoint) {
        // 1d: EntitySnapshotLoader 加载
        if ((ctx.op.needsBeforeData() || ctx.appIdSource == AppIdSourceEnum.ENTITY)
                && ctx.resourceId != null) {
            ctx.beforeData = loadEntitySnapshot(ctx.resourceId, ctx.op.getOperateObject());
        }

        // 1e: ENTITY 策略从 beforeData 提取 varchar appId
        if (ctx.appIdSource == AppIdSourceEnum.ENTITY) {
            ctx.appId = extractAppIdFromEntity(ctx.beforeData);
        }

        // 1f: AuditDataProvider 回退
        if (ctx.beforeData == null && getDataProviderMap().containsKey(ctx.op)) {
            try {
                ctx.beforeData = getDataProviderMap().get(ctx.op).provideBeforeData(joinPoint);
            } catch (Exception e) {
                log.warn("[OPERATE_LOG] AuditDataProvider.provideBeforeData failed for {}", ctx.op, e);
            }
        }
    }

    /**
     * Phase 2 失败分支：记录 status=0 的审计日志
     *
     * <p>失败时不加载 afterData：业务方法可能在 @Transactional 中，
     * catch 时事务尚未回滚，查 DB 会拿到未提交的脏数据。
     * afterData=null 表示"未捕获操作后状态"，比脏数据更诚实。</p>
     */
    private void recordFailureAudit(AuditContext ctx, ProceedingJoinPoint joinPoint) {
        writeAuditLog(ctx, joinPoint, null, null, 0);
    }

    /**
     * Phase 3 & 4 成功分支：加载 afterData → 渲染模板 → 保存审计日志
     */
    private void finalizeSuccessAudit(AuditContext ctx, ProceedingJoinPoint joinPoint, Object result) {
        String afterData = loadAfterData(ctx, joinPoint, result);
        writeAuditLog(ctx, joinPoint, result, afterData, 1);
    }

    /**
     * 渲染中英文描述 + 异步保存审计日志（成功/失败共用）
     *
     * <p>模板渲染异常仅记录日志，描述回退为 null（saveOperateLog 内部再回退到静态 descCn/descEn）。
     * 保存异常由 saveOperateLog 内部 try-catch 兜底，不影响主业务。</p>
     */
    private void writeAuditLog(AuditContext ctx, ProceedingJoinPoint joinPoint,
                               Object result, String afterData, int status) {
        String descCn = null;
        String descEn = null;
        try {
            descCn = renderTemplate(ctx.op.getTemplateCn(), ctx.op, ctx.beforeData,
                    afterData, joinPoint, result, true);
            descEn = renderTemplate(ctx.op.getTemplateEn(), ctx.op, ctx.beforeData,
                    afterData, joinPoint, result, false);
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Template rendering failed", e);
        }
        saveOperateLog(ctx.op, ctx.appId, joinPoint, ctx.beforeData, afterData,
                descCn, descEn, status);
    }

    /**
     * Phase 3: 加载 afterData 快照
     *
     * <ul>
     *   <li>RESPONSE_FIELD 策略：从响应中提取 appId，并按需转 internalId</li>
     *   <li>needsAfterData 触发：有 resourceId 走 EntitySnapshotLoader，否则从结果提取</li>
     *   <li>EntitySnapshotLoader 未命中时回退到 AuditDataProvider</li>
     * </ul>
     */
    private String loadAfterData(AuditContext ctx, ProceedingJoinPoint joinPoint, Object result) {
        String afterData = null;
        try {
            // RESPONSE_FIELD: proceed 后才能拿到 appId
            if (ctx.appIdSource == AppIdSourceEnum.RESPONSE_FIELD) {
                ctx.appId = extractAppIdFromResponse(result);
                // CREATE 类操作无 resourceId（新建前不存在），proceed 后用 appId 反查
                if (ctx.resourceId == null && ctx.appId != null
                        && !CommonConstants.UNKNOWN.equals(ctx.appId)) {
                    ctx.resourceId = resolveInternalIdFromAppId(ctx.appId);
                }
            }

            if (ctx.op.needsAfterData()) {
                if (ctx.resourceId != null) {
                    afterData = loadEntitySnapshot(ctx.resourceId, ctx.op.getOperateObject());
                } else {
                    afterData = extractEntityFromResult(result);
                }
            }
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Post-proceed snapshot extraction failed", e);
        }

        // AuditDataProvider 回退
        if (afterData == null && getDataProviderMap().containsKey(ctx.op)) {
            try {
                afterData = getDataProviderMap().get(ctx.op).provideAfterData(joinPoint, result);
            } catch (Exception e) {
                log.warn("[OPERATE_LOG] AuditDataProvider.provideAfterData failed for {}", ctx.op, e);
            }
        }
        return afterData;
    }

    // ===== 私有辅助方法 =====

    /**
     * 构造并保存操作日志
     *
     * <p>整体包裹 try-catch，任何异常仅记录日志，不影响主业务</p>
     *
     * @param renderedDescCn 渲染后的中文描述，null 时回退 op.descCn
     * @param renderedDescEn 渲染后的英文描述，null 时回退 op.descEn
     */
    private void saveOperateLog(OperateEnum op, String appId, ProceedingJoinPoint joinPoint,
                                String beforeData, String afterData,
                                String renderedDescCn, String renderedDescEn, int status) {
        try {
            OperateLog logEntry = new OperateLog();

            // 基础字段
            logEntry.setAppId(appId);
            logEntry.setOperateType(op.getOperateType());
            logEntry.setOperateObject(op.getOperateObjectCn());
            // 优先使用渲染后的描述，null 时回退静态 descCn/descEn
            logEntry.setOperateDescCn(renderedDescCn != null ? renderedDescCn : op.getDescCn());
            logEntry.setOperateDescEn(renderedDescEn != null ? renderedDescEn : op.getDescEn());

            // 用户信息（取 userId）
            String userId = UserContextHolder.getUserId();
            logEntry.setOperateUser(userId);
            logEntry.setIpAddress(extractIpAddress());

            // 前后数据
            logEntry.setBeforeData(beforeData);
            logEntry.setAfterData(afterData);
            logEntry.setStatus(status);

            // 时间戳和操作人
            Date now = new Date();
            logEntry.setCreateBy(userId);
            logEntry.setLastUpdateBy(userId);
            logEntry.setCreateTime(now);
            logEntry.setLastUpdateTime(now);

            // 异步保存
            auditLogService.saveAsync(logEntry);

        } catch (Exception e) {
            log.error("[OPERATE_LOG] Failed to construct/save log entry", e);
        }
    }

    /**
     * 渲染审计描述模板
     *
     * <p>将 ${xxx} 占位符替换为 beforeData/afterData 中的实际值。
     * 特殊占位符 ${diffFields} 触发字段级 diff 渲染（读 {@link OperateEnum#diffConfig()} 配置）。</p>
     *
     * @param template   模板字符串（含 ${xxx} 占位符），null 时返回 null
     * @param op         操作枚举（提供 diffConfig）
     * @param beforeData before_data JSON 字符串
     * @param afterData  after_data JSON 字符串
     * @param isChinese  true 渲染中文 diff，false 渲染英文 diff（透传给 {@link DiffConfig#renderField}）
     * @return 渲染后的描述文案
     */
    private String renderTemplate(String template, OperateEnum op,
                                  String beforeData, String afterData,
                                  ProceedingJoinPoint joinPoint, Object result,
                                  boolean isChinese) {
        if (template == null || template.isEmpty()) {
            return null;
        }

        JsonNode before = parseJson(beforeData);
        JsonNode after = parseJson(afterData);

        // 处理 ${diffFields} 特殊占位符
        if (template.contains("${diffFields}")) {
            String diffResult = renderDiffFields(op, before, after, isChinese);
            template = template.replace("${diffFields}", diffResult);
        }

        // 处理其他 ${xxx} 占位符
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String value = resolvePlaceholder(placeholder, op, before, after, joinPoint, result);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : ""));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 渲染字段级 diff（仅展示实际修改的字段）
     *
     * <p>切面只负责：遍历 {@link DiffConfig#fields()} → 从 before/after JSON 提取值 →
     * 比对是否变化 → 委托 {@link DiffConfig#renderField} 套模板 → 拼接。</p>
     *
     * <p><b>格式不在切面里</b>：字段映射、中/英格式模板、labelOnly 格式、分隔符
     * 全部是 {@link OperateEnum#diffConfig()} 返回的配置数据，切面零硬编码。</p>
     *
     * @param op        操作枚举（提供 diffConfig）
     * @param before    操作前实体 JSON
     * @param after     操作后实体 JSON
     * @param isChinese true 渲染中文，false 渲染英文
     * @return 多行 diff 文案，无变化或无配置时返回空串
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
            String beforeVal = getJsonFieldText(before, field.name());
            String afterVal = getJsonFieldText(after, field.name());

            if (Objects.equals(beforeVal, afterVal)) {
                continue;
            }

            // 委托给配置对象套模板渲染单行（格式、语言、labelOnly 全在配置侧）
            lines.add(config.renderField(field, beforeVal, afterVal, isChinese));
        }

        return String.join(config.separator(), lines);
    }

    /**
     * 解析单个占位符
     *
     * <p>优先委托给对应的 {@link LogFieldResolver}，无匹配时走通用字段提取</p>
     */
    private String resolvePlaceholder(String placeholder, OperateEnum op,
                                      JsonNode before, JsonNode after,
                                      ProceedingJoinPoint joinPoint, Object result) {
        // ${diffFields} 已在 renderTemplate 中单独处理
        if ("diffFields".equals(placeholder)) {
            return "${diffFields}";
        }

        // 委托给对应的 FieldResolver
        LogFieldResolver resolver = getResolverMap().get(placeholder);
        if (resolver != null) {
            String resolved = resolver.resolve(before, after, op);
            if (resolved != null && !resolved.isEmpty()) {
                return resolved;
            }
        }

        // 通用字段提取：优先 after，其次 before
        String value = getJsonFieldText(after, placeholder);
        if (value == null) {
            value = getJsonFieldText(before, placeholder);
        }
        if (value != null) {
            return value;
        }

        // AuditDataProvider 回退：从方法参数中直接提取占位符值
        AuditDataProvider provider = getDataProviderMap().get(op);
        if (provider != null) {
            try {
                Map<String, String> fields = provider.provideTemplateFields(joinPoint, result);
                if (fields != null && fields.containsKey(placeholder)) {
                    String v = fields.get(placeholder);
                    if (v != null && !v.isEmpty()) {
                        return v;
                    }
                }
            } catch (Exception e) {
                log.warn("[OPERATE_LOG] AuditDataProvider.provideTemplateFields failed for {}", placeholder, e);
            }
        }

        return "";
    }

    private Map<String, LogFieldResolver> getResolverMap() {
        if (resolverMap == null) {
            resolverMap = new HashMap<>();
            for (LogFieldResolver r : resolvers) {
                resolverMap.put(r.placeholderName(), r);
            }
        }
        return resolverMap;
    }

    private Map<OperateEnum, AuditDataProvider> getDataProviderMap() {
        if (dataProviderMap == null) {
            dataProviderMap = new HashMap<>();
            for (AuditDataProvider p : dataProviders) {
                dataProviderMap.put(p.supportedOperation(), p);
            }
        }
        return dataProviderMap;
    }

    /**
     * 从 JsonNode 中安全提取字段文本值
     */
    private String getJsonFieldText(JsonNode node, String fieldName) {
        if (node == null || fieldName == null) {
            return null;
        }
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return field.asText();
    }

    /**
     * 安全解析 JSON 字符串为 JsonNode
     */
    private JsonNode parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to parse JSON for template rendering", e);
            return null;
        }
    }

    // ===== AppId 提取策略 =====

    /**
     * 将 varchar appId（外部业务 ID）转换为数字 internalId（内部主键）
     *
     * <p>通过 AppContextResolver.resolveAndValidate() 完成转换，
     * 用于后续 EntitySnapshotLoader.loadById(internalId) 加载实体快照</p>
     *
     * @param appId varchar 外部业务 ID（如 app_1780903836377_2819）
     * @return 数字内部主键 ID，转换失败返回 null
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
        return CommonConstants.UNKNOWN;
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
            return CommonConstants.UNKNOWN;
        }
        try {
            JsonNode node = objectMapper.readTree(entityJson);
            JsonNode appIdNode = node.get("appId");
            if (appIdNode == null) {
                appIdNode = node.get(AppPropertyConstants.COL_APP_ID);
            }
            if (appIdNode == null || appIdNode.isNull()) {
                return CommonConstants.UNKNOWN;
            }
            // 实体中存储的是 numeric app_id (openplatform_app_t.id)
            Long internalId = appIdNode.asLong();
            // 通过 AppContextResolver 转换为 varchar app_id (openplatform_app_t.app_id)
            return appContextResolver.toExternalId(internalId);
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to extract appId from entity", e);
            return CommonConstants.UNKNOWN;
        }
    }

    /**
     * RESPONSE_FIELD 策略：从方法返回值 ApiResponse.data.appId 中提取 appId
     *
     * <p>适用于 CREATE 类操作（如 createApp），方法参数中无 appId，
     * 但返回的 ApiResponse&lt;CreateAppVO&gt; 中 data.appId 即为新建应用的 varchar 业务 ID。</p>
     */
    private String extractAppIdFromResponse(Object result) {
        if (result == null) {
            return CommonConstants.UNKNOWN;
        }
        try {
            if (result instanceof ApiResponse<?> apiResponse) {
                Object data = apiResponse.getData();
                if (data != null) {
                    // 尝试从 data 对象中提取 appId 字段
                    JsonNode dataNode = objectMapper.valueToTree(data);
                    JsonNode appIdNode = dataNode.get("appId");
                    if (appIdNode != null && !appIdNode.isNull()) {
                        return appIdNode.asText();
                    }
                }
            }
            log.warn("[OPERATE_LOG] RESPONSE_FIELD: could not extract appId from result, class={}",
                    result.getClass().getSimpleName());
            return CommonConstants.UNKNOWN;
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to extract appId from response", e);
            return CommonConstants.UNKNOWN;
        }
    }

    // ===== 快照加载 =====

    /**
     * 通过策略工厂加载实体快照（JSON 序列化）
     *
     * <p>根据 operateObject 路由到对应的 EntitySnapshotLoader 实现</p>
     *
     * @param id            资源 ID
     * @param operateObject 操作对象标识（如 API_PERMISSION / EVENT_PERMISSION）
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

    // ===== IP 提取 =====

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
            if (ip == null || ip.isEmpty() || CommonConstants.UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || CommonConstants.UNKNOWN.equalsIgnoreCase(ip)) {
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
