# 后端代码 QC 最终报告

> open-server + market-server 全量审查汇总。基线 `4650daed`(2026-06-10)→HEAD，313 改动 java 文件，按 review-work 5 维度框架分 8 批 + 跨模块审查。

## 执行摘要

代码库**规范性与局部设计优秀**（SQL 规范、connector 应用隔离、member 角色矩阵、密钥加密），但存在 **2 个 CRITICAL 安全漏洞**（权限绕过 + 任意文件上传）与若干并发/双写隐患。**严禁当前状态上线**。修复 CRITICAL + P0 后可复审放行。

## 总评定：❌ 不通过（2 CRITICAL + 13 MAJOR）

## 批次结果矩阵

| 批次 | 范围 | Verdict | 核心问题 | 报告 |
|------|------|---------|---------|------|
| 1-M | market-server 49文件 | ❌ FAIL | 审批并发无锁/businessId类型/版本号比较 | [batch-1-market.md](./batch-1-market.md) |
| 2-A | common 43文件 | ❌ FAIL | **2 CRITICAL**（权限空校验/文件上传） | [batch-2A-common.md](./batch-2A-common.md) |
| 2-B | app 35文件 | ⚠️ 条件通过 | sk/apiSecret 明文返回 | [batch-2B-app.md](./batch-2B-app.md) |
| 2-C | flow+顶层 | ⚠️ 条件通过 | Aspect fail-open | [batch-2C-flow.md](./batch-2C-flow.md) |
| 2-D | connector+ability | ⚠️ 条件通过 | Op\*Mapper 重构不彻底 | [batch-2D-connector.md](./batch-2D-connector.md) |
| 2-E | version+approval+member+permission | ⚠️ 条件通过 | PermissionService 上帝类 | [batch-2E-approval.md](./batch-2E-approval.md) |
| 2-F | 小模块 54文件 | ⚠️ 条件通过 | executions 拦截配置 | [batch-2F-misc.md](./batch-2F-misc.md) |
| 阶段3 | 跨模块双写 | — | approval/property_t 双写 | [dualwrite-audit.md](./dualwrite-audit.md) |

---

## 🔴 CRITICAL 问题（P0，上线前必修）

### C1. 平台管理员权限校验空实现（36 接口绕过）
- **位置**：`common/security/PlatformAdminPermissionAspect.java:30-43`
- **现象**：`@PlatformAdminPermission` 切面 TODO 未实现，`log.debug("...currently skipped")` 默认放行
- **影响面**：7 个 Controller、**36 个管理接口**对任意登录用户开放（Event/Sync/Approval/Category/Api/Callback/Debug）
- **含 OpDebugProxyController**（调试代理）—— 生产启用即灾难

### C2. 文件上传无类型校验 + 接口无鉴权
- **位置**：`common/file/service/FileV2Service.java:90-162`、`FileV2Controller.java:37`
- **现象**：注释自称"图片上传"却不校验扩展名/ContentType/大小；Controller 无鉴权注解
- **风险**：可上传 .jsp/.html；uploads 若被静态映射且可执行 → **RCE/XSS**

---

## 📋 全部阻塞问题（按优先级）

### P0（紧急，上线前必修）
| # | 问题 | 来源 | 修复 |
|---|------|------|------|
| C1 | PlatformAdminPermission 空校验 | 2-A | 实现切面校验 或 临时 403 拦截 |
| C2 | 文件上传无校验/无鉴权 | 2-A | 扩展名白名单+ContentType+鉴权+大小限制 |
| P0-1 | market 审批并发无锁 | 1-M | 乐观锁（update 带 WHERE status/node） |
| P0-2 | businessId 类型不同构(market String/open Long) | 1-M/阶段3 | market 改 Long |
| P0-3 | MAX(version_code) 字符串比较 bug | 1-M | 改用数值列/id 取最新 |
| P0-4 | property_t 共用冲突风险 | 阶段3 | 核查命名空间隔离 |

### P1（重要）
| # | 问题 | 来源 |
|---|------|------|
| P1-1 | GlobalExceptionHandlerV2 异常信息泄露 | 2-A |
| P1-2 | app getAppIdentity/getVerifyType 明文返回 sk/apiSecret | 2-B |
| P1-3 | AppDataIsolationAspect fail-open（改 fail-closed） | 2-C |
| P1-4 | /executions 未纳入 AppWhitelistInterceptor | 2-F |
| P1-5 | approval 双写并发无协调 | 阶段3 |
| P1-6 | PermissionService 1424行上帝类（拆分） | 2-E |
| P1-7 | Op\*Mapper 重构不彻底（重命名） | 2-D |
| P1-8 | market N+1 查询/整页try-catch/节点越界 | 1-M |
| P1-9 | chatbotbindtab tenantId 设空（多租户） | 1-M |

### P2/P3（次要）
generateAppId/FileV2Service Random 碰撞、pageSize 无上限、硬删除、lookup 缓存同步、AI 痕迹（CRUD 重复）、FlowPublishValidator 动态验证、test 覆盖核查。

---

## ✨ 通过项亮点（值得保持）

| 亮点 | 证据 |
|------|------|
| **SQL 规范优秀** | 全库无 SELECT *、LIKE 全 #{} 参数化、JOIN≤3、无 SQL 注入 |
| **connector 应用隔离标杆** | ConnectorService 代码级 appId 校验 + SQL 级过滤双重 |
| **member 角色权限矩阵** | Owner/Admin/Developer 细粒度，transferOwner 事务完整 |
| **flow 重构干净** | Op\*→Flow\* 迁移 0 残留，commit 说明清晰 |
| **密钥加密存储** | encryptApiSecret + API_SECRET_PATTERN 格式校验 |
| **批量查询优化** | getAppList owner 批量、ability loadPropsMap、member fillW3Accounts |
| **X-App-Id 双重鉴权**（flow/connector） | Interceptor fail-closed + Aspect 注入 |
| **审计日志** | 写操作普遍 @AuditLog |

---

## 🗺️ 修复路线图

### 阶段一：上线阻断修复（立即，1-2 天）
1. **C1 权限**：实现 PlatformAdminPermissionAspect 真实校验（或临时对所有 @PlatformAdminPermission 接口加 403）
2. **C2 文件上传**：扩展名白名单 + ContentType 魔数校验 + FileV2Controller 加鉴权 + 大小限制
3. **OpDebugProxyController**：确认生产禁用
4. **P0-1 审批并发**：ApprovalEngine 加乐观锁
5. **P0-2 businessId**：market entity 改 Long
6. **P0-3 版本号比较**：selectPublishedList 改数值比较

### 阶段二：重要修复（短期，3-5 天）
7. P1-1 异常信息脱敏
8. P1-2 凭证接口限 owner/admin（复用 member 角色矩阵）
9. P1-3 Aspect fail-closed
10. P1-4 executions 拦截配置（1 行）
11. P1-5/P0-4 双写协调 + property_t 隔离核查

### 阶段三：重构与清理（后续）
12. P1-6 PermissionService 拆分
13. P1-7 Op\*Mapper 重命名
14. P1-8/P1-9 market 健壮性
15. P2/P3 + `/remove-ai-slops` 清理

---

## ⚠️ 未尽事项（建议补充）

| 项 | 说明 |
|----|------|
| FlowPublishValidator 动态 QA | 421 行发布校验逻辑，静态难穷尽，建议补发布流程动态测试 |
| ConnectorVersionService 复核 | 546 行版本状态流转 |
| open ApprovalEngine 深审 | 739 行（与 market 143 行双写） |
| test 覆盖核查 | test 目录 +8297/-2862，确认覆盖未下降 |
| QA 实际运行 | 本次为静态审查，建议起服务按 AGENTS.md(WMI) 跑关键接口 |

## 结论

❌ **不通过，需修复后复审**。2 个 CRITICAL 是系统性安全风险（权限+文件上传），必须立即修复。代码整体工程质量良好（规范、隔离、角色矩阵），问题集中在**安全实现缺失**与**并发/双写协调**，而非规范或架构缺陷。建议按路线图阶段一修复后重新审查 CRITICAL 项。
