# Open-Server 审计日志技术方案

> 版本：v2.2.0 | 日期：2026-05-26 | 作者：SDDU Build Agent

---

## 1. 概述

### 1.1 背景

能力开放平台（open-server）需要对权限订阅管理的 **11 个非 GET 接口** 实现审计日志持久化，记录应用对 API / 事件 / 回调权限的订阅、撤回、删除、配置操作，写入 `openplateform_operate_log_t` 表，用于安全审计与合规追溯。

API / 事件 / 回调的资源管理接口（ApiController / EventController / CallbackController）因无 appId 关联，不纳入审计日志。

### 1.2 目标

- 基于 **AOP + 自定义注解** 方式，零侵入业务代码
- 注解属性通过 **统一枚举 `OperateEnum`** 维护，一处定义、全局引用
- 自动捕获操作前后实体快照（before_data / after_data）
- 自动提取操作人（UserContextHolder）和客户端 IP（HttpServletRequest）
- 异步写入数据库，不阻塞主业务流程
- 审计日志写入失败不影响主业务

### 1.3 覆盖范围

| 模块 | Controller | 接口数 | app_id 策略 |
|------|-----------|:------:|------------|
| API 权限订阅 | `PermissionController` | 3 | 从路径参数 `{appId}` 获取 |
| 事件权限订阅 | `PermissionController` | 4 | 从路径参数 `{appId}` 获取 |
| 回调权限订阅 | `PermissionController` | 4 | 从路径参数 `{appId}` 获取 |
| **合计** | | **11** | |

---

## 2. 数据库表结构

```sql
CREATE TABLE `openplateform_operate_log_t` (
  `id`               bigint(20)    NOT NULL              COMMENT '主键',
  `app_id`           varchar(100)  NOT NULL              COMMENT '应用ID',
  `operate_type`     varchar(10)   NOT NULL              COMMENT '操作类型',
  `operate_object`   varchar(64)   NOT NULL              COMMENT '操作对象',
  `operate_desc_cn`  text                                COMMENT '中文描述',
  `operate_desc_en`  text                                COMMENT '英文描述',
  `operate_user`     varchar(255)  NOT NULL              COMMENT '操作人',
  `ip_address`       varchar(255)  DEFAULT NULL          COMMENT '操作人地址',
  `before_data`      text                                COMMENT '操作前数据',
  `after_data`       text                                COMMENT '操作后数据',
  `status`           tinyint(1)    DEFAULT '1'           COMMENT '0:失败 1:成功',
  `create_by`        varchar(100)  NOT NULL DEFAULT ''   COMMENT '创建人',
  `create_time`      datetime(3)   NOT NULL              COMMENT '创建时间',
  `last_update_by`   varchar(100)  NOT NULL DEFAULT ''   COMMENT '最后更新人',
  `last_update_time` datetime(3)   NOT NULL              COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_app_id_operate_object` (`app_id`, `operate_object`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='操作记录表';
```

**索引说明**：联合索引 `(app_id, operate_object)` 支持按应用+对象类型快速查询。

---

## 3. 架构设计

### 3.1 app_id 数据链路

审计日志 `app_id` 字段统一取 `openplatform_app_t.app_id`（varchar 外部业务 ID），与系统内其他模块保持一致。

```
openplatform_app_t
├── id          BIGINT(20)   ──→  subscription.app_id (内部主键，Long)
└── app_id      VARCHAR(100) ──→  审计日志 app_id (外部业务 ID，String)
                                   ↑
                              路径 {appId} 直接传入（PATH_VARIABLE 策略）
                              或 AppContextResolver.toExternalId() 转换（ENTITY 策略）
```

**已有基础设施**：

- `AppContextResolver.resolveAndValidate(String externalAppId)` → `AppContext(internalId, externalId)`
- `AppContextResolver.toExternalId(Long internalId)` → `String externalAppId`
- `AppContext.externalId` = `openplatform_app_t.app_id`（审计日志需要的值）
- `AppContext.internalId` = `openplatform_app_t.id`（subscription 存储的值）

### 3.2 整体流程

```
Client Request
    |
    v
PermissionController Method (@AuditLog)
    |
    v
OperateLogAspect (@Around)
    |-- 1. 从注解获取 OperateEnum → 解析 operateType / operateObject / descCn / descEn
    |-- 2. 从方法参数提取资源 ID (resourceIdParam)
    |-- 3. 加载 before_data (WITHDRAW/DELETE/CONFIG → subscriptionMapper.selectById)
    |-- 4. 解析 app_id (openplatform_app_t.app_id)
    |       |-- PATH_VARIABLE: 直接从方法参数 {appId} 获取（已是 varchar 外部 ID）
    |       |-- ENTITY: 从 before_data 提取 numeric app_id → AppContextResolver.toExternalId() 转换
    |-- 5. proceed() 执行 Controller 方法
    |       |-- 成功: 继续
    |       |-- 失败: 记录 status=0, 重新抛出异常
    |-- 6. 加载 after_data
    |       |-- SUBSCRIBE: null (批量操作无单一实体)
    |       |-- WITHDRAW/CONFIG: subscriptionMapper.selectById 重新查询
    |       |-- DELETE: null (实体已删除)
    |-- 7. 提取 IP (X-Forwarded-For -> X-Real-IP -> getRemoteAddr)
    |-- 8. 提取用户 (UserContextHolder.getUserName())
    |
    v
AuditLogService.saveAsync()  [异步调用]
    |-- @Async("auditLogExecutor")
    |-- @Transactional(REQUIRES_NEW)
    |-- IdGeneratorStrategy.nextId() 生成雪花 ID
    |
    v
OperateLogMapper.insert() -> openplateform_operate_log_t
```

### 3.2 与现有切面的关系

| 切面 | 职责 | 优先级 |
|------|------|:------:|
| `AuditLogAspect` | 连接流 SLF4J 日志（startFlow/stopFlow/deleteFlow） | - |
| `OperateLogAspect` | 操作日志 DB 持久化（11 个权限订阅接口） | `@Order(2)` |

两个切面互不影响，各自独立运行。

---

## 4. 文件清单

> 所有 Java 路径基于 `src/main/java/com/xxx/it/works/wecode/v2/`

| # | 文件 | 类型 | 说明 |
|:-:|------|:----:|------|
| 1 | `common/enums/OperateEnum.java` | **新建** | 统一操作枚举（合并 operateType + operateObject + descCn + descEn） |
| 2 | `common/enums/AppIdSourceEnum.java` | **保留** | app_id 来源策略枚举（PATH_VARIABLE / ENTITY） |
| 3 | `common/annotation/AuditLog.java` | 修改 | `value()` + `appIdSource()` + `resourceIdParam()` |
| 4 | `modules/auditlog/entity/OperateLog.java` | 不变 | 实体类 |
| 5 | `modules/auditlog/mapper/OperateLogMapper.java` | 不变 | Mapper 接口 |
| 6 | `resources/mapper/OperateLogMapper.xml` | 不变 | Mapper XML |
| 7 | `common/config/AsyncConfig.java` | 不变 | 线程池配置 |
| 8 | `modules/auditlog/service/AuditLogService.java` | 不变 | 异步持久化服务 |
| 9 | `common/interceptor/OperateLogAspect.java` | 修改 | 移除 ApiMapper/EventMapper/CallbackMapper，支持两种 appId 策略 |
| 10 | `modules/permission/controller/PermissionController.java` | 修改 | 11 个 @AuditLog 改用 OperateEnum |
| 11 | `modules/api/controller/ApiController.java` | **回退** | 移除 4 个 @AuditLog |
| 12 | `modules/event/controller/EventController.java` | **回退** | 移除 4 个 @AuditLog |
| 13 | `modules/callback/controller/CallbackController.java` | **回退** | 移除 4 个 @AuditLog |
| 14 | `common/enums/OperateTypeEnum.java` | **删除** | 合并入 OperateEnum |
| 15 | `common/enums/OperateObjectEnum.java` | **删除** | 合并入 OperateEnum |

---

## 5. 核心组件详细设计

### 5.1 OperateEnum -- 统一操作枚举（核心变更）

将原 `OperateTypeEnum`、`OperateObjectEnum`、descCn、descEn 合并为一个枚举，每个枚举值代表一个具体的审计操作场景。

```java
@Getter
@AllArgsConstructor
public enum OperateEnum {

    // ===== API 权限订阅 =====
    SUBSCRIBE_API_PERMISSION("SUBSCRIBE", "API_PERMISSION",
            "申请API权限", "Subscribe API Permission"),
    WITHDRAW_API_PERMISSION("WITHDRAW", "API_PERMISSION",
            "撤回API权限申请", "Withdraw API Permission"),
    DELETE_API_PERMISSION("DELETE", "API_PERMISSION",
            "删除API权限订阅", "Delete API Permission"),

    // ===== 事件权限订阅 =====
    SUBSCRIBE_EVENT_PERMISSION("SUBSCRIBE", "EVENT_PERMISSION",
            "申请事件权限", "Subscribe Event Permission"),
    CONFIG_EVENT_PERMISSION("CONFIG", "EVENT_PERMISSION",
            "配置事件消费参数", "Configure Event Subscription"),
    WITHDRAW_EVENT_PERMISSION("WITHDRAW", "EVENT_PERMISSION",
            "撤回事件权限申请", "Withdraw Event Permission"),
    DELETE_EVENT_PERMISSION("DELETE", "EVENT_PERMISSION",
            "删除事件权限订阅", "Delete Event Permission"),

    // ===== 回调权限订阅 =====
    SUBSCRIBE_CALLBACK_PERMISSION("SUBSCRIBE", "CALLBACK_PERMISSION",
            "申请回调权限", "Subscribe Callback Permission"),
    CONFIG_CALLBACK_PERMISSION("CONFIG", "CALLBACK_PERMISSION",
            "配置回调消费参数", "Configure Callback Subscription"),
    WITHDRAW_CALLBACK_PERMISSION("WITHDRAW", "CALLBACK_PERMISSION",
            "撤回回调权限申请", "Withdraw Callback Permission"),
    DELETE_CALLBACK_PERMISSION("DELETE", "CALLBACK_PERMISSION",
            "删除回调权限订阅", "Delete Callback Permission");

    /** DB operate_type 字段值 */
    private final String operateType;

    /** DB operate_object 字段值 */
    private final String operateObject;

    /** DB operate_desc_cn 字段值 */
    private final String descCn;

    /** DB operate_desc_en 字段值 */
    private final String descEn;

    /**
     * 判断是否需要加载 before_data 实体快照
     */
    public boolean needsBeforeData() {
        return "UPDATE".equals(operateType)
                || "DELETE".equals(operateType)
                || "WITHDRAW".equals(operateType)
                || "CONFIG".equals(operateType);
    }

    /**
     * 判断是否需要加载 after_data 实体快照
     */
    public boolean needsAfterData() {
        return !"SUBSCRIBE".equals(operateType)
                && !"DELETE".equals(operateType);
    }
}
```

**设计优势**：

| 对比项 | 原方案（3 个枚举 + 4 个注解字段） | 新方案（OperateEnum） |
|--------|------|------|
| 注解字段 | `operateType` + `operateObject` + `descCn` + `descEn` + `appIdSource` | `value()` 一个字段 |
| 枚举文件 | `OperateTypeEnum` + `OperateObjectEnum` + `AppIdSourceEnum` 共 3 个 | `OperateEnum` + `AppIdSourceEnum` 共 2 个 |
| 新增接口 | 需组合多个枚举值 + 手动填描述 | 新增一个 OperateEnum 枚举值即可 |
| 一致性 | 组合可能出错（如 DELETE + SUBSCRIBE 描述） | 枚举值固定，不会错配 |
| before/after 策略 | 切面中 switch 判断 | 枚举自带 `needsBeforeData()` / `needsAfterData()` |

### 5.2 AppIdSourceEnum -- app_id 来源策略（保留扩展性）

控制审计日志中 `app_id` 字段的取值方式，支持两种策略：

```java
@Getter
@AllArgsConstructor
public enum AppIdSourceEnum {

    /**
     * 从方法参数中直接提取 appId
     *
     * <p>适用于路径中包含 {appId} 的接口，如：
     * /service/open/v2/apps/{appId}/apis/subscribe</p>
     *
     * <p>此时 {appId} 已是 openplatform_app_t.app_id (varchar 外部业务 ID)</p>
     */
    PATH_VARIABLE,

    /**
     * 从实体快照中反向查找 appId
     *
     * <p>适用于接口参数中无 appId，但实体记录中包含 app_id 字段的场景。
     * 切面先加载实体快照（before_data），从中提取 numeric app_id (openplatform_app_t.id)，
     * 再通过 AppContextResolver.toExternalId() 转换为 varchar app_id (openplatform_app_t.app_id)。</p>
     *
     * <p>典型场景：接口路径仅包含资源 ID，需要通过查询数据库获取所属 appId。
     * 例如：/service/open/v2/subscriptions/{id}/withdraw
     * 通过 subscriptionMapper.selectById(id) 查到记录后取 numeric app_id，
     * 再调用 AppContextResolver.toExternalId(numericAppId) 得到 varchar app_id。</p>
     */
    ENTITY;
}
```

**策略选择**：

| 策略 | appId 来源 | 转换方式 | 当前使用情况 |
|------|-----------|---------|:----------:|
| `PATH_VARIABLE` | 方法参数 `appId` | 直接使用（已是 varchar 外部 ID） | 11 个权限订阅接口全部使用 |
| `ENTITY` | 实体快照中的 numeric `app_id` | `AppContextResolver.toExternalId()` 转换 | 预留扩展，当前未使用 |

**ENTITY 策略提取逻辑**（切面中）：

```java
/**
 * ENTITY 策略：从实体快照中提取 numeric app_id，再转换为 varchar app_id
 *
 * @param entityJson 实体快照 JSON（before_data）
 * @return openplatform_app_t.app_id (varchar 外部业务 ID)
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
        // 转换为 varchar app_id (openplatform_app_t.app_id)
        return appContextResolver.toExternalId(internalId);
    } catch (Exception e) {
        log.warn("[OPERATE_LOG] Failed to extract appId from entity", e);
        return "unknown";
    }
}
```

### 5.3 @AuditLog 注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作枚举值（包含 operateType / operateObject / descCn / descEn）
     */
    OperateEnum value();

    /**
     * app_id 来源策略
     *
     * <ul>
     *   <li>PATH_VARIABLE（默认）：从方法参数 appId 直接获取</li>
     *   <li>ENTITY：从实体快照 JSON 的 app_id 字段获取</li>
     * </ul>
     */
    AppIdSourceEnum appIdSource() default AppIdSourceEnum.PATH_VARIABLE;

    /**
     * 资源 ID 参数名
     *
     * <p>用于从方法参数中提取资源 ID，加载实体快照（before_data / after_data）</p>
     * <p>默认 "id" 匹配 @PathVariable String id</p>
     * <p>SUBSCRIBE 操作设为 "appId"（批量操作无单一实体 ID）</p>
     */
    String resourceIdParam() default "id";
}
```

**使用示例**：

```java
// 路径含 {appId}：直接从参数获取（默认策略）
@AuditLog(OperateEnum.SUBSCRIBE_API_PERMISSION, resourceIdParam = "appId")
@AuditLog(OperateEnum.WITHDRAW_API_PERMISSION)       // appIdSource 默认 PATH_VARIABLE

// 路径无 {appId}：从实体快照获取（扩展用法）
@AuditLog(value = OperateEnum.WITHDRAW_API_PERMISSION,
          appIdSource = AppIdSourceEnum.ENTITY)
```

### 5.4 OperateLog 实体（不变）

映射 `openplateform_operate_log_t` 表。

```java
@Data
public class OperateLog implements Serializable {
    private Long id;              // 主键（雪花 ID）
    private String appId;         // 应用 ID（PATH_VARIABLE 或 ENTITY 策略获取）
    private String operateType;   // 操作类型 (SUBSCRIBE/WITHDRAW/DELETE/CONFIG)
    private String operateObject; // 操作对象 (API_PERMISSION/EVENT_PERMISSION/CALLBACK_PERMISSION)
    private String operateDescCn; // 中文描述
    private String operateDescEn; // 英文描述
    private String operateUser;   // 操作人
    private String ipAddress;     // 客户端 IP
    private String beforeData;    // 操作前数据（JSON）
    private String afterData;     // 操作后数据（JSON）
    private Integer status;       // 0:失败 1:成功
    private String createBy;
    private Date createTime;
    private String lastUpdateBy;
    private Date lastUpdateTime;
}
```

### 5.5 OperateLogMapper（不变）

```java
@Mapper
public interface OperateLogMapper {
    int insert(OperateLog operateLog);
}
```

对应 MyBatis XML：

```xml
<insert id="insert" parameterType="...OperateLog">
    INSERT INTO openplateform_operate_log_t (
        id, app_id, operate_type, operate_object,
        operate_desc_cn, operate_desc_en,
        operate_user, ip_address, before_data, after_data, status,
        create_by, create_time, last_update_by, last_update_time
    ) VALUES (
        #{id}, #{appId}, #{operateType}, #{operateObject},
        #{operateDescCn}, #{operateDescEn},
        #{operateUser}, #{ipAddress}, #{beforeData}, #{afterData}, #{status},
        #{createBy}, #{createTime}, #{lastUpdateBy}, #{lastUpdateTime}
    )
</insert>
```

### 5.6 AsyncConfig -- 线程池配置（不变）

```java
@Configuration
public class AsyncConfig {

    @Bean("auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("audit-log-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
```

**关键决策**：

| 参数 | 值 | 理由 |
|------|:--:|------|
| corePoolSize | 2 | 审计日志为低频写入，2 个常驻线程足够 |
| maxPoolSize | 5 | 高并发时可扩展 |
| queueCapacity | 200 | 缓冲突发流量 |
| RejectedHandler | CallerRunsPolicy | 队列满时由调用线程同步执行，保证日志不丢失 |

### 5.7 AuditLogService -- 异步持久化（不变）

```java
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final OperateLogMapper operateLogMapper;
    private final IdGeneratorStrategy idGenerator;

    @Async("auditLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveAsync(OperateLog operateLog) {
        try {
            if (operateLog.getId() == null) {
                operateLog.setId(idGenerator.nextId());
            }
            Date now = new Date();
            if (operateLog.getCreateTime() == null) {
                operateLog.setCreateTime(now);
            }
            if (operateLog.getLastUpdateTime() == null) {
                operateLog.setLastUpdateTime(now);
            }
            if (operateLog.getCreateBy() == null || operateLog.getCreateBy().isEmpty()) {
                operateLog.setCreateBy(operateLog.getOperateUser());
            }
            if (operateLog.getLastUpdateBy() == null || operateLog.getLastUpdateBy().isEmpty()) {
                operateLog.setLastUpdateBy(operateLog.getOperateUser());
            }
            operateLogMapper.insert(operateLog);
        } catch (Exception e) {
            log.error("[AUDIT] Failed to save audit log: type={}, object={}, user={}",
                    operateLog.getOperateType(),
                    operateLog.getOperateObject(),
                    operateLog.getOperateUser(), e);
        }
    }
}
```

**设计要点**：

| 特性 | 实现 | 目的 |
|------|------|------|
| `@Async` | `auditLogExecutor` 线程池 | 不阻塞主请求 |
| `REQUIRES_NEW` | 独立事务 | 审计日志写入失败不回滚主业务 |
| 内部 try-catch | `log.error()` | 异常不向调用方传播 |
| `IdGeneratorStrategy` | 雪花算法 | 分布式唯一 ID |

### 5.8 OperateLogAspect -- AOP 切面（简化）

```java
@Aspect
@Component
@RequiredArgsConstructor
@Order(2)
public class OperateLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final AppContextResolver appContextResolver;  // 用于 ENTITY 策略的 appId 转换
}
```

**相比 v1.0 的变化**：
- 移除 `ApiMapper`、`EventMapper`、`CallbackMapper` 依赖（资源管理类接口不再审计）
- 使用 `OperateEnum` 替代分散的 operateType / operateObject 判断
- 保留 `AppIdSourceEnum` 双策略支持（PATH_VARIABLE / ENTITY），确保可扩展性
- 新增 `AppContextResolver` 依赖，用于 ENTITY 策略中将 numeric internalId 转换为 varchar externalId

**依赖说明**：

| 依赖 | 用途 |
|------|------|
| `AuditLogService` | 异步保存审计日志 |
| `ObjectMapper` | JSON 序列化/反序列化实体快照 |
| `SubscriptionMapper` | 查询订阅实体快照（before_data / after_data） |
| `AppContextResolver` | ENTITY 策略：numeric internalId → varchar externalId |

#### 核心 @Around 流程

```java
@Around("@annotation(auditLog)")
public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
    OperateEnum op = auditLog.value();
    AppIdSourceEnum appIdSource = auditLog.appIdSource();

    // Step 1: 提取资源 ID
    Long resourceId = extractResourceId(joinPoint, auditLog.resourceIdParam());

    // Step 2: 加载 before_data（ENTITY 策略同时用于提取 appId）
    String beforeData = null;
    if (op.needsBeforeData() && resourceId != null) {
        beforeData = loadSubscriptionSnapshot(resourceId);
    }

    // Step 3: 解析 app_id
    String appId;
    if (appIdSource == AppIdSourceEnum.PATH_VARIABLE) {
        appId = extractAppIdFromParams(joinPoint);
    } else {
        // ENTITY 策略：从 before_data 实体快照中提取
        appId = extractAppIdFromEntity(beforeData);
    }

    // Step 4: 执行目标方法
    Object result;
    int status = 1;
    try {
        result = joinPoint.proceed();
    } catch (Throwable ex) {
        status = 0;
        saveOperateLog(op, appId, joinPoint, beforeData, null, status);
        throw ex;
    }

    // Step 5: 加载 after_data
    String afterData = null;
    if (op.needsAfterData() && resourceId != null) {
        afterData = loadSubscriptionSnapshot(resourceId);
    }

    // Step 6: 保存审计日志
    saveOperateLog(op, appId, joinPoint, beforeData, afterData, status);
    return result;
}
```

#### 实体快照加载

当前所有操作对象均为 `*_PERMISSION`，统一使用 `subscriptionMapper`：

```java
private String loadSubscriptionSnapshot(Long id) {
    try {
        Object entity = subscriptionMapper.selectById(id);
        return entity != null ? objectMapper.writeValueAsString(entity) : null;
    } catch (Exception e) {
        log.warn("[OPERATE_LOG] Subscription snapshot load failed: id={}", id, e);
        return null;
    }
}
```

#### app_id 解析（双策略）

```java
/**
 * PATH_VARIABLE 策略：从方法参数中提取 appId
 *
 * <p>路径 {appId} 已是 openplatform_app_t.app_id (varchar 外部业务 ID)，直接使用</p>
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
 * <p>适用于接口路径不含 appId，但实体记录中有 app_id 字段的场景。</p>
 * <p>数据链路：
 *   实体 JSON 中的 appId (Long) = openplatform_app_t.id (内部主键)
 *   → AppContextResolver.toExternalId(internalId)
 *   → openplatform_app_t.app_id (varchar 外部业务 ID) = 审计日志 app_id</p>
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
```

#### IP 地址提取

从 `RequestContextHolder` 获取 `HttpServletRequest`，按优先级取值：

```
X-Forwarded-For  ->  X-Real-IP  ->  request.getRemoteAddr()
```

如果 `X-Forwarded-For` 包含多个 IP（经过多级代理），取第一个（客户端真实 IP）。

---

## 6. Controller 注解分配表

仅 `PermissionController`，所有接口 app_id 从路径参数 `{appId}` 获取。

### 6.1 API 权限订阅（3 个接口）

| 接口# | 方法 | @AuditLog 配置 |
|:-----:|------|----------------|
| #29 | `subscribeApiPermissions` | `@AuditLog(OperateEnum.SUBSCRIBE_API_PERMISSION, resourceIdParam = "appId")` |
| #30 | `withdrawApiSubscription` | `@AuditLog(OperateEnum.WITHDRAW_API_PERMISSION)` |
| #31 | `deleteApiSubscription` | `@AuditLog(OperateEnum.DELETE_API_PERMISSION)` |

### 6.2 事件权限订阅（4 个接口）

| 接口# | 方法 | @AuditLog 配置 |
|:-----:|------|----------------|
| #34 | `subscribeEventPermissions` | `@AuditLog(OperateEnum.SUBSCRIBE_EVENT_PERMISSION, resourceIdParam = "appId")` |
| #35 | `configEventSubscription` | `@AuditLog(OperateEnum.CONFIG_EVENT_PERMISSION)` |
| #36 | `withdrawEventSubscription` | `@AuditLog(OperateEnum.WITHDRAW_EVENT_PERMISSION)` |
| #37 | `deleteEventSubscription` | `@AuditLog(OperateEnum.DELETE_EVENT_PERMISSION)` |

### 6.3 回调权限订阅（4 个接口）

| 接口# | 方法 | @AuditLog 配置 |
|:-----:|------|----------------|
| #40 | `subscribeCallbackPermissions` | `@AuditLog(OperateEnum.SUBSCRIBE_CALLBACK_PERMISSION, resourceIdParam = "appId")` |
| #41 | `configCallbackSubscription` | `@AuditLog(OperateEnum.CONFIG_CALLBACK_PERMISSION)` |
| #42 | `withdrawCallbackSubscription` | `@AuditLog(OperateEnum.WITHDRAW_CALLBACK_PERMISSION)` |
| #43 | `deleteCallbackSubscription` | `@AuditLog(OperateEnum.DELETE_CALLBACK_PERMISSION)` |

---

## 7. before_data / after_data 捕获矩阵

| OperateEnum 前缀 | before_data | after_data | 说明 |
|:-----------------:|:-----------:|:----------:|------|
| **SUBSCRIBE_*** | null | null | 批量操作，无单一实体 |
| **WITHDRAW_*** | `subscriptionMapper.selectById(id)` | `subscriptionMapper.selectById(id)` | 操作前后分别查询 |
| **DELETE_*** | `subscriptionMapper.selectById(id)` | null | 操作后实体已删除 |
| **CONFIG_*** | `subscriptionMapper.selectById(id)` | `subscriptionMapper.selectById(id)` | 操作前后分别查询 |

---

## 8. 错误处理策略

| 场景 | 处理方式 | 影响范围 |
|------|---------|---------|
| 主操作成功，审计日志写入失败 | `saveAsync()` 内部 catch，`log.error()` | 主请求不受影响，审计日志丢失 |
| 主操作失败（抛异常） | 切面 catch 记录 `status=0` + `afterData=null`，re-throw | 审计日志记录失败操作，异常由全局异常处理器处理 |
| 实体快照加载失败 | 返回 null，`log.warn()` | 审计日志以部分数据保存 |
| 异步线程池队列满 | `CallerRunsPolicy` 由调用线程同步执行 | 无数据丢失，主请求变慢 |
| HttpServletRequest 不可用 | IP 设为 null | DDL 允许 NULL |
| UserContextHolder 无用户信息 | operateUser 为 null | 切面构造日志时 catch |

---

## 9. 依赖关系

```
OpenServerApplication (@EnableAsync)
    |
    +-- AsyncConfig (@Configuration)
    |       └── auditLogExecutor (ThreadPoolTaskExecutor)
    |
    +-- OperateLogAspect (@Aspect @Component @Order(2))
    |       ├── AuditLogService
    |       │       ├── OperateLogMapper
    |       │       └── IdGeneratorStrategy
    |       ├── ObjectMapper (Spring 内置)
    |       ├── SubscriptionMapper
    |       └── AppContextResolver (ENTITY 策略: internalId → externalId)
    |
    +-- PermissionController (@AuditLog 注解, 11 个方法)
```

---

## 10. 验证方式

### 10.1 SUBSCRIBE 操作验证

```
POST /service/open/v2/apps/APP001/apis/subscribe
Body: { "permissionIds": [1, 2, 3] }

预期: openplateform_operate_log_t 新增一条记录
  - app_id = "APP001"
  - operate_type = "SUBSCRIBE"
  - operate_object = "API_PERMISSION"
  - operate_desc_cn = "申请API权限"
  - before_data = null
  - after_data = null
  - status = 1
```

### 10.2 WITHDRAW 操作验证

```
POST /service/open/v2/apps/APP001/apis/123/withdraw

预期:
  - app_id = "APP001"
  - operate_type = "WITHDRAW"
  - operate_object = "API_PERMISSION"
  - before_data = { "id": 123, "status": 0, ... }  (撤回前)
  - after_data  = { "id": 123, "status": 3, ... }  (撤回后)
```

### 10.3 DELETE 操作验证

```
DELETE /service/open/v2/apps/APP001/events/456

预期:
  - app_id = "APP001"
  - operate_type = "DELETE"
  - operate_object = "EVENT_PERMISSION"
  - before_data = { "id": 456, ... }  (删除前)
  - after_data = null
```

### 10.4 CONFIG 操作验证

```
PUT /service/open/v2/apps/APP001/events/456/config
Body: { "channel": "HTTP", "url": "https://...", ... }

预期:
  - app_id = "APP001"
  - operate_type = "CONFIG"
  - operate_object = "EVENT_PERMISSION"
  - before_data = { "id": 456, "channel": "MQ", ... }  (配置前)
  - after_data  = { "id": 456, "channel": "HTTP", ... }  (配置后)
```

### 10.5 异常场景验证

```
模拟主操作抛出异常

预期:
  - status = 0
  - after_data = null
  - before_data 正常记录（如果已加载）
  - 异常被重新抛出，由全局异常处理器返回错误响应
```

---

## 11. 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0.0 | 2026-05-26 | 初始版本，23 个接口（含资源管理 12 个 + 权限订阅 11 个） |
| v2.0.0 | 2026-05-26 | 移除资源管理类接口（无 appId），仅保留 11 个权限订阅接口；合并 OperateTypeEnum + OperateObjectEnum + descCn + descEn 为统一 OperateEnum；移除 AppIdSourceEnum；简化切面依赖 |
| v2.1.0 | 2026-05-26 | 恢复 AppIdSourceEnum（PATH_VARIABLE / ENTITY 双策略），保留 appId 解析扩展性；ENTITY 策略支持从实体快照 JSON 中提取 app_id，适用于接口无直接 appId 参数的场景 |
| v2.2.0 | 2026-05-26 | 明确 app_id 取 openplatform_app_t.app_id (varchar 外部业务 ID)；新增 app_id 数据链路说明（openplatform_app_t → subscription → audit log）；ENTITY 策略改用 AppContextResolver.toExternalId() 将 numeric internalId 转换为 varchar externalId；OperateLogAspect 新增 AppContextResolver 依赖 |
