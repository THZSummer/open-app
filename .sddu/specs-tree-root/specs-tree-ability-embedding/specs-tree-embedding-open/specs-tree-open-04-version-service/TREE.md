# VersionServiceImpl 改造 — 目录导航

**Feature ID**: EMBED-OPEN-001
**父 Feature**: EMBED-OPEN-001（嵌入能力开放面）
**阶段**: tasked | 复杂度: S | 模块: version

## 对应任务

- **任务ID**: TASK-004
- **关联ADR**: ADR-004

## 目录结构

```
specs-tree-open-04-version-service/
├── state.json        # 任务状态
├── tasks.md          # 任务详情
├── tasks.json        # 任务配置
└── TREE.md           # 本文件 — 目录导航
```

## 职责

修改 `VersionServiceImpl.createVersion()` 第173行：硬编码 `!Objects.equals(r.getAbilityType(), GROUP_JOIN_NOTIFICATION.getCode())` → 按 `require_release = 1` 字段过滤。

---

*最后更新: 2026-07-20*
