# Open-Server 审计日志技术方案

> 版本：v1.0.0 | 日期：2026-05-26 | 作者：SDDU Build Agent

---

## 1. 概述

### 1.1 背景

能力开放平台（open-server）需要对 API、事件、回调三类业务资源的 **23 个非 GET 接口** 实现审计日志持久化，记录操作人对资源的增删改查行为，写入 `openplateform_operate_log_t` 表，用于安全审计与合规追溯。

### 1.2 目标

- 基于 **AOP + 自定义注解** 方式，零侵入业务代码
- 自动捕获操作前后实体快照（before_data / after_data）
- 自动提取操作人（UserContextHolder）和客户端 IP（HttpServletRequest）
- 异步写入数据库，不阻塞主业务流程
- 审计日志写入失败不影响主业务

### 1.3 覆盖范围

| 模块 | Controller | 接口数 | app_id 策略 |
|------|-----------|:------:|------------|
| API 管理 | `ApiController` | 4 | 固定值 "platform" |
| 事件管理 | `EventController` | 4 | 固定值 "platform" |
| 回调管理 | `CallbackController` | 4 | 固定值 "platform" |
| 权限管理 | `PermissionController` | 11 | 从路径参数 `{appId}` 获取 |
| **合计** | | **23** | |

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

### 3.1 整体流程

```
Client Request
    |
    v
Controller Method (@AuditLog)
    |
    v
OperateLogAspect (@Around)
    |-- 1. 读取注解元数据 (operateType, operateObject, descCn, descEn)
    |-- 2. 解析 app_id ("platform" 或从 {appId} 路径参数获取)
    |-- 3. 从方法参数提取资源 ID (resourceIdParam)
    |-- 4. 加载 before_data 实体快照 (UPDATE/DELETE/WITHDRAW/CONFIG)
    |       |-- API -> apiMapper.selectById(id)
    |       |-- EVENT -> eventMapper.selectById(id)
    |       |-- CALLBACK -> callbackMapper.selectById(id)
    |       |-- *_PERMISSION -> subscriptionMapper.selectById(id)
    |-- 5. proceed() 执行 Controller 方法
    |       |-- 成功: 继续
    |       |-- 失败: 记录 status=0, 重新抛出异常
    |-- 6. 加载 after_data
    |       |-- CREATE: 从 ApiResponse.data 提取返回实体
    |       |-- UPDATE/WITHDRAW/CONFIG: mapper.selectById(id) 重新查询
    |       |-- DELETE: null (实体已删除)
    |       |-- SUBSCRIBE: null (批量操作无单一实体)
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
| `OperateLogAspect` | 操作日志 DB 持久化（23 个接口） | `@Order(2)` |

两个切面互不影响，各自独立运行。

---

## 4. 文件清单

> 所有 Java 路径基于 `src/main/java/com/xxx/it/works/wecode/v2/`

| # | 文件 | 类型 | 说明 |
|:-:|------|:----:|------|
| 1 | `common/enums/OperateTypeEnum.java` | 新建 | 操作类型枚举 |
| 2 | `common/enums/OperateObjectEnum.java` | 新建 | 操作对象枚举 |
| 3 | `common/enums/AppIdSourceEnum.java` | 新建 | app_id 来源枚举 |
| 4 | `common/annotation/AuditLog.java` | 新建 | 自定义注解 |
| 5 | `modules/auditlog/entity/OperateLog.java` | 新建 | 实体类 |
| 6 | `modules/auditlog/mapper/OperateLogMapper.java` | 新建 | Mapper 接口 |
| 7 | `resources/mapper/OperateLogMapper.xml` | 新建 | Mapper XML |
| 8 | `common/config/AsyncConfig.java` | 新建 | 线程池配置 |
| 9 | `modules/auditlog/service/AuditLogService.java` | 新建 | 异步持久化服务 |
| 10 | `common/interceptor/OperateLogAspect.java` | 新建 | AOP 切面 |
| 11 | `modules/api/controller/ApiController.java` | 修改 | 4 个 @AuditLog |
| 12 | `modules/event/controller/EventController.java` | 修改 | 4 个 @AuditLog |
| 13 | `modules/callback/controller/CallbackController.java` | 修改 | 4 个 @AuditLog |
| 14 | `modules/permission/controller/PermissionController.java` | 修改 | 11 个 @AuditLog |
| 15 | `OpenServerApplication.java` | 修改 | 添加 @EnableAsync |

---

## 5. 核心组件详细设计

### 5.1 枚举定义

#### OperateTypeEnum -- 操作类型

对应 `operate_type` 字段（`varchar(10)`）。

```java
@Getter @AllArgsConstructor
public enum OperateTypeEnum {
    CREATE("CREATE", "创建"),
    UPDATE("UPDATE", "更新"),
    DELETE("DELETE", "删除"),
    WITHDRAW("WITHDRAW", "撤回"),
    SUBSCRIBE("SUBSCRIBE", "订阅"),
    CONFIG("CONFIG", "配置");

    private final String code;        // DB 存储值
    private final String description; // 中文描述

    public static OperateTypeEnum fromCode(String code) { ... }
    public static boolean isValidCode(String code) { ... }
}
```

#### OperateObjectEnum -- 操作对象

对应 `operate_object` 字段（`varchar(64)`）。

```java
@Getter @AllArgsConstructor
public enum OperateObjectEnum {
    API("API", "API资源"),
    EVENT("EVENT", "事件资源"),
    CALLBACK("CALLBACK", "回调资源"),
    API_PERMISSION("API_PERMISSION", "API权限订阅"),
    EVENT_PERMISSION("EVENT_PERMISSION", "事件权限订阅"),
    CALLBACK_PERMISSION("CALLBACK_PERMISSION", "回调权限订阅");

    private final String code;
    private final String description;

    public static OperateObjectEnum fromCode(String code) { ... }
    public static boolean isValidCode(String code) { ... }
}
```

#### AppIdSourceEnum -- app_id 来源

控制审计日志中 `app_id` 字段的取值方式。

```java
@Getter @AllArgsConstructor
public enum AppIdSourceEnum {
    PLATFORM("platform"),       // 固定值，12 个资源管理接口
    PATH_VARIABLE("appId");     // 从路径参数 {appId} 获取，11 个权限订阅接口

    private final String value;
}
```

### 5.2 @AuditLog 注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    OperateTypeEnum operateType();                          // 操作类型
    OperateObjectEnum operateObject();                      // 操作对象
    String descCn() default "";                             // 中文描述
    String descEn() default "";                             // 英文描述
    AppIdSourceEnum appIdSource() default PLATFORM;         // app_id 来源
    String resourceIdParam() default "id";                  // 资源 ID 参数名
}
```

**使用示例**：

```java
// 资源管理接口（app_id = "platform"）
@AuditLog(operateType = OperateTypeEnum.CREATE,
          operateObject = OperateObjectEnum.API,
          descCn = "注册API",
          descEn = "Register API")

// 权限订阅接口（app_id 从路径参数获取）
@AuditLog(operateType = OperateTypeEnum.SUBSCRIBE,
          operateObject = OperateObjectEnum.API_PERMISSION,
          descCn = "申请API权限",
          descEn = "Subscribe API Permission",
          appIdSource = AppIdSourceEnum.PATH_VARIABLE,
          resourceIdParam = "appId")
```

### 5.3 OperateLog 实体

映射 `openplateform_operate_log_t` 表。

```java
@Data
public class OperateLog implements Serializable {
    private Long id;              // 主键（雪花 ID）
    private String appId;         // 应用 ID
    private String operateType;   // 操作类型 (CREATE/UPDATE/...)
    private String operateObject; // 操作对象 (API/EVENT/...)
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

### 5.4 OperateLogMapper

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

### 5.5 AsyncConfig -- 线程池配置

```java
@Configuration
public class AsyncConfig {

    @Bean("auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);          // 核心线程数
        executor.setMaxPoolSize(5);           // 最大线程数
        executor.setQueueCapacity(200);       // 队列容量
        executor.setThreadNamePrefix("audit-log-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时同步执行
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

### 5.6 AuditLogService -- 异步持久化

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

### 5.7 OperateLogAspect -- AOP 切面（核心）

```java
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
}
```

#### 核心 @Around 流程

```java
@Around("@annotation(auditLog)")
public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
    OperateTypeEnum operateType = auditLog.operateType();
    OperateObjectEnum operateObject = auditLog.operateObject();

    // Step 1: 提取资源 ID
    Long resourceId = extractResourceId(joinPoint, auditLog.resourceIdParam());

    // Step 2: 加载 before_data
    String beforeData = null;
    if (needsBeforeData(operateType) && resourceId != null) {
        beforeData = loadEntitySnapshot(operateObject, resourceId);
    }

    // Step 3: 执行目标方法
    Object result;
    int status = 1;
    try {
        result = joinPoint.proceed();
    } catch (Throwable ex) {
        status = 0;
        saveOperateLog(auditLog, joinPoint, beforeData, null, status);
        throw ex;
    }

    // Step 4: 加载 after_data
    String afterData = null;
    if (operateType == OperateTypeEnum.CREATE) {
        afterData = extractEntityFromResult(result);
    } else if (operateType != OperateTypeEnum.DELETE && resourceId != null) {
        afterData = loadEntitySnapshot(operateObject, resourceId);
    }

    // Step 5: 保存审计日志
    saveOperateLog(auditLog, joinPoint, beforeData, afterData, status);
    return result;
}
```

#### 实体快照加载

根据 `OperateObjectEnum` 选择对应 Mapper：

```java
private String loadEntitySnapshot(OperateObjectEnum obj, Long id) {
    Object entity = switch (obj) {
        case API         -> apiMapper.selectById(id);
        case EVENT       -> eventMapper.selectById(id);
        case CALLBACK    -> callbackMapper.selectById(id);
        case API_PERMISSION, EVENT_PERMISSION, CALLBACK_PERMISSION
                        -> subscriptionMapper.selectById(id);
    };
    return entity != null ? objectMapper.writeValueAsString(entity) : null;
}
```

#### IP 地址提取

从 `RequestContextHolder` 获取 `HttpServletRequest`，按优先级取值：

```
X-Forwarded-For  ->  X-Real-IP  ->  request.getRemoteAddr()
```

如果 `X-Forwarded-For` 包含多个 IP（经过多层代理），取第一个（客户端真实 IP）。

---

## 6. Controller 注解分配表

### 6.1 ApiController（#9 ~ #14，app_id = "platform"）

| 接口# | 方法 | 操作 | @AuditLog 配置 |
|:-----:|------|:----:|----------------|
| #11 | `createApi` | CREATE | `(CREATE, API, "注册API", "Register API")` |
| #12 | `updateApi` | UPDATE | `(UPDATE, API, "更新API", "Update API", resourceIdParam="id")` |
| #13 | `deleteApi` | DELETE | `(DELETE, API, "删除API", "Delete API", resourceIdParam="id")` |
| #14 | `withdrawApi` | WITHDRAW | `(WITHDRAW, API, "撤回API", "Withdraw API", resourceIdParam="id")` |

### 6.2 EventController（#15 ~ #20，app_id = "platform"）

| 接口# | 方法 | 操作 | @AuditLog 配置 |
|:-----:|------|:----:|----------------|
| #17 | `createEvent` | CREATE | `(CREATE, EVENT, "注册事件", "Register Event")` |
| #18 | `updateEvent` | UPDATE | `(UPDATE, EVENT, "更新事件", "Update Event", resourceIdParam="id")` |
| #19 | `deleteEvent` | DELETE | `(DELETE, EVENT, "删除事件", "Delete Event", resourceIdParam="id")` |
| #20 | `withdrawEvent` | WITHDRAW | `(WITHDRAW, EVENT, "撤回事件", "Withdraw Event", resourceIdParam="id")` |

### 6.3 CallbackController（#21 ~ #26，app_id = "platform"）

| 接口# | 方法 | 操作 | @AuditLog 配置 |
|:-----:|------|:----:|----------------|
| #23 | `createCallback` | CREATE | `(CREATE, CALLBACK, "注册回调", "Register Callback")` |
| #24 | `updateCallback` | UPDATE | `(UPDATE, CALLBACK, "更新回调", "Update Callback", resourceIdParam="id")` |
| #25 | `deleteCallback` | DELETE | `(DELETE, CALLBACK, "删除回调", "Delete Callback", resourceIdParam="id")` |
| #26 | `withdrawCallback` | WITHDRAW | `(WITHDRAW, CALLBACK, "撤回回调", "Withdraw Callback", resourceIdParam="id")` |

### 6.4 PermissionController（#27 ~ #43，app_id = PATH_VARIABLE）

| 接口# | 方法 | 操作 | @AuditLog 配置 |
|:-----:|------|:----:|----------------|
| #29 | `subscribeApiPermissions` | SUBSCRIBE | `(SUBSCRIBE, API_PERMISSION, resourceIdParam="appId")` |
| #30 | `withdrawApiSubscription` | WITHDRAW | `(WITHDRAW, API_PERMISSION, resourceIdParam="id")` |
| #31 | `deleteApiSubscription` | DELETE | `(DELETE, API_PERMISSION, resourceIdParam="id")` |
| #34 | `subscribeEventPermissions` | SUBSCRIBE | `(SUBSCRIBE, EVENT_PERMISSION, resourceIdParam="appId")` |
| #35 | `configEventSubscription` | CONFIG | `(CONFIG, EVENT_PERMISSION, resourceIdParam="id")` |
| #36 | `withdrawEventSubscription` | WITHDRAW | `(WITHDRAW, EVENT_PERMISSION, resourceIdParam="id")` |
| #37 | `deleteEventSubscription` | DELETE | `(DELETE, EVENT_PERMISSION, resourceIdParam="id")` |
| #40 | `subscribeCallbackPermissions` | SUBSCRIBE | `(SUBSCRIBE, CALLBACK_PERMISSION, resourceIdParam="appId")` |
| #41 | `configCallbackSubscription` | CONFIG | `(CONFIG, CALLBACK_PERMISSION, resourceIdParam="id")` |
| #42 | `withdrawCallbackSubscription` | WITHDRAW | `(WITHDRAW, CALLBACK_PERMISSION, resourceIdParam="id")` |
| #43 | `deleteCallbackSubscription` | DELETE | `(DELETE, CALLBACK_PERMISSION, resourceIdParam="id")` |

> 所有 PermissionController 注解均配置 `appIdSource = AppIdSourceEnum.PATH_VARIABLE`。

---

## 7. before_data / after_data 捕获矩阵

| 操作类型 | before_data | after_data | 说明 |
|---------|:-----------:|:----------:|------|
| **CREATE** | null | `ApiResponse.data` JSON | 操作前实体不存在，从返回值提取 |
| **UPDATE** | `mapper.selectById(id)` | `mapper.selectById(id)` | 操作前后分别查询 |
| **DELETE** | `mapper.selectById(id)` | null | 操作后实体已删除 |
| **WITHDRAW** | `mapper.selectById(id)` | `mapper.selectById(id)` | 操作前后分别查询 |
| **SUBSCRIBE** | null | null | 批量操作，无单一实体 |
| **CONFIG** | `subscriptionMapper.selectById(id)` | `subscriptionMapper.selectById(id)` | 操作前后分别查询 |

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
    |       ├── ApiMapper
    |       ├── EventMapper
    |       ├── CallbackMapper
    |       └── SubscriptionMapper
    |
    +-- Controllers (@AuditLog 注解)
            ├── ApiController
            ├── EventController
            ├── CallbackController
            └── PermissionController
```

---

## 10. 验证方式

### 10.1 CREATE 操作验证

```
POST /service/open/v2/apis
Body: { "nameCn": "测试API", ... }

预期: openplateform_operate_log_t 新增一条记录
  - operate_type = "CREATE"
  - operate_object = "API"
  - before_data = null
  - after_data = { "id": 123, "nameCn": "测试API", ... }
  - app_id = "platform"
  - status = 1
```

### 10.2 UPDATE 操作验证

```
PUT /service/open/v2/apis/123
Body: { "nameCn": "更新后的API", ... }

预期:
  - operate_type = "UPDATE"
  - before_data = { "id": 123, "nameCn": "测试API", ... }  (更新前)
  - after_data  = { "id": 123, "nameCn": "更新后的API", ... }  (更新后)
```

### 10.3 DELETE 操作验证

```
DELETE /service/open/v2/apis/123

预期:
  - operate_type = "DELETE"
  - before_data = { "id": 123, ... }  (删除前)
  - after_data = null
```

### 10.4 权限订阅接口验证

```
POST /service/open/v2/apps/APP001/apis/subscribe
Body: { "permissionIds": [1, 2, 3] }

预期:
  - app_id = "APP001"  (从路径参数获取)
  - operate_type = "SUBSCRIBE"
  - before_data = null
  - after_data = null
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
| v1.0.0 | 2026-05-26 | 初始版本，完成 23 个接口审计日志实现 |
