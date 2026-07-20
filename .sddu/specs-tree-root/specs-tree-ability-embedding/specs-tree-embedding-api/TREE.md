# 嵌入能力API面 — 目录导航

**Feature ID**: EMBED-API-001
**状态**: tasked（1 个后端 Task）

## 目录结构

```
specs-tree-embedding-api/
├── state.json                        # Feature 状态
├── TREE.md                           # 本文件 — 目录导航
├── spec.md                           # 需求规范 v1.2
├── plan.md                           # 技术方案 v1.2
├── tasks.md                          # 任务总览（1 后端 / 按接口维度）
├── ADR-001.md                        # 新建独立 UserRoleController
├── ADR-002.md                        # 应用标识解析器模式
├── ADR-003.md                        # 内部凭证鉴权方案
│
└── specs-tree-api-01-user-roles/     # TASK-001 [M] POST /internal/user/roles
```

## 子Feature一览

| 顺序 | 子Feature | 目录 | 复杂度 | 接口 | FR |
|:--:|-----------|------|:--:|------|:--:|
| 01 | 用户角色查询 | specs-tree-api-01-user-roles | M | POST /internal/user/roles | FR-001 |

## 依赖关系

- **前置依赖**: 无（独立于平台面和开放面）
- **后端 Task**: 仅 1 个 Task，含 DTO/解析/鉴权/Service/Controller/配置
- **推荐开发顺序**（单分支内）: DTO → 配置 → 鉴权 → 解析器 → Service → Controller

---

*最后更新: 2026-07-20*
