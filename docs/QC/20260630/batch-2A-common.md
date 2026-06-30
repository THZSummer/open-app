# 批 2-A：open-server / common 审查报告

> 43 文件（42 现存逐行读 + AuditLogAspect 已删除确认）。意见按 §2.2 格式，分类按 §2.1。

## 文件覆盖表（43/43）

| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| annotation/AuditLog.java | ✅ | 0 |
| config/AsyncConfig.java | ✅ | 0 |
| config/ConnectorPlatformPropertyService.java | ✅ | 0 |
| config/IdGeneratorConfig.java | ✅ | 1 |
| config/JacksonConfig.java | ✅ | 0 |
| config/WebMvcConfig.java | ✅ | 1 |
| constants/CommonConstants.java | ✅ | 0 |
| controller/LookupController.java | ✅ | 0 |
| enums/AppIdSourceEnum.java | ✅(已删除) | 0 |
| enums/AuthTypeEnum.java | ✅ | 0 |
| enums/ConnectorPlatformConstants.java | ✅ | 0 |
| enums/ConnectorStatus.java | ✅ | 0 |
| enums/ConnectorVersionStatus.java | ✅ | 0 |
| enums/ExecutionEnums.java | ✅ | 0 |
| enums/FlowLifecycleStatus.java | ✅ | 0 |
| enums/FlowVersionStatus.java | ✅ | 0 |
| enums/OperateEnum.java | ✅ | 0 |
| enums/OperateResultEnum.java | ✅ | 0 |
| enums/ResponseCodeEnum.java | ✅ | 1 |
| enums/StatusEnum.java | ✅ | 0 |
| exception/BusinessException.java | ✅ | 0 |
| exception/GlobalExceptionHandlerV2.java | ✅ | 1 |
| file/controller/FileV2Controller.java | ✅ | 1 |
| file/entity/FileEntity.java | ✅ | 0 |
| file/mapper/FileMapper.java | ✅ | 0 |
| file/service/FileV2Service.java | ✅ | 2 |
| file/vo/FileV2VO.java | ✅ | 0 |
| id/DevIdGeneratorStrategy.java | ✅ | 1 |
| interceptor/AuditLogAspect.java | ✅(已删除,被OperateLogV2Aspect取代) | 0 |
| interceptor/DiffConfig.java | ✅ | 0 |
| interceptor/DiffField.java | ✅ | 0 |
| interceptor/OperateLogV2Aspect.java | ✅ | 0 |
| interceptor/ServiceLogAspect.java | ✅ | 1 |
| interceptor/UserResolveInterceptor.java | ✅ | 1 |
| model/ApiResponse.java | ✅ | 1 |
| model/ErrorInfo.java | ✅ | 0 |
| security/PlatformAdminPermission.java | ✅ | 0 |
| security/PlatformAdminPermissionAspect.java | ✅ | 1 |
| snapshot/EntitySnapshotLoader.java | ✅ | 0 |
| user/strategy/impl/DevUserStrategy.java | ✅ | 0 |
| user/strategy/impl/StandardUserStrategy.java | ✅ | 1 |
| util/CommonUtils.java | ✅ | 0 |
| util/JsonUtils.java | ✅ | 1 |

## QC 意见（13 条）

### 意见 1
- 大类：安全编码
- 子类：关键资源权限分配不当
- 级别：严重
- 问题原因：security/PlatformAdminPermissionAspect.java:30-43 `@Before("@annotation(PlatformAdminPermission)")` 切面 TODO 未实现，L43 `log.debug("...currently skipped")` 默认放行。`@PlatformAdminPermission` 被 7 个 Controller 36 个管理接口使用（Category/Callback/Event/Api/Approval/Sync/Debug）→ 全部对任意登录用户开放
- 修改建议：实现切面校验（校验 UserContextHolder 用户是否在平台管理员清单）；未实现前对所有 @PlatformAdminPermission 接口临时返回 403

### 意见 2
- 大类：安全编码
- 子类：客户端校验
- 级别：严重
- 问题原因：file/service/FileV2Service.java:90-162 saveFile 注释自称"图片上传"却无扩展名白名单/ContentType 魔数校验/文件大小上限。L114-120 仅取扩展名不校验类型，可上传 .jsp/.html
- 修改建议：加扩展名白名单(png/jpg/jpeg/gif/svg+xml)+ContentType 二次校验(魔数)+文件大小上限

### 意见 3
- 大类：安全编码
- 子类：关键资源权限分配不当
- 级别：严重
- 问题原因：file/controller/FileV2Controller.java:37-43 uploadImage 无 @AuthRole/@PlatformAdminPermission 等鉴权注解，任意请求可上传（QA 实测：.txt 上传成功落盘）
- 修改建议：Controller 加鉴权注解（@AuthRole 或登录态校验）

### 意见 4
- 大类：安全编码
- 子类：错误消息中暴露信息
- 级别：严重
- 问题原因：exception/GlobalExceptionHandlerV2.java:89-90 未知异常 `e.getMessage()` 拼进 `"Internal server error: "+detail` 返客户端，可能含 SQL/内部路径/库细节
- 修改建议：未知异常只返通用"系统繁忙"，detail 仅写日志

### 意见 5
- 大类：安全编码
- 子类：关键资源权限分配不当
- 级别：严重
- 问题原因：user/strategy/impl/StandardUserStrategy.java:23-34 resolve 直接 `return null`（TODO 未实现）。标准环境(test/uat/prod)所有请求用户解析返回 null → UserResolveInterceptor 用 empty 用户继续 → 生产环境用户上下文全为空，鉴权失效
- 修改建议：实现标准环境用户解析（APIG/JWT/SOA Header），未实现前生产不可上线

### 意见 6
- 大类：安全编码
- 子类：关键资源权限分配不当
- 级别：严重
- 问题原因：interceptor/UserResolveInterceptor.java:53-58 resolveUser 失败时 `UserContextHolder.set(UserContext.empty())` 继续(return true)，fail-open。任何无法解析用户的请求以空用户身份通过
- 修改建议：解析失败应返回 401/403 拒绝（fail-closed），而非 empty 用户继续

### 意见 7
- 大类：业务功能
- 子类：功能需求没有正确实现
- 级别：严重
- 问题原因：id/DevIdGeneratorStrategy.java:121-127 supports 只匹配 dev/development/local。IdGeneratorConfig.java:37-41 按环境选策略，标准环境无实现 → orElseThrow 抛 IllegalStateException → 生产启动失败
- 修改建议：实现标准环境 IdGeneratorStrategy（或生产可复用雪花算法，去掉环境限制，workerId 由配置注入）

### 意见 8
- 大类：安全编码
- 子类：日志文件泄露信息
- 级别：严重
- 问题原因：interceptor/ServiceLogAspect.java:30-48 @Before 拦截所有 @Service 方法，L47 `log.info` 打印全部入参，L60 `JsonUtils.toJson(arg)` 序列化复杂对象。若 service 参数含密码/token/apiSecret(如 updateVerifyType)，敏感信息写入日志
- 修改建议：敏感参数(apiSecret/token/password)脱敏或跳过；或收窄拦截范围排除含敏感参数的方法

### 意见 9
- 大类：编程规范
- 子类：其他编程规范问题
- 级别：一般
- 问题原因：enums/ResponseCodeEnum.java:51 NO_ADD_ROLE_PERMISSION("403201") 与 L64 NO_ABILITY_PERMISSION("403201") 错误码重复，不同语义用相同码，前端无法区分
- 修改建议：NO_ABILITY_PERMISSION 改用独立错误码（如 403401）

### 意见 10
- 大类：软件结构
- 子类：冗余重复代码
- 级别：建议
- 问题原因：model/ApiResponse.java:11 已有 @Builder，L38-53 又手写 ApiResponseBuilder + builder()，与 Lombok 生成重复
- 修改建议：删除手写 Builder，依赖 @Builder

### 意见 11
- 大类：安全编码
- 子类：使用潜在危险函数
- 级别：建议
- 问题原因：util/JsonUtils.java:150 extractSimpleProperties 用 `f.setAccessible(true)` 反射访问私有字段，SecurityManager 启用时可能被拒；且绕过封装
- 修改建议：评估是否必须反射；可改用公共 getter 或 Jackson 序列化

### 意见 12
- 大类：安全编码
- 子类：使用不充足随机数
- 级别：一般
- 问题原因：file/service/FileV2Service.java:114 `fileId = FILE_ID_PREFIX + currentTimeMillis() + "_" + new Random().nextInt(10000)`，时间戳+小随机，fileId 可预测/碰撞（同毫秒+同随机）。若 fileId 即访问凭证，可枚举
- 修改建议：fileId 改用雪花 ID 或 SecureRandom

### 意见 13
- 大类：安全编码
- 子类：关键资源权限分配不当
- 级别：严重
- 问题原因：config/WebMvcConfig.java:49-51 AppWhitelistInterceptor 只注册 `/connectors/**`+`/flows/**`。`/executions`（运行记录）等用 X-App-Id 的路径未覆盖，依赖 service 层 requireInternalAppId 抛异常(500)而非 HTTP 层 403
- 修改建议：addPathPatterns 补 `/service/open/v2/executions/**` 等所有用 X-App-Id 的路径

## 批次结论

- 严重：9（意见 1,2,3,4,5,6,7,8,13）
- 一般：2（意见 9,12）
- 建议：2（意见 10,11）

**亮点**：enums 状态机完整(ConnectorStatus/FlowVersionStatus/FlowLifecycleStatus/ConnectorVersionStatus 均 isValidTransition)；OperateLogV2Aspect 四阶段容错设计良好(每阶段 try-catch 不影响主业务)；ConnectorPlatformPropertyService 每项硬编码兜底(符合"DB缺失禁止拒绝服务")；AsyncConfig CallerRunsPolicy 保证审计不丢；AuditLogAspect 已被 OperateLogV2Aspect 干净取代。

**不放行**：意见 1(权限空校验)、2+3(文件上传 RCE)、5+6(生产鉴权失效)、7(生产启动失败)、8(日志泄露) 均为上线阻断项。common 是本次 QC 安全问题最集中的批次。
