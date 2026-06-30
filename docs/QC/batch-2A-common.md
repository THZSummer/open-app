# 批次 2-A：open-server / common 审查报告

> 阶段 2 第 1 批。范围：`v2/common/` 下 43 文件（重构删 489 行，安全敏感）。

## 元信息

| 项 | 值 |
|----|-----|
| 范围 | common 43 文件（config/enums/exception/file/id/interceptor/model/security/user/util/snapshot/controller/constants/annotation） |
| 重构热点 | AuditLogAspect 删86行（已被 OperateLogV2Aspect 取代）、OperateLogV2Aspect +223/-215、annotation/AuditLog -25 |
| 审查日期 | 2026-06-29 |

## 总裁定：❌ FAIL（2 个 CRITICAL）

发现 **2 个 CRITICAL + 1 个 MAJOR**。其中权限绕过影响 36 个管理接口，是本次 QC 最严重问题。

---

## 🔴 CRITICAL #1：平台管理员权限校验空实现（影响 36 接口）

**位置**：`common/security/PlatformAdminPermissionAspect.java:30-43`

```java
@Before("@annotation(PlatformAdminPermission)")
public void checkPlatformAdminPermission() {
    // TODO: 平台管理员权限校验（后续集成）   ← 未实现
    log.debug("Platform admin permission check passed (currently skipped)");  // ← 默认放行
}
```

**影响面**（`@PlatformAdminPermission` 使用统计）：7 个 Controller、**36 个接口**对任意登录用户开放：

| Controller | 接口数 | 暴露操作 |
|---|---|---|
| CategoryController | 7 | 分类 CRUD |
| CallbackController | 7 | 回调注册管理 |
| EventController | 6 | 事件注册管理 |
| ApiController | 6 | API 注册管理 |
| ApprovalController(open) | 5 | 审批流模板管理 |
| SyncController | 4 | 同步管理 |
| **OpDebugProxyController** | 1 | 🚨 调试代理（若生产启用=灾难） |

**修复**：立即实现切面校验逻辑（校验 UserContextHolder 用户是否在平台管理员清单），或对所有 @PlatformAdminPermission 接口临时加 403 拦截直到实现完成。**OpDebugProxyController 必须在生产禁用**。

> 注：ConnectorController、FlowController 注释表明已改用"基于 X-App-Id 的应用访问控制"，不受此漏洞影响。

## 🔴 CRITICAL #2：文件上传无类型校验 + 接口无鉴权

**位置**：`common/file/service/FileV2Service.java:90-162`、`common/file/controller/FileV2Controller.java:37-43`

| 问题 | 证据 | 风险 |
|------|------|------|
| 无文件类型白名单 | `FileV2Service:115-120` 仅取扩展名不校验；注释自称"图片上传"却接受任意类型 | 可上传 .jsp/.html/.exe；若 uploads 目录被静态映射且可执行 → **RCE/XSS** |
| Controller 无鉴权注解 | `FileV2Controller:37` uploadImage 无 @AuthRole/@PlatformAdminPermission | 匿名/任意用户可上传（依赖是否有全局拦截器兜底，需确认） |
| 文件大小未限制 | `FileV2Service:90` 不校验 file.getSize() | 超大文件耗尽磁盘 DoS |
| fileId 可预测/碰撞 | `FileV2Service:114` `currentTimeMillis + new Random().nextInt(10000)` | 若 fileId 即访问凭证，可被枚举 |

**修复**：① 加扩展名白名单（png/jpg/jpeg/gif/svg+xml）+ ContentType 二次校验（魔数）；② Controller 加鉴权注解；③ 校验文件大小上限；④ fileId 用雪花 ID。

## 🟠 MAJOR #3：全局异常处理器泄露内部信息

**位置**：`common/exception/GlobalExceptionHandlerV2.java:89-90`

```java
String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
return ApiResponse.error("500", "Internal server error: " + detail, ...);  // ← detail 返客户端
```

未知异常的 `e.getMessage()` 可能含 SQL 语句、文件路径、库内部细节 → 信息泄露，辅助攻击者侦察。

**修复**：未知异常只返回通用"系统繁忙"，`detail` 仅写日志。业务异常(BusinessException)的可控 message 可返回。

---

## 重构确认（通过项）

| 项 | 结论 | 证据 |
|---|------|------|
| AuditLogAspect 删除 | ✅ 合理 | commit `366de82e`"删除冗余 AuditLogAspect，OperateLogV2Aspect 已完全取代"，非丢失代码 |
| OperateLogV2Aspect 重构 | ⚠️ 建议 QA 复核 | +223/-215 大改，含 8 个 catch 块（L77 catch Throwable 等，属审计切面容错设计）。静态难穷尽，建议阶段 2 动态 QA 验证审计日志仍正确写入 |

## 其他（低风险，已扫描）

- **enums**（13 文件，+1000 行）：大量枚举常量新增（FlowVersionStatus/ConnectorStatus/ExecutionEnums 等），纯数据，低风险 ✅
- **config/ConnectorPlatformPropertyService**（+234）：配置服务，需确认配置缺失时的兜底行为（AGENTS.md 提到"DB缺失时禁止拒绝服务"）
- **util/JsonUtils**（+190）、**util/CommonUtils**（+60）：工具类，含 1 处 @SuppressWarnings（待看压制内容）
- **model/ApiResponse**（+63/-58）：响应模型重构，需确认字段兼容
- **id/DevIdGeneratorStrategy**（+4/-4）：开发环境 ID 生成器，确认生产不启用

## 阻塞问题汇总

| # | 优先级 | 问题 |
|---|--------|------|
| 1 | **P0** | PlatformAdminPermissionAspect 空校验（36 接口权限绕过） |
| 2 | **P0** | FileV2 文件上传无类型校验+无鉴权（任意文件上传/RCE 风险） |
| 3 | **P1** | GlobalExceptionHandlerV2 异常信息泄露 |
| 4 | **P2** | OpDebugProxyController 需确认生产禁用 |
| 5 | **P2** | OperateLogV2Aspect 重构建议动态 QA 验证 |

## 结论

❌ **不通过**。2 个 CRITICAL（权限绕过 + 任意文件上传）必须立即修复，**严禁以此状态上线**。权限漏洞影响面横跨 event/api/callback/category/approval/sync/debug 共 7 模块 36 接口，是系统性安全风险。common 包的规范性与重构质量（AuditLogAspect 迁移）良好，但安全实现严重缺失。
