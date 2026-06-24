# 目录：连接器平台

## 目录简介

**连接器平台**是开放平台的组成部分，提供与 API、事件、回调同级并列的第四种开放形式。通过连接器封装和连接流可视化编排，将三方平台消费开放能力时的人工编码场景转化为低代码/零代码配置，使原有开放更便捷。

**当前状态**: ✅ validated（全流程完成 — 需求挖掘→规范→规划→任务分解→实现→审查→验证）
**目录 ID**: CONN-PLAT-001
**优先级**: P1
**创建时间**: 2026-05-14
**最新更新**: 2026-05-24（validate 完成，79 测试 100% FR 覆盖，全流程完成）

### 工作流进度

| 阶段 | 状态 | 完成时间 |
|------|------|----------|
| 🔍 需求挖掘 (discovery) | ✅ 完成 | 2026-05-14 |
| 📋 规范编写 (spec) | ✅ 完成 | 2026-05-18 (v1.0) → 2026-05-22 (v5.0) |
| 📐 技术规划 (plan) | ✅ 完成 | 2026-05-19 (v1.x) → 2026-05-24 (v2.8.1) |
| 📝 任务分解 (tasks) | ✅ 完成 | 2026-05-24（12 个任务，connector-api 目录结构对齐 open-server 完成） |
| 🏗️ 代码实现 (build) | ✅ 完成 | 2026-05-24（10 个任务全部实现，38 个文件） |
| 🔎 代码审查 (review) | ✅ 通过 | 2026-05-24（79 个测试，100% FR 覆盖，3 个改进项） |
| ✅ 代码验证 (validate) | ✅ 通过 | 2026-05-24（79 测试，100% FR 覆盖） |

## 目录结构

```
specs-tree-connector-platform/
├── README.md                  # 本文件 - 目录导航
├── discovery-report.md        # 需求挖掘报告
├── discovery-analysis.md      # 需求分析笔记
├── discovery-session-log.md   # 会话日志
├── spec.md                    # 产品规范文档（v5.0）
├── spec-v3.2.md               # 产品规范 v3.2（历史版本）
├── spec-v4.0.md               # 产品规范 v4.0（历史版本）
├── spec-v5.0.md               # 产品规范 v5.0（历史版本）
├── spec.json                  # 规范元数据
├── plan.md                    # 技术规划文档（v2.8.1，当前）
├── plan-v2.7.6.md             # 技术规划 v2.7.6（历史版本）
├── plan-v2.8.1.md             # 技术规划 v2.8.1（历史版本）
├── plan-page.md               # 前端页面设计文档（v2.0，当前）
├── plan-page-v2.7.5.md        # 前端页面设计 v2.7.5（历史版本）
├── plan-page-v2.8.0.md        # 前端页面设计 v2.8.0（历史版本）
├── plan-api.md                # API 接口设计文档（v2.0，当前）
├── plan-api-v2.7.6.md         # API 接口设计 v2.7.6（历史版本）
├── plan-api-v2.8.0.md         # API 接口设计 v2.8.0（历史版本）
├── plan-db.md                 # 数据库设计文档（v2.8.1，当前）
├── plan-db-v2.7.6.md          # 数据库设计 v2.7.6（历史版本）
├── plan-db-v2.8.1.md          # 数据库设计 v2.8.1（历史版本）
├── plan-code.md               # 代码规范文档
├── plan-code-check-report.md  # 代码规范符合性检查报告
├── plan-cache.md              # 缓存与限流策略设计
├── plan-json-schema.md        # JSON Schema 设计（当前）
├── plan-json-schema-v3.0.md   # JSON Schema v3.0（历史版本）
├── plan-json-schema-v5.4.md   # JSON Schema v5.4（历史版本）
├── plan-json-schema-impl.md   # JSON 构建逻辑实现参考
├── plan-multi-module-build.md # Maven 单进程合并构建方案
├── tasks.md                   # 任务分解文档（12 个任务，含 2 个外部占位，当前）
├── tasks.json                 # 任务元数据（当前）
├── tasks-v2.8.1.md            # 任务分解 v2.8.1（历史版本）
├── tasks-v2.8.1.json          # 任务元数据 v2.8.1（历史版本）
├── build.md                   # Build 实现报告（10 个任务全部完成，当前）
├── build-v2.8.1.md            # Build 实现报告 v2.8.1（历史版本）
├── review.md                  # 代码审查报告（当前）
├── review-v2.8.1.md           # 代码审查报告 v2.8.1（历史版本）
├── validation-report.md       # 代码验证报告（79 测试，100% FR 覆盖，当前）
├── validation-report-v2.8.1.md# 验证报告 v2.8.1（历史版本）
├── test-plan.md               # 测试方案：后端接口测试
├── test-plan-integration.md   # 测试方案：Python 集成测试
├── test-report.md             # 测试执行报告
├── test-report-integration.md # 集成测试报告
├── ADR-001.md                 # 架构决策：轻量顺序执行引擎
├── ADR-002.md                 # 架构决策：React Flow 可视化编排画布
├── ADR-003.md                 # 架构决策：单体嵌入 + 模块化隔离
└── state.json                 # 状态文件
```

## 文件说明

### 📂 需求挖掘阶段 (discovery)

| 文件 | 说明 | 状态 |
|------|------|------|
| discovery-report.md | 需求挖掘报告 - 连接器平台全面需求分析 | ✅ 已完成 |
| discovery-analysis.md | 需求分析笔记 - 用户场景分析、需求优先级评估 | ✅ 已完成 |
| discovery-session-log.md | 会话日志 - 原始对话记录 | ✅ 已完成 |

### 📂 规范阶段 (spec)

| 文件 | 说明 | 状态 |
|------|------|------|
| spec.md | **产品规范 v5.0（当前）** — 5 个用户故事、19 项 FR、22 项 NFR、12 项 EC | ✅ specified |
| spec-v3.2.md | 产品规范 v3.2 — 历史版本（对齐早期异步架构设计） | 📁 历史版本 |
| spec-v4.0.md | 产品规范 v4.0 — 历史版本备份 | 📁 历史版本 |
| spec-v5.0.md | 产品规范 v5.0 — 历史版本备份 | 📁 历史版本 |
| spec-v5.0.md               | 产品规范 v5.0 — 历史版本备份 | 📁 历史版本 |

### 📂 技术规划阶段 (plan)

| 文件 | 说明 | 状态 |
|------|------|------|
| plan.md | **技术规划 v2.8.1（当前）** — 同步执行引擎架构、DDL 脚本统一 FlywayDB 命名风格 | ✅ planned |
| plan-v2.7.6.md | 技术规划 v2.7.6 — 历史版本 | 📁 历史版本 |
| plan-v2.8.1.md | 技术规划 v2.8.1 — 历史版本 | 📁 历史版本 |
| plan-page.md | **前端页面设计 v2.0（当前）** — 触发器仅 HTTP/手动、数据处理节点入 MVP | ✅ planned |
| plan-page-v2.7.5.md | 前端页面设计 v2.7.5 — 历史版本 | 📁 历史版本 |
| plan-page-v2.8.0.md | 前端页面设计 v2.8.0 — 历史版本 | 📁 历史版本 |
| plan-api.md | **API 接口设计 v2.0（当前）** — 26 个端点覆盖 25 个 FR | ✅ planned |
| plan-api-v2.7.6.md | API 接口设计 v2.7.6 — 历史版本 | 📁 历史版本 |
| plan-api-v2.8.0.md | API 接口设计 v2.8.0 — 历史版本 | 📁 历史版本 |
| plan-db.md | **数据库设计 v2.8.1（当前）** — SQL 脚本存放于 `open-server/src/main/resources/db/migration/V2__init_connector_platform_schema.sql`，FlywayDB 命名风格，connector-api 不存放 DDL | ✅ planned |
| plan-db-v2.7.6.md | 数据库设计 v2.7.6 — 历史版本 | 📁 历史版本 |
| plan-db-v2.8.1.md | 数据库设计 v2.8.1 — 历史版本 | 📁 历史版本 |
| plan-code.md | **代码规范** — 16 条强制规则，沿用能力开放平台标准 | ✅ planned |
| plan-code-check-report.md | **代码规范符合性检查报告** — 74 个文件的规范检查 | ✅ 已检查 |
| plan-cache.md | **缓存与限流策略设计** — 连接器平台缓存体系与入站限流方案 | ✅ planned |
| plan-json-schema.md | JSON Schema 设计（当前，v5.6） | ✅ planned |
| plan-json-schema-v3.0.md | JSON Schema v3.0 — 历史版本 | 📁 历史版本 |
| plan-json-schema-v5.4.md | JSON Schema v5.4 — 历史版本 | 📁 历史版本 |
| plan-json-schema-impl.md | JSON 构建逻辑实现参考 — 设计态与运行态 JSON 构造算法说明 | ✅ 参考文档 |
| plan-multi-module-build.md | Maven 单进程合并构建方案 — connector-api 多模块构建策略 | ✅ planned |

### 📂 任务分解阶段 (tasks)

| 文件 | 说明 | 状态 |
|------|------|------|
| tasks.md | **任务分解文档（当前）** — 12 个任务、4 个波次、10 个当前实施 + 2 个外部占位；connector-api 包结构对齐 open-server（`com.xxx.it.works.wecode.v2`，`common` 与 `modules` 同级） | ✅ tasked |
| tasks.json | **任务元数据（当前）** — 任务结构化数据，含 connector-api 目录结构信息（`common`/`modules` 同级） | ✅ tasked |
| tasks-v2.8.1.md | 任务分解 v2.8.1 — 历史版本 | 📁 历史版本 |
| tasks-v2.8.1.json | 任务元数据 v2.8.1 — 历史版本 | 📁 历史版本 |

**任务概要**:
- **当前实施任务（10 个）**: TASK-001~TASK-008, TASK-011~TASK-012
- **外部占位任务（2 个）**: TASK-009（wecodesite 连接器前端页面）、TASK-010（wecodesite 连接流前端页面 + 编排画布）— 由其他渠道并行完成，不作为 SDDU build 实施链路强依赖
- **执行波次**: Wave 1 基础设施 → Wave 2 后端核心 → Wave 3 运行时+调试 → Wave 4 增强与集成
- **connector-api 目录结构**: 包名前缀统一 `com.xxx.it.works.wecode.v2`；`common`（公共能力）与 `modules`（业务模块）同级

### 📂 架构决策记录 (ADR)

| 文件 | 说明 | 状态 |
|------|------|------|
| ADR-001.md | 轻量顺序执行引擎技术方案 — 基于现有 open-server 扩展 | ✅ accepted |
| ADR-002.md | React Flow 可视化编排画布 — 前端编排器技术选型 | ✅ accepted |
| ADR-003.md | 运行时架构：单体嵌入 + 模块化隔离 — 运行时部署策略 | ✅ accepted |

### 其他

| 文件 | 说明 | 状态 |
|------|------|------|
| state.json | 状态文件 — 当前阶段 validate，状态 validated，全流程完成 | ✅ validated |

### 📂 代码实现阶段 (build)

| 文件 | 说明 | 状态 |
|------|------|------|
| build.md | **Build 实现报告（当前）** — 10 个任务全部完成，38 个新增文件 | ✅ built |
| build-v2.8.1.md | Build 实现报告 v2.8.1 — 历史版本 | 📁 历史版本 |

### 📂 代码审查阶段 (review)

| 文件 | 说明 | 状态 |
|------|------|------|
| review.md | **代码审查报告（当前）** — 审查通过，0 阻塞问题，79 个测试，100% FR 覆盖 | ✅ reviewed |
| review-v2.8.1.md | 代码审查报告 v2.8.1 — 历史版本 | 📁 历史版本 |

### 📂 代码验证阶段 (validate)

| 文件 | 说明 | 状态 |
|------|------|------|
| validation-report.md | **代码验证报告（当前）** — 79 测试全部通过，100% FR 覆盖，全流程完成 | ✅ validated |
| validation-report-v2.8.1.md | 验证报告 v2.8.1 — 历史版本 | 📁 历史版本 |

### 📂 测试阶段 (test)

| 文件 | 说明 | 状态 |
|------|------|------|
| test-plan.md | **测试方案（后端接口）** — 后端 API #1~#18 测试计划 | ✅ 已执行 |
| test-plan-integration.md | **测试方案（集成测试）** — Python 真调 L3 集成测试方案 | ✅ 已执行 |
| test-report.md | **测试执行报告** — 后端接口测试执行结果 | ✅ 已完成 |
| test-report-integration.md | **集成测试报告** — L3 集成测试执行结果 | ✅ 已完成 |

## 规范概要

| 维度 | 数量 |
|------|------|
| 用户故事 | 5（统一为平台管理员单一角色） |
| 功能需求 (FR) | 19（v5.0 精简后） |
| 非功能需求 (NFR) | 22 |
| 边界情况 (EC) | 12 |
| 开放问题 | 5 |

## 任务概要

| 维度 | 统计 |
|------|------|
| **总任务数** | 12 |
| **当前实施任务** | 10（TASK-001~008, 011~012） |
| **外部占位任务** | 2（TASK-009~010 — wecodesite 前端，由其他渠道完成） |
| **复杂度分布** | S 级 2 个，M 级 6 个，L 级 2 个 |
| **执行波次** | 4 个 |
| **FR 覆盖** | 全量 19 个 FR |

### 执行计划

| 波次 | 任务 | 说明 |
|:----:|------|------|
| 1 | TASK-001~002 | 基础设施（DB DDL + connector-api 脚手架） |
| 2 | TASK-003~005 | 后端核心（连接器模块 + 连接流模块 + 运行时引擎） |
| 3 | TASK-006~010 | 运行时端点 + 调试代理 + 前端占位（TASK-009/010 为外部占位） |
| 4 | TASK-011~012 | 增强与集成（错误处理/限流/审计 + E2E 测试） |

> **注**: TASK-009（wecodesite 连接器前端页面）和 TASK-010（wecodesite 连接流前端页面 + 编排画布）为 **外部占位任务**，由其他渠道并行完成，不作为当前 SDDU build 实施链路的强依赖。

## 核心技术决策

### 方案选择
- **推荐方案**: 轻量**同步**执行引擎（基于 open-server 扩展，不引入额外框架）
- **编排配置**: 以 JSON 存储，运行时引擎按节点顺序依次**同步**执行
- **调度方式**: 无消息队列——HTTP 请求/手动触发直接同步执行
- **执行上下文**: 方法调用参数传递（运行时）+ MySQL 持久化
- **MVP 节点类型**: 连接器节点 + 数据处理节点
- **MVP 触发方式**: HTTP 触发 + 手动触发
- **外部依赖**: 不依赖能力开放平台（Scope/审批移至 V1）

## 上级目录

- [返回上级](../README.md) - SDDU 规范目录
- [返回首页](../../README.md) - SDDU 工作空间

## 下一步

🎉 **Feature 全流程完成！** 所有阶段已通过 — 需求挖掘→规范→规划→任务分解→实现→审查→验证，79 测试全部通过，100% FR 覆盖。

---

*最后更新: 2026-06-09 | @sddu-docs 自动更新*