# 嵌入能力开放面 — 目录导航

**Feature ID**: EMBED-OPEN-001
**状态**: tasked（4 个后端 Task 已分解）

## 目录结构

```
specs-tree-embedding-open/
├── state.json                        # Feature 状态
├── TREE.md                           # 本文件 — 目录导航
├── spec.md                           # 需求规范 v1.3
├── plan.md                           # 技术方案 v1.6
├── tasks.md                          # 任务总览（4 后端 / 按接口维度）
├── ADR-001.md                        # 增量修改现有 ability 模块
├── ADR-002.md                        # 配置页运行时 loadMicroApp
│
├── specs-tree-open-01-list-api/      # TASK-001 [M] GET /ability/list
├── specs-tree-open-02-subscribe-api/ # TASK-002 [M] POST /ability
├── specs-tree-open-03-subscribed-api/# TASK-003 [S] GET /ability/subscribed
└── specs-tree-open-04-version-service/ # TASK-004 [S] VersionServiceImpl
```

## 子Feature一览

| 顺序 | 子Feature | 目录 | 复杂度 | 接口 | FR |
|:--:|-----------|------|:--:|------|:--:|
| 01 | 能力列表增强 | specs-tree-open-01-list-api | M | GET /ability/list | FR-001, FR-005 |
| 02 | 能力订阅增强 + 自动桥接 | specs-tree-open-02-subscribe-api | M | POST /ability | FR-002, FR-003 |
| 03 | 已订阅列表增强 | specs-tree-open-03-subscribed-api | S | GET /ability/subscribed | FR-004 |
| 04 | VersionServiceImpl 改造 | specs-tree-open-04-version-service | S | createVersion() | ADR-004 |

> ⚠️ 前端 FR-101~103 由独立流程负责，不在本 Feature 跟踪。

## 依赖关系

- **前置依赖**: 平台面 EMBED-PLATFORM-001（V4 迁移已执行，ability 表含新字段）
- **后端 Task**: TASK-001~004 无相互依赖，可并行开发
- **推荐顺序**: 先 ①（含 VO 公共基础）→ ②③④ 并行

---

*最后更新: 2026-07-20*
