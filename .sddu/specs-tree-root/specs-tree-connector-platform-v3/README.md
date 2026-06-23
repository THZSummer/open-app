# 目录：连接器平台 V3 — 多版本与增强

## 目录简介

**连接器平台 V3**（CONN-PLAT-002 / CONN-PLAT-003）从 V2 复制创建，作为 V3 新起点。V3 设计方向从「完整好用」转向「**简单可用，能兜底**」：精简非刚需功能（数据处理节点、错误处理节点），大幅放宽前置校验（仅保留 JSON 语法），以 GraalJS 脚本节点作为复杂场景兜底方案。

**当前阶段**: 📋 registered（已注册）  
**生命周期**: tracked（跟踪中）  
**Feature ID**: CONN-PLAT-003（state.json）/ CONN-PLAT-002（spec 体系）  
**优先级**: P1  
**创建时间**: 2026-06-17  
**最新更新**: 2026-06-22  
**依赖**: CONN-PLAT-001（V1 MVP — 已建成并验证）、CONN-PLAT-002（V2 — 已终止）  
**规范版本**: v3.0（47 个功能需求）

### 工作流进度

```
registered → discovered → specified → planned → tasked → builded → reviewed → validated
    ●           ○           ○           ○         ○         ○         ○          ○
    ↑ 当前阶段
```

| 阶段 | 状态 | 说明 |
|------|------|------|
| 📋 需求挖掘 (discovery) | ⏳ 待开始 | 需求设计说明书已完成（v3.0） |
| 📋 规范编写 (spec) | ✅ 已完成 | spec.md v3.0，47 FRs，2026-06-22 |
| 📐 技术规划 (plan) | ✅ 已完成 | plan.md v3.0 + 9 份子规划文档 |
| 📝 任务分解 (tasks) | ✅ 已完成 | 14 个任务，4 个波次，2026-06-22 |
| 🔨 构建实现 (build) | ⏳ 待开始 | — |
| 🔍 代码审查 (review) | ⏳ 待开始 | — |
| ✅ 验证 (validate) | ⏳ 待开始 | — |

## 目录结构

```
specs-tree-connector-platform-v3/
├── README.md                              # 本文件 - 目录导航
├── state.json                             # 状态文件
├── spec.md                                # 产品规范 v3.0（47 FRs）
├── plan.md                                # 技术规划 v3.0
├── plan-db.md                             # 数据库设计 v2.0
├── plan-api.md                            # API 接口设计 v7.0
├── plan-runtime.md                        # 运行时引擎设计 v1.0
├── plan-script.md                         # 脚本执行引擎设计 v8.1-draft（GraalJS）
├── plan-json-schema.md                    # JSON Schema 设计规范 v9.10
├── plan-code.md                           # 代码规范（沿用 V1）
├── plan-cache.md                          # 缓存设计方案 v4.0
├── plan-flow-invoke-temp.md               # 临时方案：#54 调用连接流返回格式
├── tasks.md                               # 任务分解（14 任务，4 波次）
├── tasks.json                             # 任务数据（JSON 格式）
├── 需求设计说明书-connector-platform-v3.md  # 需求设计说明书 v3.0
├── json-schema-gap-analysis.md            # JSON Schema 缺口分析（已完成）
├── ADR-004.md                             # 版本完整快照存储与递增整数版本号
├── ADR-005.md                             # Redis 令牌桶入站限流方案
├── ADR-006.md                             # MySQL 主存储运行记录与日志
├── ADR-007.md                             # 多版本模型下的引用稽核策略
└── ADR-008.md                             # #54 调用连接流 — 透明穿透响应模式
```

## 文件说明

### 📂 规范阶段 (spec)

| 文件 | 说明 | 状态 |
|------|------|------|
| spec.md | **产品规范 v3.0** — 47 个功能需求，V3 设计方向：简单可用，能兜底 | ✅ 已完成 |
| 需求设计说明书-connector-platform-v3.md | **需求设计说明书** — V3 详细需求设计文档 | ✅ 已完成 |
| state.json | 状态文件 — 当前 phase: registered, status: tracked | 📋 registered |

### 📂 技术规划阶段 (plan)

| 文件 | 说明 | 版本 |
|------|------|------|
| plan.md | **技术规划** — V3 架构分析、模块划分、技术决策 | v3.0 |
| plan-db.md | **数据库设计** — 表结构、索引、迁移方案 | v2.0 |
| plan-api.md | **API 接口设计** — 管理面 + 运行时接口定义 | v7.0 |
| plan-runtime.md | **运行时引擎设计** — 连接流执行引擎、认证、限流 | v1.0 |
| plan-script.md | **脚本执行引擎设计** — GraalJS 沙箱，`function main(ctx)` 标准函数 | v8.1-draft |
| plan-json-schema.md | **JSON Schema 设计规范** — 全平台 JSON 结构权威定义 | v9.10 |
| plan-code.md | **代码规范** — 沿用 V1 全部规范（16 条规则） | — |
| plan-cache.md | **缓存设计方案** — 连接流响应缓存策略 | v4.0 |
| plan-flow-invoke-temp.md | **临时方案** — #54 调用连接流返回格式调整（透明穿透） | DRAFT |

### 📂 任务分解 (tasks)

| 文件 | 说明 | 状态 |
|------|------|------|
| tasks.md | **任务分解** — 14 个任务，4 个执行波次 | ✅ 已完成 |
| tasks.json | **任务数据** — JSON 格式任务数据 | ✅ 已完成 |

### 📂 架构决策记录 (ADR)

| 文件 | 标题 | 状态 |
|------|------|------|
| ADR-004.md | 版本完整快照存储与递增整数版本号 | PROPOSED |
| ADR-005.md | Redis 令牌桶入站限流方案 | PROPOSED |
| ADR-006.md | MySQL 主存储运行记录与日志 | PROPOSED |
| ADR-007.md | 多版本模型下的引用稽核策略 | PROPOSED |
| ADR-008.md | #54 调用连接流 — 透明穿透响应模式 | PROPOSED |

### 📂 分析文档

| 文件 | 说明 | 状态 |
|------|------|------|
| json-schema-gap-analysis.md | JSON Schema 缺口分析（v8.4 vs spec v2.16） | ✅ 全部完成 (10/10) |

## 任务波次总览

| 波次 | 任务数 | 范围 | 可并行 |
|:---:|:---:|------|:---:|
| Wave 1 | 3 | 数据库 Schema + 实体层 + 配置常量 | ✅ 3 任务并行 |
| Wave 2 | 4 | 管理面核心（连接器/连接流/安全/审批） | ✅ 4 任务并行 |
| Wave 3 | 4 | 运行时核心（引擎/认证/限流/缓存/脚本） | ✅ 4 任务并行 |
| Wave 4 | 3 | 运维调试（运行记录/调试/操作日志） | ✅ 2+1 |

## 设计方向对比

| 维度 | V2（已归档） | V3（当前） |
|------|:-----------:|:---------:|
| 交互方式 | 编排画布（React Flow 拖拽） | 流程编排（表单配置） |
| 节点类型 | 触发器/连接器/数据输出/数据处理/错误处理 | 触发器/连接器/数据输出/**脚本** |
| 数据校验 | 11 条严格类型约束 | 仅 JSON 语法合法性 |
| 复杂逻辑 | 数据处理节点 + 错误处理策略 | 脚本节点（GraalJS 沙箱） |
| 设计目标 | 平台保证正确性 | 用户自行负责，平台兜底 |

## 上级目录

- [返回上级](../README.md) — SDDU 规范目录
- [返回首页](../../README.md) — SDDU 工作空间
- [V2（已终止）](../specs-tree-connector-platform-v2/README.md)

## 下一步

1. 运行 `@sddu build connector-platform-v3` 启动任务实现
2. 按 Wave 1→4 顺序执行 14 个构建任务

---

*最后更新: 2026-06-22 | @sddu-docs 自动生成*
