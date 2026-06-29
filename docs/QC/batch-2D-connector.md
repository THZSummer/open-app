# 批次 2-D：open-server / connector + ability + connectorversion 审查报告

> 阶段 2 第 4 批。范围：connector 18 + ability 15 + connectorversion 9 = 42 文件（删 547）。

## 元信息

| 项 | 值 |
|----|-----|
| 范围 | connector（ConnectorService 329行/ConnectorController 114）、ability（AbilityServiceImpl 238）、connectorversion（ConnectorVersionService 546行，最大） |
| 鉴权机制 | X-App-Id 双重（同 flow：AppWhitelistInterceptor + AppDataIsolationAspect） |
| 审查日期 | 2026-06-29 |

## 总评定：⚠️ 有条件通过（1 MAJOR + 4 MINOR）

应用隔离实现是已审模块中最强的（ConnectorService 代码级 appId 校验 + SQL 级过滤双重）。主要问题是 Op\*Mapper 重构不彻底与 ConnectorVersionService 待复核。

---

## ✅ 应用隔离（亮点，优于其他模块）

**ConnectorService**（329行）每个方法双重校验：

```java
Long internalAppId = AppContextHolder.requireInternalAppId();  // 从 Aspect 注入
Connector connector = connectorMapper.selectById(connectorId);
if (connector == null || !internalAppId.equals(connector.getAppId())) {
    return ApiResponse.error("404", ...);  // ← 代码级 appId 归属校验
}
```

- 创建：`connector.setAppId(internalAppId)`（L79）绑定应用
- 查询/更新/失效/恢复/删除：**全部**校验 `internalAppId.equals(connector.getAppId())`（L177/190/225/258/295）
- 列表：SQL 层 `appId` 过滤下推（L115-119，注释"全部下推到SQL"）
- 状态机完整：invalidate 校验 `isValidTransition`（L230）、recover 校验状态（L263）、delete 要求 INVALIDATED（L300）、invalidate 前校验 flow 引用（L234-239）

**对比**：common 的 @PlatformAdminPermission 是空校验（CRITICAL），connector 的 appId 校验是真实有效的数据库级隔离。

## ✅ ability 模块

- 鉴权：`AbilityServiceImpl` 每方法 `appContextResolver.resolveAndValidate(appId)` 成员校验（L71/118/176）✅
- `loadPropsMap`（L228）批量查属性 `selectByParentIds` → **避免 N+1** ✅
- addAbility：abilityType 合法性 + 已订阅校验 ✅

## 🟠 MAJOR #1：Op\*Mapper 重构不彻底（命名不一致）

`OpConnectorMapper`/`OpConnectorVersionMapper` 仍被 **6 个文件活跃引用**：
- ConnectorService、ConnectorVersionService、FlowPublishValidator、ConnectorSnapshotLoader

**对比**：flow 模块的 Op\*类（OpFlowService 等）已**完全删除**（0 残留），connector 的 Op\*Mapper 却保留。这是**重构半完成**——同一次"统一应用隔离+扁平化"重构，flow 改完了，connector 的 mapper 没改名。

**影响**：命名混乱（Op 前缀含义不明，疑似旧平台残留），可维护性差。

**修复**：统一重命名 OpConnectorMapper→ConnectorMapper 等，与 flow 风格对齐。

## 🟡 MINOR

| # | 问题 | 位置 | 说明 |
|---|------|------|------|
| 1 | ConnectorVersionService 未深审 | `ConnectorVersionService.java`(546行) | 版本管理（草稿/发布状态流转、版本号生成）逻辑复杂，建议复核 + 动态 QA |
| 2 | ability autoSubscribe TODO 空实现 | `AbilityServiceImpl:170-172` | 订阅能力后应自动订阅 API/事件权限，当前空实现 → 业务可能不完整（已知 TODO） |
| 3 | abilityId 兜底 hacky | `AbilityServiceImpl:143` | `ability!=null ? ability.getId() : (long)abilityType` 主表缺失时用 type 当 ID，潜在数据不一致 |
| 4 | connector 列表 N+1 | `ConnectorService:137` | getConnectorList 循环内 selectListByConnectorId 查版本（appId 已下推，版本查询未批量） |
| 5 | 硬删除 | `ConnectorService:306-307` | deleteConnector 硬删版本+连接器，审计风险 |

## 阻塞问题汇总

| # | 优先级 | 问题 |
|---|--------|------|
| 1 | **P2** | Op\*Mapper 重命名统一（重构收尾） |
| 2 | P2 | ConnectorVersionService 复核版本状态流转 |
| 3 | P3 | autoSubscribe 实现 / N+1 / 硬删除 |

## 结论

⚠️ **有条件通过**。connector 的应用数据隔离（代码级 + SQL 级双重 appId 校验）是已审 6 模块中最严谨的，状态机完整。问题集中在重构收尾（Op\*Mapper 命名）与 ConnectorVersionService 的版本管理逻辑（建议动态验证）。无安全 CRITICAL。
