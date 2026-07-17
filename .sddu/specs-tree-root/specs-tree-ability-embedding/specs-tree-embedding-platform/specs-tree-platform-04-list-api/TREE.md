# 列表接口（后端） — 目录导航

**Feature ID**: EMBED-PLATFORM-LIST-API-001
**父 Feature**: EMBED-PLATFORM-001（嵌入能力平台面）
**阶段**: tasked | 复杂度: M | 顺序: 03/10

## 对应任务

- **任务ID**: TASK-003
- **关联FR**: FR-001

## 目录结构

```
specs-tree-platform-03-list-api/
├── state.json        # 任务状态
├── tasks.md          # 任务详情
└── TREE.md           # 本文件 — 目录导航
```

## 职责

AdminAbilityListRequest → VO → Mapper分页查询 → Service.list() → Controller GET /admin/list

---

*最后更新: 2026-07-16*
