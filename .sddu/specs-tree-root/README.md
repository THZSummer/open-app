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
| **CONN-PLAT-001** | [连接器平台 V1](./specs-tree-connector-platform/) | ✅ validated（全流程完成） | P1 | - | 2026-05-24 |
| **CONN-PLAT-002** | [连接器平台 V2 — 多版本与增强](./specs-tree-connector-platform-v2/) | ✅ planned（规范+规划完成，待任务分解） | P1 | Summer | 2026-06-09 |
| **DATA-OPEN-001** | [数据开放平台](./specs-tree-data-open-platform/) | 🟡 suspended（搁置） | P0 | Summer | 2026-04-07 |
| **FR-DICTIONARY-001** | [数据字典管理](./specs-tree-dictionary/) | ✅ planned（规范+规划完成） | P1 | SDDU-Spec Agent | 2026-05-22 |
| **FR-LOOKUP-001** | [LookUp 管理](./specs-tree-lookup/) | ✅ planned（规范+规划完成） | P1 | SDDU-Spec Agent | 2026-05-15 |

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

**状态**: ✅ validated（全流程完成 — 需求挖掘 → 规范 → 规划 → 任务分解 → 实现 → 审查 → 验证）

**文档**:
- [需求挖掘报告](./specs-tree-connector-platform/discovery-report.md) - 连接器平台需求分析
- [需求分析笔记](./specs-tree-connector-platform/discovery-analysis.md) - 用户场景分析
- [产品规范 v5.0](./specs-tree-connector-platform/spec.md) - 5 个用户故事、19 项 FR
- [技术规划 v2.8.1](./specs-tree-connector-platform/plan.md) - 同步执行引擎架构，DDL 脚本 FlywayDB 命名风格
- [接口设计 v2.8.0](./specs-tree-connector-platform/plan-api.md) - 26 个端点
- [数据库设计 v2.8.1](./specs-tree-connector-platform/plan-db.md) - SQL 脚本存放于 open-server `db/migration/V2__init_connector_platform_schema.sql`，FlywayDB 命名风格
- [任务分解](./specs-tree-connector-platform/tasks.md) - 12 个任务（10 个实施 + 2 个外部占位），connector-api 目录结构对齐 open-server（`com.xxx.it.works.wecode.v2`，`common` 与 `modules` 同级）
- [实现报告](./specs-tree-connector-platform/build.md) - 10 个任务全部完成，38 个新增文件
- [代码审查](./specs-tree-connector-platform/review.md) - 审查通过，0 阻塞问题，3 个改进项
- [验证报告](./specs-tree-connector-platform/validation-report.md) - 代码验证报告（79 测试，100% FR 覆盖）
- [ADR-001~003](./specs-tree-connector-platform/ADR-001.md) - 架构决策记录（3 份）

**核心信息**:
- 12 个任务：10 个当前实施任务 + 2 个外部占位任务（TASK-009/TASK-010 为 wecodesite 前端任务）
- 4 个执行波次：基础设施 → 后端核心 → 运行时+调试 → 增强与集成
- 测试覆盖：79 个测试（open-server 47 + connector-api 32），全部通过
- 执行模型：同步执行引擎（基于 open-server 扩展）
- 预估工期：8-12 周
- **下一步**: 🎉 全流程完成（需求挖掘→规范→规划→任务分解→实现→审查→验证）

---

### CONN-PLAT-002: 连接器平台 V2 — 多版本与增强

**状态**: ✅ planned（规范编写完成，技术规划完成，待任务分解）
**规范版本**: v2.15-draft
**规划版本**: v1.0
**依赖**: CONN-PLAT-001（V1 MVP — 已建成并验证）

**文档**:
- [产品规范 v2.15-draft](./specs-tree-connector-platform-v2/spec.md) - 产品规范（802 行），52 项 FR、37 项 NFR
- [技术规划 v1.0](./specs-tree-connector-platform-v2/plan.md) - 技术规划（807 行），4 个 ADR（PROPOSED）
- [JSON Schema 设计规范](./specs-tree-connector-platform-v2/plan-json-schema.md) - V2 沿用 V1 的 JSON Schema 设计（v5.6）
- [状态文件](./specs-tree-connector-platform-v2/state.json) - planned / v2.15-draft
- [ADR-004](./specs-tree-connector-platform-v2/ADR-004.md) - 版本完整快照存储与递增整数版本号（PROPOSED）
- [ADR-005](./specs-tree-connector-platform-v2/ADR-005.md) - Redis 令牌桶入站限流方案（PROPOSED）
- [ADR-006](./specs-tree-connector-platform-v2/ADR-006.md) - MySQL 主存储运行记录与日志（PROPOSED）
- [ADR-007](./specs-tree-connector-platform-v2/ADR-007.md) - 多版本模型下的引用稽核策略（PROPOSED）

**核心能力**（三大升级方向）：

| 方向 | 关键能力 |
|------|---------|
| **G1: 多版本及生命周期** | 5 状态状态机（草稿→已发布→已弃用→已退役→已归档）、SemVer 规则、版本快照/对比/回滚、双凭据轮转零停机、全量审计日志 |
| **G2: 连接配置增强** | 7 种认证方式（含 mTLS）、三级超时模型、令牌桶限流、Fork-Join 并行编排（≤ 5 分支）、3 种失败/合并策略 |
| **G3: 运行时与监控** | 执行历史（7 状态，30 天保留）、5 级可配运行日志、重试+指数退避+熔断器+死信队列 |

**范围亮点**:
- **52 项功能需求**（FR-001 ~ FR-052），**37 项非功能需求**（NFR-001 ~ NFR-037）
- **4 角色用户画像**：连接器开发者 / 集成工程师 / SRE 工程师 / 业务用户
- **10 个用户故事**、**25 个边界情况**、**12 个开放问题**
- **Gherkin 场景**：版本发布、版本弃用、凭据轮转、失败重试与熔断
- **36 个 API 端点**（V1 保留 15 + 修改 3 + 新增 17）
- **4 个 ADR**（PROPOSED）：版本完整快照存储、Redis 令牌桶限流、MySQL 执行历史、引用稽核策略
- **5 批次交付**：多版本(3-4w) → 认证(2-3w) → 并行(3-4w) → 历史(2-3w) → 重试(1-2w) → GA(2w)

**下一步**:
1. 运行 `@sddu-tasks connector-platform-v2` 进行任务分解
2. ADR 评审：4 个 PROPOSED ADR 待 Review 后转为 ACCEPTED

---

### DATA-OPEN-001: 数据开放平台

**状态**: 🟡 suspended（搁置）

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

### FR-DICTIONARY-001: 数据字典管理

**状态**: ✅ planned（规范编写完成，技术规划完成，待任务分解）
**规范版本**: v1.0
**规划版本**: v1.0

**文档**:
- [产品规范 v1.0](./specs-tree-dictionary/spec.md) - 数据字典管理功能规范（113 行）
- [技术规划 v1.0](./specs-tree-dictionary/plan.md) - 技术规划（509 行）

**核心功能**:
- 数据字典 CRUD：编码、名称、路径（层级归类）、语言（中英文多语言）
- 异步批量导入/导出 Excel，任务中心进度跟踪
- 单表扁平结构，灵活扩展

**技术栈**: React + Java 21 + Spring Boot 3.x + MyBatis-Plus + MySQL 8.0 + Apache POI

**下一步**: 运行 `@sddu-tasks 数据字典管理` 开始任务分解

---

### FR-LOOKUP-001: LookUp 管理

**状态**: ✅ planned（规范编写完成，技术规划完成，待任务分解）
**规范版本**: v1.0
**规划版本**: v1.0

**文档**:
- [产品规范 v1.0](./specs-tree-lookup/spec.md) - LookUp 管理功能规范（192 行）
- [技术规划 v1.0](./specs-tree-lookup/plan.md) - 技术规划（1511 行）

**核心功能**:
- LookUp 分类管理：分类 CRUD、级联删除、异步批量导入/导出
- LookUp 项管理：项 CRUD、扩展属性 1-6、排序、状态切换
- 标准化的枚举值和配置项集中维护机制

**技术栈**: React + Java 21 + Spring Boot 3.x + MyBatis-Plus + MySQL 8.0 + Apache POI

**下一步**: 运行 `@sddu-tasks LookUp管理` 开始任务分解

---

## 使用说明

- 每个 Feature 有独立的目录
- 文档会自动维护（`@sddu-docs`）
- 使用 `@sddu 开始 [feature 名称]` 开始新 feature
