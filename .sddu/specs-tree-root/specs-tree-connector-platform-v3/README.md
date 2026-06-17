# 目录：连接器平台 V3

## 目录简介

**连接器平台 V3** 从 V2（CONN-PLAT-002，已终止）复制创建，作为 V3 的新起点。基于 V2 已有的规范和技术规划成果，重新启动需求挖掘与开发流程。

**当前状态**: 📋 registered（已注册，待启动需求挖掘）  
**Feature ID**: CONN-PLAT-003  
**优先级**: P1  
**创建时间**: 2026-06-17  
**最新更新**: 2026-06-17（从 V2 复制创建）  
**依赖**: CONN-PLAT-002（V2 — 已终止，工作转入本 V3）  
**规范版本**: v2.15-draft（继承自 V2）

### 工作流进度

| 阶段 | 状态 | 完成时间 |
|------|------|----------|
| 📋 需求挖掘 (discovery) | ⏳ 待开始 | — |
| 📋 规范编写 (spec) | ⏳ 待重写 | 继承自 V2（v2.15-draft） |
| 📐 技术规划 (plan) | ⏳ 待开始 | — |
| 📝 任务分解 (tasks) | ⏳ 待开始 | — |

## 目录结构

```
specs-tree-connector-platform-v3/
├── README.md               # 本文件 - 目录导航
├── spec.md                 # 产品规范 v2.15-draft（继承自 V2）
├── plan.md                 # 技术规划 v1.0（继承自 V2）
├── plan-json-schema.md     # JSON Schema 设计规范（继承自 V2）
├── state.json              # 状态文件
├── ADR-004.md              # 架构决策（继承自 V2）
├── ADR-005.md              # 架构决策（继承自 V2）
├── ADR-006.md              # 架构决策（继承自 V2）
├── ADR-007.md              # 架构决策（继承自 V2）
├── ADR-008.md              # 架构决策（继承自 V2）
├── plan-api.md             # API 设计规划（继承自 V2）
├── plan-cache.md           # 缓存设计规划（继承自 V2）
├── plan-code.md            # 代码设计规划（继承自 V2）
├── plan-db.md              # DB 设计规划（继承自 V2）
├── plan-page.md            # 页面设计规划（继承自 V2）
├── plan-runtime.md         # 运行时设计规划（继承自 V2）
├── json-schema-gap-analysis.md  # JSON Schema 差距分析（继承自 V2）
└── 需求设计说明书-connector-platform-v2.md  # 原 V2 需求设计说明书
```

## 文件说明

### 📂 规范阶段 (spec)

| 文件 | 说明 | 状态 |
|------|------|------|
| spec.md | **产品规范 v2.15-draft** — 继承自 V2，待根据 V3 需求重新编写 | ⏳ 待重写 |
| state.json | 状态文件 — 当前状态 registered | 📋 registered |

### 📂 技术规划阶段 (plan)

| 文件 | 说明 | 状态 |
|------|------|------|
| plan.md | **技术规划** — 继承自 V2，待根据 V3 需求重新规划 | ⏳ 待重写 |
| plan-api.md | **API 设计** — 继承自 V2 | ⏳ 待审查 |
| plan-cache.md | **缓存设计** — 继承自 V2 | ⏳ 待审查 |
| plan-code.md | **代码设计** — 继承自 V2 | ⏳ 待审查 |
| plan-db.md | **DB 设计** — 继承自 V2 | ⏳ 待审查 |
| plan-page.md | **页面设计** — 继承自 V2 | ⏳ 待审查 |
| plan-runtime.md | **运行时设计** — 继承自 V2 | ⏳ 待审查 |
| plan-json-schema.md | **JSON Schema 设计规范** — 继承自 V2 | ⏳ 待审查 |

### 📂 架构决策记录 (ADR)

| 文件 | 说明 | 状态 |
|------|------|------|
| ADR-004.md | 版本完整快照存储与递增整数版本号 | ⏳ 待审查 |
| ADR-005.md | Redis 令牌桶入站限流方案 | ⏳ 待审查 |
| ADR-006.md | MySQL 主存储运行记录与日志 | ⏳ 待审查 |
| ADR-007.md | 多版本模型下的引用稽核策略 | ⏳ 待审查 |
| ADR-008.md | 继承自 V2 | ⏳ 待审查 |

## 上级目录

- [返回上级](../README.md) - SDDU 规范目录
- [返回首页](../../README.md) - SDDU 工作空间
- [V2（已终止）](../specs-tree-connector-platform-v2/README.md)

## 下一步

1. 运行 `@sddu discovery connector-platform-v3` 重新启动需求挖掘
2. 根据新需求重新编写 spec.md
3. 审查并更新所有继承自 V2 的规划文档

---

*最后更新: 2026-06-17 | @sddu-docs 自动生成*
