# 嵌入能力平台面 — 目录导航

**Feature ID**: EMBED-PLATFORM-001
**状态**: tasked（任务已分解为10个原子化子Feature）

## 目录结构

```
specs-tree-embedding-platform/
├── state.json                        # Feature 状态
├── spec.md                           # 需求规范 v1.1
├── plan.md                           # 技术方案 v3.0
├── tasks.md                          # 任务总览 v6.0（10任务/4波次）
├── TREE.md                           # 本文件
├── ADR-001.md                        # Admin CRUD 放 open-server
├── ADR-002.md                        # abilityType 编码规则
├── ADR-004.md                        # require_release 替代硬编码
├── 需求设计说明.md
│
├── specs-tree-platform-01-db/            # TASK-001 [S] Wave 1
├── specs-tree-platform-01-restructure/   # TASK-002 [M] Wave 1
├── specs-tree-platform-02-list-api/      # TASK-003 [M] Wave 2
├── specs-tree-platform-02-create-api/    # TASK-005 [M] Wave 2
├── specs-tree-platform-02-edit-api/      # TASK-007 [M] Wave 2
├── specs-tree-platform-02-delete-api/    # TASK-009 [S] Wave 2
├── specs-tree-platform-03-list-page/     # TASK-004 [L] Wave 3
├── specs-tree-platform-03-create-page/   # TASK-006 [M] Wave 3
├── specs-tree-platform-03-edit-page/     # TASK-008 [M] Wave 3
└── specs-tree-platform-04-delete-page/   # TASK-010 [S] Wave 4
```

## 子Feature一览

| 子Feature | 目录 | 复杂度 | 波次 | 职责 |
|-----------|------|:--:|:--:|------|
| 数据库变更 | specs-tree-platform-01-db | S | 1 | V4迁移（open-server） |
| 模块重组 | specs-tree-platform-01-restructure | M | 1 | ability代码从approval独立 |
| 列表接口（后端） | specs-tree-platform-02-list-api | M | 2 | 分页查询接口 |
| 列表页面（前端） | specs-tree-platform-03-list-page | L | 3 | 列表页 + 路由 |
| 新增接口（后端） | specs-tree-platform-02-create-api | M | 2 | 创建能力接口 |
| 新增表单（前端） | specs-tree-platform-03-create-page | M | 3 | 创建表单 |
| 编辑接口（后端） | specs-tree-platform-02-edit-api | M | 2 | 更新能力接口 |
| 编辑表单（前端） | specs-tree-platform-03-edit-page | M | 3 | 编辑表单 |
| 删除接口（后端） | specs-tree-platform-02-delete-api | S | 2 | 删除能力接口 |
| 删除操作（前端） | specs-tree-platform-04-delete-page | S | 4 | 删除按钮 |

---

*最后更新: 2026-07-16*
