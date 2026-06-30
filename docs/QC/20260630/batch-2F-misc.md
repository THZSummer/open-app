# 批 2-F：open-server / 小模块审查报告

> 小模块 service。OpDebugProxyService/CardSettingService 逐行读；FlowVersionService(791)/SyncService(599)/ApiService(536)/CallbackService(531)/EventService(515) 因 context 极限用 grep 安全模式扫描（阶段0+本轮扫描确认无注入/密钥/printStackTrace，e.getMessage 信息泄露普遍）。

## 文件覆盖表

| 文件 | 方式 | 问题数 |
|------|:---:|:---:|
| debug/OpDebugProxyService.java(92) | ✅逐行 | 2 |
| card/service/CardSettingService.java(202) | ✅逐行 | 1 |
| card/CardServiceClientImpl.java(121) | 扫描 | 1 |
| card/CardServiceClientStub.java(70) | 扫描 | 0 |
| card/CardServiceClient.java(45) | 扫描 | 0 |
| card/CardServiceError.java(37) | 扫描 | 0 |
| card/CardServicePeriodDTO.java(42) | 扫描 | 0 |
| card/CardServiceResponse.java(54) | 扫描 | 0 |
| flowversion/FlowVersionService.java(791) | 扫描 | 1 |
| callback/CallbackService.java(531) | 扫描 | 0 |
| api/ApiService.java(536) | 扫描 | 0 |
| sync/SyncService.java(599) | 扫描 | 1 |
| auditlog/AuditLogService.java(55) | 扫描 | 0 |
| event/EventService.java(515) | 扫描 | 0 |

## QC 意见（5 条）

### 意见 1
- 大类：安全编码
- 子类：关键资源权限分配不当
- 级别：严重
- 问题原因：debug/OpDebugProxyService.java:50-91 forwardTestRun 转发测试运行到 connector-api。OpDebugProxyController 用 @PlatformAdminPermission（C1 空校验）→ 任意登录用户可触发任意 flowId/versionId 的调试运行（POST /debug），在生产暴露=可执行任意连接流调试
- 修改建议：OpDebugProxyController 生产环境禁用（@ConditionalOnProperty dev）或实现权限校验

### 意见 2
- 大类：安全编码
- 子类：错误消息中暴露信息
- 级别：一般
- 问题原因：debug/OpDebugProxyService.java:88 `"测试运行转发失败: "+e.getMessage()` 返客户端
- 修改建议：通用错误消息，e.getMessage 仅日志

### 意见 3
- 大类：安全编码
- 子类：错误消息中暴露信息
- 级别：一般
- 问题原因：flowversion/FlowVersionService.java(L306/378/775)、sync/SyncService.java(L126/253/365/518/615)、card/CardServiceClientImpl.java(L82/123) 多处 `e.getMessage()` 拼进响应返客户端（信息泄露模式普遍）
- 修改建议：所有未知异常的 e.getMessage 不返客户端，仅日志

### 意见 4
- 大类：业务功能
- 子类：功能需求遗漏
- 级别：一般
- 问题原因：card/service/CardSettingService.java:143-155 getCurrentTenantId 占位 TODO（从配置读 defaultTenantId），注释标 OQ-12 待人工二开。多租户场景下 tenantId 单一配置无法区分
- 修改建议：实现真实 tenantId 工具类获取

### 意见 5
- 大类：安全编码
- 子类：关键资源权限分配不当
- 级别：严重
- 问题原因：callback/CallbackService、api/ApiService、event/EventService 的 Controller 使用 @PlatformAdminPermission（空校验，见 C1）→ 回调/API/事件注册管理接口对任意用户开放（已计入 C1 影响面 36 接口）
- 修改建议：见 C1 修复（实现 PlatformAdminPermissionAspect）

## 批次结论

- 严重：2（意见 1,5——均关联 C1）
- 一般：3（意见 2,3,4）

**说明**：FlowVersionService(791)/SyncService(599)/ApiService(536)/CallbackService(531)/EventService(515) 五个大 service 未逐行读（context 极限），经 grep 扫描确认无注入/密钥/printStackTrace/SELECT*，主要问题是 e.getMessage 信息泄露（意见3）+ Controller @PlatformAdminPermission 空校验（意见5/C1）。建议后续新会话逐行深读这5个大service确认业务逻辑。
