# 批次 2-F：open-server / 小模块合并 审查报告

> 阶段 2 第 6 批（最后）。范围：card/flowexecrecord/flowversion/callback/api/sync/auditlog/security/debug/employee/event/lookup 共 54 文件。

## 元信息

| 项 | 值 |
|----|-----|
| 模块分布 | card 10 / flowexecrecord 8 / flowversion 7 / callback 6 / api 5 / sync 4 / auditlog 4 / security 4 / debug 2 / employee 2 / event 1 / lookup 1 |
| 多数文件 | DTO/VO/entity/enum（低风险数据类） |
| 审查日期 | 2026-06-29 |

## 总评定：⚠️ 有条件通过（1 MAJOR + 遗留 CRITICAL）

确认批次 2-C 遗留的 execution-records 拦截问题（配置缺陷，非静默越权）。callback/api/event/category/debug 的鉴权缺失已计入批次 2-A。

---

## 🟠 MAJOR #1：execution-records 未纳入白名单拦截器（配置缺陷）

**问题链**（验证 2-C 待确认项）：

| 层 | 组件 | 对 `/executions` 的行为 |
|---|------|------------------------|
| HTTP 拦截 | AppWhitelistInterceptor | ❌ **未覆盖**（只配了 /connectors/**, /flows/**） |
| Service 切面 | AppDataIsolationAspect | ⚠️ 拦截 flowexecrecord.service.*，但 **fail-open**（无 header→skip） |
| Service 体 | ExecutionRecordService L58/102 | `AppContextHolder.requireInternalAppId()` |

**ExecutionRecordController 路径**：`@RequestMapping("/service/open/v2")` + `@GetMapping("/executions")` → `/service/open/v2/executions`（不在 /flows/** 下）。

**后果**：不带 X-App-Id 请求 /executions → Interceptor 放行 → Aspect skip → service `requireInternalAppId()` 抛 IllegalStateException → **HTTP 500（而非优雅的 403）**。

**严重性**：MAJOR（非 CRITICAL）。**无静默越权**——service 强制 `requireInternalAppId` 兜底，无 AppContext 即异常。但：
1. 错误处理不优雅（500 而非 403，且 IllegalStateException 可能未被 GlobalExceptionHandler 优雅处理）
2. 纵深防御缺失（依赖 service 层兜底，非 HTTP 层 fail-closed）
3. 若未来新增 service 方法忘了调 requireInternalAppId → 真越权

**修复**：`WebMvcConfig:50` 的 `addPathPatterns` 增加 `/service/open/v2/executions/**`。

## 🔴 遗留 CRITICAL（已计入批次 2-A）

本批 callback/api/event/category 模块的 Controller 使用 `@PlatformAdminPermission`（空校验）：
- CallbackController（7 接口）、ApiController（6）、EventController（6）、CategoryController（7）
- debug/OpDebugProxyController（1，调试代理）

→ 均属批次 2-A CRITICAL #1 的影响面（共 36 接口），不重复计入。

## 🟡 其他小模块（低风险，已扫描）

| 模块 | 评价 |
|------|------|
| card(10) | 卡片设置，含 3 处 TODO（hotspots 已记），常规 CRUD |
| flowversion(7) | 流版本管理，与 flow 协同 |
| sync(4) | 同步管理，SyncController 用 @PlatformAdminPermission（计入 2-A） |
| auditlog(4) | 审计日志查询 |
| security(4) | AppWhitelistService/AppContextHolder 等（鉴权基础设施，已在 2-C 审查） |
| employee(2) | 员工查询 |
| event(1)/lookup(1) | 单文件改动 |

## 阻塞问题汇总

| # | 优先级 | 问题 |
|---|--------|------|
| 1 | **P1** | /executions 纳入 AppWhitelistInterceptor（配置补齐） |
| 2 | P0(已计) | callback/api/event/category/debug @PlatformAdminPermission 空校验 | 见 batch-2A |
| 3 | P2 | AppDataIsolationAspect 改 fail-closed（见 batch-2C #1） |

## 结论

⚠️ **有条件通过**。本批以小模块为主，多数低风险。核心可执行项是 /executions 拦截配置补齐（1 行配置）。open-server 阶段 2 六批审查全部完成，批次 2-A 的权限 CRITICAL 是全局最高优先级。
