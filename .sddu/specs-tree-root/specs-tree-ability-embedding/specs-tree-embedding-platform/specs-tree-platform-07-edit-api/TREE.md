# 编辑接口（后端） — 目录导航

**Feature ID**: EMBED-PLATFORM-EDIT-API-001
**父 Feature**: EMBED-PLATFORM-001（嵌入能力平台面）
**阶段**: tasked | 复杂度: M | 顺序: 07/10

## 对应任务

- **任务ID**: TASK-007
- **关联FR**: FR-003

## 目录结构

```
specs-tree-platform-07-edit-api/
├── state.json        # 任务状态
├── tasks.md          # 任务详情
└── TREE.md           # 本文件 — 目录导航
```

## 职责

AdminAbilityUpdateRequest → Service.update()(部分更新,abilityType不可改) → Controller PUT /{id}

---

*最后更新: 2026-07-16*
