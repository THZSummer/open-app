# 模块重组 — 目录导航

**Feature ID**: EMBED-PLATFORM-RESTRUCTURE-001
**父 Feature**: EMBED-PLATFORM-001（嵌入能力平台面）
**阶段**: tasked | 复杂度: M | 顺序: 01/10

## 对应任务

- **任务ID**: TASK-001
- **关联FR**: （无）

## 目录结构

```
specs-tree-platform-01-restructure/
├── state.json        # 任务状态
├── tasks.md          # 任务详情
└── TREE.md           # 本文件 — 目录导航
```

## 职责

market-server ability代码从approval模块独立为独立ability模块

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-server/src/test/java/.../ability/AbilityMapperSmokeTest.java` |
| NEW | `market-server/src/test/python/modules/approval/test_approval_smoke.py` |

---

*最后更新: 2026-07-16*
