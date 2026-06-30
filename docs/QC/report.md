# 后端代码 QC 最终报告

> open-server + market-server。基线 `4650daed`(2026-06-10)→HEAD。本次审查逐文件读核心 service/controller，数据类(dto/vo/entity/enum)基于阶段0快扫确认无系统性安全模式。

## 覆盖情况（诚实声明）

| 模块 | 核心service/controller | 数据类(dto/vo/entity/enum/mapper) |
|------|----------------------|----------------------------------|
| market(49) | ✅ 逐行读(approval全链路/chatbotbindtab/dictionary/lookup) | ✅ 逐行读 |
| common(43) | ✅ 逐行读(security/exception/file/interceptor/config/model/util/user/id) | ✅ 逐行读(含enums 12) |
| app(35) | ✅ 逐行读(AppServiceImpl 729行全) | ✅ 逐行读 |
| flow(21) | ✅ 逐行读(FlowService469/Validator450/Deploy/Copy) | ✅ 逐行读 |
| connector(18) | ✅ 逐行读(ConnectorService329) | 扫描(阶段0确认无安全模式) |
| ability(15) | ✅ 逐行读(AbilityServiceImpl238) | 扫描 |
| connectorversion(9) | ✅ 逐行读(ConnectorVersionService648) | 扫描 |
| version(13) | ✅ 逐行读(VersionServiceImpl432) | 扫描 |
| approval(open)(13) | ✅ 逐行读(ApprovalService962/ApprovalEngine855) | 扫描 |
| member(12) | ✅ 逐行读(MemberServiceImpl293) | 扫描 |
| permission(8) | ⚠️ 部分读(PermissionService 前120行/1424) | 扫描 |
| 小模块(54) | ✅ 读(flowexecrecord) ⚠️ 未读(card/flowversion/callback/api/sync/auditlog/debug/employee/event service) | 未覆盖 |

> **限制说明**：permission 后1304行、小模块(2F)多数service、非java(xml/yml/pom/sql)、test 因单会话context极限未能逐行读。数据类经阶段0快扫确认全库无 `${}`/SELECT \*/硬编码密钥。

## 🔬 QA 实测（CRITICAL 已验证可利用）

open-server 运行中(PID 468972, dev, health=UP)，curl 实测：

| 测试 | 结果 | 结论 |
|------|------|------|
| GET /categories(带X-User-Id) | **200+完整分类树** | ✅ C1权限绕过**实测可利用** |
| POST /file/upload-image(.txt) | **200+上传成功落盘** | ✅ C2任意文件上传**实测可利用** |
| GET /executions(无X-App-Id) | **500** App context not initialized | ✅ executions配置缺陷 |
| GET /flows(无X-App-Id) | **403** 缺少X-App-Id | ✅ 对照组正常 |

## 🔴 CRITICAL 问题（6 个，上线阻断）

### C1. 平台管理员权限空校验（36接口）
- 位置：`common/security/PlatformAdminPermissionAspect.java:30-43`
- @PlatformAdminPermission 切面 TODO 未实现，默认放行 → 7 Controller 36 接口对任意登录用户开放（含 OpDebugProxyController 调试代理）

### C2. 文件上传无类型校验+无鉴权
- 位置：`common/file/service/FileV2Service.java:90-162`、`FileV2Controller.java:37`
- 无扩展名白名单/ContentType/大小校验 + Controller 无鉴权 → 可传 .jsp（实测 .txt 成功）

### C3. 应用凭证假加密（明文存储+返回）
- 位置：`app/service/impl/AppServiceImpl.java:711-727`
- encryptApiSecret/decryptApiSecret/decryptSk **空实现**（return 原文）→ apiSecret/sk **明文存储** DB + getAppIdentity/getVerifyType **明文返回**前端

### C4. 标准环境用户解析未实现（生产鉴权失效）
- 位置：`common/user/strategy/impl/StandardUserStrategy.java:23-34`
- resolve return null → 生产所有请求用户为空 → 鉴权完全失效

### C5. 用户解析 fail-open
- 位置：`common/interceptor/UserResolveInterceptor.java:53-58`
- resolveUser 失败时 set(UserContext.empty()) 继续(return true)，不拒绝

### C6. 标准环境无 ID 生成策略（生产启动失败）
- 位置：`common/id/DevIdGeneratorStrategy.java:121-127` + `IdGeneratorConfig.java:37-41`
- DevIdGeneratorStrategy 只 support dev → 生产 orElseThrow 启动失败

## 🟠 MAJOR 问题（按模块）

### market(batch-1, 16条)
- 审批并发无锁(ApprovalEngine.process) / 节点越界未校验 / N+1查询 / 整页try-catch / businessId类型不同构(String vs Long) / MAX(version_code)字符串比较 / e.getMessage()泄露 / 绑定竞态 / tenantId空 / 硬删除

### common(batch-2A, 13条)
- ServiceLogAspect入参全打印(日志泄露密码/token) / GlobalExceptionHandler e.getMessage()泄露 / WebMvcConfig executions未纳入拦截器 / ResponseCodeEnum 403201重复 / FileV2Service fileId Random可预测 / ApiResponse Builder重复 / JsonUtils反射setAccessible

### app(batch-2B, 8条)
- getAppIdentity/getVerifyType明文返回sk/apiSecret / 凭证权限仅成员级(应限owner/admin) / generateAppId Random碰撞 / sk生成可预测 / AppCommonService notifyCardService TODO空实现

### flow(batch-2C, 4条)
- FlowCopyService重试循环无碰撞检查 / FlowService列表N+1(版本查询) / FlowPublishValidator JSON异常message泄露 / Op*Mapper命名不统一

### connector+ability+connectorversion(核心已读)
- Op*Mapper命名重构不彻底(6文件引用) / ConnectorVersionService ObjectMapper未注入(new实例) / JSON异常message多处泄露

### approval(open)(核心已读)
- ApprovalEngine approve/reject **并发无锁**(同market) / updateResourceStatus L762 **catch吞异常不rethrow**(审批commit但资源更新失败→数据不一致) / reject L579 无越界检查(approve有)

### version(核心已读)
- VersionServiceImpl 状态机+审批联动规范，compareSemVer版本比较正确(修复了market的字符串比较问题) ✅

### member(核心已读)
- 角色权限矩阵标杆(Owner/Admin/Developer) ✅

## ✨ 亮点（保持）

- SQL规范优秀（全库无注入、无SELECT*、LIKE全#{}参数化）
- connector/flow 应用隔离标杆（代码级+SQL级appId双校验）
- member 角色权限矩阵（Owner/Admin/Developer细粒度）
- FlowPublishValidator 9项发布校验完整 + GraalVM JS沙箱安全
- ApprovalHandlerFactory 策略模式 @PostConstruct 自动注册
- OperateLogV2Aspect 四阶段容错审计
- ConnectorPlatformPropertyService 硬编码兜底（DB缺失不拒绝服务）
- version compareSemVer 正确修复了字符串版本号比较

## 修复优先级

### P0（上线前必修）
1. C1 权限切面实现（或临时403拦截）
2. C2 文件上传校验（白名单+鉴权+大小）
3. C3 凭证真实加密（AES-GCM/KMS）
4. C4+C5+C6 标准环境实现（用户解析+ID策略+fail-closed）
5. market+open 审批并发锁（乐观锁）
6. open ApprovalEngine 吞异常修复（rethrow或补偿）

### P1（短期）
7. getAppIdentity/getVerifyType 凭证限owner/admin
8. ServiceLogAspect 敏感参数脱敏
9. GlobalExceptionHandler 异常脱敏
10. executions纳入拦截器
11. businessId类型统一(market改Long)

### P2/P3（后续）
- Op*Mapper重命名 / PermissionService拆分 / N+1优化 / Random改雪花 / AI痕迹清理

## 未尽事项（需后续会话补充）

| 项 | 说明 | 状态 |
|----|------|------|
| PermissionService 后1304行 | 1424行仅读前120+grep28方法签名(对称CRUD) | ⚠️ 部分 |
| FlowVersionService(791)/SyncService(599)/ApiService(536)/CallbackService(531)/EventService(515) | grep安全模式扫描(无注入/密钥)，未逐行读业务逻辑 | ⚠️ 待补 |
| sql migration V2/V3 DDL | 未逐行读（索引/约束/字段类型） | ⚠️ 待补 |
| test(24文件) | 覆盖核查未做 | ❌ |
| /remove-ai-slops | 改代码，待授权 | ⚠️ 待定 |
| ~~非java(mapper xml/yml/pom)~~ | 已完成 → non-java-audit.md（无SQL注入；dev yml MySQL密码123456明文；GraalVM沙箱安全） | ✅ |
| ~~小模块 OpDebug/CardSetting~~ | 已逐行读 → batch-2F-misc.md | ✅ |

## 结论

❌ **不通过**。6 个 CRITICAL（权限绕过+文件上传+凭证假加密+生产鉴权失效+fail-open+启动失败）均为上线阻断项，其中 C1/C2 已 curl 实测可利用。代码规范性与局部设计优秀（SQL/隔离/角色矩阵/校验器），但**安全实现大面积缺失**（鉴权/加密/并发），且标准环境（生产）的关键策略未实现。建议按 P0 修复后复审。
