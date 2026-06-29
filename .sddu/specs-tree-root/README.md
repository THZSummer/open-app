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
| **CONN-PLAT-002** | [连接器平台 V2 — 多版本与增强](./specs-tree-connector-platform-v2/) | 🚫 terminated（已终止，转入 V3） | P1 | Summer | 2026-06-17 |
| **CONN-PLAT-003** | [连接器平台 V3 — 多版本与增强](./specs-tree-connector-platform-v3/) | ✅ validated — completed（全流程完成） | P1 | Summer | 2026-06-22 |
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
- [ADR-004](./specs-tree-capability-open-platform/ADR-004.md) - 事件/回调通道地址白名单控制（ACCEPTED）
- [通道地址白名单补丁规划](./specs-tree-capability-open-platform/plan-channel-whitelist.md) - 白名单校验方案、任务分解、测试计划（2d）
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

**状态**: 🚫 terminated（已终止 — 用户标记丢弃，工作转入 V3）
**终止时间**: 2026-06-17
**转入**: [CONN-PLAT-003 连接器平台 V3](./specs-tree-connector-platform-v3/)

**文档**:
- [产品规范 v2.15-draft](./specs-tree-connector-platform-v2/spec.md) - 产品规范（802 行），52 项 FR、37 项 NFR
- [技术规划 v1.0](./specs-tree-connector-platform-v2/plan.md) - 技术规划（807 行），4 个 ADR（PROPOSED）
- [JSON Schema 设计规范](./specs-tree-connector-platform-v2/plan-json-schema.md) - V2 沿用 V1 的 JSON Schema 设计（v5.6）
- [状态文件](./specs-tree-connector-platform-v2/state.json) - terminated
- [ADR-004](./specs-tree-connector-platform-v2/ADR-004.md) - 版本完整快照存储与递增整数版本号（PROPOSED）
- [ADR-005](./specs-tree-connector-platform-v2/ADR-005.md) - Redis 令牌桶入站限流方案（PROPOSED）
- [ADR-006](./specs-tree-connector-platform-v2/ADR-006.md) - MySQL 主存储运行记录与日志（PROPOSED）
- [ADR-007](./specs-tree-connector-platform-v2/ADR-007.md) - 多版本模型下的引用稽核策略（PROPOSED）

**终止原因**: V2 设计方向为「完整好用」，过于复杂。V3 转向「简单可用，能兜底」。

---

### CONN-PLAT-003: 连接器平台 V3 — 多版本与增强

**状态**: ✅ validated — completed（全流程完成 — 注册 → 需求挖掘 → 规范 → 规划 → 任务分解 → 实现 → 审查 → 验证）
**Feature ID**: CONN-PLAT-003
**规范版本**: v3.0（47 FRs）
**依赖**: CONN-PLAT-001（V1 — validated）、CONN-PLAT-002（V2 — terminated）

**文档**:
- [产品规范 v3.0](./specs-tree-connector-platform-v3/spec.md) - 47 项 FR，设计方向：简单可用，能兜底
- [技术规划 v3.0](./specs-tree-connector-platform-v3/plan.md) - 架构分析、模块划分
- [数据库设计 v2.0](./specs-tree-connector-platform-v3/plan-db.md) - 表结构、索引、迁移
- [API 设计 v7.0](./specs-tree-connector-platform-v3/plan-api.md) - 管理面 + 运行时接口
- [运行时引擎 v1.0](./specs-tree-connector-platform-v3/plan-runtime.md) - 执行引擎、认证、限流
- [脚本引擎 v8.1-draft](./specs-tree-connector-platform-v3/plan-script.md) - GraalJS 沙箱，function main(ctx)
- [JSON Schema v9.10](./specs-tree-connector-platform-v3/plan-json-schema.md) - JSON 结构权威定义
- [任务分解](./specs-tree-connector-platform-v3/tasks.md) - 14 个任务，4 个波次
- [构建报告](./specs-tree-connector-platform-v3/build.md) - 14 任务全部实现
- [代码审查](./specs-tree-connector-platform-v3/review.md) - 审查通过，5 阻塞问题已修复
- [验证报告](./specs-tree-connector-platform-v3/validation-report.md) - FR 100% 覆盖，1 项已知漂移，有条件通过
- [测试任务](./specs-tree-connector-platform-v3/test-tasks.md) - 补充测试规划
- [ADR-004~008](./specs-tree-connector-platform-v3/ADR-004.md) - 5 个架构决策记录（全部 ACCEPTED）
- [状态文件](./specs-tree-connector-platform-v3/state.json) - validated / completed

**V3 vs V2 核心变化**:
| 维度 | V2（已终止） | V3（当前） |
|------|:-----------:|:---------:|
| 交互方式 | 编排画布（拖拽） | 流程编排（表单配置） |
| 节点类型 | 触发器/连接器/数据输出/数据处理/错误处理 | 触发器/连接器/数据输出/**脚本** |
| 数据校验 | 11 条严格类型约束 | 仅 JSON 语法合法性 |
| 复杂逻辑 | 数据处理节点 + 错误处理策略 | 脚本节点（GraalJS 沙箱） |

**下一步**:
🎉 全流程完成！后续：修复 GraalJS parse 校验漂移

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
