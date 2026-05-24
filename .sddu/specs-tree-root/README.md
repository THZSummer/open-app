# SDDU 规范目录

## 目录结构

```
.sddu/
├── README.md              # 本文件
├── specs-tree-root/       # 规范树根目录
│   ├── README.md          # 本文件
│   └── specs-tree-[feature]/  # Feature 目录
│       ├── README.md            # 目录导航
│       ├── discovery-report.md  # 需求挖掘报告
│       ├── discovery-analysis.md# 需求分析笔记
│       ├── discovery-session-log.md # 会话日志
│       ├── spec.md              # 产品规范
│       ├── spec.json            # 规范元数据
│       ├── plan.md              # 技术规划
│       ├── tasks.md             # 任务分解
│       ├── tasks.json           # 任务数据
│       ├── state.json           # 状态文件
│       ├── ADR-*.md             # 架构决策记录
│       └── build-*.md           # 实现报告
```

## Feature 列表

| Feature ID | Feature 名称 | 状态 | 优先级 | 作者 | 更新时间 |
|------------|-------------|------|--------|------|----------|
| **CAP-OPEN-001** | [能力开放平台](./specs-tree-capability-open-platform/) | ✅ validated（全流程完成） | P0 | Summer | 2026-04-22 |
| **CONN-PLAT-001** | [连接器平台](./specs-tree-connector-platform/) | 🔵 tasked（任务分解完成） | P1 | - | 2026-05-22 |
| **DATA-OPEN-001** | [数据开放平台](./specs-tree-data-open-platform/) | ✅ specified（规范完成） | P0 | Summer | 2026-04-07 |

---

## Feature 详情

### CAP-OPEN-001: 能力开放平台

**状态**: ✅ validated（全流程完成 — 需求挖掘 → 规范 → 规划 → 任务分解 → 实现 → 审查 → 验证）  

**文档**:
- [需求挖掘报告](./specs-tree-capability-open-platform/discovery-report.md) - 需求挖掘报告（615 行）
- [需求分析笔记](./specs-tree-capability-open-platform/discovery-analysis.md) - 分析总结
- [会话日志](./specs-tree-capability-open-platform/discovery-session-log.md) - 原始对话记录
- [产品规范](./specs-tree-capability-open-platform/spec.md) - 产品规范（567 行）
- [技术规划](./specs-tree-capability-open-platform/plan.md) - 技术规划（800+ 行）
- [接口设计](./specs-tree-capability-open-platform/plan-api.md) - 58 个接口详细设计
- [数据库设计](./specs-tree-capability-open-platform/plan-db.md) - 15 张表 DDL
- [任务分解](./specs-tree-capability-open-platform/tasks.md) - 13 个任务、5 个波次、103 人天
- [ADR-001~003](./specs-tree-capability-open-platform/ADR-001.md) - 架构决策记录（3 份）
- [实现报告](./specs-tree-capability-open-platform/build.md) - TASK-012 实现完成报告
- [代码审查](./specs-tree-capability-open-platform/review.md) - 代码审查报告（4.5/5）
- [验证报告](./specs-tree-capability-open-platform/validation-report.md) - 规范验证报告（100% FR 覆盖）

**核心指标**:
- 13 个任务全部完成，25 项测试全部通过
- 代码覆盖率 67%，API 覆盖率 100%
- 性能 P99 7ms（目标 <50ms）

---

### CONN-PLAT-001: 连接器平台

**状态**: 🔵 tasked（任务分解完成 — 12 个任务、4 个波次）  

**文档**:
- [需求挖掘报告](./specs-tree-connector-platform/discovery-report.md) - 连接器平台需求分析
- [需求分析笔记](./specs-tree-connector-platform/discovery-analysis.md) - 用户场景分析
- [产品规范 v4.0](./specs-tree-connector-platform/spec.md) - 5 个用户故事、25 项 FR
- [技术规划 v2.0](./specs-tree-connector-platform/plan.md) - 同步执行引擎架构
- [接口设计 v2.0](./specs-tree-connector-platform/plan-api.md) - 26 个端点
- [数据库设计 v2.0](./specs-tree-connector-platform/plan-db.md) - 9 张表
- [任务分解](./specs-tree-connector-platform/tasks.md) - 12 个任务（10 个实施 + 2 个外部占位）
- [ADR-001~003](./specs-tree-connector-platform/ADR-001.md) - 架构决策记录（3 份）

**核心信息**:
- 12 个任务：10 个当前实施任务 + 2 个外部占位任务（TASK-009/TASK-010 为 wecodesite 前端任务）
- 4 个执行波次：基础设施 → 后端核心 → 运行时+调试 → 增强与集成
- 执行模型：同步执行引擎（基于 open-server 扩展）
- 预估工期：8-12 周

---

### DATA-OPEN-001: 数据开放平台

**状态**: ✅ specified（规范编写完成，待技术规划）  

**文档**:
- [需求挖掘报告 v1.18](./specs-tree-data-open-platform/discovery-report.md) - 需求挖掘报告（1101 行）
- [需求分析笔记](./specs-tree-data-open-platform/discovery-analysis.md) - 分析总结
- [会话日志](./specs-tree-data-open-platform/discovery-session-log.md) - 原始对话记录
- [产品规范 v2.0](./specs-tree-data-open-platform/spec.md) - 产品规范（712 行）

**核心内容**:
- **Must Have 需求**: 7 项（FR-001 ~ FR-007）
- **Should Have 需求**: 9 项（FR-008 ~ FR-016）
- **Could Have 需求**: 4 项（FR-017 ~ FR-020）
- **核心流程**: 数据注册 → 动态审批 → 数据上架 → 订阅申请 → 消费数据 → 审计监控
- **数据治理**: L1-L5 敏感度分级，基于敏感度的动态审批链

**下一步**: 运行 `@sddu-plan 数据开放平台` 开始技术规划

---

## 使用说明

- 每个 Feature 有独立的目录
- 文档会自动维护（`@sddu-docs`）
- 使用 `@sddu 开始 [feature 名称]` 开始新 feature
