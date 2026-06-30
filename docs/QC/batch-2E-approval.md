# 批次 2-E：open-server / version + approval + member + permission 审查报告

> 阶段 2 第 5 批。范围：version 13 + approval 13 + member 12 + permission 8 = 46 文件。

## 元信息

| 项 | 值 |
|----|-----|
| 核心文件 | PermissionService(**1424行**)、approval(ApprovalService 832+ApprovalEngine 739)、MemberServiceImpl(293)、VersionServiceImpl(374) |
| 鉴权 | resolveAndValidate 成员校验 + member 角色矩阵 |
| 审查日期 | 2026-06-29 |

## 总评定：⚠️ 有条件通过（1 MAJOR + 遗留安全项）

member 的角色权限矩阵是全项目标杆；permission 鉴权到位但类过大；approval 的 Controller 鉴权缺失已计入批次 2-A 的 CRITICAL。

---

## ✅ member 模块（角色权限标杆）

**MemberServiceImpl** 实现了**角色级**权限矩阵（优于 app 的成员级）：

```
操作人角色     可操作目标角色              越权拒绝
Developer(0)   无（不能操作任何成员）       403
Admin(2)       Developer(0) 仅              403
Owner(1)       Developer(0), Admin(2)       Owner 只能转移
```

- `validateMemberOperationPermission`（L241-258）矩阵清晰 ✅
- transferOwner（L181-203）：校验操作人是 Owner → 事务内新增目标Owner+删原Owner ✅
- addMembers：批量校验重复（L127）+ insertBatch（L148）避免 N+1 ✅
- fillW3Accounts（L93）批量查 employee ✅
- getOperator 取最高角色（L266）✅

> **对比 app 批**：app 的 getAppIdentity（取 sk）任何成员可读，粒度不足。建议 app 凭证接口复用 member 的角色矩阵，限 owner/admin。

## 🟠 MAJOR #1：PermissionService 上帝类（1424 行）

`PermissionService.java` 1424 行，依赖 **14 个 mapper**，把 API/Event/Callback/Category 四类权限管理全塞进一个类。

**问题**：严重违反单一职责，可维护性差，修改任一类权限易误伤其他，测试困难。

**修复**：按资源类型拆分（ApiPermissionService/EventPermissionService/CallbackPermissionService/CategoryPermissionService），抽取公共订阅逻辑。

> 注：permission 鉴权本身到位（`resolveAndValidate` L103），问题在结构而非安全。

## ⚠️ approval 模块（遗留安全 + 双写）

| 项 | 结论 | 说明 |
|---|------|------|
| ApprovalController 鉴权 | 🔴 已计入 2-A CRITICAL | 5 接口用 @PlatformAdminPermission（空校验），审批流模板管理对任意用户开放 |
| ApprovalEngine(739) + ApprovalService(832) | ⚠️ 体量大，未深读 | open 审批引擎（发起/编排节点）远比 market(143行) 复杂 |
| 与 market 双写 | ⚠️ 交阶段 3 | open 发起写 approval_record_t，market 审批也写同表，两服务并发写需核查 |

## 🟡 version 模块（未深读）

VersionServiceImpl（374行）未逐行审查。建议复核版本状态流转（草稿/审核中/已发布）与 app 模块 updateVerifyType 的协同。

## 阻塞问题汇总

| # | 优先级 | 问题 | 备注 |
|---|--------|------|------|
| 1 | **P1** | PermissionService 拆分（1424行上帝类） | 可维护性 |
| 2 | P0(已计) | approval Controller @PlatformAdminPermission 空校验 | 见 batch-2A #1 |
| 3 | P2 | open ApprovalEngine/ApprovalService 深审 + 双写 | 阶段 3 |
| 4 | P3 | version 状态流转复核 | — |

## 结论

⚠️ **有条件通过**。member 模块的角色权限矩阵是全项目最佳实践（建议其他模块凭证/管理接口对齐其粒度）。permission 安全到位但结构臃肿需拆分。approval 的安全问题已归入批次 2-A 的 CRITICAL 统一处理，双写一致性交阶段 3。
