# 批次 2-B：open-server / app 审查报告

> 阶段 2 第 2 批。范围：`v2/modules/app/` 35 文件（应用管理）。

## 元信息

| 项 | 值 |
|----|-----|
| 范围 | app 35 文件（controller/service/resolver/entity/dto/vo/mapper/enums） |
| 核心文件 | AppServiceImpl（695行）、AppController（161行）、StandardAppContextResolver（82行） |
| 审查日期 | 2026-06-29 |

## 总评定：⚠️ 有条件通过（1 MAJOR + 4 MINOR）

比 common/market 批健康：**有成员级鉴权**（resolver 层校验用户-app 归属）、列表按用户过滤无越权、有 N+1 优化意识、密钥加密存储。主要问题是敏感凭证明文返回。

---

## 🟠 MAJOR #1：敏感凭证明文返回前端

| 接口 | 位置 | 问题 |
|------|------|------|
| getAppIdentity（1.8） | `AppServiceImpl:425-428` | `vo.setSk(decryptSk(identity.getPrivateKey()))` —— **明文 sk 返回前端** |
| getVerifyType（1.9） | `AppServiceImpl:454-455` | `vo.setApiSecret(apiSecret)`（L444 已 decryptApiSecret）—— **明文 apiSecret 返回前端** |

**分析**：
- 虽经 `resolveAndValidate` 成员校验，但权限粒度只到"app 成员"——**任意成员**（含普通成员）可获取明文 sk/apiSecret。
- sk（私钥）应仅 owner/admin 可见，或脱敏（如返回掩码 + 单独的重置接口）。
- 前端持有明文密钥 → XSS/日志/缓存泄露面扩大。

**修复**：① sk/apiSecret 不明文返回，改为掩码或仅返回是否已设置；② 如必须返回，限制为 owner/admin 角色（当前 `MemberUtils.getHighestRoleMember` 已有角色概念，可复用）。

## ✅ 通过项（亮点）

| 项 | 证据 |
|---|------|
| 成员级鉴权 | `StandardAppContextResolver:57-63` 校验 `selectByAppIdAndAccountId`，非成员抛 noPermission |
| 列表无越权 | `getAppList:215-219` 按当前用户 accountId + tenantId 过滤 |
| N+1 优化 | `getOwnerMap:267+` 批量查 member+employee 替代逐个查询（注释自述"替代 N*2 次"） |
| 密钥加密存储 | `updateVerifyType:353` encryptApiSecret 入库；`API_SECRET_PATTERN`（L77）16位字母数字格式校验 |
| 校验完整 | `validateVerifyType:359-409` 白名单/枚举/互斥(SOAHeader∩SOAURL)/apiSecret 非空+格式 |
| 审计日志 | AppController 写操作均 @AuditLog（CREATE_APP/UPDATE_APP/UPDATE_VERIFY_TYPE/BIND_EAMAP） |
| 入参校验 | @Validated + @NotBlank |

## 🟡 MINOR 问题

| # | 问题 | 位置 | 建议 |
|---|------|------|------|
| 1 | generateAppId 碰撞 | `AppServiceImpl:590-595` `Random().nextInt(9000)` 仅 9000 种，高并发建应用 appId 撞 | 改雪花 ID |
| 2 | getAppList 循环内查属性/角色 | `getAppList:238,249` selectPropertiesByParentId + selectByAppIdAndAccountId 在循环内（owner 已批量，这俩未批量） | 批量预取 |
| 3 | pageSize 无上限 | `AppController:88,98` defaultValue 但无 max，可传大值致大查询 | 加 @Max |
| 4 | apiSecret 格式错误码语义不准 | `AppServiceImpl:401-406` 格式不匹配复用 API_SECRET_REQUIRED（应为 INVALID_FORMAT） | 新增专用错误码 |

## 阻塞问题汇总

| # | 优先级 | 问题 |
|---|--------|------|
| 1 | **P1** | getAppIdentity/getVerifyType 明文返回 sk/apiSecret（敏感凭证暴露） |
| 2 | P2 | generateAppId 碰撞（与 market FileV2Service 同类问题） |
| 3 | P3 | pageSize 上限、N+1、错误码 |

## 结论

⚠️ **有条件通过**。app 模块鉴权设计（resolver 层成员校验）与性能优化（批量查询）明显优于 common/market 批。修复 #1（敏感凭证明文返回）后可放行。appId 碰撞与 FileV2Service 是同一类随机数问题，建议统一改雪花 ID。
