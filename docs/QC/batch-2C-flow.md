# 批次 2-C：open-server / flow + 顶层 审查报告

> 阶段 2 第 3 批。范围：flow 模块 20 文件 + open 顶层（实为 test 目录重构）。

## 元信息

| 项 | 值 |
|----|-----|
| 范围 | flow 20 文件（Op\*→Flow\* 重写式重构）+ test 目录 109 文件（+8297/-2862 测试重构） |
| 重构性质 | 删 OpFlowService(333)/OpFlowController(165)/FlowVersion(90)/OpFlowMapper 等；新增 FlowService(392)/FlowPublishValidator(421)/FlowDeployService(109)/FlowCopyService(135) |
| 重构 commit | "统一应用隔离+扁平化包结构"、"连接流appId对齐切面注入" |
| 审查日期 | 2026-06-29 |

## 总评定：⚠️ 有条件通过（1 MAJOR + 3 MINOR）

重构干净（Op\* 类 0 残留引用）、鉴权双重（Interceptor fail-closed + Aspect 注入）。主要问题是 Aspect fail-open 与发布校验器需动态验证。

---

## ✅ 重构正确性（通过）

| 检查 | 结论 | 证据 |
|------|------|------|
| Op\* 类是否完全删除 | ✅ 是 | `OpFlowService`/`OpFlowController` 全库引用 = **0**，无残留 |
| 重构有 commit 说明 | ✅ 是 | "统一应用隔离+扁平化包结构"、"version/execution entity/mapper/DTO 到独立包" |
| 功能对应 | ✅ 新 FlowService(392) 覆盖旧 OpFlowService(333) 范围 | 新增部署/复制/生命周期功能（见 FlowController 注释 V3 变更） |

## ✅ 鉴权设计（双重，优于 common/app）

flow 用 **X-App-Id 双重机制**（非 @PlatformAdminPermission）：

| 层 | 组件 | 行为 | 评价 |
|---|------|------|------|
| HTTP 拦截 | `AppWhitelistInterceptor`（/connectors/**, /flows/**） | 无 header→403、非白名单→403 | ✅ **fail-closed** |
| Service 切面 | `AppDataIsolationAspect` | 注入 AppContext + resolveAndValidate 成员校验 | ⚠️ 见 MAJOR #1 |

> 对比：common 的 @PlatformAdminPermission 是空校验（CRITICAL），flow 的 X-App-Id 是真实校验。flow 鉴权设计明显更成熟。

## 🟠 MAJOR #1：AppDataIsolationAspect fail-open（纵深防御不足）

**位置**：`modules/security/AppDataIsolationAspect.java:77-81`

```java
String appIdHeader = request.getHeader(HEADER_APP_ID);
if (appIdHeader == null || appIdHeader.trim().isEmpty()) {
    log.debug("No X-App-Id header, skipping isolation check...");  // ← 放行
    return joinPoint.proceed();
}
```

**分析**：Aspect 在 header 缺失时 **skip（放行）** 而非拒绝。当前安全靠 AppWhitelistInterceptor（前置）兜底拦截无 header 请求，正常 HTTP 流程下不会触发 skip。但：
- 若未来新增 flow 接口路径不在 Interceptor 覆盖范围（如 `/execution-records`，见待确认），Aspect 单独生效时 skip → **越权**
- 内部 service 间调用/定时任务无 HTTP 上下文 → skip → 无隔离

**修复**：Aspect 改为 **fail-closed**（无有效 AppContext → 抛异常拒绝），与 Interceptor 形成纵深防御，而非依赖单一前置。

## 🟠 待确认 #2：拦截路径覆盖 execution-records？

`AppWhitelistInterceptor` 只注册 `/connectors/**`+`/flows/**`（WebMvcConfig:50）。但 `ExecutionRecordController` 注释"按 appId 过滤（X-App-Id）"——若其路径非 /flows/** 下，则**仅靠 fail-open 的 Aspect** 隔离 → 越权风险。**交批次 2-F（flowexecrecord）确认**。

## 🟡 MINOR

| # | 问题 | 位置 | 说明 |
|---|------|------|------|
| 1 | FlowCopyService Random | `FlowCopyService.java:139` | 复制流用 new Random()（与 generateAppId/FileV2Service 同类），碰撞风险低（复制非高频） |
| 2 | FlowPublishValidator 未深审 | `FlowPublishValidator.java`(421行) | 发布校验器（DAG无环/节点完整性/必填项）逻辑复杂，静态难穷尽，**建议阶段动态 QA 验证发布流程** |
| 3 | test 大量重构 | test 目录 +8297/-2862 | 测试代码增删量大，需确认**测试覆盖是否下降**（删的测试是否有关键场景） |

## 阻塞问题汇总

| # | 优先级 | 问题 |
|---|--------|------|
| 1 | **P1** | AppDataIsolationAspect 改 fail-closed（纵深防御） |
| 2 | P1 | 确认 execution-records 拦截覆盖（交 2-F） |
| 3 | P2 | FlowPublishValidator 动态 QA 验证 |
| 4 | P3 | test 覆盖率核查、FlowCopyService Random |

## 结论

⚠️ **有条件通过**。flow 模块的重构质量（Op→Flow 干净迁移）与鉴权设计（X-App-Id 双重）是已审模块中最成熟的。修复 Aspect fail-closed + 确认 execution-records 覆盖后可放行。FlowPublishValidator 因复杂度高建议补动态测试。
