# 嵌入能力平台面 — 目录导航

**Feature ID**: EMBED-PLATFORM-001
**状态**: tasked（任务已分解为10个原子化子Feature）

## 目录结构

```
specs-tree-embedding-platform/
├── state.json                        # Feature 状态
├── spec.md                           # 需求规范 v1.1
├── plan.md                           # 技术方案 v3.0
├── tasks.md                          # 任务总览 v7.0（10任务/串行）
├── TREE.md                           # 本文件
├── ADR-001.md                        # Admin CRUD 放 open-server
├── ADR-002.md                        # abilityType 编码规则
├── ADR-004.md                        # require_release 替代硬编码
├── 需求设计说明.md
│
├── specs-tree-platform-01-db/            # TASK-001 [S] 01/10
├── specs-tree-platform-02-restructure/   # TASK-002 [M] 02/10
├── specs-tree-platform-03-list-api/      # TASK-003 [M] 03/10
├── specs-tree-platform-04-list-page/     # TASK-004 [L] 04/10
├── specs-tree-platform-05-create-api/    # TASK-005 [M] 05/10
├── specs-tree-platform-06-create-page/   # TASK-006 [M] 06/10
├── specs-tree-platform-07-edit-api/      # TASK-007 [M] 07/10
├── specs-tree-platform-08-edit-page/     # TASK-008 [M] 08/10
├── specs-tree-platform-09-delete-api/    # TASK-009 [S] 09/10
└── specs-tree-platform-10-delete-page/   # TASK-010 [S] 10/10
```

## 子Feature一览

| 顺序 | 子Feature | 目录 | 复杂度 | 职责 |
|:--:|-----------|------|:--:|------|
| 01/10 | 数据库变更 | specs-tree-platform-01-db | S | V4迁移（open-server） |
| 02/10 | 模块重组 | specs-tree-platform-02-restructure | M | ability代码从approval独立 |
| 03/10 | 列表接口（后端） | specs-tree-platform-03-list-api | M | 分页查询接口 |
| 04/10 | 列表页面（前端） | specs-tree-platform-04-list-page | L | 列表页 + 路由 |
| 05/10 | 新增接口（后端） | specs-tree-platform-05-create-api | M | 创建能力接口 |
| 06/10 | 新增表单（前端） | specs-tree-platform-06-create-page | M | 创建表单 |
| 07/10 | 编辑接口（后端） | specs-tree-platform-07-edit-api | M | 更新能力接口 |
| 08/10 | 编辑表单（前端） | specs-tree-platform-08-edit-page | M | 编辑表单 |
| 09/10 | 删除接口（后端） | specs-tree-platform-09-delete-api | S | 删除能力接口 |
| 10/10 | 删除操作（前端） | specs-tree-platform-10-delete-page | S | 删除按钮 |

---

*最后更新: 2026-07-16*
