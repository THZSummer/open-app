# SDD 规范目录

## 目录结构

```
.sddu/
├── README.md              # 本文件
├── specs-tree-root/       # 规范树根目录
│   ├── README.md          # 本文件
│   └── specs-tree-[feature]/  # Feature 目录
│       ├── discovery-report.md   # 需求挖掘报告
│       ├── discovery-analysis.md # 需求分析笔记
│       ├── session-log.md        # 会话日志
│       ├── spec.md               # 产品规范
│       ├── plan.md               # 技术规划
│       ├── tasks.md              # 任务分解
│       └── state.json            # 状态文件
```

## Feature 列表

| Feature ID | Feature 名称 | 状态 | 优先级 | 作者 | 更新时间 |
|------------|-------------|------|--------|------|----------|
| **CAPABILITY-001** | [能力开放平台](./specs-tree-capability-open-platform/) | planned | P0 | Summer | 2026-04-20 |
| **DATA-OPEN-001** | [数据开放平台](./specs-tree-data-open-platform/) | specified | P0 | Summer | 2026-04-07 |

## Feature 详情

### CAPABILITY-001: 能力开放平台

**状态**: planned → 技术规划完成，待任务分解

**文档**:
- [需求挖掘报告](./specs-tree-capability-open-platform/discovery-report.md) - 完整的需求挖掘报告（615 行）
- [需求分析笔记](./specs-tree-capability-open-platform/discovery-analysis.md) - 分析总结
- [会话日志](./specs-tree-capability-open-platform/discovery-session-log.md) - 原始对话记录
- [产品规范](./specs-tree-capability-open-platform/spec.md) - 完整的产品规范文档（567 行）⭐ 新增
- [技术规划](./specs-tree-capability-open-platform/plan.md) - 技术规划文档（800+ 行）⭐ 新增
- [ADR-001](./specs-tree-capability-open-platform/ADR-001.md) - 单体应用架构决策 ⭐ 新增
- [ADR-002](./specs-tree-capability-open-platform/ADR-002.md) - Mock 策略决策 ⭐ 新增
- [ADR-003](./specs-tree-capability-open-platform/ADR-003.md) - 权限资源抽象决策 ⭐ 新增

**技术规划摘要**:
- **推荐方案**: 单体应用 + 模块化设计（NestJS + React）
- **预估工作量**: 80 人天（约 4 人月）
- **核心决策**: Mock 策略隔离依赖、权限资源抽象设计
- **开发阶段**: 框架搭建 → 核心模块 → 审批网关 → 联调上线

**下一步**: 运行 `@sddu-tasks capability-open-platform` 开始任务分解

---

### DATA-OPEN-001: 数据开放平台

**状态**: specified → 规范编写完成，待技术规划

**文档**:
- [需求挖掘报告](./specs-tree-data-open-platform/discovery-report.md) - 完整的需求挖掘报告（1101 行）
- [需求分析笔记](./specs-tree-data-open-platform/discovery-analysis.md) - 分析总结
- [会话日志](./specs-tree-data-open-platform/discovery-session-log.md) - 原始对话记录
- [产品规范](./specs-tree-data-open-platform/spec.md) - 完整的产品规范文档（712 行）

**核心内容**:
- **核心定位**: open-app 体系下的子平台，将 XX 通讯平台数据开放给企业内部三方平台
- **目标用户**: 数据 Owner、平台管理员、三方平台业务方
- **Must Have 需求**: 7 项（FR-001 ~ FR-007）
- **Should Have 需求**: 9 项（FR-008 ~ FR-016）
- **Could Have 需求**: 4 项（FR-017 ~ FR-020）
- **核心流程**: 数据注册 → 动态审批 → 数据上架 → 订阅申请 → 消费数据 → 审计监控
- **数据治理**: L1-L5 敏感度分级，基于敏感度的动态审批链

**下一步**: 运行 `@sdd-plan 数据开放平台` 开始技术规划

---

## 使用说明

- 每个 Feature 有独立的目录
- 文档会自动维护（@sdd-docs）
- 使用 `@sdd 开始 [feature 名称]` 开始新 feature
