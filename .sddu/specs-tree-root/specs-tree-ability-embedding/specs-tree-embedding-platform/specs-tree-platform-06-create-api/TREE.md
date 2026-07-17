# 新增接口（后端） — 目录导航

**Feature ID**: EMBED-PLATFORM-CREATE-API-001
**父 Feature**: EMBED-PLATFORM-001（嵌入能力平台面）
**阶段**: tasked | 复杂度: M | 顺序: 05/10

## 对应任务

- **任务ID**: TASK-005
- **关联FR**: FR-002

## 目录结构

```
specs-tree-platform-05-create-api/
├── state.json        # 任务状态
├── tasks.md          # 任务详情
└── TREE.md           # 本文件 — 目录导航
```

## 职责

AdminAbilityCreateRequest → 校验(编码唯一性/URL格式) → Service.create() → 写主表+属性表 → Controller POST

---

*最后更新: 2026-07-16*
